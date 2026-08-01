package com.example.usc1.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.session.UserSession
import com.example.usc1.data.repository.SupabaseTenantHistoryRepository
import com.example.usc1.domain.model.HistoricEvent
import com.example.usc1.domain.model.HistoryPageConfig
import com.example.usc1.domain.model.OrganogramConfig
import com.example.usc1.domain.model.OrganogramMemberStatus
import com.example.usc1.domain.model.OrganogramSection
import com.example.usc1.domain.model.buildOrganogramSections
import com.example.usc1.domain.repository.TenantHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TenantHistoryUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val config: HistoryPageConfig = HistoryPageConfig(),
    val events: List<HistoricEvent> = emptyList(),
)

enum class OrganogramMembershipState {
    None,
    Pending,
    Published,
}

data class OrganogramUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val config: OrganogramConfig = OrganogramConfig(),
    val sections: List<OrganogramSection> = emptyList(),
    val membershipState: OrganogramMembershipState = OrganogramMembershipState.None,
)

/** `/historico`. */
class TenantHistoryViewModel(
    private val repository: TenantHistoryRepository = SupabaseTenantHistoryRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(TenantHistoryUiState())
    val uiState: StateFlow<TenantHistoryUiState> = _uiState.asStateFlow()

    private var lastTenantId: String = ""

    fun load(session: UserSession, forceRefresh: Boolean = false) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        if (tenantId.isBlank()) {
            lastTenantId = ""
            _uiState.value = TenantHistoryUiState(
                isLoading = false,
                errorMessage = "Escolha uma atlética para ver o histórico.",
            )
            return
        }
        if (!forceRefresh && tenantId == lastTenantId) return
        lastTenantId = tenantId

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { repository.getHistory(tenantId) }
                .onSuccess { data ->
                    _uiState.value = TenantHistoryUiState(
                        isLoading = false,
                        config = data.config,
                        events = data.events,
                    )
                }
                .onFailure { error ->
                    _uiState.value = TenantHistoryUiState(
                        isLoading = false,
                        errorMessage = error.message.orEmpty().ifBlank {
                            "Não foi possível carregar o histórico agora."
                        },
                    )
                }
        }
    }
}

/** `/historico/organograma`. */
class OrganogramViewModel(
    private val repository: TenantHistoryRepository = SupabaseTenantHistoryRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(OrganogramUiState())
    val uiState: StateFlow<OrganogramUiState> = _uiState.asStateFlow()

    private var lastLoadKey: String = ""

    fun load(session: UserSession, forceRefresh: Boolean = false) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        val userId = session.user?.id.orEmpty().trim()
        if (tenantId.isBlank()) {
            lastLoadKey = ""
            _uiState.value = OrganogramUiState(
                isLoading = false,
                errorMessage = "Escolha uma atlética para ver o organograma.",
            )
            return
        }
        val key = "$tenantId::$userId"
        if (!forceRefresh && key == lastLoadKey) return
        lastLoadKey = key

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val config = repository.getOrganogramConfig(tenantId)
                val members = repository.resolveOrganogramMembers(
                    tenantId = tenantId,
                    config = config,
                    fallbackPhotoUrl = session.tenant?.logoUrl,
                )
                config to members
            }.onSuccess { (config, members) ->
                val own = config.members.firstOrNull {
                    userId.isNotBlank() && it.userId.trim() == userId
                }
                _uiState.value = OrganogramUiState(
                    isLoading = false,
                    config = config,
                    sections = buildOrganogramSections(config, members),
                    membershipState = when {
                        own == null -> OrganogramMembershipState.None
                        own.status == OrganogramMemberStatus.Pending -> OrganogramMembershipState.Pending
                        own.isPublished -> OrganogramMembershipState.Published
                        else -> OrganogramMembershipState.None
                    },
                )
            }.onFailure { error ->
                _uiState.value = OrganogramUiState(
                    isLoading = false,
                    errorMessage = error.message.orEmpty().ifBlank {
                        "Não foi possível carregar o organograma agora."
                    },
                )
            }
        }
    }
}
