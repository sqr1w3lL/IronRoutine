package com.ironroutine.app.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ironroutine.app.data.program.ProgramData
import com.ironroutine.app.domain.Ru
import com.ironroutine.app.domain.Schedule
import com.ironroutine.app.domain.Slot
import com.ironroutine.app.domain.SlotStatus
import com.ironroutine.app.domain.Units
import com.ironroutine.app.domain.formatDuration
import com.ironroutine.app.ui.components.IronCard
import com.ironroutine.app.ui.components.PrimaryButton
import com.ironroutine.app.ui.components.SecondaryButton
import com.ironroutine.app.ui.components.SecondaryText
import com.ironroutine.app.ui.factoryOf
import com.ironroutine.app.ui.theme.IronColors
import java.time.LocalDate

private enum class CellKind { DONE, MISSED, TODAY_PENDING, UPCOMING, REST, OUTSIDE }

private fun cellKind(date: LocalDate, d: CalendarData, keys: Set<Pair<String, Long>>): CellKind {
    val day = Schedule.dayOn(date, d.plan)
    val done = day != null && (day.id to date.toEpochDay()) in keys
    if (done) return CellKind.DONE
    if (date.isBefore(d.settings.programStart)) return CellKind.OUTSIDE
    if (day == null) return CellKind.REST
    return when (Schedule.status(Slot(date, day), d.today, keys)) {
        SlotStatus.DONE -> CellKind.DONE
        SlotStatus.MISSED -> CellKind.MISSED
        SlotStatus.TODAY -> CellKind.TODAY_PENDING
        SlotStatus.UPCOMING -> CellKind.UPCOMING
    }
}

@Composable
fun CalendarScreen(
    onOpenWorkout: (dayId: String, autostart: Boolean) -> Unit,
    onOpenSession: (Long) -> Unit
) {
    val vm: CalendarViewModel = viewModel(factory = factoryOf { CalendarViewModel(it.repository) })
    val data by vm.data.collectAsStateWithLifecycle()

    val now = remember { LocalDate.now() }
    var monthIndex by rememberSaveable { mutableIntStateOf(now.year * 12 + now.monthValue - 1) }
    var selectedEpoch by rememberSaveable { mutableLongStateOf(now.toEpochDay()) }

    val d = data ?: return
    val keys = remember(d.sessions) { Schedule.doneKeys(d.sessions) }
    val year = monthIndex / 12
    val month = monthIndex % 12 + 1
    val first = LocalDate.of(year, month, 1)
    val offset = first.dayOfWeek.value - 1
    val daysInMonth = first.lengthOfMonth()
    val rows = (offset + daysInMonth + 6) / 7
    val selected = LocalDate.ofEpochDay(selectedEpoch)

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Календарь", style = MaterialTheme.typography.headlineMedium)
        }
        item {
            IronCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { monthIndex -= 1 }) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Предыдущий месяц", tint = IronColors.TextPrimary)
                    }
                    Text(
                        Ru.monthYear(year, month), Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center
                    )
                    IconButton(onClick = { monthIndex += 1 }) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Следующий месяц", tint = IronColors.TextPrimary)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    for (i in 0..6) {
                        Text(
                            Ru.weekdayShort(i), Modifier.weight(1f), textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall, color = IronColors.TextSecondary
                        )
                    }
                }
                for (r in 0 until rows) {
                    Row(Modifier.fillMaxWidth()) {
                        for (c in 0..6) {
                            val n = r * 7 + c - offset + 1
                            if (n < 1 || n > daysInMonth) {
                                Box(Modifier.weight(1f).aspectRatio(1f))
                            } else {
                                val date = LocalDate.of(year, month, n)
                                DayCell(
                                    day = n,
                                    kind = cellKind(date, d, keys),
                                    selected = date == selected,
                                    isToday = date == d.today,
                                    onClick = { selectedEpoch = date.toEpochDay() },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Legend("Выполнено", IronColors.Accent, filled = true)
                    Legend("Пропущено", IronColors.Danger, filled = false)
                    Legend("План", IronColors.TextSecondary, filled = false)
                    Legend("Отдых", IronColors.Surface2, filled = true)
                }
                TextButton(
                    onClick = {
                        monthIndex = d.today.year * 12 + d.today.monthValue - 1
                        selectedEpoch = d.today.toEpochDay()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = IronColors.Accent)
                ) { Text("Сегодня") }
            }
        }
        item {
            DayInfo(selected, d, keys, cellKind(selected, d, keys), onOpenWorkout, onOpenSession)
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    kind: CellKind,
    selected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    val bg = when {
        kind == CellKind.DONE -> IronColors.Accent
        selected -> IronColors.Surface2
        else -> Color.Transparent
    }
    val border: BorderStroke? = when {
        selected -> BorderStroke(2.dp, IronColors.TextPrimary)
        kind == CellKind.MISSED -> BorderStroke(1.5.dp, IronColors.Danger)
        kind == CellKind.TODAY_PENDING -> BorderStroke(2.dp, IronColors.Accent)
        kind == CellKind.UPCOMING -> BorderStroke(1.dp, IronColors.Outline)
        else -> null
    }
    val textColor = when (kind) {
        CellKind.DONE -> IronColors.OnAccent
        CellKind.MISSED -> IronColors.Danger
        CellKind.TODAY_PENDING -> IronColors.Accent
        CellKind.UPCOMING -> IronColors.TextPrimary
        CellKind.REST -> IronColors.TextSecondary
        CellKind.OUTSIDE -> IronColors.TextSecondary.copy(alpha = 0.5f)
    }
    Box(modifier.aspectRatio(1f).padding(3.dp)) {
        Box(
            Modifier
                .matchParentSize()
                .clip(shape)
                .background(bg)
                .then(if (border != null) Modifier.border(border, shape) else Modifier)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                day.toString(), color = textColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (kind == CellKind.DONE || isToday) FontWeight.Bold else FontWeight.Normal
            )
            if (isToday) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (kind == CellKind.DONE) IronColors.OnAccent else IronColors.Accent)
                )
            }
        }
    }
}

