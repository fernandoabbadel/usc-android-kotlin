import { getSupabaseClient } from "@/lib/supabase";

export type LegalDocumentType =
  | "terms_of_service"
  | "privacy_policy"
  | "cookies_policy"
  | "lgpd_rights"
  | "admin_confidentiality"
  | "tenant_terms";

export type LegalAcceptanceSource =
  | "cadastro"
  | "primeiro_acesso"
  | "admin"
  | "tenant_admin"
  | "cookie_banner"
  | "app"
  | "api"
  | "role_upgrade"
  | "event_creation"
  | "mini_vendor_creation";

export type LegalAcceptanceDocument = {
  documentType: LegalDocumentType;
  documentVersion: string;
};

export type PrivacyPreferencesPayload = {
  tenantId?: string | null;
  source?: LegalAcceptanceSource;
  preferences: Partial<{
    analytics: boolean;
    marketing: boolean;
    profile_public: boolean;
    photo_public: boolean;
    phone_visibility: boolean;
    email_notifications: boolean;
    show_full_name: boolean;
    show_turma: boolean;
    show_plan: boolean;
    show_achievements: boolean;
    show_followers: boolean;
    allow_discovery: boolean;
    show_mini_vendor: boolean;
    show_collectives: boolean;
  }>;
};

const getAccessToken = async (): Promise<string> => {
  const supabase = getSupabaseClient();
  const { data, error } = await supabase.auth.getSession();
  if (error || !data.session?.access_token) {
    throw new Error("Sessão inválida. Entre novamente para registrar a preferência.");
  }
  return data.session.access_token;
};

const postAuthenticated = async <TBody extends Record<string, unknown>>(
  path: string,
  body: TBody
): Promise<void> => {
  const accessToken = await getAccessToken();
  const response = await fetch(path, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify(body),
  });

  if (!response.ok) {
    const payload = (await response.json().catch(() => null)) as { error?: string } | null;
    throw new Error(payload?.error || "Não foi possível registrar a informação legal.");
  }
};

export const recordLegalAcceptance = async (payload: {
  documents: LegalAcceptanceDocument[];
  tenantId?: string | null;
  source?: LegalAcceptanceSource;
  readToEnd?: boolean;
  markedRead?: boolean;
  contextType?: string | null;
  contextId?: string | null;
  metadata?: Record<string, unknown>;
}): Promise<void> => {
  await postAuthenticated("/api/legal/acceptance", {
    documents: payload.documents,
    tenantId: payload.tenantId || null,
    source: payload.source || "app",
    readToEnd: payload.readToEnd === true,
    markedRead: payload.markedRead === true,
    contextType: payload.contextType || null,
    contextId: payload.contextId || null,
    metadata: payload.metadata || {},
  });
};

export const savePrivacyPreferences = async (
  payload: PrivacyPreferencesPayload
): Promise<void> => {
  await postAuthenticated("/api/privacy/preferences", {
    tenantId: payload.tenantId || null,
    source: payload.source || "app",
    preferences: payload.preferences,
  });
};
