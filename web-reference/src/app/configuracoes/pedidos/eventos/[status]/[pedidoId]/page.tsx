"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import { useTenantTheme } from "@/context/TenantThemeContext";
import { withTenantSlug } from "@/lib/tenantRouting";
import { PedidosByTypePage } from "../../../_components/PedidosByTypePage";

const firstParamValue = (value: string | string[] | undefined): string =>
  Array.isArray(value) ? value[0] || "" : value || "";

export default function ConfigPedidoEventoDetalhePage() {
  const params = useParams<{ status?: string | string[]; pedidoId?: string | string[] }>();
  const status = firstParamValue(params.status) || "pendentes";
  const pedidoId = firstParamValue(params.pedidoId);
  const { tenantSlug } = useTenantTheme();
  const tenantPath = (path: string) => (tenantSlug ? withTenantSlug(tenantSlug, path) : path);

  return (
    <div className="min-h-screen bg-[#050505] pb-24 font-sans text-white">
      <header className="sticky top-0 z-30 flex items-center gap-4 border-b border-white/5 bg-[#050505]/90 p-4 backdrop-blur-md">
        <Link
          href={tenantPath(`/configuracoes/pedidos/eventos?status=${status}`)}
          className="-ml-2 rounded-full p-2 text-zinc-400 transition hover:bg-zinc-900 hover:text-white"
        >
          <ArrowLeft size={24} />
        </Link>
        <h1 className="text-xl font-black italic uppercase tracking-tighter text-white">Detalhe do pedido</h1>
      </header>
      <PedidosByTypePage tab="eventos" detailPedidoId={pedidoId} initialStatusSlug={status} />
    </div>
  );
}
