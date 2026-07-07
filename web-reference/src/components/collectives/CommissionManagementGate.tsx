"use client";

import { useEffect, useRef, useState, type ReactNode } from "react";
import Image from "next/image";
import Link from "next/link";
import { useParams, usePathname, useRouter } from "next/navigation";
import { ArrowLeft, Loader2, ShieldAlert } from "lucide-react";

import { DataUseRequiredModal } from "@/app/components/legal/DataUseConsentBox";
import { useAuth } from "@/context/AuthContext";
import { useTenantTheme } from "@/context/TenantThemeContext";
import {
  fetchManagedLeagueSummaries,
  type ManagedLeagueRecord,
} from "@/lib/leaguesService";
import { resolveLeagueLogoSrc } from "@/lib/leagueMedia";
import { isPlatformMaster } from "@/lib/roles";
import { withTenantSlug } from "@/lib/tenantRouting";

type CommissionManagementGateProps = {
  children: (payload: {
    leagueId: string;
    league: ManagedLeagueRecord;
    routeSegment: string;
  }) => ReactNode;
};

const SELECTED_COMMISSION_STORAGE_PREFIX = "usc_selected_management_commission";

const selectedCommissionStorageKey = (payload: {
  tenantId?: string;
  tenantSlug?: string;
  userId?: string;
}): string => {
  const tenantScope =
    payload.tenantId?.trim() || payload.tenantSlug?.trim().toLowerCase() || "global";
  const userScope = payload.userId?.trim() || "anon";
  return `${SELECTED_COMMISSION_STORAGE_PREFIX}:${tenantScope}:${userScope}`;
};

const readSelectedCommissionId = (storageKey: string): string => {
  if (typeof window === "undefined") return "";
  try {
    return window.localStorage.getItem(storageKey)?.trim() || "";
  } catch {
    return "";
  }
};

const writeSelectedCommissionId = (storageKey: string, commissionId: string): void => {
  if (typeof window === "undefined") return;
  try {
    const cleanCommissionId = commissionId.trim();
    if (cleanCommissionId) {
      window.localStorage.setItem(storageKey, cleanCommissionId);
    } else {
      window.localStorage.removeItem(storageKey);
    }
  } catch {
    // localStorage pode estar bloqueado em alguns navegadores; a sessão atual continua funcionando.
  }
};

const normalizeRouteToken = (value: unknown): string =>
  String(value || "")
    .trim()
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^a-z0-9-]+/g, "-")
    .replace(/-+/g, "-")
    .replace(/^-|-$/g, "");

const resolveCommissionRouteSegment = (
  commission: ManagedLeagueRecord,
  preferred?: string
): string => {
  const preferredValue = String(preferred || "").trim();
  if (preferredValue) return preferredValue;
  return (
    commission.turmaId?.trim() ||
    commission.sigla?.trim() ||
    commission.id.trim()
  );
};

const matchesCommissionRouteSegment = (
  commission: ManagedLeagueRecord,
  segment: string
): boolean => {
  const normalizedSegment = normalizeRouteToken(decodeURIComponent(segment));
  if (!normalizedSegment) return false;
  return [
    commission.id,
    commission.turmaId,
    commission.sigla,
    commission.nome,
  ].some((candidate) => normalizeRouteToken(candidate) === normalizedSegment);
};

