package com.example.usc1.core.result

import com.example.usc1.core.error.AppError

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}
