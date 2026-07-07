import { NextRequest, NextResponse } from "next/server";

import { LEGAL_VERSION } from "@/components/legal/legalContent";
import { supabaseAdmin } from "@/lib/supabaseAdmin";

type LegalDocumentType =
  | "terms_of_service"
  | "privacy_policy"
  | "cookies_policy"
  | "lgpd_rights"
  | "admin_confidentiality"
  | "tenant_terms";

const ALLOWED_DOCUMENT_TYPES = new Set<LegalDocumentType>([
  "terms_of_service",
  "privacy_policy",
  "cookies_policy",
  "lgpd_rights",
  "admin_confidentiality",
  "tenant_terms",
]);

const ALLOWED_SOURCES = new Set([
  "cadastro",
  "primeiro_acesso",
  "admin",
  "tenant_admin",
  "cookie_banner",
  "app",
  "api",
  "role_upgrade",
  "event_creation",
  "mini_vendor_creation",
]);

const asObject = (value: unknown): Record<string, unknown> | null =>
  typeof value === "object" && value !== null ? (value as Record<string, unknown>) : null;

const asString = (value: unknown): string =>
  typeof value === "string" ? value.trim() : "";

type AuthorizationScope = "tenant" | "liga" | "comissao" | "diretorio" | "mini_vendor";

const normalizeToken = (value: unknown): string =>
  asString(value)
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[\s-]+/g, "_");

const normalizeAuthorizationScope = (value: unknown): AuthorizationScope | "" => {
  const token = normalizeToken(value);
  if (!token) return "";
  if (["tenant", "organograma", "organogram", "admin"].includes(token)) {
    return "tenant";
  }
  if (["mini_vendor", "minivendor", "lojinha", "loja"].includes(token)) return "mini_vendor";
  if (["liga", "ligas", "league", "leagues"].includes(token)) return "liga";
  if (["comissao", "comissoes", "commission", "commissions"].includes(token)) return "comissao";
  if (["diretorio", "diretorios", "directory", "directories"].includes(token)) return "diretorio";
  return "";
};

const resolveAuthorizationScope = (
  contextType: string | null,
  metadata: Record<string, unknown>
): AuthorizationScope => {
  const explicitScope =
    normalizeAuthorizationScope(metadata.authorizationScope) ||
    normalizeAuthorizationScope(metadata.authorization_scope) ||
    normalizeAuthorizationScope(metadata.scope);
  if (explicitScope) return explicitScope;

  const haystack = [
    contextType,
    metadata.area,
    metadata.category,
    metadata.ownerType,
    metadata.module,
  ]
    .map(normalizeToken)
    .join(" ");

  if (haystack.includes("mini_vendor") || haystack.includes("minivendor")) return "mini_vendor";
  if (haystack.includes("commission") || haystack.includes("comissao")) return "comissao";
  if (haystack.includes("directory") || haystack.includes("diretorio")) return "diretorio";
  if (haystack.includes("league") || haystack.includes("liga")) return "liga";
  return "tenant";
};

const getClientIp = (request: NextRequest): string => {
  const forwardedFor = request.headers.get("x-forwarded-for") || "";
  return (
    forwardedFor
      .split(",")
      .map((part) => part.trim())
      .find(Boolean) ||
    request.headers.get("x-real-ip") ||
    ""
  );
};

const missingColumnFromError = (error: { message?: string; details?: string } | null): string => {
  const message = `${error?.message || ""} ${error?.details || ""}`;
  const schemaCacheMatch = message.match(/Could not find the '([^']+)' column/i);
  if (schemaCacheMatch?.[1]) return schemaCacheMatch[1];
  const sqlColumnMatch = message.match(/column "?([a-zA-Z0-9_]+)"? .*does not exist/i);
  return sqlColumnMatch?.[1] || "";
};

const insertLegalRow = async (
  tableName: "user_legal_acceptances" | "user_legal_read_receipts",
  row: Record<string, unknown>
): Promise<{ message: string; code?: string } | null> => {
  const nextRow = { ...row };
  for (let attempt = 0; attempt < 6; attempt += 1) {
    const { error } = await supabaseAdmin.from(tableName).insert(nextRow);
    if (!error || error.code === "23505") return null;

    const missingColumn = missingColumnFromError(error);
    const removableKey = Object.keys(nextRow).find(
      (key) => key.toLowerCase() === missingColumn.toLowerCase()
    );
    if (!removableKey) return error;
    delete nextRow[removableKey];
  }

  return { message: "Não foi possível registrar a autorização legal." };
};

