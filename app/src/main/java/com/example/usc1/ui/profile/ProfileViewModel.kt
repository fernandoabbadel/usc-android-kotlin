package com.example.usc1.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.session.UserSession
import com.example.usc1.data.repository.SupabaseProfileRepository
import com.example.usc1.domain.repository.ProfileRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: ProfileRepository = SupabaseProfileRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var lastLoadKey: String? = null
    private var currentSession: UserSession? = null
    private var currentTargetId: String = ""

    fun load(session: UserSession, targetUserId: String? = null, forceRefresh: Boolean = false) {
        currentSession = session
        val tenantId = session.tenant?.id.orEmpty().trim()
        val viewerId = session.user?.id.orEmpty().trim()
        val targetId = targetUserId?.trim().orEmpty().ifBlank { viewerId }
        currentTargetId = targetId
        val loadKey = "$tenantId::$viewerId::$targetId"

        if (!forceRefresh && lastLoadKey == loadKey && !_uiState.value.isLoading && _uiState.value.detail != null) {
            return
        }
        lastLoadKey = loadKey

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, actionMessage = null) }
            runCatching {
                repository.getProfileBundle(
                    tenantId = tenantId,
                    targetUserId = targetId,
                    viewerUserId = viewerId,
                )
            }.onSuccess { bundle ->
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        errorMessage = null,
                        detail = bundle.profile,
                        profile = bundle.profile.toUiModel(
                            fallbackEmail = if (bundle.isOwnProfile) session.user?.email.orEmpty() else "",
                        ),
                        posts = bundle.posts,
                        eventos = bundle.eventos,
                        treinos = bundle.treinos,
                        ligas = bundle.ligas,
                        followersCount = bundle.followersCount,
                        followingCount = bundle.followingCount,
                        isFollowing = bundle.isFollowing,
                        isOwnProfile = bundle.isOwnProfile,
                        affinitySent = bundle.affinitySent,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erro ao carregar o perfil.",
                    )
                }
            }
        }
    }

    fun refresh() {
        val session = currentSession ?: return
        load(session = session, targetUserId = currentTargetId, forceRefresh = true)
    }

    fun selectTab(tab: ProfileTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun toggleFollow() {
        val session = currentSession ?: return
        val state = _uiState.value
        if (state.isOwnProfile || state.isSubmittingFollow) return
        val tenantId = session.tenant?.id.orEmpty().trim()
        val viewerId = session.user?.id.orEmpty().trim()
        if (viewerId.isBlank()) {
            _uiState.update { it.copy(actionMessage = "Entre com sua conta para seguir este perfil.") }
            return
        }
        val shouldFollow = !state.isFollowing

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingFollow = true, actionMessage = null) }
            runCatching {
                repository.toggleFollow(
                    tenantId = tenantId,
                    targetUserId = currentTargetId,
                    viewerUserId = viewerId,
                    follow = shouldFollow,
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isSubmittingFollow = false,
                        isFollowing = shouldFollow,
                        followersCount = (it.followersCount + if (shouldFollow) 1 else -1).coerceAtLeast(0),
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSubmittingFollow = false,
                        actionMessage = error.message ?: "Não foi possível atualizar o seguidor.",
                    )
                }
            }
        }
    }

    fun toggleAffinity() {
        val session = currentSession ?: return
        val state = _uiState.value
        if (state.isOwnProfile || state.isSubmittingAffinity) return
        val tenantId = session.tenant?.id.orEmpty().trim()
        val viewerId = session.user?.id.orEmpty().trim()
        if (viewerId.isBlank()) {
            _uiState.update { it.copy(actionMessage = "Entre com sua conta para enviar um crush.") }
            return
        }
        val shouldSend = !state.affinitySent

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingAffinity = true, actionMessage = null) }
            runCatching {
                repository.toggleAffinity(
                    tenantId = tenantId,
                    targetUserId = currentTargetId,
                    viewerUserId = viewerId,
                    send = shouldSend,
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isSubmittingAffinity = false,
                        affinitySent = shouldSend,
                        actionMessage = if (shouldSend) "Crush enviado!" else "Crush removido.",
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSubmittingAffinity = false,
                        actionMessage = error.message ?: "Não foi possível enviar o crush.",
                    )
                }
            }
        }
    }

    fun openFollowList(mode: ProfileFollowListMode) {
        val session = currentSession ?: return
        val tenantId = session.tenant?.id.orEmpty().trim()
        _uiState.update { it.copy(followListMode = mode, followListLoading = true, followList = emptyList()) }
        viewModelScope.launch {
            runCatching {
                repository.getFollowList(
                    tenantId = tenantId,
                    targetUserId = currentTargetId,
                    followers = mode == ProfileFollowListMode.Followers,
                )
            }.onSuccess { list ->
                _uiState.update { it.copy(followListLoading = false, followList = list) }
            }.onFailure {
                _uiState.update { it.copy(followListLoading = false, followList = emptyList()) }
            }
        }
    }

    fun closeFollowList() {
        _uiState.update { it.copy(followListMode = null, followList = emptyList(), followListLoading = false) }
    }

    fun consumeActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }
}
