package dev.sebas1705.careereditor.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Job(
    val id: String = "",
    val company: String = "",
    @SerialName("companyUrl") val companyUrl: String = "",
    @SerialName("startDate") val startDate: String = "",
    @SerialName("endDate") val endDate: String? = null,
    // LocalizedString fields — { "en": "...", "es": "...", [anyCode]: "..." }
    val role: Map<String, String> = emptyMap(),
    val type: Map<String, String> = emptyMap(),
    val period: Map<String, String> = emptyMap(),
    val desc: Map<String, String> = emptyMap(),
    // LocalizedString: the long text the portfolio shows when a card is opened
    @SerialName("long_desc") val longDesc: Map<String, String> = emptyMap(),
    // Non-localizable
    val projects: List<String> = emptyList(),
    // Localized list — { "en": ["...", "..."], "es": ["...", "..."] }
    val achievements: Map<String, List<String>> = emptyMap(),
)
