export type ZodiacElement = "Fogo" | "Terra" | "Ar" | "Água";

export type ZodiacSignOption = {
  id: string;
  label: string;
  emoji: string;
  element: ZodiacElement;
};

export type ZodiacCompatibilityReason =
  | "same-sign"
  | "ascendant"
  | "complementary"
  | "same-element";

export type ZodiacCompatibilityMatch = {
  reason: ZodiacCompatibilityReason;
  label: string;
};

export type AstroStats = {
  total: number;
  sameSignPercent: number;
  signPercentages: Record<string, number>;
  elementPercentages: Record<ZodiacElement, number>;
};

export const ZODIAC_SIGNS: ZodiacSignOption[] = [
  { id: "aries", label: "Áries", emoji: "♈", element: "Fogo" },
  { id: "touro", label: "Touro", emoji: "♉", element: "Terra" },
  { id: "gemeos", label: "Gêmeos", emoji: "♊", element: "Ar" },
  { id: "cancer", label: "Câncer", emoji: "♋", element: "Água" },
  { id: "leao", label: "Leão", emoji: "♌", element: "Fogo" },
  { id: "virgem", label: "Virgem", emoji: "♍", element: "Terra" },
  { id: "libra", label: "Libra", emoji: "♎", element: "Ar" },
  { id: "escorpiao", label: "Escorpião", emoji: "♏", element: "Água" },
  { id: "sagitario", label: "Sagitário", emoji: "♐", element: "Fogo" },
  { id: "capricornio", label: "Capricórnio", emoji: "♑", element: "Terra" },
  { id: "aquario", label: "Aquário", emoji: "♒", element: "Ar" },
  { id: "peixes", label: "Peixes", emoji: "♓", element: "Água" },
];

export const ZODIAC_ELEMENT_EMOJI: Record<ZodiacElement, string> = {
  Fogo: "🔥",
  Terra: "🌱",
  Ar: "🌬️",
  Água: "💧",
};

const COMPLEMENTARY_SIGNS: Record<string, string> = {
  aries: "libra",
  libra: "aries",
  touro: "escorpiao",
  escorpiao: "touro",
  gemeos: "sagitario",
  sagitario: "gemeos",
  cancer: "capricornio",
  capricornio: "cancer",
  leao: "aquario",
  aquario: "leao",
  virgem: "peixes",
  peixes: "virgem",
};

const normalizeText = (value: string): string =>
  value
    .trim()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "");

const ZODIAC_BY_ID = new Map(ZODIAC_SIGNS.map((sign) => [sign.id, sign] as const));
const ZODIAC_BY_NORMALIZED = new Map(
  ZODIAC_SIGNS.flatMap((sign) => [
    [normalizeText(sign.id), sign] as const,
    [normalizeText(sign.label), sign] as const,
  ])
);

export const normalizeZodiacSign = (value?: string | null): string => {
  const normalized = normalizeText(String(value || ""));
  if (!normalized) return "";
  return ZODIAC_BY_NORMALIZED.get(normalized)?.id || "";
};

export const getZodiacSignPresentation = (
  value?: string | null
): ZodiacSignOption | null => {
  const id = normalizeZodiacSign(value);
  return id ? ZODIAC_BY_ID.get(id) || null : null;
};

