package dev.sebas1705.careereditor.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class SlugTest {

    @Test
    fun lowercasesAndJoinsWordsWithHyphens() {
        assertEquals("jetpack-compose-definitive-course", "Jetpack Compose: Definitive Course".toSlug())
    }

    @Test
    fun stripsAccentsAndEnye() {
        assertEquals("master-de-desarrollo-con-ia", "Máster de Desarrollo con IA".toSlug())
        assertEquals("espanol-y-frances", "Español y Francés".toSlug())
        assertEquals("curacao", "Curaçao".toSlug())
    }

    @Test
    fun collapsesSymbolRunsIntoOneHyphen() {
        assertEquals("c-net", "C# / .NET".toSlug())
        assertEquals("full-stack-mobile", "Full Stack & Mobile".toSlug())
    }

    @Test
    fun trimsSurroundingWhitespaceAndHyphens() {
        assertEquals("kotlin", "  Kotlin  ".toSlug())
        assertEquals("kotlin", "--Kotlin!!".toSlug())
    }

    @Test
    fun keepsDigits() {
        assertEquals("android-35", "Android 35".toSlug())
    }

    @Test
    fun blankOrSymbolOnlyInputGivesEmptySlug() {
        assertEquals("", "".toSlug())
        assertEquals("", "   ".toSlug())
        assertEquals("", "!!!".toSlug())
    }

    @Test
    fun isIdempotent() {
        val once = "Senior Full Stack & Mobile Developer".toSlug()
        assertEquals(once, once.toSlug())
    }

    @Test
    fun matchesTheIdsTheApiAlreadyUses() {
        assertEquals("agedi", "AGEDI".toSlug())
        assertEquals("kotlin-multiplatform", "Kotlin Multiplatform".toSlug())
        assertEquals("adaptability", "Adaptability".toSlug())
    }
}
