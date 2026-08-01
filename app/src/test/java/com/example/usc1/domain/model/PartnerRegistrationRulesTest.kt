package com.example.usc1.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** `validateStep2` e as máscaras de `/empresa/cadastro`. */
class PartnerRegistrationRulesTest {

    private val validForm = PartnerLeadForm(
        name = "Bar do Zé",
        cnpj = "12.345.678/0001-90",
        responsible = "José da Silva",
        cpf = "123.456.789-09",
        email = "contato@bardoze.com",
        phone = "55 (12) 99999-8888",
        password = "senha1234",
        passwordConfirmation = "senha1234",
    )

    @Test
    fun `formulario completo passa na validacao`() {
        assertNull(PartnerRegistrationRules.validate(validForm))
    }

    @Test
    fun `cnpj precisa de 14 digitos`() {
        assertEquals(
            "CNPJ inválido (14 dígitos).",
            PartnerRegistrationRules.validate(validForm.copy(cnpj = "12.345.678/0001")),
        )
    }

    @Test
    fun `cpf precisa de 11 digitos`() {
        assertEquals(
            "CPF inválido (11 dígitos).",
            PartnerRegistrationRules.validate(validForm.copy(cpf = "123.456.789")),
        )
    }

    @Test
    fun `email precisa ser valido e conter ponto com`() {
        assertEquals(
            "Email inválido.",
            PartnerRegistrationRules.validate(validForm.copy(email = "contato@bardoze")),
        )
        assertEquals(
            "Email inválido.",
            PartnerRegistrationRules.validate(validForm.copy(email = "contato@bardoze.br")),
        )
    }

    @Test
    fun `telefone exige 55 mais ddd mais numero`() {
        assertEquals(
            "Telefone inválido (use 55 + DDD + número).",
            PartnerRegistrationRules.validate(validForm.copy(phone = "(12) 99999-8888")),
        )
    }

    @Test
    fun `senha exige oito caracteres e confirmacao igual`() {
        assertEquals(
            "A senha deve ter no mínimo 8 caracteres.",
            PartnerRegistrationRules.validate(
                validForm.copy(password = "1234567", passwordConfirmation = "1234567"),
            ),
        )
        assertEquals(
            "As senhas não conferem.",
            PartnerRegistrationRules.validate(validForm.copy(passwordConfirmation = "outra1234")),
        )
    }

    @Test
    fun `mascaras formatam como no web`() {
        assertEquals("12.345.678/0001-90", PartnerRegistrationRules.formatCnpj("12345678000190"))
        assertEquals("123.456.789-09", PartnerRegistrationRules.formatCpf("12345678909"))
        assertEquals("55 (12) 99999-8888", PartnerRegistrationRules.formatPhone("5512999998888"))
        assertEquals("55 (12) 9999-8888", PartnerRegistrationRules.formatPhone("551299998888"))
    }

    @Test
    fun `mascara descarta caracteres nao numericos e excedentes`() {
        assertEquals("12.345.678/0001-90", PartnerRegistrationRules.formatCnpj("12a345b678c0001d90e99"))
        assertEquals("123.456", PartnerRegistrationRules.formatCpf("123456"))
    }
}
