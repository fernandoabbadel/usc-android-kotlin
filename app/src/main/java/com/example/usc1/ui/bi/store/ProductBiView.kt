package com.example.usc1.ui.bi.store

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.usc1.domain.model.EventBiMetricRow
import com.example.usc1.domain.model.EventBiTableRow
import com.example.usc1.domain.model.EventBiValueFormat
import com.example.usc1.domain.model.ProductBiAnalytics
import com.example.usc1.domain.model.ProductBiMetricRow
import com.example.usc1.domain.model.formatEventBiCurrency
import com.example.usc1.domain.model.formatEventBiNumber
import com.example.usc1.domain.model.formatEventBiPercent
import com.example.usc1.ui.bi.charts.EventBiBars
import com.example.usc1.ui.bi.charts.EventBiChartPanel
import com.example.usc1.ui.bi.charts.EventBiColumn
import com.example.usc1.ui.bi.charts.EventBiColumnFormat
import com.example.usc1.ui.bi.charts.EventBiDataTable
import com.example.usc1.ui.bi.charts.EventBiKpiCard
import com.example.usc1.ui.bi.charts.EventBiKpiGrid
import com.example.usc1.ui.bi.charts.EventBiLineMetric
import com.example.usc1.ui.bi.charts.EventBiMetricKey
import com.example.usc1.ui.bi.charts.EventBiPieMetric
import com.example.usc1.ui.bi.charts.ProductBiGroupedBars

/**
 * As oito KPIs, os nove painéis e as duas tabelas do BI Loja (M8.3).
 *
 * Fonte: `web-reference/src/components/ProductManagementAnalytics.tsx`, o corpo do componente
 * (545-633). Uma view só para os cinco players — o que muda é o [ProductBiAnalytics] que chega.
 *
 * Reaproveita o kit de gráficos do BI de Eventos (`ui/bi/charts/`); o único componente novo é o
 * [ProductBiGroupedBars], porque o `BarsDual` daqui agrupa duas barras num eixo só, enquanto o
 * do `AdminEventBiDashboard` é barra + linha em dois eixos.
 *
 * Adaptação de tela declarada, a mesma do M8.2: o web usa `xl:grid-cols-2` nos painéis e
 * `md:grid-cols-2 xl:grid-cols-4` nas KPIs; no celular o próprio web cai para coluna única. O
 * app mantém painel em coluna única e KPI em duas colunas, que é o recorte `md`.
 */
