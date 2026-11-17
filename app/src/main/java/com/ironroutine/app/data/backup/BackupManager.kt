package com.ironroutine.app.data.backup

import androidx.room.withTransaction
import com.ironroutine.app.data.db.AppDatabase
import com.ironroutine.app.data.db.ExerciseResultEntity
import com.ironroutine.app.data.db.MeasurementEntity
import com.ironroutine.app.data.db.PlanOverrideEntity
import com.ironroutine.app.data.db.ProgressEntity
import com.ironroutine.app.data.db.SessionEntity
import com.ironroutine.app.data.db.SetLogEntity
import com.ironroutine.app.data.settings.AppSettings
import com.ironroutine.app.data.settings.SettingsStore
import com.ironroutine.app.data.settings.WeightUnit
import org.json.JSONArray
import org.json.JSONObject

/** Резервное копирование и восстановление всех данных в один JSON-файл. */
class BackupManager(
    private val db: AppDatabase,
    private val settingsStore: SettingsStore
) {
    suspend fun export(): String {
        val root = JSONObject()
        root.put("app", APP_TAG)
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val s = settingsStore.current()
        root.put("settings", JSONObject().apply {
            put("unit", s.unit.name)
            put("dumbbellKg", s.dumbbellKg)
            put("dumbbellAdjustable", s.dumbbellAdjustable)
            put("dumbbellStepKg", s.dumbbellStepKg)
            put("barbellKg", s.barbellKg)
            put("barbellStepKg", s.barbellStepKg)
            put("autoRest", s.autoRest)
            put("vibrate", s.vibrate)
            put("programStartDay", s.programStartDay)
            put("programEndDay", s.programEndDay)
        })

        root.put("sessions", JSONArray().apply {
            db.sessionDao().getAll().forEach { e ->
                put(JSONObject().apply {
                    put("id", e.id); put("dayId", e.dayId); put("slotDate", e.slotDate)
                    put("startedAt", e.startedAt); put("lastActivityAt", e.lastActivityAt)
                    put("finishedAt", e.finishedAt); put("status", e.status)
                    put("durationSec", e.durationSec); put("feeling", e.feeling)
                    put("completedSets", e.completedSets); put("plannedSets", e.plannedSets)
                    put("volumeKg", e.volumeKg)
                })
            }
        })
        root.put("sets", JSONArray().apply {
            db.setDao().getAll().forEach { e ->
                put(JSONObject().apply {
                    put("sessionId", e.sessionId); put("exerciseId", e.exerciseId)
                    put("setIndex", e.setIndex); put("reps", e.reps)
                    put("weightKg", e.weightKg); put("done", e.done)
                })
            }
        })
        root.put("results", JSONArray().apply {
            db.resultDao().getAll().forEach { e ->
                put(JSONObject().apply {
                    put("sessionId", e.sessionId); put("exerciseId", e.exerciseId)
                    put("goodTechnique", e.goodTechnique); put("variant", e.variant)
                })
            }
        })
        root.put("progress", JSONArray().apply {
            db.progressDao().getAll().forEach { e ->
                put(JSONObject().apply {
                    put("dayId", e.dayId); put("exerciseId", e.exerciseId)
                    put("workingWeightKg", e.workingWeightKg ?: JSONObject.NULL)
                    put("extraReps", e.extraReps)
                    put("handledSessionId", e.handledSessionId)
                    put("handledAccepted", e.handledAccepted)
                })
            }
        })
        root.put("measurements", JSONArray().apply {
            db.measurementDao().getAll().forEach { e ->
                put(JSONObject().apply {
                    put("id", e.id); put("date", e.date)
                    put("bodyWeightKg", e.bodyWeightKg ?: JSONObject.NULL)
                    put("waistCm", e.waistCm ?: JSONObject.NULL)
                    put("chestCm", e.chestCm ?: JSONObject.NULL)
                    put("armCm", e.armCm ?: JSONObject.NULL)
                })
            }
        })
        root.put("planOverrides", JSONArray().apply {
            db.planDao().getAll().forEach { e ->
                put(JSONObject().apply {
                    put("dayId", e.dayId); put("exerciseId", e.exerciseId)
                    put("sets", e.sets); put("repsMin", e.repsMin); put("repsMax", e.repsMax)
                    put("restMin", e.restMin); put("restMax", e.restMax)
                })
            }
        })
        return root.toString(2)
    }

    /** Заменяет все текущие данные содержимым резервной копии. Бросает исключение при неверном файле. */
    suspend fun import(json: String) {
        val root = JSONObject(json)
        require(root.optString("app") == APP_TAG) { "Это не файл резервной копии IRON ROUTINE" }

        val sessions = root.getJSONArray("sessions").mapObjects { o ->
            SessionEntity(
                id = o.getLong("id"), dayId = o.getString("dayId"), slotDate = o.getLong("slotDate"),
                startedAt = o.getLong("startedAt"), lastActivityAt = o.getLong("lastActivityAt"),
                finishedAt = o.getLong("finishedAt"), status = o.getInt("status"),
                durationSec = o.getInt("durationSec"), feeling = o.getInt("feeling"),
                completedSets = o.getInt("completedSets"), plannedSets = o.getInt("plannedSets"),
                volumeKg = o.getDouble("volumeKg")
            )
        }
        val sets = root.getJSONArray("sets").mapObjects { o ->
            SetLogEntity(
                sessionId = o.getLong("sessionId"), exerciseId = o.getString("exerciseId"),
                setIndex = o.getInt("setIndex"), reps = o.getInt("reps"),
                weightKg = o.getDouble("weightKg"), done = o.getBoolean("done")
            )
        }
        val results = root.getJSONArray("results").mapObjects { o ->
            ExerciseResultEntity(
                sessionId = o.getLong("sessionId"), exerciseId = o.getString("exerciseId"),
                goodTechnique = o.getBoolean("goodTechnique"), variant = o.getInt("variant")
            )
        }
        val progress = root.getJSONArray("progress").mapObjects { o ->
            ProgressEntity(
                dayId = o.getString("dayId"), exerciseId = o.getString("exerciseId"),
                workingWeightKg = o.optDoubleOrNull("workingWeightKg"),
                extraReps = o.getInt("extraReps"),
                handledSessionId = o.getLong("handledSessionId"),
                handledAccepted = o.getBoolean("handledAccepted")
            )
        }
        val measurements = root.getJSONArray("measurements").mapObjects { o ->
            MeasurementEntity(
                id = o.getLong("id"), date = o.getLong("date"),
                bodyWeightKg = o.optDoubleOrNull("bodyWeightKg"),
                waistCm = o.optDoubleOrNull("waistCm"),
                chestCm = o.optDoubleOrNull("chestCm"),
                armCm = o.optDoubleOrNull("armCm")
            )
        }
        val overrides = root.getJSONArray("planOverrides").mapObjects { o ->
            PlanOverrideEntity(
                dayId = o.getString("dayId"), exerciseId = o.getString("exerciseId"),
                sets = o.getInt("sets"), repsMin = o.getInt("repsMin"), repsMax = o.getInt("repsMax"),
                restMin = o.getInt("restMin"), restMax = o.getInt("restMax")
            )
        }
        val so = root.getJSONObject("settings")
        val defaults = AppSettings()
        val restored = AppSettings(
            unit = runCatching { WeightUnit.valueOf(so.getString("unit")) }.getOrDefault(defaults.unit),
            dumbbellKg = so.optDouble("dumbbellKg", defaults.dumbbellKg),
            dumbbellAdjustable = so.optBoolean("dumbbellAdjustable", defaults.dumbbellAdjustable),
            dumbbellStepKg = so.optDouble("dumbbellStepKg", defaults.dumbbellStepKg),
            barbellKg = so.optDouble("barbellKg", defaults.barbellKg),
            barbellStepKg = so.optDouble("barbellStepKg", defaults.barbellStepKg),
            autoRest = so.optBoolean("autoRest", defaults.autoRest),
            vibrate = so.optBoolean("vibrate", defaults.vibrate),
            programStartDay = so.optLong("programStartDay", defaults.programStartDay),
            programEndDay = so.optLong("programEndDay", defaults.programEndDay)
        )

        db.withTransaction {
            db.sessionDao().clear()   // каскадно очищает подходы и результаты
            db.setDao().clear()
            db.resultDao().clear()
            db.progressDao().clear()
            db.measurementDao().clear()
            db.planDao().clear()
            db.sessionDao().insertAll(sessions)
            db.setDao().insertAll(sets)
            db.resultDao().insertAll(results)
            db.progressDao().insertAll(progress)
            db.measurementDao().insertAll(measurements)
            db.planDao().insertAll(overrides)
        }
        settingsStore.update { restored }
    }

    private fun <T> JSONArray.mapObjects(block: (JSONObject) -> T): List<T> =
        (0 until length()).map { block(getJSONObject(it)) }

    private fun JSONObject.optDoubleOrNull(key: String): Double? =
        if (isNull(key)) null else getDouble(key)

    private companion object {
        const val APP_TAG = "IRON_ROUTINE_BACKUP"
    }
}
