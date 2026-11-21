package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.SurveyDatabase
import com.example.data.local.SurveyPreferences
import com.example.data.local.SurveyRepository
import com.example.model.SurveyToolId
import com.example.ui.screens.*
import com.example.ui.theme.HowladerSurveyTheme
import com.example.ui.viewmodel.SurveyViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val db = SurveyDatabase.getInstance(applicationContext)
        val prefs = SurveyPreferences(applicationContext)
        val repo = SurveyRepository(db.surveyDao(), prefs)

        setContent {
            HowladerSurveyTheme {
                val vm: SurveyViewModel = viewModel(factory = SurveyViewModel.Factory(repo))
                HowladerSurveyApp(viewModel = vm)
            }
        }
    }
}

private object Routes {
    const val HOME = "home"
    const val PROJECTS = "projects"
    const val ACCOUNT = "account"
    const val TOOL = "tool/{toolId}"
    fun toolRoute(tool: SurveyToolId) = "tool/${tool.id}"
}

@Composable
fun HowladerSurveyApp(viewModel: SurveyViewModel) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val khatians by viewModel.khatians.collectAsStateWithLifecycle()
    val csBsRecords by viewModel.csBsRecords.collectAsStateWithLifecycle()
    val activeDraftPoints by viewModel.activeDraftPoints.collectAsStateWithLifecycle()

    var pinUnlocked by remember { mutableStateOf(false) }

    if (prefs.pinLockEnabled && prefs.pinCode.length == 4 && !pinUnlocked) {
        PinLockScreen(
            expectedPin = prefs.pinCode,
            isBengali = prefs.isBengali,
            onUnlocked = { pinUnlocked = true },
        )
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in setOf(Routes.HOME, Routes.PROJECTS, Routes.ACCOUNT)

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.uiMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val isBn = prefs.isBengali
    val surveyorFooter = "${prefs.surveyorName} (${prefs.surveyorTitle})"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                ) {
                    NavigationBarItem(
                        selected = currentRoute == Routes.HOME,
                        onClick = {
                            navController.navigate(Routes.HOME) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Rounded.GridView, contentDescription = null) },
                        label = { Text(if (isBn) "টুলস" else "Tools") },
                        modifier = Modifier.testTag("nav_home"),
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.PROJECTS,
                        onClick = {
                            navController.navigate(Routes.PROJECTS) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Rounded.FolderCopy, contentDescription = null) },
                        label = { Text("Projects") },
                        modifier = Modifier.testTag("nav_projects"),
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.ACCOUNT,
                        onClick = {
                            navController.navigate(Routes.ACCOUNT) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Rounded.AccountCircle, contentDescription = null) },
                        label = { Text("Account") },
                        modifier = Modifier.testTag("nav_account"),
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    prefs = prefs,
                    projectCount = projects.size,
                    onOpenTool = { tool -> navController.navigate(Routes.toolRoute(tool)) },
                    onTogglePin = { viewModel.togglePinTool(it) },
                    onOpenProjects = {
                        navController.navigate(Routes.PROJECTS) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onUnlockProPreview = { viewModel.setProUnlocked(true) },
                )
            }

            composable(Routes.PROJECTS) {
                ProjectsScreen(
                    projects = projects,
                    surveyorFooter = surveyorFooter,
                    onSaveProject = { viewModel.saveProject(it) },
                    onDuplicateProject = { viewModel.duplicateProject(it) },
                    onDeleteProject = { viewModel.deleteProject(it) },
                    onLoadSample = { viewModel.loadSampleSurveyProjects() },
                    onOpenInCad = { proj ->
                        val pts = SurveyRepository.decodePoints(proj.verticesJson)
                        if (pts.isNotEmpty()) viewModel.setActiveDraftPoints(pts)
                        navController.navigate(Routes.toolRoute(SurveyToolId.CAD_EDITOR))
                    },
                )
            }

            composable(Routes.ACCOUNT) {
                AccountScreen(
                    prefs = prefs,
                    onSaveProfile = { n, t, p, e, o -> viewModel.saveSurveyorProfile(n, t, p, e, o) },
                    onSetLanguage = { viewModel.setLanguage(it) },
                    onSetDefaultUnit = { viewModel.setDefaultUnit(it) },
                    onSetProUnlocked = { viewModel.setProUnlocked(it) },
                    onSetPinLock = { en, pin -> viewModel.setPinLock(en, pin) },
                    onExportBackupJson = { viewModel.exportBackupJson() },
                    onImportBackupJson = { viewModel.importBackupJson(it) },
                )
            }

            composable(
                route = Routes.TOOL,
                arguments = listOf(navArgument("toolId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val toolIdStr = backStackEntry.arguments?.getString("toolId") ?: SurveyToolId.AUTO_NOKSHA.id
                val tool = SurveyToolId.fromId(toolIdStr)

                when (tool) {
                    SurveyToolId.AREA_CALCULATOR,
                    SurveyToolId.LENGTH_WIDTH,
                    SurveyToolId.QUAD_MEASURE,
                    SurveyToolId.EXACT_QUAD,
                    SurveyToolId.TRIANGLE_MEASURE,
                    SurveyToolId.MULTI_TRIANGLE,
                    SurveyToolId.AUTO_CORNER,
                    SurveyToolId.ANA_GONDA_CALC -> {
                        CalculatorsScreen(
                            tool = tool,
                            surveyorFooter = surveyorFooter,
                            onBack = { navController.popBackStack() },
                            onSaveProject = { title, owner, mouza, dag, khatian, areaSqFt, pts, sum ->
                                viewModel.saveMeasurementToProject(title, owner, mouza, dag, khatian, areaSqFt, pts, sum)
                            },
                        )
                    }

                    SurveyToolId.TIE_LINE,
                    SurveyToolId.SLOPE_ELEVATION,
                    SurveyToolId.LAND_SURVEY_TRAVERSE,
                    SurveyToolId.BOUNDARY_DEMARCATION -> {
                        SurveyCalculationsScreen(
                            tool = tool,
                            surveyorFooter = surveyorFooter,
                            onBack = { navController.popBackStack() },
                            onSaveProject = { title, owner, mouza, dag, khatian, areaSqFt, pts, sum ->
                                viewModel.saveMeasurementToProject(title, owner, mouza, dag, khatian, areaSqFt, pts, sum)
                            },
                        )
                    }

                    SurveyToolId.AUTO_NOKSHA,
                    SurveyToolId.PLOT_PLANNING,
                    SurveyToolId.PENTAGRAPH -> {
                        AutoNokshaAndPlotScreen(
                            tool = tool,
                            surveyorFooter = surveyorFooter,
                            onBack = { navController.popBackStack() },
                            onOpenInCad = { pts ->
                                viewModel.setActiveDraftPoints(pts)
                                navController.navigate(Routes.toolRoute(SurveyToolId.CAD_EDITOR))
                            },
                            onSaveProject = { title, owner, mouza, dag, khatian, areaSqFt, pts, sum ->
                                viewModel.saveMeasurementToProject(title, owner, mouza, dag, khatian, areaSqFt, pts, sum)
                            },
                        )
                    }

                    SurveyToolId.LAND_PARTITION -> {
                        PartitionScreen(
                            surveyorFooter = surveyorFooter,
                            onBack = { navController.popBackStack() },
                            onSaveProject = { title, owner, mouza, dag, khatian, areaSqFt, sum, partSum ->
                                viewModel.saveMeasurementToProject(title, owner, mouza, dag, khatian, areaSqFt, emptyList(), sum, partSum)
                            },
                        )
                    }

                    SurveyToolId.SURVEY_KHATIAN -> {
                        KhatianScreen(
                            khatians = khatians,
                            onSaveKhatian = { viewModel.saveKhatian(it) },
                            onDeleteKhatian = { viewModel.deleteKhatian(it) },
                            onBack = { navController.popBackStack() },
                        )
                    }

                    SurveyToolId.CS_BS_TRACKER -> {
                        CsBsTrackerScreen(
                            records = csBsRecords,
                            onSave = { viewModel.saveCsBs(it) },
                            onDelete = { viewModel.deleteCsBs(it) },
                            onBack = { navController.popBackStack() },
                        )
                    }

                    SurveyToolId.ONLINE_DIGITAL_NOKSHA,
                    SurveyToolId.SATELLITE_LINE_MAP,
                    SurveyToolId.GOOGLE_MAP_OVERLAY,
                    SurveyToolId.NOKSHA_MEASURE,
                    SurveyToolId.GPS_COORDINATES,
                    SurveyToolId.BOUNDARY_TRACKING -> {
                        MapAndGpsScreen(
                            tool = tool,
                            surveyorFooter = surveyorFooter,
                            onBack = { navController.popBackStack() },
                            onOpenInCad = { pts ->
                                viewModel.setActiveDraftPoints(pts)
                                navController.navigate(Routes.toolRoute(SurveyToolId.CAD_EDITOR))
                            },
                            onSaveProject = { title, owner, mouza, dag, khatian, areaSqFt, pts, sum ->
                                viewModel.saveMeasurementToProject(title, owner, mouza, dag, khatian, areaSqFt, pts, sum)
                            },
                        )
                    }

                    SurveyToolId.CAD_EDITOR -> {
                        CadEditorScreen(
                            initialPoints = activeDraftPoints,
                            surveyorFooter = surveyorFooter,
                            onPointsChanged = { viewModel.setActiveDraftPoints(it) },
                            onBack = { navController.popBackStack() },
                            onSaveProject = { title, owner, mouza, dag, khatian, areaSqFt, pts, sum ->
                                viewModel.saveMeasurementToProject(title, owner, mouza, dag, khatian, areaSqFt, pts, sum)
                            },
                        )
                    }

                    SurveyToolId.TOTAL_STATION_IMPORT -> {
                        TotalStationImportScreen(
                            surveyorFooter = surveyorFooter,
                            onBack = { navController.popBackStack() },
                            onOpenInCad = { pts ->
                                viewModel.setActiveDraftPoints(pts)
                                navController.navigate(Routes.toolRoute(SurveyToolId.CAD_EDITOR))
                            },
                            onSaveProject = { title, owner, mouza, dag, khatian, areaSqFt, pts, sum ->
                                viewModel.saveMeasurementToProject(title, owner, mouza, dag, khatian, areaSqFt, pts, sum)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PinLockScreen(
    expectedPin: String,
    isBengali: Boolean,
    onUnlocked: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Rounded.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (isBengali) "হাওলাদার ডিজিটাল সার্ভে" else "Howlader Digital Survey",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (isBengali) "প্রবেশ করতে ৪ সংখ্যার PIN দিন" else "Enter 4-digit PIN to unlock",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = input,
            onValueChange = {
                if (it.length <= 4 && it.all(Char::isDigit)) {
                    input = it
                    isError = false
                    if (it == expectedPin) onUnlocked()
                }
            },
            label = { Text("PIN") },
            isError = isError,
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = {
                if (input == expectedPin) onUnlocked() else isError = true
            },
            modifier = Modifier.height(48.dp),
        ) {
            Text(if (isBengali) "আনলক করুন" else "Unlock")
        }
    }
}
