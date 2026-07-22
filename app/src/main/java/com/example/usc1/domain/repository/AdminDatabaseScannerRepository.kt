package com.example.usc1.domain.repository

import com.example.usc1.domain.model.AdminDatabaseFieldReport

interface AdminDatabaseScannerRepository {
    suspend fun scanDatabaseFields(forceRefresh: Boolean = false): AdminDatabaseFieldReport
}
