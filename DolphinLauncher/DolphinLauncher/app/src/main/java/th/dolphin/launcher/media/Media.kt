package th.dolphin.launcher.media

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * บริการว่างๆ ที่มีไว้ขอสิทธิ์ "เข้าถึงการแจ้งเตือน"
 * สิทธิ์นี้ทำให้ Launcher อ่านเพลง/สถานีที่กำลังเล่นจากทุกแอปได้ (MediaSession)
 * แอปไม่ได้อ่านหรือเก็บเนื้อหาการแจ้งเตือน
 */
class MediaListener : NotificationListenerService()

data class NowPlaying(
    val title: String,
    val artist: String,
    val art: ImageBitmap?,
    val playing: Boolean,
    val pkg: String,
    val appLabel: String,
)

class MediaRepository(private val ctx: Context) {
    private val msm = ctx.getSystemService(MediaSessionManager::class.java)
    private val listenerCn = ComponentName(ctx, MediaListener::class.java)
    private val main = Handler(Looper.getMainLooper())
    private var controller: MediaController? = null
    private var registered = false

    private val _now = MutableStateFlow<NowPlaying?>(null)
    val now: StateFlow<NowPlaying?> = _now.asStateFlow()
    private val _hasAccess = MutableStateFlow(false)
    val hasAccess: StateFlow<Boolean> = _hasAccess.asStateFlow()

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onSessionDestroyed() = refresh()
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list -> attach(list) }

    /** เรียกตอนเปิดแอป/กลับมาหน้า Launcher (เช่น หลังผู้ใช้เพิ่งให้สิทธิ์) */
    fun refresh() {
        val granted = NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
        _hasAccess.value = granted
        if (!granted) {
            _now.value = null
            return
        }
        runCatching {
            if (!registered) {
                msm.addOnActiveSessionsChangedListener(sessionsListener, listenerCn, main)
                registered = true
            }
            attach(msm.getActiveSessions(listenerCn))
        }
    }

    private fun attach(list: List<MediaController>?) {
        val best = list?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: list?.firstOrNull()
        if (best?.sessionToken != controller?.sessionToken) {
            runCatching { controller?.unregisterCallback(callback) }
            controller = best
            runCatching { best?.registerCallback(callback, main) }
        }
        publish()
    }

    private fun publish() {
        val c = controller
        if (c == null) {
            _now.value = null
            return
        }
        val md = c.metadata
        val title = md?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: md?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) ?: ""
        val artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: md?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE) ?: ""
        val bmp = md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: md?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val label = runCatching {
            val pm = ctx.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(c.packageName, 0)).toString()
        }.getOrDefault(c.packageName)
        _now.value = NowPlaying(
            title = title,
            artist = artist,
            art = bmp?.let { shrink(it) }?.asImageBitmap(),
            playing = c.playbackState?.state == PlaybackState.STATE_PLAYING,
            pkg = c.packageName,
            appLabel = label,
        )
    }

    private fun shrink(b: Bitmap): Bitmap {
        val max = 256
        if (b.width <= max && b.height <= max) return b
        val k = max.toFloat() / maxOf(b.width, b.height)
        return Bitmap.createScaledBitmap(b, (b.width * k).toInt().coerceAtLeast(1), (b.height * k).toInt().coerceAtLeast(1), true)
    }

    fun playPause() {
        val c = controller ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause() else c.transportControls.play()
    }

    fun next() { controller?.transportControls?.skipToNext() }
    fun previous() { controller?.transportControls?.skipToPrevious() }
}

data class BtDevice(val name: String, val address: String, val battery: Int?)

data class BtState(
    val supported: Boolean = false,
    val enabled: Boolean = false,
    val connected: List<BtDevice> = emptyList(),
)

/** สถานะบลูทูธ + มือถือที่เชื่อมต่อ + แบตมือถือ (ถ้าระบบรายงาน) — อ่านอย่างเดียว */
class BluetoothRepository(private val ctx: Context, scope: CoroutineScope) {
    private val _state = MutableStateFlow(BtState())
    val state: StateFlow<BtState> = _state.asStateFlow()

    init {
        scope.launch(Dispatchers.IO) {
            while (true) {
                runCatching { poll() }
                delay(4000)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun poll() {
        val adapter = ctx.getSystemService(BluetoothManager::class.java)?.adapter
        if (adapter == null) {
            _state.value = BtState(supported = false)
            return
        }
        val enabled = runCatching { adapter.isEnabled }.getOrDefault(false)
        val connected = if (enabled) {
            runCatching {
                adapter.bondedDevices.orEmpty().filter { isConnected(it) }
                    .map { BtDevice(it.name ?: it.address, it.address, battery(it)) }
            }.getOrDefault(emptyList())
        } else emptyList()
        _state.value = BtState(true, enabled, connected)
    }

    // เมธอดภายในของ BluetoothDevice (มีตั้งแต่ Android 8) — ใช้ได้ทั้งตอนรถเป็นฝั่งรับ (HFP/A2DP sink)
    private fun isConnected(d: BluetoothDevice): Boolean =
        runCatching { d.javaClass.getMethod("isConnected").invoke(d) as Boolean }.getOrDefault(false)

    private fun battery(d: BluetoothDevice): Int? =
        runCatching { (d.javaClass.getMethod("getBatteryLevel").invoke(d) as Int).takeIf { it in 0..100 } }.getOrNull()
}
