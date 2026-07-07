import { httpsCallable } from "@/lib/supa/functions";

import { functions } from "./backend";
import { getBackendErrorCode } from "./backendErrors";
import { throwSupabaseError } from "./supabaseData";
import { getSupabaseClient } from "./supabase";

type CacheEntry<T> = {
  cachedAt: number;
  value: T;
};

const READ_CACHE_TTL_MS = 20_000;
const DEFAULT_NOTIFICATION_RESULTS = 20;
const MAX_NOTIFICATION_RESULTS = 20;
const BANNED_APPEALS_FALLBACK_LIMIT = 500;
export const BOTTOM_NAV_NOTIFICATIONS_CHANGED_EVENT = "usc:notifications:changed";

const NOTIFICATION_READ_CALLABLE = "notificationsMarkRead";

const notificationsCache = new Map<string, CacheEntry<NotificationFeed>>();
let bannedAppealsCountCache: CacheEntry<number> | null = null;

export interface BottomNavNotification {
  id: string;
  title: string;
  message: string;
  link?: string;
  read: boolean;
  createdAt: unknown;
  expiresAt?: unknown;
}

export interface NotificationFeed {
  notifications: BottomNavNotification[];
  unreadCount: number;
  hasMore: boolean;
  nextOffset: number | null;
}

const asObject = (value: unknown): Record<string, unknown> | null => {
  if (typeof value !== "object" || value === null) return null;
  return value as Record<string, unknown>;
};

const asString = (value: unknown, fallback = ""): string =>
  typeof value === "string" ? value : fallback;

const asBoolean = (value: unknown, fallback = false): boolean =>
  typeof value === "boolean" ? value : fallback;

const boundedLimit = (requested: number): number => {
  if (!Number.isFinite(requested)) return DEFAULT_NOTIFICATION_RESULTS;
  if (requested < 1) return 1;
  if (requested > MAX_NOTIFICATION_RESULTS) return MAX_NOTIFICATION_RESULTS;
  return Math.floor(requested);
};

const boundedOffset = (requested?: number): number => {
  if (!Number.isFinite(requested ?? 0)) return 0;
  return Math.max(0, Math.floor(requested ?? 0));
};

const isMissingExpiresAtColumnError = (error: unknown): boolean => {
  const raw = asObject(error);
  const message = [raw?.message, raw?.details, raw?.hint]
    .map((entry) => (typeof entry === "string" ? entry.toLowerCase() : ""))
    .join(" ");
  return message.includes("expiresat") && message.includes("column");
};

const toMillis = (value: unknown): number => {
  if (value instanceof Date) return value.getTime();
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string") {
    const parsed = Date.parse(value);
    return Number.isNaN(parsed) ? 0 : parsed;
  }

  const obj = asObject(value);
  const toDate = obj?.toDate;
  if (typeof toDate === "function") {
    const parsed = toDate.call(value) as Date;
    if (parsed instanceof Date) return parsed.getTime();
  }

  return 0;
};

const getMapCacheValue = <T>(
  cache: Map<string, CacheEntry<T>>,
  key: string
): T | null => {
  const cached = cache.get(key);
  if (!cached) return null;
  if (Date.now() - cached.cachedAt > READ_CACHE_TTL_MS) {
    cache.delete(key);
    return null;
  }
  return cached.value;
};

const setMapCacheValue = <T>(
  cache: Map<string, CacheEntry<T>>,
  key: string,
  value: T
): void => {
  cache.set(key, { cachedAt: Date.now(), value });
};

const getCacheValue = <T>(cache: CacheEntry<T> | null): T | null => {
  if (!cache) return null;
  if (Date.now() - cache.cachedAt > READ_CACHE_TTL_MS) return null;
  return cache.value;
};

