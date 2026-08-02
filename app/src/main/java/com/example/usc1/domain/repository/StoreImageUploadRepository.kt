package com.example.usc1.domain.repository

import com.example.usc1.domain.model.StoreImageUploadOptions
import com.example.usc1.domain.model.StoreImageUploadResult

/**
 * Arquivo escolhido pelo usuário, já lido para memória.
 *
 * O teto de 2MB do web (`upload.ts` 39-40) é validado antes do envio, então segurar os bytes é
 * barato e mantém o repositório sem `Context`.
 */
data class StoreImageSource(
    val bytes: ByteArray,
    val displayName: String,
    val mimeType: String,
    val lastModifiedMs: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StoreImageSource) return false
        return displayName == other.displayName &&
            mimeType == other.mimeType &&
            lastModifiedMs == other.lastModifiedMs &&
            bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + lastModifiedMs.hashCode()
        return result
    }
}

/**
 * Upload de imagem da loja no mesmo bucket do web.
 *
 * Fonte: `web-reference/src/lib/upload.ts` — `uploadImage` (292-414).
 */
interface StoreImageUploadRepository {
    /**
     * @param path caminho do objeto **antes** da normalização; o repositório aplica
     *   `normalizeStoragePath` como o web faz em 297.
     */
    suspend fun uploadImage(
        source: StoreImageSource,
        path: String,
        options: StoreImageUploadOptions,
    ): StoreImageUploadResult
}
