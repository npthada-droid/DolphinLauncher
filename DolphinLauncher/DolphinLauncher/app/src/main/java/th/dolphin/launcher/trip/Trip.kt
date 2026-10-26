package th.dolphin.launcher.trip

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import th.dolphin.launcher.settings.SettingsStore
import th.dolphin.launcher.vehicle.VehicleState
import java.util.Calendar

/** ตัวนับทริปหนึ่งชุด */
data class TripStats(
    val startedAt: Long = System.currentTimeMillis(),
    val distanceKm: Double = 0.0,
    val drivingMs: Long = 0L,
    val energyKwh: Double = 0.0,
    val maxSpeed: Float = 0f,
) {
    val avgSpeedKmh: Double get() = if (drivingMs > 60_000) distanceKm / (drivingMs / 3_600_000.0) else 0.0
    /** kWh/100km (ต้องวิ่งอย่างน้อย 1 กม. จึงคำนวณ) */
    val consumption: Double? get() = if (distanceKm >= 1.0 && energyKwh > 0) energyKwh / distanceKm * 100.0 else null

    fun toJson(): JSONObject = JSONObject().put("startedAt", startedAt).put("distanceKm", distanceKm)
        .put("drivingMs", drivingMs).put("energyKwh", energyKwh).put("maxSpeed", maxSpeed.toDouble())

    companion object {
        fun fromJson(o: JSONObject?) = if (o == null) TripStats() else TripStats(
            o.optLong("startedAt", System.currentTimeMillis()), o.optDouble("distanceKm", 0.0),
            o.optLong("drivingMs"), o.optDouble("energyKwh", 0.0), o.optDouble("maxSpeed", 0.0).toFloat(),
        )
    }
}

data class ChargeSession(
    val id: Long,
    val startAt: Long,
    val endAt: Long,
    val socStart: Float,
    val socEnd: Float,
    val kwh: Double,
    val cost: Double,
    val odometerKm: Float?,
) {
    fun toJson(): JSONObject = JSONObject().put("id", id).put("startAt", startAt).put("endAt", endAt)
        .put("socStart", socStart.toDouble()).put("socEnd", socEnd.toDouble()).put("kwh", kwh).put("cost", cost)
        .put("odo", odometerKm?.toDouble() ?: -1.0)

    companion object {
        fun fromJson(o: JSONObject) = ChargeSession(
            o.getLong("id"), o.optLong("startAt"), o.optLong("endAt"),
            o.optDouble("socStart").toFloat(), o.optDouble("socEnd").toFloat(),
            o.optDouble("kwh"), o.optDouble("cost"),
            o.optDouble("odo", -1.0).takeIf { it >= 0 }?.toFloat(),
        )
    }
}

data class TripSnapshot(
    val tripA: TripStats = TripStats(),
    val sinceCharge: TripStats = TripStats(),
    val today: TripStats = TripStats(),
    val charging: ChargeSession? = null,
    val history: List<ChargeSession> = emptyList(),
)

/**
 * บันทึกทริปและการชาร์จจากสถานะรถ (ทำงานในแอปตลอด ไม่ต้องเปิดหน้าจอ)
 *  - ระยะทาง: ใช้ผลต่างไมล์รถ ถ้าไม่มีใช้ความเร็ว × เวลา
 *  - พลังงาน: ผลต่าง SOC × ความจุแบต (ตั้งได้ในตั้งค่า)
 *  - การชาร์จ: รถแจ้งว่ากำลังชาร์จ หรือ SOC เพิ่มขึ้นขณะจอด
 */
class TripRecorder(context: Context, private val settings: SettingsStore, private val scope: CoroutineScope) {
    private val prefs = context.getSharedPreferences("trip", Context.MODE_PRIVATE)
    private val _snap = MutableStateFlow(load())
    val snapshot: StateFlow<TripSnapshot> = _snap.asStateFlow()

    private var lastT = 0L
    private var lastOdo: Float? = null
    private var lastSoc: Float? = null
    private var socAtStop: Float? = null
    private var lastSave = 0L

