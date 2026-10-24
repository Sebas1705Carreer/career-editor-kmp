package dev.sebas1705.careereditor.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * What GET /all returns: every entity in one response, instead of eight requests.
 * A missing singular entity arrives as null and falls back to its default (the client
 * decodes with coerceInputValues).
 */
@Serializable
data class AllData(
    val languages: Languages = Languages(),
    val personal: Personal = Personal(),
    val jobs: List<Job> = emptyList(),
    val projects: List<Project> = emptyList(),
    val skills: List<Skill> = emptyList(),
    val education: List<Education> = emptyList(),
    val certifications: List<Certification> = emptyList(),
    @SerialName("soft-skills") val softSkills: List<SoftSkill> = emptyList()
)
