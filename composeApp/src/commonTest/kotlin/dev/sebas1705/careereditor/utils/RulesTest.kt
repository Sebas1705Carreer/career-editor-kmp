package dev.sebas1705.careereditor.utils

import dev.sebas1705.careereditor.data.repository.trimBom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RulesTest {

    // ── Skill level: the API accepts 1..4 ─────────────────────────────────────

    @Test
    fun levelsOneToFourAreValid() {
        (1..4).forEach { assertTrue(SkillLevels.isValid(it), "level $it") }
        assertFalse(SkillLevels.isValid(0))
        assertFalse(SkillLevels.isValid(5))
        assertFalse(SkillLevels.isValid(-1))
    }

    @Test
    fun coerceClampsOutOfRangeLevels() {
        assertEquals(1, SkillLevels.coerce(0))
        assertEquals(1, SkillLevels.coerce(Int.MIN_VALUE))
        assertEquals(4, SkillLevels.coerce(5))
        assertEquals(4, SkillLevels.coerce(Int.MAX_VALUE))
        assertEquals(3, SkillLevels.coerce(3))
    }

    @Test
    fun oneLabelPerLevelInPortfolioOrder() {
        assertEquals(4, SkillLevels.labels.size)
        assertEquals("Básico", SkillLevels.label(1))
        assertEquals("Conocimiento", SkillLevels.label(2))
        assertEquals("Competente", SkillLevels.label(3))
        assertEquals("Avanzado", SkillLevels.label(4))
    }

    @Test
    fun labelNeverThrowsForBadLevels() {
        assertEquals("Básico", SkillLevels.label(0))
        assertEquals("Avanzado", SkillLevels.label(99))
    }

    // ── Project context: work | academic | personal ───────────────────────────

    @Test
    fun onlyTheThreeApiContextsAreValid() {
        assertEquals(listOf("work", "academic", "personal"), ProjectContexts.keys)
        assertTrue(ProjectContexts.isValid("work"))
        assertTrue(ProjectContexts.isValid("academic"))
        assertTrue(ProjectContexts.isValid("personal"))
        assertFalse(ProjectContexts.isValid(""))
        assertFalse(ProjectContexts.isValid("Work"))
        assertFalse(ProjectContexts.isValid("hobby"))
    }

    @Test
    fun contextLabelsAreSpanishAndUnknownKeysHaveNone() {
        assertEquals("Trabajo", ProjectContexts.label("work"))
        assertEquals("Académico", ProjectContexts.label("academic"))
        assertEquals("Personal", ProjectContexts.label("personal"))
        assertNull(ProjectContexts.label("other"))
    }

    // ── Token cleaning before it goes into the Authorization header ──────────

    @Test
    fun trimBomRemovesBomAndZeroWidthSpace() {
        assertEquals("abc123", "﻿abc123".trimBom())
        assertEquals("abc123", "abc123​".trimBom())
        assertEquals("abc123", "​﻿abc123﻿".trimBom())
    }

    @Test
    fun trimBomKeepsInnerCharactersAndPlainWhitespace() {
        assertEquals(" abc ", " abc ".trimBom())
        assertEquals("ab​c", "ab​c".trimBom())
    }

    @Test
    fun trimThenTrimBomCleansAPastedToken() {
        val pasted = "  ﻿tok_en_value​ \n"
        assertEquals("tok_en_value", pasted.trim().trimBom())
    }
}
