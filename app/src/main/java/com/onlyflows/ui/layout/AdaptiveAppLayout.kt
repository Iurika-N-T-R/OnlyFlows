package com.onlyflows.ui.layout

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
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

    if (showQuickFind) {
        QuickFindPopup(
            onDismiss      = { showQuickFind = false },
            onTaskClick    = { taskId ->
                showQuickFind = false
                navController.navigate(Screen.TaskDetail.createRoute(taskId))
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

    if (isTablet) {
        Row(modifier = modifier) {
            AppSidebar(
                navController        = navController,
                onProjectClick       = { navController.navigate(Screen.ProjectDetail.createRoute(it)) },
                onSettingsClick      = { navController.navigate(Screen.Settings.route) },
                onNewGroup           = {},
                onAddProjectToFolder = {},
                onQuickFindClick     = { showQuickFind = true },
            )
            VerticalDivider()
            AppNavigation(
                navController = navController,
                onOpenDrawer  = {},
                onSearchClick = { showQuickFind = true },
                modifier      = Modifier.weight(1f),
            )
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
            modifier = modifier,
        ) {
            AppNavigation(
                navController = navController,
                onOpenDrawer  = { coroutineScope.launch { drawerState.open() } },
                onSearchClick = { showQuickFind = true },
            )
        }
    }
}
