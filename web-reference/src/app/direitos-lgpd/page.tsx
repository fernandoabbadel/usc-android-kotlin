import type { Metadata } from "next";

import LegalPageShell from "@/components/legal/LegalPageShell";
import { lgpdRightsDocument } from "@/components/legal/legalContent";

export const metadata: Metadata = {
  title: "Seus Direitos LGPD – USC – Universidade Spot Connect",
  description:
    "Guia público da USC – Universidade Spot Connect para exercício dos direitos previstos na LGPD.",
};

export default function DireitosLgpdPage() {
  return <LegalPageShell document={lgpdRightsDocument} />;
}
