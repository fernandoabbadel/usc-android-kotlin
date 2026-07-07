import { getSupabaseClient, getSupabasePublicClient } from "./supabase";
import { isEventVisibilityBlocked } from "./eventVisibilityBlock";
import { createStoreOrder } from "./storePublicService";
import { upsertStoreProduct } from "./storeService";

type Row = Record<string, unknown>;

const EVENT_SELECT_COLUMNS =
  "id,titulo,data,hora,imagem,status,tenant_id,stats,data_extra,payment_config,createdAt,updatedAt";
const PRODUCT_SELECT_COLUMNS =
  "id,tenant_id,nome,preco,img,descricao,categoria,estoque,active,aprovado,status,payment_config,data,createdAt,updatedAt";
const LEGACY_ORDER_SELECT_COLUMNS =
  "id,tenant_id,userId,userName,productId,productName,price,total,quantidade,data,status,approvedBy,payment_config,createdAt,updatedAt";
const ORDER_SELECT_COLUMNS =
  `${LEGACY_ORDER_SELECT_COLUMNS},eventId,eventItemType,eventItemName,eventLoteNome,eventItemCategory,eventApprovalAt,eventApprovalMethod,eventCheckinAt,eventCheckinByUserId,eventCheckinByUserName,eventCheckinMethod,eventDiscountValue,eventDiscountKind,eventDiscountSource,eventCreatedManually,eventCreatedByName`;

const asObject = (value: unknown): Row | null =>
  value && typeof value === "object" && !Array.isArray(value) ? (value as Row) : null;
const asString = (value: unknown, fallback = ""): string =>
  typeof value === "string" ? value : fallback;

const normalizeEventPartyMenuText = (value: unknown): string => {
  const text = asString(value).trim();
  const normalizedText = text
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase();

  if (normalizedText === "cardapio do evento") return "";
  return text;
};
const asNumber = (value: unknown, fallback = 0): number =>
  typeof value === "number" && Number.isFinite(value) ? value : fallback;
const asBoolean = (value: unknown, fallback = false): boolean =>
  typeof value === "boolean" ? value : fallback;

const normalizeTenantId = (value?: string | null): string => asString(value).trim();

const splitSelectColumns = (selectColumns: string): string[] =>
  selectColumns
    .split(",")
    .map((entry) => entry.trim())
    .filter(Boolean);

