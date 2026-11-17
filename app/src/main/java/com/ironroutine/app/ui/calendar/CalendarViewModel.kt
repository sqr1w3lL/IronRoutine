package com.ironroutine.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ironroutine.app.data.db.SessionEntity
import com.ironroutine.app.data.program.WorkoutDay
import com.ironroutine.app.data.repo.WorkoutRepository
import com.ironroutine.app.data.settings.AppSettings
import com.ironroutine.app.ui.todayFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class CalendarData(
    val plan: List<WorkoutDay>,
    val sessions: List<SessionEntity>,
    val settings: AppSettings,
    val today: LocalDate
)

class CalendarViewModel(repo: WorkoutRepository) : ViewModel() {
    val data: StateFlow<CalendarData?> = combine(
        repo.plan, repo.completedSessions, repo.settings, todayFlow
    ) { plan, sessions, settings, today ->
        CalendarData(plan, sessions, settings, today)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
