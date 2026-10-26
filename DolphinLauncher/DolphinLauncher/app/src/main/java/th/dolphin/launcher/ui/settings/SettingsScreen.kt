@file:OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package th.dolphin.launcher.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import th.dolphin.launcher.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import th.dolphin.launcher.BuildConfig
import th.dolphin.launcher.app
import th.dolphin.launcher.apps.AppKind
import th.dolphin.launcher.apps.ShortcutKeys
import th.dolphin.launcher.settings.AppSettings
import th.dolphin.launcher.ui.camera.TurnCameraActivity
import th.dolphin.launcher.ui.camera.listCameras
import th.dolphin.launcher.ui.camera.parseCameraNames
import th.dolphin.launcher.ui.components.AddTile
import th.dolphin.launcher.ui.components.AppPickerDialog
import th.dolphin.launcher.ui.components.GenericCarSide
import th.dolphin.launcher.ui.components.GlassCard
import th.dolphin.launcher.ui.components.ScreenHeader
import th.dolphin.launcher.ui.components.ShortcutTile
import th.dolphin.launcher.ui.components.rememberImageFile
import th.dolphin.launcher.ui.theme.StatusColors
import th.dolphin.launcher.util.copyImageToFiles
import th.dolphin.launcher.util.fmt1
import th.dolphin.launcher.util.pressureText
import th.dolphin.launcher.util.pressureUnit
import th.dolphin.launcher.util.tempText
import th.dolphin.launcher.util.toast
import th.dolphin.launcher.vehicle.BydApiMap
import th.dolphin.launcher.vehicle.DemoControls
import th.dolphin.launcher.vehicle.TurnSignal
import kotlin.math.roundToInt

private val SECTIONS = listOf(
    "หน้าตา & ธีม", "รถของฉัน", "หน่วย & ลมยาง", "การแจ้งเตือน", "กล้อง & ไฟเลี้ยว",
    "ทางลัด & แอป", "ข้อมูลรถ (BYD API)", "โหมดทดสอบ", "เสียง & โทรศัพท์", "ทริป & บำรุงรักษา", "หน้าหลัก & แผนที่", "หน้าจอ & ไฟหน้า", "สำรองข้อมูล & ระบบ",
)

private val ACCENTS: List<Pair<Long, Long>> = listOf(
    0xFF2EC4B6 to 0xFF3B82F6, 0xFF38BDF8 to 0xFF8B5CF6, 0xFFA78BFA to 0xFFEC4899,
    0xFFF43F5E to 0xFFF97316, 0xFFF59E0B to 0xFFEF4444, 0xFFA3E635 to 0xFF22C55E,
    0xFF22D3EE to 0xFF6366F1, 0xFFE2E8F0 to 0xFF94A3B8,
)

@Composable
fun SettingsScreen(cfg: AppSettings, onBack: (() -> Unit)?, onOpenProbe: () -> Unit) {
    var section by rememberSaveable { mutableIntStateOf(0) }
    BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding().padding(22.dp)) {
        val wide = maxWidth > 840.dp
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("ตั้งค่า", onBack)
            if (wide) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Column(
                        Modifier.width(250.dp).fillMaxHeight().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        SECTIONS.forEachIndexed { i, s -> NavItem(s, i == section) { section = i } }
                    }
                    SectionContent(section, cfg, onOpenProbe, Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                ScrollableTabRow(selectedTabIndex = section, edgePadding = 0.dp, containerColor = Color.Transparent) {
                    SECTIONS.forEachIndexed { i, s -> Tab(selected = i == section, onClick = { section = i }, text = { Text(s) }) }
                }
                Spacer(Modifier.height(12.dp))
                SectionContent(section, cfg, onOpenProbe, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun NavItem(text: String, selected: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Text(
        text,
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(if (selected) c.primary.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        color = if (selected) c.primary else c.onSurface,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
    )
}

@Composable
private fun SectionContent(section: Int, cfg: AppSettings, onOpenProbe: () -> Unit, modifier: Modifier) {
    key(section) {
        Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when (section) {
                0 -> AppearanceSection(cfg)
                1 -> CarSection(cfg)
                2 -> UnitSection(cfg)
                3 -> AlertSection(cfg)
                4 -> CameraSection(cfg)
                5 -> ShortcutSection(cfg)
                6 -> DataSection(cfg, onOpenProbe)
                7 -> DemoSection(cfg)
                8 -> MediaPhoneSection(cfg)
                9 -> TripServiceSection(cfg)
                10 -> CenterSection(cfg)
                11 -> DisplaySection(cfg)
                else -> SystemSection()
            }
            Spacer(Modifier.height(48.dp))
        }
    }
}

// ---------------- helpers ----------------

@Composable
private fun rememberUpdater(): ((AppSettings) -> AppSettings) -> Unit {
    val app = LocalContext.current.app
    return remember(app) { { f: (AppSettings) -> AppSettings -> app.settings.update(f) } }
}

@Composable
fun SettingGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, sub: String? = null, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onChange(!checked) }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun ChoiceRow(label: String, options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (v, t) ->
                FilterChip(selected = selected == v, onClick = { onSelect(v) }, label = { Text(t) })
            }
        }
    }
}

