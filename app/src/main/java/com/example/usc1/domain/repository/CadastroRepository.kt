package com.example.usc1.domain.repository

import com.example.usc1.domain.model.CadastroConfig
import com.example.usc1.domain.model.CadastroForm

interface CadastroRepository {
    /** Carrega config do cadastro (turmas + opcoes) e o formulario ja preenchido do usuario. */
    suspend fun loadCadastro(tenantId: String, userId: String): CadastroBundle

    /** Grava o formulario na tabela `users`. */
    suspend fun saveCadastro(tenantId: String, userId: String, form: CadastroForm)
}

data class CadastroBundle(
    val config: CadastroConfig,
    val form: CadastroForm,
    val isExistingProfile: Boolean = false,
)
