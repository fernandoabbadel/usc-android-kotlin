package com.example.usc1.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.session.UserSession
import com.example.usc1.core.session.UserStatus
import com.example.usc1.data.repository.SupabaseAdminDashboardModulesRepository
import com.example.usc1.data.repository.SupabaseMiniVendorRepository
import com.example.usc1.data.repository.SupabaseSettingsRepository
import com.example.usc1.domain.model.SettingsInviteDashboard
import com.example.usc1.domain.model.SettingsMentorshipAction
import com.example.usc1.domain.model.SettingsMentorshipHub
import com.example.usc1.domain.model.SettingsMentorshipRoleSide
import com.example.usc1.domain.model.SettingsTurmaLeaderPending
import com.example.usc1.domain.model.TenantAppModulesCatalog
import com.example.usc1.domain.repository.AdminDashboardModulesRepository
import com.example.usc1.domain.repository.MiniVendorRepository
import com.example.usc1.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository = SupabaseSettingsRepository(),
    private val miniVendorRepository: MiniVendorRepository = SupabaseMiniVendorRepository(),
    private val modulesRepository: AdminDashboardModulesRepository = SupabaseAdminDashboardModulesRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var lastInviteDashboardKey: String = ""
    private var lastMentorshipKey: String = ""
    private var lastCandidatesKey: String = ""
    private var lastTurmaLeaderKey: String = ""
    private var lastMenuKey: String = ""

    fun setNotificationsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }

    fun consumeStatusMessage() {
        _uiState.update { it.copy(statusMessage = "") }
    }

    /**
     * Espelha os efeitos do `/configuracoes`: o selo do mini vendor vem do status do cadastro
     * e o item só entra no menu quando `mini_vendor` está ligado nos módulos do tenant.
     */
    fun loadMenuContext(session: UserSession, forceRefresh: Boolean = false) {
        val tenant = session.tenant ?: return
        val tenantId = tenant.id.trim()
        val userId = session.user?.id.orEmpty().trim()
        val key = "$tenantId::$userId"
        if (tenantId.isBlank() || userId.isBlank()) return
        if (!forceRefresh && key == lastMenuKey) return
        lastMenuKey = key

        viewModelScope.launch {
            val badge = runCatching {
                miniVendorRepository.getDashboard(tenantId = tenantId, userId = userId)
            }.map { dashboard ->
                if (!dashboard.hasProfile) {
                    "Novo"
                } else {
                    when (dashboard.profileStatus) {
                        "approved" -> "Aprovado"
                        "rejected" -> "Revisar"
                        "disabled" -> "Bloqueado"
                        else -> "Pendente"
                    }
                }
            }.getOrDefault("")

            val showMiniVendor = runCatching {
                modulesRepository.getModulesBundle(
                    tenantName = tenant.name,
                    tenantSlug = tenant.slug,
                    forceRefresh = forceRefresh,
                ).config
            }.map { config ->
                TenantAppModulesCatalog.isEnabled(config, MiniVendorModuleKey)
            }.getOrDefault(true)

            _uiState.update {
                it.copy(miniVendorBadge = badge, showMiniVendorMenu = showMiniVendor)
            }
        }
    }

    fun setInviteState(invite: SettingsInviteUiModel) {
        _uiState.update { it.copy(invitePanel = invite) }
    }

    fun setAccountActionLoading(isLoading: Boolean) {
        _uiState.update { it.copy(isAccountActionLoading = isLoading) }
    }

    fun loadInviteDashboard(session: UserSession, forceRefresh: Boolean = false) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        val userId = session.user?.id.orEmpty().trim()
        val key = "$tenantId::$userId"
        if (tenantId.isBlank() || userId.isBlank()) {
            lastInviteDashboardKey = ""
            _uiState.update {
                it.copy(
                    inviteDashboard = SettingsInviteDashboard(),
                    isInviteDashboardLoading = false,
                    inviteDashboardError = "",
                )
            }
            return
        }
        if (!forceRefresh && key == lastInviteDashboardKey && _uiState.value.inviteDashboard.entries.isNotEmpty()) {
            return
        }
        lastInviteDashboardKey = key
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isInviteDashboardLoading = true,
                    inviteDashboardError = "",
                )
            }
            runCatching {
                repository.getInviteDashboard(tenantId = tenantId, userId = userId)
            }.onSuccess { dashboard ->
                _uiState.update {
                    it.copy(
                        inviteDashboard = dashboard,
                        isInviteDashboardLoading = false,
                        inviteDashboardError = "",
                        invitePanel = it.invitePanel.copy(
                            remainingToday = dashboard.remainingToday,
                        ),
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isInviteDashboardLoading = false,
                        inviteDashboardError = error.message.orEmpty().ifBlank {
                            "Não foi possível carregar seus convites agora."
                        },
                    )
                }
            }
        }
    }

    fun loadMentorshipHub(session: UserSession, forceRefresh: Boolean = false) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        val userId = session.user?.id.orEmpty().trim()
        val key = "$tenantId::$userId"
        if (tenantId.isBlank() || userId.isBlank()) {
            lastMentorshipKey = ""
            _uiState.update {
                it.copy(
                    mentorshipHub = SettingsMentorshipHub(),
                    isMentorshipLoading = false,
                    mentorshipError = "",
                )
            }
            return
        }
        if (!forceRefresh && key == lastMentorshipKey && _uiState.value.mentorshipHub.hasContent) {
            return
        }
        lastMentorshipKey = key
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isMentorshipLoading = true,
                    mentorshipError = "",
                )
            }
            runCatching {
                repository.getMentorshipHub(tenantId = tenantId, userId = userId)
            }.onSuccess { hub ->
                _uiState.update {
                    it.copy(
                        mentorshipHub = hub,
                        isMentorshipLoading = false,
                        mentorshipError = "",
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isMentorshipLoading = false,
                        mentorshipError = error.message.orEmpty().ifBlank {
                            "Não foi possível carregar o apadrinhamento agora."
                        },
                    )
                }
            }
        }
    }

    fun revokeInvite(session: UserSession, inviteId: String) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        val userId = session.user?.id.orEmpty().trim()
        if (tenantId.isBlank() || userId.isBlank() || inviteId.isBlank()) return
        if (_uiState.value.revokingInviteId.isNotBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(revokingInviteId = inviteId, inviteDashboardError = "") }
            runCatching {
                repository.revokeInvite(tenantId = tenantId, userId = userId, inviteId = inviteId)
            }.onSuccess {
                _uiState.update { it.copy(revokingInviteId = "", statusMessage = "Convite encerrado.") }
                loadInviteDashboard(session, forceRefresh = true)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        revokingInviteId = "",
                        inviteDashboardError = error.message.orEmpty()
                            .ifBlank { "Erro ao encerrar convite." },
                    )
                }
            }
        }
    }

    fun requestMoreInvites(session: UserSession) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        val userId = session.user?.id.orEmpty().trim()
        if (tenantId.isBlank() || userId.isBlank()) return
        if (_uiState.value.isRequestingBonusInvites) return

        viewModelScope.launch {
            _uiState.update { it.copy(isRequestingBonusInvites = true, inviteDashboardError = "") }
            runCatching {
                repository.requestMoreInvites(tenantId = tenantId, userId = userId)
            }.onSuccess { quota ->
                _uiState.update { state ->
                    state.copy(
                        isRequestingBonusInvites = false,
                        inviteDashboard = state.inviteDashboard.copy(
                            quota = quota,
                            limitPerDay = quota.totalLimit,
                            remainingToday = (quota.totalLimit - state.inviteDashboard.totalCreatedToday)
                                .coerceAtLeast(0),
                        ),
                        statusMessage = "Pedido feito. Seus ${SettingsInviteDashboard.BonusLimit} novos convites liberam em 1 hora.",
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isRequestingBonusInvites = false,
                        inviteDashboardError = error.message.orEmpty()
                            .ifBlank { "Não consegui pedir mais convites agora." },
                    )
                }
            }
        }
    }

    fun loadMentorshipCandidates(session: UserSession, forceRefresh: Boolean = false) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        val userId = session.user?.id.orEmpty().trim()
        val key = "$tenantId::$userId"
        if (tenantId.isBlank() || userId.isBlank()) return
        if (!forceRefresh && key == lastCandidatesKey) return
        lastCandidatesKey = key

        viewModelScope.launch {
            runCatching {
                repository.getMentorshipCandidates(tenantId = tenantId, userId = userId)
            }.onSuccess { candidates ->
                _uiState.update { it.copy(mentorshipCandidates = candidates) }
            }.onFailure {
                _uiState.update { it.copy(mentorshipCandidates = emptyList()) }
            }
        }
    }

    fun sendMentorshipInvite(
        session: UserSession,
        targetUserId: String,
        targetIsMentor: Boolean,
    ) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        val userId = session.user?.id.orEmpty().trim()
        if (tenantId.isBlank() || userId.isBlank() || targetUserId.isBlank()) return
        if (_uiState.value.isSendingMentorshipInvite) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingMentorshipInvite = true, mentorshipError = "") }
            runCatching {
                repository.sendMentorshipInvite(
                    tenantId = tenantId,
                    currentUserId = userId,
                    targetUserId = targetUserId,
                    targetIsMentor = targetIsMentor,
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isSendingMentorshipInvite = false,
                        statusMessage = "Convite de apadrinhamento enviado.",
                    )
                }
                loadMentorshipHub(session, forceRefresh = true)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSendingMentorshipInvite = false,
                        mentorshipError = error.message.orEmpty()
                            .ifBlank { "Erro ao enviar convite." },
                    )
                }
            }
        }
    }

    fun respondToMentorshipInvite(
        session: UserSession,
        relationshipId: String,
        action: SettingsMentorshipAction,
        selectedRoleLabel: String = "",
    ) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        val userId = session.user?.id.orEmpty().trim()
        if (tenantId.isBlank() || userId.isBlank() || relationshipId.isBlank()) return
        if (_uiState.value.mentorshipActionId.isNotBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(mentorshipActionId = relationshipId, mentorshipError = "") }
            runCatching {
                repository.respondToMentorshipInvite(
                    tenantId = tenantId,
                    currentUserId = userId,
                    relationshipId = relationshipId,
                    action = action,
                    selectedRoleLabel = selectedRoleLabel,
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        mentorshipActionId = "",
                        statusMessage = when (action) {
                            SettingsMentorshipAction.Accept -> "Convite aceito."
                            SettingsMentorshipAction.Reject -> "Convite recusado."
                            SettingsMentorshipAction.Remove -> "Vinculo removido."
                            SettingsMentorshipAction.Cancel -> "Convite cancelado."
                        },
                    )
                }
                loadMentorshipHub(session, forceRefresh = true)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        mentorshipActionId = "",
                        mentorshipError = error.message.orEmpty()
                            .ifBlank { "Erro ao responder convite." },
                    )
                }
            }
        }
    }

    fun updateMentorshipRoleLabel(
        session: UserSession,
        relationshipId: String,
        roleSide: SettingsMentorshipRoleSide,
        roleLabel: String,
    ) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        val userId = session.user?.id.orEmpty().trim()
        if (tenantId.isBlank() || userId.isBlank() || relationshipId.isBlank()) return
        if (_uiState.value.mentorshipActionId.isNotBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(mentorshipActionId = relationshipId, mentorshipError = "") }
            runCatching {
                repository.updateMentorshipRoleLabel(
                    tenantId = tenantId,
                    currentUserId = userId,
                    relationshipId = relationshipId,
                    roleSide = roleSide,
                    roleLabel = roleLabel,
                )
            }.onSuccess {
                _uiState.update { it.copy(mentorshipActionId = "", statusMessage = "Rotulo atualizado.") }
                loadMentorshipHub(session, forceRefresh = true)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        mentorshipActionId = "",
                        mentorshipError = error.message.orEmpty()
                            .ifBlank { "Erro ao editar rotulo." },
                    )
                }
            }
        }
    }

    /** Zona de risco do `/configuracoes`: pausar/reativar a conta. */
    fun toggleAccountStatus(session: UserSession, onDone: () -> Unit = {}) {
        val user = session.user ?: return
        if (_uiState.value.isAccountActionLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isAccountActionLoading = true) }
            runCatching {
                repository.toggleAccountStatus(
                    userId = user.id,
                    isCurrentlyActive = user.status == UserStatus.Ativo,
                    currentRole = user.role.remoteValue,
                    savedRole = "",
                )
            }.onSuccess { paused ->
                _uiState.update {
                    it.copy(
                        isAccountActionLoading = false,
                        statusMessage = if (paused) {
                            "Conta pausada. Acesso restrito."
                        } else {
                            "Conta reativada! Bem-vindo de volta."
                        },
                    )
                }
                onDone()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isAccountActionLoading = false,
                        statusMessage = error.message.orEmpty()
                            .ifBlank { "Erro ao atualizar status da conta." },
                    )
                }
            }
        }
    }

    /** Zona de risco do `/configuracoes`: exclusão definitiva (soft delete). */
    fun deleteAccount(session: UserSession, onDeleted: () -> Unit) {
        val user = session.user ?: return
        if (_uiState.value.isAccountActionLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isAccountActionLoading = true) }
            runCatching {
                repository.softDeleteAccount(
                    userId = user.id,
                    photoUrl = user.avatarUrl.orEmpty(),
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isAccountActionLoading = false,
                        statusMessage = "Sua conta foi excluída. Até logo!",
                    )
                }
                onDeleted()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isAccountActionLoading = false,
                        statusMessage = error.message.orEmpty()
                            .ifBlank { "Erro ao processar exclusão." },
                    )
                }
            }
        }
    }

    fun loadTurmaLeaderPending(session: UserSession, forceRefresh: Boolean = false) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        val user = session.user
        val userId = user?.id.orEmpty().trim()
        val key = "$tenantId::$userId"
        if (tenantId.isBlank() || userId.isBlank()) {
            _uiState.update {
                it.copy(turmaLeader = SettingsTurmaLeaderPending(), isTurmaLeaderLoading = false)
            }
            return
        }
        if (!forceRefresh && key == lastTurmaLeaderKey) return
        lastTurmaLeaderKey = key

        viewModelScope.launch {
            _uiState.update { it.copy(isTurmaLeaderLoading = true, turmaLeaderError = "") }
            runCatching {
                repository.getTurmaLeaderPending(
                    tenantId = tenantId,
                    userId = userId,
                    userClass = user?.classCode.orEmpty(),
                    canManageAll = user?.role?.canManageTenant == true,
                )
            }.onSuccess { pending ->
                _uiState.update {
                    it.copy(
                        turmaLeader = pending,
                        isTurmaLeaderLoading = false,
                        turmaLeaderError = "",
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        turmaLeader = SettingsTurmaLeaderPending(),
                        isTurmaLeaderLoading = false,
                        turmaLeaderError = error.message.orEmpty()
                            .ifBlank { "Erro ao carregar pendencias da turma." },
                    )
                }
            }
        }
    }
}

private const val MiniVendorModuleKey = "mini_vendor"

private val SettingsMentorshipHub.hasContent: Boolean
    get() = mentor != null || mentee != null || incoming.isNotEmpty() || outgoing.isNotEmpty()
