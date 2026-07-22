package com.example.usc1.data.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object SupabaseTenantResolver {
    suspend fun resolveActiveTenantId(client: SupabaseClient): String {
        val authUser = client.auth.currentSessionOrNull()?.user
            ?: throw IllegalStateException("Entre com Google e selecione uma atlética antes de continuar.")
        val userId = authUser.id

        val userTenantId = client.from(UsersTable)
            .select(columns = Columns.raw("tenant_id")) {
                filter {
                    eq("uid", userId)
                }
                limit(count = 1)
            }
            .decodeList<UserTenantRow>()
            .firstOrNull()
            ?.tenantId
            ?.trim()
            .orEmpty()

        if (userTenantId.isNotBlank()) return userTenantId

        return client.from(TenantMembershipsTable)
            .select(columns = Columns.raw("tenant_id,status")) {
                filter {
                    eq("user_id", userId)
                }
                limit(count = 20)
            }
            .decodeList<TenantMembershipTenantRow>()
            .firstOrNull { it.status == "approved" }
            ?.tenantId
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Tenant ativo não definido para este usuário.")
    }

    private const val UsersTable = "users"
    private const val TenantMembershipsTable = "tenant_memberships"
}

@Serializable
private data class UserTenantRow(
    @SerialName("tenant_id") val tenantId: String? = null,
)

@Serializable
private data class TenantMembershipTenantRow(
    @SerialName("tenant_id") val tenantId: String = "",
    val status: String? = null,
)
