"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { ArrowLeft, Copy, MessageCircle, Package, QrCode, Wallet, X } from "lucide-react";

import { ReceiptContactButton } from "@/components/ReceiptContactButton";
import { useAuth } from "../../../../context/AuthContext";
import { fetchUserOrdersByTab } from "../../../../lib/settingsService";
import { fetchFinanceiroConfig } from "../../../../lib/eventsService";
import { useToast } from "../../../../context/ToastContext";
import { useTenantTheme } from "@/context/TenantThemeContext";
import { getSupabaseClient } from "@/lib/supabase";
import {
  buildEventReceiptWhatsappMessage,
  buildProductReceiptWhatsappMessage,
  buildTenantFinanceFallback,
  resolveReceiptContactProfile,
  resolveTenantBrandLabel,
} from "../../../../lib/tenantBranding";
import type { CommercePaymentConfig } from "@/lib/commerceCatalog";
import { buildEventTicketPublicPath } from "@/lib/eventTickets";
import { withTenantSlug } from "@/lib/tenantRouting";

const PEDIDOS_PAGE_SIZE = 10;

interface PedidoUnificado {
  id: string;
  titulo: string;
  subtitulo: string;
  valor: number;
  status: "aprovado" | "rejeitado" | "pendente";
  data: Date;
  raw?: PedidoRaw;
}

type PedidoRaw = {
  id: string;
  dataSolicitacao?: TimestampLike | string;
  createdAt?: TimestampLike | string;
  dataAprovacao?: TimestampLike | string;
  aprovadoPor?: string;
  data?: string | Record<string, unknown>;
  eventoNome?: string;
  eventoId?: string;
  quantidade?: number;
  loteNome?: string;
  valorTotal?: unknown;
  itens?: unknown[] | number;
  total?: unknown;
  productName?: string;
  productId?: string;
  seller_name?: string;
  seller_logo_url?: string;
  price?: unknown;
  userName?: string;
  userTurma?: string;
  payment_config?: CommercePaymentConfig | null;
  planoNome?: string;
  valor?: unknown;
  status?: PedidoUnificado["status"];
};

type TimestampLike = { toDate: () => Date };

const parseDateLike = (value: unknown): Date | null => {
  if (!value) return null;
  if (value instanceof Date && !Number.isNaN(value.getTime())) return value;
  if (typeof value === "string" || typeof value === "number") {
    const parsed = new Date(value);
    return Number.isNaN(parsed.getTime()) ? null : parsed;
  }
  if (typeof value === "object" && value !== null) {
    const toDate = (value as { toDate?: unknown }).toDate;
    if (typeof toDate === "function") {
      const parsed = toDate.call(value);
      if (parsed instanceof Date && !Number.isNaN(parsed.getTime())) return parsed;
    }
  }
  return null;
};

const parseCurrencyValue = (value: unknown): number => {
  if (typeof value === "number") return Number.isFinite(value) ? value : 0;
  if (typeof value !== "string") return 0;

  const sanitized = value.trim().replace(/[^\d,.-]/g, "");
  if (!sanitized) return 0;

  const lastComma = sanitized.lastIndexOf(",");
  const lastDot = sanitized.lastIndexOf(".");

  let normalized = sanitized;
  if (lastComma >= 0 && lastDot >= 0) {
    normalized = lastComma > lastDot ? sanitized.replace(/\./g, "").replace(",", ".") : sanitized.replace(/,/g, "");
  } else if (lastComma >= 0) {
    normalized = sanitized.replace(",", ".");
  }

  const parsed = Number.parseFloat(normalized);
  return Number.isFinite(parsed) ? parsed : 0;
};

