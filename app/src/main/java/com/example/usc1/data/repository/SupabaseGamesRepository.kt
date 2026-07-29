package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.domain.repository.GamesRepository
import com.example.usc1.ui.games.Achievement
import com.example.usc1.ui.games.GamesMockData
import com.example.usc1.ui.games.GamesUiState
import com.example.usc1.ui.games.LoyaltyReward
import com.example.usc1.ui.games.RankingEntry
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.text.NumberFormat
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SupabaseGamesRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : GamesRepository {
    override suspend fun getGamesHub(
        tenantId: String,
        userId: String,
        userName: String,
    ): GamesUiState = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanUserId = userId.trim()
        if (!SupabaseClientProvider.config.isConfigured || cleanTenantId.isBlank()) {
            return@withContext GamesUiState(
                rankings = GamesMockData.rankings,
                achievements = GamesMockData.achievements,
                rewards = GamesMockData.rewards,
                errorMessage = "Supabase não configurado para carregar a arena.",
            )
        }

        val client = clientProvider()
        val users = fetchUsers(client, cleanTenantId)
        val currentUser = users.firstOrNull { it.uid == cleanUserId }
            ?: users.firstOrNull { it.nome.equals(userName, ignoreCase = true) }
        val userXp = currentUser?.xp ?: 0
        val achievementConfigs = fetchAchievements(client, cleanTenantId)
        val achievementLogs = if (cleanUserId.isBlank()) emptyList() else fetchAchievementLogs(client, cleanTenantId, cleanUserId)
        val patents = fetchPatents(client, cleanTenantId)
        val matches = if (cleanUserId.isBlank()) emptyList() else fetchArenaMatches(client, cleanTenantId, cleanUserId)
        val levelLabel = firstNotBlank(
            currentUser?.patente,
            patents.lastOrNull { it.minXp <= userXp }?.titulo,
            "Cardume",
        )

        GamesUiState(
            xpLabel = formatXp(userXp),
            levelLabel = levelLabel,
            rankings = users.take(10).mapIndexed { index, row -> row.toRankingEntry(index) }
                .ifEmpty { GamesMockData.rankings },
            achievements = buildAchievements(achievementConfigs, achievementLogs, userXp),
            rewards = buildRewards(patents, userXp),
            matchesLabel = matches.size.toString(),
            winsLabel = matches.count { it.isWin }.toString(),
            streakLabel = "${matches.currentWinStreak()} vitórias",
            isLoading = false,
            errorMessage = null,
        )
    }

    private suspend fun fetchUsers(
        client: SupabaseClient,
        tenantId: String,
    ): List<GameUserRow> {
        return client.from(UsersTable)
            .select(columns = Columns.raw(UserColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                }
                order(column = "xp", order = Order.DESCENDING)
                limit(count = MaxUsers.toLong())
            }
            .decodeList<GameUserRow>()
    }

    private suspend fun fetchAchievements(
        client: SupabaseClient,
        tenantId: String,
    ): List<AchievementConfigRow> {
        return client.from(AchievementsTable)
            .select(columns = Columns.raw(AchievementColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("active", true)
                }
                order(column = "xp", order = Order.ASCENDING)
                limit(count = MaxAchievements.toLong())
            }
            .decodeList<AchievementConfigRow>()
    }

    private suspend fun fetchAchievementLogs(
        client: SupabaseClient,
        tenantId: String,
        userId: String,
    ): List<AchievementLogRow> {
        return client.from(AchievementLogsTable)
            .select(columns = Columns.raw(AchievementLogColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("userId", userId)
                }
                order(column = "timestamp", order = Order.DESCENDING)
                limit(count = MaxLogs.toLong())
            }
            .decodeList<AchievementLogRow>()
    }

    private suspend fun fetchPatents(
        client: SupabaseClient,
        tenantId: String,
    ): List<PatentConfigRow> {
        return client.from(PatentsTable)
            .select(columns = Columns.raw(PatentColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                }
                order(column = "minXp", order = Order.ASCENDING)
                limit(count = MaxPatents.toLong())
            }
            .decodeList<PatentConfigRow>()
    }

    private suspend fun fetchArenaMatches(
        client: SupabaseClient,
        tenantId: String,
        userId: String,
    ): List<ArenaMatchRow> {
        return client.from(ArenaMatchesTable)
            .select(columns = Columns.raw(ArenaMatchColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("userId", userId)
                }
                order(column = "date", order = Order.DESCENDING)
                limit(count = MaxMatches.toLong())
            }
            .decodeList<ArenaMatchRow>()
    }

    private fun GameUserRow.toRankingEntry(index: Int): RankingEntry {
        return RankingEntry(
            name = nome.trim().ifBlank { "Atleta USC" },
            score = formatXp(xp),
            subtitle = firstNotBlank(
                turma?.let { "Turma $it" },
                patente,
                "${index + 1}º no ranking",
            ),
        )
    }

    private fun buildAchievements(
        configs: List<AchievementConfigRow>,
        logs: List<AchievementLogRow>,
        userXp: Int,
    ): List<Achievement> {
        val unlockedKeys = logs.flatMap { row ->
            listOfNotNull(
                row.achievementId?.normalizedKey(),
                row.achievementTitle.normalizedKey(),
            )
        }.toSet()

        val fromConfig = configs.map { config ->
            val unlocked = config.id.normalizedKey() in unlockedKeys ||
                config.titulo.normalizedKey() in unlockedKeys
            Achievement(
                title = config.titulo.trim().ifBlank { "Conquista" },
                description = config.desc.trim().ifBlank { "${config.xp} XP ao concluir ${config.statKey}." },
                progress = when {
                    unlocked -> 1f
                    config.target > 0 -> (userXp.toFloat() / config.target.toFloat()).coerceIn(0f, 1f)
                    else -> 0f
                },
                unlocked = unlocked,
            )
        }

        if (fromConfig.isNotEmpty()) return fromConfig
        val fromLogs = logs.map { log ->
            Achievement(
                title = log.achievementTitle.trim().ifBlank { "Conquista registrada" },
                description = "${formatXp(log.xp)} registrados em ${log.timestamp.formatDateLabel()}.",
                progress = 1f,
                unlocked = true,
            )
        }
        return fromLogs.ifEmpty { GamesMockData.achievements }
    }

    private fun buildRewards(
        patents: List<PatentConfigRow>,
        userXp: Int,
    ): List<LoyaltyReward> {
        return patents.map { patent ->
            val minXp = patent.minXp.coerceAtLeast(1)
            LoyaltyReward(
                title = patent.titulo.trim().ifBlank { "Patente" },
                costLabel = formatXp(minXp),
                progress = (userXp.toFloat() / minXp.toFloat()).coerceIn(0f, 1f),
            )
        }.ifEmpty { GamesMockData.rewards }
    }

    private val ArenaMatchRow.isWin: Boolean
        get() {
            val clean = result.orEmpty().normalizedKey()
            return clean.contains("win") ||
                clean.contains("vitoria") ||
                clean.contains("acerto") ||
                clean.contains("correct") ||
                clean == "won"
        }

    private fun List<ArenaMatchRow>.currentWinStreak(): Int {
        return sortedByDescending { it.date.toLocalDateOrNull() ?: LocalDate.MIN }
            .takeWhile { it.isWin }
            .size
    }

    private fun String.formatDateLabel(): String {
        val parsed = toLocalDateOrNull() ?: return take(10).ifBlank { "data recente" }
        return DateLabelFormatter.format(parsed)
    }

    private fun String.toLocalDateOrNull(): LocalDate? {
        val clean = trim()
        if (clean.isBlank()) return null
        return runCatching { OffsetDateTime.parse(clean).atZoneSameInstant(Zone).toLocalDate() }
            .getOrElse { runCatching { LocalDate.parse(clean.take(10)) }.getOrNull() }
    }

    private fun String.normalizedKey(): String = trim()
        .lowercase(Locale.ROOT)
        .normalizeAscii()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')

    private fun String.normalizeAscii(): String {
        val normalized = java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{Mn}+"), "")
    }

    private fun formatXp(value: Int): String = "${IntegerFormatter.format(value)} XP"

    private fun firstNotBlank(vararg values: String?): String {
        return values.firstNotNullOfOrNull { it?.trim()?.takeIf(String::isNotBlank) }.orEmpty()
    }

    private companion object {
        const val UsersTable = "users"
        const val AchievementsTable = "achievements_config"
        const val AchievementLogsTable = "achievements_logs"
        const val PatentsTable = "patentes_config"
        const val ArenaMatchesTable = "arena_matches"
        const val MaxUsers = 30
        const val MaxAchievements = 80
        const val MaxLogs = 80
        const val MaxPatents = 40
        const val MaxMatches = 120
        const val UserColumns = "uid,nome,turma,xp,patente,tenant_id"
        const val AchievementColumns = "id,titulo,desc,cat,iconName,statKey,target,xp,active,repeatable,tenant_id"
        const val AchievementLogColumns = "id,achievementId,achievementTitle,timestamp,userId,userName,xp,tenant_id"
        const val PatentColumns = "id,titulo,minXp,cor,iconName,text,tenant_id"
        const val ArenaMatchColumns = "id,userId,game,result,date,tenant_id"
        val PtBr: Locale = Locale.forLanguageTag("pt-BR")
        val Zone: ZoneId = ZoneId.of("America/Sao_Paulo")
        val IntegerFormatter: NumberFormat = NumberFormat.getIntegerInstance(PtBr)
        val DateLabelFormatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM", PtBr)
    }
}

