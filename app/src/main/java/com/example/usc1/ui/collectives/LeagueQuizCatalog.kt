package com.example.usc1.ui.collectives

/**
 * Porte de `web-reference/src/constants/leagueQuizProfiles.ts` e do bloco `QUESTIONS`
 * de `web-reference/src/app/ligas_usc/page.tsx` (Oráculo de compatibilidade por liga).
 */
enum class LeagueQuizQuestionKey {
    Scenario,
    Audience,
    System,
    Style,
    Impact,
}

data class LeagueQuizProfile(
    val nome: String,
    val sigla: String = "",
    val aliases: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
    val quizAnswers: Map<LeagueQuizQuestionKey, List<String>> = emptyMap(),
)

data class LeagueQuizOption(
    val label: String,
    val keywords: List<String>,
)

data class LeagueQuizQuestion(
    val id: Int,
    val key: LeagueQuizQuestionKey,
    val text: String,
    val options: List<LeagueQuizOption>,
)

data class LeagueQuizMatch(
    val collective: CollectiveGroup,
    val matchScore: Int,
    val matchPercent: Int,
)

object LeagueQuizCatalog {
    /** `QUIZ_DIRECT_MATCH_WEIGHT` do web. */
    const val DirectMatchWeight = 3

    /** Máximo de opções selecionáveis por pergunta, igual ao `toggleOption` do web. */
    const val MaxSelectedOptions = 3

    /** Quantidade de ligas exibidas no resultado (`scored.slice(0, 5)`). */
    const val TopMatches = 5

    val questions: List<LeagueQuizQuestion> = listOf(
        LeagueQuizQuestion(
            id = 1,
            key = LeagueQuizQuestionKey.Scenario,
            text = "Qual cenário faz seus olhos brilharem?",
            options = listOf(
                LeagueQuizOption("Centro Cirúrgico", listOf("trauma", "cirurgia", "laparoscopia", "robotica", "ortopedia")),
                LeagueQuizOption("Emergência", listOf("emergencia", "urgencia", "trauma", "intensiva", "resgate")),
                LeagueQuizOption("Consultório", listOf("clinica", "endocrino", "dermato", "gastro", "ambulatorio")),
                LeagueQuizOption("Comunidade", listOf("familia", "comunidade", "pediatria", "gineco", "humanidades")),
                LeagueQuizOption("Laboratório", listOf("patologia", "radiologia", "genetica", "anatomia", "simulacao")),
            ),
        ),
        LeagueQuizQuestion(
            id = 2,
            key = LeagueQuizQuestionKey.Audience,
            text = "Com qual público você tem mais afinidade?",
            options = listOf(
                LeagueQuizOption("Crianças", listOf("pediatria", "neonatologia", "infancia")),
                LeagueQuizOption("Mulheres", listOf("gineco", "obstetricia", "saude da mulher")),
                LeagueQuizOption("Adultos", listOf("geriatria", "clinica", "cardio", "oncologia")),
                LeagueQuizOption("Graves", listOf("intensiva", "anestesiologia", "trauma", "urgencia")),
                LeagueQuizOption("Atletas", listOf("esportiva", "ortopedia", "performance")),
            ),
        ),
        LeagueQuizQuestion(
            id = 3,
            key = LeagueQuizQuestionKey.System,
            text = "Qual sistema te fascina?",
            options = listOf(
                LeagueQuizOption("Cérebro", listOf("neuro", "psiquiatria", "neurologia")),
                LeagueQuizOption("Coração", listOf("cardio", "coracao", "cardiovascular")),
                LeagueQuizOption("Ossos", listOf("ortopedia", "anatomia", "ossos")),
                LeagueQuizOption("Hormônios", listOf("gastro", "endocrino", "metabolismo", "obstetricia")),
                LeagueQuizOption("Rins", listOf("nefro", "urologia", "rins")),
            ),
        ),
        LeagueQuizQuestion(
            id = 4,
            key = LeagueQuizQuestionKey.Style,
            text = "Qual é o seu estilo de prática?",
            options = listOf(
                LeagueQuizOption("Manual", listOf("cirurgia", "trauma", "procedimento", "tecnica")),
                LeagueQuizOption("Raciocínio", listOf("clinica", "diagnostico", "investigacao")),
                LeagueQuizOption("Prevenção", listOf("familia", "pediatria", "promocao", "saude coletiva")),
                LeagueQuizOption("Tecnologia", listOf("radiologia", "cardio", "robotica", "simulacao")),
                LeagueQuizOption("Gestão", listOf("legal", "trabalho", "militar", "organizacao")),
            ),
        ),
        LeagueQuizQuestion(
            id = 5,
            key = LeagueQuizQuestionKey.Impact,
            text = "Qual impacto você mais quer causar?",
            options = listOf(
                LeagueQuizOption("Salvar vidas", listOf("emergencia", "trauma", "ressuscitacao", "uti")),
                LeagueQuizOption("Paciência", listOf("psiquiatria", "oncologia", "seguimento")),
                LeagueQuizOption("Detalhe", listOf("oftalmo", "dermato", "microcirurgia", "precisao")),
                LeagueQuizOption("Curiosidade", listOf("genetica", "patologia", "anatomia", "simulacao")),
                LeagueQuizOption("Vínculo", listOf("familia", "onco", "comunidade", "acolhimento")),
            ),
        ),
    )