@Composable
private fun Legend(text: String, color: Color, filled: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .then(if (filled) Modifier.background(color) else Modifier.border(1.5.dp, color, CircleShape))
        )
        Text(text, Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelSmall, color = IronColors.TextSecondary)
    }
}

@Composable
private fun DayInfo(
    date: LocalDate,
    d: CalendarData,
    keys: Set<Pair<String, Long>>,
    kind: CellKind,
    onOpenWorkout: (String, Boolean) -> Unit,
    onOpenSession: (Long) -> Unit
) {
    val day = Schedule.dayOn(date, d.plan)
    IronCard(Modifier.fillMaxWidth()) {
        SecondaryText(Ru.full(date), style = MaterialTheme.typography.labelMedium)
        when {
            kind == CellKind.OUTSIDE -> {
                Text("До начала программы", Modifier.padding(top = 4.dp), style = MaterialTheme.typography.titleMedium)
                SecondaryText("Программа стартует ${Ru.dayMonth(d.settings.programStart)}.")
            }
            day == null -> {
                Text("День отдыха", Modifier.padding(top = 4.dp), style = MaterialTheme.typography.titleMedium)
                SecondaryText("Тренировки по расписанию: Пн, Ср, Пт. В остальные дни мышцы восстанавливаются.")
            }
            else -> {
                Text("${day.title} — ${day.subtitle}", Modifier.padding(top = 4.dp), style = MaterialTheme.typography.titleMedium)
                when (kind) {
                    CellKind.DONE -> {
                        val session = d.sessions
                            .filter { it.dayId == day.id && it.slotDate == date.toEpochDay() }
                            .maxByOrNull { it.finishedAt }
                        if (session != null) {
                            SecondaryText(
                                "Выполнено · ${formatDuration(session.durationSec)} · " +
                                    "${session.completedSets}/${session.plannedSets} подходов · " +
                                    "объём ${Units.formatWithLabel(session.volumeKg, d.settings.unit)}",
                                Modifier.padding(top = 4.dp)
                            )
                            SecondaryButton("Подробнее", { onOpenSession(session.id) }, Modifier.fillMaxWidth().padding(top = 12.dp))
                        }
                    }
                    CellKind.MISSED -> {
                        SecondaryText(
                            "Тренировка пропущена. Её можно выполнить сейчас — нагрузка из-за пропуска не увеличивается.",
                            Modifier.padding(top = 4.dp)
                        )
                        PrimaryButton("Выполнить сейчас", { onOpenWorkout(day.id, true) }, Modifier.fillMaxWidth().padding(top = 12.dp))
                    }
                    CellKind.TODAY_PENDING -> {
                        SecondaryText("Запланирована на сегодня.", Modifier.padding(top = 4.dp))
                        PrimaryButton("Начать тренировку", { onOpenWorkout(day.id, true) }, Modifier.fillMaxWidth().padding(top = 12.dp))
                    }
                    else -> {
                        SecondaryText("Запланирована.", Modifier.padding(top = 4.dp))
                        Column(Modifier.padding(top = 8.dp)) {
                            day.exercises.forEach { pe ->
                                Text(
                                    "• ${ProgramData.exercise(pe.exerciseId).name} — ${pe.sets} подх.",
                                    style = MaterialTheme.typography.bodySmall, color = IronColors.TextSecondary
                                )
                            }
                        }
                        SecondaryButton("Открыть план", { onOpenWorkout(day.id, false) }, Modifier.fillMaxWidth().padding(top = 12.dp))
                    }
                }
            }
        }
    }
}
