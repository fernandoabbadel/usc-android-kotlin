import AdminEventBiDashboard from "../_components/AdminEventBiDashboard";

export default async function AdminBiVendasPage({
  searchParams,
}: {
  searchParams?: Promise<{ evento?: string }>;
}) {
  const params = searchParams ? await searchParams : {};
  return <AdminEventBiDashboard view="vendas" initialEventId={params.evento || "todos"} />;
}