    /** `KEYWORD_SYNONYMS` do web. */
    val keywordSynonyms: Map<String, List<String>> = mapOf(
        "clinica" to listOf("consultorio", "diagnostico"),
        "familia" to listOf("comunidade", "prevencao", "vinculo"),
        "emergencia" to listOf("urgencia", "trauma", "intensiva"),
        "cardio" to listOf("coracao", "cardiologia"),
        "neuro" to listOf("neurologia", "neurocirurgia"),
        "gineco" to listOf("ginecologia", "obstetricia", "mulheres"),
        "ortopedia" to listOf("ossos", "esportiva", "atletas"),
        "endocrino" to listOf("hormonios", "metabolismo"),
        "psiquiatria" to listOf("saude mental", "cerebro"),
        "onco" to listOf("oncologia", "cancer"),
        "legal" to listOf("forense", "pericia", "etica"),
        "oftalmo" to listOf("oftalmologia", "detalhe"),
        "urologia" to listOf("rins", "nefro"),
        "cirurgia" to listOf("manual", "centro cirurgico", "laparoscopia", "robotica"),
        "pediatria" to listOf("neonatologia", "criancas"),
        "gastro" to listOf("digestiva", "endoscopia"),
        "simulacao" to listOf("treinamento", "cenario"),
        "militar" to listOf("resgate", "estrategia"),
        "anatomia" to listOf("disseccao", "morfologia"),
        "humanidades" to listOf("social", "escuta"),
        "otorrino" to listOf("vias aereas", "ouvido", "garganta"),
        "laparoscopia" to listOf("robotica", "cirurgia"),
    )

