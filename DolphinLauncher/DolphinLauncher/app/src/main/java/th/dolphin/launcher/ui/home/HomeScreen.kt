@file:OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)

package th.dolphin.launcher.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AirlineSeatReclineNormal
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DoorFront
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.runtime.key
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.DeviceThermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import th.dolphin.launcher.i18n.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import th.dolphin.launcher.alerts.tireStatus
import th.dolphin.launcher.app
import th.dolphin.launcher.settings.AppSettings
import th.dolphin.launcher.ui.components.AddTile
import th.dolphin.launcher.ui.components.AppPickerDialog
import th.dolphin.launcher.ui.components.CarTopView
import th.dolphin.launcher.ui.components.GlassCard
import th.dolphin.launcher.ui.components.Pill
import th.dolphin.launcher.ui.components.RoundIconButton
import th.dolphin.launcher.ui.components.ShortcutTile
import th.dolphin.launcher.ui.components.TurnArrows
import th.dolphin.launcher.ui.components.rememberImageFile
import th.dolphin.launcher.ui.components.resolveShortcut
import th.dolphin.launcher.ui.components.tireColor
import th.dolphin.launcher.ui.theme.LocalUi
import th.dolphin.launcher.ui.theme.StatusColors
import th.dolphin.launcher.util.copyImageToFiles
import th.dolphin.launcher.util.distText
import th.dolphin.launcher.util.pressureText
import th.dolphin.launcher.util.pressureUnit
import th.dolphin.launcher.util.speedUnit
import th.dolphin.launcher.util.speedValue
import th.dolphin.launcher.util.tempText
import th.dolphin.launcher.util.toast
import th.dolphin.launcher.vehicle.Tire
import th.dolphin.launcher.vehicle.TurnSignal
import th.dolphin.launcher.vehicle.VehicleState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HomeScreen(cfg: AppSettings, st: VehicleState, onAction: (String) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp)) {
        val landscape = when (cfg.layoutMode) {
            "landscape" -> true
            "portrait" -> false
            else -> maxWidth > maxHeight
        }
        val ctl = remember { CenterCtl(cfg.carShowSec) }
        if (ctl.full) {
            BackHandler { ctl.full = false }
            CenterStage(cfg, st, Modifier.fillMaxSize(), onAction, ctl, full = true)
            return@BoxWithConstraints
        }
        Column(Modifier.fillMaxSize()) {
            if (ctl.arrange) ArrangeBar(cfg) { ctl.arrange = false }
            else TopBar(cfg, st, onAction) { ctl.arrange = true }
            Spacer(Modifier.height(12.dp))
            if (cfg.widgetsPage) {
                // ปัดซ้ายเพื่อไปหน้าวิดเจ็ต
                val pager = rememberPagerState { 2 }
                HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth(), beyondViewportPageCount = 1) { page ->
                    if (page == 0) Column(Modifier.fillMaxSize()) { Dashboard(cfg, st, onAction, landscape, ctl) }
                    else th.dolphin.launcher.ui.widgets.WidgetsBoard(Modifier.fillMaxSize())
                }
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.Center) {
                    repeat(2) { i ->
                        Box(
                            Modifier.padding(horizontal = 3.dp).size(if (i == pager.currentPage) 18.dp else 6.dp, 6.dp).clip(CircleShape)
                                .background(
                                    if (i == pager.currentPage) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                )
                        )
                    }
                }
            } else {
                Column(Modifier.weight(1f).fillMaxWidth()) { Dashboard(cfg, st, onAction, landscape, ctl) }
            }

        }
    }
}

