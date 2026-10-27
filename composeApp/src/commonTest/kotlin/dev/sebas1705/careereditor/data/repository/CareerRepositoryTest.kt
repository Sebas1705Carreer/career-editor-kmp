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

    /** Root answers 200; the auth probe answers [probe]. */
    private fun api(probe: HttpStatusCode, probeBody: String): suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { req ->
        if (req.method == HttpMethod.Get) json("""{"name":"career-api"}""") else error(probe, probeBody)
    }

    private fun store(vararg extra: Pair<String, String>) = FakeStore(mapOf(SecureKeys.API_BASE_URL to base, *extra))

    @Test
    fun aRejectedTokenIsNeverPersisted() = runTest {
        val store = store()
        val result = repo(store, api(HttpStatusCode.Unauthorized, ApiFixtures.unauthorized)).login(candidate)

        assertEquals(LoginResult.Unauthorized, result)
        assertTrue(store.puts.isEmpty(), "nothing may be written before the token is accepted")
        assertNull(store.values[SecureKeys.API_TOKEN])
        // The candidate was still the one sent, from memory
        assertEquals("Bearer $candidate", requests.last().headers[HttpHeaders.Authorization])
    }

    @Test
    fun anUnreachableApiPersistsNothingAndSkipsTheAuthProbe() = runTest {
        val store = store()
        val result = repo(store) { error(HttpStatusCode.ServiceUnavailable, "") }.login(candidate)

        assertEquals(LoginResult.Unreachable, result)
        assertTrue(store.puts.isEmpty())
        assertEquals(listOf(HttpMethod.Get), requests.map { it.method })
    }

    @Test
    fun aFailedLoginLeavesThePreviouslyStoredTokenAlone() = runTest {
        val store = store(SecureKeys.API_TOKEN to "previous-token")
        repo(store, api(HttpStatusCode.Unauthorized, ApiFixtures.unauthorized)).login(candidate)

        assertEquals("previous-token", store.values[SecureKeys.API_TOKEN])
        assertTrue(store.puts.isEmpty() && store.removes.isEmpty())
    }

    @Test
    fun anAcceptedTokenIsPersistedTrimmedAfterValidation() = runTest {
        val store = store()
        val result = repo(store, api(HttpStatusCode.BadRequest, ApiFixtures.invalidJson))
            .login("﻿  $candidate ​")

        assertEquals(LoginResult.Success, result)
        assertEquals(listOf(SecureKeys.API_TOKEN to candidate), store.puts)
        // Validated with the cleaned candidate, before it was ever stored
        assertEquals("Bearer $candidate", requests.last().headers[HttpHeaders.Authorization])
    }

    @Test
    fun loginNeverSendsAMutatingRequestOtherThanTheProbe() = runTest {
        repo(store(), api(HttpStatusCode.BadRequest, ApiFixtures.invalidJson)).login(candidate)

        val writes = requests.filter { it.method != HttpMethod.Get }
        assertEquals(1, writes.size)
        assertEquals(HttpMethod.Patch, writes.single().method)
        assertEquals("$base/jobs/__folio-auth-probe__", writes.single().url.toString())
        assertTrue(requests.all { it.url.host == "api.test.invalid" })
    }
}
