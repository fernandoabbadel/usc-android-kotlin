"use client";

import React, { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { ArrowLeft, Eye, EyeOff, FileText, Loader2, Save, ShieldAlert } from "lucide-react";

import { useAuth } from "@/context/AuthContext";
import { useTenantTheme } from "@/context/TenantThemeContext";
import { useToast } from "@/context/ToastContext";
import {
  fetchTenantPolicyDocuments,
  saveTenantPolicyDocuments,
  type TenantPolicyDocument,
  type TenantPolicyModule,
} from "@/lib/tenantPoliciesService";
import { withTenantSlug } from "@/lib/tenantRouting";

const policyTemplates: Array<{
  module: TenantPolicyModule;
  title: string;
  description: string;
  placeholder: string;
}> = [
  {
    module: "reembolso_cancelamento",
    title: "Reembolso e cancelamento",
    description: "Evento cancelado, evento adiado, chargeback, pedido aprovado por erro e regras gerais.",
    placeholder:
      "Ex.: em caso de cancelamento do evento, os compradores serão informados pelos canais oficiais. Descreva prazo, forma de reembolso, taxas não reembolsáveis quando aplicável e responsável pelo atendimento.",
  },
  {
    module: "eventos",
    title: "Eventos, ingressos e check-in",
    description: "Transferência de ingresso, QR Code, entrada, lote, consumíveis, pulseira e validação.",
    placeholder:
      "Ex.: o ingresso é pessoal, pode ser transferido até determinada data e exige QR Code válido no check-in. Descreva regras para ficha/consumível já retirado.",
  },
  {
    module: "bebidas_alcoolicas",
    title: "Eventos com bebida alcoólica",
    description: "Idade mínima, documento oficial, consumo responsável e responsabilidade do organizador.",
    placeholder:
      "Ex.: quando houver bebida alcoólica, a entrada/consumo será restrita a maiores de 18 anos mediante documento oficial com foto.",
  },
  {
    module: "menores_de_idade",
    title: "Menores de idade",
    description: "Compra de evento/produto por menores, autorização responsável e bloqueios sociais.",
    placeholder:
      "Ex.: menores de 18 anos dependem de autorização responsável quando aplicável e podem ter recursos sociais limitados conforme a política da USC e do organizador.",
  },
  {
    module: "loja",
    title: "Loja, produtos, retirada e entrega",
    description: "Produto esgotado, produto personalizado, retirada, entrega e troca.",
    placeholder:
      "Ex.: produtos personalizados podem ter regras específicas de troca. Informe prazos de retirada, entrega e contato de suporte.",
  },
  {
    module: "planos",
    title: "Planos de sócio e benefícios",
    description: "Cancelamento, benefício já usado, período de cobrança e elegibilidade.",
    placeholder:
      "Ex.: benefícios já utilizados podem não gerar reembolso integral. Informe como solicitar cancelamento e prazos de resposta.",
  },
  {
    module: "mini_vendor",
    title: "Mini vendor e repasses",
    description: "Responsabilidade do vendedor, produtos permitidos, repasse e contestação.",
    placeholder:
      "Ex.: o vendedor é responsável por descrição, disponibilidade e entrega do produto. Descreva prazos de repasse e regras de contestação.",
  },
  {
    module: "checkout",
    title: "Checkout e pagamento",
    description: "Comprovante, aprovação manual, PIX, erro operacional e contestação.",
    placeholder:
      "Ex.: pagamentos manuais dependem de validação do comprovante. Descreva prazo de análise e o que acontece em caso de comprovante inválido.",
  },
  {
    module: "termos_tenant",
    title: "Termos do tenant/organizador",
    description: "Regras próprias da organização, uso de dados, sigilo e responsabilidade.",
    placeholder:
      "Ex.: descreva regras específicas da entidade, responsáveis oficiais, canais de suporte e obrigações dos administradores.",
  },
];

const makeInitialPolicy = (template: typeof policyTemplates[number]): TenantPolicyDocument => ({
  module: template.module,
  title: template.title,
  content: "",
  visible: false,
});

export default function AdminPoliticasPage() {
  const { loading: authLoading } = useAuth();
  const { tenantId, tenantSlug, tenantName, tenantSigla } = useTenantTheme();
  const { addToast } = useToast();
  const [policies, setPolicies] = useState<TenantPolicyDocument[]>(
    policyTemplates.map(makeInitialPolicy)
  );
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const adminHref = tenantSlug ? withTenantSlug(tenantSlug, "/admin") : "/admin";
  const tenantLabel = tenantSigla || tenantName || "tenant atual";

  const policyMap = useMemo(
    () => new Map(policies.map((policy) => [policy.module, policy])),
    [policies]
  );

  useEffect(() => {
    let mounted = true;

    if (authLoading) return;
    if (!tenantId) {
      setLoading(false);
      return;
    }

    setLoading(true);
    fetchTenantPolicyDocuments(tenantId)
      .then((loadedPolicies) => {
        if (!mounted) return;
        const loadedMap = new Map(loadedPolicies.map((policy) => [policy.module, policy]));
        setPolicies(
          policyTemplates.map((template) => ({
            ...makeInitialPolicy(template),
            ...(loadedMap.get(template.module) || {}),
          }))
        );
      })
      .catch((error: unknown) => {
        console.error(error);
        if (mounted) addToast(error instanceof Error ? error.message : "Erro ao carregar políticas.", "error");
      })
      .finally(() => {
        if (mounted) setLoading(false);
      });

    return () => {
      mounted = false;
    };
  }, [addToast, authLoading, tenantId]);

  const updatePolicy = (
    module: TenantPolicyModule,
    patch: Partial<TenantPolicyDocument>
  ) => {
    setPolicies((current) =>
      current.map((policy) =>
        policy.module === module
          ? {
              ...policy,
              ...patch,
              visible:
                typeof patch.visible === "boolean"
                  ? patch.visible
                  : policy.visible,
            }
          : policy
      )
    );
  };

  const save = async () => {
    if (!tenantId) {
      addToast("Tenant não identificado.", "error");
      return;
    }
    try {
      setSaving(true);
      const payload = policies.map((policy) => ({
        ...policy,
        visible: Boolean(policy.visible && policy.content.trim()),
      }));
      const saved = await saveTenantPolicyDocuments(tenantId, payload);
      const savedMap = new Map(saved.map((policy) => [policy.module, policy]));
      setPolicies(
        policyTemplates.map((template) => ({
          ...makeInitialPolicy(template),
          ...(savedMap.get(template.module) || {}),
        }))
      );
      addToast("Políticas salvas.", "success");
    } catch (error: unknown) {
      console.error(error);
      addToast(error instanceof Error ? error.message : "Erro ao salvar políticas.", "error");
    } finally {
      setSaving(false);
    }
  };

  if (authLoading || loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#050505] text-white">
        <Loader2 className="animate-spin text-blue-400" size={34} />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#050505] px-4 py-8 text-white">
      <main className="mx-auto max-w-5xl">
        <Link href={adminHref} className="mb-6 inline-flex items-center gap-2 text-sm font-bold text-zinc-300 transition hover:text-white">
          <ArrowLeft size={16} />
          Voltar ao admin
        </Link>

        <section className="rounded-3xl border border-blue-400/20 bg-blue-500/10 p-5 sm:p-7">
          <div className="mb-4 inline-flex items-center gap-2 rounded-xl border border-white/10 bg-white/[0.06] px-3 py-2 text-xs font-black uppercase tracking-wide text-blue-100">
            <FileText size={16} />
            Governança do tenant
          </div>
          <h1 className="text-3xl font-black uppercase italic tracking-tight sm:text-4xl">
            Políticas públicas
          </h1>
          <p className="mt-4 max-w-3xl text-sm font-semibold leading-7 text-zinc-300">
            Configure regras específicas de {tenantLabel}. Uma política só deve aparecer ao público quando estiver preenchida e marcada como visível.
          </p>
          <div className="mt-4 rounded-2xl border border-amber-400/20 bg-amber-400/10 p-4 text-xs font-semibold leading-6 text-amber-100">
            <ShieldAlert size={16} className="mb-2" />
            A USC continua tendo termos globais. Este espaço cobre regras operacionais do tenant/organizador, como cancelamento, reembolso, menores de idade, bebida alcoólica e retirada/entrega.
          </div>
        </section>

        {!tenantId ? (
          <div className="mt-6 rounded-2xl border border-red-500/20 bg-red-500/10 p-5 text-sm font-semibold text-red-100">
            Tenant não identificado. Entre pelo caminho do tenant para editar estas políticas.
          </div>
        ) : (
          <>
            <div className="mt-6 grid gap-4">
              {policyTemplates.map((template) => {
                const policy = policyMap.get(template.module) || makeInitialPolicy(template);
                const canBeVisible = policy.content.trim().length > 0;
                return (
                  <section key={template.module} className="rounded-2xl border border-zinc-800 bg-zinc-950 p-4 sm:p-5">
                    <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                      <div>
                        <h2 className="text-lg font-black uppercase text-white">{template.title}</h2>
                        <p className="mt-1 text-xs font-semibold leading-6 text-zinc-500">{template.description}</p>
                      </div>
                      <button
                        type="button"
                        onClick={() => updatePolicy(template.module, { visible: !policy.visible })}
                        disabled={!canBeVisible && !policy.visible}
                        className={`inline-flex items-center justify-center gap-2 rounded-xl border px-3 py-2 text-[10px] font-black uppercase tracking-wide transition disabled:cursor-not-allowed disabled:opacity-50 ${
                          policy.visible && canBeVisible
                            ? "border-emerald-400/40 bg-emerald-500/10 text-emerald-200"
                            : "border-zinc-700 bg-zinc-900 text-zinc-400"
                        }`}
                      >
                        {policy.visible && canBeVisible ? <Eye size={14} /> : <EyeOff size={14} />}
                        {policy.visible && canBeVisible ? "Visível" : "Oculta"}
                      </button>
                    </div>

                    <textarea
                      value={policy.content}
                      onChange={(event) => updatePolicy(template.module, { content: event.target.value })}
                      placeholder={template.placeholder}
                      className="mt-4 min-h-36 w-full rounded-2xl border border-zinc-800 bg-black px-4 py-3 text-sm font-semibold leading-7 text-white outline-none transition placeholder:text-zinc-700 focus:border-blue-400"
                      maxLength={12000}
                    />
                  </section>
                );
              })}
            </div>

            <div className="sticky bottom-4 mt-6 rounded-2xl border border-zinc-800 bg-zinc-950/95 p-3 backdrop-blur-xl">
              <button
                type="button"
                onClick={() => void save()}
                disabled={saving}
                className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-blue-600 px-5 py-4 text-xs font-black uppercase tracking-wide text-white transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {saving ? <Loader2 size={18} className="animate-spin" /> : <Save size={18} />}
                Salvar políticas
              </button>
            </div>
          </>
        )}
      </main>
    </div>
  );
}
