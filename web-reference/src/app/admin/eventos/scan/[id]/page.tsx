"use client";

import { useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import { Loader2 } from "lucide-react";

import { useTenantTheme } from "@/context/TenantThemeContext";
import { getSupabaseClient } from "@/lib/supabase";
import { withTenantSlug } from "@/lib/tenantRouting";

const asRecord = (value: unknown): Record<string, unknown> | null =>
  typeof value === "object" && value !== null ? (value as Record<string, unknown>) : null;

const asString = (value: unknown): string =>
  typeof value === "string" ? value.trim() : "";

const normalizeScope = (value: unknown): string =>
  asString(value)
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "");

const contextualScanPath = (eventId: string, eventRow: unknown): string => {
  const stats = asRecord(asRecord(eventRow)?.stats) ?? {};
  const scope = normalizeScope(stats.collectiveType || stats.scopeType || stats.scope_type || stats.ownerType);
  const leagueId = asString(stats.leagueId || stats.ligaId);
  const directoryId = asString(stats.directoryId || stats.diretorioId);
  const commissionId = asString(stats.commissionId || stats.comissaoId);
  const encodedEventId = encodeURIComponent(eventId);

  if (scope === "commission" || scope === "comissao" || commissionId) {
    const ownerId = encodeURIComponent(commissionId || leagueId);
    return ownerId
      ? `/comissoes/configurar/${ownerId}/eventos/${encodedEventId}/scan`
      : `/admin/eventos/${encodedEventId}/scan`;
  }

  if (scope === "directory" || scope === "diretorio" || directoryId) {
    const ownerId = encodeURIComponent(directoryId || leagueId);
    return ownerId
      ? `/diretorio/configurar/${ownerId}/eventos/${encodedEventId}/scan`
      : `/admin/eventos/${encodedEventId}/scan`;
  }

  if (leagueId) {
    return `/ligas/${encodeURIComponent(leagueId)}/eventos/${encodedEventId}/scan`;
  }

  return `/admin/eventos/${encodedEventId}/scan`;
};

export default function LegacyAdminEventoScanRedirectPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const { tenantSlug, loading: tenantLoading } = useTenantTheme();
  const eventId = params?.id ? decodeURIComponent(params.id).trim() : "";

  useEffect(() => {
    if (!eventId || tenantLoading) return;
    let active = true;

    const redirect = async () => {
      let targetPath = `/admin/eventos/${encodeURIComponent(eventId)}/scan`;
      try {
        const { data } = await getSupabaseClient()
          .from("eventos")
          .select("id,stats")
          .eq("id", eventId)
          .maybeSingle();
        targetPath = contextualScanPath(eventId, data);
      } catch (error) {
        console.error(error);
      }

      if (!active) return;
      router.replace(tenantSlug ? withTenantSlug(tenantSlug, targetPath) : targetPath);
    };

    void redirect();
    return () => {
      active = false;
    };
  }, [eventId, router, tenantLoading, tenantSlug]);

  return (
    <main className="flex min-h-screen items-center justify-center bg-[#050505] px-6 text-white">
      <div className="flex items-center gap-3 rounded-2xl border border-zinc-800 bg-zinc-950/80 px-5 py-4">
        <Loader2 className="animate-spin text-brand" size={18} />
        <span className="text-sm font-bold uppercase tracking-[0.2em] text-zinc-400">
          Abrindo scan no contexto correto
        </span>
      </div>
    </main>
  );
}
