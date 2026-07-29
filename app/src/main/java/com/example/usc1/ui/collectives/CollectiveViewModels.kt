package com.example.usc1.ui.collectives

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.session.UserSession
import com.example.usc1.data.repository.SupabaseCollectivesRepository
import com.example.usc1.domain.repository.CollectivesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LeaguesViewModel(
    private val repository: CollectivesRepository = SupabaseCollectivesRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(LeagueUiState())
    val uiState: StateFlow<LeagueUiState> = _uiState.asStateFlow()

    private var lastTenantId = ""

    fun load(session: UserSession, forceRefresh: Boolean = false) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        if (tenantId.isBlank()) {
            lastTenantId = ""
            _uiState.value = LeagueUiState(errorMessage = "Selecione uma atlética para carregar as ligas.")
            return
        }
        if (!forceRefresh && tenantId == lastTenantId && (_uiState.value.leagues.isNotEmpty() || _uiState.value.errorMessage != null)) {
            return
        }
        lastTenantId = tenantId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                repository.getCollectives(tenantId = tenantId, kind = CollectiveKind.League)
            }.onSuccess { leagues ->
                _uiState.value = LeagueUiState(leagues = leagues, isLoading = false)
            }.onFailure { error ->
                _uiState.value = LeagueUiState(
                    isLoading = false,
                    errorMessage = error.message.orEmpty().ifBlank { "Não foi possível carregar as ligas agora." },
                )
            }
        }
    }

    fun find(id: String): CollectiveGroup? = _uiState.value.leagues.firstOrNull { it.id == id }
}

class DirectoryViewModel(
    private val repository: CollectivesRepository = SupabaseCollectivesRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(DirectoryUiState())
    val uiState: StateFlow<DirectoryUiState> = _uiState.asStateFlow()

    private var lastTenantId = ""

    fun load(session: UserSession, forceRefresh: Boolean = false) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        if (tenantId.isBlank()) {
            lastTenantId = ""
            _uiState.value = DirectoryUiState(errorMessage = "Selecione uma atlética para carregar o diretório.")
            return
        }
        if (!forceRefresh && tenantId == lastTenantId && (_uiState.value.directories.isNotEmpty() || _uiState.value.errorMessage != null)) {
            return
        }
        lastTenantId = tenantId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                repository.getCollectives(tenantId = tenantId, kind = CollectiveKind.Directory)
            }.onSuccess { directories ->
                _uiState.value = DirectoryUiState(directories = directories, isLoading = false)
            }.onFailure { error ->
                _uiState.value = DirectoryUiState(
                    isLoading = false,
                    errorMessage = error.message.orEmpty().ifBlank { "Não foi possível carregar o diretório agora." },
                )
            }
        }
    }

    fun find(id: String): CollectiveGroup? = _uiState.value.directories.firstOrNull { it.id == id }
}

class CommissionsViewModel(
    private val repository: CollectivesRepository = SupabaseCollectivesRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(CommissionUiState())
    val uiState: StateFlow<CommissionUiState> = _uiState.asStateFlow()

    private var lastTenantId = ""

    fun load(session: UserSession, forceRefresh: Boolean = false) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        if (tenantId.isBlank()) {
            lastTenantId = ""
            _uiState.value = CommissionUiState(errorMessage = "Selecione uma atlética para carregar as comissões.")
            return
        }
        if (!forceRefresh && tenantId == lastTenantId && (_uiState.value.commissions.isNotEmpty() || _uiState.value.errorMessage != null)) {
            return
        }
        lastTenantId = tenantId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                repository.getCollectives(tenantId = tenantId, kind = CollectiveKind.Commission)
            }.onSuccess { commissions ->
                _uiState.value = CommissionUiState(commissions = commissions, isLoading = false)
            }.onFailure { error ->
                _uiState.value = CommissionUiState(
                    isLoading = false,
                    errorMessage = error.message.orEmpty().ifBlank { "Não foi possível carregar as comissões agora." },
                )
            }
        }
    }

    fun find(id: String): CollectiveGroup? = _uiState.value.commissions.firstOrNull { it.id == id }
}
