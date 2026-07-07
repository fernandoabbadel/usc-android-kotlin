"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { usePathname } from "next/navigation";
import { CheckCircle2, FileText, Loader2, ShieldCheck } from "lucide-react";

import { LEGAL_VERSION } from "@/components/legal/legalContent";
import { useAuth } from "@/context/AuthContext";
import { useTenantTheme } from "@/context/TenantThemeContext";
import { useToast } from "@/context/ToastContext";
import { recordLegalAcceptance } from "@/lib/legalGovernanceService";
import {
  fetchPublicPlatformLegalDocuments,
  getFallbackPlatformLegalDocumentsForSurface,
  type PlatformLegalDocument,
} from "@/lib/platformLegalDocuments";
import { resolveEffectiveAccessRole } from "@/lib/roles";

const REQUIRED_ADMIN_SLUGS = [
  "termo-confidencialidade-admin",
  "termos-tenants-organizadores",
] as const;
const REQUIRED_ADMIN_SLUG_SET = new Set<string>(REQUIRED_ADMIN_SLUGS);

const isElevatedRole = (value: unknown): boolean => {
  const normalized = typeof value === "string" ? value.trim().toLowerCase() : "";
  return !["", "guest", "visitante", "user"].includes(normalized);
};

const parseTime = (value: unknown): number => {
  if (typeof value !== "string" || !value.trim()) return 0;
  const parsed = Date.parse(value);
  return Number.isFinite(parsed) ? parsed : 0;
};

const isBypassPath = (pathname: string): boolean =>
  pathname === "/login" ||
  pathname === "/cadastro" ||
  pathname === "/banned" ||
  pathname.startsWith("/legal/") ||
  pathname === "/politica-privacidade" ||
  pathname === "/termos-de-servico" ||
  pathname === "/politica-cookies" ||
  pathname === "/direitos-lgpd" ||
  pathname === "/termo-confidencialidade-admin" ||
  pathname === "/termos-tenants-organizadores";

