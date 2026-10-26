package th.dolphin.launcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import th.dolphin.launcher.settings.AppSettings
import java.util.Calendar

data class UiPrefs(
    val corner: Dp = 26.dp,
    val opacity: Float = 0.62f,
    val dark: Boolean = true,
    val accent: Color = Color(0xFF2EC4B6),
    val accent2: Color = Color(0xFF3B82F6),
    val showGrid: Boolean = true,
)

val LocalUi = staticCompositionLocalOf { UiPrefs() }

object StatusColors {
    val ok = Color(0xFF34D399)
    val warn = Color(0xFFFBBF24)
    val bad = Color(0xFFF87171)
    val info = Color(0xFF60A5FA)
}

@Composable
fun rememberIsDark(cfg: AppSettings): Boolean {
    val system = isSystemInDarkTheme()
    val hour by produceState(Calendar.getInstance().get(Calendar.HOUR_OF_DAY), cfg.themeMode) {
        while (true) {
            value = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            delay(30_000)
        }
    }
    val weather = androidx.compose.ui.platform.LocalContext.current.let { c ->
        (c.applicationContext as? th.dolphin.launcher.App)?.weather?.weather
    }?.collectAsState()?.value
    val nowMs by produceState(System.currentTimeMillis()) {
        while (true) { value = System.currentTimeMillis(); delay(60_000) }
    }
    // ไฟหน้า: หน่วงเวลาก่อนสลับ กันจอกระพริบเมื่อเปิด-ปิดไฟสั้นๆ
    val appRef = androidx.compose.ui.platform.LocalContext.current.applicationContext as? th.dolphin.launcher.App
    val hlNow = appRef?.vehicle?.state?.collectAsState()?.value?.headlights
    var hlStable by remember { mutableStateOf(hlNow) }
    LaunchedEffect(hlNow, cfg.lightDelaySec) {
        if (hlNow != hlStable) {
            delay(cfg.lightDelaySec.coerceIn(0, 30) * 1000L)
            hlStable = hlNow
        }
    }
    return when (cfg.themeMode) {
        "headlights" -> hlStable ?: run {
            // อ่านสถานะไฟหน้าไม่ได้ → ใช้ตามพระอาทิตย์/เวลาแทน
            val rise = weather?.sunrise
            val set = weather?.sunset
            if (rise != null && set != null) {
                val day = 86_400_000L
                val tz = java.util.TimeZone.getDefault()
                fun tod(t: Long) = ((t + tz.getOffset(t)) % day + day) % day
                val n = tod(nowMs)
                n < tod(rise) || n >= tod(set)
            } else hour < cfg.autoLightFrom || hour >= cfg.autoDarkFrom
        }
        "sun" -> {
            val rise = weather?.sunrise
            val set = weather?.sunset
            if (rise != null && set != null) {
                // เวลาเป็นของวันนี้ — เทียบเฉพาะชั่วโมง:นาทีของวัน
                val day = 86_400_000L
                val tz = java.util.TimeZone.getDefault()
                fun tod(t: Long) = ((t + tz.getOffset(t)) % day + day) % day
                val n = tod(nowMs)
                n < tod(rise) || n >= tod(set)
            } else {
                hour < cfg.autoLightFrom || hour >= cfg.autoDarkFrom
            }
        }
        "dark" -> true
        "light" -> false
        "auto" -> if (cfg.autoLightFrom <= cfg.autoDarkFrom) {
            hour < cfg.autoLightFrom || hour >= cfg.autoDarkFrom
        } else {
            hour in cfg.autoDarkFrom until cfg.autoLightFrom
        }
        else -> system
    }
}

@Composable
fun LauncherTheme(cfg: AppSettings, content: @Composable () -> Unit) {
    val dark = rememberIsDark(cfg)
    val accent = Color(cfg.accent)
    val scheme = if (dark) {
        darkColorScheme(
            primary = accent,
            onPrimary = Color(0xFF041014),
            primaryContainer = accent.copy(alpha = 0.22f),
            onPrimaryContainer = Color.White,
            secondary = accent,
            background = Color(0xFF04060A),
            onBackground = Color(0xFFEAF0F7),
            surface = Color(0xFF141C28),
            onSurface = Color(0xFFEAF0F7),
            surfaceVariant = Color(0xFF1A212D),
            onSurfaceVariant = Color(0xFF8693A6),
            outline = Color(0x17FFFFFF),
            outlineVariant = Color(0x14FFFFFF),
            error = StatusColors.bad,
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = accent.copy(alpha = 0.16f),
            onPrimaryContainer = Color(0xFF0B1220),
            secondary = accent,
            background = Color(0xFFE9EEF4),
            onBackground = Color(0xFF0D141C),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF0D141C),
            surfaceVariant = Color(0xFFE4E9EF),
            onSurfaceVariant = Color(0xFF5A6676),
            outline = Color(0x140A1428),
            outlineVariant = Color(0x10000000),
            error = Color(0xFFDC2626),
        )
    }
    CompositionLocalProvider(
        th.dolphin.launcher.i18n.LocalLang provides th.dolphin.launcher.i18n.I18n.resolve(cfg.language),
        LocalUi provides UiPrefs(cfg.cornerRadius.dp, cfg.cardOpacity, dark, accent, Color(cfg.accent2), cfg.showGrid),
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
