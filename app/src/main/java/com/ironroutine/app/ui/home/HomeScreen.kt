package com.ironroutine.app.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ironroutine.app.domain.Ru
import com.ironroutine.app.domain.SlotStatus
import com.ironroutine.app.domain.formatDuration
import com.ironroutine.app.domain.formatNumber
import com.ironroutine.app.ui.components.BarChart
import com.ironroutine.app.ui.components.IronCard
import com.ironroutine.app.ui.components.Pill
import com.ironroutine.app.ui.components.PrimaryButton
import com.ironroutine.app.ui.components.ProgressBar
import com.ironroutine.app.ui.components.SecondaryText
import com.ironroutine.app.ui.components.SectionTitle
import com.ironroutine.app.ui.components.StatTile
import com.ironroutine.app.ui.factoryOf
import com.ironroutine.app.ui.theme.IronColors
import java.time.LocalTime

@Composable
fun HomeScreen(
    onOpenWorkout: (dayId: String, autostart: Boolean) -> Unit,
    onOpenSettings: () -> Unit
) {
    val vm: HomeViewModel = viewModel(factory = factoryOf { HomeViewModel(it.repository) })
    val ui by vm.ui.collectAsStateWithLifecycle()
    val s = ui ?: return

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Header(s, onOpenSettings) }
        item { ProgramCard(s) }
        item { HeroCard(s, onOpenWorkout) }
        item { SectionTitle("Тренировки недели") }
        items(s.dayCards, key = { it.day.id }) { card ->
            DayCard(card) { onOpenWorkout(card.day.id, false) }
        }
        item { SectionTitle("Статистика") }
        item { StatsGrid(s) }
        item {
            IronCard(Modifier.fillMaxWidth()) {
                Text("Тренировок по неделям", style = MaterialTheme.typography.titleSmall)
                SecondaryText("Последние 6 недель", style = MaterialTheme.typography.bodySmall)
                Column(Modifier.padding(top = 10.dp)) { BarChart(s.weekBars) }
            }
        }
    }
}

@Composable
private fun Header(s: HomeUi, onOpenSettings: () -> Unit) {
    val hour = LocalTime.now().hour
    val greeting = when {
        hour < 5 -> "Доброй ночи"
        hour < 12 -> "Доброе утро"
        hour < 18 -> "Добрый день"
        else -> "Добрый вечер"
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("IRON ROUTINE", style = MaterialTheme.typography.labelMedium, color = IronColors.Accent)
            Text(greeting, style = MaterialTheme.typography.headlineMedium)
            SecondaryText(Ru.full(s.today))
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.Settings, contentDescription = "Настройки", tint = IronColors.TextSecondary)
        }
    }
}

