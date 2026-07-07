"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  ArrowLeft,
  ExternalLink,
  Eye,
  EyeOff,
  FilePlus,
  FileText,
  Loader2,
  Pencil,
  RefreshCw,
  Save,
  ShieldCheck,
  Trash2,
} from "lucide-react";

import { useAuth } from "@/context/AuthContext";
import { useToast } from "@/context/ToastContext";
import {
  buildLegalDocumentHref,
  fetchAdminPlatformLegalDocuments,
  saveAdminPlatformLegalDocuments,
  slugifyLegalDocument,
  type PlatformLegalDocument,
} from "@/lib/platformLegalDocuments";
import { isPlatformMaster } from "@/lib/roles";

const todayLabel = (): string =>
  new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(new Date());

const sortDocuments = (documents: PlatformLegalDocument[]): PlatformLegalDocument[] =>
  [...documents].sort((left, right) => left.label.localeCompare(right.label, "pt-BR"));

const makeUniqueSlug = (base: string, documents: PlatformLegalDocument[]): string => {
  const cleanBase = slugifyLegalDocument(base);
  const used = new Set(documents.map((document) => document.slug));
  if (!used.has(cleanBase)) return cleanBase;

  let counter = 2;
  while (used.has(`${cleanBase}-${counter}`)) {
    counter += 1;
  }

  return `${cleanBase}-${counter}`;
};

const createDraftDocument = (documents: PlatformLegalDocument[]): PlatformLegalDocument => {
  const slug = makeUniqueSlug("novo-termo", documents);

  return {
    id: slug,
    slug,
    href: buildLegalDocumentHref(slug),
    label: "Novo termo",
    title: "Novo termo",
    description: "Documento legal da USC.",
    content: "Escreva o conteúdo deste documento.",
    lastUpdated: todayLabel(),
    visibleOnLanding: false,
    visibleInApp: true,
    defaultDocument: false,
  };
};

