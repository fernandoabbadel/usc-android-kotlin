package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.data.supabase.SupabaseTenantResolver
import com.example.usc1.domain.model.CadastroChoiceOption
import com.example.usc1.domain.model.CadastroColorOption
import com.example.usc1.domain.model.CadastroConfig
import com.example.usc1.domain.model.CadastroDefaults
import com.example.usc1.domain.model.CadastroForm
import com.example.usc1.domain.model.CadastroTurmaOption
import com.example.usc1.domain.repository.CadastroBundle
import com.example.usc1.domain.repository.CadastroRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

/**
 * Espelha `cadastroConfigService.ts` + a gravacao de perfil de
 * `web-reference/src/app/cadastro/page.tsx` (tabela `users`).
 */
class SupabaseCadastroRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : CadastroRepository {

    override suspend fun loadCadastro(
        tenantId: String,
        userId: String,
    ): CadastroBundle = withContext(Dispatchers.IO) {
        if (!SupabaseClientProvider.config.isConfigured) {
            throw IllegalStateException("Supabase não configurado para carregar o cadastro.")
        }
        val client = clientProvider()
        val cleanTenantId = tenantId.trim().ifBlank {
            runCatching { SupabaseTenantResolver.resolveActiveTenantId(client) }.getOrDefault("")
        }
        val cleanUserId = userId.trim()
        if (cleanUserId.isBlank()) {
            throw IllegalStateException("Entre com sua conta para abrir o cadastro.")
        }

        val config = runCatching { fetchConfig(client, cleanTenantId) }
            .getOrDefault(CadastroConfig(turmas = emptyList()))
        val turmas = runCatching { fetchTurmas(client, cleanTenantId) }.getOrDefault(emptyList())
        val userRow = runCatching { fetchUserRow(client, cleanTenantId, cleanUserId) }.getOrNull()

        CadastroBundle(
            config = config.copy(turmas = turmas.ifEmpty { config.turmas }),
            form = userRow?.toForm() ?: CadastroForm(),
            isExistingProfile = userRow != null,
        )
    }

    override suspend fun saveCadastro(
        tenantId: String,
        userId: String,
        form: CadastroForm,
    ): Unit = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val cleanTenantId = tenantId.trim().ifBlank {
            runCatching { SupabaseTenantResolver.resolveActiveTenantId(client) }.getOrDefault("")
        }
        val cleanUserId = userId.trim()
        if (cleanUserId.isBlank()) {
            throw IllegalStateException("Entre com sua conta para salvar o cadastro.")
        }
        if (form.nome.trim().isBlank()) {
            throw IllegalStateException("Informe seu nome completo.")
        }
        if (form.turma.trim().isBlank()) {
            throw IllegalStateException("Selecione sua turma.")
        }

        val payload = linkedMapOf<String, JsonElement>(
            "nome" to JsonPrimitive(form.nome.trim().take(MaxNameLength)),
            "apelido" to JsonPrimitive(form.apelido.trim().take(MaxShortLength)),
            "matricula" to JsonPrimitive(form.matricula.trim().take(MaxShortLength)),
            "turma" to JsonPrimitive(form.turma.trim()),
            "instagram" to JsonPrimitive(form.instagram.trim().removePrefix("@").take(MaxShortLength)),
            "instagramPublico" to JsonPrimitive(form.instagramPublico),
            "telefone" to JsonPrimitive(form.telefone.trim().take(MaxShortLength)),
            "whatsappPublico" to JsonPrimitive(form.whatsappPublico),
            "bio" to JsonPrimitive(form.bio.trim().take(MaxBioLength)),
            "dataNascimento" to JsonPrimitive(form.dataNascimento.trim()),
            "idadePublica" to JsonPrimitive(form.idadePublica),
            "cidadeOrigem" to JsonPrimitive(form.cidadeOrigem.trim().take(MaxShortLength)),
            "estadoOrigem" to JsonPrimitive(form.estadoOrigem.trim().take(8)),
            "statusRelacionamento" to JsonPrimitive(form.statusRelacionamento.trim()),
            "relacionamentoPublico" to JsonPrimitive(form.relacionamentoPublico),
            "signo" to JsonPrimitive(form.signo.trim()),
            "signoPublico" to JsonPrimitive(form.signoPublico),
            "ascendente" to JsonPrimitive(form.ascendente.trim()),
            "ascendentePublico" to JsonPrimitive(form.ascendentePublico),
            "lugarEspecial" to form.lugarEspecial.toJsonArray(),
            "comidaPreferida" to form.comidaPreferida.toJsonArray(),
            "musicaPreferida" to form.musicaPreferida.toJsonArray(),
            "corPreferida" to JsonPrimitive(form.corPreferida.trim()),
            "esportes" to form.esportes.toJsonArray(),
            "pets" to JsonPrimitive(form.pets.trim().ifBlank { "nenhum" }),
            "updatedAt" to JsonPrimitive(Instant.now().toString()),
        )
        if (form.foto.trim().isNotBlank()) {
            payload["foto"] = JsonPrimitive(form.foto.trim())
        }

