package com.ironroutine.app.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ironroutine.app.data.db.MeasurementEntity
import com.ironroutine.app.data.db.SessionEntity
import com.ironroutine.app.data.db.SetLogEntity
import com.ironroutine.app.data.program.ProgramData
import com.ironroutine.app.data.program.WorkoutDay
import com.ironroutine.app.data.repo.WorkoutRepository
import com.ironroutine.app.data.settings.AppSettings
import com.ironroutine.app.domain.Ru
import com.ironroutine.app.domain.Schedule
import com.ironroutine.app.ui.components.ChartPoint
import com.ironroutine.app.ui.todayFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ExerciseSeries(
    val exerciseId: String,
    val name: String,
    val usesWeight: Boolean,
    val seconds: Boolean,
    /** Лучший вес за тренировку, кг. */
    val weights: List<ChartPoint>,
    /** Сумма повторений (секунд) за тренировку. */
    val reps: List<ChartPoint>
)

data class ProgressUi(
    val settings: AppSettings,
    val plan: List<WorkoutDay>,
    val totalWorkouts: Int,
    val missed: Int,
    val exercisesDone: Int,
    val totalDurationSec: Int,
    val weeklyBars: List<ChartPoint>,
    val series: List<ExerciseSeries>,
    val measurements: List<MeasurementEntity>,
    val history: List<SessionEntity>
)

class ProgressViewModel(private val repo: WorkoutRepository) : ViewModel() {

    private val dataFlow = combine(repo.completedSessions, repo.allSets, repo.measurements) { s, sets, m -> Triple(s, sets, m) }
    private val configFlow = combine(repo.plan, repo.settings, todayFlow) { p, st, t -> Triple(p, st, t) }

    val ui: StateFlow<ProgressUi?> = combine(dataFlow, configFlow) { data, config ->
        build(data.first, data.second, data.third, config.first, config.second, config.third)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun dateOf(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

    private fun build(
        sessions: List<SessionEntity>,
        allSets: List<SetLogEntity>,
        measurements: List<MeasurementEntity>,
        plan: List<WorkoutDay>,
        settings: AppSettings,
        today: LocalDate
    ): ProgressUi {
        val done = sessions.associateBy { it.id }
        val sets = allSets.filter { it.sessionId in done }

        // Выполненные упражнения: все подходы упражнения в тренировке отмечены.
        val exercisesDone = sets.groupBy { it.sessionId to it.exerciseId }
            .count { (_, list) -> list.isNotEmpty() && list.all { it.done } }

        val keys = Schedule.doneKeys(sessions)
        val missed = Schedule.missedCount(today, settings.programStart, plan, keys)

        val monday = Schedule.mondayOf(today)
        val bars = (7 downTo 0).map { i ->
            val m = monday.minusWeeks(i.toLong())
            val n = sessions.count { dateOf(it.finishedAt) in m..m.plusDays(6) }
            ChartPoint(Ru.dm(m), n.toDouble())
        }

        val ascending = sessions.sortedBy { it.finishedAt }
        val series = ProgramData.allExercises.mapNotNull { def ->
            val weights = ArrayList<ChartPoint>()
            val reps = ArrayList<ChartPoint>()
            for (s in ascending) {
                val mine = sets.filter { it.sessionId == s.id && it.exerciseId == def.id && it.done }
                if (mine.isEmpty()) continue
                val label = Ru.dm(dateOf(s.finishedAt))
                reps += ChartPoint(label, mine.sumOf { it.reps }.toDouble())
                val w = mine.maxOf { it.weightKg }
                if (def.usesWeight && w > 0) weights += ChartPoint(label, w)
            }
            if (reps.isEmpty()) null
            else ExerciseSeries(def.id, def.name, def.usesWeight, def.measure.name == "SECONDS", weights, reps)
        }

        return ProgressUi(
            settings = settings,
            plan = plan,
            totalWorkouts = sessions.size,
            missed = missed,
            exercisesDone = exercisesDone,
            totalDurationSec = sessions.sumOf { it.durationSec },
            weeklyBars = bars,
            series = series,
            measurements = measurements,
            history = sessions
        )
    }

    fun addMeasurement(weightKg: Double?, waist: Double?, chest: Double?, arm: Double?) {
        if (weightKg == null && waist == null && chest == null && arm == null) return
        viewModelScope.launch {
            repo.addMeasurement(
                MeasurementEntity(
                    date = LocalDate.now().toEpochDay(),
                    bodyWeightKg = weightKg, waistCm = waist, chestCm = chest, armCm = arm
                )
            )
        }
    }

    fun deleteMeasurement(id: Long) {
        viewModelScope.launch { repo.deleteMeasurement(id) }
    }
}
