"use client";

import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
  type RefObject,
} from "react";
import Image from "next/image";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Html5Qrcode } from "html5-qrcode";
import {
  AlertTriangle,
  ArrowLeft,
  Calendar,
  Camera,
  ChevronLeft,
  Check,
  ChevronRight,
  ChevronsLeft,
  ChevronsRight,
  Download,
  Edit3,
  Image as ImageIcon,
  Layers3,
  Loader2,
  MapPin,
  MessageCircle,
  MoveVertical,
  Percent,
  Package,
  QrCode,
  RotateCcw,
  Save,
  ScanLine,
  Search,
  Trash2,
  UserPlus,
  Users,
  Wallet,
  X,
} from "lucide-react";

import { ImageResizeHelpLink } from "@/components/ImageResizeHelpLink";
import { LotNameSelector } from "@/components/LotNameSelector";
import { PaymentRecipientCheckboxList } from "@/components/PaymentRecipientCheckboxList";
import { PaymentReceiversManager } from "@/components/PaymentReceiversManager";
import { useAuth } from "@/context/AuthContext";
import { useTenantTheme } from "@/context/TenantThemeContext";
import { useToast } from "@/context/ToastContext";
import { normalizePaymentConfig, type CommercePaymentConfig, type CommerceTicketEntry } from "@/lib/commerceCatalog";
import {
  createAdminEventPoll,
  deleteAdminEventPoll,
  EVENT_POLL_OPTION_MAX_CHARS,
  EVENT_POLL_OPTION_MAX_COUNT,
  EVENT_POLL_QUESTION_MAX_CHARS,
  fetchAdminEventById,
  fetchAdminEventPolls,
  fetchAdminEventSalesPage,
  incrementEventPurchaseUserStats,
  isAdminEventPublicSlugAvailable,
  setAdminTicketPayment,
  updateAdminEventPoll,
  updateAdminEventPollOptions,
  upsertAdminEvent,
} from "@/lib/eventsNativeService";
import {
  getEventVisibilityBlock,
  type EventVisibilityBlock,
} from "@/lib/eventVisibilityBlock";
import { logActivity } from "@/lib/logger";
import { fetchLeagueById } from "@/lib/leaguesService";
import {
  fetchTenantPaymentRecipients,
  filterTenantPaymentRecipientsByIds,
  type TenantPaymentRecipientContext,
  type TenantPaymentRecipientOption,
} from "@/lib/paymentRecipients";
import { fetchPlanCatalog, type PlanRecord } from "@/lib/plansPublicService";
import {
  fetchEventPartyOrders,
  fetchEventPartyProducts,
  createManualEventPartyOrder,
  deleteEventPartyOrder,
  eventPartyVoucherStatusLabel,
  getEventPartyProductOrder,
  getEventPartyProductSection,
  getEventPartyOrderReference,
  getEventPartyVoucherEntries,
  getEventPartyVoucherSummary,
  markEventPartyOrderDelivered,
  normalizeEventPartyConfig,
  serializeEventPartyConfig,
  updateEventPartyOrder,
  updateEventPartyProductMeta,
  upsertEventPartyProduct,
  type EventPartyEvent,
  type EventPartyOrder,
  type EventPartyProduct,
} from "@/lib/eventPartyService";
import { getSupabaseClient } from "@/lib/supabase";
import {
  approveStoreOrder,
  setStoreOrderStatus,
} from "@/lib/storeService";
import {
  buildDraftAssetFileName,
  sanitizeStoragePathSegment,
  uploadImage,
  VERSIONED_PUBLIC_ASSET_CACHE_CONTROL,
} from "@/lib/upload";
import { withTenantSlug } from "@/lib/tenantRouting";
import {
  CPF_MAX_DIGITS,
  CPF_MASKED_MAX_LENGTH,
  formatCpfInput,
  isValidCpf,
  isValidEmail,
  hasValidPhoneLength,
  normalizeEmailInput,
  normalizePhoneInput,
  normalizePhoneToBrE164,
  PHONE_MAX_LENGTH,
} from "@/utils/contactFields";

export type EventWorkspaceSection =
  | "extrato"
  | "bi"
  | "lotes"
  | "ingressos"
  | "cupons"
  | "checkins"
  | "scan"
  | "edicao"
  | "ficha"
  | "ficha-pagamento"
  | "ficha-cadastro"
  | "ficha-produto"
  | "ficha-produtos"
  | "ficha-produtos-cadastro"
  | "enquetes"
  | "recebedores";

type EventSaleStatus = "ativo" | "em_breve" | "esgotado";
type EventStatus = "ativo" | "encerrado";
type CouponType = "valor" | "percentual";

type LotePlanPrice = {
  planId: string;
  planName: string;
  price: string;
};

interface EventLot {
  id: number;
  nome: string;
  preco: string;
  status: EventSaleStatus;
  descricao: string;
  quantidade: number;
  ordem: number;
  qrPorIngresso: number;
  invisivel: boolean;
  transferivel: boolean;
  validadeAtiva: boolean;
  inicioVendasData: string;
  inicioVendasHora: string;
  fimVendasData: string;
  fimVendasHora: string;
  planPrices: LotePlanPrice[];
}

interface EventCoupon {
  id: string;
  titulo: string;
  codigo: string;
  tipo: CouponType;
  valor: string;
  valorMinimo: string;
  valorMaximo: string;
  quantidadeDisponivel: number;
  usos: number;
  ativo: boolean;
  createdAt: string;
}

interface EventCheckinOperator {
  id: string;
  nome: string;
  email: string;
  ativo: boolean;
  createdAt: string;
}

interface AdminEventDataExtra {
  raw: Record<string, unknown>;
  cupons: EventCoupon[];
  checkinOperators: EventCheckinOperator[];
}

interface AdminEvent {
  id: string;
  titulo: string;
  data: string;
  hora: string;
  local: string;
  tipo: string;
  destaque: string;
  mapsUrl: string;
  imagem: string;
  imagePositionY: number;
  descricao: string;
  status: EventStatus;
  saleStatus: EventSaleStatus;
  isLowStock: boolean;
  lotes: EventLot[];
  pixChave: string;
  pixBanco: string;
  pixTitular: string;
  contatoComprovante: string;
  stats: Record<string, unknown> & { confirmados: number; talvez: number; likes: number };
  paymentConfig: CommercePaymentConfig | null;
  recipientUserIds: string[];
  dataExtra: AdminEventDataExtra;
  adminVisibilityBlock: EventVisibilityBlock | null;
}

interface EventSaleRow {
  id: string;
  userId: string;
  userName: string;
  userTurma: string;
  status: string;
  loteId: string;
  loteNome: string;
  quantidade: number;
  valorUnitario: string;
  valorTotal: string;
  dataSolicitacao: unknown;
  dataAprovacao: unknown;
  dataPagamento: unknown;
  aprovadoPor: string;
  paymentConfig: CommercePaymentConfig | null;
  rawData: Record<string, unknown>;
}

type EventStatementKind = "ingresso" | "produto";
type EventStatementTypeFilter = "todos" | EventStatementKind;
type EventStatementStatusFilter = "todos" | "aprovado" | "pendente" | "analise";
type EventStatementFlowFilter = "todos" | "pedido" | "aprovacao" | "checkin" | "retirada";

interface EventStatementRow {
  id: string;
  kind: EventStatementKind;
  status: string;
  statusFilter: "aprovado" | "pendente" | "analise" | "outro";
  userName: string;
  userTurma: string;
  itemName: string;
  loteNome: string;
  categoria: string;
  quantidade: number;
  valorTotal: number;
  descontoValor: string;
  descontoFonte: string;
  expectedValue: number;
  requestAt: unknown;
  approvalAt: unknown;
  approvedBy: string;
  approvalMethod: string;
  paymentSource: string;
  paymentAt: unknown;
  checkinAt: unknown;
  checkinBy: string;
  checkinMethod: string;
  checkinNote: string;
  checkinEditedAt: unknown;
  checkinEditedBy: string;
  createdBy: string;
  source: string;
  userCode: string;
  qrStatus: string;
  qrCode: string;
  transferInfo: string;
  transferAt: unknown;
  transferFromUserName: string;
  transferToUserName: string;
  transferByUserName: string;
  approvedNearEvent: boolean;
}

interface EventPartyManualUser {
  id: string;
  orderId: string;
  nome: string;
  cpf: string;
  telefone: string;
  email: string;
  numero: string;
  ra: string;
  turma: string;
  valorPorta: string;
  createdAt: string;
  createdByName: string;
}

interface EventPartyUserOption {
  id: string;
  nome: string;
  cpf: string;
  telefone: string;
  email: string;
  numero: string;
  ra: string;
  turma: string;
  source: "app" | "manual";
}

interface TicketCheckinRow {
  orderId: string;
  ticketLabel: string;
  ticketToken: string;
  holderName: string;
  holderTurma: string;
  loteNome: string;
  scannedAt: string;
  scannedByUserId: string;
  scannedByUserName: string;
  scannedByUserTurma: string;
  scannedByUserAvatar: string;
  scanSource: string;
}

interface PollOption {
  text: string;
  votes: number;
  creatorName?: string;
}

interface EventPoll {
  id: string;
  question: string;
  allowUserOptions: boolean;
  options: PollOption[];
}

const EVENT_TITLE_MAX_LENGTH = 120;
const EVENT_LOCATION_MAX_LENGTH = 140;
const EVENT_TYPE_MAX_LENGTH = 40;
const EVENT_DESCRIPTION_MAX_LENGTH = 1200;
const EVENT_PIX_FIELD_MAX_LENGTH = 140;
const EVENT_LOTE_NAME_MAX_LENGTH = 80;
const EVENT_COUPON_TITLE_MAX_LENGTH = 120;
const EVENT_COUPON_CODE_MAX_LENGTH = 60;
const EVENT_OPERATOR_NAME_MAX_LENGTH = 120;
const EVENT_OPERATOR_EMAIL_MAX_LENGTH = 160;
const SALES_PAGE_SIZE = 20;
const PARTY_USER_PAGE_SIZE = 10;
const EVENT_PUBLIC_SLUG_MAX_LENGTH = 20;
const PARTY_USER_ALPHA_GROUPS = [
  { id: "a-f", label: "A-F", from: "a", to: "f" },
  { id: "g-l", label: "G-L", from: "g", to: "l" },
  { id: "m-r", label: "M-R", from: "m", to: "r" },
  { id: "s-z", label: "S-Z", from: "s", to: "z" },
  { id: "todos", label: "Todos", from: "", to: "" },
] as const;
type EventPartyAlphaGroup = (typeof PARTY_USER_ALPHA_GROUPS)[number]["id"];

const SECTION_LABELS: Record<EventWorkspaceSection, string> = {
  extrato: "Extrato",
  bi: "BI",
  lotes: "Lotes",
  ingressos: "Ingressos",
  cupons: "Cupons",
  checkins: "Lista de presença",
  scan: "Scan",
  edicao: "Edição",
  ficha: "Modo Vendas",
  "ficha-pagamento": "Pagamentos",
  "ficha-cadastro": "Cadastro",
  "ficha-produto": "Produto",
  "ficha-produtos": "Produtos",
  "ficha-produtos-cadastro": "Cadastro",
  enquetes: "Enquetes",
  recebedores: "Recebedores",
};

const SECTION_ORDER: EventWorkspaceSection[] = [
  "edicao",
  "checkins",
  "scan",
  "ingressos",
  "extrato",
  "ficha",
  "enquetes",
  "recebedores",
  "bi",
  "lotes",
  "cupons",
];

const SECTION_PATHS: Record<EventWorkspaceSection, string> = {
  extrato: "extrato",
  bi: "bi",
  lotes: "lotes",
  ingressos: "ingressos",
  cupons: "cupons",
  checkins: "checkins",
  scan: "scan",
  edicao: "edicao",
  ficha: "ficha",
  "ficha-pagamento": "ficha/pagamento",
  "ficha-cadastro": "ficha/cadastro",
  "ficha-produto": "ficha/produto",
  "ficha-produtos": "ficha/produtos",
  "ficha-produtos-cadastro": "ficha/produtos/cadastro",
  enquetes: "enquetes",
  recebedores: "recebedores",
};

const isEventPartyAdminSection = (section: EventWorkspaceSection): boolean =>
  section === "ficha" ||
  section === "ficha-pagamento" ||
  section === "ficha-cadastro" ||
  section === "ficha-produto" ||
  section === "ficha-produtos" ||
  section === "ficha-produtos-cadastro";

const saleStatusTone: Record<EventSaleStatus, string> = {
  ativo: "border-brand bg-brand-soft text-brand-accent",
  em_breve: "border-yellow-500/30 bg-yellow-500/10 text-yellow-200",
  esgotado: "border-red-500/30 bg-red-500/10 text-red-200",
};

const asRecord = (value: unknown): Record<string, unknown> | null =>
  typeof value === "object" && value !== null ? (value as Record<string, unknown>) : null;

const asString = (value: unknown, fallback = ""): string =>
  typeof value === "string" ? value : fallback;

const asBoolean = (value: unknown): boolean => Boolean(value);

const normalizeDigits = (value: string): string => value.replace(/\D/g, "");

const formatBrazilPhoneInput = (value: string): string => {
  const digits = normalizeDigits(value);
  const localDigits = (digits.startsWith("55") ? digits.slice(2) : digits).slice(0, 11);
  return localDigits ? `+55${localDigits}` : "";
};

const formatEmailCadastroInput = (value: string): string =>
  normalizeEmailInput(value).trim().toLowerCase();

const formatManualContactSummary = (entry: EventPartyManualUser): string => {
  const phone = formatBrazilPhoneInput(entry.telefone);
  return entry.email || phone || "-";
};

const extractSchemaFallbackColumn = (error: unknown): string => {
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

const normalizeSearch = (value: string): string =>
  value
    .trim()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase();

const normalizeLotDuplicateKey = (value: string): string =>
  normalizeSearch(value).replace(/[^a-z0-9]+/g, "");

const normalizePublicEventSlug = (value: string): string =>
  value
    .trim()
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^a-z0-9-]+/g, "-")
    .replace(/-+/g, "-")
    .replace(/^-|-$/g, "")
    .slice(0, EVENT_PUBLIC_SLUG_MAX_LENGTH);

const mapManualPartyUser = (value: unknown, index = 0): EventPartyManualUser | null => {
  const row = asRecord(value);
  if (!row) return null;
  const nome = asString(row.nome || row.name || row.userName).trim().slice(0, 120);
  const numero = asString(row.numero || row.number || row.externalNumber || row.ra).trim().slice(0, 80);
  if (!nome && !numero) return null;
  return {
    id:
      asString(row.id).trim() ||
      `manual-${normalizeSearch(numero || nome).replace(/[^a-z0-9]+/g, "-") || index}`,
    orderId: asString(row.orderId || row.ticketRequestId || row.saleId).trim(),
    nome: nome || "Usuário manual",
    cpf: normalizeDigits(asString(row.cpf)).slice(0, CPF_MAX_DIGITS),
    telefone: asString(row.telefone || row.phone).trim().slice(0, PHONE_MAX_LENGTH),
    email: asString(row.email).trim().slice(0, 160),
    numero,
    ra: asString(row.ra).trim().slice(0, 80),
    turma: asString(row.turma || row.className).trim().slice(0, 120),
    valorPorta: asString(row.valorPorta || row.gateValue || row.value).trim(),
    createdAt: asString(row.createdAt),
    createdByName: asString(row.createdByName),
  };
};

const mapManualSaleRowToManualUser = (saleRow: EventSaleRow): EventPartyManualUser | null => {
  const rowData = asRecord(saleRow.rawData.data) ?? {};
  if (!asBoolean(rowData.manualGateEntry)) return null;

  const firstTicket = saleRow.paymentConfig?.ticketEntries?.[0] ?? null;
  const rawBracelet =
    asString(rowData.pulseira) ||
    asString(rowData.braceletNumber) ||
    asString(firstTicket?.label).replace(/^pulseira\s+/i, "") ||
    asString(saleRow.rawData.userCode);
  const numero = rawBracelet.trim().slice(0, 80);
  const nome = saleRow.userName.trim().slice(0, 120);
  if (!nome && !numero) return null;

  return {
    id: saleRow.userId || `manual-sale-${saleRow.id}`,
    orderId: saleRow.id,
    nome: nome || "Usuário manual",
    cpf: normalizeDigits(asString(rowData.cpf)).slice(0, CPF_MAX_DIGITS),
    telefone: asString(rowData.telefone || rowData.phone).trim().slice(0, PHONE_MAX_LENGTH),
    email: asString(rowData.email).trim().slice(0, 160),
    numero,
    ra: asString(rowData.ra).trim().slice(0, 80),
    turma: saleRow.userTurma && saleRow.userTurma !== "-" ? saleRow.userTurma : "Porta",
    valorPorta: asString(rowData.valorPorta).trim() || saleRow.valorUnitario || "0,00",
    createdAt: asString(saleRow.rawData.createdAt || saleRow.dataSolicitacao),
    createdByName:
      asString(rowData.createdByName) ||
      saleRow.aprovadoPor ||
      asString(saleRow.rawData.checkinByUserName),
  };
};

const getManualPartyUsers = (dataExtra: unknown): EventPartyManualUser[] => {
  const eventParty = asRecord((asRecord(dataExtra) ?? {}).eventParty) ?? {};
  const rawUsers = Array.isArray(eventParty.manualUsers) ? eventParty.manualUsers : [];
  return rawUsers
    .map((entry, index) => mapManualPartyUser(entry, index))
    .filter((entry): entry is EventPartyManualUser => Boolean(entry));
};

const getPartyUserAlphaKey = (name: string): string =>
  normalizeSearch(name).replace(/[^a-z]/g, "").charAt(0);

const matchesPartyUserAlphaGroup = (name: string, group: EventPartyAlphaGroup): boolean => {
  if (group === "todos") return true;
  const first = getPartyUserAlphaKey(name);
  const config = PARTY_USER_ALPHA_GROUPS.find((entry) => entry.id === group);
  if (!first || !config || !config.from || !config.to) return false;
  return first >= config.from && first <= config.to;
};

const formatCurrency = (value: number): string =>
  value.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

const parseCurrency = (value: string): number => {
  const normalized = value.replace(/[^\d,.-]/g, "").replace(/\./g, "").replace(",", ".");
  const parsed = Number.parseFloat(normalized);
  return Number.isFinite(parsed) ? parsed : 0;
};

const parseMoneyValue = (value: unknown): number => {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  const text = asString(value).trim();
  if (!text) return 0;
  const parsed = Number(text);
  if (Number.isFinite(parsed)) return parsed;
  return parseCurrency(text);
};

const formatCurrencyTextInput = (value: string): string =>
  parseCurrency(value).toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

const formatDate = (value: string): string => {
  if (!value) return "-";
  const parsed = new Date(`${value}T00:00:00`);
  if (Number.isNaN(parsed.getTime())) return value;
  return parsed.toLocaleDateString("pt-BR");
};

const formatDateTime = (value: unknown): string => {
  if (!value) return "-";
  if (typeof value === "string" || typeof value === "number") {
    const parsed = new Date(value);
    if (!Number.isNaN(parsed.getTime())) {
      return parsed.toLocaleString("pt-BR");
    }
  }
  if (value instanceof Date) return value.toLocaleString("pt-BR");
  const row = value as { toDate?: unknown };
  if (typeof row?.toDate === "function") {
    const parsed = row.toDate();
    if (parsed instanceof Date && !Number.isNaN(parsed.getTime())) {
      return parsed.toLocaleString("pt-BR");
    }
  }
  return "-";
};

const parseDateTimeValue = (value: unknown): Date | null => {
  if (!value) return null;
  if (value instanceof Date && !Number.isNaN(value.getTime())) return value;
  if (typeof value === "string" || typeof value === "number") {
    const parsed = new Date(value);
    return Number.isNaN(parsed.getTime()) ? null : parsed;
  }
  const row = value as { toDate?: unknown };
  if (typeof row?.toDate === "function") {
    const parsed = row.toDate();
    return parsed instanceof Date && !Number.isNaN(parsed.getTime()) ? parsed : null;
  }
  return null;
};

const formatDateOnly = (value: unknown): string => {
  const parsed = parseDateTimeValue(value);
  return parsed ? parsed.toLocaleDateString("pt-BR") : "-";
};

const formatTimeOnly = (value: unknown): string => {
  const parsed = parseDateTimeValue(value);
  return parsed ? parsed.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" }) : "-";
};

const toCsvCell = (value: unknown): string =>
  `"${String(value ?? "").replace(/"/g, '""')}"`;

const formatLotValidityPeriod = (lot: EventLot): string => {
  if (!lot.validadeAtiva) return "Sem limite definido";
  const startDate = lot.inicioVendasData || "--";
  const startTime = lot.inicioVendasHora || "--:--";
  const endDate = lot.fimVendasData || "--";
  const endTime = lot.fimVendasHora || "--:--";
  return `${startDate} ${startTime} até ${endDate} ${endTime}`;
};

const parseEventStartDateTime = (event?: AdminEvent | null): Date | null => {
  if (!event?.data) return null;
  const time = event.hora?.trim() || "00:00";
  return parseDateTimeValue(`${event.data}T${time}`) || parseDateTimeValue(event.data);
};

const findEventLotPrice = (event: AdminEvent | null, lotId: string, lotName: string): number => {
  const cleanLotId = lotId.trim();
  const cleanLotName = normalizeSearch(lotName);
  const lot = event?.lotes.find((entry) => {
    const idMatches = cleanLotId && String(entry.id) === cleanLotId;
    const nameMatches = cleanLotName && normalizeSearch(entry.nome) === cleanLotName;
    return idMatches || nameMatches;
  });
  return lot ? parseCurrency(lot.preco) : Number.NaN;
};

const isApprovedNearEventStart = (event: AdminEvent | null, approvalAt: unknown): boolean => {
  const start = parseEventStartDateTime(event);
  const approval = parseDateTimeValue(approvalAt);
  if (!start || !approval) return false;
  const hoursBeforeEvent = (start.getTime() - approval.getTime()) / 36e5;
  return hoursBeforeEvent >= -1 && hoursBeforeEvent <= 2;
};

const getLatestDateTimeValue = (values: unknown[]): unknown => {
  let latestValue: unknown = null;
  let latestTime = Number.NEGATIVE_INFINITY;

  values.forEach((value) => {
    if (!value) return;
    const parsed =
      value instanceof Date
        ? value
        : typeof value === "string" || typeof value === "number"
          ? new Date(value)
          : null;
    if (!parsed || Number.isNaN(parsed.getTime())) return;
    if (parsed.getTime() > latestTime) {
      latestTime = parsed.getTime();
      latestValue = value;
    }
  });

  return latestValue;
};

const getSalePaymentDate = (row: EventSaleRow): unknown =>
  row.dataPagamento || row.dataAprovacao || null;

const getSaleLatestCheckinDate = (row: EventSaleRow): unknown =>
  getLatestDateTimeValue(
    (row.paymentConfig?.ticketEntries ?? [])
      .filter((entry) => entry.status === "lido" || Boolean(entry.scannedAt))
      .map((entry) => entry.scannedAt)
  );

const getSaleLatestCheckinEntry = (row: EventSaleRow) =>
  [...(row.paymentConfig?.ticketEntries ?? [])]
    .filter((entry) => entry.status === "lido" || Boolean(entry.scannedAt))
    .sort((left, right) => {
      const leftTime = parseDateTimeValue(left.scannedAt)?.getTime() ?? 0;
      const rightTime = parseDateTimeValue(right.scannedAt)?.getTime() ?? 0;
      return rightTime - leftTime;
    })[0] ?? null;

const countSaleCheckins = (row: EventSaleRow): number =>
  row.paymentConfig?.ticketEntries?.filter((entry) => entry.status === "lido" || Boolean(entry.scannedAt)).length ?? 0;

const saleHasCheckin = (row: EventSaleRow): boolean =>
  Boolean(row.rawData.checkinAt || getSaleLatestCheckinEntry(row));

const normalizeStatementStatus = (status: string): EventStatementRow["statusFilter"] => {
  const normalized = normalizeSearch(status);
  if (["aprovado", "approved", "paid", "pago", "delivered"].includes(normalized)) return "aprovado";
  if (["pendente", "pending"].includes(normalized)) return "pendente";
  if (["analise", "em analise", "review"].includes(normalized)) return "analise";
  return "outro";
};

const normalizeStatementTypeFilterParam = (value: string | null): EventStatementTypeFilter => {
  const normalized = normalizeSearch(value || "");
  if (normalized === "ingresso") return "ingresso";
  if (normalized === "produto") return "produto";
  return "todos";
};

const normalizeStatementStatusFilterParam = (value: string | null): EventStatementStatusFilter => {
  const normalized = normalizeSearch(value || "");
  if (normalized === "aprovado" || normalized === "pendente" || normalized === "analise") return normalized;
  return "todos";
};

const normalizeStatementAlertParam = (value: string | null): string => normalizeSearch(value || "").replace(/\s+/g, "-");
const normalizeStatementTextFilterParam = (value: string | null): string => {
  const trimmed = (value || "").trim();
  return trimmed || "todos";
};
const normalizeStatementFlowFilterParam = (value: string | null): EventStatementFlowFilter => {
  const normalized = normalizeSearch(value || "");
  if (normalized === "pedido") return "pedido";
  if (normalized === "aprovacao") return "aprovacao";
  if (normalized === "checkin" || normalized === "check in") return "checkin";
  if (normalized === "retirada") return "retirada";
  return "todos";
};
const normalizeStatementIndicatorParam = (value: string | null): string =>
  normalizeSearch(value || "").replace(/\s+/g, "-");

const normalizePresenceIndicatorParam = (value: string | null): string => {
  const normalized = normalizeStatementIndicatorParam(value);
  if (normalized === "ausente" || normalized === "sem-entrada" || normalized === "no-show") return "ausente";
  if (normalized === "presente" || normalized === "dentro" || normalized === "com-entrada") return "presente";
  if (normalized === "manual" || normalized === "checkin-manual") return "manual";
  return "";
};

const normalizeMethodLabel = (value: string, fallback = "-"): string => {
  const normalized = normalizeSearch(value);
  if (!normalized) return fallback;
  if (normalized.includes("qr") || normalized.includes("scan")) return "QR";
  if (normalized.includes("manual")) return "Manual";
  return value;
};

const isCourtesyStatementRow = (row: EventStatementRow): boolean =>
  normalizeSearch(`${row.itemName} ${row.loteNome} ${row.categoria} ${row.approvalMethod}`).includes("cortesia");

const hasStatementCode = (row: EventStatementRow): boolean => {
  const code = row.qrCode.trim();
  return Boolean(code && code !== "-");
};

const hasStatementUse = (row: EventStatementRow): boolean => Boolean(row.checkinAt);

const STATEMENT_FLOW_LABELS: Record<EventStatementFlowFilter, string> = {
  todos: "Todos os fluxos",
  pedido: "Pedido",
  aprovacao: "Aprovação",
  checkin: "Check-in",
  retirada: "Retirada",
};

const STATEMENT_ALERT_LABELS: Record<string, string> = {
  "aprovado-sem-valor": "Aprovado sem valor",
  "valor-zero-sem-cortesia": "Valor zero sem ser cortesia",
  "cortesia-com-valor": "Cortesia com valor maior que zero",
  "desconto-sem-origem": "Desconto sem origem registrada",
  "valor-diferente-tabela": "Valor aprovado diferente do preço do lote/produto",
  "manual-fora-padrao": "Pedido manual com valor fora do padrão",
  "preco-incompativel": "Ingresso/produto com preço incompatível com lote",
  "pagamento-sem-metodo": "Pagamento aprovado sem método registrado",
  "aprovado-sem-fonte-pagamento": "Pedido aprovado sem fonte de pagamento",
  "transferencia-valor-incompativel": "Transferência com valor incompatível",
  "aprovado-sem-codigo": "Pedido aprovado sem QR/código",
  "codigo-sem-uso": "Pedido com QR/código, mas sem uso",
  "uso-sem-aprovacao": "Pedido usado sem aprovação clara",
  "status-incoerente": "Pedido com status incoerente",
  "aprovado-perto-evento": "Pedido aprovado muito perto do horário do evento",
};

const statementAlertLabel = (alert: string): string => STATEMENT_ALERT_LABELS[alert] || alert;

const STATEMENT_INDICATOR_LABELS: Record<string, string> = {
  "sem-valor": "Aprovado sem valor",
  "aprovacao-manual": "Aprovação manual",
  "cancelado-pos-aprovacao": "Corrigido/cancelado após aprovação",
  "sem-uso": "Aprovado sem entrada/retirada",
  retirado: "Produto retirado/baixado",
  "pendente-retirada": "Produto pendente de retirada",
  "retirada-parcial": "Produto com retirada parcial",
  "mesmo-criador": "Criado e aprovado pela mesma pessoa",
  "mesmo-baixa": "Aprovado e baixado pela mesma pessoa",
  "pedido-manual": "Pedido/manual ou fora do checkout",
  "checkin-manual": "Check-in manual",
  "retirada-manual": "Retirada manual",
  "desconto-manual": "Desconto manual",
  "sem-comprovante": "Sem comprovante/fonte",
};

const statementIndicatorLabel = (indicator: string): string =>
  STATEMENT_INDICATOR_LABELS[indicator] || indicator;

const isManualStatementSource = (row: EventStatementRow): boolean => {
  const source = normalizeSearch(row.source || "");
  return (
    source.includes("manual") ||
    source.includes("admin") ||
    source.includes("porta") ||
    source.includes("criado") ||
    source.includes("cadastro")
  );
};

const matchesStatementIndicator = (row: EventStatementRow, indicator: string): boolean => {
  if (!indicator) return true;
  const approved = row.statusFilter === "aprovado";
  const approvalMethod = normalizeSearch(row.approvalMethod || "");
  const checkinMethod = normalizeSearch(row.checkinMethod || "");
  const paymentSource = normalizeSearch(row.paymentSource || "");
  const qrStatus = normalizeSearch(row.qrStatus || "");
  const sameCreator =
    row.createdBy !== "-" &&
    row.approvedBy !== "-" &&
    normalizeSearch(row.createdBy) === normalizeSearch(row.approvedBy);
  const sameCompletionOperator =
    row.checkinBy !== "-" &&
    row.approvedBy !== "-" &&
    normalizeSearch(row.checkinBy) === normalizeSearch(row.approvedBy);

  if (indicator === "sem-valor") return approved && row.valorTotal <= 0;
  if (indicator === "aprovacao-manual") return approved && (isManualStatementSource(row) || approvalMethod.includes("manual") || approvalMethod.includes("admin"));
  if (indicator === "cancelado-pos-aprovacao") return Boolean(row.approvalAt) && row.statusFilter !== "aprovado";
  if (indicator === "sem-uso") return approved && !hasStatementUse(row);
  if (indicator === "retirado") return row.kind === "produto" && approved && hasStatementUse(row);
  if (indicator === "pendente-retirada") return row.kind === "produto" && approved && !hasStatementUse(row);
  if (indicator === "retirada-parcial") {
    return row.kind === "produto" && approved && hasStatementUse(row) && (qrStatus.includes("ativo") || qrStatus.includes("pendente") || qrStatus.includes("parcial"));
  }
  if (indicator === "mesmo-criador") return approved && sameCreator;
  if (indicator === "mesmo-baixa") return approved && sameCompletionOperator;
  if (indicator === "pedido-manual") return isManualStatementSource(row);
  if (indicator === "checkin-manual") return row.kind === "ingresso" && checkinMethod.includes("manual");
  if (indicator === "retirada-manual") return row.kind === "produto" && checkinMethod.includes("manual");
  if (indicator === "desconto-manual") return parseCurrency(row.descontoValor || "0") > 0 && normalizeSearch(row.descontoFonte || "").includes("manual");
  if (indicator === "sem-comprovante") return approved && (!paymentSource || paymentSource === "-") && !isCourtesyStatementRow(row);
  return true;
};

const matchesStatementFlow = (row: EventStatementRow, flow: EventStatementFlowFilter): boolean => {
  if (flow === "todos") return true;
  if (flow === "pedido") return matchesStatementIndicator(row, "pedido-manual");
  if (flow === "aprovacao") return matchesStatementIndicator(row, "aprovacao-manual");
  if (flow === "checkin") return matchesStatementIndicator(row, "checkin-manual");
  if (flow === "retirada") return matchesStatementIndicator(row, "retirada-manual");
  return true;
};

const matchesStatementAlert = (row: EventStatementRow, alert: string): boolean => {
  if (!alert) return true;
  const approved = row.statusFilter === "aprovado";
  const value = row.valorTotal;
  const discount = parseCurrency(row.descontoValor || "0");
  const discountSource = normalizeSearch(row.descontoFonte || "");
  const method = normalizeSearch(row.approvalMethod || "");
  const paymentSource = normalizeSearch(row.paymentSource || "");
  const courtesy = isCourtesyStatementRow(row);
  const hasCode = hasStatementCode(row);
  const used = hasStatementUse(row);
  const expectedAfterDiscount = Number.isFinite(row.expectedValue)
    ? Math.max(0, row.expectedValue - discount)
    : Number.NaN;
  const valueMismatch = Number.isFinite(expectedAfterDiscount) && Math.abs(expectedAfterDiscount - value) > 0.01;

  if (alert === "aprovado-sem-valor") return approved && value <= 0;
  if (alert === "valor-zero-sem-cortesia") return approved && value <= 0 && !courtesy;
  if (alert === "cortesia-com-valor") return approved && courtesy && value > 0;
  if (alert === "desconto-sem-origem") return discount > 0 && (!discountSource || discountSource === "-");
  if (alert === "pagamento-sem-metodo") return approved && (!method || method === "-");
  if (alert === "aprovado-sem-fonte-pagamento") return approved && !courtesy && !isManualStatementSource(row) && (!paymentSource || paymentSource === "-");
  if (alert === "transferencia-valor-incompativel") return normalizeSearch(row.transferInfo).includes("transfer") && value > 0;
  if (alert === "aprovado-sem-codigo") return approved && !hasCode;
  if (alert === "codigo-sem-uso") return approved && hasCode && !used;
  if (alert === "uso-sem-aprovacao") return !approved && used;
  if (alert === "status-incoerente") return (approved && !row.approvalAt) || (!approved && used);
  if (alert === "aprovado-perto-evento") return approved && row.approvedNearEvent;
  if (alert === "valor-diferente-tabela") return approved && valueMismatch;
  if (alert === "manual-fora-padrao") return isManualStatementSource(row) && valueMismatch;
  if (alert === "preco-incompativel") return valueMismatch && discount <= 0;
  return true;
};

const resolveTicketCategory = (row: EventSaleRow): string => {
  const data = asRecord(row.rawData.data) ?? {};
  const raw =
    asString(row.rawData.categoria) ||
    asString(row.rawData.loteCategoria) ||
    asString(data.categoria) ||
    asString(data.loteCategoria);
  if (raw.trim()) return raw.trim();
  const lote = normalizeSearch(row.loteNome);
  if (lote.includes("nao aluno") || lote.includes("externo")) return "Não aluno";
  if (lote.includes("aluno")) return "Aluno";
  return "-";
};

const resolveTicketDiscount = (row: EventSaleRow): { value: string; source: string } => {
  const data = asRecord(row.rawData.data) ?? {};
  const persistedValue = asString(row.rawData.discountValue).trim();
  const persistedSource = asString(row.rawData.discountSource).trim();
  const persistedKind = asString(row.rawData.discountKind).trim();
  if (persistedSource || persistedKind) {
    return {
      value: persistedValue || "R$ 0,00",
      source: persistedSource || (persistedKind === "cupom" ? "Cupom" : persistedKind === "plano" ? "Plano" : "-"),
    };
  }
  const coupon =
    asString(row.rawData.cupom) ||
    asString(row.rawData.couponCode) ||
    asString(data.cupom) ||
    asString(data.couponCode);
  const plan =
    asString(row.rawData.plano) ||
    asString(row.rawData.planName) ||
    asString(data.plano) ||
    asString(data.planName);
  const discountValue =
    asString(row.rawData.desconto) ||
    asString(row.rawData.discountValue) ||
    asString(data.desconto) ||
    asString(data.discountValue) ||
    "R$ 0,00";
  if (coupon.trim()) return { value: discountValue, source: `Cupom ${coupon.trim()}` };
  if (plan.trim()) return { value: discountValue, source: `Plano ${plan.trim()}` };
  return { value: discountValue, source: "-" };
};

const resolveTicketProofUrl = (row: EventSaleRow): string => {
  const data = asRecord(row.rawData.data) ?? {};
  const candidates = [
    row.rawData.comprovanteUrl,
    row.rawData.paymentProofUrl,
    row.rawData.receiptUrl,
    row.rawData.comprovante,
    row.rawData.receipt,
    data.comprovanteUrl,
    data.paymentProofUrl,
    data.receiptUrl,
    data.comprovante,
    data.receipt,
  ];

  return candidates.map((entry) => asString(entry).trim()).find(Boolean) || "";
};

const formatTicketEntryCode = (entry: CommerceTicketEntry): string =>
  [entry.label, entry.id || entry.token]
    .map((value) => value.trim())
    .filter(Boolean)
    .join(": ");

const summarizeTicketCodes = (entries: CommerceTicketEntry[]): string =>
  entries.map(formatTicketEntryCode).filter(Boolean).join(" | ") || "-";

const summarizeTicketQrStatus = (entries: CommerceTicketEntry[], fallback: string): string => {
  const statuses = Array.from(
    new Set(
      entries.map((entry) =>
        entry.status === "lido" ? "Utilizado" : entry.status === "transferido" ? "Transferido" : "Ativo"
      )
    )
  );
  return statuses.join(" / ") || fallback || "-";
};

const summarizeTicketTransfers = (entries: CommerceTicketEntry[]): string =>
  entries
    .map((entry) => {
      if (entry.transferredToUserName) return `${entry.label}: transferido para ${entry.transferredToUserName}`;
      if (entry.transferredFromUserName) return `${entry.label}: recebido de ${entry.transferredFromUserName}`;
      return "";
    })
    .filter(Boolean)
    .join(" | ") || "-";

const resolveTicketTransferSummary = (entries: CommerceTicketEntry[]) => {
  const latest =
    [...entries]
      .filter(
        (entry) =>
          Boolean(entry.transferredAt) ||
          Boolean(entry.transferredFromUserName) ||
          Boolean(entry.transferredToUserName)
      )
      .sort((left, right) => {
        const leftTime = parseDateTimeValue(left.transferredAt)?.getTime() ?? 0;
        const rightTime = parseDateTimeValue(right.transferredAt)?.getTime() ?? 0;
        return rightTime - leftTime;
      })[0] ?? null;
  return {
    transferAt: latest?.transferredAt || null,
    transferFromUserName: latest?.transferredFromUserName || "-",
    transferToUserName: latest?.transferredToUserName || "-",
    transferByUserName: latest?.transferByUserName || latest?.transferredFromUserName || "-",
  };
};

const getPaymentRecipientIdsFromConfig = (
  paymentConfig?: CommercePaymentConfig | null
): string[] => {
  const rows =
    paymentConfig?.recipients?.length
      ? paymentConfig.recipients
      : paymentConfig?.recipient
        ? [paymentConfig.recipient]
        : [];

  return Array.from(
    new Set(
      rows
        .map((entry) => asString(entry.userId).trim())
        .filter((entry) => entry.length > 0)
    )
  );
};

const toCommerceRecipientSnapshot = (recipient: TenantPaymentRecipientOption) => ({
  userId: recipient.userId,
  name: recipient.name,
  turma: recipient.turma,
  avatarUrl: recipient.avatarUrl,
  phone: recipient.phone,
});

const buildLotePlanPrices = (
  plans: PlanRecord[],
  current?: LotePlanPrice[]
): LotePlanPrice[] => {
  const currentMap = new Map<string, string>();

  (current ?? []).forEach((entry) => {
    const normalizedPrice = String(entry.price ?? "").trim();
    const planIdKey = String(entry.planId || "").trim().toLowerCase();
    const planNameKey = String(entry.planName || "").trim().toLowerCase();
    if (planIdKey) currentMap.set(planIdKey, normalizedPrice);
    if (planNameKey) currentMap.set(planNameKey, normalizedPrice);
  });

  return plans.map((plan) => ({
    planId: plan.id,
    planName: plan.nome,
    price:
      currentMap.get((plan.id || "").trim().toLowerCase()) ||
      currentMap.get((plan.nome || "").trim().toLowerCase()) ||
      "",
  }));
};

const serializeLotePlanPrices = (
  plans: PlanRecord[],
  current?: LotePlanPrice[]
): LotePlanPrice[] =>
  buildLotePlanPrices(plans, current)
    .map((entry) => ({ ...entry, price: String(entry.price ?? "").trim() }))
    .filter((entry) => entry.price.length > 0);

const normalizeSaleStatus = (value: unknown): EventSaleStatus => {
  const raw = asString(value).trim().toLowerCase();
  if (raw === "em_breve" || raw === "agendado") return "em_breve";
  if (raw === "esgotado" || raw === "encerrado") return "esgotado";
  return "ativo";
};

const normalizeLote = (value: unknown, index: number): EventLot => {
  const row = asRecord(value) ?? {};
  return {
    id: Number.isFinite(Number(row.id)) ? Number(row.id) : Date.now() + index,
    nome: asString(row.nome, "Lote").slice(0, EVENT_LOTE_NAME_MAX_LENGTH),
    preco: asString(row.preco, ""),
    status: normalizeSaleStatus(row.status),
    descricao: asString(row.descricao, ""),
    quantidade: Math.max(0, Math.floor(Number(row.quantidade ?? 100) || 100)),
    ordem: Math.max(0, Math.floor(Number(row.ordem ?? index) || index)),
    qrPorIngresso: Math.max(1, Math.floor(Number(row.qrPorIngresso ?? row.qr_codes_por_ingresso ?? 1) || 1)),
    invisivel: asBoolean(row.invisivel ?? row.invisible),
    transferivel: row.transferivel === undefined ? true : asBoolean(row.transferivel),
    validadeAtiva: asBoolean(row.validadeAtiva ?? row.validityEnabled),
    inicioVendasData: asString(row.inicioVendasData ?? row.startDate),
    inicioVendasHora: asString(row.inicioVendasHora ?? row.startTime),
    fimVendasData: asString(row.fimVendasData ?? row.endDate),
    fimVendasHora: asString(row.fimVendasHora ?? row.endTime),
    planPrices: Array.isArray(row.planPrices)
      ? (row.planPrices as LotePlanPrice[])
      : Array.isArray(row.plan_prices)
        ? (row.plan_prices as LotePlanPrice[])
        : [],
  };
};

const createEmptyLot = (plans: PlanRecord[]): EventLot => ({
  id: Date.now(),
  nome: "",
  preco: "",
  status: "ativo",
  descricao: "",
  quantidade: 100,
  ordem: 0,
  qrPorIngresso: 1,
  invisivel: false,
  transferivel: true,
  validadeAtiva: false,
  inicioVendasData: "",
  inicioVendasHora: "",
  fimVendasData: "",
  fimVendasHora: "",
  planPrices: buildLotePlanPrices(plans),
});

const normalizeCoupon = (value: unknown, index: number): EventCoupon | null => {
  const row = asRecord(value);
  if (!row) return null;
  const titulo = asString(row.titulo, "").trim().slice(0, EVENT_COUPON_TITLE_MAX_LENGTH);
  const codigo = asString(row.codigo, "").trim().slice(0, EVENT_COUPON_CODE_MAX_LENGTH).toUpperCase();
  if (!titulo && !codigo) return null;
  return {
    id: asString(row.id, `coupon-${index + 1}`),
    titulo: titulo || "Cupom",
    codigo: codigo || `CUPOM${index + 1}`,
    tipo: asString(row.tipo).trim().toLowerCase() === "percentual" ? "percentual" : "valor",
    valor: asString(row.valor, ""),
    valorMinimo: asString(row.valorMinimo, ""),
    valorMaximo: asString(row.valorMaximo, ""),
    quantidadeDisponivel: Math.max(0, Math.floor(Number(row.quantidadeDisponivel ?? 100) || 100)),
    usos: Math.max(0, Math.floor(Number(row.usos ?? 0) || 0)),
    ativo: row.ativo === undefined ? true : asBoolean(row.ativo),
    createdAt: asString(row.createdAt, new Date().toISOString()),
  };
};

const createEmptyCoupon = (): EventCoupon => ({
  id: `coupon-${Date.now()}`,
  titulo: "",
  codigo: "",
  tipo: "valor",
  valor: "",
  valorMinimo: "",
  valorMaximo: "",
  quantidadeDisponivel: 100,
  usos: 0,
  ativo: true,
  createdAt: new Date().toISOString(),
});

const normalizeCheckinOperator = (value: unknown, index: number): EventCheckinOperator | null => {
  const row = asRecord(value);
  if (!row) return null;
  const nome = asString(row.nome, "").trim().slice(0, EVENT_OPERATOR_NAME_MAX_LENGTH);
  const email = asString(row.email, "").trim().slice(0, EVENT_OPERATOR_EMAIL_MAX_LENGTH);
  if (!nome && !email) return null;
  return {
    id: asString(row.id, `checkin-${index + 1}`),
    nome: nome || "Operador",
    email,
    ativo: row.ativo === undefined ? true : asBoolean(row.ativo),
    createdAt: asString(row.createdAt, new Date().toISOString()),
  };
};

const createEmptyCheckinOperator = (): EventCheckinOperator => ({
  id: `checkin-${Date.now()}`,
  nome: "",
  email: "",
  ativo: true,
  createdAt: new Date().toISOString(),
});

const normalizeDataExtra = (value: unknown): AdminEventDataExtra => {
  const raw = asRecord(value) ?? {};
  return {
    raw,
    cupons: Array.isArray(raw.coupons)
      ? raw.coupons
          .map((entry, index) => normalizeCoupon(entry, index))
          .filter((entry): entry is EventCoupon => entry !== null)
      : [],
    checkinOperators: Array.isArray(raw.checkinOperators)
      ? raw.checkinOperators
          .map((entry, index) => normalizeCheckinOperator(entry, index))
          .filter((entry): entry is EventCheckinOperator => entry !== null)
      : [],
  };
};

const serializeDataExtra = (dataExtra: AdminEventDataExtra): Record<string, unknown> => ({
  ...dataExtra.raw,
  coupons: dataExtra.cupons.map((coupon) => ({
    id: coupon.id,
    titulo: coupon.titulo.trim(),
    codigo: coupon.codigo.trim().toUpperCase(),
    tipo: coupon.tipo,
    valor: coupon.valor.trim(),
    valorMinimo: coupon.valorMinimo.trim(),
    valorMaximo: coupon.valorMaximo.trim(),
    quantidadeDisponivel: Math.max(0, Math.floor(coupon.quantidadeDisponivel)),
    usos: Math.max(0, Math.floor(coupon.usos)),
    ativo: coupon.ativo,
    createdAt: coupon.createdAt || new Date().toISOString(),
  })),
  checkinOperators: dataExtra.checkinOperators.map((operator) => ({
    id: operator.id,
    nome: operator.nome.trim(),
    email: operator.email.trim(),
    ativo: operator.ativo,
    createdAt: operator.createdAt || new Date().toISOString(),
  })),
});

const mergeEventOwnerScopeStats = (
  stats: AdminEvent["stats"],
  context?: TenantPaymentRecipientContext
): AdminEvent["stats"] => {
  const ownerId = asString(context?.ownerId).trim();
  const ownerType = context?.ownerType;
  if (!ownerId || !ownerType || ownerType === "tenant") return stats;

  const nextStats: AdminEvent["stats"] = {
    ...stats,
    leagueId: ownerId,
    collectiveId: ownerId,
    collectiveType: ownerType,
  };

  if (ownerType === "commission") {
    nextStats.commissionId = ownerId;
    nextStats.comissaoId = ownerId;
  } else if (ownerType === "directory") {
    nextStats.directoryId = ownerId;
    nextStats.diretorioId = ownerId;
  } else {
    nextStats.ligaId = ownerId;
  }

  return nextStats;
};

const resolveEventOwnerScope = (
  event: Pick<AdminEvent, "stats">
): "tenant" | "league" | "commission" | "directory" => {
  const stats = event.stats;
  const scope = asString(
    stats.collectiveType ||
      stats.scopeType ||
      stats.scope_type ||
      stats.seller_type
  )
    .trim()
    .toLowerCase();

  if (scope === "commission" || scope === "comissao") return "commission";
  if (scope === "directory" || scope === "diretorio") return "directory";
  if (scope === "league" || scope === "liga") return "league";
  if (asString(stats.commissionId || stats.comissaoId).trim()) return "commission";
  if (asString(stats.directoryId || stats.diretorioId).trim()) return "directory";
  if (asString(stats.leagueId || stats.ligaId).trim()) return "league";
  return "tenant";
};

const ownerScopeLabel = (scope: "tenant" | "league" | "commission" | "directory"): string => {
  if (scope === "commission") return "comissão";
  if (scope === "directory") return "diretório";
  if (scope === "league") return "liga";
  return "atlética";
};

const resolveEventOwnerId = (event: Pick<AdminEvent, "stats">): string => {
  const stats = event.stats;
  const ownerScope = resolveEventOwnerScope(event);
  if (ownerScope === "commission") return asString(stats.commissionId || stats.comissaoId || stats.collectiveId || stats.leagueId || stats.ligaId).trim();
  if (ownerScope === "directory") return asString(stats.directoryId || stats.diretorioId || stats.collectiveId || stats.leagueId || stats.ligaId).trim();
  if (ownerScope === "league") return asString(stats.leagueId || stats.ligaId || stats.collectiveId).trim();
  return "";
};

const canonicalEventWorkspacePath = (
  scope: "tenant" | "league" | "commission" | "directory",
  ownerId: string,
  eventId: string,
  sectionPath = "edicao"
): string => {
  const encodedEventId = encodeURIComponent(eventId);
  const cleanSection = sectionPath.replace(/^\/+/, "") || "edicao";
  if (scope === "directory") return `/diretorio/configurar/${encodeURIComponent(ownerId)}/eventos/${encodedEventId}/${cleanSection}`;
  if (scope === "commission") return `/comissoes/configurar/${encodeURIComponent(ownerId)}/eventos/${encodedEventId}/${cleanSection}`;
  if (scope === "league") return `/ligas/${encodeURIComponent(ownerId)}/eventos/${encodedEventId}/${cleanSection}`;
  return `/admin/eventos/${encodedEventId}/${cleanSection}`;
};

const mapAdminEventRow = (raw: Record<string, unknown>): AdminEvent => {
  const statsRow = asRecord(raw.stats) ?? {};
  const paymentConfig = normalizePaymentConfig(raw.payment_config);
  const normalizedDataExtra = normalizeDataExtra(raw.data_extra);
  const capacity = Math.max(
    0,
    Math.floor(Number(raw.capacidade ?? normalizedDataExtra.raw.capacidade ?? normalizedDataExtra.raw.capacity ?? 0) || 0)
  );
  const eventCost = Math.max(
    0,
    parseMoneyValue(
      raw.custo ??
        raw.cost ??
        raw.totalCost ??
        normalizedDataExtra.raw.custo ??
        normalizedDataExtra.raw.cost ??
        normalizedDataExtra.raw.totalCost
    )
  );
  const breakEvenValue = Math.max(
    0,
    parseMoneyValue(raw.breakEven ?? normalizedDataExtra.raw.breakEven)
  );
  const costRows = Array.isArray(raw.custos)
    ? raw.custos
    : Array.isArray(normalizedDataExtra.raw.custos)
      ? normalizedDataExtra.raw.custos
      : [];
  const dataExtra = {
    ...normalizedDataExtra,
    raw: {
      ...normalizedDataExtra.raw,
      ...(capacity > 0 ? { capacidade: capacity, capacity } : {}),
      ...(eventCost > 0 ? { custo: eventCost, cost: eventCost } : {}),
      ...(breakEvenValue > 0 ? { breakEven: breakEvenValue } : {}),
      ...(costRows.length > 0 ? { custos: costRows } : {}),
    },
  };
  return {
    id: asString(raw.id),
    titulo: asString(raw.titulo, "Evento"),
    data: asString(raw.data),
    hora: asString(raw.hora),
    local: asString(raw.local),
    tipo: asString(raw.tipo, "Evento"),
    destaque: asString(raw.destaque),
    mapsUrl: asString(raw.mapsUrl),
    imagem: asString(raw.imagem),
    imagePositionY: Number.isFinite(Number(raw.imagePositionY)) ? Number(raw.imagePositionY) : 50,
    descricao: asString(raw.descricao),
    status: asString(raw.status, "ativo") === "encerrado" ? "encerrado" : "ativo",
    saleStatus: normalizeSaleStatus(raw.sale_status),
    isLowStock: asBoolean(raw.isLowStock),
    lotes: Array.isArray(raw.lotes)
      ? raw.lotes.map((entry, index) => normalizeLote(entry, index))
      : [],
    pixChave: asString(raw.pixChave),
    pixBanco: asString(raw.pixBanco),
    pixTitular: asString(raw.pixTitular),
    contatoComprovante: asString(raw.contatoComprovante),
    stats: {
      ...statsRow,
      confirmados: Math.max(0, Math.floor(Number(statsRow.confirmados ?? 0) || 0)),
      talvez: Math.max(0, Math.floor(Number(statsRow.talvez ?? 0) || 0)),
      likes: Math.max(0, Math.floor(Number(statsRow.likes ?? 0) || 0)),
    },
    paymentConfig,
    recipientUserIds: getPaymentRecipientIdsFromConfig(paymentConfig),
    dataExtra,
    adminVisibilityBlock: getEventVisibilityBlock(raw),
  };
};

const mapSaleRow = (raw: Record<string, unknown>): EventSaleRow => {
  const data = asRecord(raw.data) ?? {};

  return {
    id: asString(raw.id),
    userId: asString(raw.userId),
    userName: asString(raw.userName, "Aluno"),
    userTurma: asString(raw.userTurma, "-"),
    status: asString(raw.status, "pendente"),
    loteId: asString(raw.loteId),
    loteNome: asString(raw.loteNome, "-"),
    quantidade: Math.max(1, Math.floor(Number(raw.quantidade ?? 1) || 1)),
    valorUnitario: asString(raw.valorUnitario, "0,00"),
    valorTotal: asString(raw.valorTotal, "0,00"),
    dataSolicitacao:
      raw.dataSolicitacao ??
      raw.createdAt ??
      data.dataSolicitacao ??
      data.createdAt ??
      null,
    dataAprovacao: raw.dataAprovacao,
    dataPagamento:
      raw.dataPagamento ??
      raw.paymentDate ??
      raw.paidAt ??
      data.dataPagamento ??
      data.paymentDate ??
      data.paidAt ??
      data.pagoEm ??
      null,
    aprovadoPor: asString(raw.aprovadoPor),
    paymentConfig: normalizePaymentConfig(raw.payment_config),
    rawData: { ...raw, data },
  };
};

const mapPollRow = (raw: Record<string, unknown>): EventPoll => {
  const options = Array.isArray(raw.options) ? raw.options : [];
  return {
    id: asString(raw.id),
    question: asString(raw.question, "Enquete"),
    allowUserOptions: asBoolean(raw.allowUserOptions),
    options: options.flatMap((entry): PollOption[] => {
      const row = asRecord(entry);
      if (!row) return [];
      const text = asString(row.text).trim();
      if (!text) return [];
      const creatorName = asString(row.creatorName).trim();
      return [
        {
          text,
          votes: Math.max(0, Math.floor(Number(row.votes ?? 0) || 0)),
          ...(creatorName ? { creatorName } : {}),
        },
      ];
    }),
  };
};

const flattenTicketCheckins = (salesRows: EventSaleRow[]): TicketCheckinRow[] =>
  salesRows
    .flatMap((row) => {
      const entries = row.paymentConfig?.ticketEntries ?? [];
      return entries
        .filter((entry) => entry.status === "lido" || Boolean(entry.scannedAt))
        .map((entry) => ({
          orderId: row.id,
          ticketLabel: asString(entry.label, "Ingresso"),
          ticketToken: asString(entry.token),
          holderName: asString(entry.holderName, row.userName),
          holderTurma: asString(entry.holderTurma, row.userTurma),
          loteNome: asString(entry.loteName, row.loteNome),
          scannedAt: asString(entry.scannedAt),
          scannedByUserId: asString(entry.scannedByUserId),
          scannedByUserName: asString(entry.scannedByUserName, "Operador"),
          scannedByUserTurma: asString(entry.scannedByUserTurma),
          scannedByUserAvatar: asString(entry.scannedByUserAvatar),
          scanSource: asString(entry.scanSource),
        }));
    })
    .sort(
      (left, right) =>
        new Date(right.scannedAt || 0).getTime() - new Date(left.scannedAt || 0).getTime()
    );

const serializeLot = (lot: EventLot, plans: PlanRecord[]) => ({
  id: lot.id,
  nome: lot.nome.trim().slice(0, EVENT_LOTE_NAME_MAX_LENGTH),
  preco: lot.preco.trim(),
  status: lot.status,
  descricao: lot.descricao.trim(),
  quantidade: Math.max(0, Math.floor(lot.quantidade)),
  ordem: Math.max(0, Math.floor(lot.ordem)),
  qrPorIngresso: Math.max(1, Math.floor(lot.qrPorIngresso)),
  invisivel: lot.invisivel,
  transferivel: lot.transferivel,
  validadeAtiva: lot.validadeAtiva,
  inicioVendasData: lot.inicioVendasData,
  inicioVendasHora: lot.inicioVendasHora,
  fimVendasData: lot.fimVendasData,
  fimVendasHora: lot.fimVendasHora,
  planPrices: serializeLotePlanPrices(plans, lot.planPrices),
});

const cloneEvent = (event: AdminEvent): AdminEvent => ({
  ...event,
  lotes: event.lotes.map((lot) => ({
    ...lot,
    planPrices: lot.planPrices.map((entry) => ({ ...entry })),
  })),
  stats: { ...event.stats },
  adminVisibilityBlock: event.adminVisibilityBlock ? { ...event.adminVisibilityBlock } : null,
  paymentConfig: event.paymentConfig
    ? {
        ...event.paymentConfig,
        ...(event.paymentConfig.recipients
          ? { recipients: event.paymentConfig.recipients.map((entry) => ({ ...entry })) }
          : {}),
        ...(event.paymentConfig.recipient
          ? { recipient: { ...event.paymentConfig.recipient } }
          : {}),
      }
    : null,
  recipientUserIds: [...event.recipientUserIds],
  dataExtra: {
    raw: { ...event.dataExtra.raw },
    cupons: event.dataExtra.cupons.map((coupon) => ({ ...coupon })),
    checkinOperators: event.dataExtra.checkinOperators.map((operator) => ({ ...operator })),
  },
});

function SectionLink({
  href,
  label,
  active,
}: {
  href: string;
  label: string;
  active: boolean;
}) {
  return (
    <Link
      href={href}
      className={`whitespace-nowrap rounded-full border px-4 py-2 text-xs font-black uppercase tracking-[0.16em] transition ${
        active
          ? "border-brand bg-brand-soft text-brand-accent shadow-brand"
          : "border-zinc-800 bg-zinc-950 text-zinc-400 hover:border-zinc-700 hover:text-zinc-200"
      }`}
    >
      {label}
    </Link>
  );
}

function PaginationControls({
  page,
  totalPages,
  totalItems,
  pageSize,
  onPageChange,
}: {
  page: number;
  totalPages: number;
  totalItems: number;
  pageSize: number;
  onPageChange: (page: number) => void;
}) {
  if (totalItems <= pageSize) return null;

  const firstItem = totalItems === 0 ? 0 : (page - 1) * pageSize + 1;
  const lastItem = Math.min(totalItems, page * pageSize);
  const goTo = (nextPage: number) =>
    onPageChange(Math.min(totalPages, Math.max(1, nextPage)));

  return (
    <div className="flex flex-col gap-3 rounded-[1.2rem] border border-zinc-800 bg-black/20 px-3 py-3 sm:flex-row sm:items-center sm:justify-between">
      <p className="text-xs font-bold text-zinc-500">
        {firstItem}-{lastItem} de {totalItems} pedidos
      </p>
      <div className="flex flex-wrap items-center gap-2">
        <button
          type="button"
          onClick={() => goTo(1)}
          disabled={page <= 1}
          className="inline-flex items-center gap-1 rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-2 text-[11px] font-black uppercase text-zinc-300 disabled:opacity-40"
        >
          <ChevronsLeft size={13} />
          Primeira
        </button>
        <button
          type="button"
          onClick={() => goTo(page - 1)}
          disabled={page <= 1}
          className="inline-flex items-center gap-1 rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-2 text-[11px] font-black uppercase text-zinc-300 disabled:opacity-40"
        >
          <ChevronLeft size={13} />
          Anterior
        </button>
        <span className="rounded-xl border border-brand bg-brand-soft px-3 py-2 text-[11px] font-black uppercase text-brand-accent">
          Página {page} de {totalPages}
        </span>
        <button
          type="button"
          onClick={() => goTo(page + 1)}
          disabled={page >= totalPages}
          className="inline-flex items-center gap-1 rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-2 text-[11px] font-black uppercase text-zinc-300 disabled:opacity-40"
        >
          Próxima
          <ChevronRight size={13} />
        </button>
        <button
          type="button"
          onClick={() => goTo(totalPages)}
          disabled={page >= totalPages}
          className="inline-flex items-center gap-1 rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-2 text-[11px] font-black uppercase text-zinc-300 disabled:opacity-40"
        >
          Última
          <ChevronsRight size={13} />
        </button>
      </div>
    </div>
  );
}

function FloatingSaveButton({
  watchRef,
  label,
  icon,
  disabled = false,
  onClick,
}: {
  watchRef: RefObject<HTMLElement | null>;
  label: string;
  icon?: ReactNode;
  disabled?: boolean;
  onClick: () => void;
}) {
  const [targetVisible, setTargetVisible] = useState(true);
  const [active, setActive] = useState(false);

  useEffect(() => {
    const target = watchRef.current;
    if (!target || typeof IntersectionObserver === "undefined") return;

    const observer = new IntersectionObserver(
      ([entry]) => {
        const isVisible = Boolean(entry?.isIntersecting);
        setTargetVisible(isVisible);
        if (isVisible) setActive(false);
      },
      { threshold: 0.01 }
    );

    observer.observe(target);
    return () => observer.disconnect();
  }, [watchRef]);

  useEffect(() => {
    if (targetVisible) {
      setActive(false);
      return;
    }

    let timeoutId: number | null = null;
    const reveal = () => {
      setActive(true);
      if (timeoutId) window.clearTimeout(timeoutId);
      timeoutId = window.setTimeout(() => setActive(false), 3000);
    };

    reveal();
    window.addEventListener("scroll", reveal, { passive: true });
    window.addEventListener("pointermove", reveal, { passive: true });
    window.addEventListener("touchstart", reveal, { passive: true });
    window.addEventListener("keydown", reveal);

    return () => {
      if (timeoutId) window.clearTimeout(timeoutId);
      window.removeEventListener("scroll", reveal);
      window.removeEventListener("pointermove", reveal);
      window.removeEventListener("touchstart", reveal);
      window.removeEventListener("keydown", reveal);
    };
  }, [targetVisible]);

  if (targetVisible || !active) return null;

  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      className="fixed bottom-5 right-5 z-[90] inline-flex max-w-[calc(100vw-2rem)] items-center justify-center gap-2 rounded-2xl border border-brand bg-zinc-950/90 px-4 py-3 text-xs font-black uppercase tracking-wide text-brand-accent shadow-brand backdrop-blur-md transition hover:bg-zinc-900 disabled:opacity-50"
    >
      {icon}
      {label}
    </button>
  );
}

export default function AdminEventWorkspace({
  eventId,
  section,
  workspaceBasePath,
  eventsListHref,
  scanEventHref,
  eventBiBasePath,
  paymentRecipientContext,
}: {
  eventId: string;
  section: EventWorkspaceSection;
  workspaceBasePath?: string;
  eventsListHref?: string;
  legacyListHref?: string;
  scanEventHref?: string;
  eventBiBasePath?: string;
  paymentRecipientContext?: TenantPaymentRecipientContext;
}) {
  const router = useRouter();
  const { addToast } = useToast();
  const { user } = useAuth();
  const { tenantId: activeTenantId, tenantSlug, tenantName, tenantSigla } = useTenantTheme();
  const searchParams = useSearchParams();
  const statementQueryString = searchParams.toString();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const partyProductImageInputRef = useRef<HTMLInputElement>(null);

  const [evento, setEvento] = useState<AdminEvent | null>(null);
  const [editDraft, setEditDraft] = useState<AdminEvent | null>(null);
  const [loadingEvento, setLoadingEvento] = useState(true);
  const [savingEvento, setSavingEvento] = useState(false);
  const [uploadingImage, setUploadingImage] = useState(false);

  const [planCatalog, setPlanCatalog] = useState<PlanRecord[]>([]);

  const [paymentRecipients, setPaymentRecipients] = useState<TenantPaymentRecipientOption[]>([]);
  const [loadingRecipients, setLoadingRecipients] = useState(false);
  const [receiversManagerOpen, setReceiversManagerOpen] = useState(false);
  const [contextOwnerCategory, setContextOwnerCategory] = useState("");

  const [salesRows, setSalesRows] = useState<EventSaleRow[]>([]);
  const [loadingSales, setLoadingSales] = useState(false);
  const [salesSearch, setSalesSearch] = useState("");
  const [salesStatusFilter, setSalesStatusFilter] = useState<EventStatementStatusFilter>("todos");
  const [salesTypeFilter, setSalesTypeFilter] = useState<EventStatementTypeFilter>("todos");
  const [salesAlertFilter, setSalesAlertFilter] = useState("");
  const [salesSourceFilter, setSalesSourceFilter] = useState("todos");
  const [salesApproverFilter, setSalesApproverFilter] = useState("todos");
  const [salesFlowFilter, setSalesFlowFilter] = useState<EventStatementFlowFilter>("todos");
  const [salesIndicatorFilter, setSalesIndicatorFilter] = useState("");
  const [salesPage, setSalesPage] = useState(1);
  const [scanGroup, setScanGroup] = useState<EventPartyAlphaGroup>("todos");
  const [ticketCheckinMutatingId, setTicketCheckinMutatingId] = useState("");
  const [ticketScannerStarting, setTicketScannerStarting] = useState(false);
  const [ticketScannerActive, setTicketScannerActive] = useState(false);
  const [ticketScannerMessage, setTicketScannerMessage] = useState("");

  const [polls, setPolls] = useState<EventPoll[]>([]);
  const [loadingPolls, setLoadingPolls] = useState(false);
  const [pollQuestion, setPollQuestion] = useState("");
  const [pollAllowUserOptions, setPollAllowUserOptions] = useState(true);
  const [pollDraftOptions, setPollDraftOptions] = useState<string[]>(["", ""]);

  const [editingLotId, setEditingLotId] = useState<number | "new" | null>(null);
  const [lotDraft, setLotDraft] = useState<EventLot>(createEmptyLot([]));

  const [editingCouponId, setEditingCouponId] = useState<string | "new" | null>(null);
  const [couponDraft, setCouponDraft] = useState<EventCoupon>(createEmptyCoupon());

  const [editingOperatorId, setEditingOperatorId] = useState<string | "new" | null>(null);
  const [operatorDraft, setOperatorDraft] = useState<EventCheckinOperator>(createEmptyCheckinOperator());

  const [partyProducts, setPartyProducts] = useState<EventPartyProduct[]>([]);
  const [partyOrders, setPartyOrders] = useState<EventPartyOrder[]>([]);
  const [loadingPartyProducts, setLoadingPartyProducts] = useState(false);
  const [loadingPartyOrders, setLoadingPartyOrders] = useState(false);
  const [uploadingPartyProductImage, setUploadingPartyProductImage] = useState(false);
  const [editingPartyProductId, setEditingPartyProductId] = useState("");
  const [draggingPartyProductId, setDraggingPartyProductId] = useState("");
  const [partyOrdersStatus, setPartyOrdersStatus] = useState<"pendente" | "approved">("pendente");
  const [partyProductDraft, setPartyProductDraft] = useState({
    nome: "",
    preco: "",
    categoria: "",
    secao: "",
    descricao: "",
    img: "",
    estoque: "0",
    ordem: "",
  });
  const [partyNewSectionName, setPartyNewSectionName] = useState("");
  const [manualPartyOrderDraft, setManualPartyOrderDraft] = useState({
    productId: "",
    userId: "",
    userName: "",
    quantidade: "1",
    manualCode: "",
    externalNumber: "",
    cpf: "",
    telefone: "",
    email: "",
    ra: "",
  });
  const [partyUsers, setPartyUsers] = useState<EventPartyUserOption[]>([]);
  const [loadingPartyUsers, setLoadingPartyUsers] = useState(false);
  const [partyUserSearch, setPartyUserSearch] = useState("");
  const [partyUserTurmaFilter, setPartyUserTurmaFilter] = useState("todos");
  const [partyUserAlphaFilter, setPartyUserAlphaFilter] = useState<EventPartyAlphaGroup>("todos");
  const [partyUserPage, setPartyUserPage] = useState(1);
  const [partyUserCadastroDraft, setPartyUserCadastroDraft] = useState({
    nome: "",
    cpf: "",
    telefone: "",
    email: "",
    numero: "",
    ra: "",
    turma: "",
    valorPorta: "",
  });
  const [editingManualPartyUserId, setEditingManualPartyUserId] = useState("");
  const [partyOrderMutatingId, setPartyOrderMutatingId] = useState("");
  const [partyProductMetaDrafts, setPartyProductMetaDrafts] = useState<
    Record<string, { section: string; order: string }>
  >({});
  const [savingPartyProductMetaId, setSavingPartyProductMetaId] = useState("");
  const [salesHasMore, setSalesHasMore] = useState(false);
  const [draggingLotId, setDraggingLotId] = useState<number | null>(null);

  const loadingPlansRef = useRef(false);
  const loadingRecipientsRef = useRef(false);
  const loadingSalesRef = useRef(false);
  const loadingPollsRef = useRef(false);
  const ticketScannerRef = useRef<Html5Qrcode | null>(null);
  const ticketScannerProcessingRef = useRef(false);
  const ticketScannerLastPayloadRef = useRef("");
  const saveLotButtonRef = useRef<HTMLButtonElement>(null);
  const saveCouponButtonRef = useRef<HTMLButtonElement>(null);
  const saveOperatorButtonRef = useRef<HTMLButtonElement>(null);
  const saveEditButtonRef = useRef<HTMLButtonElement>(null);
  const saveReceiversButtonRef = useRef<HTMLButtonElement>(null);
  const savePartyButtonRef = useRef<HTMLButtonElement>(null);
  const savingManualPartyUserRef = useRef(false);

  const scopedPath = useCallback(
    (path: string) => (tenantSlug ? withTenantSlug(tenantSlug, path) : path),
    [tenantSlug]
  );
  const normalizedWorkspaceBasePath = workspaceBasePath?.trim().replace(/\/+$/, "") || "";
  const buildWorkspaceHref = useCallback(
    (targetPath: string) => {
      const cleanTargetPath = targetPath.replace(/^\/+/, "");
      return scopedPath(
        normalizedWorkspaceBasePath
          ? `${normalizedWorkspaceBasePath}/${cleanTargetPath}`
          : `/admin/eventos/${encodeURIComponent(eventId)}/${cleanTargetPath}`
      );
    },
    [eventId, normalizedWorkspaceBasePath, scopedPath]
  );

  const eventSectionHref = useCallback(
    (targetSection: EventWorkspaceSection) => buildWorkspaceHref(SECTION_PATHS[targetSection]),
    [buildWorkspaceHref]
  );

  const adminEventosHref = scopedPath(eventsListHref?.trim() || "/admin/eventos");
  const scanEventoHref = scopedPath(
    scanEventHref?.trim() ||
      (workspaceBasePath?.trim()
        ? `${workspaceBasePath.trim().replace(/\/+$/, "")}/scan`
        : `/admin/eventos/${encodeURIComponent(eventId)}/scan`)
  );
  const scanHubHref = scopedPath("/admin/scan-eventos");
  const eventBiBaseHref = scopedPath(eventBiBasePath?.trim().replace(/\/+$/, "") || "/admin/bi");
  const eventProductsHref = scopedPath(`/eventos/${encodeURIComponent(eventId)}/produtos`);
  const eventProductTicketsHref = scopedPath(`/eventos/${encodeURIComponent(eventId)}/produtos/fichas`);
  const eventPartyAdminHref = buildWorkspaceHref("ficha");
  const eventPartyPaymentHref = buildWorkspaceHref("ficha/pagamento");
  const eventPartyUserCadastroHref = buildWorkspaceHref("ficha/cadastro");
  const eventPartyProductHref = buildWorkspaceHref("ficha/produto");
  const eventPartyProductsAdminHref = buildWorkspaceHref("ficha/produtos");
  const eventPartyProductCadastroHref = buildWorkspaceHref("ficha/produtos/cadastro");
  const eventPartyWithdrawalHref = buildWorkspaceHref("ficha/retirada");
  const adminTicketOrderHref = useCallback(
    (orderId: string) => buildWorkspaceHref(`ingressos/${encodeURIComponent(orderId)}`),
    [buildWorkspaceHref]
  );
  const hasRecipientOwnerContext = Boolean(
    paymentRecipientContext?.ownerId?.trim() &&
      paymentRecipientContext?.ownerType &&
      paymentRecipientContext.ownerType !== "tenant"
  );
  const recipientScopeLabel = hasRecipientOwnerContext ? "deste órgão" : "da tenant";

  useEffect(() => {
    let mounted = true;
    const ownerId = asString(paymentRecipientContext?.ownerId).trim();
    if (!ownerId || paymentRecipientContext?.ownerType !== "league") {
      setContextOwnerCategory("");
      return () => {
        mounted = false;
      };
    }

    void fetchLeagueById(ownerId, {
      tenantId: activeTenantId || undefined,
      forceRefresh: true,
    })
      .then((league) => {
        if (mounted) setContextOwnerCategory(asString(league?.category).trim().toLowerCase());
      })
      .catch((error: unknown) => {
        console.error(error);
        if (mounted) setContextOwnerCategory("");
      });

    return () => {
      mounted = false;
    };
  }, [activeTenantId, paymentRecipientContext?.ownerId, paymentRecipientContext?.ownerType]);

  useEffect(() => {
    if (section !== "extrato") return;
    const params = new URLSearchParams(statementQueryString);
    setSalesSearch(params.get("busca") || params.get("q") || "");
    setSalesTypeFilter(normalizeStatementTypeFilterParam(params.get("tipo")));
    setSalesStatusFilter(normalizeStatementStatusFilterParam(params.get("status")));
    setSalesAlertFilter(normalizeStatementAlertParam(params.get("alerta")));
    setSalesSourceFilter(normalizeStatementTextFilterParam(params.get("origem")));
    setSalesApproverFilter(normalizeStatementTextFilterParam(params.get("aprovador")));
    setSalesFlowFilter(normalizeStatementFlowFilterParam(params.get("fluxo")));
    setSalesIndicatorFilter(normalizeStatementIndicatorParam(params.get("indicador")));
    setSalesPage(1);
  }, [section, statementQueryString]);

  useEffect(() => {
    if (section !== "checkins") return;
    const params = new URLSearchParams(statementQueryString);
    setSalesSearch(params.get("busca") || params.get("q") || "");
    setSalesIndicatorFilter(normalizePresenceIndicatorParam(params.get("indicador")));
    setSalesPage(1);
  }, [section, statementQueryString]);

  const statementHasDeepFilter =
    section === "extrato" &&
    Boolean(
      salesSearch.trim() ||
        salesStatusFilter !== "todos" ||
        salesTypeFilter !== "todos" ||
        salesAlertFilter ||
        salesSourceFilter !== "todos" ||
        salesApproverFilter !== "todos" ||
        salesFlowFilter !== "todos" ||
        salesIndicatorFilter
    );

  const loadEvent = useCallback(
    async () => {
      const cleanEventId = eventId.trim();
      if (!cleanEventId) return;

      setLoadingEvento(true);
      try {
        const row = await fetchAdminEventById({
          eventId: cleanEventId,
          tenantId: activeTenantId || undefined,
        });
        if (!row) {
          setEvento(null);
          setEditDraft(null);
          return;
        }
        const mapped = mapAdminEventRow(row);
        setEvento(mapped);
        setEditDraft(cloneEvent(mapped));
      } catch (error: unknown) {
        console.error(error);
        addToast("Erro ao carregar evento.", "error");
      } finally {
        setLoadingEvento(false);
      }
    },
    [activeTenantId, addToast, eventId]
  );

  const loadPlanCatalog = useCallback(
    async (forceRefresh = false) => {
      if (loadingPlansRef.current) return;
      loadingPlansRef.current = true;
      try {
        const rows = await fetchPlanCatalog({
          tenantId: activeTenantId || undefined,
          forceRefresh,
          maxResults: 50,
        });
        setPlanCatalog(rows);
      } catch (error: unknown) {
        console.error(error);
      } finally {
        loadingPlansRef.current = false;
      }
    },
    [activeTenantId]
  );

  const loadPaymentRecipients = useCallback(async () => {
    const cleanTenantId = (activeTenantId || "").trim();
    if (!cleanTenantId) {
      setPaymentRecipients([]);
      setLoadingRecipients(false);
      return;
    }
    if (loadingRecipientsRef.current) return;
    loadingRecipientsRef.current = true;
    setLoadingRecipients(true);
    try {
      const rows = await fetchTenantPaymentRecipients(
        cleanTenantId,
        "events",
        paymentRecipientContext
      );
      setPaymentRecipients(rows);
    } catch (error: unknown) {
      console.error(error);
      setPaymentRecipients([]);
      addToast("Erro ao carregar recebedores.", "error");
    } finally {
      loadingRecipientsRef.current = false;
      setLoadingRecipients(false);
    }
  }, [
    activeTenantId,
    addToast,
    paymentRecipientContext,
  ]);

  const loadSales = useCallback(
    async (forceRefresh = false) => {
      if (loadingSalesRef.current) return;
      loadingSalesRef.current = true;
      setLoadingSales(true);
      try {
        const isStatementPage = section === "extrato";
        const page = await fetchAdminEventSalesPage({
          eventId,
          pageSize: isStatementPage && !statementHasDeepFilter ? SALES_PAGE_SIZE : 2000,
          cursorId: isStatementPage && !statementHasDeepFilter ? String((salesPage - 1) * SALES_PAGE_SIZE) : undefined,
          forceRefresh,
          tenantId: activeTenantId || undefined,
        });
        setSalesRows(page.rows.map((row) => mapSaleRow(row)));
        setSalesHasMore(page.hasMore);
      } catch (error: unknown) {
        console.error(error);
        addToast("Erro ao carregar ingressos.", "error");
      } finally {
        loadingSalesRef.current = false;
        setLoadingSales(false);
      }
    },
    [activeTenantId, addToast, eventId, salesPage, section, statementHasDeepFilter]
  );

  const loadPolls = useCallback(
    async (forceRefresh = false) => {
      if (loadingPollsRef.current) return;
      loadingPollsRef.current = true;
      setLoadingPolls(true);
      try {
        const rows = await fetchAdminEventPolls({
          eventId,
          forceRefresh,
          maxResults: 60,
          tenantId: activeTenantId || undefined,
        });
        setPolls(rows.map((row) => mapPollRow(row)));
      } catch (error: unknown) {
        console.error(error);
        addToast("Erro ao carregar enquetes.", "error");
      } finally {
        loadingPollsRef.current = false;
        setLoadingPolls(false);
      }
    },
    [activeTenantId, addToast, eventId]
  );

  const loadPartyProducts = useCallback(async () => {
    setLoadingPartyProducts(true);
    try {
      const rows = await fetchEventPartyProducts({
        eventId,
        tenantId: activeTenantId || undefined,
        admin: true,
      });
      setPartyProducts(rows);
      setPartyProductDraft((previous) => ({
        ...previous,
        categoria:
          previous.categoria ||
          normalizeEventPartyConfig((editDraft || evento)?.dataExtra.raw).categoryName,
      }));
    } catch (error: unknown) {
      console.error(error);
      addToast("Erro ao carregar produtos do modo vendas.", "error");
    } finally {
      setLoadingPartyProducts(false);
    }
  }, [activeTenantId, addToast, editDraft, evento, eventId]);

  const loadPartyOrders = useCallback(async () => {
    setLoadingPartyOrders(true);
    try {
      const rows = await fetchEventPartyOrders({
        eventId,
        tenantId: activeTenantId || undefined,
        productIds: partyProducts.map((product) => product.id),
        pageSize: section === "extrato" && !statementHasDeepFilter ? SALES_PAGE_SIZE : 2000,
        cursorId: section === "extrato" && !statementHasDeepFilter ? String((salesPage - 1) * SALES_PAGE_SIZE) : undefined,
      });
      setPartyOrders(rows);
    } catch (error: unknown) {
      console.error(error);
      addToast("Erro ao carregar pedidos do modo vendas.", "error");
    } finally {
      setLoadingPartyOrders(false);
    }
  }, [activeTenantId, addToast, eventId, partyProducts, salesPage, section, statementHasDeepFilter]);

  const loadPartyUsers = useCallback(async () => {
    setLoadingPartyUsers(true);
    try {
      const checkedInPage = await fetchAdminEventSalesPage({
        eventId,
        pageSize: 2000,
        forceRefresh: true,
        tenantId: activeTenantId || undefined,
      });
      const checkedInSales = checkedInPage.rows
        .map((row) => mapSaleRow(row))
        .filter(saleHasCheckin);
      const checkedInUserIds = new Set(
        checkedInSales.map((row) => row.userId.trim()).filter(Boolean)
      );
      const checkedInUserNames = new Set(
        checkedInSales.map((row) => normalizeSearch(row.userName)).filter(Boolean)
      );

      let columns = ["uid", "nome", "email", "turma", "telefone", "ra", "cpf", "tenant_id"];
      let appRows: unknown[] = [];
      while (columns.length > 0) {
        let query = getSupabaseClient()
          .from("users")
          .select(columns.join(","))
          .order("nome", { ascending: true })
          .limit(1000);
        if (activeTenantId) query = query.eq("tenant_id", activeTenantId);
        const { data, error } = await query;
        if (!error) {
          appRows = Array.isArray(data) ? data : [];
          break;
        }
        const missingColumn = extractSchemaFallbackColumn(error);
        const nextColumns = columns.filter(
          (column) => column.toLowerCase() !== missingColumn.toLowerCase()
        );
        if (!missingColumn || nextColumns.length === columns.length) throw error;
        columns = nextColumns;
      }

      const appUsers: EventPartyUserOption[] = appRows
        .map((entry) => asRecord(entry))
        .filter((entry): entry is Record<string, unknown> => Boolean(entry))
        .map((entry) => {
          const id = asString(entry.uid).trim();
          return {
            id,
            nome: asString(entry.nome).trim() || asString(entry.email).trim() || "Usuário",
            cpf: normalizeDigits(asString(entry.cpf)).slice(0, 14),
            telefone: asString(entry.telefone).trim(),
            email: asString(entry.email).trim(),
            numero: asString(entry.ra).trim(),
            ra: asString(entry.ra).trim(),
            turma: asString(entry.turma).trim(),
            source: "app" as const,
          };
        })
        .filter((entry) => entry.id)
        .filter(
          (entry) =>
            checkedInUserIds.has(entry.id) ||
            checkedInUserNames.has(normalizeSearch(entry.nome))
        );

      const manualUsers = getManualPartyUsers((editDraft || evento)?.dataExtra.raw).map(
        (entry): EventPartyUserOption => ({
          id: entry.id,
          nome: entry.nome,
          cpf: entry.cpf,
          telefone: entry.telefone,
          email: entry.email,
          numero: entry.numero,
          ra: entry.ra,
          turma: entry.turma,
          source: "manual",
        })
      );
      const manualSalesUsers = checkedInSales
        .map((entry) => mapManualSaleRowToManualUser(entry))
        .filter((entry): entry is EventPartyManualUser => Boolean(entry))
        .map(
          (entry): EventPartyUserOption => ({
            id: entry.id,
            nome: entry.nome,
            cpf: entry.cpf,
            telefone: entry.telefone,
            email: entry.email,
            numero: entry.numero,
            ra: entry.ra,
            turma: entry.turma,
            source: "manual",
          })
        );

      const seen = new Set<string>();
      setPartyUsers(
        [...appUsers, ...manualSalesUsers, ...manualUsers]
          .filter((entry) => {
            const keys = [
              entry.id ? `id:${entry.id}` : "",
              entry.numero ? `numero:${normalizeSearch(entry.numero)}` : "",
              entry.ra ? `ra:${normalizeSearch(entry.ra)}` : "",
              entry.cpf ? `cpf:${entry.cpf}` : "",
              `nome:${normalizeSearch(`${entry.nome}|${entry.numero || entry.ra}`)}`,
            ].filter(Boolean);
            if (keys.some((key) => seen.has(key))) return false;
            keys.forEach((key) => seen.add(key));
            return true;
          })
          .sort((left, right) => left.nome.localeCompare(right.nome, "pt-BR", { sensitivity: "base" }))
      );
    } catch (error: unknown) {
      console.error(error);
      addToast("Erro ao carregar usuários para pagamento.", "error");
    } finally {
      setLoadingPartyUsers(false);
    }
  }, [activeTenantId, addToast, editDraft, evento, eventId]);

  const shouldBlockAdminOperationalLoads = Boolean(
    evento &&
      !normalizedWorkspaceBasePath &&
      resolveEventOwnerScope(evento) !== "tenant" &&
      section !== "edicao"
  );

  useEffect(() => {
    void loadEvent();
  }, [loadEvent]);

  useEffect(() => {
    if (!evento || shouldBlockAdminOperationalLoads) return;
    if (section === "lotes" || section === "edicao") {
      void loadPlanCatalog();
    }
    if (section === "recebedores" || section === "edicao") {
      void loadPaymentRecipients();
    }
    if (
      section === "extrato" ||
      section === "ingressos" ||
      section === "checkins" ||
      section === "scan" ||
      section === "ficha-cadastro"
    ) {
      void loadSales();
    }
    if (section === "enquetes") {
      void loadPolls();
    }
    if (isEventPartyAdminSection(section) || section === "extrato") {
      void loadPartyProducts();
    }
    if (section === "ficha-pagamento" || section === "ficha-cadastro") {
      void loadPartyUsers();
    }
  }, [evento, loadPartyProducts, loadPartyUsers, loadPaymentRecipients, loadPlanCatalog, loadPolls, loadSales, section, shouldBlockAdminOperationalLoads]);

  useEffect(() => {
    if (!evento || shouldBlockAdminOperationalLoads) return;
    if (section !== "ficha" && section !== "ficha-pagamento" && section !== "extrato") return;
    void loadPartyOrders();
  }, [evento, loadPartyOrders, section, shouldBlockAdminOperationalLoads]);

  useEffect(() => {
    setPartyProductMetaDrafts((previous) => {
      const next: Record<string, { section: string; order: string }> = {};
      partyProducts.forEach((product, index) => {
        next[product.id] = previous[product.id] || {
          section: getEventPartyProductSection(product),
          order: String(
            getEventPartyProductOrder(product) === 9999
              ? index + 1
              : getEventPartyProductOrder(product)
          ),
        };
      });
      return next;
    });
    setManualPartyOrderDraft((previous) =>
      previous.productId || partyProducts.length === 0
        ? previous
        : { ...previous, productId: partyProducts[0].id }
    );
  }, [partyProducts]);

  useEffect(() => {
    if (!planCatalog.length) return;
    setLotDraft((previous) => ({
      ...previous,
      planPrices: buildLotePlanPrices(planCatalog, previous.planPrices),
    }));
    setEditDraft((previous) =>
      previous
        ? {
            ...previous,
            lotes: previous.lotes.map((lot) => ({
              ...lot,
              planPrices: buildLotePlanPrices(planCatalog, lot.planPrices),
            })),
          }
        : previous
    );
  }, [planCatalog]);

  const persistEvent = useCallback(
    async (nextEvent: AdminEvent, successMessage: string) => {
      if (savingEvento) return;
      const selectedPaymentRecipientsFromDirectory = filterTenantPaymentRecipientsByIds(
        paymentRecipients,
        nextEvent.recipientUserIds
      );
      const fallbackPaymentRecipients = (
        nextEvent.paymentConfig?.recipients?.length
          ? nextEvent.paymentConfig.recipients
          : nextEvent.paymentConfig?.recipient
            ? [nextEvent.paymentConfig.recipient]
            : []
      )
        .map((entry) => ({
          userId: asString(entry.userId),
          name: asString(entry.name),
          turma: asString(entry.turma),
          avatarUrl: asString(entry.avatarUrl),
          phone: asString(entry.phone),
        }))
        .filter((entry) => nextEvent.recipientUserIds.includes(entry.userId));
      const selectedPaymentRecipients =
        selectedPaymentRecipientsFromDirectory.length > 0 || nextEvent.recipientUserIds.length === 0
          ? selectedPaymentRecipientsFromDirectory
          : fallbackPaymentRecipients;
      const primaryPaymentRecipient = selectedPaymentRecipients[0] || null;
      const normalizedWhatsapp = normalizePhoneToBrE164(nextEvent.contatoComprovante || "");
      const hasPaymentConfig =
        Boolean(nextEvent.pixChave.trim()) ||
        Boolean(nextEvent.pixBanco.trim()) ||
        Boolean(nextEvent.pixTitular.trim()) ||
        Boolean(normalizedWhatsapp.trim()) ||
        selectedPaymentRecipients.length > 0;
      const eventCapacity = Math.max(
        0,
        Math.floor(Number(nextEvent.dataExtra.raw.capacidade ?? nextEvent.dataExtra.raw.capacity ?? 0) || 0)
      );
      const eventCostValue = Math.max(
        0,
        parseMoneyValue(
          nextEvent.dataExtra.raw.custo ??
            nextEvent.dataExtra.raw.cost ??
            nextEvent.dataExtra.raw.totalCost
        )
      );
      const breakEvenValue = Math.max(
        0,
        parseMoneyValue(nextEvent.dataExtra.raw.breakEven)
      );
      const detailedCosts = Array.isArray(nextEvent.dataExtra.raw.custos)
        ? nextEvent.dataExtra.raw.custos
        : [];

      const payload: Record<string, unknown> = {
        titulo: nextEvent.titulo.trim().slice(0, EVENT_TITLE_MAX_LENGTH),
        data: nextEvent.data,
        hora: nextEvent.hora,
        local: nextEvent.local.trim().slice(0, EVENT_LOCATION_MAX_LENGTH),
        tipo: nextEvent.tipo.trim().slice(0, EVENT_TYPE_MAX_LENGTH),
        destaque: nextEvent.destaque.trim().slice(0, 180),
        mapsUrl: nextEvent.mapsUrl.trim().slice(0, 400),
        imagem: nextEvent.imagem,
        imagePositionY: nextEvent.imagePositionY,
        descricao: nextEvent.descricao.trim().slice(0, EVENT_DESCRIPTION_MAX_LENGTH),
        lotes: nextEvent.lotes.map((lot) => serializeLot(lot, planCatalog)),
        status: nextEvent.status,
        sale_status: nextEvent.saleStatus,
        pixChave: nextEvent.pixChave.trim().slice(0, EVENT_PIX_FIELD_MAX_LENGTH),
        pixBanco: nextEvent.pixBanco.trim().slice(0, EVENT_PIX_FIELD_MAX_LENGTH),
        pixTitular: nextEvent.pixTitular.trim().slice(0, EVENT_PIX_FIELD_MAX_LENGTH),
        contatoComprovante: normalizedWhatsapp.slice(0, PHONE_MAX_LENGTH),
        stats: mergeEventOwnerScopeStats(nextEvent.stats, paymentRecipientContext),
        isLowStock: nextEvent.isLowStock,
        capacidade: eventCapacity > 0 ? eventCapacity : null,
        custo: eventCostValue > 0 ? eventCostValue : null,
        custos: detailedCosts,
        breakEven: breakEvenValue > 0 ? breakEvenValue : null,
        data_extra: serializeDataExtra(nextEvent.dataExtra),
        payment_config: hasPaymentConfig
          ? {
              chave: nextEvent.pixChave.trim(),
              banco: nextEvent.pixBanco.trim(),
              titular: nextEvent.pixTitular.trim(),
              ...(normalizedWhatsapp ? { whatsapp: normalizedWhatsapp } : {}),
              ...(primaryPaymentRecipient
                ? { recipient: toCommerceRecipientSnapshot(primaryPaymentRecipient) }
                : {}),
              ...(selectedPaymentRecipients.length > 0
                ? { recipients: selectedPaymentRecipients.map(toCommerceRecipientSnapshot) }
                : {}),
            }
          : null,
      };

      setSavingEvento(true);
      try {
        await upsertAdminEvent({
          eventId: nextEvent.id,
          data: payload,
          actorUserId: user?.uid,
          tenantId: activeTenantId || undefined,
        });
        await loadEvent();
        addToast(successMessage, "success");
      } catch (error: unknown) {
        console.error(error);
        addToast("Erro ao salvar evento.", "error");
      } finally {
        setSavingEvento(false);
      }
    },
    [activeTenantId, addToast, loadEvent, paymentRecipientContext, paymentRecipients, planCatalog, savingEvento, user?.uid]
  );

  const handleSaveEdit = async () => {
    if (!editDraft) return;
    if (!editDraft.titulo.trim()) {
      addToast("Título obrigatório.", "error");
      return;
    }
    if (!editDraft.data || !editDraft.hora) {
      addToast("Data e hora obrigatórias.", "error");
      return;
    }
    if (
      editDraft.contatoComprovante.trim() &&
      !hasValidPhoneLength(editDraft.contatoComprovante.trim())
    ) {
      addToast("Informe um WhatsApp válido para o comprovante.", "error");
      return;
    }
    await persistEvent(editDraft, "Evento atualizado.");
  };

  const handleSavePublicSlug = async () => {
    if (!editDraft) return;
    const publicSlug = normalizePublicEventSlug(asString(editDraft.dataExtra.raw.publicSlug));
    if (!publicSlug) {
      addToast("Informe uma slug válida para a página pública.", "error");
      return;
    }
    if (publicSlug.length > EVENT_PUBLIC_SLUG_MAX_LENGTH) {
      addToast("A slug pode ter no máximo 20 caracteres.", "error");
      return;
    }
    const available = await isAdminEventPublicSlugAvailable({
      slug: publicSlug,
      eventId: editDraft.id,
      tenantId: activeTenantId || undefined,
    });
    if (!available) {
      addToast("Já existe uma página com essa slug. Escolha outra.", "error");
      return;
    }
    const nextDraft = {
      ...editDraft,
      dataExtra: {
        ...editDraft.dataExtra,
        raw: {
          ...editDraft.dataExtra.raw,
          publicSlug,
        },
      },
    };
    setEditDraft(nextDraft);
    await persistEvent(nextDraft, "Slug pública atualizada.");
  };

  const updatePartyConfigDraft = (patch: Partial<ReturnType<typeof normalizeEventPartyConfig>>) => {
    setEditDraft((previous) => {
      if (!previous) return previous;
      const currentConfig = normalizeEventPartyConfig(previous.dataExtra.raw);
      return {
        ...previous,
        dataExtra: {
          ...previous.dataExtra,
          raw: serializeEventPartyConfig(previous.dataExtra.raw, {
            ...currentConfig,
            ...patch,
          }),
        },
      };
    });
  };

  const handleSavePartyConfig = async () => {
    if (!editDraft) return;
    const nextDraft = {
      ...editDraft,
      dataExtra: {
        ...editDraft.dataExtra,
        raw: serializeEventPartyConfig(editDraft.dataExtra.raw, normalizeEventPartyConfig(editDraft.dataExtra.raw)),
      },
    };
    setEditDraft(nextDraft);
    await persistEvent(nextDraft, "Modo vendas atualizado.");
  };

  const handleTogglePartyMode = async () => {
    if (!editDraft || savingEvento) return;
    const currentConfig = normalizeEventPartyConfig(editDraft.dataExtra.raw);
    const nextConfig = {
      ...currentConfig,
      enabled: !currentConfig.enabled,
    };
    const nextDraft = {
      ...editDraft,
      dataExtra: {
        ...editDraft.dataExtra,
        raw: serializeEventPartyConfig(editDraft.dataExtra.raw, nextConfig),
      },
    };
    setEditDraft(nextDraft);
    setEvento(nextDraft);
    await persistEvent(
      nextDraft,
      nextConfig.enabled ? "Modo vendas ativado." : "Modo vendas desativado."
    );
  };

  const buildPartyEvent = (): EventPartyEvent | null => {
    const source = editDraft || evento;
    if (!source) return null;
    return {
      id: source.id,
      titulo: source.titulo,
      data: source.data,
      hora: source.hora,
      imagem: source.imagem,
      tenantId: activeTenantId || "",
      visibility: "public",
      paymentConfig: (source.paymentConfig ?? null) as unknown as Record<string, unknown> | null,
      config: normalizeEventPartyConfig(source.dataExtra.raw),
    };
  };

  const handleCreatePartyProduct = async () => {
    const partyEvent = buildPartyEvent();
    if (!partyEvent) return;
    const editingProduct = editingPartyProductId
      ? partyProducts.find((product) => product.id === editingPartyProductId) ?? null
      : null;
    const nome = partyProductDraft.nome.trim().slice(0, 120);
    const preco = parseCurrency(partyProductDraft.preco);
    const section =
      partyProductDraft.secao.trim().slice(0, 80) ||
      partyProductDraft.categoria.trim().slice(0, 80) ||
      partyEvent.config.categoryName ||
      "Geral";
    const categoria = section;
    const order = editingProduct
      ? getEventPartyProductOrder(editingProduct)
      : Math.max(1, partyProducts.length + 1);
    if (!nome) {
      addToast("Informe o nome do produto.", "error");
      return;
    }
    if (preco <= 0) {
      addToast("Informe o preço do produto.", "error");
      return;
    }
    try {
      await upsertEventPartyProduct({
        event: partyEvent,
        productId: editingProduct?.id,
        tenantId: activeTenantId || undefined,
        data: {
          nome,
          preco,
          categoria,
          descricao: partyProductDraft.descricao.trim().slice(0, 500),
          img: partyProductDraft.img,
          estoque: Math.max(0, Math.floor(Number(partyProductDraft.estoque || 0) || 0)),
          section,
          order,
        },
      });
      setPartyProductDraft({
        nome: "",
        preco: "",
        categoria,
        secao: section,
        descricao: "",
        img: "",
        estoque: "0",
        ordem: "",
      });
      setPartyNewSectionName("");
      setEditingPartyProductId("");
      await loadPartyProducts();
      addToast(editingProduct ? "Produto atualizado no menu." : "Produto adicionado ao menu.", "success");
    } catch (error: unknown) {
      console.error(error);
      addToast("Erro ao adicionar produto.", "error");
    }
  };

  const handlePartyProductImageUpload = async (file: File) => {
    const partyEvent = buildPartyEvent();
    if (!partyEvent || uploadingPartyProductImage) return;
    setUploadingPartyProductImage(true);
    try {
      const tenantScope = sanitizeStoragePathSegment(activeTenantId || "global");
      const stableProductId = editingPartyProductId.trim();
      const objectDir = stableProductId
        ? `eventos/${tenantScope}/${sanitizeStoragePathSegment(partyEvent.id)}/produtos/${sanitizeStoragePathSegment(stableProductId)}`
        : `eventos/${tenantScope}/${sanitizeStoragePathSegment(partyEvent.id)}/produtos`;
      const { url, error } = await uploadImage(file, objectDir, {
        scopeKey: `admin:eventos:ficha-produto:${tenantScope}:${partyEvent.id}`,
        fileName: stableProductId ? "produto" : buildDraftAssetFileName("produto"),
        upsert: Boolean(stableProductId),
        versionStrategy: stableProductId ? "file-metadata" : "none",
        cacheControl: VERSIONED_PUBLIC_ASSET_CACHE_CONTROL,
        maxBytes: 200 * 1024,
        maxWidth: 2400,
        maxHeight: 2400,
        compressionMaxBytes: 200 * 1024,
        appendVersionQuery: true,
      });
      if (error || !url) {
        addToast(error || "Erro ao enviar foto do produto.", "error");
        return;
      }
      setPartyProductDraft((previous) => ({ ...previous, img: url }));
      addToast("Foto do produto enviada.", "success");
    } catch (error: unknown) {
      console.error(error);
      addToast("Erro ao enviar foto do produto.", "error");
    } finally {
      setUploadingPartyProductImage(false);
    }
  };

  const handlePartyOrderStatus = async (order: EventPartyOrder, status: "approved" | "rejected" | "delivered") => {
    setPartyOrderMutatingId(order.id);
    try {
      if (status === "approved") {
        await approveStoreOrder({
          orderId: order.id,
          userId: order.userId,
          userName: order.userName,
          productName: order.productName,
          price: order.total || order.price,
          approvedBy: user?.nome || "Admin",
          productId: order.productId,
          quantidade: order.quantidade,
        });
      } else if (status === "delivered") {
        await markEventPartyOrderDelivered({
          order,
          operatorUserId: user?.uid,
          operatorName: user?.nome || "Admin",
        });
      } else {
        await setStoreOrderStatus({
          orderId: order.id,
          status,
          approvedBy: user?.nome || "Admin",
        });
      }
      await loadPartyOrders();
      addToast("Pedido atualizado.", "success");
    } catch (error: unknown) {
      console.error(error);
      addToast("Erro ao atualizar pedido.", "error");
    } finally {
      setPartyOrderMutatingId("");
    }
  };

  const handleCreateManualPartyOrder = async () => {
    const partyEvent = buildPartyEvent();
    if (!partyEvent) return;
    const product = partyProducts.find((entry) => entry.id === manualPartyOrderDraft.productId);
    if (!product) {
      addToast("Selecione um produto para criar a ficha manual.", "error");
      return;
    }
    if (!manualPartyOrderDraft.userName.trim()) {
      addToast("Informe o nome do usuário.", "error");
      return;
    }
    setPartyOrderMutatingId("manual");
    try {
      await createManualEventPartyOrder({
        event: partyEvent,
        product,
        userId: manualPartyOrderDraft.userId,
        userName: manualPartyOrderDraft.userName.trim().slice(0, 120),
        quantity: Math.max(1, Math.floor(Number(manualPartyOrderDraft.quantidade || 1) || 1)),
        tenantId: activeTenantId || undefined,
        createdByUserId: user?.uid,
        createdByName: user?.nome || "Admin",
        manualCode: manualPartyOrderDraft.manualCode,
        externalNumber: manualPartyOrderDraft.externalNumber || manualPartyOrderDraft.ra,
        manualCustomer: {
          ra: manualPartyOrderDraft.ra,
        },
      });
      setManualPartyOrderDraft((previous) => ({
        productId: previous.productId,
        userId: "",
        userName: "",
        quantidade: "1",
        manualCode: "",
        externalNumber: "",
        cpf: "",
        telefone: "",
        email: "",
        ra: "",
      }));
      await loadPartyOrders();
      addToast(
        manualPartyOrderDraft.manualCode.trim()
          ? "Ficha manual criada e enviada para retirada pendente."
          : "Ficha manual criada.",
        "success"
      );
    } catch (error: unknown) {
      console.error(error);
      addToast("Erro ao criar ficha manual.", "error");
    } finally {
      setPartyOrderMutatingId("");
    }
  };

  const handleSelectPartyUser = (selectedUser: EventPartyUserOption) => {
    setManualPartyOrderDraft((previous) => ({
      ...previous,
      userId: selectedUser.source === "app" ? selectedUser.id : "",
      userName: selectedUser.nome,
      externalNumber: selectedUser.numero || selectedUser.ra || previous.externalNumber,
      cpf: "",
      telefone: "",
      email: "",
      ra: selectedUser.ra || selectedUser.numero,
    }));
    addToast("Usuário selecionado para a ficha.", "success");
  };

  const handleSavePartyManualUser = async () => {
    if (!editDraft || savingManualPartyUserRef.current) return;
    const nome = partyUserCadastroDraft.nome.trim().slice(0, 120);
    const numero = partyUserCadastroDraft.numero.trim().slice(0, 80);
    const cleanCpf = normalizeDigits(partyUserCadastroDraft.cpf).slice(0, CPF_MAX_DIGITS);
    const phoneDigits = normalizePhoneInput(partyUserCadastroDraft.telefone.trim(), PHONE_MAX_LENGTH);
    const cleanTelefone = phoneDigits ? normalizePhoneToBrE164(phoneDigits) : "";
    const cleanEmail = formatEmailCadastroInput(partyUserCadastroDraft.email);
    const cleanRa = partyUserCadastroDraft.ra.trim().slice(0, 80);
    const cleanTurma = partyUserCadastroDraft.turma.trim().slice(0, 120);
    const cleanValorPorta = formatCurrencyTextInput(partyUserCadastroDraft.valorPorta);
    const editingEntry = editingManualPartyUserId
      ? manualRegisteredUsers.find(
          (entry) => entry.orderId === editingManualPartyUserId || entry.id === editingManualPartyUserId
        ) ?? null
      : null;

    if (!nome) {
      addToast("Informe o nome do usuário.", "error");
      return;
    }
    if (!numero) {
      addToast("Informe o nº da pulseira para registrar o usuário.", "error");
      return;
    }
    if (cleanCpf && !isValidCpf(cleanCpf)) {
      addToast("CPF inválido. Confira os 11 dígitos.", "error");
      return;
    }
    if (phoneDigits && !hasValidPhoneLength(phoneDigits)) {
      addToast("Telefone inválido. Use DDD e número completo.", "error");
      return;
    }
    if (cleanEmail && !isValidEmail(cleanEmail)) {
      addToast("E-mail inválido.", "error");
      return;
    }

    savingManualPartyUserRef.current = true;
    try {
      const now = new Date().toISOString();
      const raw = asRecord(editDraft.dataExtra.raw) ?? {};
      const eventParty = asRecord(raw.eventParty) ?? {};
      const previousUsers = getManualPartyUsers(raw);

      const manualEntry = editingEntry?.orderId
        ? await updateManualGateEntry({
            orderId: editingEntry.orderId,
            holderName: nome,
            holderTurma: cleanTurma || "Porta",
            braceletNumber: numero,
            valorPorta: cleanValorPorta,
            cpf: cleanCpf,
            telefone: cleanTelefone,
            email: cleanEmail,
            ra: cleanRa || numero,
          })
        : await createManualGateEntry({
            holderName: nome,
            holderTurma: cleanTurma || "Porta",
            braceletNumber: numero,
            valorPorta: cleanValorPorta,
            cpf: cleanCpf,
            telefone: cleanTelefone,
            email: cleanEmail,
            ra: cleanRa || numero,
          });
      const nextUser: EventPartyManualUser = {
        id: editingEntry?.id || `manual-${Date.now()}`,
        orderId: editingEntry?.orderId || asString(manualEntry?.orderId),
        nome,
        cpf: cleanCpf,
        telefone: cleanTelefone,
        email: cleanEmail,
        numero,
        ra: cleanRa || numero,
        turma: cleanTurma,
        valorPorta: cleanValorPorta,
        createdAt: editingEntry?.createdAt || now,
        createdByName: editingEntry?.createdByName || user?.nome || "Admin",
      };
      const nextEvent = cloneEvent(editDraft);
      nextEvent.dataExtra.raw = {
        ...raw,
        eventParty: {
          ...eventParty,
          manualUsers: [
            ...previousUsers.filter((entry) => {
              const sameOrder = nextUser.orderId && entry.orderId === nextUser.orderId;
              const sameId = entry.id === nextUser.id;
              const sameBracelet = entry.numero && entry.numero === numero;
              const sameCpf = cleanCpf && entry.cpf === cleanCpf;
              return !sameOrder && !sameId && !sameBracelet && !sameCpf;
            }),
            nextUser,
          ],
          updatedAt: now,
        },
      };
      setEvento(nextEvent);
      setEditDraft(nextEvent);
      await persistEvent(nextEvent, "Usuário cadastrado.");
      setPartyUserCadastroDraft({
        nome: "",
        cpf: "",
        telefone: "",
        email: "",
        numero: "",
        ra: "",
        turma: "",
        valorPorta: "",
      });
      setEditingManualPartyUserId("");
      await loadPartyUsers();
      await loadSales(true);
    } catch (error: unknown) {
      console.error(error);
      addToast(error instanceof Error ? error.message : "Erro ao salvar usuário.", "error");
    } finally {
      savingManualPartyUserRef.current = false;
    }
  };

  const handleEditManualPartyUser = (entry: EventPartyManualUser) => {
    setEditingManualPartyUserId(entry.orderId || entry.id);
    setPartyUserCadastroDraft({
      nome: entry.nome,
      cpf: formatCpfInput(entry.cpf),
      telefone: formatBrazilPhoneInput(entry.telefone),
      email: entry.email,
      numero: entry.numero,
      ra: entry.ra,
      turma: entry.turma,
      valorPorta: entry.valorPorta,
    });
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const handleCancelManualPartyUserEdit = () => {
    setEditingManualPartyUserId("");
    setPartyUserCadastroDraft({
      nome: "",
      cpf: "",
      telefone: "",
      email: "",
      numero: "",
      ra: "",
      turma: "",
      valorPorta: "",
    });
  };

  const handleDeleteManualPartyUser = async (entry: EventPartyManualUser) => {
    if (!editDraft || savingManualPartyUserRef.current) return;
    if (!window.confirm(`Excluir o cadastro manual de ${entry.nome}?`)) return;

    savingManualPartyUserRef.current = true;
    try {
      if (entry.orderId) {
        await deleteManualGateEntry(entry.orderId);
      }

      const now = new Date().toISOString();
      const raw = asRecord(editDraft.dataExtra.raw) ?? {};
      const eventParty = asRecord(raw.eventParty) ?? {};
      const nextEvent = cloneEvent(editDraft);
      nextEvent.dataExtra.raw = {
        ...raw,
        eventParty: {
          ...eventParty,
          manualUsers: getManualPartyUsers(raw).filter((candidate) => {
            const sameOrder = entry.orderId && candidate.orderId === entry.orderId;
            const sameId = candidate.id === entry.id;
            const sameBracelet = candidate.numero && candidate.numero === entry.numero;
            return !sameOrder && !sameId && !sameBracelet;
          }),
          updatedAt: now,
        },
      };
      setEvento(nextEvent);
      setEditDraft(nextEvent);
      await persistEvent(nextEvent, "Cadastro manual excluído.");
      if (editingManualPartyUserId === entry.orderId || editingManualPartyUserId === entry.id) {
        handleCancelManualPartyUserEdit();
      }
      await loadPartyUsers();
      await loadSales(true);
    } catch (error: unknown) {
      console.error(error);
      addToast(error instanceof Error ? error.message : "Erro ao excluir cadastro manual.", "error");
    } finally {
      savingManualPartyUserRef.current = false;
    }
  };

  const handleEditPartyOrder = async (order: EventPartyOrder) => {
    const nextQuantityRaw = window.prompt("Nova quantidade", String(order.quantidade));
    if (nextQuantityRaw === null) return;
    const nextQuantity = Math.max(1, Math.floor(Number(nextQuantityRaw.replace(/\D/g, "")) || 1));
    setPartyOrderMutatingId(order.id);
    try {
      await updateEventPartyOrder({
        order,
        quantity: nextQuantity,
        editedByName: user?.nome || "Admin",
      });
      await loadPartyOrders();
      addToast("Pedido editado.", "success");
    } catch (error: unknown) {
      console.error(error);
      addToast("Erro ao editar pedido.", "error");
    } finally {
      setPartyOrderMutatingId("");
    }
  };

  const handleDeletePartyOrder = async (order: EventPartyOrder) => {
    if (!window.confirm("Excluir este pedido/ficha?")) return;
    setPartyOrderMutatingId(order.id);
    try {
      await deleteEventPartyOrder({ orderId: order.id, tenantId: activeTenantId || undefined });
      await loadPartyOrders();
      addToast("Pedido excluído.", "success");
    } catch (error: unknown) {
      console.error(error);
      addToast("Erro ao excluir pedido.", "error");
    } finally {
      setPartyOrderMutatingId("");
    }
  };

  const handleEditPartyProduct = (product: EventPartyProduct) => {
    setEditingPartyProductId(product.id);
    setPartyProductDraft({
      nome: product.nome,
      preco: formatCurrency(product.preco).replace("R$", "").trim(),
      categoria: product.categoria || normalizeEventPartyConfig((editDraft || evento)?.dataExtra.raw).categoryName,
      secao: getEventPartyProductSection(product),
      descricao: product.descricao,
      img: product.img,
      estoque: String(product.estoque),
      ordem: String(getEventPartyProductOrder(product)),
    });
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const handleClearPartyProductDraft = () => {
    setEditingPartyProductId("");
    setPartyNewSectionName("");
    setPartyProductDraft({
      nome: "",
      preco: "",
      categoria: normalizeEventPartyConfig((editDraft || evento)?.dataExtra.raw).categoryName,
      secao: normalizeEventPartyConfig((editDraft || evento)?.dataExtra.raw).categoryName,
      descricao: "",
      img: "",
      estoque: "0",
      ordem: "",
    });
  };

  const handleReorderPartyProducts = async (
    targetProduct: EventPartyProduct,
    groupProducts: EventPartyProduct[]
  ) => {
    const draggingId = draggingPartyProductId.trim();
    setDraggingPartyProductId("");
    if (!draggingId || draggingId === targetProduct.id) return;

    const fromIndex = groupProducts.findIndex((product) => product.id === draggingId);
    const toIndex = groupProducts.findIndex((product) => product.id === targetProduct.id);
    if (fromIndex < 0 || toIndex < 0) return;

    const reorderedProducts = [...groupProducts];
    const [movedProduct] = reorderedProducts.splice(fromIndex, 1);
    reorderedProducts.splice(toIndex, 0, movedProduct);
    const sectionName = getEventPartyProductSection(targetProduct);
    const savingKey = `order:${sectionName}`;
    setSavingPartyProductMetaId(savingKey);
    try {
      await Promise.all(
        reorderedProducts.map((product, index) =>
          updateEventPartyProductMeta({
            product,
            tenantId: activeTenantId || undefined,
            section: sectionName,
            order: index + 1,
          })
        )
      );
      await loadPartyProducts();
      addToast("Ordem do menu atualizada.", "success");
    } catch (error: unknown) {
      console.error(error);
      addToast("Erro ao reordenar produtos.", "error");
    } finally {
      setSavingPartyProductMetaId("");
    }
  };

  const handleSavePartyProductMeta = async (product: EventPartyProduct) => {
    const draft = partyProductMetaDrafts[product.id];
    if (!draft) return;
    setSavingPartyProductMetaId(product.id);
    try {
      await updateEventPartyProductMeta({
        product,
        tenantId: activeTenantId || undefined,
        section: draft.section.trim().slice(0, 80) || "Geral",
        order: Number.parseInt(draft.order, 10) || 9999,
      });
      await loadPartyProducts();
      addToast("Produto atualizado no menu.", "success");
    } catch (error: unknown) {
      console.error(error);
      addToast("Erro ao organizar produto.", "error");
    } finally {
      setSavingPartyProductMetaId("");
    }
  };

  const handleImageUpload = async (file: File) => {
    if (!editDraft || uploadingImage) return;
    setUploadingImage(true);
    try {
      const tenantScope = sanitizeStoragePathSegment(activeTenantId || "global");
      const objectDir = `eventos/${tenantScope}/${sanitizeStoragePathSegment(editDraft.id || "draft")}`;
      const { url, error } = await uploadImage(file, objectDir, {
        scopeKey: `admin:eventos:capa:${tenantScope}:${editDraft.id || "draft"}`,
        fileName: editDraft.id ? "capa" : buildDraftAssetFileName("capa"),
        upsert: Boolean(editDraft.id),
        versionStrategy: editDraft.id ? "file-metadata" : "none",
        cacheControl: VERSIONED_PUBLIC_ASSET_CACHE_CONTROL,
        maxBytes: 3 * 1024 * 1024,
        maxWidth: 2400,
        maxHeight: 1800,
        maxPixels: 3_600_000,
        compressionMaxWidth: 1800,
        compressionMaxHeight: 1200,
        compressionMaxBytes: 200 * 1024,
        quality: 0.82,
        rateLimitMax: 4,
      });
      if (error || !url) {
        addToast(error || "Falha no upload da capa.", "error");
        return;
      }
      setEditDraft((previous) => (previous ? { ...previous, imagem: url } : previous));
    } finally {
      setUploadingImage(false);
    }
  };

  const handleSalesPaymentToggle = async (row: EventSaleRow) => {
    const isApproving = row.status.toLowerCase() !== "aprovado";
    const amount = parseCurrency(row.valorTotal || "0");
    try {
      await setAdminTicketPayment({
        ticketRequestId: row.id,
        isApproving,
        approvedBy: user?.nome || "Admin",
      });

      if (row.userId && Number.isFinite(amount)) {
        await incrementEventPurchaseUserStats({
          userId: row.userId,
          isApproving,
          valorGasto: amount,
          lotName: row.loteNome,
          eventTitle: evento?.titulo || "Evento",
        });
      }

      if (user?.uid) {
        await logActivity(
          user.uid,
          user.nome || "Admin",
          "UPDATE",
          "Eventos/Pagamentos",
          `${isApproving ? "Aprovou" : "Reabriu"} comprovante de ${row.userName} (${evento?.titulo || "Evento"})`
        ).catch(() => {});
      }

      addToast(isApproving ? "Pagamento aprovado." : "Pagamento reaberto.", "success");
      await loadSales(true);
    } catch (error: unknown) {
      console.error(error);
      addToast("Erro ao atualizar pagamento.", "error");
    }
  };

  const handleTicketManualCheckin = async (row: EventSaleRow) => {
    if (ticketCheckinMutatingId) return;
    setTicketCheckinMutatingId(row.id);
    try {
      const session = await getSupabaseClient().auth.getSession();
      const token = session.data.session?.access_token || "";
      const response = await fetch("/api/admin/event-tickets/scan", {
        method: "POST",
        headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
        body: JSON.stringify({ orderId: row.id, eventId }),
      });
      const payload = asRecord(await response.json().catch(() => null));
      if (!response.ok) {
        throw new Error(asString(payload?.error) || "Falha ao registrar check-in.");
      }
      addToast(
        payload?.alreadyScanned ? "Ingresso já estava com check-in." : "Check-in registrado.",
        "success"
      );
      await loadSales(true);
    } catch (error: unknown) {
      console.error(error);
      addToast(error instanceof Error ? error.message : "Falha ao registrar check-in.", "error");
    } finally {
      setTicketCheckinMutatingId("");
    }
  };

  const updateTicketCheckin = async (
    row: EventSaleRow,
    action: "edit" | "undo",
    payload: Record<string, unknown> = {}
  ) => {
    if (ticketCheckinMutatingId) return;
    const latestEntry = getSaleLatestCheckinEntry(row);
    if (!latestEntry?.token) {
      addToast("Nenhuma baixa encontrada para editar.", "error");
      return;
    }
    setTicketCheckinMutatingId(row.id);
    try {
      const session = await getSupabaseClient().auth.getSession();
      const token = session.data.session?.access_token || "";
      const response = await fetch("/api/admin/event-tickets/checkin", {
        method: "POST",
        headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
        body: JSON.stringify({
          action,
          orderId: row.id,
          eventId,
          ticketToken: latestEntry.token,
          ...payload,
        }),
      });
      const responsePayload = asRecord(await response.json().catch(() => null));
      if (!response.ok) {
        throw new Error(asString(responsePayload?.error) || "Falha ao atualizar check-in.");
      }
      addToast(action === "undo" ? "Check-in desfeito." : "Check-in atualizado.", "success");
      await loadSales(true);
    } catch (error: unknown) {
      console.error(error);
      addToast(error instanceof Error ? error.message : "Falha ao atualizar check-in.", "error");
    } finally {
      setTicketCheckinMutatingId("");
    }
  };

  const handleEditTicketCheckin = async (row: EventSaleRow) => {
    const latestEntry = getSaleLatestCheckinEntry(row);
    if (!latestEntry) {
      addToast("Nenhuma baixa encontrada para editar.", "error");
      return;
    }
    const scannedByUserName = window.prompt(
      "Nome de quem fez a baixa",
      latestEntry.scannedByUserName || asString(row.rawData.checkinByUserName) || user?.nome || ""
    );
    if (scannedByUserName === null) return;
    const note = window.prompt("Observação do check-in", latestEntry.checkinNote || asString(row.rawData.checkinNote));
    if (note === null) return;
    await updateTicketCheckin(row, "edit", {
      scannedByUserName,
      note,
      scannedAt: latestEntry.scannedAt || getSaleLatestCheckinDate(row),
    });
  };

  const handleUndoTicketCheckin = async (row: EventSaleRow) => {
    if (!window.confirm("Desfazer a última baixa deste ingresso?")) return;
    const note = window.prompt("Motivo do desfazimento", "") ?? "";
    await updateTicketCheckin(row, "undo", { note });
  };

  const createManualGateEntry = async (payload: {
    holderName: string;
    holderTurma?: string;
    braceletNumber: string;
    valorPorta?: string;
    cpf?: string;
    telefone?: string;
    email?: string;
    ra?: string;
    sourceOrderId?: string;
    sourceTicketToken?: string;
  }) => {
    const session = await getSupabaseClient().auth.getSession();
    const token = session.data.session?.access_token || "";
    const response = await fetch("/api/admin/event-tickets/manual-entry", {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
      body: JSON.stringify({
        eventId,
        ...payload,
      }),
    });
    const responsePayload = asRecord(await response.json().catch(() => null));
    if (!response.ok) {
      throw new Error(asString(responsePayload?.error) || "Falha ao cadastrar entrada manual.");
    }
    return responsePayload;
  };

  const updateManualGateEntry = async (payload: {
    orderId: string;
    holderName: string;
    holderTurma?: string;
    braceletNumber: string;
    valorPorta?: string;
    cpf?: string;
    telefone?: string;
    email?: string;
    ra?: string;
  }) => {
    const session = await getSupabaseClient().auth.getSession();
    const token = session.data.session?.access_token || "";
    const response = await fetch("/api/admin/event-tickets/manual-entry", {
      method: "PATCH",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
      body: JSON.stringify({
        eventId,
        ...payload,
      }),
    });
    const responsePayload = asRecord(await response.json().catch(() => null));
    if (!response.ok) {
      throw new Error(asString(responsePayload?.error) || "Falha ao editar entrada manual.");
    }
    return responsePayload;
  };

  const deleteManualGateEntry = async (orderId: string) => {
    const session = await getSupabaseClient().auth.getSession();
    const token = session.data.session?.access_token || "";
    const response = await fetch("/api/admin/event-tickets/manual-entry", {
      method: "DELETE",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
      body: JSON.stringify({ eventId, orderId }),
    });
    const responsePayload = asRecord(await response.json().catch(() => null));
    if (!response.ok) {
      throw new Error(asString(responsePayload?.error) || "Falha ao excluir entrada manual.");
    }
    return responsePayload;
  };

  const handleManualTicketTransfer = async (row: EventSaleRow) => {
    if (ticketCheckinMutatingId) return;
    const activeTicket = row.paymentConfig?.ticketEntries?.find((entry) => entry.status === "ativo");
    if (!activeTicket?.token) {
      addToast("Esse pedido não tem ingresso ativo disponível para transferência.", "error");
      return;
    }
    const holderName = window.prompt("Nome do novo participante", "");
    if (holderName === null) return;
    const cleanHolderName = holderName.trim();
    if (!cleanHolderName) {
      addToast("Informe o nome do novo participante.", "error");
      return;
    }
    const braceletNumber = window.prompt("Nº da pulseira", "");
    if (braceletNumber === null) return;
    const cleanBraceletNumber = braceletNumber.trim();
    if (!cleanBraceletNumber) {
      addToast("Informe o nº da pulseira.", "error");
      return;
    }
    const holderTurma = window.prompt("Turma ou identificação (opcional)", "Porta");
    if (holderTurma === null) return;

    setTicketCheckinMutatingId(row.id);
    try {
      await createManualGateEntry({
        holderName: cleanHolderName,
        holderTurma: holderTurma.trim() || "Porta",
        braceletNumber: cleanBraceletNumber,
        sourceOrderId: row.id,
        sourceTicketToken: activeTicket.token,
      });
      addToast("Transferência manual registrada com entrada liberada.", "success");
      await loadSales(true);
    } catch (error: unknown) {
      console.error(error);
      addToast(error instanceof Error ? error.message : "Falha ao transferir ingresso.", "error");
    } finally {
      setTicketCheckinMutatingId("");
    }
  };

  const stopTicketScanner = useCallback(async () => {
    const scanner = ticketScannerRef.current;
    if (!scanner) return;
    try {
      if (scanner.isScanning) {
        await scanner.stop();
      }
    } catch {
      // scanner já estava parado.
    }
    try {
      await scanner.clear();
    } catch {
      // o elemento pode ter desmontado.
    }
    ticketScannerRef.current = null;
    setTicketScannerActive(false);
  }, []);

  const processTicketQrScan = useCallback(
    async (qrPayload: string) => {
      const cleanPayload = qrPayload.trim();
      if (!cleanPayload || ticketScannerProcessingRef.current) return;
      if (ticketScannerLastPayloadRef.current === cleanPayload) return;
      ticketScannerProcessingRef.current = true;
      ticketScannerLastPayloadRef.current = cleanPayload;
      setTicketScannerMessage("Validando QR code...");
      try {
        const session = await getSupabaseClient().auth.getSession();
        const token = session.data.session?.access_token || "";
        const response = await fetch("/api/admin/event-tickets/scan", {
          method: "POST",
          headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
          body: JSON.stringify({ qrPayload: cleanPayload, eventId }),
        });
        const payload = asRecord(await response.json().catch(() => null));
        if (!response.ok) {
          throw new Error(asString(payload?.error) || "Falha ao validar ingresso.");
        }
        setTicketScannerMessage(
          `${payload?.alreadyScanned ? "Ingresso já estava lido" : "Entrada liberada"}: ${asString(payload?.holderName, "participante")}`
        );
        addToast(payload?.alreadyScanned ? "Ingresso já estava lido." : "Entrada liberada.", "success");
        await loadSales(true);
      } catch (error: unknown) {
        console.error(error);
        const message = error instanceof Error ? error.message : "Falha ao validar ingresso.";
        setTicketScannerMessage(message);
        addToast(message, "error");
      } finally {
        window.setTimeout(() => {
          ticketScannerLastPayloadRef.current = "";
        }, 1800);
        ticketScannerProcessingRef.current = false;
      }
    },
    [addToast, eventId, loadSales]
  );

  const startTicketScanner = useCallback(async () => {
    if (ticketScannerActive || ticketScannerStarting) return;
    setTicketScannerStarting(true);
    setTicketScannerMessage("");
    try {
      const scanner = new Html5Qrcode("event-ticket-workspace-scanner");
      ticketScannerRef.current = scanner;
      const cameras = await Html5Qrcode.getCameras().catch(() => []);
      const preferredCamera = cameras.find((camera) => /back|rear|traseira/i.test(camera.label));
      await scanner.start(
        preferredCamera?.id || cameras[0]?.id || { facingMode: "environment" },
        {
          fps: 12,
          qrbox: (viewfinderWidth: number, viewfinderHeight: number) => {
            const minEdge = Math.max(1, Math.min(viewfinderWidth, viewfinderHeight));
            const size = Math.min(320, Math.max(220, Math.floor(minEdge * 0.72)));
            return { width: size, height: size };
          },
          disableFlip: false,
        },
        (decodedText) => {
          void processTicketQrScan(decodedText);
        },
        () => undefined
      );
      setTicketScannerActive(true);
    } catch (error: unknown) {
      console.error(error);
      setTicketScannerMessage("Não foi possível iniciar a câmera do scanner.");
      await stopTicketScanner();
    } finally {
      setTicketScannerStarting(false);
    }
  }, [processTicketQrScan, stopTicketScanner, ticketScannerActive, ticketScannerStarting]);

  useEffect(() => {
    if (section !== "scan") {
      void stopTicketScanner();
    }
  }, [section, stopTicketScanner]);

  useEffect(() => {
    return () => {
      void stopTicketScanner();
    };
  }, [stopTicketScanner]);

  const handleCreatePoll = async () => {
    const normalizedOptions = pollDraftOptions
      .map((option) => option.trim().slice(0, EVENT_POLL_OPTION_MAX_CHARS))
      .filter((option, index, array) => option.length > 0 && array.indexOf(option) === index)
      .slice(0, EVENT_POLL_OPTION_MAX_COUNT);
    if (!pollQuestion.trim()) {
      addToast("Digite a pergunta da enquete.", "error");
      return;
    }
    if (!pollAllowUserOptions && normalizedOptions.length < 2) {
      addToast("Mantenha pelo menos duas respostas para uma enquete fechada.", "error");
      return;
    }
    try {
      await createAdminEventPoll({
        eventId,
        question: pollQuestion.trim().slice(0, EVENT_POLL_QUESTION_MAX_CHARS),
        allowUserOptions: pollAllowUserOptions,
        options: normalizedOptions.map((text) => ({ text, votes: 0 })),
        tenantId: activeTenantId || undefined,
      });
      setPollQuestion("");
      setPollAllowUserOptions(true);
      setPollDraftOptions(["", ""]);
      addToast("Enquete criada.", "success");
      await loadPolls(true);
    } catch (error: unknown) {
      console.error(error);
      addToast("Erro ao criar enquete.", "error");
    }
  };

  const handleDeletePoll = async (pollId: string) => {
    if (!window.confirm("Excluir enquete?")) return;
    try {
      await deleteAdminEventPoll({
        eventId,
        pollId,
        tenantId: activeTenantId || undefined,
      });
      addToast("Enquete removida.", "success");
      await loadPolls(true);
    } catch (error: unknown) {
      console.error(error);
      addToast("Erro ao excluir enquete.", "error");
    }
  };

  const handleQuickEditPoll = async (poll: EventPoll) => {
    const nextQuestion = window.prompt("Editar pergunta da enquete:", poll.question)?.trim();
    if (!nextQuestion) return;

    const nextOptionsText = window.prompt(
      "Edite as respostas, uma por linha. Ao salvar, os votos serão zerados.",
      poll.options.map((option) => option.text).join("\n")
    );
    if (nextOptionsText === null) return;

    const nextOptions = nextOptionsText
      .split(/\r?\n/)
      .map((option) => option.trim())
      .filter(Boolean)
      .slice(0, EVENT_POLL_OPTION_MAX_COUNT)
      .map((text) => ({ text: text.slice(0, EVENT_POLL_OPTION_MAX_CHARS), votes: 0 }));

    if (!poll.allowUserOptions && nextOptions.length < 2) {
      addToast("Mantenha pelo menos duas respostas para uma enquete fechada.", "error");
      return;
    }

    if (!window.confirm("Editar esta enquete vai zerar as respostas dos usuários. Continuar?")) {
      return;
    }

    try {
      await updateAdminEventPoll({
        eventId,
        pollId: poll.id,
        question: nextQuestion,
        allowUserOptions: poll.allowUserOptions,
        options: nextOptions,
        tenantId: activeTenantId || undefined,
      });
      await loadPolls(true);
      addToast("Enquete editada e respostas zeradas.", "success");
    } catch (error: unknown) {
      console.error(error);
      addToast("Erro ao editar enquete.", "error");
    }
  };

  const handleDeletePollOption = async (poll: EventPoll, optionIndex: number) => {
    if (!window.confirm("Remover esta opção da enquete?")) return;
    const nextOptions = poll.options.filter((_, index) => index !== optionIndex);
    if (!poll.allowUserOptions && nextOptions.length < 2) {
      addToast("Mantenha pelo menos duas respostas para uma enquete fechada.", "error");
      return;
    }
    try {
      await updateAdminEventPollOptions({
        eventId,
        pollId: poll.id,
        options: nextOptions,
        tenantId: activeTenantId || undefined,
      });
      addToast("Opção removida.", "success");
      await loadPolls(true);
    } catch (error: unknown) {
      console.error(error);
      addToast("Erro ao atualizar enquete.", "error");
    }
  };

  /*
  const handleStartEditPoll = (poll: EventPoll) => {
    setPollEditDrafts((previous) => ({
      ...previous,
      [poll.id]: {
        question: poll.question,
        allowUserOptions: poll.allowUserOptions,
        options: poll.options.length ? poll.options.map((option) => option.text) : ["", ""],
      },
    }));
  };

  const handleCancelEditPoll = (pollId: string) => {
    setPollEditDrafts((previous) => {
      const next = { ...previous };
      delete next[pollId];
      return next;
    });
  };

  const handleSavePollEdit = async (poll: EventPoll) => {
    const draft = pollEditDrafts[poll.id];
    if (!draft) return;
    const normalizedOptions = draft.options
      .map((option) => option.trim())
      .filter(Boolean)
      .slice(0, EVENT_POLL_OPTION_MAX_COUNT)
      .map((text) => ({ text: text.slice(0, EVENT_POLL_OPTION_MAX_CHARS), votes: 0 }));

    if (!draft.question.trim()) {
      addToast("Digite a pergunta da enquete.", "error");
      return;
    }

    if (normalizedOptions.length < 2) {
      addToast("Mantenha pelo menos duas respostas.", "error");
      return;
    }

    if (!window.confirm("Editar esta enquete vai zerar as respostas dos usuários. Continuar?")) {
      return;
    }

    try {
      await updateAdminEventPoll({
        eventId,
        pollId: poll.id,
        question: draft.question,
        allowUserOptions: draft.allowUserOptions,
        options: normalizedOptions,
        tenantId: activeTenantId || undefined,
      });
      handleCancelEditPoll(poll.id);
      await loadPolls(true);
      addToast("Enquete editada e respostas zeradas.", "success");
    } catch (error: unknown) {
      console.error(error);
      addToast("Erro ao editar enquete.", "error");
    }
  };

  */
  const sortedLots = useMemo(
    () =>
      [...(evento?.lotes || [])].sort((left, right) => {
        const orderDelta = left.ordem - right.ordem;
        if (orderDelta !== 0) return orderDelta;
        return left.nome.localeCompare(right.nome, "pt-BR");
      }),
    [evento?.lotes]
  );

  const handleReorderLot = async (targetLotId: number) => {
    if (!evento || draggingLotId === null || draggingLotId === targetLotId) return;

    const nextOrder = [...sortedLots];
    const sourceIndex = nextOrder.findIndex((lot) => lot.id === draggingLotId);
    const targetIndex = nextOrder.findIndex((lot) => lot.id === targetLotId);
    if (sourceIndex < 0 || targetIndex < 0) return;

    const [draggedLot] = nextOrder.splice(sourceIndex, 1);
    nextOrder.splice(targetIndex, 0, draggedLot);

    const nextEvent = cloneEvent(evento);
    nextEvent.lotes = nextOrder.map((lot, index) => ({
      ...lot,
      ordem: index,
      qrPorIngresso: 1,
    }));
    await persistEvent(nextEvent, "Ordem dos lotes atualizada.");
  };

  const filteredSales = useMemo(() => {
    const search = normalizeSearch(salesSearch);
    return salesRows.filter((row) => {
      const matchesSearch =
        !search ||
        normalizeSearch(
          `${row.userName} ${row.userTurma} ${row.loteNome} ${row.id} ${row.status}`
        ).includes(search);
      const matchesStatus =
        salesStatusFilter === "todos" ||
        (salesStatusFilter === "aprovado" && row.status.toLowerCase() === "aprovado") ||
        (salesStatusFilter === "pendente" && row.status.toLowerCase() === "pendente") ||
        (salesStatusFilter === "analise" && row.status.toLowerCase() === "analise");
      return matchesSearch && matchesStatus;
    });
  }, [salesRows, salesSearch, salesStatusFilter]);

  const totalSalesPages = useMemo(
    () => Math.max(1, Math.ceil(filteredSales.length / SALES_PAGE_SIZE)),
    [filteredSales.length]
  );

  const paginatedSales = useMemo(() => {
    const safePage = Math.min(totalSalesPages, Math.max(1, salesPage));
    const start = (safePage - 1) * SALES_PAGE_SIZE;
    return filteredSales.slice(start, start + SALES_PAGE_SIZE);
  }, [filteredSales, salesPage, totalSalesPages]);

  const partyProductById = useMemo(
    () => new Map(partyProducts.map((product) => [product.id, product])),
    [partyProducts]
  );

  const statementRows = useMemo<EventStatementRow[]>(() => {
    const ticketRows: EventStatementRow[] = salesRows.map((row) => {
      const rowData = asRecord(row.rawData.data) ?? {};
      const latestCheckin = getSaleLatestCheckinEntry(row);
      const discount = resolveTicketDiscount(row);
      const ticketEntries = row.paymentConfig?.ticketEntries ?? [];
      const transferSummary = resolveTicketTransferSummary(ticketEntries);
      const approvalMethod = normalizeMethodLabel(
        asString(row.rawData.approvalMethod) ||
          asString(row.rawData.aprovacaoMetodo) ||
          asString(row.rawData.approvalSource),
        row.aprovadoPor ? "Manual" : "-"
      );
      const expectedUnitPrice = findEventLotPrice(evento, row.loteId, row.loteNome);
      const expectedValue = Number.isFinite(expectedUnitPrice) ? expectedUnitPrice * row.quantidade : Number.NaN;
      const paymentSource =
        asString(row.rawData.paymentSource) ||
        asString(row.rawData.paymentMethod) ||
        asString(rowData.paymentSource) ||
        asString(rowData.paymentMethod) ||
        (row.paymentConfig ? "Configuração de pagamento" : "-");
      const createdBy =
        asString(row.rawData.createdByName) ||
        asString(row.rawData.createdBy) ||
        asString(rowData.createdByName) ||
        asString(rowData.createdBy) ||
        "-";

      return {
        id: row.id,
        kind: "ingresso",
        status: row.status,
        statusFilter: normalizeStatementStatus(row.status),
        userName: row.userName,
        userTurma: row.userTurma,
        itemName: asString(row.rawData.itemName) || row.loteNome || "Ingresso",
        loteNome: row.loteNome || "-",
        categoria: asString(row.rawData.itemCategory) || resolveTicketCategory(row),
        quantidade: row.quantidade,
        valorTotal: parseCurrency(row.valorTotal),
        descontoValor: discount.value,
        descontoFonte: discount.source,
        expectedValue,
        requestAt: row.dataSolicitacao,
        approvalAt: row.dataAprovacao,
        approvedBy: row.aprovadoPor || "-",
        approvalMethod,
        paymentSource,
        paymentAt: getSalePaymentDate(row),
        checkinAt: row.rawData.checkinAt || latestCheckin?.scannedAt || null,
        checkinBy: asString(row.rawData.checkinByUserName) || latestCheckin?.scannedByUserName || "-",
        checkinMethod: normalizeMethodLabel(
          asString(row.rawData.checkinMethod),
          latestCheckin ? (latestCheckin.scanSource === "manual" ? "Manual" : "QR") : "-"
        ),
        checkinNote: latestCheckin?.checkinNote || asString(row.rawData.checkinNote) || "-",
        checkinEditedAt: row.rawData.checkinEditedAt || latestCheckin?.checkinEditedAt || null,
        checkinEditedBy: asString(row.rawData.checkinEditedByUserName) || latestCheckin?.checkinEditedByUserName || "-",
        createdBy,
        source: asBoolean(rowData.manualGateEntry)
          ? (asBoolean(rowData.manualTransfer) ? "Transferência manual" : "Cadastro manual")
          : "App",
        userCode:
          asString(row.rawData.pulseira) ||
          asString(rowData.pulseira) ||
          asString(rowData.braceletNumber) ||
          row.userTurma ||
          "-",
        qrStatus: summarizeTicketQrStatus(ticketEntries, latestCheckin ? "Utilizado" : row.status),
        qrCode: summarizeTicketCodes(ticketEntries),
        transferInfo: summarizeTicketTransfers(ticketEntries),
        transferAt: row.rawData.transferAt || transferSummary.transferAt,
        transferFromUserName: asString(row.rawData.transferFromUserName) || transferSummary.transferFromUserName,
        transferToUserName: asString(row.rawData.transferToUserName) || transferSummary.transferToUserName,
        transferByUserName: asString(row.rawData.transferByUserName) || transferSummary.transferByUserName,
        approvedNearEvent: isApprovedNearEventStart(evento, row.dataAprovacao),
      };
    });

    const productRows: EventStatementRow[] = partyOrders.map((order) => {
      const orderData = asRecord(order.data) ?? {};
      const eventPartyData = asRecord(orderData.eventParty) ?? {};
      const product = partyProductById.get(order.productId);
      const statusFilter = normalizeStatementStatus(order.status);
      const approvedBy = order.approvedBy || asString(eventPartyData.approvedByName) || "-";
      const approvalAt = statusFilter === "aprovado" ? order.eventApprovalAt || asString(eventPartyData.approvedAt) || order.updatedAt : null;
      const manualOrder =
        order.eventCreatedManually ||
        asBoolean(eventPartyData.manualOrder) ||
        asBoolean(eventPartyData.createdManually);
      const approvalMethod = manualOrder
        ? "Manual"
        : normalizeMethodLabel(order.eventApprovalMethod || asString(eventPartyData.approvalMethod), approvedBy !== "-" ? "Manual" : "-");
      const voucherSummary = getEventPartyVoucherSummary(order);
      const voucherEntries = getEventPartyVoucherEntries(order);
      const latestWithdrawalEntry =
        [...voucherEntries]
          .filter((entry) => entry.usedAt)
          .sort((left, right) => {
            const leftTime = parseDateTimeValue(left.usedAt)?.getTime() ?? 0;
            const rightTime = parseDateTimeValue(right.usedAt)?.getTime() ?? 0;
            return rightTime - leftTime;
          })[0] ?? null;
      const manualCustomer = asRecord(eventPartyData.manualCustomer) ?? {};
      const usedBy =
        order.eventCheckinByUserName ||
        latestWithdrawalEntry?.usedByUserName ||
        asString(eventPartyData.usedByUserName) ||
        "-";
      const qrStatuses = Array.from(
        new Set(
          voucherEntries.map((entry) => eventPartyVoucherStatusLabel(entry.status))
        )
      ).join(" / ");
      const qrCodes = voucherEntries.map((entry) => entry.code || entry.manualNumber).filter(Boolean).join(" | ");
      const transferInfo =
        voucherEntries
          .map((entry) =>
            entry.transferStatus ||
            (entry.transferredToUserName ? `Transferido para ${entry.transferredToUserName}` : "") ||
            (entry.transferredFromUserName ? `Transferido de ${entry.transferredFromUserName}` : "")
          )
          .filter(Boolean)
          .join(" | ") ||
        asString(eventPartyData.transferStatus) ||
        "-";
      const expectedValue = product ? product.preco * order.quantidade : Number.NaN;
      const orderPaymentConfig = asRecord(order.paymentConfig) ?? {};
      const paymentSource =
        asString(eventPartyData.paymentSource) ||
        asString(eventPartyData.paymentMethod) ||
        asString(orderPaymentConfig.method) ||
        asString(orderPaymentConfig.provider) ||
        (order.paymentConfig ? "Configuração de pagamento" : "-");
      const createdBy =
        order.eventCreatedByName ||
        asString(eventPartyData.createdByName) ||
        asString(eventPartyData.createdByUserName) ||
        "-";

      return {
        id: order.id,
        kind: "produto",
        status: order.status,
        statusFilter,
        userName: order.userName,
        userTurma: "-",
        itemName: order.eventItemName || order.productName || product?.nome || "Produto",
        loteNome: order.eventLoteNome || "-",
        categoria: order.eventItemCategory || (product ? getEventPartyProductSection(product) : asString(eventPartyData.section) || "-"),
        quantidade: order.quantidade,
        valorTotal: Number.isFinite(order.total) ? order.total : order.price,
        descontoValor: order.eventDiscountValue || "R$ 0,00",
        descontoFonte: order.eventDiscountSource || "-",
        expectedValue,
        requestAt: order.createdAt,
        approvalAt,
        approvedBy,
        approvalMethod,
        paymentSource,
        paymentAt: statusFilter === "aprovado" ? order.updatedAt : null,
        checkinAt: order.eventCheckinAt || latestWithdrawalEntry?.usedAt || asString(eventPartyData.usedAt) || null,
        checkinBy: usedBy,
        checkinMethod: normalizeMethodLabel(
          order.eventCheckinMethod || latestWithdrawalEntry?.usedMethod || asString(eventPartyData.usedMethod),
          usedBy !== "-" ? "QR" : "-"
        ),
        checkinNote: asString(eventPartyData.checkinNote) || "-",
        checkinEditedAt: asString(eventPartyData.checkinEditedAt) || null,
        checkinEditedBy: asString(eventPartyData.checkinEditedByUserName) || "-",
        createdBy,
        source: manualOrder ? "Criado manualmente" : asString(eventPartyData.source) || "Checkout público",
        userCode:
          asString(eventPartyData.externalNumber) ||
          asString(manualCustomer.ra) ||
          asString(manualCustomer.externalNumber) ||
          "-",
        qrStatus: qrStatuses || eventPartyVoucherStatusLabel(voucherSummary.status),
        qrCode: qrCodes || "-",
        transferInfo,
        transferAt: latestWithdrawalEntry?.transferredAt || asString(eventPartyData.transferredAt) || null,
        transferFromUserName: latestWithdrawalEntry?.transferredFromUserName || asString(eventPartyData.transferredFromUserName) || "-",
        transferToUserName: latestWithdrawalEntry?.transferredToUserName || asString(eventPartyData.transferredToUserName) || "-",
        transferByUserName: asString(eventPartyData.transferByUserName) || latestWithdrawalEntry?.transferredFromUserName || "-",
        approvedNearEvent: isApprovedNearEventStart(evento, approvalAt),
      };
    });

    return [...ticketRows, ...productRows].sort((left, right) => {
      const leftTime = parseDateTimeValue(left.requestAt)?.getTime() ?? 0;
      const rightTime = parseDateTimeValue(right.requestAt)?.getTime() ?? 0;
      return rightTime - leftTime;
    });
  }, [evento, partyOrders, partyProductById, salesRows]);

  const statementSourceOptions = useMemo(
    () =>
      Array.from(new Set(statementRows.map((row) => row.source).filter((source) => source && source !== "-"))).sort(
        (left, right) => left.localeCompare(right, "pt-BR")
      ),
    [statementRows]
  );

  const statementApproverOptions = useMemo(
    () =>
      Array.from(new Set(statementRows.map((row) => row.approvedBy).filter((approver) => approver && approver !== "-"))).sort(
        (left, right) => left.localeCompare(right, "pt-BR")
      ),
    [statementRows]
  );

  const filteredStatementRows = useMemo(() => {
    const search = normalizeSearch(salesSearch);
    const sourceFilter = normalizeSearch(salesSourceFilter);
    const approverFilter = normalizeSearch(salesApproverFilter);
    return statementRows.filter((row) => {
      const matchesSearch =
        !search ||
        normalizeSearch(
          `${row.id} ${row.kind} ${row.userName} ${row.userTurma} ${row.userCode} ${row.qrStatus} ${row.qrCode} ${row.transferInfo} ${row.itemName} ${row.loteNome} ${row.categoria} ${row.status} ${row.approvedBy} ${row.approvalMethod} ${row.paymentSource} ${row.checkinBy} ${row.checkinMethod} ${row.descontoFonte} ${row.source} ${row.createdBy}`
        ).includes(search);
      const matchesStatus =
        salesStatusFilter === "todos" || row.statusFilter === salesStatusFilter;
      const matchesType =
        salesTypeFilter === "todos" || row.kind === salesTypeFilter;
      const matchesAlert = !salesAlertFilter || matchesStatementAlert(row, salesAlertFilter);
      const rowSource = normalizeSearch(row.source);
      const matchesSource =
        salesSourceFilter === "todos" ||
        rowSource === sourceFilter ||
        rowSource.includes(sourceFilter) ||
        sourceFilter.includes(rowSource) ||
        ((sourceFilter.includes("manual") || sourceFilter.includes("admin") || sourceFilter.includes("cadastro")) &&
          isManualStatementSource(row));
      const matchesApprover = salesApproverFilter === "todos" || normalizeSearch(row.approvedBy) === approverFilter;
      const matchesFlow = matchesStatementFlow(row, salesFlowFilter);
      const matchesIndicator = !salesIndicatorFilter || matchesStatementIndicator(row, salesIndicatorFilter);
      return matchesSearch && matchesStatus && matchesType && matchesAlert && matchesSource && matchesApprover && matchesFlow && matchesIndicator;
    });
  }, [
    salesAlertFilter,
    salesApproverFilter,
    salesFlowFilter,
    salesIndicatorFilter,
    salesSearch,
    salesSourceFilter,
    salesStatusFilter,
    salesTypeFilter,
    statementRows,
  ]);

  const totalStatementPages = useMemo(
    () =>
      section === "extrato" && !statementHasDeepFilter
        ? Math.max(1, salesPage + (salesHasMore ? 1 : 0))
        : Math.max(1, Math.ceil(filteredStatementRows.length / SALES_PAGE_SIZE)),
    [filteredStatementRows.length, salesHasMore, salesPage, section, statementHasDeepFilter]
  );

  const totalStatementItems = useMemo(
    () =>
      section === "extrato" && !statementHasDeepFilter
        ? (salesPage - 1) * SALES_PAGE_SIZE + filteredStatementRows.length + (salesHasMore ? 1 : 0)
        : filteredStatementRows.length,
    [filteredStatementRows.length, salesHasMore, salesPage, section, statementHasDeepFilter]
  );

  const paginatedStatementRows = useMemo(() => {
    if (section === "extrato" && !statementHasDeepFilter) return filteredStatementRows.slice(0, SALES_PAGE_SIZE);
    const safePage = Math.min(totalStatementPages, Math.max(1, salesPage));
    const start = (safePage - 1) * SALES_PAGE_SIZE;
    return filteredStatementRows.slice(start, start + SALES_PAGE_SIZE);
  }, [filteredStatementRows, salesPage, section, statementHasDeepFilter, totalStatementPages]);

  const salesRowById = useMemo(
    () => new Map(salesRows.map((row) => [row.id, row])),
    [salesRows]
  );

  const filteredPresenceRows = useMemo(() => {
    const search = normalizeSearch(salesSearch);
    return statementRows.filter((row) => {
      if (row.kind !== "ingresso" || row.statusFilter !== "aprovado") return false;
      const saleRow = salesRowById.get(row.id);
      const readCount = saleRow ? countSaleCheckins(saleRow) : row.checkinAt ? 1 : 0;
      const totalTickets = Math.max(1, saleRow?.quantidade ?? row.quantidade);
      if (salesIndicatorFilter === "ausente" && readCount >= totalTickets) return false;
      if (salesIndicatorFilter === "presente" && readCount <= 0) return false;
      if (salesIndicatorFilter === "manual" && !normalizeSearch(row.checkinMethod || "").includes("manual")) return false;
      if (!search) return true;
      return normalizeSearch(
        `${row.id} ${row.userName} ${row.userTurma} ${row.userCode} ${row.qrStatus} ${row.qrCode} ${row.transferInfo} ${row.transferFromUserName} ${row.transferToUserName} ${row.transferByUserName} ${row.itemName} ${row.loteNome} ${row.categoria} ${row.approvedBy} ${row.checkinBy} ${row.checkinNote} ${row.checkinEditedBy}`
      ).includes(search);
    });
  }, [salesIndicatorFilter, salesRowById, salesSearch, statementRows]);

  const totalPresencePages = useMemo(
    () => Math.max(1, Math.ceil(filteredPresenceRows.length / SALES_PAGE_SIZE)),
    [filteredPresenceRows.length]
  );

  const paginatedPresenceRows = useMemo(() => {
    const safePage = Math.min(totalPresencePages, Math.max(1, salesPage));
    const start = (safePage - 1) * SALES_PAGE_SIZE;
    return filteredPresenceRows.slice(start, start + SALES_PAGE_SIZE);
  }, [filteredPresenceRows, salesPage, totalPresencePages]);

  const filteredScanRows = useMemo(() => {
    const search = normalizeSearch(salesSearch);
    return salesRows.filter((row) => {
      if (normalizeStatementStatus(row.status) !== "aprovado") return false;
      if (!matchesPartyUserAlphaGroup(row.userName, scanGroup)) return false;
      if (!search) return true;
      return normalizeSearch(`${row.id} ${row.userName} ${row.userTurma} ${row.loteNome} ${row.status}`).includes(search);
    });
  }, [salesRows, salesSearch, scanGroup]);

  const totalScanPages = useMemo(
    () => Math.max(1, Math.ceil(filteredScanRows.length / SALES_PAGE_SIZE)),
    [filteredScanRows.length]
  );

  const paginatedScanRows = useMemo(() => {
    const safePage = Math.min(totalScanPages, Math.max(1, salesPage));
    const start = (safePage - 1) * SALES_PAGE_SIZE;
    return filteredScanRows.slice(start, start + SALES_PAGE_SIZE);
  }, [filteredScanRows, salesPage, totalScanPages]);

  useEffect(() => {
    setSalesPage(1);
  }, [
    salesAlertFilter,
    salesApproverFilter,
    salesFlowFilter,
    salesIndicatorFilter,
    salesSearch,
    salesSourceFilter,
    salesStatusFilter,
    salesTypeFilter,
    section,
    scanGroup,
  ]);

  useEffect(() => {
    setPartyUserPage(1);
  }, [partyUserAlphaFilter, partyUserSearch, partyUserTurmaFilter]);

  useEffect(() => {
    const totalPages =
      section === "extrato"
        ? totalStatementPages
        : section === "checkins"
          ? totalPresencePages
          : section === "scan"
            ? totalScanPages
            : totalSalesPages;
    setSalesPage((current) => Math.min(totalPages, Math.max(1, current)));
  }, [section, totalPresencePages, totalSalesPages, totalScanPages, totalStatementPages]);

  const checkinRows = useMemo(() => flattenTicketCheckins(salesRows), [salesRows]);

  const salesMetrics = useMemo(() => {
    const bruto = statementRows.reduce((sum, row) => sum + row.valorTotal, 0);
    const aprovado = statementRows
      .filter((row) => row.statusFilter === "aprovado")
      .reduce((sum, row) => sum + row.valorTotal, 0);
    const pendente = statementRows
      .filter((row) => row.statusFilter !== "aprovado")
      .reduce((sum, row) => sum + row.valorTotal, 0);
    const descontos = (evento?.dataExtra.cupons || []).reduce((sum, coupon) => {
      if (coupon.tipo === "valor") {
        return sum + parseCurrency(coupon.valor) * coupon.usos;
      }
      return sum;
    }, 0);

    return {
      bruto,
      aprovado,
      pendente,
      descontos,
      tickets: salesRows.reduce((sum, row) => sum + row.quantidade, 0),
    };
  }, [evento?.dataExtra.cupons, salesRows, statementRows]);

  const operatorPerformance = useMemo(() => {
    const byOperator = new Map<
      string,
      { id: string; name: string; turma: string; avatarUrl: string; total: number }
    >();
    checkinRows.forEach((row) => {
      const key = row.scannedByUserId || row.scannedByUserName || "operador";
      const current = byOperator.get(key) ?? {
        id: row.scannedByUserId,
        name: row.scannedByUserName || "Operador",
        turma: row.scannedByUserTurma,
        avatarUrl: row.scannedByUserAvatar,
        total: 0,
      };
      current.name = current.name || row.scannedByUserName || "Operador";
      current.turma = current.turma || row.scannedByUserTurma;
      current.avatarUrl = current.avatarUrl || row.scannedByUserAvatar;
      current.total += 1;
      byOperator.set(key, current);
    });
    return Array.from(byOperator.values()).sort((left, right) => right.total - left.total);
  }, [checkinRows]);

  const handleOpenNewLot = () => {
    setEditingLotId("new");
    setLotDraft(createEmptyLot(planCatalog));
  };

  const handleOpenEditLot = (lot: EventLot) => {
    setEditingLotId(lot.id);
    setLotDraft({
      ...lot,
      planPrices: buildLotePlanPrices(planCatalog, lot.planPrices),
    });
  };

  const handleSaveLot = async () => {
    if (!evento) return;
    if (!lotDraft.nome.trim() || !lotDraft.preco.trim()) {
      addToast("Título e preço do lote são obrigatórios.", "error");
      return;
    }
    const duplicateKey = normalizeLotDuplicateKey(lotDraft.nome);
    const duplicatedLot = evento.lotes.find(
      (lot) =>
        lot.id !== editingLotId &&
        normalizeLotDuplicateKey(lot.nome) === duplicateKey
    );
    if (duplicatedLot) {
      addToast("Esse lote já existe. Edite o lote existente em vez de criar outro igual.", "error");
      return;
    }
    const nextLot: EventLot = {
      ...lotDraft,
      nome: lotDraft.nome.trim().slice(0, EVENT_LOTE_NAME_MAX_LENGTH),
      preco: lotDraft.preco.trim(),
      descricao: lotDraft.descricao.trim(),
      quantidade: Math.max(0, Math.floor(lotDraft.quantidade)),
      ordem: Math.max(0, Math.floor(lotDraft.ordem)),
      qrPorIngresso: 1,
      planPrices: buildLotePlanPrices(planCatalog, lotDraft.planPrices),
    };
    const nextEvent = cloneEvent(evento);
    if (editingLotId === "new") {
      nextEvent.lotes = [...nextEvent.lotes, { ...nextLot, id: Date.now() }];
    } else {
      nextEvent.lotes = nextEvent.lotes.map((lot) => (lot.id === editingLotId ? nextLot : lot));
    }
    await persistEvent(nextEvent, editingLotId === "new" ? "Lote criado." : "Lote atualizado.");
    setEditingLotId(null);
    setLotDraft(createEmptyLot(planCatalog));
  };

  const handleDeleteLot = async (lotId: number) => {
    if (!evento || !window.confirm("Remover este lote?")) return;
    const nextEvent = cloneEvent(evento);
    nextEvent.lotes = nextEvent.lotes.filter((lot) => lot.id !== lotId);
    await persistEvent(nextEvent, "Lote removido.");
    if (editingLotId === lotId) {
      setEditingLotId(null);
      setLotDraft(createEmptyLot(planCatalog));
    }
  };

  const handleOpenNewCoupon = () => {
    setEditingCouponId("new");
    setCouponDraft(createEmptyCoupon());
  };

  const handleOpenEditCoupon = (coupon: EventCoupon) => {
    setEditingCouponId(coupon.id);
    setCouponDraft({ ...coupon });
  };

  const handleSaveCoupon = async () => {
    if (!evento) return;
    if (!couponDraft.titulo.trim() || !couponDraft.codigo.trim()) {
      addToast("Título e código do cupom são obrigatórios.", "error");
      return;
    }
    const nextCoupon: EventCoupon = {
      ...couponDraft,
      titulo: couponDraft.titulo.trim().slice(0, EVENT_COUPON_TITLE_MAX_LENGTH),
      codigo: couponDraft.codigo.trim().slice(0, EVENT_COUPON_CODE_MAX_LENGTH).toUpperCase(),
      valor: couponDraft.valor.trim(),
      valorMinimo: couponDraft.valorMinimo.trim(),
      valorMaximo: couponDraft.valorMaximo.trim(),
      quantidadeDisponivel: Math.max(0, Math.floor(couponDraft.quantidadeDisponivel)),
    };
    const nextEvent = cloneEvent(evento);
    if (editingCouponId === "new") {
      nextEvent.dataExtra.cupons = [...nextEvent.dataExtra.cupons, nextCoupon];
    } else {
      nextEvent.dataExtra.cupons = nextEvent.dataExtra.cupons.map((coupon) =>
        coupon.id === editingCouponId ? nextCoupon : coupon
      );
    }
    await persistEvent(nextEvent, editingCouponId === "new" ? "Cupom criado." : "Cupom atualizado.");
    setEditingCouponId(null);
    setCouponDraft(createEmptyCoupon());
  };

  const handleDeleteCoupon = async (couponId: string) => {
    if (!evento || !window.confirm("Excluir este cupom?")) return;
    const nextEvent = cloneEvent(evento);
    nextEvent.dataExtra.cupons = nextEvent.dataExtra.cupons.filter((coupon) => coupon.id !== couponId);
    await persistEvent(nextEvent, "Cupom removido.");
    if (editingCouponId === couponId) {
      setEditingCouponId(null);
      setCouponDraft(createEmptyCoupon());
    }
  };

  const handleOpenNewOperator = () => {
    setEditingOperatorId("new");
    setOperatorDraft(createEmptyCheckinOperator());
  };

  const handleOpenEditOperator = (operator: EventCheckinOperator) => {
    setEditingOperatorId(operator.id);
    setOperatorDraft({ ...operator });
  };

  const handleSaveOperator = async () => {
    if (!evento) return;
    if (!operatorDraft.nome.trim()) {
      addToast("Informe o nome do operador.", "error");
      return;
    }
    const nextOperator: EventCheckinOperator = {
      ...operatorDraft,
      nome: operatorDraft.nome.trim().slice(0, EVENT_OPERATOR_NAME_MAX_LENGTH),
      email: operatorDraft.email.trim().slice(0, EVENT_OPERATOR_EMAIL_MAX_LENGTH),
    };
    const nextEvent = cloneEvent(evento);
    if (editingOperatorId === "new") {
      nextEvent.dataExtra.checkinOperators = [...nextEvent.dataExtra.checkinOperators, nextOperator];
    } else {
      nextEvent.dataExtra.checkinOperators = nextEvent.dataExtra.checkinOperators.map((operator) =>
        operator.id === editingOperatorId ? nextOperator : operator
      );
    }
    await persistEvent(
      nextEvent,
      editingOperatorId === "new" ? "Operador de apoio adicionado." : "Operador atualizado."
    );
    setEditingOperatorId(null);
    setOperatorDraft(createEmptyCheckinOperator());
  };

  const handleDeleteOperator = async (operatorId: string) => {
    if (!evento || !window.confirm("Remover este operador da lista visual?")) return;
    const nextEvent = cloneEvent(evento);
    nextEvent.dataExtra.checkinOperators = nextEvent.dataExtra.checkinOperators.filter(
      (operator) => operator.id !== operatorId
    );
    await persistEvent(nextEvent, "Operador removido.");
    if (editingOperatorId === operatorId) {
      setEditingOperatorId(null);
      setOperatorDraft(createEmptyCheckinOperator());
    }
  };

  const handleSaveRecebedores = async (recipientUserIds: string[]) => {
    if (!evento) return;
    const nextEvent = cloneEvent(evento);
    nextEvent.recipientUserIds = recipientUserIds;
    setEvento(nextEvent);
    setEditDraft(nextEvent);
    await persistEvent(nextEvent, "Recebedores atualizados.");
  };

  const exportExtratoCsv = () => {
    if (!filteredStatementRows.length) return;
    const headers = [
      "ID",
      "Tipo",
      "Cliente",
      "Turma",
      "Pulseira/RA",
      "Item",
      "Lote",
      "Categoria",
      "Quantidade",
      "Data do pedido",
      "Hora do pedido",
      "Data da aprovação",
      "Hora da aprovação",
      "Aprovado por",
      "Método da aprovação",
      "Fonte do pagamento",
      "Data entrada/retirada",
      "Hora entrada/retirada",
      "Entrada/retirada por",
      "Status do QR",
      "Código do QR",
      "Transferência",
      "Método entrada/retirada",
      "Valor",
      "Desconto",
      "Origem do desconto",
      "Status",
      "Fonte",
      "Criado por",
    ];
    const rows = filteredStatementRows.map((row) => [
      row.id,
      row.kind,
      row.userName,
      row.userTurma,
      row.userCode,
      row.itemName,
      row.loteNome,
      row.categoria,
      String(row.quantidade),
      formatDateOnly(row.requestAt),
      formatTimeOnly(row.requestAt),
      formatDateOnly(row.approvalAt),
      formatTimeOnly(row.approvalAt),
      row.approvedBy || "-",
      row.approvalMethod,
      row.paymentSource || "-",
      formatDateOnly(row.checkinAt),
      formatTimeOnly(row.checkinAt),
      row.checkinBy || "-",
      row.qrStatus,
      row.qrCode,
      row.transferInfo,
      row.checkinMethod,
      formatCurrency(row.valorTotal),
      row.descontoValor,
      row.descontoFonte,
      row.status,
      row.source,
      row.createdBy,
    ]);
    const csvContent = [
      headers.map(toCsvCell).join(","),
      ...rows.map((line) => line.map(toCsvCell).join(",")),
    ].join("\n");
    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = `extrato_evento_${eventId}.csv`;
    anchor.click();
    URL.revokeObjectURL(url);
  };

  const exportPresenceCsv = () => {
    if (!filteredPresenceRows.length) return;
    const headers = [
      "Pedido",
      "Usuário",
      "Turma",
      "Ingresso",
      "Lote",
      "Quantidade",
      "Status do pagamento",
      "Solicitado em",
      "Data do pagamento",
      "Hora do pagamento",
      "Aprovado por",
      "Status do QR",
      "Código do QR",
      "Check-in em",
      "Check-in por",
      "Método do check-in",
      "Observação do check-in",
      "Check-in editado em",
      "Check-in editado por",
      "Transferido em",
      "Transferido de",
      "Transferido para",
      "Transferido por",
      "Resumo da transferência",
      "Valor",
    ];
    const rows = filteredPresenceRows.map((row) => [
      row.id,
      row.userName,
      row.userTurma,
      row.itemName,
      row.loteNome,
      String(row.quantidade),
      row.status,
      formatDateTime(row.requestAt),
      formatDateOnly(row.paymentAt),
      formatTimeOnly(row.paymentAt),
      row.approvedBy || "-",
      row.qrStatus,
      row.qrCode,
      formatDateTime(row.checkinAt),
      row.checkinBy || "-",
      row.checkinMethod,
      row.checkinNote,
      formatDateTime(row.checkinEditedAt),
      row.checkinEditedBy,
      formatDateTime(row.transferAt),
      row.transferFromUserName,
      row.transferToUserName,
      row.transferByUserName,
      row.transferInfo,
      formatCurrency(row.valorTotal),
    ]);
    const csvContent = [
      headers.map(toCsvCell).join(","),
      ...rows.map((line) => line.map(toCsvCell).join(",")),
    ].join("\n");
    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = `lista_presenca_evento_${eventId}.csv`;
    anchor.click();
    URL.revokeObjectURL(url);
  };

  const contextualOwnerRedirectHref = useMemo(() => {
    if (!evento || !paymentRecipientContext?.ownerType || paymentRecipientContext.ownerType === "tenant") return "";
    const ownerScope = resolveEventOwnerScope(evento);
    const ownerId = resolveEventOwnerId(evento);
    const expectedOwnerId = asString(paymentRecipientContext.ownerId).trim();
    const expectedOwnerScope =
      contextOwnerCategory === "diretorio"
        ? "directory"
        : contextOwnerCategory === "comissao"
          ? "commission"
          : paymentRecipientContext.ownerType;
    if (expectedOwnerScope !== paymentRecipientContext.ownerType) {
      return scopedPath(canonicalEventWorkspacePath(expectedOwnerScope, expectedOwnerId || ownerId, eventId, "edicao"));
    }
    const ownerMismatch =
      ownerScope !== paymentRecipientContext.ownerType ||
      (ownerId && expectedOwnerId && ownerId !== expectedOwnerId);
    if (!ownerMismatch) return "";
    return scopedPath(canonicalEventWorkspacePath(ownerScope, ownerId, eventId, "edicao"));
  }, [contextOwnerCategory, eventId, evento, paymentRecipientContext?.ownerId, paymentRecipientContext?.ownerType, scopedPath]);

  useEffect(() => {
    if (!loadingEvento && contextualOwnerRedirectHref) {
      router.replace(contextualOwnerRedirectHref);
    }
  }, [contextualOwnerRedirectHref, loadingEvento, router]);

  if (loadingEvento) {
    return (
      <main className="min-h-screen bg-[#050505] px-4 py-6 text-white">
        <div className="mx-auto flex max-w-6xl items-center justify-center rounded-3xl border border-zinc-800 bg-zinc-950 px-6 py-16">
          <Loader2 size={24} className="animate-spin text-brand" />
        </div>
      </main>
    );
  }

  if (!evento || !editDraft) {
    return (
      <main className="min-h-screen bg-[#050505] px-4 py-6 text-white">
        <div className="mx-auto max-w-5xl space-y-4">
          <Link
            href={adminEventosHref}
            className="inline-flex items-center gap-2 rounded-full border border-zinc-800 bg-zinc-950 px-4 py-2 text-xs font-black uppercase tracking-[0.18em] text-zinc-300"
          >
            <ArrowLeft size={14} />
            Voltar para eventos
          </Link>
          <div className="rounded-3xl border border-zinc-800 bg-zinc-950 p-8 text-center text-zinc-400">
            Evento não encontrado.
          </div>
        </div>
      </main>
    );
  }

  if (contextualOwnerRedirectHref) {
    return (
      <main className="min-h-screen bg-[#050505] px-4 py-6 text-white">
        <div className="mx-auto max-w-3xl rounded-[1.8rem] border border-amber-400/30 bg-amber-400/10 p-6 text-sm font-bold leading-6 text-amber-100">
          Este evento pertence a outro portal. Redirecionando para a página correta...
          <Link href={contextualOwnerRedirectHref} className="ml-2 text-white underline underline-offset-4">
            Abrir agora
          </Link>
        </div>
      </main>
    );
  }

  const ownerScope = resolveEventOwnerScope(evento);
  const isEntityManagedEvent = ownerScope !== "tenant";
  const isAdminWorkspace = !normalizedWorkspaceBasePath;
  const isRestrictedAdminSection = isAdminWorkspace && isEntityManagedEvent && section !== "edicao";
  const tenantAdminLabel = (tenantSigla || tenantName || "tenant").trim();
  const visibilityBlock = evento.adminVisibilityBlock;
  const isHiddenByAdmin = visibilityBlock?.hidden === true;

  if (isRestrictedAdminSection) {
    return (
      <main className="min-h-screen bg-[#050505] px-4 py-6 text-white sm:px-6">
        <div className="mx-auto flex min-h-[60vh] max-w-3xl flex-col justify-center space-y-4">
          <Link
            href={adminEventosHref}
            className="inline-flex w-fit items-center gap-2 rounded-full border border-zinc-800 bg-zinc-950 px-4 py-2 text-xs font-black uppercase tracking-[0.18em] text-zinc-300"
          >
            <ArrowLeft size={14} />
            Voltar para eventos
          </Link>
          <div className="rounded-[1.8rem] border border-amber-400/25 bg-amber-400/10 p-6">
            <p className="text-[10px] font-black uppercase tracking-[0.24em] text-amber-200">
              Acesso restrito
            </p>
            <h1 className="mt-3 text-2xl font-black text-white">{evento.titulo}</h1>
            <p className="mt-3 text-sm font-bold leading-6 text-amber-50">
              Este evento pertence a uma {ownerScopeLabel(ownerScope)}. Pelo painel admin, você pode acessar somente a edição básica do evento.
            </p>
            <Link
              href={eventSectionHref("edicao")}
              className="mt-5 inline-flex items-center justify-center gap-2 rounded-xl bg-white px-4 py-3 text-xs font-black uppercase text-black transition hover:bg-emerald-300"
            >
              <Edit3 size={14} />
              Abrir edição
            </Link>
          </div>
        </div>
      </main>
    );
  }

  const selectedRecipients = filterTenantPaymentRecipientsByIds(
    paymentRecipients,
    editDraft.recipientUserIds
  );
  const partyConfig = normalizeEventPartyConfig(editDraft.dataExtra.raw);
  const filteredPartyOrders = partyOrders.filter((order) => {
    const status = order.status.trim().toLowerCase();
    return partyOrdersStatus === "pendente"
      ? status === "pendente"
      : ["approved", "aprovado", "paid", "pago", "delivered"].includes(status);
  });
  const sortedPartyProducts = [...partyProducts].sort((left, right) => {
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
  const groupedPartyProducts = sortedPartyProducts.reduce<Array<{ section: string; products: EventPartyProduct[] }>>(
    (groups, product) => {
      const sectionName = getEventPartyProductSection(product);
      const existingGroup = groups.find((group) => group.section === sectionName);
      if (existingGroup) {
        existingGroup.products.push(product);
      } else {
        groups.push({ section: sectionName, products: [product] });
      }
      return groups;
    },
    []
  );
  const partySectionOptions = Array.from(
    new Set(
      [
        partyConfig.categoryName,
        ...partyProducts.map((product) => getEventPartyProductSection(product)),
        partyProductDraft.secao,
        partyProductDraft.categoria,
        partyNewSectionName,
        "Geral",
      ]
        .map((entry) => entry.trim())
        .filter(Boolean)
    )
  );
  const selectedManualPartyProduct = partyProducts.find(
    (product) => product.id === manualPartyOrderDraft.productId
  );
  const partyUserTurmaOptions = Array.from(
    new Set(partyUsers.map((entry) => entry.turma.trim()).filter(Boolean))
  ).sort((left, right) => left.localeCompare(right, "pt-BR", { sensitivity: "base" }));
  const filteredPartyUsers = partyUsers.filter((entry) => {
    const search = normalizeSearch(partyUserSearch);
    const matchesSearch =
      !search ||
      normalizeSearch(
        `${entry.nome} ${entry.email} ${entry.telefone} ${entry.cpf} ${entry.numero} ${entry.ra} ${entry.turma} ${entry.id}`
      ).includes(search);
    const matchesTurma = partyUserTurmaFilter === "todos" || entry.turma === partyUserTurmaFilter;
    const matchesAlpha = matchesPartyUserAlphaGroup(entry.nome, partyUserAlphaFilter);
    return matchesSearch && matchesTurma && matchesAlpha;
  });
  const totalPartyUserPages = Math.max(1, Math.ceil(filteredPartyUsers.length / PARTY_USER_PAGE_SIZE));
  const safePartyUserPage = Math.min(totalPartyUserPages, Math.max(1, partyUserPage));
  const paginatedPartyUsers = filteredPartyUsers.slice(
    (safePartyUserPage - 1) * PARTY_USER_PAGE_SIZE,
    safePartyUserPage * PARTY_USER_PAGE_SIZE
  );
  const manualRegisteredUsers = (() => {
    const fromSales = salesRows
      .map((row) => mapManualSaleRowToManualUser(row))
      .filter((entry): entry is EventPartyManualUser => Boolean(entry));
    const fromEventExtra = getManualPartyUsers(editDraft.dataExtra.raw);
    const seen = new Set<string>();

    return [...fromSales, ...fromEventExtra].filter((entry) => {
      const keys = [
        entry.orderId ? `order:${entry.orderId}` : "",
        entry.id ? `id:${entry.id}` : "",
        entry.numero ? `bracelet:${normalizeSearch(entry.numero)}` : "",
        entry.cpf ? `cpf:${entry.cpf}` : "",
        `loose:${normalizeSearch(`${entry.nome}|${entry.numero}`)}`,
      ].filter(Boolean);
      if (keys.some((key) => seen.has(key))) return false;
      keys.forEach((key) => seen.add(key));
      return true;
    });
  })();

  return (
    <main className="min-h-screen w-full overflow-x-hidden bg-[#050505] px-4 py-4 text-white sm:px-6 sm:py-6">
      <div className="mx-auto w-full max-w-6xl min-w-0 space-y-4">
        <header className="overflow-hidden rounded-[2rem] border border-zinc-800 bg-zinc-950/95 shadow-[0_24px_80px_rgba(0,0,0,0.35)]">
          <div className="flex flex-col gap-4 p-4 sm:p-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-3">
                <Link
                  href={adminEventosHref}
                  className="inline-flex h-11 w-11 items-center justify-center rounded-2xl border border-zinc-800 bg-black/40 text-zinc-300 transition hover:border-zinc-700 hover:text-white"
                >
                  <ArrowLeft size={18} />
                </Link>
                <div>
                  <p className="text-[10px] font-black uppercase tracking-[0.28em] text-zinc-500">
                    Gestão do Evento
                  </p>
                  <h1 className="text-2xl font-black tracking-tight text-white sm:text-3xl">
                    {evento.titulo}
                  </h1>
                </div>
              </div>
              <div className="flex flex-wrap gap-2">
                <Link
                  href={scanEventoHref}
                  className="inline-flex items-center gap-2 rounded-full border border-brand bg-brand-soft px-4 py-2 text-[11px] font-black uppercase tracking-[0.16em] text-brand-accent shadow-brand"
                >
                  <QrCode size={14} />
                  Scanner
                </Link>
                <Link
                  href={eventSectionHref("checkins")}
                  className="inline-flex items-center gap-2 rounded-full border border-zinc-800 bg-black/30 px-4 py-2 text-[11px] font-black uppercase tracking-[0.16em] text-zinc-200"
                >
                  <Users size={14} />
                  Lista de presença
                </Link>
              </div>
            </div>

            <div className="grid gap-4 md:grid-cols-[240px_1fr]">
              <div className="relative h-44 overflow-hidden rounded-[1.6rem] border border-zinc-800 bg-black sm:h-52">
                {evento.imagem ? (
                  <Image
                    src={evento.imagem}
                    alt={evento.titulo}
                    fill
                    sizes="(max-width: 768px) 100vw, 240px"
                    className="object-cover"
                    style={{ objectPosition: `50% ${evento.imagePositionY}%` }}
                  />
                ) : (
                  <div className="flex h-full items-center justify-center text-zinc-600">
                    <ImageIcon size={28} />
                  </div>
                )}
              </div>

              <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
                <div className="rounded-[1.4rem] border border-zinc-800 bg-black/30 p-4">
                  <p className="text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">Data</p>
                  <p className="mt-2 flex items-center gap-2 text-sm font-bold text-white">
                    <Calendar size={14} className="text-brand" />
                    {formatDate(evento.data)} às {evento.hora || "--:--"}
                  </p>
                </div>
                <div className="rounded-[1.4rem] border border-zinc-800 bg-black/30 p-4">
                  <p className="text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">Local</p>
                  <p className="mt-2 flex items-center gap-2 text-sm font-bold text-white">
                    <MapPin size={14} className="text-brand-accent" />
                    {evento.local || "Sem local"}
                  </p>
                </div>
                <div className="rounded-[1.4rem] border border-zinc-800 bg-black/30 p-4">
                  <p className="text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">Status de venda</p>
                  <span
                    className={`mt-2 inline-flex rounded-full border px-3 py-1 text-[11px] font-black uppercase ${saleStatusTone[evento.saleStatus]}`}
                  >
                    {evento.saleStatus === "em_breve"
                      ? "Em breve"
                      : evento.saleStatus === "esgotado"
                        ? "Esgotado"
                        : "Ativo"}
                  </span>
                </div>
                <div className="rounded-[1.4rem] border border-zinc-800 bg-black/30 p-4">
                  <p className="text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">Confirmações</p>
                  <p className="mt-2 text-2xl font-black text-white">{evento.stats.confirmados}</p>
                  <p className="text-[11px] text-zinc-500">{evento.stats.talvez} interessados</p>
                </div>
              </div>
            </div>
          </div>

          <div className="border-t border-zinc-800 px-4 py-3 sm:px-6">
            <div className="flex gap-2 overflow-x-auto pb-1">
              {(isAdminWorkspace && isEntityManagedEvent ? (["edicao"] as EventWorkspaceSection[]) : SECTION_ORDER).map((item) => (
                <SectionLink
                  key={item}
                  href={eventSectionHref(item)}
                  label={SECTION_LABELS[item]}
                  active={section === item || (item === "ficha" && isEventPartyAdminSection(section))}
                />
              ))}
            </div>
          </div>
        </header>

        {isHiddenByAdmin && !isAdminWorkspace ? (
          <div className="rounded-[1.4rem] border border-amber-400/30 bg-amber-400/10 p-4 text-sm font-bold leading-6 text-amber-50">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-start">
              <AlertTriangle size={18} className="mt-0.5 shrink-0 text-amber-200" />
              <div>
                <p className="text-[10px] font-black uppercase tracking-[0.22em] text-amber-200">
                  Evento invisível no app
                </p>
                <p className="mt-1">
                  O admin da {tenantAdminLabel} ocultou este evento. Motivo: {visibilityBlock?.reason || "sem motivo informado"}. Entre em contato com o admin da {tenantAdminLabel}.
                </p>
              </div>
            </div>
          </div>
        ) : null}

        {section === "extrato" ? (
          <section className="space-y-4">
            <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
              {[
                { label: "Faturamento total", value: formatCurrency(salesMetrics.bruto), tone: "text-emerald-300" },
                { label: "Descontos (cupons)", value: formatCurrency(salesMetrics.descontos), tone: "text-red-300" },
                { label: "Saldo liberado", value: formatCurrency(salesMetrics.aprovado), tone: "text-violet-300" },
                { label: "Saldo a liberar", value: formatCurrency(salesMetrics.pendente), tone: "text-sky-300" },
              ].map((card) => (
                <div key={card.label} className="rounded-[1.6rem] border border-zinc-800 bg-zinc-950 p-5">
                  <p className="text-[11px] text-zinc-500">{card.label}</p>
                  <p className={`mt-3 text-3xl font-black ${card.tone}`}>{card.value}</p>
                </div>
              ))}
            </div>

            <div className="rounded-[1.8rem] border border-zinc-800 bg-zinc-950 p-4 sm:p-5">
              <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
                <div>
                  <h2 className="text-lg font-black text-white">Extrato do evento</h2>
                  <p className="text-sm text-zinc-500">Filtre as vendas e exporte o movimento deste evento.</p>
                </div>
                <button
                  type="button"
                  onClick={exportExtratoCsv}
                  className="inline-flex items-center justify-center gap-2 rounded-xl border border-zinc-700 bg-black/30 px-4 py-3 text-xs font-black uppercase text-zinc-200"
                >
                  <Download size={14} />
                  Exportar CSV
                </button>
              </div>

            <div className="mt-4 grid min-w-0 gap-3 md:grid-cols-2 xl:grid-cols-[minmax(0,1.2fr)_minmax(0,0.55fr)_minmax(0,0.55fr)_minmax(0,0.65fr)_minmax(0,0.65fr)_minmax(0,0.65fr)_minmax(0,0.75fr)_auto]">
                <div className="flex items-center gap-2 rounded-2xl border border-zinc-800 bg-black/20 px-3 py-3">
                  <Search size={16} className="text-zinc-500" />
                  <input
                    value={salesSearch}
                    onChange={(event) => setSalesSearch(event.target.value)}
                    placeholder="Buscar por cliente, item, lote ou pedido..."
                    className="min-w-0 flex-1 bg-transparent text-sm outline-none placeholder:text-zinc-600"
                  />
                </div>
                <select
                  value={salesTypeFilter}
                  onChange={(event) => setSalesTypeFilter(event.target.value as EventStatementTypeFilter)}
                  className="admin-dark-select min-w-0 rounded-2xl border border-zinc-800 bg-zinc-950 px-4 py-3 text-sm text-zinc-100 outline-none"
                >
                  <option value="todos">Ingressos e produtos</option>
                  <option value="ingresso">Ingressos</option>
                  <option value="produto">Produtos</option>
                </select>
                <select
                  value={salesStatusFilter}
                  onChange={(event) => setSalesStatusFilter(event.target.value as EventStatementStatusFilter)}
                  className="admin-dark-select min-w-0 rounded-2xl border border-zinc-800 bg-zinc-950 px-4 py-3 text-sm text-zinc-100 outline-none"
                >
                  <option value="todos">Todos os status</option>
                  <option value="aprovado">Aprovados</option>
                  <option value="pendente">Pendentes</option>
                  <option value="analise">Em análise</option>
                </select>
                <select
                  value={salesSourceFilter}
                  onChange={(event) => setSalesSourceFilter(event.target.value)}
                  className="admin-dark-select min-w-0 rounded-2xl border border-zinc-800 bg-zinc-950 px-4 py-3 text-sm text-zinc-100 outline-none"
                >
                  <option value="todos">Todas as origens</option>
                  {salesSourceFilter !== "todos" && !statementSourceOptions.includes(salesSourceFilter) ? (
                    <option value={salesSourceFilter}>{salesSourceFilter}</option>
                  ) : null}
                  {statementSourceOptions.map((source) => (
                    <option key={source} value={source}>
                      {source}
                    </option>
                  ))}
                </select>
                <select
                  value={salesApproverFilter}
                  onChange={(event) => setSalesApproverFilter(event.target.value)}
                  className="admin-dark-select min-w-0 rounded-2xl border border-zinc-800 bg-zinc-950 px-4 py-3 text-sm text-zinc-100 outline-none"
                >
                  <option value="todos">Todos os aprovadores</option>
                  {salesApproverFilter !== "todos" && !statementApproverOptions.includes(salesApproverFilter) ? (
                    <option value={salesApproverFilter}>{salesApproverFilter}</option>
                  ) : null}
                  {statementApproverOptions.map((approver) => (
                    <option key={approver} value={approver}>
                      {approver}
                    </option>
                  ))}
                </select>
                <select
                  value={salesFlowFilter}
                  onChange={(event) => setSalesFlowFilter(event.target.value as EventStatementFlowFilter)}
                  className="admin-dark-select min-w-0 rounded-2xl border border-zinc-800 bg-zinc-950 px-4 py-3 text-sm text-zinc-100 outline-none"
                >
                  {Object.entries(STATEMENT_FLOW_LABELS).map(([flow, label]) => (
                    <option key={flow} value={flow}>
                      {label}
                    </option>
                  ))}
                </select>
                <select
                  value={salesIndicatorFilter}
                  onChange={(event) => setSalesIndicatorFilter(event.target.value)}
                  className="admin-dark-select min-w-0 rounded-2xl border border-zinc-800 bg-zinc-950 px-4 py-3 text-sm text-zinc-100 outline-none"
                >
                  <option value="">Todos os indicadores</option>
                  {Object.entries(STATEMENT_INDICATOR_LABELS).map(([indicator, label]) => (
                    <option key={indicator} value={indicator}>
                      {label}
                    </option>
                  ))}
                </select>
                <button
                  type="button"
                  onClick={() => {
                    void loadSales(true);
                    void loadPartyProducts();
                    void loadPartyOrders();
                  }}
                  className="brand-button-soft"
                >
                  Atualizar
                </button>
              </div>
              {salesAlertFilter ? (
                <div className="mt-3 flex flex-wrap items-center gap-2 rounded-2xl border border-amber-400/25 bg-amber-400/10 px-3 py-2 text-xs font-bold text-amber-100">
                  <AlertTriangle size={14} />
                  <span>Filtro de alerta ativo: {statementAlertLabel(salesAlertFilter)}</span>
                  <button
                    type="button"
                    onClick={() => setSalesAlertFilter("")}
                    className="rounded-lg border border-amber-300/30 px-2 py-1 text-[10px] font-black uppercase text-amber-100 transition hover:bg-amber-300 hover:text-black"
                  >
                    Limpar
                  </button>
                </div>
              ) : null}
              {salesSourceFilter !== "todos" || salesApproverFilter !== "todos" || salesFlowFilter !== "todos" || salesIndicatorFilter ? (
                <div className="mt-3 flex flex-wrap items-center gap-2 rounded-2xl border border-sky-400/20 bg-sky-400/10 px-3 py-2 text-xs font-bold text-sky-100">
                  {salesSourceFilter !== "todos" ? <span>Origem: {salesSourceFilter}</span> : null}
                  {salesApproverFilter !== "todos" ? <span>Aprovador: {salesApproverFilter}</span> : null}
                  {salesFlowFilter !== "todos" ? <span>Fluxo: {STATEMENT_FLOW_LABELS[salesFlowFilter]}</span> : null}
                  {salesIndicatorFilter ? <span>Indicador: {statementIndicatorLabel(salesIndicatorFilter)}</span> : null}
                  <button
                    type="button"
                    onClick={() => {
                      setSalesSourceFilter("todos");
                      setSalesApproverFilter("todos");
                      setSalesFlowFilter("todos");
                      setSalesIndicatorFilter("");
                    }}
                    className="rounded-lg border border-sky-300/30 px-2 py-1 text-[10px] font-black uppercase text-sky-100 transition hover:bg-sky-300 hover:text-black"
                  >
                    Limpar
                  </button>
                </div>
              ) : null}

              <div className="mt-4 max-w-full overflow-hidden rounded-[1.4rem] border border-zinc-800">
                <div className="max-w-full overflow-x-auto">
                  <table className="w-full min-w-[2720px] text-left text-sm">
                    <thead className="bg-black/40 text-[11px] uppercase tracking-[0.16em] text-zinc-500">
                      <tr>
                        <th className="px-4 py-3">ID</th>
                        <th className="px-4 py-3">Tipo</th>
                        <th className="px-4 py-3">Cliente</th>
                        <th className="px-4 py-3">Pulseira/RA</th>
                        <th className="px-4 py-3">Item</th>
                        <th className="px-4 py-3">Lote</th>
                        <th className="px-4 py-3">Categoria</th>
                        <th className="px-4 py-3">Qtd</th>
                        <th className="px-4 py-3">Data pedido</th>
                        <th className="px-4 py-3">Hora pedido</th>
                        <th className="px-4 py-3">Data aprovação</th>
                        <th className="px-4 py-3">Hora aprovação</th>
                        <th className="px-4 py-3">Aprovado por</th>
                        <th className="px-4 py-3">Método aprovação</th>
                        <th className="px-4 py-3">Fonte pagamento</th>
                        <th className="px-4 py-3">Data entrada/retirada</th>
                        <th className="px-4 py-3">Hora entrada/retirada</th>
                        <th className="px-4 py-3">Entrada/retirada por</th>
                        <th className="px-4 py-3">Status QR</th>
                        <th className="px-4 py-3">Código QR</th>
                        <th className="px-4 py-3">Transferência</th>
                        <th className="px-4 py-3">Método entrada/retirada</th>
                        <th className="px-4 py-3">Valor</th>
                        <th className="px-4 py-3">Desconto</th>
                        <th className="px-4 py-3">Origem desconto</th>
                        <th className="px-4 py-3">Fonte</th>
                        <th className="px-4 py-3">Status</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-zinc-800 bg-zinc-950/70">
                      {loadingSales || loadingPartyOrders ? (
                        <tr>
                          <td colSpan={27} className="px-4 py-10 text-center">
                            <Loader2 size={20} className="mx-auto animate-spin text-brand" />
                          </td>
                        </tr>
                      ) : filteredStatementRows.length === 0 ? (
                        <tr>
                          <td colSpan={27} className="px-4 py-10 text-center text-zinc-500">
                            Nenhuma transação encontrada.
                          </td>
                        </tr>
                      ) : (
                        paginatedStatementRows.map((row) => (
                          <tr key={row.id} className="hover:bg-white/[0.03]">
                            <td className="px-4 py-3 font-mono text-xs text-zinc-400">{row.id.slice(0, 8)}</td>
                            <td className="px-4 py-3">
                              <span className={`rounded-full border px-2.5 py-1 text-[10px] font-black uppercase ${
                                row.kind === "produto"
                                  ? "border-sky-500/30 bg-sky-500/10 text-sky-200"
                                  : "border-brand bg-brand-soft text-brand-accent"
                              }`}>
                                {row.kind}
                              </span>
                            </td>
                            <td className="px-4 py-3">
                              <p className="font-bold text-white">{row.userName}</p>
                              <p className="text-xs text-zinc-500">{row.userTurma || "-"}</p>
                            </td>
                            <td className="px-4 py-3 font-mono text-xs text-zinc-400">{row.userCode || "-"}</td>
                            <td className="px-4 py-3 font-bold text-zinc-200">{row.itemName}</td>
                            <td className="px-4 py-3 text-zinc-400">{row.loteNome}</td>
                            <td className="px-4 py-3 text-zinc-400">{row.categoria}</td>
                            <td className="px-4 py-3 text-zinc-300">{row.quantidade}</td>
                            <td className="px-4 py-3 text-zinc-400">{formatDateOnly(row.requestAt)}</td>
                            <td className="px-4 py-3 text-zinc-400">{formatTimeOnly(row.requestAt)}</td>
                            <td className="px-4 py-3 text-zinc-400">{formatDateOnly(row.approvalAt)}</td>
                            <td className="px-4 py-3 text-zinc-400">{formatTimeOnly(row.approvalAt)}</td>
                            <td className="px-4 py-3 text-zinc-400">{row.approvedBy || "-"}</td>
                            <td className="px-4 py-3 text-zinc-400">{row.approvalMethod}</td>
                            <td className="px-4 py-3 text-zinc-400">{row.paymentSource || "-"}</td>
                            <td className="px-4 py-3 text-zinc-400">{formatDateOnly(row.checkinAt)}</td>
                            <td className="px-4 py-3 text-zinc-400">{formatTimeOnly(row.checkinAt)}</td>
                            <td className="px-4 py-3 text-zinc-400">{row.checkinBy || "-"}</td>
                            <td className="px-4 py-3 text-zinc-400">{row.qrStatus || "-"}</td>
                            <td className="max-w-[220px] truncate px-4 py-3 font-mono text-xs text-zinc-400" title={row.qrCode}>
                              {row.qrCode || "-"}
                            </td>
                            <td className="max-w-[220px] truncate px-4 py-3 text-zinc-400" title={row.transferInfo}>
                              {row.transferInfo || "-"}
                            </td>
                            <td className="px-4 py-3 text-zinc-400">{row.checkinMethod}</td>
                            <td className="px-4 py-3 font-semibold text-emerald-300">
                              {formatCurrency(row.valorTotal)}
                            </td>
                            <td className="px-4 py-3 text-zinc-500">{row.descontoValor}</td>
                            <td className="px-4 py-3 text-zinc-400">{row.descontoFonte}</td>
                            <td className="px-4 py-3 text-zinc-400">{row.source}</td>
                            <td className="px-4 py-3">
                              <span className="rounded-full border border-zinc-700 px-2.5 py-1 text-[10px] font-black uppercase text-zinc-300">
                                {row.status}
                              </span>
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
              <div className="mt-4">
                <PaginationControls
                  page={salesPage}
                  totalPages={totalStatementPages}
                  totalItems={totalStatementItems}
                  pageSize={SALES_PAGE_SIZE}
                  onPageChange={setSalesPage}
                />
              </div>
            </div>
          </section>
        ) : null}

        {section === "bi" ? (
          <section className="space-y-4">
            <div className="rounded-[1.8rem] border border-zinc-800 bg-zinc-950 p-4 sm:p-5">
              <p className="text-[10px] font-black uppercase tracking-[0.24em] text-brand-accent">BI do evento</p>
              <h2 className="mt-2 text-2xl font-black uppercase text-white">{evento.titulo}</h2>
              <p className="mt-1 text-sm font-semibold text-zinc-500">
                Escolha uma visão para abrir a análise filtrada deste evento.
              </p>
            </div>

            <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-5">
              {[
                {
                  title: "BI Comercial",
                  description: "Venda, receita, lote, preço, turma e funil.",
                  href: `${eventBiBaseHref}/comercial?evento=${encodeURIComponent(evento.id)}`,
                  icon: <Wallet size={20} />,
                },
                {
                  title: "BI Operacional",
                  description: "Aprovação, comprovante, fila, gargalo e aprovadores.",
                  href: `${eventBiBaseHref}/operacional?evento=${encodeURIComponent(evento.id)}`,
                  icon: <Check size={20} />,
                },
                {
                  title: "BI Portaria",
                  description: "Entrada, presença, ausência e leitura de QR code.",
                  href: `${eventBiBaseHref}/portaria?evento=${encodeURIComponent(evento.id)}`,
                  icon: <QrCode size={20} />,
                },
                {
                  title: "BI Estratégico",
                  description: "Recorrência, previsão, comportamento e decisão.",
                  href: `${eventBiBaseHref}/estrategico?evento=${encodeURIComponent(evento.id)}`,
                  icon: <Users size={20} />,
                },
                {
                  title: "BI Modo Vendas",
                  description: "Produto, ficha, bar, retirada, baixa e auditoria.",
                  href: `${eventBiBaseHref}/vendas?evento=${encodeURIComponent(evento.id)}`,
                  icon: <Package size={20} />,
                },
              ].map((item) => (
                <Link
                  key={item.title}
                  href={item.href}
                  className="group flex min-h-44 flex-col justify-between rounded-[1.4rem] border border-zinc-800 bg-zinc-950 p-5 transition hover:border-brand/60 hover:bg-zinc-900"
                >
                  <span className="inline-flex h-11 w-11 items-center justify-center rounded-xl border border-brand/20 bg-brand-soft text-brand-accent">
                    {item.icon}
                  </span>
                  <span>
                    <strong className="block text-lg font-black uppercase text-white">{item.title}</strong>
                    <span className="mt-2 block text-sm font-semibold leading-5 text-zinc-500">{item.description}</span>
                  </span>
                </Link>
              ))}
            </div>
          </section>
        ) : null}

        {section === "lotes" ? (
          <section className="grid gap-4 xl:grid-cols-[1.15fr_0.85fr]">
            <div className="rounded-[1.8rem] border border-zinc-800 bg-zinc-950 p-4 sm:p-5">
              <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <h2 className="text-lg font-black text-white">Lotes do evento</h2>
                  <p className="text-sm text-zinc-500">Organize visibilidade, preços, validade e regras por lote.</p>
                </div>
                <button
                  type="button"
                  onClick={handleOpenNewLot}
                  className="brand-button-soft"
                >
                  <Layers3 size={14} />
                  Criar novo lote
                </button>
              </div>

              <div className="mt-4 space-y-3">
                {sortedLots.length === 0 ? (
                  <div className="rounded-[1.4rem] border border-dashed border-zinc-800 bg-black/20 p-6 text-sm text-zinc-500">
                    Nenhum lote cadastrado para este evento.
                  </div>
                ) : (
                  sortedLots.map((lot) => (
                    <div
                      key={lot.id}
                      draggable
                      onDragStart={() => setDraggingLotId(lot.id)}
                      onDragOver={(event) => event.preventDefault()}
                      onDrop={() => void handleReorderLot(lot.id)}
                      onDragEnd={() => setDraggingLotId(null)}
                      className={`rounded-[1.4rem] border bg-black/25 p-4 transition ${
                        draggingLotId === lot.id ? "border-brand/60 opacity-70" : "border-zinc-800"
                      }`}
                    >
                      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                        <div className="flex gap-3">
                          <button
                            type="button"
                            className="mt-0.5 inline-flex h-9 w-9 shrink-0 cursor-grab items-center justify-center rounded-xl border border-zinc-800 bg-zinc-950 text-zinc-500 active:cursor-grabbing"
                            title="Arraste para mudar a ordem"
                            aria-label="Arraste para mudar a ordem"
                          >
                            <MoveVertical size={16} />
                          </button>
                          <div>
                            <div className="flex flex-wrap items-center gap-2">
                              <h3 className="text-base font-black text-white">{lot.nome}</h3>
                              <span className={`rounded-full border px-2.5 py-1 text-[10px] font-black uppercase ${saleStatusTone[lot.status]}`}>
                                {lot.status === "em_breve" ? "Em breve" : lot.status === "esgotado" ? "Esgotado" : "Ativo"}
                              </span>
                              {lot.invisivel ? (
                                <span className="rounded-full border border-amber-500/25 bg-amber-500/10 px-2.5 py-1 text-[10px] font-black uppercase text-amber-200">
                                  Invisível
                                </span>
                              ) : null}
                            </div>
                            <p className="mt-2 text-sm text-zinc-400">{lot.descricao || "Sem descrição adicional."}</p>
                          </div>
                        </div>
                        <div className="flex gap-2">
                          <button
                            type="button"
                            onClick={() => handleOpenEditLot(lot)}
                            className="inline-flex items-center gap-2 rounded-xl border border-zinc-700 bg-black/40 px-3 py-2 text-xs font-black uppercase text-zinc-200"
                          >
                            <Edit3 size={13} />
                            Editar
                          </button>
                          <button
                            type="button"
                            onClick={() => void handleDeleteLot(lot.id)}
                            className="inline-flex items-center gap-2 rounded-xl border border-red-500/20 bg-red-500/10 px-3 py-2 text-xs font-black uppercase text-red-200"
                          >
                            <Trash2 size={13} />
                            Excluir
                          </button>
                        </div>
                      </div>

                      <div className="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
                        <div className="rounded-2xl border border-zinc-800 bg-zinc-950/60 p-3">
                          <p className="text-[10px] uppercase tracking-[0.16em] text-zinc-500">Preço</p>
                          <p className="mt-2 text-lg font-black text-emerald-300">R$ {lot.preco || "0,00"}</p>
                        </div>
                        <div className="rounded-2xl border border-zinc-800 bg-zinc-950/60 p-3">
                          <p className="text-[10px] uppercase tracking-[0.16em] text-zinc-500">Quantidade</p>
                          <p className="mt-2 text-lg font-black text-white">{lot.quantidade}</p>
                        </div>
                        <div className="rounded-2xl border border-zinc-800 bg-zinc-950/60 p-3">
                          <p className="text-[10px] uppercase tracking-[0.16em] text-zinc-500">Ordem na lista</p>
                          <p className="mt-2 text-lg font-black text-white">{lot.ordem}</p>
                          <p className="mt-1 text-[11px] text-zinc-500">Arraste o lote para reorganizar a exibição.</p>
                        </div>
                        <div className="rounded-2xl border border-zinc-800 bg-zinc-950/60 p-3">
                          <p className="text-[10px] uppercase tracking-[0.16em] text-zinc-500">Período</p>
                          <p className="mt-2 text-sm font-bold text-white">
                            {formatLotValidityPeriod(lot)}
                          </p>
                        </div>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>

            <div className="rounded-[1.8rem] border border-zinc-800 bg-zinc-950 p-4 sm:p-5">
              <div>
                <p className="text-[10px] font-black uppercase tracking-[0.24em] text-zinc-500">
                  {editingLotId === "new" ? "Novo lote" : editingLotId ? "Editar lote" : "Criação de lotes"}
                </p>
                <h2 className="mt-2 text-xl font-black text-white">Crie ou atualize um lote</h2>
                <p className="mt-1 text-sm text-zinc-500">
                  Configure os mesmos campos operacionais usados hoje e já deixe o lote pronto para vendas futuras.
                </p>
              </div>

              <div className="mt-5 space-y-4">
                <div className="rounded-[1.4rem] border border-zinc-800 bg-black/20 p-4">
                  <p className="text-[10px] font-black uppercase tracking-[0.22em] text-zinc-500">Informações básicas</p>
                  <div className="mt-4 grid gap-3">
                    <div className="space-y-2">
                      <p className="text-[11px] font-black uppercase tracking-[0.16em] text-zinc-500">Nome do lote</p>
                      <LotNameSelector
                        value={lotDraft.nome}
                        maxLength={EVENT_LOTE_NAME_MAX_LENGTH}
                        onChange={(value) => setLotDraft((previous) => ({ ...previous, nome: value }))}
                      />
                    </div>
                    <div className="space-y-2">
                      <p className="text-[11px] font-black uppercase tracking-[0.16em] text-zinc-500">Descrição do lote</p>
                      <textarea
                        value={lotDraft.descricao}
                        onChange={(event) =>
                          setLotDraft((previous) => ({ ...previous, descricao: event.target.value }))
                        }
                        placeholder="Descreva os benefícios ou detalhes deste lote..."
                        className="min-h-24 rounded-2xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm outline-none placeholder:text-zinc-600"
                      />
                    </div>
                  </div>
                </div>

                <div className="rounded-[1.4rem] border border-zinc-800 bg-black/20 p-4">
                  <p className="text-[10px] font-black uppercase tracking-[0.22em] text-zinc-500">Preço, quantidade e ordem</p>
                  <div className="mt-4 grid gap-3 sm:grid-cols-3">
                    <label className="space-y-2">
                      <span className="block text-[11px] font-black uppercase tracking-[0.16em] text-zinc-500">Preço do lote</span>
                      <input
                        value={lotDraft.preco}
                        onChange={(event) => setLotDraft((previous) => ({ ...previous, preco: event.target.value }))}
                        placeholder="Preço (R$)"
                        inputMode="decimal"
                        className="w-full rounded-2xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm outline-none placeholder:text-zinc-600"
                      />
                    </label>
                    <label className="space-y-2">
                      <span className="block text-[11px] font-black uppercase tracking-[0.16em] text-zinc-500">Quantidade disponível</span>
                      <input
                        value={lotDraft.quantidade}
                        onChange={(event) =>
                          setLotDraft((previous) => ({
                            ...previous,
                            quantidade: Math.max(0, Number(event.target.value) || 0),
                          }))
                        }
                        placeholder="Quantidade"
                        inputMode="numeric"
                        className="w-full rounded-2xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm outline-none placeholder:text-zinc-600"
                      />
                    </label>
                    <label className="space-y-2">
                      <span className="block text-[11px] font-black uppercase tracking-[0.16em] text-zinc-500">Ordem de exibição</span>
                      <input
                        value={lotDraft.ordem}
                        onChange={(event) =>
                          setLotDraft((previous) => ({
                            ...previous,
                            ordem: Math.max(0, Number(event.target.value) || 0),
                          }))
                        }
                        placeholder="0 aparece primeiro"
                        inputMode="numeric"
                        className="w-full rounded-2xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm outline-none placeholder:text-zinc-600"
                      />
                    </label>
                    <p className="text-[11px] leading-5 text-zinc-500 sm:col-span-3">
                      Cada ingresso gera um QR code. Para reorganizar a ordem pública, arraste os lotes na lista à esquerda ou ajuste a ordem manualmente.
                    </p>
                  </div>
                </div>

                <div className="rounded-[1.4rem] border border-zinc-800 bg-black/20 p-4">
                  <p className="text-[10px] font-black uppercase tracking-[0.22em] text-zinc-500">Opções do lote</p>
                  <div className="mt-4 grid gap-3">
                    <label className="flex items-start gap-3 rounded-2xl border border-zinc-800 bg-zinc-950/60 p-4">
                      <input
                        type="checkbox"
                        checked={lotDraft.invisivel}
                        onChange={(event) =>
                          setLotDraft((previous) => ({ ...previous, invisivel: event.target.checked }))
                        }
                        className="mt-1 h-4 w-4 accent-emerald-500"
                      />
                      <div>
                        <p className="font-bold text-white">Lote invisível</p>
                        <p className="text-sm text-zinc-500">
                          Fica oculto da venda pública e pode ser reservado para operação interna.
                        </p>
                      </div>
                    </label>
                    <label className="flex items-start gap-3 rounded-2xl border border-zinc-800 bg-zinc-950/60 p-4">
                      <input
                        type="checkbox"
                        checked={lotDraft.transferivel}
                        onChange={(event) =>
                          setLotDraft((previous) => ({ ...previous, transferivel: event.target.checked }))
                        }
                        className="mt-1 h-4 w-4 accent-emerald-500"
                      />
                      <div>
                        <p className="font-bold text-white">Lote transferível</p>
                        <p className="text-sm text-zinc-500">
                          Permite a circulação do ingresso entre usuários.
                        </p>
                      </div>
                    </label>
                    <div className="rounded-2xl border border-zinc-800 bg-zinc-950/60 p-4">
                      <label className="flex items-start gap-3">
                        <input
                          type="checkbox"
                          checked={lotDraft.validadeAtiva}
                          onChange={(event) =>
                            setLotDraft((previous) => ({ ...previous, validadeAtiva: event.target.checked }))
                          }
                          className="mt-1 h-4 w-4 accent-emerald-500"
                        />
                        <div>
                          <p className="font-bold text-white">Período de validade</p>
                          <p className="text-sm text-zinc-500">Defina início e fim das vendas deste lote.</p>
                        </div>
                      </label>
                      {lotDraft.validadeAtiva ? (
                        <div className="mt-4 grid gap-3 sm:grid-cols-2">
                          <div className="space-y-2">
                            <p className="text-[11px] font-black uppercase tracking-[0.16em] text-zinc-500">Início das vendas</p>
                            <input
                              type="date"
                              value={lotDraft.inicioVendasData}
                              onChange={(event) =>
                                setLotDraft((previous) => ({ ...previous, inicioVendasData: event.target.value }))
                              }
                              className="w-full rounded-2xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm outline-none"
                            />
                            <input
                              type="time"
                              value={lotDraft.inicioVendasHora}
                              onChange={(event) =>
                                setLotDraft((previous) => ({ ...previous, inicioVendasHora: event.target.value }))
                              }
                              className="w-full rounded-2xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm outline-none"
                            />
                          </div>
                          <div className="space-y-2">
                            <p className="text-[11px] font-black uppercase tracking-[0.16em] text-zinc-500">Fim das vendas</p>
                            <input
                              type="date"
                              value={lotDraft.fimVendasData}
                              onChange={(event) =>
                                setLotDraft((previous) => ({ ...previous, fimVendasData: event.target.value }))
                              }
                              className="w-full rounded-2xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm outline-none"
                            />
                            <input
                              type="time"
                              value={lotDraft.fimVendasHora}
                              onChange={(event) =>
                                setLotDraft((previous) => ({ ...previous, fimVendasHora: event.target.value }))
                              }
                              className="w-full rounded-2xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm outline-none"
                            />
                          </div>
                        </div>
                      ) : null}
                    </div>
                  </div>
                </div>

                <div className="rounded-[1.4rem] border border-zinc-800 bg-black/20 p-4">
                  <p className="text-[10px] font-black uppercase tracking-[0.22em] text-zinc-500">Preço por plano</p>
                  <div className="mt-4 space-y-3">
                    {lotDraft.planPrices.map((entry) => (
                      <div
                        key={entry.planId}
                        className="grid gap-3 rounded-2xl border border-zinc-800 bg-zinc-950/60 p-3 sm:grid-cols-[1fr_160px]"
                      >
                        <div>
                          <p className="font-bold text-white">{entry.planName}</p>
                          <p className="text-xs text-zinc-500">Em branco usa o valor geral do lote.</p>
                        </div>
                        <input
                          value={entry.price}
                          onChange={(event) =>
                            setLotDraft((previous) => ({
                              ...previous,
                              planPrices: previous.planPrices.map((planEntry) =>
                                planEntry.planId === entry.planId
                                  ? { ...planEntry, price: event.target.value }
                                  : planEntry
                              ),
                            }))
                          }
                          placeholder="Preço especial"
                          className="rounded-2xl border border-zinc-700 bg-black/30 px-4 py-3 text-sm outline-none placeholder:text-zinc-600"
                        />
                      </div>
                    ))}
                  </div>
                </div>

                <div className="rounded-[1.4rem] border border-zinc-800 bg-black/20 p-4">
                  <p className="text-[10px] font-black uppercase tracking-[0.22em] text-zinc-500">Status do lote</p>
                  <div className="mt-3 grid grid-cols-3 gap-2">
                    {(["ativo", "em_breve", "esgotado"] as EventSaleStatus[]).map((status) => (
                      <button
                        key={status}
                        type="button"
                        onClick={() => setLotDraft((previous) => ({ ...previous, status }))}
                        className={`rounded-xl border px-3 py-3 text-[11px] font-black uppercase ${
                          lotDraft.status === status
                            ? saleStatusTone[status]
                            : "border-zinc-700 bg-zinc-950 text-zinc-400"
                        }`}
                      >
                        {status === "ativo" ? "Ativar" : status === "em_breve" ? "Em breve" : "Esgotado"}
                      </button>
                    ))}
                  </div>
                </div>
              </div>

              <div className="mt-5 flex flex-col gap-3 border-t border-zinc-800 pt-4 sm:flex-row sm:justify-end">
                {editingLotId ? (
                  <button
                    type="button"
                    onClick={() => {
                      setEditingLotId(null);
                      setLotDraft(createEmptyLot(planCatalog));
                    }}
                    className="rounded-2xl border border-zinc-700 bg-black/20 px-4 py-3 text-xs font-black uppercase text-zinc-300"
                  >
                    Cancelar
                  </button>
                ) : null}
                <button
                  ref={saveLotButtonRef}
                  type="button"
                  onClick={() => void handleSaveLot()}
                  disabled={savingEvento}
                  className="brand-button-soft"
                >
                  {editingLotId === "new" ? "Criar lote" : editingLotId ? "Salvar lote" : "Salvar rascunho"}
                </button>
              </div>
              <FloatingSaveButton
                watchRef={saveLotButtonRef}
                label={editingLotId === "new" ? "Criar lote" : editingLotId ? "Salvar lote" : "Salvar rascunho"}
                icon={<Save size={14} />}
                disabled={savingEvento}
                onClick={() => void handleSaveLot()}
              />
            </div>
          </section>
        ) : null}

        {section === "scan" ? (
          <section className="space-y-4">
            <div className="rounded-[1.8rem] border border-zinc-800 bg-zinc-950 p-4 sm:p-5">
              <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
                <div>
                  <p className="text-[10px] font-black uppercase tracking-[0.28em] text-brand-accent">Scan do evento</p>
                  <h2 className="mt-2 text-xl font-black text-white">{evento.titulo}</h2>
                  <p className="mt-1 text-sm text-zinc-500">
                    Valide ingressos aprovados dentro do contexto deste evento.
                  </p>
                </div>
                <div className="flex flex-wrap gap-2">
                  <Link
                    href={eventSectionHref("checkins")}
                    className="inline-flex items-center justify-center gap-2 rounded-xl border border-zinc-700 bg-black/20 px-4 py-3 text-xs font-black uppercase text-zinc-200"
                  >
                    <Users size={14} />
                    Lista de presença
                  </Link>
                  <button
                    type="button"
                    onClick={() => void loadSales(true)}
                    className="brand-button-soft"
                  >
                    <RotateCcw size={14} />
                    Atualizar
                  </button>
                  <button
                    type="button"
                    onClick={() => void startTicketScanner()}
                    disabled={ticketScannerActive || ticketScannerStarting}
                    className="inline-flex items-center justify-center gap-2 rounded-xl border border-brand bg-brand-soft px-4 py-3 text-xs font-black uppercase text-brand-accent disabled:opacity-50"
                  >
                    {ticketScannerStarting ? <Loader2 size={14} className="animate-spin" /> : <Camera size={14} />}
                    Abrir câmera
                  </button>
                  <button
                    type="button"
                    onClick={() => void stopTicketScanner()}
                    disabled={!ticketScannerActive}
                    className="inline-flex items-center justify-center gap-2 rounded-xl border border-zinc-700 bg-black/20 px-4 py-3 text-xs font-black uppercase text-zinc-200 disabled:opacity-40"
                  >
                    <ScanLine size={14} />
                    Parar
                  </button>
                </div>
              </div>

              <div className="mt-4 grid gap-4 lg:grid-cols-[360px_1fr]">
                <div
                  id="event-ticket-workspace-scanner"
                  className="qr-reader-surface min-h-[320px] overflow-hidden rounded-[1.4rem] border border-dashed border-zinc-700 bg-black/40"
                />
                <div className="rounded-[1.4rem] border border-zinc-800 bg-black/20 p-4">
                  <p className="text-[10px] font-black uppercase tracking-[0.22em] text-zinc-500">Resultado da câmera</p>
                  <p className="mt-3 text-sm font-bold text-zinc-300">
                    {ticketScannerMessage || "Abra a câmera e aponte para o QR Code do ingresso."}
                  </p>
                </div>
              </div>

              <div className="mt-4 flex flex-wrap gap-2">
                {PARTY_USER_ALPHA_GROUPS.map((entry) => (
                  <button
                    key={entry.id}
                    type="button"
                    onClick={() => setScanGroup(entry.id)}
                    className={`rounded-xl border px-3 py-2 text-xs font-black uppercase ${
                      scanGroup === entry.id
                        ? "border-brand bg-brand-soft text-brand-accent"
                        : "border-zinc-800 bg-black/20 text-zinc-400"
                    }`}
                  >
                    {entry.label}
                  </button>
                ))}
              </div>

              <div className="mt-4 flex items-center gap-2 rounded-2xl border border-zinc-800 bg-black/20 px-3 py-3">
                <Search size={16} className="text-zinc-500" />
                <input
                  value={salesSearch}
                  onChange={(event) => setSalesSearch(event.target.value)}
                  placeholder="Buscar por usuário, turma, pedido ou lote..."
                  className="min-w-0 flex-1 bg-transparent text-sm outline-none placeholder:text-zinc-600"
                />
              </div>

              <div className="mt-4 overflow-hidden rounded-[1.4rem] border border-zinc-800">
                <div className="overflow-x-auto">
                  <table className="w-full min-w-[1100px] text-left text-sm">
                    <thead className="bg-black/40 text-[11px] uppercase tracking-[0.16em] text-zinc-500">
                      <tr>
                        <th className="px-4 py-3">Usuário</th>
                        <th className="px-4 py-3">Turma</th>
                        <th className="px-4 py-3">Pagamento</th>
                        <th className="px-4 py-3">Lote</th>
                        <th className="px-4 py-3">Check-in</th>
                        <th className="px-4 py-3">Última leitura</th>
                        <th className="px-4 py-3 text-right">Ação</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-zinc-800 bg-zinc-950/70">
                      {loadingSales ? (
                        <tr>
                          <td colSpan={7} className="px-4 py-10 text-center">
                            <Loader2 size={20} className="mx-auto animate-spin text-brand" />
                          </td>
                        </tr>
                      ) : filteredScanRows.length === 0 ? (
                        <tr>
                          <td colSpan={7} className="px-4 py-10 text-center text-zinc-500">
                            Nenhum ingresso aprovado encontrado para o filtro atual.
                          </td>
                        </tr>
                      ) : (
                        paginatedScanRows.map((row) => {
                          const checked = countSaleCheckins(row);
                          const totalTickets = Math.max(1, row.quantidade);
                          const fullyChecked = checked >= totalTickets;
                          return (
                            <tr key={row.id} className="hover:bg-white/[0.03]">
                              <td className="px-4 py-3">
                                <p className="font-bold text-white">{row.userName}</p>
                                <p className="font-mono text-xs text-zinc-500">#{row.id.slice(0, 8)}</p>
                              </td>
                              <td className="px-4 py-3 text-zinc-300">{row.userTurma || "-"}</td>
                              <td className="px-4 py-3 text-zinc-300">{row.status}</td>
                              <td className="px-4 py-3 text-zinc-400">{row.quantidade}x {row.loteNome}</td>
                              <td className={fullyChecked ? "px-4 py-3 font-black text-emerald-300" : "px-4 py-3 font-black text-yellow-200"}>
                                {checked}/{totalTickets}
                              </td>
                              <td className="px-4 py-3 text-zinc-400">{formatDateTime(getSaleLatestCheckinDate(row))}</td>
                              <td className="px-4 py-3 text-right">
                                <button
                                  type="button"
                                  onClick={() => void handleTicketManualCheckin(row)}
                                  disabled={fullyChecked || ticketCheckinMutatingId === row.id}
                                  className="inline-flex items-center justify-center gap-2 rounded-xl border border-brand bg-brand-soft px-3 py-2 text-[11px] font-black uppercase text-brand-accent disabled:border-zinc-800 disabled:bg-black/20 disabled:text-zinc-500"
                                >
                                  {ticketCheckinMutatingId === row.id ? <Loader2 size={13} className="animate-spin" /> : <Check size={13} />}
                                  {fullyChecked ? "Dentro" : "Check-in"}
                                </button>
                              </td>
                            </tr>
                          );
                        })
                      )}
                    </tbody>
                  </table>
                </div>
              </div>

              <div className="mt-4">
                <PaginationControls
                  page={salesPage}
                  totalPages={totalScanPages}
                  totalItems={filteredScanRows.length}
                  pageSize={SALES_PAGE_SIZE}
                  onPageChange={setSalesPage}
                />
              </div>
            </div>
          </section>
        ) : null}

        {section === "ingressos" ? (
          <section className="space-y-4">
            <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
              {[
                { label: "Pedidos", value: String(salesRows.length) },
                { label: "Ingressos emitidos", value: String(salesMetrics.tickets) },
                { label: "Aprovados", value: String(salesRows.filter((row) => row.status.toLowerCase() === "aprovado").length) },
                { label: "Check-ins lidos", value: String(checkinRows.length) },
              ].map((card) => (
                <div key={card.label} className="rounded-[1.5rem] border border-zinc-800 bg-zinc-950 p-5">
                  <p className="text-[11px] text-zinc-500">{card.label}</p>
                  <p className="mt-3 text-3xl font-black text-white">{card.value}</p>
                </div>
              ))}
            </div>

            <div className="rounded-[1.8rem] border border-zinc-800 bg-zinc-950 p-4 sm:p-5">
              <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
                <div>
                  <h2 className="text-lg font-black text-white">Ingressos e pedidos</h2>
                  <p className="text-sm text-zinc-500">Acompanhe pedidos, liberações e consumo dos ingressos do evento.</p>
                </div>
                <div className="flex flex-wrap gap-2">
                  <Link
                    href={scanEventoHref}
                    className="brand-button-soft"
                  >
                    <QrCode size={14} />
                    Scanner do evento
                  </Link>
                  <Link
                    href={eventSectionHref("checkins")}
                    className="inline-flex items-center gap-2 rounded-xl border border-zinc-700 bg-black/20 px-4 py-3 text-xs font-black uppercase text-zinc-200"
                  >
                    <Users size={14} />
                    Lista de presença
                  </Link>
                </div>
              </div>

              <div className="mt-4 grid gap-3 md:grid-cols-[1.3fr_0.8fr_auto]">
                <div className="flex items-center gap-2 rounded-2xl border border-zinc-800 bg-black/20 px-3 py-3">
                  <Search size={16} className="text-zinc-500" />
                  <input
                    value={salesSearch}
                    onChange={(event) => setSalesSearch(event.target.value)}
                    placeholder="Buscar por aluno, lote ou pedido..."
                    className="min-w-0 flex-1 bg-transparent text-sm outline-none placeholder:text-zinc-600"
                  />
                </div>
                <select
                  value={salesStatusFilter}
                  onChange={(event) => setSalesStatusFilter(event.target.value as EventStatementStatusFilter)}
                  className="admin-dark-select rounded-2xl border border-zinc-800 bg-zinc-950 px-4 py-3 text-sm text-zinc-100 outline-none"
                >
                  <option value="todos">Todos</option>
                  <option value="aprovado">Aprovado</option>
                  <option value="pendente">Pendente</option>
                  <option value="analise">Em análise</option>
                </select>
                <button
                  type="button"
                  onClick={() => void loadSales(true)}
                  className="rounded-2xl border border-zinc-700 bg-black/20 px-4 py-3 text-xs font-black uppercase text-zinc-200"
                >
                  Atualizar
                </button>
              </div>

              <div className="mt-4 overflow-hidden rounded-[1.4rem] border border-zinc-800">
                <div className="overflow-x-auto">
                  <table className="w-full min-w-[2200px] text-left text-sm">
                    <thead className="bg-black/40 text-[11px] uppercase tracking-[0.16em] text-zinc-500">
                      <tr>
                        <th className="px-4 py-3">Pedido</th>
                        <th className="px-4 py-3">Usuário</th>
                        <th className="px-4 py-3">Turma</th>
                        <th className="px-4 py-3">Status</th>
                        <th className="px-4 py-3">Lote</th>
                        <th className="px-4 py-3">Qtd.</th>
                        <th className="px-4 py-3">Unitário</th>
                        <th className="px-4 py-3">Valor</th>
                        <th className="px-4 py-3">QR</th>
                        <th className="px-4 py-3">Solicitado em</th>
                        <th className="px-4 py-3">Data do pagamento</th>
                        <th className="px-4 py-3">Hora do pagamento</th>
                        <th className="px-4 py-3">Aprovado por</th>
                        <th className="px-4 py-3">Comprovante</th>
                        <th className="px-4 py-3">Check-in</th>
                        <th className="px-4 py-3 text-right">Ações</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-zinc-800 bg-zinc-950/70">
                      {loadingSales ? (
                        <tr>
                          <td colSpan={16} className="px-4 py-10 text-center">
                            <Loader2 size={20} className="mx-auto animate-spin text-brand" />
                          </td>
                        </tr>
                      ) : filteredSales.length === 0 ? (
                        <tr>
                          <td colSpan={16} className="px-4 py-10 text-center text-zinc-500">
                            Nenhum ingresso encontrado para o filtro atual.
                          </td>
                        </tr>
                      ) : (
                        paginatedSales.map((row) => {
                          const ticketEntries = row.paymentConfig?.ticketEntries ?? [];
                          const checkins = countSaleCheckins(row);
                          const approved = normalizeStatementStatus(row.status) === "aprovado";
                          const paymentProof = resolveTicketProofUrl(row);
                          const latestCheckin = getSaleLatestCheckinDate(row);
                          const totalTickets = Math.max(1, row.quantidade);
                          const fullyChecked = checkins >= totalTickets;
                          return (
                            <tr key={row.id} className="hover:bg-white/[0.03]">
                              <td className="px-4 py-3">
                                <p className="font-mono text-xs font-bold text-zinc-200">#{row.id.slice(0, 8)}</p>
                                <p className="mt-1 max-w-[180px] truncate font-mono text-[10px] text-zinc-600" title={row.id}>
                                  {row.id}
                                </p>
                              </td>
                              <td className="px-4 py-3">
                                <p className="font-bold text-white">{row.userName}</p>
                                <p className="font-mono text-xs text-zinc-500">{row.userId || "-"}</p>
                              </td>
                              <td className="px-4 py-3 text-zinc-300">{row.userTurma || "-"}</td>
                              <td className="px-4 py-3">
                                <span className={`rounded-full border px-2.5 py-1 text-[10px] font-black uppercase ${
                                  approved
                                    ? "border-brand bg-brand-soft text-brand-accent"
                                    : "border-zinc-700 bg-zinc-900/60 text-zinc-300"
                                }`}>
                                  {row.status}
                                </span>
                              </td>
                              <td className="px-4 py-3 text-zinc-300">{row.loteNome || "-"}</td>
                              <td className="px-4 py-3 font-bold text-white">{row.quantidade}</td>
                              <td className="px-4 py-3 text-zinc-300">R$ {row.valorUnitario || "0,00"}</td>
                              <td className="px-4 py-3 font-black text-emerald-300">
                                {formatCurrency(parseCurrency(row.valorTotal))}
                              </td>
                              <td className="max-w-[260px] px-4 py-3">
                                <p className="text-xs font-bold text-zinc-300">
                                  {summarizeTicketQrStatus(ticketEntries, latestCheckin ? "Utilizado" : row.status)}
                                </p>
                                <p className="mt-1 truncate font-mono text-[10px] text-zinc-500" title={summarizeTicketCodes(ticketEntries)}>
                                  {summarizeTicketCodes(ticketEntries)}
                                </p>
                              </td>
                              <td className="px-4 py-3 text-zinc-400">{formatDateTime(row.dataSolicitacao)}</td>
                              <td className="px-4 py-3 text-zinc-400">{formatDateOnly(getSalePaymentDate(row))}</td>
                              <td className="px-4 py-3 text-zinc-400">{formatTimeOnly(getSalePaymentDate(row))}</td>
                              <td className="px-4 py-3 text-zinc-400">{row.aprovadoPor || "-"}</td>
                              <td className="px-4 py-3">
                                {paymentProof ? (
                                  <a
                                    href={paymentProof}
                                    target="_blank"
                                    rel="noreferrer"
                                    className="inline-flex items-center gap-2 rounded-xl border border-zinc-700 bg-black/20 px-3 py-2 text-[11px] font-black uppercase text-zinc-200"
                                  >
                                    <ImageIcon size={13} />
                                    Abrir
                                  </a>
                                ) : (
                                  <span className="text-zinc-500">-</span>
                                )}
                              </td>
                              <td className={fullyChecked ? "px-4 py-3 font-black text-emerald-300" : "px-4 py-3 font-black text-yellow-200"}>
                                {checkins}/{totalTickets}
                              </td>
                              <td className="px-4 py-3 text-right">
                                <div className="flex flex-wrap justify-end gap-2">
                                  <Link
                                    href={adminTicketOrderHref(row.id)}
                                    className="inline-flex items-center justify-center gap-2 rounded-xl border border-zinc-700 bg-black/20 px-3 py-2 text-[11px] font-black uppercase text-zinc-200"
                                  >
                                    <Edit3 size={13} />
                                    Detalhes
                                  </Link>
                                  <button
                                    type="button"
                                    onClick={() => void handleSalesPaymentToggle(row)}
                                    className={`inline-flex items-center justify-center gap-2 rounded-xl px-3 py-2 text-[11px] font-black uppercase ${
                                      approved
                                        ? "border border-zinc-700 bg-black/20 text-zinc-200"
                                        : "border border-brand bg-brand-soft text-brand-accent"
                                    }`}
                                  >
                                    {approved ? <RotateCcw size={13} /> : <Check size={13} />}
                                    {approved ? "Reabrir" : "Aprovar"}
                                  </button>
                                  <button
                                    type="button"
                                    onClick={() => void handleTicketManualCheckin(row)}
                                    disabled={!approved || fullyChecked || ticketCheckinMutatingId === row.id}
                                    className="inline-flex items-center justify-center gap-2 rounded-xl border border-brand bg-brand-soft px-3 py-2 text-[11px] font-black uppercase text-brand-accent disabled:border-zinc-800 disabled:bg-black/20 disabled:text-zinc-500"
                                  >
                                    {ticketCheckinMutatingId === row.id ? <Loader2 size={13} className="animate-spin" /> : <Check size={13} />}
                                    Check-in
                                  </button>
                                </div>
                              </td>
                            </tr>
                          );
                        })
                      )}
                    </tbody>
                  </table>
                </div>
              </div>

              <div className="hidden">
                {loadingSales ? (
                  <div className="rounded-[1.4rem] border border-zinc-800 bg-black/20 p-8 text-center">
                    <Loader2 size={20} className="mx-auto animate-spin text-brand" />
                  </div>
                ) : filteredSales.length === 0 ? (
                  <div className="rounded-[1.4rem] border border-dashed border-zinc-800 bg-black/20 p-8 text-center text-sm text-zinc-500">
                    Nenhum ingresso encontrado para o filtro atual.
                  </div>
                ) : (
                  paginatedSales.map((row) => {
                    const checkins = row.paymentConfig?.ticketEntries?.filter(
                      (entry) => entry.status === "lido" || Boolean(entry.scannedAt)
                    ).length ?? 0;
                    const approved = row.status.toLowerCase() === "aprovado";
                    const latestCheckin = getSaleLatestCheckinDate(row);
                    return (
                      <div key={row.id} className="rounded-[1.4rem] border border-zinc-800 bg-black/20 p-4">
                        <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                          <div>
                            <div className="flex flex-wrap items-center gap-2">
                              <h3 className="text-base font-black text-white">{row.userName}</h3>
                              <span className="rounded-full border border-zinc-700 px-2.5 py-1 text-[10px] font-black uppercase text-zinc-300">
                                {row.userTurma || "Sem turma"}
                              </span>
                              <span className={`rounded-full border px-2.5 py-1 text-[10px] font-black uppercase ${
                                approved
                                  ? "border-brand bg-brand-soft text-brand-accent"
                                  : "border-zinc-700 bg-zinc-900/60 text-zinc-300"
                              }`}>
                                {row.status}
                              </span>
                            </div>
                            <p className="mt-2 text-sm text-zinc-400">
                              Pedido #{row.id.slice(0, 8)} • {row.quantidade}x {row.loteNome}
                            </p>
                          </div>
                          <div className="flex flex-wrap gap-2">
                            {approved ? (
                              <Link
                                href={adminTicketOrderHref(row.id)}
                                className="inline-flex items-center justify-center gap-2 rounded-xl border border-brand bg-brand-soft px-4 py-3 text-xs font-black uppercase text-brand-accent"
                              >
                                <QrCode size={14} />
                                Detalhes
                              </Link>
                            ) : null}
                            <button
                              type="button"
                              onClick={() => void handleSalesPaymentToggle(row)}
                              className={`inline-flex items-center justify-center gap-2 rounded-xl px-4 py-3 text-xs font-black uppercase ${
                                approved
                                  ? "border border-zinc-700 bg-black/20 text-zinc-200"
                                  : "border border-brand bg-brand-soft text-brand-accent"
                              }`}
                            >
                              {approved ? <RotateCcw size={14} /> : <Check size={14} />}
                              {approved ? "Reabrir pagamento" : "Aprovar pagamento"}
                            </button>
                          </div>
                        </div>

                        <div className="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
                          <div className="rounded-2xl border border-zinc-800 bg-zinc-950/60 p-3">
                            <p className="text-[10px] uppercase tracking-[0.16em] text-zinc-500">Valor</p>
                            <p className="mt-2 text-lg font-black text-emerald-300">{formatCurrency(parseCurrency(row.valorTotal))}</p>
                          </div>
                          <div className="rounded-2xl border border-zinc-800 bg-zinc-950/60 p-3">
                            <p className="text-[10px] uppercase tracking-[0.16em] text-zinc-500">Unitário</p>
                            <p className="mt-2 text-lg font-black text-white">R$ {row.valorUnitario || "0,00"}</p>
                          </div>
                          <div className="rounded-2xl border border-zinc-800 bg-zinc-950/60 p-3">
                            <p className="text-[10px] uppercase tracking-[0.16em] text-zinc-500">QRs lidos</p>
                            <p className="mt-2 text-lg font-black text-white">{checkins}/{row.quantidade}</p>
                          </div>
                          <div className="rounded-2xl border border-zinc-800 bg-zinc-950/60 p-3">
                            <p className="text-[10px] uppercase tracking-[0.16em] text-zinc-500">Solicitado em</p>
                            <p className="mt-2 text-sm font-bold text-white">{formatDateTime(row.dataSolicitacao)}</p>
                          </div>
                          <div className="rounded-2xl border border-zinc-800 bg-zinc-950/60 p-3">
                            <p className="text-[10px] uppercase tracking-[0.16em] text-zinc-500">Data do pagamento</p>
                            <p className="mt-2 text-sm font-bold text-white">{formatDateOnly(getSalePaymentDate(row))}</p>
                          </div>
                          <div className="rounded-2xl border border-zinc-800 bg-zinc-950/60 p-3">
                            <p className="text-[10px] uppercase tracking-[0.16em] text-zinc-500">Aprovado por</p>
                            <p className="mt-2 text-sm font-bold text-white">{row.aprovadoPor || "-"}</p>
                          </div>
                          <div className="rounded-2xl border border-zinc-800 bg-zinc-950/60 p-3">
                            <p className="text-[10px] uppercase tracking-[0.16em] text-zinc-500">Hora do pagamento</p>
                            <p className="mt-2 text-sm font-bold text-white">{formatTimeOnly(getSalePaymentDate(row))}</p>
                          </div>
                          <div className="rounded-2xl border border-zinc-800 bg-zinc-950/60 p-3">
                            <p className="text-[10px] uppercase tracking-[0.16em] text-zinc-500">Check-in em</p>
                            <p className="mt-2 text-sm font-bold text-white">{formatDateTime(latestCheckin)}</p>
                          </div>
                        </div>
                      </div>
                    );
                  })
                )}
              </div>
              <div className="mt-4">
                <PaginationControls
                  page={salesPage}
                  totalPages={totalSalesPages}
                  totalItems={filteredSales.length}
                  pageSize={SALES_PAGE_SIZE}
                  onPageChange={setSalesPage}
                />
              </div>
            </div>
          </section>
        ) : null}

        {section === "cupons" ? (
          <section className="space-y-4">
            <div className="rounded-[1.8rem] border border-zinc-800 bg-zinc-950 p-4 sm:p-5">
              <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <h2 className="text-lg font-black text-white">Cupons do evento</h2>
                  <p className="text-sm text-zinc-500">Gerencie cupons internos e deixe o evento pronto para campanhas.</p>
                </div>
                <button
                  type="button"
                  onClick={handleOpenNewCoupon}
                  className="brand-button-soft"
                >
                  <Percent size={14} />
                  Criar novo cupom
                </button>
              </div>

              <div className="mt-4 space-y-3">
                {evento.dataExtra.cupons.length === 0 ? (
                  <div className="rounded-[1.4rem] border border-dashed border-zinc-800 bg-black/20 p-6 text-sm text-zinc-500">
                    Nenhum cupom cadastrado para este evento.
                  </div>
                ) : (
                  evento.dataExtra.cupons.map((coupon) => (
                    <div key={coupon.id} className="rounded-[1.4rem] border border-zinc-800 bg-black/20 p-4">
                      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                        <div>
                          <div className="flex flex-wrap items-center gap-2">
                            <h3 className="text-base font-black text-white">{coupon.titulo}</h3>
                            <span className="rounded-full border border-zinc-700 px-2.5 py-1 text-[10px] font-black uppercase text-zinc-300">
                              {coupon.codigo}
                            </span>
                            <span className={`rounded-full border px-2.5 py-1 text-[10px] font-black uppercase ${
                              coupon.ativo
                                ? "border-brand bg-brand-soft text-brand-accent"
                                : "border-zinc-700 bg-zinc-900/60 text-zinc-400"
                            }`}>
                              {coupon.ativo ? "Ativo" : "Inativo"}
                            </span>
                          </div>
                          <p className="mt-2 text-sm text-zinc-400">
                            {coupon.tipo === "percentual" ? `${coupon.valor}%` : `R$ ${coupon.valor || "0,00"}`} • mínimo R$ {coupon.valorMinimo || "0,00"}
                          </p>
                        </div>
                        <div className="flex gap-2">
                          <button
                            type="button"
                            onClick={() => handleOpenEditCoupon(coupon)}
                            className="inline-flex items-center gap-2 rounded-xl border border-zinc-700 bg-black/30 px-3 py-2 text-xs font-black uppercase text-zinc-200"
                          >
                            <Edit3 size={13} />
                            Editar
                          </button>
                          <button
                            type="button"
                            onClick={() => void handleDeleteCoupon(coupon.id)}
                            className="inline-flex items-center gap-2 rounded-xl border border-red-500/20 bg-red-500/10 px-3 py-2 text-xs font-black uppercase text-red-200"
                          >
                            <Trash2 size={13} />
                            Excluir
                          </button>
                        </div>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>

            <div className="rounded-[1.8rem] border border-zinc-800 bg-zinc-950 p-4 sm:p-5">
              <p className="text-[10px] font-black uppercase tracking-[0.24em] text-zinc-500">
                {editingCouponId === "new" ? "Novo cupom" : editingCouponId ? "Editar cupom" : "Criação de cupons"}
              </p>
              <h2 className="mt-2 text-xl font-black text-white">Crie um novo cupom</h2>
              <p className="mt-1 text-sm text-zinc-500">Configure o catálogo promocional específico deste evento.</p>

              <div className="mt-5 space-y-4">
                <div className="rounded-[1.4rem] border border-zinc-800 bg-black/20 p-4">
                  <p className="text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">Informações do cupom</p>
                  <div className="mt-4 grid gap-3">
                    <input
                      value={couponDraft.titulo}
                      onChange={(event) =>
                        setCouponDraft((previous) => ({
                          ...previous,
                          titulo: event.target.value.slice(0, EVENT_COUPON_TITLE_MAX_LENGTH),
                        }))
                      }
                      placeholder="Título do cupom"
                      className="rounded-2xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm outline-none placeholder:text-zinc-600"
                    />
                    <input
                      value={couponDraft.codigo}
                      onChange={(event) =>
                        setCouponDraft((previous) => ({
                          ...previous,
                          codigo: event.target.value.slice(0, EVENT_COUPON_CODE_MAX_LENGTH).toUpperCase(),
                        }))
                      }
                      placeholder="Código do cupom"
                      className="rounded-2xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm uppercase outline-none placeholder:text-zinc-600"
                    />
                  </div>
                </div>

                <div className="rounded-[1.4rem] border border-zinc-800 bg-black/20 p-4">
                  <p className="text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">Tipo e valor</p>
                  <div className="mt-4 grid gap-3 sm:grid-cols-2">
                    <select
                      value={couponDraft.tipo}
                      onChange={(event) =>
                        setCouponDraft((previous) => ({
                          ...previous,
                          tipo: event.target.value === "percentual" ? "percentual" : "valor",
                        }))
                      }
                      className="admin-dark-select rounded-2xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm text-zinc-100 outline-none"
                    >
                      <option value="valor">Valor fixo</option>
                      <option value="percentual">Percentual</option>
                    </select>
                    <input
                      value={couponDraft.valor}
                      onChange={(event) =>
                        setCouponDraft((previous) => ({ ...previous, valor: event.target.value }))
                      }
                      placeholder={couponDraft.tipo === "percentual" ? "Valor (%)" : "Valor (R$)"}
                      className="rounded-2xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm outline-none placeholder:text-zinc-600"
                    />
                    <input
                      value={couponDraft.valorMinimo}
                      onChange={(event) =>
                        setCouponDraft((previous) => ({ ...previous, valorMinimo: event.target.value }))
                      }
                      placeholder="Valor mínimo (R$)"
                      className="rounded-2xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm outline-none placeholder:text-zinc-600"
                    />
                    <input
                      value={couponDraft.valorMaximo}
                      onChange={(event) =>
                        setCouponDraft((previous) => ({ ...previous, valorMaximo: event.target.value }))
                      }
                      placeholder="Valor máximo (R$)"
                      className="rounded-2xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm outline-none placeholder:text-zinc-600"
                    />
                    <input
                      value={couponDraft.quantidadeDisponivel}
                      onChange={(event) =>
                        setCouponDraft((previous) => ({
                          ...previous,
                          quantidadeDisponivel: Math.max(0, Number(event.target.value) || 0),
                        }))
                      }
                      placeholder="Quantidade disponível"
                      inputMode="numeric"
                      className="rounded-2xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm outline-none placeholder:text-zinc-600 sm:col-span-2"
                    />
                  </div>
                </div>

                <label className="flex items-start gap-3 rounded-[1.4rem] border border-zinc-800 bg-black/20 p-4">
                  <input
                    type="checkbox"
                    checked={couponDraft.ativo}
                    onChange={(event) =>
                      setCouponDraft((previous) => ({ ...previous, ativo: event.target.checked }))
                    }
                    className="mt-1 h-4 w-4 accent-emerald-500"
                  />
                  <div>
                    <p className="font-bold text-white">Cupom ativo</p>
                    <p className="text-sm text-zinc-500">Mantém o cupom disponível na operação deste evento.</p>
                  </div>
                </label>
              </div>

              <div className="mt-5 flex flex-col gap-3 border-t border-zinc-800 pt-4 sm:flex-row sm:justify-end">
                {editingCouponId ? (
                  <button
                    type="button"
                    onClick={() => {
                      setEditingCouponId(null);
                      setCouponDraft(createEmptyCoupon());
                    }}
                    className="rounded-2xl border border-zinc-700 bg-black/20 px-4 py-3 text-xs font-black uppercase text-zinc-300"
                  >
                    Cancelar
                  </button>
                ) : null}
                <button
                  ref={saveCouponButtonRef}
                  type="button"
                  onClick={() => void handleSaveCoupon()}
                  disabled={savingEvento}
                  className="brand-button-soft"
                >
                  {editingCouponId === "new" ? "Criar cupom" : editingCouponId ? "Salvar cupom" : "Salvar rascunho"}
                </button>
              </div>
              <FloatingSaveButton
                watchRef={saveCouponButtonRef}
                label={editingCouponId === "new" ? "Criar cupom" : editingCouponId ? "Salvar cupom" : "Salvar rascunho"}
                icon={<Save size={14} />}
                disabled={savingEvento}
                onClick={() => void handleSaveCoupon()}
              />
            </div>
          </section>
        ) : null}

        {section === "checkins" ? (
          <section className="space-y-4">
            <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
              {[
                {
                  label: "Ingressos pagos",
                  value: String(filteredPresenceRows.reduce((sum, row) => sum + row.quantidade, 0)),
                },
                { label: "Check-ins lidos", value: String(checkinRows.length) },
                {
                  label: "Pendentes de entrada",
                  value: String(
                    Math.max(0, filteredPresenceRows.reduce((sum, row) => sum + row.quantidade, 0) - checkinRows.length)
                  ),
                },
                { label: "Leitores únicos", value: String(operatorPerformance.length) },
              ].map((card) => (
                <div key={card.label} className="rounded-[1.5rem] border border-zinc-800 bg-zinc-950 p-5">
                  <p className="text-[11px] text-zinc-500">{card.label}</p>
                  <p className="mt-3 text-3xl font-black text-white">{card.value}</p>
                </div>
              ))}
            </div>

            <div className="rounded-[1.8rem] border border-zinc-800 bg-zinc-950 p-4 sm:p-5">
              <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
                <div>
                  <h2 className="text-lg font-black text-white">Lista de presença</h2>
                  <p className="text-sm text-zinc-500">Ingressos aprovados, leitura do QR e check-in manual em uma única tabela.</p>
                </div>
                <div className="flex flex-wrap gap-2">
                  <Link href={scanEventoHref} className="brand-button-soft">
                    <QrCode size={14} />
                    Scan
                  </Link>
                  <Link
                    href={eventPartyUserCadastroHref}
                    className="inline-flex items-center justify-center gap-2 rounded-xl border border-zinc-700 bg-black/20 px-4 py-3 text-xs font-black uppercase text-zinc-200"
                  >
                    <UserPlus size={14} />
                    Cadastrar usuário
                  </Link>
                  <button
                    type="button"
                    onClick={exportPresenceCsv}
                    disabled={filteredPresenceRows.length === 0}
                    className="inline-flex items-center justify-center gap-2 rounded-xl border border-zinc-700 bg-black/20 px-4 py-3 text-xs font-black uppercase text-zinc-200 disabled:opacity-40"
                  >
                    <Download size={14} />
                    Exportar CSV
                  </button>
                </div>
              </div>

              <div className="mt-4 grid gap-3 md:grid-cols-[1fr_auto]">
                <div className="flex items-center gap-2 rounded-2xl border border-zinc-800 bg-black/20 px-3 py-3">
                  <Search size={16} className="text-zinc-500" />
                  <input
                    value={salesSearch}
                    onChange={(event) => setSalesSearch(event.target.value)}
                    placeholder="Buscar por nome, turma, pedido, QR ou transferência..."
                    className="min-w-0 flex-1 bg-transparent text-sm outline-none placeholder:text-zinc-600"
                  />
                </div>
                <button
                  type="button"
                  onClick={() => void loadSales(true)}
                  className="rounded-2xl border border-zinc-700 bg-black/20 px-4 py-3 text-xs font-black uppercase text-zinc-200"
                  >
                    Atualizar
                  </button>
                </div>
                <div className="mt-3 flex flex-wrap gap-2">
                  {[
                    { value: "", label: "Todos" },
                    { value: "presente", label: "Presentes" },
                    { value: "ausente", label: "Ausentes" },
                    { value: "manual", label: "Manuais" },
                  ].map((filter) => (
                    <button
                      key={filter.value || "todos"}
                      type="button"
                      onClick={() => setSalesIndicatorFilter(filter.value)}
                      className={`rounded-xl border px-3 py-2 text-[11px] font-black uppercase transition ${
                        salesIndicatorFilter === filter.value
                          ? "border-brand bg-brand-soft text-brand-accent"
                          : "border-zinc-800 bg-black/20 text-zinc-400 hover:text-white"
                      }`}
                    >
                      {filter.label}
                    </button>
                  ))}
                </div>

              <div className="mt-4 overflow-hidden rounded-[1.4rem] border border-zinc-800">
                <div className="overflow-x-auto">
                  <table className="w-full min-w-[2400px] text-left text-sm">
                    <thead className="bg-black/40 text-[11px] uppercase tracking-[0.16em] text-zinc-500">
                      <tr>
                        <th className="px-4 py-3">Usuário</th>
                        <th className="px-4 py-3">Turma</th>
                        <th className="px-4 py-3">Ingresso</th>
                        <th className="px-4 py-3">Lote</th>
                        <th className="px-4 py-3">Pagamento</th>
                        <th className="px-4 py-3">Data do pagamento</th>
                        <th className="px-4 py-3">Hora do pagamento</th>
                        <th className="px-4 py-3">Check-in</th>
                        <th className="px-4 py-3">Entrada em</th>
                        <th className="px-4 py-3">Quem escaneou</th>
                        <th className="px-4 py-3">Método</th>
                        <th className="px-4 py-3">QR</th>
                        <th className="px-4 py-3">Observação</th>
                        <th className="px-4 py-3">Editado em</th>
                        <th className="px-4 py-3">Editado por</th>
                        <th className="px-4 py-3">Transf. em</th>
                        <th className="px-4 py-3">De</th>
                        <th className="px-4 py-3">Para</th>
                        <th className="px-4 py-3">Transferido por</th>
                        <th className="px-4 py-3">Histórico</th>
                        <th className="px-4 py-3 text-right">Ações</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-zinc-800 bg-zinc-950/70">
                      {loadingSales ? (
                        <tr>
                          <td colSpan={21} className="px-4 py-10 text-center">
                            <Loader2 size={20} className="mx-auto animate-spin text-brand" />
                          </td>
                        </tr>
                      ) : filteredPresenceRows.length === 0 ? (
                        <tr>
                          <td colSpan={21} className="px-4 py-10 text-center text-zinc-500">
                            Nenhum ingresso pago encontrado para a lista de presença.
                          </td>
                        </tr>
                      ) : (
                        paginatedPresenceRows.map((row) => {
                          const saleRow = salesRowById.get(row.id);
                          const readCount = saleRow ? countSaleCheckins(saleRow) : row.checkinAt ? 1 : 0;
                          const totalTickets = Math.max(1, saleRow?.quantidade ?? row.quantidade);
                          const fullyChecked = readCount >= totalTickets;
                          const hasActiveTicket = Boolean(
                            saleRow?.paymentConfig?.ticketEntries?.some((entry) => entry.status === "ativo")
                          );
                          return (
                            <tr key={row.id} className="hover:bg-white/[0.03]">
                              <td className="px-4 py-3">
                                <p className="font-bold text-white">{row.userName}</p>
                                <p className="font-mono text-xs text-zinc-500">#{row.id.slice(0, 8)}</p>
                              </td>
                              <td className="px-4 py-3 text-zinc-300">{row.userTurma || "-"}</td>
                              <td className="px-4 py-3 text-zinc-300">{row.itemName}</td>
                              <td className="px-4 py-3 text-zinc-400">{row.loteNome}</td>
                              <td className="px-4 py-3">
                                <span className="rounded-full border border-brand bg-brand-soft px-2.5 py-1 text-[10px] font-black uppercase text-brand-accent">
                                  {row.status}
                                </span>
                              </td>
                              <td className="px-4 py-3 text-zinc-400">{formatDateOnly(row.paymentAt)}</td>
                              <td className="px-4 py-3 text-zinc-400">{formatTimeOnly(row.paymentAt)}</td>
                              <td className={fullyChecked ? "px-4 py-3 font-black text-emerald-300" : "px-4 py-3 font-black text-yellow-200"}>
                                {readCount}/{totalTickets}
                              </td>
                              <td className="px-4 py-3 text-zinc-400">{formatDateTime(row.checkinAt)}</td>
                              <td className="px-4 py-3 text-zinc-400">{row.checkinBy || "-"}</td>
                              <td className="px-4 py-3 text-zinc-400">{row.checkinMethod}</td>
                              <td className="max-w-[220px] truncate px-4 py-3 font-mono text-xs text-zinc-400" title={row.qrCode}>
                                {row.qrStatus || "-"}
                              </td>
                              <td className="max-w-[220px] truncate px-4 py-3 text-zinc-400" title={row.checkinNote}>
                                {row.checkinNote || "-"}
                              </td>
                              <td className="px-4 py-3 text-zinc-400">{formatDateTime(row.checkinEditedAt)}</td>
                              <td className="px-4 py-3 text-zinc-400">{row.checkinEditedBy || "-"}</td>
                              <td className="px-4 py-3 text-zinc-400">{formatDateTime(row.transferAt)}</td>
                              <td className="px-4 py-3 text-zinc-400">{row.transferFromUserName || "-"}</td>
                              <td className="px-4 py-3 text-zinc-400">{row.transferToUserName || "-"}</td>
                              <td className="px-4 py-3 text-zinc-400">{row.transferByUserName || "-"}</td>
                              <td className="max-w-[240px] truncate px-4 py-3 text-zinc-400" title={row.transferInfo}>
                                {row.transferInfo || "-"}
                              </td>
                              <td className="px-4 py-3 text-right">
                                <div className="flex flex-wrap justify-end gap-2">
                                  <button
                                    type="button"
                                    onClick={() => saleRow && void handleTicketManualCheckin(saleRow)}
                                    disabled={!saleRow || fullyChecked || ticketCheckinMutatingId === row.id}
                                    className="inline-flex items-center justify-center gap-2 rounded-xl border border-brand bg-brand-soft px-3 py-2 text-[11px] font-black uppercase text-brand-accent disabled:border-zinc-800 disabled:bg-black/20 disabled:text-zinc-500"
                                  >
                                    {ticketCheckinMutatingId === row.id ? <Loader2 size={13} className="animate-spin" /> : <Check size={13} />}
                                    {fullyChecked ? "Dentro" : "Check-in"}
                                  </button>
                                  <button
                                    type="button"
                                    onClick={() => saleRow && void handleEditTicketCheckin(saleRow)}
                                    disabled={!saleRow || readCount === 0 || ticketCheckinMutatingId === row.id}
                                    className="inline-flex items-center justify-center gap-2 rounded-xl border border-zinc-700 bg-black/20 px-3 py-2 text-[11px] font-black uppercase text-zinc-200 disabled:opacity-40"
                                  >
                                    <Edit3 size={13} />
                                    Editar
                                  </button>
                                  <button
                                    type="button"
                                    onClick={() => saleRow && void handleUndoTicketCheckin(saleRow)}
                                    disabled={!saleRow || readCount === 0 || ticketCheckinMutatingId === row.id}
                                    className="inline-flex items-center justify-center gap-2 rounded-xl border border-red-500/30 bg-red-500/10 px-3 py-2 text-[11px] font-black uppercase text-red-200 disabled:opacity-40"
                                  >
                                    <RotateCcw size={13} />
                                    Desfazer
                                  </button>
                                  <button
                                    type="button"
                                    onClick={() => saleRow && void handleManualTicketTransfer(saleRow)}
                                    disabled={!saleRow || !hasActiveTicket || ticketCheckinMutatingId === row.id}
                                    className="inline-flex items-center justify-center gap-2 rounded-xl border border-cyan-500/30 bg-cyan-500/10 px-3 py-2 text-[11px] font-black uppercase text-cyan-200 disabled:opacity-40"
                                  >
                                    <UserPlus size={13} />
                                    Transferir
                                  </button>
                                </div>
                              </td>
                            </tr>
                          );
                        })
                      )}
                    </tbody>
                  </table>
                </div>
              </div>

              <div className="mt-4">
                <PaginationControls
                  page={salesPage}
                  totalPages={totalPresencePages}
                  totalItems={filteredPresenceRows.length}
                  pageSize={SALES_PAGE_SIZE}
                  onPageChange={setSalesPage}
                />
              </div>
            </div>

            <div className="hidden">
              <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
                {[
                  { label: "Ingressos emitidos", value: String(salesMetrics.tickets) },
                  { label: "Check-ins lidos", value: String(checkinRows.length) },
                  {
                    label: "Taxa de leitura",
                    value:
                      salesMetrics.tickets > 0
                        ? `${Math.round((checkinRows.length / salesMetrics.tickets) * 100)}%`
                        : "0%",
                  },
                  { label: "Leitores únicos", value: String(operatorPerformance.length) },
                ].map((card) => (
                  <div key={card.label} className="rounded-[1.5rem] border border-zinc-800 bg-zinc-950 p-5">
                    <p className="text-[11px] text-zinc-500">{card.label}</p>
                    <p className="mt-3 text-3xl font-black text-white">{card.value}</p>
                  </div>
                ))}
              </div>

              <div className="hidden">
                <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
                  <div>
                    <h2 className="text-lg font-black text-white">Histórico de leituras</h2>
                    <p className="text-sm text-zinc-500">Veja quem validou cada ingresso e acione o scanner do evento.</p>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <Link
                      href={scanEventoHref}
                      className="brand-button-soft"
                    >
                      <QrCode size={14} />
                      Scanner do evento
                    </Link>
                    <Link
                      href={scanHubHref}
                      className="inline-flex items-center gap-2 rounded-xl border border-zinc-700 bg-black/20 px-4 py-3 text-xs font-black uppercase text-zinc-200"
                    >
                      <ScanLine size={14} />
                      Hub de scanner
                    </Link>
                  </div>
                </div>

                <div className="mt-4 space-y-3">
                  {operatorPerformance.length > 0 ? (
                    <div className="rounded-[1.4rem] border border-brand/20 bg-brand-soft/40 p-4">
                      <p className="text-[10px] font-black uppercase tracking-[0.22em] text-brand-accent">
                        Leitores do check-in
                      </p>
                      <div className="mt-3 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
                        {operatorPerformance.map((operator) => (
                          <div key={operator.id || operator.name} className="flex items-center gap-3 rounded-2xl border border-zinc-800 bg-black/25 p-3">
                            <div className="relative h-11 w-11 overflow-hidden rounded-full border border-zinc-700 bg-zinc-900">
                              {operator.avatarUrl ? (
                                <Image
                                  src={operator.avatarUrl}
                                  alt={operator.name}
                                  fill
                                  sizes="44px"
                                  className="object-cover"
                                  unoptimized={operator.avatarUrl.startsWith("http")}
                                />
                              ) : (
                                <div className="flex h-full w-full items-center justify-center text-sm font-black text-zinc-300">
                                  {operator.name.slice(0, 1).toUpperCase() || "O"}
                                </div>
                              )}
                            </div>
                            <div className="min-w-0">
                              <p className="truncate text-sm font-black text-white">{operator.name}</p>
                              <p className="text-xs font-semibold text-zinc-500">
                                {operator.total} leitura{operator.total === 1 ? "" : "s"}
                                {operator.turma ? ` • ${operator.turma}` : ""}
                              </p>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  ) : null}
                  {loadingSales ? (
                    <div className="rounded-[1.4rem] border border-zinc-800 bg-black/20 p-8 text-center">
                      <Loader2 size={20} className="mx-auto animate-spin text-brand" />
                    </div>
                  ) : checkinRows.length === 0 ? (
                    <div className="rounded-[1.4rem] border border-dashed border-zinc-800 bg-black/20 p-8 text-center text-sm text-zinc-500">
                      Nenhum check-in realizado até agora.
                    </div>
                  ) : (
                    checkinRows.map((row) => (
                      <div key={`${row.orderId}:${row.ticketToken}`} className="rounded-[1.4rem] border border-zinc-800 bg-black/20 p-4">
                        <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                          <div>
                            <div className="flex flex-wrap items-center gap-2">
                              <h3 className="text-base font-black text-white">{row.holderName}</h3>
                              <span className="rounded-full border border-zinc-700 px-2.5 py-1 text-[10px] font-black uppercase text-zinc-300">
                                {row.ticketLabel}
                              </span>
                            </div>
                            <p className="mt-2 text-sm text-zinc-400">
                              {row.holderTurma || "Sem turma"} • {row.loteNome || "Sem lote"}
                            </p>
                          </div>
                          <div className="rounded-2xl border border-zinc-800 bg-zinc-950/70 px-4 py-3 text-right">
                            <p className="text-[10px] uppercase tracking-[0.16em] text-zinc-500">Lido em</p>
                            <p className="mt-2 text-sm font-bold text-white">{formatDateTime(row.scannedAt)}</p>
                          </div>
                        </div>
                        <div className="mt-4 grid gap-3 sm:grid-cols-2">
                          <div className="rounded-2xl border border-zinc-800 bg-zinc-950/60 p-3">
                            <p className="text-[10px] uppercase tracking-[0.16em] text-zinc-500">Pedido</p>
                            <p className="mt-2 font-mono text-xs text-zinc-300">{row.orderId}</p>
                          </div>
                          <div className="rounded-2xl border border-zinc-800 bg-zinc-950/60 p-3">
                            <p className="text-[10px] uppercase tracking-[0.16em] text-zinc-500">Operador</p>
                            <p className="mt-2 font-bold text-white">
                              {row.scannedByUserName || "Operador"}
                              {row.scannedByUserTurma ? ` • ${row.scannedByUserTurma}` : ""}
                            </p>
                          </div>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>
            </div>

            <div className="space-y-4">
              <div className="hidden">
                <p className="text-[10px] font-black uppercase tracking-[0.24em] text-zinc-500">
                  {editingOperatorId === "new" ? "Novo operador" : editingOperatorId ? "Editar operador" : "Equipe de apoio"}
                </p>
                <h2 className="mt-2 text-xl font-black text-white">Equipe visual de check-in</h2>
                <p className="mt-1 text-sm text-zinc-500">
                  Cadastre os nomes de apoio do evento. As permissões reais de scanner continuam sendo controladas pelo perfil do app.
                </p>

                <div className="mt-5 space-y-4">
                  <input
                    value={operatorDraft.nome}
                    onChange={(event) =>
                      setOperatorDraft((previous) => ({
                        ...previous,
                        nome: event.target.value.slice(0, EVENT_OPERATOR_NAME_MAX_LENGTH),
                      }))
                    }
                    placeholder="Nome do operador"
                    className="w-full rounded-2xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm outline-none placeholder:text-zinc-600"
                  />
                  <input
                    value={operatorDraft.email}
                    onChange={(event) =>
                      setOperatorDraft((previous) => ({
                        ...previous,
                        email: event.target.value.slice(0, EVENT_OPERATOR_EMAIL_MAX_LENGTH),
                      }))
                    }
                    placeholder="E-mail do operador"
                    className="w-full rounded-2xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm outline-none placeholder:text-zinc-600"
                  />
                  <label className="flex items-start gap-3 rounded-2xl border border-zinc-800 bg-black/20 p-4">
                    <input
                      type="checkbox"
                      checked={operatorDraft.ativo}
                      onChange={(event) =>
                        setOperatorDraft((previous) => ({ ...previous, ativo: event.target.checked }))
                      }
                      className="mt-1 h-4 w-4 accent-emerald-500"
                    />
                    <div>
                      <p className="font-bold text-white">Operador ativo</p>
                      <p className="text-sm text-zinc-500">Mantém este nome visível para a operação do evento.</p>
                    </div>
                  </label>
                </div>

                <div className="mt-5 flex flex-col gap-3 border-t border-zinc-800 pt-4 sm:flex-row sm:justify-end">
                  {editingOperatorId ? (
                    <button
                      type="button"
                      onClick={() => {
                        setEditingOperatorId(null);
                        setOperatorDraft(createEmptyCheckinOperator());
                      }}
                      className="rounded-2xl border border-zinc-700 bg-black/20 px-4 py-3 text-xs font-black uppercase text-zinc-300"
                    >
                      Cancelar
                    </button>
                  ) : null}
                  <button
                    ref={saveOperatorButtonRef}
                    type="button"
                    onClick={() => void handleSaveOperator()}
                    disabled={savingEvento}
                    className="brand-button-soft"
                  >
                    <Users size={14} />
                    {editingOperatorId === "new" ? "Adicionar operador" : editingOperatorId ? "Salvar operador" : "Salvar rascunho"}
                  </button>
                </div>
                <FloatingSaveButton
                  watchRef={saveOperatorButtonRef}
                  label={editingOperatorId === "new" ? "Adicionar operador" : editingOperatorId ? "Salvar operador" : "Salvar rascunho"}
                  icon={<Save size={14} />}
                  disabled={savingEvento}
                  onClick={() => void handleSaveOperator()}
                />
              </div>

              <div className="hidden">
                <div className="flex items-center justify-between gap-3">
                  <div>
                    <h3 className="text-lg font-black text-white">Leituras por operador</h3>
                    <p className="text-sm text-zinc-500">Resumo real do scanner com base nos ingressos lidos.</p>
                  </div>
                  <button
                    type="button"
                    onClick={handleOpenNewOperator}
                    className="rounded-xl border border-zinc-700 bg-black/20 px-3 py-2 text-xs font-black uppercase text-zinc-200"
                  >
                    Novo
                  </button>
                </div>

                <div className="mt-4 space-y-3">
                  {operatorPerformance.length === 0 && evento.dataExtra.checkinOperators.length === 0 ? (
                    <div className="rounded-[1.4rem] border border-dashed border-zinc-800 bg-black/20 p-6 text-sm text-zinc-500">
                      Nenhum operador visível ou leitura registrada.
                    </div>
                  ) : null}

                  {operatorPerformance.map((operator) => (
                    <div key={operator.name} className="rounded-[1.2rem] border border-zinc-800 bg-black/20 p-4">
                      <p className="font-black text-white">{operator.name}</p>
                      <p className="mt-2 text-sm text-zinc-500">{operator.total} validações realizadas</p>
                    </div>
                  ))}

                  {evento.dataExtra.checkinOperators.map((operator) => (
                    <div key={operator.id} className="rounded-[1.2rem] border border-zinc-800 bg-black/20 p-4">
                      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                        <div>
                          <p className="font-black text-white">{operator.nome}</p>
                          <p className="mt-1 text-sm text-zinc-500">{operator.email || "Sem e-mail"} • {operator.ativo ? "Ativo" : "Inativo"}</p>
                        </div>
                        <div className="flex gap-2">
                          <button
                            type="button"
                            onClick={() => handleOpenEditOperator(operator)}
                            className="rounded-xl border border-zinc-700 bg-black/20 px-3 py-2 text-xs font-black uppercase text-zinc-200"
                          >
                            Editar
                          </button>
                          <button
                            type="button"
                            onClick={() => void handleDeleteOperator(operator.id)}
                            className="rounded-xl border border-red-500/20 bg-red-500/10 px-3 py-2 text-xs font-black uppercase text-red-200"
                          >
                            Excluir
                          </button>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </section>
        ) : null}

        {section === "ficha" ? (
          <section className="space-y-4">
            <div className="space-y-4">
              <div className="rounded-[1.8rem] border border-zinc-800 bg-zinc-950 p-4 sm:p-5">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                  <div>
                    <p className="text-[10px] font-black uppercase tracking-[0.24em] text-zinc-500">Modo vendas</p>
                    <h2 className="mt-2 text-xl font-black text-white">Vendas via pagamento e QR Code</h2>
                    <p className="mt-1 text-sm text-zinc-500">
                      Ativar modo para vender produtos durante o evento diretamente via pagamento e QR Code.
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => void handleTogglePartyMode()}
                    disabled={savingEvento}
                    className={`group inline-flex min-w-56 items-center justify-between gap-4 rounded-2xl border px-5 py-4 text-left shadow-2xl transition ${
                      partyConfig.enabled
                        ? "border-emerald-400/40 bg-emerald-500/15 shadow-emerald-950/40"
                        : "border-zinc-700 bg-black/30 shadow-black/30 hover:border-emerald-500/30"
                    }`}
                  >
                    <span>
                      <span className="block text-[10px] font-black uppercase tracking-[0.22em] text-zinc-500">
                        Status
                      </span>
                      <span className={`mt-1 block text-xs font-black uppercase ${partyConfig.enabled ? "text-emerald-200" : "text-zinc-200"}`}>
                        {savingEvento ? "Salvando..." : partyConfig.enabled ? "Modo vendas ativo" : "Ativar modo vendas"}
                      </span>
                    </span>
                    <span className={`relative h-8 w-14 rounded-full border transition ${partyConfig.enabled ? "border-emerald-300 bg-emerald-400" : "border-zinc-700 bg-zinc-900"}`}>
                      <span className={`absolute top-1 h-6 w-6 rounded-full bg-white shadow-lg transition ${partyConfig.enabled ? "left-7" : "left-1"}`} />
                    </span>
                  </button>
                </div>

                <div className="hidden">
                  <label className="text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">
                    Menu do evento
                    <input
                      value={partyConfig.menuTitle}
                      onChange={(event) => updatePartyConfigDraft({ menuTitle: event.target.value.slice(0, 80) })}
                      className="mt-2 w-full rounded-2xl border border-zinc-700 bg-black/30 px-4 py-3 text-sm text-white outline-none"
                      placeholder="Menu do evento"
                    />
                  </label>
                  <label className="text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">
                    Categoria
                    <input
                      value={partyConfig.categoryName}
                      onChange={(event) => {
                        const categoryName = event.target.value.slice(0, 80);
                        updatePartyConfigDraft({ categoryName });
                        setPartyProductDraft((previous) => ({ ...previous, categoria: categoryName }));
                      }}
                      className="mt-2 w-full rounded-2xl border border-zinc-700 bg-black/30 px-4 py-3 text-sm text-white outline-none"
                      placeholder="Bebidas, Fichas, Menu..."
                    />
                  </label>
                </div>

                <div className="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-6">
                  <Link
                    href={eventProductsHref}
                    className="hidden"
                  >
                    Ver menu público
                  </Link>
                  <Link
                    href={eventProductTicketsHref}
                    className="hidden"
                  >
                    Ver fichas
                  </Link>
                  <Link
                    href={eventPartyProductsAdminHref}
                    className="rounded-2xl border border-zinc-700 bg-black/20 px-4 py-3 text-center text-xs font-black uppercase text-zinc-200"
                  >
                    Produtos
                  </Link>
                  <Link
                    href={eventPartyProductHref}
                    className="rounded-2xl border border-zinc-700 bg-black/20 px-4 py-3 text-center text-xs font-black uppercase text-zinc-200"
                  >
                    Cadastrar produto
                  </Link>
                  <Link
                    href={eventPartyPaymentHref}
                    className="rounded-2xl border border-zinc-700 bg-black/20 px-4 py-3 text-center text-xs font-black uppercase text-zinc-200"
                  >
                    Pagamentos
                  </Link>
                  <Link
                    href={eventPartyUserCadastroHref}
                    className="rounded-2xl border border-zinc-700 bg-black/20 px-4 py-3 text-center text-xs font-black uppercase text-zinc-200"
                  >
                    Cadastro usuário
                  </Link>
                  <Link
                    href={eventSectionHref("recebedores")}
                    className="rounded-2xl border border-zinc-700 bg-black/20 px-4 py-3 text-center text-xs font-black uppercase text-zinc-200"
                  >
                    Recebedores
                  </Link>
                  <Link
                    href={eventPartyWithdrawalHref}
                    className="rounded-2xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-center text-xs font-black uppercase text-emerald-200"
                  >
                    Retirada
                  </Link>
                  <button
                    ref={savePartyButtonRef}
                    type="button"
                    onClick={() => void handleSavePartyConfig()}
                    disabled={savingEvento}
                    className="hidden"
                  >
                    {savingEvento ? <Loader2 size={14} className="animate-spin" /> : <Save size={14} />}
                    Salvar modo vendas
                  </button>
                </div>
              </div>

              <div className="hidden">
                <h2 className="text-lg font-black text-white">Adicionar produto</h2>
                <div className="mt-4 grid gap-3">
                  <input
                    value={partyProductDraft.nome}
                    onChange={(event) => setPartyProductDraft((previous) => ({ ...previous, nome: event.target.value.slice(0, 120) }))}
                    className="rounded-2xl border border-zinc-700 bg-black/30 px-4 py-3 text-sm outline-none"
                    placeholder="Nome do produto"
                  />
                  <div className="grid gap-3 sm:grid-cols-3">
                    <input
                      value={partyProductDraft.preco}
                      onChange={(event) => setPartyProductDraft((previous) => ({ ...previous, preco: event.target.value }))}
                      className="rounded-2xl border border-zinc-700 bg-black/30 px-4 py-3 text-sm outline-none"
                      placeholder="Preço (R$)"
                      inputMode="decimal"
                    />
                    <input
                      value={partyProductDraft.estoque}
                      onChange={(event) => setPartyProductDraft((previous) => ({ ...previous, estoque: event.target.value.replace(/\D/g, "").slice(0, 6) }))}
                      className="rounded-2xl border border-zinc-700 bg-black/30 px-4 py-3 text-sm outline-none"
                      placeholder="Estoque"
                      inputMode="numeric"
                    />
                    <input
                      value={partyProductDraft.categoria}
                      onChange={(event) => setPartyProductDraft((previous) => ({ ...previous, categoria: event.target.value.slice(0, 80) }))}
                      className="rounded-2xl border border-zinc-700 bg-black/30 px-4 py-3 text-sm outline-none"
                      placeholder={partyConfig.categoryName}
                    />
                  </div>
                  <div className="rounded-2xl border border-zinc-800 bg-black/20 p-3">
                    <label className="text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">
                      Custo total do evento
                    </label>
                    <input
                      type="number"
                      min="0"
                      step="0.01"
                      value={String(editDraft.dataExtra.raw.custo ?? editDraft.dataExtra.raw.cost ?? "")}
                      onChange={(event) =>
                        setEditDraft((previous) => {
                          if (!previous) return previous;
                          const cost = Math.max(0, Number(event.target.value) || 0);
                          const nextRaw = { ...previous.dataExtra.raw };
                          if (cost > 0) {
                            nextRaw.custo = cost;
                            nextRaw.cost = cost;
                          } else {
                            delete nextRaw.custo;
                            delete nextRaw.cost;
                          }
                          return {
                            ...previous,
                            dataExtra: {
                              ...previous.dataExtra,
                              raw: nextRaw,
                            },
                          };
                        })
                      }
                      placeholder="Ex.: 2500,00"
                      className="mt-2 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-3 text-sm text-zinc-100 outline-none placeholder:text-zinc-600"
                    />
                    <p className="mt-2 text-[11px] text-zinc-500">
                      Opcional. O BI Estratégico usa esse valor na cascata de resultado, margem e ponto de equilíbrio.
                    </p>
                  </div>
                  <textarea
                    value={partyProductDraft.descricao}
                    onChange={(event) => setPartyProductDraft((previous) => ({ ...previous, descricao: event.target.value.slice(0, 500) }))}
                    className="min-h-24 rounded-2xl border border-zinc-700 bg-black/30 px-4 py-3 text-sm outline-none"
                    placeholder="Descrição rápida"
                  />
                  <button
                    type="button"
                    onClick={() => void handleCreatePartyProduct()}
                    className="brand-button-solid justify-center py-3 text-xs"
                  >
                    <Package size={14} />
                    Adicionar produto ao evento
                  </button>
                </div>
              </div>
            </div>

            <div className="hidden">
              <div className="rounded-[1.8rem] border border-zinc-800 bg-zinc-950 p-4 sm:p-5">
                <div className="flex items-center justify-between gap-3">
                  <div>
                    <h2 className="text-lg font-black text-white">Produtos disponíveis</h2>
                    <p className="mt-1 text-sm text-zinc-500">{partyProducts.length} item(ns) no menu.</p>
                  </div>
                  <button
                    type="button"
                    onClick={() => void loadPartyProducts()}
                    className="rounded-xl border border-zinc-700 bg-black/20 px-3 py-2 text-xs font-black uppercase text-zinc-200"
                  >
                    Atualizar
                  </button>
                </div>
                <div className="mt-4 space-y-3">
                  {loadingPartyProducts ? (
                    <div className="rounded-2xl border border-zinc-800 bg-black/20 p-6 text-center text-brand">
                      <Loader2 size={18} className="mx-auto animate-spin" />
                    </div>
                  ) : partyProducts.length === 0 ? (
                    <div className="rounded-2xl border border-dashed border-zinc-800 bg-black/20 p-6 text-sm text-zinc-500">
                      Nenhum produto vinculado ao evento.
                    </div>
                  ) : (
                    partyProducts.map((product) => (
                      <div key={product.id} className="rounded-2xl border border-zinc-800 bg-black/20 p-4">
                        <div className="flex items-start justify-between gap-3">
                          <div>
                            <p className="font-black text-white">{product.nome}</p>
                            <p className="mt-1 text-xs text-zinc-500">
                              {product.categoria || partyConfig.categoryName} • {formatCurrency(product.preco)} • estoque {product.estoque}
                            </p>
                          </div>
                          <span className="rounded-full border border-brand/40 bg-brand-soft px-3 py-1 text-[10px] font-black uppercase text-brand-accent">
                            Somente evento
                          </span>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>

              <div className="rounded-[1.8rem] border border-zinc-800 bg-zinc-950 p-4 sm:p-5">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <h2 className="text-lg font-black text-white">Pedidos do evento</h2>
                    <p className="mt-1 text-sm text-zinc-500">Aprovação libera QR nas fichas do usuário.</p>
                  </div>
                  <div className="grid grid-cols-2 rounded-2xl border border-zinc-800 bg-black/30 p-1">
                    {(["pendente", "approved"] as const).map((status) => (
                      <button
                        key={status}
                        type="button"
                        onClick={() => setPartyOrdersStatus(status)}
                        className={`rounded-xl px-3 py-2 text-[10px] font-black uppercase ${
                          partyOrdersStatus === status ? "bg-brand text-black" : "text-zinc-400"
                        }`}
                      >
                        {status === "pendente" ? "Pendentes" : "Aprovados"}
                      </button>
                    ))}
                  </div>
                </div>
                <div className="mt-4 space-y-3">
                  {loadingPartyOrders ? (
                    <div className="rounded-2xl border border-zinc-800 bg-black/20 p-6 text-center text-brand">
                      <Loader2 size={18} className="mx-auto animate-spin" />
                    </div>
                  ) : filteredPartyOrders.length === 0 ? (
                    <div className="rounded-2xl border border-dashed border-zinc-800 bg-black/20 p-6 text-sm text-zinc-500">
                      Nenhum pedido nessa lista.
                    </div>
                  ) : (
                    filteredPartyOrders.map((order) => {
                      const reference = getEventPartyOrderReference(order);
                      return (
                        <div key={order.id} className="rounded-2xl border border-zinc-800 bg-black/20 p-4">
                          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                            <div>
                              <p className="font-black text-white">{order.userName}</p>
                              <p className="mt-1 text-xs text-zinc-500">
                                {order.productName} • {order.quantidade} un. • {formatCurrency(order.total || order.price)}
                              </p>
                              <p className="mt-1 font-mono text-[10px] text-zinc-600">{reference.summary}</p>
                            </div>
                            <div className="flex flex-wrap gap-2">
                              {partyOrdersStatus === "pendente" ? (
                                <>
                                  <button type="button" onClick={() => void handlePartyOrderStatus(order, "approved")} className="rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-3 py-2 text-xs font-black uppercase text-emerald-200">
                                    Aprovar
                                  </button>
                                  <button type="button" onClick={() => void handlePartyOrderStatus(order, "rejected")} className="rounded-xl border border-red-500/30 bg-red-500/10 px-3 py-2 text-xs font-black uppercase text-red-200">
                                    Reprovar
                                  </button>
                                </>
                              ) : (
                                <button type="button" onClick={() => void handlePartyOrderStatus(order, "delivered")} className="rounded-xl border border-zinc-700 bg-zinc-900 px-3 py-2 text-xs font-black uppercase text-zinc-200">
                                  Registrar retirada
                                </button>
                              )}
                            </div>
                          </div>
                        </div>
                      );
                    })
                  )}
                </div>
              </div>
            </div>
          </section>
        ) : null}

        {section === "ficha-pagamento" ? (
          <section className="space-y-4">
            <div className="rounded-[1.8rem] border border-zinc-800 bg-zinc-950 p-4 sm:p-5">
              <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                <div>
                  <p className="text-[10px] font-black uppercase tracking-[0.24em] text-zinc-500">Modo vendas</p>
                  <h2 className="mt-2 text-xl font-black text-white">Pagamentos e fichas</h2>
                  <p className="mt-1 text-sm text-zinc-500">
                    Aprove pedidos, crie fichas manuais e acompanhe como cada ficha foi liberada.
                  </p>
                </div>
                <div className="flex flex-wrap gap-2">
                  <Link href={eventPartyAdminHref} className="rounded-xl border border-zinc-700 bg-black/20 px-3 py-2 text-xs font-black uppercase text-zinc-200">
                    Voltar
                  </Link>
                  <button
                    type="button"
                    onClick={() => void loadPartyOrders()}
                    className="rounded-xl border border-zinc-700 bg-black/20 px-3 py-2 text-xs font-black uppercase text-zinc-200"
                  >
                    Atualizar
                  </button>
                </div>
              </div>

              <div className="mt-5 rounded-[1.4rem] border border-zinc-800 bg-black/20 p-4">
                <div className="grid gap-3 lg:grid-cols-[1.2fr_0.8fr_auto]">
                  <div className="flex items-center gap-2 rounded-2xl border border-zinc-700 bg-black/40 px-4 py-3">
                    <Search size={16} className="text-zinc-500" />
                    <input
                      value={partyUserSearch}
                      onChange={(event) => setPartyUserSearch(event.target.value)}
                      className="min-w-0 flex-1 bg-transparent text-sm text-white outline-none placeholder:text-zinc-600"
                      placeholder="Buscar por nome, RA, pulseira ou telefone"
                    />
                  </div>
                  <select
                    value={partyUserTurmaFilter}
                    onChange={(event) => setPartyUserTurmaFilter(event.target.value)}
                    className="admin-dark-select rounded-2xl border border-zinc-700 bg-black/40 px-4 py-3 text-sm text-white outline-none"
                  >
                    <option value="todos">Todas as turmas</option>
                    {partyUserTurmaOptions.map((turma) => (
                      <option key={turma} value={turma}>{turma}</option>
                    ))}
                  </select>
                  <button
                    type="button"
                    onClick={() => void loadPartyUsers()}
                    className="rounded-2xl border border-zinc-700 bg-black/30 px-4 py-3 text-xs font-black uppercase text-zinc-200"
                  >
                    Atualizar alunos
                  </button>
                </div>

                <div className="mt-3 flex flex-wrap gap-2">
                  {PARTY_USER_ALPHA_GROUPS.map((group) => (
                    <button
                      key={group.id}
                      type="button"
                      onClick={() => setPartyUserAlphaFilter(group.id)}
                      className={`rounded-full border px-3 py-2 text-[10px] font-black uppercase ${
                        partyUserAlphaFilter === group.id
                          ? "border-brand bg-brand text-black"
                          : "border-zinc-700 bg-black/30 text-zinc-300"
                      }`}
                    >
                      {group.label}
                    </button>
                  ))}
                </div>

                <div className="mt-4 overflow-hidden rounded-2xl border border-zinc-800">
                  {loadingPartyUsers ? (
                    <div className="p-6 text-center text-brand">
                      <Loader2 size={18} className="mx-auto animate-spin" />
                    </div>
                  ) : paginatedPartyUsers.length === 0 ? (
                    <div className="p-6 text-center text-sm text-zinc-500">
                      Nenhum usuário com check-in encontrado para o filtro atual.
                    </div>
                  ) : (
                    <div className="divide-y divide-zinc-800">
                      {paginatedPartyUsers.map((entry) => (
                        <div key={`${entry.source}:${entry.id}`} className="grid gap-3 p-3 md:grid-cols-[1fr_180px_auto] md:items-center">
                          <div>
                            <p className="font-bold text-white">{entry.nome}</p>
                            <p className="mt-1 text-xs text-zinc-500">
                              {entry.turma || "Sem turma"} • {entry.source === "manual" ? "Pulseira" : "RA"} {entry.ra || entry.numero || "-"}
                            </p>
                          </div>
                          <span className="rounded-full border border-zinc-700 bg-black/30 px-3 py-2 text-center text-[10px] font-black uppercase text-zinc-300">
                            {entry.source === "manual" ? "Cadastro manual" : "App"}
                          </span>
                          <button
                            type="button"
                            onClick={() => handleSelectPartyUser(entry)}
                            className="rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-3 py-2 text-[10px] font-black uppercase text-emerald-200"
                          >
                            Selecionar
                          </button>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                <div className="mt-3">
                  <PaginationControls
                    page={safePartyUserPage}
                    totalPages={totalPartyUserPages}
                    totalItems={filteredPartyUsers.length}
                    pageSize={PARTY_USER_PAGE_SIZE}
                    onPageChange={setPartyUserPage}
                  />
                </div>
              </div>

              <div className="mt-5 grid gap-3 rounded-[1.4rem] border border-zinc-800 bg-black/20 p-4 lg:grid-cols-[1fr_1.2fr_110px_1fr]">
                <select
                  value={manualPartyOrderDraft.productId}
                  onChange={(event) => setManualPartyOrderDraft((previous) => ({ ...previous, productId: event.target.value }))}
                  className="rounded-2xl border border-zinc-700 bg-black/40 px-4 py-3 text-sm text-white outline-none"
                >
                  <option value="">Produto</option>
                  {partyProducts.map((product) => (
                    <option key={product.id} value={product.id}>
                      {product.nome}
                    </option>
                  ))}
                </select>
                <input
                  value={manualPartyOrderDraft.userName}
                  onChange={(event) => setManualPartyOrderDraft((previous) => ({ ...previous, userName: event.target.value.slice(0, 120) }))}
                  className="rounded-2xl border border-zinc-700 bg-black/40 px-4 py-3 text-sm text-white outline-none"
                  placeholder="Nome"
                  maxLength={120}
                />
                <input
                  value={manualPartyOrderDraft.quantidade}
                  onChange={(event) => setManualPartyOrderDraft((previous) => ({ ...previous, quantidade: event.target.value.replace(/\D/g, "").slice(0, 4) }))}
                  className="rounded-2xl border border-zinc-700 bg-black/40 px-4 py-3 text-sm text-white outline-none"
                  placeholder="Qtd"
                  inputMode="numeric"
                  maxLength={4}
                />
                <input
                  value={manualPartyOrderDraft.manualCode}
                  onChange={(event) => setManualPartyOrderDraft((previous) => ({ ...previous, manualCode: event.target.value.slice(0, 80) }))}
                  className="rounded-2xl border border-zinc-700 bg-black/40 px-4 py-3 text-sm text-white outline-none"
                  placeholder="Nº da ficha"
                  maxLength={80}
                />
                <p className="text-xs font-semibold leading-relaxed text-zinc-500 lg:col-span-4">
                  Pesquise e selecione o participante com check-in na lista acima. Ao informar o nº da ficha, a compra manual fica aprovada e aparece na lista de retirada pendente.
                </p>
                <button
                  type="button"
                  onClick={() => void handleCreateManualPartyOrder()}
                  disabled={partyOrderMutatingId === "manual" || !selectedManualPartyProduct}
                  className="brand-button-solid justify-center px-4 py-3 text-xs disabled:opacity-50 lg:col-span-4"
                >
                  {partyOrderMutatingId === "manual" ? <Loader2 size={14} className="animate-spin" /> : <Package size={14} />}
                  Criar manual
                </button>
              </div>
            </div>

            <div className="rounded-[1.8rem] border border-zinc-800 bg-zinc-950 p-4 sm:p-5">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <h2 className="text-lg font-black text-white">Pedidos</h2>
                  <p className="mt-1 text-sm text-zinc-500">Data, usuário, produto, aprovação e origem da ficha.</p>
                </div>
                <div className="grid grid-cols-2 rounded-2xl border border-zinc-800 bg-black/30 p-1">
                  {(["pendente", "approved"] as const).map((status) => (
                    <button
                      key={status}
                      type="button"
                      onClick={() => setPartyOrdersStatus(status)}
                      className={`rounded-xl px-3 py-2 text-[10px] font-black uppercase ${
                        partyOrdersStatus === status ? "bg-brand text-black" : "text-zinc-400"
                      }`}
                    >
                      {status === "pendente" ? "Pendentes" : "Aprovados"}
                    </button>
                  ))}
                </div>
              </div>

              <div className="mt-4 overflow-x-auto rounded-[1.4rem] border border-zinc-800">
                <table className="min-w-[1480px] w-full text-left text-xs">
                  <thead className="bg-black/40 text-[10px] uppercase tracking-[0.16em] text-zinc-500">
                    <tr>
                      <th className="px-4 py-3">Data do pedido do produto</th>
                      <th className="px-4 py-3">Hora do pedido do produto</th>
                      <th className="px-4 py-3">Pedido / ficha / código</th>
                      <th className="px-4 py-3">Usuário</th>
                      <th className="px-4 py-3">Produto</th>
                      <th className="px-4 py-3">Qtd</th>
                      <th className="px-4 py-3">Aprovou pagamento</th>
                      <th className="px-4 py-3">Data da aprovação do pagamento</th>
                      <th className="px-4 py-3">Hora da aprovação do pagamento</th>
                      <th className="px-4 py-3">Origem</th>
                      <th className="px-4 py-3">Criado por</th>
                      <th className="px-4 py-3 text-right">Ações</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-zinc-800">
                    {loadingPartyOrders ? (
                      <tr>
                        <td colSpan={12} className="px-4 py-8 text-center text-brand">
                          <Loader2 size={18} className="mx-auto animate-spin" />
                        </td>
                      </tr>
                    ) : filteredPartyOrders.length === 0 ? (
                      <tr>
                        <td colSpan={12} className="px-4 py-8 text-center text-zinc-500">
                          Nenhum pedido nessa lista.
                        </td>
                      </tr>
                    ) : (
                      filteredPartyOrders.map((order) => {
                        const orderPartyData = asRecord(order.data.eventParty) ?? {};
                        const manualOrder = asBoolean(orderPartyData.manualOrder) || asBoolean(orderPartyData.createdManually);
                        const usedBy = asString(orderPartyData.usedByUserName);
                        const approvalMethod = usedBy ? "QR" : asString(orderPartyData.approvalMethod) || (order.approvedBy ? "Manual" : "-");
                        const approvedAt = asString(orderPartyData.approvedAt) || order.updatedAt;
                        const reference = getEventPartyOrderReference(order);
                        return (
                          <tr key={order.id} className="align-top text-zinc-300">
                            <td className="px-4 py-3 text-zinc-400">{formatDateOnly(order.createdAt)}</td>
                            <td className="px-4 py-3 text-zinc-400">{formatTimeOnly(order.createdAt)}</td>
                            <td className="px-4 py-3">
                              <p className="font-mono text-[10px] text-zinc-300">{reference.summary}</p>
                              <p className="mt-1 font-mono text-[10px] text-zinc-600">{order.id}</p>
                            </td>
                            <td className="px-4 py-3 font-bold text-white">{order.userName}</td>
                            <td className="px-4 py-3">
                              <p className="font-bold text-white">{order.productName}</p>
                            </td>
                            <td className="px-4 py-3">{order.quantidade}</td>
                            <td className="px-4 py-3">{order.approvedBy || "-"}</td>
                            <td className="px-4 py-3">{formatDateOnly(approvedAt)}</td>
                            <td className="px-4 py-3">{formatTimeOnly(approvedAt)}</td>
                            <td className="px-4 py-3">
                              <span className="rounded-full border border-zinc-700 bg-black/30 px-2 py-1 text-[10px] font-black uppercase text-zinc-300">
                                {manualOrder ? "Manual" : approvalMethod}
                              </span>
                            </td>
                            <td className="px-4 py-3">{asString(orderPartyData.createdByName) || "-"}</td>
                            <td className="px-4 py-3">
                              <div className="flex justify-end gap-2">
                                {partyOrdersStatus === "pendente" ? (
                                  <button
                                    type="button"
                                    onClick={() => void handlePartyOrderStatus(order, "approved")}
                                    disabled={partyOrderMutatingId === order.id}
                                    className="rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-3 py-2 text-[10px] font-black uppercase text-emerald-200 disabled:opacity-50"
                                  >
                                    Aprovar
                                  </button>
                                ) : null}
                                <button
                                  type="button"
                                  onClick={() => void handleEditPartyOrder(order)}
                                  disabled={partyOrderMutatingId === order.id}
                                  className="rounded-xl border border-zinc-700 bg-black/30 px-3 py-2 text-[10px] font-black uppercase text-zinc-200 disabled:opacity-50"
                                >
                                  Editar
                                </button>
                                <button
                                  type="button"
                                  onClick={() => void handleDeletePartyOrder(order)}
                                  disabled={partyOrderMutatingId === order.id}
                                  className="rounded-xl border border-red-500/30 bg-red-500/10 px-3 py-2 text-[10px] font-black uppercase text-red-200 disabled:opacity-50"
                                >
                                  Excluir
                                </button>
                              </div>
                            </td>
                          </tr>
                        );
                      })
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </section>
        ) : null}

        {section === "ficha-cadastro" ? (
          <section className="space-y-4">
            <div className="rounded-[1.8rem] border border-zinc-800 bg-zinc-950 p-4 sm:p-5">
              <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                <div>
                  <p className="text-[10px] font-black uppercase tracking-[0.24em] text-zinc-500">Modo vendas</p>
                  <h2 className="mt-2 text-xl font-black text-white">Cadastro de usuário</h2>
                  <p className="mt-1 text-sm text-zinc-500">
                    Registre pessoas sem app ou sem vínculo com a faculdade para emissão manual de fichas.
                  </p>
                </div>
                <Link href={eventPartyAdminHref} className="rounded-xl border border-zinc-700 bg-black/20 px-3 py-2 text-center text-xs font-black uppercase text-zinc-200">
                  Voltar
                </Link>
              </div>

              <div className="mt-5 grid gap-3 rounded-[1.4rem] border border-zinc-800 bg-black/20 p-4 md:grid-cols-2">
                <input
                  value={partyUserCadastroDraft.nome}
                  onChange={(event) => setPartyUserCadastroDraft((previous) => ({ ...previous, nome: event.target.value.slice(0, 120) }))}
                  className="rounded-2xl border border-zinc-700 bg-black/40 px-4 py-3 text-sm text-white outline-none"
                  placeholder="Nome"
                  maxLength={120}
                />
                <input
                  value={partyUserCadastroDraft.numero}
                  onChange={(event) => setPartyUserCadastroDraft((previous) => ({ ...previous, numero: event.target.value.slice(0, 80) }))}
                  className="rounded-2xl border border-zinc-700 bg-black/40 px-4 py-3 text-sm text-white outline-none"
                  placeholder="Nº da pulseira"
                  maxLength={80}
                />
                <input
                  value={partyUserCadastroDraft.cpf}
                  onChange={(event) => setPartyUserCadastroDraft((previous) => ({ ...previous, cpf: formatCpfInput(event.target.value) }))}
                  className="rounded-2xl border border-zinc-700 bg-black/40 px-4 py-3 text-sm text-white outline-none"
                  placeholder="CPF XXX.XXX.XXX-XX"
                  inputMode="numeric"
                  maxLength={CPF_MASKED_MAX_LENGTH}
                />
                <input
                  value={partyUserCadastroDraft.telefone}
                  onChange={(event) => setPartyUserCadastroDraft((previous) => ({ ...previous, telefone: formatBrazilPhoneInput(event.target.value) }))}
                  className="rounded-2xl border border-zinc-700 bg-black/40 px-4 py-3 text-sm text-white outline-none"
                  placeholder="Telefone +55XXXXXXXXXXX"
                  inputMode="tel"
                  maxLength={PHONE_MAX_LENGTH}
                />
                <input
                  value={partyUserCadastroDraft.email}
                  onChange={(event) => setPartyUserCadastroDraft((previous) => ({ ...previous, email: formatEmailCadastroInput(event.target.value) }))}
                  className="rounded-2xl border border-zinc-700 bg-black/40 px-4 py-3 text-sm text-white outline-none"
                  placeholder="E-mail usuario@dominio.com"
                  type="email"
                  maxLength={160}
                />
                <input
                  value={partyUserCadastroDraft.ra}
                  onChange={(event) => setPartyUserCadastroDraft((previous) => ({ ...previous, ra: event.target.value.slice(0, 80) }))}
                  className="rounded-2xl border border-zinc-700 bg-black/40 px-4 py-3 text-sm text-white outline-none"
                  placeholder="RA (opcional)"
                  maxLength={80}
                />
                <input
                  value={partyUserCadastroDraft.turma}
                  onChange={(event) => setPartyUserCadastroDraft((previous) => ({ ...previous, turma: event.target.value.slice(0, 120) }))}
                  className="rounded-2xl border border-zinc-700 bg-black/40 px-4 py-3 text-sm text-white outline-none"
                  placeholder="Turma (opcional)"
                  maxLength={120}
                />
                <input
                  value={partyUserCadastroDraft.valorPorta}
                  onChange={(event) => setPartyUserCadastroDraft((previous) => ({ ...previous, valorPorta: event.target.value.slice(0, 20) }))}
                  onBlur={() => setPartyUserCadastroDraft((previous) => ({ ...previous, valorPorta: formatCurrencyTextInput(previous.valorPorta) }))}
                  className="rounded-2xl border border-zinc-700 bg-black/40 px-4 py-3 text-sm text-white outline-none"
                  placeholder="Valor entrada/porta (R$)"
                  inputMode="decimal"
                  maxLength={20}
                />
                <button
                  type="button"
                  onClick={() => void handleSavePartyManualUser()}
                  disabled={savingEvento}
                  className="brand-button-solid justify-center px-4 py-3 text-xs disabled:opacity-50 md:col-span-2"
                >
                  {savingEvento ? <Loader2 size={14} className="animate-spin" /> : <Users size={14} />}
                  {editingManualPartyUserId ? "Salvar cadastro" : "Cadastrar usuário"}
                </button>
                {editingManualPartyUserId ? (
                  <button
                    type="button"
                    onClick={handleCancelManualPartyUserEdit}
                    disabled={savingEvento}
                    className="rounded-2xl border border-zinc-700 bg-black/30 px-4 py-3 text-xs font-black uppercase text-zinc-200 disabled:opacity-50 md:col-span-2"
                  >
                    Cancelar edição
                  </button>
                ) : null}
              </div>
            </div>

            <div className="rounded-[1.8rem] border border-zinc-800 bg-zinc-950 p-4 sm:p-5">
              <h3 className="text-lg font-black text-white">Cadastros manuais</h3>
              <div className="mt-4 space-y-3">
                {manualRegisteredUsers.length === 0 ? (
                  <div className="rounded-2xl border border-dashed border-zinc-800 bg-black/20 p-6 text-sm text-zinc-500">
                    Nenhum usuário manual cadastrado para este evento.
                  </div>
                ) : (
                  manualRegisteredUsers.map((entry) => (
                    <div key={entry.orderId || entry.id} className="grid gap-3 rounded-2xl border border-zinc-800 bg-black/20 p-4 md:grid-cols-[1fr_280px] md:items-center">
                      <div>
                        <p className="font-bold text-white">{entry.nome}</p>
                        <p className="mt-1 text-xs text-zinc-500">
                          Pulseira {entry.numero} • Entrada/porta R$ {entry.valorPorta || "0,00"} • RA {entry.ra || "-"} • CPF {formatCpfInput(entry.cpf) || "-"} • {formatManualContactSummary(entry)}
                        </p>
                      </div>
                      <div className="flex flex-wrap items-center justify-end gap-2">
                        <span className="rounded-full border border-zinc-700 bg-black/30 px-3 py-2 text-center text-[10px] font-black uppercase text-zinc-300">
                          {entry.turma || "Sem turma"}
                        </span>
                        <button
                          type="button"
                          onClick={() => handleEditManualPartyUser(entry)}
                          className="rounded-full border border-zinc-700 bg-black/30 px-3 py-2 text-[10px] font-black uppercase text-zinc-200"
                        >
                          Editar
                        </button>
                        <button
                          type="button"
                          onClick={() => void handleDeleteManualPartyUser(entry)}
                          className="rounded-full border border-red-500/30 bg-red-500/10 px-3 py-2 text-[10px] font-black uppercase text-red-200"
                        >
                          Excluir
                        </button>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          </section>
        ) : null}

        {section === "ficha-produto" || section === "ficha-produtos-cadastro" || (section === "ficha-produtos" && editingPartyProductId) ? (
          <section className="grid gap-4 lg:grid-cols-[0.9fr_1.1fr]">
            <div className="rounded-[1.8rem] border border-zinc-800 bg-zinc-950 p-4 sm:p-5">
              <p className="text-[10px] font-black uppercase tracking-[0.24em] text-zinc-500">
                {editingPartyProductId ? "Editar produto" : "Novo produto"}
              </p>
              <h2 className="mt-2 text-xl font-black text-white">Cadastro do menu</h2>
              <p className="mt-1 text-sm text-zinc-500">
                O produto fica salvo na tenant e pode ser reutilizado em outros eventos depois.
              </p>

              <input
                ref={partyProductImageInputRef}
                type="file"
                accept="image/png,image/jpeg,image/webp"
                className="hidden"
                onChange={(event) => {
                  const file = event.target.files?.[0];
                  if (file) void handlePartyProductImageUpload(file);
                  event.target.value = "";
                }}
              />

              <button
                type="button"
                onClick={() => partyProductImageInputRef.current?.click()}
                className="mt-5 flex aspect-[4/3] w-full items-center justify-center overflow-hidden rounded-[1.4rem] border border-dashed border-zinc-700 bg-black/30 text-zinc-500 transition hover:border-brand"
              >
                {uploadingPartyProductImage ? (
                  <Loader2 size={22} className="animate-spin text-brand" />
                ) : partyProductDraft.img ? (
                  <Image src={partyProductDraft.img} alt="Foto do produto" width={900} height={675} className="h-full w-full object-cover" />
                ) : (
                  <span className="text-center text-xs font-black uppercase">
                    <ImageIcon className="mx-auto mb-2" />
                    Enviar foto
                  </span>
                )}
              </button>
              <ImageResizeHelpLink label="Compacte a foto no Squoosh.app antes do upload. Tamanho máximo final: 200 KB." className="mt-3" />
            </div>

            <div className="rounded-[1.8rem] border border-zinc-800 bg-zinc-950 p-4 sm:p-5">
              <div className="grid gap-3">
                <input
                  value={partyProductDraft.nome}
                  onChange={(event) => setPartyProductDraft((previous) => ({ ...previous, nome: event.target.value.slice(0, 120) }))}
                  className="rounded-2xl border border-zinc-700 bg-black/30 px-4 py-3 text-sm outline-none"
                  placeholder="Nome do produto"
                  maxLength={120}
                />
                <div className="grid gap-3 sm:grid-cols-2">
                  <input
                    value={partyProductDraft.preco}
                    onChange={(event) => setPartyProductDraft((previous) => ({ ...previous, preco: event.target.value.slice(0, 40) }))}
                    className="rounded-2xl border border-zinc-700 bg-black/30 px-4 py-3 text-sm outline-none"
                    placeholder="Preço (R$)"
                    inputMode="decimal"
                    maxLength={40}
                  />
                  <input
                    value={partyProductDraft.estoque}
                    onChange={(event) => setPartyProductDraft((previous) => ({ ...previous, estoque: event.target.value.replace(/\D/g, "").slice(0, 6) }))}
                    className="rounded-2xl border border-zinc-700 bg-black/30 px-4 py-3 text-sm outline-none"
                    placeholder="Estoque"
                    inputMode="numeric"
                    maxLength={6}
                  />
                </div>
                <div className="grid gap-3 lg:grid-cols-[1fr_1fr_auto]">
                  <select
                    value={partyProductDraft.secao || partyProductDraft.categoria || partySectionOptions[0] || "Geral"}
                    onChange={(event) => {
                      const sectionName = event.target.value.slice(0, 80);
                      setPartyProductDraft((previous) => ({ ...previous, categoria: sectionName, secao: sectionName }));
                    }}
                    className="admin-dark-select rounded-2xl border border-zinc-700 bg-black/30 px-4 py-3 text-sm outline-none"
                  >
                    {partySectionOptions.map((sectionName) => (
                      <option key={sectionName} value={sectionName}>{sectionName}</option>
                    ))}
                  </select>
                  <input
                    value={partyNewSectionName}
                    onChange={(event) => setPartyNewSectionName(event.target.value.slice(0, 80))}
                    className="rounded-2xl border border-zinc-700 bg-black/30 px-4 py-3 text-sm outline-none"
                    placeholder="Nova seção"
                    maxLength={80}
                  />
                  <button
                    type="button"
                    onClick={() => {
                      const sectionName = partyNewSectionName.trim().slice(0, 80);
                      if (!sectionName) return;
                      setPartyProductDraft((previous) => ({ ...previous, categoria: sectionName, secao: sectionName }));
                      setPartyNewSectionName("");
                    }}
                    className="rounded-2xl border border-brand bg-brand-soft px-4 py-3 text-xs font-black uppercase text-brand-accent"
                  >
                    Criar seção
                  </button>
                </div>
                <textarea
                  value={partyProductDraft.descricao}
                  onChange={(event) => setPartyProductDraft((previous) => ({ ...previous, descricao: event.target.value.slice(0, 500) }))}
                  className="min-h-32 rounded-2xl border border-zinc-700 bg-black/30 px-4 py-3 text-sm outline-none"
                  placeholder="Descrição rápida"
                  maxLength={500}
                />
                <div className="flex flex-col gap-3 sm:flex-row sm:justify-end">
                  <Link href={eventPartyProductsAdminHref} className="rounded-2xl border border-zinc-700 bg-black/20 px-4 py-3 text-center text-xs font-black uppercase text-zinc-200">
                    Ver produtos
                  </Link>
                  {editingPartyProductId ? (
                    <button
                      type="button"
                      onClick={handleClearPartyProductDraft}
                      className="rounded-2xl border border-zinc-700 bg-black/20 px-4 py-3 text-center text-xs font-black uppercase text-zinc-200"
                    >
                      Cancelar edição
                    </button>
                  ) : null}
                  <button
                    type="button"
                    onClick={() => void handleCreatePartyProduct()}
                    className="brand-button-solid justify-center px-4 py-3 text-xs"
                  >
                    <Package size={14} />
                    {editingPartyProductId ? "Atualizar produto" : "Salvar produto"}
                  </button>
                </div>
              </div>
            </div>
          </section>
        ) : null}

        {section === "ficha-produtos" ? (
          <section className="space-y-4">
            <div className="rounded-[1.8rem] border border-zinc-800 bg-zinc-950 p-4 sm:p-5">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div>
                  <p className="text-[10px] font-black uppercase tracking-[0.24em] text-zinc-500">Menu</p>
                  <h2 className="mt-2 text-xl font-black text-white">Produtos do evento</h2>
                  <p className="mt-1 text-sm text-zinc-500">Arraste os produtos para ordenar e subdivida o menu em seções.</p>
                </div>
                <div className="flex flex-wrap gap-2">
                  <Link href={eventPartyProductCadastroHref} className="brand-button-solid px-4 py-3 text-xs">
                    <Package size={14} />
                    Novo produto
                  </Link>
                  <button
                    type="button"
                    onClick={() => void loadPartyProducts()}
                    className="rounded-2xl border border-zinc-700 bg-black/20 px-4 py-3 text-xs font-black uppercase text-zinc-200"
                  >
                    Atualizar
                  </button>
                </div>
              </div>
            </div>

            {loadingPartyProducts ? (
              <div className="rounded-[1.8rem] border border-zinc-800 bg-zinc-950 p-8 text-center text-brand">
                <Loader2 size={22} className="mx-auto animate-spin" />
              </div>
            ) : groupedPartyProducts.length === 0 ? (
              <div className="rounded-[1.8rem] border border-dashed border-zinc-800 bg-zinc-950 p-8 text-center text-sm text-zinc-500">
                Nenhum produto vinculado ao evento.
              </div>
            ) : (
              groupedPartyProducts.map((group) => (
                <div key={group.section} className="rounded-[1.8rem] border border-zinc-800 bg-zinc-950 p-4 sm:p-5">
                  <h3 className="text-lg font-black text-white">{group.section}</h3>
                  <div className="mt-4 space-y-3">
                    {group.products.map((product) => {
                      const draft = partyProductMetaDrafts[product.id] || {
                        section: getEventPartyProductSection(product),
                        order: String(getEventPartyProductOrder(product)),
                      };
                      return (
                        <div
                          key={product.id}
                          draggable
                          onDragStart={() => setDraggingPartyProductId(product.id)}
                          onDragOver={(event) => event.preventDefault()}
                          onDrop={() => void handleReorderPartyProducts(product, group.products)}
                          onDragEnd={() => setDraggingPartyProductId("")}
                          className={`grid gap-3 rounded-[1.4rem] border bg-black/20 p-3 transition lg:grid-cols-[36px_88px_1fr_200px_auto] lg:items-center ${
                            draggingPartyProductId === product.id
                              ? "border-brand opacity-60"
                              : "border-zinc-800 hover:border-zinc-700"
                          }`}
                        >
                          <button
                            type="button"
                            className="flex h-10 w-10 cursor-grab items-center justify-center rounded-xl border border-zinc-800 bg-black/30 text-zinc-500 active:cursor-grabbing"
                            aria-label="Arrastar produto"
                          >
                            <MoveVertical size={16} />
                          </button>
                          <div className="relative h-20 w-20 overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900">
                            {product.img ? (
                              <Image src={product.img} alt={product.nome} fill sizes="80px" className="object-cover" />
                            ) : (
                              <div className="flex h-full items-center justify-center text-zinc-600">
                                <ImageIcon size={18} />
                              </div>
                            )}
                          </div>
                          <div>
                            <p className="font-black text-white">{product.nome}</p>
                            <p className="mt-1 text-xs text-zinc-500">
                              {formatCurrency(product.preco)} • estoque {product.estoque}
                            </p>
                          </div>
                          <input
                            value={draft.section}
                            onChange={(event) =>
                              setPartyProductMetaDrafts((previous) => ({
                                ...previous,
                                [product.id]: { ...draft, section: event.target.value.slice(0, 80) },
                              }))
                            }
                            className="rounded-xl border border-zinc-700 bg-black/40 px-3 py-2 text-xs text-white outline-none"
                            placeholder="Seção"
                            maxLength={80}
                          />
                          <div className="flex flex-wrap justify-end gap-2">
                            <button
                              type="button"
                              onClick={() => handleEditPartyProduct(product)}
                              className="rounded-xl border border-sky-500/30 bg-sky-500/10 px-3 py-2 text-[10px] font-black uppercase text-sky-200"
                            >
                              Editar
                            </button>
                            <button
                              type="button"
                              onClick={() => void handleSavePartyProductMeta(product)}
                              disabled={savingPartyProductMetaId === product.id || savingPartyProductMetaId.startsWith("order:")}
                              className="rounded-xl border border-brand bg-brand-soft px-3 py-2 text-[10px] font-black uppercase text-brand-accent disabled:opacity-50"
                            >
                              {savingPartyProductMetaId === product.id ? "Salvando" : "Salvar"}
                            </button>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              ))
            )}
          </section>
        ) : null}

        {section === "edicao" ? (
          <section className="grid gap-4 xl:grid-cols-[1.05fr_0.95fr]">
            <div className="space-y-4">
              <div className="rounded-[1.8rem] border border-zinc-800 bg-zinc-950 p-4 sm:p-5">
                <div className="flex items-center justify-between gap-3">
                  <div>
                    <p className="text-[10px] font-black uppercase tracking-[0.24em] text-zinc-500">Capa do evento</p>
                    <h2 className="mt-2 text-xl font-black text-white">Imagem e posicionamento</h2>
                  </div>
                  <button
                    type="button"
                    onClick={() => fileInputRef.current?.click()}
                    className="rounded-xl border border-zinc-700 bg-black/20 px-4 py-3 text-xs font-black uppercase text-zinc-200"
                  >
                    Trocar imagem
                  </button>
                </div>

                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/png,image/jpeg,image/webp"
                  className="hidden"
                  onChange={(event) => {
                    const file = event.target.files?.[0];
                    if (file) {
                      void handleImageUpload(file);
                    }
                    event.target.value = "";
                  }}
                />

                <div className="mt-4 overflow-hidden rounded-[1.6rem] border border-zinc-800 bg-black">
                  <div className="relative h-56">
                    {uploadingImage ? (
                      <div className="flex h-full items-center justify-center text-brand">
                        <Loader2 size={22} className="animate-spin" />
                      </div>
                    ) : editDraft.imagem ? (
                      <Image
                        src={editDraft.imagem}
                        alt={editDraft.titulo}
                        fill
                        sizes="(max-width: 1280px) 100vw, 640px"
                        className="object-cover"
                        style={{ objectPosition: `50% ${editDraft.imagePositionY}%` }}
                      />
                    ) : (
                      <div className="flex h-full items-center justify-center text-zinc-600">
                        <ImageIcon size={30} />
                      </div>
                    )}
                  </div>
                  <div className="space-y-3 border-t border-zinc-800 p-4">
                    <ImageResizeHelpLink label="Diminuir a imagem do evento no Squoosh.app" />
                    <div className="rounded-2xl border border-zinc-800 bg-zinc-950/60 p-3">
                      <div className="mb-2 flex items-center justify-between text-[10px] font-black uppercase tracking-[0.16em] text-zinc-500">
                        <span className="inline-flex items-center gap-1">
                          <MoveVertical size={12} />
                          Ajuste fino
                        </span>
                        <span>{editDraft.imagePositionY}%</span>
                      </div>
                      <input
                        type="range"
                        min="0"
                        max="100"
                        value={editDraft.imagePositionY}
                        onChange={(event) =>
                          setEditDraft((previous) =>
                            previous ? { ...previous, imagePositionY: Number(event.target.value) } : previous
                          )
                        }
                        className="w-full accent-emerald-500"
                      />
                    </div>
                  </div>
                </div>
              </div>

              <div className="rounded-[1.8rem] border border-zinc-800 bg-zinc-950 p-4 sm:p-5">
                <p className="text-[10px] font-black uppercase tracking-[0.24em] text-zinc-500">Dados principais</p>
                <h2 className="mt-2 text-xl font-black text-white">Informações do evento</h2>

                <div className="mt-5 grid gap-3">
                  <input
                    value={editDraft.titulo}
                    onChange={(event) =>
                      setEditDraft((previous) =>
                        previous
                          ? {
                              ...previous,
                              titulo: event.target.value.slice(0, EVENT_TITLE_MAX_LENGTH),
                            }
                          : previous
                      )
                    }
                    placeholder="Nome do evento"
                    className="rounded-2xl border border-zinc-700 bg-black/30 px-4 py-3 text-sm outline-none placeholder:text-zinc-600"
                  />
                  <div className="rounded-2xl border border-zinc-800 bg-black/20 p-3">
                    <label className="text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">
                      Slug da página pública
                    </label>
                    <div className="mt-2 grid gap-2 sm:grid-cols-[1fr_auto]">
                      <input
                        value={asString(editDraft.dataExtra.raw.publicSlug)}
                        onChange={(event) =>
                          setEditDraft((previous) =>
                            previous
                              ? {
                                  ...previous,
                                  dataExtra: {
                                    ...previous.dataExtra,
                                    raw: {
                                      ...previous.dataExtra.raw,
                                      publicSlug: normalizePublicEventSlug(event.target.value),
                                    },
                                  },
                                }
                              : previous
                          )
                        }
                        placeholder={editDraft.id}
                        maxLength={EVENT_PUBLIC_SLUG_MAX_LENGTH}
                        className="rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-3 text-sm outline-none placeholder:text-zinc-600"
                      />
                      <button
                        type="button"
                        onClick={() => void handleSavePublicSlug()}
                        disabled={savingEvento}
                        className="rounded-xl border border-brand bg-brand-soft px-4 py-3 text-xs font-black uppercase text-brand-accent"
                      >
                        Alterar slug
                      </button>
                    </div>
                    <p className="mt-2 text-[11px] text-zinc-500">
                      Máximo de 20 caracteres. Exemplo: /eventos/{asString(editDraft.dataExtra.raw.publicSlug) || editDraft.id}
                    </p>
                  </div>
                  <div className="grid gap-3 sm:grid-cols-2">
                    <input
                      type="date"
                      value={editDraft.data}
                      onChange={(event) =>
                        setEditDraft((previous) =>
                          previous ? { ...previous, data: event.target.value } : previous
                        )
                      }
                      className="admin-dark-select rounded-2xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm text-zinc-100 outline-none"
                    />
                    <input
                      type="time"
                      value={editDraft.hora}
                      onChange={(event) =>
                        setEditDraft((previous) =>
                          previous ? { ...previous, hora: event.target.value } : previous
                        )
                      }
                      className="admin-dark-select rounded-2xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm text-zinc-100 outline-none"
                    />
                  </div>
                  <div className="grid gap-3 sm:grid-cols-2">
                    <select
                      value={editDraft.tipo}
                      onChange={(event) =>
                        setEditDraft((previous) =>
                          previous
                            ? {
                                ...previous,
                                tipo: event.target.value.slice(0, EVENT_TYPE_MAX_LENGTH),
                              }
                            : previous
                        )
                      }
                      className="admin-dark-select rounded-2xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm text-zinc-100 outline-none"
                    >
                      <option value="Festa">Festa</option>
                      <option value="Esporte">Esporte</option>
                      <option value="Outro">Outro</option>
                    </select>
                    <input
                      value={editDraft.local}
                      onChange={(event) =>
                        setEditDraft((previous) =>
                          previous
                            ? {
                                ...previous,
                                local: event.target.value.slice(0, EVENT_LOCATION_MAX_LENGTH),
                              }
                            : previous
                        )
                      }
                      placeholder="Local"
                      className="rounded-2xl border border-zinc-700 bg-black/30 px-4 py-3 text-sm outline-none placeholder:text-zinc-600"
                    />
                  </div>
                  <div className="rounded-2xl border border-zinc-800 bg-black/20 p-3">
                    <label className="text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">
                      Capacidade total da portaria
                    </label>
                    <input
                      type="number"
                      min="0"
                      step="1"
                      value={String(editDraft.dataExtra.raw.capacity ?? editDraft.dataExtra.raw.capacidade ?? "")}
                      onChange={(event) =>
                        setEditDraft((previous) => {
                          if (!previous) return previous;
                          const capacity = Math.max(0, Math.floor(Number(event.target.value) || 0));
                          const nextRaw = { ...previous.dataExtra.raw };
                          if (capacity > 0) {
                            nextRaw.capacity = capacity;
                            nextRaw.capacidade = capacity;
                          } else {
                            delete nextRaw.capacity;
                            delete nextRaw.capacidade;
                          }
                          return {
                            ...previous,
                            dataExtra: {
                              ...previous.dataExtra,
                              raw: nextRaw,
                            },
                          };
                        })
                      }
                      placeholder="Ex.: 300"
                      className="mt-2 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-3 text-sm text-zinc-100 outline-none placeholder:text-zinc-600"
                    />
                    <p className="mt-2 text-[11px] text-zinc-500">
                      Opcional. O BI Portaria usa esse valor para ocupação e capacidade restante em tempo real.
                    </p>
                  </div>
                  <textarea
                    value={editDraft.descricao}
                    onChange={(event) =>
                      setEditDraft((previous) =>
                        previous
                          ? {
                              ...previous,
                              descricao: event.target.value.slice(0, EVENT_DESCRIPTION_MAX_LENGTH),
                            }
                          : previous
                      )
                    }
                    placeholder="Descrição completa"
                    className="min-h-40 rounded-2xl border border-zinc-700 bg-black/30 px-4 py-3 text-sm outline-none placeholder:text-zinc-600"
                  />
                </div>
              </div>
            </div>

            <div className="space-y-4">
              <div className="rounded-[1.8rem] border border-zinc-800 bg-zinc-950 p-4 sm:p-5">
                <div className="flex items-center gap-2">
                  <Wallet size={16} className="text-brand" />
                  <div>
                    <h2 className="text-lg font-black text-white">Financeiro & recebimento</h2>
                    <p className="text-sm text-zinc-500">Substitua a conta global apenas neste evento.</p>
                  </div>
                </div>

                <div className="mt-5 grid gap-3">
                  <input
                    value={editDraft.pixChave}
                    onChange={(event) =>
                      setEditDraft((previous) =>
                        previous
                          ? {
                              ...previous,
                              pixChave: event.target.value.slice(0, EVENT_PIX_FIELD_MAX_LENGTH),
                            }
                          : previous
                      )
                    }
                    placeholder="Chave PIX"
                    className="rounded-2xl border border-zinc-700 bg-black/30 px-4 py-3 text-sm outline-none placeholder:text-zinc-600"
                  />
                  <div className="grid gap-3 sm:grid-cols-2">
                    <input
                      value={editDraft.pixBanco}
                      onChange={(event) =>
                        setEditDraft((previous) =>
                          previous
                            ? {
                                ...previous,
                                pixBanco: event.target.value.slice(0, EVENT_PIX_FIELD_MAX_LENGTH),
                              }
                            : previous
                        )
                      }
                      placeholder="Banco"
                      className="rounded-2xl border border-zinc-700 bg-black/30 px-4 py-3 text-sm outline-none placeholder:text-zinc-600"
                    />
                    <input
                      value={editDraft.pixTitular}
                      onChange={(event) =>
                        setEditDraft((previous) =>
                          previous
                            ? {
                                ...previous,
                                pixTitular: event.target.value.slice(0, EVENT_PIX_FIELD_MAX_LENGTH),
                              }
                            : previous
                        )
                      }
                      placeholder="Nome do titular"
                      className="rounded-2xl border border-zinc-700 bg-black/30 px-4 py-3 text-sm outline-none placeholder:text-zinc-600"
                    />
                  </div>
                  <input
                    value={editDraft.contatoComprovante}
                    onChange={(event) =>
                      setEditDraft((previous) =>
                        previous
                          ? {
                              ...previous,
                              contatoComprovante: normalizePhoneToBrE164(event.target.value),
                            }
                          : previous
                      )
                    }
                    placeholder="WhatsApp para comprovante"
                    className="rounded-2xl border border-zinc-700 bg-black/30 px-4 py-3 text-sm outline-none placeholder:text-zinc-600"
                  />
                </div>
              </div>

              <div className="rounded-[1.8rem] border border-zinc-800 bg-zinc-950 p-4 sm:p-5">
                <h2 className="text-lg font-black text-white">Status de venda</h2>
                <p className="text-sm text-zinc-500">Controle se o evento está ativo, em breve ou esgotado.</p>
                <div className="mt-4 grid grid-cols-3 gap-2">
                  {(["ativo", "em_breve", "esgotado"] as EventSaleStatus[]).map((status) => (
                    <button
                      key={status}
                      type="button"
                      onClick={() =>
                        setEditDraft((previous) => (previous ? { ...previous, saleStatus: status } : previous))
                      }
                      className={`rounded-xl border px-3 py-3 text-[11px] font-black uppercase ${
                        editDraft.saleStatus === status
                          ? saleStatusTone[status]
                          : "border-zinc-700 bg-black/20 text-zinc-400"
                      }`}
                    >
                      {status === "ativo" ? "Ativar" : status === "em_breve" ? "Em breve" : "Esgotado"}
                    </button>
                  ))}
                </div>
              </div>

              <div className="rounded-[1.8rem] border border-zinc-800 bg-zinc-950 p-4 sm:p-5">
                <h2 className="text-lg font-black text-white">Atalhos do evento</h2>
                <div className="mt-4 grid gap-3 sm:grid-cols-2">
                  <Link
                    href={eventSectionHref("lotes")}
                    className="rounded-[1.4rem] border border-zinc-800 bg-black/20 p-4 transition hover:border-brand"
                  >
                    <p className="text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">Lotes</p>
                    <p className="mt-2 font-black text-white">{evento.lotes.length} configurados</p>
                    <span className="mt-3 inline-flex items-center gap-1 text-sm text-brand-accent">
                      Abrir página
                      <ChevronRight size={14} />
                    </span>
                  </Link>
                  <Link
                    href={eventSectionHref("recebedores")}
                    className="rounded-[1.4rem] border border-zinc-800 bg-black/20 p-4 transition hover:border-brand"
                  >
                    <p className="text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">Recebedores</p>
                    <p className="mt-2 font-black text-white">{editDraft.recipientUserIds.length} selecionados</p>
                    <span className="mt-3 inline-flex items-center gap-1 text-sm text-brand-accent">
                      Abrir página
                      <ChevronRight size={14} />
                    </span>
                  </Link>
                  <Link
                    href={eventSectionHref("ficha")}
                    className="rounded-[1.4rem] border border-zinc-800 bg-black/20 p-4 transition hover:border-brand"
                  >
                    <p className="text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">Modo Vendas</p>
                    <p className="mt-2 font-black text-white">{normalizeEventPartyConfig(editDraft.dataExtra.raw).enabled ? "Ativo" : "Inativo"}</p>
                    <span className="mt-3 inline-flex items-center gap-1 text-sm text-brand-accent">
                      Abrir página
                      <ChevronRight size={14} />
                    </span>
                  </Link>
                </div>
              </div>

              <div className="flex flex-col gap-3 sm:flex-row sm:justify-end">
                <button
                  type="button"
                  onClick={() => setEditDraft(cloneEvent(evento))}
                  className="rounded-2xl border border-zinc-700 bg-black/20 px-4 py-3 text-xs font-black uppercase text-zinc-300"
                >
                  Recarregar rascunho
                </button>
                <button
                  ref={saveEditButtonRef}
                  type="button"
                  onClick={() => void handleSaveEdit()}
                  disabled={savingEvento}
                  className="brand-button-soft"
                >
                  {savingEvento ? <Loader2 size={14} className="animate-spin" /> : <Check size={14} />}
                  Atualizar evento
                </button>
              </div>
              <FloatingSaveButton
                watchRef={saveEditButtonRef}
                label="Atualizar evento"
                icon={<Save size={14} />}
                disabled={savingEvento}
                onClick={() => void handleSaveEdit()}
              />
            </div>
          </section>
        ) : null}

        {section === "enquetes" ? (
          <section className="grid gap-4 xl:grid-cols-[0.95fr_1.05fr]">
            <div className="rounded-[1.8rem] border border-zinc-800 bg-zinc-950 p-4 sm:p-5">
              <p className="text-[10px] font-black uppercase tracking-[0.24em] text-zinc-500">Nova enquete</p>
              <h2 className="mt-2 text-xl font-black text-white">Crie uma enquete do evento</h2>
              <div className="mt-5 space-y-4">
                <input
                  value={pollQuestion}
                  onChange={(event) =>
                    setPollQuestion(event.target.value.slice(0, EVENT_POLL_QUESTION_MAX_CHARS))
                  }
                  placeholder="Pergunta da enquete"
                  className="w-full rounded-2xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm outline-none placeholder:text-zinc-600"
                />

                <label className="flex items-start gap-3 rounded-2xl border border-zinc-800 bg-black/20 p-4">
                  <input
                    type="checkbox"
                    checked={pollAllowUserOptions}
                    onChange={(event) => setPollAllowUserOptions(event.target.checked)}
                    className="mt-1 h-4 w-4 accent-emerald-500"
                  />
                  <div>
                    <p className="font-bold text-white">Permitir novas respostas dos usuários</p>
                    <p className="text-sm text-zinc-500">Mantém a enquete aberta para sugestões da comunidade.</p>
                  </div>
                </label>

                <div className="rounded-[1.4rem] border border-zinc-800 bg-black/20 p-4">
                  <div className="flex items-center justify-between gap-3">
                    <p className="text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">Opções iniciais</p>
                    <button
                      type="button"
                      onClick={() =>
                        setPollDraftOptions((previous) =>
                          previous.length >= EVENT_POLL_OPTION_MAX_COUNT ? previous : [...previous, ""]
                        )
                      }
                      disabled={pollDraftOptions.length >= EVENT_POLL_OPTION_MAX_COUNT}
                      className="rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-2 text-xs font-black uppercase text-zinc-200 disabled:opacity-50"
                    >
                      Adicionar resposta
                    </button>
                  </div>
                  <div className="mt-4 space-y-3">
                    {pollDraftOptions.map((option, index) => (
                      <div key={`draft-option-${index}`} className="flex gap-2">
                        <input
                          value={option}
                          onChange={(event) =>
                            setPollDraftOptions((previous) =>
                              previous.map((entry, entryIndex) =>
                                entryIndex === index
                                  ? event.target.value.slice(0, EVENT_POLL_OPTION_MAX_CHARS)
                                  : entry
                              )
                            )
                          }
                          placeholder={`Resposta ${index + 1}`}
                          className="min-w-0 flex-1 rounded-2xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm outline-none placeholder:text-zinc-600"
                        />
                        {pollDraftOptions.length > 2 ? (
                          <button
                            type="button"
                            onClick={() =>
                              setPollDraftOptions((previous) =>
                                previous.filter((_, entryIndex) => entryIndex !== index)
                              )
                            }
                            className="rounded-2xl border border-red-500/20 bg-red-500/10 px-3 text-red-200"
                          >
                            <X size={14} />
                          </button>
                        ) : null}
                      </div>
                    ))}
                  </div>
                </div>
              </div>

              <div className="mt-5 flex justify-end">
                <button
                  type="button"
                  onClick={() => void handleCreatePoll()}
                  className="brand-button-soft"
                >
                  <MessageCircle size={14} />
                  Criar enquete
                </button>
              </div>
            </div>

            <div className="rounded-[1.8rem] border border-zinc-800 bg-zinc-950 p-4 sm:p-5">
              <h2 className="text-xl font-black text-white">Enquetes já publicadas</h2>
              <p className="mt-1 text-sm text-zinc-500">Gerencie respostas e remova opções quando precisar.</p>

              <div className="mt-5 space-y-4">
                {loadingPolls ? (
                  <div className="rounded-[1.4rem] border border-zinc-800 bg-black/20 p-8 text-center">
                    <Loader2 size={20} className="mx-auto animate-spin text-brand" />
                  </div>
                ) : polls.length === 0 ? (
                  <div className="rounded-[1.4rem] border border-dashed border-zinc-800 bg-black/20 p-8 text-center text-sm text-zinc-500">
                    Nenhuma enquete publicada para este evento.
                  </div>
                ) : (
                  polls.map((poll) => (
                    <div key={poll.id} className="rounded-[1.4rem] border border-zinc-800 bg-black/20 p-4">
                      <div className="flex items-start justify-between gap-3">
                        <div>
                          <h3 className="text-base font-black text-white">{poll.question}</h3>
                          <p className="mt-1 text-sm text-zinc-500">
                            {poll.allowUserOptions ? "Aberta para respostas da comunidade." : "Somente respostas pré-definidas."}
                          </p>
                        </div>
                        <div className="flex gap-2">
                          <button
                            type="button"
                            onClick={() => void handleQuickEditPoll(poll)}
                            className="rounded-xl border border-zinc-700 bg-black/20 p-2 text-zinc-200"
                            title="Editar enquete"
                          >
                            <Edit3 size={14} />
                          </button>
                          <button
                            type="button"
                            onClick={() => void handleDeletePoll(poll.id)}
                            className="rounded-xl border border-red-500/20 bg-red-500/10 p-2 text-red-200"
                            title="Excluir enquete"
                          >
                            <Trash2 size={14} />
                          </button>
                        </div>
                      </div>

                      <div className="mt-4 space-y-2">
                        {poll.options.map((option, index) => (
                          <div
                            key={`${poll.id}:${option.text}:${index}`}
                            className="flex items-center justify-between gap-3 rounded-2xl border border-zinc-800 bg-zinc-950/70 px-4 py-3"
                          >
                            <div>
                              <p className="font-bold text-white">{option.text}</p>
                              <p className="text-xs text-zinc-500">
                                {option.votes} voto(s)
                                {option.creatorName ? ` • ${option.creatorName}` : ""}
                              </p>
                            </div>
                            <button
                              type="button"
                              onClick={() => void handleDeletePollOption(poll, index)}
                              className="rounded-xl border border-zinc-700 bg-black/20 p-2 text-zinc-300"
                            >
                              <Trash2 size={13} />
                            </button>
                          </div>
                        ))}
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          </section>
        ) : null}

        {section === "recebedores" ? (
          <section className="grid gap-4 xl:grid-cols-[0.95fr_1.05fr]">
            <div className="rounded-[1.8rem] border border-zinc-800 bg-zinc-950 p-4 sm:p-5">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <h2 className="text-xl font-black text-white">Recebedores do evento</h2>
                  <p className="mt-1 text-sm text-zinc-500">
                    Escolha a partir dos recebedores de eventos já configurados para {recipientScopeLabel}.
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => setReceiversManagerOpen(true)}
                  className="inline-flex items-center justify-center gap-2 rounded-xl border border-brand/30 bg-brand-soft px-4 py-3 text-xs font-black uppercase text-brand-accent transition hover:opacity-90"
                >
                  <UserPlus size={14} />
                  Gerenciar
                </button>
              </div>

              <div className="mt-5">
                <PaymentRecipientCheckboxList
                  id="admin-event-workspace-recipients"
                  label="Liberar comprovantes do evento"
                  helperText="Marque quem pode receber o comprovante específico deste evento."
                  emptyText={`Nenhum recebedor de evento cadastrado para ${recipientScopeLabel}.`}
                  options={paymentRecipients}
                  selectedUserIds={editDraft.recipientUserIds}
                  loading={loadingRecipients}
                  onChange={(recipientUserIds) =>
                    setEditDraft((previous) =>
                      previous ? { ...previous, recipientUserIds } : previous
                    )
                  }
                />
              </div>

              <div className="mt-5 flex justify-end">
                <button
                  ref={saveReceiversButtonRef}
                  type="button"
                  onClick={() => void handleSaveRecebedores(editDraft.recipientUserIds)}
                  disabled={savingEvento}
                  className="brand-button-soft"
                >
                  {savingEvento ? <Loader2 size={14} className="animate-spin" /> : <Check size={14} />}
                  Salvar recebedores
                </button>
              </div>
              <FloatingSaveButton
                watchRef={saveReceiversButtonRef}
                label="Salvar recebedores"
                icon={<Save size={14} />}
                disabled={savingEvento}
                onClick={() => void handleSaveRecebedores(editDraft.recipientUserIds)}
              />
            </div>

            <div className="space-y-4">
              <div className="rounded-[1.8rem] border border-zinc-800 bg-zinc-950 p-4 sm:p-5">
                <h2 className="text-xl font-black text-white">Resumo do financeiro</h2>
                <div className="mt-5 grid gap-3">
                  <div className="rounded-[1.4rem] border border-zinc-800 bg-black/20 p-4">
                    <p className="text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">Chave PIX</p>
                    <p className="mt-2 font-bold text-white">{editDraft.pixChave || "Não definida"}</p>
                  </div>
                  <div className="rounded-[1.4rem] border border-zinc-800 bg-black/20 p-4">
                    <p className="text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">Banco / titular</p>
                    <p className="mt-2 font-bold text-white">
                      {[editDraft.pixBanco, editDraft.pixTitular].filter(Boolean).join(" • ") || "Não definido"}
                    </p>
                  </div>
                  <div className="rounded-[1.4rem] border border-zinc-800 bg-black/20 p-4">
                    <p className="text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">WhatsApp de comprovante</p>
                    <p className="mt-2 font-bold text-white">{editDraft.contatoComprovante || "Não definido"}</p>
                  </div>
                </div>
              </div>

              <div className="rounded-[1.8rem] border border-zinc-800 bg-zinc-950 p-4 sm:p-5">
                <h3 className="text-lg font-black text-white">Recebedores selecionados</h3>
                <div className="mt-4 space-y-3">
                  {selectedRecipients.length === 0 ? (
                    <div className="rounded-[1.4rem] border border-dashed border-zinc-800 bg-black/20 p-6 text-sm text-zinc-500">
                      Nenhum recebedor está marcado neste evento.
                    </div>
                  ) : (
                    selectedRecipients.map((recipient) => (
                      <div key={recipient.userId} className="rounded-[1.4rem] border border-zinc-800 bg-black/20 p-4">
                        <p className="font-black text-white">{recipient.name}</p>
                        <p className="mt-1 text-sm text-zinc-500">
                          {recipient.turma || "Sem turma"}
                          {recipient.phone ? ` • ${recipient.phone}` : ""}
                        </p>
                      </div>
                    ))
                  )}
                </div>
              </div>
            </div>
          </section>
        ) : null}
      </div>
      <PaymentReceiversManager
        tenantId={activeTenantId || ""}
        scope="events"
        recipientContext={paymentRecipientContext}
        open={receiversManagerOpen}
        recipients={paymentRecipients}
        title={hasRecipientOwnerContext ? "Recebedores do órgão" : "Recebedores de eventos"}
        description={
          hasRecipientOwnerContext
            ? "Lista própria deste órgão. Apenas diretoria e gestores podem ser adicionados."
            : "Lista usada somente pelos comprovantes de eventos desta tenant."
        }
        savedMessage="Recebedores de eventos atualizados."
        onClose={() => setReceiversManagerOpen(false)}
        onSaved={setPaymentRecipients}
      />
    </main>
  );
}
