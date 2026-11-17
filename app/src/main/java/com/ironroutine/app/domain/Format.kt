package com.ironroutine.app.domain

import com.ironroutine.app.data.program.ExerciseDef
import com.ironroutine.app.data.program.MeasureType
import com.ironroutine.app.data.program.PlannedExercise
import com.ironroutine.app.data.settings.WeightUnit
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object Units {
    private const val KG_TO_LB = 2.2046226218

    fun toDisplay(kg: Double, unit: WeightUnit): Double = if (unit == WeightUnit.KG) kg else kg * KG_TO_LB

    fun fromDisplay(value: Double, unit: WeightUnit): Double = if (unit == WeightUnit.KG) value else value / KG_TO_LB

    fun label(unit: WeightUnit): String = if (unit == WeightUnit.KG) "кг" else "фунт"

    /** Вес в кг -> строка в выбранных единицах, без единицы измерения. */
    fun format(kg: Double, unit: WeightUnit): String = formatNumber(toDisplay(kg, unit))

    fun formatWithLabel(kg: Double, unit: WeightUnit): String = format(kg, unit) + " " + label(unit)
}

/** Число с запятой, максимум один знак после запятой, без хвоста ",0". */
fun formatNumber(v: Double): String {
    val rounded = (v * 10).roundToLong() / 10.0
    return if (abs(rounded - rounded.toLong()) < 0.0001) {
        rounded.toLong().toString()
    } else {
        String.format(Locale.US, "%.1f", rounded).replace('.', ',')
    }
}

fun parseNumber(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

fun pluralRu(n: Int, one: String, few: String, many: String): String {
    val m100 = n % 100
    val m10 = n % 10
    return when {
        m100 in 11..14 -> many
        m10 == 1 -> one
        m10 in 2..4 -> few
        else -> many
    }
}

fun pluralSets(n: Int) = "$n " + pluralRu(n, "подход", "подхода", "подходов")

object Ru {
    private val months = listOf(
        "января", "февраля", "марта", "апреля", "мая", "июня",
        "июля", "августа", "сентября", "октября", "ноября", "декабря"
    )
    private val monthsNom = listOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    )
    private val weekdays = listOf(
        "понедельник", "вторник", "среда", "четверг", "пятница", "суббота", "воскресенье"
    )
    private val weekdaysShort = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    fun weekday(d: LocalDate): String = weekdays[d.dayOfWeek.value - 1]
    fun weekdayCap(d: LocalDate): String = weekday(d).replaceFirstChar { it.uppercase() }
    fun weekdayShort(index: Int): String = weekdaysShort[index]

    /** "16 ноября" */
    fun dayMonth(d: LocalDate): String = "${d.dayOfMonth} ${months[d.monthValue - 1]}"

    /** "Понедельник, 16 ноября" */
    fun full(d: LocalDate): String = "${weekdayCap(d)}, ${dayMonth(d)}"

    /** "Ноябрь 2026" */
    fun monthYear(year: Int, month: Int): String = "${monthsNom[month - 1]} $year"

    /** "16.11" */
    fun dm(d: LocalDate): String = "%02d.%02d".format(d.dayOfMonth, d.monthValue)

    /** "16.11.2026" */
    fun dmy(d: LocalDate): String = "%02d.%02d.%d".format(d.dayOfMonth, d.monthValue, d.year)
}

fun formatDuration(totalSec: Int): String {
    val minutes = totalSec / 60
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "$h ч ${"%02d".format(m)} мин" else "$minutes мин"
}

fun formatClock(totalSec: Int): String {
    val s = totalSec.coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

/** "10–15" или "12" */
fun rangeText(min: Int, max: Int): String = if (min == max) "$min" else "$min–$max"

/** Целевое количество: "10–15 повторений" / "30–45 с" / "12 повторений на каждую руку". */
fun targetText(plan: PlannedExercise, def: ExerciseDef, extraReps: Int = 0): String {
    val range = rangeText(plan.repsMin, plan.repsMax + extraReps)
    val base = if (def.measure == MeasureType.SECONDS) "$range с" else
        "$range " + pluralRu(plan.repsMax + extraReps, "повторение", "повторения", "повторений")
    return if (def.perSide != null) "$base ${def.perSide}" else base
}

fun restText(plan: PlannedExercise): String =
    if (plan.restMin == plan.restMax) "${plan.restMin} с" else "${plan.restMin}–${plan.restMax} с"
