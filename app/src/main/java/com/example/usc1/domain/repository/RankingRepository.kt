package com.example.usc1.domain.repository

import com.example.usc1.domain.model.RankingUser

interface RankingRepository {
    /** `fetchGlobalRankingUsers` do web. */
    suspend fun getGlobalRanking(tenantId: String, limit: Int): List<RankingUser>

    /** `fetchTurmaRankingUsers` do web. */
    suspend fun getClassRanking(tenantId: String, className: String, limit: Int): List<RankingUser>
}
