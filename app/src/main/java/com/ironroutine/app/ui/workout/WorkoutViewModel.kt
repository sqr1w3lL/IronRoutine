package com.ironroutine.app.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ironroutine.app.data.db.ExerciseResultEntity
import com.ironroutine.app.data.db.ProgressEntity
import com.ironroutine.app.data.db.SessionEntity
import com.ironroutine.app.data.db.SetLogEntity
import com.ironroutine.app.data.program.ExerciseDef
import com.ironroutine.app.data.program.PlannedExercise
import com.ironroutine.app.data.program.ProgramData
import com.ironroutine.app.data.program.WorkoutDay
import com.ironroutine.app.data.repo.WorkoutRepository
import com.ironroutine.app.data.settings.AppSettings
import com.ironroutine.app.domain.Progression
import com.ironroutine.app.domain.Schedule
import com.ironroutine.app.domain.SessionHistory
import com.ironroutine.app.domain.Suggestion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ExerciseUi(
    val planned: PlannedExercise,
    val def: ExerciseDef,
    val sets: List<SetLogEntity>,
    val goodTechnique: Boolean,
    val variant: Int,
    val suggestion: Suggestion,
    val extraReps: Int,
    val lastHistory: SessionHistory?
) {
    val doneCount: Int get() = sets.count { it.done }
    val allDone: Boolean get() = sets.isNotEmpty() && sets.all { it.done }
}

data class WorkoutUi(
    val loading: Boolean = true,
    val day: WorkoutDay? = null,
    val settings: AppSettings = AppSettings(),
    val session: SessionEntity? = null,
    val otherActive: SessionEntity? = null,
    val otherActiveDay: WorkoutDay? = null,
    val exercises: List<ExerciseUi> = emptyList()
) {
    val plannedSets: Int get() = exercises.sumOf { it.planned.sets }
    val doneSets: Int get() = exercises.sumOf { it.doneCount }
}

data class RestState(val endsAtMs: Long, val totalSec: Int)

private class Base(
    val plan: List<WorkoutDay>,
    val settings: AppSettings,
    val active: SessionEntity?,
    val progress: List<ProgressEntity>
)

