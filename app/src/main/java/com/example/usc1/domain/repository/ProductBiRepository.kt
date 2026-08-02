package com.example.usc1.domain.repository

import com.example.usc1.domain.model.ProductBiDataset
import com.example.usc1.domain.model.ProductBiScope

/**
 * Motor de dados do BI Loja (M8.3 + M8.4).
 *
 * Espelha o que cada consumidor de `web-reference/src/components/ProductManagementAnalytics.tsx`
 * entrega nas props `products`, `orders` e `users`:
 * - tenant: `ProductsBi` de `app/admin/gestao/_components/AdminBiDashboard.tsx` (1393-1424);
 * - liga/comissão/diretório: `LeagueFinanceDashboard` com `view="produtos"` (769-778);
 * - mini-vendor: `app/configuracoes/mini-vendor/gestao/page.tsx`.
 *
 * Uma implementação só atende os cinco players — o escopo é parâmetro, não repositório por área.
 */
interface ProductBiRepository {
    /**
     * @param sellerId id do coletivo ou do mini-vendor. Ignorado no escopo tenant, onde o dono
     *   é o próprio `tenant_id`.
     * @param userId usuário logado; só o escopo mini-vendor usa, para resolver o perfil da
     *   lojinha em `mini_vendors` (`fetchCurrentMiniVendorProfile` do web).
     */
    suspend fun getDataset(
        tenantId: String,
        scope: ProductBiScope,
        sellerId: String = "",
        userId: String = "",
    ): ProductBiDataset
}
