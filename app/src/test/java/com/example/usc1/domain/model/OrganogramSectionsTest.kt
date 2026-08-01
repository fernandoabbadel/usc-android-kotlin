package com.example.usc1.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regras de `/historico/organograma` (`organogramService.ts` + `groupedMembers`). */
class OrganogramSectionsTest {

    @Test
    fun `pendente e recusado nao sao publicados`() {
        assertFalse(member(status = OrganogramMemberStatus.Pending).isPublished)
        assertFalse(member(status = OrganogramMemberStatus.Rejected).isPublished)
        assertTrue(member(status = OrganogramMemberStatus.Approved).isPublished)
        assertTrue(member(status = OrganogramMemberStatus.Unset).isPublished)
    }

    @Test
    fun `secoes seguem a ordem configurada e as novas vao para o fim`() {
        val config = OrganogramConfig(
            sectionOrder = listOf("Presidência", "Diretoria"),
        )
        val members = listOf(
            display(member(id = "m1", section = "Marketing")),
            display(member(id = "m2", section = "Diretoria")),
            display(member(id = "m3", section = "Presidência")),
        )

        val sections = buildOrganogramSections(config, members)

        assertEquals(listOf("Presidência", "Diretoria", "Marketing"), sections.map { it.name })
    }

    @Test
    fun `membros da secao saem por ordem e depois por cargo`() {
        val config = OrganogramConfig(sectionOrder = listOf("Diretoria"))
        val members = listOf(
            display(member(id = "m1", section = "Diretoria", role = "Vice", order = 2)),
            display(member(id = "m2", section = "Diretoria", role = "Tesouraria", order = 1)),
            display(member(id = "m3", section = "Diretoria", role = "Secretaria", order = 1)),
        )

        val sections = buildOrganogramSections(config, members)

        assertEquals(
            listOf("m3", "m2", "m1"),
            sections.single().members.map { it.member.id },
        )
    }

    @Test
    fun `secao sem membro publicado some da pagina`() {
        val config = OrganogramConfig(sectionOrder = listOf("Presidência", "Diretoria"))
        val members = listOf(display(member(id = "m1", section = "Diretoria")))

        assertEquals(listOf("Diretoria"), buildOrganogramSections(config, members).map { it.name })
    }

    @Test
    fun `nome de secao vazio vira Diretoria`() {
        assertEquals("Diretoria", TenantHistoryCatalog.normalizeSectionName("   "))
        assertEquals("Marketing Digital", TenantHistoryCatalog.normalizeSectionName(" Marketing   Digital "))
    }

    private fun member(
        id: String = "m1",
        section: String = "Diretoria",
        role: String = "Membro",
        order: Int = 0,
        status: OrganogramMemberStatus = OrganogramMemberStatus.Approved,
    ) = OrganogramMember(
        id = id,
        section = section,
        role = role,
        order = order,
        status = status,
    )

    private fun display(member: OrganogramMember) = OrganogramDisplayMember(
        member = member,
        displayName = member.id,
        displayPhotoUrl = null,
        displayDetail = "",
        hasCanonicalVisual = false,
    )
}
