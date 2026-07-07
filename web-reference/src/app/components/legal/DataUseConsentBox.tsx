"use client";

import { useEffect, useMemo, useState } from "react";
import { CheckCircle2, Loader2, ShieldCheck, X } from "lucide-react";

import { LEGAL_VERSION } from "@/components/legal/legalContent";
import { useAuth } from "@/context/AuthContext";
import { useTenantTheme } from "@/context/TenantThemeContext";
import { useToast } from "@/context/ToastContext";
import {
  type LegalAcceptanceDocument,
  type LegalAcceptanceSource,
  recordLegalAcceptance,
} from "@/lib/legalGovernanceService";
import { getSupabaseClient } from "@/lib/supabase";

export const DATA_USE_DOCUMENTS: Array<Omit<LegalAcceptanceDocument, "documentVersion">> = [
  { documentType: "privacy_policy" },
  { documentType: "lgpd_rights" },
];

const clean = (value: unknown): string =>
  typeof value === "string" ? value.trim() : "";

export type DataUseAuthorizationScope =
  | "tenant"
  | "liga"
  | "comissao"
  | "diretorio"
  | "mini_vendor";

type DataUseConsentAction = "management" | "request" | "creation" | "organogram" | "data_use";

const normalizeToken = (value: unknown): string =>
  clean(value)
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[\s-]+/g, "_");

const normalizeAuthorizationScope = (value: unknown): DataUseAuthorizationScope | "" => {
  const token = normalizeToken(value);
  if (!token) return "";
  if (["tenant", "organograma", "organogram", "admin"].includes(token)) {
    return "tenant";
  }
  if (["mini_vendor", "minivendor", "lojinha", "loja"].includes(token)) return "mini_vendor";
  if (["liga", "ligas", "league", "leagues"].includes(token)) return "liga";
  if (["comissao", "comissoes", "commission", "commissions"].includes(token)) return "comissao";
  if (["diretorio", "diretorios", "directory", "directories"].includes(token)) return "diretorio";
  return "";
};

export const resolveDataUseAuthorizationScope = (
  contextType: string,
  metadata?: Record<string, unknown>
): DataUseAuthorizationScope => {
  const explicitScope =
    normalizeAuthorizationScope(metadata?.authorizationScope) ||
    normalizeAuthorizationScope(metadata?.authorization_scope) ||
    normalizeAuthorizationScope(metadata?.scope);
  if (explicitScope) return explicitScope;

  const haystack = [
    contextType,
    metadata?.area,
    metadata?.category,
    metadata?.ownerType,
    metadata?.module,
  ]
    .map(normalizeToken)
    .join(" ");

  if (haystack.includes("mini_vendor") || haystack.includes("minivendor")) return "mini_vendor";
  if (haystack.includes("commission") || haystack.includes("comissao")) return "comissao";
  if (haystack.includes("directory") || haystack.includes("diretorio")) return "diretorio";
  if (haystack.includes("league") || haystack.includes("liga")) return "liga";
  return "tenant";
};

const resolveDataUseConsentAction = (contextType: string): DataUseConsentAction => {
  const token = normalizeToken(contextType);
  if (token.includes("management") || token.includes("gestao")) return "management";
  if (token.includes("member_request") || token.includes("request") || token.includes("solicit")) {
    return "request";
  }
  if (token.includes("mini_vendor") || token.includes("minivendor")) return "creation";
  if (token.includes("creation") || token.includes("cadastro")) return "creation";
  if (token.includes("organogram") || token.includes("organograma")) return "organogram";
  return "data_use";
};

const tenantResponsibleLabel = (tenantSigla?: string | null, tenantName?: string | null): string => {
  const label = clean(tenantSigla) || clean(tenantName) || "AAAKN";
  const normalized = normalizeToken(label);
  return normalized.includes("atletica") ? label : `Atlética ${label}`;
};

const responsibleLabel = (
  scope: DataUseAuthorizationScope,
  tenantSigla?: string | null,
  tenantName?: string | null
): string => {
  if (scope === "tenant") return tenantResponsibleLabel(tenantSigla, tenantName);
  if (scope === "liga") return "Liga";
  if (scope === "comissao") return "Comissão";
  if (scope === "diretorio") return "Diretoria";
  return "Mini Vendor";
};

