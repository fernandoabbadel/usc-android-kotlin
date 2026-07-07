import { randomUUID } from "node:crypto";
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

const EVENT_GATE_ROLES = new Set(["master", "master_tenant", "admin_geral", "admin_tenant", "vendas"]);

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

const parseCurrency = (value: unknown): number => {
  if (typeof value === "number" && Number.isFinite(value)) return Math.max(0, value);
  if (typeof value !== "string") return 0;
  const parsed = Number.parseFloat(value.replace(/[^\d,.-]/g, "").replace(/\./g, "").replace(",", "."));
  return Number.isFinite(parsed) ? Math.max(0, parsed) : 0;
};

const formatCurrencyInput = (value: unknown): string =>
  parseCurrency(value).toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

const normalizeDigits = (value: string): string => value.replace(/\D/g, "");

const isValidOptionalEmail = (value: string): boolean =>
  !value || /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim().toLowerCase());

const hasValidOptionalPhone = (value: string): boolean => {
  if (!value) return true;
  const digits = normalizeDigits(value);
  return digits.length >= 10 && digits.length <= 15;
};

const normalizeStatus = (value: string): string =>
  value.trim().normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase();

const isApprovedStatus = (status: string): boolean =>
  ["aprovado", "approved", "pago", "paid", "delivered"].includes(normalizeStatus(status));

const compactIdPart = (value: string): string =>
  value.trim().normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/[^a-zA-Z0-9]+/g, "-").replace(/^-|-$/g, "").slice(0, 48);

const createManualUserId = (braceletNumber: string): string =>
  `manual-porta-${compactIdPart(braceletNumber) || Date.now()}-${randomUUID().slice(0, 8)}`;

const updateWithColumnFallback = async (
  table: string,
  payload: Record<string, unknown>,
  apply: (nextPayload: Record<string, unknown>) => Promise<{ error: unknown }>
) => {
  let mutablePayload = { ...payload };
  while (Object.keys(mutablePayload).length > 0) {
    const { error } = await apply(mutablePayload);
    if (!error) return;

    const missingColumn = extractMissingColumn(error);
    if (!missingColumn || !Object.prototype.hasOwnProperty.call(mutablePayload, missingColumn)) {
      const message = error instanceof Error ? error.message : `Erro ao gravar em ${table}.`;
      throw new LeagueAdminApiError(message, 400);
    }
    const nextPayload = { ...mutablePayload };
    delete nextPayload[missingColumn];
    mutablePayload = nextPayload;
  }
};

