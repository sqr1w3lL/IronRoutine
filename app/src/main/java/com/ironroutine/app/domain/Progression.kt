package com.ironroutine.app.domain

import com.ironroutine.app.data.db.ProgressEntity
import com.ironroutine.app.data.program.Equipment
import com.ironroutine.app.data.program.ExerciseDef
import com.ironroutine.app.data.program.MeasureType
import com.ironroutine.app.data.program.PlannedExercise
import com.ironroutine.app.data.settings.AppSettings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.floor
import kotlin.math.roundToLong

data class SetResult(val reps: Int, val weightKg: Double)

/** Итог упражнения в одной завершённой тренировке (только выполненные подходы). */
data class SessionHistory(
    val sessionId: Long,
    val finishedAt: Long,
    val feeling: Int,
    val goodTechnique: Boolean,
    val variant: Int,
    val sets: List<SetResult>
) {
    val maxWeight: Double get() = sets.maxOfOrNull { it.weightKg } ?: 0.0

    fun volume(usesWeight: Boolean): Double =
        sets.sumOf { it.reps * (if (usesWeight && it.weightKg > 0) it.weightKg else 1.0) }
}

enum class SuggestionType { FIRST, KEEP, GOAL, INCREASE_WEIGHT, INCREASE_REPS, REDUCE_WEIGHT, REDUCE_REPS }

data class Suggestion(
    val type: SuggestionType,
    val message: String,
    val newWeightKg: Double? = null,
    val newExtraReps: Int? = null,
    val basedOnSessionId: Long = 0L
) {
    /** Рекомендации, которые пользователь должен подтвердить или отклонить. */
    val needsConfirmation: Boolean
        get() = type == SuggestionType.INCREASE_WEIGHT || type == SuggestionType.INCREASE_REPS ||
            type == SuggestionType.REDUCE_WEIGHT || type == SuggestionType.REDUCE_REPS

    val isIncrease: Boolean
        get() = type == SuggestionType.INCREASE_WEIGHT || type == SuggestionType.INCREASE_REPS
}

data class SetPrefill(val reps: Int, val weightKg: Double)

/**
 * Движок постепенного увеличения нагрузки.
 *
 * Смотрит только на реальные результаты последних выполненных аналогичных тренировок
 * (тот же день программы). Пропущенные тренировки и наступление новой недели на решение
 * не влияют, нагрузка никогда не «компенсируется» и не растёт автоматически.
 */
object Progression {

    fun repStep(def: ExerciseDef): Int = if (def.measure == MeasureType.SECONDS) 5 else 2

    /** Максимум расширения диапазона сверх верхней границы (3 шага). */
    fun maxExtra(def: ExerciseDef): Int = repStep(def) * 3

    fun canChangeWeight(def: ExerciseDef, s: AppSettings): Boolean = def.usesWeight && when (def.equipment) {
        Equipment.BARBELL -> true
        Equipment.DUMBBELL -> s.dumbbellAdjustable
        else -> false
    }

    fun weightStep(def: ExerciseDef, s: AppSettings): Double =
        if (def.equipment == Equipment.BARBELL) s.barbellStepKg else s.dumbbellStepKg

    fun defaultWeight(def: ExerciseDef, s: AppSettings): Double = if (!def.usesWeight) 0.0 else when (def.equipment) {
        Equipment.DUMBBELL -> s.dumbbellKg
        Equipment.BARBELL -> s.barbellKg
        else -> 0.0
    }

    private fun round1(v: Double): Double = (v * 10).roundToLong() / 10.0

    private fun repWord(def: ExerciseDef, n: Int): String =
        if (def.measure == MeasureType.SECONDS) pluralRu(n, "секунду", "секунды", "секунд")
        else pluralRu(n, "повторение", "повторения", "повторений")

    /** Начальные значения подходов: прошлые повторения (или нижняя граница) и рабочий вес. */
    fun prefill(
        plan: PlannedExercise,
        def: ExerciseDef,
        history: List<SessionHistory>,
        progress: ProgressEntity?,
        s: AppSettings
    ): List<SetPrefill> {
        val last = history.firstOrNull()
        val weight = if (!def.usesWeight) 0.0 else {
            progress?.workingWeightKg
                ?: last?.sets?.lastOrNull()?.weightKg?.takeIf { it > 0.0 }
                ?: defaultWeight(def, s)
        }
        return List(plan.sets) { i ->
            SetPrefill(reps = last?.sets?.getOrNull(i)?.reps ?: plan.repsMin, weightKg = weight)
        }
    }

