import type { Metadata } from "next";

import LegalPageShell from "@/components/legal/LegalPageShell";
import { adminConfidentialityDocument } from "@/components/legal/legalContent";

export const metadata: Metadata = {
  title: adminConfidentialityDocument.title,
  description: adminConfidentialityDocument.description,
};

export default function AdminConfidentialityPage() {
  return <LegalPageShell document={adminConfidentialityDocument} />;
}
