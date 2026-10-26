package th.dolphin.launcher.ui

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import th.dolphin.launcher.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import th.dolphin.launcher.CrashGuard
import th.dolphin.launcher.SafetyGuard
import th.dolphin.launcher.util.toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * หน้าข้อผิดพลาดแบบเบา: ไม่อ่านข้อมูลรถ ไม่มีแอนิเมชัน ไม่มีงานเบื้องหลัง
 * แสดงเมื่อระบบป้องกันตัดการทำงาน
 */
@Composable
fun ErrorScreen(trip: SafetyGuard.Trip, onRestart: (simMode: Boolean) -> Unit) {
    val ctx = LocalContext.current
    val df = SimpleDateFormat("d MMM yyyy HH:mm:ss", th.dolphin.launcher.i18n.I18n.dateLocale())
    Box(Modifier.fillMaxSize().background(Color(0xFF07090D)).systemBarsPadding(), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 820.dp).fillMaxWidth().padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.GppMaybe, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(44.dp))
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Dolphin Launcher หยุดทำงานเพื่อความปลอดภัย", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Text(df.format(Date(trip.time)), color = Color(0xFF8693A6))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(trip.reason, color = Color(0xFFF87171), fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Text(
                "หยุดอ่านข้อมูลรถและงานเบื้องหลังทั้งหมดแล้ว ระบบของรถ (BYD) ทำงานตามปกติ",
                color = Color(0xFFB8C2D0),
            )
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF111620)).padding(12.dp)
            ) {
                Text(
                    trip.detail, color = Color(0xFFCBD5E1), fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    { if (!CrashGuard.openFallbackHome(ctx)) ctx.toast("ไม่พบ Launcher อื่นในเครื่อง") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EC4B6), contentColor = Color(0xFF041014)),
                ) { Text("เปิด Launcher เดิมของรถ") }
                OutlinedButton({ onRestart(false) }) { Text("เริ่มทำงานใหม่", color = Color.White) }
                OutlinedButton({ onRestart(true) }) { Text("เริ่มใหม่ในโหมดจำลอง", color = Color.White) }
                OutlinedButton({
                    val cm = ctx.getSystemService(ClipboardManager::class.java)
                    cm.setPrimaryClip(ClipData.newPlainText("dolphin-error", SafetyGuard.report(trip)))
                    ctx.toast("คัดลอกรายละเอียดแล้ว")
                }) { Text("คัดลอกรายละเอียด", color = Color.White) }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "‘เริ่มใหม่ในโหมดจำลอง’ จะไม่เรียก BYD API — ใช้เมื่อสาเหตุมาจากการอ่านข้อมูลรถ",
                color = Color(0xFF8693A6), fontSize = 12.sp,
            )
        }
    }
}