        updateUserWithOptionalColumnFallback(
            client = client,
            tenantId = cleanTenantId,
            userId = cleanUserId,
            payload = payload,
        )
    }

    private suspend fun fetchConfig(
        client: SupabaseClient,
        tenantId: String,
    ): CadastroConfig {
        val docIds = listOf(
            buildTenantScopedConfigId(tenantId, CadastroConfigDocId),
            CadastroConfigDocId,
        ).filter(String::isNotBlank)

        val data = client.from(AppConfigTable)
            .select(columns = Columns.raw("id,data,updatedAt")) {
                filter { isIn("id", docIds) }
                limit(count = 2)
            }
            .decodeList<CadastroConfigRow>()
            .sortedBy { if (it.id == docIds.firstOrNull()) 0 else 1 }
            .firstOrNull()
            ?.data as? JsonObject
            ?: return CadastroConfig(turmas = emptyList())

        return CadastroConfig(
            turmas = emptyList(),
            sportOptions = data.choiceOptions("sportOptions", CadastroDefaults.sports),
            specialPlaceOptions = data.choiceOptions("specialPlaceOptions", CadastroDefaults.specialPlaces),
            foodOptions = data.choiceOptions("foodOptions", CadastroDefaults.foods),
            musicOptions = data.choiceOptions("musicOptions", CadastroDefaults.musics),
            colorOptions = data.colorOptions("colorOptions", CadastroDefaults.colors),
        )
    }

    /** Turmas vem de `app_config` id `turmas_config`, igual ao album. */
    private suspend fun fetchTurmas(
        client: SupabaseClient,
        tenantId: String,
    ): List<CadastroTurmaOption> {
        val docIds = listOf(
            buildTenantScopedConfigId(tenantId, TurmasConfigDocId),
            TurmasConfigDocId,
        ).filter(String::isNotBlank)

        val data = client.from(AppConfigTable)
            .select(columns = Columns.raw("id,data,updatedAt")) {
                filter { isIn("id", docIds) }
                limit(count = 2)
            }
            .decodeList<CadastroConfigRow>()
            .sortedBy { if (it.id == docIds.firstOrNull()) 0 else 1 }
            .firstOrNull()
            ?.data as? JsonObject
            ?: return emptyList()

        val array = data["turmas"] as? JsonArray ?: return emptyList()
        return array.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val id = obj.stringValue("id").ifBlank { return@mapNotNull null }
            CadastroTurmaOption(
                id = id,
                label = obj.stringValue("nome").ifBlank { obj.stringValue("label").ifBlank { id } },
                hidden = (obj["hidden"] as? JsonPrimitive)?.booleanOrNull ?: false,
            )
        }.filterNot(CadastroTurmaOption::hidden)
    }

    private suspend fun fetchUserRow(
        client: SupabaseClient,
        tenantId: String,
        userId: String,
    ): CadastroUserRow? {
        val scoped = runCatching {
            client.from(UsersTable)
                .select(columns = Columns.raw(CadastroUserColumns)) {
                    filter {
                        eq("uid", userId)
                        if (tenantId.isNotBlank()) eq("tenant_id", tenantId)
                    }
                    limit(count = 1)
                }
                .decodeList<CadastroUserRow>()
                .firstOrNull()
        }.getOrNull()
        if (scoped != null) return scoped

        return runCatching {
            client.from(UsersTable)
                .select(columns = Columns.raw(CadastroUserColumns)) {
                    filter { eq("uid", userId) }
                    limit(count = 1)
                }
                .decodeList<CadastroUserRow>()
                .firstOrNull()
        }.getOrNull()
    }

    /** Remove colunas inexistentes no schema do tenant em vez de falhar o save inteiro. */
    private suspend fun updateUserWithOptionalColumnFallback(
        client: SupabaseClient,
        tenantId: String,
        userId: String,
        payload: LinkedHashMap<String, JsonElement>,
    ) {
        val nonRemovableColumns = setOf("nome", "turma")
        val attempted = mutableSetOf<String>()
        var mutablePayload = LinkedHashMap(payload)

        while (mutablePayload.isNotEmpty()) {
            try {
                client.from(UsersTable).update(JsonObject(mutablePayload)) {
                    filter {
                        eq("uid", userId)
                        if (tenantId.isNotBlank()) eq("tenant_id", tenantId)
                    }
                }
                return
            } catch (error: Throwable) {
                val column = extractCadastroProblematicColumn(error)
                if (
                    column.isNullOrBlank() ||
                    column in nonRemovableColumns ||
                    column in attempted ||
                    !mutablePayload.containsKey(column)
                ) {
                    throw error
                }
                attempted += column
                mutablePayload = LinkedHashMap(mutablePayload).also { it.remove(column) }
            }
        }
    }

    private fun buildTenantScopedConfigId(tenantId: String, baseId: String): String {
        val cleanTenantId = tenantId.trim()
        return if (cleanTenantId.isBlank()) baseId else "tenant:$cleanTenantId::$baseId"
    }

    private companion object {
        const val UsersTable = "users"
        const val AppConfigTable = "app_config"
        const val CadastroConfigDocId = "cadastro_config"
        const val TurmasConfigDocId = "turmas_config"
        const val MaxNameLength = 120
        const val MaxShortLength = 60
        const val MaxBioLength = 240
        const val CadastroUserColumns =
            "uid,nome,apelido,matricula,turma,instagram,instagramPublico,telefone,whatsappPublico," +
                "bio,dataNascimento,idadePublica,cidadeOrigem,estadoOrigem,statusRelacionamento," +
                "relacionamentoPublico,signo,signoPublico,ascendente,ascendentePublico,lugarEspecial," +
                "comidaPreferida,musicaPreferida,corPreferida,esportes,pets,foto,tenant_id"
    }
}

