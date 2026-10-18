package dev.sebas1705.careereditor.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Education(
    val id: String = "",
    val school: String = "",
    val icon: String = "",
    // LocalizedString fields — { "en": "...", "es": "...", [anyCode]: "..." }
    val degree: Map<String, String> = emptyMap(),
    val period: Map<String, String> = emptyMap(),
    val detail: Map<String, String> = emptyMap(),
    // LocalizedString: the long text the portfolio shows when a card is opened
    @SerialName("long_desc") val longDesc: Map<String, String> = emptyMap(),
)
