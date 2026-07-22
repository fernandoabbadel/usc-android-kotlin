package com.example.usc1.domain.model

data class AdminStoreBundle(
    val tenantId: String,
    val finance: AdminStoreFinanceConfig,
    val menuItems: List<AdminStoreMenuItem> = AdminStoreCatalog.menuItems,
)

data class AdminStoreFinanceConfig(
    val chave: String = "",
    val banco: String = "",
    val titular: String = "",
    val whatsapp: String = "",
)

data class AdminStoreReview(
    val id: String,
    val userName: String,
    val productId: String,
    val comment: String,
    val rating: Int,
    val status: AdminStoreReviewStatus,
)

data class AdminStoreCategoriesBundle(
    val tenantId: String,
    val categories: List<AdminStoreCategory>,
    val defaultButtonColor: String = AdminStoreCatalog.CategoryColorDefault,
)

data class AdminStoreCategory(
    val key: String,
    val categoryId: String?,
    val nome: String,
    val coverImg: String,
    val logoUrl: String,
    val buttonColor: String,
    val displayOrder: Int?,
    val sellerType: AdminStoreSellerType,
    val sellerId: String,
    val derivedOnly: Boolean,
    val categoryVisible: Boolean,
    val productCount: Int,
)

data class AdminStoreCategoryForm(
    val categoryId: String? = null,
    val previousName: String = "",
    val nome: String = "",
    val coverImg: String = "",
    val buttonColor: String = AdminStoreCatalog.CategoryColorDefault,
    val visible: Boolean = true,
)

data class AdminStoreProductsPage(
    val tenantId: String,
    val products: List<AdminStoreProduct>,
    val categoryNames: List<String>,
    val selectedCategory: String,
    val inactiveOnly: Boolean,
)

data class AdminStoreProduct(
    val id: String,
    val nome: String,
    val descricao: String,
    val preco: Double,
    val precoAntigo: Double,
    val img: String,
    val categoria: String,
    val estoque: Int,
    val lote: String,
    val tagLabel: String,
    val tagColor: String,
    val tagEffect: String,
    val active: Boolean,
    val aprovado: Boolean,
    val status: AdminStoreProductStatus,
    val vendidos: Int,
    val cliques: Int,
    val cores: String,
    val caracteristicas: List<String>,
    val variantCount: Int,
    val sellerType: AdminStoreSellerType,
    val sellerId: String,
    val sellerName: String,
    val sellerLogoUrl: String,
)

data class AdminStoreProductForm(
    val productId: String? = null,
    val nome: String = "",
    val categoria: String = "Geral",
    val descricao: String = "",
    val img: String = "",
    val preco: String = "",
    val precoAntigo: String = "",
    val status: AdminStoreProductStatus = AdminStoreProductStatus.Ativo,
    val estoque: String = "",
    val lote: String = "",
    val tagLabel: String = "",
    val tagColor: String = "zinc",
    val tagEffect: String = "none",
    val coresText: String = "",
    val caracteristicasText: String = "",
    val sellerType: AdminStoreSellerType = AdminStoreSellerType.Tenant,
    val sellerId: String = "",
    val sellerName: String = "",
    val sellerLogoUrl: String = "",
)

enum class AdminStoreProductStatus(val remoteValue: String, val label: String) {
    Ativo("ativo", "Ativo"),
    EmBreve("em_breve", "Em-breve"),
    Esgotado("esgotado", "Esgotado");

    companion object {
        fun fromRemote(value: String?): AdminStoreProductStatus {
            return when (value?.trim()?.lowercase()) {
                "em_breve" -> EmBreve
                "esgotado" -> Esgotado
                else -> Ativo
            }
        }
    }
}

enum class AdminStoreSellerType(val remoteValue: String, val label: String) {
    Tenant("tenant", "Tenant"),
    MiniVendor("mini_vendor", "Mini Vendor"),
    League("league", "Liga");

    companion object {
        fun fromRemote(value: String?): AdminStoreSellerType {
            return when (value?.trim()?.lowercase()) {
                "mini_vendor" -> MiniVendor
                "league", "liga" -> League
                else -> Tenant
            }
        }
    }
}

data class AdminStoreOrdersPage(
    val rows: List<AdminStoreOrder>,
    val categoryNames: List<String>,
    val hasMore: Boolean,
    val page: Int,
    val categoryLabel: String,
    val mode: AdminStoreOrdersMode,
)

