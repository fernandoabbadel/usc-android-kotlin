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
| 29 | `web-reference/src/app/admin/loja/page.tsx` | `AdminStoreScreen`, `AdminStoreViewModel`, `SupabaseAdminStoreRepository` | M9: hub com PIX geral da loja em `app_config` e os seis cards, todos navegando para subrotas já traduzidas |
| 30 | `web-reference/src/app/admin/loja/categorias/page.tsx` | `AdminStoreCategoriesScreen`, `AdminStoreCategoriesViewModel`, `SupabaseAdminStoreRepository` | M9: fechou com upload de capa via Storage (2MB/2400px, WEBP 200KB, caminho por tenant) e a logo oficial da liga vinda de `ligas_config` com teto 80 |
| 31 | `web-reference/src/app/admin/loja/produtos/page.tsx` | `AdminStoreProductsScreen`, `AdminStoreProductsViewModel`, `SupabaseAdminStoreRepository` | M9: fechou com upload de imagem, recebedores (`app_config` `product_payment_receivers` + diretório de membros aprovados) e preço/visibilidade por plano (`plan_prices`/`plan_visibility`, catálogo de 40) |
| 32 | `web-reference/src/app/admin/loja/produtos-desativados/page.tsx` | `AdminStoreProductsScreen`, rota `admin/loja/produtos-desativados` | M9: no web é o mesmo componente decidindo por `pathname.endsWith("/produtos-desativados")` (306); o app usa `inactiveOnly=true`, com `active=false` na consulta |
| 33 | `web-reference/src/app/admin/loja/pedidos-pendentes/page.tsx` | `AdminStoreOrdersScreen`, `AdminStoreOrdersViewModel`, `SupabaseAdminStoreRepository` | M9: aprovação real com estoque, XP/selos, notificação, fichas do modo vendas e estoque de variação — o caminho direto que o web executa, sem Edge Function |
| 34 | `web-reference/src/app/admin/loja/pedidos-aprovados/page.tsx` | `AdminStoreOrdersScreen`, `AdminStoreOrdersViewModel`, `SupabaseAdminStoreRepository` | M9: mesmo componente com `mode="approved"`, reabrir e marcar entregue reais |
| 35 | `web-reference/src/app/admin/loja/pedidos-pendentes/[categoria]/page.tsx` | `AdminStoreOrdersScreen`, rota `admin/loja/pedidos-pendentes/{category}` | M9: filtro por `productId` da categoria, como o `categoryLabel` do wrapper web |
| 36 | `web-reference/src/app/admin/loja/pedidos-aprovados/[categoria]/page.tsx` | `AdminStoreOrdersScreen`, rota `admin/loja/pedidos-aprovados/{category}` | M9: idem, com `mode="approved"` |

## Próximas Rotas Admin

1. `web-reference/src/app/admin/eventos/page.tsx` (M10, 26 `page.tsx` no diretório)
2. `web-reference/src/app/admin/usuarios/page.tsx`
3. `web-reference/src/app/admin/permissoes/page.tsx`
4. `web-reference/src/app/admin/carteirinha/page.tsx`
5. `web-reference/src/app/admin/fidelidade/page.tsx`
6. `web-reference/src/app/admin/boardround/page.tsx`
7. `web-reference/src/app/admin/guia/page.tsx`

## Em Andamento, Ainda Não Contadas

Estas páginas já têm código Kotlin/Supabase iniciado, mas ainda não entram nas traduzidas confirmadas porque dependem de subrotas/componentes relacionados, Storage, fluxo sensível ou paridade visual/funcional ainda incompleta.

