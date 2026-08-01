package com.example.usc1.core.ui

import androidx.annotation.DrawableRes
import com.example.usc1.R

/**
 * Espelha `web-reference/src/constants/turmaImages.ts`.
 * Normaliza "t8", "T8", "Turma 8", "turma-8" para o id canonico "T8".
 */
object TurmaVisuals {
    fun normalizeTurmaId(value: String?): String {
        val clean = value.orEmpty().trim()
        if (clean.isBlank()) return ""
        val digits = Regex("\\d+").find(clean)?.value.orEmpty()
        if (digits.isBlank()) return ""
        val number = digits.toIntOrNull() ?: return ""
        if (number !in 1..12) return ""
        return "T$number"
    }

    @DrawableRes
    fun photoDrawable(turma: String?): Int = when (normalizeTurmaId(turma)) {
        "T1" -> R.drawable.turma1
        "T2" -> R.drawable.turma2
        "T3" -> R.drawable.turma3
        "T4" -> R.drawable.turma4
        "T5" -> R.drawable.turma5
        "T6" -> R.drawable.turma6
        "T7" -> R.drawable.turma7
        "T8" -> R.drawable.turma8
        "T9" -> R.drawable.turma9
        else -> R.drawable.turma8
    }

    @DrawableRes
    fun coverDrawable(turma: String?): Int = when (normalizeTurmaId(turma)) {
        "T1" -> R.drawable.capa_t1
        "T2" -> R.drawable.capa_t2
        "T3" -> R.drawable.capa_t3
        "T4" -> R.drawable.capa_t4
        "T5" -> R.drawable.capa_t5
        "T6" -> R.drawable.capa_t6
        "T7" -> R.drawable.capa_t7
        "T8" -> R.drawable.capa_t8
        "T9" -> R.drawable.capa_t9
        else -> R.drawable.capa_t8
    }

    fun label(turma: String?): String {
        val id = normalizeTurmaId(turma)
        return if (id.isBlank()) turma.orEmpty().trim().ifBlank { "Sem Turma" } else id
    }
}
