import { randomUUID } from "node:crypto";
import { NextResponse } from "next/server";

import { normalizePaymentConfig, type CommerceTicketEntry } from "@/lib/commerceCatalog";
import { supabaseAdmin } from "@/lib/supabaseAdmin";

const asObject = (value: unknown): Record<string, unknown> =>
  typeof value === "object" && value !== null && !Array.isArray(value) ? (value as Record<string, unknown>) : {};

const asString = (value: unknown): string =>
  typeof value === "string" ? value.trim() : "";

const inferTicketCategory = (loteName: string): string => {
  const normalized = loteName
    .trim()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase();
  if (normalized.includes("nao aluno") || normalized.includes("externo")) return "Não aluno";
  if (normalized.includes("aluno")) return "Aluno";
  return "";
};

const extractMissingColumn = (error: unknown): string => {
  if (!error || typeof error !== "object") return "";
  const message = String((error as { message?: unknown; details?: unknown }).message || "");
  const details = String((error as { details?: unknown }).details || "");
  const text = `${message} ${details}`;
  const match =
    text.match(/column\s+[a-z0-9_]+\.(["']?)([a-z0-9_]+)\1\s+does not exist/i) ||
    text.match(/could not find the ['"]?([a-z0-9_]+)['"]? column/i);
  return String(match?.[2] || match?.[1] || "");
};

const RECIPIENT_NOT_FOUND_MESSAGE =
  "Destinatário não encontrado. Confira o e-mail, telefone ou RA exato. Se continuar com dúvida, entre em contato com alguém da atlética do usuário que irá receber.";

const normalizeLookup = (value: string): string =>
  value
    .trim()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase();

const phoneVariants = (digits: string): Set<string> => {
  const cleanDigits = digits.replace(/\D/g, "");
  const variants = new Set<string>();
  if (!cleanDigits) return variants;
  variants.add(cleanDigits);
  if (cleanDigits.startsWith("55") && cleanDigits.length > 11) {
    variants.add(cleanDigits.slice(2));
  } else if (cleanDigits.length >= 10) {
    variants.add(`55${cleanDigits}`);
  }
  return variants;
};

const fetchUsersWithSelectFallback = async (
  queryBuilder: (selectColumns: string) => Promise<{ data: unknown; error: unknown }>
) => {
  let columns = ["uid", "nome", "turma", "foto", "email", "telefone", "matricula", "ra", "tenant_id"];
  while (columns.length > 0) {
    const { data, error } = await queryBuilder(columns.join(","));
    if (!error) return Array.isArray(data) ? data.map(asObject) : data ? [asObject(data)] : [];

    const missingColumn = extractMissingColumn(error);
    const nextColumns = columns.filter((column) => column.toLowerCase() !== missingColumn.toLowerCase());
    if (!missingColumn || nextColumns.length === columns.length) throw new Error(String((error as { message?: unknown }).message || "Erro ao buscar usuário."));
    columns = nextColumns;
  }
  return [];
};

const findRecipient = async (recipient: string, tenantId: string) => {
  const clean = recipient.trim();
  const digits = clean.replace(/\D/g, "");
  const cleanNormalized = normalizeLookup(clean);
  const cleanEmail = clean.includes("@") ? clean.toLowerCase() : "";
  const cleanPhoneVariants = phoneVariants(digits);

  const rows = await fetchUsersWithSelectFallback(async (selectColumns) => {
    let query = supabaseAdmin.from("users").select(selectColumns).limit(2000);
    if (tenantId) query = query.eq("tenant_id", tenantId);
    return query;
  });

  for (const user of rows) {
    const uid = asString(user.uid);
    if (!uid) continue;
    const email = asString(user.email).toLowerCase();
    const matricula = normalizeLookup(asString(user.matricula) || asString(user.ra));
    const telefoneDigits = asString(user.telefone).replace(/\D/g, "");

    if (uid === clean) return user;
    if (cleanEmail && email === cleanEmail) return user;
    if (cleanNormalized && matricula && matricula === cleanNormalized) return user;
    if (cleanPhoneVariants.size > 0 && phoneVariants(telefoneDigits).size > 0) {
      const matchesPhone = [...cleanPhoneVariants].some((variant) => phoneVariants(telefoneDigits).has(variant));
      if (matchesPhone) return user;
    }
  }

  return null;
};

export async function POST(request: Request) {
  try {
    const body = (await request.json().catch(() => ({}))) as Record<string, unknown>;
    const orderId = asString(body.orderId);
    const ticketToken = asString(body.ticketToken);
    const recipientValue = asString(body.recipient);
    if (!orderId || !ticketToken || !recipientValue) {
      return NextResponse.json({ error: "Informe o ingresso e o destinatário." }, { status: 400 });
    }

    const { data: orderRow, error: orderError } = await supabaseAdmin
      .from("solicitacoes_ingressos")
      .select("*")
      .eq("id", orderId)
      .maybeSingle();
    if (orderError) throw new Error(orderError.message);
    const order = asObject(orderRow);
    if (!asString(order.id)) {
      return NextResponse.json({ error: "Pedido não encontrado." }, { status: 404 });
    }

    const recipient = await findRecipient(recipientValue, asString(order.tenant_id));
    if (!recipient) {
      return NextResponse.json({ error: RECIPIENT_NOT_FOUND_MESSAGE }, { status: 404 });
    }

    const paymentConfig = normalizePaymentConfig(order.payment_config);
    const entries = paymentConfig?.ticketEntries || [];
    const sourceEntry = entries.find((entry) => entry.token === ticketToken);
    if (!sourceEntry) {
      return NextResponse.json({ error: "Ingresso não encontrado." }, { status: 404 });
    }
    if (sourceEntry.status !== "ativo") {
      return NextResponse.json({ error: "Somente ingressos ativos podem ser transferidos." }, { status: 400 });
    }

    const now = new Date().toISOString();
    const recipientUid = asString(recipient.uid);
    if (recipientUid === asString(order.userId)) {
      return NextResponse.json({ error: "Esse ingresso já pertence a esse usuário." }, { status: 400 });
    }
    const recipientName = asString(recipient.nome) || "Participante";
    const recipientTurma = asString(recipient.turma) || "Sem turma";
    const senderName = asString(sourceEntry.holderName) || asString(order.userName) || "Participante";
    const newOrderId = randomUUID();
    const newToken = randomUUID();
    const transferAudit = {
      at: now,
      fromOrderId: orderId,
      fromUserId: asString(order.userId),
      fromUserName: senderName,
      toOrderId: newOrderId,
      toUserId: recipientUid,
      toUserName: recipientName,
      ticketToken,
      byUserId: asString(order.userId),
      byUserName: senderName,
    };
    const newTicket: CommerceTicketEntry = {
      id: `${newOrderId}:1`,
      token: newToken,
      label: sourceEntry.label || "Ingresso 1",
      unitIndex: 1,
      status: "ativo",
      orderId: newOrderId,
      orderCode: newOrderId.slice(0, 8).toUpperCase(),
      eventId: sourceEntry.eventId || asString(order.eventoId),
      eventTitle: sourceEntry.eventTitle || asString(order.eventoNome),
      loteName: sourceEntry.loteName || asString(order.loteNome),
      holderName: recipientName,
      holderTurma: recipientTurma,
      transferredAt: now,
      transferredFromUserId: asString(order.userId),
      transferredFromUserName: senderName,
      transferByUserId: asString(order.userId),
      transferByUserName: senderName,
      transferHistory: [transferAudit],
    };

    const nextSourceEntries = entries.map((entry) =>
      entry.token === ticketToken
        ? {
            ...entry,
            status: "transferido" as const,
            transferredAt: now,
            transferredToUserId: recipientUid,
            transferredToUserName: recipientName,
            transferByUserId: asString(order.userId),
            transferByUserName: senderName,
            transferHistory: [...(Array.isArray(entry.transferHistory) ? entry.transferHistory : []), transferAudit].slice(-50),
          }
        : entry
    );

    let updatePayload: Record<string, unknown> = {
      payment_config: {
        ...paymentConfig,
        ticketEntries: nextSourceEntries,
      },
      transferAt: now,
      transferFromUserId: asString(order.userId),
      transferFromUserName: senderName,
      transferToUserId: recipientUid,
      transferToUserName: recipientName,
      transferByUserId: asString(order.userId),
      transferByUserName: senderName,
      transferHistory: [
        ...(Array.isArray(order.transferHistory) ? order.transferHistory : []),
        transferAudit,
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
        throw new Error(updateError.message);
      }
      const nextPayload = { ...updatePayload };
      delete nextPayload[missingColumn];
      updatePayload = nextPayload;
    }

    const newPaymentConfig = {
      chave: paymentConfig?.chave || "",
      banco: paymentConfig?.banco || "",
      titular: paymentConfig?.titular || "",
      ...(paymentConfig?.whatsapp ? { whatsapp: paymentConfig.whatsapp } : {}),
      ...(paymentConfig?.recipient ? { recipient: paymentConfig.recipient } : {}),
      ticketEntries: [newTicket],
    };

    const loteNome = asString(order.loteNome);
    let insertPayload: Record<string, unknown> = {
      id: newOrderId,
      tenant_id: order.tenant_id ?? null,
      userId: recipientUid,
      userName: recipientName,
      userTurma: recipientTurma,
      status: "aprovado",
      eventoId: asString(order.eventoId),
      eventoNome: asString(order.eventoNome),
      loteId: order.loteId ?? null,
      loteNome,
      quantidade: 1,
      valorUnitario: "0,00",
      valorTotal: "0,00",
      dataSolicitacao: now,
      dataAprovacao: now,
      aprovadoPor: "Transferência de ingresso",
      itemType: "ingresso",
      itemName: loteNome || "Ingresso",
      itemCategory: inferTicketCategory(loteNome),
      approvalMethod: "transferencia",
      transferAt: now,
      transferFromUserId: asString(order.userId),
      transferFromUserName: senderName,
      transferToUserId: recipientUid,
      transferToUserName: recipientName,
      transferByUserId: asString(order.userId),
      transferByUserName: senderName,
      transferHistory: [transferAudit],
      discountValue: "R$ 0,00",
      discountKind: "",
      discountSource: "",
      payment_config: newPaymentConfig,
      data: {
        ...(typeof order.data === "object" && order.data !== null ? (order.data as Record<string, unknown>) : {}),
        transferencia: true,
        pedidoOriginalId: orderId,
        transferidoPor: senderName,
      },
    };

    while (Object.keys(insertPayload).length > 0) {
      const { error: insertError } = await supabaseAdmin.from("solicitacoes_ingressos").insert(insertPayload);
      if (!insertError) break;

      const missingColumn = extractMissingColumn(insertError);
      if (!missingColumn || !Object.prototype.hasOwnProperty.call(insertPayload, missingColumn)) {
        throw new Error(insertError.message);
      }
      const nextPayload = { ...insertPayload };
      delete nextPayload[missingColumn];
      insertPayload = nextPayload;
    }

    return NextResponse.json({
      message: `Ingresso transferido para ${recipientName}.`,
      ticketPath: `/public/ingressos/${newOrderId}/${newToken}`,
    });
  } catch (error: unknown) {
    console.error("Falha ao transferir ingresso.", error);
    return NextResponse.json(
      { error: error instanceof Error ? error.message : "Não foi possível transferir o ingresso." },
      { status: 500 }
    );
  }
}
