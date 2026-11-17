package com.ironroutine.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun get(id: Long): SessionEntity?

    @Query("SELECT * FROM sessions WHERE id = :id")
    fun observe(id: Long): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE status = 0 ORDER BY startedAt DESC LIMIT 1")
    fun observeActive(): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE status = 1 ORDER BY finishedAt DESC")
    fun observeCompleted(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE status = 1 AND dayId = :dayId ORDER BY finishedAt DESC LIMIT :limit")
    suspend fun lastCompleted(dayId: String, limit: Int): List<SessionEntity>

    @Query("UPDATE sessions SET lastActivityAt = :time WHERE id = :id")
    suspend fun touch(id: Long, time: Long)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM sessions")
    suspend fun getAll(): List<SessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SessionEntity>)

    @Query("DELETE FROM sessions")
    suspend fun clear()
}

@Dao
interface SetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SetLogEntity>)

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId ORDER BY exerciseId, setIndex")
    fun observeForSession(sessionId: Long): Flow<List<SetLogEntity>>

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId")
    suspend fun forSession(sessionId: Long): List<SetLogEntity>

    @Query("SELECT * FROM set_logs")
    fun observeAll(): Flow<List<SetLogEntity>>

    @Query("SELECT * FROM set_logs")
    suspend fun getAll(): List<SetLogEntity>

    @Query(
        "UPDATE set_logs SET reps = :reps, weightKg = :weightKg " +
            "WHERE sessionId = :sessionId AND exerciseId = :exerciseId AND setIndex = :setIndex"
    )
    suspend fun updateValues(sessionId: Long, exerciseId: String, setIndex: Int, reps: Int, weightKg: Double)

    @Query(
        "UPDATE set_logs SET done = :done " +
            "WHERE sessionId = :sessionId AND exerciseId = :exerciseId AND setIndex = :setIndex"
    )
    suspend fun updateDone(sessionId: Long, exerciseId: String, setIndex: Int, done: Boolean)

    @Query("DELETE FROM set_logs")
    suspend fun clear()
}

@Dao
interface ResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ExerciseResultEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ExerciseResultEntity>)

    @Query("SELECT * FROM exercise_results WHERE sessionId = :sessionId")
    fun observeForSession(sessionId: Long): Flow<List<ExerciseResultEntity>>

    @Query("SELECT * FROM exercise_results WHERE sessionId = :sessionId AND exerciseId = :exerciseId")
    suspend fun get(sessionId: Long, exerciseId: String): ExerciseResultEntity?

    @Query("SELECT * FROM exercise_results")
    fun observeAll(): Flow<List<ExerciseResultEntity>>

    @Query("SELECT * FROM exercise_results")
    suspend fun getAll(): List<ExerciseResultEntity>

    @Query("DELETE FROM exercise_results")
    suspend fun clear()
}

@Dao
interface ProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ProgressEntity>)

    @Query("SELECT * FROM exercise_progress WHERE dayId = :dayId AND exerciseId = :exerciseId")
    suspend fun get(dayId: String, exerciseId: String): ProgressEntity?

    @Query("SELECT * FROM exercise_progress")
    fun observeAll(): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM exercise_progress")
    suspend fun getAll(): List<ProgressEntity>

    @Query("DELETE FROM exercise_progress")
    suspend fun clear()
}

@Dao
interface MeasurementDao {
    @Insert
    suspend fun insert(item: MeasurementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MeasurementEntity>)

    @Query("SELECT * FROM measurements ORDER BY date ASC, id ASC")
    fun observeAll(): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements")
    suspend fun getAll(): List<MeasurementEntity>

    @Query("DELETE FROM measurements WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM measurements")
    suspend fun clear()
}

@Dao
interface PlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: PlanOverrideEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PlanOverrideEntity>)

    @Query("SELECT * FROM plan_overrides")
    fun observeAll(): Flow<List<PlanOverrideEntity>>

    @Query("SELECT * FROM plan_overrides")
    suspend fun getAll(): List<PlanOverrideEntity>

    @Query("DELETE FROM plan_overrides WHERE dayId = :dayId AND exerciseId = :exerciseId")
    suspend fun delete(dayId: String, exerciseId: String)

    @Query("DELETE FROM plan_overrides")
    suspend fun clear()
}
