package com.example.usc1.ui.bi

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.session.UserSession
import com.example.usc1.data.repository.SupabaseEventBiRepository
import com.example.usc1.domain.model.EventBiAnalytics
import com.example.usc1.domain.model.EventBiAudienceBasis
import com.example.usc1.domain.model.EventBiContext
import com.example.usc1.domain.model.EventBiDataset
import com.example.usc1.domain.model.EventBiFilter
import com.example.usc1.domain.model.EventBiLinkBuilder
import com.example.usc1.domain.model.EventBiOwnerRedirect
import com.example.usc1.domain.model.EventBiRecordKind
import com.example.usc1.domain.model.EventBiScope
import com.example.usc1.domain.model.EventBiScopeRef
import com.example.usc1.domain.model.EventBiStatementLink
import com.example.usc1.domain.model.EventBiStatementStatus
import com.example.usc1.domain.model.EventBiView
import com.example.usc1.ui.bi.charts.EventBiChipLink
import com.example.usc1.domain.model.computeEventBiAnalytics
import com.example.usc1.domain.model.eventBiOwnerRedirect
import com.example.usc1.domain.model.eventBiSelectedStatementEventId
import com.example.usc1.domain.repository.CollectiveEventBiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado do BI de Eventos, igual para os quatro players (M8.1).
 *
 * Web: os `useState` de `AdminEventBiDashboard` (`scopeType`, `scopeId`, `eventFilter`,
 * `productFilter`, `audienceBasis`, `startDate`, `endDate`) mais as props de contexto
 * (`lockedScopeType`, `lockedScopeId`, `scopeLabel`, `contextTitle`, `contextLogo`,
 * `contextEyebrow`).
 */
data class EventBiUiState(
    val view: EventBiView = EventBiView.Home,
    val context: EventBiContext = EventBiContext(),
    val filter: EventBiFilter = EventBiFilter(),
    val dataset: EventBiDataset = EventBiDataset(),
    /**
     * `analytics` do web (3843-6619). Fica vazio no hub, que carrega só as opções de filtro
     * (`includeTransactions = false`), e completo nas cinco visões do M8.2.
     */
    val analytics: EventBiAnalytics = EventBiAnalytics(),
    /**
     * `eventOwnerRedirectHref` (6622): preenchido quando o evento selecionado pertence a outro
     * portal. O web faz `router.replace`; aqui o M8.2 decide navegar — a tela mostra o banner.
     */
    val ownerRedirect: EventBiOwnerRedirect? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) {
    /**
     * `scopeLocked={Boolean(lockedScopeType)}` do web: como a prop tem default `"tenant"`,
     * o seletor de escopo nunca aparece em nenhum player.
     */
    val scopeLocked: Boolean get() = EventBiScopeRef.SelectorLocked

    /** `showProduct={view === "vendas"}` do `Filters`. */
    val showsProductFilter: Boolean get() = view.showsProductFilter

    val eventOptions get() = dataset.eventOptions

    val productOptions get() = dataset.productOptions

    /**
     * `selectedStatementEventId` (6691): o evento do filtro, ou o único evento do recorte quando
     * o filtro está em "todos".
     */
    val selectedStatementEventId: String
        get() = eventBiSelectedStatementEventId(filter.eventId, dataset.events.map { it.id })

    /**
     * `salesWithdrawalLegendLinks` (6703): os quatro atalhos de extrato do modo vendas.
     *
     * Os href saem de `buildStatementHref`, que leva ao workspace de evento — o M10. Enquanto o
     * `EventBiLinkBuilder` do motor for `Inert`, eles vêm vazios e `FilterLinkChips` não renderiza
     * nada, que é o mesmo comportamento do web quando não há evento selecionado.
     */
    val withdrawalLegendLinks: List<EventBiChipLink>
        get() {
            val eventId = selectedStatementEventId
            if (eventId.isBlank()) return emptyList()
            val builder = EventBiLinkBuilder.Inert
            return listOf(
                EventBiChipLink(
                    label = "Retirado",
                    href = builder.statement(
                        eventId,
                        EventBiStatementLink(
                            type = EventBiRecordKind.Product,
                            status = EventBiStatementStatus.Approved,
                            indicator = "retirado",
                        ),
                    ),
                    color = Color(0xFF22C55E),
                ),
                EventBiChipLink(
                    label = "Pendente",
                    href = builder.statement(
                        eventId,
                        EventBiStatementLink(
                            type = EventBiRecordKind.Product,
                            status = EventBiStatementStatus.Approved,
                            indicator = "pendente-retirada",
                        ),
                    ),
                    color = Color(0xFFFACC15),
                ),
                EventBiChipLink(
                    label = "Parcial",
                    href = builder.statement(
                        eventId,
                        EventBiStatementLink(
                            type = EventBiRecordKind.Product,
                            status = EventBiStatementStatus.Approved,
                            indicator = "retirada-parcial",
                        ),
                    ),
                    color = Color(0xFF38BDF8),
                ),
                EventBiChipLink(
                    label = "Cancelado",
                    href = builder.statement(
                        eventId,
                        EventBiStatementLink(
                            type = EventBiRecordKind.Product,
                            indicator = "cancelado-pos-aprovacao",
                        ),
                    ),
                    color = Color(0xFFFB7185),
                ),
            )
        }
}

