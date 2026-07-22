# Matriz de Paridade Web x Android

## Atualização - Home/dashboard público Android

- A Home passou a usar `SupabaseHomeDashboardRepository` e a RPC já existente `dashboard_public_home_bundle` como fonte principal.
- Eventos, produtos, parceiros, ligas, comunidade, treinos, contagem de membros e Caça aos Calouros possuem fallbacks diretos, sempre filtrados pelo `tenant_id` recebido pela tela.
- O contrato Kotlin preserva os campos visuais usados pelo web: `viewerIsInterested`, `imagePositionY` e `topTurmas`.
- A composição mobile foi reorganizada conforme o dashboard web: parceiros premium, Modo Vendas, carteirinha, treinos, Caça aos Calouros, eventos, ligas, loja, parceiros standard e comunidade.
- Ouro e prata são exibidos em blocos distintos; eventos e produtos usam cards responsivos de largura integral; drawer e navegação inferior respeitam os limites móveis do web.
- Imagens remotas são carregadas com Coil 3.4.0. Não foi adicionado WebView, backend novo, Edge Function, Realtime ou serviço pago.
- O visitante sem tenant agora passa pelo diretório público real, e a configuração efetiva combina os 47 módulos retornados pelo endpoint público do dashboard com fallbacks conservadores.
- Esta rota permanece em andamento e não entra no contador de traduzidas confirmadas: ainda faltam a landing intermediária e App Links por slug, persistência da escolha guest, tema dinâmico por atlética, interações sociais, rota nativa específica do cardápio do evento e QA visual em aparelho.

## Atualização - Eventos públicos Android

- Eventos públicos Android agora estão parcialmente integrados ao Supabase real.
- Telas cobertas: `EventsScreen` e `EventDetailScreen`.
- Fonte web usada: `eventsNativeService.ts`, `eventsService.ts`, `EventosPageContent.tsx`, `EventosClientPage.tsx` e `eventos/[id]/page.tsx`.
- Tabela usada: `eventos`.
- Filtros obrigatórios implementados: `tenant_id` ativo e `status = ativo` na listagem; `id + tenant_id` no detalhe.
- Paginação/limite: busca limitada e recorte de página em 24 eventos, seguindo o limite público usado no web.
- Organizador/escopo: derivado de `tipo`, `categoria` e `stats.leagueId`, porque as consultas web analisadas não expõem `owner_type` ou `organizer_type` explícitos.
- Não há fallback para mock no fluxo real de eventos públicos; `MockEventsRepository` permanece apenas como arquivo legado sem uso por navegação/ViewModel/UI de eventos.
- Checkout, ingressos, pedidos, QR, financeiro, split e pagamento continuam pendentes e não foram conectados para evitar gravar fluxo sensível sem clonar o web app com segurança.
- Custo: Supabase direto, sem Edge Function, sem Realtime, sem Storage novo e sem backend novo.

## Fonte Confirmada

- Web app: `web-reference/`
- Framework web: Next.js, `src/app`, Supabase JS.
- Total de rotas web `page.tsx`: 362.
- Total de migrations Supabase locais: 90.
- Android nativo: `app/src/main/java/com/example/usc1`.
- Total de arquivos Kotlin Android no estado auditado: 260.

Regra permanente: Supabase Free primeiro. Edge Function só com justificativa e autorização.

## Contagem de Rotas Web Por Área

| Área web | Rotas | Situação Android |
|---|---:|---|
| `admin` | 120 | Parcial/majoritariamente ausente no Android nativo. |
| `[tenant]` | 48 | Parcial. Android tem tenant visual, mas sem slug/landing pública real. |
| `diretorio` | 30 | Parcial/mockado. |
| `ligas` | 28 | Parcial/mockado. |
| `comissoes` | 26 | Parcial/mockado. |
| `configuracoes` | 23 | Parcial/mockado. |
| `master` | 15 | Ausente no Android; deve continuar majoritariamente web/admin salvo necessidade mobile. |
| `eventos` | 6 | Parcial/mockado. |
| `ligas_usc` | 5 | Parcial via telas genéricas de ligas. |
| `empresa` | 5 | Parcial via parceiros; gestão de empresa ausente. |
| `perfil` | 3 | Parcial/mockado. |
| `loja` | 3 | Parcial/mockado. |
| `gym` | 3 | Parcial/mockado. |
| `boardround` | 3 | Parcial/mockado. |
| `album`, `treinos`, `parceiros`, `planos`, `ranking`, `direitos-lgpd` | 2 cada | Parcial/mockado. |
| Rotas unitárias públicas/legais/auth | 1 cada | Parcial; telas existem para várias, mas sem Supabase real. |

## Matriz Por Módulo

