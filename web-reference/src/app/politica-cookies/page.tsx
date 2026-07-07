import type { Metadata } from "next";

import LegalPageShell from "@/components/legal/LegalPageShell";
import { cookiesPolicyDocument } from "@/components/legal/legalContent";

export const metadata: Metadata = {
  title: "Política de Cookies – USC – Universidade Spot Connect",
  description:
    "Política de Cookies, localStorage e tecnologias semelhantes da USC – Universidade Spot Connect.",
};

export default function PoliticaCookiesPage() {
  return <LegalPageShell document={cookiesPolicyDocument} />;
}
