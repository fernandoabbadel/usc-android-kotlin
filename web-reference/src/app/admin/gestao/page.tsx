"use client";

import Link from "next/link";
import {
  ArrowLeft,
  BarChart3,
  Dumbbell,
  FileSpreadsheet,
  ShoppingBag,
  Ticket,
} from "lucide-react";

import { useTenantTheme } from "@/context/TenantThemeContext";
import { withTenantSlug } from "@/lib/tenantRouting";

const cards = [
  {
    title: "Eventos",
    description: "BI comercial, operacional, portaria, estratégico e modo vendas.",
    href: "/admin/gestao/eventos",
    icon: Ticket,
  },
  {
    title: "BI Loja",
    description: "Produtos oficiais da atlética, sem misturar mini vendors ou entidades.",
    href: "/admin/gestao/loja",
    icon: ShoppingBag,
  },
  {
    title: "Treinos",
    description: "Presenças, confirmações, modalidades e frequência por data.",
    href: "/admin/gestao/treinos",
    icon: Dumbbell,
  },
  {
    title: "Financeiro",
    description: "Extrato completo com pedidos, aprovações, QR, transferências, custo e CSV.",
    href: "/admin/gestao/financeiro",
    icon: FileSpreadsheet,
  },
];

export default function AdminGestaoPage() {
  const { tenantSlug } = useTenantTheme();
  const tenantHref = (path: string) => (tenantSlug ? withTenantSlug(tenantSlug, path) : path);

  return (
    <main className="min-h-screen bg-[#050505] px-4 py-5 text-white md:px-6">
      <div className="mx-auto max-w-7xl space-y-5">
        <header className="rounded-lg border border-zinc-800 bg-zinc-950/70 p-4">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            <div className="flex items-center gap-3">
              <Link href={tenantHref("/admin")} className="rounded-lg border border-zinc-800 bg-black p-2 text-zinc-300 hover:text-white">
                <ArrowLeft size={18} />
              </Link>
              <div>
                <p className="text-[10px] font-black uppercase tracking-[0.28em] text-emerald-300">
                  Gestão administrativa
                </p>
                <h1 className="mt-1 text-2xl font-black uppercase text-white">Gestão</h1>
                <p className="mt-1 text-sm font-bold text-zinc-500">
                  Painéis de BI e extrato financeiro da atlética.
                </p>
              </div>
            </div>
            <Link
              href={tenantHref("/admin/gestao/financeiro")}
              className="inline-flex items-center justify-center gap-2 rounded-lg border border-emerald-500/30 bg-emerald-500 px-4 py-3 text-xs font-black uppercase text-black hover:bg-emerald-400"
            >
              <FileSpreadsheet size={15} />
              Abrir Financeiro
            </Link>
          </div>
        </header>

        <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          {cards.map((card) => {
            const Icon = card.icon;
            return (
              <Link
                key={card.href}
                href={tenantHref(card.href)}
                className="group flex min-h-44 flex-col justify-between rounded-lg border border-zinc-800 bg-zinc-950 p-5 transition hover:border-emerald-400/50 hover:bg-zinc-900"
              >
                <span className="inline-flex h-11 w-11 items-center justify-center rounded-lg border border-emerald-400/20 bg-emerald-400/10 text-emerald-300">
                  <Icon size={20} />
                </span>
                <span>
                  <strong className="block text-lg font-black uppercase text-white">{card.title}</strong>
                  <span className="mt-2 block text-sm font-semibold leading-5 text-zinc-500">{card.description}</span>
                </span>
              </Link>
            );
          })}
        </section>

        <section className="rounded-lg border border-zinc-800 bg-zinc-950/70 p-4">
          <div className="flex items-center gap-2 text-xs font-black uppercase tracking-[0.2em] text-zinc-400">
            <BarChart3 size={16} className="text-emerald-300" />
            Integrações de BI
          </div>
          <div className="mt-4 flex flex-wrap gap-2">
            <Link href={tenantHref("/admin/bi/comercial")} className="rounded-lg border border-zinc-800 bg-black px-3 py-2 text-xs font-black uppercase text-zinc-300 hover:border-zinc-600">
              BI Comercial
            </Link>
            <Link href={tenantHref("/admin/bi/operacional")} className="rounded-lg border border-zinc-800 bg-black px-3 py-2 text-xs font-black uppercase text-zinc-300 hover:border-zinc-600">
              BI Operacional
            </Link>
            <Link href={tenantHref("/admin/bi/portaria")} className="rounded-lg border border-zinc-800 bg-black px-3 py-2 text-xs font-black uppercase text-zinc-300 hover:border-zinc-600">
              BI Portaria
            </Link>
            <Link href={tenantHref("/admin/gestao/loja")} className="rounded-lg border border-zinc-800 bg-black px-3 py-2 text-xs font-black uppercase text-zinc-300 hover:border-zinc-600">
              BI Loja
            </Link>
          </div>
        </section>
      </div>
    </main>
  );
}
