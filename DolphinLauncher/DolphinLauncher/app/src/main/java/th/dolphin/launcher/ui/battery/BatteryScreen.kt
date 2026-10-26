package th.dolphin.launcher.ui.battery

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import th.dolphin.launcher.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import th.dolphin.launcher.settings.AppSettings
import th.dolphin.launcher.ui.components.GlassCard
import th.dolphin.launcher.ui.components.ScreenHeader
import th.dolphin.launcher.ui.theme.LocalUi
import th.dolphin.launcher.ui.theme.StatusColors
import th.dolphin.launcher.util.distText
import th.dolphin.launcher.util.tempText
import th.dolphin.launcher.vehicle.VehicleState
import java.util.Locale
import kotlin.math.roundToInt

/** รายละเอียดแบตเตอรี่ขับเคลื่อน (แบตใหญ่) + แบต 12V */
@Composable
fun BatteryScreen(cfg: AppSettings, st: VehicleState, onBack: () -> Unit) {
    val power = if (st.hvVoltage != null && st.hvCurrentA != null) st.hvVoltage * st.hvCurrentA / 1000f else st.powerKw
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(22.dp)) {
        ScreenHeader("แบตเตอรี่", onBack)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val wide = maxWidth > maxHeight
            if (wide) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    HealthPanel(cfg, st, Modifier.weight(1f).fillMaxHeight())
                    Column(Modifier.weight(1.3f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Tiles(cfg, st, power, Modifier.weight(1f).fillMaxWidth())
                    }
                }
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    HealthPanel(cfg, st, Modifier.fillMaxWidth().height(320.dp))
                    Tiles(cfg, st, power, Modifier.fillMaxWidth().height(360.dp))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "ค่าอ่านจาก BYD API ตาม mapping ในตั้งค่า > ข้อมูลรถ · ช่องที่ขึ้น -- คือรถคันนี้ยังอ่านค่านั้นไม่ได้ " +
                "(ตรวจด้วยเมนูสแกน BYD API) · SOH เป็นค่าประมาณจากระบบจัดการแบตเตอรี่ของรถ",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HealthPanel(cfg: AppSettings, st: VehicleState, modifier: Modifier) {
    val ui = LocalUi.current
    val soh = st.sohPercent
    val sohColor = when {
        soh == null -> MaterialTheme.colorScheme.onSurfaceVariant
        soh >= 90f -> StatusColors.ok
        soh >= 80f -> StatusColors.warn
        else -> StatusColors.bad
    }
    GlassCard(modifier, padding = 22.dp) {
        Text("SOH · สุขภาพแบตใหญ่", style = MaterialTheme.typography.labelLarge, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                soh?.let { String.format(Locale.US, "%.1f", it) } ?: "--",
                style = TextStyle(fontSize = 64.sp, fontWeight = FontWeight.SemiBold, shadow = Shadow(sohColor.copy(alpha = 0.6f), blurRadius = 30f)),
                color = sohColor,
            )
            Text("%", Modifier.padding(start = 4.dp, bottom = 14.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            when {
                soh == null -> "ยังอ่านค่า SOH ไม่ได้"
                soh >= 90f -> "สภาพดีมาก"
                soh >= 80f -> "สภาพดี เสื่อมตามการใช้งาน"
                else -> "ควรให้ศูนย์บริการตรวจสอบ"
            },
            color = sohColor, style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.weight(1f))
        Text("SOC · ไฟคงเหลือ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val soc = (st.socPercent ?: 0f).coerceIn(0f, 100f)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(st.socPercent?.let { "${it.roundToInt()}%" } ?: "--", fontSize = 28.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.padding(6.dp))
            Text(distText(st.rangeKm, cfg.speedUnit), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        // แท่งแบต: SOC เทียบความจุที่เหลือจริง (SOH)
        Canvas(Modifier.fillMaxWidth().height(26.dp)) {
            val r = CornerRadius(8.dp.toPx())
            val cap = (soh ?: 100f) / 100f
            drawRoundRect(Color.White.copy(alpha = if (ui.dark) 0.06f else 0.5f), cornerRadius = r)
            drawRoundRect(Color.Gray.copy(alpha = 0.25f), size = Size(size.width * cap, size.height), cornerRadius = r)
            drawRoundRect(
                Brush.horizontalGradient(listOf(ui.accent2, ui.accent)),
                size = Size(size.width * cap * soc / 100f, size.height), cornerRadius = r,
            )
        }
        Text(
            "แถบเทา = ความจุที่เหลือจริงตาม SOH",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Tiles(cfg: AppSettings, st: VehicleState, power: Float?, modifier: Modifier) {
    val f1 = { v: Float? -> v?.let { String.format(Locale.US, "%.1f", it) } ?: "--" }
    val f0 = { v: Float? -> v?.let { String.format(Locale.US, "%.0f", it) } ?: "--" }
    val hvTempColor = when {
        st.hvTempC == null -> null
        st.hvTempC >= 50f -> StatusColors.bad
        st.hvTempC >= 42f -> StatusColors.warn
        else -> null
    }
    val lvColor = st.lvVoltage?.let { if (it < 12.0f) StatusColors.bad else if (it < 12.4f) StatusColors.warn else null }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Tile("อุณหภูมิแบต", tempText(st.hvTempC, cfg.tempUnit), "", hvTempColor, Modifier.weight(1f).fillMaxHeight())
            Tile("แรงดันแบต", f0(st.hvVoltage), "V", null, Modifier.weight(1f).fillMaxHeight())
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Tile("กระแส", f0(st.hvCurrentA), "A", null, Modifier.weight(1f).fillMaxHeight())
            Tile("กำลัง", f1(power), "kW", null, Modifier.weight(1f).fillMaxHeight())
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Tile("แบต 12V", f1(st.lvVoltage), "V", lvColor, Modifier.weight(1f).fillMaxHeight())
            Tile(
                "สถานะชาร์จ",
                when (st.charging) { true -> "กำลังชาร์จ"; false -> "ไม่ได้ชาร์จ"; null -> "--" },
                "", if (st.charging == true) StatusColors.ok else null, Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun Tile(label: String, value: String, unit: String, color: Color?, modifier: Modifier) {
    GlassCard(modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, color = color ?: MaterialTheme.colorScheme.onSurface)
            if (unit.isNotEmpty()) Text(" $unit", Modifier.padding(bottom = 5.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
