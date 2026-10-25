package dev.sebas1705.careereditor.data.model

import dev.sebas1705.careereditor.ApiFixtures
import dev.sebas1705.careereditor.data.api.careerJson
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** DTOs against the JSON shapes the API really serves, decoded with the client's own Json. */
class SerializationTest {

    private val json = careerJson

    @Test
    fun allDataDecodesEveryEntityIncludingHyphenatedSoftSkills() {
        val all = json.decodeFromString<AllData>(ApiFixtures.all)
        assertEquals("en", all.languages.default)
        assertEquals("Sebastián Entrerrios García", all.personal.name)
        assertEquals(listOf("senior-solusoft"), all.jobs.map { it.id })
        assertEquals(listOf("agedi"), all.projects.map { it.id })
        assertEquals(listOf("kotlin", "kmp"), all.skills.map { it.id })
        assertEquals(listOf("big-school"), all.education.map { it.id })
        assertEquals(listOf("jetpack-compose-appcademy", "aws-saa"), all.certifications.map { it.id })
        assertEquals(listOf("adaptability"), all.softSkills.map { it.id })
    }

    @Test
    fun allDataSerializesSoftSkillsUnderTheApiKey() {
        val encoded = json.encodeToJsonElement(AllData.serializer(), AllData(softSkills = listOf(SoftSkill("x")))).jsonObject
        assertTrue("soft-skills" in encoded, "expected the hyphenated key, got ${encoded.keys}")
        assertFalse("softSkills" in encoded)
    }

    @Test
    fun allDataToleratesMissingAndNullSingularEntities() {
        val all = json.decodeFromString<AllData>("""{"languages":null,"jobs":[]}""")
        assertEquals(Languages(), all.languages)
        assertEquals(Personal(), all.personal)
        assertTrue(all.projects.isEmpty())
    }

    @Test
    fun unknownKeysFromANewerApiAreIgnored() {
        val skill = json.decodeFromString<Skill>("""{"id":"go","name":"Go","level":2,"category":"languages","brand_new_field":true}""")
        assertEquals("go", skill.id)
    }

    @Test
    fun languagesRoundTripKeepsLabelNativeKey() {
        val decoded = json.decodeFromString<Languages>(ApiFixtures.languages)
        assertEquals(Language("es", "Spanish", "Español"), decoded.supported[1])
        val encoded = json.encodeToJsonElement(Languages.serializer(), decoded).jsonObject
        val es = encoded["supported"]!!.jsonArray[1].jsonObject
        assertEquals("Español", es["label_native"]!!.jsonPrimitive.content)
        assertEquals(decoded, json.decodeFromString<Languages>(json.encodeToString(Languages.serializer(), decoded)))
    }

    @Test
    fun personalRoundTripUsesSnakeCaseCvUrl() {
        val decoded = json.decodeFromString<Personal>(ApiFixtures.personal)
        assertEquals("https://sebas1705carreer.github.io/carreerV2/cv/cv-en.pdf", decoded.cvUrl)
        assertEquals(mapOf("en" to "Madrid, Spain", "es" to "Madrid, España"), decoded.location)
        val encoded = json.encodeToJsonElement(Personal.serializer(), decoded).jsonObject
        assertTrue("cv_url" in encoded)
        assertFalse("cvUrl" in encoded)
        assertEquals(decoded, json.decodeFromString<Personal>(json.encodeToString(Personal.serializer(), decoded)))
    }

    @Test
    fun jobDecodesNullEndDateAndLocalizedAchievements() {
        val job = json.decodeFromString<Job>(ApiFixtures.job)
        assertNull(job.endDate)
        assertEquals("2026-01", job.startDate)
        assertEquals("https://www.solusoft.es/", job.companyUrl)
        assertEquals(2, job.achievements.getValue("en").size)
        assertEquals("Promoción a Senior dentro del mismo equipo", job.achievements.getValue("es").first())
        assertEquals(listOf("AGEDI", "Sisley", "Iberext"), job.projects)
        assertEquals("Technical lead of the mobile and full-stack side.", job.longDesc["en"])
    }

