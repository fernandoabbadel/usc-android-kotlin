package com.example.usc1.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/** Regra do `turmasMap` da aba "Por Turma" em `/ranking`. */
class RankingAggregationTest {

    @Test
    fun `turmas somam xp e contam membros ordenando por pontos`() {
        val users = listOf(
            user("a", className = "T1", xp = 120),
            user("b", className = "t1", xp = 80),
            user("c", className = "T2", xp = 300),
            user("d", className = "", xp = 10),
        )

        val classes = RankingCatalog.aggregateClasses(users)

        assertEquals(listOf("T2", "T1", "GERAL"), classes.map { it.id })
        assertEquals(300, classes[0].points)
        assertEquals(1, classes[0].members)
        assertEquals(200, classes[1].points)
        assertEquals(2, classes[1].members)
        assertEquals("GERAL", classes[2].name)
    }

    @Test
    fun `podio usa apelido e cai para o primeiro nome`() {
        assertEquals("Fê", user("a", nickname = "Fê", name = "Fernando Abbade").podiumLabel)
        assertEquals("Fernando", user("a", name = "Fernando Abbade").podiumLabel)
    }

    @Test
    fun `nome sem espaco continua inteiro no podio`() {
        assertEquals("Ana", user("a", name = "Ana").podiumLabel)
    }

    private fun user(
        id: String,
        name: String = "Atleta",
        nickname: String = "",
        className: String = "T1",
        xp: Int = 0,
    ) = RankingUser(
        id = id,
        name = name,
        nickname = nickname,
        className = className,
        xp = xp,
    )
}
