package com.example.usc1.domain.repository

import com.example.usc1.domain.model.AdminAlbumUiConfig

interface AdminAlbumRepository {
    suspend fun getAlbumUiConfig(forceRefresh: Boolean = false): AdminAlbumUiConfig
    suspend fun saveAlbumUiConfig(config: AdminAlbumUiConfig)
}