const VisibilityBadge = ({
  active,
  label,
  color,
}: {
  active: boolean;
  label: string;
  color: "emerald" | "blue";
}) => {
  const activeClass =
    color === "emerald"
      ? "border-emerald-400/30 bg-emerald-500/10 text-emerald-200"
      : "border-blue-400/30 bg-blue-500/10 text-blue-200";

  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full border px-2 py-1 text-[9px] font-black uppercase tracking-[0.12em] ${
        active ? activeClass : "border-zinc-700 bg-zinc-900 text-zinc-500"
      }`}
    >
      {active ? <Eye size={11} /> : <EyeOff size={11} />}
      {label}
    </span>
  );
};

export function MasterLegalDocumentsHub() {
  const { user } = useAuth();
  const { addToast } = useToast();
  const [documents, setDocuments] = useState<PlatformLegalDocument[]>([]);
  const [loading, setLoading] = useState(true);

  const canAccess = isPlatformMaster(user);

  const loadDocuments = useCallback(async () => {
    try {
      setLoading(true);
      setDocuments(sortDocuments(await fetchAdminPlatformLegalDocuments()));
    } catch (error: unknown) {
      addToast(
        error instanceof Error ? error.message : "Erro ao carregar documentos legais.",
        "error"
      );
    } finally {
      setLoading(false);
    }
  }, [addToast]);

  useEffect(() => {
    if (!canAccess) return;
    void loadDocuments();
  }, [canAccess, loadDocuments]);

  if (!canAccess) return null;

  if (loading) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center text-red-300">
        <Loader2 className="animate-spin" />
      </div>
    );
  }

  const landingCount = documents.filter((document) => document.visibleOnLanding).length;
  const appCount = documents.filter((document) => document.visibleInApp).length;

  return (
    <div className="min-h-screen bg-[#050505] text-white">
      <header className="mb-6 rounded-[2rem] border border-red-500/15 bg-[linear-gradient(135deg,rgba(127,29,29,0.22),rgba(10,10,10,0.94)_52%,rgba(127,29,29,0.12))] p-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <Link
              href="/master"
              className="mb-4 inline-flex items-center gap-2 text-sm font-bold text-zinc-300 transition hover:text-white"
            >
              <ArrowLeft size={16} />
              Voltar ao master
            </Link>
            <p className="text-[10px] font-black uppercase tracking-[0.28em] text-red-200">
              Governança global
            </p>
            <h1 className="mt-2 flex items-center gap-3 text-3xl font-black uppercase tracking-tight text-white">
              <ShieldCheck className="text-red-300" /> Privacidade e termos
            </h1>
            <p className="mt-3 max-w-3xl text-sm leading-6 text-zinc-300">
              Cada documento agora tem uma página própria de edição. Documentos novos continuam
              gerando automaticamente uma página pública pelo slug configurado.
            </p>
          </div>

          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => void loadDocuments()}
              className="inline-flex items-center gap-2 rounded-xl border border-zinc-700 bg-black px-4 py-3 text-[11px] font-black uppercase tracking-[0.16em] text-zinc-300 transition hover:bg-zinc-900"
            >
              <RefreshCw size={15} />
              Atualizar
            </button>
            <Link
              href="/master/privacy/novo"
              className="inline-flex items-center gap-2 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-[11px] font-black uppercase tracking-[0.16em] text-red-100 transition hover:bg-red-500/15"
            >
              <FilePlus size={15} />
              Novo termo
            </Link>
          </div>
        </div>
      </header>

      <section className="mb-5 grid gap-3 md:grid-cols-3">
        <article className="rounded-2xl border border-zinc-800 bg-zinc-900/50 p-4">
          <p className="text-[10px] font-black uppercase tracking-[0.2em] text-zinc-500">
            Biblioteca
          </p>
          <p className="mt-2 text-2xl font-black text-white">{documents.length}</p>
        </article>
        <article className="rounded-2xl border border-emerald-500/20 bg-emerald-500/10 p-4">
          <p className="text-[10px] font-black uppercase tracking-[0.2em] text-emerald-200">
            Landing
          </p>
          <p className="mt-2 text-2xl font-black text-white">{landingCount}</p>
        </article>
        <article className="rounded-2xl border border-blue-500/20 bg-blue-500/10 p-4">
          <p className="text-[10px] font-black uppercase tracking-[0.2em] text-blue-200">
            Páginas públicas
          </p>
          <p className="mt-2 text-2xl font-black text-white">{appCount}</p>
        </article>
      </section>

      <main className="grid gap-3">
        {documents.map((document) => (
          <article
            key={document.slug}
            className="rounded-2xl border border-zinc-800 bg-zinc-900/50 p-4 transition hover:border-red-500/30"
          >
            <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
              <div className="flex min-w-0 items-start gap-3">
                <FileText
                  size={20}
                  className={document.defaultDocument ? "mt-1 text-blue-300" : "mt-1 text-red-300"}
                />
                <div className="min-w-0">
                  <h2 className="truncate text-base font-black uppercase text-white">
                    {document.label}
                  </h2>
                  <p className="mt-1 truncate font-mono text-[11px] text-zinc-500">
                    {document.href}
                  </p>
                  <p className="mt-2 max-w-4xl text-sm leading-6 text-zinc-400">
                    {document.description || document.title}
                  </p>
                  <div className="mt-3 flex flex-wrap gap-2">
                    <VisibilityBadge
                      active={document.visibleOnLanding}
                      label="Landing"
                      color="emerald"
                    />
                    <VisibilityBadge active={document.visibleInApp} label="Páginas" color="blue" />
                  </div>
                </div>
              </div>

              <div className="flex shrink-0 flex-wrap gap-2">
                <Link
                  href={document.href}
                  target="_blank"
                  className="inline-flex items-center gap-2 rounded-xl border border-zinc-700 bg-black px-4 py-3 text-[11px] font-black uppercase tracking-[0.14em] text-zinc-300 transition hover:bg-zinc-900"
                >
                  <ExternalLink size={14} />
                  Ver página
                </Link>
                <Link
                  href={`/master/privacy/${encodeURIComponent(document.slug)}`}
                  className="inline-flex items-center gap-2 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-[11px] font-black uppercase tracking-[0.14em] text-red-100 transition hover:bg-red-500/15"
                >
                  <Pencil size={14} />
                  Editar
                </Link>
              </div>
            </div>
          </article>
        ))}
      </main>
    </div>
  );
}

export function MasterLegalDocumentEdit({ routeSlug }: { routeSlug: string }) {
  const { user } = useAuth();
  const { addToast } = useToast();
  const router = useRouter();
  const [documents, setDocuments] = useState<PlatformLegalDocument[]>([]);
  const [selectedSlug, setSelectedSlug] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [dirty, setDirty] = useState(false);
  const [initializedDraft, setInitializedDraft] = useState(false);

  const canAccess = isPlatformMaster(user);
  const normalizedRouteSlug = slugifyLegalDocument(routeSlug || "novo");
  const isNewRoute = normalizedRouteSlug === "novo";

  const selectedDocument = useMemo(
    () => documents.find((document) => document.slug === selectedSlug) || null,
    [documents, selectedSlug]
  );

  const loadDocuments = useCallback(async () => {
    try {
      setLoading(true);
      const loadedDocuments = sortDocuments(await fetchAdminPlatformLegalDocuments());

      if (isNewRoute) {
        const newDocument = createDraftDocument(loadedDocuments);
        setDocuments(sortDocuments([...loadedDocuments, newDocument]));
        setSelectedSlug(newDocument.slug);
        setDirty(true);
        setInitializedDraft(true);
        return;
      }

      setDocuments(loadedDocuments);
      setSelectedSlug(
        loadedDocuments.some((document) => document.slug === normalizedRouteSlug)
          ? normalizedRouteSlug
          : loadedDocuments[0]?.slug || ""
      );
      setDirty(false);
    } catch (error: unknown) {
      addToast(
        error instanceof Error ? error.message : "Erro ao carregar documentos legais.",
        "error"
      );
    } finally {
      setLoading(false);
    }
  }, [addToast, isNewRoute, normalizedRouteSlug]);

  useEffect(() => {
    if (!canAccess) return;
    if (isNewRoute && initializedDraft) return;
    void loadDocuments();
  }, [canAccess, initializedDraft, isNewRoute, loadDocuments]);

  const updateDocument = (slug: string, patch: Partial<PlatformLegalDocument>) => {
    setDocuments((current) =>
      sortDocuments(
        current.map((document) =>
          document.slug === slug
            ? {
                ...document,
                ...patch,
                href: patch.slug ? buildLegalDocumentHref(patch.slug) : document.href,
              }
            : document
        )
      )
    );
    setDirty(true);
  };

  const handleChangeSlug = (value: string) => {
    if (!selectedDocument) return;
    const nextSlug = makeUniqueSlug(
      value || selectedDocument.title,
      documents.filter((document) => document.slug !== selectedDocument.slug)
    );

    setDocuments((current) =>
      sortDocuments(
        current.map((document) =>
          document.slug === selectedDocument.slug
            ? {
                ...document,
                id: document.defaultDocument ? document.id : nextSlug,
                slug: nextSlug,
                href: buildLegalDocumentHref(nextSlug),
              }
            : document
        )
      )
    );
    setSelectedSlug(nextSlug);
    setDirty(true);
  };

  const handleDeleteDocument = () => {
    if (!selectedDocument) return;
    if (!window.confirm(`Excluir "${selectedDocument.label}"?`)) return;

    setDocuments((current) =>
      current.filter((document) => document.slug !== selectedDocument.slug)
    );
    setDirty(true);
    setSelectedSlug("");
  };

  const handleSave = async () => {
    try {
      setSaving(true);
      const savedDocuments = sortDocuments(await saveAdminPlatformLegalDocuments(documents));
      setDocuments(savedDocuments);
      const nextSlug = selectedSlug || savedDocuments[0]?.slug || "";
      setSelectedSlug(
        savedDocuments.some((document) => document.slug === nextSlug)
          ? nextSlug
          : savedDocuments[0]?.slug || ""
      );
      setDirty(false);
      addToast("Documento legal salvo.", "success");
      if (!selectedSlug) {
        router.replace("/master/privacy");
      } else {
        router.replace(`/master/privacy/${encodeURIComponent(selectedSlug)}`, { scroll: false });
      }
    } catch (error: unknown) {
      addToast(error instanceof Error ? error.message : "Erro ao salvar documento.", "error");
    } finally {
      setSaving(false);
    }
  };

  if (!canAccess) return null;

  if (loading) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center text-red-300">
        <Loader2 className="animate-spin" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#050505] text-white">
      <header className="mb-6 rounded-[2rem] border border-red-500/15 bg-[linear-gradient(135deg,rgba(127,29,29,0.22),rgba(10,10,10,0.94)_52%,rgba(127,29,29,0.12))] p-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <Link
              href="/master/privacy"
              className="mb-4 inline-flex items-center gap-2 text-sm font-bold text-zinc-300 transition hover:text-white"
            >
              <ArrowLeft size={16} />
              Biblioteca de documentos
            </Link>
            <p className="text-[10px] font-black uppercase tracking-[0.28em] text-red-200">
              Página de edição
            </p>
            <h1 className="mt-2 flex items-center gap-3 text-3xl font-black uppercase tracking-tight text-white">
              <ShieldCheck className="text-red-300" />
              {selectedDocument?.label || "Documento legal"}
            </h1>
            {selectedDocument ? (
              <p className="mt-3 max-w-3xl text-sm leading-6 text-zinc-300">
                Página pública:{" "}
                <Link
                  href={selectedDocument.href}
                  target="_blank"
                  className="font-black text-red-100 underline decoration-red-500/40 underline-offset-4"
                >
                  {selectedDocument.href}
                </Link>
              </p>
            ) : null}
          </div>

          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => void loadDocuments()}
              disabled={saving}
              className="inline-flex items-center gap-2 rounded-xl border border-zinc-700 bg-black px-4 py-3 text-[11px] font-black uppercase tracking-[0.16em] text-zinc-300 transition hover:bg-zinc-900 disabled:opacity-60"
            >
              <RefreshCw size={15} />
              Recarregar
            </button>
            <button
              type="button"
              onClick={() => void handleSave()}
              disabled={saving || !dirty}
              className="inline-flex items-center gap-2 rounded-xl bg-red-600 px-4 py-3 text-[11px] font-black uppercase tracking-[0.16em] text-white transition hover:bg-red-500 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {saving ? <Loader2 size={15} className="animate-spin" /> : <Save size={15} />}
              Salvar
            </button>
          </div>
        </div>
      </header>

      <main className="grid gap-5 xl:grid-cols-[320px_1fr]">
        <aside className="rounded-[2rem] border border-zinc-800 bg-zinc-900/50 p-4">
          <div className="mb-4 flex items-center justify-between gap-3">
            <h2 className="text-xs font-black uppercase tracking-[0.22em] text-zinc-500">
              Documentos
            </h2>
            <span className="rounded-full border border-zinc-800 bg-black px-3 py-1 text-[10px] font-black text-zinc-400">
              {documents.length}
            </span>
          </div>

          <div className="space-y-2">
            {documents.map((document) => (
              <Link
                key={document.slug}
                href={`/master/privacy/${encodeURIComponent(document.slug)}`}
                className={`block rounded-2xl border p-4 text-left transition ${
                  selectedSlug === document.slug
                    ? "border-red-500/40 bg-red-500/10"
                    : "border-zinc-800 bg-black/35 hover:border-zinc-700"
                }`}
              >
                <div className="flex items-start gap-3">
                  <FileText
                    size={18}
                    className={document.defaultDocument ? "text-blue-300" : "text-red-300"}
                  />
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-black uppercase text-white">
                      {document.label}
                    </p>
                    <p className="mt-1 truncate font-mono text-[10px] text-zinc-500">
                      {document.href}
                    </p>
                    <div className="mt-3 flex flex-wrap gap-2">
                      <VisibilityBadge
                        active={document.visibleOnLanding}
                        label="Landing"
                        color="emerald"
                      />
                      <VisibilityBadge active={document.visibleInApp} label="Páginas" color="blue" />
                    </div>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        </aside>

        <section className="min-h-[70vh] rounded-[2rem] border border-zinc-800 bg-zinc-900/50 p-5">
          {selectedDocument ? (
            <div className="grid gap-5">
              <div className="flex flex-wrap items-center justify-between gap-3 border-b border-zinc-800 pb-4">
                <div>
                  <p className="text-[10px] font-black uppercase tracking-[0.22em] text-zinc-500">
                    Documento selecionado
                  </p>
                  <h2 className="mt-1 text-xl font-black uppercase text-white">
                    {selectedDocument.label}
                  </h2>
                </div>
                <button
                  type="button"
                  onClick={handleDeleteDocument}
                  className="inline-flex items-center gap-2 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-2 text-[11px] font-black uppercase tracking-[0.16em] text-red-200 transition hover:bg-red-500/15"
                >
                  <Trash2 size={14} />
                  Excluir
                </button>
              </div>

              <div className="grid gap-4 lg:grid-cols-2">
                <label className="grid gap-2">
                  <span className="text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">
                    Nome do link
                  </span>
                  <input
                    value={selectedDocument.label}
                    onChange={(event) =>
                      updateDocument(selectedDocument.slug, { label: event.target.value })
                    }
                    className="rounded-xl border border-zinc-800 bg-black px-4 py-3 text-sm font-bold text-white outline-none transition focus:border-red-400"
                  />
                </label>

                <label className="grid gap-2">
                  <span className="text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">
                    Slug
                  </span>
                  <input
                    value={selectedDocument.slug}
                    onChange={(event) => handleChangeSlug(event.target.value)}
                    className="rounded-xl border border-zinc-800 bg-black px-4 py-3 font-mono text-sm font-bold text-red-100 outline-none transition focus:border-red-400"
                  />
                </label>

                <label className="grid gap-2 lg:col-span-2">
                  <span className="text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">
                    Título
                  </span>
                  <input
                    value={selectedDocument.title}
                    onChange={(event) =>
                      updateDocument(selectedDocument.slug, { title: event.target.value })
                    }
                    className="rounded-xl border border-zinc-800 bg-black px-4 py-3 text-sm font-bold text-white outline-none transition focus:border-red-400"
                  />
                </label>

                <label className="grid gap-2 lg:col-span-2">
                  <span className="text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">
                    Descrição
                  </span>
                  <input
                    value={selectedDocument.description}
                    onChange={(event) =>
                      updateDocument(selectedDocument.slug, { description: event.target.value })
                    }
                    className="rounded-xl border border-zinc-800 bg-black px-4 py-3 text-sm font-bold text-white outline-none transition focus:border-red-400"
                  />
                </label>

                <label className="grid gap-2">
                  <span className="text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">
                    Última atualização
                  </span>
                  <input
                    value={selectedDocument.lastUpdated}
                    onChange={(event) =>
                      updateDocument(selectedDocument.slug, { lastUpdated: event.target.value })
                    }
                    className="rounded-xl border border-zinc-800 bg-black px-4 py-3 text-sm font-bold text-white outline-none transition focus:border-red-400"
                  />
                </label>

                <div className="grid gap-2">
                  <span className="text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">
                    Visibilidade
                  </span>
                  <div className="grid gap-2 sm:grid-cols-2">
                    <button
                      type="button"
                      onClick={() =>
                        updateDocument(selectedDocument.slug, {
                          visibleOnLanding: !selectedDocument.visibleOnLanding,
                        })
                      }
                      className={`inline-flex min-h-12 items-center justify-center gap-2 rounded-xl border px-3 py-2 text-[10px] font-black uppercase tracking-[0.14em] transition ${
                        selectedDocument.visibleOnLanding
                          ? "border-emerald-400/40 bg-emerald-500/10 text-emerald-200"
                          : "border-zinc-700 bg-black text-zinc-400"
                      }`}
                    >
                      {selectedDocument.visibleOnLanding ? <Eye size={14} /> : <EyeOff size={14} />}
                      Landing
                    </button>
                    <button
                      type="button"
                      onClick={() =>
                        updateDocument(selectedDocument.slug, {
                          visibleInApp: !selectedDocument.visibleInApp,
                        })
                      }
                      className={`inline-flex min-h-12 items-center justify-center gap-2 rounded-xl border px-3 py-2 text-[10px] font-black uppercase tracking-[0.14em] transition ${
                        selectedDocument.visibleInApp
                          ? "border-blue-400/40 bg-blue-500/10 text-blue-200"
                          : "border-zinc-700 bg-black text-zinc-400"
                      }`}
                    >
                      {selectedDocument.visibleInApp ? <Eye size={14} /> : <EyeOff size={14} />}
                      Páginas
                    </button>
                  </div>
                </div>

                <label className="grid gap-2 lg:col-span-2">
                  <span className="text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">
                    Conteúdo
                  </span>
                  <textarea
                    value={selectedDocument.content}
                    onChange={(event) =>
                      updateDocument(selectedDocument.slug, { content: event.target.value })
                    }
                    className="min-h-[420px] rounded-2xl border border-zinc-800 bg-black px-4 py-3 font-mono text-xs leading-6 text-zinc-200 outline-none transition focus:border-red-400"
                  />
                </label>
              </div>
            </div>
          ) : (
            <div className="flex min-h-[50vh] flex-col items-center justify-center gap-3 text-zinc-500">
              <FileText size={42} className="opacity-30" />
              <p className="text-sm font-black uppercase tracking-[0.18em]">
                Nenhum documento selecionado
              </p>
              <button
                type="button"
                onClick={() => void handleSave()}
                disabled={saving || !dirty}
                className="rounded-xl bg-red-600 px-4 py-3 text-[11px] font-black uppercase tracking-[0.16em] text-white transition hover:bg-red-500 disabled:cursor-not-allowed disabled:opacity-50"
              >
                Salvar exclusão
              </button>
            </div>
          )}
        </section>
      </main>
    </div>
  );
}
