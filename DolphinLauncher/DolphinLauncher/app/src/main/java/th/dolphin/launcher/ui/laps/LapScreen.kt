package th.dolphin.launcher.ui.laps

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import th.dolphin.launcher.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import th.dolphin.launcher.app
import th.dolphin.launcher.laps.LapSession
import th.dolphin.launcher.laps.Stopwatch
import th.dolphin.launcher.settings.AppSettings
import th.dolphin.launcher.ui.components.GlassCard
import th.dolphin.launcher.ui.components.ScreenHeader
import th.dolphin.launcher.ui.theme.StatusColors
import th.dolphin.launcher.util.lapText
import th.dolphin.launcher.util.speedUnit
import th.dolphin.launcher.util.speedValue
import th.dolphin.launcher.util.toast
import th.dolphin.launcher.vehicle.VehicleState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

@Composable
fun LapScreen(cfg: AppSettings, st: VehicleState, onBack: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(22.dp)) {
        ScreenHeader("บันทึกรอบ & จับเวลา", onBack)
        SegTabs(listOf("จับเวลารอบ", "อัตราเร่ง 0-${cfg.accelTarget}", "ประวัติ"), tab) { tab = it }
        Spacer(Modifier.height(16.dp))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                0 -> LapMode(cfg, st)
                1 -> AccelMode(cfg, st)
                else -> HistoryPane(cfg)
            }
        }
    }
}

