package com.example.usc1.domain.model

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.addJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regras do motor de métricas do BI de Eventos (M8.1b), portadas de
 * `web-reference/src/app/admin/bi/_components/AdminEventBiDashboard.tsx`:
 * formatadores e estatística (266-470), acessores (595-2137), o `analytics` (3843-6619) e o
 * `eventOwnerRedirectHref` (6622-6634).
 */
class EventBiM8bRulesTest {

    // Instante fixo para o `Date.now()` do web: 2026-08-01T12:00:00Z.
    private val now = 1_785_931_200_000L

    // ------------------------------------------------------------------
    // Formatadores e estatística (266-470)
    // ------------------------------------------------------------------

    @Test
    fun `formatDecimal sempre mostra uma casa e formatPercent adiciona o sinal`() {
        // `decimalFormatter` (271): `minimumFractionDigits: 1`.
        assertEquals("12,0", formatEventBiDecimal(12.0))
        assertEquals("12,5", formatEventBiDecimal(12.45))
        // `formatPercent` (288) e `formatHours` (292).
        assertEquals("40,0%", formatEventBiPercent(40.0))
        assertEquals("1,5h", formatEventBiHours(1.5))
    }

    @Test
    fun `parseNumber derruba o ponto de milhar e troca a virgula decimal`() {
        // `parseNumber` (326): "R$ 1.234,56" precisa virar 1234.56.
        assertEquals(1234.56, parseEventBiNumber("R$ 1.234,56"), 0.001)
        assertEquals(50.0, parseEventBiNumber("50"), 0.001)
        // Valor não numérico cai no fallback.
        assertEquals(7.0, parseEventBiNumber("abc", 7.0), 0.001)
    }

    @Test
    fun `median usa a media dos dois centrais em lista par e percentile interpola`() {
        // `median` (380).
        assertEquals(3.0, eventBiMedian(listOf(1.0, 5.0, 2.0, 4.0)), 0.001)
        assertEquals(2.0, eventBiMedian(listOf(1.0, 2.0, 3.0)), 0.001)
        assertEquals(0.0, eventBiMedian(emptyList()), 0.001)
        // `percentile` (387): índice 2.7 entre 3 e 4 devolve 3.7.
        assertEquals(3.7, eventBiPercentile(listOf(1.0, 2.0, 3.0, 4.0), 0.9), 0.001)
    }

    @Test
    fun `safeDivide devolve zero em vez de infinito e clamp prende entre zero e cem`() {
        // `safeDivide` (376): o denominador zero nunca vira `Infinity`.
        assertEquals(0.0, safeDivide(10.0, 0.0), 0.001)
        // `clamp` (402): valor não finito devolve o mínimo.
        assertEquals(0.0, eventBiClamp(Double.NaN), 0.001)
        assertEquals(100.0, eventBiClamp(180.0), 0.001)
        // `scoreFromInverseRate` (411): quanto menor a taxa, maior a nota.
        assertEquals(70.0, scoreFromInverseRate(30.0), 0.001)
    }

    @Test
    fun `maxValue nunca devolve negativo porque o reduce do web parte de zero`() {
        // `maxValue` (398): `reduce((current, value) => Math.max(current, value), 0)`.
        assertEquals(0.0, eventBiMaxValue(listOf(-5.0, -2.0)), 0.001)
        assertEquals(9.0, eventBiMaxValue(listOf(3.0, 9.0, 1.0)), 0.001)
    }

    @Test
    fun `scoreBandLabel devolve as quatro faixas de decisao`() {
        // `scoreBandLabel` (422).
        assertEquals("85-100 repetir e escalar", scoreBandLabel(90.0))
        assertEquals("70-84 repetir", scoreBandLabel(70.0))
        assertEquals("40-69 ajustar", scoreBandLabel(55.0))
        assertEquals("0-39 repensar", scoreBandLabel(10.0))
    }

