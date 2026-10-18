package dev.sebas1705.careereditor.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.sebas1705.careereditor.data.model.Language

/**
 * The app's own chrome (top bar and bottom navigation) hides while an editor is open: the editor
 * is a full-screen page with its own bar, so there is one header instead of two stacked ones.
 */
class ChromeState {
    var hiddenCount by mutableIntStateOf(0)
    val hidden: Boolean get() = hiddenCount > 0
}

val LocalChrome = staticCompositionLocalOf { ChromeState() }

/**
 * Full-screen editor: back arrow + title (+ delete) on top, a save bar that is always visible at
 * the bottom, and a scrolling body. System Back and the arrow ask before throwing away changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScaffold(
    title: String,
    isNew: Boolean,
    dirty: Boolean,
    canSave: Boolean,
    saving: Boolean,
    saveLabel: String,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onDelete: (() -> Unit)? = null,
    deleteName: String = "",
    content: @Composable ColumnScope.() -> Unit
) {
    val chrome = LocalChrome.current
    DisposableEffect(Unit) {
        chrome.hiddenCount++
        onDispose { chrome.hiddenCount-- }
    }

    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val requestBack = { if (dirty && !saving) confirmDiscard = true else onBack() }
    PlatformBackHandler(onBack = requestBack)

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("¿Descartar los cambios?") },
            text = { Text("Has modificado datos que aún no se han guardado.") },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; onBack() }) {
                    Text("Descartar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Seguir editando") } }
        )
    }
    if (confirmDelete && onDelete != null) {
        DeleteConfirmDialog(
            itemName = deleteName,
            onConfirm = { confirmDelete = false; onDelete() },
            onDismiss = { confirmDelete = false }
        )
    }

    // The outer scaffold already consumed the system bars, so none of the insets repeat here
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = requestBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
                actions = { if (onDelete != null) DeleteButton(onClick = { confirmDelete = true }, enabled = !saving) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = onSave,
                    // Editing: nothing to save until something changed. Creating: as soon as it is valid.
                    enabled = canSave && !saving && (isNew || dirty),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(48.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (saving) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Guardando…")
                    } else {
                        Text(saveLabel)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

/** A titled card of fields; [trailing] sits on the title row (e.g. the language switch). */
@Composable
fun FieldGroup(title: String, trailing: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            trailing?.invoke()
        }
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), content = content)
        }
    }
}

/** Compact EN | ES switch. Renders nothing when there is a single language. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LangSwitch(languages: List<Language>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    if (languages.size < 2) return
    SingleChoiceSegmentedButtonRow(modifier) {
        languages.forEachIndexed { index, lang ->
            SegmentedButton(
                selected = lang.code == selected,
                onClick = { onSelect(lang.code) },
                shape = SegmentedButtonDefaults.itemShape(index, languages.size),
                icon = {},
                modifier = Modifier.height(36.dp),
                label = { Text(lang.code.uppercase(), style = MaterialTheme.typography.labelMedium) }
            )
        }
    }
}

/** Dropdown over a fixed set of values: [options] are value to label. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectField(label: String, value: String, options: List<Pair<String, String>>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = options.firstOrNull { it.first == value }?.second ?: value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).menuAnchor(),
            shape = MaterialTheme.shapes.medium
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (v, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(v); expanded = false })
            }
        }
    }
}

/** "Copy from…" for a new item: one dropdown, no extra card around it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> TemplatePicker(items: List<T>, selected: T?, itemLabel: (T) -> String, onSelect: (T?) -> Unit) {
    if (items.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.let(itemLabel) ?: "Empezar en blanco",
            onValueChange = {},
            readOnly = true,
            label = { Text("Copiar datos de… (opcional)") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            shape = MaterialTheme.shapes.medium
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Empezar en blanco") }, onClick = { onSelect(null); expanded = false })
            items.forEach { item -> DropdownMenuItem(text = { Text(itemLabel(item)) }, onClick = { onSelect(item); expanded = false }) }
        }
    }
}

/** Sets or clears one language of a localized map; an emptied field removes its key. */
fun Map<String, String>.with(lang: String, value: String): Map<String, String> =
    if (value.isBlank()) this - lang else this + (lang to value)

/** Same for a localized list. */
fun Map<String, List<String>>.withList(lang: String, value: List<String>): Map<String, List<String>> =
    if (value.isEmpty()) this - lang else this + (lang to value)
