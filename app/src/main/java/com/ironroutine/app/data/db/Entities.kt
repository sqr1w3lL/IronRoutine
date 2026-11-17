package com.ironroutine.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Одна тренировка (активная или завершённая). */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** id тренировочного дня: "push", "pull", "legs". */
    val dayId: String,
    /** Дата запланированного слота (epochDay), который закрывает эта тренировка. */
    val slotDate: Long,
    val startedAt: Long,
    /** Время последнего действия — нужно, чтобы не раздувать длительность забытой тренировки. */
    val lastActivityAt: Long,
    val finishedAt: Long = 0,
    val status: Int = STATUS_ACTIVE,
    val durationSec: Int = 0,
    /** 1 — плохое, 2 — нормальное, 3 — отличное самочувствие. */
    val feeling: Int = 2,
    val completedSets: Int = 0,
    val plannedSets: Int = 0,
    /** Σ (повторения × вес) по выполненным подходам, кг. */
    val volumeKg: Double = 0.0
) {
    companion object {
        const val STATUS_ACTIVE = 0
        const val STATUS_DONE = 1
    }
}

/** Один подход. Строки создаются при старте тренировки и обновляются при каждом изменении. */
@Entity(
    tableName = "set_logs",
    primaryKeys = ["sessionId", "exerciseId", "setIndex"],
    foreignKeys = [ForeignKey(
        entity = SessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class SetLogEntity(
    val sessionId: Long,
    val exerciseId: String,
    val setIndex: Int,
    /** Повторения (или секунды для упражнений на время). */
    val reps: Int,
    val weightKg: Double,
    val done: Boolean
)

/** Результат упражнения в тренировке: чистота техники и вариант выполнения. */
@Entity(
    tableName = "exercise_results",
    primaryKeys = ["sessionId", "exerciseId"],
    foreignKeys = [ForeignKey(
        entity = SessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class ExerciseResultEntity(
    val sessionId: Long,
    val exerciseId: String,
    val goodTechnique: Boolean = true,
    /** 0 — обычный, 1 — негативные повторения, 2 — с помощью (для подтягиваний). */
    val variant: Int = 0
)

/** Состояние прогрессии упражнения в конкретном тренировочном дне. */
@Entity(tableName = "exercise_progress", primaryKeys = ["dayId", "exerciseId"])
data class ProgressEntity(
    val dayId: String,
    val exerciseId: String,
    val workingWeightKg: Double? = null,
    /** Принятое расширение целевого диапазона повторений сверх верхней границы. */
    val extraReps: Int = 0,
    /** Сессия, по которой рекомендация уже обработана (принята или отклонена). */
    val handledSessionId: Long = 0,
    val handledAccepted: Boolean = false
)

@Entity(tableName = "measurements")
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val bodyWeightKg: Double? = null,
    val waistCm: Double? = null,
    val chestCm: Double? = null,
    val armCm: Double? = null
)

/** Пользовательское изменение параметров упражнения из настроек. */
@Entity(tableName = "plan_overrides", primaryKeys = ["dayId", "exerciseId"])
data class PlanOverrideEntity(
    val dayId: String,
    val exerciseId: String,
    val sets: Int,
    val repsMin: Int,
    val repsMax: Int,
    val restMin: Int,
    val restMax: Int
)
