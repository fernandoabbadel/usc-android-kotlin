"use client";

import React, { useEffect, useState } from "react";
import {
  ArrowLeft,
  Building2,
  CheckCircle,
  ChevronRight,
  CreditCard,
  Crown,
  FileText,
  Loader2,
  Lock,
  Mail,
  Phone,
  Shield,
  Star,
  Store,
  Tag,
  User,
} from "lucide-react";
import Image from "next/image";
import { usePathname, useRouter, useSearchParams } from "next/navigation";

import { useAuth } from "../../../context/AuthContext";
import { useToast } from "../../../context/ToastContext";
import { PLATFORM_BRAND_NAME, PLATFORM_LOGO_URL } from "@/constants/platformBrand";
import { useTenantTheme } from "@/context/TenantThemeContext";
import { createPartnerLead, resetPartnerPasswordWithCode } from "../../../lib/partnersService";
import { parseTenantScopedPath, withTenantSlug } from "@/lib/tenantRouting";

const PLANOS = [
  {
    id: "ouro",
    nome: "Ouro",
    valor: "R$ 500",
    icon: Crown,
    color: "text-yellow-500",
    border: "border-yellow-500/50",
    bg: "bg-yellow-500/10",
  },
  {
    id: "prata",
    nome: "Prata",
    valor: "R$ 250",
    icon: Star,
    color: "text-zinc-300",
    border: "border-zinc-500/50",
    bg: "bg-zinc-500/10",
  },
  {
    id: "standard",
    nome: "Standard",
    valor: "Grátis",
    icon: Shield,
    color: "text-emerald-500",
    border: "border-emerald-500/50",
    bg: "bg-emerald-500/10",
  },
];

type PublicTenantOption = {
  id: string;
  nome: string;
  sigla: string;
  slug: string;
  status: string;
};

const keepDigits = (value: string, maxDigits: number): string =>
  value.replace(/\D/g, "").slice(0, maxDigits);

const asObject = (value: unknown): Record<string, unknown> | null =>
  typeof value === "object" && value !== null ? (value as Record<string, unknown>) : null;

const asString = (value: unknown): string => (typeof value === "string" ? value.trim() : "");

const normalizeTenantOption = (value: unknown): PublicTenantOption | null => {
  const row = asObject(value);
  if (!row) return null;

  const id = asString(row.id);
  const slug = asString(row.slug).toLowerCase();
  if (!id || !slug) return null;

  return {
    id,
    nome: asString(row.nome),
    sigla: asString(row.sigla),
    slug,
    status: asString(row.status) || "active",
  };
};

const formatCnpjInput = (value: string): string => {
  const digits = keepDigits(value, 14);
  if (!digits) return "";
  if (digits.length <= 2) return digits;
  if (digits.length <= 5) return `${digits.slice(0, 2)}.${digits.slice(2)}`;
  if (digits.length <= 8) return `${digits.slice(0, 2)}.${digits.slice(2, 5)}.${digits.slice(5)}`;
  if (digits.length <= 12) {
    return `${digits.slice(0, 2)}.${digits.slice(2, 5)}.${digits.slice(5, 8)}/${digits.slice(8)}`;
  }
  return `${digits.slice(0, 2)}.${digits.slice(2, 5)}.${digits.slice(5, 8)}/${digits.slice(8, 12)}-${digits.slice(12)}`;
};

const formatCpfInput = (value: string): string => {
  const digits = keepDigits(value, 11);
  if (!digits) return "";
  if (digits.length <= 3) return digits;
  if (digits.length <= 6) return `${digits.slice(0, 3)}.${digits.slice(3)}`;
  if (digits.length <= 9) return `${digits.slice(0, 3)}.${digits.slice(3, 6)}.${digits.slice(6)}`;
  return `${digits.slice(0, 3)}.${digits.slice(3, 6)}.${digits.slice(6, 9)}-${digits.slice(9)}`;
};

const formatPhoneInput = (value: string): string => {
  const digits = keepDigits(value, 13);
  if (!digits) return "";
  if (digits.length <= 2) return digits;

  const country = digits.slice(0, 2);
  if (digits.length <= 4) return `${country} (${digits.slice(2)}`;

  const ddd = digits.slice(2, 4);
  const number = digits.slice(4);
  if (!number) return `${country} (${ddd})`;
  if (number.length <= 4) return `${country} (${ddd}) ${number}`;
  if (number.length <= 8) return `${country} (${ddd}) ${number.slice(0, 4)}-${number.slice(4)}`;
  return `${country} (${ddd}) ${number.slice(0, 5)}-${number.slice(5, 9)}`;
};