data class AdminStoreOrder(
    val id: String,
    val userId: String,
    val userName: String,
    val productId: String,
    val productName: String,
    val productCategory: String,
    val price: Double,
    val total: Double,
    val quantidade: Int,
    val itens: Int,
    val status: AdminStoreOrderStatus,
    val approvedBy: String,
    val receiverLabel: String,
    val variantLabel: String,
    val createdAt: String,
    val updatedAt: String,
)

enum class AdminStoreOrdersMode {
    Pending,
    Approved,
}

enum class AdminStoreOrderStatus(val remoteValue: String, val label: String) {
    Pendente("pendente", "Pendente"),
    Approved("approved", "Confirmado"),
    Rejected("rejected", "Rejeitado"),
    Delivered("delivered", "Entregue");

    companion object {
        fun fromRemote(value: String?): AdminStoreOrderStatus {
            return when (value?.trim()?.lowercase()) {
                "approved" -> Approved
                "rejected" -> Rejected
                "delivered" -> Delivered
                else -> Pendente
            }
        }
    }
}

enum class AdminStoreReviewStatus(val remoteValue: String, val label: String) {
    Pending("pending", "Pendente"),
    Approved("approved", "Aprovada"),
    Rejected("rejected", "Rejeitada");

    companion object {
        fun fromRemote(value: String?): AdminStoreReviewStatus {
            return when (value?.trim()?.lowercase()) {
                "approved" -> Approved
                "rejected" -> Rejected
                else -> Pending
            }
        }
    }
}

data class AdminStoreMenuItem(
    val route: String,
    val title: String,
    val description: String,
    val kind: AdminStoreMenuKind,
)

enum class AdminStoreMenuKind {
    Category,
    Products,
    Disabled,
    PendingOrders,
    ApprovedOrders,
    Reviews,
}

object AdminStoreCatalog {
    val menuItems = listOf(
        AdminStoreMenuItem(
            route = "admin/loja/categorias",
            title = "Categorias",
            description = "Página própria para criar, editar e revisar as categorias",
            kind = AdminStoreMenuKind.Category,
        ),
        AdminStoreMenuItem(
            route = "admin/loja/produtos",
            title = "Produtos",
            description = "Catálogo admin com leitura dedicada",
            kind = AdminStoreMenuKind.Products,
        ),
        AdminStoreMenuItem(
            route = "admin/loja/produtos-desativados",
            title = "Desativados",
            description = "Histórico dos produtos fora do ar com reativação segura",
            kind = AdminStoreMenuKind.Disabled,
        ),
        AdminStoreMenuItem(
            route = "admin/loja/pedidos-pendentes",
            title = "Pedidos Pendentes",
            description = "Aprovação separada para evitar bundle pesado",
            kind = AdminStoreMenuKind.PendingOrders,
        ),
        AdminStoreMenuItem(
            route = "admin/loja/pedidos-aprovados",
            title = "Pedidos Aprovados",
            description = "Histórico editável dos comprovantes confirmados",
            kind = AdminStoreMenuKind.ApprovedOrders,
        ),
        AdminStoreMenuItem(
            route = "admin/loja/review",
            title = "Reviews",
            description = "Fila de avaliações moderada por página",
            kind = AdminStoreMenuKind.Reviews,
        ),
    )

    const val PixKeyMaxLength = 180
    const val PixBankMaxLength = 120
    const val PixHolderMaxLength = 180
    const val PhoneMaxLength = 15
    const val CategoryNameMaxLength = 80
    const val CategoryUrlMaxLength = 400
    const val CategoryColorDefault = "#10b981"
    const val MiniVendorColorDefault = "#2563eb"
    const val LeagueColorDefault = "#6366f1"
    const val ProductNameMaxLength = 120
    const val ProductCategoryMaxLength = 80
    const val ProductDescriptionMaxLength = 1200
    const val ProductImageUrlMaxLength = 400
    const val ProductLotMaxLength = 80
    const val ProductBadgeMaxLength = 30
    const val ProductColorsTextMaxLength = 600
    const val ProductFeaturesTextMaxLength = 1200

    fun normalizePhoneInput(value: String): String {
        return value.filter(Char::isDigit).take(PhoneMaxLength)
    }

    fun hasValidPhoneLength(value: String): Boolean {
        val digits = value.filter(Char::isDigit)
        return digits.length in 10..PhoneMaxLength
    }
}
