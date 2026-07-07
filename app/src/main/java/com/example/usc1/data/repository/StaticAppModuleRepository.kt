package com.example.usc1.data.repository

import com.example.usc1.domain.model.AppModule
import com.example.usc1.domain.model.AppModules
import com.example.usc1.domain.repository.AppModuleRepository

class StaticAppModuleRepository : AppModuleRepository {
    override suspend fun getModules(): List<AppModule> = AppModules.androidModules
}
