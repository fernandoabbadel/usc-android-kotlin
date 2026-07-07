"use client";

import React, { useState } from "react";
import { KeyRound, Loader2, Lock, Mail } from "lucide-react";
import Link from "next/link";
import Image from "next/image";
import { usePathname, useRouter } from "next/navigation";

import { useToast } from "../../context/ToastContext";
import { useTenantTheme } from "@/context/TenantThemeContext";
import {
  loginPartnerByEmail,
  loginPartnerByEmailGlobal,
  verifyPartnerPasswordResetCode,
} from "../../lib/partnersService";
import { PLATFORM_BRAND_NAME, PLATFORM_LOGO_URL } from "@/constants/platformBrand";
import { parseTenantScopedPath, withTenantSlug } from "@/lib/tenantRouting";

export default function EmpresaLoginPage() {
  const router = useRouter();
  const pathname = usePathname() || "/empresa";
  const { addToast } = useToast();
  const { tenantId, tenantLogoUrl, tenantName } = useTenantTheme();
  const pathInfo = parseTenantScopedPath(pathname);
  const isTenantScopedPartnerPage = Boolean(pathInfo.tenantSlug);
  const companyBasePath = isTenantScopedPartnerPage
    ? withTenantSlug(pathInfo.tenantSlug, "/empresa")
    : "/empresa";
  const companyRegisterPath = isTenantScopedPartnerPage
    ? withTenantSlug(pathInfo.tenantSlug, "/empresa/cadastro")
    : "/empresa/cadastro";
  const supportPath = isTenantScopedPartnerPage
    ? withTenantSlug(pathInfo.tenantSlug, "/contato-usc")
    : "/contato-usc";
  const displayLogoUrl = isTenantScopedPartnerPage ? tenantLogoUrl : PLATFORM_LOGO_URL;
  const displayBrandName = isTenantScopedPartnerPage
    ? tenantName || "Atlética"
    : PLATFORM_BRAND_NAME;
  const accent = isTenantScopedPartnerPage
    ? {
        orb: "bg-emerald-600/15",
        glow: "bg-emerald-500/30 group-hover:bg-emerald-500/50",
        focus: "focus:border-emerald-500",
        button: "bg-emerald-600 hover:bg-emerald-500",
        link: "text-emerald-400",
      }
    : {
        orb: "bg-blue-600/15",
        glow: "bg-blue-500/30 group-hover:bg-blue-500/50",
        focus: "focus:border-blue-500",
        button: "bg-blue-600 hover:bg-blue-500",
        link: "text-blue-400",
      };

  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");
  const [codigoReset, setCodigoReset] = useState("");
  const [resetMode, setResetMode] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      if (resetMode) {
        if (!isTenantScopedPartnerPage || !tenantId) {
          addToast("Para redefinir a senha, entre pelo painel parceiro da atlética.", "info");
          setLoading(false);
          return;
        }

        const resetResult = await verifyPartnerPasswordResetCode({
          email,
          code: codigoReset,
          tenantId,
        });

        if (!resetResult) {
          addToast("Código inválido ou expirado.", "error");
          setLoading(false);
          return;
        }

        const params = new URLSearchParams({
          resetEmail: email.trim().toLowerCase(),
          resetCode: codigoReset.trim(),
        });
        addToast("Código confirmado. Cadastre a nova senha.", "success");
        router.push(`${companyRegisterPath}?${params.toString()}`);
        return;
      }

      if (isTenantScopedPartnerPage && !tenantId) {
        addToast("Atlética não identificada. Recarregue a página e tente novamente.", "error");
        setLoading(false);
        return;
      }

      const loginResult = isTenantScopedPartnerPage
        ? await loginPartnerByEmail({
            email,
            senha,
            tenantId,
          })
        : await loginPartnerByEmailGlobal({
            email,
            senha,
          });

      if (!loginResult) {
        addToast("E-mail não encontrado.", "error");
        setLoading(false);
        return;
      }

      if (!loginResult.passwordValid) {
        if (isTenantScopedPartnerPage && loginResult.hasPasswordResetCode) {
          setResetMode(true);
          addToast("Informe o código de 6 números enviado pelo suporte.", "info");
          setLoading(false);
          return;
        }
        addToast("Senha incorreta.", "error");
        setLoading(false);
        return;
      }

      if (loginResult.status === "pending") {
        addToast("Cadastro em análise. Aguarde aprovação.", "info");
        setLoading(false);
        return;
      }

      if (loginResult.status === "disabled") {
        addToast("Acesso desativado. Contate a Atlética.", "error");
        setLoading(false);
        return;
      }

      addToast(`Bem-vindo, ${loginResult.nome}!`, "success");
      const targetBasePath =
        !isTenantScopedPartnerPage && loginResult.tenantSlug
          ? withTenantSlug(loginResult.tenantSlug, "/empresa")
          : companyBasePath;
      router.push(`${targetBasePath}/${loginResult.id}`);
    } catch (error: unknown) {
      console.error("Erro no login:", error);
      addToast("Erro de conexão.", "error");
      setLoading(false);
    }
  };

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-[#050505] p-6 font-sans">
      <div
        className={`pointer-events-none absolute left-[-20%] top-[-20%] h-[60%] w-[60%] animate-pulse-slow rounded-full ${accent.orb} blur-[120px]`}
      />

      <div className="relative z-10 w-full max-w-md rounded-[2rem] border border-zinc-800/80 bg-zinc-900/60 p-8 shadow-2xl backdrop-blur-xl">
        <div className="mb-8 text-center">
          <div className="group relative mx-auto mb-4 h-24 w-24">
            <div
              className={`absolute inset-0 rounded-full ${accent.glow} blur-xl transition duration-500`}
            />
            <Image
              src={displayLogoUrl || "/logo.png"}
              alt={`Logo ${displayBrandName}`}
              fill
              sizes="96px"
              className="relative z-10 object-contain drop-shadow-2xl"
              unoptimized={Boolean(displayLogoUrl && displayLogoUrl.startsWith("http"))}
              priority
            />
          </div>
          <h1 className="mb-1 text-2xl font-black uppercase tracking-tighter text-white">
            Área do Parceiro
          </h1>
          <p className="text-xs font-medium text-zinc-400">Gerencie seus cupons e métricas.</p>
        </div>

        <form onSubmit={handleLogin} className="space-y-4">
          <div className="relative">
            <Mail className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500" size={18} />
            <input
              type="email"
              placeholder="Email Corporativo"
              className={`w-full rounded-xl border border-zinc-700 bg-black/50 p-4 pl-12 text-white outline-none transition ${accent.focus}`}
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                if (resetMode) setResetMode(false);
              }}
              required
            />
          </div>

          {resetMode ? (
            <div className="relative">
              <KeyRound
                className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500"
                size={18}
              />
              <input
                type="text"
                inputMode="numeric"
                maxLength={6}
                placeholder="Código de 6 números"
                className={`w-full rounded-xl border border-zinc-700 bg-black/50 p-4 pl-12 text-white outline-none transition ${accent.focus}`}
                value={codigoReset}
                onChange={(e) => setCodigoReset(e.target.value.replace(/\D/g, "").slice(0, 6))}
                required
              />
            </div>
          ) : (
            <div className="relative">
              <Lock
                className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500"
                size={18}
              />
              <input
                type="password"
                placeholder="Senha"
                className={`w-full rounded-xl border border-zinc-700 bg-black/50 p-4 pl-12 text-white outline-none transition ${accent.focus}`}
                value={senha}
                onChange={(e) => setSenha(e.target.value)}
                required
              />
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className={`flex w-full justify-center gap-2 rounded-xl py-4 font-black uppercase text-white shadow-lg transition active:scale-95 disabled:cursor-not-allowed disabled:opacity-70 ${accent.button}`}
          >
            {loading ? <Loader2 className="animate-spin" /> : resetMode ? "Confirmar código" : "Acessar Painel"}
          </button>
        </form>

        <div className="mt-6 border-t border-zinc-800 pt-6 text-center">
          <Link
            href={companyRegisterPath}
            className={`text-sm font-bold uppercase tracking-wide hover:underline ${accent.link}`}
          >
            Quero me Cadastrar
          </Link>
          <p className="mt-4 text-[11px] leading-relaxed text-zinc-500">
            Esqueceu a senha? Entre em contato com o{" "}
            <Link href={supportPath} className={`font-bold hover:underline ${accent.link}`}>
              canal de suporte
            </Link>{" "}
            para solicitar um código de reset. Depois, tente entrar com o mesmo e-mail e informe o
            código recebido.
          </p>
        </div>
      </div>
    </div>
  );
}
