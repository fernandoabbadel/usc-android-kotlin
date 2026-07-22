package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.data.supabase.SupabaseTenantResolver
import com.example.usc1.domain.model.AdminPartnerScansPage
import com.example.usc1.domain.model.AdminPartnersBundle
import com.example.usc1.domain.model.AdminPartnersPage
import com.example.usc1.domain.model.AdminPartnersTierCounts
import com.example.usc1.domain.model.PartnerCoupon
import com.example.usc1.domain.model.PartnerForm
import com.example.usc1.domain.model.PartnerPasswordReset
import com.example.usc1.domain.model.PartnerRecord
import com.example.usc1.domain.model.PartnerScanRecord
import com.example.usc1.domain.model.PartnerStatus
import com.example.usc1.domain.model.PartnerTier
import com.example.usc1.domain.model.PartnersCatalog
import com.example.usc1.domain.repository.PartnersRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

class SupabasePartnersRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : PartnersRepository {
    override suspend fun getPublicPartners(forceRefresh: Boolean): List<PartnerRecord> = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        client.from(PartnersTable)
            .select(columns = Columns.raw(PartnerColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("status", PartnerStatus.Active.remoteValue)
                }
                order(column = "tier", order = Order.ASCENDING)
                order(column = "nome", order = Order.ASCENDING)
                limit(count = PartnersCatalog.MaxPublicPartners.toLong())
            }
            .decodeList<PartnerRow>()
            .mapNotNull { it.toDomain(tenantId) }
            .filter { it.status == PartnerStatus.Active }
            .sortedWith(compareBy<PartnerRecord> { it.tier.rank }.thenBy { it.name.lowercase() })
    }

    override suspend fun getPartnerById(partnerId: String, forceRefresh: Boolean): PartnerRecord? = withContext(Dispatchers.IO) {
        val cleanPartnerId = partnerId.trim()
        if (cleanPartnerId.isBlank()) return@withContext null
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        client.from(PartnersTable)
            .select(columns = Columns.raw(PartnerColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("id", cleanPartnerId)
                }
                limit(count = 1)
            }
            .decodeList<PartnerRow>()
            .firstOrNull()
            ?.toDomain(tenantId)
    }

    override suspend fun getAdminPartnersPage(
        status: PartnerStatus?,
        page: Int,
        pageSize: Int,
        forceRefresh: Boolean,
    ): AdminPartnersPage = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val safePage = page.coerceAtLeast(1)
        val safePageSize = pageSize.coerceIn(1, PartnersCatalog.PageSize)
        val from = ((safePage - 1) * safePageSize).toLong()
        val to = from + safePageSize
        val rows = client.from(PartnersTable)
            .select(columns = Columns.raw(PartnerColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                    if (status != null) {
                        eq("status", status.remoteValue)
                    }
                }
                order(column = "nome", order = Order.ASCENDING)
                range(from..to)
            }
            .decodeList<PartnerRow>()
            .mapNotNull { it.toDomain(tenantId) }

        AdminPartnersPage(
            tenantId = tenantId,
            partners = rows.take(safePageSize),
            page = safePage,
            pageSize = safePageSize,
            hasMore = rows.size > safePageSize,
            statusFilter = status,
        )
    }

    override suspend fun getAdminPartnerScansPage(
        page: Int,
        pageSize: Int,
        forceRefresh: Boolean,
    ): AdminPartnerScansPage = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val safePage = page.coerceAtLeast(1)
        val safePageSize = pageSize.coerceIn(1, PartnersCatalog.PageSize)
        val from = ((safePage - 1) * safePageSize).toLong()
        val to = from + safePageSize
        val rows = client.from(ScansTable)
            .select(columns = Columns.raw(ScanColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                }
                order(column = "timestamp", order = Order.DESCENDING)
                range(from..to)
            }
            .decodeList<PartnerScanRow>()
            .mapNotNull { it.toDomain(tenantId) }

        AdminPartnerScansPage(
            tenantId = tenantId,
            scans = rows.take(safePageSize),
            page = safePage,
            pageSize = safePageSize,
            hasMore = rows.size > safePageSize,
        )
    }

    override suspend fun getAdminPartnersBundle(forceRefresh: Boolean): AdminPartnersBundle = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val partners = client.from(PartnersTable)
            .select(columns = Columns.raw(PartnerColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                }
                order(column = "nome", order = Order.ASCENDING)
                limit(count = PartnersCatalog.BiPartnersLimit.toLong())
            }
            .decodeList<PartnerRow>()
            .mapNotNull { it.toDomain(tenantId) }

        val scans = client.from(ScansTable)
            .select(columns = Columns.raw(ScanColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                }
                order(column = "timestamp", order = Order.DESCENDING)
                limit(count = PartnersCatalog.BiScansLimit.toLong())
            }
            .decodeList<PartnerScanRow>()
            .mapNotNull { it.toDomain(tenantId) }

        AdminPartnersBundle(
            tenantId = tenantId,
            partners = partners,
            scans = scans,
        )
    }

    override suspend fun getAdminPartnersTierCounts(forceRefresh: Boolean): AdminPartnersTierCounts = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val rows = client.from(PartnersTable)
            .select(columns = Columns.raw("id,status,tier")) {
                filter {
                    eq("tenant_id", tenantId)
                }
                limit(count = PartnersCatalog.BiPartnersLimit.toLong())
            }
            .decodeList<PartnerCountRow>()

        rows.fold(AdminPartnersTierCounts()) { acc, row ->
            val status = PartnerStatus.fromRemote(row.status)
            val tier = PartnerTier.fromRemote(row.tier)
            acc.copy(
                total = acc.total + 1,
                active = acc.active + if (status == PartnerStatus.Active) 1 else 0,
                pending = acc.pending + if (status == PartnerStatus.Pending) 1 else 0,
                disabled = acc.disabled + if (status == PartnerStatus.Disabled) 1 else 0,
                gold = acc.gold + if (status == PartnerStatus.Active && tier == PartnerTier.Ouro) 1 else 0,
                silver = acc.silver + if (status == PartnerStatus.Active && tier == PartnerTier.Prata) 1 else 0,
                standard = acc.standard + if (status == PartnerStatus.Active && tier == PartnerTier.Standard) 1 else 0,
            )
        }
    }

    override suspend fun setPartnerStatus(partnerId: String, status: PartnerStatus): Unit = withContext(Dispatchers.IO) {
        val cleanPartnerId = partnerId.trim()
        if (cleanPartnerId.isBlank()) return@withContext
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        client.from(PartnersTable)
            .update(
                mapOf(
                    "status" to status.remoteValue,
                    "updatedAt" to OffsetDateTime.now().toString(),
                ),
            ) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("id", cleanPartnerId)
                }
            }
    }

    override suspend fun savePartner(form: PartnerForm): PartnerRecord = withContext(Dispatchers.IO) {
        val cleanName = form.name.trim().take(PartnersCatalog.MaxNameLength)
        if (cleanName.isBlank()) throw IllegalArgumentException("Nome do parceiro obrigatório.")
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val now = OffsetDateTime.now().toString()
        val cleanPartnerId = form.partnerId.trim()
        val targetPartnerId = cleanPartnerId.ifBlank { UUID.randomUUID().toString() }
        val payload = mapOf(
            "nome" to cleanName,
            "categoria" to form.category.trim().take(PartnersCatalog.MaxCategoryLength).ifBlank { "Parceiro" },
            "tier" to form.tier.remoteValue,
            "status" to form.status.remoteValue,
            "cnpj" to form.cnpj.trim().take(PartnersCatalog.MaxContactLength),
            "responsavel" to form.responsible.trim().take(PartnersCatalog.MaxContactLength),
            "email" to form.email.trim().lowercase().take(PartnersCatalog.MaxContactLength),
            "telefone" to form.phone.trim().take(PartnersCatalog.MaxContactLength),
            "descricao" to form.description.trim().take(PartnersCatalog.MaxDescriptionLength),
            "endereco" to form.address.trim().take(PartnersCatalog.MaxContactLength),
            "horario" to form.businessHours.trim().take(PartnersCatalog.MaxContactLength),
            "insta" to form.instagram.trim().take(PartnersCatalog.MaxContactLength),
            "site" to form.site.trim().take(PartnersCatalog.MaxUrlLength),
            "whats" to form.whatsApp.trim().take(PartnersCatalog.MaxContactLength),
            "imgLogo" to form.logoUrl.trim().take(PartnersCatalog.MaxUrlLength),
            "imgCapa" to form.coverUrl.trim().take(PartnersCatalog.MaxUrlLength),
            "updatedAt" to now,
        )

        if (cleanPartnerId.isBlank()) {
            client.from(PartnersTable)
                .insert(
                    payload + mapOf(
                        "id" to targetPartnerId,
                        "tenant_id" to tenantId,
                        "createdAt" to now,
                        "mensalidade" to 0,
                        "vendasTotal" to 0,
                        "totalScans" to 0,
                        "cupons" to JsonArray(emptyList()),
                    ),
                )
        } else {
            client.from(PartnersTable)
                .update(payload) {
                    filter {
                        eq("tenant_id", tenantId)
                        eq("id", cleanPartnerId)
                    }
                }
        }

        getPartnerById(targetPartnerId, forceRefresh = true)
            ?: throw IllegalStateException("Não foi possível carregar o parceiro salvo.")
    }

    override suspend fun requestPasswordReset(partnerId: String): PartnerPasswordReset = withContext(Dispatchers.IO) {
        val cleanPartnerId = partnerId.trim()
        if (cleanPartnerId.isBlank()) throw IllegalArgumentException("Parceiro obrigatório.")
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val code = (100000..999999).random().toString()
        val expiresAt = OffsetDateTime.now().plusMinutes(30).toString()
        client.from(PartnersTable)
            .update(
                mapOf(
                    "password_reset_code" to code,
                    "password_reset_expires_at" to expiresAt,
                    "password_reset_requested_at" to OffsetDateTime.now().toString(),
                ),
            ) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("id", cleanPartnerId)
                }
            }
        PartnerPasswordReset(code = code, expiresAt = expiresAt)
    }

    private companion object {
        const val PartnersTable = "parceiros"
        const val ScansTable = "scans"
        const val PartnerColumns =
            "id,tenant_id,nome,categoria,tier,status,cnpj,responsavel,email,telefone,descricao,endereco,horario,insta,site,whats,imgCapa,imgLogo,mensalidade,vendasTotal,totalScans,cupons,createdAt"
        const val ScanColumns =
            "id,tenant_id,empresaId,empresa,usuario,userId,cupom,valorEconomizado,data,hora,coupon_id,coupon_title,scan_method,approval_mode,qr_code,coupon_type,coupon_value,coupon_value_numeric,status,approved_by_partner_id,user_display_name,timestamp"
    }
}

