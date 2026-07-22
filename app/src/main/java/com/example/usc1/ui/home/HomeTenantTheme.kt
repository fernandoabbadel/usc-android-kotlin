package com.example.usc1.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.usc1.core.tenant.TenantContext
import com.example.usc1.core.tenant.TenantPalette

data class HomeTenantColors(
    val primary: Color,
    val accent: Color,
)

data class HomeTenantStyle(
    val tenantContext: TenantContext?,
    val displayName: String,
    val logoUrl: String?,
    val colors: HomeTenantColors,
)

internal fun TenantPalette.toHomeTenantColors(): HomeTenantColors = when (this) {
    TenantPalette.Green -> HomeTenantColors(
        primary = Color(0xFF10B981),
        accent = Color(0xFF34D399),
    )
    TenantPalette.Yellow -> HomeTenantColors(
        primary = Color(0xFFF59E0B),
        accent = Color(0xFFFBBF24),
    )
    TenantPalette.Red -> HomeTenantColors(
        primary = Color(0xFFEF4444),
        accent = Color(0xFFF87171),
    )
    TenantPalette.Blue -> HomeTenantColors(
        primary = Color(0xFF3B82F6),
        accent = Color(0xFF60A5FA),
    )
    TenantPalette.Orange -> HomeTenantColors(
        primary = Color(0xFFF97316),
        accent = Color(0xFFFB923C),
    )
    TenantPalette.Purple -> HomeTenantColors(
        primary = Color(0xFF8B5CF6),
        accent = Color(0xFFA78BFA),
    )
    TenantPalette.Pink -> HomeTenantColors(
        primary = Color(0xFFEC4899),
        accent = Color(0xFFF472B6),
    )
}

internal fun resolveHomeTenantStyle(tenantContext: TenantContext?): HomeTenantStyle = HomeTenantStyle(
    tenantContext = tenantContext,
    displayName = tenantContext?.name?.trim()?.takeIf(String::isNotBlank) ?: "USC",
    logoUrl = tenantContext?.logoUrl?.trim()?.takeIf(String::isNotBlank),
    colors = (tenantContext?.palette ?: TenantPalette.Green).toHomeTenantColors(),
)

private val LocalHomeTenantStyle = staticCompositionLocalOf {
    resolveHomeTenantStyle(tenantContext = null)
}

object HomeTenantTheme {
    val current: HomeTenantStyle
        @Composable
        @ReadOnlyComposable
        get() = LocalHomeTenantStyle.current

    val colors: HomeTenantColors
        @Composable
        @ReadOnlyComposable
        get() = current.colors

    @Composable
    operator fun invoke(
        tenantContext: TenantContext?,
        content: @Composable () -> Unit,
    ) {
        val style = remember(tenantContext) { resolveHomeTenantStyle(tenantContext) }
        CompositionLocalProvider(LocalHomeTenantStyle provides style, content = content)
    }
}

val HomeBrand: Color
    @Composable
    @ReadOnlyComposable
    get() = HomeTenantTheme.colors.primary

val HomeBrandAccent: Color
    @Composable
    @ReadOnlyComposable
    get() = HomeTenantTheme.colors.accent
