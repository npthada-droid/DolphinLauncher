package th.dolphin.launcher.ui.trip

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EvStation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import th.dolphin.launcher.i18n.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import th.dolphin.launcher.alerts.Maintenance
import th.dolphin.launcher.app
import th.dolphin.launcher.settings.AppSettings
import th.dolphin.launcher.trip.TripStats
import th.dolphin.launcher.ui.components.GlassCard
import th.dolphin.launcher.ui.components.ScreenHeader
import th.dolphin.launcher.ui.theme.StatusColors
import th.dolphin.launcher.util.speedUnit
import th.dolphin.launcher.util.speedValue
import th.dolphin.launcher.vehicle.VehicleState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val L = Locale.US

@Composable
fun TripScreen(cfg: AppSettings, st: VehicleState, onBack: () -> Unit) {
    val app = LocalContext.current.app
    val snap by app.trip.snapshot.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(22.dp)) {
        ScreenHeader("ทริปและการชาร์จ", onBack)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val wide = maxWidth > maxHeight
            if (wide) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(Modifier.weight(1.2f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TripCard("ทริป A", snap.tripA, cfg, Modifier.weight(1f).fillMaxWidth()) { app.trip.resetTripA() }
                        TripCard("ตั้งแต่ชาร์จล่าสุด", snap.sinceCharge, cfg, Modifier.weight(1f).fillMaxWidth(), null)
                        TripCard("วันนี้", snap.today, cfg, Modifier.weight(1f).fillMaxWidth(), null)
                    }
                    ChargePanel(cfg, st, Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TripCard("ทริป A", snap.tripA, cfg, Modifier.fillMaxWidth().height(140.dp)) { app.trip.resetTripA() }
                    TripCard("ตั้งแต่ชาร์จล่าสุด", snap.sinceCharge, cfg, Modifier.fillMaxWidth().height(140.dp), null)
                    ChargePanel(cfg, st, Modifier.weight(1f).fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun TripCard(title: String, t: TripStats, cfg: AppSettings, modifier: Modifier, onReset: (() -> Unit)?) {
    val fmt = remember { SimpleDateFormat("d MMM HH:mm", th.dolphin.launcher.i18n.I18n.dateLocale()) }
    val mph = cfg.speedUnit == "mph"
    val dist = if (mph) t.distanceKm * 0.621371 else t.distanceKm
    GlassCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text("เริ่ม ${fmt.format(Date(t.startedAt))}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onReset != null) TextButton(onReset) { Text("รีเซ็ต") }
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Stat("ระยะทาง", String.format(L, "%.1f", dist), if (mph) "mi" else "km")
            Stat("เวลาขับ", durationText(t.drivingMs), "")
            Stat("เฉลี่ย", "${speedValue(t.avgSpeedKmh.toFloat(), cfg.speedUnit)}", speedUnit(cfg.speedUnit))
            Stat("ใช้ไฟ", t.consumption?.let { String.format(L, "%.1f", it) } ?: "--", "kWh/100km")
            Stat("พลังงาน", String.format(L, "%.1f", t.energyKwh), "kWh")
        }
    }
}

@Composable
private fun Stat(label: String, value: String, unit: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            if (unit.isNotEmpty()) Text(" $unit", Modifier.padding(bottom = 3.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun durationText(ms: Long): String {
    val m = ms / 60_000
    return if (m >= 60) "${m / 60}ชม. ${m % 60}น." else "${m}น."
}

@Composable
private fun ChargePanel(cfg: AppSettings, st: VehicleState, modifier: Modifier) {
    val app = LocalContext.current.app
    val snap by app.trip.snapshot.collectAsStateWithLifecycle()
    val fmt = remember { SimpleDateFormat("d MMM HH:mm", th.dolphin.launcher.i18n.I18n.dateLocale()) }
    val month = remember(snap.history) {
        val c = Calendar.getInstance()
        snap.history.filter {
            val h = Calendar.getInstance().apply { timeInMillis = it.startAt }
            h.get(Calendar.YEAR) == c.get(Calendar.YEAR) && h.get(Calendar.MONTH) == c.get(Calendar.MONTH)
        }
    }
    val service = Maintenance.status(st, cfg)
    GlassCard(modifier) {
        Text("การชาร์จ", style = MaterialTheme.typography.titleMedium)
        snap.charging?.let { c ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(CircleShape)
                    .background(StatusColors.ok.copy(alpha = 0.14f)).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.EvStation, null, tint = StatusColors.ok)
                Spacer(Modifier.width(8.dp))
                Text(
                    String.format(L, "กำลังชาร์จ %.0f%% → %.0f%% · %.1f kWh · ฿%.0f", c.socStart, c.socEnd, c.kwh, c.cost),
                    color = StatusColors.ok,
                )
            }
        }
        Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Stat("เดือนนี้", "${month.size}", "ครั้ง")
            Stat("ไฟ", String.format(L, "%.0f", month.sumOf { it.kwh }), "kWh")
            Stat("ค่าไฟ", String.format(L, "%.0f", month.sumOf { it.cost }), "บาท")
        }
        Text(
            String.format(L, "คิดที่ %.2f บาท/kWh · ความจุแบต %.1f kWh (ปรับในตั้งค่า)", cfg.elecPrice, cfg.batteryKwh),
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (service != null) {
            Text(
                "เข้าศูนย์ครั้งถัดไป: ${service.text}",
                Modifier.padding(top = 4.dp),
                color = if (service.due) StatusColors.warn else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Spacer(Modifier.height(6.dp))
        if (snap.history.isEmpty()) {
            Text(
                "ยังไม่มีประวัติ — แอปจะบันทึกเองเมื่อรถชาร์จ (หรือ SOC เพิ่มขึ้นขณะจอด)",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            items(snap.history, key = { it.id }) { h ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.EvStation, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(String.format(L, "%.0f%% → %.0f%% · %.1f kWh", h.socStart, h.socEnd, h.kwh))
                        Text(
                            fmt.format(Date(h.startAt)) + " · " + durationText(h.endAt - h.startAt),
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(String.format(L, "฿%.0f", h.cost), fontWeight = FontWeight.SemiBold)
                    IconButton({ app.trip.deleteCharge(h.id) }) { Icon(Icons.Rounded.Delete, "ลบ", modifier = Modifier.size(18.dp)) }
                }
            }
        }
    }
}
