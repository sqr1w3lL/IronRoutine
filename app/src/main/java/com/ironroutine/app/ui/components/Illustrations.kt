package com.ironroutine.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.ironroutine.app.ui.theme.IronColors

/**
 * Иллюстрации техники: схематичный человечек (вид сбоку/спереди), который плавно
 * переходит из начального положения в конечное. Рисуется векторно, внешних картинок нет.
 *
 * Формат позы (координаты 0..120 по X, 0..96 по Y, пол на y=90), элементы через ';':
 *   H x,y            — голова
 *   L x,y x,y ...    — линия тела/конечности (первая L — корпус, её первая точка — шея)
 *   P x,y[,r]        — блин/гантель, вид с торца (круг)
 *   D x,y            — гантель горизонтально
 *   V x,y            — гантель вертикально
 *   G x,y x,y        — неподвижный элемент (пол, турник, скамья)
 * Начальная и конечная позы обязаны иметь одинаковое число элементов каждого типа.
 */
object PoseData {
    private const val FLOOR = "G 5,90 115,90"
    private const val BAR = "G 30,12 90,12"

    val poses: Map<String, Pair<String, String>> = mapOf(
        "db_floor_press" to Pair(
            "H 18,82; L 30,84 58,84; L 30,84 36,87 36,72; L 58,84 74,66 80,88; P 36,70; $FLOOR",
            "H 18,82; L 30,84 58,84; L 30,84 30,70 30,56; L 58,84 74,66 80,88; P 30,54; $FLOOR"
        ),
        "pushup" to Pair(
            "H 20,56; L 28,58 62,72; L 28,58 28,73 28,88; L 62,72 80,79 98,88; $FLOOR",
            "H 20,75; L 28,78 62,82; L 28,78 42,81 28,88; L 62,82 80,85 98,88; $FLOOR"
        ),
        "db_ohp" to Pair(
            "H 50,20; L 50,31 50,56; L 50,31 58,42 54,30; L 50,56 50,73 52,90; P 54,28; $FLOOR",
            "H 50,20; L 50,31 50,56; L 50,31 52,19 52,7; L 50,56 50,73 52,90; P 52,5; $FLOOR"
        ),
        "lateral_raise" to Pair(
            "H 60,18; L 60,30 60,56; L 52,30 68,30; L 60,56 54,73 54,90; L 60,56 66,73 66,90; L 52,30 47,43 45,55; L 68,30 73,43 75,55; P 45,58; P 75,58; $FLOOR",
            "H 60,18; L 60,30 60,56; L 52,30 68,30; L 60,56 54,73 54,90; L 60,56 66,73 66,90; L 52,30 38,29 25,28; L 68,30 82,29 95,28; P 23,28; P 97,28; $FLOOR"
        ),
        "overhead_tri_ext" to Pair(
            "H 48,21; L 50,32 50,57; L 50,32 53,16 44,27; L 50,57 50,74 52,90; P 43,29; $FLOOR",
            "H 48,21; L 50,32 50,57; L 50,32 53,16 53,3; L 50,57 50,74 52,90; P 53,1; $FLOOR"
        ),
        "pullup" to Pair(
            "H 63,27; L 58,33 58,58; L 58,33 60,22 60,12; L 58,58 58,72 62,85; $BAR; $FLOOR",
            "H 63,7; L 60,22 60,47; L 60,22 53,25 60,12; L 60,47 60,61 64,74; $BAR; $FLOOR"
        ),
        "bb_row" to Pair(
            "H 74,45; L 66,48 42,58; L 66,48 68,62 68,76; L 42,58 50,73 52,90; P 68,78,7; $FLOOR",
            "H 74,45; L 66,48 42,58; L 66,48 54,52 58,63; L 42,58 50,73 52,90; P 58,65,7; $FLOOR"
        ),
        "db_row" to Pair(
            "H 43,44; L 50,48 76,54; L 50,48 43,54 36,60; L 76,54 82,73 80,90; L 50,48 52,62 52,76; P 52,78; G 20,60 46,60; G 24,60 24,90; G 42,60 42,90; $FLOOR",
            "H 43,44; L 50,48 76,54; L 50,48 43,54 36,60; L 76,54 82,73 80,90; L 50,48 62,47 56,58; P 56,60; G 20,60 46,60; G 24,60 24,90; G 42,60 42,90; $FLOOR"
        ),
        "db_curl" to Pair(
            "H 50,20; L 50,31 50,56; L 50,31 52,44 53,57; L 50,56 50,73 52,90; P 53,59; $FLOOR",
            "H 50,20; L 50,31 50,56; L 50,31 52,44 60,33; L 50,56 50,73 52,90; P 61,31; $FLOOR"
        ),
        "hammer_curl" to Pair(
            "H 50,20; L 50,31 50,56; L 50,31 52,44 53,57; L 50,56 50,73 52,90; D 53,59; $FLOOR",
            "H 50,20; L 50,31 50,56; L 50,31 52,44 60,33; L 50,56 50,73 52,90; D 61,31; $FLOOR"
        ),
        "wrist_curl" to Pair(
            "H 47,22; L 44,33 40,58; L 40,58 68,58 68,90; L 44,33 50,51 70,53 80,59; P 82,61; G 32,60 64,60; G 35,60 35,90; G 61,60 61,90; $FLOOR",
            "H 47,22; L 44,33 40,58; L 40,58 68,58 68,90; L 44,33 50,51 70,53 80,47; P 82,45; G 32,60 64,60; G 35,60 35,90; G 61,60 61,90; $FLOOR"
        ),
        "goblet_squat" to Pair(
            "H 50,20; L 50,31 50,56; L 50,31 55,43 57,35; L 50,56 50,73 52,90; V 58,38; $FLOOR",
            "H 54,35; L 50,46 38,70; L 50,46 56,57 58,49; L 38,70 62,68 54,90; V 59,50; $FLOOR"
        ),
        "rdl" to Pair(
            "H 50,20; L 50,31 50,56; L 50,31 52,44 53,57; L 50,56 50,73 52,90; P 53,59,7; $FLOOR",
            "H 67,43; L 60,46 36,58; L 60,46 61,60 61,74; L 36,58 42,74 46,90; P 61,76,7; $FLOOR"
        ),
        "rev_lunge" to Pair(
            "H 50,20; L 50,31 50,56; L 50,31 50,44 50,57; L 50,56 50,73 52,90; L 50,56 50,73 52,90; $FLOOR",
            "H 54,26; L 54,37 54,62; L 54,37 54,50 54,63; L 54,62 68,72 68,90; L 54,62 46,82 30,88; $FLOOR"
        ),
        "hanging_knee_raise" to Pair(
            "H 63,27; L 58,33 58,58; L 58,33 60,22 60,12; L 58,58 58,70 60,82; $BAR; $FLOOR",
            "H 63,27; L 58,33 58,58; L 58,33 60,22 60,12; L 58,58 72,52 66,66; $BAR; $FLOOR"
        ),
        "plank" to Pair(
            "H 21,66; L 30,68 64,76; L 30,68 30,86 46,88; L 64,76 80,81 96,88; $FLOOR",
            "H 21,66; L 30,68 64,74; L 30,68 30,86 46,88; L 64,74 80,80 96,88; $FLOOR"
        )
    )
}

