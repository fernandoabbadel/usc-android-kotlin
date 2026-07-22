package com.example.usc1.ui.settings

import com.example.usc1.core.roles.UserRole
import com.example.usc1.core.session.AuthStatus
import com.example.usc1.core.session.AuthUser
import com.example.usc1.core.session.UserSession
import com.example.usc1.core.session.UserStatus
import com.example.usc1.core.tenant.TenantContext
import com.example.usc1.core.tenant.TenantMembershipStatus
import com.example.usc1.core.tenant.TenantPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsUiStateTest {

    @Test
    fun `withSession exposes the compact profile identity without tenant-specific mocks`() {
        val session = UserSession(
            user = AuthUser(
                id = "3e4fc3ca-0000-0000-0000-000000000000",
                name = "Fernando Lopes Abbade",
                email = "fernando@example.com",
                avatarUrl = "https://example.com/avatar.png",
                classCode = "T2",
                classPhotoUrl = "https://example.com/t2.png",
                planName = "Plano Atleta",
                planBadge = "Atleta",
                planColorKey = "emerald",
                role = UserRole.MasterTenant,
                status = UserStatus.Ativo,
            ),
            tenant = TenantContext(
                id = "tenant-1",
                slug = "medicina",
                name = "Atlética de Medicina",
                palette = TenantPalette.Purple,
                membershipStatus = TenantMembershipStatus.Approved,
            ),
            status = AuthStatus.Authenticated,
        )

        val result = SettingsUiState().withSession(session)

        assertEquals("Fernando Lopes Abbade", result.userName)
        assertEquals("FL", result.userInitials)
        assertEquals("T2", result.classLabel)
        assertEquals("Atleta", result.planLabel)
        assertEquals("Master", result.roleLabel)
        assertEquals("ativo", result.statusLabel)
        assertEquals("3E4FC3CA", result.userIdLabel)
        assertEquals(TenantPalette.Purple, result.tenantPalette)
        assertTrue(result.invitePanel.isVisible)
    }

    @Test
    fun `default rows only display badges supplied by real screen state`() {
        val items = defaultSettingsSections.flatMap(SettingsSectionUiModel::items)

        assertEquals("Novo", items.single { it.action == SettingsAction.Orders }.badge)
        assertEquals("Bloqueado", items.single { it.action == SettingsAction.Security }.badge)
        assertFalse(items.single { it.action == SettingsAction.Security }.isEnabled)
        assertNull(items.single { it.action == SettingsAction.Mentorship }.badge)
        assertNull(items.single { it.action == SettingsAction.MiniVendor }.badge)
    }

    @Test
    fun `sign out is kept out of support rows because it belongs to the risk zone`() {
        val actions = defaultSettingsSections.flatMap(SettingsSectionUiModel::items).map { it.action }

        assertFalse(SettingsAction.SignOut in actions)
        assertTrue(SettingsAction.Support in actions)
        assertTrue(SettingsAction.TermsPrivacy in actions)
    }

    @Test
    fun `invite creation is disabled locally when quota is exhausted or loading`() {
        assertFalse(SettingsInviteUiModel(isVisible = true, remainingToday = 0).canCreate)
        assertFalse(SettingsInviteUiModel(isVisible = true, remainingToday = 5, isLoading = true).canCreate)
        assertTrue(SettingsInviteUiModel(isVisible = true, remainingToday = 1).canCreate)
    }
}
