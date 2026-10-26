package th.dolphin.launcher.vehicle

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import java.io.File
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/** หนึ่งบรรทัดใน mapping: คลาส#เมธอด(args) | ตัวเลือก */
data class ApiSpec(
    val cls: String,
    val method: String,
    val args: List<Int>,
    val scale: Double = 1.0,
    val offset: Double = 0.0,
    val trueValues: Set<Int>? = null,
    val map: Map<Int, String> = emptyMap(),
)

object BydApiMap {
    /**
     * ค่าเริ่มต้นเป็น "ค่าคาดการณ์" จากโครงสร้าง BYD Auto API ที่พบบ่อยใน DiLink
     * ชื่อคลาส/เมธอด/อาร์กิวเมนต์อาจต่างกันตามรุ่นเฟิร์มแวร์ → ตรวจสอบด้วยเมนู "สแกน BYD API" แล้วแก้ในตั้งค่า
     */
    val DEFAULT: String = """
# BYD Auto API mapping (แก้ไขได้)
# รูปแบบ:  key = ชื่อคลาส#เมธอด(อาร์กิวเมนต์) | scale=ตัวคูณ | offset=ค่าบวก | true=ค่าที่แปลว่า "ใช่" | map=ค่า:ข้อความ
# ประตู: true = เปิดอยู่   เข็มขัด: true = คาดแล้ว   ไฟเลี้ยว: true = กำลังกระพริบ/เปิด
# ลมยาง: หน่วยปลายทางคือ bar (ถ้า API ให้ kPa ใช้ scale=0.01)

speed = android.hardware.bydauto.speed.BYDAutoSpeedDevice#getCurrentSpeed()
soc = android.hardware.bydauto.statistic.BYDAutoStatisticDevice#getElecPercentageValue()
range = android.hardware.bydauto.statistic.BYDAutoStatisticDevice#getEVMileageValue()
odometer = android.hardware.bydauto.statistic.BYDAutoStatisticDevice#getTotalMileageValue()
power = android.hardware.bydauto.engine.BYDAutoEngineDevice#getEnginePower()
gear = android.hardware.bydauto.gearbox.BYDAutoGearboxDevice#getGearboxAutoModeType() | map=1:P,2:R,3:N,4:D
# reverse = (ถ้ามีเมธอดบอกเกียร์ถอยแยก ใส่ที่นี่ | true=ค่าที่หมายถึงถอย) — ใช้ปล่อยกล้องให้ระบบกล้องถอยของรถ
outside_temp = android.hardware.bydauto.ac.BYDAutoAcDevice#getTemprature(4)

# แบตเตอรี่ขับเคลื่อน (ค่าคาดการณ์ — หลายเฟิร์มแวร์ไม่เปิดให้อ่าน SOH ให้ตรวจด้วยเมนูสแกน)
soh = android.hardware.bydauto.statistic.BYDAutoStatisticDevice#getSOHValue()
hv_temp = android.hardware.bydauto.energy.BYDAutoEnergyDevice#getBatteryTemperature()
hv_voltage = android.hardware.bydauto.energy.BYDAutoEnergyDevice#getBatteryVoltage()
hv_current = android.hardware.bydauto.energy.BYDAutoEnergyDevice#getBatteryCurrent()
lv_voltage = android.hardware.bydauto.energy.BYDAutoEnergyDevice#getLowVoltageBatteryVoltage()
charging = android.hardware.bydauto.charging.BYDAutoChargingDevice#getChargingState() | true=1

tire_fl = android.hardware.bydauto.tyre.BYDAutoTyreDevice#getTyrePressureValue(1) | scale=0.01
tire_fr = android.hardware.bydauto.tyre.BYDAutoTyreDevice#getTyrePressureValue(2) | scale=0.01
tire_rl = android.hardware.bydauto.tyre.BYDAutoTyreDevice#getTyrePressureValue(3) | scale=0.01
tire_rr = android.hardware.bydauto.tyre.BYDAutoTyreDevice#getTyrePressureValue(4) | scale=0.01
tire_temp_fl = android.hardware.bydauto.tyre.BYDAutoTyreDevice#getTyreTemperatureValue(1)
tire_temp_fr = android.hardware.bydauto.tyre.BYDAutoTyreDevice#getTyreTemperatureValue(2)
tire_temp_rl = android.hardware.bydauto.tyre.BYDAutoTyreDevice#getTyreTemperatureValue(3)
tire_temp_rr = android.hardware.bydauto.tyre.BYDAutoTyreDevice#getTyreTemperatureValue(4)

door_fl = android.hardware.bydauto.bodywork.BYDAutoBodyworkDevice#getDoorState(1) | true=1
door_fr = android.hardware.bydauto.bodywork.BYDAutoBodyworkDevice#getDoorState(2) | true=1
door_rl = android.hardware.bydauto.bodywork.BYDAutoBodyworkDevice#getDoorState(3) | true=1
door_rr = android.hardware.bydauto.bodywork.BYDAutoBodyworkDevice#getDoorState(4) | true=1
trunk = android.hardware.bydauto.bodywork.BYDAutoBodyworkDevice#getDoorState(5) | true=1
hood = android.hardware.bydauto.bodywork.BYDAutoBodyworkDevice#getDoorState(6) | true=1

belt_driver = android.hardware.bydauto.safetybelt.BYDAutoSafetyBeltDevice#getSafetyBeltStatus(1) | true=1
belt_passenger = android.hardware.bydauto.safetybelt.BYDAutoSafetyBeltDevice#getSafetyBeltStatus(2) | true=1
passenger_occupied = android.hardware.bydauto.safetybelt.BYDAutoSafetyBeltDevice#getPassengerSeatStatus() | true=1

headlights = android.hardware.bydauto.light.BYDAutoLightDevice#getLightStatus(1) | true=1
turn_left = android.hardware.bydauto.light.BYDAutoLightDevice#getLightStatus(5) | true=1
turn_right = android.hardware.bydauto.light.BYDAutoLightDevice#getLightStatus(6) | true=1
""".trimIndent()

