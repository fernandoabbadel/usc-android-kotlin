package com.example.usc1.ui.bi.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.usc1.domain.model.EventBiAnalytics
import com.example.usc1.domain.model.EventBiValueFormat
import com.example.usc1.domain.model.formatEventBiDecimal
import com.example.usc1.domain.model.formatEventBiNumber
import com.example.usc1.domain.model.formatEventBiPercent
import com.example.usc1.ui.bi.charts.EventBiBars
import com.example.usc1.ui.bi.charts.EventBiBarsDual
import com.example.usc1.ui.bi.charts.EventBiChartPanel
import com.example.usc1.ui.bi.charts.EventBiColumn
import com.example.usc1.ui.bi.charts.EventBiColumnFormat
import com.example.usc1.ui.bi.charts.EventBiDataTable
import com.example.usc1.ui.bi.charts.EventBiKpiCard
import com.example.usc1.ui.bi.charts.EventBiKpiGrid
import com.example.usc1.ui.bi.charts.EventBiPieMetric

/**
 * Visão operacional do BI de Eventos (M8.2).
 *
 * Fonte: `web-reference/src/app/admin/bi/_components/AdminEventBiDashboard.tsx`, linhas 6847-6986
 * (`{view === "operacional" ? ... : null}`).
 */
@Composable
fun EventBiOperationalView(
    analytics: EventBiAnalytics,
    modifier: Modifier = Modifier,
) {
    val operational = analytics.operational
    val commercial = analytics.commercial

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        EventBiKpiGrid {
            EventBiKpiCard(
                label = "Pendentes agora",
                value = formatEventBiNumber(operational.operationalPendingCount.toDouble()),
                hint = "Ingressos, produtos, cortesia, transferência e cadastro manual",
                icon = BiIcon.Clock3,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Tempo médio até aprovação",
                value = "${formatEventBiDecimal(operational.operationalApprovalAverage)}h",
                hint = "Pedido criado até aprovação",
                icon = BiIcon.Clock3,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Mediana / P90",
                value = "${formatEventBiDecimal(operational.operationalApprovalMedian)}h / " +
                    "${formatEventBiDecimal(operational.operationalApprovalP90)}h",
                hint = "Centro da fila e piores casos",
                icon = BiIcon.BarChart3,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Maior espera pendente",
                value = "${formatEventBiDecimal(operational.operationalMaxPendingHours)}h",
                hint = "Pedido ainda aguardando aprovação",
                icon = BiIcon.AlertTriangle,
                modifier = Modifier.weight(1f),
            )
        }

        EventBiKpiGrid {
            EventBiKpiCard(
                label = "Aprovado em até 5 min",
                value = formatEventBiPercent(operational.operationalApprovedWithin5m),
                hint = "Até 15 min: ${formatEventBiPercent(operational.operationalApprovedWithin15m)}",
                icon = BiIcon.CheckCircle2,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Aprovado em até 1h",
                value = formatEventBiPercent(operational.operationalApprovedWithin1h),
                hint = "Até 24h: ${formatEventBiPercent(operational.operationalApprovedWithin24h)}",
                icon = BiIcon.CheckCircle2,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Operadores ativos",
                value = formatEventBiNumber(operational.activeOperatorCount.toDouble()),
                hint = "${formatEventBiNumber(operational.outsideHoursApprovals.toDouble())} " +
                    "aprovações fora do horário esperado",
                icon = BiIcon.Users,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Dependência top 1",
                value = formatEventBiPercent(operational.topApproverDependency),
                hint = "Top 3: ${formatEventBiPercent(operational.top3ApproverDependency)}",
                icon = BiIcon.Users,
                info = "Percentual de aprovações feitas pelo principal aprovador. Quanto maior, " +
                    "maior a dependência operacional em uma pessoa.",
                modifier = Modifier.weight(1f),
            )
        }

        EventBiKpiGrid {
            EventBiKpiCard(
                label = "Próximos do evento",
                value = formatEventBiNumber(operational.operationalPendingNearEvent.toDouble()),
                hint = "Pendentes com evento nas próximas 24h",
                icon = BiIcon.AlertTriangle,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Criados na porta",
                value = formatEventBiNumber(operational.operationalPendingAtDoor.toDouble()),
                hint = "Pendentes originados em entrada/manual",
                icon = BiIcon.Ticket,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Aprovados sem QR/código",
                value = formatEventBiNumber(operational.approvedWithoutCodeCount.toDouble()),
                hint = "Aprovados sem identificador operacional",
                icon = BiIcon.QrCode,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "QR/código sem uso",
                value = formatEventBiNumber(operational.codeWithoutUseCount.toDouble()),
                hint = "Aprovados com código, mas sem entrada/retirada",
                icon = BiIcon.Package,
                modifier = Modifier.weight(1f),
            )
        }

        EventBiKpiGrid {
            EventBiKpiCard(
                label = "Aprovação até entrada",
                value = "${formatEventBiDecimal(operational.approvalToEntryMedian)}h",
                hint = "Mediana entre aprovação e check-in",
                icon = BiIcon.QrCode,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Aprovação até retirada",
                value = "${formatEventBiDecimal(operational.approvalToWithdrawalMedian)}h",
                hint = "Mediana entre aprovação e baixa de produto",
                icon = BiIcon.Package,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Uso sem aprovação",
                value = formatEventBiNumber(operational.usedWithoutApprovalCount.toDouble()),
                hint = "Entrada/retirada sem aprovação clara",
                icon = BiIcon.AlertTriangle,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Status incoerente",
                value = formatEventBiNumber(operational.inconsistentStatusCount.toDouble()),
                hint = "${formatEventBiNumber(operational.approvedNearEventCount.toDouble())} " +
                    "aprovados perto do evento",
                icon = BiIcon.AlertTriangle,
                modifier = Modifier.weight(1f),
            )
        }

        EventBiChartPanel(title = "Fila por evento", subtitle = "Pedidos pendentes agora e valor parado") {
            EventBiBarsDual(
                data = operational.operationalPendingByEventRows,
                valueName = "Valor parado",
                quantityName = "Pendentes",
            )
        }

        EventBiChartPanel(
            title = "Fila por tipo",
            subtitle = "Ingresso, produto, cortesia, transferência e cadastro manual",
        ) {
            EventBiPieMetric(data = operational.operationalPendingByTypeRows)
        }

        EventBiChartPanel(title = "Idade da fila", subtitle = "Tempo real aguardando aprovação") {
            EventBiBars(data = operational.operationalPendingAgeRows)
        }

        EventBiChartPanel(
            title = "Distribuição por operador",
            subtitle = "Volume aprovado e valor processado",
        ) {
            EventBiBarsDual(data = operational.operatorDistributionRows)
        }

        EventBiChartPanel(
            title = "Método de aprovação",
            subtitle = "Manual, automático, Pix, cortesia, transferência ou admin",
        ) {
            EventBiPieMetric(data = commercial.approvalMethodRows)
        }

        EventBiChartPanel(
            title = "SLA por origem",
            subtitle = "Clique na origem para abrir o extrato com os itens correspondentes",
        ) {
            EventBiBarsDual(
                data = operational.slaBySourceRows,
                valueName = "Mediana",
                quantityName = "Aprovados",
                valueFormat = EventBiValueFormat.Hours,
            )
        }

        EventBiChartPanel(
            title = "SLA por aprovador",
            subtitle = "Mediana de aprovação e volume por operador",
        ) {
            EventBiBarsDual(
                data = operational.slaByApproverRows,
                valueName = "Mediana",
                quantityName = "Aprovados",
                valueFormat = EventBiValueFormat.Hours,
            )
        }

        EventBiChartPanel(
            title = "SLA por evento",
            subtitle = "Compara velocidade operacional entre eventos",
        ) {
            EventBiBarsDual(
                data = operational.slaByEventRows,
                valueName = "Mediana",
                quantityName = "Aprovados",
                valueFormat = EventBiValueFormat.Hours,
            )
        }

        EventBiDataTable(
            title = "Qualidade por operador",
            rows = operational.operatorQualityRows,
            columns = listOf(
                EventBiColumn("evento", "Evento", hrefKey = "href"),
                EventBiColumn("operador", "Operador", hrefKey = "href"),
                EventBiColumn("aprovados", "Aprovados", EventBiColumnFormat.Number, "hrefAprovados"),
                EventBiColumn("valor", "Valor aprovado", EventBiColumnFormat.Currency, "hrefValor"),
                EventBiColumn("mediana", "Mediana (h)", EventBiColumnFormat.Decimal),
                EventBiColumn("semValor", "Sem valor", EventBiColumnFormat.Number, "hrefSemValor"),
                EventBiColumn("manuais", "Manuais", EventBiColumnFormat.Number, "hrefManuais"),
                EventBiColumn("corrigidos", "Corrigidos/cancelados", EventBiColumnFormat.Number, "hrefCorrigidos"),
                EventBiColumn("semUso", "Sem entrada/retirada", EventBiColumnFormat.Number, "hrefSemUso"),
                EventBiColumn("mesmoCriador", "Mesmo criador", EventBiColumnFormat.Number, "hrefMesmoCriador"),
                EventBiColumn("mesmoBaixa", "Mesmo baixa", EventBiColumnFormat.Number, "hrefMesmoBaixa"),
            ),
            pageSize = 20,
        )

        EventBiDataTable(
            title = "Eventos operados por 1 pessoa",
            rows = operational.singleOperatorEventRows,
            columns = listOf(
                EventBiColumn("evento", "Evento", hrefKey = "href"),
                EventBiColumn("operador", "Operador"),
                EventBiColumn("aprovacoes", "Aprovações", EventBiColumnFormat.Number, "href"),
            ),
        )

        EventBiChartPanel(
            title = "Manualidade por etapa",
            subtitle = "Pedido, aprovação, check-in e retirada por tipo de item",
        ) {
            EventBiBarsDual(
                data = operational.manualityStageChartRows,
                valueName = "% do tipo",
                quantityName = "Itens",
                valueFormat = EventBiValueFormat.Percent,
            )
        }

        EventBiDataTable(
            title = "Manualidade por tipo e etapa",
            rows = operational.manualityStageRows,
            columns = listOf(
                EventBiColumn("evento", "Evento", hrefKey = "href"),
                EventBiColumn("tipo", "Tipo", hrefKey = "href"),
                EventBiColumn("etapa", "Etapa", hrefKey = "href"),
                EventBiColumn("quantidade", "Itens", EventBiColumnFormat.Number, "href"),
                EventBiColumn("valor", "Valor", EventBiColumnFormat.Currency, "href"),
                EventBiColumn("percentual", "% do tipo", EventBiColumnFormat.Percent),
            ),
            pageSize = 20,
        )

        EventBiDataTable(
            title = "Alertas operacionais com atalho para o extrato",
            rows = operational.operationalControlAlertRows,
            columns = listOf(
                EventBiColumn("alerta", "Alerta", hrefKey = "href"),
                EventBiColumn("descricao", "O que significa", width = 260),
                EventBiColumn("evento", "Evento"),
                EventBiColumn("item", "Item"),
                EventBiColumn("tipo", "Tipo"),
                EventBiColumn("quantidade", "Quantidade", EventBiColumnFormat.Number, "href"),
            ),
            pageSize = 20,
        )
    }
}
