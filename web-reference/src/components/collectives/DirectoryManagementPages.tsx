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
import type { LeagueRecord } from "@/lib/leaguesService";
import { DirectoryManagementGate } from "./DirectoryManagementGate";

const DIRECTORY_ROOT_PATH = "/diretorio/configurar";

const buildDirectoryBasePath = (league: LeagueRecord, routeSegment: string): string => {
  const segment = routeSegment.trim() || league.sigla?.trim() || league.id;
  return `${DIRECTORY_ROOT_PATH}/${encodeURIComponent(segment)}`;
};

const sharedAdminProps = {
  showBoard: false,
  category: "diretorio" as const,
  storageNamespace: "diretorio",
  entityLabel: "diretório",
  entityArticle: "do" as const,
};

export function DirectoryManagementHub() {
  return (
    <DirectoryManagementGate>
      {({ leagueId, league, routeSegment }) => (
        <LigasAdminPageContent
          pageVariant="hub"
          leagueIdOverride={leagueId}
          basePath={buildDirectoryBasePath(league, routeSegment)}
          {...sharedAdminProps}
        />
      )}
    </DirectoryManagementGate>
  );
}

export function DirectoryManagementInfoPage() {
  return (
    <DirectoryManagementGate>
      {({ leagueId, league, routeSegment }) => (
        <LigasAdminPageContent
          lockedTab="visual"
          leagueIdOverride={leagueId}
          basePath={buildDirectoryBasePath(league, routeSegment)}
          {...sharedAdminProps}
        />
      )}
    </DirectoryManagementGate>
  );
}

export function DirectoryManagementMembersPage() {
  return (
    <DirectoryManagementGate>
      {({ leagueId, league, routeSegment }) => (
        <LigasAdminPageContent
          lockedTab="members"
          leagueIdOverride={leagueId}
          basePath={buildDirectoryBasePath(league, routeSegment)}
          {...sharedAdminProps}
        />
      )}
    </DirectoryManagementGate>
  );
}

export function DirectoryManagementEventsPage() {
  return (
    <DirectoryManagementGate>
      {({ leagueId, league, routeSegment }) => (
        <LigasAdminPageContent
          lockedTab="events"
          leagueIdOverride={leagueId}
          basePath={buildDirectoryBasePath(league, routeSegment)}
          {...sharedAdminProps}
        />
      )}
    </DirectoryManagementGate>
  );
}

export function DirectoryManagementStorePage({
  mode = "overview",
}: {
  mode?: "overview" | "products" | "pending" | "approved";
}) {
  return (
    <DirectoryManagementGate>
      {({ leagueId, league, routeSegment }) => (
        <LeagueStoreAdminPage
          mode={mode}
          basePath={buildDirectoryBasePath(league, routeSegment)}
          leagueIdOverride={leagueId}
          showBoard={false}
          entityLabel="diretório"
          entityArticle="do"
        />
      )}
    </DirectoryManagementGate>
  );
}

export function DirectoryManagementFinancePage({
  view = "hub",
}: {
  view?: "hub" | "eventos" | "produtos";
}) {
  return (
    <DirectoryManagementGate>
      {({ leagueId, league, routeSegment }) => (
        <LeagueFinanceDashboard
          view={view}
          basePath={buildDirectoryBasePath(league, routeSegment)}
          leagueIdOverride={leagueId}
          showBoard={false}
          entityLabel="diretório"
          entityArticle="do"
        />
      )}
    </DirectoryManagementGate>
  );
}

export function DirectoryManagementFinancialStatementPage() {
  return (
    <DirectoryManagementGate>
      {({ leagueId, league, routeSegment }) => {
        const basePath = buildDirectoryBasePath(league, routeSegment);
        return (
          <FinancialStatementPage
            scopeType="directory"
            scopeId={leagueId}
            title={league.sigla?.trim() || league.nome?.trim() || "Diretório"}
            subtitle="Extrato isolado do diretório: eventos, loja e modo vendas vinculados somente a esta entidade."
            eyebrow="Financeiro do diretório"
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
    </DirectoryManagementGate>
  );
}

export function DirectoryManagementEventBiPage({
  view = "inicio",
}: {
  view?: AdminEventBiView;
}) {
  return (
    <DirectoryManagementGate>
      {({ leagueId, league, routeSegment }) => {
        const basePath = buildDirectoryBasePath(league, routeSegment);
        const title = league.sigla?.trim() || league.nome?.trim() || "Diretório";
        const logo = resolveLeagueLogoSrc(league) || "/logo.png";
        return (
          <AdminEventBiDashboard
            view={view}
            basePath={`${basePath}/gestao/eventos`}
            eventWorkspaceBasePath={`${basePath}/eventos`}
            backHref={basePath}
            lockedScopeType="directory"
            lockedScopeId={leagueId}
            scopeLabel="do diretório"
            contextTitle={title}
            contextLogo={logo}
            contextEyebrow="BI do diretório"
          />
        );
      }}
    </DirectoryManagementGate>
  );
}

export function DirectoryManagementFrequencyPage() {
  return (
    <DirectoryManagementGate>
      {({ leagueId, league, routeSegment }) => (
        <LeagueFrequencyPage
          basePath={buildDirectoryBasePath(league, routeSegment)}
          leagueIdOverride={leagueId}
          showBoard={false}
        />
      )}
    </DirectoryManagementGate>
  );
}

export function DirectoryManagementEventPresencePage({ eventId }: { eventId: string }) {
  return (
    <DirectoryManagementGate>
      {({ leagueId, league, routeSegment }) => {
        const basePath = buildDirectoryBasePath(league, routeSegment);
        return (
          <LigaEventPresencePage
            eventId={eventId}
            leagueId={leagueId}
            backHref={`${basePath}/eventos`}
          />
        );
      }}
    </DirectoryManagementGate>
  );
}

export function DirectoryManagementEventWorkspacePage({
  eventId,
  sectionSegments,
}: {
  eventId: string;
  sectionSegments?: string[];
}) {
  return (
    <DirectoryManagementGate>
      {({ leagueId, league, routeSegment }) => {
        const decodedEventId = decodeURIComponent(eventId);
        const encodedEventId = encodeURIComponent(decodedEventId);
        const basePath = buildDirectoryBasePath(league, routeSegment);
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
            paymentRecipientContext={{ ownerType: "directory", ownerId: leagueId }}
          />
        );
      }}
    </DirectoryManagementGate>
  );
}
