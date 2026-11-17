package com.ironroutine.app.ui.progress

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ironroutine.app.data.program.ProgramData
import com.ironroutine.app.domain.Ru
import com.ironroutine.app.domain.Units
import com.ironroutine.app.domain.formatDuration
import com.ironroutine.app.domain.formatNumber
import com.ironroutine.app.domain.parseNumber
import com.ironroutine.app.ui.components.BarChart
import com.ironroutine.app.ui.components.ChartPoint
import com.ironroutine.app.ui.components.IronCard
import com.ironroutine.app.ui.components.IronDialog
import com.ironroutine.app.ui.components.LineChart
import com.ironroutine.app.ui.components.PrimaryButton
import com.ironroutine.app.ui.components.SecondaryText
import com.ironroutine.app.ui.components.SectionTitle
import com.ironroutine.app.ui.components.StatTile
import com.ironroutine.app.ui.factoryOf
import com.ironroutine.app.ui.theme.IronColors
import java.time.Instant
import java.time.ZoneId

private enum class Metric(val label: String, val unit: String) {
    WEIGHT("Вес тела", ""),
    WAIST("Талия", "см"),
    CHEST("Грудь", "см"),
    ARM("Рука", "см")
}

@Composable
fun ProgressScreen(onOpenSession: (Long) -> Unit) {
    val vm: ProgressViewModel = viewModel(factory = factoryOf { ProgressViewModel(it.repository) })
    val ui by vm.ui.collectAsStateWithLifecycle()
    val s = ui ?: return

    var selectedExercise by rememberSaveable { mutableStateOf<String?>(null) }
    var metric by rememberSaveable { mutableStateOf(Metric.WEIGHT.name) }
    var showAdd by remember { mutableStateOf(false) }
    val unit = s.settings.unit
    val unitLabel = Units.label(unit)
    val currentExercise = s.series.firstOrNull { it.exerciseId == selectedExercise } ?: s.series.firstOrNull()
    val m = Metric.valueOf(metric)

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Прогресс", style = MaterialTheme.typography.headlineMedium)
            SecondaryText("Только реальные данные твоих тренировок")
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("тренировок", s.totalWorkouts.toString(), Modifier.weight(1f), accent = true)
                    StatTile("пропущено", s.missed.toString(), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("упражнений выполнено", s.exercisesDone.toString(), Modifier.weight(1f))
                    StatTile("общее время", formatDuration(s.totalDurationSec), Modifier.weight(1f))
                }
            }
        }
        item {
            IronCard(Modifier.fillMaxWidth()) {
                Text("Активность", style = MaterialTheme.typography.titleSmall)
                SecondaryText("Тренировок по неделям (последние 8)", style = MaterialTheme.typography.bodySmall)
                Column(Modifier.padding(top = 10.dp)) { BarChart(s.weeklyBars) }
            }
        }

        item { SectionTitle("Рабочие веса и повторения") }
        if (currentExercise == null) {
            item {
                IronCard(Modifier.fillMaxWidth()) {
                    SecondaryText("Графики появятся после первой завершённой тренировки.")
                }
            }
        } else {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(s.series, key = { it.exerciseId }) { ex ->
                        FilterChip(
                            selected = ex.exerciseId == currentExercise.exerciseId,
                            onClick = { selectedExercise = ex.exerciseId },
                            label = { Text(ex.name) },
                            border = null,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = IronColors.Card,
                                labelColor = IronColors.TextPrimary,
                                selectedContainerColor = IronColors.Accent,
                                selectedLabelColor = IronColors.OnAccent
                            )
                        )
                    }
                }
            }
            if (currentExercise.usesWeight) {
                item {
                    IronCard(Modifier.fillMaxWidth()) {
                        Text("Рабочий вес, $unitLabel", style = MaterialTheme.typography.titleSmall)
                        Column(Modifier.padding(top = 8.dp)) {
                            LineChart(
                                currentExercise.weights.map { ChartPoint(it.label, Units.toDisplay(it.value, unit)) },
                                unitLabel
                            )
                        }
                    }
                }
            }
            item {
                IronCard(Modifier.fillMaxWidth()) {
                    Text(
                        if (currentExercise.seconds) "Секунды за тренировку (сумма)" else "Повторения за тренировку (сумма)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Column(Modifier.padding(top = 8.dp)) {
                        LineChart(currentExercise.reps, if (currentExercise.seconds) "с" else "повт.")
                    }
                }
            }
        }

        item { SectionTitle("Замеры тела") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Metric.values().toList(), key = { it.name }) { mt ->
                    FilterChip(
                        selected = mt == m,
                        onClick = { metric = mt.name },
                        label = { Text(mt.label) },
                        border = null,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = IronColors.Card,
                            labelColor = IronColors.TextPrimary,
                            selectedContainerColor = IronColors.Accent,
                            selectedLabelColor = IronColors.OnAccent
                        )
                    )
                }
            }
        }
        item {
            val points = s.measurements.mapNotNull { e ->
                val v = when (m) {
                    Metric.WEIGHT -> e.bodyWeightKg?.let { Units.toDisplay(it, unit) }
                    Metric.WAIST -> e.waistCm
                    Metric.CHEST -> e.chestCm
                    Metric.ARM -> e.armCm
                }
                v?.let { ChartPoint(Ru.dm(java.time.LocalDate.ofEpochDay(e.date)), it) }
            }
            val u = if (m == Metric.WEIGHT) unitLabel else m.unit
            IronCard(Modifier.fillMaxWidth()) {
                Text("${m.label}, $u", style = MaterialTheme.typography.titleSmall)
                Column(Modifier.padding(top = 8.dp)) {
                    LineChart(points, u, emptyText = "Добавь замеры — здесь появится график изменений.")
                }
                if (points.size >= 2) {
                    val diff = points.last().value - points.first().value
                    SecondaryText(
                        "Изменение за всё время: ${if (diff > 0) "+" else ""}${formatNumber(diff)} $u",
                        Modifier.padding(top = 6.dp), MaterialTheme.typography.bodySmall
                    )
                }
                PrimaryButton("Добавить замер", { showAdd = true }, Modifier.fillMaxWidth().padding(top = 12.dp))
            }
        }

        item { SectionTitle("История тренировок") }
        if (s.history.isEmpty()) {
            item { IronCard(Modifier.fillMaxWidth()) { SecondaryText("Завершённых тренировок пока нет.") } }
        } else {
            items(s.history, key = { it.id }) { session ->
                val day = s.plan.firstOrNull { it.id == session.dayId }
                val date = Instant.ofEpochMilli(session.finishedAt).atZone(ZoneId.systemDefault()).toLocalDate()
                IronCard(Modifier.fillMaxWidth(), onClick = { onOpenSession(session.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(day?.title ?: session.dayId.uppercase(), style = MaterialTheme.typography.titleMedium)
                            SecondaryText(Ru.full(date), style = MaterialTheme.typography.bodySmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${session.completedSets}/${session.plannedSets} подх.", style = MaterialTheme.typography.labelLarge)
                            SecondaryText(formatDuration(session.durationSec), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var w by remember { mutableStateOf("") }
        var waist by remember { mutableStateOf("") }
        var chest by remember { mutableStateOf("") }
        var arm by remember { mutableStateOf("") }
        val any = listOf(w, waist, chest, arm).any { parseNumber(it) != null }
        IronDialog(
            title = "Новый замер",
            onDismiss = { showAdd = false },
            confirmText = "Сохранить",
            confirmEnabled = any,
            onConfirm = {
                vm.addMeasurement(
                    parseNumber(w)?.let { Units.fromDisplay(it, unit) },
                    parseNumber(waist), parseNumber(chest), parseNumber(arm)
                )
                showAdd = false
            }
        ) {
            SecondaryText("Заполни только то, что измерил сегодня.", Modifier.padding(bottom = 8.dp))
            MeasureField("Вес тела, $unitLabel", w) { w = it }
            MeasureField("Обхват талии, см", waist) { waist = it }
            MeasureField("Обхват груди, см", chest) { chest = it }
            MeasureField("Обхват руки, см", arm) { arm = it }
        }
    }
}

@Composable
private fun MeasureField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}
