"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, Copy, Loader2, QrCode, ShoppingBag, Wallet, X } from "lucide-react";

import { useAuth } from "@/context/AuthContext";
import { useTenantTheme } from "@/context/TenantThemeContext";
import { useToast } from "@/context/ToastContext";
import { buildLoginPath } from "@/lib/authRedirect";
import {
  createEventPartyOrder,
  fetchEventPartyOrders,
  fetchEventPartyPublicBundle,
  type EventPartyEvent,
  type EventPartyOrder,
  type EventPartyProduct,
} from "@/lib/eventPartyService";
import { fetchFinanceiroConfig } from "@/lib/eventsService";
import { withTenantSlug } from "@/lib/tenantRouting";
import { buildTenantFinanceFallback, resolveTenantBrandLabel } from "@/lib/tenantBranding";

type PixData = {
  chave: string;
  banco: string;
  titular: string;
  whatsapp: string;
};

const formatCurrency = (value: number): string =>
  value.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

const asObject = (value: unknown): Record<string, unknown> | null =>
  value && typeof value === "object" && !Array.isArray(value) ? (value as Record<string, unknown>) : null;

const asString = (value: unknown, fallback = ""): string =>
  typeof value === "string" ? value : fallback;

const readRecipientPhone = (value: unknown): string => {
  const row = asObject(value);
  if (!row) return "";
  const recipient = asObject(row.recipient);
  const recipients = Array.isArray(row.recipients)
    ? row.recipients
    : Array.isArray(row.paymentRecipients)
      ? row.paymentRecipients
      : Array.isArray(row.receivers)
        ? row.receivers
        : [];
  const firstRecipientWithPhone =
    recipients.map(asObject).find((entry) => Boolean(asString(entry?.phone || entry?.whatsapp).trim())) ?? null;
  return (
    asString(recipient?.phone || recipient?.whatsapp).trim() ||
    asString(firstRecipientWithPhone?.phone || firstRecipientWithPhone?.whatsapp).trim()
  );
};

const readPixData = (value: unknown, fallback: PixData): PixData => {
  const source = asObject(value) ?? {};
  const row = { ...(asObject(source.data) ?? {}), ...source };
  return {
    chave: asString(row.chave).trim() || fallback.chave,
    banco: asString(row.banco).trim() || fallback.banco,
    titular: asString(row.titular).trim() || fallback.titular,
    whatsapp: asString(row.whatsapp).trim() || readRecipientPhone(row) || fallback.whatsapp,
  };
};

const formatOrderDateTime = (value: unknown): string => {
  if (!value) return "--";
  const date =
    value instanceof Date
      ? value
      : typeof value === "string" || typeof value === "number"
        ? new Date(value)
        : typeof value === "object" && "toDate" in value && typeof (value as { toDate?: unknown }).toDate === "function"
          ? ((value as { toDate: () => Date }).toDate())
          : null;
  if (!date || Number.isNaN(date.getTime())) return "--";
  return `${date.toLocaleDateString("pt-BR")} ${date.toLocaleTimeString("pt-BR", {
    hour: "2-digit",
    minute: "2-digit",
  })}`;
};

