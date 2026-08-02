package com.example.usc1.domain.repository

import com.example.usc1.domain.model.EventBiDataset
import com.example.usc1.domain.model.EventBiFilter

/**
 * Motor do BI de Eventos (M8.1).
 *
 * Espelha `loadBiData` + `selectedData` de
 * `web-reference/src/app/admin/bi/_components/AdminEventBiDashboard.tsx`.
 *
 * Uma implementação só atende os quatro players (tenant, liga, comissão e diretório):
 * o escopo é parâmetro (`EventBiFilter.scope`), não um repositório por área.
 *
 * Diferença deliberada em relação ao web, registrada em `docs/ANDROID_PROGRESS.md`:
 * `loadBiData` baixa sete tabelas inteiras do tenant (600 eventos, 6000 ingressos,
 * 12000 RSVPs, 3000 produtos, 8000 pedidos, 6000 usuários e 1000 registros de
 * `ligas_config`) e filtra em memória. Aqui o escopo vai para a consulta.
 */
interface CollectiveEventBiRepository {
    /**
     * Recorte do escopo já filtrado.
     *
     * @param includeTransactions `false` traz só o que os filtros precisam (eventos e produtos
     *   do escopo), que é o suficiente para o hub. `true` acrescenta ingressos, pedidos e RSVPs.
     */
    suspend fun getDataset(
        tenantId: String,
        filter: EventBiFilter,
        includeTransactions: Boolean = false,
    ): EventBiDataset
}
