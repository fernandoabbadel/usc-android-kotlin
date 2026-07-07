"use client";

import { useEffect, useMemo, useState } from "react";
import { useParams } from "next/navigation";

import AdminEventBiDashboard, { type AdminEventBiView } from "@/app/admin/bi/_components/AdminEventBiDashboard";
import { useTenantTheme } from "@/context/TenantThemeContext";
import { fetchLeagueById, type LeagueRecord } from "@/lib/leaguesService";
import { resolveLeagueLogoSrc } from "@/lib/leagueMedia";

interface LeagueEventBiDashboardProps {
  view: AdminEventBiView;
  initialEventId?: string;
  leagueId?: string;
}

export function LeagueEventBiDashboard({
  view,
  initialEventId = "todos",
  leagueId: leagueIdOverride,
}: LeagueEventBiDashboardProps) {
  const params = useParams<{ leagueId?: string }>();
  const { tenantId } = useTenantTheme();
  const routeLeagueId = typeof params?.leagueId === "string" ? params.leagueId : "";
  const cleanLeagueId = decodeURIComponent((leagueIdOverride || routeLeagueId).trim());
  const [league, setLeague] = useState<LeagueRecord | null>(null);

  useEffect(() => {
    let mounted = true;
    if (!cleanLeagueId) {
      setLeague(null);
      return () => {
        mounted = false;
      };
    }

    void fetchLeagueById(cleanLeagueId, {
      tenantId: tenantId || undefined,
      forceRefresh: true,
    })
      .then((nextLeague) => {
        if (mounted) setLeague(nextLeague);
      })
      .catch((error: unknown) => {
        console.error(error);
        if (mounted) setLeague(null);
      });

    return () => {
      mounted = false;
    };
  }, [cleanLeagueId, tenantId]);

  const { basePath, backHref } = useMemo(() => {
    const encodedLeagueId = encodeURIComponent(cleanLeagueId);
    return {
      basePath: `/ligas/${encodedLeagueId}/gestao/eventos`,
      backHref: `/ligas/${encodedLeagueId}/gestao`,
    };
  }, [cleanLeagueId]);

  const leagueTitle = league?.sigla?.trim() || league?.nome?.trim() || "Liga";
  const leagueLogo = (league ? resolveLeagueLogoSrc(league) : "") || "/logo.png";

  return (
    <AdminEventBiDashboard
      view={view}
      initialEventId={initialEventId}
      basePath={basePath}
      eventWorkspaceBasePath={`/ligas/${encodeURIComponent(cleanLeagueId)}/eventos`}
      lockedScopeType="league"
      lockedScopeId={cleanLeagueId}
      scopeLabel="da liga"
      backHref={backHref}
      contextTitle={leagueTitle}
      contextLogo={leagueLogo}
      contextEyebrow="BI da liga"
    />
  );
}