    @Test
    fun `leadBucketLabel e ticketBucket seguem os cortes do web`() {
        // `leadBucketLabel` (446): 10 dias de antecedência caem em "7 a 14 dias".
        val eventStart = now + (10 * 86_400_000L)
        assertEquals("7 a 14 dias", eventBiLeadBucketLabel(now, eventStart))
        assertEquals("Menos de 24h", eventBiLeadBucketLabel(now, now + 3_600_000L))
        assertEquals("Sem data", eventBiLeadBucketLabel(0L, eventStart))
        // `ticketBucket` (457).
        assertEquals("R$ 0-25", eventBiTicketBucket(24.99))
        assertEquals("R$ 25-50", eventBiTicketBucket(25.0))
        assertEquals("Mais de R$ 150", eventBiTicketBucket(151.0))
        // `ticketBucketSortValue` (467): nome fora da lista vai para o fim.
        assertEquals(5.0, eventBiTicketBucketSortValue("Outro"), 0.001)
    }

    // ------------------------------------------------------------------
    // `addMetric` / `metricRows` (2068-2090)
    // ------------------------------------------------------------------

    @Test
    fun `addMetric acumula recalculando a media e nome vazio vira Sem dado`() {
        // `addMetric` (2068).
        val bucket = EventBiMetricBucket()
        bucket.add("Lote 1", 2.0, 100.0)
        bucket.add("Lote 1", 2.0, 300.0)
        bucket.add("  ", 1.0, 50.0)

        val lote = bucket.valueOf("Lote 1")!!
        assertEquals(4.0, lote.quantity, 0.001)
        assertEquals(400.0, lote.value, 0.001)
        assertEquals(100.0, lote.average, 0.001)
        assertNotNull(bucket.valueOf("Sem dado"))
    }

    @Test
    fun `metricRows ordena por valor depois quantidade e corta no limite`() {
        // `metricRows` (2086).
        val bucket = EventBiMetricBucket()
        bucket.add("A", 1.0, 10.0)
        bucket.add("B", 5.0, 90.0)
        bucket.add("C", 3.0, 50.0)

        assertEquals(listOf("B", "C", "A"), bucket.rows(12).map { it.name })
        assertEquals(listOf("B", "C"), bucket.rows(2).map { it.name })
    }

    @Test
    fun `countRateRows calcula presenca e deixa o ausente no secondary`() {
        // `countRateRows` (4762).
        val bucket = EventBiCountRateBucket()
        bucket.add("Aluno", approved = 10.0, present = 6.0, value = 600.0, href = "")

        val row = bucket.rows().single()
        assertEquals(6.0, row.quantity, 0.001)
        assertEquals(60.0, row.value, 0.001)
        assertEquals(4.0, row.secondary, 0.001)
    }

    // ------------------------------------------------------------------
    // Acessores (595-2137)
    // ------------------------------------------------------------------

    @Test
    fun `ticketQuantity usa a quantidade explicita e cai para o numero de entradas`() {
        // `ticketQuantity` (705).
        assertEquals(3, ticketRow(quantity = 3).ticketQuantity())
        val withEntries = buildJsonObject {
            putJsonObject("payment_config") {
                putJsonArray("ticketEntries") {
                    addJsonObject { put("token", "a") }
                    addJsonObject { put("token", "b") }
                }
            }
        }
        assertEquals(2, withEntries.ticketQuantity())
        // Sem quantidade e sem entradas, o mínimo é 1.
        assertEquals(1, buildJsonObject { }.ticketQuantity())
    }

    @Test
    fun `orderTotal usa o preco cheio quando o pedido e de evento`() {
        // `orderTotal` (1121): pedido de evento não multiplica pela quantidade.
        val eventOrder = buildJsonObject {
            put("eventId", "ev-1")
            put("price", 40.0)
            put("quantidade", 3)
        }
        assertEquals(40.0, eventOrder.orderTotal(), 0.001)

        val shopOrder = buildJsonObject {
            put("price", 40.0)
            put("quantidade", 3)
        }
        assertEquals(120.0, shopOrder.orderTotal(), 0.001)
    }

