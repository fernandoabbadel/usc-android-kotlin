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
- **Gestão dos coletivos (`/ligas`, `/comissoes/configurar`, `/diretorio/configurar`).** É o M7.

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
