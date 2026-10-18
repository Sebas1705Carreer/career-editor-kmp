package dev.sebas1705.careereditor.presentation.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.sebas1705.careereditor.data.model.Education
import dev.sebas1705.careereditor.data.model.resolve
import dev.sebas1705.careereditor.presentation.components.*
import dev.sebas1705.careereditor.presentation.viewmodel.CareerUiState
import dev.sebas1705.careereditor.utils.toSlug

/** The API stores a key; the portfolio turns it into an emoji, and so does this screen. */
private val iconOptions = listOf(
    "graduation" to "🎓 Posgrado",
    "master" to "🎓 Máster",
    "university" to "🎓 Universidad",
    "school" to "🏫 Instituto"
)
private val iconEmoji = mapOf("graduation" to "🎓", "master" to "🎓", "university" to "🎓", "school" to "🏫")
private fun emojiFor(icon: String) = iconEmoji[icon] ?: icon.ifBlank { "🎓" }

@Composable
fun EducationScreen(
    state: CareerUiState,
    lang: String,
    onSelectLang: (String) -> Unit,
    onSave: (Education, (Boolean) -> Unit) -> Unit,
    onCreate: (Education, (Boolean) -> Unit) -> Unit,
    onDelete: (String, (Boolean) -> Unit) -> Unit
) {
    var selected by remember { mutableStateOf<Education?>(null) }
    var creating by remember { mutableStateOf(false) }
    val editing = selected

    when {
        editing != null -> EducationEditor(
            education = editing, isNew = false, existing = state.education, state = state, lang = lang, onLang = onSelectLang,
            onSave = { e -> onSave(e) { ok -> if (ok) selected = null } },
            onDelete = { onDelete(editing.id) { ok -> if (ok) selected = null } },
            onBack = { selected = null }
        )
        creating -> EducationEditor(
            education = Education(), isNew = true, existing = state.education, state = state, lang = lang, onLang = onSelectLang,
            onSave = { e -> onCreate(e) { ok -> if (ok) creating = false } },
            onDelete = null,
            onBack = { creating = false }
        )
        else -> ListFrame(
            addLabel = "Nueva formación",
            onAdd = { creating = true },
            header = {
                ListHeader("${state.education.size} entradas", trailing = { LangSwitch(state.languages.supported, lang, onSelectLang) })
            }
        ) {
            items(state.education, key = { it.id }) { edu ->
                ItemCard(onClick = { selected = edu }) {
                    Row {
                        Text(emojiFor(edu.icon), style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.width(12.dp))
                        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                            Text(edu.degree.resolve(lang), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(edu.school, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(edu.period.resolve(lang), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EducationEditor(
    education: Education,
    isNew: Boolean,
    existing: List<Education>,
    state: CareerUiState,
    lang: String,
    onLang: (String) -> Unit,
    onSave: (Education) -> Unit,
    onDelete: (() -> Unit)?,
    onBack: () -> Unit
) {
    var template by remember { mutableStateOf<Education?>(null) }
    val base = if (isNew) template ?: Education() else education

    var id by remember { mutableStateOf("") }
    var idEdited by remember { mutableStateOf(false) }

    var icon     by remember(base) { mutableStateOf(base.icon.ifBlank { "graduation" }) }
    var school   by remember(base) { mutableStateOf(base.school) }
    var degree   by remember(base) { mutableStateOf(base.degree) }
    var period   by remember(base) { mutableStateOf(base.period) }
    var detail   by remember(base) { mutableStateOf(base.detail) }
    var longDesc by remember(base) { mutableStateOf(base.longDesc) }

    val built = Education(
        id = if (isNew) id else education.id, school = school, icon = icon,
        degree = degree, period = period, detail = detail, longDesc = longDesc
    )
    val dirty = if (isNew) school.isNotBlank() || degree.isNotEmpty() else built != education
    val options = iconOptions + (if (icon !in iconEmoji) listOf(icon to emojiFor(icon)) else emptyList())

    EditorScaffold(
        title = if (isNew) "Nueva formación" else school.ifBlank { education.id },
        isNew = isNew,
        dirty = dirty,
        canSave = school.isNotBlank() && (!isNew || id.isNotBlank()),
        saving = state.isLoading,
        saveLabel = if (isNew) "Crear formación" else "Guardar cambios",
        onSave = { onSave(built) },
        onBack = onBack,
        onDelete = onDelete,
        deleteName = school.ifBlank { education.id }
    ) {
        if (isNew) TemplatePicker(existing, template, { it.school }, { template = it; idEdited = false })

        FieldGroup("Centro") {
            SectionField("Centro educativo", school, {
                school = it
                if (isNew && !idEdited) id = it.toSlug()
            }, required = true, singleLine = true)
            if (isNew) IdField(id) { id = it; idEdited = true }
            SelectField("Icono", icon, options) { icon = it }
        }

        FieldGroup("Titulación", trailing = { LangSwitch(state.languages.supported, lang, onLang) }) {
            SectionField("Titulación", degree[lang] ?: "", { degree = degree.with(lang, it) }, singleLine = true)
            SectionField("Periodo", period[lang] ?: "", { period = period.with(lang, it) }, singleLine = true)
            SectionField("Detalle", detail[lang] ?: "", { detail = detail.with(lang, it) }, minLines = 2)
            SectionField("Descripción larga", longDesc[lang] ?: "", { longDesc = longDesc.with(lang, it) }, minLines = 4)
        }
    }
}
