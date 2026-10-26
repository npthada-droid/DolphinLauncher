package th.dolphin.launcher.ui.tires

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import th.dolphin.launcher.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import th.dolphin.launcher.alerts.tireStatus
import th.dolphin.launcher.alerts.tireStatusText
import th.dolphin.launcher.settings.AppSettings
import th.dolphin.launcher.ui.components.CarTopView
import th.dolphin.launcher.ui.components.GlassCard
import th.dolphin.launcher.ui.components.ScreenHeader
import th.dolphin.launcher.ui.components.tireColor
import th.dolphin.launcher.util.pressureText
import th.dolphin.launcher.util.pressureUnit
import th.dolphin.launcher.util.tempText
import th.dolphin.launcher.vehicle.Tire
import th.dolphin.launcher.vehicle.VehicleState

@Composable
fun TiresScreen(cfg: AppSettings, st: VehicleState, onBack: () -> Unit) {
    val colors = List(4) { tireColor(tireStatus(st.tire(it), cfg)) }
    val pu = cfg.pressureUnit
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(22.dp)) {
        ScreenHeader("ลมยาง & อุณหภูมิยาง", onBack)
        Row(
            Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TireTile("หน้าซ้าย", st.tire(0), cfg, Modifier.weight(1f).fillMaxWidth())
                TireTile("หลังซ้าย", st.tire(2), cfg, Modifier.weight(1f).fillMaxWidth())
            }
            GlassCard(Modifier.weight(1f).fillMaxHeight()) {
                CarTopView(Modifier.fillMaxSize(), st.doors, st.belts, cfg.rightHandDrive, colors, st.turn)
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TireTile("หน้าขวา", st.tire(1), cfg, Modifier.weight(1f).fillMaxWidth())
                TireTile("หลังขวา", st.tire(3), cfg, Modifier.weight(1f).fillMaxWidth())
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "เกณฑ์ปกติ ${pressureText(cfg.tireLowBar, pu)}–${pressureText(cfg.tireHighBar, pu)} ${pressureUnit(pu)} · " +
                "ยางร้อนเกิน ${tempText(cfg.tireHotC, cfg.tempUnit)} · ปรับได้ในตั้งค่า > หน่วย & ลมยาง",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TireTile(name: String, t: Tire, cfg: AppSettings, modifier: Modifier) {
    val status = tireStatus(t, cfg)
    val color = tireColor(status)
    GlassCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Box(Modifier.clip(CircleShape).background(color.copy(alpha = 0.18f)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                Text(tireStatusText(status), color = color, style = MaterialTheme.typography.labelLarge)
            }
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(pressureText(t.pressureBar, cfg.pressureUnit), fontSize = 52.sp, fontWeight = FontWeight.Bold, lineHeight = 54.sp)
            Text(
                " " + pressureUnit(cfg.pressureUnit), Modifier.padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text("อุณหภูมิ ${tempText(t.tempC, cfg.tempUnit)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        PressureBar(t.pressureBar, cfg, color)
    }
}

/** แถบแสดงตำแหน่งลมยางเทียบช่วงต่ำ–สูง */
@Composable
private fun PressureBar(bar: Float?, cfg: AppSettings, color: Color) {
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val okZone = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    Canvas(Modifier.fillMaxWidth().height(10.dp)) {
        val min = cfg.tireLowBar - 0.6f
        val max = cfg.tireHighBar + 0.6f
        fun x(v: Float) = ((v - min) / (max - min)).coerceIn(0f, 1f) * size.width
        val r = CornerRadius(size.height / 2)
        drawRoundRect(track, Offset.Zero, size, r)
        drawRoundRect(okZone, Offset(x(cfg.tireLowBar), 0f), Size(x(cfg.tireHighBar) - x(cfg.tireLowBar), size.height), r)
        if (bar != null) drawCircle(color, size.height * 0.9f, Offset(x(bar), size.height / 2))
    }
}
