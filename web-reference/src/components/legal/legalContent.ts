export type LegalListItem = {
  label: string;
  text: string;
};

export type LegalSection = {
  title: string;
  body?: string[];
  bullets?: string[];
  items?: LegalListItem[];
  note?: string;
};

export type LegalDocument = {
  slug: string;
  title: string;
  description: string;
  lastUpdated: string;
  sections: LegalSection[];
};

export const LEGAL_LAST_UPDATED = "23/05/2026";
export const LEGAL_VERSION = "2026-05-23";

export const LEGAL_NAV_ITEMS = [
  { href: "/politica-privacidade", label: "Política de Privacidade" },
  { href: "/termos-de-servico", label: "Termos de Serviço" },
  { href: "/politica-cookies", label: "Política de Cookies" },
  { href: "/direitos-lgpd", label: "Direitos LGPD" },
  { href: "/termo-confidencialidade-admin", label: "Sigilo Administrativo" },
  { href: "/termos-tenants-organizadores", label: "Termos para Tenants" },
] as const;

const institutionalFields = [
  { label: "Razão social", text: "[RAZÃO SOCIAL DA OPERADORA DA USC]" },
  { label: "CNPJ", text: "[CNPJ DA OPERADORA DA USC]" },
  { label: "Endereço", text: "[ENDEREÇO DA OPERADORA DA USC]" },
  { label: "E-mail de contato", text: "[EMAIL OFICIAL DA USC]" },
  { label: "E-mail LGPD/Encarregado", text: "[EMAIL DO ENCARREGADO OU PRIVACIDADE]" },
];

const dataFeatureItems: LegalListItem[] = [
  {
    label: "Cadastro e login",
    text:
      "Podem envolver e-mail, nome, telefone, CPF/CNPJ quando necessário, foto, data de nascimento, autenticação social, dados de convite e status da conta. A finalidade é criar conta, identificar o usuário, controlar acesso, evitar fraude e liberar perfis de visitante, membro ou administrador. A retenção ocorre enquanto a conta estiver ativa e pelo prazo necessário para cumprimento legal, segurança e auditoria.",
  },
  {
    label: "Loja",
    text:
      "Podem ser tratados produtos visualizados, carrinho, pedidos, histórico de compra, endereço de entrega, CPF/CNPJ, comprovantes, status de pagamento, retirada e entrega. A finalidade é vender, entregar, faturar, controlar pedidos, prestar suporte e prevenir fraude. A retenção observa a conta ativa e prazos legais, fiscais, contábeis, de defesa e prestação de contas, inclusive por até 5 ou 7 anos quando aplicável.",
  },
  {
    label: "Eventos",
    text:
      "Podem ser tratados inscrições, participações, check-in, ingressos, QR Code, dados de pagamento, data e hora de entrada, lote, categoria, transferência de ingresso e responsável pela validação. A finalidade é organizar eventos, controlar acesso, validar ingresso, evitar duplicidade ou fraude e gerar indicadores operacionais. A retenção segue a atividade do evento e prazos de prestação de contas, auditoria, defesa e histórico operacional.",
  },
  {
    label: "Planos de sócio",
    text:
      "Podem ser tratados plano escolhido, início, fim, pagamentos, benefícios usados, status, histórico e carteirinha. A finalidade é validar acesso, cobrar, liberar benefícios e controlar elegibilidade. A retenção segue o vínculo ativo e os prazos contratuais, fiscais e de prestação de contas.",
  },
  {
    label: "Mini vendor",
    text:
      "Podem ser tratados produtos vendidos, cadastro do vendedor, dados bancários para repasse, vendas, pedidos, documentos, comprovantes e histórico financeiro. A finalidade é controlar vendas, aprovar vendedores, fazer repasses, prestar contas e prevenir fraude. A retenção segue o período de atividade e prazos fiscais, contábeis e de defesa.",
  },
  {
    label: "Grupos, ligas, comissões e diretório",
    text:
      "Podem ser tratados filiação, cargo, função, entrada, saída, permissões, membros, solicitações, agenda, loja, eventos, frequência e financeiro. A finalidade é controle de acesso, governança interna, organização de grupos e comunicação oficial. A retenção segue o vínculo ativo e prazos de histórico institucional, segurança, auditoria e prestação de contas.",
  },
  {
    label: "Permissões e roles",
    text:
      "Podem ser tratados papel de acesso, permissões, logs, auditoria, ações administrativas, bloqueios e aprovações. A finalidade é segurança, controle de acesso, rastreabilidade e prevenção de abuso. A retenção segue a conta ativa e o prazo necessário para auditoria e segurança.",
  },
  {
    label: "Treinos",
    text:
      "Podem ser tratados presença, modalidade, frequência, resultados, altura, peso e outros dados corporais quando a funcionalidade coletar. A finalidade é organizar treinos, frequência, acompanhamento esportivo e gestão. Quando houver dados de saúde, biometria ou dados sensíveis, a USC aplica minimização, proteção reforçada e base legal apropriada.",
  },
  {
    label: "Parceiros",
    text:
      "Podem ser tratados dados comerciais, empresa, contato, categoria, histórico, benefícios e campanhas. A finalidade é gerenciar relacionamento, divulgar benefícios e operar atividades comerciais. A retenção segue o vínculo ativo e prazos contratuais e históricos.",
  },
  {
    label: "Comunidade",
    text:
      "Podem ser tratados posts, comentários, denúncias, conteúdo criado, moderação e banimentos. A finalidade é interação social, moderação, prevenção de spam e segurança comunitária. A retenção segue a conta ativa e pode continuar após exclusão quando necessário para moderação, segurança, cumprimento legal ou defesa.",
  },
  {
    label: "Reviews",
    text:
      "Podem ser tratados avaliações, feedback, notas e comentários sobre produtos, eventos e serviços. A finalidade é melhorar produtos, serviços, reputação e transparência. A retenção segue o histórico do produto ou serviço, com anonimização quando adequada.",
  },
  {
    label: "Likes e favoritos",
    text:
      "Podem ser tratados marcações, favoritos e interesses. A finalidade é experiência do usuário, recomendações, personalização e organização pessoal. A retenção segue a conta ativa ou a exclusão pelo usuário.",
  },
  {
    label: "Perfil",
    text:
      "Podem ser tratados bio, foto, interesses, hobbies, visibilidade pública ou privada, turma, vínculo e perfil público. A finalidade é construir comunidade, identificação interna e networking. A retenção segue a conta ativa ou alteração e exclusão pelo usuário.",
  },
  {
    label: "Lista de crush ou afinidade",
    text:
      "Podem ser tratados usuários adicionados a listas privadas, matches e afinidades quando a funcionalidade existir. A finalidade é recurso social opcional. Esses dados são privados e não devem ser exibidos publicamente sem ação clara do usuário.",
  },
  {
    label: "Enquetes",
    text:
      "Podem ser tratados respostas, escolhas, votos, data, hora e identificação quando necessária. A finalidade é pesquisa, feedback, deliberações, eventos e decisões internas. A retenção segue a enquete ativa e prazos de histórico ou relatório.",
  },
  {
    label: "Ligas acadêmicas, comissões, diretório e apadrinhamento",
    text:
      "Podem ser tratados inscrições, participação, resultados, frequência, eventos, loja, membros, cargos, atas, responsáveis, relações de mentoria e histórico de participação. A finalidade é organização de atividades, comunicação, governança, integração e memória institucional.",
  },
  {
    label: "Seguidores e seguindo",
    text:
      "Podem ser tratadas relações de seguidores, perfis seguidos e rede social interna. A finalidade é construir comunidade, recomendações e interação. A retenção segue a conta ativa ou desfazimento da relação.",
  },
];

