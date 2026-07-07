package com.example.usc1.ui.scanner

enum class ScannerMode(val label: String) {
    EventTicket("Ingresso"),
    PartyAccess("Festa"),
    ProductWithdrawal("Produto/Ficha"),
}

data class ScannerResult(
    val title: String,
    val subtitle: String,
    val payload: String,
)

data class ScannerUiState(
    val hasPermission: Boolean = true,
    val currentMode: ScannerMode = ScannerMode.EventTicket,
    val successResult: ScannerResult = ScannerResult(
        title = "Entrada validada",
        subtitle = "Ingresso ativo para Intermed USC",
        payload = "USC-SCAN-OK-2026",
    ),
    val errorResult: ScannerResult = ScannerResult(
        title = "QR inválido",
        subtitle = "Token expirado, usado ou sem permissão para este evento",
        payload = "USC-SCAN-ERROR",
    ),
)
