# Progresso Android USC

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

- Home/Dashboard real criado com dados mockados, saudação, tenant, plano, pedidos, atalhos, eventos e módulos principais.
- `HomeViewModel` e `HomeUiState` adicionados.
- Componentes reutilizáveis adicionados:
  - `AppSectionHeader`
  - `InfoChip`
  - `QuickActionCard`
  - `DashboardSummaryCard`
  - `HomeEventCard`
  - `HomeModuleCard`
- Tela de perfil criada com avatar mockado, dados pessoais, curso, turma, tenant, role, status, plano ativo e atalhos.
- Tela de configurações criada com seções para conta, pedidos, operação, suporte, termos, LGPD e sair da conta.
- Carteirinha digital nativa criada com dados mockados, status, validade, identificador e QR visual mockado.
- Navigation Compose atualizado para substituir placeholders por telas reais de Home, Perfil, Configurações e Carteirinha.
- Placeholders mantidos apenas para módulos de fases futuras.
- Previews criados:
  - `HomeScreenPreview`
  - `HomeScreenLoadingPreview`
  - `HomeScreenErrorPreview`
  - `ProfileScreenPreview`
  - `SettingsScreenPreview`
  - `MembershipCardScreenPreview`
  - `MembershipCardPreview`
- Validação executada:
  - `.\gradlew.bat :app:assembleDebug --no-daemon --console=plain`
  - `.\gradlew.bat :app:testDebugUnitTest --no-daemon --console=plain`
- Commit local: `Add home profile settings and membership card screens`.

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
- Interfaces preparadas:
  - `EventsRepository`
  - `EventTicketsRepository`
  - `EventOrdersRepository`
- Repositórios mockados criados:
  - `MockEventsRepository`
  - `MockEventTicketsRepository`
  - `MockEventOrdersRepository`
- Telas nativas criadas:
  - `EventsScreen`
  - `EventDetailScreen`
  - `EventCheckoutScreen`
  - `EventTicketsScreen`
  - `EventTicketDetailScreen`
  - `EventOrdersScreen`
  - `EventOrderDetailScreen`
- Componentes criados:
  - `EventCard`
  - `EventStatusChip`
  - `EventCover`
  - `TicketCard`
  - `TicketQrPlaceholder`
  - `TicketStatusChip`
  - `EventOrderCard`
  - `OrderStatusChip`
- ViewModels e estados adicionados:
  - `EventsViewModel`
  - `EventDetailViewModel`
  - `EventsUiState`
  - `EventDetailUiState`
  - `EventCheckoutUiState`
  - `EventTicketsViewModel`
  - `EventTicketDetailViewModel`
  - `EventTicketsUiState`
  - `EventTicketDetailUiState`
  - `EventOrdersViewModel`
  - `EventOrderDetailViewModel`
  - `EventOrdersUiState`
  - `EventOrderDetailUiState`
- Navigation Compose integrado para lista, detalhe, checkout, ingressos, detalhe de ingresso, pedidos e detalhe de pedido.
- Previews criados:
  - `EventsScreenPreview`
  - `EventsScreenLoadingPreview`
  - `EventsScreenEmptyPreview`
  - `EventDetailScreenPreview`
  - `EventTicketsScreenPreview`
  - `EventTicketDetailScreenPreview`
  - `EventOrdersScreenPreview`
  - `EventOrderDetailScreenPreview`
  - `TicketCardPreview`
  - `EventCardPreview`
- Validação executada:
  - `.\gradlew.bat :app:assembleDebug --no-daemon --console=plain`
  - `.\gradlew.bat :app:testDebugUnitTest --no-daemon --console=plain`
- Commit local: `Add events tickets QR and event orders screens`.

## Próximas fases

1. Fase 6: Loja + Carrinho + Checkout visual + Pedidos de loja.
2. Fase 7: Planos + Treinos + Parceiros + Comunidade.
3. Fase 8: Ligas + Diretório + Comissões + Tenant.
4. Fase 9: Mini-vendor + Modo vendas + Scanner/check-in.
5. Fase 10: Supabase real.
