package com.example.usc1.data.repository

import com.example.usc1.BuildConfig
import com.example.usc1.core.tenant.TenantPalette
import com.example.usc1.domain.model.PublicTenant
import com.example.usc1.domain.repository.PublicTenantRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class WebPublicTenantRepository(
    private val client: HttpClient = PublicTenantHttpClient.instance,
    private val baseUrl: String = BuildConfig.WEB_APP_URL,
    private val json: Json = PublicTenantJson,
) : PublicTenantRepository {
    override suspend fun getDirectory(): List<PublicTenant> {
        val response = client.get(endpoint()) {
            parameter("limit", DirectoryLimit)
        }
        check(response.status.isSuccess()) {
            "Não foi possível carregar as atléticas públicas (${response.status.value})."
        }

        return PublicTenantPayloadMapper.decodeDirectory(
            payload = response.bodyAsText(),
            json = json,
        )
    }

    override suspend fun resolveActiveTenant(
        tenantId: String,
        tenantSlug: String,
    ): PublicTenant? {
        val cleanTenantId = tenantId.trim()
        val cleanTenantSlug = tenantSlug.trim().lowercase()
        if (cleanTenantId.isBlank() || cleanTenantSlug.isBlank()) return null

        val response = client.get(endpoint()) {
            parameter("slug", cleanTenantSlug)
        }
        if (response.status == HttpStatusCode.NotFound) return null
        check(response.status.isSuccess()) {
            "Não foi possível validar a atlética selecionada (${response.status.value})."
        }

        return PublicTenantPayloadMapper.decodeResolvedTenant(
            payload = response.bodyAsText(),
            expectedTenantId = cleanTenantId,
            expectedTenantSlug = cleanTenantSlug,
            json = json,
        )
    }

    private fun endpoint(): String {
        val cleanBaseUrl = baseUrl.trim().trimEnd('/')
        require(cleanBaseUrl.startsWith("https://") || cleanBaseUrl.startsWith("http://")) {
            "WEB_APP_URL inválida para o diretório público."
        }
        return "$cleanBaseUrl/api/public/tenants"
    }

    private companion object {
        const val DirectoryLimit = 60
    }
}

private object PublicTenantHttpClient {
    val instance: HttpClient by lazy {
        HttpClient(Android) {
            expectSuccess = false
        }
    }
}

private val PublicTenantJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

internal object PublicTenantPayloadMapper {
    fun decodeDirectory(
        payload: String,
        json: Json = PublicTenantJson,
    ): List<PublicTenant> {
        return json.decodeFromString<List<PublicTenantPayload>>(payload)
            .mapNotNull(PublicTenantPayload::toDirectoryTenantOrNull)
            .sortedBy { tenant -> tenant.name.lowercase() }
    }

    fun decodeResolvedTenant(
        payload: String,
        expectedTenantId: String,
        expectedTenantSlug: String,
        json: Json = PublicTenantJson,
    ): PublicTenant? {
        val cleanExpectedId = expectedTenantId.trim()
        val cleanExpectedSlug = expectedTenantSlug.trim().lowercase()
        if (cleanExpectedId.isBlank() || cleanExpectedSlug.isBlank()) return null

        return json.decodeFromString<PublicTenantPayload>(payload)
            .toActiveTenantOrNull()
            ?.takeIf { tenant ->
                tenant.id == cleanExpectedId && tenant.slug == cleanExpectedSlug
            }
    }
}

@Serializable
private data class PublicTenantPayload(
    val id: String = "",
    val nome: String = "",
    val sigla: String = "",
    val slug: String = "",
    val faculdade: String = "",
    val curso: String = "",
    val cidade: String = "",
    val logoUrl: String = "",
    val paletteKey: String = "green",
    val visibleInDirectory: Boolean = true,
    val status: String = "active",
) {
    fun toDirectoryTenantOrNull(): PublicTenant? {
        if (!visibleInDirectory) return null
        return toActiveTenantOrNull()
    }

    fun toActiveTenantOrNull(): PublicTenant? {
        val cleanId = id.trim()
        val cleanSlug = slug.trim().lowercase()
        if (cleanId.isBlank() || cleanSlug.isBlank() || status.trim().lowercase() != "active") {
            return null
        }

        return PublicTenant(
            id = cleanId,
            slug = cleanSlug,
            name = nome.trim().ifBlank { sigla.trim().ifBlank { cleanSlug.uppercase() } },
            acronym = sigla.trim(),
            institution = faculdade.trim(),
            course = curso.trim(),
            city = cidade.trim(),
            logoUrl = logoUrl.trim().ifBlank { null },
            palette = TenantPalette.entries.firstOrNull { palette ->
                palette.remoteValue == paletteKey.trim().lowercase()
            } ?: TenantPalette.Green,
        )
    }
}
