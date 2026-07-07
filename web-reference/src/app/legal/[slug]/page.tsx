import type { Metadata } from "next";

import LegalPageShell from "@/components/legal/LegalPageShell";
import { LEGAL_LAST_UPDATED, type LegalDocument } from "@/components/legal/legalContent";
import { slugifyLegalDocument } from "@/lib/platformLegalDocuments";

type LegalDynamicPageProps = {
  params: Promise<{ slug: string }>;
};

export const metadata: Metadata = {
  title: "Documento legal – USC – Universidade Spot Connect",
  description: "Documento legal público da USC – Universidade Spot Connect.",
};

export default async function LegalDynamicPage({ params }: LegalDynamicPageProps) {
  const { slug } = await params;
  const cleanSlug = slugifyLegalDocument(slug || "documento-legal");
  const fallbackDocument: LegalDocument = {
    slug: cleanSlug,
    title: "Documento legal",
    description: "Documento legal público da USC.",
    lastUpdated: LEGAL_LAST_UPDATED,
    sections: [
      {
        title: "Documento",
        body: ["Carregando documento legal."],
      },
    ],
  };

  return <LegalPageShell document={fallbackDocument} fallbackEnabled={false} />;
}