    @Test
    fun `orderRedeemedQuantity nunca passa da quantidade do pedido`() {
        // `orderRedeemedQuantity` (1404): `Math.min(orderQuantity(row), redeemed)`.
        val order = buildJsonObject {
            put("quantidade", 2)
            putJsonObject("data") {
                putJsonObject("eventParty") {
                    putJsonArray("voucherEntries") {
                        addJsonObject { put("usedAt", "2026-07-01T10:00:00Z") }
                        addJsonObject { put("usedAt", "2026-07-01T10:05:00Z") }
                        addJsonObject { put("status", "utilizado") }
                    }
                }
            }
        }
        assertEquals(2, order.orderRedeemedQuantity())
    }

    @Test
    fun `classifyTicketAudience marca nao aluno na entrada manual mesmo com turma`() {
        // `classifyTicketAudience` (899): `manualGateEntry` vence a turma.
        val manual = buildJsonObject {
            put("userTurma", "ENG 2026")
            putJsonObject("data") { put("manualGateEntry", true) }
        }
        assertEquals("Não aluno", manual.classifyTicketAudience(null, null))

        val student = buildJsonObject { put("userTurma", "ENG 2026") }
        assertEquals("Aluno", student.classifyTicketAudience(null, null))

        // `hasStudentClass` (876): "Sem turma" não identifica aluno.
        val unknown = buildJsonObject { put("userTurma", "Sem turma") }
        assertEquals("Não classificado", unknown.classifyTicketAudience(null, null))
    }

    @Test
    fun `classifyTicketOperationalCategory usa o cargo de diretoria antes da turma`() {
        // `classifyTicketOperationalCategory` (1719): `member.management` vence `hasStudentClass`.
        val row = buildJsonObject {
            put("userId", "u-1")
            put("userTurma", "ENG 2026")
        }
        val management = EventBiMemberMeta(roles = listOf("Presidente"), management = true)
        assertEquals("Diretoria", row.classifyTicketOperationalCategory(null, null, management))

        val member = EventBiMemberMeta(roles = listOf("Membro"))
        assertEquals("Membro", row.classifyTicketOperationalCategory(null, null, member))
        assertEquals("Aluno", row.classifyTicketOperationalCategory(null, null, null))
    }

    @Test
    fun `eventCapacity soma os lotes quando nao ha capacidade explicita`() {
        // `eventCapacity` (924).
        val withLots = buildJsonObject {
            putJsonArray("lotes") {
                addJsonObject { put("quantidade", 100) }
                addJsonObject { put("capacidade", 50) }
            }
        }
        assertEquals(150, withLots.eventCapacity())

        val explicit = buildJsonObject {
            put("capacidade", 500)
            putJsonArray("lotes") { addJsonObject { put("quantidade", 100) } }
        }
        assertEquals(500, explicit.eventCapacity())
    }

    @Test
    fun `eventCost soma numero lista e objeto e hasEventCostField enxerga o campo zerado`() {
        // `eventCost` (1523) e `hasEventCostField` (1566).
        val row = buildJsonObject {
            put("custo", 1000.0)
            putJsonArray("custos") {
                addJsonObject { put("valor", 200.0) }
                addJsonObject { put("total", 300.0) }
            }
        }
        assertEquals(1500.0, row.eventCost(), 0.001)
        assertTrue(row.hasEventCostField())

        val zeroed = buildJsonObject { put("custo", 0.0) }
        assertEquals(0.0, zeroed.eventCost(), 0.001)
        // O campo existe, mesmo zerado — é o que o web usa para decidir se mostra o indicador.
        assertTrue(zeroed.hasEventCostField())
        assertTrue(!buildJsonObject { }.hasEventCostField())
    }