    private val specRe = Regex("""^([\w.]+)#(\w+)\(([^)]*)\)$""")

    fun parse(text: String): Map<String, ApiSpec> {
        val out = LinkedHashMap<String, ApiSpec>()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val eq = line.indexOf('=')
            if (eq <= 0) continue
            val key = line.substring(0, eq).trim()
            val parts = line.substring(eq + 1).split('|').map { it.trim() }
            val m = specRe.matchEntire(parts[0]) ?: continue
            val args = m.groupValues[3].split(',').map { it.trim() }.filter { it.isNotEmpty() }
                .mapNotNull { it.toIntOrNull() }
            var scale = 1.0
            var offset = 0.0
            var trueValues: Set<Int>? = null
            var valueMap: Map<Int, String> = emptyMap()
            for (opt in parts.drop(1)) {
                val k = opt.substringBefore('=').trim()
                val v = opt.substringAfter('=', "").trim()
                when (k) {
                    "scale" -> scale = v.toDoubleOrNull() ?: 1.0
                    "offset" -> offset = v.toDoubleOrNull() ?: 0.0
                    "true" -> trueValues = v.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()
                    "map" -> valueMap = v.split(',').mapNotNull { p ->
                        val a = p.split(':')
                        if (a.size == 2) a[0].trim().toIntOrNull()?.let { it to a[1].trim() } else null
                    }.toMap()
                    else -> {}
                }
            }
            out[key] = ApiSpec(m.groupValues[1], m.groupValues[2], args, scale, offset, trueValues, valueMap)
        }
        return out
    }
}

/** เรียก BYD device class ผ่าน reflection พร้อม cache — ไม่ crash ถ้าไม่พบ */
class BydReflect(private val ctx: Context) {
    private val devices = HashMap<String, Any>()
    private val failed = HashSet<String>()
    private val methods = HashMap<String, Method?>()

