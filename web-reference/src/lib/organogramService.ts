import { resolveStoredTenantScopeId } from "./activeTenantSnapshot";
import { getSupabaseClient } from "./supabase";
import { throwSupabaseError } from "./supabaseData";
import { buildTenantScopedRowId } from "./tenantScopedCatalog";

type CacheEntry<T> = {
  cachedAt: number;
  value: T;
};

const READ_CACHE_TTL_MS = 45_000;
const ORGANOGRAM_DOC_ID = "organograma";

const configCache = new Map<string, CacheEntry<OrganogramConfig>>();

const asObject = (value: unknown): Record<string, unknown> | null =>
  typeof value === "object" && value !== null ? (value as Record<string, unknown>) : null;

const asString = (value: unknown, fallback = ""): string =>
  typeof value === "string" ? value : fallback;

const asStringArray = (value: unknown): string[] =>
  Array.isArray(value) ? value.filter((entry): entry is string => typeof entry === "string") : [];

const asNumber = (value: unknown, fallback = 0): number => {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string") {
    const parsed = Number(value);
    if (Number.isFinite(parsed)) return parsed;
  }
  return fallback;
};

const nowIso = (): string => new Date().toISOString();

const extractMissingSchemaColumn = (error: unknown): string | null => {
  if (!error || typeof error !== "object") return null;
  const raw = error as { message?: unknown; details?: unknown };
  const text = [asString(raw.message), asString(raw.details)]
    .filter((entry) => entry.length > 0)
    .join(" | ");
  if (!text) return null;

  const patterns = [
    /column\s+[a-z0-9_]+\.(\w+)\s+does not exist/i,
    /column\s+(\w+)\s+does not exist/i,
    /could not find the ['"]?(\w+)['"]? column/i,
  ];

  for (const pattern of patterns) {
    const match = text.match(pattern);
    if (match?.[1]) return match[1];
  }

  return null;
};

const removeMissingColumnFromSelection = (
  columns: string[],
  missingColumn: string
): string[] | null => {
  const next = columns.filter(
    (column) => column.toLowerCase() !== missingColumn.toLowerCase()
  );
  if (next.length === columns.length) return null;
  return next;
};

const resolveOrganogramTenantId = (tenantId?: string | null): string =>
  resolveStoredTenantScopeId(asString(tenantId).trim());

const resolveOrganogramDocId = (tenantId?: string | null): string =>
  buildTenantScopedRowId(resolveOrganogramTenantId(tenantId), ORGANOGRAM_DOC_ID) ||
  ORGANOGRAM_DOC_ID;

const getCacheKey = (tenantId?: string | null): string =>
  resolveOrganogramTenantId(tenantId) || "global";

export interface OrganogramMemberRecord {
  id: string;
  secao: string;
  cargo: string;
  ordem: number;
  status?: "pending" | "approved" | "rejected";
  userId?: string;
  nome?: string;
  foto?: string;
  requestedAt?: string;
  requestedBy?: string;
  approvedAt?: string;
  approvedBy?: string;
  updatedAt?: string;
}

export interface OrganogramConfig {
  tituloPagina: string;
  subtituloPagina: string;
  membros: OrganogramMemberRecord[];
  ordemSecoes: string[];
}

const DEFAULT_ORGANOGRAM_CONFIG: OrganogramConfig = {
  tituloPagina: "Organograma da Atlética",
  subtituloPagina: "Presidencia, vice-presidencia e diretorias em um painel vivo.",
  membros: [],
  ordemSecoes: [],
};

const normalizeSectionName = (value: unknown): string =>
  asString(value, "Diretoria").trim().replace(/\s+/g, " ").slice(0, 60) || "Diretoria";

const normalizeMemberStatus = (value: unknown): OrganogramMemberRecord["status"] | undefined => {
  const normalized = asString(value).trim().toLowerCase();
  if (normalized === "pending" || normalized === "approved" || normalized === "rejected") {
    return normalized;
  }
  return undefined;
};

const buildSectionOrder = (
  rawOrder: unknown,
  members: OrganogramMemberRecord[]
): string[] => {
  const configuredOrder = Array.from(
    new Set(asStringArray(rawOrder).map((entry) => normalizeSectionName(entry)).filter(Boolean))
  );
  const missingSections = Array.from(
    new Set(members.map((member) => normalizeSectionName(member.secao)).filter(Boolean))
  ).filter((section) => !configuredOrder.includes(section));
  return [...configuredOrder, ...missingSections];
};

const normalizeMember = (raw: unknown, index: number): OrganogramMemberRecord | null => {
  const data = asObject(raw);
  if (!data) return null;

  const cargo = asString(data.cargo).trim().slice(0, 80);
  const secao = normalizeSectionName(data.secao);
  if (!cargo) return null;

  const id =
    asString(data.id).trim().slice(0, 120) || `organograma:${secao}:${cargo}:${index}`;
  const userId = asString(data.userId).trim().slice(0, 120) || undefined;
  const nome = asString(data.nome).trim().slice(0, 120) || undefined;
  const foto = asString(data.foto).trim().slice(0, 2000) || undefined;
  const status = normalizeMemberStatus(data.status);
  const requestedAt = asString(data.requestedAt).trim().slice(0, 80) || undefined;
  const requestedBy = asString(data.requestedBy).trim().slice(0, 120) || undefined;
  const approvedAt = asString(data.approvedAt).trim().slice(0, 80) || undefined;
  const approvedBy = asString(data.approvedBy).trim().slice(0, 120) || undefined;
  const updatedAt = asString(data.updatedAt).trim().slice(0, 80) || undefined;

  return {
    id,
    secao,
    cargo,
    ordem: Math.max(0, Math.floor(asNumber(data.ordem, index))),
    ...(status ? { status } : {}),
    ...(userId ? { userId } : {}),
    ...(nome ? { nome } : {}),
    ...(foto ? { foto } : {}),
    ...(requestedAt ? { requestedAt } : {}),
    ...(requestedBy ? { requestedBy } : {}),
    ...(approvedAt ? { approvedAt } : {}),
    ...(approvedBy ? { approvedBy } : {}),
    ...(updatedAt ? { updatedAt } : {}),
  };
};

export const ORGANOGRAM_DATA_USE_CONTEXT_TYPE = "organogram_member";

export const buildOrganogramDataUseContextId = (userId?: string | null): string =>
  asString(userId).trim();

export const isPublishedOrganogramMember = (member: OrganogramMemberRecord): boolean =>
  member.status !== "pending" && member.status !== "rejected";

const normalizeConfig = (raw: unknown): OrganogramConfig => {
  const data = asObject(raw) ?? {};
  const nested = asObject(data.data) ?? {};
  const title =
    asString(data.tituloPagina).trim() ||
    asString(nested.tituloPagina).trim() ||
    DEFAULT_ORGANOGRAM_CONFIG.tituloPagina;
  const subtitle =
    asString(data.subtituloPagina).trim() ||
    asString(nested.subtituloPagina).trim() ||
    DEFAULT_ORGANOGRAM_CONFIG.subtituloPagina;
  const membersSource = Array.isArray(data.membros)
    ? data.membros
    : Array.isArray(nested.membros)
      ? nested.membros
      : [];
  const normalizedMembers = membersSource
    .map((member, index) => normalizeMember(member, index))
    .filter((member): member is OrganogramMemberRecord => member !== null);
  const ordemSecoes = buildSectionOrder(
    data.ordemSecoes ?? nested.ordemSecoes ?? data.sectionOrder ?? nested.sectionOrder,
    normalizedMembers
  );
  const sectionOrderIndex = new Map(ordemSecoes.map((section, index) => [section, index]));

  return {
    tituloPagina: title.slice(0, 120),
    subtituloPagina: subtitle.slice(0, 240),
    membros: normalizedMembers.sort(
      (left, right) =>
        (sectionOrderIndex.get(normalizeSectionName(left.secao)) ?? Number.MAX_SAFE_INTEGER) -
          (sectionOrderIndex.get(normalizeSectionName(right.secao)) ?? Number.MAX_SAFE_INTEGER) ||
        left.ordem - right.ordem ||
        left.cargo.localeCompare(right.cargo, "pt-BR")
    ),
    ordemSecoes,
  };
};

export async function fetchOrganogramConfig(options?: {
  forceRefresh?: boolean;
  tenantId?: string | null;
}): Promise<OrganogramConfig> {
  const forceRefresh = options?.forceRefresh ?? false;
  const cacheKey = getCacheKey(options?.tenantId);
  const cached = configCache.get(cacheKey);
  if (!forceRefresh && cached && Date.now() - cached.cachedAt <= READ_CACHE_TTL_MS) {
    return cached.value;
  }

  const supabase = getSupabaseClient();
  let selectColumns = ["id", "tituloPagina", "subtituloPagina", "membros", "data"];
  let row: Record<string, unknown> | null = null;

  while (selectColumns.length > 0) {
    const { data, error } = await supabase
      .from("app_config")
      .select(selectColumns.join(","))
      .eq("id", resolveOrganogramDocId(options?.tenantId))
      .maybeSingle();

    if (!error) {
      row = asObject(data);
      break;
    }

    const missingColumn = asString(extractMissingSchemaColumn(error)).trim();
    const nextColumns =
      removeMissingColumnFromSelection(selectColumns, missingColumn) ?? [];
    if (nextColumns.length === 0) throwSupabaseError(error);
    selectColumns = nextColumns;
  }

  const config = row ? normalizeConfig(row) : { ...DEFAULT_ORGANOGRAM_CONFIG };
  configCache.set(cacheKey, { cachedAt: Date.now(), value: config });
  return config;
}

export async function saveOrganogramConfig(
  payload: OrganogramConfig,
  options?: { tenantId?: string | null }
): Promise<void> {
  const normalized = normalizeConfig(payload);
  const supabase = getSupabaseClient();
  const scopedTenantId = resolveOrganogramTenantId(options?.tenantId);
  const mutablePayload: Record<string, unknown> = {
    id: resolveOrganogramDocId(scopedTenantId),
    ...(scopedTenantId ? { tenant_id: scopedTenantId } : {}),
    tituloPagina: normalized.tituloPagina,
    subtituloPagina: normalized.subtituloPagina,
    membros: normalized.membros,
    ordemSecoes: normalized.ordemSecoes,
    data: {
      tituloPagina: normalized.tituloPagina,
      subtituloPagina: normalized.subtituloPagina,
      membros: normalized.membros,
      ordemSecoes: normalized.ordemSecoes,
    },
    updatedAt: nowIso(),
  };

  while (Object.keys(mutablePayload).length > 0) {
    const { error } = await supabase
      .from("app_config")
      .upsert(mutablePayload, { onConflict: "id" });
    if (!error) {
      configCache.set(getCacheKey(scopedTenantId), {
        cachedAt: Date.now(),
        value: normalized,
      });
      return;
    }

    const missingColumn = asString(extractMissingSchemaColumn(error)).trim();
    if (!missingColumn) throwSupabaseError(error);

    const removableKey = Object.keys(mutablePayload).find(
      (key) => key.toLowerCase() === missingColumn.toLowerCase()
    );
    if (typeof removableKey !== "string" || removableKey === "id") {
      throwSupabaseError(error);
    }
    delete mutablePayload[removableKey as keyof typeof mutablePayload];
  }
}

export async function requestOrganogramMembership(payload: {
  tenantId?: string | null;
  cargo: string;
  secao: string;
}): Promise<OrganogramMemberRecord> {
  const supabase = getSupabaseClient();
  const { data, error } = await supabase.auth.getSession();
  if (error || !data.session?.access_token) {
    throw new Error("Sessão inválida. Entre novamente para pedir entrada no organograma.");
  }

  const response = await fetch("/api/organogram/requests", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${data.session.access_token}`,
    },
    body: JSON.stringify({
      tenantId: payload.tenantId || null,
      cargo: payload.cargo,
      secao: payload.secao,
    }),
  });

  const result = (await response.json().catch(() => null)) as {
    member?: OrganogramMemberRecord;
    error?: string;
  } | null;

  if (!response.ok || !result?.member) {
    throw new Error(result?.error || "Não foi possível enviar a solicitação.");
  }

  clearOrganogramConfigCache();
  return result.member;
}

export function getDefaultOrganogramConfig(): OrganogramConfig {
  return {
    tituloPagina: DEFAULT_ORGANOGRAM_CONFIG.tituloPagina,
    subtituloPagina: DEFAULT_ORGANOGRAM_CONFIG.subtituloPagina,
    membros: [],
    ordemSecoes: [],
  };
}

export function clearOrganogramConfigCache(): void {
  configCache.clear();
}
