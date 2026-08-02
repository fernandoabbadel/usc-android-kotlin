# Progresso Android USC

## Integração Supabase inicial - Eventos públicos

- `web-reference/src/lib/eventsNativeService.ts`, `web-reference/src/lib/eventsService.ts`, `web-reference/src/app/eventos/EventosPageContent.tsx`, `web-reference/src/app/eventos/EventosClientPage.tsx` e `web-reference/src/app/eventos/[id]/page.tsx` foram usados como fonte.
- Android ganhou `SupabaseEventsRepository` como implementação real de `EventsRepository`.
- `EventsViewModel` e `EventDetailViewModel` agora usam Supabase real como padrão, sem fallback silencioso para `MockEventsRepository`.
- A listagem pública consulta `eventos` por `tenant_id`, `status = ativo`, ordenação por `data`, limite de busca e recorte de página.
- O detalhe consulta `eventos` por `id` e `tenant_id`.
- O escopo do organizador é derivado dos campos reais encontrados no web app: `tipo`, `categoria` e `stats.leagueId`; não foi encontrado campo explícito `owner_type`/`organizer_type` nas consultas web analisadas.
- `Event` agora carrega `tenantId`, `saleStatus`, `imageUrl`, `ownerType`, `ownerId`, `ownerName` e `likesCount`.
- Checkout, ingressos, pedidos, QR, baixa, pagamento, financeiro e split de evento não foram implementados; a rota de checkout mostra estado bloqueado até clonagem segura do fluxo web.
- Nenhuma Edge Function, Realtime, Storage novo, câmera ou serviço pago foi adicionado.
- Validação executada:
  - `.\gradlew.bat :app:compileDebugKotlin --no-daemon --console=plain --stacktrace`
  - `.\gradlew.bat :app:assembleDebug --no-daemon --console=plain`

## Direção Obrigatória

- Não invente tela. Replique a tela web em Compose.
- `web-reference` é a fonte visual obrigatória.
- O Android deve ser nativo em Kotlin/Jetpack Compose.
- Não usar WebView ou wrapper.
- Usar Supabase real direto quando for seguro, com Auth + RLS e tenant_id obrigatório.
- Nunca colocar service_role, segredo de pagamento, token privado, `.env` ou credencial sensível no app.
- Não fazer push sem ordem explícita.

## Fase 1 - Fundação Kotlin/Compose

- Kotlin e Jetpack Compose habilitados no projeto Android.
- Material 3 e Navigation Compose adicionados.
- `MainActivity`, tema Compose e tela inicial nativa criados.
- Build validado com `assembleDebug`.
- Commit local: `0432ca1 Set up Kotlin Compose Android foundation`.

## Fase 2 - Estrutura de arquitetura

- Pacotes iniciais criados em `core`, `domain`, `data`, `navigation` e `ui`.
- Modelos base de módulos, roles, sessão, tenant, permissões, erros e resultados adicionados.
- Dashboard nativo com placeholders navegáveis para módulos Android v1/v2.
- Build e teste unitário validados.
- Commit local: `ef00691 Add Android architecture module skeleton`.

## Fase 3 - Autenticação mockada e RouteGuard

- Telas nativas criadas em Jetpack Compose:
  - `LoginScreen`
  - `RegisterScreen`
  - `WaitingApprovalScreen`
  - `InviteRequiredScreen`
  - `BannedUserScreen`
  - `AccountSecurityScreen`
- `AuthUiState` e `AuthViewModel` mockado adicionados.
- `AuthRepository` preparado para integração futura com Supabase.
- `MockAuthRepository` implementa cenários locais de usuário autenticado, aguardando aprovação, sem convite e banido.
- `RouteGuard` nativo redireciona conforme `AuthStatus`.
- Navigation Compose integrado ao fluxo de autenticação.
- Nenhuma URL, anon key, service role ou segredo foi adicionado.
- Commit local: `5b2d5d6 Add native auth session and route guard flow`.

## Fase 4 - Home, Perfil, Configurações e Carteirinha

- Home/Dashboard, Perfil, Configurações e Carteirinha criados inicialmente com dados mockados.
- `HomeViewModel`, `HomeUiState`, `ProfileViewModel`, `SettingsViewModel` e `MembershipCardViewModel` adicionados.
- Navigation Compose atualizado para substituir placeholders por telas reais.
- Commit local: `d857d6b Add home profile settings and membership card screens`.

## Fase 4.1 - Correção de paridade visual da Home

- Implementação de novas funcionalidades pausada para corrigir a direção visual da Home Android.
- Home refeita para se aproximar da dashboard mobile web:
  - fundo preto premium;
  - identidade neon verde/dourada;
  - header com “Fala, Fernando!” e “Pronto para dominar?”;
  - avatar circular no topo direito;
  - cards verticais grandes com gradiente, imagem e bordas arredondadas grandes;
  - card “Modo vendas / Menu do evento”;
  - card “Carteirinha” com imagem de fundo e visual premium;
  - card “Caça aos Calouros” com visual de radar neon;
  - bottom navigation flutuante com Início, Eventos, Scanner central, Carteira e Menu.
- Assets públicos da referência web copiados para `app/src/main/res/drawable-nodpi`.
- Commit local: `842d5e9 Improve dashboard visual parity with web app`.

## Fase 5 - Eventos, Ingressos, QR e Pedidos

- Modelos de domínio criados:
  - `Event`
  - `EventStatus`
  - `EventProduct`
  - `EventTicket`
  - `TicketStatus`
  - `EventOrder`
  - `OrderStatus`
  - `PaymentStatus`
- Interfaces e repositórios mockados criados para eventos, ingressos e pedidos.
- Telas nativas criadas:
  - `EventsScreen`
  - `EventDetailScreen`
  - `EventCheckoutScreen`
  - `EventTicketsScreen`
  - `EventTicketDetailScreen`
  - `EventOrdersScreen`
  - `EventOrderDetailScreen`
- Navigation Compose integrado para lista, detalhe, checkout, ingressos, detalhe de ingresso, pedidos e detalhe de pedido.
- Commit local: `7169aba Add events tickets QR and event orders screens`.

## Fase 5.1 - Revisão visual das fases anteriores

- Revisão feita a partir de `web-reference`, principalmente:
  - `login/LoginPageClient.tsx`
  - `cadastro/page.tsx`
  - `carteirinha/page.tsx`
  - `configuracoes/page.tsx`
  - `eventos/EventosClientPage.tsx`
  - `eventos/[id]/page.tsx`
  - `components/BottomNav.tsx`
  - `globals.css`
- Base visual Compose criada em `PremiumComponents.kt`, com:
  - fundo dark premium;
  - cards `rounded-3xl`;
  - chips neon;
  - botões sólidos e outline;
  - inputs escuros;
  - header mobile;
  - QR visual;
  - menu rows no estilo web.
- Telas de autenticação revisadas visualmente:
  - `LoginScreen`
  - `RegisterScreen`
  - `WaitingApprovalScreen`
  - `InviteRequiredScreen`
  - `BannedUserScreen`
  - `AccountSecurityScreen`
- Telas de conta revisadas visualmente:
  - `ProfileScreen`
  - `SettingsScreen`
  - `MembershipCardScreen`
  - `MembershipCard`
- Telas de eventos, ingressos e pedidos revisadas visualmente:
  - `EventsScreen`
  - `EventDetailScreen`
  - `EventCheckoutScreen`
  - `EventTicketsScreen`
  - `EventTicketDetailScreen`
  - `EventOrdersScreen`
  - `EventOrderDetailScreen`
  - `EventCard`
  - `TicketCard`
  - `EventOrderCard`
- Previews principais atualizados para visual dark/premium.
- Validação executada:
  - `.\gradlew.bat :app:assembleDebug --no-daemon --console=plain`
  - `.\gradlew.bat :app:testDebugUnitTest --no-daemon --console=plain`

## Fase 6 - Loja, Planos, Treinos/Gym e Parceiros

- Bloco nativo criado em Kotlin/Jetpack Compose, mantendo a regra: não invente tela, replique a tela web em Compose.
- Referências visuais usadas:
  - `web-reference/src/app/loja`
  - `web-reference/src/app/carrinho/page.tsx`
  - `web-reference/src/app/checkout/page.tsx`
  - `web-reference/src/app/configuracoes/pedidos/loja/page.tsx`
  - `web-reference/src/app/configuracoes/pedidos/planos/page.tsx`
  - `web-reference/src/app/gym/page.tsx`
  - `web-reference/src/app/parceiros/page.tsx`
  - `web-reference/src/app/empresa`
- Telas de Loja criadas:
  - `StoreScreen`
  - `ProductDetailScreen`
  - `CartScreen`
  - `CheckoutScreen`
  - `StoreOrdersScreen`
  - `StoreOrderDetailScreen`
- Componentes/modelos de Loja criados:
  - `ProductCard`
  - `CartItemCard`
  - `StoreOrderCard`
  - `StoreUiState`
  - `StoreViewModel`
  - `CartUiState`
  - `CartViewModel`
  - `StoreOrdersUiState`
  - `StoreOrdersViewModel`
  - modelos mockados de produto, carrinho, pedido, status de pedido e pagamento.
- Telas de Planos criadas:
  - `PlansScreen`
  - `PlanDetailScreen`
  - `UserPlanStatusScreen`
  - `PlanOrdersScreen`
- Componentes/modelos de Planos criados:
  - `PlanCard`
  - `PlanBenefitChip`
  - `PlanUiState`
  - `PlansViewModel`
  - modelos mockados de plano, benefício, assinatura e pedidos.
- Telas de Treinos/Gym criadas:
  - `TrainingScreen`
  - `TrainingCheckInScreen`
  - `TrainingCheckInDetailScreen`
  - `TrainingFrequencyScreen`
  - `TrainingHistoryScreen`
- Componentes/modelos de Treinos criados:
  - `TrainingCard`
  - `TrainingUiState`
  - `TrainingViewModel`
  - modelos mockados de treino, check-in, frequência e histórico.
- Telas de Parceiros criadas:
  - `PartnersScreen`
  - `PartnerDetailScreen`
  - `PartnerBenefitsScreen`
- Componentes/modelos de Parceiros criados:
  - `PartnerCard`
  - `PartnerUiState`
  - `PartnersViewModel`
  - modelos mockados de parceiro, empresa, benefício e histórico.
- Navegação integrada:
  - Home/Dashboard abre Loja, Planos, Treinos/Gym e Parceiros.
  - Profile e Settings apontam para pedidos de loja, ingressos e pedidos de planos.
  - Rotas concretas substituem placeholders de `store`, `plans`, `training`, `gym` e `partners`.
- Previews criados/atualizados:
  - `StoreScreenPreview`
  - `ProductDetailScreenPreview`
  - `CartScreenPreview`
  - `CheckoutScreenPreview`
  - `StoreOrdersScreenPreview`
  - `StoreOrderDetailScreenPreview`
  - `ProductCardPreview`
  - `PlansScreenPreview`
  - `PlanDetailScreenPreview`
  - `UserPlanStatusScreenPreview`
  - `TrainingScreenPreview`
  - `TrainingCheckInScreenPreview`
  - `TrainingFrequencyScreenPreview`
  - `PartnersScreenPreview`
  - `PartnerDetailScreenPreview`
  - `PartnerCardPreview`
- Assets usados:
  - `logo_aaakn.png`
  - `logo_usc.png`
  - `logo_usc_wide.png`
  - `carteirinha_bg.jpg`
  - `logo_platform_web.webp`
  - `battle_forest.webp`
- Nenhuma URL secreta, anon key, service role, token, senha, `.env` ou segredo de pagamento foi adicionado.
- Validação executada:
  - `.\gradlew.bat :app:assembleDebug --no-daemon --console=plain`
  - `.\gradlew.bat :app:testDebugUnitTest --no-daemon --console=plain`
- Registro histórico da fase: o commit não foi criado por esta auditoria.

## Fase 7 a 10 - Módulos restantes com paridade visual

- Bloco nativo criado em Kotlin/Jetpack Compose para cobrir os módulos restantes sem WebView, wrapper ou segredo no app.
- Kit compartilhado criado:
  - `NativeModuleComponents.kt`
  - `NativeModuleHeroCard`
  - `NativeActionCard`
  - `NativeStatCard`
  - `NativeSectionTitle`
  - `NativeProgressBar`
- Comunidade criada:
  - `CommunityScreen`
  - `CommunityPostDetailScreen`
  - `CommunityPostCard`
  - `CommunityUiState`
  - `CommunityViewModel`
- Ligas, Diretório e Comissões criados:
  - `LeaguesScreen`, `LeagueDetailScreen`, `LeagueMembersScreen`, `LeagueAgendaScreen`, `LeagueStoreScreen`, `LeagueEventsScreen`, `LeagueInfoScreen`
  - `DirectoryScreen`, `DirectoryDetailScreen`, `DirectoryMembersScreen`, `DirectoryAgendaScreen`, `DirectoryStoreScreen`, `DirectoryEventsScreen`, `DirectoryInfoScreen`
  - `CommissionsScreen`, `CommissionDetailScreen`, `CommissionMembersScreen`, `CommissionAgendaScreen`, `CommissionStoreScreen`, `CommissionEventsScreen`
  - `LeagueCard`, `DirectoryCard`, `CommissionCard`
  - `LeagueUiState`, `DirectoryUiState`, `CommissionUiState`
  - `LeaguesViewModel`, `DirectoryViewModel`, `CommissionsViewModel`
- Tenant/Atlética preparado:
  - `TenantSwitcherScreen`
  - `TenantIdentityHeader`
  - `TenantThemePreviewCard`
  - `TenantUiState`
  - `TenantViewModel`
- Mini-vendor e Modo Vendas criados:
  - `MiniVendorScreen`
  - `MiniVendorProductsScreen`
  - `MiniVendorPendingOrdersScreen`
  - `MiniVendorApprovedOrdersScreen`
  - `MiniVendorFinanceScreen`
  - `SalesModeScreen`
  - `SalesModeEventMenuScreen`
  - `MiniVendorProductCard`
  - `MiniVendorOrderCard`
  - `MiniVendorUiState`
  - `MiniVendorViewModel`
