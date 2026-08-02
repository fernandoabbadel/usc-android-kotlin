package com.example.usc1.ui.admin

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.usc1.domain.model.StoreImageUpload
import com.example.usc1.domain.repository.StoreImageSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Seletor de imagem equivalente ao `<input type="file" accept="image/png,image/jpeg,image/webp">`
 * do web (`produtos/page.tsx` 1272 e 1354).
 *
 * O arquivo é lido para memória com o mesmo teto do web (2MB, `upload.ts` 39-40) antes de chegar
 * ao repositório — arquivo maior é recusado sem gastar Storage nem banda.
 */
@Composable
fun rememberStoreImagePicker(
    onError: (String) -> Unit,
    onPicked: (StoreImageSource) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mimeFilter = remember { StoreImageUpload.AllowedUploadImageTypes.toTypedArray() }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val source = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.readStoreImage(uri) }.getOrNull()
            }
            when {
                source == null -> onError("Não foi possível ler a imagem escolhida.")
                else -> {
                    val validation = StoreImageUpload.validateImageFile(
                        sizeBytes = source.bytes.size.toLong(),
                        mimeType = source.mimeType,
                    )
                    if (validation != null) onError(validation) else onPicked(source)
                }
            }
        }
    }

    return { launcher.launch(mimeFilter) }
}

private fun ContentResolver.readStoreImage(uri: Uri): StoreImageSource? {
    val mimeType = getType(uri)?.trim()?.lowercase().orEmpty()
    var displayName = "imagem"
    // O SAF não expõe `lastModified` de forma confiável; o token de versão do web
    // (`upload.ts` 110-126) combina tamanho e data, então o instante da escolha serve.
    val lastModified = System.currentTimeMillis()

    query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
                    displayName = cursor.getString(nameIndex).orEmpty().ifBlank { displayName }
                }
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    // Recusa cedo, sem ler o arquivo inteiro para a memória.
                    if (cursor.getLong(sizeIndex) > StoreImageUpload.MaxUploadImageBytes) return null
                }
            }
        }

    val bytes = openInputStream(uri)?.use { it.readBytes() } ?: return null
    if (bytes.isEmpty()) return null

    return StoreImageSource(
        bytes = bytes,
        displayName = displayName,
        mimeType = mimeType,
        lastModifiedMs = lastModified,
    )
}
