package com.example.usc1.domain.repository

import com.example.usc1.ui.collectives.CollectiveAreaUiConfig
import com.example.usc1.ui.collectives.CollectiveGroup
import com.example.usc1.ui.collectives.CollectiveInteractionState
import com.example.usc1.ui.collectives.CollectiveKind
import com.example.usc1.ui.collectives.CollectiveLikeResult
import com.example.usc1.ui.collectives.CollectiveSellerStats
import com.example.usc1.ui.collectives.CollectiveStoreState

/**
 * Área pública dos coletivos (ligas, comissões e diretório).
 *
 * Espelha `web-reference/src/lib/leaguesService.ts`, `collectiveAreaUiService.ts`,
 * `ligasUscUiService.ts`, `storePublicService.ts` e `turmasService.ts`.
 */
interface CollectivesRepository {
    /** `fetchLigasUscUiConfig` / `fetchCollectiveAreaUiConfig`. */
    suspend fun getAreaUiConfig(tenantId: String, kind: CollectiveKind): CollectiveAreaUiConfig

    /** `fetchLeagueSummaries` filtrado por categoria. */
    suspend fun getCollectives(tenantId: String, kind: CollectiveKind): List<CollectiveGroup>

    /** `fetchPrimaryLeagueRecord`: usado pela raiz `/diretorio`. */
    suspend fun getPrimaryCollective(tenantId: String, kind: CollectiveKind): CollectiveGroup?

    /** `fetchLeagueById` + checagem de categoria da área. */
    suspend fun getCollective(tenantId: String, kind: CollectiveKind, collectiveId: String): CollectiveGroup?

    /** `fetchUserLeagueInteractionState` em `users.extra`. */
    suspend fun getInteractionState(tenantId: String, userId: String): CollectiveInteractionState

    /** `toggleUserLeagueLike`: grava `users.extra` e ajusta `ligas_config.likes`. */
    suspend fun toggleLike(tenantId: String, userId: String, collectiveId: String): CollectiveLikeResult

    /** `toggleUserLeagueFollow`: grava `users.extra`. */
    suspend fun toggleFollow(tenantId: String, userId: String, collectiveId: String): List<String>

    /** `fetchStoreCategories` + `fetchStoreProductsBySeller` para a aba Loja. */
    suspend fun getStore(tenantId: String, collectiveId: String, loadProducts: Boolean): CollectiveStoreState

    /** `fetchTurmaMemberCounts`. */
    suspend fun getTurmaMemberCounts(tenantId: String, turmaIds: List<String>): Map<String, Int>

    /** `fetchLeagueUsers` filtrado pela turma da comissão. */
    suspend fun getTurmaMemberIds(tenantId: String, turmaId: String): List<String>

    /** `fetchStoreProductStatsBySellers`: ordena o catálogo de comissões. */
    suspend fun getSellerStats(tenantId: String, collectiveIds: List<String>): Map<String, CollectiveSellerStats>

    /** `addLeagueQuizHistory`: grava o resultado do Oráculo em `quiz_history`. */
    suspend fun addQuizHistory(userId: String, topMatch: String, keywords: List<String>)
}
