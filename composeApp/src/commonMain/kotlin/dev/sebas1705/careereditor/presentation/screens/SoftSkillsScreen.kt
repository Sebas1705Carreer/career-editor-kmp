package dev.sebas1705.careereditor.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.sebas1705.careereditor.data.model.SoftSkill
import dev.sebas1705.careereditor.data.model.resolve
import dev.sebas1705.careereditor.presentation.components.*
import dev.sebas1705.careereditor.presentation.viewmodel.CareerUiState
import dev.sebas1705.careereditor.utils.toSlug

@Composable
fun SoftSkillsScreen(
    state: CareerUiState,
    lang: String,
    onSelectLang: (String) -> Unit,
    onSave: (SoftSkill, (Boolean) -> Unit) -> Unit,
    onCreate: (SoftSkill, (Boolean) -> Unit) -> Unit,
    onDelete: (String, (Boolean) -> Unit) -> Unit
) {
    var selected by remember { mutableStateOf<SoftSkill?>(null) }
    var creating by remember { mutableStateOf(false) }
    val editing = selected

    when {
        editing != null -> SoftSkillEditor(
            skill = editing, isNew = false, existing = state.softSkills, state = state, lang = lang, onLang = onSelectLang,
            onSave = { s -> onSave(s) { ok -> if (ok) selected = null } },
            onDelete = { onDelete(editing.id) { ok -> if (ok) selected = null } },
            onBack = { selected = null }
        )
        creating -> SoftSkillEditor(
            skill = SoftSkill(), isNew = true, existing = state.softSkills, state = state, lang = lang, onLang = onSelectLang,
            onSave = { s -> onCreate(s) { ok -> if (ok) creating = false } },
            onDelete = null,
            onBack = { creating = false }
        )
        else -> ListFrame(
            addLabel = "Nueva soft skill",
            onAdd = { creating = true },
            header = {
                ListHeader("${state.softSkills.size} habilidades", trailing = { LangSwitch(state.languages.supported, lang, onSelectLang) })
            }
        ) {
            // They are single words: one card with rows beats twelve cards
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    state.softSkills.forEachIndexed { index, skill ->
                        if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                        Row(
                            Modifier.fillMaxWidth().clickable { selected = skill }.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(skill.name.resolve(lang).ifBlank { skill.id }, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SoftSkillEditor(
    skill: SoftSkill,
    isNew: Boolean,
    existing: List<SoftSkill>,
    state: CareerUiState,
    lang: String,
    onLang: (String) -> Unit,
    onSave: (SoftSkill) -> Unit,
    onDelete: (() -> Unit)?,
    onBack: () -> Unit
) {
    var template by remember { mutableStateOf<SoftSkill?>(null) }
    val base = if (isNew) template ?: SoftSkill() else skill

    var id by remember { mutableStateOf("") }
    var idEdited by remember { mutableStateOf(false) }
    var name by remember(base) { mutableStateOf(base.name) }

    val built = SoftSkill(id = if (isNew) id else skill.id, name = name)
    val dirty = if (isNew) name.isNotEmpty() else built != skill

    EditorScaffold(
        title = if (isNew) "Nueva soft skill" else skill.name.resolve(lang).ifBlank { skill.id },
        isNew = isNew,
        dirty = dirty,
        canSave = name.values.any { it.isNotBlank() } && (!isNew || id.isNotBlank()),
        saving = state.isLoading,
        saveLabel = if (isNew) "Crear soft skill" else "Guardar cambios",
        onSave = { onSave(built) },
        onBack = onBack,
        onDelete = onDelete,
        deleteName = skill.name.resolve(lang).ifBlank { skill.id }
    ) {
        if (isNew) TemplatePicker(existing, template, { it.name.resolve("en") }, { template = it; idEdited = false })

        FieldGroup("Nombre", trailing = { LangSwitch(state.languages.supported, lang, onLang) }) {
            SectionField("Nombre", name[lang] ?: "", {
                name = name.with(lang, it)
                if (isNew && !idEdited && lang == state.languages.default) id = it.toSlug()
            }, required = true, singleLine = true)
            if (isNew) IdField(id) { id = it; idEdited = true }
        }
    }
}
