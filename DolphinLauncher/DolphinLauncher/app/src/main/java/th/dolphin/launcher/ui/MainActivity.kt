package th.dolphin.launcher.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import th.dolphin.launcher.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import th.dolphin.launcher.CrashGuard
import th.dolphin.launcher.SafetyGuard
import th.dolphin.launcher.alerts.AlertEngine
import th.dolphin.launcher.alerts.AlertLevel
import th.dolphin.launcher.app
import th.dolphin.launcher.apps.QuickActions
import th.dolphin.launcher.service.VehicleMonitorService
import th.dolphin.launcher.ui.apps.AppsScreen
import th.dolphin.launcher.ui.audio.AudioScreen
import th.dolphin.launcher.ui.battery.BatteryScreen
import th.dolphin.launcher.ui.camera.CameraScreen
import th.dolphin.launcher.ui.components.AlertBannerStack
import th.dolphin.launcher.ui.components.LauncherBackground
import th.dolphin.launcher.ui.components.QuickPanel
import th.dolphin.launcher.ui.home.HomeScreen
import th.dolphin.launcher.ui.laps.LapScreen
import th.dolphin.launcher.ui.settings.ProbeScreen
import th.dolphin.launcher.ui.settings.SettingsScreen
import th.dolphin.launcher.ui.theme.LauncherTheme
import th.dolphin.launcher.ui.theme.LocalUi
import th.dolphin.launcher.ui.tires.TiresScreen
import th.dolphin.launcher.ui.trip.TripScreen
import th.dolphin.launcher.util.toast

/** หน้าหลัก 5 หน้า (แถบเมนูซ้าย) + หน้าย่อย */
enum class Screen { HOME, APPS, AUDIO, CAMERA, SETTINGS, TIRES, LAPS, PROBE, BATTERY, TRIP }

/** หน้าย่อยอยู่ใต้หน้าหลักใด (ใช้ไฮไลต์เมนูและปุ่มย้อนกลับ) */
fun Screen.root(): Screen = when (this) {
    Screen.TIRES, Screen.LAPS, Screen.BATTERY, Screen.TRIP -> Screen.HOME
    Screen.PROBE -> Screen.SETTINGS
    else -> this
}

class MainActivity : ComponentActivity() {
    private val nav = mutableStateOf(Screen.HOME)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ยังไม่ปลดล็อกการใช้เป็นหน้าหลัก → ส่งปุ่ม Home ไปหน้าจอเดิมของรถ
        if (intent?.hasCategory(Intent.CATEGORY_HOME) == true && !th.dolphin.launcher.HomeLock.isUnlocked(this) &&
            CrashGuard.openFallbackHome(this)
        ) {
            finish()
            return
        }
        // แอปเด้งซ้ำ → ปุ่ม Home พาไป Launcher เดิมของรถทันที (ไม่เสี่ยงเด้งซ้ำอีก)
        if (CrashGuard.inSafeMode(this) && intent?.hasCategory(Intent.CATEGORY_HOME) == true && CrashGuard.openFallbackHome(this)) {
            toast("Dolphin Launcher หยุดทำงานเพื่อความปลอดภัย · เปิดแอปจากไอคอนเพื่อดูสาเหตุ")
            finish()
            return
        }
        // หน้านี้ถูกสร้างใหม่ถี่ผิดปกติ (วนลูป) → ตัดการทำงาน
        SafetyGuard.allow("activity", 12, 30_000, "หน้า Launcher ถูกสร้างใหม่")
        // ใช้งานได้ปกติครบ 1 นาที → ล้างตัวนับการเด้ง
        lifecycleScope.launch {
            delay(60_000)
            if (!SafetyGuard.isTripped) CrashGuard.clear(this@MainActivity)
        }

