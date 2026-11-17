package com.ironroutine.app.ui.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ironroutine.app.data.backup.BackupManager
import com.ironroutine.app.data.db.PlanOverrideEntity
import com.ironroutine.app.data.program.WorkoutDay
import com.ironroutine.app.data.repo.WorkoutRepository
import com.ironroutine.app.data.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUi(
    val settings: AppSettings,
    val plan: List<WorkoutDay>,
    val overrides: List<PlanOverrideEntity>
)

class SettingsViewModel(
    private val repo: WorkoutRepository,
    private val backup: BackupManager
) : ViewModel() {

    val ui: StateFlow<SettingsUi?> = combine(repo.settings, repo.plan, repo.planOverrides) { s, p, o ->
        SettingsUi(s, p, o)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun consumeMessage() {
        _message.value = null
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { repo.updateSettings(transform) }
    }

    fun saveOverride(o: PlanOverrideEntity) {
        viewModelScope.launch { repo.saveOverride(o) }
    }

    fun resetOverride(dayId: String, exerciseId: String) {
        viewModelScope.launch { repo.resetOverride(dayId, exerciseId) }
    }

    fun exportTo(uri: Uri, resolver: ContentResolver) {
        viewModelScope.launch {
            val result = runCatching {
                val json = backup.export()
                withContext(Dispatchers.IO) {
                    val out = resolver.openOutputStream(uri, "wt") ?: error("Не удалось открыть файл для записи")
                    out.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                }
            }
            _message.value =
                if (result.isSuccess) "Резервная копия сохранена"
                else "Не удалось сохранить: ${result.exceptionOrNull()?.message}"
        }
    }

    fun importFrom(uri: Uri, resolver: ContentResolver) {
        viewModelScope.launch {
            val result = runCatching {
                val text = withContext(Dispatchers.IO) {
                    val input = resolver.openInputStream(uri) ?: error("Не удалось открыть файл")
                    input.bufferedReader(Charsets.UTF_8).use { it.readText() }
                }
                backup.import(text)
            }
            _message.value =
                if (result.isSuccess) "Данные восстановлены"
                else "Не удалось восстановить: ${result.exceptionOrNull()?.message}"
        }
    }
}
