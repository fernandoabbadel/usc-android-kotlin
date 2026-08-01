package com.example.usc1.domain.model

import com.example.usc1.ui.collectives.CollectiveAreaUiConfig
import com.example.usc1.ui.collectives.CollectiveDetailUiState
import com.example.usc1.ui.collectives.CollectiveEvent
import com.example.usc1.ui.collectives.CollectiveEventVisibility
import com.example.usc1.ui.collectives.CollectiveGroup
import com.example.usc1.ui.collectives.CollectiveKind
import com.example.usc1.ui.collectives.CollectiveMember
import com.example.usc1.ui.collectives.CollectiveTab
import com.example.usc1.ui.collectives.CollectiveTextUtils
import com.example.usc1.ui.collectives.LeagueQuizCatalog
import com.example.usc1.ui.collectives.LeagueQuizEngine
import com.example.usc1.ui.collectives.LeagueQuizQuestionKey
import com.example.usc1.ui.collectives.LeagueRoleCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regras copiadas de `/ligas_usc`, `/comissoes` e `/diretorio` do web
 * (`leaguesService.ts`, `leagueRoles.ts`, `CollectivePublicDetailClient.tsx`).
 */
class CollectivesM6RulesTest {

    @Test
    fun `cargos sao normalizados e ordenados pela hierarquia do web`() {
        assertEquals("Presidente", LeagueRoleCatalog.resolveRoleLabel(" PRESIDENTE "))
        assertEquals("Vice-Presidente", LeagueRoleCatalog.resolveRoleLabel("vice presidente"))
        assertEquals("Secretaria", LeagueRoleCatalog.resolveRoleLabel("secretário"))
        assertEquals("Tesouraria", LeagueRoleCatalog.resolveRoleLabel("tesoureiro"))
        assertEquals("Diretoria", LeagueRoleCatalog.resolveRoleLabel("diretor de eventos"))
        assertEquals("Membro", LeagueRoleCatalog.resolveRoleLabel(""))
        // Igual ao web: cargo livre que não casa com nenhum prefixo é preservado.
        assertEquals("presidencia", LeagueRoleCatalog.resolveRoleLabel("presidencia "))

        val sorted = LeagueRoleCatalog.sortMembersByRole(
            listOf(
                member("u1", "Zeca", "Membro"),
                member("u2", "Ana", "Diretoria"),
                member("u3", "Bruno", "Presidente"),
                member("u4", "Alice", "Diretoria"),
            ),
        )

        assertEquals(listOf("Bruno", "Alice", "Ana", "Zeca"), sorted.map { it.name })
    }

    @Test
    fun `apenas cargos de gestao podem configurar a pagina`() {
        assertTrue(LeagueRoleCatalog.canManageRole("Presidente"))
        assertTrue(LeagueRoleCatalog.canManageRole("Tesouraria"))
        assertFalse(LeagueRoleCatalog.canManageRole("Membro"))
    }

    @Test
    fun `comissao publica so a diretoria e liga publica todos os membros`() {
        val members = listOf(
            member("u1", "Ana", "Presidente"),
            member("u2", "Bruno", "Membro"),
        )

        val commission = group(kind = CollectiveKind.Commission).copy(members = members)
        val league = group(kind = CollectiveKind.League).copy(members = members)

        assertEquals(listOf("Ana"), commission.publicMembers.map { it.name })
        assertEquals(listOf("Ana", "Bruno"), league.publicMembers.map { it.name })
    }

    @Test
    fun `evento interno so aparece para membro oficial`() {
        val group = group(kind = CollectiveKind.League).copy(
            members = listOf(member("u1", "Ana", "Membro")),
            events = listOf(
                event("e1", "Aulão aberto", CollectiveEventVisibility.Public),
                event("e2", "Reunião fechada", CollectiveEventVisibility.Internal),
            ),
        )

        val visitor = detailState(group).copy(userId = "u9")
        val official = detailState(group).copy(userId = "u1")

        assertEquals(listOf("Aulão aberto"), visitor.publicAgendaEvents.map { it.title })
        assertTrue(visitor.internalAgendaEvents.isEmpty())
        assertEquals(1, visitor.visibleAgendaCount)

        assertEquals(listOf("Reunião fechada"), official.internalAgendaEvents.map { it.title })
        assertEquals(2, official.visibleAgendaCount)
    }

    @Test
    fun `membro da turma conta como oficial na comissao`() {
        val group = group(kind = CollectiveKind.Commission).copy(turmaId = "T3")

        val byUserTurma = detailState(group, CollectiveKind.Commission).copy(userId = "u9", userTurma = "t3")
        val byTurmaRoster = detailState(group, CollectiveKind.Commission)
            .copy(userId = "u9", turmaMemberIds = listOf("u9"))
        val outsider = detailState(group, CollectiveKind.Commission).copy(userId = "u9", userTurma = "T1")

        assertTrue(byUserTurma.isOfficialMember)
        assertTrue(byTurmaRoster.isOfficialMember)
        assertFalse(outsider.isOfficialMember)
    }

