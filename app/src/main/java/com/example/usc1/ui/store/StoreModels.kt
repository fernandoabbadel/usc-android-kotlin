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
    val priceValue: Double = 0.0,
    val priceLabel: String,
    val status: StoreProductStatus,
    val badge: String,
    val tagColor: String? = null,
    val tagEffect: String? = null,
    @param:DrawableRes val imageRes: Int,
    val imageUrl: String? = null,
    val oldPriceLabel: String? = null,
    val colors: List<String> = emptyList(),
    val variants: List<String> = emptyList(),
    val characteristics: List<String> = emptyList(),
    val likesCount: Int = 0,
    val soldCount: Int = 0,
    val clicksCount: Int = 0,
    val stockLabel: String,
    val reviewLabel: String,
    val pixKey: String = "",
    val pixBank: String = "",
    val pixHolder: String = "",
    val receiptWhatsapp: String = "",
    val receiptName: String = "",
    val sellerType: StoreSellerType = StoreSellerType.Tenant,
    val sellerId: String = "",
    val sellerName: String = "",
    val sellerLogoUrl: String? = null,
)

data class StoreCategory(
    val id: String,
    val name: String,
    val sellerType: StoreSellerType,
    val sellerId: String,
    val sellerName: String,
    val sellerLogoUrl: String? = null,
    val coverImageUrl: String? = null,
    val buttonColor: String? = null,
    val isReceivingOrders: Boolean = true,
)

data class CartItem(
    val product: StoreProduct,
    val quantity: Int,
    val variantLabel: String = "",
    val colorLabel: String = "",
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
    val sellerName: String = "",
    val sellerTypeLabel: String = "",
)

data class StoreUiState(
    val isLoading: Boolean = false,
    val selectedCategory: String = "Todos",
    val categories: List<String> = listOf("Todos"),
    val categoryCards: List<StoreCategory> = emptyList(),
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
    val isSubmitting: Boolean = false,
    val checkoutError: String? = null,
)

data class StoreOrdersUiState(
    val selectedStatus: StoreOrderStatus? = null,
    val orders: List<StoreOrder> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val filteredOrders: List<StoreOrder>
        get() = selectedStatus?.let { status -> orders.filter { it.status == status } } ?: orders
}
