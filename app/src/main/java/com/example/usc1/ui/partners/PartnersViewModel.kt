package com.example.usc1.ui.partners

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.data.repository.SupabasePartnersRepository
import com.example.usc1.domain.repository.PartnersRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PartnersViewModel(
    private val repository: PartnersRepository = SupabasePartnersRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(PartnerUiState())
    val uiState: StateFlow<PartnerUiState> = _uiState.asStateFlow()

    private val _detailState = MutableStateFlow(PartnerDetailUiState())
    val detailState: StateFlow<PartnerDetailUiState> = _detailState.asStateFlow()

    fun load(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val partners = repository.getPublicPartners(forceRefresh)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                        partners = partners,
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erro ao carregar parceiros.",
                        partners = emptyList(),
                    )
                }
            }
        }
    }

    fun loadPartner(partnerId: String, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _detailState.update { it.copy(isLoading = true, errorMessage = null, partner = null) }
            try {
                val partner = repository.getPartnerById(partnerId, forceRefresh)
                _detailState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = if (partner == null) "Parceiro não encontrado neste tenant." else null,
                        partner = partner,
                    )
                }
            } catch (error: Throwable) {
                _detailState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erro ao carregar parceiro.",
                    )
                }
            }
        }
    }
}
