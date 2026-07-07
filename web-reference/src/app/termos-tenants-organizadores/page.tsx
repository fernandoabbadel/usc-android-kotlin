import type { Metadata } from "next";

import LegalPageShell from "@/components/legal/LegalPageShell";
import { tenantOrganizerTermsDocument } from "@/components/legal/legalContent";

export const metadata: Metadata = {
  title: tenantOrganizerTermsDocument.title,
  description: tenantOrganizerTermsDocument.description,
};

export default function TenantOrganizerTermsPage() {
  return <LegalPageShell document={tenantOrganizerTermsDocument} />;
}
