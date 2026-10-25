package dev.sebas1705.careereditor.data.repository

import dev.sebas1705.careereditor.data.api.CareerApiClient
import dev.sebas1705.careereditor.data.api.DEFAULT_BASE_URL
import dev.sebas1705.careereditor.data.model.*
import dev.sebas1705.careereditor.security.SecureKeys
import dev.sebas1705.careereditor.security.embeddedApiToken
import dev.sebas1705.careereditor.security.SecureStorage

// BOM (U+FEFF) y zero-width space (U+200B) llegan al pegar desde algunos portapapeles,
// trim() no los considera whitespace y rompen el Authorization header.
internal fun String.trimBom(): String = trim('\uFEFF', '\u200B')

class CareerRepository(private val secureStorage: SecureStorage = SecureStorage()) {

    private fun buildClient(): CareerApiClient {
        val token = getToken()
        val baseUrl = secureStorage.get(SecureKeys.API_BASE_URL) ?: DEFAULT_BASE_URL
        return CareerApiClient(baseUrl = baseUrl, token = token)
    }

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
    suspend fun validateAuth(): Boolean = buildClient().validateAuth()

    fun saveToken(token: String) = secureStorage.put(SecureKeys.API_TOKEN, token.trim().trimBom())
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
