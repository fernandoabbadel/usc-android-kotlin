import { NextResponse } from "next/server";

import {
  extractPlatformLegalDocuments,
  filterPlatformLegalDocuments,
  type PlatformLegalDocument,
  type PlatformLegalSurface,
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

const normalizeSurface = (value: string | null): PlatformLegalSurface => {
  const surface = (value || "app").trim().toLowerCase();
  if (surface === "landing" || surface === "all") return surface;
  return "app";
};

const stripContentWhenNeeded = (
  documents: PlatformLegalDocument[],
  includeContent: boolean
): PlatformLegalDocument[] =>
  includeContent
    ? documents
    : documents.map((document) => ({
        ...document,
        content: "",
      }));

export async function GET(request: Request) {
  const requestUrl = new URL(request.url);
  const surface = normalizeSurface(requestUrl.searchParams.get("surface"));
  const includeContent = requestUrl.searchParams.get("content") === "1";
  try {
    const documents = filterPlatformLegalDocuments(await fetchStoredDocuments(), surface);

    return NextResponse.json(
      {
        documents: stripContentWhenNeeded(documents, includeContent),
      },
      {
        headers: {
          "Cache-Control": "no-store",
        },
      }
    );
  } catch (error: unknown) {
    console.error("Falha ao carregar documentos legais públicos:", error);
    const fallbackDocuments = filterPlatformLegalDocuments(
      extractPlatformLegalDocuments(null),
      surface
    );
    return NextResponse.json(
      {
        documents: stripContentWhenNeeded(fallbackDocuments, includeContent),
        source: "fallback",
      },
      {
        headers: {
          "Cache-Control": "no-store",
        },
      }
    );
  }
}
