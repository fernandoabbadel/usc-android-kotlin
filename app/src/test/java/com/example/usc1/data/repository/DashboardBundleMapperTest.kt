package com.example.usc1.data.repository

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardBundleMapperTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `mapeia o bundle público mantendo a ordem e os totais`() {
        val payload = json.parseToJsonElement(
            """
            {
              "events": [
                {
                  "id": "evento-1",
                  "titulo": "Calourada 02/2026",
                  "data": "2026-08-15",
                  "imagem": "https://cdn.test/evento.jpg",
                  "imagePositionY": 72,
                  "likesCount": 3,
                  "interessadosCount": 7,
                  "viewerHasLiked": true,
                  "viewerIsInterested": true
                }
              ],
              "produtos": [
                {
                  "id": "produto-1",
                  "nome": "Caderno Medicina",
                  "preco": 30.0,
                  "topTurmas": [
                    {"turma": "T2", "count": 8},
                    {"turma": "T4", "count": 3}
                  ]
                }
              ],
              "parceiros": [
                {"id": "parceiro-1", "nome": "MedCode", "plano": "OURO"}
              ],
              "ligas": [
                {"id": "liga-1", "nome": "Liga Acadêmica", "logoUrl": "https://cdn.test/liga.png"}
              ],
              "mensagens": [
                {"id": "post-1", "userName": "Fernando", "texto": "Fut hoje 20h"}
              ],
              "treinos": ["https://cdn.test/1.jpg", "https://cdn.test/1.jpg", "https://cdn.test/2.jpg"],
              "totalCaca": 7,
              "totalAlunos": 51
            }
            """.trimIndent(),
        ).jsonObject

        val result = DashboardBundleMapper.fromJson(payload)

        assertEquals("Calourada 02/2026", result.events.single().title)
        assertTrue(result.events.single().viewerHasLiked)
        assertTrue(result.events.single().viewerIsInterested)
        assertEquals(72.0, result.events.single().imagePositionY ?: 0.0, 0.0)
        assertEquals(30.0, result.products.single().price, 0.0)
        assertEquals("T2", result.products.single().topClasses.first().className)
        assertEquals(8, result.products.single().topClasses.first().count)
        assertEquals("ouro", result.partners.single().tier)
        assertEquals("https://cdn.test/liga.png", result.leagues.single().logoUrl)
        assertEquals("Fut hoje 20h", result.posts.single().text)
        assertEquals(listOf("https://cdn.test/1.jpg", "https://cdn.test/2.jpg"), result.trainingImageUrls)
        assertEquals(7, result.capturedFreshmen)
        assertEquals(51, result.totalMembers)
    }

    @Test
    fun `ignora linhas sem identificador e normaliza valores inválidos`() {
        val payload = json.parseToJsonElement(
            """
            {
              "events": [{"titulo": "Sem identificador"}],
              "produtos": [{"id": "produto-1", "preco": -10}],
              "parceiros": [],
              "ligas": [],
              "mensagens": [],
              "treinos": [],
              "totalCaca": -2,
              "totalAlunos": -9
            }
            """.trimIndent(),
        ).jsonObject

        val result = DashboardBundleMapper.fromJson(payload)

        assertTrue(result.events.isEmpty())
        assertEquals(0.0, result.products.single().price, 0.0)
        assertEquals("Produto", result.products.single().name)
        assertFalse(result.products.single().viewerHasLiked)
        assertNull(result.products.single().imageUrl)
        assertEquals(0, result.capturedFreshmen)
        assertEquals(0, result.totalMembers)
    }
}