private fun List<String>.toJsonArray(): JsonArray =
    JsonArray(map(String::trim).filter(String::isNotBlank).distinct().map(::JsonPrimitive))

private fun JsonObject.stringValue(key: String): String =
    (this[key] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()

private fun JsonObject.choiceOptions(
    key: String,
    fallback: List<CadastroChoiceOption>,
): List<CadastroChoiceOption> {
    val array = this[key] as? JsonArray ?: return fallback
    val parsed = array.mapNotNull { element ->
        val obj = element as? JsonObject ?: return@mapNotNull null
        val id = obj.stringValue("id").ifBlank { return@mapNotNull null }
        val enabled = (obj["enabled"] as? JsonPrimitive)?.booleanOrNull ?: true
        if (!enabled) return@mapNotNull null
        CadastroChoiceOption(
            id = id,
            label = obj.stringValue("label").ifBlank { id },
            icon = obj.stringValue("icon").ifBlank { "✨" },
        )
    }
    // O web faz merge com os defaults; aqui unimos por id preservando os padroes.
    val merged = LinkedHashMap<String, CadastroChoiceOption>()
    fallback.forEach { merged[it.id] = it }
    parsed.forEach { merged[it.id] = it }
    return merged.values.toList()
}

private fun JsonObject.colorOptions(
    key: String,
    fallback: List<CadastroColorOption>,
): List<CadastroColorOption> {
    val array = this[key] as? JsonArray ?: return fallback
    val parsed = array.mapNotNull { element ->
        val obj = element as? JsonObject ?: return@mapNotNull null
        val id = obj.stringValue("id").ifBlank { return@mapNotNull null }
        CadastroColorOption(
            id = id,
            label = obj.stringValue("label").ifBlank { id },
            hex = obj.stringValue("hex").ifBlank { "#71717A" },
        )
    }
    return parsed.ifEmpty { fallback }
}

private fun extractCadastroProblematicColumn(error: Throwable): String? {
    val message = generateSequence(error) { it.cause }
        .joinToString("\n") { it.message.orEmpty() }
    val patterns = listOf(
        Regex("column\\s+[a-z0-9_]+\\.([a-zA-Z0-9_]+)\\s+does not exist", RegexOption.IGNORE_CASE),
        Regex("column\\s+[\"']?([a-zA-Z0-9_]+)[\"']?\\s+does not exist", RegexOption.IGNORE_CASE),
        Regex("could not find the [\"']?([a-zA-Z0-9_]+)[\"']? column", RegexOption.IGNORE_CASE),
    )
    return patterns.firstNotNullOfOrNull { it.find(message)?.groupValues?.getOrNull(1) }
}

@Serializable
private data class CadastroConfigRow(
    val id: String = "",
    val data: JsonElement? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
)

@Serializable
private data class CadastroUserRow(
    val uid: String = "",
    val nome: String? = null,
    val apelido: String? = null,
    val matricula: String? = null,
    val turma: String? = null,
    val instagram: String? = null,
    val instagramPublico: Boolean? = null,
    val telefone: String? = null,
    val whatsappPublico: Boolean? = null,
    val bio: String? = null,
    val dataNascimento: String? = null,
    val idadePublica: Boolean? = null,
    val cidadeOrigem: String? = null,
    val estadoOrigem: String? = null,
    val statusRelacionamento: String? = null,
    val relacionamentoPublico: Boolean? = null,
    val signo: String? = null,
    val signoPublico: Boolean? = null,
    val ascendente: String? = null,
    val ascendentePublico: Boolean? = null,
    val lugarEspecial: List<String>? = null,
    val comidaPreferida: List<String>? = null,
    val musicaPreferida: List<String>? = null,
    val corPreferida: String? = null,
    val esportes: List<String>? = null,
    val pets: String? = null,
    val foto: String? = null,
    @SerialName("tenant_id") val tenantId: String? = null,
) {
    fun toForm(): CadastroForm = CadastroForm(
        nome = nome.orEmpty().trim(),
        apelido = apelido.orEmpty().trim(),
        matricula = matricula.orEmpty().trim(),
        turma = turma.orEmpty().trim(),
        instagram = instagram.orEmpty().trim().removePrefix("@"),
        instagramPublico = instagramPublico ?: false,
        telefone = telefone.orEmpty().trim(),
        whatsappPublico = whatsappPublico ?: false,
        bio = bio.orEmpty().trim(),
        dataNascimento = dataNascimento.orEmpty().trim(),
        idadePublica = idadePublica ?: true,
        cidadeOrigem = cidadeOrigem.orEmpty().trim(),
        estadoOrigem = estadoOrigem.orEmpty().trim(),
        statusRelacionamento = statusRelacionamento.orEmpty().trim().ifBlank { "Solteiro(a)" },
        relacionamentoPublico = relacionamentoPublico ?: false,
        signo = signo.orEmpty().trim(),
        signoPublico = signoPublico ?: false,
        ascendente = ascendente.orEmpty().trim(),
        ascendentePublico = ascendentePublico ?: false,
        lugarEspecial = lugarEspecial.orEmpty().filter(String::isNotBlank),
        comidaPreferida = comidaPreferida.orEmpty().filter(String::isNotBlank),
        musicaPreferida = musicaPreferida.orEmpty().filter(String::isNotBlank),
        corPreferida = corPreferida.orEmpty().trim(),
        esportes = esportes.orEmpty().filter(String::isNotBlank),
        pets = pets.orEmpty().trim().ifBlank { "nenhum" },
        foto = foto.orEmpty().trim(),
    )
}
