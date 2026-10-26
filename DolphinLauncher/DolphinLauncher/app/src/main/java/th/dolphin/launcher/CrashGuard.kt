package th.dolphin.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * กันแอปค้างวนลูปเมื่อถูกตั้งเป็น Launcher หลัก:
 * ถ้าแอปเด้ง (crash) 3 ครั้งภายใน 2 นาที → เข้า "โหมดปลอดภัย"
 * ในโหมดนี้ เมื่อกดปุ่ม Home ระบบจะเปิด Launcher เดิมของรถแทน
 * ออกจากโหมดปลอดภัยได้โดยเปิดแอปจากไอคอนตามปกติ
 */
object CrashGuard {
    private const val PREF = "crash_guard"
    private const val KEY = "times"
    private const val WINDOW_MS = 120_000L
    private const val LIMIT = 3

    fun install(ctx: Context) {
        val appCtx = ctx.applicationContext
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            runCatching {
                record(appCtx)
                val trace = android.util.Log.getStackTraceString(e).take(5000)
                appCtx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString("last", trace).commit()
                if (inSafeMode(appCtx)) {
                    SafetyGuard.trip("แอปเด้งซ้ำ $LIMIT ครั้งภายใน ${WINDOW_MS / 60000} นาที", "ข้อผิดพลาดล่าสุด (${t.name}):\n$trace")
                }
            }
            prev?.uncaughtException(t, e)
        }
    }

    private fun times(ctx: Context): List<Long> {
        val now = System.currentTimeMillis()
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, "").orEmpty()
            .split(',').mapNotNull { it.toLongOrNull() }.filter { now - it < WINDOW_MS }
    }

    private fun record(ctx: Context) {
        val list = times(ctx) + System.currentTimeMillis()
        // commit() แบบ synchronous เพราะ process กำลังจะปิด
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY, list.joinToString(",")).commit()
    }

    fun inSafeMode(ctx: Context): Boolean = times(ctx).size >= LIMIT

    fun clear(ctx: Context) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }

    /** Launcher อื่นในเครื่อง (ให้ความสำคัญกับของ BYD ก่อน) */
    fun fallbackHome(ctx: Context): ComponentName? {
        val pm = ctx.packageManager
        val homes = runCatching {
            pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
        }.getOrDefault(emptyList())
            .mapNotNull { it.activityInfo }
            .filter { it.packageName != ctx.packageName && !it.name.contains("FallbackHome") }
        val best = homes.firstOrNull { it.packageName.contains("byd", true) }
            ?: homes.firstOrNull { it.packageName.contains("launcher", true) }
            ?: homes.firstOrNull()
        return best?.let { ComponentName(it.packageName, it.name) }
    }

    fun openFallbackHome(ctx: Context): Boolean {
        val cn = fallbackHome(ctx) ?: return false
        return runCatching {
            ctx.startActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setComponent(cn)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        }.getOrDefault(false)
    }
}
