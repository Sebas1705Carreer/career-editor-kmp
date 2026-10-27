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

/**
 * Project cover image, as the API validates it (`image_url: must be an absolute https URL, empty
 * or null`, at most 2048 characters). The portfolio serves the files from GitHub Pages.
 */
object ProjectImages {
    const val MAX_URL_LENGTH = 2048
    const val BASE_URL = "https://sebas1705carreer.github.io/carreerV2/projects/"

    private val httpsUrl = Regex("""^https://[^\s/]+\S*$""")

    /** Blank means "no image"; anything else must be an absolute https URL of a sane length. */
    fun isValidUrl(url: String): Boolean =
        url.isBlank() || (url.trim().length <= MAX_URL_LENGTH && httpsUrl.matches(url.trim()))

    /** Where the portfolio expects the cover of a project. */
    fun suggestedUrl(projectId: String): String = "$BASE_URL$projectId.webp"

    /** The value to send: trimmed URL, or null when blank. */
    fun urlOrNull(url: String): String? = url.trim().ifBlank { null }

    /** The alt map to send: blank languages dropped, null when nothing is left. */
    fun altOrNull(alt: Map<String, String>): Map<String, String>? =
        alt.filterValues { it.isNotBlank() }.ifEmpty { null }
}
