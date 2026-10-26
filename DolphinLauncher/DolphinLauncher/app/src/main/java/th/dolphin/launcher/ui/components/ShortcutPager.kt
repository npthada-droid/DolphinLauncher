@file:OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)

package th.dolphin.launcher.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import th.dolphin.launcher.app

/** การ์ดทางลัดหลายหน้า (ปัดซ้าย-ขวา) */
@Composable
fun ShortcutPagerCard(modifier: Modifier, onAction: (String) -> Unit) {
    val app = LocalContext.current.app
    val pages by app.shortcuts.pages.collectAsStateWithLifecycle()
    val apps by app.apps.apps.collectAsStateWithLifecycle()
    var edit by remember { mutableStateOf(false) }
    var pickFor by remember { mutableStateOf<Int?>(null) }
    val pager = rememberPagerState { pages.size.coerceAtLeast(1) }

    GlassCard(modifier, padding = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ทางลัด", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 6.dp).weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(pages.size) { i ->
                    Box(
                        Modifier.size(if (i == pager.currentPage) 16.dp else 6.dp, 6.dp).clip(CircleShape)
                            .background(
                                if (i == pager.currentPage) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            )
                    )
                }
            }
            if (edit) {
                TextButton({ app.shortcuts.addPage() }) { Text("+หน้า") }
                if (pages.size > 1) TextButton({ app.shortcuts.removePage(pager.currentPage) }) { Text("ลบ") }
            }
            TextButton({ edit = !edit }) { Text(if (edit) "เสร็จ" else "แก้ไข") }
        }
        HorizontalPager(pager, Modifier.fillMaxWidth().weight(1f)) { pageIdx ->
            val items = pages.getOrElse(pageIdx) { emptyList() }
            FlowRow(
                Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items.forEach { key ->
                    ShortcutTile(
                        key, apps, edit,
                        onClick = { if (!edit) onAction(key) },
                        onLongClick = { edit = true },
                        onRemove = { app.shortcuts.remove(pageIdx, key) },
                        iconSize = 46.dp,
                        onMove = { d -> app.shortcuts.move(pageIdx, key, d) },
                    )
                }
                if (edit || items.isEmpty()) AddTile(46.dp) { pickFor = pageIdx }
            }
        }
    }
    pickFor?.let { idx ->
        AppPickerDialog(onDismiss = { pickFor = null }) { key ->
            app.shortcuts.add(idx, key)
            pickFor = null
        }
    }
}

