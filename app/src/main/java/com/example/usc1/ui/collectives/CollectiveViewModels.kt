package com.example.usc1.ui.collectives

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.roles.UserRole
import com.example.usc1.core.session.UserSession
import com.example.usc1.data.repository.SupabaseCollectivesRepository
import com.example.usc1.domain.repository.CollectivesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Catálogo público de coletivos.
 *
 * Fonte web: `app/ligas_usc/page.tsx` (ligas, com Oráculo) e
 * `components/collectives/CollectiveCatalogPage.tsx` (comissões e diretório).
 */
data class CollectiveCatalogUiState(
    val kind: CollectiveKind = CollectiveKind.League,
    val uiConfig: CollectiveAreaUiConfig = CollectiveAreaUiConfig.default(CollectiveKind.League),
    val groups: List<CollectiveGroup> = emptyList(),
    val turmaMemberCounts: Map<String, Int> = emptyMap(),
    val sellerStats: Map<String, CollectiveSellerStats> = emptyMap(),
    val likedIds: List<String> = emptyList(),
    val followedIds: List<String> = emptyList(),
    val togglingIds: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val canManageCatalog: Boolean = false,
    val quiz: LeagueQuizUiState = LeagueQuizUiState(),
) {
    /**
     * `orderedRecords` do web: comissões ordenam por vendas/exposição/likes/nome; as demais
     * áreas mantêm a ordem retornada pela consulta.
     */
    val orderedGroups: List<CollectiveGroup>
        get() = if (kind != CollectiveKind.Commission) {
            groups
        } else {
            groups.sortedWith(
                compareByDescending<CollectiveGroup> { sellerStats[it.id]?.soldCount ?: 0 }
                    .thenByDescending { sellerStats[it.id]?.exposedCount ?: 0 }
                    .thenByDescending { it.likesCount }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name },
            )
        }

    /** `publishedCount` do web. */
    val publishedCount: Int get() = groups.count { it.visible }

    fun memberCountFor(group: CollectiveGroup): Int =
        if (kind == CollectiveKind.Commission && group.turmaId.isNotBlank()) {
            turmaMemberCounts[group.turmaId] ?: group.membersCount
        } else {
            group.membersCount
        }
}

/** Estado do Oráculo (`QUESTIONS`/`calculateMatches` de `app/ligas_usc/page.tsx`). */
data class LeagueQuizUiState(
    val step: Int = 0,
    val selectedOptions: List<String> = emptyList(),
    val answers: Map<LeagueQuizQuestionKey, List<String>> = emptyMap(),
    val keywords: List<String> = emptyList(),
    val matches: List<LeagueQuizMatch> = emptyList(),
    val showResult: Boolean = false,
) {
    val question: LeagueQuizQuestion get() = LeagueQuizCatalog.questions[step.coerceIn(0, LeagueQuizCatalog.questions.lastIndex)]
    val canAdvance: Boolean get() = selectedOptions.isNotEmpty()
    val allZero: Boolean get() = matches.isNotEmpty() && matches.all { it.matchPercent == 0 }
}