export const privacyPolicyDocument: LegalDocument = {
  slug: "politica-privacidade",
  title: "Política de Privacidade – USC – Universidade Spot Connect",
  description:
    "Como a USC coleta, utiliza, armazena, compartilha e protege dados pessoais na plataforma multitenant.",
  lastUpdated: LEGAL_LAST_UPDATED,
  sections: [
    {
      title: "1. Identificação da plataforma e controlador",
      body: [
        "A USC – Universidade Spot Connect é uma plataforma digital multitenant para gestão de atléticas, ligas, comissões, diretórios, eventos, loja, planos, treinos, mini vendors, parceiros, comunidade, rankings, permissões e rotinas administrativas.",
        "Esta Política de Privacidade explica como a USC coleta, utiliza, armazena, compartilha e protege dados pessoais no uso de sites, páginas públicas, áreas autenticadas, painéis administrativos e demais módulos da plataforma.",
        "Dependendo do módulo e da finalidade, a USC poderá atuar como controladora, operadora ou corresponsável pelo tratamento de dados com organizações, organizadores, parceiros ou vendedores que utilizam a infraestrutura da plataforma.",
      ],
      items: institutionalFields,
    },
    {
      title: "2. Conceitos importantes",
      items: [
        {
          label: "Usuário",
          text:
            "Visitante, estudante, membro, atleta, comprador, participante de evento, administrador, mini vendor, representante de grupo, parceiro ou qualquer pessoa que utilize a USC.",
        },
        {
          label: "Tenant ou organização",
          text:
            "Entidade, atlética, liga, comissão, diretório, organizador, parceiro, vendedor ou grupo autorizado a operar módulos dentro da USC.",
        },
        {
          label: "Dados pessoais",
          text:
            "Informações relacionadas a pessoa natural identificada ou identificável, nos termos da LGPD – Lei nº 13.709/2018.",
        },
        {
          label: "Dados sensíveis",
          text:
            "Dados pessoais sujeitos a proteção especial, incluindo dados de saúde, biometria, origem racial ou étnica, convicção religiosa, opinião política, filiação sindical e outros previstos na LGPD.",
        },
      ],
    },
    {
      title: "3. Dados pessoais que coletamos",
      body: [
        "A USC pode coletar dados de identificação, contato, cadastro, autenticação, perfil, preferências, compras, eventos, planos, pagamentos, entrega, retirada, histórico de uso, interações sociais, permissões, auditoria, suporte e segurança.",
        "Também podemos tratar dados técnicos, como endereço IP, identificadores de dispositivo, navegador, registros de acesso, data, hora, rota acessada e logs de segurança. Quando aplicável, esses registros observam a LGPD e o Marco Civil da Internet – Lei nº 12.965/2014.",
      ],
      items: [
        {
          label: "Dados pessoais comuns",
          text:
            "Incluem nome, e-mail, telefone, CPF, CNPJ, endereço, data de nascimento, foto comum e histórico de compra. Esses dados identificam ou podem identificar uma pessoa, mas não são classificados como dados sensíveis apenas por essa razão.",
        },
        {
          label: "Dados pessoais críticos",
          text:
            "Incluem CPF, CNPJ, endereço, telefone, chave PIX, dados bancários, comprovantes, registros de acesso, permissões administrativas e QR Code de ingresso. Recebem controle de acesso e retenção proporcional ao risco operacional, financeiro, antifraude e de segurança.",
        },
        {
          label: "Dados sensíveis ou potencialmente sensíveis",
          text:
            "Incluem biometria real, dados de saúde, dados corporais de treino quando usados para avaliação física, dados de menores e informações que revelem origem racial ou étnica, religião, opinião política, filiação sindical, saúde, vida sexual, dado genético ou biométrico, conforme a LGPD.",
        },
      ],
    },
    {
      title: "4. Dados coletados por funcionalidade",
      items: dataFeatureItems,
    },
    {
      title: "5. Como coletamos os dados",
      bullets: [
        "Diretamente do usuário, por cadastro, login, formulários, pedidos, inscrições, avaliações, publicações, votos e solicitações.",
        "Pelo uso da plataforma, incluindo navegação, ações administrativas, compras, check-ins, QR Code, presença, permissões, favoritos e interações.",
        "Por autenticação social, convites, integrações de pagamento, serviços de nuvem, suporte e registros técnicos necessários à segurança.",
        "Por organizações, organizadores, parceiros ou vendedores que inserem informações necessárias para operar módulos dentro da USC.",
      ],
    },
    {
      title: "6. Finalidades do tratamento",
      bullets: [
        "Criar e manter contas, autenticar usuários e controlar acesso.",
        "Operar lojas, pedidos, entrega, retirada, eventos, ingressos, QR Code, planos, benefícios, treinos, grupos, rankings, comunidade e suporte.",
        "Processar pagamentos, repasses, chargebacks, disputas, auditorias, prestação de contas e obrigações fiscais.",
        "Prevenir fraudes, abuso, spam, uso indevido, acessos não autorizados e violações de segurança.",
        "Melhorar a plataforma, gerar métricas internas, corrigir falhas, personalizar experiência e manter histórico operacional.",
        "Cumprir ordens legais, obrigações regulatórias, direitos de consumidores e solicitações de autoridades competentes.",
      ],
    },
    {
      title: "7. Bases legais utilizadas",
      items: [
        {
          label: "Execução de contrato ou procedimentos preliminares",
          text:
            "Usada em cadastro, compras, eventos, planos, ingressos, mini vendor, repasses, entrega, retirada, suporte e demais funcionalidades solicitadas pelo usuário.",
        },
        {
          label: "Obrigação legal ou regulatória",
          text:
            "Usada para dados fiscais, contábeis, registros necessários, cumprimento de ordens legais, prevenção à fraude quando aplicável e guarda exigida por normas.",
        },
        {
          label: "Legítimo interesse",
          text:
            "Usado para segurança, prevenção de abuso, melhoria da plataforma, métricas internas não invasivas, moderação, auditoria e proteção da USC, sempre respeitando direitos e expectativas legítimas dos titulares.",
        },
        {
          label: "Consentimento",
          text:
            "Usado para cookies não essenciais, marketing, comunicações promocionais, funcionalidades opcionais e tratamento de dados sensíveis quando aplicável e exigido.",
        },
        {
          label: "Exercício regular de direitos",
          text:
            "Usado para defesa da USC, resposta a reclamações, disputas, chargebacks, auditorias, processos administrativos, judiciais ou arbitrais.",
        },
        {
          label: "Proteção do crédito e prevenção de fraude",
          text:
            "Pode ser usada em contexto de pagamento, venda, repasse, estorno, chargeback e validação de transações, conforme a legislação aplicável.",
        },
      ],
    },
    {
      title: "8. Dados sensíveis e dados de menores, se aplicável",
      body: [
        "A USC pode tratar dados sensíveis apenas quando a funcionalidade exigir e houver base legal adequada, transparência, minimização e controles reforçados. Exemplos incluem dados corporais, biométricos ou de saúde em funcionalidades esportivas, quando realmente necessários.",
        "Quando houver uso por menores de idade, a USC e as organizações responsáveis devem observar as regras da LGPD, o melhor interesse do menor e eventuais exigências de consentimento ou representação legal.",
      ],
    },
    {
      title: "9. Cookies, localStorage, analytics e tecnologias semelhantes",
      body: [
        "A USC usa cookies, localStorage, sessionStorage, tokens e tecnologias semelhantes para autenticação, segurança, sessão, carrinho, checkout, tenant ativo, preferências, tema, desempenho e registro de escolhas de privacidade.",
        "Cookies essenciais podem funcionar independentemente de consentimento, pois viabilizam a prestação do serviço. Cookies não essenciais, analytics e marketing só devem ser ativados quando houver base legal adequada e, quando exigido, consentimento explícito.",
      ],
    },
    {
      title: "10. Compartilhamento de dados",
      body: [
        "Dados podem ser compartilhados com provedores de autenticação, infraestrutura em nuvem, banco de dados, hospedagem, processamento de pagamento, entrega, comunicação, suporte, prevenção a fraude, auditoria e ferramentas necessárias para operar a USC.",
        "Também pode haver compartilhamento com organizações, organizadores, parceiros, vendedores e administradores autorizados, limitado ao necessário para operar eventos, lojas, planos, grupos, treinos, benefícios, suporte e governança.",
        "A USC não vende dados pessoais. Compartilhamentos devem observar necessidade, finalidade, segurança, contratos aplicáveis e bases legais adequadas.",
      ],
    },
    {
      title: "11. Relação entre USC, tenants, organizadores, parceiros, mini vendors e usuários",
      body: [
        "A USC fornece infraestrutura tecnológica multitenant. Organizações, organizadores, parceiros e mini vendors podem definir ofertas, eventos, produtos, regras de participação, benefícios, entregas, retiradas, moderação e permissões dentro da plataforma.",
        "Quando uma organização define a finalidade e os meios de determinada atividade, ela também pode ser responsável pelo tratamento de dados relacionado a essa operação. A USC pode atuar como operadora ou corresponsável conforme o caso concreto.",
      ],
    },
    {
      title: "12. Transferência internacional",
      body: [
        "A USC pode utilizar serviços de nuvem, autenticação, hospedagem, banco de dados, segurança, suporte ou processamento com infraestrutura localizada fora do Brasil. Nesses casos, a transferência internacional observará a LGPD e medidas compatíveis de proteção.",
      ],
    },
    {
      title: "13. Segurança da informação",
      body: [
        "A USC adota medidas técnicas e administrativas para proteger dados pessoais, incluindo HTTPS, autenticação, controle de permissões, logs, segmentação por contexto, restrição de acesso, monitoramento e práticas de desenvolvimento seguro.",
        "Nenhuma plataforma digital é absolutamente imune a incidentes. Por isso, a USC mantém controles proporcionais ao risco e busca responder rapidamente a falhas, abuso ou acesso indevido.",
      ],
    },
    {
      title: "14. Retenção e eliminação de dados",
      body: [
        "Os dados são mantidos pelo tempo necessário para cumprir as finalidades informadas, obrigações legais, fiscais e contábeis, segurança, auditoria, prestação de contas, defesa de direitos e continuidade operacional.",
        "Quando não houver necessidade de retenção, os dados poderão ser eliminados, anonimizados ou bloqueados, conforme viabilidade técnica, obrigação legal e direitos de terceiros.",
      ],
    },
    {
      title: "15. Direitos dos titulares",
      body: [
        "Nos termos da LGPD, titulares podem solicitar confirmação de tratamento, acesso, correção, anonimização, bloqueio, eliminação, portabilidade, informação sobre compartilhamento, informação sobre consequências de não consentir, revogação de consentimento, revisão de decisões automatizadas e peticionamento perante autoridades competentes.",
      ],
    },
    {
      title: "16. Como exercer direitos",
      body: [
        "Solicitações devem ser enviadas ao canal de privacidade da USC indicado nesta política. A USC poderá solicitar informações mínimas para confirmar identidade e proteger dados contra acesso indevido.",
        "Quando a solicitação envolver atividade definida por uma organização, organizador, parceiro ou vendedor, a USC poderá encaminhar ou coordenar a resposta com o responsável correspondente.",
      ],
    },
    {
      title: "17. Incidentes de segurança",
      body: [
        "Em caso de incidente de segurança com risco ou dano relevante, a USC avaliará o evento, adotará medidas de contenção e, quando exigido pela legislação, comunicará titulares e autoridades competentes em prazo adequado.",
      ],
    },
    {
      title: "18. Atualizações da política",
      body: [
        "Esta política pode ser atualizada para refletir mudanças legais, técnicas, operacionais ou de negócio. A versão vigente será publicada nesta página, com indicação da data de última atualização.",
      ],
    },
    {
      title: "19. Contato e encarregado/DPO",
      body: [
        "Para dúvidas, solicitações LGPD ou questões sobre privacidade, utilize os canais institucionais abaixo. Os placeholders devem ser substituídos pelos dados oficiais antes da publicação definitiva.",
      ],
      items: institutionalFields,
    },
  ],
};

