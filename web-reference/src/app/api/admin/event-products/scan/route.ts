import { NextRequest, NextResponse } from "next/server";

import { parseEventProductVoucherQrPayload } from "@/lib/qrPayloads";
import { supabaseAdmin } from "@/lib/supabaseAdmin";

import {
  LeagueAdminApiError,
  asObject,
  asString,
  getLeagueAdminAuthScope,
} from "../../ligas/_auth";

export const runtime = "nodejs";

const EVENT_QR_READER_ROLES = new Set(["master", "master_tenant", "admin_geral", "admin_tenant", "vendas"]);

const canScanEventProduct = (scope: Awaited<ReturnType<typeof getLeagueAdminAuthScope>>): boolean =>
  scope.isPlatformMaster ||
  (scope.tenantStatus === "approved" &&
    (EVENT_QR_READER_ROLES.has(scope.tenantRole) || EVENT_QR_READER_ROLES.has(scope.userRole)));

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

type VoucherEntry = {
  id: string;
  label: string;
  status: "pendente" | "ativo" | "utilizado" | "inativo" | "cancelado" | "transferido" | "estornado" | "reembolsado";
  code?: string;
  manualNumber?: string;
  usedAt: string;
  usedByUserId: string;
  usedByUserName: string;
  usedMethod: string;
  transferredToUserId?: string;
  transferredToUserName?: string;
  transferredFromUserId?: string;
  transferredFromUserName?: string;
  withdrawalCorrectionHistory?: unknown[];
};

const normalizeVoucherStatus = (
  value: unknown,
  fallback: VoucherEntry["status"]
): VoucherEntry["status"] => {
  const raw = asString(value).trim().toLowerCase();
  if (["inativo", "used", "lido", "consumido", "retirado", "utilizado"].includes(raw)) return "utilizado";
  if (["ativo", "active", "liberado", "aprovado"].includes(raw)) return "ativo";
  if (["cancelado", "canceled", "cancelled", "rejected", "rejeitado"].includes(raw)) return "cancelado";
  if (["transferido", "transferred"].includes(raw)) return "transferido";
  if (["estornado", "reembolsado", "refunded", "refund"].includes(raw)) return raw === "reembolsado" ? "reembolsado" : "estornado";
  if (["pendente", "pending", "analise"].includes(raw)) return "pendente";
  return fallback;
};

const buildVoucherEntries = (
  eventParty: Record<string, unknown>,
  quantityRaw: unknown,
  fallbackStatus: VoucherEntry["status"]
): VoucherEntry[] => {
  const rawEntries = Array.isArray(eventParty.voucherEntries)
    ? eventParty.voucherEntries
    : Array.isArray(eventParty.vouchers)
      ? eventParty.vouchers
      : [];
  const quantity = Math.max(1, Math.floor(Number(quantityRaw || rawEntries.length || 1) || 1));
  const legacyStatus = normalizeVoucherStatus(eventParty.voucherStatus, fallbackStatus);
  const entries = rawEntries
    .map((entry, index): VoucherEntry | null => {
      const row = asObject(entry);
      if (!row) return null;
      const id = asString(row.id || row.voucherId || row.token).trim() || `item-${index + 1}`;
      const entryStatus = normalizeVoucherStatus(row.status, legacyStatus);
      const manualNumber = asString(row.manualNumber || row.fichaNumero || row.numeroFicha);
      const rawLabel = asString(row.label).trim();
      return {
        id,
        label:
          manualNumber
            ? `Ficha ${manualNumber}`
            : rawLabel && !/^ficha\s+\d+$/i.test(rawLabel)
              ? rawLabel
              : "Ficha digital",
        status: entryStatus === "pendente" && legacyStatus === "ativo" ? "ativo" : entryStatus,
        code: asString(row.code || row.codigo || row.manualCode || row.manualNumber),
        manualNumber,
        usedAt: asString(row.usedAt || row.withdrawalAt || row.scannedAt),
        usedByUserId: asString(row.usedByUserId || row.withdrawalByUserId || row.scannedByUserId),
        usedByUserName: asString(row.usedByUserName || row.withdrawalByUserName || row.scannedByUserName),
        usedMethod: asString(row.usedMethod || row.withdrawalMethod || row.scanSource),
        transferredToUserId: asString(row.transferredToUserId),
        transferredToUserName: asString(row.transferredToUserName),
        transferredFromUserId: asString(row.transferredFromUserId),
        transferredFromUserName: asString(row.transferredFromUserName),
        withdrawalCorrectionHistory: Array.isArray(row.withdrawalCorrectionHistory)
          ? row.withdrawalCorrectionHistory
          : [],
      };
    })
    .filter((entry): entry is VoucherEntry => Boolean(entry));
  const seen = new Set(entries.map((entry) => entry.id));
  for (let index = 0; entries.length < quantity; index += 1) {
    const id = `item-${index + 1}`;
    if (seen.has(id)) continue;
    seen.add(id);
    entries.push({
      id,
      label: "Ficha digital",
      status: legacyStatus,
      code: `${asString(eventParty.manualCode || eventParty.externalNumber) || "FICHA"}-${index + 1}`,
      manualNumber: "",
      usedAt: "",
      usedByUserId: "",
      usedByUserName: "",
      usedMethod: "",
      withdrawalCorrectionHistory: [],
    });
  }
  return entries.slice(0, quantity);
};