    @Test
    fun `gestao da pagina segue managerUserIds, cargo de gestao e master`() {
        val group = group(kind = CollectiveKind.Directory).copy(
            members = listOf(member("u1", "Ana", "Tesouraria"), member("u2", "Bruno", "Membro")),
            managerUserIds = listOf("u5"),
        )

        assertTrue(detailState(group, CollectiveKind.Directory).copy(userId = "u1").canManagePage)
        assertTrue(detailState(group, CollectiveKind.Directory).copy(userId = "u5").canManagePage)
        assertTrue(
            detailState(group, CollectiveKind.Directory).copy(userId = "u9", isPlatformMaster = true).canManagePage,
        )
        assertFalse(detailState(group, CollectiveKind.Directory).copy(userId = "u2").canManagePage)
    }

    @Test
    fun `comissao so oferece cargos de gestao na solicitacao`() {
        val commission = detailState(group(kind = CollectiveKind.Commission), CollectiveKind.Commission)
        val league = detailState(group(kind = CollectiveKind.League))

        assertEquals(LeagueRoleCatalog.managementRoleOptions, commission.requestRoleOptions)
        assertEquals(LeagueRoleCatalog.roleOptions, league.requestRoleOptions)
        assertFalse(commission.requestRoleOptions.contains("Membro"))
    }

    @Test
    fun `contagem de membros da comissao usa a turma fora da aba de membros`() {
        val group = group(kind = CollectiveKind.Commission).copy(
            turmaId = "T2",
            membersCount = 4,
            members = listOf(member("u1", "Ana", "Presidente"), member("u2", "Bruno", "Membro")),
        )

        val overview = detailState(group, CollectiveKind.Commission).copy(turmaMemberCount = 37)
        val membersTab = overview.copy(tab = CollectiveTab.Members)

        assertEquals(37, overview.displayMembersCount)
        // Na aba de membros o web mostra o tamanho da diretoria publicada.
        assertEquals(1, membersTab.displayMembersCount)
    }

    @Test
    fun `visao geral e quebrada em linhas nao vazias`() {
        val group = group(kind = CollectiveKind.League).copy(overview = "Primeira linha\n\n  Segunda linha  \n")
        assertEquals(listOf("Primeira linha", "Segunda linha"), group.overviewHighlights)
    }

    @Test
    fun `nome longo do card e truncado no limite do web`() {
        val short = "Liga Academica de Cardiologia"
        assertEquals(short, CollectiveTextUtils.clampCardName(short))

        val long = "Liga Academica de Gastroenterologia e Cirurgia Digestiva"
        val clamped = CollectiveTextUtils.clampCardName(long)
        assertTrue(clamped.endsWith("..."))
        assertTrue(clamped.length <= CollectiveTextUtils.LeagueNameMaxLength + 3)
    }

    @Test
    fun `oraculo prioriza a liga com perfil compativel`() {
        val cardio = group(id = "cardio", name = "Liga Academica de Cardiologia").copy(acronym = "LACARDIO")
        val legal = group(id = "legal", name = "Liga Academica de Medicina Legal de Caraguatatuba")
            .copy(acronym = "LAMELC")

        val answers = mapOf(
            LeagueQuizQuestionKey.Scenario to listOf("Consultório"),
            LeagueQuizQuestionKey.System to listOf("Coração"),
            LeagueQuizQuestionKey.Impact to listOf("Salvar vidas"),
        )
        val keywords = listOf("clinica", "cardio", "coracao", "emergencia")

        val matches = LeagueQuizEngine.calculateMatches(listOf(legal, cardio), answers, keywords)

        assertEquals("cardio", matches.first().collective.id)
        assertTrue(matches.first().matchPercent > 0)
        assertTrue(matches.first().matchScore >= matches.last().matchScore)
    }

    @Test
    fun `oraculo casa perfil por sigla e por alias`() {
        val bySigla = LeagueQuizEngine.resolveProfile(group().copy(acronym = "LANN"))
        val byAlias = LeagueQuizEngine.resolveProfile(
            group(name = "Liga de Otorrinolaringologia da USC").copy(acronym = ""),
        )

        assertEquals("Liga de Neurologia e Neurocirurgia", bySigla?.nome)
        assertEquals("Liga Academica de Otorrinolaringologia", byAlias?.nome)
    }

    @Test
    fun `oraculo tem as cinco perguntas do web`() {
        assertEquals(5, LeagueQuizCatalog.questions.size)
        assertEquals(
            listOf(
                LeagueQuizQuestionKey.Scenario,
                LeagueQuizQuestionKey.Audience,
                LeagueQuizQuestionKey.System,
                LeagueQuizQuestionKey.Style,
                LeagueQuizQuestionKey.Impact,
            ),
            LeagueQuizCatalog.questions.map { it.key },
        )
        LeagueQuizCatalog.questions.forEach { assertEquals(5, it.options.size) }
    }

    private fun member(id: String, name: String, role: String) =
        CollectiveMember(id = id, name = name, role = role)

    private fun event(id: String, title: String, visibility: CollectiveEventVisibility) =
        CollectiveEvent(id = id, title = title, date = "10/08/2026", visibility = visibility)

    private fun group(
        id: String = "liga-1",
        name: String = "Liga Academica de Teste",
        kind: CollectiveKind = CollectiveKind.League,
    ) = CollectiveGroup(id = id, name = name, kind = kind)

    private fun detailState(
        group: CollectiveGroup,
        kind: CollectiveKind = CollectiveKind.League,
    ) = CollectiveDetailUiState(
        kind = kind,
        tab = CollectiveTab.Overview,
        group = group,
        uiConfig = CollectiveAreaUiConfig.default(kind),
        isLoading = false,
    )
}