export const termsOfServiceDocument: LegalDocument = {
  slug: "termos-de-servico",
  title: "Termos de Serviço – USC – Universidade Spot Connect",
  description:
    "Regras de acesso e uso da USC como plataforma multitenant para organizações, usuários, compradores, participantes e administradores.",
  lastUpdated: LEGAL_LAST_UPDATED,
  sections: [
    {
      title: "1. Aceitação dos termos",
      body: [
        "Estes Termos de Serviço regulam o acesso e o uso da USC – Universidade Spot Connect, incluindo site, aplicativo, áreas autenticadas, páginas públicas, painéis administrativos, loja, eventos, ingressos, planos, treinos, ligas, comissões, diretórios, comunidade, parceiros, mini vendors, BI, permissões e demais funcionalidades.",
        "Ao criar conta, acessar a plataforma, aceitar estes Termos, solicitar participação em organização, comprar produto, adquirir ingresso, aderir a plano, cadastrar produto, operar painel administrativo ou continuar usando a USC, o usuário declara que leu, entendeu e concorda com estes Termos e com a Política de Privacidade.",
      ],
    },
    {
      title: "2. Descrição da USC como plataforma multitenant",
      body: [
        "A USC é uma infraestrutura digital multitenant que permite que organizações autorizadas operem espaços, conteúdos, lojas, eventos, planos, grupos, treinos, permissões, comunidades e rotinas administrativas dentro da mesma plataforma.",
        "As organizações internas usam a infraestrutura da USC, mas as páginas legais públicas globais pertencem à USC como plataforma.",
      ],
    },
    {
      title: "3. Perfis de usuários",
      bullets: [
        "Visitantes podem navegar por áreas públicas e conhecer funcionalidades disponíveis.",
        "Usuários cadastrados podem acessar módulos liberados conforme vínculo, aprovação, convite, plano ou permissão.",
        "Administradores, representantes, vendedores, parceiros, organizadores e usuários master possuem responsabilidades adicionais compatíveis com seus acessos.",
      ],
    },
    {
      title: "4. Cadastro e responsabilidade por credenciais",
      body: [
        "O usuário deve fornecer informações verdadeiras, atualizadas e compatíveis com a finalidade do cadastro. A USC pode exigir validação, convite, aprovação administrativa ou atualização de dados para liberar determinadas funcionalidades.",
        "Credenciais, sessões, dispositivos e contas são pessoais. O usuário é responsável por preservar acesso, senha, e-mail, autenticação social e dispositivos utilizados.",
      ],
    },
    {
      title: "5. Uso por tenants, atléticas, ligas, comissões, diretórios, mini vendors e parceiros",
      body: [
        "Organizações, ligas, comissões, diretórios, organizadores, parceiros e mini vendors devem usar a USC conforme a lei, estes Termos, as permissões concedidas e as regras operacionais da plataforma.",
        "Quem administra módulos deve manter informações corretas, respeitar direitos de titulares, consumidores e participantes, evitar abuso de permissões e zelar por conteúdo, preços, produtos, eventos, benefícios, listas, pagamentos e comunicações publicados.",
      ],
    },
    {
      title: "6. Regras de loja, produtos, pedidos, retirada e entrega",
      body: [
        "Produtos, preços, descrições, disponibilidade, retirada, entrega, prazos, trocas e suporte devem ser apresentados de forma clara. Quando houver relação de consumo, aplicam-se o Código de Defesa do Consumidor – Lei nº 8.078/1990 e demais normas aplicáveis.",
        "A USC pode registrar pedidos, pagamentos, comprovantes, status, responsáveis por aprovação, entrega ou retirada e histórico operacional para viabilizar a compra, suporte, auditoria, prestação de contas e prevenção a fraude.",
      ],
    },
    {
      title: "7. Regras de eventos, ingressos, QR Code, check-in, transferência e cancelamento",
      body: [
        "Eventos podem envolver lotes, categorias, inscrições, ingressos, QR Code, check-in, listas, transferência, validação de entrada, controle de lotação, retirada de fichas e regras próprias definidas pelo organizador.",
        "O usuário deve conferir dados do evento antes da compra ou inscrição. Cancelamentos, reembolsos e remarcações devem observar informações do evento, regras do organizador, legislação aplicável e, quando houver relação de consumo, o Código de Defesa do Consumidor.",
      ],
    },
    {
      title: "8. Planos de sócio e benefícios",
      body: [
        "Planos podem liberar benefícios, carteirinha, descontos, prioridade, acesso a recursos e status dentro da plataforma. A validade, renovação, cobrança, cancelamento e elegibilidade devem seguir as regras publicadas para cada oferta.",
        "Benefícios podem depender de pagamento confirmado, aprovação administrativa, vínculo ativo e regras operacionais.",
      ],
    },
    {
      title: "9. Treinos e atividades esportivas",
      body: [
        "Treinos e atividades esportivas podem registrar presença, modalidade, frequência, resultados e informações corporais quando necessárias. O usuário deve respeitar orientações, horários, regras de segurança e responsáveis pela atividade.",
        "A USC não substitui orientação médica, profissional de educação física, avaliação de saúde ou responsabilidade dos organizadores por atividades presenciais.",
      ],
    },
    {
      title: "10. Comunidade, posts, comentários, denúncias, moderação e banimento",
      body: [
        "A comunidade deve ser usada com respeito, finalidade legítima e responsabilidade. Posts, comentários, reviews, denúncias e interações podem ser moderados para prevenir spam, fraude, assédio, discriminação, abuso, conteúdo ilegal ou violação destes Termos.",
        "A USC e administradores autorizados podem remover conteúdo, restringir alcance, suspender funcionalidades ou banir usuários quando houver violação, risco à segurança ou exigência legal.",
      ],
    },
    {
      title: "11. Permissões, roles, administradores e usuário master",
      body: [
        "Permissões e roles definem quem pode visualizar, editar, aprovar, vender, escanear, moderar, configurar, auditar ou administrar módulos. Usuários com acesso elevado devem agir com diligência, rastreabilidade e finalidade legítima.",
        "A USC pode registrar logs e ações administrativas para segurança, auditoria, prevenção de abuso e defesa de direitos.",
      ],
    },
    {
      title: "12. Condutas proibidas",
      bullets: [
        "Usar a USC para fraude, golpe, assédio, ameaça, discriminação, discurso de ódio, spam, conteúdo ilegal ou violação de direitos de terceiros.",
        "Tentar burlar permissões, explorar falhas, invadir contas, raspar dados sem autorização, automatizar abusivamente ou prejudicar disponibilidade da plataforma.",
        "Publicar informações falsas, produtos inexistentes, eventos enganosos, comprovantes adulterados ou dados de terceiros sem autorização.",
        "Usar marca, logo, conteúdo, código, dados ou interfaces da USC de forma não autorizada.",
      ],
    },
    {
      title: "13. Propriedade intelectual",
      body: [
        "A USC, seus elementos visuais, software, interfaces, fluxos, textos, estruturas, marcas, logos e componentes pertencem à USC ou a seus licenciantes, salvo conteúdo de terceiros ou de organizações publicado com autorização.",
        "O uso da plataforma não transfere propriedade intelectual ao usuário.",
      ],
    },
    {
      title: "14. Conteúdo gerado pelo usuário",
      body: [
        "O usuário mantém direitos sobre conteúdo que criar, quando aplicável, mas concede à USC uma licença limitada, não exclusiva e necessária para hospedar, exibir, moderar, processar, armazenar e disponibilizar esse conteúdo dentro da plataforma.",
        "O usuário declara ter direitos ou autorização para publicar o conteúdo e responde por violações legais ou de terceiros.",
      ],
    },
    {
      title: "15. Pagamentos, repasses, chargeback, disputas e estornos",
      body: [
        "Pagamentos, repasses, estornos, chargebacks e disputas podem envolver provedores de pagamento, administradores, organizadores, vendedores e parceiros. A USC pode registrar evidências operacionais, comprovantes, logs e status para auditoria e defesa.",
        "Repasses a mini vendors, parceiros ou organizações dependem de aprovação, dados corretos, conformidade fiscal, regras da plataforma e ausência de indícios de fraude ou disputa relevante.",
      ],
    },
    {
      title: "16. Responsabilidade da USC versus responsabilidade do tenant/organizador/vendedor",
      body: [
        "A USC é responsável pela infraestrutura tecnológica que disponibiliza, dentro dos limites destes Termos e da legislação aplicável.",
        "Organizações, organizadores, parceiros e vendedores são responsáveis por informações que publicam, eventos que organizam, produtos que vendem, preços, entrega, retirada, benefícios, regras próprias, atendimento, cumprimento de ofertas e tratamento de dados sob sua decisão.",
      ],
    },
    {
      title: "17. Isenções e limitações de responsabilidade",
      body: [
        "A USC busca manter disponibilidade, segurança e funcionamento adequado, mas não garante operação ininterrupta, ausência absoluta de falhas, compatibilidade com todos os dispositivos ou resultados específicos.",
        "A USC não se responsabiliza por danos decorrentes de uso indevido, dados incorretos inseridos por usuários ou organizações, falhas de terceiros, indisponibilidade de provedores externos, eventos presenciais, produtos, entregas ou decisões operacionais de organizações, salvo quando a lei determinar de modo diferente.",
      ],
    },
    {
      title: "18. Suspensão, bloqueio e encerramento de conta",
      body: [
        "A USC pode suspender, bloquear, restringir ou encerrar contas e funcionalidades em caso de violação destes Termos, risco à segurança, fraude, abuso, ordem legal, inadimplência, uso indevido ou necessidade de proteção da plataforma e de terceiros.",
        "O usuário pode solicitar encerramento de conta, respeitadas retenções necessárias para obrigações legais, prestação de contas, segurança, auditoria, defesa de direitos e histórico operacional.",
      ],
    },
    {
      title: "18.1 Idade mínima, menores de idade e eventos com bebida alcoólica",
      body: [
        "Nesta versão, o cadastro operacional da USC exige confirmação de 18 anos ou mais. Quando houver fluxo específico para menores, compras de produtos, participação em eventos, recursos sociais e tratamento de dados deverão observar autorização responsável, melhor interesse do menor, regras do tenant/organizador e legislação aplicável.",
        "Eventos com bebida alcoólica devem restringir consumo e acesso conforme a idade mínima legal, exigir documento oficial quando necessário e deixar claras as responsabilidades do organizador.",
      ],
    },
    {
      title: "18.2 Perfil público, perfil invisível e contatos visíveis",
      body: [
        "O usuário pode configurar preferências de visibilidade, incluindo telefone, foto e perfil público. Ao tornar o telefone visível, outros usuários poderão acessar esse contato no perfil, inclusive para iniciar conversa por WhatsApp.",
        "Ao tornar o perfil invisível, o usuário aceita que seu perfil apareça como invisível em áreas sociais, inclusive álbum da turma, e que a visualização de perfis de outras pessoas também seja bloqueada enquanto essa preferência estiver ativa.",
      ],
    },
    {
      title: "18.3 Lista de crush ou afinidade",
      body: [
        "Recursos de crush, afinidade ou matches são opcionais e devem ser tratados como dados sociais privados de alto risco de exposição. A USC deve restringir acesso, não exibir a administradores comuns, não publicar em BI identificável e eliminar registros quando a conta for excluída, ressalvadas necessidades técnicas, legais ou de segurança estritamente justificadas.",
      ],
    },
    {
      title: "19. Alterações dos termos",
      body: [
        "A USC pode atualizar estes Termos para refletir mudanças legais, técnicas, operacionais ou comerciais. A versão vigente será publicada nesta página, com indicação da data de atualização.",
      ],
    },
    {
      title: "20. Lei aplicável e foro",
      body: [
        "Estes Termos são regidos pelas leis da República Federativa do Brasil, incluindo, quando aplicáveis, a LGPD, o Marco Civil da Internet e o Código de Defesa do Consumidor.",
        "O foro aplicável deverá ser definido conforme a legislação brasileira e as informações institucionais definitivas da operadora da USC, sem prejuízo de direitos legais de consumidores quando aplicável.",
      ],
    },
    {
      title: "21. Contato",
      body: [
        "Dúvidas sobre estes Termos devem ser enviadas pelos canais oficiais da USC. Os placeholders abaixo devem ser substituídos pelos dados institucionais definitivos.",
      ],
      items: institutionalFields,
    },
  ],
};

