package th.dolphin.launcher.ui.camera

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import th.dolphin.launcher.app
import th.dolphin.launcher.ui.theme.LauncherTheme
import th.dolphin.launcher.vehicle.TurnSignal

/**
 * หน้าต่างลอยแสดงกล้องเมื่อเปิดไฟเลี้ยว
 * - วางชิดซ้าย/ขวาตามทิศไฟเลี้ยว (จอแนวตั้ง: วางด้านล่าง)
 * - ไม่บังการสัมผัสส่วนอื่นของจอ
 * - ปิดเองหลังไฟเลี้ยวดับตามเวลาที่ตั้งไว้
 */
class TurnCameraActivity : ComponentActivity() {

    companion object {
        /** true ระหว่างหน้าต่างกล้องเปิดอยู่ — กันการสั่งเปิดซ้ำทุกจังหวะไฟกระพริบ */
        @Volatile var showing = false

        fun show(ctx: Context, turn: TurnSignal) {
            if (showing) return
            // ไฟเลี้ยวเป็นเรื่องปกติของการขับ: เกินโควตาแค่ไม่เปิดกล้อง ไม่หยุด Launcher
            if (!th.dolphin.launcher.SafetyGuard.allowSoft("turncam", 12, 60_000)) return
            val i = Intent(ctx, TurnCameraActivity::class.java)
                .putExtra("turn", turn.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            runCatching { ctx.startActivity(i) }
        }
    }

    private val side = mutableStateOf(TurnSignal.LEFT)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (th.dolphin.launcher.SafetyGuard.isTripped) { finish(); return }
        showing = true
        readIntent(intent)
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        applyWindow()

        setContent {
            val cfg by app.settings.flow.collectAsStateWithLifecycle()
            val st by app.vehicle.state.collectAsStateWithLifecycle()
            val turn by side

            val tripped by th.dolphin.launcher.SafetyGuard.tripped.collectAsStateWithLifecycle()
            LaunchedEffect(st.reversing, tripped) {
                if (st.reversing || tripped != null) finish()
            }
            LaunchedEffect(st.turn, cfg.turnCamHoldSec, cfg.turnCamOnHazard) {
                val active = st.turn == TurnSignal.LEFT || st.turn == TurnSignal.RIGHT ||
                    (st.turn == TurnSignal.HAZARD && cfg.turnCamOnHazard)
                if (active) {
                    if (st.turn != side.value) {
                        side.value = st.turn
                        applyWindow()
                    }
                } else {
                    delay(cfg.turnCamHoldSec * 1000L + 300L)
                    finish()
                }
            }

            LauncherTheme(cfg) {
                TurnCameraPanel(turn, if (turn == TurnSignal.RIGHT) cfg.rightCamId else cfg.leftCamId) { finish() }
            }
        }
    }

    override fun onDestroy() {
        showing = false
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readIntent(intent)
        applyWindow()
    }

    private fun readIntent(i: Intent?) {
        val name = i?.getStringExtra("turn") ?: return
        side.value = runCatching { TurnSignal.valueOf(name) }.getOrDefault(TurnSignal.LEFT)
    }

    private fun applyWindow() {
        val cfg = app.settings.value
        val dm = resources.displayMetrics
        val portrait = dm.heightPixels > dm.widthPixels
        if (portrait) {
            window.setLayout((dm.widthPixels * 0.94f).toInt(), (dm.heightPixels * cfg.turnCamSize * 0.75f).toInt())
            window.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
        } else {
            window.setLayout((dm.widthPixels * cfg.turnCamSize).toInt(), (dm.heightPixels * 0.62f).toInt())
            val h = if (side.value == TurnSignal.RIGHT) Gravity.END else Gravity.START
            window.setGravity(h or Gravity.CENTER_VERTICAL)
        }
    }
}