@Serializable
private data class PartnerRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
    val nome: String = "",
    val categoria: String? = null,
    val tier: String? = null,
    val status: String? = null,
    val cnpj: String? = null,
    val responsavel: String? = null,
    val email: String? = null,
    val telefone: String? = null,
    val descricao: String? = null,
    val endereco: String? = null,
    val horario: String? = null,
    val insta: String? = null,
    val site: String? = null,
    val whats: String? = null,
    @SerialName("imgCapa") val imgCapa: String? = null,
    @SerialName("imgLogo") val imgLogo: String? = null,
    val mensalidade: Double? = null,
    @SerialName("vendasTotal") val vendasTotal: Double? = null,
    @SerialName("totalScans") val totalScans: Int? = null,
    val cupons: JsonElement? = null,
    @SerialName("createdAt") val createdAt: String? = null,
) {
    fun toDomain(activeTenantId: String): PartnerRecord? {
        val cleanId = id.trim()
        val cleanTenantId = tenantId?.trim().orEmpty().ifBlank { activeTenantId }
        val cleanName = nome.trim()
        if (cleanId.isBlank() || cleanTenantId.isBlank() || cleanName.isBlank()) return null
        return PartnerRecord(
            id = cleanId,
            tenantId = cleanTenantId,
            name = cleanName,
            category = categoria?.trim().orEmpty().ifBlank { "Parceiro" },
            tier = PartnerTier.fromRemote(tier),
            status = PartnerStatus.fromRemote(status),
            cnpj = cnpj?.trim().orEmpty(),
            responsible = responsavel?.trim().orEmpty(),
            email = email?.trim().orEmpty(),
            phone = telefone?.trim().orEmpty(),
            description = descricao?.trim().orEmpty(),
            address = endereco?.trim().orEmpty(),
            businessHours = horario?.trim().orEmpty(),
            instagram = insta?.trim().orEmpty(),
            site = site?.trim().orEmpty(),
            whatsApp = whats?.trim().orEmpty(),
            coverUrl = imgCapa?.trim().orEmpty(),
            logoUrl = imgLogo?.trim().orEmpty(),
            monthlyFee = mensalidade ?: 0.0,
            salesTotal = vendasTotal ?: 0.0,
            totalScans = max(0, totalScans ?: 0),
            coupons = cupons.toCoupons(),
            createdAt = createdAt?.trim().orEmpty(),
        )
    }
}

