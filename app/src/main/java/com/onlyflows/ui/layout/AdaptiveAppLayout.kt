package com.onlyflows.ui.layout

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.onlyflows.MainActivity
import com.onlyflows.ui.components.sidebar.AppSidebar
import com.onlyflows.ui.navigation.AppNavigation
import com.onlyflows.ui.navigation.Screen
import com.onlyflows.ui.overlays.QuickFindPopup
import kotlinx.coroutines.launch

@Composable
fun AdaptiveAppLayout(
    navController: NavHostController,
    isTablet: Boolean,
    modifier: Modifier = Modifier,
) {
    var showQuickFind by remember { mutableStateOf(false) }

    // Tablet-only: currently selected detail pane
    var detailPane by remember { mutableStateOf<DetailPane>(DetailPane.None) }

    // Register hardware keyboard shortcuts at Activity level so they fire regardless of focus
    val activity = LocalContext.current as? MainActivity
    DisposableEffect(navController) {
        activity?.keyShortcutHandler = { event ->
            when {
                event.isCtrlPressed && event.keyCode == AndroidKeyEvent.KEYCODE_F -> { showQuickFind = true; true }
                event.isCtrlPressed && event.keyCode == AndroidKeyEvent.KEYCODE_T -> { navController.navigate(Screen.Today.route); true }
                event.isCtrlPressed && event.keyCode == AndroidKeyEvent.KEYCODE_U -> { navController.navigate(Screen.Upcoming.route); true }
                event.isCtrlPressed && event.keyCode == AndroidKeyEvent.KEYCODE_I -> { navController.navigate(Screen.Inbox.route); true }
                event.isCtrlPressed && event.keyCode == AndroidKeyEvent.KEYCODE_A -> { navController.navigate(Screen.Anytime.route); true }
                event.isCtrlPressed && event.keyCode == AndroidKeyEvent.KEYCODE_P -> { navController.navigate(Screen.Projects.route); true }
                event.isCtrlPressed && event.keyCode == AndroidKeyEvent.KEYCODE_L -> { navController.navigate(Screen.Logbook.route); true }
                event.isCtrlPressed && event.keyCode == AndroidKeyEvent.KEYCODE_S -> { navController.navigate(Screen.Someday.route); true }
                event.keyCode == AndroidKeyEvent.KEYCODE_ESCAPE && showQuickFind -> { showQuickFind = false; true }
                else -> false
            }
        }
        onDispose { activity?.keyShortcutHandler = null }
    }

    // Root Box — QuickFind overlay is placed LAST so it renders on top of everything
    Box(modifier = modifier) {
        if (isTablet) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Column 1 — sidebar (fixed width)
                // Projects always navigate in the middle NavHost; only tasks open the right pane
                val navigateToProject = { id: String ->
                    navController.navigate(Screen.ProjectDetail.createRoute(id))
                }
                AppSidebar(
                    navController        = navController,
                    onProjectClick       = { navigateToProject(it) },
                    onSettingsClick      = { navController.navigate(Screen.Settings.route) },
                    onNewGroup           = {},
                    onAddProjectToFolder = {},
                    onQuickFindClick     = { showQuickFind = true },
                )
                VerticalDivider()
                if (detailPane == DetailPane.None) {
                    // Nothing selected: master list fills all remaining space
                    AppNavigation(
                        navController     = navController,
                        onOpenDrawer      = {},
                        onSearchClick     = { showQuickFind = true },
                        isTablet          = true,
                        onTaskSelected    = { detailPane = DetailPane.Task(it) },
                        onProjectSelected = { navigateToProject(it) },
                        onNoteSelected    = { nId, pId -> detailPane = DetailPane.Note(nId, pId) },
                        modifier          = Modifier.weight(1f),
                    )
                } else {
                    // Task selected: master list 38 % + task detail panel 62 %
                    AppNavigation(
                        navController     = navController,
                        onOpenDrawer      = {},
                        onSearchClick     = { showQuickFind = true },
                        isTablet          = true,
                        onTaskSelected    = { detailPane = DetailPane.Task(it) },
                        onProjectSelected = { navigateToProject(it) },
                        onNoteSelected    = { nId, pId -> detailPane = DetailPane.Note(nId, pId) },
                        modifier          = Modifier.weight(0.38f),
                    )
                    VerticalDivider()
                    DetailPaneContent(
                        pane           = detailPane,
                        onClose        = { detailPane = DetailPane.None },
                        onProjectClick = { navigateToProject(it) },
                        modifier       = Modifier.weight(0.62f),
                    )
                }
            }
        } else {
            val drawerState    = rememberDrawerState(DrawerValue.Closed)
            val coroutineScope = rememberCoroutineScope()

            ModalNavigationDrawer(
                drawerState   = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = MaterialTheme.colorScheme.surface,
                        drawerContentColor   = MaterialTheme.colorScheme.onSurface,
                    ) {
                        AppSidebar(
                            navController        = navController,
                            onProjectClick       = {
                                navController.navigate(Screen.ProjectDetail.createRoute(it))
                                coroutineScope.launch { drawerState.close() }
                            },
                            onSettingsClick      = {
                                coroutineScope.launch { drawerState.close() }
                                navController.navigate(Screen.Settings.route)
                            },
                            onNewGroup           = {},
                            onAddProjectToFolder = {},
                            onItemSelected       = { coroutineScope.launch { drawerState.close() } },
                            onQuickFindClick     = {
                                coroutineScope.launch { drawerState.close() }
                                showQuickFind = true
                            },
                        )
                    }
                },
                modifier = Modifier.fillMaxSize(),
            ) {
                AppNavigation(
                    navController = navController,
                    onOpenDrawer  = { coroutineScope.launch { drawerState.open() } },
                    onSearchClick = { showQuickFind = true },
                )
            }
        }

        // QuickFind overlay — rendered LAST in the Box so it floats above all content
        if (showQuickFind) {
            QuickFindPopup(
                onDismiss      = { showQuickFind = false },
                onTaskClick    = { taskId ->
                    showQuickFind = false
                    if (isTablet) detailPane = DetailPane.Task(taskId)
                    else navController.navigate(Screen.TaskDetail.createRoute(taskId))
                },
                onProjectClick = { projectId ->
                    showQuickFind = false
                    navController.navigate(Screen.ProjectDetail.createRoute(projectId))
                },
                onShowMore     = { query ->
                    showQuickFind = false
                    navController.navigate(Screen.Search.createRoute(query))
                },
                onDateClick    = { date ->
                    showQuickFind = false
                    navController.navigate(Screen.DateResults.createRoute(date.toString()))
                },
            )
        }
    }
}
