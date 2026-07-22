# Progresso de Tradução de Rotas Web para Android

Regra de contagem: só conta como rota web um arquivo cujo nome exato é `page.tsx`.

## Totais

- Total web `page.tsx`: 362
- Admin raiz `web-reference/src/app/admin/**/page.tsx`: 120
- Admin incluindo `[tenant]/admin/**/page.tsx`: 127

## Critério Para Contar Como Traduzida

Uma página só entra em "traduzidas" quando:

- tem fonte web identificada;
- tem rota/tela Kotlin correspondente;
- usa textos e estrutura do web app;
- não usa mock no fluxo real;
- usa Supabase direto quando o web usa Supabase;
- respeita tenant, membership e role;
- compila.

## Traduzidas Confirmadas

| # | Web `page.tsx` | Android Kotlin | Status |
|---:|---|---|---|
| 1 | `web-reference/src/app/admin/page.tsx` | `AdminDashboardScreen`, `AdminDashboardViewModel`, `SupabaseAdminDashboardRepository` | Traduzida para Kotlin com Supabase direto, `tenant_id`, limites 5/5 e role admin/master |
| 2 | `web-reference/src/app/admin/dashboard-modulos/page.tsx` | `AdminDashboardModulesScreen`, `AdminDashboardModulesViewModel`, `SupabaseAdminDashboardModulesRepository` | Traduzida para Kotlin com Supabase direto em `app_config`, `tenant_id`, perfil admin A/B, edição de `data.modules` e role admin/master |
| 3 | `web-reference/src/app/admin/politicas/page.tsx` | `AdminTenantPoliciesScreen`, `AdminTenantPoliciesViewModel`, `SupabaseAdminTenantPoliciesRepository` | Traduzida para Kotlin com Supabase direto em `tenant_policy_documents`, `tenant_id`, módulos oficiais, conteúdo limitado a 12000 e role admin/master |
| 4 | `web-reference/src/app/admin/usuarios/[id]/page.tsx` | `AdminUserDetailScreen`, `AdminUserDetailViewModel`, `SupabaseAdminUsersRepository` | Traduzida para Kotlin com Supabase direto em `users`, validação prévia em `tenant_memberships`, edição de nome/telefone/matrícula/turma/plano/status, bloqueio/desbloqueio e exclusão |
| 5 | `web-reference/src/app/admin/loja/review/page.tsx` | `AdminStoreReviewsScreen`, `AdminStoreReviewsViewModel`, `SupabaseAdminStoreRepository` | Traduzida para Kotlin com Supabase direto em `produtos` + `reviews`, limite 300, paginação 20, filtro por tenant via produto e aprovação/rejeição sem callable |
| 6 | `web-reference/src/app/admin/mini-vendors/page.tsx` | `AdminMiniVendorsHubScreen`, rota `admin/mini-vendors` | Traduzida para Kotlin como hub visual/funcional com os dois links do web: pendentes de aprovação e todos os mini vendors. A rota web não faz consulta Supabase direta |
| 7 | `web-reference/src/app/admin/denuncias/page.tsx` | `AdminReportsHubScreen`, rota `admin/denuncias` | Traduzida para Kotlin como hub visual/funcional com os quatro links do web: Banidos, Comunidade, Gym e Suporte |
| 8 | `web-reference/src/app/admin/denuncias/banidos/page.tsx` | `AdminReportsListScreen`, `AdminReportsViewModel`, `SupabaseAdminReportsRepository` | Traduzida para Kotlin com Supabase direto em `banned_appeals`, filtro `tenant_id`, limite 240, paginação 20, resposta administrativa e exclusão direta com RLS |
| 9 | `web-reference/src/app/admin/denuncias/comunidade/page.tsx` | `AdminReportsListScreen`, `AdminReportsViewModel`, `SupabaseAdminReportsRepository` | Traduzida para Kotlin com Supabase direto em `denuncias`, filtro `tenant_id`, limite 240, paginação 20 e link para usuário denunciante |
| 10 | `web-reference/src/app/admin/denuncias/gym/page.tsx` | `AdminReportsListScreen`, `AdminReportsViewModel`, `SupabaseAdminReportsRepository` | Traduzida para Kotlin com Supabase direto em `support_requests`, filtro `tenant_id`, `category=denuncia`, filtro local de módulo/termos gym e paginação 20 |
| 11 | `web-reference/src/app/admin/denuncias/suporte/page.tsx` | `AdminReportsListScreen`, `AdminReportsViewModel`, `SupabaseAdminReportsRepository` | Traduzida para Kotlin com Supabase direto em `support_requests`, filtro `tenant_id`, limite 240, paginação 20, resposta administrativa, notificação e exclusão direta com RLS |
| 12 | `web-reference/src/app/admin/parceiros/page.tsx` | `AdminPartnersHubScreen`, rota `admin/parceiros` | Traduzida para Kotlin como hub leve com os quatro links do web: parceiros ativos, empresas, dados cadastrais e histórico. A rota web não faz consulta Supabase direta |
| 13 | `web-reference/src/app/admin/parceiros/ativos/page.tsx` | `AdminPartnersActiveScreen`, `AdminPartnersViewModel`, `SupabasePartnersRepository` | Traduzida para Kotlin com Supabase direto em `parceiros`, filtro `tenant_id`, `status=active`, contagem por status/plano, limite 600 para contagem e paginação 20 |
| 14 | `web-reference/src/app/admin/parceiros/dados/page.tsx` | `AdminPartnersBiScreen`, `AdminPartnersViewModel`, `SupabasePartnersRepository` | Traduzida para Kotlin com Supabase direto em `parceiros` e `scans`, filtro `tenant_id`, limites 600/1200, métrica quantidade/valor, agrupamentos por parceiro, cupom, tipo de QR e usuário |
| 15 | `web-reference/src/app/admin/parceiros/historico/page.tsx` | `AdminPartnersHistoryScreen`, `AdminPartnersViewModel`, `SupabasePartnersRepository` | Traduzida para Kotlin com Supabase direto em `scans`, filtro `tenant_id`, ordenação por `timestamp`, paginação 20 e campos administrativos da leitura |
| 16 | `web-reference/src/app/admin/planos/page.tsx` | `AdminPlansHubScreen`, rota `admin/planos` | Traduzida para Kotlin como hub com as listas por plano, gestão/auditoria, botão de catálogo e link para marketing CSS. A rota web não faz consulta Supabase direta |
| 17 | `web-reference/src/app/admin/gestao/page.tsx` | `AdminManagementHubScreen`, rota `admin/gestao` | Traduzida para Kotlin como hub de gestão administrativa com cards Eventos, BI Loja, Treinos, Financeiro e integrações de BI. A rota web não faz consulta Supabase direta |
| 18 | `web-reference/src/app/admin/album/page.tsx` | `AdminAlbumScreen`, `AdminAlbumViewModel`, `SupabaseAdminAlbumRepository` | Traduzida para Kotlin com Supabase direto em `app_config`, id `tenant:{tenant_id}::album_ui`, edição de capa/título/subtítulo e menu com Caça Calouro, Pontuação Calouro, Pontuação Geral e Customização |
| 19 | `web-reference/src/app/admin/games/page.tsx` | `AdminGamesScreen`, `AdminGamesViewModel`, `SupabaseAdminGamesRepository` | Traduzida para Kotlin com Supabase direto em `users`, filtro `tenant_id`, limite 80, busca por atleta e cálculo local de stats equivalente a `calculateUserStats` |
| 20 | `web-reference/src/app/admin/scanner/page.tsx` | `AdminDatabaseScannerScreen`, `AdminDatabaseScannerViewModel`, `SupabaseAdminDatabaseScannerRepository` | Traduzida para Kotlin com Supabase direto nas tabelas `users`, `produtos`, `eventos`, `orders` e `parceiros`, limite 40, filtro por `tenant_id` e sem mock |
| 21 | `web-reference/src/app/admin/apadrinhamento/page.tsx` | `AdminMentorshipScreen`, `AdminMentorshipViewModel`, `SupabaseAdminMentorshipRepository` | Traduzida para Kotlin com Supabase direto em `app_config`, id `tenant:{tenant_id}::mentorship_labels`, mesmos rótulos dinâmicos e validação de limites do web |
| 22 | `web-reference/src/app/admin/logs/page.tsx` | `AdminActivityLogsScreen`, `AdminActivityLogsViewModel`, `SupabaseAdminActivityLogsRepository` | Traduzida para Kotlin com Supabase direto em `activity_logs`, filtro `tenant_id`, paginação 20, cursor por offset, busca local e fallback de ordenação equivalente ao web |
| 23 | `web-reference/src/app/admin/permissoes/usuarios/page.tsx` | `AdminPermissionUsersScreen`, `AdminPermissionUsersViewModel`, `SupabaseAdminUsersRepository` | Traduzida para Kotlin com Supabase direto em `users`, `tenant_memberships` e `activity_logs`, filtro `tenant_id`, paginação 20, cargos do web, filtros A-F/G-K/L-Q/R-Z/Todos, liderança de turma em `users.extra` e sem mock |
| 24 | `web-reference/src/app/admin/planos/lista_bicho_solto/page.tsx` | `AdminPlanSubscriptionsScreen`, `AdminPlanSubscriptionsViewModel`, `SupabaseAdminPlanSubscriptionsRepository` | Traduzida para Kotlin com Supabase direto em `assinaturas`, filtro `tenant_id`, limite 600, paginação local 20 e matcher `bicho` igual ao web |
| 25 | `web-reference/src/app/admin/planos/lista_cardume_livre/page.tsx` | `AdminPlanSubscriptionsScreen`, `AdminPlanSubscriptionsViewModel`, `SupabaseAdminPlanSubscriptionsRepository` | Traduzida para Kotlin com Supabase direto em `assinaturas`, filtro `tenant_id`, limite 600, paginação local 20 e matcher `cardume` igual ao web |
| 26 | `web-reference/src/app/admin/planos/lista_atleta/page.tsx` | `AdminPlanSubscriptionsScreen`, `AdminPlanSubscriptionsViewModel`, `SupabaseAdminPlanSubscriptionsRepository` | Traduzida para Kotlin com Supabase direto em `assinaturas`, filtro `tenant_id`, limite 600, paginação local 20 e matcher `atleta` igual ao web |
| 27 | `web-reference/src/app/admin/planos/lista_lenda/page.tsx` | `AdminPlanSubscriptionsScreen`, `AdminPlanSubscriptionsViewModel`, `SupabaseAdminPlanSubscriptionsRepository` | Traduzida para Kotlin com Supabase direto em `assinaturas`, filtro `tenant_id`, limite 600, paginação local 20 e matcher `lenda` igual ao web |
| 28 | `web-reference/src/app/admin/planos/auditoria/page.tsx` | `AdminPlanAuditScreen`, `AdminPlanAuditViewModel`, `SupabaseAdminPlanSubscriptionsRepository` | Traduzida para Kotlin com Supabase direto em `solicitacoes_adesao` e `assinaturas`, filtro `tenant_id`, limites 300/600 e métricas de fluxo equivalentes ao web |

