package com.example.usc1.ui.admin

import com.example.usc1.domain.model.AdminStoreProduct
import com.example.usc1.domain.model.AdminStoreProductForm
import com.example.usc1.domain.model.AdminStoreProductStatus
import com.example.usc1.domain.model.AdminStoreProductsPage

data class AdminStoreProductsUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val tenantId: String = "",
    val products: List<AdminStoreProduct> = emptyList(),
    val categoryNames: List<String> = emptyList(),
    val selectedCategory: String = "Geral",
    val inactiveOnly: Boolean = false,
    val isProductOpen: Boolean = false,
    val form: AdminStoreProductForm = AdminStoreProductForm(),
    val mutatingProductId: String = "",
) {
    val title: String
        get() = if (inactiveOnly) "Produtos Desativados" else "Produtos"

    val subtitle: String
        get() = if (inactiveOnly) {
            "histórico completo para reativação sem perder dados"
        } else {
            "criação completa + categorias + variações"
        }

    val listTitle: String
        get() = if (inactiveOnly) "Lista Desativada" else "Lista da Categoria"

    val selectedCategoryLabel: String
        get() = selectedCategory.ifBlank { categoryNames.firstOrNull().orEmpty().ifBlank { "Geral" } }

    val emptyTitle: String
        get() = if (inactiveOnly) "Nenhum produto desativado no momento." else "Nenhum produto encontrado em $selectedCategoryLabel."

    val isEditingProduct: Boolean
        get() = form.productId != null

    val canCreateProduct: Boolean
        get() = !inactiveOnly
}

fun AdminStoreProductsPage.toUiState(
    current: AdminStoreProductsUiState,
): AdminStoreProductsUiState {
    val selected = selectedCategory.ifBlank { categoryNames.firstOrNull().orEmpty().ifBlank { "Geral" } }
    return current.copy(
        isLoading = false,
        errorMessage = null,
        tenantId = tenantId,
        products = products,
        categoryNames = categoryNames.ifEmpty { listOf("Geral") },
        selectedCategory = selected,
        inactiveOnly = inactiveOnly,
        form = if (!current.isProductOpen) {
            AdminStoreProductForm(categoria = selected)
        } else {
            current.form
        },
    )
}

fun AdminStoreProduct.toForm(): AdminStoreProductForm {
    return AdminStoreProductForm(
        productId = id,
        nome = nome,
        categoria = categoria.ifBlank { "Geral" },
        descricao = descricao,
        img = img,
        preco = if (preco == 0.0) "" else preco.toPlainInput(),
        precoAntigo = if (precoAntigo == 0.0) "" else precoAntigo.toPlainInput(),
        status = status,
        estoque = if (estoque == 0) "" else estoque.toString(),
        lote = lote,
        tagLabel = tagLabel,
        tagColor = tagColor.ifBlank { "zinc" },
        tagEffect = tagEffect.ifBlank { "none" },
        coresText = cores,
        caracteristicasText = caracteristicas.joinToString("\n"),
        sellerType = sellerType,
        sellerId = sellerId,
        sellerName = sellerName,
        sellerLogoUrl = sellerLogoUrl,
    )
}

fun AdminStoreProductStatus.next(): AdminStoreProductStatus {
    return when (this) {
        AdminStoreProductStatus.Ativo -> AdminStoreProductStatus.EmBreve
        AdminStoreProductStatus.EmBreve -> AdminStoreProductStatus.Esgotado
        AdminStoreProductStatus.Esgotado -> AdminStoreProductStatus.Ativo
    }
}

private fun Double.toPlainInput(): String {
    return if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        toString()
    }
}
