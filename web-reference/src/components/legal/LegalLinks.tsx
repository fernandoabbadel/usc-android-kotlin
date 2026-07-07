"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";

import {
  fetchPublicPlatformLegalDocuments,
  getFallbackPlatformLegalDocumentsForSurface,
  type PlatformLegalDocument,
  type PlatformLegalSurface,
} from "@/lib/platformLegalDocuments";

type LegalLinksProps = {
  activeSlug?: string;
  className?: string;
  compact?: boolean;
  surface?: PlatformLegalSurface;
};

export default function LegalLinks({
  activeSlug = "",
  className = "",
  compact = false,
  surface = "app",
}: LegalLinksProps) {
  const fallbackDocs = useMemo(
    () => getFallbackPlatformLegalDocumentsForSurface(surface),
    [surface]
  );
  const [documents, setDocuments] = useState<PlatformLegalDocument[]>(fallbackDocs);

  useEffect(() => {
    let mounted = true;

    setDocuments(fallbackDocs);
    fetchPublicPlatformLegalDocuments({ surface })
      .then((loadedDocuments) => {
        if (!mounted) return;
        setDocuments(loadedDocuments);
      })
      .catch(() => {
        if (mounted) setDocuments(fallbackDocs);
      });

    return () => {
      mounted = false;
    };
  }, [fallbackDocs, surface]);

  if (documents.length === 0) return null;

  return (
    <nav
      className={`flex flex-wrap ${compact ? "gap-x-3 gap-y-2" : "gap-3"} ${className}`}
      aria-label="Links legais da USC"
    >
      {documents.map((item) => {
        const isActive = activeSlug && item.slug === activeSlug;
        return (
          <Link
            key={item.slug}
            href={item.href}
            className={
              compact
                ? `text-[10px] font-bold uppercase tracking-wide transition ${
                    isActive ? "text-blue-100" : "text-zinc-500 hover:text-blue-200"
                  }`
                : `rounded-lg border px-3 py-2 text-[11px] font-black uppercase tracking-wide transition ${
                    isActive
                      ? "border-blue-400/50 bg-blue-500/15 text-blue-100"
                      : "border-white/10 bg-white/[0.04] text-zinc-400 hover:border-blue-400/40 hover:text-white"
                  }`
            }
          >
            {item.label}
          </Link>
        );
      })}
    </nav>
  );
}
