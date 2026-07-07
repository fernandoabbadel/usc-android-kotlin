"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { Html5Qrcode } from "html5-qrcode";
import { ArrowLeft, CheckCircle2, ClipboardList, Loader2, QrCode, RotateCcw, ScanLine, Search } from "lucide-react";

import { useAuth } from "@/context/AuthContext";
import { useTenantTheme } from "@/context/TenantThemeContext";
import { useToast } from "@/context/ToastContext";
import {
  fetchEventPartyAdminBundle,
  fetchEventPartyOrders,
  getEventPartyOrderReference,
  getEventPartyVoucherEntries,
  type EventPartyEvent,
  type EventPartyOrder,
  type EventPartyProduct,
  type EventPartyVoucherEntry,
} from "@/lib/eventPartyService";
import { parseEventProductVoucherQrPayload } from "@/lib/qrPayloads";
import { getSupabaseClient } from "@/lib/supabase";
import { withTenantSlug } from "@/lib/tenantRouting";

type WithdrawalPageMode = "hub" | "pendentes" | "retirados";

const WITHDRAWAL_PAGE_SIZE = 20;
const MAX_WITHDRAWAL_ORDER_PAGES = 10;

type WithdrawalItem = {
  key: string;
  order: EventPartyOrder;
  entry: EventPartyVoucherEntry;
  productName: string;
  status: "pendente" | "retirado";
};

const isApprovedOrder = (order: EventPartyOrder): boolean =>
  ["approved", "aprovado", "paid", "pago", "delivered"].includes(order.status.trim().toLowerCase());

const normalizeSearch = (value: string): string =>
  value
    .trim()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase();

const asRecord = (value: unknown): Record<string, unknown> =>
  value && typeof value === "object" && !Array.isArray(value) ? (value as Record<string, unknown>) : {};

const formatDateTime = (value: unknown): string => {
  if (!value) return "-";
  const date =
    typeof value === "string" || typeof value === "number"
      ? new Date(value)
      : value instanceof Date
        ? value
        : null;
  if (!date || Number.isNaN(date.getTime())) return "-";
  return date.toLocaleString("pt-BR");
};

const formatDateOnly = (value: unknown): string => {
  if (!value) return "-";
  const date =
    typeof value === "string" || typeof value === "number"
      ? new Date(value)
      : value instanceof Date
        ? value
        : null;
  return date && !Number.isNaN(date.getTime()) ? date.toLocaleDateString("pt-BR") : "-";
};

const formatTimeOnly = (value: unknown): string => {
  if (!value) return "-";
  const date =
    typeof value === "string" || typeof value === "number"
      ? new Date(value)
      : value instanceof Date
        ? value
        : null;
  return date && !Number.isNaN(date.getTime())
    ? date.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" })
    : "-";
};

const methodLabel = (value: string): string => {
  const normalized = normalizeSearch(value);
  if (normalized.includes("manual")) return "Manual";
  if (normalized.includes("qr") || normalized.includes("scan")) return "Scan";
  return value || "-";
};

const isSameOperator = (currentUser: { uid?: string; nome?: string } | null, item: WithdrawalItem): boolean => {
  const currentUserId = currentUser?.uid?.trim() || "";
  const currentUserName = normalizeSearch(currentUser?.nome || "");
  const usedByUserId = item.entry.usedByUserId?.trim() || "";
  const usedByUserName = normalizeSearch(item.entry.usedByUserName || "");
  if (currentUserId && usedByUserId) return currentUserId === usedByUserId;
  return Boolean(currentUserName && usedByUserName && currentUserName === usedByUserName);
};

