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
import com.example.usc1.ui.bi.charts.EventBiLineMetric
import com.example.usc1.ui.bi.charts.EventBiParetoMetric
import com.example.usc1.ui.bi.charts.EventBiPieMetric
import com.example.usc1.ui.bi.charts.EventBiRadarMetric
import com.example.usc1.ui.bi.charts.EventBiScanModeByHourChart
import com.example.usc1.ui.bi.charts.EventBiSemiDonutMetric
import com.example.usc1.ui.bi.charts.EventBiSimplePieMetric

/**
 * Visão de portaria do BI de Eventos (M8.2).
 *
 * Fonte: `web-reference/src/app/admin/bi/_components/AdminEventBiDashboard.tsx`, linhas 6988-7172
 * (`{view === "portaria" ? ... : null}`).
 *
 * `absenceHref` e `manualEntryHref` (6697-6702) levam ao workspace de evento, que é o M10; com
 * `EventBiLinkBuilder.Inert` eles chegam vazios e o card fica sem link, mas com o número certo.
 */
@Composable
fun EventBiGateView(
    analytics: EventBiAnalytics,
    modifier: Modifier = Modifier,
) {
    val gate = analytics.gate
    val totals = analytics.totals

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        EventBiKpiGrid {
            EventBiKpiCard(
                label = "Taxa de presença",
                value = formatEventBiPercent(gate.showRate),
                hint = "${formatEventBiNumber(gate.ticketScanned.toDouble())} entradas de " +
                    "${formatEventBiNumber(totals.approvedTicketQuantity.toDouble())} ingressos",
                icon = BiIcon.QrCode,
                info = "Mostra quantos ingressos aprovados realmente viraram entrada. É a conversão " +
                    "real de aprovado para presente.",
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Taxa de ausência",
                value = formatEventBiPercent(gate.noShowRate),
                hint = "${formatEventBiNumber(gate.noShow.toDouble())} aprovados sem entrada",
                icon = BiIcon.AlertTriangle,
                info = "Mostra o no-show: pessoas com ingresso aprovado que ainda não aparecem como " +
                    "entrada na lista de presença.",
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Taxa manual",
                value = formatEventBiPercent(gate.manualityRate),
                hint = "${formatEventBiNumber(gate.manualScans.toDouble())} manuais de " +
                    "${formatEventBiNumber(gate.ticketScanned.toDouble())} entradas",
                icon = BiIcon.CheckCircle2,
                info = "Entrada manual dividida pelo total de entradas. Taxa alta aumenta risco de " +
                    "erro, duplicidade e perda de rastreabilidade.",
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Leituras inválidas",
                value = formatEventBiNumber((gate.duplicateScans + gate.invalidScans).toDouble()),
                hint = "${formatEventBiNumber(gate.duplicateScans.toDouble())} duplicadas",
                icon = BiIcon.Ticket,
                info = "Soma leituras inválidas e tentativas duplicadas registradas nos ingressos e " +
                    "na auditoria.",
                modifier = Modifier.weight(1f),
            )
        }

        EventBiKpiGrid {
            EventBiKpiCard(
                label = "Entrada por QR",
                value = formatEventBiNumber(gate.appScans.toDouble()),
                hint = "${formatEventBiPercent(gate.qrRate)} das entradas",
                icon = BiIcon.QrCode,
                info = "Entradas validadas por leitura do QR (Quick Response).",
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Entrada manual",
                value = formatEventBiNumber(gate.manualScans.toDouble()),
                hint = "Entradas marcadas pela equipe",
                icon = BiIcon.CheckCircle2,
                info = "Entradas sem leitura direta do QR (Quick Response), normalmente registradas " +
                    "pela equipe da porta.",
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Ocupação",
                value = if (gate.totalCapacity > 0) formatEventBiPercent(gate.occupancyRate) else "Sem capacidade",
                hint = if (gate.totalCapacity > 0) {
                    "${formatEventBiNumber(gate.ticketScanned.toDouble())} de " +
                        "${formatEventBiNumber(gate.totalCapacity.toDouble())} vagas"
                } else {
                    "Cadastre a capacidade na edição do evento"
                },
                icon = BiIcon.Users,
                info = "Usa presentes divididos pela capacidade real do local. É diferente da taxa de " +
                    "presença, que usa ingressos aprovados como base.",
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Capacidade restante",
                value = if (gate.totalCapacity > 0) formatEventBiNumber(gate.capacityRemaining.toDouble()) else "-",
                hint = if (gate.totalCapacity > 0) "Vagas restantes em tempo real" else "Sem limite cadastrado",
                icon = BiIcon.Target,
                info = "Mostra quantas pessoas ainda cabem no evento, considerando as entradas já " +
                    "registradas.",
                modifier = Modifier.weight(1f),
            )
        }

        EventBiKpiGrid {
            EventBiKpiCard(
                label = "Pico em 15 min",
                value = formatEventBiNumber(gate.peakInterval.quantity),
                hint = gate.peakInterval.name,
                icon = BiIcon.TrendingUp,
                info = "Maior volume registrado em uma janela de 15 minutos. Ajuda a enxergar fila " +
                    "que o gráfico por hora pode esconder.",
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Velocidade média",
                value = "${formatEventBiDecimal(gate.averageMinutesBetweenScans)} min",
                hint = "Tempo médio entre leituras",
                icon = BiIcon.Clock3,
                info = "Tempo médio entre uma entrada e outra. Quanto menor, mais rápida foi a operação.",
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Risco de fila",
                value = gate.queueRisk,
                hint = "${formatEventBiPercent(gate.queuePressure)} da capacidade estimada da equipe",
                icon = BiIcon.AlertTriangle,
                info = "Compara o pico de 15 minutos com uma capacidade estimada de 4 entradas por " +
                    "minuto por operador ativo.",
                modifier = Modifier.weight(1f),
            )
            EventBiKpiCard(
                label = "Operadores ativos",
                value = formatEventBiNumber(gate.activeGateOperators.toDouble()),
                hint = "Maior sequência: ${formatEventBiNumber(gate.longestFastSequence.toDouble())} leituras",
                icon = BiIcon.Users,
                info = "Conta operadores que aparecem registrando entradas e mostra a maior sequência " +
                    "rápida de leituras.",
                modifier = Modifier.weight(1f),
            )
        }

        EventBiChartPanel(
            title = "Curva acumulada de entrada",
            subtitle = "Entradas somadas ao longo do evento",
            info = "Mostra a portaria enchendo ao longo do tempo. Se a curva sobe rápido, o público " +
                "entrou cedo; se fica plana, a entrada parou; se sobe tarde, houve pico atrasado.",
        ) {
            EventBiLineMetric(data = gate.entryCumulativeRows)
        }

        EventBiChartPanel(
            title = "QR versus manual por horário",
            subtitle = "Barra empilhada com linha de manualidade",
            info = "Compara, por horário, quantas entradas foram por QR (Quick Response) e quantas " +
                "foram manuais. A linha mostra se a equipe passou a abandonar o scanner em algum pico.",
        ) {
            EventBiScanModeByHourChart(data = gate.scanModeByHourRows)
        }

        EventBiChartPanel(
            title = "Antes, durante e tarde",
            subtitle = "Entrada em relação ao início do evento",
            info = "Separa quem entrou antes do início, na primeira hora, no meio do evento, muito " +
                "tarde e quem não entrou. Ajuda a entender o comportamento real do público.",
        ) {
            EventBiSimplePieMetric(data = gate.entryTimingRows)
        }

        EventBiChartPanel(
            title = "Ocupação do evento",
            subtitle = "Presentes versus capacidade real",
            info = "Mostra a ocupação do local: entradas registradas contra capacidade cadastrada. " +
                "Quando não há capacidade, cadastre esse número na edição do evento.",
        ) {
            EventBiSemiDonutMetric(data = gate.occupancyRows)
        }

        EventBiChartPanel(
            title = "Leituras por horário",
            subtitle = "Pico de entrada",
            info = "Mostra os horários com mais entradas. Use para reforçar equipe, comunicação e " +
                "abertura de fila nos horários críticos.",
        ) {
            EventBiLineMetric(data = gate.scanByHourRows)
        }

        EventBiChartPanel(
            title = "Entrada por intervalo",
            subtitle = "Janelas de 15 minutos",
            info = "Mostra o pico real de fila em intervalos menores. Uma hora pode parecer tranquila, " +
                "mas esconder uma concentração forte em poucos minutos.",
        ) {
            EventBiBarsDual(
                data = gate.intervalRows,
                valueName = "Entradas/min",
                quantityName = "Entradas",
                valueFormat = EventBiValueFormat.Decimal,
            )
        }

        EventBiChartPanel(
            title = "Entrada por modo",
            subtitle = "QR (Quick Response) e manual",
            info = "Mostra a divisão geral das entradas entre leitura de QR (Quick Response) e " +
                "entrada manual.",
        ) {
            EventBiPieMetric(data = gate.entryModeRows)
        }

        EventBiChartPanel(
            title = "Presença por tipo",
            subtitle = "Tipo de ingresso ou categoria operacional",
            info = "Mostra quais tipos de público realmente apareceram. O foco aqui não é venda, e " +
                "sim presença real na portaria.",
        ) {
            EventBiBarsDual(
                data = gate.presenceByTypeRows,
                valueName = "Presença",
                quantityName = "Presentes",
                valueFormat = EventBiValueFormat.Percent,
            )
        }

        EventBiChartPanel(
            title = "Taxa de no-show por lote",
            subtitle = "Ausentes proporcionais por lote",
            info = "Transforma ausência em taxa. Assim, um lote grande não parece pior só porque " +
                "vendeu mais; o gráfico mostra a proporção real de faltas.",
        ) {
            EventBiParetoMetric(data = gate.noShowRateByLotRows)
        }

        EventBiChartPanel(
            title = "Presença por turma",
            subtitle = "Aprovados que entraram",
            info = "Mostra quais turmas compareceram melhor. É diferente da ausência por turma, " +
                "porque destaca quem realmente ocupou o evento.",
        ) {
            EventBiBarsDual(
                data = gate.presentByClassRows,
                valueName = "Presença",
                quantityName = "Presentes",
                valueFormat = EventBiValueFormat.Percent,
            )
        }

        EventBiChartPanel(
            title = "Ausência por turma",
            subtitle = "Aprovados sem entrada",
            info = "Aponta turmas com mais aprovados sem leitura de entrada. Use para conferir lista " +
                "nominal e casos de QR (Quick Response) não lido.",
        ) {
            EventBiBars(data = gate.noShowByClassRows)
        }

        EventBiChartPanel(
            title = "Ausência por lote",
            subtitle = "Comparação entre tipos de ingresso",
            info = "Mostra a quantidade de aprovados sem entrada em cada lote. Use junto da taxa de " +
                "no-show para não confundir volume com comportamento.",
        ) {
            EventBiBars(data = gate.noShowByLotRows)
        }

        EventBiChartPanel(
            title = "Origem do pedido",
            subtitle = "Checkout, manual, admin, cortesia e porta",
            info = "Mostra de onde vieram os ingressos que realmente entraram. Pedidos manuais e " +
                "cortesia podem ter comportamento diferente do checkout público.",
        ) {
            EventBiPieMetric(data = gate.presenceBySourceRows)
        }

        EventBiChartPanel(
            title = "Presença por transferência",
            subtitle = "Impacto operacional das transferências",
            info = "Mostra transferidos que entraram ou ficaram sem uso. Transferências perto da " +
                "entrada podem afetar conferência, QR (Quick Response) e fila.",
        ) {
            EventBiSemiDonutMetric(data = gate.presenceByTransferRows)
        }

        EventBiChartPanel(
            title = "Categoria operacional",
            subtitle = "Aluno, não aluno, membro, diretoria e porta",
            info = "Classifica a ocupação real do evento pela estrutura disponível: aluno, não aluno, " +
                "membro, diretoria, cortesia, convidado e entrada/porta.",
        ) {
            EventBiBarsDual(
                data = gate.operationalCategoryRows,
                valueName = "Presença",
                quantityName = "Presentes",
                valueFormat = EventBiValueFormat.Percent,
            )
        }

        EventBiChartPanel(
            title = "Qualidade da portaria",
            subtitle = "Radar operacional",
            info = "Resume a saúde da operação: uso de QR (Quick Response), baixa manual controlada, " +
                "leituras válidas, velocidade, presença e rastreabilidade por operador.",
        ) {
            EventBiRadarMetric(data = gate.operatorQualityRadarRows)
        }

        EventBiChartPanel(
            title = "Entradas por operador",
            subtitle = "Volume e manualidade por pessoa",
            info = "Mostra quem registrou entradas e qual percentual foi manual. Ajuda a entender quem " +
                "trabalhou na porta e como trabalhou.",
        ) {
            EventBiBarsDual(
                data = gate.portariaOperatorChartRows,
                valueName = "Manualidade",
                quantityName = "Entradas",
                valueFormat = EventBiValueFormat.Percent,
            )
        }

        EventBiChartPanel(
            title = "Motivos inválidos",
            subtitle = "Pareto de problemas na leitura",
            info = "Quebra as leituras inválidas por motivo: QR (Quick Response) usado, cancelado, de " +
                "outro evento, sem aprovação, erro técnico ou código mal formatado.",
        ) {
            EventBiParetoMetric(data = gate.invalidReasonRows)
        }

        EventBiChartPanel(
            title = "Aprovado sem QR lido",
            subtitle = "Separação inteligente da ausência",
            info = "Separa não comparecimento real de problemas operacionais, como entrada manual, QR " +
                "(Quick Response) ausente, tentativa inválida ou dados incompletos.",
        ) {
            EventBiSemiDonutMetric(data = gate.approvedWithoutReadRows)
        }

        EventBiChartPanel(
            title = "Comparativo de portarias",
            subtitle = "Somente eventos do recorte atual",
            info = "Compara presença, pico e manualidade entre eventos da mesma entidade ou do mesmo " +
                "recorte ativo. Não mistura entidades diferentes quando o painel está travado em Liga, " +
                "Comissão ou Diretório.",
        ) {
            EventBiBarsDual(
                data = gate.portariaEventComparisonChartRows,
                valueName = "Manualidade",
                quantityName = "Presentes",
                valueFormat = EventBiValueFormat.Percent,
            )
        }

        EventBiDataTable(
            title = "Qualidade por operador de portaria",
            rows = gate.portariaOperatorRows,
            columns = listOf(
                EventBiColumn("operador", "Operador", hrefKey = "href"),
                EventBiColumn("entradas", "Entradas", EventBiColumnFormat.Number, "href"),
                EventBiColumn("qr", "QR", EventBiColumnFormat.Number, "href"),
                EventBiColumn("manual", "Manual", EventBiColumnFormat.Number, "href"),
                EventBiColumn("invalidas", "Inválidas", EventBiColumnFormat.Number, "href"),
                EventBiColumn("duplicadas", "Duplicadas", EventBiColumnFormat.Number, "href"),
                EventBiColumn("erro", "% erro", EventBiColumnFormat.Percent),
                EventBiColumn("manualidade", "% manual", EventBiColumnFormat.Percent),
            ),
            pageSize = 20,
        )

        EventBiDataTable(
            title = "Status da portaria em tempo real",
            rows = gate.liveStatusRows,
            columns = listOf(
                EventBiColumn("evento", "Evento", hrefKey = "href"),
                EventBiColumn("iniciado", "Iniciado"),
                EventBiColumn("ultimaEntrada", "Última entrada"),
                EventBiColumn("ultimaInvalida", "Última inválida"),
                EventBiColumn("presentes", "Presentes", EventBiColumnFormat.Number, "href"),
                EventBiColumn("ausentes", "Ausentes", EventBiColumnFormat.Number, "href"),
                EventBiColumn("entrada", "% entrada", EventBiColumnFormat.Percent),
                EventBiColumn("pico", "Pico atual"),
            ),
            pageSize = 20,
        )

        EventBiDataTable(
            title = "Lista de ausentes acionável",
            rows = gate.absentRows,
            columns = listOf(
                EventBiColumn("nome", "Nome", hrefKey = "href"),
                EventBiColumn("turma", "Turma"),
                EventBiColumn("lote", "Lote"),
                EventBiColumn("tipo", "Tipo"),
                EventBiColumn("quantidade", "Qtd.", EventBiColumnFormat.Number),
                EventBiColumn("compra", "Compra"),
                EventBiColumn("contato", "Contato"),
                EventBiColumn("qr", "Status do QR"),
                EventBiColumn("transferencia", "Transferência"),
            ),
            pageSize = 20,
        )

        EventBiDataTable(
            title = "Ingressos ativos não utilizados",
            rows = gate.unusedActiveRows,
            columns = listOf(
                EventBiColumn("situacao", "Situação", hrefKey = "href"),
                EventBiColumn("nome", "Nome", hrefKey = "href"),
                EventBiColumn("turma", "Turma"),
                EventBiColumn("lote", "Lote"),
                EventBiColumn("quantidade", "Qtd.", EventBiColumnFormat.Number),
                EventBiColumn("qr", "QR"),
            ),
            pageSize = 20,
        )

        EventBiDataTable(
            title = "Tentativas duplicadas com contexto",
            rows = gate.duplicateContextRows,
            columns = listOf(
                EventBiColumn("evento", "Evento", hrefKey = "href"),
                EventBiColumn("pessoa", "Quem tentou"),
                EventBiColumn("ingresso", "Ingresso"),
                EventBiColumn("primeira", "Primeira entrada"),
                EventBiColumn("segunda", "Segunda tentativa"),
                EventBiColumn("diferenca", "Dif. min", EventBiColumnFormat.Decimal),
                EventBiColumn("operador", "Operador"),
                EventBiColumn("acao", "Ação"),
            ),
            pageSize = 20,
        )

        EventBiDataTable(
            title = "Comparativo entre eventos da mesma entidade",
            rows = gate.portariaEventComparisonRows,
            columns = listOf(
                EventBiColumn("evento", "Evento", hrefKey = "href"),
                EventBiColumn("aprovados", "Aprovados", EventBiColumnFormat.Number),
                EventBiColumn("presentes", "Presentes", EventBiColumnFormat.Number, "href"),
                EventBiColumn("presenca", "Presença", EventBiColumnFormat.Percent),
                EventBiColumn("pico", "Pico"),
                EventBiColumn("manualidade", "Manualidade", EventBiColumnFormat.Percent),
                EventBiColumn("invalidas", "Inválidas", EventBiColumnFormat.Number),
            ),
            pageSize = 20,
        )

        EventBiDataTable(
            title = "Alertas específicos da portaria",
            rows = gate.portariaAlertRows,
            columns = listOf(
                EventBiColumn("alerta", "Alerta", hrefKey = "href"),
                EventBiColumn("descricao", "O que significa", width = 260),
                EventBiColumn("impacto", "Impacto"),
                EventBiColumn("quantidade", "Quantidade", EventBiColumnFormat.Number, "href"),
            ),
            pageSize = 20,
        )
    }
}
