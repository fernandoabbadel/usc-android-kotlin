package com.example.usc1.ui.album

import com.example.usc1.R

data class AlbumTurma(
    val id: String,
    val name: String,
    val score: Int,
    val members: Int,
    val coverRes: Int,
    val photoRes: Int,
)

data class AlbumPhoto(
    val id: String,
    val title: String,
    val imageRes: Int,
)

data class AlbumUiState(
    val turmas: List<AlbumTurma> = AlbumMockData.turmas,
    val photos: List<AlbumPhoto> = AlbumMockData.photos,
)

object AlbumMockData {
    val turmas = listOf(
        AlbumTurma("t9", "Turma 9", 9840, 96, R.drawable.capa_t9, R.drawable.turma9),
        AlbumTurma("t8", "Turma 8", 8720, 88, R.drawable.capa_t8, R.drawable.turma8),
        AlbumTurma("t7", "Turma 7", 7410, 74, R.drawable.capa_t7, R.drawable.turma7),
        AlbumTurma("t6", "Turma 6", 6930, 69, R.drawable.capa_t6, R.drawable.turma6),
    )

    val photos = listOf(
        AlbumPhoto("p1", "Caça-calouro", R.drawable.turma9),
        AlbumPhoto("p2", "Arquibancada", R.drawable.turma8),
        AlbumPhoto("p3", "Treino aberto", R.drawable.turma7),
        AlbumPhoto("p4", "Intermed", R.drawable.turma6),
    )

    fun turmaById(id: String): AlbumTurma =
        turmas.firstOrNull { it.id == id } ?: turmas.first()
}
