package com.ironroutine.app.ui.workout

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ironroutine.app.data.program.Equipment
import com.ironroutine.app.data.program.MeasureType
import com.ironroutine.app.data.settings.AppSettings
import com.ironroutine.app.data.settings.WeightUnit
import com.ironroutine.app.domain.SessionHistory
import com.ironroutine.app.domain.Suggestion
import com.ironroutine.app.domain.SuggestionType
import com.ironroutine.app.domain.Units
import com.ironroutine.app.domain.formatClock
import com.ironroutine.app.domain.formatDuration
import com.ironroutine.app.domain.formatNumber
import com.ironroutine.app.domain.pluralRu
import com.ironroutine.app.domain.pluralSets
import com.ironroutine.app.domain.restText
import com.ironroutine.app.domain.targetText
import com.ironroutine.app.ui.components.ExerciseIllustration
import com.ironroutine.app.ui.components.IronCard
import com.ironroutine.app.ui.components.IronDialog
import com.ironroutine.app.ui.components.NumberStepper
import com.ironroutine.app.ui.components.Pill
import com.ironroutine.app.ui.components.PrimaryButton
import com.ironroutine.app.ui.components.ProgressBar
import com.ironroutine.app.ui.components.SecondaryButton
import com.ironroutine.app.ui.components.SecondaryText
import com.ironroutine.app.ui.factoryOf
import com.ironroutine.app.ui.theme.IronColors
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun WorkoutScreen(
    dayId: String,
    autostart: Boolean,
    onBack: () -> Unit,
    onOpenExercise: (String) -> Unit,
    onOpenOtherWorkout: (String) -> Unit
) {
    val vm: WorkoutViewModel = viewModel(
        key = "workout_$dayId",
        factory = factoryOf { WorkoutViewModel(it.repository, dayId, autostart) }
    )
    val ui by vm.ui.collectAsStateWithLifecycle()
    val rest by vm.rest.collectAsStateWithLifecycle()
    val finished by vm.finished.collectAsStateWithLifecycle()

    var showFinish by remember { mutableStateOf(false) }
    var showCancel by remember { mutableStateOf(false) }
    var showReplace by remember { mutableStateOf(false) }

    val startedAt = ui.session?.startedAt
    val elapsed by produceState(0, startedAt) {
        val start = startedAt ?: return@produceState
        while (true) {
            value = ((System.currentTimeMillis() - start) / 1000).toInt()
            delay(1000)
        }
    }

    val day = ui.day
    if (ui.loading || day == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = IronColors.Accent)
        }
        return
    }
    val session = ui.session

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = IronColors.TextPrimary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(day.title, style = MaterialTheme.typography.headlineSmall)
                        SecondaryText(day.subtitle)
                    }
                    if (session != null) Pill("⏱ " + formatClock(elapsed), color = IronColors.Accent.copy(alpha = 0.18f), textColor = IronColors.Accent)
                }
            }

            if (session == null) {
                item {
                    val other = ui.otherActive
                    if (other != null) {
                        IronCard(Modifier.fillMaxWidth(), color = IronColors.Warning.copy(alpha = 0.12f)) {
                            Text(
                                "Не завершена тренировка ${ui.otherActiveDay?.title ?: ""}",
                                style = MaterialTheme.typography.titleSmall, color = IronColors.Warning
                            )
                            SecondaryText("Её данные сохранены. Можно продолжить или отменить её и начать эту.")
                            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SecondaryButton("Продолжить её", { onOpenOtherWorkout(other.dayId) }, Modifier.weight(1f))
                                SecondaryButton("Начать эту", { showReplace = true }, Modifier.weight(1f), danger = true)
                            }
                        }
                    } else {
                        PrimaryButton("Начать тренировку", { vm.startWorkout(false) }, Modifier.fillMaxWidth())
                        SecondaryText(
                            "Сейчас режим просмотра. После старта можно отмечать подходы, а данные сохраняются автоматически.",
                            Modifier.padding(top = 6.dp), MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                item {
                    val total = ui.plannedSets
                    val done = ui.doneSets
                    IronCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Выполнено $done из $total", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                            SecondaryText(pluralSets(total))
                        }
                        ProgressBar(if (total == 0) 0f else done.toFloat() / total, Modifier.padding(top = 10.dp))
                    }
                }
            }

            items(ui.exercises, key = { it.planned.exerciseId }) { ex ->
                ExerciseCard(
                    ex = ex,
                    started = session != null,
                    settings = ui.settings,
                    onInfo = { onOpenExercise(ex.def.id) },
                    onUpdateSet = { idx, reps, w -> vm.updateSet(ex.def.id, idx, reps, w) },
                    onToggle = { idx -> vm.toggleDone(ex.def.id, idx) },
                    onTechnique = { vm.setTechnique(ex.def.id, it) },
                    onVariant = { vm.setVariant(ex.def.id, it) },
                    onResolve = { vm.resolveSuggestion(ex.def.id, it) }
                )
            }

            if (session != null) {
                item {
                    Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrimaryButton("Завершить тренировку", { showFinish = true }, Modifier.fillMaxWidth())
                        SecondaryButton("Отменить тренировку", { showCancel = true }, Modifier.fillMaxWidth(), danger = true)
                    }
                }
            }
        }

        rest?.let { r ->
            RestTimerBar(
                rest = r,
                vibrate = ui.settings.vibrate,
                onAdd = { vm.addRest(15) },
                onSkip = { vm.stopRest() },
                onFinished = { vm.stopRest() },
                modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)
            )
        }
    }

    if (showFinish && session != null) {
        var feeling by remember { mutableIntStateOf(2) }
        val done = ui.doneSets
        IronDialog(
            title = "Завершить тренировку?",
            onDismiss = { showFinish = false },
            confirmText = "Завершить",
            confirmEnabled = done > 0,
            dismissText = "Назад",
            onConfirm = { showFinish = false; vm.finish(feeling) }
        ) {
            if (done == 0) {
                SecondaryText("Отметь хотя бы один подход, чтобы сохранить тренировку.")
            } else {
                Text("Выполнено $done из ${ui.plannedSets} подходов.")
                if (done < ui.plannedSets) {
                    SecondaryText("Невыполненные подходы не попадут в статистику.", Modifier.padding(top = 4.dp))
                }
                Text("Как самочувствие?", Modifier.padding(top = 14.dp, bottom = 6.dp), style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1 to "Плохо", 2 to "Нормально", 3 to "Отлично").forEach { (v, label) ->
                        FilterChip(
                            selected = feeling == v,
                            onClick = { feeling = v },
                            label = { Text(label) },
                            border = null,
                            colors = chipColors()
                        )
                    }
                }
            }
        }
    }

    if (showCancel) {
        IronDialog(
            title = "Отменить тренировку?",
            onDismiss = { showCancel = false },
            confirmText = "Отменить тренировку",
            dismissText = "Назад",
            onConfirm = { showCancel = false; vm.cancelWorkout(); onBack() }
        ) { SecondaryText("Все данные этой тренировки будут удалены.") }
    }

    if (showReplace) {
        IronDialog(
            title = "Заменить активную тренировку?",
            onDismiss = { showReplace = false },
            confirmText = "Заменить",
            dismissText = "Назад",
            onConfirm = { showReplace = false; vm.startWorkout(true) }
        ) { SecondaryText("Данные незавершённой тренировки будут удалены, и начнётся ${day.title}.") }
    }

    finished?.let { f ->
        IronDialog(
            title = "Тренировка завершена",
            onDismiss = onBack,
            confirmText = "Готово",
            dismissText = null,
            onConfirm = onBack
        ) {
            Text("Подходов: ${f.completedSets} из ${f.plannedSets}")
            Text("Время: ${formatDuration(f.durationSec)}", Modifier.padding(top = 4.dp))
            Text(
                "Объём: ${Units.formatWithLabel(f.volumeKg, ui.settings.unit)}",
                Modifier.padding(top = 4.dp)
            )
            SecondaryText("Результаты сохранены. Рекомендации по нагрузке появятся на следующей такой же тренировке.", Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    containerColor = IronColors.Surface2,
    labelColor = IronColors.TextPrimary,
    selectedContainerColor = IronColors.Accent,
    selectedLabelColor = IronColors.OnAccent
)

@Composable
private fun ExerciseCard(
    ex: ExerciseUi,
    started: Boolean,
    settings: AppSettings,
    onInfo: () -> Unit,
    onUpdateSet: (index: Int, reps: Int, weightKg: Double) -> Unit,
    onToggle: (index: Int) -> Unit,
    onTechnique: (Boolean) -> Unit,
    onVariant: (Int) -> Unit,
    onResolve: (Boolean) -> Unit
) {
    val def = ex.def
    val plan = ex.planned
    IronCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            ExerciseIllustration(def.id, Modifier.width(112.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(def.name, style = MaterialTheme.typography.titleMedium)
                SecondaryText(def.muscles.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                Text(
                    "${pluralSets(plan.sets)} · ${targetText(plan, def, ex.extraReps)}",
                    Modifier.padding(top = 6.dp), style = MaterialTheme.typography.labelLarge, color = IronColors.Accent
                )
                SecondaryText("Отдых ${restText(plan)}", style = MaterialTheme.typography.bodySmall)
            }
        }
        plan.note?.let { SecondaryText(it, Modifier.padding(top = 8.dp), MaterialTheme.typography.bodySmall) }
        SecondaryText(def.shortTip, Modifier.padding(top = 8.dp))
        TextButton(
            onClick = onInfo,
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = IronColors.Accent)
        ) { Text("Техника выполнения →") }

        ex.lastHistory?.let { h ->
            SecondaryText(lastSummary(h, ex, settings.unit), style = MaterialTheme.typography.bodySmall)
        }

        Box(Modifier.padding(top = 10.dp)) {
            SuggestionCard(ex.suggestion, onResolve)
        }

        if (started) {
            if (def.id == "pullup") {
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0 to "Обычные", 1 to "Негативные", 2 to "С помощью").forEach { (v, label) ->
                        FilterChip(
                            selected = ex.variant == v,
                            onClick = { onVariant(v) },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                            border = null,
                            colors = chipColors()
                        )
                    }
                }
            }

            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ex.sets.forEach { set ->
                    SetRow(
                        index = set.setIndex,
                        reps = set.reps,
                        weightKg = set.weightKg,
                        done = set.done,
                        usesWeight = def.usesWeight,
                        seconds = def.measure == MeasureType.SECONDS,
                        barbell = def.equipment == Equipment.BARBELL,
                        unit = settings.unit,
                        onChange = { r, w -> onUpdateSet(set.setIndex, r, w) },
                        onToggle = { onToggle(set.setIndex) }
                    )
                }
            }

            if (ex.allDone) {
                Pill(
                    "Упражнение выполнено", Modifier.padding(top = 10.dp),
                    color = IronColors.Accent.copy(alpha = 0.2f), textColor = IronColors.Accent
                )
            }
            if (ex.doneCount > 0) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Техника была чистой", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = ex.goodTechnique,
                        onCheckedChange = onTechnique,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = IronColors.OnAccent,
                            checkedTrackColor = IronColors.Accent,
                            uncheckedThumbColor = IronColors.TextSecondary,
                            uncheckedTrackColor = IronColors.Surface2,
                            uncheckedBorderColor = IronColors.Outline
                        )
                    )
                }
            }
        }
    }
}