export function CommissionManagementGate({ children }: CommissionManagementGateProps) {
  const params = useParams<{ leagueId?: string }>();
  const pathname = usePathname();
  const router = useRouter();
  const { user, loading: authLoading } = useAuth();
  const { tenantId, tenantSlug, loading: tenantLoading } = useTenantTheme();
  const [loading, setLoading] = useState(true);
  const [managedCommissions, setManagedCommissions] = useState<ManagedLeagueRecord[]>([]);
  const [selectedLeagueId, setSelectedLeagueId] = useState("");
  const selectedLeagueIdRef = useRef("");
  const routeSegment =
    typeof params?.leagueId === "string" ? decodeURIComponent(params.leagueId).trim() : "";

  const tenantPath = (path: string) => (tenantSlug ? withTenantSlug(tenantSlug, path) : path);
  const storageKey = selectedCommissionStorageKey({
    tenantId,
    tenantSlug,
    userId: user?.uid,
  });

  useEffect(() => {
    selectedLeagueIdRef.current = selectedLeagueId;
  }, [selectedLeagueId]);

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
          category: "comissao",
        });
        if (!mounted) return;
        const orderedRows = rows
          .filter((row) => row.id)
          .sort(
            (left, right) =>
              (left.turmaId || "").localeCompare(right.turmaId || "", "pt-BR") ||
              left.nome.localeCompare(right.nome, "pt-BR")
          );
        const availableIds = new Set(orderedRows.map((row) => row.id));
        const currentSelectedId = selectedLeagueIdRef.current;
        const storedSelectedId = readSelectedCommissionId(storageKey);
        const routeSelectedId =
          routeSegment
            ? orderedRows.find((row) => matchesCommissionRouteSegment(row, routeSegment))?.id || ""
            : "";
        const nextSelectedId = availableIds.has(routeSelectedId)
          ? routeSelectedId
          : availableIds.has(currentSelectedId)
          ? currentSelectedId
          : availableIds.has(storedSelectedId)
          ? storedSelectedId
          : orderedRows.length === 1
          ? orderedRows[0].id
          : "";
        setManagedCommissions(orderedRows);
        setSelectedLeagueId(nextSelectedId);
        writeSelectedCommissionId(storageKey, nextSelectedId);
      } catch (error) {
        console.error(error);
        if (!mounted) return;
        setManagedCommissions([]);
        setSelectedLeagueId("");
        writeSelectedCommissionId(storageKey, "");
      } finally {
        if (mounted) setLoading(false);
      }
    };

    void load();
    return () => {
      mounted = false;
    };
  }, [authLoading, routeSegment, storageKey, tenantId, tenantLoading, user]);

  const selectedLeague =
    managedCommissions.find((commission) => commission.id === selectedLeagueId) || null;

  useEffect(() => {
    if (!selectedLeague || routeSegment) return;
    const segment = resolveCommissionRouteSegment(selectedLeague);
    const marker = "/comissoes/configurar";
    const markerIndex = pathname.indexOf(marker);
    if (!segment || markerIndex < 0) return;
    const nextPath = `${pathname.slice(0, markerIndex)}${marker}/${encodeURIComponent(segment)}${pathname.slice(markerIndex + marker.length)}`;
    if (nextPath !== pathname) {
      router.replace(nextPath);
    }
  }, [pathname, routeSegment, router, selectedLeague]);

  const selectCommission = (commission: ManagedLeagueRecord) => {
    selectedLeagueIdRef.current = commission.id;
    writeSelectedCommissionId(storageKey, commission.id);
    setSelectedLeagueId(commission.id);
  };

  if (authLoading || tenantLoading || loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#050505] text-white">
        <div className="flex items-center gap-3 rounded-2xl border border-zinc-800 bg-zinc-950/80 px-5 py-4">
          <Loader2 className="animate-spin text-brand" size={18} />
          <span className="text-sm font-bold uppercase tracking-[0.2em] text-zinc-400">Carregando gestão</span>
        </div>
      </div>
    );
  }

  if (selectedLeague) {
    return (
      <>
        <DataUseRequiredModal
          userId={user?.uid || null}
          contextType="commission_management_data_use"
          contextId={`${selectedLeague.id}:${user?.uid || ""}`}
          tenantId={tenantId || null}
          source="app"
          metadata={{
            authorizationScope: "comissao",
            leagueId: selectedLeague.id,
            leagueName: selectedLeague.nome,
            role: selectedLeague.managementRole || null,
          }}
        />
        {children({
          leagueId: selectedLeague.id,
          league: selectedLeague,
          routeSegment: resolveCommissionRouteSegment(selectedLeague, routeSegment),
        })}
      </>
    );
  }

  if (managedCommissions.length === 0) {
    return (
      <div className="min-h-screen bg-[#050505] px-6 py-10 text-white">
        <div className="mx-auto max-w-3xl rounded-[2rem] border border-zinc-800 bg-zinc-950/80 p-8 text-center">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full border border-red-500/20 bg-red-500/10 text-red-300">
            <ShieldAlert size={24} />
          </div>
          <p className="mt-5 text-[10px] font-black uppercase tracking-[0.28em] text-zinc-500">Acesso restrito</p>
          <h1 className="mt-4 text-3xl font-black uppercase tracking-tight text-white">Você não tem comissão para gerenciar</h1>
          <p className="mt-3 text-sm leading-6 text-zinc-400">
            O acesso é liberado para Presidente, Vice-Presidente, Secretaria, Tesouraria, Diretoria ou master da plataforma.
          </p>
          <Link
            href={tenantPath("/comissoes")}
            className="mt-6 inline-flex items-center gap-2 rounded-full border border-zinc-800 bg-zinc-950/80 px-5 py-3 text-xs font-black uppercase text-zinc-200 hover:bg-zinc-900"
          >
            <ArrowLeft size={14} />
            Voltar para comissões
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#050505] px-6 py-8 text-white">
      <div className="mx-auto max-w-6xl">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <Link
            href={tenantPath("/comissoes")}
            className="inline-flex items-center gap-2 rounded-full border border-zinc-800 bg-zinc-950/80 px-4 py-2 text-[11px] font-black uppercase tracking-[0.2em] text-zinc-300 hover:bg-zinc-900"
          >
            <ArrowLeft size={14} />
            Comissões
          </Link>
          <span className="rounded-full border border-brand/30 bg-brand-soft px-4 py-2 text-[11px] font-black uppercase tracking-[0.2em] text-brand-accent">
            {managedCommissions.length} disponíveis
          </span>
        </div>

        <div className="mt-8 rounded-[2rem] border border-zinc-800 bg-zinc-950/80 p-6">
          <p className="text-[10px] font-black uppercase tracking-[0.28em] text-brand-accent">Gestão de comissões</p>
          <h1 className="mt-3 text-3xl font-black uppercase tracking-tight text-white">Escolha a comissão</h1>
          <p className="mt-3 text-sm leading-6 text-zinc-400">
            Abra a página de configuração da comissão em que você tem cargo de gestão.
          </p>

          <div className="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {managedCommissions.map((commission) => {
              const imageSrc = resolveLeagueLogoSrc(commission, "/placeholder_liga.png");
              return (
                <button
                  key={commission.id}
                  type="button"
                  onClick={() => selectCommission(commission)}
                  className="rounded-[1.5rem] border border-white/10 bg-black/30 p-4 text-left transition hover:border-brand/30 hover:bg-zinc-900"
                >
                  <div className="flex items-center gap-4">
                    <div className="relative h-16 w-16 shrink-0 overflow-hidden rounded-2xl border border-white/10 bg-black/40">
                      <Image src={imageSrc} alt={commission.nome} fill sizes="64px" className="object-cover" />
                    </div>
                    <div className="min-w-0">
                      <p className="text-[10px] font-black uppercase tracking-[0.22em] text-brand-accent">
                        {commission.turmaId || commission.sigla || "Comissão"}
                      </p>
                      <h2 className="mt-2 truncate text-lg font-black text-white">{commission.nome}</h2>
                      <p className="mt-1 text-xs font-semibold text-zinc-500">
                        {commission.managementRole || "Gestão"}
                      </p>
                    </div>
                  </div>
                </button>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}
