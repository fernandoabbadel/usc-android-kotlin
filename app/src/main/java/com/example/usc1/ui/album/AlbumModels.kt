package com.example.usc1.ui.album

import com.example.usc1.R

data class AlbumTurma(
    val id: String,
    val name: String,
    val slug: String,
    val mascot: String,
    val score: Int,
    val members: Int,
    val coverRes: Int,
    val photoRes: Int,
    val coverUrl: String? = null,
    val logoUrl: String? = null,
    val hidden: Boolean = false,
)

data class AlbumPhoto(
    val id: String,
    val title: String,
    val imageRes: Int,
    val imageUrl: String? = null,
    val turma: String = "",
    val subtitle: String = "",
    val collected: Boolean = false,
    val publicProfileUrl: String? = null,
    val bio: String = "",
    val origin: String = "",
    val ageLabel: String = "",
    val relationship: String = "",
    val instagram: String = "",
    /** Campos preenchidos no cadastro/edicao de perfil e exibidos pelo album do web. */
    val pets: String = "",
    val sports: List<String> = emptyList(),
    val profileVisible: Boolean = true,
) {
    /** Rotulo do pet como no `/album/[turmaId]`: Dog / Cat / Zoo. */
    val petLabel: String
        get() = when (pets.trim().lowercase()) {
            "cachorro" -> "Dog"
            "gato" -> "Cat"
            "ambos" -> "Zoo"
            "", "nenhum" -> ""
            else -> pets.trim()
        }
}

data class AlbumRankingEntry(
    val id: String,
    val userId: String,
    val name: String,
    val photoUrl: String? = null,
    val turma: String = "",
    val totalCollected: Int = 0,
    val scansT8: Int = 0,
)

data class AlbumUiState(
    val title: String = "Álbum",
    val subtitle: String = "Escolha a turma para abrir somente o que você precisa",
    val heroHeadline: String = "Escolha a turma e domine o álbum",
    val heroCoverUrl: String? = null,
    val turmas: List<AlbumTurma> = emptyList(),
    val photos: List<AlbumPhoto> = emptyList(),
    val ranking: List<AlbumRankingEntry> = emptyList(),
    val currentUserCollected: Int = 0,
    val currentTurmaSlug: String = "t8",
    val myQrPayload: String = "",
    val canUseQr: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

object AlbumMockData {
    val turmas = listOf(
        AlbumTurma("T9", "Turma 9", "t9", "Cardume", 9840, 96, R.drawable.capa_t9, R.drawable.turma9),
        AlbumTurma("T8", "Turma 8", "t8", "Calouros", 8720, 88, R.drawable.capa_t8, R.drawable.turma8),
        AlbumTurma("T7", "Turma 7", "t7", "Urso Polar", 7410, 74, R.drawable.capa_t7, R.drawable.turma7),
        AlbumTurma("T6", "Turma 6", "t6", "Lagosta", 6930, 69, R.drawable.capa_t6, R.drawable.turma6),
    )

    val photos = listOf(
        AlbumPhoto("p1", "Caça-calouro", R.drawable.turma9, turma = "T9", collected = true),
        AlbumPhoto("p2", "Arquibancada", R.drawable.turma8, turma = "T8"),
        AlbumPhoto("p3", "Treino aberto", R.drawable.turma7, turma = "T7"),
        AlbumPhoto("p4", "Intermed", R.drawable.turma6, turma = "T6"),
    )

    val ranking = turmas.map { turma ->
        AlbumRankingEntry(
            id = turma.id,
            userId = turma.id,
            name = turma.name,
            turma = turma.id,
            totalCollected = turma.score / 100,
            scansT8 = if (turma.id == "T8") turma.score / 200 else 0,
        )
    }

    fun turmaById(id: String): AlbumTurma =
        turmas.firstOrNull { it.id.equals(id, ignoreCase = true) || it.slug.equals(id, ignoreCase = true) } ?: turmas.first()
}
