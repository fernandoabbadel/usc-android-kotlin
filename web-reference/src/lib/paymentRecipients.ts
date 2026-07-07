import { fetchTenantMembershipDirectory } from "./tenantMembershipDirectory";
import { resolveStoredTenantScopeId } from "./activeTenantSnapshot";
import { getSupabaseClient } from "./supabase";
import { buildTenantScopedRowId } from "./tenantScopedCatalog";
import { fetchLeagueById } from "./leaguesService";
import { canManageLeagueRole, resolveLeagueRoleLabel } from "./leagueRoles";

export interface TenantPaymentRecipientOption {
  userId: string;
  name: string;
  turma: string;
  phone: string;
  avatarUrl: string;
}

export type TenantPaymentRecipientScope = "tenant" | "events" | "products";
export type TenantPaymentRecipientOwnerType = "tenant" | "league" | "commission" | "directory";

export interface TenantPaymentRecipientContext {
  ownerType?: TenantPaymentRecipientOwnerType | null;
  ownerId?: string | null;
}

const DEFAULT_AVATAR_URL = "https://github.com/shadcn.png";
const PAYMENT_RECEIVERS_DOC_IDS: Record<TenantPaymentRecipientScope, string> = {
  tenant: "payment_receivers",
  events: "event_payment_receivers",
  products: "product_payment_receivers",
};

const resolveRecipientsTenantId = (tenantId?: string | null): string =>
  resolveStoredTenantScopeId(String(tenantId || "").trim());

const normalizeRecipientScope = (
  scope?: TenantPaymentRecipientScope | null
): TenantPaymentRecipientScope =>
  scope === "events" || scope === "products" ? scope : "tenant";

const resolvePaymentReceiversDocId = (
  scope?: TenantPaymentRecipientScope | null,
  context?: TenantPaymentRecipientContext | null
): string => {
  const docId = PAYMENT_RECEIVERS_DOC_IDS[normalizeRecipientScope(scope)];
  const ownerType = String(context?.ownerType || "").trim().toLowerCase();
  const ownerId = String(context?.ownerId || "").trim();
  if (!ownerId || !ownerType || ownerType === "tenant") return docId;
  return `${ownerType}:${ownerId}:${docId}`;
};

const buildPaymentReceiversConfigId = (
  tenantId: string,
  scope?: TenantPaymentRecipientScope | null,
  context?: TenantPaymentRecipientContext | null
): string => {
  const docId = resolvePaymentReceiversDocId(scope, context);
  return buildTenantScopedRowId(resolveRecipientsTenantId(tenantId), docId) || docId;
};

const asRecord = (value: unknown): Record<string, unknown> | null =>
  value && typeof value === "object" ? (value as Record<string, unknown>) : null;

const readRecipientsFromConfigData = (value: unknown): TenantPaymentRecipientOption[] => {
  const data = asRecord(value);
  const rows = Array.isArray(data?.recipients)
    ? data?.recipients
    : Array.isArray(value)
      ? value
      : [];

  return rows
    .map((entry) => normalizeTenantPaymentRecipient(asRecord(entry) || {}))
    .filter((entry): entry is TenantPaymentRecipientOption => entry !== null);
};

export const normalizeTenantPaymentRecipient = (
  value: Partial<TenantPaymentRecipientOption> | null | undefined
): TenantPaymentRecipientOption | null => {
  const userId = String(value?.userId || "").trim();
  const name = String(value?.name || "").trim();
  const turma = String(value?.turma || "").trim();
  const phone = String(value?.phone || "").trim();
  const avatarUrl = String(value?.avatarUrl || "").trim();

  if (!userId && !name && !turma && !phone && !avatarUrl) return null;

  return {
    userId,
    name: name || "Usuário",
    turma: turma || "Sem turma",
    phone,
    avatarUrl: avatarUrl || DEFAULT_AVATAR_URL,
  };
};

export const findTenantPaymentRecipient = (
  recipients: TenantPaymentRecipientOption[],
  userId: string
): TenantPaymentRecipientOption | null => {
  const cleanUserId = userId.trim();
  if (!cleanUserId) return null;
  return recipients.find((entry) => entry.userId === cleanUserId) || null;
};

export const filterTenantPaymentRecipientsByIds = (
  recipients: TenantPaymentRecipientOption[],
  userIds: string[]
): TenantPaymentRecipientOption[] => {
  const selectedIds = new Set(
    userIds.map((entry) => String(entry || "").trim()).filter(Boolean)
  );
  if (selectedIds.size === 0) return [];
  return recipients.filter((entry) => selectedIds.has(entry.userId));
};

