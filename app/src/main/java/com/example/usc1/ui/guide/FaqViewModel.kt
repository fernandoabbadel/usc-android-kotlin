package com.example.usc1.ui.guide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.session.UserSession
import com.example.usc1.data.repository.SupabasePlatformFaqRepository
import com.example.usc1.data.repository.SupabaseSettingsRepository
import com.example.usc1.domain.model.PlatformFaqCatalog
import com.example.usc1.domain.model.PlatformFaqConfig
import com.example.usc1.domain.model.PlatformFaqSection
import com.example.usc1.domain.model.SettingsSupportCategory
import com.example.usc1.domain.model.buildFaqDoubtMessage
import com.example.usc1.domain.model.buildFaqDoubtSubject
import com.example.usc1.domain.model.filterByQuery
import com.example.usc1.domain.repository.PlatformFaqRepository
import com.example.usc1.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FaqUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val config: PlatformFaqConfig = PlatformFaqConfig(),
    val query: String = "",
    val activeSectionId: String = "",
    val openQuestionId: String = "",
    val doubtQuestionId: String = "",
    val doubtText: String = "",
    val sendingDoubt: Boolean = false,
    val message: String? = null,
) {
    private val filtered: List<PlatformFaqSection>
        get() = config.filterByQuery(query)

    /** `contentSections`: busca ativa mostra tudo que casou; sem busca, só a seção aberta. */
    val visibleSections: List<PlatformFaqSection>
        get() = if (query.isBlank()) {
            config.sections.filter { it.id == activeSectionId }
        } else {
            filtered
        }

    val matchCount: Int
        get() = filtered.sumOf { it.questions.size }
}

/** `/faq`. */
class FaqViewModel(
    private val repository: PlatformFaqRepository = SupabasePlatformFaqRepository(),
    private val settingsRepository: SettingsRepository = SupabaseSettingsRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(FaqUiState())
    val uiState: StateFlow<FaqUiState> = _uiState.asStateFlow()

    private var loaded = false

    fun load(forceRefresh: Boolean = false) {
        if (loaded && !forceRefresh) return
        loaded = true

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { repository.getFaqConfig() }
                .onSuccess { config ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            config = config,
                            activeSectionId = config.sections.firstOrNull()?.id.orEmpty(),
                        )
                    }
                }
                .onFailure { error ->
                    loaded = false
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message.orEmpty().ifBlank {
                                "Não foi possível carregar o FAQ agora."
                            },
                        )
                    }
                }
        }
    }

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value, openQuestionId = "") }
    }

    fun selectSection(sectionId: String) {
        _uiState.update {
            it.copy(activeSectionId = sectionId, openQuestionId = "", query = "")
        }
    }

    fun toggleQuestion(questionId: String) {
        _uiState.update {
            it.copy(openQuestionId = if (it.openQuestionId == questionId) "" else questionId)
        }
    }

    fun toggleDoubt(questionId: String) {
        _uiState.update {
            val next = if (it.doubtQuestionId == questionId) "" else questionId
            it.copy(doubtQuestionId = next, doubtText = if (next.isBlank()) "" else it.doubtText)
        }
    }

    fun onDoubtTextChange(value: String) {
        _uiState.update { it.copy(doubtText = value.take(PlatformFaqCatalog.DoubtMaxLength)) }
    }

    /** `handleQuestionDoubt`: abre chamado em `support_requests` com a origem no corpo. */
    fun sendDoubt(session: UserSession, section: PlatformFaqSection, questionId: String) {
        val user = session.user
        if (user == null || user.id.isBlank()) {
            _uiState.update { it.copy(message = "Entre com sua conta para enviar uma dúvida.") }
            return
        }
        val question = section.questions.firstOrNull { it.id == questionId } ?: return
        val doubt = _uiState.value.doubtText.trim()
        if (doubt.isBlank()) {
            _uiState.update { it.copy(message = "Escreva a sua dúvida antes de enviar.") }
            return
        }
        if (_uiState.value.sendingDoubt) return

        viewModelScope.launch {
            _uiState.update { it.copy(sendingDoubt = true, message = null) }
            runCatching {
                settingsRepository.submitSupportTicket(
                    // O FAQ é da plataforma: o web grava o chamado sem tenant_id.
                    tenantId = "",
                    userId = user.id,
                    userName = user.name,
                    userEmail = user.email,
                    category = SettingsSupportCategory.Geral,
                    subject = buildFaqDoubtSubject(section.title, question.question),
                    message = buildFaqDoubtMessage(section.title, question, doubt),
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        sendingDoubt = false,
                        doubtQuestionId = "",
                        doubtText = "",
                        message = "Dúvida enviada para o painel master.",
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        sendingDoubt = false,
                        message = error.message.orEmpty().ifBlank {
                            "Não foi possível enviar a dúvida."
                        },
                    )
                }
            }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
