package com.example.usc1.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Busca e envio de dúvida de `/faq`. */
class PlatformFaqTest {

    private val config = PlatformFaqConfig(
        sections = listOf(
            PlatformFaqSection(
                id = "eventos",
                title = "Eventos",
                description = "Ingressos e pedidos",
                audience = "Aluno",
                questions = listOf(
                    question("q1", "Como comprar ingresso?", "Abra o evento e escolha o lote."),
                    question("q2", "Posso transferir?", "A transferência depende da atlética."),
                ),
            ),
            PlatformFaqSection(
                id = "loja",
                title = "Loja",
                description = "Produtos e retirada",
                audience = "Aluno",
                questions = listOf(
                    question("q3", "Onde retiro o produto?", "No ponto informado no pedido."),
                ),
            ),
        ),
    )

    @Test
    fun `busca sem termo devolve todas as secoes`() {
        assertEquals(2, config.filterByQuery("").size)
    }

    @Test
    fun `busca casa pergunta ignorando acento e caixa`() {
        val result = config.filterByQuery("TRANSFERENCIA")

        assertEquals(1, result.size)
        assertEquals(listOf("q2"), result.single().questions.map { it.id })
    }

    @Test
    fun `secao que casa mantem todas as perguntas`() {
        val result = config.filterByQuery("eventos")

        assertEquals(1, result.size)
        assertEquals(listOf("q1", "q2"), result.single().questions.map { it.id })
    }

    @Test
    fun `busca sem resultado devolve lista vazia`() {
        assertTrue(config.filterByQuery("boletim").isEmpty())
    }

    @Test
    fun `contadores do topo somam secoes e respostas`() {
        assertEquals(3, config.totalQuestions)
        assertEquals(2, config.sections.size)
    }

    @Test
    fun `assunto e corpo da duvida seguem o formato do web`() {
        val section = config.sections.first()
        val question = section.questions.first()

        assertEquals(
            "FAQ USC - Eventos - Como comprar ingresso?",
            buildFaqDoubtSubject(section.title, question.question),
        )
        assertEquals(
            listOf(
                "Origem: FAQ USC",
                "Seção: Eventos",
                "Pergunta: Como comprar ingresso?",
                "ID da pergunta: q1",
                "",
                "Dúvida enviada:",
                "Não achei o lote.",
            ).joinToString("\n"),
            buildFaqDoubtMessage(section.title, question, "  Não achei o lote.  "),
        )
    }

    @Test
    fun `icone desconhecido cai no inicio`() {
        assertEquals(PlatformFaqIcon.Start, PlatformFaqIcon.fromRemote("inexistente"))
        assertEquals(PlatformFaqIcon.Training, PlatformFaqIcon.fromRemote("training"))
    }

    private fun question(id: String, text: String, answer: String) = PlatformFaqQuestion(
        id = id,
        question = text,
        answer = answer,
    )
}
