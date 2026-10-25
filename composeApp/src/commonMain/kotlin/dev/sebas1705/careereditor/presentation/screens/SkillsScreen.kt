package dev.sebas1705.careereditor.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.sebas1705.careereditor.data.model.Skill
import dev.sebas1705.careereditor.presentation.components.*
import dev.sebas1705.careereditor.presentation.viewmodel.CareerUiState
import dev.sebas1705.careereditor.utils.SkillLevels
import dev.sebas1705.careereditor.utils.toSlug

/** Display order and Spanish names of the skill categories (the API stores the English key). */
private val categories = listOf(
    "languages" to "Lenguajes",
    "frameworks" to "Frameworks",
    "tools" to "Herramientas",
    "databases" to "Bases de datos",
    "cloud" to "Cloud",
    "ides" to "IDEs",
    "ai" to "IA",
    "os" to "Sistemas operativos",
    "architecture" to "Arquitectura",
    "methodologies" to "Metodologías"
)
private val categoryLabels = categories.toMap()
private fun categoryLabel(key: String) = categoryLabels[key] ?: key.replaceFirstChar { it.uppercase() }

@Composable
fun SkillsScreen(
    state: CareerUiState,
    onSave: (Skill, (Boolean) -> Unit) -> Unit,
    onCreate: (Skill, (Boolean) -> Unit) -> Unit,
    onDelete: (String, (Boolean) -> Unit) -> Unit
) {
    var selected by remember { mutableStateOf<Skill?>(null) }
    var creating by remember { mutableStateOf(false) }
    val editing = selected

    when {
        editing != null -> SkillEditor(
            skill = editing, isNew = false, existing = state.skills, state = state,
            onSave = { s -> onSave(s) { ok -> if (ok) selected = null } },
            onDelete = { onDelete(editing.id) { ok -> if (ok) selected = null } },
            onBack = { selected = null }
        )
        creating -> SkillEditor(
            skill = Skill(), isNew = true, existing = state.skills, state = state,
            onSave = { s -> onCreate(s) { ok -> if (ok) creating = false } },
            onDelete = null,
            onBack = { creating = false }
        )
        else -> SkillList(state, onOpen = { selected = it }, onAdd = { creating = true })
    }
}

@Composable
private fun SkillList(state: CareerUiState, onOpen: (Skill) -> Unit, onAdd: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("all") }

    val present = remember(state.skills) { state.skills.map { it.category }.distinct() }
    // Known categories first in their usual order, anything unexpected after them
    val orderedKeys = remember(present) { categories.map { it.first }.filter { it in present } + present.filter { it !in categoryLabels } }

    val filtered = remember(state.skills, query, filter) {
        state.skills.filter { (filter == "all" || it.category == filter) && (query.isBlank() || it.name.contains(query, ignoreCase = true)) }
    }
    val groups = remember(filtered, orderedKeys) { orderedKeys.map { key -> key to filtered.filter { it.category == key } }.filter { it.second.isNotEmpty() } }

    ListFrame(
        addLabel = "Nueva habilidad",
        onAdd = onAdd,
        header = {
            SearchField(query, { query = it }, "Buscar habilidad")
            FilterRow(listOf("all" to "Todas · ${state.skills.size}") + orderedKeys.map { key -> key to "${categoryLabel(key)} · ${state.skills.count { it.category == key }}" }, filter) { filter = it }
        }
    ) {
        if (groups.isEmpty()) {
            item { Text("Ninguna habilidad coincide", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 24.dp)) }
        }
        items(groups, key = { it.first }) { (key, skills) ->
            Column {
                if (filter == "all") {
                    Text(
                        "${categoryLabel(key)} · ${skills.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 6.dp)
                    )
                }
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    skills.forEachIndexed { index, skill ->
                        if (index > 0) HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                        Row(
                            Modifier.fillMaxWidth().clickable { onOpen(skill) }.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SkillIcon(skill, 24)
                            Spacer(Modifier.width(16.dp))
                            Text(skill.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            LevelDots(skill.level)
                        }
                    }
                }
            }
        }
    }
}

/** The skill's icon from the API; a lettermark while it loads, when there is none, or if it fails. */
@Composable
private fun SkillIcon(skill: Skill, size: Int) {
    val letter: @Composable () -> Unit = {
        Box(
            Modifier.size(size.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(skill.name.take(1).uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
    val url = skill.icon_url
    if (url.isNullOrBlank()) {
        letter()
    } else {
        Box(Modifier.size(size.dp)) {
            letter()
            AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(size.dp))
        }
    }
}

@Composable
private fun LevelDots(level: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(4) { i ->
            Box(
                Modifier.size(8.dp).background(
                    if (i < level) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(2.dp)
                )
            )
        }
    }
}

@Composable
private fun SkillEditor(
    skill: Skill,
    isNew: Boolean,
    existing: List<Skill>,
    state: CareerUiState,
    onSave: (Skill) -> Unit,
    onDelete: (() -> Unit)?,
    onBack: () -> Unit
) {
    var template by remember { mutableStateOf<Skill?>(null) }
    val base = if (isNew) template ?: Skill() else skill

    var id by remember { mutableStateOf("") }
    var idEdited by remember { mutableStateOf(false) }

    var name     by remember(base) { mutableStateOf(base.name) }
    var category by remember(base) { mutableStateOf(base.category) }
    var level    by remember(base) { mutableStateOf(base.level) }
    var iconUrl  by remember(base) { mutableStateOf(base.icon_url ?: "") }

    val built = Skill(id = if (isNew) id else skill.id, name = name, category = category, level = level, icon_url = iconUrl.ifBlank { null })
    val dirty = if (isNew) name.isNotBlank() || iconUrl.isNotBlank() else built != skill
    val categoryOptions = categories + (if (category.isNotBlank() && category !in categoryLabels) listOf(category to category) else emptyList())

    EditorScaffold(
        title = if (isNew) "Nueva habilidad" else skill.name,
        isNew = isNew,
        dirty = dirty,
        canSave = name.isNotBlank() && category.isNotBlank() && (!isNew || id.isNotBlank()),
        saving = state.isLoading,
        saveLabel = if (isNew) "Crear habilidad" else "Guardar cambios",
        onSave = { onSave(built) },
        onBack = onBack,
        onDelete = onDelete,
        deleteName = skill.name.ifBlank { skill.id }
    ) {
        if (isNew) TemplatePicker(existing, template, { "${it.name} · ${categoryLabel(it.category)}" }, { template = it; idEdited = false })

        FieldGroup("Habilidad") {
            SectionField("Nombre", name, {
                name = it
                if (isNew && !idEdited) id = it.toSlug()
            }, required = true, singleLine = true)
            if (isNew) IdField(id) { id = it; idEdited = true }
            SelectField("Categoría", category, categoryOptions) { category = it }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (iconUrl.isNotBlank()) {
                    SkillIcon(Skill(name = name, icon_url = iconUrl), 32)
                    Spacer(Modifier.width(12.dp))
                }
                Box(Modifier.weight(1f)) { SectionField("URL del icono", iconUrl, { iconUrl = it }, singleLine = true) }
            }
        }

        FieldGroup("Nivel") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(SkillLevels.label(level), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                LevelDots(level)
            }
            Slider(value = level.toFloat(), onValueChange = { level = SkillLevels.coerce(it.toInt()) }, valueRange = SkillLevels.MIN.toFloat()..SkillLevels.MAX.toFloat(), steps = 2)
        }
    }
}