- Scanner/check-in criado sem câmera real:
  - `ScannerScreen`
  - `ScannerResultSuccessScreen`
  - `ScannerResultErrorScreen`
  - `EventCheckInScannerScreen`
  - `PartyScannerScreen`
  - `ProductWithdrawalScannerScreen`
  - `ScannerPermissionDeniedScreen`
  - `ScannerUiState`
  - `ScannerViewModel`
- Guia, FAQ, Suporte, Termos e LGPD criados:
  - `GuideScreen`
  - `FaqScreen`
  - `ContactUscScreen`
  - `SupportScreen`
  - `TermsScreen`
  - `PrivacyLgpdScreen`
  - `LgpdRequestScreen`
  - `LegalDocumentScreen`
  - `GuideUiState`
  - `LegalUiState`
- Álbum, Galera e Caça-calouro criados:
  - `AlbumScreen`
  - `AlbumTurmaScreen`
  - `CacaCalouroScreen`
  - `CalouroRankingScreen`
  - `AlbumPhotoGrid`
  - `AlbumUiState`
  - `AlbumViewModel`
- Games, Boardround, Conquistas e Fidelidade criados:
  - `GamesScreen`
  - `BoardroundScreen`
  - `BoardroundRankingScreen`
  - `BoardroundStatsScreen`
  - `AchievementsScreen`
  - `LoyaltyScreen`
  - `GameRulesScreen`
  - `RankingCard`
  - `AchievementCard`
  - `LoyaltyCard`
  - `GamesUiState`
  - `GamesViewModel`
- Pedidos gerais criados:
  - `OrdersHubScreen`
  - `GeneralOrderDetailScreen`
  - `OrdersByTypeScreen`
  - `OrdersStatusTabs`
  - `GeneralOrderCard`
  - `GeneralOrdersViewModel`
- Roles, permissões e navegação revisadas:
  - novas permissões para pedidos, ligas, diretório, comissões, parceiros, tenant, guia, legal, álbum, games, conquistas e fidelidade.
  - novas roles mockadas para gestor de liga, diretório e comissão.
  - `AppRoute.kt` expandido para todos os módulos nativos.
  - `RemainingNativeRoutes.kt` criado para manter o `UscNavGraph.kt` organizado.
  - `SettingsUiState` atualizado para apontar para Mini-vendor, Modo Vendas, Suporte, Termos e LGPD nativos.
- Assets reais copiados do `web-reference/public`:
  - `turma1.jpeg` a `turma9.jpeg`
  - `capa_t1.jpg` a `capa_t9.jpeg`
- Previews principais criados/atualizados:
  - `CommunityScreenPreview`
  - `CommunityPostDetailScreenPreview`
  - `CommunityPostCardPreview`
  - `LeaguesScreenPreview`
  - `LeagueDetailScreenPreview`
  - `DirectoryScreenPreview`
  - `DirectoryDetailScreenPreview`
  - `CommissionsScreenPreview`
  - `CommissionDetailScreenPreview`
  - `TenantSwitcherScreenPreview`
  - `MiniVendorScreenPreview`
  - `SalesModeScreenPreview`
  - `ScannerScreenPreview`
  - `ScannerResultSuccessScreenPreview`
  - `GuideScreenPreview`
  - `TermsScreenPreview`
  - `AlbumScreenPreview`
  - `CacaCalouroScreenPreview`
  - `GamesScreenPreview`
  - `BoardroundScreenPreview`
  - `OrdersHubScreenPreview`
  - `GeneralOrderDetailScreenPreview`
- Validação executada:
  - `.\gradlew.bat :app:assembleDebug --no-daemon --console=plain`
  - `.\gradlew.bat :app:testDebugUnitTest --no-daemon --console=plain`
- Registro histórico da fase: o commit não foi criado por esta auditoria.

## QA Global - Navegação, visual e permissões

- Revisão global executada após o commit `8262890 Add remaining native modules with web visual parity`.
- Módulos revisados no Android nativo:
  - Auth, Home/Dashboard, Perfil, Configurações, Carteirinha, Eventos, Loja, Planos, Treinos/Gym, Parceiros, Comunidade, Ligas, Diretório, Comissões, Tenant, Mini-vendor, Modo vendas, Scanner, Guia, Legal/LGPD, Álbum, Games, Boardround, Conquistas, Fidelidade e Pedidos gerais.
- Navegação corrigida:
  - Home mantém bottom navigation flutuante com Início, Eventos, Scanner central, Carteirinha e Menu.
  - Registro histórico: o card de Modo Vendas apontava para `AppRoute.SalesMode`; hoje abre provisoriamente o detalhe do evento até existir a rota nativa exata do cardápio.
  - Registro histórico: o scanner central apontava sempre para `AppRoute.Scanner`; hoje abre o scanner administrativo somente para perfis autorizados e `CacaCalouro` para os demais membros.
  - Boardround usa `AppRoute.Boardround`.
  - Perfil ganhou atalho para Pedidos gerais.
  - Configurações virou hub real dos módulos nativos, incluindo Comunidade, Ligas, Diretório, Comissões, Atlética, Álbum, Games, Mini-vendor, Modo vendas, Scanner, Guia, Suporte, Termos e LGPD.
- Permissões mockadas revisadas:
  - `PermissionPolicy` agora valida `UseScanner`, `ManageMiniVendor` e `ManageTenant` por role.
  - Rotas de Mini-vendor, Modo vendas e Scanner exibem bloqueio premium quando a role mockada não permite acesso.
  - `MockAuthRepository` permite testar roles por e-mail mockado: `admin`, `vendas`, `mini`, `master`, `liga`, `diretorio` e `comissao`.
- Previews:
  - Os módulos principais continuam com previews dark/premium em Compose.
  - A revisão manteve previews sem rede e sem dependência de ViewModel real quando aplicável.
- Avisos mantidos:
  - Registro histórico: a integração começou em Auth/Tenant. No estado atual, Home, eventos públicos, catálogo público da loja, parceiros e vários fluxos admin já usam Supabase real, ainda com cobertura parcial.
  - Nenhuma URL secreta, anon key, service role, token, senha, `.env` ou segredo foi adicionado.
  - `web-reference` continua sendo fonte visual obrigatória e não deve ser editado.
- Validação executada nesta QA:
  - `.\gradlew.bat :app:assembleDebug --no-daemon --console=plain`
  - `.\gradlew.bat :app:testDebugUnitTest --no-daemon --console=plain`
- Registro histórico da fase: o commit não foi criado por esta auditoria.

## Próximas fases

1. Revisão visual fina com screenshots em Android Studio/emulador para comparar pixel a pixel contra `web-reference`.
2. Completar microinterações e estados vazios específicos por módulo.
3. Substituir os mocks restantes por Supabase real, módulo a módulo, sem expor segredos no app.

## Marco de paridade - Home/dashboard real

- `web-reference/src/app/dashboard/DashboardPageContent.tsx`, `web-reference/src/lib/dashboardPublicService.ts`, o drawer e a navegação inferior do web foram usados como fonte da verdade.
- `HomeViewModel` deixou de montar a tela com dados locais e passou a carregar `HomeDashboardBundle` por `SupabaseHomeDashboardRepository`.
- A fonte principal é a RPC existente `dashboard_public_home_bundle`; os fallbacks de eventos, produtos, parceiros, ligas, comunidade, treinos, membros e Caça aos Calouros filtram explicitamente o tenant recebido.
- O mapeamento preserva confirmação do usuário no evento, posição vertical da capa e ranking de turmas dos produtos.
- A ordem das seções, cards, carrosséis, parceiros ouro/prata/standard, Modo Vendas, carteirinha, treinos, Caça aos Calouros e barra inferior foram reestruturados para a referência mobile.
- O scanner central diferencia perfis autorizados e membros comuns; o Modo Vendas não envia mais o sócio para a área administrativa de mini-vendor.
- Coil 3.4.0 foi adicionado para imagens remotas, versão compatível com o compilador Kotlin atual do projeto.
- O visitante sem tenant agora é enviado ao diretório público real, seleciona uma atlética ativa revalidada por ID/slug/status e só então entra na Home. A sessão guest permanece em memória e não cria membership nem grava em `users`.
- A visibilidade usa os 47 módulos efetivos devolvidos pelo endpoint público do dashboard. Em falha, preserva bloqueios do último mapa e da configuração da tenant; sem fonte válida, bloqueia todos os módulos conhecidos.
- O marco ainda não é contado como rota concluída: faltam landing intermediária/App Links, persistência guest, identidade visual dinâmica, ações sociais, cardápio nativo específico do evento e QA visual em aparelho.
- Validação executada:
  - `.\\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon --console=plain`
  - build limpo concluído com sucesso; 46 tarefas executadas; 17 testes executados, sem falhas.

## Integração Supabase inicial - Auth/Tenant

- Gradle recebeu Supabase Auth, PostgREST e Ktor Android, sem Storage, Realtime ou Functions.
- `BuildConfig` lê `SUPABASE_URL`/`SUPABASE_ANON_KEY` ou `NEXT_PUBLIC_SUPABASE_URL`/`NEXT_PUBLIC_SUPABASE_ANON_KEY` via `local.properties` ou variáveis de ambiente.
- `SupabaseClientProvider` instala apenas Auth e PostgREST.
- `SupabaseAuthRepository` substitui o mock como padrão do `AuthViewModel`.
- Login Android foi alinhado ao web app: Google via Supabase Auth e visitante local.
- Sessão tenta restaurar o usuário atual e consulta colunas mínimas em `users`, `tenant_memberships` e `tenants`.
- Multi-tenant inicial usa `tenant_id`, status de membership e dados de tenant vindos do Supabase.
- Convite inicial usa a RPC existente `tenant_request_join_with_invite`, sem criar Edge Function nova.
- Cadastro/ficha de perfil, troca real de tenant e módulos de dados ainda estão pendentes.
- Validação executada:
  - `.\gradlew.bat :app:compileDebugKotlin --no-daemon --console=plain --stacktrace`
  - `.\gradlew.bat :app:assembleDebug --no-daemon --console=plain`

## Integração Supabase inicial - Loja pública

- `web-reference/src/lib/storePublicService.ts` foi usado como fonte para a loja pública.
- Android ganhou contrato `StoreCatalogRepository` e implementação `SupabaseStoreCatalogRepository`.
- A loja Android agora lê `categorias` e `produtos` diretamente do Supabase com `tenant_id`, `active = true`, `aprovado = true`, paginação de 20 itens e limite de categorias.
- O detalhe de produto agora busca `produtos` por `id` e `tenant_id`, sem fallback para o primeiro mock.
- Produtos carregam `seller_type`, `seller_id`, `seller_name` e `seller_logo_url` no model Android.
- O Android diferencia os tipos `tenant`, `league/liga`, `comissao`, `diretorio` e `mini_vendor` no model; o web app atual ainda normaliza publicamente loja como `tenant`, `league` e `mini_vendor`.
- Carrinho, checkout, pedidos, pagamento, retirada e financeiro continuam mockados/visuais até espelhar o fluxo web completo com segurança.
- Nenhuma Edge Function, Realtime, Storage novo, câmera ou serviço pago foi adicionado.
- Validação executada:
  - `.\gradlew.bat :app:compileDebugKotlin --no-daemon --console=plain --stacktrace`

## M3 - Eventos do usuário completo

Fonte: `web-reference/src/app/eventos/**` e `web-reference/src/app/public/ingressos/[orderId]/[ticketToken]/page.tsx`.

### Ficha do evento (`/eventos/[id]`)

- Bloco "Seus Pedidos" traduzido: pendentes com dados do PIX, cópia da chave, envio de
  comprovante no WhatsApp e cancelamento; finalizados com atalho para o pedido.
  Leitura em `solicitacoes_ingressos` filtrada por `tenant_id`, `userId` e `eventoId`.
- Mural do rolê ganhou curtir, denunciar, apagar e ocultar/restaurar em `eventos_comentarios`.
  Comentário oculto agora vem do banco e só aparece para admin, como no web.
- Enquete aceita nova resposta quando `allowUserOptions` está ligado, com os limites do web
  (60 caracteres, 20 respostas por enquete, 1 sugestão por usuário) e voto automático do autor.
- Hero recebeu contagem regressiva ao vivo, ranking das três turmas com mais confirmados e
  faixa "Últimas vagas" quando `isLowStock` está ligado.
- Contadores de confirmados/interessados abrem a lista completa em modal, com link para o perfil.
- Lotes exibem preço por plano: preço de tabela riscado e selo "Benefício <plano>".

### Menu do evento

- `/eventos/[id]/produtos` virou tela própria (`EventPartyMenuScreen`).
- `/eventos/[id]/produtos/[productId]` virou tela própria (`EventPartyProductScreen`) com
  quantidade, total e criação do pedido em `orders`.
- `/eventos/[id]/produtos/fichas` virou tela própria (`EventPartyVouchersScreen`) com uma ficha
  por voucher, QR real no mesmo payload `evento-produto` do web, código de fallback e status.

### Compra e ingresso

- `/eventos/compra`: quando o evento tem mais de um recebedor em `payment_config.recipients`,
  o passo 2 deixa escolher para quem enviar o comprovante, e a escolha vai para o
  `payment_config` do pedido. Eventos de liga não expõem recebedores, como no web.
- Ingresso transferido ou cancelado passa a esconder o QR com o aviso "QR Code desativado",
  e o cartão mostra turma do titular e origem/destino da transferência.

### Fora de escopo com motivo

- Transferência de ficha (`/api/event-products/transfer`) e de ingresso
  (`/api/event-tickets/transfer`) rodam no servidor com `supabaseAdmin` (service role):
  resolvem o destinatário na tabela `users` e escrevem no pedido de outro usuário.
  Reproduzir isso no cliente exigiria embarcar a service-role key no APK, então as telas
  mostram o estado da transferência mas não disparam a ação.
- O botão "Transferir ingresso" que existia no Android era um `onTransferClick = {}` sem efeito
  e foi removido no lugar de manter um controle que não faz nada.

### Validação executada

