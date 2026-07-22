package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.data.supabase.SupabaseTenantResolver
import com.example.usc1.domain.model.AdminMiniVendor
import com.example.usc1.domain.model.AdminMiniVendorStatus
import com.example.usc1.domain.repository.AdminMiniVendorsRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.time.OffsetDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SupabaseAdminMiniVendorsRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : AdminMiniVendorsRepository {
    override suspend fun getMiniVendors(forceRefresh: Boolean): List<AdminMiniVendor> = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        client.from(MiniVendorsTable)
            .select(columns = Columns.raw(MiniVendorColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                }
                order(column = "created_at", order = Order.DESCENDING)
                limit(count = MaxMiniVendors.toLong())
            }
            .decodeList<MiniVendorRow>()
            .mapNotNull(MiniVendorRow::toDomain)
    }

    override suspend fun setMiniVendorStatus(
        miniVendorId: String,
        status: AdminMiniVendorStatus,
        approvedBy: String,
    ): Unit = withContext(Dispatchers.IO) {
        val cleanMiniVendorId = miniVendorId.trim()
        if (cleanMiniVendorId.isBlank()) return@withContext
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val current = fetchMiniVendor(client, tenantId, cleanMiniVendorId)
        val now = OffsetDateTime.now().toString()
        val approvedByValue = if (status == AdminMiniVendorStatus.Approved) {
            approvedBy.trim().ifBlank { current.approvedBy.ifBlank { "admin" } }
        } else {
            ""
        }

        val updated = client.from(MiniVendorsTable)
            .update(
                mapOf(
                    "status" to status.remoteValue,
                    "approved_by" to approvedByValue.ifBlank { null },
                    "approved_at" to if (status == AdminMiniVendorStatus.Approved) now else null,
                    "updated_at" to now,
                ),
            ) {
                filter {
                    eq("id", current.id)
                    eq("tenant_id", tenantId)
                }
                select(columns = Columns.raw(MiniVendorColumns))
            }
            .decodeList<MiniVendorRow>()
            .firstOrNull()
            ?.toDomain()
            ?: throw IllegalStateException("Não foi possível atualizar o mini vendor.")

        if (updated.status == AdminMiniVendorStatus.Approved) {
            updateUserRole(client, updated.userId, updated.tenantId, "mini_vendor")
            syncMiniVendorStoreCategory(client, updated)
        } else if (updated.status == AdminMiniVendorStatus.Rejected || updated.status == AdminMiniVendorStatus.Disabled) {
            maybeDowngradeMiniVendorUser(client, updated)
        }
    }

    override suspend fun setCategoryVisibility(
        miniVendorId: String,
        visible: Boolean,
    ): Unit = withContext(Dispatchers.IO) {
        val cleanMiniVendorId = miniVendorId.trim()
        if (cleanMiniVendorId.isBlank()) return@withContext
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val current = fetchMiniVendor(client, tenantId, cleanMiniVendorId)
        val now = OffsetDateTime.now().toString()

        val updated = client.from(MiniVendorsTable)
            .update(
                mapOf(
                    "category_visible" to visible,
                    "updated_at" to now,
                ),
            ) {
                filter {
                    eq("id", current.id)
                    eq("tenant_id", tenantId)
                }
                select(columns = Columns.raw(MiniVendorColumns))
            }
            .decodeList<MiniVendorRow>()
            .firstOrNull()
            ?.toDomain()
            ?: throw IllegalStateException("Não foi possível atualizar a categoria do mini vendor.")

        if (updated.status == AdminMiniVendorStatus.Approved) {
            syncMiniVendorStoreCategory(client, updated)
        }
    }

    private suspend fun fetchMiniVendor(
        client: SupabaseClient,
        tenantId: String,
        miniVendorId: String,
    ): AdminMiniVendor {
        return client.from(MiniVendorsTable)
            .select(columns = Columns.raw(MiniVendorColumns)) {
                filter {
                    eq("id", miniVendorId)
                    eq("tenant_id", tenantId)
                }
                limit(count = 1)
            }
            .decodeList<MiniVendorRow>()
            .firstOrNull()
            ?.toDomain()
            ?: throw IllegalStateException("Mini vendor não encontrado.")
    }

    private suspend fun updateUserRole(
        client: SupabaseClient,
        userId: String,
        tenantId: String,
        role: String,
    ) {
        val now = OffsetDateTime.now().toString()
        client.from(UsersTable)
            .update(
                mapOf(
                    "role" to role,
                    "tenant_role" to role,
                    "tenant_status" to "approved",
                    "tenant_id" to tenantId,
                    "legal_admin_required_at" to now,
                    "legal_admin_required_reason" to "role:$role",
                    "legal_admin_accepted_at" to null,
                    "updatedAt" to now,
                ),
            ) {
                filter {
                    eq("uid", userId)
                }
            }

        client.from(TenantMembershipsTable)
            .update(
                mapOf(
                    "role" to role,
                    "status" to "approved",
                    "updated_at" to now,
                ),
            ) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("user_id", userId)
                }
            }
    }

    private suspend fun maybeDowngradeMiniVendorUser(
        client: SupabaseClient,
        profile: AdminMiniVendor,
    ) {
        val user = client.from(UsersTable)
            .select(columns = Columns.raw("uid,tenant_role")) {
                filter {
                    eq("uid", profile.userId)
                }
                limit(count = 1)
            }
            .decodeList<UserRoleRow>()
            .firstOrNull()
            ?: return
        if (user.tenantRole.trim().lowercase() == "mini_vendor") {
            updateUserRole(client, profile.userId, profile.tenantId, "user")
        }
    }

    private suspend fun syncMiniVendorStoreCategory(
        client: SupabaseClient,
        profile: AdminMiniVendor,
    ) {
        if (profile.status != AdminMiniVendorStatus.Approved || profile.storeName.isBlank()) return
        val now = OffsetDateTime.now().toString()
        val existingCategory = client.from(CategoriesTable)
            .select(columns = Columns.raw("id,cover_img,logo_url")) {
                filter {
                    eq("tenant_id", profile.tenantId)
                    eq("seller_type", "mini_vendor")
                    eq("seller_id", profile.id)
                }
                limit(count = 1)
            }
            .decodeList<MiniVendorCategoryRow>()
            .firstOrNull()

        val categoryPayload = mapOf(
            "tenant_id" to profile.tenantId,
            "nome" to profile.storeName.take(80),
            "cover_img" to (profile.coverUrl.ifBlank { existingCategory?.coverImg.orEmpty() }.ifBlank { null }),
            "button_color" to profile.categoryButtonColor.ifBlank { "#2563eb" },
            "logo_url" to (profile.logoUrl.ifBlank { existingCategory?.logoUrl.orEmpty() }.ifBlank { null }),
            "seller_type" to "mini_vendor",
            "seller_id" to profile.id,
            "visible" to profile.categoryVisible,
            "updatedAt" to now,
        )

        if (existingCategory?.id?.isNotBlank() == true) {
            client.from(CategoriesTable)
                .update(categoryPayload) {
                    filter {
                        eq("id", existingCategory.id)
                        eq("tenant_id", profile.tenantId)
                    }
                }
        } else {
            client.from(CategoriesTable)
                .insert(categoryPayload + mapOf("createdAt" to now))
        }

        val productPatch = mutableMapOf<String, Any?>(
            "categoria" to profile.storeName,
            "seller_name" to profile.storeName,
            "updatedAt" to now,
        )
        if (profile.logoUrl.isNotBlank()) {
            productPatch["seller_logo_url"] = profile.logoUrl
        }
        client.from(ProductsTable)
            .update(productPatch) {
                filter {
                    eq("tenant_id", profile.tenantId)
                    eq("seller_type", "mini_vendor")
                    eq("seller_id", profile.id)
                }
            }
    }

    private companion object {
        const val MiniVendorsTable = "mini_vendors"
        const val UsersTable = "users"
        const val TenantMembershipsTable = "tenant_memberships"
        const val CategoriesTable = "categorias"
        const val ProductsTable = "produtos"
        const val MaxMiniVendors = 240
        const val MiniVendorColumns =
            "id,tenant_id,user_id,status,store_name,slug,description,logo_url,cover_url,pix_key,pix_bank,pix_holder,pix_whatsapp,instagram,instagram_enabled,whatsapp,whatsapp_enabled,profile_visible,category_visible,products_visible,category_button_color,approved_by,approved_at,created_at,updated_at"
    }
}

