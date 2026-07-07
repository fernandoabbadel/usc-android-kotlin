import AdminEventWorkspace from "../../../_components/AdminEventWorkspace";

export default async function AdminEventFichaProdutoPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;

  return <AdminEventWorkspace eventId={decodeURIComponent(id)} section="ficha-produto" />;
}
