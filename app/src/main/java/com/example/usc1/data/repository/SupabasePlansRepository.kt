package com.example.usc1.data.repository

import com.example.usc1.R
import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.domain.repository.PlansRepository
import com.example.usc1.ui.plans.PlanBenefit
import com.example.usc1.ui.plans.PlanOrder
import com.example.usc1.ui.plans.PlanOrderStatus
import com.example.usc1.ui.plans.PlanStatus
import com.example.usc1.ui.plans.PlanUiState
import com.example.usc1.ui.plans.UscPlan
import com.example.usc1.ui.plans.UserPlanStatus
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.text.NumberFormat
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SupabasePlansRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : PlansRepository {
    override suspend fun getPlansHub(
        tenantId: String,
        userId: String,
        userPlanName: String,
        userPlanStatus: String,
    ): PlanUiState = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanUserId = userId.trim()
        if (!SupabaseClientProvider.config.isConfigured || cleanTenantId.isBlank()) {
            return@withContext PlanUiState(
                plans = emptyList(),
                orders = emptyList(),
                activePlan = inactivePlan(userPlanName, userPlanStatus),
                errorMessage = "Supabase não configurado para carregar os planos.",
            )
        }

        val client = clientProvider()
        val subscriptions = if (cleanUserId.isBlank()) {
            emptyList()
        } else {
            fetchUserSubscriptions(client, cleanTenantId, cleanUserId)
        }
        val activeSubscription = subscriptions.firstOrNull { it.status.normalizedStatus() == "ativo" }
            ?: subscriptions.firstOrNull()
        val activePlanId = activeSubscription?.planoId.orEmpty()
        val activePlanName = firstNotBlank(activeSubscription?.planoNome, userPlanName)

        val plans = fetchPlans(client, cleanTenantId)
            .map { row -> row.toUiPlan(activePlanId, activePlanName) }

        PlanUiState(
            activePlan = activeSubscription?.toActiveStatus(userPlanName, userPlanStatus)
                ?: inactivePlan(userPlanName, userPlanStatus),
            plans = plans,
            orders = subscriptions.map { it.toOrder() },
            isLoading = false,
            errorMessage = null,
        )
    }

    private suspend fun fetchPlans(
        client: SupabaseClient,
        tenantId: String,
    ): List<PublicPlanCatalogRow> {
        return client.from(PlansTable)
            .select(columns = Columns.raw(PlanColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                }
                order(column = "precoVal", order = Order.ASCENDING)
                limit(count = MaxPlans.toLong())
            }
            .decodeList<PublicPlanCatalogRow>()
    }

    private suspend fun fetchUserSubscriptions(
        client: SupabaseClient,
        tenantId: String,
        userId: String,
    ): List<PublicPlanSubscriptionRow> {
        return client.from(SubscriptionsTable)
            .select(columns = Columns.raw(SubscriptionColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("userId", userId)
                }
                order(column = "createdAt", order = Order.DESCENDING)
                limit(count = MaxSubscriptions.toLong())
            }
            .decodeList<PublicPlanSubscriptionRow>()
    }

    private fun PublicPlanCatalogRow.toUiPlan(
        activePlanId: String,
        activePlanName: String,
    ): UscPlan {
        val cleanId = id.trim()
        val cleanName = nome.trim().ifBlank { "Plano" }
        val corKey = firstNotBlank(cor, icon, cleanName).lowercase(Locale.ROOT).normalizeAscii()
        val status = when {
            cleanId.equals(activePlanId, ignoreCase = true) -> PlanStatus.Active
            cleanName.equals(activePlanName, ignoreCase = true) -> PlanStatus.Active
            cleanName.lowercase(Locale.ROOT).normalizeAscii().contains("bicho") -> PlanStatus.Locked
            precoVal <= 0.0 && preco.lowercase(Locale.ROOT).contains("consulta") -> PlanStatus.Locked
            else -> PlanStatus.Available
        }
        val benefitRows = beneficios
            .map(String::trim)
            .filter(String::isNotBlank)
            .take(8)
            .ifEmpty { listOf("Carteirinha digital", "Benefícios da atlética", "Descontos no ecossistema") }
        return UscPlan(
            id = cleanId.ifBlank { cleanName.slugify() },
            name = cleanName,
            subtitle = when (status) {
                PlanStatus.Active -> "Plano ativo"
                PlanStatus.Locked -> "Acesso por convite"
                PlanStatus.Available -> parcelamento?.trim().orEmpty().ifBlank { "Disponível" }
            },
            description = descricao.orEmpty().trim().ifBlank {
                "Plano oficial da atlética com benefícios, carteirinha e vantagens exclusivas."
            },
            priceLabel = priceLabel(preco, precoVal, parcelamento),
            status = status,
            accentName = accentLabel(corKey),
            imageRes = imageFor(corKey, cleanName),
            benefits = benefitRows.mapIndexed { index, item ->
                PlanBenefit(
                    title = item,
                    highlighted = index < 2 ||
                        item.contains("carteirinha", ignoreCase = true) ||
                        item.contains("desconto", ignoreCase = true),
                )
            },
        )
    }

    private fun PublicPlanSubscriptionRow.toActiveStatus(
        fallbackPlanName: String,
        fallbackStatus: String,
    ): UserPlanStatus {
        val cleanStatus = statusLabel(firstNotBlank(status, fallbackStatus))
        return UserPlanStatus(
            planName = firstNotBlank(planoNome, fallbackPlanName, "Plano ativo"),
            memberSince = dataInicio?.trim()?.takeIf(String::isNotBlank)?.let { "Membro desde $it" }
                ?: createdAt?.formatDateLabel("Membro desde")
                ?: "Membro da atlética",
            renewalLabel = if (metodo.isNotBlank()) {
                "Método: ${metodo.uppercase(Locale.ROOT)}"
            } else {
                "Renovação gerenciada pela atlética"
            },
            statusLabel = cleanStatus,
        )
    }

    private fun PublicPlanSubscriptionRow.toOrder(): PlanOrder {
        return PlanOrder(
            id = id.trim().ifBlank { "ASSINATURA" },
            planName = planoNome.trim().ifBlank { "Plano" },
            createdAtLabel = firstNotBlank(dataInicio, createdAt).formatShortDate(),
            amountLabel = formatCurrency(valorPago),
            status = when (status.normalizedStatus()) {
                "ativo", "aprovado", "approved" -> PlanOrderStatus.Approved
                "cancelado", "cancelled", "vencido", "rejeitado" -> PlanOrderStatus.Cancelled
                else -> PlanOrderStatus.Pending
            },
        )
    }

    private fun inactivePlan(
        userPlanName: String,
        userPlanStatus: String,
    ): UserPlanStatus {
        val planName = userPlanName.trim().ifBlank { "Sem plano ativo" }
        return UserPlanStatus(
            planName = planName,
            memberSince = "Adesão ainda não localizada",
            renewalLabel = "Escolha um plano para solicitar adesão",
            statusLabel = statusLabel(userPlanStatus).ifBlank { "Pendente" },
        )
    }

    private fun priceLabel(preco: String, precoVal: Double, parcelamento: String?): String {
        val cleanPrice = preco.trim()
        val base = when {
            cleanPrice.startsWith("R$", ignoreCase = true) -> cleanPrice
            cleanPrice.isNotBlank() -> "R$ $cleanPrice"
            precoVal > 0.0 -> formatCurrency(precoVal)
            else -> "Sob consulta"
        }
        val cleanInstallment = parcelamento?.trim().orEmpty()
        return if (cleanInstallment.isBlank()) base else "$base • $cleanInstallment"
    }

    private fun statusLabel(value: String): String {
        return when (value.normalizedStatus()) {
            "ativo", "aprovado", "approved", "active" -> "Ativo"
            "pendente", "pending" -> "Pendente"
            "cancelado", "cancelled" -> "Cancelado"
            "vencido" -> "Vencido"
            "rejeitado", "rejected" -> "Rejeitado"
            else -> value.trim().replaceFirstChar { it.titlecase(Locale.forLanguageTag("pt-BR")) }
        }
    }

    private fun String.normalizedStatus(): String = trim().lowercase(Locale.ROOT).normalizeAscii()

    private fun imageFor(corKey: String, name: String): Int {
        val haystack = "$corKey $name".lowercase(Locale.ROOT).normalizeAscii()
        return when {
            haystack.contains("roxo") || haystack.contains("lenda") -> R.drawable.logo_platform_web
            haystack.contains("dour") || haystack.contains("atleta") -> R.drawable.battle_forest
            haystack.contains("fogo") || haystack.contains("vermelh") || haystack.contains("bicho") -> R.drawable.logo_usc
            else -> R.drawable.carteirinha_bg
        }
    }

    private fun accentLabel(corKey: String): String {
        return when {
            corKey.contains("roxo") || corKey.contains("purple") -> "Roxo"
            corKey.contains("dour") || corKey.contains("gold") -> "Dourado"
            corKey.contains("fogo") || corKey.contains("red") -> "Fogo"
            corKey.contains("emerald") || corKey.contains("esmeralda") || corKey.contains("verde") -> "Esmeralda"
            else -> "Neon"
        }
    }

    private fun String.formatShortDate(): String {
        val clean = trim()
        if (clean.isBlank()) return "Sem data"
        parseInstant(clean)?.let { instant ->
            return ShortDateFormatter.format(instant.atZone(Zone).toLocalDate()).uppercase(Locale.forLanguageTag("pt-BR"))
        }
        return clean.take(16)
    }

    private fun String.formatDateLabel(prefix: String): String? {
        val clean = trim()
        if (clean.isBlank()) return null
        parseInstant(clean)?.let { instant ->
            return "$prefix ${YearFormatter.format(instant.atZone(Zone).toLocalDate())}"
        }
        return "$prefix $clean"
    }

    private fun parseInstant(value: String): Instant? {
        return runCatching { OffsetDateTime.parse(value).toInstant() }
            .getOrElse { runCatching { Instant.parse(value) }.getOrNull() }
    }

    private fun formatCurrency(value: Double): String = CurrencyFormatter.format(value)

    private fun firstNotBlank(vararg values: String?): String {
        return values.firstNotNullOfOrNull { it?.trim()?.takeIf(String::isNotBlank) }.orEmpty()
    }

    private fun String.slugify(): String {
        return lowercase(Locale.ROOT)
            .normalizeAscii()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "plano" }
    }

    private fun String.normalizeAscii(): String {
        val normalized = java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{Mn}+"), "")
    }

    private companion object {
        const val PlansTable = "planos"
        const val SubscriptionsTable = "assinaturas"
        const val MaxPlans = 24
        const val MaxSubscriptions = 30
        const val PlanColumns = "id,nome,preco,precoVal,parcelamento,descricao,cor,icon,destaque,beneficios,xpMultiplier,nivelPrioridade,descontoLoja,tenant_id"
        const val SubscriptionColumns = "id,aluno,turma,foto,planoId,planoNome,valorPago,dataInicio,status,metodo,userId,createdAt,tenant_id"
        val PtBr: Locale = Locale.forLanguageTag("pt-BR")
        val Zone: ZoneId = ZoneId.of("America/Sao_Paulo")
        val CurrencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance(PtBr)
        val ShortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", PtBr)
        val YearFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy", PtBr)
    }
}

@Serializable
private data class PublicPlanCatalogRow(
    val id: String = "",
    val nome: String = "",
    val preco: String = "",
    val precoVal: Double = 0.0,
    val parcelamento: String? = null,
    val descricao: String? = null,
    val cor: String? = null,
    val icon: String? = null,
    val destaque: Boolean = false,
    val beneficios: List<String> = emptyList(),
    val xpMultiplier: Double = 1.0,
    val nivelPrioridade: Int = 1,
    val descontoLoja: Double = 0.0,
    @SerialName("tenant_id") val tenantId: String? = null,
)

@Serializable
private data class PublicPlanSubscriptionRow(
    val id: String = "",
    val aluno: String = "",
    val turma: String = "",
    val foto: String? = null,
    val planoId: String = "",
    val planoNome: String = "",
    val valorPago: Double = 0.0,
    val dataInicio: String? = null,
    val status: String = "",
    val metodo: String = "",
    val userId: String? = null,
    val createdAt: String? = null,
    @SerialName("tenant_id") val tenantId: String? = null,
)
