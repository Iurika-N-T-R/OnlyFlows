package com.onlyflows.wear.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.onlyflows.wear.presentation.theme.OnlyFlowsWearTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OnlyFlowsWearTheme {
                val nav = rememberSwipeDismissableNavController()
                // Shared across list + menu so a picked filter reaches the list.
                val taskListVm: TaskListViewModel = viewModel()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    timeText = { TimeText() },
                ) {
                    SwipeDismissableNavHost(navController = nav, startDestination = "list") {
                        composable("list") {
                            TaskListScreen(
                                viewModel = taskListVm,
                                onOpenMenu = { nav.navigate("menu") },
                                onOpenTask = { id -> nav.navigate("edit/$id") },
                            )
                        }
                        composable("menu") {
                            MenuScreen(
                                onPick = { type, id, label ->
                                    taskListVm.setFilter(type, id, label)
                                    nav.popBackStack()
                                },
                                onOpenSettings = { nav.navigate("settings") },
                            )
                        }
                        composable(
                            "edit/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.StringType }),
                        ) { entry ->
                            TaskEditScreen(
                                taskId = entry.arguments?.getString("id").orEmpty(),
                                onDone = { taskListVm.load(); nav.popBackStack() },
                            )
                        }
                        composable("settings") { SettingsScreen() }
                    }
                }
            }
        }
    }
}