@Composable
private fun SegTabs(items: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)).padding(4.dp)) {
        items.forEachIndexed { i, s ->
            val on = i == selected
            Box(
                Modifier.clip(CircleShape)
                    .background(if (on) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(
                    s, fontWeight = FontWeight.SemiBold,
                    color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ---------------- จับเวลารอบ ----------------

@Composable
private fun LapMode(cfg: AppSettings, st: VehicleState) {
    val ctx = LocalContext.current
    val app = ctx.app
    val sw = app.stopwatch
    val now by produceState(SystemClock.elapsedRealtime(), sw.running) {
        value = SystemClock.elapsedRealtime()
        while (sw.running) {
            withFrameMillis { }
            value = SystemClock.elapsedRealtime()
        }
    }
    LaunchedEffect(st.speedKmh) {
        if (sw.running && st.speedKmh > sw.maxSpeed) sw.maxSpeed = st.speedKmh
    }
    val onSave = {
        val t = SystemClock.elapsedRealtime()
        val laps = sw.laps.toMutableList()
        val rest = sw.currentLap(t)
        if (laps.isEmpty() || rest > 500) laps.add(rest)
        val id = System.currentTimeMillis()
        app.laps.add(LapSession(id, "lap", id, laps, sw.maxSpeed, "จับเวลารอบ"))
        sw.reset()
        ctx.toast("บันทึกแล้ว ${laps.size} รอบ")
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth > maxHeight) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TimerPanel(sw, now, cfg, Modifier.weight(1.2f).fillMaxHeight(), onSave)
                LapList(sw, Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TimerPanel(sw, now, cfg, Modifier.fillMaxWidth(), onSave)
                LapList(sw, Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}

@Composable
private fun TimerPanel(sw: Stopwatch, now: Long, cfg: AppSettings, modifier: Modifier, onSave: () -> Unit) {
    val best = sw.laps.minOrNull()
    GlassCard(modifier, padding = 24.dp) {
        Text("เวลารวม", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(lapText(sw.total(now)), fontSize = 76.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            Metric("รอบปัจจุบัน", lapText(sw.currentLap(now)))
            Metric("รอบดีที่สุด", best?.let { lapText(it) } ?: "--")
            Metric("เร็วสุด", "${speedValue(sw.maxSpeed, cfg.speedUnit)} ${speedUnit(cfg.speedUnit)}")
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigButton(
                if (sw.running) "หยุด" else "เริ่ม",
                if (sw.running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, primary = true
            ) {
                val t = SystemClock.elapsedRealtime()
                if (sw.running) sw.pause(t) else sw.start(t)
            }
            BigButton("Lap", Icons.Rounded.Flag, enabled = sw.running) { sw.lap(SystemClock.elapsedRealtime()) }
            BigButton("รีเซ็ต", Icons.Rounded.Refresh, enabled = !sw.running) { sw.reset() }
            BigButton("บันทึก", Icons.Rounded.Save, enabled = !sw.running && sw.accumulated > 0) { onSave() }
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BigButton(
    text: String,
    icon: ImageVector,
    primary: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val bg = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val fg = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Column(
        Modifier.alpha(if (enabled) 1f else 0.35f)
            .clip(RoundedCornerShape(22.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(30.dp))
        Text(text, color = fg, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LapList(sw: Stopwatch, modifier: Modifier) {
    val laps = sw.laps.toList()
    val best = laps.minOrNull()
    GlassCard(modifier) {
        Text("รอบ (${laps.size})", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.fillMaxSize()) {
            itemsIndexed(laps.reversed()) { i, t ->
                val n = laps.size - i
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("รอบ $n", Modifier.width(80.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(lapText(t), Modifier.weight(1f), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleMedium)
                    if (t == best) {
                        Text("BEST", color = StatusColors.ok, fontWeight = FontWeight.Bold)
                    } else if (best != null) {
                        Text("+${lapText(t - best)}", color = StatusColors.warn, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

// ---------------- อัตราเร่ง ----------------

private enum class AccelPhase { WAIT, READY, RUNNING, DONE }

@Composable
private fun AccelMode(cfg: AppSettings, st: VehicleState) {
    val app = LocalContext.current.app
    var phase by remember { mutableStateOf(AccelPhase.WAIT) }
    var t0 by remember { mutableLongStateOf(0L) }
    var result by remember { mutableStateOf<Long?>(null) }
    var peak by remember { mutableFloatStateOf(0f) }
    val now by produceState(SystemClock.elapsedRealtime(), phase) {
        value = SystemClock.elapsedRealtime()
        while (phase == AccelPhase.RUNNING) {
            withFrameMillis { }
            value = SystemClock.elapsedRealtime()
        }
    }

    LaunchedEffect(st.speedKmh, phase) {
        val v = st.speedKmh
        when (phase) {
            AccelPhase.WAIT -> {
                if (v < 0.5f) phase = AccelPhase.READY
            }
            AccelPhase.READY -> {
                if (v >= 0.5f) {
                    t0 = SystemClock.elapsedRealtime(); peak = v; phase = AccelPhase.RUNNING
                }
            }
            AccelPhase.RUNNING -> {
                peak = max(peak, v)
                if (v >= cfg.accelTarget) {
                    val r = SystemClock.elapsedRealtime() - t0
                    result = r
                    phase = AccelPhase.DONE
                    val id = System.currentTimeMillis()
                    app.laps.add(LapSession(id, "accel", id, listOf(r), peak, "0-${cfg.accelTarget} km/h"))
                } else if (v < 0.5f) {
                    phase = AccelPhase.READY
                }
            }
            AccelPhase.DONE -> {}
        }
    }

    val (title, color) = when (phase) {
        AccelPhase.WAIT -> "หยุดรถให้นิ่งก่อน" to StatusColors.warn
        AccelPhase.READY -> "พร้อมแล้ว — ออกตัวได้" to StatusColors.ok
        AccelPhase.RUNNING -> "กำลังจับเวลา…" to MaterialTheme.colorScheme.primary
        AccelPhase.DONE -> "เสร็จสิ้น" to StatusColors.ok
    }
    val shown = when (phase) {
        AccelPhase.RUNNING -> lapText(now - t0)
        AccelPhase.DONE -> result?.let { lapText(it) } ?: "--"
        else -> "0:00.00"
    }

    GlassCard(Modifier.fillMaxSize(), padding = 28.dp) {
        Text("วัดอัตราเร่ง 0–${cfg.accelTarget} km/h", style = MaterialTheme.typography.titleLarge)
        Text(
            "ใช้ในสนามหรือพื้นที่ปิดเท่านั้น · ความละเอียดขึ้นกับความถี่อ่านข้อมูล (${cfg.pollMs} ms)",
            style = MaterialTheme.typography.bodyMedium, color = StatusColors.warn
        )
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, color = color)
        Text(shown, fontSize = 88.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text("ความเร็วตอนนี้ ${speedValue(st.speedKmh, "kmh")} km/h · สูงสุด ${speedValue(peak, "kmh")} km/h")
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { (st.speedKmh / cfg.accelTarget).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
        )
        Spacer(Modifier.height(20.dp))
        Button({ phase = AccelPhase.WAIT; result = null; peak = 0f }) { Text("เริ่มใหม่") }
    }
}

// ---------------- ประวัติ ----------------

@Composable
private fun HistoryPane(cfg: AppSettings) {
    val app = LocalContext.current.app
    val sessions by app.laps.sessions.collectAsStateWithLifecycle()
    val fmt = remember { SimpleDateFormat("d MMM yyyy HH:mm", th.dolphin.launcher.i18n.I18n.dateLocale()) }
    var expanded by remember { mutableStateOf<Long?>(null) }

    if (sessions.isEmpty()) {
        GlassCard(Modifier.fillMaxWidth()) {
            Text("ยังไม่มีประวัติ — กด ‘บันทึก’ หลังจับเวลารอบ หรือวัดอัตราเร่งให้ครบ", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(sessions, key = { it.id }) { s ->
            GlassCard(Modifier.fillMaxWidth(), onClick = { expanded = if (expanded == s.id) null else s.id }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (s.type == "accel") Icons.Rounded.Speed else Icons.Rounded.Timer, null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${fmt.format(Date(s.createdAt))} · ${s.laps.size} รอบ · เร็วสุด ${speedValue(s.maxSpeed, cfg.speedUnit)} ${speedUnit(cfg.speedUnit)}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(s.best?.let { lapText(it) } ?: "--", fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    IconButton({ app.laps.delete(s.id) }) { Icon(Icons.Rounded.Delete, "ลบ") }
                }
                if (expanded == s.id) {
                    Spacer(Modifier.height(8.dp))
                    s.laps.forEachIndexed { i, t ->
                        Row(Modifier.padding(vertical = 3.dp)) {
                            Text("รอบ ${i + 1}", Modifier.width(90.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                lapText(t), fontFamily = FontFamily.Monospace,
                                color = if (t == s.best) StatusColors.ok else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
