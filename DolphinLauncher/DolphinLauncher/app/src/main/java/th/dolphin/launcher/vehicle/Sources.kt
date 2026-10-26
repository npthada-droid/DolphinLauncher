package th.dolphin.launcher.vehicle

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import th.dolphin.launcher.settings.SettingsStore
import kotlin.math.sin

interface VehicleDataSource {
    val name: String
    fun stream(pollMs: Long): Flow<VehicleState>
}

/** ค่าที่ปรับได้จากเมนู "โหมดทดสอบ" */
data class DemoControls(
    val driving: Boolean = true,
    val reverse: Boolean = false,
    val headlights: Boolean = false,
    val doors: Doors = Doors(),
    val driverBelt: Boolean = true,
    val passengerBelt: Boolean = true,
    val passengerOccupied: Boolean = false,
    val turn: TurnSignal = TurnSignal.NONE,
    val tireOffsetBar: Float = 0f,
)

class SimulatedSource(private val demo: StateFlow<DemoControls>) : VehicleDataSource {
    override val name = "โหมดจำลอง"

    override fun stream(pollMs: Long): Flow<VehicleState> = flow {
        var t = 0.0
        var soc = 78f
        var odo = 12_480f
        val base = floatArrayOf(2.50f, 2.50f, 2.45f, 2.45f)
        while (true) {
            val d = demo.value
            val speed = if (d.reverse) 4f else if (d.driving) (45 + 38 * sin(t / 9.0)).toFloat().coerceAtLeast(0f) else 0f
            soc = (soc - 0.0004f).coerceAtLeast(5f)
            odo += speed / 3600f * (pollMs / 1000f)
            val tires = List(4) { i ->
                Tire(base[i] + d.tireOffsetBar + (0.03 * sin(t / 20.0 + i)).toFloat(), 32f + speed / 8f + i)
            }
            emit(
                VehicleState(
                    connected = true,
                    source = name,
                    speedKmh = speed,
                    gear = if (d.reverse) "R" else if (d.driving) "D" else "P",
                    socPercent = soc,
                    rangeKm = soc * 4.2f,
                    odometerKm = odo,
                    powerKw = (speed * 0.35f + 12 * sin(t / 3.0)).toFloat(),
                    outsideTempC = 31f,
                    sohPercent = 97.4f,
                    hvTempC = 29f + speed / 20f,
                    hvVoltage = 398f - speed / 10f,
                    hvCurrentA = speed * 0.9f,
                    lvVoltage = 13.9f,
                    charging = false,
                    tires = tires,
                    doors = d.doors,
                    belts = Belts(d.driverBelt, d.passengerBelt, d.passengerOccupied),
                    turn = d.turn,
                    headlights = d.headlights,
                )
            )
            t += pollMs / 1000.0
            delay(pollMs)
        }
    }
}

/** อ่านข้อมูลจริงจาก BYD Auto API ผ่าน reflection ตามตาราง mapping ที่แก้ได้ในตั้งค่า */
class BydAutoSource(private val ctx: Context, private val mapText: String) : VehicleDataSource {
    override val name = "BYD API"