export const cookiesPolicyDocument: LegalDocument = {
  slug: "politica-cookies",
  title: "Política de Cookies – USC – Universidade Spot Connect",
  description:
    "Como a USC utiliza cookies, localStorage, tokens e tecnologias semelhantes para segurança, autenticação, preferências e funcionalidades.",
  lastUpdated: LEGAL_LAST_UPDATED,
  sections: [
    {
      title: "1. O que são cookies, localStorage e tecnologias semelhantes",
      body: [
        "Cookies são pequenos arquivos ou identificadores armazenados pelo navegador. A USC também pode usar localStorage, sessionStorage, cache local, tokens, identificadores de sessão, service workers, SDKs, pixels e tecnologias semelhantes.",
        "Essas tecnologias ajudam a manter login, lembrar preferências, preservar carrinho, operar checkout, identificar tenant ativo, melhorar desempenho, registrar escolhas de privacidade, aumentar segurança e entender o uso da plataforma.",
      ],
    },
    {
      title: "2. Por que a USC utiliza essas tecnologias",
      bullets: [
        "Autenticar usuários e manter sessão segura.",
        "Guardar preferências de tema, interface e navegação.",
        "Preservar carrinho, checkout, ingresso, evento, rota e contexto de uso.",
        "Prevenir fraude, abuso, spam, acessos indevidos e falhas de segurança.",
        "Melhorar desempenho, estabilidade e experiência.",
        "Registrar consentimentos e preferências de privacidade.",
      ],
    },
    {
      title: "3. Cookies essenciais",
      body: [
        "São indispensáveis para funcionamento básico da USC, incluindo autenticação, sessão, carrinho, checkout, QR Code, permissões, tenant ativo, segurança, prevenção de fraude e registro de escolhas de privacidade.",
        "Cookies essenciais não dependem de consentimento, pois são necessários para prestar o serviço solicitado pelo usuário.",
      ],
    },
    {
      title: "4. Cookies de autenticação e segurança",
      body: [
        "A USC pode usar tokens e identificadores para login, autenticação social, sessão, controle de acesso, proteção contra abuso, rastreabilidade, auditoria e prevenção de fraude, em conformidade com a LGPD e o Marco Civil da Internet quando aplicável.",
      ],
    },
    {
      title: "5. Cookies/preferências de interface e tema",
      body: [
        "A plataforma pode guardar tema, identidade visual, filtros, módulos acessados, tenant ativo, preferências de navegação e configurações de interface para manter uma experiência consistente.",
      ],
    },
    {
      title: "6. Cookies de carrinho, checkout, evento e sessão",
      body: [
        "A USC pode usar armazenamento técnico para preservar carrinho, pedidos em andamento, inscrição em evento, ingresso, QR Code, lote, checkout, produto, plano, contexto de compra e retorno após autenticação.",
      ],
    },
    {
      title: "7. Cookies analíticos",
      body: [
        "A USC poderá usar tecnologias analíticas para entender páginas acessadas, tempo de carregamento, erros, cliques, módulos mais utilizados e estabilidade. Quando não forem essenciais, esses recursos dependerão de base legal adequada e, quando exigido, consentimento.",
        "Se analytics ainda não estiver implementado em determinada versão, a USC poderá implementá-lo futuramente mediante transparência e controle de consentimento quando exigido.",
      ],
    },
    {
      title: "8. Cookies de marketing, somente se implementados e mediante consentimento",
      body: [
        "Cookies, pixels ou identificadores de marketing podem ser usados para campanhas, mensuração, conversões, remarketing ou comunicações personalizadas apenas quando implementados, informados e autorizados conforme a base legal aplicável.",
      ],
    },
    {
      title: "9. Cookies de terceiros",
      body: [
        "Serviços de autenticação, pagamentos, hospedagem, banco de dados, segurança, suporte, mapas, mídia, analytics ou marketing podem usar tecnologias próprias. A USC busca limitar esses usos ao necessário e compatível com a finalidade informada.",
      ],
    },
    {
      title: "10. Como o usuário pode gerenciar ou excluir cookies",
      bullets: [
        "Usar o banner ou painel de preferências da USC quando disponível.",
        "Limpar cookies, cache, localStorage e dados do site nas configurações do navegador.",
        "Bloquear cookies de terceiros ou restringir rastreamento conforme recursos do navegador.",
        "Revogar consentimentos não essenciais quando a funcionalidade estiver disponível.",
      ],
    },
    {
      title: "11. Consequências de desativar cookies essenciais",
      body: [
        "Desativar cookies ou armazenamento essencial pode impedir login, compra, carrinho, checkout, ingresso, QR Code, permissões, tenant ativo, segurança e demais funcionalidades básicas.",
      ],
    },
    {
      title: "12. Atualizações e contato",
      body: [
        "Esta Política de Cookies pode ser atualizada para refletir mudanças técnicas, legais ou operacionais. Para dúvidas, utilize os canais oficiais abaixo.",
      ],
      items: institutionalFields,
    },
  ],
};

