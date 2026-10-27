package dev.sebas1705.careereditor.data.repository

import dev.sebas1705.careereditor.ApiFixtures
import dev.sebas1705.careereditor.security.KeyValueStore
import dev.sebas1705.careereditor.security.SecureKeys
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** In-memory stand-in for the encrypted store, recording every write. */
private class FakeStore(initial: Map<String, String> = emptyMap()) : KeyValueStore {
    val values = initial.toMutableMap()
    val puts = mutableListOf<Pair<String, String>>()
    val removes = mutableListOf<String>()
    override fun put(key: String, value: String) { puts += key to value; values[key] = value }
    override fun get(key: String): String? = values[key]
    override fun remove(key: String) { removes += key; values.remove(key) }
    override fun clear() = values.clear()
}

/** Login through the repository against a MockEngine: nothing here ever reaches the real API. */
class CareerRepositoryTest {

    private val base = "https://api.test.invalid"
    private val candidate = "candidate-token-not-real"
    private val requests = mutableListOf<HttpRequestData>()

    private fun MockRequestHandleScope.json(body: String) =
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))

    private fun MockRequestHandleScope.error(status: HttpStatusCode, body: String) =
        respondError(status, body, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))

    private fun repo(
        store: KeyValueStore,
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
    ) = CareerRepository(store, MockEngine { request ->
        requests += request
        handler(request)
    })

    /** Root answers 200; GET /auth/check answers 204 when [tokenAccepted], else 401. Anything else fails the test. */
    private fun api(tokenAccepted: Boolean): suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { req ->
        when {
            req.method != HttpMethod.Get -> kotlin.test.fail("unexpected ${req.method.value} ${req.url}")
            req.url.encodedPath == "/auth/check" ->
                if (tokenAccepted) respond("", HttpStatusCode.NoContent)
                else error(HttpStatusCode.Unauthorized, ApiFixtures.unauthorized)
            else -> json("""{"name":"career-api"}""")
        }
    }

    private fun store(vararg extra: Pair<String, String>) = FakeStore(mapOf(SecureKeys.API_BASE_URL to base, *extra))

    @Test
    fun aRejectedTokenIsNeverPersisted() = runTest {
        val store = store()
        val result = repo(store, api(tokenAccepted = false)).login(candidate)

        assertEquals(LoginResult.Unauthorized, result)
        assertTrue(store.puts.isEmpty(), "nothing may be written before the token is accepted")
        assertNull(store.values[SecureKeys.API_TOKEN])
        // The candidate was still the one sent, from memory
        assertEquals("Bearer $candidate", requests.last().headers[HttpHeaders.Authorization])
    }

    @Test
    fun anUnreachableApiPersistsNothingAndSkipsTheAuthCheck() = runTest {
        val store = store()
        val result = repo(store) { error(HttpStatusCode.ServiceUnavailable, "") }.login(candidate)

        assertEquals(LoginResult.Unreachable, result)
        assertTrue(store.puts.isEmpty())
        assertEquals(listOf(HttpMethod.Get), requests.map { it.method })
    }

    @Test
    fun aFailedLoginLeavesThePreviouslyStoredTokenAlone() = runTest {
        val store = store(SecureKeys.API_TOKEN to "previous-token")
        repo(store, api(tokenAccepted = false)).login(candidate)

        assertEquals("previous-token", store.values[SecureKeys.API_TOKEN])
        assertTrue(store.puts.isEmpty() && store.removes.isEmpty())
    }

    @Test
    fun anAcceptedTokenIsPersistedTrimmedAfterValidation() = runTest {
        val store = store()
        val result = repo(store, api(tokenAccepted = true))
            .login("﻿  $candidate ​")

        assertEquals(LoginResult.Success, result)
        assertEquals(listOf(SecureKeys.API_TOKEN to candidate), store.puts)
        // Validated with the cleaned candidate, before it was ever stored
        assertEquals("Bearer $candidate", requests.last().headers[HttpHeaders.Authorization])
    }

    @Test
    fun loginOnlySendsReadOnlyRequests() = runTest {
        repo(store(), api(tokenAccepted = true)).login(candidate)

        // Root reachability, then GET /auth/check: no write verb anywhere in the auth path
        assertEquals(listOf(HttpMethod.Get, HttpMethod.Get), requests.map { it.method })
        assertEquals("$base/auth/check", requests.last().url.toString())
        assertTrue(requests.all { it.url.host == "api.test.invalid" })
    }

    // ── Settings ──────────────────────────────────────────────────────────────

    @Test
    fun settingsWithARejectedTokenPersistNothingAndKeepThePreviousToken() = runTest {
        val store = store(SecureKeys.API_TOKEN to "previous-token")
        val result = repo(store, api(tokenAccepted = false)).saveSettings(newUrl = "", newToken = candidate)

        assertEquals(LoginResult.Unauthorized, result)
        assertEquals("previous-token", store.values[SecureKeys.API_TOKEN])
        assertTrue(store.puts.isEmpty() && store.removes.isEmpty())
        assertEquals("Bearer $candidate", requests.last().headers[HttpHeaders.Authorization])
        assertTrue(requests.all { it.method == HttpMethod.Get })
    }

    @Test
    fun settingsWithARejectedTokenDoNotSaveTheNewUrlEither() = runTest {
        val newBase = "https://other.test.invalid"
        val store = store(SecureKeys.API_TOKEN to "previous-token")
        val result = repo(store, api(tokenAccepted = false)).saveSettings(newUrl = newBase, newToken = candidate)

        assertEquals(LoginResult.Unauthorized, result)
        assertTrue(store.puts.isEmpty())
        assertEquals(base, store.values[SecureKeys.API_BASE_URL])
        // The token was checked against the URL being saved
        assertEquals("$newBase/auth/check", requests.last().url.toString())
    }

    @Test
    fun settingsWithAnUnreachableApiPersistNothing() = runTest {
        val store = store(SecureKeys.API_TOKEN to "previous-token")
        val result = repo(store) { error(HttpStatusCode.ServiceUnavailable, "") }.saveSettings("", candidate)

        assertEquals(LoginResult.Unreachable, result)
        assertTrue(store.puts.isEmpty())
        assertEquals("previous-token", store.values[SecureKeys.API_TOKEN])
    }

    @Test
    fun settingsWithAnAcceptedTokenPersistItOnceCleaned() = runTest {
        val store = store(SecureKeys.API_TOKEN to "previous-token")
        val result = repo(store, api(tokenAccepted = true)).saveSettings(newUrl = "", newToken = "﻿  $candidate ​")

        assertEquals(LoginResult.Success, result)
        assertEquals(listOf(SecureKeys.API_TOKEN to candidate), store.puts)
        assertEquals(candidate, store.values[SecureKeys.API_TOKEN])
        assertEquals("Bearer $candidate", requests.last().headers[HttpHeaders.Authorization])
        assertTrue(requests.all { it.method == HttpMethod.Get })
    }

    @Test
    fun settingsWithAnAcceptedTokenAndNewUrlPersistBoth() = runTest {
        val newBase = "https://other.test.invalid"
        val store = store()
        val result = repo(store, api(tokenAccepted = true)).saveSettings(newUrl = "  $newBase ", newToken = candidate)

        assertEquals(LoginResult.Success, result)
        assertEquals(
            listOf(SecureKeys.API_BASE_URL to newBase, SecureKeys.API_TOKEN to candidate),
            store.puts
        )
        assertTrue(requests.all { it.url.host == "other.test.invalid" })
    }

    @Test
    fun settingsWithoutANewTokenSaveOnlyTheUrlAndSendNothing() = runTest {
        val store = store(SecureKeys.API_TOKEN to "previous-token")
        val result = repo(store, api(tokenAccepted = false)).saveSettings(newUrl = "https://other.test.invalid", newToken = " ​ ")

        assertEquals(LoginResult.Success, result)
        assertEquals(listOf(SecureKeys.API_BASE_URL to "https://other.test.invalid"), store.puts)
        assertEquals("previous-token", store.values[SecureKeys.API_TOKEN])
        assertTrue(requests.isEmpty())
    }
}
