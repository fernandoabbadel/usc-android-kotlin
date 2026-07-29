package com.example.usc1.ui.vendor

import com.example.usc1.R

enum class MiniVendorOrderStatus(val label: String) {
    Pending("Pendente"),
    Approved("Aprovado"),
}

data class MiniVendorProduct(
    val id: String,
    val name: String,
    val priceLabel: String,
    val stockLabel: String,
    val status: String,
    val imageUrl: String? = null,
    val imageRes: Int = R.drawable.logo_usc_wide,
)

data class MiniVendorOrder(
    val id: String,
    val customerName: String,
    val productName: String,
    val amountLabel: String,
    val createdAtLabel: String,
    val status: MiniVendorOrderStatus,
)

data class MiniVendorUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val profileId: String = "",
    val storeName: String = "Mini Vendor",
    val statusLabel: String = "Carregando dados reais",
    val description: String = "",
    val logoUrl: String? = null,
    val coverUrl: String? = null,
    val totalRevenueLabel: String = "R$ 0,00",
    val pendingAmountLabel: String = "R$ 0,00",
    val products: List<MiniVendorProduct> = emptyList(),
    val pendingOrders: List<MiniVendorOrder> = emptyList(),
    val approvedOrders: List<MiniVendorOrder> = emptyList(),
) {
    val hasProfile: Boolean
        get() = profileId.isNotBlank()

    val ordersCount: Int
        get() = pendingOrders.size + approvedOrders.size
}

object MiniVendorMockData {
    val products = listOf(
        MiniVendorProduct(
            id = "mv-kit-01",
            name = "Kit Pós-Evento",
            priceLabel = "R$ 39,90",
            stockLabel = "24 disponíveis",
            status = "Publicado",
            imageRes = R.drawable.battle_forest,
        ),
        MiniVendorProduct(
            id = "mv-ficha-02",
            name = "Ficha Bebida",
            priceLabel = "R$ 12,00",
            stockLabel = "148 disponíveis",
            status = "Evento",
            imageRes = R.drawable.logo_usc_wide,
        ),
        MiniVendorProduct(
            id = "mv-copo-03",
            name = "Copo USC",
            priceLabel = "R$ 18,00",
            stockLabel = "Esgotando",
            status = "Destaque",
            imageRes = R.drawable.logo_platform_web,
        ),
    )

    val orders = listOf(
        MiniVendorOrder("MV-901", "Fernando USC", "Kit Pós-Evento", "R$ 39,90", "Hoje • 15:02", MiniVendorOrderStatus.Pending),
        MiniVendorOrder("MV-899", "Ana Costa", "Ficha Bebida", "R$ 24,00", "Hoje • 14:41", MiniVendorOrderStatus.Pending),
        MiniVendorOrder("MV-812", "Lívia Martins", "Copo USC", "R$ 18,00", "Ontem • 21:10", MiniVendorOrderStatus.Approved),
        MiniVendorOrder("MV-780", "Lucas T9", "Kit Pós-Evento", "R$ 39,90", "30 JUN • 19:33", MiniVendorOrderStatus.Approved),
    )

    val previewState = MiniVendorUiState(
        profileId = "preview-mini-vendor",
        storeName = "Mini Vendor USC",
        statusLabel = "Aprovado para vender",
        description = "Produtos, pedidos e financeiro da lojinha do aluno.",
        totalRevenueLabel = "R$ 1.284,70",
        pendingAmountLabel = "R$ 342,90",
        products = products,
        pendingOrders = orders.filter { it.status == MiniVendorOrderStatus.Pending },
        approvedOrders = orders.filter { it.status == MiniVendorOrderStatus.Approved },
    )
}