const responsibleArticle = (scope: DataUseAuthorizationScope): string =>
  scope === "mini_vendor" ? "pelo" : "pela";

const contextNoun = (scope: DataUseAuthorizationScope): string => {
  if (scope === "tenant") return "organograma institucional";
  if (scope === "liga") return "liga";
  if (scope === "comissao") return "comissão";
  if (scope === "diretorio") return "diretoria";
  return "Mini Vendor";
};

const contextArticleNoun = (scope: DataUseAuthorizationScope): string => {
  if (scope === "tenant") return "no organograma institucional";
  if (scope === "mini_vendor") return "no Mini Vendor";
  return `na ${contextNoun(scope)}`;
};

const contextPossessiveNoun = (scope: DataUseAuthorizationScope): string => {
  if (scope === "tenant") return "da Atlética";
  if (scope === "mini_vendor") return "do Mini Vendor";
  return `da ${contextNoun(scope)}`;
};

const buildDataUseConsentCopy = ({
  contextType,
  metadata,
  tenantSigla,
  tenantName,
}: {
  contextType: string;
  metadata?: Record<string, unknown>;
  tenantSigla?: string | null;
  tenantName?: string | null;
}): {
  action: DataUseConsentAction;
  scope: DataUseAuthorizationScope;
  title: string;
  description: string;
  authorizationText: string;
  actionLabel: string;
  responsible: string;
} => {
  const scope = resolveDataUseAuthorizationScope(contextType, metadata);
  const action = resolveDataUseConsentAction(contextType);
  const responsible = responsibleLabel(scope, tenantSigla, tenantName);
  const article = responsibleArticle(scope);
  const articleNoun = contextArticleNoun(scope);
  const possessiveNoun = contextPossessiveNoun(scope);

  const titleByAction: Record<DataUseConsentAction, string> = {
    management: `Autorizar gestão ${possessiveNoun}`,
    request: `Autorizar participação ${articleNoun}`,
    creation: `Autorizar cadastro ${possessiveNoun}`,
    organogram: "Autorizar uso dos dados do organograma",
    data_use: `Autorizar uso dos dados ${possessiveNoun}`,
  };

  const descriptionByAction: Record<DataUseConsentAction, string> = {
    management: `Confirme a autorização para usar seus dados pessoais e de contato na gestão ${possessiveNoun}.`,
    request: `Confirme a autorização para enviar sua solicitação de participação ${articleNoun}.`,
    creation: `Confirme a autorização para usar seus dados pessoais e de contato no cadastro ${possessiveNoun}.`,
    organogram:
      "Confirme a autorização para usar seus dados pessoais e de contato no organograma institucional.",
    data_use: `Confirme a autorização para usar seus dados pessoais e de contato ${articleNoun}.`,
  };

  const actionLabelByAction: Record<DataUseConsentAction, string> = {
    management: "Autorizar e abrir gestão",
    request: "Autorizar e enviar solicitação",
    creation: "Autorizar e salvar cadastro",
    organogram: "Autorizar e continuar",
    data_use: "Autorizar uso dos dados",
  };

  const purpose =
    scope === "tenant"
      ? "cadastro no organograma institucional, comunicação institucional, organização de atividades internas, divulgação relacionada às funções exercidas na Atlética"
      : scope === "mini_vendor"
        ? "cadastro e operação do Mini Vendor, comunicação operacional, organização de produtos e pedidos, divulgação relacionada à lojinha"
        : `cadastro e participação ${articleNoun}, comunicação institucional, organização de atividades internas, divulgação relacionada às funções exercidas ${articleNoun}`;

  return {
    action,
    scope,
    title: titleByAction[action],
    description: descriptionByAction[action],
    actionLabel: actionLabelByAction[action],
    responsible,
    authorizationText: `Autorizo o uso dos meus dados pessoais e de contato ${article} ${responsible} para fins de ${purpose} e, quando necessário, cadastro financeiro para repasses vinculados às atividades ${possessiveNoun}. Estou ciente de que posso solicitar a atualização, correção ou exclusão dos meus dados.`,
  };
};