export async function POST(request: NextRequest) {
  try {
    const authHeader = request.headers.get("authorization") || "";
    const accessToken = authHeader.replace(/^Bearer\s+/i, "").trim();
    if (!accessToken) {
      return NextResponse.json({ error: "Não autenticado." }, { status: 401 });
    }

    const { data: authData, error: authError } = await supabaseAdmin.auth.getUser(accessToken);
    if (authError || !authData.user) {
      return NextResponse.json({ error: "Sessão inválida." }, { status: 401 });
    }

    const body = asObject(await request.json().catch(() => null));
    const source = ALLOWED_SOURCES.has(asString(body?.source)) ? asString(body?.source) : "app";
    const tenantId = asString(body?.tenantId) || null;
    const readToEnd = body?.readToEnd === true;
    const markedRead = body?.markedRead === true;
    const contextType = asString(body?.contextType).slice(0, 80) || null;
    const contextId = asString(body?.contextId).slice(0, 120) || null;
    const metadata = asObject(body?.metadata) || {};
    const rawDocuments = Array.isArray(body?.documents) ? body?.documents : [];
    const documents = rawDocuments
      .map((entry) => asObject(entry))
      .map((entry) => ({
        document_type: asString(entry?.documentType) as LegalDocumentType,
        document_version: asString(entry?.documentVersion) || LEGAL_VERSION,
      }))
      .filter((entry) => ALLOWED_DOCUMENT_TYPES.has(entry.document_type));

    if (!documents.length) {
      return NextResponse.json({ error: "Documento legal inválido." }, { status: 400 });
    }

    const userId = authData.user.id;
    const acceptedAt = new Date().toISOString();
    const ipAddress = getClientIp(request);
    const userAgent = request.headers.get("user-agent") || "";
    const { data: userData } = await supabaseAdmin
      .from("users")
      .select("nome,email")
      .eq("uid", userId)
      .maybeSingle();
    const userRecord = asObject(userData) || {};
    const userMetadata = asObject(authData.user.user_metadata) || {};
    const userName =
      (
        asString(userRecord.nome) ||
        asString(userMetadata.nome) ||
        asString(userMetadata.full_name) ||
        asString(userMetadata.name) ||
        asString(userMetadata.user_name) ||
        asString(userRecord.email) ||
        asString(authData.user.email) ||
        "Usuário"
      ).slice(0, 180);
    const authorizationScope = resolveAuthorizationScope(contextType, metadata);
    const enrichedMetadata = {
      ...metadata,
      userName,
      authorizationScope,
      authorization_scope: authorizationScope,
    };

    const rows = documents.map((document) => ({
      user_id: userId,
      user_name: userName,
      document_type: document.document_type,
      document_version: document.document_version,
      accepted_at: acceptedAt,
      ip_address: ipAddress,
      user_agent: userAgent,
      tenant_id: tenantId,
      source,
      context_type: contextType,
      context_id: contextId,
      authorization_scope: authorizationScope,
      metadata: enrichedMetadata,
    }));

    for (const row of rows) {
      const insertError = await insertLegalRow("user_legal_acceptances", row);
      if (insertError) {
        return NextResponse.json({ error: insertError.message }, { status: 400 });
      }
    }

    if (readToEnd || markedRead) {
      const readRows = documents.map((document) => ({
        user_id: userId,
        user_name: userName,
        document_type: document.document_type,
        document_version: document.document_version,
        tenant_id: tenantId,
        source,
        read_completed_at: acceptedAt,
        marked_read_at: markedRead ? acceptedAt : null,
        accepted_at: acceptedAt,
        ip_address: ipAddress,
        user_agent: userAgent,
        context_type: contextType,
        context_id: contextId,
        authorization_scope: authorizationScope,
        metadata: enrichedMetadata,
      }));

      for (const row of readRows) {
        const readInsertError = await insertLegalRow("user_legal_read_receipts", row);
        if (readInsertError) {
          return NextResponse.json({ error: readInsertError.message }, { status: 400 });
        }
      }
    }

    const userPatch: Record<string, unknown> = {
      legal_accepted_version: LEGAL_VERSION,
      legal_accepted_source: source,
      legal_accepted_tenant_id: tenantId,
    };
    if (documents.some((document) => document.document_type === "terms_of_service")) {
      userPatch.legal_terms_accepted_at = acceptedAt;
    }
    if (documents.some((document) => document.document_type === "privacy_policy")) {
      userPatch.legal_privacy_accepted_at = acceptedAt;
    }
    if (
      source === "role_upgrade" &&
      documents.some(
        (document) =>
          document.document_type === "admin_confidentiality" ||
          document.document_type === "tenant_terms"
      )
    ) {
      userPatch.legal_admin_accepted_at = acceptedAt;
    }

    if (Object.keys(userPatch).length > 0) {
      const { error: userUpdateError } = await supabaseAdmin
        .from("users")
        .update(userPatch)
        .eq("uid", userId);
      if (userUpdateError) {
        return NextResponse.json({ error: userUpdateError.message }, { status: 400 });
      }
    }

    return NextResponse.json({ ok: true });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : "Erro inesperado.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
