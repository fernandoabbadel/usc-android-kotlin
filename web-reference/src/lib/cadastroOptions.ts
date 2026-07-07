export type CadastroFieldKey =
  | "instagram"
  | "bio"
  | "statusRelacionamento"
  | "pets"
  | "esportes"
  | "signo"
  | "preferencias";

export type CadastroFieldConfig = {
  enabled: boolean;
  required: boolean;
};

export type CadastroFieldConfigMap = Record<CadastroFieldKey, CadastroFieldConfig>;

export type CadastroSportOption = {
  id: string;
  label: string;
  icon: string;
  enabled: boolean;
};

export type CadastroChoiceOption = {
  id: string;
  label: string;
  icon: string;
  enabled: boolean;
};

export type CadastroColorOption = {
  id: string;
  label: string;
  hex: string;
};

type InternalSportOption = CadastroSportOption & {
  aliases?: string[];
  colorClass?: string;
};

type SportPresentation = {
  id: string;
  label: string;
  emoji: string;
  colorClass: string;
};

const SPORT_COLOR_FALLBACKS = [
  "bg-emerald-500/20 text-emerald-400",
  "bg-blue-500/20 text-blue-300",
  "bg-orange-500/20 text-orange-300",
  "bg-cyan-500/20 text-cyan-300",
  "bg-pink-500/20 text-pink-300",
  "bg-violet-500/20 text-violet-300",
];

const DEFAULT_SPORTS_INTERNAL: InternalSportOption[] = [
  { id: "futebol", label: "Futebol", icon: "⚽", enabled: true, colorClass: "bg-green-500/20 text-green-400" },
  { id: "futsal", label: "Futsal", icon: "👟", enabled: true, colorClass: "bg-emerald-500/20 text-emerald-400" },
  { id: "volei", label: "Vôlei", icon: "🏐", enabled: true, colorClass: "bg-blue-400/20 text-blue-200" },
  { id: "basquete", label: "Basquete", icon: "🏀", enabled: true, colorClass: "bg-orange-500/20 text-orange-400" },
  {
    id: "handball",
    label: "Handball",
    icon: "🤾",
    enabled: true,
    aliases: ["handebol"],
    colorClass: "bg-red-500/20 text-red-400",
  },
  { id: "rugby", label: "Rugby", icon: "🏉", enabled: true, colorClass: "bg-amber-500/20 text-amber-400" },
  { id: "baseball", label: "Baseball", icon: "⚾", enabled: true, colorClass: "bg-zinc-700/40 text-zinc-200" },
  { id: "futevolei", label: "Futevôlei", icon: "🏐", enabled: true, colorClass: "bg-sky-500/20 text-sky-300" },
  { id: "beach_tennis", label: "Beach Tennis", icon: "🏖️", enabled: true, colorClass: "bg-yellow-600/20 text-yellow-400" },
  { id: "tenis", label: "Tênis", icon: "🎾", enabled: true, colorClass: "bg-lime-500/20 text-lime-300" },
  { id: "frescobol", label: "Frescobol", icon: "🏓", enabled: true, colorClass: "bg-cyan-500/20 text-cyan-300" },
  { id: "taco", label: "Taco (Bets)", icon: "🏏", enabled: true, colorClass: "bg-purple-500/20 text-purple-300" },
  { id: "peteca", label: "Peteca", icon: "🏸", enabled: true, colorClass: "bg-amber-400/20 text-amber-200" },
  { id: "surf", label: "Surf", icon: "🏄", enabled: true, colorClass: "bg-blue-500/20 text-blue-400" },
  { id: "natacao", label: "Natação", icon: "🏊", enabled: true, colorClass: "bg-cyan-500/20 text-cyan-400" },
  { id: "canoagem", label: "Canoagem", icon: "🛶", enabled: true, colorClass: "bg-blue-800/20 text-blue-300" },
  { id: "skate", label: "Skate", icon: "🛹", enabled: true, colorClass: "bg-zinc-700/40 text-zinc-200" },
  { id: "dog_walking", label: "Dog Walking", icon: "🐕", enabled: true, colorClass: "bg-orange-900/20 text-orange-300" },
  { id: "truco", label: "Truco", icon: "🃏", enabled: true, colorClass: "bg-rose-500/20 text-rose-300" },
  { id: "sinuca", label: "Sinuca", icon: "🎱", enabled: true, colorClass: "bg-zinc-700/40 text-zinc-200" },
];

