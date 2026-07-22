package com.example.usc1.domain.model

data class AdminDatabaseFieldReport(
    val tenantId: String,
    val tables: List<AdminDatabaseTableFields>,
)

data class AdminDatabaseTableFields(
    val tableName: String,
    val fields: List<String>,
)

object AdminDatabaseScannerCatalog {
    const val SampleDocsPerCollection = 40
    const val MaxScannerSampleDocs = 80

    val TablesToScan = listOf("users", "produtos", "eventos", "orders", "parceiros")

    val FallbackColumnsByTable: Map<String, List<String>> = mapOf(
        "users" to listOf(
            "uid",
            "nome",
            "email",
            "role",
            "status",
            "turma",
            "plano",
            "patente",
            "createdAt",
            "updatedAt",
        ),
        "produtos" to listOf(
            "id",
            "nome",
            "categoria",
            "descricao",
            "img",
            "preco",
            "estoque",
            "active",
            "aprovado",
            "vendidos",
            "createdAt",
            "updatedAt",
        ),
        "eventos" to listOf(
            "id",
            "titulo",
            "data",
            "hora",
            "local",
            "status",
            "categoria",
            "createdAt",
            "updatedAt",
        ),
        "orders" to listOf(
            "id",
            "userId",
            "productId",
            "productName",
            "status",
            "total",
            "createdAt",
            "updatedAt",
        ),
        "parceiros" to listOf(
            "id",
            "nome",
            "categoria",
            "tier",
            "status",
            "imgLogo",
            "imgCapa",
            "totalScans",
            "createdAt",
        ),
    )
}
