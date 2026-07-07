import { LeagueEventBiDashboard } from "@/app/ligas/_components/LeagueEventBiDashboard";

export default async function LigaBiPortariaPage({
  params,
  searchParams,
}: {
  params: Promise<{ leagueId: string }>;
  searchParams?: Promise<{ evento?: string }>;
}) {
  const { leagueId } = await params;
  const query = searchParams ? await searchParams : {};
  const cleanLeagueId = decodeURIComponent(leagueId);
  return (
    <LeagueEventBiDashboard
      view="portaria"
      initialEventId={query.evento || "todos"}
      leagueId={cleanLeagueId}
    />
  );
}