@Composable
fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    display: (Float) -> String,
    onChange: (Float) -> Unit,
) {
    var v by remember(value) { mutableFloatStateOf(value) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(display(v), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
        Slider(value = v, onValueChange = { v = it }, valueRange = range, steps = steps, onValueChangeFinished = { onChange(v) })
    }
}

@Composable
fun TextRow(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true)
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun pct(v: Float) = "${(v * 100).roundToInt()}%"

// ---------------- sections ----------------

@Composable
private fun AppearanceSection(cfg: AppSettings) {
    val up = rememberUpdater()
    val ctx = LocalContext.current
    val pickWp = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { copyImageToFiles(ctx, it, "wallpaper") }?.let { p -> up { s -> s.copy(wallpaperPath = p) } }
    }
    SettingGroup("ภาษา / Language") {
        ChoiceRow(
            "ภาษา / Language",
            listOf("system" to "ตามเครื่อง / System", "th" to "ไทย", "en" to "English"),
            cfg.language,
        ) { v -> up { it.copy(language = v) } }
    }
    SettingGroup("ธีมสี") {
        ChoiceRow(
            "โหมด",
            listOf("dark" to "มืด", "light" to "สว่าง", "headlights" to "ตามไฟหน้ารถ", "sun" to "ตามพระอาทิตย์ขึ้น/ตก", "auto" to "ตามเวลาที่ตั้ง", "system" to "ตามระบบรถ"),
            cfg.themeMode
        ) { v -> up { it.copy(themeMode = v) } }
        if (cfg.themeMode == "headlights") Hint("เปิดไฟหน้า → ธีมมืด, ปิดไฟหน้า → ธีมสว่าง · ถ้าอ่านไฟหน้าไม่ได้จะใช้เวลาพระอาทิตย์แทน · หน่วงเวลาปรับได้ในหมวด หน้าจอ & ไฟหน้า")
        if (cfg.themeMode == "auto" || cfg.themeMode == "headlights") {
            SliderRow("เริ่มโหมดสว่างเวลา", cfg.autoLightFrom.toFloat(), 0f..23f, 22, { "${it.roundToInt()}:00 น." }) { v ->
                up { it.copy(autoLightFrom = v.roundToInt()) }
            }
            SliderRow("เริ่มโหมดมืดเวลา", cfg.autoDarkFrom.toFloat(), 0f..23f, 22, { "${it.roundToInt()}:00 น." }) { v ->
                up { it.copy(autoDarkFrom = v.roundToInt()) }
            }
        }
        Text("ชุดสี (ไล่เฉดสองสี)", style = MaterialTheme.typography.bodyLarge)
        AccentPicker(cfg.accent, cfg.accent2) { a, b -> up { it.copy(accent = a, accent2 = b) } }
        SwitchRow("พื้นกริดแบบโชว์รูม", cfg.showGrid) { v -> up { it.copy(showGrid = v) } }
    }
    SettingGroup("การ์ด & เลย์เอาต์") {
        SliderRow("ความทึบของการ์ด", cfg.cardOpacity, 0.15f..1f, 0, ::pct) { v -> up { it.copy(cardOpacity = v) } }
        SliderRow("ความโค้งมุม", cfg.cornerRadius.toFloat(), 0f..40f, 0, { "${it.roundToInt()} dp" }) { v ->
            up { it.copy(cornerRadius = v.roundToInt()) }
        }
        ChoiceRow(
            "เลย์เอาต์หน้าหลัก",
            listOf("auto" to "อัตโนมัติ (ตามการหมุนจอ)", "landscape" to "แนวนอน", "portrait" to "แนวตั้ง"),
            cfg.layoutMode
        ) { v -> up { it.copy(layoutMode = v) } }
        SwitchRow("ซ่อนแถบระบบ (เต็มจอ)", cfg.immersive) { v -> up { it.copy(immersive = v) } }
        SwitchRow("หน้าวิดเจ็ต (ปัดซ้ายจากหน้าหลัก)", cfg.widgetsPage) { v -> up { it.copy(widgetsPage = v) } }
    }
    SettingGroup("นาฬิกา") {
        SwitchRow("แบบ 24 ชั่วโมง", cfg.use24h) { v -> up { it.copy(use24h = v) } }
        SwitchRow("แสดงวินาที", cfg.showSeconds) { v -> up { it.copy(showSeconds = v) } }
    }
    SettingGroup("วอลเปเปอร์") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button({ pickWp.launch("image/*") }) { Text("เลือกรูปวอลเปเปอร์") }
            if (cfg.wallpaperPath.isNotBlank()) OutlinedButton({ up { it.copy(wallpaperPath = "") } }) { Text("ใช้พื้นหลังเริ่มต้น") }
        }
        SliderRow("ความมืดทับรูป", cfg.wallpaperDim, 0f..0.9f, 0, ::pct) { v -> up { it.copy(wallpaperDim = v) } }
    }
}

@Composable
private fun AccentPicker(current: Long, current2: Long, onPick: (Long, Long) -> Unit) {
    var hex by remember(current) { mutableStateOf(String.format("%06X", current and 0xFFFFFFL)) }
    var hex2 by remember(current2) { mutableStateOf(String.format("%06X", current2 and 0xFFFFFFL)) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ACCENTS.forEach { (a, b) ->
            val sel = a == current && b == current2
            Box(
                Modifier.size(46.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(b), Color(a))))
                    .border(
                        if (sel) 3.dp else 1.dp,
                        if (sel) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                        CircleShape
                    )
                    .clickable { onPick(a, b) },
                contentAlignment = Alignment.Center,
            ) { if (sel) Icon(Icons.Rounded.Check, null, tint = Color.Black.copy(alpha = 0.7f)) }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            hex, { hex = it.filter { ch -> ch.isLetterOrDigit() }.take(6).uppercase() }, Modifier.width(150.dp),
            label = { Text("สีหลัก") }, prefix = { Text("#") }, singleLine = true,
        )
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            hex2, { hex2 = it.filter { ch -> ch.isLetterOrDigit() }.take(6).uppercase() }, Modifier.width(150.dp),
            label = { Text("สีรอง") }, prefix = { Text("#") }, singleLine = true,
        )
        Spacer(Modifier.width(10.dp))
        OutlinedButton({
            val a = hex.toLongOrNull(16)
            val b = hex2.toLongOrNull(16)
            if (a != null && b != null) onPick(0xFF000000 or a, 0xFF000000 or b)
        }) { Text("ใช้สีนี้") }
    }
}

@Composable
private fun CarSection(cfg: AppSettings) {
    val up = rememberUpdater()
    val ctx = LocalContext.current
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { copyImageToFiles(ctx, it, "car") }?.let { p -> up { s -> s.copy(carImagePath = p) } }
    }
    val img = rememberImageFile(cfg.carImagePath, 1000)
    SettingGroup("ข้อมูลรถ") {
        TextRow("ชื่อรถ", cfg.carName) { v -> up { it.copy(carName = v) } }
        TextRow("ทะเบียนรถ", cfg.plate) { v -> up { it.copy(plate = v) } }
        SwitchRow("พวงมาลัยขวา (RHD)", cfg.rightHandDrive, "ใช้กำหนดตำแหน่งที่นั่งผู้ขับในภาพเตือนเข็มขัด") { v ->
            up { it.copy(rightHandDrive = v) }
        }
    }
    SettingGroup("รูปรถบนหน้าหลัก") {
        Box(
            Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center,
        ) {
            if (img != null) Image(img, null, Modifier.fillMaxSize().padding(8.dp), contentScale = ContentScale.Fit)
            else GenericCarSide(Modifier.fillMaxWidth(0.8f).aspectRatio(2.3f))
        }
        Hint("แนะนำรูป PNG ที่ตัดพื้นหลังออกแล้ว (พื้นโปร่งใส) ถ่ายด้านข้างหรือมุม 3/4 จะดูสวยที่สุด")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button({ pick.launch("image/*") }) { Text("เลือกรูปรถ") }
            if (cfg.carImagePath.isNotBlank()) OutlinedButton({ up { it.copy(carImagePath = "") } }) { Text("ใช้รูปเริ่มต้น") }
        }
    }
}

