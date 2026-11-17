package com.ironroutine.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ironroutine.app.AppContainer
import com.ironroutine.app.IronApp
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

fun CreationExtras.ironApp(): IronApp = checkNotNull(this[APPLICATION_KEY]) as IronApp

/** Создаёт фабрику ViewModel с доступом к контейнеру зависимостей. */
inline fun <reified VM : ViewModel> factoryOf(crossinline create: (AppContainer) -> VM): ViewModelProvider.Factory =
    viewModelFactory {
        initializer<VM> { create(ironApp().container) }
    }

/** Текущая дата; обновляется, если приложение «пережило» полночь. */
val todayFlow: Flow<LocalDate> = flow {
    while (true) {
        emit(LocalDate.now())
        delay(60_000)
    }
}.distinctUntilChanged()