export const inferZodiacSignFromBirthDate = (value?: string | null): string => {
  const date = String(value || "").trim();
  const match = date.match(/^(\d{4})-(\d{2})-(\d{2})/);
  if (!match) return "";

  const month = Number(match[2]);
  const day = Number(match[3]);
  if (!Number.isFinite(month) || !Number.isFinite(day)) return "";

  if ((month === 3 && day >= 21) || (month === 4 && day <= 19)) return "aries";
  if ((month === 4 && day >= 20) || (month === 5 && day <= 20)) return "touro";
  if ((month === 5 && day >= 21) || (month === 6 && day <= 20)) return "gemeos";
  if ((month === 6 && day >= 21) || (month === 7 && day <= 22)) return "cancer";
  if ((month === 7 && day >= 23) || (month === 8 && day <= 22)) return "leao";
  if ((month === 8 && day >= 23) || (month === 9 && day <= 22)) return "virgem";
  if ((month === 9 && day >= 23) || (month === 10 && day <= 22)) return "libra";
  if ((month === 10 && day >= 23) || (month === 11 && day <= 21)) return "escorpiao";
  if ((month === 11 && day >= 22) || (month === 12 && day <= 21)) return "sagitario";
  if ((month === 12 && day >= 22) || (month === 1 && day <= 19)) return "capricornio";
  if ((month === 1 && day >= 20) || (month === 2 && day <= 18)) return "aquario";
  if ((month === 2 && day >= 19) || (month === 3 && day <= 20)) return "peixes";
  return "";
};

const sameNormalizedSign = (left?: string | null, right?: string | null): boolean => {
  const leftId = normalizeZodiacSign(left);
  const rightId = normalizeZodiacSign(right);
  return Boolean(leftId && rightId && leftId === rightId);
};

export const resolveZodiacCompatibility = (payload: {
  viewerSign?: string | null;
  viewerAscendant?: string | null;
  targetSign?: string | null;
  targetAscendant?: string | null;
}): ZodiacCompatibilityMatch[] => {
  const viewerSign = getZodiacSignPresentation(payload.viewerSign);
  const viewerAscendant = getZodiacSignPresentation(payload.viewerAscendant);
  const targetSign = getZodiacSignPresentation(payload.targetSign);
  const targetAscendant = getZodiacSignPresentation(payload.targetAscendant);
  if (!viewerSign || !targetSign) return [];

  const matches: ZodiacCompatibilityMatch[] = [];

  if (viewerSign.id === targetSign.id) {
    matches.push({ reason: "same-sign", label: "Signos iguais" });
  }

  if (
    sameNormalizedSign(viewerAscendant?.id, targetSign.id) ||
    sameNormalizedSign(targetAscendant?.id, viewerSign.id)
  ) {
    matches.push({ reason: "ascendant", label: "Conexão por ascendente" });
  }

  if (COMPLEMENTARY_SIGNS[viewerSign.id] === targetSign.id) {
    matches.push({ reason: "complementary", label: "Signos complementares" });
  }

  if (viewerSign.element === targetSign.element) {
    matches.push({ reason: "same-element", label: `Mesmo elemento: ${viewerSign.element}` });
  }

  return matches;
};

export const calculateAstroStats = (
  signs: readonly string[],
  viewerSign?: string | null
): AstroStats => {
  const signCounts = new Map<string, number>();
  const elementCounts = new Map<ZodiacElement, number>();
  const total = signs.length;

  signs.forEach((value) => {
    const sign = getZodiacSignPresentation(value);
    if (!sign) return;
    signCounts.set(sign.id, (signCounts.get(sign.id) || 0) + 1);
    elementCounts.set(sign.element, (elementCounts.get(sign.element) || 0) + 1);
  });

  const signPercentages: Record<string, number> = {};
  ZODIAC_SIGNS.forEach((sign) => {
    signPercentages[sign.id] = total ? Math.round(((signCounts.get(sign.id) || 0) / total) * 100) : 0;
  });

  const elementPercentages: Record<ZodiacElement, number> = {
    Fogo: total ? Math.round(((elementCounts.get("Fogo") || 0) / total) * 100) : 0,
    Terra: total ? Math.round(((elementCounts.get("Terra") || 0) / total) * 100) : 0,
    Ar: total ? Math.round(((elementCounts.get("Ar") || 0) / total) * 100) : 0,
    Água: total ? Math.round(((elementCounts.get("Água") || 0) / total) * 100) : 0,
  };

  const viewerSignId = normalizeZodiacSign(viewerSign);
  return {
    total,
    sameSignPercent: viewerSignId ? signPercentages[viewerSignId] || 0 : 0,
    signPercentages,
    elementPercentages,
  };
};
