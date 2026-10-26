@file:OptIn(ExperimentalLayoutApi::class)

package th.dolphin.launcher.ui.widgets

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import th.dolphin.launcher.i18n.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import th.dolphin.launcher.app
import th.dolphin.launcher.ui.components.GlassCard
import th.dolphin.launcher.ui.theme.LocalUi
import th.dolphin.launcher.ui.theme.StatusColors
import th.dolphin.launcher.util.toast

/** วิดเจ็ตหนึ่งช่องบนหน้าวิดเจ็ต */
data class WidgetSlot(val id: Int, val heightDp: Int = 220, val wide: Boolean = false)

class WidgetStore(context: Context) {
    val host = AppWidgetHost(context.applicationContext, HOST_ID)
    private val prefs = context.getSharedPreferences("widgets", Context.MODE_PRIVATE)
    private val _slots = MutableStateFlow(load())
    val slots: StateFlow<List<WidgetSlot>> = _slots.asStateFlow()

    private fun load(): List<WidgetSlot> = runCatching {
        val a = JSONArray(prefs.getString("slots", "[]"))
        List(a.length()) { i ->
            val o = a.getJSONObject(i)
            WidgetSlot(o.getInt("id"), o.optInt("h", 220), o.optBoolean("w", false))
        }
    }.getOrDefault(emptyList())

    private fun set(v: List<WidgetSlot>) {
        _slots.value = v
        prefs.edit().putString(
            "slots", JSONArray(v.map { JSONObject().put("id", it.id).put("h", it.heightDp).put("w", it.wide) }).toString()
        ).apply()
    }

    fun add(id: Int) = set(_slots.value + WidgetSlot(id))

    fun remove(id: Int) {
        runCatching { host.deleteAppWidgetId(id) }
        set(_slots.value.filterNot { it.id == id })
    }

    fun cycleHeight(id: Int) = set(_slots.value.map {
        if (it.id == id) it.copy(heightDp = when (it.heightDp) { 160 -> 220; 220 -> 320; else -> 160 }) else it
    })

    fun toggleWide(id: Int) = set(_slots.value.map { if (it.id == id) it.copy(wide = !it.wide) else it })

    fun startListening() = runCatching { host.startListening() }
    fun stopListening() = runCatching { host.stopListening() }

    companion object {
        const val HOST_ID = 7301
    }
}

/**
 * ตัวช่วยเพิ่มวิดเจ็ต (เลือก → ขอสิทธิ์ผูก → หน้าตั้งค่าของวิดเจ็ต)
 * คืนค่าฟังก์ชันสำหรับเปิดหน้าต่างเลือกวิดเจ็ต
 */
@Composable
fun rememberWidgetAdder(onAdded: (Int) -> Unit): () -> Unit {
    val ctx = LocalContext.current
    val store = ctx.app.widgets
    val awm = remember { AppWidgetManager.getInstance(ctx) }
    var pendingId by remember { mutableStateOf<Int?>(null) }
    var picking by remember { mutableStateOf(false) }

    val configureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val id = pendingId
        if (id != null) {
            if (r.resultCode == Activity.RESULT_OK) onAdded(id) else runCatching { store.host.deleteAppWidgetId(id) }
        }
        pendingId = null
    }

    fun configureOrAdd(id: Int, info: AppWidgetProviderInfo) {
        val cfgActivity = info.configure
        if (cfgActivity == null) {
            onAdded(id)
            pendingId = null
            return
        }
        pendingId = id
        val ok = runCatching {
            configureLauncher.launch(
                Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).setComponent(cfgActivity)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            )
        }.isSuccess
        if (!ok) {
            onAdded(id)
            pendingId = null
        }
    }

    val bindLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val id = pendingId
        val info = id?.let { awm.getAppWidgetInfo(it) }
        if (id != null && r.resultCode == Activity.RESULT_OK && info != null) {
            configureOrAdd(id, info)
        } else {
            id?.let { runCatching { store.host.deleteAppWidgetId(it) } }
            pendingId = null
        }
    }

    fun pick(p: AppWidgetProviderInfo) {
        picking = false
        val id = store.host.allocateAppWidgetId()
        val bound = runCatching { awm.bindAppWidgetIdIfAllowed(id, p.provider) }.getOrDefault(false)
        if (bound) {
            configureOrAdd(id, p)
        } else {
            pendingId = id
            val ok = runCatching {
                bindLauncher.launch(
                    Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, p.provider)
                )
            }.isSuccess
            if (!ok) {
                runCatching { store.host.deleteAppWidgetId(id) }
                pendingId = null
                ctx.toast("ระบบไม่อนุญาตให้เพิ่มวิดเจ็ตนี้")
            }
        }
    }

    if (picking) WidgetPicker(onDismiss = { picking = false }) { pick(it) }
    return remember { { picking = true } }
}