export const buildDataUseAuthorizationText = (
  tenantSigla?: string | null,
  contextType = "",
  metadata?: Record<string, unknown>,
  tenantName?: string | null
): string =>
  buildDataUseConsentCopy({ contextType, metadata, tenantSigla, tenantName }).authorizationText;

type DataUseConsentStatusPayload = {
  userId?: string | null;
  contextType: string;
  contextId: string;
  tenantId?: string | null;
  source?: LegalAcceptanceSource;
};

export const hasDataUseConsent = async ({
  userId,
  contextType,
  contextId,
  tenantId,
  source = "app",
}: DataUseConsentStatusPayload): Promise<boolean> => {
  const cleanUserId = clean(userId);
  const cleanContextType = clean(contextType);
  const cleanContextId = clean(contextId);
  const cleanTenantId = clean(tenantId);
  if (!cleanUserId || !cleanContextType || !cleanContextId) return false;

  const documentTypes = DATA_USE_DOCUMENTS.map((document) => document.documentType);
  const supabase = getSupabaseClient();
  let query = supabase
    .from("user_legal_acceptances")
    .select("document_type")
    .eq("user_id", cleanUserId)
    .eq("source", source)
    .eq("context_type", cleanContextType)
    .eq("context_id", cleanContextId)
    .eq("document_version", LEGAL_VERSION)
    .in("document_type", documentTypes);

  query = cleanTenantId ? query.eq("tenant_id", cleanTenantId) : query.is("tenant_id", null);

  const { data, error } = await query;
  if (error) throw error;

  const acceptedTypes = new Set(
    (Array.isArray(data) ? data : [])
      .map((row) => clean((row as { document_type?: unknown }).document_type))
      .filter(Boolean)
  );
  return documentTypes.every((documentType) => acceptedTypes.has(documentType));
};

export const recordDataUseConsent = async ({
  tenantId,
  source = "app",
  contextType,
  contextId,
  metadata,
  authorizationText,
  tenantSigla,
  tenantName,
}: {
  tenantId?: string | null;
  source?: LegalAcceptanceSource;
  contextType: string;
  contextId: string;
  metadata?: Record<string, unknown>;
  authorizationText: string;
  tenantSigla?: string | null;
  tenantName?: string | null;
}): Promise<void> => {
  const consentCopy = buildDataUseConsentCopy({ contextType, metadata, tenantSigla, tenantName });
  const authorizationScope = consentCopy.scope;

  await recordLegalAcceptance({
    tenantId: clean(tenantId) || null,
    source,
    readToEnd: true,
    markedRead: true,
    contextType: clean(contextType),
    contextId: clean(contextId),
    documents: DATA_USE_DOCUMENTS.map((document) => ({
      ...document,
      documentVersion: LEGAL_VERSION,
    })),
    metadata: {
      ...(metadata || {}),
      authorizationScope,
      authorization_scope: authorizationScope,
      authorizationAction: consentCopy.action,
      authorization_action: consentCopy.action,
      authorizationResponsible: consentCopy.responsible,
      authorization_responsible: consentCopy.responsible,
      authorizationText,
      authorization_text: authorizationText,
      tenantName: tenantName || null,
      tenantSigla: tenantSigla || null,
    },
  });
};

type DataUseConsentBoxProps = {
  contextType: string;
  contextId: string;
  tenantId?: string | null;
  source?: LegalAcceptanceSource;
  title?: string;
  description?: string;
  metadata?: Record<string, unknown>;
  actionLabel?: string;
  className?: string;
  compact?: boolean;
  onAccepted?: () => void;
  onAcceptanceChange?: (accepted: boolean) => void;
};

