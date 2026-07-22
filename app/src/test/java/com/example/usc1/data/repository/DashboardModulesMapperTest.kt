package com.example.usc1.data.repository

import com.example.usc1.domain.model.TenantAppModulesCatalog
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardModulesMapperTest {
    @Test
    fun `extrai e normaliza o mapa efetivo completo`() {
        val modules = TenantAppModulesCatalog.defaultConfig.modules.toMutableMap().apply {
            this["loja"] = false
            this["eventos"] = false
        }

        val result = DashboardModulesMapper.fromPublicDashboardResponse(payloadWith(modules))

        requireNotNull(result)
        assertEquals(TenantAppModulesCatalog.definitions.size, result.size)
        assertFalse(result.getValue("loja"))
        assertFalse(result.getValue("eventos"))
        assertTrue(result.getValue("dashboard"))
    }

    @Test
    fun `rejeita resposta parcial para não liberar módulo ausente`() {
        val partialModules = TenantAppModulesCatalog.defaultConfig.modules - "loja"

        val result = DashboardModulesMapper.fromPublicDashboardResponse(payloadWith(partialModules))

        assertNull(result)
    }

    @Test
    fun `fallback conserva bloqueios da tenant e do último mapa efetivo`() {
        val tenantModules = TenantAppModulesCatalog.defaultConfig.modules.toMutableMap().apply {
            this["eventos"] = false
        }
        val lastEffectiveModules = TenantAppModulesCatalog.defaultConfig.modules.toMutableMap().apply {
            this["loja"] = false
        }

        val result = DashboardModulesMapper.conservativeFallback(
            tenantModules = tenantModules,
            lastEffectiveModules = lastEffectiveModules,
        )

        assertFalse(result.getValue("eventos"))
        assertFalse(result.getValue("loja"))
        assertTrue(result.getValue("dashboard"))
    }

    @Test
    fun `fallback sem endpoint cache ou configuração da tenant bloqueia tudo`() {
        val result = DashboardModulesMapper.fallbackAfterEndpointFailure(
            tenantModules = null,
            lastEffectiveModules = null,
        )

        assertEquals(TenantAppModulesCatalog.definitions.size, result.size)
        assertTrue(result.values.none { it })
    }

    private fun payloadWith(modules: Map<String, Boolean>): JsonObject = buildJsonObject {
        put("modulesConfig", buildJsonObject {
            put("modules", buildJsonObject {
                modules.forEach { (key, enabled) -> put(key, enabled) }
            })
        })
    }
}
