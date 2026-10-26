@file:OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package th.dolphin.launcher.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import th.dolphin.launcher.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import th.dolphin.launcher.app
import th.dolphin.launcher.settings.AppSettings
import th.dolphin.launcher.ui.components.GlassCard
import th.dolphin.launcher.ui.components.ScreenHeader
import th.dolphin.launcher.ui.theme.StatusColors
import th.dolphin.launcher.util.toast
import th.dolphin.launcher.vehicle.BydProbe
import java.io.File

@Composable
fun ProbeScreen(cfg: AppSettings, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val st by ctx.app.vehicle.state.collectAsStateWithLifecycle()
    var report by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }
    var classes by remember { mutableStateOf<List<String>>(emptyList()) }
    val selected = remember { mutableStateListOf<String>() }
    var testMapping by remember { mutableStateOf(false) }

    fun scan(read: Boolean) {
        running = true
        scope.launch {
            report = withContext(Dispatchers.IO) {
                runCatching {
                    if (!read) classes = BydProbe.loadableClasses(cfg.bydMap)
                    BydProbe.run(
                        ctx, cfg.bydMap,
                        readClasses = if (read) selected.toSet() else emptySet(),
                        testMapping = read && testMapping,
                    )
                }.getOrElse { "สแกนล้มเหลว: $it" }
            }
            running = false
        }
    }

    Column(Modifier.fillMaxSize().systemBarsPadding().padding(22.dp)) {
        ScreenHeader("สแกน BYD Auto API", onBack) {
            OutlinedButton(enabled = report.isNotEmpty(), onClick = {
                val cm = ctx.getSystemService(ClipboardManager::class.java)
                cm.setPrimaryClip(ClipData.newPlainText("byd-probe", report))
                ctx.toast("คัดลอกแล้ว")
            }) { Text("คัดลอก") }
            Spacer(Modifier.padding(4.dp))
            OutlinedButton(enabled = report.isNotEmpty(), onClick = {
                runCatching {
                    val f = File(ctx.getExternalFilesDir(null), "byd_probe_${System.currentTimeMillis()}.txt")
                    f.writeText(report)
                    ctx.toast("บันทึกที่ ${f.absolutePath}")
                }.onFailure { ctx.toast("บันทึกไม่สำเร็จ: ${it.message}") }
            }) { Text("บันทึกไฟล์") }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Text("ขั้นที่ 1 · สแกนรายชื่อ (ปลอดภัย)", style = MaterialTheme.typography.titleMedium)
            Text(
                "แสดงชื่อคลาส เมธอด และชื่อค่าคงที่เท่านั้น ไม่เรียกโค้ดใดๆ ของ BYD",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Button(enabled = !running, onClick = { scan(false) }) { Text(if (running) "กำลังสแกน…" else "สแกนรายชื่อ") }

            if (classes.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text("ขั้นที่ 2 · อ่านค่าเฉพาะคลาสที่เลือก", style = MaterialTheme.typography.titleMedium)
                Text(
                    "ทำตอนจอดรถ เกียร์ P เท่านั้น · จะเรียกเมธอด get/is ที่ไม่มีพารามิเตอร์ของคลาสที่เลือก",
                    color = StatusColors.warn,
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    classes.forEach { cn ->
                        FilterChip(
                            selected = cn in selected,
                            onClick = { if (cn in selected) selected.remove(cn) else selected.add(cn) },
                            label = { Text(cn.substringAfterLast('.')) },
                        )
                    }
                    FilterChip(selected = testMapping, onClick = { testMapping = !testMapping }, label = { Text("+ ทดสอบ mapping") })
                }
                Spacer(Modifier.height(8.dp))
                val parked = !st.reversing && st.speedKmh < 1f
                Button(
                    enabled = !running && parked && (selected.isNotEmpty() || testMapping),
                    onClick = { scan(true) },
                ) { Text(if (parked) "อ่านค่าคลาสที่เลือก" else "จอดรถก่อนจึงอ่านค่าได้") }
            }
        }
        Spacer(Modifier.height(12.dp))
        GlassCard(Modifier.fillMaxWidth().weight(1f).heightIn(min = 120.dp)) {
            SelectionContainer {
                Text(
                    report.ifEmpty {
                        "วิธีใช้:\n" +
                            "1) กด ‘สแกนรายชื่อ’ เพื่อดูว่าเฟิร์มแวร์มีคลาส android.hardware.bydauto.* อะไรบ้าง\n" +
                            "2) เลือกคลาสที่สนใจ เช่น Tyre, Bodywork, SafetyBelt, Light แล้วกด ‘อ่านค่า’\n" +
                            "3) เปิด/ปิดประตู คาด/ปลดเข็มขัด เปิดไฟเลี้ยว แล้วอ่านค่าซ้ำ ดูว่าค่าไหนเปลี่ยน\n" +
                            "4) นำชื่อเมธอดและตัวเลขไปแก้ mapping ที่ ตั้งค่า > ข้อมูลรถ"
                    },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                )
            }
        }
    }
}
