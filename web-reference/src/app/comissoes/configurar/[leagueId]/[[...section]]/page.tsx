import {
  CommissionManagementEventBiPage,
  CommissionManagementEventPresencePage,
  CommissionManagementEventsPage,
  CommissionManagementEventWorkspacePage,
  CommissionManagementFinancePage,
  CommissionManagementFrequencyPage,
  CommissionManagementHub,
  CommissionManagementInfoPage,
  CommissionManagementMembersPage,
  CommissionManagementStorePage,
} from "@/components/collectives/CommissionManagementPages";

const BI_VIEWS = new Set(["comercial", "operacional", "portaria", "estrategico", "vendas"]);

export default async function ComissaoConfigurarScopedPage({
  params,
}: {
  params: Promise<{ leagueId: string; section?: string[] }>;
}) {
  const { section = [] } = await params;
  const [area, subArea, third, ...rest] = section;

  if (!area) return <CommissionManagementHub />;
  if (area === "informacoes") return <CommissionManagementInfoPage />;
  if (area === "membros") return <CommissionManagementMembersPage />;

  if (area === "loja") {
    if (subArea === "produtos") return <CommissionManagementStorePage mode="products" />;
    if (subArea === "pedidos-pendentes") return <CommissionManagementStorePage mode="pending" />;
    if (subArea === "pedidos-aprovados") return <CommissionManagementStorePage mode="approved" />;
    return <CommissionManagementStorePage />;
  }

  if (area === "gestao") {
    if (!subArea) return <CommissionManagementFinancePage />;
    if (subArea === "produtos") return <CommissionManagementFinancePage view="produtos" />;
    if (subArea === "frequencia") return <CommissionManagementFrequencyPage />;
    if (subArea === "eventos") {
      if (third && BI_VIEWS.has(third)) {
        return <CommissionManagementEventBiPage view={third as "comercial" | "operacional" | "portaria" | "estrategico" | "vendas"} />;
      }
      return <CommissionManagementEventBiPage />;
    }
  }

  if (area === "eventos") {
    if (!subArea || subArea === "novo") return <CommissionManagementEventsPage />;
    if (subArea === "lista" && third) {
      return <CommissionManagementEventPresencePage eventId={decodeURIComponent(third)} />;
    }
    return (
      <CommissionManagementEventWorkspacePage
        eventId={decodeURIComponent(subArea)}
        sectionSegments={third ? [third, ...rest] : undefined}
      />
    );
  }

  return <CommissionManagementHub />;
}