@Composable
private fun UnitSection(cfg: AppSettings) {
    val up = rememberUpdater()
    val pu = cfg.pressureUnit
    SettingGroup("หน่วย") {
        ChoiceRow("ความเร็ว / ระยะทาง", listOf("kmh" to "km/h · km", "mph" to "mph · mi"), cfg.speedUnit) { v -> up { it.copy(speedUnit = v) } }
        ChoiceRow("แรงดันลมยาง", listOf("bar" to "bar", "psi" to "psi", "kpa" to "kPa"), pu) { v -> up { it.copy(pressureUnit = v) } }
        ChoiceRow("อุณหภูมิ", listOf("c" to "°C", "f" to "°F"), cfg.tempUnit) { v -> up { it.copy(tempUnit = v) } }
    }
    SettingGroup("เกณฑ์ลมยาง") {
        SliderRow("ต่ำกว่านี้ = ลมอ่อน", cfg.tireLowBar, 1.5f..3.0f, 29, { "${pressureText(it, pu)} ${pressureUnit(pu)}" }) { v ->
            up { it.copy(tireLowBar = v) }
        }
        SliderRow("สูงกว่านี้ = ลมสูงเกิน", cfg.tireHighBar, 2.4f..4.0f, 31, { "${pressureText(it, pu)} ${pressureUnit(pu)}" }) { v ->
            up { it.copy(tireHighBar = v) }
        }
        SliderRow("ยางร้อนเกิน", cfg.tireHotC, 50f..110f, 59, { tempText(it, cfg.tempUnit) }) { v -> up { it.copy(tireHotC = v) } }
        Hint("ตั้งตามค่าที่ระบุบนสติกเกอร์ขอบประตูฝั่งคนขับของรถคุณ")
    }
    SettingGroup("จับเวลาอัตราเร่ง") {
        ChoiceRow(
            "เป้าหมายความเร็ว",
            listOf("60" to "0-60", "80" to "0-80", "100" to "0-100", "120" to "0-120"),
            cfg.accelTarget.toString()
        ) { v -> up { it.copy(accelTarget = v.toInt()) } }
    }
}

@Composable
private fun AlertSection(cfg: AppSettings) {
    val up = rememberUpdater()
    val ctx = LocalContext.current
    var canOverlay by remember { mutableStateOf(Settings.canDrawOverlays(ctx)) }
    LifecycleResumeEffect(Unit) {
        canOverlay = Settings.canDrawOverlays(ctx)
        onPauseOrDispose { }
    }
    SettingGroup("ประตูปิดไม่สนิท") {
        SwitchRow("เตือนเมื่อประตู / ฝาท้าย / ฝากระโปรงเปิด", cfg.doorAlert) { v -> up { it.copy(doorAlert = v) } }
        SliderRow("เตือนระดับวิกฤต (มีเสียง) เมื่อเร็วเกิน", cfg.doorAlertSpeed.toFloat(), 0f..30f, 29, { "${it.roundToInt()} km/h" }) { v ->
            up { it.copy(doorAlertSpeed = v.roundToInt()) }
        }
    }
    SettingGroup("เข็มขัดนิรภัย") {
        SwitchRow("เตือนเมื่อไม่คาดเข็มขัด", cfg.beltAlert) { v -> up { it.copy(beltAlert = v) } }
        SliderRow("เตือนระดับวิกฤต (มีเสียง) เมื่อเร็วเกิน", cfg.beltAlertSpeed.toFloat(), 0f..40f, 39, { "${it.roundToInt()} km/h" }) { v ->
            up { it.copy(beltAlertSpeed = v.roundToInt()) }
        }
    }
    SettingGroup("ลมยาง") {
        SwitchRow("เตือนลมยางผิดปกติ / ยางร้อน", cfg.tireAlert) { v -> up { it.copy(tireAlert = v) } }
    }
    SettingGroup("เสียง & การแสดงผล") {
        SwitchRow("เสียงเตือนเมื่อวิกฤต", cfg.alertSound) { v -> up { it.copy(alertSound = v) } }
        SwitchRow(
            "แสดงแจ้งเตือนทับแอปอื่น (เช่น ขณะเปิดแผนที่)", cfg.overlayAlerts,
            if (canOverlay) "ได้รับสิทธิ์ ‘แสดงทับแอปอื่น’ แล้ว" else "ยังไม่ได้รับสิทธิ์ ‘แสดงทับแอปอื่น’"
        ) { v -> up { it.copy(overlayAlerts = v) } }
        if (!canOverlay) {
            Button({
                runCatching {
                    ctx.startActivity(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }.onFailure { ctx.toast("เปิดหน้าขอสิทธิ์ไม่ได้ ลองตั้งค่าที่ ตั้งค่าระบบ > แอป") }
            }) { Text("ขอสิทธิ์แสดงทับแอปอื่น") }
        }
        Hint("สิทธิ์นี้ยังจำเป็นสำหรับการเด้งกล้องไฟเลี้ยวขณะใช้แอปอื่นอยู่")
    }
}

@Composable
private fun CameraSection(cfg: AppSettings) {
    val up = rememberUpdater()
    val ctx = LocalContext.current
    val cams = remember { listCameras(ctx) }
    val names = remember(cfg.cameraNames) { parseCameraNames(cfg.cameraNames) }
    var pickFor by remember { mutableStateOf<String?>(null) }
    val camOptions = listOf("" to "อัตโนมัติ") + cams.map { it.id to (names[it.id] ?: "กล้อง ${it.id}") }

    SettingGroup("กล้องเมื่อเปิดไฟเลี้ยว") {
        SwitchRow("แสดงกล้องอัตโนมัติเมื่อเปิดไฟเลี้ยว", cfg.turnCamEnabled) { v -> up { it.copy(turnCamEnabled = v) } }
        ChoiceRow(
            "วิธีแสดง",
            listOf("internal" to "หน้าต่างกล้องของ Launcher", "app" to "เปิดแอปกล้องของรถ"),
            cfg.turnCamMode
        ) { v -> up { it.copy(turnCamMode = v) } }
        if (cfg.turnCamMode == "app") {
            AppTargetRow("แอป / Activity ที่จะเปิด", cfg.turnCamAppPkg, { pickFor = "turn" }) { v -> up { it.copy(turnCamAppPkg = v) } }
        } else {
            ChoiceRow("กล้องฝั่งซ้าย", camOptions, cfg.leftCamId) { v -> up { it.copy(leftCamId = v) } }
            ChoiceRow("กล้องฝั่งขวา", camOptions, cfg.rightCamId) { v -> up { it.copy(rightCamId = v) } }
            SliderRow("ขนาดหน้าต่างกล้อง", cfg.turnCamSize, 0.25f..0.7f, 0, { "${pct(it)} ของจอ" }) { v -> up { it.copy(turnCamSize = v) } }
        }
        SwitchRow("แสดงเมื่อเปิดไฟฉุกเฉินด้วย", cfg.turnCamOnHazard) { v -> up { it.copy(turnCamOnHazard = v) } }
        SwitchRow("เส้นช่วยกะระยะบนหน้ากล้อง 360", cfg.camGuides, "เส้นอ้างอิงคร่าวๆ ไม่ใช่ระยะจริง") { v -> up { it.copy(camGuides = v) } }
        SliderRow("ค้างภาพหลังไฟเลี้ยวดับ", cfg.turnCamHoldSec.toFloat(), 0f..10f, 9, { "${it.roundToInt()} วินาที" }) { v ->
            up { it.copy(turnCamHoldSec = v.roundToInt()) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton({ TurnCameraActivity.show(ctx, TurnSignal.LEFT) }) { Text("ทดสอบฝั่งซ้าย") }
            OutlinedButton({ TurnCameraActivity.show(ctx, TurnSignal.RIGHT) }) { Text("ทดสอบฝั่งขวา") }
        }
    }
    SettingGroup("กล้อง 360° ของรถ") {
        AppTargetRow("แอป 360° (package หรือ package/.Activity)", cfg.panoramaPkg, { pickFor = "pano" }) { v ->
            up { it.copy(panoramaPkg = v) }
        }
        TextRow("ตั้งชื่อกล้อง เช่น 0=หน้า, 1=หลัง, 2=ซ้าย, 3=ขวา", cfg.cameraNames) { v -> up { it.copy(cameraNames = v) } }
        Hint("พบกล้อง ${cams.size} ตัว: " + cams.joinToString { "${it.id} (${it.facing})" }.ifBlank { "-" })
    }
    pickFor?.let { which ->
        AppPickerDialog(onDismiss = { pickFor = null }, includeActions = false) { key ->
            up { if (which == "pano") it.copy(panoramaPkg = key) else it.copy(turnCamAppPkg = key) }
            pickFor = null
        }
    }
}

@Composable
private fun AppTargetRow(label: String, value: String, onPick: () -> Unit, onChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(value, onChange, Modifier.weight(1f), label = { Text(label) }, singleLine = true)
        Spacer(Modifier.width(10.dp))
        OutlinedButton(onPick) { Text("เลือกแอป") }
    }
}

@Composable
private fun ShortcutSection(cfg: AppSettings) {
    val up = rememberUpdater()
    val ctx = LocalContext.current
    val app = ctx.app
    val apps by app.apps.apps.collectAsStateWithLifecycle()
    var picking by remember { mutableStateOf(false) }
    SettingGroup("Dock ด้านล่าง (สูงสุด 8)") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            cfg.dock.forEach { key ->
                ShortcutTile(key, apps, true, onClick = {}, onRemove = { up { s -> s.copy(dock = s.dock - key) } })
            }
            if (cfg.dock.size < 8) AddTile { picking = true }
        }
    }
    SettingGroup("หน้าทางลัด") {
        Hint("กด ‘แก้ไข’ บนการ์ดทางลัดที่หน้าหลัก (หรือกดค้างที่ไอคอน) เพื่อเพิ่ม/ลบไอคอนและเพิ่มหน้า ปัดซ้าย-ขวาเพื่อเปลี่ยนหน้า")
        OutlinedButton({ app.shortcuts.reset() }) { Text("รีเซ็ตทางลัดเป็นค่าเริ่มต้น") }
    }
    BubbleGroup(cfg)
    ProjectionGroup(cfg)
    LinkShortcutGroup()
    SettingGroup("แอปที่ใช้ล่าสุด") {
        Hint("แสดงในหน้า ‘แอปทั้งหมด’ และในปุ่มลอย")
        OutlinedButton({ app.apps.clearRecents(); ctx.toast("ล้างแล้ว") }) { Text("ล้างรายการล่าสุด") }
    }
    if (picking) {
        AppPickerDialog(onDismiss = { picking = false }) { key ->
            up { s -> s.copy(dock = (s.dock + key).distinct().take(8)) }
            picking = false
        }
    }
}

