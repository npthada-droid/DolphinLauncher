package th.dolphin.launcher

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ระบบป้องกันค้าง / ทำงานซ้ำผิดปกติ
 *
 * เมื่อ "ตัดการทำงาน" (trip):
 *  - หยุดอ่านข้อมูลรถ (ไม่เรียก BYD API อีก) และหยุดงานเบื้องหลังทั้งหมด (เสียง, กล้องไฟเลี้ยว, หน้าต่างลอย, ปรับความสว่าง)
 *  - หน้า Launcher แสดงหน้าข้อผิดพลาดแบบเบา (ไม่มีงานหนัก) พร้อมสาเหตุและปุ่มกลับ Launcher เดิม
 *  - สถานะถูกบันทึกไว้ เปิดใหม่ก็ยังหยุดอยู่ จนกว่าผู้ใช้กด "เริ่มทำงานใหม่"
 */
object SafetyGuard {
    data class Trip(val reason: String, val detail: String, val time: Long)

    private const val PREF = "safety_guard"
    private const val TAG = "SafetyGuard"
    private val _tripped = MutableStateFlow<Trip?>(null)
    val tripped: StateFlow<Trip?> = _tripped.asStateFlow()
    private val counters = HashMap<String, ArrayDeque<Long>>()
    private var appCtx: Context? = null
    @Volatile var strict: Boolean = false

    fun init(ctx: Context) {
        appCtx = ctx.applicationContext
        _tripped.value = load(ctx)
    }

    val isTripped: Boolean get() = _tripped.value != null

    /**
     * นับเหตุการณ์ — ถ้าเกิน [limit] ครั้งภายใน [windowMs] จะตัดการทำงานทันที
     * @return false = ห้ามทำต่อ
     */

    /** จำกัดความถี่แบบนุ่มนวล: เกินแล้วแค่ข้าม ไม่หยุด Launcher (ใช้กับเหตุการณ์ปกติจากการขับ เช่น ไฟเลี้ยว) */
    @Synchronized
    fun allowSoft(key: String, limit: Int, windowMs: Long): Boolean {
        if (_tripped.value != null) return false
        val now = SystemClock.elapsedRealtime()
        val q = counters.getOrPut("soft:$key") { ArrayDeque() }
        while (q.isNotEmpty() && now - q.first() > windowMs) q.removeFirst()
        if (q.size >= limit) return false
        q.addLast(now)
        return true
    }

    @Synchronized
    fun allow(key: String, limit: Int, windowMs: Long, label: String): Boolean {
        if (_tripped.value != null) return false
        val lim = if (strict) (limit * 2 / 3).coerceAtLeast(2) else limit
        val now = SystemClock.elapsedRealtime()
        val q = counters.getOrPut(key) { ArrayDeque() }
        while (q.isNotEmpty() && now - q.first() > windowMs) q.removeFirst()
        q.addLast(now)
        if (q.size > lim) {
            trip("ทำงานซ้ำผิดปกติ: $label", "$label เกิดขึ้น ${q.size} ครั้งภายใน ${windowMs / 1000} วินาที (จำกัด $lim ครั้ง)")
            return false
        }
        return true
    }

    fun trip(reason: String, detail: String) {
        if (_tripped.value != null) return
        val t = Trip(reason, detail, System.currentTimeMillis())
        _tripped.value = t
        Log.e(TAG, "$reason\n$detail")
        appCtx?.let { save(it, t) }
    }

    fun reset(ctx: Context) {
        synchronized(this) { counters.clear() }
        HangWatchdog.grace()
        _tripped.value = null
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().remove("trip").apply()
    }

    /** ประวัติข้อผิดพลาด 20 รายการล่าสุด */
    fun history(ctx: Context): List<Trip> = runCatching {
        val a = JSONArray(ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("history", "[]"))
        List(a.length()) { i -> a.getJSONObject(i).let { Trip(it.optString("r"), it.optString("d"), it.optLong("t")) } }
    }.getOrDefault(emptyList())

    fun report(t: Trip): String {
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return "Dolphin Launcher ${BuildConfig.VERSION_NAME}\n${df.format(Date(t.time))}\n${t.reason}\n\n${t.detail}"
    }

    private fun save(ctx: Context, t: Trip) {
        val prefs = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val o = JSONObject().put("r", t.reason).put("d", t.detail.take(6000)).put("t", t.time)
        val hist = runCatching { JSONArray(prefs.getString("history", "[]")) }.getOrDefault(JSONArray())
        val newHist = JSONArray().put(o)
        for (i in 0 until minOf(hist.length(), 19)) newHist.put(hist.get(i))
        // commit() เพราะอาจถูกเรียกก่อน process ถูกปิด
        prefs.edit().putString("trip", o.toString()).putString("history", newHist.toString()).commit()
    }

    private fun load(ctx: Context): Trip? = runCatching {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("trip", null)?.let {
            val o = JSONObject(it)
            Trip(o.optString("r"), o.optString("d"), o.optLong("t"))
        }
    }.getOrNull()
}

/**
 * ตรวจจอค้าง: ส่งสัญญาณไปยัง main thread ทุก 1 วินาที
 * ถ้า main thread ไม่ตอบเกินเวลาที่ตั้ง → บันทึกสาเหตุ (พร้อม stack trace), เปิด Launcher เดิม แล้วปิดตัวเอง
 * เพื่อไม่ให้หน้าจอรถค้างหรือขึ้นหน้าต่าง "แอปไม่ตอบสนอง"
 */
object HangWatchdog {
    @Volatile private var lastPong = SystemClock.uptimeMillis()
    @Volatile var timeoutSec: Int = 6
    private var thread: Thread? = null
    // ช่วงเริ่มทำงาน (เปิดเครื่อง/โหลดแผนที่ครั้งแรก) อาจช้า — ยังไม่ตรวจ 30 วินาทีแรก
    @Volatile private var graceUntil = SystemClock.uptimeMillis() + 30_000L

    fun grace(ms: Long = 30_000L) {
        graceUntil = SystemClock.uptimeMillis() + ms
        lastPong = SystemClock.uptimeMillis()
    }

    fun start(ctx: Context) {
        if (thread != null) return
        val appCtx = ctx.applicationContext
        val main = Handler(Looper.getMainLooper())
        thread = Thread({
            while (true) {
                main.post { lastPong = SystemClock.uptimeMillis() }
                try { Thread.sleep(1000) } catch (e: InterruptedException) { return@Thread }
                val limit = timeoutSec
                if (limit <= 0 || SafetyGuard.isTripped || SystemClock.uptimeMillis() < graceUntil) { lastPong = SystemClock.uptimeMillis(); continue }
                // uptimeMillis ไม่นับช่วงเครื่องหลับ จึงไม่แจ้งเตือนผิดหลังรถดับ/ติดเครื่อง
                val late = SystemClock.uptimeMillis() - lastPong
                if (late > limit * 1000L) {
                    val stack = Looper.getMainLooper().thread.stackTrace.take(40).joinToString("\n") { "  at $it" }
                    SafetyGuard.trip(
                        "หน้าจอค้าง: แอปไม่ตอบสนอง ${late / 1000} วินาที",
                        "ตำแหน่งที่ค้าง (main thread):\n$stack",
                    )
                    CrashGuard.openFallbackHome(appCtx)
                    Thread.sleep(300)
                    Process.killProcess(Process.myPid())
                }
            }
        }, "dolphin-watchdog").apply { isDaemon = true; start() }
    }
}