| Módulo | Web app equivalente | Android atual | Mock atual | Supabase real no Android | Custo/decisão |
|---|---|---|---|---|---|
| Auth/sessão | `src/context/AuthContext.tsx`, `src/lib/supa/auth.ts`, `src/app/login`, `src/app/cadastro` | Login Android com Google/visitante; guard diferencia guest sem tenant, guest com tenant e membro | `MockAuthRepository` permanece para histórico/testes, mas não é o padrão | Parcial: Auth + perfil mínimo + convite; tenant guest validado pelo endpoint público | Supabase direto + Auth + RPC/endpoint já existentes. Sem Edge Function nova. |
| Tenant/membership/roles | `tenantService.ts`, `publicTenantDirectoryService.ts`, `tenantContext.ts`, `tenant_memberships` | Diretório público real, revalidação por ID/slug/status e `TenantContext` em memória para visitante | O switcher público não usa mais `TenantMockData` | Parcial: endpoint `/api/public/tenants`; memberships reais continuam vindo do Supabase | Faltam landing intermediária, App Links, persistência guest e seletor real para membros multi-tenant. |
| Dashboard/Home | `dashboard`, `[tenant]/dashboard`, `DashboardClientPage.tsx`, `dashboardPublicService.ts` | `HomeScreen`, `HomeViewModel`, `SupabaseHomeDashboardRepository` | Sem mock no fluxo principal; previews/legados locais permanecem fora da navegação real | Parcial: RPC `dashboard_public_home_bundle`, fallbacks por `tenant_id` e 47 módulos efetivos do endpoint público | Em andamento: faltam tema por atlética, interações sociais, cardápio nativo, landing/App Links e QA visual em aparelho. Sem Realtime/Edge nova. |
| Perfil/conexões | `perfil`, `profileService.ts`, `profilePublicService.ts` | `ProfileScreen` | Dados locais | Não | Supabase direto para perfil; RPC existente para bundle público se necessário. Sem Edge nova. |
| Loja pública | `loja`, `storePublicService.ts` | `StoreScreen` e detalhe de produto agora leem Supabase; carrinho/checkout continuam visuais | Carrinho, checkout, pedidos e previews ainda usam `StoreMockData` | Parcial: `categorias`/`produtos` por `tenant_id`, `active`, `aprovado`, paginação e vendedor | Supabase direto em `produtos`/`categorias`. Roda no Free. Sem Edge, Realtime ou Storage novo. Pedidos simples ainda pendentes antes de gravar em `orders`. |
| Loja admin | `admin/loja`, `storeService.ts` | Ausente como admin completo | N/A | Não | Supabase direto para CRUD comum com RLS. Operações perigosas exigem análise/autorização. |
| Pedidos loja | `configuracoes/pedidos/loja`, `orders` | `StoreOrdersScreen` | `StoreMockData.orders` | Não | Supabase direto com paginação por usuário/tenant/status. Sem Realtime. |
| Eventos públicos | `eventos`, `eventsNativeService.ts`, `eventsService.ts` | `EventsScreen` e detalhe usam repositório real; checkout permanece parcial | `MockEventsRepository` é legado e não participa do fluxo real de lista/detalhe | Parcial: `SupabaseEventsRepository` por `tenant_id`; escopo derivado de `tipo`, `categoria` e `stats.leagueId` | Checkout, ingressos, pedidos, QR, financeiro, split e pagamento permanecem pendentes. |
| Eventos de liga/comissão/diretório | `leaguesService.ts`, `ligas`, `comissoes`, `diretorio` | Telas genéricas existem | `CollectiveMockData` | Não | Supabase direto; usar campos reais `scope_type`, `seller_type`, `seller_id`, `leagueId`/`commissionId`/`directoryId` conforme web. |
| Ingressos e pedidos de evento | `solicitacoes_ingressos`, `eventTickets.ts` | Telas de ingressos/pedidos | `MockEventTicketsRepository`, `MockEventOrdersRepository` | Não | Leitura/pedido simples direto. Validação crítica/baixa definitiva de QR pode exigir endpoint/RPC existente; Edge nova só com autorização. |
| Scanner/QR | `scanner`, `admin/scanner`, `eventTickets.ts`, APIs admin | Telas placeholder | Resultado fixo local | Não | Câmera só quando implementar scanner real. Validação sensível não deve confiar no APK; precisa checar fluxo web antes. Edge nova só com autorização. |
| Comunidade | `comunidade`, `communityService.ts` | Feed/detalhe visual | `CommunityMockData` | Não | Supabase direto em `posts`/`denuncias`; paginação, filtros por categoria/status/tenant. Sem Realtime por padrão. |
| Carteirinha | `carteirinha`, `carteirinhaService.ts` | Carteirinha visual | QR mockado | Não | Supabase direto + Storage controlado para assets. Upload com compressão/limite. |
| Planos | `planos`, `plansService.ts`, `plansPublicService.ts` | Catálogo/status/pedidos visual | `PlansMockData` | Não | Supabase direto em `planos` e solicitações. Pagamento real exige análise/autorização. |
| Parceiros/patrocinadores | `parceiros`, `partnersService.ts`, `partnersPublicService.ts` | Lista/detalhe usam `PartnersViewModel` com repositório real | Previews/legados locais fora do fluxo principal | Parcial: `SupabasePartnersRepository` filtrado por `tenant_id` | Ainda faltam agrupamento visual completo, página pública da empresa, cupom/QR e registro de scan. |
| Ligas | `ligas`, `ligas_usc`, `leaguesService.ts` | Lista/detalhe/membros/agenda/loja/eventos/info | `CollectiveMockData` | Não | Supabase direto, `tenant_id`, paginação e limites já visíveis no web. |
| Comissões | `comissoes`, `commissionPagesService.ts`, `leaguesService.ts` | Lista/detalhe/membros/agenda/loja/eventos | `CollectiveMockData` | Não | Supabase direto; categoria `comissao`, escopo `commission`. |
| Diretório | `diretorio`, `leaguesService.ts` | Lista/detalhe/membros/agenda/loja/eventos/info | `CollectiveMockData` | Não | Supabase direto; categoria `diretorio`, escopo `directory`. |
| Mini-vendor | `configuracoes/mini-vendor`, `[tenant]/configuracoes/mini-vendor`, `miniVendorService.ts` | Telas de produtos/pedidos/financeiro | `MiniVendorMockData` | Não | Supabase direto para cadastro/produtos/pedidos comuns; financeiro/split exige autorização. |
| Álbum/caça-calouro | `album`, `albumService.ts` | Telas visuais | `AlbumMockData` | Não | Supabase direto com `tenant_id`; Storage controlado se houver upload/captura. Sem Realtime por padrão. |
| Treinos/Gym | `treinos`, `gym`, `treinosNativeService.ts`, `gymService.ts` | Telas visuais | `TrainingMockData` | Não | Supabase direto em treinos/chamadas/posts, filtros e limites. |
| Games/Boardround/Conquistas/Fidelidade | `games`, `boardround`, `conquistas`, `fidelidade` | Telas visuais | `GamesMockData` | Não | Supabase direto quando seguro; evitar Realtime. Validar regras no web antes. |
| Guia/FAQ/Suporte/Legal/LGPD | `guia`, `faq`, `direitos-lgpd`, `legal`, services legais | Telas visuais | Textos locais/mockados | Não | Supabase direto para documentos/solicitações simples; sem Edge por padrão. |
| Admin completo | `admin/*` | Parcial: 28 rotas confirmadas e outras em andamento | Há placeholders em rotas ainda não traduzidas | Parcial, conforme o contador e a lista de rotas | Operações perigosas continuam exigindo revisão de RLS/RPC e autorização para Edge nova. |
| Master | `master/*` | Ausente | N/A | Não | Provavelmente web/admin primeiro. Android só se houver requisito explícito e referência visual/funcional. |

