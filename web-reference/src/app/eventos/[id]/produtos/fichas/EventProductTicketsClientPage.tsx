"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { QRCodeSVG } from "qrcode.react";
import { ArrowLeft, CheckCircle2, Loader2, QrCode, Send } from "lucide-react";

import { useAuth } from "@/context/AuthContext";
import { useTenantTheme } from "@/context/TenantThemeContext";
import { useToast } from "@/context/ToastContext";
import {
  eventPartyVoucherStatusLabel,
  fetchEventPartyOrders,
  fetchEventPartyPublicBundle,
  getEventPartyOrderReference,
  getEventPartyVoucherEntries,
  getEventPartyVoucherSummary,
  getEventPartyVoucherStatus,
  type EventPartyEvent,
  type EventPartyOrder,
  type EventPartyProduct,
  type EventPartyVoucherEntryStatus,
  type EventPartyVoucherStatus,
} from "@/lib/eventPartyService";
import { buildEventProductVoucherQrPayload } from "@/lib/qrPayloads";
import { getSupabaseClient } from "@/lib/supabase";
import { buildLoginPath } from "@/lib/authRedirect";
import { withTenantSlug } from "@/lib/tenantRouting";

type PendingTransfer = {
  orderId: string;
  productName: string;
  voucherId: string;
  voucherLabel: string;
  fromUserName: string;
  requestedAt: string;
};

const formatCurrency = (value: number): string =>
  value.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

const getStatusTone = (status: EventPartyVoucherStatus | EventPartyVoucherEntryStatus): string => {
  if (status === "ativo") return "bg-emerald-500/10 text-emerald-300";
  if (status === "parcial") return "bg-sky-500/10 text-sky-300";
  if (status === "utilizado" || status === "inativo") return "bg-red-500/10 text-red-300";
  if (status === "transferido") return "bg-violet-500/10 text-violet-300";
  if (status === "cancelado" || status === "estornado" || status === "reembolsado") {
    return "bg-zinc-700 text-zinc-300";
  }
  return "bg-yellow-500/10 text-yellow-300";
};

const getEntryCardTone = (status: EventPartyVoucherEntryStatus): string => {
  if (status === "ativo") return "bg-emerald-100 text-emerald-700";
  if (status === "utilizado" || status === "inativo") return "bg-red-100 text-red-700";
  if (status === "transferido") return "bg-violet-100 text-violet-700";
  if (status === "cancelado" || status === "estornado" || status === "reembolsado") {
    return "bg-zinc-200 text-zinc-700";
  }
  return "bg-yellow-100 text-yellow-700";
};