const normalizePedido = (item: PedidoRaw, tab: "eventos" | "loja" | "planos"): PedidoUnificado => {
  let titulo = "Item";
  let subtitulo = "";
  let valor = 0;
  let data = new Date();

  data =
    parseDateLike(item.dataSolicitacao) ||
    parseDateLike(item.createdAt) ||
    (typeof item.data === "string" ? parseDateLike(item.data) : null) ||
    data;

  if (tab === "eventos") {
    titulo = item.eventoNome || "Ingresso";
    subtitulo = `${item.quantidade || 1}x ${item.loteNome || "Lote unico"}`;
    valor = parseCurrencyValue(item.valorTotal);
  } else if (tab === "loja") {
    const qtd =
      typeof item.quantidade === "number"
        ? item.quantidade
        : typeof item.itens === "number"
        ? item.itens
        : Array.isArray(item.itens)
        ? item.itens.length
        : 1;
    titulo = item.productName || `Pedido #${item.id.slice(0, 6).toUpperCase()}`;
    subtitulo = `${qtd || 1} item(ns)`;
    valor = parseCurrencyValue(item.total ?? item.price);
  } else {
    titulo = item.planoNome || "Adesao";
    subtitulo = "Anuidade";
    valor = parseCurrencyValue(item.valor);
  }

  const statusRaw = String(item.status || "pendente").toLowerCase();
  const normalizedStatus: PedidoUnificado["status"] =
    statusRaw === "approved" || statusRaw === "aprovado"
      ? "aprovado"
      : statusRaw === "rejected" || statusRaw === "rejeitado"
      ? "rejeitado"
      : "pendente";

  return {
    id: item.id,
    titulo,
    subtitulo,
    valor,
    status: normalizedStatus,
    data,
    raw: item,
  };
};

interface PedidosByTypePageProps {
  tab: "eventos" | "loja" | "planos";
  detailPedidoId?: string;
  detailTicketToken?: string;
  initialStatusSlug?: string;
}

