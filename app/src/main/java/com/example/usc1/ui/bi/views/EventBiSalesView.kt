package com.example.usc1.ui.bi.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.domain.model.EventBiAnalytics
import com.example.usc1.domain.model.EventBiTableRow
import com.example.usc1.domain.model.EventBiValueFormat
import com.example.usc1.domain.model.formatEventBiCurrency
import com.example.usc1.domain.model.formatEventBiDecimal
import com.example.usc1.domain.model.formatEventBiNumber
import com.example.usc1.domain.model.formatEventBiPercent
import com.example.usc1.domain.model.eventBiSalesHealthLabel
import com.example.usc1.domain.model.eventBiSalesHealthValue
import com.example.usc1.domain.model.safeDivide
import com.example.usc1.ui.bi.charts.EventBiBars
import com.example.usc1.ui.bi.charts.EventBiBarsDual
import com.example.usc1.ui.bi.charts.EventBiChartPanel
import com.example.usc1.ui.bi.charts.EventBiChipLink
import com.example.usc1.ui.bi.charts.EventBiColumn
import com.example.usc1.ui.bi.charts.EventBiColumnFormat
import com.example.usc1.ui.bi.charts.EventBiComboBarsLines
import com.example.usc1.ui.bi.charts.EventBiComboSort
import com.example.usc1.ui.bi.charts.EventBiDataTable
import com.example.usc1.ui.bi.charts.EventBiFilterLinkChips
import com.example.usc1.ui.bi.charts.EventBiHeatmapMetric
import com.example.usc1.ui.bi.charts.EventBiHeatmapOrder
import com.example.usc1.ui.bi.charts.EventBiKpiCard
import com.example.usc1.ui.bi.charts.EventBiKpiGrid
import com.example.usc1.ui.bi.charts.EventBiMetricKey
import com.example.usc1.ui.bi.charts.EventBiNetworkMetric
import com.example.usc1.ui.bi.charts.EventBiPieMetric
import com.example.usc1.ui.bi.charts.EventBiRadarMetric
import com.example.usc1.ui.bi.charts.EventBiScoreGauge
import com.example.usc1.ui.bi.charts.EventBiSemiDonutMetric
import com.example.usc1.ui.bi.charts.EventBiStackedPercentChart
import com.example.usc1.ui.bi.charts.EventBiWaterfallMetric

/**
 * Visão modo vendas do BI de Eventos (M8.2).
 *
 * Fonte: `web-reference/src/app/admin/bi/_components/AdminEventBiDashboard.tsx`, linhas 7356-7586
 * (`{view === "vendas" ? ... : null}`).
 *
 * O bloco `{false && view === "vendas"}` (7589-7650) **não** foi portado: está desabilitado no web
 * e nunca renderiza.
 */
