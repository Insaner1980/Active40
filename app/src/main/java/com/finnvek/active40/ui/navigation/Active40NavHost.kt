package com.finnvek.active40.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.finnvek.active40.R
import com.finnvek.active40.data.SettingsRepository
import com.finnvek.active40.ui.appViewModel
import com.finnvek.active40.ui.checkin.CheckInScreen
import com.finnvek.active40.ui.components.GreenBackground
import com.finnvek.active40.ui.components.cappedFontSize
import com.finnvek.active40.ui.exercises.ExercisesScreen
import com.finnvek.active40.ui.exercises.SymptomsScreen
import com.finnvek.active40.ui.history.HistoryScreen
import com.finnvek.active40.ui.onboarding.OnboardingScreen
import com.finnvek.active40.ui.progress.ProgressScreen
import com.finnvek.active40.ui.recovery.RecoveryScreen
import com.finnvek.active40.ui.routines.NEW_ROUTINE_ID
import com.finnvek.active40.ui.routines.RoutineEditScreen
import com.finnvek.active40.ui.routines.RoutinesScreen
import com.finnvek.active40.ui.run.NEW_RUN_ID
import com.finnvek.active40.ui.run.RunLogScreen
import com.finnvek.active40.ui.settings.SettingsScreen
import com.finnvek.active40.ui.strength.SessionDetailScreen
import com.finnvek.active40.ui.strength.StrengthSessionScreen
import com.finnvek.active40.ui.theme.Active40Colors
import com.finnvek.active40.ui.today.TodayScreen
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import kotlin.reflect.KClass

private const val NAV_LABEL_MAX_SCALE = 1.3f

private enum class TopLevelDestination(
    val route: Any,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    Today(TodayRoute, R.string.nav_today, R.drawable.ic_nav_today),
    History(HistoryRoute(), R.string.nav_history, R.drawable.ic_nav_history),
    Progress(ProgressRoute, R.string.nav_progress, R.drawable.ic_nav_progress),
    Recovery(RecoveryRoute, R.string.nav_recovery, R.drawable.ic_nav_recovery),
    ;

    val routeClass: KClass<*> get() = route::class
}

private fun NavDestination?.isOn(destination: TopLevelDestination) = this?.hierarchy?.any { it.hasRoute(destination.routeClass) } == true

