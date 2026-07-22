package com.example.usc1.domain.model

data class AdminAlbumUiConfig(
    val cover: String = "/capa_t8.jpg",
    val title: String = "Álbum da Galera",
    val subtitle: String = "Escolha a turma para abrir somente o que você precisa",
)

object AdminAlbumCatalog {
    const val ConfigDocId = "album_ui"
    const val MaxUrlLength = 600
    const val MaxTitleLength = 120
    const val MaxSubtitleLength = 240
}
