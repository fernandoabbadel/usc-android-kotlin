package com.example.usc1.domain.model

/**
 * Formulario de cadastro/edicao de perfil.
 * Espelha `UserFormData` de `web-reference/src/app/cadastro/page.tsx`.
 */
data class CadastroForm(
    val nome: String = "",
    val apelido: String = "",
    val matricula: String = "",
    val turma: String = "",
    val instagram: String = "",
    val instagramPublico: Boolean = false,
    val telefone: String = "",
    val whatsappPublico: Boolean = false,
    val bio: String = "",
    val dataNascimento: String = "",
    val idadePublica: Boolean = true,
    val cidadeOrigem: String = "",
    val estadoOrigem: String = "",
    val statusRelacionamento: String = "Solteiro(a)",
    val relacionamentoPublico: Boolean = false,
    val signo: String = "",
    val signoPublico: Boolean = false,
    val ascendente: String = "",
    val ascendentePublico: Boolean = false,
    val lugarEspecial: List<String> = emptyList(),
    val comidaPreferida: List<String> = emptyList(),
    val musicaPreferida: List<String> = emptyList(),
    val corPreferida: String = "",
    val esportes: List<String> = emptyList(),
    val pets: String = "nenhum",
    val foto: String = "",
)

/** Opcao simples com emoji, como `CadastroChoiceOption` do web. */
data class CadastroChoiceOption(
    val id: String,
    val label: String,
    val icon: String,
    val enabled: Boolean = true,
)

data class CadastroColorOption(
    val id: String,
    val label: String,
    val hex: String,
)

data class CadastroTurmaOption(
    val id: String,
    val label: String,
    val hidden: Boolean = false,
)

/**
 * Configuracao do cadastro vinda de `app_config` (id `cadastro_config`),
 * espelhando `cadastroConfigService.ts`.
 */
data class CadastroConfig(
    val turmas: List<CadastroTurmaOption> = emptyList(),
    val sportOptions: List<CadastroChoiceOption> = CadastroDefaults.sports,
    val specialPlaceOptions: List<CadastroChoiceOption> = CadastroDefaults.specialPlaces,
    val foodOptions: List<CadastroChoiceOption> = CadastroDefaults.foods,
    val musicOptions: List<CadastroChoiceOption> = CadastroDefaults.musics,
    val colorOptions: List<CadastroColorOption> = CadastroDefaults.colors,
    val petOptions: List<CadastroChoiceOption> = CadastroDefaults.pets,
    val relationshipOptions: List<String> = CadastroDefaults.relationshipStatuses,
)

/** Listas padrao de `web-reference/src/lib/cadastroOptions.ts`. */
object CadastroDefaults {
    val relationshipStatuses = listOf("Solteiro(a)", "Namorando", "Casado(a)", "Enrolado(a)")

    val pets = listOf(
        CadastroChoiceOption("cachorro", "Cachorro", "🐶"),
        CadastroChoiceOption("gato", "Gato", "🐱"),
        CadastroChoiceOption("ambos", "Ambos", "🐶🐱"),
        CadastroChoiceOption("nenhum", "Sem Pet", "🚫"),
    )

    val sports = listOf(
        CadastroChoiceOption("futebol", "Futebol", "⚽"),
        CadastroChoiceOption("futsal", "Futsal", "👟"),
        CadastroChoiceOption("volei", "Vôlei", "🏐"),
        CadastroChoiceOption("basquete", "Basquete", "🏀"),
        CadastroChoiceOption("handball", "Handball", "🤾"),
        CadastroChoiceOption("rugby", "Rugby", "🏉"),
        CadastroChoiceOption("baseball", "Baseball", "⚾"),
        CadastroChoiceOption("futevolei", "Futevôlei", "🏐"),
        CadastroChoiceOption("beach_tennis", "Beach Tennis", "🏖️"),
        CadastroChoiceOption("tenis", "Tênis", "🎾"),
        CadastroChoiceOption("frescobol", "Frescobol", "🏓"),
        CadastroChoiceOption("taco", "Taco (Bets)", "🏏"),
        CadastroChoiceOption("peteca", "Peteca", "🏸"),
        CadastroChoiceOption("surf", "Surf", "🏄"),
        CadastroChoiceOption("natacao", "Natação", "🏊"),
        CadastroChoiceOption("canoagem", "Canoagem", "🛶"),
        CadastroChoiceOption("skate", "Skate", "🛹"),
        CadastroChoiceOption("dog_walking", "Dog Walking", "🐕"),
        CadastroChoiceOption("truco", "Truco", "🃏"),
        CadastroChoiceOption("sinuca", "Sinuca", "🎱"),
    )

