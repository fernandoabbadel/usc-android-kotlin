"use client";

import LigasAdminPageContent from "@/app/ligas/LigasAdminPageContent";
import { LeagueStoreAdminPage } from "@/app/ligas/LeagueStoreAdminPage";
import AdminEventBiDashboard, {
  type AdminEventBiView,
} from "@/app/admin/bi/_components/AdminEventBiDashboard";
import { ContextualAdminEventWorkspace } from "@/app/admin/eventos/_components/ContextualAdminEventWorkspace";
import { AdminEventTicketOrderPage } from "@/app/admin/eventos/_components/AdminEventTicketOrderPage";
import { EventProductWithdrawalClientPage } from "@/app/admin/eventos/[id]/ficha/retirada/EventProductWithdrawalClientPage";
import { LigaEventPresencePage } from "@/app/ligas/_components/LigaEventPresencePage";
import { LeagueFinanceDashboard } from "@/app/ligas/_components/LeagueFinanceDashboard";
import { LeagueFrequencyPage } from "@/app/ligas/_components/LeagueFrequencyPage";
import { FinancialStatementPage } from "@/components/financeiro/FinancialStatementPage";
import { resolveLeagueLogoSrc } from "@/lib/leagueMedia";
import type { ManagedLeagueRecord } from "@/lib/leaguesService";
import { CommissionManagementGate } from "./CommissionManagementGate";

const COMMISSION_ROOT_PATH = "/comissoes/configurar";

const buildCommissionBasePath = (
  league: ManagedLeagueRecord,
  routeSegment: string
): string => {
  const segment = routeSegment.trim() || league.turmaId?.trim() || league.sigla?.trim() || league.id;
  return `${COMMISSION_ROOT_PATH}/${encodeURIComponent(segment)}`;
};

const sharedAdminProps = {
  showBoard: false,
  category: "comissao" as const,
  storageNamespace: "comissoes",
  entityLabel: "comissão",
  entityArticle: "da" as const,
};

export function CommissionManagementHub() {
  return (
    <CommissionManagementGate>
      {({ leagueId, league, routeSegment }) => (
        <LigasAdminPageContent
          pageVariant="hub"
          leagueIdOverride={leagueId}
          basePath={buildCommissionBasePath(league, routeSegment)}
          {...sharedAdminProps}
        />
      )}
    </CommissionManagementGate>
  );
}

export function CommissionManagementInfoPage() {
  return (
    <CommissionManagementGate>
      {({ leagueId, league, routeSegment }) => (
        <LigasAdminPageContent
          lockedTab="visual"
          leagueIdOverride={leagueId}
          basePath={buildCommissionBasePath(league, routeSegment)}
          {...sharedAdminProps}
        />
      )}
    </CommissionManagementGate>
  );
}

export function CommissionManagementMembersPage() {
  return (
    <CommissionManagementGate>
      {({ leagueId, league, routeSegment }) => (
        <LigasAdminPageContent
          lockedTab="members"
          leagueIdOverride={leagueId}
          basePath={buildCommissionBasePath(league, routeSegment)}
          {...sharedAdminProps}
        />
      )}
    </CommissionManagementGate>
  );
}

export function CommissionManagementEventsPage() {
  return (
    <CommissionManagementGate>
      {({ leagueId, league, routeSegment }) => (
        <LigasAdminPageContent
          lockedTab="events"
          leagueIdOverride={leagueId}
          basePath={buildCommissionBasePath(league, routeSegment)}
          {...sharedAdminProps}
        />
      )}
    </CommissionManagementGate>
  );
}

export function CommissionManagementStorePage({
  mode = "overview",
}: {
  mode?: "overview" | "products" | "pending" | "approved";
}) {
  return (
    <CommissionManagementGate>
      {({ leagueId, league, routeSegment }) => (
        <LeagueStoreAdminPage
          mode={mode}
          basePath={buildCommissionBasePath(league, routeSegment)}
          leagueIdOverride={leagueId}
          showBoard={false}
          entityLabel="comissão"
          entityArticle="da"
        />
      )}
    </CommissionManagementGate>
  );
}

export function CommissionManagementFinancePage({
  view = "hub",
}: {
  view?: "hub" | "eventos" | "produtos";
}) {
  return (
    <CommissionManagementGate>
      {({ leagueId, league, routeSegment }) => (
        <LeagueFinanceDashboard
          view={view}
          basePath={buildCommissionBasePath(league, routeSegment)}
          leagueIdOverride={leagueId}
          showBoard={false}
          entityLabel="comissão"
          entityArticle="da"
        />
      )}
    </CommissionManagementGate>
  );
}