/** แดชบอร์ดหลัก (ต้องเรียกภายใน Column ที่มีความสูงจำกัด) — ตำแหน่งการ์ดมาจากตั้งค่า (homeLeft / homeRight / centerPos) */
@Composable
private fun androidx.compose.foundation.layout.ColumnScope.Dashboard(
    cfg: AppSettings, st: VehicleState, onAction: (String) -> Unit, landscape: Boolean, ctl: CenterCtl,
) {
    val app = LocalContext.current.app
    val arrange = ctl.arrange
    val left = cfg.homeLeft.filter { it in HOME_CARDS }
    val right = cfg.homeRight.filter { it in HOME_CARDS && it !in left }
    fun move(id: String, dir: String) = app.settings.update { s -> moveHomeCard(s, id, dir) }

    @Composable
    fun Card(id: String, modifier: Modifier) {
        Arrangeable(arrange, modifier, onMove = { dir -> move(id, dir) }, onHide = { move(id, "hide") }) {
            val m = if (isBigCard(id)) Modifier.fillMaxSize() else Modifier.fillMaxWidth()
            when (id) {
                "energy" -> EnergyCard(cfg, st, m) { onAction("action:battery") }
                "safety" -> SafetyCard(st, m)
                "tires" -> TireCard(cfg, st, m) { onAction("action:tires") }
                "media" -> MediaCard(m, onAction)
                "phone" -> PhoneCard(cfg, m, onAction)
            }
        }
    }

    if (landscape) {
        @Composable
        fun androidx.compose.foundation.layout.RowScope.Side(ids: List<String>) {
            Column(Modifier.weight(0.25f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ids.forEach { id ->
                    key(id) { Card(id, if (isBigCard(id)) Modifier.weight(1f).fillMaxWidth() else Modifier.fillMaxWidth()) }
                }
                if (ids.none { isBigCard(it) }) Spacer(Modifier.weight(1f))
            }
        }
        @Composable
        fun androidx.compose.foundation.layout.RowScope.Center() {
            Arrangeable(
                arrange, Modifier.weight(0.5f).fillMaxHeight(),
                onMove = { dir -> app.settings.update { s -> s.copy(centerPos = shiftCenter(s.centerPos, dir)) } },
                onHide = null, vertical = false,
            ) { CenterStage(cfg, st, Modifier.fillMaxSize(), onAction, ctl) }
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            when (cfg.centerPos) {
                "left" -> { Center(); Side(left); Side(right) }
                "right" -> { Side(left); Side(right); Center() }
                else -> { Side(left); Center(); Side(right) }
            }
        }
    } else {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CenterStage(cfg, st, Modifier.fillMaxWidth().height(460.dp), onAction, ctl)
            (left + right).forEach { id ->
                key(id) { Card(id, if (isBigCard(id)) Modifier.fillMaxWidth().height(300.dp) else Modifier.fillMaxWidth()) }
            }
        }
    }
}

private val HOME_CARDS = listOf("energy", "safety", "tires", "media", "phone")
private fun isBigCard(id: String) = id == "energy" || id == "tires"
private fun cardName(id: String) = when (id) {
    "energy" -> "แบตเตอรี่"
    "safety" -> "ประตู/เข็มขัด"
    "tires" -> "ลมยาง"
    "media" -> "เพลง"
    "phone" -> "โทรศัพท์"
    else -> id
}

private fun shiftCenter(pos: String, dir: String): String {
    val order = listOf("left", "center", "right")
    val i = order.indexOf(pos).coerceAtLeast(0)
    return order[(i + if (dir == "left") -1 else if (dir == "right") 1 else 0).coerceIn(0, 2)]
}

/** ย้ายการ์ด: up/down = ในคอลัมน์เดิม, left/right = ไปคอลัมน์ข้าง, hide = ซ่อน, show = แสดงกลับ (ท้ายคอลัมน์ขวา) */
private fun moveHomeCard(s: AppSettings, id: String, dir: String): AppSettings {
    val l = s.homeLeft.filter { it in HOME_CARDS }.toMutableList()
    val r = s.homeRight.filter { it in HOME_CARDS && it !in l }.toMutableList()
    val col = if (id in l) l else if (id in r) r else null
    when (dir) {
        "up", "down" -> col?.let { c ->
            val i = c.indexOf(id)
            val j = if (dir == "up") i - 1 else i + 1
            if (j in c.indices) { c[i] = c[j]; c[j] = id }
        }
        "left" -> if (id in r) { r.remove(id); l.add(id) }
        "right" -> if (id in l) { l.remove(id); r.add(id) }
        "hide" -> { l.remove(id); r.remove(id) }
        "show" -> if (col == null) r.add(id)
    }
    return s.copy(homeLeft = l, homeRight = r)
}

/** ห่อการ์ดด้วยปุ่มย้ายตำแหน่งเมื่ออยู่ในโหมดจัดวาง */
@Composable
private fun Arrangeable(
    arrange: Boolean,
    modifier: Modifier,
    onMove: (String) -> Unit,
    onHide: (() -> Unit)?,
    vertical: Boolean = true,
    content: @Composable () -> Unit,
) {
    val ui = LocalUi.current
    val shape = RoundedCornerShape(ui.corner)
    Box(modifier) {
        content()
        if (arrange) {
            Box(
                Modifier.matchParentSize().clip(shape).background(Color.Black.copy(alpha = 0.5f))
                    .border(2.dp, ui.accent, shape).clickable { },
                contentAlignment = Alignment.Center,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    ArrowBtn(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "ย้ายซ้าย") { onMove("left") }
                    if (vertical) {
                        ArrowBtn(Icons.Rounded.KeyboardArrowUp, "ย้ายขึ้น") { onMove("up") }
                        ArrowBtn(Icons.Rounded.KeyboardArrowDown, "ย้ายลง") { onMove("down") }
                    }
                    ArrowBtn(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "ย้ายขวา") { onMove("right") }
                    if (onHide != null) ArrowBtn(Icons.Rounded.VisibilityOff, "ซ่อน", StatusColors.bad) { onHide() }
                }
            }
        }
    }
}