- `.\gradlew.bat :app:compileDebugKotlin --console=plain`
- `.\gradlew.bat :app:testDebugUnitTest --console=plain` — BUILD SUCCESSFUL
- `.\gradlew.bat :app:assembleDebug --console=plain` — `app/build/outputs/apk/debug/app-debug.apk`

## M4 - Configurações do usuário

Fonte: `web-reference/src/app/configuracoes/**` (23 rotas), mais
`src/lib/tenantService.ts`, `src/lib/inviteQuota.ts`, `src/lib/mentorshipService.ts`,
`src/lib/settingsService.ts`, `src/lib/reportsService.ts`, `src/lib/tenantBranding.ts`,
`src/lib/turmaLeaderService.ts` e `src/app/api/turma-leader/pendentes/route.ts`.

### Central do Sócio (`/configuracoes`)

- O menu passou a ser montado como no web (`buildSettingsSections`): item do Mini Vendor só
  aparece quando `mini_vendor` está ligado nos módulos do tenant, e carrega o selo real do
  cadastro (`Novo`, `Pendente`, `Aprovado`, `Revisar`, `Bloqueado`).
- O item de apadrinhamento usa o `hubTitle` configurado pelo tenant em vez do texto fixo.
- Novo item "Lider da Turma", visível para `extra.turmaLeader = true` ou papel de gestão.
  `AuthUser` ganhou `isClassLeader`, lido de `users.extra.turmaLeader` na sessão.
- Zona de risco ligada: "Pausar/Reativar conta" (`toggleAccountStatus`: `status`, `role` e
  `saved_role` na própria linha) e "Excluir permanentemente" (`softDeleteAccount`, anonimização).

### Convites

- `/configuracoes/convites` ganhou a cota do dia com bônus (`users.extra.memberInviteQuotaByTenant`),
  botão "Pedir mais convites", contagem regressiva ao vivo do desbloqueio, validade por convite e
  encerramento (`revokeTenantInvite` → `is_active`, `is_revoked`, `revoked_at`, `revoked_by`).
- A lista de ativos agora exclui convites já aprovados, como `activeEntries` do web.
- `/configuracoes/convites/aprovados` virou tela própria, com aprovados e expirados/encerrados
  separados.

### Apadrinhamento

- `/configuracoes/apadrinhamento` deixou de ser somente leitura. Ganhou envio de convite
  (turma → aluno), aceitar com escolha de rótulo, recusar, cancelar, remover vínculo e troca do
  próprio rótulo — todas em `tenant_mentorships`.
