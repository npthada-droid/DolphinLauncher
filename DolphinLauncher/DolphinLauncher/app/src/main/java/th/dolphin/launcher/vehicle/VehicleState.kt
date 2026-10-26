package th.dolphin.launcher.vehicle

enum class TurnSignal { NONE, LEFT, RIGHT, HAZARD }

/** index ยาง: 0=หน้าซ้าย 1=หน้าขวา 2=หลังซ้าย 3=หลังขวา */
data class Tire(val pressureBar: Float? = null, val tempC: Float? = null)

data class Doors(
    val fl: Boolean = false,
    val fr: Boolean = false,
    val rl: Boolean = false,
    val rr: Boolean = false,
    val trunk: Boolean = false,
    val hood: Boolean = false,
) {
    val anyOpen: Boolean get() = fl || fr || rl || rr || trunk || hood

    fun openNames(): List<String> = buildList {
        if (fl) add("หน้าซ้าย")
        if (fr) add("หน้าขวา")
        if (rl) add("หลังซ้าย")
        if (rr) add("หลังขวา")
        if (trunk) add("ฝาท้าย")
        if (hood) add("ฝากระโปรงหน้า")
    }
}

/** true = คาดแล้ว, false = ยังไม่คาด, null = ไม่ทราบ */
data class Belts(
    val driver: Boolean? = true,
    val passenger: Boolean? = true,
    val passengerOccupied: Boolean = false,
)

data class VehicleState(
    val connected: Boolean = false,
    val source: String = "",
    val speedKmh: Float = 0f,
    /** false = อ่านความเร็วจากรถไม่ได้ (จะใช้ GPS แทนเมื่อเลือกแหล่งความเร็วอัตโนมัติ) */
    val speedValid: Boolean = true,
    val speedFromGps: Boolean = false,
    val gear: String = "P",
    val socPercent: Float? = null,
    val rangeKm: Float? = null,
    val odometerKm: Float? = null,
    val powerKw: Float? = null,
    val outsideTempC: Float? = null,
    // แบตเตอรี่ขับเคลื่อน (แบตใหญ่)
    val sohPercent: Float? = null,
    val hvTempC: Float? = null,
    val hvVoltage: Float? = null,
    val hvCurrentA: Float? = null,
    val lvVoltage: Float? = null,
    val charging: Boolean? = null,
    val tires: List<Tire> = List(4) { Tire() },
    val doors: Doors = Doors(),
    val belts: Belts = Belts(),
    val turn: TurnSignal = TurnSignal.NONE,
    /** ไฟหน้า/ไฟหรี่เปิดอยู่ (null = อ่านไม่ได้) */
    val headlights: Boolean? = null,
) {
    fun tire(i: Int): Tire = tires.getOrElse(i) { Tire() }

    /** อยู่เกียร์ถอย — แอปต้องปล่อยกล้องให้ระบบกล้องถอยของรถ */
    val reversing: Boolean get() = gear.equals("R", ignoreCase = true)
}