## Campos Críticos Confirmados No Web

Loja usa `tenant_id`, `seller_type`, `seller_id`, `seller_name`, `seller_logo_url`, `payment_config`, `produtos` e `orders` em `storePublicService.ts` e `storeService.ts`.

Eventos usam `eventos`, `solicitacoes_ingressos`, `eventos_rsvps`, `eventos_likes`, `eventos_comentarios`, `eventos_enquetes`, `eventos_enquete_votos` e `tenant_id` em `eventsNativeService.ts`.

Ligas/comissões/diretório usam `scope_type`, `seller_type`, `seller_id`, `leagueId`, `commissionId`/`comissaoId`, `directoryId`/`diretorioId` em `leaguesService.ts`.

Tenant usa `tenants`, `tenant_memberships`, `tenant_invites`, `tenant_join_requests`, `tenant_onboarding_requests`, `role`, `status`, `tenant_role`, `tenant_status` em `tenantService.ts` e `AuthContext.tsx`.

Storage web já tem limites e compressão: imagens gerais até 2 MB de origem e compressão alvo de 200 KB em `upload.ts`; carteirinha usa fonte até 12 MB e upload final até 256 KB em `carteirinhaService.ts`.

## Bloqueios Para Marcar Tela Como Pronta

- Uma rota não pode ser confirmada enquanto o seu fluxo real ainda depender de `MockData` ou `MockRepository`.
- A loja Android já lê `seller_type`/`seller_id` no catálogo público, mas carrinho, checkout, pedidos, retirada e financeiro ainda não estão integrados ao Supabase.
- Eventos derivam o escopo do organizador de `tipo`, `categoria` e `stats.leagueId`; checkout, ingressos e operações sensíveis ainda estão pendentes.
- Auth Android usa Supabase Auth de forma inicial e convite por RPC existente, mas cadastro/ficha/onboarding completo ainda não está pronto.
- Tenant Android valida membership/role na sessão inicial, mas troca de tenant e switcher ainda usam mock visual.
- Manifest Android já tem `INTERNET` e deep link `usc1://auth`; câmera ainda não foi adicionada porque scanner real não foi implementado.
