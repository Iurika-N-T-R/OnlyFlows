package com.onlyflows.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import java.net.URLDecoder
import com.onlyflows.ui.screens.anytime.AnytimeScreen
import com.onlyflows.ui.screens.inbox.InboxScreen
import com.onlyflows.ui.screens.trash.TrashScreen
import com.onlyflows.ui.screens.projects.ProjectsScreen
import com.onlyflows.ui.screens.logbook.LogbookScreen
import com.onlyflows.ui.screens.someday.SomedayScreen
import com.onlyflows.ui.screens.project.ProjectDetailScreen
import com.onlyflows.ui.screens.search.DateResultsScreen
import com.onlyflows.ui.screens.search.QuickFindScreen
import com.onlyflows.ui.screens.settings.AppearanceScreen
import com.onlyflows.ui.screens.settings.BackupSettingsScreen
import com.onlyflows.ui.screens.settings.CalendarSettingsScreen
import com.onlyflows.ui.screens.settings.AboutAppScreen
import com.onlyflows.ui.screens.settings.GeneralSettingsScreen
import com.onlyflows.ui.screens.settings.NotificationSettingsScreen
import com.onlyflows.ui.screens.settings.SettingsScreen
import com.onlyflows.ui.screens.task.TaskDetailScreen
import com.onlyflows.ui.screens.today.TodayScreen
import com.onlyflows.ui.screens.upcoming.UpcomingScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    onOpenDrawer: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Today.route,
        modifier = modifier,
    ) {
        composable(Screen.Today.route) {
            TodayScreen(
                onTaskClick   = { taskId -> navController.navigate(Screen.TaskDetail.createRoute(taskId)) },
                onSearchClick = onSearchClick,
                onOpenDrawer  = onOpenDrawer,
            )
        }

        composable(Screen.Upcoming.route) {
            UpcomingScreen(
                onTaskClick  = { taskId -> navController.navigate(Screen.TaskDetail.createRoute(taskId)) },
                onOpenDrawer = onOpenDrawer,
            )
        }

        composable(Screen.Inbox.route) {
            InboxScreen(
                onTaskClick  = { taskId -> navController.navigate(Screen.TaskDetail.createRoute(taskId)) },
                onOpenDrawer = onOpenDrawer,
            )
        }

        composable(Screen.Projects.route) {
            ProjectsScreen(
                onProjectClick = { projectId -> navController.navigate(Screen.ProjectDetail.createRoute(projectId)) },
                onOpenDrawer   = onOpenDrawer,
            )
        }

        composable(Screen.Logbook.route) {
            LogbookScreen(
                onProjectClick = { projectId -> navController.navigate(Screen.ProjectDetail.createRoute(projectId)) },
                onOpenDrawer   = onOpenDrawer,
            )
        }

        composable(Screen.Someday.route) {
            SomedayScreen(
                onTaskClick  = { taskId -> navController.navigate(Screen.TaskDetail.createRoute(taskId)) },
                onOpenDrawer = onOpenDrawer,
            )
        }

        composable(Screen.Anytime.route) {
            AnytimeScreen(
                onTaskClick  = { taskId -> navController.navigate(Screen.TaskDetail.createRoute(taskId)) },
                onOpenDrawer = onOpenDrawer,
            )
        }

        composable(Screen.Trash.route) {
            TrashScreen(onOpenDrawer = onOpenDrawer)
        }

        composable(
            route = Screen.ProjectDetail.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStack ->
            val projectId = backStack.arguments?.getString("projectId") ?: return@composable
            ProjectDetailScreen(
                projectId   = projectId,
                onTaskClick = { taskId -> navController.navigate(Screen.TaskDetail.createRoute(taskId)) },
                onListClick = { listId -> navController.navigate(Screen.ProjectDetail.createRoute(listId)) },
                onBack      = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.TaskDetail.route,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { backStack ->
            val taskId = backStack.arguments?.getString("taskId") ?: return@composable
            TaskDetailScreen(
                taskId      = taskId,
                onBack      = { navController.popBackStack() },
                onDeleted   = { navController.popBackStack() },
                onConverted = { projectId ->
                    navController.popBackStack()
                    navController.navigate(Screen.ProjectDetail.createRoute(projectId))
                },
            )
        }

        composable(
            route = Screen.Search.route,
            arguments = listOf(navArgument("query") { type = NavType.StringType; defaultValue = "" }),
        ) { backStack ->
            val initialQuery = URLDecoder.decode(backStack.arguments?.getString("query") ?: "", "UTF-8")
            QuickFindScreen(
                initialQuery   = initialQuery,
                onTaskClick    = { taskId    -> navController.navigate(Screen.TaskDetail.createRoute(taskId)) },
                onProjectClick = { projectId -> navController.navigate(Screen.ProjectDetail.createRoute(projectId)) },
                onDateClick    = { date      -> navController.navigate(Screen.DateResults.createRoute(date.toString())) },
                onBack         = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.DateResults.route,
            arguments = listOf(navArgument("date") { type = NavType.StringType }),
        ) { backStack ->
            val dateStr = backStack.arguments?.getString("date") ?: return@composable
            DateResultsScreen(
                dateStr     = dateStr,
                onTaskClick = { taskId -> navController.navigate(Screen.TaskDetail.createRoute(taskId)) },
                onBack      = { navController.popBackStack() },
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack                       = { navController.popBackStack() },
                onAppearanceClick            = { navController.navigate(Screen.Appearance.route) },
                onCalendarSettingsClick      = { navController.navigate(Screen.CalendarSettings.route) },
                onBackupClick                = { navController.navigate(Screen.BackupSettings.route) },
                onNotificationSettingsClick  = { navController.navigate(Screen.NotificationSettings.route) },
                onAboutClick                 = { navController.navigate(Screen.AboutApp.route) },
                onGeneralSettingsClick       = { navController.navigate(Screen.GeneralSettings.route) },
            )
        }

        composable(Screen.Appearance.route) {
            AppearanceScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.CalendarSettings.route) {
            CalendarSettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.BackupSettings.route) {
            BackupSettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.NotificationSettings.route) {
            NotificationSettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.AboutApp.route) {
            AboutAppScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.GeneralSettings.route) {
            GeneralSettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
