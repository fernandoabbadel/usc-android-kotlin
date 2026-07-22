package com.example.usc1.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AdminMentorshipLabelsConfig(
    val hubTitle: String = "Apadrinhamento",
    val mentorLabel: String = "Padrinho/Madrinha",
    val menteeLabel: String = "Afilhado/Afilhada",
    val inviteMentorLabel: String = "Adicionar como meu padrinho/madrinha",
    val inviteMenteeLabel: String = "Adicionar como meu afilhado/afilhada",
    val requestHelpText: String = "Cada perfil pode ter 1 padrinho/madrinha e 1 afilhado/afilhada por atlética.",
)

object AdminMentorshipCatalog {
    const val LabelsDocId = "mentorship_labels"
    const val MaxHubTitleLength = 60
    const val MaxRoleLabelLength = 80
    const val MaxInviteLabelLength = 120
    const val MaxHelpTextLength = 220

    fun normalize(config: AdminMentorshipLabelsConfig): AdminMentorshipLabelsConfig {
        val fallback = AdminMentorshipLabelsConfig()
        return AdminMentorshipLabelsConfig(
            hubTitle = config.hubTitle.trim().take(MaxHubTitleLength).ifBlank { fallback.hubTitle },
            mentorLabel = config.mentorLabel.trim().take(MaxRoleLabelLength).ifBlank { fallback.mentorLabel },
            menteeLabel = config.menteeLabel.trim().take(MaxRoleLabelLength).ifBlank { fallback.menteeLabel },
            inviteMentorLabel = config.inviteMentorLabel.trim().take(MaxInviteLabelLength)
                .ifBlank { fallback.inviteMentorLabel },
            inviteMenteeLabel = config.inviteMenteeLabel.trim().take(MaxInviteLabelLength)
                .ifBlank { fallback.inviteMenteeLabel },
            requestHelpText = config.requestHelpText.trim().take(MaxHelpTextLength)
                .ifBlank { fallback.requestHelpText },
        )
    }
}