private fun lastSummary(h: SessionHistory, ex: ExerciseUi, unit: WeightUnit): String {
    val reps = h.sets.joinToString(" · ") { it.reps.toString() }
    val suffix = if (ex.def.measure == MeasureType.SECONDS) " с" else ""
    val weight = if (ex.def.usesWeight && h.maxWeight > 0) " × ${Units.formatWithLabel(h.maxWeight, unit)}" else ""
    return "Прошлый раз: $reps$suffix$weight"
}

@Composable
private fun SetRow(
    index: Int,
    reps: Int,
    weightKg: Double,
    done: Boolean,
    usesWeight: Boolean,
    seconds: Boolean,
    barbell: Boolean,
    unit: WeightUnit,
    onChange: (Int, Double) -> Unit,
    onToggle: () -> Unit
) {
    val displayWeight = (Units.toDisplay(weightKg, unit) * 10).roundToInt() / 10.0
    val weightStep = when {
        barbell && unit == WeightUnit.KG -> 2.5
        barbell -> 5.0
        unit == WeightUnit.KG -> 0.5
        else -> 1.0
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (done) IronColors.Accent.copy(alpha = 0.10f) else IronColors.Background.copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Подход ${index + 1}", style = MaterialTheme.typography.labelMedium, color = IronColors.TextSecondary)
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    NumberStepper(
                        value = reps.toDouble(),
                        step = if (seconds) 5.0 else 1.0,
                        onValueChange = { onChange(it.toInt(), weightKg) },
                        compact = true
                    )
                    SecondaryText(if (seconds) "секунды" else "повторения", style = MaterialTheme.typography.labelSmall)
                }
                if (usesWeight) {
                    Column {
                        NumberStepper(
                            value = displayWeight,
                            step = weightStep,
                            onValueChange = { onChange(reps, Units.fromDisplay(it, unit)) },
                            compact = true
                        )
                        SecondaryText("вес, ${Units.label(unit)}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(if (done) IronColors.Accent else IronColors.Surface2)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = if (done) "Подход выполнен (нажми, чтобы отменить)" else "Подход выполнен",
                tint = if (done) IronColors.OnAccent else IronColors.TextSecondary
            )
        }
    }
}

