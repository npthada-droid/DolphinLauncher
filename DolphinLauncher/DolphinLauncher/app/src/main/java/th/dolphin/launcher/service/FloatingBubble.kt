@file:OptIn(ExperimentalLayoutApi::class)

package th.dolphin.launcher.service

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import th.dolphin.launcher.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import th.dolphin.launcher.app
import th.dolphin.launcher.apps.AppEntry
import th.dolphin.launcher.apps.QuickActions
import th.dolphin.launcher.ui.MainActivity
import th.dolphin.launcher.ui.components.ShortcutIcon
import th.dolphin.launcher.ui.components.resolveShortcut
import th.dolphin.launcher.ui.theme.LauncherTheme
import th.dolphin.launcher.ui.theme.LocalUi
import th.dolphin.launcher.util.toast
import kotlin.math.roundToInt

/**
 * ปุ่มลอยเรียกแอป: แสดงทับทุกแอป (ซ่อนเมื่ออยู่หน้า Launcher)
 * ลากย้ายตำแหน่งได้ แตะเพื่อเปิดเมนู Dock + แอปล่าสุด
 */
class FloatingBubble(private val ctx: Context) {
    private val wm = ctx.getSystemService(WindowManager::class.java)
    private var view: ComposeView? = null
    private var owner: OverlayOwner? = null
    private val lp = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT,
    ).apply { gravity = Gravity.TOP or Gravity.START }

    fun show() {
        if (view != null) return
        if (!th.dolphin.launcher.SafetyGuard.allow("overlay", 12, 60_000, "การสร้างปุ่มลอย")) return
        val cfg = ctx.app.settings.value
        lp.x = cfg.bubbleX
        lp.y = cfg.bubbleY
        val o = OverlayOwner().also { it.start() }
        val v = ComposeView(ctx).apply {
            setViewTreeLifecycleOwner(o)
            setViewTreeSavedStateRegistryOwner(o)
            setContent { BubbleContent(onDrag = ::move, onDragEnd = ::savePosition) }
        }
        runCatching {
            wm.addView(v, lp)
            view = v
            owner = o
        }.onFailure { o.stop() }
    }

    fun hide() {
        view?.let { runCatching { wm.removeView(it) } }
        view = null
        owner?.stop()
        owner = null
    }

    private fun move(dx: Float, dy: Float) {
        val v = view ?: return
        val dm = ctx.resources.displayMetrics
        lp.x = (lp.x + dx.roundToInt()).coerceIn(0, dm.widthPixels - 60)
        lp.y = (lp.y + dy.roundToInt()).coerceIn(0, dm.heightPixels - 60)
        runCatching { wm.updateViewLayout(v, lp) }
    }

    private fun savePosition() {
        ctx.app.settings.update { it.copy(bubbleX = lp.x, bubbleY = lp.y) }
    }
}

@Composable
private fun BubbleContent(onDrag: (Float, Float) -> Unit, onDragEnd: () -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.app
    val cfg by app.settings.flow.collectAsState()
    val fg by app.foreground.collectAsState()
    val apps by app.apps.apps.collectAsState()
    val recents by app.apps.recents.collectAsState()
    var open by remember { mutableStateOf(false) }
    if (fg) return

    fun go(key: String) {
        open = false
        val ok = when (key) {
            "action:theme" -> {
                app.settings.update { it.copy(themeMode = if (it.themeMode == "light") "dark" else "light") }
                true
            }
            else -> QuickActions.run(ctx, key, cfg) ?: runCatching {
                ctx.startActivity(
                    Intent(ctx, MainActivity::class.java)
                        .putExtra(MainActivity.EXTRA_ACTION, key)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }.isSuccess
        }
        if (!ok) ctx.toast(QuickActions.failMessage(key))
    }

    LauncherTheme(cfg) {
        val ui = LocalUi.current
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(58.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(ui.accent2, ui.accent)))
                    .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures(onDragEnd = onDragEnd) { change, drag ->
                            change.consume()
                            onDrag(drag.x, drag.y)
                        }
                    }
                    .clickable { open = !open },
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (open) Icons.Rounded.Close else Icons.Rounded.Apps, "เมนูแอป", tint = Color(0xFF04101A))
            }
            if (open) {
                Spacer(Modifier.width(8.dp))
                val shape = RoundedCornerShape(20.dp)
                Column(
                    Modifier.widthIn(max = 400.dp).clip(shape).background(Color(0xF2101722))
                        .border(1.dp, Color(0x22FFFFFF), shape).padding(12.dp)
                ) {
                    Text("เรียกแอป", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        maxItemsInEachRow = 5,
                    ) {
                        BubbleItem("action:home", apps) { go(it) }
                        cfg.dock.forEach { k -> BubbleItem(k, apps) { go(it) } }
                    }
                    if (recents.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("ใช้ล่าสุด", color = Color(0xFF93A0B2), style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), maxItemsInEachRow = 5) {
                            recents.take(5).forEach { k -> BubbleItem(k, apps) { go(it) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BubbleItem(key: String, apps: List<AppEntry>, onClick: (String) -> Unit) {
    Column(
        Modifier.width(66.dp).clip(RoundedCornerShape(14.dp)).clickable { onClick(key) }.padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (key == "action:home") {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Home, null, tint = MaterialTheme.colorScheme.primary) }
        } else {
            ShortcutIcon(resolveShortcut(key, apps), 40.dp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            if (key == "action:home") "หน้าหลัก" else resolveShortcut(key, apps).label,
            color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
        )
    }
}
