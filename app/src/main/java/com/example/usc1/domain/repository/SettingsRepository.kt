package com.example.usc1.domain.repository

import com.example.usc1.domain.model.SettingsInviteDashboard
import com.example.usc1.domain.model.SettingsInviteQuota
import com.example.usc1.domain.model.SettingsMentorshipAction
import com.example.usc1.domain.model.SettingsMentorshipCandidate
import com.example.usc1.domain.model.SettingsMentorshipHub
import com.example.usc1.domain.model.SettingsMentorshipRoleSide
import com.example.usc1.domain.model.SettingsSupportCategory
import com.example.usc1.domain.model.SettingsSupportTicket
import com.example.usc1.domain.model.SettingsTurmaLeaderPending

interface SettingsRepository {
    suspend fun getInviteDashboard(
        tenantId: String,
        userId: String,
        limit: Int = 50,
    ): SettingsInviteDashboard

    /** `revokeTenantInvite`: encerra um convite criado pelo próprio usuário. */
    suspend fun revokeInvite(
        tenantId: String,
        userId: String,
        inviteId: String,
    )

    /** `requestMoreMemberInvites`: grava o pedido de bônus em `users.extra` do próprio usuário. */
    suspend fun requestMoreInvites(
        tenantId: String,
        userId: String,
    ): SettingsInviteQuota

    suspend fun getMentorshipHub(
        tenantId: String,
        userId: String,
    ): SettingsMentorshipHub

    /** Candidatos do seletor de vínculo: membros do tenant, menos o próprio usuário. */
    suspend fun getMentorshipCandidates(
        tenantId: String,
        userId: String,
        maxResults: Int = 200,
    ): List<SettingsMentorshipCandidate>

    /** `sendMentorshipInvite`: `mentorSide = true` quando o alvo será o padrinho/madrinha. */
    suspend fun sendMentorshipInvite(
        tenantId: String,
        currentUserId: String,
        targetUserId: String,
        targetIsMentor: Boolean,
    )

    /** `respondToMentorshipInvite`: aceitar, recusar, cancelar ou remover vínculo. */
    suspend fun respondToMentorshipInvite(
        tenantId: String,
        currentUserId: String,
        relationshipId: String,
        action: SettingsMentorshipAction,
        selectedRoleLabel: String = "",
    )

    /** `updateMentorshipRoleLabel`: só o próprio lado do vínculo pode ser reetiquetado. */
    suspend fun updateMentorshipRoleLabel(
        tenantId: String,
        currentUserId: String,
        relationshipId: String,
        roleSide: SettingsMentorshipRoleSide,
        roleLabel: String,
    )

    /** Pendências de cadastro visíveis para o líder de turma. */
    suspend fun getTurmaLeaderPending(
        tenantId: String,
        userId: String,
        userClass: String,
        canManageAll: Boolean,
    ): SettingsTurmaLeaderPending

    suspend fun getSupportTickets(
        userId: String,
        maxResults: Int = 20,
    ): List<SettingsSupportTicket>

    /**
     * `toggleAccountStatus`: pausa (status `paused` + role `inactive`, guardando o papel
     * original em `saved_role`) ou reativa restaurando o papel salvo.
     */
    suspend fun toggleAccountStatus(
        userId: String,
        isCurrentlyActive: Boolean,
        currentRole: String,
        savedRole: String,
    ): Boolean

    /** `softDeleteAccount`: anonimiza a própria linha em `users` e marca `status = deleted`. */
    suspend fun softDeleteAccount(
        userId: String,
        photoUrl: String,
    )

    suspend fun submitSupportTicket(
        tenantId: String,
        userId: String,
        userName: String,
        userEmail: String,
        category: SettingsSupportCategory,
        subject: String,
        message: String,
    )
}
