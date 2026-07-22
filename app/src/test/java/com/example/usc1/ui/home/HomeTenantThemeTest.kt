package com.example.usc1.ui.home

import androidx.compose.ui.graphics.Color
import com.example.usc1.core.tenant.TenantContext
import com.example.usc1.core.tenant.TenantPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeTenantThemeTest {
    @Test
    fun `cada paleta preserva as cores da referência web`() {
        val expected = mapOf(
            TenantPalette.Green to HomeTenantColors(Color(0xFF10B981), Color(0xFF34D399)),
            TenantPalette.Yellow to HomeTenantColors(Color(0xFFF59E0B), Color(0xFFFBBF24)),
            TenantPalette.Red to HomeTenantColors(Color(0xFFEF4444), Color(0xFFF87171)),
            TenantPalette.Blue to HomeTenantColors(Color(0xFF3B82F6), Color(0xFF60A5FA)),
            TenantPalette.Orange to HomeTenantColors(Color(0xFFF97316), Color(0xFFFB923C)),
            TenantPalette.Purple to HomeTenantColors(Color(0xFF8B5CF6), Color(0xFFA78BFA)),
            TenantPalette.Pink to HomeTenantColors(Color(0xFFEC4899), Color(0xFFF472B6)),
        )

        expected.forEach { (palette, colors) ->
            assertEquals(colors, palette.toHomeTenantColors())
        }
    }

    @Test
    fun `sem tenant usa identidade neutra da USC`() {
        val style = resolveHomeTenantStyle(tenantContext = null)

        assertEquals("USC", style.displayName)
        assertNull(style.logoUrl)
        assertEquals(TenantPalette.Green.toHomeTenantColors(), style.colors)
    }

    @Test
    fun `tenant ativo fornece nome logo e paleta à Home`() {
        val tenant = TenantContext(
            id = "tenant-azul",
            slug = "tubaroes-azuis",
            name = "  Tubarões Azuis  ",
            logoUrl = "  https://cdn.exemplo.com/tubaroes.png  ",
            palette = TenantPalette.Blue,
        )

        val style = resolveHomeTenantStyle(tenant)

        assertEquals(tenant, style.tenantContext)
        assertEquals("Tubarões Azuis", style.displayName)
        assertEquals("https://cdn.exemplo.com/tubaroes.png", style.logoUrl)
        assertEquals(TenantPalette.Blue.toHomeTenantColors(), style.colors)
    }
}
