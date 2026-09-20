package com.onlyflows.ui.navigation

sealed class Screen(val route: String) {
    data object Today         : Screen("today")
    data object Upcoming      : Screen("upcoming")
    data object Inbox         : Screen("inbox")
    data object Anytime       : Screen("anytime")
    data object Someday       : Screen("someday")
    data object Logbook       : Screen("logbook")
    data object Trash         : Screen("trash")
    data object Archived      : Screen("archived")
    data object Projects      : Screen("projects")
    data object Search        : Screen("search?q={query}") {
        fun createRoute(query: String = "") = "search?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
    }
    data object Settings              : Screen("settings")
    data object Appearance            : Screen("settings/appearance")
    data object CalendarSettings      : Screen("settings/calendar")
    data object BackupSettings        : Screen("settings/backup")
    data object NotificationSettings  : Screen("settings/notifications")
    data object AboutApp               : Screen("settings/about")
    data object GeneralSettings        : Screen("settings/general")
    data object LocationsSettings      : Screen("settings/locations")
    data object Statistics             : Screen("settings/statistics")
    data object Logs                   : Screen("settings/logs")
    data object LanSyncSettings : Screen("settings/lan-sync")
    data object Tags                   : Screen("settings/tags")

    data object DateTracker    : Screen("datetracker")
    data object Rss            : Screen("rss")
    data object RssSettings    : Screen("settings/rss")
    data object RssArticle     : Screen("rss/article/{articleId}") {
        fun createRoute(articleId: String) = "rss/article/$articleId"
    }
    data object Goals          : Screen("goals")
    data object GoalDetail     : Screen("goals/{goalId}") {
        fun createRoute(goalId: String) = "goals/$goalId"
    }

    data object ProjectDetail : Screen("project/{projectId}") {
        fun createRoute(projectId: String) = "project/$projectId"
    }
    data object TaskDetail    : Screen("task/{taskId}?openReminder={openReminder}") {
        fun createRoute(taskId: String, openReminder: Boolean = false) =
            "task/$taskId?openReminder=$openReminder"
    }
    data object DateResults   : Screen("date_results/{date}") {
        fun createRoute(date: String) = "date_results/$date"
    }
}
