import { asObject, asString } from "./supabaseData";

export const EVENT_VISIBILITY_BLOCK_REASON_MAX_LENGTH = 100;
export const EVENT_VISIBILITY_BLOCK_KEY = "adminVisibilityBlock";

export interface EventVisibilityBlock {
  hidden: boolean;
  reason: string;
  blockedAt: string;
  blockedBy: string;
  unblockedAt?: string;
  unblockedBy?: string;
}

const asBooleanLike = (value: unknown): boolean => {
  if (typeof value === "boolean") return value;
  if (typeof value === "string") {
    const normalized = value.trim().toLowerCase();
    return normalized === "true" || normalized === "1" || normalized === "sim";
  }
  if (typeof value === "number") return value === 1;
  return false;
};

export const sanitizeVisibilityBlockReason = (value: unknown): string =>
  asString(value)
    .trim()
    .replace(/\s+/g, " ")
    .slice(0, EVENT_VISIBILITY_BLOCK_REASON_MAX_LENGTH);

export const normalizeEventVisibilityBlock = (value: unknown): EventVisibilityBlock | null => {
  const raw = asObject(value);
  if (!raw) return null;

  const hidden = asBooleanLike(raw.hidden ?? raw.invisible ?? raw.blocked);
  const reason = sanitizeVisibilityBlockReason(raw.reason ?? raw.motivo ?? raw.message);
  const blockedAt = asString(raw.blockedAt || raw.createdAt || raw.updatedAt);
  const blockedBy = asString(raw.blockedBy || raw.actorUserId || raw.userId);
  const unblockedAt = asString(raw.unblockedAt);
  const unblockedBy = asString(raw.unblockedBy);

  return {
    hidden,
    reason,
    blockedAt,
    blockedBy,
    ...(unblockedAt ? { unblockedAt } : {}),
    ...(unblockedBy ? { unblockedBy } : {}),
  };
};

export const getEventVisibilityBlock = (event: unknown): EventVisibilityBlock | null => {
  const row = asObject(event);
  if (!row) return null;

  const stats = asObject(row.stats) ?? {};
  const dataExtra = asObject(row.data_extra ?? row.dataExtra) ?? {};

  return normalizeEventVisibilityBlock(
    stats[EVENT_VISIBILITY_BLOCK_KEY] ??
      stats.eventVisibilityBlock ??
      dataExtra[EVENT_VISIBILITY_BLOCK_KEY] ??
      dataExtra.eventVisibilityBlock
  );
};

export const isEventVisibilityBlocked = (event: unknown): boolean =>
  getEventVisibilityBlock(event)?.hidden === true;
