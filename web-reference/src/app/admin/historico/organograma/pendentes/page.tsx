"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { ArrowLeft, CheckCircle2, Loader2, Save, Trash2 } from "lucide-react";

import { useAuth } from "@/context/AuthContext";
import { useTenantTheme } from "@/context/TenantThemeContext";
import { useToast } from "@/context/ToastContext";
import {
  fetchOrganogramConfig,
  getDefaultOrganogramConfig,
  saveOrganogramConfig,
  type OrganogramConfig,
  type OrganogramMemberRecord,
} from "@/lib/organogramService";
import { withTenantSlug } from "@/lib/tenantRouting";

const normalizeSectionName = (value: string): string =>
  value.trim().replace(/\s+/g, " ").slice(0, 60) || "Diretoria";

const approvedAtNow = (): string => new Date().toISOString();

export default function AdminOrganogramaPendentesPage() {
  const { user } = useAuth();
  const { addToast } = useToast();
  const { tenantId, tenantSlug, tenantLogoUrl } = useTenantTheme();
  const [config, setConfig] = useState<OrganogramConfig>(getDefaultOrganogramConfig());
  const [loading, setLoading] = useState(true);
  const [savingId, setSavingId] = useState("");

  const backHref = tenantSlug
    ? withTenantSlug(tenantSlug, "/admin/historico/organograma")
    : "/admin/historico/organograma";

  const pendingMembers = useMemo(
    () => config.membros.filter((member) => member.status === "pending"),
    [config.membros]
  );

  useEffect(() => {
    let mounted = true;
    const load = async () => {
      try {
        setLoading(true);
        const nextConfig = await fetchOrganogramConfig({
          forceRefresh: true,
          tenantId: tenantId || undefined,
        });
        if (mounted) setConfig(nextConfig);
      } catch (error: unknown) {
        console.error(error);
        if (mounted) addToast("Erro ao carregar pendentes do organograma.", "error");
      } finally {
        if (mounted) setLoading(false);
      }
    };

    void load();
    return () => {
      mounted = false;
    };
  }, [addToast, tenantId]);

  const updatePendingMember = (memberId: string, patch: Partial<OrganogramMemberRecord>) => {
    setConfig((current) => ({
      ...current,
      membros: current.membros.map((member) =>
        member.id === memberId
          ? {
              ...member,
              ...patch,
              secao: patch.secao ? normalizeSectionName(patch.secao) : member.secao,
              updatedAt: approvedAtNow(),
            }
          : member
      ),
    }));
  };

  const persistConfig = async (nextConfig: OrganogramConfig, successMessage: string) => {
    await saveOrganogramConfig(nextConfig, { tenantId: tenantId || undefined });
    setConfig(nextConfig);
    addToast(successMessage, "success");
  };

  const approveMember = async (memberId: string) => {
    const now = approvedAtNow();
    const nextConfig = {
      ...config,
      membros: config.membros.map((member) =>
        member.id === memberId
          ? {
              ...member,
              status: "approved" as const,
              approvedAt: now,
              approvedBy: user?.uid || "",
              updatedAt: now,
            }
          : member
      ),
    };

    try {
      setSavingId(memberId);
      await persistConfig(nextConfig, "Membro aprovado no organograma.");
    } catch (error: unknown) {
      console.error(error);
      addToast("Erro ao aprovar membro.", "error");
    } finally {
      setSavingId("");
    }
  };

  const removeMember = async (memberId: string) => {
    if (!window.confirm("Remover esta solicitação pendente?")) return;
    const nextConfig = {
      ...config,
      membros: config.membros.filter((member) => member.id !== memberId),
    };

    try {
      setSavingId(memberId);
      await persistConfig(nextConfig, "Solicitação removida.");
    } catch (error: unknown) {
      console.error(error);
      addToast("Erro ao remover solicitação.", "error");
    } finally {
      setSavingId("");
    }
  };

  const saveEdits = async (memberId: string) => {
    try {
      setSavingId(memberId);
      await persistConfig(config, "Solicitação atualizada.");
    } catch (error: unknown) {
      console.error(error);
      addToast("Erro ao salvar edição.", "error");
    } finally {
      setSavingId("");
    }
  };

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#050505] text-white">
        <Loader2 className="animate-spin text-emerald-400" size={34} />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#050505] pb-20 text-white">
      <header className="sticky top-0 z-20 border-b border-zinc-800 bg-[#050505]/92 px-6 py-5 backdrop-blur-md">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <Link
              href={backHref}
              className="inline-flex h-11 w-11 items-center justify-center rounded-full border border-zinc-800 bg-zinc-900 transition hover:border-zinc-600"
            >
              <ArrowLeft size={18} />
            </Link>
            <div>
              <p className="text-[10px] font-black uppercase tracking-[0.3em] text-zinc-500">
                Organograma
              </p>
              <h1 className="text-2xl font-black uppercase tracking-tight">
                Aprovações pendentes
              </h1>
            </div>
          </div>
          <span className="rounded-2xl border border-amber-500/30 bg-amber-500/10 px-4 py-3 text-xs font-black uppercase text-amber-100">
            {pendingMembers.length} pendente{pendingMembers.length === 1 ? "" : "s"}
          </span>
        </div>
      </header>

      <main className="mx-auto max-w-6xl space-y-4 px-6 py-6">
        {pendingMembers.length === 0 ? (
          <section className="rounded-[2rem] border border-dashed border-zinc-800 bg-zinc-950/80 p-10 text-center">
            <p className="text-sm font-bold text-zinc-400">
              Nenhuma solicitação pendente no organograma.
            </p>
          </section>
        ) : (
          pendingMembers.map((member) => (
            <article
              key={member.id}
              className="rounded-[2rem] border border-amber-500/20 bg-zinc-950/90 p-5"
            >
              <div className="flex flex-col gap-5 lg:flex-row lg:items-center">
                <div className="flex min-w-0 flex-1 items-center gap-4">
                  <div className="relative h-16 w-16 shrink-0 overflow-hidden rounded-2xl border border-zinc-700 bg-black">
                    <Image
                      src={member.foto || tenantLogoUrl || "/logo.png"}
                      alt={member.nome || "Usuário"}
                      fill
                      sizes="64px"
                      className="object-cover"
                      unoptimized={Boolean(member.foto?.startsWith("http"))}
                    />
                  </div>
                  <div className="min-w-0">
                    <p className="text-[10px] font-black uppercase tracking-[0.24em] text-amber-200">
                      Solicitante
                    </p>
                    <h2 className="mt-1 truncate text-lg font-black uppercase text-white">
                      {member.nome || member.userId || "Usuário"}
                    </h2>
                    <p className="mt-1 text-xs font-semibold text-zinc-500">
                      {member.requestedAt
                        ? new Date(member.requestedAt).toLocaleString("pt-BR")
                        : "Data não informada"}
                    </p>
                  </div>
                </div>

                <div className="grid flex-1 gap-3 md:grid-cols-2">
                  <label className="text-[11px] font-black uppercase tracking-[0.18em] text-zinc-500">
                    Cargo
                    <input
                      value={member.cargo}
                      onChange={(event) =>
                        updatePendingMember(member.id, { cargo: event.target.value })
                      }
                      className="mt-2 w-full rounded-2xl border border-zinc-800 bg-black/35 px-4 py-3 text-sm text-white outline-none focus:border-amber-400"
                    />
                  </label>
                  <label className="text-[11px] font-black uppercase tracking-[0.18em] text-zinc-500">
                    Área
                    <input
                      value={member.secao}
                      onChange={(event) =>
                        updatePendingMember(member.id, { secao: event.target.value })
                      }
                      className="mt-2 w-full rounded-2xl border border-zinc-800 bg-black/35 px-4 py-3 text-sm text-white outline-none focus:border-amber-400"
                    />
                  </label>
                </div>

                <div className="flex flex-wrap gap-2 lg:justify-end">
                  <button
                    type="button"
                    onClick={() => void saveEdits(member.id)}
                    disabled={savingId === member.id}
                    className="inline-flex items-center gap-2 rounded-2xl border border-zinc-700 bg-black/40 px-4 py-3 text-xs font-black uppercase text-zinc-200 transition hover:border-zinc-500 disabled:opacity-60"
                  >
                    {savingId === member.id ? (
                      <Loader2 size={14} className="animate-spin" />
                    ) : (
                      <Save size={14} />
                    )}
                    Salvar edição
                  </button>
                  <button
                    type="button"
                    onClick={() => void approveMember(member.id)}
                    disabled={savingId === member.id}
                    className="inline-flex items-center gap-2 rounded-2xl bg-emerald-500 px-4 py-3 text-xs font-black uppercase text-black transition hover:bg-emerald-400 disabled:opacity-60"
                  >
                    {savingId === member.id ? (
                      <Loader2 size={14} className="animate-spin" />
                    ) : (
                      <CheckCircle2 size={14} />
                    )}
                    Aprovar
                  </button>
                  <button
                    type="button"
                    onClick={() => void removeMember(member.id)}
                    disabled={savingId === member.id}
                    className="inline-flex items-center gap-2 rounded-2xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-xs font-black uppercase text-red-100 transition hover:bg-red-500/20 disabled:opacity-60"
                  >
                    <Trash2 size={14} />
                    Remover
                  </button>
                </div>
              </div>
            </article>
          ))
        )}
      </main>
    </div>
  );
}