export const lgpdRightsDocument: LegalDocument = {
  slug: "direitos-lgpd",
  title: "Seus Direitos LGPD – USC – Universidade Spot Connect",
  description:
    "Como titulares de dados pessoais podem exercer direitos previstos na LGPD perante a USC.",
  lastUpdated: LEGAL_LAST_UPDATED,
  sections: [
    {
      title: "1. O que é a LGPD",
      body: [
        "A Lei Geral de Proteção de Dados Pessoais – LGPD, Lei nº 13.709/2018, regula o tratamento de dados pessoais no Brasil e garante direitos aos titulares.",
        "Este documento explica como exercer direitos relacionados a dados pessoais tratados pela USC – Universidade Spot Connect.",
      ],
    },
    {
      title: "2. Quem pode exercer direitos",
      bullets: [
        "O próprio titular dos dados.",
        "Representante legal, quando aplicável.",
        "Responsável legal por menor de idade, quando exigido por lei.",
        "Procurador com poderes suficientes, mediante comprovação.",
      ],
      note:
        "A USC poderá solicitar comprovação de identidade para impedir acesso indevido aos dados de outra pessoa.",
    },
    {
      title: "3. Direitos do titular",
      items: [
        {
          label: "Confirmação da existência de tratamento",
          text:
            "Você pode solicitar confirmação sobre a existência de tratamento de seus dados pessoais pela USC.",
        },
        {
          label: "Acesso aos dados",
          text:
            "Você pode solicitar cópia ou relatório dos dados pessoais tratados, respeitados segredos comerciais, segurança de terceiros, dados de outros usuários e limites legais.",
        },
        {
          label: "Correção",
          text:
            "Você pode pedir correção de dados incompletos, inexatos ou desatualizados. Alguns dados também podem ser corrigidos diretamente no perfil ou por administradores autorizados.",
        },
        {
          label: "Anonimização, bloqueio ou eliminação",
          text:
            "Você pode solicitar anonimização, bloqueio ou eliminação de dados desnecessários, excessivos ou tratados em desconformidade com a LGPD.",
        },
        {
          label: "Portabilidade",
          text:
            "Você pode solicitar portabilidade dos dados a outro fornecedor, quando regulamentada pela autoridade competente e tecnicamente viável.",
        },
        {
          label: "Eliminação de dados tratados com consentimento",
          text:
            "Quando o tratamento estiver baseado em consentimento, você pode solicitar eliminação dos dados, respeitadas exceções legais.",
        },
        {
          label: "Informação sobre compartilhamento",
          text:
            "Você pode solicitar informações sobre entidades públicas e privadas com as quais a USC realizou uso compartilhado de dados.",
        },
        {
          label: "Informação sobre possibilidade de não consentir",
          text:
            "Você pode pedir esclarecimentos sobre a possibilidade de não fornecer consentimento e as consequências dessa escolha.",
        },
        {
          label: "Revogação do consentimento",
          text:
            "Você pode revogar consentimentos concedidos, sem afetar tratamentos realizados anteriormente de forma válida.",
        },
        {
          label: "Revisão de decisões automatizadas",
          text:
            "Você pode solicitar revisão de decisões tomadas exclusivamente com base em tratamento automatizado que afetem seus interesses, quando aplicável.",
        },
        {
          label: "Peticionamento",
          text:
            "Você pode peticionar perante a ANPD e órgãos de defesa do consumidor, conforme a legislação aplicável.",
        },
      ],
    },
    {
      title: "4. Como solicitar",
      body: [
        "Envie sua solicitação ao canal LGPD/Encarregado da USC. Informe o direito que deseja exercer e descreva o pedido com clareza.",
        "Você também pode registrar a solicitação pelo formulário público em /direitos-lgpd/solicitar.",
        "Quando a solicitação envolver uma organização, evento, loja, grupo ou vendedor específico dentro da USC, informe esse contexto para facilitar a localização dos dados.",
      ],
      items: [
        { label: "Canal LGPD/Encarregado", text: "[EMAIL DO ENCARREGADO OU PRIVACIDADE]" },
        { label: "Contato oficial", text: "[EMAIL OFICIAL DA USC]" },
      ],
    },
    {
      title: "5. Dados necessários para confirmar identidade",
      bullets: [
        "Nome completo.",
        "E-mail utilizado na USC.",
        "Identificador de conta, quando conhecido.",
        "Descrição do vínculo com a USC ou com uma organização dentro da plataforma.",
        "Documento ou comprovação adicional, apenas quando necessário para proteger o titular contra acesso indevido.",
      ],
    },
    {
      title: "6. Prazo de resposta",
      body: [
        "A USC responderá às solicitações nos prazos previstos na LGPD e na regulamentação aplicável. Quando a resposta depender de confirmação de identidade, informações adicionais ou coordenação com uma organização responsável por determinado módulo, o prazo poderá ser contado a partir do recebimento das informações necessárias.",
      ],
    },
    {
      title: "7. Situações em que a USC pode manter dados mesmo após pedido de exclusão",
      bullets: [
        "Cumprimento de obrigação legal ou regulatória.",
        "Guarda fiscal, contábil, contratual ou de prestação de contas.",
        "Prevenção a fraude, segurança, auditoria e investigação de abuso.",
        "Exercício regular de direitos em processos judiciais, administrativos ou arbitrais.",
        "Proteção de direitos de terceiros, histórico de transações, chargebacks, disputas, ingressos, eventos e obrigações de consumo.",
        "Anonimização para fins estatísticos, operacionais ou históricos, quando cabível.",
      ],
    },
    {
      title: "8. Canal de contato",
      body: [
        "Utilize os canais institucionais abaixo para dúvidas ou solicitações relacionadas a dados pessoais. Os placeholders devem ser substituídos pelos dados oficiais antes da publicação definitiva.",
      ],
      items: institutionalFields,
    },
    {
      title: "9. Modelo de solicitação, sem usar dados reais",
      body: [
        "Assunto: Solicitação LGPD – USC – Universidade Spot Connect",
        "Prezados, solicito o exercício do seguinte direito previsto na LGPD: [INFORME O DIREITO DESEJADO]. Meus dados de identificação para localização da conta são: nome [SEU NOME], e-mail de cadastro [SEU E-MAIL DE CADASTRO] e contexto de uso [INFORME O CONTEXTO, SE HOUVER]. Declaro que as informações fornecidas são verdadeiras e aguardo orientação sobre eventual confirmação de identidade.",
      ],
    },
  ],
};

