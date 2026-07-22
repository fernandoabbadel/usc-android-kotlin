package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.data.supabase.SupabaseTenantResolver
import com.example.usc1.domain.model.AdminDatabaseFieldReport
import com.example.usc1.domain.model.AdminDatabaseScannerCatalog
import com.example.usc1.domain.model.AdminDatabaseTableFields
import com.example.usc1.domain.repository.AdminDatabaseScannerRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabaseAdminDatabaseScannerRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : AdminDatabaseScannerRepository {
    override suspend fun scanDatabaseFields(forceRefresh: Boolean): AdminDatabaseFieldReport = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val tables = AdminDatabaseScannerCatalog.TablesToScan.map { tableName ->
            AdminDatabaseTableFields(
                tableName = tableName,
                fields = scanTableFields(
                    client = client,
                    tenantId = tenantId,
                    tableName = tableName,
                    sampleDocsPerCollection = AdminDatabaseScannerCatalog.SampleDocsPerCollection,
                ),
            )
        }
        AdminDatabaseFieldReport(tenantId = tenantId, tables = tables)
    }

    private suspend fun scanTableFields(
        client: SupabaseClient,
        tenantId: String,
        tableName: String,
        sampleDocsPerCollection: Int,
    ): List<String> {
        val mutableColumns = AdminDatabaseScannerCatalog.FallbackColumnsByTable[tableName]
            ?.toMutableList()
            ?: mutableListOf("id")
        val limit = sampleDocsPerCollection.coerceIn(1, AdminDatabaseScannerCatalog.MaxScannerSampleDocs).toLong()

        while (mutableColumns.isNotEmpty()) {
            try {
                client.from(tableName)
                    .select(columns = Columns.raw(mutableColumns.joinToString(","))) {
                        filter {
                            eq("tenant_id", tenantId)
                        }
                        limit(count = limit)
                    }
                return mutableColumns.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
            } catch (error: Throwable) {
                if (isMissingTableError(error)) return emptyList()
                val missingColumn = extractMissingColumn(error)
                    ?: throw IllegalStateException("Erro ao escanear campos de $tableName: ${error.message}", error)
                val removed = mutableColumns.removeAll { it.equals(missingColumn, ignoreCase = true) }
                if (!removed) {
                    throw IllegalStateException("Erro ao escanear campos de $tableName: ${error.message}", error)
                }
            }
        }

        return emptyList()
    }

    private fun isMissingTableError(error: Throwable): Boolean {
        val message = error.message.orEmpty().lowercase()
        return message.contains("42p01") ||
            message.contains("pgrst205") ||
            message.contains("does not exist") && message.contains("table")
    }

    private fun extractMissingColumn(error: Throwable): String? {
        val message = error.message.orEmpty()
        val patterns = listOf(
            Regex("column\\s+[a-z0-9_]+\\.([a-z0-9_]+)\\s+does not exist", RegexOption.IGNORE_CASE),
            Regex("column\\s+[\"']?([a-z0-9_]+)[\"']?\\s+does not exist", RegexOption.IGNORE_CASE),
            Regex("could not find the [\"']?([a-z0-9_]+)[\"']? column", RegexOption.IGNORE_CASE),
        )
        return patterns.firstNotNullOfOrNull { pattern ->
            pattern.find(message)?.groupValues?.getOrNull(1)
        }
    }
}
