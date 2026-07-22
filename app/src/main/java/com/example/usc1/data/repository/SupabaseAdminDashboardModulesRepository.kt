package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.data.supabase.SupabaseTenantResolver
import com.example.usc1.domain.model.AdminDashboardModulesBundle
import com.example.usc1.domain.model.TenantAppModulesCatalog
import com.example.usc1.domain.model.TenantAppModulesConfig
import com.example.usc1.domain.repository.AdminDashboardModulesRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import java.time.OffsetDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class SupabaseAdminDashboardModulesRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : AdminDashboardModulesRepository {
    override suspend fun getModulesBundle(
        tenantName: String,
        tenantSlug: String,
        forceRefresh: Boolean,
    ): AdminDashboardModulesBundle = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val modulesConfig = fetchTenantAppModulesConfig(client, tenantId)
        val profilesConfig = fetchTenantAdminSidebarProfilesConfig(client)
        val profileKey = fetchTenantAdminSidebarProfileAssignment(
            client = client,
            tenantId = tenantId,
            tenantSlug = tenantSlug,
            profilesConfig = profilesConfig,
        )
        val activeProfile = profilesConfig.resolveProfile(profileKey)

        AdminDashboardModulesBundle(
            tenantId = tenantId,
            tenantName = tenantName,
            tenantSlug = tenantSlug,
            activeProfileName = activeProfile.name,
            config = modulesConfig,
            groups = TenantAppModulesCatalog.groupDefinitions(
                modules = modulesConfig.modules,
                profileAppModules = activeProfile.appModules,
            ),
        )
    }

    override suspend fun saveModulesConfig(config: TenantAppModulesConfig): Unit = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val normalizedModules = TenantAppModulesCatalog.normalizeModules(config.modules)
        client.from(AppConfigTable).upsert(
            AppConfigUpsertRow(
                id = buildTenantScopedRowId(tenantId, AppModulesDocId),
                tenantId = tenantId,
                data = buildJsonObject {
                    put("modules", normalizedModules.toJsonObject())
                },
                updatedAt = OffsetDateTime.now().toString(),
            ),
        ) {
            onConflict = "id"
        }
    }

    private suspend fun fetchTenantAppModulesConfig(
        client: SupabaseClient,
        tenantId: String,
    ): TenantAppModulesConfig {
        val row = client.from(AppConfigTable)
            .select(columns = Columns.raw("id,data")) {
                filter {
                    eq("id", buildTenantScopedRowId(tenantId, AppModulesDocId))
                }
                limit(count = 1)
            }
            .decodeList<AppConfigRow>()
            .firstOrNull()

        val rawModules = row?.data.asJsonObjectOrEmpty()
            .jsonObject("modules")
            .booleanMap()

        return TenantAppModulesConfig(
            modules = TenantAppModulesCatalog.normalizeModules(rawModules),
        )
    }

    private suspend fun fetchTenantAdminSidebarProfilesConfig(
        client: SupabaseClient,
    ): TenantAdminSidebarProfilesConfig {
        val row = client.from(AppConfigTable)
            .select(columns = Columns.raw("id,data")) {
                filter {
                    eq("id", AdminSidebarProfilesDocId)
                }
                limit(count = 1)
            }
            .decodeList<AppConfigRow>()
            .firstOrNull()

        return normalizeProfilesConfig(row?.data)
    }

    private suspend fun fetchTenantAdminSidebarProfileAssignment(
        client: SupabaseClient,
        tenantId: String,
        tenantSlug: String,
        profilesConfig: TenantAdminSidebarProfilesConfig,
    ): String {
        val row = client.from(AppConfigTable)
            .select(columns = Columns.raw("id,data")) {
                filter {
                    eq("id", buildTenantScopedRowId(tenantId, AdminSidebarAssignmentDocId))
                }
                limit(count = 1)
            }
            .decodeList<AppConfigRow>()
            .firstOrNull()

        val storedProfileKey = row?.data.asJsonObjectOrEmpty()
            .stringValue("profileKey")
            .trim()
            .takeIf { it.isNotBlank() }

        if (storedProfileKey != null && profilesConfig.profiles.containsKey(storedProfileKey)) {
            return storedProfileKey
        }

        return defaultProfileKey(tenantSlug, profilesConfig)
    }

    private fun normalizeProfilesConfig(raw: JsonElement?): TenantAdminSidebarProfilesConfig {
        val source = raw.asJsonObjectOrEmpty()
        val profilesRaw = source.jsonObject("profiles") ?: source
        val profileKeys = linkedSetOf("A", "B")
        profilesRaw.keys.forEach { key ->
            key.trim().takeIf { it.isNotBlank() }?.let(profileKeys::add)
        }
        val profiles = profileKeys.associateWith { key ->
            normalizeProfileDefinition(profilesRaw[key], key)
        }
        val order = source.jsonArray("order")
            ?.mapNotNull { entry -> entry.stringOrNull()?.trim()?.takeIf { profiles.containsKey(it) } }
            ?.distinct()
            ?.takeIf { it.isNotEmpty() }
            ?: profiles.keys.toList().ifEmpty { listOf("A", "B") }

        return TenantAdminSidebarProfilesConfig(order = order, profiles = profiles)
    }

    private fun normalizeProfileDefinition(
        raw: JsonElement?,
        fallbackKey: String,
    ): TenantAdminSidebarProfileDefinition {
        val source = raw.asJsonObjectOrEmpty()
        val defaultProfile = buildDefaultProfileDefinition(fallbackKey)
        return TenantAdminSidebarProfileDefinition(
            name = source.stringValue("name").ifBlank { defaultProfile.name },
            appModules = source.jsonObject("appModules").booleanMap(),
        )
    }

    private fun buildDefaultProfileDefinition(key: String): TenantAdminSidebarProfileDefinition {
        return TenantAdminSidebarProfileDefinition(
            name = when (key) {
                "A" -> "Perfil A"
                "B" -> "Perfil B"
                else -> "Perfil $key"
            },
            appModules = emptyMap(),
        )
    }

    private fun TenantAdminSidebarProfilesConfig.resolveProfile(
        profileKey: String?,
    ): TenantAdminSidebarProfileDefinition {
        if (!profileKey.isNullOrBlank()) {
            profiles[profileKey]?.let { return it }
        }
        val fallbackKey = order.firstOrNull { profiles.containsKey(it) }
            ?: profiles.keys.firstOrNull()
            ?: "A"
        return profiles[fallbackKey] ?: buildDefaultProfileDefinition(fallbackKey)
    }

    private fun defaultProfileKey(
        tenantSlug: String,
        config: TenantAdminSidebarProfilesConfig,
    ): String {
        val preferred = when (tenantSlug.trim().lowercase()) {
            "aaaenf" -> "A"
            "aaakn" -> "B"
            else -> null
        }
        if (preferred != null && config.profiles.containsKey(preferred)) return preferred
        return config.order.firstOrNull { config.profiles.containsKey(it) }
            ?: config.profiles.keys.firstOrNull()
            ?: "A"
    }

    private fun buildTenantScopedRowId(tenantId: String, baseId: String): String {
        val cleanTenantId = tenantId.trim()
        val cleanBaseId = baseId.trim()
        if (cleanTenantId.isBlank()) return cleanBaseId
        return "tenant:$cleanTenantId::$cleanBaseId"
    }

    private companion object {
        const val AppConfigTable = "app_config"
        const val AppModulesDocId = "app_modules"
        const val AdminSidebarProfilesDocId = "tenant_admin_sidebar_profiles"
        const val AdminSidebarAssignmentDocId = "tenant_admin_sidebar_profile_assignment"
    }
}