/** Juuritason tila: onko käyttöönotto tehty (null = asetuksia luetaan vielä). */
class RootViewModel(
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val onboardingCompleted: StateFlow<Boolean?> =
        settingsRepository.settings
            .map { it.onboardingCompleted }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

@Composable
fun Active40Root() {
    val viewModel = appViewModel { RootViewModel(it.settingsRepository) }
    val completed by viewModel.onboardingCompleted.collectAsStateWithLifecycle()
    GreenBackground {
        when (completed) {
            null -> Unit
            false -> OnboardingScreen()
            true -> Active40NavHost()
        }
    }
}

@Composable
private fun Active40NavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    // Alapalkki näkyy vain välilehdillä; kirjaus- ja hallintanäkymät ovat koko näytön näkymiä.
    val showBottomBar = currentDestination == null || TopLevelDestination.entries.any { currentDestination.isOn(it) }

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = Active40Colors.onBackground,
        bottomBar = { if (showBottomBar) BottomBar(navController, currentDestination) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TodayRoute,
            // consumeWindowInsets: näkymien imePadding ei lisää alapalkin tilaa kahteen kertaan.
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
        ) {
            composable<TodayRoute> {
                TodayScreen(
                    onOpenSettings = { navController.navigate(SettingsRoute) },
                    onOpenSession = { navController.navigate(StrengthSessionRoute(it)) { launchSingleTop = true } },
                    onManageRoutines = { navController.navigate(RoutinesRoute) },
                    onLogRun = { navController.navigate(RunLogRoute(NEW_RUN_ID)) },
                    onOpenCheckIn = { navController.navigate(CheckInRoute(it.toEpochDay())) },
                    onOpenDay = { day ->
                        navController.navigate(HistoryRoute(day.toEpochDay())) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable<HistoryRoute> { entry ->
                val epochDay = entry.toRoute<HistoryRoute>().epochDay
                HistoryScreen(
                    initialDay = epochDay.takeIf { it != NO_DAY }?.let(LocalDate::ofEpochDay),
                    onOpenSession = { navController.navigate(SessionDetailRoute(it)) },
                    onOpenRun = { navController.navigate(RunLogRoute(it)) },
                    onOpenCheckIn = { navController.navigate(CheckInRoute(it.toEpochDay())) },
                )
            }
            composable<ProgressRoute> { ProgressScreen() }
            composable<RecoveryRoute> {
                RecoveryScreen(onOpenCheckIn = { navController.navigate(CheckInRoute(it.toEpochDay())) })
            }

            composable<SettingsRoute> {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onManageExercises = { navController.navigate(ExercisesRoute) },
                    onManageSymptoms = { navController.navigate(SymptomsRoute) },
                    onManageRoutines = { navController.navigate(RoutinesRoute) },
                )
            }
            composable<ExercisesRoute> { ExercisesScreen(onBack = { navController.popBackStack() }) }
            composable<SymptomsRoute> { SymptomsScreen(onBack = { navController.popBackStack() }) }
            composable<RoutinesRoute> {
                RoutinesScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(RoutineEditRoute(it)) },
                    onCreate = { navController.navigate(RoutineEditRoute(NEW_ROUTINE_ID)) },
                )
            }
            composable<StrengthSessionRoute> { entry ->
                StrengthSessionScreen(
                    sessionId = entry.toRoute<StrengthSessionRoute>().sessionId,
                    onBack = { navController.popBackStack() },
                    onClosed = { navController.popBackStack() },
                )
            }
            composable<SessionDetailRoute> { entry ->
                val sessionId = entry.toRoute<SessionDetailRoute>().sessionId
                SessionDetailScreen(
                    sessionId = sessionId,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(StrengthSessionRoute(sessionId)) },
                )
            }
            composable<RunLogRoute> { entry ->
                RunLogScreen(
                    runId = entry.toRoute<RunLogRoute>().runId,
                    onClose = { navController.popBackStack() },
                )
            }
            composable<CheckInRoute> { entry ->
                CheckInScreen(
                    date = LocalDate.ofEpochDay(entry.toRoute<CheckInRoute>().epochDay),
                    onClose = { navController.popBackStack() },
                )
            }
            composable<RoutineEditRoute> { entry ->
                RoutineEditScreen(
                    routineId = entry.toRoute<RoutineEditRoute>().routineId,
                    onDone = { navController.popBackStack() },
                )
            }
        }
    }
}

@Composable
private fun BottomBar(
    navController: NavHostController,
    currentDestination: NavDestination?,
) {
    NavigationBar(containerColor = Active40Colors.inkDark) {
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = currentDestination.isOn(destination),
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(painterResource(destination.iconRes), contentDescription = null) },
                label = {
                    // Alapalkin kohteilla on kiinteä leveys, joten otsikon kasvu rajataan (ikonit ja nimet säilyvät).
                    val style = MaterialTheme.typography.labelMedium
                    Text(
                        stringResource(destination.labelRes),
                        style = style.copy(fontSize = cappedFontSize(style.fontSize, NAV_LABEL_MAX_SCALE)),
                        maxLines = 1,
                    )
                },
                colors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = Active40Colors.inkDark,
                        selectedTextColor = Active40Colors.onBackground,
                        indicatorColor = Active40Colors.surfaceWhite,
                        unselectedIconColor = Active40Colors.onBackgroundMuted,
                        unselectedTextColor = Active40Colors.onBackgroundMuted,
                    ),
            )
        }
    }
}
