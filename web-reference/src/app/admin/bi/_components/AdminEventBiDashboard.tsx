"use client";

import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  AlertTriangle,
  ArrowLeft,
  BarChart3,
  CheckCircle2,
  Clock3,
  Info,
  DollarSign,
  Loader2,
  Package,
  QrCode,
  ShoppingBag,
  Target,
  Ticket,
  TrendingUp,
  Users,
} from "lucide-react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ComposedChart,
  Legend,
  LabelList,
  Line,
  LineChart,
  Pie,
  PieChart,
  PolarAngleAxis,
  PolarGrid,
  PolarRadiusAxis,
  Radar,
  RadarChart,
  ReferenceLine,
  ResponsiveContainer,
  Scatter,
  ScatterChart,
  Tooltip,
  XAxis,
  YAxis,
  ZAxis,
} from "recharts";

import { useTenantTheme } from "@/context/TenantThemeContext";
import { getSupabaseClient } from "@/lib/supabase";
import { asObject, asString, type Row } from "@/lib/supabaseData";
import { withTenantSlug } from "@/lib/tenantRouting";

export type AdminEventBiView =
  | "inicio"
  | "comercial"
  | "operacional"
  | "portaria"
  | "estrategico"
  | "vendas";

type ScopeType = "tenant" | "league" | "directory" | "commission";
type AudienceBasis = "pedidos" | "aprovados" | "checkin";

type MetricRow = {
  name: string;
  quantity: number;
  value: number;
  average?: number;
  secondary?: number;
  sortValue?: number;
  hint?: string;
  href?: string;
};

type TableRow = Record<string, string | number | undefined>;
type StatementKind = "ingresso" | "produto";
type StatementTypeFilter = "todos" | StatementKind;
type StatementStatusFilter = "todos" | "aprovado" | "pendente" | "analise";
type OperationalFlow = "pedido" | "aprovacao" | "checkin" | "retirada";
type ChartValueFormat = "currency" | "number" | "decimal" | "hours" | "percent";
type HeatmapEntry = {
  row: string;
  column: string;
  value: number;
  href?: string;
};

type BubbleEntry = {
  name: string;
  x: number;
  y: number;
  z: number;
  value: number;
  decision?: string;
  href?: string;
};

type NetworkEdge = {
  from: string;
  to: string;
  value: number;
  href?: string;
};

type StatementLinkOptions = {
  type?: StatementTypeFilter;
  status?: StatementStatusFilter;
  search?: string;
  alert?: string;
  source?: string;
  approver?: string;
  flow?: OperationalFlow;
  indicator?: string;
};

type CheckinsLinkOptions = {
  search?: string;
  indicator?: string;
};

type OperationalRecord = {
  id: string;
  eventId: string;
  eventName: string;
  kind: StatementKind;
  status: string;
  statusFilter: StatementStatusFilter;
  typeLabel: string;
  itemName: string;
  category: string;
  lotName: string;
  quantity: number;
  value: number;
  expectedValue: number;
  discount: number;
  discountSource: string;
  createdAt: Date | null;
  approvedAt: Date | null;
  completedAt: Date | null;
  approver: string;
  approvalMethod: string;
  source: string;
  paymentSource: string;
  createdBy: string;
  completedBy: string;
  completionMethod: string;
  manual: boolean;
  manualAtDoor: boolean;
  hasCode: boolean;
  usedQuantity: number;
  approved: boolean;
  pending: boolean;
  rejected: boolean;
  cancelled: boolean;
  transferred: boolean;
  courtesy: boolean;
};

type BiData = {
  events: Row[];
  tickets: Row[];
  rsvps: Row[];
  products: Row[];
  orders: Row[];
  users: Row[];
  entities: Row[];
};

type ScopeOption = {
  id: string;
  name: string;
};

type ProductMetricRow = MetricRow & {
  redeemed: number;
  pending: number;
};

const emptyData: BiData = {
  events: [],
  tickets: [],
  rsvps: [],
  products: [],
  orders: [],
  users: [],
  entities: [],
};

const COLORS = ["#22c55e", "#38bdf8", "#facc15", "#fb7185", "#a78bfa", "#f97316", "#14b8a6", "#e879f9"];
const WEEKDAYS = ["Domingo", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado"];
const PERIODS = ["Madrugada", "Manhã", "Tarde", "Noite"];
const AUDIENCE_BASIS_OPTIONS: Array<{ id: AudienceBasis; label: string }> = [
  { id: "pedidos", label: "Pedidos" },
  { id: "aprovados", label: "Aprovados" },
  { id: "checkin", label: "Check-in" },
];

const OPERATIONAL_ALERT_DESCRIPTIONS: Record<string, string> = {
  "aprovado-sem-valor": "Existe item aprovado sem valor registrado. Vale conferir se era cortesia real ou falha de cobrança.",
  "valor-zero-sem-cortesia": "O item ficou aprovado por R$ 0,00, mas não aparece como cortesia. Pode indicar desconto indevido ou preço ausente.",
  "cortesia-com-valor": "O item parece cortesia, mas tem valor cobrado. Confirme se a classificação ou o preço estão corretos.",
  "desconto-sem-origem": "Há desconto aplicado sem cupom, plano ou justificativa registrada. Isso dificulta auditoria.",
  "valor-diferente-tabela": "O valor aprovado não bate com o preço esperado do lote ou produto depois dos descontos registrados.",
  "manual-fora-padrao": "Um pedido criado manualmente ficou com valor diferente do padrão do item.",
  "preco-incompativel": "O preço do ingresso ou produto não combina com o lote/produto cadastrado.",
  "pagamento-sem-metodo": "O pedido foi aprovado, mas o método de aprovação/pagamento não ficou identificado.",
  "aprovado-sem-fonte-pagamento": "O pedido aprovado não mostra de onde veio o pagamento ou a confirmação.",
  "transferencia-valor-incompativel": "Uma transferência aparece com valor associado. Transferência normalmente não deveria gerar cobrança nova.",
  "aprovado-sem-codigo": "O item está aprovado, mas não tem QR ou código operacional para entrada/retirada.",
  "codigo-sem-uso": "O item aprovado tem QR ou código gerado, mas ainda não foi usado na entrada ou retirada.",
  "uso-sem-aprovacao": "O item foi usado na entrada ou retirada sem uma aprovação clara no extrato.",
  "status-incoerente": "O status operacional não combina com o histórico do item, como cancelado com uso ou aprovado sem data.",
  "aprovado-perto-evento": "O pedido foi aprovado muito perto do horário do evento, aumentando risco de fila ou erro operacional.",
};

const MODULES: Array<{
  id: Exclude<AdminEventBiView, "inicio">;
  title: string;
  subtitle: string;
  icon: ReactNode;
}> = [
  {
    id: "comercial",
    title: "BI Comercial",
    subtitle: "Venda, receita, lote, preço, turma e funil.",
    icon: <DollarSign size={20} />,
  },
  {
    id: "operacional",
    title: "BI Operacional",
    subtitle: "Aprovação, comprovante, fila, gargalo e aprovadores.",
    icon: <Clock3 size={20} />,
  },
  {
    id: "portaria",
    title: "BI Portaria",
    subtitle: "Entrada, presença, ausência e leitura de QR code.",
    icon: <QrCode size={20} />,
  },
  {
    id: "estrategico",
    title: "BI Estratégico",
    subtitle: "Recorrência, previsão, comportamento e repetição.",
    icon: <Target size={20} />,
  },
  {
    id: "vendas",
    title: "BI Modo Vendas",
    subtitle: "Produto, ficha, bar, retirada, baixa e auditoria.",
    icon: <ShoppingBag size={20} />,
  },
];

const chartTooltipStyle = {
  background: "#09090b",
  border: "1px solid rgba(255,255,255,.14)",
  borderRadius: 8,
  color: "#fff",
};
const chartTooltipLabelStyle = { color: "#ffffff", fontWeight: 800 };
const chartTooltipItemStyle = { color: "#ffffff", fontWeight: 700 };

const currencyFormatter = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});
const numberFormatter = new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 0 });
const decimalFormatter = new Intl.NumberFormat("pt-BR", {
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
});

function formatCurrency(value: number): string {
  return currencyFormatter.format(Number.isFinite(value) ? value : 0);
}

function formatNumber(value: number): string {
  return numberFormatter.format(Number.isFinite(value) ? value : 0);
}

function formatDecimal(value: number): string {
  return decimalFormatter.format(Number.isFinite(value) ? value : 0);
}

function formatPercent(value: number): string {
  return `${formatDecimal(value)}%`;
}

function formatHours(value: number): string {
  return `${formatDecimal(value)}h`;
}

function formatChartValue(value: number, format: ChartValueFormat): string {
  if (format === "currency") return formatCurrency(value);
  if (format === "percent") return formatPercent(value);
  if (format === "hours") return formatHours(value);
  if (format === "decimal") return formatDecimal(value);
  return formatNumber(value);
}

function formatShortChartValue(value: number, format: ChartValueFormat): string {
  const safeValue = Number.isFinite(value) ? value : 0;
  if (format === "currency") {
    if (Math.abs(safeValue) >= 1000) {
      return new Intl.NumberFormat("pt-BR", {
        style: "currency",
        currency: "BRL",
        notation: "compact",
        maximumFractionDigits: 1,
      }).format(safeValue);
    }
    return formatCurrency(safeValue);
  }
  if (format === "percent") return formatPercent(safeValue);
  if (format === "hours") return formatHours(safeValue);
  if (format === "decimal") return formatDecimal(safeValue);
  if (Math.abs(safeValue) >= 1000) {
    return new Intl.NumberFormat("pt-BR", { notation: "compact", maximumFractionDigits: 1 }).format(safeValue);
  }
  return formatNumber(safeValue);
}

function parseNumber(value: unknown, fallback = 0): number {
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
}

function parseDate(value: unknown): Date | null {
  if (value instanceof Date && Number.isFinite(value.getTime())) return value;
  if (typeof value !== "string" && typeof value !== "number") return null;
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
  const parsed = new Date(value);
  return Number.isFinite(parsed.getTime()) ? parsed : null;
}

function asArray(value: unknown): unknown[] {
  return Array.isArray(value) ? value : [];
}

function normalizeText(value: unknown): string {
  return asString(value)
    .trim()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase();
}

function safeDivide(numerator: number, denominator: number): number {
  return denominator ? numerator / denominator : 0;
}

function median(values: number[]): number {
  const sorted = values.filter(Number.isFinite).sort((left, right) => left - right);
  if (!sorted.length) return 0;
  const middle = Math.floor(sorted.length / 2);
  return sorted.length % 2 ? sorted[middle] : (sorted[middle - 1] + sorted[middle]) / 2;
}

function percentile(values: number[], target: number): number {
  const sorted = values.filter(Number.isFinite).sort((left, right) => left - right);
  if (!sorted.length) return 0;
  const safeTarget = Math.min(1, Math.max(0, target));
  const index = (sorted.length - 1) * safeTarget;
  const lower = Math.floor(index);
  const upper = Math.ceil(index);
  if (lower === upper) return sorted[lower];
  return sorted[lower] + (sorted[upper] - sorted[lower]) * (index - lower);
}

function maxValue(values: number[]): number {
  return values.filter(Number.isFinite).reduce((current, value) => Math.max(current, value), 0);
}

function clamp(value: number, min = 0, max = 100): number {
  if (!Number.isFinite(value)) return min;
  return Math.min(max, Math.max(min, value));
}

function scoreFromRatio(value: number, max: number): number {
  return clamp(safeDivide(value, max) * 100);
}

function scoreFromInverseRate(value: number, max = 100): number {
  return clamp(100 - safeDivide(value, max) * 100);
}

function scoreColor(score: number): string {
  if (score >= 85) return "#22c55e";
  if (score >= 70) return "#38bdf8";
  if (score >= 40) return "#facc15";
  return "#fb7185";
}

function scoreBandLabel(score: number): string {
  if (score >= 85) return "85-100 repetir e escalar";
  if (score >= 70) return "70-84 repetir";
  if (score >= 40) return "40-69 ajustar";
  return "0-39 repensar";
}

function hourLabel(date: Date | null): string {
  return date ? `${String(date.getHours()).padStart(2, "0")}h` : "Sem horário";
}

function hourSortValue(label: string): number {
  const match = label.match(/^(\d{1,2})h$/i);
  if (!match) return Number.MAX_SAFE_INTEGER;
  const hour = Number(match[1]);
  return Number.isFinite(hour) ? hour : Number.MAX_SAFE_INTEGER;
}

function dateKey(date: Date | null): string {
  return date
    ? `${String(date.getDate()).padStart(2, "0")}/${String(date.getMonth() + 1).padStart(2, "0")}`
    : "Sem data";
}

function leadBucketLabel(purchase: Date | null, eventStart: Date | null): string {
  if (!purchase || !eventStart) return "Sem data";
  const diffDays = (eventStart.getTime() - purchase.getTime()) / 864e5;
  if (diffDays >= 30) return "30 dias ou mais";
  if (diffDays >= 15) return "15 a 29 dias";
  if (diffDays >= 7) return "7 a 14 dias";
  if (diffDays >= 3) return "3 a 6 dias";
  if (diffDays >= 1) return "24 a 72h";
  return "Menos de 24h";
}

function ticketBucket(value: number): string {
  if (value < 25) return "R$ 0-25";
  if (value < 50) return "R$ 25-50";
  if (value < 100) return "R$ 50-100";
  if (value < 150) return "R$ 100-150";
  return "Mais de R$ 150";
}

const TICKET_BUCKET_ORDER = ["R$ 0-25", "R$ 25-50", "R$ 50-100", "R$ 100-150", "Mais de R$ 150"];

function ticketBucketSortValue(name: string): number {
  const index = TICKET_BUCKET_ORDER.indexOf(name);
  return index >= 0 ? index : TICKET_BUCKET_ORDER.length;
}

function extractMissingSchemaColumn(error: unknown): string {
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
}

async function queryRows(
  table: string,
  select: string,
  tenantId: string,
  orderColumn: string,
  limit: number
): Promise<Row[]> {
  const supabase = getSupabaseClient();
  let selectColumns = select
    .split(",")
    .map((entry) => entry.trim())
    .filter(Boolean);
  let canOrder = orderColumn.trim().length > 0;
  let canFilterTenant = tenantId.trim().length > 0;

  while (selectColumns.length > 0) {
    let query = supabase.from(table).select(selectColumns.join(",")).limit(limit);
    if (canOrder) query = query.order(orderColumn, { ascending: false });
    if (canFilterTenant) query = query.eq("tenant_id", tenantId);
    const { data, error } = await query;
    if (!error) return Array.isArray(data) ? (data as unknown as Row[]) : [];

    const missingColumn = extractMissingSchemaColumn(error).trim().toLowerCase();
    if (missingColumn) {
      if (canFilterTenant && missingColumn === "tenant_id") {
        canFilterTenant = false;
        continue;
      }
      if (canOrder && missingColumn === orderColumn.trim().toLowerCase()) {
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

async function loadBiData(tenantId: string): Promise<BiData> {
  const [events, tickets, rsvps, products, orders, users, entities] = await Promise.all([
    queryRows(
      "eventos",
      "id,titulo,data,hora,lotes,stats,data_extra,payment_config,status,tipo,categoria,capacidade,capacity,vagas,tenant_id,createdAt",
      tenantId,
      "data",
      600
    ),
    queryRows(
      "solicitacoes_ingressos",
      "id,eventoId,eventoNome,userId,userName,userTurma,email,telefone,phone,status,loteId,loteNome,quantidade,valorUnitario,valorTotal,payment_config,paymentSource,paymentMethod,source,data,dataSolicitacao,dataAprovacao,dataPagamento,paymentDate,paidAt,createdAt,aprovadoPor,metodo,itemType,itemName,itemCategory,approvalMethod,checkinAt,checkinByUserId,checkinByUserName,checkinMethod,checkinNote,checkinEditedAt,checkinEditedByUserId,checkinEditedByUserName,checkinAuditLog,discountValue,discountKind,discountSource,transferAt,transferFromUserId,transferFromUserName,transferToUserId,transferToUserName,transferByUserId,transferByUserName,transferHistory,tenant_id",
      tenantId,
      "dataSolicitacao",
      6000
    ),
    queryRows(
      "eventos_rsvps",
      "id,eventoId,userId,userName,userTurma,status,timestamp,tenant_id",
      tenantId,
      "timestamp",
      12000
    ),
    queryRows(
      "produtos",
      "id,tenant_id,nome,preco,categoria,lote,estoque,likes,cliques,vendidos,active,aprovado,status,seller_type,seller_id,seller_name,payment_config,data,createdAt",
      tenantId,
      "createdAt",
      3000
    ),
    queryRows(
      "orders",
      "id,tenant_id,userId,userName,userTurma,email,telefone,phone,productId,productName,price,total,quantidade,itens,data,status,approvedBy,payment_config,paymentSource,paymentMethod,source,createdAt,updatedAt,seller_type,seller_id,seller_name,eventId,eventItemType,eventItemName,eventLoteNome,eventItemCategory,eventApprovalAt,eventApprovalMethod,eventCheckinAt,eventCheckinByUserId,eventCheckinByUserName,eventCheckinMethod,eventDiscountValue,eventDiscountKind,eventDiscountSource,eventCreatedManually,eventCreatedByUserId,eventCreatedByName",
      tenantId,
      "createdAt",
      8000
    ),
    queryRows(
      "users",
      "uid,nome,turma,email,telefone,phone,ra,cpf,role,tenant_role,tenant_id,createdAt",
      tenantId,
      "createdAt",
      6000
    ),
    queryRows(
      "ligas_config",
      "id,nome,sigla,category,data,membros,membrosIds,eventos,tenant_id,createdAt",
      tenantId,
      "createdAt",
      1000
    ),
  ]);

  return { events, tickets, rsvps, products, orders, users, entities };
}

function statusValue(row: Row): string {
  return normalizeText(row.status || row.situacao || row.state);
}

function isApprovedStatus(value: unknown): boolean {
  return [
    "approved",
    "aprovado",
    "aprovada",
    "paid",
    "pago",
    "confirmado",
    "confirmada",
    "entregue",
    "validado",
    "redeemed",
  ].includes(normalizeText(value));
}

function isRejectedStatus(value: unknown): boolean {
  return [
    "rejected",
    "recusado",
    "recusada",
    "reprovado",
    "reprovada",
    "denied",
  ].includes(normalizeText(value));
}

function isCancelledStatus(value: unknown): boolean {
  return ["cancelled", "canceled", "cancelado", "cancelada", "expired", "expirado"].includes(
    normalizeText(value)
  );
}

function isRefundedStatus(value: unknown): boolean {
  return ["refunded", "refund", "reembolsado", "estornado"].includes(normalizeText(value));
}

function statementStatusFilterFromStatus(value: unknown): StatementStatusFilter {
  if (isApprovedStatus(value)) return "aprovado";
  if (isRejectedStatus(value) || isCancelledStatus(value)) return "todos";
  const normalized = normalizeText(value);
  if (normalized.includes("analise") || normalized.includes("review")) return "analise";
  if (normalized.includes("pend") || normalized.includes("pending") || normalized.includes("aguard")) return "pendente";
  return "todos";
}

function eventId(row: Row): string {
  return asString(row.id || row.eventId || row.eventoId || row.globalEventId).trim();
}

function eventName(row: Row): string {
  return asString(row.titulo || row.nome || row.name || row.eventoNome).trim() || "Evento sem nome";
}

function eventStats(row: Row): Row {
  return asObject(row.stats) ?? {};
}

function eventCardClickCount(row: Row): number {
  const stats = eventStats(row);
  return Math.max(
    parseNumber(stats.cardClicks, 0),
    parseNumber(stats.eventCardClicks, 0),
    parseNumber(stats.cliquesCard, 0)
  );
}

function eventBuyClickCount(row: Row): number {
  const stats = eventStats(row);
  return Math.max(
    parseNumber(stats.cliquesCompra, 0),
    parseNumber(stats.buyClicks, 0),
    parseNumber(stats.checkoutClicks, 0),
    parseNumber(stats.purchaseClicks, 0),
    parseNumber(stats.clicks, 0)
  );
}

function eventDate(row?: Row | null): Date | null {
  if (!row) return null;
  const date = asString(row.data || row.date || row.startsAt || row.inicio).trim();
  const hour = asString(row.hora || row.time || row.horario).trim();
  if (date && hour && !date.includes("T")) {
    return parseDate(`${date}T${hour}`);
  }
  return parseDate(date || row.createdAt);
}

function ticketEventId(row: Row): string {
  return asString(row.eventoId || row.eventId || row.event_id || row.linkEvento || row.globalEventId).trim();
}

function rsvpEventId(row: Row): string {
  return asString(row.eventoId || row.eventId || row.event_id).trim();
}

function rsvpDate(row: Row): Date | null {
  return parseDate(row.timestamp || row.createdAt || row.created_at);
}

function rsvpStatus(row: Row): "going" | "maybe" | "" {
  const status = normalizeText(row.status);
  if (status === "going" || status === "vou" || status === "confirmado") return "going";
  if (status === "maybe" || status === "talvez" || status === "interessado") return "maybe";
  return "";
}

function ticketQuantity(row: Row): number {
  const explicit = parseNumber(row.quantidade ?? row.quantity ?? row.qtd, 0);
  if (explicit > 0) return explicit;
  return Math.max(readTicketEntries(row).length, 1);
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

function ticketLotName(row: Row): string {
  return asString(row.loteNome || row.lote || row.ticketName || row.tipoIngresso || row.categoria).trim() || "Sem lote";
}

function ticketClassName(row: Row): string {
  const data = asObject(row.data) ?? {};
  return asString(row.userTurma || row.turma || data.turma || data.userTurma).trim() || "Sem turma";
}

function ticketBuyerId(row: Row): string {
  return asString(row.userId || row.user_id || row.compradorId || row.email || row.userEmail || row.userName).trim() || `pedido-${asString(row.id)}`;
}

function ticketApproverName(row: Row): string {
  return asString(row.aprovadoPor || row.approvedBy || row.aprovador || row.approverName).trim() || "Sem aprovador";
}

function ticketItemName(row: Row): string {
  return asString(row.itemName || row.ticketName || row.loteNome || row.lote || row.tipoIngresso).trim() || "Ingresso";
}

function ticketItemCategory(row: Row): string {
  const data = asObject(row.data) ?? {};
  return asString(row.itemCategory || data.itemCategory || data.categoria || data.loteCategoria || ticketLotName(row)).trim() || "Ingresso";
}

function ticketApprovalMethod(row: Row): string {
  return approvalMethodLabel(
    row.approvalMethod || row.metodo || row.aprovacaoMetodo || row.approvalSource,
    ticketApproverName(row) !== "Sem aprovador" ? "Manual" : "-"
  );
}

function isManualTicket(row: Row): boolean {
  const data = asObject(row.data) ?? {};
  const method = normalizeText(row.approvalMethod || row.metodo || data.approvalMethod);
  return Boolean(
    data.manualGateEntry ||
      data.createdManually ||
      data.manualOrder ||
      method.includes("manual") ||
      method.includes("porta") ||
      isManualUserId(row.userId)
  );
}

function ticketSource(row: Row): string {
  const data = asObject(row.data) ?? {};
  const source = asString(data.source || data.origem || row.source || row.canal_origem).trim();
  if (source) return source;
  if (asObject(row.data)?.manualGateEntry) return "Cadastro manual";
  if (isManualTicket(row)) return "Manual/admin";
  return "App";
}

function ticketPaymentSource(row: Row): string {
  const config = asObject(row.payment_config ?? row.paymentConfig) ?? {};
  const data = asObject(row.data) ?? {};
  return (
    asString(row.paymentSource || row.paymentMethod || data.paymentSource || data.paymentMethod || config.method || config.provider).trim() ||
    (row.payment_config ? "Configuração de pagamento" : "-")
  );
}

function ticketDiscountSource(row: Row): string {
  const data = asObject(row.data) ?? {};
  return asString(row.discountSource || data.discountSource || row.discountKind || data.discountKind).trim();
}

function ticketHasQrCode(row: Row): boolean {
  return readTicketEntries(row).some((entry) =>
    Boolean(asString(entry.token || entry.id || entry.codigo || entry.qrCode || entry.code).trim())
  );
}

function readTicketEntries(row: Row): Row[] {
  const config = asObject(row.payment_config ?? row.paymentConfig ?? row.data) ?? {};
  const candidates = [
    config.ticketEntries,
    config.tickets,
    config.ingressos,
    row.ticketEntries,
    row.tickets,
    row.ingressos,
  ];
  for (const candidate of candidates) {
    if (Array.isArray(candidate)) {
      return candidate.map((entry) => asObject(entry)).filter((entry): entry is Row => Boolean(entry));
    }
  }
  return [];
}

function entryScannedAt(entry: Row): Date | null {
  return parseDate(entry.scannedAt ?? entry.scanAt ?? entry.checkedAt ?? entry.checkinAt ?? entry.lidoEm ?? entry.dataCheckin);
}

function getLatestDateFromEntries(entries: Row[]): Date | null {
  return entries
    .map((entry) => parseDate(entry.scannedAt ?? entry.usedAt ?? entry.withdrawalAt ?? entry.checkinAt ?? entry.checkedAt))
    .filter((date): date is Date => Boolean(date))
    .sort((left, right) => right.getTime() - left.getTime())[0] ?? null;
}

function ticketRowCheckinAt(row: Row): Date | null {
  return parseDate(row.checkinAt ?? row.checkedAt ?? row.scannedAt ?? row.dataCheckin);
}

function isTicketEntryCheckedIn(entry: Row): boolean {
  const status = normalizeText(entry.status || entry.scanStatus || entry.situacao);
  return Boolean(entryScannedAt(entry)) || status.includes("lido") || status.includes("scan") || status.includes("check");
}

function entryScanSource(entry: Row): string {
  const source = normalizeText(entry.scanSource || entry.source || entry.scannerSource || entry.usedMethod);
  if (source.includes("manual")) return "Manual";
  if (source.includes("qr") || source.includes("scan") || source.includes("app")) return "QR code";
  return entryScannedAt(entry) ? "QR code" : "-";
}

function ticketScannedCount(row: Row): number {
  const entries = readTicketEntries(row);
  const scannedEntries = entries.filter(isTicketEntryCheckedIn).length;
  if (scannedEntries > 0) return scannedEntries;
  return ticketRowCheckinAt(row) ? ticketQuantity(row) : 0;
}

function ticketInvalidScanCount(row: Row): number {
  return readTicketEntries(row).filter((entry) => {
    const status = normalizeText(entry.status || entry.scanStatus || entry.situacao);
    return status.includes("invalid") || status.includes("inval") || status.includes("duplic");
  }).length;
}

function isTransferredEntry(entry: Row): boolean {
  const status = normalizeText(entry.status || entry.situacao || entry.scanStatus);
  return status.includes("transfer");
}

function isCancelledEntry(entry: Row): boolean {
  const status = normalizeText(entry.status || entry.situacao || entry.scanStatus);
  return status.includes("cancel") || status.includes("rejeit") || status.includes("estorn") || status.includes("refund");
}

function activeTicketEntries(row: Row): Row[] {
  return readTicketEntries(row).filter((entry) => !isTransferredEntry(entry) && !isCancelledEntry(entry));
}

function hasStudentClass(value: unknown): boolean {
  const normalized = normalizeText(value);
  return Boolean(
    normalized &&
      ![
        "-",
        "geral",
        "sem turma",
        "porta",
        "visitante",
        "visitor",
        "externo",
        "nao aluno",
        "não aluno",
      ].includes(normalized)
  );
}

function isManualUserId(value: unknown): boolean {
  const normalized = normalizeText(value);
  return normalized.startsWith("manual") || normalized.includes("porta");
}

function classifyTicketAudience(row: Row, entry: Row | null, userById: Map<string, Row>): string {
  const data = asObject(row.data) ?? {};
  const category = normalizeText(row.itemCategory || data.itemCategory || ticketLotName(row));
  const lote = normalizeText(ticketLotName(row));
  const userId = asString(row.userId).trim();
  const user = userById.get(userId);
  const turma = asString(user?.turma || row.userTurma || entry?.holderTurma || entry?.userTurma).trim();
  const manualGateEntry = Boolean(data.manualGateEntry);

  if (category.includes("convid") || lote.includes("convid")) return "Convidado";
  if (
    manualGateEntry ||
    isManualUserId(userId) ||
    category.includes("porta") ||
    category.includes("extern") ||
    category.includes("nao aluno") ||
    lote.includes("extern") ||
    lote.includes("nao aluno")
  ) {
    return hasStudentClass(turma) && !manualGateEntry ? "Aluno" : "Não aluno";
  }
  if (hasStudentClass(turma)) return "Aluno";
  return "Não classificado";
}

function eventCapacity(row?: Row | null): number {
  if (!row) return 0;
  const dataExtra = asObject(row.data_extra) ?? {};
  const eventParty = asObject(dataExtra.eventParty) ?? {};
  const stats = asObject(row.stats) ?? {};
  const explicitCapacity = [
    row.capacidade,
    row.capacity,
    row.vagas,
    dataExtra.capacidade,
    dataExtra.capacity,
    dataExtra.capacidadeTotal,
    dataExtra.totalCapacity,
    dataExtra.eventCapacity,
    eventParty.capacidade,
    eventParty.capacity,
    eventParty.capacidadeTotal,
    eventParty.totalCapacity,
    stats.capacidade,
    stats.capacity,
    stats.vagas,
  ]
    .map((value) => parseNumber(value, 0))
    .find((value) => value > 0);
  if (explicitCapacity) return explicitCapacity;

  return asArray(row.lotes ?? row.batches ?? row.tickets).reduce<number>((sum, entry) => {
    const lot = asObject(entry);
    if (!lot) return sum;
    return (
      sum +
      parseNumber(
        lot.quantidade ?? lot.capacidade ?? lot.capacity ?? lot.limite ?? lot.total ?? lot.estoque ?? lot.vagas,
        0
      )
    );
  }, 0);
}

function ticketEntryToken(entry: Row): string {
  return asString(entry.token || entry.ticketToken || entry.id || entry.codigo || entry.qrCode || entry.code).trim();
}

function ticketEntryUserId(entry: Row): string {
  return asString(entry.holderUserId || entry.userId || entry.uid || entry.ownerUserId || entry.toUserId).trim();
}

function ticketHolderName(row: Row, entry?: Row | null): string {
  return (
    asString(entry?.holderName || entry?.userName || entry?.nome).trim() ||
    asString(row.userName || row.nome).trim() ||
    "Participante"
  );
}

function ticketHolderTurma(row: Row, entry?: Row | null): string {
  return (
    asString(entry?.holderTurma || entry?.userTurma || entry?.turma).trim() ||
    ticketClassName(row) ||
    "Sem turma"
  );
}

function ticketContact(row: Row, userById: Map<string, Row>): string {
  const data = asObject(row.data) ?? {};
  const user = userById.get(asString(row.userId).trim());
  return (
    asString(data.telefone || data.phone || data.whatsapp || row.telefone || row.phone || user?.telefone || user?.phone).trim() ||
    asString(data.email || row.email || user?.email).trim() ||
    "-"
  );
}

function rowCheckinOperator(row: Row): string {
  const data = asObject(row.data) ?? {};
  return asString(row.checkinByUserName || row.checkinBy || data.checkinByUserName || data.checkinBy).trim() || "Sem operador";
}

function entryScanOperator(entry: Row, row: Row): string {
  return (
    asString(entry.scannedByUserName || entry.usedByUserName || entry.checkinByUserName || entry.operatorName).trim() ||
    rowCheckinOperator(row)
  );
}

function checkinAuditRows(row: Row, entry?: Row | null): Row[] {
  const data = asObject(row.data) ?? {};
  return [
    ...asArray(row.checkinAuditLog),
    ...asArray(data.checkinAuditLog),
    ...asArray(entry?.checkinAuditLog),
    ...asArray(entry?.auditLog),
  ]
    .map((audit) => asObject(audit))
    .filter((audit): audit is Row => Boolean(audit));
}

function isDuplicateAuditEntry(entry: Row): boolean {
  const action = normalizeText(entry.action || entry.type || entry.status || entry.reason);
  return action.includes("repeated") || action.includes("duplic") || action.includes("ja utilizado") || action.includes("already");
}

function invalidReasonLabel(value: unknown): string {
  const normalized = normalizeText(value);
  if (!normalized) return "";
  if (normalized.includes("outro evento")) return "QR (Quick Response) de outro evento";
  if (normalized.includes("ja utilizado") || normalized.includes("already") || normalized.includes("duplic")) {
    return "QR (Quick Response) já utilizado";
  }
  if (normalized.includes("cancel")) return "QR (Quick Response) cancelado";
  if (normalized.includes("aprov") || normalized.includes("payment") || normalized.includes("pagamento")) return "Ingresso não aprovado";
  if (normalized.includes("expir")) return "Código expirado";
  if (normalized.includes("mal format") || normalized.includes("invalid") || normalized.includes("inval")) return "Código mal formatado";
  if (normalized.includes("produto")) return "Produto tentando entrar como ingresso";
  if (normalized.includes("permiss")) return "Usuário sem permissão";
  if (normalized.includes("scanner") || normalized.includes("tecnic") || normalized.includes("erro")) return "Erro técnico do scanner";
  if (normalized.includes("nao encontrado") || normalized.includes("not found") || normalized.includes("inexist")) {
    return "QR (Quick Response) inexistente";
  }
  return asString(value).trim() || "Leitura inválida sem motivo informado";
}

function ticketEntryInvalidReason(entry: Row, row: Row): string {
  const directReason = invalidReasonLabel(
    entry.invalidReason || entry.errorReason || entry.reason || entry.error || entry.message || entry.scanError
  );
  if (directReason) return directReason;

  const status = normalizeText(entry.status || entry.scanStatus || entry.situacao);
  if (status.includes("duplic")) return "QR (Quick Response) já utilizado";
  if (status.includes("cancel")) return "QR (Quick Response) cancelado";
  if (status.includes("inval")) return "Leitura inválida sem motivo informado";
  if (checkinAuditRows(row, entry).some(isDuplicateAuditEntry)) return "QR (Quick Response) já utilizado";
  return "";
}

function ticketQrStatus(row: Row): string {
  const entries = readTicketEntries(row);
  if (!entries.length) return ticketHasQrCode(row) ? "QR disponível" : "Sem QR (Quick Response)";
  const checked = entries.filter(isTicketEntryCheckedIn).length;
  const invalid = entries.filter((entry) => ticketEntryInvalidReason(entry, row)).length;
  if (checked >= entries.length) return "Usado";
  if (checked > 0) return "Parcialmente usado";
  if (invalid > 0) return "Com tentativa inválida";
  return "Ativo sem uso";
}

function ticketTransferLabel(row: Row): string {
  const transfers = extractTicketTransfers(row);
  if (transfers.length > 0 || asString(row.transferAt).trim()) {
    const latest = transfers.sort((left, right) => (right.at?.getTime() ?? 0) - (left.at?.getTime() ?? 0))[0];
    const target = latest?.target || asString(row.transferToUserName).trim();
    return target ? `Transferido para ${target}` : "Transferido";
  }
  return "Sem transferência";
}

function formatDateTimeShort(value: Date | null): string {
  return value
    ? value.toLocaleString("pt-BR", { day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit" })
    : "-";
}

function minuteBucketLabel(date: Date, intervalMinutes: number): string {
  const startMinute = Math.floor(date.getMinutes() / intervalMinutes) * intervalMinutes;
  const start = new Date(date);
  start.setMinutes(startMinute, 0, 0);
  const end = new Date(start.getTime() + intervalMinutes * 60_000);
  return `${String(start.getHours()).padStart(2, "0")}h${String(start.getMinutes()).padStart(2, "0")}-${String(
    end.getHours()
  ).padStart(2, "0")}h${String(end.getMinutes()).padStart(2, "0")}`;
}

function productEventId(row: Row): string {
  const data = asObject(row.data) ?? {};
  const eventParty = asObject(data.eventParty) ?? {};
  return asString(eventParty.eventId || row.eventId || row.eventoId).trim();
}

function productName(row: Row): string {
  return asString(row.nome || row.productName || row.name).trim() || "Produto";
}

function orderEventId(row: Row): string {
  const data = asObject(row.data) ?? {};
  const eventParty = asObject(data.eventParty) ?? {};
  return asString(row.eventId || row.eventoId || eventParty.eventId).trim();
}

function orderProductId(row: Row): string {
  return asString(row.productId || row.produtoId || row.product_id || row.produto_id).trim();
}

function orderQuantity(row: Row): number {
  return Math.max(1, Math.floor(parseNumber(row.quantidade ?? row.quantity ?? row.qtd, 1)));
}

function orderTotal(row: Row): number {
  const explicitTotal = parseNumber(row.total ?? row.valorTotal, Number.NaN);
  if (Number.isFinite(explicitTotal)) return explicitTotal;
  const price = parseNumber(row.price ?? row.preco ?? row.valor, 0);
  return orderEventId(row) ? price : price * orderQuantity(row);
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

function approvalMethodLabel(value: unknown, fallback = "-"): string {
  const method = normalizeText(value);
  if (method.includes("manual")) return "Manual";
  if (method.includes("pix")) return "Pix validado";
  if (method.includes("auto")) return "Automático";
  if (method.includes("import")) return "Importado";
  if (method.includes("cortesia")) return "Cortesia";
  if (method.includes("admin")) return "Admin";
  return fallback;
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
  return asString(row.approvedBy || row.eventCreatedByName || eventParty.approvedByName || eventParty.createdByName).trim() || "Sem aprovador";
}

function orderApprovalMethod(row: Row): string {
  const data = asObject(row.data) ?? {};
  const eventParty = asObject(data.eventParty) ?? {};
  return approvalMethodLabel(
    row.eventApprovalMethod || eventParty.approvalMethod,
    orderApproverName(row) !== "Sem aprovador" ? "Manual" : "-"
  );
}

function isManualOrder(row: Row): boolean {
  const data = asObject(row.data) ?? {};
  const eventParty = asObject(data.eventParty) ?? {};
  return Boolean(row.eventCreatedManually || eventParty.manualOrder || eventParty.createdManually);
}

function orderSource(row: Row): string {
  const data = asObject(row.data) ?? {};
  const eventParty = asObject(data.eventParty) ?? {};
  const source = asString(eventParty.source || row.source || row.canal_origem).trim();
  if (source) return source;
  if (isManualOrder(row)) return "Criado manualmente";
  const sellerType = normalizeText(row.seller_type);
  if (sellerType.includes("pdv")) return "PDV/bar";
  if (sellerType.includes("admin")) return "Admin";
  return "Checkout público";
}

function orderBuyerId(row: Row): string {
  return asString(row.userId || row.user_id || row.email || row.userName || row.id).trim() || `pedido-${asString(row.id)}`;
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

function activeVoucherEntries(row: Row): Row[] {
  return readVoucherEntries(row).filter((entry) => !isTransferredEntry(entry) && !isCancelledEntry(entry));
}

function orderAudienceQuantity(row: Row): number {
  const entries = readVoucherEntries(row);
  if (entries.length > 0) return activeVoucherEntries(row).length;
  const data = asObject(row.data) ?? {};
  const eventParty = asObject(data.eventParty) ?? {};
  if (eventParty.transferOrder && orderTotal(row) <= 0) return Math.max(1, orderQuantity(row));
  return orderQuantity(row);
}

function orderCheckedInAudienceQuantity(row: Row): number {
  const entries = activeVoucherEntries(row);
  if (entries.length > 0) return entries.filter(isRedeemedEntry).length;
  return orderRedeemedQuantity(row);
}

function classifyOrderAudience(row: Row, userById: Map<string, Row>): string {
  const data = asObject(row.data) ?? {};
  const eventParty = asObject(data.eventParty) ?? {};
  const manualCustomer = asObject(eventParty.manualCustomer) ?? {};
  const userId = asString(row.userId).trim();
  const user = userById.get(userId);
  const turma = asString(user?.turma || row.userTurma || eventParty.userTurma || manualCustomer.turma).trim();
  const category = normalizeText(row.eventItemCategory || eventParty.section || row.productName || row.productname);

  if (category.includes("convid")) return "Convidado";
  if (hasStudentClass(turma)) return "Aluno";
  if (
    isManualUserId(userId) ||
    isManualOrder(row) ||
    eventParty.manualOrder ||
    eventParty.createdManually ||
    eventParty.externalNumber ||
    manualCustomer.externalNumber ||
    manualCustomer.cpf ||
    manualCustomer.telefone ||
    manualCustomer.email
  ) {
    return "Não aluno";
  }
  return "Não classificado";
}

type TransferMetricEvent = {
  key: string;
  mode: "Manual" | "App";
  target: "Usuário da faculdade" | "Cadastro manual/externo";
  actor: string;
  at: Date | null;
};

function transferModeFromAudit(audit: Row, fallback: unknown): "Manual" | "App" {
  const text = normalizeText(
    `${audit.manual ? "manual" : ""} ${audit.mode || audit.method || audit.source || fallback || ""}`
  );
  return text.includes("manual") ? "Manual" : "App";
}

function transferTargetFromUserId(value: unknown): "Usuário da faculdade" | "Cadastro manual/externo" {
  return isManualUserId(value) ? "Cadastro manual/externo" : "Usuário da faculdade";
}

function transferActorName(audit: Row, fallback = "Sem usuário"): string {
  return (
    asString(audit.byUserName) ||
    asString(audit.transferByUserName) ||
    asString(audit.fromUserName) ||
    asString(audit.transferredFromUserName) ||
    fallback
  );
}

function extractTicketTransfers(row: Row): TransferMetricEvent[] {
  const data = asObject(row.data) ?? {};
  const audits: Row[] = [];
  const pushAudit = (value: unknown) => {
    const audit = asObject(value);
    if (audit) audits.push(audit);
  };
  if (Array.isArray(row.transferHistory)) {
    row.transferHistory.forEach(pushAudit);
  }
  const dataAudit = asObject(data.transferAudit);
  if (dataAudit && Object.keys(dataAudit).length > 0) audits.push(dataAudit);
  readTicketEntries(row).forEach((entry) => {
    if (Array.isArray(entry.transferHistory)) {
      entry.transferHistory.forEach(pushAudit);
    }
    if (entry.transferredAt || entry.transferredToUserId || entry.transferredFromUserId) {
      audits.push({
        at: entry.transferredAt,
        fromUserId: entry.transferredFromUserId,
        fromUserName: entry.transferredFromUserName,
        toUserId: entry.transferredToUserId || row.transferToUserId || row.userId,
        toUserName: entry.transferredToUserName || row.transferToUserName || row.userName,
        byUserName: entry.transferByUserName || row.transferByUserName,
        manual: data.manualTransfer,
        ticketToken: entry.token || entry.id,
      });
    }
  });
  if (!audits.length && row.transferAt) {
    audits.push({
      at: row.transferAt,
      fromUserId: row.transferFromUserId,
      fromUserName: row.transferFromUserName,
      toUserId: row.transferToUserId,
      toUserName: row.transferToUserName,
      byUserName: row.transferByUserName,
      manual: data.manualTransfer,
    });
  }

  return audits
    .map((audit, index) => {
      const toUserId = audit.toUserId || audit.transferredToUserId || row.transferToUserId || row.userId;
      const at = parseDate(audit.at || audit.transferredAt || row.transferAt);
      return {
        key:
          asString(audit.id) ||
          [
            "ticket",
            audit.fromOrderId || row.id,
            audit.toOrderId || row.id,
            audit.ticketToken || audit.token || index,
            audit.at || audit.transferredAt || row.transferAt || "",
          ]
            .map((entry) => asString(entry))
            .join(":"),
        mode: transferModeFromAudit(audit, row.approvalMethod),
        target: transferTargetFromUserId(toUserId),
        actor: transferActorName(audit, asString(row.transferByUserName) || asString(row.userName) || "Sem usuário"),
        at,
      };
    })
    .filter((transfer) => transfer.actor || transfer.at);
}

function extractProductTransfers(row: Row): TransferMetricEvent[] {
  const data = asObject(row.data) ?? {};
  const eventParty = asObject(data.eventParty) ?? {};
  const requests = Array.isArray(eventParty.transferRequests)
    ? eventParty.transferRequests.map(asObject).filter((entry): entry is Row => Boolean(entry))
    : [];
  return requests
    .filter((entry) => ["aceito", "accepted"].includes(normalizeText(entry.status)))
    .map((entry, index) => ({
      key: [
        "product",
        row.id,
        entry.id || index,
        entry.voucherId || "",
        entry.acceptedAt || entry.requestedAt || "",
      ]
        .map((value) => asString(value))
        .join(":"),
      mode: "App" as const,
      target: transferTargetFromUserId(entry.toUserId),
      actor: asString(entry.fromUserName) || asString(row.userName) || "Sem usuário",
      at: parseDate(entry.acceptedAt || entry.requestedAt || row.updatedAt || row.createdAt),
    }));
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
  if (dates.length) {
    return dates.sort((left, right) => right.getTime() - left.getTime())[0];
  }
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

function orderHasCode(row: Row): boolean {
  return readVoucherEntries(row).some((entry) =>
    Boolean(asString(entry.code || entry.manualNumber || entry.token || entry.id).trim())
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
  if (!entries.length) return orderHasCode(row) ? "Ativo" : "Sem QR (Quick Response)";
  const redeemed = entries.filter(isRedeemedEntry).length;
  const cancelled = entries.filter(isCancelledEntry).length;
  const invalid = entries.filter((entry) => normalizeText(entry.status || entry.situacao).includes("inval")).length;
  if (redeemed >= entries.length) return "Utilizado";
  if (redeemed > 0) return "Parcial";
  if (cancelled >= entries.length) return "Cancelado";
  if (invalid > 0) return "Inválido";
  return "Ativo";
}

function orderCodes(row: Row): string[] {
  const data = asObject(row.data) ?? {};
  const eventParty = asObject(data.eventParty) ?? {};
  const direct = [
    eventParty.orderCode,
    eventParty.orderNumber,
    eventParty.manualCode,
    eventParty.manualNumber,
    eventParty.fichaNumero,
    row.code,
    row.codigo,
  ]
    .map((value) => asString(value).trim())
    .filter(Boolean);
  const entryCodes = readVoucherEntries(row)
    .flatMap((entry) => [entry.code, entry.manualNumber, entry.token, entry.id])
    .map((value) => asString(value).trim())
    .filter(Boolean);
  return Array.from(new Set([...direct, ...entryCodes]));
}

function orderCreatedByName(row: Row): string {
  const data = asObject(row.data) ?? {};
  const eventParty = asObject(data.eventParty) ?? {};
  return asString(row.eventCreatedByName || eventParty.createdByName || data.createdByName).trim() || "-";
}

function orderClassName(row: Row, userById: Map<string, Row>): string {
  const data = asObject(row.data) ?? {};
  const eventParty = asObject(data.eventParty) ?? {};
  const manualCustomer = asObject(eventParty.manualCustomer) ?? {};
  const user = userById.get(asString(row.userId).trim());
  return (
    asString(row.userTurma || user?.turma || eventParty.userTurma || manualCustomer.turma).trim() ||
    "Sem turma"
  );
}

function eventCost(row?: Row | null): number {
  if (!row) return 0;
  const dataExtra = asObject(row.data_extra) ?? {};
  const eventParty = asObject(dataExtra.eventParty) ?? {};
  const stats = asObject(row.stats) ?? {};
  const costs = [
    row.custo,
    row.custos,
    row.cost,
    row.totalCost,
    row.custoTotal,
    row.valorCusto,
    dataExtra.custo,
    dataExtra.custos,
    dataExtra.cost,
    dataExtra.totalCost,
    eventParty.custo,
    eventParty.custos,
    eventParty.cost,
    eventParty.totalCost,
    stats.custo,
    stats.custos,
    stats.cost,
    stats.totalCost,
  ];
  return costs.reduce<number>((sum, value) => {
    if (Array.isArray(value)) {
      return (
        sum +
        value.reduce<number>((innerSum, entry) => {
          const row = asObject(entry) ?? {};
          return innerSum + parseNumber(row.valor ?? row.value ?? row.total ?? row.custo ?? row.cost ?? entry, 0);
        }, 0)
      );
    }
    const objectValue = asObject(value);
    if (objectValue) {
      return sum + Object.values(objectValue).reduce<number>((innerSum, entry) => innerSum + parseNumber(entry, 0), 0);
    }
    return sum + parseNumber(value, 0);
  }, 0);
}

function hasEventCostField(row?: Row | null): boolean {
  if (!row) return false;
  const dataExtra = asObject(row.data_extra) ?? {};
  const eventParty = asObject(dataExtra.eventParty) ?? {};
  const stats = asObject(row.stats) ?? {};
  const values = [
    row.custo,
    row.custos,
    row.cost,
    row.totalCost,
    row.custoTotal,
    row.valorCusto,
    dataExtra.custo,
    dataExtra.custos,
    dataExtra.cost,
    dataExtra.totalCost,
    eventParty.custo,
    eventParty.custos,
    eventParty.cost,
    eventParty.totalCost,
    stats.custo,
    stats.custos,
    stats.cost,
    stats.totalCost,
  ];
  return values.some((value) => {
    if (value === null || typeof value === "undefined") return false;
    if (typeof value === "string") return value.trim().length > 0;
    if (Array.isArray(value)) return value.length > 0;
    if (typeof value === "object") return Object.keys(value as Record<string, unknown>).length > 0;
    return true;
  });
}

function isCourtesyText(value: unknown): boolean {
  const normalized = normalizeText(value);
  return normalized.includes("cortesia") || normalized.includes("gratuito") || normalized.includes("free");
}

function eventLotRows(row?: Row | null): Row[] {
  if (!row) return [];
  const dataExtra = asObject(row.data_extra) ?? {};
  const stats = asObject(row.stats) ?? {};
  const candidates = [row.lotes, row.lots, dataExtra.lotes, dataExtra.lots, stats.lotes];
  for (const candidate of candidates) {
    if (Array.isArray(candidate)) {
      return candidate.map((entry) => asObject(entry)).filter((entry): entry is Row => Boolean(entry));
    }
  }
  return [];
}

function eventLotUnitPrice(row: Row | undefined, lotId: unknown, lotName: unknown): number {
  const cleanLotId = asString(lotId).trim();
  const cleanLotName = normalizeText(lotName);
  const lot = eventLotRows(row).find((entry) => {
    const idMatches = cleanLotId && asString(entry.id).trim() === cleanLotId;
    const nameMatches = cleanLotName && normalizeText(entry.nome || entry.name || entry.loteNome) === cleanLotName;
    return idMatches || nameMatches;
  });
  return parseNumber(lot?.preco ?? lot?.price ?? lot?.valor ?? lot?.value, Number.NaN);
}

function expectedTicketTotal(row: Row, relatedEvent?: Row): number {
  const lotPrice = eventLotUnitPrice(relatedEvent, row.loteId, ticketLotName(row));
  const unit = Number.isFinite(lotPrice) ? lotPrice : parseNumber(row.valorUnitario ?? row.unitPrice, Number.NaN);
  return Number.isFinite(unit) ? unit * ticketQuantity(row) : Number.NaN;
}

function expectedOrderTotal(row: Row, productsById: Map<string, Row>): number {
  const product = productsById.get(orderProductId(row));
  const unit = parseNumber(product?.preco ?? product?.price ?? row.price ?? row.preco, Number.NaN);
  return Number.isFinite(unit) ? unit * orderQuantity(row) : Number.NaN;
}

type EntityScopeType = Exclude<ScopeType, "tenant">;

type EntityScopeMeta = {
  scopeType: EntityScopeType;
  name: string;
};

type EventScopeLink = EntityScopeMeta & {
  ownerId: string;
};

type EntityMemberMeta = {
  roles: string[];
  scopeTypes: EntityScopeType[];
  management: boolean;
};

type ForcedEntityScope = {
  id: string;
  scopeType: EntityScopeType;
};

function isManagementEntityRole(value: unknown): boolean {
  const role = normalizeText(value);
  return (
    role.startsWith("president") ||
    role.startsWith("vice") ||
    role.startsWith("secretar") ||
    role.startsWith("tesour") ||
    role.startsWith("diretor")
  );
}

function entityMemberRows(row: Row): Array<{ id: string; role: string }> {
  const data = asObject(row.data) ?? {};
  const members = [row.membros, data.membros, data.members]
    .flatMap((candidate) => asArray(candidate))
    .map((candidate) => asObject(candidate))
    .filter((candidate): candidate is Row => Boolean(candidate))
    .map((member) => ({
      id: asString(member.id || member.uid || member.userId || member.user_id).trim(),
      role: asString(member.cargo || member.role || member.funcao || member.position).trim() || "Membro",
    }))
    .filter((member) => member.id);

  const memberIds = [row.membrosIds, row.memberIds, data.membrosIds, data.memberIds]
    .flatMap((candidate) => asArray(candidate))
    .map((memberId) => asString(memberId).trim())
    .filter(Boolean)
    .map((id) => ({ id, role: "Membro" }));

  const seen = new Set<string>();
  return [...members, ...memberIds].filter((member) => {
    const key = `${member.id}:${member.role}`;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

function buildEntityMemberIndex(
  rows: Row[],
  forcedScope?: ForcedEntityScope | null
): Map<string, EntityMemberMeta> {
  const index = new Map<string, EntityMemberMeta>();
  rows.forEach((row) => {
    const scopeType = resolvedEntityScopeType(row, forcedScope);
    entityMemberRows(row).forEach((member) => {
      const current = index.get(member.id) ?? { roles: [], scopeTypes: [], management: false };
      if (!current.roles.includes(member.role)) current.roles.push(member.role);
      if (!current.scopeTypes.includes(scopeType)) current.scopeTypes.push(scopeType);
      current.management = current.management || isManagementEntityRole(member.role);
      index.set(member.id, current);
    });
  });
  return index;
}

function classifyTicketOperationalCategory(
  row: Row,
  entry: Row | null,
  userById: Map<string, Row>,
  entityMemberIndex: Map<string, EntityMemberMeta>
): string {
  const data = asObject(row.data) ?? {};
  const category = normalizeText(row.itemCategory || data.itemCategory || ticketLotName(row));
  const lote = normalizeText(ticketLotName(row));
  const userId = ticketEntryUserId(entry ?? {}) || asString(row.userId).trim();
  const user = userById.get(userId);
  const turma = asString(user?.turma || row.userTurma || entry?.holderTurma || entry?.userTurma).trim();
  const member = entityMemberIndex.get(userId);
  const manualGateEntry = Boolean(data.manualGateEntry) || isManualUserId(userId);

  if (manualGateEntry || category.includes("porta") || lote.includes("porta")) return "Entrada/porta";
  if (isCourtesyText(`${category} ${lote} ${row.itemName || ""}`)) return "Cortesia";
  if (category.includes("convid") || lote.includes("convid")) return "Convidado";
  if (member?.management) return "Diretoria";
  if (member) return "Membro";
  if (category.includes("extern") || lote.includes("extern")) return "Externo";
  if (category.includes("nao aluno") || lote.includes("nao aluno")) return "Não aluno";
  if (hasStudentClass(turma)) return "Aluno";
  return "Não aluno";
}

function buildForcedEntityScope(scopeType: ScopeType, scopeId: string): ForcedEntityScope | null {
  const cleanScopeId = scopeId.trim();
  if (!cleanScopeId || cleanScopeId === "todos" || scopeType === "tenant") return null;
  return { id: cleanScopeId, scopeType };
}

function entityName(row: Row): string {
  return asString(row.sigla || row.nome || row.name).trim() || asString(row.id).trim();
}

function entityScopeType(row: Row): EntityScopeType {
  const data = asObject(row.data) ?? {};
  const category = normalizeText(row.category || row.categoria || data.category || data.categoria || data.tipo);
  if (category.includes("diretorio")) return "directory";
  if (category.includes("comissao") || category.includes("comiss")) return "commission";
  if (asString(row.turmaId || data.turmaId).trim()) return "commission";
  return "league";
}

function resolvedEntityScopeType(row: Row, forcedScope?: ForcedEntityScope | null): EntityScopeType {
  const id = asString(row.id).trim();
  if (forcedScope && id === forcedScope.id) return forcedScope.scopeType;
  return entityScopeType(row);
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

function buildEntityScopeIndex(
  rows: Row[],
  forcedScope?: ForcedEntityScope | null
): Map<string, EntityScopeMeta> {
  const index = new Map<string, EntityScopeMeta>();
  rows.forEach((row) => {
    const id = asString(row.id).trim();
    if (!id) return;
    index.set(id, {
      scopeType: resolvedEntityScopeType(row, forcedScope),
      name: entityName(row),
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
      [
        row.globalEventId,
        row.eventId,
        row.eventoId,
        row.id,
        eventIdFromLink(row.linkEvento),
        eventIdFromLink(row.href),
        eventIdFromLink(row.url),
      ]
        .map((entry) => asString(entry).trim())
        .filter(Boolean)
    )
  );
}

function entityEventRows(row: Row): Row[] {
  const data = asObject(row.data) ?? {};
  return [row.eventos, data.eventos, data.events]
    .flatMap((candidate) => (Array.isArray(candidate) ? candidate : []))
    .map((entry) => asObject(entry))
    .filter((entry): entry is Row => Boolean(entry));
}

function buildEventScopeIndex(
  rows: Row[],
  forcedScope?: ForcedEntityScope | null
): Map<string, EventScopeLink> {
  const index = new Map<string, EventScopeLink>();
  rows.forEach((entity) => {
    const ownerId = asString(entity.id).trim();
    if (!ownerId) return;
    const scopeType = resolvedEntityScopeType(entity, forcedScope);
    const name = entityName(entity);
    entityEventRows(entity).forEach((eventRow) => {
      linkedEventIdsFromEntityEvent(eventRow).forEach((linkedEventId) => {
        index.set(linkedEventId, { ownerId, scopeType, name });
      });
    });
  });
  return index;
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
  eventScopeIndex?: Map<string, EventScopeLink>
): Record<EntityScopeType, string[]> {
  const stats = asObject(row.stats) ?? {};
  const dataExtra = asObject(row.data_extra) ?? {};
  const eventParty = asObject(dataExtra.eventParty) ?? {};
  const scopeIds = emptyScopeIds();
  const linkedScope = eventScopeIndex?.get(eventId(row));
  if (linkedScope) {
    addScopedId(scopeIds, entityIndex, linkedScope.scopeType, linkedScope.ownerId);
  }
  [
    row.leagueId,
    row.ligaId,
    stats.leagueId,
    stats.ligaId,
    stats.sellerId,
    eventParty.leagueId,
  ].forEach((value) => addScopedId(scopeIds, entityIndex, "league", value));
  [
    row.directoryId,
    row.diretorioId,
    stats.directoryId,
    stats.diretorioId,
    eventParty.directoryId,
  ].forEach((value) => addScopedId(scopeIds, entityIndex, "directory", value));
  [
    row.commissionId,
    row.comissaoId,
    stats.commissionId,
    stats.comissaoId,
    eventParty.commissionId,
  ].forEach((value) => addScopedId(scopeIds, entityIndex, "commission", value));
  addScopedId(scopeIds, entityIndex, "league", stats.collectiveId);
  return uniqueScopeIds(scopeIds);
}

function canonicalEventOwnerScope(
  row: Row,
  entityIndex: Map<string, EntityScopeMeta>,
  eventScopeIndex?: Map<string, EventScopeLink>
): { scopeType: ScopeType; scopeId: string } {
  const ids = eventScopeIds(row, entityIndex, eventScopeIndex);
  const declared = declaredExternalScopeType(row);
  if (declared && ids[declared]?.[0]) return { scopeType: declared, scopeId: ids[declared][0] };
  if (ids.directory[0]) return { scopeType: "directory", scopeId: ids.directory[0] };
  if (ids.commission[0]) return { scopeType: "commission", scopeId: ids.commission[0] };
  if (ids.league[0]) return { scopeType: "league", scopeId: ids.league[0] };
  return { scopeType: "tenant", scopeId: "todos" };
}

function canonicalEventWorkspacePath(scopeType: ScopeType, scopeId: string, targetEventId: string, sectionPath = "edicao"): string {
  const encodedEventId = encodeURIComponent(targetEventId);
  const cleanSection = sectionPath.replace(/^\/+/, "") || "edicao";
  if (scopeType === "directory") return `/diretorio/configurar/${encodeURIComponent(scopeId)}/eventos/${encodedEventId}/${cleanSection}`;
  if (scopeType === "commission") return `/comissoes/configurar/${encodeURIComponent(scopeId)}/eventos/${encodedEventId}/${cleanSection}`;
  if (scopeType === "league") return `/ligas/${encodeURIComponent(scopeId)}/eventos/${encodedEventId}/${cleanSection}`;
  return `/admin/eventos/${encodedEventId}/${cleanSection}`;
}

function rowScopeIds(
  row: Row,
  relatedEvent: Row | undefined,
  entityIndex: Map<string, EntityScopeMeta>,
  eventScopeIndex?: Map<string, EventScopeLink>
): Record<EntityScopeType, string[]> {
  const data = asObject(row.data) ?? {};
  const eventParty = asObject(data.eventParty) ?? {};
  const sellerType = normalizeText(row.seller_type || row.sellerType);
  const sellerId = asString(row.seller_id || row.sellerId).trim();
  const scopeIds = relatedEvent ? eventScopeIds(relatedEvent, entityIndex, eventScopeIndex) : emptyScopeIds();

  [
    row.leagueId,
    row.ligaId,
    data.leagueId,
    data.ligaId,
    eventParty.leagueId,
  ].forEach((value) => addScopedId(scopeIds, entityIndex, "league", value));
  [
    row.directoryId,
    row.diretorioId,
    data.directoryId,
    data.diretorioId,
    eventParty.directoryId,
  ].forEach((value) => addScopedId(scopeIds, entityIndex, "directory", value));
  [
    row.commissionId,
    row.comissaoId,
    data.commissionId,
    data.comissaoId,
    eventParty.commissionId,
  ].forEach((value) => addScopedId(scopeIds, entityIndex, "commission", value));

  if (sellerType.includes("directory") || sellerType.includes("diretorio")) {
    addScopedId(scopeIds, entityIndex, "directory", sellerId);
  } else if (sellerType.includes("commission") || sellerType.includes("comissao")) {
    addScopedId(scopeIds, entityIndex, "commission", sellerId);
  } else if (sellerType.includes("league") || sellerType.includes("liga")) {
    addScopedId(scopeIds, entityIndex, "league", sellerId);
  }

  return uniqueScopeIds(scopeIds);
}

function hasExternalEventScope(
  row: Row,
  relatedEvent: Row | undefined,
  entityIndex: Map<string, EntityScopeMeta>,
  eventScopeIndex?: Map<string, EventScopeLink>
): boolean {
  const ids = rowScopeIds(row, relatedEvent, entityIndex, eventScopeIndex);
  return Boolean(
    ids.league.length > 0 ||
      ids.directory.length > 0 ||
      ids.commission.length > 0 ||
      declaredExternalScopeType(row) ||
      declaredExternalScopeType(relatedEvent)
  );
}

function isTenantOwnedRow(
  row: Row,
  relatedEvent: Row | undefined,
  entityIndex: Map<string, EntityScopeMeta>,
  eventScopeIndex?: Map<string, EventScopeLink>
): boolean {
  return !hasExternalEventScope(row, relatedEvent, entityIndex, eventScopeIndex);
}

function dateInPeriod(date: Date | null, startDate: string, endDate: string): boolean {
  if (!date) return true;
  if (startDate) {
    const start = parseDate(`${startDate}T00:00:00`);
    if (start && date.getTime() < start.getTime()) return false;
  }
  if (endDate) {
    const end = parseDate(`${endDate}T23:59:59`);
    if (end && date.getTime() > end.getTime()) return false;
  }
  return true;
}

function periodFromDate(date: Date | null): string {
  if (!date) return "Sem horário";
  const hour = date.getHours();
  if (hour < 6) return "Madrugada";
  if (hour < 12) return "Manhã";
  if (hour < 18) return "Tarde";
  return "Noite";
}

function addMetric(
  map: Map<string, MetricRow>,
  name: string,
  quantity: number,
  value: number,
  secondary = 0,
  href = ""
): void {
  const cleanName = name.trim() || "Sem dado";
  const current = map.get(cleanName) ?? { name: cleanName, quantity: 0, value: 0, secondary: 0 };
  current.quantity += quantity;
  current.value += value;
  current.secondary = (current.secondary ?? 0) + secondary;
  current.average = safeDivide(current.value, current.quantity);
  if (!current.href && href) current.href = href;
  map.set(cleanName, current);
}

function metricRows(map: Map<string, MetricRow>, limit = 12): MetricRow[] {
  return Array.from(map.values())
    .sort((left, right) => right.value - left.value || right.quantity - left.quantity || left.name.localeCompare(right.name, "pt-BR"))
    .slice(0, limit);
}

function buildScopeOptions(
  data: BiData,
  entityIndex: Map<string, EntityScopeMeta>,
  eventScopeIndex: Map<string, EventScopeLink>
): Record<EntityScopeType, ScopeOption[]> {
  const maps: Record<EntityScopeType, Map<string, string>> = {
    league: new Map(),
    directory: new Map(),
    commission: new Map(),
  };
  const add = (type: EntityScopeType, id: unknown, name?: unknown) => {
    const cleanId = asString(id).trim();
    if (!cleanId) return;
    const cleanName = asString(name).trim() || entityIndex.get(cleanId)?.name || cleanId;
    maps[type].set(cleanId, cleanName);
  };

  data.entities.forEach((entity) => {
    const id = asString(entity.id).trim();
    if (!id) return;
    add(entityIndex.get(id)?.scopeType ?? entityScopeType(entity), id, entityName(entity));
  });
  data.events.forEach((event) => {
    const stats = asObject(event.stats) ?? {};
    const ids = eventScopeIds(event, entityIndex, eventScopeIndex);
    ids.league.forEach((id) => add("league", id, stats.leagueName || stats.ligaNome));
    ids.directory.forEach((id) => add("directory", id, stats.directoryName || stats.diretorioNome));
    ids.commission.forEach((id) => add("commission", id, stats.commissionName || stats.comissaoNome));
  });
  [...data.products, ...data.orders].forEach((row) => {
    const sellerType = normalizeText(row.seller_type || row.sellerType);
    const sellerId = asString(row.seller_id || row.sellerId).trim();
    const sellerName = asString(row.seller_name || row.sellerName).trim();
    if (sellerType.includes("directory") || sellerType.includes("diretorio")) add("directory", sellerId, sellerName);
    else if (sellerType.includes("commission") || sellerType.includes("comissao")) add("commission", sellerId, sellerName);
    else if (sellerType.includes("league") || sellerType.includes("liga")) {
      add(entityIndex.get(sellerId)?.scopeType ?? "league", sellerId, sellerName);
    }
  });

  return {
    league: Array.from(maps.league.entries()).map(([id, name]) => ({ id, name })),
    directory: Array.from(maps.directory.entries()).map(([id, name]) => ({ id, name })),
    commission: Array.from(maps.commission.entries()).map(([id, name]) => ({ id, name })),
  };
}

function KpiCard({
  label,
  value,
  hint,
  icon,
  info,
  href,
}: {
  label: string;
  value: string;
  hint: string;
  icon: ReactNode;
  info?: string;
  href?: string;
}) {
  const card = (
    <div className="rounded-lg border border-zinc-800 bg-zinc-950/80 p-4 shadow-[0_18px_60px_rgba(0,0,0,0.24)]">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="flex items-center gap-1.5 text-[10px] font-black uppercase tracking-[0.2em] text-zinc-500">
            {label}
            {info ? (
              <span
                className="inline-flex h-4 w-4 items-center justify-center rounded bg-sky-500 text-black"
                title={info}
                aria-label={info}
              >
                <Info size={11} />
              </span>
            ) : null}
          </p>
          <p className="mt-3 text-2xl font-black text-white">{value}</p>
        </div>
        <span className="inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-lg border border-emerald-400/20 bg-emerald-400/10 text-emerald-300">
          {icon}
        </span>
      </div>
      <p className="mt-3 text-xs font-bold text-zinc-500">{hint}</p>
    </div>
  );
  return href ? (
    <Link href={href} prefetch={false} className="block rounded-lg outline-none transition hover:border-emerald-400/50 focus-visible:ring-2 focus-visible:ring-emerald-400">
      {card}
    </Link>
  ) : (
    card
  );
}

function KpiGrid({ children }: { children: ReactNode }) {
  return <section className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">{children}</section>;
}

function ChartPanel({
  title,
  subtitle,
  children,
  info,
  toolbar,
  cornerMetric,
  footer,
}: {
  title: string;
  subtitle?: string;
  children: ReactNode;
  info?: string;
  toolbar?: ReactNode;
  cornerMetric?: string;
  footer?: ReactNode;
}) {
  const [infoOpen, setInfoOpen] = useState(false);

  return (
    <section className="relative rounded-lg border border-zinc-800 bg-zinc-950/70 p-4">
      <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="flex items-center gap-2 text-xs font-black uppercase tracking-[0.2em] text-zinc-300">
            {title}
            {info ? (
              <button
                type="button"
                onClick={() => setInfoOpen(true)}
                className="inline-flex h-5 w-5 items-center justify-center rounded bg-sky-500 text-black transition hover:bg-sky-300"
                aria-label={`Entender ${title}`}
              >
                <Info size={11} />
              </button>
            ) : null}
          </h2>
          {subtitle ? <p className="mt-1 text-xs font-bold text-zinc-500">{subtitle}</p> : null}
        </div>
        {toolbar ? <div className="shrink-0">{toolbar}</div> : null}
      </div>
      {cornerMetric ? (
        <div className="absolute left-4 top-14 z-10 rounded border border-emerald-400/25 bg-black/70 px-2.5 py-1 text-[10px] font-black uppercase tracking-[0.16em] text-emerald-300">
          {cornerMetric}
        </div>
      ) : null}
      <div className="h-[340px] min-w-0 sm:h-[360px]">{children}</div>
      {footer ? <div className="mt-3">{footer}</div> : null}
      {info && infoOpen ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4">
          <div className="w-full max-w-lg rounded-lg border border-zinc-700 bg-zinc-950 p-5 shadow-2xl">
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="text-[10px] font-black uppercase tracking-[0.22em] text-sky-300">Como funciona</p>
                <h3 className="mt-1 text-lg font-black uppercase text-white">{title}</h3>
              </div>
              <button
                type="button"
                onClick={() => setInfoOpen(false)}
                className="rounded-lg border border-zinc-700 px-3 py-2 text-xs font-black uppercase text-zinc-300 transition hover:border-zinc-500 hover:text-white"
              >
                Fechar
              </button>
            </div>
            <p className="mt-4 whitespace-pre-line text-sm font-semibold leading-6 text-zinc-300">{info}</p>
          </div>
        </div>
      ) : null}
    </section>
  );
}

function EmptyChart() {
  return (
    <div className="flex h-full items-center justify-center rounded-lg border border-dashed border-zinc-800 text-sm font-bold text-zinc-600">
      Sem dados para o filtro atual.
    </div>
  );
}

function metricFromChartEvent(entry: unknown): MetricRow | null {
  if (!entry || typeof entry !== "object") return null;
  if ("href" in entry || "name" in entry) return entry as MetricRow;
  if ("payload" in entry) {
    const payload = (entry as { payload?: unknown }).payload;
    if (payload && typeof payload === "object") return payload as MetricRow;
  }
  return null;
}

function openMetricHref(entry: unknown): void {
  const metric = metricFromChartEvent(entry);
  const href = metric?.href?.trim();
  if (href && typeof window !== "undefined") {
    window.location.href = href;
  }
}

function FilterLinkChips({
  links,
  label = "Abrir filtro no extrato",
}: {
  links: Array<{ label: string; href?: string; color?: string }>;
  label?: string;
}) {
  const visibleLinks = links.filter((link) => link.href?.trim());
  if (!visibleLinks.length) return null;
  return (
    <div className="flex flex-wrap items-center gap-2 text-[10px] font-black uppercase tracking-[0.12em]">
      <span className="text-zinc-500">{label}:</span>
      {visibleLinks.map((link) => (
        <Link
          key={`${link.label}-${link.href}`}
          href={link.href || "#"}
          prefetch={false}
          className="inline-flex items-center gap-1 rounded border border-zinc-700 px-2 py-1 text-zinc-300 transition hover:border-emerald-400 hover:text-white"
        >
          {link.color ? <span className="h-2.5 w-2.5 rounded-full" style={{ backgroundColor: link.color }} /> : null}
          {link.label}
        </Link>
      ))}
    </div>
  );
}

function Bars({ data, dataKey = "quantity", currency = false }: { data: MetricRow[]; dataKey?: keyof MetricRow; currency?: boolean }) {
  if (!data.length) return <EmptyChart />;
  const metricName = dataKey === "value" ? "Receita" : "Quantidade";
  const hasLinks = data.some((row) => row.href);
  return (
    <ResponsiveContainer width="100%" height="100%" minWidth={0} minHeight={240}>
      <BarChart data={data} layout="vertical" margin={{ top: 8, right: 18, left: 18, bottom: 8 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.08)" horizontal={false} />
        <XAxis
          type="number"
          stroke="rgba(255,255,255,0.45)"
          tick={{ fontSize: 11 }}
          tickFormatter={(value) => (currency ? formatCurrency(Number(value)) : formatNumber(Number(value)))}
        />
        <YAxis dataKey="name" type="category" width={118} stroke="rgba(255,255,255,0.45)" tick={{ fontSize: 11 }} />
        <Tooltip
          contentStyle={chartTooltipStyle}
          labelStyle={chartTooltipLabelStyle}
          itemStyle={chartTooltipItemStyle}
          formatter={(value) => (currency ? formatCurrency(Number(value)) : formatNumber(Number(value)))}
        />
        <Bar
          dataKey={dataKey as string}
          name={metricName}
          fill="#22c55e"
          radius={[0, 7, 7, 0]}
          cursor={hasLinks ? "pointer" : undefined}
          onClick={openMetricHref}
        >
          <LabelList
            dataKey={dataKey as string}
            position="right"
            fill="#ffffff"
            fontSize={11}
            fontWeight={900}
            formatter={(value: unknown) => (currency ? formatShortChartValue(Number(value), "currency") : formatShortChartValue(Number(value), "number"))}
          />
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}

function BarsDual({
  data,
  valueName = "Receita",
  quantityName = "Quantidade",
  valueFormat = "currency",
  quantityFormat = "number",
}: {
  data: MetricRow[];
  valueName?: string;
  quantityName?: string;
  valueFormat?: ChartValueFormat;
  quantityFormat?: ChartValueFormat;
}) {
  if (!data.length) return <EmptyChart />;
  const hasLinks = data.some((row) => row.href);
  return (
    <ResponsiveContainer width="100%" height="100%" minWidth={0} minHeight={240}>
      <ComposedChart data={data} margin={{ top: 30, right: 12, left: 10, bottom: 44 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.08)" vertical={false} />
        <XAxis dataKey="name" stroke="rgba(255,255,255,0.45)" tick={{ fontSize: 11 }} angle={-18} textAnchor="end" height={58} />
        <YAxis
          yAxisId="value"
          stroke="rgba(56,189,248,0.75)"
          tick={{ fontSize: 11 }}
          tickFormatter={(value) => formatChartValue(Number(value), valueFormat)}
          domain={[0, (dataMax: number) => Math.max(1, Math.ceil(dataMax * 1.18))]}
        />
        <YAxis
          yAxisId="quantity"
          orientation="right"
          stroke="rgba(34,197,94,0.8)"
          tick={{ fontSize: 11 }}
          tickFormatter={(value) => formatChartValue(Number(value), quantityFormat)}
        />
        <Tooltip
          contentStyle={chartTooltipStyle}
          labelStyle={chartTooltipLabelStyle}
          itemStyle={chartTooltipItemStyle}
          formatter={(value, name) =>
            String(name) === quantityName
              ? formatChartValue(Number(value), quantityFormat)
              : formatChartValue(Number(value), valueFormat)
          }
        />
        <Legend />
        <Bar
          yAxisId="value"
          dataKey="value"
          name={valueName}
          fill="#38bdf8"
          radius={[7, 7, 0, 0]}
          cursor={hasLinks ? "pointer" : undefined}
          onClick={openMetricHref}
        >
          <LabelList
            dataKey="value"
            position="top"
            fill="#ffffff"
            fontSize={11}
            fontWeight={900}
            formatter={(value: unknown) => formatShortChartValue(Number(value), valueFormat)}
          />
        </Bar>
        <Line
          yAxisId="quantity"
          type="monotone"
          dataKey="quantity"
          name={quantityName}
          stroke="#22c55e"
          strokeWidth={3}
          dot={{ r: 4 }}
          cursor={hasLinks ? "pointer" : undefined}
          onClick={openMetricHref}
        />
      </ComposedChart>
    </ResponsiveContainer>
  );
}

function ColumnBars({
  data,
  dataKey = "value",
  valueName = "Valor",
  valueFormat = "currency",
}: {
  data: MetricRow[];
  dataKey?: keyof MetricRow;
  valueName?: string;
  valueFormat?: ChartValueFormat;
}) {
  const rows = data
    .filter((row) => Number(row[dataKey] ?? 0) > 0)
    .sort((left, right) => Number(right[dataKey] ?? 0) - Number(left[dataKey] ?? 0));
  if (!rows.length) return <EmptyChart />;
  const hasLinks = rows.some((row) => row.href);
  return (
    <ResponsiveContainer width="100%" height="100%" minWidth={0} minHeight={240}>
      <BarChart data={rows} margin={{ top: 30, right: 16, left: 8, bottom: 42 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.08)" vertical={false} />
        <XAxis dataKey="name" stroke="rgba(255,255,255,0.45)" tick={{ fontSize: 11 }} angle={-16} textAnchor="end" height={54} interval={0} />
        <YAxis
          stroke="rgba(255,255,255,0.45)"
          tick={{ fontSize: 11 }}
          tickFormatter={(value) => formatChartValue(Number(value), valueFormat)}
          domain={[0, (dataMax: number) => Math.max(1, Math.ceil(dataMax * 1.18))]}
        />
        <Tooltip
          contentStyle={chartTooltipStyle}
          labelStyle={chartTooltipLabelStyle}
          itemStyle={chartTooltipItemStyle}
          formatter={(value) => [formatChartValue(Number(value), valueFormat), valueName]}
        />
        <Legend />
        <Bar dataKey={dataKey as string} name={valueName} fill="#22c55e" radius={[7, 7, 0, 0]} cursor={hasLinks ? "pointer" : undefined} onClick={openMetricHref}>
          <LabelList dataKey={dataKey as string} position="top" fill="#ffffff" fontSize={11} fontWeight={900} formatter={(value: unknown) => formatShortChartValue(Number(value), valueFormat)} />
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}

function LineMetric({ data }: { data: MetricRow[] }) {
  if (!data.length) return <EmptyChart />;
  return (
    <ResponsiveContainer width="100%" height="100%" minWidth={0} minHeight={240}>
      <LineChart data={data} margin={{ top: 8, right: 18, left: 0, bottom: 28 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.08)" vertical={false} />
        <XAxis dataKey="name" stroke="rgba(255,255,255,0.45)" tick={{ fontSize: 11 }} />
        <YAxis stroke="rgba(255,255,255,0.45)" tick={{ fontSize: 11 }} />
        <Tooltip
          contentStyle={chartTooltipStyle}
          labelStyle={chartTooltipLabelStyle}
          itemStyle={chartTooltipItemStyle}
          formatter={(value) => [formatNumber(Number(value)), "Quantidade"]}
        />
        <Line type="monotone" dataKey="quantity" name="Quantidade" stroke="#22c55e" strokeWidth={3} dot={{ r: 4 }} />
      </LineChart>
    </ResponsiveContainer>
  );
}

function PieMetric({
  data,
  dataKey = "quantity",
  valueName = "Quantidade",
  valueFormat = "number",
}: {
  data: MetricRow[];
  dataKey?: keyof Pick<MetricRow, "quantity" | "value">;
  valueName?: string;
  valueFormat?: ChartValueFormat;
}) {
  const visible = data.filter((row) => Number(row[dataKey] ?? 0) > 0);
  if (!visible.length) return <EmptyChart />;
  const hasLinks = visible.some((row) => row.href);
  return (
    <div className="flex h-full min-h-0 flex-col">
      <div className="min-h-0 flex-1">
        <ResponsiveContainer width="100%" height="100%" minWidth={0} minHeight={240}>
          <PieChart>
            <Pie
              data={visible}
              dataKey={dataKey}
              nameKey="name"
              innerRadius={62}
              outerRadius={98}
              paddingAngle={3}
              cursor={hasLinks ? "pointer" : undefined}
              onClick={openMetricHref}
            >
              {visible.map((entry, index) => (
                <Cell key={entry.name} fill={COLORS[index % COLORS.length]} />
              ))}
              <LabelList
                dataKey={dataKey}
                position="outside"
                fill="#ffffff"
                fontSize={11}
                fontWeight={900}
                formatter={(value: unknown) => formatShortChartValue(Number(value), valueFormat)}
              />
            </Pie>
            <Tooltip
              contentStyle={chartTooltipStyle}
              labelStyle={chartTooltipLabelStyle}
              itemStyle={chartTooltipItemStyle}
              formatter={(value) => [formatChartValue(Number(value), valueFormat), valueName]}
            />
            <Legend />
          </PieChart>
        </ResponsiveContainer>
      </div>
      <FilterLinkChips
        links={visible.slice(0, 8).map((row, index) => ({
          label: row.name,
          href: row.href,
          color: COLORS[index % COLORS.length],
        }))}
      />
    </div>
  );
}

function SimplePieMetric({ data }: { data: MetricRow[] }) {
  const visible = data.filter((row) => row.quantity > 0 || row.value > 0);
  if (!visible.length) return <EmptyChart />;
  const hasLinks = visible.some((row) => row.href);
  return (
    <ResponsiveContainer width="100%" height="100%" minWidth={0} minHeight={240}>
      <PieChart>
        <Pie
          data={visible}
          dataKey="quantity"
          nameKey="name"
          outerRadius={98}
          paddingAngle={1}
          cursor={hasLinks ? "pointer" : undefined}
          onClick={openMetricHref}
        >
          {visible.map((entry, index) => (
            <Cell key={entry.name} fill={COLORS[index % COLORS.length]} />
          ))}
          <LabelList dataKey="quantity" position="outside" fill="#ffffff" fontSize={11} fontWeight={900} formatter={(value: unknown) => formatShortChartValue(Number(value), "number")} />
        </Pie>
        <Tooltip
          contentStyle={chartTooltipStyle}
          labelStyle={chartTooltipLabelStyle}
          itemStyle={chartTooltipItemStyle}
          formatter={(value) => [formatNumber(Number(value)), "Quantidade"]}
        />
        <Legend />
      </PieChart>
    </ResponsiveContainer>
  );
}

function SemiDonutMetric({
  data,
  valueFormat = "number",
}: {
  data: MetricRow[];
  valueFormat?: ChartValueFormat;
}) {
  const visible = data.filter((row) => row.quantity > 0 || row.value > 0);
  if (!visible.length) return <EmptyChart />;
  const hasLinks = visible.some((row) => row.href);
  return (
    <ResponsiveContainer width="100%" height="100%" minWidth={0} minHeight={240}>
      <PieChart margin={{ top: 18, right: 8, left: 8, bottom: 8 }}>
        <Pie
          data={visible}
          dataKey="quantity"
          nameKey="name"
          startAngle={180}
          endAngle={0}
          cx="50%"
          cy="76%"
          innerRadius={72}
          outerRadius={112}
          paddingAngle={3}
          cursor={hasLinks ? "pointer" : undefined}
          onClick={openMetricHref}
        >
          {visible.map((entry, index) => (
            <Cell key={entry.name} fill={COLORS[index % COLORS.length]} />
          ))}
        </Pie>
        <Tooltip
          contentStyle={chartTooltipStyle}
          labelStyle={chartTooltipLabelStyle}
          itemStyle={chartTooltipItemStyle}
          formatter={(value) => [formatChartValue(Number(value), valueFormat), "Quantidade"]}
        />
        <Legend verticalAlign="bottom" />
      </PieChart>
    </ResponsiveContainer>
  );
}

function ParetoMetric({ data }: { data: MetricRow[] }) {
  const total = data.reduce((sum, row) => sum + row.quantity, 0);
  const rows = data
    .filter((row) => row.quantity > 0)
    .sort((left, right) => right.quantity - left.quantity)
    .slice(0, 10)
    .reduce<Array<MetricRow & { acumulado: number }>>((acc, row) => {
      const previous = acc[acc.length - 1]?.acumulado ?? 0;
      acc.push({ ...row, acumulado: previous + safeDivide(row.quantity, total) * 100 });
      return acc;
    }, []);
  if (!rows.length) return <EmptyChart />;
  const hasLinks = rows.some((row) => row.href);
  return (
    <ResponsiveContainer width="100%" height="100%" minWidth={0} minHeight={240}>
      <ComposedChart data={rows} margin={{ top: 30, right: 12, left: 8, bottom: 44 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.08)" vertical={false} />
        <XAxis dataKey="name" stroke="rgba(255,255,255,0.45)" tick={{ fontSize: 11 }} angle={-18} textAnchor="end" height={58} />
        <YAxis
          yAxisId="quantity"
          stroke="rgba(34,197,94,0.8)"
          tick={{ fontSize: 11 }}
          domain={[0, (dataMax: number) => Math.max(1, Math.ceil(dataMax * 1.18))]}
          allowDecimals={false}
        />
        <YAxis
          yAxisId="percent"
          orientation="right"
          domain={[0, 100]}
          stroke="rgba(56,189,248,0.75)"
          tick={{ fontSize: 11 }}
          tickFormatter={(value) => formatPercent(Number(value))}
        />
        <Tooltip
          contentStyle={chartTooltipStyle}
          labelStyle={chartTooltipLabelStyle}
          itemStyle={chartTooltipItemStyle}
          formatter={(value, name) =>
            String(name) === "Acumulado" ? formatPercent(Number(value)) : formatNumber(Number(value))
          }
        />
        <Legend />
        <Bar
          yAxisId="quantity"
          dataKey="quantity"
          name="Quantidade"
          fill="#22c55e"
          radius={[7, 7, 0, 0]}
          cursor={hasLinks ? "pointer" : undefined}
          onClick={openMetricHref}
        >
          <LabelList dataKey="quantity" position="top" fill="#ffffff" fontSize={11} fontWeight={900} formatter={(value: unknown) => formatShortChartValue(Number(value), "number")} />
        </Bar>
        <Line
          yAxisId="percent"
          type="monotone"
          dataKey="acumulado"
          name="Acumulado"
          stroke="#38bdf8"
          strokeWidth={3}
          dot={{ r: 4 }}
          cursor={hasLinks ? "pointer" : undefined}
          onClick={openMetricHref}
        />
      </ComposedChart>
    </ResponsiveContainer>
  );
}

function RadarMetric({ data }: { data: MetricRow[] }) {
  const rows = data.filter((row) => row.value > 0 || row.quantity > 0).slice(0, 6);
  if (!rows.length) return <EmptyChart />;
  return (
    <ResponsiveContainer width="100%" height="100%" minWidth={0} minHeight={240}>
      <RadarChart data={rows} outerRadius={102}>
        <PolarGrid stroke="rgba(255,255,255,0.16)" />
        <PolarAngleAxis dataKey="name" tick={{ fill: "rgba(255,255,255,0.65)", fontSize: 11 }} />
        <PolarRadiusAxis angle={90} domain={[0, 100]} tick={{ fill: "rgba(255,255,255,0.35)", fontSize: 10 }} />
        <Radar name="Indicador" dataKey="value" stroke="#38bdf8" fill="#38bdf8" fillOpacity={0.28} />
        <Tooltip
          contentStyle={chartTooltipStyle}
          labelStyle={chartTooltipLabelStyle}
          itemStyle={chartTooltipItemStyle}
          formatter={(value) => [formatPercent(Number(value)), "Percentual"]}
        />
      </RadarChart>
    </ResponsiveContainer>
  );
}

function ScanModeByHourChart({ data }: { data: TableRow[] }) {
  if (!data.length) return <EmptyChart />;
  const hasLinks = data.some((row) => asString(row.href).trim());
  return (
    <ResponsiveContainer width="100%" height="100%" minWidth={0} minHeight={240}>
      <ComposedChart data={data} margin={{ top: 8, right: 12, left: 8, bottom: 44 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.08)" vertical={false} />
        <XAxis dataKey="name" stroke="rgba(255,255,255,0.45)" tick={{ fontSize: 11 }} angle={-18} textAnchor="end" height={58} />
        <YAxis yAxisId="entries" stroke="rgba(34,197,94,0.8)" tick={{ fontSize: 11 }} />
        <YAxis
          yAxisId="manualRate"
          orientation="right"
          domain={[0, 100]}
          stroke="rgba(251,191,36,0.9)"
          tick={{ fontSize: 11 }}
          tickFormatter={(value) => formatPercent(Number(value))}
        />
        <Tooltip
          contentStyle={chartTooltipStyle}
          labelStyle={chartTooltipLabelStyle}
          itemStyle={chartTooltipItemStyle}
          formatter={(value, name) =>
            String(name) === "Manual %" ? formatPercent(Number(value)) : formatNumber(Number(value))
          }
        />
        <Legend />
        <Bar yAxisId="entries" dataKey="qr" name="QR" stackId="mode" fill="#22c55e" cursor={hasLinks ? "pointer" : undefined} onClick={openMetricHref} />
        <Bar yAxisId="entries" dataKey="manual" name="Manual" stackId="mode" fill="#facc15" cursor={hasLinks ? "pointer" : undefined} onClick={openMetricHref} />
        <Line yAxisId="manualRate" type="monotone" dataKey="manualRate" name="Manual %" stroke="#fb7185" strokeWidth={3} dot={{ r: 4 }} />
      </ComposedChart>
    </ResponsiveContainer>
  );
}

function ScoreGauge({
  score,
  label,
}: {
  score: number | null;
  label: string;
}) {
  if (score === null || !Number.isFinite(score)) return <EmptyChart />;
  const safeScore = clamp(score);
  const path = "M 56 166 A 104 104 0 0 1 264 166";
  return (
    <div className="flex h-full min-h-[280px] flex-col items-center justify-center gap-3">
      <svg viewBox="0 0 320 220" role="img" aria-label={`Score estratégico ${formatNumber(safeScore)} - ${label}`} className="h-[220px] w-full max-w-[430px] overflow-visible">
        <path d={path} fill="none" stroke="#27272a" strokeWidth="34" strokeLinecap="round" pathLength={100} />
        <path
          d={path}
          fill="none"
          stroke={scoreColor(safeScore)}
          strokeWidth="34"
          strokeLinecap="round"
          pathLength={100}
          strokeDasharray={`${safeScore} 100`}
        />
        <text x="160" y="126" textAnchor="middle" fill="#ffffff" fontSize="42" fontWeight="900">
          {formatNumber(safeScore)}
        </text>
        <text x="160" y="150" textAnchor="middle" fill="rgba(255,255,255,.58)" fontSize="12" fontWeight="900" letterSpacing="2.8">
          {label.toUpperCase()}
        </text>
        <text x="56" y="206" textAnchor="middle" fill="rgba(255,255,255,.45)" fontSize="10" fontWeight="800">
          0
        </text>
        <text x="264" y="206" textAnchor="middle" fill="rgba(255,255,255,.45)" fontSize="10" fontWeight="800">
          100
        </text>
      </svg>
      <div className="flex flex-wrap justify-center gap-2 text-[10px] font-black uppercase tracking-[0.12em] text-zinc-400">
        <span><span className="mr-1 inline-block h-2 w-2 rounded-full bg-[#fb7185]" />0-39 repensar</span>
        <span><span className="mr-1 inline-block h-2 w-2 rounded-full bg-[#facc15]" />40-69 ajustar</span>
        <span><span className="mr-1 inline-block h-2 w-2 rounded-full bg-[#38bdf8]" />70-84 repetir</span>
        <span><span className="mr-1 inline-block h-2 w-2 rounded-full bg-[#22c55e]" />85-100 escalar</span>
      </div>
    </div>
  );
}

function FunnelMetric({ data }: { data: MetricRow[] }) {
  const rows = data.filter((row) => row.quantity > 0);
  if (!rows.length) return <EmptyChart />;
  const max = maxValue(rows.map((row) => row.quantity));
  return (
    <div className="flex h-full flex-col justify-center gap-3 overflow-y-auto pr-1">
      {rows.map((row, index) => {
        const width = Math.max(8, safeDivide(row.quantity, max) * 100);
        const content = (
          <div className="rounded-lg border border-zinc-800 bg-black/30 p-2">
            <div className="flex items-center justify-between gap-3 text-xs font-black uppercase tracking-[0.12em] text-zinc-300">
              <span>{row.name}</span>
              <span>{formatNumber(row.quantity)}</span>
            </div>
            <div className="mt-2 h-6 overflow-hidden rounded bg-zinc-900">
              <div
                className="h-full rounded bg-emerald-400"
                style={{ width: `${width}%`, backgroundColor: COLORS[index % COLORS.length] }}
              />
            </div>
          </div>
        );
        return row.href ? (
          <Link key={row.name} href={row.href} prefetch={false} className="block outline-none transition hover:opacity-90">
            {content}
          </Link>
        ) : (
          <div key={row.name}>{content}</div>
        );
      })}
    </div>
  );
}

function ComboBarsLines({
  data,
  barName = "Quantidade",
  lineOneName = "Receita",
  lineTwoName = "Secundário",
  valueFormat = "currency",
  secondaryFormat = "currency",
  sortBy = "quantity",
}: {
  data: MetricRow[];
  barName?: string;
  lineOneName?: string;
  lineTwoName?: string;
  valueFormat?: ChartValueFormat;
  secondaryFormat?: ChartValueFormat;
  sortBy?: "quantity" | "value" | "none";
}) {
  if (!data.length) return <EmptyChart />;
  const rows = [...data].sort((left, right) => {
    if (sortBy === "none") return Number(left.sortValue ?? 0) - Number(right.sortValue ?? 0);
    if (sortBy === "value") return right.value - left.value || right.quantity - left.quantity;
    return right.quantity - left.quantity || right.value - left.value;
  });
  const hasSecondary = rows.some((row) => Number(row.secondary ?? 0) > 0);
  const hasLinks = rows.some((row) => row.href);
  const rightAxisLabel = hasSecondary ? `${lineOneName} e ${lineTwoName} (eixo direito)` : `${lineOneName} (eixo direito)`;
  return (
    <ResponsiveContainer width="100%" height="100%" minWidth={0} minHeight={240}>
      <ComposedChart data={rows} margin={{ top: 26, right: 20, left: 16, bottom: 48 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.08)" vertical={false} />
        <XAxis dataKey="name" stroke="rgba(255,255,255,0.45)" tick={{ fontSize: 11 }} angle={-18} textAnchor="end" height={58} />
        <YAxis
          yAxisId="qty"
          stroke="rgba(34,197,94,0.8)"
          tick={{ fontSize: 11 }}
          domain={[0, (dataMax: number) => Math.max(1, Math.ceil(dataMax * 1.18))]}
          allowDecimals={false}
          label={{ value: `${barName} (eixo esquerdo)`, angle: -90, position: "insideLeft", fill: "rgba(34,197,94,0.9)", fontSize: 10, fontWeight: 900 }}
        />
        <YAxis
          yAxisId="value"
          orientation="right"
          stroke="rgba(56,189,248,0.75)"
          tick={{ fontSize: 11 }}
          tickFormatter={(value) => formatChartValue(Number(value), valueFormat)}
          label={{ value: rightAxisLabel, angle: 90, position: "insideRight", fill: "rgba(56,189,248,0.9)", fontSize: 10, fontWeight: 900 }}
        />
        <Tooltip
          contentStyle={chartTooltipStyle}
          labelStyle={chartTooltipLabelStyle}
          itemStyle={chartTooltipItemStyle}
          formatter={(value, name) => {
            if (String(name) === barName) return formatNumber(Number(value));
            if (String(name) === lineTwoName) return formatChartValue(Number(value), secondaryFormat);
            return formatChartValue(Number(value), valueFormat);
          }}
        />
        <Legend formatter={(value) => {
          if (String(value) === barName) return `${barName} · eixo esquerdo`;
          return `${String(value)} · eixo direito`;
        }} />
        <Bar yAxisId="qty" dataKey="quantity" name={barName} fill="#22c55e" radius={[7, 7, 0, 0]} cursor={hasLinks ? "pointer" : undefined} onClick={openMetricHref}>
          <LabelList dataKey="quantity" position="top" fill="#ffffff" fontSize={11} fontWeight={900} formatter={(value: unknown) => formatShortChartValue(Number(value), "number")} />
        </Bar>
        <Line yAxisId="value" type="monotone" dataKey="value" name={lineOneName} stroke="#38bdf8" strokeWidth={3} dot={{ r: 4 }} cursor={hasLinks ? "pointer" : undefined} onClick={openMetricHref} />
        {hasSecondary ? (
          <Line yAxisId="value" type="monotone" dataKey="secondary" name={lineTwoName} stroke="#facc15" strokeWidth={3} dot={{ r: 4 }} cursor={hasLinks ? "pointer" : undefined} onClick={openMetricHref} />
        ) : null}
      </ComposedChart>
    </ResponsiveContainer>
  );
}

function StackedPercentChart({ data }: { data: TableRow[] }) {
  if (!data.length) return <EmptyChart />;
  const keys = [
    { key: "retirado", label: "Retirado", color: "#22c55e" },
    { key: "pendente", label: "Pendente", color: "#facc15" },
    { key: "parcial", label: "Parcial", color: "#38bdf8" },
    { key: "cancelado", label: "Cancelado", color: "#fb7185" },
  ];
  const rows = data.map((row) => {
    const total = keys.reduce((sum, entry) => sum + parseNumber(row[entry.key], 0), 0);
    return {
      ...row,
      ...Object.fromEntries(keys.map((entry) => [entry.key, safeDivide(parseNumber(row[entry.key], 0), total) * 100])),
    };
  });
  return (
    <ResponsiveContainer width="100%" height="100%" minWidth={0} minHeight={240}>
      <BarChart data={rows} layout="vertical" margin={{ top: 8, right: 18, left: 22, bottom: 8 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.08)" horizontal={false} />
        <XAxis type="number" domain={[0, 100]} tickFormatter={(value) => formatPercent(Number(value))} stroke="rgba(255,255,255,0.45)" tick={{ fontSize: 11 }} />
        <YAxis dataKey="name" type="category" width={118} stroke="rgba(255,255,255,0.45)" tick={{ fontSize: 11 }} />
        <Tooltip
          contentStyle={chartTooltipStyle}
          labelStyle={chartTooltipLabelStyle}
          itemStyle={chartTooltipItemStyle}
          formatter={(value, name) => [formatPercent(Number(value)), String(name)]}
        />
        <Legend />
        {keys.map((entry) => (
          <Bar key={entry.key} dataKey={entry.key} stackId="status" name={entry.label} fill={entry.color} />
        ))}
      </BarChart>
    </ResponsiveContainer>
  );
}

function HeatmapMetric({
  data,
  maxColumns = 8,
  columnOrder = "none",
}: {
  data: HeatmapEntry[];
  maxColumns?: number;
  columnOrder?: "none" | "hour" | "alpha";
}) {
  const rows = Array.from(new Set(data.map((entry) => entry.row))).slice(0, 10);
  const columns = Array.from(new Set(data.map((entry) => entry.column)))
    .sort((left, right) => {
      if (columnOrder === "hour") return hourSortValue(left) - hourSortValue(right) || left.localeCompare(right, "pt-BR");
      if (columnOrder === "alpha") return left.localeCompare(right, "pt-BR");
      return 0;
    })
    .slice(0, maxColumns);
  if (!rows.length || !columns.length) return <EmptyChart />;
  const max = maxValue(data.map((entry) => entry.value));
  const byKey = new Map(data.map((entry) => [`${entry.row}:${entry.column}`, entry]));
  return (
    <div className="h-full overflow-auto">
      <div className="grid min-w-[680px] gap-1" style={{ gridTemplateColumns: `150px repeat(${columns.length}, minmax(74px, 1fr))` }}>
        <div />
        {columns.map((column) => (
          <div key={column} className="truncate px-2 text-center text-[10px] font-black uppercase tracking-[0.12em] text-zinc-500">
            {column}
          </div>
        ))}
        {rows.map((row) => (
          <div key={row} className="contents">
            <div key={`${row}-label`} className="truncate px-2 py-2 text-xs font-black text-zinc-300">
              {row}
            </div>
            {columns.map((column) => {
              const entry = byKey.get(`${row}:${column}`);
              const intensity = clamp(safeDivide(entry?.value ?? 0, max) * 100);
              const style = {
                backgroundColor: `rgba(34, 197, 94, ${0.12 + intensity / 115})`,
                borderColor: `rgba(34, 197, 94, ${0.18 + intensity / 130})`,
              };
              const content = (
                <div className="rounded border px-2 py-2 text-center text-[11px] font-black text-white" style={style}>
                  {entry?.value ? formatNumber(entry.value) : "-"}
                </div>
              );
              return entry?.href ? (
                <Link key={`${row}-${column}`} href={entry.href} prefetch={false}>
                  {content}
                </Link>
              ) : (
                <div key={`${row}-${column}`}>{content}</div>
              );
            })}
          </div>
        ))}
      </div>
      <div className="mt-2 flex items-center justify-end gap-2 text-[10px] font-black uppercase tracking-[0.12em] text-zinc-500">
        <span>Menor</span>
        <span className="h-2 w-20 rounded-full bg-gradient-to-r from-emerald-500/15 to-emerald-400" />
        <span>Maior valor</span>
      </div>
    </div>
  );
}

function TreemapMetric({ data }: { data: MetricRow[] }) {
  const rows = data.filter((row) => row.value > 0 || row.quantity > 0).slice(0, 12);
  if (!rows.length) return <EmptyChart />;
  const total = rows.reduce((sum, row) => sum + Math.max(row.value, row.quantity), 0);
  return (
    <div className="flex h-full flex-wrap content-stretch gap-2">
      {rows.map((row, index) => {
        const size = Math.max(18, safeDivide(Math.max(row.value, row.quantity), total) * 100);
        const block = (
          <div
            className="flex h-full min-h-[76px] flex-col justify-between rounded-lg border border-white/10 p-3 text-white"
            style={{ backgroundColor: COLORS[index % COLORS.length], flexBasis: `${size}%`, flexGrow: size }}
          >
            <span className="line-clamp-2 text-xs font-black uppercase tracking-[0.12em]">{row.name}</span>
            <span className="text-lg font-black">{formatCurrency(row.value || row.quantity)}</span>
            {typeof row.secondary === "number" ? <span className="text-[10px] font-bold opacity-80">{formatPercent(row.secondary)}</span> : null}
          </div>
        );
        return row.href ? (
          <Link key={row.name} href={row.href} prefetch={false} className="flex min-w-[130px]">
            {block}
          </Link>
        ) : (
          <div key={row.name} className="flex min-w-[130px]">
            {block}
          </div>
        );
      })}
    </div>
  );
}

function BubbleTooltip({
  active,
  payload,
  xLabel,
  yLabel,
  xFormat,
  yFormat,
}: {
  active?: boolean;
  payload?: Array<{ payload?: BubbleEntry }>;
  xLabel: string;
  yLabel: string;
  xFormat: ChartValueFormat;
  yFormat: ChartValueFormat;
}) {
  if (!active) return null;
  const row = payload?.find((entry) => entry.payload)?.payload;
  if (!row) return null;
  return (
    <div className="rounded-lg border border-zinc-700 bg-black/95 p-3 text-xs text-white shadow-xl">
      <p className="font-black uppercase tracking-[0.14em]">{row.name}</p>
      <div className="mt-2 space-y-1 font-bold text-zinc-300">
        <p>{xLabel}: <span className="text-white">{formatChartValue(row.x, xFormat)}</span></p>
        <p>{yLabel}: <span className="text-white">{formatChartValue(row.y, yFormat)}</span></p>
        <p>Receita/tamanho da bolha: <span className="text-white">{formatCurrency(row.z)}</span></p>
        <p>Score: <span className="text-white">{formatNumber(row.value)}</span></p>
        {row.decision ? <p>Decisão: <span className="text-white">{row.decision}</span></p> : null}
      </div>
    </div>
  );
}

function BubbleMetric({
  data,
  xLabel = "Presença",
  yLabel = "Receita",
  xFormat = "percent",
  yFormat = "currency",
}: {
  data: BubbleEntry[];
  xLabel?: string;
  yLabel?: string;
  xFormat?: ChartValueFormat;
  yFormat?: ChartValueFormat;
}) {
  const rows = data.filter((row) => row.x > 0 || row.y > 0 || row.value > 0);
  if (!rows.length) return <EmptyChart />;
  const filters = rows.filter((row) => row.href).slice(0, 8);
  const scoreLegend = [
    { label: "0-39 repensar", color: "#fb7185" },
    { label: "40-69 ajustar", color: "#facc15" },
    { label: "70-84 repetir", color: "#38bdf8" },
    { label: "85-100 escalar", color: "#22c55e" },
  ];
  const bubbleChips = rows.slice(0, 8);
  return (
    <div className="flex h-full min-h-0 flex-col">
      <div className="min-h-0 flex-1">
        <ResponsiveContainer width="100%" height="100%" minWidth={0} minHeight={240}>
          <ScatterChart margin={{ top: 26, right: 18, left: 8, bottom: 34 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.08)" />
        <XAxis
          type="number"
          dataKey="x"
          name={xLabel}
          domain={xFormat === "percent" ? [0, 100] : ["auto", "auto"]}
          stroke="rgba(255,255,255,0.45)"
          tick={{ fontSize: 11 }}
          tickFormatter={(value) => formatChartValue(Number(value), xFormat)}
        />
        <YAxis
          type="number"
          dataKey="y"
          name={yLabel}
          stroke="rgba(255,255,255,0.45)"
          tick={{ fontSize: 11 }}
          tickFormatter={(value) => formatChartValue(Number(value), yFormat)}
        />
        <ZAxis dataKey="z" range={[80, 900]} />
        {xFormat === "percent" ? (
          <>
            <ReferenceLine x={50} stroke="rgba(255,255,255,0.22)" strokeDasharray="4 4" />
            <ReferenceLine
              x={100}
              stroke="rgba(255,255,255,0.38)"
              strokeDasharray="4 4"
              label={{ value: "100%", fill: "#ffffff", fontSize: 11, position: "insideTopRight" }}
            />
          </>
        ) : null}
        <ReferenceLine y={0} stroke="rgba(255,255,255,0.22)" strokeDasharray="4 4" />
            <Tooltip
              cursor={{ strokeDasharray: "3 3" }}
              content={<BubbleTooltip xLabel={xLabel} yLabel={yLabel} xFormat={xFormat} yFormat={yFormat} />}
            />
            <Scatter data={rows} name="Eventos">
              {rows.map((entry) => (
                <Cell key={entry.name} fill={scoreColor(entry.value)} />
              ))}
              <LabelList dataKey="value" position="top" fill="#ffffff" fontSize={11} fontWeight={900} formatter={(value: unknown) => formatNumber(Number(value))} />
            </Scatter>
          </ScatterChart>
        </ResponsiveContainer>
      </div>
      <div className="mt-2 flex flex-wrap items-center gap-2 text-[10px] font-black uppercase tracking-[0.12em] text-zinc-400">
        {scoreLegend.map((item) => (
          <span key={item.label} className="inline-flex items-center gap-1">
            <span className="h-2.5 w-2.5 rounded-full" style={{ backgroundColor: item.color }} />
            {item.label}
          </span>
        ))}
        {xFormat === "percent" ? <span className="text-zinc-500">100% = presença completa</span> : null}
      </div>
      {filters.length ? (
        <div className="mt-2 flex flex-wrap gap-2 text-[10px] font-black uppercase tracking-[0.12em]">
          <span className="text-zinc-500">Abrir filtro:</span>
          {filters.map((row) => (
            <Link
              key={`${row.name}-${row.href}`}
              href={row.href || "#"}
              prefetch={false}
              className="rounded border border-zinc-700 px-2 py-1 text-zinc-300 transition hover:border-emerald-400 hover:text-white"
              title={row.decision || scoreBandLabel(row.value)}
            >
              {row.name}
            </Link>
          ))}
        </div>
      ) : (
        <div className="mt-2 flex flex-wrap gap-2 text-[10px] font-black uppercase tracking-[0.12em]">
          <span className="text-zinc-500">Bolinhas:</span>
          {bubbleChips.map((row) => (
            <span key={row.name} className="inline-flex items-center gap-1 rounded border border-zinc-800 px-2 py-1 text-zinc-300">
              <span className="h-2.5 w-2.5 rounded-full" style={{ backgroundColor: scoreColor(row.value) }} />
              {row.name}
            </span>
          ))}
        </div>
      )}
    </div>
  );
}

function WaterfallMetric({ data }: { data: MetricRow[] }) {
  const rows = data.filter((row) => row.quantity !== 0 || row.value !== 0);
  if (!rows.length) return <EmptyChart />;
  return (
    <ResponsiveContainer width="100%" height="100%" minWidth={0} minHeight={240}>
      <BarChart data={rows} margin={{ top: 8, right: 12, left: 8, bottom: 44 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.08)" vertical={false} />
        <XAxis dataKey="name" stroke="rgba(255,255,255,0.45)" tick={{ fontSize: 11 }} angle={-18} textAnchor="end" height={58} />
        <YAxis stroke="rgba(255,255,255,0.45)" tick={{ fontSize: 11 }} tickFormatter={(value) => formatCurrency(Number(value))} />
        <Tooltip
          contentStyle={chartTooltipStyle}
          labelStyle={chartTooltipLabelStyle}
          itemStyle={chartTooltipItemStyle}
          formatter={(value) => [formatCurrency(Number(value)), "Valor"]}
        />
        <Bar dataKey="value" name="Valor" radius={[7, 7, 0, 0]} onClick={openMetricHref}>
          {rows.map((entry) => (
            <Cell key={entry.name} fill={entry.value >= 0 ? "#22c55e" : "#fb7185"} />
          ))}
          <LabelList dataKey="value" position="top" fill="#ffffff" fontSize={11} fontWeight={900} formatter={(value: unknown) => formatShortChartValue(Number(value), "currency")} />
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}

function NetworkMetric({ data }: { data: NetworkEdge[] }) {
  const edges = data.filter((edge) => edge.value > 0).slice(0, 12);
  if (!edges.length) return <EmptyChart />;
  const nodes = Array.from(new Set(edges.flatMap((edge) => [edge.from, edge.to]))).slice(0, 10);
  const radius = 112;
  const centerX = 300;
  const centerY = 150;
  const positions = new Map(
    nodes.map((node, index) => {
      const angle = (Math.PI * 2 * index) / Math.max(nodes.length, 1) - Math.PI / 2;
      return [node, { x: centerX + Math.cos(angle) * radius, y: centerY + Math.sin(angle) * radius }];
    })
  );
  const max = maxValue(edges.map((edge) => edge.value));
  return (
    <svg viewBox="0 0 600 300" className="h-full w-full">
      {edges.map((edge, index) => {
        const from = positions.get(edge.from);
        const to = positions.get(edge.to);
        if (!from || !to) return null;
        return (
          <line
            key={`${edge.from}-${edge.to}-${index}`}
            x1={from.x}
            y1={from.y}
            x2={to.x}
            y2={to.y}
            stroke={COLORS[index % COLORS.length]}
            strokeOpacity={0.38}
            strokeWidth={1 + safeDivide(edge.value, max) * 8}
          />
        );
      })}
      {nodes.map((node, index) => {
        const position = positions.get(node);
        if (!position) return null;
        return (
          <g key={node}>
            <circle cx={position.x} cy={position.y} r={26} fill={COLORS[index % COLORS.length]} opacity={0.92} />
            <text x={position.x} y={position.y + 4} textAnchor="middle" fill="#fff" fontSize={9} fontWeight={900}>
              {node.slice(0, 18)}
            </text>
          </g>
        );
      })}
    </svg>
  );
}

function DataTable({
  title,
  rows,
  columns,
  pageSize,
}: {
  title: string;
  rows: TableRow[];
  columns: Array<{
    key: string;
    label: string;
    format?: "currency" | "percent" | "decimal" | "number";
    hrefKey?: string;
  }>;
  pageSize?: number;
}) {
  const [page, setPage] = useState(1);
  const totalPages = pageSize ? Math.max(1, Math.ceil(rows.length / pageSize)) : 1;
  const safePage = Math.min(totalPages, Math.max(1, page));
  const visibleRows = pageSize ? rows.slice((safePage - 1) * pageSize, safePage * pageSize) : rows;

  useEffect(() => {
    setPage((current) => Math.min(totalPages, Math.max(1, current)));
  }, [totalPages]);

  return (
    <section className="overflow-hidden rounded-lg border border-zinc-800 bg-zinc-950/70">
      <div className="border-b border-zinc-800 px-4 py-3">
        <h2 className="text-xs font-black uppercase tracking-[0.2em] text-zinc-300">{title}</h2>
      </div>
      <div className="overflow-x-auto">
        <table className="min-w-full text-left text-sm">
          <thead className="bg-black/30 text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">
            <tr>
              {columns.map((column) => (
                <th key={column.key} className="px-4 py-3">
                  {column.label}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-800">
            {visibleRows.length ? (
              visibleRows.map((row, index) => (
                <tr key={`${row[columns[0].key]}-${index}`} className="text-zinc-300">
                  {columns.map((column, columnIndex) => {
                    const raw = row[column.key];
                    const numeric = typeof raw === "number" ? raw : parseNumber(raw, 0);
                    const value =
                      column.format === "currency"
                        ? formatCurrency(numeric)
                        : column.format === "percent"
                          ? formatPercent(numeric)
                          : column.format === "decimal"
                            ? formatDecimal(numeric)
                            : column.format === "number"
                              ? formatNumber(numeric)
                              : raw;
                    const href = asString(column.hrefKey ? row[column.hrefKey] : columnIndex === 0 ? row.href : "").trim();
                    return (
                      <td key={column.key} className="px-4 py-3 font-semibold">
                        {href ? (
                          <Link href={href} prefetch={false} className="text-emerald-300 underline-offset-4 hover:underline">
                            {value}
                          </Link>
                        ) : (
                          value
                        )}
                      </td>
                    );
                  })}
                </tr>
              ))
            ) : (
              <tr>
                <td className="px-4 py-6 text-center text-sm font-bold text-zinc-500" colSpan={columns.length}>
                  Sem dados para o filtro atual.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
      {pageSize && rows.length > pageSize ? (
        <div className="flex flex-col gap-3 border-t border-zinc-800 px-4 py-3 text-xs font-bold text-zinc-400 sm:flex-row sm:items-center sm:justify-between">
          <span>
            Página {safePage} de {totalPages} · {formatNumber(rows.length)} registros
          </span>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => setPage((current) => Math.max(1, current - 1))}
              disabled={safePage <= 1}
              className="rounded-lg border border-zinc-700 px-3 py-2 text-[10px] font-black uppercase text-zinc-300 transition hover:border-zinc-500 disabled:opacity-40"
            >
              Anterior
            </button>
            <button
              type="button"
              onClick={() => setPage((current) => Math.min(totalPages, current + 1))}
              disabled={safePage >= totalPages}
              className="rounded-lg border border-zinc-700 px-3 py-2 text-[10px] font-black uppercase text-zinc-300 transition hover:border-zinc-500 disabled:opacity-40"
            >
              Próxima
            </button>
          </div>
        </div>
      ) : null}
    </section>
  );
}

function DashboardShell({
  view,
  children,
  basePath,
  scopeLabel,
  backHref,
  contextTitle,
  contextLogo,
  contextEyebrow,
}: {
  view: AdminEventBiView;
  children: ReactNode;
  basePath?: string;
  scopeLabel?: string;
  backHref?: string;
  contextTitle?: string;
  contextLogo?: string;
  contextEyebrow?: string;
}) {
  const { tenantSlug } = useTenantTheme();
  const resolvedBasePath = basePath?.trim() || "/admin/bi";
  const homeHref = tenantSlug ? withTenantSlug(tenantSlug, resolvedBasePath) : resolvedBasePath;
  const resolvedBackHref = backHref?.trim()
    ? tenantSlug
      ? withTenantSlug(tenantSlug, backHref.trim())
      : backHref.trim()
    : "";
  const title = view === "inicio" ? "BI de Eventos" : MODULES.find((module) => module.id === view)?.title || "BI de Eventos";
  const subtitle =
    view === "inicio"
      ? `Escolha a visão analítica${scopeLabel ? ` ${scopeLabel}` : ""}.`
      : MODULES.find((module) => module.id === view)?.subtitle || "Indicadores do evento.";
  const titleLabel = contextTitle?.trim() || title;
  const subtitleLabel = contextTitle?.trim() ? `${title}. ${subtitle}` : subtitle;
  const logoSrc = contextLogo?.trim();
  const shellBackHref = view !== "inicio" ? homeHref : resolvedBackHref;

  return (
    <main className="min-h-screen bg-[#050505] px-4 py-5 text-white md:px-6">
      <div className="mx-auto max-w-7xl space-y-5">
        <header className="rounded-lg border border-zinc-800 bg-zinc-950/70 p-4">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            <div className="flex items-center gap-3">
              {shellBackHref ? (
                <Link href={shellBackHref} className="rounded-lg border border-zinc-800 bg-black p-2 text-zinc-300 hover:text-white" title="Voltar">
                  <ArrowLeft size={18} />
                </Link>
              ) : null}
              {logoSrc ? (
                <div className="relative h-11 w-11 shrink-0 overflow-hidden rounded-xl border border-zinc-700 bg-black">
                  <Image src={logoSrc} alt={titleLabel} fill sizes="44px" className="object-cover" />
                </div>
              ) : null}
              <div>
                <p className="text-[10px] font-black uppercase tracking-[0.28em] text-emerald-300">
                  {contextEyebrow?.trim() || "BI Administrativo"}
                </p>
                <h1 className="mt-1 text-2xl font-black uppercase text-white">{titleLabel}</h1>
                <p className="mt-1 text-sm font-bold text-zinc-500">{subtitleLabel}</p>
              </div>
            </div>
            {view !== "inicio" ? (
              <nav className="flex flex-wrap gap-2">
                {MODULES.map((module) => {
                  const modulePath = `${resolvedBasePath.replace(/\/+$/, "")}/${module.id}`;
                  const href = tenantSlug ? withTenantSlug(tenantSlug, modulePath) : modulePath;
                  return (
                    <Link
                      key={module.id}
                      href={href}
                      className={`rounded-lg border px-3 py-2 text-xs font-black uppercase ${
                        view === module.id
                          ? "border-emerald-400 bg-emerald-400 text-black"
                          : "border-zinc-800 bg-black text-zinc-300 hover:border-zinc-600"
                      }`}
                    >
                      {module.title}
                    </Link>
                  );
                })}
              </nav>
            ) : null}
          </div>
        </header>
        {children}
      </div>
    </main>
  );
}

function HubContent({ basePath }: { basePath?: string }) {
  const { tenantSlug } = useTenantTheme();
  const resolvedBasePath = basePath?.trim() || "/admin/bi";
  return (
    <section className="grid gap-3 md:grid-cols-2 xl:grid-cols-5">
      {MODULES.map((module) => {
        const modulePath = `${resolvedBasePath.replace(/\/+$/, "")}/${module.id}`;
        const href = tenantSlug ? withTenantSlug(tenantSlug, modulePath) : modulePath;
        return (
          <Link
            key={module.id}
            href={href}
            className="group flex min-h-44 flex-col justify-between rounded-lg border border-zinc-800 bg-zinc-950 p-5 transition hover:border-emerald-400/50 hover:bg-zinc-900"
          >
            <span className="inline-flex h-11 w-11 items-center justify-center rounded-lg border border-emerald-400/20 bg-emerald-400/10 text-emerald-300">
              {module.icon}
            </span>
            <span>
              <strong className="block text-lg font-black uppercase text-white">{module.title}</strong>
              <span className="mt-2 block text-sm font-semibold leading-5 text-zinc-500">{module.subtitle}</span>
            </span>
          </Link>
        );
      })}
    </section>
  );
}

function Filters({
  scopeType,
  setScopeType,
  scopeId,
  setScopeId,
  scopeOptions,
  eventFilter,
  setEventFilter,
  productFilter,
  setProductFilter,
  startDate,
  setStartDate,
  endDate,
  setEndDate,
  eventOptions,
  productOptions,
  showProduct,
  scopeLocked,
  scopeLabel,
}: {
  scopeType: ScopeType;
  setScopeType: (value: ScopeType) => void;
  scopeId: string;
  setScopeId: (value: string) => void;
  scopeOptions: Record<Exclude<ScopeType, "tenant">, ScopeOption[]>;
  eventFilter: string;
  setEventFilter: (value: string) => void;
  productFilter: string;
  setProductFilter: (value: string) => void;
  startDate: string;
  setStartDate: (value: string) => void;
  endDate: string;
  setEndDate: (value: string) => void;
  eventOptions: ScopeOption[];
  productOptions: ScopeOption[];
  showProduct: boolean;
  scopeLocked: boolean;
  scopeLabel?: string;
}) {
  const entityOptions = scopeType === "tenant" ? [] : scopeOptions[scopeType];
  return (
    <section className="rounded-lg border border-zinc-800 bg-black/40 p-3">
      <div className="grid gap-2 md:grid-cols-2 xl:grid-cols-6">
        {scopeLocked ? (
          <div className="flex min-h-11 items-center rounded-lg border border-zinc-800 bg-zinc-950 px-3 text-sm font-black text-zinc-300">
            {scopeLabel || (scopeType === "tenant" ? "Atlética" : "Entidade")}
          </div>
        ) : (
          <>
            <select
              value={scopeType}
              onChange={(event) => {
                setScopeType(event.target.value as ScopeType);
                setScopeId("todos");
              }}
              className="min-h-11 rounded-lg border border-zinc-700 bg-zinc-950 px-3 text-sm font-bold text-white outline-none focus:border-emerald-400"
            >
              <option value="tenant">Atlética</option>
              <option value="league">Liga</option>
              <option value="directory">Diretório</option>
              <option value="commission">Comissão</option>
            </select>
            <select
              value={scopeId}
              onChange={(event) => setScopeId(event.target.value)}
              disabled={scopeType === "tenant"}
              className="min-h-11 rounded-lg border border-zinc-700 bg-zinc-950 px-3 text-sm font-bold text-white outline-none focus:border-emerald-400 disabled:opacity-60"
            >
              <option value="todos">{scopeType === "tenant" ? "Toda a atlética" : "Todas as entidades"}</option>
              {entityOptions.map((option) => (
                <option key={option.id} value={option.id}>
                  {option.name}
                </option>
              ))}
            </select>
          </>
        )}
        <select
          value={eventFilter}
          onChange={(event) => setEventFilter(event.target.value)}
          className="min-h-11 rounded-lg border border-zinc-700 bg-zinc-950 px-3 text-sm font-bold text-white outline-none focus:border-emerald-400"
        >
          <option value="todos">Todos os eventos</option>
          {eventOptions.map((option) => (
            <option key={option.id} value={option.id}>
              {option.name}
            </option>
          ))}
        </select>
        {showProduct ? (
          <select
            value={productFilter}
            onChange={(event) => setProductFilter(event.target.value)}
            className="min-h-11 rounded-lg border border-zinc-700 bg-zinc-950 px-3 text-sm font-bold text-white outline-none focus:border-emerald-400"
          >
            <option value="todos">Todos os produtos</option>
            {productOptions.map((option) => (
              <option key={option.id} value={option.id}>
                {option.name}
              </option>
            ))}
          </select>
        ) : null}
        <input
          type="date"
          value={startDate}
          onChange={(event) => setStartDate(event.target.value)}
          className="min-h-11 rounded-lg border border-zinc-700 bg-zinc-950 px-3 text-sm font-bold text-white outline-none focus:border-emerald-400"
        />
        <input
          type="date"
          value={endDate}
          onChange={(event) => setEndDate(event.target.value)}
          className="min-h-11 rounded-lg border border-zinc-700 bg-zinc-950 px-3 text-sm font-bold text-white outline-none focus:border-emerald-400"
        />
      </div>
    </section>
  );
}

export default function AdminEventBiDashboard({
  view,
  initialEventId = "todos",
  basePath,
  eventWorkspaceBasePath,
  lockedScopeType = "tenant",
  lockedScopeId = "todos",
  scopeLabel,
  backHref,
  contextTitle,
  contextLogo,
  contextEyebrow,
}: {
  view: AdminEventBiView;
  initialEventId?: string;
  basePath?: string;
  eventWorkspaceBasePath?: string;
  lockedScopeType?: ScopeType;
  lockedScopeId?: string;
  scopeLabel?: string;
  backHref?: string;
  contextTitle?: string;
  contextLogo?: string;
  contextEyebrow?: string;
}) {
  const router = useRouter();
  const { tenantId, tenantSlug } = useTenantTheme();
  const [data, setData] = useState<BiData>(emptyData);
  const [loading, setLoading] = useState(view !== "inicio");
  const [error, setError] = useState("");
  const [scopeType, setScopeType] = useState<ScopeType>(lockedScopeType);
  const [scopeId, setScopeId] = useState(lockedScopeId || "todos");
  const [eventFilter, setEventFilter] = useState(initialEventId || "todos");
  const [productFilter, setProductFilter] = useState("todos");
  const [audienceBasis, setAudienceBasis] = useState<AudienceBasis>("aprovados");
  const [categoryRevenueMode, setCategoryRevenueMode] = useState<"value" | "quantity">("value");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const normalizedEventWorkspaceBasePath = eventWorkspaceBasePath?.trim().replace(/\/+$/, "") || "/admin/eventos";
  const buildStatementHref = useCallback(
    (targetEventId: string, options: StatementLinkOptions = {}) => {
      const cleanEventId = targetEventId.trim();
      if (!cleanEventId) return "";
      const params = new URLSearchParams();
      if (options.type && options.type !== "todos") params.set("tipo", options.type);
      if (options.status && options.status !== "todos") params.set("status", options.status);
      if (options.search?.trim()) params.set("busca", options.search.trim());
      if (options.alert?.trim()) params.set("alerta", options.alert.trim());
      if (options.source?.trim()) params.set("origem", options.source.trim());
      if (options.approver?.trim()) params.set("aprovador", options.approver.trim());
      if (options.flow?.trim()) params.set("fluxo", options.flow.trim());
      if (options.indicator?.trim()) params.set("indicador", options.indicator.trim());
      const path = `${normalizedEventWorkspaceBasePath}/${encodeURIComponent(cleanEventId)}/extrato${
        params.toString() ? `?${params.toString()}` : ""
      }`;
      return tenantSlug ? withTenantSlug(tenantSlug, path) : path;
    },
    [normalizedEventWorkspaceBasePath, tenantSlug]
  );
  const buildCheckinsHref = useCallback(
    (targetEventId: string, options: CheckinsLinkOptions = {}) => {
      const cleanEventId = targetEventId.trim();
      if (!cleanEventId) return "";
      const params = new URLSearchParams();
      if (options.search?.trim()) params.set("busca", options.search.trim());
      if (options.indicator?.trim()) params.set("indicador", options.indicator.trim());
      const path = `${normalizedEventWorkspaceBasePath}/${encodeURIComponent(cleanEventId)}/checkins${
        params.toString() ? `?${params.toString()}` : ""
      }`;
      return tenantSlug ? withTenantSlug(tenantSlug, path) : path;
    },
    [normalizedEventWorkspaceBasePath, tenantSlug]
  );

  useEffect(() => {
    setEventFilter(initialEventId || "todos");
  }, [initialEventId]);

  useEffect(() => {
    setScopeType(lockedScopeType);
    setScopeId(lockedScopeId || "todos");
  }, [lockedScopeId, lockedScopeType]);

  useEffect(() => {
    if (view === "inicio") return;
    let mounted = true;
    setLoading(true);
    setError("");
    void loadBiData(tenantId.trim())
      .then((nextData) => {
        if (mounted) setData(nextData);
      })
      .catch((loadError: unknown) => {
        console.error(loadError);
        if (mounted) setError(loadError instanceof Error ? loadError.message : "Erro ao carregar BI.");
      })
      .finally(() => {
        if (mounted) setLoading(false);
      });
    return () => {
      mounted = false;
    };
  }, [tenantId, view]);

  const eventById = useMemo(() => new Map(data.events.map((event) => [eventId(event), event])), [data.events]);
  const productsById = useMemo(() => new Map(data.products.map((product) => [asString(product.id), product])), [data.products]);
  const userById = useMemo(() => new Map(data.users.map((user) => [asString(user.uid), user])), [data.users]);
  const forcedEntityScope = useMemo(
    () => buildForcedEntityScope(lockedScopeType, lockedScopeId || "todos"),
    [lockedScopeId, lockedScopeType]
  );
  const entityScopeIndex = useMemo(
    () => buildEntityScopeIndex(data.entities, forcedEntityScope),
    [data.entities, forcedEntityScope]
  );
  const entityMemberIndex = useMemo(
    () => buildEntityMemberIndex(data.entities, forcedEntityScope),
    [data.entities, forcedEntityScope]
  );
  const eventScopeIndex = useMemo(
    () => buildEventScopeIndex(data.entities, forcedEntityScope),
    [data.entities, forcedEntityScope]
  );
  const scopeOptions = useMemo(
    () => buildScopeOptions(data, entityScopeIndex, eventScopeIndex),
    [data, entityScopeIndex, eventScopeIndex]
  );

  const matchesActiveScope = useMemo(
    () =>
      (row: Row, relatedEvent?: Row): boolean => {
        if (scopeType === "tenant") {
          return isTenantOwnedRow(row, relatedEvent, entityScopeIndex, eventScopeIndex);
        }
        if (scopeId === "todos") return false;
        return rowScopeIds(row, relatedEvent, entityScopeIndex, eventScopeIndex)[scopeType].includes(scopeId);
      },
    [entityScopeIndex, eventScopeIndex, scopeId, scopeType]
  );

  const eventOptions = useMemo(
    () =>
      data.events
        .filter((event) => matchesActiveScope(event, event))
        .map((event) => ({ id: eventId(event), name: eventName(event) }))
        .filter((event, index, list) => event.id && list.findIndex((item) => item.id === event.id) === index)
        .sort((left, right) => left.name.localeCompare(right.name, "pt-BR")),
    [data.events, matchesActiveScope]
  );

  const productOptions = useMemo(
    () =>
      data.products
        .filter((product) => {
          const productEvent = productEventId(product);
          if (!productEvent) return false;
          const relatedEvent = eventById.get(productEvent);
          return matchesActiveScope(product, relatedEvent);
        })
        .map((product) => ({ id: asString(product.id), name: productName(product) }))
        .filter((product, index, list) => product.id && list.findIndex((item) => item.id === product.id) === index)
        .sort((left, right) => left.name.localeCompare(right.name, "pt-BR")),
    [data.products, eventById, matchesActiveScope]
  );

  const selectedData = useMemo(() => {
    const selectedEvents = data.events.filter((event) => {
      if (eventFilter !== "todos" && eventId(event) !== eventFilter) return false;
      return matchesActiveScope(event, event);
    });
    const selectedEventIds = new Set(selectedEvents.map(eventId).filter(Boolean));

    const selectedTickets = data.tickets.filter((ticket) => {
      const ticketEvent = ticketEventId(ticket);
      const relatedEvent = eventById.get(ticketEvent);
      if (!ticketEvent || !relatedEvent) return false;
      if (eventFilter !== "todos" && ticketEvent !== eventFilter) return false;
      if (!selectedEventIds.has(ticketEvent)) return false;
      if (!matchesActiveScope(ticket, relatedEvent)) return false;
      return dateInPeriod(ticketPurchaseDate(ticket), startDate, endDate);
    });

    const selectedRsvps = data.rsvps.filter((rsvp) => {
      const event = rsvpEventId(rsvp);
      const relatedEvent = eventById.get(event);
      if (!event || !relatedEvent) return false;
      if (eventFilter !== "todos" && event !== eventFilter) return false;
      if (!selectedEventIds.has(event)) return false;
      if (!matchesActiveScope(rsvp, relatedEvent)) return false;
      return dateInPeriod(rsvpDate(rsvp), startDate, endDate);
    });

    const selectedProducts = data.products.filter((product) => {
      const productEvent = productEventId(product);
      if (!productEvent) return false;
      const relatedEvent = eventById.get(productEvent);
      if (eventFilter !== "todos" && productEvent !== eventFilter) return false;
      if (productFilter !== "todos" && asString(product.id) !== productFilter) return false;
      if (selectedEventIds.size > 0 && !selectedEventIds.has(productEvent)) return false;
      return matchesActiveScope(product, relatedEvent);
    });
    const selectedProductIds = new Set(selectedProducts.map((product) => asString(product.id)).filter(Boolean));

    const selectedOrders = data.orders.filter((order) => {
      const orderEvent = orderEventId(order);
      const productId = orderProductId(order);
      const productEvent = productEventId(productsById.get(productId) ?? {});
      const relatedEvent = eventById.get(orderEvent || productEvent);
      if (!orderEvent && !selectedProductIds.has(productId)) return false;
      if (eventFilter !== "todos" && (orderEvent || productEvent) !== eventFilter) return false;
      if (productFilter !== "todos" && productId !== productFilter) return false;
      if ((orderEvent || productEvent) && !relatedEvent) return false;
      if ((orderEvent || productEvent) && !selectedEventIds.has(orderEvent || productEvent)) return false;
      if (!matchesActiveScope(order, relatedEvent)) return false;
      return dateInPeriod(orderCreatedAt(order), startDate, endDate);
    });

    return {
      events: selectedEvents,
      tickets: selectedTickets,
      rsvps: selectedRsvps,
      products: selectedProducts,
      orders: selectedOrders,
    };
  }, [data.events, data.orders, data.products, data.rsvps, data.tickets, endDate, eventById, eventFilter, matchesActiveScope, productFilter, productsById, startDate]);

  const analytics = useMemo(() => {
    const approvedTickets = selectedData.tickets.filter((ticket) => isApprovedStatus(statusValue(ticket)));
    const rejectedTickets = selectedData.tickets.filter((ticket) => isRejectedStatus(statusValue(ticket)));
    const pendingTickets = selectedData.tickets.filter(
      (ticket) => !isApprovedStatus(statusValue(ticket)) && !isRejectedStatus(statusValue(ticket)) && !isCancelledStatus(statusValue(ticket))
    );
    const approvedOrders = selectedData.orders.filter((order) => isApprovedStatus(statusValue(order)));
    const rejectedOrders = selectedData.orders.filter((order) => isRejectedStatus(statusValue(order)));
    const pendingOrders = selectedData.orders.filter(
      (order) => !isApprovedStatus(statusValue(order)) && !isRejectedStatus(statusValue(order)) && !isCancelledStatus(statusValue(order))
    );
    const cancelledOrders = selectedData.orders.filter((order) => isCancelledStatus(statusValue(order)));
    const refundedOrders = selectedData.orders.filter((order) => isRefundedStatus(statusValue(order)));

    const approvedTicketQuantity = approvedTickets.reduce((sum, ticket) => sum + ticketQuantity(ticket), 0);
    const approvedProductQuantity = approvedOrders.reduce((sum, order) => sum + orderQuantity(order), 0);
    const ticketRevenue = approvedTickets.reduce((sum, ticket) => sum + ticketValue(ticket), 0);
    const productRevenue = approvedOrders.reduce((sum, order) => sum + orderTotal(order), 0);
    const ticketDiscounts = approvedTickets.reduce((sum, ticket) => sum + ticketDiscount(ticket), 0);
    const productDiscounts = approvedOrders.reduce((sum, order) => sum + orderDiscount(order), 0);
    const grossRevenue = ticketRevenue + productRevenue;
    const netRevenue = Math.max(0, grossRevenue - ticketDiscounts - productDiscounts);
    const allApprovedCount = approvedTickets.length + approvedOrders.length;
    const allCreatedCount = selectedData.tickets.length + selectedData.orders.length;
    const paymentSent = allCreatedCount;
    const eventCardClicks = selectedData.events.reduce((sum, event) => sum + eventCardClickCount(event), 0);
    const eventPurchaseClicks = selectedData.events.reduce((sum, event) => sum + eventBuyClickCount(event), 0);
    const productPurchaseClicks = selectedData.products.reduce(
      (sum, product) => sum + parseNumber(product.cliques, 0),
      0
    );
    const purchaseClicks = Math.max(eventPurchaseClicks, productPurchaseClicks);
    const hasRsvpDateFilter = Boolean(startDate || endDate);
    const rsvpGoingFromRows = selectedData.rsvps.filter((rsvp) => rsvpStatus(rsvp) === "going").length;
    const rsvpMaybeFromRows = selectedData.rsvps.filter((rsvp) => rsvpStatus(rsvp) === "maybe").length;
    const rsvpGoing = selectedData.rsvps.length || hasRsvpDateFilter
      ? rsvpGoingFromRows
      : selectedData.events.reduce((sum, event) => sum + parseNumber(eventStats(event).confirmados, 0), 0);
    const rsvpMaybe = selectedData.rsvps.length || hasRsvpDateFilter
      ? rsvpMaybeFromRows
      : selectedData.events.reduce((sum, event) => sum + parseNumber(eventStats(event).talvez, 0), 0);

    const redeemedItems = approvedOrders.reduce((sum, order) => sum + orderRedeemedQuantity(order), 0);
    const redeemedValue = approvedOrders.reduce((sum, order) => {
      const quantity = orderQuantity(order);
      return sum + orderTotal(order) * safeDivide(orderRedeemedQuantity(order), quantity);
    }, 0);
    const pendingRedeemItems = Math.max(0, approvedProductQuantity - redeemedItems);
    const pendingRedeemValue = Math.max(
      0,
      productRevenue -
        redeemedValue -
        cancelledOrders.reduce((sum, order) => sum + orderTotal(order), 0) -
        refundedOrders.reduce((sum, order) => sum + orderTotal(order), 0)
    );

    const byLot = new Map<string, MetricRow>();
    const byClass = new Map<string, MetricRow>();
    const byAudience = new Map<string, MetricRow>();
    const byWeekday = new Map<string, MetricRow>();
    const byPeriod = new Map<string, MetricRow>();
    const byPrice = new Map<string, MetricRow>();
    const byApprover = new Map<string, MetricRow>();
    const byApprovalMethod = new Map<string, MetricRow>();
    const byTicketApprover = new Map<string, MetricRow>();
    const byTicketApprovalMethod = new Map<string, MetricRow>();
    const noShowByClass = new Map<string, MetricRow>();
    const noShowByLot = new Map<string, MetricRow>();
    const scanByHour = new Map<string, MetricRow>();
    const productRows = new Map<string, ProductMetricRow>();
    const byProductCategory = new Map<string, MetricRow>();
    const byDiscountSource = new Map<string, MetricRow>();
    const byOrderSource = new Map<string, MetricRow>();
    const byWithdrawalMethod = new Map<string, MetricRow>();
    const byWithdrawalOperator = new Map<string, MetricRow>();
    const byTransferMode = new Map<string, MetricRow>();
    const byTransferTarget = new Map<string, MetricRow>();
    const byTransferActor = new Map<string, MetricRow>();
    const leadBuckets = new Map<string, MetricRow>();
    ["30 dias ou mais", "15 a 29 dias", "7 a 14 dias", "3 a 6 dias", "24 a 72h", "Menos de 24h"].forEach((bucket) =>
      leadBuckets.set(bucket, { name: bucket, quantity: 0, value: 0 })
    );
    const recurrenceRows = new Map<string, MetricRow>();
    const eventSummary = new Map<string, MetricRow>();
    const buyerPurchases = new Map<string, number>();
    const ticketBuyerPurchases = new Map<string, number>();
    const checkedInTicketBuyerIds = new Set<string>();
    const productPurchaseBuyerIds = new Set<string>();
    const transferKeys = new Set<string>();
    const approvalDurations: number[] = [];
    const ticketApprovalDurations: number[] = [];
    const pendingAging = { less1: 0, oneTo6: 0, sixTo24: 0, more24: 0 };
    const ticketPendingAging = { less1: 0, oneTo6: 0, sixTo24: 0, more24: 0 };
    const scanTokens = new Map<string, number>();
    let invalidScans = 0;
    let appScans = 0;
    let manualScans = 0;
    let slowApprovals = 0;

    const toStatementHref = (
      record: OperationalRecord,
      options: StatementLinkOptions = {}
    ) =>
      buildStatementHref(record.eventId, {
        type: options.type ?? record.kind,
        status: options.status ?? record.statusFilter,
        search: options.search ?? record.itemName,
        alert: options.alert,
        source: options.source,
        approver: options.approver,
        flow: options.flow,
        indicator: options.indicator,
      });

    const ticketRecords: OperationalRecord[] = selectedData.tickets.map((ticket) => {
      const relatedEvent = eventById.get(ticketEventId(ticket));
      const status = statusValue(ticket);
      const data = asObject(ticket.data) ?? {};
      const createdAt = ticketPurchaseDate(ticket);
      const approvedAt = ticketApprovalDate(ticket);
      const method = ticketApprovalMethod(ticket);
      const source = ticketSource(ticket);
      const category = ticketItemCategory(ticket);
      const lotName = ticketLotName(ticket);
      const itemName = ticketItemName(ticket);
      const completedAt = ticketRowCheckinAt(ticket) || getLatestDateFromEntries(readTicketEntries(ticket));
      const completedBy =
        asString(ticket.checkinByUserName || ticket.checkinBy || data.checkinByUserName).trim() ||
        readTicketEntries(ticket)
          .map((entry) => asString(entry.scannedByUserName || entry.usedByUserName || entry.checkinByUserName).trim())
          .find(Boolean) ||
        "-";
      const completionMethod =
        approvalMethodLabel(ticket.checkinMethod || data.checkinMethod, "") ||
        readTicketEntries(ticket)
          .map((entry) => entryScanSource(entry))
          .find((entry) => entry !== "-") ||
        (completedAt ? "QR code" : "-");
      const transferred = extractTicketTransfers(ticket).length > 0 || readTicketEntries(ticket).some(isTransferredEntry);
      const manual = isManualTicket(ticket);
      const courtesy = isCourtesyText(`${category} ${lotName} ${itemName} ${method}`);
      const approved = isApprovedStatus(status);
      const rejected = isRejectedStatus(status);
      const cancelled = isCancelledStatus(status) || isRefundedStatus(status);
      return {
        id: asString(ticket.id).trim(),
        eventId: ticketEventId(ticket),
        eventName: relatedEvent ? eventName(relatedEvent) : asString(ticket.eventoNome, "Evento"),
        kind: "ingresso",
        status: asString(ticket.status || status).trim() || "-",
        statusFilter: statementStatusFilterFromStatus(status),
        typeLabel: transferred ? "Transferência" : manual ? "Cadastro manual" : courtesy ? "Cortesia" : "Ingresso",
        itemName,
        category,
        lotName,
        quantity: ticketQuantity(ticket),
        value: ticketValue(ticket),
        expectedValue: expectedTicketTotal(ticket, relatedEvent),
        discount: ticketDiscount(ticket),
        discountSource: ticketDiscountSource(ticket),
        createdAt,
        approvedAt,
        completedAt,
        approver: ticketApproverName(ticket),
        approvalMethod: method,
        source,
        paymentSource: ticketPaymentSource(ticket),
        createdBy: asString(data.createdByName || data.createdBy || data.operatorName).trim() || "-",
        completedBy,
        completionMethod,
        manual,
        manualAtDoor: Boolean(data.manualGateEntry) || normalizeText(`${source} ${method} ${lotName}`).includes("porta"),
        hasCode: ticketHasQrCode(ticket),
        usedQuantity: ticketScannedCount(ticket),
        approved,
        pending: !approved && !rejected && !cancelled,
        rejected,
        cancelled,
        transferred,
        courtesy,
      };
    });

    const orderRecords: OperationalRecord[] = selectedData.orders.map((order) => {
      const orderEvent = orderEventId(order) || productEventId(productsById.get(orderProductId(order)) ?? {});
      const relatedEvent = eventById.get(orderEvent);
      const status = statusValue(order);
      const data = asObject(order.data) ?? {};
      const eventParty = asObject(data.eventParty) ?? {};
      const method = orderApprovalMethod(order);
      const source = orderSource(order);
      const itemName = orderItemName(order, productsById);
      const category = orderItemCategory(order, productsById);
      const manual = isManualOrder(order);
      const transferred = extractProductTransfers(order).length > 0 || readVoucherEntries(order).some(isTransferredEntry);
      const courtesy = isCourtesyText(`${category} ${itemName} ${method} ${source}`);
      const approved = isApprovedStatus(status);
      const rejected = isRejectedStatus(status);
      const cancelled = isCancelledStatus(status) || isRefundedStatus(status);
      return {
        id: asString(order.id).trim(),
        eventId: orderEvent,
        eventName: relatedEvent ? eventName(relatedEvent) : "Evento",
        kind: "produto",
        status: asString(order.status || status).trim() || "-",
        statusFilter: statementStatusFilterFromStatus(status),
        typeLabel: transferred ? "Transferência" : manual ? "Cadastro manual" : courtesy ? "Cortesia" : "Produto",
        itemName,
        category,
        lotName: asString(order.eventLoteNome || eventParty.loteNome).trim() || "-",
        quantity: orderQuantity(order),
        value: orderTotal(order),
        expectedValue: expectedOrderTotal(order, productsById),
        discount: orderDiscount(order),
        discountSource: orderDiscountSource(order),
        createdAt: orderCreatedAt(order),
        approvedAt: orderApprovalDate(order),
        completedAt: orderWithdrawalDate(order),
        approver: orderApproverName(order),
        approvalMethod: method,
        source,
        paymentSource: orderPaymentSource(order),
        createdBy: asString(order.eventCreatedByName || eventParty.createdByName || eventParty.createdByUserName).trim() || "-",
        completedBy: orderWithdrawalOperator(order),
        completionMethod: orderWithdrawalMethod(order),
        manual,
        manualAtDoor: normalizeText(`${source} ${method} ${category}`).includes("porta"),
        hasCode: orderHasCode(order),
        usedQuantity: orderRedeemedQuantity(order),
        approved,
        pending: !approved && !rejected && !cancelled,
        rejected,
        cancelled,
        transferred,
        courtesy,
      };
    });

    const operationalRecords = [...ticketRecords, ...orderRecords].filter((record) => record.eventId);
    const approvedOperationalRecords = operationalRecords.filter((record) => record.approved);
    const pendingOperationalRecords = operationalRecords.filter((record) => record.pending);
    const operationalApprovalDurations = approvedOperationalRecords
      .map((record) =>
        record.createdAt && record.approvedAt
          ? (record.approvedAt.getTime() - record.createdAt.getTime()) / 36e5
          : Number.NaN
      )
      .filter((value) => Number.isFinite(value) && value >= 0);
    const pendingWaitHours = pendingOperationalRecords
      .map((record) => (record.createdAt ? (Date.now() - record.createdAt.getTime()) / 36e5 : Number.NaN))
      .filter((value) => Number.isFinite(value) && value >= 0);
    const singleOperationalEventId =
      eventFilter !== "todos"
        ? eventFilter
        : selectedData.events.length === 1
          ? eventId(selectedData.events[0])
          : "";
    const eventHref = (targetEventId: string, options: StatementLinkOptions = {}) =>
      buildStatementHref(targetEventId, options);
    const checkinsHref = (targetEventId: string, options: CheckinsLinkOptions = {}) =>
      buildCheckinsHref(targetEventId, options);

    const pendingByEvent = new Map<string, MetricRow>();
    const pendingByType = new Map<string, MetricRow>();
    const pendingAgeBuckets = new Map<string, MetricRow>();
    ["Menos de 15 min", "15 a 60 min", "1 a 6h", "6 a 24h", "Mais de 24h"].forEach((bucket) =>
      pendingAgeBuckets.set(bucket, { name: bucket, quantity: 0, value: 0 })
    );
    pendingOperationalRecords.forEach((record) => {
      addMetric(
        pendingByEvent,
        record.eventName,
        1,
        record.value,
        0,
        eventHref(record.eventId, { status: "pendente" })
      );
      addMetric(
        pendingByType,
        record.typeLabel,
        1,
        record.value,
        0,
        singleOperationalEventId
          ? eventHref(singleOperationalEventId, {
              type: record.kind,
              status: "pendente",
              search: record.typeLabel === "Produto" || record.typeLabel === "Ingresso" ? "" : record.typeLabel,
            })
          : ""
      );
      const ageHours = record.createdAt ? (Date.now() - record.createdAt.getTime()) / 36e5 : 0;
      const ageBucket =
        ageHours < 0.25
          ? "Menos de 15 min"
          : ageHours < 1
            ? "15 a 60 min"
            : ageHours < 6
              ? "1 a 6h"
              : ageHours < 24
                ? "6 a 24h"
                : "Mais de 24h";
      addMetric(pendingAgeBuckets, ageBucket, 1, record.value);
    });

    const pendingNearEvent = pendingOperationalRecords.filter((record) => {
      const relatedEvent = eventById.get(record.eventId);
      const start = eventDate(relatedEvent);
      if (!start) return false;
      const hoursUntilEvent = (start.getTime() - Date.now()) / 36e5;
      return hoursUntilEvent >= -2 && hoursUntilEvent <= 24;
    }).length;
    const pendingAtDoor = pendingOperationalRecords.filter((record) => record.manualAtDoor).length;

    const groupRecords = (
      records: OperationalRecord[],
      getKey: (record: OperationalRecord) => string
    ) => {
      const groups = new Map<string, OperationalRecord[]>();
      records.forEach((record) => {
        const key = getKey(record).trim() || "Sem dado";
        groups.set(key, [...(groups.get(key) ?? []), record]);
      });
      return groups;
    };

    const showEventInOperationalGroups = eventFilter === "todos" && selectedData.events.length > 1;
    const linkedOperationalGroups = (
      records: OperationalRecord[],
      getName: (record: OperationalRecord) => string
    ) => {
      const groups = new Map<string, { name: string; rawName: string; eventId: string; records: OperationalRecord[] }>();
      records.forEach((record) => {
        const rawName = getName(record).trim() || "Sem dado";
        const key = showEventInOperationalGroups ? `${record.eventId}:${rawName}` : rawName;
        const current = groups.get(key) ?? {
          name: showEventInOperationalGroups ? `${record.eventName} · ${rawName}` : rawName,
          rawName,
          eventId: record.eventId,
          records: [],
        };
        current.records.push(record);
        groups.set(key, current);
      });
      return Array.from(groups.values());
    };

    const buildSlaMetricRows = (
      groups: Array<{ name: string; rawName: string; eventId: string; records: OperationalRecord[] }>,
      hrefOptions: (rawName: string) => StatementLinkOptions
    ): MetricRow[] =>
      groups
        .map((group) => {
          const durations = group.records
            .map((record) =>
              record.createdAt && record.approvedAt
                ? (record.approvedAt.getTime() - record.createdAt.getTime()) / 36e5
                : Number.NaN
            )
            .filter((value) => Number.isFinite(value) && value >= 0);
          return {
            name: group.name,
            quantity: group.records.length,
            value: median(durations),
            average: safeDivide(durations.reduce((sum, value) => sum + value, 0), durations.length),
            secondary: percentile(durations, 0.9),
            hint: `P90 ${formatHours(percentile(durations, 0.9))} · P95 ${formatHours(percentile(durations, 0.95))}`,
            href: eventHref(group.eventId, { status: "aprovado", ...hrefOptions(group.rawName) }),
          };
        })
        .sort((left, right) => right.quantity - left.quantity || right.value - left.value)
        .slice(0, 12);

    const slaBySourceRows = buildSlaMetricRows(
      linkedOperationalGroups(approvedOperationalRecords, (record) => record.source),
      (source) => ({ source })
    );
    const slaByApproverRows = buildSlaMetricRows(
      linkedOperationalGroups(approvedOperationalRecords, (record) => record.approver),
      (approver) => ({ approver })
    );
    const slaByEventRows = buildSlaMetricRows(
      Array.from(groupRecords(approvedOperationalRecords, (record) => record.eventName).entries()).map(([name, records]) => ({
        name,
        rawName: name,
        eventId: records[0]?.eventId || "",
        records,
      })),
      () => ({})
    );

    const approvalToEntryDurations = approvedOperationalRecords
      .filter((record) => record.kind === "ingresso")
      .map((record) =>
        record.approvedAt && record.completedAt
          ? (record.completedAt.getTime() - record.approvedAt.getTime()) / 36e5
          : Number.NaN
      )
      .filter((value) => Number.isFinite(value) && value >= 0);
    const approvalToWithdrawalDurations = approvedOperationalRecords
      .filter((record) => record.kind === "produto")
      .map((record) =>
        record.approvedAt && record.completedAt
          ? (record.completedAt.getTime() - record.approvedAt.getTime()) / 36e5
          : Number.NaN
      )
      .filter((value) => Number.isFinite(value) && value >= 0);
    const approvedWithoutCode = approvedOperationalRecords.filter((record) => !record.hasCode);
    const codeWithoutUse = approvedOperationalRecords.filter((record) => record.hasCode && record.usedQuantity <= 0);
    const usedWithoutApproval = operationalRecords.filter((record) => !record.approved && record.usedQuantity > 0);
    const inconsistentStatus = operationalRecords.filter(
      (record) =>
        (record.approved && !record.approvedAt) ||
        (!record.approved && record.usedQuantity > 0) ||
        (record.cancelled && record.usedQuantity > 0)
    );
    const approvedNearEvent = approvedOperationalRecords.filter((record) => {
      const start = eventDate(eventById.get(record.eventId));
      if (!start || !record.approvedAt) return false;
      const hoursBeforeEvent = (start.getTime() - record.approvedAt.getTime()) / 36e5;
      return hoursBeforeEvent >= -1 && hoursBeforeEvent <= 2;
    });

    const operatorQualityRows = linkedOperationalGroups(approvedOperationalRecords, (record) => record.approver)
      .map((group) => {
        const operator = group.rawName;
        const records = group.records;
        const durations = records
          .map((record) =>
            record.createdAt && record.approvedAt
              ? (record.approvedAt.getTime() - record.createdAt.getTime()) / 36e5
              : Number.NaN
          )
          .filter((value) => Number.isFinite(value) && value >= 0);
        const cancelledAfterApproval = operationalRecords.filter(
          (record) => record.eventId === group.eventId && record.cancelled && record.approver === operator && Boolean(record.approvedAt)
        ).length;
        return {
          evento: showEventInOperationalGroups ? records[0]?.eventName || "-" : records[0]?.eventName || "-",
          operador: operator,
          aprovados: records.length,
          valor: records.reduce((sum, record) => sum + record.value, 0),
          mediana: median(durations),
          semValor: records.filter((record) => record.value <= 0).length,
          manuais: records.filter((record) => record.manual || normalizeText(record.approvalMethod).includes("manual")).length,
          corrigidos: cancelledAfterApproval,
          semUso: records.filter((record) => record.usedQuantity <= 0).length,
          mesmoCriador: records.filter(
            (record) => record.createdBy !== "-" && normalizeText(record.createdBy) === normalizeText(record.approver)
          ).length,
          mesmoBaixa: records.filter(
            (record) => record.completedBy !== "-" && normalizeText(record.completedBy) === normalizeText(record.approver)
          ).length,
          href: eventHref(group.eventId, { status: "aprovado", approver: operator }),
          hrefAprovados: eventHref(group.eventId, { status: "aprovado", approver: operator }),
          hrefValor: eventHref(group.eventId, { status: "aprovado", approver: operator }),
          hrefSemValor: eventHref(group.eventId, { status: "aprovado", approver: operator, indicator: "sem-valor" }),
          hrefManuais: eventHref(group.eventId, { status: "aprovado", approver: operator, flow: "aprovacao" }),
          hrefCorrigidos: eventHref(group.eventId, { approver: operator, indicator: "cancelado-pos-aprovacao" }),
          hrefSemUso: eventHref(group.eventId, { status: "aprovado", approver: operator, indicator: "sem-uso" }),
          hrefMesmoCriador: eventHref(group.eventId, { status: "aprovado", approver: operator, indicator: "mesmo-criador" }),
          hrefMesmoBaixa: eventHref(group.eventId, { status: "aprovado", approver: operator, indicator: "mesmo-baixa" }),
        };
      })
      .sort((left, right) => right.aprovados - left.aprovados);

    const activeOperatorCount = new Set(
      approvedOperationalRecords
        .map((record) => record.approver)
        .filter((operator) => operator && operator !== "Sem aprovador")
    ).size;
    const operatorDistributionRows = metricRows(
      approvedOperationalRecords.reduce((map, record) => {
        addMetric(
          map,
          showEventInOperationalGroups ? `${record.eventName} · ${record.approver}` : record.approver,
          1,
          record.value,
          0,
          eventHref(record.eventId, { status: "aprovado", approver: record.approver })
        );
        return map;
      }, new Map<string, MetricRow>()),
      12
    );
    const hourKey = (date: Date | null) =>
      date
        ? `${String(date.getDate()).padStart(2, "0")}/${String(date.getMonth() + 1).padStart(2, "0")} ${String(
            date.getHours()
          ).padStart(2, "0")}h`
        : "";
    const createdByHour = new Map<string, number>();
    const approvedByHour = new Map<string, number>();
    operationalRecords.forEach((record) => {
      const createdKey = hourKey(record.createdAt);
      if (createdKey) createdByHour.set(createdKey, (createdByHour.get(createdKey) ?? 0) + 1);
      const approvedKey = hourKey(record.approvedAt);
      if (approvedKey) approvedByHour.set(approvedKey, (approvedByHour.get(approvedKey) ?? 0) + 1);
    });
    const demandWithoutCoverageRows = Array.from(createdByHour.entries())
      .filter(([key]) => (approvedByHour.get(key) ?? 0) === 0)
      .map(([horario, criados]) => ({ horario, criados, aprovacoes: 0 }))
      .sort((left, right) => right.criados - left.criados)
      .slice(0, 10);
    const outsideHoursApprovals = approvedOperationalRecords.filter((record) => {
      const hour = record.approvedAt?.getHours();
      return typeof hour === "number" && (hour < 8 || hour >= 23);
    }).length;
    const singleOperatorEventRows = Array.from(groupRecords(approvedOperationalRecords, (record) => record.eventName).entries())
      .map(([eventLabel, records]) => {
        const operators = new Set(records.map((record) => record.approver).filter((operator) => operator !== "Sem aprovador"));
        return {
          evento: eventLabel,
          operador: Array.from(operators)[0] || "Sem aprovador",
          aprovacoes: records.length,
          href: eventHref(records[0]?.eventId || "", { status: "aprovado" }),
          operadores: operators.size,
        };
      })
      .filter((row) => row.operadores === 1 && row.aprovacoes > 0)
      .sort((left, right) => right.aprovacoes - left.aprovacoes)
      .slice(0, 10);

    type ManualityStageRow = TableRow & {
      evento: string;
      tipo: string;
      etapa: string;
      quantidade: number;
      valor: number;
      percentual: number;
      href: string;
    };
    const manualityGroups = new Map<string, ManualityStageRow>();
    const addManualityStage = (
      record: OperationalRecord,
      flow: OperationalFlow,
      label: string,
      indicator: string,
      status?: StatementStatusFilter
    ) => {
      const typeLabel = record.kind === "produto" ? "Produto / modo vendas" : "Ingresso";
      const key = `${record.eventId}:${record.kind}:${flow}:${indicator}`;
      const sameBase = operationalRecords.filter((entry) => entry.eventId === record.eventId && entry.kind === record.kind).length;
      const current =
        manualityGroups.get(key) ??
        {
          evento: record.eventName,
          tipo: typeLabel,
          etapa: label,
          quantidade: 0,
          valor: 0,
          percentual: 0,
          href: eventHref(record.eventId, { type: record.kind, status, flow, indicator }),
        };
      current.quantidade += 1;
      current.valor += record.value;
      current.percentual = safeDivide(current.quantidade, sameBase) * 100;
      manualityGroups.set(key, current);
    };

    operationalRecords.forEach((record) => {
      const normalizedSource = normalizeText(record.source);
      const normalizedApproval = normalizeText(record.approvalMethod);
      const normalizedCompletion = normalizeText(record.completionMethod);
      const outsideCheckout =
        normalizedSource &&
        normalizedSource !== "app" &&
        normalizedSource !== "app usc" &&
        normalizedSource !== "checkout publico";

      if (record.manual || outsideCheckout) {
        addManualityStage(record, "pedido", "Pedido", "pedido-manual");
      }
      if (record.approved && (record.manual || normalizedApproval.includes("manual") || normalizedApproval.includes("admin"))) {
        addManualityStage(record, "aprovacao", "Aprovação", "aprovacao-manual", "aprovado");
      }
      if (record.kind === "ingresso" && record.usedQuantity > 0 && normalizedCompletion.includes("manual")) {
        addManualityStage(record, "checkin", "Check-in", "checkin-manual");
      }
      if (record.kind === "produto" && record.usedQuantity > 0 && normalizedCompletion.includes("manual")) {
        addManualityStage(record, "retirada", "Retirada", "retirada-manual");
      }
    });

    const manualityStageRows = Array.from(manualityGroups.values()).sort(
      (left, right) => right.quantidade - left.quantidade || left.evento.localeCompare(right.evento, "pt-BR")
    );
    const manualityStageChartRows: MetricRow[] = manualityStageRows.slice(0, 12).map((row) => ({
      name: `${row.evento} · ${row.tipo.replace(" / modo vendas", "")} · ${row.etapa}`,
      quantity: row.quantidade,
      value: row.percentual,
      href: row.href,
    }));

    type AlertGroup = {
      alerta: string;
      descricao: string;
      evento: string;
      item: string;
      tipo: string;
      quantidade: number;
      href: string;
    };
    const alertGroups = new Map<string, AlertGroup>();
    const addOperationalAlert = (
      alertKey: string,
      label: string,
      record: OperationalRecord,
      status: StatementStatusFilter = record.statusFilter
    ) => {
      const key = `${alertKey}:${record.eventId}:${record.kind}:${record.itemName}:${status}`;
      const current = alertGroups.get(key) ?? {
        alerta: label,
        descricao: OPERATIONAL_ALERT_DESCRIPTIONS[alertKey] || "Confira os itens deste alerta no extrato para entender o impacto operacional.",
        evento: record.eventName,
        item: record.itemName,
        tipo: record.typeLabel,
        quantidade: 0,
        href: toStatementHref(record, {
          status,
          search: record.itemName,
          alert: alertKey,
        }),
      };
      current.quantidade += 1;
      alertGroups.set(key, current);
    };

    operationalRecords.forEach((record) => {
      const expectedAfterDiscount = Number.isFinite(record.expectedValue)
        ? Math.max(0, record.expectedValue - record.discount)
        : Number.NaN;
      const hasValueMismatch = Number.isFinite(expectedAfterDiscount) && Math.abs(expectedAfterDiscount - record.value) > 0.01;
      if (record.approved && record.value <= 0) addOperationalAlert("aprovado-sem-valor", "Aprovado sem valor", record, "aprovado");
      if (record.approved && record.value <= 0 && !record.courtesy) addOperationalAlert("valor-zero-sem-cortesia", "Valor zero sem ser cortesia", record, "aprovado");
      if (record.approved && record.courtesy && record.value > 0) addOperationalAlert("cortesia-com-valor", "Cortesia com valor maior que zero", record, "aprovado");
      if (record.discount > 0 && !record.discountSource.trim()) addOperationalAlert("desconto-sem-origem", "Desconto sem origem registrada", record);
      if (record.approved && hasValueMismatch) addOperationalAlert("valor-diferente-tabela", "Valor aprovado diferente do preço do lote/produto", record, "aprovado");
      if (record.manual && hasValueMismatch) addOperationalAlert("manual-fora-padrao", "Pedido manual com valor fora do padrão", record);
      if (hasValueMismatch && record.discount <= 0) addOperationalAlert("preco-incompativel", "Ingresso/produto com preço incompatível com lote", record);
      if (record.approved && record.approvalMethod === "-") addOperationalAlert("pagamento-sem-metodo", "Pagamento aprovado sem método registrado", record, "aprovado");
      if (record.approved && record.paymentSource === "-" && !record.courtesy && !record.manual) addOperationalAlert("aprovado-sem-fonte-pagamento", "Pedido aprovado sem fonte de pagamento", record, "aprovado");
      if (record.transferred && record.value > 0) addOperationalAlert("transferencia-valor-incompativel", "Transferência com valor incompatível", record);
      if (record.approved && !record.hasCode) addOperationalAlert("aprovado-sem-codigo", "Pedido aprovado sem QR/código", record, "aprovado");
      if (record.approved && record.hasCode && record.usedQuantity <= 0) addOperationalAlert("codigo-sem-uso", "Pedido com QR/código, mas sem uso", record, "aprovado");
      if (!record.approved && record.usedQuantity > 0) addOperationalAlert("uso-sem-aprovacao", "Pedido usado sem aprovação clara", record);
      if ((record.approved && !record.approvedAt) || (record.cancelled && record.usedQuantity > 0)) addOperationalAlert("status-incoerente", "Pedido com status incoerente", record);
      if (approvedNearEvent.includes(record)) addOperationalAlert("aprovado-perto-evento", "Pedido aprovado muito perto do horário do evento", record, "aprovado");
    });
    const operationalAlertRows = Array.from(alertGroups.values())
      .sort((left, right) => right.quantidade - left.quantidade || left.alerta.localeCompare(right.alerta, "pt-BR"));

    const addPurchase = (buyer: string, date: Date | null, quantity: number, value: number) => {
      buyerPurchases.set(buyer, (buyerPurchases.get(buyer) ?? 0) + 1);
      const weekday = date ? WEEKDAYS[date.getDay()] : "Sem data";
      addMetric(byWeekday, weekday, quantity, value);
      addMetric(byPeriod, periodFromDate(date), quantity, value);
    };

    const addTransfer = (transfer: TransferMetricEvent) => {
      if (transferKeys.has(transfer.key)) return;
      if (transfer.at && !dateInPeriod(transfer.at, startDate, endDate)) return;
      transferKeys.add(transfer.key);
      addMetric(byTransferMode, transfer.mode, 1, 0);
      addMetric(byTransferTarget, transfer.target, 1, 0);
      addMetric(byTransferActor, transfer.actor || "Sem usuário", 1, 0);
    };

    selectedData.tickets.forEach((ticket) => extractTicketTransfers(ticket).forEach(addTransfer));
    selectedData.orders.forEach((order) => extractProductTransfers(order).forEach(addTransfer));

    const addTicketAudience = (ticket: Row, basis: AudienceBasis) => {
      const quantity = ticketQuantity(ticket);
      const value = ticketValue(ticket);
      const entries = readTicketEntries(ticket);
      const activeEntries = activeTicketEntries(ticket);

      if (basis === "checkin") {
        const checkedEntries = activeEntries.filter(isTicketEntryCheckedIn);
        if (checkedEntries.length > 0) {
          checkedEntries.forEach((entry) => {
            addMetric(byAudience, classifyTicketAudience(ticket, entry, userById), 1, safeDivide(value, quantity));
          });
        } else if (!entries.length && ticketRowCheckinAt(ticket)) {
          addMetric(byAudience, classifyTicketAudience(ticket, null, userById), quantity, value);
        }
        return;
      }

      if (activeEntries.length > 0) {
        activeEntries.forEach((entry) => {
          addMetric(byAudience, classifyTicketAudience(ticket, entry, userById), 1, safeDivide(value, quantity));
        });
      } else if (!entries.length) {
        addMetric(byAudience, classifyTicketAudience(ticket, null, userById), quantity, value);
      }
    };

    const addOrderAudience = (order: Row, basis: AudienceBasis) => {
      const quantity = orderQuantity(order);
      const value = orderTotal(order);
      const audienceQuantity = basis === "checkin" ? orderCheckedInAudienceQuantity(order) : orderAudienceQuantity(order);
      if (audienceQuantity <= 0) return;
      addMetric(
        byAudience,
        classifyOrderAudience(order, userById),
        audienceQuantity,
        value * safeDivide(audienceQuantity, quantity)
      );
    };

    if (audienceBasis === "pedidos") {
      selectedData.tickets.forEach((ticket) => addTicketAudience(ticket, audienceBasis));
      selectedData.orders.forEach((order) => addOrderAudience(order, audienceBasis));
    } else if (audienceBasis === "checkin") {
      approvedTickets.forEach((ticket) => addTicketAudience(ticket, audienceBasis));
      approvedOrders.forEach((order) => addOrderAudience(order, audienceBasis));
    } else {
      approvedTickets.forEach((ticket) => addTicketAudience(ticket, audienceBasis));
      approvedOrders.forEach((order) => addOrderAudience(order, audienceBasis));
    }

    approvedTickets.forEach((ticket) => {
      const quantity = ticketQuantity(ticket);
      const value = ticketValue(ticket);
      const purchase = ticketPurchaseDate(ticket);
      const scanned = ticketScannedCount(ticket);
      const relatedEvent = eventById.get(ticketEventId(ticket));
      const eventLabel = relatedEvent ? eventName(relatedEvent) : asString(ticket.eventoNome, "Evento");
      const noShow = Math.max(quantity - scanned, 0);
      const unit = safeDivide(value, quantity);

      addMetric(byLot, ticketLotName(ticket), quantity, value);
      addMetric(byClass, ticketClassName(ticket), quantity, value);
      addMetric(byPrice, formatCurrency(unit), quantity, value);
      addMetric(noShowByClass, ticketClassName(ticket), noShow, 0);
      addMetric(noShowByLot, ticketLotName(ticket), noShow, 0);
      addMetric(byApprover, ticketApproverName(ticket), 1, value);
      addMetric(byTicketApprover, ticketApproverName(ticket), 1, value);
      const ticketMethod = ticketApprovalMethod(ticket);
      addMetric(byApprovalMethod, ticketMethod, 1, value);
      addMetric(byTicketApprovalMethod, ticketMethod, 1, value);
      addMetric(eventSummary, eventLabel, quantity, value, scanned);
      addPurchase(ticketBuyerId(ticket), purchase, quantity, value);
      ticketBuyerPurchases.set(ticketBuyerId(ticket), (ticketBuyerPurchases.get(ticketBuyerId(ticket)) ?? 0) + 1);

      const eventStart = eventDate(relatedEvent);
      if (purchase && eventStart) {
        const diffDays = (eventStart.getTime() - purchase.getTime()) / 864e5;
        const bucket =
          diffDays >= 30
            ? "30 dias ou mais"
            : diffDays >= 15
              ? "15 a 29 dias"
              : diffDays >= 7
                ? "7 a 14 dias"
                : diffDays >= 3
                  ? "3 a 6 dias"
                  : diffDays >= 1
                    ? "24 a 72h"
                    : "Menos de 24h";
        const current = leadBuckets.get(bucket);
        if (current) {
          current.quantity += quantity;
          current.value += value;
        }
      }

      readTicketEntries(ticket).forEach((entry) => {
        const scannedAt = entryScannedAt(entry);
        const token = asString(entry.token || entry.id || entry.codigo || entry.qrCode).trim();
        if (token && scannedAt) scanTokens.set(token, (scanTokens.get(token) ?? 0) + 1);
        if (scannedAt) {
          const hour = `${String(scannedAt.getHours()).padStart(2, "0")}h`;
          addMetric(scanByHour, hour, 1, safeDivide(value, quantity));
          if (entryScanSource(entry) === "Manual") manualScans += 1;
          else appScans += 1;
          checkedInTicketBuyerIds.add(ticketBuyerId(ticket));
        }
      });
      if (!readTicketEntries(ticket).length && ticketScannedCount(ticket) > 0) {
        checkedInTicketBuyerIds.add(ticketBuyerId(ticket));
      }
      invalidScans += ticketInvalidScanCount(ticket);

      const approval = ticketApprovalDate(ticket);
      if (purchase && approval) {
        const hours = (approval.getTime() - purchase.getTime()) / 36e5;
        if (Number.isFinite(hours) && hours >= 0) {
          approvalDurations.push(hours);
          ticketApprovalDurations.push(hours);
          if (hours > 24) slowApprovals += 1;
        }
      }
    });

    approvedOrders.forEach((order) => {
      const quantity = orderQuantity(order);
      const value = orderTotal(order);
      const redeemed = orderRedeemedQuantity(order);
      const pending = Math.max(0, quantity - redeemed);
      const productLabel = orderItemName(order, productsById);
      const category = orderItemCategory(order, productsById);
      const purchase = orderCreatedAt(order);
      const approval = orderApprovalDate(order);
      const existing = productRows.get(productLabel) ?? {
        name: productLabel,
        quantity: 0,
        value: 0,
        average: 0,
        redeemed: 0,
        pending: 0,
      };
      existing.quantity += quantity;
      existing.value += value;
      existing.redeemed += redeemed;
      existing.pending += pending;
      existing.average = safeDivide(existing.value, existing.quantity);
      productRows.set(productLabel, existing);

      addMetric(byProductCategory, category, quantity, value);
      addMetric(byApprover, orderApproverName(order), 1, value);
      addMetric(byApprovalMethod, orderApprovalMethod(order), 1, value);
      addMetric(byDiscountSource, orderDiscountSource(order), orderDiscount(order) > 0 ? 1 : 0, orderDiscount(order));
      addMetric(byOrderSource, orderSource(order), 1, value);
      if (redeemed > 0) {
        addMetric(byWithdrawalMethod, orderWithdrawalMethod(order), redeemed, value * safeDivide(redeemed, quantity));
        addMetric(byWithdrawalOperator, orderWithdrawalOperator(order), redeemed, value * safeDivide(redeemed, quantity));
      }
      buyerPurchases.set(orderBuyerId(order), (buyerPurchases.get(orderBuyerId(order)) ?? 0) + 1);
      if (value > 0) {
        productPurchaseBuyerIds.add(orderBuyerId(order));
      }

      if (purchase && approval) {
        const hours = (approval.getTime() - purchase.getTime()) / 36e5;
        if (Number.isFinite(hours) && hours >= 0) {
          approvalDurations.push(hours);
          if (hours > 24) slowApprovals += 1;
        }
      }
    });

    [...pendingTickets, ...pendingOrders].forEach((row) => {
      const isTicket = Boolean(ticketEventId(row));
      const createdAt = isTicket ? ticketPurchaseDate(row) : orderCreatedAt(row);
      if (!createdAt) return;
      const hours = (Date.now() - createdAt.getTime()) / 36e5;
      if (hours < 1) pendingAging.less1 += 1;
      else if (hours < 6) pendingAging.oneTo6 += 1;
      else if (hours < 24) pendingAging.sixTo24 += 1;
      else pendingAging.more24 += 1;
      if (!isTicket) return;
      if (hours < 1) ticketPendingAging.less1 += 1;
      else if (hours < 6) ticketPendingAging.oneTo6 += 1;
      else if (hours < 24) ticketPendingAging.sixTo24 += 1;
      else ticketPendingAging.more24 += 1;
    });

    const duplicateScans = Array.from(scanTokens.values()).filter((count) => count > 1).length;
    const ticketScanned = approvedTickets.reduce((sum, ticket) => sum + ticketScannedCount(ticket), 0);
    const checkedInBuyersWithPurchase = Array.from(checkedInTicketBuyerIds).filter((buyerId) =>
      productPurchaseBuyerIds.has(buyerId)
    ).length;
    const noShow = Math.max(0, approvedTicketQuantity - ticketScanned);

    type GateScanRow = {
      eventId: string;
      eventName: string;
      orderId: string;
      token: string;
      holderName: string;
      turma: string;
      lotName: string;
      ticketType: string;
      source: "Manual" | "QR code";
      operator: string;
      scannedAt: Date;
      value: number;
      href: string;
      transferLabel: string;
    };
    type CountRate = { approved: number; present: number; value: number; href: string };
    type OperatorStats = {
      total: number;
      qr: number;
      manual: number;
      invalid: number;
      duplicate: number;
      eventId: string;
      href: string;
    };

    const gateScans: GateScanRow[] = [];
    const timingBuckets = new Map<string, MetricRow>();
    ["Antes do início", "Primeira hora", "Meio do evento", "Entrada muito tarde", "Não entraram"].forEach((bucket) =>
      timingBuckets.set(bucket, { name: bucket, quantity: 0, value: 0 })
    );
    const presenceByType = new Map<string, CountRate>();
    const presenceByLot = new Map<string, CountRate>();
    const presenceByClass = new Map<string, CountRate>();
    const presenceBySource = new Map<string, CountRate>();
    const presenceByTransfer = new Map<string, CountRate>();
    const presenceByOperationalCategory = new Map<string, CountRate>();
    const invalidReasonGroups = new Map<string, MetricRow>();
    const operatorStats = new Map<string, OperatorStats>();
    const duplicateContextRows: TableRow[] = [];
    const absentRows: TableRow[] = [];
    const unusedActiveRows: TableRow[] = [];

    const addCountRate = (map: Map<string, CountRate>, name: string, approved: number, present: number, value: number, href: string) => {
      const cleanName = name.trim() || "Sem dado";
      const current = map.get(cleanName) ?? { approved: 0, present: 0, value: 0, href };
      current.approved += approved;
      current.present += present;
      current.value += value;
      if (!current.href && href) current.href = href;
      map.set(cleanName, current);
    };
    const countRateRows = (map: Map<string, CountRate>, limit = 12): MetricRow[] =>
      Array.from(map.entries())
        .map(([name, row]) => ({
          name,
          quantity: row.present,
          value: safeDivide(row.present, row.approved) * 100,
          secondary: Math.max(0, row.approved - row.present),
          average: safeDivide(row.value, row.present),
          href: row.href,
        }))
        .sort((left, right) => right.value - left.value || right.quantity - left.quantity || left.name.localeCompare(right.name, "pt-BR"))
        .slice(0, limit);
    const addTimingBucket = (label: string, quantity: number, href = "") => {
      const current = timingBuckets.get(label) ?? { name: label, quantity: 0, value: 0, href };
      current.quantity += quantity;
      current.value += quantity;
      if (!current.href && href) current.href = href;
      timingBuckets.set(label, current);
    };
    const addOperatorStat = (
      operator: string,
      eventIdValue: string,
      source: "Manual" | "QR code" | "Inválida" | "Duplicada",
      href: string
    ) => {
      const cleanOperator = operator.trim() || "Sem operador";
      const current = operatorStats.get(cleanOperator) ?? { total: 0, qr: 0, manual: 0, invalid: 0, duplicate: 0, eventId: eventIdValue, href };
      if (source === "QR code") {
        current.total += 1;
        current.qr += 1;
      } else if (source === "Manual") {
        current.total += 1;
        current.manual += 1;
      } else if (source === "Inválida") {
        current.invalid += 1;
      } else {
        current.duplicate += 1;
      }
      if (!current.href && href) current.href = href;
      operatorStats.set(cleanOperator, current);
    };

    approvedTickets.forEach((ticket) => {
      const eventIdValue = ticketEventId(ticket);
      const relatedEvent = eventById.get(eventIdValue);
      const eventLabel = relatedEvent ? eventName(relatedEvent) : asString(ticket.eventoNome, "Evento");
      const eventStart = eventDate(relatedEvent);
      const quantity = ticketQuantity(ticket);
      const present = ticketScannedCount(ticket);
      const absent = Math.max(0, quantity - present);
      const unitValue = safeDivide(ticketValue(ticket), quantity);
      const ticketHref = eventHref(eventIdValue, { type: "ingresso", status: "aprovado", search: ticketHolderName(ticket) });
      const activeEntries = activeTicketEntries(ticket);
      const entriesForBase = activeEntries.length ? activeEntries : [];
      const typeLabel = classifyTicketOperationalCategory(ticket, entriesForBase[0] ?? null, userById, entityMemberIndex);
      const transferLabel = ticketTransferLabel(ticket);
      const sourceLabel = ticketSource(ticket);
      const classLabel = ticketClassName(ticket);
      const lotLabel = ticketLotName(ticket);
      const ticketCheckinsHref = checkinsHref(eventIdValue, {
        indicator: absent > 0 ? "ausente" : "",
        search: ticketHolderName(ticket),
      });

      addCountRate(presenceByType, typeLabel, quantity, present, ticketValue(ticket), ticketHref);
      addCountRate(presenceByLot, lotLabel, quantity, present, ticketValue(ticket), ticketHref);
      addCountRate(presenceByClass, classLabel, quantity, present, ticketValue(ticket), ticketHref);
      addCountRate(presenceBySource, sourceLabel, quantity, present, ticketValue(ticket), ticketHref);
      addCountRate(presenceByTransfer, transferLabel, quantity, present, ticketValue(ticket), ticketHref);
      addCountRate(presenceByOperationalCategory, typeLabel, quantity, present, ticketValue(ticket), ticketHref);

      const classifyTiming = (scannedAt: Date | null): string => {
        if (!scannedAt) return "Não entraram";
        if (!eventStart) return "Meio do evento";
        const hoursFromStart = (scannedAt.getTime() - eventStart.getTime()) / 36e5;
        if (hoursFromStart < 0) return "Antes do início";
        if (hoursFromStart <= 1) return "Primeira hora";
        if (hoursFromStart <= 4) return "Meio do evento";
        return "Entrada muito tarde";
      };

      readTicketEntries(ticket).forEach((entry) => {
        const reason = ticketEntryInvalidReason(entry, ticket);
        const token = ticketEntryToken(entry);
        const entryHref = eventHref(eventIdValue, { type: "ingresso", status: "aprovado", search: token || ticketHolderName(ticket, entry) });
        if (reason) {
          addMetric(invalidReasonGroups, reason, 1, 0, 0, entryHref);
          addOperatorStat(entryScanOperator(entry, ticket), eventIdValue, "Inválida", entryHref);
        }
        checkinAuditRows(ticket, entry)
          .filter(isDuplicateAuditEntry)
          .forEach((audit) => {
            const firstAt = entryScannedAt(entry) || ticketRowCheckinAt(ticket);
            const secondAt = parseDate(audit.at || audit.createdAt || audit.timestamp);
            const operator = asString(audit.byUserName || audit.operatorName).trim() || entryScanOperator(entry, ticket);
            const diffMinutes = firstAt && secondAt ? Math.max(0, (secondAt.getTime() - firstAt.getTime()) / 60_000) : 0;
            addOperatorStat(operator, eventIdValue, "Duplicada", entryHref);
            duplicateContextRows.push({
              evento: eventLabel,
              pessoa: ticketHolderName(ticket, entry),
              ingresso: token || asString(ticket.id).trim(),
              primeira: formatDateTimeShort(firstAt),
              segunda: formatDateTimeShort(secondAt),
              diferenca: diffMinutes,
              operador: operator,
              acao: normalizeText(audit.action).includes("manual") ? "Liberado manualmente" : "Bloqueado",
              href: entryHref,
            });
          });
      });

      const checkedEntries = activeEntries.filter(isTicketEntryCheckedIn);
      checkedEntries.forEach((entry) => {
        const scannedAt = entryScannedAt(entry) || ticketRowCheckinAt(ticket);
        if (!scannedAt) return;
        const source = entryScanSource(entry) === "Manual" ? "Manual" : "QR code";
        const operator = entryScanOperator(entry, ticket);
        const entryType = classifyTicketOperationalCategory(ticket, entry, userById, entityMemberIndex);
        const entryHref = eventHref(eventIdValue, { type: "ingresso", status: "aprovado", search: ticketEntryToken(entry) || ticketHolderName(ticket, entry) });
        gateScans.push({
          eventId: eventIdValue,
          eventName: eventLabel,
          orderId: asString(ticket.id).trim(),
          token: ticketEntryToken(entry),
          holderName: ticketHolderName(ticket, entry),
          turma: ticketHolderTurma(ticket, entry),
          lotName: lotLabel,
          ticketType: entryType,
          source,
          operator,
          scannedAt,
          value: unitValue,
          href: entryHref,
          transferLabel,
        });
        addTimingBucket(classifyTiming(scannedAt), 1, entryHref);
        addOperatorStat(operator, eventIdValue, source, entryHref);
      });

      if (!readTicketEntries(ticket).length && ticketRowCheckinAt(ticket)) {
        const scannedAt = ticketRowCheckinAt(ticket);
        const source = normalizeText(ticket.checkinMethod).includes("manual") ? "Manual" : "QR code";
        for (let index = 0; index < quantity; index += 1) {
          gateScans.push({
            eventId: eventIdValue,
            eventName: eventLabel,
            orderId: asString(ticket.id).trim(),
            token: asString(ticket.id).trim(),
            holderName: ticketHolderName(ticket),
            turma: ticketHolderTurma(ticket),
            lotName: lotLabel,
            ticketType: typeLabel,
            source,
            operator: rowCheckinOperator(ticket),
            scannedAt: scannedAt as Date,
            value: unitValue,
            href: ticketHref,
            transferLabel,
          });
          addTimingBucket(classifyTiming(scannedAt), 1, ticketHref);
          addOperatorStat(rowCheckinOperator(ticket), eventIdValue, source, ticketHref);
        }
      }

      if (absent > 0) {
        addTimingBucket("Não entraram", absent, ticketHref);
        absentRows.push({
          nome: ticketHolderName(ticket),
          turma: ticketHolderTurma(ticket),
          lote: lotLabel,
          tipo: typeLabel,
          quantidade: absent,
          compra: formatDateTimeShort(ticketPurchaseDate(ticket)),
          contato: ticketContact(ticket, userById),
          qr: ticketQrStatus(ticket),
          transferencia: transferLabel,
          href: ticketCheckinsHref || ticketHref,
        });
      }

      if (
        absent > 0 ||
        !ticketHasQrCode(ticket) ||
        ticketEntryInvalidReason(readTicketEntries(ticket)[0] ?? {}, ticket) ||
        (present > 0 && normalizeText(ticket.checkinMethod).includes("manual"))
      ) {
        const reason = !ticketHasQrCode(ticket)
          ? "Aprovado sem QR (Quick Response)"
          : present > 0 && absent > 0
            ? "Aprovado com entrada parcial"
            : present > 0 && normalizeText(ticket.checkinMethod).includes("manual")
              ? "Aprovado com entrada manual"
              : ticketEntryInvalidReason(readTicketEntries(ticket)[0] ?? {}, ticket)
                ? "Aprovado com tentativa inválida"
                : transferLabel !== "Sem transferência"
                  ? "Aprovado e transferido, mas não utilizado"
                  : "Aprovado e não utilizado";
        unusedActiveRows.push({
          situacao: reason,
          nome: ticketHolderName(ticket),
          turma: ticketHolderTurma(ticket),
          lote: lotLabel,
          quantidade: absent || present || quantity,
          qr: ticketQrStatus(ticket),
          href: ticketCheckinsHref || ticketHref,
        });
      }
    });

    const gateScansSorted = [...gateScans].sort((left, right) => left.scannedAt.getTime() - right.scannedAt.getTime());
    const cumulativeByHour = new Map<string, MetricRow>();
    gateScansSorted.forEach((scan) => {
      const label = `${String(scan.scannedAt.getDate()).padStart(2, "0")}/${String(scan.scannedAt.getMonth() + 1).padStart(2, "0")} ${String(
        scan.scannedAt.getHours()
      ).padStart(2, "0")}h`;
      addMetric(cumulativeByHour, label, 1, 0, 0, scan.href);
    });
    let cumulativeTotal = 0;
    const entryCumulativeRows = Array.from(cumulativeByHour.values())
      .sort((left, right) => left.name.localeCompare(right.name, "pt-BR"))
      .map((row) => {
        cumulativeTotal += row.quantity;
        return { ...row, quantity: cumulativeTotal, value: row.quantity };
      });

    const scanModeByHour = new Map<string, TableRow>();
    gateScansSorted.forEach((scan) => {
      const label = `${String(scan.scannedAt.getHours()).padStart(2, "0")}h`;
      const current = scanModeByHour.get(label) ?? { name: label, qr: 0, manual: 0, total: 0, manualRate: 0, href: scan.href };
      current[scan.source === "Manual" ? "manual" : "qr"] = Number(current[scan.source === "Manual" ? "manual" : "qr"] ?? 0) + 1;
      current.total = Number(current.total ?? 0) + 1;
      current.manualRate = safeDivide(Number(current.manual), Number(current.total)) * 100;
      if (!current.href) current.href = scan.href;
      scanModeByHour.set(label, current);
    });
    const scanModeByHourRows = Array.from(scanModeByHour.values()).sort((left, right) =>
      asString(left.name).localeCompare(asString(right.name), "pt-BR")
    );

    const intervalRows = Array.from(
      gateScansSorted.reduce((map, scan) => {
        const label = minuteBucketLabel(scan.scannedAt, 15);
        addMetric(map, label, 1, safeDivide(1, 15), 0, scan.href);
        return map;
      }, new Map<string, MetricRow>())
    ).map(([, row]) => ({ ...row, value: safeDivide(row.quantity, 15) }))
      .sort((left, right) => left.name.localeCompare(right.name, "pt-BR"));
    const peakInterval = intervalRows.reduce((current, row) => (row.quantity > current.quantity ? row : current), { name: "-", quantity: 0, value: 0 });
    const scanDiffMinutes = gateScansSorted
      .slice(1)
      .map((scan, index) => (scan.scannedAt.getTime() - gateScansSorted[index].scannedAt.getTime()) / 60_000)
      .filter((value) => Number.isFinite(value) && value >= 0);
    const averageMinutesBetweenScans = safeDivide(scanDiffMinutes.reduce((sum, value) => sum + value, 0), scanDiffMinutes.length);
    const longestIdleMinutes = maxValue(scanDiffMinutes);
    let longestFastSequence = 0;
    let currentFastSequence = 0;
    gateScansSorted.forEach((scan, index) => {
      if (index === 0) {
        currentFastSequence = 1;
      } else {
        const diffSeconds = (scan.scannedAt.getTime() - gateScansSorted[index - 1].scannedAt.getTime()) / 1000;
        currentFastSequence = diffSeconds <= 60 ? currentFastSequence + 1 : 1;
      }
      longestFastSequence = Math.max(longestFastSequence, currentFastSequence);
    });

    const activeGateOperators = Array.from(new Set(gateScans.map((scan) => scan.operator).filter((operator) => operator !== "Sem operador"))).length;
    const estimatedOperatorCapacityPer15 = Math.max(1, activeGateOperators || 1) * 4 * 15;
    const queuePressure = safeDivide(peakInterval.quantity, estimatedOperatorCapacityPer15) * 100;
    const queueRisk = queuePressure >= 120 ? "Alto" : queuePressure >= 75 ? "Médio" : "Baixo";
    const totalCapacity = selectedData.events.reduce((sum, event) => sum + eventCapacity(event), 0);
    const capacityRemaining = totalCapacity > 0 ? Math.max(0, totalCapacity - ticketScanned) : 0;
    const occupancyRate = safeDivide(ticketScanned, totalCapacity) * 100;
    const manualityRate = safeDivide(manualScans, ticketScanned) * 100;
    const qrRate = safeDivide(appScans, ticketScanned) * 100;

    const portariaOperatorRows = Array.from(operatorStats.entries())
      .map(([operador, stats]) => ({
        operador,
        entradas: stats.total,
        qr: stats.qr,
        manual: stats.manual,
        invalidas: stats.invalid,
        duplicadas: stats.duplicate,
        erro: safeDivide(stats.invalid + stats.duplicate, stats.total + stats.invalid + stats.duplicate) * 100,
        manualidade: safeDivide(stats.manual, stats.total) * 100,
        href: stats.href,
      }))
      .sort((left, right) => right.entradas - left.entradas || right.erro - left.erro);
    const portariaOperatorChartRows: MetricRow[] = portariaOperatorRows.slice(0, 12).map((row) => ({
      name: asString(row.operador),
      quantity: Number(row.entradas),
      value: Number(row.manualidade),
      href: asString(row.href),
    }));
    const operatorQualityRadarRows: MetricRow[] = [
      { name: "QR", quantity: appScans, value: qrRate },
      { name: "Manual", quantity: manualScans, value: Math.max(0, 100 - manualityRate) },
      { name: "Válidas", quantity: ticketScanned, value: Math.max(0, 100 - safeDivide(invalidScans + duplicateScans, ticketScanned + invalidScans + duplicateScans) * 100) },
      { name: "Velocidade", quantity: peakInterval.quantity, value: Math.min(100, safeDivide(peakInterval.quantity, estimatedOperatorCapacityPer15) * 100) },
      { name: "Presença", quantity: ticketScanned, value: safeDivide(ticketScanned, approvedTicketQuantity) * 100 },
      { name: "Rastreio", quantity: portariaOperatorRows.length, value: gateScans.length ? safeDivide(gateScans.filter((scan) => scan.operator !== "Sem operador").length, gateScans.length) * 100 : 0 },
    ];

    const entryTimingRows = Array.from(timingBuckets.values());
    const presenceByTypeRows = countRateRows(presenceByType, 12);
    const presenceByLotRows = countRateRows(presenceByLot, 12);
    const noShowRateByLotRows = countRateRows(presenceByLot, 12).map((row) => ({
      ...row,
      quantity: row.secondary ?? 0,
      value: safeDivide(row.secondary ?? 0, (row.secondary ?? 0) + row.quantity) * 100,
    }));
    const presentByClassRows = countRateRows(presenceByClass, 12);
    const presenceBySourceRows = countRateRows(presenceBySource, 10);
    const presenceByTransferRows = countRateRows(presenceByTransfer, 10);
    const operationalCategoryRows = countRateRows(presenceByOperationalCategory, 10);
    const invalidReasonRows = metricRows(invalidReasonGroups, 12);

    const entryModeRows: MetricRow[] = [
      { name: "QR (Quick Response)", quantity: appScans, value: 0 },
      { name: "Manual", quantity: manualScans, value: 0 },
    ];
    const occupancyRows: MetricRow[] =
      totalCapacity > 0
        ? [
            { name: "Ocupado", quantity: ticketScanned, value: ticketScanned },
            { name: "Restante", quantity: capacityRemaining, value: capacityRemaining },
          ]
        : [];
    const approvedWithoutReadRows: MetricRow[] = [
      { name: "Não compareceu", quantity: noShow, value: 0 },
      { name: "Entrada manual", quantity: approvedTickets.filter((ticket) => ticketScannedCount(ticket) > 0 && normalizeText(ticket.checkinMethod).includes("manual")).length, value: 0 },
      { name: "Sem QR (Quick Response)", quantity: approvedTickets.filter((ticket) => !ticketHasQrCode(ticket)).length, value: 0 },
      { name: "Tentativa inválida", quantity: approvedTickets.filter((ticket) => readTicketEntries(ticket).some((entry) => ticketEntryInvalidReason(entry, ticket))).length, value: 0 },
      { name: "Dados incompletos", quantity: approvedTickets.filter((ticket) => !ticketHolderName(ticket) || !ticketClassName(ticket)).length, value: 0 },
    ].filter((row) => row.quantity > 0);

    const liveStatusRows = selectedData.events
      .map((event) => {
        const targetEventId = eventId(event);
        const start = eventDate(event);
        const eventScans = gateScans.filter((scan) => scan.eventId === targetEventId);
        const eventApproved = approvedTickets
          .filter((ticket) => ticketEventId(ticket) === targetEventId)
          .reduce((sum, ticket) => sum + ticketQuantity(ticket), 0);
        const eventPresent = eventScans.length;
        const eventLastEntry = eventScans.sort((left, right) => right.scannedAt.getTime() - left.scannedAt.getTime())[0];
        const eventInvalidRows = invalidReasonRows.filter((row) => row.href?.includes(encodeURIComponent(targetEventId)));
        const elapsed = start ? (Date.now() - start.getTime()) / 36e5 : 0;
        return {
          evento: eventName(event),
          iniciado: start ? (elapsed >= 0 ? `${formatDecimal(elapsed)}h` : "Ainda não iniciou") : "Sem data",
          ultimaEntrada: formatDateTimeShort(eventLastEntry?.scannedAt ?? null),
          ultimaInvalida: eventInvalidRows[0]?.name || "-",
          presentes: eventPresent,
          ausentes: Math.max(0, eventApproved - eventPresent),
          entrada: safeDivide(eventPresent, eventApproved) * 100,
          pico: eventScans.length ? peakInterval.name : "-",
          href: checkinsHref(targetEventId),
        };
      })
      .sort((left, right) => right.presentes - left.presentes);

    const portariaEventComparisonRows = selectedData.events
      .map((event) => {
        const targetEventId = eventId(event);
        const eventTickets = approvedTickets.filter((ticket) => ticketEventId(ticket) === targetEventId);
        const approved = eventTickets.reduce((sum, ticket) => sum + ticketQuantity(ticket), 0);
        const present = eventTickets.reduce((sum, ticket) => sum + ticketScannedCount(ticket), 0);
        const scans = gateScans.filter((scan) => scan.eventId === targetEventId);
        const manual = scans.filter((scan) => scan.source === "Manual").length;
        const eventIntervals = new Map<string, number>();
        scans.forEach((scan) => {
          const label = minuteBucketLabel(scan.scannedAt, 15);
          eventIntervals.set(label, (eventIntervals.get(label) ?? 0) + 1);
        });
        const peak = Array.from(eventIntervals.entries()).sort((left, right) => right[1] - left[1])[0];
        return {
          evento: eventName(event),
          aprovados: approved,
          presentes: present,
          presenca: safeDivide(present, approved) * 100,
          pico: peak ? `${peak[0]} (${peak[1]})` : "-",
          manualidade: safeDivide(manual, present) * 100,
          invalidas: eventTickets.reduce((sum, ticket) => sum + ticketInvalidScanCount(ticket), 0),
          href: checkinsHref(targetEventId),
        };
      })
      .filter((row) => row.aprovados > 0 || row.presentes > 0)
      .sort((left, right) => right.presentes - left.presentes);
    const portariaEventComparisonChartRows: MetricRow[] = portariaEventComparisonRows.slice(0, 12).map((row) => ({
      name: asString(row.evento),
      quantity: Number(row.presentes),
      value: Number(row.manualidade),
      href: asString(row.href),
    }));

    const portariaAlertRows = [
      manualityRate > 30
        ? {
            alerta: "Entrada manual excessiva",
            descricao: "A portaria está registrando muita entrada sem leitura direta de QR (Quick Response).",
            impacto: "Alto",
            quantidade: manualScans,
            href: singleOperationalEventId ? eventHref(singleOperationalEventId, { type: "ingresso", status: "aprovado", flow: "checkin", indicator: "manual" }) : "",
          }
        : null,
      usedWithoutApproval.length > 0
        ? {
            alerta: "Entrada sem aprovação",
            descricao: "Existe uso de ingresso sem aprovação clara no extrato.",
            impacto: "Alto",
            quantidade: usedWithoutApproval.length,
            href: singleOperationalEventId ? eventHref(singleOperationalEventId, { type: "ingresso", alert: "uso-sem-aprovacao" }) : "",
          }
        : null,
      duplicateContextRows.length > 0
        ? {
            alerta: "QR (Quick Response) duplicado",
            descricao: "Houve tentativa de reutilizar ingresso já lido.",
            impacto: "Alto",
            quantidade: duplicateContextRows.length,
            href: asString(duplicateContextRows[0]?.href),
          }
        : null,
      ...invalidReasonRows.map((row) => ({
        alerta: row.name,
        descricao: "Motivo de leitura inválida identificado na auditoria da portaria.",
        impacto: row.name.includes("outro evento") || row.name.includes("cancelado") ? "Alto" : "Médio",
        quantidade: row.quantity,
        href: row.href || "",
      })),
      approvedTickets.filter((ticket) => !ticketHasQrCode(ticket)).length > 0
        ? {
            alerta: "Ingresso aprovado sem QR (Quick Response)",
            descricao: "O ingresso foi aprovado, mas não tem código operacional para leitura.",
            impacto: "Médio",
            quantidade: approvedTickets.filter((ticket) => !ticketHasQrCode(ticket)).length,
            href: singleOperationalEventId ? eventHref(singleOperationalEventId, { type: "ingresso", status: "aprovado", indicator: "sem-qr" }) : "",
          }
        : null,
      gateScans.filter((scan) => {
        const ticket = approvedTickets.find((entry) => asString(entry.id).trim() === scan.orderId);
        const approvedAt = ticket ? ticketApprovalDate(ticket) : null;
        return approvedAt && scan.scannedAt.getTime() < approvedAt.getTime();
      }).length > 0
        ? {
            alerta: "Entrada registrada antes da aprovação",
            descricao: "A entrada aconteceu antes da aprovação do pedido.",
            impacto: "Alto",
            quantidade: gateScans.filter((scan) => {
              const ticket = approvedTickets.find((entry) => asString(entry.id).trim() === scan.orderId);
              const approvedAt = ticket ? ticketApprovalDate(ticket) : null;
              return approvedAt && scan.scannedAt.getTime() < approvedAt.getTime();
            }).length,
            href: singleOperationalEventId ? eventHref(singleOperationalEventId, { type: "ingresso", indicator: "entrada-antes-aprovacao" }) : "",
          }
        : null,
      portariaOperatorRows.filter((row) => Number(row.erro) >= 20 && Number(row.invalidas) + Number(row.duplicadas) >= 2).length > 0
        ? {
            alerta: "Muitas leituras inválidas pelo mesmo operador",
            descricao: "Um ou mais operadores concentram leituras inválidas ou duplicadas.",
            impacto: "Médio",
            quantidade: portariaOperatorRows.filter((row) => Number(row.erro) >= 20 && Number(row.invalidas) + Number(row.duplicadas) >= 2).length,
            href: asString(portariaOperatorRows[0]?.href),
          }
        : null,
      portariaOperatorRows.filter((row) => Number(row.manualidade) >= 60 && Number(row.manual) >= 3).length > 0
        ? {
            alerta: "Muitas entradas manuais pelo mesmo operador",
            descricao: "Um ou mais operadores usaram entrada manual em excesso.",
            impacto: "Médio",
            quantidade: portariaOperatorRows.filter((row) => Number(row.manualidade) >= 60 && Number(row.manual) >= 3).length,
            href: asString(portariaOperatorRows[0]?.href),
          }
        : null,
      selectedData.events.some((event) => {
        const start = eventDate(event);
        return start && Date.now() > start.getTime() && absentRows.length > 0;
      })
        ? {
            alerta: "Aprovado sem entrada após início do evento",
            descricao: "Há ingressos aprovados que ainda não viraram presença depois do início.",
            impacto: "Baixo",
            quantidade: absentRows.length,
            href: singleOperationalEventId ? checkinsHref(singleOperationalEventId, { indicator: "ausente" }) : "",
          }
        : null,
    ].filter(Boolean) as TableRow[];

    const recurringBuyers = Array.from(buyerPurchases.values()).filter((count) => count > 1).length;
    recurrenceRows.set("Novos", { name: "Novos", quantity: Math.max(0, buyerPurchases.size - recurringBuyers), value: 0 });
    recurrenceRows.set("Recorrentes", { name: "Recorrentes", quantity: recurringBuyers, value: 0 });

    const firstPurchase = [...approvedTickets.map(ticketPurchaseDate), ...approvedOrders.map(orderCreatedAt)]
      .filter((date): date is Date => Boolean(date))
      .sort((left, right) => left.getTime() - right.getTime())[0];
    const selectedSingleEvent = eventFilter !== "todos" ? selectedData.events[0] : null;
    const selectedEventDate = eventDate(selectedSingleEvent);
    const daysElapsed = firstPurchase ? Math.max(1, (Date.now() - firstPurchase.getTime()) / 864e5) : 0;
    const daysRemaining = selectedEventDate ? Math.max(0, (selectedEventDate.getTime() - Date.now()) / 864e5) : 0;
    const projectedRevenue = selectedSingleEvent && daysElapsed > 0 && daysRemaining > 0
      ? grossRevenue + safeDivide(grossRevenue, daysElapsed) * daysRemaining
      : 0;

    const approvalRows = metricRows(byApprover, 12);
    const ticketApprovalRows = metricRows(byTicketApprover, 12);
    const topApproverCount = approvalRows[0]?.quantity ?? 0;
    const top3ApproverCount = approvalRows.slice(0, 3).reduce((sum, row) => sum + row.quantity, 0);
    const ticketTopApproverCount = ticketApprovalRows[0]?.quantity ?? 0;
    const ticketTop3ApproverCount = ticketApprovalRows.slice(0, 3).reduce((sum, row) => sum + row.quantity, 0);
    const approvalAverage = safeDivide(
      approvalDurations.reduce((sum, value) => sum + value, 0),
      approvalDurations.length
    );
    const ticketApprovalAverage = safeDivide(
      ticketApprovalDurations.reduce((sum, value) => sum + value, 0),
      ticketApprovalDurations.length
    );
    const withdrawalDurations = approvedOrders
      .map((order) => {
        const approval = orderApprovalDate(order);
        const withdrawal = orderWithdrawalDate(order);
        return approval && withdrawal ? (withdrawal.getTime() - approval.getTime()) / 36e5 : Number.NaN;
      })
      .filter((value) => Number.isFinite(value) && value >= 0);
    const productAuditHref = (options: StatementLinkOptions = {}) =>
      singleOperationalEventId ? eventHref(singleOperationalEventId, { type: "produto", ...options }) : "";

    const auditRows: TableRow[] = [
      { alerta: "Aprovado sem retirada", quantidade: approvedOrders.filter((order) => orderRedeemedQuantity(order) === 0).length, href: productAuditHref({ status: "aprovado", indicator: "pendente-retirada" }) },
      { alerta: "Retirado sem aprovação", quantidade: selectedData.orders.filter((order) => !isApprovedStatus(statusValue(order)) && orderRedeemedQuantity(order) > 0).length, href: productAuditHref({ alert: "uso-sem-aprovacao" }) },
      { alerta: "Baixa manual", quantidade: approvedOrders.filter((order) => orderWithdrawalMethod(order) === "Manual").length, href: productAuditHref({ status: "aprovado", indicator: "retirada-manual" }) },
      { alerta: "Pedido criado manualmente", quantidade: selectedData.orders.filter(isManualOrder).length, href: productAuditHref({ indicator: "pedido-manual" }) },
      { alerta: "Desconto 100%", quantidade: approvedOrders.filter((order) => orderDiscount(order) >= orderTotal(order) && orderTotal(order) > 0).length, href: productAuditHref({ status: "aprovado", search: "desconto" }) },
      { alerta: "Valor zerado", quantidade: approvedOrders.filter((order) => orderTotal(order) <= 0).length, href: productAuditHref({ status: "aprovado", indicator: "sem-valor" }) },
      { alerta: "Pedido cancelado após aprovação", quantidade: cancelledOrders.length, href: productAuditHref({ indicator: "cancelado-pos-aprovacao" }) },
      { alerta: "Produto sem estoque", quantidade: selectedData.products.filter((product) => parseNumber(product.estoque, 0) <= 0).length },
      { alerta: "Pedido aprovado por quem criou", quantidade: approvedOrders.filter((order) => isManualOrder(order) && orderApproverName(order) === asString(order.eventCreatedByName)).length, href: productAuditHref({ status: "aprovado", indicator: "mesmo-criador" }) },
      { alerta: "Pedido baixado por quem aprovou", quantidade: approvedOrders.filter((order) => orderWithdrawalOperator(order) !== "-" && orderWithdrawalOperator(order) === orderApproverName(order)).length, href: productAuditHref({ status: "aprovado", indicator: "mesmo-baixa" }) },
    ].filter((row) => Number(row.quantidade) > 0);

    const operationalAlerts: TableRow[] = [
      { alerta: "Pedido aprovado sem valor", quantidade: [...approvedTickets, ...approvedOrders].filter((row) => ("eventoId" in row ? ticketValue(row) : orderTotal(row)) <= 0).length },
      { alerta: "Pedido com desconto 100%", quantidade: approvedOrders.filter((order) => orderDiscount(order) >= orderTotal(order) && orderTotal(order) > 0).length },
      { alerta: "Pedido manual criado por administrador", quantidade: selectedData.orders.filter(isManualOrder).length },
      { alerta: "Pedido sem comprovante", quantidade: [...selectedData.tickets, ...selectedData.orders].filter((row) => !row.payment_config && !isManualOrder(row)).length },
      { alerta: "Pedido pendente há mais de 24h", quantidade: pendingAging.more24 },
      { alerta: "Pedido com retirada manual", quantidade: approvedOrders.filter((order) => orderWithdrawalMethod(order) === "Manual").length },
      { alerta: "Pedido com divergência de valor", quantidade: [...approvedTickets, ...approvedOrders].filter((row) => ("eventoId" in row ? ticketValue(row) : orderTotal(row)) < 0).length },
    ].filter((row) => Number(row.quantidade) > 0);

    const operationalTicketAlerts: TableRow[] = [
      { alerta: "Ingresso aprovado sem valor", quantidade: approvedTickets.filter((ticket) => ticketValue(ticket) <= 0).length },
      { alerta: "Ingresso sem comprovante", quantidade: selectedData.tickets.filter((ticket) => !ticket.payment_config).length },
      { alerta: "Ingresso pendente há mais de 24h", quantidade: ticketPendingAging.more24 },
      { alerta: "Ingresso com divergência de valor", quantidade: selectedData.tickets.filter((ticket) => ticketValue(ticket) < 0).length },
    ].filter((row) => Number(row.quantidade) > 0);

    const eventDecisionRows = metricRows(eventSummary, 20).map((row) => {
      const presenceRate = safeDivide(row.secondary ?? 0, row.quantity) * 100;
      const decision =
        row.value > 0 && presenceRate >= 70
          ? "Repetir"
          : row.value > 0 && presenceRate >= 40
            ? "Ajustar divulgação"
            : row.value > 0
              ? "Ajustar formato"
              : "Revisar proposta";
      return {
        evento: row.name,
        ingressos: row.quantity,
        receita: row.value,
        ticket: safeDivide(row.value, row.quantity),
        presença: presenceRate,
        decisão: decision,
      };
    });

    const productTableRows = Array.from(productRows.values())
      .sort((left, right) => right.value - left.value || right.quantity - left.quantity)
      .slice(0, 20)
      .map((row) => ({
        produto: row.name,
        itens: row.quantity,
        receita: row.value,
        ticket: safeDivide(row.value, row.quantity),
        retirados: row.redeemed,
        pendentes: row.pending,
      }));
    const productChartRows = Array.from(productRows.values()).sort((left, right) => right.value - left.value).slice(0, 12);

    const singleEventIdForLinks =
      eventFilter !== "todos"
        ? eventFilter
        : selectedData.events.length === 1
          ? eventId(selectedData.events[0])
          : "";
    const ticketBuyerIds = new Set(approvedTickets.map(ticketBuyerId).filter(Boolean));
    const productBuyerIds = new Set(approvedOrders.map(orderBuyerId).filter(Boolean));
    const productRedeemedBuyerIds = new Set(
      approvedOrders.filter((order) => orderRedeemedQuantity(order) > 0).map(orderBuyerId).filter(Boolean)
    );
    const checkedInApprovedTicketBuyerIds = checkedInTicketBuyerIds;
    const checkedInProductBuyerIds = new Set(
      Array.from(checkedInApprovedTicketBuyerIds).filter((buyerId) => productBuyerIds.has(buyerId))
    );
    const checkedInProductRedeemedBuyerIds = new Set(
      Array.from(checkedInProductBuyerIds).filter((buyerId) => productRedeemedBuyerIds.has(buyerId))
    );
    const buyersWithTicketAndProduct = Array.from(ticketBuyerIds).filter((buyerId) => productBuyerIds.has(buyerId)).length;
    const ticketWithoutProduct = Math.max(0, checkedInApprovedTicketBuyerIds.size - checkedInProductBuyerIds.size);
    const productWithoutTicket = Array.from(productBuyerIds).filter((buyerId) => !ticketBuyerIds.has(buyerId)).length;
    const productPresentBuyerIds = checkedInProductBuyerIds.size;
    const productPerPresent = safeDivide(approvedProductQuantity, ticketScanned);
    const productRevenuePerPresent = safeDivide(productRevenue, ticketScanned);
    const ticketRevenuePerPresent = safeDivide(ticketRevenue, ticketScanned);
    const totalRevenuePerPresent = safeDivide(grossRevenue, ticketScanned);
    const totalRevenuePerBuyer = safeDivide(grossRevenue, buyerPurchases.size);
    const productRevenueShare = safeDivide(productRevenue, grossRevenue) * 100;
    const ticketRevenueShare = safeDivide(ticketRevenue, grossRevenue) * 100;
    const eventCostsTotal = selectedData.events.reduce((sum, event) => sum + eventCost(event), 0);
    const hasEventCostsField = selectedData.events.some(hasEventCostField);

    type BuyerEventMarker = { eventId: string };
    const addBuyerEventMarker = (map: Map<string, BuyerEventMarker[]>, buyerId: string, targetEventId: string) => {
      const cleanBuyerId = buyerId.trim();
      const cleanEventId = targetEventId.trim();
      if (!cleanBuyerId || !cleanEventId) return;
      const current = map.get(cleanBuyerId) ?? [];
      if (!current.some((marker) => marker.eventId === cleanEventId)) current.push({ eventId: cleanEventId });
      map.set(cleanBuyerId, current);
    };
    const isPreviousEvent = (previousEventId: string, currentEventId: string): boolean => {
      if (!previousEventId || !currentEventId || previousEventId === currentEventId) return false;
      const previousAt = eventDate(eventById.get(previousEventId));
      const currentAt = eventDate(eventById.get(currentEventId));
      return Boolean(previousAt && currentAt && previousAt.getTime() < currentAt.getTime());
    };
    const hasHistoricalEvent = (map: Map<string, BuyerEventMarker[]>, buyerId: string, currentEventId: string) =>
      (map.get(buyerId) ?? []).some((marker) => isPreviousEvent(marker.eventId, currentEventId));
    const classifyHistoricalRecurrence = (
      currentMap: Map<string, BuyerEventMarker[]>,
      historicalMap: Map<string, BuyerEventMarker[]>
    ) => {
      let novos = 0;
      let recorrentes = 0;
      currentMap.forEach((currentMarkers, buyerId) => {
        const isRecurring = currentMarkers.some((currentMarker) => hasHistoricalEvent(historicalMap, buyerId, currentMarker.eventId));
        if (isRecurring) recorrentes += 1;
        else novos += 1;
      });
      return { novos, recorrentes };
    };
    const historicalTicketCheckinsByBuyer = new Map<string, BuyerEventMarker[]>();
    const historicalProductPurchasesByBuyer = new Map<string, BuyerEventMarker[]>();
    data.tickets.forEach((ticket) => {
      const targetEventId = ticketEventId(ticket);
      const relatedEvent = eventById.get(targetEventId);
      if (!targetEventId || !relatedEvent || !matchesActiveScope(ticket, relatedEvent)) return;
      if (!isApprovedStatus(statusValue(ticket)) || ticketScannedCount(ticket) <= 0) return;
      addBuyerEventMarker(historicalTicketCheckinsByBuyer, ticketBuyerId(ticket), targetEventId);
    });
    data.orders.forEach((order) => {
      const targetEventId = orderEventId(order) || productEventId(productsById.get(orderProductId(order)) ?? {});
      const relatedEvent = eventById.get(targetEventId);
      if (!targetEventId || !relatedEvent || !matchesActiveScope(order, relatedEvent)) return;
      if (!isApprovedStatus(statusValue(order))) return;
      addBuyerEventMarker(historicalProductPurchasesByBuyer, orderBuyerId(order), targetEventId);
    });
    const currentTicketCheckinsByBuyer = new Map<string, BuyerEventMarker[]>();
    const currentProductPurchasesByBuyer = new Map<string, BuyerEventMarker[]>();
    approvedTickets.forEach((ticket) => {
      if (ticketScannedCount(ticket) <= 0) return;
      addBuyerEventMarker(currentTicketCheckinsByBuyer, ticketBuyerId(ticket), ticketEventId(ticket));
    });
    approvedOrders.forEach((order) => {
      const targetEventId = orderEventId(order) || productEventId(productsById.get(orderProductId(order)) ?? {});
      addBuyerEventMarker(currentProductPurchasesByBuyer, orderBuyerId(order), targetEventId);
    });
    const ticketHistoricalRecurrence = classifyHistoricalRecurrence(currentTicketCheckinsByBuyer, historicalTicketCheckinsByBuyer);
    const productHistoricalRecurrence = classifyHistoricalRecurrence(currentProductPurchasesByBuyer, historicalProductPurchasesByBuyer);
    const recurrenceDetailRows: MetricRow[] = [
      { name: "Novo em ingresso", quantity: ticketHistoricalRecurrence.novos, value: 0 },
      { name: "Recorrente em ingresso", quantity: ticketHistoricalRecurrence.recorrentes, value: 0 },
      { name: "Novo em produto", quantity: productHistoricalRecurrence.novos, value: 0 },
      { name: "Recorrente em produto", quantity: productHistoricalRecurrence.recorrentes, value: 0 },
    ];
    const strategicRecurrenceBaseBuyerIds = new Set([
      ...Array.from(currentTicketCheckinsByBuyer.keys()),
      ...Array.from(currentProductPurchasesByBuyer.keys()),
    ]);
    const strategicRecurringBuyerIds = new Set<string>();
    currentTicketCheckinsByBuyer.forEach((currentMarkers, buyerId) => {
      if (currentMarkers.some((marker) => hasHistoricalEvent(historicalTicketCheckinsByBuyer, buyerId, marker.eventId))) {
        strategicRecurringBuyerIds.add(buyerId);
      }
    });
    currentProductPurchasesByBuyer.forEach((currentMarkers, buyerId) => {
      if (currentMarkers.some((marker) => hasHistoricalEvent(historicalProductPurchasesByBuyer, buyerId, marker.eventId))) {
        strategicRecurringBuyerIds.add(buyerId);
      }
    });
    const strategicRecurringBuyers = strategicRecurringBuyerIds.size;
    const strategicRecurringRate = safeDivide(strategicRecurringBuyers, strategicRecurrenceBaseBuyerIds.size) * 100;
    const tenantUserCount = new Set(data.users.map((user) => asString(user.uid).trim()).filter(Boolean)).size || data.users.length;
    const tenantParticipationRate = safeDivide(checkedInApprovedTicketBuyerIds.size, tenantUserCount) * 100;
    const tenantParticipationRows: MetricRow[] = [
      { name: "Participaram", quantity: checkedInApprovedTicketBuyerIds.size, value: tenantParticipationRate },
      { name: "Não participaram", quantity: Math.max(0, tenantUserCount - checkedInApprovedTicketBuyerIds.size), value: Math.max(0, 100 - tenantParticipationRate) },
    ];

    type EventStrategicAccumulator = {
      eventId: string;
      evento: string;
      href: string;
      ticketQty: number;
      present: number;
      ticketRevenue: number;
      productRevenue: number;
      productQty: number;
      productRedeemed: number;
      productPending: number;
      ticketBuyers: Set<string>;
      checkedInTicketBuyers: Set<string>;
      productBuyers: Set<string>;
      productRedeemedBuyers: Set<string>;
      buyers: Set<string>;
      recurringBuyers: Set<string>;
      manualCount: number;
      operationalCount: number;
      cost: number;
    };
    const eventStrategicMap = new Map<string, EventStrategicAccumulator>();
    const ensureStrategicEvent = (targetEventId: string): EventStrategicAccumulator => {
      const cleanEventId = targetEventId.trim();
      const relatedEvent = eventById.get(cleanEventId);
      const current =
        eventStrategicMap.get(cleanEventId) ??
        {
          eventId: cleanEventId,
          evento: relatedEvent ? eventName(relatedEvent) : cleanEventId || "Evento",
          href: cleanEventId ? eventHref(cleanEventId, { status: "aprovado" }) : "",
          ticketQty: 0,
          present: 0,
          ticketRevenue: 0,
          productRevenue: 0,
          productQty: 0,
          productRedeemed: 0,
          productPending: 0,
          ticketBuyers: new Set<string>(),
          checkedInTicketBuyers: new Set<string>(),
          productBuyers: new Set<string>(),
          productRedeemedBuyers: new Set<string>(),
          buyers: new Set<string>(),
          recurringBuyers: new Set<string>(),
          manualCount: 0,
          operationalCount: 0,
          cost: eventCost(relatedEvent),
        };
      eventStrategicMap.set(cleanEventId, current);
      return current;
    };

    selectedData.events.forEach((event) => ensureStrategicEvent(eventId(event)));
    approvedTickets.forEach((ticket) => {
      const targetEventId = ticketEventId(ticket);
      const row = ensureStrategicEvent(targetEventId);
      const buyerId = ticketBuyerId(ticket);
      row.ticketQty += ticketQuantity(ticket);
      row.present += ticketScannedCount(ticket);
      row.ticketRevenue += ticketValue(ticket);
      row.ticketBuyers.add(buyerId);
      if (ticketScannedCount(ticket) > 0) {
        row.checkedInTicketBuyers.add(buyerId);
        if (hasHistoricalEvent(historicalTicketCheckinsByBuyer, buyerId, targetEventId)) row.recurringBuyers.add(buyerId);
      }
      row.buyers.add(buyerId);
    });
    approvedOrders.forEach((order) => {
      const targetEventId = orderEventId(order) || productEventId(productsById.get(orderProductId(order)) ?? {});
      if (!targetEventId) return;
      const row = ensureStrategicEvent(targetEventId);
      const buyerId = orderBuyerId(order);
      const quantity = orderQuantity(order);
      const redeemed = orderRedeemedQuantity(order);
      row.productQty += quantity;
      row.productRedeemed += redeemed;
      row.productPending += Math.max(0, quantity - redeemed);
      row.productRevenue += orderTotal(order);
      row.productBuyers.add(buyerId);
      if (redeemed > 0) row.productRedeemedBuyers.add(buyerId);
      row.buyers.add(buyerId);
      if (hasHistoricalEvent(historicalProductPurchasesByBuyer, buyerId, targetEventId)) row.recurringBuyers.add(buyerId);
    });
    operationalRecords.forEach((record) => {
      if (!record.eventId) return;
      const row = ensureStrategicEvent(record.eventId);
      row.operationalCount += 1;
      if (record.manual || normalizeText(`${record.source} ${record.approvalMethod} ${record.completionMethod}`).includes("manual")) {
        row.manualCount += 1;
      }
    });

    const strategicEventBaseRows = Array.from(eventStrategicMap.values()).filter(
      (row) => row.ticketQty > 0 || row.productQty > 0 || row.ticketRevenue > 0 || row.productRevenue > 0
    );
    const maxEventRevenue = maxValue(strategicEventBaseRows.map((row) => row.ticketRevenue + row.productRevenue));
    const maxEventPresence = maxValue(strategicEventBaseRows.map((row) => row.present));
    const maxProductPerPresent = maxValue(
      strategicEventBaseRows.map((row) => safeDivide(row.productRevenue, row.present))
    );
    const strategicEventRows = strategicEventBaseRows
      .map((row) => {
        const totalRevenue = row.ticketRevenue + row.productRevenue;
        const presenceRate = safeDivide(row.present, row.ticketQty) * 100;
        const productConversion = safeDivide(
          Array.from(row.checkedInTicketBuyers).filter((buyerId) => row.productBuyers.has(buyerId)).length,
          row.checkedInTicketBuyers.size
        ) * 100;
        const productPerPerson = safeDivide(row.productRevenue, row.present);
        const withdrawalRateByEvent = safeDivide(row.productRedeemed, row.productQty) * 100;
        const pendingRate = safeDivide(row.productPending, row.productQty) * 100;
        const manualRate = safeDivide(row.manualCount, row.operationalCount) * 100;
        const recurringRateByEvent = safeDivide(row.recurringBuyers.size, row.buyers.size) * 100;
        const score = Math.round(
          scoreFromRatio(totalRevenue, maxEventRevenue) * 0.2 +
            scoreFromRatio(row.present, maxEventPresence) * 0.15 +
            clamp(presenceRate) * 0.15 +
            scoreFromRatio(productPerPerson, maxProductPerPresent) * 0.15 +
            clamp(productConversion) * 0.15 +
            clamp(recurringRateByEvent) * 0.08 +
            scoreFromInverseRate(manualRate) * 0.06 +
            scoreFromInverseRate(pendingRate) * 0.06
        );
        const decision =
          score >= 85
            ? "Repetir e escalar"
            : score >= 70
              ? "Repetir"
              : presenceRate < 45 && totalRevenue > 0
                ? "Ajustar divulgação"
                : productConversion < 25 && row.present > 0
                  ? "Ajustar produtos"
                  : pendingRate > 30
                    ? "Ajustar portaria"
                    : score < 40
                      ? "Evitar repetir"
                      : "Ajustar formato";
        const reason =
          productConversion < 25 && row.present > 0
            ? "Presença não virou consumo"
            : pendingRate > 30
              ? "Muita retirada pendente"
              : presenceRate < 45
                ? "No-show alto"
                : productPerPerson > safeDivide(maxProductPerPresent, 2)
                  ? "Consumo interno forte"
                  : "Equilíbrio geral";
        return {
          evento: row.evento,
          ingressos: row.ticketQty,
          checkins: row.present,
          presentes: row.present,
          presenca: presenceRate,
          receitaIngressos: row.ticketRevenue,
          receitaProdutos: row.productRevenue,
          receitaTotal: totalRevenue,
          produtoPorPresente: productPerPerson,
          ticketTotalCliente: safeDivide(totalRevenue, row.buyers.size),
          retirada: withdrawalRateByEvent,
          pendencias: row.productPending,
          score,
          decisao: decision,
          motivo: reason,
          href: row.href,
          hrefProdutos: eventHref(row.eventId, { type: "produto", status: "aprovado" }),
        };
      })
      .sort((left, right) => Number(right.score) - Number(left.score) || Number(right.receitaTotal) - Number(left.receitaTotal));
    const hasStrategicScoreBasis =
      strategicEventRows.length > 0 && (approvedTicketQuantity > 0 || approvedProductQuantity > 0 || grossRevenue > 0);
    const strategicScore = hasStrategicScoreBasis
      ? Math.round(
          safeDivide(strategicEventRows.reduce((sum, row) => sum + Number(row.score), 0), strategicEventRows.length)
        )
      : null;
    const strategicDecision = strategicScore === null
      ? "Sem dados suficientes"
      : strategicScore >= 85 ? "Repetir e escalar" : strategicScore >= 70 ? "Repetir" : strategicScore >= 40 ? "Ajustar" : "Repensar";
    const strategicRadarRows: MetricRow[] = [
      { name: "Ingresso", quantity: approvedTicketQuantity, value: scoreFromRatio(ticketRevenue, Math.max(grossRevenue, 1)) },
      { name: "Check-in", quantity: ticketScanned, value: clamp(safeDivide(ticketScanned, approvedTicketQuantity) * 100) },
      { name: "Produtos", quantity: approvedProductQuantity, value: scoreFromRatio(productRevenue, Math.max(grossRevenue, 1)) },
      { name: "Recorrência", quantity: strategicRecurringBuyers, value: clamp(strategicRecurringRate) },
      { name: "Operação", quantity: approvedOperationalRecords.length, value: scoreFromInverseRate(safeDivide(operationalAlertRows.length, Math.max(approvedOperationalRecords.length, 1)) * 100) },
      { name: "Auditoria", quantity: operationalAlertRows.length, value: scoreFromInverseRate((duplicateScans + invalidScans + operationalAlertRows.length) * 10, 100) },
    ];
    const strategicBubbleRows: BubbleEntry[] = strategicEventRows.map((row) => ({
      name: asString(row.evento),
      x: Number(row.presenca),
      y: Number(row.receitaTotal),
      z: Math.max(1, Number(row.receitaProdutos)),
      value: Number(row.score),
      decision: asString(row.decisao),
      href: asString(row.href),
    }));
    const revenueOriginRows: MetricRow[] = [
      {
        name: "Ingressos",
        quantity: ticketRevenue,
        value: ticketRevenue,
        href: singleEventIdForLinks ? eventHref(singleEventIdForLinks, { type: "ingresso", status: "aprovado" }) : "",
      },
      {
        name: "Produtos (Modo Vendas)",
        quantity: productRevenue,
        value: productRevenue,
        href: singleEventIdForLinks ? eventHref(singleEventIdForLinks, { type: "produto", status: "aprovado" }) : "",
      },
    ];
    const revenueDetailRows: MetricRow[] = [
      ...metricRows(byLot, 6).map((row) => ({ ...row, name: `Lote: ${row.name}`, quantity: row.value, href: row.href || "" })),
      ...productChartRows.slice(0, 6).map((row) => ({ ...row, name: `Produto: ${row.name}`, quantity: row.value })),
    ].sort((left, right) => right.quantity - left.quantity).slice(0, 12);
    const revenuePerPresentRows: MetricRow[] = strategicEventRows.map((row) => ({
      name: asString(row.evento),
      quantity: Number(row.presentes),
      value: Number(row.receitaTotal) ? safeDivide(Number(row.receitaTotal), Number(row.presentes)) : 0,
      secondary: safeDivide(Number(row.receitaProdutos), Number(row.presentes)),
      href: asString(row.href),
    }));
    const strategicFunnelRows: MetricRow[] = [
      { name: "Usuários com ingresso aprovado", quantity: ticketBuyerIds.size, value: 0, href: singleEventIdForLinks ? eventHref(singleEventIdForLinks, { type: "ingresso", status: "aprovado" }) : "" },
      { name: "Desses, usuários com check-in", quantity: checkedInApprovedTicketBuyerIds.size, value: 0, href: singleEventIdForLinks ? checkinsHref(singleEventIdForLinks) : "" },
      { name: "Desses, compraram produto", quantity: checkedInProductBuyerIds.size, value: 0, href: singleEventIdForLinks ? eventHref(singleEventIdForLinks, { type: "produto", status: "aprovado" }) : "" },
      { name: "Desses, retiraram produto", quantity: checkedInProductRedeemedBuyerIds.size, value: 0, href: singleEventIdForLinks ? eventHref(singleEventIdForLinks, { type: "produto", status: "aprovado", flow: "retirada" }) : "" },
    ];
    const attachRateRows: MetricRow[] = Array.from(productRows.values())
      .map((row) => ({
        name: row.name,
        quantity: row.quantity,
        value: safeDivide(row.value, Math.max(ticketScanned, 1)),
        secondary: safeDivide(row.quantity, Math.max(ticketScanned, 1)),
        href: singleEventIdForLinks ? eventHref(singleEventIdForLinks, { type: "produto", status: "aprovado", search: row.name }) : "",
      }))
      .sort((left, right) => right.quantity - left.quantity || right.value - left.value);
    const eventProductHeatmapRows: HeatmapEntry[] = [];
    const eventCategoryHeatmapRows: HeatmapEntry[] = [];
    const categoryCompositionRows = new Map<string, TableRow>();
    const addComposition = (eventLabel: string, key: string, value: number, href: string) => {
      const current = categoryCompositionRows.get(eventLabel) ?? { name: eventLabel, ingresso: 0, produto: 0, ficha: 0, bar: 0, cortesia: 0, outros: 0, href };
      const normalized = normalizeText(key);
      const field = normalized.includes("ingresso")
        ? "ingresso"
        : normalized.includes("ficha")
          ? "ficha"
          : normalized.includes("bar") || normalized.includes("drink") || normalized.includes("bebida")
            ? "bar"
            : normalized.includes("cortesia")
              ? "cortesia"
              : normalized.includes("produto")
                ? "produto"
                : "outros";
      current[field] = parseNumber(current[field], 0) + value;
      categoryCompositionRows.set(eventLabel, current);
    };
    approvedTickets.forEach((ticket) => {
      const targetEventId = ticketEventId(ticket);
      const relatedEvent = eventById.get(targetEventId);
      const eventLabel = relatedEvent ? eventName(relatedEvent) : asString(ticket.eventoNome, "Evento");
      addComposition(eventLabel, "Ingresso", ticketValue(ticket), eventHref(targetEventId, { type: "ingresso", status: "aprovado" }));
    });
    approvedOrders.forEach((order) => {
      const targetEventId = orderEventId(order) || productEventId(productsById.get(orderProductId(order)) ?? {});
      const relatedEvent = eventById.get(targetEventId);
      const eventLabel = relatedEvent ? eventName(relatedEvent) : asString(order.eventoNome, "Evento");
      const productLabel = orderItemName(order, productsById);
      const category = orderItemCategory(order, productsById);
      eventProductHeatmapRows.push({
        row: eventLabel,
        column: productLabel,
        value: orderTotal(order),
        href: targetEventId ? eventHref(targetEventId, { type: "produto", status: "aprovado", search: productLabel }) : "",
      });
      eventCategoryHeatmapRows.push({
        row: eventLabel,
        column: category,
        value: orderTotal(order),
        href: targetEventId ? eventHref(targetEventId, { type: "produto", status: "aprovado", search: category }) : "",
      });
      addComposition(eventLabel, category, orderTotal(order), targetEventId ? eventHref(targetEventId, { type: "produto", status: "aprovado" }) : "");
    });
    const categoryCompositionChartRows = Array.from(categoryCompositionRows.values()).slice(0, 12);

    const leadBucketNames = ["30 dias ou mais", "15 a 29 dias", "7 a 14 dias", "3 a 6 dias", "24 a 72h", "Menos de 24h", "Sem data"];
    const createLeadMap = () =>
      new Map<string, MetricRow>(
        leadBucketNames.map((name, index) => [name, { name, quantity: 0, value: 0, sortValue: index }])
      );
    const ticketLeadMap = createLeadMap();
    const productLeadMap = createLeadMap();
    const addLeadRow = (map: Map<string, MetricRow>, bucket: string, quantity: number, value: number, href = "") => {
      const fallbackSort = leadBucketNames.indexOf(bucket);
      const current = map.get(bucket) ?? { name: bucket, quantity: 0, value: 0, sortValue: fallbackSort >= 0 ? fallbackSort : leadBucketNames.length, href };
      current.quantity += quantity;
      current.value += value;
      if (!current.href && href) current.href = href;
      map.set(bucket, current);
    };
    approvedTickets.forEach((ticket) => {
      const targetEventId = ticketEventId(ticket);
      addLeadRow(
        ticketLeadMap,
        leadBucketLabel(ticketPurchaseDate(ticket), eventDate(eventById.get(targetEventId))),
        ticketQuantity(ticket),
        ticketValue(ticket),
        eventHref(targetEventId, { type: "ingresso", status: "aprovado" })
      );
    });
    approvedOrders.forEach((order) => {
      const targetEventId = orderEventId(order) || productEventId(productsById.get(orderProductId(order)) ?? {});
      addLeadRow(
        productLeadMap,
        leadBucketLabel(orderCreatedAt(order), eventDate(eventById.get(targetEventId))),
        orderQuantity(order),
        orderTotal(order),
        eventHref(targetEventId, { type: "produto", status: "aprovado" })
      );
    });
    const leadRowsFromMap = (map: Map<string, MetricRow>): MetricRow[] =>
      Array.from(map.values()).sort((left, right) => Number(left.sortValue ?? 0) - Number(right.sortValue ?? 0));
    const ticketLeadRows = leadRowsFromMap(ticketLeadMap);
    const productLeadRows = leadRowsFromMap(productLeadMap);
    type CustomerValueAccumulator = { name: string; event: string; total: number; ticket: number; product: number; items: number; href: string };
    const customerValueMap = new Map<string, CustomerValueAccumulator>();
    const customerByEventMap = new Map<string, Map<string, CustomerValueAccumulator>>();
    const ensureCustomer = (buyerId: string, name: string, href = "") => {
      const current = customerValueMap.get(buyerId) ?? { name: name || buyerId || "Cliente", event: "Todos", total: 0, ticket: 0, product: 0, items: 0, href };
      if (!current.href && href) current.href = href;
      customerValueMap.set(buyerId, current);
      return current;
    };
    const ensureEventCustomer = (targetEventId: string, buyerId: string, name: string, href = "") => {
      const cleanEventId = targetEventId.trim() || "sem-evento";
      const relatedEvent = eventById.get(cleanEventId);
      const eventLabel = relatedEvent ? eventName(relatedEvent) : cleanEventId === "sem-evento" ? "Sem evento vinculado" : cleanEventId;
      const eventMap = customerByEventMap.get(cleanEventId) ?? new Map<string, CustomerValueAccumulator>();
      const current = eventMap.get(buyerId) ?? { name: name || buyerId || "Cliente", event: eventLabel, total: 0, ticket: 0, product: 0, items: 0, href };
      if (!current.href && href) current.href = href;
      eventMap.set(buyerId, current);
      customerByEventMap.set(cleanEventId, eventMap);
      return current;
    };
    approvedTickets.forEach((ticket) => {
      const buyerId = ticketBuyerId(ticket);
      const buyerName = asString(ticket.userName || buyerId);
      const targetEventId = ticketEventId(ticket);
      const href = eventHref(targetEventId, { type: "ingresso", status: "aprovado", search: asString(ticket.userName) });
      const customer = ensureCustomer(buyerId, buyerName, href);
      customer.total += ticketValue(ticket);
      customer.ticket += ticketValue(ticket);
      customer.items += ticketQuantity(ticket);
      const eventCustomer = ensureEventCustomer(targetEventId, buyerId, buyerName, href);
      eventCustomer.total += ticketValue(ticket);
      eventCustomer.ticket += ticketValue(ticket);
      eventCustomer.items += ticketQuantity(ticket);
    });
    approvedOrders.forEach((order) => {
      const targetEventId = orderEventId(order) || productEventId(productsById.get(orderProductId(order)) ?? {});
      const buyerId = orderBuyerId(order);
      const buyerName = asString(order.userName || buyerId);
      const href = targetEventId ? eventHref(targetEventId, { type: "produto", status: "aprovado", search: asString(order.userName) }) : "";
      const customer = ensureCustomer(buyerId, buyerName, href);
      customer.total += orderTotal(order);
      customer.product += orderTotal(order);
      customer.items += orderQuantity(order);
      const eventCustomer = ensureEventCustomer(targetEventId, buyerId, buyerName, href);
      eventCustomer.total += orderTotal(order);
      eventCustomer.product += orderTotal(order);
      eventCustomer.items += orderQuantity(order);
    });
    const customerTicketHistogramRows = Array.from(customerValueMap.values()).reduce((map, customer) => {
      addMetric(map, ticketBucket(customer.total), 1, customer.total);
      return map;
    }, new Map<string, MetricRow>());
    const topCustomerRows = Array.from(customerValueMap.values())
      .sort((left, right) => right.total - left.total)
      .slice(0, 15)
      .map((customer) => ({
        cliente: customer.name,
        receitaTotal: customer.total,
        ingresso: customer.ticket,
        produto: customer.product,
        itens: customer.items,
        href: customer.href,
      }));
    const topCustomersByEventRows = Array.from(customerByEventMap.values())
      .flatMap((eventCustomers) =>
        Array.from(eventCustomers.values())
          .sort((left, right) => right.total - left.total)
          .slice(0, 5)
          .map((customer, index) => ({
            evento: customer.event,
            posicao: index + 1,
            cliente: customer.name,
            receitaTotal: customer.total,
            ingresso: customer.ticket,
            produto: customer.product,
            itens: customer.items,
            href: customer.href,
          }))
      )
      .sort((left, right) => asString(left.evento).localeCompare(asString(right.evento), "pt-BR") || Number(left.posicao) - Number(right.posicao));
    const classConsumptionMap = new Map<string, { present: number; productRevenue: number; productQty: number; noShow: number; href: string }>();
    const lotConsumptionMap = new Map<string, { present: number; productRevenue: number; productQty: number; noShow: number; href: string }>();
    const addConsumption = (map: Map<string, { present: number; productRevenue: number; productQty: number; noShow: number; href: string }>, key: string, patch: Partial<{ present: number; productRevenue: number; productQty: number; noShow: number; href: string }>) => {
      const current = map.get(key) ?? { present: 0, productRevenue: 0, productQty: 0, noShow: 0, href: patch.href || "" };
      current.present += patch.present ?? 0;
      current.productRevenue += patch.productRevenue ?? 0;
      current.productQty += patch.productQty ?? 0;
      current.noShow += patch.noShow ?? 0;
      if (!current.href && patch.href) current.href = patch.href;
      map.set(key, current);
    };
    approvedTickets.forEach((ticket) => {
      const present = ticketScannedCount(ticket);
      const noShowCount = Math.max(0, ticketQuantity(ticket) - present);
      const href = eventHref(ticketEventId(ticket), { type: "ingresso", status: "aprovado", search: ticketHolderName(ticket) });
      addConsumption(classConsumptionMap, ticketClassName(ticket), { present, noShow: noShowCount, href });
      addConsumption(lotConsumptionMap, ticketLotName(ticket), { present, noShow: noShowCount, href });
    });
    approvedOrders.forEach((order) => {
      const targetEventId = orderEventId(order) || productEventId(productsById.get(orderProductId(order)) ?? {});
      const href = targetEventId ? eventHref(targetEventId, { type: "produto", status: "aprovado", search: orderItemName(order, productsById) }) : "";
      addConsumption(classConsumptionMap, orderClassName(order, userById), { productRevenue: orderTotal(order), productQty: orderQuantity(order), href });
      addConsumption(lotConsumptionMap, asString(order.eventLoteNome || orderItemCategory(order, productsById) || "Sem lote"), { productRevenue: orderTotal(order), productQty: orderQuantity(order), href });
    });
    const consumptionRowsFromMap = (map: Map<string, { present: number; productRevenue: number; productQty: number; noShow: number; href: string }>): MetricRow[] =>
      Array.from(map.entries())
        .map(([name, row]) => ({
          name,
          quantity: row.present,
          value: safeDivide(row.productRevenue, row.present),
          secondary: row.noShow,
          href: row.href,
        }))
        .sort((left, right) => right.value - left.value || right.quantity - left.quantity)
        .slice(0, 12);
    const sourceResultMap = new Map<string, { revenue: number; present: number; redeemed: number; sold: number; manual: number; total: number; href: string }>();
    const addSourceResult = (source: string, patch: Partial<{ revenue: number; present: number; redeemed: number; sold: number; manual: number; total: number; href: string }>) => {
      const key = source.trim() || "Sem fonte";
      const current = sourceResultMap.get(key) ?? { revenue: 0, present: 0, redeemed: 0, sold: 0, manual: 0, total: 0, href: patch.href || "" };
      current.revenue += patch.revenue ?? 0;
      current.present += patch.present ?? 0;
      current.redeemed += patch.redeemed ?? 0;
      current.sold += patch.sold ?? 0;
      current.manual += patch.manual ?? 0;
      current.total += patch.total ?? 0;
      if (!current.href && patch.href) current.href = patch.href;
      sourceResultMap.set(key, current);
    };
    approvedTickets.forEach((ticket) =>
      addSourceResult(ticketSource(ticket), {
        revenue: ticketValue(ticket),
        present: ticketScannedCount(ticket),
        sold: ticketQuantity(ticket),
        manual: isManualTicket(ticket) ? 1 : 0,
        total: 1,
        href: eventHref(ticketEventId(ticket), { type: "ingresso", status: "aprovado", source: ticketSource(ticket) }),
      })
    );
    approvedOrders.forEach((order) => {
      const targetEventId = orderEventId(order) || productEventId(productsById.get(orderProductId(order)) ?? {});
      addSourceResult(orderSource(order), {
        revenue: orderTotal(order),
        redeemed: orderRedeemedQuantity(order),
        sold: orderQuantity(order),
        manual: isManualOrder(order) || orderWithdrawalMethod(order) === "Manual" ? 1 : 0,
        total: 1,
        href: targetEventId ? eventHref(targetEventId, { type: "produto", status: "aprovado", source: orderSource(order) }) : "",
      });
    });
    const sourceTreemapRows: MetricRow[] = Array.from(sourceResultMap.entries()).map(([name, row]) => ({
      name,
      quantity: row.sold,
      value: row.revenue,
      secondary: safeDivide(row.present + row.redeemed, row.sold) * 100,
      href: row.href,
    }));
    const withDiscount = [...approvedTickets, ...approvedOrders].filter((row) => ("eventoId" in row ? ticketDiscount(row) : orderDiscount(row)) > 0);
    const withoutDiscount = [...approvedTickets, ...approvedOrders].filter((row) => ("eventoId" in row ? ticketDiscount(row) : orderDiscount(row)) <= 0);
    const revenueOfMixed = (rows: Row[]) => rows.reduce((sum, row) => sum + ("eventoId" in row ? ticketValue(row) : orderTotal(row)), 0);
    const quantityOfMixed = (rows: Row[]) => rows.reduce((sum, row) => sum + ("eventoId" in row ? ticketQuantity(row) : orderQuantity(row)), 0);
    const discountImpactRows: MetricRow[] = [
      { name: "Com desconto", quantity: quantityOfMixed(withDiscount), value: revenueOfMixed(withDiscount), secondary: withDiscount.length },
      { name: "Sem desconto", quantity: quantityOfMixed(withoutDiscount), value: revenueOfMixed(withoutDiscount), secondary: withoutDiscount.length },
    ];
    const priceStrategyRows: BubbleEntry[] = [
      ...metricRows(byPrice, 10).map((row) => ({
        name: row.name,
        x: parseNumber(row.name, 0),
        y: safeDivide(row.secondary ?? row.quantity, row.quantity) * 100,
        z: Math.max(row.value, 1),
        value: scoreFromRatio(row.value, grossRevenue),
      })),
      ...productChartRows.slice(0, 10).map((row) => ({
        name: row.name,
        x: safeDivide(row.value, row.quantity),
        y: safeDivide(row.redeemed, row.quantity) * 100,
        z: Math.max(row.value, 1),
        value: scoreFromRatio(row.value, grossRevenue),
      })),
    ];
    const dailyRevenueRows = Array.from(
      [...approvedTickets, ...approvedOrders].reduce((map, row) => {
        const isTicket = "eventoId" in row;
        const date = isTicket ? ticketPurchaseDate(row) : orderCreatedAt(row);
        const day = dateKey(date);
        const daySortValue = date
          ? new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()
          : Number.MAX_SAFE_INTEGER;
        const current = map.get(day) ?? { name: day, quantity: 0, value: 0, secondary: 0, sortValue: daySortValue };
        current.quantity += isTicket ? ticketQuantity(row) : orderQuantity(row);
        current.value += isTicket ? ticketValue(row) : orderTotal(row);
        current.sortValue = Math.min(Number(current.sortValue ?? daySortValue), daySortValue);
        map.set(day, current);
        return map;
      }, new Map<string, MetricRow>()).values()
    ).sort((left, right) => Number(left.sortValue ?? 0) - Number(right.sortValue ?? 0));
    let cumulativeRevenue = 0;
    const forecastRows = dailyRevenueRows.map((row, index) => {
      cumulativeRevenue += row.value;
      const projected = projectedRevenue && dailyRevenueRows.length > 0
        ? cumulativeRevenue + safeDivide(projectedRevenue - grossRevenue, Math.max(1, dailyRevenueRows.length - index))
        : cumulativeRevenue;
      return { ...row, value: cumulativeRevenue, secondary: Math.max(projected, cumulativeRevenue) };
    });
    const resultWaterfallRows: MetricRow[] = [
      { name: "Ingressos", quantity: 1, value: ticketRevenue },
      { name: "Produtos", quantity: 1, value: productRevenue },
      { name: "Descontos", quantity: 1, value: -(ticketDiscounts + productDiscounts) },
      { name: "Custos", quantity: 1, value: -eventCostsTotal },
      { name: "Resultado", quantity: 1, value: Math.max(0, grossRevenue - ticketDiscounts - productDiscounts - eventCostsTotal) },
    ];
    const breakEvenTickets = eventCostsTotal > 0 ? Math.ceil(safeDivide(eventCostsTotal, Math.max(safeDivide(ticketRevenue, approvedTicketQuantity), 1))) : 0;

    const pendingProductOrders = approvedOrders.filter((order) => orderRedeemedQuantity(order) < orderQuantity(order));
    const partialProductOrders = approvedOrders.filter((order) => orderRedeemedQuantity(order) > 0 && orderRedeemedQuantity(order) < orderQuantity(order));
    const pendingWaitHoursProducts = pendingProductOrders
      .map((order) => {
        const started = orderApprovalDate(order) || orderCreatedAt(order);
        return started ? (Date.now() - started.getTime()) / 36e5 : 0;
      })
      .filter(Number.isFinite);
    const oldestPendingOrder = pendingProductOrders
      .map((order) => ({ order, date: orderApprovalDate(order) || orderCreatedAt(order) }))
      .filter((entry): entry is { order: Row; date: Date } => Boolean(entry.date))
      .sort((left, right) => left.date.getTime() - right.date.getTime())[0];
    const pendingRedeemAgingRows = [
      { name: "Menos de 15 min", min: 0, max: 0.25 },
      { name: "15–60 min", min: 0.25, max: 1 },
      { name: "1–3h", min: 1, max: 3 },
      { name: "3–12h", min: 3, max: 12 },
      { name: "Mais de 12h", min: 12, max: 24 },
      { name: "Mais de 24h", min: 24, max: Number.POSITIVE_INFINITY },
    ].map((bucket) => {
      const orders = pendingProductOrders.filter((order) => {
        const started = orderApprovalDate(order) || orderCreatedAt(order);
        const hours = started ? (Date.now() - started.getTime()) / 36e5 : 0;
        return hours >= bucket.min && hours < bucket.max;
      });
      return {
        name: bucket.name,
        quantity: orders.reduce((sum, order) => sum + Math.max(0, orderQuantity(order) - orderRedeemedQuantity(order)), 0),
        value: orders.reduce((sum, order) => sum + orderTotal(order) * safeDivide(Math.max(0, orderQuantity(order) - orderRedeemedQuantity(order)), orderQuantity(order)), 0),
        href: orders[0] ? eventHref(orderEventId(orders[0]) || productEventId(productsById.get(orderProductId(orders[0])) ?? {}), { type: "produto", status: "aprovado", indicator: "pendente-retirada" }) : "",
      };
    });
    const withdrawalStatusRows: MetricRow[] = [
      { name: "Retirado", quantity: redeemedItems, value: redeemedValue },
      { name: "Pendente", quantity: pendingRedeemItems, value: pendingRedeemValue },
      { name: "Retirada parcial", quantity: partialProductOrders.length, value: partialProductOrders.reduce((sum, order) => sum + orderTotal(order), 0) },
      { name: "Cancelado/estornado", quantity: cancelledOrders.length + refundedOrders.length, value: cancelledOrders.concat(refundedOrders).reduce((sum, order) => sum + orderTotal(order), 0) },
    ];
    const salesWithdrawalTimelineMap = new Map<string, MetricRow>();
    approvedOrders.forEach((order) => {
      const soldKey = hourLabel(orderApprovalDate(order) || orderCreatedAt(order));
      const sold = salesWithdrawalTimelineMap.get(soldKey) ?? { name: soldKey, quantity: 0, value: 0, secondary: 0, sortValue: hourSortValue(soldKey) };
      sold.quantity += orderQuantity(order);
      sold.value += orderTotal(order);
      sold.sortValue = Math.min(Number(sold.sortValue ?? hourSortValue(soldKey)), hourSortValue(soldKey));
      salesWithdrawalTimelineMap.set(soldKey, sold);
      const withdrawalKey = hourLabel(orderWithdrawalDate(order));
      if (withdrawalKey !== "Sem horário") {
        const withdrawn = salesWithdrawalTimelineMap.get(withdrawalKey) ?? { name: withdrawalKey, quantity: 0, value: 0, secondary: 0, sortValue: hourSortValue(withdrawalKey) };
        withdrawn.secondary = (withdrawn.secondary ?? 0) + orderRedeemedQuantity(order);
        withdrawn.sortValue = Math.min(Number(withdrawn.sortValue ?? hourSortValue(withdrawalKey)), hourSortValue(withdrawalKey));
        salesWithdrawalTimelineMap.set(withdrawalKey, withdrawn);
      }
    });
    const salesWithdrawalTimelineRows = Array.from(salesWithdrawalTimelineMap.values())
      .sort((left, right) => Number(left.sortValue ?? 0) - Number(right.sortValue ?? 0) || left.name.localeCompare(right.name, "pt-BR"))
      .map((row) => ({ ...row, value: row.secondary ?? 0, secondary: Math.max(0, row.quantity - (row.secondary ?? 0)) }));
    const productWithdrawalRows: TableRow[] = productChartRows.map((row) => ({
      name: row.name,
      retirado: row.redeemed,
      pendente: row.pending,
      parcial: Math.min(row.redeemed, row.pending),
      cancelado: 0,
    }));
    const categoryWithdrawalMap = new Map<string, TableRow>();
    approvedOrders.forEach((order) => {
      const category = orderItemCategory(order, productsById);
      const current = categoryWithdrawalMap.get(category) ?? { name: category, retirado: 0, pendente: 0, parcial: 0, cancelado: 0 };
      const quantity = orderQuantity(order);
      const redeemed = orderRedeemedQuantity(order);
      current.retirado = parseNumber(current.retirado, 0) + redeemed;
      current.pendente = parseNumber(current.pendente, 0) + Math.max(0, quantity - redeemed);
      current.parcial = parseNumber(current.parcial, 0) + (redeemed > 0 && redeemed < quantity ? 1 : 0);
      categoryWithdrawalMap.set(category, current);
    });
    const categoryWithdrawalRows = Array.from(categoryWithdrawalMap.values());
    const withdrawalRateValue = safeDivide(redeemedItems, approvedProductQuantity) * 100;
    const manualWithdrawalRateValue = safeDivide(
      approvedOrders.filter((order) => orderWithdrawalMethod(order) === "Manual").length,
      approvedOrders.filter((order) => orderRedeemedQuantity(order) > 0).length
    ) * 100;
    const salesHealthScore = approvedOrders.length > 0
      ? Math.round(
          clamp(withdrawalRateValue) * 0.25 +
            scoreFromInverseRate(manualWithdrawalRateValue) * 0.2 +
            scoreFromInverseRate(safeDivide(pendingRedeemValue, Math.max(productRevenue, 1)) * 100) * 0.2 +
            scoreFromInverseRate(safeDivide(pendingRedeemAgingRows.find((row) => row.name === "Mais de 24h")?.quantity ?? 0, Math.max(approvedProductQuantity, 1)) * 100) * 0.15 +
            scoreFromInverseRate(safeDivide(auditRows.length, Math.max(approvedOrders.length, 1)) * 100) * 0.1 +
            scoreFromInverseRate(safeDivide(median(withdrawalDurations), 24) * 100) * 0.1
        )
      : null;
    const productRiskRadarRows: MetricRow[] = [
      { name: "Volume", quantity: approvedProductQuantity, value: scoreFromRatio(approvedProductQuantity, Math.max(approvedTicketQuantity, approvedProductQuantity, 1)) },
      { name: "Receita", quantity: approvedOrders.length, value: scoreFromRatio(productRevenue, Math.max(grossRevenue, 1)) },
      { name: "Pendência", quantity: pendingRedeemItems, value: clamp(safeDivide(pendingRedeemItems, Math.max(approvedProductQuantity, 1)) * 100) },
      { name: "Manualidade", quantity: approvedOrders.filter((order) => orderWithdrawalMethod(order) === "Manual").length, value: clamp(manualWithdrawalRateValue) },
      { name: "Atraso", quantity: pendingProductOrders.length, value: clamp(safeDivide(maxValue(pendingWaitHoursProducts), 24) * 100) },
      { name: "Auditoria", quantity: auditRows.length, value: clamp(safeDivide(auditRows.length, Math.max(approvedOrders.length, 1)) * 100) },
    ];
    const operatorMethodHeatmapRows: HeatmapEntry[] = approvedOrders
      .filter((order) => orderRedeemedQuantity(order) > 0)
      .map((order) => ({
        row: orderWithdrawalOperator(order),
        column: orderWithdrawalMethod(order),
        value: orderRedeemedQuantity(order),
        href: eventHref(orderEventId(order) || productEventId(productsById.get(orderProductId(order)) ?? {}), { type: "produto", status: "aprovado", approver: orderWithdrawalOperator(order) }),
      }));
    const withdrawalErrorRows: MetricRow[] = [
      { name: "QR ausente", quantity: approvedOrders.filter((order) => !orderHasCode(order)).length, value: 0 },
      { name: "Utilizado sem data", quantity: approvedOrders.filter((order) => orderQrStatus(order).toLowerCase().includes("util") && !orderWithdrawalDate(order)).length, value: 0 },
      { name: "Ativo com baixa", quantity: approvedOrders.filter((order) => normalizeText(orderQrStatus(order)).includes("ativo") && orderRedeemedQuantity(order) > 0).length, value: 0 },
      { name: "Duplicado", quantity: Array.from(approvedOrders.flatMap(orderCodes).reduce((map, code) => map.set(code, (map.get(code) ?? 0) + 1), new Map<string, number>()).values()).filter((count) => count > 1).length, value: 0 },
    ].filter((row) => row.quantity > 0);
    const operatorSalesRows = Array.from(
      approvedOrders.reduce((map, order) => {
        const operator = orderWithdrawalOperator(order);
        const current = map.get(operator) ?? { operador: operator, baixas: 0, valor: 0, manualidade: 0, mediana: 0, conflitos: 0, href: "" };
        const redeemed = orderRedeemedQuantity(order);
        current.baixas += redeemed;
        current.valor += orderTotal(order) * safeDivide(redeemed, orderQuantity(order));
        current.manualidade += orderWithdrawalMethod(order) === "Manual" ? redeemed : 0;
        current.conflitos += orderApproverName(order) === operator || orderCreatedByName(order) === operator ? 1 : 0;
        current.href ||= eventHref(orderEventId(order) || productEventId(productsById.get(orderProductId(order)) ?? {}), { type: "produto", status: "aprovado", approver: operator });
        map.set(operator, current);
        return map;
      }, new Map<string, { operador: string; baixas: number; valor: number; manualidade: number; mediana: number; conflitos: number; href: string }>())
    ).map(([, row]) => ({
      ...row,
      manualidade: safeDivide(row.manualidade, row.baixas) * 100,
      mediana: median(
        approvedOrders
          .filter((order) => orderWithdrawalOperator(order) === row.operador)
          .map((order) => {
            const approval = orderApprovalDate(order);
            const withdrawal = orderWithdrawalDate(order);
            return approval && withdrawal ? (withdrawal.getTime() - approval.getTime()) / 36e5 : Number.NaN;
          })
          .filter((value) => Number.isFinite(value) && value >= 0)
      ),
    })).sort((left, right) => right.baixas - left.baixas);
    const conflictAuditRows: TableRow[] = approvedOrders
      .map((order) => {
        const created = orderCreatedByName(order);
        const approved = orderApproverName(order);
        const withdrawn = orderWithdrawalOperator(order);
        const sameCreatedApproved = created !== "-" && normalizeText(created) === normalizeText(approved);
        const sameApprovedWithdrawn = withdrawn !== "-" && normalizeText(approved) === normalizeText(withdrawn);
        const sameCreatedWithdrawn = withdrawn !== "-" && created !== "-" && normalizeText(created) === normalizeText(withdrawn);
        const allSame = sameCreatedApproved && sameApprovedWithdrawn;
        const severity = allSame && orderTotal(order) >= 100 ? "Crítica" : allSame ? "Alta" : sameCreatedApproved || sameApprovedWithdrawn ? "Média" : sameCreatedWithdrawn ? "Baixa" : "";
        return severity
          ? {
              gravidade: severity,
              pedido: asString(order.id),
              cliente: asString(order.userName || orderBuyerId(order)),
              produto: orderItemName(order, productsById),
              valor: orderTotal(order),
              criado: created,
              aprovado: approved,
              baixado: withdrawn,
              href: eventHref(orderEventId(order) || productEventId(productsById.get(orderProductId(order)) ?? {}), { type: "produto", status: "aprovado", search: asString(order.id), indicator: "conflito-funcao" }),
            }
          : null;
      })
      .filter(Boolean) as TableRow[];
    const partialWithdrawalRows: TableRow[] = partialProductOrders.map((order) => ({
      pedido: asString(order.id),
      cliente: asString(order.userName || orderBuyerId(order)),
      produto: orderItemName(order, productsById),
      vendido: orderQuantity(order),
      retirado: orderRedeemedQuantity(order),
      pendente: Math.max(0, orderQuantity(order) - orderRedeemedQuantity(order)),
      saldo: orderTotal(order) * safeDivide(Math.max(0, orderQuantity(order) - orderRedeemedQuantity(order)), orderQuantity(order)),
      href: eventHref(orderEventId(order) || productEventId(productsById.get(orderProductId(order)) ?? {}), { type: "produto", status: "aprovado", search: asString(order.id), indicator: "retirada-parcial" }),
    }));
    const pendingProductDetailRows: TableRow[] = pendingProductOrders.map((order) => {
      const started = orderApprovalDate(order) || orderCreatedAt(order);
      const pendingQuantity = Math.max(0, orderQuantity(order) - orderRedeemedQuantity(order));
      return {
        cliente: asString(order.userName || orderBuyerId(order)),
        produto: orderItemName(order, productsById),
        quantidade: pendingQuantity,
        valor: orderTotal(order) * safeDivide(pendingQuantity, orderQuantity(order)),
        aprovadoEm: formatDateTimeShort(orderApprovalDate(order)),
        tempo: started ? (Date.now() - started.getTime()) / 36e5 : 0,
        origem: orderSource(order),
        aprovador: orderApproverName(order),
        qr: orderQrStatus(order),
        href: eventHref(orderEventId(order) || productEventId(productsById.get(orderProductId(order)) ?? {}), { type: "produto", status: "aprovado", search: asString(order.id), indicator: "pendente-retirada" }),
      };
    });
    const salesWaterfallRows: MetricRow[] = [
      { name: "Aprovada", quantity: 1, value: productRevenue },
      { name: "Entregue", quantity: 1, value: redeemedValue },
      { name: "Pendente", quantity: 1, value: -pendingRedeemValue },
      { name: "Cancelada", quantity: 1, value: -cancelledOrders.reduce((sum, order) => sum + orderTotal(order), 0) },
    ];
    const orderSourceQualityRows = Array.from(
      approvedOrders.reduce((map, order) => {
        const source = orderSource(order);
        const current = map.get(source) ?? { name: source, receita: 0, itens: 0, retirado: 0, pendente: 0, parcial: 0, tempo: 0, manualidade: 0, alertas: 0, href: "" };
        const quantity = orderQuantity(order);
        const redeemed = orderRedeemedQuantity(order);
        current.receita += orderTotal(order);
        current.itens += quantity;
        current.retirado += redeemed;
        current.pendente += Math.max(0, quantity - redeemed);
        current.parcial += redeemed > 0 && redeemed < quantity ? 1 : 0;
        current.manualidade += orderWithdrawalMethod(order) === "Manual" ? 1 : 0;
        current.href ||= eventHref(orderEventId(order) || productEventId(productsById.get(orderProductId(order)) ?? {}), { type: "produto", status: "aprovado", source });
        map.set(source, current);
        return map;
      }, new Map<string, { name: string; receita: number; itens: number; retirado: number; pendente: number; parcial: number; tempo: number; manualidade: number; alertas: number; href: string }>())
    ).map(([, row]) => ({
      ...row,
      taxaRetirada: safeDivide(row.retirado, row.itens) * 100,
      manualidade: safeDivide(row.manualidade, row.itens) * 100,
    }));
    const paymentSourceRows = metricRows(
      approvedOrders.reduce((map, order) => {
        addMetric(map, orderPaymentSource(order), orderQuantity(order), orderTotal(order), 0, eventHref(orderEventId(order) || productEventId(productsById.get(orderProductId(order)) ?? {}), { type: "produto", status: "aprovado", indicator: "fonte-pagamento" }));
        return map;
      }, new Map<string, MetricRow>()),
      12
    );
    const paymentIssueRows: TableRow[] = [
      { problema: "Fonte pagamento ausente", quantidade: approvedOrders.filter((order) => orderPaymentSource(order) === "-").length },
      { problema: "Pedido aprovado sem fonte", quantidade: approvedOrders.filter((order) => orderPaymentSource(order) === "-" && orderTotal(order) > 0).length },
      { problema: "Manual sem origem", quantidade: approvedOrders.filter((order) => isManualOrder(order) && orderPaymentSource(order) === "-").length },
    ].filter((row) => Number(row.quantidade) > 0);
    const discountDetailedRows: MetricRow[] = [
      { name: "Receita bruta", quantity: approvedOrders.length, value: productRevenue + productDiscounts },
      { name: "Receita líquida", quantity: approvedOrders.length, value: productRevenue },
      { name: "Desconto", quantity: approvedOrders.filter((order) => orderDiscount(order) > 0).length, value: productDiscounts },
    ];
    const productHourHeatmapRows: HeatmapEntry[] = approvedOrders.map((order) => ({
      row: orderItemName(order, productsById),
      column: hourLabel(orderApprovalDate(order) || orderCreatedAt(order)),
      value: orderQuantity(order),
      href: eventHref(orderEventId(order) || productEventId(productsById.get(orderProductId(order)) ?? {}), { type: "produto", status: "aprovado", search: orderItemName(order, productsById) }),
    }));
    const stockRows: TableRow[] = selectedData.products
      .map((product) => {
        const productLabel = productName(product);
        const metric = productRows.get(productLabel);
        const stock = parseNumber(product.estoque, 0);
        const sold = metric?.quantity ?? 0;
        const redeemed = metric?.redeemed ?? 0;
        const pending = metric?.pending ?? 0;
        return {
          produto: productLabel,
          estoque: stock,
          vendido: sold,
          retirado: redeemed,
          pendente: pending,
          disponivel: Math.max(0, stock - sold),
          ruptura: stock > 0 && sold > stock ? sold - stock : 0,
          href: productEventId(product) ? eventHref(productEventId(product), { type: "produto", status: "aprovado", search: productLabel }) : "",
        };
      })
      .filter((row) => Number(row.estoque) > 0 || Number(row.vendido) > 0)
      .sort((left, right) => Number(right.vendido) - Number(left.vendido));
    const turnoverRows: MetricRow[] = stockRows.slice(0, 12).map((row) => ({
      name: asString(row.produto),
      quantity: Number(row.vendido),
      value: safeDivide(Number(row.vendido), Math.max(Number(row.estoque), 1)) * 100,
      secondary: Number(row.disponivel),
      href: asString(row.href),
    }));
    const productByBuyer = new Map<string, Set<string>>();
    approvedOrders.forEach((order) => {
      const buyerId = orderBuyerId(order);
      const set = productByBuyer.get(buyerId) ?? new Set<string>();
      set.add(orderItemName(order, productsById));
      productByBuyer.set(buyerId, set);
    });
    const crossSellMap = new Map<string, NetworkEdge>();
    productByBuyer.forEach((set) => {
      const products = Array.from(set).sort((left, right) => left.localeCompare(right, "pt-BR"));
      products.forEach((from, index) => {
        products.slice(index + 1).forEach((to) => {
          const key = `${from}:${to}`;
          const current = crossSellMap.get(key) ?? { from, to, value: 0 };
          current.value += 1;
          crossSellMap.set(key, current);
        });
      });
    });
    const crossSellRows = Array.from(crossSellMap.values()).sort((left, right) => right.value - left.value).slice(0, 12);
    const productTicketHistogramRows = Array.from(customerValueMap.values()).reduce((map, customer) => {
      addMetric(map, ticketBucket(customer.product), 1, customer.product);
      return map;
    }, new Map<string, MetricRow>());
    const productTransferRows: HeatmapEntry[] = approvedOrders.flatMap((order) =>
      extractProductTransfers(order).map((transfer) => ({
        row: transfer.actor || "Origem",
        column: `${transfer.target} · ${orderRedeemedQuantity(order) > 0 ? "retirado" : "pendente"}`,
        value: 1,
        href: eventHref(orderEventId(order) || productEventId(productsById.get(orderProductId(order)) ?? {}), { type: "produto", status: "aprovado", indicator: "transferencia" }),
      }))
    );
    const qrStatusRows = metricRows(
      approvedOrders.reduce((map, order) => {
        addMetric(map, orderQrStatus(order), orderQuantity(order), orderTotal(order), 0, eventHref(orderEventId(order) || productEventId(productsById.get(orderProductId(order)) ?? {}), { type: "produto", status: "aprovado", indicator: "status-qr" }));
        return map;
      }, new Map<string, MetricRow>()),
      10
    );
    const improvedAuditRows: TableRow[] = [
      ...auditRows.map((row) => ({
        gravidade: Number(row.quantidade) >= 10 ? "Alta" : Number(row.quantidade) >= 3 ? "Média" : "Baixa",
        alerta: asString(row.alerta),
        quantidade: Number(row.quantidade),
        href: asString(row.href),
        acao: asString(row.href) ? "Abrir extrato" : "",
      })),
      ...conflictAuditRows.map((row) => ({
        gravidade: asString(row.gravidade),
        alerta: `Conflito de função · ${asString(row.produto)}`,
        quantidade: 1,
        href: asString(row.href),
        acao: asString(row.href) ? "Abrir extrato" : "",
      })),
      ...withdrawalErrorRows.map((row) => ({
        gravidade: row.name.includes("Duplicado") ? "Crítica" : "Alta",
        alerta: row.name,
        quantidade: row.quantity,
        href: row.href || "",
        acao: row.href ? "Abrir extrato" : "",
      })),
    ].filter((row) => Number(row.quantidade) > 0);

    return {
      approvedTickets,
      approvedOrders,
      rejectedTickets,
      rejectedOrders,
      pendingTickets,
      pendingOrders,
      grossRevenue,
      netRevenue,
      ticketRevenue,
      ticketNetRevenue: Math.max(0, ticketRevenue - ticketDiscounts),
      productRevenue,
      approvedTicketQuantity,
      approvedProductQuantity,
      ticketCreatedCount: selectedData.tickets.length,
      ticketApprovedCount: approvedTickets.length,
      allApprovedCount,
      allCreatedCount,
      approvalRate: safeDivide(allApprovedCount, paymentSent) * 100,
      rejectionRate: safeDivide(rejectedTickets.length + rejectedOrders.length, paymentSent) * 100,
      ticketApprovalRate: safeDivide(approvedTickets.length, selectedData.tickets.length) * 100,
      ticketRejectionRate: safeDivide(rejectedTickets.length, selectedData.tickets.length) * 100,
      ticketAverageByOrder: safeDivide(ticketRevenue, approvedTickets.length),
      ticketAverageByItem: safeDivide(ticketRevenue, approvedTicketQuantity),
      ticketAverageByCustomer: safeDivide(ticketRevenue, ticketBuyerPurchases.size),
      averageByItem: safeDivide(grossRevenue, approvedTicketQuantity + approvedProductQuantity),
      averageByCustomer: safeDivide(grossRevenue, buyerPurchases.size),
      ticketFunnelRows: [
        { name: "Clique no card", quantity: eventCardClicks, value: 0 },
        { name: "Clique em comprar", quantity: eventPurchaseClicks, value: 0 },
        { name: "Pedido criado", quantity: selectedData.tickets.length, value: 0 },
        { name: "RSVP Eu vou", quantity: rsvpGoing, value: 0 },
        { name: "RSVP Talvez", quantity: rsvpMaybe, value: 0 },
        { name: "Pedido aprovado", quantity: approvedTickets.length, value: 0 },
        { name: "Check-in", quantity: ticketScanned, value: 0 },
        { name: "Check-in com compra", quantity: checkedInBuyersWithPurchase, value: 0 },
      ],
      funnelRows: [
        { name: "Clique no card", quantity: eventCardClicks, value: 0 },
        { name: "Clique em comprar", quantity: purchaseClicks, value: 0 },
        { name: "Pedido criado", quantity: allCreatedCount, value: 0 },
        { name: "RSVP Eu vou", quantity: rsvpGoing, value: 0 },
        { name: "RSVP Talvez", quantity: rsvpMaybe, value: 0 },
        { name: "Pedido aprovado", quantity: allApprovedCount, value: 0 },
        { name: "Check-in", quantity: ticketScanned, value: 0 },
        { name: "Check-in com compra", quantity: checkedInBuyersWithPurchase, value: 0 },
      ],
      lotRows: metricRows(byLot, 12),
      classRows: metricRows(byClass, 12),
      audienceRows: metricRows(byAudience, 8),
      audienceTotal: Array.from(byAudience.values()).reduce((sum, row) => sum + row.quantity, 0),
      weekdayRows: WEEKDAYS.map((name) => byWeekday.get(name) ?? { name, quantity: 0, value: 0 }),
      periodRows: PERIODS.map((name) => byPeriod.get(name) ?? { name, quantity: 0, value: 0 }),
      priceRows: metricRows(byPrice, 12),
      approvalRows,
      ticketApprovalRows,
      approvalMethodRows: metricRows(byApprovalMethod, 10),
      ticketApprovalMethodRows: metricRows(byTicketApprovalMethod, 10),
      approvalAverage,
      ticketApprovalAverage,
      approvalMedian: median(approvalDurations),
      ticketApprovalMedian: median(ticketApprovalDurations),
      pendingAgingRows: [
        { name: "Menos de 1h", quantity: pendingAging.less1, value: 0 },
        { name: "1 a 6h", quantity: pendingAging.oneTo6, value: 0 },
        { name: "6 a 24h", quantity: pendingAging.sixTo24, value: 0 },
        { name: "Mais de 24h", quantity: pendingAging.more24, value: 0 },
      ],
      ticketPendingAgingRows: [
        { name: "Menos de 1h", quantity: ticketPendingAging.less1, value: 0 },
        { name: "1 a 6h", quantity: ticketPendingAging.oneTo6, value: 0 },
        { name: "6 a 24h", quantity: ticketPendingAging.sixTo24, value: 0 },
        { name: "Mais de 24h", quantity: ticketPendingAging.more24, value: 0 },
      ],
      operationalPendingCount: pendingOperationalRecords.length,
      operationalPendingNearEvent: pendingNearEvent,
      operationalPendingAtDoor: pendingAtDoor,
      operationalPendingByEventRows: metricRows(pendingByEvent, 12),
      operationalPendingByTypeRows: metricRows(pendingByType, 10),
      operationalPendingAgeRows: Array.from(pendingAgeBuckets.values()),
      operationalApprovalAverage: safeDivide(
        operationalApprovalDurations.reduce((sum, value) => sum + value, 0),
        operationalApprovalDurations.length
      ),
      operationalApprovalMedian: median(operationalApprovalDurations),
      operationalApprovalP90: percentile(operationalApprovalDurations, 0.9),
      operationalApprovalP95: percentile(operationalApprovalDurations, 0.95),
      operationalMaxPendingHours: maxValue(pendingWaitHours),
      operationalApprovedWithin5m: safeDivide(operationalApprovalDurations.filter((value) => value <= 5 / 60).length, operationalApprovalDurations.length) * 100,
      operationalApprovedWithin15m: safeDivide(operationalApprovalDurations.filter((value) => value <= 0.25).length, operationalApprovalDurations.length) * 100,
      operationalApprovedWithin1h: safeDivide(operationalApprovalDurations.filter((value) => value <= 1).length, operationalApprovalDurations.length) * 100,
      operationalApprovedWithin24h: safeDivide(operationalApprovalDurations.filter((value) => value <= 24).length, operationalApprovalDurations.length) * 100,
      slaBySourceRows,
      slaByApproverRows,
      slaByEventRows,
      approvalToEntryMedian: median(approvalToEntryDurations),
      approvalToWithdrawalMedian: median(approvalToWithdrawalDurations),
      approvedWithoutCodeCount: approvedWithoutCode.length,
      codeWithoutUseCount: codeWithoutUse.length,
      usedWithoutApprovalCount: usedWithoutApproval.length,
      inconsistentStatusCount: inconsistentStatus.length,
      approvedNearEventCount: approvedNearEvent.length,
      operatorQualityRows,
      activeOperatorCount,
      operatorDistributionRows,
      demandWithoutCoverageRows,
      outsideHoursApprovals,
      singleOperatorEventRows,
      manualityStageRows,
      manualityStageChartRows,
      operationalControlAlertRows: operationalAlertRows,
      topApproverDependency: safeDivide(topApproverCount, allApprovedCount) * 100,
      top3ApproverDependency: safeDivide(top3ApproverCount, allApprovedCount) * 100,
      ticketTopApproverDependency: safeDivide(ticketTopApproverCount, approvedTickets.length) * 100,
      ticketTop3ApproverDependency: safeDivide(ticketTop3ApproverCount, approvedTickets.length) * 100,
      slowApprovals,
      operationalAlerts,
      operationalTicketAlerts,
      ticketScanned,
      noShow,
      showRate: safeDivide(ticketScanned, approvedTicketQuantity) * 100,
      noShowRate: safeDivide(noShow, approvedTicketQuantity) * 100,
      revenuePerPresent: safeDivide(ticketRevenue, ticketScanned),
      duplicateScans,
      invalidScans,
      appScans,
      manualScans,
      manualityRate,
      qrRate,
      totalCapacity,
      capacityRemaining,
      occupancyRate,
      queueRisk,
      queuePressure,
      activeGateOperators,
      peakInterval,
      averageMinutesBetweenScans,
      longestFastSequence,
      longestIdleMinutes,
      entryCumulativeRows,
      entryTimingRows,
      presenceByTypeRows,
      presenceByLotRows,
      noShowRateByLotRows,
      scanModeByHourRows,
      entryModeRows,
      portariaOperatorRows,
      portariaOperatorChartRows,
      operatorQualityRadarRows,
      invalidReasonRows,
      duplicateContextRows,
      approvedWithoutReadRows,
      presentByClassRows,
      presenceBySourceRows,
      presenceByTransferRows,
      operationalCategoryRows,
      occupancyRows,
      intervalRows,
      liveStatusRows,
      absentRows,
      unusedActiveRows,
      portariaAlertRows,
      portariaEventComparisonRows,
      portariaEventComparisonChartRows,
      scanByHourRows: metricRows(scanByHour, 24).sort((left, right) => left.name.localeCompare(right.name)),
      noShowByClassRows: metricRows(noShowByClass, 12),
      noShowByLotRows: metricRows(noShowByLot, 12),
      uniqueBuyers: buyerPurchases.size,
      recurringBuyers,
      recurringRate: safeDivide(recurringBuyers, buyerPurchases.size) * 100,
      leadRows: Array.from(leadBuckets.values()),
      recurrenceRows: Array.from(recurrenceRows.values()),
      projectedRevenue,
      resultWithoutCosts: netRevenue,
      eventDecisionRows,
      revenueOriginRows,
      revenueDetailRows,
      totalRevenuePerBuyer,
      totalRevenuePerPresent,
      ticketRevenuePerPresent,
      productRevenuePerPresent,
      productPerPresent,
      productRevenueShare,
      ticketRevenueShare,
      ticketBuyerCount: ticketBuyerIds.size,
      checkedInTicketBuyerCount: checkedInApprovedTicketBuyerIds.size,
      productBuyerCount: productBuyerIds.size,
      productRedeemedBuyerCount: productRedeemedBuyerIds.size,
      buyersWithTicketAndProduct,
      ticketWithoutProduct,
      productWithoutTicket,
      productPresentBuyerIds,
      strategicFunnelRows,
      attachRateRows,
      strategicEventRows,
      strategicScore,
      strategicDecision,
      strategicRadarRows,
      strategicBubbleRows,
      revenuePerPresentRows,
      eventProductHeatmapRows,
      eventCategoryHeatmapRows,
      categoryCompositionChartRows,
      ticketLeadRows,
      productLeadRows,
      recurrenceDetailRows,
      strategicRecurringBuyers,
      strategicRecurringRate,
      tenantParticipationRows,
      customerTicketHistogramRows: Array.from(customerTicketHistogramRows.values()).sort(
        (left, right) => ticketBucketSortValue(left.name) - ticketBucketSortValue(right.name)
      ),
      topCustomerRows,
      topCustomersByEventRows,
      classConsumptionRows: consumptionRowsFromMap(classConsumptionMap),
      lotConsumptionRows: consumptionRowsFromMap(lotConsumptionMap),
      sourceTreemapRows,
      discountImpactRows,
      priceStrategyRows,
      forecastRows,
      resultWaterfallRows,
      eventCostsTotal,
      hasEventCostsField,
      breakEvenTickets,
      redeemedItems,
      redeemedValue,
      pendingRedeemItems,
      pendingRedeemValue,
      withdrawalRate: safeDivide(redeemedItems, approvedProductQuantity) * 100,
      pendingRedeemOrders: approvedOrders.filter((order) => orderRedeemedQuantity(order) < orderQuantity(order)).length,
      partialRedeemOrders: partialProductOrders.length,
      oldestPendingOrderName: oldestPendingOrder ? orderItemName(oldestPendingOrder.order, productsById) : "-",
      maxPendingRedeemHours: maxValue(pendingWaitHoursProducts),
      averageWithdrawalHours: median(withdrawalDurations),
      manualWithdrawalRate: safeDivide(
        approvedOrders.filter((order) => orderWithdrawalMethod(order) === "Manual").length,
        approvedOrders.filter((order) => orderRedeemedQuantity(order) > 0).length
      ) * 100,
      productRows: productTableRows,
      productChartRows,
      categoryRows: metricRows(byProductCategory, 12),
      discountRows: metricRows(byDiscountSource, 10),
      orderSourceRows: metricRows(byOrderSource, 10),
      withdrawalMethodRows: metricRows(byWithdrawalMethod, 10),
      withdrawalOperatorRows: metricRows(byWithdrawalOperator, 12),
      transferModeRows: metricRows(byTransferMode, 6),
      transferTargetRows: metricRows(byTransferTarget, 6),
      transferActorRows: metricRows(byTransferActor, 12),
      auditRows,
      pendingRedeemAgingRows,
      withdrawalStatusRows,
      salesWithdrawalTimelineRows,
      productWithdrawalRows,
      categoryWithdrawalRows,
      salesHealthScore,
      productRiskRadarRows,
      operatorMethodHeatmapRows,
      withdrawalErrorRows,
      operatorSalesRows,
      conflictAuditRows,
      partialWithdrawalRows,
      pendingProductDetailRows,
      salesWaterfallRows,
      orderSourceQualityRows,
      paymentSourceRows,
      paymentIssueRows,
      discountDetailedRows,
      productHourHeatmapRows,
      stockRows,
      turnoverRows,
      crossSellRows,
      productTicketHistogramRows: Array.from(productTicketHistogramRows.values()).sort(
        (left, right) => ticketBucketSortValue(left.name) - ticketBucketSortValue(right.name)
      ),
      productTransferRows,
      qrStatusRows,
      improvedAuditRows,
    };
  }, [audienceBasis, buildCheckinsHref, buildStatementHref, data.orders, data.tickets, data.users, endDate, entityMemberIndex, eventById, eventFilter, matchesActiveScope, productsById, selectedData.events, selectedData.orders, selectedData.products, selectedData.rsvps, selectedData.tickets, startDate, userById]);

  const shellContext = { backHref, contextTitle, contextLogo, contextEyebrow };
  const eventOwnerRedirectHref = useMemo(() => {
    if (lockedScopeType === "tenant" || eventFilter === "todos") return "";
    const selectedEvent = eventById.get(eventFilter);
    if (!selectedEvent) return "";
    const owner = canonicalEventOwnerScope(selectedEvent, entityScopeIndex, eventScopeIndex);
    const cleanLockedScopeId = (lockedScopeId || "todos").trim();
    const ownerMismatch =
      owner.scopeType !== lockedScopeType ||
      (cleanLockedScopeId !== "todos" && owner.scopeId !== cleanLockedScopeId);
    if (!ownerMismatch) return "";
    const path = canonicalEventWorkspacePath(owner.scopeType, owner.scopeId, eventFilter, "edicao");
    return tenantSlug ? withTenantSlug(tenantSlug, path) : path;
  }, [entityScopeIndex, eventById, eventFilter, eventScopeIndex, lockedScopeId, lockedScopeType, tenantSlug]);

  useEffect(() => {
    if (!loading && eventOwnerRedirectHref) {
      router.replace(eventOwnerRedirectHref);
    }
  }, [eventOwnerRedirectHref, loading, router]);

  if (view === "inicio" && eventFilter === "todos") {
    return (
      <DashboardShell view={view} basePath={basePath} scopeLabel={scopeLabel} {...shellContext}>
        <HubContent basePath={basePath} />
      </DashboardShell>
    );
  }

  if (loading) {
    return (
      <DashboardShell view={view} basePath={basePath} scopeLabel={scopeLabel} {...shellContext}>
        <div className="flex min-h-[420px] items-center justify-center rounded-lg border border-zinc-800 bg-zinc-950">
          <Loader2 className="animate-spin text-emerald-300" />
        </div>
      </DashboardShell>
    );
  }

  if (error) {
    return (
      <DashboardShell view={view} basePath={basePath} scopeLabel={scopeLabel} {...shellContext}>
        <div className="rounded-lg border border-red-500/30 bg-red-500/10 p-5 text-sm font-bold text-red-200">
          {error}
        </div>
      </DashboardShell>
    );
  }

  if (eventOwnerRedirectHref) {
    return (
      <DashboardShell view={view} basePath={basePath} scopeLabel={scopeLabel} {...shellContext}>
        <div className="rounded-lg border border-amber-400/30 bg-amber-400/10 p-5 text-sm font-bold leading-6 text-amber-100">
          Este evento pertence a outro portal. Redirecionando para a página correta...
          <Link href={eventOwnerRedirectHref} prefetch={false} className="ml-2 text-white underline underline-offset-4">
            Abrir agora
          </Link>
        </div>
      </DashboardShell>
    );
  }

  if (view === "inicio") {
    return (
      <DashboardShell view={view} basePath={basePath} scopeLabel={scopeLabel} {...shellContext}>
        <HubContent basePath={basePath} />
      </DashboardShell>
    );
  }

  const selectedStatementEventId =
    eventFilter !== "todos"
      ? eventFilter
      : selectedData.events.length === 1
        ? eventId(selectedData.events[0])
        : "";
  const absenceHref = selectedStatementEventId
    ? buildCheckinsHref(selectedStatementEventId, { indicator: "ausente" })
    : "";
  const manualEntryHref = selectedStatementEventId
    ? buildStatementHref(selectedStatementEventId, { type: "ingresso", status: "aprovado", flow: "checkin", indicator: "manual" })
    : "";
  const salesWithdrawalLegendLinks = selectedStatementEventId
    ? [
        {
          label: "Retirado",
          color: "#22c55e",
          href: buildStatementHref(selectedStatementEventId, { type: "produto", status: "aprovado", indicator: "retirado" }),
        },
        {
          label: "Pendente",
          color: "#facc15",
          href: buildStatementHref(selectedStatementEventId, { type: "produto", status: "aprovado", indicator: "pendente-retirada" }),
        },
        {
          label: "Parcial",
          color: "#38bdf8",
          href: buildStatementHref(selectedStatementEventId, { type: "produto", status: "aprovado", indicator: "retirada-parcial" }),
        },
        {
          label: "Cancelado",
          color: "#fb7185",
          href: buildStatementHref(selectedStatementEventId, { type: "produto", indicator: "cancelado-pos-aprovacao" }),
        },
      ]
    : [];
  const strategicScoreLabel =
    analytics.strategicScore === null ? "Sem dados" : formatNumber(analytics.strategicScore);
  const strategicCostHint =
    analytics.eventCostsTotal > 0
      ? `Custos: ${formatCurrency(analytics.eventCostsTotal)}`
      : analytics.hasEventCostsField
        ? "Custo cadastrado como R$ 0,00"
        : "Campo de custo opcional vazio";
  const salesHealthLabel =
    analytics.salesHealthScore === null
      ? "Sem dados suficientes"
      : analytics.salesHealthScore >= 85
        ? "Excelente"
        : analytics.salesHealthScore >= 70
          ? "Boa"
          : analytics.salesHealthScore >= 40
            ? "Atenção"
            : "Crítica";
  const salesHealthValue =
    analytics.salesHealthScore === null ? "Sem dados" : formatNumber(analytics.salesHealthScore);

  return (
    <DashboardShell view={view} basePath={basePath} scopeLabel={scopeLabel} {...shellContext}>
      <Filters
        scopeType={scopeType}
        setScopeType={setScopeType}
        scopeId={scopeId}
        setScopeId={setScopeId}
        scopeOptions={scopeOptions}
        eventFilter={eventFilter}
        setEventFilter={setEventFilter}
        productFilter={productFilter}
        setProductFilter={setProductFilter}
        startDate={startDate}
        setStartDate={setStartDate}
        endDate={endDate}
        setEndDate={setEndDate}
        eventOptions={eventOptions}
        productOptions={productOptions}
        showProduct={view === "vendas"}
        scopeLocked={Boolean(lockedScopeType)}
        scopeLabel={scopeLabel || (scopeType === "tenant" ? "Atlética" : "Entidade")}
      />

      {view === "comercial" ? (
        <section className="space-y-5">
          <KpiGrid>
            <KpiCard label="Receita bruta" value={formatCurrency(analytics.ticketRevenue)} hint={`${formatNumber(analytics.approvedTicketQuantity)} ingressos aprovados`} icon={<DollarSign size={18} />} />
            <KpiCard label="Receita líquida" value={formatCurrency(analytics.ticketNetRevenue)} hint="Receita de ingressos menos descontos registrados" icon={<TrendingUp size={18} />} />
            <KpiCard label="Taxa de aprovação" value={formatPercent(analytics.ticketApprovalRate)} hint={`${formatNumber(analytics.ticketApprovedCount)} aprovados de ${formatNumber(analytics.ticketCreatedCount)} pedidos`} icon={<CheckCircle2 size={18} />} />
            <KpiCard label="Ticket por cliente" value={formatCurrency(analytics.ticketAverageByCustomer)} hint="Receita de ingressos / compradores únicos" icon={<Users size={18} />} />
          </KpiGrid>
          <KpiGrid>
            <KpiCard label="Ticket por pedido" value={formatCurrency(analytics.ticketAverageByOrder)} hint="Receita de ingressos / pedidos aprovados" icon={<Ticket size={18} />} />
            <KpiCard label="Valor médio por ingresso" value={formatCurrency(analytics.ticketAverageByItem)} hint="Receita de ingressos / ingressos aprovados" icon={<Package size={18} />} />
            <KpiCard label="Pedidos criados" value={formatNumber(analytics.ticketCreatedCount)} hint="Pedidos de ingresso no filtro" icon={<ShoppingBag size={18} />} />
            <KpiCard label="Ingressos aprovados" value={formatNumber(analytics.approvedTicketQuantity)} hint="Apenas ingressos de evento" icon={<BarChart3 size={18} />} />
          </KpiGrid>
          <div className="grid gap-4 xl:grid-cols-2">
            <ChartPanel
              title="Funil completo"
              subtitle="Card, compra, pedido, RSVP, aprovação, check-in e compra"
              info={
                "Clique no card soma aberturas vindas da página /eventos.\nClique em comprar soma os botões Comprar/Garantir da página do evento.\nRSVP Eu vou e RSVP Talvez vêm dos botões de presença.\nCheck-in soma entradas por QR e entradas manuais.\nCheck-in com compra conta usuários que entraram no evento e também tiveram compra/ficha aprovada no mesmo filtro."
              }
            >
              <Bars data={analytics.ticketFunnelRows} />
            </ChartPanel>
            <ChartPanel title="Lotes por retorno" subtitle="Quantidade e receita por lote de ingresso">
              <BarsDual data={analytics.lotRows} />
            </ChartPanel>
            <ChartPanel title="Turmas por venda" subtitle="Pedidos, itens e receita por turma">
              <BarsDual data={analytics.classRows} />
            </ChartPanel>
            <ChartPanel title="Preço que performa" subtitle="Faixas de valor médio por item">
              <BarsDual data={analytics.priceRows} />
            </ChartPanel>
            <ChartPanel title="Compras por dia da semana" subtitle="Volume e receita por dia">
              <BarsDual data={analytics.weekdayRows} />
            </ChartPanel>
            <ChartPanel title="Compras por período" subtitle="Madrugada, manhã, tarde e noite">
              <BarsDual data={analytics.periodRows} />
            </ChartPanel>
            <ChartPanel
              title="Aluno, convidado e externo"
              cornerMetric={`Total ${formatNumber(analytics.audienceTotal)}`}
              toolbar={
                <div className="flex rounded-lg border border-zinc-800 bg-black p-1">
                  {AUDIENCE_BASIS_OPTIONS.map((option) => (
                    <button
                      key={option.id}
                      type="button"
                      onClick={() => setAudienceBasis(option.id)}
                      className={`rounded-md px-2.5 py-1.5 text-[10px] font-black uppercase transition ${
                        audienceBasis === option.id
                          ? "bg-emerald-400 text-black"
                          : "text-zinc-500 hover:text-white"
                      }`}
                    >
                      {option.label}
                    </button>
                  ))}
                </div>
              }
            >
              <PieMetric data={analytics.audienceRows} />
            </ChartPanel>
            <ChartPanel title="Transferências por origem" subtitle="Separação entre app e operação manual">
              <PieMetric data={analytics.transferModeRows} />
            </ChartPanel>
            <ChartPanel title="Destino da transferência" subtitle="Usuário da faculdade ou cadastro manual/externo">
              <PieMetric data={analytics.transferTargetRows} />
            </ChartPanel>
            <ChartPanel title="Quem mais transferiu" subtitle="Usuários que mais enviaram ingressos ou fichas">
              <Bars data={analytics.transferActorRows} />
            </ChartPanel>
          </div>
        </section>
      ) : null}

      {view === "operacional" ? (
        <section className="space-y-5">
          <KpiGrid>
            <KpiCard label="Pendentes agora" value={formatNumber(analytics.operationalPendingCount)} hint="Ingressos, produtos, cortesia, transferência e cadastro manual" icon={<Clock3 size={18} />} />
            <KpiCard label="Tempo médio até aprovação" value={`${formatDecimal(analytics.operationalApprovalAverage)}h`} hint="Pedido criado até aprovação" icon={<Clock3 size={18} />} />
            <KpiCard label="Mediana / P90" value={`${formatDecimal(analytics.operationalApprovalMedian)}h / ${formatDecimal(analytics.operationalApprovalP90)}h`} hint="Centro da fila e piores casos" icon={<BarChart3 size={18} />} />
            <KpiCard label="Maior espera pendente" value={`${formatDecimal(analytics.operationalMaxPendingHours)}h`} hint="Pedido ainda aguardando aprovação" icon={<AlertTriangle size={18} />} />
          </KpiGrid>
          <KpiGrid>
            <KpiCard label="Aprovado em até 5 min" value={formatPercent(analytics.operationalApprovedWithin5m)} hint={`Até 15 min: ${formatPercent(analytics.operationalApprovedWithin15m)}`} icon={<CheckCircle2 size={18} />} />
            <KpiCard label="Aprovado em até 1h" value={formatPercent(analytics.operationalApprovedWithin1h)} hint={`Até 24h: ${formatPercent(analytics.operationalApprovedWithin24h)}`} icon={<CheckCircle2 size={18} />} />
            <KpiCard label="Operadores ativos" value={formatNumber(analytics.activeOperatorCount)} hint={`${formatNumber(analytics.outsideHoursApprovals)} aprovações fora do horário esperado`} icon={<Users size={18} />} />
            <KpiCard
              label="Dependência top 1"
              value={formatPercent(analytics.topApproverDependency)}
              hint={`Top 3: ${formatPercent(analytics.top3ApproverDependency)}`}
              icon={<Users size={18} />}
              info="Percentual de aprovações feitas pelo principal aprovador. Quanto maior, maior a dependência operacional em uma pessoa."
            />
          </KpiGrid>
          <KpiGrid>
            <KpiCard label="Próximos do evento" value={formatNumber(analytics.operationalPendingNearEvent)} hint="Pendentes com evento nas próximas 24h" icon={<AlertTriangle size={18} />} />
            <KpiCard label="Criados na porta" value={formatNumber(analytics.operationalPendingAtDoor)} hint="Pendentes originados em entrada/manual" icon={<Ticket size={18} />} />
            <KpiCard label="Aprovados sem QR/código" value={formatNumber(analytics.approvedWithoutCodeCount)} hint="Aprovados sem identificador operacional" icon={<QrCode size={18} />} />
            <KpiCard label="QR/código sem uso" value={formatNumber(analytics.codeWithoutUseCount)} hint="Aprovados com código, mas sem entrada/retirada" icon={<Package size={18} />} />
          </KpiGrid>
          <KpiGrid>
            <KpiCard label="Aprovação até entrada" value={`${formatDecimal(analytics.approvalToEntryMedian)}h`} hint="Mediana entre aprovação e check-in" icon={<QrCode size={18} />} />
            <KpiCard label="Aprovação até retirada" value={`${formatDecimal(analytics.approvalToWithdrawalMedian)}h`} hint="Mediana entre aprovação e baixa de produto" icon={<Package size={18} />} />
            <KpiCard label="Uso sem aprovação" value={formatNumber(analytics.usedWithoutApprovalCount)} hint="Entrada/retirada sem aprovação clara" icon={<AlertTriangle size={18} />} />
            <KpiCard label="Status incoerente" value={formatNumber(analytics.inconsistentStatusCount)} hint={`${formatNumber(analytics.approvedNearEventCount)} aprovados perto do evento`} icon={<AlertTriangle size={18} />} />
          </KpiGrid>
          <div className="grid gap-4 xl:grid-cols-2">
            <ChartPanel title="Fila por evento" subtitle="Pedidos pendentes agora e valor parado">
              <BarsDual data={analytics.operationalPendingByEventRows} valueName="Valor parado" quantityName="Pendentes" />
            </ChartPanel>
            <ChartPanel title="Fila por tipo" subtitle="Ingresso, produto, cortesia, transferência e cadastro manual">
              <PieMetric data={analytics.operationalPendingByTypeRows} />
            </ChartPanel>
            <ChartPanel title="Idade da fila" subtitle="Tempo real aguardando aprovação">
              <Bars data={analytics.operationalPendingAgeRows} />
            </ChartPanel>
            <ChartPanel title="Distribuição por operador" subtitle="Volume aprovado e valor processado">
              <BarsDual data={analytics.operatorDistributionRows} />
            </ChartPanel>
            <ChartPanel title="Método de aprovação" subtitle="Manual, automático, Pix, cortesia, transferência ou admin">
              <PieMetric data={analytics.approvalMethodRows} />
            </ChartPanel>
          </div>
          <div className="grid gap-4 xl:grid-cols-3">
            <ChartPanel title="SLA por origem" subtitle="Clique na origem para abrir o extrato com os itens correspondentes">
              <BarsDual
                data={analytics.slaBySourceRows}
                valueName="Mediana"
                quantityName="Aprovados"
                valueFormat="hours"
              />
            </ChartPanel>
            <ChartPanel title="SLA por aprovador" subtitle="Mediana de aprovação e volume por operador">
              <BarsDual
                data={analytics.slaByApproverRows}
                valueName="Mediana"
                quantityName="Aprovados"
                valueFormat="hours"
              />
            </ChartPanel>
            <ChartPanel title="SLA por evento" subtitle="Compara velocidade operacional entre eventos">
              <BarsDual
                data={analytics.slaByEventRows}
                valueName="Mediana"
                quantityName="Aprovados"
                valueFormat="hours"
              />
            </ChartPanel>
          </div>
          <DataTable
            title="Qualidade por operador"
            rows={analytics.operatorQualityRows}
            columns={[
              { key: "evento", label: "Evento", hrefKey: "href" },
              { key: "operador", label: "Operador", hrefKey: "href" },
              { key: "aprovados", label: "Aprovados", format: "number", hrefKey: "hrefAprovados" },
              { key: "valor", label: "Valor aprovado", format: "currency", hrefKey: "hrefValor" },
              { key: "mediana", label: "Mediana (h)", format: "decimal" },
              { key: "semValor", label: "Sem valor", format: "number", hrefKey: "hrefSemValor" },
              { key: "manuais", label: "Manuais", format: "number", hrefKey: "hrefManuais" },
              { key: "corrigidos", label: "Corrigidos/cancelados", format: "number", hrefKey: "hrefCorrigidos" },
              { key: "semUso", label: "Sem entrada/retirada", format: "number", hrefKey: "hrefSemUso" },
              { key: "mesmoCriador", label: "Mesmo criador", format: "number", hrefKey: "hrefMesmoCriador" },
              { key: "mesmoBaixa", label: "Mesmo baixa", format: "number", hrefKey: "hrefMesmoBaixa" },
            ]}
            pageSize={20}
          />
          <div className="grid gap-4 xl:grid-cols-[0.9fr_1.1fr]">
            <DataTable
              title="Eventos operados por 1 pessoa"
              rows={analytics.singleOperatorEventRows}
              columns={[
                { key: "evento", label: "Evento", hrefKey: "href" },
                { key: "operador", label: "Operador" },
                { key: "aprovacoes", label: "Aprovações", format: "number", hrefKey: "href" },
              ]}
            />
            <ChartPanel title="Manualidade por etapa" subtitle="Pedido, aprovação, check-in e retirada por tipo de item">
              <BarsDual
                data={analytics.manualityStageChartRows}
                valueName="% do tipo"
                quantityName="Itens"
                valueFormat="percent"
              />
            </ChartPanel>
          </div>
          <DataTable
            title="Manualidade por tipo e etapa"
            rows={analytics.manualityStageRows}
            columns={[
              { key: "evento", label: "Evento", hrefKey: "href" },
              { key: "tipo", label: "Tipo", hrefKey: "href" },
              { key: "etapa", label: "Etapa", hrefKey: "href" },
              { key: "quantidade", label: "Itens", format: "number", hrefKey: "href" },
              { key: "valor", label: "Valor", format: "currency", hrefKey: "href" },
              { key: "percentual", label: "% do tipo", format: "percent" },
            ]}
            pageSize={20}
          />
          <DataTable
            title="Alertas operacionais com atalho para o extrato"
            rows={analytics.operationalControlAlertRows}
            columns={[
              { key: "alerta", label: "Alerta", hrefKey: "href" },
              { key: "descricao", label: "O que significa" },
              { key: "evento", label: "Evento" },
              { key: "item", label: "Item" },
              { key: "tipo", label: "Tipo" },
              { key: "quantidade", label: "Quantidade", format: "number", hrefKey: "href" },
            ]}
            pageSize={20}
          />
        </section>
      ) : null}

      {view === "portaria" ? (
        <section className="space-y-5">
          <KpiGrid>
            <KpiCard label="Taxa de presença" value={formatPercent(analytics.showRate)} hint={`${formatNumber(analytics.ticketScanned)} entradas de ${formatNumber(analytics.approvedTicketQuantity)} ingressos`} icon={<QrCode size={18} />} info="Mostra quantos ingressos aprovados realmente viraram entrada. É a conversão real de aprovado para presente." />
            <KpiCard label="Taxa de ausência" value={formatPercent(analytics.noShowRate)} hint={`${formatNumber(analytics.noShow)} aprovados sem entrada`} icon={<AlertTriangle size={18} />} href={absenceHref} info="Mostra o no-show: pessoas com ingresso aprovado que ainda não aparecem como entrada na lista de presença." />
            <KpiCard label="Taxa manual" value={formatPercent(analytics.manualityRate)} hint={`${formatNumber(analytics.manualScans)} manuais de ${formatNumber(analytics.ticketScanned)} entradas`} icon={<CheckCircle2 size={18} />} href={manualEntryHref} info="Entrada manual dividida pelo total de entradas. Taxa alta aumenta risco de erro, duplicidade e perda de rastreabilidade." />
            <KpiCard label="Leituras inválidas" value={formatNumber(analytics.duplicateScans + analytics.invalidScans)} hint={`${formatNumber(analytics.duplicateScans)} duplicadas`} icon={<Ticket size={18} />} info="Soma leituras inválidas e tentativas duplicadas registradas nos ingressos e na auditoria." />
          </KpiGrid>
          <KpiGrid>
            <KpiCard label="Entrada por QR" value={formatNumber(analytics.appScans)} hint={`${formatPercent(analytics.qrRate)} das entradas`} icon={<QrCode size={18} />} info="Entradas validadas por leitura do QR (Quick Response)." />
            <KpiCard label="Entrada manual" value={formatNumber(analytics.manualScans)} hint="Entradas marcadas pela equipe" icon={<CheckCircle2 size={18} />} href={manualEntryHref} info="Entradas sem leitura direta do QR (Quick Response), normalmente registradas pela equipe da porta." />
            <KpiCard label="Ocupação" value={analytics.totalCapacity > 0 ? formatPercent(analytics.occupancyRate) : "Sem capacidade"} hint={analytics.totalCapacity > 0 ? `${formatNumber(analytics.ticketScanned)} de ${formatNumber(analytics.totalCapacity)} vagas` : "Cadastre a capacidade na edição do evento"} icon={<Users size={18} />} info="Usa presentes divididos pela capacidade real do local. É diferente da taxa de presença, que usa ingressos aprovados como base." />
            <KpiCard label="Capacidade restante" value={analytics.totalCapacity > 0 ? formatNumber(analytics.capacityRemaining) : "-"} hint={analytics.totalCapacity > 0 ? "Vagas restantes em tempo real" : "Sem limite cadastrado"} icon={<Target size={18} />} info="Mostra quantas pessoas ainda cabem no evento, considerando as entradas já registradas." />
          </KpiGrid>
          <KpiGrid>
            <KpiCard label="Pico em 15 min" value={formatNumber(analytics.peakInterval.quantity)} hint={analytics.peakInterval.name} icon={<TrendingUp size={18} />} info="Maior volume registrado em uma janela de 15 minutos. Ajuda a enxergar fila que o gráfico por hora pode esconder." />
            <KpiCard label="Velocidade média" value={`${formatDecimal(analytics.averageMinutesBetweenScans)} min`} hint="Tempo médio entre leituras" icon={<Clock3 size={18} />} info="Tempo médio entre uma entrada e outra. Quanto menor, mais rápida foi a operação." />
            <KpiCard label="Risco de fila" value={analytics.queueRisk} hint={`${formatPercent(analytics.queuePressure)} da capacidade estimada da equipe`} icon={<AlertTriangle size={18} />} info="Compara o pico de 15 minutos com uma capacidade estimada de 4 entradas por minuto por operador ativo." />
            <KpiCard label="Operadores ativos" value={formatNumber(analytics.activeGateOperators)} hint={`Maior sequência: ${formatNumber(analytics.longestFastSequence)} leituras`} icon={<Users size={18} />} info="Conta operadores que aparecem registrando entradas e mostra a maior sequência rápida de leituras." />
          </KpiGrid>
          <div className="grid gap-4 xl:grid-cols-2">
            <ChartPanel title="Curva acumulada de entrada" subtitle="Entradas somadas ao longo do evento" info="Mostra a portaria enchendo ao longo do tempo. Se a curva sobe rápido, o público entrou cedo; se fica plana, a entrada parou; se sobe tarde, houve pico atrasado.">
              <LineMetric data={analytics.entryCumulativeRows} />
            </ChartPanel>
            <ChartPanel title="QR versus manual por horário" subtitle="Barra empilhada com linha de manualidade" info="Compara, por horário, quantas entradas foram por QR (Quick Response) e quantas foram manuais. A linha mostra se a equipe passou a abandonar o scanner em algum pico.">
              <ScanModeByHourChart data={analytics.scanModeByHourRows} />
            </ChartPanel>
            <ChartPanel title="Antes, durante e tarde" subtitle="Entrada em relação ao início do evento" info="Separa quem entrou antes do início, na primeira hora, no meio do evento, muito tarde e quem não entrou. Ajuda a entender o comportamento real do público.">
              <SimplePieMetric data={analytics.entryTimingRows} />
            </ChartPanel>
            <ChartPanel title="Ocupação do evento" subtitle="Presentes versus capacidade real" info="Mostra a ocupação do local: entradas registradas contra capacidade cadastrada. Quando não há capacidade, cadastre esse número na edição do evento.">
              <SemiDonutMetric data={analytics.occupancyRows} />
            </ChartPanel>
          </div>
          <div className="grid gap-4 xl:grid-cols-3">
            <ChartPanel title="Leituras por horário" subtitle="Pico de entrada" info="Mostra os horários com mais entradas. Use para reforçar equipe, comunicação e abertura de fila nos horários críticos.">
              <LineMetric data={analytics.scanByHourRows} />
            </ChartPanel>
            <ChartPanel title="Entrada por intervalo" subtitle="Janelas de 15 minutos" info="Mostra o pico real de fila em intervalos menores. Uma hora pode parecer tranquila, mas esconder uma concentração forte em poucos minutos.">
              <BarsDual data={analytics.intervalRows} valueName="Entradas/min" quantityName="Entradas" valueFormat="decimal" />
            </ChartPanel>
            <ChartPanel title="Entrada por modo" subtitle="QR (Quick Response) e manual" info="Mostra a divisão geral das entradas entre leitura de QR (Quick Response) e entrada manual.">
              <PieMetric data={analytics.entryModeRows} />
            </ChartPanel>
            <ChartPanel title="Presença por tipo" subtitle="Tipo de ingresso ou categoria operacional" info="Mostra quais tipos de público realmente apareceram. O foco aqui não é venda, e sim presença real na portaria.">
              <BarsDual data={analytics.presenceByTypeRows} valueName="Presença" quantityName="Presentes" valueFormat="percent" />
            </ChartPanel>
            <ChartPanel title="Taxa de no-show por lote" subtitle="Ausentes proporcionais por lote" info="Transforma ausência em taxa. Assim, um lote grande não parece pior só porque vendeu mais; o gráfico mostra a proporção real de faltas.">
              <ParetoMetric data={analytics.noShowRateByLotRows} />
            </ChartPanel>
            <ChartPanel title="Presença por turma" subtitle="Aprovados que entraram" info="Mostra quais turmas compareceram melhor. É diferente da ausência por turma, porque destaca quem realmente ocupou o evento.">
              <BarsDual data={analytics.presentByClassRows} valueName="Presença" quantityName="Presentes" valueFormat="percent" />
            </ChartPanel>
            <ChartPanel title="Ausência por turma" subtitle="Aprovados sem entrada" info="Aponta turmas com mais aprovados sem leitura de entrada. Use para conferir lista nominal e casos de QR (Quick Response) não lido.">
              <Bars data={analytics.noShowByClassRows} />
            </ChartPanel>
            <ChartPanel title="Ausência por lote" subtitle="Comparação entre tipos de ingresso" info="Mostra a quantidade de aprovados sem entrada em cada lote. Use junto da taxa de no-show para não confundir volume com comportamento.">
              <Bars data={analytics.noShowByLotRows} />
            </ChartPanel>
            <ChartPanel title="Origem do pedido" subtitle="Checkout, manual, admin, cortesia e porta" info="Mostra de onde vieram os ingressos que realmente entraram. Pedidos manuais e cortesia podem ter comportamento diferente do checkout público.">
              <PieMetric data={analytics.presenceBySourceRows} />
            </ChartPanel>
            <ChartPanel title="Presença por transferência" subtitle="Impacto operacional das transferências" info="Mostra transferidos que entraram ou ficaram sem uso. Transferências perto da entrada podem afetar conferência, QR (Quick Response) e fila.">
              <SemiDonutMetric data={analytics.presenceByTransferRows} />
            </ChartPanel>
            <ChartPanel title="Categoria operacional" subtitle="Aluno, não aluno, membro, diretoria e porta" info="Classifica a ocupação real do evento pela estrutura disponível: aluno, não aluno, membro, diretoria, cortesia, convidado e entrada/porta.">
              <BarsDual data={analytics.operationalCategoryRows} valueName="Presença" quantityName="Presentes" valueFormat="percent" />
            </ChartPanel>
            <ChartPanel title="Qualidade da portaria" subtitle="Radar operacional" info="Resume a saúde da operação: uso de QR (Quick Response), baixa manual controlada, leituras válidas, velocidade, presença e rastreabilidade por operador.">
              <RadarMetric data={analytics.operatorQualityRadarRows} />
            </ChartPanel>
            <ChartPanel title="Entradas por operador" subtitle="Volume e manualidade por pessoa" info="Mostra quem registrou entradas e qual percentual foi manual. Ajuda a entender quem trabalhou na porta e como trabalhou.">
              <BarsDual data={analytics.portariaOperatorChartRows} valueName="Manualidade" quantityName="Entradas" valueFormat="percent" />
            </ChartPanel>
            <ChartPanel title="Motivos inválidos" subtitle="Pareto de problemas na leitura" info="Quebra as leituras inválidas por motivo: QR (Quick Response) usado, cancelado, de outro evento, sem aprovação, erro técnico ou código mal formatado.">
              <ParetoMetric data={analytics.invalidReasonRows} />
            </ChartPanel>
            <ChartPanel title="Aprovado sem QR lido" subtitle="Separação inteligente da ausência" info="Separa não comparecimento real de problemas operacionais, como entrada manual, QR (Quick Response) ausente, tentativa inválida ou dados incompletos.">
              <SemiDonutMetric data={analytics.approvedWithoutReadRows} />
            </ChartPanel>
            <ChartPanel title="Comparativo de portarias" subtitle="Somente eventos do recorte atual" info="Compara presença, pico e manualidade entre eventos da mesma entidade ou do mesmo recorte ativo. Não mistura entidades diferentes quando o painel está travado em Liga, Comissão ou Diretório.">
              <BarsDual data={analytics.portariaEventComparisonChartRows} valueName="Manualidade" quantityName="Presentes" valueFormat="percent" />
            </ChartPanel>
          </div>
          <DataTable
            title="Qualidade por operador de portaria"
            rows={analytics.portariaOperatorRows}
            columns={[
              { key: "operador", label: "Operador", hrefKey: "href" },
              { key: "entradas", label: "Entradas", format: "number", hrefKey: "href" },
              { key: "qr", label: "QR", format: "number", hrefKey: "href" },
              { key: "manual", label: "Manual", format: "number", hrefKey: "href" },
              { key: "invalidas", label: "Inválidas", format: "number", hrefKey: "href" },
              { key: "duplicadas", label: "Duplicadas", format: "number", hrefKey: "href" },
              { key: "erro", label: "% erro", format: "percent" },
              { key: "manualidade", label: "% manual", format: "percent" },
            ]}
            pageSize={20}
          />
          <DataTable
            title="Status da portaria em tempo real"
            rows={analytics.liveStatusRows}
            columns={[
              { key: "evento", label: "Evento", hrefKey: "href" },
              { key: "iniciado", label: "Iniciado" },
              { key: "ultimaEntrada", label: "Última entrada" },
              { key: "ultimaInvalida", label: "Última inválida" },
              { key: "presentes", label: "Presentes", format: "number", hrefKey: "href" },
              { key: "ausentes", label: "Ausentes", format: "number", hrefKey: "href" },
              { key: "entrada", label: "% entrada", format: "percent" },
              { key: "pico", label: "Pico atual" },
            ]}
            pageSize={20}
          />
          <DataTable
            title="Lista de ausentes acionável"
            rows={analytics.absentRows}
            columns={[
              { key: "nome", label: "Nome", hrefKey: "href" },
              { key: "turma", label: "Turma" },
              { key: "lote", label: "Lote" },
              { key: "tipo", label: "Tipo" },
              { key: "quantidade", label: "Qtd.", format: "number" },
              { key: "compra", label: "Compra" },
              { key: "contato", label: "Contato" },
              { key: "qr", label: "Status do QR" },
              { key: "transferencia", label: "Transferência" },
            ]}
            pageSize={20}
          />
          <DataTable
            title="Ingressos ativos não utilizados"
            rows={analytics.unusedActiveRows}
            columns={[
              { key: "situacao", label: "Situação", hrefKey: "href" },
              { key: "nome", label: "Nome", hrefKey: "href" },
              { key: "turma", label: "Turma" },
              { key: "lote", label: "Lote" },
              { key: "quantidade", label: "Qtd.", format: "number" },
              { key: "qr", label: "QR" },
            ]}
            pageSize={20}
          />
          <DataTable
            title="Tentativas duplicadas com contexto"
            rows={analytics.duplicateContextRows}
            columns={[
              { key: "evento", label: "Evento", hrefKey: "href" },
              { key: "pessoa", label: "Quem tentou" },
              { key: "ingresso", label: "Ingresso" },
              { key: "primeira", label: "Primeira entrada" },
              { key: "segunda", label: "Segunda tentativa" },
              { key: "diferenca", label: "Dif. min", format: "decimal" },
              { key: "operador", label: "Operador" },
              { key: "acao", label: "Ação" },
            ]}
            pageSize={20}
          />
          <DataTable
            title="Comparativo entre eventos da mesma entidade"
            rows={analytics.portariaEventComparisonRows}
            columns={[
              { key: "evento", label: "Evento", hrefKey: "href" },
              { key: "aprovados", label: "Aprovados", format: "number" },
              { key: "presentes", label: "Presentes", format: "number", hrefKey: "href" },
              { key: "presenca", label: "Presença", format: "percent" },
              { key: "pico", label: "Pico" },
              { key: "manualidade", label: "Manualidade", format: "percent" },
              { key: "invalidas", label: "Inválidas", format: "number" },
            ]}
            pageSize={20}
          />
          <DataTable
            title="Alertas específicos da portaria"
            rows={analytics.portariaAlertRows}
            columns={[
              { key: "alerta", label: "Alerta", hrefKey: "href" },
              { key: "descricao", label: "O que significa" },
              { key: "impacto", label: "Impacto" },
              { key: "quantidade", label: "Quantidade", format: "number", hrefKey: "href" },
            ]}
            pageSize={20}
          />
        </section>
      ) : null}

      {view === "estrategico" ? (
        <section className="space-y-5">
          <KpiGrid>
            <KpiCard label="Receita total" value={formatCurrency(analytics.grossRevenue)} hint="Ingressos + produtos do Modo Vendas aprovados" icon={<DollarSign size={18} />} />
            <KpiCard label="Receita de ingressos" value={formatCurrency(analytics.ticketRevenue)} hint={`${formatPercent(analytics.ticketRevenueShare)} da receita`} icon={<Ticket size={18} />} />
            <KpiCard label="Receita de produtos" value={formatCurrency(analytics.productRevenue)} hint={`${formatPercent(analytics.productRevenueShare)} da receita`} icon={<Package size={18} />} />
            <KpiCard label="Score estratégico" value={strategicScoreLabel} hint={analytics.strategicDecision} icon={<Target size={18} />} />
          </KpiGrid>
          <KpiGrid>
            <KpiCard label="Receita por comprador" value={formatCurrency(analytics.totalRevenuePerBuyer)} hint={`${formatNumber(analytics.uniqueBuyers)} compradores únicos`} icon={<Users size={18} />} />
            <KpiCard label="Receita por check-in" value={formatCurrency(analytics.totalRevenuePerPresent)} hint={`${formatCurrency(analytics.productRevenuePerPresent)} em produtos do Modo Vendas`} icon={<TrendingUp size={18} />} />
            <KpiCard label="Taxa check-in → produto" value={formatPercent(safeDivide(analytics.productPresentBuyerIds, Math.max(analytics.checkedInTicketBuyerCount, 1)) * 100)} hint={`${formatNumber(analytics.productPresentBuyerIds)} usuários com check-in e produto`} icon={<ShoppingBag size={18} />} />
            <KpiCard label="Produto por check-in" value={formatDecimal(analytics.productPerPresent)} hint="Itens vendidos no Modo Vendas por check-in" icon={<BarChart3 size={18} />} />
          </KpiGrid>
          <KpiGrid>
            <KpiCard label="Compradores recorrentes" value={formatPercent(analytics.strategicRecurringRate)} hint={`${formatNumber(analytics.strategicRecurringBuyers)} recorrentes em eventos anteriores`} icon={<TrendingUp size={18} />} />
            <KpiCard label="Público sem consumo" value={formatNumber(analytics.ticketWithoutProduct)} hint="Fez check-in, mas não comprou produto" icon={<AlertTriangle size={18} />} />
            <KpiCard label="Produtos sem ingresso" value={formatNumber(analytics.productWithoutTicket)} hint="Compra de produto sem ingresso no recorte" icon={<Package size={18} />} />
            <KpiCard label="Resultado com custo" value={formatCurrency(analytics.resultWithoutCosts - analytics.eventCostsTotal)} hint={strategicCostHint} icon={<DollarSign size={18} />} />
          </KpiGrid>
          <div className="grid gap-4 xl:grid-cols-2">
            <ChartPanel
              title="Score estratégico"
              subtitle="0 a 100 para repetir, ajustar ou repensar"
              info={"O score é a média ponderada dos eventos do recorte.\n\nPor evento, a nota usa: receita total (20%), volume de check-ins (15%), taxa de check-in sobre ingressos aprovados (15%), receita de produto por check-in (15%), conversão de compradores de ingresso para produto (15%), recorrência (8%), baixa manual/operacionalidade (6%) e pendências de retirada (6%).\n\n0-39 = repensar, 40-69 = ajustar, 70-84 = repetir, 85-100 = repetir e escalar."}
            >
              <ScoreGauge score={analytics.strategicScore} label={analytics.strategicDecision} />
            </ChartPanel>
            <ChartPanel title="Receita por origem" subtitle="Barras: ingressos x produtos do Modo Vendas">
              <ColumnBars data={analytics.revenueOriginRows} valueName="Receita" valueFormat="currency" />
            </ChartPanel>
            <ChartPanel title="Receita por check-in" subtitle="Check-ins, receita total por check-in e produto por check-in">
              <ComboBarsLines data={analytics.revenuePerPresentRows} barName="Check-ins" lineOneName="Receita por check-in" lineTwoName="Produto por check-in" valueFormat="currency" secondaryFormat="currency" />
            </ChartPanel>
            <ChartPanel title="Funil ingresso → produto" subtitle="Usuários em cascata: aprovado → check-in → compra de produto → retirada">
              <FunnelMetric data={analytics.strategicFunnelRows} />
            </ChartPanel>
            <ChartPanel
              title="Matriz evento x resultado"
              subtitle="X: presença, Y: receita, bolha: receita de produtos do Modo Vendas, cor: score"
              info={"Cada bolinha é um evento. O eixo X mostra a taxa de check-in: check-ins divididos por ingressos aprovados. O eixo Y mostra a receita total do evento. O tamanho da bolha é a receita de produtos do Modo Vendas. A cor vem do score estratégico: vermelho repensar, amarelo ajustar, azul repetir, verde escalar."}
            >
              <BubbleMetric data={analytics.strategicBubbleRows} />
            </ChartPanel>
            <ChartPanel
              title="Radar hexagonal"
              subtitle="Ingresso, check-in, produtos, recorrência, operação e auditoria"
              info={"Ingresso: participação da receita de ingressos na receita total.\nCheck-in: check-ins divididos por ingressos aprovados.\nProdutos: participação da receita de produtos do Modo Vendas na receita total.\nRecorrência: compradores recorrentes divididos pelos compradores únicos.\nOperação: reduz a nota quando há muitos alertas operacionais por pedido aprovado.\nAuditoria: reduz a nota com duplicidade, leitura inválida e alerta operacional."}
            >
              <RadarMetric data={analytics.strategicRadarRows} />
            </ChartPanel>
            <ChartPanel title="Pareto de produtos" subtitle="Ordem do mais vendido para o menos vendido, com acumulado">
              <ParetoMetric data={analytics.attachRateRows} />
            </ChartPanel>
            <ChartPanel title="Produtos que tornam eventos melhores" subtitle="Heatmap evento x produto por receita">
              <HeatmapMetric data={analytics.eventProductHeatmapRows} />
            </ChartPanel>
            <ChartPanel title="Categoria estratégica" subtitle="Heatmap evento x categoria por receita">
              <HeatmapMetric data={analytics.eventCategoryHeatmapRows} />
            </ChartPanel>
            <ChartPanel title="Antecedência de ingressos" subtitle="Somente ingressos aprovados, sem misturar produtos">
              <ComboBarsLines data={analytics.ticketLeadRows} barName="Ingressos" lineOneName="Receita de ingressos" valueFormat="currency" secondaryFormat="number" sortBy="none" />
            </ChartPanel>
            <ChartPanel title="Antecedência de produtos" subtitle="Somente produtos do Modo Vendas">
              <ComboBarsLines data={analytics.productLeadRows} barName="Itens" lineOneName="Receita de produtos" valueFormat="currency" secondaryFormat="number" sortBy="none" />
            </ChartPanel>
            <ChartPanel
              title="Novos x recorrentes"
              subtitle="Baseado em eventos anteriores ao evento filtrado"
              info={"Ingresso novo: usuário que fez check-in no evento filtrado e não tinha check-in em evento anterior.\nIngresso recorrente: usuário que fez check-in no evento filtrado e também tinha check-in em evento anterior.\nProduto novo: usuário que comprou produto no Modo Vendas do evento filtrado e nunca tinha comprado produto em evento anterior.\nProduto recorrente: usuário que comprou produto no Modo Vendas do evento filtrado e já tinha comprado produto em evento anterior."}
            >
              <PieMetric data={analytics.recurrenceDetailRows} />
            </ChartPanel>
            <ChartPanel title="Participação na tenant" subtitle="Percentual da base total de usuários com check-in no evento">
              <BarsDual data={analytics.tenantParticipationRows} valueName="% da tenant" quantityName="Usuários" valueFormat="percent" quantityFormat="number" />
            </ChartPanel>
            <ChartPanel title="Ticket por cliente" subtitle="Histograma de receita total por cliente">
              <BarsDual data={analytics.customerTicketHistogramRows} valueName="Receita" quantityName="Clientes" />
            </ChartPanel>
            <ChartPanel title="Turma que comparece e consome" subtitle="Check-ins com linha de consumo médio">
              <ComboBarsLines data={analytics.classConsumptionRows} barName="Check-ins" lineOneName="Consumo médio" lineTwoName="No-show" valueFormat="currency" secondaryFormat="number" />
            </ChartPanel>
            <ChartPanel title="Lote que comparece e consome" subtitle="Presença por lote com consumo médio">
              <ComboBarsLines data={analytics.lotConsumptionRows} barName="Check-ins" lineOneName="Consumo médio" lineTwoName="No-show" valueFormat="currency" secondaryFormat="number" />
            </ChartPanel>
            <ChartPanel title="Origem que traz resultado" subtitle="Treemap por receita e qualidade de presença/retirada">
              <TreemapMetric data={analytics.sourceTreemapRows} />
            </ChartPanel>
            <ChartPanel title="Impacto de descontos" subtitle="Com desconto versus sem desconto">
              <BarsDual data={analytics.discountImpactRows} valueName="Receita" quantityName="Itens" />
            </ChartPanel>
            <ChartPanel
              title="Estratégia de preço"
              subtitle="Preço médio, presença/retirada e receita"
              info={"Cada bolinha representa uma faixa de preço de ingresso ou um produto do Modo Vendas. O eixo X é o preço médio. O eixo Y é a conversão: presença para ingressos e retirada para produtos. O tamanho da bolha é a receita gerada. A cor mostra o score relativo dentro do recorte."}
            >
              <BubbleMetric data={analytics.priceStrategyRows} xLabel="Preço médio" yLabel="Conversão" xFormat="currency" yFormat="percent" />
            </ChartPanel>
            <ChartPanel
              title="Forecast estratégico"
              subtitle="Linha real e projeção simples"
              info={"Pedidos são os ingressos e produtos aprovados por dia, em ordem cronológica. Real acumulado soma a receita realizada até cada dia. Projetado usa uma projeção simples: se existe uma receita esperada pelo ritmo atual, distribui a diferença restante pelos dias do recorte; se não existe base suficiente, a linha projetada acompanha o realizado. Não é previsão estatística avançada."}
            >
              <ComboBarsLines data={analytics.forecastRows} barName="Pedidos" lineOneName="Real acumulado" lineTwoName="Projetado" valueFormat="currency" secondaryFormat="currency" sortBy="none" />
            </ChartPanel>
            <ChartPanel title="Resultado com custo" subtitle="Cascata de receita, descontos, custos e resultado">
              <WaterfallMetric data={analytics.resultWaterfallRows} />
            </ChartPanel>
          </div>
          <DataTable
            title="Ranking de eventos para repetir, ajustar ou cancelar"
            rows={analytics.strategicEventRows}
            columns={[
              { key: "evento", label: "Evento", hrefKey: "href" },
              { key: "ingressos", label: "Ingressos", format: "number" },
              { key: "checkins", label: "Check-ins", format: "number" },
              { key: "presenca", label: "Taxa check-in", format: "percent" },
              { key: "receitaIngressos", label: "Receita ingressos", format: "currency" },
              { key: "receitaProdutos", label: "Receita produtos", format: "currency", hrefKey: "hrefProdutos" },
              { key: "receitaTotal", label: "Receita total", format: "currency" },
              { key: "produtoPorPresente", label: "Produto/check-in", format: "currency" },
              { key: "ticketTotalCliente", label: "Ticket cliente", format: "currency" },
              { key: "retirada", label: "Retirada", format: "percent" },
              { key: "pendencias", label: "Pendências", format: "number", hrefKey: "hrefProdutos" },
              { key: "score", label: "Score", format: "number" },
              { key: "decisao", label: "Decisão" },
              { key: "motivo", label: "Motivo" },
            ]}
            pageSize={20}
          />
          <DataTable
            title="Top 5 clientes que mais gastaram por evento"
            rows={analytics.topCustomersByEventRows}
            columns={[
              { key: "evento", label: "Evento" },
              { key: "posicao", label: "#", format: "number" },
              { key: "cliente", label: "Cliente", hrefKey: "href" },
              { key: "receitaTotal", label: "Receita total", format: "currency" },
              { key: "ingresso", label: "Ingresso", format: "currency" },
              { key: "produto", label: "Produto", format: "currency" },
              { key: "itens", label: "Itens", format: "number" },
            ]}
            pageSize={25}
          />
        </section>
      ) : null}

      {false && view === "estrategico" ? (
        <section className="space-y-5">
          <KpiGrid>
            <KpiCard label="Compradores únicos" value={formatNumber(analytics.uniqueBuyers)} hint="Pessoas diferentes no filtro" icon={<Users size={18} />} />
            <KpiCard label="Taxa de recorrência" value={formatPercent(analytics.recurringRate)} hint={`${formatNumber(analytics.recurringBuyers)} compradores recorrentes`} icon={<TrendingUp size={18} />} />
            <KpiCard label="Previsão simples" value={analytics.projectedRevenue ? formatCurrency(analytics.projectedRevenue) : "Sem ritmo"} hint="Estimativa simples, não estatística avançada" icon={<BarChart3 size={18} />} />
            <KpiCard label="Resultado sem custos" value={formatCurrency(analytics.resultWithoutCosts)} hint={strategicCostHint} icon={<DollarSign size={18} />} />
          </KpiGrid>
          <div className="grid gap-4 xl:grid-cols-2">
            <ChartPanel title="Antecedência da compra" subtitle="Distância entre pedido e evento">
              <Bars data={analytics.leadRows} />
            </ChartPanel>
            <ChartPanel
              title="Novos x recorrentes"
              subtitle="Compradores únicos e recompra"
              info="Novo é quem aparece uma única vez no histórico filtrado. Recorrente é quem comprou mais de uma vez no conjunto de ingressos e pedidos carregado para o período."
            >
              <PieMetric data={analytics.recurrenceRows} />
            </ChartPanel>
          </div>
          <DataTable
            title="Eventos para repetir ou ajustar"
            rows={analytics.eventDecisionRows}
            columns={[
              { key: "evento", label: "Evento" },
              { key: "ingressos", label: "Ingressos", format: "number" },
              { key: "receita", label: "Receita", format: "currency" },
              { key: "ticket", label: "Ticket médio", format: "currency" },
              { key: "presença", label: "Presença", format: "percent" },
              { key: "decisão", label: "Decisão" },
            ]}
          />
        </section>
      ) : null}

      {view === "vendas" ? (
        <section className="space-y-5">
          <KpiGrid>
            <KpiCard label="Receita aprovada" value={formatCurrency(analytics.productRevenue)} hint="Modo vendas aprovado" icon={<DollarSign size={18} />} />
            <KpiCard label="Receita retirada" value={formatCurrency(analytics.redeemedValue)} hint="Valor já entregue ou baixado" icon={<CheckCircle2 size={18} />} />
            <KpiCard label="Receita pendente" value={formatCurrency(analytics.pendingRedeemValue)} hint="Dinheiro recebido sem entrega registrada" icon={<AlertTriangle size={18} />} />
            <KpiCard label="Saúde operacional" value={salesHealthValue} hint={salesHealthLabel} icon={<Target size={18} />} />
          </KpiGrid>
          <KpiGrid>
            <KpiCard label="Itens vendidos" value={formatNumber(analytics.approvedProductQuantity)} hint={`${formatNumber(analytics.redeemedItems)} retirados`} icon={<Package size={18} />} />
            <KpiCard label="Itens pendentes" value={formatNumber(analytics.pendingRedeemItems)} hint={`${formatNumber(analytics.pendingRedeemOrders)} pedidos com saldo`} icon={<Clock3 size={18} />} />
            <KpiCard label="Retirada parcial" value={formatNumber(analytics.partialRedeemOrders)} hint="Pedidos aprovados com entrega incompleta" icon={<ShoppingBag size={18} />} />
            <KpiCard label="Taxa de retirada" value={formatPercent(analytics.withdrawalRate)} hint={`${formatNumber(analytics.redeemedItems)} de ${formatNumber(analytics.approvedProductQuantity)} itens`} icon={<QrCode size={18} />} />
          </KpiGrid>
          <KpiGrid>
            <KpiCard label="Baixa manual" value={formatPercent(analytics.manualWithdrawalRate)} hint="Baixas manuais / total de baixas" icon={<AlertTriangle size={18} />} />
            <KpiCard label="Tempo até retirada" value={`${formatDecimal(analytics.averageWithdrawalHours)}h`} hint="Mediana entre aprovação e baixa" icon={<Clock3 size={18} />} />
            <KpiCard label="Maior pendência" value={`${formatDecimal(analytics.maxPendingRedeemHours)}h`} hint={analytics.oldestPendingOrderName} icon={<AlertTriangle size={18} />} />
            <KpiCard label="Valor médio por item" value={formatCurrency(safeDivide(analytics.productRevenue, analytics.approvedProductQuantity))} hint="Receita aprovada / itens" icon={<BarChart3 size={18} />} />
          </KpiGrid>
          <div className="grid gap-4 xl:grid-cols-2">
            <ChartPanel title="Score do modo vendas" subtitle="Retirada, QR, pendências, auditoria e tempo">
              <ScoreGauge score={analytics.salesHealthScore} label={salesHealthLabel} />
            </ChartPanel>
            <ChartPanel title="Fila de retirada" subtitle="Retirado, pendente, parcial e cancelado/estornado">
              <SemiDonutMetric data={analytics.withdrawalStatusRows} valueFormat="currency" />
            </ChartPanel>
            <ChartPanel title="Aging dos pendentes" subtitle="Tempo desde aprovação sem retirada completa">
              <BarsDual data={analytics.pendingRedeemAgingRows} valueName="Valor pendente" quantityName="Itens pendentes" />
            </ChartPanel>
            <ChartPanel
              title="Curva venda x retirada"
              subtitle="Vendas aprovadas, retiradas e pendentes acumulados por horário"
              footer={<FilterLinkChips links={salesWithdrawalLegendLinks} />}
            >
              <ComboBarsLines data={analytics.salesWithdrawalTimelineRows} barName="Vendas" lineOneName="Retiradas" lineTwoName="Pendentes" valueFormat="number" secondaryFormat="number" sortBy="none" />
            </ChartPanel>
            <ChartPanel title="Retirada por produto" subtitle="Barras 100%: retirado, pendente, parcial e cancelado" footer={<FilterLinkChips links={salesWithdrawalLegendLinks} />}>
              <StackedPercentChart data={analytics.productWithdrawalRows} />
            </ChartPanel>
            <ChartPanel title="Retirada por categoria" subtitle="Qual categoria entrega melhor" footer={<FilterLinkChips links={salesWithdrawalLegendLinks} />}>
              <StackedPercentChart data={analytics.categoryWithdrawalRows} />
            </ChartPanel>
            <ChartPanel
              title="Receita por categoria"
              subtitle="Rosca de produtos/fichas/bar"
              toolbar={
                <div className="inline-flex overflow-hidden rounded-lg border border-zinc-800 bg-black/40 text-[10px] font-black uppercase">
                  {[
                    { id: "value" as const, label: "Valor" },
                    { id: "quantity" as const, label: "Itens" },
                  ].map((option) => (
                    <button
                      key={option.id}
                      type="button"
                      onClick={() => setCategoryRevenueMode(option.id)}
                      className={`px-3 py-2 transition ${
                        categoryRevenueMode === option.id ? "bg-emerald-400 text-black" : "text-zinc-400 hover:text-white"
                      }`}
                    >
                      {option.label}
                    </button>
                  ))}
                </div>
              }
            >
              <PieMetric
                data={analytics.categoryRows}
                dataKey={categoryRevenueMode}
                valueName={categoryRevenueMode === "value" ? "Receita" : "Itens"}
                valueFormat={categoryRevenueMode === "value" ? "currency" : "number"}
              />
            </ChartPanel>
            <ChartPanel
              title="Risco operacional do produto"
              subtitle="Volume, receita, pendência, manualidade, atraso e auditoria"
              info={"Volume: compara os itens vendidos no Modo Vendas com o maior volume do recorte.\nReceita: participação da receita de produtos na receita total aprovada.\nPendência: percentual de itens aprovados que ainda não foram retirados.\nManualidade: percentual de baixas feitas manualmente.\nAtraso: maior tempo de espera entre aprovação e retirada pendente, usando 24h como referência.\nAuditoria: quantidade de alertas de auditoria dividida pelos pedidos aprovados de produto."}
            >
              <RadarMetric data={analytics.productRiskRadarRows} />
            </ChartPanel>
            <ChartPanel title="Método de retirada" subtitle="QR, manual, código curto, documento ou lista">
              <PieMetric data={analytics.withdrawalMethodRows} />
            </ChartPanel>
            <ChartPanel title="Erros de QR na retirada" subtitle="QR ausente, duplicado ou status incoerente">
              <Bars data={analytics.withdrawalErrorRows} />
            </ChartPanel>
            <ChartPanel title="Operador x método" subtitle="Heatmap de baixa por pessoa e método">
              <HeatmapMetric data={analytics.operatorMethodHeatmapRows} />
            </ChartPanel>
            <ChartPanel title="Retiradas por operador" subtitle="Baixas e valor entregue">
              <BarsDual data={analytics.withdrawalOperatorRows} valueName="Valor entregue" quantityName="Baixas" />
            </ChartPanel>
            <ChartPanel title="Origem do pedido com qualidade" subtitle="Checkout, manual, admin, PDV/bar e cortesia" footer={<FilterLinkChips links={salesWithdrawalLegendLinks} />}>
              <StackedPercentChart data={analytics.orderSourceQualityRows.map((row: TableRow) => ({ name: row.name, retirado: row.retirado, pendente: row.pendente, parcial: row.parcial, cancelado: 0 }))} />
            </ChartPanel>
            <ChartPanel title="Fonte de pagamento" subtitle="Receita por fonte para conciliação">
              <PieMetric data={analytics.paymentSourceRows} />
            </ChartPanel>
            <ChartPanel title="Descontos com impacto real" subtitle="Receita bruta, líquida e desconto">
              <BarsDual data={analytics.discountDetailedRows} valueName="Valor" quantityName="Pedidos" />
            </ChartPanel>
            <ChartPanel title="Venda por horário" subtitle="Itens vendidos, receita e retiradas" footer={<FilterLinkChips links={salesWithdrawalLegendLinks} />}>
              <ComboBarsLines data={analytics.salesWithdrawalTimelineRows} barName="Itens vendidos" lineOneName="Itens retirados" lineTwoName="Pendentes" valueFormat="number" secondaryFormat="number" sortBy="none" />
            </ChartPanel>
            <ChartPanel title="Mapa produto x horário" subtitle="Heatmap de quantidade vendida por hora">
              <HeatmapMetric data={analytics.productHourHeatmapRows} columnOrder="hour" />
            </ChartPanel>
            <ChartPanel title="Estoque e ruptura" subtitle="Vendido, retirado, pendente e disponível">
              <ComboBarsLines data={analytics.turnoverRows} barName="Vendido" lineOneName="Giro do estoque" lineTwoName="Disponível" valueFormat="percent" secondaryFormat="number" />
            </ChartPanel>
            <ChartPanel title="Combos e venda cruzada" subtitle="Produtos comprados juntos">
              <NetworkMetric data={analytics.crossSellRows} />
            </ChartPanel>
            <ChartPanel title="Ticket do modo vendas" subtitle="Histograma de ticket por cliente em produto/bar">
              <BarsDual data={analytics.productTicketHistogramRows} valueName="Receita" quantityName="Clientes" />
            </ChartPanel>
            <ChartPanel title="Transferência no modo vendas" subtitle="Origem, destino e status de retirada">
              <HeatmapMetric data={analytics.productTransferRows} />
            </ChartPanel>
            <ChartPanel title="Status QR (Quick Response)" subtitle="Ativo, utilizado, cancelado, expirado, inválido ou sem QR">
              <PieMetric data={analytics.qrStatusRows} />
            </ChartPanel>
            <ChartPanel title="Receita aprovada x entregue" subtitle="Cascata operacional do modo vendas">
              <WaterfallMetric data={analytics.salesWaterfallRows} />
            </ChartPanel>
          </div>
          <DataTable
            title="Produtos, fichas e bar"
            rows={analytics.productRows}
            columns={[
              { key: "produto", label: "Produto" },
              { key: "itens", label: "Itens", format: "number" },
              { key: "receita", label: "Receita", format: "currency" },
              { key: "ticket", label: "Ticket", format: "currency" },
              { key: "retirados", label: "Retirados", format: "number" },
              { key: "pendentes", label: "Pendentes", format: "number" },
            ]}
            pageSize={20}
          />
          <DataTable
            title="Clientes com produto pendente"
            rows={analytics.pendingProductDetailRows}
            columns={[
              { key: "cliente", label: "Cliente", hrefKey: "href" },
              { key: "produto", label: "Produto", hrefKey: "href" },
              { key: "quantidade", label: "Qtd.", format: "number" },
              { key: "valor", label: "Valor", format: "currency" },
              { key: "aprovadoEm", label: "Aprovado em" },
              { key: "tempo", label: "Tempo (h)", format: "decimal" },
              { key: "origem", label: "Origem" },
              { key: "aprovador", label: "Aprovador" },
              { key: "qr", label: "Status QR" },
            ]}
            pageSize={20}
          />
          <DataTable
            title="Retiradas parciais"
            rows={analytics.partialWithdrawalRows}
            columns={[
              { key: "pedido", label: "Pedido", hrefKey: "href" },
              { key: "cliente", label: "Cliente" },
              { key: "produto", label: "Produto" },
              { key: "vendido", label: "Vendido", format: "number" },
              { key: "retirado", label: "Retirado", format: "number" },
              { key: "pendente", label: "Pendente", format: "number" },
              { key: "saldo", label: "Saldo", format: "currency" },
            ]}
            pageSize={20}
          />
          <DataTable
            title="Qualidade por operador de retirada"
            rows={analytics.operatorSalesRows}
            columns={[
              { key: "operador", label: "Operador", hrefKey: "href" },
              { key: "baixas", label: "Baixas", format: "number", hrefKey: "href" },
              { key: "valor", label: "Valor entregue", format: "currency" },
              { key: "manualidade", label: "Manualidade", format: "percent" },
              { key: "mediana", label: "Mediana (h)", format: "decimal" },
              { key: "conflitos", label: "Conflitos", format: "number" },
            ]}
            pageSize={20}
          />
          <DataTable
            title="Auditoria de conflito de função"
            rows={analytics.conflictAuditRows}
            columns={[
              { key: "gravidade", label: "Gravidade", hrefKey: "href" },
              { key: "pedido", label: "Pedido", hrefKey: "href" },
              { key: "cliente", label: "Cliente" },
              { key: "produto", label: "Produto" },
              { key: "valor", label: "Valor", format: "currency" },
              { key: "criado", label: "Criado por" },
              { key: "aprovado", label: "Aprovado por" },
              { key: "baixado", label: "Baixado por" },
            ]}
            pageSize={20}
          />
          <DataTable
            title="Fonte de pagamento e inconsistências"
            rows={analytics.paymentIssueRows}
            columns={[
              { key: "problema", label: "Problema" },
              { key: "quantidade", label: "Quantidade", format: "number" },
            ]}
          />
          <DataTable
            title="Estoque preparado para integração"
            rows={analytics.stockRows}
            columns={[
              { key: "produto", label: "Produto", hrefKey: "href" },
              { key: "estoque", label: "Estoque", format: "number" },
              { key: "vendido", label: "Vendido", format: "number" },
              { key: "retirado", label: "Retirado", format: "number" },
              { key: "pendente", label: "Pendente", format: "number" },
              { key: "disponivel", label: "Disponível", format: "number" },
              { key: "ruptura", label: "Ruptura", format: "number" },
            ]}
            pageSize={20}
          />
          <DataTable
            title="Tabela de auditoria melhorada"
            rows={analytics.improvedAuditRows}
            columns={[
              { key: "gravidade", label: "Gravidade", hrefKey: "href" },
              { key: "alerta", label: "Alerta", hrefKey: "href" },
              { key: "quantidade", label: "Quantidade", format: "number", hrefKey: "href" },
              { key: "acao", label: "Filtro", hrefKey: "href" },
            ]}
            pageSize={20}
          />
        </section>
      ) : null}

      {false && view === "vendas" ? (
        <section className="space-y-5">
          <KpiGrid>
            <KpiCard label="Receita aprovada" value={formatCurrency(analytics.productRevenue)} hint="Pedidos de produto/ficha aprovados" icon={<DollarSign size={18} />} />
            <KpiCard label="Receita retirada" value={formatCurrency(analytics.redeemedValue)} hint="Valor já entregue ou baixado" icon={<CheckCircle2 size={18} />} />
            <KpiCard label="Pendente de retirada" value={formatCurrency(analytics.pendingRedeemValue)} hint="Dinheiro recebido sem entrega registrada" icon={<AlertTriangle size={18} />} />
            <KpiCard label="Taxa de retirada" value={formatPercent(analytics.withdrawalRate)} hint={`${formatNumber(analytics.redeemedItems)} de ${formatNumber(analytics.approvedProductQuantity)} itens`} icon={<QrCode size={18} />} />
          </KpiGrid>
          <KpiGrid>
            <KpiCard label="Pedidos aprovados" value={formatNumber(analytics.approvedOrders.length)} hint="Modo Vendas" icon={<ShoppingBag size={18} />} />
            <KpiCard label="Itens vendidos" value={formatNumber(analytics.approvedProductQuantity)} hint="Quantidade aprovada" icon={<Package size={18} />} />
            <KpiCard label="Pedidos pendentes" value={formatNumber(analytics.pendingRedeemOrders)} hint="Aprovados sem baixa completa" icon={<Clock3 size={18} />} />
            <KpiCard label="Baixa manual" value={formatPercent(analytics.manualWithdrawalRate)} hint="Baixas manuais / total de baixas" icon={<AlertTriangle size={18} />} />
          </KpiGrid>
          <KpiGrid>
            <KpiCard label="Tempo até retirada" value={`${formatDecimal(analytics.averageWithdrawalHours)}h`} hint="Mediana entre aprovação e retirada" icon={<Clock3 size={18} />} />
            <KpiCard label="Itens retirados" value={formatNumber(analytics.redeemedItems)} hint="Baixa ou retirada realizada" icon={<CheckCircle2 size={18} />} />
            <KpiCard label="Itens pendentes" value={formatNumber(analytics.pendingRedeemItems)} hint="Aprovados sem retirada" icon={<Package size={18} />} />
            <KpiCard label="Valor médio por item" value={formatCurrency(safeDivide(analytics.productRevenue, analytics.approvedProductQuantity))} hint="Receita aprovada / itens" icon={<BarChart3 size={18} />} />
          </KpiGrid>
          <div className="grid gap-4 xl:grid-cols-2">
            <ChartPanel title="Produtos mais vendidos" subtitle="Quantidade, receita, retirados e pendentes">
              <BarsDual data={analytics.productChartRows} />
            </ChartPanel>
            <ChartPanel title="Categorias mais vendidas" subtitle="Drinks, comida, fichas, combos e demais categorias">
              <BarsDual data={analytics.categoryRows} />
            </ChartPanel>
            <ChartPanel title="Origem do pedido" subtitle="Checkout, manual, cortesia, PDV/bar ou admin">
              <PieMetric data={analytics.orderSourceRows} />
            </ChartPanel>
            <ChartPanel title="Método de retirada" subtitle="QR code, manual, código curto, documento ou lista">
              <PieMetric data={analytics.withdrawalMethodRows} />
            </ChartPanel>
            <ChartPanel title="Descontos por origem" subtitle="Total e quantidade por fonte">
              <BarsDual data={analytics.discountRows} valueName="Desconto" quantityName="Pedidos" />
            </ChartPanel>
            <ChartPanel title="Retiradas por operador" subtitle="Quem deu baixa e valor entregue">
              <BarsDual data={analytics.withdrawalOperatorRows} valueName="Valor entregue" quantityName="Baixas" />
            </ChartPanel>
          </div>
          <DataTable
            title="Produtos, fichas e bar"
            rows={analytics.productRows}
            columns={[
              { key: "produto", label: "Produto" },
              { key: "itens", label: "Itens", format: "number" },
              { key: "receita", label: "Receita", format: "currency" },
              { key: "ticket", label: "Ticket", format: "currency" },
              { key: "retirados", label: "Retirados", format: "number" },
              { key: "pendentes", label: "Pendentes", format: "number" },
            ]}
          />
          <DataTable
            title="Alertas de auditoria"
            rows={analytics.auditRows}
            columns={[
              { key: "alerta", label: "Alerta" },
              { key: "quantidade", label: "Quantidade", format: "number" },
            ]}
          />
        </section>
      ) : null}
    </DashboardShell>
  );
}
