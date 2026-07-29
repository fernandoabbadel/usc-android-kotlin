package com.example.usc1.domain.repository

import com.example.usc1.ui.album.AlbumUiState

interface AlbumRepository {
    suspend fun getAlbumHub(
        tenantId: String,
        userId: String,
        currentUserClass: String,
    ): AlbumUiState
}
