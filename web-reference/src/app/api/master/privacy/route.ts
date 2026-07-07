import { NextRequest, NextResponse } from "next/server";

import {
  extractPlatformLegalDocuments,
  sanitizePlatformLegalDocuments,
  type PlatformLegalDocument,
} from "@/lib/platformLegalDocuments";
import { supabaseAdmin } from "@/lib/supabaseAdmin";

export const dynamic = "force-dynamic";

const SITE_CONFIG_TABLE = "site_config";
const LEGAL_CONFIG_ROW_ID = "platform_legal_documents";
const SELECT_CANDIDATES = ["id,data", "id,config", "id,payload", "*"] as const;

const asObject = (value: unknown): Record<string, unknown> | null =>
  typeof value === "object" && value !== null ? (value as Record<string, unknown>) : null;

const asString = (value: unknown): string =>
  typeof value === "string" ? value.trim() : "";

const getSupabaseErrorText = (error: unknown): string => {
  const raw = asObject(error);
  return [
    error instanceof Error ? error.message : "",
    asString(raw?.message),
    asString(raw?.details),
    asString(raw?.hint),
  ]
    .filter(Boolean)
    .join(" ")
    .toLowerCase();
};

const shouldFallbackMissingColumns = (error: unknown, columns: readonly string[]): boolean => {
  const message = getSupabaseErrorText(error);
  if (!message.includes("column") || !message.includes("does not exist")) return false;
  return columns.some((column) => message.includes(column.toLowerCase()));
};

const extractPayload = (raw: unknown): unknown => {
  const obj = asObject(raw);
  if (!obj) return raw;
  if (obj.data && typeof obj.data === "object") return obj.data;
  if (obj.config && typeof obj.config === "object") return obj.config;
  if (obj.payload && typeof obj.payload === "object") return obj.payload;
  return raw;
};

const getMasterUserId = async (request: NextRequest): Promise<string> => {
  const authHeader = request.headers.get("authorization") || "";
  const accessToken = authHeader.replace(/^Bearer\s+/i, "").trim();
  if (!accessToken) throw new Error("Não autenticado.");

  const { data: authData, error: authError } = await supabaseAdmin.auth.getUser(accessToken);
  if (authError || !authData.user) throw new Error("Sessão inválida.");

  const { data: profileRow, error: profileError } = await supabaseAdmin
    .from("users")
    .select("uid,role,status")
    .eq("uid", authData.user.id)
    .maybeSingle();
  if (profileError) throw new Error(profileError.message);

  const profile = asObject(profileRow) ?? {};
  const role = asString(profile.role).toLowerCase();
  const status = asString(profile.status).toLowerCase();
  if (role !== "master" || status === "banned" || status === "bloqueado") {
    throw new Error("Sem permissão para gerenciar a privacidade global.");
  }

  return authData.user.id;
};

const fetchStoredDocuments = async (): Promise<PlatformLegalDocument[]> => {
  for (const selectColumns of SELECT_CANDIDATES) {
    const { data, error } = await supabaseAdmin
      .from(SITE_CONFIG_TABLE)
      .select(selectColumns)
      .eq("id", LEGAL_CONFIG_ROW_ID)
      .maybeSingle();

    if (!error) {
      return extractPlatformLegalDocuments(extractPayload(data));
    }

    if (shouldFallbackMissingColumns(error, ["data", "config", "payload", "updated_at"])) {
      continue;
    }

    throw error;
  }

  return extractPlatformLegalDocuments(null);
};

const saveStoredDocuments = async (
  documents: PlatformLegalDocument[],
  userId: string
): Promise<void> => {
  const nowIso = new Date().toISOString();
  const payload = {
    documents,
    updatedAt: nowIso,
    updatedByUserId: userId,
  };
  const payloadCandidates: Array<Record<string, unknown>> = [
    {
      id: LEGAL_CONFIG_ROW_ID,
      tenant_id: null,
      data: payload,
      updated_at: nowIso,
    },
    {
      id: LEGAL_CONFIG_ROW_ID,
      data: payload,
      updated_at: nowIso,
    },
    {
      id: LEGAL_CONFIG_ROW_ID,
      tenant_id: null,
      config: payload,
      updated_at: nowIso,
    },
    {
      id: LEGAL_CONFIG_ROW_ID,
      config: payload,
      updated_at: nowIso,
    },
    {
      id: LEGAL_CONFIG_ROW_ID,
      tenant_id: null,
      payload,
      updated_at: nowIso,
    },
    {
      id: LEGAL_CONFIG_ROW_ID,
      payload,
      updated_at: nowIso,
    },
    {
      id: LEGAL_CONFIG_ROW_ID,
      data: payload,
    },
    {
      id: LEGAL_CONFIG_ROW_ID,
      config: payload,
    },
    {
      id: LEGAL_CONFIG_ROW_ID,
      payload,
    },
  ];

  let lastSchemaError: unknown = null;
  for (const candidate of payloadCandidates) {
    const { error } = await supabaseAdmin.from(SITE_CONFIG_TABLE).upsert(candidate, {
      onConflict: "id",
    });

    if (!error) return;

    if (
      shouldFallbackMissingColumns(error, [
        "tenant_id",
        "data",
        "config",
        "payload",
        "updated_at",
      ])
    ) {
      lastSchemaError = error;
      continue;
    }

    throw error;
  }

  if (lastSchemaError) throw lastSchemaError;
};

const statusForMessage = (message: string): number => {
  const lower = message.toLowerCase();
  if (lower.includes("autentic") || lower.includes("sessão")) return 401;
  if (lower.includes("permiss")) return 403;
  return 500;
};

export async function GET(request: NextRequest) {
  try {
    await getMasterUserId(request);
    return NextResponse.json({ documents: await fetchStoredDocuments() });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : "Erro inesperado.";
    return NextResponse.json({ error: message }, { status: statusForMessage(message) });
  }
}

export async function POST(request: NextRequest) {
  try {
    const userId = await getMasterUserId(request);
    const body = asObject(await request.json().catch(() => null));
    const documents = sanitizePlatformLegalDocuments(body?.documents || []);
    await saveStoredDocuments(documents, userId);
    return NextResponse.json({ documents });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : "Erro inesperado.";
    return NextResponse.json({ error: message }, { status: statusForMessage(message) });
  }
}
