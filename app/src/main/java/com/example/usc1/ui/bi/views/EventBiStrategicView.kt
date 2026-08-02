package com.example.usc1.ui.bi.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.usc1.domain.model.EventBiAnalytics
import com.example.usc1.domain.model.EventBiValueFormat
import com.example.usc1.domain.model.formatEventBiCurrency
import com.example.usc1.domain.model.formatEventBiDecimal
import com.example.usc1.domain.model.formatEventBiNumber
import com.example.usc1.domain.model.formatEventBiPercent
import com.example.usc1.domain.model.eventBiStrategicCostHint
import com.example.usc1.domain.model.eventBiStrategicScoreLabel
import com.example.usc1.domain.model.safeDivide
import com.example.usc1.ui.bi.charts.EventBiBarsDual
import com.example.usc1.ui.bi.charts.EventBiBubbleMetric
import com.example.usc1.ui.bi.charts.EventBiChartPanel
import com.example.usc1.ui.bi.charts.EventBiColumn
import com.example.usc1.ui.bi.charts.EventBiColumnBars
import com.example.usc1.ui.bi.charts.EventBiColumnFormat
import com.example.usc1.ui.bi.charts.EventBiComboBarsLines
import com.example.usc1.ui.bi.charts.EventBiComboSort
import com.example.usc1.ui.bi.charts.EventBiDataTable
import com.example.usc1.ui.bi.charts.EventBiFunnelMetric
import com.example.usc1.ui.bi.charts.EventBiHeatmapMetric
import com.example.usc1.ui.bi.charts.EventBiKpiCard
import com.example.usc1.ui.bi.charts.EventBiKpiGrid
import com.example.usc1.ui.bi.charts.EventBiParetoMetric
import com.example.usc1.ui.bi.charts.EventBiPieMetric
import com.example.usc1.ui.bi.charts.EventBiRadarMetric
import com.example.usc1.ui.bi.charts.EventBiScoreGauge
import com.example.usc1.ui.bi.charts.EventBiTreemapMetric
import com.example.usc1.ui.bi.charts.EventBiWaterfallMetric

/**
 * Visão estratégica do BI de Eventos (M8.2).
 *
 * Fonte: `web-reference/src/app/admin/bi/_components/AdminEventBiDashboard.tsx`, linhas 7174-7319
 * (`{view === "estrategico" ? ... : null}`).
 *
 * O bloco `{false && view === "estrategico"}` (7321-7354) **não** foi portado: está desabilitado
 * no web e nunca renderiza.
 */
