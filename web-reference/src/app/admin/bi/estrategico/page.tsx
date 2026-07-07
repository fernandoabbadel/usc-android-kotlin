import AdminEventBiDashboard from "../_components/AdminEventBiDashboard";

export default async function AdminBiEstrategicoPage({
  searchParams,
}: {
  searchParams?: Promise<{ evento?: string }>;
}) {
  const params = searchParams ? await searchParams : {};
  return <AdminEventBiDashboard view="estrategico" initialEventId={params.evento || "todos"} />;
}
