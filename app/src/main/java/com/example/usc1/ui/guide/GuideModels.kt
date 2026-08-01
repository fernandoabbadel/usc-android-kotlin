package com.example.usc1.ui.guide

enum class GuideCategory(
    val raw: String,
    val label: String,
    val shortLabel: String,
) {
    Academic("academico", "Acadêmico", "Acad"),
    Groups("grupos", "Grupos", "Grupo"),
    Transport("transporte", "Transporte", "Bus"),
    Tourism("turismo", "Turismo", "Local"),
    Emergency("emergencia", "Emergência", "SOS");

    companion object {
        fun from(raw: String?): GuideCategory {
            val clean = raw.orEmpty().trim().lowercase()
            return entries.firstOrNull { it.raw == clean } ?: Academic
        }
    }
}

data class GuideItem(
    val id: String,
    val category: GuideCategory,
    val order: Int,
    val title: String,
    val description: String,
    val badge: String,
    val url: String? = null,
    val schedule: String? = null,
    val detail: String? = null,
    val photoUrl: String? = null,
    val phone: String? = null,
    val color: String? = null,
)

data class GuideSection(
    val category: GuideCategory,
    val items: List<GuideItem>,
)

data class GuideUiState(
    val sections: List<GuideSection> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

data class LegalDocUiModel(
    val id: String,
    val title: String,
    val content: String,
    val iconName: String,
    val type: String,
    val updatedAtLabel: String,
)

data class LegalUiState(
    val docs: List<LegalDocUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    /** `/legal/[slug]`: o web resolve o documento pelo slug da URL. */
    fun documentBySlug(slug: String): LegalDocUiModel? {
        val clean = slug.trim().lowercase()
        if (clean.isBlank()) return null
        return docs.firstOrNull { it.id.trim().lowercase() == clean }
            ?: docs.firstOrNull { it.type.trim().lowercase() == clean }
    }
}

