package dev.sebas1705.careereditor.presentation.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.sebas1705.careereditor.presentation.viewmodel.CareerUiState

private class SectionTile(val label: String, val count: Int, val route: String, val icon: ImageVector)

@Composable
fun DashboardScreen(state: CareerUiState, apiHost: String, onNavigate: (String) -> Unit, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Published profile: tapping it opens the personal editor ──────────
        if (state.personal.name.isNotBlank()) {
            Surface(
                onClick = { onNavigate("personal") },
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(state.personal.name.take(2).uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(state.personal.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            state.personal.role.values.firstOrNull() ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            state.personal.location.values.firstOrNull() ?: "",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Icon(Icons.Default.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        } else if (state.isLoading) {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth().height(88.dp)) {}
        }

        // ── Sections: every one of them, as buttons that look like buttons ───
        Text("Secciones", style = MaterialTheme.typography.titleMedium)
        val tiles = listOf(
            SectionTile("Experiencia", state.jobs.size, "jobs", Icons.Default.Build),
            SectionTile("Proyectos", state.projects.size, "projects", Icons.Default.Edit),
            SectionTile("Skills", state.skills.size, "skills", Icons.Default.Star),
            SectionTile("Educación", state.education.size, "education", Icons.Default.DateRange),
            SectionTile("Certificaciones", state.certifications.size, "certifications", Icons.Default.Done),
            SectionTile("Soft skills", state.softSkills.size, "soft-skills", Icons.Default.Favorite)
        )
        tiles.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { tile -> TileCard(tile, state.isLoading && tile.count == 0, { onNavigate(tile.route) }, Modifier.weight(1f)) }
            }
        }

        ApiStatusStrip(isLoading = state.isLoading, failed = state.loadFailed, host = apiHost, onRetry = onRetry)
    }
}

@Composable
private fun TileCard(tile: SectionTile, loading: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(tile.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(8.dp))
            if (loading) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text(tile.count.toString(), style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.primary)
            }
            Text(tile.label, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Connection state, taken from the last real load: not a hardcoded "ok". */
@Composable
private fun ApiStatusStrip(isLoading: Boolean, failed: Boolean, host: String, onRetry: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "api-pulse").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "api-pulse-alpha"
    )
    val color = when {
        failed -> MaterialTheme.colorScheme.error
        isLoading -> MaterialTheme.colorScheme.tertiary.copy(alpha = pulse)
        else -> MaterialTheme.colorScheme.secondary
    }
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, end = if (failed) 4.dp else 16.dp).heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Spacer(Modifier.width(10.dp))
            Text(
                when {
                    failed -> "sin conexión · $host"
                    isLoading -> "conectando · $host"
                    else -> "conectado · $host"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (failed) TextButton(onClick = onRetry) { Text("Reintentar") }
        }
    }
}
