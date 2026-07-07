import { EventProductsClientPage } from "./EventProductsClientPage";

export default async function EventProductsPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;

  return <EventProductsClientPage eventId={decodeURIComponent(id)} />;
}
