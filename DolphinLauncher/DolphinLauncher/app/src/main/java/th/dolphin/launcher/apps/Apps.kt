package th.dolphin.launcher.apps

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.net.Uri
import android.os.Process
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray

data class AppEntry(val key: String, val label: String, val pkg: String, val icon: ImageBitmap?)

class AppRepository(private val context: Context, private val scope: CoroutineScope) {
    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    init {
        refresh()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(context, object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) = refresh()
        }, filter, ContextCompat.RECEIVER_EXPORTED)
    }

    fun refresh() {
        scope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val list = runCatching { pm.queryIntentActivities(intent, 0) }.getOrDefault(emptyList())
                .mapNotNull { ri ->
                    val ai = ri.activityInfo ?: return@mapNotNull null
                    if (ai.packageName == context.packageName) return@mapNotNull null
                    AppEntry(
                        key = ComponentName(ai.packageName, ai.name).flattenToString(),
                        label = ri.loadLabel(pm).toString(),
                        pkg = ai.packageName,
                        icon = runCatching { ri.loadIcon(pm).toBitmap(144, 144).asImageBitmap() }.getOrNull(),
                    )
                }
                .sortedBy { it.label.lowercase() }
            _apps.value = list
        }
    }

    // ---------------- เปิดแอป / ทางลัดทุกแบบ ----------------

    private val prefs = context.getSharedPreferences("apps", Context.MODE_PRIVATE)
    private val _recents = MutableStateFlow(
        prefs.getString("recents", "").orEmpty().split('\n').filter { it.isNotBlank() }
    )
    /** แอปที่เปิดล่าสุด (key ของแอป) ใหม่สุดอยู่หน้า */
    val recents: StateFlow<List<String>> = _recents.asStateFlow()

    private fun remember(key: String) {
        val list = (listOf(key) + _recents.value.filter { it != key }).take(12)
        _recents.value = list
        prefs.edit().putString("recents", list.joinToString("\n")).apply()
    }

    fun clearRecents() {
        _recents.value = emptyList()
        prefs.edit().remove("recents").apply()
    }

    /**
     * เปิดรายการทางลัดทุกรูปแบบ (ยกเว้น action:* ซึ่งหน้า Launcher จัดการเอง)
     *  - "package/activity"            แอป
     *  - "shortcut:pkg|id|label"       เมนูลัดของแอป (App shortcut)
     *  - "uri:label|uri"               ลิงก์ / intent เช่น google.navigation:q=...
     */
    fun open(key: String): Boolean = when {
        key.startsWith(ShortcutKeys.SHORTCUT) -> ShortcutKeys.parseShortcut(key)?.let { (pkg, id, _) -> startShortcut(pkg, id) } ?: false
        key.startsWith(ShortcutKeys.URI) -> ShortcutKeys.parseUri(key)?.let { (_, uri) -> openUri(uri) } ?: false
        else -> launch(key)
    }

    fun launch(key: String): Boolean {
        val cn = ComponentName.unflattenFromString(key) ?: return false
        val ok = start(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(cn))
        if (ok) remember(key)
        return ok
    }

    /** target = "package" หรือ "package/.Activity" */
    fun launchTarget(target: String): Boolean {
        val t = target.trim()
        if (t.isEmpty()) return false
        if (t.contains('/')) {
            val cn = ComponentName.unflattenFromString(t) ?: return false
            return start(Intent().setComponent(cn))
        }
        val i = context.packageManager.getLaunchIntentForPackage(t) ?: return false
        return start(i)
    }

    fun openUri(uri: String): Boolean = runCatching {
        val i = if (uri.startsWith("intent:")) Intent.parseUri(uri, Intent.URI_INTENT_SCHEME)
        else Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        start(i)
    }.getOrDefault(false)

    // ---------------- เมนูลัดของแอป (ต้องเป็น Launcher หลัก) ----------------

    private val launcherApps: LauncherApps? = context.getSystemService(LauncherApps::class.java)

    fun canUseAppShortcuts(): Boolean = runCatching { launcherApps?.hasShortcutHostPermission() == true }.getOrDefault(false)

    fun appShortcuts(pkg: String): List<ShortcutInfo> = runCatching {
        val la = launcherApps ?: return emptyList()
        if (!la.hasShortcutHostPermission()) return emptyList()
        val q = LauncherApps.ShortcutQuery().setPackage(pkg).setQueryFlags(
            LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
        )
        la.getShortcuts(q, Process.myUserHandle()).orEmpty().sortedBy { it.rank }
    }.getOrDefault(emptyList())

    fun startShortcut(pkg: String, id: String): Boolean = runCatching {
        launcherApps!!.startShortcut(pkg, id, null, null, Process.myUserHandle())
        true
    }.getOrDefault(false)

    // ---------------- แอปมิเรอร์ / Android Auto ----------------

    /** แอปฉายภาพมือถือที่ติดตั้งอยู่ในเครื่อง (ค้นจากชื่อแพ็กเกจที่พบบ่อย) */
    fun projectionApps(): List<AppEntry> = _apps.value.filter { a ->
        PROJECTION_HINTS.any { a.pkg.contains(it, ignoreCase = true) } ||
            listOf("android auto", "carplay", "carlife", "mirror", "มิเรอร์").any { a.label.contains(it, ignoreCase = true) }
    }

    /** ค้นหาแอปตามประเภท (วิทยุ, เพลงบลูทูธ, เพลง, โทรศัพท์) จากชื่อแพ็กเกจ/ชื่อแอป */
    fun detect(kind: AppKind): List<AppEntry> = _apps.value.filter { a ->
        kind.pkgHints.any { a.pkg.contains(it, ignoreCase = true) } ||
            kind.labelHints.any { a.label.contains(it, ignoreCase = true) }
    }

    fun launchKind(kind: AppKind, preferred: String): Boolean =
        launchTarget(preferred.ifBlank { detect(kind).firstOrNull()?.key.orEmpty() })

    /** โทรออกผ่านแอปโทรศัพท์ของรถ (เปิดหน้าหมุนเบอร์ให้กดโทรเอง) */
    fun dial(number: String): Boolean = start(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number))))

    fun openVoiceAssistant(): Boolean =
        start(Intent(Intent.ACTION_VOICE_COMMAND)) || start(Intent("android.intent.action.ASSIST"))

    fun openBluetoothSettings(): Boolean = start(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))

    /** เปิดแอปมิเรอร์: ใช้ที่ตั้งค่าไว้ ถ้าไม่ได้ตั้งจะเลือกตัวแรกที่พบในเครื่อง */
    fun launchProjection(preferred: String): Boolean {
        val target = preferred.ifBlank { projectionApps().firstOrNull()?.key.orEmpty() }
        return launchTarget(target)
    }

    private fun start(i: Intent): Boolean {
        if (!th.dolphin.launcher.SafetyGuard.allow("launch", 15, 10_000, "การเปิดแอป/หน้าอื่น")) return false
        return startRaw(i)
    }

    private fun startRaw(i: Intent): Boolean = runCatching {
        context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED))
        true
    }.getOrDefault(false)

    companion object {
        val PROJECTION_HINTS = listOf(
            "gb.xxy.hr",                                  // HeadUnit Reloaded
            "com.google.android.projection.gearhead",     // Android Auto
            "carlife", "carplay", "projection", "androidauto", "easyconnect", "carbit", "zlink", "autokit", "mirror",
        )
    }
}

