package com.example.usc1.domain.repository

import com.example.usc1.domain.model.AppModule

interface AppModuleRepository {
    suspend fun getModules(): List<AppModule>
}