| Web `page.tsx` | Android Kotlin | Pendente Para Contar |
|---|---|---|
| `web-reference/src/app/dashboard/page.tsx` | `HomeScreen`, `HomeViewModel`, `SupabaseHomeDashboardRepository` | Bundle real, tenant guest e 47 módulos efetivos portados; ainda faltam tema dinâmico, interações, cardápio nativo do evento e QA visual em aparelho |
| `web-reference/src/app/[tenant]/dashboard/page.tsx` | Mesma implementação da Home Android | O tenant explícito já chega à Home após revalidação; ainda faltam landing intermediária e Android App Link equivalentes ao `tenantSlugOverride`, além das pendências da Home raiz |
| `web-reference/src/app/admin/usuarios/page.tsx` | `AdminUsersScreen`, `AdminUsersViewModel`, `SupabaseAdminUsersRepository` | Decidir fluxo seguro para `Recontar Follows`, que no web usa callable administrativa. Não foi criada Edge Function e não há fallback mock |
| `web-reference/src/app/admin/mini-vendors/aprovacoes/page.tsx` | `AdminMiniVendorsScreen`, `AdminMiniVendorsViewModel`, `SupabaseAdminMiniVendorsRepository` | Aprovação/rejeição real em Supabase foi portada, mas links internos de edição/produtos/pedidos do mini-vendor ainda não foram traduzidos |
| `web-reference/src/app/admin/mini-vendors/cadastros/page.tsx` | `AdminMiniVendorsScreen`, `AdminMiniVendorsViewModel`, `SupabaseAdminMiniVendorsRepository` | Diretório real em Supabase foi portado, mas links internos de página pública, edição, produtos e pedidos ainda não foram traduzidos |
| `web-reference/src/app/admin/parceiros/empresas/page.tsx` | `AdminPartnersCompaniesScreen`, `AdminPartnersViewModel`, `SupabasePartnersRepository` | Lista, busca, filtro, criação/edição, status e reset de senha foram portados com Supabase direto em `parceiros`, mas ainda não entra no contador porque o upload de logo/capa via Storage e a rota pública `/empresa/[id]` ainda não foram traduzidos |
| `web-reference/src/app/parceiros/page.tsx` | `PartnersScreen`, `PartnersViewModel`, `SupabasePartnersRepository` | Mock público removido e leitura real em `parceiros` com `tenant_id`/`status=active`, mas ainda não entra no contador porque faltam agrupamento visual por plano, busca e link real para `/empresa` |
| `web-reference/src/app/parceiros/[id]/page.tsx` | `PartnerDetailScreen`, `PartnerBenefitsScreen`, `PartnersViewModel`, `SupabasePartnersRepository` | Mock de detalhe removido e busca real por `id + tenant_id`, mas ainda não entra no contador porque o web também ativa cupom/gera QR e grava scan com `createPartnerScan` |
| `web-reference/src/app/admin/planos/historico/page.tsx` | `AdminPendingRouteScreen`, rota `admin/planos/historico` | Rota registrada para não quebrar navegação, mas ainda pendente de tradução real |
| `web-reference/src/app/admin/planos/editar/page.tsx` | `AdminPendingRouteScreen`, rota `admin/planos/editar` | Rota registrada para não quebrar navegação, mas ainda pendente de tradução real |
| `web-reference/src/app/admin/gestao/treinos/page.tsx` | `AdminBiSnapshotScreen` com `focus=Training`, rota `admin/gestao/treinos` | `<AdminBiDashboard mode="treinos" />` **ficou fora do M8, por decisão registrada** — a rota mostra um resumo herdado de módulo anterior, que não é o painel do web. Ver "Decisão sobre `mode=treinos`" em `ANDROID_PROGRESS.md` |
| `web-reference/src/app/admin/gestao/financeiro/page.tsx` | `AdminPendingRouteScreen`, rota `admin/gestao/financeiro` | Rota registrada para não quebrar navegação, mas ainda pendente de tradução real |
| `web-reference/src/app/admin/album/caca_calouro/page.tsx` | `AdminPendingRouteScreen`, rota `admin/album/caca_calouro` | Rota registrada para não quebrar navegação, mas ainda pendente de tradução real |
| `web-reference/src/app/admin/album/pontua_calouro/page.tsx` | `AdminPendingRouteScreen`, rota `admin/album/pontua_calouro` | Rota registrada para não quebrar navegação, mas ainda pendente de tradução real |
| `web-reference/src/app/admin/album/pontua_geral/page.tsx` | `AdminPendingRouteScreen`, rota `admin/album/pontua_geral` | Rota registrada para não quebrar navegação, mas ainda pendente de tradução real |
| `web-reference/src/app/admin/album/customizacao/page.tsx` | `AdminPendingRouteScreen`, rota `admin/album/customizacao` | Rota registrada para não quebrar navegação, mas ainda pendente de tradução real |
| `web-reference/src/app/admin/turma/page.tsx` | `AdminPendingRouteScreen`, rota `admin/turma` | Rota registrada para não quebrar navegação, mas ainda pendente de tradução real |

