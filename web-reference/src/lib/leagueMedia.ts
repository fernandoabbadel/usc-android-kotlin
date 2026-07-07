type LeagueLogoSource = {
  logoUrl?: unknown;
  logo?: unknown;
  foto?: unknown;
};

const isRenderableImageValue = (value: string): boolean => {
  const normalized = value.trim();
  if (!normalized) return false;
  return (
    normalized.startsWith("/") ||
    normalized.startsWith("http://") ||
    normalized.startsWith("https://") ||
    normalized.startsWith("data:image/") ||
    normalized.startsWith("blob:")
  );
};

const normalizeImageValue = (value: unknown): string => {
  const normalized = typeof value === "string" ? value.trim() : "";
  return isRenderableImageValue(normalized) ? normalized : "";
};

export const resolveLeagueLogoSrc = (
  source?: LeagueLogoSource | null,
  fallback = ""
): string => {
  if (!source) return fallback;

  const candidates = [source.logoUrl, source.logo, source.foto]
    .map(normalizeImageValue)
    .filter((value) => value.length > 0);

  return candidates[0] || fallback;
};
