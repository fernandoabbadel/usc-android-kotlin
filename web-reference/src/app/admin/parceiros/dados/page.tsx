"use client";

import React, { useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { ArrowLeft, BarChart3, ChevronLeft, ChevronRight, Loader2, PieChart as PieIcon } from "lucide-react";

import { useToast } from "@/context/ToastContext";
import {
  fetchAdminPartnersBundle,
  type PartnerRecord,
  type PartnerScanRecord,
} from "@/lib/partnersService";

const COLORS = ["#10b981", "#3b82f6", "#f59e0b", "#ef4444", "#a855f7", "#14b8a6", "#f97316"];
const PAGE_SIZE = 8;

const chartTooltipStyle = {
  background: "#09090b",
  border: "1px solid #27272a",
  borderRadius: 12,
  color: "#fff",
};

const formatCurrency = (value: number): string =>
  new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
    maximumFractionDigits: 2,
  }).format(Number.isFinite(value) ? value : 0);

const parseMoney = (scan: PartnerScanRecord): number => {
  if (Number.isFinite(scan.couponValueNumeric) && scan.couponValueNumeric > 0) {
    return scan.couponValueNumeric;
  }

  const raw = scan.couponValue || scan.valorEconomizado || "";
  if (raw.includes("%")) return 0;

  const normalized = raw
    .replace(/[^\d,.-]/g, "")
    .replace(/\./g, "")
    .replace(",", ".");
  const parsed = Number(normalized);
  return Number.isFinite(parsed) ? parsed : 0;
};

const qrTypeLabel = (mode: string): string => {
  if (mode === "manual_partner") return "Tipo 2: ativação manual";
  if (mode === "printed_qr") return "Tipo 3: QR impresso";
  return "Tipo 1: QR do usuário";
};

const daysSince = (value: unknown): number | null => {
  if (!value) return null;
  const date = new Date(String(value));
  if (Number.isNaN(date.getTime())) return null;
  return Math.max(0, Math.floor((Date.now() - date.getTime()) / 86_400_000));
};

