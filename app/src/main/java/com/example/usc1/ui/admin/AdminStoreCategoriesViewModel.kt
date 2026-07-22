package com.example.usc1.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.data.repository.SupabaseAdminStoreRepository
import com.example.usc1.domain.model.AdminStoreCatalog
import com.example.usc1.domain.model.AdminStoreCategory
import com.example.usc1.domain.model.AdminStoreCategoryForm
import com.example.usc1.domain.model.AdminStoreSellerType
import com.example.usc1.domain.repository.AdminStoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminStoreCategoriesViewModel(
    private val repository: AdminStoreRepository = SupabaseAdminStoreRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminStoreCategoriesUiState())
    val uiState: StateFlow<AdminStoreCategoriesUiState> = _uiState.asStateFlow()

    fun load(
        tenantLogoUrl: String?,
        defaultButtonColor: String = AdminStoreCatalog.CategoryColorDefault,
        forceRefresh: Boolean = false,
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    tenantLogoUrl = tenantLogoUrl.orEmpty(),
                    defaultButtonColor = defaultButtonColor.ifBlank { AdminStoreCatalog.CategoryColorDefault },
                    errorMessage = null,
                    actionMessage = null,
                )
            }
            try {
                val bundle = repository.getCategories(
                    tenantLogoUrl = tenantLogoUrl,
                    defaultButtonColor = defaultButtonColor,
                    forceRefresh = forceRefresh,
                )
                _uiState.update { bundle.toUiState(it, tenantLogoUrl.orEmpty()) }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erro ao carregar categorias da loja.",
                    )
                }
            }
        }
    }

    fun toggleOrderPanel() {
        _uiState.update { it.copy(isOrderPanelOpen = !it.isOrderPanelOpen) }
    }

    fun startNewCategory() {
        _uiState.update {
            it.copy(
                form = AdminStoreCategoryForm(buttonColor = it.defaultButtonColor),
                actionMessage = null,
                errorMessage = null,
            )
        }
    }

    fun editCategory(category: AdminStoreCategory) {
        if (category.sellerType != AdminStoreSellerType.Tenant) {
            _uiState.update {
                it.copy(
                    actionMessage = if (category.sellerType == AdminStoreSellerType.League) {
                        "A logo e a categoria da liga continuam no cadastro da própria liga."
                    } else {
                        "A logo e a categoria da lojinha continuam no cadastro do mini vendor."
                    },
                    errorMessage = null,
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                form = AdminStoreCategoryForm(
                    categoryId = category.categoryId,
                    previousName = category.nome,
                    nome = category.nome,
                    coverImg = category.coverImg,
                    buttonColor = category.buttonColor.ifBlank { it.defaultButtonColor },
                    visible = category.categoryVisible,
                ),
                actionMessage = null,
                errorMessage = null,
            )
        }
    }

    fun updateName(value: String) {
        updateForm { it.copy(nome = value.take(AdminStoreCatalog.CategoryNameMaxLength)) }
    }

    fun updateCoverUrl(value: String) {
        updateForm { it.copy(coverImg = value.take(AdminStoreCatalog.CategoryUrlMaxLength)) }
    }

    fun updateButtonColor(value: String) {
        val clean = value.take(40)
        updateForm { it.copy(buttonColor = clean) }
    }

    fun saveCategory() {
        val state = _uiState.value
        val form = state.form
        if (form.nome.trim().isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Nome da categoria obrigatório.", actionMessage = null)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, actionMessage = null) }
            try {
                repository.saveCategory(form, state.tenantLogoUrl)
                val bundle = repository.getCategories(
                    tenantLogoUrl = state.tenantLogoUrl,
                    defaultButtonColor = state.defaultButtonColor,
                    forceRefresh = true,
                )
                _uiState.update {
                    bundle.toUiState(
                        current = it.copy(
                            isSaving = false,
                            form = AdminStoreCategoryForm(buttonColor = state.defaultButtonColor),
                            actionMessage = if (form.categoryId != null) {
                                "Categoria atualizada."
                            } else {
                                "Categoria criada."
                            },
                        ),
                        tenantLogoUrl = state.tenantLogoUrl,
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: if (form.categoryId != null) {
                            "Erro ao atualizar categoria."
                        } else {
                            "Erro ao criar categoria."
                        },
                    )
                }
            }
        }
    }

    fun moveCategoryUp(category: AdminStoreCategory) {
        moveCategory(category, -1)
    }

    fun moveCategoryDown(category: AdminStoreCategory) {
        moveCategory(category, 1)
    }

    fun restoreOrder() {
        _uiState.update { it.copy(orderedCategoryKeys = it.defaultOrderedKeys) }
    }

    fun saveOrder() {
        val state = _uiState.value
        val categoryIds = state.orderableCategories.mapNotNull { it.categoryId }
        if (categoryIds.isEmpty()) {
            _uiState.update {
                it.copy(actionMessage = "Nenhuma categoria persistida para ordenar.", errorMessage = null)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingOrder = true, errorMessage = null, actionMessage = null) }
            try {
                repository.saveCategoryDisplayOrder(categoryIds)
                val bundle = repository.getCategories(
                    tenantLogoUrl = state.tenantLogoUrl,
                    defaultButtonColor = state.defaultButtonColor,
                    forceRefresh = true,
                )
                _uiState.update {
                    bundle.toUiState(
                        current = it.copy(
                            isSavingOrder = false,
                            actionMessage = "Ordem das categorias salva.",
                        ),
                        tenantLogoUrl = state.tenantLogoUrl,
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isSavingOrder = false,
                        errorMessage = error.message ?: "Erro ao salvar a ordem das categorias.",
                    )
                }
            }
        }
    }

    fun toggleVisibility(category: AdminStoreCategory) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    visibilityActionKey = category.key,
                    errorMessage = null,
                    actionMessage = null,
                )
            }
            try {
                if (category.sellerType == AdminStoreSellerType.MiniVendor) {
                    repository.setMiniVendorCategoryVisibility(category.sellerId, !category.categoryVisible)
                } else {
                    val categoryId = category.categoryId
                        ?: throw IllegalStateException("Categoria persistida não encontrada.")
                    repository.setCategoryVisibility(categoryId, !category.categoryVisible)
                }
                val state = _uiState.value
                val bundle = repository.getCategories(
                    tenantLogoUrl = state.tenantLogoUrl,
                    defaultButtonColor = state.defaultButtonColor,
                    forceRefresh = true,
                )
                _uiState.update {
                    bundle.toUiState(
                        current = it.copy(
                            visibilityActionKey = "",
                            actionMessage = when {
                                category.sellerType == AdminStoreSellerType.MiniVendor && category.categoryVisible ->
                                    "Categoria do mini vendor ocultada na loja."
                                category.sellerType == AdminStoreSellerType.MiniVendor ->
                                    "Categoria do mini vendor exibida na loja."
                                category.categoryVisible ->
                                    "Categoria do tenant ocultada na loja."
                                else ->
                                    "Categoria do tenant exibida na loja."
                            },
                        ),
                        tenantLogoUrl = state.tenantLogoUrl,
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        visibilityActionKey = "",
                        errorMessage = error.message ?: "Erro ao atualizar visibilidade da categoria.",
                    )
                }
            }
        }
    }

    private fun updateForm(transform: (AdminStoreCategoryForm) -> AdminStoreCategoryForm) {
        _uiState.update {
            it.copy(
                form = transform(it.form),
                actionMessage = null,
                errorMessage = null,
            )
        }
    }

    private fun moveCategory(category: AdminStoreCategory, delta: Int) {
        _uiState.update { state ->
            val keys = state.orderedCategoryKeys.toMutableList()
            val currentIndex = keys.indexOf(category.key)
            if (currentIndex < 0) return@update state
            val nextIndex = (currentIndex + delta).coerceIn(0, keys.lastIndex)
            if (nextIndex == currentIndex) return@update state
            val item = keys.removeAt(currentIndex)
            keys.add(nextIndex, item)
            state.copy(orderedCategoryKeys = keys, actionMessage = null, errorMessage = null)
        }
    }
}