    @Test
    fun `expectedTicketTotal casa o lote por nome e multiplica pela quantidade`() {
        // `eventLotUnitPrice` (1618) + `expectedTicketTotal` (1629).
        val event = buildJsonObject {
            putJsonArray("lotes") {
                addJsonObject {
                    put("nome", "Lote 1")
                    put("preco", 50.0)
                }
            }
        }
        val ticket = buildJsonObject {
            put("loteNome", "Lote 1")
            put("quantidade", 2)
        }
        assertEquals(100.0, ticket.expectedTicketTotal(event), 0.001)

        // Sem lote correspondente e sem valor unitário, o esperado é NaN.
        val orphan = buildJsonObject { put("loteNome", "Lote 9") }
        assertTrue(orphan.expectedTicketTotal(event).isNaN())
    }

    @Test
    fun `invalidReasonLabel normaliza os motivos de leitura invalida`() {
        // `invalidReasonLabel` (1026).
        assertEquals("QR (Quick Response) já utilizado", invalidReasonLabel("QR já utilizado"))
        assertEquals("QR (Quick Response) de outro evento", invalidReasonLabel("de outro evento"))
        assertEquals("Código expirado", invalidReasonLabel("token expirado"))
        assertEquals("", invalidReasonLabel(""))
        // Motivo desconhecido é preservado como veio.
        assertEquals("motivo estranho", invalidReasonLabel("motivo estranho"))
    }

    @Test
    fun `ticketQrStatus distingue usado parcial e tentativa invalida`() {
        // `ticketQrStatus` (1060).
        fun withEntries(vararg statuses: String) = buildJsonObject {
            putJsonObject("payment_config") {
                putJsonArray("ticketEntries") {
                    statuses.forEach { status ->
                        addJsonObject {
                            put("token", "t-$status")
                            put("status", status)
                        }
                    }
                }
            }
        }
        assertEquals("Usado", withEntries("lido", "lido").ticketQrStatus())
        assertEquals("Parcialmente usado", withEntries("lido", "ativo").ticketQrStatus())
        assertEquals("Com tentativa inválida", withEntries("invalid").ticketQrStatus())
        assertEquals("Ativo sem uso", withEntries("ativo").ticketQrStatus())
        assertEquals("Sem QR (Quick Response)", buildJsonObject { }.ticketQrStatus())

        // Pegadinha do web: `isTicketEntryCheckedIn` (836) testa `status.includes("lido")`, e
        // "inva-LIDO" contém "lido". Uma entrada em português marcada como "invalido" conta
        // como lida e o status vira "Usado". O port repete o comportamento de propósito.
        assertEquals("Usado", withEntries("invalido").ticketQrStatus())
    }

    @Test
    fun `extractProductTransfers so conta as solicitacoes aceitas`() {
        // `extractProductTransfers` (1365): `["aceito", "accepted"].includes(status)`.
        val order = buildJsonObject {
            put("id", "o-1")
            putJsonObject("data") {
                putJsonObject("eventParty") {
                    putJsonArray("transferRequests") {
                        addJsonObject {
                            put("id", "t-1")
                            put("status", "aceito")
                            put("toUserId", "u-2")
                            put("fromUserName", "Ana")
                        }
                        addJsonObject {
                            put("id", "t-2")
                            put("status", "pendente")
                        }
                    }
                }
            }
        }
        val transfers = order.extractProductTransfers()
        assertEquals(1, transfers.size)
        assertEquals("Ana", transfers.single().actor)
        assertEquals("Usuário da faculdade", transfers.single().target)
    }

    @Test
    fun `transferTargetFromUserId marca cadastro externo quando o id e manual`() {
        // `transferTargetFromUserId` (1285) via `isManualUserId` (894).
        val manual = buildJsonObject {
            put("id", "o-1")
            putJsonObject("data") {
                putJsonObject("eventParty") {
                    putJsonArray("transferRequests") {
                        addJsonObject {
                            put("status", "accepted")
                            put("toUserId", "manual-99")
                            put("fromUserName", "Bia")
                        }
                    }
                }
            }
        }
        assertEquals("Cadastro manual/externo", manual.extractProductTransfers().single().target)
    }

