package com.example.usc1.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.usc1.BuildConfig
import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.domain.model.StoreImageCompressionPlan
import com.example.usc1.domain.model.StoreImageUpload
import com.example.usc1.domain.model.StoreImageUploadOptions
import com.example.usc1.domain.model.StoreImageUploadResult
import com.example.usc1.domain.model.StoreUploadGuard
import com.example.usc1.domain.model.StoreUploadVersionStrategy
import com.example.usc1.domain.repository.StoreImageSource
import com.example.usc1.domain.repository.StoreImageUploadRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Porte de `web-reference/src/lib/upload.ts::uploadImage` (292-414) para o Storage do mesmo
 * projeto Supabase, com a sessão autenticada do admin e sem `service_role`.
 *
 * A ordem do web é preservada: valida arquivo, reserva a guarda de custo, valida resolução,
 * comprime, revalida o resultado comprimido, revalida a resolução do candidato, envia, e só então
 * registra o dedupe.
 */
class SupabaseStoreImageUploadRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
    private val guard: StoreUploadGuard = SharedGuard,
    private val bucket: String = BuildConfig.SUPABASE_STORAGE_BUCKET.trim().ifBlank {
        StoreImageUpload.DefaultBucket
    },
    private val nowMs: () -> Long = { System.currentTimeMillis() },
) : StoreImageUploadRepository {

    override suspend fun uploadImage(
        source: StoreImageSource,
        path: String,
        options: StoreImageUploadOptions,
    ): StoreImageUploadResult = withContext(Dispatchers.IO) {
        val safePath = StoreImageUpload.normalizeStoragePath(path).ifBlank { "misc" }
        val scope = options.scopeKey.trim().lowercase().ifBlank { safePath.ifBlank { "uploads" } }

        // upload.ts 301-307
        StoreImageUpload.validateImageFile(
            sizeBytes = source.bytes.size.toLong(),
            mimeType = source.mimeType,
            maxBytes = options.maxBytes,
        )?.let { return@withContext StoreImageUploadResult(error = it) }

        val fingerprint = StoreImageUpload.buildUploadFingerprint(
            name = source.displayName,
            mimeType = source.mimeType,
            sizeBytes = source.bytes.size.toLong(),
            lastModifiedMs = source.lastModifiedMs,
        )

        // upload.ts 309-312
        guard.reserve(scope, fingerprint, nowMs())?.let {
            return@withContext StoreImageUploadResult(error = it)
        }

        try {
            val bounds = decodeBounds(source.bytes)
                ?: return@withContext StoreImageUploadResult(error = "Não foi possível processar a imagem.")

            // upload.ts 315-324
            StoreImageUpload.validateImageDimensions(bounds.first, bounds.second)
                ?.let { return@withContext StoreImageUploadResult(error = it) }

            val compressed = compress(source, options.compressionMaxBytes)
            // upload.ts 335-354: sem o fallback autorizado, o arquivo grande é recusado.
            val optimizedError = StoreImageUpload.validateImageFile(
                sizeBytes = compressed?.bytes?.size?.toLong() ?: source.bytes.size.toLong(),
                mimeType = compressed?.mimeType ?: source.mimeType,
                maxBytes = options.compressionMaxBytes,
            )
            val candidate = when {
                optimizedError == null -> compressed ?: source
                !options.allowOriginalOnCompressionFail ->
                    return@withContext StoreImageUploadResult(error = optimizedError)
                StoreImageUpload.validateImageFile(
                    sizeBytes = source.bytes.size.toLong(),
                    mimeType = source.mimeType,
                    maxBytes = options.maxBytes,
                ) != null -> return@withContext StoreImageUploadResult(error = optimizedError)
                else -> source
            }

            // upload.ts 356-365
            decodeBounds(candidate.bytes)?.let { candidateBounds ->
                StoreImageUpload.validateImageDimensions(candidateBounds.first, candidateBounds.second)
                    ?.let { return@withContext StoreImageUploadResult(error = it) }
            }

            val filename = StoreImageUpload.resolveOutputFileName(
                sourceName = candidate.displayName,
                mimeType = candidate.mimeType,
                fileNameHint = options.fileName,
                nowMs = nowMs(),
            )
            val objectPath = "$safePath/$filename"
            val versionToken = if (options.versionStrategy == StoreUploadVersionStrategy.FileMetadata) {
                StoreImageUpload.buildFileMetadataVersionToken(
                    sizeBytes = candidate.bytes.size.toLong(),
                    lastModifiedMs = candidate.lastModifiedMs,
                )
            } else {
                null
            }

            val storage = clientProvider().storage.from(bucket)
            storage.upload(objectPath, candidate.bytes) {
                upsert = options.upsert
            }

            val publicUrl = storage.publicUrl(objectPath)
            val url = if (options.versionStrategy == StoreUploadVersionStrategy.FileMetadata) {
                StoreImageUpload.appendAssetVersionQuery(publicUrl, versionToken)
            } else {
                publicUrl
            }

            // upload.ts 395-399: dedupe só depois de o Storage aceitar.
            guard.registerSuccess(scope, fingerprint, nowMs())

            StoreImageUploadResult(
                url = url,
                bucket = bucket,
                objectPath = objectPath,
                versionToken = versionToken,
            )
        } catch (error: Throwable) {
            // upload.ts 402-411
            StoreImageUploadResult(error = "Falha ao subir imagem. Tente novamente.")
        } finally {
            guard.release(scope)
        }
    }

    private fun decodeBounds(bytes: ByteArray): Pair<Int, Int>? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return null
        return options.outWidth to options.outHeight
    }

    /**
     * Porte de `imageCompression.ts::compressImageFile` (57-121). O WEBP do Android substitui o
     * `canvas.toBlob("image/webp", q)` do navegador; a sequência de dimensão e qualidade é a
     * mesma, e vem de [StoreImageCompressionPlan].
     */
    private fun compress(source: StoreImageSource, maxBytes: Long): StoreImageSource? {
        val decoded = BitmapFactory.decodeByteArray(source.bytes, 0, source.bytes.size) ?: return null
        return try {
            var (width, height) = StoreImageCompressionPlan.initialSize(
                sourceWidth = decoded.width,
                sourceHeight = decoded.height,
                maxWidth = StoreImageUpload.CompressionMaxWidth,
                maxHeight = StoreImageUpload.CompressionMaxHeight,
            )
            var best: ByteArray? = null

            repeat(StoreImageCompressionPlan.MaxDimensionAttempts) {
                val scaled = Bitmap.createScaledBitmap(decoded, width, height, true)
                try {
                    var smallestThisRound: ByteArray? = null
                    for (quality in StoreImageCompressionPlan.qualitySteps()) {
                        val encoded = encodeWebp(scaled, quality)
                        if (smallestThisRound == null || encoded.size < smallestThisRound!!.size) {
                            smallestThisRound = encoded
                        }
                        if (encoded.size <= maxBytes) {
                            return source.copy(
                                bytes = encoded,
                                displayName = StoreImageCompressionPlan.toWebpFileName(source.displayName),
                                mimeType = WebpMimeType,
                            )
                        }
                    }
                    val roundBest = smallestThisRound
                    if (roundBest != null && (best == null || roundBest.size < best!!.size)) {
                        best = roundBest
                    }
                } finally {
                    if (scaled !== decoded) scaled.recycle()
                }

                val next = StoreImageCompressionPlan.shrink(width, height) ?: return@repeat
                width = next.first
                height = next.second
            }

            best?.let {
                source.copy(
                    bytes = it,
                    displayName = StoreImageCompressionPlan.toWebpFileName(source.displayName),
                    mimeType = WebpMimeType,
                )
            }
        } finally {
            decoded.recycle()
        }
    }

    private fun encodeWebp(bitmap: Bitmap, quality: Int): ByteArray {
        val stream = ByteArrayOutputStream()
        @Suppress("DEPRECATION")
        val format = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            Bitmap.CompressFormat.WEBP
        }
        bitmap.compress(format, quality, stream)
        return stream.toByteArray()
    }

    private companion object {
        const val WebpMimeType = "image/webp"

        /**
         * O web guarda o histórico das guardas em `Map` de módulo (upload.ts 57-59), então o teto
         * de 6 uploads por minuto vale para o app inteiro, não por tela.
         */
        val SharedGuard = StoreUploadGuard()
    }
}
