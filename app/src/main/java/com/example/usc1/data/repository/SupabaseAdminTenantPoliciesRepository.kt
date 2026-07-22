package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.data.supabase.SupabaseTenantResolver
import com.example.usc1.domain.model.AdminTenantPoliciesBundle
import com.example.usc1.domain.model.TenantPolicyCatalog
import com.example.usc1.domain.model.TenantPolicyDocument
import com.example.usc1.domain.repository.AdminTenantPoliciesRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SupabaseAdminTenantPoliciesRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : AdminTenantPoliciesRepository {
    override suspend fun getPoliciesBundle(
        tenantName: String,
        tenantSlug: String,
        forceRefresh: Boolean,
    ): AdminTenantPoliciesBundle = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val policies = fetchPolicies(client, tenantId)

        AdminTenantPoliciesBundle(
            tenantId = tenantId,
            tenantName = tenantName,
            tenantSlug = tenantSlug,
            policies = TenantPolicyCatalog.mergeLoaded(policies),
        )
    }

    override suspend fun savePolicies(
        policies: List<TenantPolicyDocument>,
    ): List<TenantPolicyDocument> = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val userId = client.auth.currentSessionOrNull()?.user?.id
            ?: throw IllegalStateException("Sessão inválida.")

        val rows = policies
            .map(TenantPolicyCatalog::sanitizeForSave)
            .filter { it.module in TenantPolicyCatalog.modules && it.title.isNotBlank() }
            .map { policy ->
                TenantPolicyUpsertRow(
                    tenantId = tenantId,
                    module = policy.module,
                    title = policy.title,
                    content = policy.content,
                    visible = policy.visible,
                    updatedByUserId = userId,
                    createdByUserId = userId,
                )
            }

        if (rows.isEmpty()) {
            throw IllegalArgumentException("Nenhuma política válida informada.")
        }

        client.from(PoliciesTable).upsert(rows) {
            onConflict = "tenant_id,module"
        }

        TenantPolicyCatalog.mergeLoaded(fetchPolicies(client, tenantId))
    }

    private suspend fun fetchPolicies(
        client: SupabaseClient,
        tenantId: String,
    ): List<TenantPolicyDocument> {
        return client.from(PoliciesTable)
            .select(columns = Columns.raw(PolicyColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                }
                order(column = "module", order = Order.ASCENDING)
            }
            .decodeList<TenantPolicyRow>()
            .mapNotNull { it.toDomain() }
    }

    private fun TenantPolicyRow.toDomain(): TenantPolicyDocument? {
        val cleanModule = module.trim()
        if (cleanModule !in TenantPolicyCatalog.modules) return null
        val template = TenantPolicyCatalog.templates.first { it.module == cleanModule }
        return template.copy(
            id = id?.trim()?.takeIf { it.isNotBlank() },
            tenantId = tenantId?.trim()?.takeIf { it.isNotBlank() },
            title = title.trim().ifBlank { template.title },
            content = content.orEmpty(),
            visible = visible ?: false,
            updatedAt = updatedAt?.trim()?.takeIf { it.isNotBlank() },
        )
    }

    private companion object {
        const val PoliciesTable = "tenant_policy_documents"
        const val PolicyColumns = "id,tenant_id,module,title,content,visible,updated_at"
    }
}

@Serializable
private data class TenantPolicyRow(
    val id: String? = null,
    @SerialName("tenant_id") val tenantId: String? = null,
    val module: String = "",
    val title: String = "",
    val content: String? = null,
    val visible: Boolean? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
private data class TenantPolicyUpsertRow(
    @SerialName("tenant_id") val tenantId: String,
    val module: String,
    val title: String,
    val content: String,
    val visible: Boolean,
    @SerialName("updated_by_user_id") val updatedByUserId: String,
    @SerialName("created_by_user_id") val createdByUserId: String,
)
