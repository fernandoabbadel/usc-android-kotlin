# Progresso Android USC

## Direção Obrigatória

- Não invente tela. Replique a tela web em Compose.
- `web-reference` é a fonte visual obrigatória.
- O Android deve ser nativo em Kotlin/Jetpack Compose.
- Não usar WebView, wrapper, Supabase real, chaves, tokens, `.env` ou segredos no app.
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

## Próximas fases

1. Fase 6: Loja + Carrinho + Checkout visual + Pedidos de loja, copiando `web-reference/src/app/loja`, `carrinho` e `checkout`.
2. Fase 7: Planos + Treinos/Gym + Parceiros + Comunidade.
3. Fase 8: Ligas + Diretório + Comissões + Tenant.
4. Fase 9: Mini-vendor + Modo vendas + Scanner/check-in.
5. Fase 10: Guia, Álbum, Games, Boardround, Conquistas, Fidelidade, Pedidos gerais e revisão de roles.
6. Fase 11: Integração Supabase real, somente depois da paridade visual e sem expor segredos no app.
