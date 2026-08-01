package com.example.usc1.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.session.UserSession
import com.example.usc1.data.repository.SupabaseCommunityRepository
import com.example.usc1.domain.repository.CommunityReactionField
import com.example.usc1.domain.repository.CommunityReactionResult
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
                    currentUserId = userId,
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
                        currentUserId = loaded.currentUserId.ifBlank { current.currentUserId },
                        currentUserName = loaded.currentUserName.ifBlank { current.currentUserName },
                        currentUserAvatarUrl = loaded.currentUserAvatarUrl ?: current.currentUserAvatarUrl,
                        isUserBanned = current.isUserBanned,
                        postDraft = current.postDraft,
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

    fun onPostDraftChange(value: String) {
        _uiState.update { it.copy(postDraft = value.take(it.postDraftLimit), postError = null) }
    }

    fun createPost(session: UserSession) {
        val current = _uiState.value
        val text = current.postDraft.trim()
        val user = session.user
        val userId = user?.id.orEmpty().trim()
        val tenantId = session.tenant?.id.orEmpty().trim()
        if (user == null || userId.isBlank() || user.role.remoteValue == "guest") {
            _uiState.update { it.copy(postError = "Entre com sua conta para publicar na comunidade.") }
            return
        }
        if (text.isBlank()) {
            _uiState.update { it.copy(postError = "Escreva uma mensagem para publicar no feed.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingPost = true, postError = null) }
            runCatching {
                repository.createPost(
                    tenantId = tenantId,
                    userId = userId,
                    userName = user.name,
                    userAvatarUrl = user.avatarUrl,
                    category = current.activeTab,
                    text = text,
                )
            }.onSuccess {
                _uiState.update { it.copy(postDraft = "", isSubmittingPost = false, postError = null) }
                load(session = session, forceRefresh = true)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSubmittingPost = false,
                        postError = error.message ?: "Não foi possível publicar no feed.",
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

    fun toggleReaction(session: UserSession, postId: String, field: CommunityReactionField) {
        val user = session.user
        val userId = user?.id.orEmpty().trim()
        if (userId.isBlank() || user?.role?.remoteValue == "guest") {
            _uiState.update { it.copy(postError = "Entre com sua conta para reagir na comunidade.") }
            return
        }
        if (_uiState.value.isUserBanned) {
            _uiState.update { it.copy(postError = "Conta restrita: você não pode reagir por enquanto.") }
            return
        }
        val tenantId = session.tenant?.id.orEmpty().trim()

        // Atualização otimista, como no web (setAllPostsRaw antes da resposta).
        applyReactionLocally(postId, field, null)

        viewModelScope.launch {
            runCatching {
                repository.togglePostReaction(
                    tenantId = tenantId,
                    postId = postId,
                    userId = userId,
                    field = field,
                )
            }.onSuccess { result ->
                applyReactionLocally(postId, field, result)
            }.onFailure { error ->
                applyReactionLocally(postId, field, null)
                _uiState.update {
                    it.copy(postError = error.message ?: "Não foi possível registrar sua reação.")
                }
            }
        }
    }

    private fun applyReactionLocally(
        postId: String,
        field: CommunityReactionField,
        result: CommunityReactionResult?,
    ) {
        _uiState.update { current ->
            val updateAll = current.allPosts.map { post ->
                if (post.id != postId) return@map post
                when (field) {
                    CommunityReactionField.Likes -> {
                        val active = result?.active ?: !post.likedByMe
                        post.copy(
                            likedByMe = active,
                            likes = result?.total ?: (post.likes + if (active) 1 else -1).coerceAtLeast(0),
                        )
                    }
                    CommunityReactionField.Hype -> {
                        val active = result?.active ?: !post.hypedByMe
                        post.copy(
                            hypedByMe = active,
                            hype = result?.total ?: (post.hype + if (active) 1 else -1).coerceAtLeast(0),
                        )
                    }
                }
            }
            current.copy(
                allPosts = updateAll,
                posts = filterPosts(
                    posts = updateAll,
                    tab = current.activeTab,
                    filter = current.activeFilter,
                    maxVisiblePosts = current.maxVisiblePosts,
                ),
            )
        }
    }

    fun openComments(session: UserSession, postId: String) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        _uiState.update {
            it.copy(
                openCommentsPostId = postId,
                comments = emptyList(),
                commentsLoading = true,
                commentDraft = "",
                commentError = null,
            )
        }
        viewModelScope.launch {
            runCatching { repository.getComments(tenantId = tenantId, postId = postId) }
                .onSuccess { list ->
                    _uiState.update { it.copy(commentsLoading = false, comments = list) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            commentsLoading = false,
                            commentError = error.message ?: "Erro ao carregar comentários.",
                        )
                    }
                }
        }
    }

    fun closeComments() {
        _uiState.update {
            it.copy(
                openCommentsPostId = null,
                comments = emptyList(),
                commentsLoading = false,
                commentDraft = "",
                commentError = null,
            )
        }
    }

    fun onCommentDraftChange(value: String) {
        _uiState.update { it.copy(commentDraft = value.take(300), commentError = null) }
    }

    fun submitComment(session: UserSession) {
        val current = _uiState.value
        val postId = current.openCommentsPostId ?: return
        val user = session.user
        val userId = user?.id.orEmpty().trim()
        if (user == null || userId.isBlank() || user.role.remoteValue == "guest") {
            _uiState.update { it.copy(commentError = "Entre com sua conta para comentar.") }
            return
        }
        val text = current.commentDraft.trim()
        if (text.isBlank()) {
            _uiState.update { it.copy(commentError = "Escreva um comentário para publicar.") }
            return
        }
        val tenantId = session.tenant?.id.orEmpty().trim()

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingComment = true, commentError = null) }
            runCatching {
                repository.createComment(
                    tenantId = tenantId,
                    postId = postId,
                    userId = userId,
                    userName = user.name,
                    userAvatarUrl = user.avatarUrl,
                    text = text,
                )
            }.onSuccess {
                _uiState.update { state ->
                    val bumped = state.allPosts.map { post ->
                        if (post.id == postId) post.copy(comments = post.comments + 1) else post
                    }
                    state.copy(
                        isSubmittingComment = false,
                        commentDraft = "",
                        allPosts = bumped,
                        posts = filterPosts(
                            posts = bumped,
                            tab = state.activeTab,
                            filter = state.activeFilter,
                            maxVisiblePosts = state.maxVisiblePosts,
                        ),
                    )
                }
                runCatching { repository.getComments(tenantId = tenantId, postId = postId) }
                    .onSuccess { list -> _uiState.update { it.copy(comments = list) } }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSubmittingComment = false,
                        commentError = error.message ?: "Não foi possível publicar o comentário.",
                    )
                }
            }
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
