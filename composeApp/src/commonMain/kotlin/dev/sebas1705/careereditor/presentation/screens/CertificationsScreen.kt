package dev.sebas1705.careereditor.presentation.screens

import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.text.style.TextOverflow
import dev.sebas1705.careereditor.data.model.Certification
import dev.sebas1705.careereditor.presentation.components.*
import dev.sebas1705.careereditor.presentation.viewmodel.CareerUiState
import dev.sebas1705.careereditor.utils.toSlug

@Composable
fun CertificationsScreen(
    state: CareerUiState,
    lang: String,
    onSelectLang: (String) -> Unit,
    onSave: (Certification, (Boolean) -> Unit) -> Unit,
    onCreate: (Certification, (Boolean) -> Unit) -> Unit,
    onDelete: (String, (Boolean) -> Unit) -> Unit
) {
    var selected by remember { mutableStateOf<Certification?>(null) }
    var creating by remember { mutableStateOf(false) }
    val editing = selected

    when {
        editing != null -> CertificationEditor(
            cert = editing, isNew = false, existing = state.certifications, state = state, lang = lang, onLang = onSelectLang,
            onSave = { c -> onSave(c) { ok -> if (ok) selected = null } },
            onDelete = { onDelete(editing.id) { ok -> if (ok) selected = null } },
            onBack = { selected = null }
        )
        creating -> CertificationEditor(
            cert = Certification(), isNew = true, existing = state.certifications, state = state, lang = lang, onLang = onSelectLang,
            onSave = { c -> onCreate(c) { ok -> if (ok) creating = false } },
            onDelete = null,
            onBack = { creating = false }
        )
        else -> ListFrame(
            addLabel = "Nueva certificación",
            onAdd = { creating = true },
            header = { ListHeader("${state.certifications.size} certificaciones") }
        ) {
            items(state.certifications, key = { it.id }) { cert ->
                ItemCard(onClick = { selected = cert }) {
                    Text(cert.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOf(cert.issuer, cert.date).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun CertificationEditor(
    cert: Certification,
    isNew: Boolean,
    existing: List<Certification>,
    state: CareerUiState,
    lang: String,
    onLang: (String) -> Unit,
    onSave: (Certification) -> Unit,
    onDelete: (() -> Unit)?,
    onBack: () -> Unit
) {
    var template by remember { mutableStateOf<Certification?>(null) }
    val base = if (isNew) template ?: Certification() else cert

    var id by remember { mutableStateOf("") }
    var idEdited by remember { mutableStateOf(false) }

    var name     by remember(base) { mutableStateOf(base.name) }
    var issuer   by remember(base) { mutableStateOf(base.issuer) }
    var date     by remember(base) { mutableStateOf(base.date) }
    var url      by remember(base) { mutableStateOf(base.url ?: "") }
    var longDesc by remember(base) { mutableStateOf(base.longDesc) }

    val built = Certification(
        id = if (isNew) id else cert.id, name = name, issuer = issuer, date = date,
        url = url.ifBlank { null }, longDesc = longDesc
    )
    val dirty = if (isNew) name.isNotBlank() || issuer.isNotBlank() else built != cert

    EditorScaffold(
        title = if (isNew) "Nueva certificación" else cert.name.ifBlank { cert.id },
        isNew = isNew,
        dirty = dirty,
        canSave = name.isNotBlank() && issuer.isNotBlank() && (!isNew || id.isNotBlank()),
        saving = state.isLoading,
        saveLabel = if (isNew) "Crear certificación" else "Guardar cambios",
        onSave = { onSave(built) },
        onBack = onBack,
        onDelete = onDelete,
        deleteName = cert.name.ifBlank { cert.id }
    ) {
        if (isNew) TemplatePicker(existing, template, { "${it.name} · ${it.issuer}" }, { template = it; idEdited = false })

        FieldGroup("Certificado") {
            SectionField("Nombre", name, {
                name = it
                if (isNew && !idEdited) id = it.toSlug()
            }, required = true, singleLine = true)
            if (isNew) IdField(id) { id = it; idEdited = true }
            SectionField("Emisor", issuer, { issuer = it }, required = true, singleLine = true)
            SectionField("Fecha", date, { date = it }, singleLine = true, supportingText = "Ej: Dec 2025, o In progress")
            SectionField("URL del certificado", url, { url = it }, singleLine = true)
        }

        FieldGroup("Descripción larga", trailing = { LangSwitch(state.languages.supported, lang, onLang) }) {
            SectionField("Descripción", longDesc[lang] ?: "", { longDesc = longDesc.with(lang, it) }, minLines = 4)
        }
    }
}