@Serializable
private data class PartnerCountRow(
    val id: String = "",
    val status: String? = null,
    val tier: String? = null,
)

@Serializable
private data class PartnerScanRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
    @SerialName("empresaId") val empresaId: String? = null,
    val empresa: String? = null,
    val usuario: String? = null,
    @SerialName("userId") val userId: String? = null,
    val cupom: String? = null,
    @SerialName("valorEconomizado") val valorEconomizado: String? = null,
    val data: String? = null,
    val hora: String? = null,
    @SerialName("coupon_id") val couponId: String? = null,
    @SerialName("coupon_title") val couponTitle: String? = null,
    @SerialName("scan_method") val scanMethod: String? = null,
    @SerialName("approval_mode") val approvalMode: String? = null,
    @SerialName("qr_code") val qrCode: String? = null,
    @SerialName("coupon_type") val couponType: String? = null,
    @SerialName("coupon_value") val couponValue: String? = null,
    @SerialName("coupon_value_numeric") val couponValueNumeric: Double? = null,
    val status: String? = null,
    @SerialName("approved_by_partner_id") val approvedByPartnerId: String? = null,
    @SerialName("user_display_name") val userDisplayName: String? = null,
    val timestamp: String? = null,
) {
    fun toDomain(activeTenantId: String): PartnerScanRecord? {
        val cleanId = id.trim()
        val cleanCompanyId = empresaId?.trim().orEmpty()
        val cleanUser = usuario?.trim().orEmpty()
        if (cleanId.isBlank() || cleanCompanyId.isBlank() || cleanUser.isBlank()) return null
        return PartnerScanRecord(
            id = cleanId,
            tenantId = tenantId?.trim().orEmpty().ifBlank { activeTenantId },
            companyId = cleanCompanyId,
            companyName = empresa?.trim().orEmpty().ifBlank { "Empresa" },
            userName = cleanUser,
            userId = userId?.trim().orEmpty(),
            couponName = cupom?.trim().orEmpty().ifBlank { "Cupom" },
            savedValueLabel = valorEconomizado?.trim().orEmpty().ifBlank { "R$ 0,00" },
            date = data?.trim().orEmpty(),
            hour = hora?.trim().orEmpty(),
            couponId = couponId?.trim().orEmpty(),
            couponTitle = couponTitle?.trim().orEmpty().ifBlank { cupom?.trim().orEmpty().ifBlank { "Cupom" } },
            scanMethod = scanMethod?.trim().orEmpty().ifBlank { "qr_code" },
            approvalMode = approvalMode?.trim().orEmpty().ifBlank { "direct_scan" },
            qrCode = qrCode?.trim().orEmpty(),
            couponType = couponType?.trim().orEmpty(),
            couponValue = couponValue?.trim().orEmpty().ifBlank { valorEconomizado?.trim().orEmpty() },
            couponValueNumeric = couponValueNumeric ?: 0.0,
            status = status?.trim().orEmpty().ifBlank { "approved" },
            approvedByPartnerId = approvedByPartnerId?.trim().orEmpty(),
            userDisplayName = userDisplayName?.trim().orEmpty().ifBlank { cleanUser },
            timestamp = timestamp?.trim().orEmpty(),
        )
    }
}

