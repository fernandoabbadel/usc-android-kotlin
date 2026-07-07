"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { ArrowLeft, Loader2, QrCode, Wallet } from "lucide-react";

import { normalizePaymentConfig } from "@/lib/commerceCatalog";
import { buildEventTicketPublicPath } from "@/lib/eventTickets";
import { fetchAdminEventSalesPage } from "@/lib/eventsNativeService";
import { useTenantTheme } from "@/context/TenantThemeContext";
import { withTenantSlug } from "@/lib/tenantRouting";

type Row = Record<string, unknown>;
type DateLike = { toDate: () => Date };

const asRecord = (value: unknown): Row =>
  typeof value === "object" && value !== null && !Array.isArray(value) ? (value as Row) : {};

const asString = (value: unknown, fallback = ""): string =>
  typeof value === "string" ? value.trim() : fallback;

const parseDateLike = (value: unknown): Date | null => {
  if (!value) return null;
  if (value instanceof Date && !Number.isNaN(value.getTime())) return value;
  if (typeof value === "string" || typeof value === "number") {
    const parsed = new Date(value);
    return Number.isNaN(parsed.getTime()) ? null : parsed;
  }
  const toDate = (value as DateLike)?.toDate;
  if (typeof toDate === "function") {
    const parsed = toDate.call(value);
    return parsed instanceof Date && !Number.isNaN(parsed.getTime()) ? parsed : null;
  }
  return null;
};

const parseCurrency = (value: unknown): number => {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value !== "string") return 0;
  const parsed = Number.parseFloat(value.replace(/[^\d,.-]/g, "").replace(/\./g, "").replace(",", "."));
  return Number.isFinite(parsed) ? parsed : 0;
};

const formatDateTime = (value: unknown): string => {
  const parsed = parseDateLike(value);
  return parsed ? parsed.toLocaleString("pt-BR") : "-";
};

const formatCurrency = (value: number): string =>
  value.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

