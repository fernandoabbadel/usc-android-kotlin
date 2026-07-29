package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.domain.repository.GuideRepository
import com.example.usc1.ui.guide.GuideCategory
import com.example.usc1.ui.guide.GuideItem
import com.example.usc1.ui.guide.GuideMockData
import com.example.usc1.ui.guide.GuideSection
import com.example.usc1.ui.guide.GuideUiState
import com.example.usc1.ui.guide.LegalDocUiModel
import com.example.usc1.ui.guide.LegalUiState
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SupabaseGuideRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : GuideRepository {
    override suspend fun getGuide(tenantId: String): GuideUiState = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        if (!SupabaseClientProvider.config.isConfigured || cleanTenantId.isBlank()) {
            return@withContext GuideUiState(
                sections = GuideMockData.sections,
                faqItems = GuideMockData.faqItems,
                errorMessage = "Supabase não configurado para carregar o guia.",
            )
        }

        val rows = clientProvider()
            .from(GuideTable)
            .select(columns = Columns.raw(GuideColumns)) {
                filter {
                    eq("tenant_id", cleanTenantId)
                }
                order(column = "ordem", order = Order.ASCENDING)
                limit(count = MaxGuideItems.toLong())
            }
            .decodeList<GuideDataRow>()

        val items = rows
            .map { it.toUiItem() }
            .sortedWith(compareBy<GuideItem> { it.category.ordinal }.thenBy { it.order }.thenBy { it.title })

        val sections = GuideCategory.entries
            .map { category ->
                GuideSection(
                    category = category,
                    items = items.filter { it.category == category },
                )
            }
            .filter { it.items.isNotEmpty() }

        GuideUiState(
            sections = sections.ifEmpty { GuideMockData.sections },
            faqItems = buildFaqItems(items),
            isLoading = false,
            errorMessage = null,
        )
    }

    override suspend fun getLegalDocs(tenantId: String): LegalUiState = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        if (!SupabaseClientProvider.config.isConfigured || cleanTenantId.isBlank()) {
            return@withContext LegalUiState(
                docs = GuideMockData.legalDocs,
                errorMessage = "Supabase não configurado para carregar documentos legais.",
            )
        }

        val docs = clientProvider()
            .from(LegalDocsTable)
            .select(columns = Columns.raw(LegalDocColumns)) {
                filter {
                    eq("tenant_id", cleanTenantId)
                }
                order(column = "updatedAt", order = Order.DESCENDING)
                limit(count = MaxLegalDocs.toLong())
            }
            .decodeList<LegalDocRow>()
            .map { it.toUiDoc() }

        LegalUiState(
            docs = docs.ifEmpty { GuideMockData.legalDocs },
            isLoading = false,
            errorMessage = null,
        )
    }

    private fun GuideDataRow.toUiItem(): GuideItem {
        val category = GuideCategory.from(categoria)
        val primaryTitle = firstNotBlank(titulo, nome, "Item do guia")
        val primaryDescription = when (category) {
            GuideCategory.Academic,
            GuideCategory.Groups -> firstNotBlank(url, descricao, detalhe, "Link informativo da atlética.")
            GuideCategory.Transport -> firstNotBlank(detalhe, horario, descricao, "Informações de transporte.")
            GuideCategory.Tourism -> firstNotBlank(descricao, detalhe, "Ponto recomendado pela atlética.")
            GuideCategory.Emergency -> firstNotBlank(numero, detalhe, descricao, "Canal de emergência.")
        }
        return GuideItem(
            id = id.trim().ifBlank { primaryTitle.slugify() },
            category = category,
            order = ordem ?: Int.MAX_VALUE,
            title = primaryTitle,
            description = primaryDescription,
            badge = category.shortLabel,
            url = url?.trim()?.takeIf(String::isNotBlank),
            schedule = horario?.trim()?.takeIf(String::isNotBlank),
            detail = detalhe?.trim()?.takeIf(String::isNotBlank),
            photoUrl = foto?.trim()?.takeIf(String::isNotBlank),
            phone = numero?.trim()?.takeIf(String::isNotBlank),
            color = cor?.trim()?.takeIf(String::isNotBlank),
        )
    }

    private fun LegalDocRow.toUiDoc(): LegalDocUiModel {
        return LegalDocUiModel(
            id = id.trim().ifBlank { titulo.slugify() },
            title = titulo.trim().ifBlank { "Documento legal" },
            content = conteudo.trim().ifBlank { "Documento legal da plataforma USC." },
            iconName = iconName.trim().ifBlank { "description" },
            type = tipo.trim().ifBlank { "Legal" },
            updatedAtLabel = updatedAt.formatUpdatedAt(),
        )
    }

    private fun buildFaqItems(items: List<GuideItem>): List<GuideItem> {
        val generated = items
            .filter { item ->
                item.title.contains("?", ignoreCase = true) ||
                    item.description.contains("como", ignoreCase = true) ||
                    item.description.contains("dúvida", ignoreCase = true)
            }
            .take(8)
        return generated.ifEmpty { GuideMockData.faqItems }
    }

    private fun String.formatUpdatedAt(): String {
        val clean = trim()
        if (clean.isBlank()) return "Atualizado recentemente"
        parseInstant(clean)?.let { instant ->
            val date = LegalDateFormatter.format(instant.atZone(Zone).toLocalDate())
            return "Atualizado em $date"
        }
        return clean.take(18)
    }

    private fun parseInstant(value: String): Instant? {
        return runCatching { OffsetDateTime.parse(value).toInstant() }
            .getOrElse { runCatching { Instant.parse(value) }.getOrNull() }
    }

    private fun firstNotBlank(vararg values: String?): String {
        return values.firstNotNullOfOrNull { it?.trim()?.takeIf(String::isNotBlank) }.orEmpty()
    }

    private fun String.slugify(): String {
        return lowercase(Locale.ROOT)
            .normalizeAscii()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "item" }
    }

    private fun String.normalizeAscii(): String {
        val normalized = java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{Mn}+"), "")
    }

    private companion object {
        const val GuideTable = "guia_data"
        const val LegalDocsTable = "legal_docs"
        const val MaxGuideItems = 600
        const val MaxLegalDocs = 60
        const val GuideColumns = "id,categoria,ordem,titulo,url,nome,horario,detalhe,descricao,foto,numero,cor,tenant_id"
        const val LegalDocColumns = "id,titulo,conteudo,iconName,tipo,updatedAt,tenant_id"
        val Zone: ZoneId = ZoneId.of("America/Sao_Paulo")
        val LegalDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("pt-BR"))
    }
}

@Serializable
private data class GuideDataRow(
    val id: String = "",
    val categoria: String? = null,
    val ordem: Int? = null,
    val titulo: String? = null,
    val url: String? = null,
    val nome: String? = null,
    val horario: String? = null,
    val detalhe: String? = null,
    val descricao: String? = null,
    val foto: String? = null,
    val numero: String? = null,
    val cor: String? = null,
    @SerialName("tenant_id") val tenantId: String? = null,
)

@Serializable
private data class LegalDocRow(
    val id: String = "",
    val titulo: String = "",
    val conteudo: String = "",
    val iconName: String = "",
    val tipo: String = "",
    val updatedAt: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
)
