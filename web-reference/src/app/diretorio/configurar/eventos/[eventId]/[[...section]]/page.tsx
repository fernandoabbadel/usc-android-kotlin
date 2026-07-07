import { DirectoryManagementEventWorkspacePage } from "@/components/collectives/DirectoryManagementPages";

export default async function DiretorioEventoWorkspacePage({
  params,
}: {
  params: Promise<{ eventId: string; section?: string[] }>;
}) {
  const { eventId, section } = await params;
  return <DirectoryManagementEventWorkspacePage eventId={eventId} sectionSegments={section} />;
}
