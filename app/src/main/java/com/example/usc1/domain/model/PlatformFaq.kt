package com.example.usc1.domain.model

import java.text.Normalizer

/**
 * `/faq` do web: `site_config` na linha `faq_page`, servida por `/api/public/faq`
 * e tipada em `platformFaqConfig.ts`.
 */
object PlatformFaqCatalog {
    const val SiteConfigTable = "site_config"
    const val ConfigRowId = "faq_page"
    const val MaxSections = 16
    const val MaxQuestionsPerSection = 24
    const val MaxSteps = 8
    const val DoubtMaxLength = 1_000

    fun normalizeText(value: String): String =
        Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
}

enum class PlatformFaqIcon(val remoteValue: String) {
    Start("start"),
    Profile("profile"),
    Card("card"),
    Events("events"),
    Store("store"),
    Training("training"),
    Admin("admin"),
    Support("support");

    companion object {
        fun fromRemote(value: String?): PlatformFaqIcon {
            val normalized = value?.trim()?.lowercase().orEmpty()
            return entries.firstOrNull { it.remoteValue == normalized } ?: Start
        }
    }
}

data class PlatformFaqQuestion(
    val id: String,
    val question: String,
    val answer: String,
    val imageUrl: String? = null,
    val imageAlt: String = "",
    val likes: Int = 0,
    val dislikes: Int = 0,
)

data class PlatformFaqSection(
    val id: String,
    val title: String,
    val description: String = "",
    val audience: String = "",
    val icon: PlatformFaqIcon = PlatformFaqIcon.Start,
    val questions: List<PlatformFaqQuestion> = emptyList(),
)

data class PlatformFaqStep(
    val id: String,
    val kicker: String,
    val title: String,
    val description: String,
    val actionLabel: String,
)

data class PlatformFaqConfig(
    val eyebrow: String = "",
    val heroTitle: String = "",
    val heroHighlight: String = "",
    val heroDescription: String = "",
    val searchPlaceholder: String = "",
    val supportTitle: String = "",
    val supportDescription: String = "",
    val supportCtaLabel: String = "",
    val updatedLabel: String = "",
    val steps: List<PlatformFaqStep> = emptyList(),
    val sections: List<PlatformFaqSection> = emptyList(),
) {
    val totalQuestions: Int get() = sections.sumOf { it.questions.size }
    val isEmpty: Boolean get() = sections.isEmpty()
}

/** `sectionMatchesQuery` / `questionMatchesQuery` da página. */
fun PlatformFaqConfig.filterByQuery(rawQuery: String): List<PlatformFaqSection> {
    val query = PlatformFaqCatalog.normalizeText(rawQuery.trim())
    if (query.isBlank()) return sections

    return sections.mapNotNull { section ->
        val sectionMatch = PlatformFaqCatalog
            .normalizeText("${section.title} ${section.description} ${section.audience}")
            .contains(query)
        val matchingQuestions = section.questions.filter { question ->
            PlatformFaqCatalog
                .normalizeText("${question.question} ${question.answer}")
                .contains(query)
        }
        val questions = if (sectionMatch) section.questions else matchingQuestions
        if (questions.isEmpty() && !sectionMatch) null else section.copy(questions = questions)
    }
}

/**
 * `handleQuestionDoubt`: assunto e corpo enviados ao painel master.
 * O web trunca o assunto em 50 caracteres na própria `submitSupportRequest`.
 */
fun buildFaqDoubtSubject(sectionTitle: String, question: String): String =
    "FAQ USC - $sectionTitle - $question"

fun buildFaqDoubtMessage(
    sectionTitle: String,
    question: PlatformFaqQuestion,
    doubt: String,
): String = listOf(
    "Origem: FAQ USC",
    "Seção: $sectionTitle",
    "Pergunta: ${question.question}",
    "ID da pergunta: ${question.id}",
    "",
    "Dúvida enviada:",
    doubt.trim(),
).joinToString("\n")