export async function POST(request: NextRequest) {
  try {
    const scope = await getLeagueAdminAuthScope(request);
    const hasGateRole =
      scope.isPlatformMaster ||
      (scope.tenantStatus === "approved" &&
        (EVENT_GATE_ROLES.has(scope.tenantRole) || EVENT_GATE_ROLES.has(scope.userRole)));
    if (!hasGateRole) {
      throw new LeagueAdminApiError("Sem permissão para cadastrar entrada manual.", 403);
    }

    const { data: operatorRow } = await supabaseAdmin
      .from("users")
      .select("uid,nome,turma,foto")
      .eq("uid", scope.userId)
      .maybeSingle();
    const operator = asObject(operatorRow) ?? {};
    const operatorName = asString(operator.nome) || scope.userId || "Admin";
    const operatorTurma = asString(operator.turma);
    const operatorAvatar = asString(operator.foto);

    const body = asObject(await request.json());
    const eventId = asString(body?.eventId);
    const holderName = asString(body?.holderName || body?.nome).slice(0, 120);
    const holderTurma = asString(body?.holderTurma || body?.turma).slice(0, 120);
    const braceletNumber = asString(body?.braceletNumber || body?.pulseira || body?.numero).slice(0, 80);
    const cpf = normalizeDigits(asString(body?.cpf)).slice(0, 11);
    const telefone = asString(body?.telefone).slice(0, 30);
    const email = asString(body?.email).trim().slice(0, 160);
    const ra = asString(body?.ra).slice(0, 80);
    const sourceOrderId = asString(body?.sourceOrderId);
    const sourceTicketToken = asString(body?.sourceTicketToken);

    if (!eventId) throw new LeagueAdminApiError("Evento inválido.", 400);
    if (!holderName) throw new LeagueAdminApiError("Informe o nome do participante.", 400);
    if (!braceletNumber) throw new LeagueAdminApiError("Informe o número da pulseira.", 400);
    if (cpf && cpf.length !== 11) throw new LeagueAdminApiError("CPF inválido.", 400);
    if (!hasValidOptionalPhone(telefone)) throw new LeagueAdminApiError("Telefone inválido.", 400);
    if (!isValidOptionalEmail(email)) throw new LeagueAdminApiError("E-mail inválido.", 400);

    const { data: eventRow, error: eventError } = await supabaseAdmin
      .from("eventos")
      .select("id,titulo,payment_config,tenant_id")
      .eq("id", eventId)
      .maybeSingle();
    if (eventError) throw new LeagueAdminApiError(eventError.message, 400);
    const event = asObject(eventRow);
    if (!event) throw new LeagueAdminApiError("Evento não encontrado.", 404);
    if (!scope.isPlatformMaster && scope.userTenantId !== asString(event.tenant_id)) {
      throw new LeagueAdminApiError("Evento fora do tenant ativo.", 403);
    }

    const now = new Date().toISOString();
    const manualUserId = createManualUserId(braceletNumber);
    const newOrderId = randomUUID();
    const newToken = randomUUID();
    const isTransfer = Boolean(sourceOrderId);
    let sourceOrder: Record<string, unknown> = {};
    let sourceEntry: CommerceTicketEntry | null = null;
    let sourcePaymentConfig = normalizePaymentConfig(null);
    let sourceHolderName = "";
    let sourceHolderId = "";
    let sourceLoteId: unknown = "entrada-porta";
    let sourceLoteNome = "Entrada/porta";

    if (isTransfer) {
      const { data: sourceOrderRow, error: sourceOrderError } = await supabaseAdmin
        .from("solicitacoes_ingressos")
        .select("*")
        .eq("id", sourceOrderId)
        .maybeSingle();
      if (sourceOrderError) throw new LeagueAdminApiError(sourceOrderError.message, 400);
      sourceOrder = asObject(sourceOrderRow) ?? {};
      if (!asString(sourceOrder.id)) throw new LeagueAdminApiError("Pedido original não encontrado.", 404);
      if (asString(sourceOrder.eventoId) !== eventId) {
        throw new LeagueAdminApiError("Pedido original pertence a outro evento.", 400);
      }
      if (!scope.isPlatformMaster && scope.userTenantId !== asString(sourceOrder.tenant_id)) {
        throw new LeagueAdminApiError("Pedido original fora do tenant ativo.", 403);
      }
      if (!isApprovedStatus(asString(sourceOrder.status))) {
        throw new LeagueAdminApiError("Só é possível transferir ingresso com pagamento aprovado.", 400);
      }

      sourcePaymentConfig = normalizePaymentConfig(sourceOrder.payment_config);
      const entries = sourcePaymentConfig?.ticketEntries || [];
      sourceEntry =
        (sourceTicketToken ? entries.find((entry) => entry.token === sourceTicketToken) ?? null : null) ||
        entries.find((entry) => entry.status === "ativo") ||
        entries.find((entry) => entry.status !== "transferido") ||
        null;
      if (!sourceEntry || sourceEntry.status !== "ativo") {
        throw new LeagueAdminApiError("Nenhum ingresso ativo disponível para transferência manual.", 400);
      }
      sourceHolderName = sourceEntry.holderName || asString(sourceOrder.userName) || "Participante";
      sourceHolderId = asString(sourceOrder.userId);
      sourceLoteId = sourceOrder.loteId ?? "entrada-porta";
      sourceLoteNome = sourceEntry.loteName || asString(sourceOrder.loteNome) || "Ingresso";
    }

    const loteNome = isTransfer ? sourceLoteNome : "Entrada/porta";
    const valorUnitario = isTransfer ? "0,00" : formatCurrencyInput(body?.valorPorta ?? body?.valor);
    const valorTotal = valorUnitario;
    const transferAudit = isTransfer
      ? {
          at: now,
          fromOrderId: sourceOrderId,
          fromUserId: sourceHolderId,
          fromUserName: sourceHolderName,
          toOrderId: newOrderId,
          toUserId: manualUserId,
          toUserName: holderName,
          ticketToken: sourceEntry?.token,
          byUserId: scope.userId,
          byUserName: operatorName,
          manual: true,
          braceletNumber,
        }
      : null;

    if (isTransfer && sourcePaymentConfig && sourceEntry && transferAudit) {
      const nextSourceEntries = sourcePaymentConfig.ticketEntries?.map((entry) =>
        entry.token === sourceEntry?.token
          ? {
              ...entry,
              status: "transferido" as const,
              transferredAt: now,
              transferredToUserId: manualUserId,
              transferredToUserName: holderName,
              transferByUserId: scope.userId,
              transferByUserName: operatorName,
              transferHistory: [
                ...(Array.isArray(entry.transferHistory) ? entry.transferHistory : []),
                transferAudit,
              ].slice(-50),
            }
          : entry
      ) ?? [];
      const sourceUpdatePayload = {
        payment_config: {
          ...sourcePaymentConfig,
          ticketEntries: nextSourceEntries,
        },
        transferAt: now,
        transferFromUserId: sourceHolderId,
        transferFromUserName: sourceHolderName,
        transferToUserId: manualUserId,
        transferToUserName: holderName,
        transferByUserId: scope.userId,
        transferByUserName: operatorName,
        transferHistory: [
          ...(Array.isArray(sourceOrder.transferHistory) ? sourceOrder.transferHistory : []),
          transferAudit,
        ].slice(-100),
      };
      await updateWithColumnFallback("solicitacoes_ingressos", sourceUpdatePayload, async (nextPayload) =>
        supabaseAdmin.from("solicitacoes_ingressos").update(nextPayload).eq("id", sourceOrderId)
      );
    }

    const ticketEntry: CommerceTicketEntry = {
      id: `${newOrderId}:1`,
      token: newToken,
      label: braceletNumber ? `Pulseira ${braceletNumber}` : "Entrada 1",
      unitIndex: 1,
      status: "lido",
      orderId: newOrderId,
      orderCode: newOrderId.slice(0, 8).toUpperCase(),
      eventId,
      eventTitle: asString(event.titulo) || "Evento",
      loteName: loteNome,
      holderName,
      holderTurma: holderTurma || "Porta",
      scannedAt: now,
      scannedByUserId: scope.userId,
      scannedByUserName: operatorName,
      ...(operatorTurma ? { scannedByUserTurma: operatorTurma } : {}),
      ...(operatorAvatar ? { scannedByUserAvatar: operatorAvatar } : {}),
      scanSource: "manual",
      checkinNote: isTransfer ? "Transferência manual na entrada" : "Cadastro manual na entrada",
      checkinEditedAt: now,
      checkinEditedByUserId: scope.userId,
      checkinEditedByUserName: operatorName,
      checkinAuditLog: [
        {
          action: isTransfer ? "manual_transfer_checkin" : "manual_gate_entry",
          at: now,
          byUserId: scope.userId,
          byUserName: operatorName,
          braceletNumber,
        },
      ],
      ...(transferAudit
        ? {
            transferredAt: now,
            transferredFromUserId: sourceHolderId,
            transferredFromUserName: sourceHolderName,
            transferByUserId: scope.userId,
            transferByUserName: operatorName,
            transferHistory: [transferAudit],
          }
        : {}),
    };

    const basePaymentConfig = normalizePaymentConfig(event.payment_config) ?? { chave: "", banco: "", titular: "" };
    const newPaymentConfig = {
      chave: basePaymentConfig.chave,
      banco: basePaymentConfig.banco,
      titular: basePaymentConfig.titular,
      ...(basePaymentConfig.whatsapp ? { whatsapp: basePaymentConfig.whatsapp } : {}),
      ...(basePaymentConfig.recipient ? { recipient: basePaymentConfig.recipient } : {}),
      ...(basePaymentConfig.recipients?.length ? { recipients: basePaymentConfig.recipients } : {}),
      ticketEntries: [ticketEntry],
    };

    const insertPayload = {
      id: newOrderId,
      tenant_id: event.tenant_id ?? null,
      userId: manualUserId,
      userName: holderName,
      userTurma: holderTurma || "Porta",
      status: "aprovado",
      eventoId: eventId,
      eventoNome: asString(event.titulo) || "Evento",
      loteId: sourceLoteId,
      loteNome,
      quantidade: 1,
      valorUnitario,
      valorTotal,
      metodo: "manual",
      dataSolicitacao: now,
      dataAprovacao: now,
      dataPagamento: now,
      paymentDate: now,
      paidAt: now,
      aprovadoPor: operatorName,
      itemType: "ingresso",
      itemName: loteNome,
      itemCategory: isTransfer ? "Transferência manual" : "Entrada/porta",
      approvalMethod: isTransfer ? "transferencia_manual_porta" : "manual_porta",
      checkinAt: now,
      checkinByUserId: scope.userId,
      checkinByUserName: operatorName,
      checkinMethod: "manual",
      checkinNote: isTransfer ? "Transferência manual na entrada" : "Cadastro manual na entrada",
      checkinEditedAt: now,
      checkinEditedByUserId: scope.userId,
      checkinEditedByUserName: operatorName,
      checkinAuditLog: ticketEntry.checkinAuditLog,
      ...(transferAudit
        ? {
            transferAt: now,
            transferFromUserId: sourceHolderId,
            transferFromUserName: sourceHolderName,
            transferToUserId: manualUserId,
            transferToUserName: holderName,
            transferByUserId: scope.userId,
            transferByUserName: operatorName,
            transferHistory: [transferAudit],
          }
        : {}),
      discountValue: "R$ 0,00",
      discountKind: "",
      discountSource: "",
      payment_config: newPaymentConfig,
      data: {
        manualGateEntry: true,
        manualTransfer: isTransfer,
        manualUserId,
        pulseira: braceletNumber,
        braceletNumber,
        valorPorta: valorUnitario,
        cpf,
        telefone,
        email,
        ra,
        dataPagamento: now,
        paymentDate: now,
        checkinAt: now,
        approvedAt: now,
        createdByUserId: scope.userId,
        createdByName: operatorName,
        ...(transferAudit ? { transferAudit } : {}),
      },
      createdAt: now,
    };

    await updateWithColumnFallback("solicitacoes_ingressos", insertPayload, async (nextPayload) =>
      supabaseAdmin.from("solicitacoes_ingressos").insert(nextPayload)
    );

    return NextResponse.json({
      ok: true,
      orderId: newOrderId,
      ticketToken: newToken,
      transferred: isTransfer,
    });
  } catch (error: unknown) {
    if (error instanceof LeagueAdminApiError) {
      return NextResponse.json({ error: error.message }, { status: error.status });
    }
    const message = error instanceof Error ? error.message : "Erro ao cadastrar entrada manual.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}

export async function PATCH(request: NextRequest) {
  try {
    const scope = await getLeagueAdminAuthScope(request);
    const hasGateRole =
      scope.isPlatformMaster ||
      (scope.tenantStatus === "approved" &&
        (EVENT_GATE_ROLES.has(scope.tenantRole) || EVENT_GATE_ROLES.has(scope.userRole)));
    if (!hasGateRole) {
      throw new LeagueAdminApiError("Sem permissão para editar entrada manual.", 403);
    }

    const { data: operatorRow } = await supabaseAdmin
      .from("users")
      .select("uid,nome")
      .eq("uid", scope.userId)
      .maybeSingle();
    const operator = asObject(operatorRow) ?? {};
    const operatorName = asString(operator.nome) || scope.userId || "Admin";

    const body = asObject(await request.json());
    const eventId = asString(body?.eventId);
    const orderId = asString(body?.orderId);
    const holderName = asString(body?.holderName || body?.nome).trim().slice(0, 120);
    const holderTurma = asString(body?.holderTurma || body?.turma).trim().slice(0, 120);
    const braceletNumber = asString(body?.braceletNumber || body?.pulseira || body?.numero).trim().slice(0, 80);
    const cpf = normalizeDigits(asString(body?.cpf)).slice(0, 11);
    const telefone = asString(body?.telefone).trim().slice(0, 30);
    const email = asString(body?.email).trim().slice(0, 160);
    const ra = asString(body?.ra).trim().slice(0, 80);
    const valorPorta = formatCurrencyInput(body?.valorPorta ?? body?.valor);

    if (!eventId || !orderId) throw new LeagueAdminApiError("Evento ou pedido inválido.", 400);
    if (!holderName) throw new LeagueAdminApiError("Informe o nome do participante.", 400);
    if (!braceletNumber) throw new LeagueAdminApiError("Informe o número da pulseira.", 400);
    if (cpf && cpf.length !== 11) throw new LeagueAdminApiError("CPF inválido.", 400);
    if (!hasValidOptionalPhone(telefone)) throw new LeagueAdminApiError("Telefone inválido.", 400);
    if (!isValidOptionalEmail(email)) throw new LeagueAdminApiError("E-mail inválido.", 400);

    const { data: orderRow, error: orderError } = await supabaseAdmin
      .from("solicitacoes_ingressos")
      .select("*")
      .eq("id", orderId)
      .maybeSingle();
    if (orderError) throw new LeagueAdminApiError(orderError.message, 400);

    const order = asObject(orderRow);
    if (!order) throw new LeagueAdminApiError("Entrada manual não encontrada.", 404);
    if (asString(order.eventoId) !== eventId) {
      throw new LeagueAdminApiError("Entrada pertence a outro evento.", 400);
    }
    if (!scope.isPlatformMaster && scope.userTenantId !== asString(order.tenant_id)) {
      throw new LeagueAdminApiError("Entrada fora do tenant ativo.", 403);
    }

    const currentData = asObject(order.data) ?? {};
    if (!currentData.manualGateEntry) {
      throw new LeagueAdminApiError("Só cadastros manuais podem ser editados por aqui.", 400);
    }

    const now = new Date().toISOString();
    const isTransfer = Boolean(currentData.manualTransfer);
    const paymentConfig = normalizePaymentConfig(order.payment_config) ?? { chave: "", banco: "", titular: "" };
    const nextTicketEntries = (paymentConfig.ticketEntries ?? []).map((entry, index) => {
      if (entry.orderId && entry.orderId !== orderId) return entry;
      if (!entry.orderId && index > 0) return entry;
      return {
        ...entry,
        label: braceletNumber ? `Pulseira ${braceletNumber}` : entry.label,
        holderName,
        holderTurma: holderTurma || "Porta",
        checkinEditedAt: now,
        checkinEditedByUserId: scope.userId,
        checkinEditedByUserName: operatorName,
      };
    });

    const nextData = {
      ...currentData,
      pulseira: braceletNumber,
      braceletNumber,
      valorPorta: isTransfer ? asString(currentData.valorPorta) || asString(order.valorUnitario) : valorPorta,
      cpf,
      telefone,
      email,
      ra,
      editedAt: now,
      editedByUserId: scope.userId,
      editedByName: operatorName,
    };

    const updatePayload: Record<string, unknown> = {
      userName: holderName,
      userTurma: holderTurma || "Porta",
      payment_config: {
        ...paymentConfig,
        ticketEntries: nextTicketEntries,
      },
      data: nextData,
      checkinEditedAt: now,
      checkinEditedByUserId: scope.userId,
      checkinEditedByUserName: operatorName,
    };

    if (!isTransfer) {
      updatePayload.loteId = "entrada-porta";
      updatePayload.loteNome = "Entrada/porta";
      updatePayload.valorUnitario = valorPorta;
      updatePayload.valorTotal = valorPorta;
      updatePayload.itemName = "Entrada/porta";
      updatePayload.itemCategory = "Entrada/porta";
    } else {
      updatePayload.transferToUserName = holderName;
    }

    await updateWithColumnFallback("solicitacoes_ingressos", updatePayload, async (nextPayload) =>
      supabaseAdmin.from("solicitacoes_ingressos").update(nextPayload).eq("id", orderId)
    );

    return NextResponse.json({ ok: true, orderId });
  } catch (error: unknown) {
    if (error instanceof LeagueAdminApiError) {
      return NextResponse.json({ error: error.message }, { status: error.status });
    }
    const message = error instanceof Error ? error.message : "Erro ao editar entrada manual.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}

export async function DELETE(request: NextRequest) {
  try {
    const scope = await getLeagueAdminAuthScope(request);
    const hasGateRole =
      scope.isPlatformMaster ||
      (scope.tenantStatus === "approved" &&
        (EVENT_GATE_ROLES.has(scope.tenantRole) || EVENT_GATE_ROLES.has(scope.userRole)));
    if (!hasGateRole) {
      throw new LeagueAdminApiError("Sem permissão para excluir entrada manual.", 403);
    }

    const { data: operatorRow } = await supabaseAdmin
      .from("users")
      .select("uid,nome")
      .eq("uid", scope.userId)
      .maybeSingle();
    const operator = asObject(operatorRow) ?? {};
    const operatorName = asString(operator.nome) || scope.userId || "Admin";

    const body = asObject(await request.json());
    const eventId = asString(body?.eventId);
    const orderId = asString(body?.orderId);
    if (!eventId || !orderId) throw new LeagueAdminApiError("Evento ou pedido inválido.", 400);

    const { data: orderRow, error: orderError } = await supabaseAdmin
      .from("solicitacoes_ingressos")
      .select("*")
      .eq("id", orderId)
      .maybeSingle();
    if (orderError) throw new LeagueAdminApiError(orderError.message, 400);

    const order = asObject(orderRow);
    if (!order) throw new LeagueAdminApiError("Entrada manual não encontrada.", 404);
    if (asString(order.eventoId) !== eventId) {
      throw new LeagueAdminApiError("Entrada pertence a outro evento.", 400);
    }
    if (!scope.isPlatformMaster && scope.userTenantId !== asString(order.tenant_id)) {
      throw new LeagueAdminApiError("Entrada fora do tenant ativo.", 403);
    }

    const currentData = asObject(order.data) ?? {};
    if (!currentData.manualGateEntry) {
      throw new LeagueAdminApiError("Só cadastros manuais podem ser excluídos por aqui.", 400);
    }

    const transferAudit = asObject(currentData.transferAudit);
    const sourceOrderId = asString(transferAudit?.fromOrderId);
    const sourceTicketToken = asString(transferAudit?.ticketToken);
    const now = new Date().toISOString();

    if (currentData.manualTransfer && sourceOrderId && sourceTicketToken) {
      const { data: sourceOrderRow, error: sourceOrderError } = await supabaseAdmin
        .from("solicitacoes_ingressos")
        .select("*")
        .eq("id", sourceOrderId)
        .maybeSingle();
      if (sourceOrderError) throw new LeagueAdminApiError(sourceOrderError.message, 400);

      const sourceOrder = asObject(sourceOrderRow) ?? {};
      if (asString(sourceOrder.id) && asString(sourceOrder.eventoId) === eventId) {
        const sourcePaymentConfig = normalizePaymentConfig(sourceOrder.payment_config);
        const restoreAudit = {
          action: "manual_transfer_deleted",
          at: now,
          byUserId: scope.userId,
          byUserName: operatorName,
          deletedOrderId: orderId,
          ticketToken: sourceTicketToken,
        };
        const nextEntries = (sourcePaymentConfig?.ticketEntries ?? []).map((entry) => {
          if (entry.token !== sourceTicketToken) return entry;
          const restored: CommerceTicketEntry = {
            ...entry,
            status: "ativo",
            transferHistory: [
              ...(Array.isArray(entry.transferHistory) ? entry.transferHistory : []),
              restoreAudit,
            ].slice(-50),
          };
          delete restored.transferredAt;
          delete restored.transferredToUserId;
          delete restored.transferredToUserName;
          delete restored.transferByUserId;
          delete restored.transferByUserName;
          return restored;
        });

        await updateWithColumnFallback(
          "solicitacoes_ingressos",
          {
            payment_config: sourcePaymentConfig
              ? {
                  ...sourcePaymentConfig,
                  ticketEntries: nextEntries,
                }
              : sourceOrder.payment_config,
            transferAt: null,
            transferToUserId: null,
            transferToUserName: null,
            transferByUserId: null,
            transferByUserName: null,
            transferHistory: [
              ...(Array.isArray(sourceOrder.transferHistory) ? sourceOrder.transferHistory : []),
              restoreAudit,
            ].slice(-100),
          },
          async (nextPayload) =>
            supabaseAdmin.from("solicitacoes_ingressos").update(nextPayload).eq("id", sourceOrderId)
        );
      }
    }

    const { error: deleteError } = await supabaseAdmin
      .from("solicitacoes_ingressos")
      .delete()
      .eq("id", orderId);
    if (deleteError) throw new LeagueAdminApiError(deleteError.message, 400);

    return NextResponse.json({ ok: true, orderId });
  } catch (error: unknown) {
    if (error instanceof LeagueAdminApiError) {
      return NextResponse.json({ error: error.message }, { status: error.status });
    }
    const message = error instanceof Error ? error.message : "Erro ao excluir entrada manual.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
