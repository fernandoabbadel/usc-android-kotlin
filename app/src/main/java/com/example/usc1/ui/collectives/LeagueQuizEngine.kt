package com.example.usc1.ui.collectives

/**
 * Porte de `resolveLeagueProfile` + `calculateMatches` de
 * `web-reference/src/app/ligas_usc/page.tsx` (Oráculo de compatibilidade por liga).
 */
object LeagueQuizEngine {
    /** `expandLeagueKeyword` do web. */
    fun expandKeyword(keyword: String): List<String> {
        val base = CollectiveTextUtils.normalize(keyword)
        if (base.isBlank()) return emptyList()

        val synonyms = LeagueQuizCatalog.keywordSynonyms[base].orEmpty()
        return (listOf(base) + synonyms.map(CollectiveTextUtils::normalize)).distinct()
    }

    /** `resolveLeagueProfile` do web: casa por sigla e depois por nome/alias. */
    fun resolveProfile(group: CollectiveGroup): LeagueQuizProfile? {
        val groupName = CollectiveTextUtils.normalize(group.name)
        val groupSigla = CollectiveTextUtils.normalize(group.acronym)

        LeagueQuizCatalog.profiles.forEach { profile ->
            val profileSigla = CollectiveTextUtils.normalize(profile.sigla)
            if (profileSigla.isNotBlank() && groupSigla.isNotBlank() && profileSigla == groupSigla) {
                return profile
            }
        }

        LeagueQuizCatalog.profiles.forEach { profile ->
            val profileName = CollectiveTextUtils.normalize(profile.nome)
            val aliases = profile.aliases.map(CollectiveTextUtils::normalize)
            val hasNameMatch =
                (profileName.isNotBlank() && (groupName.contains(profileName) || profileName.contains(groupName))) ||
                    aliases.any { alias -> alias.isNotBlank() && groupName.contains(alias) }

            if (hasNameMatch) return profile
        }

        return null
    }

    /**
     * `calculateMatches` do web: soma peso de resposta direta ao perfil e peso das
     * palavras-chave (perfil ou texto da liga), normalizado pelo peso total.
     */
    fun calculateMatches(
        groups: List<CollectiveGroup>,
        answers: Map<LeagueQuizQuestionKey, List<String>>,
        keywords: List<String>,
    ): List<LeagueQuizMatch> {
        val keywordWeight = LinkedHashMap<String, Int>()
        keywords.forEach { keyword ->
            val normalized = CollectiveTextUtils.normalize(keyword)
            if (normalized.isBlank()) return@forEach
            keywordWeight[normalized] = (keywordWeight[normalized] ?: 0) + 1
        }

        val keywordTotalWeight = keywordWeight.values.sum()
        val selectedAnswerCount = answers.values.sumOf { it.size }
        val totalWeight = keywordTotalWeight + (selectedAnswerCount * LeagueQuizCatalog.DirectMatchWeight)

        return groups
            .map { group ->
                val profile = resolveProfile(group)
                val groupText = CollectiveTextUtils.normalize(
                    "${group.name} ${group.acronym} ${group.description}",
                )

                val profileKeywords = linkedSetOf<String>()
                if (profile != null) {
                    (listOf(profile.nome, profile.sigla) + profile.aliases + profile.keywords)
                        .flatMap(CollectiveTextUtils::splitTokens)
                        .forEach(profileKeywords::add)
                }
                CollectiveTextUtils.splitTokens(groupText).forEach(profileKeywords::add)

                var answerScore = 0
                if (profile != null) {
                    LeagueQuizCatalog.questions.forEach { question ->
                        val selectedAnswers = answers[question.key].orEmpty()
                        val profileAnswers = profile.quizAnswers[question.key].orEmpty()

                        selectedAnswers.forEach { selectedAnswer ->
                            val normalizedSelected = CollectiveTextUtils.normalize(selectedAnswer)
                            val matched = profileAnswers.any { profileAnswer ->
                                CollectiveTextUtils.normalize(profileAnswer) == normalizedSelected
                            }
                            if (matched) answerScore += LeagueQuizCatalog.DirectMatchWeight
                        }
                    }
                }

                var keywordScore = 0
                keywordWeight.forEach { (selectedKeyword, weight) ->
                    val expanded = expandKeyword(selectedKeyword)
                    val matchedByProfile = expanded.any { candidate ->
                        profileKeywords.any { profileKeyword ->
                            profileKeyword.contains(candidate) || candidate.contains(profileKeyword)
                        }
                    }
                    val matchedByText = expanded.any { candidate -> groupText.contains(candidate) }
                    if (matchedByProfile || matchedByText) keywordScore += weight
                }

                val score = answerScore + keywordScore
                val percent = if (totalWeight > 0) {
                    Math.round((score.toDouble() / totalWeight) * 100).toInt()
                } else {
                    0
                }

                LeagueQuizMatch(
                    collective = group,
                    matchScore = score,
                    matchPercent = percent.coerceIn(0, 100),
                )
            }
            .sortedWith(
                compareByDescending<LeagueQuizMatch> { it.matchPercent }
                    .thenByDescending { it.matchScore }
                    .thenByDescending { it.collective.likesCount },
            )
    }
}