@Composable
private fun DataSection(cfg: AppSettings, onOpenProbe: () -> Unit) {
    val up = rememberUpdater()
    val app = LocalContext.current.app
    val st by app.vehicle.state.collectAsStateWithLifecycle()
    var mapText by remember(cfg.bydMap) { mutableStateOf(cfg.bydMap) }
    SettingGroup("แหล่งข้อมูลรถ") {
        ChoiceRow(
            "อ่านข้อมูลจาก",
            listOf("sim" to "โหมดจำลอง (ทดสอบหน้าตา)", "byd" to "BYD Auto API (ในรถจริง)"),
            cfg.dataSource
        ) { v -> up { it.copy(dataSource = v) } }
        SliderRow("ความถี่อ่านข้อมูล", cfg.pollMs.toFloat(), 100f..2000f, 18, { "${it.roundToInt()} ms" }) { v ->
            up { it.copy(pollMs = v.roundToInt()) }
        }
        Text("สถานะ: ${st.source}", color = if (st.connected) StatusColors.ok else StatusColors.bad, fontWeight = FontWeight.SemiBold)
        Button(onOpenProbe) {
            Icon(Icons.Rounded.BugReport, null)
            Spacer(Modifier.width(8.dp))
            Text("สแกน BYD API ในรถคันนี้")
        }
        Hint("ใช้เมนูสแกนเพื่อดูชื่อคลาส เมธอด ค่าคงที่ และค่าที่อ่านได้จริง แล้วนำมาแก้ mapping ด้านล่าง")
    }
    SettingGroup("BYD API Mapping (ขั้นสูง)") {
        Hint("1 บรรทัด = 1 ค่า · key = คลาส#เมธอด(อาร์กิวเมนต์) | scale= | offset= | true= | map=")
        OutlinedTextField(
            mapText, { mapText = it }, Modifier.fillMaxWidth().heightIn(min = 280.dp),
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button({ up { it.copy(bydMap = mapText) } }, enabled = mapText != cfg.bydMap) { Text("บันทึก mapping") }
            OutlinedButton({
                mapText = BydApiMap.DEFAULT
                up { it.copy(bydMap = BydApiMap.DEFAULT) }
            }) { Text("คืนค่าเริ่มต้น") }
        }
    }
}

