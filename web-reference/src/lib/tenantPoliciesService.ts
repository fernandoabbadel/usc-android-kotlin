import { getSupabaseClient } from "@/lib/supabase";

export type TenantPolicyModule =
  | "eventos"
  | "loja"
  | "planos"
  | "mini_vendor"
  | "checkout"
  | "reembolso_cancelamento"
  | "bebidas_alcoolicas"
  | "menores_de_idade"
  | "termos_tenant";

export type TenantPolicyDocument = {
  id?: string;
  tenant_id?: string;
  module: TenantPolicyModule;
  title: string;
  content: string;
  visible: boolean;
  updated_at?: string;
};

const getAccessToken = async (): Promise<string> => {
  const supabase = getSupabaseClient();
  const { data, error } = await supabase.auth.getSession();
  if (error || !data.session?.access_token) {
    throw new Error("Sessão inválida.");
  }
  return data.session.access_token;
};

export const fetchTenantPolicyDocuments = async (
  tenantId: string
): Promise<TenantPolicyDocument[]> => {
  const token = await getAccessToken();
  const response = await fetch(`/api/admin/politicas?tenantId=${encodeURIComponent(tenantId)}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) {
    const payload = (await response.json().catch(() => null)) as { error?: string } | null;
    throw new Error(payload?.error || "Erro ao carregar políticas.");
  }
  const payload = (await response.json()) as { policies?: TenantPolicyDocument[] };
  return payload.policies || [];
};

export const saveTenantPolicyDocuments = async (
  tenantId: string,
  policies: TenantPolicyDocument[]
): Promise<TenantPolicyDocument[]> => {
  const token = await getAccessToken();
  const response = await fetch("/api/admin/politicas", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({ tenantId, policies }),
  });
  if (!response.ok) {
    const payload = (await response.json().catch(() => null)) as { error?: string } | null;
    throw new Error(payload?.error || "Erro ao salvar políticas.");
  }
  const payload = (await response.json()) as { policies?: TenantPolicyDocument[] };
  return payload.policies || [];
};
