@file:OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package th.dolphin.launcher.ui.audio

import android.content.Intent
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import th.dolphin.launcher.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import th.dolphin.launcher.app
import th.dolphin.launcher.settings.AppSettings
import th.dolphin.launcher.ui.components.GlassCard
import th.dolphin.launcher.ui.components.ScreenHeader
import th.dolphin.launcher.ui.theme.LocalUi
import th.dolphin.launcher.util.toast
import kotlin.math.roundToInt

/** หน้าเสียงและเพลง: กำลังเล่น + แหล่งเสียง + ระดับเสียงแยกประเภท + ปรับแต่งเสียง */
@Composable
fun AudioScreen(cfg: AppSettings, onAction: (String) -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.app
    val now by app.media.now.collectAsStateWithLifecycle()
    val access by app.media.hasAccess.collectAsStateWithLifecycle()
    val ui = LocalUi.current

    Column(Modifier.fillMaxSize().systemBarsPadding().padding(22.dp)) {
        ScreenHeader("เสียงและเพลง", null)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val wide = maxWidth > maxHeight
            val player = @Composable { m: Modifier ->
                GlassCard(m, padding = 20.dp) {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Box(
                            Modifier.fillMaxWidth(0.5f).aspectRatio(1f).clip(RoundedCornerShape(22.dp))
                                .background(Brush.linearGradient(listOf(ui.accent2, ui.accent))),
                            contentAlignment = Alignment.Center,
                        ) {
                            val art = now?.art
                            if (art != null) Image(art, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            else Icon(Icons.Rounded.MusicNote, null, tint = Color.White, modifier = Modifier.size(72.dp))
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            now?.title?.ifBlank { null } ?: if (access) "ไม่มีเพลงที่กำลังเล่น" else "ต้องอนุญาต ‘เข้าถึงการแจ้งเตือน’",
                            fontSize = 22.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            listOfNotNull(now?.artist?.ifBlank { null }, now?.appLabel).joinToString(" · ").ifBlank { "—" },
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                        )
                        if (!access) {
                            Text(
                                "แตะเพื่ออนุญาต", Modifier.padding(top = 6.dp).clip(CircleShape).clickable {
                                    runCatching { ctx.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                                }.padding(horizontal = 12.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                            Ctrl(Icons.Rounded.SkipPrevious, "ก่อนหน้า", 52.dp, false) { app.media.previous() }
                            Ctrl(if (now?.playing == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "เล่น/หยุด", 72.dp, true) { app.media.playPause() }
                            Ctrl(Icons.Rounded.SkipNext, "ถัดไป", 52.dp, false) { app.media.next() }
                        }
                    }
                }
            }
            val side = @Composable { m: Modifier ->
                Column(m.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Text("แหล่งเสียง", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Source(Icons.Rounded.Radio, "วิทยุ") { onAction("action:radio") }
                            Source(Icons.Rounded.BluetoothAudio, "บลูทูธ") { onAction("action:btmusic") }
                            Source(Icons.Rounded.LibraryMusic, "แอปเพลง") { onAction("action:music") }
                            Source(Icons.Rounded.Cast, "Android Auto") { onAction("action:projection") }
                        }
                    }
                    GlassCard(Modifier.fillMaxWidth()) {
                        Text("ระดับเสียง", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        VolumeRow(Icons.Rounded.MusicNote, "เพลง/สื่อ", AudioManager.STREAM_MUSIC)
                        VolumeRow(Icons.Rounded.Phone, "โทรศัพท์", AudioManager.STREAM_VOICE_CALL)
                        VolumeRow(Icons.Rounded.Notifications, "แจ้งเตือน", AudioManager.STREAM_NOTIFICATION)
                    }
                    GlassCard(Modifier.fillMaxWidth()) {
                        Text("ปรับแต่งเสียง", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        LinkRow(Icons.Rounded.Equalizer, "อีควอไลเซอร์ของระบบ") {
                            val ok = runCatching {
                                ctx.startActivity(
                                    Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                                        .putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }.isSuccess
                            if (!ok) ctx.toast("เครื่องนี้ไม่มีอีควอไลเซอร์ของระบบ — ลองใช้ในแอปเพลงหรือตั้งค่าเสียงของรถ")
                        }
                        LinkRow(Icons.Rounded.Tune, "ตั้งค่าเสียงของระบบ/รถ") {
                            runCatching { ctx.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                                .onFailure { ctx.toast("เปิดหน้าตั้งค่าเสียงไม่ได้") }
                        }
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Warning, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(10.dp))
                            Text("เสียงเตือนของ Launcher (กรณีวิกฤต)", Modifier.weight(1f))
                            Switch(cfg.alertSound, { v -> app.settings.update { it.copy(alertSound = v) } })
                        }
                    }
                }
            }
            if (wide) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    player(Modifier.weight(1.1f).fillMaxHeight())
                    side(Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    player(Modifier.fillMaxWidth().height(420.dp))
                    side(Modifier.weight(1f).fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun Ctrl(icon: ImageVector, desc: String, size: androidx.compose.ui.unit.Dp, primary: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(size).clip(CircleShape)
            .background(if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, desc, tint = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
private fun Source(icon: ImageVector, label: String, onClick: () -> Unit) {
    FilterChip(selected = false, onClick = onClick, label = { Text(label) }, leadingIcon = { Icon(icon, null, Modifier.size(18.dp)) })
}

@Composable
private fun VolumeRow(icon: ImageVector, label: String, stream: Int) {
    val ctx = LocalContext.current
    val am = remember { ctx.getSystemService(AudioManager::class.java) }
    val max = remember { runCatching { am.getStreamMaxVolume(stream) }.getOrDefault(15).coerceAtLeast(1) }
    var v by remember { mutableFloatStateOf(runCatching { am.getStreamVolume(stream).toFloat() }.getOrDefault(0f)) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, Modifier.width(76.dp), style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = v,
            onValueChange = {
                v = it
                runCatching { am.setStreamVolume(stream, it.roundToInt(), 0) }
            },
            valueRange = 0f..max.toFloat(), steps = (max - 1).coerceAtLeast(0), modifier = Modifier.weight(1f),
        )
        Text("${v.roundToInt()}", Modifier.width(28.dp))
    }
}

@Composable
private fun LinkRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(10.dp))
        Text(label, Modifier.weight(1f))
        Text("เปิด", color = MaterialTheme.colorScheme.primary)
    }
}
