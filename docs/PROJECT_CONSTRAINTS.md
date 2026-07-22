# Restrições do Projeto

## Regra Central

Supabase Free primeiro. Edge Function só com justificativa e autorização.

Este projeto deve rodar no plano Free do Supabase pelo maior tempo possível. O Android nativo em Kotlin/Jetpack Compose deve clonar o web app existente, usando o mesmo Supabase como fonte única de dados, sem criar produto novo, schema paralelo ou fluxo mais completo que o web app.

## Fonte da Verdade

- O web app é a fonte da verdade visual, funcional e de regra de negócio.
- Antes de implementar tela, fluxo, model, repository, ViewModel, query ou mutation no Android, localizar o equivalente no web app.
- O Android deve usar os mesmos registros, tabelas, policies, storage e regras já usados pelo web app.
- Tudo que for gravado no Android precisa aparecer no web app.
- Tudo que for gravado no web app precisa aparecer no Android.
- Não criar funcionalidade nova que não exista no web app.
- Não criar tela mockada nova.

## Padrão Técnico Econômico

O padrão do projeto é:

```text
Android Kotlin/Compose
-> Supabase client direto
-> Supabase Auth
-> RLS bem configurado
-> tenant_id obrigatório
-> paginação
-> cache quando fizer sentido
-> mínimo de chamadas possível
```

## Edge Functions

Edge Functions são exceção, não padrão.

Antes de propor ou criar uma Edge Function, é obrigatório explicar:

- por que não dá para fazer direto com Supabase Auth + RLS;
- qual risco existiria sem Edge Function;
- qual é a alternativa mais barata;
- se precisa de autorização explícita antes de implementar.

Edge Function só deve ser considerada para:

- pagamento;
- webhook;
- financeiro;
- recebedores/split;
- segredo que não pode ir no APK;
- validação crítica de QR code;
- baixa definitiva sensível de ingresso/produto;
- operação administrativa perigosa;
- operação que no web app já rode server-side por motivo de segurança.

## Chaves e Segredos

Nunca colocar no Android:

- service_role key;
- segredo de pagamento;
- token privado;
- chave administrativa;
- credencial de webhook;
- chave de integração sensível.

A anon key pode existir no cliente Android, mas a segurança real deve depender de Supabase Auth + RLS.

## Serviços Pagos

Não adicionar serviços pagos externos sem autorização explícita.

Proibido adicionar automaticamente:

- Firebase pago;
- servidor próprio;
- backend novo;
- SaaS de analytics pago;
- serviço de push pago;
- storage externo pago;
- API externa paga;
- CDN paga;
- ferramenta de monitoramento paga.

## Realtime

Realtime é exceção.

Não usar Realtime em tudo. Usar apenas quando o web app já depender disso ou quando for realmente indispensável. Para a maioria das telas, usar busca normal com paginação e cache.

## Queries

Toda query precisa ser econômica:

- respeitar tenant ativo;
- filtrar por tenant_id quando o dado for de tenant;
- respeitar membership e role;
- usar paginação;
- usar limit;
- filtrar por status e tipo quando aplicável;
- selecionar apenas os campos necessários;
- evitar carregar imagens grandes;
- evitar carregar listas gigantes;
- evitar N+1 queries;
- evitar refresh desnecessário;
- nunca carregar dados de todos os tenants sem necessidade.

## Multi-Tenant

Toda tela e toda query precisam respeitar:

- tenant ativo;
- membership do usuário;
- role/permissão;
- seller_type quando for loja;
- seller_id quando for loja;
- owner_type ou organizer_type quando for evento;
- owner_id ou organizer_id quando for evento.

Nunca misturar dados de atléticas diferentes.

## Loja

A loja precisa diferenciar:

- produto da atlética/tenant;
- produto de liga acadêmica;
- produto de comissão de formatura;
- produto de diretório acadêmico;
- produto de mini-vendor.

Campos obrigatórios quando existirem no web app/Supabase:

- tenant_id;
- seller_type;
- seller_id;
- seller_name;
- status;
- estoque;
- preço;
- imagens;
- categorias;
- pedidos;
- retirada;
- financeiro.

## Eventos

Eventos precisam diferenciar:

- evento da atlética/tenant;
- evento de liga acadêmica;
- evento de comissão de formatura;
- evento de diretório acadêmico.

Campos obrigatórios quando existirem no web app/Supabase:

- tenant_id;
- owner_type ou organizer_type;
- owner_id ou organizer_id;
- status;
- lotes;
- ingressos;
- check-in;
- QR code;
- pedidos;
- produtos vinculados ao evento;
- financeiro;
- retirada.

## Storage

Storage deve ser controlado:

- limite de tamanho;
- compressão quando possível;
- tipo de arquivo validado;
- caminho organizado por tenant;
- política de acesso clara;
- evitar vídeo pesado se não for obrigatório.

## Critério de Pronto

Uma tela só pode ser marcada como pronta quando:

- existe no Android;
- existe equivalente no web app;
- está visualmente fiel ao web app;
- navega corretamente;
- usa dados reais do Supabase ou endpoint equivalente;
- grava no mesmo backend do web app;
- respeita tenant/membership/role;
- não usa mock;
- trata loading;
- trata erro;
- trata estado vazio;
- é responsiva no celular;
- não aumenta custo desnecessariamente;
- não usa Edge Function sem necessidade;
- não usa Realtime sem necessidade;
- não baixa dados de outros tenants.

