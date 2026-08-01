package com.example.usc1.ui.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.session.UserSession
import com.example.usc1.data.repository.SupabaseAlbumRepository
import com.example.usc1.domain.repository.AlbumRepository
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AlbumViewModel(
    private val repository: AlbumRepository = SupabaseAlbumRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(AlbumUiState())
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()
    private var lastLoadedKey: String? = null
    private var loadJob: Job? = null

    fun load(session: UserSession, forceRefresh: Boolean = false) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        val userId = session.user?.id.orEmpty().trim()
        val userName = session.user?.name.orEmpty().trim()
        val userClass = session.user?.classCode.orEmpty().trim()
        val userAvatar = session.user?.avatarUrl.orEmpty().trim()
        val turmaSlug = userClass.toAlbumTurmaSlug()
        val qrPayload = buildAlbumIdentityQrPayload(
            userId = userId,
            tenantId = tenantId,
            userName = userName,
            userTurma = userClass,
            userAvatar = userAvatar,
        )
        val key = "$tenantId:$userId:$userClass"

        if (tenantId.isBlank()) {
            loadJob?.cancel()
            lastLoadedKey = null
            _uiState.update {
                it.copy(
                    currentTurmaSlug = turmaSlug,
                    myQrPayload = qrPayload,
                    canUseQr = userId.isNotBlank(),
                    isLoading = false,
                    errorMessage = "Selecione uma atlética para carregar o álbum.",
                )
            }
            return
        }
        if (!forceRefresh && lastLoadedKey == key && _uiState.value.errorMessage == null) return

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    currentTurmaSlug = turmaSlug,
                    myQrPayload = qrPayload,
                    canUseQr = userId.isNotBlank(),
                    isLoading = true,
                    errorMessage = null,
                )
            }
            try {
                val next = repository.getAlbumHub(
                    tenantId = tenantId,
                    userId = userId,
                    currentUserClass = userClass,
                )
                lastLoadedKey = key
                _uiState.value = next.copy(
                    currentTurmaSlug = turmaSlug,
                    myQrPayload = qrPayload,
                    canUseQr = userId.isNotBlank(),
                    isLoading = false,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                lastLoadedKey = null
                _uiState.update {
                    it.copy(
                        currentTurmaSlug = turmaSlug,
                        myQrPayload = qrPayload,
                        canUseQr = userId.isNotBlank(),
                        isLoading = false,
                        errorMessage = error.message ?: "Não foi possível carregar o álbum.",
                    )
                }
            }
        }
    }

    fun findTurma(id: String): AlbumTurma? {
        val clean = id.trim()
        return _uiState.value.turmas.firstOrNull {
            it.id.equals(clean, ignoreCase = true) || it.slug.equals(clean, ignoreCase = true)
        }
    }

    private fun String.toAlbumTurmaSlug(): String {
        val normalized = trim().lowercase(Locale.ROOT)
        val digits = normalized.filter(Char::isDigit)
        return when {
            normalized.isBlank() -> "t8"
            digits.isNotBlank() -> "t$digits"
            else -> normalized.replace(Regex("[^a-z0-9_-]"), "").ifBlank { "t8" }
        }
    }

    private fun buildAlbumIdentityQrPayload(
        userId: String,
        tenantId: String,
        userName: String,
        userTurma: String,
        userAvatar: String,
    ): String {
        if (userId.isBlank()) return ""
        return buildJsonObject {
            put("t", "usuario")
            put("v", 1)
            put("uid", userId)
            put("ten", tenantId)
            put("n", userName)
            put("tu", userTurma)
            put("av", userAvatar)
        }.toString()
    }
}
