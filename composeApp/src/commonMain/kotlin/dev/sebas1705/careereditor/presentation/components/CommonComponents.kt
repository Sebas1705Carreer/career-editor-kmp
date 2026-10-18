package dev.sebas1705.careereditor.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sebas1705.careereditor.data.model.Language
import dev.sebas1705.careereditor.utils.toSlug

@Composable
fun SectionField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = false,
    required: Boolean = false,
    supportingText: String? = null,
    minLines: Int = 1
) {
    // A new, empty form should not open already covered in red: errors wait until the field was visited
    var visited by remember { mutableStateOf(false) }
    var hadFocus by remember { mutableStateOf(false) }
    val isError = required && value.isBlank() && visited
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(if (required) "$label *" else label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).onFocusChanged {
            if (it.isFocused) hadFocus = true else if (hadFocus) visited = true
        },
        singleLine = singleLine,
        minLines = minLines,
        maxLines = if (singleLine) 1 else maxOf(5, minLines),
        isError = isError,
        supportingText = when {
            isError -> ({ Text("Campo obligatorio", color = MaterialTheme.colorScheme.error) })
            supportingText != null -> ({ Text(supportingText, style = MaterialTheme.typography.bodySmall) })
            else -> null
        },
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(imeAction = if (singleLine) ImeAction.Next else ImeAction.Default)
    )
}

@Composable
fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("Cargando datos...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Lettermark de Folio dibujado en Compose: cuadrado índigo redondeado al 18%,
 * "F" en blanco y punto teal — el mismo diseño que el icono de la app.
 */
@Composable
fun FolioMark(size: Dp = 56.dp) {
    Box(
        Modifier
            .size(size)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(size * 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "F",
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = (size.value * 0.46f).sp,
            fontWeight = FontWeight.Bold
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(size * 0.14f)
                .size(size * 0.14f)
                .background(MaterialTheme.colorScheme.secondary, CircleShape)
        )
    }
}

// ── Dynamic list field (add/remove chips) ─────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DynamicListField(
    label: String,
    items: List<String>,
    onItemsChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Añadir elemento"
) {
    var input by remember { mutableStateOf("") }

    fun addItem() {
        val v = input.trim()
        if (v.isNotBlank() && v !in items) { onItemsChange(items + v); input = "" }
    }

    FieldGroup(label) {
        Column(modifier) {
            if (items.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items.forEach { item ->
                        InputChip(
                            selected = false,
                            onClick = { onItemsChange(items - item) },
                            label = { Text(item, style = MaterialTheme.typography.labelSmall) },
                            trailingIcon = { Icon(Icons.Default.Close, "Quitar", modifier = Modifier.size(14.dp)) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { addItem() })
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(onClick = ::addItem, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Add, "Añadir")
                }
            }
        }
    }
}

// ── Id field ─────────────────────────────────────────────────────────────────

@Composable
fun IdField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.lowercase().replace(Regex("[^a-z0-9-]"), "")) },
        label = { Text("ID (slug) *") },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        supportingText = { Text("Solo letras minúsculas, números y guiones", style = MaterialTheme.typography.bodySmall) }
    )
}

// ── Delete confirmation ────────────────────────────────────────────────────────

@Composable
fun DeleteButton(onClick: () -> Unit, enabled: Boolean = true) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error)
    }
}

@Composable
fun DeleteConfirmDialog(itemName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eliminar") },
        text = { Text("¿Eliminar \"$itemName\"? Esta acción no se puede deshacer.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Eliminar", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
