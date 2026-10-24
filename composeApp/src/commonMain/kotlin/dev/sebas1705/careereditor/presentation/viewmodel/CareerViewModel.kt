package dev.sebas1705.careereditor.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sebas1705.careereditor.data.model.*
import dev.sebas1705.careereditor.data.repository.CareerRepository
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import kotlinx.coroutines.CancellationException
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
    val loadFailed: Boolean = false,
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
                // One request: it used to be eight, one after the other
                val all = repository.getAll()
                _selectedLangCode.value = all.languages.default.ifBlank { "en" }
                _uiState.value = CareerUiState(
                    languages      = all.languages,
                    personal       = all.personal,
                    jobs           = all.jobs,
                    projects       = all.projects,
                    skills         = all.skills,
                    education      = all.education,
                    certifications = all.certifications,
                    softSkills     = all.softSkills
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, loadFailed = true, error = friendlyError(e, "cargar los datos"))
            }
        }
    }

    private fun <T> saveItem(
        request: suspend () -> T,
        onSuccess: (T) -> CareerUiState,
        onDone: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, saveSuccess = false)
            try {
                val result = request()
                _uiState.value = onSuccess(result).copy(saveSuccess = true)
                onDone(true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = friendlyError(e, "guardar"))
                onDone(false)
            }
        }
    }

    /** What the user needs to know, in Spanish, instead of the raw exception text (often English). */
    private fun friendlyError(e: Exception, action: String): String = when (e) {
        is ClientRequestException ->
            if (e.response.status == HttpStatusCode.Unauthorized) "El token no es válido o ha caducado: revísalo en Ajustes"
            else "La API rechazó la petición (${e.response.status.value}) al $action"
        is ServerResponseException -> "La API falló (${e.response.status.value}) al $action. Inténtalo de nuevo."
        else -> "No se pudo conectar con la API. Revisa la conexión e inténtalo de nuevo."
    }

    // ── Singular saves ────────────────────────────────────────────────────────

    fun saveLanguages(languages: Languages, onDone: (Boolean) -> Unit = {}) = saveItem(
        request = { repository.updateLanguages(languages) },
        onSuccess = { updated -> _uiState.value.copy(isLoading = false, languages = updated) },
        onDone = onDone
    )

    fun savePersonal(personal: Personal, onDone: (Boolean) -> Unit = {}) = saveItem(
        request = { repository.updatePersonal(personal) },
        onSuccess = { _uiState.value.copy(isLoading = false, personal = it) },
        onDone = onDone
    )

    // ── Updates (PATCH) ───────────────────────────────────────────────────────

    fun saveJob(job: Job, onDone: (Boolean) -> Unit = {}) = saveItem(
        request = { repository.updateJob(job) },
        onSuccess = { updated ->
            _uiState.value.copy(isLoading = false, jobs = _uiState.value.jobs.map { if (it.id == job.id) updated else it })
        },
        onDone = onDone
    )

    fun saveProject(project: Project, onDone: (Boolean) -> Unit = {}) = saveItem(
        request = { repository.updateProject(project) },
        onSuccess = { updated ->
            _uiState.value.copy(isLoading = false, projects = _uiState.value.projects.map { if (it.id == project.id) updated else it })
        },
        onDone = onDone
    )

    fun saveSkill(skill: Skill, onDone: (Boolean) -> Unit = {}) = saveItem(
        request = { repository.updateSkill(skill) },
        onSuccess = { updated ->
            _uiState.value.copy(isLoading = false, skills = _uiState.value.skills.map { if (it.id == skill.id) updated else it })
        },
        onDone = onDone
    )

    fun saveEducation(education: Education, onDone: (Boolean) -> Unit = {}) = saveItem(
        request = { repository.updateEducation(education) },
        onSuccess = { updated ->
            _uiState.value.copy(isLoading = false, education = _uiState.value.education.map { if (it.id == education.id) updated else it })
        },
        onDone = onDone
    )

    fun saveCertification(cert: Certification, onDone: (Boolean) -> Unit = {}) = saveItem(
        request = { repository.updateCertification(cert) },
        onSuccess = { updated ->
            _uiState.value.copy(isLoading = false, certifications = _uiState.value.certifications.map { if (it.id == cert.id) updated else it })
        },
        onDone = onDone
    )

    fun saveSoftSkill(skill: SoftSkill, onDone: (Boolean) -> Unit = {}) = saveItem(
        request = { repository.updateSoftSkill(skill) },
        onSuccess = { updated ->
            _uiState.value.copy(isLoading = false, softSkills = _uiState.value.softSkills.map { if (it.id == skill.id) updated else it })
        },
        onDone = onDone
    )

    // ── Creates (POST) ────────────────────────────────────────────────────────

    fun createJob(job: Job, onDone: (Boolean) -> Unit = {}) = saveItem(
        request = { repository.createJob(job) },
        onSuccess = { created -> _uiState.value.copy(isLoading = false, jobs = _uiState.value.jobs + created) },
        onDone = onDone
    )

    fun createProject(project: Project, onDone: (Boolean) -> Unit = {}) = saveItem(
        request = { repository.createProject(project) },
        onSuccess = { created -> _uiState.value.copy(isLoading = false, projects = _uiState.value.projects + created) },
        onDone = onDone
    )

    fun createSkill(skill: Skill, onDone: (Boolean) -> Unit = {}) = saveItem(
        request = { repository.createSkill(skill) },
        onSuccess = { created -> _uiState.value.copy(isLoading = false, skills = _uiState.value.skills + created) },
        onDone = onDone
    )

    fun createEducation(education: Education, onDone: (Boolean) -> Unit = {}) = saveItem(
        request = { repository.createEducation(education) },
        onSuccess = { created -> _uiState.value.copy(isLoading = false, education = _uiState.value.education + created) },
        onDone = onDone
    )

    fun createCertification(cert: Certification, onDone: (Boolean) -> Unit = {}) = saveItem(
        request = { repository.createCertification(cert) },
        onSuccess = { created -> _uiState.value.copy(isLoading = false, certifications = _uiState.value.certifications + created) },
        onDone = onDone
    )

    fun createSoftSkill(skill: SoftSkill, onDone: (Boolean) -> Unit = {}) = saveItem(
        request = { repository.createSoftSkill(skill) },
        onSuccess = { created -> _uiState.value.copy(isLoading = false, softSkills = _uiState.value.softSkills + created) },
        onDone = onDone
    )

    // ── Deletes ───────────────────────────────────────────────────────────────

    fun deleteJob(id: String, onDone: (Boolean) -> Unit = {}) = saveItem(
        request = { repository.deleteJob(id) },
        onSuccess = { _uiState.value.copy(isLoading = false, jobs = _uiState.value.jobs.filter { it.id != id }) },
        onDone = onDone
    )

    fun deleteProject(id: String, onDone: (Boolean) -> Unit = {}) = saveItem(
        request = { repository.deleteProject(id) },
        onSuccess = { _uiState.value.copy(isLoading = false, projects = _uiState.value.projects.filter { it.id != id }) },
        onDone = onDone
    )

    fun deleteSkill(id: String, onDone: (Boolean) -> Unit = {}) = saveItem(
        request = { repository.deleteSkill(id) },
        onSuccess = { _uiState.value.copy(isLoading = false, skills = _uiState.value.skills.filter { it.id != id }) },
        onDone = onDone
    )

    fun deleteEducation(id: String, onDone: (Boolean) -> Unit = {}) = saveItem(
        request = { repository.deleteEducation(id) },
        onSuccess = { _uiState.value.copy(isLoading = false, education = _uiState.value.education.filter { it.id != id }) },
        onDone = onDone
    )

    fun deleteCertification(id: String, onDone: (Boolean) -> Unit = {}) = saveItem(
        request = { repository.deleteCertification(id) },
        onSuccess = { _uiState.value.copy(isLoading = false, certifications = _uiState.value.certifications.filter { it.id != id }) },
        onDone = onDone
    )

    fun deleteSoftSkill(id: String, onDone: (Boolean) -> Unit = {}) = saveItem(
        request = { repository.deleteSoftSkill(id) },
        onSuccess = { _uiState.value.copy(isLoading = false, softSkills = _uiState.value.softSkills.filter { it.id != id }) },
        onDone = onDone
    )

    // ── Utilities ─────────────────────────────────────────────────────────────

    fun clearSaveSuccess() { _uiState.value = _uiState.value.copy(saveSuccess = false) }
    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }

    // ── Settings ──────────────────────────────────────────────────────────────

    private fun refreshSettings() {
        val token = repository.getToken()
        _settingsState.value = SettingsUiState(
            baseUrl = repository.getBaseUrl(),
            tokenMasked = if (token.isNullOrBlank()) "" else "••••••••  (${token.length} caracteres)",
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
