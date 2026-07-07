"use client";

import React, { useState } from "react";
import Link from "next/link";
import { ArrowLeft, Loader2, Send, ShieldCheck } from "lucide-react";

import { getSupabaseClient } from "@/lib/supabase";

const requestTypes = [
  { value: "confirmacao_tratamento", label: "Confirmação da existência de tratamento" },
  { value: "acesso", label: "Acesso aos dados" },
  { value: "correcao", label: "Correção de dados" },
  { value: "anonimizacao_bloqueio_eliminacao", label: "Anonimização, bloqueio ou eliminação" },
  { value: "portabilidade", label: "Portabilidade" },
  { value: "eliminacao_consentimento", label: "Eliminação de dados tratados com consentimento" },
  { value: "compartilhamento", label: "Informação sobre compartilhamento" },
  { value: "informacao_consentimento", label: "Informação sobre consentimento" },
  { value: "revogacao_consentimento", label: "Revogação de consentimento" },
  { value: "revisao_decisao_automatizada", label: "Revisão de decisão automatizada" },
  { value: "oposicao", label: "Oposição ao tratamento" },
  { value: "outro", label: "Outro pedido LGPD" },
] as const;

type RequestTypeValue = typeof requestTypes[number]["value"];

export default function LgpdRequestPage() {
  const [requesterName, setRequesterName] = useState("");
  const [requesterEmail, setRequesterEmail] = useState("");
  const [requestType, setRequestType] = useState<RequestTypeValue>(requestTypes[0].value);
  const [requestDetails, setRequestDetails] = useState("");
  const [acceptedIdentityNotice, setAcceptedIdentityNotice] = useState(false);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setLoading(true);
    setError("");
    setMessage("");

    try {
      const supabase = getSupabaseClient();
      const { data } = await supabase.auth.getSession();
      const response = await fetch("/api/lgpd/requests", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...(data.session?.access_token
            ? { Authorization: `Bearer ${data.session.access_token}` }
            : {}),
        },
        body: JSON.stringify({
          requesterName,
          requesterEmail,
          requestType,
          requestDetails,
        }),
      });

      if (!response.ok) {
        const payload = (await response.json().catch(() => null)) as { error?: string } | null;
        throw new Error(payload?.error || "Não foi possível enviar a solicitação.");
      }

      setMessage("Solicitação recebida. A USC poderá pedir confirmação de identidade antes de responder.");
      setRequesterName("");
      setRequesterEmail("");
      setRequestType(requestTypes[0].value);
      setRequestDetails("");
      setAcceptedIdentityNotice(false);
    } catch (submitError: unknown) {
      setError(submitError instanceof Error ? submitError.message : "Erro inesperado.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#02050d] text-white">
      <div className="pointer-events-none fixed inset-0 bg-[radial-gradient(circle_at_top_left,rgba(37,99,235,0.18),transparent_35%),linear-gradient(180deg,rgba(2,5,13,0.92),#02050d)]" />
      <main className="relative z-10 mx-auto flex min-h-screen max-w-3xl flex-col px-4 py-8">
        <Link
          href="/direitos-lgpd"
          className="mb-6 inline-flex w-fit items-center gap-2 text-sm font-bold text-zinc-300 transition hover:text-white"
        >
          <ArrowLeft size={16} />
          Voltar para Direitos LGPD
        </Link>

        <section className="rounded-lg border border-blue-400/20 bg-blue-500/10 p-5 sm:p-7">
          <div className="mb-4 inline-flex items-center gap-2 rounded-lg border border-white/10 bg-white/[0.06] px-3 py-2 text-xs font-black uppercase tracking-wide text-blue-100">
            <ShieldCheck size={16} />
            Solicitação LGPD
          </div>
          <h1 className="text-3xl font-black leading-tight sm:text-4xl">
            Exercício de direitos do titular
          </h1>
          <p className="mt-4 text-sm leading-7 text-zinc-300 sm:text-base">
            Use este formulário para registrar pedidos relacionados à LGPD na USC – Universidade Spot Connect.
            Não informe senhas, dados bancários completos ou documentos de terceiros neste campo aberto.
          </p>
        </section>

        <form onSubmit={submit} className="mt-6 space-y-4 rounded-lg border border-white/10 bg-white/[0.045] p-5 sm:p-6">
          <div className="grid gap-4 sm:grid-cols-2">
            <label className="space-y-2">
              <span className="text-xs font-black uppercase tracking-wide text-zinc-400">Nome</span>
              <input
                value={requesterName}
                onChange={(event) => setRequesterName(event.target.value)}
                className="w-full rounded-lg border border-zinc-700 bg-black px-4 py-3 text-sm font-semibold text-white outline-none transition focus:border-blue-400"
                required
                maxLength={180}
              />
            </label>
            <label className="space-y-2">
              <span className="text-xs font-black uppercase tracking-wide text-zinc-400">E-mail</span>
              <input
                type="email"
                value={requesterEmail}
                onChange={(event) => setRequesterEmail(event.target.value)}
                className="w-full rounded-lg border border-zinc-700 bg-black px-4 py-3 text-sm font-semibold text-white outline-none transition focus:border-blue-400"
                required
                maxLength={180}
              />
            </label>
          </div>

          <label className="space-y-2 block">
            <span className="text-xs font-black uppercase tracking-wide text-zinc-400">Tipo de solicitação</span>
            <select
              value={requestType}
              onChange={(event) => setRequestType(event.target.value as RequestTypeValue)}
              className="w-full rounded-lg border border-zinc-700 bg-black px-4 py-3 text-sm font-semibold text-white outline-none transition focus:border-blue-400"
            >
              {requestTypes.map((type) => (
                <option key={type.value} value={type.value}>
                  {type.label}
                </option>
              ))}
            </select>
          </label>

          <label className="space-y-2 block">
            <span className="text-xs font-black uppercase tracking-wide text-zinc-400">Detalhes do pedido</span>
            <textarea
              value={requestDetails}
              onChange={(event) => setRequestDetails(event.target.value)}
              className="min-h-40 w-full rounded-lg border border-zinc-700 bg-black px-4 py-3 text-sm font-semibold leading-7 text-white outline-none transition focus:border-blue-400"
              required
              maxLength={4000}
            />
          </label>

          <label className="flex cursor-pointer items-start gap-3 rounded-lg border border-amber-400/20 bg-amber-400/10 p-4">
            <input
              type="checkbox"
              checked={acceptedIdentityNotice}
              onChange={(event) => setAcceptedIdentityNotice(event.target.checked)}
              className="mt-1 h-4 w-4 rounded border-zinc-700 bg-zinc-950 accent-blue-500"
              required
            />
            <span className="text-xs font-semibold leading-6 text-amber-100">
              Estou ciente de que a USC poderá solicitar informações adicionais para confirmar minha identidade
              antes de atender ao pedido, evitando acesso indevido a dados pessoais.
            </span>
          </label>

          {error ? <p className="rounded-lg border border-red-500/25 bg-red-500/10 p-3 text-sm text-red-100">{error}</p> : null}
          {message ? <p className="rounded-lg border border-emerald-500/25 bg-emerald-500/10 p-3 text-sm text-emerald-100">{message}</p> : null}

          <button
            type="submit"
            disabled={loading || !acceptedIdentityNotice}
            className="inline-flex w-full items-center justify-center gap-2 rounded-lg bg-blue-600 px-5 py-4 text-xs font-black uppercase tracking-wide text-white transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {loading ? <Loader2 size={18} className="animate-spin" /> : <Send size={18} />}
            Enviar solicitação
          </button>
        </form>
      </main>
    </div>
  );
}