export async function fetchTenantPaymentRecipients(
  tenantId: string,
  scope?: TenantPaymentRecipientScope,
  context?: TenantPaymentRecipientContext | null
): Promise<TenantPaymentRecipientOption[]> {
  const cleanTenantId = tenantId.trim();
  if (!cleanTenantId) return [];

  const supabase = getSupabaseClient();
  const configId = buildPaymentReceiversConfigId(cleanTenantId, scope, context);
  const { data, error } = await supabase
    .from("app_config")
    .select("id,data")
    .eq("id", configId)
    .maybeSingle();

  if (error) {
    throw Object.assign(new Error(error.message), {
      code: error.code ?? "db/query-failed",
      cause: error,
    });
  }

  return readRecipientsFromConfigData(asRecord(data)?.data);
}

export async function fetchTenantPaymentReceiverDirectory(
  tenantId: string,
  context?: TenantPaymentRecipientContext | null
): Promise<TenantPaymentRecipientOption[]> {
  const cleanTenantId = tenantId.trim();
  if (!cleanTenantId) return [];

  const directory = await fetchTenantMembershipDirectory({
    tenantId: cleanTenantId,
    statuses: ["approved"],
    limit: 400,
  });

  const ownerType = String(context?.ownerType || "").trim().toLowerCase();
  const ownerId = String(context?.ownerId || "").trim();
  let allowedUserIds: Set<string> | null = null;

  if (ownerId && ownerType && ownerType !== "tenant") {
    allowedUserIds = new Set<string>();
    try {
      const league = await fetchLeagueById(ownerId, {
        forceRefresh: true,
        tenantId: cleanTenantId,
      });
      (league?.managerUserIds || []).forEach((entry) => {
        const userId = String(entry || "").trim();
        if (userId) allowedUserIds?.add(userId);
      });
      (league?.membros || []).forEach((member) => {
        const userId = String(member.id || "").trim();
        if (userId && canManageLeagueRole(member.cargo)) allowedUserIds?.add(userId);
      });
    } catch (error: unknown) {
      console.warn("Recebedores: falha ao carregar gestores do órgão.", error);
    }

    try {
      let query = getSupabaseClient()
        .from("ligas_membros")
        .select("userId,cargo")
        .eq("ligaId", ownerId);
      const scopedTenantId = resolveRecipientsTenantId(cleanTenantId);
      if (scopedTenantId) query = query.eq("tenant_id", scopedTenantId);
      const { data, error } = await query;
      if (!error) {
        (Array.isArray(data) ? data : []).forEach((entry) => {
          const row = asRecord(entry) || {};
          const userId = String(row.userId || "").trim();
          const role = resolveLeagueRoleLabel(String(row.cargo || ""));
          if (userId && canManageLeagueRole(role)) allowedUserIds?.add(userId);
        });
      }
    } catch (error: unknown) {
      console.warn("Recebedores: falha ao consultar membros do órgão.", error);
    }
  }

  return directory
    .filter((entry) => !allowedUserIds || allowedUserIds.has(entry.userId))
    .map((entry) =>
      normalizeTenantPaymentRecipient({
        userId: entry.userId,
        name: entry.nome,
        turma: entry.turma,
        phone: entry.telefone,
        avatarUrl: entry.foto,
      })
    )
    .filter((entry): entry is TenantPaymentRecipientOption => entry !== null);
}

export async function saveTenantPaymentRecipients(
  tenantId: string,
  recipients: TenantPaymentRecipientOption[],
  scope?: TenantPaymentRecipientScope,
  context?: TenantPaymentRecipientContext | null
): Promise<TenantPaymentRecipientOption[]> {
  const cleanTenantId = tenantId.trim();
  if (!cleanTenantId) return [];

  const normalized = recipients
    .map((entry) => normalizeTenantPaymentRecipient(entry))
    .filter((entry): entry is TenantPaymentRecipientOption => entry !== null)
    .filter((entry, index, rows) => {
      const key = entry.userId || `${entry.name}:${entry.phone}`;
      return rows.findIndex((candidate) => (candidate.userId || `${candidate.name}:${candidate.phone}`) === key) === index;
    });

  const supabase = getSupabaseClient();
  const scopedTenantId = resolveRecipientsTenantId(cleanTenantId);
  const { error } = await supabase.from("app_config").upsert(
    {
      id: buildPaymentReceiversConfigId(scopedTenantId, scope, context),
      ...(scopedTenantId ? { tenant_id: scopedTenantId } : {}),
      data: {
        recipients: normalized,
        scope: normalizeRecipientScope(scope),
        ownerType: context?.ownerType || "tenant",
        ownerId: context?.ownerId || "",
        updatedAt: new Date().toISOString(),
      },
      updatedAt: new Date().toISOString(),
    },
    { onConflict: "id" }
  );

  if (error) {
    throw Object.assign(new Error(error.message), {
      code: error.code ?? "db/query-failed",
      cause: error,
    });
  }

  return normalized;
}
