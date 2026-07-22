package com.example.usc1.data.repository

import com.example.usc1.core.tenant.TenantPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PublicTenantPayloadMapperTest {
    @Test
    fun `diretório mantém apenas tenants ativos e visíveis`() {
        val result = PublicTenantPayloadMapper.decodeDirectory(
            """
            [
              {"id":"2","slug":"beta","nome":"Beta","status":"inactive","visibleInDirectory":true},
              {"id":"3","slug":"oculta","nome":"Oculta","status":"active","visibleInDirectory":false},
              {"id":"","slug":"invalida","nome":"Inválida","status":"active","visibleInDirectory":true},
              {"id":"1","slug":"alpha","nome":"Alpha","status":"active","visibleInDirectory":true}
            ]
            """.trimIndent(),
        )

        assertEquals(listOf("alpha"), result.map { tenant -> tenant.slug })
    }

    @Test
    fun `lookup aceita tenant ativo oculto mas exige o mesmo id e slug`() {
        val payload = """
            {
              "id":"tenant-1",
              "slug":"AAAKN",
              "nome":"",
              "sigla":"AAAKN",
              "status":"active",
              "visibleInDirectory":false,
              "paletteKey":"unknown"
            }
        """.trimIndent()

        val resolved = PublicTenantPayloadMapper.decodeResolvedTenant(
            payload = payload,
            expectedTenantId = "tenant-1",
            expectedTenantSlug = "aaakn",
        )

        assertEquals("AAAKN", resolved?.name)
        assertEquals(TenantPalette.Green, resolved?.palette)
        assertNull(
            PublicTenantPayloadMapper.decodeResolvedTenant(
                payload = payload,
                expectedTenantId = "outro-tenant",
                expectedTenantSlug = "aaakn",
            ),
        )
        assertNull(
            PublicTenantPayloadMapper.decodeResolvedTenant(
                payload = payload,
                expectedTenantId = "tenant-1",
                expectedTenantSlug = "outra",
            ),
        )
    }
}