- As regras do web foram espelhadas: 1 padrinho e 1 afilhado por perfil (`ensureInviteSlotAvailable`),
  quem enviou só pode cancelar, a resposta cabe à outra pessoa, e o rótulo editável é só o do
  próprio lado. As variações de rótulo saem do label do tenant separado por `/`, `\` ou `|`.

### Líder da turma

- `/configuracoes/lider-turma` virou tela própria, lendo `tenant_join_requests` pendentes do tenant
  com os dados do solicitante, do convite e de quem gerou o convite, filtrando pela turma do líder
  quando ele não tem papel de gestão.

### Pedidos

- `/configuracoes/pedidos` e as três abas foram reescritos sobre as tabelas reais de cada tipo:
  `solicitacoes_ingressos` (eventos), `orders` (loja) e `solicitacoes_adesao` (planos) — antes o
  Android juntava tudo numa lista só e usava `assinaturas` para planos.
- Contadores por status (aprovado/pendente/negado), filtro e paginação de 10 itens, como no web.
- O detalhe do pedido virou rota própria (`/pedidos/{aba}/{status}/{pedidoId}`) com bloco PIX
  (`payment_config` do pedido → `financeiro` do tenant → fallback de marca), cópia da chave,
  contato do comprovante e botão de WhatsApp com a mesma mensagem de
  `buildEventReceiptWhatsappMessage` / `buildProductReceiptWhatsappMessage`.
- O ingresso dentro do pedido virou rota própria (`.../ingressos/{ticketToken}`), com estado de
  transferência e atalho para o QR público.
- O fluxo antigo (`ui/generalorders`, `SupabaseGeneralOrdersRepository`) foi removido: era uma
  lista unificada com dados mockados de fallback que não existia no web.

### Suporte e termos

- `/configuracoes/suporte` deixou de ser lista estática: agora abre chamado em `support_requests`
  (categoria, assunto com 50 caracteres, mensagem com 300) e lista os últimos 20 com a resposta da
  diretoria.
- `/configuracoes/termos` passa a ler os documentos da plataforma em
  `site_config.platform_legal_documents` filtrando por `visibleInApp`, que é a fonte do web;
  os documentos legais do tenant continuam como fallback.

### Mini vendor

- Rotas por categoria (`/pedidos-pendentes/{categoria}` e `/pedidos-aprovados/{categoria}`)
  registradas, com título, subtítulo e atalho "Todas categorias" iguais aos do
  `MiniVendorOrdersStatusPage`. A categoria pública do mini vendor é o próprio nome da loja.
- Pedidos aprovados agora mostram "Aprovado por" com o nome resolvido em `users` (como
  `fetchCanonicalUserVisuals`) e "Data da aprovação".

### Fora de escopo com motivo

- Gerar convite (`/api/member-invite`) roda no servidor com `supabaseAdmin` (service role): cria o
  token e escreve em `tenant_invites` fora do contexto do usuário. O botão "Trazer amigo" que
  existia no Android era um `onCreateInviteClick = {}` sem efeito; virou atalho para "Meus
  convites", com o texto dizendo que a geração acontece no painel web.
- Aprovar/rejeitar pendência de turma (`/api/turma-leader/pendentes`) também roda com service role
  e grava no cadastro de outro usuário. A tela mostra a fila real e diz explicitamente que a
  decisão continua no painel web, em vez de exibir botões sem efeito.
- `/configuracoes/seguranca` é uma tela "Em breve" no web e o item do menu está desativado nos dois
  lados; a `AccountSecurityScreen` do Android segue servindo o fluxo de recuperação do login.

### Validação executada

- `.\gradlew.bat :app:compileDebugKotlin --console=plain`
- `.\gradlew.bat :app:testDebugUnitTest --console=plain`
- `.\gradlew.bat :app:assembleDebug --console=plain`

## M4 - Correção de escopo de vendedor na loja

Auditoria do domínio da loja contra `web-reference` depois do M4.

### Modelo real do web

`seller_type` na tabela `produtos`/`categorias` só assume três valores: `tenant`,
`league` e `mini_vendor` (`normalizeStoreSellerType` em `storeService.ts` e
`storePublicService.ts`). Liga, comissão e diretório **compartilham** `seller_type = "league"`
e são distinguidos pelo `seller_id`: `CommissionManagementStorePage` e as páginas de diretório
reaproveitam `LeagueStoreAdminPage` passando `leagueIdOverride`, e o casamento é
`seller_id === leagueId` (`src/app/ligas/LeagueStoreAdminPage.tsx:142-148`). Registro legado pode
ter `seller_type = "tenant"` com `seller_id` de um coletivo, e nesse caso o dono é o coletivo.

### O que já estava correto

- Mini vendor isolado por `seller_type = mini_vendor` + `seller_id` em todas as leituras e
  escritas de `SupabaseMiniVendorRepository`.
- Loja pública mistura todos os vendedores (é a vitrine) e resolve o dono pelo `seller_id`
  mesmo em linha legada com `seller_type = tenant` (`StoreSellerType.fromRemote`).
- Tela de categorias do admin lista todos os vendedores como panorama com toggle de
  visibilidade, igual ao web.
- Renomear categoria já escrevia só nos produtos com `seller_type = tenant` e `seller_id` da tenant.

### Corrigido

- `getProductsPage` e `fetchTenantProductLookup` buscavam todos os vendedores até o limite e só
  então filtravam em memória. Com catálogo grande de coletivo ou mini vendor, as linhas dos
  outros donos consumiam o `limit` e produtos da tenant sumiam da lista. O filtro
  `seller_type = tenant` passou para a consulta, como `fetchStoreProducts({ sellerType: "tenant" })`
  faz no web; a checagem de `seller_id` continua em memória por causa da linha legada.
- `setProductActive` só validava `tenant_id`, então um id de produto de coletivo ou de mini
  vendor passaria pela guarda. Agora a leitura de guarda e o update filtram por vendedor.

### Ainda não construído

- As telas de configuração de liga, comissão e diretório (`/{coletivo}/configurar/loja/**`) são
  escopo do M7. Hoje o Android não tem tela onde um coletivo administra o próprio catálogo.

## M5 - Treinos, ranking, história e área do parceiro

Fonte: `web-reference/src/app/treinos/**`, `src/app/ranking/**`, `src/app/historico/**`,
`src/app/faq/page.tsx`, `src/app/legal/[slug]/page.tsx`, `src/app/guia/**` e `src/app/empresa/**`,
mais `src/lib/treinosNativeService.ts`, `src/lib/rankingService.ts`, `src/lib/historyService.ts`,
`src/lib/organogramService.ts`, `src/lib/platformFaqConfig.ts`, `src/lib/platformFaqService.ts`,
`src/lib/partnersService.ts`, `src/lib/qrPayloads.ts` e as rotas de servidor
`src/app/api/public/faq/route.ts` e `src/app/api/organogram/requests/route.ts`.

Gym (`/gym`, `/gym/checkin`) ficou fora por decisão de escopo.

### Treinos

- `/treinos` deixou de ser um hub de "desafio + check-in" e virou a agenda mensal do web:
  calendário com navegação de mês, pontos coloridos por modalidade
  (`settings` escopado por tenant → `data.modalidadeColors`), feriados da lista fixa da rota
  (`FERIADOS`, calendário UNITAU 2026) e lista de treinos do dia selecionado.
- O card do dia traz os dois contadores do web — confirmados (`treinos_rsvps` com `going`) e
  presentes (`treinos_chamada` com `presente`) —, o top 3 de turmas, os avatares e os botões
  "Não vou"/"Eu vou!".
- **Escrita nova:** `setRsvp` espelha `setTreinoRsvp` — apaga a linha em `treinos_rsvps` no
  "não vou", faz upsert com `onConflict: treinoId,userId` no "eu vou" e depois recalcula
  `treinos.confirmedCount` como `refreshTreinoConfirmedCount`.
- `/treinos/{id}` é rota própria: capa, contadores, ranking de turmas, RSVP, QR de presença
  (mesmo payload de `buildTreinoPresenceQrPayload`), descrição, local com atalho para o Google Maps,
  card do responsável e lista de presença. A regra de `TREINO_HISTORICO_ALLOWED_ROLES` foi mantida:
  treino encerrado só abre para master, admin geral/gestor, admin de treino e treinador.
- A lista de presença segue o `listaFinal` do web: parte de quem confirmou e é sobrescrita pela
  chamada oficial; linha de chamada manual sem RSVP não aparece na página pública.
- Economia: o web busca os RSVPs de cada card numa chamada por treino. No Android os RSVPs dos
  treinos do dia selecionado são lidos numa consulta só (`isIn("treinoId", ...)`).

### Ranking

- `/ranking` com as duas abas do web: individual (`users` do tenant por `xp` desc, limite 100) e
  por turma, agregada em memória como o `turmasMap` da página.
- Pódio com 1º/2º/3º, rótulo do pódio usando `apelido` ou o primeiro nome, e lista a partir do 4º.
- `/ranking/{turmaId}` com brasão local da turma, total de alunos, soma de pontos e classificação
  interna (limite 50), igual ao web.

### Histórico e organograma

- `/historico` lê `app_config` (id escopado por tenant, `historico`) para título, subtítulo e capa,
  e `historic_events` em ordem crescente de data para a linha do tempo.
- `/historico/organograma` lê `app_config:organograma`, filtra os membros publicados
  (`isPublishedOrganogramMember`), resolve nome/foto/turma canônicos em `users` e agrupa por seção
  respeitando `ordemSecoes`, com as seções novas no fim.
- O estado do próprio usuário aparece como no web: "Solicitação pendente" ou "Você já participa".

### FAQ, legal e guia

- `/faq` deixou de ser derivado do `guia_data` por heurística. Agora lê a mesma linha que
  `/api/public/faq` serve (`site_config` id `faq_page`, aceitando `data`, `config` ou `payload`),
  com categorias, busca sem acento, acordeão de perguntas, imagem da resposta e contadores.
- **Escrita nova:** "Enviar dúvida" abre chamado em `support_requests` com o assunto e o corpo de
  `handleQuestionDoubt` (origem, seção, pergunta e id), sem `tenant_id`, como o web faz no FAQ da
  plataforma.
- `/legal/{slug}` virou rota própria: com slug abre o documento pedido, sem slug segue listando.
- `GuideMockData` foi removido: `GuideUiState` e `LegalUiState` não têm mais conteúdo fixo como
  valor padrão, e o FAQ heurístico (`buildFaqItems`) saiu do `SupabaseGuideRepository`.

### Empresa (área do parceiro)

- `/empresa`: login por e-mail e senha na tabela `parceiros` do tenant, com as mesmas respostas do
  web para senha inválida, cadastro pendente e acesso desativado.
- `/empresa/cadastro`: escolha de plano (Ouro/Prata/Standard), formulário com as máscaras de
  CNPJ/CPF/telefone e as validações de `validateStep2`. **Escrita nova:** cria o parceiro em
  `parceiros` com `status = pending`, como `createPartnerLead`.
- `/empresa/{id}`: painel com capa, contadores, cupons publicados e os 10 últimos scans.
- `/empresa/{id}/editar`: **escrita nova** em `parceiros` (`whats`, `insta`, `site`, `cupons` e
  `contact_visibility_ack`), com as mesmas guardas do web — contato preenchido exige o aceite de
  visibilidade — e a normalização do site sem protocolo.
- `/empresa/{id}/historico`: scans do parceiro paginados de 20 em 20.

### Fora de escopo com motivo

- **Pedir entrada no organograma.** `/api/organogram/requests` roda com `supabaseAdmin`
  (service role) e escreve no `app_config` do tenant. A tela mostra o estado real do usuário e diz
  que o pedido continua no painel web, em vez de exibir um botão sem efeito.
- **Curtir/descurtir resposta do FAQ.** O `PATCH /api/public/faq` também usa service role para
  gravar em `site_config`. Os contadores aparecem em leitura; não há botão que finja gravar.
- **Registrar leitura de cupom no painel do parceiro.** O web usa a câmera (`html5-qrcode`) para
  ler o QR e chamar `createPartnerScan`. O app não tem câmera em nenhum módulo — o scanner atual é
  visual —, então o painel mostra os scans em leitura e diz isso explicitamente.
- **Sincronizar conquistas/XP no RSVP.** `setTreinoRsvp` chama `incrementUserStats` →
  `syncUserAchievementState`, que recalcula `stats`, `xp` e patente contra o catálogo de conquistas.
  Esse motor é do módulo de conquistas e não foi portado; o RSVP do Android grava o RSVP e o
  `confirmedCount`, mas não mexe em `users.stats`/`xp`.
- **Gym.** `/gym` e `/gym/checkin` não foram construídos. A rota `gym` saiu da lista de módulos
  concretos e voltou a renderizar o placeholder de módulo, em vez de reaproveitar a tela de treinos.

### Removido

- `TrainingCheckInScreen`, `TrainingCheckInDetailScreen`, `TrainingFrequencyScreen` e
  `TrainingHistoryScreen`, com as rotas `training-checkin`, `training-checkin-detail/{checkInId}`,
  `training-frequency` e `training-history`. Nenhuma delas existe no web: o QR de presença vive
  dentro de `/treinos/[id]` e virou um bloco da tela de detalhe.
- `TrainingMockData` e `GuideMockData`.

### Validação executada

- `.\gradlew.bat :app:compileDebugKotlin --console=plain` — BUILD SUCCESSFUL
- `.\gradlew.bat :app:testDebugUnitTest --console=plain` — BUILD SUCCESSFUL, 78 testes, 0 falhas
- `.\gradlew.bat :app:assembleDebug --console=plain` — `app/build/outputs/apk/debug/app-debug.apk`

## M6 - Coletivos: área pública (ligas, comissões e diretório)

### Fonte web

O web serve as três áreas públicas com o mesmo `LeagueRecord` de `ligas_config`, normalizado por
`web-reference/src/lib/leaguesService.ts`:

- `app/ligas_usc/page.tsx` — catálogo de ligas com o Oráculo de compatibilidade.
- `app/ligas_usc/[leagueId]/_components/LeaguePublicDetailClient.tsx` — abas
  `overview | membros | agenda | loja` da liga.
- `components/collectives/CollectiveCatalogPage.tsx` — catálogo de comissões e diretório.
- `components/collectives/CollectivePublicDetailClient.tsx` — mesmas quatro abas para comissão e
  diretório.
- `components/collectives/PrimaryDirectoryPage.tsx` — a raiz `/diretorio` abre o registro primário
  do diretório (`fetchPrimaryLeagueRecord`), com `pathMode = "root"`.

Serviços auxiliares portados: `collectiveAreaUiService.ts`, `ligasUscUiService.ts`,
`leagueRoles.ts`, `leagueMedia.ts`, `eventDateUtils.ts`, `storePublicService.ts`,
`turmasService.ts` e `constants/leagueQuizProfiles.ts`.

### Rotas traduzidas (18 `page.tsx`)

| Web | Android |
|---|---|
| `ligas_usc/page.tsx` | `CollectiveCatalogScreen` em `AppRoute.Leagues` |
| `ligas_usc/[leagueId]/page.tsx` | `CollectiveDetailScreen` aba Visão geral em `league-detail/{leagueId}` |
| `ligas_usc/[leagueId]/membros/page.tsx` | aba Membros em `league-members/{leagueId}` |
| `ligas_usc/[leagueId]/agenda/page.tsx` | aba Agenda em `league-agenda/{leagueId}` |
| `ligas_usc/[leagueId]/loja/page.tsx` | aba Loja em `league-store/{leagueId}` |
| `comissoes/page.tsx` | `CollectiveCatalogScreen` em `AppRoute.Commissions` |
| `comissoes/[leagueId]/page.tsx` | aba Visão geral em `commission-detail/{commissionId}` |
| `comissoes/[leagueId]/membros/page.tsx` | aba Membros em `commission-members/{commissionId}` |
| `comissoes/[leagueId]/agenda/page.tsx` | aba Agenda em `commission-agenda/{commissionId}` |
| `comissoes/[leagueId]/loja/page.tsx` | aba Loja em `commission-store/{commissionId}` |
| `diretorio/page.tsx` | registro primário, aba Visão geral em `AppRoute.Directory` |
| `diretorio/membros/page.tsx` | registro primário, aba Membros em `directory-root-members` |
| `diretorio/agenda/page.tsx` | registro primário, aba Agenda em `directory-root-agenda` |
| `diretorio/loja/page.tsx` | registro primário, aba Loja em `directory-root-store` |
| `diretorio/[leagueId]/page.tsx` | aba Visão geral em `directory-detail/{directoryId}` |
| `diretorio/[leagueId]/membros/page.tsx` | aba Membros em `directory-members/{directoryId}` |
| `diretorio/[leagueId]/agenda/page.tsx` | aba Agenda em `directory-agenda/{directoryId}` |
| `diretorio/[leagueId]/loja/page.tsx` | aba Loja em `directory-store/{directoryId}` |

### Supabase real

`SupabaseCollectivesRepository` foi reescrito para espelhar o serviço web, sempre com `tenant_id`:

- `ligas_config` com as colunas de `LEAGUE_SUMMARY_SELECT_COLUMNS` no catálogo e
  `LEAGUES_SELECT_COLUMNS` no detalhe. Ligas ordenam por `likes desc` com limite 60; comissões e
  diretório ordenam por `nome asc` com limite 120, como no web.
- Categoria vem de `data.category` por `normalizeLeagueCategory` (fallback `liga`). Comissões e
  diretório escondem `visivel = false`; `ligas_usc` não filtra, igual ao web.
- Detalhe hidrata os eventos que apontam para `globalEventId` a partir de `eventos`
  (`hydrateLeagueEventsFromGlobalCatalog`).
- Curtir e seguir gravam `users.extra.likedLeagueIdsByTenant` / `followedLeagueIdsByTenant` e
  ajustam `ligas_config.likes`, exatamente como `updateUserLeagueInteractionIds` +
  `changeLeagueLikeCount` sem callable.
- Loja lê `categorias` (visibilidade e capa por `seller_id`) e `produtos` com
  `seller_type = tenant`, `active`, `aprovado` e limite 12 — o mesmo mapeamento `league → tenant`
  de `fetchStoreProductsBySeller`.
- Comissões contam membros por turma em `users` (`fetchTurmaMemberCounts`) e ordenam o catálogo por
  vendas/exposição/likes/nome usando `produtos.vendidos`/`likes`
  (`fetchStoreProductStatsBySellers`).
- Títulos e rótulos de cada área saem de `app_config` em `tenant:{tenant_id}::ligas_usc_ui`,
  `::comissoes_ui` e `::diretorio_ui`.
- O Oráculo grava o resultado em `quiz_history`, ignorando tabela ausente como o web.

### Regras portadas

- Cargos, hierarquia e permissão de gestão em `LeagueRoleCatalog` (porte de `leagueRoles.ts`).
- Comissão publica só a diretoria; liga e diretório publicam todos os membros.
- Evento interno só aparece para membro oficial; na comissão, pertencer à turma já conta como
  membro oficial.
- `canManagePage` combina master da plataforma, `managerUserIds` da página, `managerUserIds` da
  área e cargo de gestão.
- Oráculo com as 5 perguntas, os 22 perfis de liga, sinônimos de palavra-chave e o mesmo cálculo de
  percentual (`QUIZ_DIRECT_MATCH_WEIGHT = 3`).

### Fora de escopo com motivo

- **Enviar solicitação de entrada/acesso.** O web chama `POST /api/ligas/member-requests`, que roda
  com `supabaseAdmin` (service role) e regrava `ligas_config.data.memberRequests`. O card de
  participação mostra o cargo escolhido e o estado real da solicitação, e diz que o envio continua
  no painel web, em vez de exibir um botão sem efeito.
- **Modal de consentimento de uso de dados.** `DataUseConsentModal` só existe no web como porta de
  entrada dessas duas escritas com service role (solicitação e abertura da gestão); sem a escrita, o
  modal não teria função no app.
- **Gestão dos coletivos (`/ligas`, `/comissoes/configurar`, `/diretorio/configurar`).** Feita no M7.

### Removido

- `CollectiveMockData` e os modelos que só existiam para ele (`CollectiveAgendaItem`,
  `CollectiveStoreItem` com status inventado, `LeagueUiState`/`DirectoryUiState`/`CommissionUiState`).
- Abas `eventos` e `informações` dos coletivos, com as rotas `league-events/{id}`,
  `league-info/{id}`, `directory-events/{id}`, `directory-info/{id}` e `commission-events/{id}`.
  Não existem no web: a agenda concentra os eventos e a visão geral concentra as informações.

### Validação executada

- `.\gradlew.bat :app:compileDebugKotlin --console=plain` — BUILD SUCCESSFUL
- `.\gradlew.bat :app:testDebugUnitTest --console=plain` — BUILD SUCCESSFUL, 91 testes, 0 falhas
- `.\gradlew.bat :app:assembleDebug --console=plain` — `app/build/outputs/apk/debug/app-debug.apk`

## M7 - Gestão dos coletivos

Fonte: `web-reference/src/app/ligas/LigasAdminPageContent.tsx`, `app/ligas/LeagueStoreAdminPage.tsx`,
`app/ligas/_components/LeagueAdminQuickNav.tsx`, `_components/LeagueFinanceDashboard.tsx`,
`_components/LeagueFrequencyPage.tsx`, `components/financeiro/FinancialStatementPage.tsx` e os
wrappers `components/collectives/CommissionManagementPages.tsx`,
`CommissionManagementGate.tsx`, `DirectoryManagementPages.tsx` e `DirectoryManagementGate.tsx`.

Android: `SupabaseCollectiveManagementRepository`, `CollectiveManagementViewModel`,
`CollectiveStoreAdminViewModel`, `CollectiveFinanceViewModel`, `CollectiveFrequencyViewModel`,
`CollectiveStatementViewModel` e as telas em `ui/collectives/management/`.

### Modelo real do web

As três áreas usam os **mesmos componentes**. Liga abre em `/ligas/{leagueId}`; comissão e diretório
abrem em `/{area}/configurar/{segmento}` e passam `leagueIdOverride` para os mesmos componentes da
liga, mudando só `entityLabel`, `entityArticle`, `storageNamespace` e `showBoard`. O segmento da
rota é `turmaId` na comissão (`/comissoes/configurar/T2`), a sigla no diretório
(`/diretorio/configurar/DASZ`) e o id na liga. O Android reproduz isso com um único conjunto de
telas parametrizado por `CollectiveKind` e as rotas `league-manage`, `commission-manage` e
`directory-manage`, com `{collectiveId}/{section}`.

### Gate de acesso

- `fetchManagedLeagueSummaries` portado: master da plataforma vê todos os registros com o cargo
  "Master da Plataforma"; os demais entram por cargo de gestão em `ligas_membros`, por cargo de
  gestão no membro embutido em `ligas_config.membros` ou por `managerUserIds` ("Gestor da página").
- Sem coletivo gerenciável a tela é a de "Acesso restrito" do web, com o mesmo texto sobre
  Presidente, Vice-Presidente, Secretaria, Tesouraria e Diretoria.
- Com mais de um registro aparece a lista de seleção; com um só, o painel abre direto — igual ao
  `nextSelectedId` do gate.

### Hub e navegação

- `LeagueAdminQuickNav` portado com os sete itens (Início, Informações, Membros, Agenda, Loja,
  Gestão, Board Round) e o filtro `showBoard`: comissão e diretório mostram seis.
- Hub com os cards do web e os rótulos por área ("Editar dados da liga / da comissão / do
  diretório", "Gerir diretoria", "Publicar e editar agenda", "Produtos e pedidos", "Vendas e BI").

### Escritas novas

- **Informações** (`handleSaveVisualSection`): grava nome, sigla, descrição, visão geral, links
  públicos, `payment_config` e bizu em `ligas_config` com o mesmo merge de
  `updateLeagueConfigRecordCompat` (colunas planas + espelho dentro de `data`), incluindo os limites
  do web (10/42/180/500 caracteres e 12 links). O toggle "Enviar notificação?" insere a linha em
  `notifications` com `userId = GLOBAL`, como no web.
- **Membros** (`handleSaveMembersSection`): grava `membros`, `memberRequests`, `membersCount` e
  `membrosIds` em `ligas_config` e sincroniza `ligas_membros` (insert dos novos, update de cargo
  alterado, delete dos removidos) — é o caminho direto que `syncLeagueMembers` usa quando a rota
  admin não responde. Aprovar/rejeitar solicitação altera só o rascunho, como no web: nada é
  persistido antes do "Salvar membros".
- **Loja**: `upsertStoreCategory` (nome, capa, cor, visibilidade e `renameStoreProductsCategory`
  quando o nome muda), `upsertStoreProduct`, exibir/ocultar produto e exibir/ocultar todo o
  catálogo. `seller_type` é gravado como `tenant` com `seller_id` do coletivo, que é o que
  `normalizeStoreSellerTypeForWrite` faz no web mesmo recebendo `league`.
- **Pedidos**: aprovar (com baixa de estoque, contador de vendidos e notificação), rejeitar,
  reabrir e marcar como entregue, sempre depois de checar que o pedido é de um produto do coletivo.

### Leituras

- Loja, gestão e extrato filtram o vendedor **na consulta** (`seller_id` + checagem de
  `seller_type` em league/tenant/vazio), em vez de baixar a tabela inteira e filtrar em memória como
  o web faz. Mesmo resultado, muito menos linha trafegada.
- Gestão financeira: receita, quantidade e catálogo de produtos e ingressos aprovados, com os
  mesmos `statusIsApproved` e `sortMetrics(limit = 8)` do web.
- BI de produtos (`gestao/produtos`): compradores únicos, ticket médio, estoque, recompra e curva
  ABC, portados de `ProductManagementAnalytics`.
- Frequência: matriz de presença por membro e evento a partir de `solicitacoes_ingressos` aprovados,
  com "Presente" para QR lido e "Aprovado" para ingresso liberado, filtro por evento
  público/interno e, na comissão, `memberScope = "turma"`.
- Extrato: lançamentos de loja e ingressos do coletivo com os filtros de tipo, status e busca, e a
  paginação de 20 do `FinancialStatementPage`.

### Fora de escopo com motivo

- **Ajuste manual de frequência.** O web grava por `PATCH /api/admin/ligas/frequency`, que roda com
  `supabaseAdmin` (service role). O app **lê** os ajustes já gravados em
  `ligas_config.data.frequencyManualEntries` e mostra a contagem, avisando que registrar novo ajuste
  continua no painel web — mesma decisão do M6 para as solicitações de entrada.
- **Upload de imagem** (logo do coletivo, capa da loja e imagem do produto). O web usa
  `uploadLeagueImageToStorage`; no Android os campos aceitam URL. Storage ainda não foi ligado em
  nenhum módulo.
- **Aba Agenda e Board Round do painel.** A agenda do coletivo abre o mesmo workspace de evento do
  admin (`EDIÇÃO | LISTA DE PRESENÇA | SCAN | INGRESSOS | EXTRATO | MODO VENDAS | ENQUETES |
  RECEBEDORES | BI`), que é o M8/M10. O Board Round da liga é o módulo de jogos. Os dois itens
  continuam no quick nav, como no web, mas voltam ao hub.
- **BI de eventos do coletivo** (`gestao/eventos` com comercial, estratégico, operacional, portaria
  e vendas) é o M8. O card "Eventos" da gestão aponta hoje para o BI de produtos já portado.
- **Variações de produto** (tamanho/cor), cores e características em lista. O formulário do app
  cobre os campos principais; `variantes`/`cores`/`caracteristicas` ficam preservados no registro
  porque o update só toca os campos enviados.

### Validação executada

- `.\gradlew.bat :app:compileDebugKotlin --console=plain` — BUILD SUCCESSFUL
- `.\gradlew.bat :app:testDebugUnitTest --console=plain` — BUILD SUCCESSFUL, 103 testes, 0 falhas
  (91 do M6 + 12 novos em `CollectiveManagementM7RulesTest`)
- `.\gradlew.bat :app:assembleDebug --console=plain` — `app/build/outputs/apk/debug/app-debug.apk`

## M8.1 - Motor do BI de Eventos + hub, nos 4 escopos (camada de escopo/consulta e hub)

> **Estado:** entregue a camada de escopo/consulta, o shell, o cabeçalho de contexto, os filtros
> e o hub. O débito que esta seção declarava — `analytics`, acessores, formatadores, alertas,
> ampliação do dataset e o banner de outro portal — foi fechado no **M8.1b**, na seção seguinte.
> Continua fora o bloco de 26 componentes de gráfico (2139-3378); ver o débito do M8.1b.

Fonte: `web-reference/src/app/admin/bi/_components/AdminEventBiDashboard.tsx`, linhas 1-6770
(os blocos por visão, de 6771 em diante, são o M8.2). Wrappers de escopo:
`app/admin/bi/page.tsx` e `app/admin/gestao/eventos/page.tsx` (tenant),
`app/ligas/_components/LeagueEventBiDashboard.tsx` (liga),
`CommissionManagementEventBiPage` de `components/collectives/CommissionManagementPages.tsx`
e `DirectoryManagementEventBiPage` de `components/collectives/DirectoryManagementPages.tsx`.

Android: `domain/model/EventBi.kt`, `domain/repository/CollectiveEventBiRepository.kt`,
`data/repository/SupabaseEventBiRepository.kt`, `ui/bi/EventBiViewModel.kt` e
`ui/bi/EventBiScreen.kt`.

### Um motor, quatro players

O web já é parametrizado: os quatro players chamam o **mesmo** `AdminEventBiDashboard` e mudam
apenas `lockedScopeType`, `lockedScopeId`, `scopeLabel`, `contextTitle`, `contextLogo`,
`contextEyebrow` e `basePath`. O Android faz igual: um `EventBiScope`
(`Tenant`/`League`/`Commission`/`Directory`) + id em `EventBiScopeRef`, um repositório
(`SupabaseEventBiRepository`), um ViewModel (`EventBiViewModel`) e uma tela (`EventBiScreen`).
Nada é duplicado por área.

### Escopo na consulta (diferença deliberada em relação ao web)

`loadBiData` (linha ~539 do web) baixa sete tabelas **inteiras** do tenant — `eventos` 600,
`solicitacoes_ingressos` 6000, `eventos_rsvps` 12000, `produtos` 3000, `orders` 8000, `users` 6000
e `ligas_config` 1000 — e só depois filtra em memória por escopo, evento, produto e período.
Inviável no celular e contra `PROJECT_CONSTRAINTS.md`. O app leva o escopo para a consulta:

- **liga/comissão/diretório**: uma leitura de `ligas_config` pelo id do coletivo devolve as chaves
  de evento (`eventos[].id`, `globalEventId`, `eventId`, `eventoId` e o id extraído de
  `linkEvento`/`href`/`url`, como `linkedEventIdsFromEntityEvent`); `eventos`,
  `solicitacoes_ingressos`, `eventos_rsvps` e `orders` são filtrados por essas chaves;
- **tenant**: `ligas_config` é lido uma vez (200 em vez de 1000) só para montar o índice
  evento → entidade e **excluir** os eventos que pertencem a um coletivo — é o que
  `isTenantOwnedRow` faz em memória no web;
- `tenant_id` está em todas as consultas; o filtro de evento e o de produto entram na própria
  consulta quando estão preenchidos;
- limites: eventos 120 (web 600), ingressos 800 (6000), RSVPs 1200 (12000), produtos 300 (3000),
  pedidos 800 (8000);
- `users` não é lida: no web ela só serve a `classifyTicketAudience`, que é da visão de portaria
  (M8.2);
- o hub carrega só o que os filtros precisam (`includeTransactions = false`): eventos e produtos
  do escopo. As cinco visões do M8.2 pedem o dataset completo pelo mesmo método;
- degradação de coluna: `queryRows` do web derruba coluna ausente e repete a consulta. O app faz o
  equivalente com um conjunto de colunas de fallback em `eventos` e em `orders` (sem `eventId`, o
  pedido só é alcançado pelo produto do evento).

### Regras portadas

- **Cabeçalho** (`DashboardShell`): `titleLabel = contextTitle || title`,
  `subtitleLabel = contextTitle ? "{title}. {subtitle}" : subtitle`, eyebrow
  `contextEyebrow || "BI Administrativo"`. No `inicio` o subtítulo é
  "Escolha a visão analítica{scopeLabel}.".
- **Hub** (`HubContent`): os cinco cards com título e subtítulo exatos do array `MODULES`.
- **Status**: o BI usa a própria lista de aprovados (`isApprovedStatus`), maior que a
  `statusIsApproved` do M7 — inclui `paid`, `pago`, `confirmado`, `confirmada` e `redeemed`. Por
  isso `EventBiStatus` existe em vez de reaproveitar `ApprovedStatuses` do repositório do M7.
- **Período** (`dateInPeriod`): registro sem data sempre passa; `startDate` corta em `T00:00:00` e
  `endDate` em `T23:59:59`.
- **Escopo de entidade** (`entityScopeType`, `declaredExternalScopeType`,
  `canonicalEventOwnerScope`): categoria do `ligas_config`, `turmaId` marcando comissão e o escopo
  declarado no próprio evento (`tipo`, `categoria`, `stats`, `data_extra.eventParty`).
- **Seletor de escopo travado**: `scopeLocked={Boolean(lockedScopeType)}` com default `"tenant"` é
  sempre verdadeiro no web — o `<select>` de escopo **nunca** aparece, nem no player tenant, que
  mostra só o rótulo "Atlética". O app repete isso (`EventBiScopeRef.SelectorLocked`), e
  `EventBiViewModel.selectScope` já está pronto caso o comportamento mude no web.
  Como consequência, `buildScopeOptions` do web é código morto e não foi portado.

### Rotas

| Web | Android |
|---|---|
| `app/admin/bi/page.tsx` | `admin/bi` |
| `app/admin/gestao/eventos/page.tsx` | `admin/gestao/eventos` |
| `app/ligas/[leagueId]/gestao/eventos/page.tsx` | `league-manage/{id}/gestao-eventos` |
| `app/comissoes/configurar/gestao/eventos/page.tsx` | `commission-manage/{id}/gestao-eventos` |
| `app/diretorio/configurar/gestao/eventos/page.tsx` | `directory-manage/{id}/gestao-eventos` |

Correção do M7: o card "Eventos" da gestão do coletivo (`onEventsBiClick`) apontava para
`gestao-produtos` como placeholder; agora abre `gestao-eventos`.

### Fora do escopo do M8.1, com motivo

- **As cinco visões analíticas** (comercial, operacional, portaria, estratégico e modo vendas) são
  o M8.2. Os cards do hub aparecem, mas ainda não navegam.
- **`buildStatementHref` e `buildCheckinsHref`** levam ao workspace de evento
  (`/admin/eventos/{id}/extrato` e `/checkins`, ou o equivalente do coletivo), que é o M10. Os
  links ficaram inertes; nenhum indicador do M8.2 deve navegar até lá antes do M10.
- **`AdminEventSalesModeScreen`** ficou sem rota. Ele ocupava `admin/gestao/eventos`, mas no web
  essa rota é o hub do BI de Eventos (`<AdminEventBiDashboard view="inicio" />`). O conteúdo dele
  pertence à visão "vendas", que o M8.2 porta.
- **Seletor de data nativo**: o web usa `<input type="date">`. O app aceita o mesmo formato
  (`AAAA-MM-DD`) em campo de texto; um date picker é ajuste de UI, não de regra.
- **Filtros no hub**: no web o bloco `Filters` só aparece nas cinco visões — o `inicio` renderiza
  apenas os cards. No app ele já aparece no hub, porque o estado de filtro é compartilhado com as
  visões do M8.2 e é o que torna verificável o escopo da consulta.

### Validação executada

- `.\gradlew.bat :app:compileDebugKotlin --console=plain` — BUILD SUCCESSFUL
- `.\gradlew.bat :app:testDebugUnitTest --console=plain` — BUILD SUCCESSFUL, 117 testes, 0 falhas
  (103 do M7 + 14 novos em `EventBiM8RulesTest`)
- `.\gradlew.bat :app:assembleDebug --console=plain` — `app/build/outputs/apk/debug/app-debug.apk`

## M8.1b - Motor de métricas do BI de Eventos (fecha o débito do M8.1)

Fonte: `web-reference/src/app/admin/bi/_components/AdminEventBiDashboard.tsx`, o `analytics`
useMemo (3843-6619), os acessores (595-2137), os formatadores e a estatística (266-470),
`OPERATIONAL_ALERT_DESCRIPTIONS` (201-217) e `eventOwnerRedirectHref` (6622-6634).

Android: `domain/model/EventBiFormat.kt`, `EventBiMetrics.kt`, `EventBiAccessors.kt`,
`EventBiAnalyticsModel.kt`, `EventBiAnalyticsEngine.kt`, `EventBiOwnerRedirect.kt`, mais o
`SupabaseEventBiRepository` ampliado e o `EventBiViewModel`.

### O `return` do analytics tem 238 chaves, não 211

A auditoria anterior contou 211. O `return` real (6341-6618) tem **238 chaves de primeiro nível**
e o port entrega as 238. Elas vêm agrupadas por visão (`EventBiTotals`, `EventBiCommercial`,
`EventBiOperational`, `EventBiGate`, `EventBiStrategic`, `EventBiSales`) por uma restrição da
JVM, não por escolha de arquitetura: um construtor aceita no máximo 255 slots e cada `Double`
ocupa dois, então uma data class única com 238 campos falha em tempo de execução com
`ClassFormatError`. O agrupamento é o mesmo recorte que as cinco telas do M8.2 consomem, e cada
campo mantém o nome exato do web.

### A linha crua viaja junto com o registro

`EventBiEvent`, `EventBiTicket`, `EventBiOrder` e `EventBiProduct` ganharam `raw: JsonObject`. O
web lê `Row = Record<string, unknown>` direto do Supabase, com cadeias longas de apelido
(`row.valorTotal ?? row.total ?? row.valor ?? row.amount ?? row.preco`). Reescrever cada cadeia
duas vezes — uma no mapeamento do repositório, outra no motor — era a receita para divergir. Com
`raw`, os acessores de `EventBiAccessors.kt` são a **única** implementação de cada cadeia, e o
motor chama exatamente o mesmo acessor que o web chama.

### Escopo continua na consulta, agora com o que faltava

- **`eventos.lotes`** e as colunas de custo entraram no `SELECT` (`eventCapacity`, `eventCost`,
  `eventLotRows`, `expectedTicketTotal`).
- **`users`**: o web baixa 6000 linhas. O app faz duas coisas mais baratas: um `isIn("uid", ...)`
  só com os compradores citados pelo recorte (teto 300, colunas `turma`/`email`/`telefone`), e um
  `count(Count.PLANNED)` de cabeçalho para o `tenantParticipationRate` — que não traz linha
  nenhuma.
- **Auditoria de check-in, vouchers e transferências** entraram como colunas de
  `solicitacoes_ingressos` (`checkinAuditLog`, `transferHistory`, `transferAt`, `data`) e de
  `orders` (`data` com `eventParty.voucherEntries`/`transferRequests`, `eventCheckin*`).
- **`membros`/`membrosIds`** de `ligas_config` alimentam `buildEntityMemberIndex`, que
  `classifyTicketOperationalCategory` usa para separar Diretoria/Membro.
- **Ingressos e pedidos passam a ser consultados por todos os eventos do escopo**, sem o filtro de
  evento nem o de período na query. O custo é o mesmo (um `SELECT`, mesmo teto de 800), e é o que
  permite calcular a recorrência histórica: no web ela lê `data.tickets`/`data.orders`, o tenant
  inteiro. Isso virou `EventBiDataset.scopeTickets`/`scopeOrders`; `tickets`/`orders` (o
  `selectedData`) saem daí por filtro em memória.

### Regras portadas que merecem nota

- **`links` inertes**: `buildStatementHref` e `buildCheckinsHref` levam ao workspace de evento,
  que é o M10. O motor recebe um `EventBiLinkBuilder`; o default é `Inert`, que devolve string
  vazia. O indicador continua sendo calculado — só o link fica desligado.
- **`nowMillis` por parâmetro** no lugar de `Date.now()`, para o envelhecimento de pendência e a
  projeção de receita serem testáveis.
- **`approvedNearEvent.includes(record)`** (4490) compara por identidade de objeto no JS. O port
  usa a chave `tipo:id`, que dois registros distintos nunca compartilham.
- **`isTicketEntryCheckedIn` (836) testa `status.includes("lido")`** — e "inva**lido**" contém
  "lido". Uma entrada marcada como `invalido` em português conta como lida no web e o QR vira
  "Usado". O port repete o comportamento; há teste fixando isso.
- **`row.secondary ?? row.quantity`** em `priceStrategyRows` (5964): `addMetric` sempre soma
  `secondary`, então o valor é `0` e o `??` do JS nunca dispara. O eixo Y sai zerado no web
  também.
- **`buildScopeOptions` (2092)** continua código morto: o seletor de escopo nunca renderiza.

### Validação executada

- `.\gradlew.bat :app:compileDebugKotlin --console=plain` — BUILD SUCCESSFUL
- `.\gradlew.bat :app:testDebugUnitTest --console=plain` — BUILD SUCCESSFUL, 154 testes, 0 falhas
  (117 do M8.1 + 37 novos em `EventBiM8bRulesTest`)
- `.\gradlew.bat :app:assembleDebug --console=plain` — `app/build/outputs/apk/debug/app-debug.apk`

### Débito do M8.1b

| Faixa | Bloco | Tamanho | Estado |
|---|---|---:|---|
| 2139-3378 | 26 componentes de gráfico/tabela | 1.240 linhas | **Não portado** |
| 1890-2044 | `emptyScopeIds`, `addScopedId`, `uniqueScopeIds`, `rowScopeIds`, `hasExternalEventScope`, `isTenantOwnedRow` | ~155 linhas | **Parcial** |

- **26 componentes visuais (2139-3378)** — `KpiCard`, `KpiGrid`, `ChartPanel`, `EmptyChart`,
  `FilterLinkChips`, `Bars`, `BarsDual`, `ColumnBars`, `LineMetric`, `PieMetric`,
  `SimplePieMetric`, `SemiDonutMetric`, `ParetoMetric`, `RadarMetric`, `ScanModeByHourChart`,
  `ScoreGauge`, `FunnelMetric`, `ComboBarsLines`, `StackedPercentChart`, `HeatmapMetric`,
  `TreemapMetric`, `BubbleMetric`, `BubbleTooltip`, `WaterfallMetric`, `NetworkMetric`,
  `DataTable`. **Decisão tomada e autorizada: `com.patrykandpatrick.vico` (Apache-2.0,
  gratuita)**, resolução verificada na versão 2.1.3. A dependência **ainda não foi adicionada** ao
  `build.gradle.kts` porque o M8.1b não desenha gráfico nenhum — entra no M8.2, junto do primeiro
  gráfico. O Vico cobre barras, linha, combo, coluna e stacked; heatmap, treemap, waterfall, rede,
  radar, bolha, funil, gauge e semi-donut continuam precisando de Canvas próprio.
- **Escopo por linha (1890-2044)** — o app resolve escopo **na consulta** (ingressos e pedidos são
  buscados pelos ids de evento do escopo) e aplica `isTenantOwnedRow` ao **evento**, em
  `fetchScopedEvents`. O web também reavalia linha a linha, lendo `leagueId`/`ligaId`/
  `directoryId`/`commissionId`/`seller_type`/`seller_id` do próprio ingresso/pedido. A divergência
  só aparece num caso: ingresso ou pedido de um evento do tenant que carrega no próprio registro
  um vínculo com outro coletivo — no web ele sai do recorte do tenant, no app ele fica. Não foi
  portado porque exigiria trazer essas colunas de volta em todas as tabelas e refazer o filtro em
  memória, que é justamente o que `PROJECT_CONSTRAINTS.md` manda evitar. Fica registrado para o
  M8.2 decidir se vale o custo.

## M8.2 - As cinco visões analíticas do BI de Eventos e as 20 rotas

Fonte: `web-reference/src/app/admin/bi/_components/AdminEventBiDashboard.tsx`, os blocos por visão
(6771-7653) e os 26 componentes de gráfico/tabela (2139-3378), mais os rótulos derivados do corpo
do dashboard (6691-6746).

Android: `ui/bi/charts/` (`EventBiChartKit`, `EventBiChartData`, `EventBiBarCharts`,
`EventBiLineCharts`, `EventBiPieCharts`, `EventBiSpecialCharts`, `EventBiDataTable`),
`ui/bi/views/` (as cinco visões e `EventBiViewIcons`), `domain/model/EventBiViewLabels.kt`, mais
`EventBiScreen`, `EventBiViewModel`, `AppRoute`, `UscNavGraph` e `RemainingNativeRoutes`.

### O Vico cobriria 6 dos 26 componentes, não 17 — a decisão mudou

O M8.1b deixou registrado que os gráficos entrariam com
`com.patrykandpatrick.vico:compose-m3:2.1.3`, deixando para o Canvas apenas heatmap, treemap,
waterfall, rede, radar, bolha, funil, gauge e semi-donut. A auditoria da API do Vico 2.1.3 contra
os 26 componentes mostrou que a divisão real seria outra: o Vico cobre **6** — `BarsDual`,
`ColumnBars`, `LineMetric`, `ParetoMetric`, `ScanModeByHourChart` e `ComboBarsLines`. Ele não tem
pizza/rosca (`PieMetric` e `SimplePieMetric` sozinhos aparecem em cerca de dez painéis), não tem
barra horizontal (`Bars` e `StackedPercentChart`), não tem radar e não tem dispersão. Sobrariam 20
para o Canvas.

Com 20 de 26 já em Canvas, manter o Vico significaria dois renderizadores no mesmo scroll: painéis
com tema Material3 ao lado de painéis com o zinc-950/emerald do web, tipografia diferente e eixos
diferentes. A decisão foi refeita **com autorização**: os 26 componentes são Canvas do Compose,
`build.gradle.kts` não ganhou dependência nenhuma e o APK não cresceu por gráfico. A resolução do
Vico chegou a ser testada e funciona (`BUILD SUCCESSFUL` com a dependência declarada) — o motivo
de não usá-lo é cobertura e consistência visual, não disponibilidade.

### Três rotas do tenant mostravam a tela errada

`admin/bi/comercial`, `admin/bi/operacional` e `admin/bi/portaria` já existiam no `UscNavGraph`,
mas renderizavam `AdminBiSnapshotScreen` — um resumo herdado de módulo anterior, sem relação com
`AdminEventBiDashboard`. No web essas três rotas são `<AdminEventBiDashboard view="..." />`. O M8.2
substituiu as três e acrescentou `admin/bi/estrategico` e `admin/bi/vendas`, que não existiam.
`AdminBiSnapshotScreen` continua servindo `admin/gestao/loja`, `.../treinos` e `.../financeiro`.

### Regras portadas que merecem nota

- **Pareto acumula sobre o total inteiro** (2639): `const total = data.reduce(...)` roda antes do
  `.slice(0, 10)` (2643). Com 12 barras de valor igual, a décima fecha em 83,3%, não em 100%. Há
  teste fixando isso.
- **`PieMetric` e `SimplePieMetric` filtram diferente**: a rosca olha só a chave escolhida
  (`row[dataKey] > 0`, 2511); a pizza simples e a meia-rosca aceitam `quantity > 0 || value > 0`
  (2563/2602). Uma linha com quantidade zero e valor positivo aparece numa e some na outra.
- **`hasSecondary`** (2865): a segunda linha do combo só existe se alguma linha tiver
  `secondary > 0`. Sem essa checagem o gráfico desenharia uma linha constante em zero.
- **`sortBy="none"`** (2861) ordena por `sortValue` crescente — é o que mantém a ordem cronológica
  do forecast e das antecedências, e o que faz `ticketLeadRows`/`productLeadRows` não virarem
  ranking.
- **Waterfall trata zero como positivo** (`value >= 0`, 3209): uma etapa zerada que sobreviva ao
  filtro sai verde, não rosa.
- **Nós da rede saem das arestas já cortadas** (3219-3221): o corte de 12 arestas vem antes do de
  10 nós, então uma aresta descartada não traz nó nenhum.
- **`orderSourceQualityRows` perde o "cancelado"** (7449): o web remonta a linha com
  `cancelado: 0` antes de entregar ao `StackedPercentChart`. O port faz o mesmo, explicitamente.
- **`{false && ...}`** (7321-7354 e 7589-7650) não foi portado: são dois blocos desabilitados no
  web, que nunca renderizam. Somam ~100 linhas da faixa-fonte e estão fora de propósito.

### Adaptações de tela, declaradas

- **Grade**: o web usa `xl:grid-cols-2`/`xl:grid-cols-3` nos painéis e
  `md:grid-cols-2 xl:grid-cols-4` nos KPIs — no celular o próprio web cai para coluna única. O app
  mantém painel em coluna única e KPI em duas colunas, que é o recorte `md`.
- **Sem corte de série**: os gráficos desenham a série inteira, como o Recharts. Quando o eixo de
  categoria não cabe, o rótulo é exibido 1 a cada N (equivalente ao `interval` do Recharts) em vez
  de truncar dado. Os únicos cortes são os que o próprio web faz: Pareto 10, radar 6, treemap 12,
  heatmap 10x8, rede 12 arestas/10 nós.
- **Tooltip de hover**: os `<Tooltip>` do Recharts não têm equivalente no celular. O
  `BubbleTooltip` (3049), que carrega dado que não está em nenhum outro lugar do painel (x, y,
  tamanho da bolha, score e decisão), virou uma lista abaixo do gráfico. Nos demais gráficos o
  valor já aparece como rótulo na barra ou na fatia.
- **`info`**: o modal "Como funciona" do `ChartPanel` (2239) virou `AlertDialog`, com o mesmo
  texto. Os `title`/`aria-label` dos `KpiCard` viraram o mesmo modal, acionado pelo ícone.

### Validação executada

- `.\gradlew.bat :app:compileDebugKotlin --console=plain` — BUILD SUCCESSFUL
- `.\gradlew.bat :app:testDebugUnitTest --console=plain` — BUILD SUCCESSFUL, 178 testes, 0 falhas
  (154 do M8.1b + 24 novos em `EventBiM82RulesTest`)
- `.\gradlew.bat :app:assembleDebug --console=plain` — `app/build/outputs/apk/debug/app-debug.apk`

### Débito do M8.2

| Faixa | Bloco | Tamanho | Estado |
|---|---|---:|---|
| 2281-2287 | `openMetricHref` (clique na barra/fatia abre o extrato) | ~7 linhas | **Não portado — M10** |
| 2179-2185 | `href` do `KpiCard` | ~7 linhas | **Não portado — M10** |
| 3325-3334 | célula-link do `DataTable` | ~10 linhas | **Parcial — M10** |
| 1890-2044 | escopo por linha (`rowScopeIds`, `hasExternalEventScope`, `isTenantOwnedRow`) | ~155 linhas | **Parcial — decisão tomada, não entra** |

- **Links (M10)** — os três primeiros itens são a mesma dependência: `buildStatementHref` e
  `buildCheckinsHref` levam ao workspace de evento, que é o M10. Com `EventBiLinkBuilder.Inert`
  todo href chega vazio, então `FilterLinkChips` não renderiza, o `KpiCard` não vira botão e a
  célula da tabela não ganha destaque de link. **O indicador continua sendo calculado** — o que
  está desligado é só o destino. Quando o M10 existir, trocar o `EventBiLinkBuilder` liga os três
  de uma vez; nenhuma outra mudança é necessária.
- **Escopo por linha (1890-2044) — decisão do M8.2: não entra.** O M8.1b deixou a decisão para
  este módulo. O app resolve escopo **na consulta** e aplica `isTenantOwnedRow` ao evento; o web
  também reavalia linha a linha, lendo `leagueId`/`ligaId`/`directoryId`/`commissionId`/
  `seller_type`/`seller_id` do próprio ingresso/pedido. Portar exigiria trazer essas seis colunas
  de volta em `solicitacoes_ingressos` e `orders` e refazer o filtro em memória — mais bytes por
  consulta e mais trabalho no cliente, contra `PROJECT_CONSTRAINTS.md`, para corrigir um caso
  único: ingresso ou pedido de evento do tenant que carrega no próprio registro vínculo com outro
  coletivo (no web sai do recorte, no app fica). O M8.2 é um módulo de apresentação e não abre
  consulta nova; a divergência fica registrada aqui e no `PARITY_MATRIX.md`.
- **`presenceByLotRows`** é calculado pelo motor e não é consumido por nenhuma visão — no web
  também não: a única referência viva é `noShowRateByLotRows`. Não é débito do port.

## M8.3 e M8.4 - BI Loja nos cinco players e fechamento do M8

Fonte: `web-reference/src/components/ProductManagementAnalytics.tsx` (634 linhas) e
`web-reference/src/app/admin/gestao/_components/AdminBiDashboard.tsx` (1738 linhas), o bloco
`ProductsBi` (1393-1424).

Android: `domain/model/ProductBi.kt`, `domain/model/ProductBiEngine.kt`,
`domain/repository/ProductBiRepository.kt`, `data/repository/SupabaseProductBiRepository.kt`,
`ui/bi/store/` (`ProductBiScreen`, `ProductBiView`, `ProductBiViewModel`),
`ui/bi/charts/ProductBiGroupedBars.kt` e as rotas em `AppRoute`, `UscNavGraph` e
`RemainingNativeRoutes`.

### A descoberta que mudou o módulo: `EventManagementAnalytics` está morto

O escopo declarado do M8.3 pedia **dois** motores. O segundo, `EventBiSummaryEngine`, seria o
porte de `web-reference/src/components/EventManagementAnalytics.tsx` (1572 linhas), descrito como
"o `view="eventos"` do `LeagueFinanceDashboard`, que o M7 NÃO portou", "um só, para os 4 players
com eventos".

A auditoria das rotas mostrou que o componente **não é alcançável por nenhuma rota do web**. São
três referências, e as três estão desligadas:

| Referência | Por que não renderiza |
|---|---|
| `LeagueFinanceDashboard.tsx:762` | Depende de `view === "eventos"`. Nenhuma página passa esse valor: `ligas/[leagueId]/gestao/page.tsx` passa `"hub"`, `.../gestao/produtos/page.tsx` passa `"produtos"`, e o catch-all `comissoes/configurar/[leagueId]/[[...section]]/page.tsx` (linhas 35-44) manda `gestao/eventos` para `CommissionManagementEventBiPage` — que é o `AdminEventBiDashboard`, portado no M8.1/M8.2 |
| `LeagueFinanceDashboard.tsx:898` | Dentro do bloco `{false ? <> ... </> : null}`, linhas 774-1001 |
| `AdminBiDashboard.tsx:757` (`EventsBi`) | Depende de `mode === "eventos"`. As únicas três páginas que montam `AdminBiDashboard` passam `mode="produtos"` (`admin/gestao/loja` e `admin/gestao/produtos`) e `mode="treinos"` (`admin/gestao/treinos`) |

**Decisão: não portado.** É a terceira vez que o mesmo padrão aparece — `{false ? ... : null}` no
`LeagueFinanceDashboard` durante o M7, `{false && view === ...}` no `AdminEventBiDashboard` durante
o M8.2, e agora um componente inteiro cujo consumidor perdeu a rota. Portar colocaria no app um
painel que o web não mostra. Os quatro players com eventos continuam servidos pelo
`AdminEventBiDashboard`, que é o que `gestao/eventos` de fato abre nos quatro.

### As duas tabelas de BI pré-agregado do tenant também não são lidas

O escopo do M8.4 destacava que o tenant "lê DUAS tabelas de BI pré-agregado que nenhum outro
player usa": `bi_produtos_vendas_dimensoes` e `bi_produtos_engajamento`. `loadDashboardData`
(linhas 572-585) realmente consulta as duas. Mas quem as consome é `LegacyProductsBi` (1428-1580),
marcado com `// eslint-disable-next-line @typescript-eslint/no-unused-vars` e **nunca renderizado**:
`AdminBiDashboard` fecha em `return <ProductsBi data={data} />` (1737), e `ProductsBi` (1393-1424)
não toca em nenhuma das duas — ele repassa `products`, `orders` e `users` crus para
`ProductManagementAnalytics`.

Consequência: o BI Loja do tenant **não é diferente** dos outros quatro em métrica nenhuma. O que
muda é só o recorte dos dados. As duas tabelas não foram portadas — seriam duas consultas por
abertura de tela, com teto de 5000 e 2000 linhas, para alimentar código morto. A tolerância a
tabela ausente que o `queryRowsOptional` implementa deixa de ser necessária pelo mesmo motivo.

### Um motor, um repositório, uma tela, cinco players

`ProductBiEngine` é o porte integral do `useMemo` de `analytics` (371-543): 8 KPIs, 13 séries e
2 tabelas. `SupabaseProductBiRepository` resolve o escopo na consulta e entrega
`ProductBiDataset` já normalizado; `ProductBiScreen` + `ProductBiView` desenham. Nenhum player
tem cálculo próprio.

| Player | Recorte da consulta | `title` / `allLabel` |
|---|---|---|
| Tenant | `produtos`/`orders` do tenant, menos `mini_vendor`/`league`/`liga` | "Produtos oficiais da loja" / "Todos os produtos oficiais" |
| Liga | `seller_id = {ligaId}` | "Produtos da liga" / "Todos os produtos da liga" |
| Comissão | `seller_id = {comissaoId}` | "Produtos da comissão" / "Todos os produtos da comissão" |
| Diretório | `seller_id = {diretorioId}` | "Produtos do diretório" / "Todos os produtos do diretório" |
| Mini-vendor | `seller_id` = perfil em `mini_vendors` por `user_id` | nome da loja / "Todos os produtos da lojinha" |

A lojinha é o único player que **não** recebe a prop `users` no web, então o mapa de turma fica
vazio de propósito e a turma sai só do próprio pedido. O repositório reproduz isso pulando a
consulta a `users` nesse escopo.

### Regras portadas que merecem nota

- **A exclusão do tenant não exclui comissão nem diretório** (1399/1409): a lista é
  `["mini_vendor", "league", "liga"]` — tem `liga` **e** `league`, mas comissão e diretório
  continuam entrando no BI Loja da atlética. Contraintuitivo diante do subtítulo "sem misturar
  mini vendors, ligas ou outros players", e é o comportamento do web. Há teste fixando isso.
- **Pedido de produto do recorte entra mesmo com vendedor divergente** (1408): a primeira linha do
  filtro de pedidos aceita qualquer `order.productId` que esteja em `tenantProductIds`, antes de
  olhar `seller_type`. Sem ela, um pedido com vendedor desatualizado sumiria da receita de um
  produto que o painel mostra.
- **A curva ABC acumula sobre a receita inteira** (499), não sobre a soma das 14 linhas de
  `byProduct` (494). Com 15 produtos de receita igual, o 15º fica fora do corte e o acumulado das
  14 primeiras para em 93,3%: nenhuma linha chega à faixa "C". Há teste.
- **"Com desconto" carrega `qtd` 1 ou 0** (485), não a contagem de pedidos — é um sinalizador, e
  "Sem desconto" desconta esse mesmo 1 do total de aprovados.
- **`clickConversion` e `sellThrough` usam o acumulador por produto** (516-525), não
  `approvedOrders.length`: um pedido de produto fora do catálogo não entra no numerador.
- **Produto parado tem duas condições** (474): estoque positivo com venda zero, **ou** 5+ cliques
  sem nenhum pedido. Um produto com 4 cliques e sem venda entra pela primeira.
- **Pedido sem data some do gráfico de dias**: `weekdayLabel` (438) devolve "Sem data", e a saída
  percorre só os sete dias (533). O eixo continua com os sete, mesmo zerados.
- **`statusIsApproved` do BI Loja é menor que a do BI de Eventos** (117): não aceita `validado`
  nem `redeemed`. Não dá para reaproveitar `EventBiStatus`.
- **`medio` é recalculado a cada soma** (172), então é `valor acumulado / qtd acumulada` — não a
  média das médias.
- **O subtítulo do coletivo diz "apenas desta liga." nos três** (774): `title` e `allLabel`
  interpolam a entidade, o subtítulo é literal. Mantido como está.

### Um gráfico novo, e só um

Os 26 componentes do M8.2 cobriram tudo menos um: o `BarsDual` do `ProductManagementAnalytics`
(241-256) é um `BarChart` com **duas barras agrupadas** num único `<YAxis>` (248), enquanto o
`BarsDual` do `AdminEventBiDashboard` — já portado — é barra + linha em dois eixos. Entrou
`ProductBiGroupedBars`, que reusa toda a base de Canvas do kit. O eixo compartilhado foi mantido:
com receita em reais e quantidade em unidades na mesma escala a barra de quantidade fica curta, o
que é o comportamento do web. Nenhuma dependência de gráfico foi adicionada.

### Divergência de consulta declarada

A exclusão de vendedor do tenant é **o único filtro do M8 que ficou em memória**. Ela lê duas
colunas e compara `seller_type` em minúsculas; o `not.in` do PostgREST compara texto exato, então
um `Liga` gravado com maiúscula passaria pela consulta e teria de ser recusado no cliente de
qualquer jeito. Como o teto já é `tenant_id` + 400 linhas, empurrar meio filtro para a query só
criaria uma segunda fonte de verdade. Todo o resto do escopo (liga, comissão, diretório,
mini-vendor) vai para a consulta, contra as ~11 mil linhas que o web baixa por abertura.

### Decisão sobre `mode="treinos"` — fica fora do M8, e o motivo

`AdminBiDashboard` também serve `mode="treinos"` em `/admin/gestao/treinos` (`TrainingsBiEnhanced`,
~1078-1390). A decisão pedida foi tomada: **(b) deixar fora.**

- O escopo declarado do M8 é BI de Eventos e BI Loja. Treinos é um **terceiro** BI, com dataset
  próprio (`treinos`, `treinos_chamada`, `treinos_rsvps`, mais `bi_treinos_presencas_dimensoes` e
  `bi_treinos_modalidades`) e nenhuma métrica em comum com os dois motores entregues — não há
  reaproveitamento a ganhar por portar junto.
- O componente ser o mesmo arquivo não implica lógica compartilhada: `AdminBiDashboard` é um
  roteador de três painéis independentes. O que o M8.4 reaproveitou de lá foi só o recorte de
  vendedor do `ProductsBi`.
- **Consequência honesta, registrada:** `/admin/gestao/treinos` continua mostrando
  `AdminBiSnapshotScreen` com `focus=Training`, que é um resumo herdado de módulo anterior e não
  o painel do web. A rota permanece **fora do contador** e está marcada como divergência em
  `PARITY_MATRIX.md`. É a mesma situação que o M8.2 encontrou em `admin/bi/comercial` e corrigiu;
  aqui ela fica aberta de propósito, para o módulo que portar o BI de Treinos.

### Varredura de duplicação

Nenhuma lógica de BI ficou em paralelo. Removido neste módulo:

| Removido | Onde estava | Por quê |
|---|---|---|
| `CollectiveProductsBi` + `MetricList` | `CollectiveManagementScreens.kt` (~125 linhas) | Versão reduzida do M7 (5 indicadores); substituída pelo motor completo |
| `uniqueBuyers`, `averageTicket`, `stockTotal`, `repurchaseBuyers`, `abcCurve`, `productSalesByName`, `productSalesByLot`, `eventSalesByName` | `CollectiveFinanceUiState` | Só a versão reduzida usava; as três séries `*SalesBy*` só aparecem no web dentro do `{false ? ... : null}` |
| `CollectiveMetricRow`, `addMetric`, `sortedMetrics`, `MetricsLimit`, `AbcCurveSize` | `CollectiveManagementModels.kt` e `SupabaseCollectiveManagementRepository.kt` | Agregadores da versão reduzida, sem uso restante |
| `CollectiveFinanceView` | `CollectiveManagementModels.kt` | O enum só existia para separar `Hub` de `Products`; `Products` virou rota própria |
| `MiniVendorManagementScreen` | `MiniVendorScreens.kt` (~60 linhas) | Resumo sem fonte no web; a rota agora abre o BI Loja |
| `AdminBiSnapshotFocus.Store` | `AdminBiSnapshotScreen.kt` | Seria um segundo BI de loja em paralelo ao real |

O mapa de alertas do BI de eventos (`OPERATIONAL_ALERT_DESCRIPTIONS`, web 201-220) **já havia
entrado no M8.1b**, com as 15 chaves e a descrição padrão do `addOperationalAlert` (4456), em
`EventBiAnalyticsModel.kt`. Não era débito.

### Validação executada

- `.\gradlew.bat :app:compileDebugKotlin --console=plain` — BUILD SUCCESSFUL
- `.\gradlew.bat :app:testDebugUnitTest --console=plain` — BUILD SUCCESSFUL, 215 testes, 0 falhas
  (178 do M8.2 + 37 novos: 22 em `ProductBiM83RulesTest` e 15 em `ProductBiM84RulesTest`)
- `.\gradlew.bat :app:assembleDebug --console=plain` — `app/build/outputs/apk/debug/app-debug.apk`

### Débito do M8.3/M8.4

| Faixa | Bloco | Tamanho | Estado |
|---|---|---:|---|
| `EventManagementAnalytics.tsx` 1-1572 | componente inteiro | 1572 linhas | **Não portado — sem rota viva no web** (evidência acima) |
| `AdminBiDashboard.tsx` 1428-1580 | `LegacyProductsBi` | ~153 linhas | **Não portado — código morto** (`no-unused-vars`, nunca renderizado) |
| `AdminBiDashboard.tsx` 572-585 | consulta a `bi_produtos_vendas_dimensoes` e `bi_produtos_engajamento` | ~14 linhas | **Não portado** — alimenta só o `LegacyProductsBi` |
| `AdminBiDashboard.tsx` 516-541, 1078-1390 | `mode="treinos"` (`TrainingsBiEnhanced` + carga) | ~340 linhas | **Fora do M8, por decisão registrada acima** |
| `AdminBiDashboard.tsx` 745-763 | `EventsBi` | ~19 linhas | **Não portado** — `mode="eventos"` não é montado por nenhuma página |

A faixa-fonte que o M8.3/M8.4 declarou como entregável — `ProductManagementAnalytics.tsx` inteiro
(634 linhas) e o `ProductsBi` do tenant (1393-1424) — foi portada **integralmente**, sem exceção.
Os itens acima são blocos que o escopo previa mas que a auditoria mostrou estarem desligados no
web, mais a decisão explícita sobre treinos.

## M8 - fechamento

O M8 fecha com quatro sub-módulos: M8.1 (escopo/consulta e hub do BI de Eventos), M8.1b (motor de
238 métricas), M8.2 (as cinco visões, 26 gráficos e 20 rotas) e M8.3/M8.4 (BI Loja nos cinco
players). Dois motores de BI no app, um por BI, nenhum por área:

| Motor | Arquivo | Players | Fonte web |
|---|---|---|---|
| BI de Eventos | `domain/model/EventBiAnalyticsEngine.kt` | tenant, liga, comissão, diretório | `AdminEventBiDashboard.tsx` |
| BI Loja | `domain/model/ProductBiEngine.kt` | tenant, liga, comissão, diretório, mini-vendor | `ProductManagementAnalytics.tsx` |

O que ficou fora do M8, com motivo, em um lugar só:

1. **BI de Treinos do tenant** — terceiro BI, fora do escopo declarado; `/admin/gestao/treinos`
   segue mostrando um resumo herdado. Decisão registrada acima.
2. **`EventManagementAnalytics`** — sem rota viva no web.
3. **`LegacyProductsBi` e as duas tabelas `bi_produtos_*`** — código morto no web.
4. **Links de extrato/check-in do BI de Eventos** — dependem do workspace de evento (M10). Com
   `EventBiLinkBuilder.Inert` o indicador é calculado e só o destino está desligado; trocar o
   builder no M10 liga KPI, clique em barra/fatia e célula de tabela de uma vez.
5. **Banner "Este evento pertence a outro portal"** — calcula `webPath` e não navega, pelo mesmo
   motivo do item 4.
6. **`AdminEventSalesModeScreen`** — segue sem rota desde o M8.1.
7. **Escopo por linha do BI de Eventos** (web 1890-2044) — decisão do M8.2, não reabrir sem motivo
   novo: exigiria trazer seis colunas a mais em `solicitacoes_ingressos` e `orders` e filtrar em
   memória, contra `PROJECT_CONSTRAINTS.md`, para corrigir um caso único.

## M9 - Admin: Loja (8 rotas novas no contador)

Fonte: `web-reference/src/app/admin/loja/` — `page.tsx` (291 linhas), `categorias/page.tsx` (1265),
`produtos/page.tsx` (1845), `_components/AdminStoreOrdersStatusPage.tsx` (796) e os quatro
wrappers de pedidos/desativados — mais `lib/storeService.ts` (`approveStoreOrder` 1109-1345 e
`syncApprovedOrderVariantStock` 1017-1107), `lib/upload.ts` (414), `lib/imageCompression.ts` e
`lib/paymentRecipients.ts` (255).

Android: `domain/model/StoreImageUpload.kt`, `domain/model/StoreOrderApproval.kt`,
`domain/model/StoreProductPlanScope.kt`, `domain/repository/StoreImageUploadRepository.kt`,
`data/repository/SupabaseStoreImageUploadRepository.kt`,
`ui/admin/AdminStoreProductFormBlocks.kt`, `ui/admin/StoreImagePicker.kt`, mais
`SupabaseAdminStoreRepository`, os três ViewModels de loja e as telas.

### O bloqueio registrado do M9 não existia

`ROUTE_TRANSLATION_PROGRESS.md` dizia que as quatro rotas de pedido estavam presas porque
`approveStoreOrder` "atualiza vouchers/evento, operação sensível ainda pendente de autorização".
A auditoria da fonte mostrou o contrário:

| Bloco | Onde roda no web |
|---|---|
| `callWithFallback(CALLABLE_APPROVE_ORDER, ...)` (1128-1255) | Tenta a callable e cai num **fallback Supabase-direto completo**: `orders` → `produtos.estoque/vendidos` → `users.xp/selos` → `notifications` |
| Sincronização de evento/voucher (1257-1332) | **Fora** da callable, sempre no cliente |
| `syncApprovedOrderVariantStock` (1334-1342) | **Fora** da callable, sempre no cliente |

Não é operação server-side: é escrita direta que o RLS do web já permite. Nenhuma Edge Function
foi criada.

### Auditoria de RLS pedida antes de codar

Comparação do JWT e de `auth.uid()`: o Android usa a mesma anon key e a mesma sessão do Supabase
Auth (`SupabaseClientProvider`, com `Auth` + `Postgrest` e `autoLoadFromStorage`), e
`SupabasePublicConfig` (8-10) recusa em tempo de construção qualquer chave que contenha
`service_role`. Todas as policies envolvidas são `to authenticated` e resolvem por `auth.uid()`,
então as permissões efetivas do Kotlin são **idênticas** às do web.

| Escrita | Policy | USING / WITH CHECK | Migração |
|---|---|---|---|
| `orders` UPDATE | `tenant_orders_update` | `mt_is_platform_master() or mt_can_manage_tenant(tenant_id) or (mini_vendor dono)` nos dois | `20260408000200` |
| `produtos` UPDATE | `tenant_scope_update` | `mt_can_access_tenant_row(tenant_id)` nos dois | `20260306000400` |
| `users` UPDATE | `users_update_self_or_manage` | USING aceita o próprio uid; WITH CHECK exige `mt_can_manage_tenant` | `20260310000400` |
| `notifications` INSERT | `tenant_notification_insert` | WITH CHECK `mt_can_access_tenant_row(tenant_id)`, com `trg_notifications_tenant_fill` preenchendo | `20260310000400` |

`mt_can_manage_tenant` (20260306000500, 133-150) exige membership `approved` com cargo em
`master`, `admin_geral`, `admin_gestor`, `master_tenant` ou `admin_tenant`. **Nenhuma policy nova
foi criada, e nenhuma existente foi alargada.**

Duas correções ao enunciado da auditoria: **não existe tabela `vouchers`** — o voucher mora em
`orders.data.eventParty.voucherEntries` mais as colunas `event*` da própria `orders`, portanto cai
na policy de `orders`; e **`eventos` não é escrita** na aprovação, o web só lê `orders`.

### O filtro que escondia pedidos que o web mostra

`SupabaseAdminStoreRepository` descartava toda linha com `isEventLinked()` na lista de pedidos, e
`isEventPartyOrder()` na de produtos. O web não filtra em lugar nenhum:
`AdminStoreOrdersStatusPage.tsx` não menciona `eventParty`, e `fetchStoreOrdersPage`
(`storeService.ts` 563-647) filtra só por `status`, `productId` e `tenant_id`.

O efeito era pior que cosmético: o pedido de produto vendido dentro do evento é **justamente** o
que dispara o bloco de fichas em `approveStoreOrder`. Escondê-lo deixava esse pedido sem tela de
aprovação no app.

Os três pontos foram removidos, e `isEventLinked()` e `isEventPartyOrder()` saíram do arquivo:

| Ponto | O que escondia | Fonte web que não filtra |
|---|---|---|
| Lista de pedidos | pedido com `eventId`, `eventoId`, `eventItemType` ou `data.eventParty` | `fetchStoreOrdersPage` (563-647) e `AdminStoreOrdersStatusPage.tsx` |
| `ProductLookupRow` (categorias e `allowedProductIds`) | produto com `data.eventParty` | `fetchStoreProducts` (715-758) |
| `AdminStoreProductRow.toDomain` | idem, na lista de produtos | idem, e a página não filtra depois |

### Idempotência e falha parcial, por decisão do usuário

O web não protege contra aprovação duplicada: `approveStoreOrder` reescreve `status="approved"` e
repete baixa de estoque, XP, selo e notificação a cada clique. Sem transação no cliente, isso
corrompe estoque e XP em silêncio. Por decisão explícita do usuário, o Android acrescentou:

- **Guarda de leitura** (`StoreOrderApproval.shouldSkipApproval`): pedido já `approved` não é
  reaprovado, e a UI diz "Pedido já estava aprovado; nada foi alterado.".
- **Guarda de escrita**: o `UPDATE` carrega `neq("status", "approved")`, então duas telas
  aprovando ao mesmo tempo não somam estoque duas vezes.
- **Falha parcial relatada**: o web faz `console.warn` e segue (1191-1193, 1231-1233, 1248-1250,
  1330-1332, 1340-1342). O Android mantém o mesmo comportamento — o pedido **fica** aprovado — mas
  devolve `StoreApprovalOutcome` com as etapas que falharam, e a tela mostra "Pedido aprovado, mas
  falhou ao atualizar: …".

A idempotência do estoque de variação **já era do web**: `data.variantStockAppliedAt` (1032) é o
marcador que impede a segunda baixa, e foi portado como está.

### Upload via Storage, com os controles do PROJECT_CONSTRAINTS

`upload.ts` foi portado inteiro, na mesma ordem: valida arquivo → reserva a guarda de custo →
valida resolução → comprime → revalida o comprimido → revalida a resolução → envia → registra o
dedupe **só depois** de o Storage aceitar (395-399).

| Controle | Valor do web | Onde |
|---|---|---|
| Tipo | `image/jpeg`, `image/png`, `image/webp` | `validateImageFile` |
| Tamanho | 2MB na origem, 200KB depois de comprimir | idem |
| Resolução | 2400x2400, e o produto das duas | `validateImageDimensions` |
| Compressão | WEBP, 1600x1600, qualidade 0,82 a 0,50 | `StoreImageCompressionPlan` |
| Caminho por tenant | `store/{tenant}/categorias\|produtos/{id}` | `StoreUploadTargets` |
| Teto de custo | 1 por vez, 1,2s entre envios, 6/min, dedupe 45s | `StoreUploadGuard` |

O `SAF` do Android substitui o `<input type="file">`, e `StoreImagePicker` recusa arquivo grande
pelo tamanho do `OpenableColumns.SIZE` antes de ler os bytes.

**Uma dependência foi adicionada**: `io.github.jan-tennert.supabase:storage-kt`, já versionada
pelo BOM que o projeto usa. Não é serviço novo nem pago — é o cliente oficial do mesmo bucket que
o web escreve, e o upload foi autorizado explicitamente. `SUPABASE_STORAGE_BUCKET` entrou no
`BuildConfig` lendo a mesma variável do web (`NEXT_PUBLIC_SUPABASE_STORAGE_BUCKET`, padrão
`uploads`).

### Regras portadas que merecem nota

- **`total || price || 0`** (`AdminStoreOrdersStatusPage.tsx` 376): o `||` do JS trata `0` como
  ausente, então pedido com `total` zerado usa `price`. O `?:` do Kotlin não faria isso sozinho —
  `StoreOrderApproval.approvalPrice` reproduz o comportamento, e há teste.
- **O corte de fichas é destrutivo** (1276): com 3 vouchers gravados e quantidade 1, o web fica
  com 1. Portado como está.
- **Só o literal "inativo" desativa a ficha** (1282); qualquer outro texto vira "ativo".
- **`sanitizeStoragePathSegment` nunca devolve vazio** (upload.ts 92): o `|| "file"` vale para a
  cadeia inteira, então o `filter(Boolean)` de 98 não corta nada e `a//b` vira `a/file/b`.
- **A varredura de qualidade nunca chega ao mínimo**: de 0,82 descendo 0,08, o passo depois de
  0,50 é 0,42, já abaixo de `minQuality` 0,45. O último WEBP tentado é 0,50.
- **Preço e visibilidade por plano têm fallbacks diferentes** (`produtos/page.tsx` 262-275):
  preço ausente vira campo vazio (o plano usa o preço geral), visibilidade ausente vira `true`, e
  só o `false` explícito esconde.
- **`plan_prices` só grava linha com preço; `plan_visibility` grava todas** (877-889).
- **Seleção vazia de recebedor devolve lista vazia** (`paymentRecipients.ts` 111), não a lista
  inteira.
- **`fetchLeagueSummaries` serve só para uma coisa** (`categorias/page.tsx` 244-256): a logo
  oficial da liga na categoria de liga. A consulta Android traz só `id` e `logo_url` de
  `ligas_config`, com `tenant_id` e o mesmo teto de 80 — não o resumo inteiro.
- **O diretório de recebedores é consulta preguiçosa** (`produtos/page.tsx` 1835): o web só chama
  `fetchTenantPaymentReceiverDirectory` quando o `CommerceReceiversManager` abre; a tela em si só
  carrega os recebedores já salvos (338-365). O app faz igual — são duas leituras de até 400
  linhas que não devem rodar a cada abertura da tela de produtos.

### Adaptações de tela, declaradas

- **Variações**: o web tem linhas estruturadas com botões de adicionar/remover; o app mantém o
  campo de texto (`tamanho | cor | estoque | vendidos`, uma por linha) que já existia. O payload
  gravado em `variantes` é idêntico ao do web (810-820).
- **Preço por plano**: no web é um modal (`isPlanModalOpen`); no celular virou bloco recolhível
  dentro do próprio formulário, pela mesma razão do M8.2 — o formulário já é uma coluna longa e um
  segundo modal empilhado não cabe.
- **Sem alternador de "pagamento próprio"**: o web tem `form.payment.enabled` e valida "preencha
  chave, banco e titular"; no app `enabled` é **derivado** dos campos preenchidos, então essa
  validação não tem equivalente. O `payment_config` gravado segue a mesma regra de existência
  (859-862).

### Validação executada

- `.\gradlew.bat :app:compileDebugKotlin --console=plain` — BUILD SUCCESSFUL
- `.\gradlew.bat :app:testDebugUnitTest --console=plain` — BUILD SUCCESSFUL, 248 testes, 0 falhas
  (215 do M8.4 + 33 novos em `AdminStoreM9RulesTest`)
- `.\gradlew.bat :app:assembleDebug --console=plain` — `app/build/outputs/apk/debug/app-debug.apk`

### Débito do M9

| Faixa | Bloco | Tamanho | Estado |
|---|---|---:|---|
| `produtos/page.tsx` 1350-1420 | editor de variação em linhas estruturadas | ~70 linhas | **Adaptado** — campo de texto, mesmo payload |
| `produtos/page.tsx` 825-837 | validação do alternador `payment.enabled` | ~13 linhas | **Sem equivalente** — `enabled` é derivado no app |
| `upload.ts` 341-353 | `allowOriginalOnCompressionFail` | ~13 linhas | **Portado, sem uso** — nenhuma tela da loja liga a opção, como no web |
| `storeService.ts` 1128-1131 | `callWithFallback` para a callable | ~4 linhas | **Não portado** — o app executa direto o fallback; a callable não existe no Android |
| `storeService.ts` 1318-1328 | laço que remove coluna ausente e repete o `UPDATE` | ~11 linhas | **Não portado** — tolerância a schema legado; a migração `20260505040500` já cria as onze colunas `event*`, então o `UPDATE` único basta |

A faixa-fonte declarada como entregável do M9 — as 9 rotas de `admin/loja`, `approveStoreOrder`
inteiro, `upload.ts` inteiro e `paymentRecipients.ts` — foi portada. Os itens acima são adaptações
de tela declaradas e um bloco que só existe por causa da callable do web.
