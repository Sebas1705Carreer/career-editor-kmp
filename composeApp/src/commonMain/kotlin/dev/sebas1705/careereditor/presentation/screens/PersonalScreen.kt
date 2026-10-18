package dev.sebas1705.careereditor.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.sebas1705.careereditor.data.model.Personal
import dev.sebas1705.careereditor.presentation.components.*
import dev.sebas1705.careereditor.presentation.viewmodel.CareerUiState

@Composable
fun PersonalScreen(
    state: CareerUiState,
    lang: String,
    onSelectLang: (String) -> Unit,
    onSave: (Personal, (Boolean) -> Unit) -> Unit
) {
    val p = state.personal
    var name     by remember(p) { mutableStateOf(p.name) }
    var email    by remember(p) { mutableStateOf(p.email) }
    var cvUrl    by remember(p) { mutableStateOf(p.cvUrl) }
    var github   by remember(p) { mutableStateOf(p.github) }
    var linkedin by remember(p) { mutableStateOf(p.linkedin) }
    var codewars by remember(p) { mutableStateOf(p.codewars) }

    // Edits to every language accumulate, so switching EN/ES never loses what was typed
    var greeting by remember(p) { mutableStateOf(p.greeting) }
    var role     by remember(p) { mutableStateOf(p.role) }
    var tagline  by remember(p) { mutableStateOf(p.tagline) }
    var bio      by remember(p) { mutableStateOf(p.bio) }
    var location by remember(p) { mutableStateOf(p.location) }

    val built = p.copy(
        name = name, email = email, cvUrl = cvUrl, github = github, linkedin = linkedin, codewars = codewars,
        greeting = greeting, role = role, tagline = tagline, bio = bio, location = location
    )
    val dirty = built != p
    var saving by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FieldGroup("Presentación", trailing = { LangSwitch(state.languages.supported, lang, onSelectLang) }) {
                SectionField("Saludo", greeting[lang] ?: "", { greeting = greeting.with(lang, it) }, singleLine = true)
                SectionField("Rol", role[lang] ?: "", { role = role.with(lang, it) }, singleLine = true)
                SectionField("Tagline", tagline[lang] ?: "", { tagline = tagline.with(lang, it) }, minLines = 2)
                SectionField("Bio", bio[lang] ?: "", { bio = bio.with(lang, it) }, minLines = 3)
                SectionField("Ubicación", location[lang] ?: "", { location = location.with(lang, it) }, singleLine = true)
            }

            FieldGroup("Identidad") {
                SectionField("Nombre completo", name, { name = it }, required = true, singleLine = true)
                SectionField("Email", email, { email = it }, required = true, singleLine = true)
            }

            FieldGroup("Enlaces") {
                SectionField("URL del CV", cvUrl, { cvUrl = it }, singleLine = true)
                SectionField("GitHub", github, { github = it }, singleLine = true)
                SectionField("LinkedIn", linkedin, { linkedin = it }, singleLine = true)
                SectionField("Codewars", codewars, { codewars = it }, singleLine = true)
            }
        }

        // Always in reach: the form is long, the button is not at its end
        Surface(tonalElevation = 3.dp) {
            Button(
                onClick = { saving = true; onSave(built) { saving = false } },
                enabled = dirty && !saving && name.isNotBlank() && email.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(48.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                if (saving) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("Guardando…")
                } else {
                    Text(if (dirty) "Guardar cambios" else "Sin cambios")
                }
            }
        }
    }
}
