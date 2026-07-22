package com.example.usc1.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.data.repository.SupabaseAdminStoreRepository
import com.example.usc1.domain.model.AdminStoreOrder
import com.example.usc1.domain.model.AdminStoreOrderStatus
import com.example.usc1.domain.model.AdminStoreOrdersMode
import com.example.usc1.domain.repository.AdminStoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminStoreOrdersViewModel(
    private val repository: AdminStoreRepository = SupabaseAdminStoreRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminStoreOrdersUiState())
    val uiState: StateFlow<AdminStoreOrdersUiState> = _uiState.asStateFlow()

    fun load(
        mode: AdminStoreOrdersMode,
        categoryLabel: String? = null,
        page: Int = _uiState.value.page,
    ) {
        val safePage = page.coerceAtLeast(1)
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    actionMessage = null,
                    mode = mode,
                    categoryLabel = categoryLabel?.trim().orEmpty(),
                    page = safePage,
                )
            }
            try {
                val result = repository.getOrdersPage(
                    mode = mode,
                    page = safePage,
                    pageSize = PageSize,
                    categoryLabel = categoryLabel,
                )
                if (safePage > 1 && result.rows.isEmpty()) {
                    load(mode = mode, categoryLabel = categoryLabel, page = safePage - 1)
                    return@launch
                }
                _uiState.update { result.toUiState(it) }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erro ao carregar pedidos da loja.",
                    )
                }
            }
        }
    }

    fun previousPage() {
        val state = _uiState.value
        if (state.page <= 1) return
        load(mode = state.mode, categoryLabel = state.categoryLabel, page = state.page - 1)
    }

    fun nextPage() {
        val state = _uiState.value
        if (!state.hasMore) return
        load(mode = state.mode, categoryLabel = state.categoryLabel, page = state.page + 1)
    }

    fun approve(order: AdminStoreOrder, approvedBy: String) {
        mutateOrder(
            order = order,
            successMessage = "Pedido aprovado.",
        ) {
            repository.approveOrder(order.id, approvedBy)
        }
    }

    fun reject(order: AdminStoreOrder) {
        mutateOrder(
            order = order,
            successMessage = "Pedido rejeitado.",
        ) {
            repository.setOrderStatus(order.id, AdminStoreOrderStatus.Rejected)
        }
    }

    fun returnToPending(order: AdminStoreOrder) {
        mutateOrder(
            order = order,
            successMessage = "Pedido voltou para pendente.",
        ) {
            repository.setOrderStatus(order.id, AdminStoreOrderStatus.Pendente)
        }
    }

    fun markDelivered(order: AdminStoreOrder) {
        mutateOrder(
            order = order,
            successMessage = "Pedido marcado como entregue.",
        ) {
            repository.setOrderStatus(order.id, AdminStoreOrderStatus.Delivered)
        }
    }

    fun toggleEditing(order: AdminStoreOrder) {
        _uiState.update {
            it.copy(
                editingOrderId = if (it.editingOrderId == order.id) "" else order.id,
                actionMessage = null,
                errorMessage = null,
            )
        }
    }

    private fun mutateOrder(
        order: AdminStoreOrder,
        successMessage: String,
        action: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    mutatingOrderId = order.id,
                    errorMessage = null,
                    actionMessage = null,
                )
            }
            try {
                action()
                val state = _uiState.value
                _uiState.update {
                    it.copy(
                        mutatingOrderId = null,
                        editingOrderId = "",
                        actionMessage = successMessage,
                    )
                }
                load(mode = state.mode, categoryLabel = state.categoryLabel, page = state.page)
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        mutatingOrderId = null,
                        errorMessage = error.message ?: "Erro ao atualizar pedido.",
                    )
                }
            }
        }
    }

    private companion object {
        const val PageSize = 20
    }
}
