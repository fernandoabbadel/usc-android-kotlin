package com.example.usc1.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.data.repository.SupabaseAdminStoreRepository
import com.example.usc1.data.repository.SupabaseStoreImageUploadRepository
import com.example.usc1.domain.model.AdminStoreCatalog
import com.example.usc1.domain.model.AdminStoreProduct
import com.example.usc1.domain.model.AdminStoreProductForm
import com.example.usc1.domain.model.AdminStoreProductStatus
import com.example.usc1.domain.model.StorePaymentRecipient
import com.example.usc1.domain.model.StoreProductPlanScope
import com.example.usc1.domain.model.StoreUploadTargets
import com.example.usc1.domain.repository.AdminStoreRepository
import com.example.usc1.domain.repository.StoreImageSource
import com.example.usc1.domain.repository.StoreImageUploadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminStoreProductsViewModel(
    private val repository: AdminStoreRepository = SupabaseAdminStoreRepository(),
    private val uploadRepository: StoreImageUploadRepository = SupabaseStoreImageUploadRepository(),
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
                // `produtos/page.tsx` 338-365: só os recebedores já salvos carregam com a tela.
                // O diretório de membros é preguiçoso, como no web.
                val recipients = repository.getProductPaymentRecipients(forceRefresh)
                _uiState.update { page.toUiState(it.copy(paymentRecipients = recipients)) }
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
                form = it.emptyForm(),
                actionMessage = null,
                errorMessage = null,
            )
        }
    }

    fun closeProductForm() {
        _uiState.update {
            it.copy(
                isProductOpen = false,
                isPlanModalOpen = false,
                showReceiversManager = false,
                form = it.emptyForm(),
                actionMessage = null,
                errorMessage = null,
            )
        }
    }

    fun editProduct(product: AdminStoreProduct) {
        _uiState.update {
            it.copy(
                isProductOpen = true,
                form = product.toForm(it.planCatalog),
                actionMessage = null,
                errorMessage = null,
            )
        }
    }

    /** Recusa do seletor (tipo ou tamanho), antes de qualquer chamada ao Storage. */
    fun showUploadError(message: String) {
        _uiState.update { it.copy(errorMessage = message, actionMessage = null) }
    }

    fun setPlanModalOpen(open: Boolean) {
        _uiState.update { it.copy(isPlanModalOpen = open) }
    }

    /**
     * `produtos/page.tsx` 1835: o diretório de membros só é consultado quando o gerenciador abre,
     * e uma vez por sessão de tela.
     */
    fun setReceiversManagerOpen(open: Boolean) {
        _uiState.update { it.copy(showReceiversManager = open) }
        if (!open || _uiState.value.recipientDirectory.isNotEmpty()) return
        viewModelScope.launch {
            try {
                val directory = repository.getRecipientDirectory()
                _uiState.update { it.copy(recipientDirectory = directory) }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "Erro ao carregar o diretório de membros.")
                }
            }
        }
    }

    /** `produtos/page.tsx` 1612: em branco, o plano usa o preço geral do produto. */
    fun updatePlanPrice(planId: String, planName: String, value: String) {
        val key = StoreProductPlanScope.planKey(planId, planName)
        updateForm { form ->
            form.copy(
                planScopeRows = form.planScopeRows.map { row ->
                    if (StoreProductPlanScope.planKey(row.planId, row.planName) == key) {
                        row.copy(price = value.filterMoneyInput())
                    } else {
                        row
                    }
                },
            )
        }
    }

    fun updatePlanVisibility(planId: String, planName: String, visible: Boolean) {
        val key = StoreProductPlanScope.planKey(planId, planName)
        updateForm { form ->
            form.copy(
                planScopeRows = form.planScopeRows.map { row ->
                    if (StoreProductPlanScope.planKey(row.planId, row.planName) == key) {
                        row.copy(visible = visible)
                    } else {
                        row
                    }
                },
            )
        }
    }

    fun togglePaymentRecipient(userId: String) {
        val clean = userId.trim()
        if (clean.isEmpty()) return
        updateForm { form ->
            val current = form.paymentRecipientUserIds
            form.copy(
                paymentRecipientUserIds = if (current.contains(clean)) {
                    current - clean
                } else {
                    current + clean
                },
            )
        }
    }

    /** paymentRecipients.ts 212-255: salva o documento de recebedores do escopo "produtos". */
    fun saveRecipientDirectorySelection(recipients: List<StorePaymentRecipient>) {
        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null, actionMessage = null) }
            try {
                val saved = repository.saveProductPaymentRecipients(recipients)
                _uiState.update {
                    it.copy(
                        paymentRecipients = saved,
                        actionMessage = "Recebedores atualizados.",
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "Erro ao salvar recebedores.")
                }
            }
        }
    }

    /**
     * `produtos/page.tsx` 764-799. O caminho e as opções saem de [StoreUploadTargets]; a URL
     * pública devolvida vai para o campo `img` do formulário, como o `setForm` do web.
     */
    fun uploadProductImage(source: StoreImageSource) {
        val state = _uiState.value
        if (state.isUploadingProductImage) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(isUploadingProductImage = true, errorMessage = null, actionMessage = null)
            }
            val target = StoreUploadTargets.productImage(
                tenantId = state.tenantId,
                productId = state.form.productId.orEmpty(),
                nowMs = System.currentTimeMillis(),
            )
            val result = uploadRepository.uploadImage(source, target.path, target.options)
            _uiState.update { current ->
                val url = result.url
                if (url.isNullOrBlank()) {
                    current.copy(
                        isUploadingProductImage = false,
                        errorMessage = result.error ?: "Erro ao subir imagem do produto.",
                    )
                } else {
                    current.copy(
                        isUploadingProductImage = false,
                        form = current.form.copy(img = url),
                        actionMessage = "Imagem do produto enviada.",
                    )
                }
            }
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

    fun updateUseVariants(value: Boolean) {
        updateForm { it.copy(usarVariantes = value) }
    }

    fun updateVariantsText(value: String) {
        updateForm { it.copy(variantesText = value.take(AdminStoreCatalog.ProductVariantsTextMaxLength)) }
    }

    fun updatePaymentPixKey(value: String) {
        updateForm { it.copy(paymentPixKey = value.take(AdminStoreCatalog.PixKeyMaxLength)) }
    }

    fun updatePaymentBank(value: String) {
        updateForm { it.copy(paymentBank = value.take(AdminStoreCatalog.PixBankMaxLength)) }
    }

    fun updatePaymentHolder(value: String) {
        updateForm { it.copy(paymentHolder = value.take(AdminStoreCatalog.PixHolderMaxLength)) }
    }

    fun updatePaymentWhatsapp(value: String) {
        updateForm { it.copy(paymentWhatsapp = AdminStoreCatalog.normalizePhoneInput(value)) }
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