private class Extra(
    val logs: List<SetLogEntity>,
    val results: List<ExerciseResultEntity>,
    val histories: Map<String, List<SessionHistory>>
)

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModel(
    private val repo: WorkoutRepository,
    private val dayId: String,
    private val autostart: Boolean
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()
    private val histories = MutableStateFlow<Map<String, List<SessionHistory>>>(emptyMap())
    private val historiesLoaded = MutableStateFlow(false)

    private val _rest = MutableStateFlow<RestState?>(null)
    val rest: StateFlow<RestState?> = _rest

    private val _finished = MutableStateFlow<SessionEntity?>(null)
    /** Итог только что завершённой тренировки (для диалога «Готово»). */
    val finished: StateFlow<SessionEntity?> = _finished

    private val activeFlow = repo.activeSession
    private val logFlow = activeFlow.flatMapLatest { s ->
        if (s == null) flowOf(emptyList<SetLogEntity>()) else repo.setsForSession(s.id)
    }
    private val resultFlow = activeFlow.flatMapLatest { s ->
        if (s == null) flowOf(emptyList<ExerciseResultEntity>()) else repo.resultsForSession(s.id)
    }

    private val baseFlow = combine(repo.plan, repo.settings, activeFlow, repo.allProgress) { plan, settings, active, progress ->
        Base(plan, settings, active, progress)
    }
    private val extraFlow = combine(logFlow, resultFlow, histories) { logs, results, h -> Extra(logs, results, h) }

    val ui: StateFlow<WorkoutUi> = combine(baseFlow, extraFlow, historiesLoaded) { b, e, loaded ->
        build(b, e, loaded)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutUi())

    init {
        viewModelScope.launch {
            val day = repo.plan.first().firstOrNull { it.id == dayId }
            if (day != null) {
                histories.value = day.exercises.associate { it.exerciseId to repo.history(dayId, it.exerciseId, 3) }
            }
            historiesLoaded.value = true
            if (autostart) startWorkout(replaceOther = false)
        }
    }

    private fun build(b: Base, e: Extra, loaded: Boolean): WorkoutUi {
        val day = b.plan.firstOrNull { it.id == dayId } ?: return WorkoutUi(loading = !loaded, settings = b.settings)
        val session = b.active?.takeIf { it.dayId == dayId }
        val other = b.active?.takeIf { it.dayId != dayId }
        val exercises = day.exercises.map { pe ->
            val def = ProgramData.exercise(pe.exerciseId)
            val progress = b.progress.firstOrNull { it.dayId == dayId && it.exerciseId == pe.exerciseId }
            val hist = e.histories[pe.exerciseId].orEmpty()
            val result = e.results.firstOrNull { it.exerciseId == pe.exerciseId }
            ExerciseUi(
                planned = pe,
                def = def,
                sets = if (session == null) emptyList()
                else e.logs.filter { it.exerciseId == pe.exerciseId }.sortedBy { it.setIndex }.take(pe.sets),
                goodTechnique = result?.goodTechnique ?: true,
                variant = result?.variant ?: 0,
                suggestion = Progression.suggest(pe, def, hist, progress, b.settings, today),
                extraReps = progress?.extraReps ?: 0,
                lastHistory = hist.firstOrNull()
            )
        }
        return WorkoutUi(
            loading = false,
            day = day,
            settings = b.settings,
            session = session,
            otherActive = other,
            otherActiveDay = other?.let { o -> b.plan.firstOrNull { it.id == o.dayId } },
            exercises = exercises
        )
    }

    fun startWorkout(replaceOther: Boolean) {
        viewModelScope.launch {
            val day = repo.plan.first().firstOrNull { it.id == dayId } ?: return@launch
            val settings = repo.settingsNow()
            val active = repo.activeSession.first()
            if (active != null) {
                if (active.dayId == dayId) return@launch
                if (!replaceOther) return@launch
                repo.cancelSession(active.id)
            }
            val doneKeys = Schedule.doneKeys(repo.completedSessions.first())
            val slot = Schedule.resolveSlot(day, LocalDate.now(), doneKeys)
            val progressList = repo.allProgress.first()
            val hist = histories.value
            val sets = day.exercises.flatMap { pe ->
                val def = ProgramData.exercise(pe.exerciseId)
                val prog = progressList.firstOrNull { it.dayId == dayId && it.exerciseId == pe.exerciseId }
                Progression.prefill(pe, def, hist[pe.exerciseId].orEmpty(), prog, settings)
                    .mapIndexed { i, pf -> SetLogEntity(0L, pe.exerciseId, i, pf.reps, pf.weightKg, false) }
            }
            repo.startSession(day, slot.toEpochDay(), sets)
        }
    }

    fun updateSet(exerciseId: String, setIndex: Int, reps: Int, weightKg: Double) {
        val sid = ui.value.session?.id ?: return
        viewModelScope.launch { repo.updateSetValues(sid, exerciseId, setIndex, reps, weightKg) }
    }

    fun toggleDone(exerciseId: String, setIndex: Int) {
        val state = ui.value
        val sid = state.session?.id ?: return
        val ex = state.exercises.firstOrNull { it.planned.exerciseId == exerciseId } ?: return
        val set = ex.sets.firstOrNull { it.setIndex == setIndex } ?: return
        val nowDone = !set.done
        viewModelScope.launch {
            repo.updateSetDone(sid, exerciseId, setIndex, nowDone)
            val last = state.exercises.last()
            val isVeryLast = ex === last && setIndex == ex.planned.sets - 1
            if (nowDone && state.settings.autoRest && !isVeryLast) {
                startRest(ex.planned.restMax)
            }
        }
    }

    fun setTechnique(exerciseId: String, good: Boolean) = saveResult(exerciseId) { it.copy(goodTechnique = good) }

    fun setVariant(exerciseId: String, variant: Int) = saveResult(exerciseId) { it.copy(variant = variant) }

    private fun saveResult(exerciseId: String, change: (ExerciseResultEntity) -> ExerciseResultEntity) {
        val sid = ui.value.session?.id ?: return
        val ex = ui.value.exercises.firstOrNull { it.planned.exerciseId == exerciseId } ?: return
        val current = ExerciseResultEntity(sid, exerciseId, ex.goodTechnique, ex.variant)
        viewModelScope.launch { repo.saveResult(change(current)) }
    }

    /** Пользователь принял или отклонил рекомендацию по нагрузке. */
    fun resolveSuggestion(exerciseId: String, accept: Boolean) {
        val state = ui.value
        val ex = state.exercises.firstOrNull { it.planned.exerciseId == exerciseId } ?: return
        val sug = ex.suggestion
        if (!sug.needsConfirmation) return
        viewModelScope.launch {
            repo.resolveSuggestion(dayId, exerciseId, sug.basedOnSessionId, accept, sug.newWeightKg, sug.newExtraReps)
            // Если тренировка уже идёт, подставляем новый вес в ещё не выполненные подходы.
            val sid = state.session?.id
            val newWeight = sug.newWeightKg
            if (accept && sid != null && newWeight != null) {
                ex.sets.filter { !it.done }.forEach {
                    repo.updateSetValues(sid, exerciseId, it.setIndex, it.reps, newWeight)
                }
            }
        }
    }

    fun finish(feeling: Int) {
        val state = ui.value
        val session = state.session ?: return
        val day = state.day ?: return
        viewModelScope.launch {
            _rest.value = null
            _finished.value = repo.finishSession(session.id, feeling, day)
        }
    }

    fun cancelWorkout() {
        val session = ui.value.session ?: return
        viewModelScope.launch {
            _rest.value = null
            repo.cancelSession(session.id)
        }
    }

    fun startRest(seconds: Int) {
        _rest.value = RestState(System.currentTimeMillis() + seconds * 1000L, seconds)
    }

    fun addRest(seconds: Int) {
        val r = _rest.value ?: return
        _rest.value = RestState(r.endsAtMs + seconds * 1000L, r.totalSec + seconds)
    }

    fun stopRest() {
        _rest.value = null
    }
}
