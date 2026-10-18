package dev.sebas1705.careereditor.presentation.screens

import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import dev.sebas1705.careereditor.data.model.Project
import dev.sebas1705.careereditor.data.model.resolve
import dev.sebas1705.careereditor.presentation.components.*
import dev.sebas1705.careereditor.presentation.viewmodel.CareerUiState
import dev.sebas1705.careereditor.utils.toSlug

private val contextOptions = listOf("work" to "Trabajo", "academic" to "Académico", "personal" to "Personal")
private val contextLabels = contextOptions.toMap()

@Composable
fun ProjectsScreen(
    state: CareerUiState,
    lang: String,
    onSelectLang: (String) -> Unit,
    onSave: (Project, (Boolean) -> Unit) -> Unit,
    onCreate: (Project, (Boolean) -> Unit) -> Unit,
    onDelete: (String, (Boolean) -> Unit) -> Unit
) {
    var selected by remember { mutableStateOf<Project?>(null) }
    var creating by remember { mutableStateOf(false) }
    val editing = selected

    when {
        editing != null -> ProjectEditor(
            project = editing, isNew = false, existing = state.projects, state = state, lang = lang, onLang = onSelectLang,
            onSave = { p -> onSave(p) { ok -> if (ok) selected = null } },
            onDelete = { onDelete(editing.id) { ok -> if (ok) selected = null } },
            onBack = { selected = null }
        )
        creating -> ProjectEditor(
            project = Project(), isNew = true, existing = state.projects, state = state, lang = lang, onLang = onSelectLang,
            onSave = { p -> onCreate(p) { ok -> if (ok) creating = false } },
            onDelete = null,
            onBack = { creating = false }
        )
        else -> ProjectList(state, lang, onSelectLang, onOpen = { selected = it }, onAdd = { creating = true })
    }
}

@Composable
private fun ProjectList(state: CareerUiState, lang: String, onSelectLang: (String) -> Unit, onOpen: (Project) -> Unit, onAdd: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("all") }

    val filtered = remember(state.projects, query, filter) {
        state.projects.filter { p ->
            (filter == "all" || p.context == filter) &&
                (query.isBlank() || p.name.contains(query, ignoreCase = true) || p.tags.any { it.contains(query, ignoreCase = true) })
        }
    }
    val filterOptions = listOf("all" to "Todos · ${state.projects.size}") +
        contextOptions.map { (key, label) -> key to "$label · ${state.projects.count { it.context == key }}" }

    ListFrame(
        addLabel = "Nuevo proyecto",
        onAdd = onAdd,
        header = {
            SearchField(query, { query = it }, "Buscar por nombre o tag")
            FilterRow(filterOptions, filter) { filter = it }
            ListHeader(
                count = if (filtered.size == state.projects.size) "${filtered.size} proyectos" else "${filtered.size} de ${state.projects.size}",
                trailing = { LangSwitch(state.languages.supported, lang, onSelectLang) }
            )
        }
    ) {
        items(filtered, key = { it.id }) { project ->
            ItemCard(onClick = { onOpen(project) }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(project.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(contextLabels[project.context] ?: project.context, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                val desc = project.desc.resolve(lang)
                if (desc.isNotBlank()) {
                    Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (project.tags.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    // One line instead of rows of chips: the tags are for orientation, not for tapping
                    Text(
                        project.tags.take(4).joinToString(" · ") + if (project.tags.size > 4) "  +${project.tags.size - 4}" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectEditor(
    project: Project,
    isNew: Boolean,
    existing: List<Project>,
    state: CareerUiState,
    lang: String,
    onLang: (String) -> Unit,
    onSave: (Project) -> Unit,
    onDelete: (() -> Unit)?,
    onBack: () -> Unit
) {
    var template by remember { mutableStateOf<Project?>(null) }
    val base = if (isNew) template ?: Project() else project

    var id by remember { mutableStateOf("") }
    var idEdited by remember { mutableStateOf(false) }

    var name     by remember(base) { mutableStateOf(base.name) }
    var context  by remember(base) { mutableStateOf(base.context.ifBlank { if (isNew) "personal" else "" }) }
    var github   by remember(base) { mutableStateOf(base.github ?: "") }
    var demo     by remember(base) { mutableStateOf(base.demo ?: "") }
    var tags     by remember(base) { mutableStateOf(base.tags) }
    var desc     by remember(base) { mutableStateOf(base.desc) }
    var longDesc by remember(base) { mutableStateOf(base.longDesc) }

    val built = Project(
        id = if (isNew) id else project.id, name = name, context = context, desc = desc, longDesc = longDesc,
        tags = tags, github = github.ifBlank { null }, demo = demo.ifBlank { null }
    )
    val dirty = if (isNew) name.isNotBlank() || tags.isNotEmpty() || desc.isNotEmpty() else built != project

    EditorScaffold(
        title = if (isNew) "Nuevo proyecto" else project.name,
        isNew = isNew,
        dirty = dirty,
        canSave = name.isNotBlank() && context.isNotBlank() && (!isNew || id.isNotBlank()),
        saving = state.isLoading,
        saveLabel = if (isNew) "Crear proyecto" else "Guardar cambios",
        onSave = { onSave(built) },
        onBack = onBack,
        onDelete = onDelete,
        deleteName = project.name
    ) {
        if (isNew) TemplatePicker(existing, template, { it.name }, { template = it; idEdited = false })

        FieldGroup("General") {
            SectionField("Nombre del proyecto", name, {
                name = it
                if (isNew && !idEdited) id = it.toSlug()
            }, required = true, singleLine = true)
            if (isNew) IdField(id) { id = it; idEdited = true }
            SelectField("Contexto", context, contextOptions) { context = it }
            SectionField("GitHub", github, { github = it }, singleLine = true)
            SectionField("Demo", demo, { demo = it }, singleLine = true)
        }

        DynamicListField(label = "Tags", items = tags, onItemsChange = { tags = it }, placeholder = "Añadir tag (Kotlin, Firebase…)")

        FieldGroup("Descripción", trailing = { LangSwitch(state.languages.supported, lang, onLang) }) {
            SectionField("Descripción corta", desc[lang] ?: "", { desc = desc.with(lang, it) }, minLines = 2)
            SectionField("Descripción larga", longDesc[lang] ?: "", { longDesc = longDesc.with(lang, it) }, minLines = 4)
        }
    }
}