enum class AppKind(val pkgHints: List<String>, val labelHints: List<String>) {
    RADIO(listOf("radio", ".fm", "tuner"), listOf("วิทยุ", "radio", "FM")),
    BT_MUSIC(listOf("btmusic", "bluetoothmusic", "a2dp", "btaudio", "bt.music"), listOf("เพลงบลูทูธ", "bluetooth music", "BT Music", "บลูทูธ")),
    MUSIC(listOf("music", "player", "audio"), listOf("เพลง", "music")),
    PHONE(listOf("dialer", "btphone", "bluetoothphone", "phone", "contacts", "telecom"), listOf("โทรศัพท์", "phone", "dialer")),
}

/** รูปแบบ key ของทางลัด */
object ShortcutKeys {
    const val SHORTCUT = "shortcut:"
    const val URI = "uri:"

    fun shortcut(pkg: String, id: String, label: String) = "$SHORTCUT$pkg|$id|${label.replace('|', ' ')}"
    fun uri(label: String, uri: String) = "$URI${label.replace('|', ' ')}|$uri"

    fun parseShortcut(key: String): Triple<String, String, String>? {
        val p = key.removePrefix(SHORTCUT).split('|')
        return if (p.size >= 3) Triple(p[0], p[1], p.drop(2).joinToString("|")) else null
    }

