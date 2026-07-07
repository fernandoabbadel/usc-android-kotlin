import AdminEventWorkspace, { type EventWorkspaceSection } from "./AdminEventWorkspace";
import AdminEventBiDashboard, { type AdminEventBiView } from "@/app/admin/bi/_components/AdminEventBiDashboard";
import { EventProductWithdrawalClientPage } from "../[id]/ficha/retirada/EventProductWithdrawalClientPage";
import type { TenantPaymentRecipientContext } from "@/lib/paymentRecipients";

const SECTION_BY_PATH: Record<string, EventWorkspaceSection> = {
  extrato: "extrato",
  bi: "bi",
  lotes: "lotes",
  ingressos: "ingressos",
  cupons: "cupons",
  checkins: "checkins",
  scan: "scan",
  edicao: "edicao",
  enquetes: "enquetes",
  recebedores: "recebedores",
  ficha: "ficha",
  "ficha/pagamento": "ficha-pagamento",
  "ficha/cadastro": "ficha-cadastro",
  "ficha/produto": "ficha-produto",
  "ficha/produtos": "ficha-produtos",
  "ficha/produtos/cadastro": "ficha-produtos-cadastro",
};

const resolveSection = (segments?: string[]): EventWorkspaceSection => {
  const path = (segments && segments.length > 0 ? segments.join("/") : "edicao").toLowerCase();
  return SECTION_BY_PATH[path] || "edicao";
};

const BI_VIEWS = new Set<AdminEventBiView>(["inicio", "comercial", "operacional", "portaria", "estrategico", "vendas"]);

const resolveBiView = (segments?: string[]): AdminEventBiView | null => {
  if (!segments?.length || segments[0].toLowerCase() !== "bi") return null;
  if (!segments[1]) return null;
  const candidate = (segments[1] || "inicio").toLowerCase() as AdminEventBiView;
  return BI_VIEWS.has(candidate) ? candidate : null;
};

const scopeLabelForOwner = (context?: TenantPaymentRecipientContext): string | undefined => {
  if (context?.ownerType === "directory") return "do diretório";
  if (context?.ownerType === "commission") return "da comissão";
  if (context?.ownerType === "league") return "da liga";
  return undefined;
};

export function ContextualAdminEventWorkspace({
  eventId,
  sectionSegments,
  workspaceBasePath,
  eventsListHref,
  legacyListHref,
  eventBiBasePath,
  paymentRecipientContext,
}: {
  eventId: string;
  sectionSegments?: string[];
  workspaceBasePath: string;
  eventsListHref: string;
  legacyListHref: string;
  eventBiBasePath?: string;
  paymentRecipientContext?: TenantPaymentRecipientContext;
}) {
  const normalizedSectionPath = (sectionSegments || [])
    .map((segment) => segment.toLowerCase())
    .join("/");
  const biView = resolveBiView(sectionSegments);

  if (biView) {
    const normalizedWorkspaceBasePath = workspaceBasePath.trim().replace(/\/+$/, "");
    const ownerType = paymentRecipientContext?.ownerType || "tenant";
    const ownerId = paymentRecipientContext?.ownerId || "todos";
    return (
      <AdminEventBiDashboard
        view={biView}
        initialEventId={eventId}
        basePath={`${normalizedWorkspaceBasePath}/bi`}
        eventWorkspaceBasePath={eventsListHref}
        lockedScopeType={ownerType}
        lockedScopeId={ownerId}
        scopeLabel={scopeLabelForOwner(paymentRecipientContext)}
        backHref={`${normalizedWorkspaceBasePath}/edicao`}
      />
    );
  }

  if (
    normalizedSectionPath === "ficha/retirada" ||
    normalizedSectionPath === "ficha/retirada/pendentes" ||
    normalizedSectionPath === "ficha/retirada/retirados"
  ) {
    const mode = normalizedSectionPath.endsWith("/pendentes")
      ? "pendentes"
      : normalizedSectionPath.endsWith("/retirados")
        ? "retirados"
        : "hub";
    return (
      <EventProductWithdrawalClientPage
        eventId={eventId}
        mode={mode}
        basePath={workspaceBasePath}
      />
    );
  }

  return (
    <AdminEventWorkspace
      eventId={eventId}
      section={resolveSection(sectionSegments)}
      workspaceBasePath={workspaceBasePath}
      eventsListHref={eventsListHref}
      legacyListHref={legacyListHref}
      eventBiBasePath={eventBiBasePath}
      paymentRecipientContext={paymentRecipientContext}
    />
  );
}