    val profiles: List<LeagueQuizProfile> = listOf(
        LeagueQuizProfile(
            nome = "Liga Academica de Emergencia",
            sigla = "LAMEI",
            aliases = listOf("LAME", "emergencia", "urgencia"),
            keywords = listOf("emergencia", "urgencia", "trauma", "intensiva", "choque", "resgate"),
            quizAnswers = mapOf(
                LeagueQuizQuestionKey.Scenario to listOf("Emergencia", "Centro Cirurgico"),
                LeagueQuizQuestionKey.Audience to listOf("Graves", "Adultos"),
                LeagueQuizQuestionKey.System to emptyList(),
                LeagueQuizQuestionKey.Style to listOf("Manual", "Raciocinio"),
                LeagueQuizQuestionKey.Impact to listOf("Salvar vidas"),
            ),
        ),
        LeagueQuizProfile(
            nome = "Liga Academica de Cirurgia Geral",
            sigla = "LIAC",
            aliases = listOf("cirurgia geral"),
            keywords = listOf("cirurgia", "centro cirurgico", "manual", "trauma", "procedimento"),
            quizAnswers = mapOf(
                LeagueQuizQuestionKey.Scenario to listOf("Centro Cirurgico"),
                LeagueQuizQuestionKey.Audience to listOf("Adultos", "Graves"),
                LeagueQuizQuestionKey.System to emptyList(),
                LeagueQuizQuestionKey.Style to listOf("Manual"),
                LeagueQuizQuestionKey.Impact to listOf("Salvar vidas", "Detalhe"),
            ),
        ),
        LeagueQuizProfile(
            nome = "Liga Academica de Endocrinologia e Metabologia",
            sigla = "LAEM",
            aliases = listOf("endocrinologia", "metabologia"),
            keywords = listOf("endocrino", "hormonios", "metabolismo", "clinica", "consultorio"),
            quizAnswers = mapOf(
                LeagueQuizQuestionKey.Scenario to listOf("Consultorio"),
                LeagueQuizQuestionKey.Audience to listOf("Adultos"),
                LeagueQuizQuestionKey.System to listOf("Hormonios"),
                LeagueQuizQuestionKey.Style to listOf("Raciocinio"),
                LeagueQuizQuestionKey.Impact to listOf("Paciencia"),
            ),
        ),
        LeagueQuizProfile(
            nome = "Liga Academica de Ginecologia e Obstetricia",
            sigla = "LIAGO",
            aliases = listOf("ginecologia", "obstetricia", "saude da mulher"),
            keywords = listOf("gineco", "obstetricia", "mulheres", "familia", "prenatal"),
            quizAnswers = mapOf(
                LeagueQuizQuestionKey.Scenario to listOf("Consultorio", "Comunidade"),
                LeagueQuizQuestionKey.Audience to listOf("Mulheres"),
                LeagueQuizQuestionKey.System to listOf("Hormonios"),
                LeagueQuizQuestionKey.Style to listOf("Raciocinio", "Prevencao"),
                LeagueQuizQuestionKey.Impact to listOf("Vinculo", "Salvar vidas"),
            ),
        ),
        LeagueQuizProfile(
            nome = "Liga Academica de Medicina Legal de Caraguatatuba",
            sigla = "LAMELC",
            aliases = listOf("medicina legal", "forense"),
            keywords = listOf("legal", "forense", "pericia", "etica", "laudo"),
            quizAnswers = mapOf(
                LeagueQuizQuestionKey.Scenario to listOf("Laboratorio"),
                LeagueQuizQuestionKey.Audience to listOf("Adultos"),
                LeagueQuizQuestionKey.System to emptyList(),
                LeagueQuizQuestionKey.Style to listOf("Gestao", "Raciocinio"),
                LeagueQuizQuestionKey.Impact to listOf("Curiosidade"),
            ),
        ),
        LeagueQuizProfile(
            nome = "Liga Academica de Anatomia e Saude",
            sigla = "LAAS",
            aliases = listOf("anatomia"),
            keywords = listOf("anatomia", "disseccao", "morfologia", "saude", "estudo"),
            quizAnswers = mapOf(
                LeagueQuizQuestionKey.Scenario to listOf("Laboratorio"),
                LeagueQuizQuestionKey.Audience to listOf("Adultos"),
                LeagueQuizQuestionKey.System to listOf("Ossos"),
                LeagueQuizQuestionKey.Style to listOf("Raciocinio"),
                LeagueQuizQuestionKey.Impact to listOf("Curiosidade"),
            ),
        ),
        LeagueQuizProfile(
            nome = "Liga Academica de Clinica Medica",
            sigla = "LACM",
            aliases = listOf("clinica medica"),
            keywords = listOf("clinica", "raciocinio", "diagnostico", "consultorio", "enfermaria"),
            quizAnswers = mapOf(
                LeagueQuizQuestionKey.Scenario to listOf("Consultorio"),
                LeagueQuizQuestionKey.Audience to listOf("Adultos"),
                LeagueQuizQuestionKey.System to emptyList(),
                LeagueQuizQuestionKey.Style to listOf("Raciocinio"),
                LeagueQuizQuestionKey.Impact to listOf("Paciencia"),
            ),
        ),
        LeagueQuizProfile(
            nome = "Liga Academica de Psiquiatria",
            sigla = "LIAPS",
            aliases = listOf("psiquiatria", "saude mental"),
            keywords = listOf("psiquiatria", "saude mental", "escuta", "cerebro", "acolhimento"),
            quizAnswers = mapOf(
                LeagueQuizQuestionKey.Scenario to listOf("Consultorio", "Comunidade"),
                LeagueQuizQuestionKey.Audience to listOf("Adultos"),
                LeagueQuizQuestionKey.System to listOf("Cerebro"),
                LeagueQuizQuestionKey.Style to listOf("Raciocinio"),
                LeagueQuizQuestionKey.Impact to listOf("Paciencia", "Vinculo"),
            ),
        ),
        LeagueQuizProfile(
            nome = "Liga Academica de Ortopedia e Medicina Esportiva",
            sigla = "LAOME",
            aliases = listOf("ortopedia", "medicina esportiva"),
            keywords = listOf("ortopedia", "atletas", "esportiva", "ossos", "trauma"),
            quizAnswers = mapOf(
                LeagueQuizQuestionKey.Scenario to listOf("Centro Cirurgico"),
                LeagueQuizQuestionKey.Audience to listOf("Atletas", "Adultos"),
                LeagueQuizQuestionKey.System to listOf("Ossos"),
                LeagueQuizQuestionKey.Style to listOf("Manual"),
                LeagueQuizQuestionKey.Impact to listOf("Salvar vidas", "Detalhe"),
            ),
        ),
        LeagueQuizProfile(
            nome = "Liga Academica de Oncologia",
            sigla = "LAONC",
            aliases = listOf("oncologia"),
            keywords = listOf("oncologia", "cancer", "cuidado", "adultos", "seguimento"),
            quizAnswers = mapOf(
                LeagueQuizQuestionKey.Scenario to listOf("Consultorio"),
                LeagueQuizQuestionKey.Audience to listOf("Adultos"),
                LeagueQuizQuestionKey.System to emptyList(),
                LeagueQuizQuestionKey.Style to listOf("Raciocinio"),
                LeagueQuizQuestionKey.Impact to listOf("Paciencia", "Vinculo"),
            ),
        ),
        LeagueQuizProfile(
            nome = "Liga Academica de Humanidades e Saude",
            sigla = "LAHS",
            aliases = listOf("humanidades", "saude coletiva"),
            keywords = listOf("humanidades", "comunidade", "escuta", "prevencao", "social"),
            quizAnswers = mapOf(
                LeagueQuizQuestionKey.Scenario to listOf("Comunidade"),
                LeagueQuizQuestionKey.Audience to listOf("Adultos"),
                LeagueQuizQuestionKey.System to emptyList(),
                LeagueQuizQuestionKey.Style to listOf("Prevencao", "Gestao"),
                LeagueQuizQuestionKey.Impact to listOf("Vinculo"),
            ),
        ),
        LeagueQuizProfile(
            nome = "Liga Academica de Dermatologia",
            sigla = "LADERM",
            aliases = listOf("dermatologia", "pele"),
            keywords = listOf("dermatologia", "pele", "consultorio", "detalhe", "lesao"),
            quizAnswers = mapOf(
                LeagueQuizQuestionKey.Scenario to listOf("Consultorio"),
                LeagueQuizQuestionKey.Audience to listOf("Adultos"),
                LeagueQuizQuestionKey.System to emptyList(),
                LeagueQuizQuestionKey.Style to listOf("Raciocinio"),
                LeagueQuizQuestionKey.Impact to listOf("Detalhe"),
            ),
        ),
        LeagueQuizProfile(
            nome = "Liga Academica de Neonatologia e Pediatria",
            sigla = "LANPED",
            aliases = listOf("neonatologia", "pediatria"),
            keywords = listOf("neonatologia", "pediatria", "criancas", "familia", "acolhimento"),
            quizAnswers = mapOf(
                LeagueQuizQuestionKey.Scenario to listOf("Comunidade", "Consultorio"),
                LeagueQuizQuestionKey.Audience to listOf("Criancas"),
                LeagueQuizQuestionKey.System to emptyList(),
                LeagueQuizQuestionKey.Style to listOf("Prevencao", "Raciocinio"),
                LeagueQuizQuestionKey.Impact to listOf("Vinculo", "Salvar vidas"),
            ),
        ),
        LeagueQuizProfile(
            nome = "Liga Academica de Urologia",
            sigla = "LIU",
            aliases = listOf("urologia"),
            keywords = listOf("urologia", "nefro", "rins", "consultorio", "cirurgia"),
            quizAnswers = mapOf(
                LeagueQuizQuestionKey.Scenario to listOf("Consultorio"),
                LeagueQuizQuestionKey.Audience to listOf("Adultos"),
                LeagueQuizQuestionKey.System to listOf("Rins"),
                LeagueQuizQuestionKey.Style to listOf("Raciocinio", "Manual"),
                LeagueQuizQuestionKey.Impact to listOf("Detalhe"),
            ),
        ),
        LeagueQuizProfile(
            nome = "Liga de Neurologia e Neurocirurgia",
            sigla = "LANN",
            aliases = listOf("neurologia", "neurocirurgia"),
            keywords = listOf("neurologia", "neurocirurgia", "cerebro", "neuro", "cirurgia"),
            quizAnswers = mapOf(
                LeagueQuizQuestionKey.Scenario to listOf("Centro Cirurgico", "Consultorio"),
                LeagueQuizQuestionKey.Audience to listOf("Adultos", "Graves"),
                LeagueQuizQuestionKey.System to listOf("Cerebro"),
                LeagueQuizQuestionKey.Style to listOf("Raciocinio", "Manual"),
                LeagueQuizQuestionKey.Impact to listOf("Detalhe", "Salvar vidas"),
            ),
        ),
        LeagueQuizProfile(
            nome = "Liga Academica de Oftalmologia",
            sigla = "LAOFT",
            aliases = listOf("oftalmologia", "oftalmo"),
            keywords = listOf("oftalmologia", "oftalmo", "visao", "detalhe", "cirurgia"),
            quizAnswers = mapOf(
                LeagueQuizQuestionKey.Scenario to listOf("Consultorio", "Centro Cirurgico"),
                LeagueQuizQuestionKey.Audience to listOf("Adultos"),
                LeagueQuizQuestionKey.System to emptyList(),
                LeagueQuizQuestionKey.Style to listOf("Raciocinio", "Manual"),
                LeagueQuizQuestionKey.Impact to listOf("Detalhe"),
            ),
        ),
        LeagueQuizProfile(
            nome = "Liga Academica de Cardiologia",
            sigla = "LACARDIO",
            aliases = listOf("cardiologia"),
            keywords = listOf("cardiologia", "cardio", "coracao", "ecg", "hemodinamica"),
            quizAnswers = mapOf(
                LeagueQuizQuestionKey.Scenario to listOf("Consultorio"),
                LeagueQuizQuestionKey.Audience to listOf("Adultos"),
                LeagueQuizQuestionKey.System to listOf("Coracao"),
                LeagueQuizQuestionKey.Style to listOf("Raciocinio", "Tecnologia"),
                LeagueQuizQuestionKey.Impact to listOf("Salvar vidas"),
            ),
        ),
        LeagueQuizProfile(
            nome = "Liga da Saude da Familia",
            aliases = listOf("saude da familia", "medicina de familia", "familia"),
            keywords = listOf("familia", "comunidade", "prevencao", "territorio", "vinculo"),
            quizAnswers = mapOf(
                LeagueQuizQuestionKey.Scenario to listOf("Comunidade"),
                LeagueQuizQuestionKey.Audience to listOf("Criancas", "Adultos", "Mulheres"),
                LeagueQuizQuestionKey.System to emptyList(),
                LeagueQuizQuestionKey.Style to listOf("Prevencao"),
                LeagueQuizQuestionKey.Impact to listOf("Vinculo"),
            ),
        ),
        LeagueQuizProfile(
            nome = "Liga Academica de Otorrinolaringologia",
            sigla = "LAORL",
            aliases = listOf("otorrinolaringologia", "otorrino"),
            keywords = listOf("otorrino", "vias aereas", "ouvido", "garganta", "cirurgia"),
            quizAnswers = mapOf(
                LeagueQuizQuestionKey.Scenario to listOf("Consultorio", "Centro Cirurgico"),
                LeagueQuizQuestionKey.Audience to listOf("Adultos"),
                LeagueQuizQuestionKey.System to emptyList(),
                LeagueQuizQuestionKey.Style to listOf("Raciocinio", "Manual"),
                LeagueQuizQuestionKey.Impact to listOf("Detalhe"),
            ),
        ),
        LeagueQuizProfile(
            nome = "Liga Academica de Gastroenterologia e Cirurgia Digestiva",
            sigla = "LAGAC",
            aliases = listOf("gastroenterologia", "cirurgia digestiva", "gastro"),
            keywords = listOf("gastro", "digestiva", "aparelho digestivo", "endoscopia", "cirurgia"),
            quizAnswers = mapOf(
                LeagueQuizQuestionKey.Scenario to listOf("Consultorio", "Centro Cirurgico"),
                LeagueQuizQuestionKey.Audience to listOf("Adultos"),
                LeagueQuizQuestionKey.System to listOf("Hormonios"),
                LeagueQuizQuestionKey.Style to listOf("Raciocinio", "Manual"),
                LeagueQuizQuestionKey.Impact to listOf("Detalhe"),
            ),
        ),
        LeagueQuizProfile(
            nome = "Liga Academica de Cardiologia e Cirurgia Cardiovascular",
            sigla = "LAC",
            aliases = listOf("cirurgia cardiovascular", "cardiovascular"),
            keywords = listOf("cardiovascular", "coracao", "cirurgia", "tecnologia", "manual"),
            quizAnswers = mapOf(
                LeagueQuizQuestionKey.Scenario to listOf("Centro Cirurgico"),
                LeagueQuizQuestionKey.Audience to listOf("Adultos", "Graves"),
                LeagueQuizQuestionKey.System to listOf("Coracao"),
                LeagueQuizQuestionKey.Style to listOf("Manual", "Tecnologia"),
                LeagueQuizQuestionKey.Impact to listOf("Salvar vidas", "Detalhe"),
            ),
        ),
        LeagueQuizProfile(
            nome = "Liga Academica de Medicina Militar",
            sigla = "LAMM",
            aliases = listOf("medicina militar"),
            keywords = listOf("militar", "urgencia", "estrategia", "trauma", "resgate"),
            quizAnswers = mapOf(
                LeagueQuizQuestionKey.Scenario to listOf("Emergencia"),
                LeagueQuizQuestionKey.Audience to listOf("Graves", "Adultos"),
                LeagueQuizQuestionKey.System to emptyList(),
                LeagueQuizQuestionKey.Style to listOf("Gestao", "Manual"),
                LeagueQuizQuestionKey.Impact to listOf("Salvar vidas"),
            ),
        ),
        LeagueQuizProfile(
            nome = "Liga de Simulacao Realistica",
            sigla = "LASIR",
            aliases = listOf("simulacao realistica", "simulacao"),
            keywords = listOf("simulacao", "realistica", "treinamento", "cenario", "procedimento"),
            quizAnswers = mapOf(
                LeagueQuizQuestionKey.Scenario to listOf("Laboratorio", "Emergencia"),
                LeagueQuizQuestionKey.Audience to listOf("Graves", "Adultos"),
                LeagueQuizQuestionKey.System to emptyList(),
                LeagueQuizQuestionKey.Style to listOf("Tecnologia", "Manual"),
                LeagueQuizQuestionKey.Impact to listOf("Curiosidade", "Salvar vidas"),
            ),
        ),
        LeagueQuizProfile(
            nome = "Liga Academica de Laparoscopia e Robotica",
            sigla = "LALR",
            aliases = listOf("laparoscopia", "robotica"),
            keywords = listOf("laparoscopia", "robotica", "cirurgia", "tecnologia", "minimamente invasiva"),
            quizAnswers = mapOf(
                LeagueQuizQuestionKey.Scenario to listOf("Centro Cirurgico"),
                LeagueQuizQuestionKey.Audience to listOf("Adultos"),
                LeagueQuizQuestionKey.System to emptyList(),
                LeagueQuizQuestionKey.Style to listOf("Manual", "Tecnologia"),
                LeagueQuizQuestionKey.Impact to listOf("Detalhe"),
            ),
        ),
    )
}