@Composable
private fun DemoSection(cfg: AppSettings) {
    val app = LocalContext.current.app
    val d by app.vehicle.demo.collectAsStateWithLifecycle()
    fun set(f: (DemoControls) -> DemoControls) {
        app.vehicle.demo.value = f(app.vehicle.demo.value)
    }
    if (cfg.dataSource != "sim") {
        GlassCard(Modifier.fillMaxWidth()) {
            Text("โหมดทดสอบใช้ได้เมื่อเลือกแหล่งข้อมูลเป็น ‘โหมดจำลอง’ (ตั้งค่า > ข้อมูลรถ)", color = StatusColors.warn)
        }
    }
    SettingGroup("จำลองการขับขี่") {
        SwitchRow("รถกำลังวิ่ง", d.driving) { v -> set { it.copy(driving = v) } }
        SwitchRow("เข้าเกียร์ R (ทดสอบการปล่อยกล้อง)", d.reverse) { v -> set { it.copy(reverse = v) } }
        SwitchRow("เปิดไฟหน้า (ทดสอบธีม/ความสว่างตามไฟหน้า)", d.headlights) { v -> set { it.copy(headlights = v) } }
        ChoiceRow(
            "ไฟเลี้ยว",
            listOf("NONE" to "ปิด", "LEFT" to "ซ้าย", "RIGHT" to "ขวา", "HAZARD" to "ฉุกเฉิน"),
            d.turn.name
        ) { v -> set { it.copy(turn = TurnSignal.valueOf(v)) } }
        SliderRow(
            "ปรับลมยางจำลอง", d.tireOffsetBar, -1f..1f, 0,
            { (if (it >= 0f) "+" else "") + fmt1(it) + " bar" }
        ) { v -> set { it.copy(tireOffsetBar = v) } }
    }
    SettingGroup("จำลองประตู") {
        SwitchRow("ประตูหน้าซ้าย", d.doors.fl) { v -> set { it.copy(doors = it.doors.copy(fl = v)) } }
        SwitchRow("ประตูหน้าขวา", d.doors.fr) { v -> set { it.copy(doors = it.doors.copy(fr = v)) } }
        SwitchRow("ประตูหลังซ้าย", d.doors.rl) { v -> set { it.copy(doors = it.doors.copy(rl = v)) } }
        SwitchRow("ประตูหลังขวา", d.doors.rr) { v -> set { it.copy(doors = it.doors.copy(rr = v)) } }
        SwitchRow("ฝาท้าย", d.doors.trunk) { v -> set { it.copy(doors = it.doors.copy(trunk = v)) } }
        SwitchRow("ฝากระโปรงหน้า", d.doors.hood) { v -> set { it.copy(doors = it.doors.copy(hood = v)) } }
    }
    SettingGroup("จำลองเข็มขัดนิรภัย") {
        SwitchRow("ผู้ขับคาดเข็มขัด", d.driverBelt) { v -> set { it.copy(driverBelt = v) } }
        SwitchRow("มีผู้โดยสารด้านหน้า", d.passengerOccupied) { v -> set { it.copy(passengerOccupied = v) } }
        SwitchRow("ผู้โดยสารคาดเข็มขัด", d.passengerBelt) { v -> set { it.copy(passengerBelt = v) } }
    }
}

@Composable
private fun SystemSection() {
    val ctx = LocalContext.current
    val app = ctx.app
    val cm = remember { ctx.getSystemService(ClipboardManager::class.java) }
    var confirmReset by remember { mutableStateOf(false) }
    SettingGroup("ตั้งเป็นหน้าหลัก (Launcher) ของรถ") {
        Hint("เลือก Dolphin Launcher เป็นแอปหน้าแรก แล้วกดปุ่ม Home จะกลับมาที่หน้านี้เสมอ")
        Button({
            runCatching { ctx.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                .onFailure { ctx.toast("ระบบรถไม่มีหน้าเลือก Launcher — กดปุ่ม Home แล้วเลือกแอปนี้แทน") }
        }) { Text("เปิดหน้าตั้งค่าแอปหน้าแรก") }
    }
    SettingGroup("สำรอง / กู้คืนการตั้งค่า") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button({
                cm.setPrimaryClip(ClipData.newPlainText("dolphin-settings", app.settings.exportJson()))
                ctx.toast("คัดลอกการตั้งค่า (JSON) แล้ว")
            }) { Text("ส่งออก (คัดลอก)") }
            OutlinedButton({
                val t = cm.primaryClip?.getItemAt(0)?.coerceToText(ctx)?.toString().orEmpty()
                ctx.toast(if (app.settings.importJson(t)) "นำเข้าสำเร็จ" else "ข้อมูลในคลิปบอร์ดไม่ใช่ JSON การตั้งค่า")
            }) { Text("นำเข้าจากคลิปบอร์ด") }
        }
    }
    SettingGroup("รีเซ็ต") {
        OutlinedButton({
            if (confirmReset) {
                app.settings.reset(); confirmReset = false; ctx.toast("รีเซ็ตแล้ว")
            } else {
                confirmReset = true
            }
        }) { Text(if (confirmReset) "กดอีกครั้งเพื่อยืนยัน" else "รีเซ็ตการตั้งค่าทั้งหมด") }
    }
    SettingGroup("ระบบป้องกันค้าง") {
        val up = rememberUpdater()
        val cfg by app.settings.flow.collectAsStateWithLifecycle()
        Hint("เมื่อพบปัญหา Launcher จะหยุดอ่านข้อมูลรถและงานเบื้องหลังทันที แล้วแสดงสาเหตุ (ระบบของรถทำงานต่อตามปกติ)")
        SliderRow("ตรวจจอค้าง: หยุดเมื่อไม่ตอบสนองเกิน", cfg.watchdogSec.toFloat(), 0f..15f, 14, { if (it < 1f) "ปิด" else "${it.roundToInt()} วินาที" }) { v ->
            up { it.copy(watchdogSec = v.roundToInt()) }
        }
        SwitchRow("โหมดเข้มงวด", cfg.guardStrict, "ลดจำนวนครั้งที่ยอมให้ทำซ้ำลง ~1/3 — เหมาะช่วงทดลองใช้ในรถ") { v -> up { it.copy(guardStrict = v) } }
        val hist = remember { th.dolphin.launcher.SafetyGuard.history(ctx) }
        val df = remember { java.text.SimpleDateFormat("d MMM HH:mm", java.util.th.dolphin.launcher.i18n.I18n.dateLocale()) }
        Text("ประวัติการหยุดทำงาน (${hist.size})", style = MaterialTheme.typography.titleSmall)
        if (hist.isEmpty()) Hint("ยังไม่เคยเกิดปัญหา")
        hist.take(5).forEach { t -> Hint("• ${df.format(java.util.Date(t.time))}  ${t.reason}") }
        OutlinedButton({
            th.dolphin.launcher.SafetyGuard.trip("ทดสอบระบบป้องกัน", "ผู้ใช้กดทดสอบจากหน้าตั้งค่า — กด ‘เริ่มทำงานใหม่’ เพื่อกลับมาใช้งาน")
        }) { Text("ทดสอบ: จำลองการหยุดทำงาน") }
    }
    SettingGroup("เกี่ยวกับ") {
        Text("Dolphin Launcher ${BuildConfig.VERSION_NAME}")
        Hint("Launcher สำหรับหัวเครื่อง BYD DiLink · ไม่ใช่ผลิตภัณฑ์ทางการของ BYD")
    }
}

