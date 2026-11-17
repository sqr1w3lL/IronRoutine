package com.ironroutine.app.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ironroutine.app.data.db.ExerciseResultEntity
import com.ironroutine.app.data.db.SessionEntity
import com.ironroutine.app.data.db.SetLogEntity
import com.ironroutine.app.data.program.WorkoutDay
import com.ironroutine.app.data.repo.WorkoutRepository
import com.ironroutine.app.data.settings.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SessionDetailUi(
    val session: SessionEntity?,
    val day: WorkoutDay?,
    val sets: List<SetLogEntity>,
    val results: List<ExerciseResultEntity>,
    val settings: AppSettings
)

class SessionDetailViewModel(private val repo: WorkoutRepository, private val sessionId: Long) : ViewModel() {

    private val first = combine(repo.observeSession(sessionId), repo.setsForSession(sessionId), repo.resultsForSession(sessionId)) { s, sets, r ->
        Triple(s, sets, r)
    }

    val ui: StateFlow<SessionDetailUi?> = combine(first, repo.plan, repo.settings) { f, plan, settings ->
        SessionDetailUi(f.first, plan.firstOrNull { it.id == f.first?.dayId }, f.second, f.third, settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.cancelSession(sessionId)
            onDone()
        }
    }
}
