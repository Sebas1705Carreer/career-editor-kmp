package dev.sebas1705.careereditor.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sebas1705.careereditor.data.model.*
import dev.sebas1705.careereditor.data.repository.CareerRepository
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    data object Checking : AuthState()
    data object Unauthenticated : AuthState()
    data object Authenticated : AuthState()
}

data class CareerUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false,
    val languages: Languages = Languages(),
    val personal: Personal = Personal(),
    val jobs: List<Job> = emptyList(),
    val projects: List<Project> = emptyList(),
    val skills: List<Skill> = emptyList(),
    val education: List<Education> = emptyList(),
    val certifications: List<Certification> = emptyList(),
    val softSkills: List<SoftSkill> = emptyList()
)

data class SettingsUiState(
    val baseUrl: String = "",
    val tokenMasked: String = "",
    val tokenEmbedded: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null
)

class CareerViewModel(
    private val repository: CareerRepository = CareerRepository()
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Checking)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _uiState = MutableStateFlow(CareerUiState())
    val uiState: StateFlow<CareerUiState> = _uiState.asStateFlow()

    private val _settingsState = MutableStateFlow(SettingsUiState())
    val settingsState: StateFlow<SettingsUiState> = _settingsState.asStateFlow()

    private val _loginLoading = MutableStateFlow(false)
    val loginLoading: StateFlow<Boolean> = _loginLoading.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _selectedLangCode = MutableStateFlow("en")
    val selectedLangCode: StateFlow<String> = _selectedLangCode.asStateFlow()

    init {
        checkAuthAndLoad()
        refreshSettings()
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    private fun checkAuthAndLoad() {
        viewModelScope.launch {
            val token = repository.getToken()
            if (token.isNullOrBlank()) {
                _authState.value = AuthState.Unauthenticated
            } else {
                val ok = repository.validateConnection()
                if (ok) {
                    _authState.value = AuthState.Authenticated
                    loadAll()
                } else {
                    _authState.value = AuthState.Unauthenticated
                }
            }
        }
    }

    fun login(token: String) {
        if (token.isBlank()) { _loginError.value = "El token no puede estar vacío"; return }
        viewModelScope.launch {
            _loginLoading.value = true
            _loginError.value = null
            repository.saveToken(token)
            when {
                !repository.validateConnection() -> {
                    repository.clearToken()
                    _loginError.value = "API no accesible"
                }
                // Las lecturas son públicas: hay que validar el token contra una escritura real
                !repository.validateAuth() -> {
                    repository.clearToken()
                    _loginError.value = "Token inválido: la API rechaza las escrituras (401)"
                }
                else -> {
                    _authState.value = AuthState.Authenticated
                    loadAll()
                }
            }
            _loginLoading.value = false
        }
    }

    fun loginAnonymous() {
        repository.clearToken()
        _authState.value = AuthState.Authenticated
        loadAll()
    }

    fun logout() {
        repository.clearToken()
        _uiState.value = CareerUiState()
        _authState.value = AuthState.Unauthenticated
    }

    fun clearLoginError() { _loginError.value = null }

    // ── Language selection ────────────────────────────────────────────────────

    fun selectLanguage(code: String) { _selectedLangCode.value = code }

    // ── Data load ─────────────────────────────────────────────────────────────

    fun loadAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val languages = runCatching { repository.getLanguages() }.getOrDefault(Languages())
                _selectedLangCode.value = languages.default.ifBlank { "en" }
                _uiState.value = CareerUiState(
                    languages      = languages,
                    personal       = repository.getPersonal(),
                    jobs           = repository.getJobs(),
                    projects       = repository.getProjects(),
                    skills         = repository.getSkills(),
                    education      = repository.getEducation(),
                    certifications = repository.getCertifications(),
                    softSkills     = repository.getSoftSkills()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Error de red")
            }
        }
    }

    private fun <T> saveItem(
        request: suspend () -> T,
        onSuccess: (T) -> CareerUiState
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, saveSuccess = false)
            try {
                val result = request()
                _uiState.value = onSuccess(result).copy(saveSuccess = true)
            } catch (e: ClientRequestException) {
                val msg = if (e.response.status == HttpStatusCode.Unauthorized)
                    "Token inválido o caducado: vuelve a iniciar sesión en Ajustes"
                else "Error ${e.response.status.value} al guardar"
                _uiState.value = _uiState.value.copy(isLoading = false, error = msg)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Error al guardar")
            }
        }
    }

    // ── Singular saves ────────────────────────────────────────────────────────

    fun saveLanguages(languages: Languages) = saveItem(
        request = { repository.updateLanguages(languages) },
        onSuccess = { updated -> _uiState.value.copy(isLoading = false, languages = updated) }
    )

    fun savePersonal(personal: Personal) = saveItem(
        request = { repository.updatePersonal(personal) },
        onSuccess = { _uiState.value.copy(isLoading = false, personal = it) }
    )

    // ── Updates (PATCH) ───────────────────────────────────────────────────────

    fun saveJob(job: Job) = saveItem(
        request = { repository.updateJob(job) },
        onSuccess = { updated ->
            _uiState.value.copy(isLoading = false, jobs = _uiState.value.jobs.map { if (it.id == job.id) updated else it })
        }
    )

    fun saveProject(project: Project) = saveItem(
        request = { repository.updateProject(project) },
        onSuccess = { updated ->
            _uiState.value.copy(isLoading = false, projects = _uiState.value.projects.map { if (it.id == project.id) updated else it })
        }
    )

    fun saveSkill(skill: Skill) = saveItem(
        request = { repository.updateSkill(skill) },
        onSuccess = { updated ->
            _uiState.value.copy(isLoading = false, skills = _uiState.value.skills.map { if (it.id == skill.id) updated else it })
        }
    )

    fun saveEducation(education: Education) = saveItem(
        request = { repository.updateEducation(education) },
        onSuccess = { updated ->
            _uiState.value.copy(isLoading = false, education = _uiState.value.education.map { if (it.id == education.id) updated else it })
        }
    )

    fun saveCertification(cert: Certification) = saveItem(
        request = { repository.updateCertification(cert) },
        onSuccess = { updated ->
            _uiState.value.copy(isLoading = false, certifications = _uiState.value.certifications.map { if (it.id == cert.id) updated else it })
        }
    )

    fun saveSoftSkill(skill: SoftSkill) = saveItem(
        request = { repository.updateSoftSkill(skill) },
        onSuccess = { updated ->
            _uiState.value.copy(isLoading = false, softSkills = _uiState.value.softSkills.map { if (it.id == skill.id) updated else it })
        }
    )

    // ── Creates (POST) ────────────────────────────────────────────────────────

    fun createJob(job: Job) = saveItem(
        request = { repository.createJob(job) },
        onSuccess = { created -> _uiState.value.copy(isLoading = false, jobs = _uiState.value.jobs + created) }
    )

    fun createProject(project: Project) = saveItem(
        request = { repository.createProject(project) },
        onSuccess = { created -> _uiState.value.copy(isLoading = false, projects = _uiState.value.projects + created) }
    )

    fun createSkill(skill: Skill) = saveItem(
        request = { repository.createSkill(skill) },
        onSuccess = { created -> _uiState.value.copy(isLoading = false, skills = _uiState.value.skills + created) }
    )

    fun createEducation(education: Education) = saveItem(
        request = { repository.createEducation(education) },
        onSuccess = { created -> _uiState.value.copy(isLoading = false, education = _uiState.value.education + created) }
    )

    fun createCertification(cert: Certification) = saveItem(
        request = { repository.createCertification(cert) },
        onSuccess = { created -> _uiState.value.copy(isLoading = false, certifications = _uiState.value.certifications + created) }
    )

    fun createSoftSkill(skill: SoftSkill) = saveItem(
        request = { repository.createSoftSkill(skill) },
        onSuccess = { created -> _uiState.value.copy(isLoading = false, softSkills = _uiState.value.softSkills + created) }
    )

    // ── Deletes ───────────────────────────────────────────────────────────────

    fun deleteJob(id: String) = saveItem(
        request = { repository.deleteJob(id) },
        onSuccess = { _uiState.value.copy(isLoading = false, jobs = _uiState.value.jobs.filter { it.id != id }) }
    )

    fun deleteProject(id: String) = saveItem(
        request = { repository.deleteProject(id) },
        onSuccess = { _uiState.value.copy(isLoading = false, projects = _uiState.value.projects.filter { it.id != id }) }
    )

    fun deleteSkill(id: String) = saveItem(
        request = { repository.deleteSkill(id) },
        onSuccess = { _uiState.value.copy(isLoading = false, skills = _uiState.value.skills.filter { it.id != id }) }
    )

    fun deleteEducation(id: String) = saveItem(
        request = { repository.deleteEducation(id) },
        onSuccess = { _uiState.value.copy(isLoading = false, education = _uiState.value.education.filter { it.id != id }) }
    )

    fun deleteCertification(id: String) = saveItem(
        request = { repository.deleteCertification(id) },
        onSuccess = { _uiState.value.copy(isLoading = false, certifications = _uiState.value.certifications.filter { it.id != id }) }
    )

    fun deleteSoftSkill(id: String) = saveItem(
        request = { repository.deleteSoftSkill(id) },
        onSuccess = { _uiState.value.copy(isLoading = false, softSkills = _uiState.value.softSkills.filter { it.id != id }) }
    )

    // ── Utilities ─────────────────────────────────────────────────────────────

    fun clearSaveSuccess() { _uiState.value = _uiState.value.copy(saveSuccess = false) }
    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }

    // ── Settings ──────────────────────────────────────────────────────────────

    private fun refreshSettings() {
        val token = repository.getToken()
        _settingsState.value = SettingsUiState(
            baseUrl = repository.getBaseUrl(),
            tokenMasked = if (token.isNullOrBlank()) "" else token.take(6) + "••••••••",
            tokenEmbedded = repository.hasEmbeddedToken
        )
    }

    fun saveSettings(newUrl: String, newToken: String) {
        viewModelScope.launch {
            _settingsState.value = _settingsState.value.copy(isSaving = true, message = null)
            if (newUrl.isNotBlank()) repository.saveBaseUrl(newUrl)
            if (newToken.isNotBlank()) repository.saveToken(newToken)
            refreshSettings()
            _settingsState.value = _settingsState.value.copy(isSaving = false, message = "Ajustes guardados")
            loadAll()
        }
    }

    fun clearSettingsMessage() { _settingsState.value = _settingsState.value.copy(message = null) }
}
