import type { Metadata } from "next";

import LegalPageShell from "@/components/legal/LegalPageShell";
import { termsOfServiceDocument } from "@/components/legal/legalContent";

export const metadata: Metadata = {
  title: "Termos de Serviço – USC – Universidade Spot Connect",
  description:
    "Termos de Serviço públicos da USC – Universidade Spot Connect, plataforma digital multitenant.",
};

export default function TermosDeServicoPage() {
  return <LegalPageShell document={termsOfServiceDocument} />;
}
