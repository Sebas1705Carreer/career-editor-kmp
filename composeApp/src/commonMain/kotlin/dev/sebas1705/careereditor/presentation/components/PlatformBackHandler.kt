package dev.sebas1705.careereditor.presentation.components

import androidx.compose.runtime.Composable

/** System Back. The innermost enabled handler wins, so an editor can intercept it before the section does. */
@Composable
expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)