    // ------------------------------------------------------------------
    // Motor `analytics` (3843-6619)
    // ------------------------------------------------------------------

    @Test
    fun `analytics soma receita bruta liquida e quantidade dos aprovados`() {
        val analytics = computeEventBiAnalytics(sampleDataset(), EventBiFilter(), nowMillis = now)

        // `grossRevenue` (3863): 200 de ingresso + 60 de produto.
        assertEquals(260.0, analytics.totals.grossRevenue, 0.001)
        // `netRevenue` (3864): bruto menos os descontos (20 no ingresso).
        assertEquals(240.0, analytics.totals.netRevenue, 0.001)
        assertEquals(2, analytics.totals.approvedTicketQuantity)
        assertEquals(2, analytics.totals.approvedProductQuantity)
        // O ingresso pendente não entra na receita, mas conta como criado.
        assertEquals(3, analytics.totals.ticketCreatedCount)
        assertEquals(1, analytics.totals.ticketApprovedCount)
    }

    @Test
    fun `analytics separa presenca e no-show a partir das entradas lidas`() {
        val analytics = computeEventBiAnalytics(sampleDataset(), EventBiFilter(), nowMillis = now)

        // `ticketScanned` (4703): uma das duas entradas foi lida.
        assertEquals(1, analytics.gate.ticketScanned)
        assertEquals(1, analytics.gate.noShow)
        // `showRate`/`noShowRate` (6461).
        assertEquals(50.0, analytics.gate.showRate, 0.001)
        assertEquals(50.0, analytics.gate.noShowRate, 0.001)
    }

    @Test
    fun `weekdayRows e periodRows trazem todos os rotulos mesmo sem venda`() {
        val analytics = computeEventBiAnalytics(sampleDataset(), EventBiFilter(), nowMillis = now)

        // `WEEKDAYS.map(...)` (6392) e `PERIODS.map(...)` (6393): balde zerado continua na lista.
        assertEquals(7, analytics.commercial.weekdayRows.size)
        assertEquals(EventBiWeekdays, analytics.commercial.weekdayRows.map { it.name })
        assertEquals(4, analytics.commercial.periodRows.size)
        assertEquals(EventBiPeriods, analytics.commercial.periodRows.map { it.name })
    }

    @Test
    fun `leadRows mantem os seis baldes na ordem do web`() {
        val analytics = computeEventBiAnalytics(sampleDataset(), EventBiFilter(), nowMillis = now)

        // `leadBuckets` (3921): ordem fixa, do mais antecipado ao mais tardio.
        assertEquals(
            listOf(
                "30 dias ou mais", "15 a 29 dias", "7 a 14 dias", "3 a 6 dias",
                "24 a 72h", "Menos de 24h",
            ),
            analytics.strategic.leadRows.map { it.name },
        )
    }

    @Test
    fun `funnelRows segue a ordem de clique ate check-in com compra`() {
        val analytics = computeEventBiAnalytics(sampleDataset(), EventBiFilter(), nowMillis = now)

        // `funnelRows` (6378).
        assertEquals(
            listOf(
                "Clique no card", "Clique em comprar", "Pedido criado", "RSVP Eu vou",
                "RSVP Talvez", "Pedido aprovado", "Check-in", "Check-in com compra",
            ),
            analytics.totals.funnelRows.map { it.name },
        )
        // `eventCardClicks` (3868) vem do `stats` do evento.
        assertEquals(120.0, analytics.totals.funnelRows.first().quantity, 0.001)
    }