class CollectiveCatalogViewModel(
    private val repository: CollectivesRepository = SupabaseCollectivesRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(CollectiveCatalogUiState())
    val uiState: StateFlow<CollectiveCatalogUiState> = _uiState.asStateFlow()

    private var lastLoadKey = ""

    fun load(session: UserSession, kind: CollectiveKind, forceRefresh: Boolean = false) {
        val tenantId = session.tenant?.id.orEmpty()
        val key = "$kind:$tenantId:${session.user?.id.orEmpty()}"
        if (!forceRefresh && key == lastLoadKey) return
        lastLoadKey = key

        _uiState.update {
            it.copy(
                kind = kind,
                uiConfig = CollectiveAreaUiConfig.default(kind),
                isLoading = true,
                errorMessage = null,
                quiz = LeagueQuizUiState(),
            )
        }

        viewModelScope.launch {
            runCatching {
                val uiConfig = repository.getAreaUiConfig(tenantId, kind)
                val all = repository.getCollectives(tenantId, kind)
                // `visibleRecords` do web: comissões e diretório escondem `visivel === false`.
                val groups = if (kind == CollectiveKind.League) all else all.filter { it.visible }

                val turmaCounts = if (kind == CollectiveKind.Commission) {
                    repository.getTurmaMemberCounts(tenantId, groups.map { it.turmaId })
                } else {
                    emptyMap()
                }
                val sellerStats = if (kind == CollectiveKind.Commission) {
                    repository.getSellerStats(tenantId, groups.map { it.id })
                } else {
                    emptyMap()
                }
                val interaction = session.user?.id
                    ?.takeIf { it.isNotBlank() }
                    ?.let { repository.getInteractionState(tenantId, it) }
                    ?: CollectiveInteractionState()

                CollectiveCatalogUiState(
                    kind = kind,
                    uiConfig = uiConfig,
                    groups = groups,
                    turmaMemberCounts = turmaCounts,
                    sellerStats = sellerStats,
                    likedIds = interaction.likedIds,
                    followedIds = interaction.followedIds,
                    isLoading = false,
                    canManageCatalog = resolveCanManageCatalog(session, kind, groups, uiConfig),
                )
            }.onSuccess { state ->
                _uiState.value = state
            }.onFailure { error ->
                _uiState.value = CollectiveCatalogUiState(
                    kind = kind,
                    uiConfig = CollectiveAreaUiConfig.default(kind),
                    isLoading = false,
                    errorMessage = error.message.orEmpty().ifBlank { defaultLoadError(kind) },
                )
            }
        }
    }

    fun toggleLike(session: UserSession, group: CollectiveGroup) {
        val userId = session.user?.id.orEmpty()
        if (userId.isBlank()) return
        val current = _uiState.value
        if (current.togglingIds.contains(group.id)) return

        val wasLiked = current.likedIds.contains(group.id)
        val optimisticDelta = if (wasLiked) -1 else 1

        _uiState.update { state ->
            state.copy(
                togglingIds = state.togglingIds + group.id,
                likedIds = if (wasLiked) state.likedIds - group.id else (state.likedIds + group.id).distinct(),
                groups = state.groups.applyLikeDelta(group.id, optimisticDelta),
            )
        }

        viewModelScope.launch {
            runCatching {
                repository.toggleLike(session.tenant?.id.orEmpty(), userId, group.id)
            }.onSuccess { result ->
                _uiState.update { state ->
                    val correction = if (result.isLiked == !wasLiked) 0 else (if (result.isLiked) 1 else -1) - optimisticDelta
                    state.copy(
                        likedIds = result.likedIds,
                        togglingIds = state.togglingIds - group.id,
                        groups = state.groups.applyLikeDelta(group.id, correction),
                    )
                }
            }.onFailure {
                _uiState.update { state ->
                    state.copy(
                        likedIds = if (wasLiked) (state.likedIds + group.id).distinct() else state.likedIds - group.id,
                        togglingIds = state.togglingIds - group.id,
                        groups = state.groups.applyLikeDelta(group.id, if (wasLiked) 1 else -1),
                    )
                }
            }
        }
    }

    fun toggleFollow(session: UserSession, group: CollectiveGroup) {
        val userId = session.user?.id.orEmpty()
        if (userId.isBlank()) return
        val previousIds = _uiState.value.followedIds
        val isFollowing = previousIds.contains(group.id)

        _uiState.update { state ->
            state.copy(
                followedIds = if (isFollowing) previousIds - group.id else (previousIds + group.id).distinct(),
            )
        }

        viewModelScope.launch {
            runCatching {
                repository.toggleFollow(session.tenant?.id.orEmpty(), userId, group.id)
            }.onSuccess { nextIds ->
                _uiState.update { it.copy(followedIds = nextIds) }
            }.onFailure {
                _uiState.update { it.copy(followedIds = previousIds) }
            }
        }
    }

    // -------- Oráculo --------

    /** `toggleOption` do web: no máximo 3 opções por pergunta. */
    fun toggleQuizOption(label: String) {
        _uiState.update { state ->
            val current = state.quiz.selectedOptions
            val next = when {
                current.contains(label) -> current - label
                current.size < LeagueQuizCatalog.MaxSelectedOptions -> current + label
                else -> current
            }
            state.copy(quiz = state.quiz.copy(selectedOptions = next))
        }
    }

    /** `handleNextStep` do web. */
    fun advanceQuiz(session: UserSession) {
        val state = _uiState.value
        val quiz = state.quiz
        val question = quiz.question
        val selectedQuestionOptions = question.options.filter { quiz.selectedOptions.contains(it.label) }
        val nextAnswers = quiz.answers + (question.key to selectedQuestionOptions.map { it.label })
        val nextKeywords = quiz.keywords + selectedQuestionOptions.flatMap { it.keywords }

        if (quiz.step < LeagueQuizCatalog.questions.lastIndex) {
            _uiState.update {
                it.copy(
                    quiz = it.quiz.copy(
                        step = quiz.step + 1,
                        selectedOptions = emptyList(),
                        answers = nextAnswers,
                        keywords = nextKeywords,
                    ),
                )
            }
            return
        }

        val scored = LeagueQuizEngine.calculateMatches(state.groups, nextAnswers, nextKeywords)
        _uiState.update {
            it.copy(
                quiz = it.quiz.copy(
                    selectedOptions = emptyList(),
                    answers = nextAnswers,
                    keywords = nextKeywords,
                    matches = scored.take(LeagueQuizCatalog.TopMatches),
                    showResult = true,
                ),
            )
        }

        val userId = session.user?.id.orEmpty()
        if (userId.isBlank()) return
        val topMatch = scored.firstOrNull { it.matchScore > 0 }?.collective?.name ?: "Nenhum"
        viewModelScope.launch {
            runCatching { repository.addQuizHistory(userId, topMatch, nextKeywords) }
        }
    }

    /** Botão "Refazer" do web. */
    fun resetQuiz() {
        _uiState.update { it.copy(quiz = LeagueQuizUiState()) }
    }

    private fun List<CollectiveGroup>.applyLikeDelta(id: String, delta: Int): List<CollectiveGroup> {
        if (delta == 0) return this
        return map { group ->
            if (group.id == id) group.copy(likesCount = maxOf(0, group.likesCount + delta)) else group
        }
    }

    /** `canManageCatalog` do web. */
    private fun resolveCanManageCatalog(
        session: UserSession,
        kind: CollectiveKind,
        groups: List<CollectiveGroup>,
        uiConfig: CollectiveAreaUiConfig,
    ): Boolean {
        val userId = session.user?.id.orEmpty()
        if (userId.isBlank()) return false
        if (kind == CollectiveKind.Directory) return true
        if (kind != CollectiveKind.Commission) return false
        if (session.user?.role == UserRole.Master) return true
        if (uiConfig.managerUserIds.contains(userId)) return true
        return groups.any { group ->
            group.managerUserIds.contains(userId) ||
                group.members.any { it.id.trim() == userId && LeagueRoleCatalog.canManageRole(it.role) }
        }
    }

    private fun defaultLoadError(kind: CollectiveKind): String = when (kind) {
        CollectiveKind.League -> "Não foi possível carregar as ligas agora."
        CollectiveKind.Commission -> "Não foi possível carregar as comissões agora."
        CollectiveKind.Directory -> "Não foi possível carregar o diretório agora."
    }
}

