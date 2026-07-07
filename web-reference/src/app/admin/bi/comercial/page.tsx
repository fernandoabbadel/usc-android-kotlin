import AdminEventBiDashboard from "../_components/AdminEventBiDashboard";

export default async function AdminBiComercialPage({
  searchParams,
}: {
  searchParams?: Promise<{ evento?: string }>;
}) {
  const params = searchParams ? await searchParams : {};
  return <AdminEventBiDashboard view="comercial" initialEventId={params.evento || "todos"} />;
}