## Contador Atual

- Traduzidas confirmadas: 64 / 362
- Faltam: 298
- Admin raiz traduzidas: 45 / 120
- Admin raiz faltam: 75
- Admin total incluindo `[tenant]/admin`: 45 / 127
- Admin total incluindo `[tenant]/admin` faltam: 82

O M9 somou **8** rotas, todas no admin raiz: o hub `admin/loja`, `categorias`, `produtos`,
`produtos-desativados` e as quatro de pedidos (pendentes/aprovados, com e sem categoria).
`admin/loja/review` já estava no contador desde antes e não soma de novo.

As 25 rotas do BI de Eventos (5 do hub do M8.1 + 20 visões do M8.2) entraram no contador no M8.2:
os cards do hub passaram a navegar e as cinco visões renderizam as 238 métricas do motor. Do lado
do admin raiz entraram 7: `admin/bi`, `admin/gestao/eventos` e as cinco `admin/bi/{visão}`.

O M8.3/M8.4 somou **3** rotas ao contador (`admin/gestao/loja`, `admin/gestao/produtos` e
`configuracoes/mini-vendor/gestao`), sendo 2 no admin raiz. As três rotas de `gestao/produtos` dos
coletivos **já estavam no contador desde o M7** e não somam de novo — o que mudou nelas é a
profundidade: saíram da versão reduzida (5 indicadores) para o motor completo.

## M6 - Coletivos, área pública (18 rotas)

Fonte única: `ligas_config` normalizada por `web-reference/src/lib/leaguesService.ts`.
Android: `SupabaseCollectivesRepository`, `CollectiveCatalogViewModel`, `CollectiveDetailViewModel`,
`CollectiveCatalogScreen`, `CollectiveDetailScreen`.

| # | Web `page.tsx` | Android | Status |
|---:|---|---|---|
| 1 | `app/ligas_usc/page.tsx` | `CollectiveCatalogScreen` (`leagues`) | Catálogo real por `likes desc`/60, curtir, seguir e Oráculo com `quiz_history` |
| 2 | `app/ligas_usc/[leagueId]/page.tsx` | `CollectiveDetailScreen` aba Visão geral | Visão geral, links e pagamento reais |
| 3 | `app/ligas_usc/[leagueId]/membros/page.tsx` | aba Membros | `ligas_config.membros` ordenado por cargo |
| 4 | `app/ligas_usc/[leagueId]/agenda/page.tsx` | aba Agenda | Público/interno com hidratação do evento global |
| 5 | `app/ligas_usc/[leagueId]/loja/page.tsx` | aba Loja | `categorias` + `produtos` por `seller_id` |
| 6 | `app/comissoes/page.tsx` | `CollectiveCatalogScreen` (`commissions`) | Ordenado por vendas/exposição/likes/nome, membros por turma |
| 7 | `app/comissoes/[leagueId]/page.tsx` | aba Visão geral | Idem liga, rótulo "Representação oficial" |
| 8 | `app/comissoes/[leagueId]/membros/page.tsx` | aba Membros | Só a diretoria, como no web |
| 9 | `app/comissoes/[leagueId]/agenda/page.tsx` | aba Agenda | Interno liberado por cargo ou pela turma |
| 10 | `app/comissoes/[leagueId]/loja/page.tsx` | aba Loja | Idem liga |
| 11 | `app/diretorio/page.tsx` | `AppRoute.Directory` | Registro primário (`fetchPrimaryLeagueRecord`), aba Visão geral |
| 12 | `app/diretorio/membros/page.tsx` | `directory-root-members` | Registro primário, aba Membros |
| 13 | `app/diretorio/agenda/page.tsx` | `directory-root-agenda` | Registro primário, aba Agenda |
| 14 | `app/diretorio/loja/page.tsx` | `directory-root-store` | Registro primário, aba Loja |
| 15 | `app/diretorio/[leagueId]/page.tsx` | `directory-detail/{id}` | Aba Visão geral |
| 16 | `app/diretorio/[leagueId]/membros/page.tsx` | `directory-members/{id}` | Aba Membros |
| 17 | `app/diretorio/[leagueId]/agenda/page.tsx` | `directory-agenda/{id}` | Aba Agenda |
| 18 | `app/diretorio/[leagueId]/loja/page.tsx` | `directory-store/{id}` | Aba Loja |

