package com.example.usc1.ui.store

import androidx.annotation.DrawableRes
import com.example.usc1.domain.model.StoreSellerType

enum class StoreProductStatus(val label: String) {
    Available("Disponível"),
    SoldOut("Esgotado"),
    ComingSoon("Em breve"),
}

enum class StoreOrderStatus(val label: String) {
    Pending("Pendente"),
    Approved("Aprovado"),
    Cancelled("Cancelado"),
}

enum class StorePaymentStatus(val label: String) {
    WaitingPayment("Aguardando pagamento"),
    Paid("Pagamento aprovado"),
    Cancelled("Pagamento cancelado"),
}

data class StoreProduct(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val priceLabel: String,
    val status: StoreProductStatus,
    val badge: String,
    @DrawableRes val imageRes: Int,
    val imageUrl: String? = null,
    val stockLabel: String,
    val reviewLabel: String,
    val sellerType: StoreSellerType = StoreSellerType.Tenant,
    val sellerId: String = "",
    val sellerName: String = "",
)

data class CartItem(
    val product: StoreProduct,
    val quantity: Int,
)

data class StoreOrder(
    val id: String,
    val title: String,
    val createdAtLabel: String,
    val status: StoreOrderStatus,
    val paymentStatus: StorePaymentStatus,
    val amountLabel: String,
    val items: List<CartItem>,
    val pickupLabel: String,
)

data class StoreUiState(
    val isLoading: Boolean = false,
    val selectedCategory: String = "Todos",
    val categories: List<String> = listOf("Todos"),
    val products: List<StoreProduct> = emptyList(),
    val cartCount: Int = 0,
    val hasMore: Boolean = false,
    val page: Int = 1,
    val activeTenantId: String? = null,
    val errorMessage: String? = null,
)

data class CartUiState(
    val items: List<CartItem> = emptyList(),
    val subtotalLabel: String = "R$ 0,00",
    val serviceLabel: String = "R$ 0,00",
    val totalLabel: String = "R$ 0,00",
)

data class StoreOrdersUiState(
    val selectedStatus: StoreOrderStatus? = null,
    val orders: List<StoreOrder> = emptyList(),
)
