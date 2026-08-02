package com.example.usc1.domain.model

/**
 * Regras de upload de imagem da loja, portadas de `web-reference/src/lib/upload.ts`.
 *
 * O web valida antes de tocar no Storage: tipo, tamanho, resolução, e três guardas de custo
 * (intervalo mínimo, teto por minuto e dedupe por impressão digital do arquivo). O Android
 * reproduz a mesma sequência porque o bucket é o mesmo do web e o custo de egress é o mesmo.
 */
object StoreImageUpload {

    // upload.ts 39-49
    const val MaxUploadImageMb = 2
    const val MaxUploadImageBytes = MaxUploadImageMb * 1024L * 1024L
    const val MaxUploadImageWidth = 2400
    const val MaxUploadImageHeight = 2400
    const val MaxUploadImagePixels = MaxUploadImageWidth.toLong() * MaxUploadImageHeight.toLong()
    const val VersionedPublicAssetCacheControl = "31536000"
    const val DefaultCacheControl = "3600"

    val AllowedUploadImageTypes = listOf("image/jpeg", "image/png", "image/webp")

    // upload.ts 51-55
    const val DefaultMinUploadIntervalMs = 1_200L
    const val DefaultUploadRateLimitWindowMs = 60_000L
    const val DefaultUploadRateLimitMax = 6
    const val DefaultUploadDedupeWindowMs = 45_000L
    const val DefaultCompressedUploadMaxBytes = 200L * 1024L

    // upload.ts 327-333: a compressão roda antes do envio para reduzir Storage e egress.
    const val CompressionMaxWidth = 1600
    const val CompressionMaxHeight = 1600
    const val CompressionQuality = 82

    const val DefaultBucket = "uploads"

    /** upload.ts 61-85. Devolve a mensagem de erro do web, ou `null` quando o arquivo passa. */
    fun validateImageFile(
        sizeBytes: Long,
        mimeType: String?,
        maxBytes: Long = MaxUploadImageBytes,
        allowedTypes: List<String> = AllowedUploadImageTypes,
    ): String? {
        val cleanType = mimeType?.trim()?.lowercase().orEmpty()
        if (cleanType !in allowedTypes) return "Formato inválido. Use JPG, PNG ou WEBP."
        if (sizeBytes > maxBytes) {
            if (maxBytes < 1024L * 1024L) {
                val kbLimit = maxOf(50L, Math.round(maxBytes / 1024.0))
                return "A imagem excede ${kbLimit}KB."
            }
            val mbLimit = maxOf(1.0, Math.round(maxBytes / (1024.0 * 1024.0) * 10.0) / 10.0)
            val rendered = if (mbLimit % 1.0 == 0.0) mbLimit.toInt().toString() else mbLimit.toString()
            return "A imagem excede ${rendered}MB."
        }
        return null
    }

    /** upload.ts 200-225. */
    fun validateImageDimensions(
        width: Int,
        height: Int,
        maxWidth: Int = MaxUploadImageWidth,
        maxHeight: Int = MaxUploadImageHeight,
        maxPixels: Long = MaxUploadImagePixels,
    ): String? {
        if (width <= 0 || height <= 0) return "Não foi possível processar a imagem."
        if (width > maxWidth || height > maxHeight) return "Resolucao maxima: ${maxWidth}x${maxHeight}."
        if (width.toLong() * height.toLong() > maxPixels) return "Imagem muito grande. Reduza a resolucao."
        return null
    }