Pendência única do M6: `POST /api/ligas/member-requests` (solicitar entrada/acesso) roda com
service role no web e não foi portado. O card de participação mostra o estado real e explica isso.

## M7 - Gestão dos coletivos (38 rotas)

As três áreas compartilham os mesmos componentes no web (`LigasAdminPageContent`,
`LeagueStoreAdminPage`, `LeagueFinanceDashboard`, `LeagueFrequencyPage`,
`FinancialStatementPage`), variando só `entityLabel`, `entityArticle`, `storageNamespace` e
`showBoard`. O Android reproduz isso com um único conjunto de telas em
`ui/collectives/management/`, parametrizado por `CollectiveKind`.

Rotas Android: `league-manage`, `commission-manage` e `directory-manage`, cada uma com
`{collectiveId}/{section}`. `section` aceita `inicio`, `informacoes`, `membros`, `loja`,
`loja-produtos`, `loja-pendentes`, `loja-aprovados`, `gestao`, `gestao-produtos`,
`gestao-frequencia` e `gestao-financeiro`.

| # | Web `page.tsx` | Android (seção) | Status |
|---:|---|---|---|
| 1 | `app/ligas/page.tsx` | `league-manage` | Gate + seleção + hub, com `fetchManagedLeagueSummaries` real |
| 2 | `app/ligas/[leagueId]/page.tsx` | `league-manage/{id}/inicio` | Hub com os 6 cards e o quick nav de 7 itens |
| 3 | `app/ligas/informacoes/page.tsx` | `league-manage/{id}/informacoes` | Escrita real em `ligas_config` |
| 4 | `app/ligas/membros/page.tsx` | `league-manage/{id}/membros` | Escrita real em `ligas_config` + `ligas_membros` |
| 5 | `app/ligas/[leagueId]/membros/page.tsx` | idem, com id na rota | Mesma tela |
| 6 | `app/ligas/[leagueId]/loja/page.tsx` | `league-manage/{id}/loja` | Categoria, capa, cor e visibilidade |
| 7 | `app/ligas/[leagueId]/loja/produtos/page.tsx` | `league-manage/{id}/loja-produtos` | CRUD de produto com `seller_id` do coletivo |
| 8 | `app/ligas/[leagueId]/loja/pedidos-pendentes/page.tsx` | `league-manage/{id}/loja-pendentes` | Aprovar/rejeitar reais |
| 9 | `app/ligas/[leagueId]/loja/pedidos-aprovados/page.tsx` | `league-manage/{id}/loja-aprovados` | Reabrir/entregue reais |
| 10 | `app/ligas/[leagueId]/gestao/page.tsx` | `league-manage/{id}/gestao` | 4 métricas + 4 atalhos + scanner |
| 11 | `app/ligas/[leagueId]/gestao/produtos/page.tsx` | `league-manage/{id}/gestao-produtos` | BI de produtos |
| 12 | `app/ligas/[leagueId]/gestao/frequencia/page.tsx` | `league-manage/{id}/gestao-frequencia` | Matriz de presença |
| 13 | `app/ligas/[leagueId]/gestao/financeiro/page.tsx` | `league-manage/{id}/gestao-financeiro` | Extrato do coletivo |
| 14 | `app/ligas/[leagueId]/informacoes/page.tsx` | `league-manage/{id}/informacoes` | Mesma tela com id na rota |
| 15-26 | `app/comissoes/configurar/**` (12 rotas, inclui `[leagueId]/[[...section]]` e `[leagueId]/gestao/financeiro`) | `commission-manage/**` | Mesmas telas, sem Board Round, `memberScope = turma` |
| 27-38 | `app/diretorio/configurar/**` (12 rotas) | `directory-manage/**` | Mesmas telas, sem Board Round, artigo "do" |

