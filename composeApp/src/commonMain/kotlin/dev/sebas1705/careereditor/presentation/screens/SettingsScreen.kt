package dev.sebas1705.careereditor.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.sebas1705.careereditor.data.api.DEFAULT_BASE_URL
import dev.sebas1705.careereditor.presentation.viewmodel.SettingsUiState

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onSave: (url: String, token: String) -> Unit,
    onLogout: () -> Unit,
    onClearMessage: () -> Unit
) {
    var newToken by remember { mutableStateOf("") }
    var showNewToken by remember { mutableStateOf(false) }
    var newUrl by remember(state.baseUrl) { mutableStateOf(state.baseUrl) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Ajustes", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        AnimatedVisibility(state.message != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Row(Modifier.padding(12.dp)) {
                    Text(
                        state.message ?: "",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onClearMessage, contentPadding = PaddingValues(0.dp)) {
                        Text("✕", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // API Config section
        SettingsSection(title = "Conexión a la API") {
            OutlinedTextField(
                value = newUrl,
                onValueChange = { newUrl = it },
                label = { Text("URL base de la API") },
                placeholder = { Text(DEFAULT_BASE_URL) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                supportingText = { Text("Por defecto: $DEFAULT_BASE_URL") }
            )

            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1f).padding(vertical = 4.dp)
                ) {
                    Row(Modifier.padding(12.dp)) {
                        Text(
                            "Token actual: ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            state.tokenMasked.ifBlank { "(sin token)" },
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = newToken,
                onValueChange = { newToken = it },
                label = { Text("Nuevo token (opcional)") },
                placeholder = { Text("Dejar vacío para no cambiar") },
                visualTransformation = if (showNewToken) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { showNewToken = !showNewToken }) {
                        Text(if (showNewToken) "Ocultar" else "Ver", style = MaterialTheme.typography.labelSmall)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { onSave(newUrl, newToken); newToken = "" },
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(8.dp))
            }
            Text("Guardar ajustes")
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        // Security section
        SettingsSection(title = "Seguridad") {
            Text(
                if (state.tokenEmbedded) "Esta versión lleva el token incluido (se gestiona en Doppler): no hace falta introducirlo. Si escribes uno nuevo arriba, tiene prioridad sobre el incluido. Nunca se transmite sin HTTPS."
                else "El token se almacena de forma cifrada (AES-256-GCM) en este dispositivo. Nunca se transmite sin HTTPS.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(16.dp))

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

        Spacer(Modifier.height(24.dp))

        Text(
            "Folio v1.2.0 · career-api.sebas1705.workers.dev",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(8.dp))
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}
