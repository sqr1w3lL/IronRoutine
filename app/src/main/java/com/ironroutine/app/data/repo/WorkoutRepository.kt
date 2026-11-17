package com.ironroutine.app.data.repo

import androidx.room.withTransaction
import com.ironroutine.app.data.db.AppDatabase
import com.ironroutine.app.data.db.ExerciseResultEntity
import com.ironroutine.app.data.db.MeasurementEntity
import com.ironroutine.app.data.db.PlanOverrideEntity
import com.ironroutine.app.data.db.ProgressEntity
import com.ironroutine.app.data.db.SessionEntity
import com.ironroutine.app.data.db.SetLogEntity
import com.ironroutine.app.data.program.ProgramData
import com.ironroutine.app.data.program.WorkoutDay
import com.ironroutine.app.data.settings.AppSettings
import com.ironroutine.app.data.settings.SettingsStore
import com.ironroutine.app.domain.SessionHistory
import com.ironroutine.app.domain.SetResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkoutRepository(
    private val db: AppDatabase,
    private val settingsStore: SettingsStore
) {
    val settings: Flow<AppSettings> = settingsStore.flow
    suspend fun settingsNow(): AppSettings = settingsStore.current()
    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) = settingsStore.update(transform)

    /** Программа с учётом пользовательских изменений. */
    val plan: Flow<List<WorkoutDay>> = db.planDao().observeAll().map { ProgramData.applyOverrides(it) }
    val planOverrides: Flow<List<PlanOverrideEntity>> = db.planDao().observeAll()

    val activeSession: Flow<SessionEntity?> = db.sessionDao().observeActive()
    val completedSessions: Flow<List<SessionEntity>> = db.sessionDao().observeCompleted()
    val allSets: Flow<List<SetLogEntity>> = db.setDao().observeAll()
    val allResults: Flow<List<ExerciseResultEntity>> = db.resultDao().observeAll()
    val allProgress: Flow<List<ProgressEntity>> = db.progressDao().observeAll()
    val measurements: Flow<List<MeasurementEntity>> = db.measurementDao().observeAll()

    fun observeSession(id: Long): Flow<SessionEntity?> = db.sessionDao().observe(id)
    fun setsForSession(id: Long): Flow<List<SetLogEntity>> = db.setDao().observeForSession(id)
    fun resultsForSession(id: Long): Flow<List<ExerciseResultEntity>> = db.resultDao().observeForSession(id)

    /** Последние выполненные тренировки этого дня, где упражнение реально выполнялось (новые — первыми). */
    suspend fun history(dayId: String, exerciseId: String, limit: Int = 3): List<SessionHistory> {
        val out = ArrayList<SessionHistory>()
        for (s in db.sessionDao().lastCompleted(dayId, 20)) {
            val sets = db.setDao().forSession(s.id)
                .filter { it.exerciseId == exerciseId && it.done }
                .sortedBy { it.setIndex }
            if (sets.isEmpty()) continue
            val res = db.resultDao().get(s.id, exerciseId)
            out += SessionHistory(
                sessionId = s.id,
                finishedAt = s.finishedAt,
                feeling = s.feeling,
                goodTechnique = res?.goodTechnique ?: true,
                variant = res?.variant ?: 0,
                sets = sets.map { SetResult(it.reps, it.weightKg) }
            )
            if (out.size >= limit) break
        }
        return out
    }

    suspend fun startSession(day: WorkoutDay, slotEpochDay: Long, sets: List<SetLogEntity>): Long =
        db.withTransaction {
            val now = System.currentTimeMillis()
            val id = db.sessionDao().insert(
                SessionEntity(dayId = day.id, slotDate = slotEpochDay, startedAt = now, lastActivityAt = now)
            )
            db.setDao().insertAll(sets.map { it.copy(sessionId = id) })
            id
        }

    suspend fun updateSetValues(sessionId: Long, exerciseId: String, setIndex: Int, reps: Int, weightKg: Double) {
        db.setDao().updateValues(sessionId, exerciseId, setIndex, reps, weightKg)
        db.sessionDao().touch(sessionId, System.currentTimeMillis())
    }

    suspend fun updateSetDone(sessionId: Long, exerciseId: String, setIndex: Int, done: Boolean) {
        db.setDao().updateDone(sessionId, exerciseId, setIndex, done)
        db.sessionDao().touch(sessionId, System.currentTimeMillis())
    }

    suspend fun saveResult(result: ExerciseResultEntity) {
        db.resultDao().upsert(result)
        db.sessionDao().touch(result.sessionId, System.currentTimeMillis())
    }

    suspend fun cancelSession(id: Long) = db.sessionDao().delete(id)

    /** Завершает тренировку, считает итоги и обновляет рабочие веса. */
    suspend fun finishSession(sessionId: Long, feeling: Int, day: WorkoutDay): SessionEntity =
        db.withTransaction {
            val s = checkNotNull(db.sessionDao().get(sessionId))
            val done = db.setDao().forSession(sessionId).filter { it.done }
            val now = System.currentTimeMillis()
            // Если приложение «забыли» без активности больше 30 минут — не раздуваем длительность.
            val endMs = if (now - s.lastActivityAt > 30 * 60_000L) s.lastActivityAt + 2 * 60_000L else now
            val duration = ((endMs - s.startedAt) / 1000).toInt().coerceAtLeast(60)
            val updated = s.copy(
                status = SessionEntity.STATUS_DONE,
                finishedAt = endMs,
                durationSec = duration,
                feeling = feeling,
                completedSets = done.size,
                plannedSets = day.exercises.sumOf { it.sets },
                volumeKg = done.sumOf { it.reps * it.weightKg }
            )
            db.sessionDao().update(updated)
            for (pe in day.exercises) {
                val def = ProgramData.exerciseOrNull(pe.exerciseId) ?: continue
                if (!def.usesWeight) continue
                val w = done.filter { it.exerciseId == pe.exerciseId }.maxOfOrNull { it.weightKg } ?: continue
                if (w <= 0.0) continue
                val old = db.progressDao().get(day.id, pe.exerciseId) ?: ProgressEntity(day.id, pe.exerciseId)
                db.progressDao().upsert(old.copy(workingWeightKg = w))
            }
            updated
        }

    suspend fun resolveSuggestion(
        dayId: String,
        exerciseId: String,
        sessionId: Long,
        accepted: Boolean,
        newWeightKg: Double?,
        newExtraReps: Int?
    ) {
        val old = db.progressDao().get(dayId, exerciseId) ?: ProgressEntity(dayId, exerciseId)
        var p = old.copy(handledSessionId = sessionId, handledAccepted = accepted)
        if (accepted) {
            if (newWeightKg != null) p = p.copy(workingWeightKg = newWeightKg)
            if (newExtraReps != null) p = p.copy(extraReps = newExtraReps)
            // Новый вес — новый диапазон: счётчик расширения повторений сбрасывается.
            if (newWeightKg != null && newExtraReps == null && (newWeightKg > (old.workingWeightKg ?: 0.0))) {
                p = p.copy(extraReps = 0)
            }
        }
        db.progressDao().upsert(p)
    }

    suspend fun addMeasurement(m: MeasurementEntity) {
        db.measurementDao().insert(m)
    }

    suspend fun deleteMeasurement(id: Long) = db.measurementDao().delete(id)

    suspend fun saveOverride(o: PlanOverrideEntity) = db.planDao().upsert(o)
    suspend fun resetOverride(dayId: String, exerciseId: String) = db.planDao().delete(dayId, exerciseId)
}
