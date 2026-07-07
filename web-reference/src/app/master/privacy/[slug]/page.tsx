import { MasterLegalDocumentEdit } from "../_components/MasterLegalDocumentsEditor";

type MasterLegalDocumentEditPageProps = {
  params: Promise<{ slug: string }>;
};

export default async function MasterLegalDocumentEditPage({
  params,
}: MasterLegalDocumentEditPageProps) {
  const { slug } = await params;
  return <MasterLegalDocumentEdit routeSlug={slug} />;
}