const shouldFallbackToClientWrites = (error: unknown): boolean => {
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

const isIndexRequiredError = (error: unknown): boolean => {
  const code = getBackendErrorCode(error)?.toLowerCase();
  if (code?.includes("failed-precondition")) return true;

  if (error instanceof Error) {
    const message = error.message.toLowerCase();
    return message.includes("index") && message.includes("query");
  }
  return false;
};

const isNetworkFetchFailure = (error: unknown): boolean => {
  const raw = asObject(error);
  const message = [
    error instanceof Error ? error.message : "",
    asString(raw?.message),
    asString(raw?.details),
    asString(raw?.hint),
  ]
    .filter((entry) => entry.length > 0)
    .join(" ")
    .toLowerCase();

  return (
    message.includes("failed to fetch") ||
    message.includes("networkerror") ||
    message.includes("load failed") ||
    message.includes("internet_disconnected")
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
    if (shouldFallbackToClientWrites(error)) {
      return fallbackFn();
    }
    throw error;
  }
}

async function fetchRowsWithFallback(payload: {
  userId: string;
  maxResults: number;
  offset: number;
}): Promise<Record<string, unknown>[]> {
  const supabase = getSupabaseClient();
  const requestLimit = payload.maxResults + 1;
  const from = payload.offset;
  const to = payload.offset + requestLimit - 1;

  try {
    const { data, error } = await supabase
      .from("notifications")
      .select("id,userId,title,message,link,read,createdAt,expiresAt")
      .eq("userId", payload.userId)
      .order("createdAt", { ascending: false })
      .range(from, to);
    if (error) throw error;
    return (data ?? []) as Record<string, unknown>[];
  } catch (error: unknown) {
    if (isNetworkFetchFailure(error)) {
      return [];
    }
    if (isMissingExpiresAtColumnError(error)) {
      const { data, error: retryError } = await supabase
        .from("notifications")
        .select("id,userId,title,message,link,read,createdAt")
        .eq("userId", payload.userId)
        .order("createdAt", { ascending: false })
        .range(from, to);
      if (retryError) throwSupabaseError(retryError);
      return (data ?? []) as Record<string, unknown>[];
    }
    if (!isIndexRequiredError(error)) {
      throwSupabaseError(error as { message: string; code?: string | null; name?: string | null });
    }
  }

  const { data: fallbackData, error: fallbackError } = await supabase
    .from("notifications")
    .select("id,userId,title,message,link,read,createdAt,expiresAt")
    .eq("userId", payload.userId)
    .range(from, to);
  if (fallbackError) {
    if (isNetworkFetchFailure(fallbackError)) {
      return [];
    }
    if (isMissingExpiresAtColumnError(fallbackError)) {
      const { data, error: retryError } = await supabase
        .from("notifications")
        .select("id,userId,title,message,link,read,createdAt")
        .eq("userId", payload.userId)
        .range(from, to);
      if (retryError) throwSupabaseError(retryError);
      return (data ?? []) as Record<string, unknown>[];
    }
    throwSupabaseError(fallbackError);
  }
  return (fallbackData ?? []) as Record<string, unknown>[];
}

const normalizeNotification = (
  id: string,
  raw: unknown
): BottomNavNotification | null => {
  const data = asObject(raw);
  if (!data) return null;

  const title = asString(data.title).trim();
  const message = asString(data.message).trim();
  if (!title && !message) return null;

  const link = asString(data.link).trim();

  return {
    id,
    title: title || "Atualizacao",
    message,
    ...(link ? { link } : {}),
    read: asBoolean(data.read, false),
    createdAt: data.createdAt ?? null,
    expiresAt: data.expiresAt ?? null,
  };
};

export async function fetchBottomNavNotifications(options: {
  userId: string;
  maxResults?: number;
  offset?: number;
  forceRefresh?: boolean;
}): Promise<NotificationFeed> {
  const userId = options.userId.trim();
  if (!userId) return { notifications: [], unreadCount: 0, hasMore: false, nextOffset: null };

  const maxResults = boundedLimit(options.maxResults ?? DEFAULT_NOTIFICATION_RESULTS);
  const offset = boundedOffset(options.offset);
  const forceRefresh = options.forceRefresh ?? false;
  const cacheKey = `${userId}:${maxResults}:${offset}`;

  if (!forceRefresh) {
    const cached = getMapCacheValue(notificationsCache, cacheKey);
    if (cached) return cached;
  }

  const rows = await fetchRowsWithFallback({ userId, maxResults, offset });

  const notifications = rows
    .map((row) => normalizeNotification(asString(row.id), row))
    .filter((row): row is BottomNavNotification => row !== null)
    .filter((row) => {
      const expiresAt = toMillis(row.expiresAt);
      return !expiresAt || expiresAt > Date.now();
    })
    .sort((left, right) => toMillis(right.createdAt) - toMillis(left.createdAt))
    .slice(0, maxResults);

  const feed: NotificationFeed = {
    unreadCount: notifications.reduce(
      (count, row) => (row.read ? count : count + 1),
      0
    ),
    notifications,
    hasMore: rows.length > maxResults,
    nextOffset: rows.length > maxResults ? offset + maxResults : null,
  };

  setMapCacheValue(notificationsCache, cacheKey, feed);
  return feed;
}

export async function fetchBottomNavBannedAppealsCount(options?: {
  forceRefresh?: boolean;
}): Promise<number> {
  const forceRefresh = options?.forceRefresh ?? false;
  if (!forceRefresh) {
    const cached = getCacheValue(bannedAppealsCountCache);
    if (cached !== null) return cached;
  }

  let count = 0;
  const supabase = getSupabaseClient();
  try {
    const { count: exactCount, error } = await supabase
      .from("banned_appeals")
      .select("id", { count: "exact", head: true })
      .eq("readByAdmin", false);
    if (error) throw error;
    count = exactCount ?? 0;
  } catch {
    const { data, error } = await supabase
      .from("banned_appeals")
      .select("id")
      .eq("readByAdmin", false)
      .limit(BANNED_APPEALS_FALLBACK_LIMIT);
    if (error) throwSupabaseError(error);
    count = (data ?? []).length;
  }

  bannedAppealsCountCache = { cachedAt: Date.now(), value: count };
  return count;
}

export async function markBottomNavNotificationRead(
  notificationId: string
): Promise<void> {
  const notificationIdClean = notificationId.trim();
  if (!notificationIdClean) return;

  await callWithFallback<{ notificationId: string }, { ok: boolean }>(
    NOTIFICATION_READ_CALLABLE,
    { notificationId: notificationIdClean },
    async () => {
      const supabase = getSupabaseClient();
      const { error } = await supabase
        .from("notifications")
        .update({ read: true, updatedAt: new Date().toISOString() })
        .eq("id", notificationIdClean);
      if (error) throwSupabaseError(error);
      return { ok: true };
    }
  );

  notificationsCache.clear();
}

export function clearBottomNavCaches(): void {
  notificationsCache.clear();
  bannedAppealsCountCache = null;
}

export function dispatchBottomNavNotificationsChanged(): void {
  clearBottomNavCaches();
  if (typeof window === "undefined") return;
  window.dispatchEvent(new Event(BOTTOM_NAV_NOTIFICATIONS_CHANGED_EVENT));
}