## Próximas Rotas Admin

1. `web-reference/src/app/admin/usuarios/page.tsx`
2. `web-reference/src/app/admin/loja/categorias/page.tsx`
3. `web-reference/src/app/admin/loja/produtos/page.tsx`
4. `web-reference/src/app/admin/loja/pedidos-pendentes/page.tsx`
5. `web-reference/src/app/admin/loja/pedidos-aprovados/page.tsx`
6. `web-reference/src/app/admin/permissoes/page.tsx`
7. `web-reference/src/app/admin/carteirinha/page.tsx`
8. `web-reference/src/app/admin/fidelidade/page.tsx`
9. `web-reference/src/app/admin/boardround/page.tsx`
10. `web-reference/src/app/admin/guia/page.tsx`

## Em Andamento, Ainda Não Contadas

Estas páginas já têm código Kotlin/Supabase iniciado, mas ainda não entram nas traduzidas confirmadas porque dependem de subrotas/componentes relacionados, Storage, fluxo sensível ou paridade visual/funcional ainda incompleta.

| Web `page.tsx` | Android Kotlin | Pendente Para Contar |
|---|---|---|
| `web-reference/src/app/dashboard/page.tsx` | `HomeScreen`, `HomeViewModel`, `SupabaseHomeDashboardRepository` | Bundle real, tenant guest e 47 módulos efetivos portados; ainda faltam tema dinâmico, interações, cardápio nativo do evento e QA visual em aparelho |
| `web-reference/src/app/[tenant]/dashboard/page.tsx` | Mesma implementação da Home Android | O tenant explícito já chega à Home após revalidação; ainda faltam landing intermediária e Android App Link equivalentes ao `tenantSlugOverride`, além das pendências da Home raiz |
| `web-reference/src/app/admin/usuarios/page.tsx` | `AdminUsersScreen`, `AdminUsersViewModel`, `SupabaseAdminUsersRepository` | Decidir fluxo seguro para `Recontar Follows`, que no web usa callable administrativa. Não foi criada Edge Function e não há fallback mock |
| `web-reference/src/app/admin/loja/page.tsx` | `AdminStoreScreen`, `AdminStoreViewModel`, `SupabaseAdminStoreRepository` | Traduzir as subrotas de loja chamadas pelos cards: categorias, produtos, pedidos pendentes e pedidos aprovados. `review` já foi traduzida |
| `web-reference/src/app/admin/loja/categorias/page.tsx` | `AdminStoreCategoriesScreen`, `AdminStoreCategoriesViewModel`, `SupabaseAdminStoreRepository` | Categorias reais em Supabase foram portadas com `tenant_id`, `seller_type`, `seller_id`, ordem e visibilidade, mas ainda não entra no contador porque o upload de capa via Storage do web está pendente no Android e ligas ainda não usam `fetchLeagueSummaries` equivalente |
| `web-reference/src/app/admin/loja/produtos/page.tsx` | `AdminStoreProductsScreen`, `AdminStoreProductsViewModel`, `SupabaseAdminStoreRepository` | Lista real por categoria, histórico de desativados, criação/edição básica e ativar/desativar produto foram portados com Supabase direto em `produtos`, `tenant_id`, `seller_type` e `seller_id`, mas ainda não entra no contador porque upload de imagem, recebedores, preço/visibilidade por plano e variações avançadas do web ainda não foram traduzidos |
| `web-reference/src/app/admin/loja/produtos-desativados/page.tsx` | `AdminStoreProductsScreen`, rota `admin/loja/produtos-desativados` | Rota wrapper do web foi portada para a variante desativada com `active=false`, mas segue a mesma pendência funcional da rota principal de produtos |
| `web-reference/src/app/admin/loja/pedidos-pendentes/page.tsx` | `AdminStoreOrdersScreen`, `AdminStoreOrdersViewModel`, `SupabaseAdminStoreRepository` | Fluxo comum de pedidos foi portado com Supabase real, mas não entra no contador porque o web também atualiza vouchers/evento dentro de `approveStoreOrder`, operação sensível ainda pendente de autorização |
| `web-reference/src/app/admin/loja/pedidos-aprovados/page.tsx` | `AdminStoreOrdersScreen`, `AdminStoreOrdersViewModel`, `SupabaseAdminStoreRepository` | Mesmo componente de pedidos portado, mas ainda depende de fechar a divergência de vouchers/evento e nomes visuais de aprovadores |
| `web-reference/src/app/admin/loja/pedidos-pendentes/[categoria]/page.tsx` | `AdminStoreOrdersScreen`, rota `admin/loja/pedidos-pendentes/{category}` | Rota por categoria portada com filtro por `productId`, mas segue bloqueada pelo mesmo ponto sensível de aprovação |
| `web-reference/src/app/admin/loja/pedidos-aprovados/[categoria]/page.tsx` | `AdminStoreOrdersScreen`, rota `admin/loja/pedidos-aprovados/{category}` | Rota por categoria portada com filtro por `productId`, mas segue bloqueada pelo mesmo ponto sensível de aprovação |
| `web-reference/src/app/admin/mini-vendors/aprovacoes/page.tsx` | `AdminMiniVendorsScreen`, `AdminMiniVendorsViewModel`, `SupabaseAdminMiniVendorsRepository` | Aprovação/rejeição real em Supabase foi portada, mas links internos de edição/produtos/pedidos do mini-vendor ainda não foram traduzidos |
| `web-reference/src/app/admin/mini-vendors/cadastros/page.tsx` | `AdminMiniVendorsScreen`, `AdminMiniVendorsViewModel`, `SupabaseAdminMiniVendorsRepository` | Diretório real em Supabase foi portado, mas links internos de página pública, edição, produtos e pedidos ainda não foram traduzidos |
| `web-reference/src/app/admin/parceiros/empresas/page.tsx` | `AdminPartnersCompaniesScreen`, `AdminPartnersViewModel`, `SupabasePartnersRepository` | Lista, busca, filtro, criação/edição, status e reset de senha foram portados com Supabase direto em `parceiros`, mas ainda não entra no contador porque o upload de logo/capa via Storage e a rota pública `/empresa/[id]` ainda não foram traduzidos |
| `web-reference/src/app/parceiros/page.tsx` | `PartnersScreen`, `PartnersViewModel`, `SupabasePartnersRepository` | Mock público removido e leitura real em `parceiros` com `tenant_id`/`status=active`, mas ainda não entra no contador porque faltam agrupamento visual por plano, busca e link real para `/empresa` |
| `web-reference/src/app/parceiros/[id]/page.tsx` | `PartnerDetailScreen`, `PartnerBenefitsScreen`, `PartnersViewModel`, `SupabasePartnersRepository` | Mock de detalhe removido e busca real por `id + tenant_id`, mas ainda não entra no contador porque o web também ativa cupom/gera QR e grava scan com `createPartnerScan` |
| `web-reference/src/app/admin/planos/historico/page.tsx` | `AdminPendingRouteScreen`, rota `admin/planos/historico` | Rota registrada para não quebrar navegação, mas ainda pendente de tradução real |
| `web-reference/src/app/admin/planos/editar/page.tsx` | `AdminPendingRouteScreen`, rota `admin/planos/editar` | Rota registrada para não quebrar navegação, mas ainda pendente de tradução real |
| `web-reference/src/app/admin/gestao/eventos/page.tsx` | `AdminPendingRouteScreen`, rota `admin/gestao/eventos` | Rota registrada para não quebrar navegação, mas ainda pendente de tradução real |
| `web-reference/src/app/admin/gestao/loja/page.tsx` | `AdminPendingRouteScreen`, rota `admin/gestao/loja` | Rota registrada para não quebrar navegação, mas ainda pendente de tradução real |
| `web-reference/src/app/admin/gestao/treinos/page.tsx` | `AdminPendingRouteScreen`, rota `admin/gestao/treinos` | Rota registrada para não quebrar navegação, mas ainda pendente de tradução real |
| `web-reference/src/app/admin/gestao/financeiro/page.tsx` | `AdminPendingRouteScreen`, rota `admin/gestao/financeiro` | Rota registrada para não quebrar navegação, mas ainda pendente de tradução real |
| `web-reference/src/app/admin/album/caca_calouro/page.tsx` | `AdminPendingRouteScreen`, rota `admin/album/caca_calouro` | Rota registrada para não quebrar navegação, mas ainda pendente de tradução real |
| `web-reference/src/app/admin/album/pontua_calouro/page.tsx` | `AdminPendingRouteScreen`, rota `admin/album/pontua_calouro` | Rota registrada para não quebrar navegação, mas ainda pendente de tradução real |
| `web-reference/src/app/admin/album/pontua_geral/page.tsx` | `AdminPendingRouteScreen`, rota `admin/album/pontua_geral` | Rota registrada para não quebrar navegação, mas ainda pendente de tradução real |
| `web-reference/src/app/admin/album/customizacao/page.tsx` | `AdminPendingRouteScreen`, rota `admin/album/customizacao` | Rota registrada para não quebrar navegação, mas ainda pendente de tradução real |
| `web-reference/src/app/admin/turma/page.tsx` | `AdminPendingRouteScreen`, rota `admin/turma` | Rota registrada para não quebrar navegação, mas ainda pendente de tradução real |

## Contador Atual

- Traduzidas confirmadas: 28 / 362
- Faltam: 334
- Admin raiz traduzidas: 28 / 120
- Admin raiz faltam: 92
- Admin total incluindo `[tenant]/admin`: 28 / 127
- Admin total incluindo `[tenant]/admin` faltam: 99
