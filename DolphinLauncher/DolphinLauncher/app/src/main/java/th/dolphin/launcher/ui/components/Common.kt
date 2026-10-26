package th.dolphin.launcher.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import th.dolphin.launcher.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import th.dolphin.launcher.settings.AppSettings
import th.dolphin.launcher.ui.theme.LocalUi

/** การ์ดกระจกโปร่งแสง — ความทึบ/ความโค้งมาจากตั้งค่า */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val ui = LocalUi.current
    val shape = RoundedCornerShape(ui.corner)
    val c = MaterialTheme.colorScheme
    val edge = Brush.linearGradient(
        listOf(Color.White.copy(alpha = if (ui.dark) 0.16f else 0.7f), c.outline, ui.accent.copy(alpha = 0.22f))
    )
    Column(
        modifier
            .clip(shape)
            .background(c.surface.copy(alpha = ui.opacity))
            .border(1.dp, edge, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

@Composable
fun RoundIconButton(
    icon: ImageVector,
    desc: String,
    onClick: () -> Unit,
    size: Dp = 52.dp,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    val ui = LocalUi.current
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = ui.opacity))
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, desc, tint = tint, modifier = Modifier.size(size * 0.46f))
    }
}

@Composable
fun ScreenHeader(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "กลับ", onBack)
            Spacer(Modifier.width(16.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        actions()
    }
}

@Composable
fun Pill(text: String, icon: ImageVector? = null, dot: Color? = null) {
    val ui = LocalUi.current
    Row(
        Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = ui.opacity))
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(8.dp))
        }
        if (icon != null) {
            Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
fun rememberImageFile(path: String, maxDim: Int = 1600): ImageBitmap? {
    val state = produceState<ImageBitmap?>(null, path) {
        value = if (path.isBlank()) null else withContext(Dispatchers.IO) { decodeSampled(path, maxDim)?.asImageBitmap() }
    }
    return state.value
}

private fun decodeSampled(path: String, maxDim: Int): Bitmap? = runCatching {
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, o)
    var s = 1
    while (o.outWidth / s > maxDim || o.outHeight / s > maxDim) s *= 2
    BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = s })
}.getOrNull()

/** พื้นหลังสไตล์โชว์รูม: แสงเรืองสองสี + พื้นกริดมีมิติ หรือวอลเปเปอร์ของผู้ใช้ */
@Composable
fun LauncherBackground(cfg: AppSettings) {
    val c = MaterialTheme.colorScheme
    val ui = LocalUi.current
    val wp = rememberImageFile(cfg.wallpaperPath, 2400)
    Box(Modifier.fillMaxSize().background(c.background)) {
        if (wp != null) {
            Image(wp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(c.background.copy(alpha = cfg.wallpaperDim)))
        }
        Canvas(Modifier.fillMaxSize()) {
            val k = if (ui.dark) 1f else 0.7f
            drawRect(
                Brush.radialGradient(
                    listOf(ui.accent.copy(alpha = 0.26f * k), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.62f), radius = size.maxDimension * 0.42f,
                )
            )
            drawRect(
                Brush.radialGradient(
                    listOf(ui.accent2.copy(alpha = 0.22f * k), Color.Transparent),
                    center = Offset(0f, 0f), radius = size.maxDimension * 0.45f,
                )
            )
            drawRect(
                Brush.radialGradient(
                    listOf(ui.accent.copy(alpha = 0.14f * k), Color.Transparent),
                    center = Offset(size.width, size.height), radius = size.maxDimension * 0.4f,
                )
            )
            if (ui.showGrid && wp == null) {
                // พื้นกริดแบบ perspective: เส้นแนวนอนถี่ขึ้นเมื่อไกล เส้นแนวตั้งลู่เข้าจุดหาย
                val horizon = size.height * 0.58f
                val vx = size.width / 2f
                val line = if (ui.dark) Color.White else Color(0xFF0A1428)
                for (i in 1..14) {
                    val t = i / 14f
                    val y = horizon + (size.height - horizon) * t * t
                    drawLine(line.copy(alpha = 0.07f * t), Offset(0f, y), Offset(size.width, y), 1f)
                }
                for (i in -16..16) {
                    val xb = vx + i * size.width * 0.09f
                    drawLine(line.copy(alpha = 0.05f), Offset(vx + i * size.width * 0.012f, horizon), Offset(xb, size.height), 1f)
                }
                drawRect(
                    Brush.verticalGradient(listOf(c.background, Color.Transparent), startY = horizon, endY = horizon + size.height * 0.18f),
                    topLeft = Offset(0f, horizon), size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.18f),
                )
            }
        }
    }
}

/** แสงเรืองด้านหลังองค์ประกอบ (วาดก่อนเนื้อหา) */
fun Modifier.glow(color: Color, radiusFraction: Float = 0.7f): Modifier = this.drawBehind {
    drawRect(
        Brush.radialGradient(
            listOf(color.copy(alpha = 0.35f), Color.Transparent),
            center = center, radius = size.minDimension * radiusFraction,
        )
    )
}