/**
 * Página pública do coletivo com as abas do web
 * (`CollectivePublicDetailClient` e `LeaguePublicDetailClient`).
 */
data class CollectiveDetailUiState(
    val kind: CollectiveKind = CollectiveKind.League,
    val tab: CollectiveTab = CollectiveTab.Overview,
    val group: CollectiveGroup? = null,
    val uiConfig: CollectiveAreaUiConfig = CollectiveAreaUiConfig.default(CollectiveKind.League),
    val store: CollectiveStoreState = CollectiveStoreState(),
    val likedIds: List<String> = emptyList(),
    val followedIds: List<String> = emptyList(),
    val turmaMemberCount: Int? = null,
    val turmaMemberIds: List<String> = emptyList(),
    val requestRole: String = LeagueRoleCatalog.DefaultRole,
    val isLoading: Boolean = true,
    val isTogglingLike: Boolean = false,
    val errorMessage: String? = null,
    val userId: String = "",
    val userTurma: String = "",
    val isPlatformMaster: Boolean = false,
) {
    val isLiked: Boolean get() = group != null && likedIds.contains(group.id)
    val isFollowing: Boolean get() = group != null && followedIds.contains(group.id)

    /** `currentMemberRequest` do web. */
    val currentMemberRequest: CollectiveMemberRequest?
        get() = group?.memberRequests?.firstOrNull { it.userId.trim() == userId.trim() && userId.isNotBlank() }

    private val isListedMember: Boolean
        get() = userId.isNotBlank() && group?.sortedMembers.orEmpty().any { it.id.trim() == userId.trim() }

    private val isManagementMember: Boolean
        get() = userId.isNotBlank() && group?.sortedMembers.orEmpty().any {
            it.id.trim() == userId.trim() && LeagueRoleCatalog.canManageRole(it.role)
        }

    /** `isCommissionTurmaMember` do web. */
    private val isCommissionTurmaMember: Boolean
        get() {
            if (kind != CollectiveKind.Commission || userId.isBlank()) return false
            val turma = CollectiveTextUtils.normalizeTurmaCode(group?.turmaId)
            if (turma.isBlank()) return false
            return CollectiveTextUtils.normalizeTurmaCode(userTurma) == turma ||
                turmaMemberIds.any { it.trim() == userId.trim() }
        }

    val isOfficialMember: Boolean
        get() = if (kind == CollectiveKind.Commission) isListedMember || isCommissionTurmaMember else isListedMember

    /** `canManagePage` do web. */
    val canManagePage: Boolean
        get() = userId.isNotBlank() && (
            isPlatformMaster ||
                group?.managerUserIds.orEmpty().contains(userId) ||
                uiConfig.managerUserIds.contains(userId) ||
                isManagementMember
            )

    /** `requestBlockedByMembership` do web. */
    val requestBlockedByMembership: Boolean
        get() = if (kind == CollectiveKind.Commission) isManagementMember else isOfficialMember

    /** `requestRoleOptions` do web: comissão só aceita cargo de gestão. */
    val requestRoleOptions: List<String>
        get() = if (kind == CollectiveKind.Commission) {
            LeagueRoleCatalog.managementRoleOptions
        } else {
            LeagueRoleCatalog.roleOptions
        }

    val publicAgendaEvents: List<CollectiveEvent> get() = group?.publicAgendaEvents.orEmpty()

    /** Eventos internos só aparecem para membro oficial, igual ao web. */
    val internalAgendaEvents: List<CollectiveEvent>
        get() = if (isOfficialMember) group?.internalAgendaEvents.orEmpty() else emptyList()

    val visibleAgendaCount: Int get() = publicAgendaEvents.size + internalAgendaEvents.size

    /** `displayMembersCount` do web. */
    val displayMembersCount: Int
        get() = when {
            kind != CollectiveKind.Commission -> group?.membersCount ?: 0
            tab == CollectiveTab.Members -> group?.publicMembers.orEmpty().size
            else -> turmaMemberCount ?: group?.membersCount ?: 0
        }

    /** `heroImageSrc` do web: aba loja usa a capa da categoria quando existir. */
    val heroImageUrl: String?
        get() = if (tab == CollectiveTab.Store && store.enabled) {
            store.coverImageUrl ?: group?.imageUrl
        } else {
            group?.imageUrl
        }

    /** `headerLabel` do web. */
    val headerLabel: String
        get() = when (kind) {
            CollectiveKind.League -> "Ecossistema acadêmico"
            CollectiveKind.Commission -> "Representação oficial"
            CollectiveKind.Directory -> "Estrutura institucional"
        }

    val emptyTitle: String
        get() = when (kind) {
            CollectiveKind.League -> "Liga não encontrada"
            CollectiveKind.Commission -> "Comissão não encontrada"
            CollectiveKind.Directory -> "Página de diretório não encontrada"
        }

    val emptyDescription: String
        get() = when (kind) {
            CollectiveKind.League -> "A liga pode ter sido removida ou ainda não estar visível neste tenant."
            CollectiveKind.Commission -> "A comissão pode ter sido removida ou ainda não estar publicada nesta tenant."
            CollectiveKind.Directory -> "O diretório pode ter sido removido ou ainda não estar publicado nesta tenant."
        }
}