export function EventProductTicketsClientPage({ eventId }: { eventId: string }) {
  const { user } = useAuth();
  const { tenantId, tenantSlug } = useTenantTheme();
  const { addToast } = useToast();
  const [event, setEvent] = useState<EventPartyEvent | null>(null);
  const [products, setProducts] = useState<EventPartyProduct[]>([]);
  const [orders, setOrders] = useState<EventPartyOrder[]>([]);
  const [pendingTransfers, setPendingTransfers] = useState<PendingTransfer[]>([]);
  const [transferTargets, setTransferTargets] = useState<Record<string, string>>({});
  const [transferLoadingKey, setTransferLoadingKey] = useState("");
  const [loading, setLoading] = useState(true);

  const tenantPath = useCallback(
    (path: string): string => (tenantSlug.trim() ? withTenantSlug(tenantSlug, path) : path),
    [tenantSlug]
  );
  const cardapioHref = tenantPath(`/eventos/${encodeURIComponent(eventId)}/produtos`);
  const loginPath = useMemo(
    () => buildLoginPath(tenantPath(`/eventos/${encodeURIComponent(eventId)}/produtos/fichas`)),
    [eventId, tenantPath]
  );
  const accessDenied =
    event?.visibility === "internal" &&
    (!user?.uid ||
      Boolean(event.tenantId && String(user?.tenant_id || tenantId || "").trim() !== event.tenantId));

  const fetchAuthToken = useCallback(async (): Promise<string> => {
    const { data } = await getSupabaseClient().auth.getSession();
    return data.session?.access_token || "";
  }, []);

  const loadTransfers = useCallback(async (): Promise<PendingTransfer[]> => {
    if (!user?.uid) return [];
    const token = await fetchAuthToken();
    if (!token) return [];
    const response = await fetch(`/api/event-products/transfer?eventId=${encodeURIComponent(eventId)}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (!response.ok) return [];
    const payload = (await response.json().catch(() => ({}))) as { transfers?: PendingTransfer[] };
    return Array.isArray(payload.transfers) ? payload.transfers : [];
  }, [eventId, fetchAuthToken, user?.uid]);

  const refreshOrdersAndTransfers = useCallback(async () => {
    if (!user?.uid) {
      setOrders([]);
      setPendingTransfers([]);
      return;
    }
    const nextOrders = await fetchEventPartyOrders({
      eventId,
      tenantId: tenantId || undefined,
      userId: user.uid,
      productIds: products.map((product) => product.id),
    });
    setOrders(nextOrders);
    setPendingTransfers(await loadTransfers());
  }, [eventId, loadTransfers, products, tenantId, user?.uid]);

  useEffect(() => {
    let mounted = true;
    const load = async () => {
      setLoading(true);
      try {
        const bundle = await fetchEventPartyPublicBundle({
          eventId,
          tenantId: tenantId || undefined,
        });
        const nextOrders = user?.uid
          ? await fetchEventPartyOrders({
              eventId,
              tenantId: tenantId || undefined,
              userId: user.uid,
              productIds: bundle.products.map((product) => product.id),
            })
          : [];
        const transfers = user?.uid ? await loadTransfers() : [];
        if (!mounted) return;
        setEvent(bundle.event);
        setProducts(bundle.products);
        setOrders(nextOrders);
        setPendingTransfers(transfers);
      } catch (error: unknown) {
        console.error(error);
        if (mounted) addToast("Erro ao carregar fichas do evento.", "error");
      } finally {
        if (mounted) setLoading(false);
      }
    };
    void load();
    return () => {
      mounted = false;
    };
  }, [addToast, eventId, loadTransfers, tenantId, user?.uid]);

  const productById = useMemo(
    () => new Map(products.map((product) => [product.id, product])),
    [products]
  );

  const submitTransfer = useCallback(
    async (payload: { action: "request" | "accept"; orderId: string; voucherId: string; recipient?: string }) => {
      const key = `${payload.action}:${payload.orderId}:${payload.voucherId}`;
      setTransferLoadingKey(key);
      try {
        const token = await fetchAuthToken();
        if (!token) throw new Error("Entre novamente para transferir a ficha.");
        const response = await fetch("/api/event-products/transfer", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify(payload),
        });
        const result = (await response.json().catch(() => ({}))) as { error?: string; message?: string };
        if (!response.ok) throw new Error(result.error || "Erro ao transferir ficha.");
        addToast(result.message || "Transferência atualizada.", "success");
        setTransferTargets((previous) => ({ ...previous, [`${payload.orderId}:${payload.voucherId}`]: "" }));
        await refreshOrdersAndTransfers();
      } catch (error: unknown) {
        console.error(error);
        addToast(error instanceof Error ? error.message : "Erro ao transferir ficha.", "error");
      } finally {
        setTransferLoadingKey("");
      }
    },
    [addToast, fetchAuthToken, refreshOrdersAndTransfers]
  );

  if (!user?.uid && !loading) {
    return (
      <main className="min-h-screen bg-[#050505] px-4 py-6 text-white">
        <div className="mx-auto max-w-xl rounded-[2rem] border border-zinc-800 bg-zinc-950 p-6 text-center">
          <QrCode size={28} className="mx-auto text-brand" />
          <h1 className="mt-4 text-xl font-black uppercase">Entre para ver suas fichas</h1>
          <Link href={loginPath} className="mt-5 inline-flex rounded-2xl bg-brand px-5 py-3 text-xs font-black uppercase text-black">
            Fazer login
          </Link>
        </div>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-[#050505] px-4 py-5 pb-28 text-white">
      <div className="mx-auto max-w-4xl space-y-5">
        <header className="flex items-center justify-between gap-3">
          <Link href={cardapioHref} className="inline-flex h-11 w-11 items-center justify-center rounded-2xl border border-zinc-800 bg-zinc-950 text-zinc-300">
            <ArrowLeft size={18} />
          </Link>
          <Link href={cardapioHref} className="inline-flex rounded-full border border-zinc-800 bg-zinc-950 px-4 py-2 text-xs font-black uppercase text-zinc-200">
            Menu
          </Link>
        </header>

        <section className="rounded-[2rem] border border-zinc-800 bg-zinc-950 p-5">
          <p className="text-[10px] font-black uppercase tracking-[0.28em] text-brand-accent">{event?.titulo || "Evento"}</p>
          <h1 className="mt-2 text-3xl font-black uppercase">Minhas fichas</h1>
          <p className="mt-2 text-sm text-zinc-500">Use o QR no evento. Se a leitura falhar, informe o código exibido no cartão.</p>
        </section>

        {pendingTransfers.length > 0 ? (
          <section className="rounded-[2rem] border border-emerald-500/25 bg-emerald-500/10 p-5">
            <h2 className="text-sm font-black uppercase text-emerald-200">Transferências para aceitar</h2>
            <div className="mt-4 space-y-3">
              {pendingTransfers.map((transfer) => {
                const key = `accept:${transfer.orderId}:${transfer.voucherId}`;
                return (
                  <div key={`${transfer.orderId}:${transfer.voucherId}`} className="grid gap-3 rounded-2xl border border-emerald-500/20 bg-black/20 p-4 sm:grid-cols-[1fr_auto] sm:items-center">
                    <div>
                      <p className="font-black text-white">{transfer.productName}</p>
                      <p className="mt-1 text-xs text-emerald-100">
                        {transfer.voucherLabel} transferida por {transfer.fromUserName || "usuário"}
                      </p>
                    </div>
                    <button
                      type="button"
                      onClick={() => void submitTransfer({ action: "accept", orderId: transfer.orderId, voucherId: transfer.voucherId })}
                      disabled={transferLoadingKey === key}
                      className="inline-flex items-center justify-center gap-2 rounded-xl bg-emerald-400 px-4 py-3 text-xs font-black uppercase text-black disabled:opacity-60"
                    >
                      {transferLoadingKey === key ? <Loader2 size={14} className="animate-spin" /> : <CheckCircle2 size={14} />}
                      Aceitar
                    </button>
                  </div>
                );
              })}
            </div>
          </section>
        ) : null}

        {loading ? (
          <div className="rounded-3xl border border-zinc-800 bg-zinc-950 p-10 text-center text-brand">
            <Loader2 size={22} className="mx-auto animate-spin" />
          </div>
        ) : accessDenied ? (
          <div className="rounded-3xl border border-dashed border-zinc-800 bg-zinc-950 p-8 text-center text-zinc-500">
            Estas fichas pertencem a um evento interno.
          </div>
        ) : orders.length === 0 ? (
          <div className="rounded-3xl border border-dashed border-zinc-800 bg-zinc-950 p-8 text-center text-zinc-500">
            Nenhuma ficha comprada para este evento.
          </div>
        ) : (
          <section className="grid gap-4 sm:grid-cols-2">
            {orders.map((order) => {
              const voucherStatus = getEventPartyVoucherStatus(order);
              const voucherSummary = getEventPartyVoucherSummary(order);
              const voucherEntries = getEventPartyVoucherEntries(order);
              const reference = getEventPartyOrderReference(order);
              const product = productById.get(order.productId);
              return (
                <article key={order.id} className="rounded-[1.8rem] border border-zinc-800 bg-zinc-950 p-5">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="text-[10px] font-black uppercase tracking-[0.2em] text-zinc-500">
                        {eventPartyVoucherStatusLabel(voucherStatus)}
                      </p>
                      <h2 className="mt-2 text-lg font-black text-white">{order.productName}</h2>
                      <p className="mt-1 text-sm text-zinc-500">
                        {order.quantidade} un. • {formatCurrency(order.total || order.price)}
                      </p>
                      <p className="mt-1 font-mono text-[10px] text-zinc-600">{reference.summary}</p>
                      {voucherStatus !== "pendente" ? (
                        <p className="mt-1 text-xs font-bold text-zinc-600">
                          {voucherSummary.used}/{voucherSummary.total} retirada(s)
                        </p>
                      ) : null}
                    </div>
                    <span className={`rounded-full px-3 py-1 text-[10px] font-black uppercase ${getStatusTone(voucherStatus)}`}>
                      {eventPartyVoucherStatusLabel(voucherStatus)}
                    </span>
                  </div>

                  <div className="mt-5 grid gap-3">
                    {voucherEntries.map((entry) => {
                      const transferKey = `${order.id}:${entry.id}`;
                      const requestKey = `request:${order.id}:${entry.id}`;
                      const qrValue = buildEventProductVoucherQrPayload({
                        orderId: order.id,
                        eventId,
                        productId: order.productId,
                        voucherId: entry.id,
                      });
                      const entryIsActive = voucherStatus !== "pendente" && entry.status === "ativo";
                      const code = entry.code || `${order.id.slice(0, 8).toUpperCase()}-${entry.id}`;
                      const transferNote =
                        entry.transferStatus ||
                        (entry.transferredToUserName ? `Transferido para ${entry.transferredToUserName}` : "") ||
                        (entry.transferredFromUserName ? `Transferido de ${entry.transferredFromUserName}` : "");
                      const inactiveText =
                        transferNote ||
                        eventPartyVoucherStatusLabel(entry.status);
                      return (
                        <div key={transferKey} className="rounded-3xl border border-zinc-800 bg-white p-4 text-center text-black">
                          <div className="mb-3 flex items-center justify-between gap-2">
                            <span className="text-xs font-black uppercase">{entry.label}</span>
                            <span className={`rounded-full px-2 py-1 text-[10px] font-black uppercase ${getEntryCardTone(entry.status)}`}>
                              {entryIsActive ? "Ativo" : inactiveText}
                            </span>
                          </div>
                          {entryIsActive ? (
                            <>
                              <QRCodeSVG value={qrValue} size={210} includeMargin className="mx-auto" />
                              <div className="mt-3 rounded-2xl bg-zinc-100 px-3 py-2 text-left">
                                <p className="text-[10px] font-black uppercase text-zinc-500">Código do QR</p>
                                <p className="mt-1 break-all font-mono text-xs font-black text-zinc-900">{code}</p>
                                {transferNote ? (
                                  <p className="mt-2 text-[11px] font-black uppercase text-violet-700">{transferNote}</p>
                                ) : null}
                              </div>
                              <div className="mt-3 grid gap-2 sm:grid-cols-[1fr_auto]">
                                <input
                                  value={transferTargets[transferKey] || ""}
                                  onChange={(event) =>
                                    setTransferTargets((previous) => ({
                                      ...previous,
                                      [transferKey]: event.target.value.slice(0, 160),
                                    }))
                                  }
                                  className="min-w-0 rounded-xl border border-zinc-300 bg-white px-3 py-2 text-xs font-bold outline-none"
                                  placeholder="E-mail, RA, telefone ou ID"
                                />
                                <button
                                  type="button"
                                  onClick={() =>
                                    void submitTransfer({
                                      action: "request",
                                      orderId: order.id,
                                      voucherId: entry.id,
                                      recipient: transferTargets[transferKey] || "",
                                    })
                                  }
                                  disabled={transferLoadingKey === requestKey || !(transferTargets[transferKey] || "").trim()}
                                  className="inline-flex items-center justify-center gap-2 rounded-xl bg-zinc-950 px-3 py-2 text-[10px] font-black uppercase text-white disabled:opacity-50"
                                >
                                  {transferLoadingKey === requestKey ? <Loader2 size={13} className="animate-spin" /> : <Send size={13} />}
                                  Transferir
                                </button>
                              </div>
                            </>
                          ) : (
                            <div className="flex h-[242px] flex-col items-center justify-center rounded-2xl bg-zinc-100 px-4 text-sm font-black uppercase text-zinc-400">
                              <span>{entry.status === "pendente" ? "Aguardando aprovação" : inactiveText}</span>
                              {code ? <span className="mt-3 break-all font-mono text-[11px] text-zinc-500">{code}</span> : null}
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>

                  <p className="mt-3 text-xs text-zinc-600">
                    {product?.descricao || "Apresente esta ficha no evento quando o pedido for aprovado."}
                  </p>
                </article>
              );
            })}
          </section>
        )}
      </div>
    </main>
  );
}