export default function CompanyRegisterPage() {
  const router = useRouter();
  const pathname = usePathname() || "/empresa/cadastro";
  const searchParams = useSearchParams();
  const { addToast } = useToast();
  const { user } = useAuth();
  const { tenantId, tenantLogoUrl, tenantName } = useTenantTheme();

  const pathInfo = parseTenantScopedPath(pathname);
  const isTenantScopedPartnerPage = Boolean(pathInfo.tenantSlug);
  const companyLoginPath = isTenantScopedPartnerPage
    ? withTenantSlug(pathInfo.tenantSlug, "/empresa")
    : "/empresa";
  const landingPath = isTenantScopedPartnerPage ? `/${pathInfo.tenantSlug}` : "/";
  const displayLogoUrl = isTenantScopedPartnerPage ? tenantLogoUrl : PLATFORM_LOGO_URL;
  const displayBrandName = isTenantScopedPartnerPage
    ? tenantName || "Atlética"
    : PLATFORM_BRAND_NAME;
  const accent = isTenantScopedPartnerPage
    ? {
        orb: "bg-emerald-600/15",
        glow: "bg-emerald-500/30 group-hover:bg-emerald-500/50",
        progress: "bg-emerald-500",
        text: "text-emerald-500",
        button: "bg-emerald-600 hover:bg-emerald-500",
        panel: "border-emerald-500/20 bg-emerald-500/10",
      }
    : {
        orb: "bg-blue-600/15",
        glow: "bg-blue-500/30 group-hover:bg-blue-500/50",
        progress: "bg-blue-500",
        text: "text-blue-400",
        button: "bg-blue-600 hover:bg-blue-500",
        panel: "border-blue-500/20 bg-blue-500/10",
      };
  const rootStyle = {
    "--partner-accent": isTenantScopedPartnerPage ? "#10b981" : "#3b82f6",
  } as React.CSSProperties;

  const resetEmail = searchParams.get("resetEmail")?.trim().toLowerCase() || "";
  const resetCode = searchParams.get("resetCode")?.trim() || "";
  const isPasswordResetMode = Boolean(resetEmail && resetCode);

  const [step, setStep] = useState(1);
  const [isLoading, setIsLoading] = useState(false);
  const [tenantsLoading, setTenantsLoading] = useState(false);
  const [selectedPlan, setSelectedPlan] = useState("");
  const [tenantOptions, setTenantOptions] = useState<PublicTenantOption[]>([]);
  const [selectedTenantId, setSelectedTenantId] = useState("");
  const [formData, setFormData] = useState({
    nome: "",
    cnpj: "",
    responsavel: "",
    cpf: "",
    categoria: "Alimentação",
    email: "",
    telefone: "",
    senha: "",
    confirmSenha: "",
    descricao: "",
    endereco: "",
    horario: "",
  });

  const targetTenantId = isTenantScopedPartnerPage ? tenantId : selectedTenantId;

  const handleMaskedInputChange = (
    field: "cnpj" | "cpf" | "telefone",
    event: React.ChangeEvent<HTMLInputElement>
  ) => {
    const rawValue = event.target.value;
    const formatted =
      field === "cnpj"
        ? formatCnpjInput(rawValue)
        : field === "cpf"
          ? formatCpfInput(rawValue)
          : formatPhoneInput(rawValue);

    setFormData((prev) => ({ ...prev, [field]: formatted }));
  };

  useEffect(() => {
    if (user && user.role === "partner") {
      addToast("Você já está logado!", "info");
      router.push(companyLoginPath);
    }
  }, [addToast, companyLoginPath, router, user]);

  useEffect(() => {
    if (isTenantScopedPartnerPage || isPasswordResetMode) return;

    let mounted = true;
    setTenantsLoading(true);

    fetch("/api/public/tenants?limit=60&refresh=1", {
      cache: "no-store",
      headers: { Accept: "application/json" },
    })
      .then(async (response) => {
        if (!response.ok) throw new Error("Falha ao carregar atléticas.");
        const payload = (await response.json()) as unknown;
        const options = (Array.isArray(payload) ? payload : [])
          .map((entry) => normalizeTenantOption(entry))
          .filter((entry): entry is PublicTenantOption => Boolean(entry))
          .filter((entry) => entry.status === "active");

        if (!mounted) return;
        setTenantOptions(options);
        setSelectedTenantId((current) =>
          options.some((entry) => entry.id === current) ? current : options[0]?.id || ""
        );
      })
      .catch((error: unknown) => {
        console.error("Erro ao carregar atléticas para parceria:", error);
        if (mounted) addToast("Não foi possível carregar as atléticas ativas.", "error");
      })
      .finally(() => {
        if (mounted) setTenantsLoading(false);
      });

    return () => {
      mounted = false;
    };
  }, [addToast, isPasswordResetMode, isTenantScopedPartnerPage]);

  const handleBack = (e: React.MouseEvent) => {
    e.preventDefault();
    if (user) {
      router.push(
        isTenantScopedPartnerPage ? withTenantSlug(pathInfo.tenantSlug, "/parceiros") : "/parceiros"
      );
      return;
    }

    router.push(landingPath);
  };

  const handleSelectPlan = (planId: string) => {
    setSelectedPlan(planId);
    setStep(2);
  };

  const validateStep2 = () => {
    const cleanCNPJ = formData.cnpj.replace(/\D/g, "");
    const cleanCPF = formData.cpf.replace(/\D/g, "");
    const cleanPhone = formData.telefone.replace(/\D/g, "");

    if (!isTenantScopedPartnerPage && !selectedTenantId) return "Escolha a atlética da parceria.";
    if (!formData.nome) return "Nome fantasia é obrigatório.";
    if (cleanCNPJ.length !== 14) return "CNPJ inválido (14 dígitos).";
    if (!formData.responsavel) return "Nome do responsável é obrigatório.";
    if (cleanCPF.length !== 11) return "CPF inválido (11 dígitos).";

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(formData.email) || !formData.email.includes(".com")) {
      return "Email inválido.";
    }

    if (cleanPhone.length < 12 || cleanPhone.length > 13) {
      return "Telefone inválido (use 55 + DDD + número).";
    }

    if (formData.senha.length < 8) return "A senha deve ter no mínimo 8 caracteres.";
    if (formData.senha !== formData.confirmSenha) return "As senhas não conferem.";

    return null;
  };

  const handleNextStep = (e: React.FormEvent) => {
    e.preventDefault();
    const error = validateStep2();
    if (error) return addToast(error, "error");

    setIsLoading(true);
    setTimeout(() => {
      setIsLoading(false);
      setStep(3);
      addToast("Dados válidos! Configure o perfil.", "success");
    }, 1000);
  };

  const handleFinalSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);

    try {
      if (!targetTenantId) {
        addToast("Escolha a atlética da parceria antes de finalizar.", "error");
        setIsLoading(false);
        return;
      }

      await createPartnerLead({
        nome: formData.nome,
        cnpj: formData.cnpj,
        responsavel: formData.responsavel,
        cpf: formData.cpf,
        categoria: formData.categoria,
        email: formData.email,
        telefone: formData.telefone,
        senha: formData.senha,
        descricao: formData.descricao,
        endereco: formData.endereco,
        horario: formData.horario,
        tier: selectedPlan,
        tenantId: targetTenantId,
      });

      addToast("Cadastro enviado para aprovação!", "success");
      setTimeout(() => router.push(companyLoginPath), 1500);
    } catch (err: unknown) {
      console.error(err);
      addToast("Erro ao salvar cadastro.", "error");
    } finally {
      setIsLoading(false);
    }
  };

  const handlePasswordResetSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (formData.senha.length < 8) {
      addToast("A senha deve ter no mínimo 8 caracteres.", "error");
      return;
    }
    if (formData.senha !== formData.confirmSenha) {
      addToast("As senhas não conferem.", "error");
      return;
    }
    if (!tenantId) {
      addToast("Atlética não identificada para redefinir a senha.", "error");
      return;
    }

    setIsLoading(true);
    try {
      await resetPartnerPasswordWithCode({
        email: resetEmail,
        code: resetCode,
        senha: formData.senha,
        tenantId,
      });
      addToast("Senha redefinida com sucesso.", "success");
      router.push(companyLoginPath);
    } catch (err: unknown) {
      console.error(err);
      addToast(err instanceof Error ? err.message : "Erro ao redefinir senha.", "error");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div
      className="relative flex min-h-screen items-center justify-center overflow-hidden bg-[#050505] p-6 font-sans"
      style={rootStyle}
    >
      <div
        className={`pointer-events-none absolute left-[-20%] top-[-20%] h-[60%] w-[60%] animate-pulse-slow rounded-full ${accent.orb} blur-[120px]`}
      />
      <div className="pointer-events-none absolute bottom-[-20%] right-[-20%] h-[60%] w-[60%] rounded-full bg-blue-600/10 blur-[120px]" />

      <button
        onClick={handleBack}
        className="absolute left-6 top-6 z-50 flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-zinc-500 transition hover:text-white"
      >
        <ArrowLeft size={18} /> Voltar
      </button>

      <div className="relative z-10 my-10 w-full max-w-lg rounded-[2rem] border border-zinc-800/80 bg-zinc-900/60 p-8 shadow-2xl backdrop-blur-xl">
        <div className="mb-8 text-center">
          <div className="group relative mx-auto mb-4 h-24 w-24 animate-float-slow">
            <div
              className={`absolute inset-0 rounded-full ${accent.glow} blur-xl transition duration-500`}
            />
            <Image
              src={displayLogoUrl || "/logo.png"}
              alt={`Logo ${displayBrandName}`}
              fill
              sizes="96px"
              className="relative z-10 object-contain drop-shadow-2xl transition group-hover:scale-105"
              unoptimized={Boolean(displayLogoUrl && displayLogoUrl.startsWith("http"))}
              priority
            />
          </div>
          <h1 className="text-2xl font-black uppercase tracking-tighter text-white">
            Parceria oficial
          </h1>

          {!isPasswordResetMode && (
            <>
              <div className="mt-4 flex items-center justify-center gap-2 text-[10px] font-bold uppercase tracking-widest text-zinc-600">
                <span className={step >= 1 ? accent.text : ""}>1. Planos</span>
                <ChevronRight size={10} />
                <span className={step >= 2 ? accent.text : ""}>2. Dados</span>
                <ChevronRight size={10} />
                <span className={step >= 3 ? accent.text : ""}>3. Perfil</span>
              </div>
              <div className="mt-4 h-1 w-full overflow-hidden rounded-full bg-zinc-800">
                <div
                  className={`h-full ${accent.progress} transition-all duration-500 ease-out`}
                  style={{ width: step === 1 ? "33%" : step === 2 ? "66%" : "100%" }}
                />
              </div>
            </>
          )}
        </div>

        {isPasswordResetMode && (
          <form onSubmit={handlePasswordResetSubmit} className="animate-in space-y-4 slide-in-from-right duration-300">
            <div className={`rounded-2xl border p-4 ${accent.panel}`}>
              <h3 className="text-sm font-black uppercase text-white">Redefinir senha do parceiro</h3>
              <p className="mt-2 text-xs leading-relaxed text-zinc-300">
                Código confirmado para {resetEmail}. Cadastre uma nova senha para voltar ao painel
                do parceiro.
              </p>
            </div>
            <div className="grid grid-cols-1 gap-4 pt-2 md:grid-cols-2">
              <div className="relative">
                <Lock className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500" size={18} />
                <input
                  type="password"
                  placeholder="Nova senha (mín. 8)"
                  className="input-field pl-14"
                  value={formData.senha}
                  onChange={(e) => setFormData({ ...formData, senha: e.target.value })}
                />
              </div>
              <div className="relative">
                <Lock className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500" size={18} />
                <input
                  type="password"
                  placeholder="Confirmar nova senha"
                  className="input-field pl-14"
                  value={formData.confirmSenha}
                  onChange={(e) => setFormData({ ...formData, confirmSenha: e.target.value })}
                />
              </div>
            </div>
            <button
              type="submit"
              disabled={isLoading}
              className={`mt-6 flex w-full items-center justify-center gap-2 rounded-xl py-4 font-black uppercase text-white shadow-lg transition active:scale-95 disabled:cursor-not-allowed disabled:opacity-70 ${accent.button}`}
            >
              {isLoading ? <Loader2 className="animate-spin" /> : <>Salvar nova senha <CheckCircle size={18} /></>}
            </button>
          </form>
        )}

        {!isPasswordResetMode && step === 1 && (
          <div className="animate-in space-y-4 slide-in-from-right duration-300">
            <h3 className="mb-4 text-center text-sm font-bold uppercase text-white">
              Escolha seu Plano
            </h3>
            <div className="space-y-3">
              {PLANOS.map((plano) => (
                <button
                  type="button"
                  key={plano.id}
                  onClick={() => handleSelectPlan(plano.id)}
                  className={`flex w-full cursor-pointer items-center justify-between rounded-2xl border p-4 text-left transition hover:scale-[1.02] ${plano.bg} ${plano.border}`}
                >
                  <div className="flex items-center gap-3">
                    <div className={`rounded-full bg-black/20 p-2 ${plano.color}`}>
                      <plano.icon size={20} />
                    </div>
                    <div>
                      <h4 className={`text-sm font-black uppercase ${plano.color}`}>
                        {plano.nome}
                      </h4>
                      <span className="text-[10px] text-zinc-400">Benefícios exclusivos</span>
                    </div>
                  </div>
                  <div className="text-right">
                    <span className="block font-black text-white">{plano.valor}</span>
                    <span className="rounded bg-black/40 px-2 py-1 text-[9px] uppercase text-zinc-500">
                      Selecionar
                    </span>
                  </div>
                </button>
              ))}
            </div>
          </div>
        )}

        {!isPasswordResetMode && step === 2 && (
          <form onSubmit={handleNextStep} className="animate-in space-y-4 slide-in-from-right duration-300">
            {!isTenantScopedPartnerPage && (
              <div className="relative">
                <Building2 className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500" size={18} />
                <select
                  className="input-field appearance-none pl-14 text-zinc-300"
                  value={selectedTenantId}
                  disabled={tenantsLoading || tenantOptions.length === 0}
                  onChange={(e) => setSelectedTenantId(e.target.value)}
                >
                  <option value="">
                    {tenantsLoading ? "Carregando atléticas..." : "Escolha a atlética"}
                  </option>
                  {tenantOptions.map((tenant) => (
                    <option key={tenant.id} value={tenant.id}>
                      {(tenant.sigla || tenant.nome || tenant.slug).toUpperCase()} -{" "}
                      {tenant.nome || tenant.slug}
                    </option>
                  ))}
                </select>
              </div>
            )}

            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              <div className="relative">
                <Store className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500" size={18} />
                <input
                  type="text"
                  placeholder="Nome Fantasia"
                  className="input-field pl-14"
                  value={formData.nome}
                  onChange={(e) => setFormData({ ...formData, nome: e.target.value })}
                />
              </div>
              <div className="relative">
                <FileText className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500" size={18} />
                <input
                  type="text"
                  inputMode="numeric"
                  placeholder="CNPJ (14 dígitos)"
                  maxLength={18}
                  className="input-field pl-14"
                  value={formData.cnpj}
                  onChange={(e) => handleMaskedInputChange("cnpj", e)}
                />
              </div>
            </div>

            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              <div className="relative">
                <User className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500" size={18} />
                <input
                  type="text"
                  placeholder="Nome Responsável"
                  className="input-field pl-14"
                  value={formData.responsavel}
                  onChange={(e) => setFormData({ ...formData, responsavel: e.target.value })}
                />
              </div>
              <div className="relative">
                <CreditCard className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500" size={18} />
                <input
                  type="text"
                  inputMode="numeric"
                  placeholder="CPF Responsável"
                  maxLength={14}
                  className="input-field pl-14"
                  value={formData.cpf}
                  onChange={(e) => handleMaskedInputChange("cpf", e)}
                />
              </div>
            </div>

            <div className="relative">
              <Tag className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500" size={18} />
              <select
                className="input-field appearance-none pl-14 text-zinc-300"
                value={formData.categoria}
                onChange={(e) => setFormData({ ...formData, categoria: e.target.value })}
              >
                <option>Alimentação</option>
                <option>Saúde</option>
                <option>Lazer</option>
                <option>Serviços</option>
                <option>Vestuário</option>
              </select>
            </div>

            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              <div className="relative">
                <Mail className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500" size={18} />
                <input
                  type="email"
                  placeholder="Email Comercial"
                  className="input-field pl-14"
                  value={formData.email}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                />
              </div>
              <div className="relative">
                <Phone className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500" size={18} />
                <input
                  type="tel"
                  inputMode="numeric"
                  placeholder="WhatsApp (55 + DDD + número)"
                  maxLength={18}
                  className="input-field pl-14"
                  value={formData.telefone}
                  onChange={(e) => handleMaskedInputChange("telefone", e)}
                />
              </div>
            </div>

            <div className="grid grid-cols-1 gap-4 pt-2 md:grid-cols-2">
              <div className="relative">
                <Lock className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500" size={18} />
                <input
                  type="password"
                  placeholder="Senha (mín. 8)"
                  className="input-field pl-14"
                  value={formData.senha}
                  onChange={(e) => setFormData({ ...formData, senha: e.target.value })}
                />
              </div>
              <div className="relative">
                <Lock className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-zinc-500" size={18} />
                <input
                  type="password"
                  placeholder="Confirmar"
                  className="input-field pl-14"
                  value={formData.confirmSenha}
                  onChange={(e) => setFormData({ ...formData, confirmSenha: e.target.value })}
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className={`mt-6 flex w-full items-center justify-center gap-2 rounded-xl py-4 font-black uppercase text-white shadow-lg transition active:scale-95 disabled:cursor-not-allowed disabled:opacity-70 ${accent.button}`}
            >
              {isLoading ? <Loader2 className="animate-spin" /> : <>Próximo Passo <ChevronRight size={18} /></>}
            </button>
          </form>
        )}

        {!isPasswordResetMode && step === 3 && (
          <form onSubmit={handleFinalSubmit} className="animate-in space-y-4 slide-in-from-right duration-300">
            <div className="mb-4 rounded-xl border border-zinc-700/50 bg-zinc-800/50 p-4 text-center">
              <p className="text-xs text-zinc-400">Descreva sua empresa para aprovação.</p>
              <span className={`mt-1 block text-[10px] font-bold uppercase ${accent.text}`}>
                Plano Selecionado: {PLANOS.find((plan) => plan.id === selectedPlan)?.nome}
              </span>
            </div>

            <textarea
              placeholder="Descreva sua empresa e benefícios..."
              rows={3}
              className="input-field px-4 pt-3"
              value={formData.descricao}
              onChange={(e) => setFormData({ ...formData, descricao: e.target.value })}
            />
            <input
              type="text"
              placeholder="Endereço Completo"
              className="input-field px-4"
              value={formData.endereco}
              onChange={(e) => setFormData({ ...formData, endereco: e.target.value })}
            />
            <input
              type="text"
              placeholder="Horário de Funcionamento"
              className="input-field px-4"
              value={formData.horario}
              onChange={(e) => setFormData({ ...formData, horario: e.target.value })}
            />

            <div className="mt-2 text-center text-xs text-zinc-500">
              * Logos e capas poderão ser adicionados após a aprovação no painel.
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className={`mt-6 flex w-full items-center justify-center gap-2 rounded-xl py-4 font-black uppercase text-white shadow-lg transition active:scale-95 disabled:cursor-not-allowed disabled:opacity-70 ${accent.button}`}
            >
              {isLoading ? <Loader2 className="animate-spin" /> : <>Finalizar Cadastro <CheckCircle size={18} /></>}
            </button>
          </form>
        )}
      </div>

      <style jsx>{`
        .input-field {
          width: 100%;
          background-color: rgba(0, 0, 0, 0.4);
          border: 1px solid #27272a;
          border-radius: 0.75rem;
          padding: 1rem;
          padding-left: 3.5rem;
          color: white;
          outline: none;
          transition: all 0.3s;
          font-size: 0.875rem;
        }
        .input-field:focus {
          border-color: var(--partner-accent);
          background-color: rgba(0, 0, 0, 0.8);
        }
        .input-field option {
          color: #111827;
          background: #ffffff;
        }
        .animate-float-slow {
          animation: float 6s ease-in-out infinite;
        }
        @keyframes float {
          0%,
          100% {
            transform: translateY(0);
          }
          50% {
            transform: translateY(-10px);
          }
        }
      `}</style>
    </div>
  );
}
