package com.ironroutine.app

import android.app.Application
import android.content.Context
import com.ironroutine.app.data.backup.BackupManager
import com.ironroutine.app.data.db.AppDatabase
import com.ironroutine.app.data.repo.WorkoutRepository
import com.ironroutine.app.data.settings.SettingsStore

/** Простой контейнер зависимостей (без внешних DI-библиотек). */
class AppContainer(context: Context) {
    private val db = AppDatabase.create(context)
    private val settingsStore = SettingsStore(context.applicationContext)
    val repository = WorkoutRepository(db, settingsStore)
    val backup = BackupManager(db, settingsStore)
}

class IronApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
