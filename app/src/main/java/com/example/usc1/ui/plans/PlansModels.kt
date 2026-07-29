package com.example.usc1.ui.plans

import com.example.usc1.R

enum class PlanStatus(val label: String) {
    Active("Ativo"),
    Available("Disponível"),
    Locked("Restrito"),
}

enum class PlanOrderStatus(val label: String) {
    Pending("Pendente"),
    Approved("Aprovado"),
    Cancelled("Cancelado"),
}

data class PlanBenefit(
    val title: String,
    val highlighted: Boolean = false,
)

data class UscPlan(
    val id: String,
    val name: String,
    val subtitle: String,
    val description: String,
    val priceLabel: String,
    val status: PlanStatus,
    val accentName: String,
    val imageRes: Int,
    val benefits: List<PlanBenefit>,
)

data class UserPlanStatus(
    val planName: String,
    val memberSince: String,
    val renewalLabel: String,
    val statusLabel: String,
)

data class PlanOrder(
    val id: String,
    val planName: String,
    val createdAtLabel: String,
    val amountLabel: String,
    val status: PlanOrderStatus,
)

data class PlanUiState(
    val activePlan: UserPlanStatus = PlansMockData.activePlan,
    val plans: List<UscPlan> = PlansMockData.plans,
    val orders: List<PlanOrder> = PlansMockData.orders,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

object PlansMockData {
    val plans = listOf(
        UscPlan(
            id = "cardume-livre",
            name = "Cardume Livre",
            subtitle = "Plano ativo",
            description = "Benefícios essenciais da atlética, carteirinha ativa e descontos no ecossistema USC.",
            priceLabel = "R$ 19,90/mês",
            status = PlanStatus.Active,
            accentName = "Neon",
            imageRes = R.drawable.carteirinha_bg,
            benefits = listOf(
                PlanBenefit("Carteirinha digital", highlighted = true),
                PlanBenefit("Descontos em eventos", highlighted = true),
                PlanBenefit("Parceiros USC"),
                PlanBenefit("Ranking e badges"),
            ),
        ),
        UscPlan(
            id = "atleta",
            name = "Atleta",
            subtitle = "Treinos e frequência",
            description = "Plano para quem participa de treino, check-in, desafios e ranking de presença.",
            priceLabel = "R$ 29,90/mês",
            status = PlanStatus.Available,
            accentName = "Dourado",
            imageRes = R.drawable.battle_forest,
            benefits = listOf(
                PlanBenefit("Check-in gym", highlighted = true),
                PlanBenefit("Frequência oficial"),
                PlanBenefit("Campeonatos internos"),
                PlanBenefit("Benefícios em loja"),
            ),
        ),
        UscPlan(
            id = "lenda",
            name = "Lenda",
            subtitle = "Premium USC",
            description = "Plano premium com prioridade em drops, eventos e benefícios especiais da atlética.",
            priceLabel = "R$ 49,90/mês",
            status = PlanStatus.Available,
            accentName = "Roxo",
            imageRes = R.drawable.logo_platform_web,
            benefits = listOf(
                PlanBenefit("Prioridade em drops", highlighted = true),
                PlanBenefit("Fila premium"),
                PlanBenefit("Badge lendário"),
                PlanBenefit("Cupom mensal"),
            ),
        ),
    )

    val activePlan = UserPlanStatus(
        planName = "Cardume Livre",
        memberSince = "Membro desde 2026",
        renewalLabel = "Renovação conforme assinatura",
        statusLabel = "Ativo e aprovado",
    )

    val orders = listOf(
        PlanOrder(
            id = "PLAN-9102",
            planName = "Cardume Livre",
            createdAtLabel = "Hoje • 10:18",
            amountLabel = "R$ 19,90",
            status = PlanOrderStatus.Pending,
        ),
    )

    fun planById(id: String): UscPlan =
        plans.firstOrNull { it.id == id } ?: plans.first()
}
