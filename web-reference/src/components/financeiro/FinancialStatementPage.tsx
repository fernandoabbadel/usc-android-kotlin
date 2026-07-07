"use client";

import { useEffect, useMemo, useState, type ReactNode } from "react";
import Image from "next/image";
import Link from "next/link";
import {
  ArrowLeft,
  BarChart3,
  Download,
  FileSpreadsheet,
  Loader2,
  Printer,
  Search,
  ShieldAlert,
  Wallet,
} from "lucide-react";

import { useAuth } from "@/context/AuthContext";
import { useTenantTheme } from "@/context/TenantThemeContext";
import { getSupabaseClient } from "@/lib/supabase";
import { asObject, asString, type Row } from "@/lib/supabaseData";
import { withTenantSlug } from "@/lib/tenantRouting";
import {
  fetchCurrentMiniVendorProfile,
  type MiniVendorProfile,
} from "@/lib/miniVendorService";

type FinancialScopeType = "tenant" | "league" | "commission" | "directory" | "mini_vendor";
type FinancialType =
  | "planos"
  | "ingressos"
  | "produtos_loja"
  | "produtos_modo_vendas";
type StatusGroup = "aprovado" | "pendente" | "cancelado" | "recusado" | "outro";
type AlphabetGroup = "todos" | "a-f" | "g-l" | "m-r" | "s-z";

type LoadedData = {
  events: Row[];
  tickets: Row[];
  products: Row[];
  orders: Row[];
  users: Row[];
  entities: Row[];
  planRequests: Row[];
};

type EntityScopeType = Exclude<FinancialScopeType, "tenant" | "mini_vendor">;

type EntityScopeMeta = {
  scopeType: EntityScopeType;
  name: string;
};

type EventScopeLink = EntityScopeMeta & {
  ownerId: string;
};

type StatementRow = {
  id: string;
  tipo: FinancialType;
  tipoLabel: string;
  ficha: string;
  pulseiraRa: string;
  item: string;
  lote: string;
  categoria: string;
  qtd: number;
  pedidoAt: Date | null;
  cliente: string;
  aprovacaoAt: Date | null;
  aprovadoPor: string;
  metodoAprovacao: string;
  fontePagamento: string;
  entradaAt: Date | null;
  entradaPor: string;
  metodoEntradaAprovacao: string;
  statusQr: string;
  codigoQr: string;
  transferencia: string;
  transferenciaAt: Date | null;
  metodoEntradaRetirada: string;
  valor: number;
  custo: number;
  desconto: number;
  origemDesconto: string;
  fonte: string;
  status: string;
  statusGroup: StatusGroup;
  destinoHref: string;
  destinoLabel: string;
  eventId: string;
  sortAt: number;
};

const PAGE_SIZE = 20;
const ALPHABET_GROUPS: Array<{ id: AlphabetGroup; label: string }> = [
  { id: "a-f", label: "A-F" },
  { id: "g-l", label: "G-L" },
  { id: "m-r", label: "M-R" },
  { id: "s-z", label: "S-Z" },
  { id: "todos", label: "Todos" },
];

const TYPE_OPTIONS: Array<{ id: "todos" | FinancialType; label: string }> = [
  { id: "todos", label: "Todos os tipos" },
  { id: "planos", label: "Planos" },
  { id: "ingressos", label: "Ingressos" },
  { id: "produtos_loja", label: "Produtos Loja" },
  { id: "produtos_modo_vendas", label: "Produtos Modo Vendas" },
];

const STATUS_OPTIONS: Array<{ id: "todos" | StatusGroup; label: string }> = [
  { id: "todos", label: "Todos os status" },
  { id: "aprovado", label: "Aprovado" },
  { id: "pendente", label: "Pendente" },
  { id: "cancelado", label: "Cancelado" },
  { id: "recusado", label: "Recusado" },
  { id: "outro", label: "Outros" },
];

const emptyLoadedData: LoadedData = {
  events: [],
  tickets: [],
  products: [],
  orders: [],
  users: [],
  entities: [],
  planRequests: [],
};

const currencyFormatter = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});

const numberFormatter = new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 0 });

const removeAccents = (value: string): string =>
  value.normalize("NFD").replace(/[\u0300-\u036f]/g, "");

const normalizeText = (value: unknown): string =>
  removeAccents(asString(value).trim().toLowerCase());

const parseNumber = (value: unknown, fallback = 0): number => {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string") {
    const normalized = value
      .replace(/[^\d,.-]/g, "")
      .replace(/\.(?=\d{3}(?:\D|$))/g, "")
      .replace(",", ".");
    const parsed = Number(normalized);
    return Number.isFinite(parsed) ? parsed : fallback;
  }
  return fallback;
};

const parseDate = (value: unknown): Date | null => {
  if (value instanceof Date && Number.isFinite(value.getTime())) return value;
  if (typeof value === "object" && value !== null) {
    const toDate = (value as { toDate?: unknown }).toDate;
    if (typeof toDate === "function") {
      const parsed = toDate.call(value);
      if (parsed instanceof Date && Number.isFinite(parsed.getTime())) return parsed;
    }
  }
  if (typeof value === "string") {
    const trimmed = value.trim();
    const brMatch = trimmed.match(
      /^(\d{1,2})\/(\d{1,2})\/(\d{4})(?:\s+(\d{1,2}):(\d{2})(?::(\d{2}))?)?$/
    );
    if (brMatch) {
      const [, day, month, year, hour = "0", minute = "0", second = "0"] = brMatch;
      const parsed = new Date(
        Number(year),
        Number(month) - 1,
        Number(day),
        Number(hour),
        Number(minute),
        Number(second)
      );
      return Number.isFinite(parsed.getTime()) ? parsed : null;
    }
  }
  if (typeof value === "string" || typeof value === "number") {
    const parsed = new Date(value);
    return Number.isFinite(parsed.getTime()) ? parsed : null;
  }
  return null;
};

const formatCurrency = (value: number): string =>
  currencyFormatter.format(Number.isFinite(value) ? value : 0);

const formatNumber = (value: number): string =>
  numberFormatter.format(Number.isFinite(value) ? value : 0);

const formatDate = (value: Date | null): string =>
  value ? value.toLocaleDateString("pt-BR") : "-";

const formatTime = (value: Date | null): string =>
  value ? value.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" }) : "-";

const csvEscape = (value: unknown): string => {
  const text =
    typeof value === "number"
      ? String(value).replace(".", ",")
      : value instanceof Date
        ? value.toISOString()
        : String(value ?? "");
  return `"${text.replace(/"/g, '""')}"`;
};