    fun suggest(
        plan: PlannedExercise,
        def: ExerciseDef,
        history: List<SessionHistory>,
        progress: ProgressEntity?,
        s: AppSettings,
        today: LocalDate
    ): Suggestion {
        val last = history.firstOrNull() ?: return Suggestion(
            SuggestionType.FIRST,
            if (def.usesWeight)
                "Первая тренировка: выбери вес, при котором последние повторения даются тяжело, но техника остаётся чистой."
            else
                "Первая тренировка: работай в чистой технике и оставляй 1–2 повторения в запасе."
        )

        // Рекомендация по этой тренировке уже обработана пользователем.
        if (progress != null && progress.handledSessionId == last.sessionId) {
            return if (progress.handledAccepted)
                Suggestion(SuggestionType.GOAL, "Нагрузка обновлена по рекомендации. Работаем по новой цели.", basedOnSessionId = last.sessionId)
            else
                Suggestion(SuggestionType.KEEP, "Нагрузка остаётся прежней. Следующая рекомендация появится после новой тренировки.", basedOnSessionId = last.sessionId)
        }

        val extra = progress?.extraReps ?: 0
        val top = plan.repsMax + extra
        val low = plan.repsMin
        val workSets = last.sets.take(plan.sets)
        val completed = last.sets.size >= plan.sets && workSets.all { it.reps >= low }
        val hitTop = completed && workSets.all { it.reps >= top }
        val id = last.sessionId

        // 1. Долгий перерыв: возвращаемся без прибавки.
        val lastDate = Instant.ofEpochMilli(last.finishedAt).atZone(ZoneId.systemDefault()).toLocalDate()
        val gap = ChronoUnit.DAYS.between(lastDate, today)
        if (gap >= 14) {
            return Suggestion(
                SuggestionType.KEEP,
                "Был перерыв $gap ${pluralRu(gap.toInt(), "день", "дня", "дней")}. Вернись к прежней нагрузке без прибавки и ориентируйся на самочувствие.",
                basedOnSessionId = id
            )
        }

        // 2. Ухудшение результатов или самочувствия.
        fun worse(i: Int): Boolean {
            val h = history.getOrNull(i) ?: return false
            val prev = history.getOrNull(i + 1)
            return h.feeling == 1 ||
                (prev != null && h.volume(def.usesWeight) < prev.volume(def.usesWeight) * 0.9)
        }
        if (worse(0)) {
            if (worse(1)) {
                val w = last.maxWeight
                val step = weightStep(def, s)
                if (canChangeWeight(def, s) && w > step) {
                    var nw = floor(w * 0.9 / step) * step
                    if (nw >= w) nw -= step
                    nw = round1(nw.coerceAtLeast(step))
                    return Suggestion(
                        SuggestionType.REDUCE_WEIGHT,
                        "Два занятия подряд результат или самочувствие были хуже обычного. Предлагаю уменьшить вес до ${Units.formatWithLabel(nw, s.unit)} и вернуть уверенную технику.",
                        newWeightKg = nw, basedOnSessionId = id
                    )
                }
                if (extra > 0) {
                    val ne = (extra - repStep(def)).coerceAtLeast(0)
                    return Suggestion(
                        SuggestionType.REDUCE_REPS,
                        "Два занятия подряд результат или самочувствие были хуже обычного. Предлагаю вернуть цель к ${rangeText(plan.repsMin, plan.repsMax + ne)}.",
                        newExtraReps = ne, basedOnSessionId = id
                    )
                }
                return Suggestion(
                    SuggestionType.KEEP,
                    "Результат снижается второй раз подряд. Нагрузку не меняем: отдыхай между подходами по верхней границе и уделяй время сну и восстановлению.",
                    basedOnSessionId = id
                )
            }
            return Suggestion(
                SuggestionType.KEEP,
                "На прошлой тренировке результат или самочувствие были хуже обычного. Нагрузку не увеличиваем — сохрани её и дай телу восстановиться.",
                basedOnSessionId = id
            )
        }

        // 3. Техника.
        if (!last.goodTechnique) {
            return Suggestion(
                SuggestionType.KEEP,
                "В прошлый раз техника страдала. Нагрузку не увеличиваем — сосредоточься на качестве движения.",
                basedOnSessionId = id
            )
        }

        // 4. План не выполнен.
        if (!completed) {
            val reason = if (last.sets.size < plan.sets)
                "выполнено ${last.sets.size} из ${plan.sets} подходов"
            else "в некоторых подходах меньше ${low} ${repWord(def, low)}"
            return Suggestion(
                SuggestionType.KEEP,
                "План прошлой тренировки не выполнен ($reason). Нагрузку не увеличиваем — цель прежняя.",
                basedOnSessionId = id
            )
        }

        // 5. Всё выполнено на верхней границе — предлагаем прибавку.
        if (hitTop) {
            if (last.variant != 0) {
                return Suggestion(
                    SuggestionType.GOAL,
                    "Облегчённый вариант даётся уверенно. Попробуй перейти на обычное выполнение и сделай столько чистых повторений, сколько получится.",
                    basedOnSessionId = id
                )
            }
            val minReps = workSets.minOf { it.reps }
            val done = "На прошлой тренировке ты выполнил ${pluralSets(workSets.size)} по $minReps ${repWord(def, minReps)}."
            if (canChangeWeight(def, s)) {
                val baseWeight = if (last.maxWeight > 0) last.maxWeight else defaultWeight(def, s)
                val nw = round1(baseWeight + weightStep(def, s))
                return Suggestion(
                    SuggestionType.INCREASE_WEIGHT,
                    "$done Попробуй немного увеличить нагрузку до ${Units.formatWithLabel(nw, s.unit)}, если техника остаётся правильной.",
                    newWeightKg = nw, basedOnSessionId = id
                )
            }
            if (extra < maxExtra(def)) {
                val ne = extra + repStep(def)
                return Suggestion(
                    SuggestionType.INCREASE_REPS,
                    "$done Вес увеличить нельзя, поэтому добавим повторения: новая цель — до ${top + repStep(def)} ${repWord(def, top + repStep(def))}, если техника остаётся правильной.",
                    newExtraReps = ne, basedOnSessionId = id
                )
            }
            return Suggestion(
                SuggestionType.GOAL,
                "$done Ты дошёл до предела диапазона. Усложни упражнение: опускайся медленнее (3 секунды) или сделай паузу в самой сложной точке.",
                basedOnSessionId = id
            )
        }

        // 6. План выполнен, но до верхней границы ещё есть куда расти.
        val reps = workSets.joinToString(", ") { it.reps.toString() }
        return Suggestion(
            SuggestionType.GOAL,
            "Прошлый раз: $reps. Цель сегодня — добавить по одному повторению в самых слабых подходах, оставаясь в диапазоне ${rangeText(low, top)}. Нагрузку пока не увеличиваем.",
            basedOnSessionId = id
        )
    }
}
