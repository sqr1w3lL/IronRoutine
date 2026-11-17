package com.ironroutine.app.ui.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ironroutine.app.data.program.Guides
import com.ironroutine.app.data.program.ProgramData
import com.ironroutine.app.ui.components.ExerciseIllustration
import com.ironroutine.app.ui.components.IronCard
import com.ironroutine.app.ui.components.Pill
import com.ironroutine.app.ui.components.SecondaryText
import com.ironroutine.app.ui.theme.IronColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseInfoScreen(exerciseId: String, onBack: () -> Unit) {
    val def = ProgramData.exerciseOrNull(exerciseId)
    if (def == null) {
        Column(Modifier.padding(16.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = IronColors.TextPrimary)
            }
            Text("Упражнение не найдено")
        }
        return
    }
    val guide = Guides.forExercise(exerciseId)

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = IronColors.TextPrimary)
                }
                Text(def.name, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            }
        }
        item { ExerciseIllustration(def.id, Modifier.fillMaxWidth()) }
        item {
            SecondaryText(
                "Схема показывает начальное и конечное положение. Подробности — ниже.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        item {
            IronCard(Modifier.fillMaxWidth()) {
                Text("Какие мышцы работают", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    def.muscles.forEach { Pill(it, color = IronColors.Accent.copy(alpha = 0.16f), textColor = IronColors.Accent) }
                }
            }
        }
        item { InfoBlock("Исходное положение") { Text(guide.startPosition) } }
        item {
            InfoBlock("Последовательность движений") {
                guide.steps.forEachIndexed { i, step ->
                    Row(Modifier.padding(bottom = 6.dp)) {
                        Text("${i + 1}.", Modifier.width(24.dp), color = IronColors.Accent)
                        Text(step)
                    }
                }
            }
        }
        item { InfoBlock("Правильное дыхание") { Text(guide.breathing) } }
        item { InfoBlock("Типичные ошибки") { Bullets(guide.mistakes, IronColors.Warning) } }
        item { InfoBlock("Безопасность") { Bullets(guide.safety, IronColors.Accent) } }
        guide.easier?.let { text ->
            item { InfoBlock("Если пока тяжело") { Text(text) } }
        }
    }
}

@Composable
private fun InfoBlock(title: String, content: @Composable () -> Unit) {
    IronCard(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = IronColors.Accent)
        Column(Modifier.padding(top = 8.dp)) { content() }
    }
}

@Composable
private fun Bullets(items: List<String>, color: androidx.compose.ui.graphics.Color) {
    items.forEach {
        Row(Modifier.padding(bottom = 6.dp)) {
            Text("•", Modifier.width(18.dp), color = color)
            Text(it)
        }
    }
}