@Composable
private fun SuggestionCard(sug: Suggestion, onResolve: (Boolean) -> Unit) {
    val (bg, fg, title) = when (sug.type) {
        SuggestionType.INCREASE_WEIGHT, SuggestionType.INCREASE_REPS ->
            Triple(IronColors.Accent.copy(alpha = 0.12f), IronColors.Accent, "Рекомендация: увеличить нагрузку")
        SuggestionType.REDUCE_WEIGHT, SuggestionType.REDUCE_REPS ->
            Triple(IronColors.Warning.copy(alpha = 0.14f), IronColors.Warning, "Рекомендация: снизить нагрузку")
        SuggestionType.KEEP ->
            Triple(IronColors.Warning.copy(alpha = 0.10f), IronColors.Warning, "Нагрузку сохраняем")
        SuggestionType.FIRST ->
            Triple(IronColors.Surface2, IronColors.TextPrimary, "Первая тренировка")
        SuggestionType.GOAL ->
            Triple(IronColors.Surface2, IronColors.TextPrimary, "Цель")
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .padding(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = fg)
        Text(sug.message, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodyMedium)
        if (sug.needsConfirmation) {
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton("Принять", { onResolve(true) }, Modifier.weight(1f).height(44.dp))
                SecondaryButton("Оставить как есть", { onResolve(false) }, Modifier.weight(1f).height(44.dp))
            }
        }
    }
}