const updateOrderWithSchemaFallback = async (
  orderId: string,
  payload: Record<string, unknown>
): Promise<void> => {
  let updatePayload = { ...payload };
  while (Object.keys(updatePayload).length > 0) {
    const { error } = await supabaseAdmin
      .from("orders")
      .update(updatePayload)
      .eq("id", orderId);
    if (!error) return;

    const missingColumn = extractMissingColumn(error);
    if (!missingColumn || !Object.prototype.hasOwnProperty.call(updatePayload, missingColumn)) {
      throw new LeagueAdminApiError(error.message, 400);
    }
    const nextPayload = { ...updatePayload };
    delete nextPayload[missingColumn];
    updatePayload = nextPayload;
  }
};

export async function POST(request: NextRequest) {
  try {
    const scope = await getLeagueAdminAuthScope(request);
    if (!canScanEventProduct(scope)) {
      throw new LeagueAdminApiError("Sem permissão para validar fichas de evento.", 403);
    }

    const body = asObject(await request.json());
    const qrPayload = asString(body?.qrPayload);
    const manualOrderId = asString(body?.orderId);
    const parsed =
      parseEventProductVoucherQrPayload(qrPayload) ||
      (manualOrderId
        ? {
            orderId: manualOrderId,
            eventId: asString(body?.eventId),
            productId: asString(body?.productId),
            voucherId: asString(body?.voucherId),
          }
        : null);
    if (!parsed?.orderId) {
      throw new LeagueAdminApiError("QR code inválido para ficha de produto.", 400);
    }

    const { data: orderRow, error: orderError } = await supabaseAdmin
      .from("orders")
      .select("id,tenant_id,userId,userName,productId,productName,status,data,quantidade")
      .eq("id", parsed.orderId)
      .maybeSingle();
    if (orderError) throw new LeagueAdminApiError(orderError.message, 400);

    const order = asObject(orderRow);
    if (!order) {
      throw new LeagueAdminApiError("Pedido da ficha não encontrado.", 404);
    }

    const orderTenantId = asString(order.tenant_id);
    if (!scope.isPlatformMaster && scope.userTenantId !== orderTenantId) {
      throw new LeagueAdminApiError("Ficha fora do tenant ativo.", 403);
    }

    const status = asString(order.status).trim().toLowerCase();
    if (!["approved", "aprovado", "paid", "pago", "delivered"].includes(status)) {
      throw new LeagueAdminApiError("Pagamento ainda não aprovado para esta ficha.", 400);
    }

    const orderData = asObject(order.data) ?? {};
    const eventParty = asObject(orderData.eventParty) ?? {};
    const parsedEventId = asString(parsed.eventId) || asString(eventParty.eventId);
    const parsedProductId = asString(parsed.productId) || asString(order.productId);
    if (!parsedEventId || !parsedProductId) {
      throw new LeagueAdminApiError("Ficha sem evento ou produto vinculado.", 400);
    }
    if (asString(eventParty.eventId) !== parsedEventId || asString(order.productId) !== parsedProductId) {
      throw new LeagueAdminApiError("Essa ficha pertence a outro evento ou produto.", 400);
    }

    const { data: readerProfileRow } = await supabaseAdmin
      .from("users")
      .select("uid,nome,turma,foto")
      .eq("uid", scope.userId)
      .maybeSingle();
    const readerProfile = asObject(readerProfileRow) ?? {};
    const readerName = asString(readerProfile.nome) || scope.userId;
    const method = qrPayload ? "qr" : "manual";
    const voucherEntries = buildVoucherEntries(eventParty, order.quantidade, "ativo");
    const requestedVoucherId = asString(parsed.voucherId);
    const targetVoucher =
      (requestedVoucherId
        ? voucherEntries.find((entry) => entry.id === requestedVoucherId)
        : null) ||
      voucherEntries.find((entry) => entry.status === "ativo") ||
      voucherEntries[0];
    if (!targetVoucher) {
      throw new LeagueAdminApiError("Ficha não encontrada para retirada.", 404);
    }

    if (targetVoucher.status !== "ativo" && targetVoucher.status !== "utilizado" && targetVoucher.status !== "inativo") {
      throw new LeagueAdminApiError(`Ficha com status ${targetVoucher.status}. Apenas fichas ativas podem ser retiradas.`, 400);
    }

    const alreadyScanned = targetVoucher.status === "utilizado" || targetVoucher.status === "inativo";
    const usedAt = alreadyScanned && targetVoucher.usedAt ? targetVoucher.usedAt : new Date().toISOString();

    if (!alreadyScanned) {
      const nextVoucherEntries = voucherEntries.map((entry) =>
        entry.id === targetVoucher.id
          ? {
              ...entry,
              status: "utilizado" as const,
              usedAt,
              usedByUserId: scope.userId,
              usedByUserName: readerName,
              usedMethod: method,
              withdrawalAt: usedAt,
              withdrawalByUserId: scope.userId,
              withdrawalByUserName: readerName,
              withdrawalMethod: method,
            }
          : entry
      );
      const usedCount = nextVoucherEntries.filter((entry) => entry.status === "utilizado" || entry.status === "inativo").length;
      const nextVoucherStatus =
        usedCount >= nextVoucherEntries.length ? "utilizado" : usedCount > 0 ? "parcial" : "ativo";

      const updatePayload: Record<string, unknown> = {
        data: {
          ...orderData,
          eventParty: {
            ...eventParty,
            voucherStatus: nextVoucherStatus,
            voucherEntries: nextVoucherEntries,
            usedAt,
            usedByUserId: scope.userId,
            usedByUserName: readerName,
            usedMethod: method,
            withdrawalAt: usedAt,
            withdrawalByUserId: scope.userId,
            withdrawalByUserName: readerName,
            withdrawalMethod: method,
          },
        },
        eventId: parsedEventId,
        eventItemType: "produto",
        eventItemName: asString(order.productName) || "Produto",
        eventLoteNome: "-",
        eventItemCategory: asString(eventParty.section) || "Geral",
        eventCheckinAt: usedAt,
        eventCheckinByUserId: scope.userId,
        eventCheckinByUserName: readerName,
        eventCheckinMethod: method,
        updatedAt: new Date().toISOString(),
      };
      await updateOrderWithSchemaFallback(parsed.orderId, updatePayload);
    }

    return NextResponse.json({
      ok: true,
      alreadyScanned,
      orderId: parsed.orderId,
      eventId: parsedEventId,
      productId: parsedProductId,
      productName: asString(order.productName),
      holderName: asString(order.userName),
      scannedAt: usedAt,
      voucherId: targetVoucher.id,
      voucherLabel: targetVoucher.label,
      status: "utilizado",
    });
  } catch (error: unknown) {
    if (error instanceof LeagueAdminApiError) {
      return NextResponse.json({ error: error.message }, { status: error.status });
    }

    const message = error instanceof Error ? error.message : "Erro ao validar ficha.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}

export async function PATCH(request: NextRequest) {
  try {
    const scope = await getLeagueAdminAuthScope(request);
    if (!canScanEventProduct(scope)) {
      throw new LeagueAdminApiError("Sem permissão para corrigir retirada de ficha.", 403);
    }

    const body = asObject(await request.json());
    const orderId = asString(body?.orderId).trim();
    const eventId = asString(body?.eventId).trim();
    const productId = asString(body?.productId).trim();
    const voucherId = asString(body?.voucherId).trim();
    const note = asString(body?.note).trim().slice(0, 240);
    if (!orderId || !voucherId) {
      throw new LeagueAdminApiError("Pedido ou ficha inválidos para correção.", 400);
    }

    const { data: orderRow, error: orderError } = await supabaseAdmin
      .from("orders")
      .select("id,tenant_id,userId,userName,productId,productName,status,data,quantidade")
      .eq("id", orderId)
      .maybeSingle();
    if (orderError) throw new LeagueAdminApiError(orderError.message, 400);

    const order = asObject(orderRow);
    if (!order) {
      throw new LeagueAdminApiError("Pedido da ficha não encontrado.", 404);
    }

    const orderTenantId = asString(order.tenant_id);
    if (!scope.isPlatformMaster && scope.userTenantId !== orderTenantId) {
      throw new LeagueAdminApiError("Ficha fora do tenant ativo.", 403);
    }

    const orderData = asObject(order.data) ?? {};
    const eventParty = asObject(orderData.eventParty) ?? {};
    const parsedEventId = eventId || asString(eventParty.eventId);
    const parsedProductId = productId || asString(order.productId);
    if (!parsedEventId || !parsedProductId) {
      throw new LeagueAdminApiError("Ficha sem evento ou produto vinculado.", 400);
    }
    if (asString(eventParty.eventId) !== parsedEventId || asString(order.productId) !== parsedProductId) {
      throw new LeagueAdminApiError("Essa ficha pertence a outro evento ou produto.", 400);
    }

    const { data: readerProfileRow } = await supabaseAdmin
      .from("users")
      .select("uid,nome,turma,foto")
      .eq("uid", scope.userId)
      .maybeSingle();
    const readerProfile = asObject(readerProfileRow) ?? {};
    const readerName = asString(readerProfile.nome) || scope.userId;
    const normalizeOperator = (value: unknown): string =>
      asString(value)
        .trim()
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .toLowerCase();

    const voucherEntries = buildVoucherEntries(eventParty, order.quantidade, "ativo");
    const targetVoucher = voucherEntries.find((entry) => entry.id === voucherId);
    if (!targetVoucher) {
      throw new LeagueAdminApiError("Ficha não encontrada para correção.", 404);
    }

    if (targetVoucher.status !== "utilizado" && targetVoucher.status !== "inativo") {
      throw new LeagueAdminApiError("Apenas fichas já retiradas podem ser corrigidas.", 400);
    }

    const sameOperatorById = Boolean(targetVoucher.usedByUserId && targetVoucher.usedByUserId === scope.userId);
    const sameOperatorByName =
      !targetVoucher.usedByUserId &&
      Boolean(targetVoucher.usedByUserName) &&
      normalizeOperator(targetVoucher.usedByUserName) === normalizeOperator(readerName);
    if (sameOperatorById || sameOperatorByName) {
      throw new LeagueAdminApiError(
        "A correção precisa ser feita por outro operador. Quem marcou como retirado não pode desfazer a própria baixa.",
        403
      );
    }

    const now = new Date().toISOString();
    const correctionRecord = {
      action: "withdrawal_correction",
      correctedAt: now,
      correctedByUserId: scope.userId,
      correctedByUserName: readerName,
      note: note || "Correção de retirada",
      previousStatus: targetVoucher.status,
      previousUsedAt: targetVoucher.usedAt,
      previousUsedByUserId: targetVoucher.usedByUserId,
      previousUsedByUserName: targetVoucher.usedByUserName,
      previousUsedMethod: targetVoucher.usedMethod,
    };

    const nextVoucherEntries = voucherEntries.map((entry) => {
      if (entry.id !== targetVoucher.id) return entry;
      return {
        ...entry,
        status: "ativo" as const,
        usedAt: "",
        usedByUserId: "",
        usedByUserName: "",
        usedMethod: "",
        withdrawalAt: "",
        withdrawalByUserId: "",
        withdrawalByUserName: "",
        withdrawalMethod: "",
        withdrawalCorrectedAt: now,
        withdrawalCorrectedByUserId: scope.userId,
        withdrawalCorrectedByUserName: readerName,
        withdrawalCorrectionNote: note || "Correção de retirada",
        withdrawalCorrectionHistory: [
          ...(Array.isArray(entry.withdrawalCorrectionHistory) ? entry.withdrawalCorrectionHistory : []),
          correctionRecord,
        ].slice(-25),
      };
    });
    const usedEntries = nextVoucherEntries
      .filter((entry) => entry.status === "utilizado" || entry.status === "inativo")
      .sort((left, right) => {
        const leftTime = left.usedAt ? new Date(left.usedAt).getTime() : 0;
        const rightTime = right.usedAt ? new Date(right.usedAt).getTime() : 0;
        return rightTime - leftTime;
      });
    const latestUsed = usedEntries[0] ?? null;
    const nextVoucherStatus =
      usedEntries.length >= nextVoucherEntries.length
        ? "utilizado"
        : usedEntries.length > 0
          ? "parcial"
          : "ativo";
    const eventCorrectionHistory = Array.isArray(eventParty.withdrawalCorrectionHistory)
      ? eventParty.withdrawalCorrectionHistory
      : [];

    await updateOrderWithSchemaFallback(orderId, {
      status: latestUsed ? "delivered" : "approved",
      data: {
        ...orderData,
        eventParty: {
          ...eventParty,
          voucherStatus: nextVoucherStatus,
          voucherEntries: nextVoucherEntries,
          usedAt: latestUsed?.usedAt || "",
          usedByUserId: latestUsed?.usedByUserId || "",
          usedByUserName: latestUsed?.usedByUserName || "",
          usedMethod: latestUsed?.usedMethod || "",
          withdrawalAt: latestUsed?.usedAt || "",
          withdrawalByUserId: latestUsed?.usedByUserId || "",
          withdrawalByUserName: latestUsed?.usedByUserName || "",
          withdrawalMethod: latestUsed?.usedMethod || "",
          withdrawalCorrectionHistory: [...eventCorrectionHistory, correctionRecord].slice(-50),
        },
      },
      eventId: parsedEventId,
      eventItemType: "produto",
      eventItemName: asString(order.productName) || "Produto",
      eventLoteNome: "-",
      eventItemCategory: asString(eventParty.section) || "Geral",
      eventCheckinAt: latestUsed?.usedAt || null,
      eventCheckinByUserId: latestUsed?.usedByUserId || "",
      eventCheckinByUserName: latestUsed?.usedByUserName || "",
      eventCheckinMethod: latestUsed?.usedMethod || "",
      updatedAt: now,
    });

    return NextResponse.json({
      ok: true,
      corrected: true,
      orderId,
      eventId: parsedEventId,
      productId: parsedProductId,
      voucherId: targetVoucher.id,
      voucherLabel: targetVoucher.label,
      correctedAt: now,
      correctedByUserName: readerName,
      status: "ativo",
    });
  } catch (error: unknown) {
    if (error instanceof LeagueAdminApiError) {
      return NextResponse.json({ error: error.message }, { status: error.status });
    }

    const message = error instanceof Error ? error.message : "Erro ao corrigir retirada.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
