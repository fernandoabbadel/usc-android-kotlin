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

## Próximas fases

1. Fase 4: Home/Dashboard real + Perfil + Configurações + Carteirinha.
2. Fase 5: Eventos + Ingressos + QR visual + Pedidos.
3. Fase 6: Loja + Carrinho + Checkout visual + Pedidos de loja.
4. Fase 7: Planos + Treinos + Parceiros + Comunidade.
5. Fase 8: Ligas + Diretório + Comissões + Tenant.
6. Fase 9: Mini-vendor + Modo vendas + Scanner/check-in.
7. Fase 10: Supabase real.
