package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.data.supabase.SupabaseTenantResolver
import com.example.usc1.domain.model.AdminMentorshipCatalog
import com.example.usc1.domain.model.AdminMentorshipLabelsConfig
import com.example.usc1.domain.repository.AdminMentorshipRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import java.time.OffsetDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class SupabaseAdminMentorshipRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : AdminMentorshipRepository {
    override suspend fun fetchMentorshipLabels(forceRefresh: Boolean): AdminMentorshipLabelsConfig = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        client.from(AppConfigTable)
            .select(columns = Columns.raw("id,tenant_id,data")) {
                filter {
                    eq("id", buildTenantScopedRowId(tenantId, AdminMentorshipCatalog.LabelsDocId))
                    eq("tenant_id", tenantId)
                }
                limit(count = 1)
            }
            .decodeList<MentorshipLabelsRow>()
            .firstOrNull()
            ?.toDomain()
            ?: AdminMentorshipLabelsConfig()
    }

    override suspend fun saveMentorshipLabels(config: AdminMentorshipLabelsConfig): AdminMentorshipLabelsConfig = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val normalized = AdminMentorshipCatalog.normalize(config)
        client.from(AppConfigTable)
            .upsert(
                MentorshipLabelsUpsertRow(
                    id = buildTenantScopedRowId(tenantId, AdminMentorshipCatalog.LabelsDocId),
                    tenantId = tenantId,
                    data = normalized,
                    updatedAt = OffsetDateTime.now().toString(),
                ),
            ) {
                onConflict = "id"
            }
        normalized
    }

    private fun buildTenantScopedRowId(tenantId: String, baseId: String): String {
        return "tenant:${tenantId.trim()}::${baseId.trim()}"
    }

    private companion object {
        const val AppConfigTable = "app_config"
    }
}

@Serializable
private data class MentorshipLabelsRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
    val data: JsonObject? = null,
) {
    fun toDomain(): AdminMentorshipLabelsConfig {
        val fallback = AdminMentorshipLabelsConfig()
        return AdminMentorshipCatalog.normalize(
            AdminMentorshipLabelsConfig(
                hubTitle = data.string("hubTitle").ifBlank { fallback.hubTitle },
                mentorLabel = data.string("mentorLabel").ifBlank { fallback.mentorLabel },
                menteeLabel = data.string("menteeLabel").ifBlank { fallback.menteeLabel },
                inviteMentorLabel = data.string("inviteMentorLabel").ifBlank { fallback.inviteMentorLabel },
                inviteMenteeLabel = data.string("inviteMenteeLabel").ifBlank { fallback.inviteMenteeLabel },
                requestHelpText = data.string("requestHelpText").ifBlank { fallback.requestHelpText },
            ),
        )
    }
}

@Serializable
private data class MentorshipLabelsUpsertRow(
    val id: String,
    @SerialName("tenant_id") val tenantId: String,
    val data: AdminMentorshipLabelsConfig,
    @SerialName("updatedAt") val updatedAt: String,
)

private fun JsonObject?.string(key: String): String {
    return this?.get(key)?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
}
