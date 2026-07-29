package com.example.usc1.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.session.UserSession
import com.example.usc1.data.repository.SupabaseCommunityRepository
import com.example.usc1.domain.repository.CommunityRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CommunityViewModel(
    private val repository: CommunityRepository = SupabaseCommunityRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(CommunityUiState())
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null
    private var lastLoadKey: String? = null

    fun load(session: UserSession, forceRefresh: Boolean = false) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        val user = session.user
        val userId = user?.id.orEmpty().trim()
        val loadKey = "$tenantId::$userId"
        if (tenantId.isBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Selecione uma atlética para carregar a comunidade.",
                    currentUserName = user?.name.orEmpty(),
                    currentUserAvatarUrl = user?.avatarUrl,
                )
            }
            return
        }
        if (!forceRefresh && lastLoadKey == loadKey && !_uiState.value.isLoading && _uiState.value.allPosts.isNotEmpty()) {
            return
        }
        lastLoadKey = loadKey
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    currentUserName = user?.name.orEmpty(),
                    currentUserAvatarUrl = user?.avatarUrl,
                    isUserBanned = user?.status?.isBlocked == true,
                )
            }
            runCatching {
                repository.getCommunityFeed(
                    tenantId = tenantId,
                    userId = userId,
                    userName = user?.name.orEmpty(),
                    userAvatarUrl = user?.avatarUrl,
                    includeBlocked = user?.role?.isAdminLike == true,
                )
            }.onSuccess { loaded ->
                _uiState.update { current ->
                    val merged = loaded.copy(
                        activeTab = current.activeTab.takeIf { it in loaded.tabs } ?: loaded.activeTab,
                        activeFilter = current.activeFilter,
                        currentUserName = loaded.currentUserName.ifBlank { current.currentUserName },
                        currentUserAvatarUrl = loaded.currentUserAvatarUrl ?: current.currentUserAvatarUrl,
                        isUserBanned = current.isUserBanned,
                        isLoading = false,
                        errorMessage = null,
                    )
                    merged.copy(
                        posts = filterPosts(
                            posts = merged.allPosts,
                            tab = merged.activeTab,
                            filter = merged.activeFilter,
                            maxVisiblePosts = merged.maxVisiblePosts,
                        ),
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erro ao carregar feed da comunidade.",
                    )
                }
            }
        }
    }

    fun selectTab(tab: String) {
        _uiState.update { current ->
            current.copy(
                activeTab = tab,
                posts = filterPosts(
                    posts = current.allPosts,
                    tab = tab,
                    filter = current.activeFilter,
                    maxVisiblePosts = current.maxVisiblePosts,
                ),
            )
        }
    }

    fun selectFilter(filter: CommunityFeedFilter) {
        _uiState.update { current ->
            current.copy(
                activeFilter = filter,
                posts = filterPosts(
                    posts = current.allPosts,
                    tab = current.activeTab,
                    filter = filter,
                    maxVisiblePosts = current.maxVisiblePosts,
                ),
            )
        }
    }

    fun findPost(postId: String): CommunityPost? {
        return _uiState.value.allPosts.firstOrNull { it.id == postId }
            ?: _uiState.value.posts.firstOrNull { it.id == postId }
    }

    private fun filterPosts(
        posts: List<CommunityPost>,
        tab: String,
        filter: CommunityFeedFilter,
        maxVisiblePosts: Int,
    ): List<CommunityPost> {
        val scoped = posts.filter { post ->
            tab.equals("Todos", ignoreCase = true) || post.category.equals(tab, ignoreCase = true)
        }
        return when (filter) {
            CommunityFeedFilter.Recent -> scoped
            CommunityFeedFilter.Likes -> scoped.sortedByDescending { it.likes }
            CommunityFeedFilter.Comments -> scoped.sortedByDescending { it.comments }
            CommunityFeedFilter.Hype -> scoped.sortedByDescending { it.hype }
        }.take(maxVisiblePosts.coerceAtLeast(1))
    }
}
