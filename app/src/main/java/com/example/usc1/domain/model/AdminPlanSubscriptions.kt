package com.example.usc1.domain.model

data class AdminPlanSubscription(
    val id: String,
    val aluno: String,
    val turma: String,
    val foto: String,
    val planoId: String,
    val planoNome: String,
    val valorPago: Double,
    val dataInicio: String,
    val status: AdminPlanSubscriptionStatus,
    val metodo: AdminPlanSubscriptionMethod,
    val userId: String,
)

data class AdminPlanRequest(
    val id: String,
    val userId: String,
    val userName: String,
    val userTurma: String,
    val planoId: String,
    val planoNome: String,
    val valor: Double,
    val comprovanteUrl: String,
    val dataSolicitacao: String,
    val status: AdminPlanRequestStatus,
    val metodo: String,
)

enum class AdminPlanRequestStatus(val remoteValue: String, val label: String) {
    Pendente("pendente", "Pendente"),
    Aprovado("aprovado", "Aprovado"),
    Rejeitado("rejeitado", "Rejeitado");

    companion object {
        fun fromRemote(value: String?): AdminPlanRequestStatus {
            return when (value?.trim()?.lowercase()) {
                "aprovado" -> Aprovado
                "rejeitado" -> Rejeitado
                else -> Pendente
            }
        }
    }
}

enum class AdminPlanSubscriptionStatus(val remoteValue: String, val label: String) {
    Ativo("ativo", "Ativo"),
    Vencido("vencido", "Vencido"),
    Pendente("pendente", "Pendente");

    companion object {
        fun fromRemote(value: String?): AdminPlanSubscriptionStatus {
            return when (value?.trim()?.lowercase()) {
                "vencido" -> Vencido
                "pendente" -> Pendente
                else -> Ativo
            }
        }
    }
}

enum class AdminPlanSubscriptionMethod(val remoteValue: String, val label: String) {
    Pix("pix", "PIX"),
    Cartao("cartao", "Cartão");

    companion object {
        fun fromRemote(value: String?): AdminPlanSubscriptionMethod {
            return if (value?.trim()?.lowercase() == "cartao") Cartao else Pix
        }
    }
}

enum class AdminPlanSubscriptionListKind(
    val title: String,
    val keyword: String,
    val source: String,
) {
    BichoSolto(
        title = "Lista Bicho Solto",
        keyword = "bicho",
        source = "web-reference/src/app/admin/planos/lista_bicho_solto/page.tsx",
    ),
    CardumeLivre(
        title = "Lista Cardume Livre",
        keyword = "cardume",
        source = "web-reference/src/app/admin/planos/lista_cardume_livre/page.tsx",
    ),
    Atleta(
        title = "Lista Atleta",
        keyword = "atleta",
        source = "web-reference/src/app/admin/planos/lista_atleta/page.tsx",
    ),
    Lenda(
        title = "Lista Lenda",
        keyword = "lenda",
        source = "web-reference/src/app/admin/planos/lista_lenda/page.tsx",
    );

    fun matches(row: AdminPlanSubscription): Boolean {
        val key = "${row.planoId} ${row.planoNome}".lowercase()
        return keyword in key
    }
}