export function CommissionManagementFinancialStatementPage() {
  return (
    <CommissionManagementGate>
      {({ leagueId, league, routeSegment }) => {
        const basePath = buildCommissionBasePath(league, routeSegment);
        return (
          <FinancialStatementPage
            scopeType="commission"
            scopeId={leagueId}
            title={league.sigla?.trim() || league.nome?.trim() || "Comissão"}
            subtitle="Extrato isolado da comissão: eventos, loja e modo vendas vinculados somente a esta entidade."
            eyebrow="Financeiro da comissão"
            logoSrc={resolveLeagueLogoSrc(league) || "/placeholder_liga.png"}
            backHref={`${basePath}/gestao`}
            basePath={basePath}
            biLinks={[
              { label: "BI Gestão", href: `${basePath}/gestao/eventos` },
              { label: "BI Loja", href: `${basePath}/gestao/produtos` },
              { label: "Frequência", href: `${basePath}/gestao/frequencia` },
            ]}
          />
        );
      }}
    </CommissionManagementGate>
  );
}

export function CommissionManagementEventBiPage({
  view = "inicio",
}: {
  view?: AdminEventBiView;
}) {
  return (
    <CommissionManagementGate>
      {({ leagueId, league, routeSegment }) => {
        const basePath = buildCommissionBasePath(league, routeSegment);
        const title = league.sigla?.trim() || league.nome?.trim() || "Comissão";
        const logo = resolveLeagueLogoSrc(league) || "/logo.png";
        return (
          <AdminEventBiDashboard
            view={view}
            basePath={`${basePath}/gestao/eventos`}
            eventWorkspaceBasePath={`${basePath}/eventos`}
            backHref={basePath}
            lockedScopeType="commission"
            lockedScopeId={leagueId}
            scopeLabel="da comissão"
            contextTitle={title}
            contextLogo={logo}
            contextEyebrow="BI da comissão"
          />
        );
      }}
    </CommissionManagementGate>
  );
}

export function CommissionManagementFrequencyPage() {
  return (
    <CommissionManagementGate>
      {({ leagueId, league, routeSegment }) => (
        <LeagueFrequencyPage
          basePath={buildCommissionBasePath(league, routeSegment)}
          leagueIdOverride={leagueId}
          showBoard={false}
          memberScope="turma"
        />
      )}
    </CommissionManagementGate>
  );
}

export function CommissionManagementEventPresencePage({ eventId }: { eventId: string }) {
  return (
    <CommissionManagementGate>
      {({ leagueId, league, routeSegment }) => {
        const basePath = buildCommissionBasePath(league, routeSegment);
        return (
          <LigaEventPresencePage
            eventId={eventId}
            leagueId={leagueId}
            backHref={`${basePath}/eventos`}
          />
        );
      }}
    </CommissionManagementGate>
  );
}

export function CommissionManagementEventWorkspacePage({
  eventId,
  sectionSegments,
}: {
  eventId: string;
  sectionSegments?: string[];
}) {
  return (
    <CommissionManagementGate>
      {({ leagueId, league, routeSegment }) => {
        const decodedEventId = decodeURIComponent(eventId);
        const encodedEventId = encodeURIComponent(decodedEventId);
        const basePath = buildCommissionBasePath(league, routeSegment);
        const workspaceBasePath = `${basePath}/eventos/${encodedEventId}`;
        const normalizedSectionPath = (sectionSegments || [])
          .map((segment) => segment.toLowerCase())
          .join("/");

        if (sectionSegments?.[0] === "ingressos" && sectionSegments[1]) {
          return (
            <AdminEventTicketOrderPage
              eventId={decodedEventId}
              pedidoId={decodeURIComponent(sectionSegments[1])}
              backHref={`${workspaceBasePath}/ingressos`}
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
              eventId={decodedEventId}
              mode={mode}
              basePath={workspaceBasePath}
            />
          );
        }

        return (
          <ContextualAdminEventWorkspace
            eventId={decodedEventId}
            sectionSegments={sectionSegments}
            workspaceBasePath={workspaceBasePath}
            eventsListHref={`${basePath}/eventos`}
            legacyListHref={`${basePath}/eventos/lista/${encodedEventId}`}
            eventBiBasePath={`${basePath}/gestao/eventos`}
            paymentRecipientContext={{ ownerType: "commission", ownerId: leagueId }}
          />
        );
      }}
    </CommissionManagementGate>
  );
}
