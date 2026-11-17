package com.ironroutine.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

enum class WeightUnit { KG, LB }

data class AppSettings(
    val unit: WeightUnit = WeightUnit.KG,
    /** Вес одной гантели, кг. */
    val dumbbellKg: Double = 5.0,
    /** true — гантели разборные/регулируемые, и вес можно увеличивать. */
    val dumbbellAdjustable: Boolean = false,
    val dumbbellStepKg: Double = 1.0,
    /** Общий вес штанги с блинами, кг (уточни в настройках). */
    val barbellKg: Double = 20.0,
    val barbellStepKg: Double = 2.5,
    val autoRest: Boolean = true,
    val vibrate: Boolean = true,
    val programStartDay: Long = LocalDate.of(2026, 11, 16).toEpochDay(),
    val programEndDay: Long = LocalDate.of(2026, 12, 31).toEpochDay()
) {
    val programStart: LocalDate get() = LocalDate.ofEpochDay(programStartDay)
    val programEnd: LocalDate get() = LocalDate.ofEpochDay(programEndDay)
}

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    val flow: Flow<AppSettings> = context.settingsDataStore.data.map { it.toSettings() }

    suspend fun current(): AppSettings = flow.first()

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.settingsDataStore.edit { prefs ->
            val s = transform(prefs.toSettings())
            prefs[K_UNIT] = s.unit.name
            prefs[K_DB] = s.dumbbellKg
            prefs[K_DB_ADJ] = s.dumbbellAdjustable
            prefs[K_DB_STEP] = s.dumbbellStepKg
            prefs[K_BB] = s.barbellKg
            prefs[K_BB_STEP] = s.barbellStepKg
            prefs[K_AUTO_REST] = s.autoRest
            prefs[K_VIBRATE] = s.vibrate
            prefs[K_START] = s.programStartDay
            prefs[K_END] = s.programEndDay
        }
    }

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            unit = this[K_UNIT]?.let { runCatching { WeightUnit.valueOf(it) }.getOrNull() } ?: d.unit,
            dumbbellKg = this[K_DB] ?: d.dumbbellKg,
            dumbbellAdjustable = this[K_DB_ADJ] ?: d.dumbbellAdjustable,
            dumbbellStepKg = this[K_DB_STEP] ?: d.dumbbellStepKg,
            barbellKg = this[K_BB] ?: d.barbellKg,
            barbellStepKg = this[K_BB_STEP] ?: d.barbellStepKg,
            autoRest = this[K_AUTO_REST] ?: d.autoRest,
            vibrate = this[K_VIBRATE] ?: d.vibrate,
            programStartDay = this[K_START] ?: d.programStartDay,
            programEndDay = this[K_END] ?: d.programEndDay
        )
    }

    private companion object {
        val K_UNIT = stringPreferencesKey("unit")
        val K_DB = doublePreferencesKey("dumbbell_kg")
        val K_DB_ADJ = booleanPreferencesKey("dumbbell_adjustable")
        val K_DB_STEP = doublePreferencesKey("dumbbell_step_kg")
        val K_BB = doublePreferencesKey("barbell_kg")
        val K_BB_STEP = doublePreferencesKey("barbell_step_kg")
        val K_AUTO_REST = booleanPreferencesKey("auto_rest")
        val K_VIBRATE = booleanPreferencesKey("vibrate")
        val K_START = longPreferencesKey("program_start")
        val K_END = longPreferencesKey("program_end")
    }
}
