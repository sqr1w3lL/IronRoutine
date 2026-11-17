package com.ironroutine.app.ui.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ironroutine.app.data.program.ProgramData
import com.ironroutine.app.domain.Ru
import com.ironroutine.app.domain.pluralSets
import com.ironroutine.app.domain.rangeText
import com.ironroutine.app.ui.components.IronCard
import com.ironroutine.app.ui.components.Pill
import com.ironroutine.app.ui.components.PrimaryButton
import com.ironroutine.app.ui.components.SecondaryText
import com.ironroutine.app.ui.factoryOf
import com.ironroutine.app.ui.theme.IronColors

@Composable
fun WorkoutsScreen(
    onOpenWorkout: (dayId: String, autostart: Boolean) -> Unit,
    onOpenExercise: (String) -> Unit
) {
    val vm: WorkoutsViewModel = viewModel(factory = factoryOf { WorkoutsViewModel(it.repository) })
    val ui by vm.ui.collectAsStateWithLifecycle()
    val s = ui ?: return

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Тренировки", style = MaterialTheme.typography.headlineMedium)
            SecondaryText("Три дня в неделю: понедельник, среда, пятница")
        }
        items(s.plan, key = { it.id }) { day ->
            val isActive = s.active?.dayId == day.id
            IronCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        SecondaryText(Ru.weekdayCap(java.time.LocalDate.now().with(java.time.temporal.TemporalAdjusters.nextOrSame(day.weekday))), style = MaterialTheme.typography.labelMedium)
                        Text(day.title, style = MaterialTheme.typography.titleLarge)
                        SecondaryText(day.subtitle, style = MaterialTheme.typography.bodySmall)
                    }
                    if (isActive) Pill("В процессе", color = IronColors.Warning.copy(alpha = 0.2f), textColor = IronColors.Warning)
                }
                Column(Modifier.padding(top = 10.dp)) {
                    day.exercises.forEach { pe ->
                        val def = ProgramData.exercise(pe.exerciseId)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onOpenExercise(def.id) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(def.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            SecondaryText(
                                "${pe.sets} × ${rangeText(pe.repsMin, pe.repsMax)}" +
                                    if (def.measure == com.ironroutine.app.data.program.MeasureType.SECONDS) " с" else "",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                SecondaryText(
                    "${day.exercises.size} упражнений · ${pluralSets(day.exercises.sumOf { it.sets })}",
                    Modifier.padding(top = 4.dp), MaterialTheme.typography.bodySmall
                )
                PrimaryButton(
                    if (isActive) "Продолжить тренировку" else "Начать тренировку",
                    { onOpenWorkout(day.id, !isActive) },
                    Modifier.fillMaxWidth().padding(top = 12.dp)
                )
                androidx.compose.material3.TextButton(
                    onClick = { onOpenWorkout(day.id, false) },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = IronColors.Accent)
                ) { Text("Открыть план") }
            }
        }
    }
}
