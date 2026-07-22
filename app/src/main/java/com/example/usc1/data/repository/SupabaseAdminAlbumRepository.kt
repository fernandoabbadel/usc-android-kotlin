package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.data.supabase.SupabaseTenantResolver
import com.example.usc1.domain.model.AdminAlbumCatalog
import com.example.usc1.domain.model.AdminAlbumUiConfig
import com.example.usc1.domain.repository.AdminAlbumRepository
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

class SupabaseAdminAlbumRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : AdminAlbumRepository {
    override suspend fun getAlbumUiConfig(forceRefresh: Boolean): AdminAlbumUiConfig = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val docId = buildTenantScopedRowId(tenantId, AdminAlbumCatalog.ConfigDocId)
        client.from(AppConfigTable)
            .select(columns = Columns.raw("id,tenant_id,capa,titulo,subtitulo,data")) {
                filter {
                    eq("id", docId)
                    eq("tenant_id", tenantId)
                }
                limit(count = 1)
            }
            .decodeList<AlbumUiConfigRow>()
            .firstOrNull()
            ?.toDomain()
            ?: AdminAlbumUiConfig()
    }

    override suspend fun saveAlbumUiConfig(config: AdminAlbumUiConfig): Unit = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val normalized = AdminAlbumUiConfig(
            cover = config.cover.trim().take(AdminAlbumCatalog.MaxUrlLength),
            title = config.title.trim().take(AdminAlbumCatalog.MaxTitleLength),
            subtitle = config.subtitle.trim().take(AdminAlbumCatalog.MaxSubtitleLength),
        )
        client.from(AppConfigTable)
            .upsert(
                AlbumUiConfigUpsertRow(
                    id = buildTenantScopedRowId(tenantId, AdminAlbumCatalog.ConfigDocId),
                    tenantId = tenantId,
                    cover = normalized.cover,
                    title = normalized.title,
                    subtitle = normalized.subtitle,
                    updatedAt = OffsetDateTime.now().toString(),
                ),
            ) {
                onConflict = "id"
            }
    }

    private fun buildTenantScopedRowId(tenantId: String, baseId: String): String {
        return "tenant:${tenantId.trim()}::${baseId.trim()}"
    }

    private companion object {
        const val AppConfigTable = "app_config"
    }
}

@Serializable
private data class AlbumUiConfigRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
    val capa: String? = null,
    val titulo: String? = null,
    val subtitulo: String? = null,
    val data: JsonObject? = null,
) {
    fun toDomain(): AdminAlbumUiConfig {
        return AdminAlbumUiConfig(
            cover = capa?.trim().orEmpty().ifBlank { data.string("capa").ifBlank { AdminAlbumUiConfig().cover } },
            title = titulo?.trim().orEmpty().ifBlank { data.string("titulo").ifBlank { AdminAlbumUiConfig().title } },
            subtitle = subtitulo?.trim().orEmpty().ifBlank { data.string("subtitulo").ifBlank { AdminAlbumUiConfig().subtitle } },
        )
    }
}

@Serializable
private data class AlbumUiConfigUpsertRow(
    val id: String,
    @SerialName("tenant_id") val tenantId: String,
    @SerialName("capa") val cover: String,
    @SerialName("titulo") val title: String,
    @SerialName("subtitulo") val subtitle: String,
    @SerialName("updatedAt") val updatedAt: String,
)

private fun JsonObject?.string(key: String): String {
    return this?.get(key)?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
}