@Composable
fun EventBiStrategicView(
    analytics: EventBiAnalytics,
    modifier: Modifier = Modifier,
) {
    val totals = analytics.totals
    val strategic = analytics.strategic

    val strategicScoreLabel = eventBiStrategicScoreLabel(strategic.strategicScore)
    val strategicCostHint = eventBiStrategicCostHint(
        eventCostsTotal = strategic.eventCostsTotal,
        hasEventCostsField = strategic.hasEventCostsField,
    )

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        EventBiKpiGrid {
            EventBiKpiCard(
                label = "Receita total",
                value = formatEventBiCurrency(totals.grossRevenue),
                hint = "Ingressos + produtos do Modo Vendas aprovados",
                icon = BiIcon.DollarSign,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Receita de ingressos",
                value = formatEventBiCurrency(totals.ticketRevenue),
                hint = "${formatEventBiPercent(strategic.ticketRevenueShare)} da receita",
                icon = BiIcon.Ticket,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Receita de produtos",
                value = formatEventBiCurrency(totals.productRevenue),
                hint = "${formatEventBiPercent(strategic.productRevenueShare)} da receita",
                icon = BiIcon.Package,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Score estratégico",
                value = strategicScoreLabel,
                hint = strategic.strategicDecision,
                icon = BiIcon.Target,
                modifier = Modifier.weight(1f),
            )
        }

        EventBiKpiGrid {
            EventBiKpiCard(
                label = "Receita por comprador",
                value = formatEventBiCurrency(strategic.totalRevenuePerBuyer),
                hint = "${formatEventBiNumber(strategic.uniqueBuyers.toDouble())} compradores únicos",
                icon = BiIcon.Users,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Receita por check-in",
                value = formatEventBiCurrency(strategic.totalRevenuePerPresent),
                hint = "${formatEventBiCurrency(strategic.productRevenuePerPresent)} em produtos do Modo Vendas",
                icon = BiIcon.TrendingUp,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Taxa check-in → produto",
                value = formatEventBiPercent(
                    // `Math.max(checkedInTicketBuyerCount, 1)` (7185).
                    safeDivide(
                        strategic.productPresentBuyerIds.toDouble(),
                        maxOf(strategic.checkedInTicketBuyerCount, 1).toDouble(),
                    ) * 100,
                ),
                hint = "${formatEventBiNumber(strategic.productPresentBuyerIds.toDouble())} " +
                    "usuários com check-in e produto",
                icon = BiIcon.ShoppingBag,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Produto por check-in",
                value = formatEventBiDecimal(strategic.productPerPresent),
                hint = "Itens vendidos no Modo Vendas por check-in",
                icon = BiIcon.BarChart3,
                modifier = Modifier.weight(1f),
            )
        }

        EventBiKpiGrid {
            EventBiKpiCard(
                label = "Compradores recorrentes",
                value = formatEventBiPercent(strategic.strategicRecurringRate),
                hint = "${formatEventBiNumber(strategic.strategicRecurringBuyers.toDouble())} " +
                    "recorrentes em eventos anteriores",
                icon = BiIcon.TrendingUp,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Público sem consumo",
                value = formatEventBiNumber(strategic.ticketWithoutProduct.toDouble()),
                hint = "Fez check-in, mas não comprou produto",
                icon = BiIcon.AlertTriangle,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Produtos sem ingresso",
                value = formatEventBiNumber(strategic.productWithoutTicket.toDouble()),
                hint = "Compra de produto sem ingresso no recorte",
                icon = BiIcon.Package,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Resultado com custo",
                value = formatEventBiCurrency(strategic.resultWithoutCosts - strategic.eventCostsTotal),
                hint = strategicCostHint,
                icon = BiIcon.DollarSign,
                modifier = Modifier.weight(1f),
            )
        }

        EventBiChartPanel(
            title = "Score estratégico",
            subtitle = "0 a 100 para repetir, ajustar ou repensar",
            info = "O score é a média ponderada dos eventos do recorte.\n\n" +
                "Por evento, a nota usa: receita total (20%), volume de check-ins (15%), taxa de " +
                "check-in sobre ingressos aprovados (15%), receita de produto por check-in (15%), " +
                "conversão de compradores de ingresso para produto (15%), recorrência (8%), baixa " +
                "manual/operacionalidade (6%) e pendências de retirada (6%).\n\n" +
                "0-39 = repensar, 40-69 = ajustar, 70-84 = repetir, 85-100 = repetir e escalar.",
        ) {
            EventBiScoreGauge(score = strategic.strategicScore, label = strategic.strategicDecision)
        }

        EventBiChartPanel(
            title = "Receita por origem",
            subtitle = "Barras: ingressos x produtos do Modo Vendas",
        ) {
            EventBiColumnBars(
                data = strategic.revenueOriginRows,
                valueName = "Receita",
                valueFormat = EventBiValueFormat.Currency,
            )
        }

        EventBiChartPanel(
            title = "Receita por check-in",
            subtitle = "Check-ins, receita total por check-in e produto por check-in",
        ) {
            EventBiComboBarsLines(
                data = strategic.revenuePerPresentRows,
                barName = "Check-ins",
                lineOneName = "Receita por check-in",
                lineTwoName = "Produto por check-in",
                valueFormat = EventBiValueFormat.Currency,
                secondaryFormat = EventBiValueFormat.Currency,
            )
        }

        EventBiChartPanel(
            title = "Funil ingresso → produto",
            subtitle = "Usuários em cascata: aprovado → check-in → compra de produto → retirada",
        ) {
            EventBiFunnelMetric(data = strategic.strategicFunnelRows)
        }

        EventBiChartPanel(
            title = "Matriz evento x resultado",
            subtitle = "X: presença, Y: receita, bolha: receita de produtos do Modo Vendas, cor: score",
            info = "Cada bolinha é um evento. O eixo X mostra a taxa de check-in: check-ins divididos " +
                "por ingressos aprovados. O eixo Y mostra a receita total do evento. O tamanho da " +
                "bolha é a receita de produtos do Modo Vendas. A cor vem do score estratégico: " +
                "vermelho repensar, amarelo ajustar, azul repetir, verde escalar.",
        ) {
            EventBiBubbleMetric(data = strategic.strategicBubbleRows)
        }

        EventBiChartPanel(
            title = "Radar hexagonal",
            subtitle = "Ingresso, check-in, produtos, recorrência, operação e auditoria",
            info = "Ingresso: participação da receita de ingressos na receita total.\n" +
                "Check-in: check-ins divididos por ingressos aprovados.\n" +
                "Produtos: participação da receita de produtos do Modo Vendas na receita total.\n" +
                "Recorrência: compradores recorrentes divididos pelos compradores únicos.\n" +
                "Operação: reduz a nota quando há muitos alertas operacionais por pedido aprovado.\n" +
                "Auditoria: reduz a nota com duplicidade, leitura inválida e alerta operacional.",
        ) {
            EventBiRadarMetric(data = strategic.strategicRadarRows)
        }

        EventBiChartPanel(
            title = "Pareto de produtos",
            subtitle = "Ordem do mais vendido para o menos vendido, com acumulado",
        ) {
            EventBiParetoMetric(data = strategic.attachRateRows)
        }

        EventBiChartPanel(
            title = "Produtos que tornam eventos melhores",
            subtitle = "Heatmap evento x produto por receita",
        ) {
            EventBiHeatmapMetric(data = strategic.eventProductHeatmapRows)
        }

        EventBiChartPanel(
            title = "Categoria estratégica",
            subtitle = "Heatmap evento x categoria por receita",
        ) {
            EventBiHeatmapMetric(data = strategic.eventCategoryHeatmapRows)
        }

        EventBiChartPanel(
            title = "Antecedência de ingressos",
            subtitle = "Somente ingressos aprovados, sem misturar produtos",
        ) {
            EventBiComboBarsLines(
                data = strategic.ticketLeadRows,
                barName = "Ingressos",
                lineOneName = "Receita de ingressos",
                valueFormat = EventBiValueFormat.Currency,
                secondaryFormat = EventBiValueFormat.Number,
                sortBy = EventBiComboSort.None,
            )
        }

        EventBiChartPanel(
            title = "Antecedência de produtos",
            subtitle = "Somente produtos do Modo Vendas",
        ) {
            EventBiComboBarsLines(
                data = strategic.productLeadRows,
                barName = "Itens",
                lineOneName = "Receita de produtos",
                valueFormat = EventBiValueFormat.Currency,
                secondaryFormat = EventBiValueFormat.Number,
                sortBy = EventBiComboSort.None,
            )
        }

        EventBiChartPanel(
            title = "Novos x recorrentes",
            subtitle = "Baseado em eventos anteriores ao evento filtrado",
            info = "Ingresso novo: usuário que fez check-in no evento filtrado e não tinha check-in " +
                "em evento anterior.\nIngresso recorrente: usuário que fez check-in no evento " +
                "filtrado e também tinha check-in em evento anterior.\nProduto novo: usuário que " +
                "comprou produto no Modo Vendas do evento filtrado e nunca tinha comprado produto em " +
                "evento anterior.\nProduto recorrente: usuário que comprou produto no Modo Vendas do " +
                "evento filtrado e já tinha comprado produto em evento anterior.",
        ) {
            EventBiPieMetric(data = strategic.recurrenceDetailRows)
        }

        EventBiChartPanel(
            title = "Participação na tenant",
            subtitle = "Percentual da base total de usuários com check-in no evento",
        ) {
            EventBiBarsDual(
                data = strategic.tenantParticipationRows,
                valueName = "% da tenant",
                quantityName = "Usuários",
                valueFormat = EventBiValueFormat.Percent,
                quantityFormat = EventBiValueFormat.Number,
            )
        }

        EventBiChartPanel(
            title = "Ticket por cliente",
            subtitle = "Histograma de receita total por cliente",
        ) {
            EventBiBarsDual(
                data = strategic.customerTicketHistogramRows,
                valueName = "Receita",
                quantityName = "Clientes",
            )
        }

        EventBiChartPanel(
            title = "Turma que comparece e consome",
            subtitle = "Check-ins com linha de consumo médio",
        ) {
            EventBiComboBarsLines(
                data = strategic.classConsumptionRows,
                barName = "Check-ins",
                lineOneName = "Consumo médio",
                lineTwoName = "No-show",
                valueFormat = EventBiValueFormat.Currency,
                secondaryFormat = EventBiValueFormat.Number,
            )
        }

        EventBiChartPanel(
            title = "Lote que comparece e consome",
            subtitle = "Presença por lote com consumo médio",
        ) {
            EventBiComboBarsLines(
                data = strategic.lotConsumptionRows,
                barName = "Check-ins",
                lineOneName = "Consumo médio",
                lineTwoName = "No-show",
                valueFormat = EventBiValueFormat.Currency,
                secondaryFormat = EventBiValueFormat.Number,
            )
        }

        EventBiChartPanel(
            title = "Origem que traz resultado",
            subtitle = "Treemap por receita e qualidade de presença/retirada",
        ) {
            EventBiTreemapMetric(data = strategic.sourceTreemapRows)
        }

        EventBiChartPanel(
            title = "Impacto de descontos",
            subtitle = "Com desconto versus sem desconto",
        ) {
            EventBiBarsDual(
                data = strategic.discountImpactRows,
                valueName = "Receita",
                quantityName = "Itens",
            )
        }

        EventBiChartPanel(
            title = "Estratégia de preço",
            subtitle = "Preço médio, presença/retirada e receita",
            info = "Cada bolinha representa uma faixa de preço de ingresso ou um produto do Modo " +
                "Vendas. O eixo X é o preço médio. O eixo Y é a conversão: presença para ingressos e " +
                "retirada para produtos. O tamanho da bolha é a receita gerada. A cor mostra o score " +
                "relativo dentro do recorte.",
        ) {
            EventBiBubbleMetric(
                data = strategic.priceStrategyRows,
                xLabel = "Preço médio",
                yLabel = "Conversão",
                xFormat = EventBiValueFormat.Currency,
                yFormat = EventBiValueFormat.Percent,
            )
        }

        EventBiChartPanel(
            title = "Forecast estratégico",
            subtitle = "Linha real e projeção simples",
            info = "Pedidos são os ingressos e produtos aprovados por dia, em ordem cronológica. Real " +
                "acumulado soma a receita realizada até cada dia. Projetado usa uma projeção simples: " +
                "se existe uma receita esperada pelo ritmo atual, distribui a diferença restante " +
                "pelos dias do recorte; se não existe base suficiente, a linha projetada acompanha o " +
                "realizado. Não é previsão estatística avançada.",
        ) {
            EventBiComboBarsLines(
                data = strategic.forecastRows,
                barName = "Pedidos",
                lineOneName = "Real acumulado",
                lineTwoName = "Projetado",
                valueFormat = EventBiValueFormat.Currency,
                secondaryFormat = EventBiValueFormat.Currency,
                sortBy = EventBiComboSort.None,
            )
        }

        EventBiChartPanel(
            title = "Resultado com custo",
            subtitle = "Cascata de receita, descontos, custos e resultado",
        ) {
            EventBiWaterfallMetric(data = strategic.resultWaterfallRows)
        }

        EventBiDataTable(
            title = "Ranking de eventos para repetir, ajustar ou cancelar",
            rows = strategic.strategicEventRows,
            columns = listOf(
                EventBiColumn("evento", "Evento", hrefKey = "href"),
                EventBiColumn("ingressos", "Ingressos", EventBiColumnFormat.Number),
                EventBiColumn("checkins", "Check-ins", EventBiColumnFormat.Number),
                EventBiColumn("presenca", "Taxa check-in", EventBiColumnFormat.Percent),
                EventBiColumn("receitaIngressos", "Receita ingressos", EventBiColumnFormat.Currency),
                EventBiColumn("receitaProdutos", "Receita produtos", EventBiColumnFormat.Currency, "hrefProdutos"),
                EventBiColumn("receitaTotal", "Receita total", EventBiColumnFormat.Currency),
                EventBiColumn("produtoPorPresente", "Produto/check-in", EventBiColumnFormat.Currency),
                EventBiColumn("ticketTotalCliente", "Ticket cliente", EventBiColumnFormat.Currency),
                EventBiColumn("retirada", "Retirada", EventBiColumnFormat.Percent),
                EventBiColumn("pendencias", "Pendências", EventBiColumnFormat.Number, "hrefProdutos"),
                EventBiColumn("score", "Score", EventBiColumnFormat.Number),
                EventBiColumn("decisao", "Decisão"),
                EventBiColumn("motivo", "Motivo", width = 240),
            ),
            pageSize = 20,
        )

        EventBiDataTable(
            title = "Top 5 clientes que mais gastaram por evento",
            rows = strategic.topCustomersByEventRows,
            columns = listOf(
                EventBiColumn("evento", "Evento"),
                EventBiColumn("posicao", "#", EventBiColumnFormat.Number, width = 56),
                EventBiColumn("cliente", "Cliente", hrefKey = "href"),
                EventBiColumn("receitaTotal", "Receita total", EventBiColumnFormat.Currency),
                EventBiColumn("ingresso", "Ingresso", EventBiColumnFormat.Currency),
                EventBiColumn("produto", "Produto", EventBiColumnFormat.Currency),
                EventBiColumn("itens", "Itens", EventBiColumnFormat.Number),
            ),
            pageSize = 25,
        )
    }
}
