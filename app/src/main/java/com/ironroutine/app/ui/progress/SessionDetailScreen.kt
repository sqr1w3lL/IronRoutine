package com.ironroutine.app.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ironroutine.app.data.program.MeasureType
import com.ironroutine.app.data.program.ProgramData
import com.ironroutine.app.domain.Ru
import com.ironroutine.app.domain.Units
import com.ironroutine.app.domain.formatDuration
import com.ironroutine.app.ui.components.IronCard
import com.ironroutine.app.ui.components.IronDialog
import com.ironroutine.app.ui.components.Pill
import com.ironroutine.app.ui.components.SecondaryButton
import com.ironroutine.app.ui.components.SecondaryText
import com.ironroutine.app.ui.components.StatTile
import com.ironroutine.app.ui.factoryOf
import com.ironroutine.app.ui.theme.IronColors
import java.time.Instant
import java.time.ZoneId

@Composable
fun SessionDetailScreen(sessionId: Long, onBack: () -> Unit) {
    val vm: SessionDetailViewModel = viewModel(
        key = "session_$sessionId",
        factory = factoryOf { SessionDetailViewModel(it.repository, sessionId) }
    )
    val ui by vm.ui.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    val s = ui ?: return
    val session = s.session

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = IronColors.TextPrimary)
                }
                Column(Modifier.weight(1f)) {
                    Text(s.day?.title ?: "Тренировка", style = MaterialTheme.typography.headlineSmall)
                    if (session != null) {
                        val date = Instant.ofEpochMilli(session.finishedAt).atZone(ZoneId.systemDefault()).toLocalDate()
                        SecondaryText(Ru.full(date))
                    }
                }
            }
        }
        if (session == null) {
            item { SecondaryText("Тренировка не найдена.") }
            return@LazyColumn
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("время", formatDuration(session.durationSec), Modifier.weight(1f), accent = true)
                    StatTile("подходов", "${session.completedSets} / ${session.plannedSets}", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("объём", Units.formatWithLabel(session.volumeKg, s.settings.unit), Modifier.weight(1f))
                    StatTile(
                        "самочувствие",
                        when (session.feeling) { 1 -> "Плохо"; 3 -> "Отлично"; else -> "Нормально" },
                        Modifier.weight(1f)
                    )
                }
            }
        }
        val exerciseIds = s.sets.map { it.exerciseId }.distinct()
        val ordered = s.day?.exercises?.map { it.exerciseId }?.filter { it in exerciseIds } ?: exerciseIds
        ordered.forEach { exId ->
            val def = ProgramData.exerciseOrNull(exId) ?: return@forEach
            val sets = s.sets.filter { it.exerciseId == exId }.sortedBy { it.setIndex }
            val res = s.results.firstOrNull { it.exerciseId == exId }
            item(key = exId) {
                IronCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(def.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        if (res != null && !res.goodTechnique) {
                            Pill("Техника страдала", color = IronColors.Warning.copy(alpha = 0.2f), textColor = IronColors.Warning)
                        }
                    }
                    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        sets.forEach { st ->
                            val unitText = if (def.measure == MeasureType.SECONDS) "с" else "повт."
                            val weightText = if (def.usesWeight && st.weightKg > 0) " × ${Units.formatWithLabel(st.weightKg, s.settings.unit)}" else ""
                            Row {
                                Text(
                                    "Подход ${st.setIndex + 1}: ${st.reps} $unitText$weightText",
                                    Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                                    color = if (st.done) IronColors.TextPrimary else IronColors.TextSecondary
                                )
                                Text(
                                    if (st.done) "✓" else "—",
                                    color = if (st.done) IronColors.Accent else IronColors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            SecondaryButton("Удалить тренировку", { confirmDelete = true }, Modifier.fillMaxWidth(), danger = true)
        }
    }

    if (confirmDelete) {
        IronDialog(
            title = "Удалить тренировку?",
            onDismiss = { confirmDelete = false },
            confirmText = "Удалить",
            dismissText = "Отмена",
            onConfirm = { confirmDelete = false; vm.delete(onBack) }
        ) { SecondaryText("Запись исчезнет из истории, календаря и статистики. Это действие нельзя отменить.") }
    }
}
