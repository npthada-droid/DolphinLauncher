package th.dolphin.launcher.ui.apps

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import th.dolphin.launcher.i18n.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import th.dolphin.launcher.app
import th.dolphin.launcher.apps.AppEntry
import th.dolphin.launcher.apps.ShortcutKeys
import th.dolphin.launcher.ui.components.ScreenHeader
import th.dolphin.launcher.ui.components.ShortcutTile
import th.dolphin.launcher.util.toast

@Composable
fun AppsScreen(onBack: (() -> Unit)?, onAction: (String) -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.app
    val apps by app.apps.apps.collectAsStateWithLifecycle()
    val recents by app.apps.recents.collectAsStateWithLifecycle()
    var q by rememberSaveable { mutableStateOf("") }
    var menuFor by remember { mutableStateOf<AppEntry?>(null) }
    val list = apps.filter { q.isBlank() || it.label.contains(q, true) || it.pkg.contains(q, true) }
    val recentApps = recents.filter { k -> apps.any { it.key == k } }.take(8)

    fun launch(a: AppEntry) {
        if (!app.apps.launch(a.key)) ctx.toast("เปิดแอปไม่ได้")
    }

    Column(Modifier.fillMaxSize().systemBarsPadding().padding(22.dp)) {
        ScreenHeader("แอปและทางลัด", onBack) {
            OutlinedTextField(
                q, { q = it }, Modifier.width(300.dp),
                placeholder = { Text("ค้นหาแอป") }, singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, null) }, shape = CircleShape,
            )
        }
        if (q.isBlank()) {
            th.dolphin.launcher.ui.components.ShortcutPagerCard(Modifier.fillMaxWidth().height(170.dp), onAction)
            Spacer(Modifier.height(10.dp))
        }
        Text(
            "กดค้างที่แอปเพื่อดูเมนูลัดของแอป หรือเพิ่มลงทางลัด / Dock · ทั้งหมด ${apps.size} แอป",
            Modifier.padding(bottom = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyVerticalGrid(
            GridCells.Adaptive(112.dp), Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (q.isBlank() && recentApps.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("ใช้ล่าสุด", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                        TextButton({ app.apps.clearRecents() }) { Text("ล้าง") }
                    }
                }
                items(recentApps, key = { "r:$it" }) { k ->
                    val a = apps.firstOrNull { it.key == k }
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        ShortcutTile(k, apps, false, onClick = { a?.let { launch(it) } }, onLongClick = { menuFor = a })
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        HorizontalDivider(Modifier.padding(vertical = 6.dp))
                        Text("ทั้งหมด", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            items(list, key = { it.key }) { a ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ShortcutTile(a.key, apps, false, onClick = { launch(a) }, onLongClick = { menuFor = a })
                }
            }
        }
    }

    menuFor?.let { a -> AppMenu(a, onDismiss = { menuFor = null }) }
}

@Composable
private fun AppMenu(a: AppEntry, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.app
    val pages by app.shortcuts.pages.collectAsStateWithLifecycle()
    val shortcuts = remember(a.pkg) { app.apps.appShortcuts(a.pkg) }
    val canShortcuts = remember { app.apps.canUseAppShortcuts() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(a.label) },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                Text("เมนูลัดของแอป", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                when {
                    !canShortcuts -> Text(
                        "ต้องตั้ง Dolphin Launcher เป็นหน้าหลักก่อน จึงจะเห็นเมนูลัดของแอป",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    shortcuts.isEmpty() -> Text(
                        "แอปนี้ไม่มีเมนูลัด", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    else -> shortcuts.forEach { sc ->
                        val label = (sc.shortLabel ?: sc.longLabel ?: sc.id).toString()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton({
                                if (!app.apps.startShortcut(a.pkg, sc.id)) ctx.toast("เปิดเมนูลัดไม่ได้")
                                onDismiss()
                            }, Modifier.weight(1f)) { Text(label, Modifier.fillMaxWidth()) }
                            TextButton({
                                app.shortcuts.add(0, ShortcutKeys.shortcut(a.pkg, sc.id, label))
                                ctx.toast("เพิ่ม ‘$label’ ในทางลัดหน้า 1")
                            }) { Text("+ ทางลัด") }
                        }
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                pages.indices.forEach { i ->
                    TextButton({
                        app.shortcuts.add(i, a.key)
                        ctx.toast("เพิ่มในทางลัดหน้า ${i + 1}")
                        onDismiss()
                    }) { Text("เพิ่มในทางลัดหน้า ${i + 1}") }
                }
                TextButton({
                    app.settings.update { s -> s.copy(dock = (s.dock + a.key).distinct().take(8)) }
                    ctx.toast("เพิ่มใน Dock แล้ว")
                    onDismiss()
                }) { Text("เพิ่มใน Dock") }
                TextButton({
                    app.settings.update { it.copy(projectionPkg = a.key) }
                    ctx.toast("ตั้ง ‘${a.label}’ เป็นแอป Android Auto / มิเรอร์")
                    onDismiss()
                }) { Text("ใช้เป็นแอป Android Auto / มิเรอร์") }
                TextButton({
                    runCatching {
                        ctx.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${a.pkg}"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                    onDismiss()
                }) { Text("ข้อมูลแอป") }
                Spacer(Modifier.height(4.dp))
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("ปิด") } },
    )
}
