package com.ironroutine.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ironroutine.app.domain.formatNumber
import com.ironroutine.app.domain.parseNumber
import com.ironroutine.app.ui.theme.IronColors

@Composable
fun IronCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = IronColors.Card,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    val tap = if (onClick != null) Modifier.clip(shape).clickable(onClick = onClick) else Modifier
    Surface(modifier = modifier.then(tap), shape = shape, color = color, border = border) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text, modifier = modifier.padding(top = 8.dp),
        style = MaterialTheme.typography.titleMedium, color = IronColors.TextPrimary
    )
}

@Composable
fun SecondaryText(text: String, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.bodyMedium) {
    Text(text, modifier = modifier, style = style, color = IronColors.TextSecondary)
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(IronColors.Card)
            .padding(14.dp)
    ) {
        Text(
            value, style = MaterialTheme.typography.headlineSmall,
            color = if (accent) IronColors.Accent else IronColors.TextPrimary
        )
        Spacer4()
        SecondaryText(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Spacer4() = Box(Modifier.height(4.dp))

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = IronColors.Accent,
            contentColor = IronColors.OnAccent,
            disabledContainerColor = IronColors.Surface2,
            disabledContentColor = IronColors.TextSecondary
        )
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, danger: Boolean = false) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (danger) IronColors.Danger else IronColors.Outline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (danger) IronColors.Danger else IronColors.TextPrimary
        )
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun ProgressBar(progress: Float, modifier: Modifier = Modifier, height: Dp = 10.dp) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height)),
        color = IronColors.Accent,
        trackColor = IronColors.Surface2,
        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {}
    )
}

@Composable
fun Pill(text: String, modifier: Modifier = Modifier, color: Color = IronColors.Surface2, textColor: Color = IronColors.TextPrimary) {
    Text(
        text,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = textColor
    )
}

/** Круглая кнопка с иконкой +/- для шагового поля. */
@Composable
private fun StepButton(plus: Boolean, size: Dp, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(IronColors.Surface2)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (plus) Icons.Filled.Add else Icons.Filled.Remove,
            contentDescription = if (plus) "Больше" else "Меньше",
            tint = IronColors.TextPrimary,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Числовое поле с кнопками − и +. [value] — число в отображаемых единицах,
 * [onValueChange] вызывается с корректным числом >= 0.
 */
@Composable
fun NumberStepper(
    value: Double,
    step: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    compact: Boolean = false
) {
    val buttonSize = if (compact) 30.dp else 34.dp
    val fieldWidth = if (compact) 44.dp else 56.dp
    // Текст пересоздаётся только когда число реально изменилось (кнопками или вводом).
    var text by remember(value) { mutableStateOf(formatNumber(value)) }

    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        StepButton(false, buttonSize) {
            if (enabled) {
                val n = (value - step).coerceAtLeast(0.0)
                text = formatNumber(n); onValueChange(n)
            }
        }
        Box(
            Modifier
                .width(fieldWidth)
                .height(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(IronColors.Surface2),
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = text,
                onValueChange = { new ->
                    if (!enabled) return@BasicTextField
                    text = new
                    parseNumber(new)?.let { if (it >= 0) onValueChange(it) }
                },
                singleLine = true,
                enabled = enabled,
                textStyle = TextStyle(
                    color = IronColors.TextPrimary, fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center
                ),
                cursorBrush = SolidColor(IronColors.Accent),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            )
        }
        StepButton(true, buttonSize) {
            if (enabled) {
                val n = value + step
                text = formatNumber(n); onValueChange(n)
            }
        }
    }
}

/** Диалог в стиле приложения. */
@Composable
fun IronDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmEnabled: Boolean = true,
    dismissText: String? = "Отмена",
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = confirmEnabled,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = IronColors.Accent,
                    disabledContentColor = IronColors.TextSecondary
                )
            ) { Text(confirmText) }
        },
        dismissButton = {
            if (dismissText != null) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = IronColors.TextSecondary)
                ) { Text(dismissText) }
            }
        },
        title = { Text(title) },
        text = { Column(content = content) },
        containerColor = IronColors.Card,
        titleContentColor = IronColors.TextPrimary,
        textContentColor = IronColors.TextPrimary
    )
}
