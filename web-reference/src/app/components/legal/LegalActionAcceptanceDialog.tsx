"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { CheckCircle2, FileText, Loader2, ShieldCheck, X } from "lucide-react";

import { LEGAL_VERSION } from "@/components/legal/legalContent";
import {
  type LegalAcceptanceDocument,
  type LegalAcceptanceSource,
  recordLegalAcceptance,
} from "@/lib/legalGovernanceService";
import {
  fetchPublicPlatformLegalDocuments,
  getFallbackPlatformLegalDocumentsForSurface,
  type PlatformLegalDocument,
} from "@/lib/platformLegalDocuments";

type LegalActionAcceptanceDialogProps = {
  open: boolean;
  title: string;
  description: string;
  tenantId?: string | null;
  source: LegalAcceptanceSource;
  documentSlugs: string[];
  documents: Array<Omit<LegalAcceptanceDocument, "documentVersion">>;
  onAccepted: () => void;
  onCancel: () => void;
};

const slugSet = (slugs: string[]): Set<string> =>
  new Set(slugs.map((slug) => slug.trim()).filter(Boolean));

export default function LegalActionAcceptanceDialog({
  open,
  title,
  description,
  tenantId,
  source,
  documentSlugs,
  documents,
  onAccepted,
  onCancel,
}: LegalActionAcceptanceDialogProps) {
  const scrollRef = useRef<HTMLDivElement | null>(null);
  const [legalDocuments, setLegalDocuments] = useState<PlatformLegalDocument[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [reachedEnd, setReachedEnd] = useState(false);
  const [markedRead, setMarkedRead] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const documentSlugsKey = documentSlugs.join("|");

  const checkScrollEnd = useCallback(() => {
    const element = scrollRef.current;
    if (!element) return;
    setReachedEnd(element.scrollTop + element.clientHeight >= element.scrollHeight - 12);
  }, []);

  useEffect(() => {
    if (!open) return;
    let mounted = true;
    const allowedSlugs = slugSet(documentSlugsKey.split("|"));

    const loadDocuments = async () => {
      try {
        setLoading(true);
        const loaded = await fetchPublicPlatformLegalDocuments({
          surface: "all",
          includeContent: true,
          forceRefresh: true,
        });
        const sourceDocs = loaded.length ? loaded : getFallbackPlatformLegalDocumentsForSurface("all");
        if (mounted) {
          setLegalDocuments(sourceDocs.filter((document) => allowedSlugs.has(document.slug)));
        }
      } catch {
        if (mounted) {
          setLegalDocuments(
            getFallbackPlatformLegalDocumentsForSurface("all").filter((document) =>
              allowedSlugs.has(document.slug)
            )
          );
        }
      } finally {
        if (mounted) setLoading(false);
      }
    };

    setReachedEnd(false);
    setMarkedRead(false);
    setErrorMessage("");
    void loadDocuments();

    return () => {
      mounted = false;
    };
  }, [documentSlugsKey, open]);

  useEffect(() => {
    if (!open || !legalDocuments.length) return;
    const timer = window.setTimeout(checkScrollEnd, 80);
    return () => window.clearTimeout(timer);
  }, [checkScrollEnd, legalDocuments.length, open]);

  const handleAccept = async () => {
    if (!reachedEnd || !markedRead || saving) return;

    try {
      setSaving(true);
      setErrorMessage("");
      await recordLegalAcceptance({
        tenantId: tenantId || null,
        source,
        readToEnd: true,
        markedRead: true,
        documents: documents.map((document) => ({
          ...document,
          documentVersion: LEGAL_VERSION,
        })),
      });
      onAccepted();
    } catch (error: unknown) {
      setErrorMessage(
        error instanceof Error ? error.message : "Não foi possível registrar o aceite."
      );
    } finally {
      setSaving(false);
    }
  };

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-[130] flex items-center justify-center bg-black/85 p-4 backdrop-blur-md">
      <section className="flex max-h-[92vh] w-full max-w-3xl flex-col rounded-[2rem] border border-emerald-500/25 bg-zinc-950 shadow-2xl">
        <div className="border-b border-zinc-800 p-5">
          <div className="flex items-start justify-between gap-4">
            <div className="flex items-center gap-3">
              <div className="rounded-2xl border border-emerald-500/30 bg-emerald-500/10 p-3 text-emerald-200">
                <ShieldCheck size={20} />
              </div>
              <div>
                <p className="text-[10px] font-black uppercase tracking-[0.24em] text-emerald-200">
                  Aceite jurídico
                </p>
                <h2 className="text-lg font-black uppercase text-white">{title}</h2>
              </div>
            </div>
            <button
              type="button"
              onClick={onCancel}
              disabled={saving}
              className="rounded-full border border-zinc-800 bg-black/40 p-2 text-zinc-400 transition hover:text-white disabled:opacity-50"
            >
              <X size={16} />
            </button>
          </div>
          <p className="mt-4 text-sm leading-6 text-zinc-300">{description}</p>
        </div>

        <div
          ref={scrollRef}
          onScroll={checkScrollEnd}
          className="min-h-[260px] flex-1 overflow-y-auto p-5"
        >
          {loading ? (
            <div className="flex min-h-[260px] items-center justify-center text-emerald-200">
              <Loader2 className="animate-spin" />
            </div>
          ) : (
            <div className="space-y-5">
              {legalDocuments.map((document) => (
                <article
                  key={document.slug}
                  className="rounded-2xl border border-zinc-800 bg-black/35 p-4"
                >
                  <div className="mb-4 flex items-center gap-2 text-white">
                    <FileText size={16} className="text-emerald-200" />
                    <h3 className="text-sm font-black uppercase">{document.label}</h3>
                  </div>
                  <div className="whitespace-pre-wrap text-sm leading-7 text-zinc-300">
                    {document.content || "Documento sem conteúdo publicado no momento."}
                  </div>
                </article>
              ))}
            </div>
          )}
        </div>

        <div className="border-t border-zinc-800 p-5">
          <label
            className={`flex items-start gap-3 rounded-2xl border p-4 text-sm leading-6 transition ${
              reachedEnd
                ? "cursor-pointer border-emerald-500/30 bg-emerald-500/10 text-emerald-100"
                : "cursor-not-allowed border-zinc-800 bg-black/30 text-zinc-500"
            }`}
          >
            <input
              type="checkbox"
              disabled={!reachedEnd || saving}
              checked={markedRead}
              onChange={(event) => setMarkedRead(event.target.checked)}
              className="mt-1 h-4 w-4 accent-emerald-500"
            />
            <span>
              {reachedEnd
                ? "Li até o fim e aceito continuar com esta ação."
                : "Role o documento até o fim para liberar o aceite."}
            </span>
          </label>

          {errorMessage ? (
            <p className="mt-3 rounded-2xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-xs font-semibold leading-5 text-red-100">
              {errorMessage}
            </p>
          ) : null}

          <button
            type="button"
            onClick={() => void handleAccept()}
            disabled={!reachedEnd || !markedRead || saving}
            className="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-2xl bg-emerald-600 px-5 py-4 text-xs font-black uppercase tracking-[0.18em] text-white transition hover:bg-emerald-500 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {saving ? <Loader2 size={16} className="animate-spin" /> : <CheckCircle2 size={16} />}
            {saving ? "Registrando..." : "Aceitar e continuar"}
          </button>
        </div>
      </section>
    </div>
  );
}
