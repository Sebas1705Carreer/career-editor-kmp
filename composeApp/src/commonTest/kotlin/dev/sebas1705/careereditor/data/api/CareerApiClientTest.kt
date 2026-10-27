package dev.sebas1705.careereditor.data.api

import dev.sebas1705.careereditor.ApiFixtures
import dev.sebas1705.careereditor.data.model.Job
import dev.sebas1705.careereditor.data.model.Languages
import dev.sebas1705.careereditor.data.model.Personal
import dev.sebas1705.careereditor.data.model.Project
import dev.sebas1705.careereditor.data.model.Skill
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.HttpRequestData
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The Ktor client against a MockEngine: nothing here ever reaches the real API. */
class CareerApiClientTest {

    private val base = "https://api.test.invalid"
    private val token = "test-token-not-real"
    private val requests = mutableListOf<HttpRequestData>()

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))

    private fun MockRequestHandleScope.error(status: HttpStatusCode, body: String) =
        respondError(status, body, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))

    private fun client(
        token: String? = this.token,
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> io.ktor.client.request.HttpResponseData
    ) = CareerApiClient(baseUrl = base, token = token, engine = MockEngine { request ->
        requests += request
        handler(request)
    })

    private suspend fun HttpRequestData.bodyText(): String = body.toByteArray().decodeToString()

    // ── GET ───────────────────────────────────────────────────────────────────

    @Test
    fun getAllHitsSlashAllOnceAndDecodesTheWholePayload() = runTest {
        val api = client { json(ApiFixtures.all) }
        val all = api.getAll()
        assertEquals(1, requests.size)
        assertEquals(HttpMethod.Get, requests[0].method)
        assertEquals("$base/all", requests[0].url.toString())
        assertEquals(2, all.skills.size)
        assertEquals("adaptability", all.softSkills.single().id)
        assertNull(all.certifications[1].url)
    }

    @Test
    fun getRequestsCarryTheBearerTokenWithoutWaitingForA401() = runTest {
        client { json(ApiFixtures.all) }.getAll()
        assertEquals("Bearer $token", requests.single().headers[HttpHeaders.Authorization])
    }

    @Test
    fun noAuthorizationHeaderWhenThereIsNoToken() = runTest {
        client(token = null) { json(ApiFixtures.all) }.getAll()
        assertNull(requests.single().headers[HttpHeaders.Authorization])
        requests.clear()
        client(token = "   ") { json(ApiFixtures.all) }.getAll()
        assertNull(requests.single().headers[HttpHeaders.Authorization])
    }

    @Test
    fun listEndpointsUseTheApiPaths() = runTest {
        val api = client { request ->
            when (request.url.encodedPath) {
                "/skills" -> json("[${ApiFixtures.skill}]")
                "/soft-skills" -> json("[${ApiFixtures.softSkill}]")
                "/languages" -> json(ApiFixtures.languages)
                else -> error(HttpStatusCode.NotFound, """{"error":"Not found"}""")
            }
        }
        assertEquals("kotlin", api.getSkills().single().id)
        assertEquals("adaptability", api.getSoftSkills().single().id)
        assertEquals("en", api.getLanguages().default)
    }

    // ── Writes ────────────────────────────────────────────────────────────────

    @Test
    fun updatePersonalIsAJsonPutWithBearerAndFullBody() = runTest {
        val personal = Personal(name = "N", email = "n@example.com", github = "")
        val api = client { request -> json(request.bodyText()) }
        val echoed = api.updatePersonal(personal)

        val request = requests.single()
        assertEquals(HttpMethod.Put, request.method)
        assertEquals("$base/personal", request.url.toString())
        assertEquals("Bearer $token", request.headers[HttpHeaders.Authorization])
        assertTrue(request.body.contentType.toString().startsWith("application/json"))
        val sent = careerJson.parseToJsonElement(request.bodyText()).jsonObject
        assertEquals("n@example.com", sent["email"]!!.jsonPrimitive.content)
        assertEquals("", sent["github"]!!.jsonPrimitive.content, "cleared fields must be sent (PATCH merge semantics)")
        assertEquals(personal, echoed)
    }

    @Test
    fun updateLanguagesIsAPut() = runTest {
        val api = client { request -> json(request.bodyText()) }
        api.updateLanguages(Languages(default = "es"))
        assertEquals(HttpMethod.Put, requests.single().method)
        assertEquals("$base/languages", requests.single().url.toString())
    }

    @Test
    fun updateJobPatchesTheItemPathUsingItsId() = runTest {
        val job = careerJson.decodeFromString<Job>(ApiFixtures.job)
        val api = client { request -> json(request.bodyText()) }
        val updated = api.updateJob(job.copy(company = "Solusoft S.L."))

        val request = requests.single()
        assertEquals(HttpMethod.Patch, request.method)
        assertEquals("$base/jobs/senior-solusoft", request.url.toString())
        val sent = careerJson.parseToJsonElement(request.bodyText()).jsonObject
        assertEquals("Solusoft S.L.", sent["company"]!!.jsonPrimitive.content)
        assertEquals(2, sent["achievements"]!!.jsonObject["en"]!!.jsonArray.size)
        assertEquals("Solusoft S.L.", updated.company)
    }

    @Test
    fun otherEntitiesPatchTheirOwnCollections() = runTest {
        val api = client { request -> json(request.bodyText()) }
        api.updateProject(Project(id = "agedi", name = "AGEDI", context = "work"))
        api.updateSkill(Skill(id = "kotlin", name = "Kotlin", level = 4, category = "languages"))
        assertEquals(listOf("/projects/agedi", "/skills/kotlin"), requests.map { it.url.encodedPath })
        assertTrue(requests.all { it.method == HttpMethod.Patch })
    }

    @Test
    fun createPostsToTheCollectionAndReturnsTheServerCopy() = runTest {
        val api = client { request -> json(request.bodyText(), HttpStatusCode.Created) }
        val created = api.createSkill(Skill(id = "kmp", name = "Kotlin Multiplatform", level = 2, category = "frameworks"))
        assertEquals(HttpMethod.Post, requests.single().method)
        assertEquals("$base/skills", requests.single().url.toString())
        assertEquals("kmp", created.id)
        assertNull(created.icon_url)
    }

    @Test
    fun deleteUsesTheItemPathAndReturnsTheDeletedEntity() = runTest {
        val api = client { json(ApiFixtures.project) }
        val deleted = api.deleteProject("agedi")
        assertEquals(HttpMethod.Delete, requests.single().method)
        assertEquals("$base/projects/agedi", requests.single().url.toString())
        assertEquals("AGEDI System", deleted.name)
    }

    // ── Errors ────────────────────────────────────────────────────────────────

    @Test
    fun a401IsAClientRequestExceptionCarryingTheStatus() = runTest {
        val api = client { error(HttpStatusCode.Unauthorized, ApiFixtures.unauthorized) }
        val e = assertFailsWith<ClientRequestException> { api.updateLanguages(Languages()) }
        assertEquals(HttpStatusCode.Unauthorized, e.response.status)
        assertEquals(ApiFixtures.unauthorized, e.response.bodyAsText())
    }

    @Test
    fun a400KeepsTheValidationDetailsInTheResponseBody() = runTest {
        val api = client { error(HttpStatusCode.BadRequest, ApiFixtures.validationFailed) }
        val e = assertFailsWith<ClientRequestException> {
            api.createProject(Project(id = "x", name = "X", context = "hobby"))
        }
        assertEquals(400, e.response.status.value)
        val body = careerJson.parseToJsonElement(e.response.bodyAsText()).jsonObject
        assertEquals("Validation failed", body["error"]!!.jsonPrimitive.content)
        val details = body["details"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertEquals(2, details.size)
        assertTrue(details[0].startsWith("projects.context"))
    }

    @Test
    fun a413IsSurfacedAsAClientError() = runTest {
        val api = client { error(HttpStatusCode.PayloadTooLarge, ApiFixtures.bodyTooLarge) }
        val e = assertFailsWith<ClientRequestException> { api.updatePersonal(Personal()) }
        assertEquals(413, e.response.status.value)
    }

    @Test
    fun a5xxIsAServerResponseException() = runTest {
        val api = client { error(HttpStatusCode.BadGateway, "upstream down") }
        val e = assertFailsWith<ServerResponseException> { api.getAll() }
        assertEquals(502, e.response.status.value)
    }

    @Test
    fun errorBodiesAreNeverDecodedIntoEmptyModels() = runTest {
        // expectSuccess = true: a 404 must throw, not come back as Job()
        val api = client { error(HttpStatusCode.NotFound, """{"error":"Not found"}""") }
        assertFailsWith<ClientRequestException> { api.deleteJob("missing") }
    }

    // ── Probes used by login ──────────────────────────────────────────────────

    @Test
    fun validateConnectionIsTrueOnlyForASuccessfulRoot() = runTest {
        assertTrue(client { json("""{"name":"career-api"}""") }.validateConnection())
        assertFalse(client { error(HttpStatusCode.ServiceUnavailable, "") }.validateConnection())
        assertFalse(client { throw IllegalStateException("no network") }.validateConnection())
        assertEquals("api.test.invalid", requests.first().url.host)
        assertTrue(requests.first().url.encodedPath in listOf("", "/"))
    }

    @Test
    fun validateAuthSendsOnlyTheNonMutatingProbe() = runTest {
        client { error(HttpStatusCode.BadRequest, ApiFixtures.invalidJson) }.validateAuth()
        val probe = requests.single()
        // The only write verb is a PATCH to an id that cannot exist, with a body that cannot parse:
        // the worker answers it after auth and before any KV access. Never /languages or a real id.
        assertEquals(HttpMethod.Patch, probe.method)
        assertEquals("$base/jobs/__folio-auth-probe__", probe.url.toString())
        assertEquals("{", probe.bodyText())
        assertEquals("Bearer $token", probe.headers[HttpHeaders.Authorization])
    }

    @Test
    fun validateAuthIsTrueOnlyWhenTheProbeGetsPastAuth() = runTest {
        // 400 = auth passed, the malformed body was rejected; nothing was written
        assertTrue(client { error(HttpStatusCode.BadRequest, ApiFixtures.invalidJson) }.validateAuth())
        assertFalse(client { error(HttpStatusCode.Unauthorized, ApiFixtures.unauthorized) }.validateAuth())
        assertFalse(client { error(HttpStatusCode.Forbidden, """{"error":"Forbidden"}""") }.validateAuth())
        // Unknown answers are not proof of a good token
        assertFalse(client { error(HttpStatusCode.NotFound, """{"error":"Route not found"}""") }.validateAuth())
        assertFalse(client { error(HttpStatusCode.InternalServerError, "") }.validateAuth())
        assertFalse(client { throw IllegalStateException("no network") }.validateAuth())
        // Without a token there is no Authorization header at all
        assertFalse(client(token = null) { error(HttpStatusCode.Unauthorized, ApiFixtures.unauthorized) }.validateAuth())
        assertNull(requests.last().headers[HttpHeaders.Authorization])
    }
}