const DEFAULT_SPORT_BY_ID = new Map(
  DEFAULT_SPORTS_INTERNAL.map((entry) => [entry.id, entry] as const)
);

export const DEFAULT_STATUS_RELACIONAMENTO_OPTIONS = [
  "Solteiro(a)",
  "Namorando",
  "Casado(a)",
  "Enrolado(a)",
] as const;

export const DEFAULT_PET_OPTIONS = [
  { id: "cachorro", label: "Cachorro", icon: "🐶" },
  { id: "gato", label: "Gato", icon: "🐱" },
  { id: "ambos", label: "Ambos", icon: "🐶🐱" },
  { id: "nenhum", label: "Sem Pet", icon: "🚫" },
] as const;

export const DEFAULT_SPECIAL_PLACE_OPTIONS: CadastroChoiceOption[] = [
  { id: "cafe", label: "Café", icon: "☕", enabled: true },
  { id: "bar", label: "Bar", icon: "🍻", enabled: true },
  { id: "biblioteca", label: "Biblioteca", icon: "📚", enabled: true },
  { id: "praia", label: "Praia", icon: "🏖️", enabled: true },
  { id: "cachoeira", label: "Cachoeira", icon: "💦", enabled: true },
  { id: "cinema", label: "Cinema", icon: "🎬", enabled: true },
  { id: "igreja", label: "Igreja", icon: "⛪", enabled: true },
  { id: "academia", label: "Academia", icon: "🏋️", enabled: true },
  { id: "trilha", label: "Trilha", icon: "🥾", enabled: true },
  { id: "teatro", label: "Teatro", icon: "🎭", enabled: true },
  { id: "karaoke", label: "Karaokê", icon: "🎤", enabled: true },
];

export const DEFAULT_FOOD_OPTIONS: CadastroChoiceOption[] = [
  { id: "japonesa", label: "Comida Japonesa", icon: "🍣", enabled: true },
  { id: "mexicana", label: "Comida Mexicana", icon: "🌮", enabled: true },
  { id: "tailandesa", label: "Comida Tailandesa", icon: "🍜", enabled: true },
  { id: "brasileira", label: "Comida Brasileira", icon: "🍛", enabled: true },
  { id: "italiana", label: "Comida Italiana", icon: "🍝", enabled: true },
  { id: "arabe", label: "Comida Árabe", icon: "🧆", enabled: true },
];

export const DEFAULT_MUSIC_OPTIONS: CadastroChoiceOption[] = [
  { id: "rock", label: "Rock", icon: "🎸", enabled: true },
  { id: "funk", label: "Funk", icon: "🔊", enabled: true },
  { id: "pop", label: "Pop", icon: "🎧", enabled: true },
  { id: "pagode", label: "Pagode", icon: "🪕", enabled: true },
  { id: "samba", label: "Samba", icon: "🥁", enabled: true },
  { id: "jazz", label: "Jazz", icon: "🎷", enabled: true },
  { id: "sertanejo", label: "Sertanejo", icon: "🤠", enabled: true },
  { id: "rap", label: "Rap", icon: "🎙️", enabled: true },
  { id: "axe", label: "Axé", icon: "🌞", enabled: true },
  { id: "piseiro", label: "Piseiro", icon: "🪗", enabled: true },
  { id: "eletronica", label: "Eletrônica", icon: "🪩", enabled: true },
  { id: "kpop", label: "K-pop", icon: "💿", enabled: true },
  { id: "reggae", label: "Reggae", icon: "🌿", enabled: true },
  { id: "gospel", label: "Gospel", icon: "✨", enabled: true },
  { id: "forro", label: "Forró", icon: "🪗", enabled: true },
  { id: "classica", label: "Clássica", icon: "🎻", enabled: true },
];

