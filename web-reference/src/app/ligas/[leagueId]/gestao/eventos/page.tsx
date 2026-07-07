import { LeagueEventBiDashboard } from "@/app/ligas/_components/LeagueEventBiDashboard";

export default async function LigaEventosGestaoPage({
  params,
}: {
  params: Promise<{ leagueId: string }>;
}) {
  const { leagueId } = await params;
  const cleanLeagueId = decodeURIComponent(leagueId);
  return <LeagueEventBiDashboard view="inicio" leagueId={cleanLeagueId} />;
}
