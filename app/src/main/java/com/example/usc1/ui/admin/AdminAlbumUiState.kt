package com.example.usc1.ui.admin

import com.example.usc1.domain.model.AdminAlbumUiConfig

data class AdminAlbumUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val config: AdminAlbumUiConfig = AdminAlbumUiConfig(),
)