export const DEFAULT_COLOR_OPTIONS: CadastroColorOption[] = [
  { id: "preto", label: "Preto", hex: "#050505" },
  { id: "branco", label: "Branco", hex: "#f8fafc" },
  { id: "vermelho", label: "Vermelho", hex: "#ef4444" },
  { id: "laranja", label: "Laranja", hex: "#f97316" },
  { id: "amarelo", label: "Amarelo", hex: "#facc15" },
  { id: "verde", label: "Verde", hex: "#22c55e" },
  { id: "azul", label: "Azul", hex: "#3b82f6" },
  { id: "roxo", label: "Roxo", hex: "#8b5cf6" },
  { id: "rosa", label: "Rosa", hex: "#ec4899" },
  { id: "cinza", label: "Cinza", hex: "#71717a" },
];

export const getDefaultCadastroFieldConfig = (): CadastroFieldConfigMap => ({
  instagram: { enabled: true, required: false },
  bio: { enabled: true, required: false },
  statusRelacionamento: { enabled: true, required: false },
  pets: { enabled: true, required: false },
  esportes: { enabled: true, required: false },
  signo: { enabled: true, required: false },
  preferencias: { enabled: true, required: false },
});

const normalizeText = (value: string): string =>
  value
    .trim()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase();

const prettyLabelFromId = (id: string): string =>
  id
    .trim()
    .replace(/[_-]+/g, " ")
    .replace(/\s+/g, " ")
    .split(" ")
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ") || "Modalidade";

const hashString = (value: string): number => {
  let hash = 0;
  for (let index = 0; index < value.length; index += 1) {
    hash = (hash << 5) - hash + value.charCodeAt(index);
    hash |= 0;
  }
  return Math.abs(hash);
};

export const normalizeCadastroSportId = (value: string): string => {
  const normalized = normalizeText(value)
    .replace(/[^a-z0-9]+/g, "_")
    .replace(/^_+|_+$/g, "");
  return normalized || "modalidade";
};

export const normalizeCadastroChoiceId = (value: string): string => {
  const normalized = normalizeText(value)
    .replace(/[^a-z0-9]+/g, "_")
    .replace(/^_+|_+$/g, "");
  return normalized || "opcao";
};

export const normalizeCadastroSportOption = (
  value: Partial<CadastroSportOption>
): CadastroSportOption | null => {
  const rawLabel = typeof value.label === "string" ? value.label.trim().slice(0, 40) : "";
  const id = normalizeCadastroSportId(
    typeof value.id === "string" && value.id.trim() ? value.id : rawLabel
  );
  if (!id) return null;

  const canonicalSport = DEFAULT_SPORT_BY_ID.get(id);
  const label =
    rawLabel && canonicalSport && normalizeText(rawLabel) === normalizeText(canonicalSport.label)
      ? canonicalSport.label
      : rawLabel || canonicalSport?.label || prettyLabelFromId(id);

  const icon =
    typeof value.icon === "string" && value.icon.trim().slice(0, 6)
      ? value.icon.trim().slice(0, 6)
      : canonicalSport?.icon || "🏅";

  return {
    id,
    label,
    icon,
    enabled: typeof value.enabled === "boolean" ? value.enabled : true,
  };
};

export const dedupeCadastroSportOptions = (
  options: readonly Partial<CadastroSportOption>[]
): CadastroSportOption[] => {
  const map = new Map<string, CadastroSportOption>();

  options.forEach((entry) => {
    const normalized = normalizeCadastroSportOption(entry);
    if (!normalized) return;
    map.set(normalized.id, normalized);
  });

  return Array.from(map.values()).sort((left, right) =>
    left.label.localeCompare(right.label, "pt-BR")
  );
};