private data class GearItem(val kind: Char, val p: Offset, val r: Float)

private class ParsedPose(
    val head: Offset,
    val limbs: List<List<Offset>>,
    val gear: List<GearItem>,
    val scenery: List<Pair<Offset, Offset>>
)

private fun parsePoint(s: String): List<Float> = s.split(',').map { it.trim().toFloat() }

private fun parsePose(src: String): ParsedPose {
    var head = Offset.Zero
    val limbs = ArrayList<List<Offset>>()
    val gear = ArrayList<GearItem>()
    val scenery = ArrayList<Pair<Offset, Offset>>()
    for (token in src.split(';')) {
        val t = token.trim()
        if (t.isEmpty()) continue
        val kind = t[0]
        val parts = t.substring(1).trim().split(Regex("\\s+")).map { parsePoint(it) }
        when (kind) {
            'H' -> head = Offset(parts[0][0], parts[0][1])
            'L' -> limbs += parts.map { Offset(it[0], it[1]) }
            'P', 'D', 'V' -> gear += GearItem(
                kind, Offset(parts[0][0], parts[0][1]), if (parts[0].size > 2) parts[0][2] else 5.5f
            )
            'G' -> scenery += Pair(
                Offset(parts[0][0], parts[0][1]), Offset(parts[1][0], parts[1][1])
            )
        }
    }
    return ParsedPose(head, limbs, gear, scenery)
}

