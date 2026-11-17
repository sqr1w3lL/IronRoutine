package com.ironroutine.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SessionEntity::class,
        SetLogEntity::class,
        ExerciseResultEntity::class,
        ProgressEntity::class,
        MeasurementEntity::class,
        PlanOverrideEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun setDao(): SetDao
    abstract fun resultDao(): ResultDao
    abstract fun progressDao(): ProgressDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun planDao(): PlanDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "iron_routine.db")
                .build()
    }
}
