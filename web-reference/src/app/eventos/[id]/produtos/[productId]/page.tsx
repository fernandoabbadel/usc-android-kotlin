import { EventProductDetailClientPage } from "./EventProductDetailClientPage";

export default async function EventProductDetailPage({
  params,
}: {
  params: Promise<{ id: string; productId: string }>;
}) {
  const { id, productId } = await params;

  return (
    <EventProductDetailClientPage
      eventId={decodeURIComponent(id)}
      productId={decodeURIComponent(productId)}
    />
  );
}
