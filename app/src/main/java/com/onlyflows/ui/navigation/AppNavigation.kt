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
import com.onlyflows.ui.screens.datetracker.DateTrackerScreen
import com.onlyflows.ui.screens.rss.RssArticleScreen
import com.onlyflows.ui.screens.rss.RssScreen
import com.onlyflows.ui.screens.rss.RssSettingsScreen
import com.onlyflows.ui.screens.goals.GoalsScreen
import com.onlyflows.ui.screens.goals.GoalDetailScreen
import com.onlyflows.ui.screens.search.DateResultsScreen
import com.onlyflows.ui.screens.search.QuickFindScreen
import com.onlyflows.ui.screens.settings.AppearanceScreen
import com.onlyflows.ui.screens.settings.BackupSettingsScreen
import com.onlyflows.ui.screens.settings.CalendarSettingsScreen
import com.onlyflows.ui.screens.settings.AboutAppScreen
import com.onlyflows.ui.screens.settings.GeneralSettingsScreen
import com.onlyflows.ui.screens.settings.LocationsSettingsScreen
import com.onlyflows.ui.screens.settings.NotificationSettingsScreen
import com.onlyflows.ui.screens.settings.SettingsScreen
import com.onlyflows.ui.screens.logs.LogsScreen
import com.onlyflows.ui.screens.settings.SyncthingSettingsScreen
import com.onlyflows.ui.screens.task.TaskDetailScreen
import com.onlyflows.ui.screens.today.TodayScreen
import com.onlyflows.ui.screens.upcoming.UpcomingScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    onOpenDrawer: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    isTablet: Boolean = false,
    onTaskSelected: ((String) -> Unit)? = null,
    onProjectSelected: ((String) -> Unit)? = null,
    onNoteSelected: ((noteId: String, projectId: String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    fun navigateToTask(id: String) {
        if (isTablet && onTaskSelected != null) onTaskSelected(id)
        else navController.navigate(Screen.TaskDetail.createRoute(id))
    }
    fun navigateToProject(id: String) {
        if (isTablet && onProjectSelected != null) onProjectSelected(id)
        else navController.navigate(Screen.ProjectDetail.createRoute(id))
    }
    NavHost(
        navController = navController,
        startDestination = Screen.Today.route,
        modifier = modifier,
    ) {
        composable(Screen.Today.route) {
            TodayScreen(
                onTaskClick   = { navigateToTask(it) },
                onSearchClick = onSearchClick,
                onOpenDrawer  = onOpenDrawer,
            )
        }

        composable(Screen.Upcoming.route) {
            UpcomingScreen(
                onTaskClick  = { navigateToTask(it) },
                onOpenDrawer = onOpenDrawer,
            )
        }

        composable(Screen.Inbox.route) {
            InboxScreen(
                onTaskClick  = { navigateToTask(it) },
                onOpenDrawer = onOpenDrawer,
            )
        }

        composable(Screen.Projects.route) {
            ProjectsScreen(
                onProjectClick = { navigateToProject(it) },
                onOpenDrawer   = onOpenDrawer,
            )
        }

        composable(Screen.Logbook.route) {
            LogbookScreen(
                onProjectClick = { navigateToProject(it) },
                onOpenDrawer   = onOpenDrawer,
            )
        }

        composable(Screen.Someday.route) {
            SomedayScreen(
                onTaskClick  = { navigateToTask(it) },
                onOpenDrawer = onOpenDrawer,
            )
        }

        composable(Screen.Anytime.route) {
            AnytimeScreen(
                onTaskClick  = { navigateToTask(it) },
                onOpenDrawer = onOpenDrawer,
            )
        }

        composable(Screen.Rss.route) {
            RssScreen(
                onOpenDrawer    = onOpenDrawer,
                onSettingsClick = { navController.navigate(Screen.RssSettings.route) },
                onArticleClick  = { url, title -> navController.navigate(Screen.RssArticle.createRoute(url, title)) },
            )
        }
        composable(Screen.RssSettings.route) {
            RssSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route     = Screen.RssArticle.route,
            arguments = listOf(
                androidx.navigation.navArgument("url")   { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("title") { type = androidx.navigation.NavType.StringType },
            ),
        ) { backStack ->
            val url   = java.net.URLDecoder.decode(backStack.arguments?.getString("url")   ?: "", "UTF-8")
            val title = java.net.URLDecoder.decode(backStack.arguments?.getString("title") ?: "", "UTF-8")
            RssArticleScreen(url = url, title = title, onBack = { navController.popBackStack() })
        }
        composable(Screen.DateTracker.route) {
            DateTrackerScreen(onOpenDrawer = onOpenDrawer)
        }

        composable(Screen.Trash.route) {
            TrashScreen(onOpenDrawer = onOpenDrawer)
        }

        composable(Screen.Goals.route) {
            GoalsScreen(
                onOpenDrawer = onOpenDrawer,
                onGoalClick = { goalId -> navController.navigate(Screen.GoalDetail.createRoute(goalId)) },
            )
        }

        composable(
            route = Screen.GoalDetail.route,
            arguments = listOf(navArgument("goalId") { type = NavType.StringType }),
        ) {
            val goalId = it.arguments!!.getString("goalId")!!
            GoalDetailScreen(
                goalId    = goalId,
                onBack    = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.ProjectDetail.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStack ->
            val projectId = backStack.arguments?.getString("projectId") ?: return@composable
            ProjectDetailScreen(
                projectId       = projectId,
                onTaskClick     = { navigateToTask(it) },
                onListClick     = { navigateToProject(it) },
                onBack          = { navController.popBackStack() },
                onNoteSelected  = if (isTablet) onNoteSelected else null,
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
                onTaskClick    = { navigateToTask(it) },
                onProjectClick = { navigateToProject(it) },
                onGoalClick    = { navController.navigate(Screen.GoalDetail.createRoute(it)) },
                onDateClick    = { date -> navController.navigate(Screen.DateResults.createRoute(date.toString())) },
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
                onTaskClick = { navigateToTask(it) },
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
                onLocationsSettingsClick     = { navController.navigate(Screen.LocationsSettings.route) },
                onStatisticsClick            = { navController.navigate(Screen.Statistics.route) },
                onLogsClick                  = { navController.navigate(Screen.Logs.route) },
                onSyncthingClick             = { navController.navigate(Screen.SyncthingSettings.route) },
                onTagsClick                  = { navController.navigate(Screen.Tags.route) },
                onRssSettingsClick           = { navController.navigate(Screen.RssSettings.route) },
            )
        }

        composable(Screen.Tags.route) {
            com.onlyflows.ui.screens.tags.TagsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Logs.route) {
            LogsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.SyncthingSettings.route) {
            SyncthingSettingsScreen(onBack = { navController.popBackStack() })
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

        composable(Screen.LocationsSettings.route) {
            LocationsSettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Statistics.route) {
            com.onlyflows.ui.screens.settings.StatisticsScreen(onBack = { navController.popBackStack() })
        }

    }
}
