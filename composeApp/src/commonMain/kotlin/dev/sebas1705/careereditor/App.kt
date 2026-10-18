package dev.sebas1705.careereditor

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.sebas1705.careereditor.presentation.components.ChromeState
import dev.sebas1705.careereditor.presentation.components.LoadingScreen
import dev.sebas1705.careereditor.presentation.components.LocalChrome
import dev.sebas1705.careereditor.presentation.components.PlatformBackHandler
import dev.sebas1705.careereditor.presentation.screens.*
import dev.sebas1705.careereditor.presentation.viewmodel.AuthState
import dev.sebas1705.careereditor.presentation.viewmodel.CareerViewModel
import dev.sebas1705.careereditor.ui.theme.CareerTheme

private enum class Section(
    val label: String,
    val icon: ImageVector,
    val route: String
) {
    DASHBOARD("Inicio", Icons.Default.Home, "dashboard"),
    PERSONAL("Personal", Icons.Default.Person, "personal"),
    JOBS("Experiencia", Icons.Default.Build, "jobs"),
    PROJECTS("Proyectos", Icons.Default.Edit, "projects"),
    SKILLS("Skills", Icons.Default.Star, "skills"),
    EDUCATION("Educación", Icons.Default.DateRange, "education"),
    CERTIFICATIONS("Certificaciones", Icons.Default.Done, "certifications"),
    SOFT_SKILLS("Soft skills", Icons.Default.Favorite, "soft-skills"),
    SETTINGS("Ajustes", Icons.Default.Settings, "settings")
}

private val bottomNavItems = listOf(
    Section.DASHBOARD,
    Section.PERSONAL,
    Section.PROJECTS,
    Section.SKILLS,
    Section.SETTINGS
)

