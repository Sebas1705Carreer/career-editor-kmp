package dev.sebas1705.careereditor.data.model

import kotlin.test.Test
import kotlin.test.assertEquals

/** `{ lang: value }` maps: resolve() picks the requested language, then "en", then anything. */
class LocalizedStringTest {

    private val role = mapOf("en" to "Developer", "es" to "Desarrollador")

    @Test
    fun resolvesTheRequestedLanguage() {
        assertEquals("Desarrollador", role.resolve("es"))
        assertEquals("Developer", role.resolve("en"))
    }

    @Test
    fun fallsBackToEnglishWhenTheLanguageIsMissing() {
        assertEquals("Developer", role.resolve("fr"))
    }

    @Test
    fun fallbackLanguageCanBeOverridden() {
        assertEquals("Desarrollador", role.resolve("fr", fallback = "es"))
    }

    @Test
    fun fallsBackToTheFirstAvailableValueWhenNeitherExists() {
        val onlyGerman = mapOf("de" to "Entwickler")
        assertEquals("Entwickler", onlyGerman.resolve("es"))
    }

    @Test
    fun emptyMapResolvesToEmptyString() {
        assertEquals("", emptyMap<String, String>().resolve("en"))
    }

    @Test
    fun codesAreCaseSensitiveLikeTheApiKeys() {
        // "ES" is not a key: falls back to English rather than matching "es"
        assertEquals("Developer", role.resolve("ES"))
    }

    @Test
    fun resolveListFollowsTheSameOrder() {
        val achievements = mapOf("en" to listOf("A", "B"), "es" to listOf("C"))
        assertEquals(listOf("C"), achievements.resolveList("es"))
        assertEquals(listOf("A", "B"), achievements.resolveList("pt"))
        assertEquals(listOf("A", "B"), achievements.resolveList("pt", fallback = "zz"))
        assertEquals(emptyList(), emptyMap<String, List<String>>().resolveList("en"))
    }
}