@Serializable
private data class GameUserRow(
    val uid: String = "",
    val nome: String = "",
    val turma: String? = null,
    val xp: Int = 0,
    val patente: String? = null,
    @SerialName("tenant_id") val tenantId: String? = null,
)

@Serializable
private data class AchievementConfigRow(
    val id: String = "",
    val titulo: String = "",
    val desc: String = "",
    val cat: String = "",
    val iconName: String = "",
    val statKey: String = "",
    val target: Int = 0,
    val xp: Int = 0,
    val active: Boolean = true,
    val repeatable: Boolean = false,
    @SerialName("tenant_id") val tenantId: String? = null,
)

@Serializable
private data class AchievementLogRow(
    val id: String = "",
    val achievementId: String? = null,
    val achievementTitle: String = "",
    val timestamp: String = "",
    val userId: String? = null,
    val userName: String = "",
    val xp: Int = 0,
    @SerialName("tenant_id") val tenantId: String? = null,
)

@Serializable
private data class PatentConfigRow(
    val id: String = "",
    val titulo: String = "",
    val minXp: Int = 0,
    val cor: String = "",
    val iconName: String = "",
    val text: String? = null,
    @SerialName("tenant_id") val tenantId: String? = null,
)

@Serializable
private data class ArenaMatchRow(
    val id: String = "",
    val userId: String = "",
    val game: String? = null,
    val result: String? = null,
    val date: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
)
