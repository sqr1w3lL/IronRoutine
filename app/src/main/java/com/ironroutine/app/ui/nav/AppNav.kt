package com.ironroutine.app.ui.nav

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ironroutine.app.ui.calendar.CalendarScreen
import com.ironroutine.app.ui.exercise.ExerciseInfoScreen
import com.ironroutine.app.ui.home.HomeScreen
import com.ironroutine.app.ui.progress.ProgressScreen
import com.ironroutine.app.ui.progress.SessionDetailScreen
import com.ironroutine.app.ui.settings.SettingsScreen
import com.ironroutine.app.ui.theme.IronColors
import com.ironroutine.app.ui.workout.WorkoutScreen
import com.ironroutine.app.ui.workout.WorkoutsScreen

object Routes {
    const val HOME = "home"
    const val WORKOUTS = "workouts"
    const val PROGRESS = "progress"
    const val CALENDAR = "calendar"
    const val SETTINGS = "settings"

    const val WORKOUT = "workout/{dayId}?autostart={autostart}"
    const val EXERCISE = "exercise/{exerciseId}"
    const val SESSION = "session/{sessionId}"

    fun workout(dayId: String, autostart: Boolean) = "workout/$dayId?autostart=$autostart"
    fun exercise(id: String) = "exercise/$id"
    fun session(id: Long) = "session/$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Главная", Icons.Filled.Home),
    Tab(Routes.WORKOUTS, "Тренировки", Icons.Filled.FitnessCenter),
    Tab(Routes.PROGRESS, "Прогресс", Icons.Filled.BarChart),
    Tab(Routes.CALENDAR, "Календарь", Icons.Filled.CalendarMonth)
)

@Composable
fun IronRoutineApp() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val showBar = tabs.any { tab -> destination?.hierarchy?.any { it.route == tab.route } == true }

    val openWorkout: (String, Boolean) -> Unit = { id, auto -> nav.navigate(Routes.workout(id, auto)) }
    val openExercise: (String) -> Unit = { id -> nav.navigate(Routes.exercise(id)) }
    val openSession: (Long) -> Unit = { id -> nav.navigate(Routes.session(id)) }

    Scaffold(
        containerColor = IronColors.Background,
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = IronColors.Card) {
                    tabs.forEach { tab ->
                        val selected = destination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = IronColors.OnAccent,
                                selectedTextColor = IronColors.Accent,
                                indicatorColor = IronColors.Accent,
                                unselectedIconColor = IronColors.TextSecondary,
                                unselectedTextColor = IronColors.TextSecondary
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding).imePadding()
        ) {
            composable(Routes.HOME) {
                HomeScreen(onOpenWorkout = openWorkout, onOpenSettings = { nav.navigate(Routes.SETTINGS) })
            }
            composable(Routes.WORKOUTS) {
                WorkoutsScreen(onOpenWorkout = openWorkout, onOpenExercise = openExercise)
            }
            composable(Routes.PROGRESS) {
                ProgressScreen(onOpenSession = openSession)
            }
            composable(Routes.CALENDAR) {
                CalendarScreen(onOpenWorkout = openWorkout, onOpenSession = openSession)
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { nav.popBackStack() })
            }
            composable(
                route = Routes.WORKOUT,
                arguments = listOf(
                    navArgument("dayId") { type = NavType.StringType },
                    navArgument("autostart") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { entry ->
                val dayId = entry.arguments?.getString("dayId") ?: return@composable
                val autostart = entry.arguments?.getBoolean("autostart") ?: false
                WorkoutScreen(
                    dayId = dayId,
                    autostart = autostart,
                    onBack = { nav.popBackStack() },
                    onOpenExercise = openExercise,
                    onOpenOtherWorkout = { id -> nav.navigate(Routes.workout(id, false)) }
                )
            }
            composable(
                route = Routes.EXERCISE,
                arguments = listOf(navArgument("exerciseId") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("exerciseId") ?: return@composable
                ExerciseInfoScreen(exerciseId = id, onBack = { nav.popBackStack() })
            }
            composable(
                route = Routes.SESSION,
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("sessionId") ?: return@composable
                SessionDetailScreen(sessionId = id, onBack = { nav.popBackStack() })
            }
        }
    }
}
