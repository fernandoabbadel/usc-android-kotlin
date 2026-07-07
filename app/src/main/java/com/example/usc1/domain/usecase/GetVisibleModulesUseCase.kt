package com.example.usc1.domain.usecase

import com.example.usc1.core.permissions.PermissionPolicy
import com.example.usc1.core.session.UserSession
import com.example.usc1.domain.model.AppModule
import com.example.usc1.domain.repository.AppModuleRepository

class GetVisibleModulesUseCase(
    private val appModuleRepository: AppModuleRepository,
    private val permissionPolicy: PermissionPolicy = PermissionPolicy(),
) {
    suspend operator fun invoke(
        session: UserSession,
        visibleTenantModuleKeys: Set<String> = emptySet(),
    ): List<AppModule> {
        return appModuleRepository.getModules()
            .filter { module ->
                permissionPolicy.canOpenModule(
                    session = session,
                    module = module,
                    visibleTenantModuleKeys = visibleTenantModuleKeys,
                ).allowed
            }
    }
}
