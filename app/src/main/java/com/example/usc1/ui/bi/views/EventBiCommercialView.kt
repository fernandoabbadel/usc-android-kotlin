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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.domain.model.EventBiAnalytics
import com.example.usc1.domain.model.EventBiAudienceBasis
import com.example.usc1.domain.model.formatEventBiCurrency
import com.example.usc1.domain.model.formatEventBiNumber
import com.example.usc1.domain.model.formatEventBiPercent
import com.example.usc1.ui.bi.charts.EventBiBars
import com.example.usc1.ui.bi.charts.EventBiBarsDual
import com.example.usc1.ui.bi.charts.EventBiChartPanel
import com.example.usc1.ui.bi.charts.EventBiKpiCard
import com.example.usc1.ui.bi.charts.EventBiKpiGrid
import com.example.usc1.ui.bi.charts.EventBiPieMetric

/**
 * Visão comercial do BI de Eventos (M8.2).
 *
 * Fonte: `web-reference/src/app/admin/bi/_components/AdminEventBiDashboard.tsx`, linhas 6771-6845
 * (`{view === "comercial" ? ... : null}`).
 */
@Composable
fun EventBiCommercialView(
    analytics: EventBiAnalytics,
    audienceBasis: EventBiAudienceBasis,
    onAudienceBasisChange: (EventBiAudienceBasis) -> Unit,
    modifier: Modifier = Modifier,
) {
    val totals = analytics.totals
    val commercial = analytics.commercial
    val sales = analytics.sales

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        EventBiKpiGrid {
            EventBiKpiCard(
                label = "Receita bruta",
                value = formatEventBiCurrency(totals.ticketRevenue),
                hint = "${formatEventBiNumber(totals.approvedTicketQuantity.toDouble())} ingressos aprovados",
                icon = BiIcon.DollarSign,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Receita líquida",
                value = formatEventBiCurrency(totals.ticketNetRevenue),
                hint = "Receita de ingressos menos descontos registrados",
                icon = BiIcon.TrendingUp,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Taxa de aprovação",
                value = formatEventBiPercent(totals.ticketApprovalRate),
                hint = "${formatEventBiNumber(totals.ticketApprovedCount.toDouble())} aprovados de " +
                    "${formatEventBiNumber(totals.ticketCreatedCount.toDouble())} pedidos",
                icon = BiIcon.CheckCircle2,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Ticket por cliente",
                value = formatEventBiCurrency(totals.ticketAverageByCustomer),
                hint = "Receita de ingressos / compradores únicos",
                icon = BiIcon.Users,
                modifier = Modifier.weight(1f),
            )
        }

        EventBiKpiGrid {
            EventBiKpiCard(
                label = "Ticket por pedido",
                value = formatEventBiCurrency(totals.ticketAverageByOrder),
                hint = "Receita de ingressos / pedidos aprovados",
                icon = BiIcon.Ticket,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Valor médio por ingresso",
                value = formatEventBiCurrency(totals.ticketAverageByItem),
                hint = "Receita de ingressos / ingressos aprovados",
                icon = BiIcon.Package,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Pedidos criados",
                value = formatEventBiNumber(totals.ticketCreatedCount.toDouble()),
                hint = "Pedidos de ingresso no filtro",
                icon = BiIcon.ShoppingBag,
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Ingressos aprovados",
                value = formatEventBiNumber(totals.approvedTicketQuantity.toDouble()),
                hint = "Apenas ingressos de evento",
                icon = BiIcon.BarChart3,
                modifier = Modifier.weight(1f),
            )
        }

        EventBiChartPanel(
            title = "Funil completo",
            subtitle = "Card, compra, pedido, RSVP, aprovação, check-in e compra",
            info = "Clique no card soma aberturas vindas da página /eventos.\n" +
                "Clique em comprar soma os botões Comprar/Garantir da página do evento.\n" +
                "RSVP Eu vou e RSVP Talvez vêm dos botões de presença.\n" +
                "Check-in soma entradas por QR e entradas manuais.\n" +
                "Check-in com compra conta usuários que entraram no evento e também tiveram " +
                "compra/ficha aprovada no mesmo filtro.",
        ) {
            EventBiBars(data = totals.ticketFunnelRows)
        }

        EventBiChartPanel(
            title = "Lotes por retorno",
            subtitle = "Quantidade e receita por lote de ingresso",
        ) {
            EventBiBarsDual(data = commercial.lotRows)
        }

        EventBiChartPanel(
            title = "Turmas por venda",
            subtitle = "Pedidos, itens e receita por turma",
        ) {
            EventBiBarsDual(data = commercial.classRows)
        }

        EventBiChartPanel(
            title = "Preço que performa",
            subtitle = "Faixas de valor médio por item",
        ) {
            EventBiBarsDual(data = commercial.priceRows)
        }

        EventBiChartPanel(
            title = "Compras por dia da semana",
            subtitle = "Volume e receita por dia",
        ) {
            EventBiBarsDual(data = commercial.weekdayRows)
        }

        EventBiChartPanel(
            title = "Compras por período",
            subtitle = "Madrugada, manhã, tarde e noite",
        ) {
            EventBiBarsDual(data = commercial.periodRows)
        }

        EventBiChartPanel(
            title = "Aluno, convidado e externo",
            cornerMetric = "Total ${formatEventBiNumber(commercial.audienceTotal)}",
            toolbar = {
                // `AUDIENCE_BASIS_OPTIONS.map(...)` (6815).
                AudienceBasisToolbar(selected = audienceBasis, onSelect = onAudienceBasisChange)
            },
        ) {
            EventBiPieMetric(data = commercial.audienceRows)
        }

        EventBiChartPanel(
            title = "Transferências por origem",
            subtitle = "Separação entre app e operação manual",
        ) {
            EventBiPieMetric(data = sales.transferModeRows)
        }

        EventBiChartPanel(
            title = "Destino da transferência",
            subtitle = "Usuário da faculdade ou cadastro manual/externo",
        ) {
            EventBiPieMetric(data = sales.transferTargetRows)
        }

        EventBiChartPanel(
            title = "Quem mais transferiu",
            subtitle = "Usuários que mais enviaram ingressos ou fichas",
        ) {
            EventBiBars(data = sales.transferActorRows)
        }
    }
}

/** O seletor de base do público que o web renderiza como `toolbar` do painel (6813). */
@Composable
private fun AudienceBasisToolbar(
    selected: EventBiAudienceBasis,
    onSelect: (EventBiAudienceBasis) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = Color.Black,
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Row(modifier = Modifier.padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            EventBiAudienceBasis.entries.forEach { option ->
                val active = option == selected
                Surface(
                    modifier = Modifier.clickable { onSelect(option) },
                    shape = RoundedCornerShape(6.dp),
                    color = if (active) PremiumBrand else Color.Transparent,
                ) {
                    Text(
                        text = option.label.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        color = if (active) Color.Black else PremiumZinc500,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

/** Fundo neutro reaproveitado pelas visões quando o painel não tem gráfico. */
@Composable
internal fun EventBiViewNotice(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(12.dp),
            color = PremiumZinc500,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
