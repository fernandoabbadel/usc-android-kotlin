package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider

private const val PublicWebBaseUrl = "https://usc-atleticas.vercel.app"

/**
 * Bucket padrao do web-reference (`src/lib/supa/storage.ts`):
 * "public" e reservado no Supabase Storage, entao o padrao seguro e "uploads".
 */
private const val DefaultStorageBucket = "uploads"

private val ImageExtensions = listOf(".png", ".jpg", ".jpeg", ".webp", ".gif", ".svg", ".avif")

/**
 * Converte o valor gravado no Supabase em uma URL carregavel pelo Coil.
 *
 * Cobre os mesmos formatos aceitos por `parseStorageUrl`/`splitBucketAndPath` do web-reference:
 * URL absoluta, data URI, protocolo relativo, caminho `/storage/v1/object/...`,
 * formato explicito `bucket:path`, asset publico do Next (`/logo.png`) e
 * caminho relativo dentro do bucket padrao (`tenants/aaakn/capa.png`).
 */
internal fun resolveRemoteImageUrl(value: String?): String? {
    val clean = value?.trim().orEmpty()
    if (clean.isBlank()) return null
    if (clean.startsWith("http://", ignoreCase = true)) return clean
    if (clean.startsWith("https://", ignoreCase = true)) return clean
    if (clean.startsWith("data:image/", ignoreCase = true)) return clean
    if (clean.startsWith("//")) return "https:$clean"

    val supabaseUrl = SupabaseClientProvider.config.url.trim().trimEnd('/')
    val withoutLeadingSlash = clean.trimStart('/')

    // Caminho direto de storage ja montado pelo backend.
    if (withoutLeadingSlash.startsWith("storage/v1/object/", ignoreCase = true)) {
        return if (supabaseUrl.isNotBlank()) "$supabaseUrl/$withoutLeadingSlash" else null
    }

    // Formato explicito "bucket:path" aceito pelo splitBucketAndPath do web.
    val bucketSeparator = withoutLeadingSlash.indexOf(':')
    if (bucketSeparator > 0 && supabaseUrl.isNotBlank()) {
        val bucket = withoutLeadingSlash.take(bucketSeparator).trim()
        val objectPath = withoutLeadingSlash.drop(bucketSeparator + 1).trim().trimStart('/')
        if (bucket.isNotBlank() && objectPath.isNotBlank() && !bucket.contains('/')) {
            return "$supabaseUrl/storage/v1/object/public/$bucket/${encodeStoragePath(objectPath)}"
        }
    }

    val looksLikeImage = ImageExtensions.any { clean.endsWith(it, ignoreCase = true) }

    // Assets estaticos do Next ficam em /public e sao servidos pelo dominio web.
    if (clean.startsWith("/")) {
        return "$PublicWebBaseUrl$clean"
    }

    // Caminho relativo dentro do bucket padrao (ex.: "tenants/aaakn/capa.png").
    if (withoutLeadingSlash.contains('/') && supabaseUrl.isNotBlank()) {
        return "$supabaseUrl/storage/v1/object/public/$DefaultStorageBucket/" +
            encodeStoragePath(withoutLeadingSlash)
    }

    // Arquivo solto com extensao de imagem: assume asset publico do web.
    if (looksLikeImage) {
        return "$PublicWebBaseUrl/$withoutLeadingSlash"
    }

    return null
}

private fun encodeStoragePath(path: String): String {
    return path.split("/").joinToString("/") { segment ->
        if (segment.isBlank()) {
            segment
        } else {
            java.net.URLEncoder.encode(segment, "UTF-8")
                .replace("+", "%20")
                .replace("%2F", "/")
        }
    }
}