Ficaram de fora do escopo do M7 as 4 rotas de agenda (`ligas/eventos`, `ligas/[leagueId]/eventos`
e as equivalentes de comissão/diretório, que abrem o workspace de evento do M8/M10) e as 2 de
Board Round (`ligas/board-round`, `ligas/[leagueId]/board-round`, que pertencem ao módulo de jogos).

Pendências do M7, com motivo em `ANDROID_PROGRESS.md`: upload de imagem (Storage), ajuste manual de
frequência (service role no web), aba Agenda/workspace de evento (M8/M10) e variações de produto.
O BI de eventos do coletivo saiu da lista no M8.1.

## M8.1 - Motor do BI de Eventos + hub (5 rotas)

O web já parametriza o BI por escopo: as cinco rotas abaixo chamam o **mesmo**
`AdminEventBiDashboard` com `view="inicio"`, mudando só `lockedScopeType`, `lockedScopeId`,
`scopeLabel`, `contextTitle`, `contextLogo` e `contextEyebrow`. O Android reproduz isso com uma
tela (`ui/bi/EventBiScreen.kt`), um ViewModel e um repositório
(`data/repository/SupabaseEventBiRepository.kt`) para os quatro players.

`CollectiveManagementSection` ganhou a seção `gestao-eventos`, e o card "Eventos" da gestão do
coletivo — que no M7 apontava para `gestao-produtos` como placeholder — passou a abri-la.

| # | Web `page.tsx` | Android (rota/seção) | Status |
|---:|---|---|---|
| 1 | `app/admin/bi/page.tsx` | `admin/bi` | Escopo tenant, sem props de contexto |
| 2 | `app/admin/gestao/eventos/page.tsx` | `admin/gestao/eventos` | Mesma tela da rota 1 |
| 3 | `app/ligas/[leagueId]/gestao/eventos/page.tsx` | `league-manage/{id}/gestao-eventos` | `lockedScopeType="league"`, "da liga" |
| 4 | `app/comissoes/configurar/gestao/eventos/page.tsx` | `commission-manage/{id}/gestao-eventos` | `commission`, "da comissão" |
| 5 | `app/diretorio/configurar/gestao/eventos/page.tsx` | `directory-manage/{id}/gestao-eventos` | `directory`, "do diretório" |

### M8.1b - motor de métricas (nenhuma rota nova)

O M8.1b não abre rota: ele preenche o `analytics` que as cinco rotas acima carregam e que as 20
rotas do M8.2 vão consumir. As cinco rotas continuam **fora do contador**, porque os cards do hub
só passam a navegar no M8.2.

Uma regra de rota entrou aqui: `eventOwnerRedirectHref` (web 6622) + `canonicalEventWorkspacePath`
(1967). Quando o evento selecionado pertence a outro portal, o web faz `router.replace` para
`/ligas/{id}/eventos/{ev}/edicao`, `/comissoes/configurar/{id}/...`, `/diretorio/configurar/{id}/...`
ou `/admin/eventos/{ev}/...`. Esses destinos são o workspace de evento, que é o **M10**. O app
calcula o caminho (`EventBiOwnerRedirect.webPath`) e mostra o banner "Este evento pertence a outro
portal", sem navegar — a navegação liga quando o M10 existir.

As cinco rotas ficam fora do contador de traduzidas enquanto os cards do hub não navegarem: as
visões `comercial`, `operacional`, `portaria`, `estrategico` e `vendas` (15 rotas web nos três
coletivos + 5 no admin) são o M8.2.

## M8.2 - As cinco visões analíticas do BI de Eventos (20 rotas)

As 20 rotas abaixo são o mesmo `AdminEventBiDashboard` das 5 do M8.1, mudando só `view`. No
Android elas reaproveitam `EventBiScreen`, `EventBiViewModel` e `SupabaseEventBiRepository`: o que
muda é o `EventBiView` que a rota passa, e a tela escolhe entre `EventBiCommercialView`,
`EventBiOperationalView`, `EventBiGateView`, `EventBiStrategicView` e `EventBiSalesView`.

