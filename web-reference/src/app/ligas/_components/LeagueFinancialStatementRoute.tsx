"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { ArrowLeft, Loader2, ShieldAlert } from "lucide-react";

import { FinancialStatementPage } from "@/components/financeiro/FinancialStatementPage";
import { useAuth } from "@/context/AuthContext";
import { useTenantTheme } from "@/context/TenantThemeContext";
import {
  fetchManagedLeagueSummaries,
  type ManagedLeagueRecord,
} from "@/lib/leaguesService";
import { resolveLeagueLogoSrc } from "@/lib/leagueMedia";
import { isPlatformMaster } from "@/lib/roles";
import { withTenantSlug } from "@/lib/tenantRouting";

export function LeagueFinancialStatementRoute() {
  const params = useParams<{ leagueId?: string }>();
  const { user, loading: authLoading } = useAuth();
  const { tenantId, tenantSlug, loading: tenantLoading } = useTenantTheme();
  const [loading, setLoading] = useState(true);
  const [league, setLeague] = useState<ManagedLeagueRecord | null>(null);
  const routeLeagueId =
    typeof params?.leagueId === "string" ? decodeURIComponent(params.leagueId).trim() : "";
  const tenantPath = (path: string) => (tenantSlug ? withTenantSlug(tenantSlug, path) : path);

  useEffect(() => {
    if (authLoading || tenantLoading) {
      setLoading(true);
      return;
    }
    let mounted = true;
    const load = async () => {
      setLoading(true);
      try {
        const rows = await fetchManagedLeagueSummaries({
          userId: user?.uid,
          tenantId: tenantId || undefined,
          isPlatformMaster: isPlatformMaster(user),
          forceRefresh: true,
          category: "liga",
        });
        if (!mounted) return;
        setLeague(rows.find((row) => row.id === routeLeagueId) || null);
      } catch (error) {
        console.error(error);
        if (mounted) setLeague(null);
      } finally {
        if (mounted) setLoading(false);
      }
    };
    void load();
    return () => {
      mounted = false;
    };
  }, [authLoading, routeLeagueId, tenantId, tenantLoading, user]);

  if (authLoading || tenantLoading || loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#050505] text-white">
        <div className="flex items-center gap-3 rounded-2xl border border-zinc-800 bg-zinc-950/80 px-5 py-4">
          <Loader2 className="animate-spin text-emerald-300" size={18} />
          <span className="text-sm font-bold uppercase tracking-[0.2em] text-zinc-400">Carregando financeiro</span>
        </div>
      </div>
    );
  }

  if (!league?.id) {
    return (
      <div className="min-h-screen bg-[#050505] px-6 py-10 text-white">
        <div className="mx-auto max-w-3xl rounded-[2rem] border border-zinc-800 bg-zinc-950/80 p-8 text-center">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full border border-red-500/20 bg-red-500/10 text-red-300">
            <ShieldAlert size={24} />
          </div>
          <p className="mt-5 text-[10px] font-black uppercase tracking-[0.28em] text-zinc-500">Acesso restrito</p>
          <h1 className="mt-4 text-3xl font-black uppercase tracking-tight text-white">Você não tem acesso ao financeiro desta liga</h1>
          <p className="mt-3 text-sm leading-6 text-zinc-400">
            O extrato é liberado apenas para cargos de gestão da liga ou para o Master da plataforma.
          </p>
          <Link
            href={tenantPath("/ligas")}
            className="mt-6 inline-flex items-center gap-2 rounded-full border border-zinc-800 bg-zinc-950 px-5 py-3 text-xs font-black uppercase text-zinc-200 hover:bg-zinc-900"
          >
            <ArrowLeft size={14} />
            Voltar para ligas
          </Link>
        </div>
      </div>
    );
  }

  const basePath = `/ligas/${encodeURIComponent(league.id)}`;

  return (
    <FinancialStatementPage
      scopeType="league"
      scopeId={league.id}
      title={league.sigla?.trim() || league.nome || "Liga"}
      subtitle="Extrato isolado da liga: loja, eventos e modo vendas vinculados somente a esta entidade."
      eyebrow="Financeiro da liga"
      logoSrc={resolveLeagueLogoSrc(league) || "/placeholder_liga.png"}
      backHref={`${basePath}/gestao`}
      basePath={basePath}
      biLinks={[
        { label: "BI Gestão", href: `${basePath}/gestao/eventos` },
        { label: "BI Loja", href: `${basePath}/gestao/produtos` },
        { label: "Frequência", href: `${basePath}/gestao/frequencia` },
      ]}
    />
  );
}
