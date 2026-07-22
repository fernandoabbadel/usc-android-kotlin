package com.example.usc1.ui.admin

import com.example.usc1.domain.model.AdminMiniVendor
import com.example.usc1.domain.model.AdminMiniVendorDirectoryMode
import com.example.usc1.domain.model.AdminMiniVendorStatus

data class AdminMiniVendorsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val mode: AdminMiniVendorDirectoryMode = AdminMiniVendorDirectoryMode.Approvals,
    val rows: List<AdminMiniVendor> = emptyList(),
    val mutatingId: String = "",
) {
    val visibleRows: List<AdminMiniVendor>
        get() = if (mode == AdminMiniVendorDirectoryMode.Approvals) {
            rows.filter { it.status == AdminMiniVendorStatus.Pending }
        } else {
            rows
        }

    val title: String
        get() = when (mode) {
            AdminMiniVendorDirectoryMode.Approvals -> "Pendentes de Aprovacao"
            AdminMiniVendorDirectoryMode.Vendors -> "Todos os Mini Vendors"
        }

    val subtitle: String
        get() = when (mode) {
            AdminMiniVendorDirectoryMode.Approvals -> "Cadastros aguardando revisão do admin da atlética."
            AdminMiniVendorDirectoryMode.Vendors -> "Visao completa das lojinhas cadastradas no tenant."
        }

    val emptyText: String
        get() = when (mode) {
            AdminMiniVendorDirectoryMode.Approvals -> "Nenhum cadastro pendente."
            AdminMiniVendorDirectoryMode.Vendors -> "Nenhuma lojinha cadastrada ainda."
        }
}
