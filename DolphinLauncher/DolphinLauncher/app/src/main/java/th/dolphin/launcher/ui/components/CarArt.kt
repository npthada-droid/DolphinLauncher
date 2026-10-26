package th.dolphin.launcher.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import th.dolphin.launcher.alerts.TireStatus
import th.dolphin.launcher.ui.theme.LocalUi
import th.dolphin.launcher.ui.theme.StatusColors
import th.dolphin.launcher.vehicle.Belts
import th.dolphin.launcher.vehicle.Doors
import th.dolphin.launcher.vehicle.TurnSignal
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

fun tireColor(s: TireStatus): Color = when (s) {
    TireStatus.OK -> StatusColors.ok
    TireStatus.LOW, TireStatus.HIGH -> StatusColors.warn
    TireStatus.HOT -> StatusColors.bad
    TireStatus.UNKNOWN -> Color(0xFF6B7280)
}

/** รถแฮทช์แบ็กด้านข้างแบบกราฟิก — แสดงเมื่อผู้ใช้ยังไม่ได้ใส่รูปรถของตัวเอง */
@Composable
fun GenericCarSide(modifier: Modifier = Modifier, body: Color = MaterialTheme.colorScheme.primary) {
    val dark = LocalUi.current.dark
    Canvas(modifier.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
        val w = size.width
        val h = size.height
        fun p(x: Float, y: Float) = Offset(x * w, y * h)

        drawOval(
            Color.Black.copy(alpha = if (dark) 0.5f else 0.18f),
            topLeft = p(0.05f, 0.72f), size = Size(w * 0.90f, h * 0.08f)
        )
        val bodyPath = Path().apply {
            moveTo(0.03f * w, 0.69f * h)
            lineTo(0.03f * w, 0.57f * h)
            cubicTo(0.035f * w, 0.50f * h, 0.09f * w, 0.47f * h, 0.19f * w, 0.455f * h)
            lineTo(0.33f * w, 0.43f * h)
            cubicTo(0.40f * w, 0.30f * h, 0.47f * w, 0.235f * h, 0.58f * w, 0.225f * h)
            lineTo(0.80f * w, 0.225f * h)
            cubicTo(0.90f * w, 0.235f * h, 0.955f * w, 0.31f * h, 0.97f * w, 0.43f * h)
            lineTo(0.975f * w, 0.62f * h)
            cubicTo(0.975f * w, 0.67f * h, 0.96f * w, 0.69f * h, 0.93f * w, 0.69f * h)
            close()
        }
        drawPath(
            bodyPath,
            Brush.verticalGradient(
                listOf(body, body.copy(alpha = 0.55f)), startY = 0.2f * h, endY = 0.7f * h
            )
        )
        drawLine(Color.White.copy(alpha = 0.35f), p(0.08f, 0.52f), p(0.95f, 0.50f), strokeWidth = h * 0.006f)

        val glass = Path().apply {
            moveTo(0.355f * w, 0.425f * h)
            cubicTo(0.42f * w, 0.31f * h, 0.48f * w, 0.26f * h, 0.585f * w, 0.255f * h)
            lineTo(0.79f * w, 0.255f * h)
            cubicTo(0.875f * w, 0.262f * h, 0.915f * w, 0.32f * h, 0.93f * w, 0.415f * h)
            close()
        }
        drawPath(
            glass,
            Brush.verticalGradient(listOf(Color(0xFF0B1220), Color(0xFF2A3546)), startY = 0.25f * h, endY = 0.43f * h)
        )
        drawLine(body, p(0.62f, 0.255f), p(0.62f, 0.425f), strokeWidth = w * 0.008f)
        drawLine(Color(0xFFE6F7FF), p(0.04f, 0.53f), p(0.12f, 0.50f), strokeWidth = h * 0.018f, cap = StrokeCap.Round)
        drawLine(Color(0xFFFF4D5E), p(0.94f, 0.47f), p(0.972f, 0.47f), strokeWidth = h * 0.03f, cap = StrokeCap.Round)

        val r = min(w * 0.085f, h * 0.17f)
        listOf(0.22f, 0.80f).forEach { x ->
            val c = p(x, 0.69f)
            drawCircle(Color.Transparent, r * 1.18f, c, blendMode = BlendMode.Clear)
            drawCircle(Color(0xFF111317), r, c)
            drawCircle(Color(0xFF9AA4B2), r * 0.62f, c)
            for (k in 0 until 5) {
                val a = (k * 72.0 + 18.0) * PI / 180.0
                drawLine(
                    Color(0xFF2A2F37), c,
                    Offset(c.x + (cos(a) * r * 0.6f).toFloat(), c.y + (sin(a) * r * 0.6f).toFloat()),
                    strokeWidth = r * 0.12f
                )
            }
            drawCircle(Color(0xFF2A2F37), r * 0.2f, c)
        }
    }
}

