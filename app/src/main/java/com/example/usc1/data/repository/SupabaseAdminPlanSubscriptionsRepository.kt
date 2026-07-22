package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.data.supabase.SupabaseTenantResolver
import com.example.usc1.domain.model.AdminPlanRequest
import com.example.usc1.domain.model.AdminPlanRequestStatus
import com.example.usc1.domain.model.AdminPlanSubscription
import com.example.usc1.domain.model.AdminPlanSubscriptionMethod
import com.example.usc1.domain.model.AdminPlanSubscriptionStatus
import com.example.usc1.domain.repository.AdminPlanSubscriptionsRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant
import java.time.OffsetDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SupabaseAdminPlanSubscriptionsRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : AdminPlanSubscriptionsRepository {
    override suspend fun fetchPlanSubscriptions(
        maxResults: Int,
        forceRefresh: Boolean,
    ): List<AdminPlanSubscription> = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val limit = maxResults.coerceIn(1, MaxSubscriptionResults)

        client.from(SubscriptionsTable)
            .select(columns = Columns.raw(SubscriptionColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                }
                order(column = "dataInicio", order = Order.DESCENDING)
                limit(count = limit.toLong())
            }
            .decodeList<PlanSubscriptionRow>()
            .mapNotNull { it.toDomain() }
    }

    override suspend fun fetchPlanRequests(
        maxResults: Int,
        forceRefresh: Boolean,
    ): List<AdminPlanRequest> = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val limit = maxResults.coerceIn(1, MaxRequestResults)
        runCatching {
            fetchRequestRows(client = client, tenantId = tenantId, limit = limit, withOrder = true)
        }.recoverCatching {
            fetchRequestRows(client = client, tenantId = tenantId, limit = limit, withOrder = false)
                .sortedByDescending { row -> row.dataSolicitacao.toMillis() }
        }.getOrThrow()
    }

    private suspend fun fetchRequestRows(
        client: SupabaseClient,
        tenantId: String,
        limit: Int,
        withOrder: Boolean,
    ): List<AdminPlanRequest> {
        return client.from(RequestsTable)
            .select(columns = Columns.raw(RequestColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                }
                if (withOrder) {
                    order(column = "dataSolicitacao", order = Order.DESCENDING)
                }
                limit(count = limit.toLong())
            }
            .decodeList<PlanRequestRow>()
            .mapNotNull { it.toDomain() }
    }

    private companion object {
        const val MaxSubscriptionResults = 900
        const val MaxRequestResults = 500
        const val SubscriptionsTable = "assinaturas"
        const val RequestsTable = "solicitacoes_adesao"
        const val SubscriptionColumns =
            "id,aluno,turma,foto,planoId,planoNome,valorPago,dataInicio,status,metodo,userId"
        const val RequestColumns =
            "id,userId,userName,userTurma,planoId,planoNome,valor,comprovanteUrl,dataSolicitacao,status,metodo"
    }
}

@Serializable
private data class PlanRequestRow(
    val id: String = "",
    @SerialName("userId") val userId: String? = null,
    @SerialName("userName") val userName: String? = null,
    @SerialName("userTurma") val userTurma: String? = null,
    @SerialName("planoId") val planoId: String? = null,
    @SerialName("planoNome") val planoNome: String? = null,
    val valor: Double? = null,
    @SerialName("comprovanteUrl") val comprovanteUrl: String? = null,
    @SerialName("dataSolicitacao") val dataSolicitacao: String? = null,
    val status: String? = null,
    val metodo: String? = null,
) {
    fun toDomain(): AdminPlanRequest? {
        val cleanId = id.trim()
        if (cleanId.isBlank()) return null
        return AdminPlanRequest(
            id = cleanId,
            userId = userId?.trim().orEmpty(),
            userName = userName?.trim().orEmpty().ifBlank { "Aluno" },
            userTurma = userTurma?.trim().orEmpty().ifBlank { "T??" },
            planoId = planoId?.trim().orEmpty(),
            planoNome = planoNome?.trim().orEmpty(),
            valor = (valor ?: 0.0).coerceAtLeast(0.0),
            comprovanteUrl = comprovanteUrl?.trim().orEmpty(),
            dataSolicitacao = dataSolicitacao?.trim().orEmpty(),
            status = AdminPlanRequestStatus.fromRemote(status),
            metodo = metodo?.trim().orEmpty(),
        )
    }
}

@Serializable
private data class PlanSubscriptionRow(
    val id: String = "",
    val aluno: String? = null,
    val turma: String? = null,
    val foto: String? = null,
    @SerialName("planoId") val planoId: String? = null,
    @SerialName("planoNome") val planoNome: String? = null,
    @SerialName("valorPago") val valorPago: Double? = null,
    @SerialName("dataInicio") val dataInicio: String? = null,
    val status: String? = null,
    val metodo: String? = null,
    @SerialName("userId") val userId: String? = null,
) {
    fun toDomain(): AdminPlanSubscription? {
        val cleanId = id.trim()
        if (cleanId.isBlank()) return null
        return AdminPlanSubscription(
            id = cleanId,
            aluno = aluno?.trim().orEmpty().ifBlank { "Aluno" },
            turma = turma?.trim().orEmpty().ifBlank { "T??" },
            foto = foto?.trim().orEmpty(),
            planoId = planoId?.trim().orEmpty(),
            planoNome = planoNome?.trim().orEmpty(),
            valorPago = (valorPago ?: 0.0).coerceAtLeast(0.0),
            dataInicio = dataInicio?.trim().orEmpty(),
            status = AdminPlanSubscriptionStatus.fromRemote(status),
            metodo = AdminPlanSubscriptionMethod.fromRemote(metodo),
            userId = userId?.trim().orEmpty(),
        )
    }
}

private fun String.toMillis(): Long {
    val value = trim()
    if (value.isBlank()) return 0L
    return runCatching { Instant.parse(value).toEpochMilli() }
        .recoverCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }
        .getOrDefault(0L)
}