@Composable
private fun RestTimerBar(
    rest: RestState,
    vibrate: Boolean,
    onAdd: () -> Unit,
    onSkip: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    var now by remember(rest.endsAtMs) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(rest.endsAtMs) {
        while (System.currentTimeMillis() < rest.endsAtMs) {
            now = System.currentTimeMillis()
            delay(200)
        }
        if (vibrate) vibrateOnce(ctx)
        onFinished()
    }
    val remaining = (((rest.endsAtMs - now) + 999) / 1000).toInt().coerceAtLeast(0)
    val progress = if (rest.totalSec == 0) 0f else remaining.toFloat() / rest.totalSec

    IronCard(
        modifier.fillMaxWidth(),
        color = IronColors.Surface2,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SecondaryText("Отдых", style = MaterialTheme.typography.labelMedium)
                Text(formatClock(remaining), style = MaterialTheme.typography.headlineSmall, color = IronColors.Accent)
            }
            TextButton(onClick = onAdd, colors = ButtonDefaults.textButtonColors(contentColor = IronColors.TextPrimary)) { Text("+15 с") }
            TextButton(onClick = onSkip, colors = ButtonDefaults.textButtonColors(contentColor = IronColors.Accent)) { Text("Пропустить") }
        }
        ProgressBar(progress, Modifier.padding(top = 8.dp), height = 6.dp)
    }
}

private fun vibrateOnce(ctx: Context) {
    val v = ctx.getSystemService(Vibrator::class.java) ?: return
    if (v.hasVibrator()) v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
}