const extractMissingSchemaColumn = (error: unknown): string => {
  if (!error || typeof error !== "object") return "";
  const raw = error as { message?: unknown; details?: unknown; hint?: unknown };
  const text = [raw.message, raw.details, raw.hint]
    .map((entry) => (typeof entry === "string" ? entry : ""))
    .join(" ");
  const match =
    text.match(/column\s+[a-z0-9_]+\.(["']?)([a-z0-9_]+)\1\s+does not exist/i) ||
    text.match(/could not find the ['"]?([a-z0-9_]+)['"]? column/i);
  return String(match?.[2] || match?.[1] || "");
};

const extractNonWritableSchemaColumn = (error: unknown): string => {
  if (!error || typeof error !== "object") return "";
  const raw = error as { message?: unknown; details?: unknown; hint?: unknown };
  const text = [raw.message, raw.details, raw.hint]
    .map((entry) => (typeof entry === "string" ? entry : ""))
    .join(" ");
  const match =
    text.match(/non-DEFAULT value into column\s+(["']?)([a-z0-9_]+)\1/i) ||
    text.match(/column\s+(["']?)([a-z0-9_]+)\1\s+is a generated column/i);
  return String(match?.[2] || match?.[1] || "");
};

const extractSchemaFallbackColumn = (error: unknown): string =>
  extractMissingSchemaColumn(error) || extractNonWritableSchemaColumn(error);

export type EventPartyConfig = {
  enabled: boolean;
  menuTitle: string;
  categoryName: string;
};

export type EventPartyEvent = {
  id: string;
  titulo: string;
  data: string;
  hora: string;
  imagem: string;
  tenantId: string;
  visibility: "public" | "internal";
  paymentConfig: Row | null;
  config: EventPartyConfig;
};

export type EventPartyProduct = {
  id: string;
  tenantId: string;
  nome: string;
  preco: number;
  img: string;
  descricao: string;
  categoria: string;
  estoque: number;
  status: string;
  paymentConfig: Row | null;
  data: Row;
  createdAt: unknown;
  updatedAt: unknown;
};

export type EventPartyOrder = {
  id: string;
  tenantId: string;
  userId: string;
  userName: string;
  productId: string;
  productName: string;
  price: number;
  total: number;
  quantidade: number;
  status: string;
  approvedBy: string;
  paymentConfig: Row | null;
  data: Row;
  createdAt: unknown;
  updatedAt: unknown;
  eventId: string;
  eventItemType: string;
  eventItemName: string;
  eventLoteNome: string;
  eventItemCategory: string;
  eventApprovalAt: unknown;
  eventApprovalMethod: string;
  eventCheckinAt: unknown;
  eventCheckinByUserId: string;
  eventCheckinByUserName: string;
  eventCheckinMethod: string;
  eventDiscountValue: string;
  eventDiscountKind: string;
  eventDiscountSource: string;
  eventCreatedManually: boolean;
  eventCreatedByName: string;
};

export type EventPartyVoucherStatus =
  | "pendente"
  | "ativo"
  | "parcial"
  | "utilizado"
  | "cancelado"
  | "transferido"
  | "estornado"
  | "reembolsado"
  | "inativo";

export type EventPartyVoucherEntryStatus = Exclude<EventPartyVoucherStatus, "parcial">;

export type EventPartyVoucherEntry = {
  id: string;
  label: string;
  status: EventPartyVoucherEntryStatus;
  code: string;
  manualNumber: string;
  usedAt: string;
  usedByUserId: string;
  usedByUserName: string;
  usedMethod: string;
  transferStatus: string;
  transferredAt: string;
  transferredToUserId: string;
  transferredToUserName: string;
  transferredFromUserId: string;
  transferredFromUserName: string;
};

export type EventPartyOrderReference = {
  orderCode: string;
  fichaCode: string;
  voucherCodes: string[];
  summary: string;
};

export const normalizeEventPartyConfig = (dataExtra: unknown): EventPartyConfig => {
  const raw = asObject(dataExtra) ?? {};
  const config = asObject(raw.eventParty) ?? {};
  return {
    enabled: asBoolean(config.enabled, false),
    menuTitle:
      normalizeEventPartyMenuText(config.menuTitle) ||
      normalizeEventPartyMenuText(config.cardapioTitle) ||
      "Menu do evento",
    categoryName:
      normalizeEventPartyMenuText(config.categoryName) ||
      normalizeEventPartyMenuText(config.categoria) ||
      "Menu do evento",
  };
};

export const serializeEventPartyConfig = (
  previousDataExtra: unknown,
  config: EventPartyConfig
): Row => {
  const raw = asObject(previousDataExtra) ?? {};
  return {
    ...raw,
    eventParty: {
      ...(asObject(raw.eventParty) ?? {}),
      enabled: config.enabled,
      menuTitle: normalizeEventPartyMenuText(config.menuTitle) || "Menu do evento",
      categoryName: normalizeEventPartyMenuText(config.categoryName) || "Menu do evento",
      updatedAt: new Date().toISOString(),
    },
  };
};

const normalizeEvent = (row: unknown): EventPartyEvent | null => {
  const data = asObject(row);
  if (!data) return null;
  const id = asString(data.id).trim();
  if (!id) return null;
  return {
    id,
    titulo: asString(data.titulo, "Evento"),
    data: asString(data.data),
    hora: asString(data.hora),
    imagem: asString(data.imagem),
    tenantId: asString(data.tenant_id),
    visibility:
      asString((asObject(data.stats) ?? {}).eventVisibility || (asObject(data.stats) ?? {}).tenantEventVisibility)
        .trim()
        .toLowerCase() === "internal"
        ? "internal"
        : "public",
    paymentConfig: asObject(data.payment_config),
    config: normalizeEventPartyConfig(data.data_extra),
  };
};

const normalizeProduct = (row: unknown): EventPartyProduct | null => {
  const data = asObject(row);
  if (!data) return null;
  const id = asString(data.id).trim();
  if (!id) return null;
  return {
    id,
    tenantId: asString(data.tenant_id),
    nome: asString(data.nome, "Produto"),
    preco: asNumber(data.preco, 0),
    img: asString(data.img),
    descricao: asString(data.descricao),
    categoria: asString(data.categoria),
    estoque: Math.max(0, Math.floor(asNumber(data.estoque, 0))),
    status: asString(data.status, "ativo"),
    paymentConfig: asObject(data.payment_config),
    data: asObject(data.data) ?? {},
    createdAt: data.createdAt,
    updatedAt: data.updatedAt,
  };
};

const normalizeOrder = (row: unknown): EventPartyOrder | null => {
  const data = asObject(row);
  if (!data) return null;
  const id = asString(data.id).trim();
  if (!id) return null;
  return {
    id,
    tenantId: asString(data.tenant_id),
    userId: asString(data.userId),
    userName: asString(data.userName, "Aluno"),
    productId: asString(data.productId),
    productName: asString(data.productName, "Produto"),
    price: asNumber(data.price, 0),
    total: asNumber(data.total, asNumber(data.price, 0)),
    quantidade: Math.max(1, Math.floor(asNumber(data.quantidade, 1))),
    status: asString(data.status, "pendente"),
    approvedBy: asString(data.approvedBy),
    paymentConfig: asObject(data.payment_config),
    data: asObject(data.data) ?? {},
    createdAt: data.createdAt,
    updatedAt: data.updatedAt,
    eventId: asString(data.eventId),
    eventItemType: asString(data.eventItemType),
    eventItemName: asString(data.eventItemName),
    eventLoteNome: asString(data.eventLoteNome),
    eventItemCategory: asString(data.eventItemCategory),
    eventApprovalAt: data.eventApprovalAt,
    eventApprovalMethod: asString(data.eventApprovalMethod),
    eventCheckinAt: data.eventCheckinAt,
    eventCheckinByUserId: asString(data.eventCheckinByUserId),
    eventCheckinByUserName: asString(data.eventCheckinByUserName),
    eventCheckinMethod: asString(data.eventCheckinMethod),
    eventDiscountValue: asString(data.eventDiscountValue),
    eventDiscountKind: asString(data.eventDiscountKind),
    eventDiscountSource: asString(data.eventDiscountSource),
    eventCreatedManually: asBoolean(data.eventCreatedManually),
    eventCreatedByName: asString(data.eventCreatedByName),
  };
};

const getProductEventId = (product: EventPartyProduct): string =>
  asString(asObject(product.data.eventParty)?.eventId).trim();

const getOrderEventId = (order: EventPartyOrder): string =>
  order.eventId.trim() || asString(asObject(order.data.eventParty)?.eventId).trim();

const normalizeVoucherEntryStatus = (
  value: unknown,
  fallback: EventPartyVoucherEntryStatus = "pendente"
): EventPartyVoucherEntryStatus => {
  const raw = asString(value).trim().toLowerCase();
  if (["ativo", "active", "liberado", "aprovado"].includes(raw)) return "ativo";
  if (["inativo", "used", "lido", "consumido", "retirado", "utilizado"].includes(raw)) return "utilizado";
  if (["cancelado", "canceled", "cancelled", "rejected", "rejeitado"].includes(raw)) return "cancelado";
  if (["transferido", "transferred"].includes(raw)) return "transferido";
  if (["estornado", "reembolsado", "refunded", "refund"].includes(raw)) return raw === "reembolsado" ? "reembolsado" : "estornado";
  if (["pendente", "pending", "analise"].includes(raw)) return "pendente";
  return fallback;
};

export const buildEventPartyVoucherEntries = (
  quantity: number,
  status: EventPartyVoucherEntryStatus = "pendente",
  options?: {
    manualNumber?: string;
    codePrefix?: string;
  }
): EventPartyVoucherEntry[] =>
  Array.from({ length: Math.max(1, Math.floor(Number(quantity) || 1)) }, (_, index) => ({
    id: options?.manualNumber && index === 0 ? `ficha-${options.manualNumber}` : `item-${index + 1}`,
    label: options?.manualNumber && index === 0 ? `Ficha ${options.manualNumber}` : "Ficha digital",
    status,
    code: options?.manualNumber && index === 0 ? options.manualNumber : `${options?.codePrefix || "FICHA"}-${index + 1}`,
    manualNumber: options?.manualNumber && index === 0 ? options.manualNumber : "",
    usedAt: "",
    usedByUserId: "",
    usedByUserName: "",
    usedMethod: "",
    transferStatus: "",
    transferredAt: "",
    transferredToUserId: "",
    transferredToUserName: "",
    transferredFromUserId: "",
    transferredFromUserName: "",
  }));

export const getEventPartyVoucherEntries = (order: EventPartyOrder): EventPartyVoucherEntry[] => {
  const orderData = asObject(order.data) ?? {};
  const eventParty = asObject(orderData.eventParty) ?? {};
  const approvedStatus = normalizeEventPartyVoucherStatus(order.status) !== "pendente";
  const legacyVoucherStatus = normalizeVoucherEntryStatus(
    eventParty.voucherStatus,
    approvedStatus ? "ativo" : "pendente"
  );
  const rawEntries = Array.isArray(eventParty.voucherEntries)
    ? eventParty.voucherEntries
    : Array.isArray(eventParty.vouchers)
    ? eventParty.vouchers
    : [];
  const quantity = Math.max(1, Math.floor(Number(order.quantidade || rawEntries.length || 1) || 1));
  const normalizedEntries = rawEntries
    .map((entry, index): EventPartyVoucherEntry | null => {
      const row = asObject(entry);
      if (!row) return null;
      const id = asString(row.id || row.voucherId || row.token).trim() || `item-${index + 1}`;
      const entryStatus = normalizeVoucherEntryStatus(row.status, legacyVoucherStatus);
      const manualNumber = asString(row.manualNumber || row.fichaNumero || row.numeroFicha).trim();
      const rawLabel = asString(row.label).trim();
      const label =
        manualNumber
          ? `Ficha ${manualNumber}`
          : rawLabel && !/^ficha\s+\d+$/i.test(rawLabel)
            ? rawLabel
            : "Ficha digital";
      return {
        id,
        label,
        status:
          approvedStatus && entryStatus === "pendente" && legacyVoucherStatus === "ativo"
            ? "ativo"
            : entryStatus,
        code: asString(row.code || row.codigo || row.manualCode || row.manualNumber).trim() || `${order.id.slice(0, 8).toUpperCase()}-${index + 1}`,
        manualNumber,
        usedAt: asString(row.usedAt || row.withdrawalAt || row.scannedAt),
        usedByUserId: asString(row.usedByUserId || row.withdrawalByUserId || row.scannedByUserId),
        usedByUserName: asString(row.usedByUserName || row.withdrawalByUserName || row.scannedByUserName),
        usedMethod: asString(row.usedMethod || row.withdrawalMethod || row.scanSource),
        transferStatus: asString(row.transferStatus),
        transferredAt: asString(row.transferredAt),
        transferredToUserId: asString(row.transferredToUserId),
        transferredToUserName: asString(row.transferredToUserName),
        transferredFromUserId: asString(row.transferredFromUserId),
        transferredFromUserName: asString(row.transferredFromUserName),
      };
    })
    .filter((entry): entry is EventPartyVoucherEntry => Boolean(entry));

  if (normalizedEntries.length >= quantity) return normalizedEntries.slice(0, quantity);

  const seen = new Set(normalizedEntries.map((entry) => entry.id));
  const missing = buildEventPartyVoucherEntries(quantity, legacyVoucherStatus, {
    codePrefix: order.id.slice(0, 8).toUpperCase(),
  }).filter(
    (entry) => !seen.has(entry.id)
  );
  return [...normalizedEntries, ...missing].slice(0, quantity);
};

const uniqueReferenceParts = (values: string[]): string[] => {
  const seen = new Set<string>();
  return values
    .map((value) => value.trim())
    .filter((value) => {
      if (!value || seen.has(value)) return false;
      seen.add(value);
      return true;
    });
};

export const getEventPartyOrderReference = (order: EventPartyOrder): EventPartyOrderReference => {
  const orderData = asObject(order.data) ?? {};
  const eventParty = asObject(orderData.eventParty) ?? {};
  const orderCode =
    asString(eventParty.orderCode || eventParty.orderNumber || eventParty.pedidoCodigo).trim() ||
    order.id.slice(0, 8).toUpperCase();
  const voucherEntries = getEventPartyVoucherEntries(order);
  const manualCode = asString(
    eventParty.manualCode ||
      eventParty.manualNumber ||
      eventParty.fichaNumero ||
      eventParty.numeroFicha
  ).trim();
  const voucherCodes = uniqueReferenceParts(
    voucherEntries.flatMap((entry) => [entry.manualNumber, entry.code])
  );
  const fichaCode = manualCode || voucherCodes[0] || orderCode;
  const extraCodes = voucherCodes.filter((code) => code !== fichaCode);
  const summary = [
    `Pedido #${orderCode}`,
    fichaCode ? `Ficha ${fichaCode}` : "",
    extraCodes.length > 0 ? `Código ${extraCodes.join(" / ")}` : "",
  ].filter(Boolean).join(" • ");

  return { orderCode, fichaCode, voucherCodes, summary };
};

const normalizeEventPartyVoucherStatus = (status: string): EventPartyVoucherStatus => {
  const normalized = status.trim().toLowerCase();
  if (!["approved", "aprovado", "paid", "pago", "delivered"].includes(normalized)) return "pendente";
  return "ativo";
};

export const isEventPartyVoucherEntryActive = (entry: EventPartyVoucherEntry): boolean =>
  entry.status === "ativo";

export const isEventPartyVoucherEntryUsed = (entry: EventPartyVoucherEntry): boolean =>
  entry.status === "utilizado" || entry.status === "inativo";

export const eventPartyVoucherStatusLabel = (status: EventPartyVoucherStatus | EventPartyVoucherEntryStatus): string => {
  if (status === "ativo") return "Ativo";
  if (status === "utilizado" || status === "inativo") return "Utilizado";
  if (status === "cancelado") return "Cancelado";
  if (status === "transferido") return "Transferido";
  if (status === "estornado") return "Estornado";
  if (status === "reembolsado") return "Reembolsado";
  if (status === "parcial") return "Parcial";
  return "Pendente";
};

export const getEventPartyVoucherSummary = (
  order: EventPartyOrder
): { status: EventPartyVoucherStatus; total: number; used: number; pending: number; active: number } => {
  const status = normalizeEventPartyVoucherStatus(order.status);
  if (status === "pendente") {
    const total = Math.max(1, Math.floor(Number(order.quantidade || 1) || 1));
    return { status: "pendente", total, used: 0, pending: total, active: 0 };
  }

  const entries = getEventPartyVoucherEntries(order);
  const total = entries.length || Math.max(1, Math.floor(Number(order.quantidade || 1) || 1));
  const used = entries.filter(isEventPartyVoucherEntryUsed).length;
  const pending = entries.filter((entry) => entry.status === "pendente").length;
  const inactive = entries.filter(
    (entry) =>
      isEventPartyVoucherEntryUsed(entry) ||
      entry.status === "cancelado" ||
      entry.status === "transferido" ||
      entry.status === "estornado" ||
      entry.status === "reembolsado"
  ).length;
  const active = entries.filter(isEventPartyVoucherEntryActive).length;
  const allWithStatus = (target: EventPartyVoucherEntryStatus) =>
    entries.length > 0 && entries.every((entry) => entry.status === target);
  const nextStatus: EventPartyVoucherStatus =
    inactive >= total
      ? used >= total
        ? "utilizado"
        : allWithStatus("transferido")
        ? "transferido"
        : allWithStatus("cancelado")
        ? "cancelado"
        : allWithStatus("estornado")
        ? "estornado"
        : allWithStatus("reembolsado")
        ? "reembolsado"
        : "inativo"
      : inactive > 0
      ? "parcial"
      : "ativo";
  return { status: nextStatus, total, used, pending, active };
};

export const getEventPartyProductSection = (product: EventPartyProduct): string =>
  asString(asObject(product.data.eventParty)?.section).trim() ||
  product.categoria.trim() ||
  "Geral";

export const getEventPartyProductOrder = (product: EventPartyProduct): number => {
  const rawOrder = asObject(product.data.eventParty)?.order;
  const parsed = typeof rawOrder === "string" ? Number(rawOrder) : rawOrder;
  return typeof parsed === "number" && Number.isFinite(parsed) ? parsed : 9999;
};

const getPaymentRecipientsValue = (row: Row): unknown[] | undefined => {
  const raw = row.recipients || row.paymentRecipients || row.receivers;
  return Array.isArray(raw) && raw.length > 0 ? raw : undefined;
};

const mergeEventPartyPaymentConfig = (
  productPaymentConfig: unknown,
  eventPaymentConfig: unknown
): Row | null => {
  const productConfig = asObject(productPaymentConfig);
  const eventConfig = asObject(eventPaymentConfig);
  if (!productConfig && !eventConfig) return null;
  if (!productConfig) return eventConfig ? { ...eventConfig } : null;
  if (!eventConfig) return { ...productConfig };

  const productRecipients = getPaymentRecipientsValue(productConfig);
  const eventRecipients = getPaymentRecipientsValue(eventConfig);
  const productRecipient = asObject(productConfig.recipient);
  const eventRecipient = asObject(eventConfig.recipient);

  return {
    ...eventConfig,
    ...productConfig,
    chave: asString(productConfig.chave).trim() || eventConfig.chave,
    banco: asString(productConfig.banco).trim() || eventConfig.banco,
    titular: asString(productConfig.titular).trim() || eventConfig.titular,
    whatsapp: asString(productConfig.whatsapp).trim() || eventConfig.whatsapp,
    ...(productRecipient || eventRecipient ? { recipient: productRecipient || eventRecipient } : {}),
    ...(productRecipients || eventRecipients ? { recipients: productRecipients || eventRecipients } : {}),
  };
};

const sortEventPartyProducts = (products: EventPartyProduct[]): EventPartyProduct[] =>
  [...products].sort((left, right) => {
    const sectionComparison = getEventPartyProductSection(left).localeCompare(
      getEventPartyProductSection(right),
      "pt-BR",
      { sensitivity: "base" }
    );
    if (sectionComparison !== 0) return sectionComparison;
    const orderComparison = getEventPartyProductOrder(left) - getEventPartyProductOrder(right);
    if (orderComparison !== 0) return orderComparison;
    return left.nome.localeCompare(right.nome, "pt-BR", { sensitivity: "base" });
  });

const updateOrderWithSchemaFallback = async (orderId: string, payload: Row): Promise<void> => {
  let mutablePayload = { ...payload };
  while (Object.keys(mutablePayload).length > 0) {
    const { error } = await getSupabaseClient().from("orders").update(mutablePayload).eq("id", orderId);
    if (!error) return;

    const fallbackColumn = extractSchemaFallbackColumn(error);
    if (!fallbackColumn || !Object.prototype.hasOwnProperty.call(mutablePayload, fallbackColumn)) {
      throw error;
    }
    const nextPayload = { ...mutablePayload };
    delete nextPayload[fallbackColumn];
    mutablePayload = nextPayload;
  }
};

const syncEventPartyOrderStatementColumns = async (payload: {
  orderId: string;
  event: EventPartyEvent;
  product: EventPartyProduct;
  manual?: boolean;
  createdByName?: string;
  approvedAt?: string | null;
  approvalMethod?: string | null;
}): Promise<void> => {
  const orderId = payload.orderId.trim();
  if (!orderId) return;
  const productSection = getEventPartyProductSection(payload.product);
  try {
    await updateOrderWithSchemaFallback(orderId, {
      eventId: payload.event.id,
      eventItemType: "produto",
      eventItemName: payload.product.nome,
      eventLoteNome: "-",
      eventItemCategory: productSection,
      eventDiscountValue: "R$ 0,00",
      eventDiscountKind: "",
      eventDiscountSource: "",
      eventCreatedManually: Boolean(payload.manual),
      eventCreatedByName: payload.createdByName || "",
      ...(payload.approvedAt ? { eventApprovalAt: payload.approvedAt } : {}),
      ...(payload.approvalMethod ? { eventApprovalMethod: payload.approvalMethod } : {}),
    });
  } catch (error) {
    console.warn("Modo Vendas: pedido criado, mas falhou ao sincronizar colunas do extrato.", error);
  }
};

export const getEventPartyVoucherStatus = (order: EventPartyOrder): EventPartyVoucherStatus =>
  getEventPartyVoucherSummary(order).status;

export async function fetchActiveEventParty(options?: {
  tenantId?: string | null;
  forceRefresh?: boolean;
}): Promise<EventPartyEvent | null> {
  const tenantId = normalizeTenantId(options?.tenantId);
  const supabase = getSupabaseClient();
  let query = supabase
    .from("eventos")
    .select(EVENT_SELECT_COLUMNS)
    .eq("status", "ativo")
    .order("data", { ascending: true })
    .limit(30);
  if (tenantId) query = query.eq("tenant_id", tenantId);

  const { data, error } = await query;
  if (error) throw error;

  return (
    ((data ?? []) as unknown[])
      .map(normalizeEvent)
      .filter((event): event is EventPartyEvent => Boolean(event?.config.enabled))[0] ?? null
  );
}

export async function fetchEventPartyProducts(options: {
  eventId: string;
  tenantId?: string | null;
  admin?: boolean;
}): Promise<EventPartyProduct[]> {
  const eventId = options.eventId.trim();
  if (!eventId) return [];
  const tenantId = normalizeTenantId(options.tenantId);
  const supabase = options.admin ? getSupabaseClient() : getSupabasePublicClient();

  const runQuery = async (withJsonFilter: boolean) => {
    let query = supabase
      .from("produtos")
      .select(PRODUCT_SELECT_COLUMNS)
      .order("createdAt", { ascending: false })
      .limit(300);
    if (tenantId) query = query.eq("tenant_id", tenantId);
    if (!options.admin) query = query.eq("aprovado", true);
    if (withJsonFilter) {
      query = query.contains("data", { eventParty: { eventId } });
    }
    return query;
  };

  let rows: unknown[] = [];
  const first = await runQuery(true);
  if (first.error) {
    const fallback = await runQuery(false);
    if (fallback.error) throw fallback.error;
    rows = fallback.data ?? [];
  } else {
    rows = first.data ?? [];
    if (rows.length === 0) {
      const fallback = await runQuery(false);
      if (!fallback.error) rows = fallback.data ?? [];
    }
  }

  return sortEventPartyProducts(rows
    .map(normalizeProduct)
    .filter((product): product is EventPartyProduct => Boolean(product && getProductEventId(product) === eventId)));
}

export async function fetchEventPartyPublicBundle(options: {
  eventId: string;
  tenantId?: string | null;
}): Promise<{ event: EventPartyEvent | null; products: EventPartyProduct[] }> {
  const eventId = options.eventId.trim();
  if (!eventId) return { event: null, products: [] };
  const tenantId = normalizeTenantId(options.tenantId);
  const supabase = getSupabaseClient();
  const fetchEvent = async (withTenant: boolean) => {
    let query = supabase.from("eventos").select(EVENT_SELECT_COLUMNS).eq("id", eventId);
    if (withTenant && tenantId) query = query.eq("tenant_id", tenantId);
    return query.maybeSingle();
  };
  let { data, error } = await fetchEvent(Boolean(tenantId));
  if (!data && tenantId && !error) {
    const fallback = await fetchEvent(false);
    data = fallback.data;
    error = fallback.error;
  }
  if (error) throw error;
  if (isEventVisibilityBlocked(data)) return { event: null, products: [] };
  const event = normalizeEvent(data);
  if (!event?.config.enabled) return { event, products: [] };
  const products = await fetchEventPartyProducts({ eventId, tenantId });
  return { event, products };
}

export async function fetchEventPartyAdminBundle(options: {
  eventId: string;
  tenantId?: string | null;
}): Promise<{ event: EventPartyEvent | null; products: EventPartyProduct[] }> {
  const eventId = options.eventId.trim();
  if (!eventId) return { event: null, products: [] };
  const tenantId = normalizeTenantId(options.tenantId);
  let query = getSupabaseClient().from("eventos").select(EVENT_SELECT_COLUMNS).eq("id", eventId);
  if (tenantId) query = query.eq("tenant_id", tenantId);
  let { data, error } = await query.maybeSingle();
  if (!data && tenantId && !error) {
    const fallback = await getSupabaseClient()
      .from("eventos")
      .select(EVENT_SELECT_COLUMNS)
      .eq("id", eventId)
      .maybeSingle();
    data = fallback.data;
    error = fallback.error;
  }
  if (error) throw error;
  const event = normalizeEvent(data);
  const products = await fetchEventPartyProducts({ eventId, tenantId, admin: true });
  return { event, products };
}

export async function fetchEventPartyOrders(options: {
  eventId: string;
  tenantId?: string | null;
  userId?: string | null;
  productIds?: string[];
  pageSize?: number;
  cursorId?: string | null;
}): Promise<EventPartyOrder[]> {
  const eventId = options.eventId.trim();
  if (!eventId) return [];
  const tenantId = normalizeTenantId(options.tenantId);
  const userId = asString(options.userId).trim();
  const productIds = Array.from(
    new Set((options.productIds ?? []).map((entry) => entry.trim()).filter(Boolean))
  );
  const supabase = getSupabaseClient();
  const pageSize = Math.min(500, Math.max(1, Math.floor(Number(options.pageSize ?? 500) || 500)));
  const offset = Math.max(0, Math.floor(Number(options.cursorId ?? 0) || 0));
  const runQuery = async (selectColumns: string, filterByEventIdColumn: boolean) => {
    let query = supabase
      .from("orders")
      .select(selectColumns)
      .order("createdAt", { ascending: false });
    if (tenantId) query = query.eq("tenant_id", tenantId);
    if (userId) query = query.eq("userId", userId);
    if (productIds.length > 0) query = query.in("productId", productIds);
    if (filterByEventIdColumn) query = query.eq("eventId", eventId);
    return query.range(offset, offset + pageSize - 1);
  };

  let rows: unknown[] = [];
  let columns = splitSelectColumns(ORDER_SELECT_COLUMNS);
  let filterByEventIdColumn = true;
  while (columns.length > 0) {
    const { data, error } = await runQuery(columns.join(","), filterByEventIdColumn);
    if (!error) {
      rows = data ?? [];
      break;
    }

    const missingColumn = extractMissingSchemaColumn(error);
    if (missingColumn && filterByEventIdColumn && missingColumn.toLowerCase() === "eventid") {
      filterByEventIdColumn = false;
      columns = splitSelectColumns(LEGACY_ORDER_SELECT_COLUMNS);
      continue;
    }
    if (missingColumn) {
      const nextColumns = columns.filter((column) => column.toLowerCase() !== missingColumn.toLowerCase());
      if (nextColumns.length > 0 && nextColumns.length < columns.length) {
        columns = nextColumns;
        continue;
      }
    }
    if (filterByEventIdColumn) {
      filterByEventIdColumn = false;
      columns = splitSelectColumns(LEGACY_ORDER_SELECT_COLUMNS);
      continue;
    }
    throw error;
  }

  return rows
    .map(normalizeOrder)
    .filter((order): order is EventPartyOrder => Boolean(order && getOrderEventId(order) === eventId));
}

export async function createEventPartyOrder(payload: {
  event: EventPartyEvent;
  product: EventPartyProduct;
  userId: string;
  userName: string;
  quantity: number;
  tenantId?: string | null;
}): Promise<{ id: string }> {
  const created = await createStoreOrder({
    userId: payload.userId,
    userName: payload.userName,
    productId: payload.product.id,
    productName: payload.product.nome,
    price: payload.product.preco,
    quantity: payload.quantity,
    tenantId: payload.tenantId || payload.event.tenantId || payload.product.tenantId,
    paymentConfig: mergeEventPartyPaymentConfig(payload.product.paymentConfig, payload.event.paymentConfig),
    extraData: {
      eventParty: {
        eventId: payload.event.id,
        eventTitle: payload.event.titulo,
        productId: payload.product.id,
        productName: payload.product.nome,
        section: getEventPartyProductSection(payload.product),
        voucherStatus: "pendente",
        voucherEntries: buildEventPartyVoucherEntries(payload.quantity, "pendente", {
          codePrefix: payload.userId.slice(0, 8).toUpperCase(),
        }),
        createdAt: new Date().toISOString(),
      },
    },
  });
  await syncEventPartyOrderStatementColumns({
    orderId: created.id,
    event: payload.event,
    product: payload.product,
  });
  return created;
}

export async function upsertEventPartyProduct(payload: {
  event: EventPartyEvent;
  productId?: string;
  tenantId?: string | null;
  data: {
    nome: string;
    preco: number;
    categoria: string;
    descricao?: string;
    img?: string;
    estoque?: number;
    section?: string;
    order?: number;
  };
}): Promise<void> {
  const now = new Date().toISOString();
  const section = payload.data.section?.trim().slice(0, 80) || payload.data.categoria || "Geral";
  const order =
    typeof payload.data.order === "number" && Number.isFinite(payload.data.order)
      ? Math.max(0, Math.floor(payload.data.order))
      : 9999;
  await upsertStoreProduct({
    productId: payload.productId,
    tenantId: payload.tenantId || payload.event.tenantId,
    data: {
      nome: payload.data.nome,
      preco: payload.data.preco,
      categoria: payload.data.categoria,
      descricao: payload.data.descricao || "",
      img: payload.data.img || payload.event.imagem,
      estoque: Math.max(0, Math.floor(payload.data.estoque ?? 0)),
      active: false,
      aprovado: true,
      status: "ativo",
      seller_type: "tenant",
      payment_config: payload.event.paymentConfig,
      data: {
        eventParty: {
          eventId: payload.event.id,
          eventTitle: payload.event.titulo,
          enabled: true,
          reusableInTenant: true,
          onlyEventStore: true,
          hiddenFromStore: true,
          section,
          order,
          createdAt: now,
          updatedAt: now,
        },
      },
    },
  });
}

export async function updateEventPartyProductMeta(payload: {
  product: EventPartyProduct;
  tenantId?: string | null;
  section?: string;
  order?: number;
}): Promise<void> {
  const productId = payload.product.id.trim();
  if (!productId) return;
  const tenantId = normalizeTenantId(payload.tenantId);
  const now = new Date().toISOString();
  const previousData = asObject(payload.product.data) ?? {};
  const previousEventParty = asObject(previousData.eventParty) ?? {};
  const nextEventParty = {
    ...previousEventParty,
    ...(payload.section !== undefined ? { section: payload.section.trim().slice(0, 80) || "Geral" } : {}),
    ...(payload.order !== undefined
      ? { order: Math.max(0, Math.floor(Number(payload.order) || 0)) }
      : {}),
    updatedAt: now,
  };

  let query = getSupabaseClient()
    .from("produtos")
    .update({
      data: {
        ...previousData,
        eventParty: nextEventParty,
      },
      updatedAt: now,
    })
    .eq("id", productId);
  if (tenantId) query = query.eq("tenant_id", tenantId);
  const { error } = await query;
  if (error) throw error;
}

export async function createManualEventPartyOrder(payload: {
  event: EventPartyEvent;
  product: EventPartyProduct;
  userId?: string | null;
  userName: string;
  quantity: number;
  tenantId?: string | null;
  createdByUserId?: string | null;
  createdByName: string;
  manualCode?: string | null;
  externalNumber?: string | null;
  manualCustomer?: {
    cpf?: string | null;
    telefone?: string | null;
    email?: string | null;
    ra?: string | null;
  };
}): Promise<{ id: string }> {
  const quantity = Math.max(1, Math.floor(Number(payload.quantity) || 1));
  const total = Number((payload.product.preco * quantity).toFixed(2));
  const tenantId = normalizeTenantId(payload.tenantId) || payload.event.tenantId || payload.product.tenantId;
  const now = new Date().toISOString();
  const cleanUserName = payload.userName.trim().slice(0, 120) || "Compra manual";
  const cleanExternalNumber = asString(payload.externalNumber).trim().slice(0, 80);
  const cleanManualCode = asString(payload.manualCode).trim().slice(0, 80);
  const cleanUserId =
    asString(payload.userId).trim() ||
    (cleanExternalNumber ? `manual-${cleanExternalNumber}` : `manual-${Date.now()}`);
  const operatorName = payload.createdByName.trim().slice(0, 120) || "Admin";
  const voucherEntries = buildEventPartyVoucherEntries(quantity, "ativo", {
    manualNumber: cleanManualCode,
    codePrefix: cleanExternalNumber || cleanUserId.slice(0, 8).toUpperCase(),
  });

  let insertPayload: Row = {
      ...(tenantId ? { tenant_id: tenantId } : {}),
      userId: cleanUserId,
      userName: cleanUserName,
      productId: payload.product.id,
      productName: payload.product.nome,
      price: total,
      quantidade: quantity,
      status: "approved",
      approvedBy: operatorName,
      eventId: payload.event.id,
      eventItemType: "produto",
      eventItemName: payload.product.nome,
      eventLoteNome: "-",
      eventItemCategory: getEventPartyProductSection(payload.product),
      eventApprovalAt: now,
      eventApprovalMethod: "manual",
      eventDiscountValue: "R$ 0,00",
      eventDiscountKind: "",
      eventDiscountSource: "",
      eventCreatedManually: true,
      eventCreatedByName: operatorName,
      payment_config: mergeEventPartyPaymentConfig(payload.product.paymentConfig, payload.event.paymentConfig),
      data: {
        eventParty: {
          eventId: payload.event.id,
          eventTitle: payload.event.titulo,
          productId: payload.product.id,
          productName: payload.product.nome,
          ...(cleanManualCode ? { manualCode: cleanManualCode, manualNumber: cleanManualCode } : {}),
          ...(cleanExternalNumber ? { externalNumber: cleanExternalNumber } : {}),
          voucherStatus: "ativo",
          voucherEntries,
          manualOrder: true,
          createdManually: true,
          approvalMethod: "manual",
          manualCustomer: {
            cpf: asString(payload.manualCustomer?.cpf).trim(),
            telefone: asString(payload.manualCustomer?.telefone).trim(),
            email: asString(payload.manualCustomer?.email).trim(),
            ra: asString(payload.manualCustomer?.ra).trim(),
            externalNumber: cleanExternalNumber,
          },
          createdByUserId: asString(payload.createdByUserId).trim(),
          createdByName: operatorName,
          approvedAt: now,
          createdAt: now,
        },
      },
      createdAt: now,
      updatedAt: now,
    };

  let createdId = "";
  while (Object.keys(insertPayload).length > 0) {
    const { data, error } = await getSupabaseClient()
      .from("orders")
      .insert(insertPayload)
      .select("id")
      .single();
    if (!error) {
      createdId = asString(asObject(data)?.id);
      break;
    }

    const fallbackColumn = extractSchemaFallbackColumn(error);
    if (!fallbackColumn || !Object.prototype.hasOwnProperty.call(insertPayload, fallbackColumn)) {
      throw error;
    }
    const nextPayload = { ...insertPayload };
    delete nextPayload[fallbackColumn];
    insertPayload = nextPayload;
  }
  return { id: createdId };
}

export async function updateEventPartyOrder(payload: {
  order: EventPartyOrder;
  quantity: number;
  editedByName: string;
}): Promise<void> {
  const orderId = payload.order.id.trim();
  if (!orderId) return;
  const quantity = Math.max(1, Math.floor(Number(payload.quantity) || 1));
  const unitPrice =
    payload.order.quantidade > 0
      ? (payload.order.total || payload.order.price) / payload.order.quantidade
      : payload.order.price;
  const total = Number((Math.max(0, unitPrice) * quantity).toFixed(2));
  const now = new Date().toISOString();
  const orderData = asObject(payload.order.data) ?? {};
  const eventParty = asObject(orderData.eventParty) ?? {};
  await updateOrderWithSchemaFallback(orderId, {
    quantidade: quantity,
    price: total,
    data: {
      ...orderData,
      eventParty: {
        ...eventParty,
        editedAt: now,
        editedByName: payload.editedByName.trim().slice(0, 120) || "Admin",
      },
    },
    updatedAt: now,
  });
}

export async function markEventPartyOrderDelivered(payload: {
  order: EventPartyOrder;
  operatorUserId?: string | null;
  operatorName: string;
}): Promise<void> {
  const orderId = payload.order.id.trim();
  if (!orderId) return;
  const now = new Date().toISOString();
  const operatorName = payload.operatorName.trim().slice(0, 120) || "Admin";
  const operatorUserId = asString(payload.operatorUserId).trim();
  const orderData = asObject(payload.order.data) ?? {};
  const eventParty = asObject(orderData.eventParty) ?? {};
  const nextVoucherEntries = getEventPartyVoucherEntries(payload.order).map((entry) =>
    entry.status === "utilizado" || entry.status === "inativo"
      ? entry
      : {
          ...entry,
          status: "utilizado" as const,
          usedAt: now,
          usedByUserId: operatorUserId,
          usedByUserName: operatorName,
          usedMethod: "manual",
          withdrawalAt: now,
          withdrawalByUserId: operatorUserId,
          withdrawalByUserName: operatorName,
          withdrawalMethod: "manual",
        }
  );

  await updateOrderWithSchemaFallback(orderId, {
    status: "delivered",
    eventCheckinAt: now,
    eventCheckinByUserId: operatorUserId,
    eventCheckinByUserName: operatorName,
    eventCheckinMethod: "manual",
    data: {
      ...orderData,
      eventParty: {
        ...eventParty,
        voucherStatus: "utilizado",
        voucherEntries: nextVoucherEntries,
        usedAt: now,
        usedByUserId: operatorUserId,
        usedByUserName: operatorName,
        usedMethod: "manual",
        withdrawalAt: now,
        withdrawalByUserId: operatorUserId,
        withdrawalByUserName: operatorName,
        withdrawalMethod: "manual",
      },
    },
    updatedAt: now,
  });
}

export async function deleteEventPartyOrder(payload: {
  orderId: string;
  tenantId?: string | null;
}): Promise<void> {
  const orderId = payload.orderId.trim();
  if (!orderId) return;
  const tenantId = normalizeTenantId(payload.tenantId);
  let query = getSupabaseClient().from("orders").delete().eq("id", orderId);
  if (tenantId) query = query.eq("tenant_id", tenantId);
  const { error } = await query;
  if (error) throw error;
}
