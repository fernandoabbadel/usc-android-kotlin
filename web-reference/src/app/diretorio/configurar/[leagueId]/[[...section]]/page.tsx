import {
  DirectoryManagementEventBiPage,
  DirectoryManagementEventPresencePage,
  DirectoryManagementEventsPage,
  DirectoryManagementEventWorkspacePage,
  DirectoryManagementFinancePage,
  DirectoryManagementFrequencyPage,
  DirectoryManagementHub,
  DirectoryManagementInfoPage,
  DirectoryManagementMembersPage,
  DirectoryManagementStorePage,
} from "@/components/collectives/DirectoryManagementPages";

const BI_VIEWS = new Set(["comercial", "operacional", "portaria", "estrategico", "vendas"]);

export default async function DiretorioConfigurarScopedPage({
  params,
}: {
  params: Promise<{ leagueId: string; section?: string[] }>;
}) {
  const { section = [] } = await params;
  const [area, subArea, third, ...rest] = section;

  if (!area) return <DirectoryManagementHub />;
  if (area === "informacoes") return <DirectoryManagementInfoPage />;
  if (area === "membros") return <DirectoryManagementMembersPage />;

  if (area === "loja") {
    if (subArea === "produtos") return <DirectoryManagementStorePage mode="products" />;
    if (subArea === "pedidos-pendentes") return <DirectoryManagementStorePage mode="pending" />;
    if (subArea === "pedidos-aprovados") return <DirectoryManagementStorePage mode="approved" />;
    return <DirectoryManagementStorePage />;
  }

  if (area === "gestao") {
    if (!subArea) return <DirectoryManagementFinancePage />;
    if (subArea === "produtos") return <DirectoryManagementFinancePage view="produtos" />;
    if (subArea === "frequencia") return <DirectoryManagementFrequencyPage />;
    if (subArea === "eventos") {
      if (third && BI_VIEWS.has(third)) {
        return <DirectoryManagementEventBiPage view={third as "comercial" | "operacional" | "portaria" | "estrategico" | "vendas"} />;
      }
      return <DirectoryManagementEventBiPage />;
    }
  }

  if (area === "eventos") {
    if (!subArea || subArea === "novo") return <DirectoryManagementEventsPage />;
    if (subArea === "lista" && third) {
      return <DirectoryManagementEventPresencePage eventId={decodeURIComponent(third)} />;
    }
    return (
      <DirectoryManagementEventWorkspacePage
        eventId={decodeURIComponent(subArea)}
        sectionSegments={third ? [third, ...rest] : undefined}
      />
    );
  }

  return <DirectoryManagementHub />;
}
