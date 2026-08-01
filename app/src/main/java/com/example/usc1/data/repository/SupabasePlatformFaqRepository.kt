package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.domain.model.PlatformFaqCatalog
import com.example.usc1.domain.model.PlatformFaqConfig
import com.example.usc1.domain.model.PlatformFaqIcon
import com.example.usc1.domain.model.PlatformFaqQuestion
import com.example.usc1.domain.model.PlatformFaqSection
import com.example.usc1.domain.model.PlatformFaqStep
import com.example.usc1.domain.repository.PlatformFaqRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Leitura direta da linha que `/api/public/faq` serve para `/faq`.
 * A rota do web usa service role só para poder gravar; a leitura é pública.
 */
class SupabasePlatformFaqRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : PlatformFaqRepository {

    override suspend fun getFaqConfig(): PlatformFaqConfig = withContext(Dispatchers.IO) {
        if (!SupabaseClientProvider.config.isConfigured) return@withContext PlatformFaqConfig()

        val row = clientProvider().from(PlatformFaqCatalog.SiteConfigTable)
            .select(columns = Columns.raw("id,data")) {
                filter { eq("id", PlatformFaqCatalog.ConfigRowId) }
                limit(count = 1)
            }
            .decodeList<JsonObject>()
            .firstOrNull()
            ?: return@withContext PlatformFaqConfig()

        // `extractPayloadData` aceita `config`, `data` ou `payload`.
        val payload = (row["config"] as? JsonObject)
            ?: (row["data"] as? JsonObject)
            ?: (row["payload"] as? JsonObject)
            ?: row

        parseConfig(payload)
    }

    private fun parseConfig(payload: JsonObject): PlatformFaqConfig {
        val sections = (payload["sections"] as? JsonArray)
            .orEmpty()
            .take(PlatformFaqCatalog.MaxSections)
            .mapIndexedNotNull { index, element ->
                val entry = element as? JsonObject ?: return@mapIndexedNotNull null
                val title = entry.text("title")
                if (title.isBlank()) return@mapIndexedNotNull null
                PlatformFaqSection(
                    id = entry.text("id").ifBlank { "section_$index" },
                    title = title,
                    description = entry.text("description"),
                    audience = entry.text("audience"),
                    icon = PlatformFaqIcon.fromRemote(entry.text("icon")),
                    questions = (entry["questions"] as? JsonArray)
                        .orEmpty()
                        .take(PlatformFaqCatalog.MaxQuestionsPerSection)
                        .mapIndexedNotNull { questionIndex, questionElement ->
                            val question = questionElement as? JsonObject
                                ?: return@mapIndexedNotNull null
                            val text = question.text("question")
                            if (text.isBlank()) return@mapIndexedNotNull null
                            PlatformFaqQuestion(
                                id = question.text("id").ifBlank { "question_$questionIndex" },
                                question = text,
                                answer = question.text("answer"),
                                imageUrl = resolveRemoteImageUrl(question.text("imageUrl")),
                                imageAlt = question.text("imageAlt"),
                                likes = question.number("likes"),
                                dislikes = question.number("dislikes"),
                            )
                        },
                )
            }

        val steps = (payload["steps"] as? JsonArray)
            .orEmpty()
            .take(PlatformFaqCatalog.MaxSteps)
            .mapIndexedNotNull { index, element ->
                val entry = element as? JsonObject ?: return@mapIndexedNotNull null
                val title = entry.text("title")
                if (title.isBlank()) return@mapIndexedNotNull null
                PlatformFaqStep(
                    id = entry.text("id").ifBlank { "step_$index" },
                    kicker = entry.text("kicker").ifBlank { "%02d".format(index + 1) },
                    title = title,
                    description = entry.text("description"),
                    actionLabel = entry.text("actionLabel"),
                )
            }

        return PlatformFaqConfig(
            eyebrow = payload.text("eyebrow"),
            heroTitle = payload.text("heroTitle"),
            heroHighlight = payload.text("heroHighlight"),
            heroDescription = payload.text("heroDescription"),
            searchPlaceholder = payload.text("searchPlaceholder").ifBlank { "Buscar no FAQ" },
            supportTitle = payload.text("supportTitle"),
            supportDescription = payload.text("supportDescription"),
            supportCtaLabel = payload.text("supportCtaLabel"),
            updatedLabel = payload.text("updatedLabel"),
            steps = steps,
            sections = sections,
        )
    }
}

private fun JsonObject.text(key: String): String {
    val element = this[key] ?: return ""
    if (element is JsonNull) return ""
    return (element as? JsonPrimitive)?.contentOrNull.orEmpty().trim()
}

private fun JsonObject.number(key: String): Int {
    val element = this[key] as? JsonPrimitive ?: return 0
    return element.intOrNull ?: element.contentOrNull?.toIntOrNull() ?: 0
}