export function EventProductDetailClientPage({
  eventId,
  productId,
}: {
  eventId: string;
  productId: string;
}) {
  const router = useRouter();
  const { user } = useAuth();
  const { tenantId, tenantSlug, tenantSigla, tenantName } = useTenantTheme();
  const { addToast } = useToast();
  const [event, setEvent] = useState<EventPartyEvent | null>(null);
  const [product, setProduct] = useState<EventPartyProduct | null>(null);
  const [orders, setOrders] = useState<EventPartyOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [checkoutOpen, setCheckoutOpen] = useState(false);
  const [checkoutStep, setCheckoutStep] = useState<1 | 2 | 3>(1);
  const [quantity, setQuantity] = useState(1);
  const [creatingOrder, setCreatingOrder] = useState(false);
  const [checkoutOrderId, setCheckoutOrderId] = useState("");
  const [pixData, setPixData] = useState<PixData>({
    chave: "Carregando...",
    banco: "...",
    titular: "...",
    whatsapp: "",
  });
  const [loadingPix, setLoadingPix] = useState(false);

  const tenantPath = useCallback(
    (path: string): string => (tenantSlug.trim() ? withTenantSlug(tenantSlug, path) : path),
    [tenantSlug]
  );
  const productsHref = tenantPath(`/eventos/${encodeURIComponent(eventId)}/produtos`);
  const fichasHref = tenantPath(`/eventos/${encodeURIComponent(eventId)}/produtos/fichas`);
  const loginPath = useMemo(
    () =>
      buildLoginPath(
        tenantPath(`/eventos/${encodeURIComponent(eventId)}/produtos/${encodeURIComponent(productId)}`)
      ),
    [eventId, productId, tenantPath]
  );
  const brandLabel = useMemo(
    () => resolveTenantBrandLabel(tenantSigla, tenantName),
    [tenantName, tenantSigla]
  );
  const financeFallback = useMemo(
    () => buildTenantFinanceFallback({ tenantSigla, tenantName }),
    [tenantName, tenantSigla]
  );
  const checkoutTotal = Number(((product?.preco || 0) * quantity).toFixed(2));
  const hasStockLimit = Boolean(product && product.estoque > 0);
  const maxQuantity = Math.max(1, Math.min(10, hasStockLimit ? product?.estoque || 1 : 10));
  const unavailable = !product || product.status !== "ativo";
  const accessDenied =
    event?.visibility === "internal" &&
    (!user?.uid ||
      Boolean(event.tenantId && String(user?.tenant_id || tenantId || "").trim() !== event.tenantId));
  const registrarCliqueProduto = useCallback(() => {
    if (!productId) return;
    void fetch("/api/event-products/click", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        eventId,
        productId,
        tenantId: tenantId || undefined,
      }),
      keepalive: true,
    }).catch(() => undefined);
  }, [eventId, productId, tenantId]);

  const load = useCallback(
    async (forceOrders = false) => {
      setLoading((current) => current || !forceOrders);
      try {
        const bundle = await fetchEventPartyPublicBundle({
          eventId,
          tenantId: tenantId || undefined,
        });
        const selectedProduct =
          bundle.products.find((entry) => entry.id === productId) || null;
        const nextOrders =
          user?.uid && selectedProduct
            ? await fetchEventPartyOrders({
                eventId,
                tenantId: tenantId || undefined,
                userId: user.uid,
                productIds: [selectedProduct.id],
                pageSize: 50,
              })
            : [];
        setEvent(bundle.event);
        setProduct(selectedProduct);
        setOrders(nextOrders);
      } catch (error: unknown) {
        console.error(error);
        addToast("Erro ao carregar produto do evento.", "error");
      } finally {
        setLoading(false);
      }
    },
    [addToast, eventId, productId, tenantId, user?.uid]
  );

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    setQuantity((current) => Math.min(current, maxQuantity));
  }, [maxQuantity]);

  const loadPixData = useCallback(async () => {
    if (loadingPix) return;
    setLoadingPix(true);
    try {
      const financeiro = await fetchFinanceiroConfig({
        forceRefresh: false,
        tenantId: tenantId || undefined,
      });
      const base = readPixData(financeiro, financeFallback);
      const eventPayment = readPixData(event?.paymentConfig, base);
      setPixData(readPixData(product?.paymentConfig, eventPayment));
    } catch (error: unknown) {
      console.error(error);
      setPixData(financeFallback);
    } finally {
      setLoadingPix(false);
    }
  }, [event?.paymentConfig, financeFallback, loadingPix, product?.paymentConfig, tenantId]);

  const pendingOrders = orders.filter((order) => order.status.trim().toLowerCase() === "pendente");
  const historyOrders = orders.filter((order) => order.status.trim().toLowerCase() !== "pendente");

  const handleBuy = () => {
    registrarCliqueProduto();
    if (!user?.uid) {
      router.push(loginPath);
      return;
    }
    if (unavailable) {
      addToast("Produto indisponível no momento.", "info");
      return;
    }
    setCheckoutStep(1);
    setQuantity(1);
    setCheckoutOrderId("");
    setCheckoutOpen(true);
    void loadPixData();
  };

  const handleCreateOrder = async () => {
    if (!event || !product || !user?.uid || creatingOrder) return;
    setCreatingOrder(true);
    try {
      const order = await createEventPartyOrder({
        event,
        product,
        userId: user.uid,
        userName: user.nome || "Aluno",
        quantity,
        tenantId: tenantId || undefined,
      });
      setCheckoutOrderId(order.id);
      setCheckoutStep(2);
      addToast("Pedido gerado. Envie o comprovante para liberação.", "success");
      await load(true);
    } catch (error: unknown) {
      console.error(error);
      addToast(error instanceof Error ? error.message : "Erro ao criar pedido.", "error");
    } finally {
      setCreatingOrder(false);
    }
  };

  const copyPix = async () => {
    try {
      await navigator.clipboard.writeText(pixData.chave);
      addToast("Chave PIX copiada.", "success");
    } catch {
      addToast("Não foi possível copiar a chave PIX.", "error");
    }
  };

  const sendReceipt = () => {
    const phone = pixData.whatsapp.replace(/\D/g, "");
    if (!phone || !product || !checkoutOrderId) {
      addToast("WhatsApp financeiro não configurado.", "error");
      return;
    }
    const buyerName = user?.nome?.trim() || "Cliente";
    const buyerTurma = user?.turma?.trim() || "Sem turma";
    const buyerPhone = user?.telefone?.trim() || "Não informado";
    const message = [
      `Olá, ${brandLabel}! Segue comprovante do pedido de ficha.`,
      `Pedido: #${checkoutOrderId.slice(0, 8).toUpperCase()}`,
      `Evento: ${event?.titulo || "Evento"}`,
      `Produto: ${product.nome}`,
      `Quantidade: ${quantity}`,
      `Valor: ${formatCurrency(checkoutTotal)}`,
      `Comprador: ${buyerName}`,
      `Turma: ${buyerTurma}`,
      `Telefone: ${buyerPhone}`,
    ].join("\n");
    window.open(`https://wa.me/${phone}?text=${encodeURIComponent(message)}`, "_blank");
    setCheckoutStep(3);
  };

  if (loading) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-[#050505] text-emerald-400">
        <Loader2 className="animate-spin" />
      </main>
    );
  }

  if (!event || !product || accessDenied || !event.config.enabled) {
    return (
      <main className="min-h-screen bg-[#050505] p-6 text-white">
        <div className="mx-auto max-w-xl rounded-3xl border border-zinc-800 bg-zinc-950 p-6 text-center">
          <p className="text-sm text-zinc-400">Produto não encontrado ou indisponível.</p>
          <Link href={productsHref} className="mt-4 inline-flex rounded-xl border border-zinc-700 px-4 py-3 text-xs font-black uppercase text-zinc-200">
            Voltar ao menu
          </Link>
        </div>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-[#050505] pb-28 text-white">
      <div className="relative h-[42vh] min-h-[280px] bg-black">
        {product.img ? (
          <Image src={product.img} alt={product.nome} fill priority sizes="100vw" className="object-cover" />
        ) : (
          <div className="flex h-full items-center justify-center text-zinc-700">
            <ShoppingBag size={42} />
          </div>
        )}
        <div className="absolute inset-0 bg-gradient-to-t from-[#050505] via-black/20 to-black/40" />
        <button
          type="button"
          onClick={() => router.back()}
          className="absolute left-5 top-5 z-10 rounded-full border border-white/10 bg-black/50 p-3 text-white backdrop-blur"
        >
          <ArrowLeft size={20} />
        </button>
        <Link
          href={fichasHref}
          className="absolute right-5 top-5 z-10 inline-flex items-center gap-2 rounded-full border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-xs font-black uppercase text-emerald-200 backdrop-blur"
        >
          <QrCode size={14} />
          Minhas fichas
        </Link>
      </div>

      <div className="relative z-10 mx-auto -mt-10 max-w-5xl space-y-5 px-4">
        <section className="rounded-[2rem] border border-zinc-800 bg-zinc-950 p-5">
          <p className="text-[10px] font-black uppercase tracking-[0.24em] text-emerald-400">{product.categoria || event.config.categoryName}</p>
          <div className="mt-3 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
            <div>
              <h1 className="text-3xl font-black uppercase text-white">{product.nome}</h1>
              <p className="mt-2 max-w-2xl text-sm leading-relaxed text-zinc-400 whitespace-pre-wrap">
                {product.descricao || "Produto disponível no evento."}
              </p>
            </div>
            <div className="shrink-0 text-left sm:text-right">
              <p className="text-3xl font-black text-emerald-400">{formatCurrency(product.preco)}</p>
              <span className="mt-2 inline-flex rounded-full border border-emerald-500/20 bg-emerald-500/10 px-3 py-1 text-[10px] font-black uppercase text-emerald-300">
                {product.status === "ativo" ? "Disponível" : "Indisponível"}
              </span>
            </div>
          </div>

          <div className="mt-5 rounded-2xl border border-zinc-800 bg-black/30 p-4">
            <p className="text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">Estoque disponível</p>
            <p className="mt-2 text-sm font-bold text-zinc-200">
              {hasStockLimit ? `${product.estoque} unidade(s)` : "Venda sob demanda"}
            </p>
          </div>

          <button
            type="button"
            onClick={handleBuy}
            disabled={unavailable}
            className="mt-5 inline-flex w-full items-center justify-center gap-2 rounded-2xl bg-emerald-500 px-5 py-4 text-sm font-black uppercase text-black shadow-lg shadow-emerald-500/15 transition hover:bg-emerald-400 disabled:opacity-50"
          >
            <ShoppingBag size={18} />
            {unavailable ? "Produto indisponível" : "Comprar agora"}
          </button>
        </section>

        <section className="rounded-[2rem] border border-zinc-800 bg-zinc-950 p-5">
          <div className="flex items-center justify-between gap-3">
            <h2 className="text-lg font-black uppercase">Seus pedidos</h2>
            <Link href={fichasHref} className="rounded-xl border border-zinc-700 px-3 py-2 text-[10px] font-black uppercase text-zinc-200">
              Minhas fichas
            </Link>
          </div>

          <div className="mt-4 space-y-3">
            {[...pendingOrders, ...historyOrders].length === 0 ? (
              <div className="rounded-2xl border border-dashed border-zinc-800 p-5 text-sm text-zinc-500">
                Você ainda não fez pedidos deste produto.
              </div>
            ) : (
              [...pendingOrders, ...historyOrders].map((order) => (
                <article key={order.id} className="rounded-2xl border border-zinc-800 bg-black/20 p-4">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="font-black text-white">Pedido #{order.id.slice(0, 8).toUpperCase()}</p>
                      <p className="mt-1 text-xs text-zinc-500">{formatOrderDateTime(order.createdAt)}</p>
                      <p className="mt-2 text-xs text-zinc-300">
                        Qtd: {order.quantidade} • {formatCurrency(order.total || order.price)}
                      </p>
                    </div>
                    <span className={`rounded-full border px-2.5 py-1 text-[10px] font-black uppercase ${
                      order.status === "pendente"
                        ? "border-yellow-500/30 bg-yellow-500/10 text-yellow-300"
                        : "border-emerald-500/30 bg-emerald-500/10 text-emerald-300"
                    }`}>
                      {order.status}
                    </span>
                  </div>
                </article>
              ))
            )}
          </div>
        </section>
      </div>

      {checkoutOpen ? (
        <div className="fixed inset-0 z-[80] flex items-end justify-center bg-black/80 p-4 backdrop-blur-sm sm:items-center">
          <div className="w-full max-w-md rounded-2xl border border-zinc-800 bg-[#0b0b0c] shadow-2xl">
            <div className="flex items-center justify-between border-b border-zinc-800 p-4">
              <div>
                <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Checkout do evento</p>
                <h3 className="text-sm font-black uppercase text-white">
                  {checkoutStep === 1 ? "Confirmar pedido" : checkoutStep === 2 ? "Pagamento via PIX" : "Pedido registrado"}
                </h3>
              </div>
              <button
                type="button"
                onClick={() => {
                  if (!creatingOrder) setCheckoutOpen(false);
                }}
                className="rounded-lg border border-zinc-700 bg-zinc-900 p-2 hover:bg-zinc-800"
              >
                <X size={14} />
              </button>
            </div>

            <div className="px-4 pt-4">
              <div className="h-1 w-full overflow-hidden rounded-full bg-zinc-800">
                <div
                  className="h-full bg-emerald-500 transition-all"
                  style={{ width: checkoutStep === 1 ? "33%" : checkoutStep === 2 ? "66%" : "100%" }}
                />
              </div>
            </div>

            <div className="space-y-4 p-4">
              {checkoutStep === 1 ? (
                <>
                  <div className="space-y-3 rounded-xl border border-zinc-800 bg-zinc-900/70 p-4">
                    <div className="flex justify-between gap-3 text-sm">
                      <span className="font-bold uppercase text-zinc-400">Produto</span>
                      <span className="font-bold text-white">{product.nome}</span>
                    </div>
                    <div className="flex items-center justify-between gap-3">
                      <span className="text-sm font-bold uppercase text-zinc-400">Quantidade</span>
                      <div className="flex items-center gap-2">
                        <button type="button" onClick={() => setQuantity((current) => Math.max(1, current - 1))} className="h-8 w-8 rounded-lg border border-zinc-700 bg-black font-black">-</button>
                        <span className="w-8 text-center text-sm font-black">{quantity}</span>
                        <button type="button" onClick={() => setQuantity((current) => Math.min(maxQuantity, current + 1))} className="h-8 w-8 rounded-lg border border-zinc-700 bg-black font-black">+</button>
                      </div>
                    </div>
                    <div className="flex items-center justify-between border-t border-zinc-800 pt-3">
                      <span className="text-xs font-black uppercase text-zinc-300">Valor</span>
                      <span className="text-xl font-black text-emerald-400">{formatCurrency(checkoutTotal)}</span>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => void handleCreateOrder()}
                    disabled={creatingOrder}
                    className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-emerald-600 py-3 text-xs font-black uppercase text-white transition hover:bg-emerald-500 disabled:opacity-60"
                  >
                    {creatingOrder ? <Loader2 size={14} className="animate-spin" /> : <ShoppingBag size={14} />}
                    Confirmar pedido
                  </button>
                </>
              ) : null}

              {checkoutStep === 2 ? (
                <>
                  <div className="space-y-3 rounded-xl border border-zinc-800 bg-zinc-900/70 p-4">
                    <div className="flex items-center gap-2">
                      <Wallet size={14} className="text-emerald-400" />
                      <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Pagamento via PIX</p>
                    </div>
                    <div>
                      <p className="text-[10px] font-bold uppercase text-zinc-500">Chave PIX</p>
                      <div className="mt-1 flex items-center gap-2">
                        <p className="min-w-0 flex-1 truncate rounded-lg border border-zinc-700 bg-black px-3 py-2 font-mono text-xs text-white">
                          {loadingPix ? "Carregando..." : pixData.chave}
                        </p>
                        <button type="button" onClick={() => void copyPix()} className="rounded-lg border border-zinc-700 bg-zinc-800 p-2">
                          <Copy size={14} />
                        </button>
                      </div>
                    </div>
                    <div className="grid grid-cols-2 gap-3 text-xs">
                      <div>
                        <p className="text-[10px] font-bold uppercase text-zinc-500">Banco</p>
                        <p className="mt-1 font-bold text-zinc-300">{pixData.banco}</p>
                      </div>
                      <div>
                        <p className="text-[10px] font-bold uppercase text-zinc-500">Titular</p>
                        <p className="mt-1 truncate font-bold text-zinc-300">{pixData.titular}</p>
                      </div>
                    </div>
                    <div className="rounded-lg border border-zinc-800 bg-black/50 p-3 text-center">
                      <p className="text-[10px] font-bold uppercase text-zinc-500">Valor exato</p>
                      <p className="text-lg font-black text-emerald-400">{formatCurrency(checkoutTotal)}</p>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={sendReceipt}
                    className="inline-flex w-full items-center justify-center gap-2 rounded-xl border border-emerald-500/30 bg-emerald-500/10 py-3 text-xs font-black uppercase text-emerald-300"
                  >
                    Enviar comprovante
                  </button>
                </>
              ) : null}

              {checkoutStep === 3 ? (
                <div className="space-y-4 text-center">
                  <div className="rounded-xl border border-emerald-500/20 bg-emerald-500/5 p-4">
                    <p className="text-sm font-black uppercase text-white">Pedido em análise</p>
                    <p className="mt-2 text-xs text-zinc-400">
                      Pedido #{checkoutOrderId.slice(0, 8).toUpperCase()} gerado. Acompanhe a liberação nas suas fichas.
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => setCheckoutOpen(false)}
                    className="w-full rounded-xl border border-zinc-700 bg-zinc-900 py-3 text-xs font-black uppercase"
                  >
                    Fechar
                  </button>
                </div>
              ) : null}
            </div>
          </div>
        </div>
      ) : null}
    </main>
  );
}
