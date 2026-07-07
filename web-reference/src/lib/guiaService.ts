import { httpsCallable } from "@/lib/supa/functions";
import { getDownloadURL, ref, uploadBytes } from "@/lib/supa/storage";

import { compressImageFile } from "./imageCompression";
import { functions, storage } from "./backend";
import { getBackendErrorCode } from "./backendErrors";
import { throwSupabaseError } from "./supabaseData";
import { getSupabaseClient } from "./supabase";
import { validateImageFile } from "./upload";
import { resolveStoredTenantScopeId } from "./activeTenantSnapshot";

type CacheEntry<T> = { cachedAt: number; value: T };
type Row = Record<string, unknown>;

const TTL_MS = 20_000;
const MAX_GUIDE_ITEMS = 1200;
export const GUIDE_PHOTO_SOURCE_MAX_BYTES = 2 * 1024 * 1024;
export const GUIDE_PHOTO_COMPRESSED_MAX_BYTES = 200 * 1024;
export const GUIDE_PHOTO_ACCEPTED_TYPES = ["image/jpeg", "image/png", "image/webp"] as const;

const CALLABLE_GUIDE_SEED = "guiaAdminSeed";
const CALLABLE_GUIDE_UPSERT = "guiaAdminUpsert";
const CALLABLE_GUIDE_DELETE = "guiaAdminDelete";
const GUIDE_SELECT_COLUMNS =
  "id,categoria,ordem,titulo,url,nome,horario,detalhe,descricao,foto,numero,cor,tenant_id";

const guideCache = new Map<string, CacheEntry<Row[]>>();

export type GuideCategory =
  | "academico"
  | "transporte"
  | "turismo"
  | "emergencia"
  | "grupos";

const getCache = <T>(cache: Map<string, CacheEntry<T>>, key: string): T | null => {
  const cached = cache.get(key);
  if (!cached) return null;
  if (Date.now() - cached.cachedAt > TTL_MS) {
    cache.delete(key);
    return null;
  }
  return cached.value;
};

const setCache = <T>(cache: Map<string, CacheEntry<T>>, key: string, value: T): void => {
  cache.set(key, { cachedAt: Date.now(), value });
};

const boundedLimit = (requested: number, maxAllowed: number): number => {
  if (!Number.isFinite(requested)) return maxAllowed;
  if (requested < 1) return 1;
  if (requested > maxAllowed) return maxAllowed;
  return Math.floor(requested);
};

const resolveGuideTenantId = (tenantId?: string | null): string =>
  resolveStoredTenantScopeId(typeof tenantId === "string" ? tenantId.trim() : "");

const scopeGuideData = (data: Row, tenantId?: string | null): Row => {
  const scopedTenantId = resolveGuideTenantId(tenantId);
  if (!scopedTenantId) return data;
  return { ...data, tenant_id: scopedTenantId };
};

export const validateGuidePhotoFile = (file: File): string | null =>
  validateImageFile(file, {
    maxBytes: GUIDE_PHOTO_SOURCE_MAX_BYTES,
    allowedTypes: GUIDE_PHOTO_ACCEPTED_TYPES,
  });

const shouldFallbackToClient = (error: unknown): boolean => {
  const code = getBackendErrorCode(error)?.toLowerCase();
  if (!code) return true;

  return (
    code.includes("functions/not-found") ||
    code.includes("functions/unavailable") ||
    code.includes("functions/internal") ||
    code.includes("functions/deadline-exceeded") ||
    code.includes("functions/cancelled") ||
    code.includes("functions/unknown")
  );
};

async function callWithFallback<TReq, TRes>(
  callableName: string,
  payload: TReq,
  fallbackFn: () => Promise<TRes>
): Promise<TRes> {
  try {
    const callable = httpsCallable<TReq, TRes>(functions, callableName);
    const response = await callable(payload);
    return response.data;
  } catch (error: unknown) {
    if (shouldFallbackToClient(error)) {
      return fallbackFn();
    }
    throw error;
  }
}

async function queryRows(options: {
  maxResults: number;
  category?: GuideCategory;
  tenantId?: string | null;
}): Promise<Row[]> {
  const supabase = getSupabaseClient();
  const scopedTenantId = resolveGuideTenantId(options.tenantId);
  let query = supabase
    .from("guia_data")
    .select(GUIDE_SELECT_COLUMNS)
    .limit(options.maxResults);

  if (scopedTenantId) {
    query = query.eq("tenant_id", scopedTenantId);
  }

  if (options.category) {
    query = query.eq("categoria", options.category);
  }

  const { data, error } = await query;
  if (error) throwSupabaseError(error);
  return (data ?? []) as unknown as Row[];
}

