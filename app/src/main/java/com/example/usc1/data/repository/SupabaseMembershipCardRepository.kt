package com.example.usc1.data.repository

import com.example.usc1.BuildConfig
import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.domain.model.MembershipCardConfig
import com.example.usc1.domain.repository.MembershipCardRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.math.roundToInt

class SupabaseMembershipCardRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
    private val supabaseUrlProvider: () -> String = { BuildConfig.SUPABASE_URL },
    private val webAppUrlProvider: () -> String = { BuildConfig.WEB_APP_URL },
) : MembershipCardRepository {
    override suspend fun getConfig(tenantId: String): MembershipCardConfig {
        if (!SupabaseClientProvider.config.isConfigured) return MembershipCardConfig()

        val scopedId = buildMembershipCardConfigId(tenantId)
        val row = clientProvider()
            .from(AppConfigTable)
            .select(columns = Columns.raw(ConfigColumns)) {
                filter {
                    eq("id", scopedId)
                }
                limit(count = 1)
            }
            .decodeList<MembershipCardConfigRow>()
            .firstOrNull()

        return normalizeMembershipCardConfig(
            validity = row?.validade,
            backgrounds = row?.backgrounds,
            data = row?.data,
            supabaseUrl = supabaseUrlProvider(),
            webAppUrl = webAppUrlProvider(),
        )
    }

    private companion object {
        const val AppConfigTable = "app_config"
        const val ConfigColumns = "id,validade,backgrounds,data"
    }
}

@Serializable
private data class MembershipCardConfigRow(
    val id: String = "",
    val validade: String? = null,
    val backgrounds: JsonElement? = null,
    val data: JsonElement? = null,
)

internal fun buildMembershipCardConfigId(tenantId: String): String {
    val normalizedTenantId = tenantId.trim()
    return if (normalizedTenantId.isBlank()) {
        "carteirinha"
    } else {
        "tenant:$normalizedTenantId::carteirinha"
    }
}

internal fun normalizeMembershipCardConfig(
    validity: String?,
    backgrounds: JsonElement?,
    data: JsonElement?,
    supabaseUrl: String,
    webAppUrl: String,
): MembershipCardConfig {
    val normalizedValidity = validity
        .orEmpty()
        .trim()
        .ifBlank { MembershipCardConfig.DefaultValidity }
        .take(24)
    val dataObject = data as? JsonObject
    val normalizedBackgrounds = linkedMapOf<String, String>()

    addBackgroundEntries(
        source = backgrounds as? JsonObject,
        destination = normalizedBackgrounds,
        supabaseUrl = supabaseUrl,
        webAppUrl = webAppUrl,
    )
    addBackgroundEntries(
        source = dataObject?.get("backgrounds") as? JsonObject,
        destination = normalizedBackgrounds,
        supabaseUrl = supabaseUrl,
        webAppUrl = webAppUrl,
    )
    addBackgroundEntries(
        source = dataObject?.get("backgroundAssets") as? JsonObject,
        destination = normalizedBackgrounds,
        supabaseUrl = supabaseUrl,
        webAppUrl = webAppUrl,
    )

    val opacity = (dataObject?.get("backgroundOpacity") as? JsonPrimitive)
        ?.doubleOrNull
        ?.takeIf(Double::isFinite)
        ?.roundToInt()
        ?.coerceIn(0, 100)
        ?: MembershipCardConfig.DefaultBackgroundOpacity

    return MembershipCardConfig(
        validity = normalizedValidity,
        backgroundUrls = normalizedBackgrounds,
        backgroundOpacity = opacity,
    )
}

internal fun normalizeMembershipClassCode(value: String): String {
    val normalized = value.trim().uppercase()
    val exact = Regex("^T\\d{1,3}$").matchEntire(normalized)
    if (exact != null) {
        return "T${normalized.drop(1).toIntOrNull() ?: return ""}"
    }

    val digits = normalized.filter(Char::isDigit)
    return digits.toIntOrNull()?.let { "T$it" }.orEmpty()
}

private fun addBackgroundEntries(
    source: JsonObject?,
    destination: MutableMap<String, String>,
    supabaseUrl: String,
    webAppUrl: String,
) {
    source?.forEach { (rawClassCode, rawValue) ->
        val classCode = normalizeMembershipClassCode(rawClassCode)
        if (classCode.isBlank()) return@forEach
        resolveMembershipBackgroundUrl(
            rawValue = rawValue,
            supabaseUrl = supabaseUrl,
            webAppUrl = webAppUrl,
        )?.let { destination[classCode] = it }
    }
}

internal fun resolveMembershipBackgroundUrl(
    rawValue: JsonElement?,
    supabaseUrl: String,
    webAppUrl: String,
): String? {
    if (rawValue is JsonPrimitive && rawValue.isString) {
        return normalizeRemoteMembershipUrl(rawValue.content, webAppUrl)
    }

    val rawObject = rawValue as? JsonObject ?: return null
    val directUrl = rawObject.string("url")
    if (directUrl.isNotBlank()) return normalizeRemoteMembershipUrl(directUrl, webAppUrl)

    val bucket = rawObject.string("bucket")
    val path = rawObject.string("path")
        .ifBlank { rawObject.string("objectPath") }
        .ifBlank { rawObject.string("fullPath") }
    if (bucket.isBlank() || path.isBlank() || supabaseUrl.isBlank()) return null

    val encodedBucket = encodeUrlSegment(bucket)
    val encodedPath = path
        .split('/')
        .filter(String::isNotBlank)
        .joinToString("/") { encodeUrlSegment(it) }
    if (encodedPath.isBlank()) return null

    val versionToken = rawObject.string("versionToken")
    val versionSuffix = versionToken
        .takeIf(String::isNotBlank)
        ?.let { "?v=${encodeUrlSegment(it)}" }
        .orEmpty()
    return "${supabaseUrl.trimEnd('/')}/storage/v1/object/public/$encodedBucket/$encodedPath$versionSuffix"
}

private fun JsonObject.string(key: String): String {
    return (this[key] as? JsonPrimitive)
        ?.takeIf(JsonPrimitive::isString)
        ?.content
        ?.trim()
        .orEmpty()
}

private fun normalizeRemoteMembershipUrl(rawValue: String, webAppUrl: String): String? {
    val value = rawValue.trim()
    return when {
        value.startsWith("https://") || value.startsWith("http://") -> value
        value.startsWith("/") && webAppUrl.isNotBlank() -> "${webAppUrl.trimEnd('/')}$value"
        else -> null
    }
}

private fun encodeUrlSegment(value: String): String {
    return URLEncoder.encode(value.trim(), StandardCharsets.UTF_8.toString())
        .replace("+", "%20")
}