export function EventProductWithdrawalClientPage({
  eventId,
  mode = "hub",
  basePath,
}: {
  eventId: string;
  mode?: WithdrawalPageMode;
  basePath?: string;
}) {
  const { user } = useAuth();
  const { tenantId, tenantSlug } = useTenantTheme();
  const { addToast } = useToast();
  const scannerRef = useRef<Html5Qrcode | null>(null);
  const lastPayloadRef = useRef("");
  const [event, setEvent] = useState<EventPartyEvent | null>(null);
  const [products, setProducts] = useState<EventPartyProduct[]>([]);
  const [orders, setOrders] = useState<EventPartyOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [processingKey, setProcessingKey] = useState("");
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(1);
  const [scannerActive, setScannerActive] = useState(false);
  const [scannerStarting, setScannerStarting] = useState(false);
  const [scanMessage, setScanMessage] = useState("Abra a câmera para ler uma ficha.");

  const scopedPath = useCallback(
    (path: string) => (tenantSlug ? withTenantSlug(tenantSlug, path) : path),
    [tenantSlug]
  );
  const normalizedBasePath = useMemo(
    () => basePath?.trim().replace(/\/+$/, "") || `/admin/eventos/${encodeURIComponent(eventId)}`,
    [basePath, eventId]
  );
  const fichaHref = scopedPath(`${normalizedBasePath}/ficha`);
  const extratoHref = scopedPath(`${normalizedBasePath}/extrato`);
  const retiradaHref = scopedPath(`${normalizedBasePath}/ficha/retirada`);
  const pendentesHref = scopedPath(`${normalizedBasePath}/ficha/retirada/pendentes`);
  const retiradosHref = scopedPath(`${normalizedBasePath}/ficha/retirada/retirados`);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const bundle = await fetchEventPartyAdminBundle({
        eventId,
        tenantId: tenantId || undefined,
      });
      const productIds = bundle.products.map((product) => product.id);
      const nextOrders: EventPartyOrder[] = [];
      for (let pageIndex = 0; pageIndex < MAX_WITHDRAWAL_ORDER_PAGES; pageIndex += 1) {
        const orderPage = await fetchEventPartyOrders({
          eventId,
          tenantId: tenantId || undefined,
          productIds,
          pageSize: 500,
          cursorId: String(pageIndex * 500),
        });
        nextOrders.push(...orderPage);
        if (orderPage.length < 500) break;
      }
      setEvent(bundle.event);
      setProducts(bundle.products);
      setOrders(nextOrders);
    } catch (error: unknown) {
      console.error(error);
      addToast("Erro ao carregar retiradas.", "error");
    } finally {
      setLoading(false);
    }
  }, [addToast, eventId, tenantId]);

  useEffect(() => {
    void load();
  }, [load]);

  const stopScanner = useCallback(async () => {
    const scanner = scannerRef.current;
    if (!scanner) return;
    try {
      if (scanner.isScanning) await scanner.stop();
      await scanner.clear();
    } catch {
      // O navegador pode desmontar a câmera antes do stop.
    } finally {
      scannerRef.current = null;
      setScannerActive(false);
    }
  }, []);

  useEffect(() => {
    return () => {
      void stopScanner();
    };
  }, [stopScanner]);

  const postWithdrawal = useCallback(
    async (payload: {
      qrPayload?: string;
      orderId?: string;
      productId?: string;
      voucherId?: string;
      manual?: boolean;
    }) => {
      const session = await getSupabaseClient().auth.getSession();
      const token = session.data.session?.access_token || "";
      const response = await fetch("/api/admin/event-products/scan", {
        method: "POST",
        headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
        body: JSON.stringify({
          ...payload,
          eventId,
        }),
      });
      const data = await response.json().catch(() => null);
      if (!response.ok) throw new Error(data?.error || "Falha ao registrar retirada.");
      return data as {
        alreadyScanned?: boolean;
        holderName?: string;
        productName?: string;
        voucherLabel?: string;
      };
    },
    [eventId]
  );

  const patchWithdrawalCorrection = useCallback(
    async (item: WithdrawalItem, note: string) => {
      const session = await getSupabaseClient().auth.getSession();
      const token = session.data.session?.access_token || "";
      const response = await fetch("/api/admin/event-products/scan", {
        method: "PATCH",
        headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
        body: JSON.stringify({
          eventId,
          orderId: item.order.id,
          productId: item.order.productId,
          voucherId: item.entry.id,
          note,
        }),
      });
      const data = await response.json().catch(() => null);
      if (!response.ok) throw new Error(data?.error || "Falha ao corrigir retirada.");
      return data as { corrected?: boolean };
    },
    [eventId]
  );

  const handleManualWithdrawal = async (item: WithdrawalItem) => {
    setProcessingKey(item.key);
    try {
      const result = await postWithdrawal({
        orderId: item.order.id,
        productId: item.order.productId,
        voucherId: item.entry.id,
        manual: true,
      });
      addToast(result.alreadyScanned ? "Ficha já estava retirada." : "Retirada manual registrada.", "success");
      await load();
    } catch (error: unknown) {
      addToast(error instanceof Error ? error.message : "Falha ao registrar retirada.", "error");
    } finally {
      setProcessingKey("");
    }
  };

  const handleCorrectWithdrawal = async (item: WithdrawalItem) => {
    if (isSameOperator(user, item)) {
      addToast("A correção precisa ser feita por outro operador.", "error");
      return;
    }

    const note = window.prompt(
      "Motivo da correção da retirada",
      "Baixa marcada por engano"
    );
    if (note === null) return;

    setProcessingKey(`correct:${item.key}`);
    try {
      await patchWithdrawalCorrection(item, note);
      addToast("Retirada corrigida. A ficha voltou para pendentes.", "success");
      await load();
    } catch (error: unknown) {
      addToast(error instanceof Error ? error.message : "Falha ao corrigir retirada.", "error");
    } finally {
      setProcessingKey("");
    }
  };

  const handleScanPayload = useCallback(
    async (decoded: string) => {
      const clean = decoded.trim();
      if (!clean || lastPayloadRef.current === clean || processingKey) return;
      lastPayloadRef.current = clean;
      setTimeout(() => {
        if (lastPayloadRef.current === clean) lastPayloadRef.current = "";
      }, 1800);

      const parsed = parseEventProductVoucherQrPayload(clean);
      if (!parsed || parsed.eventId !== eventId) {
        setScanMessage("QR inválido para este evento.");
        addToast("QR inválido para este evento.", "error");
        return;
      }

      setProcessingKey("scan");
      setScanMessage("Registrando retirada...");
      try {
        const result = await postWithdrawal({ qrPayload: clean });
        setScanMessage(
          result.alreadyScanned
            ? `${result.voucherLabel || "Ficha"} já estava retirada.`
            : `${result.voucherLabel || "Ficha"} retirada com sucesso.`
        );
        addToast(result.alreadyScanned ? "Ficha já estava retirada." : "Retirada via scan registrada.", "success");
        await load();
      } catch (error: unknown) {
        const message = error instanceof Error ? error.message : "Falha ao registrar retirada.";
        setScanMessage(message);
        addToast(message, "error");
      } finally {
        setProcessingKey("");
      }
    },
    [addToast, eventId, load, postWithdrawal, processingKey]
  );

  const startScanner = async () => {
    if (scannerActive || scannerStarting) return;
    setScannerStarting(true);
    setScanMessage("Abrindo câmera...");
    try {
      const scanner = new Html5Qrcode("event-product-withdrawal-scanner");
      scannerRef.current = scanner;
      const cameras = await Html5Qrcode.getCameras().catch(() => []);
      const camera =
        cameras.find((entry) => /back|rear|traseira|environment/i.test(entry.label)) ||
        cameras[0];
      await scanner.start(
        camera?.id || { facingMode: "environment" },
        {
          fps: 12,
          qrbox: (width, height) => {
            const edge = Math.max(1, Math.min(width, height));
            const size = Math.min(320, Math.max(220, Math.floor(edge * 0.72)));
            return { width: size, height: size };
          },
          disableFlip: false,
        },
        (decoded) => void handleScanPayload(decoded),
        () => undefined
      );
      setScannerActive(true);
      setScanMessage("Câmera ativa. Aponte para o QR da ficha.");
    } catch {
      setScanMessage("Não foi possível abrir a câmera neste dispositivo.");
    } finally {
      setScannerStarting(false);
    }
  };

  const productById = useMemo(
    () => new Map(products.map((product) => [product.id, product])),
    [products]
  );

  const withdrawalItems = useMemo<WithdrawalItem[]>(() => {
    return orders
      .filter(isApprovedOrder)
      .flatMap((order) =>
        getEventPartyVoucherEntries(order)
          .filter((entry) => entry.status === "ativo" || entry.status === "utilizado" || entry.status === "inativo")
          .map((entry) => ({
            key: `${order.id}:${entry.id}`,
            order,
            entry,
            productName: productById.get(order.productId)?.nome || order.productName,
            status: entry.status === "utilizado" || entry.status === "inativo" ? "retirado" : "pendente",
          }))
      );
  }, [orders, productById]);

  const filteredItems = useMemo(() => {
    const normalizedSearch = normalizeSearch(search);
    return withdrawalItems.filter((item) => {
      const eventPartyData = asRecord(item.order.data.eventParty);
      const manualCustomer = asRecord(eventPartyData.manualCustomer);
      const reference = getEventPartyOrderReference(item.order);
      const matchesMode =
        mode === "hub" ||
        (mode === "pendentes" && item.status === "pendente") ||
        (mode === "retirados" && item.status === "retirado");
      const matchesSearch =
        !normalizedSearch ||
        normalizeSearch(
          [
            item.order.id,
            item.order.userId,
            item.order.userName,
            item.productName,
            item.entry.id,
            item.entry.label,
            item.entry.code,
            item.entry.manualNumber,
            reference.summary,
            eventPartyData.manualCode,
            eventPartyData.manualNumber,
            eventPartyData.externalNumber,
            manualCustomer.externalNumber,
            manualCustomer.ra,
            item.status,
          ].join(" ")
        ).includes(normalizedSearch);
      return matchesMode && matchesSearch;
    });
  }, [mode, search, withdrawalItems]);

  const total = withdrawalItems.length;
  const retirados = withdrawalItems.filter((item) => item.status === "retirado").length;
  const pendentes = Math.max(0, total - retirados);
  const totalPages = Math.max(1, Math.ceil(filteredItems.length / WITHDRAWAL_PAGE_SIZE));
  const currentPage = Math.min(page, totalPages);
  const paginatedItems = filteredItems.slice(
    (currentPage - 1) * WITHDRAWAL_PAGE_SIZE,
    currentPage * WITHDRAWAL_PAGE_SIZE
  );
  const pageTitle =
    mode === "pendentes" ? "Fichas pendentes" : mode === "retirados" ? "Fichas retiradas" : "Retirada do produto";
  const pageDescription =
    mode === "hub"
      ? "Acesse as listas separadas ou abra a câmera para ler o QR individual da ficha."
      : mode === "pendentes"
        ? "Marque manualmente as fichas aprovadas que ainda não foram retiradas."
        : "Consulte as fichas já retiradas com data, operador e origem da baixa.";

  useEffect(() => {
    setPage(1);
  }, [mode, search]);

  useEffect(() => {
    setPage((current) => Math.min(current, totalPages));
  }, [totalPages]);

  return (
    <main className="min-h-screen bg-[#050505] px-4 py-5 pb-28 text-white sm:px-6">
      <div className="mx-auto max-w-6xl space-y-5">
        <header className="rounded-[2rem] border border-zinc-800 bg-zinc-950 p-5">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
            <div className="flex items-start gap-3">
              <Link
                href={mode === "hub" ? fichaHref : retiradaHref}
                className="inline-flex h-11 w-11 items-center justify-center rounded-2xl border border-zinc-800 bg-black/40 text-zinc-300"
              >
                <ArrowLeft size={18} />
              </Link>
              <div>
                <p className="text-[10px] font-black uppercase tracking-[0.28em] text-emerald-400">Retirada do produto</p>
                <h1 className="mt-2 text-2xl font-black uppercase text-white">{pageTitle}</h1>
                <p className="mt-1 text-sm text-zinc-500">
                  {event?.titulo || "Evento"} • {pageDescription}
                </p>
              </div>
            </div>
            <Link href={extratoHref} className="rounded-xl border border-zinc-700 bg-black/20 px-4 py-3 text-xs font-black uppercase text-zinc-200">
              Ver extrato
            </Link>
          </div>
        </header>

        <section className="grid gap-3 sm:grid-cols-2">
          <Link
            href={pendentesHref}
            className={`rounded-[1.6rem] border p-5 transition ${
              mode === "pendentes"
                ? "border-yellow-400/40 bg-yellow-500/10"
                : "border-zinc-800 bg-zinc-950 hover:border-yellow-400/30"
            }`}
          >
            <div className="flex items-center justify-between gap-3">
              <div>
                <p className="text-[11px] font-black uppercase tracking-[0.2em] text-zinc-500">Página</p>
                <p className="mt-2 text-lg font-black uppercase text-white">Pendentes</p>
              </div>
              <ClipboardList className="text-yellow-300" size={22} />
            </div>
            <p className="mt-4 text-3xl font-black text-yellow-300">{pendentes}</p>
          </Link>
          <Link
            href={retiradosHref}
            className={`rounded-[1.6rem] border p-5 transition ${
              mode === "retirados"
                ? "border-emerald-400/40 bg-emerald-500/10"
                : "border-zinc-800 bg-zinc-950 hover:border-emerald-400/30"
            }`}
          >
            <div className="flex items-center justify-between gap-3">
              <div>
                <p className="text-[11px] font-black uppercase tracking-[0.2em] text-zinc-500">Página</p>
                <p className="mt-2 text-lg font-black uppercase text-white">Retirados</p>
              </div>
              <CheckCircle2 className="text-emerald-300" size={22} />
            </div>
            <p className="mt-4 text-3xl font-black text-emerald-300">{retirados}</p>
          </Link>
        </section>

        {mode === "hub" ? (
          <section className="rounded-[1.8rem] border border-zinc-800 bg-zinc-950 p-4">
            <div className="flex items-center justify-between gap-3">
              <div>
                <h2 className="text-lg font-black text-white">Scanner</h2>
                <p className="text-sm text-zinc-500">{total} ficha(s) aprovadas neste evento.</p>
              </div>
              <QrCode className="text-emerald-400" size={22} />
            </div>
            <div className="mt-4 grid gap-2 sm:grid-cols-2">
              <button
                type="button"
                onClick={() => void startScanner()}
                disabled={scannerStarting || scannerActive}
                className="inline-flex items-center justify-center gap-2 rounded-xl bg-emerald-500 px-4 py-3 text-xs font-black uppercase text-black disabled:opacity-50"
              >
                {scannerStarting ? <Loader2 size={14} className="animate-spin" /> : <ScanLine size={14} />}
                Abrir câmera
              </button>
              <button
                type="button"
                onClick={() => void stopScanner()}
                disabled={!scannerActive}
                className="rounded-xl border border-zinc-700 bg-black/20 px-4 py-3 text-xs font-black uppercase text-zinc-200 disabled:opacity-50"
              >
                Parar
              </button>
            </div>
            <div id="event-product-withdrawal-scanner" className="qr-reader-surface mt-4 min-h-[300px] overflow-hidden rounded-3xl border border-dashed border-zinc-700 bg-black" />
            <div className="mt-3 rounded-xl border border-zinc-800 bg-black/30 p-3 text-xs text-zinc-300">
              {processingKey === "scan" ? <Loader2 size={14} className="mr-2 inline animate-spin text-emerald-400" /> : null}
              {scanMessage}
            </div>
          </section>
        ) : (
          <section className="rounded-[1.8rem] border border-zinc-800 bg-zinc-950 p-4">
            <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
              <div>
                <h2 className="text-lg font-black text-white">
                  {mode === "pendentes" ? "Retiradas pendentes" : "Retiradas registradas"}
                </h2>
                <p className="text-sm text-zinc-500">
                  {mode === "pendentes"
                    ? "Marque manualmente quando a leitura não for possível."
                    : "Histórico de retirada com data, usuário e método."}
                </p>
              </div>
              <button
                type="button"
                onClick={() => void load()}
                className="rounded-xl border border-zinc-700 bg-black/20 px-4 py-3 text-xs font-black uppercase text-zinc-200"
              >
                Atualizar
              </button>
            </div>

            <div className="mt-4 grid gap-3 md:grid-cols-[1fr_auto]">
              <div className="flex items-center gap-2 rounded-2xl border border-zinc-800 bg-black/20 px-3 py-3">
                <Search size={16} className="text-zinc-500" />
                <input
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                  placeholder="Buscar por usuário, pulseira, nº da ficha ou pedido..."
                  className="min-w-0 flex-1 bg-transparent text-sm outline-none placeholder:text-zinc-600"
                />
              </div>
              <div className="rounded-2xl border border-zinc-800 bg-black/30 px-4 py-3 text-xs font-black uppercase text-zinc-400">
                20 por página
              </div>
            </div>

            <div className="mt-4 space-y-3">
              {loading ? (
                <div className="rounded-2xl border border-zinc-800 bg-black/20 p-8 text-center text-emerald-400">
                  <Loader2 size={20} className="mx-auto animate-spin" />
                </div>
              ) : filteredItems.length === 0 ? (
                <div className="rounded-2xl border border-dashed border-zinc-800 bg-black/20 p-8 text-center text-sm text-zinc-500">
                  Nenhuma ficha encontrada nessa página.
                </div>
              ) : (
                paginatedItems.map((item) => {
                  const reference = getEventPartyOrderReference(item.order);
                  const sameWithdrawalOperator = isSameOperator(user, item);
                  return (
                    <article key={item.key} className="rounded-2xl border border-zinc-800 bg-black/20 p-4">
                      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                        <div>
                          <div className="flex flex-wrap items-center gap-2">
                            <p className="font-black text-white">{item.order.userName}</p>
                            <span className={`rounded-full px-2 py-1 text-[10px] font-black uppercase ${
                              item.status === "retirado"
                                ? "bg-emerald-500/10 text-emerald-300"
                                : "bg-yellow-500/10 text-yellow-300"
                            }`}>
                              {item.status}
                            </span>
                          </div>
                          <p className="mt-1 text-sm font-bold text-zinc-300">{item.productName} • {item.entry.label}</p>
                          <p className="mt-1 font-mono text-[10px] text-zinc-600">{reference.summary}</p>
                          <div className="mt-2 grid gap-2 text-xs text-zinc-500 sm:grid-cols-2">
                            <span>Data do pedido do produto: {formatDateOnly(item.order.createdAt)}</span>
                            <span>Hora do pedido do produto: {formatTimeOnly(item.order.createdAt)}</span>
                          </div>
                          {item.status === "retirado" ? (
                            <p className="mt-2 text-xs text-zinc-500">
                              {formatDateTime(item.entry.usedAt)} • {methodLabel(item.entry.usedMethod)} • {item.entry.usedByUserName || "-"}
                            </p>
                          ) : null}
                        </div>
                        {mode === "pendentes" ? (
                          <button
                            type="button"
                            onClick={() => void handleManualWithdrawal(item)}
                            disabled={processingKey === item.key || item.status === "retirado"}
                            className="inline-flex items-center justify-center gap-2 rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-xs font-black uppercase text-emerald-300 disabled:opacity-50"
                          >
                            {processingKey === item.key ? <Loader2 size={14} className="animate-spin" /> : <CheckCircle2 size={14} />}
                            Retirar
                          </button>
                        ) : mode === "retirados" ? (
                          <button
                            type="button"
                            onClick={() => void handleCorrectWithdrawal(item)}
                            disabled={
                              processingKey === `correct:${item.key}` ||
                              item.status !== "retirado" ||
                              sameWithdrawalOperator
                            }
                            title={
                              sameWithdrawalOperator
                                ? "A correção precisa ser feita por outro operador."
                                : "Corrigir retirada e devolver a ficha para pendentes."
                            }
                            className="inline-flex items-center justify-center gap-2 rounded-xl border border-yellow-500/30 bg-yellow-500/10 px-4 py-3 text-xs font-black uppercase text-yellow-200 disabled:cursor-not-allowed disabled:opacity-50"
                          >
                            {processingKey === `correct:${item.key}` ? <Loader2 size={14} className="animate-spin" /> : <RotateCcw size={14} />}
                            {sameWithdrawalOperator ? "Outro operador" : "Corrigir retirada"}
                          </button>
                        ) : (
                          <span className={`inline-flex items-center justify-center gap-2 rounded-xl border px-4 py-3 text-xs font-black uppercase ${
                            item.status === "retirado"
                              ? "border-emerald-500/20 bg-emerald-500/10 text-emerald-300"
                              : "border-yellow-500/20 bg-yellow-500/10 text-yellow-300"
                          }`}>
                            <CheckCircle2 size={14} />
                            {item.status === "retirado" ? "Retirado" : "Pendente"}
                          </span>
                        )}
                      </div>
                    </article>
                  );
                })
              )}
            </div>

            {filteredItems.length > 0 ? (
              <div className="mt-4 flex flex-col gap-3 border-t border-zinc-800 pt-4 sm:flex-row sm:items-center sm:justify-between">
                <p className="text-xs font-bold text-zinc-500">
                  Página {currentPage} de {totalPages} • {filteredItems.length} ficha(s)
                </p>
                <div className="grid grid-cols-2 gap-2 sm:flex">
                  <button
                    type="button"
                    onClick={() => setPage((current) => Math.max(1, current - 1))}
                    disabled={currentPage <= 1}
                    className="rounded-xl border border-zinc-700 bg-black/20 px-4 py-3 text-xs font-black uppercase text-zinc-200 disabled:opacity-40"
                  >
                    Anterior
                  </button>
                  <button
                    type="button"
                    onClick={() => setPage((current) => Math.min(totalPages, current + 1))}
                    disabled={currentPage >= totalPages}
                    className="rounded-xl border border-zinc-700 bg-black/20 px-4 py-3 text-xs font-black uppercase text-zinc-200 disabled:opacity-40"
                  >
                    Próxima
                  </button>
                </div>
              </div>
            ) : null}
          </section>
        )}
      </div>
    </main>
  );
}