export default function RequiredLegalAcceptanceModal() {
  const { user, updateUser } = useAuth();
  const { tenantId } = useTenantTheme();
  const { addToast } = useToast();
  const pathname = usePathname() || "/";
  const scrollRef = useRef<HTMLDivElement | null>(null);
  const [documents, setDocuments] = useState<PlatformLegalDocument[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [reachedEnd, setReachedEnd] = useState(false);
  const [markedRead, setMarkedRead] = useState(false);
  const [acceptedLocally, setAcceptedLocally] = useState(false);

  const shouldRequire = useMemo(() => {
    if (!user || user.isAnonymous || acceptedLocally || isBypassPath(pathname)) return false;
    if (!isElevatedRole(resolveEffectiveAccessRole(user))) return false;

    const requiredAt = parseTime(user.legal_admin_required_at);
    if (!requiredAt) return false;

    const acceptedAt = parseTime(user.legal_admin_accepted_at);
    return !acceptedAt || acceptedAt < requiredAt;
  }, [acceptedLocally, pathname, user]);

  const checkScrollEnd = useCallback(() => {
    const element = scrollRef.current;
    if (!element) return;
    const isAtEnd = element.scrollTop + element.clientHeight >= element.scrollHeight - 12;
    setReachedEnd(isAtEnd);
  }, []);

  useEffect(() => {
    if (!shouldRequire) return;
    let mounted = true;

    const loadDocuments = async () => {
      try {
        setLoading(true);
        const loaded = await fetchPublicPlatformLegalDocuments({
          surface: "all",
          includeContent: true,
          forceRefresh: true,
        });
        if (!mounted) return;
        const fallback = getFallbackPlatformLegalDocumentsForSurface("all");
        const source = loaded.length ? loaded : fallback;
        setDocuments(source.filter((document) => REQUIRED_ADMIN_SLUG_SET.has(document.slug)));
      } catch {
        if (!mounted) return;
        setDocuments(
          getFallbackPlatformLegalDocumentsForSurface("all").filter((document) =>
            REQUIRED_ADMIN_SLUG_SET.has(document.slug)
          )
        );
      } finally {
        if (mounted) setLoading(false);
      }
    };

    setReachedEnd(false);
    setMarkedRead(false);
    void loadDocuments();

    return () => {
      mounted = false;
    };
  }, [shouldRequire]);

  useEffect(() => {
    if (!documents.length) return;
    const timer = window.setTimeout(checkScrollEnd, 80);
    return () => window.clearTimeout(timer);
  }, [checkScrollEnd, documents.length]);

  const handleAccept = async () => {
    if (!user?.uid || !reachedEnd || !markedRead) return;

    try {
      setSaving(true);
      const acceptedAt = new Date().toISOString();
      await recordLegalAcceptance({
        tenantId: tenantId.trim() || user.tenant_id || null,
        source: "role_upgrade",
        readToEnd: true,
        markedRead: true,
        documents: [
          { documentType: "admin_confidentiality", documentVersion: LEGAL_VERSION },
          { documentType: "tenant_terms", documentVersion: LEGAL_VERSION },
        ],
      });
      setAcceptedLocally(true);
      await updateUser({ legal_admin_accepted_at: acceptedAt });
      addToast("Aceite administrativo registrado.", "success");
    } catch (error: unknown) {
      addToast(
        error instanceof Error ? error.message : "Não foi possível registrar o aceite.",
        "error"
      );
    } finally {
      setSaving(false);
    }
  };

  if (!shouldRequire) return null;

  return (
    <div className="fixed inset-0 z-[120] flex items-center justify-center bg-black/85 p-4 backdrop-blur-md">
      <section className="flex max-h-[92vh] w-full max-w-3xl flex-col rounded-[2rem] border border-red-500/25 bg-zinc-950 shadow-2xl">
        <div className="border-b border-zinc-800 p-5">
          <div className="flex items-center gap-3">
            <div className="rounded-2xl border border-red-500/30 bg-red-500/10 p-3 text-red-200">
              <ShieldCheck size={20} />
            </div>
            <div>
              <p className="text-[10px] font-black uppercase tracking-[0.24em] text-red-200">
                Aceite obrigatório
              </p>
              <h2 className="text-lg font-black uppercase text-white">
                Termos administrativos da USC
              </h2>
            </div>
          </div>
          <p className="mt-4 text-sm leading-6 text-zinc-300">
            Seu acesso recebeu uma função acima de membro. Para continuar usando áreas
            administrativas, leia até o fim e confirme o aceite.
          </p>
        </div>

        <div
          ref={scrollRef}
          onScroll={checkScrollEnd}
          className="min-h-[260px] flex-1 overflow-y-auto p-5"
        >
          {loading ? (
            <div className="flex min-h-[260px] items-center justify-center text-red-200">
              <Loader2 className="animate-spin" />
            </div>
          ) : (
            <div className="space-y-5">
              {documents.map((document) => (
                <article
                  key={document.slug}
                  className="rounded-2xl border border-zinc-800 bg-black/35 p-4"
                >
                  <div className="mb-4 flex items-center gap-2 text-white">
                    <FileText size={16} className="text-red-200" />
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
                ? "Li até o fim e aceito os termos administrativos."
                : "Role o documento até o fim para liberar o aceite."}
            </span>
          </label>

          <button
            type="button"
            onClick={() => void handleAccept()}
            disabled={!reachedEnd || !markedRead || saving}
            className="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-2xl bg-red-600 px-5 py-4 text-xs font-black uppercase tracking-[0.18em] text-white transition hover:bg-red-500 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {saving ? <Loader2 size={16} className="animate-spin" /> : <CheckCircle2 size={16} />}
            {saving ? "Registrando..." : "Aceitar e continuar"}
          </button>
        </div>
      </section>
    </div>
  );
}
