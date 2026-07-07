"use client";

import { useCallback, useEffect, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { ArrowLeft, Loader2, QrCode, ShoppingBag } from "lucide-react";

import { useAuth } from "@/context/AuthContext";
import { useTenantTheme } from "@/context/TenantThemeContext";
import { useToast } from "@/context/ToastContext";
import {
  fetchEventPartyPublicBundle,
  type EventPartyEvent,
  type EventPartyProduct,
} from "@/lib/eventPartyService";
import { withTenantSlug } from "@/lib/tenantRouting";

const formatCurrency = (value: number): string =>
  value.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

export function EventProductsClientPage({ eventId }: { eventId: string }) {
  const { user } = useAuth();
  const { tenantId, tenantSlug } = useTenantTheme();
  const { addToast } = useToast();
  const [event, setEvent] = useState<EventPartyEvent | null>(null);
  const [products, setProducts] = useState<EventPartyProduct[]>([]);
  const [loading, setLoading] = useState(true);

  const tenantPath = useCallback(
    (path: string): string => (tenantSlug.trim() ? withTenantSlug(tenantSlug, path) : path),
    [tenantSlug]
  );
  const fichasHref = tenantPath(`/eventos/${encodeURIComponent(eventId)}/produtos/fichas`);
  const eventosHref = tenantPath(`/eventos/${encodeURIComponent(eventId)}`);
  const registrarCliqueProduto = useCallback(
    (productId: string) => {
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
    },
    [eventId, tenantId]
  );
  const accessDenied =
    event?.visibility === "internal" &&
    (!user?.uid ||
      Boolean(event.tenantId && String(user?.tenant_id || tenantId || "").trim() !== event.tenantId));
  const heroImage = event?.imagem.trim() || "";

  useEffect(() => {
    let mounted = true;
    const load = async () => {
      setLoading(true);
      try {
        const bundle = await fetchEventPartyPublicBundle({
          eventId,
          tenantId: tenantId || undefined,
        });
        if (!mounted) return;
        setEvent(bundle.event);
        setProducts(bundle.products);
      } catch (error: unknown) {
        console.error(error);
        if (mounted) addToast("Erro ao carregar menu do evento.", "error");
      } finally {
        if (mounted) setLoading(false);
      }
    };
    void load();
    return () => {
      mounted = false;
    };
  }, [addToast, eventId, tenantId]);

  return (
    <main className="min-h-screen bg-[#050505] px-4 py-5 pb-28 text-white">
      <div className="mx-auto max-w-5xl space-y-5">
        <header className="flex items-center justify-between gap-3">
          <Link href={eventosHref} className="inline-flex h-11 w-11 items-center justify-center rounded-2xl border border-zinc-800 bg-zinc-950 text-zinc-300">
            <ArrowLeft size={18} />
          </Link>
          <Link href={fichasHref} className="inline-flex items-center gap-2 rounded-full border border-brand bg-brand-soft px-4 py-2 text-xs font-black uppercase text-brand-accent">
            <QrCode size={14} />
            Minhas fichas
          </Link>
        </header>

        <section className="overflow-hidden rounded-[2rem] border border-zinc-800 bg-zinc-950">
          <div className="relative h-44 bg-black">
            {heroImage ? (
              <Image src={heroImage} alt={event?.titulo || "Evento"} fill sizes="100vw" className="object-cover opacity-40" />
            ) : (
              <div className="absolute inset-0 bg-zinc-950" />
            )}
            <div className="absolute inset-0 bg-gradient-to-t from-zinc-950 via-black/55 to-black/20" />
            <div className="absolute inset-x-0 bottom-0 p-5">
              <p className="text-[10px] font-black uppercase tracking-[0.28em] text-brand-accent">{event?.titulo || "Evento"}</p>
              <h1 className="mt-2 text-3xl font-black uppercase text-white">{event?.config.menuTitle || "Menu do evento"}</h1>
            </div>
          </div>
        </section>

        {loading ? (
          <div className="rounded-3xl border border-zinc-800 bg-zinc-950 p-10 text-center text-brand">
            <Loader2 size={22} className="mx-auto animate-spin" />
          </div>
        ) : accessDenied ? (
          <div className="rounded-3xl border border-dashed border-zinc-800 bg-zinc-950 p-8 text-center text-zinc-500">
            Este menu é interno para membros da atlética.
          </div>
        ) : !event?.config.enabled ? (
          <div className="rounded-3xl border border-dashed border-zinc-800 bg-zinc-950 p-8 text-center text-zinc-500">
            O menu deste evento ainda não está ativo.
          </div>
        ) : products.length === 0 ? (
          <div className="rounded-3xl border border-dashed border-zinc-800 bg-zinc-950 p-8 text-center text-zinc-500">
            Nenhum produto disponível neste evento.
          </div>
        ) : (
          <section className="grid gap-4 sm:grid-cols-2">
            {products.map((product) => {
              const productHref = tenantPath(
                `/eventos/${encodeURIComponent(eventId)}/produtos/${encodeURIComponent(product.id)}`
              );
              return (
                <article key={product.id} className="overflow-hidden rounded-[1.6rem] border border-zinc-800 bg-zinc-950">
                  <Link href={productHref} onClick={() => registrarCliqueProduto(product.id)} className="block">
                    <div className="relative h-40 bg-black">
                      {product.img ? (
                        <Image src={product.img} alt={product.nome} fill sizes="(max-width: 768px) 100vw, 50vw" className="object-cover transition duration-500 hover:scale-105" />
                      ) : (
                        <div className="flex h-full items-center justify-center text-zinc-700">
                          <ShoppingBag size={32} />
                        </div>
                      )}
                    </div>
                  </Link>
                  <div className="space-y-3 p-4">
                    <Link href={productHref} onClick={() => registrarCliqueProduto(product.id)} className="block">
                      <p className="text-[10px] font-black uppercase tracking-[0.2em] text-zinc-500">{product.categoria || event?.config.categoryName || "Menu do evento"}</p>
                      <h2 className="mt-1 text-lg font-black text-white">{product.nome}</h2>
                      <p className="mt-1 line-clamp-2 text-sm text-zinc-500">{product.descricao || "Produto disponível no evento."}</p>
                    </Link>
                    <div className="flex items-center justify-between gap-3">
                      <p className="text-xl font-black text-brand-accent">{formatCurrency(product.preco)}</p>
                      <span className="rounded-xl border border-zinc-700 bg-black px-3 py-2 text-[10px] font-black uppercase text-zinc-300">
                        {product.estoque > 0 ? `estoque ${product.estoque}` : "sob demanda"}
                      </span>
                    </div>
                    <Link
                      href={productHref}
                      onClick={() => registrarCliqueProduto(product.id)}
                      className="flex w-full items-center justify-center gap-2 rounded-2xl bg-emerald-500 px-4 py-3 text-xs font-black uppercase text-black shadow-lg shadow-emerald-500/15 transition hover:bg-emerald-400"
                    >
                      <ShoppingBag size={14} />
                      Comprar
                    </Link>
                  </div>
                </article>
              );
            })}
          </section>
        )}
      </div>
    </main>
  );
}
