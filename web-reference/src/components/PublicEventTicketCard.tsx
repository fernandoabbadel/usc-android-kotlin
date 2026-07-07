"use client";

import { useState } from "react";
import Image from "next/image";
import { ArrowLeft, CalendarDays, Loader2, MapPin, QrCode, Send } from "lucide-react";
import { QRCodeSVG } from "qrcode.react";

interface PublicEventTicketCardProps {
  qrValue: string;
  imageUrl: string;
  eventTitle: string;
  eventDateLabel: string;
  eventLocation: string;
  loteName: string;
  holderName: string;
  holderTurma: string;
  ticketCode: string;
  status: "ativo" | "lido" | "transferido";
  orderId: string;
  ticketToken: string;
  transferAllowed: boolean;
  transferredToUserName: string;
  transferredFromUserName: string;
}

export function PublicEventTicketCard({
  qrValue,
  imageUrl,
  eventTitle,
  eventDateLabel,
  eventLocation,
  loteName,
  holderName,
  holderTurma,
  ticketCode,
  status,
  orderId,
  ticketToken,
  transferAllowed,
  transferredToUserName,
  transferredFromUserName,
}: PublicEventTicketCardProps) {
  const [recipient, setRecipient] = useState("");
  const [transferLoading, setTransferLoading] = useState(false);
  const [transferMessage, setTransferMessage] = useState("");
  const canTransfer = transferAllowed && status === "ativo";

  const handleTransfer = async () => {
    const cleanRecipient = recipient.trim();
    if (!cleanRecipient) {
      setTransferMessage("Informe o e-mail, telefone ou RA exato do usuário que vai receber.");
      return;
    }
    if (!window.confirm(`Transferir este ingresso para ${cleanRecipient}? O seu QR Code atual será desativado.`)) {
      return;
    }
    setTransferLoading(true);
    setTransferMessage("");
    try {
      const response = await fetch("/api/event-tickets/transfer", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ orderId, ticketToken, recipient: cleanRecipient }),
      });
      const payload = (await response.json().catch(() => null)) as { message?: string; error?: string } | null;
      if (!response.ok) {
        throw new Error(payload?.error || "Não foi possível transferir o ingresso.");
      }
      setTransferMessage(payload?.message || "Ingresso transferido. Atualize a página para ver o novo status.");
    } catch (error: unknown) {
      setTransferMessage(error instanceof Error ? error.message : "Não foi possível transferir o ingresso.");
    } finally {
      setTransferLoading(false);
    }
  };

  return (
    <main className="min-h-screen bg-[#050505] px-4 py-8 text-white">
      <div className="mx-auto mb-3 max-w-[360px]">
        <button
          type="button"
          onClick={() => window.history.back()}
          className="inline-flex items-center gap-2 rounded-xl border border-zinc-800 bg-zinc-950 px-3 py-2 text-xs font-black uppercase text-zinc-300 transition hover:border-zinc-700 hover:text-white"
        >
          <ArrowLeft size={14} />
          Voltar
        </button>
      </div>
      <div className="mx-auto max-w-[360px] overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-950 shadow-[0_24px_80px_rgba(0,0,0,0.45)]">
        <div className="relative h-44 w-full bg-black">
          <Image
            src={imageUrl || "/logo.png"}
            alt={eventTitle}
            fill
            sizes="340px"
            className="object-cover"
            unoptimized={imageUrl.startsWith("http")}
          />
          <div className="absolute inset-0 bg-gradient-to-t from-[#050505] via-black/35 to-transparent" />
          <div className="absolute inset-x-0 bottom-0 p-4">
            <div className="flex items-end justify-between gap-3">
              <div className="min-w-0">
                <p className="text-[10px] font-black uppercase tracking-widest text-emerald-300">
                  Ingresso digital
                </p>
                <h1 className="mt-1 line-clamp-2 text-xl font-black uppercase leading-tight text-white">
                  {eventTitle}
                </h1>
              </div>
              <span
                className={`shrink-0 rounded-full border px-2 py-1 text-[10px] font-black uppercase ${
                  status === "lido" || status === "transferido"
                    ? "border-red-500/30 bg-red-500/15 text-red-300"
                    : "border-emerald-500/30 bg-emerald-500/15 text-emerald-300"
                }`}
              >
                {status === "lido" ? "Lido" : status === "transferido" ? "Transferido" : "Válido"}
              </span>
            </div>
          </div>
        </div>

        <div className="border-y border-zinc-800 bg-black/35 px-4 py-3">
          <div className="space-y-1 text-[11px] text-zinc-300">
            <div className="flex items-center gap-2">
              <CalendarDays size={12} className="text-emerald-300" />
              <span className="truncate">{eventDateLabel}</span>
            </div>
            <div className="flex items-center gap-2">
              <MapPin size={12} className="text-emerald-300" />
              <span className="truncate">{eventLocation || "Local a confirmar"}</span>
            </div>
          </div>
        </div>

        <div className="px-5 py-4">
          {status === "transferido" ? (
            <div className="rounded-2xl border border-red-500/30 bg-red-500/10 px-4 py-6 text-center">
              <QrCode size={44} className="mx-auto text-red-300" />
              <p className="mt-3 text-xs font-black uppercase text-red-200">QR Code desativado</p>
              <p className="mt-2 text-[11px] text-red-100/80">
                Este ingresso foi transferido e o QR Code antigo não pode mais ser usado.
              </p>
            </div>
          ) : (
            <div className="rounded-2xl border border-zinc-800 bg-white px-4 py-5 text-center">
              <QRCodeSVG value={qrValue} size={210} includeMargin className="mx-auto" />
              <p className="mt-3 text-[10px] font-mono text-zinc-400">{ticketCode}</p>
              <p className="mt-2 text-[10px] text-zinc-500">
                Apresente este QR Code na entrada do evento
              </p>
            </div>
          )}

          <div className="mt-4 grid gap-3 border-t border-dashed border-zinc-800 pt-4 text-sm">
            <div className="rounded-xl border border-zinc-800 bg-black/30 px-3 py-3">
              <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">
                Tipo de ingresso
              </p>
              <p className="mt-1 font-bold text-white">{loteName || "Ingresso"}</p>
            </div>
            <div className="rounded-xl border border-zinc-800 bg-black/30 px-3 py-3">
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">
                    Titular do ingresso
                  </p>
                  <p className="mt-1 break-words font-bold text-white">{holderName}</p>
                </div>
                {canTransfer ? (
                  <button
                    type="button"
                    onClick={handleTransfer}
                    disabled={transferLoading}
                    className="inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-full border border-emerald-500/25 bg-emerald-500/10 text-emerald-300 disabled:opacity-60"
                    title="Transferir ingresso"
                  >
                    {transferLoading ? <Loader2 size={16} className="animate-spin" /> : <Send size={16} />}
                  </button>
                ) : null}
              </div>
              <p className="text-xs text-zinc-400">{holderTurma || "Sem turma"}</p>
              {transferredToUserName ? (
                <p className="mt-2 text-xs font-semibold text-amber-200">Transferido para {transferredToUserName}.</p>
              ) : null}
              {transferredFromUserName ? (
                <p className="mt-2 text-xs font-semibold text-emerald-200">Recebido de {transferredFromUserName}.</p>
              ) : null}
              {canTransfer ? (
                <div className="mt-3 flex flex-col gap-2">
                  <input
                    value={recipient}
                    onChange={(event) => setRecipient(event.target.value)}
                    placeholder="E-mail, telefone ou RA exato"
                    className="w-full rounded-xl border border-zinc-700 bg-black px-3 py-2 text-xs text-white outline-none placeholder:text-zinc-600"
                  />
                  {transferMessage ? <p className="text-xs text-zinc-400">{transferMessage}</p> : null}
                </div>
              ) : transferMessage ? (
                <p className="mt-2 text-xs text-zinc-400">{transferMessage}</p>
              ) : null}
            </div>
          </div>

          <div className="mt-4 rounded-2xl border border-amber-500/20 bg-amber-500/10 px-4 py-3 text-[11px] text-amber-200">
            <div className="flex items-start gap-2">
              <QrCode size={14} className="mt-0.5" />
              <p>
                O QR Code é individual. Depois da leitura, o status deste ingresso muda para
                lido automaticamente.
              </p>
            </div>
          </div>
        </div>
      </div>
    </main>
  );
}
