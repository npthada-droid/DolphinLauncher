package th.dolphin.launcher.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioManager
import android.media.ToneGenerator
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import th.dolphin.launcher.R
import th.dolphin.launcher.alerts.AlertEngine
import th.dolphin.launcher.alerts.AlertLevel
import th.dolphin.launcher.app
import th.dolphin.launcher.ui.MainActivity
import th.dolphin.launcher.ui.camera.TurnCameraActivity
import th.dolphin.launcher.ui.components.AlertBannerStack
import th.dolphin.launcher.ui.theme.LauncherTheme
import th.dolphin.launcher.vehicle.TurnSignal

/**
 * บริการเบื้องหลัง: ทำงานตลอดแม้เปิดแอปอื่นอยู่
 *  - เสียงเตือนเมื่อวิกฤต (ประตูเปิด/ไม่คาดเข็มขัดขณะวิ่ง, ยางร้อน)
 *  - เด้งกล้องเมื่อเปิดไฟเลี้ยว
 *  - แบนเนอร์เตือนแบบลอยทับแอปอื่น
 */
class VehicleMonitorService : LifecycleService() {
    private var overlay: AlertOverlay? = null
    private var bubble: FloatingBubble? = null
    private var brightness: BrightnessController? = null
    private var tone: ToneGenerator? = null

    override fun onCreate() {
        super.onCreate()
        startInForeground()
        if (!th.dolphin.launcher.SafetyGuard.allow("service", 8, 60_000, "บริการเบื้องหลังเริ่มใหม่")) {
            stopSelf()
            return
        }
        brightness = BrightnessController(this, lifecycleScope)
        tone = runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90) }.getOrNull()
        val app = this.app

        lifecycleScope.launch {
            combine(app.vehicle.state, app.settings.flow, th.dolphin.launcher.SafetyGuard.tripped) { s, c, tr ->
                tr == null && c.alertSound && AlertEngine.evaluate(s, c).any { it.level == AlertLevel.CRITICAL }
            }.distinctUntilChanged().collectLatest { on ->
                if (on) {
                    while (true) {
                        tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
                        delay(1600)
                    }
                }
            }
        }

        lifecycleScope.launch {
            app.vehicle.state.map { it.turn }.distinctUntilChanged().collect { onTurn(it) }
        }

        lifecycleScope.launch {
            combine(app.settings.flow.map { it.overlayAlerts to it.bubbleEnabled }, th.dolphin.launcher.SafetyGuard.tripped) { a, b -> a to b }
                .distinctUntilChanged().collect { refreshOverlay() }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        refreshOverlay()
        return START_STICKY
    }

    private fun onTurn(turn: TurnSignal) {
        val cfg = app.settings.value
        if (!cfg.turnCamEnabled || th.dolphin.launcher.SafetyGuard.isTripped) return
        if (app.vehicle.state.value.reversing) return
        val trigger = turn == TurnSignal.LEFT || turn == TurnSignal.RIGHT ||
            (turn == TurnSignal.HAZARD && cfg.turnCamOnHazard)
        if (!trigger) return
        if (cfg.turnCamMode == "app" && cfg.turnCamAppPkg.isNotBlank()) {
            app.apps.launchTarget(cfg.turnCamAppPkg)
        } else {
            TurnCameraActivity.show(this, turn)
        }
    }

    private fun refreshOverlay() {
        val canDraw = Settings.canDrawOverlays(this) && !th.dolphin.launcher.SafetyGuard.isTripped
        val wantBubble = app.settings.value.bubbleEnabled && canDraw
        if (wantBubble && bubble == null) {
            bubble = FloatingBubble(this).also { it.show() }
        } else if (!wantBubble) {
            bubble?.hide()
            bubble = null
        }
        val want = app.settings.value.overlayAlerts && canDraw
        if (want && overlay == null) {
            overlay = AlertOverlay(this).also { it.show() }
        } else if (!want) {
            overlay?.hide()
            overlay = null
        }
    }

    override fun onDestroy() {
        overlay?.hide()
        bubble?.hide()
        tone?.release()
        super.onDestroy()
    }

    private fun startInForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("monitor", th.dolphin.launcher.i18n.I18n.tr("สถานะรถ"), NotificationManager.IMPORTANCE_MIN)
        )
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(this, "monitor")
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("Dolphin Launcher")
            .setContentText(th.dolphin.launcher.i18n.I18n.tr("กำลังตรวจสถานะประตู เข็มขัด ลมยาง และไฟเลี้ยว"))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(pi)
            .build()
        startForeground(1001, n)
    }
}

/** แบนเนอร์เตือนลอยด้านบนจอ (ใช้สิทธิ์ SYSTEM_ALERT_WINDOW) */
class AlertOverlay(private val ctx: Context) {
    private val wm = ctx.getSystemService(WindowManager::class.java)
    private var view: ComposeView? = null
    private var owner: OverlayOwner? = null

    fun show() {
        if (view != null) return
        if (!th.dolphin.launcher.SafetyGuard.allow("overlay", 12, 60_000, "การสร้างหน้าต่างแจ้งเตือนลอย")) return
        val o = OverlayOwner().also { it.start() }
        val v = ComposeView(ctx).apply {
            setViewTreeLifecycleOwner(o)
            setViewTreeSavedStateRegistryOwner(o)
            setContent { OverlayContent() }
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP }
        runCatching {
            wm.addView(v, lp)
            view = v
            owner = o
        }.onFailure { o.stop() }
    }

    fun hide() {
        view?.let { runCatching { wm.removeView(it) } }
        view = null
        owner?.stop()
        owner = null
    }
}

@Composable
private fun OverlayContent() {
    val ctx = LocalContext.current
    val app = ctx.app
    val st by app.vehicle.state.collectAsState()
    val cfg by app.settings.flow.collectAsState()
    val fg by app.foreground.collectAsState()
    if (fg) return
    val alerts = remember(st, cfg) { AlertEngine.evaluate(st, cfg).filter { it.level != AlertLevel.INFO } }
    if (alerts.isEmpty()) return
    LauncherTheme(cfg) {
        Box(Modifier.fillMaxWidth().padding(top = 16.dp, start = 16.dp, end = 16.dp), contentAlignment = Alignment.TopCenter) {
            AlertBannerStack(alerts.take(2)) {
                runCatching {
                    ctx.startActivity(Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        }
    }
}

internal class OverlayOwner : SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val controller = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry

    fun start() {
        controller.performRestore(null)
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun stop() {
        if (registry.currentState != Lifecycle.State.INITIALIZED) registry.currentState = Lifecycle.State.DESTROYED
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        runCatching {
            ContextCompat.startForegroundService(context, Intent(context, VehicleMonitorService::class.java))
        }
    }
}
