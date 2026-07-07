package com.example.usc1.core.roles

enum class UserRole(val remoteValue: String) {
    Guest("guest"),
    Visitante("visitante"),
    User("user"),
    MiniVendor("mini_vendor"),
    Treinador("treinador"),
    Empresa("empresa"),
    AdminTreino("admin_treino"),
    GestorLiga("gestor_liga"),
    GestorDiretorio("gestor_diretorio"),
    GestorComissao("gestor_comissao"),
    AdminGeral("admin_geral"),
    AdminGestor("admin_gestor"),
    MasterTenant("master_tenant"),
    Master("master"),
    Vendas("vendas");

    val isAdminLike: Boolean
        get() = this in adminPanelRoles

    val canManageTenant: Boolean
        get() = this in tenantManagerRoles || this == Master

    companion object {
        val adminPanelRoles = setOf(
            Master,
            MasterTenant,
            AdminGeral,
            AdminGestor,
            AdminTreino,
            Treinador,
            GestorLiga,
            GestorDiretorio,
            GestorComissao,
        )

        val tenantManagerRoles = setOf(
            MasterTenant,
            AdminGeral,
            AdminGestor,
            GestorLiga,
            GestorDiretorio,
            GestorComissao,
        )

        fun fromRemote(value: String?): UserRole {
            val normalized = value?.trim()?.lowercase().orEmpty()
            return entries.firstOrNull { it.remoteValue == normalized }
                ?: when (normalized) {
                    "admin_tenant" -> AdminGeral
                    "league_manager" -> GestorLiga
                    "directory_manager" -> GestorDiretorio
                    "commission_manager" -> GestorComissao
                    else -> Visitante
                }
        }
    }
}