    @Test
    fun jobRoundTripKeepsCamelCaseDatesAndSnakeCaseLongDesc() {
        val job = json.decodeFromString<Job>(ApiFixtures.job)
        val encoded = json.encodeToJsonElement(Job.serializer(), job).jsonObject
        assertTrue("startDate" in encoded && "endDate" in encoded && "companyUrl" in encoded)
        assertTrue("long_desc" in encoded)
        assertFalse("longDesc" in encoded)
        assertEquals(JsonNull, encoded["endDate"])
        assertEquals(job, json.decodeFromString<Job>(encoded.toString()))
    }

    @Test
    fun projectDecodesExplicitNullLinks() {
        val project = json.decodeFromString<Project>(ApiFixtures.project)
        assertNull(project.github)
        assertNull(project.demo)
        assertEquals("work", project.context)
        assertEquals(listOf("C#", ".NET", "SQL Server"), project.tags)
    }

    @Test
    fun projectRoundTripIsLossless() {
        val project = json.decodeFromString<Project>(ApiFixtures.project)
        assertEquals(project, json.decodeFromString<Project>(json.encodeToString(Project.serializer(), project)))
    }

    @Test
    fun skillDecodesWithAndWithoutIcon() {
        val kotlin = json.decodeFromString<Skill>(ApiFixtures.skill)
        assertEquals(4, kotlin.level)
        assertTrue(kotlin.icon_url!!.startsWith("https://img.icons8.com/"))
        val kmp = json.decodeFromString<Skill>(ApiFixtures.skillWithoutIcon)
        assertNull(kmp.icon_url)
        assertEquals(2, kmp.level)
    }

    @Test
    fun skillNullLevelFallsBackToDefaultInsteadOfFailing() {
        // coerceInputValues: a null in a non-nullable field must not take the whole GET /all down
        val skill = json.decodeFromString<Skill>("""{"id":"x","name":"X","level":null,"category":null}""")
        assertEquals(1, skill.level)
        assertEquals("", skill.category)
    }

    @Test
    fun educationRoundTripKeepsLocalizedMaps() {
        val education = json.decodeFromString<Education>(ApiFixtures.education)
        assertEquals("graduation", education.icon)
        assertEquals("Máster de Desarrollo con IA", education.degree["es"])
        assertEquals(education, json.decodeFromString<Education>(json.encodeToString(Education.serializer(), education)))
    }

    @Test
    fun certificationUrlMayBeNullForInProgressOnes() {
        val done = json.decodeFromString<Certification>(ApiFixtures.certification)
        assertEquals("https://www.appcademy.dev/certificates/cert_JGzFpajl", done.url)
        val inProgress = json.decodeFromString<Certification>(ApiFixtures.certificationInProgress)
        assertNull(inProgress.url)
        assertTrue(inProgress.longDesc.isEmpty())
    }

    @Test
    fun softSkillNameIsALocalizedMap() {
        val soft = json.decodeFromString<SoftSkill>(ApiFixtures.softSkill)
        assertEquals(mapOf("en" to "Adaptability", "es" to "Adaptabilidad"), soft.name)
        assertEquals(soft, json.decodeFromString<SoftSkill>(json.encodeToString(SoftSkill.serializer(), soft)))
    }

    @Test
    fun encodeDefaultsSendsClearedFieldsSoAPatchMergeDoesNotKeepOldValues() {
        // The user emptied the GitHub URL: the PATCH body must carry "" (and null links as null),
        // otherwise the API merge would keep the previous value.
        val personal = json.encodeToJsonElement(Personal.serializer(), Personal(name = "N", github = "")).jsonObject
        assertEquals(JsonPrimitive(""), personal["github"])
        assertEquals(JsonPrimitive(""), personal["email"])

        val project = json.encodeToJsonElement(Project.serializer(), Project(id = "p", name = "P", context = "personal")).jsonObject
        assertEquals(JsonNull, project["github"])
        assertEquals(JsonNull, project["demo"])
        assertEquals(0, project["tags"]!!.jsonArray.size)
    }
}
