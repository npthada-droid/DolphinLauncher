package th.dolphin.launcher.util

import android.content.Context
import android.net.Uri
import android.widget.Toast
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

private val L = Locale.US

fun pressureText(bar: Float?, unit: String): String = when {
    bar == null -> "--"
    unit == "psi" -> String.format(L, "%.0f", bar * 14.5038f)
    unit == "kpa" -> String.format(L, "%.0f", bar * 100f)
    else -> String.format(L, "%.1f", bar)
}

fun pressureUnit(unit: String): String = when (unit) {
    "psi" -> "psi"
    "kpa" -> "kPa"
    else -> "bar"
}

fun speedValue(kmh: Float, unit: String): Int =
    if (unit == "mph") (kmh * 0.621371f).roundToInt() else kmh.roundToInt()

fun speedUnit(unit: String): String = if (unit == "mph") "mph" else "km/h"

fun tempText(c: Float?, unit: String): String = when {
    c == null -> "--"
    unit == "f" -> String.format(L, "%.0f°F", c * 9f / 5f + 32f)
    else -> String.format(L, "%.0f°C", c)
}

fun distText(km: Float?, unit: String): String = when {
    km == null -> "--"
    unit == "mph" -> String.format(L, "%,.0f mi", km * 0.621371f)
    else -> String.format(L, "%,.0f km", km)
}

fun lapText(ms: Long): String {
    val m = ms / 60000
    val s = (ms / 1000) % 60
    val cs = (ms / 10) % 100
    return String.format(L, "%d:%02d.%02d", m, s, cs)
}

fun fmt1(v: Float): String = String.format(L, "%.1f", v)

fun Context.toast(msg: String) = Toast.makeText(this, th.dolphin.launcher.i18n.I18n.tr(msg), Toast.LENGTH_SHORT).show()

/** คัดลอกรูปที่ผู้ใช้เลือกเข้ามาเก็บในแอป แล้วลบไฟล์เก่าของ prefix เดียวกัน */
fun copyImageToFiles(ctx: Context, uri: Uri, prefix: String): String? = runCatching {
    val dst = File(ctx.filesDir, "$prefix-${System.currentTimeMillis()}.img")
    ctx.contentResolver.openInputStream(uri)!!.use { input -> dst.outputStream().use { input.copyTo(it) } }
    ctx.filesDir.listFiles()?.filter { it.name.startsWith("$prefix-") && it.name != dst.name }?.forEach { it.delete() }
    dst.absolutePath
}.getOrNull()