/** แสดงวิดเจ็ตตาม id (ใช้ทั้งหน้าวิดเจ็ตและแผนที่กลางหน้าหลัก) */
@Composable
fun HostedWidget(id: Int, modifier: Modifier) {
    val ctx = LocalContext.current
    val store = ctx.app.widgets
    val info = remember(id) { AppWidgetManager.getInstance(ctx).getAppWidgetInfo(id) }
    Box(modifier) {
        if (info == null) {
            Text("วิดเจ็ตนี้ถูกถอนการติดตั้งแล้ว — เลือกใหม่ในตั้งค่า", Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            androidx.compose.runtime.key(id) {
                AndroidView(
                    factory = { c -> store.host.createView(c, id, info).apply { setPadding(0, 0, 0, 0) } },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/** หน้าวิดเจ็ต (ปัดซ้ายจากหน้าหลัก) */
@Composable
fun WidgetsBoard(modifier: Modifier) {
    val ctx = LocalContext.current
    val store = ctx.app.widgets
    val slots by store.slots.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf(false) }
    val addWidget = rememberWidgetAdder { store.add(it) }

    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("วิดเจ็ต", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (slots.isNotEmpty()) TextButton({ editing = !editing }) { Text(if (editing) "เสร็จ" else "จัดการ") }
            TextButton(addWidget) {
                Icon(Icons.Rounded.Add, null)
                Text("เพิ่มวิดเจ็ต")
            }
        }
        if (slots.isEmpty()) {
            GlassCard(Modifier.fillMaxWidth().weight(1f).clickable { addWidget() }) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.Widgets, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("แตะเพื่อเพิ่มวิดเจ็ต เช่น แผนที่ เพลง ปฏิทิน นาฬิกา", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            FlowRow(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = 2,
            ) {
                slots.forEach { slot -> WidgetBox(slot, store, editing) }
            }
        }
    }
}

@Composable
private fun WidgetBox(slot: WidgetSlot, store: WidgetStore, editing: Boolean) {
    val shape = RoundedCornerShape(LocalUi.current.corner)
    Box(
        Modifier.fillMaxWidth(if (slot.wide) 1f else 0.49f).height(slot.heightDp.dp).clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = LocalUi.current.opacity))
    ) {
        HostedWidget(slot.id, Modifier.fillMaxSize())
        if (editing) {
            Row(
                Modifier.align(Alignment.TopEnd).padding(6.dp).clip(CircleShape).background(Color(0xCC0B1018)).padding(2.dp)
            ) {
                MiniBtn(Icons.Rounded.Height, "ความสูง") { store.cycleHeight(slot.id) }
                MiniBtn(Icons.Rounded.OpenInFull, "ความกว้าง") { store.toggleWide(slot.id) }
                MiniBtn(Icons.Rounded.Close, "ลบ", StatusColors.bad) { store.remove(slot.id) }
            }
        }
    }
}

@Composable
private fun MiniBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, tint: Color = Color.White, onClick: () -> Unit) {
    Box(Modifier.size(34.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = tint, modifier = Modifier.size(20.dp))
    }
}

private data class ProviderItem(val info: AppWidgetProviderInfo, val label: String, val app: String, val icon: ImageBitmap?)

@Composable
private fun WidgetPicker(onDismiss: () -> Unit, onPick: (AppWidgetProviderInfo) -> Unit) {
    val ctx = LocalContext.current
    val items = remember {
        val pm = ctx.packageManager
        val density = ctx.resources.displayMetrics.densityDpi
        AppWidgetManager.getInstance(ctx).installedProviders.map { p ->
            ProviderItem(
                p,
                runCatching { p.loadLabel(pm) }.getOrDefault(p.provider.className),
                runCatching { pm.getApplicationLabel(pm.getApplicationInfo(p.provider.packageName, 0)).toString() }.getOrDefault(p.provider.packageName),
                runCatching { p.loadIcon(ctx, density).toBitmap(96, 96).asImageBitmap() }.getOrNull(),
            )
        }.sortedWith(compareBy({ it.app.lowercase() }, { it.label.lowercase() }))
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxWidth(0.7f).fillMaxHeight(0.85f),
            shape = RoundedCornerShape(LocalUi.current.corner), color = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("เลือกวิดเจ็ต (${items.size})", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                if (items.isEmpty()) Text("ไม่พบวิดเจ็ตในเครื่อง", color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyColumn(Modifier.weight(1f)) {
                    items(items) { it2 ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onPick(it2.info) }.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val ic = it2.icon
                            if (ic != null) Image(ic, null, Modifier.size(40.dp)) else Icon(Icons.Rounded.Widgets, null, Modifier.size(40.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(it2.label, style = MaterialTheme.typography.titleSmall)
                                Text(it2.app, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                TextButton(onDismiss, Modifier.align(Alignment.End)) { Text("ปิด") }
            }
        }
    }
}