@Composable
private fun BubbleGroup(cfg: AppSettings) {
    val up = rememberUpdater()
    val ctx = LocalContext.current
    var canOverlay by remember { mutableStateOf(Settings.canDrawOverlays(ctx)) }
    LifecycleResumeEffect(Unit) {
        canOverlay = Settings.canDrawOverlays(ctx)
        onPauseOrDispose { }
    }
    SettingGroup("ปุ่มลอยเรียกแอป") {
        SwitchRow(
            "แสดงปุ่มลอยทับแอปอื่น", cfg.bubbleEnabled,
            "ลากย้ายได้ · แตะเพื่อเปิดเมนู หน้าหลัก + Dock + แอปล่าสุด · ซ่อนเองเมื่ออยู่หน้า Launcher"
        ) { v -> up { it.copy(bubbleEnabled = v) } }
        if (!canOverlay) {
            Button({
                runCatching {
                    ctx.startActivity(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }) { Text("ขอสิทธิ์แสดงทับแอปอื่น") }
        }
        OutlinedButton({ up { it.copy(bubbleX = 24, bubbleY = 260) } }) { Text("รีเซ็ตตำแหน่งปุ่ม") }
    }
}

@Composable
private fun ProjectionGroup(cfg: AppSettings) {
    val up = rememberUpdater()
    val app = LocalContext.current.app
    val apps by app.apps.apps.collectAsStateWithLifecycle()
    val detected = remember(apps) { app.apps.projectionApps() }
    var picking by remember { mutableStateOf(false) }
    SettingGroup("Android Auto / มิเรอร์มือถือ") {
        Hint(
            "Launcher เพิ่มความสามารถ Android Auto ให้หัวเครื่องเองไม่ได้ แต่เรียกแอปฉายภาพที่มีในเครื่องได้ " +
                "(เช่น แอปมิเรอร์ของรถ หรือ HeadUnit Reloaded) ผ่านปุ่ม ‘Android Auto / มิเรอร์’"
        )
        if (detected.isNotEmpty()) {
            ChoiceRow(
                "พบในเครื่อง",
                listOf("" to "อัตโนมัติ") + detected.map { it.key to it.label },
                cfg.projectionPkg,
            ) { v -> up { it.copy(projectionPkg = v) } }
        } else {
            Text("ไม่พบแอปมิเรอร์ที่รู้จัก — เลือกแอปเองได้", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AppTargetRow("แอปที่จะเปิด (package หรือ package/.Activity)", cfg.projectionPkg, { picking = true }) { v ->
            up { it.copy(projectionPkg = v) }
        }
    }
    if (picking) {
        AppPickerDialog(onDismiss = { picking = false }, includeActions = false) { key ->
            up { it.copy(projectionPkg = key) }
            picking = false
        }
    }
}

private val LINK_PRESETS = listOf(
    "นำทางกลับบ้าน" to "google.navigation:q=บ้าน",
    "ปั๊มชาร์จใกล้ฉัน" to "geo:0,0?q=EV+charging+station",
    "ค้นหาร้านกาแฟ" to "geo:0,0?q=coffee",
    "เว็บไซต์" to "https://",
)

@Composable
private fun LinkShortcutGroup() {
    val ctx = LocalContext.current
    val app = ctx.app
    var label by remember { mutableStateOf("") }
    var uri by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    SettingGroup("ทางลัดแบบลิงก์ (นำทาง / แผนที่ / เว็บ / intent)") {
        Hint("ใช้เปิดหน้าเฉพาะของแอปได้โดยตรง เช่น นำทางไปที่อยู่ที่ตั้งไว้ ต้องมีแอปที่รองรับลิงก์นั้นในเครื่อง")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LINK_PRESETS.forEach { (l, u) ->
                FilterChip(selected = false, onClick = { label = l; uri = u; error = null }, label = { Text(l) })
            }
        }
        OutlinedTextField(label, { label = it; error = null }, Modifier.fillMaxWidth(), label = { Text("ชื่อปุ่ม") }, singleLine = true)
        OutlinedTextField(
            uri, { uri = it; error = null }, Modifier.fillMaxWidth(),
            label = { Text("ลิงก์ เช่น google.navigation:q=Central+World") }, singleLine = true,
        )
        error?.let { Text(it, color = StatusColors.bad, style = MaterialTheme.typography.bodySmall) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton({
                if (uri.isBlank()) error = "ใส่ลิงก์ก่อน"
                else if (!app.apps.openUri(uri.trim())) error = "ไม่มีแอปที่เปิดลิงก์นี้ได้"
            }) { Text("ลองเปิด") }
            Button({
                when {
                    label.isBlank() -> error = "ใส่ชื่อปุ่มก่อน"
                    uri.isBlank() || uri.trim() == "https://" -> error = "ใส่ลิงก์ก่อน"
                    else -> {
                        app.shortcuts.add(0, ShortcutKeys.uri(label.trim(), uri.trim()))
                        ctx.toast("เพิ่ม ‘${label.trim()}’ ในทางลัดหน้า 1")
                        label = ""; uri = ""
                    }
                }
            }) { Text("เพิ่มลงทางลัด") }
        }
    }
}

@Composable
private fun MediaPhoneSection(cfg: AppSettings) {
    val up = rememberUpdater()
    val ctx = LocalContext.current
    val app = ctx.app
    val apps by app.apps.apps.collectAsStateWithLifecycle()
    val access by app.media.hasAccess.collectAsStateWithLifecycle()
    val bt by app.bluetooth.state.collectAsStateWithLifecycle()
    var pickFor by remember { mutableStateOf<String?>(null) }
    LifecycleResumeEffect(Unit) {
        app.media.refresh()
        onPauseOrDispose { }
    }

    SettingGroup("เพลงที่กำลังเล่น (ทุกแอป รวมวิทยุ)") {
        Text(
            if (access) "ได้รับสิทธิ์แล้ว — การ์ดเพลงบนหน้าหลักควบคุมได้" else "ยังไม่ได้รับสิทธิ์ ‘เข้าถึงการแจ้งเตือน’",
            color = if (access) StatusColors.ok else StatusColors.warn,
        )
        Hint("ใช้อ่านชื่อเพลง/สถานี และปุ่มเล่น-หยุด-ถัดไป จาก MediaSession ของแอปที่กำลังเล่น · แอปไม่อ่านหรือเก็บข้อความแจ้งเตือน")
        if (!access) {
            Button({
                runCatching {
                    ctx.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }.onFailure { ctx.toast("เปิดหน้าตั้งค่าไม่ได้ ลองที่ ตั้งค่าระบบ > แอป > สิทธิ์พิเศษ") }
            }) { Text("อนุญาตการเข้าถึงการแจ้งเตือน") }
        }
    }

    KindRow("แอปวิทยุ", AppKind.RADIO, cfg.radioPkg, { pickFor = "radio" }) { v -> up { it.copy(radioPkg = v) } }
    KindRow("แอปเพลงบลูทูธ", AppKind.BT_MUSIC, cfg.btMusicPkg, { pickFor = "bt" }) { v -> up { it.copy(btMusicPkg = v) } }
    KindRow("แอปเพลง", AppKind.MUSIC, cfg.musicPkg, { pickFor = "music" }) { v -> up { it.copy(musicPkg = v) } }

    SettingGroup("บลูทูธ") {
        Text(
            when {
                !bt.supported -> "อุปกรณ์นี้ไม่มีบลูทูธ"
                !bt.enabled -> "บลูทูธปิดอยู่"
                bt.connected.isEmpty() -> "เปิดอยู่ · ยังไม่มีอุปกรณ์เชื่อมต่อ"
                else -> "เชื่อมต่อ: " + bt.connected.joinToString { d -> d.name + (d.battery?.let { " ($it%)" } ?: "") }
            }
        )
        Hint("แบตมือถือจะแสดงเมื่อมือถือรายงานค่าผ่านบลูทูธ (มือถือส่วนใหญ่รองรับ)")
        OutlinedButton({ if (!app.apps.openBluetoothSettings()) ctx.toast("เปิดหน้าตั้งค่าบลูทูธไม่ได้") }) { Text("เปิดตั้งค่าบลูทูธ") }
    }

    KindRow("แอปโทรศัพท์", AppKind.PHONE, cfg.phonePkg, { pickFor = "phone" }) { v -> up { it.copy(phonePkg = v) } }

    SettingGroup("โทรด่วน") {
        Hint("บรรทัดละ 1 คน รูปแบบ ชื่อ=เบอร์ · แสดง 3 รายการแรกบนการ์ดโทรศัพท์ · กดแล้วจะเปิดหน้าหมุนเบอร์ให้กดโทรเอง")
        OutlinedTextField(
            cfg.favorites, { v -> up { it.copy(favorites = v) } }, Modifier.fillMaxWidth().heightIn(min = 110.dp),
            label = { Text("เช่น แม่=0812345678") },
        )
    }

    pickFor?.let { which ->
        AppPickerDialog(onDismiss = { pickFor = null }, includeActions = false) { key ->
            up {
                when (which) {
                    "radio" -> it.copy(radioPkg = key)
                    "bt" -> it.copy(btMusicPkg = key)
                    "music" -> it.copy(musicPkg = key)
                    else -> it.copy(phonePkg = key)
                }
            }
            pickFor = null
        }
    }
}

@Composable
private fun KindRow(title: String, kind: AppKind, value: String, onPick: () -> Unit, onChange: (String) -> Unit) {
    val app = LocalContext.current.app
    val apps by app.apps.apps.collectAsStateWithLifecycle()
    val detected = remember(apps) { app.apps.detect(kind) }
    SettingGroup(title) {
        if (detected.isNotEmpty()) {
            ChoiceRow("พบในเครื่อง", listOf("" to "อัตโนมัติ") + detected.take(6).map { it.key to it.label }, value, onChange)
        } else {
            Text("ไม่พบอัตโนมัติ — เลือกแอปเอง", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AppTargetRow("แอปที่จะเปิด", value, onPick, onChange)
    }
}

@Composable
private fun TripServiceSection(cfg: AppSettings) {
    val up = rememberUpdater()
    val ctx = LocalContext.current
    val app = ctx.app
    val st by app.vehicle.state.collectAsStateWithLifecycle()
    val service = th.dolphin.launcher.alerts.Maintenance.status(st, cfg)
    val df = remember { java.text.SimpleDateFormat("d MMM yyyy", java.util.th.dolphin.launcher.i18n.I18n.dateLocale()) }

    SettingGroup("แบตเตอรี่และค่าไฟ") {
        ChoiceRow(
            "ความจุแบตของรถ (ใช้คำนวณ kWh)",
            listOf("44.9" to "44.9 kWh (Standard)", "60.48" to "60.48 kWh (Extended)"),
            when (cfg.batteryKwh) { 44.9f -> "44.9"; 60.48f -> "60.48"; else -> "" },
        ) { v -> up { it.copy(batteryKwh = v.toFloat()) } }
        SliderRow("ปรับความจุเอง", cfg.batteryKwh, 30f..90f, 0, { String.format(java.util.Locale.US, "%.1f kWh", it) }) { v ->
            up { it.copy(batteryKwh = (v * 10).roundToInt() / 10f) }
        }
        SliderRow("ค่าไฟ", cfg.elecPrice, 0f..12f, 47, { String.format(java.util.Locale.US, "%.2f บาท/kWh", it) }) { v ->
            up { it.copy(elecPrice = (v * 4).roundToInt() / 4f) }
        }
        Hint("ตรวจความจุแบตจากเอกสารรถ · ค่าไฟบ้านหรือสถานีชาร์จที่ใช้บ่อย")
    }
    SettingGroup("ความเร็ว") {
        SliderRow("เตือนเมื่อเร็วเกิน", cfg.overspeedKmh.toFloat(), 0f..160f, 31, { if (it < 1f) "ปิด" else "${it.roundToInt()} km/h" }) { v ->
            up { it.copy(overspeedKmh = v.roundToInt()) }
        }
        ChoiceRow(
            "แหล่งความเร็ว",
            listOf("auto" to "อัตโนมัติ (ใช้ GPS เมื่ออ่านจากรถไม่ได้)", "car" to "จากรถ", "gps" to "จาก GPS"),
            cfg.speedSource,
        ) { v -> up { it.copy(speedSource = v) } }
    }
    SettingGroup("เตือนเข้าศูนย์บริการ") {
        SliderRow("ทุกๆ", cfg.serviceEveryKm.toFloat(), 5000f..40000f, 34, { "${it.roundToInt()} km" }) { v ->
            up { it.copy(serviceEveryKm = (v / 1000).roundToInt() * 1000) }
        }
        SliderRow("หรือทุกๆ", cfg.serviceEveryMonths.toFloat(), 3f..24f, 20, { "${it.roundToInt()} เดือน" }) { v ->
            up { it.copy(serviceEveryMonths = v.roundToInt()) }
        }
        Text(
            if (cfg.lastServiceDate == 0L) "ยังไม่ได้บันทึกการเข้าศูนย์ครั้งล่าสุด"
            else "เข้าศูนย์ล่าสุด ${df.format(java.util.Date(cfg.lastServiceDate))} ที่ ${cfg.lastServiceOdo.roundToInt()} km · ครั้งถัดไป ${service?.text ?: "-"}",
        )
        Button({
            val odo = st.odometerKm
            up { it.copy(lastServiceDate = System.currentTimeMillis(), lastServiceOdo = odo ?: it.lastServiceOdo) }
            ctx.toast(if (odo == null) "บันทึกวันที่แล้ว (ยังอ่านเลขไมล์จากรถไม่ได้)" else "บันทึกแล้วที่ ${odo.roundToInt()} km")
        }) { Text("บันทึกว่าเข้าศูนย์วันนี้") }
    }
    SettingGroup("สภาพอากาศ") {
        SwitchRow("แสดงสภาพอากาศ (Open-Meteo · ใช้ตำแหน่งและอินเทอร์เน็ต)", cfg.weatherEnabled) { v -> up { it.copy(weatherEnabled = v) } }
        Hint("ส่งเฉพาะพิกัดโดยประมาณ (ทศนิยม 3 ตำแหน่ง) ไปยัง api.open-meteo.com ทุก 30 นาที")
    }
}

@Composable
private fun CenterSection(cfg: AppSettings) {
    val up = rememberUpdater()
    val ctx = LocalContext.current
    val app = ctx.app
    val pickWidget = th.dolphin.launcher.ui.widgets.rememberWidgetAdder { id ->
        val old = app.settings.value.centerWidgetId
        if (old >= 0 && old != id) runCatching { app.widgets.host.deleteAppWidgetId(old) }
        up { it.copy(centerWidgetId = id, mapSource = "widget") }
    }
    SettingGroup("ส่วนกลางหน้าหลัก") {
        SliderRow("แสดงรูปรถ", cfg.carShowSec.toFloat(), 3f..60f, 56, { "${it.roundToInt()} วินาที" }) { v -> up { it.copy(carShowSec = v.roundToInt()) } }
        ChoiceRow(
            "จากนั้นเปลี่ยนเป็น",
            listOf("auto" to "อัตโนมัติ (มีเพลง → มัลติมีเดีย, ไม่มี → แผนที่)", "map" to "แผนที่", "media" to "มัลติมีเดีย", "car" to "ไม่เปลี่ยน (รูปรถตลอด)"),
            cfg.centerAfter,
        ) { v -> up { it.copy(centerAfter = v) } }
        Hint("กดปุ่ม รถ / แผนที่ / เพลง บนหน้าหลักเพื่อสลับเองได้ตลอด · กลับมาหน้าหลักจะเริ่มจากรูปรถใหม่")
    }
    SettingGroup("แผนที่") {
        ChoiceRow(
            "แหล่งแผนที่",
            listOf("osm" to "OpenStreetMap ในตัว (ไม่ต้องใช้ key)", "widget" to "วิดเจ็ตของแอปแผนที่"),
            cfg.mapSource,
        ) { v -> up { it.copy(mapSource = v) } }
        if (cfg.mapSource == "widget") {
            Text(if (cfg.centerWidgetId >= 0) "เลือกวิดเจ็ตไว้แล้ว" else "ยังไม่ได้เลือกวิดเจ็ต (จะใช้ OpenStreetMap แทน)")
            Button(pickWidget) { Text("เลือกวิดเจ็ตแผนที่") }
        } else {
            SliderRow("ระดับซูม", cfg.mapZoom, 10f..19f, 8, { "${it.roundToInt()}" }) { v -> up { it.copy(mapZoom = v.roundToInt().toFloat()) } }
            SwitchRow("แผนที่โทนมืดเมื่อใช้ธีมมืด", cfg.mapDarkTiles) { v -> up { it.copy(mapDarkTiles = v) } }
            Hint("แผนที่ © ผู้ร่วมพัฒนา OpenStreetMap · ใช้อินเทอร์เน็ตโหลดครั้งแรก แล้วเก็บแคชไว้ในเครื่อง · ไม่มีระบบนำทาง (กด ‘เปิดแอปแผนที่’ เพื่อนำทาง)")
        }
    }
    SettingGroup("หน้าวิดเจ็ต") {
        SwitchRow("เปิดหน้าวิดเจ็ต (ปัดซ้ายจากหน้าหลัก)", cfg.widgetsPage) { v -> up { it.copy(widgetsPage = v) } }
    }
}

@Composable
private fun DisplaySection(cfg: AppSettings) {
    val up = rememberUpdater()
    val ctx = LocalContext.current
    val app = ctx.app
    val st by app.vehicle.state.collectAsStateWithLifecycle()
    var canWrite by remember { mutableStateOf(Settings.System.canWrite(ctx)) }
    LifecycleResumeEffect(Unit) {
        canWrite = Settings.System.canWrite(ctx)
        onPauseOrDispose { }
    }
    SettingGroup("สถานะไฟหน้า") {
        Text(
            when (st.headlights) {
                true -> "เปิดอยู่"
                false -> "ปิดอยู่"
                null -> "อ่านค่าไม่ได้ — ตรวจ mapping บรรทัด headlights ในหมวด ข้อมูลรถ"
            },
            color = if (st.headlights == null) StatusColors.warn else MaterialTheme.colorScheme.onSurface,
        )
        SliderRow("หน่วงเวลาก่อนเปลี่ยน (กันกระพริบ)", cfg.lightDelaySec.toFloat(), 0f..15f, 14, { "${it.roundToInt()} วินาที" }) { v ->
            up { it.copy(lightDelaySec = v.roundToInt()) }
        }
        OutlinedButton({ up { it.copy(themeMode = "headlights") } }, enabled = cfg.themeMode != "headlights") {
            Text(if (cfg.themeMode == "headlights") "✓ ธีมเปลี่ยนตามไฟหน้าอยู่แล้ว" else "ให้ธีมมืด/สว่างเปลี่ยนตามไฟหน้า")
        }
    }
    SettingGroup("ความสว่างจออัตโนมัติ") {
        SwitchRow("ปรับความสว่างอัตโนมัติ", cfg.autoBrightness, if (canWrite) null else "ต้องอนุญาต ‘แก้ไขการตั้งค่าระบบ’ ก่อน") { v ->
            up { it.copy(autoBrightness = v) }
        }
        if (!canWrite) {
            Button({
                runCatching {
                    ctx.startActivity(
                        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }) { Text("อนุญาตแก้ไขการตั้งค่าระบบ") }
        }
        ChoiceRow(
            "เปลี่ยนตาม",
            listOf("headlights" to "ไฟหน้ารถ", "sun" to "พระอาทิตย์ขึ้น/ตก"),
            cfg.brightTrigger,
        ) { v -> up { it.copy(brightTrigger = v) } }
        SliderRow("ความสว่างกลางวัน (ไฟหน้าปิด)", cfg.brightDay.toFloat(), 20f..255f, 0, { "${(it / 255f * 100).roundToInt()}%" }) { v ->
            up { it.copy(brightDay = v.roundToInt()) }
        }
        SliderRow("ความสว่างกลางคืน (ไฟหน้าเปิด)", cfg.brightNight.toFloat(), 5f..255f, 0, { "${(it / 255f * 100).roundToInt()}%" }) { v ->
            up { it.copy(brightNight = v.roundToInt()) }
        }
        Hint("ถ้าระบบของรถปรับความสว่างเองด้วย ค่าที่ตั้งอาจถูกแทนที่ ให้ปิดความสว่างอัตโนมัติของรถก่อน")
    }
}