    override fun stream(pollMs: Long): Flow<VehicleState> = flow {
        val specs = BydApiMap.parse(mapText)
        val r = BydReflect(ctx)
        var slowStreak = 0
        var emptyStreak = 0
        // อ่านไม่ถี่กว่า 250ms เพื่อไม่ให้ระบบของรถทำงานหนัก
        val interval = pollMs.coerceAtLeast(250L)
        while (true) {
            if (th.dolphin.launcher.SafetyGuard.isTripped) return@flow
            val t0 = android.os.SystemClock.uptimeMillis()
            var hits = 0
            fun num(k: String): Float? = specs[k]?.let { r.num(it) }?.toFloat()?.also { hits++ }
            fun flag(k: String): Boolean? = specs[k]?.let { r.bool(it) }?.also { hits++ }

            val gearRaw = specs["gear"]?.let { s ->
                r.raw(s)?.let { v ->
                    hits++
                    (v as? Number)?.toInt()?.let { s.map[it] ?: it.toString() } ?: v.toString()
                }
            } ?: "-"
            val gear = if (flag("reverse") == true) "R" else gearRaw
            val tl = flag("turn_left") == true
            val tr = flag("turn_right") == true
            val tires = listOf("fl", "fr", "rl", "rr").map { Tire(num("tire_$it"), num("tire_temp_$it")) }
            val doors = Doors(
                fl = flag("door_fl") == true, fr = flag("door_fr") == true,
                rl = flag("door_rl") == true, rr = flag("door_rr") == true,
                trunk = flag("trunk") == true, hood = flag("hood") == true,
            )
            val belts = Belts(
                driver = flag("belt_driver"),
                passenger = flag("belt_passenger"),
                passengerOccupied = flag("passenger_occupied") ?: false,
            )
            val speedRaw = num("speed")
            val speed = speedRaw ?: 0f
            val soc = num("soc")
            val range = num("range")
            val odo = num("odometer")
            val power = num("power")
            val outside = num("outside_temp")
            val soh = num("soh")
            val hvTemp = num("hv_temp")
            val hvV = num("hv_voltage")
            val hvA = num("hv_current")
            val lvV = num("lv_voltage")
            val chg = flag("charging")

            emit(
                VehicleState(
                    connected = hits > 0,
                    source = if (hits > 0) "BYD API · $hits ค่า" else "BYD API: ยังอ่านค่าไม่ได้",
                    speedKmh = speed, speedValid = speedRaw != null, gear = gear, socPercent = soc, rangeKm = range, odometerKm = odo,
                    powerKw = power, outsideTempC = outside,
                    sohPercent = soh, hvTempC = hvTemp, hvVoltage = hvV, hvCurrentA = hvA, lvVoltage = lvV, charging = chg, tires = tires, doors = doors, belts = belts,
                    headlights = flag("headlights"),
                    turn = when {
                        tl && tr -> TurnSignal.HAZARD
                        tl -> TurnSignal.LEFT
                        tr -> TurnSignal.RIGHT
                        else -> TurnSignal.NONE
                    },
                )
            )
            // ตรวจ BYD API ตอบช้า: ถ้าอ่านหนึ่งรอบนานเกิน 1.5 วินาที ติดกัน 3 รอบ → หยุดทันที
            val took = android.os.SystemClock.uptimeMillis() - t0
            slowStreak = if (took > 1500) slowStreak + 1 else 0
            if (slowStreak >= 3) {
                th.dolphin.launcher.SafetyGuard.trip(
                    "BYD API ตอบช้า",
                    "การอ่านข้อมูลรถใช้เวลา ${took} ms ติดกัน 3 รอบ — หยุดอ่านเพื่อไม่ให้ระบบของรถค้าง\n" +
                        "คำสั่งที่ถูกปิดเพราะ error: ${r.disabledSpecs.joinToString().ifBlank { "-" }}",
                )
                return@flow
            }
            // อ่านไม่ได้เลยต่อเนื่อง → ลดความถี่เหลือทุก 5 วินาที
            emptyStreak = if (hits == 0) emptyStreak + 1 else 0
            delay(if (emptyStreak >= 20) 5000L else interval)
        }
    }.flowOn(Dispatchers.IO)
}

@OptIn(ExperimentalCoroutinesApi::class)
class VehicleRepository(
    context: Context,
    settings: SettingsStore,
    scope: CoroutineScope,
    location: th.dolphin.launcher.location.LocationRepository,
) {
    private val ctx = context.applicationContext
    val demo = MutableStateFlow(DemoControls())
    private val sim = SimulatedSource(demo)

    private data class SourceKey(val source: String, val pollMs: Int, val map: String, val stopped: Boolean)

    private val raw = combine(settings.flow, th.dolphin.launcher.SafetyGuard.tripped) { c, t -> SourceKey(c.dataSource, c.pollMs, c.bydMap, t != null) }
        .distinctUntilChanged()
        .flatMapLatest { k ->
            if (k.stopped) return@flatMapLatest kotlinx.coroutines.flow.flowOf(
                VehicleState(connected = false, source = "หยุดทำงาน (ระบบป้องกัน)", speedValid = false)
            )
            val src: VehicleDataSource = if (k.source == "byd") BydAutoSource(ctx, k.map) else sim
            src.stream(k.pollMs.toLong().coerceIn(50L, 5000L))
                .catch { e -> emit(VehicleState(connected = false, source = "ผิดพลาด: ${e.message}")) }
        }

    /** สถานะรถ + ความเร็วจาก GPS เมื่อเลือกไว้ หรือเมื่ออ่านความเร็วจากรถไม่ได้ */
    val state: StateFlow<VehicleState> = combine(raw, location.fix, settings.flow) { s, fix, cfg ->
        val gps = fix?.takeIf { System.currentTimeMillis() - it.time < 5000 }?.speedKmh
        val useGps = gps != null && (cfg.speedSource == "gps" || (cfg.speedSource == "auto" && !s.speedValid))
        if (useGps) s.copy(speedKmh = gps!!, speedFromGps = true) else s
    }.stateIn(scope, SharingStarted.Eagerly, VehicleState())
}
