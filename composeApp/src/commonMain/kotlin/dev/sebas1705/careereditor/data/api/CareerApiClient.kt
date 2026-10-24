package dev.sebas1705.careereditor.data.api

import dev.sebas1705.careereditor.data.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

const val DEFAULT_BASE_URL = "https://api.sebas1705.dev"

class CareerApiClient(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val token: String? = null
) {
    // coerceInputValues: un null explicito en un campo no-nullable cae al valor
    // por defecto en vez de tirar la deserializacion entera (y con ella loadAll).
    // encodeDefaults: updates are PATCH (merge). Without it an emptied field equals its default
    // and is left out of the body, so clearing e.g. the GitHub URL would silently not persist.
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true; encodeDefaults = true }

    val httpClient = HttpClient {
        // Sin esto, un 401/4xx no lanza excepción y el JSON de error se
        // deserializa en un modelo vacío: la app cree que guardó y no guardó.
        expectSuccess = true
        install(ContentNegotiation) { json(json) }
        install(Logging) { level = LogLevel.INFO }
        if (!token.isNullOrBlank()) {
            install(Auth) {
                bearer {
                    loadTokens { BearerTokens(token, "") }
                    sendWithoutRequest { true }
                }
            }
        }
    }

    /** Every entity in one request (GET /all). */
    suspend fun getAll(): AllData = httpClient.get("$baseUrl/all").body()
    suspend fun getLanguages(): Languages = httpClient.get("$baseUrl/languages").body()
    suspend fun getPersonal(): Personal = httpClient.get("$baseUrl/personal").body()
    suspend fun getJobs(): List<Job> = httpClient.get("$baseUrl/jobs").body()
    suspend fun getProjects(): List<Project> = httpClient.get("$baseUrl/projects").body()
    suspend fun getSkills(): List<Skill> = httpClient.get("$baseUrl/skills").body()
    suspend fun getEducation(): List<Education> = httpClient.get("$baseUrl/education").body()
    suspend fun getCertifications(): List<Certification> = httpClient.get("$baseUrl/certifications").body()
    suspend fun getSoftSkills(): List<SoftSkill> = httpClient.get("$baseUrl/soft-skills").body()

    suspend fun validateConnection(): Boolean = runCatching {
        httpClient.get(baseUrl).status.isSuccess()
    }.getOrDefault(false)

    /**
     * Valida que el token autoriza escrituras: PATCH /languages con cuerpo vacío
     * es un merge sin cambios (no-op) que devuelve 200 con auth valida y 401 sin ella.
     */
    suspend fun validateAuth(): Boolean = runCatching {
        httpClient.patch("$baseUrl/languages") {
            contentType(ContentType.Application.Json); setBody("{}")
        }.status.isSuccess()
    }.getOrDefault(false)

    // ── Singular updates ──────────────────────────────────────────────────────

    suspend fun updateLanguages(languages: Languages): Languages =
        httpClient.put("$baseUrl/languages") {
            contentType(ContentType.Application.Json); setBody(languages)
        }.body()

    suspend fun updatePersonal(personal: Personal): Personal =
        httpClient.put("$baseUrl/personal") {
            contentType(ContentType.Application.Json); setBody(personal)
        }.body()

    // ── PATCH (update existing) ───────────────────────────────────────────────

    suspend fun updateJob(job: Job): Job =
        httpClient.patch("$baseUrl/jobs/${job.id}") {
            contentType(ContentType.Application.Json); setBody(job)
        }.body()

    suspend fun updateProject(project: Project): Project =
        httpClient.patch("$baseUrl/projects/${project.id}") {
            contentType(ContentType.Application.Json); setBody(project)
        }.body()

    suspend fun updateSkill(skill: Skill): Skill =
        httpClient.patch("$baseUrl/skills/${skill.id}") {
            contentType(ContentType.Application.Json); setBody(skill)
        }.body()

    suspend fun updateEducation(education: Education): Education =
        httpClient.patch("$baseUrl/education/${education.id}") {
            contentType(ContentType.Application.Json); setBody(education)
        }.body()

    suspend fun updateCertification(cert: Certification): Certification =
        httpClient.patch("$baseUrl/certifications/${cert.id}") {
            contentType(ContentType.Application.Json); setBody(cert)
        }.body()

    suspend fun updateSoftSkill(skill: SoftSkill): SoftSkill =
        httpClient.patch("$baseUrl/soft-skills/${skill.id}") {
            contentType(ContentType.Application.Json); setBody(skill)
        }.body()

    // ── POST (create new) ─────────────────────────────────────────────────────

    suspend fun createJob(job: Job): Job =
        httpClient.post("$baseUrl/jobs") {
            contentType(ContentType.Application.Json); setBody(job)
        }.body()

    suspend fun createProject(project: Project): Project =
        httpClient.post("$baseUrl/projects") {
            contentType(ContentType.Application.Json); setBody(project)
        }.body()

    suspend fun createSkill(skill: Skill): Skill =
        httpClient.post("$baseUrl/skills") {
            contentType(ContentType.Application.Json); setBody(skill)
        }.body()

    suspend fun createEducation(education: Education): Education =
        httpClient.post("$baseUrl/education") {
            contentType(ContentType.Application.Json); setBody(education)
        }.body()

    suspend fun createCertification(cert: Certification): Certification =
        httpClient.post("$baseUrl/certifications") {
            contentType(ContentType.Application.Json); setBody(cert)
        }.body()

    suspend fun createSoftSkill(skill: SoftSkill): SoftSkill =
        httpClient.post("$baseUrl/soft-skills") {
            contentType(ContentType.Application.Json); setBody(skill)
        }.body()

    // ── DELETE ────────────────────────────────────────────────────────────────

    suspend fun deleteJob(id: String): Job =
        httpClient.delete("$baseUrl/jobs/$id").body()

    suspend fun deleteProject(id: String): Project =
        httpClient.delete("$baseUrl/projects/$id").body()

    suspend fun deleteSkill(id: String): Skill =
        httpClient.delete("$baseUrl/skills/$id").body()

    suspend fun deleteEducation(id: String): Education =
        httpClient.delete("$baseUrl/education/$id").body()

    suspend fun deleteCertification(id: String): Certification =
        httpClient.delete("$baseUrl/certifications/$id").body()

    suspend fun deleteSoftSkill(id: String): SoftSkill =
        httpClient.delete("$baseUrl/soft-skills/$id").body()
}
