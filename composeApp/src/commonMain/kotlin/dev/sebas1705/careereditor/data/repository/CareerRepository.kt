package dev.sebas1705.careereditor.data.repository

import dev.sebas1705.careereditor.data.api.CareerApiClient
import dev.sebas1705.careereditor.data.api.DEFAULT_BASE_URL
import dev.sebas1705.careereditor.data.model.*
import dev.sebas1705.careereditor.security.SecureKeys
import dev.sebas1705.careereditor.security.embeddedApiToken
import dev.sebas1705.careereditor.security.KeyValueStore
import dev.sebas1705.careereditor.security.SecureStorage
import io.ktor.client.engine.HttpClientEngine

// BOM (U+FEFF) y zero-width space (U+200B) llegan al pegar desde algunos portapapeles,
// trim() no los considera whitespace y rompen el Authorization header.
internal fun String.trimBom(): String = trim('\uFEFF', '\u200B')

/** What gets validated and stored: whitespace, BOM and zero-width spaces stripped in any order. */
internal fun String.cleanToken(): String = trim { it.isWhitespace() || it == '\uFEFF' || it == '\u200B' }

/** Outcome of [CareerRepository.login]; the view model maps each one to its message/state. */
enum class LoginResult { Success, Unreachable, Unauthorized }

class CareerRepository(
    private val secureStorage: KeyValueStore = SecureStorage(),
    // Tests pass a MockEngine; the app leaves it null and gets the platform engine.
    private val engine: HttpClientEngine? = null
) {

    private fun buildClient(token: String? = getToken()): CareerApiClient =
        CareerApiClient(baseUrl = getBaseUrl(), token = token, engine = engine)

    suspend fun getAll() = buildClient().getAll()
    suspend fun getLanguages() = buildClient().getLanguages()
    suspend fun getPersonal() = buildClient().getPersonal()
    suspend fun getJobs() = buildClient().getJobs()
    suspend fun getProjects() = buildClient().getProjects()
    suspend fun getSkills() = buildClient().getSkills()
    suspend fun getEducation() = buildClient().getEducation()
    suspend fun getCertifications() = buildClient().getCertifications()
    suspend fun getSoftSkills() = buildClient().getSoftSkills()

    suspend fun validateConnection(): Boolean = buildClient().validateConnection()

    /**
     * Validates [token] with a client that holds it only in memory and persists it only when the
     * API is reachable and accepts it. A rejected token never touches [secureStorage], and
     * whatever was stored before is left as it was.
     */
    suspend fun login(token: String): LoginResult {
        val candidate = token.cleanToken()
        val client = buildClient(token = candidate)
        return when {
            !client.validateConnection() -> LoginResult.Unreachable
            !client.validateAuth() -> LoginResult.Unauthorized
            else -> {
                saveToken(candidate)
                LoginResult.Success
            }
        }
    }

    fun saveToken(token: String) = secureStorage.put(SecureKeys.API_TOKEN, token.cleanToken())
    /** A token the user typed wins; otherwise the one compiled into the build, if any. */
    fun getToken(): String? =
        secureStorage.get(SecureKeys.API_TOKEN)?.takeIf { it.isNotBlank() }
            ?: embeddedApiToken.takeIf { it.isNotBlank() }

    /** True when this build carries its own token (Android release), so there is nothing to sign in with or out of. */
    val hasEmbeddedToken: Boolean get() = embeddedApiToken.isNotBlank()
    fun clearToken() = secureStorage.remove(SecureKeys.API_TOKEN)

    fun saveBaseUrl(url: String) = secureStorage.put(SecureKeys.API_BASE_URL, url.trim())
    fun getBaseUrl(): String = secureStorage.get(SecureKeys.API_BASE_URL) ?: DEFAULT_BASE_URL

    // ── Updates ───────────────────────────────────────────────────────────────
    suspend fun updateLanguages(languages: Languages) = buildClient().updateLanguages(languages)
    suspend fun updatePersonal(personal: Personal) = buildClient().updatePersonal(personal)
    suspend fun updateJob(job: Job) = buildClient().updateJob(job)
    suspend fun updateProject(project: Project) = buildClient().updateProject(project)
    suspend fun updateSkill(skill: Skill) = buildClient().updateSkill(skill)
    suspend fun updateEducation(education: Education) = buildClient().updateEducation(education)
    suspend fun updateCertification(cert: Certification) = buildClient().updateCertification(cert)
    suspend fun updateSoftSkill(skill: SoftSkill) = buildClient().updateSoftSkill(skill)

    // ── Creates ───────────────────────────────────────────────────────────────
    suspend fun createJob(job: Job) = buildClient().createJob(job)
    suspend fun createProject(project: Project) = buildClient().createProject(project)
    suspend fun createSkill(skill: Skill) = buildClient().createSkill(skill)
    suspend fun createEducation(education: Education) = buildClient().createEducation(education)
    suspend fun createCertification(cert: Certification) = buildClient().createCertification(cert)
    suspend fun createSoftSkill(skill: SoftSkill) = buildClient().createSoftSkill(skill)

    // ── Deletes ───────────────────────────────────────────────────────────────
    suspend fun deleteJob(id: String) = buildClient().deleteJob(id)
    suspend fun deleteProject(id: String) = buildClient().deleteProject(id)
    suspend fun deleteSkill(id: String) = buildClient().deleteSkill(id)
    suspend fun deleteEducation(id: String) = buildClient().deleteEducation(id)
    suspend fun deleteCertification(id: String) = buildClient().deleteCertification(id)
    suspend fun deleteSoftSkill(id: String) = buildClient().deleteSoftSkill(id)
}