const extractMissingSchemaColumn = (error: unknown): string => {
  if (!error || typeof error !== "object") return "";
  const raw = error as { message?: unknown; details?: unknown; hint?: unknown };
  const text = [raw.message, raw.details, raw.hint]
    .map((entry) => (typeof entry === "string" ? entry : ""))
    .join(" ");
  const patterns = [
    /column\s+(?:(?:"?[\w]+"?)\.)?"?([\w]+)"?\s+does not exist/i,
    /could not find the ['"]?([\w]+)['"]? column/i,
  ];
  for (const pattern of patterns) {
    const match = text.match(pattern);
    if (match?.[1]) return String(match[1]);
  }
  return "";
};

async function queryRows(options: {
  table: string;
  select: string;
  tenantId: string;
  orderColumn?: string;
  limit: number;
  filters?: Array<{ column: string; value: string }>;
}): Promise<Row[]> {
  const supabase = getSupabaseClient();
  let selectColumns = options.select
    .split(",")
    .map((entry) => entry.trim())
    .filter(Boolean);
  let canOrder = Boolean(options.orderColumn?.trim());
  let canFilterTenant = options.tenantId.trim().length > 0;
  const activeFilters = [...(options.filters ?? [])];

  while (selectColumns.length > 0) {
    let query = supabase.from(options.table).select(selectColumns.join(",")).limit(options.limit);
    if (canFilterTenant) query = query.eq("tenant_id", options.tenantId);
    activeFilters.forEach((filter) => {
      query = query.eq(filter.column, filter.value);
    });
    if (canOrder && options.orderColumn) {
      query = query.order(options.orderColumn, { ascending: false });
    }

    const { data, error } = await query;
    if (!error) return Array.isArray(data) ? (data as unknown as Row[]) : [];

    const missingColumn = extractMissingSchemaColumn(error).trim().toLowerCase();
    if (missingColumn) {
      if (canFilterTenant && missingColumn === "tenant_id") {
        canFilterTenant = false;
        continue;
      }
      if (canOrder && missingColumn === options.orderColumn?.trim().toLowerCase()) {
        canOrder = false;
        continue;
      }
      const nextColumns = selectColumns.filter(
        (column) => column.trim().toLowerCase() !== missingColumn
      );
      if (nextColumns.length > 0 && nextColumns.length < selectColumns.length) {
        selectColumns = nextColumns;
        continue;
      }
    }
    if (canOrder) {
      canOrder = false;
      continue;
    }
    throw error;
  }
  return [];
}

async function loadStatementData(tenantId: string, includePlans: boolean): Promise<LoadedData> {
  const [events, tickets, products, orders, users, entities, planRequests] = await Promise.all([
    queryRows({
      table: "eventos",
      select:
        "id,titulo,nome,data,hora,lotes,stats,data_extra,payment_config,status,tipo,categoria,leagueId,ligaId,directoryId,diretorioId,commissionId,comissaoId,custo,custos,cost,totalCost,tenant_id,createdAt",
      tenantId,
      orderColumn: "data",
      limit: 1200,
    }),
    queryRows({
      table: "solicitacoes_ingressos",
      select:
        "id,tenant_id,eventoId,eventoNome,userId,userName,userTurma,email,telefone,phone,status,loteId,loteNome,quantidade,valorUnitario,valorTotal,payment_config,paymentSource,paymentMethod,source,data,dataSolicitacao,dataAprovacao,dataPagamento,paymentDate,paidAt,createdAt,updatedAt,aprovadoPor,metodo,itemType,itemName,itemCategory,approvalMethod,checkinAt,checkinByUserId,checkinByUserName,checkinMethod,checkinNote,discountValue,discountKind,discountSource,transferAt,transferFromUserId,transferFromUserName,transferToUserId,transferToUserName,transferByUserId,transferByUserName,transferHistory",
      tenantId,
      orderColumn: "dataSolicitacao",
      limit: 10000,
    }),
    queryRows({
      table: "produtos",
      select:
        "id,tenant_id,nome,productName,categoria,lote,preco,price,custo,cost,custos,estoque,status,active,aprovado,seller_type,seller_id,seller_name,payment_config,data,createdAt,eventId,eventoId,leagueId,ligaId,directoryId,diretorioId,commissionId,comissaoId",
      tenantId,
      orderColumn: "createdAt",
      limit: 5000,
    }),
    queryRows({
      table: "orders",
      select:
        "id,tenant_id,userId,userName,userTurma,email,telefone,phone,productId,productName,price,total,quantidade,itens,data,status,approvedBy,payment_config,paymentSource,paymentMethod,source,createdAt,updatedAt,seller_type,seller_id,seller_name,eventId,eventoId,eventItemType,eventItemName,eventLoteNome,eventItemCategory,eventApprovalAt,eventApprovalMethod,eventCheckinAt,eventCheckinByUserId,eventCheckinByUserName,eventCheckinMethod,eventDiscountValue,eventDiscountKind,eventDiscountSource,eventCreatedManually,eventCreatedByUserId,eventCreatedByName,custo,cost",
      tenantId,
      orderColumn: "createdAt",
      limit: 10000,
    }),
    queryRows({
      table: "users",
      select: "uid,nome,turma,email,telefone,phone,ra,cpf,tenant_id,createdAt",
      tenantId,
      orderColumn: "createdAt",
      limit: 8000,
    }),
    queryRows({
      table: "ligas_config",
      select:
        "id,nome,sigla,category,categoria,turmaId,data,membros,membrosIds,managerUserIds,eventos,tenant_id,createdAt",
      tenantId,
      orderColumn: "createdAt",
      limit: 2000,
    }),
    includePlans
      ? queryRows({
          table: "solicitacoes_adesao",
          select:
            "id,tenant_id,userId,userName,userTurma,planoId,planoNome,valor,comprovanteUrl,metodo,status,dataSolicitacao,dataAprovacao,aprovadoPor,approvalMethod,paymentSource,discountValue,discountSource,updatedAt,data",
          tenantId,
          orderColumn: "dataSolicitacao",
          limit: 5000,
        })
      : Promise.resolve([]),
  ]);

  return { events, tickets, products, orders, users, entities, planRequests };
}

function eventId(row: Row): string {
  return asString(row.id || row.eventId || row.eventoId || row.globalEventId).trim();
}

function eventName(row?: Row): string {
  return asString(row?.titulo || row?.nome || row?.name || row?.eventoNome).trim() || "Evento";
}

function ticketEventId(row: Row): string {
  return asString(row.eventoId || row.eventId || row.event_id || row.globalEventId).trim();
}

function productEventId(row: Row): string {
  const data = asObject(row.data) ?? {};
  const eventParty = asObject(data.eventParty) ?? {};
  return asString(row.eventId || row.eventoId || eventParty.eventId).trim();
}

function orderEventId(row: Row): string {
  const data = asObject(row.data) ?? {};
  const eventParty = asObject(data.eventParty) ?? {};
  return asString(row.eventId || row.eventoId || eventParty.eventId).trim();
}

function orderProductId(row: Row): string {
  return asString(row.productId || row.produtoId || row.product_id || row.produto_id).trim();
}

function orderItemName(row: Row, productsById: Map<string, Row>): string {
  const product = productsById.get(orderProductId(row));
  return asString(row.eventItemName || row.productName || product?.nome).trim() || "Produto";
}

function orderItemCategory(row: Row, productsById: Map<string, Row>): string {
  const data = asObject(row.data) ?? {};
  const eventParty = asObject(data.eventParty) ?? {};
  const product = productsById.get(orderProductId(row));
  return asString(row.eventItemCategory || eventParty.section || product?.categoria).trim() || "Sem categoria";
}

function entityScopeType(row: Row): EntityScopeType {
  const data = asObject(row.data) ?? {};
  const category = normalizeText(row.category || row.categoria || data.category || data.categoria || data.tipo);
  if (category.includes("diretorio")) return "directory";
  if (category.includes("comissao") || category.includes("comiss")) return "commission";
  if (asString(row.turmaId || data.turmaId).trim()) return "commission";
  return "league";
}

function buildEntityScopeIndex(rows: Row[], forced?: { id: string; scopeType: EntityScopeType }): Map<string, EntityScopeMeta> {
  const index = new Map<string, EntityScopeMeta>();
  rows.forEach((row) => {
    const id = asString(row.id).trim();
    if (!id) return;
    index.set(id, {
      scopeType: forced?.id === id ? forced.scopeType : entityScopeType(row),
      name: asString(row.sigla || row.nome || row.name).trim() || id,
    });
  });
  return index;
}

function eventIdFromLink(value: unknown): string {
  const raw = asString(value).trim();
  if (!raw) return "";
  const cleanPath = raw.split("?")[0]?.split("#")[0] || raw;
  const parts = cleanPath.split("/").map((part) => part.trim()).filter(Boolean);
  const eventIndex = parts.findIndex((part) => normalizeText(part) === "eventos");
  const candidate = eventIndex >= 0 ? parts[eventIndex + 1] : parts[parts.length - 1];
  try {
    return decodeURIComponent(candidate || "").trim();
  } catch {
    return asString(candidate).trim();
  }
}

function linkedEventIdsFromEntityEvent(value: unknown): string[] {
  const row = asObject(value);
  if (!row) return [];
  return Array.from(
    new Set(
      [row.globalEventId, row.eventId, row.eventoId, row.id, eventIdFromLink(row.linkEvento)]
        .map((entry) => asString(entry).trim())
        .filter(Boolean)
    )
  );
}

function buildEventScopeIndex(
  rows: Row[],
  entityIndex: Map<string, EntityScopeMeta>
): Map<string, EventScopeLink> {
  const index = new Map<string, EventScopeLink>();
  rows.forEach((entity) => {
    const ownerId = asString(entity.id).trim();
    const owner = entityIndex.get(ownerId);
    if (!ownerId || !owner) return;
    const data = asObject(entity.data) ?? {};
    const events = [entity.eventos, data.eventos, data.events]
      .flatMap((candidate) => (Array.isArray(candidate) ? candidate : []));
    events.forEach((eventRow) => {
      linkedEventIdsFromEntityEvent(eventRow).forEach((linkedEventId) => {
        index.set(linkedEventId, { ...owner, ownerId });
      });
    });
  });
  return index;
}

function declaredExternalScopeType(row?: Row | null): EntityScopeType | null {
  if (!row) return null;
  const data = asObject(row.data) ?? {};
  const dataExtra = asObject(row.data_extra) ?? {};
  const stats = asObject(row.stats) ?? {};
  const eventParty = asObject(dataExtra.eventParty) ?? {};
  const values = [
    row.scope_type,
    row.scopeType,
    row.tipo,
    row.categoria,
    row.category,
    row.seller_type,
    row.sellerType,
    data.scope_type,
    data.scopeType,
    data.tipo,
    data.categoria,
    data.category,
    data.seller_type,
    data.sellerType,
    dataExtra.scope_type,
    dataExtra.scopeType,
    dataExtra.tipo,
    dataExtra.categoria,
    dataExtra.category,
    stats.scope_type,
    stats.scopeType,
    stats.tipo,
    stats.categoria,
    stats.category,
    eventParty.scope_type,
    eventParty.scopeType,
    eventParty.tipo,
    eventParty.categoria,
    eventParty.category,
  ].map(normalizeText);
  if (values.some((value) => value.includes("diretorio") || value.includes("directory"))) return "directory";
  if (values.some((value) => value.includes("comissao") || value.includes("commission"))) return "commission";
  if (values.some((value) => value === "liga" || value.includes("league"))) return "league";
  return null;
}

function emptyScopeIds(): Record<EntityScopeType, string[]> {
  return { league: [], directory: [], commission: [] };
}

function addScopedId(
  target: Record<EntityScopeType, string[]>,
  entityIndex: Map<string, EntityScopeMeta>,
  fallbackType: EntityScopeType,
  value: unknown
): void {
  const id = asString(value).trim();
  if (!id) return;
  const scopeType = entityIndex.get(id)?.scopeType ?? fallbackType;
  target[scopeType].push(id);
}

function uniqueScopeIds(ids: Record<EntityScopeType, string[]>): Record<EntityScopeType, string[]> {
  return {
    league: Array.from(new Set(ids.league.filter(Boolean))),
    directory: Array.from(new Set(ids.directory.filter(Boolean))),
    commission: Array.from(new Set(ids.commission.filter(Boolean))),
  };
}

function eventScopeIds(
  row: Row,
  entityIndex: Map<string, EntityScopeMeta>,
  eventScopeIndex: Map<string, EventScopeLink>
): Record<EntityScopeType, string[]> {
  const stats = asObject(row.stats) ?? {};
  const dataExtra = asObject(row.data_extra) ?? {};
  const eventParty = asObject(dataExtra.eventParty) ?? {};
  const scopeIds = emptyScopeIds();
  const linkedScope = eventScopeIndex.get(eventId(row));
  if (linkedScope) addScopedId(scopeIds, entityIndex, linkedScope.scopeType, linkedScope.ownerId);
  [row.leagueId, row.ligaId, stats.leagueId, stats.ligaId, stats.sellerId, eventParty.leagueId].forEach((value) =>
    addScopedId(scopeIds, entityIndex, "league", value)
  );
  [row.directoryId, row.diretorioId, stats.directoryId, stats.diretorioId, eventParty.directoryId].forEach((value) =>
    addScopedId(scopeIds, entityIndex, "directory", value)
  );
  [row.commissionId, row.comissaoId, stats.commissionId, stats.comissaoId, eventParty.commissionId].forEach((value) =>
    addScopedId(scopeIds, entityIndex, "commission", value)
  );
  addScopedId(scopeIds, entityIndex, "league", stats.collectiveId);
  return uniqueScopeIds(scopeIds);
}

function rowScopeIds(
  row: Row,
  relatedEvent: Row | undefined,
  entityIndex: Map<string, EntityScopeMeta>,
  eventScopeIndex: Map<string, EventScopeLink>
): Record<EntityScopeType, string[]> {
  const data = asObject(row.data) ?? {};
  const eventParty = asObject(data.eventParty) ?? {};
  const sellerType = normalizeText(row.seller_type || row.sellerType);
  const sellerId = asString(row.seller_id || row.sellerId).trim();
  const scopeIds = relatedEvent ? eventScopeIds(relatedEvent, entityIndex, eventScopeIndex) : emptyScopeIds();

  [row.leagueId, row.ligaId, data.leagueId, data.ligaId, eventParty.leagueId].forEach((value) =>
    addScopedId(scopeIds, entityIndex, "league", value)
  );
  [row.directoryId, row.diretorioId, data.directoryId, data.diretorioId, eventParty.directoryId].forEach((value) =>
    addScopedId(scopeIds, entityIndex, "directory", value)
  );
  [row.commissionId, row.comissaoId, data.commissionId, data.comissaoId, eventParty.commissionId].forEach((value) =>
    addScopedId(scopeIds, entityIndex, "commission", value)
  );

  if (sellerType.includes("directory") || sellerType.includes("diretorio")) {
    addScopedId(scopeIds, entityIndex, "directory", sellerId);
  } else if (sellerType.includes("commission") || sellerType.includes("comissao")) {
    addScopedId(scopeIds, entityIndex, "commission", sellerId);
  } else if (sellerType.includes("league") || sellerType.includes("liga")) {
    addScopedId(scopeIds, entityIndex, "league", sellerId);
  }

  return uniqueScopeIds(scopeIds);
}

function hasExternalScope(
  row: Row,
  relatedEvent: Row | undefined,
  entityIndex: Map<string, EntityScopeMeta>,
  eventScopeIndex: Map<string, EventScopeLink>
): boolean {
  const ids = rowScopeIds(row, relatedEvent, entityIndex, eventScopeIndex);
  return Boolean(
    ids.league.length ||
      ids.directory.length ||
      ids.commission.length ||
      declaredExternalScopeType(row) ||
      declaredExternalScopeType(relatedEvent)
  );
}

function rowMatchesScope(options: {
  row: Row;
  relatedEvent?: Row;
  scopeType: FinancialScopeType;
  scopeId?: string;
  entityIndex: Map<string, EntityScopeMeta>;
  eventScopeIndex: Map<string, EventScopeLink>;
  productsById?: Map<string, Row>;
}): boolean {
  const cleanScopeId = asString(options.scopeId).trim();
  const sellerType = normalizeText(options.row.seller_type || options.row.sellerType);
  const sellerId = asString(options.row.seller_id || options.row.sellerId).trim();

  if (options.scopeType === "mini_vendor") {
    return Boolean(cleanScopeId && sellerType === "mini_vendor" && sellerId === cleanScopeId);
  }

  if (options.scopeType === "tenant") {
    if (sellerType === "mini_vendor") return false;
    if (["league", "liga", "commission", "comissao", "directory", "diretorio"].includes(sellerType)) return false;
    return !hasExternalScope(options.row, options.relatedEvent, options.entityIndex, options.eventScopeIndex);
  }

  if (!cleanScopeId) return false;
  const ids = rowScopeIds(options.row, options.relatedEvent, options.entityIndex, options.eventScopeIndex);
  if (ids[options.scopeType].includes(cleanScopeId)) return true;
  if (sellerId === cleanScopeId) {
    if (options.scopeType === "league") return ["league", "liga", "tenant", ""].includes(sellerType);
    if (options.scopeType === "commission") return sellerType.includes("commission") || sellerType.includes("comissao");
    if (options.scopeType === "directory") return sellerType.includes("directory") || sellerType.includes("diretorio");
  }
  return false;
}

function statusGroup(value: unknown): StatusGroup {
  const normalized = normalizeText(value);
  if (["approved", "aprovado", "aprovada", "paid", "pago", "confirmado", "entregue", "validado", "redeemed"].includes(normalized)) {
    return "aprovado";
  }
  if (["rejected", "recusado", "recusada", "reprovado", "reprovada", "denied"].includes(normalized)) {
    return "recusado";
  }
  if (["cancelled", "canceled", "cancelado", "cancelada", "expired", "expirado", "estornado", "refunded"].includes(normalized)) {
    return "cancelado";
  }
  if (normalized.includes("pend") || normalized.includes("aguard") || normalized.includes("analise") || normalized.includes("review")) {
    return "pendente";
  }
  return "outro";
}

function readTicketEntries(row: Row): Row[] {
  const config = asObject(row.payment_config ?? row.paymentConfig ?? row.data) ?? {};
  const candidates = [config.ticketEntries, config.tickets, config.ingressos, row.ticketEntries, row.tickets, row.ingressos];
  for (const candidate of candidates) {
    if (Array.isArray(candidate)) {
      return candidate.map((entry) => asObject(entry)).filter((entry): entry is Row => Boolean(entry));
    }
  }
  return [];
}

function isTransferredEntry(entry: Row): boolean {
  const status = normalizeText(entry.status || entry.situacao);
  return status.includes("transfer");
}

function isCancelledEntry(entry: Row): boolean {
  const status = normalizeText(entry.status || entry.situacao);
  return status.includes("cancel") || status.includes("expir") || status.includes("inativo");
}

function entryScannedAt(entry: Row): Date | null {
  return parseDate(entry.scannedAt ?? entry.scanAt ?? entry.checkedAt ?? entry.checkinAt ?? entry.lidoEm ?? entry.dataCheckin);
}

function isTicketEntryCheckedIn(entry: Row): boolean {
  const status = normalizeText(entry.status || entry.scanStatus || entry.situacao);
  return Boolean(entryScannedAt(entry)) || status.includes("lido") || status.includes("scan") || status.includes("check");
}

function ticketQuantity(row: Row): number {
  const explicit = Math.floor(parseNumber(row.quantidade ?? row.quantity ?? row.qtd, 0));
  return explicit > 0 ? explicit : Math.max(readTicketEntries(row).length, 1);
}

function ticketValue(row: Row): number {
  return parseNumber(row.valorTotal ?? row.total ?? row.valor ?? row.amount ?? row.preco, 0);
}

function ticketDiscount(row: Row): number {
  const data = asObject(row.data) ?? {};
  return parseNumber(row.discountValue ?? row.desconto ?? data.discountValue ?? data.desconto, 0);
}

function ticketPurchaseDate(row: Row): Date | null {
  return parseDate(row.dataSolicitacao ?? row.createdAt ?? row.created_at ?? row.insertedAt);
}

function ticketApprovalDate(row: Row): Date | null {
  return parseDate(row.dataAprovacao ?? row.approvedAt ?? row.aprovadoEm ?? row.updatedAt);
}

function ticketCheckinDate(row: Row): Date | null {
  const direct = parseDate(row.checkinAt ?? row.checkedAt ?? row.scannedAt ?? row.dataCheckin);
  if (direct) return direct;
  return readTicketEntries(row)
    .map((entry) => entryScannedAt(entry))
    .filter((date): date is Date => Boolean(date))
    .sort((left, right) => right.getTime() - left.getTime())[0] ?? null;
}

function ticketLotName(row: Row): string {
  return asString(row.loteNome || row.lote || row.ticketName || row.tipoIngresso || row.categoria).trim() || "Sem lote";
}

function ticketItemName(row: Row): string {
  return asString(row.itemName || row.ticketName || row.loteNome || row.lote || row.tipoIngresso).trim() || "Ingresso";
}

function ticketItemCategory(row: Row): string {
  const data = asObject(row.data) ?? {};
  return asString(row.itemCategory || data.itemCategory || data.categoria || data.loteCategoria || ticketLotName(row)).trim() || "Ingresso";
}

function ticketApprovalMethod(row: Row): string {
  const method = normalizeText(row.approvalMethod || row.metodo || row.aprovacaoMetodo || row.approvalSource);
  if (method.includes("manual")) return "Manual";
  if (method.includes("qr")) return "QR code";
  if (method.includes("pix")) return "Pix validado";
  if (method.includes("auto")) return "Automático";
  if (method.includes("transfer")) return "Transferência";
  return asString(row.aprovadoPor).trim() ? "Manual" : "-";
}

function ticketPaymentSource(row: Row): string {
  const config = asObject(row.payment_config ?? row.paymentConfig) ?? {};
  const data = asObject(row.data) ?? {};
  return (
    asString(row.paymentSource || row.paymentMethod || data.paymentSource || data.paymentMethod || config.method || config.provider).trim() ||
    (row.payment_config ? "Configuração de pagamento" : "-")
  );
}

function ticketQrStatus(row: Row): string {
  const entries = readTicketEntries(row);
  if (!entries.length) return "-";
  const checked = entries.filter(isTicketEntryCheckedIn).length;
  const transferred = entries.filter(isTransferredEntry).length;
  const cancelled = entries.filter(isCancelledEntry).length;
  if (checked >= entries.length) return "Usado";
  if (checked > 0) return "Parcialmente usado";
  if (transferred >= entries.length) return "Transferido";
  if (cancelled >= entries.length) return "Cancelado";
  return "Ativo";
}

function ticketCodes(row: Row): string {
  const codes = readTicketEntries(row)
    .flatMap((entry) => [entry.token, entry.id, entry.codigo, entry.qrCode, entry.code])
    .map((value) => asString(value).trim())
    .filter(Boolean);
  return Array.from(new Set(codes)).join(" | ") || "-";
}

function latestTicketTransfer(row: Row): { label: string; at: Date | null } {
  const data = asObject(row.data) ?? {};
  const audits: Row[] = [];
  const pushAudit = (value: unknown) => {
    const audit = asObject(value);
    if (audit) audits.push(audit);
  };
  if (Array.isArray(row.transferHistory)) row.transferHistory.forEach(pushAudit);
  const dataAudit = asObject(data.transferAudit);
  if (dataAudit) audits.push(dataAudit);
  readTicketEntries(row).forEach((entry) => {
    if (Array.isArray(entry.transferHistory)) entry.transferHistory.forEach(pushAudit);
    if (entry.transferredAt || entry.transferredToUserName) {
      audits.push({
        transferredAt: entry.transferredAt,
        transferredToUserName: entry.transferredToUserName || row.transferToUserName,
        transferByUserName: entry.transferByUserName || row.transferByUserName,
      });
    }
  });
  if (!audits.length && row.transferAt) {
    audits.push({
      transferredAt: row.transferAt,
      transferredToUserName: row.transferToUserName,
      transferByUserName: row.transferByUserName,
    });
  }

  const latest = audits
    .map((audit) => ({
      to: asString(audit.toUserName || audit.transferredToUserName || row.transferToUserName).trim(),
      by: asString(audit.byUserName || audit.transferByUserName || row.transferByUserName).trim(),
      at: parseDate(audit.at || audit.transferredAt || row.transferAt),
    }))
    .sort((left, right) => (right.at?.getTime() ?? 0) - (left.at?.getTime() ?? 0))[0];
  if (!latest) return { label: "Sem transferência", at: null };
  const target = latest.to ? `para ${latest.to}` : "registrada";
  const actor = latest.by ? ` por ${latest.by}` : "";
  return { label: `Transferência ${target}${actor}`, at: latest.at };
}

function readVoucherEntries(row: Row): Row[] {
  const data = asObject(row.data) ?? {};
  const eventParty = asObject(data.eventParty) ?? {};
  const rawEntries = Array.isArray(eventParty.voucherEntries)
    ? eventParty.voucherEntries
    : Array.isArray(eventParty.vouchers)
      ? eventParty.vouchers
      : [];
  return rawEntries.map((entry) => asObject(entry)).filter((entry): entry is Row => Boolean(entry));
}

function orderQuantity(row: Row): number {
  return Math.max(1, Math.floor(parseNumber(row.quantidade ?? row.quantity ?? row.qtd, 1)));
}

function orderTotal(row: Row): number {
  const explicitTotal = parseNumber(row.total ?? row.valorTotal, Number.NaN);
  if (Number.isFinite(explicitTotal)) return explicitTotal;
  const price = parseNumber(row.price ?? row.preco ?? row.valor, 0);
  return price * orderQuantity(row);
}

function orderDiscount(row: Row): number {
  const data = asObject(row.data) ?? {};
  const eventParty = asObject(data.eventParty) ?? {};
  return parseNumber(row.eventDiscountValue ?? eventParty.discountValue ?? eventParty.desconto, 0);
}

function orderDiscountSource(row: Row): string {
  const data = asObject(row.data) ?? {};
  const eventParty = asObject(data.eventParty) ?? {};
  return (
    asString(row.eventDiscountSource || eventParty.discountSource || eventParty.discountKind).trim() ||
    (orderDiscount(row) > 0 ? "Manual" : "Sem desconto")
  );
}

function orderCreatedAt(row: Row): Date | null {
  return parseDate(row.createdAt ?? row.data ?? row.created_at);
}

function orderApprovalDate(row: Row): Date | null {
  const data = asObject(row.data) ?? {};
  const eventParty = asObject(data.eventParty) ?? {};
  return parseDate(row.eventApprovalAt ?? eventParty.approvedAt ?? row.updatedAt);
}

function orderApproverName(row: Row): string {
  const data = asObject(row.data) ?? {};
  const eventParty = asObject(data.eventParty) ?? {};
  return asString(row.approvedBy || row.eventCreatedByName || eventParty.approvedByName || eventParty.createdByName).trim() || "-";
}

function orderApprovalMethod(row: Row): string {
  const data = asObject(row.data) ?? {};
  const eventParty = asObject(data.eventParty) ?? {};
  const method = normalizeText(row.eventApprovalMethod || eventParty.approvalMethod);
  if (method.includes("manual")) return "Manual";
  if (method.includes("qr")) return "QR code";
  if (method.includes("pix")) return "Pix validado";
  if (method.includes("transfer")) return "Transferência";
  return orderApproverName(row) !== "-" ? "Manual" : "-";
}

function isRedeemedEntry(entry: Row): boolean {
  const status = normalizeText(entry.status || entry.situacao);
  return Boolean(
    entry.usedAt ||
      entry.withdrawalAt ||
      entry.scannedAt ||
      status.includes("utilizado") ||
      status.includes("inativo") ||
      status.includes("retirado") ||
      status.includes("redeemed") ||
      status.includes("used")
  );
}

function orderRedeemedQuantity(row: Row): number {
  const entries = readVoucherEntries(row);
  const redeemed = entries.filter(isRedeemedEntry).length;
  if (redeemed > 0) return Math.min(orderQuantity(row), redeemed);
  if (row.eventCheckinAt || normalizeText(row.status) === "redeemed") return orderQuantity(row);
  return 0;
}

function orderWithdrawalDate(row: Row): Date | null {
  const data = asObject(row.data) ?? {};
  const eventParty = asObject(data.eventParty) ?? {};
  const dates = readVoucherEntries(row)
    .map((entry) => parseDate(entry.usedAt || entry.withdrawalAt || entry.scannedAt))
    .filter((date): date is Date => Boolean(date));
  if (dates.length) return dates.sort((left, right) => right.getTime() - left.getTime())[0];
  return parseDate(row.eventCheckinAt || eventParty.usedAt);
}

function orderWithdrawalMethod(row: Row): string {
  const data = asObject(row.data) ?? {};
  const eventParty = asObject(data.eventParty) ?? {};
  const entryMethod = readVoucherEntries(row)
    .map((entry) => asString(entry.usedMethod || entry.withdrawalMethod || entry.scanSource).trim())
    .find(Boolean);
  const method = normalizeText(row.eventCheckinMethod || entryMethod || eventParty.usedMethod);
  if (method.includes("manual")) return "Manual";
  if (method.includes("codigo") || method.includes("code")) return "Código curto";
  if (method.includes("document")) return "Documento";
  if (method.includes("lista")) return "Lista nominal";
  if (method.includes("transfer")) return "Transferido";
  if (method.includes("qr") || method.includes("scan")) return "QR code";
  return orderRedeemedQuantity(row) > 0 ? "QR code" : "-";
}

function orderWithdrawalOperator(row: Row): string {
  const data = asObject(row.data) ?? {};
  const eventParty = asObject(data.eventParty) ?? {};
  return (
    asString(row.eventCheckinByUserName || eventParty.usedByUserName).trim() ||
    readVoucherEntries(row)
      .map((entry) => asString(entry.usedByUserName || entry.withdrawalByUserName || entry.scannedByUserName).trim())
      .find(Boolean) ||
    "-"
  );
}

function orderPaymentSource(row: Row): string {
  const data = asObject(row.data) ?? {};
  const eventParty = asObject(data.eventParty) ?? {};
  const config = asObject(row.payment_config ?? row.paymentConfig) ?? {};
  return (
    asString(row.paymentSource || row.paymentMethod || eventParty.paymentSource || eventParty.paymentMethod || config.method || config.provider).trim() ||
    (row.payment_config ? "Configuração de pagamento" : "-")
  );
}

function orderQrStatus(row: Row): string {
  const entries = readVoucherEntries(row);
  const data = asObject(row.data) ?? {};
  const eventParty = asObject(data.eventParty) ?? {};
  const directStatus = asString(eventParty.voucherStatus || eventParty.qrStatus || row.qrStatus || row.statusQr).trim();
  if (directStatus) return directStatus;
  if (!entries.length) return "-";
  const redeemed = entries.filter(isRedeemedEntry).length;
  const cancelled = entries.filter(isCancelledEntry).length;
  const invalid = entries.filter((entry) => normalizeText(entry.status || entry.situacao).includes("inval")).length;
  if (redeemed >= entries.length) return "Utilizado";
  if (redeemed > 0) return "Parcial";
  if (cancelled >= entries.length) return "Cancelado";
  if (invalid > 0) return "Inválido";
  return "Ativo";
}

function orderCodes(row: Row): string {
  const data = asObject(row.data) ?? {};
  const eventParty = asObject(data.eventParty) ?? {};
  const direct = [eventParty.orderCode, eventParty.orderNumber, eventParty.manualCode, eventParty.manualNumber, eventParty.fichaNumero, row.code, row.codigo]
    .map((value) => asString(value).trim())
    .filter(Boolean);
  const entryCodes = readVoucherEntries(row)
    .flatMap((entry) => [entry.code, entry.manualNumber, entry.token, entry.id])
    .map((value) => asString(value).trim())
    .filter(Boolean);
  return Array.from(new Set([...direct, ...entryCodes])).join(" | ") || "-";
}

function latestProductTransfer(row: Row): { label: string; at: Date | null } {
  const data = asObject(row.data) ?? {};
  const eventParty = asObject(data.eventParty) ?? {};
  const requests = Array.isArray(eventParty.transferRequests)
    ? eventParty.transferRequests.map(asObject).filter((entry): entry is Row => Boolean(entry))
    : [];
  const latest = requests
    .filter((entry) => ["aceito", "accepted"].includes(normalizeText(entry.status)))
    .map((entry) => ({
      to: asString(entry.toUserName || entry.transferredToUserName).trim(),
      from: asString(entry.fromUserName || entry.transferredFromUserName || row.userName).trim(),
      at: parseDate(entry.acceptedAt || entry.requestedAt || row.updatedAt || row.createdAt),
    }))
    .sort((left, right) => (right.at?.getTime() ?? 0) - (left.at?.getTime() ?? 0))[0];
  if (!latest) return { label: "Sem transferência", at: null };
  return {
    label: latest.to ? `Transferido para ${latest.to}` : latest.from ? `Transferido por ${latest.from}` : "Transferido",
    at: latest.at,
  };
}

function userRa(row: Row | undefined, sourceRow: Row): string {
  const data = asObject(sourceRow.data) ?? {};
  const eventParty = asObject(data.eventParty) ?? {};
  const manualCustomer = asObject(eventParty.manualCustomer) ?? {};
  return (
    asString(row?.ra || data.ra || eventParty.ra || manualCustomer.ra || eventParty.externalNumber || data.pulseira || data.braceletNumber).trim() ||
    "-"
  );
}

function productCost(product: Row | undefined, order: Row): number {
  const unitCost = parseNumber(product?.custo ?? product?.cost ?? order.custo ?? order.cost, 0);
  return Math.max(0, unitCost * orderQuantity(order));
}

function planApprovalDate(row: Row): Date | null {
  if (statusGroup(row.status) !== "aprovado") return null;
  return parseDate(row.dataAprovacao ?? row.approvedAt ?? row.updatedAt);
}

function makeTenantHref(tenantSlug: string, path: string): string {
  return tenantSlug ? withTenantSlug(tenantSlug, path) : path;
}

function pendingHrefFor(options: {
  scopeType: FinancialScopeType;
  basePath: string;
  tenantSlug: string;
  tipo: FinancialType;
  eventId?: string;
}): string {
  const cleanBase = options.basePath.replace(/\/+$/, "");
  const eventIdValue = asString(options.eventId).trim();
  let path = cleanBase || "/admin";

  if (options.scopeType === "mini_vendor") {
    path = "/configuracoes/mini-vendor/pedidos-pendentes";
  } else if (options.scopeType === "tenant") {
    if (options.tipo === "planos") path = "/admin/planos/historico";
    else if (options.tipo === "ingressos" && eventIdValue) path = `/admin/eventos/${encodeURIComponent(eventIdValue)}/ingressos`;
    else if (options.tipo === "produtos_modo_vendas" && eventIdValue) path = `/admin/eventos/${encodeURIComponent(eventIdValue)}/ficha/retirada/pendentes`;
    else path = "/admin/loja/pedidos-pendentes";
  } else {
    if (options.tipo === "ingressos" && eventIdValue) path = `${cleanBase}/eventos/${encodeURIComponent(eventIdValue)}/ingressos`;
    else if (options.tipo === "produtos_modo_vendas" && eventIdValue) path = `${cleanBase}/eventos/${encodeURIComponent(eventIdValue)}/ficha/retirada/pendentes`;
    else path = `${cleanBase}/loja/pedidos-pendentes`;
  }

  return makeTenantHref(options.tenantSlug, path);
}

function buildStatementRows(options: {
  data: LoadedData;
  scopeType: FinancialScopeType;
  scopeId?: string;
  basePath: string;
  tenantSlug: string;
  includePlans: boolean;
}): StatementRow[] {
  const eventById = new Map(options.data.events.map((event) => [eventId(event), event]));
  const productById = new Map(options.data.products.map((product) => [asString(product.id).trim(), product]));
  const userById = new Map(options.data.users.map((user) => [asString(user.uid).trim(), user]));
  const forced =
    options.scopeType === "league" || options.scopeType === "commission" || options.scopeType === "directory"
      ? { id: asString(options.scopeId).trim(), scopeType: options.scopeType }
      : undefined;
  const entityIndex = buildEntityScopeIndex(options.data.entities, forced);
  const eventScopeIndex = buildEventScopeIndex(options.data.entities, entityIndex);
  const rows: StatementRow[] = [];

  if (options.includePlans && options.scopeType === "tenant") {
    options.data.planRequests.forEach((request) => {
      const user = userById.get(asString(request.userId).trim());
      const approvedAt = planApprovalDate(request);
      const pedidoAt = parseDate(request.dataSolicitacao ?? request.createdAt);
      const data = asObject(request.data) ?? {};
      const status = asString(request.status).trim() || "-";
      const desconto = parseNumber(request.discountValue ?? data.discountValue ?? data.desconto, 0);
      rows.push({
        id: asString(request.id).trim(),
        tipo: "planos",
        tipoLabel: "Planos",
        ficha: asString(request.planoId).trim() || "-",
        pulseiraRa: userRa(user, request),
        item: asString(request.planoNome).trim() || "Plano",
        lote: "-",
        categoria: "Plano",
        qtd: 1,
        pedidoAt,
        cliente: asString(request.userName || user?.nome).trim() || "Usuário",
        aprovacaoAt: approvedAt,
        aprovadoPor: asString(request.aprovadoPor || data.aprovadoPor).trim() || "-",
        metodoAprovacao: asString(request.approvalMethod || data.approvalMethod).trim() || "-",
        fontePagamento: asString(request.paymentSource || request.metodo || data.paymentSource).trim() || "-",
        entradaAt: null,
        entradaPor: "-",
        metodoEntradaAprovacao: "-",
        statusQr: "-",
        codigoQr: "-",
        transferencia: "Sem transferência",
        transferenciaAt: null,
        metodoEntradaRetirada: "-",
        valor: parseNumber(request.valor, 0),
        custo: 0,
        desconto,
        origemDesconto: asString(request.discountSource || data.discountSource).trim() || (desconto > 0 ? "Manual" : "Sem desconto"),
        fonte: "Planos",
        status,
        statusGroup: statusGroup(status),
        destinoHref: pendingHrefFor({ ...options, tipo: "planos" }),
        destinoLabel: "Abrir planos",
        eventId: "",
        sortAt: pedidoAt?.getTime() ?? 0,
      });
    });
  }

  options.data.tickets.forEach((ticket) => {
    const ticketEvent = ticketEventId(ticket);
    const relatedEvent = eventById.get(ticketEvent);
    if (!ticketEvent || !relatedEvent) return;
    if (!rowMatchesScope({ ...options, row: ticket, relatedEvent, entityIndex, eventScopeIndex })) return;

    const user = userById.get(asString(ticket.userId).trim());
    const transfer = latestTicketTransfer(ticket);
    const pedidoAt = ticketPurchaseDate(ticket);
    const entradaAt = ticketCheckinDate(ticket);
    const status = asString(ticket.status).trim() || "-";
    const data = asObject(ticket.data) ?? {};
    rows.push({
      id: asString(ticket.id).trim(),
      tipo: "ingressos",
      tipoLabel: "Ingressos",
      ficha: asString(data.pulseira || data.braceletNumber || data.externalNumber).trim() || "-",
      pulseiraRa: userRa(user, ticket),
      item: ticketItemName(ticket),
      lote: ticketLotName(ticket),
      categoria: ticketItemCategory(ticket),
      qtd: ticketQuantity(ticket),
      pedidoAt,
      cliente: asString(ticket.userName || user?.nome).trim() || "Usuário",
      aprovacaoAt: ticketApprovalDate(ticket),
      aprovadoPor: asString(ticket.aprovadoPor || ticket.approvedBy).trim() || "-",
      metodoAprovacao: ticketApprovalMethod(ticket),
      fontePagamento: ticketPaymentSource(ticket),
      entradaAt,
      entradaPor: asString(ticket.checkinByUserName || data.checkinByUserName).trim() || "-",
      metodoEntradaAprovacao: ticketApprovalMethod(ticket),
      statusQr: ticketQrStatus(ticket),
      codigoQr: ticketCodes(ticket),
      transferencia: transfer.label,
      transferenciaAt: transfer.at,
      metodoEntradaRetirada:
        asString(ticket.checkinMethod || data.checkinMethod).trim() ||
        (entradaAt ? "QR code" : "-"),
      valor: ticketValue(ticket),
      custo: 0,
      desconto: ticketDiscount(ticket),
      origemDesconto:
        asString(ticket.discountSource || data.discountSource || ticket.discountKind || data.discountKind).trim() ||
        (ticketDiscount(ticket) > 0 ? "Manual" : "Sem desconto"),
      fonte: eventName(relatedEvent),
      status,
      statusGroup: statusGroup(status),
      destinoHref: pendingHrefFor({ ...options, tipo: "ingressos", eventId: ticketEvent }),
      destinoLabel: "Abrir ingressos",
      eventId: ticketEvent,
      sortAt: pedidoAt?.getTime() ?? 0,
    });
  });

  options.data.orders.forEach((order) => {
    const product = productById.get(orderProductId(order));
    const explicitEventId = orderEventId(order);
    const linkedEventId = explicitEventId || productEventId(product ?? {});
    const relatedEvent = linkedEventId ? eventById.get(linkedEventId) : undefined;
    const isEventProduct = Boolean(linkedEventId);
    if (isEventProduct && !relatedEvent) return;
    if (!rowMatchesScope({ ...options, row: order, relatedEvent, entityIndex, eventScopeIndex })) return;
    if (!isEventProduct && options.scopeType === "tenant") {
      const sellerType = normalizeText(order.seller_type);
      const sellerId = asString(order.seller_id).trim();
      if (sellerType === "mini_vendor" || sellerId) return;
    }

    const user = userById.get(asString(order.userId).trim());
    const transfer = latestProductTransfer(order);
    const pedidoAt = orderCreatedAt(order);
    const entradaAt = orderWithdrawalDate(order);
    const status = asString(order.status).trim() || "-";
    const tipo: FinancialType = isEventProduct ? "produtos_modo_vendas" : "produtos_loja";
    const tipoLabel = isEventProduct ? "Produtos Modo Vendas" : "Produtos Loja";
    const fonte = isEventProduct
      ? eventName(relatedEvent)
      : asString(order.seller_name || product?.seller_name).trim() || "Loja oficial";
    rows.push({
      id: asString(order.id).trim(),
      tipo,
      tipoLabel,
      ficha: orderCodes(order),
      pulseiraRa: userRa(user, order),
      item: orderItemName(order, productById),
      lote: asString(order.eventLoteNome || product?.lote).trim() || "-",
      categoria: orderItemCategory(order, productById),
      qtd: orderQuantity(order),
      pedidoAt,
      cliente: asString(order.userName || user?.nome).trim() || "Usuário",
      aprovacaoAt: orderApprovalDate(order),
      aprovadoPor: orderApproverName(order),
      metodoAprovacao: orderApprovalMethod(order),
      fontePagamento: orderPaymentSource(order),
      entradaAt,
      entradaPor: orderWithdrawalOperator(order),
      metodoEntradaAprovacao: orderApprovalMethod(order),
      statusQr: orderQrStatus(order),
      codigoQr: orderCodes(order),
      transferencia: transfer.label,
      transferenciaAt: transfer.at,
      metodoEntradaRetirada: orderWithdrawalMethod(order),
      valor: orderTotal(order),
      custo: productCost(product, order),
      desconto: orderDiscount(order),
      origemDesconto: orderDiscountSource(order),
      fonte,
      status,
      statusGroup: statusGroup(status),
      destinoHref: pendingHrefFor({ ...options, tipo, eventId: linkedEventId }),
      destinoLabel: isEventProduct ? "Abrir retirada" : "Abrir loja",
      eventId: linkedEventId,
      sortAt: pedidoAt?.getTime() ?? 0,
    });
  });

  return rows.sort((left, right) => right.sortAt - left.sortAt || left.cliente.localeCompare(right.cliente, "pt-BR"));
}

function alphabetMatches(value: string, group: AlphabetGroup): boolean {
  if (group === "todos") return true;
  const first = removeAccents(value.trim().charAt(0).toUpperCase());
  if (!first) return false;
  if (group === "a-f") return first >= "A" && first <= "F";
  if (group === "g-l") return first >= "G" && first <= "L";
  if (group === "m-r") return first >= "M" && first <= "R";
  return first >= "S" && first <= "Z";
}

function dateInRange(value: Date | null, startDate: string, endDate: string): boolean {
  if (!value) return true;
  if (startDate) {
    const start = parseDate(`${startDate}T00:00:00`);
    if (start && value.getTime() < start.getTime()) return false;
  }
  if (endDate) {
    const end = parseDate(`${endDate}T23:59:59`);
    if (end && value.getTime() > end.getTime()) return false;
  }
  return true;
}

function KpiCard({
  label,
  value,
  hint,
  icon,
}: {
  label: string;
  value: string;
  hint: string;
  icon: ReactNode;
}) {
  return (
    <article className="rounded-lg border border-zinc-800 bg-zinc-950/80 p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-[10px] font-black uppercase tracking-[0.2em] text-zinc-500">{label}</p>
          <p className="mt-3 text-2xl font-black text-white">{value}</p>
        </div>
        <span className="rounded-lg border border-emerald-400/20 bg-emerald-400/10 p-2 text-emerald-300">
          {icon}
        </span>
      </div>
      <p className="mt-3 text-xs font-bold text-zinc-500">{hint}</p>
    </article>
  );
}

function StatusBadge({ status, group }: { status: string; group: StatusGroup }) {
  const className =
    group === "aprovado"
      ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-300"
      : group === "pendente"
        ? "border-yellow-500/30 bg-yellow-500/10 text-yellow-300"
        : group === "recusado" || group === "cancelado"
          ? "border-red-500/30 bg-red-500/10 text-red-300"
          : "border-zinc-700 bg-zinc-900 text-zinc-300";
  return (
    <span className={`inline-flex rounded-full border px-2 py-1 text-[10px] font-black uppercase ${className}`}>
      {status || "-"}
    </span>
  );
}

type FinancialStatementPageProps = {
  scopeType: FinancialScopeType;
  scopeId?: string;
  title: string;
  subtitle?: string;
  eyebrow?: string;
  logoSrc?: string;
  backHref: string;
  basePath: string;
  includePlans?: boolean;
  biLinks?: Array<{ label: string; href: string }>;
};

export function FinancialStatementPage({
  scopeType,
  scopeId,
  title,
  subtitle,
  eyebrow = "Gestão financeira",
  logoSrc,
  backHref,
  basePath,
  includePlans = false,
  biLinks = [],
}: FinancialStatementPageProps) {
  const { tenantId, tenantSlug } = useTenantTheme();
  const [data, setData] = useState<LoadedData>(emptyLoadedData);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [typeFilter, setTypeFilter] = useState<"todos" | FinancialType>("todos");
  const [statusFilter, setStatusFilter] = useState<"todos" | StatusGroup>("todos");
  const [paymentFilter, setPaymentFilter] = useState("todos");
  const [sourceFilter, setSourceFilter] = useState("todos");
  const [alphabet, setAlphabet] = useState<AlphabetGroup>("todos");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [page, setPage] = useState(1);

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    setError("");
    void loadStatementData(tenantId.trim(), includePlans)
      .then((nextData) => {
        if (mounted) setData(nextData);
      })
      .catch((loadError: unknown) => {
        console.error("Erro ao carregar extrato financeiro.", loadError);
        if (mounted) setError(loadError instanceof Error ? loadError.message : "Erro ao carregar extrato financeiro.");
      })
      .finally(() => {
        if (mounted) setLoading(false);
      });
    return () => {
      mounted = false;
    };
  }, [includePlans, tenantId]);

  const allRows = useMemo(
    () =>
      buildStatementRows({
        data,
        scopeType,
        scopeId,
        basePath,
        tenantSlug,
        includePlans,
      }),
    [basePath, data, includePlans, scopeId, scopeType, tenantSlug]
  );

  const paymentOptions = useMemo(
    () =>
      Array.from(new Set(allRows.map((row) => row.fontePagamento).filter((entry) => entry && entry !== "-")))
        .sort((left, right) => left.localeCompare(right, "pt-BR")),
    [allRows]
  );
  const sourceOptions = useMemo(
    () =>
      Array.from(new Set(allRows.map((row) => row.fonte).filter((entry) => entry && entry !== "-")))
        .sort((left, right) => left.localeCompare(right, "pt-BR")),
    [allRows]
  );

  const filteredRows = useMemo(() => {
    const query = normalizeText(search);
    return allRows.filter((row) => {
      if (typeFilter !== "todos" && row.tipo !== typeFilter) return false;
      if (statusFilter !== "todos" && row.statusGroup !== statusFilter) return false;
      if (paymentFilter !== "todos" && row.fontePagamento !== paymentFilter) return false;
      if (sourceFilter !== "todos" && row.fonte !== sourceFilter) return false;
      if (!alphabetMatches(row.cliente, alphabet)) return false;
      if (!dateInRange(row.pedidoAt, startDate, endDate)) return false;
      if (!query) return true;
      const haystack = normalizeText(
        [
          row.id,
          row.tipoLabel,
          row.ficha,
          row.pulseiraRa,
          row.item,
          row.lote,
          row.categoria,
          row.cliente,
          row.aprovadoPor,
          row.fontePagamento,
          row.fonte,
          row.status,
          row.codigoQr,
          row.transferencia,
        ].join(" ")
      );
      return haystack.includes(query);
    });
  }, [allRows, alphabet, endDate, paymentFilter, search, sourceFilter, startDate, statusFilter, typeFilter]);

  useEffect(() => {
    setPage(1);
  }, [alphabet, endDate, paymentFilter, search, sourceFilter, startDate, statusFilter, typeFilter]);

  const totalPages = Math.max(1, Math.ceil(filteredRows.length / PAGE_SIZE));
  const safePage = Math.min(totalPages, Math.max(1, page));
  const pageRows = filteredRows.slice((safePage - 1) * PAGE_SIZE, safePage * PAGE_SIZE);

  const metrics = useMemo(() => {
    const approved = filteredRows.filter((row) => row.statusGroup === "aprovado");
    const value = approved.reduce((sum, row) => sum + row.valor, 0);
    const cost = approved.reduce((sum, row) => sum + row.custo, 0);
    const discount = filteredRows.reduce((sum, row) => sum + row.desconto, 0);
    const pending = filteredRows.filter((row) => row.statusGroup === "pendente");
    return {
      value,
      cost,
      result: value - cost,
      discount,
      count: filteredRows.length,
      pending: pending.length,
    };
  }, [filteredRows]);

  const resolvedBackHref = makeTenantHref(tenantSlug, backHref);
  const resolvedBiLinks = biLinks.map((link) => ({
    ...link,
    href: makeTenantHref(tenantSlug, link.href),
  }));

  const exportRows = () => {
    const headers = [
      "ID",
      "Tipo",
      "Ficha",
      "Pulseira/RA",
      "Item",
      "Lote",
      "Categoria",
      "Qtd",
      "Data pedido",
      "Hora pedido",
      "Cliente",
      "Data aprovação do pedido",
      "Hora aprovação do pedido",
      "Usuário que aprovou o pedido",
      "Método aprovação",
      "Fonte pagamento",
      "Data entrada/retirada",
      "Hora entrada/retirada",
      "Usuário que aprovou entrada/retirada",
      "Método de aprovação da entrada/retirada",
      "Status QR",
      "Código QR",
      "Transferência",
      "Data da transferência",
      "Hora da transferência",
      "Método entrada/retirada",
      "Valor",
      "Custo",
      "Desconto",
      "Origem desconto",
      "Fonte",
      "Status",
      "Destino pedido",
    ];
    const lines = filteredRows.map((row) =>
      [
        row.id,
        row.tipoLabel,
        row.ficha,
        row.pulseiraRa,
        row.item,
        row.lote,
        row.categoria,
        row.qtd,
        formatDate(row.pedidoAt),
        formatTime(row.pedidoAt),
        row.cliente,
        formatDate(row.aprovacaoAt),
        formatTime(row.aprovacaoAt),
        row.aprovadoPor,
        row.metodoAprovacao,
        row.fontePagamento,
        formatDate(row.entradaAt),
        formatTime(row.entradaAt),
        row.entradaPor,
        row.metodoEntradaAprovacao,
        row.statusQr,
        row.codigoQr,
        row.transferencia,
        formatDate(row.transferenciaAt),
        formatTime(row.transferenciaAt),
        row.metodoEntradaRetirada,
        row.valor,
        row.custo,
        row.desconto,
        row.origemDesconto,
        row.fonte,
        row.status,
        row.destinoHref,
      ]
        .map(csvEscape)
        .join(";")
    );
    const csv = [headers.map(csvEscape).join(";"), ...lines].join("\n");
    const blob = new Blob([`\ufeff${csv}`], { type: "text/csv;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = `extrato-financeiro-${scopeType}-${new Date().toISOString().slice(0, 10)}.csv`;
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    URL.revokeObjectURL(url);
  };

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#050505] text-white">
        <div className="flex items-center gap-3 rounded-2xl border border-zinc-800 bg-zinc-950/80 px-5 py-4">
          <Loader2 className="animate-spin text-emerald-300" size={18} />
          <span className="text-sm font-bold uppercase tracking-[0.2em] text-zinc-400">Carregando extrato</span>
        </div>
      </div>
    );
  }

  return (
    <main className="min-h-screen bg-[#050505] px-4 py-5 text-white md:px-6 print:bg-white print:text-black">
      <div className="mx-auto max-w-7xl space-y-5">
        <header className="rounded-lg border border-zinc-800 bg-zinc-950/70 p-4 print:border-zinc-300 print:bg-white">
          <div className="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">
            <div className="flex items-center gap-3">
              <Link href={resolvedBackHref} className="rounded-lg border border-zinc-800 bg-black p-2 text-zinc-300 hover:text-white print:hidden">
                <ArrowLeft size={18} />
              </Link>
              {logoSrc ? (
                <div className="relative h-11 w-11 shrink-0 overflow-hidden rounded-xl border border-zinc-700 bg-black">
                  <Image src={logoSrc} alt={title} fill sizes="44px" className="object-cover" />
                </div>
              ) : (
                <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl border border-emerald-400/20 bg-emerald-400/10 text-emerald-300">
                  <Wallet size={20} />
                </div>
              )}
              <div>
                <p className="text-[10px] font-black uppercase tracking-[0.28em] text-emerald-300 print:text-zinc-700">{eyebrow}</p>
                <h1 className="mt-1 text-2xl font-black uppercase text-white print:text-black">{title}</h1>
                <p className="mt-1 text-sm font-bold text-zinc-500 print:text-zinc-700">
                  {subtitle || "Extrato isolado por entidade, com filtros, impressão e exportação CSV."}
                </p>
              </div>
            </div>
            <div className="flex flex-wrap gap-2 print:hidden">
              {resolvedBiLinks.map((link) => (
                <Link
                  key={link.href}
                  href={link.href}
                  className="inline-flex items-center gap-2 rounded-lg border border-zinc-800 bg-black px-3 py-2 text-xs font-black uppercase text-zinc-300 hover:border-emerald-400/50 hover:text-white"
                >
                  <BarChart3 size={14} />
                  {link.label}
                </Link>
              ))}
              <button
                type="button"
                onClick={exportRows}
                className="inline-flex items-center gap-2 rounded-lg border border-emerald-500/30 bg-emerald-500/10 px-3 py-2 text-xs font-black uppercase text-emerald-200 hover:bg-emerald-500 hover:text-black"
              >
                <Download size={14} />
                Baixar CSV
              </button>
              <button
                type="button"
                onClick={() => window.print()}
                className="inline-flex items-center gap-2 rounded-lg border border-zinc-700 bg-zinc-950 px-3 py-2 text-xs font-black uppercase text-zinc-200 hover:border-zinc-500"
              >
                <Printer size={14} />
                Imprimir
              </button>
            </div>
          </div>
        </header>

        {error ? (
          <section className="rounded-lg border border-red-500/30 bg-red-500/10 p-4 text-sm font-bold text-red-100">
            {error}
          </section>
        ) : null}

        <section className="grid gap-3 md:grid-cols-2 xl:grid-cols-5">
          <KpiCard label="Receita aprovada" value={formatCurrency(metrics.value)} hint={`${formatNumber(metrics.count)} movimentações filtradas`} icon={<Wallet size={18} />} />
          <KpiCard label="Custo" value={formatCurrency(metrics.cost)} hint="Custo registrado em produtos/eventos" icon={<FileSpreadsheet size={18} />} />
          <KpiCard label="Resultado" value={formatCurrency(metrics.result)} hint="Receita aprovada menos custo" icon={<BarChart3 size={18} />} />
          <KpiCard label="Descontos" value={formatCurrency(metrics.discount)} hint="Descontos registrados no extrato" icon={<Download size={18} />} />
          <KpiCard label="Pendências" value={formatNumber(metrics.pending)} hint="Pedidos aguardando ação" icon={<ShieldAlert size={18} />} />
        </section>

        <section className="rounded-lg border border-zinc-800 bg-black/40 p-3 print:hidden">
          <div className="grid gap-2 md:grid-cols-2 xl:grid-cols-[1.2fr_repeat(6,minmax(0,1fr))]">
            <label className="relative">
              <Search size={15} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500" />
              <input
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                placeholder="Buscar por cliente, item, QR, ID..."
                className="min-h-11 w-full rounded-lg border border-zinc-700 bg-zinc-950 pl-9 pr-3 text-sm font-bold text-white outline-none focus:border-emerald-400"
              />
            </label>
            <select value={typeFilter} onChange={(event) => setTypeFilter(event.target.value as "todos" | FinancialType)} className="min-h-11 rounded-lg border border-zinc-700 bg-zinc-950 px-3 text-sm font-bold text-white outline-none focus:border-emerald-400">
              {TYPE_OPTIONS.map((option) => (
                <option key={option.id} value={option.id}>{option.label}</option>
              ))}
            </select>
            <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value as "todos" | StatusGroup)} className="min-h-11 rounded-lg border border-zinc-700 bg-zinc-950 px-3 text-sm font-bold text-white outline-none focus:border-emerald-400">
              {STATUS_OPTIONS.map((option) => (
                <option key={option.id} value={option.id}>{option.label}</option>
              ))}
            </select>
            <select value={paymentFilter} onChange={(event) => setPaymentFilter(event.target.value)} className="min-h-11 rounded-lg border border-zinc-700 bg-zinc-950 px-3 text-sm font-bold text-white outline-none focus:border-emerald-400">
              <option value="todos">Todas as fontes de pagamento</option>
              {paymentOptions.map((option) => (
                <option key={option} value={option}>{option}</option>
              ))}
            </select>
            <select value={sourceFilter} onChange={(event) => setSourceFilter(event.target.value)} className="min-h-11 rounded-lg border border-zinc-700 bg-zinc-950 px-3 text-sm font-bold text-white outline-none focus:border-emerald-400">
              <option value="todos">Todas as fontes</option>
              {sourceOptions.map((option) => (
                <option key={option} value={option}>{option}</option>
              ))}
            </select>
            <input type="date" value={startDate} onChange={(event) => setStartDate(event.target.value)} className="min-h-11 rounded-lg border border-zinc-700 bg-zinc-950 px-3 text-sm font-bold text-white outline-none focus:border-emerald-400" />
            <input type="date" value={endDate} onChange={(event) => setEndDate(event.target.value)} className="min-h-11 rounded-lg border border-zinc-700 bg-zinc-950 px-3 text-sm font-bold text-white outline-none focus:border-emerald-400" />
          </div>
          <div className="mt-3 flex flex-wrap gap-2">
            {ALPHABET_GROUPS.map((group) => (
              <button
                key={group.id}
                type="button"
                onClick={() => setAlphabet(group.id)}
                className={`rounded-lg border px-3 py-2 text-[10px] font-black uppercase ${
                  alphabet === group.id
                    ? "border-emerald-400 bg-emerald-400 text-black"
                    : "border-zinc-800 bg-zinc-950 text-zinc-300 hover:border-zinc-600"
                }`}
              >
                {group.label}
              </button>
            ))}
          </div>
        </section>

        <section className="overflow-hidden rounded-lg border border-zinc-800 bg-zinc-950/70 print:border-zinc-300 print:bg-white">
          <div className="flex flex-col gap-2 border-b border-zinc-800 px-4 py-3 sm:flex-row sm:items-center sm:justify-between print:border-zinc-300">
            <h2 className="text-xs font-black uppercase tracking-[0.2em] text-zinc-300 print:text-zinc-800">
              Tabela de extrato
            </h2>
            <span className="text-xs font-bold text-zinc-500 print:text-zinc-700">
              Página {safePage} de {totalPages} · {formatNumber(filteredRows.length)} registros · 20 por página
            </span>
          </div>
          <div className="overflow-x-auto">
            <table className="min-w-[3200px] text-left text-xs">
              <thead className="bg-black/30 text-[10px] font-black uppercase tracking-[0.14em] text-zinc-500 print:bg-zinc-100 print:text-zinc-700">
                <tr>
                  {[
                    "ID",
                    "Tipo",
                    "Ficha",
                    "Pulseira/RA",
                    "Item",
                    "Lote",
                    "Categoria",
                    "Qtd",
                    "Data pedido",
                    "Hora pedido",
                    "Cliente",
                    "Data aprovação",
                    "Hora aprovação",
                    "Usuário aprovou",
                    "Método aprovação",
                    "Fonte pagamento",
                    "Data entrada/retirada",
                    "Hora entrada/retirada",
                    "Usuário entrada/retirada",
                    "Método aprovação entrada/retirada",
                    "Status QR",
                    "Código QR",
                    "Transferência",
                    "Data transferência",
                    "Hora transferência",
                    "Método entrada/retirada",
                    "Valor",
                    "Custo",
                    "Desconto",
                    "Origem desconto",
                    "Fonte",
                    "Status",
                    "Destino",
                  ].map((header) => (
                    <th key={header} className="px-3 py-3">{header}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-800 print:divide-zinc-200">
                {pageRows.length ? (
                  pageRows.map((row) => (
                    <tr key={`${row.tipo}-${row.id}`} className="align-top text-zinc-300 hover:bg-zinc-900/70 print:text-zinc-900">
                      <td className="max-w-[180px] px-3 py-3 font-mono text-[11px] text-zinc-400">{row.id}</td>
                      <td className="px-3 py-3 font-black uppercase text-white print:text-black">{row.tipoLabel}</td>
                      <td className="px-3 py-3 font-semibold">{row.ficha}</td>
                      <td className="px-3 py-3 font-semibold">{row.pulseiraRa}</td>
                      <td className="max-w-[220px] px-3 py-3 font-semibold text-white print:text-black">{row.item}</td>
                      <td className="px-3 py-3 font-semibold">{row.lote}</td>
                      <td className="px-3 py-3 font-semibold">{row.categoria}</td>
                      <td className="px-3 py-3 font-black">{formatNumber(row.qtd)}</td>
                      <td className="px-3 py-3">{formatDate(row.pedidoAt)}</td>
                      <td className="px-3 py-3">{formatTime(row.pedidoAt)}</td>
                      <td className="max-w-[220px] px-3 py-3 font-black text-white print:text-black">{row.cliente}</td>
                      <td className="px-3 py-3">{formatDate(row.aprovacaoAt)}</td>
                      <td className="px-3 py-3">{formatTime(row.aprovacaoAt)}</td>
                      <td className="px-3 py-3">{row.aprovadoPor}</td>
                      <td className="px-3 py-3">{row.metodoAprovacao}</td>
                      <td className="px-3 py-3">{row.fontePagamento}</td>
                      <td className="px-3 py-3">{formatDate(row.entradaAt)}</td>
                      <td className="px-3 py-3">{formatTime(row.entradaAt)}</td>
                      <td className="px-3 py-3">{row.entradaPor}</td>
                      <td className="px-3 py-3">{row.metodoEntradaAprovacao}</td>
                      <td className="px-3 py-3">{row.statusQr}</td>
                      <td className="max-w-[260px] px-3 py-3 font-mono text-[11px]">{row.codigoQr}</td>
                      <td className="max-w-[240px] px-3 py-3">{row.transferencia}</td>
                      <td className="px-3 py-3">{formatDate(row.transferenciaAt)}</td>
                      <td className="px-3 py-3">{formatTime(row.transferenciaAt)}</td>
                      <td className="px-3 py-3">{row.metodoEntradaRetirada}</td>
                      <td className="px-3 py-3 font-black text-emerald-300 print:text-zinc-900">{formatCurrency(row.valor)}</td>
                      <td className="px-3 py-3 font-black text-cyan-300 print:text-zinc-900">{formatCurrency(row.custo)}</td>
                      <td className="px-3 py-3 font-black text-yellow-300 print:text-zinc-900">{formatCurrency(row.desconto)}</td>
                      <td className="px-3 py-3">{row.origemDesconto}</td>
                      <td className="max-w-[220px] px-3 py-3">{row.fonte}</td>
                      <td className="px-3 py-3"><StatusBadge status={row.status} group={row.statusGroup} /></td>
                      <td className="px-3 py-3 print:hidden">
                        <Link href={row.destinoHref} className="font-black uppercase text-emerald-300 underline-offset-4 hover:underline">
                          {row.destinoLabel}
                        </Link>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={33} className="px-4 py-10 text-center text-sm font-bold text-zinc-500">
                      Nenhuma movimentação encontrada para o filtro atual.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
          <div className="flex flex-col gap-3 border-t border-zinc-800 px-4 py-3 text-xs font-bold text-zinc-400 sm:flex-row sm:items-center sm:justify-between print:hidden">
            <span>
              Mostrando {formatNumber(pageRows.length)} de {formatNumber(filteredRows.length)} registros
            </span>
            <div className="flex items-center gap-2">
              <button type="button" onClick={() => setPage((current) => Math.max(1, current - 1))} disabled={safePage <= 1} className="rounded-lg border border-zinc-700 px-3 py-2 text-[10px] font-black uppercase text-zinc-300 transition hover:border-zinc-500 disabled:opacity-40">
                Anterior
              </button>
              <button type="button" onClick={() => setPage((current) => Math.min(totalPages, current + 1))} disabled={safePage >= totalPages} className="rounded-lg border border-zinc-700 px-3 py-2 text-[10px] font-black uppercase text-zinc-300 transition hover:border-zinc-500 disabled:opacity-40">
                Próxima
              </button>
            </div>
          </div>
        </section>
      </div>
    </main>
  );
}

export function MiniVendorFinancialStatementPage() {
  const { user } = useAuth();
  const { tenantId, tenantSlug } = useTenantTheme();
  const [loading, setLoading] = useState(true);
  const [profile, setProfile] = useState<MiniVendorProfile | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    setError("");
    const cleanTenantId = tenantId.trim();
    const cleanUserId = user?.uid?.trim() || "";
    if (!cleanTenantId || !cleanUserId) {
      setProfile(null);
      setLoading(false);
      return;
    }
    void fetchCurrentMiniVendorProfile({
      tenantId: cleanTenantId,
      userId: cleanUserId,
      forceRefresh: true,
    })
      .then((nextProfile) => {
        if (mounted) setProfile(nextProfile);
      })
      .catch((loadError: unknown) => {
        console.error(loadError);
        if (mounted) setError(loadError instanceof Error ? loadError.message : "Erro ao carregar mini vendor.");
      })
      .finally(() => {
        if (mounted) setLoading(false);
      });
    return () => {
      mounted = false;
    };
  }, [tenantId, user?.uid]);

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#050505] text-white">
        <Loader2 className="animate-spin text-emerald-300" />
      </div>
    );
  }

  if (error || !profile?.id) {
    const href = makeTenantHref(tenantSlug, "/configuracoes/mini-vendor");
    return (
      <div className="min-h-screen bg-[#050505] px-6 py-10 text-white">
        <div className="mx-auto max-w-3xl rounded-[2rem] border border-zinc-800 bg-zinc-950/80 p-8 text-center">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full border border-red-500/20 bg-red-500/10 text-red-300">
            <ShieldAlert size={24} />
          </div>
          <h1 className="mt-5 text-3xl font-black uppercase">Financeiro indisponível</h1>
          <p className="mt-3 text-sm font-semibold text-zinc-400">
            {error || "Cadastre a lojinha antes de abrir o extrato financeiro."}
          </p>
          <Link href={href} className="mt-6 inline-flex rounded-full border border-zinc-800 bg-zinc-950 px-5 py-3 text-xs font-black uppercase text-zinc-200 hover:bg-zinc-900">
            Voltar ao Mini Vendor
          </Link>
        </div>
      </div>
    );
  }

  return (
    <FinancialStatementPage
      scopeType="mini_vendor"
      scopeId={profile.id}
      title={profile.storeName || "Mini Vendor"}
      subtitle="Extrato privado da sua lojinha, sem misturar atlética, ligas, comissões, diretório ou outros vendedores."
      eyebrow="Financeiro Mini Vendor"
      logoSrc={profile.logoUrl || undefined}
      backHref="/configuracoes/mini-vendor"
      basePath="/configuracoes/mini-vendor"
      biLinks={[{ label: "BI Loja", href: "/configuracoes/mini-vendor/gestao" }]}
    />
  );
}
