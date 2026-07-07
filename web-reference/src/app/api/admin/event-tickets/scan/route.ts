import { NextRequest, NextResponse } from "next/server";

import { normalizePaymentConfig } from "@/lib/commerceCatalog";
import { findEventTicketEntry, parseEventTicketQrPayload } from "@/lib/eventTickets";
import { supabaseAdmin } from "@/lib/supabaseAdmin";

import {
  LeagueAdminApiError,
  asObject,
  asString,
  getLeagueAdminAuthScope,
} from "../../ligas/_auth";

export const runtime = "nodejs";

const EVENT_QR_READER_ROLES = new Set(["master", "master_tenant", "admin_geral", "admin_tenant", "vendas"]);

const normalizeRoleKey = (value: unknown): string =>
  asString(value)
    .trim()
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/\s+/g, " ");

const inferTicketCategory = (loteName: string): string => {
  const normalized = normalizeRoleKey(loteName);
  if (normalized.includes("nao aluno") || normalized.includes("externo")) return "Não aluno";
  if (normalized.includes("aluno")) return "Aluno";
  return "";
};

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

export async function POST(request: NextRequest) {
  try {
    const scope = await getLeagueAdminAuthScope(request);
    const hasTenantScannerRole =
      scope.isPlatformMaster ||
      (scope.tenantStatus === "approved" &&
        (EVENT_QR_READER_ROLES.has(scope.tenantRole) || EVENT_QR_READER_ROLES.has(scope.userRole)));
    const { data: readerProfileRow } = await supabaseAdmin
      .from("users")
      .select("uid,nome,turma,foto")
      .eq("uid", scope.userId)
      .maybeSingle();
    const readerProfile = asObject(readerProfileRow) ?? {};
    const readerName = asString(readerProfile.nome) || scope.userId;
    const readerTurma = asString(readerProfile.turma);
    const readerAvatar = asString(readerProfile.foto);

    const body = asObject(await request.json());
    const qrPayload = asString(body?.qrPayload);
    const selectedEventId = asString(body?.eventId);
    const manualOrderId = asString(body?.orderId);
    const manualTicketToken = asString(body?.ticketToken);
    const parsedPayload =
      parseEventTicketQrPayload(qrPayload) ||
      (manualOrderId
        ? {
            orderId: manualOrderId,
            ticketToken: manualTicketToken,
          }
        : null);
    if (!parsedPayload?.orderId) {
      throw new LeagueAdminApiError("QR code inválido para ingresso.", 400);
    }

    const { data: orderRow, error: orderError } = await supabaseAdmin
      .from("solicitacoes_ingressos")
      .select("id,tenant_id,eventoId,eventoNome,userName,userTurma,loteNome,status,payment_config,checkinAuditLog")
      .eq("id", parsedPayload.orderId)
      .maybeSingle();
    if (orderError) {
      throw new LeagueAdminApiError(orderError.message, 400);
    }

    const order = asObject(orderRow);
    if (!order) {
      throw new LeagueAdminApiError("Pedido do ingresso não encontrado.", 404);
    }

    const orderTenantId = asString(order.tenant_id);
    const eventId = asString(order.eventoId);
    if (selectedEventId && selectedEventId !== eventId) {
      throw new LeagueAdminApiError("Esse ingresso pertence a outro evento.", 400);
    }
    if (!scope.isPlatformMaster && scope.userTenantId !== orderTenantId) {
      throw new LeagueAdminApiError("Ingresso fora do tenant ativo.", 403);
    }
    if (!["aprovado", "approved", "pago", "paid", "entregue", "presente"].includes(asString(order.status).toLowerCase())) {
      throw new LeagueAdminApiError("Pagamento ainda não aprovado para check-in.", 400);
    }

    if (!hasTenantScannerRole) {
      throw new LeagueAdminApiError("Sem permissão para validar ingressos.", 403);
    }

    const paymentConfig = normalizePaymentConfig(order.payment_config);
    const ticketEntry = parsedPayload.ticketToken
      ? findEventTicketEntry(paymentConfig, parsedPayload.ticketToken)
      : paymentConfig?.ticketEntries?.find((entry) => entry.status !== "lido") ||
        paymentConfig?.ticketEntries?.[0] ||
        null;
    if (!ticketEntry || !paymentConfig?.ticketEntries) {
      throw new LeagueAdminApiError("Ingresso não encontrado para esse QR code.", 404);
    }

    if (ticketEntry.status === "transferido") {
      throw new LeagueAdminApiError("QR code inutilizado por transferência. Use o novo QR code do destinatário.", 400);
    }

    const alreadyScanned = ticketEntry.status === "lido";
    const scannedAt = new Date().toISOString();
    const auditEntry = {
      action: alreadyScanned ? "scan_repeated" : "scan",
      at: scannedAt,
      byUserId: scope.userId,
      byUserName: readerName,
      method: qrPayload ? "qr" : "manual",
      ticketToken: ticketEntry.token,
    };
    const nextTicketEntries = paymentConfig.ticketEntries.map((entry) =>
      entry.token === ticketEntry.token
        ? {
            ...entry,
            status: "lido" as const,
            scannedAt: entry.scannedAt || scannedAt,
            scannedByUserId: entry.scannedByUserId || scope.userId,
            scannedByUserName: entry.scannedByUserName || readerName,
            scannedByUserTurma: entry.scannedByUserTurma || readerTurma,
            scannedByUserAvatar: entry.scannedByUserAvatar || readerAvatar,
            scanSource: entry.scanSource || (qrPayload ? "app" : "manual"),
            checkinAuditLog: [...(Array.isArray(entry.checkinAuditLog) ? entry.checkinAuditLog : []), auditEntry].slice(-50),
          }
        : entry
    );

    const loteNome = asString(order.loteNome);
    let updatePayload: Record<string, unknown> = {
      payment_config: {
        ...paymentConfig,
        ticketEntries: nextTicketEntries,
      },
      itemType: "ingresso",
      itemName: loteNome || "Ingresso",
      itemCategory: inferTicketCategory(loteNome),
      checkinAt: scannedAt,
      checkinByUserId: scope.userId,
      checkinByUserName: readerName,
      checkinMethod: qrPayload ? "qr" : "manual",
      checkinAuditLog: [
        ...(Array.isArray(order.checkinAuditLog) ? order.checkinAuditLog : []),
        auditEntry,
      ].slice(-100),
    };
    while (Object.keys(updatePayload).length > 0) {
      const { error: updateError } = await supabaseAdmin
        .from("solicitacoes_ingressos")
        .update(updatePayload)
        .eq("id", parsedPayload.orderId);
      if (!updateError) break;

      const missingColumn = extractMissingColumn(updateError);
      if (!missingColumn || !Object.prototype.hasOwnProperty.call(updatePayload, missingColumn)) {
        throw new LeagueAdminApiError(updateError.message, 400);
      }
      const nextPayload = { ...updatePayload };
      delete nextPayload[missingColumn];
      updatePayload = nextPayload;
    }

    const updatedEntry = nextTicketEntries.find((entry) => entry.token === ticketEntry.token);
    return NextResponse.json({
      ok: true,
      alreadyScanned,
      orderId: parsedPayload.orderId,
      eventId,
      eventTitle: asString(order.eventoNome),
      holderName: asString(updatedEntry?.holderName || order.userName),
      holderTurma: asString(updatedEntry?.holderTurma || order.userTurma),
      ticketLabel: asString(updatedEntry?.label),
      scannedAt: asString(updatedEntry?.scannedAt || scannedAt),
      status: "lido",
    });
  } catch (error: unknown) {
    if (error instanceof LeagueAdminApiError) {
      return NextResponse.json({ error: error.message }, { status: error.status });
    }

    const message = error instanceof Error ? error.message : "Erro ao validar ingresso.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