    @Test
    fun `alerta aprovado sem codigo aparece quando o ingresso nao tem QR`() {
        val analytics = computeEventBiAnalytics(sampleDataset(), EventBiFilter(), nowMillis = now)

        // `addOperationalAlert("aprovado-sem-codigo", ...)` (4486): o pedido aprovado não tem
        // voucher com código, então o alerta precisa existir com a descrição do mapa.
        val alert = analytics.operational.operationalControlAlertRows
            .firstOrNull { it.text("alerta") == "Pedido aprovado sem QR/código" }
        assertNotNull(alert)
        assertEquals(
            EventBiOperationalAlertDescriptions.getValue("aprovado-sem-codigo"),
            alert!!.text("descricao"),
        )
    }

    @Test
    fun `pendingAgingRows classifica o pendente pelas quatro faixas de hora`() {
        val analytics = computeEventBiAnalytics(sampleDataset(), EventBiFilter(), nowMillis = now)

        // `pendingAging` (4686): o ingresso pendente foi criado 30h antes de `now`.
        assertEquals(
            listOf("Menos de 1h", "1 a 6h", "6 a 24h", "Mais de 24h"),
            analytics.operational.pendingAgingRows.map { it.name },
        )
        assertEquals(1.0, analytics.operational.pendingAgingRows.last().quantity, 0.001)
    }

    @Test
    fun `audienceBasis pedidos conta tambem o ingresso pendente`() {
        val dataset = sampleDataset()

        // `audienceBasis === "pedidos"` (4554) varre `selectedData.tickets` inteiro;
        // "aprovados" varre só os aprovados.
        val byOrders = computeEventBiAnalytics(
            dataset, EventBiFilter(audienceBasis = EventBiAudienceBasis.Orders), nowMillis = now,
        )
        val byApproved = computeEventBiAnalytics(
            dataset, EventBiFilter(audienceBasis = EventBiAudienceBasis.Approved), nowMillis = now,
        )
        assertTrue(byOrders.commercial.audienceTotal > byApproved.commercial.audienceTotal)
    }

    @Test
    fun `strategicScore fica nulo sem base e salesHealthScore nulo sem pedido aprovado`() {
        // `hasStrategicScoreBasis` (5642) e `salesHealthScore` (5090).
        val empty = computeEventBiAnalytics(EventBiDataset(), EventBiFilter(), nowMillis = now)
        assertNull(empty.strategic.strategicScore)
        assertEquals("Sem dados suficientes", empty.strategic.strategicDecision)
        assertNull(empty.sales.salesHealthScore)

        val analytics = computeEventBiAnalytics(sampleDataset(), EventBiFilter(), nowMillis = now)
        assertNotNull(analytics.strategic.strategicScore)
        assertNotNull(analytics.sales.salesHealthScore)
    }

    @Test
    fun `withdrawalRate e pendingRedeem saem da diferenca entre vendido e retirado`() {
        val analytics = computeEventBiAnalytics(sampleDataset(), EventBiFilter(), nowMillis = now)

        // O pedido aprovado tem 2 itens e nenhum retirado (`orderRedeemedQuantity` = 0).
        assertEquals(0, analytics.sales.redeemedItems)
        assertEquals(2, analytics.sales.pendingRedeemItems)
        assertEquals(0.0, analytics.sales.withdrawalRate, 0.001)
        assertEquals(1, analytics.sales.pendingRedeemOrders)
    }

    @Test
    fun `links inertes mantem o indicador e deixam o href vazio ate o M10`() {
        val analytics = computeEventBiAnalytics(sampleDataset(), EventBiFilter(), nowMillis = now)

        // `buildStatementHref` (3656) aponta para o workspace de evento, que é o M10.
        assertTrue(analytics.operational.operationalControlAlertRows.isNotEmpty())
        assertTrue(analytics.operational.operationalControlAlertRows.all { it.href.isBlank() })
    }

    // ------------------------------------------------------------------
    // `eventOwnerRedirectHref` (6622-6634)
    // ------------------------------------------------------------------

