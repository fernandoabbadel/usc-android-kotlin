import { randomUUID } from "node:crypto";
import { NextRequest, NextResponse } from "next/server";

import { supabaseAdmin } from "@/lib/supabaseAdmin";

export const runtime = "nodejs";

type Row = Record<string, unknown>;

const asObject = (value: unknown): Row =>
  value && typeof value === "object" && !Array.isArray(value) ? (value as Row) : {};

const asString = (value: unknown, fallback = ""): string =>
  typeof value === "string" ? value.trim() : fallback;

const extractSchemaFallbackColumn = (error: unknown): string => {
  if (!error || typeof error !== "object") return "";
  const raw = error as { message?: unknown; details?: unknown; hint?: unknown };
  const text = [raw.message, raw.details, raw.hint]
    .map((entry) => (typeof entry === "string" ? entry : ""))
    .join(" ");
  const match =
    text.match(/column\s+[a-z0-9_]+\.(["']?)([a-z0-9_]+)\1\s+does not exist/i) ||
    text.match(/could not find the ['"]?([a-z0-9_]+)['"]? column/i) ||
    text.match(/non-DEFAULT value into column\s+(["']?)([a-z0-9_]+)\1/i) ||
    text.match(/column\s+(["']?)([a-z0-9_]+)\1\s+is a generated column/i);
  return String(match?.[2] || match?.[1] || "");
};

const getAuthUser = async (request: NextRequest): Promise<Row> => {
  const token = (request.headers.get("authorization") || "").replace(/^Bearer\s+/i, "").trim();
  if (!token) throw Object.assign(new Error("Não autenticado."), { status: 401 });

  const { data, error } = await supabaseAdmin.auth.getUser(token);
  if (error || !data.user?.id) throw Object.assign(new Error("Sessão inválida."), { status: 401 });

  const { data: row, error: profileError } = await supabaseAdmin
    .from("users")
    .select("uid,nome,turma,email,telefone,tenant_id")
    .eq("uid", data.user.id)
    .maybeSingle();
  if (profileError) throw new Error(profileError.message);
  const profile = asObject(row);
  return {
    uid: data.user.id,
    nome: asString(profile.nome) || asString(data.user.user_metadata?.nome) || "Usuário",
    turma: asString(profile.turma),
    email: asString(profile.email || data.user.email),
    telefone: asString(profile.telefone),
    tenant_id: asString(profile.tenant_id),
  };
};

const fetchUserWithSelectFallback = async (
  queryBuilder: (selectColumns: string) => Promise<{ data: unknown; error: unknown }>
): Promise<Row | null> => {
  let columns = ["uid", "nome", "turma", "email", "telefone", "ra", "cpf", "tenant_id"];
  while (columns.length > 0) {
    const { data, error } = await queryBuilder(columns.join(","));
    if (!error) {
      const row = Array.isArray(data) ? data[0] : data;
      return row ? asObject(row) : null;
    }
    const missing = extractSchemaFallbackColumn(error);
    const nextColumns = columns.filter((column) => column.toLowerCase() !== missing.toLowerCase());
    if (!missing || nextColumns.length === columns.length) throw error;
    columns = nextColumns;
  }
  return null;
};

const findRecipient = async (recipient: string, tenantId: string): Promise<Row | null> => {
  const clean = recipient.trim();
  const digits = clean.replace(/\D/g, "");
  const selectors = [
    { column: "uid", value: clean, mode: "eq" as const },
    { column: "email", value: clean, mode: "ilike" as const },
    ...(digits ? [{ column: "telefone", value: digits.slice(-9), mode: "ilike" as const }] : []),
    ...(clean ? [{ column: "ra", value: clean, mode: "ilike" as const }] : []),
  ];

  for (const selector of selectors) {
    try {
      const row = await fetchUserWithSelectFallback(async (selectColumns) => {
        let query = supabaseAdmin.from("users").select(selectColumns).limit(1);
        if (tenantId) query = query.eq("tenant_id", tenantId);
        return selector.mode === "eq"
          ? query.eq(selector.column, selector.value)
          : query.ilike(selector.column, `%${selector.value}%`);
      });
      if (asString(row?.uid)) return row;
    } catch {
      // Campo opcional ausente, tenta o próximo seletor.
    }
  }

  return null;
};

const normalizeVoucherStatus = (value: unknown): string => {
  const raw = asString(value).toLowerCase();
  if (["utilizado", "inativo", "retirado", "used", "lido"].includes(raw)) return "utilizado";
  if (["transferido", "transferred"].includes(raw)) return "transferido";
  if (["cancelado", "rejeitado", "rejected"].includes(raw)) return "cancelado";
  if (["estornado", "reembolsado", "refunded"].includes(raw)) return raw === "reembolsado" ? "reembolsado" : "estornado";
  if (["pendente", "pending"].includes(raw)) return "pendente";
  return "ativo";
};

const buildVoucherEntries = (order: Row): Row[] => {
  const data = asObject(order.data);
  const eventParty = asObject(data.eventParty);
  const rawEntries = Array.isArray(eventParty.voucherEntries) ? eventParty.voucherEntries : [];
  const quantity = Math.max(1, Math.floor(Number(order.quantidade || rawEntries.length || 1) || 1));
  const entries = rawEntries
    .map((entry, index) => {
      const row = asObject(entry);
      const id = asString(row.id || row.voucherId || row.token) || `item-${index + 1}`;
      return {
        ...row,
        id,
        label: asString(row.label) || `Ficha ${index + 1}`,
        status: normalizeVoucherStatus(row.status || eventParty.voucherStatus),
        code: asString(row.code || row.codigo || row.manualNumber) || `${asString(order.id).slice(0, 8).toUpperCase()}-${index + 1}`,
      };
    });
  const seen = new Set(entries.map((entry) => asString(entry.id)));
  for (let index = 0; entries.length < quantity; index += 1) {
    const id = `item-${index + 1}`;
    if (seen.has(id)) continue;
    seen.add(id);
    entries.push({
      id,
      label: `Ficha ${index + 1}`,
      status: normalizeVoucherStatus(eventParty.voucherStatus),
      code: `${asString(order.id).slice(0, 8).toUpperCase()}-${index + 1}`,
    });
  }
  return entries.slice(0, quantity);
};

const updateOrderData = async (orderId: string, payload: Row): Promise<void> => {
  const { error } = await supabaseAdmin.from("orders").update(payload).eq("id", orderId);
  if (error) throw new Error(error.message);
};

const insertOrderWithFallback = async (payload: Row): Promise<void> => {
  let mutable = { ...payload };
  while (Object.keys(mutable).length > 0) {
    const { error } = await supabaseAdmin.from("orders").insert(mutable);
    if (!error) return;
    const missing = extractSchemaFallbackColumn(error);
    if (!missing || !Object.prototype.hasOwnProperty.call(mutable, missing)) throw new Error(error.message);
    const next = { ...mutable };
    delete next[missing];
    mutable = next;
  }
};

export async function GET(request: NextRequest) {
  try {
    const user = await getAuthUser(request);
    const userId = asString(user.uid);
    const eventId = request.nextUrl.searchParams.get("eventId")?.trim() || "";
    if (!eventId) return NextResponse.json({ transfers: [] });

    const { data, error } = await supabaseAdmin
      .from("orders")
      .select("id,tenant_id,userId,userName,productId,productName,price,quantidade,status,data,createdAt,updatedAt")
      .contains("data", { eventParty: { eventId } })
      .limit(300);
    if (error) throw new Error(error.message);

    const transfers = ((data ?? []) as unknown[])
      .map(asObject)
      .flatMap((order) => {
        const eventParty = asObject(asObject(order.data).eventParty);
        const requests = Array.isArray(eventParty.transferRequests) ? eventParty.transferRequests : [];
        return requests
          .map(asObject)
          .filter((entry) => asString(entry.status) === "pendente" && asString(entry.toUserId) === userId)
          .map((entry) => ({
            orderId: asString(order.id),
            productId: asString(order.productId),
            productName: asString(order.productName),
            voucherId: asString(entry.voucherId),
            voucherLabel: asString(entry.voucherLabel),
            fromUserName: asString(entry.fromUserName),
            requestedAt: asString(entry.requestedAt),
          }));
      });

    return NextResponse.json({ transfers });
  } catch (error: unknown) {
    const status = typeof (error as { status?: unknown }).status === "number" ? (error as { status: number }).status : 500;
    return NextResponse.json({ error: error instanceof Error ? error.message : "Erro ao carregar transferências." }, { status });
  }
}

export async function POST(request: NextRequest) {
  try {
    const user = await getAuthUser(request);
    const body = asObject(await request.json().catch(() => ({})));
    const action = asString(body.action);
    const orderId = asString(body.orderId);
    const voucherId = asString(body.voucherId);
    if (!orderId || !voucherId) {
      return NextResponse.json({ error: "Informe a ficha." }, { status: 400 });
    }

    const { data: orderRow, error: orderError } = await supabaseAdmin
      .from("orders")
      .select("id,tenant_id,userId,userName,productId,productName,price,quantidade,status,data,payment_config,createdAt,updatedAt")
      .eq("id", orderId)
      .maybeSingle();
    if (orderError) throw new Error(orderError.message);
    const order = asObject(orderRow);
    if (!asString(order.id)) return NextResponse.json({ error: "Pedido não encontrado." }, { status: 404 });

    const data = asObject(order.data);
    const eventParty = asObject(data.eventParty);
    const entries = buildVoucherEntries(order);
    const sourceEntry = entries.find((entry) => asString(entry.id) === voucherId);
    if (!sourceEntry) return NextResponse.json({ error: "Ficha não encontrada." }, { status: 404 });

    if (action === "request") {
      if (asString(order.userId) !== asString(user.uid)) {
        return NextResponse.json({ error: "Você só pode transferir suas próprias fichas." }, { status: 403 });
      }
      if (normalizeVoucherStatus(sourceEntry.status) !== "ativo") {
        return NextResponse.json({ error: "Somente fichas pagas e ativas podem ser transferidas." }, { status: 400 });
      }
      const recipient = await findRecipient(asString(body.recipient), asString(order.tenant_id));
      if (!recipient) return NextResponse.json({ error: "Destinatário não encontrado." }, { status: 404 });
      const toUserId = asString(recipient.uid);
      if (toUserId === asString(user.uid)) {
        return NextResponse.json({ error: "A ficha já está com esse usuário." }, { status: 400 });
      }

      const now = new Date().toISOString();
      const previousRequests = Array.isArray(eventParty.transferRequests) ? eventParty.transferRequests.map(asObject) : [];
      const nextRequests = [
        ...previousRequests.filter(
          (entry) => !(asString(entry.voucherId) === voucherId && asString(entry.status) === "pendente")
        ),
        {
          id: randomUUID(),
          status: "pendente",
          voucherId,
          voucherLabel: asString(sourceEntry.label),
          fromUserId: asString(user.uid),
          fromUserName: asString(user.nome),
          toUserId,
          toUserName: asString(recipient.nome) || "Usuário",
          requestedAt: now,
        },
      ];
      await updateOrderData(orderId, {
        data: {
          ...data,
          eventParty: {
            ...eventParty,
            transferRequests: nextRequests,
          },
        },
        updatedAt: now,
      });
      return NextResponse.json({ ok: true, message: "Transferência enviada para aceite." });
    }

    if (action !== "accept") {
      return NextResponse.json({ error: "Ação inválida." }, { status: 400 });
    }

    const requests = Array.isArray(eventParty.transferRequests) ? eventParty.transferRequests.map(asObject) : [];
    const requestEntry = requests.find(
      (entry) =>
        asString(entry.voucherId) === voucherId &&
        asString(entry.status) === "pendente" &&
        asString(entry.toUserId) === asString(user.uid)
    );
    if (!requestEntry) return NextResponse.json({ error: "Transferência pendente não encontrada." }, { status: 404 });

    const now = new Date().toISOString();
    const recipientName = asString(user.nome) || "Usuário";
    const sourceName = asString(requestEntry.fromUserName) || asString(order.userName) || "Usuário";
    const nextSourceEntries = entries.map((entry) =>
      asString(entry.id) === voucherId
        ? {
            ...entry,
            status: "transferido",
            transferredAt: now,
            transferredToUserId: asString(user.uid),
            transferredToUserName: recipientName,
            transferStatus: `Transferido para ${recipientName}`,
          }
        : entry
    );
    const nextRequests = requests.map((entry) =>
      asString(entry.voucherId) === voucherId && asString(entry.status) === "pendente"
        ? { ...entry, status: "aceito", acceptedAt: now }
        : entry
    );
    const inactiveCount = nextSourceEntries.filter((entry) => normalizeVoucherStatus(entry.status) !== "ativo").length;
    await updateOrderData(orderId, {
      data: {
        ...data,
        eventParty: {
          ...eventParty,
          voucherEntries: nextSourceEntries,
          voucherStatus: inactiveCount >= nextSourceEntries.length ? "transferido" : "parcial",
          transferRequests: nextRequests,
        },
      },
      updatedAt: now,
    });

    const newOrderId = randomUUID();
    const newVoucherId = `transfer-${voucherId}-${Date.now()}`;
    const productName = asString(order.productName) || asString(eventParty.productName) || "Produto";
    const newEntry = {
      id: newVoucherId,
      label: asString(sourceEntry.label) || "Ficha transferida",
      status: "ativo",
      code: `${newOrderId.slice(0, 8).toUpperCase()}-1`,
      transferredAt: now,
      transferredFromUserId: asString(requestEntry.fromUserId),
      transferredFromUserName: sourceName,
      transferStatus: `Transferido de ${sourceName}`,
    };
    await insertOrderWithFallback({
      id: newOrderId,
      tenant_id: order.tenant_id,
      userId: asString(user.uid),
      userName: recipientName,
      productId: asString(order.productId),
      productName,
      price: 0,
      quantidade: 1,
      status: "approved",
      approvedBy: "Transferência",
      payment_config: order.payment_config ?? null,
      eventId: asString(eventParty.eventId),
      eventItemType: "produto",
      eventItemName: productName,
      eventLoteNome: "-",
      eventItemCategory: asString(eventParty.section) || "Geral",
      eventApprovalAt: now,
      eventApprovalMethod: "transferencia",
      eventDiscountValue: "R$ 0,00",
      eventDiscountKind: "",
      eventDiscountSource: "",
      eventCreatedManually: false,
      eventCreatedByName: "",
      data: {
        eventParty: {
          ...eventParty,
          voucherStatus: "ativo",
          voucherEntries: [newEntry],
          transferOrder: true,
          originalOrderId: orderId,
          transferredFromUserId: asString(requestEntry.fromUserId),
          transferredFromUserName: sourceName,
          transferredAt: now,
        },
      },
      createdAt: now,
      updatedAt: now,
    });

    return NextResponse.json({ ok: true, message: "Transferência aceita.", orderId: newOrderId });
  } catch (error: unknown) {
    const status = typeof (error as { status?: unknown }).status === "number" ? (error as { status: number }).status : 500;
    return NextResponse.json({ error: error instanceof Error ? error.message : "Erro ao transferir ficha." }, { status });
  }
}
