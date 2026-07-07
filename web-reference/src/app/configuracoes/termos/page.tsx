"use client";

import React, { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { ArrowLeft, CheckCircle, FileText, Lock, Shield } from "lucide-react";

import { useTenantTheme } from "@/context/TenantThemeContext";
import {
  fetchPublicPlatformLegalDocuments,
  getFallbackPlatformLegalDocumentsForSurface,
} from "@/lib/platformLegalDocuments";
import { withTenantSlug } from "@/lib/tenantRouting";

type TermDoc = {
  id: string;
  title: string;
  content: string;
  icon: React.ElementType;
};

const clampStr = (value: string, max: number): string => {
  const cleanValue = String(value ?? "");
  return cleanValue.length > max ? cleanValue.slice(0, max) : cleanValue;
};

const fallbackDocs = (): TermDoc[] =>
  getFallbackPlatformLegalDocumentsForSurface("app").map((document) => ({
    id: document.slug,
    title: clampStr(document.label || document.title || "Documento legal", 120),
    content: clampStr(document.content, 80_000),
    icon: FileText,
  }));

export default function TermosLegaisPage() {
  const { tenantSlug } = useTenantTheme();
  const [docs, setDocs] = useState<TermDoc[]>([]);
  const [activeDocId, setActiveDocId] = useState("");
  const [loading, setLoading] = useState(true);
  const settingsHref = tenantSlug?.trim()
    ? withTenantSlug(tenantSlug.trim(), "/configuracoes")
    : "/configuracoes";

  useEffect(() => {
    let alive = true;

    const load = async () => {
      setLoading(true);
      try {
        const rows = await fetchPublicPlatformLegalDocuments({
          surface: "app",
          includeContent: true,
          forceRefresh: true,
        });
        const list: TermDoc[] = rows.map((document) => ({
          id: document.slug,
          title: clampStr(document.label || document.title || "Documento legal", 120),
          content: clampStr(document.content, 80_000),
          icon: FileText,
        }));

        if (!alive) return;
        setDocs(list);
        setActiveDocId((previous) =>
          list.some((document) => document.id === previous) ? previous : list[0]?.id || ""
        );
      } catch (error: unknown) {
        console.error("Erro ao carregar termos:", error);
        if (!alive) return;
        const list = fallbackDocs();
        setDocs(list);
        setActiveDocId((previous) =>
          list.some((document) => document.id === previous) ? previous : list[0]?.id || ""
        );
      } finally {
        if (alive) setLoading(false);
      }
    };

    void load();

    return () => {
      alive = false;
    };
  }, []);

  const activeDoc = useMemo(
    () => docs.find((document) => document.id === activeDocId) || null,
    [docs, activeDocId]
  );
  const ActiveDocIcon = activeDoc?.icon || FileText;

  return (
    <div className="flex min-h-screen flex-col bg-[#050505] font-sans text-white selection:bg-emerald-500">
      <header className="sticky top-0 z-30 flex items-center gap-4 border-b border-zinc-800 bg-[#050505]/95 p-4 backdrop-blur-md">
        <Link
          href={settingsHref}
          className="rounded-full border border-zinc-800 bg-zinc-900 p-2 text-zinc-400 transition hover:text-white"
        >
          <ArrowLeft size={20} />
        </Link>
        <h1 className="text-lg font-black uppercase tracking-tight">Jurídico</h1>
      </header>

      <div className="sticky top-[73px] z-20 overflow-x-auto border-b border-zinc-800 bg-[#050505] px-4 py-3">
        <div className="flex min-w-max gap-3">
          {docs.map((document) => {
            const Icon = document.icon;
            return (
              <button
                key={document.id}
                onClick={() => setActiveDocId(document.id)}
                className={`flex items-center gap-2 rounded-full border px-4 py-2 text-xs font-bold uppercase transition ${
                  activeDocId === document.id
                    ? "border-emerald-500 bg-emerald-500 text-black shadow-lg shadow-emerald-500/20"
                    : "border-zinc-800 bg-zinc-900 text-zinc-400 hover:text-white"
                }`}
              >
                <Icon size={14} />
                {document.title}
              </button>
            );
          })}
        </div>
      </div>

      <main className="mx-auto w-full max-w-3xl flex-1 p-4 pb-24">
        {loading ? (
          <div className="flex items-center gap-2 text-xs text-zinc-500 animate-pulse">
            <Shield size={14} /> Carregando documentos jurídicos...
          </div>
        ) : null}

        {!loading && docs.length === 0 ? (
          <div className="rounded-3xl border border-zinc-800 bg-zinc-900 p-6 text-center text-sm text-zinc-400">
            <Lock size={32} className="mx-auto mb-3 opacity-20" />
            Nenhum documento público disponível no momento.
          </div>
        ) : null}

        {activeDoc ? (
          <div className="rounded-3xl border border-zinc-800 bg-zinc-900 p-6 shadow-2xl">
            <div className="mb-6 flex items-center gap-3 border-b border-zinc-800 pb-6">
              <div className="rounded-xl border border-emerald-500/20 bg-emerald-500/10 p-3 text-emerald-500">
                <ActiveDocIcon size={24} />
              </div>
              <div>
                <h2 className="text-xl font-black uppercase leading-none">{activeDoc.title}</h2>
                <p className="mt-1 text-[10px] font-bold uppercase text-zinc-500">
                  Fonte: USC - Universidade Spot Connect
                </p>
              </div>
            </div>

            <div className="whitespace-pre-wrap text-sm leading-7 text-zinc-300">
              {activeDoc.content}
            </div>

            <div className="mt-8 flex items-center justify-center gap-2 border-t border-zinc-800 pt-6 text-xs font-medium text-zinc-500 opacity-60">
              <CheckCircle size={14} /> Você leu até o fim
            </div>
          </div>
        ) : null}
      </main>
    </div>
  );
}