/**
 * มุมมองรถจากด้านบน (หน้ารถอยู่ด้านบน) แสดง:
 * สีของยางตามสถานะลม, ประตู/ฝาท้าย/ฝากระโปรงที่เปิด, ที่นั่งที่ยังไม่คาดเข็มขัด และไฟเลี้ยว
 */
@Composable
fun CarTopView(
    modifier: Modifier,
    doors: Doors,
    belts: Belts,
    rightHandDrive: Boolean,
    tireColors: List<Color>,
    turn: TurnSignal = TurnSignal.NONE,
) {
    val body = MaterialTheme.colorScheme.primary
    val neutral = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    val blink = rememberInfiniteTransition(label = "turn")
    val turnAlpha by blink.animateFloat(1f, 0.1f, infiniteRepeatable(tween(420), RepeatMode.Reverse), label = "a")

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val bw = min(w * 0.58f, h * 0.44f)
        val bh = min(h * 0.92f, bw * 2.2f)
        val left = (w - bw) / 2f
        val top = (h - bh) / 2f
        val right = left + bw
        val bottom = top + bh

        // ยาง: 0=FL 1=FR 2=RL 3=RR
        val tw = bw * 0.17f
        val th = bh * 0.17f
        val fy = top + bh * 0.12f
        val ry = top + bh * 0.70f
        val wheels = listOf(
            Rect(left - tw * 0.55f, fy, left + tw * 0.45f, fy + th),
            Rect(right - tw * 0.45f, fy, right + tw * 0.55f, fy + th),
            Rect(left - tw * 0.55f, ry, left + tw * 0.45f, ry + th),
            Rect(right - tw * 0.45f, ry, right + tw * 0.55f, ry + th),
        )
        wheels.forEachIndexed { i, r ->
            drawRoundRect(tireColors.getOrElse(i) { neutral }, r.topLeft, r.size, CornerRadius(tw * 0.3f))
        }

        // ตัวถัง
        drawRoundRect(
            Brush.verticalGradient(listOf(body, body.copy(alpha = 0.6f)), startY = top, endY = bottom),
            Offset(left, top), Size(bw, bh), CornerRadius(bw * 0.32f)
        )
        // กระจก + หลังคา
        drawRoundRect(
            Color(0xFF0B1220).copy(alpha = 0.88f),
            Offset(left + bw * 0.12f, top + bh * 0.28f), Size(bw * 0.76f, bh * 0.52f), CornerRadius(bw * 0.18f)
        )
        drawRoundRect(
            body.copy(alpha = 0.85f),
            Offset(left + bw * 0.17f, top + bh * 0.40f), Size(bw * 0.66f, bh * 0.30f), CornerRadius(bw * 0.12f)
        )
        // ไฟหน้า/ไฟท้าย
        drawLine(Color(0xFFE6F7FF), Offset(left + bw * 0.12f, top + bh * 0.03f), Offset(left + bw * 0.32f, top + bh * 0.025f), bw * 0.035f, StrokeCap.Round)
        drawLine(Color(0xFFE6F7FF), Offset(right - bw * 0.32f, top + bh * 0.025f), Offset(right - bw * 0.12f, top + bh * 0.03f), bw * 0.035f, StrokeCap.Round)
        drawLine(Color(0xFFFF4D5E), Offset(left + bw * 0.10f, bottom - bh * 0.02f), Offset(right - bw * 0.10f, bottom - bh * 0.02f), bw * 0.03f, StrokeCap.Round)

        // ที่นั่งด้านหน้า (สีแดง = ไม่คาดเข็มขัด)
        val seatR = bw * 0.085f
        val seatY = top + bh * 0.47f
        val leftSeat = Offset(left + bw * 0.32f, seatY)
        val rightSeat = Offset(right - bw * 0.32f, seatY)
        val driverSeat = if (rightHandDrive) rightSeat else leftSeat
        val passSeat = if (rightHandDrive) leftSeat else rightSeat
        drawCircle(if (belts.driver == false) StatusColors.bad else StatusColors.ok.copy(alpha = 0.8f), seatR, driverSeat)
        if (belts.passengerOccupied) {
            drawCircle(if (belts.passenger == false) StatusColors.bad else StatusColors.ok.copy(alpha = 0.8f), seatR, passSeat)
        } else {
            drawCircle(Color.White.copy(alpha = 0.18f), seatR, passSeat)
        }

        // ประตูที่เปิด
        val doorStroke = bw * 0.06f
        val fHinge = top + bh * 0.33f
        val rHinge = top + bh * 0.52f
        val doorLen = bh * 0.17f
        fun openDoor(x: Float, y: Float, dir: Float) {
            drawLine(StatusColors.bad, Offset(x, y), Offset(x + dir * doorLen * 0.75f, y + doorLen * 0.62f), doorStroke, StrokeCap.Round)
        }
        if (doors.fl) openDoor(left, fHinge, -1f)
        if (doors.fr) openDoor(right, fHinge, 1f)
        if (doors.rl) openDoor(left, rHinge, -1f)
        if (doors.rr) openDoor(right, rHinge, 1f)
        if (doors.hood) drawLine(StatusColors.bad, Offset(left + bw * 0.2f, top - bh * 0.02f), Offset(right - bw * 0.2f, top - bh * 0.02f), doorStroke, StrokeCap.Round)
        if (doors.trunk) drawLine(StatusColors.bad, Offset(left + bw * 0.2f, bottom + bh * 0.03f), Offset(right - bw * 0.2f, bottom + bh * 0.03f), doorStroke, StrokeCap.Round)

        // ไฟเลี้ยว
        val amber = StatusColors.warn.copy(alpha = turnAlpha)
        val lr = bw * 0.07f
        if (turn == TurnSignal.LEFT || turn == TurnSignal.HAZARD) {
            drawCircle(amber, lr, Offset(left + bw * 0.06f, top + bh * 0.06f))
            drawCircle(amber, lr, Offset(left + bw * 0.06f, bottom - bh * 0.06f))
        }
        if (turn == TurnSignal.RIGHT || turn == TurnSignal.HAZARD) {
            drawCircle(amber, lr, Offset(right - bw * 0.06f, top + bh * 0.06f))
            drawCircle(amber, lr, Offset(right - bw * 0.06f, bottom - bh * 0.06f))
        }
    }
}

