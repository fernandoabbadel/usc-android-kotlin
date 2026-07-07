import { EventProductWithdrawalClientPage } from "../EventProductWithdrawalClientPage";

export default async function AdminEventFichaRetiradaPendentesPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;

  return <EventProductWithdrawalClientPage eventId={decodeURIComponent(id)} mode="pendentes" />;
}
