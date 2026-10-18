package dev.sebas1705.careereditor.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.sebas1705.careereditor.data.api.DEFAULT_BASE_URL
import dev.sebas1705.careereditor.presentation.components.FieldGroup
import dev.sebas1705.careereditor.presentation.viewmodel.SettingsUiState
import dev.sebas1705.careereditor.security.appVersionName

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onSave: (url: String, token: String) -> Unit,
    onLogout: () -> Unit
) {
    var newToken by remember { mutableStateOf("") }
    var showNewToken by remember { mutableStateOf(false) }
    var newUrl by remember(state.baseUrl) { mutableStateOf(state.baseUrl) }
    val changed = newUrl.trim() != state.baseUrl || newToken.isNotBlank()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FieldGroup("Conexión a la API") {
            OutlinedTextField(
                value = newUrl,
                onValueChange = { newUrl = it },
                label = { Text("URL base") },
                placeholder = { Text(DEFAULT_BASE_URL) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                supportingText = if (newUrl.trim() != DEFAULT_BASE_URL) ({ Text("Por defecto: $DEFAULT_BASE_URL") }) else null
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text("Token  ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (state.tokenEmbedded && state.tokenMasked.isBlank()) "incluido en la app" else state.tokenMasked.ifBlank { "sin token" },
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            OutlinedTextField(
                value = newToken,
                onValueChange = { newToken = it },
                label = { Text(if (state.tokenEmbedded) "Usar otro token (opcional)" else "Nuevo token (opcional)") },
                visualTransformation = if (showNewToken) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { showNewToken = !showNewToken }) {
                        Text(if (showNewToken) "Ocultar" else "Ver", style = MaterialTheme.typography.labelSmall)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                supportingText = {
                    Text(
                        if (state.tokenEmbedded) "Esta versión ya lleva el token (se gestiona en Doppler). Uno nuevo tendría prioridad."
                        else "Se guarda cifrado en este dispositivo.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            )
        }

        Button(
            onClick = { onSave(newUrl, newToken); newToken = "" },
            enabled = changed && !state.isSaving,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(8.dp))
            }
            Text("Guardar ajustes")
        }

        // A build with its own token has no session to close: it would sign straight back in
        if (!state.tokenEmbedded) {
            OutlinedButton(
                onClick = onLogout,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Cerrar sesión y eliminar token")
            }
        }

        Text(
            "Folio v$appVersionName · ${state.baseUrl.removePrefix("https://").removePrefix("http://")}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
