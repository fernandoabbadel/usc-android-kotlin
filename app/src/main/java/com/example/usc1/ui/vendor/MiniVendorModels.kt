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
    val imageRes: Int,
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
    val storeName: String = "Mini Vendor USC",
    val statusLabel: String = "Aguardando aprovação mockada",
    val totalRevenueLabel: String = "R$ 1.284,70",
    val pendingAmountLabel: String = "R$ 342,90",
    val products: List<MiniVendorProduct> = MiniVendorMockData.products,
    val pendingOrders: List<MiniVendorOrder> = MiniVendorMockData.orders.filter { it.status == MiniVendorOrderStatus.Pending },
    val approvedOrders: List<MiniVendorOrder> = MiniVendorMockData.orders.filter { it.status == MiniVendorOrderStatus.Approved },
)

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
            imageRes = R.drawable.logo_aaakn,
        ),
    )

    val orders = listOf(
        MiniVendorOrder("MV-901", "Fernando USC", "Kit Pós-Evento", "R$ 39,90", "Hoje • 15:02", MiniVendorOrderStatus.Pending),
        MiniVendorOrder("MV-899", "Ana Costa", "Ficha Bebida", "R$ 24,00", "Hoje • 14:41", MiniVendorOrderStatus.Pending),
        MiniVendorOrder("MV-812", "Lívia Martins", "Copo USC", "R$ 18,00", "Ontem • 21:10", MiniVendorOrderStatus.Approved),
        MiniVendorOrder("MV-780", "Lucas T9", "Kit Pós-Evento", "R$ 39,90", "30 JUN • 19:33", MiniVendorOrderStatus.Approved),
    )
}