export const normalizeCadastroChoiceOption = (
  value: Partial<CadastroChoiceOption>,
  fallbackIcon = "✨"
): CadastroChoiceOption | null => {
  const rawLabel = typeof value.label === "string" ? value.label.trim().slice(0, 44) : "";
  const id = normalizeCadastroChoiceId(
    typeof value.id === "string" && value.id.trim() ? value.id : rawLabel
  );
  if (!id) return null;

  return {
    id,
    label: rawLabel || prettyLabelFromId(id),
    icon:
      typeof value.icon === "string" && value.icon.trim()
        ? value.icon.trim().slice(0, 8)
        : fallbackIcon,
    enabled: typeof value.enabled === "boolean" ? value.enabled : true,
  };
};

export const dedupeCadastroChoiceOptions = (
  options: readonly Partial<CadastroChoiceOption>[],
  fallbackIcon = "✨"
): CadastroChoiceOption[] => {
  const map = new Map<string, CadastroChoiceOption>();

  options.forEach((entry) => {
    const normalized = normalizeCadastroChoiceOption(entry, fallbackIcon);
    if (!normalized) return;
    map.set(normalized.id, normalized);
  });

  return Array.from(map.values()).sort((left, right) =>
    left.label.localeCompare(right.label, "pt-BR")
  );
};

export const getDefaultCadastroSportOptions = (): CadastroSportOption[] =>
  DEFAULT_SPORTS_INTERNAL.map(({ id, label, icon, enabled }) => ({
    id,
    label,
    icon,
    enabled,
  }));

export const getDefaultSpecialPlaceOptions = (): CadastroChoiceOption[] =>
  DEFAULT_SPECIAL_PLACE_OPTIONS.map((entry) => ({ ...entry }));

export const getDefaultFoodOptions = (): CadastroChoiceOption[] =>
  DEFAULT_FOOD_OPTIONS.map((entry) => ({ ...entry }));

export const getDefaultMusicOptions = (): CadastroChoiceOption[] =>
  DEFAULT_MUSIC_OPTIONS.map((entry) => ({ ...entry }));

export const getDefaultColorOptions = (): CadastroColorOption[] =>
  DEFAULT_COLOR_OPTIONS.map((entry) => ({ ...entry }));

const buildSportRegistry = (options?: readonly Partial<CadastroSportOption>[]) => {
  const merged = dedupeCadastroSportOptions([...DEFAULT_SPORTS_INTERNAL, ...(options ?? [])]);
  const metadataById = new Map(
    DEFAULT_SPORTS_INTERNAL.map((entry) => [entry.id, entry] as const)
  );
  const registry = new Map<string, SportPresentation>();

  merged.forEach((entry) => {
    const internalEntry = metadataById.get(entry.id);
    const colorClass =
      internalEntry?.colorClass ||
      SPORT_COLOR_FALLBACKS[hashString(entry.id) % SPORT_COLOR_FALLBACKS.length];
    const presentation: SportPresentation = {
      id: entry.id,
      label: entry.label,
      emoji: entry.icon,
      colorClass,
    };
    registry.set(normalizeText(entry.id), presentation);
    registry.set(normalizeText(entry.label), presentation);

    if (internalEntry?.aliases?.length) {
      internalEntry.aliases.forEach((alias) => {
        registry.set(normalizeText(alias), presentation);
      });
    }
  });

  return registry;
};

export const normalizeSelectedSportIds = (
  values: readonly string[],
  options?: readonly Partial<CadastroSportOption>[]
): string[] => {
  const registry = buildSportRegistry(options);
  const unique = new Set<string>();

  values.forEach((value) => {
    const normalized = normalizeText(value);
    if (!normalized) return;
    const match = registry.get(normalized);
    unique.add(match?.id || normalizeCadastroSportId(value));
  });

  return Array.from(unique);
};

export const getSportPresentation = (
  value: string,
  options?: readonly Partial<CadastroSportOption>[]
): SportPresentation => {
  const registry = buildSportRegistry(options);
  const normalized = normalizeText(value);
  const existing = normalized ? registry.get(normalized) : null;
  if (existing) return existing;

  const id = normalizeCadastroSportId(value);
  return {
    id,
    label: prettyLabelFromId(id),
    emoji: "🏅",
    colorClass: SPORT_COLOR_FALLBACKS[hashString(id) % SPORT_COLOR_FALLBACKS.length],
  };
};
