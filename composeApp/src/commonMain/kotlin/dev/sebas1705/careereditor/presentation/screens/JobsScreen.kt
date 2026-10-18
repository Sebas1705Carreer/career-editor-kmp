package dev.sebas1705.careereditor.presentation.screens

import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.text.style.TextOverflow
import dev.sebas1705.careereditor.data.model.Job
import dev.sebas1705.careereditor.data.model.resolve
import dev.sebas1705.careereditor.presentation.components.*
import dev.sebas1705.careereditor.presentation.viewmodel.CareerUiState
import dev.sebas1705.careereditor.utils.toSlug

@Composable
fun JobsScreen(
    state: CareerUiState,
    lang: String,
    onSelectLang: (String) -> Unit,
    onSave: (Job, (Boolean) -> Unit) -> Unit,
    onCreate: (Job, (Boolean) -> Unit) -> Unit,
    onDelete: (String, (Boolean) -> Unit) -> Unit
) {
    var selected by remember { mutableStateOf<Job?>(null) }
    var creating by remember { mutableStateOf(false) }
    val editing = selected

    when {
        editing != null -> JobEditor(
            job = editing, isNew = false, existing = state.jobs, state = state, lang = lang, onLang = onSelectLang,
            onSave = { j -> onSave(j) { ok -> if (ok) selected = null } },
            onDelete = { onDelete(editing.id) { ok -> if (ok) selected = null } },
            onBack = { selected = null }
        )
        creating -> JobEditor(
            job = Job(), isNew = true, existing = state.jobs, state = state, lang = lang, onLang = onSelectLang,
            onSave = { j -> onCreate(j) { ok -> if (ok) creating = false } },
            onDelete = null,
            onBack = { creating = false }
        )
        else -> ListFrame(
            addLabel = "Nuevo trabajo",
            onAdd = { creating = true },
            header = {
                ListHeader("${state.jobs.size} posiciones", trailing = { LangSwitch(state.languages.supported, lang, onSelectLang) })
            }
        ) {
            items(state.jobs, key = { it.id }) { job ->
                ItemCard(onClick = { selected = job }) {
                    Text(job.role.resolve(lang), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOf(job.company, job.type.resolve(lang)).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(job.period.resolve(lang), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun JobEditor(
    job: Job,
    isNew: Boolean,
    existing: List<Job>,
    state: CareerUiState,
    lang: String,
    onLang: (String) -> Unit,
    onSave: (Job) -> Unit,
    onDelete: (() -> Unit)?,
    onBack: () -> Unit
) {
    var template by remember { mutableStateOf<Job?>(null) }
    val base = if (isNew) template ?: Job() else job

    var id by remember { mutableStateOf("") }
    var idEdited by remember { mutableStateOf(false) }

    var company      by remember(base) { mutableStateOf(base.company) }
    var companyUrl   by remember(base) { mutableStateOf(base.companyUrl) }
    var startDate    by remember(base) { mutableStateOf(base.startDate) }
    var endDate      by remember(base) { mutableStateOf(base.endDate ?: "") }
    var role         by remember(base) { mutableStateOf(base.role) }
    var type         by remember(base) { mutableStateOf(base.type) }
    var period       by remember(base) { mutableStateOf(base.period) }
    var desc         by remember(base) { mutableStateOf(base.desc) }
    var longDesc     by remember(base) { mutableStateOf(base.longDesc) }
    var projects     by remember(base) { mutableStateOf(base.projects) }
    var achievements by remember(base) { mutableStateOf(base.achievements) }

    val built = Job(
        id = if (isNew) id else job.id, company = company, companyUrl = companyUrl,
        startDate = startDate, endDate = endDate.ifBlank { null },
        role = role, type = type, period = period, desc = desc, longDesc = longDesc,
        projects = projects, achievements = achievements
    )
    val dirty = if (isNew) company.isNotBlank() || role.isNotEmpty() || desc.isNotEmpty() else built != job

    EditorScaffold(
        title = if (isNew) "Nuevo trabajo" else company.ifBlank { job.id },
        isNew = isNew,
        dirty = dirty,
        canSave = company.isNotBlank() && (!isNew || id.isNotBlank()),
        saving = state.isLoading,
        saveLabel = if (isNew) "Crear trabajo" else "Guardar cambios",
        onSave = { onSave(built) },
        onBack = onBack,
        onDelete = onDelete,
        deleteName = company.ifBlank { job.id }
    ) {
        if (isNew) TemplatePicker(existing, template, { "${it.company} — ${it.role.resolve("en")}" }, { template = it; idEdited = false })

        FieldGroup("Empresa y fechas") {
            SectionField("Empresa", company, {
                company = it
                if (isNew && !idEdited) id = it.toSlug()
            }, required = true, singleLine = true)
            if (isNew) IdField(id) { id = it; idEdited = true }
            SectionField("Web de la empresa", companyUrl, { companyUrl = it }, singleLine = true)
            SectionField("Inicio (AAAA-MM)", startDate, { startDate = it }, singleLine = true)
            SectionField("Fin (AAAA-MM)", endDate, { endDate = it }, singleLine = true, supportingText = "Vacío si es el trabajo actual")
        }

        FieldGroup("Puesto", trailing = { LangSwitch(state.languages.supported, lang, onLang) }) {
            SectionField("Rol", role[lang] ?: "", { role = role.with(lang, it) }, singleLine = true)
            SectionField("Periodo", period[lang] ?: "", { period = period.with(lang, it) }, singleLine = true)
            SectionField("Modalidad", type[lang] ?: "", { type = type.with(lang, it) }, singleLine = true, supportingText = "Ej: Hybrid, Remote, On-site")
            SectionField("Descripción corta", desc[lang] ?: "", { desc = desc.with(lang, it) }, minLines = 2)
            SectionField("Descripción larga", longDesc[lang] ?: "", { longDesc = longDesc.with(lang, it) }, minLines = 4)
        }

        DynamicListField("Logros [${lang.uppercase()}]", achievements[lang] ?: emptyList(), { achievements = achievements.withList(lang, it) }, placeholder = "Añadir logro")
        DynamicListField("Proyectos vinculados", projects, { projects = it }, placeholder = "ID del proyecto")
    }
}