@Composable
private fun ProgramCard(s: HomeUi) {
    val target = if (s.programTotal == 0) 0f else s.programDone.toFloat() / s.programTotal
    var shown by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(target) { shown = target }
    val animated by animateFloatAsState(shown, tween(700), label = "programProgress")

    IronCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                val title = when {
                    !s.programStarted -> "Программа начнётся ${Ru.dayMonth(s.settings.programStart)}"
                    s.programFinished -> "Неделя ${s.weekNumber} · продолжаем"
                    else -> "Неделя ${s.weekNumber} из ${s.totalWeeks}"
                }
                Text(title, style = MaterialTheme.typography.titleMedium)
                SecondaryText(
                    if (s.programFinished) "Плановый период завершён, тренировки продолжаются"
                    else "${Ru.dayMonth(s.settings.programStart)} – ${Ru.dayMonth(s.settings.programEnd)}"
                )
            }
            Text(
                "${(animated * 100).toInt()}%",
                style = MaterialTheme.typography.headlineSmall, color = IronColors.Accent
            )
        }
        ProgressBar(animated, Modifier.padding(top = 12.dp))
        SecondaryText(
            "Выполнено ${s.programDone} из ${s.programTotal} запланированных тренировок",
            Modifier.padding(top = 8.dp), MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun HeroCard(s: HomeUi, onOpenWorkout: (String, Boolean) -> Unit) {
    IronCard(
        Modifier.fillMaxWidth(),
        color = IronColors.Surface2,
        border = BorderStroke(1.dp, IronColors.Accent.copy(alpha = 0.35f))
    ) {
        val active = s.active
        val next = s.nextSlot
        when {
            active != null -> {
                Pill("В процессе", color = IronColors.Warning.copy(alpha = 0.2f), textColor = IronColors.Warning)
                Text(
                    "${active.title} — тренировка не завершена",
                    Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleLarge
                )
                SecondaryText("Все введённые данные сохранены. Можно продолжить с того же места.")
                PrimaryButton(
                    "Продолжить тренировку", { onOpenWorkout(active.id, false) },
                    Modifier.fillMaxWidth().padding(top = 14.dp)
                )
            }
            s.todayDay != null && !s.todayDone && s.programStarted -> {
                val d = s.todayDay
                Pill("Сегодня")
                Text("${d.title} — ${d.subtitle}", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleLarge)
                SecondaryText("${d.exercises.size} упражнений · ${d.exercises.sumOf { it.sets }} подходов")
                PrimaryButton(
                    "Начать тренировку", { onOpenWorkout(d.id, true) },
                    Modifier.fillMaxWidth().padding(top = 14.dp)
                )
            }
            else -> {
                if (s.todayDone) {
                    Pill("Сегодня выполнено", color = IronColors.Accent.copy(alpha = 0.2f), textColor = IronColors.Accent)
                    Text("Отличная работа!", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleLarge)
                    SecondaryText("Теперь восстановление. Хороший сон и белок — часть программы.")
                } else {
                    Pill("День восстановления")
                    Text("День восстановления", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleLarge)
                    SecondaryText("Мышцы растут в отдыхе. Сегодня без тренировки.")
                }
                if (next != null) {
                    SecondaryText(
                        "Следующая тренировка: ${Ru.full(next.date)} — ${next.day.title}",
                        Modifier.padding(top = 10.dp)
                    )
                    PrimaryButton(
                        "Начать тренировку", { onOpenWorkout(next.day.id, true) },
                        Modifier.fillMaxWidth().padding(top = 14.dp)
                    )
                    SecondaryText(
                        "Можно начать раньше срока — нагрузка от этого не меняется.",
                        Modifier.padding(top = 6.dp), MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCard(card: DayCardUi, onClick: () -> Unit) {
    IronCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SecondaryText(Ru.weekdayCap(card.slotDate), style = MaterialTheme.typography.labelMedium)
                Text(card.day.title, style = MaterialTheme.typography.titleLarge)
                SecondaryText(card.day.subtitle, style = MaterialTheme.typography.bodySmall)
            }
            when (card.status) {
                SlotStatus.DONE -> Pill("Выполнено", color = IronColors.Accent.copy(alpha = 0.2f), textColor = IronColors.Accent)
                SlotStatus.MISSED -> Pill("Пропущено", color = IronColors.Danger.copy(alpha = 0.2f), textColor = IronColors.Danger)
                SlotStatus.TODAY -> Pill("Сегодня", color = IronColors.Accent, textColor = IronColors.OnAccent)
                SlotStatus.UPCOMING -> Pill(Ru.dayMonth(card.slotDate))
            }
        }
    }
}

@Composable
private fun StatsGrid(s: HomeUi) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("тренировок завершено", s.completedCount.toString(), Modifier.weight(1f), accent = true)
            StatTile("серия подряд", s.streak.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("на этой неделе", "${s.weekDone} / ${s.weekPlanned}", Modifier.weight(1f))
            StatTile("пропущено", s.missed.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("общее время", formatDuration(s.totalDurationSec), Modifier.weight(1f))
            StatTile(
                "общий объём",
                if (s.totalVolumeKg >= 1000) "${formatNumber(s.totalVolumeKg / 1000)} т"
                else "${formatNumber(s.totalVolumeKg)} кг",
                Modifier.weight(1f)
            )
        }
    }
}
