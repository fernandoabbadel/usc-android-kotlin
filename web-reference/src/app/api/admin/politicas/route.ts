import { NextRequest, NextResponse } from "next/server";

import { supabaseAdmin } from "@/lib/supabaseAdmin";

const MANAGER_TENANT_ROLES = new Set([
  "master",
  "master_tenant",
  "admin_geral",
  "admin_gestor",
  "admin_tenant",
]);

const POLICY_MODULES = new Set([
  "eventos",
  "loja",
  "planos",
  "mini_vendor",
  "checkout",
  "reembolso_cancelamento",
  "bebidas_alcoolicas",
  "menores_de_idade",
  "termos_tenant",
]);

const asObject = (value: unknown): Record<string, unknown> | null =>
  typeof value === "object" && value !== null ? (value as Record<string, unknown>) : null;

const asString = (value: unknown): string =>
  typeof value === "string" ? value.trim() : "";

const getAuthScope = async (request: NextRequest) => {
  const authHeader = request.headers.get("authorization") || "";
  const accessToken = authHeader.replace(/^Bearer\s+/i, "").trim();
  if (!accessToken) throw new Error("Não autenticado.");

  const { data: authData, error: authError } = await supabaseAdmin.auth.getUser(accessToken);
  if (authError || !authData.user) throw new Error("Sessão inválida.");

  const { data: profileRow, error: profileError } = await supabaseAdmin
    .from("users")
    .select("uid,role,tenant_id,tenant_role")
    .eq("uid", authData.user.id)
    .maybeSingle();
  if (profileError) throw new Error(profileError.message);

  const profile = asObject(profileRow) ?? {};
  return {
    userId: authData.user.id,
    role: asString(profile.role).toLowerCase(),
    tenantId: asString(profile.tenant_id),
    tenantRole: asString(profile.tenant_role).toLowerCase(),
  };
};

const canManageTenant = (
  scope: Awaited<ReturnType<typeof getAuthScope>>,
  tenantId: string
): boolean =>
  scope.role === "master" ||
  (scope.tenantId === tenantId && MANAGER_TENANT_ROLES.has(scope.tenantRole));

export async function GET(request: NextRequest) {
  try {
    const scope = await getAuthScope(request);
    const tenantId = asString(request.nextUrl.searchParams.get("tenantId"));
    if (!tenantId) {
      return NextResponse.json({ error: "Tenant inválido." }, { status: 400 });
    }
    if (!canManageTenant(scope, tenantId)) {
      return NextResponse.json({ error: "Sem permissão para gerenciar políticas deste tenant." }, { status: 403 });
    }

    const { data, error } = await supabaseAdmin
      .from("tenant_policy_documents")
      .select("id,tenant_id,module,title,content,visible,updated_at")
      .eq("tenant_id", tenantId)
      .order("module", { ascending: true });
    if (error) {
      return NextResponse.json({ error: error.message }, { status: 400 });
    }

    return NextResponse.json({ policies: data || [] });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : "Erro inesperado.";
    return NextResponse.json({ error: message }, { status: message.includes("autentic") ? 401 : 500 });
  }
}

export async function POST(request: NextRequest) {
  try {
    const scope = await getAuthScope(request);
    const body = asObject(await request.json().catch(() => null));
    const tenantId = asString(body?.tenantId);
    if (!tenantId) {
      return NextResponse.json({ error: "Tenant inválido." }, { status: 400 });
    }
    if (!canManageTenant(scope, tenantId)) {
      return NextResponse.json({ error: "Sem permissão para gerenciar políticas deste tenant." }, { status: 403 });
    }

    const rawPolicies = Array.isArray(body?.policies) ? body?.policies : [];
    const policies = rawPolicies
      .map((entry) => asObject(entry))
      .map((entry) => ({
        tenant_id: tenantId,
        module: asString(entry?.module),
        title: asString(entry?.title).slice(0, 160),
        content: asString(entry?.content).slice(0, 12000),
        visible: Boolean(entry?.visible),
        updated_by_user_id: scope.userId,
        created_by_user_id: scope.userId,
      }))
      .filter((entry) => POLICY_MODULES.has(entry.module) && entry.title);

    if (!policies.length) {
      return NextResponse.json({ error: "Nenhuma política válida informada." }, { status: 400 });
    }

    const { error } = await supabaseAdmin
      .from("tenant_policy_documents")
      .upsert(policies, { onConflict: "tenant_id,module" });

    if (error) {
      return NextResponse.json({ error: error.message }, { status: 400 });
    }

    const { data, error: fetchError } = await supabaseAdmin
      .from("tenant_policy_documents")
      .select("id,tenant_id,module,title,content,visible,updated_at")
      .eq("tenant_id", tenantId)
      .order("module", { ascending: true });
    if (fetchError) {
      return NextResponse.json({ error: fetchError.message }, { status: 400 });
    }

    return NextResponse.json({ policies: data || [] });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : "Erro inesperado.";
    return NextResponse.json({ error: message }, { status: message.includes("autentic") ? 401 : 500 });
  }
}
