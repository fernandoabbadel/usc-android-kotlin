import {
  LEGAL_LAST_UPDATED,
  LEGAL_NAV_ITEMS,
  legalDocumentsBySlug,
  type LegalDocument,
  type LegalSection,
} from "@/components/legal/legalContent";
import { getSupabaseClient } from "@/lib/supabase";

export type PlatformLegalSurface = "landing" | "app" | "all";

export type PlatformLegalDocument = {
  id: string;
  slug: string;
  href: string;
  label: string;
  title: string;
  description: string;
  content: string;
  lastUpdated: string;
  visibleOnLanding: boolean;
  visibleInApp: boolean;
  defaultDocument: boolean;
};

const DEFAULT_LANDING_SLUGS = new Set(["politica-privacidade", "termos-de-servico"]);
const MAX_LEGAL_DOCUMENTS = 80;
const MAX_SHORT_FIELD_LENGTH = 180;
const MAX_DESCRIPTION_LENGTH = 500;
const MAX_CONTENT_LENGTH = 160_000;

const defaultHrefBySlug = new Map(
  LEGAL_NAV_ITEMS.map((item) => [item.href.replace(/^\//, ""), item.href])
);

const asObject = (value: unknown): Record<string, unknown> | null =>
  typeof value === "object" && value !== null ? (value as Record<string, unknown>) : null;

const asString = (value: unknown, fallback = ""): string =>
  typeof value === "string" ? value : fallback;

const asBoolean = (value: unknown, fallback = false): boolean =>
  typeof value === "boolean" ? value : fallback;

const trimField = (value: unknown, maxLength: number, fallback = ""): string =>
  asString(value, fallback).trim().slice(0, maxLength);

export const slugifyLegalDocument = (value: string): string => {
  const normalized = value
    .trim()
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 80);

  return normalized || "documento-legal";
};

export const buildLegalDocumentHref = (slug: string): string => {
  const cleanSlug = slugifyLegalDocument(slug);
  return defaultHrefBySlug.get(cleanSlug) || `/legal/${cleanSlug}`;
};

const flattenLegalSection = (section: LegalSection): string => {
  const parts: string[] = [`## ${section.title}`];

  if (section.body?.length) {
    parts.push(section.body.join("\n\n"));
  }

  if (section.bullets?.length) {
    parts.push(section.bullets.map((bullet) => `- ${bullet}`).join("\n"));
  }

  if (section.items?.length) {
    parts.push(
      section.items
        .map((item) => `${item.label}\n${item.text}`)
        .join("\n\n")
    );
  }

  if (section.note) {
    parts.push(`Nota: ${section.note}`);
  }

  return parts.filter(Boolean).join("\n\n");
};

export const flattenLegalDocument = (document: LegalDocument): string =>
  document.sections.map(flattenLegalSection).join("\n\n");

export const defaultPlatformLegalDocuments = (): PlatformLegalDocument[] =>
  LEGAL_NAV_ITEMS.map((item) => {
    const slug = item.href.replace(/^\//, "");
    const document = legalDocumentsBySlug[slug as keyof typeof legalDocumentsBySlug];
    const title = document?.title || item.label;

    return {
      id: slug,
      slug,
      href: item.href,
      label: item.label,
      title,
      description: document?.description || item.label,
      content: document ? flattenLegalDocument(document) : "",
      lastUpdated: document?.lastUpdated || LEGAL_LAST_UPDATED,
      visibleOnLanding: DEFAULT_LANDING_SLUGS.has(slug),
      visibleInApp: true,
      defaultDocument: true,
    };
  });

const normalizeDocument = (
  entry: unknown,
  index: number,
  seenSlugs: Set<string>
): PlatformLegalDocument | null => {
  const obj = asObject(entry);
  if (!obj) return null;

  const fallbackTitle = `Documento legal ${index + 1}`;
  const rawTitle = trimField(obj.title, MAX_SHORT_FIELD_LENGTH, fallbackTitle);
  const rawSlug = trimField(obj.slug, 100) || rawTitle;
  let slug = slugifyLegalDocument(rawSlug);
  if (seenSlugs.has(slug)) {
    slug = `${slug}-${index + 1}`;
  }
  seenSlugs.add(slug);

  const fallbackHref = buildLegalDocumentHref(slug);
  const href = trimField(obj.href, 160) || fallbackHref;
  const safeHref = href.startsWith("/") ? href : fallbackHref;

  return {
    id: trimField(obj.id, 120, slug) || slug,
    slug,
    href: safeHref,
    label: trimField(obj.label, MAX_SHORT_FIELD_LENGTH, rawTitle) || rawTitle,
    title: rawTitle,
    description: trimField(obj.description, MAX_DESCRIPTION_LENGTH, ""),
    content: trimField(obj.content, MAX_CONTENT_LENGTH, ""),
    lastUpdated: trimField(obj.lastUpdated, 40, LEGAL_LAST_UPDATED) || LEGAL_LAST_UPDATED,
    visibleOnLanding: asBoolean(obj.visibleOnLanding, false),
    visibleInApp: asBoolean(obj.visibleInApp, true),
    defaultDocument: asBoolean(
      obj.defaultDocument,
      Object.prototype.hasOwnProperty.call(legalDocumentsBySlug, slug)
    ),
  };
};

export const sanitizePlatformLegalDocuments = (rawDocuments: unknown): PlatformLegalDocument[] => {
  if (!Array.isArray(rawDocuments)) return defaultPlatformLegalDocuments();

  const seenSlugs = new Set<string>();
  return rawDocuments
    .slice(0, MAX_LEGAL_DOCUMENTS)
    .map((entry, index) => normalizeDocument(entry, index, seenSlugs))
    .filter((document): document is PlatformLegalDocument => Boolean(document));
};

export const extractPlatformLegalDocuments = (raw: unknown): PlatformLegalDocument[] => {
  const obj = asObject(raw);
  if (obj && Array.isArray(obj.documents)) {
    return sanitizePlatformLegalDocuments(obj.documents);
  }

  if (Array.isArray(raw)) {
    return sanitizePlatformLegalDocuments(raw);
  }

  return defaultPlatformLegalDocuments();
};

export const filterPlatformLegalDocuments = (
  documents: PlatformLegalDocument[],
  surface: PlatformLegalSurface
): PlatformLegalDocument[] => {
  if (surface === "landing") {
    return documents.filter((document) => document.visibleOnLanding);
  }

  if (surface === "app") {
    return documents.filter((document) => document.visibleInApp);
  }

  return documents;
};

export const getFallbackPlatformLegalDocumentsForSurface = (
  surface: PlatformLegalSurface
): PlatformLegalDocument[] =>
  filterPlatformLegalDocuments(defaultPlatformLegalDocuments(), surface);

const contentToSections = (content: string): LegalSection[] => {
  const cleanContent = content.trim();
  if (!cleanContent) {
    return [
      {
        title: "Conteúdo",
        body: ["Documento sem conteúdo publicado no momento."],
      },
    ];
  }

  const sections: LegalSection[] = [];
  const blocks = cleanContent.split(/\n(?=##\s+)/g);

  blocks.forEach((block, index) => {
    const lines = block.split(/\r?\n/);
    const firstLine = lines[0]?.trim() || "";
    const hasHeading = firstLine.startsWith("## ");
    const title = hasHeading ? firstLine.replace(/^##\s+/, "").trim() : "Conteúdo";
    const bodyText = (hasHeading ? lines.slice(1) : lines)
      .join("\n")
      .trim();

    const paragraphs = bodyText
      .split(/\n{2,}/)
      .map((paragraph) => paragraph.trim())
      .filter(Boolean);

    sections.push({
      title: title || `Seção ${index + 1}`,
      body: paragraphs.length > 0 ? paragraphs : [" "],
    });
  });

  return sections.length > 0 ? sections : [{ title: "Conteúdo", body: [cleanContent] }];
};

export const platformLegalDocumentToLegalDocument = (
  document: PlatformLegalDocument
): LegalDocument => ({
  slug: document.slug,
  title: document.title,
  description: document.description || document.label,
  lastUpdated: document.lastUpdated || LEGAL_LAST_UPDATED,
  sections: contentToSections(document.content),
});

const getAccessToken = async (): Promise<string> => {
  const supabase = getSupabaseClient();
  const { data, error } = await supabase.auth.getSession();
  if (error || !data.session?.access_token) {
    throw new Error("Sessão inválida. Entre novamente para editar os documentos.");
  }
  return data.session.access_token;
};

export const fetchPublicPlatformLegalDocuments = async (options?: {
  surface?: PlatformLegalSurface;
  includeContent?: boolean;
  forceRefresh?: boolean;
}): Promise<PlatformLegalDocument[]> => {
  const params = new URLSearchParams({
    surface: options?.surface || "app",
  });
  if (options?.includeContent) params.set("content", "1");
  if (options?.forceRefresh) params.set("refresh", "1");

  const response = await fetch(`/api/public/legal-documents?${params.toString()}`, {
    cache: "no-store",
  });
  if (!response.ok) {
    throw new Error("Não foi possível carregar os documentos legais.");
  }

  const payload = (await response.json()) as { documents?: unknown };
  return sanitizePlatformLegalDocuments(payload.documents || []);
};

export const fetchAdminPlatformLegalDocuments = async (): Promise<PlatformLegalDocument[]> => {
  const token = await getAccessToken();
  const response = await fetch("/api/master/privacy", {
    cache: "no-store",
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
  if (!response.ok) {
    const payload = (await response.json().catch(() => null)) as { error?: string } | null;
    throw new Error(payload?.error || "Não foi possível carregar os documentos legais.");
  }

  const payload = (await response.json()) as { documents?: unknown };
  return sanitizePlatformLegalDocuments(payload.documents || []);
};

export const saveAdminPlatformLegalDocuments = async (
  documents: PlatformLegalDocument[]
): Promise<PlatformLegalDocument[]> => {
  const token = await getAccessToken();
  const normalized = sanitizePlatformLegalDocuments(documents);
  const response = await fetch("/api/master/privacy", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({ documents: normalized }),
  });

  if (!response.ok) {
    const payload = (await response.json().catch(() => null)) as { error?: string } | null;
    throw new Error(payload?.error || "Não foi possível salvar os documentos legais.");
  }

  const payload = (await response.json()) as { documents?: unknown };
  return sanitizePlatformLegalDocuments(payload.documents || []);
};
