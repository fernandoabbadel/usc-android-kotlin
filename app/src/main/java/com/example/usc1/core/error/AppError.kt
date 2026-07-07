package com.example.usc1.core.error

sealed interface AppError {
    val message: String

    data class Network(override val message: String) : AppError
    data class Unauthorized(override val message: String = "Sessão inválida.") : AppError
    data class Forbidden(override val message: String = "Você não tem permissão para esta ação.") : AppError
    data class NotFound(override val message: String = "Registro não encontrado.") : AppError
    data class Validation(override val message: String) : AppError
    data class Unknown(override val message: String = "Erro inesperado.") : AppError
}
