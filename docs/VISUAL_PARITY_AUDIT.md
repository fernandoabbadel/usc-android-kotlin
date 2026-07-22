# Auditoria de Paridade Visual e Funcional

## Objetivo

Traduzir o web app USC para Kotlin/Jetpack Compose, sem WebView e sem alterar estrutura, identidade visual ou regras de negócio. O diretório `web-reference` é a fonte da verdade.

## Situação Verificada

- O web app possui 362 arquivos `page.tsx`.
- A matriz mantém 28 rotas confirmadas; a Home continua fora do contador até cumprir todo o critério de pronto.
- Muitas telas Android existentes são esqueletos visuais ou ainda dependem de mocks. A existência de uma `Screen` não significa paridade concluída.
- A Home foi o primeiro marco refeito com dados reais e comparação direta contra a versão mobile do web.

## Home: Entregue Neste Marco

- Bundle público real do Supabase como fonte principal.
- Fallbacks econômicos e isolados por `tenant_id`.
- Ordem e agrupamento das seções equivalentes ao web.
- Imagens remotas, cards responsivos, drawer lateral e navegação inferior flutuante.
- Correções de rota do Modo Vendas e do scanner central.
- Estado de confirmação do evento, recorte vertical da capa e ranking de turmas no produto.
- Separação visual de parceiros ouro, prata e standard.
- Tratamento de cancelamento de requisições no `HomeViewModel`.
- Diretório público real para visitante, com revalidação do tenant e bloqueio de rotas privadas.
- Configuração efetiva completa dos 47 módulos do web, com fallback conservador.

## Pendências P0

1. Aplicar logo, nome, sigla e paleta de cada tenant em todo o shell; remover fallbacks visuais específicos da AAAKN quando não houver contexto AAAKN.
2. Traduzir a rota nativa do cardápio de evento usada pelo Modo Vendas.
3. Substituir mocks dos fluxos públicos prioritários: carteirinha, comunidade, ligas, treinos, planos e pedidos.
4. Criar a landing intermediária e Android App Links por slug; persistir a escolha guest com revalidação na inicialização.

## Pendências P1

1. Replicar ações de like, interesse, convite, cupom/QR e notificações com as mesmas regras do web.
2. Consolidar um shell responsivo compartilhado para Home, Eventos, Loja, Configurações e demais módulos.
3. Fazer comparação visual em dispositivo Android nos tamanhos compactos e grandes.
4. Ampliar testes para falha integral da RPC e troca de membership em contas multi-tenant; visitante e configuração de módulos já possuem cobertura inicial.

## Critério de Aceite

Uma rota só deve mudar para concluída quando estiver visualmente fiel, responsiva, navegável, sem mock no fluxo real, isolada por tenant, protegida por membership/role, integrada ao mesmo Supabase e coberta pelos estados de carregamento, erro e vazio.