export function PedidosByTypePage({
  tab,
  detailPedidoId = "",
  detailTicketToken = "",
  initialStatusSlug = "",
}: PedidosByTypePageProps) {
  const searchParams = useSearchParams();
  const { user } = useAuth();
  const { addToast } = useToast();
  const { tenantId, tenantSigla, tenantName, tenantSlug } = useTenantTheme();
  const [loading, setLoading] = useState(true);
  const [pedidos, setPedidos] = useState<PedidoUnificado[]>([]);
  const [selectedPedido, setSelectedPedido] = useState<PedidoUnificado | null>(null);
  const [ensuringTickets, setEnsuringTickets] = useState(false);
  const [financeiro, setFinanceiro] = useState<{
    chave: string;
    banco: string;
    titular: string;
    whatsapp: string;
  } | null>(null);
  const [loadingFinanceiro, setLoadingFinanceiro] = useState(false);
  const financeFallback = useMemo(
    () =>
      buildTenantFinanceFallback({
        tenantSigla,
        tenantName,
      }),
    [tenantName, tenantSigla]
  );
  const brandLabel = useMemo(
    () => resolveTenantBrandLabel(tenantSigla, tenantName),
    [tenantName, tenantSigla]
  );
  const requestedOrderId = useMemo(
    () => detailPedidoId.trim() || String(searchParams.get("pedido") || "").trim(),
    [detailPedidoId, searchParams]
  );
  const statusFilter = useMemo(() => {
    const raw = String(searchParams.get("status") || initialStatusSlug || "pendentes").trim().toLowerCase();
    if (raw === "aprovados") return "aprovado" as const;
    if (raw === "negados" || raw === "rejeitados") return "rejeitado" as const;
    return "pendente" as const;
  }, [initialStatusSlug, searchParams]);
  const currentPage = Math.max(1, Number.parseInt(String(searchParams.get("pagina") || "1"), 10) || 1);
  const tenantPath = (path: string) => (tenantSlug ? withTenantSlug(tenantSlug, path) : path);

  useEffect(() => {
    if (!user) {
      setPedidos([]);
      setLoading(false);
      return;
    }

    let mounted = true;
    setLoading(true);

    const load = async () => {
      try {
        const records = await fetchUserOrdersByTab(user.uid, tab, {
          maxResults: 90,
          tenantId: tenantId || undefined,
        });
        const rawList = records.map((row) => ({ id: row.id, ...(row.data as Record<string, unknown>) })) as PedidoRaw[];

        const sorted = rawList
          .map((row) => normalizePedido(row, tab))
          .sort((a, b) => b.data.getTime() - a.data.getTime());

        if (mounted) setPedidos(sorted);
      } finally {
        if (mounted) setLoading(false);
      }
    };

    void load();
    return () => {
      mounted = false;
    };
  }, [tenantId, user, tab]);

  useEffect(() => {
    if (!selectedPedido) return;
    let mounted = true;
    setLoadingFinanceiro(true);
    const loadFinanceiro = async () => {
      try {
        const row = await fetchFinanceiroConfig({
          forceRefresh: false,
          tenantId: tenantId || undefined,
        });
        if (!mounted) return;
        setFinanceiro({
          chave: typeof row?.chave === "string" && row.chave.trim() ? row.chave.trim() : financeFallback.chave,
          banco: typeof row?.banco === "string" && row.banco.trim() ? row.banco.trim() : financeFallback.banco,
          titular: typeof row?.titular === "string" && row.titular.trim() ? row.titular.trim() : financeFallback.titular,
          whatsapp: typeof row?.whatsapp === "string" && row.whatsapp.trim() ? row.whatsapp.trim() : financeFallback.whatsapp,
        });
      } catch (error: unknown) {
        console.error(error);
      } finally {
        if (mounted) setLoadingFinanceiro(false);
      }
    };
    void loadFinanceiro();
    return () => {
      mounted = false;
    };
  }, [financeFallback, selectedPedido, tenantId]);

  useEffect(() => {
    if (!requestedOrderId) return;
    const targetPedido = pedidos.find((row) => row.id === requestedOrderId);
    if (targetPedido) {
      setSelectedPedido((previous) => (previous?.id === targetPedido.id ? previous : targetPedido));
    }
  }, [pedidos, requestedOrderId]);

  const counts = useMemo(() => {
    return {
      approved: pedidos.filter((row) => row.status === "aprovado").length,
      pending: pedidos.filter((row) => row.status === "pendente").length,
      rejected: pedidos.filter((row) => row.status === "rejeitado").length,
    };
  }, [pedidos]);
  const filteredOrders = useMemo(
    () => pedidos.filter((row) => row.status === statusFilter),
    [pedidos, statusFilter]
  );
  const totalPages = Math.max(1, Math.ceil(filteredOrders.length / PEDIDOS_PAGE_SIZE));
  const paginatedOrders = useMemo(() => {
    const start = (Math.min(currentPage, totalPages) - 1) * PEDIDOS_PAGE_SIZE;
    return filteredOrders.slice(start, start + PEDIDOS_PAGE_SIZE);
  }, [currentPage, filteredOrders, totalPages]);
  const resolvePedidoPaymentConfig = (pedido?: PedidoUnificado | null): CommercePaymentConfig => {
    const rawConfig =
      pedido?.raw?.payment_config && typeof pedido.raw.payment_config === "object"
        ? pedido.raw.payment_config
        : null;
    return {
      chave:
        typeof rawConfig?.chave === "string" && rawConfig.chave.trim()
          ? rawConfig.chave.trim()
          : financeiro?.chave || financeFallback.chave,
      banco:
        typeof rawConfig?.banco === "string" && rawConfig.banco.trim()
          ? rawConfig.banco.trim()
          : financeiro?.banco || financeFallback.banco,
      titular:
        typeof rawConfig?.titular === "string" && rawConfig.titular.trim()
          ? rawConfig.titular.trim()
          : financeiro?.titular || financeFallback.titular,
      whatsapp:
        typeof rawConfig?.whatsapp === "string" && rawConfig.whatsapp.trim()
          ? rawConfig.whatsapp.trim()
          : financeiro?.whatsapp || financeFallback.whatsapp,
      ...(rawConfig?.recipient ? { recipient: rawConfig.recipient } : {}),
      ...(Array.isArray(rawConfig?.ticketEntries) ? { ticketEntries: rawConfig.ticketEntries } : {}),
    };
  };
  const selectedPaymentConfig = resolvePedidoPaymentConfig(selectedPedido);
  const selectedRecipient = resolveReceiptContactProfile({
    paymentConfig: selectedPaymentConfig,
    tenantSigla,
    tenantName,
    fallbackAvatarUrl:
      (tab === "loja" ? String(selectedPedido?.raw?.seller_logo_url || "").trim() : "") || "/logo.png",
    fallbackPhone: selectedPaymentConfig.whatsapp || financeFallback.whatsapp,
  });
  const selectedOrganizerLabel =
    tab === "loja"
      ? String(selectedPedido?.raw?.seller_name || "").trim() || brandLabel
      : brandLabel;
  const whatsappDigits = String(selectedRecipient.phone || "").replace(/\D/g, "");
  const whatsappMessage = selectedPedido
    ? tab === "eventos"
      ? buildEventReceiptWhatsappMessage({
          tenantSigla,
          tenantName,
          eventTitle: selectedPedido.titulo,
          buyerName: user?.nome || selectedPedido.raw?.userName || "Aluno",
          buyerTurma: user?.turma || selectedPedido.raw?.userTurma || "Sem turma",
          buyerPhone: user?.telefone || "Não informado",
          ticketLabel: selectedPedido.subtitulo,
          totalValue: selectedPedido.valor.toFixed(2),
          orderCode: selectedPedido.id.slice(0, 8).toUpperCase(),
          recipientName: selectedRecipient.name,
          recipientTurma: selectedRecipient.turma,
        })
      : tab === "loja"
      ? buildProductReceiptWhatsappMessage({
          organizerLabel: selectedOrganizerLabel,
          productName: selectedPedido.titulo,
          buyerName: user?.nome || selectedPedido.raw?.userName || "Cliente",
          buyerTurma: user?.turma || selectedPedido.raw?.userTurma || "Sem turma",
          buyerPhone: user?.telefone || "Não informado",
          quantity:
            typeof selectedPedido.raw?.quantidade === "number"
              ? selectedPedido.raw.quantidade
              : typeof selectedPedido.raw?.itens === "number"
              ? selectedPedido.raw.itens
              : Array.isArray(selectedPedido.raw?.itens)
              ? selectedPedido.raw.itens.length
              : 1,
          totalValue: selectedPedido.valor.toFixed(2),
          orderCode: selectedPedido.id.slice(0, 8).toUpperCase(),
          recipientName: selectedRecipient.name,
          recipientTurma: selectedRecipient.turma,
        })
      : `Fala, equipe *${brandLabel}*! Segue o comprovante do pedido *${selectedPedido.id.slice(0, 8).toUpperCase()}*.`
    : "";
  const whatsappUrl = whatsappDigits
    ? `https://wa.me/${whatsappDigits}?text=${encodeURIComponent(whatsappMessage)}`
    : "";
  const selectedTicketLinks = useMemo(() => {
    if (tab !== "eventos") return [];
    const statusSlug =
      selectedPedido?.status === "aprovado"
        ? "aprovados"
        : selectedPedido?.status === "rejeitado"
          ? "negados"
          : "pendentes";

    return (selectedPaymentConfig.ticketEntries || []).map((ticket) => {
      const detailPath = `/configuracoes/pedidos/eventos/${statusSlug}/${selectedPedido?.id || ""}/ingressos/${encodeURIComponent(ticket.token)}`;
      return {
          id: ticket.id,
          label: ticket.label,
          status: ticket.status,
          href: tenantSlug ? withTenantSlug(tenantSlug, detailPath) : detailPath,
          qrHref: buildEventTicketPublicPath({
            orderId: selectedPedido?.id || "",
            ticketToken: ticket.token,
            tenantSlug,
          }),
          token: ticket.token,
          transferredToUserName: ticket.transferredToUserName || "",
          transferredFromUserName: ticket.transferredFromUserName || "",
        };
      });
  }, [selectedPaymentConfig.ticketEntries, selectedPedido?.id, selectedPedido?.status, tab, tenantSlug]);
  const selectedTicketDetail = useMemo(
    () => selectedTicketLinks.find((ticket) => ticket.token === detailTicketToken.trim()) || null,
    [detailTicketToken, selectedTicketLinks]
  );

  useEffect(() => {
    if (tab !== "eventos" || !selectedPedido || selectedPedido.status !== "aprovado") return;
    if ((selectedPaymentConfig.ticketEntries || []).length > 0 || ensuringTickets) return;

    let mounted = true;
    setEnsuringTickets(true);
    const run = async () => {
      try {
        const session = await getSupabaseClient().auth.getSession();
        const accessToken = session.data.session?.access_token || "";
        if (!accessToken) return;

        const response = await fetch("/api/event-tickets/ensure", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${accessToken}`,
          },
          body: JSON.stringify({ orderId: selectedPedido.id }),
        });

        const payload = (await response.json().catch(() => null)) as
          | { paymentConfig?: CommercePaymentConfig | null; error?: string }
          | null;
        if (!response.ok || !payload?.paymentConfig || !mounted) return;

        setPedidos((previous) =>
          previous.map((pedido) =>
            pedido.id === selectedPedido.id
              ? {
                  ...pedido,
                  raw: {
                    ...(pedido.raw || { id: pedido.id }),
                    payment_config: payload.paymentConfig,
                  },
                }
              : pedido
          )
        );
        setSelectedPedido((previous) =>
          previous && previous.id === selectedPedido.id
            ? {
                ...previous,
                raw: {
                  ...(previous.raw || { id: previous.id }),
                  payment_config: payload.paymentConfig,
                },
              }
            : previous
        );
      } catch (error: unknown) {
        console.error(error);
      } finally {
        if (mounted) setEnsuringTickets(false);
      }
    };

    void run();
    return () => {
      mounted = false;
    };
  }, [ensuringTickets, selectedPedido, selectedPaymentConfig.ticketEntries, tab]);

  if (detailTicketToken) {
    const backHref = selectedPedido
      ? tenantPath(
          `/configuracoes/pedidos/eventos/${
            selectedPedido.status === "aprovado" ? "aprovados" : selectedPedido.status === "rejeitado" ? "negados" : "pendentes"
          }/${selectedPedido.id}`
        )
      : tenantPath(`/configuracoes/pedidos/eventos?status=${statusFilter === "aprovado" ? "aprovados" : statusFilter === "rejeitado" ? "negados" : "pendentes"}`);

    return (
      <main className="space-y-4 p-4">
        <Link
          href={backHref}
          className="inline-flex items-center gap-2 rounded-xl border border-zinc-800 bg-zinc-950 px-3 py-2 text-xs font-black uppercase text-zinc-300 transition hover:border-zinc-700 hover:text-white"
        >
          <ArrowLeft size={14} />
          Voltar ao pedido
        </Link>

        {loading || (ensuringTickets && !selectedTicketDetail) ? (
          <div className="rounded-2xl border border-zinc-800 bg-zinc-900/70 p-6 text-xs font-bold uppercase text-zinc-500">
            Carregando ingresso...
          </div>
        ) : !selectedPedido || !selectedTicketDetail ? (
          <div className="rounded-2xl border border-zinc-800 bg-zinc-900/70 p-6 text-sm text-zinc-400">
            Ingresso não encontrado para este pedido.
          </div>
        ) : (
          <section className="space-y-4 rounded-2xl border border-zinc-800 bg-[#0b0b0c] p-4">
            <div className="border-b border-zinc-800 pb-4">
              <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Detalhe do ingresso</p>
              <h2 className="mt-1 text-lg font-black uppercase text-white">{selectedTicketDetail.label}</h2>
              <p className="mt-1 text-xs text-zinc-500">{selectedPedido.titulo}</p>
            </div>

            <div className="grid gap-3 sm:grid-cols-2">
              <div className="rounded-xl border border-zinc-800 bg-zinc-900/70 p-4">
                <p className="text-[10px] font-bold uppercase text-zinc-500">Pedido</p>
                <p className="mt-1 font-mono text-sm font-black text-white">#{selectedPedido.id.slice(0, 8).toUpperCase()}</p>
              </div>
              <div className="rounded-xl border border-zinc-800 bg-zinc-900/70 p-4">
                <p className="text-[10px] font-bold uppercase text-zinc-500">Status</p>
                <p className="mt-1 text-sm font-black uppercase text-emerald-300">{selectedTicketDetail.status}</p>
              </div>
              <div className="rounded-xl border border-zinc-800 bg-zinc-900/70 p-4">
                <p className="text-[10px] font-bold uppercase text-zinc-500">Valor</p>
                <p className="mt-1 text-sm font-black text-emerald-400">R$ {selectedPedido.valor.toFixed(2)}</p>
              </div>
              <div className="rounded-xl border border-zinc-800 bg-zinc-900/70 p-4">
                <p className="text-[10px] font-bold uppercase text-zinc-500">Data e hora do pedido</p>
                <p className="mt-1 text-sm font-bold text-white">
                  {selectedPedido.data.toLocaleDateString("pt-BR")} {selectedPedido.data.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" })}
                </p>
              </div>
            </div>

            <div className="rounded-xl border border-zinc-800 bg-zinc-900/70 p-4">
              <p className="text-[10px] font-bold uppercase text-zinc-500">Ingresso</p>
              <p className="mt-2 text-sm font-bold text-white">{selectedPedido.subtitulo}</p>
              {selectedTicketDetail.transferredToUserName ? (
                <p className="mt-2 text-xs font-semibold text-amber-200">Transferido para {selectedTicketDetail.transferredToUserName}.</p>
              ) : null}
              {selectedTicketDetail.transferredFromUserName ? (
                <p className="mt-2 text-xs font-semibold text-emerald-200">Recebido de {selectedTicketDetail.transferredFromUserName}.</p>
              ) : null}
            </div>

            <div className="rounded-xl border border-zinc-800 bg-zinc-900/70 p-4">
              <div className="flex items-center gap-2">
                <Wallet size={14} className="text-emerald-400" />
                <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Pagamento via PIX</p>
              </div>
              <div className="mt-3 grid gap-3 sm:grid-cols-2">
                <div>
                  <p className="text-[10px] font-bold uppercase text-zinc-500">Banco</p>
                  <p className="mt-1 text-xs font-bold text-zinc-300">{selectedPaymentConfig.banco}</p>
                </div>
                <div>
                  <p className="text-[10px] font-bold uppercase text-zinc-500">Titular</p>
                  <p className="mt-1 text-xs font-bold text-zinc-300">{selectedPaymentConfig.titular}</p>
                </div>
              </div>
            </div>

            <Link
              href={selectedTicketDetail.qrHref}
              className="inline-flex w-full items-center justify-center gap-2 rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-xs font-black uppercase text-emerald-300"
            >
              <QrCode size={16} />
              Abrir QR Code
            </Link>
          </section>
        )}
      </main>
    );
  }

  const isPedidoDetailMode = Boolean(detailPedidoId.trim());

  const renderPedidoCard = (pedido: PedidoUnificado) => {
    const cardContent = (
      <>
        <div className="flex items-start justify-between gap-3">
          <div>
            <h3 className="text-sm font-bold text-white">{pedido.titulo}</h3>
            <p className="text-xs text-zinc-400">{pedido.subtitulo}</p>
          </div>
          <span
            className={`rounded border px-2 py-1 text-[10px] font-black uppercase ${
              pedido.status === "aprovado"
                ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-400"
                : pedido.status === "rejeitado"
                ? "border-red-500/30 bg-red-500/10 text-red-400"
                : "border-yellow-500/30 bg-yellow-500/10 text-yellow-300"
            }`}
          >
            {pedido.status}
          </span>
        </div>

        <div className="mt-3 flex items-center justify-between border-t border-zinc-800 pt-3">
          <p className="text-xs text-zinc-500">
            {pedido.data.toLocaleDateString("pt-BR")}{" "}
            {pedido.data.toLocaleTimeString("pt-BR", {
              hour: "2-digit",
              minute: "2-digit",
            })}
          </p>
          <p className="text-sm font-black text-emerald-400">
            R$ {pedido.valor.toFixed(2)}
          </p>
        </div>
      </>
    );
    const cardClassName = `block cursor-pointer rounded-xl border p-4 transition hover:border-zinc-600 ${
      pedido.status === "pendente"
        ? "border-yellow-500/20 bg-yellow-500/5"
        : pedido.status === "aprovado"
        ? "border-emerald-500/20 bg-emerald-500/5"
        : "border-red-500/20 bg-red-500/5"
    }`;

    if (tab === "eventos") {
      const statusSlug = pedido.status === "aprovado" ? "aprovados" : pedido.status === "rejeitado" ? "negados" : "pendentes";
      return (
        <Link key={pedido.id} href={tenantPath(`/configuracoes/pedidos/eventos/${statusSlug}/${pedido.id}`)} className={cardClassName}>
          {cardContent}
        </Link>
      );
    }

    return (
      <article
      key={pedido.id}
      className={cardClassName}
      onClick={() => {
        setSelectedPedido(pedido);
      }}
    >
      {cardContent}
    </article>
    );
  };

  return (
    <main className="p-4 space-y-4">
      {!isPedidoDetailMode ? (
        <>
          <section className="grid grid-cols-3 gap-2">
            <Link href={tenantPath(`/configuracoes/pedidos/${tab}?status=aprovados`)} className={`bg-zinc-900 border rounded-lg p-3 text-center transition ${statusFilter === "aprovado" ? "border-emerald-500/60" : "border-zinc-800"}`}>
              <p className="text-[10px] text-zinc-500 uppercase font-bold">Aprovado</p>
              <p className="text-lg font-black text-emerald-400">{counts.approved}</p>
            </Link>
            <Link href={tenantPath(`/configuracoes/pedidos/${tab}?status=pendentes`)} className={`bg-zinc-900 border rounded-lg p-3 text-center transition ${statusFilter === "pendente" ? "border-yellow-500/60" : "border-zinc-800"}`}>
              <p className="text-[10px] text-zinc-500 uppercase font-bold">Pendente</p>
              <p className="text-lg font-black text-yellow-400">{counts.pending}</p>
            </Link>
            <Link href={tenantPath(`/configuracoes/pedidos/${tab}?status=negados`)} className={`bg-zinc-900 border rounded-lg p-3 text-center transition ${statusFilter === "rejeitado" ? "border-red-500/60" : "border-zinc-800"}`}>
              <p className="text-[10px] text-zinc-500 uppercase font-bold">Negado</p>
              <p className="text-lg font-black text-red-400">{counts.rejected}</p>
            </Link>
          </section>

          {loading ? (
        <div className="text-xs text-zinc-500 uppercase font-bold">Carregando...</div>
      ) : filteredOrders.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 text-zinc-600 gap-3">
          <Package size={42} className="opacity-20" />
          <p className="text-sm">Nenhum pedido encontrado.</p>
        </div>
      ) : (
        <section className="space-y-3">
          <p className="text-[11px] font-black uppercase text-zinc-400">
            {statusFilter === "aprovado" ? "Aprovados" : statusFilter === "rejeitado" ? "Negados" : "Pendentes"}
          </p>
          {paginatedOrders.map(renderPedidoCard)}
          {totalPages > 1 ? (
            <div className="flex items-center justify-between gap-3 pt-2">
              <Link
                href={tenantPath(`/configuracoes/pedidos/${tab}?status=${statusFilter === "aprovado" ? "aprovados" : statusFilter === "rejeitado" ? "negados" : "pendentes"}&pagina=${Math.max(1, currentPage - 1)}`)}
                className={`rounded-xl border border-zinc-800 px-4 py-2 text-xs font-black uppercase ${currentPage <= 1 ? "pointer-events-none opacity-40" : "text-zinc-200"}`}
              >
                Anterior
              </Link>
              <span className="text-xs font-bold text-zinc-500">
                Página {Math.min(currentPage, totalPages)} de {totalPages}
              </span>
              <Link
                href={tenantPath(`/configuracoes/pedidos/${tab}?status=${statusFilter === "aprovado" ? "aprovados" : statusFilter === "rejeitado" ? "negados" : "pendentes"}&pagina=${Math.min(totalPages, currentPage + 1)}`)}
                className={`rounded-xl border border-zinc-800 px-4 py-2 text-xs font-black uppercase ${currentPage >= totalPages ? "pointer-events-none opacity-40" : "text-zinc-200"}`}
              >
                Próxima
              </Link>
            </div>
          ) : null}
        </section>
      )}
        </>
      ) : loading ? (
        <div className="rounded-2xl border border-zinc-800 bg-zinc-900/70 p-6 text-xs font-bold uppercase text-zinc-500">
          Carregando pedido...
        </div>
      ) : !selectedPedido ? (
        <div className="rounded-2xl border border-zinc-800 bg-zinc-900/70 p-6 text-sm text-zinc-400">
          Pedido não encontrado.
        </div>
      ) : null}

      {selectedPedido && (
        <div
          className={
            detailPedidoId
              ? "rounded-2xl border border-zinc-800 bg-[#0b0b0c]"
              : "fixed inset-0 z-[80] bg-black/80 backdrop-blur-sm flex items-end sm:items-center justify-center p-4"
          }
        >
          <div className={detailPedidoId ? "w-full rounded-2xl bg-[#0b0b0c]" : "w-full max-w-md rounded-2xl border border-zinc-800 bg-[#0b0b0c] shadow-2xl"}>
            <div className="p-4 border-b border-zinc-800 flex items-center justify-between">
              <div>
                <p className="text-[10px] uppercase font-black tracking-widest text-zinc-500">Detalhe do Pedido</p>
                <h3 className="text-sm font-black uppercase text-white">{selectedPedido.titulo}</h3>
              </div>
              {detailPedidoId ? null : (
                <button
                  onClick={() => setSelectedPedido(null)}
                  className="p-2 rounded-lg border border-zinc-700 bg-zinc-900 hover:bg-zinc-800"
                >
                  <X size={14} />
                </button>
              )}
            </div>

            <div className="p-4 space-y-4">
              <div className="rounded-xl border border-zinc-800 bg-zinc-900/70 p-4 space-y-2">
                <div className="flex items-center justify-between text-xs">
                  <span className="text-zinc-500 font-bold uppercase">Pedido</span>
                  <span className="text-zinc-300 font-mono">#{selectedPedido.id.slice(0, 8).toUpperCase()}</span>
                </div>
                <div className="flex items-center justify-between text-xs">
                  <span className="text-zinc-500 font-bold uppercase">Status</span>
                  <span className="text-zinc-300 font-bold uppercase">{selectedPedido.status}</span>
                </div>
                <div className="flex items-center justify-between text-xs">
                  <span className="text-zinc-500 font-bold uppercase">Valor</span>
                  <span className="text-emerald-400 font-black">R$ {selectedPedido.valor.toFixed(2)}</span>
                </div>
                <div className="text-[11px] text-zinc-500">
                  {selectedPedido.data.toLocaleDateString("pt-BR")} {selectedPedido.data.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" })}
                </div>
                {typeof selectedPedido.raw?.data === "object" && selectedPedido.raw?.data && "corSelecionada" in selectedPedido.raw.data && (
                  <div className="text-xs text-zinc-300">
                    <span className="text-zinc-500 font-bold uppercase">Cor:</span>{" "}
                    {String((selectedPedido.raw.data as Record<string, unknown>).corSelecionada || "-")}
                  </div>
                )}
              </div>

                <div className="rounded-xl border border-zinc-800 bg-zinc-900/70 p-4 space-y-3">
                <div className="flex items-center gap-2">
                  <Wallet size={14} className="text-emerald-400" />
                  <p className="text-[10px] uppercase font-black tracking-widest text-zinc-500">Pagamento via PIX</p>
                </div>
                <div>
                  <p className="text-[10px] text-zinc-500 font-bold uppercase">Chave PIX</p>
                  <div className="mt-1 flex items-center gap-2">
                    <p className="flex-1 rounded-lg border border-zinc-700 bg-black px-3 py-2 text-xs font-mono text-white truncate">
                      {loadingFinanceiro ? "Carregando..." : selectedPaymentConfig.chave}
                    </p>
                    <button
                      onClick={async () => {
                        try {
                          await navigator.clipboard.writeText(selectedPaymentConfig.chave);
                          addToast("Chave PIX copiada!", "success");
                        } catch (error: unknown) {
                          console.error(error);
                          addToast("Não foi possível copiar a chave PIX.", "error");
                        }
                      }}
                      className="p-2 rounded-lg border border-zinc-700 bg-zinc-800 hover:bg-zinc-700"
                    >
                      <Copy size={14} />
                    </button>
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-3 text-xs">
                  <div>
                    <p className="text-zinc-500 font-bold uppercase text-[10px]">Banco</p>
                    <p className="text-zinc-300 font-bold mt-1">{selectedPaymentConfig.banco}</p>
                  </div>
                  <div>
                    <p className="text-zinc-500 font-bold uppercase text-[10px]">Titular</p>
                    <p className="text-zinc-300 font-bold mt-1 truncate">{selectedPaymentConfig.titular}</p>
                  </div>
                </div>
                <div className="rounded-lg border border-zinc-800 bg-black/50 p-3 space-y-1">
                  <p className="text-[10px] text-zinc-500 uppercase font-bold">WhatsApp p/ comprovante</p>
                  <p className="text-zinc-300 text-xs font-mono">{selectedRecipient.phone || financeFallback.whatsapp}</p>
                </div>
                <div className="rounded-lg border border-zinc-800 bg-black/50 p-3 flex items-start gap-2">
                  <MessageCircle size={14} className="text-emerald-400 mt-0.5" />
                  <p className="text-[11px] text-zinc-400">
                    Envie o comprovante informando o número do pedido <span className="font-mono text-zinc-200">#{selectedPedido.id.slice(0, 8).toUpperCase()}</span>.
                  </p>
                </div>
                {whatsappUrl && (
                  <ReceiptContactButton
                    recipient={selectedRecipient}
                    onClick={() => window.open(whatsappUrl, "_blank")}
                  />
                )}
                {ensuringTickets && selectedTicketLinks.length === 0 ? (
                  <div className="rounded-lg border border-zinc-800 bg-black/40 px-3 py-3 text-xs text-zinc-500">
                    Preparando ingressos digitais...
                  </div>
                ) : null}
                {selectedTicketLinks.length > 0 ? (
                  <div className="space-y-2 rounded-lg border border-zinc-800 bg-black/40 p-3">
                    <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Ingressos</p>
                    {selectedTicketLinks.map((ticket) => (
                      <Link
                        key={ticket.id}
                        href={ticket.href}
                        className="flex items-center justify-between rounded-lg border border-zinc-700 bg-zinc-900 px-3 py-2 text-xs font-bold text-white hover:bg-zinc-800"
                      >
                        <span>{ticket.label}</span>
                        <span
                          className={`rounded border px-2 py-1 text-[10px] font-black uppercase ${
                            ticket.status === "lido" || ticket.status === "transferido"
                              ? "border-red-500/30 bg-red-500/10 text-red-300"
                              : "border-emerald-500/30 bg-emerald-500/10 text-emerald-300"
                          }`}
                        >
                          {ticket.status === "lido" ? "Lido" : ticket.status === "transferido" ? "Transferido" : "Ativo"}
                        </span>
                      </Link>
                    ))}
                  </div>
                ) : null}
              </div>
            </div>
          </div>
        </div>
      )}
    </main>
  );
}


