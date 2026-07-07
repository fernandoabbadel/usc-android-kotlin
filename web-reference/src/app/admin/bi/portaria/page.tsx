import AdminEventBiDashboard from "../_components/AdminEventBiDashboard";

export default async function AdminBiPortariaPage({
  searchParams,
}: {
  searchParams?: Promise<{ evento?: string }>;
}) {
  const params = searchParams ? await searchParams : {};
  return <AdminEventBiDashboard view="portaria" initialEventId={params.evento || "todos"} />;
}
