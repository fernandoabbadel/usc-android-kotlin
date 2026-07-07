"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { ArrowLeft, FileText, ShieldCheck } from "lucide-react";

import type { LegalDocument } from "./legalContent";
import LegalLinks from "./LegalLinks";
import {
  fetchPublicPlatformLegalDocuments,
  platformLegalDocumentToLegalDocument,
} from "@/lib/platformLegalDocuments";

type LegalPageShellProps = {
  document: LegalDocument;
  fallbackEnabled?: boolean;
};

export default function LegalPageShell({
  document,
  fallbackEnabled = true,
}: LegalPageShellProps) {
  const [resolvedDocument, setResolvedDocument] = useState<LegalDocument>(document);
  const [hiddenByConfig, setHiddenByConfig] = useState(false);
  const [loadedConfig, setLoadedConfig] = useState(false);

  useEffect(() => {
    let mounted = true;

    setResolvedDocument(document);
    setHiddenByConfig(false);
    setLoadedConfig(false);

    fetchPublicPlatformLegalDocuments({
      surface: "all",
      includeContent: true,
      forceRefresh: true,
    })
      .then((documents) => {
        if (!mounted) return;
        const configuredDocument = documents.find((item) => item.slug === document.slug);
        if (!configuredDocument) {
          setHiddenByConfig(!fallbackEnabled);
          return;
        }

        setHiddenByConfig(!configuredDocument.visibleInApp);
        if (configuredDocument.visibleInApp) {
          setResolvedDocument(platformLegalDocumentToLegalDocument(configuredDocument));
        }
      })
      .catch(() => {
        if (mounted) setHiddenByConfig(!fallbackEnabled);
      })
      .finally(() => {
        if (mounted) setLoadedConfig(true);
      });

    return () => {
      mounted = false;
    };
  }, [document, fallbackEnabled]);

  const sectionLinks = useMemo(
    () =>
      resolvedDocument.sections.map((section) => ({
        title: section.title,
        id: section.title.replace(/[^\wÀ-ÿ]+/g, "-").toLowerCase(),
      })),
    [resolvedDocument.sections]
  );

  const showUnavailable = loadedConfig && hiddenByConfig;

  return (
    <div className="min-h-screen bg-[#02050d] text-white">
      <div className="pointer-events-none fixed inset-0">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(37,99,235,0.18),transparent_35%),linear-gradient(180deg,rgba(2,5,13,0.92),#02050d)]" />
      </div>

      <header className="relative z-10 border-b border-white/10 bg-[#02050d]/85 backdrop-blur-xl">
        <div className="mx-auto flex max-w-6xl flex-col gap-5 px-4 py-5 sm:flex-row sm:items-center sm:justify-between">
          <Link
            href="/"
            className="inline-flex items-center gap-2 text-sm font-bold text-zinc-300 transition hover:text-white"
          >
            <ArrowLeft size={16} />
            Voltar para a USC
          </Link>

          <LegalLinks activeSlug={resolvedDocument.slug} surface="app" />
        </div>
      </header>

      <main className="relative z-10 mx-auto max-w-6xl px-4 py-10 sm:py-14">
        {showUnavailable ? (
          <section className="rounded-lg border border-white/10 bg-white/[0.045] p-6 text-center">
            <FileText className="mx-auto mb-4 text-zinc-500" size={34} />
            <h1 className="text-2xl font-black text-white">Documento indisponível</h1>
            <p className="mx-auto mt-3 max-w-2xl text-sm leading-7 text-zinc-400">
              Este documento não está visível nas páginas públicas no momento.
            </p>
          </section>
        ) : (
          <>
            <section className="mb-8 rounded-lg border border-blue-400/20 bg-blue-500/10 p-5 sm:p-7">
              <div className="mb-5 inline-flex items-center gap-2 rounded-lg border border-white/10 bg-white/[0.06] px-3 py-2 text-xs font-black uppercase tracking-wide text-blue-100">
                <ShieldCheck size={16} />
                Documento público global da USC
              </div>
              <h1 className="max-w-4xl text-3xl font-black leading-tight text-white sm:text-5xl">
                {resolvedDocument.title}
              </h1>
              <p className="mt-4 max-w-3xl text-base leading-7 text-zinc-300">
                {resolvedDocument.description}
              </p>
              <p className="mt-5 text-sm font-semibold text-zinc-400">
                Última atualização:{" "}
                <span className="text-zinc-100">{resolvedDocument.lastUpdated}</span>
              </p>
            </section>

            <div className="grid gap-8 lg:grid-cols-[260px_1fr]">
              <aside className="hidden lg:block">
                <div className="sticky top-6 rounded-lg border border-white/10 bg-white/[0.04] p-4">
                  <div className="mb-3 flex items-center gap-2 text-xs font-black uppercase tracking-wide text-zinc-400">
                    <FileText size={14} />
                    Nesta página
                  </div>
                  <ol className="space-y-2">
                    {sectionLinks.map((section) => (
                      <li key={section.id}>
                        <a
                          href={`#${section.id}`}
                          className="block rounded-md px-2 py-2 text-sm text-zinc-400 transition hover:bg-white/[0.06] hover:text-white"
                        >
                          {section.title}
                        </a>
                      </li>
                    ))}
                  </ol>
                </div>
              </aside>

              <article className="space-y-5">
                {resolvedDocument.sections.map((section) => {
                  const sectionId = section.title.replace(/[^\wÀ-ÿ]+/g, "-").toLowerCase();
                  return (
                    <section
                      key={section.title}
                      id={sectionId}
                      className="scroll-mt-6 rounded-lg border border-white/10 bg-white/[0.045] p-5 sm:p-6"
                    >
                      <h2 className="text-xl font-black leading-tight text-white sm:text-2xl">
                        {section.title}
                      </h2>

                      {section.body?.length ? (
                        <div className="mt-4 space-y-3">
                          {section.body.map((paragraph) => (
                            <p
                              key={paragraph}
                              className="whitespace-pre-wrap text-sm leading-7 text-zinc-300 sm:text-base"
                            >
                              {paragraph}
                            </p>
                          ))}
                        </div>
                      ) : null}

                      {section.bullets?.length ? (
                        <ul className="mt-4 space-y-2">
                          {section.bullets.map((bullet) => (
                            <li
                              key={bullet}
                              className="flex gap-3 text-sm leading-7 text-zinc-300 sm:text-base"
                            >
                              <span className="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-blue-300" />
                              <span>{bullet}</span>
                            </li>
                          ))}
                        </ul>
                      ) : null}

                      {section.items?.length ? (
                        <dl className="mt-4 grid gap-3">
                          {section.items.map((item) => (
                            <div
                              key={`${item.label}-${item.text}`}
                              className="border-l border-blue-400/30 py-2 pl-4"
                            >
                              <dt className="text-sm font-black uppercase tracking-wide text-blue-100">
                                {item.label}
                              </dt>
                              <dd className="mt-2 text-sm leading-7 text-zinc-300 sm:text-base">
                                {item.text}
                              </dd>
                            </div>
                          ))}
                        </dl>
                      ) : null}

                      {section.note ? (
                        <p className="mt-4 rounded-lg border border-amber-400/20 bg-amber-400/10 p-4 text-sm leading-7 text-amber-100">
                          {section.note}
                        </p>
                      ) : null}
                    </section>
                  );
                })}
              </article>
            </div>
          </>
        )}
      </main>
    </div>
  );
}
