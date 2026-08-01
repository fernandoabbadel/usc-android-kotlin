package com.example.usc1.ui.vendor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.session.UserSession
import com.example.usc1.data.repository.SupabaseMiniVendorRepository
import com.example.usc1.domain.repository.MiniVendorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MiniVendorViewModel(
    private val repository: MiniVendorRepository = SupabaseMiniVendorRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(MiniVendorUiState())
    val uiState: StateFlow<MiniVendorUiState> = _uiState.asStateFlow()

    private var lastLoadKey: String = ""
    private var lastSession: UserSession? = null

    fun load(session: UserSession, forceRefresh: Boolean = false) {
        lastSession = session
        val tenantId = session.tenant?.id.orEmpty().trim()
        val userId = session.user?.id.orEmpty().trim()
        val key = "${tenantId.ifBlank { "active" }}::${userId.ifBlank { "auth" }}"
        if (!forceRefresh && key == lastLoadKey && (_uiState.value.hasProfile || _uiState.value.errorMessage != null)) {
            return
        }
        lastLoadKey = key

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    actionMessage = null,
                )
            }
            runCatching {
                repository.getDashboard(tenantId = tenantId, userId = userId)
            }.onSuccess { dashboard ->
                _uiState.value = dashboard.copy(isLoading = false, errorMessage = null)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message.orEmpty().ifBlank {
                            "Não foi possível carregar seu Mini Vendor agora."
                        },
                    )
                }
            }
        }
    }

    fun saveProfile(form: MiniVendorProfileForm) {
        val session = lastSession ?: return
        val tenantId = session.tenant?.id.orEmpty().trim()
        val userId = session.user?.id.orEmpty().trim()
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSavingProfile = true,
                    errorMessage = null,
                    actionMessage = null,
                )
            }
            runCatching {
                repository.saveProfile(
                    tenantId = tenantId,
                    userId = userId,
                    form = form,
                )
            }.onSuccess { dashboard ->
                lastLoadKey = ""
                _uiState.value = dashboard.copy(
                    isLoading = false,
                    isSavingProfile = false,
                    errorMessage = null,
                )
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSavingProfile = false,
                        errorMessage = error.message.orEmpty().ifBlank {
                            "Não foi possível salvar seu Mini Vendor agora."
                        },
                    )
                }
            }
        }
    }

    fun saveProduct(form: MiniVendorProductForm) {
        val session = lastSession ?: return
        val tenantId = session.tenant?.id.orEmpty().trim()
        val userId = session.user?.id.orEmpty().trim()
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSavingProfile = true,
                    errorMessage = null,
                    actionMessage = null,
                )
            }
            runCatching {
                repository.saveProduct(
                    tenantId = tenantId,
                    userId = userId,
                    form = form,
                )
            }.onSuccess { dashboard ->
                lastLoadKey = ""
                _uiState.value = dashboard.copy(
                    isLoading = false,
                    isSavingProfile = false,
                    errorMessage = null,
                )
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSavingProfile = false,
                        errorMessage = error.message.orEmpty().ifBlank {
                            "Não foi possível salvar o produto agora."
                        },
                    )
                }
            }
        }
    }

    fun setProductActive(product: MiniVendorProduct, active: Boolean) {
        val session = lastSession ?: return
        val tenantId = session.tenant?.id.orEmpty().trim()
        val userId = session.user?.id.orEmpty().trim()
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSavingProfile = true,
                    errorMessage = null,
                    actionMessage = null,
                )
            }
            runCatching {
                repository.setProductActive(
                    tenantId = tenantId,
                    userId = userId,
                    productId = product.id,
                    active = active,
                )
            }.onSuccess { dashboard ->
                lastLoadKey = ""
                _uiState.value = dashboard.copy(
                    isLoading = false,
                    isSavingProfile = false,
                    errorMessage = null,
                )
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSavingProfile = false,
                        errorMessage = error.message.orEmpty().ifBlank {
                            "Não foi possível alterar o produto agora."
                        },
                    )
                }
            }
        }
    }

    fun setOrderStatus(order: MiniVendorOrder, status: MiniVendorOrderStatus) {
        val session = lastSession ?: return
        val tenantId = session.tenant?.id.orEmpty().trim()
        val userId = session.user?.id.orEmpty().trim()
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSavingProfile = true,
                    errorMessage = null,
                    actionMessage = null,
                )
            }
            runCatching {
                repository.setOrderStatus(
                    tenantId = tenantId,
                    userId = userId,
                    orderId = order.id,
                    status = status,
                    approvedBy = userId,
                )
            }.onSuccess { dashboard ->
                lastLoadKey = ""
                _uiState.value = dashboard.copy(
                    isLoading = false,
                    isSavingProfile = false,
                    errorMessage = null,
                )
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSavingProfile = false,
                        errorMessage = error.message.orEmpty().ifBlank {
                            "Não foi possível atualizar o pedido agora."
                        },
                    )
                }
            }
        }
    }
}
