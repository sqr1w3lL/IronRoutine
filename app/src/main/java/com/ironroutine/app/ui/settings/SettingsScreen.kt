package com.ironroutine.app.ui.settings

import android.app.DatePickerDialog
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ironroutine.app.data.db.PlanOverrideEntity
import com.ironroutine.app.data.program.MeasureType
import com.ironroutine.app.data.program.PlannedExercise
import com.ironroutine.app.data.program.ProgramData
import com.ironroutine.app.data.settings.AppSettings
import com.ironroutine.app.data.settings.WeightUnit
import com.ironroutine.app.domain.Ru
import com.ironroutine.app.domain.Units
import com.ironroutine.app.domain.rangeText
import com.ironroutine.app.domain.restText
import com.ironroutine.app.ui.components.IronCard
import com.ironroutine.app.ui.components.IronDialog
import com.ironroutine.app.ui.components.NumberStepper
import com.ironroutine.app.ui.components.SecondaryButton
import com.ironroutine.app.ui.components.SecondaryText
import com.ironroutine.app.ui.components.SectionTitle
import com.ironroutine.app.ui.factoryOf
import com.ironroutine.app.ui.theme.IronColors
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = factoryOf { SettingsViewModel(it.repository, it.backup) })
    val ui by vm.ui.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    var editing by remember { mutableStateOf<Pair<String, PlannedExercise>?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) vm.exportTo(uri, context.contentResolver) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) pendingImport = uri }

    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            vm.consumeMessage()
        }
    }

    val s = ui ?: return
    val st = s.settings
    val unit = st.unit
    val unitLabel = Units.label(unit)
    val weightStep = if (unit == WeightUnit.KG) 0.5 else 1.0

    fun pickDate(current: LocalDate, onPicked: (LocalDate) -> Unit) {
        DatePickerDialog(
            context,
            { _, y, m, d -> onPicked(LocalDate.of(y, m + 1, d)) },
            current.year, current.monthValue - 1, current.dayOfMonth
        ).show()
    }

    /** Строка «подпись — число со степпером». Значения хранятся в кг, показываются в выбранных единицах. */
    @Composable
    fun WeightRow(label: String, kg: Double, step: Double = weightStep, onChange: (Double) -> Unit) {
        val shown = (Units.toDisplay(kg, unit) * 10).roundToInt() / 10.0
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            NumberStepper(
                value = shown, step = step,
                onValueChange = { onChange(Units.fromDisplay(it, unit)) },
                compact = true
            )
        }
    }

    @Composable
    fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = checked, onCheckedChange = onChange,
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

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = IronColors.TextPrimary)
                }
                Text("Настройки", style = MaterialTheme.typography.headlineSmall)
            }
        }

        item {
            IronCard(Modifier.fillMaxWidth()) {
                Text("Единицы измерения веса", style = MaterialTheme.typography.titleSmall)
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(WeightUnit.KG to "Килограммы", WeightUnit.LB to "Фунты").forEach { (u, label) ->
                        FilterChip(
                            selected = unit == u,
                            onClick = { vm.update { it.copy(unit = u) } },
                            label = { Text(label) },
                            border = null,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = IronColors.Surface2,
                                labelColor = IronColors.TextPrimary,
                                selectedContainerColor = IronColors.Accent,
                                selectedLabelColor = IronColors.OnAccent
                            )
                        )
                    }
                }
            }
        }

        item {
            IronCard(Modifier.fillMaxWidth()) {
                Text("Оборудование", style = MaterialTheme.typography.titleSmall)
                SecondaryText("Влияет на стартовые веса и на возможность увеличивать нагрузку.", Modifier.padding(bottom = 6.dp), MaterialTheme.typography.bodySmall)
                WeightRow("Одна гантель, $unitLabel", st.dumbbellKg) { v -> vm.update { it.copy(dumbbellKg = v) } }
                SwitchRow("Гантели разборные (вес можно менять)", st.dumbbellAdjustable) { v ->
                    vm.update { it.copy(dumbbellAdjustable = v) }
                }
                if (st.dumbbellAdjustable) {
                    WeightRow("Шаг прибавки гантелей, $unitLabel", st.dumbbellStepKg, 0.5) { v ->
                        vm.update { it.copy(dumbbellStepKg = v.coerceAtLeast(0.1)) }
                    }
                }
                WeightRow("Штанга с блинами (общий вес), $unitLabel", st.barbellKg, weightStep * 5) { v ->
                    vm.update { it.copy(barbellKg = v) }
                }
                WeightRow("Шаг прибавки штанги, $unitLabel", st.barbellStepKg, 0.5) { v ->
                    vm.update { it.copy(barbellStepKg = v.coerceAtLeast(0.1)) }
                }
            }
        }

        item {
            IronCard(Modifier.fillMaxWidth()) {
                Text("Отдых между подходами", style = MaterialTheme.typography.titleSmall)
                SwitchRow("Запускать таймер после подхода", st.autoRest) { v -> vm.update { it.copy(autoRest = v) } }
                SwitchRow("Вибрация, когда отдых закончился", st.vibrate) { v -> vm.update { it.copy(vibrate = v) } }
            }
        }

        item {
            IronCard(Modifier.fillMaxWidth()) {
                Text("Период программы", style = MaterialTheme.typography.titleSmall)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Начало", Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            pickDate(st.programStart) { d ->
                                vm.update {
                                    val end = if (d.isAfter(it.programEnd)) d.plusWeeks(6) else it.programEnd
                                    it.copy(programStartDay = d.toEpochDay(), programEndDay = end.toEpochDay())
                                }
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = IronColors.Accent)
                    ) { Text(Ru.dmy(st.programStart)) }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Конец", Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            pickDate(st.programEnd) { d ->
                                vm.update { it.copy(programEndDay = maxOf(d.toEpochDay(), it.programStartDay)) }
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = IronColors.Accent)
                    ) { Text(Ru.dmy(st.programEnd)) }
                }
                SecondaryText(
                    "После окончания периода тренировки не прекращаются: расписание Пн / Ср / Пт продолжается, а прогресс сохраняется. Просто продли дату окончания.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        item { SectionTitle("Параметры тренировок") }
        s.plan.forEach { day ->
            item(key = "plan_${day.id}") {
                IronCard(Modifier.fillMaxWidth()) {
                    Text("${day.title} · ${day.subtitle}", style = MaterialTheme.typography.titleSmall)
                    day.exercises.forEach { pe ->
                        val def = ProgramData.exercise(pe.exerciseId)
                        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(def.name, style = MaterialTheme.typography.bodyMedium)
                                SecondaryText(
                                    "${pe.sets} × ${rangeText(pe.repsMin, pe.repsMax)}" +
                                        (if (def.measure == MeasureType.SECONDS) " с" else "") +
                                        " · отдых ${restText(pe)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            TextButton(
                                onClick = { editing = day.id to pe },
                                colors = ButtonDefaults.textButtonColors(contentColor = IronColors.Accent)
                            ) { Text("Изменить") }
                        }
                    }
                }
            }
        }

        item {
            IronCard(Modifier.fillMaxWidth()) {
                Text("Резервная копия", style = MaterialTheme.typography.titleSmall)
                SecondaryText(
                    "Все тренировки, замеры и настройки сохраняются в один файл. Его можно перенести на другой телефон.",
                    Modifier.padding(vertical = 6.dp), MaterialTheme.typography.bodySmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(
                        "Сохранить в файл",
                        { exportLauncher.launch("iron_routine_backup_${LocalDate.now()}.json") },
                        Modifier.weight(1f)
                    )
                    SecondaryButton(
                        "Восстановить",
                        { importLauncher.launch(arrayOf("*/*")) },
                        Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            SecondaryText(
                "IRON ROUTINE 1.0.0 · все данные хранятся только на этом устройстве",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    pendingImport?.let { uri ->
        IronDialog(
            title = "Восстановить данные?",
            onDismiss = { pendingImport = null },
            confirmText = "Восстановить",
            onConfirm = { vm.importFrom(uri, context.contentResolver); pendingImport = null }
        ) {
            SecondaryText("Текущие данные приложения будут заменены данными из файла.")
        }
    }

    editing?.let { (dayId, pe) ->
        val def = ProgramData.exercise(pe.exerciseId)
        val seconds = def.measure == MeasureType.SECONDS
        var sets by remember { mutableDoubleStateOf(pe.sets.toDouble()) }
        var repsMin by remember { mutableDoubleStateOf(pe.repsMin.toDouble()) }
        var repsMax by remember { mutableDoubleStateOf(pe.repsMax.toDouble()) }
        var restMin by remember { mutableDoubleStateOf(pe.restMin.toDouble()) }
        var restMax by remember { mutableDoubleStateOf(pe.restMax.toDouble()) }
        IronDialog(
            title = def.name,
            onDismiss = { editing = null },
            confirmText = "Сохранить",
            onConfirm = {
                val lo = repsMin.toInt().coerceAtLeast(1)
                val rLo = restMin.toInt().coerceAtLeast(0)
                vm.saveOverride(
                    PlanOverrideEntity(
                        dayId = dayId, exerciseId = pe.exerciseId,
                        sets = sets.toInt().coerceIn(1, 10),
                        repsMin = lo, repsMax = maxOf(lo, repsMax.toInt()),
                        restMin = rLo, restMax = maxOf(rLo, restMax.toInt())
                    )
                )
                editing = null
            }
        ) {
            @Composable
            fun Line(label: String, value: Double, step: Double, onChange: (Double) -> Unit) {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    NumberStepper(value = value, step = step, onValueChange = onChange, compact = true)
                }
            }
            Line("Подходов", sets, 1.0) { sets = it }
            Line(if (seconds) "Секунд от" else "Повторений от", repsMin, if (seconds) 5.0 else 1.0) { repsMin = it }
            Line(if (seconds) "Секунд до" else "Повторений до", repsMax, if (seconds) 5.0 else 1.0) { repsMax = it }
            Line("Отдых от, с", restMin, 5.0) { restMin = it }
            Line("Отдых до, с", restMax, 5.0) { restMax = it }
            SecondaryButton(
                "Вернуть как в программе",
                { vm.resetOverride(dayId, pe.exerciseId); editing = null },
                Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}
