package com.example.usc1.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.data.repository.SupabaseAdminStoreRepository
import com.example.usc1.domain.model.AdminStoreCatalog
import com.example.usc1.domain.model.AdminStoreProduct
import com.example.usc1.domain.model.AdminStoreProductForm
import com.example.usc1.domain.model.AdminStoreProductStatus
import com.example.usc1.domain.repository.AdminStoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminStoreProductsViewModel(
    private val repository: AdminStoreRepository = SupabaseAdminStoreRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminStoreProductsUiState())
    val uiState: StateFlow<AdminStoreProductsUiState> = _uiState.asStateFlow()

    fun load(
        categoryLabel: String? = null,
        inactiveOnly: Boolean = false,
        forceRefresh: Boolean = false,
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    inactiveOnly = inactiveOnly,
                    errorMessage = null,
                    actionMessage = null,
                )
            }
            try {
                val page = repository.getProductsPage(
                    categoryLabel = categoryLabel,
                    inactiveOnly = inactiveOnly,
                    forceRefresh = forceRefresh,
                )
                _uiState.update { page.toUiState(it) }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erro ao carregar produtos da loja.",
                    )
                }
            }
        }
    }

    fun selectCategory(category: String) {
        val clean = category.trim().ifBlank { "Geral" }
        load(categoryLabel = clean, inactiveOnly = false, forceRefresh = false)
    }

    fun openCreateProduct() {
        _uiState.update {
            it.copy(
                isProductOpen = true,
                form = AdminStoreProductForm(categoria = it.selectedCategoryLabel),
                actionMessage = null,
                errorMessage = null,
            )
        }
    }

    fun closeProductForm() {
        _uiState.update {
            it.copy(
                isProductOpen = false,
                form = AdminStoreProductForm(categoria = it.selectedCategoryLabel),
                actionMessage = null,
                errorMessage = null,
            )
        }
    }

    fun editProduct(product: AdminStoreProduct) {
        _uiState.update {
            it.copy(
                isProductOpen = true,
                form = product.toForm(),
                actionMessage = null,
                errorMessage = null,
            )
        }
    }

    fun updateName(value: String) {
        updateForm { it.copy(nome = value.take(AdminStoreCatalog.ProductNameMaxLength)) }
    }

    fun updateCategory(value: String) {
        updateForm { it.copy(categoria = value.take(AdminStoreCatalog.ProductCategoryMaxLength)) }
    }

    fun updateDescription(value: String) {
        updateForm { it.copy(descricao = value.take(AdminStoreCatalog.ProductDescriptionMaxLength)) }
    }

    fun updateImage(value: String) {
        updateForm { it.copy(img = value.take(AdminStoreCatalog.ProductImageUrlMaxLength)) }
    }

    fun updatePrice(value: String) {
        updateForm { it.copy(preco = value.filterMoneyInput()) }
    }

    fun updateOldPrice(value: String) {
        updateForm { it.copy(precoAntigo = value.filterMoneyInput()) }
    }

    fun updateStatus(status: AdminStoreProductStatus) {
        updateForm { it.copy(status = status) }
    }

    fun cycleStatus() {
        updateForm { it.copy(status = it.status.next()) }
    }

    fun updateStock(value: String) {
        updateForm { it.copy(estoque = value.filter(Char::isDigit)) }
    }

    fun updateLot(value: String) {
        updateForm { it.copy(lote = value.take(AdminStoreCatalog.ProductLotMaxLength)) }
    }

    fun updateTagLabel(value: String) {
        updateForm { it.copy(tagLabel = value.take(AdminStoreCatalog.ProductBadgeMaxLength)) }
    }

    fun updateTagColor(value: String) {
        updateForm { it.copy(tagColor = value.take(30)) }
    }

    fun updateTagEffect(value: String) {
        updateForm { it.copy(tagEffect = value.take(30)) }
    }

    fun updateColorsText(value: String) {
        updateForm { it.copy(coresText = value.take(AdminStoreCatalog.ProductColorsTextMaxLength)) }
    }

    fun updateFeaturesText(value: String) {
        updateForm { it.copy(caracteristicasText = value.take(AdminStoreCatalog.ProductFeaturesTextMaxLength)) }
    }

    fun saveProduct(
        tenantName: String?,
        tenantLogoUrl: String?,
    ) {
        val state = _uiState.value
        if (state.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, actionMessage = null) }
            try {
                repository.saveProduct(state.form, tenantName, tenantLogoUrl)
                val nextCategory = state.form.categoria.trim().ifBlank { state.selectedCategoryLabel }
                val page = repository.getProductsPage(
                    categoryLabel = if (state.inactiveOnly) null else nextCategory,
                    inactiveOnly = state.inactiveOnly,
                    forceRefresh = true,
                )
                _uiState.update {
                    page.toUiState(
                        it.copy(
                            isSaving = false,
                            isProductOpen = false,
                            actionMessage = if (state.form.productId == null) {
                                "Produto criado com sucesso."
                            } else {
                                "Produto atualizado com sucesso."
                            },
                        ),
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: if (state.form.productId == null) {
                            "Erro ao criar produto."
                        } else {
                            "Erro ao atualizar produto."
                        },
                    )
                }
            }
        }
    }

    fun toggleProductActive(product: AdminStoreProduct) {
        val nextActive = !product.active
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    mutatingProductId = product.id,
                    errorMessage = null,
                    actionMessage = null,
                )
            }
            try {
                repository.setProductActive(product.id, nextActive)
                val state = _uiState.value
                val page = repository.getProductsPage(
                    categoryLabel = if (state.inactiveOnly) null else state.selectedCategoryLabel,
                    inactiveOnly = state.inactiveOnly,
                    forceRefresh = true,
                )
                _uiState.update {
                    page.toUiState(
                        it.copy(
                            mutatingProductId = "",
                            actionMessage = if (nextActive) "Produto ativado." else "Produto desativado.",
                        ),
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        mutatingProductId = "",
                        errorMessage = error.message ?: "Erro ao atualizar status do produto.",
                    )
                }
            }
        }
    }

    private fun updateForm(transform: (AdminStoreProductForm) -> AdminStoreProductForm) {
        _uiState.update {
            it.copy(
                form = transform(it.form),
                actionMessage = null,
                errorMessage = null,
            )
        }
    }
}

private fun String.filterMoneyInput(): String {
    return filter { char -> char.isDigit() || char == ',' || char == '.' }
}