`CollectiveManagementSection` ganhou cinco seções (`gestao-eventos-comercial`,
`-operacional`, `-portaria`, `-estrategico`, `-vendas`), e os cinco cards do hub passaram a
navegar nos quatro players.

| # | Web `page.tsx` | Android (rota/seção) | Status |
|---:|---|---|---|
| 1 | `app/admin/bi/comercial/page.tsx` | `admin/bi/comercial` | Substituiu o `AdminBiSnapshotScreen` que ocupava a rota |
| 2 | `app/admin/bi/operacional/page.tsx` | `admin/bi/operacional` | Substituiu o `AdminBiSnapshotScreen` |
| 3 | `app/admin/bi/portaria/page.tsx` | `admin/bi/portaria` | Substituiu o `AdminBiSnapshotScreen` |
| 4 | `app/admin/bi/estrategico/page.tsx` | `admin/bi/estrategico` | Rota nova |
| 5 | `app/admin/bi/vendas/page.tsx` | `admin/bi/vendas` | Rota nova |
| 6 | `app/ligas/[leagueId]/gestao/eventos/comercial/page.tsx` | `league-manage/{id}/gestao-eventos-comercial` | `lockedScopeType="league"` |
| 7 | `app/ligas/[leagueId]/gestao/eventos/operacional/page.tsx` | `league-manage/{id}/gestao-eventos-operacional` | idem |
| 8 | `app/ligas/[leagueId]/gestao/eventos/portaria/page.tsx` | `league-manage/{id}/gestao-eventos-portaria` | idem |
| 9 | `app/ligas/[leagueId]/gestao/eventos/estrategico/page.tsx` | `league-manage/{id}/gestao-eventos-estrategico` | idem |
| 10 | `app/ligas/[leagueId]/gestao/eventos/vendas/page.tsx` | `league-manage/{id}/gestao-eventos-vendas` | idem |
| 11 | `app/comissoes/configurar/gestao/eventos/comercial/page.tsx` | `commission-manage/{id}/gestao-eventos-comercial` | `commission` |
| 12 | `app/comissoes/configurar/gestao/eventos/operacional/page.tsx` | `commission-manage/{id}/gestao-eventos-operacional` | idem |
| 13 | `app/comissoes/configurar/gestao/eventos/portaria/page.tsx` | `commission-manage/{id}/gestao-eventos-portaria` | idem |
| 14 | `app/comissoes/configurar/gestao/eventos/estrategico/page.tsx` | `commission-manage/{id}/gestao-eventos-estrategico` | idem |
| 15 | `app/comissoes/configurar/gestao/eventos/vendas/page.tsx` | `commission-manage/{id}/gestao-eventos-vendas` | idem |
| 16 | `app/diretorio/configurar/gestao/eventos/comercial/page.tsx` | `directory-manage/{id}/gestao-eventos-comercial` | `directory` |
| 17 | `app/diretorio/configurar/gestao/eventos/operacional/page.tsx` | `directory-manage/{id}/gestao-eventos-operacional` | idem |
| 18 | `app/diretorio/configurar/gestao/eventos/portaria/page.tsx` | `directory-manage/{id}/gestao-eventos-portaria` | idem |
| 19 | `app/diretorio/configurar/gestao/eventos/estrategico/page.tsx` | `directory-manage/{id}/gestao-eventos-estrategico` | idem |
| 20 | `app/diretorio/configurar/gestao/eventos/vendas/page.tsx` | `directory-manage/{id}/gestao-eventos-vendas` | idem |

### O que ainda liga essas rotas ao M10

Nenhum indicador destas 20 rotas navega para fora: `buildStatementHref` e `buildCheckinsHref`
levam ao workspace de evento (`/admin/eventos/{id}/extrato` e `/checkins`, ou o equivalente do
coletivo), que é o M10. Com `EventBiLinkBuilder.Inert` o href chega vazio, o rodapé de
`FilterLinkChips` não renderiza e a célula de tabela não vira link — o número continua correto.
O banner "Este evento pertence a outro portal" segue calculando o caminho sem navegar, pelo mesmo
motivo. Trocar o `EventBiLinkBuilder` no M10 liga tudo de uma vez.

