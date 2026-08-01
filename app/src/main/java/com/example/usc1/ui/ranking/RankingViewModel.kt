package com.example.usc1.ui.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.session.UserSession
import com.example.usc1.data.repository.SupabaseRankingRepository
import com.example.usc1.domain.model.RankingCatalog
import com.example.usc1.domain.model.RankingClass
import com.example.usc1.domain.model.RankingUser
import com.example.usc1.domain.repository.RankingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RankingTab(val label: String) {
    Individual("Individual"),
    Turma("Por turma"),
}

data class RankingUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val activeTab: RankingTab = RankingTab.Individual,
    val users: List<RankingUser> = emptyList(),
    val classes: List<RankingClass> = emptyList(),
)

data class RankingClassUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val className: String = "",
    val members: List<RankingUser> = emptyList(),
) {
    val totalPoints: Int get() = members.sumOf { it.xp }
}

/** `/ranking`. */
class RankingViewModel(
    private val repository: RankingRepository = SupabaseRankingRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(RankingUiState())
    val uiState: StateFlow<RankingUiState> = _uiState.asStateFlow()

    private var lastTenantId: String = ""

    fun load(session: UserSession, forceRefresh: Boolean = false) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        if (tenantId.isBlank()) {
            lastTenantId = ""
            _uiState.value = RankingUiState(
                isLoading = false,
                errorMessage = "Escolha uma atlética para ver o ranking.",
            )
            return
        }
        if (!forceRefresh && tenantId == lastTenantId) return
        lastTenantId = tenantId

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                repository.getGlobalRanking(tenantId, RankingCatalog.MaxGlobalResults)
            }.onSuccess { users ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                        users = users,
                        classes = RankingCatalog.aggregateClasses(users),
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        users = emptyList(),
                        classes = emptyList(),
                        errorMessage = error.message.orEmpty().ifBlank {
                            "Não foi possível carregar o ranking agora."
                        },
                    )
                }
            }
        }
    }

    fun selectTab(tab: RankingTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }
}

/** `/ranking/[turmaId]`. */
class RankingClassViewModel(
    private val repository: RankingRepository = SupabaseRankingRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(RankingClassUiState())
    val uiState: StateFlow<RankingClassUiState> = _uiState.asStateFlow()

    private var lastLoadKey: String = ""

    fun load(session: UserSession, className: String, forceRefresh: Boolean = false) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        val cleanClass = className.trim()
        if (tenantId.isBlank() || cleanClass.isBlank()) {
            lastLoadKey = ""
            _uiState.value = RankingClassUiState(
                isLoading = false,
                className = cleanClass,
                errorMessage = "Turma não identificada.",
            )
            return
        }

        val key = "$tenantId::$cleanClass"
        if (!forceRefresh && key == lastLoadKey) return
        lastLoadKey = key

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, className = cleanClass, errorMessage = null) }
            runCatching {
                repository.getClassRanking(tenantId, cleanClass, RankingCatalog.MaxClassResults)
            }.onSuccess { members ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = null, members = members)
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        members = emptyList(),
                        errorMessage = error.message.orEmpty().ifBlank {
                            "Não foi possível carregar a turma agora."
                        },
                    )
                }
            }
        }
    }
}
