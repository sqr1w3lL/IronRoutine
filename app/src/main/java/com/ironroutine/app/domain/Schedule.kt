package com.ironroutine.app.domain

import com.ironroutine.app.data.db.SessionEntity
import com.ironroutine.app.data.program.WorkoutDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

data class Slot(val date: LocalDate, val day: WorkoutDay)

enum class SlotStatus { DONE, MISSED, TODAY, UPCOMING }

/** Ключ слота: (dayId, epochDay). */
typealias SlotKey = Pair<String, Long>

object Schedule {

    fun dayOn(date: LocalDate, plan: List<WorkoutDay>): WorkoutDay? =
        plan.firstOrNull { it.weekday == date.dayOfWeek }

    fun doneKeys(completed: List<SessionEntity>): Set<SlotKey> =
        completed.map { it.dayId to it.slotDate }.toSet()

    fun status(slot: Slot, today: LocalDate, done: Set<SlotKey>): SlotStatus = when {
        (slot.day.id to slot.date.toEpochDay()) in done -> SlotStatus.DONE
        slot.date.isBefore(today) -> SlotStatus.MISSED
        slot.date == today -> SlotStatus.TODAY
        else -> SlotStatus.UPCOMING
    }

    fun slotsBetween(from: LocalDate, to: LocalDate, plan: List<WorkoutDay>): List<Slot> {
        val out = ArrayList<Slot>()
        var d = from
        while (!d.isAfter(to)) {
            dayOn(d, plan)?.let { out += Slot(d, it) }
            d = d.plusDays(1)
        }
        return out
    }

    /** Ближайшая невыполненная тренировка начиная с сегодняшнего дня. */
    fun nextSlot(today: LocalDate, plan: List<WorkoutDay>, done: Set<SlotKey>): Slot? {
        for (i in 0..14) {
            val d = today.plusDays(i.toLong())
            val day = dayOn(d, plan) ?: continue
            if ((day.id to d.toEpochDay()) !in done) return Slot(d, day)
        }
        return null
    }

    /** Текущая серия подряд выполненных запланированных тренировок. */
    fun streak(today: LocalDate, start: LocalDate, plan: List<WorkoutDay>, done: Set<SlotKey>): Int {
        var count = 0
        var d = today
        while (!d.isBefore(start)) {
            val day = dayOn(d, plan)
            if (day != null) {
                val isDone = (day.id to d.toEpochDay()) in done
                if (isDone) count++
                else if (d != today) break
            }
            d = d.minusDays(1)
        }
        return count
    }

    fun missedCount(today: LocalDate, start: LocalDate, plan: List<WorkoutDay>, done: Set<SlotKey>): Int =
        slotsBetween(start, today.minusDays(1), plan)
            .count { (it.day.id to it.date.toEpochDay()) !in done }

    /**
     * Какой слот закрывает тренировка этого дня, начатая сегодня:
     * сегодняшний, если сегодня «её» день; иначе последний пропущенный за неделю;
     * иначе ближайший будущий.
     */
    fun resolveSlot(day: WorkoutDay, today: LocalDate, done: Set<SlotKey>): LocalDate {
        if (today.dayOfWeek == day.weekday) return today
        val candidates = (-6..7).map { today.plusDays(it.toLong()) }
            .filter { it.dayOfWeek == day.weekday && (day.id to it.toEpochDay()) !in done }
        val past = candidates.filter { it.isBefore(today) }.maxByOrNull { it.toEpochDay() }
        return past ?: candidates.filter { it.isAfter(today) }.minByOrNull { it.toEpochDay() } ?: today
    }

    private fun weekStart(d: LocalDate): LocalDate = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /** Номер недели программы (с 1). 0 — программа ещё не началась. */
    fun weekNumber(start: LocalDate, date: LocalDate): Int {
        if (date.isBefore(start)) return 0
        return (ChronoUnit.DAYS.between(weekStart(start), weekStart(date)) / 7).toInt() + 1
    }

    fun totalWeeks(start: LocalDate, end: LocalDate): Int = weekNumber(start, end).coerceAtLeast(1)

    fun mondayOf(d: LocalDate): LocalDate = weekStart(d)
}
