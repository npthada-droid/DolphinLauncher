@file:OptIn(ExperimentalFoundationApi::class)

package th.dolphin.launcher.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ThreeSixty
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TireRepair
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import th.dolphin.launcher.app
import th.dolphin.launcher.apps.AppEntry
import th.dolphin.launcher.apps.ShortcutKeys
import th.dolphin.launcher.ui.theme.LocalUi
import th.dolphin.launcher.ui.theme.StatusColors

data class BuiltinAction(val key: String, val label: String, val icon: ImageVector)

object BuiltinActions {
    val all = listOf(
        BuiltinAction("action:camera", "กล้องรอบคัน", Icons.Rounded.Videocam),
        BuiltinAction("action:panorama", "360° ของรถ", Icons.Rounded.ThreeSixty),
        BuiltinAction("action:projection", "Android Auto / มิเรอร์", Icons.Rounded.Cast),
        BuiltinAction("action:audio", "เสียงและเพลง", Icons.Rounded.MusicNote),
        BuiltinAction("action:radio", "วิทยุ", Icons.Rounded.Radio),
        BuiltinAction("action:btmusic", "เพลงบลูทูธ", Icons.Rounded.BluetoothAudio),
        BuiltinAction("action:music", "แอปเพลง", Icons.Rounded.MusicNote),
        BuiltinAction("action:phone", "โทรศัพท์", Icons.Rounded.Phone),
        BuiltinAction("action:bluetooth", "บลูทูธ", Icons.Rounded.Bluetooth),
        BuiltinAction("action:battery", "แบตเตอรี่", Icons.Rounded.BatteryChargingFull),
        BuiltinAction("action:trip", "ทริป & ชาร์จ", Icons.Rounded.Route),
        BuiltinAction("action:quick", "แผงควบคุม", Icons.Rounded.Tune),
        BuiltinAction("action:voice", "ผู้ช่วยเสียง", Icons.Rounded.Mic),
        BuiltinAction("action:tires", "ลมยาง", Icons.Rounded.TireRepair),
        BuiltinAction("action:laps", "จับเวลารอบ", Icons.Rounded.Timer),
        BuiltinAction("action:apps", "แอปทั้งหมด", Icons.Rounded.Apps),
        BuiltinAction("action:theme", "สลับธีม", Icons.Rounded.Contrast),
        BuiltinAction("action:settings", "ตั้งค่า", Icons.Rounded.Settings),
    )

    fun find(key: String) = all.firstOrNull { it.key == key }
}

data class ResolvedShortcut(val label: String, val bitmap: ImageBitmap?, val vector: ImageVector?)

fun resolveShortcut(key: String, apps: List<AppEntry>): ResolvedShortcut {
    BuiltinActions.find(key)?.let { return ResolvedShortcut(it.label, null, it.icon) }
    ShortcutKeys.parseShortcut(key).takeIf { key.startsWith(ShortcutKeys.SHORTCUT) }?.let { (pkg, _, label) ->
        return ResolvedShortcut(label, apps.firstOrNull { it.pkg == pkg }?.icon, Icons.Rounded.Bolt)
    }
    ShortcutKeys.parseUri(key).takeIf { key.startsWith(ShortcutKeys.URI) }?.let { (label, _) ->
        return ResolvedShortcut(label, null, Icons.Rounded.Link)
    }
    val a = apps.firstOrNull { it.key == key }
    return if (a != null) ResolvedShortcut(a.label, a.icon, if (a.icon == null) Icons.Rounded.Android else null)
    else ResolvedShortcut(key.substringBefore('/').substringAfterLast('.'), null, Icons.Rounded.Android)
}

@Composable
fun ShortcutIcon(s: ResolvedShortcut, size: Dp) {
    val bmp = s.bitmap
    if (bmp != null) {
        Image(bmp, s.label, Modifier.size(size))
    } else {
        Box(
            Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(s.vector ?: Icons.Rounded.Android, s.label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(size * 0.5f))
        }
    }
}

@Composable
fun ShortcutTile(
    key: String,
    apps: List<AppEntry>,
    editing: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onRemove: () -> Unit = {},
    iconSize: Dp = 58.dp,
) {
    val s = resolveShortcut(key, apps)
    Column(
        Modifier
            .width(iconSize + 34.dp)
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            ShortcutIcon(s, iconSize)
            if (editing) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(6.dp, (-6).dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(StatusColors.bad)
                        .clickable(onClick = onRemove),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Close, "ลบ", tint = Color.White, modifier = Modifier.size(16.dp)) }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            s.label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
fun AddTile(iconSize: Dp = 58.dp, onClick: () -> Unit) {
    Column(
        Modifier.width(iconSize + 34.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(iconSize).clip(CircleShape).border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Add, "เพิ่ม", tint = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.height(6.dp))
        Text("เพิ่ม", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    }
}

/** เลือกแอปหรือฟังก์ชันในตัว — คืนค่า key */
@Composable
fun AppPickerDialog(onDismiss: () -> Unit, includeActions: Boolean = true, onPick: (String) -> Unit) {
    val app = LocalContext.current.app
    val apps by app.apps.apps.collectAsStateWithLifecycle()
    var q by remember { mutableStateOf("") }
    val actions = if (includeActions) BuiltinActions.all.filter { q.isBlank() || it.label.contains(q, true) } else emptyList()
    val list = apps.filter { q.isBlank() || it.label.contains(q, true) || it.pkg.contains(q, true) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxWidth(0.82f).fillMaxHeight(0.86f),
            shape = RoundedCornerShape(LocalUi.current.corner),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.padding(22.dp)) {
                Text("เลือกแอป / ฟังก์ชัน", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    q, { q = it }, Modifier.fillMaxWidth(),
                    placeholder = { Text("ค้นหา") }, singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                )
                Spacer(Modifier.height(12.dp))
                LazyVerticalGrid(
                    GridCells.Adaptive(104.dp), Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (actions.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text("ฟังก์ชันใน Launcher", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        items(actions, key = { it.key }) { a ->
                            Box(contentAlignment = Alignment.Center) { ShortcutTile(a.key, apps, false, onClick = { onPick(a.key) }) }
                        }
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text("แอป", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    items(list, key = { it.key }) { a ->
                        Box(contentAlignment = Alignment.Center) { ShortcutTile(a.key, apps, false, onClick = { onPick(a.key) }) }
                    }
                }
                TextButton(onDismiss, Modifier.align(Alignment.End)) { Text("ปิด") }
            }
        }
    }
}
