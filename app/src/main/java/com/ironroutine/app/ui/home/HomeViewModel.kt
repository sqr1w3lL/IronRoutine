package com.ironroutine.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ironroutine.app.data.db.SessionEntity
import com.ironroutine.app.data.program.WorkoutDay
import com.ironroutine.app.data.repo.WorkoutRepository
import com.ironroutine.app.data.settings.AppSettings
import com.ironroutine.app.domain.Schedule
import com.ironroutine.app.domain.Slot
import com.ironroutine.app.domain.SlotStatus
import com.ironroutine.app.ui.components.ChartPoint
import com.ironroutine.app.domain.Ru
import com.ironroutine.app.ui.todayFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class DayCardUi(val day: WorkoutDay, val slotDate: LocalDate, val status: SlotStatus)

data class HomeUi(
    val today: LocalDate,
    val settings: AppSettings,
    val plan: List<WorkoutDay>,
    val todayDay: WorkoutDay?,
    val todayDone: Boolean,
    val nextSlot: Slot?,
    val active: WorkoutDay?,
    val activeSession: SessionEntity?,
    val completedCount: Int,
    val streak: Int,
    val missed: Int,
    val weekNumber: Int,
    val totalWeeks: Int,
    val programDone: Int,
    val programTotal: Int,
    val programStarted: Boolean,
    val programFinished: Boolean,
    val weekDone: Int,
    val weekPlanned: Int,
    val totalDurationSec: Int,
    val totalVolumeKg: Double,
    val weekBars: List<ChartPoint>,
    val dayCards: List<DayCardUi>
)

class HomeViewModel(repo: WorkoutRepository) : ViewModel() {

    val ui: StateFlow<HomeUi?> = combine(
        repo.plan, repo.settings, repo.completedSessions, repo.activeSession, todayFlow
    ) { plan, settings, completed, active, today ->
        build(plan, settings, completed, active, today)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun build(
        plan: List<WorkoutDay>,
        settings: AppSettings,
        completed: List<SessionEntity>,
        active: SessionEntity?,
        today: LocalDate
    ): HomeUi {
        val keys = Schedule.doneKeys(completed)
        val start = settings.programStart
        val end = settings.programEnd
        val inRange = completed.count { it.slotDate in start.toEpochDay()..end.toEpochDay() }
        val total = Schedule.slotsBetween(start, end, plan).size

        val todayDay = Schedule.dayOn(today, plan)
        val todayDone = todayDay != null && (todayDay.id to today.toEpochDay()) in keys
        val from = if (today.isBefore(start)) start else today
        val next = Schedule.nextSlot(from, plan, keys)

        val monday = Schedule.mondayOf(today)
        val weekSessions = completed.count { it.slotDate in monday.toEpochDay()..monday.plusDays(6).toEpochDay() }

        val bars = (5 downTo 0).map { i ->
            val m = monday.minusWeeks(i.toLong())
            val n = completed.count { it.slotDate in m.toEpochDay()..m.plusDays(6).toEpochDay() }
            ChartPoint(Ru.dm(m), n.toDouble())
        }

        val cards = plan.map { day ->
            val date = monday.plusDays((day.weekday.value - 1).toLong())
            DayCardUi(day, date, Schedule.status(Slot(date, day), today, keys))
        }

        return HomeUi(
            today = today,
            settings = settings,
            plan = plan,
            todayDay = todayDay,
            todayDone = todayDone,
            nextSlot = next,
            active = active?.let { a -> plan.firstOrNull { it.id == a.dayId } },
            activeSession = active,
            completedCount = completed.size,
            streak = Schedule.streak(today, start, plan, keys),
            missed = Schedule.missedCount(today, start, plan, keys),
            weekNumber = Schedule.weekNumber(start, today),
            totalWeeks = Schedule.totalWeeks(start, end),
            programDone = inRange,
            programTotal = total,
            programStarted = !today.isBefore(start),
            programFinished = today.isAfter(end),
            weekDone = weekSessions,
            weekPlanned = plan.size,
            totalDurationSec = completed.sumOf { it.durationSec },
            totalVolumeKg = completed.sumOf { it.volumeKg },
            weekBars = bars,
            dayCards = cards
        )
    }
}
