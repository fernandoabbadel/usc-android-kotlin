import { FinancialStatementPage } from "@/components/financeiro/FinancialStatementPage";

export default function AdminGestaoFinanceiroPage() {
  return (
    <FinancialStatementPage
      scopeType="tenant"
      title="Financeiro"
      subtitle="Extrato oficial da atlética: planos, ingressos, loja oficial e modo vendas, sem misturar ligas, comissões, diretório ou mini vendors."
      eyebrow="Gestão financeira"
      backHref="/admin/gestao"
      basePath="/admin"
      includePlans
      biLinks={[
        { label: "BI Gestão", href: "/admin/gestao/eventos" },
        { label: "BI Loja", href: "/admin/gestao/loja" },
        { label: "BI Comercial", href: "/admin/bi/comercial" },
      ]}
    />
  );
}