    fun device(cls: String): Any? {
        devices[cls]?.let { return it }
        if (cls in failed) return null
        val create: () -> Any? = {
            runCatching {
                val c = Class.forName(cls)
                runCatching { c.getMethod("getInstance", Context::class.java).invoke(null, ctx) }.getOrNull()
                    ?: runCatching { c.getMethod("getInstance").invoke(null) }.getOrNull()
            }.getOrNull()
        }
        // คลาสของ BYD บางตัวสร้าง Handler ภายใน จึงต้องสร้างบน main thread (รอไม่เกิน 2 วินาที กันจอค้าง)
        val inst: Any? = if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) create() else {
            var r: Any? = null
            val latch = java.util.concurrent.CountDownLatch(1)
            android.os.Handler(android.os.Looper.getMainLooper()).post { r = create(); latch.countDown() }
            if (latch.await(2, java.util.concurrent.TimeUnit.SECONDS)) r else create()
        }
        if (inst == null) failed += cls else devices[cls] = inst
        return inst
    }

    private val failures = HashMap<String, Int>()
    private val disabled = HashSet<String>()

    /** คำสั่งที่ถูกปิดเพราะ error ซ้ำ (แสดงในหน้าสแกน) */
    val disabledSpecs: Set<String> get() = disabled

    /**
     * เรียกเมธอดตาม spec — ถ้า error ติดกัน 3 ครั้ง จะเลิกเรียกคำสั่งนั้นถาวร (จนกว่าจะเปลี่ยน mapping)
     * เพื่อไม่ให้ยิงคำสั่งที่ล้มเหลวเข้าไปยังระบบของ BYD ซ้ำๆ
     */
    fun raw(spec: ApiSpec): Any? {
        val key = "${spec.cls}#${spec.method}(${spec.args.joinToString(",")})"
        if (key in disabled) return null
        val d = device(spec.cls) ?: return null
        val mk = "${spec.cls}#${spec.method}/${spec.args.size}"
        val m = methods.getOrPut(mk) {
            d.javaClass.methods.firstOrNull { it.name == spec.method && it.parameterTypes.size == spec.args.size }
        } ?: return null
        val args: Array<Any?> = spec.args.map<Int, Any?> { it }.toTypedArray()
        return try {
            m.invoke(d, *args).also { failures.remove(key) }
        } catch (t: Throwable) {
            val n = (failures[key] ?: 0) + 1
            failures[key] = n
            if (n >= 3) disabled += key
            null
        }
    }

    fun num(spec: ApiSpec): Double? = when (val v = raw(spec)) {
        is Number -> v.toDouble() * spec.scale + spec.offset
        is Boolean -> if (v) 1.0 else 0.0
        is IntArray -> v.firstOrNull()?.let { it * spec.scale + spec.offset }
        else -> null
    }

    fun bool(spec: ApiSpec): Boolean? {
        val n = when (val v = raw(spec)) {
            is Boolean -> return v
            is Number -> v.toInt()
            else -> return null
        }
        return spec.trueValues?.contains(n) ?: (n != 0)
    }
}

/** สแกนหา BYD API ในรถ เพื่อใช้แก้ mapping ให้ตรงกับเฟิร์มแวร์ */
object BydProbe {
    private const val P = "android.hardware.bydauto"
    val CANDIDATES = listOf(
        "$P.BYDAutoManager", "$P.AbsBYDAutoDevice",
        "$P.bodywork.BYDAutoBodyworkDevice", "$P.tyre.BYDAutoTyreDevice",
        "$P.safetybelt.BYDAutoSafetyBeltDevice", "$P.light.BYDAutoLightDevice",
        "$P.speed.BYDAutoSpeedDevice", "$P.statistic.BYDAutoStatisticDevice",
        "$P.gearbox.BYDAutoGearboxDevice", "$P.ac.BYDAutoAcDevice",
        "$P.panorama.BYDAutoPanoramaDevice", "$P.doorlock.BYDAutoDoorLockDevice",
        "$P.instrument.BYDAutoInstrumentDevice", "$P.energy.BYDAutoEnergyDevice",
        "$P.engine.BYDAutoEngineDevice", "$P.radar.BYDAutoRadarDevice",
        "$P.sensor.BYDAutoSensorDevice", "$P.setting.BYDAutoSettingDevice",
        "$P.charging.BYDAutoChargingDevice", "$P.signal.BYDAutoSignalDevice",
    )

    /** โหลดคลาสโดยไม่รัน static initializer (ปลอดภัย: ไม่มีโค้ดของ BYD ถูกเรียก) */
    private fun peek(cn: String): Class<*>? =
        runCatching { Class.forName(cn, false, BydProbe::class.java.classLoader) }.getOrNull()