    @Test
    fun `owner redirect fica nulo no player tenant e sem evento selecionado`() {
        val dataset = EventBiDataset(
            events = listOf(EventBiEvent(id = "ev-1", name = "Festa", ownerScope = EventBiScope.League, ownerId = "liga-9")),
        )

        // `lockedScopeType === "tenant"` (6623).
        assertNull(
            eventBiOwnerRedirect(
                dataset,
                EventBiFilter(eventId = "ev-1"),
                EventBiContext(scope = EventBiScopeRef(EventBiScope.Tenant)),
            ),
        )
        // `eventFilter === "todos"` (6623).
        assertNull(
            eventBiOwnerRedirect(
                dataset,
                EventBiFilter(),
                EventBiContext(scope = EventBiScopeRef(EventBiScope.League, "liga-1")),
            ),
        )
    }

    @Test
    fun `owner redirect aponta para o workspace do dono quando o escopo nao bate`() {
        val dataset = EventBiDataset(
            events = listOf(
                EventBiEvent(
                    id = "ev-1", name = "Festa",
                    ownerScope = EventBiScope.Directory, ownerId = "dir-7",
                ),
            ),
        )
        val redirect = eventBiOwnerRedirect(
            dataset,
            EventBiFilter(eventId = "ev-1"),
            EventBiContext(scope = EventBiScopeRef(EventBiScope.League, "liga-1")),
        )

        assertNotNull(redirect)
        // `canonicalEventWorkspacePath` (1967).
        assertEquals("/diretorio/configurar/dir-7/eventos/ev-1/edicao", redirect!!.webPath)
    }

    @Test
    fun `owner redirect fica nulo quando o dono e o proprio escopo travado`() {
        val dataset = EventBiDataset(
            events = listOf(
                EventBiEvent(
                    id = "ev-1", name = "Festa",
                    ownerScope = EventBiScope.League, ownerId = "liga-1",
                ),
            ),
        )
        assertNull(
            eventBiOwnerRedirect(
                dataset,
                EventBiFilter(eventId = "ev-1"),
                EventBiContext(scope = EventBiScopeRef(EventBiScope.League, "liga-1")),
            ),
        )
    }

    @Test
    fun `canonicalEventWorkspacePath cobre os quatro escopos`() {
        // `canonicalEventWorkspacePath` (1967).
        assertEquals(
            "/ligas/l-1/eventos/e-1/edicao",
            canonicalEventWorkspacePath(EventBiScope.League, "l-1", "e-1"),
        )
        assertEquals(
            "/comissoes/configurar/c-1/eventos/e-1/edicao",
            canonicalEventWorkspacePath(EventBiScope.Commission, "c-1", "e-1"),
        )
        assertEquals(
            "/admin/eventos/e-1/extrato",
            canonicalEventWorkspacePath(EventBiScope.Tenant, "todos", "e-1", "extrato"),
        )
    }

    // ------------------------------------------------------------------
    // Cenário compartilhado
    // ------------------------------------------------------------------

    private fun ticketRow(quantity: Int): JsonObject = buildJsonObject { put("quantidade", quantity) }

