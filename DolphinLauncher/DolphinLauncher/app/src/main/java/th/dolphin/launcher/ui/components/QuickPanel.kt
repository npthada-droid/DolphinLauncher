@file:OptIn(ExperimentalLayoutApi::class)

package th.dolphin.launcher.ui.components

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import th.dolphin.launcher.i18n.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import th.dolphin.launcher.CrashGuard
import th.dolphin.launcher.app
import th.dolphin.launcher.location.WeatherCodes
import th.dolphin.launcher.settings.AppSettings
import th.dolphin.launcher.ui.theme.LocalUi
import th.dolphin.launcher.util.tempText
import th.dolphin.launcher.util.toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** แผงควบคุมด่วน: เสียง ความสว่าง ผู้ช่วยเสียง Wi-Fi บลูทูธ + สภาพอากาศและ GPS */
@Composable
fun QuickPanel(cfg: AppSettings, onDismiss: () -> Unit, onAction: (String) -> Unit, onRequestLocation: () -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.app
    val audio = remember { ctx.getSystemService(AudioManager::class.java) }
    val maxVol = remember { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var vol by remember { mutableFloatStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }
    val canWrite = remember { Settings.System.canWrite(ctx) }
    var bright by remember {
        mutableFloatStateOf(runCatching { Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS) }.getOrDefault(128).toFloat())
    }
    val weather by app.weather.weather.collectAsStateWithLifecycle()
    val fix by app.location.fix.collectAsStateWithLifecycle()
    val permitted by app.location.permitted.collectAsStateWithLifecycle()
    val hm = remember { SimpleDateFormat("HH:mm", Locale.US) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.widthIn(max = 760.dp).fillMaxWidth(0.9f),
            shape = RoundedCornerShape(LocalUi.current.corner), color = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.padding(22.dp).verticalScroll(rememberScrollState())) {
                Text("แผงควบคุมด่วน", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))

                // ---- เสียง / ความสว่าง ----
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Speaker, "ระดับเสียง")
                    Spacer(Modifier.width(10.dp))
                    Slider(
                        value = vol, onValueChange = {
                            vol = it
                            runCatching { audio.setStreamVolume(AudioManager.STREAM_MUSIC, it.roundToInt(), 0) }
                        },
                        valueRange = 0f..maxVol.toFloat(), steps = (maxVol - 1).coerceAtLeast(0), modifier = Modifier.weight(1f),
                    )
                    Text("${vol.roundToInt()}", Modifier.width(32.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.BrightnessMedium, "ความสว่าง")
                    Spacer(Modifier.width(10.dp))
                    if (canWrite) {
                        Slider(
                            value = bright, onValueChange = {
                                bright = it
                                runCatching {
                                    Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                                    Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS, it.roundToInt())
                                }
                            },
                            valueRange = 10f..255f, modifier = Modifier.weight(1f),
                        )
                        Text("${(bright / 255f * 100).roundToInt()}%", Modifier.width(40.dp))
                    } else {
                        Text("ต้องอนุญาต ‘แก้ไขการตั้งค่าระบบ’", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton({
                            open(ctx, Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${ctx.packageName}")))
                        }) { Text("อนุญาต") }
                    }
                }
                Spacer(Modifier.height(10.dp))

                // ---- ปุ่มลัด ----
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickBtn(Icons.Rounded.Mic, "ผู้ช่วยเสียง") {
                        val ok = open(ctx, Intent(Intent.ACTION_VOICE_COMMAND)) || open(ctx, Intent("android.intent.action.ASSIST"))
                        if (!ok) ctx.toast("ไม่พบแอปผู้ช่วยเสียงในเครื่อง")
                    }
                    QuickBtn(Icons.Rounded.Wifi, "Wi-Fi") { open(ctx, Intent(Settings.ACTION_WIFI_SETTINGS)) }
                    QuickBtn(Icons.Rounded.Bluetooth, "บลูทูธ") { onAction("action:bluetooth") }
                    QuickBtn(Icons.Rounded.Contrast, "สลับธีม") { onAction("action:theme") }
                    QuickBtn(Icons.Rounded.Settings, "ตั้งค่าระบบ") { open(ctx, Intent(Settings.ACTION_SETTINGS)) }
                    QuickBtn(Icons.Rounded.Home, "Launcher เดิม") {
                        if (!CrashGuard.openFallbackHome(ctx)) ctx.toast("ไม่พบ Launcher อื่นในเครื่อง")
                    }
                }
                Spacer(Modifier.height(16.dp))

                // ---- สภาพอากาศ ----
                Text("สภาพอากาศ", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                val w = weather
                when {
                    !permitted -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("ต้องอนุญาตตำแหน่งเพื่อดูสภาพอากาศและใช้ GPS", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onRequestLocation) { Text("อนุญาตตำแหน่ง") }
                    }
                    !cfg.weatherEnabled -> Text("ปิดสภาพอากาศไว้ในตั้งค่า", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    w == null -> Text("กำลังโหลด… (ต้องเชื่อมต่ออินเทอร์เน็ต)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else -> {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(tempText(w.tempC, cfg.tempUnit), fontSize = 40.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.padding(bottom = 6.dp)) {
                                Text(WeatherCodes.text(w.code), style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "ลม ${w.windKmh.roundToInt()} km/h · ชื้น ${w.humidity}%" + (w.rainChance?.let { " · โอกาสฝน $it%" } ?: ""),
                                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.padding(top = 6.dp)) {
                            w.days.forEach { d ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(d.dateLabel, style = MaterialTheme.typography.labelMedium)
                                    Text(WeatherCodes.text(d.code), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${d.max.roundToInt()}° / ${d.min.roundToInt()}°", style = MaterialTheme.typography.titleSmall)
                                }
                            }
                            Column {
                                Text("พระอาทิตย์", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    "ขึ้น ${w.sunrise?.let { hm.format(Date(it)) } ?: "--"} · ตก ${w.sunset?.let { hm.format(Date(it)) } ?: "--"}",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))

                // ---- GPS ----
                Text("GPS", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                val f = fix
                if (f == null) {
                    Text(if (permitted) "กำลังหาสัญญาณ GPS…" else "ยังไม่ได้อนุญาตตำแหน่ง", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                        Info(Icons.Rounded.Explore, "ทิศทาง", f.bearing?.let { "${compass(it)} ${it.roundToInt()}°" } ?: "--")
                        Info(Icons.Rounded.LocationOn, "ความสูง", f.altitudeM?.let { "${it.roundToInt()} ม." } ?: "--")
                        Info(Icons.Rounded.Explore, "ความเร็ว GPS", f.speedKmh?.let { "${it.roundToInt()} km/h" } ?: "--")
                        Info(Icons.Rounded.LocationOn, "ความแม่นยำ", f.accuracyM?.let { "±${it.roundToInt()} ม." } ?: "--")
                    }
                }
                TextButton(onDismiss, Modifier.align(Alignment.End)) { Text("ปิด") }
            }
        }
    }
}

fun compass(deg: Float): String {
    val dirs = listOf("เหนือ", "ตอ.เฉียงเหนือ", "ตะวันออก", "ตอ.เฉียงใต้", "ใต้", "ตต.เฉียงใต้", "ตะวันตก", "ตต.เฉียงเหนือ")
    return dirs[(((deg % 360) + 360) % 360 / 45f).roundToInt() % 8]
}

private fun open(ctx: Context, i: Intent): Boolean =
    runCatching { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true }.getOrDefault(false)

@Composable
private fun QuickBtn(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier.width(96.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun Info(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall)
        }
    }
}
