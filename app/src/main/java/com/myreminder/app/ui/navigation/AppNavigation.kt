package com.myreminder.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.myreminder.app.ui.screens.addedit.AddEditScreen
import com.myreminder.app.ui.screens.calendar.CalendarScreen
import com.myreminder.app.ui.screens.detail.TaskDetailScreen
import com.myreminder.app.ui.screens.home.HomeScreen
import com.myreminder.app.ui.screens.search.SearchScreen
import com.myreminder.app.ui.screens.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val ADD_TASK = "add_task"
    const val EDIT_TASK = "edit_task/{taskId}"
    const val TASK_DETAIL = "task_detail/{taskId}"
    const val CALENDAR = "calendar"
    const val SEARCH = "search"
    const val SETTINGS = "settings"

    fun editTask(taskId: Long) = "edit_task/$taskId"
    fun taskDetail(taskId: Long) = "task_detail/$taskId"
}

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToAddTask = { navController.navigate(Routes.ADD_TASK) },
                onNavigateToTaskDetail = { taskId -> navController.navigate(Routes.taskDetail(taskId)) },
                onNavigateToCalendar = { navController.navigate(Routes.CALENDAR) },
                onNavigateToSearch = { navController.navigate(Routes.SEARCH) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.ADD_TASK) {
            AddEditScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.EDIT_TASK,
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) {
            AddEditScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.TASK_DETAIL,
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) {
            TaskDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { taskId -> navController.navigate(Routes.editTask(taskId)) }
            )
        }
        composable(Routes.CALENDAR) {
            CalendarScreen(
                onNavigateToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
                onNavigateToSearch = { navController.navigate(Routes.SEARCH) },
                onNavigateToTaskDetail = { taskId -> navController.navigate(Routes.taskDetail(taskId)) }
            )
        }
        composable(Routes.SEARCH) {
            SearchScreen(
                onNavigateToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
                onNavigateToCalendar = { navController.navigate(Routes.CALENDAR) },
                onNavigateToTaskDetail = { taskId -> navController.navigate(Routes.taskDetail(taskId)) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
