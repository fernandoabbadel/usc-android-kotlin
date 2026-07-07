import { LeagueEventBiDashboard } from "@/app/ligas/_components/LeagueEventBiDashboard";

export default async function TenantLigaEventosGestaoPage({
  params,
}: {
  params: Promise<{ leagueId: string }>;
}) {
  const { leagueId } = await params;
  return <LeagueEventBiDashboard view="inicio" leagueId={decodeURIComponent(leagueId)} />;
}
