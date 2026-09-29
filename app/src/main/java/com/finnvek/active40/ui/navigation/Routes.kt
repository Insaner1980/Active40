package com.finnvek.active40.ui.navigation

import kotlinx.serialization.Serializable

// Alanavigaation välilehdet
@Serializable
data object TodayRoute

/** epochDay = NO_DAY avaa kuukausinäkymän ilman valittua päivää. */
@Serializable
data class HistoryRoute(
    val epochDay: Long = NO_DAY,
)

const val NO_DAY = Long.MIN_VALUE

@Serializable
data object ProgressRoute

@Serializable
data object RecoveryRoute

// Koko näytön näkymät ilman alanavigaatiota
@Serializable
data object SettingsRoute

@Serializable
data object ExercisesRoute

@Serializable
data object SymptomsRoute

@Serializable
data object RoutinesRoute

/** routineId = NEW_ROUTINE_ID luo uuden rutiinin. */
@Serializable
data class RoutineEditRoute(
    val routineId: Long,
)

@Serializable
data class StrengthSessionRoute(
    val sessionId: Long,
)

/** runId = NEW_RUN_ID kirjaa uuden juoksun. */
@Serializable
data class RunLogRoute(
    val runId: Long,
)

/** Päivän kirjaus päivälle (epochDay). Olemassa oleva kirjaus avautuu muokattavaksi. */
@Serializable
data class CheckInRoute(
    val epochDay: Long,
)

@Serializable
data class SessionDetailRoute(
    val sessionId: Long,
)