@Composable
private fun ArrowBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, bg: Color = LocalUi.current.accent, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = Color.White, modifier = Modifier.size(26.dp)) }
}

/** แถบด้านบนระหว่างจัดวาง: แสดงการ์ดที่ซ่อน + รีเซ็ต + เสร็จ */
@Composable
private fun ArrangeBar(cfg: AppSettings, onDone: () -> Unit) {
    val app = LocalContext.current.app
    val hidden = HOME_CARDS.filter { it !in cfg.homeLeft && it !in cfg.homeRight }
    Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("จัดวางหน้าหลัก", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("ใช้ลูกศรย้ายการ์ด · ปุ่มแดง = ซ่อน", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        hidden.forEach { id ->
            TextButton({ app.settings.update { s -> moveHomeCard(s, id, "show") } }) { Text("+ " + cardName(id)) }
        }
        TextButton({
            val d = AppSettings()
            app.settings.update { s -> s.copy(homeLeft = d.homeLeft, homeRight = d.homeRight, centerPos = d.centerPos) }
        }) { Text("รีเซ็ต") }
        Spacer(Modifier.width(6.dp))
        androidx.compose.material3.Button(onDone) { Text("เสร็จ") }
    }
}

// ---------------- แถบบน ----------------

@Composable
private fun rememberClock(): State<Date> = produceState(Date()) {
    while (true) {
        value = Date()
        delay(1000L - System.currentTimeMillis() % 1000L)
    }
}

@Composable
private fun TopBar(cfg: AppSettings, st: VehicleState, onAction: (String) -> Unit, onArrange: () -> Unit) {
    val now by rememberClock()
    val timeFmt = remember(cfg.use24h, cfg.showSeconds) {
        SimpleDateFormat((if (cfg.use24h) "HH:mm" else "h:mm") + (if (cfg.showSeconds) ":ss" else ""), Locale.US)
    }
    val dateFmt = remember { SimpleDateFormat("EEEE d MMMM", th.dolphin.launcher.i18n.I18n.dateLocale()) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
            Text(timeFmt.format(now), fontSize = 40.sp, fontWeight = FontWeight.Light, lineHeight = 42.sp, letterSpacing = 1.sp)
            Spacer(Modifier.width(12.dp))
            Text(
                dateFmt.format(now), style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill(if (st.connected) "เชื่อมต่อรถ" else st.source.ifBlank { "ไม่เชื่อมต่อ" }, dot = if (st.connected) StatusColors.ok else StatusColors.bad)
            val weather by LocalContext.current.app.weather.weather.collectAsStateWithLifecycle()
            val fix by LocalContext.current.app.location.fix.collectAsStateWithLifecycle()
            fix?.bearing?.takeIf { st.speedKmh > 3f }?.let { b ->
                Pill(th.dolphin.launcher.ui.components.compass(b), icon = Icons.Rounded.Explore)
            }
            val w = weather
            Box(Modifier.clip(CircleShape).clickable { onAction("action:quick") }) {
                when {
                    w != null -> Pill(
                        tempText(w.tempC, cfg.tempUnit) + " " + th.dolphin.launcher.location.WeatherCodes.text(w.code),
                        icon = Icons.Rounded.WbSunny,
                    )
                    st.outsideTempC != null -> Pill(tempText(st.outsideTempC, cfg.tempUnit), icon = Icons.Rounded.Thermostat)
                    else -> Pill("แผงควบคุม", icon = Icons.Rounded.Tune)
                }
            }
            RoundIconButton(Icons.Rounded.Dashboard, "จัดวางหน้าหลัก", onArrange, size = 46.dp)
            RoundIconButton(Icons.Rounded.Tune, "แผงควบคุม", { onAction("action:quick") }, size = 46.dp)
        }
    }
}

// ---------------- ส่วนกลาง: รูปรถ + เกจ ----------------

/**
 * ส่วนกลาง: แสดงรูปรถ [AppSettings.carShowSec] วินาที แล้วสลับเป็นแผนที่หรือมัลติมีเดียอัตโนมัติ
 * กดปุ่ม รถ / แผนที่ / เพลง เพื่อสลับเองได้ (กด "รถ" = เริ่มนับใหม่)
 */
@Composable
private fun CenterStage(
    cfg: AppSettings, st: VehicleState, modifier: Modifier, onAction: (String) -> Unit,
    ctl: CenterCtl, full: Boolean = false,
) {
    val app = LocalContext.current.app
    val now by app.media.now.collectAsStateWithLifecycle()
    var mode by ctl::mode
    var manual by ctl::manual
    var restart by ctl::restart
    var left by ctl::left
    val target = when (cfg.centerAfter) {
        "map" -> "map"
        "media" -> "media"
        "car" -> "car"
        else -> if (now?.playing == true) "media" else "map"
    }
    // นับถอยหลังแล้วสลับ
    LaunchedEffect(restart, cfg.carShowSec, cfg.centerAfter) {
        if (mode != "car" || cfg.centerAfter == "car") return@LaunchedEffect
        if (left <= 0 || left > cfg.carShowSec) left = cfg.carShowSec.coerceIn(1, 120)
        while (left > 0) {
            delay(1000)
            left--
        }
        mode = target
    }
    // โหมดอัตโนมัติ: ตามสถานะเพลงหลังสลับแล้ว (ถ้าผู้ใช้ไม่ได้เลือกเอง)
    LaunchedEffect(target) {
        if (mode != "car" && !manual && cfg.centerAfter == "auto") mode = target
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(cfg.carName.uppercase(), fontSize = 20.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp, maxLines = 1)
                if (cfg.plate.isNotBlank()) Text(cfg.plate, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            ModeSwitch(mode) { m ->
                manual = m != "car"
                mode = m
                if (m == "car") { left = cfg.carShowSec.coerceIn(1, 120); restart++ }
            }
            Spacer(Modifier.width(10.dp))
            GearIndicator(st.gear)
            Spacer(Modifier.width(10.dp))
            RoundIconButton(
                if (full) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                if (full) "ออกจากเต็มจอ" else "เต็มจอ",
                { ctl.full = !full }, size = 42.dp,
            )
        }
        Spacer(Modifier.height(6.dp))
        androidx.compose.animation.Crossfade(mode, Modifier.weight(1f).fillMaxWidth(), animationSpec = tween(600), label = "center") { m ->
            when (m) {
                "map" -> CenterMap(cfg, Modifier.fillMaxSize(), onAction)
                "media" -> CenterMedia(Modifier.fillMaxSize(), onAction)
                else -> Box(Modifier.fillMaxSize()) {
                    CarStage(cfg, Modifier.fillMaxSize())
                    if (cfg.centerAfter != "car") {
                        Text(
                            "สลับเป็น${if (target == "media") "มัลติมีเดีย" else "แผนที่"}ใน $left วินาที",
                            Modifier.align(Alignment.BottomCenter).clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 12.dp, vertical = 4.dp),
                            color = Color.White, style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
        SpeedGauge(cfg, st, Modifier.fillMaxWidth(if (full) 0.36f else 0.6f).height(if (full) 92.dp else 108.dp))
    }
}

/** สถานะจอกลาง เก็บไว้ที่ HomeScreen เพื่อไม่ให้รีเซ็ตเมื่อสลับเต็มจอ */
class CenterCtl(showSec: Int) {
    var mode by mutableStateOf("car")
    var manual by mutableStateOf(false)
    var restart by mutableIntStateOf(0)
    var left by mutableIntStateOf(showSec)
    var full by mutableStateOf(false)
    var arrange by mutableStateOf(false)
}

@Composable
private fun ModeSwitch(mode: String, onSelect: (String) -> Unit) {
    val items = listOf("car" to Icons.Rounded.DirectionsCar, "map" to Icons.Rounded.Map, "media" to Icons.Rounded.MusicNote)
    Row(Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)).padding(3.dp)) {
        items.forEach { (k, icon) ->
            val on = k == mode
            Box(
                Modifier.size(40.dp, 34.dp).clip(CircleShape)
                    .background(if (on) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(k) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, k, tint = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** แผนที่กลางจอ: OpenStreetMap ในตัว หรือวิดเจ็ตแอปแผนที่ที่เลือกไว้ */
@Composable
private fun CenterMap(cfg: AppSettings, modifier: Modifier, onAction: (String) -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.app
    val shape = RoundedCornerShape(LocalUi.current.corner)
    Box(modifier.clip(shape).border(1.dp, MaterialTheme.colorScheme.outline, shape)) {
        if (cfg.mapSource == "widget" && cfg.centerWidgetId >= 0) {
            th.dolphin.launcher.ui.widgets.HostedWidget(cfg.centerWidgetId, Modifier.fillMaxSize())
        } else {
            val fix by app.location.fix.collectAsStateWithLifecycle()
            OsmMap(fix, cfg.mapZoom.toDouble(), LocalUi.current.dark && cfg.mapDarkTiles, Modifier.fillMaxSize())
            if (fix == null) {
                Text(
                    "กำลังหาตำแหน่ง GPS…", Modifier.align(Alignment.Center).clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 14.dp, vertical = 6.dp),
                    color = Color.White,
                )
            }
            Row(Modifier.align(Alignment.BottomEnd).padding(8.dp)) {
                Text(
                    "เปิดแอปแผนที่", Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.6f))
                        .clickable {
                            val ok = app.apps.openUri("geo:0,0?q=")
                            if (!ok) ctx.toast("ไม่พบแอปแผนที่")
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    color = Color.White, style = MaterialTheme.typography.labelLarge,
                )
            }
            Text(
                "© OpenStreetMap", Modifier.align(Alignment.BottomStart).padding(6.dp),
                color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

/** มัลติมีเดียกลางจอ: ปกเพลงใหญ่ + ปุ่มควบคุม */
@Composable
private fun CenterMedia(modifier: Modifier, onAction: (String) -> Unit) {
    val app = LocalContext.current.app
    val now by app.media.now.collectAsStateWithLifecycle()
    val ui = LocalUi.current
    GlassCard(modifier, padding = 18.dp) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.fillMaxHeight(0.8f).aspectRatio(1f).clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(ui.accent2, ui.accent))).clickable { onAction("action:audio") },
                contentAlignment = Alignment.Center,
            ) {
                val art = now?.art
                if (art != null) Image(art, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Icon(Icons.Rounded.MusicNote, null, tint = Color.White, modifier = Modifier.size(56.dp))
            }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                Text("กำลังเล่น · ${now?.appLabel ?: "—"}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(now?.title?.ifBlank { null } ?: "ไม่มีเพลงที่กำลังเล่น", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Text(now?.artist.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CtrlButton(Icons.Rounded.SkipPrevious, "ก่อนหน้า", false) { app.media.previous() }
                    CtrlButton(if (now?.playing == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "เล่น/หยุด", true) { app.media.playPause() }
                    CtrlButton(Icons.Rounded.SkipNext, "ถัดไป", false) { app.media.next() }
                }
                Spacer(Modifier.height(10.dp))
                Row {
                    SourceButton(Icons.Rounded.Radio, "วิทยุ") { onAction("action:radio") }
                    SourceButton(Icons.Rounded.BluetoothAudio, "บลูทูธ") { onAction("action:btmusic") }
                    SourceButton(Icons.Rounded.LibraryMusic, "เพลง") { onAction("action:music") }
                    SourceButton(Icons.Rounded.Tune, "หน้าเสียง") { onAction("action:audio") }
                }
            }
        }
    }
}

/** เวทีรูปรถ: แสงพื้น + รูปรถ + เงาสะท้อน หรือช่องใส่รูปเมื่อยังไม่มีรูป */
@Composable
private fun CarStage(cfg: AppSettings, modifier: Modifier) {
    val ctx = LocalContext.current
    val app = ctx.app
    val ui = LocalUi.current
    val img = rememberImageFile(cfg.carImagePath)
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { copyImageToFiles(ctx, it, "car") }?.let { p -> app.settings.update { s -> s.copy(carImagePath = p) } }
    }
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val h = maxHeight
        // แสงบนพื้นใต้รถ
        Canvas(
            Modifier.fillMaxWidth(0.82f).height(h * 0.2f).align(Alignment.BottomCenter).offset(y = -(h * 0.06f))
        ) {
            drawOval(
                Brush.radialGradient(
                    listOf(ui.accent.copy(alpha = 0.55f), ui.accent.copy(alpha = 0.12f), Color.Transparent),
                    center = center, radius = size.width / 2f,
                ),
                size = size,
            )
        }
        if (img != null) {
            val carH = h * 0.74f
            Column(Modifier.fillMaxWidth(0.94f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom) {
                Image(
                    img, cfg.carName,
                    Modifier.fillMaxWidth().height(carH).clickable { pick.launch("image/*") },
                    contentScale = ContentScale.Fit, alignment = Alignment.BottomCenter,
                )
                // เงาสะท้อน: กลับหัวรูป แล้วไล่ความจางลง
                Box(Modifier.fillMaxWidth().height(h * 0.2f).clipToBounds()) {
                    Image(
                        img, null,
                        Modifier.fillMaxWidth()
                            .wrapContentHeight(Alignment.Top, unbounded = true)
                            .requiredHeight(carH)
                            .graphicsLayer {
                                scaleY = -1f
                                alpha = 0.28f
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                            .drawWithContent {
                                drawContent()
                                drawRect(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Transparent, Color.Black),
                                        startY = 0f, endY = size.height,
                                    ),
                                    blendMode = BlendMode.DstIn,
                                )
                            },
                        contentScale = ContentScale.Fit, alignment = Alignment.BottomCenter,
                    )
                }
            }
        } else {
            val shape = RoundedCornerShape(ui.corner)
            Column(
                Modifier.fillMaxWidth(0.84f).fillMaxHeight(0.74f)
                    .clip(shape)
                    .background(ui.accent.copy(alpha = 0.06f))
                    .drawBehind {
                        drawRoundRect(
                            ui.accent.copy(alpha = 0.6f),
                            cornerRadius = CornerRadius(ui.corner.toPx()),
                            style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f))),
                        )
                    }
                    .clickable { pick.launch("image/*") },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Rounded.AddPhotoAlternate, null, tint = ui.accent, modifier = Modifier.size(44.dp))
                Spacer(Modifier.height(8.dp))
                Text("แตะเพื่อใส่รูปรถของคุณ", style = MaterialTheme.typography.titleMedium)
                Text(
                    "PNG พื้นหลังโปร่งใส · ถ่ายด้านข้างหรือมุม 3/4",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun GearIndicator(gear: String) {
    val ui = LocalUi.current
    Row(
        Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surface.copy(alpha = ui.opacity))
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape).padding(4.dp)
    ) {
        listOf("P", "R", "N", "D").forEach { g ->
            val on = gear.equals(g, ignoreCase = true)
            Box(
                Modifier.size(38.dp).clip(CircleShape)
                    .background(if (on) MaterialTheme.colorScheme.primary else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    g, fontWeight = FontWeight.Bold,
                    color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** เกจความเร็วโค้งครึ่งวง ไล่สี accent2 → accent พร้อมแสงเรือง */
@Composable
private fun SpeedGauge(cfg: AppSettings, st: VehicleState, modifier: Modifier) {
    val ui = LocalUi.current
    val frac by animateFloatAsState((st.speedKmh / 160f).coerceIn(0f, 1f), tween(300), label = "spd")
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    Box(modifier, contentAlignment = Alignment.BottomCenter) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val d = minOf(size.width, size.height * 2f) - stroke
            val tl = Offset((size.width - d) / 2f, stroke / 2f)
            val arc = Size(d, d)
            val brush = Brush.horizontalGradient(listOf(ui.accent2, ui.accent), startX = tl.x, endX = tl.x + d)
            drawArc(track, 180f, 180f, false, tl, arc, style = Stroke(stroke, cap = StrokeCap.Round))
            if (frac > 0.005f) {
                drawArc(brush, 180f, 180f * frac, false, tl, arc, alpha = 0.25f, style = Stroke(stroke * 2.4f, cap = StrokeCap.Round))
                drawArc(brush, 180f, 180f * frac, false, tl, arc, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${speedValue(st.speedKmh, cfg.speedUnit)}",
                style = TextStyle(
                    fontSize = 54.sp, fontWeight = FontWeight.SemiBold, lineHeight = 54.sp,
                    shadow = Shadow(ui.accent.copy(alpha = 0.6f), blurRadius = 28f),
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                speedUnit(cfg.speedUnit) + (st.powerKw?.let { " · ${it.roundToInt()} kW" } ?: ""),
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TurnArrows(st.turn, 30.dp, onlyLeft = true)
            TurnArrows(st.turn, 30.dp, onlyRight = true)
        }
    }
}

// ---------------- การ์ดซ้าย ----------------

@Composable
private fun Label(text: String) {
    Text(
        text, style = MaterialTheme.typography.labelMedium, letterSpacing = 2.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun EnergyCard(cfg: AppSettings, st: VehicleState, modifier: Modifier, onClick: () -> Unit) {
    val ui = LocalUi.current
    val soc = st.socPercent
    val v = (soc ?: 0f).coerceIn(0f, 100f)
    val anim by animateFloatAsState(v / 100f, tween(800), label = "soc")
    val color = when {
        soc == null -> MaterialTheme.colorScheme.onSurfaceVariant
        v < 20f -> StatusColors.bad
        v < 40f -> StatusColors.warn
        else -> ui.accent
    }
    GlassCard(modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { Label("BATTERY") }
            st.sohPercent?.let {
                Text(
                    "SOH ${it.roundToInt()}%", style = MaterialTheme.typography.labelMedium,
                    color = if (it >= 90f) StatusColors.ok else if (it >= 80f) StatusColors.warn else StatusColors.bad,
                )
            }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                if (soc == null) "--" else "${v.roundToInt()}",
                style = TextStyle(
                    fontSize = 42.sp, fontWeight = FontWeight.SemiBold, lineHeight = 44.sp,
                    shadow = Shadow(color.copy(alpha = 0.7f), blurRadius = 24f),
                ),
                color = color,
            )
            Text("%", Modifier.padding(start = 4.dp, bottom = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Canvas(Modifier.fillMaxWidth().height(8.dp)) {
            val r = CornerRadius(size.height / 2)
            drawRoundRect(Color.White.copy(alpha = if (ui.dark) 0.06f else 0.0f), cornerRadius = r)
            drawRoundRect(Color.Black.copy(alpha = if (ui.dark) 0f else 0.05f), cornerRadius = r)
            if (anim > 0f) {
                drawRoundRect(
                    Brush.horizontalGradient(listOf(ui.accent2, color)),
                    size = Size(size.width * anim, size.height), cornerRadius = r,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Label("RANGE")
        Text(distText(st.rangeKm, cfg.speedUnit), fontSize = 24.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(10.dp))
        val weather by LocalContext.current.app.weather.weather.collectAsStateWithLifecycle()
        val outside = st.outsideTempC ?: weather?.tempC?.toFloat()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniStat(Icons.Rounded.Thermostat, "ภายนอก", tempText(outside, cfg.tempUnit), Modifier.weight(1f))
            MiniStat(Icons.Rounded.DeviceThermostat, "แบตเตอรี่", tempText(st.hvTempC, cfg.tempUnit), Modifier.weight(1f))
        }
        Spacer(Modifier.weight(1f))
        Row {
            Column(Modifier.weight(1f)) {
                Label("ODO")
                Text(distText(st.odometerKm, cfg.speedUnit), style = MaterialTheme.typography.titleSmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Label("POWER")
                Text(st.powerKw?.let { "${it.roundToInt()} kW" } ?: "--", style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun SafetyCard(st: VehicleState, modifier: Modifier) {
    val beltOk = st.belts.driver != false && !(st.belts.passengerOccupied && st.belts.passenger == false)
    GlassCard(modifier, padding = 10.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(
                Icons.Rounded.DoorFront, "ประตู",
                if (st.doors.anyOpen) st.doors.openNames().joinToString(", ") else "ปิดสนิท",
                !st.doors.anyOpen, Modifier.weight(1f)
            )
            StatusChip(
                Icons.Rounded.AirlineSeatReclineNormal, "เข็มขัด",
                if (beltOk) "คาดแล้ว" else "ยังไม่คาด", beltOk, Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatusChip(icon: ImageVector, title: String, value: String, ok: Boolean, modifier: Modifier) {
    val color = if (ok) StatusColors.ok else StatusColors.bad
    Column(
        modifier.clip(RoundedCornerShape(14.dp))
            .background(if (ok) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f) else color.copy(alpha = 0.16f))
            .padding(10.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, maxLines = 1, color = if (ok) MaterialTheme.colorScheme.onSurface else color)
    }
}

// ---------------- การ์ดขวา ----------------

@Composable
private fun TireCard(cfg: AppSettings, st: VehicleState, modifier: Modifier, onClick: () -> Unit) {
    val statuses = List(4) { tireStatus(st.tire(it), cfg) }
    val colors = statuses.map { tireColor(it) }
    val allOk = statuses.all { it == th.dolphin.launcher.alerts.TireStatus.OK }
    GlassCard(modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { Label("TIRES") }
            Text(pressureUnit(cfg.pressureUnit), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceAround) {
                MiniTire(st.tire(0), cfg, colors[0], Alignment.Start)
                MiniTire(st.tire(2), cfg, colors[2], Alignment.Start)
            }
            CarTopView(Modifier.weight(1.1f).fillMaxHeight(), st.doors, st.belts, cfg.rightHandDrive, colors, st.turn)
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceAround, horizontalAlignment = Alignment.End) {
                MiniTire(st.tire(1), cfg, colors[1], Alignment.End)
                MiniTire(st.tire(3), cfg, colors[3], Alignment.End)
            }
        }
        Text(
            if (allOk) "ทุกล้อปกติ" else "ตรวจสอบลมยาง",
            Modifier.fillMaxWidth(), color = if (allOk) MaterialTheme.colorScheme.onSurfaceVariant else StatusColors.warn,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun MiniTire(t: Tire, cfg: AppSettings, color: Color, align: Alignment.Horizontal) {
    Column(horizontalAlignment = align) {
        Text(
            pressureText(t.pressureBar, cfg.pressureUnit), fontSize = 24.sp, fontWeight = FontWeight.SemiBold,
            color = if (color == StatusColors.ok) MaterialTheme.colorScheme.onSurface else color
        )
        Text(tempText(t.tempC, cfg.tempUnit), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------------- Dock ----------------

@Composable
private fun Dock(cfg: AppSettings, onAction: (String) -> Unit) {
    val app = LocalContext.current.app
    val apps by app.apps.apps.collectAsStateWithLifecycle()
    val ui = LocalUi.current
    val shape = RoundedCornerShape(24.dp)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            Modifier.clip(shape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = ui.opacity))
                .border(1.dp, MaterialTheme.colorScheme.outline, shape)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            cfg.dock.forEach { key -> DockIcon(key, apps, 54.dp) { onAction(key) } }
        }
    }
}

@Composable
private fun DockIcon(key: String, apps: List<th.dolphin.launcher.apps.AppEntry>, size: Dp, onClick: () -> Unit) {
    val s = resolveShortcut(key, apps)
    val shape = RoundedCornerShape(16.dp)
    val bmp = s.bitmap
    Box(
        Modifier.size(size).clip(shape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (bmp != null) Image(bmp, s.label, Modifier.size(size * 0.66f))
        else Icon(s.vector ?: Icons.Rounded.Android, s.label, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(size * 0.46f))
    }
}

// ---------------- เพลง / วิทยุ ----------------

@Composable
private fun MediaCard(modifier: Modifier, onAction: (String) -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.app
    val now by app.media.now.collectAsStateWithLifecycle()
    val access by app.media.hasAccess.collectAsStateWithLifecycle()
    val ui = LocalUi.current
    GlassCard(modifier, padding = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val art = now?.art
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(ui.accent2.copy(alpha = 0.6f), ui.accent.copy(alpha = 0.6f)))),
                contentAlignment = Alignment.Center,
            ) {
                if (art != null) Image(art, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Icon(Icons.Rounded.MusicNote, null, tint = Color.White)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    now?.title?.ifBlank { null } ?: if (access) "ไม่มีเพลงที่กำลังเล่น" else "แตะเพื่ออนุญาตแสดงเพลง",
                    style = MaterialTheme.typography.titleSmall, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = if (!access) Modifier.clickable {
                        runCatching {
                            ctx.startActivity(
                                android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    } else Modifier,
                )
                Text(
                    listOfNotNull(now?.artist?.ifBlank { null }, now?.appLabel).joinToString(" · ").ifBlank { "วิทยุ · บลูทูธ · เพลง" },
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SourceButton(Icons.Rounded.Radio, "วิทยุ") { onAction("action:radio") }
            SourceButton(Icons.Rounded.BluetoothAudio, "บลูทูธ") { onAction("action:btmusic") }
            SourceButton(Icons.Rounded.LibraryMusic, "เพลง") { onAction("action:music") }
            Spacer(Modifier.weight(1f))
            CtrlButton(Icons.Rounded.SkipPrevious, "ก่อนหน้า", false) { app.media.previous() }
            CtrlButton(if (now?.playing == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "เล่น/หยุด", true) { app.media.playPause() }
            CtrlButton(Icons.Rounded.SkipNext, "ถัดไป", false) { app.media.next() }
        }
    }
}

@Composable
private fun SourceButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CtrlButton(icon: ImageVector, desc: String, primary: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.padding(horizontal = 2.dp).size(if (primary) 38.dp else 32.dp).clip(CircleShape)
            .background(if (primary) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon, desc, modifier = Modifier.size(22.dp),
            tint = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ---------------- โทรศัพท์ / บลูทูธ ----------------

@Composable
private fun PhoneCard(cfg: AppSettings, modifier: Modifier, onAction: (String) -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.app
    val bt by app.bluetooth.state.collectAsStateWithLifecycle()
    val dev = bt.connected.firstOrNull()
    val favs = remember(cfg.favorites) { th.dolphin.launcher.apps.QuickActions.favorites(cfg.favorites) }
    GlassCard(modifier, padding = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).clip(CircleShape)
                    .background(if (dev != null) StatusColors.ok.copy(alpha = 0.18f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                    .clickable { onAction("action:bluetooth") },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (dev != null) Icons.Rounded.BluetoothConnected else Icons.Rounded.Bluetooth, "บลูทูธ",
                    tint = if (dev != null) StatusColors.ok else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    dev?.name ?: if (!bt.enabled) "บลูทูธปิดอยู่" else "ยังไม่ได้เชื่อมต่อมือถือ",
                    style = MaterialTheme.typography.titleSmall, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    dev?.battery?.let { "แบตมือถือ $it%" } ?: if (dev != null) "เชื่อมต่อแล้ว" else "แตะไอคอนเพื่อเปิดตั้งค่าบลูทูธ",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(StatusColors.ok).clickable { onAction("action:phone") },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Phone, "โทรศัพท์", tint = Color(0xFF04140C)) }
        }
        if (favs.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                favs.take(3).forEach { (name, num) ->
                    Text(
                        name,
                        Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                            .clickable { if (!app.apps.dial(num)) ctx.toast("เปิดหน้าโทรไม่ได้") }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium, maxLines = 1,
                    )
                }
            }
        }
    }
}


@Composable
private fun MiniStat(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, modifier: Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = LocalUi.current.accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}
