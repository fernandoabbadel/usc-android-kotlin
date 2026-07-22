package com.example.usc1.ui.tenant

import androidx.compose.ui.graphics.Color
import com.example.usc1.core.tenant.TenantPalette
import com.example.usc1.domain.model.PublicTenant

data class TenantIdentity(
    val id: String,
    val name: String,
    val slug: String,
    val subtitle: String,
    val accent: Color,
    val logoUrl: String? = null,
)

data class TenantUiState(
    val isLoading: Boolean = true,
    val tenants: List<TenantIdentity> = emptyList(),
    val selectingTenantId: String? = null,
    val errorMessage: String? = null,
) {
    val isSelecting: Boolean
        get() = selectingTenantId != null
}

internal fun PublicTenant.toTenantIdentity(): TenantIdentity = TenantIdentity(
    id = id,
    name = name,
    slug = slug,
    subtitle = subtitle,
    accent = palette.toAccentColor(),
    logoUrl = logoUrl,
)

private fun TenantPalette.toAccentColor(): Color = when (this) {
    TenantPalette.Green -> Color(0xFF10B981)
    TenantPalette.Yellow -> Color(0xFFF59E0B)
    TenantPalette.Red -> Color(0xFFEF4444)
    TenantPalette.Blue -> Color(0xFF3B82F6)
    TenantPalette.Orange -> Color(0xFFF97316)
    TenantPalette.Purple -> Color(0xFF8B5CF6)
    TenantPalette.Pink -> Color(0xFFEC4899)
}
