package dev.sebas1705.careereditor.utils

/**
 * Value rules mirrored from the career API, so the editor never offers a value the
 * API would answer with 400 for (`skills.level: must be between 1 and 4`,
 * `projects.context: must be one of: work, academic, personal`).
 */
object SkillLevels {
    const val MIN = 1
    const val MAX = 4

    // Same names as the portfolio shows for levels 1-4
    val labels: List<String> = listOf("Básico", "Conocimiento", "Competente", "Avanzado")

    fun isValid(level: Int): Boolean = level in MIN..MAX
    fun coerce(level: Int): Int = level.coerceIn(MIN, MAX)
    fun label(level: Int): String = labels[coerce(level) - MIN]
}

object ProjectContexts {
    /** API key to the label shown in the editor, in display order. */
    val options: List<Pair<String, String>> =
        listOf("work" to "Trabajo", "academic" to "Académico", "personal" to "Personal")

    val keys: List<String> = options.map { it.first }

    fun isValid(context: String): Boolean = context in keys
    fun label(context: String): String? = options.firstOrNull { it.first == context }?.second
}