        enableEdgeToEdge()
        applyActionExtra(intent)
        startMonitor()
        setContent {
            val cfg by app.settings.flow.collectAsStateWithLifecycle()
            val trip by SafetyGuard.tripped.collectAsStateWithLifecycle()
            LauncherTheme(cfg) {
                val dark = LocalUi.current.dark
                LaunchedEffect(dark, cfg.immersive, trip) {
                    val c = WindowCompat.getInsetsController(window, window.decorView)
                    c.isAppearanceLightStatusBars = !dark && trip == null
                    c.isAppearanceLightNavigationBars = !dark && trip == null
                    if (cfg.immersive) {
                        c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        c.hide(WindowInsetsCompat.Type.systemBars())
                    } else {
                        c.show(WindowInsetsCompat.Type.systemBars())
                    }
                }
                val t = trip
                if (t != null) {
                    ErrorScreen(t) { sim ->
                        if (sim) app.settings.update { it.copy(dataSource = "sim") }
                        CrashGuard.clear(this@MainActivity)
                        SafetyGuard.reset(this@MainActivity)
                        nav.value = Screen.HOME
                        startMonitor()
                    }
                } else {
                    LauncherRoot(nav)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // กดปุ่ม Home ระหว่างอยู่หน้าอื่น → กลับหน้าหลัก
        if (intent.hasCategory(Intent.CATEGORY_HOME)) nav.value = Screen.HOME
        applyActionExtra(intent)
    }

    /** เปิดหน้าตาม action ที่ส่งมาจากปุ่มลอย */
    private fun applyActionExtra(i: Intent?) {
        nav.value = screenFor(i?.getStringExtra(EXTRA_ACTION) ?: return) ?: return
    }

    companion object {
        const val EXTRA_ACTION = "launcher_action"

        fun screenFor(key: String): Screen? = when (key) {
            "action:home" -> Screen.HOME
            "action:apps", "action:search" -> Screen.APPS
            "action:audio" -> Screen.AUDIO
            "action:camera" -> Screen.CAMERA
            "action:settings" -> Screen.SETTINGS
            "action:tires" -> Screen.TIRES
            "action:laps" -> Screen.LAPS
            "action:battery" -> Screen.BATTERY
            "action:trip" -> Screen.TRIP
            else -> null
        }
    }

    override fun onStart() {
        super.onStart()
        app.widgets.startListening()
    }

    override fun onStop() {
        super.onStop()
        app.widgets.stopListening()
    }

    override fun onResume() {
        super.onResume()
        app.foreground.value = true
        if (!SafetyGuard.isTripped) {
            app.location.start()
            app.media.refresh()
            startMonitor()
        }
    }

    override fun onPause() {
        super.onPause()
        app.foreground.value = false
    }

    private fun startMonitor() {
        runCatching { ContextCompat.startForegroundService(this, Intent(this, VehicleMonitorService::class.java)) }
    }
}

@Composable
fun LauncherRoot(nav: MutableState<Screen>) {
    val ctx = LocalContext.current
    val app = ctx.app
    val cfg by app.settings.flow.collectAsStateWithLifecycle()
    val st by app.vehicle.state.collectAsStateWithLifecycle()
    val dark = LocalUi.current.dark
    var showQuick by remember { mutableStateOf(false) }
    val locPerm = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { app.location.start() }
    LaunchedEffect(Unit) {
        if (!app.location.hasPermission()) {
            locPerm.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
        } else app.location.start()
    }

    fun handle(key: String) {
        val screen = MainActivity.screenFor(key)
        when {
            screen != null -> nav.value = screen
            key == "action:quick" -> showQuick = true
            key == "action:theme" -> app.settings.update { it.copy(themeMode = if (dark) "light" else "dark") }
            else -> {
                val ok = QuickActions.run(ctx, key, cfg)
                if (ok == false) {
                    ctx.toast(QuickActions.failMessage(key))
                    if (key == "action:panorama") nav.value = Screen.CAMERA
                }
            }
        }
    }

    val current = nav.value
    val back: () -> Unit = { nav.value = current.root().takeIf { it != current } ?: Screen.HOME }
    BackHandler { if (nav.value != Screen.HOME) nav.value = if (nav.value.root() != nav.value) nav.value.root() else Screen.HOME }

    Box(Modifier.fillMaxSize()) {
        LauncherBackground(cfg)
        Row(Modifier.fillMaxSize()) {
            NavRail(current.root(), onSelect = { nav.value = it }, onTheme = { handle("action:theme") })
            Box(Modifier.weight(1f).fillMaxHeight()) {
                AnimatedContent(
                    targetState = current,
                    transitionSpec = {
                        (fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.985f)) togetherWith fadeOut(tween(140))
                    },
                    label = "nav",
                ) { screen ->
                    when (screen) {
                        Screen.HOME -> HomeScreen(cfg, st) { handle(it) }
                        Screen.APPS -> AppsScreen(null) { handle(it) }
                        Screen.AUDIO -> AudioScreen(cfg) { handle(it) }
                        Screen.CAMERA -> CameraScreen(cfg, null)
                        Screen.SETTINGS -> SettingsScreen(cfg, onBack = null, onOpenProbe = { nav.value = Screen.PROBE })
                        Screen.TIRES -> TiresScreen(cfg, st, back)
                        Screen.LAPS -> LapScreen(cfg, st, back)
                        Screen.BATTERY -> BatteryScreen(cfg, st, back)
                        Screen.TRIP -> TripScreen(cfg, st, back)
                        Screen.PROBE -> ProbeScreen(cfg, onBack = { nav.value = Screen.SETTINGS })
                    }
                }

                // แบนเนอร์เตือน: แตะเพื่อซ่อนจนกว่าสถานะจะเปลี่ยน
                val alerts = remember(st, cfg) { AlertEngine.evaluate(st, cfg).filter { it.level != AlertLevel.INFO } }
                val dismissed = remember { mutableStateListOf<String>() }
                val ids = alerts.map { it.id }
                LaunchedEffect(ids) { dismissed.retainAll(ids.toSet()) }
                AlertBannerStack(
                    alerts.filter { it.id !in dismissed }.take(3),
                    Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 12.dp, start = 16.dp, end = 16.dp),
                ) { dismissed.add(it) }
            }
        }
        if (showQuick) {
            QuickPanel(
                cfg, onDismiss = { showQuick = false }, onAction = { handle(it) },
                onRequestLocation = {
                    locPerm.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
                },
            )
        }
    }
}

