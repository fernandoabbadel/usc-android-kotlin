package com.example.usc1.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regras copiadas do web em `inviteQuota.ts` e `mentorshipService.ts`. */
class SettingsM4RulesTest {

    @Test
    fun `quota pendente bloqueia novo pedido e mantem o limite base`() {
        val quota = SettingsInviteQuota(
            unlockAtMillis = Long.MAX_VALUE,
            status = SettingsInviteQuotaStatus.Pending,
            bonusLimit = 0,
        )

        assertFalse(quota.canRequestMore)
        assertEquals(SettingsInviteDashboard.DefaultDailyLimit, quota.totalLimit)
    }

    @Test
    fun `quota liberada soma o bonus ao limite do dia`() {
        val quota = SettingsInviteQuota(
            status = SettingsInviteQuotaStatus.Granted,
            bonusLimit = SettingsInviteDashboard.BonusLimit,
        )

        assertFalse(quota.canRequestMore)
        assertEquals(
            SettingsInviteDashboard.DefaultDailyLimit + SettingsInviteDashboard.BonusLimit,
            quota.totalLimit,
        )
    }

    @Test
    fun `convite aprovado sai dos ativos e entra no historico`() {
        val approved = inviteEntry(
            id = "a",
            activity = SettingsInviteActivity.Active,
            approval = SettingsInviteApprovalStatus.Approved,
        )
        val active = inviteEntry(
            id = "b",
            activity = SettingsInviteActivity.Active,
            approval = SettingsInviteApprovalStatus.Pending,
        )
        val expired = inviteEntry(
            id = "c",
            activity = SettingsInviteActivity.Expired,
            approval = SettingsInviteApprovalStatus.Unused,
        )
        val dashboard = SettingsInviteDashboard(entries = listOf(approved, active, expired))

        assertEquals(listOf("b"), dashboard.activeEntries.map(SettingsInviteEntry::id))
        assertEquals(listOf("a"), dashboard.approvedEntries.map(SettingsInviteEntry::id))
        assertEquals(listOf("c"), dashboard.closedEntries.map(SettingsInviteEntry::id))
        assertEquals(2, dashboard.historyCount)
    }

    @Test
    fun `contagem regressiva do convite usa dias e horas como no web`() {
        val now = 1_000_000L
        val entry = inviteEntry(
            id = "x",
            activity = SettingsInviteActivity.Active,
            approval = SettingsInviteApprovalStatus.Unused,
        )

        assertEquals("Sem validade", entry.countdownLabel(now))
        assertEquals(
            "Expirado",
            entry.copy(expiresAtMillis = now - 1).countdownLabel(now),
        )
        assertEquals(
            "3h",
            entry.copy(expiresAtMillis = now + 3 * 60 * 60 * 1000L).countdownLabel(now),
        )
        assertEquals(
            "2d 1h",
            entry.copy(expiresAtMillis = now + 49 * 60 * 60 * 1000L).countdownLabel(now),
        )
    }

    @Test
    fun `rotulos de apadrinhamento sao divididos pelos separadores do web`() {
        val labels = AdminMentorshipLabelsConfig(
            mentorLabel = "Padrinho/Madrinha",
            menteeLabel = "Afilhado|Afilhada",
        )

        assertEquals(
            listOf("Padrinho", "Madrinha"),
            SettingsMentorshipRoleOptions.resolve(labels, SettingsMentorshipRoleSide.Mentor),
        )
        assertEquals(
            listOf("Afilhado", "Afilhada"),
            SettingsMentorshipRoleOptions.resolve(labels, SettingsMentorshipRoleSide.Mentee),
        )
    }

    @Test
    fun `rotulo sem separador vira uma unica opcao`() {
        val labels = AdminMentorshipLabelsConfig(mentorLabel = "Veterano")

        assertEquals(
            listOf("Veterano"),
            SettingsMentorshipRoleOptions.resolve(labels, SettingsMentorshipRoleSide.Mentor),
        )
    }

    @Test
    fun `status remoto do pedido segue o mapa do web`() {
        assertEquals(UserOrderStatus.Aprovado, UserOrderStatus.fromRemote("approved"))
        assertEquals(UserOrderStatus.Aprovado, UserOrderStatus.fromRemote("aprovado"))
        assertEquals(UserOrderStatus.Rejeitado, UserOrderStatus.fromRemote("rejected"))
        assertEquals(UserOrderStatus.Rejeitado, UserOrderStatus.fromRemote("rejeitado"))
        assertEquals(UserOrderStatus.Pendente, UserOrderStatus.fromRemote("qualquer_outro"))
    }

    @Test
    fun `slug de status do pedido casa com as rotas do web`() {
        assertEquals(UserOrderStatus.Aprovado, UserOrderStatus.fromSlug("aprovados"))
        assertEquals(UserOrderStatus.Rejeitado, UserOrderStatus.fromSlug("negados"))
        assertEquals(UserOrderStatus.Rejeitado, UserOrderStatus.fromSlug("rejeitados"))
        assertEquals(UserOrderStatus.Pendente, UserOrderStatus.fromSlug("pendentes"))
        assertEquals(UserOrderStatus.Pendente, UserOrderStatus.fromSlug(null))
    }

    @Test
    fun `ingresso lido ou transferido bloqueia o QR`() {
        assertTrue(ticket("lido").isBlocked)
        assertTrue(ticket("transferido").isBlocked)
        assertFalse(ticket("ativo").isBlocked)
        assertEquals("Lido", ticket("lido").statusLabel)
        assertEquals("Transferido", ticket("transferido").statusLabel)
        assertEquals("Ativo", ticket("").statusLabel)
    }

    private fun inviteEntry(
        id: String,
        activity: SettingsInviteActivity,
        approval: SettingsInviteApprovalStatus,
    ) = SettingsInviteEntry(
        id = id,
        token = "token-$id",
        createdAt = "",
        expiresAt = "",
        usesCount = 0,
        maxUses = 1,
        approvalStatus = approval,
        activity = activity,
    )

    private fun ticket(status: String) = UserOrderTicketEntry(
        id = "t",
        token = "tok",
        label = "Ingresso",
        status = status,
    )
}
