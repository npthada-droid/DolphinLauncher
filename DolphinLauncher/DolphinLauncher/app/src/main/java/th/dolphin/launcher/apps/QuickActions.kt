package th.dolphin.launcher.apps

import android.content.Context
import th.dolphin.launcher.app
import th.dolphin.launcher.settings.AppSettings

/**
 * ปุ่มลัดที่ "เปิดแอปอื่น" (ใช้ร่วมกันระหว่างหน้า Launcher และปุ่มลอย)
 * คืนค่า null = เป็นปุ่มนำทางภายใน Launcher (เช่น action:camera) ให้ผู้เรียกจัดการเอง
 */
object QuickActions {
    fun run(ctx: Context, key: String, cfg: AppSettings): Boolean? {
        val apps = ctx.app.apps
        return when (key) {
            "action:panorama" -> apps.launchTarget(cfg.panoramaPkg)
            "action:projection" -> apps.launchProjection(cfg.projectionPkg)
            "action:radio" -> apps.launchKind(AppKind.RADIO, cfg.radioPkg)
            "action:btmusic" -> apps.launchKind(AppKind.BT_MUSIC, cfg.btMusicPkg)
            "action:music" -> apps.launchKind(AppKind.MUSIC, cfg.musicPkg)
            "action:phone" -> apps.launchKind(AppKind.PHONE, cfg.phonePkg)
            "action:bluetooth" -> apps.openBluetoothSettings()
            "action:voice" -> apps.openVoiceAssistant()
            else -> if (key.startsWith("action:")) null else apps.open(key)
        }
    }

    /** ข้อความเมื่อเปิดไม่สำเร็จ */
    fun failMessage(key: String): String = when (key) {
        "action:panorama" -> "ยังไม่ได้ตั้งค่าแอป 360° — ตั้งค่า > กล้อง & ไฟเลี้ยว"
        "action:projection" -> "ไม่พบแอป Android Auto / มิเรอร์ — ตั้งค่า > ทางลัด & แอป"
        "action:radio" -> "ไม่พบแอปวิทยุ — เลือกได้ที่ ตั้งค่า > เสียง & โทรศัพท์"
        "action:btmusic" -> "ไม่พบแอปเพลงบลูทูธ — เลือกได้ที่ ตั้งค่า > เสียง & โทรศัพท์"
        "action:music" -> "ไม่พบแอปเพลง — เลือกได้ที่ ตั้งค่า > เสียง & โทรศัพท์"
        "action:phone" -> "ไม่พบแอปโทรศัพท์ — เลือกได้ที่ ตั้งค่า > เสียง & โทรศัพท์"
        "action:bluetooth" -> "เปิดหน้าตั้งค่าบลูทูธไม่ได้"
        "action:voice" -> "ไม่พบแอปผู้ช่วยเสียงในเครื่อง"
        else -> if (key.startsWith("shortcut:")) "เปิดเมนูลัดไม่ได้ — ต้องตั้ง Dolphin Launcher เป็นหน้าหลักก่อน"
        else "เปิดไม่ได้ แอปอาจถูกถอนการติดตั้ง"
    }

    /** แปลงข้อความ "ชื่อ=เบอร์" บรรทัดละคน เป็นรายการโทรด่วน */
    fun favorites(text: String): List<Pair<String, String>> = text.split('\n', ',').mapNotNull { line ->
        val p = line.split('=')
        if (p.size == 2 && p[0].isNotBlank() && p[1].isNotBlank()) p[0].trim() to p[1].trim() else null
    }
}