@Composable
fun ProductBiView(
    analytics: ProductBiAnalytics,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 570-575
        EventBiKpiGrid {
            EventBiKpiCard(
                label = "Receita",
                value = formatEventBiCurrency(analytics.revenue),
                hint = "${formatEventBiNumber(analytics.itemQtd)} itens vendidos",
                icon = Icons.Outlined.AttachMoney,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Pedidos aprovados",
                value = formatEventBiNumber(analytics.approvedOrderCount.toDouble()),
                hint = "pagamentos confirmados",
                icon = Icons.Outlined.ShoppingBag,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Compradores únicos",
                value = formatEventBiNumber(analytics.uniqueBuyers.toDouble()),
                hint = "clientes diferentes no filtro",
                icon = Icons.Outlined.Groups,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Valor médio",
                value = formatEventBiCurrency(analytics.averageOrder),
                hint = "receita / pedidos aprovados",
                icon = Icons.AutoMirrored.Outlined.ShowChart,
                modifier = Modifier.weight(1f),
            )
        }

        // 577-582
        EventBiKpiGrid {
            EventBiKpiCard(
                label = "Clique para compra",
                value = formatEventBiPercent(analytics.clickConversion),
                hint = "pedidos aprovados / cliques",
                icon = Icons.AutoMirrored.Outlined.TrendingUp,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Venda do estoque",
                value = formatEventBiPercent(analytics.sellThrough),
                hint = "vendidos / estoque total estimado",
                icon = Icons.Outlined.Inventory2,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Produtos parados",
                value = formatEventBiNumber(analytics.stalledCount.toDouble()),
                hint = "estoque alto, sem venda ou clique sem compra",
                icon = Icons.Outlined.WarningAmber,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Recompra",
                value = formatEventBiPercent(analytics.repeatRate),
                hint = "compradores com mais de um pedido",
                icon = Icons.Outlined.Repeat,
                modifier = Modifier.weight(1f),
            )
        }

        // 584-612: os nove painéis, na ordem do web.
        EventBiChartPanel(
            title = "Receita por produto",
            subtitle = "Qtd, receita e valor médio por item",
        ) {
            EventBiBars(
                data = analytics.byProduct.toChartRows(),
                dataKey = EventBiMetricKey.Value,
                currency = true,
            )
        }
        EventBiChartPanel(
            title = "Vendas por variação",
            subtitle = "Tamanho/cor vendidos e receita por item",
        ) {
            ProductBiGroupedBars(analytics.byVariant)
        }
        EventBiChartPanel(
            title = "Lotes por qtd e valor",
            subtitle = "Venda do estoque e receita por lote padronizado",
        ) {
            ProductBiGroupedBars(analytics.byLot)
        }
        EventBiChartPanel(
            title = "Turmas por consumo",
            subtitle = "Qtd comprada e receita por turma",
        ) {
            ProductBiGroupedBars(analytics.byClass)
        }
        EventBiChartPanel(
            title = "Dias da semana",
            subtitle = "Pedidos e receita por dia",
        ) {
            // `LineMetric` (258-271) desenha só `qtd`, ainda que o subtítulo cite receita.
            EventBiLineMetric(analytics.byWeekday.toChartRows())
        }
        EventBiChartPanel(
            title = "Likes, cliques e conversão",
            subtitle = "Interesse e eficiência por produto",
        ) {
            // `qtd` = likes e `valor` = cliques (451-455); a conversão fica em `medio`.
            ProductBiGroupedBars(
                data = analytics.engagementRows,
                quantityName = "Likes",
                valueName = "Cliques",
                valueFormat = EventBiValueFormat.Number,
            )
        }
        EventBiChartPanel(
            title = "Estoque e giro",
            subtitle = "Vendidos, estoque e taxa de saída",
        ) {
            ProductBiGroupedBars(
                data = analytics.stockRows,
                quantityName = "Vendidos",
                valueName = "Estoque",
                valueFormat = EventBiValueFormat.Number,
            )
        }
        EventBiChartPanel(
            title = "Dependência de desconto",
            subtitle = "Receita com e sem desconto",
        ) {
            EventBiPieMetric(analytics.discountRows.toChartRows())
        }
        EventBiChartPanel(
            title = "Curva ABC de produtos",
            subtitle = "Concentração de receita",
        ) {
            EventBiPieMetric(analytics.abcRows.toChartRows())
        }

        // 614-631: as duas tabelas.
        EventBiDataTable(
            title = "Produtos parados ou em risco",
            rows = analytics.stalledRows.map { row ->
                EventBiTableRow(
                    linkedMapOf(
                        "nome" to row.name,
                        "estoque" to row.qtd,
                        "cliques" to row.valor,
                        "conversao" to row.medio,
                    ),
                )
            },
            columns = listOf(
                EventBiColumn("nome", "Nome"),
                EventBiColumn("estoque", "Estoque", EventBiColumnFormat.Number),
                EventBiColumn("cliques", "Cliques", EventBiColumnFormat.Number),
                EventBiColumn("conversao", "Conversão", EventBiColumnFormat.Percent),
            ),
        )
        EventBiDataTable(
            title = "Resumo de recompra",
            rows = analytics.recurrenceRows.map { row ->
                EventBiTableRow(linkedMapOf("nome" to row.name, "compradores" to row.qtd))
            },
            columns = listOf(
                EventBiColumn("nome", "Nome"),
                EventBiColumn("compradores", "Compradores", EventBiColumnFormat.Number),
            ),
        )
    }
}

/**
 * O kit de gráficos fala `EventBiMetricRow` (`quantity`/`value`/`average`); o motor do BI Loja
 * fala `MetricRow` do web (`qtd`/`valor`/`medio`). A ponte é aqui, e só aqui.
 */
private fun List<ProductBiMetricRow>.toChartRows(): List<EventBiMetricRow> = map { row ->
    EventBiMetricRow(
        name = row.name,
        quantity = row.qtd,
        value = row.valor,
        average = row.medio,
        secondary = row.extra,
    )
}