export async function fetchGuideData(options?: {
  maxResults?: number;
  forceRefresh?: boolean;
  category?: GuideCategory;
  tenantId?: string | null;
}): Promise<Row[]> {
  const maxResults = boundedLimit(options?.maxResults ?? 600, MAX_GUIDE_ITEMS);
  const forceRefresh = options?.forceRefresh ?? false;
  const category = options?.category;
  const scopedTenantId = resolveGuideTenantId(options?.tenantId);
  const cacheKey = `${scopedTenantId || "default"}:${category ?? "all"}:${maxResults}`;

  if (!forceRefresh) {
    const cached = getCache(guideCache, cacheKey);
    if (cached) return cached;
  }

  const rows = await queryRows({ maxResults, category, tenantId: scopedTenantId });
  const sorted = [...rows].sort((left, right) => {
    const leftOrder =
      typeof left.ordem === "number" && Number.isFinite(left.ordem)
        ? left.ordem
        : Number.MAX_SAFE_INTEGER;
    const rightOrder =
      typeof right.ordem === "number" && Number.isFinite(right.ordem)
        ? right.ordem
        : Number.MAX_SAFE_INTEGER;
    if (leftOrder !== rightOrder) return leftOrder - rightOrder;

    const leftLabel =
      (typeof left.titulo === "string" ? left.titulo : "") ||
      (typeof left.nome === "string" ? left.nome : "");
    const rightLabel =
      (typeof right.titulo === "string" ? right.titulo : "") ||
      (typeof right.nome === "string" ? right.nome : "");
    return leftLabel.localeCompare(rightLabel, "pt-BR");
  });

  setCache(guideCache, cacheKey, sorted);
  return sorted;
}

export async function seedGuideDefaults(
  items: Row[],
  options?: { tenantId?: string | null }
): Promise<void> {
  if (!items.length) return;
  const scopedTenantId = resolveGuideTenantId(options?.tenantId);
  const scopedItems = items.map((item) => scopeGuideData(item, scopedTenantId));

  await callWithFallback<{ items: Row[]; tenantId?: string }, { ok: boolean }>(
    CALLABLE_GUIDE_SEED,
    {
      items: scopedItems,
      ...(scopedTenantId ? { tenantId: scopedTenantId } : {}),
    },
    async () => {
      const supabase = getSupabaseClient();
      const { error } = await supabase.from("guia_data").insert(scopedItems);
      if (error) throwSupabaseError(error);
      return { ok: true };
    }
  );

  guideCache.clear();
}

export async function upsertGuideItem(payload: {
  itemId?: string;
  data: Row;
  tenantId?: string | null;
}): Promise<void> {
  const itemId = payload.itemId?.trim() || "";
  const scopedTenantId = resolveGuideTenantId(payload.tenantId);
  const scopedData = scopeGuideData(payload.data, scopedTenantId);
  const requestPayload = {
    ...(itemId ? { itemId } : {}),
    ...(scopedTenantId ? { tenantId: scopedTenantId } : {}),
    data: scopedData,
  };

  await callWithFallback<typeof requestPayload, { ok: boolean }>(
    CALLABLE_GUIDE_UPSERT,
    requestPayload,
    async () => {
      const supabase = getSupabaseClient();
      if (itemId) {
        let query = supabase.from("guia_data").update(scopedData).eq("id", itemId);
        if (scopedTenantId) {
          query = query.eq("tenant_id", scopedTenantId);
        }
        const { error } = await query;
        if (error) throwSupabaseError(error);
      } else {
        const { error } = await supabase.from("guia_data").insert(scopedData);
        if (error) throwSupabaseError(error);
      }
      return { ok: true };
    }
  );

  guideCache.clear();
}

export async function deleteGuideItem(
  itemId: string,
  options?: { tenantId?: string | null }
): Promise<void> {
  const cleanId = itemId.trim();
  if (!cleanId) return;
  const scopedTenantId = resolveGuideTenantId(options?.tenantId);

  await callWithFallback<{ itemId: string; tenantId?: string }, { ok: boolean }>(
    CALLABLE_GUIDE_DELETE,
    {
      itemId: cleanId,
      ...(scopedTenantId ? { tenantId: scopedTenantId } : {}),
    },
    async () => {
      const supabase = getSupabaseClient();
      let query = supabase.from("guia_data").delete().eq("id", cleanId);
      if (scopedTenantId) {
        query = query.eq("tenant_id", scopedTenantId);
      }
      const { error } = await query;
      if (error) throwSupabaseError(error);
      return { ok: true };
    }
  );

  guideCache.clear();
}

export async function uploadGuidePhoto(file: File): Promise<string> {
  const sourceValidationError = validateGuidePhotoFile(file);
  if (sourceValidationError) {
    throw new Error(sourceValidationError);
  }

  const compressed = await compressImageFile(file, {
    maxWidth: 1600,
    maxHeight: 1600,
    maxBytes: GUIDE_PHOTO_COMPRESSED_MAX_BYTES,
    quality: 0.82,
    minQuality: 0.45,
  });

  const compressedValidationError = validateImageFile(compressed, {
    maxBytes: GUIDE_PHOTO_COMPRESSED_MAX_BYTES,
    allowedTypes: GUIDE_PHOTO_ACCEPTED_TYPES,
  });
  if (compressedValidationError) {
    throw new Error(compressedValidationError);
  }

  const storageRef = ref(storage, `guia/${Date.now()}_${compressed.name}`);
  await uploadBytes(storageRef, compressed, {
    cacheControl: "31536000",
    contentType: compressed.type || "image/webp",
  });
  return getDownloadURL(storageRef);
}

export function clearGuideCaches(): void {
  guideCache.clear();
}