    fun onState(st: VehicleState) {
        val now = SystemClock.elapsedRealtime()
        val dtMs = if (lastT == 0L) 0L else (now - lastT).coerceIn(0L, 5_000L)
        lastT = now
        val cap = settings.value.batteryKwh.toDouble()
        var s = _snap.value

        // วันใหม่ → เริ่มตัวนับ "วันนี้" ใหม่
        if (!sameDay(s.today.startedAt, System.currentTimeMillis())) s = s.copy(today = TripStats())

        // ระยะทาง
        val odo = st.odometerKm
        val dKm = when {
            odo != null && lastOdo != null && odo - lastOdo!! in 0f..1f -> (odo - lastOdo!!).toDouble()
            st.speedKmh > 1f -> st.speedKmh * dtMs / 3_600_000.0
            else -> 0.0
        }
        if (odo != null) lastOdo = odo
        val driving = st.speedKmh > 1f
        val dDrive = if (driving) dtMs else 0L

        // พลังงานที่ใช้ (SOC ลดลงขณะไม่ได้ชาร์จ)
        val soc = st.socPercent
        var dKwh = 0.0
        if (soc != null && lastSoc != null && st.charging != true) {
            val drop = lastSoc!! - soc
            if (drop in 0f..2f) dKwh = drop / 100.0 * cap
        }

        fun add(t: TripStats) = t.copy(
            distanceKm = t.distanceKm + dKm, drivingMs = t.drivingMs + dDrive,
            energyKwh = t.energyKwh + dKwh, maxSpeed = maxOf(t.maxSpeed, st.speedKmh),
        )
        s = s.copy(tripA = add(s.tripA), sinceCharge = add(s.sinceCharge), today = add(s.today))

        // การชาร์จ
        if (soc != null) {
            if (!driving && socAtStop == null) socAtStop = soc
            if (driving) socAtStop = null
            val rising = !driving && socAtStop != null && soc - socAtStop!! >= 1f
            val isCharging = st.charging == true || rising
            val cur = s.charging
            if (isCharging && cur == null) {
                s = s.copy(charging = ChargeSession(
                    System.currentTimeMillis(), System.currentTimeMillis(), System.currentTimeMillis(),
                    socAtStop ?: soc, soc, 0.0, 0.0, odo,
                ))
            } else if (cur != null) {
                val kwh = ((soc - cur.socStart).coerceAtLeast(0f) / 100.0) * cap
                val updated = cur.copy(endAt = System.currentTimeMillis(), socEnd = soc, kwh = kwh, cost = kwh * settings.value.elecPrice)
                val ended = driving || (st.charging == false && !rising) || (st.charging == null && System.currentTimeMillis() - cur.endAt > 15 * 60_000 && soc <= cur.socEnd)
                s = if (ended) {
                    val keep = updated.socEnd - updated.socStart >= 1f
                    s.copy(
                        charging = null,
                        history = if (keep) (listOf(updated) + s.history).take(200) else s.history,
                        sinceCharge = if (keep) TripStats() else s.sinceCharge,
                    )
                } else s.copy(charging = updated)
                if (ended) socAtStop = null
            }
        }
        if (soc != null) lastSoc = soc
        _snap.value = s

        if (now - lastSave > 30_000) {
            lastSave = now
            save()
        }
    }

    fun resetTripA() {
        _snap.value = _snap.value.copy(tripA = TripStats())
        save()
    }

    fun deleteCharge(id: Long) {
        _snap.value = _snap.value.copy(history = _snap.value.history.filterNot { it.id == id })
        save()
    }

    fun save() {
        val s = _snap.value
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                prefs.edit()
                    .putString("tripA", s.tripA.toJson().toString())
                    .putString("since", s.sinceCharge.toJson().toString())
                    .putString("today", s.today.toJson().toString())
                    .putString("history", JSONArray(s.history.map { it.toJson() }).toString())
                    .apply()
            }
        }
    }

    private fun load(): TripSnapshot = runCatching {
        fun obj(k: String) = prefs.getString(k, null)?.let { JSONObject(it) }
        val hist = prefs.getString("history", null)?.let { a -> JSONArray(a).let { arr -> List(arr.length()) { ChargeSession.fromJson(arr.getJSONObject(it)) } } }
            ?: emptyList()
        TripSnapshot(TripStats.fromJson(obj("tripA")), TripStats.fromJson(obj("since")), TripStats.fromJson(obj("today")), null, hist)
    }.getOrDefault(TripSnapshot())

    private fun sameDay(a: Long, b: Long): Boolean {
        val ca = Calendar.getInstance().apply { timeInMillis = a }
        val cb = Calendar.getInstance().apply { timeInMillis = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }
}