private fun JsonElement?.toCoupons(): List<PartnerCoupon> {
    val array = this as? JsonArray ?: return emptyList()
    return array.mapNotNull { element ->
        val obj = element as? JsonObject ?: return@mapNotNull null
        val title = obj.string("titulo").ifBlank { return@mapNotNull null }
        PartnerCoupon(
            id = obj.string("id").ifBlank { UUID.randomUUID().toString() },
            title = title,
            rule = obj.string("regra"),
            valueLabel = obj.string("valor"),
            imageUrl = obj.string("imagem"),
            type = obj.string("tipo"),
            active = obj["ativo"]?.jsonPrimitive?.booleanOrNull ?: true,
            qrCode = obj.string("codigoQr").ifBlank { obj.string("codigo_qr") },
        )
    }
}

private fun JsonObject.string(key: String): String {
    return this[key]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
}

private fun String.parseMoneyValue(): Double {
    val normalized = replace(Regex("[^\\d,.-]"), "")
        .replace(".", "")
        .replace(",", ".")
    return normalized.toDoubleOrNull() ?: 0.0
}

internal fun PartnerScanRecord.savedMoneyValue(): Double {
    if (couponValueNumeric > 0.0) return couponValueNumeric
    if (couponValue.contains("%") || savedValueLabel.contains("%")) return 0.0
    return couponValue.ifBlank { savedValueLabel }.parseMoneyValue()
}