    /** upload.ts 87-92. O `|| "file"` do web vale para a cadeia inteira, não só para o trim. */
    fun sanitizeStoragePathSegment(value: String): String {
        val sanitized = value
            .replace(Regex("[^a-zA-Z0-9._-]+"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .lowercase()
        return sanitized.ifEmpty { "file" }
    }

    /**
     * upload.ts 94-99. O `filter(Boolean)` do web nunca corta nada, porque
     * [sanitizeStoragePathSegment] já devolve "file" para segmento vazio — então `a//b` vira
     * `a/file/b`, e não `a/b`.
     */
    fun normalizeStoragePath(path: String): String =
        path.split("/").joinToString("/") { sanitizeStoragePathSegment(it) }

    /** upload.ts 101-105. */
    fun detectExtension(mimeType: String?): String = when (mimeType?.trim()?.lowercase()) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        else -> "jpg"
    }

    /** upload.ts 107-108. */
    fun buildDraftAssetFileName(baseName: String, nowMs: Long): String =
        "${sanitizeStoragePathSegment(baseName)}-$nowMs"

    /** upload.ts 110-126. Base 36 de tamanho e data de modificação, como no web. */
    fun buildFileMetadataVersionToken(sizeBytes: Long, lastModifiedMs: Long): String? {
        val sizeToken = if (sizeBytes >= 0) sizeBytes.toString(36) else ""
        val modifiedToken = if (lastModifiedMs > 0) lastModifiedMs.toString(36) else ""
        val token = listOf(sizeToken, modifiedToken).filter { it.isNotEmpty() }.joinToString("-")
        return token.ifEmpty { null }
    }

    /** upload.ts 128-139. */
    fun appendAssetVersionQuery(url: String?, versionToken: String?): String? {
        val cleanUrl = url?.trim().orEmpty()
        if (cleanUrl.isEmpty()) return null
        val cleanToken = versionToken?.trim().orEmpty()
        if (cleanToken.isEmpty()) return cleanUrl
        val separator = if (cleanUrl.contains("?")) "&" else "?"
        return "$cleanUrl$separator" + "v=" + encodeUriComponent(cleanToken)
    }

    /** upload.ts 150-161. O nome vindo do chamador ganha extensão só se não tiver uma. */
    fun resolveOutputFileName(
        sourceName: String,
        mimeType: String?,
        fileNameHint: String?,
        nowMs: Long,
    ): String {
        val hint = fileNameHint?.trim().orEmpty()
        if (hint.isNotEmpty()) {
            val safeHint = sanitizeStoragePathSegment(hint)
            val hasExtension = Regex("\\.[a-z0-9]{2,5}$", RegexOption.IGNORE_CASE).containsMatchIn(safeHint)
            return if (hasExtension) safeHint else "$safeHint.${detectExtension(mimeType)}"
        }
        return "$nowMs-${sanitizeStoragePathSegment(sourceName)}"
    }

    /** upload.ts 163-164. */
    fun buildUploadFingerprint(
        name: String,
        mimeType: String?,
        sizeBytes: Long,
        lastModifiedMs: Long,
    ): String = "${name.lowercase()}::${mimeType.orEmpty()}::$sizeBytes::$lastModifiedMs"

    private fun encodeUriComponent(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}

/** upload.ts 13. */
enum class StoreUploadVersionStrategy { None, FileMetadata }

/** Espelha o `UploadImageOptions` do web (upload.ts 15-37) no recorte que a loja usa. */
data class StoreImageUploadOptions(
    val scopeKey: String,
    val fileName: String? = null,
    val upsert: Boolean = false,
    val versionStrategy: StoreUploadVersionStrategy = StoreUploadVersionStrategy.None,
    val cacheControl: String = StoreImageUpload.DefaultCacheControl,
    val maxBytes: Long = StoreImageUpload.MaxUploadImageBytes,
    val compressionMaxBytes: Long = StoreImageUpload.DefaultCompressedUploadMaxBytes,
    val allowOriginalOnCompressionFail: Boolean = false,
)

/** upload.ts 5-11. */
data class StoreImageUploadResult(
    val url: String? = null,
    val error: String? = null,
    val bucket: String? = null,
    val objectPath: String? = null,
    val versionToken: String? = null,
)

/**
 * Guardas de custo do upload (upload.ts 227-290), portadas como máquina de estado pura para
 * poderem ser testadas sem Storage. O web mantém esse estado em `Map` de módulo; aqui ele é
 * uma instância só, com o mesmo escopo por chave.
 */
class StoreUploadGuard(
    private val minIntervalMs: Long = StoreImageUpload.DefaultMinUploadIntervalMs,
    private val rateLimitWindowMs: Long = StoreImageUpload.DefaultUploadRateLimitWindowMs,
    private val rateLimitMax: Int = StoreImageUpload.DefaultUploadRateLimitMax,
    private val dedupeWindowMs: Long = StoreImageUpload.DefaultUploadDedupeWindowMs,
) {
    private val historyByScope = mutableMapOf<String, MutableList<Long>>()
    private val fingerprintByScope = mutableMapOf<String, MutableMap<String, Long>>()
    private val inFlightScopes = mutableSetOf<String>()

    /** upload.ts 248-286. Devolve a mensagem de recusa, ou `null` quando o envio pode seguir. */
    @Synchronized
    fun reserve(scope: String, fingerprint: String, nowMs: Long): String? {
        cleanup(scope, nowMs)

        if (inFlightScopes.contains(scope)) return "Upload ja em andamento. Aguarde terminar."

        val history = historyByScope[scope] ?: mutableListOf()
        val lastTimestamp = history.lastOrNull() ?: 0L
        if (nowMs - lastTimestamp < minIntervalMs) return "Aguarde alguns segundos antes de novo upload."

        val recent = history.filter { nowMs - it <= rateLimitWindowMs }
        if (recent.size >= rateLimitMax) return "Limite de uploads por minuto atingido."

        val previousFingerprintAt = fingerprintByScope[scope]?.get(fingerprint)
        if (previousFingerprintAt != null && nowMs - previousFingerprintAt <= dedupeWindowMs) {
            return "Arquivo repetido detectado. Evite uploads duplicados."
        }

        historyByScope[scope] = (recent + nowMs).toMutableList()
        inFlightScopes.add(scope)
        return null
    }

    /** upload.ts 395-399: o dedupe só é registrado depois de o Storage aceitar o arquivo. */
    @Synchronized
    fun registerSuccess(scope: String, fingerprint: String, nowMs: Long) {
        val cache = fingerprintByScope.getOrPut(scope) { mutableMapOf() }
        cache[fingerprint] = nowMs
    }

    /** upload.ts 288-290. */
    @Synchronized
    fun release(scope: String) {
        inFlightScopes.remove(scope)
    }

    private fun cleanup(scope: String, nowMs: Long) {
        val history = historyByScope[scope] ?: mutableListOf()
        historyByScope[scope] = history.filter { nowMs - it <= rateLimitWindowMs }.toMutableList()

        val fingerprints = fingerprintByScope[scope] ?: return
        fingerprintByScope[scope] = fingerprints
            .filterValues { nowMs - it <= dedupeWindowMs }
            .toMutableMap()
    }
}

/**
 * Caminhos e escopos do upload da loja, portados de
 * `web-reference/src/app/admin/loja/produtos/page.tsx` 727-799.
 *
 * Alvo estável (categoria já nomeada, produto já criado) grava sempre no mesmo objeto com
 * `upsert` e versiona a URL; rascunho grava com nome único e sem versão.
 */
object StoreUploadTargets {

    fun categoryCover(tenantId: String, categoryName: String, nowMs: Long): StoreUploadTarget {
        val tenantScope = StoreImageUpload.sanitizeStoragePathSegment(tenantId.ifBlank { "global" })
        val stableCategoryId = categoryName.trim().takeIf { it.isNotEmpty() }
            ?.let { StoreImageUpload.sanitizeStoragePathSegment(it) }
            .orEmpty()
        val isStable = stableCategoryId.isNotEmpty()
        return StoreUploadTarget(
            path = if (isStable) {
                "store/$tenantScope/categorias/$stableCategoryId"
            } else {
                "store/$tenantScope/categorias/drafts"
            },
            options = StoreImageUploadOptions(
                scopeKey = "store:category:$tenantScope:${stableCategoryId.ifEmpty { "draft" }}",
                fileName = if (isStable) "cover" else StoreImageUpload.buildDraftAssetFileName("cover", nowMs),
                upsert = isStable,
                versionStrategy = if (isStable) {
                    StoreUploadVersionStrategy.FileMetadata
                } else {
                    StoreUploadVersionStrategy.None
                },
                cacheControl = StoreImageUpload.VersionedPublicAssetCacheControl,
            ),
        )
    }

    fun productImage(tenantId: String, productId: String, nowMs: Long): StoreUploadTarget {
        val tenantScope = StoreImageUpload.sanitizeStoragePathSegment(tenantId.ifBlank { "global" })
        val stableProductId = productId.trim()
        val isStable = stableProductId.isNotEmpty()
        val safeProductId = if (isStable) StoreImageUpload.sanitizeStoragePathSegment(stableProductId) else ""
        return StoreUploadTarget(
            path = if (isStable) {
                "store/$tenantScope/produtos/$safeProductId"
            } else {
                "store/$tenantScope/produtos/drafts"
            },
            options = StoreImageUploadOptions(
                scopeKey = "store:product:$tenantScope:${stableProductId.ifEmpty { "draft" }}",
                fileName = if (isStable) "produto" else StoreImageUpload.buildDraftAssetFileName("produto", nowMs),
                upsert = isStable,
                versionStrategy = if (isStable) {
                    StoreUploadVersionStrategy.FileMetadata
                } else {
                    StoreUploadVersionStrategy.None
                },
                cacheControl = StoreImageUpload.VersionedPublicAssetCacheControl,
            ),
        )
    }
}

data class StoreUploadTarget(
    val path: String,
    val options: StoreImageUploadOptions,
)

/**
 * Plano de compressão, portado de `web-reference/src/lib/imageCompression.ts` (57-121).
 *
 * O web encolhe em até 8 rodadas de dimensão e, dentro de cada uma, varre a qualidade de 0,82 a
 * 0,45 de 0,08 em 0,08, aceitando o primeiro WEBP que couber em 200KB. A sequência é pura, então
 * fica aqui e o encode do bitmap fica na camada de dados.
 */
object StoreImageCompressionPlan {

    const val DefaultQuality = 0.82
    const val MinQuality = 0.45
    const val QualityStep = 0.08
    const val MinDimension = 320
    const val MaxDimensionAttempts = 8
    const val DimensionShrinkFactor = 0.85

    /** imageCompression.ts 66-68. */
    fun initialSize(sourceWidth: Int, sourceHeight: Int, maxWidth: Int, maxHeight: Int): Pair<Int, Int> {
        if (sourceWidth <= 0 || sourceHeight <= 0) return 1 to 1
        val scale = minOf(
            maxWidth.toDouble() / sourceWidth.toDouble(),
            maxHeight.toDouble() / sourceHeight.toDouble(),
            1.0,
        )
        return maxOf(1, Math.round(sourceWidth * scale).toInt()) to
            maxOf(1, Math.round(sourceHeight * scale).toInt())
    }

    /** imageCompression.ts 86-101: a varredura de qualidade, em percentuais inteiros do Android. */
    fun qualitySteps(): List<Int> {
        val steps = mutableListOf<Int>()
        var quality = DefaultQuality
        while (quality >= MinQuality) {
            steps.add(Math.round(quality * 100.0).toInt())
            // 100: o web arredonda em 2 casas a cada passo para não acumular erro de ponto flutuante.
            quality = Math.round((quality - QualityStep) * 100.0) / 100.0
        }
        return steps
    }

    /** imageCompression.ts 107-112: encolhe 15% por rodada, com piso de 320 em qualquer lado. */
    fun shrink(width: Int, height: Int): Pair<Int, Int>? {
        if (width <= MinDimension || height <= MinDimension) return null
        return maxOf(MinDimension, Math.round(width * DimensionShrinkFactor).toInt()) to
            maxOf(MinDimension, Math.round(height * DimensionShrinkFactor).toInt())
    }

    /** imageCompression.ts 45-52: o arquivo comprimido sai sempre como `.webp`. */
    fun toWebpFileName(name: String): String {
        val withoutExt = name.replace(Regex("\\.[a-z0-9]+$", RegexOption.IGNORE_CASE), "")
            .ifEmpty { "image" }
        val sanitized = withoutExt
            .replace(Regex("[^a-zA-Z0-9_-]+"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
        return "${sanitized.ifEmpty { "image" }}.webp"
    }
}