export default function AdminParceirosDadosPage() {
  const { addToast } = useToast();
  const [partners, setPartners] = useState<PartnerRecord[]>([]);
  const [scans, setScans] = useState<PartnerScanRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [metric, setMetric] = useState<"quantidade" | "valor">("quantidade");
  const [usersPage, setUsersPage] = useState(1);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const bundle = await fetchAdminPartnersBundle({
        partnersLimit: 600,
        scansLimit: 1200,
        forceRefresh: true,
      });
      setPartners(bundle.partners);
      setScans(bundle.scans);
    } catch (error: unknown) {
      console.error(error);
      addToast("Erro ao carregar BI de parceiros.", "error");
    } finally {
      setLoading(false);
    }
  }, [addToast]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  const partnerScanData = useMemo(() => {
    const partnerMap = new Map(partners.map((partner) => [partner.id, partner.nome]));
    const grouped = new Map<string, { name: string; quantidade: number; valor: number }>();

    scans.forEach((scan) => {
      const partnerId = scan.empresaId;
      const current = grouped.get(partnerId) || {
        name: partnerMap.get(partnerId) || scan.empresa || "Parceiro",
        quantidade: 0,
        valor: 0,
      };
      current.quantidade += 1;
      current.valor += parseMoney(scan);
      grouped.set(partnerId, current);
    });

    return [...grouped.values()]
      .sort((a, b) => b[metric] - a[metric])
      .slice(0, 12);
  }, [metric, partners, scans]);

  const couponUsageData = useMemo(() => {
    const grouped = new Map<string, { name: string; quantidade: number; valor: number }>();
    scans.forEach((scan) => {
      const name = scan.couponTitle || scan.cupom || "Cupom";
      const current = grouped.get(name) || { name, quantidade: 0, valor: 0 };
      current.quantidade += 1;
      current.valor += parseMoney(scan);
      grouped.set(name, current);
    });
    return [...grouped.values()].sort((a, b) => b[metric] - a[metric]).slice(0, 10);
  }, [metric, scans]);

  const qrTypeData = useMemo(() => {
    const grouped = new Map<string, { name: string; quantidade: number }>();
    scans.forEach((scan) => {
      const name = qrTypeLabel(scan.approvalMode || "direct_scan");
      const current = grouped.get(name) || { name, quantidade: 0 };
      current.quantidade += 1;
      grouped.set(name, current);
    });
    return [...grouped.values()].sort((a, b) => b.quantidade - a.quantidade);
  }, [scans]);

  const topUsers = useMemo(() => {
    const grouped = new Map<string, { user: string; userId: string; scans: number; valor: number }>();
    scans.forEach((scan) => {
      const key = scan.userId || scan.usuario;
      const current = grouped.get(key) || {
        user: scan.userDisplayName || scan.usuario || "Usuário",
        userId: scan.userId || "-",
        scans: 0,
        valor: 0,
      };
      current.scans += 1;
      current.valor += parseMoney(scan);
      grouped.set(key, current);
    });
    return [...grouped.values()].sort((a, b) => b.scans - a.scans || b.valor - a.valor);
  }, [scans]);

  const inactivePartners = useMemo(() => {
    const lastByPartner = new Map<string, unknown>();
    scans.forEach((scan) => {
      if (!lastByPartner.has(scan.empresaId)) {
        lastByPartner.set(scan.empresaId, scan.timestamp || scan.data);
      }
    });

    return partners
      .map((partner) => ({
        id: partner.id,
        name: partner.nome,
        days: daysSince(lastByPartner.get(partner.id)),
      }))
      .sort((a, b) => (b.days ?? 99999) - (a.days ?? 99999))
      .slice(0, 8);
  }, [partners, scans]);

  const paginatedUsers = topUsers.slice((usersPage - 1) * PAGE_SIZE, usersPage * PAGE_SIZE);
  const hasNextUsers = usersPage * PAGE_SIZE < topUsers.length;

  return (
    <div className="min-h-screen bg-[#050505] pb-20 text-white">
      <header className="sticky top-0 z-20 border-b border-zinc-800 bg-[#050505]/90 px-6 py-5 backdrop-blur-md">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <Link
              href="/admin/parceiros"
              className="rounded-full border border-zinc-800 bg-zinc-900 p-2 hover:bg-zinc-800"
            >
              <ArrowLeft size={18} className="text-zinc-300" />
            </Link>
            <div>
              <h1 className="text-xl font-black uppercase tracking-tight">BI de parceiros</h1>
              <p className="text-[11px] font-bold text-zinc-500">
                Scans, cupons, tipos de QR Code e uso por usuário.
              </p>
            </div>
          </div>
          <button
            onClick={() => setMetric((prev) => (prev === "quantidade" ? "valor" : "quantidade"))}
            className="rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-2.5 text-xs font-black uppercase text-emerald-300 hover:bg-emerald-500/15"
          >
            Métrica: {metric === "quantidade" ? "Quantidade" : "Valor R$"}
          </button>
        </div>
      </header>

      <main className="space-y-6 px-6 py-6">
        {loading ? (
          <div className="flex items-center justify-center gap-2 py-20 text-zinc-400">
            <Loader2 size={18} className="animate-spin" />
            Carregando BI...
          </div>
        ) : (
          <>
            <section className="grid gap-4 lg:grid-cols-2">
              <div className="rounded-3xl border border-zinc-800 bg-zinc-900 p-5">
                <div className="mb-4 flex items-center justify-between gap-3">
                  <h2 className="flex items-center gap-2 text-sm font-black uppercase">
                    <PieIcon size={16} className="text-emerald-400" />
                    Scans por parceiro
                  </h2>
                  <span className="text-[10px] font-bold uppercase text-zinc-500">
                    {metric === "quantidade" ? "número" : "valor R$"}
                  </span>
                </div>
                <div className="h-80">
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={partnerScanData}
                        dataKey={metric}
                        nameKey="name"
                        innerRadius={70}
                        outerRadius={110}
                        paddingAngle={2}
                      >
                        {partnerScanData.map((entry, index) => (
                          <Cell key={entry.name} fill={COLORS[index % COLORS.length]} />
                        ))}
                      </Pie>
                      <Tooltip
                        contentStyle={chartTooltipStyle}
                        formatter={(value) =>
                          metric === "valor" ? formatCurrency(Number(value)) : Number(value)
                        }
                      />
                      <Legend />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
              </div>

              <div className="rounded-3xl border border-zinc-800 bg-zinc-900 p-5">
                <h2 className="mb-4 flex items-center gap-2 text-sm font-black uppercase">
                  <BarChart3 size={16} className="text-blue-400" />
                  Cupons mais usados
                </h2>
                <div className="h-80">
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={couponUsageData} layout="vertical" margin={{ left: 20, right: 20 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#27272a" />
                      <XAxis type="number" stroke="#71717a" />
                      <YAxis dataKey="name" type="category" stroke="#a1a1aa" width={130} />
                      <Tooltip
                        contentStyle={chartTooltipStyle}
                        formatter={(value) =>
                          metric === "valor" ? formatCurrency(Number(value)) : Number(value)
                        }
                      />
                      <Bar dataKey={metric} fill="#3b82f6" radius={[0, 8, 8, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              </div>
            </section>

            <section className="grid gap-4 lg:grid-cols-3">
              <div className="rounded-3xl border border-zinc-800 bg-zinc-900 p-5 lg:col-span-1">
                <h2 className="mb-4 text-sm font-black uppercase">Tipos mais usados de QR Code</h2>
                <div className="h-72">
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={qrTypeData}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#27272a" />
                      <XAxis dataKey="name" stroke="#a1a1aa" tick={{ fontSize: 10 }} />
                      <YAxis stroke="#71717a" />
                      <Tooltip contentStyle={chartTooltipStyle} />
                      <Bar dataKey="quantidade" fill="#10b981" radius={[8, 8, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              </div>

              <div className="rounded-3xl border border-zinc-800 bg-zinc-900 p-5 lg:col-span-2">
                <div className="mb-4 flex items-center justify-between">
                  <h2 className="text-sm font-black uppercase">Usuários que mais usaram cupons</h2>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => setUsersPage((prev) => Math.max(1, prev - 1))}
                      disabled={usersPage === 1}
                      className="rounded-lg border border-zinc-700 bg-zinc-950 p-2 disabled:opacity-40"
                    >
                      <ChevronLeft size={14} />
                    </button>
                    <span className="text-xs font-bold text-zinc-500">Página {usersPage}</span>
                    <button
                      onClick={() => setUsersPage((prev) => prev + 1)}
                      disabled={!hasNextUsers}
                      className="rounded-lg border border-zinc-700 bg-zinc-950 p-2 disabled:opacity-40"
                    >
                      <ChevronRight size={14} />
                    </button>
                  </div>
                </div>
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-black/30 text-[10px] font-black uppercase text-zinc-500">
                      <tr>
                        <th className="p-3">Usuário</th>
                        <th className="p-3">ID</th>
                        <th className="p-3">Scans</th>
                        <th className="p-3">Valor R$</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-zinc-800">
                      {paginatedUsers.map((row) => (
                        <tr key={`${row.userId}-${row.user}`}>
                          <td className="p-3 font-bold text-white">{row.user}</td>
                          <td className="p-3 font-mono text-zinc-500">{row.userId}</td>
                          <td className="p-3">{row.scans}</td>
                          <td className="p-3">{formatCurrency(row.valor)}</td>
                        </tr>
                      ))}
                      {paginatedUsers.length === 0 ? (
                        <tr>
                          <td colSpan={4} className="p-8 text-center text-zinc-500">
                            Nenhum uso registrado.
                          </td>
                        </tr>
                      ) : null}
                    </tbody>
                  </table>
                </div>
              </div>
            </section>

            <section className="rounded-3xl border border-zinc-800 bg-zinc-900 p-5">
              <h2 className="mb-4 text-sm font-black uppercase">
                Há quantos dias ninguém usa cupom de cada parceiro
              </h2>
              <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
                {inactivePartners.map((partner) => (
                  <div key={partner.id} className="rounded-2xl border border-zinc-800 bg-black/30 p-4">
                    <p className="text-sm font-black uppercase text-white">{partner.name}</p>
                    <p className="mt-2 text-2xl font-black text-amber-300">
                      {partner.days === null ? "Sem uso" : `${partner.days} dias`}
                    </p>
                  </div>
                ))}
              </div>
            </section>
          </>
        )}
      </main>
    </div>
  );
}
