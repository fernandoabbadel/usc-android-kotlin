package com.example.usc1.ui.admin

import com.example.usc1.domain.model.AdminStoreOrder
import com.example.usc1.domain.model.AdminStoreOrdersMode
import com.example.usc1.domain.model.AdminStoreOrdersPage

data class AdminStoreOrdersUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val mode: AdminStoreOrdersMode = AdminStoreOrdersMode.Pending,
    val categoryLabel: String = "",
    val page: Int = 1,
    val hasMore: Boolean = false,
    val rows: List<AdminStoreOrder> = emptyList(),
    val categoryNames: List<String> = emptyList(),
    val mutatingOrderId: String? = null,
    val editingOrderId: String = "",
) {
    val title: String
        get() {
            val base = when (mode) {
                AdminStoreOrdersMode.Pending -> "Pedidos Pendentes"
                AdminStoreOrdersMode.Approved -> "Pedidos Aprovados"
            }
            return if (categoryLabel.isBlank()) base else "$base • $categoryLabel"
        }

    val subtitle: String
        get() {
            return if (categoryLabel.isNotBlank()) {
                "Mostra somente os comprovantes da categoria $categoryLabel."
            } else {
                when (mode) {
                    AdminStoreOrdersMode.Pending -> "Aprove ou rejeite comprovantes sem carregar o histórico já confirmado."
                    AdminStoreOrdersMode.Approved -> "Histórico dos comprovantes confirmados com edição rápida de status."
                }
            }
        }

    val emptyText: String
        get() = when (mode) {
            AdminStoreOrdersMode.Pending -> "Sem pedidos pendentes."
            AdminStoreOrdersMode.Approved -> "Nenhum pedido aprovado ainda."
        }
}

fun AdminStoreOrdersPage.toUiState(current: AdminStoreOrdersUiState): AdminStoreOrdersUiState {
    return current.copy(
        isLoading = false,
        errorMessage = null,
        rows = rows,
        categoryNames = categoryNames,
        hasMore = hasMore,
        page = page,
        categoryLabel = categoryLabel,
        mode = mode,
    )
}
