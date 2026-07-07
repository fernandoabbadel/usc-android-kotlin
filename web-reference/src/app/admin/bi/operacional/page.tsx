import AdminEventBiDashboard from "../_components/AdminEventBiDashboard";

export default async function AdminBiOperacionalPage({
  searchParams,
}: {
  searchParams?: Promise<{ evento?: string }>;
}) {
  const params = searchParams ? await searchParams : {};
  return <AdminEventBiDashboard view="operacional" initialEventId={params.evento || "todos"} />;
}
