package th.dolphin.launcher

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * ล็อกการใช้เป็นหน้าหลัก (Launcher): ปิด HOME ไว้จนกว่าจะใส่รหัสผ่านในตั้งค่า
 * ระหว่างล็อก ระบบจะไม่เสนอแอปนี้เป็นหน้าแรกเลย จึงไม่กระทบหน้าจอเดิมของรถ
 */
object HomeLock {
    const val PASSWORD = "123"

    private fun alias(ctx: Context) = ComponentName(ctx.packageName, "th.dolphin.launcher.HomeAlias")

    fun isUnlocked(ctx: Context): Boolean = runCatching {
        ctx.packageManager.getComponentEnabledSetting(alias(ctx)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }.getOrDefault(false)

    fun tryUnlock(ctx: Context, password: String): Boolean {
        if (password.trim() != PASSWORD) return false
        set(ctx, true)
        return true
    }

    fun lock(ctx: Context) = set(ctx, false)

    private fun set(ctx: Context, on: Boolean) {
        runCatching {
            ctx.packageManager.setComponentEnabledSetting(
                alias(ctx),
                if (on) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
    }
}
