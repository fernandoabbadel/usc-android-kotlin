package com.example.usc1.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.data.repository.SupabaseAdminStoreRepository
import com.example.usc1.domain.model.AdminStoreReview
import com.example.usc1.domain.model.AdminStoreReviewStatus
import com.example.usc1.domain.repository.AdminStoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminStoreReviewsViewModel(
    private val repository: AdminStoreRepository = SupabaseAdminStoreRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminStoreReviewsUiState())
    val uiState: StateFlow<AdminStoreReviewsUiState> = _uiState.asStateFlow()

    init {
        load(forceRefresh = true)
    }

    fun load(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, actionMessage = null) }
            try {
                val reviews = repository.getReviews(limit = 300, forceRefresh = forceRefresh)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        reviews = reviews,
                        page = 1,
                        errorMessage = null,
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erro ao carregar reviews.",
                    )
                }
            }
        }
    }

    fun approve(review: AdminStoreReview) {
        updateStatus(review, AdminStoreReviewStatus.Approved)
    }

    fun reject(review: AdminStoreReview) {
        updateStatus(review, AdminStoreReviewStatus.Rejected)
    }

    fun previousPage() {
        _uiState.update { it.copy(page = (it.page - 1).coerceAtLeast(1)) }
    }

    fun nextPage() {
        _uiState.update { it.copy(page = (it.page + 1).coerceAtMost(it.totalPages)) }
    }

    private fun updateStatus(
        review: AdminStoreReview,
        status: AdminStoreReviewStatus,
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    mutatingReviewId = review.id,
                    errorMessage = null,
                    actionMessage = null,
                )
            }
            try {
                repository.setReviewStatus(review.id, status)
                _uiState.update { state ->
                    state.copy(
                        mutatingReviewId = null,
                        reviews = state.reviews.map {
                            if (it.id == review.id) it.copy(status = status) else it
                        },
                        actionMessage = if (status == AdminStoreReviewStatus.Approved) {
                            "Review aprovada."
                        } else {
                            "Review rejeitada."
                        },
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        mutatingReviewId = null,
                        errorMessage = error.message ?: "Erro ao atualizar review.",
                    )
                }
            }
        }
    }
}
