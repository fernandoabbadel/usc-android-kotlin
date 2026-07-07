import { AdminEventTicketOrderPage } from "@/app/admin/eventos/_components/AdminEventTicketOrderPage";

export default async function AdminEventoIngressoDetalhePage({
  params,
}: {
  params: Promise<{ id: string; pedidoId: string }>;
}) {
  const { id, pedidoId } = await params;
  return (
    <AdminEventTicketOrderPage
      eventId={decodeURIComponent(id)}
      pedidoId={decodeURIComponent(pedidoId)}
    />
  );
}