private data class TenantAdminSidebarProfilesConfig(
    val order: List<String>,
    val profiles: Map<String, TenantAdminSidebarProfileDefinition>,
)

private data class TenantAdminSidebarProfileDefinition(
    val name: String,
    val appModules: Map<String, Boolean>,
)

@Serializable
private data class AppConfigRow(
    val id: String = "",
    val data: JsonElement? = null,
)

@Serializable
private data class AppConfigUpsertRow(
    val id: String,
    @SerialName("tenant_id") val tenantId: String,
    val data: JsonObject,
    @SerialName("updatedAt") val updatedAt: String,
)

private fun JsonElement?.asJsonObjectOrEmpty(): JsonObject {
    return when (this) {
        is JsonObject -> this
        else -> JsonObject(emptyMap())
    }
}

private fun JsonElement?.stringOrNull(): String? {
    if (this == null || this is JsonNull) return null
    return runCatching { jsonPrimitive.contentOrNull }.getOrNull()
}

private fun JsonObject.stringValue(key: String): String {
    return this[key].stringOrNull().orEmpty()
}

private fun JsonObject.jsonObject(key: String): JsonObject? {
    return this[key] as? JsonObject
}

private fun JsonObject.jsonArray(key: String): JsonArray? {
    return this[key] as? JsonArray
}

private fun JsonObject?.booleanMap(): Map<String, Boolean> {
    if (this == null) return emptyMap()
    return entries.mapNotNull { (key, value) ->
        val booleanValue = (value as? JsonPrimitive)?.runCatchingBoolean()
        if (booleanValue == null) null else key to booleanValue
    }.toMap()
}

private fun JsonPrimitive.runCatchingBoolean(): Boolean? {
    return when {
        isString -> null
        content == "true" -> true
        content == "false" -> false
        else -> null
    }
}

private fun Map<String, Boolean>.toJsonObject(): JsonObject {
    return buildJsonObject {
        forEach { (key, value) ->
            put(key, JsonPrimitive(value))
        }
    }
}