/** แถบเมนูซ้าย */
@Composable
private fun NavRail(current: Screen, onSelect: (Screen) -> Unit, onTheme: () -> Unit) {
    val ui = LocalUi.current
    val line = MaterialTheme.colorScheme.outline
    Column(
        Modifier.fillMaxHeight().width(92.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = (ui.opacity + 0.25f).coerceAtMost(1f)))
            .drawBehind { drawLine(line, Offset(size.width, 0f), Offset(size.width, size.height), 1f) }
            .systemBarsPadding().padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(Brush.linearGradient(listOf(ui.accent2, ui.accent))))
        Spacer(Modifier.height(10.dp))
        RailItem(Icons.Rounded.Home, "หน้าหลัก", current == Screen.HOME) { onSelect(Screen.HOME) }
        RailItem(Icons.Rounded.Apps, "แอป", current == Screen.APPS) { onSelect(Screen.APPS) }
        RailItem(Icons.Rounded.LibraryMusic, "เสียง", current == Screen.AUDIO) { onSelect(Screen.AUDIO) }
        RailItem(Icons.Rounded.Videocam, "กล้อง 360", current == Screen.CAMERA) { onSelect(Screen.CAMERA) }
        RailItem(Icons.Rounded.Settings, "ตั้งค่า", current == Screen.SETTINGS) { onSelect(Screen.SETTINGS) }
        Spacer(Modifier.weight(1f))
        RailItem(if (ui.dark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode, "ธีม", false, onTheme)
    }
}

@Composable
private fun RailItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Column(
        Modifier.width(76.dp).clip(RoundedCornerShape(16.dp))
            .background(if (selected) c.primary.copy(alpha = 0.18f) else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, label, tint = if (selected) c.primary else c.onSurfaceVariant, modifier = Modifier.size(28.dp))
        Text(
            label, fontSize = 11.sp, color = if (selected) c.primary else c.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