## M8.3 e M8.4 - BI Loja nos cinco players (3 rotas novas + 3 aprofundadas)

Fonte: `web-reference/src/components/ProductManagementAnalytics.tsx` (634 linhas), o componente
reutilizável, e seus três consumidores — `AdminBiDashboard` com `mode="produtos"` (tenant),
`LeagueFinanceDashboard` com `view="produtos"` (liga/comissão/diretório) e
`app/configuracoes/mini-vendor/gestao/page.tsx` (mini-vendor).

Android: `domain/model/ProductBi.kt`, `domain/model/ProductBiEngine.kt`,
`domain/repository/ProductBiRepository.kt`, `data/repository/SupabaseProductBiRepository.kt`,
`ui/bi/store/` (`ProductBiScreen`, `ProductBiView`, `ProductBiViewModel`) e
`ui/bi/charts/ProductBiGroupedBars.kt`. Um motor, um repositório e uma tela para os cinco
players; o escopo é parâmetro (`ProductBiScope`).

| # | Web `page.tsx` | Android (rota/seção) | Status |
|---:|---|---|---|
| 1 | `app/admin/gestao/loja/page.tsx` | `admin/gestao/loja` | **Nova no contador.** Substituiu o `AdminBiSnapshotScreen` que ocupava a rota |
| 2 | `app/admin/gestao/produtos/page.tsx` | `admin/gestao/produtos` | **Nova no contador.** Rota não existia no app; no web é o mesmo `mode="produtos"` da rota 1 |
| 3 | `app/configuracoes/mini-vendor/gestao/page.tsx` | `mini-vendor-management` | **Nova no contador.** Substituiu o resumo reduzido que ocupava a rota |
| 4 | `app/ligas/[leagueId]/gestao/produtos/page.tsx` | `league-manage/{id}/gestao-produtos` | Já contada no M7; trocou a versão reduzida pelo motor completo |
| 5 | `app/comissoes/configurar/gestao/produtos/page.tsx` | `commission-manage/{id}/gestao-produtos` | idem, `entityArticle="da"`, `entityLabel="comissão"` |
| 6 | `app/diretorio/configurar/gestao/produtos/page.tsx` | `directory-manage/{id}/gestao-produtos` | idem, `"do"`, `"diretório"` |

### O que o M8.3 declarou e não portou: `EventManagementAnalytics`

O M8.3 previa um segundo motor, `EventBiSummaryEngine`, porte de
`web-reference/src/components/EventManagementAnalytics.tsx` (1572 linhas), "para os 4 players com
eventos". A auditoria mostrou que o componente **não tem consumidor vivo no web**. As três
referências:

| Referência | Situação |
|---|---|
| `LeagueFinanceDashboard.tsx:762` (`view === "eventos"`) | Nenhuma página passa `view="eventos"`. As rotas existentes passam só `"hub"` e `"produtos"`, e o catch-all `[leagueId]/[[...section]]` manda `gestao/eventos` para `CommissionManagementEventBiPage` (o `AdminEventBiDashboard`, já portado no M8.1/M8.2) |
| `LeagueFinanceDashboard.tsx:898` | Dentro do bloco `{false ? <> ... </> : null}` das linhas 774-1001 |
| `AdminBiDashboard.tsx:757` (`EventsBi`, `mode === "eventos"`) | Nenhuma página monta `<AdminBiDashboard mode="eventos" />`. As três que existem passam `mode="produtos"` (duas) e `mode="treinos"` (uma) |

Portar o componente colocaria no app uma tela que o web não mostra, em rotas que não existem — a
mesma armadilha do `{false && ...}` do M8.2 e do `{false ? ... : null}` do M7. **Não foi portado**,
por decisão registrada. Os quatro players com eventos continuam servidos pelo `AdminEventBiDashboard`
(M8.1/M8.2), que é o painel que o web de fato abre em `gestao/eventos`.
