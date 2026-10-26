package th.dolphin.launcher.alerts

import th.dolphin.launcher.settings.AppSettings
import th.dolphin.launcher.util.pressureText
import th.dolphin.launcher.util.pressureUnit
import th.dolphin.launcher.util.tempText
import th.dolphin.launcher.vehicle.Tire
import th.dolphin.launcher.vehicle.VehicleState

enum class AlertLevel { INFO, WARN, CRITICAL }

data class VehicleAlert(val id: String, val level: AlertLevel, val title: String, val detail: String)

enum class TireStatus { UNKNOWN, OK, LOW, HIGH, HOT }

fun tireStatus(t: Tire, c: AppSettings): TireStatus {
    val p = t.pressureBar
    val temp = t.tempC
    return when {
        temp != null && temp >= c.tireHotC -> TireStatus.HOT
        p == null -> TireStatus.UNKNOWN
        p < c.tireLowBar -> TireStatus.LOW
        p > c.tireHighBar -> TireStatus.HIGH
        else -> TireStatus.OK
    }
}

fun tireStatusText(s: TireStatus): String = when (s) {
    TireStatus.OK -> "ปกติ"
    TireStatus.LOW -> "ลมอ่อน"
    TireStatus.HIGH -> "ลมสูง"
    TireStatus.HOT -> "ร้อนเกิน"
    TireStatus.UNKNOWN -> "ไม่มีข้อมูล"
}

private val TIRE_NAMES = listOf("หน้าซ้าย", "หน้าขวา", "หลังซ้าย", "หลังขวา")

object AlertEngine {
    fun evaluate(s: VehicleState, c: AppSettings): List<VehicleAlert> {
        val out = mutableListOf<VehicleAlert>()

        if (c.doorAlert && s.doors.anyOpen) {
            val critical = s.speedKmh >= c.doorAlertSpeed
            out += VehicleAlert(
                "door", if (critical) AlertLevel.CRITICAL else AlertLevel.WARN,
                "ประตูปิดไม่สนิท", s.doors.openNames().joinToString(" · ")
            )
        }

        if (c.beltAlert) {
            val who = buildList {
                if (s.belts.driver == false) add("ผู้ขับ")
                if (s.belts.passenger == false && s.belts.passengerOccupied) add("ผู้โดยสารด้านหน้า")
            }
            if (who.isNotEmpty()) {
                val critical = s.speedKmh >= c.beltAlertSpeed
                out += VehicleAlert(
                    "belt", if (critical) AlertLevel.CRITICAL else AlertLevel.WARN,
                    "ไม่ได้คาดเข็มขัดนิรภัย", who.joinToString(" · ")
                )
            }
        }

        if (c.tireAlert) {
            s.tires.forEachIndexed { i, t ->
                val name = TIRE_NAMES.getOrElse(i) { "ยาง ${i + 1}" }
                val p = "${pressureText(t.pressureBar, c.pressureUnit)} ${pressureUnit(c.pressureUnit)}"
                when (tireStatus(t, c)) {
                    TireStatus.LOW -> out += VehicleAlert("tire$i", AlertLevel.WARN, "ลมยางอ่อน", "$name $p")
                    TireStatus.HIGH -> out += VehicleAlert("tire$i", AlertLevel.WARN, "ลมยางสูงเกิน", "$name $p")
                    TireStatus.HOT -> out += VehicleAlert(
                        "tire$i", AlertLevel.CRITICAL, "ยางร้อนเกิน", "$name ${tempText(t.tempC, c.tempUnit)}"
                    )
                    else -> {}
                }
            }
        }
        if (c.overspeedKmh > 0 && s.speedKmh > c.overspeedKmh) {
            out += VehicleAlert(
                "speed", AlertLevel.CRITICAL, "ขับเร็วเกินกำหนด",
                "${s.speedKmh.toInt()} km/h · ตั้งไว้ไม่เกิน ${c.overspeedKmh} km/h",
            )
        }

        Maintenance.status(s, c)?.let { m ->
            if (m.due) out += VehicleAlert("service", AlertLevel.WARN, "ถึงกำหนดเข้าศูนย์บริการ", m.text)
        }
        return out.sortedByDescending { it.level.ordinal }
    }
}

data class MaintenanceStatus(val remainingKm: Float?, val remainingDays: Long?, val due: Boolean, val text: String)

/** เตือนเข้าศูนย์ตามระยะทาง/เวลา — ใช้ได้เมื่อบันทึกการเข้าศูนย์ครั้งล่าสุดไว้แล้ว */
object Maintenance {
    fun status(s: VehicleState, c: AppSettings): MaintenanceStatus? {
        if (c.lastServiceDate == 0L && c.lastServiceOdo == 0f) return null
        val km = s.odometerKm?.let { c.lastServiceOdo + c.serviceEveryKm - it }
        val days = if (c.lastServiceDate > 0L) {
            val dueAt = java.util.Calendar.getInstance().apply {
                timeInMillis = c.lastServiceDate
                add(java.util.Calendar.MONTH, c.serviceEveryMonths)
            }.timeInMillis
            (dueAt - System.currentTimeMillis()) / 86_400_000L
        } else null
        val due = (km != null && km <= 0f) || (days != null && days <= 0L)
        val parts = buildList {
            km?.let { add(if (it > 0) "อีก ${it.toInt()} km" else "เกินมา ${(-it).toInt()} km") }
            days?.let { add(if (it > 0) "อีก $it วัน" else "เกินมา ${-it} วัน") }
        }
        return MaintenanceStatus(km, days, due, parts.joinToString(" · ").ifBlank { "—" })
    }
}