class EventBiViewModel(
    private val repository: CollectiveEventBiRepository = SupabaseEventBiRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(EventBiUiState())
    val uiState: StateFlow<EventBiUiState> = _uiState.asStateFlow()

    private var session: UserSession? = null

    /**
     * Abre o dashboard no escopo do player.
     *
     * `useEffect(() => { setScopeType(lockedScopeType); setScopeId(lockedScopeId) })` do web:
     * o escopo travado sempre reescreve o estado do filtro.
     */
    fun load(
        session: UserSession,
        view: EventBiView,
        context: EventBiContext,
        initialEventId: String = EventBiScopeRef.All,
    ) {
        this.session = session
        _uiState.update {
            it.copy(
                view = view,
                context = context,
                filter = it.filter.copy(
                    scope = context.scope,
                    eventId = initialEventId.trim().ifBlank { EventBiScopeRef.All },
                ),
                isLoading = true,
                errorMessage = null,
            )
        }
        refresh()
    }

    fun selectEvent(eventId: String) = updateFilter { it.copy(eventId = eventId) }

    fun selectProduct(productId: String) = updateFilter { it.copy(productId = productId) }

    fun updateStartDate(value: String) = updateFilter { it.copy(startDate = value.trim()) }

    fun updateEndDate(value: String) = updateFilter { it.copy(endDate = value.trim()) }

    fun selectAudienceBasis(basis: EventBiAudienceBasis) {
        // `audienceBasis` só reclassifica o que já está em memória: não refaz a consulta.
        _uiState.update { it.copy(filter = it.filter.copy(audienceBasis = basis)) }
    }

    /**
     * `setScopeType`/`setScopeId` do `Filters`. Só tem efeito com o seletor destravado — hoje
     * nunca, porque o web trava o escopo em todos os players (ver `EventBiScopeRef.SelectorLocked`).
     */
    fun selectScope(scope: EventBiScopeRef) {
        if (_uiState.value.scopeLocked) return
        updateFilter { it.copy(scope = scope, eventId = EventBiScopeRef.All, productId = EventBiScopeRef.All) }
    }

    fun clearPeriod() = updateFilter { it.copy(startDate = "", endDate = "") }

    fun refresh() {
        val current = session ?: return
        val state = _uiState.value

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            runCatching {
                repository.getDataset(
                    tenantId = current.tenant?.id.orEmpty(),
                    filter = state.filter,
                    // O hub só precisa das opções de filtro; as cinco visões (M8.2) pedem o resto.
                    includeTransactions = state.view != EventBiView.Home,
                )
            }.onSuccess { dataset ->
                // O motor roda uma vez por carga e vale para as cinco visões (M8.2).
                val analytics = if (dataset.hasTransactions) {
                    computeEventBiAnalytics(dataset, state.filter)
                } else {
                    EventBiAnalytics()
                }
                _uiState.update {
                    it.copy(
                        dataset = dataset,
                        analytics = analytics,
                        ownerRedirect = eventBiOwnerRedirect(dataset, state.filter, it.context),
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message.orEmpty().ifBlank {
                            "Não foi possível carregar o BI de eventos agora."
                        },
                    )
                }
            }
        }
    }

    private fun updateFilter(transform: (EventBiFilter) -> EventBiFilter) {
        _uiState.update { it.copy(filter = transform(it.filter)) }
        refresh()
    }
}

/** Contexto do player tenant: `app/admin/bi/page.tsx` não passa nenhuma prop de contexto. */
fun tenantEventBiContext(): EventBiContext = EventBiContext(
    scope = EventBiScopeRef(EventBiScope.Tenant, EventBiScopeRef.All),
    scopeLabel = EventBiScope.Tenant.defaultScopeLabel,
    contextEyebrow = EventBiScope.Tenant.defaultEyebrow,
)
