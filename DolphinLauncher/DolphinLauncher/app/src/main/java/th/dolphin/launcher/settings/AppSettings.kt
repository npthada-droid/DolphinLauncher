package th.dolphin.launcher.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import th.dolphin.launcher.vehicle.BydApiMap

data class AppSettings(
    // ---- หน้าตา ----
    val language: String = "system",          // system | th | en
    val themeMode: String = "dark",          // dark | light | headlights | auto | sun | system
    val autoLightFrom: Int = 6,
    val autoDarkFrom: Int = 18,
    val accent: Long = 0xFF2EC4B6,
    val accent2: Long = 0xFF3B82F6,
    val showGrid: Boolean = true,
    val cardOpacity: Float = 0.38f,
    val textGlow: Boolean = true,           // เงาตัวหนังสือ อ่านง่ายบนการ์ดโปร่งใส
    val glassShine: Boolean = true,         // ไล่แสงแบบกระจกบนการ์ด
    val cornerRadius: Int = 26,
    val layoutMode: String = "auto",         // auto | landscape | portrait
    val use24h: Boolean = true,
    val showSeconds: Boolean = false,
    val immersive: Boolean = false,
    val wallpaperPath: String = "",
    val wallpaperDim: Float = 0.45f,
    // ---- รถ ----
    val carName: String = "BYD Dolphin",
    val plate: String = "",
    val carImagePath: String = "",
    val rightHandDrive: Boolean = true,
    // ---- หน่วย ----
    val speedUnit: String = "kmh",           // kmh | mph
    val pressureUnit: String = "bar",        // bar | psi | kpa
    val tempUnit: String = "c",              // c | f
    // ---- ลมยาง ----
    val tireAlert: Boolean = true,
    val tireLowBar: Float = 2.2f,
    val tireHighBar: Float = 3.0f,
    val tireHotC: Float = 75f,
    // ---- แจ้งเตือน ----
    val doorAlert: Boolean = true,
    val doorAlertSpeed: Int = 3,
    val beltAlert: Boolean = true,
    val beltAlertSpeed: Int = 10,
    val alertSound: Boolean = true,
    val overlayAlerts: Boolean = true,
    // ---- กล้อง ----
    val turnCamEnabled: Boolean = true,
    val turnCamMode: String = "internal",    // internal | app
    val turnCamOnHazard: Boolean = false,
    val leftCamId: String = "",
    val rightCamId: String = "",
    val turnCamHoldSec: Int = 3,
    val turnCamSize: Float = 0.42f,
    val turnCamAppPkg: String = "",
    val panoramaPkg: String = "",
    val projectionPkg: String = "",
    // ---- เสียง & โทรศัพท์ ----
    val radioPkg: String = "",
    val btMusicPkg: String = "",
    val musicPkg: String = "",
    val phonePkg: String = "",
    val favorites: String = "",              // "ชื่อ=เบอร์" บรรทัดละคน
    // ---- ปุ่มลอย ----
    val bubbleEnabled: Boolean = false,
    val bubbleX: Int = 24,
    val bubbleY: Int = 260,
    val cameraNames: String = "",
    // ---- ข้อมูลรถ ----
    val dataSource: String = "sim",          // sim | byd
    val pollMs: Int = 300,
    val bydMap: String = BydApiMap.DEFAULT,
    // ---- อื่นๆ ----
    val dock: List<String> = listOf(
        "action:apps", "action:phone", "action:radio", "action:camera", "action:projection", "action:battery", "action:settings"
    ),
    val accelTarget: Int = 100,
    // ---- จัดวางหน้าหลัก ----
    val homeLeft: List<String> = listOf("energy", "safety"),
    val homeRight: List<String> = listOf("tires", "media", "phone"),
    val centerPos: String = "center",       // left | center | right
    // ---- ทริป / ชาร์จ / บำรุงรักษา ----
    val batteryKwh: Float = 60.48f,          // ความจุแบต (Dolphin Extended 60.48 / Standard 44.9)
    val elecPrice: Float = 4.5f,             // บาท/kWh
    val overspeedKmh: Int = 0,               // 0 = ปิด
    val serviceEveryKm: Int = 20000,
    val serviceEveryMonths: Int = 12,
    val lastServiceOdo: Float = 0f,
    val lastServiceDate: Long = 0L,
    // ---- ตำแหน่ง / สภาพอากาศ ----
    val weatherEnabled: Boolean = true,
    val speedSource: String = "auto",        // auto | car | gps
    val widgetsPage: Boolean = true,
    // ---- ส่วนกลางหน้าหลัก ----
    val carShowSec: Int = 10,                // แสดงรูปรถกี่วินาที
    val centerAfter: String = "auto",        // auto | map | media | car (ไม่สลับ)
    val mapSource: String = "osm",           // osm | widget
    val mapZoom: Float = 16f,
    val mapDarkTiles: Boolean = true,
    val centerWidgetId: Int = -1,
    val camGuides: Boolean = true,
    // ---- ไฟหน้า → หน้าจอ ----
    val autoBrightness: Boolean = false,
    val brightDay: Int = 220,
    val brightNight: Int = 70,
    val brightTrigger: String = "headlights", // headlights | sun
    val lightDelaySec: Int = 3,
    // ---- ระบบป้องกัน ----
    val watchdogSec: Int = 6,                // 0 = ปิด
    val guardStrict: Boolean = false,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("language", language); put("themeMode", themeMode); put("autoLightFrom", autoLightFrom); put("autoDarkFrom", autoDarkFrom)
        put("accent", accent); put("accent2", accent2); put("showGrid", showGrid); put("cardOpacity", cardOpacity.toDouble()); put("textGlow", textGlow); put("glassShine", glassShine); put("cornerRadius", cornerRadius)
        put("layoutMode", layoutMode); put("use24h", use24h); put("showSeconds", showSeconds); put("immersive", immersive)
        put("wallpaperPath", wallpaperPath); put("wallpaperDim", wallpaperDim.toDouble())
        put("carName", carName); put("plate", plate); put("carImagePath", carImagePath); put("rightHandDrive", rightHandDrive)
        put("speedUnit", speedUnit); put("pressureUnit", pressureUnit); put("tempUnit", tempUnit)
        put("tireAlert", tireAlert); put("tireLowBar", tireLowBar.toDouble()); put("tireHighBar", tireHighBar.toDouble())
        put("tireHotC", tireHotC.toDouble())
        put("doorAlert", doorAlert); put("doorAlertSpeed", doorAlertSpeed); put("beltAlert", beltAlert)
        put("beltAlertSpeed", beltAlertSpeed); put("alertSound", alertSound); put("overlayAlerts", overlayAlerts)
        put("turnCamEnabled", turnCamEnabled); put("turnCamMode", turnCamMode); put("turnCamOnHazard", turnCamOnHazard)
        put("leftCamId", leftCamId); put("rightCamId", rightCamId); put("turnCamHoldSec", turnCamHoldSec)
        put("turnCamSize", turnCamSize.toDouble()); put("turnCamAppPkg", turnCamAppPkg); put("panoramaPkg", panoramaPkg); put("projectionPkg", projectionPkg)
        put("radioPkg", radioPkg); put("btMusicPkg", btMusicPkg); put("musicPkg", musicPkg); put("phonePkg", phonePkg); put("favorites", favorites)
        put("bubbleEnabled", bubbleEnabled); put("bubbleX", bubbleX); put("bubbleY", bubbleY)
        put("cameraNames", cameraNames)
        put("dataSource", dataSource); put("pollMs", pollMs); put("bydMap", bydMap)
        put("dock", JSONArray(dock)); put("homeLeft", JSONArray(homeLeft)); put("homeRight", JSONArray(homeRight)); put("centerPos", centerPos); put("accelTarget", accelTarget)
        put("batteryKwh", batteryKwh.toDouble()); put("elecPrice", elecPrice.toDouble()); put("overspeedKmh", overspeedKmh)
        put("serviceEveryKm", serviceEveryKm); put("serviceEveryMonths", serviceEveryMonths)
        put("lastServiceOdo", lastServiceOdo.toDouble()); put("lastServiceDate", lastServiceDate)
        put("weatherEnabled", weatherEnabled); put("speedSource", speedSource); put("widgetsPage", widgetsPage)
        put("carShowSec", carShowSec); put("centerAfter", centerAfter); put("mapSource", mapSource)
        put("mapZoom", mapZoom.toDouble()); put("mapDarkTiles", mapDarkTiles); put("centerWidgetId", centerWidgetId); put("camGuides", camGuides)
        put("autoBrightness", autoBrightness); put("brightDay", brightDay); put("brightNight", brightNight)
        put("brightTrigger", brightTrigger); put("lightDelaySec", lightDelaySec)
        put("watchdogSec", watchdogSec); put("guardStrict", guardStrict)
    }

    companion object {
        fun fromJson(o: JSONObject): AppSettings {
            val d = AppSettings()
            fun s(k: String, def: String) = o.optString(k, def)
            fun b(k: String, def: Boolean) = o.optBoolean(k, def)
            fun i(k: String, def: Int) = o.optInt(k, def)
            fun f(k: String, def: Float) = o.optDouble(k, def.toDouble()).toFloat()
            val dock = o.optJSONArray("dock")?.let { a -> List(a.length()) { a.getString(it) } } ?: d.dock
            val homeLeft = o.optJSONArray("homeLeft")?.let { a -> List(a.length()) { a.getString(it) } } ?: d.homeLeft
            val homeRight = o.optJSONArray("homeRight")?.let { a -> List(a.length()) { a.getString(it) } } ?: d.homeRight
            return AppSettings(
                language = s("language", d.language), themeMode = s("themeMode", d.themeMode), autoLightFrom = i("autoLightFrom", d.autoLightFrom),
                autoDarkFrom = i("autoDarkFrom", d.autoDarkFrom), accent = o.optLong("accent", d.accent), accent2 = o.optLong("accent2", d.accent2),
                showGrid = b("showGrid", d.showGrid),
                cardOpacity = f("cardOpacity", d.cardOpacity), textGlow = b("textGlow", d.textGlow), glassShine = b("glassShine", d.glassShine), cornerRadius = i("cornerRadius", d.cornerRadius),
                layoutMode = s("layoutMode", d.layoutMode), use24h = b("use24h", d.use24h),
                showSeconds = b("showSeconds", d.showSeconds), immersive = b("immersive", d.immersive),
                wallpaperPath = s("wallpaperPath", d.wallpaperPath), wallpaperDim = f("wallpaperDim", d.wallpaperDim),
                carName = s("carName", d.carName), plate = s("plate", d.plate),
                carImagePath = s("carImagePath", d.carImagePath), rightHandDrive = b("rightHandDrive", d.rightHandDrive),
                speedUnit = s("speedUnit", d.speedUnit), pressureUnit = s("pressureUnit", d.pressureUnit),
                tempUnit = s("tempUnit", d.tempUnit), tireAlert = b("tireAlert", d.tireAlert),
                tireLowBar = f("tireLowBar", d.tireLowBar), tireHighBar = f("tireHighBar", d.tireHighBar),
                tireHotC = f("tireHotC", d.tireHotC), doorAlert = b("doorAlert", d.doorAlert),
                doorAlertSpeed = i("doorAlertSpeed", d.doorAlertSpeed), beltAlert = b("beltAlert", d.beltAlert),
                beltAlertSpeed = i("beltAlertSpeed", d.beltAlertSpeed), alertSound = b("alertSound", d.alertSound),
                overlayAlerts = b("overlayAlerts", d.overlayAlerts), turnCamEnabled = b("turnCamEnabled", d.turnCamEnabled),
                turnCamMode = s("turnCamMode", d.turnCamMode), turnCamOnHazard = b("turnCamOnHazard", d.turnCamOnHazard),
                leftCamId = s("leftCamId", d.leftCamId), rightCamId = s("rightCamId", d.rightCamId),
                turnCamHoldSec = i("turnCamHoldSec", d.turnCamHoldSec), turnCamSize = f("turnCamSize", d.turnCamSize),
                turnCamAppPkg = s("turnCamAppPkg", d.turnCamAppPkg), panoramaPkg = s("panoramaPkg", d.panoramaPkg),
                projectionPkg = s("projectionPkg", d.projectionPkg),
                radioPkg = s("radioPkg", d.radioPkg), btMusicPkg = s("btMusicPkg", d.btMusicPkg), musicPkg = s("musicPkg", d.musicPkg),
                phonePkg = s("phonePkg", d.phonePkg), favorites = s("favorites", d.favorites), bubbleEnabled = b("bubbleEnabled", d.bubbleEnabled),
                bubbleX = i("bubbleX", d.bubbleX), bubbleY = i("bubbleY", d.bubbleY),
                cameraNames = s("cameraNames", d.cameraNames), dataSource = s("dataSource", d.dataSource),
                pollMs = i("pollMs", d.pollMs), bydMap = s("bydMap", d.bydMap), dock = dock, homeLeft = homeLeft, homeRight = homeRight, centerPos = s("centerPos", d.centerPos),
                accelTarget = i("accelTarget", d.accelTarget),
                batteryKwh = f("batteryKwh", d.batteryKwh), elecPrice = f("elecPrice", d.elecPrice),
                overspeedKmh = i("overspeedKmh", d.overspeedKmh), serviceEveryKm = i("serviceEveryKm", d.serviceEveryKm),
                serviceEveryMonths = i("serviceEveryMonths", d.serviceEveryMonths),
                lastServiceOdo = f("lastServiceOdo", d.lastServiceOdo), lastServiceDate = o.optLong("lastServiceDate", d.lastServiceDate),
                weatherEnabled = b("weatherEnabled", d.weatherEnabled), speedSource = s("speedSource", d.speedSource),
                widgetsPage = b("widgetsPage", d.widgetsPage),
                carShowSec = i("carShowSec", d.carShowSec), centerAfter = s("centerAfter", d.centerAfter),
                mapSource = s("mapSource", d.mapSource), mapZoom = f("mapZoom", d.mapZoom), mapDarkTiles = b("mapDarkTiles", d.mapDarkTiles),
                centerWidgetId = i("centerWidgetId", d.centerWidgetId), camGuides = b("camGuides", d.camGuides),
                autoBrightness = b("autoBrightness", d.autoBrightness), brightDay = i("brightDay", d.brightDay),
                brightNight = i("brightNight", d.brightNight), brightTrigger = s("brightTrigger", d.brightTrigger),
                lightDelaySec = i("lightDelaySec", d.lightDelaySec),
                watchdogSec = i("watchdogSec", d.watchdogSec), guardStrict = b("guardStrict", d.guardStrict),
            )
        }
    }
}

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _flow = MutableStateFlow(load())
    val flow: StateFlow<AppSettings> = _flow.asStateFlow()
    val value: AppSettings get() = _flow.value

    private fun load(): AppSettings =
        prefs.getString("json", null)?.let { runCatching { AppSettings.fromJson(JSONObject(it)) }.getOrNull() }
            ?: AppSettings()

    private fun save() {
        prefs.edit().putString("json", _flow.value.toJson().toString()).apply()
    }

    fun update(block: (AppSettings) -> AppSettings) {
        _flow.update(block)
        save()
    }

    fun exportJson(): String = _flow.value.toJson().toString(2)

    fun importJson(text: String): Boolean = runCatching {
        _flow.value = AppSettings.fromJson(JSONObject(text))
        save()
    }.isSuccess

    fun reset() {
        _flow.value = AppSettings()
        save()
    }
}
