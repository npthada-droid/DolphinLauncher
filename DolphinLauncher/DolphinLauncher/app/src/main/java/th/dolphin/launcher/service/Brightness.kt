package th.dolphin.launcher.service

import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import th.dolphin.launcher.SafetyGuard
import th.dolphin.launcher.app
import java.util.Calendar
import java.util.TimeZone

/**
 * ปรับความสว่างจออัตโนมัติ
 *  - ตามไฟหน้า: เปิดไฟหน้า → ระดับกลางคืน, ปิด → ระดับกลางวัน (อ่านไฟหน้าไม่ได้จะใช้เวลาพระอาทิตย์แทน)
 *  - ตามพระอาทิตย์: ใช้เวลาขึ้น/ตกจากสภาพอากาศ หรือชั่วโมงที่ตั้งไว้
 * เขียนค่าเฉพาะเมื่อเปลี่ยน และหน่วงเวลาก่อนเปลี่ยน กันกระพริบ
 */
class BrightnessController(private val ctx: Context, scope: CoroutineScope) {
    private var lastWritten: Int? = null

    init {
        val app = ctx.app
        val minuteTicker = flow {
            while (true) {
                emit(System.currentTimeMillis())
                delay(60_000)
            }
        }
        scope.launch {
            combine(
                app.vehicle.state.map { it.headlights }.distinctUntilChanged(),
                app.settings.flow,
                app.weather.weather,
                minuteTicker,
                SafetyGuard.tripped,
            ) { hl, cfg, w, now, tripped ->
                if (!cfg.autoBrightness || tripped != null) null
                else {
                    val night = if (cfg.brightTrigger == "headlights" && hl != null) hl
                    else isNight(now, w?.sunrise, w?.sunset, cfg.autoLightFrom, cfg.autoDarkFrom)
                    Triple(if (night) cfg.brightNight else cfg.brightDay, cfg.lightDelaySec, night)
                }
            }.distinctUntilChanged().collectLatest { target ->
                if (target == null) {
                    lastWritten = null
                    return@collectLatest
                }
                delay(target.second.coerceIn(0, 30) * 1000L)
                apply(target.first)
            }
        }
    }

    private fun apply(level: Int) {
        val v = level.coerceIn(5, 255)
        if (v == lastWritten || !Settings.System.canWrite(ctx)) return
        if (!SafetyGuard.allow("brightness", 20, 60_000, "การปรับความสว่างจอ")) return
        runCatching {
            Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS, v)
            lastWritten = v
        }
    }

    companion object {
        fun isNight(now: Long, rise: Long?, set: Long?, lightFrom: Int, darkFrom: Int): Boolean {
            if (rise != null && set != null) {
                val day = 86_400_000L
                val tz = TimeZone.getDefault()
                fun tod(t: Long) = ((t + tz.getOffset(t)) % day + day) % day
                val n = tod(now)
                return n < tod(rise) || n >= tod(set)
            }
            val h = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
            return h < lightFrom || h >= darkFrom
        }
    }
}
