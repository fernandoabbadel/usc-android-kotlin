package com.example.usc1.domain.model

data class AdminUsersPage(
    val users: List<AdminUserListItem>,
    val nextCursor: String?,
    val hasMore: Boolean,
    val tenantId: String,
)

data class AdminUserListItem(
    val id: String,
    val nome: String,
    val email: String,
    val telefone: String,
    val turma: String,
    val matricula: String,
    val status: AdminUserStatus,
    val plano: AdminUserPlan,
    val foto: String,
    val xp: Long,
    val role: String,
    val tenantId: String,
    val isTurmaLeader: Boolean,
)

data class AdminUserProfile(
    val id: String,
    val nome: String,
    val email: String,
    val foto: String,
    val matricula: String,
    val turma: String,
    val telefone: String,
    val status: AdminUserStatus,
    val level: Long,
    val xp: Long,
    val sharkCoins: Long,
    val planoBadge: String,
    val tier: AdminUserPlan,
    val patente: String,
    val role: String,
)

data class AdminUserUpdate(
    val userId: String,
    val nome: String,
    val telefone: String,
    val matricula: String,
    val turma: String,
    val status: AdminUserStatus,
    val plano: AdminUserPlan,
)

data class AdminUserRoleUpdate(
    val targetUserId: String,
    val role: String,
    val actorUserId: String,
    val actorName: String,
)

data class AdminUserTurmaLeaderUpdate(
    val targetUserId: String,
    val enabled: Boolean,
)

enum class AdminPermissionRole(val remoteValue: String, val label: String) {
    MasterTenant("master_tenant", "Master Tenant"),
    AdminGeral("admin_geral", "Admin Geral"),
    AdminGestor("admin_gestor", "Gestor"),
    AdminTreino("admin_treino", "Adm Treino"),
    Vendas("vendas", "Vendas"),
    Treinador("treinador", "Coach"),
    Empresa("empresa", "Empresa"),
    MiniVendor("mini_vendor", "Mini Vendor"),
    User("user", "Membro"),
    Visitante("visitante", "Visitante");

    companion object {
        fun normalize(value: String?): String {
            val normalized = value?.trim()?.lowercase().orEmpty()
            return when {
                normalized.isBlank() || normalized == "guest" -> Visitante.remoteValue
                entries.any { it.remoteValue == normalized } -> normalized
                else -> User.remoteValue
            }
        }

        fun labelFor(value: String?): String {
            val normalized = normalize(value)
            return entries.firstOrNull { it.remoteValue == normalized }?.label ?: normalized.uppercase()
        }

        fun requiresAdminLegalAcceptance(value: String): Boolean {
            return normalize(value) !in setOf("", "guest", "visitante", "user")
        }
    }
}

enum class AdminUserStatus(val remoteValue: String, val label: String) {
    Ativo("ativo", "Ativo"),
    Inadimplente("inadimplente", "Inadimplente"),
    Pendente("pendente", "Pendente"),
    Bloqueado("bloqueado", "Bloqueado");

    companion object {
        fun fromRemote(value: String?): AdminUserStatus {
            return entries.firstOrNull { it.remoteValue == value?.trim()?.lowercase() } ?: Pendente
        }
    }
}

enum class AdminUserPlan(val remoteValue: String, val label: String) {
    Todos("todos", "Todos"),
    Lenda("lenda", "Lenda"),
    Atleta("atleta", "Atleta"),
    Cardume("cardume", "Cardume"),
    Bicho("bicho", "Bicho");

    companion object {
        fun fromRemote(value: String?): AdminUserPlan {
            return when (value?.trim()?.lowercase()) {
                "lenda" -> Lenda
                "atleta" -> Atleta
                "cardume" -> Cardume
                else -> Bicho
            }
        }
    }
}

data class AdminUsersFilters(
    val search: String = "",
    val plan: AdminUserPlan = AdminUserPlan.Todos,
    val letterGroup: AdminUsersLetterGroup = AdminUsersLetterGroup.Todos,
)

enum class AdminUsersLetterGroup(
    val remoteValue: String,
    val label: String,
    val letters: List<String>,
) {
    Todos("todos", "Todos", emptyList()),
    AF("a-f", "A-F", listOf("A", "B", "C", "D", "E", "F")),
    GK("g-k", "G-K", listOf("G", "H", "I", "J", "K")),
    LQ("l-q", "L-Q", listOf("L", "M", "N", "O", "P", "Q")),
    RZ("r-z", "R-Z", listOf("R", "S", "T", "U", "V", "W", "X", "Y", "Z")),
}