class CollectiveDetailViewModel(
    private val repository: CollectivesRepository = SupabaseCollectivesRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(CollectiveDetailUiState())
    val uiState: StateFlow<CollectiveDetailUiState> = _uiState.asStateFlow()

    private var lastLoadKey = ""

    fun load(
        session: UserSession,
        kind: CollectiveKind,
        tab: CollectiveTab,
        collectiveId: String,
        usePrimaryRecord: Boolean = false,
        forceRefresh: Boolean = false,
    ) {
        val tenantId = session.tenant?.id.orEmpty()
        val key = "$kind:$tab:$collectiveId:$usePrimaryRecord:$tenantId:${session.user?.id.orEmpty()}"
        if (!forceRefresh && key == lastLoadKey) return
        lastLoadKey = key

        _uiState.update {
            it.copy(
                kind = kind,
                tab = tab,
                uiConfig = CollectiveAreaUiConfig.default(kind),
                isLoading = true,
                errorMessage = null,
                userId = session.user?.id.orEmpty(),
                userTurma = session.user?.classCode.orEmpty(),
                isPlatformMaster = session.user?.role == UserRole.Master,
            )
        }

        viewModelScope.launch {
            runCatching {
                val uiConfig = repository.getAreaUiConfig(tenantId, kind)
                val group = if (usePrimaryRecord) {
                    repository.getPrimaryCollective(tenantId, kind)
                } else {
                    repository.getCollective(tenantId, kind, collectiveId)
                }

                if (group == null) {
                    return@runCatching _uiState.value.copy(
                        kind = kind,
                        tab = tab,
                        uiConfig = uiConfig,
                        group = null,
                        isLoading = false,
                    )
                }

                val store = repository.getStore(
                    tenantId = tenantId,
                    collectiveId = group.id,
                    loadProducts = tab == CollectiveTab.Store,
                )
                val interaction = session.user?.id
                    ?.takeIf { it.isNotBlank() }
                    ?.let { repository.getInteractionState(tenantId, it) }
                    ?: CollectiveInteractionState()

                val turmaCount = if (kind == CollectiveKind.Commission && group.turmaId.isNotBlank()) {
                    repository.getTurmaMemberCounts(tenantId, listOf(group.turmaId))[group.turmaId] ?: 0
                } else {
                    null
                }
                val turmaMembers = if (kind == CollectiveKind.Commission && group.turmaId.isNotBlank()) {
                    repository.getTurmaMemberIds(tenantId, group.turmaId)
                } else {
                    emptyList()
                }

                val pendingRequest = group.memberRequests
                    .firstOrNull { it.userId.trim() == session.user?.id?.trim() }
                val defaultRole = resolveInitialRequestRole(kind, pendingRequest?.requestedRole)

                _uiState.value.copy(
                    kind = kind,
                    tab = tab,
                    group = group,
                    uiConfig = uiConfig,
                    store = store,
                    likedIds = interaction.likedIds,
                    followedIds = interaction.followedIds,
                    turmaMemberCount = turmaCount,
                    turmaMemberIds = turmaMembers,
                    requestRole = defaultRole,
                    isLoading = false,
                    errorMessage = null,
                )
            }.onSuccess { state ->
                _uiState.value = state
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        group = null,
                        errorMessage = error.message.orEmpty().ifBlank { it.emptyDescription },
                    )
                }
            }
        }
    }

    fun selectRequestRole(role: String) {
        _uiState.update { it.copy(requestRole = LeagueRoleCatalog.resolveRoleLabel(role)) }
    }

    fun toggleLike(session: UserSession) {
        val state = _uiState.value
        val group = state.group ?: return
        val userId = session.user?.id.orEmpty()
        if (userId.isBlank() || state.isTogglingLike) return

        val wasLiked = state.likedIds.contains(group.id)
        val optimisticDelta = if (wasLiked) -1 else 1

        _uiState.update {
            it.copy(
                isTogglingLike = true,
                likedIds = if (wasLiked) it.likedIds - group.id else (it.likedIds + group.id).distinct(),
                group = group.copy(likesCount = maxOf(0, group.likesCount + optimisticDelta)),
            )
        }

        viewModelScope.launch {
            runCatching {
                repository.toggleLike(session.tenant?.id.orEmpty(), userId, group.id)
            }.onSuccess { result ->
                _uiState.update { it.copy(likedIds = result.likedIds, isTogglingLike = false) }
            }.onFailure {
                _uiState.update { current ->
                    current.copy(
                        isTogglingLike = false,
                        likedIds = if (wasLiked) (current.likedIds + group.id).distinct() else current.likedIds - group.id,
                        group = current.group?.copy(
                            likesCount = maxOf(0, (current.group.likesCount) + (if (wasLiked) 1 else -1)),
                        ),
                    )
                }
            }
        }
    }

    fun toggleFollow(session: UserSession) {
        val group = _uiState.value.group ?: return
        val userId = session.user?.id.orEmpty()
        if (userId.isBlank()) return

        val previousIds = _uiState.value.followedIds
        val isFollowing = previousIds.contains(group.id)
        _uiState.update {
            it.copy(followedIds = if (isFollowing) previousIds - group.id else (previousIds + group.id).distinct())
        }

        viewModelScope.launch {
            runCatching {
                repository.toggleFollow(session.tenant?.id.orEmpty(), userId, group.id)
            }.onSuccess { nextIds ->
                _uiState.update { it.copy(followedIds = nextIds) }
            }.onFailure {
                _uiState.update { it.copy(followedIds = previousIds) }
            }
        }
    }

    /**
     * Reproduz os dois efeitos do web: sem solicitação o cargo cai no primeiro da lista
     * válida da área; com solicitação pendente o cargo pedido é mantido (comissão troca
     * um cargo não gerencial por "Diretoria").
     */
    private fun resolveInitialRequestRole(kind: CollectiveKind, requestedRole: String?): String {
        val options = if (kind == CollectiveKind.Commission) {
            LeagueRoleCatalog.managementRoleOptions
        } else {
            LeagueRoleCatalog.roleOptions
        }

        if (requestedRole.isNullOrBlank()) {
            return if (options.contains(LeagueRoleCatalog.DefaultRole)) {
                LeagueRoleCatalog.DefaultRole
            } else {
                options.firstOrNull() ?: LeagueRoleCatalog.DefaultRole
            }
        }

        val resolved = LeagueRoleCatalog.resolveRoleLabel(requestedRole)
        return if (kind == CollectiveKind.Commission && !LeagueRoleCatalog.canManageRole(resolved)) {
            "Diretoria"
        } else {
            resolved
        }
    }
}
