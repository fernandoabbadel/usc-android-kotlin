import { NextRequest, NextResponse } from "next/server";

import { normalizePaymentConfig, type CommerceTicketEntry } from "@/lib/commerceCatalog";
import { supabaseAdmin } from "@/lib/supabaseAdmin";

import {
  LeagueAdminApiError,
  asObject,
  asString,
  getLeagueAdminAuthScope,
} from "../../ligas/_auth";

export const runtime = "nodejs";

const EVENT_QR_READER_ROLES = new Set(["master", "master_tenant", "admin_geral", "admin_tenant", "vendas"]);

type CheckinAction = "edit" | "undo";

const extractMissingColumn = (error: unknown): string => {
  if (!error || typeof error !== "object") return "";
  const raw = error as { message?: unknown; details?: unknown };
  const text = [raw.message, raw.details]
    .map((entry) => (typeof entry === "string" ? entry : ""))
    .join(" ");
  const match =
    text.match(/column\s+[a-z0-9_]+\.(["']?)([a-z0-9_]+)\1\s+does not exist/i) ||
    text.match(/could not find the ['"]?([a-z0-9_]+)['"]? column/i);
  return String(match?.[2] || match?.[1] || "");
};

const isApprovedStatus = (status: string): boolean =>
  ["aprovado", "approved", "pago", "paid", "entregue", "presente"].includes(status.trim().toLowerCase());

const parseDateTimeValue = (value: unknown): number => {
  const parsed = typeof value === "string" || typeof value === "number" ? new Date(value) : null;
  return parsed && !Number.isNaN(parsed.getTime()) ? parsed.getTime() : 0;
};

const latestScannedEntry = (entries: CommerceTicketEntry[]): CommerceTicketEntry | null =>
  [...entries]
    .filter((entry) => entry.status === "lido" || Boolean(entry.scannedAt))
    .sort((left, right) => parseDateTimeValue(right.scannedAt) - parseDateTimeValue(left.scannedAt))[0] ?? null;

const buildOrderCheckinSummary = (entries: CommerceTicketEntry[]) => {
  const latest = latestScannedEntry(entries);
  return {
    checkinAt: latest?.scannedAt || null,
    checkinByUserId: latest?.scannedByUserId || null,
    checkinByUserName: latest?.scannedByUserName || null,
    checkinMethod: latest ? (latest.scanSource === "manual" ? "manual" : "qr") : null,
    checkinNote: latest?.checkinNote || null,
  };
};

const findTargetEntry = (entries: CommerceTicketEntry[], ticketToken: string): CommerceTicketEntry | null => {
  if (ticketToken) return entries.find((entry) => entry.token === ticketToken) ?? null;
  return latestScannedEntry(entries) ?? entries.find((entry) => entry.status !== "transferido") ?? null;
};

export async function POST(request: NextRequest) {
  try {
    const scope = await getLeagueAdminAuthScope(request);
    const hasTenantScannerRole =
      scope.isPlatformMaster ||
      (scope.tenantStatus === "approved" &&
        (EVENT_QR_READER_ROLES.has(scope.tenantRole) || EVENT_QR_READER_ROLES.has(scope.userRole)));
    if (!hasTenantScannerRole) {
      throw new LeagueAdminApiError("Sem permissão para editar check-ins.", 403);
    }

    const { data: readerProfileRow } = await supabaseAdmin
      .from("users")
      .select("uid,nome,turma,foto")
      .eq("uid", scope.userId)
      .maybeSingle();
    const readerProfile = asObject(readerProfileRow) ?? {};
    const readerName = asString(readerProfile.nome) || scope.userId;

    const body = asObject(await request.json());
    const action = asString(body?.action) as CheckinAction;
    const orderId = asString(body?.orderId);
    const eventId = asString(body?.eventId);
    const ticketToken = asString(body?.ticketToken);
    const note = asString(body?.note).slice(0, 500);
    const scannedByUserName = asString(body?.scannedByUserName).slice(0, 160);
    const scannedAtInput = asString(body?.scannedAt);
    const scannedAt = scannedAtInput && !Number.isNaN(new Date(scannedAtInput).getTime())
      ? new Date(scannedAtInput).toISOString()
      : "";

    if (!orderId || !eventId) {
      throw new LeagueAdminApiError("Pedido ou evento inválido.", 400);
    }
    if (action !== "edit" && action !== "undo") {
      throw new LeagueAdminApiError("Ação de check-in inválida.", 400);
    }

    const { data: orderRow, error: orderError } = await supabaseAdmin
      .from("solicitacoes_ingressos")
      .select("id,tenant_id,eventoId,status,payment_config,checkinAuditLog")
      .eq("id", orderId)
      .maybeSingle();
    if (orderError) {
      throw new LeagueAdminApiError(orderError.message, 400);
    }

    const order = asObject(orderRow);
    if (!order) {
      throw new LeagueAdminApiError("Pedido do ingresso não encontrado.", 404);
    }
    if (asString(order.eventoId) !== eventId) {
      throw new LeagueAdminApiError("Esse ingresso pertence a outro evento.", 400);
    }
    if (!scope.isPlatformMaster && scope.userTenantId !== asString(order.tenant_id)) {
      throw new LeagueAdminApiError("Ingresso fora do tenant ativo.", 403);
    }
    if (!isApprovedStatus(asString(order.status))) {
      throw new LeagueAdminApiError("Pagamento ainda não aprovado para check-in.", 400);
    }

    const paymentConfig = normalizePaymentConfig(order.payment_config);
    const entries = paymentConfig?.ticketEntries || [];
    const targetEntry = findTargetEntry(entries, ticketToken);
    if (!paymentConfig || !targetEntry) {
      throw new LeagueAdminApiError("Ingresso não encontrado para edição.", 404);
    }
    if (targetEntry.status === "transferido") {
      throw new LeagueAdminApiError("Ingresso transferido não pode receber baixa manual nesta origem.", 400);
    }

    const now = new Date().toISOString();
    const auditEntry = {
      action,
      at: now,
      byUserId: scope.userId,
      byUserName: readerName,
      note,
      ticketToken: targetEntry.token,
    };

    const nextTicketEntries = entries.map((entry) => {
      if (entry.token !== targetEntry.token) return entry;
      const nextAuditLog = [...(Array.isArray(entry.checkinAuditLog) ? entry.checkinAuditLog : []), auditEntry].slice(-50);

      if (action === "undo") {
        const rest: CommerceTicketEntry = { ...entry };
        delete rest.scannedAt;
        delete rest.scannedByUserId;
        delete rest.scannedByUserName;
        delete rest.scannedByUserTurma;
        delete rest.scannedByUserAvatar;
        delete rest.scanSource;
        delete rest.checkinNote;
        return {
          ...rest,
          status: "ativo" as const,
          checkinEditedAt: now,
          checkinEditedByUserId: scope.userId,
          checkinEditedByUserName: readerName,
          checkinAuditLog: nextAuditLog,
        };
      }

      return {
        ...entry,
        status: "lido" as const,
        scannedAt: scannedAt || entry.scannedAt || now,
        scannedByUserName: scannedByUserName || entry.scannedByUserName || readerName,
        checkinNote: note || entry.checkinNote || "",
        checkinEditedAt: now,
        checkinEditedByUserId: scope.userId,
        checkinEditedByUserName: readerName,
        checkinAuditLog: nextAuditLog,
      };
    });

    const summary = buildOrderCheckinSummary(nextTicketEntries);
    let updatePayload: Record<string, unknown> = {
      payment_config: {
        ...paymentConfig,
        ticketEntries: nextTicketEntries,
      },
      ...summary,
      checkinEditedAt: now,
      checkinEditedByUserId: scope.userId,
      checkinEditedByUserName: readerName,
      checkinAuditLog: [
        ...(Array.isArray(order.checkinAuditLog) ? order.checkinAuditLog : []),
        auditEntry,
      ].slice(-100),
    };

    while (Object.keys(updatePayload).length > 0) {
      const { error: updateError } = await supabaseAdmin
        .from("solicitacoes_ingressos")
        .update(updatePayload)
        .eq("id", orderId);
      if (!updateError) break;

      const missingColumn = extractMissingColumn(updateError);
      if (!missingColumn || !Object.prototype.hasOwnProperty.call(updatePayload, missingColumn)) {
        throw new LeagueAdminApiError(updateError.message, 400);
      }
      const nextPayload = { ...updatePayload };
      delete nextPayload[missingColumn];
      updatePayload = nextPayload;
    }

    return NextResponse.json({
      ok: true,
      action,
      orderId,
      ticketToken: targetEntry.token,
      ...summary,
    });
  } catch (error: unknown) {
    if (error instanceof LeagueAdminApiError) {
      return NextResponse.json({ error: error.message }, { status: error.status });
    }

    const message = error instanceof Error ? error.message : "Erro ao editar check-in.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
