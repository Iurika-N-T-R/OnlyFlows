package com.onlyflows

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowWidthSizeClass
import com.onlyflows.data.preferences.ThemeMode
import com.onlyflows.domain.model.Task
import com.onlyflows.domain.model.TaskGroup
import com.onlyflows.domain.model.TaskPriority
import com.onlyflows.ui.layout.AdaptiveAppLayout
import com.onlyflows.ui.navigation.Screen
import com.onlyflows.ui.overlays.AddTaskSheet
import com.onlyflows.ui.theme.LocalFontScale
import com.onlyflows.ui.theme.LocalUse24hFormat
import com.onlyflows.ui.theme.OnlyFlowsTheme
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ADD_TASK      = "extra_add_task"
        const val EXTRA_DESTINATION   = "extra_destination"
        const val DESTINATION_TODAY   = "TODAY"
        const val DESTINATION_INBOX   = "INBOX"
        const val EXTRA_TASK_ID       = "extra_task_id"
    }

    var keyShortcutHandler: ((android.view.KeyEvent) -> Boolean)? = null
    var navigateToTask: ((String) -> Unit)? = null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_ADD_TASK, false) ||
            intent.getStringExtra(EXTRA_DESTINATION) != null) {
            (application as OnlyFlowsApp).requestAddTask()
        }
        intent.getStringExtra(EXTRA_TASK_ID)?.let { taskId ->
            navigateToTask?.invoke(taskId)
        }
    }

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (event.action == android.view.KeyEvent.ACTION_DOWN &&
            keyShortcutHandler?.invoke(event) == true
        ) return true
        return super.dispatchKeyEvent(event)
    }

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPostNotificationsIfNeeded()
        requestBatteryOptimizationExemption()
        enableEdgeToEdge()
        if (intent.getBooleanExtra(EXTRA_ADD_TASK, false) ||
            intent.getStringExtra(EXTRA_DESTINATION) != null) {
            (application as OnlyFlowsApp).requestAddTask()
        }
        setContent {
            val app = application as OnlyFlowsApp
            val themeMode by app.container.appPreferences.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val use24hFormat by app.container.appPreferences.use24hFormat
                .collectAsStateWithLifecycle(initialValue = true)
            val fontScaleMultiplier by app.container.appPreferences.fontScale
                .collectAsStateWithLifecycle(initialValue = 1.0f)
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT  -> false
                ThemeMode.DARK   -> true
                ThemeMode.SYSTEM -> systemDark
            }

            OnlyFlowsTheme(darkTheme = darkTheme) {
                val sysDensity = LocalDensity.current
                var showGlobalAddTask by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    app.addTaskRequested.collect { showGlobalAddTask = true }
                }

                androidx.compose.runtime.CompositionLocalProvider(
                    LocalUse24hFormat provides use24hFormat,
                    LocalFontScale provides fontScaleMultiplier,
                    LocalDensity provides Density(
                        density   = sysDensity.density,
                        fontScale = sysDensity.fontScale * fontScaleMultiplier,
                    ),
                ) {
                    val navController = rememberNavController()

                    LaunchedEffect(navController) {
                        navigateToTask = { taskId ->
                            navController.navigate(Screen.TaskDetail.createRoute(taskId))
                        }
                        intent.getStringExtra(EXTRA_TASK_ID)?.let { taskId ->
                            navController.navigate(Screen.TaskDetail.createRoute(taskId))
                        }
                    }

                    val windowInfo = currentWindowAdaptiveInfo()
                    val isTablet = windowInfo.windowSizeClass.windowWidthSizeClass ==
                            WindowWidthSizeClass.EXPANDED

                    AdaptiveAppLayout(
                        navController = navController,
                        isTablet      = isTablet,
                        modifier      = Modifier.fillMaxSize(),
                    )

                    if (showGlobalAddTask) {
                        AddTaskSheet(
                            defaultGroup = TaskGroup.TODAY,
                            onAdd = { id, title, note, group, dueDate, dueTime, endTime, priority, projectId, sectionId, checklistItems ->
                                showGlobalAddTask = false
                                val repo = app.container.taskRepository
                                CoroutineScope(Dispatchers.IO).launch {
                                    repo.insert(Task(
                                        id             = id,
                                        title          = title,
                                        note           = note,
                                        priority       = priority,
                                        projectId      = projectId,
                                        sectionId      = sectionId,
                                        scheduledGroup = group,
                                        dueDate        = dueDate,
                                        dueTime        = dueTime,
                                        endTime        = endTime,
                                        createdAt      = LocalDateTime.now(),
                                        updatedAt      = LocalDateTime.now(),
                                    ))
                                    checklistItems.forEachIndexed { index, itemTitle ->
                                        repo.addChecklistItem(com.onlyflows.domain.model.ChecklistItem(
                                            id        = "cli_${System.currentTimeMillis()}_$index",
                                            taskId    = id,
                                            title     = itemTitle,
                                            sortOrder = index,
                                        ))
                                    }
                                }
                            },
                            onAddReminderForTask = { id, at, locId, rec ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    com.onlyflows.data.notifications.ReminderCreator.attach(
                                        applicationContext, id, at, locId, rec,
                                    )
                                }
                            },
                            onDismiss = { showGlobalAddTask = false },
                        )
                    }
                }
            }
        }
    }

    private fun requestPostNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /**
     * Ask Android to exclude this app from battery optimization.
     * Without this, alarms set via AlarmManager can be deferred or cancelled
     * when the app is in the background / "closed" by the user.
     *
     * The system shows a one-time dialog; the user can deny it.
     * We only request if not already exempted.
     */
    private fun requestBatteryOptimizationExemption() {
        val pm = getSystemService(PowerManager::class.java) ?: return
        if (pm.isIgnoringBatteryOptimizations(packageName)) return  // already exempted

        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        } catch (e: Exception) {
            // Some OEMs don't support this intent — fail silently
        }
    }
}