/** Анимированная иллюстрация техники упражнения. */
@Composable
fun ExerciseIllustration(
    exerciseId: String,
    modifier: Modifier = Modifier,
    animated: Boolean = true
) {
    val poses = remember(exerciseId) {
        PoseData.poses[exerciseId]?.let { parsePose(it.first) to parsePose(it.second) }
    }
    val transition = rememberInfiniteTransition(label = "pose")
    val animT = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "poseT"
    )
    val shape = RoundedCornerShape(18.dp)
    if (poses == null) {
        Box(modifier.aspectRatio(1.25f).clip(shape).background(IronColors.Surface2))
        return
    }
    val (a, b) = poses
    val figure = Color(0xFFEDEDED)
    val gearColor = IronColors.Accent
    val sceneryColor = Color(0xFF555555)

    Canvas(modifier.aspectRatio(1.25f).clip(shape).background(IronColors.Surface2)) {
        val t = if (animated) animT.value else 0f
        val k = size.width / 120f
        fun mix(p: Offset, q: Offset): Offset = (p + (q - p) * t) * k

        for ((s, e) in a.scenery) {
            drawLine(sceneryColor, s * k, e * k, strokeWidth = 2.4f * k, cap = StrokeCap.Round)
        }

        val stroke = Stroke(width = 3.6f * k, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val head = mix(a.head, b.head)
        var neck: Offset? = null
        for (i in a.limbs.indices) {
            val la = a.limbs[i]
            val lb = b.limbs.getOrNull(i) ?: la
            val path = Path()
            for (j in la.indices) {
                val pt = mix(la[j], lb.getOrNull(j) ?: la[j])
                if (j == 0) {
                    path.moveTo(pt.x, pt.y)
                    if (i == 0) neck = pt
                } else {
                    path.lineTo(pt.x, pt.y)
                }
            }
            drawPath(path, figure, style = stroke)
        }
        neck?.let { drawLine(figure, head, it, strokeWidth = 3.2f * k, cap = StrokeCap.Round) }
        drawCircle(figure, radius = 6f * k, center = head, style = Stroke(width = 3.2f * k))

        for (i in a.gear.indices) {
            val ga = a.gear[i]
            val gb = b.gear.getOrNull(i) ?: ga
            val c = mix(ga.p, gb.p)
            when (ga.kind) {
                'P' -> {
                    drawCircle(gearColor, radius = ga.r * k, center = c)
                    drawCircle(Color(0xFF101010), radius = 1.6f * k, center = c)
                }
                'D' -> {
                    drawLine(gearColor, c + Offset(-6f * k, 0f), c + Offset(6f * k, 0f), 2.6f * k, StrokeCap.Round)
                    drawLine(gearColor, c + Offset(-6f * k, -4.5f * k), c + Offset(-6f * k, 4.5f * k), 3.6f * k, StrokeCap.Round)
                    drawLine(gearColor, c + Offset(6f * k, -4.5f * k), c + Offset(6f * k, 4.5f * k), 3.6f * k, StrokeCap.Round)
                }
                'V' -> {
                    drawLine(gearColor, c + Offset(0f, -6f * k), c + Offset(0f, 6f * k), 2.6f * k, StrokeCap.Round)
                    drawLine(gearColor, c + Offset(-4.5f * k, -6f * k), c + Offset(4.5f * k, -6f * k), 3.6f * k, StrokeCap.Round)
                    drawLine(gearColor, c + Offset(-4.5f * k, 6f * k), c + Offset(4.5f * k, 6f * k), 3.6f * k, StrokeCap.Round)
                }
            }
        }
    }
}