/** Reached from the dashboard rather than from a tab: they show a back arrow and light up "Inicio". */
private val dashboardChildren = setOf(Section.JOBS, Section.EDUCATION, Section.CERTIFICATIONS, Section.SOFT_SKILLS)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    CareerTheme {
        val viewModel: CareerViewModel = viewModel { CareerViewModel() }
        val authState by viewModel.authState.collectAsState()
        val state by viewModel.uiState.collectAsState()
        val settingsState by viewModel.settingsState.collectAsState()
        val loginLoading by viewModel.loginLoading.collectAsState()
        val loginError by viewModel.loginError.collectAsState()
        val selectedLangCode by viewModel.selectedLangCode.collectAsState()

        AnimatedContent(
            targetState = authState,
            transitionSpec = { fadeIn() togetherWith fadeOut() }
        ) { auth ->
            when (auth) {
                AuthState.Checking -> LoadingScreen()

                AuthState.Unauthenticated -> LoginScreen(
                    isLoading = loginLoading,
                    error = loginError,
                    onLogin = viewModel::login,
                    onAnonymous = viewModel::loginAnonymous,
                    onClearError = viewModel::clearLoginError
                )

                AuthState.Authenticated -> MainScaffold(
                    viewModel = viewModel,
                    state = state,
                    settingsState = settingsState,
                    selectedLangCode = selectedLangCode
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(
    viewModel: CareerViewModel,
    state: dev.sebas1705.careereditor.presentation.viewmodel.CareerUiState,
    settingsState: dev.sebas1705.careereditor.presentation.viewmodel.SettingsUiState,
    selectedLangCode: String
) {
    var currentSection by remember { mutableStateOf(Section.DASHBOARD) }
    val chrome = remember { ChromeState() }
    val snackbar = remember { SnackbarHostState() }

    // Back from any section returns to the dashboard; from the dashboard it leaves the app.
    // Editors register their own handler deeper in the tree, which takes priority.
    PlatformBackHandler(enabled = currentSection != Section.DASHBOARD) { currentSection = Section.DASHBOARD }

    // Feedback is a transient message, not a banner that pushes the content down
    LaunchedEffect(state.saveSuccess) { if (state.saveSuccess) snackbar.showSnackbar("Guardado", duration = SnackbarDuration.Short) }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it, actionLabel = "Cerrar", duration = SnackbarDuration.Long)
            viewModel.clearError()
        }
    }
    LaunchedEffect(settingsState.message) {
        settingsState.message?.let {
            snackbar.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearSettingsMessage()
        }
    }

    CompositionLocalProvider(LocalChrome provides chrome) {
        Scaffold(
            // While an editor is open its save bar sits at the bottom: keep the message above it
            snackbarHost = { SnackbarHost(snackbar, Modifier.padding(bottom = if (chrome.hidden) 72.dp else 0.dp)) },
            topBar = {
                if (!chrome.hidden) {
                    TopAppBar(
                        title = { Text(currentSection.label) },
                        navigationIcon = {
                            if (currentSection in dashboardChildren) {
                                IconButton(onClick = { currentSection = Section.DASHBOARD }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
                            }
                        },
                        actions = {
                            if (currentSection != Section.SETTINGS) {
                                IconButton(onClick = { viewModel.loadAll() }) { Icon(Icons.Default.Refresh, "Recargar") }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                }
            },
            bottomBar = {
                if (!chrome.hidden) {
                    NavigationBar {
                        bottomNavItems.forEach { section ->
                            NavigationBarItem(
                                selected = currentSection == section || (section == Section.DASHBOARD && currentSection in dashboardChildren),
                                onClick = {
                                    currentSection = section
                                    viewModel.clearSaveSuccess()
                                    viewModel.clearError()
                                },
                                icon = { Icon(section.icon, section.label) },
                                // Explicit sans style: the theme's labelMedium is mono (wider) and with it
                                // labels such as "Proyectos" wrapped onto two lines.
                                label = {
                                    Text(
                                        section.label,
                                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            // consumeWindowInsets: the keyboard then pushes the content up by what is missing
            // (keyboard minus the bottom bar) instead of adding to it, which left a dead strip.
            Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
                AnimatedContent(
                    targetState = currentSection,
                    transitionSpec = {
                        slideInHorizontally { it / 4 } + fadeIn() togetherWith
                                slideOutHorizontally { -it / 4 } + fadeOut()
                    }
                ) { section ->
                    when (section) {
                        Section.DASHBOARD -> DashboardScreen(
                            state = state,
                            apiHost = settingsState.baseUrl.removePrefix("https://").removePrefix("http://"),
                            onNavigate = { route -> Section.entries.find { it.route == route }?.let { currentSection = it } },
                            onRetry = viewModel::loadAll
                        )
                        Section.PERSONAL -> PersonalScreen(
                            state = state,
                            lang = selectedLangCode,
                            onSelectLang = viewModel::selectLanguage,
                            onSave = viewModel::savePersonal
                        )
                        Section.JOBS -> JobsScreen(
                            state = state,
                            lang = selectedLangCode,
                            onSelectLang = viewModel::selectLanguage,
                            onSave = viewModel::saveJob,
                            onCreate = viewModel::createJob,
                            onDelete = viewModel::deleteJob
                        )
                        Section.PROJECTS -> ProjectsScreen(
                            state = state,
                            lang = selectedLangCode,
                            onSelectLang = viewModel::selectLanguage,
                            onSave = viewModel::saveProject,
                            onCreate = viewModel::createProject,
                            onDelete = viewModel::deleteProject
                        )
                        Section.SKILLS -> SkillsScreen(
                            state = state,
                            onSave = viewModel::saveSkill,
                            onCreate = viewModel::createSkill,
                            onDelete = viewModel::deleteSkill
                        )
                        Section.EDUCATION -> EducationScreen(
                            state = state,
                            lang = selectedLangCode,
                            onSelectLang = viewModel::selectLanguage,
                            onSave = viewModel::saveEducation,
                            onCreate = viewModel::createEducation,
                            onDelete = viewModel::deleteEducation
                        )
                        Section.CERTIFICATIONS -> CertificationsScreen(
                            state = state,
                            lang = selectedLangCode,
                            onSelectLang = viewModel::selectLanguage,
                            onSave = viewModel::saveCertification,
                            onCreate = viewModel::createCertification,
                            onDelete = viewModel::deleteCertification
                        )
                        Section.SOFT_SKILLS -> SoftSkillsScreen(
                            state = state,
                            lang = selectedLangCode,
                            onSelectLang = viewModel::selectLanguage,
                            onSave = viewModel::saveSoftSkill,
                            onCreate = viewModel::createSoftSkill,
                            onDelete = viewModel::deleteSoftSkill
                        )
                        Section.SETTINGS -> SettingsScreen(
                            state = settingsState,
                            onSave = viewModel::saveSettings,
                            onLogout = viewModel::logout
                        )
                    }
                }
            }
        }
    }
}
