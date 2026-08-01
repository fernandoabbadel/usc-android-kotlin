package com.example.usc1.ui.cadastro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.session.UserSession
import com.example.usc1.data.repository.SupabaseCadastroRepository
import com.example.usc1.domain.model.CadastroConfig
import com.example.usc1.domain.model.CadastroForm
import com.example.usc1.domain.repository.CadastroRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CadastroUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedMessage: String? = null,
    val config: CadastroConfig = CadastroConfig(turmas = emptyList()),
    val form: CadastroForm = CadastroForm(),
    val isExistingProfile: Boolean = false,
) {
    val title: String get() = if (isExistingProfile) "Editar perfil" else "Completar cadastro"

    val canSave: Boolean
        get() = !isSaving && form.nome.trim().isNotBlank() && form.turma.trim().isNotBlank()
}

class CadastroViewModel(
    private val repository: CadastroRepository = SupabaseCadastroRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(CadastroUiState())
    val uiState: StateFlow<CadastroUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var currentSession: UserSession? = null
    private var lastLoadKey: String? = null

    fun load(session: UserSession, forceRefresh: Boolean = false) {
        currentSession = session
        val tenantId = session.tenant?.id.orEmpty().trim()
        val userId = session.user?.id.orEmpty().trim()
        val loadKey = "$tenantId::$userId"
        if (!forceRefresh && lastLoadKey == loadKey && !_uiState.value.isLoading) return
        lastLoadKey = loadKey

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, savedMessage = null) }
            runCatching { repository.loadCadastro(tenantId = tenantId, userId = userId) }
                .onSuccess { bundle ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            config = bundle.config,
                            // Pre-preenche nome com o da sessao quando o perfil ainda nao existe.
                            form = bundle.form.copy(
                                nome = bundle.form.nome.ifBlank { session.user?.name.orEmpty() },
                                foto = bundle.form.foto.ifBlank { session.user?.avatarUrl.orEmpty() },
                                turma = bundle.form.turma.ifBlank { session.user?.classCode.orEmpty() },
                                matricula = bundle.form.matricula.ifBlank {
                                    session.user?.registrationNumber.orEmpty()
                                },
                            ),
                            isExistingProfile = bundle.isExistingProfile,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Erro ao carregar o cadastro.",
                        )
                    }
                }
        }
    }

    fun updateForm(transform: (CadastroForm) -> CadastroForm) {
        _uiState.update { it.copy(form = transform(it.form), savedMessage = null, errorMessage = null) }
    }

    fun toggleChoice(current: List<String>, id: String, apply: (List<String>) -> Unit) {
        val next = if (current.any { it.equals(id, ignoreCase = true) }) {
            current.filterNot { it.equals(id, ignoreCase = true) }
        } else {
            current + id
        }
        apply(next)
    }

    fun save(onSaved: () -> Unit = {}) {
        val session = currentSession ?: return
        val state = _uiState.value
        if (!state.canSave) {
            _uiState.update {
                it.copy(errorMessage = "Preencha nome e turma para salvar.")
            }
            return
        }
        val tenantId = session.tenant?.id.orEmpty().trim()
        val userId = session.user?.id.orEmpty().trim()

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, savedMessage = null) }
            runCatching {
                repository.saveCadastro(tenantId = tenantId, userId = userId, form = state.form)
            }.onSuccess {
                _uiState.update {
                    it.copy(isSaving = false, savedMessage = "Cadastro salvo com sucesso.")
                }
                onSaved()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Não foi possível salvar o cadastro.",
                    )
                }
            }
        }
    }

    fun consumeSavedMessage() {
        _uiState.update { it.copy(savedMessage = null) }
    }
}