    @Suppress("DEPRECATION")
    private fun frameworkClasses(): Set<String> {
        val found = sortedSetOf<String>()
        runCatching {
            File("/system/framework").listFiles { f -> f.name.endsWith(".jar") }?.forEach { jar ->
                runCatching {
                    val dex = dalvik.system.DexFile(jar.path)
                    val e = dex.entries()
                    while (e.hasMoreElements()) {
                        val n = e.nextElement()
                        if (n.contains("bydauto", true) && !n.contains('$')) found += n
                    }
                    dex.close()
                }
            }
        }
        return found
    }

    /** รายชื่อคลาสที่มีอยู่จริงในรถ (ไม่เรียกโค้ดใดๆ ของ BYD) — ใช้ให้ผู้ใช้เลือกคลาสที่จะอ่านค่า */
    fun loadableClasses(mapText: String): List<String> =
        (CANDIDATES + frameworkClasses() + BydApiMap.parse(mapText).values.map { it.cls })
            .distinct().filter { peek(it) != null }

    /**
     * @param readClasses คลาสที่อนุญาตให้ "อ่านค่า" (สร้าง instance + เรียก getter ไม่มีพารามิเตอร์)
     *                    ว่าง = โหมดปลอดภัย: แสดงแค่ชื่อคลาส/เมธอด/ชื่อค่าคงที่ ไม่เรียกโค้ดของ BYD เลย
     * @param testMapping อ่านค่าตาม mapping ปัจจุบัน (เหมือนที่แอปทำในโหมด BYD API)
     */
    fun run(ctx: Context, mapText: String, readClasses: Set<String> = emptySet(), testMapping: Boolean = false): String = buildString {
        appendLine("=== Dolphin Launcher · BYD API Probe ===")
        appendLine("อุปกรณ์: ${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("Build: ${Build.DISPLAY}")
        appendLine()

        appendLine(if (readClasses.isEmpty() && !testMapping) "โหมด: แสดงรายชื่ออย่างเดียว (ปลอดภัย)" else "โหมด: อ่านค่า ${readClasses.size} คลาส" + if (testMapping) " + ทดสอบ mapping" else "")
        appendLine()

        // 1) ค้นชื่อคลาส bydauto จาก framework jar
        appendLine("--- คลาสที่มีคำว่า bydauto ใน /system/framework ---")
        val found = frameworkClasses()
        if (found.isEmpty()) appendLine("(ไม่พบ หรือระบบไม่อนุญาตให้อ่าน)") else found.forEach { appendLine(it) }
        appendLine()

        // 2) สิทธิ์ที่เกี่ยวกับ BYD
        appendLine("--- สิทธิ์ (permission) ที่มีคำว่า BYD ---")
        val pm = ctx.packageManager
        val perms = sortedSetOf<String>()
        runCatching {
            pm.queryPermissionsByGroup(null, 0)?.forEach { if (it.name.contains("byd", true)) perms += it.name }
            pm.getAllPermissionGroups(0).forEach { g ->
                runCatching { pm.queryPermissionsByGroup(g.name, 0) }.getOrNull()
                    ?.forEach { if (it.name.contains("byd", true)) perms += it.name }
            }
        }
        if (perms.isEmpty()) appendLine("(ไม่พบ)")
        perms.forEach { p ->
            val ok = ctx.checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED
            appendLine("${if (ok) "✓" else "✗"} $p")
        }
        appendLine()

        // 3) แอปที่อาจเป็นกล้อง 360 / แอปรถ
        appendLine("--- แพ็กเกจที่น่าสนใจ (byd / avm / panorama / camera / 360) ---")
        runCatching {
            pm.getInstalledPackages(0).map { it.packageName }.filter { n ->
                listOf("byd", "avm", "panorama", "camera", "360", "dilink", "carlife", "carplay", "projection", "gearhead", "mirror", "xxy.hr").any { n.contains(it, true) }
            }.sorted().forEach { pkg ->
                val launch = pm.getLaunchIntentForPackage(pkg)?.component?.flattenToShortString() ?: "-"
                appendLine("$pkg   [เปิดได้: $launch]")
            }
        }
        appendLine()

        // 4) เมธอดของแต่ละคลาส (อ่านค่าเฉพาะคลาสที่ผู้ใช้เลือก)
        val r = BydReflect(ctx)
        val specs = BydApiMap.parse(mapText)
        val classes = (CANDIDATES + found + specs.values.map { it.cls }).distinct()
        appendLine("--- รายละเอียดคลาส (✓ = มีในรถ, ★ = อ่านค่า) ---")
        for (cn in classes) {
            val c = peek(cn)
            if (c == null) { appendLine("✗ $cn"); continue }
            val read = cn in readClasses
            val inst = if (read) r.device(cn) else null
            appendLine((if (read) "★ " else "✓ ") + cn + if (read) "   instance=${if (inst != null) "ได้" else "ไม่ได้"}" else "")
            val ms = c.declaredMethods.filter { Modifier.isPublic(it.modifiers) }.sortedBy { it.name }
            for (m in ms) {
                val sig = "${m.name}(${m.parameterTypes.joinToString { it.simpleName }}): ${m.returnType.simpleName}"
                val readable = inst != null && m.parameterTypes.isEmpty() &&
                    (m.name.startsWith("get") || m.name.startsWith("is")) &&
                    (m.returnType.isPrimitive || m.returnType == String::class.java)
                val value = if (readable) {
                    runCatching { m.invoke(inst) }.fold({ " = $it" }, { " = ERR ${(it.cause ?: it).javaClass.simpleName}" })
                } else ""
                appendLine("    $sig$value")
            }
            // ค่าคงที่ (static int) บอกความหมายของอาร์กิวเมนต์ เช่น DOOR_LEFT_FRONT = 1
            // การอ่าน "ค่า" ต้องรัน static initializer จึงทำเฉพาะคลาสที่เลือกอ่านค่า
            c.declaredFields.filter { Modifier.isStatic(it.modifiers) && it.type == Int::class.javaPrimitiveType }
                .take(150).forEach { f ->
                    if (read) runCatching { f.isAccessible = true; appendLine("    const ${f.name} = ${f.getInt(null)}") }
                    else appendLine("    const ${f.name}")
                }
        }
        appendLine()

        // 5) ทดสอบ mapping ปัจจุบัน
        if (testMapping) {
            appendLine("--- ทดสอบ mapping ปัจจุบัน ---")
            specs.forEach { (k, s) ->
                val v = runCatching { r.raw(s) }.getOrNull()
                appendLine("$k = ${v ?: "null (อ่านไม่ได้)"}")
            }
            appendLine()
        }

        // 6) กล้อง
        appendLine("--- กล้อง (Camera2) ---")
        runCatching {
            val cm = ctx.getSystemService(CameraManager::class.java)
            cm.cameraIdList.forEach { id ->
                val ch = cm.getCameraCharacteristics(id)
                val facing = when (ch.get(CameraCharacteristics.LENS_FACING)) {
                    CameraCharacteristics.LENS_FACING_FRONT -> "front"
                    CameraCharacteristics.LENS_FACING_BACK -> "back"
                    CameraCharacteristics.LENS_FACING_EXTERNAL -> "external"
                    else -> "?"
                }
                val sizes = ch.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    ?.getOutputSizes(android.graphics.SurfaceTexture::class.java)?.take(4)?.joinToString() ?: "-"
                appendLine("id=$id facing=$facing sizes=$sizes")
            }
        }.onFailure { appendLine("อ่านกล้องไม่ได้: ${it.message}") }
    }
}

/** เปิดทางให้ reflection เรียกคลาสภายในของระบบได้ (ใช้ได้ถึง Android 10 ซึ่งเป็นเวอร์ชันของ DiLink ส่วนใหญ่) */
object HiddenApi {
    fun exempt() {
        if (Build.VERSION.SDK_INT < 28) return
        runCatching {
            val forName = Class::class.java.getDeclaredMethod("forName", String::class.java)
            val getDeclaredMethod = Class::class.java.getDeclaredMethod(
                "getDeclaredMethod", String::class.java, arrayOf<Class<*>>()::class.java
            )
            val vmRuntime = forName.invoke(null, "dalvik.system.VMRuntime") as Class<*>
            val getRuntime = getDeclaredMethod.invoke(vmRuntime, "getRuntime", null) as Method
            val setExemptions = getDeclaredMethod.invoke(
                vmRuntime, "setHiddenApiExemptions", arrayOf<Class<*>>(Array<String>::class.java)
            ) as Method
            setExemptions.invoke(getRuntime.invoke(null), arrayOf("L"))
        }
    }
}