    fun parseUri(key: String): Pair<String, String>? {
        val body = key.removePrefix(URI)
        val i = body.indexOf('|')
        return if (i > 0) body.substring(0, i) to body.substring(i + 1) else null
    }
}

/** หน้าทางลัดหลายหน้า: แต่ละรายการเป็น "action:xxx" (ฟังก์ชันในตัว) หรือ "package/activity" (แอป) */
class ShortcutStore(context: Context) {
    private val prefs = context.getSharedPreferences("shortcuts", Context.MODE_PRIVATE)
    private val _pages = MutableStateFlow(load())
    val pages: StateFlow<List<List<String>>> = _pages.asStateFlow()

    private fun load(): List<List<String>> = prefs.getString("pages", null)?.let { s ->
        runCatching {
            val arr = JSONArray(s)
            List(arr.length()) { i -> val p = arr.getJSONArray(i); List(p.length()) { j -> p.getString(j) } }
        }.getOrNull()
    }?.takeIf { it.isNotEmpty() } ?: DEFAULT

    private fun set(v: List<List<String>>) {
        _pages.value = v
        prefs.edit().putString("pages", JSONArray(v.map { JSONArray(it) }).toString()).apply()
    }

    fun add(page: Int, key: String) {
        val v = _pages.value.toMutableList()
        while (v.size <= page) v.add(emptyList())
        if (key !in v[page]) v[page] = v[page] + key
        set(v)
    }

    fun remove(page: Int, key: String) = set(_pages.value.mapIndexed { i, p -> if (i == page) p - key else p })

    fun addPage() = set(_pages.value + listOf(emptyList<String>()))

    /** ย้ายทางลัดซ้าย(-1)/ขวา(+1) — ถ้าสุดขอบหน้าจะย้ายไปหน้าถัดไป/ก่อนหน้า */
    fun move(page: Int, key: String, delta: Int) {
        val v = _pages.value.map { it.toMutableList() }.toMutableList()
        val p = v.getOrNull(page) ?: return
        val i = p.indexOf(key)
        if (i < 0) return
        val j = i + delta
        when {
            j in p.indices -> { p[i] = p[j]; p[j] = key }
            j < 0 && page > 0 -> { p.removeAt(i); v[page - 1].add(key) }
            j >= p.size && page < v.size - 1 -> { p.removeAt(i); v[page + 1].add(0, key) }
            else -> return
        }
        set(v)
    }

    fun removePage(page: Int) {
        if (_pages.value.size <= 1) return
        set(_pages.value.filterIndexed { i, _ -> i != page })
    }

    fun reset() = set(DEFAULT)

    companion object {
        val DEFAULT: List<List<String>> = listOf(
            listOf("action:camera", "action:panorama", "action:projection", "action:trip", "action:quick", "action:voice", "action:tires", "action:laps")
        )
    }
}
