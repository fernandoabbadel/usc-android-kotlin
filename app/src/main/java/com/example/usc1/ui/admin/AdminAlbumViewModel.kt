package com.example.usc1.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.data.repository.SupabaseAdminAlbumRepository
import com.example.usc1.domain.model.AdminAlbumUiConfig
import com.example.usc1.domain.repository.AdminAlbumRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminAlbumViewModel(
    private val repository: AdminAlbumRepository = SupabaseAdminAlbumRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminAlbumUiState())
    val uiState: StateFlow<AdminAlbumUiState> = _uiState.asStateFlow()

    fun load(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, actionMessage = null) }
            try {
                val config = repository.getAlbumUiConfig(forceRefresh)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        config = config,
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erro ao carregar capa do álbum.",
                    )
                }
            }
        }
    }

    fun updateConfig(transform: (AdminAlbumUiConfig) -> AdminAlbumUiConfig) {
        _uiState.update { it.copy(config = transform(it.config), actionMessage = null, errorMessage = null) }
    }

    fun save() {
        viewModelScope.launch {
            val config = _uiState.value.config
            _uiState.update { it.copy(isSaving = true, errorMessage = null, actionMessage = null) }
            try {
                repository.saveAlbumUiConfig(config)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        actionMessage = "Capa e textos da /album atualizados.",
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Erro ao salvar configuração da capa.",
                    )
                }
            }
        }
    }
}
