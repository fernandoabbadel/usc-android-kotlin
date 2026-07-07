import { CommissionManagementEventWorkspacePage } from "@/components/collectives/CommissionManagementPages";

export default async function ComissaoEventoWorkspacePage({
  params,
}: {
  params: Promise<{ eventId: string; section?: string[] }>;
}) {
  const { eventId, section } = await params;
  return <CommissionManagementEventWorkspacePage eventId={eventId} sectionSegments={section} />;
}