export const adminConfidentialityDocument: LegalDocument = {
  slug: "termo-confidencialidade-admin",
  title: "Termo de Confidencialidade e Uso Administrativo – USC – Universidade Spot Connect",
  description:
    "Regras de sigilo, acesso mínimo necessário e uso responsável de dados por administradores, gestores, operadores e representantes autorizados.",
  lastUpdated: LEGAL_LAST_UPDATED,
  sections: [
    {
      title: "1. Finalidade do termo",
      body: [
        "Este termo estabelece obrigações de confidencialidade para usuários com acesso administrativo na USC, incluindo master tenant, administradores, gestores financeiros, editores de evento, gestores de loja, gestores de mini vendor, representantes de ligas, comissões e diretórios, operadores de scanner/check-in e demais perfis com acesso a dados de terceiros.",
      ],
    },
    {
      title: "2. Dados protegidos",
      body: [
        "O acesso administrativo pode envolver dados pessoais comuns, dados pessoais críticos, registros financeiros, comprovantes, permissões, logs, QR Codes, dados de check-in, pedidos, inscrições, comunicações e informações operacionais de usuários finais.",
        "CPF, CNPJ, telefone, endereço, PIX, dados bancários, comprovantes, logs e QR Codes são tratados como dados pessoais críticos e exigem cuidado reforçado, ainda que não sejam automaticamente dados sensíveis na definição técnica da LGPD.",
      ],
    },
    {
      title: "3. Obrigações do administrador",
      bullets: [
        "Usar dados somente para finalidade operacional legítima dentro da USC.",
        "Não copiar, exportar, compartilhar ou fotografar dados fora de necessidade autorizada.",
        "Não consultar dados por curiosidade, interesse pessoal, perseguição, exposição ou constrangimento.",
        "Manter sigilo sobre informações financeiras, pedidos, inscrições, listas, logs, denúncias, dados de perfil e dados de contato.",
        "Comunicar suspeita de incidente, acesso indevido, vazamento, erro de permissão ou uso abusivo.",
      ],
    },
    {
      title: "4. Ações críticas e auditoria",
      body: [
        "A USC pode registrar logs de auditoria para alteração de roles, alteração de PIX/recebedor, aprovação ou rejeição de cadastro, check-in manual, reimpressão ou abertura de QR Code, transferência de ingresso, aprovação de pagamento, cancelamento de pedido, banimento, exclusão de post, exportação de dados, visualização de dados financeiros e alteração de políticas ou termos.",
      ],
    },
    {
      title: "5. Consequências de uso indevido",
      body: [
        "O uso indevido de dados pode gerar bloqueio de acesso, revogação de permissões, comunicação ao tenant responsável, preservação de evidências, medidas administrativas, contratuais e legais cabíveis.",
      ],
    },
    {
      title: "6. Aceite e versão",
      body: [
        "O aceite deste termo deve ser registrado com usuário, versão, data, IP, user-agent, tenant e origem quando o usuário receber ou exercer função administrativa relevante.",
      ],
    },
  ],
};