export default function DataUseConsentBox({
  contextType,
  contextId,
  tenantId,
  source = "app",
  title,
  description,
  metadata,
  actionLabel,
  className = "",
  compact = false,
  onAccepted,
  onAcceptanceChange,
}: DataUseConsentBoxProps) {
  const { user } = useAuth();
  const { addToast } = useToast();
  const { tenantId: activeTenantId, tenantName, tenantSigla } = useTenantTheme();
  const [checking, setChecking] = useState(true);
  const [accepted, setAccepted] = useState(false);
  const [marked, setMarked] = useState(false);
  const [saving, setSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const cleanTenantId = clean(tenantId) || clean(activeTenantId);
  const cleanContextType = clean(contextType);
  const cleanContextId = clean(contextId);
  const consentCopy = useMemo(
    () =>
      buildDataUseConsentCopy({
        contextType: cleanContextType,
        metadata,
        tenantSigla,
        tenantName,
      }),
    [cleanContextType, metadata, tenantName, tenantSigla]
  );
  const effectiveTitle = title || consentCopy.title;
  const effectiveDescription = description || consentCopy.description;
  const effectiveActionLabel = actionLabel || consentCopy.actionLabel;

  useEffect(() => {
    onAcceptanceChange?.(accepted);
  }, [accepted, onAcceptanceChange]);

  useEffect(() => {
    if (!user?.uid || !cleanContextType || !cleanContextId) {
      setChecking(false);
      setAccepted(true);
      return;
    }

    let mounted = true;
    setChecking(true);
    hasDataUseConsent({
      userId: user.uid,
      contextType: cleanContextType,
      contextId: cleanContextId,
      tenantId: cleanTenantId,
      source,
    })
      .then((hasConsent) => {
        if (mounted) setAccepted(hasConsent);
      })
      .catch((error: unknown) => {
        console.error("Erro ao consultar aceite de dados:", error);
        if (mounted) setAccepted(false);
      })
      .finally(() => {
        if (mounted) setChecking(false);
      });

    return () => {
      mounted = false;
    };
  }, [cleanContextId, cleanContextType, cleanTenantId, source, user?.uid]);

  const handleAccept = async () => {
    if (!marked || saving || !user?.uid || !cleanContextType || !cleanContextId) return;

    try {
      setSaving(true);
      setErrorMessage("");
      await recordDataUseConsent({
        tenantId: cleanTenantId || null,
        source,
        contextType: cleanContextType,
        contextId: cleanContextId,
        metadata,
        authorizationText: consentCopy.authorizationText,
        tenantSigla,
        tenantName,
      });
      setAccepted(true);
      setMarked(false);
      addToast("Autorização registrada.", "success");
      onAccepted?.();
    } catch (error: unknown) {
      const message =
        error instanceof Error ? error.message : "Não foi possível registrar a autorização.";
      setErrorMessage(message);
      addToast(message, "error");
    } finally {
      setSaving(false);
    }
  };

  if (!user?.uid || accepted) return null;

  return (
    <section
      className={`rounded-2xl border border-emerald-500/25 bg-emerald-500/10 p-4 text-emerald-50 shadow-[0_18px_50px_rgba(16,185,129,0.08)] ${className}`}
    >
      <div className="flex items-start gap-3">
        <div className="mt-1 rounded-xl border border-emerald-400/30 bg-black/25 p-2 text-emerald-200">
          {checking ? <Loader2 size={16} className="animate-spin" /> : <ShieldCheck size={16} />}
        </div>
        <div className="min-w-0 flex-1">
          <h2 className="text-sm font-black uppercase text-white">{effectiveTitle}</h2>
          {!compact ? (
            <p className="mt-2 text-xs leading-5 text-emerald-50/80">{effectiveDescription}</p>
          ) : null}
        </div>
      </div>

      <label className="mt-4 flex cursor-pointer items-start gap-3 rounded-2xl border border-emerald-400/20 bg-black/20 p-3 text-xs leading-5 text-emerald-50">
        <input
          type="checkbox"
          checked={marked}
          disabled={checking || saving}
          onChange={(event) => setMarked(event.target.checked)}
          className="mt-1 h-4 w-4 accent-emerald-500"
        />
        <span>{consentCopy.authorizationText}</span>
      </label>

      {errorMessage ? (
        <p className="mt-3 rounded-xl border border-red-500/30 bg-red-500/10 px-3 py-2 text-xs font-semibold text-red-100">
          {errorMessage}
        </p>
      ) : null}

      <button
        type="button"
        onClick={() => void handleAccept()}
        disabled={!marked || checking || saving}
        className="mt-3 inline-flex w-full items-center justify-center gap-2 rounded-2xl bg-emerald-500 px-4 py-3 text-xs font-black uppercase tracking-[0.16em] text-black transition hover:bg-emerald-400 disabled:cursor-not-allowed disabled:opacity-50"
      >
        {saving ? <Loader2 size={14} className="animate-spin" /> : <CheckCircle2 size={14} />}
        {saving ? "Registrando..." : effectiveActionLabel}
      </button>
    </section>
  );
}

type DataUseConsentModalProps = {
  open: boolean;
  contextType: string;
  contextId: string;
  tenantId?: string | null;
  source?: LegalAcceptanceSource;
  title?: string;
  description?: string;
  metadata?: Record<string, unknown>;
  actionLabel?: string;
  cancelLabel?: string;
  allowCancel?: boolean;
  onAccepted?: () => void;
  onCancel?: () => void;
};

export function DataUseConsentModal({
  open,
  contextType,
  contextId,
  tenantId,
  source = "app",
  title,
  description,
  metadata,
  actionLabel,
  cancelLabel = "Cancelar",
  allowCancel = true,
  onAccepted,
  onCancel,
}: DataUseConsentModalProps) {
  const { user } = useAuth();
  const { addToast } = useToast();
  const { tenantId: activeTenantId, tenantName, tenantSigla } = useTenantTheme();
  const [marked, setMarked] = useState(false);
  const [saving, setSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const cleanTenantId = clean(tenantId) || clean(activeTenantId);
  const cleanContextType = clean(contextType);
  const cleanContextId = clean(contextId);
  const consentCopy = useMemo(
    () =>
      buildDataUseConsentCopy({
        contextType: cleanContextType,
        metadata,
        tenantSigla,
        tenantName,
      }),
    [cleanContextType, metadata, tenantName, tenantSigla]
  );
  const effectiveTitle = title || consentCopy.title;
  const effectiveDescription = description || consentCopy.description;
  const effectiveActionLabel = actionLabel || consentCopy.actionLabel;

  useEffect(() => {
    if (!open) {
      setMarked(false);
      setErrorMessage("");
    }
  }, [open]);

  const handleAccept = async () => {
    if (!marked || saving || !user?.uid || !cleanContextType || !cleanContextId) return;

    try {
      setSaving(true);
      setErrorMessage("");
      await recordDataUseConsent({
        tenantId: cleanTenantId || null,
        source,
        contextType: cleanContextType,
        contextId: cleanContextId,
        metadata,
        authorizationText: consentCopy.authorizationText,
        tenantSigla,
        tenantName,
      });
      setMarked(false);
      addToast("Autorização registrada.", "success");
      onAccepted?.();
    } catch (error: unknown) {
      const message =
        error instanceof Error ? error.message : "Não foi possível registrar a autorização.";
      setErrorMessage(message);
      addToast(message, "error");
    } finally {
      setSaving(false);
    }
  };

  if (!open || !user?.uid) return null;

  return (
    <div className="fixed inset-0 z-[1000] flex items-center justify-center bg-black/82 px-4 py-6 backdrop-blur-md">
      <section
        role="dialog"
        aria-modal="true"
        className="w-full max-w-xl rounded-[1.75rem] border border-emerald-500/25 bg-zinc-950 p-5 text-white shadow-[0_30px_100px_rgba(0,0,0,0.6)]"
      >
        <div className="flex items-start justify-between gap-4">
          <div className="flex items-start gap-3">
            <div className="mt-1 rounded-2xl border border-emerald-400/25 bg-emerald-500/10 p-3 text-emerald-200">
              <ShieldCheck size={18} />
            </div>
            <div>
              <h2 className="text-lg font-black uppercase tracking-tight text-white">{effectiveTitle}</h2>
              <p className="mt-2 text-sm leading-6 text-zinc-300">{effectiveDescription}</p>
            </div>
          </div>
          {allowCancel ? (
            <button
              type="button"
              onClick={onCancel}
              className="inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-full border border-zinc-800 bg-black/40 text-zinc-400 transition hover:border-zinc-600 hover:text-white disabled:opacity-60"
              aria-label={cancelLabel}
              disabled={saving}
            >
              <X size={16} />
            </button>
          ) : null}
        </div>

        <label className="mt-5 flex cursor-pointer items-start gap-3 rounded-2xl border border-emerald-500/20 bg-emerald-500/10 p-4 text-xs leading-5 text-emerald-50">
          <input
            type="checkbox"
            checked={marked}
            disabled={saving}
            onChange={(event) => setMarked(event.target.checked)}
            className="mt-1 h-4 w-4 accent-emerald-500"
          />
          <span>{consentCopy.authorizationText}</span>
        </label>

        {errorMessage ? (
          <p className="mt-3 rounded-xl border border-red-500/30 bg-red-500/10 px-3 py-2 text-xs font-semibold text-red-100">
            {errorMessage}
          </p>
        ) : null}

        <div className="mt-5 flex flex-col gap-3 sm:flex-row sm:justify-end">
          {allowCancel ? (
            <button
              type="button"
              onClick={onCancel}
              disabled={saving}
              className="inline-flex items-center justify-center rounded-2xl border border-zinc-700 bg-black/40 px-5 py-3 text-xs font-black uppercase tracking-[0.16em] text-zinc-200 transition hover:border-zinc-500 disabled:opacity-60"
            >
              {cancelLabel}
            </button>
          ) : null}
          <button
            type="button"
            onClick={() => void handleAccept()}
            disabled={!marked || saving}
            className="inline-flex items-center justify-center gap-2 rounded-2xl bg-emerald-500 px-5 py-3 text-xs font-black uppercase tracking-[0.16em] text-black transition hover:bg-emerald-400 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {saving ? <Loader2 size={14} className="animate-spin" /> : <CheckCircle2 size={14} />}
            {saving ? "Registrando..." : effectiveActionLabel}
          </button>
        </div>
      </section>
    </div>
  );
}

type DataUseRequiredModalProps = {
  enabled?: boolean;
  userId?: string | null;
  contextType: string;
  contextId: string;
  tenantId?: string | null;
  source?: LegalAcceptanceSource;
  title?: string;
  description?: string;
  actionLabel?: string;
  metadata?: Record<string, unknown>;
};

export function DataUseRequiredModal({
  enabled = true,
  userId,
  contextType,
  contextId,
  tenantId,
  source = "app",
  title,
  description,
  actionLabel,
  metadata,
}: DataUseRequiredModalProps) {
  const [checking, setChecking] = useState(true);
  const [accepted, setAccepted] = useState(false);

  useEffect(() => {
    if (!enabled || !userId || !clean(contextType) || !clean(contextId)) {
      setChecking(false);
      setAccepted(true);
      return;
    }

    let mounted = true;
    setChecking(true);
    hasDataUseConsent({
      userId,
      contextType,
      contextId,
      tenantId,
      source,
    })
      .then((hasConsent) => {
        if (!mounted) return;
        setAccepted(hasConsent);
      })
      .catch((error: unknown) => {
        console.error("Erro ao consultar autorização de dados:", error);
        if (!mounted) return;
        setAccepted(false);
      })
      .finally(() => {
        if (mounted) setChecking(false);
      });

    return () => {
      mounted = false;
    };
  }, [contextId, contextType, enabled, source, tenantId, userId]);

  return (
    <DataUseConsentModal
      open={enabled && !checking && !accepted}
      contextType={contextType}
      contextId={contextId}
      tenantId={tenantId}
      source={source}
      title={title}
      description={description}
      actionLabel={actionLabel}
      metadata={metadata}
      allowCancel={false}
      onAccepted={() => setAccepted(true)}
    />
  );
}