export function AdminEventTicketOrderPage({
  eventId,
  pedidoId,
  backHref,
}: {
  eventId: string;
  pedidoId: string;
  backHref?: string;
}) {
  const { tenantId, tenantSlug } = useTenantTheme();
  const tenantPath = (path: string) => (tenantSlug ? withTenantSlug(tenantSlug, path) : path);
  const [loading, setLoading] = useState(true);
  const [order, setOrder] = useState<Row | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    setError("");
    void fetchAdminEventSalesPage({
      eventId,
      pageSize: 2000,
      forceRefresh: true,
      tenantId: tenantId || undefined,
    })
      .then((page) => {
        if (!mounted) return;
        setOrder(page.rows.find((row) => asString(row.id) === pedidoId) || null);
      })
      .catch((loadError: unknown) => {
        console.error(loadError);
        if (mounted) setError(loadError instanceof Error ? loadError.message : "Erro ao carregar pedido.");
      })
      .finally(() => {
        if (mounted) setLoading(false);
      });
    return () => {
      mounted = false;
    };
  }, [eventId, pedidoId, tenantId]);

  const paymentConfig = useMemo(() => normalizePaymentConfig(order?.payment_config), [order]);
  const tickets = paymentConfig?.ticketEntries ?? [];
  const orderData = asRecord(order?.data);
  const requestedAt = order?.dataSolicitacao ?? order?.createdAt ?? orderData.dataSolicitacao ?? orderData.createdAt;
  const approvedAt = order?.dataAprovacao ?? orderData.dataAprovacao;
  const resolvedBackHref =
    backHref?.trim() || `/admin/eventos/${encodeURIComponent(eventId)}/ingressos`;
  const orderListHref = tenantPath(resolvedBackHref);

  return (
    <main className="min-h-screen bg-[#050505] px-4 py-5 text-white sm:px-6">
      <div className="mx-auto max-w-4xl space-y-4">
        <Link
          href={orderListHref}
          className="inline-flex items-center gap-2 rounded-xl border border-zinc-800 bg-zinc-950 px-3 py-2 text-xs font-black uppercase text-zinc-300 transition hover:border-zinc-700 hover:text-white"
        >
          <ArrowLeft size={14} />
          Voltar para ingressos
        </Link>

        {loading ? (
          <div className="rounded-2xl border border-zinc-800 bg-zinc-950 p-10 text-center">
            <Loader2 size={22} className="mx-auto animate-spin text-emerald-300" />
          </div>
        ) : error ? (
          <div className="rounded-2xl border border-red-500/20 bg-red-500/10 p-5 text-sm text-red-200">{error}</div>
        ) : !order ? (
          <div className="rounded-2xl border border-zinc-800 bg-zinc-950 p-5 text-sm text-zinc-400">
            Pedido não encontrado.
          </div>
        ) : (
          <section className="space-y-4 rounded-2xl border border-zinc-800 bg-zinc-950 p-4 sm:p-5">
            <div className="border-b border-zinc-800 pb-4">
              <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Detalhe do pedido</p>
              <h1 className="mt-1 text-xl font-black uppercase text-white">{asString(order.eventoNome) || "Ingresso"}</h1>
              <p className="mt-1 text-xs text-zinc-500">Pedido #{asString(order.id).slice(0, 8).toUpperCase()}</p>
            </div>

            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
              <div className="rounded-xl border border-zinc-800 bg-black/30 p-4">
                <p className="text-[10px] font-bold uppercase text-zinc-500">Cliente</p>
                <p className="mt-1 text-sm font-black text-white">{asString(order.userName, "Aluno")}</p>
                <p className="text-xs text-zinc-500">{asString(order.userTurma, "Sem turma")}</p>
              </div>
              <div className="rounded-xl border border-zinc-800 bg-black/30 p-4">
                <p className="text-[10px] font-bold uppercase text-zinc-500">Status</p>
                <p className="mt-1 text-sm font-black uppercase text-emerald-300">{asString(order.status, "-")}</p>
              </div>
              <div className="rounded-xl border border-zinc-800 bg-black/30 p-4">
                <p className="text-[10px] font-bold uppercase text-zinc-500">Valor</p>
                <p className="mt-1 text-sm font-black text-emerald-400">{formatCurrency(parseCurrency(order.valorTotal))}</p>
              </div>
              <div className="rounded-xl border border-zinc-800 bg-black/30 p-4">
                <p className="text-[10px] font-bold uppercase text-zinc-500">Quantidade</p>
                <p className="mt-1 text-sm font-black text-white">{String(order.quantidade || 1)}</p>
              </div>
            </div>

            <div className="grid gap-3 sm:grid-cols-2">
              <div className="rounded-xl border border-zinc-800 bg-black/30 p-4">
                <p className="text-[10px] font-bold uppercase text-zinc-500">Data e hora do pedido</p>
                <p className="mt-1 text-sm font-bold text-white">{formatDateTime(requestedAt)}</p>
              </div>
              <div className="rounded-xl border border-zinc-800 bg-black/30 p-4">
                <p className="text-[10px] font-bold uppercase text-zinc-500">Aprovação</p>
                <p className="mt-1 text-sm font-bold text-white">{formatDateTime(approvedAt)}</p>
                <p className="text-xs text-zinc-500">{asString(order.aprovadoPor) || "-"}</p>
              </div>
            </div>

            <div className="rounded-xl border border-zinc-800 bg-black/30 p-4">
              <div className="flex items-center gap-2">
                <Wallet size={14} className="text-emerald-400" />
                <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Pagamento via PIX</p>
              </div>
              <div className="mt-3 grid gap-3 sm:grid-cols-3">
                <div>
                  <p className="text-[10px] font-bold uppercase text-zinc-500">Chave</p>
                  <p className="mt-1 break-all text-xs font-mono text-zinc-300">{paymentConfig?.chave || "-"}</p>
                </div>
                <div>
                  <p className="text-[10px] font-bold uppercase text-zinc-500">Banco</p>
                  <p className="mt-1 text-xs font-bold text-zinc-300">{paymentConfig?.banco || "-"}</p>
                </div>
                <div>
                  <p className="text-[10px] font-bold uppercase text-zinc-500">Titular</p>
                  <p className="mt-1 text-xs font-bold text-zinc-300">{paymentConfig?.titular || "-"}</p>
                </div>
              </div>
            </div>

            <div className="space-y-2 rounded-xl border border-zinc-800 bg-black/30 p-4">
              <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Ingressos</p>
              {tickets.length === 0 ? (
                <p className="text-sm text-zinc-500">Nenhum QR Code emitido para este pedido.</p>
              ) : (
                tickets.map((ticket) => (
                  <Link
                    key={ticket.token}
                    href={buildEventTicketPublicPath({
                      orderId: asString(order.id),
                      ticketToken: ticket.token,
                      tenantSlug,
                    })}
                    className="flex flex-col gap-2 rounded-lg border border-zinc-800 bg-zinc-950 px-3 py-3 text-sm text-white transition hover:border-emerald-500/40 sm:flex-row sm:items-center sm:justify-between"
                  >
                    <span>
                      <span className="font-black">{ticket.label}</span>
                      <span className="mt-1 block font-mono text-[11px] text-zinc-500">{ticket.id}</span>
                    </span>
                    <span className="inline-flex items-center gap-2 text-xs font-black uppercase text-emerald-300">
                      <QrCode size={14} />
                      Abrir QR Code
                    </span>
                  </Link>
                ))
              )}
            </div>
          </section>
        )}
      </div>
    </main>
  );
}
