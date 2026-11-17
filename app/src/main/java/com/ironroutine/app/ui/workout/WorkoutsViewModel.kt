package com.ironroutine.app.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ironroutine.app.data.db.SessionEntity
import com.ironroutine.app.data.program.WorkoutDay
import com.ironroutine.app.data.repo.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class WorkoutsUi(val plan: List<WorkoutDay>, val active: SessionEntity?)

class WorkoutsViewModel(repo: WorkoutRepository) : ViewModel() {
    val ui: StateFlow<WorkoutsUi?> = combine(repo.plan, repo.activeSession) { plan, active ->
        WorkoutsUi(plan, active)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