    val specialPlaces = listOf(
        CadastroChoiceOption("cafe", "Café", "☕"),
        CadastroChoiceOption("bar", "Bar", "🍻"),
        CadastroChoiceOption("biblioteca", "Biblioteca", "📚"),
        CadastroChoiceOption("praia", "Praia", "🏖️"),
        CadastroChoiceOption("cachoeira", "Cachoeira", "💦"),
        CadastroChoiceOption("cinema", "Cinema", "🎬"),
        CadastroChoiceOption("igreja", "Igreja", "⛪"),
        CadastroChoiceOption("academia", "Academia", "🏋️"),
        CadastroChoiceOption("trilha", "Trilha", "🥾"),
        CadastroChoiceOption("teatro", "Teatro", "🎭"),
        CadastroChoiceOption("karaoke", "Karaokê", "🎤"),
    )

    val foods = listOf(
        CadastroChoiceOption("japonesa", "Comida Japonesa", "🍣"),
        CadastroChoiceOption("mexicana", "Comida Mexicana", "🌮"),
        CadastroChoiceOption("tailandesa", "Comida Tailandesa", "🍜"),
        CadastroChoiceOption("brasileira", "Comida Brasileira", "🍛"),
        CadastroChoiceOption("italiana", "Comida Italiana", "🍝"),
        CadastroChoiceOption("arabe", "Comida Árabe", "🧆"),
    )

    val musics = listOf(
        CadastroChoiceOption("rock", "Rock", "🎸"),
        CadastroChoiceOption("funk", "Funk", "🔊"),
        CadastroChoiceOption("pop", "Pop", "🎧"),
        CadastroChoiceOption("pagode", "Pagode", "🪕"),
        CadastroChoiceOption("samba", "Samba", "🥁"),
        CadastroChoiceOption("jazz", "Jazz", "🎷"),
        CadastroChoiceOption("sertanejo", "Sertanejo", "🤠"),
        CadastroChoiceOption("rap", "Rap", "🎙️"),
        CadastroChoiceOption("axe", "Axé", "🌞"),
        CadastroChoiceOption("piseiro", "Piseiro", "🪗"),
        CadastroChoiceOption("eletronica", "Eletrônica", "🪩"),
        CadastroChoiceOption("kpop", "K-pop", "💿"),
        CadastroChoiceOption("reggae", "Reggae", "🌿"),
        CadastroChoiceOption("gospel", "Gospel", "✨"),
        CadastroChoiceOption("forro", "Forró", "🪗"),
        CadastroChoiceOption("classica", "Clássica", "🎻"),
    )

    val colors = listOf(
        CadastroColorOption("preto", "Preto", "#050505"),
        CadastroColorOption("branco", "Branco", "#F8FAFC"),
        CadastroColorOption("vermelho", "Vermelho", "#EF4444"),
        CadastroColorOption("laranja", "Laranja", "#F97316"),
        CadastroColorOption("amarelo", "Amarelo", "#FACC15"),
        CadastroColorOption("verde", "Verde", "#22C55E"),
        CadastroColorOption("azul", "Azul", "#3B82F6"),
        CadastroColorOption("roxo", "Roxo", "#8B5CF6"),
        CadastroColorOption("rosa", "Rosa", "#EC4899"),
        CadastroColorOption("cinza", "Cinza", "#71717A"),
    )

    /** Signos usados por `astroProfile.ts`. */
    val zodiacSigns = listOf(
        "Áries", "Touro", "Gêmeos", "Câncer", "Leão", "Virgem",
        "Libra", "Escorpião", "Sagitário", "Capricórnio", "Aquário", "Peixes",
    )
}
