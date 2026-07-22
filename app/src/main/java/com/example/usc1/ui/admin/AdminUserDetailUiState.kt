package com.example.usc1.ui.admin

import com.example.usc1.domain.model.AdminUserPlan
import com.example.usc1.domain.model.AdminUserProfile
import com.example.usc1.domain.model.AdminUserStatus
import com.example.usc1.domain.model.AdminUserUpdate

data class AdminUserDetailUiState(
    val userId: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isChangingStatus: Boolean = false,
    val isDeleting: Boolean = false,
    val shouldNavigateBack: Boolean = false,
    val profile: AdminUserProfile? = null,
    val form: AdminUserDetailForm? = null,
    val showDeleteConfirmation: Boolean = false,
    val actionMessage: String? = null,
    val errorMessage: String? = null,
)

data class AdminUserDetailForm(
    val nome: String = "",
    val telefone: String = "",
    val matricula: String = "",
    val turma: String = "",
    val status: AdminUserStatus = AdminUserStatus.Pendente,
    val plano: AdminUserPlan = AdminUserPlan.Bicho,
) {
    fun toUpdate(userId: String): AdminUserUpdate {
        return AdminUserUpdate(
            userId = userId,
            nome = nome,
            telefone = telefone,
            matricula = matricula,
            turma = turma,
            status = status,
            plano = plano,
        )
    }
}

fun AdminUserProfile.toDetailForm(): AdminUserDetailForm {
    return AdminUserDetailForm(
        nome = nome,
        telefone = telefone,
        matricula = matricula,
        turma = turma,
        status = status,
        plano = tier,
    )
}