@Serializable
private data class MiniVendorRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String = "",
    @SerialName("user_id") val userId: String = "",
    val status: String? = null,
    @SerialName("store_name") val storeName: String? = null,
    val slug: String? = null,
    val description: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("pix_key") val pixKey: String? = null,
    @SerialName("pix_bank") val pixBank: String? = null,
    @SerialName("pix_holder") val pixHolder: String? = null,
    @SerialName("pix_whatsapp") val pixWhatsapp: String? = null,
    val instagram: String? = null,
    @SerialName("instagram_enabled") val instagramEnabled: Boolean? = null,
    val whatsapp: String? = null,
    @SerialName("whatsapp_enabled") val whatsappEnabled: Boolean? = null,
    @SerialName("profile_visible") val profileVisible: Boolean? = null,
    @SerialName("category_visible") val categoryVisible: Boolean? = null,
    @SerialName("products_visible") val productsVisible: Boolean? = null,
    @SerialName("category_button_color") val categoryButtonColor: String? = null,
    @SerialName("approved_by") val approvedBy: String? = null,
    @SerialName("approved_at") val approvedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
) {
    fun toDomain(): AdminMiniVendor? {
        val cleanId = id.trim()
        val cleanTenantId = tenantId.trim()
        val cleanUserId = userId.trim()
        if (cleanId.isBlank() || cleanTenantId.isBlank() || cleanUserId.isBlank()) return null
        return AdminMiniVendor(
            id = cleanId,
            tenantId = cleanTenantId,
            userId = cleanUserId,
            status = AdminMiniVendorStatus.fromRemote(status),
            storeName = storeName?.trim().orEmpty(),
            slug = slug?.trim().orEmpty(),
            description = description?.trim().orEmpty(),
            logoUrl = logoUrl?.trim().orEmpty(),
            coverUrl = coverUrl?.trim().orEmpty(),
            pixKey = pixKey?.trim().orEmpty(),
            pixBank = pixBank?.trim().orEmpty(),
            pixHolder = pixHolder?.trim().orEmpty(),
            pixWhatsapp = pixWhatsapp?.trim().orEmpty(),
            instagram = instagram?.trim().orEmpty(),
            instagramEnabled = instagramEnabled ?: false,
            whatsapp = whatsapp?.trim().orEmpty(),
            whatsappEnabled = whatsappEnabled ?: false,
            profileVisible = profileVisible ?: true,
            categoryVisible = categoryVisible ?: true,
            productsVisible = productsVisible ?: true,
            categoryButtonColor = categoryButtonColor?.trim().orEmpty().ifBlank { "#2563eb" },
            approvedBy = approvedBy?.trim().orEmpty(),
            approvedAt = approvedAt?.trim().orEmpty(),
            createdAt = createdAt?.trim().orEmpty(),
            updatedAt = updatedAt?.trim().orEmpty(),
        )
    }
}

@Serializable
private data class UserRoleRow(
    val uid: String = "",
    @SerialName("tenant_role") val tenantRole: String = "",
)

@Serializable
private data class MiniVendorCategoryRow(
    val id: String = "",
    @SerialName("cover_img") val coverImg: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
)
