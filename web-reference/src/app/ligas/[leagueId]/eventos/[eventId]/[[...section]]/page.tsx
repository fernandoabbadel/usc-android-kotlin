import { ContextualAdminEventWorkspace } from "@/app/admin/eventos/_components/ContextualAdminEventWorkspace";

export default async function LigaEventoWorkspacePage({
  params,
}: {
  params: Promise<{ leagueId: string; eventId: string; section?: string[] }>;
}) {
  const { leagueId, eventId, section } = await params;
  const encodedLeagueId = encodeURIComponent(decodeURIComponent(leagueId));
  const encodedEventId = encodeURIComponent(decodeURIComponent(eventId));

  return (
    <ContextualAdminEventWorkspace
      eventId={decodeURIComponent(eventId)}
      sectionSegments={section}
      workspaceBasePath={`/ligas/${encodedLeagueId}/eventos/${encodedEventId}`}
      eventsListHref={`/ligas/${encodedLeagueId}/eventos`}
      legacyListHref={`/ligas/${encodedLeagueId}/eventos/lista/${encodedEventId}`}
      eventBiBasePath={`/ligas/${encodedLeagueId}/gestao/eventos`}
      paymentRecipientContext={{ ownerType: "league", ownerId: decodeURIComponent(leagueId) }}
    />
  );
}
