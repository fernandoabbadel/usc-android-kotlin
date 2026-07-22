package com.example.usc1.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.data.repository.SupabaseAdminStoreRepository
import com.example.usc1.domain.model.AdminStoreCatalog
import com.example.usc1.domain.model.AdminStoreFinanceConfig
import com.example.usc1.domain.repository.AdminStoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminStoreViewModel(
    private val repository: AdminStoreRepository = SupabaseAdminStoreRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminStoreUiState())
    val uiState: StateFlow<AdminStoreUiState> = _uiState.asStateFlow()

    init {
        load(forceRefresh = true)
    }

    fun load(forceRefresh: Boolean = true) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, saveMessage = null) }
            try {
                _uiState.value = repository.getStoreBundle(forceRefresh).toUiState()
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erro ao carregar dados do PIX da loja.",
                    )
                }
            }
        }
    }

    fun updatePixKey(value: String) {
        updateFinance { it.copy(chave = value.take(AdminStoreCatalog.PixKeyMaxLength)) }
    }

    fun updateBank(value: String) {
        updateFinance { it.copy(banco = value.take(AdminStoreCatalog.PixBankMaxLength)) }
    }

    fun updateHolder(value: String) {
        updateFinance { it.copy(titular = value.take(AdminStoreCatalog.PixHolderMaxLength)) }
    }

    fun updateWhatsapp(value: String) {
        updateFinance { it.copy(whatsapp = AdminStoreCatalog.normalizePhoneInput(value)) }
    }

    fun saveFinance() {
        val current = _uiState.value
        val finance = current.finance
        if (current.isSavingFinance) return
        if (finance.chave.trim().isBlank() || finance.banco.trim().isBlank() || finance.titular.trim().isBlank()) {
            _uiState.update { it.copy(saveMessage = "Preencha chave PIX, banco e titular.") }
            return
        }
        if (finance.whatsapp.isNotBlank() && !AdminStoreCatalog.hasValidPhoneLength(finance.whatsapp)) {
            _uiState.update { it.copy(saveMessage = "Informe um WhatsApp valido com DDI e somente numeros.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSavingFinance = true,
                    errorMessage = null,
                    saveMessage = null,
                )
            }
            try {
                repository.saveFinanceConfig(finance)
                _uiState.update {
                    it.copy(
                        isSavingFinance = false,
                        saveMessage = "Dados financeiros da loja atualizados.",
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isSavingFinance = false,
                        errorMessage = error.message ?: "Erro ao salvar dados do PIX da loja.",
                    )
                }
            }
        }
    }

    private fun updateFinance(transform: (AdminStoreFinanceConfig) -> AdminStoreFinanceConfig) {
        _uiState.update {
            it.copy(
                finance = transform(it.finance),
                saveMessage = null,
            )
        }
    }
}
