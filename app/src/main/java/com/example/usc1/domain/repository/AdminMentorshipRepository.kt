package com.example.usc1.domain.repository

import com.example.usc1.domain.model.AdminMentorshipLabelsConfig

interface AdminMentorshipRepository {
    suspend fun fetchMentorshipLabels(forceRefresh: Boolean = false): AdminMentorshipLabelsConfig
    suspend fun saveMentorshipLabels(config: AdminMentorshipLabelsConfig): AdminMentorshipLabelsConfig
}
