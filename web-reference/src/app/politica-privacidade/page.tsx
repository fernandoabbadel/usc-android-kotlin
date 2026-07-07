import type { Metadata } from "next";

import LegalPageShell from "@/components/legal/LegalPageShell";
import { privacyPolicyDocument } from "@/components/legal/legalContent";

export const metadata: Metadata = {
  title: "Política de Privacidade – USC – Universidade Spot Connect",
  description:
    "Política de Privacidade pública da USC – Universidade Spot Connect, plataforma digital multitenant.",
};

export default function PoliticaPrivacidadePage() {
  return <LegalPageShell document={privacyPolicyDocument} />;
}