@Composable
fun EventBiSalesView(
    analytics: EventBiAnalytics,
    modifier: Modifier = Modifier,
    /**
     * `salesWithdrawalLegendLinks` (6703): quatro atalhos de extrato. Com
     * `EventBiLinkBuilder.Inert` (M10 pendente) eles chegam vazios e o rodapé não renderiza,
     * exatamente como no web quando não há evento selecionado.
     */
    withdrawalLegendLinks: List<EventBiChipLink> = emptyList(),
) {
    val totals = analytics.totals
    val sales = analytics.sales

    // `categoryRevenueMode` (useState do web, consumido em 7422).
    var categoryRevenueMode by remember { mutableStateOf(EventBiMetricKey.Value) }

    val salesHealthLabel = eventBiSalesHealthLabel(sales.salesHealthScore)
    val salesHealthValue = eventBiSalesHealthValue(sales.salesHealthScore)

    val legendFooter: @Composable () -> Unit = {
        EventBiFilterLinkChips(links = withdrawalLegendLinks)
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        EventBiKpiGrid {
            EventBiKpiCard(
                label = "Receita aprovada",
                value = formatEventBiCurrency(totals.productRevenue),
                hint = "Modo vendas aprovado",
                icon = BiIcon.DollarSign,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Receita retirada",
                value = formatEventBiCurrency(sales.redeemedValue),
                hint = "Valor já entregue ou baixado",
                icon = BiIcon.CheckCircle2,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Receita pendente",
                value = formatEventBiCurrency(sales.pendingRedeemValue),
                hint = "Dinheiro recebido sem entrega registrada",
                icon = BiIcon.AlertTriangle,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Saúde operacional",
                value = salesHealthValue,
                hint = salesHealthLabel,
                icon = BiIcon.Target,
                modifier = Modifier.weight(1f),
            )
        }

        EventBiKpiGrid {
            EventBiKpiCard(
                label = "Itens vendidos",
                value = formatEventBiNumber(totals.approvedProductQuantity.toDouble()),
                hint = "${formatEventBiNumber(sales.redeemedItems.toDouble())} retirados",
                icon = BiIcon.Package,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Itens pendentes",
                value = formatEventBiNumber(sales.pendingRedeemItems.toDouble()),
                hint = "${formatEventBiNumber(sales.pendingRedeemOrders.toDouble())} pedidos com saldo",
                icon = BiIcon.Clock3,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Retirada parcial",
                value = formatEventBiNumber(sales.partialRedeemOrders.toDouble()),
                hint = "Pedidos aprovados com entrega incompleta",
                icon = BiIcon.ShoppingBag,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Taxa de retirada",
                value = formatEventBiPercent(sales.withdrawalRate),
                hint = "${formatEventBiNumber(sales.redeemedItems.toDouble())} de " +
                    "${formatEventBiNumber(totals.approvedProductQuantity.toDouble())} itens",
                icon = BiIcon.QrCode,
                modifier = Modifier.weight(1f),
            )
        }

        EventBiKpiGrid {
            EventBiKpiCard(
                label = "Baixa manual",
                value = formatEventBiPercent(sales.manualWithdrawalRate),
                hint = "Baixas manuais / total de baixas",
                icon = BiIcon.AlertTriangle,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Tempo até retirada",
                value = "${formatEventBiDecimal(sales.averageWithdrawalHours)}h",
                hint = "Mediana entre aprovação e baixa",
                icon = BiIcon.Clock3,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Maior pendência",
                value = "${formatEventBiDecimal(sales.maxPendingRedeemHours)}h",
                hint = sales.oldestPendingOrderName,
                icon = BiIcon.AlertTriangle,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Valor médio por item",
                value = formatEventBiCurrency(
                    safeDivide(totals.productRevenue, totals.approvedProductQuantity.toDouble()),
                ),
                hint = "Receita aprovada / itens",
                icon = BiIcon.BarChart3,
                modifier = Modifier.weight(1f),
            )
        }

        EventBiChartPanel(
            title = "Score do modo vendas",
            subtitle = "Retirada, QR, pendências, auditoria e tempo",
        ) {
            EventBiScoreGauge(score = sales.salesHealthScore, label = salesHealthLabel)
        }

        EventBiChartPanel(
            title = "Fila de retirada",
            subtitle = "Retirado, pendente, parcial e cancelado/estornado",
        ) {
            EventBiSemiDonutMetric(
                data = sales.withdrawalStatusRows,
                valueFormat = EventBiValueFormat.Currency,
            )
        }

        EventBiChartPanel(
            title = "Aging dos pendentes",
            subtitle = "Tempo desde aprovação sem retirada completa",
        ) {
            EventBiBarsDual(
                data = sales.pendingRedeemAgingRows,
                valueName = "Valor pendente",
                quantityName = "Itens pendentes",
            )
        }

        EventBiChartPanel(
            title = "Curva venda x retirada",
            subtitle = "Vendas aprovadas, retiradas e pendentes acumulados por horário",
            footer = legendFooter,
        ) {
            EventBiComboBarsLines(
                data = sales.salesWithdrawalTimelineRows,
                barName = "Vendas",
                lineOneName = "Retiradas",
                lineTwoName = "Pendentes",
                valueFormat = EventBiValueFormat.Number,
                secondaryFormat = EventBiValueFormat.Number,
                sortBy = EventBiComboSort.None,
            )
        }

        EventBiChartPanel(
            title = "Retirada por produto",
            subtitle = "Barras 100%: retirado, pendente, parcial e cancelado",
            footer = legendFooter,
        ) {
            EventBiStackedPercentChart(data = sales.productWithdrawalRows)
        }

        EventBiChartPanel(
            title = "Retirada por categoria",
            subtitle = "Qual categoria entrega melhor",
            footer = legendFooter,
        ) {
            EventBiStackedPercentChart(data = sales.categoryWithdrawalRows)
        }

        EventBiChartPanel(
            title = "Receita por categoria",
            subtitle = "Rosca de produtos/fichas/bar",
            toolbar = {
                CategoryRevenueToolbar(
                    selected = categoryRevenueMode,
                    onSelect = { categoryRevenueMode = it },
                )
            },
        ) {
            EventBiPieMetric(
                data = sales.categoryRows,
                dataKey = categoryRevenueMode,
                valueName = if (categoryRevenueMode == EventBiMetricKey.Value) "Receita" else "Itens",
                valueFormat = if (categoryRevenueMode == EventBiMetricKey.Value) {
                    EventBiValueFormat.Currency
                } else {
                    EventBiValueFormat.Number
                },
            )
        }

        EventBiChartPanel(
            title = "Risco operacional do produto",
            subtitle = "Volume, receita, pendência, manualidade, atraso e auditoria",
            info = "Volume: compara os itens vendidos no Modo Vendas com o maior volume do recorte.\n" +
                "Receita: participação da receita de produtos na receita total aprovada.\n" +
                "Pendência: percentual de itens aprovados que ainda não foram retirados.\n" +
                "Manualidade: percentual de baixas feitas manualmente.\n" +
                "Atraso: maior tempo de espera entre aprovação e retirada pendente, usando 24h como " +
                "referência.\nAuditoria: quantidade de alertas de auditoria dividida pelos pedidos " +
                "aprovados de produto.",
        ) {
            EventBiRadarMetric(data = sales.productRiskRadarRows)
        }

        EventBiChartPanel(
            title = "Método de retirada",
            subtitle = "QR, manual, código curto, documento ou lista",
        ) {
            EventBiPieMetric(data = sales.withdrawalMethodRows)
        }

        EventBiChartPanel(
            title = "Erros de QR na retirada",
            subtitle = "QR ausente, duplicado ou status incoerente",
        ) {
            EventBiBars(data = sales.withdrawalErrorRows)
        }

        EventBiChartPanel(
            title = "Operador x método",
            subtitle = "Heatmap de baixa por pessoa e método",
        ) {
            EventBiHeatmapMetric(data = sales.operatorMethodHeatmapRows)
        }

        EventBiChartPanel(
            title = "Retiradas por operador",
            subtitle = "Baixas e valor entregue",
        ) {
            EventBiBarsDual(
                data = sales.withdrawalOperatorRows,
                valueName = "Valor entregue",
                quantityName = "Baixas",
            )
        }

        EventBiChartPanel(
            title = "Origem do pedido com qualidade",
            subtitle = "Checkout, manual, admin, PDV/bar e cortesia",
            footer = legendFooter,
        ) {
            // `.map(row => ({ ..., cancelado: 0 }))` (7449): a origem nunca mostra cancelado.
            EventBiStackedPercentChart(
                data = sales.orderSourceQualityRows.map { row ->
                    EventBiTableRow(
                        linkedMapOf(
                            "name" to row["name"],
                            "retirado" to row["retirado"],
                            "pendente" to row["pendente"],
                            "parcial" to row["parcial"],
                            "cancelado" to 0.0,
                        ),
                    )
                },
            )
        }

        EventBiChartPanel(
            title = "Fonte de pagamento",
            subtitle = "Receita por fonte para conciliação",
        ) {
            EventBiPieMetric(data = sales.paymentSourceRows)
        }

        EventBiChartPanel(
            title = "Descontos com impacto real",
            subtitle = "Receita bruta, líquida e desconto",
        ) {
            EventBiBarsDual(
                data = sales.discountDetailedRows,
                valueName = "Valor",
                quantityName = "Pedidos",
            )
        }

        EventBiChartPanel(
            title = "Venda por horário",
            subtitle = "Itens vendidos, receita e retiradas",
            footer = legendFooter,
        ) {
            EventBiComboBarsLines(
                data = sales.salesWithdrawalTimelineRows,
                barName = "Itens vendidos",
                lineOneName = "Itens retirados",
                lineTwoName = "Pendentes",
                valueFormat = EventBiValueFormat.Number,
                secondaryFormat = EventBiValueFormat.Number,
                sortBy = EventBiComboSort.None,
            )
        }

        EventBiChartPanel(
            title = "Mapa produto x horário",
            subtitle = "Heatmap de quantidade vendida por hora",
        ) {
            EventBiHeatmapMetric(
                data = sales.productHourHeatmapRows,
                columnOrder = EventBiHeatmapOrder.Hour,
            )
        }

        EventBiChartPanel(
            title = "Estoque e ruptura",
            subtitle = "Vendido, retirado, pendente e disponível",
        ) {
            EventBiComboBarsLines(
                data = sales.turnoverRows,
                barName = "Vendido",
                lineOneName = "Giro do estoque",
                lineTwoName = "Disponível",
                valueFormat = EventBiValueFormat.Percent,
                secondaryFormat = EventBiValueFormat.Number,
            )
        }

        EventBiChartPanel(
            title = "Combos e venda cruzada",
            subtitle = "Produtos comprados juntos",
        ) {
            EventBiNetworkMetric(data = sales.crossSellRows)
        }

        EventBiChartPanel(
            title = "Ticket do modo vendas",
            subtitle = "Histograma de ticket por cliente em produto/bar",
        ) {
            EventBiBarsDual(
                data = sales.productTicketHistogramRows,
                valueName = "Receita",
                quantityName = "Clientes",
            )
        }

        EventBiChartPanel(
            title = "Transferência no modo vendas",
            subtitle = "Origem, destino e status de retirada",
        ) {
            EventBiHeatmapMetric(data = sales.productTransferRows)
        }

        EventBiChartPanel(
            title = "Status QR (Quick Response)",
            subtitle = "Ativo, utilizado, cancelado, expirado, inválido ou sem QR",
        ) {
            EventBiPieMetric(data = sales.qrStatusRows)
        }

        EventBiChartPanel(
            title = "Receita aprovada x entregue",
            subtitle = "Cascata operacional do modo vendas",
        ) {
            EventBiWaterfallMetric(data = sales.salesWaterfallRows)
        }

        EventBiDataTable(
            title = "Produtos, fichas e bar",
            rows = sales.productRows,
            columns = listOf(
                EventBiColumn("produto", "Produto"),
                EventBiColumn("itens", "Itens", EventBiColumnFormat.Number),
                EventBiColumn("receita", "Receita", EventBiColumnFormat.Currency),
                EventBiColumn("ticket", "Ticket", EventBiColumnFormat.Currency),
                EventBiColumn("retirados", "Retirados", EventBiColumnFormat.Number),
                EventBiColumn("pendentes", "Pendentes", EventBiColumnFormat.Number),
            ),
            pageSize = 20,
        )

        EventBiDataTable(
            title = "Clientes com produto pendente",
            rows = sales.pendingProductDetailRows,
            columns = listOf(
                EventBiColumn("cliente", "Cliente", hrefKey = "href"),
                EventBiColumn("produto", "Produto", hrefKey = "href"),
                EventBiColumn("quantidade", "Qtd.", EventBiColumnFormat.Number),
                EventBiColumn("valor", "Valor", EventBiColumnFormat.Currency),
                EventBiColumn("aprovadoEm", "Aprovado em"),
                EventBiColumn("tempo", "Tempo (h)", EventBiColumnFormat.Decimal),
                EventBiColumn("origem", "Origem"),
                EventBiColumn("aprovador", "Aprovador"),
                EventBiColumn("qr", "Status QR"),
            ),
            pageSize = 20,
        )

        EventBiDataTable(
            title = "Retiradas parciais",
            rows = sales.partialWithdrawalRows,
            columns = listOf(
                EventBiColumn("pedido", "Pedido", hrefKey = "href"),
                EventBiColumn("cliente", "Cliente"),
                EventBiColumn("produto", "Produto"),
                EventBiColumn("vendido", "Vendido", EventBiColumnFormat.Number),
                EventBiColumn("retirado", "Retirado", EventBiColumnFormat.Number),
                EventBiColumn("pendente", "Pendente", EventBiColumnFormat.Number),
                EventBiColumn("saldo", "Saldo", EventBiColumnFormat.Currency),
            ),
            pageSize = 20,
        )

        EventBiDataTable(
            title = "Qualidade por operador de retirada",
            rows = sales.operatorSalesRows,
            columns = listOf(
                EventBiColumn("operador", "Operador", hrefKey = "href"),
                EventBiColumn("baixas", "Baixas", EventBiColumnFormat.Number, "href"),
                EventBiColumn("valor", "Valor entregue", EventBiColumnFormat.Currency),
                EventBiColumn("manualidade", "Manualidade", EventBiColumnFormat.Percent),
                EventBiColumn("mediana", "Mediana (h)", EventBiColumnFormat.Decimal),
                EventBiColumn("conflitos", "Conflitos", EventBiColumnFormat.Number),
            ),
            pageSize = 20,
        )

        EventBiDataTable(
            title = "Auditoria de conflito de função",
            rows = sales.conflictAuditRows,
            columns = listOf(
                EventBiColumn("gravidade", "Gravidade", hrefKey = "href"),
                EventBiColumn("pedido", "Pedido", hrefKey = "href"),
                EventBiColumn("cliente", "Cliente"),
                EventBiColumn("produto", "Produto"),
                EventBiColumn("valor", "Valor", EventBiColumnFormat.Currency),
                EventBiColumn("criado", "Criado por"),
                EventBiColumn("aprovado", "Aprovado por"),
                EventBiColumn("baixado", "Baixado por"),
            ),
            pageSize = 20,
        )

        EventBiDataTable(
            title = "Fonte de pagamento e inconsistências",
            rows = sales.paymentIssueRows,
            columns = listOf(
                EventBiColumn("problema", "Problema", width = 260),
                EventBiColumn("quantidade", "Quantidade", EventBiColumnFormat.Number),
            ),
        )

        EventBiDataTable(
            title = "Estoque preparado para integração",
            rows = sales.stockRows,
            columns = listOf(
                EventBiColumn("produto", "Produto", hrefKey = "href"),
                EventBiColumn("estoque", "Estoque", EventBiColumnFormat.Number),
                EventBiColumn("vendido", "Vendido", EventBiColumnFormat.Number),
                EventBiColumn("retirado", "Retirado", EventBiColumnFormat.Number),
                EventBiColumn("pendente", "Pendente", EventBiColumnFormat.Number),
                EventBiColumn("disponivel", "Disponível", EventBiColumnFormat.Number),
                EventBiColumn("ruptura", "Ruptura", EventBiColumnFormat.Number),
            ),
            pageSize = 20,
        )

        EventBiDataTable(
            title = "Tabela de auditoria melhorada",
            rows = sales.improvedAuditRows,
            columns = listOf(
                EventBiColumn("gravidade", "Gravidade", hrefKey = "href"),
                EventBiColumn("alerta", "Alerta", hrefKey = "href", width = 220),
                EventBiColumn("quantidade", "Quantidade", EventBiColumnFormat.Number, "href"),
                EventBiColumn("acao", "Filtro", hrefKey = "href"),
            ),
            pageSize = 20,
        )
    }
}

/** O alternador Valor/Itens do painel "Receita por categoria" (7402). */
@Composable
private fun CategoryRevenueToolbar(
    selected: EventBiMetricKey,
    onSelect: (EventBiMetricKey) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = Color.Black.copy(alpha = 0.40f),
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Row {
            listOf(EventBiMetricKey.Value to "Valor", EventBiMetricKey.Quantity to "Itens")
                .forEach { (key, label) ->
                    val active = key == selected
                    Surface(
                        modifier = Modifier.clickable { onSelect(key) },
                        color = if (active) PremiumBrand else Color.Transparent,
                    ) {
                        Text(
                            text = label.uppercase(),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            color = if (active) Color.Black else PremiumZinc500,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
        }
    }
}