    /**
     * Um evento, três ingressos (um aprovado com duas entradas — uma lida —, um pendente e um
     * recusado) e um pedido de produto aprovado sem retirada.
     */
    private fun sampleDataset(): EventBiDataset {
        val eventRaw = buildJsonObject {
            put("id", "ev-1")
            put("titulo", "Festa da Atlética")
            put("data", "2026-08-10")
            put("hora", "22:00")
            put("capacidade", 300)
            putJsonObject("stats") {
                put("cardClicks", 120)
                put("cliquesCompra", 45)
            }
            putJsonArray("lotes") {
                addJsonObject {
                    put("nome", "Lote 1")
                    put("preco", 100.0)
                }
            }
        }
        val event = EventBiEvent(
            id = "ev-1",
            name = "Festa da Atlética",
            startsAtMillis = parseEventBiDate("2026-08-10T22:00"),
            capacity = 300,
            cardClicks = 120,
            buyClicks = 45,
            raw = eventRaw,
        )

        val approvedTicketRaw = buildJsonObject {
            put("id", "t-1")
            put("eventoId", "ev-1")
            put("status", "aprovado")
            put("userId", "u-1")
            put("userName", "Ana")
            put("userTurma", "ENG 2026")
            put("loteNome", "Lote 1")
            put("quantidade", 2)
            put("valorTotal", 200.0)
            put("discountValue", 20.0)
            put("dataSolicitacao", "2026-07-20T10:00:00Z")
            put("dataAprovacao", "2026-07-20T11:00:00Z")
            put("aprovadoPor", "Diretor")
            putJsonObject("payment_config") {
                putJsonArray("ticketEntries") {
                    addJsonObject {
                        put("token", "qr-1")
                        put("scannedAt", "2026-08-10T22:30:00Z")
                        put("scanSource", "qr")
                    }
                    addJsonObject { put("token", "qr-2") }
                }
            }
        }
        val pendingTicketRaw = buildJsonObject {
            put("id", "t-2")
            put("eventoId", "ev-1")
            put("status", "pendente")
            put("userId", "u-2")
            put("quantidade", 1)
            put("valorTotal", 100.0)
            // 30h antes de `now`: cai no balde "Mais de 24h".
            put("dataSolicitacao", "2026-07-31T06:00:00Z")
        }
        val rejectedTicketRaw = buildJsonObject {
            put("id", "t-3")
            put("eventoId", "ev-1")
            put("status", "recusado")
            put("userId", "u-3")
            put("quantidade", 1)
            put("valorTotal", 100.0)
            put("dataSolicitacao", "2026-07-25T10:00:00Z")
        }

        val orderRaw = buildJsonObject {
            put("id", "o-1")
            put("eventId", "ev-1")
            put("productId", "p-1")
            put("productName", "Ficha de bebida")
            put("status", "aprovado")
            put("userId", "u-1")
            put("userName", "Ana")
            put("total", 60.0)
            put("quantidade", 2)
            put("createdAt", "2026-07-21T10:00:00Z")
            put("eventApprovalAt", "2026-07-21T12:00:00Z")
            put("approvedBy", "Diretor")
        }

        val productRaw = buildJsonObject {
            put("id", "p-1")
            put("nome", "Ficha de bebida")
            put("preco", 30.0)
            put("estoque", 100)
            putJsonObject("data") {
                putJsonObject("eventParty") { put("eventId", "ev-1") }
            }
        }

        fun ticketOf(raw: JsonObject) = EventBiTicket(
            id = raw.str("id"),
            eventId = "ev-1",
            status = EventBiStatus.classify(raw.statusValue()),
            quantity = raw.ticketQuantity(),
            value = raw.ticketValue(),
            purchasedAtMillis = raw.ticketPurchaseDate(),
            raw = raw,
        )

        val order = EventBiOrder(
            id = "o-1",
            eventId = "ev-1",
            productId = "p-1",
            status = EventBiStatus.Approved,
            quantity = 2,
            total = 60.0,
            createdAtMillis = orderRaw.orderCreatedAt(),
            raw = orderRaw,
        )
        val product = EventBiProduct(
            id = "p-1", eventId = "ev-1", name = "Ficha de bebida",
            price = 30.0, stock = 100, raw = productRaw,
        )
        val tickets = listOf(approvedTicketRaw, pendingTicketRaw, rejectedTicketRaw).map(::ticketOf)

        return EventBiDataset(
            events = listOf(event),
            tickets = tickets,
            orders = listOf(order),
            products = listOf(product),
            scopeTickets = tickets,
            scopeOrders = listOf(order),
            tenantUserCount = 10,
            hasTransactions = true,
        )
    }
}