export const tenantOrganizerTermsDocument: LegalDocument = {
  slug: "termos-tenants-organizadores",
  title: "Termos de Uso para Tenants, Organizadores e Entidades – USC – Universidade Spot Connect",
  description:
    "Regras específicas para organizações, organizadores, entidades, parceiros e vendedores que operam módulos dentro da USC.",
  lastUpdated: LEGAL_LAST_UPDATED,
  sections: [
    {
      title: "1. Aplicação",
      body: [
        "Estes termos se aplicam a entidades, atléticas, ligas, comissões, diretórios, organizadores, parceiros, mini vendors, vendedores e representantes que utilizem módulos da USC para operar eventos, loja, planos, conteúdo, grupos, permissões e rotinas administrativas.",
      ],
    },
    {
      title: "2. Responsabilidade do tenant",
      bullets: [
        "Definir informações corretas de eventos, produtos, planos, preços, regras, prazos, benefícios e canais de suporte.",
        "Cumprir o Código de Defesa do Consumidor – Lei nº 8.078/1990 quando houver relação de consumo.",
        "Não ofertar produtos, serviços, eventos ou conteúdos ilegais, enganosos, discriminatórios ou sem autorização.",
        "Orientar seus administradores sobre sigilo, uso correto de dados e permissões.",
        "Responder por conteúdo publicado, regras próprias, cancelamentos, comunicação com participantes e execução operacional sob sua responsabilidade.",
      ],
    },
    {
      title: "3. Eventos, ingressos e cancelamentos",
      body: [
        "O tenant ou organizador é responsável por regras de evento, lote, check-in, QR Code, transferência, restrições de entrada, política de cancelamento, adiamento, reembolso, consumíveis, ficha, pulseira e comunicação com participantes.",
        "Eventos com bebida alcoólica devem observar idade mínima legal, controle de acesso, documentação oficial e regras locais aplicáveis.",
      ],
    },
    {
      title: "4. Loja, produtos e mini vendors",
      body: [
        "O tenant, parceiro ou vendedor é responsável por descrição, disponibilidade, entrega, retirada, troca, produto personalizado, estoque, suporte, repasse, chargeback e contestações relacionadas aos produtos ou serviços que operar.",
      ],
    },
    {
      title: "5. Dados pessoais e sigilo",
      body: [
        "O tenant deve acessar dados pessoais apenas quando necessário para a operação autorizada, respeitando a LGPD – Lei nº 13.709/2018, o Marco Civil da Internet – Lei nº 12.965/2014 quando aplicável e as políticas da USC.",
        "Lista de crush/afinidade, denúncias, logs sensíveis, dados financeiros e dados de menores ou de saúde não devem ser expostos a administradores sem necessidade e autorização técnica adequada.",
      ],
    },
    {
      title: "6. Políticas próprias visíveis",
      body: [
        "A USC disponibiliza área administrativa para políticas do tenant, como reembolso, cancelamento, menores de idade, bebidas alcoólicas, loja, planos, checkout e mini vendor. Essas políticas só devem aparecer ao público quando estiverem preenchidas e marcadas como visíveis.",
      ],
    },
    {
      title: "7. Repasses, disputas e chargebacks",
      body: [
        "O tenant deve manter dados de recebedor corretos, comprovar operações quando solicitado, colaborar em chargebacks, disputas e estornos e respeitar regras financeiras, fiscais e de prestação de contas aplicáveis.",
      ],
    },
    {
      title: "8. Uso de marca e conteúdo",
      body: [
        "O tenant é responsável pelo uso de marcas, imagens, textos, logos, materiais promocionais e conteúdos que publicar na USC, garantindo que possui autorização para uso e que não viola direitos de terceiros.",
      ],
    },
  ],
};

export const legalDocumentsBySlug = {
  "politica-privacidade": privacyPolicyDocument,
  "termos-de-servico": termsOfServiceDocument,
  "politica-cookies": cookiesPolicyDocument,
  "direitos-lgpd": lgpdRightsDocument,
  "termo-confidencialidade-admin": adminConfidentialityDocument,
  "termos-tenants-organizadores": tenantOrganizerTermsDocument,
} as const;
