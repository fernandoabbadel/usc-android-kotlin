import { EventProductTicketsClientPage } from "./EventProductTicketsClientPage";

export default async function EventProductTicketsPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;

  return <EventProductTicketsClientPage eventId={decodeURIComponent(id)} />;
}
