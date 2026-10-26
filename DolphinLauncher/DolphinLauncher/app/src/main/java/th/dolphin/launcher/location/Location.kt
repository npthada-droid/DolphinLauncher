package th.dolphin.launcher.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.Locale

data class GeoFix(
    val lat: Double,
    val lon: Double,
    val speedKmh: Float?,
    val bearing: Float?,
    val altitudeM: Double?,
    val accuracyM: Float?,
    val time: Long,
)

/** ตำแหน่ง GPS: ความเร็วสำรอง, เข็มทิศ, ความสูง, และพิกัดสำหรับสภาพอากาศ */
class LocationRepository(private val ctx: Context) {
    private val lm = ctx.getSystemService(LocationManager::class.java)
    private val _fix = MutableStateFlow<GeoFix?>(null)
    val fix: StateFlow<GeoFix?> = _fix.asStateFlow()
    private val _permitted = MutableStateFlow(false)
    val permitted: StateFlow<Boolean> = _permitted.asStateFlow()
    private var started = false

    private val listener = object : LocationListener {
        override fun onLocationChanged(l: Location) = publish(l)
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private fun publish(l: Location) {
        _fix.value = GeoFix(
            l.latitude, l.longitude,
            if (l.hasSpeed()) l.speed * 3.6f else null,
            if (l.hasBearing()) l.bearing else null,
            if (l.hasAltitude()) l.altitude else null,
            if (l.hasAccuracy()) l.accuracy else null,
            l.time,
        )
    }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** เรียกซ้ำได้ — เริ่มรับตำแหน่งเมื่อได้รับสิทธิ์แล้ว */
    @SuppressLint("MissingPermission")
    fun start() {
        _permitted.value = hasPermission()
        if (started || !_permitted.value || lm == null) return
        runCatching {
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).forEach { p ->
                if (lm.allProviders.contains(p)) {
                    lm.getLastKnownLocation(p)?.let { if (_fix.value == null) publish(it) }
                    lm.requestLocationUpdates(p, 1000L, 0f, listener, Looper.getMainLooper())
                }
            }
            started = true
        }
    }
}

data class DayForecast(val dateLabel: String, val code: Int, val max: Float, val min: Float)

data class Weather(
    val tempC: Float,
    val code: Int,
    val windKmh: Float,
    val humidity: Int,
    val rainChance: Int?,
    val sunrise: Long?,
    val sunset: Long?,
    val days: List<DayForecast>,
    val updatedAt: Long,
)

object WeatherCodes {
    fun text(code: Int): String = when (code) {
        0 -> "ท้องฟ้าแจ่มใส"
        1, 2 -> "มีเมฆบางส่วน"
        3 -> "เมฆมาก"
        45, 48 -> "หมอก"
        51, 53, 55, 56, 57 -> "ฝนปรอยๆ"
        61, 63, 66, 80, 81 -> "ฝนตก"
        65, 67, 82 -> "ฝนตกหนัก"
        71, 73, 75, 77, 85, 86 -> "หิมะ"
        95, 96, 99 -> "พายุฝนฟ้าคะนอง"
        else -> "—"
    }
}

/** สภาพอากาศจาก Open-Meteo (ฟรี ไม่ต้องใช้ API key) อัปเดตทุก 30 นาที */
class WeatherRepository(private val location: LocationRepository, scope: CoroutineScope, private val enabled: () -> Boolean) {
    private val _weather = MutableStateFlow<Weather?>(null)
    val weather: StateFlow<Weather?> = _weather.asStateFlow()

    init {
        scope.launch {
            while (true) {
                val f = location.fix.value
                if (enabled() && f != null) {
                    val w = withContext(Dispatchers.IO) { runCatching { fetch(f.lat, f.lon) }.getOrNull() }
                    if (w != null) _weather.value = w
                }
                delay(if (_weather.value == null) 60_000L else 30 * 60_000L)
            }
        }
    }

    private fun fetch(lat: Double, lon: Double): Weather {
        val url = URL(
            String.format(
                Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.3f&longitude=%.3f" +
                    "&current=temperature_2m,weather_code,wind_speed_10m,relative_humidity_2m" +
                    "&hourly=precipitation_probability&forecast_hours=3" +
                    "&daily=sunrise,sunset,temperature_2m_max,temperature_2m_min,weather_code" +
                    "&timezone=auto&forecast_days=3&timeformat=unixtime",
                lat, lon,
            )
        )
        val conn = (url.openConnection() as HttpURLConnection).apply { connectTimeout = 8000; readTimeout = 8000 }
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()
        val o = JSONObject(body)
        val cur = o.getJSONObject("current")
        val daily = o.getJSONObject("daily")
        val times = daily.getJSONArray("time")
        val dayNames = listOf("อา.", "จ.", "อ.", "พ.", "พฤ.", "ศ.", "ส.")
        val days = List(times.length()) { i ->
            val c = Calendar.getInstance().apply { timeInMillis = times.getLong(i) * 1000 }
            DayForecast(
                if (i == 0) "วันนี้" else dayNames[c.get(Calendar.DAY_OF_WEEK) - 1],
                daily.getJSONArray("weather_code").optInt(i),
                daily.getJSONArray("temperature_2m_max").optDouble(i).toFloat(),
                daily.getJSONArray("temperature_2m_min").optDouble(i).toFloat(),
            )
        }
        val rain = o.optJSONObject("hourly")?.optJSONArray("precipitation_probability")?.let { a ->
            (0 until a.length()).maxOfOrNull { a.optInt(it) }
        }
        return Weather(
            tempC = cur.optDouble("temperature_2m").toFloat(),
            code = cur.optInt("weather_code"),
            windKmh = cur.optDouble("wind_speed_10m").toFloat(),
            humidity = cur.optInt("relative_humidity_2m"),
            rainChance = rain,
            sunrise = daily.optJSONArray("sunrise")?.optLong(0)?.times(1000),
            sunset = daily.optJSONArray("sunset")?.optLong(0)?.times(1000),
            days = days,
            updatedAt = System.currentTimeMillis(),
        )
    }
}