/** ลูกศรไฟเลี้ยวซ้าย/ขวา กระพริบเมื่อเปิด */
@Composable
fun TurnArrows(turn: TurnSignal, size: Dp = 34.dp, onlyLeft: Boolean = false, onlyRight: Boolean = false) {
    val tr = rememberInfiniteTransition(label = "blink")
    val a by tr.animateFloat(1f, 0.15f, infiniteRepeatable(tween(420), RepeatMode.Reverse), label = "a")
    val idle = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    val left = turn == TurnSignal.LEFT || turn == TurnSignal.HAZARD
    val right = turn == TurnSignal.RIGHT || turn == TurnSignal.HAZARD
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!onlyRight) Arrow(true, if (left) StatusColors.warn.copy(alpha = a) else idle, size)
        if (!onlyLeft) Arrow(false, if (right) StatusColors.warn.copy(alpha = a) else idle, size)
    }
}

@Composable
private fun Arrow(pointLeft: Boolean, color: Color, dim: Dp) {
    Canvas(Modifier.size(dim)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            if (pointLeft) {
                moveTo(0f, h / 2); lineTo(w * 0.55f, 0f); lineTo(w * 0.55f, h * 0.3f); lineTo(w, h * 0.3f)
                lineTo(w, h * 0.7f); lineTo(w * 0.55f, h * 0.7f); lineTo(w * 0.55f, h); close()
            } else {
                moveTo(w, h / 2); lineTo(w * 0.45f, 0f); lineTo(w * 0.45f, h * 0.3f); lineTo(0f, h * 0.3f)
                lineTo(0f, h * 0.7f); lineTo(w * 0.45f, h * 0.7f); lineTo(w * 0.45f, h); close()
            }
        }
        drawPath(path, color)
    }
}
