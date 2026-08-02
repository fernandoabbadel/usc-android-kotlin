package com.example.usc1.ui.bi.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.data.repository.SupabaseProductBiRepository
import com.example.usc1.domain.model.ProductBiAnalytics
import com.example.usc1.domain.model.ProductBiDataset
import com.example.usc1.domain.model.ProductBiScope
import com.example.usc1.domain.model.buildProductBiAnalytics
import com.example.usc1.core.session.UserSession
import com.example.usc1.domain.repository.ProductBiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estado do BI Loja. Um estado só para os cinco players; o escopo é campo, não subclasse.
 */
data class ProductBiUiState(
    val scope: ProductBiScope = ProductBiScope.Tenant,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val dataset: ProductBiDataset = ProductBiDataset(),
    /** `productFilter` do web (361). */
    val productFilter: String = ProductBiDataset.AllProducts,
    val analytics: ProductBiAnalytics = ProductBiAnalytics(),
    /** Mini-vendor sem perfil: "Cadastre a lojinha antes de abrir a gestão." do web. */
    val missingProfile: Boolean = false,
) {
    val eyebrow: String get() = scope.eyebrowLabel

    val isEmptyScope: Boolean get() = !isLoading && (missingProfile || dataset.isEmpty)

    val emptyTitle: String
        get() = if (missingProfile) "Lojinha não cadastrada" else "Sem catálogo no recorte"

    val emptySubtitle: String
        get() = if (missingProfile) {
            "Cadastre a lojinha antes de abrir a gestão."
        } else {
            "Nenhum produto ou pedido deste escopo chegou ao recorte."
        }
}

/**
 * Carrega o BI Loja no escopo pedido e roda o motor.
 *
 * Web: os três consumidores de `ProductManagementAnalytics`. Como o motor é um só e o escopo
 * vem do repositório, esta ViewModel também é uma só — não há uma por área.
 */
class ProductBiViewModel(
    private val repository: ProductBiRepository = SupabaseProductBiRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductBiUiState())
    val uiState: StateFlow<ProductBiUiState> = _uiState.asStateFlow()

    fun load(
        session: UserSession,
        scope: ProductBiScope,
        sellerId: String = "",
        labels: ProductBiLabels = ProductBiLabels.of(scope),
    ) {
        _uiState.value = ProductBiUiState(scope = scope, isLoading = true)
        viewModelScope.launch {
            val tenantId = session.tenant?.id.orEmpty()
            val userId = session.user?.id.orEmpty()
            runCatching { repository.getDataset(tenantId, scope, sellerId, userId) }
                .onSuccess { loaded ->
                    // Na lojinha o título é o nome da loja (`profile.storeName`), que só o
                    // repositório conhece; nos outros players o rótulo já veio pronto.
                    val resolved = if (scope == ProductBiScope.MiniVendor) {
                        ProductBiLabels.ofMiniVendor(loaded.sellerName)
                    } else {
                        labels
                    }
                    val dataset = loaded.copy(
                        title = resolved.title,
                        subtitle = resolved.subtitle,
                        allLabel = resolved.allLabel,
                    )
                    _uiState.value = ProductBiUiState(
                        scope = scope,
                        isLoading = false,
                        dataset = dataset,
                        analytics = buildProductBiAnalytics(dataset),
                        missingProfile = !dataset.hasSellerProfile,
                    )
                }
                .onFailure { error ->
                    _uiState.value = ProductBiUiState(
                        scope = scope,
                        isLoading = false,
                        errorMessage = error.message?.takeIf { it.isNotBlank() }
                            ?: "Não foi possível carregar o BI da loja agora.",
                    )
                }
        }
    }

    /** Troca de produto no `<select>`: recalcula o motor sem nova consulta. */
    fun selectProduct(productId: String) {
        val current = _uiState.value
        _uiState.value = current.copy(
            productFilter = productId,
            analytics = buildProductBiAnalytics(current.dataset, productId),
        )
    }
}

/**
 * `title`, `subtitle` e `allLabel` (props 36-38), com o texto exato de cada consumidor do web.
 */
data class ProductBiLabels(
    val title: String,
    val subtitle: String,
    val allLabel: String,
) {
    companion object {
        /** `ProductsBi` do `AdminBiDashboard` (1417-1423). */
        val Tenant = ProductBiLabels(
            title = "Produtos oficiais da loja",
            subtitle = "Receita, compradores únicos, valor médio, conversão por produto, " +
                "estoque, recompra e curva ABC apenas da loja oficial da atlética.",
            allLabel = "Todos os produtos oficiais",
        )

        /** `MiniVendorGestaoPage`: `title` é o nome da loja, resolvido em [ofMiniVendor]. */
        val MiniVendor = ProductBiLabels(
            title = "Minha lojinha",
            subtitle = "Análises privadas da sua lojinha: sem comparar com atlética, ligas ou " +
                "outros vendedores.",
            allLabel = "Todos os produtos da lojinha",
        )

        /**
         * `LeagueFinanceDashboard` com `view="produtos"` (769-777).
         *
         * O subtítulo termina em "apenas desta liga." mesmo na comissão e no diretório: no web
         * ele é literal (774), enquanto `title` e `allLabel` interpolam `entityArticle`/
         * `entityLabel`. Mantido como está — corrigir aqui seria divergir da fonte.
         */
        fun ofCollective(entityArticle: String, entityLabel: String) = ProductBiLabels(
            title = "Produtos $entityArticle $entityLabel",
            subtitle = "Receita, compradores únicos, valor médio, conversão por produto, " +
                "estoque, recompra e curva ABC apenas desta liga.",
            allLabel = "Todos os produtos $entityArticle $entityLabel",
        )

        /** `profile.storeName || "Minha lojinha"` do web. */
        fun ofMiniVendor(storeName: String) =
            MiniVendor.copy(title = storeName.trim().ifBlank { MiniVendor.title })

        fun of(scope: ProductBiScope): ProductBiLabels = when (scope) {
            ProductBiScope.Tenant -> Tenant
            ProductBiScope.MiniVendor -> MiniVendor
            ProductBiScope.League -> ofCollective("da", "liga")
            ProductBiScope.Commission -> ofCollective("da", "comissão")
            ProductBiScope.Directory -> ofCollective("do", "diretório")
        }
    }
}
