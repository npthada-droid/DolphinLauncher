@file:OptIn(ExperimentalCamera2Interop::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package th.dolphin.launcher.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ThreeSixty
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import th.dolphin.launcher.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import th.dolphin.launcher.app
import th.dolphin.launcher.settings.AppSettings
import th.dolphin.launcher.ui.components.GlassCard
import th.dolphin.launcher.ui.components.ScreenHeader
import th.dolphin.launcher.ui.components.TurnArrows
import th.dolphin.launcher.ui.theme.LocalUi
import th.dolphin.launcher.ui.theme.StatusColors
import th.dolphin.launcher.util.toast
import th.dolphin.launcher.vehicle.TurnSignal

data class CamInfo(val id: String, val facing: String)

fun listCameras(ctx: Context): List<CamInfo> = runCatching {
    val cm = ctx.getSystemService(CameraManager::class.java)
    cm.cameraIdList.map { id ->
        val f = runCatching { cm.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) }.getOrNull()
        CamInfo(
            id, when (f) {
                CameraCharacteristics.LENS_FACING_FRONT -> "หน้า"
                CameraCharacteristics.LENS_FACING_BACK -> "หลัง"
                CameraCharacteristics.LENS_FACING_EXTERNAL -> "ภายนอก"
                else -> "?"
            }
        )
    }
}.getOrDefault(emptyList())

fun parseCameraNames(s: String): Map<String, String> =
    s.split(',', '\n').mapNotNull {
        val p = it.split('=')
        if (p.size == 2 && p[0].isNotBlank()) p[0].trim() to p[1].trim() else null
    }.toMap()

fun hasCameraPermission(ctx: Context) =
    ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

/** แสดงภาพจากกล้องตาม id (เว้นว่าง = กล้องตัวแรกที่พบ) */
@Composable
fun CameraPreview(cameraId: String, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val granted = hasCameraPermission(ctx)
    val vstate by ctx.app.vehicle.state.collectAsStateWithLifecycle()
    // เกียร์ R: ไม่เปิดกล้องเลย (และปล่อยกล้องที่เปิดอยู่) เพื่อให้กล้องถอยของรถใช้งานได้เสมอ
    val reversing = vstate.reversing
    val previewView = remember {
        PreviewView(ctx).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    var error by remember(cameraId) { mutableStateOf<String?>(null) }

    if (granted && !reversing) {
        DisposableEffect(cameraId, owner) {
            val future = ProcessCameraProvider.getInstance(ctx)
            var provider: ProcessCameraProvider? = null
            var useCase: Preview? = null
            future.addListener({
                try {
                    val p = future.get()
                    provider = p
                    val preview = Preview.Builder().build()
                    preview.setSurfaceProvider(previewView.surfaceProvider)
                    val selector = CameraSelector.Builder().addCameraFilter { infos ->
                        val match = infos.filter { Camera2CameraInfo.from(it).cameraId == cameraId }
                        if (match.isNotEmpty()) match else infos.take(1)
                    }.build()
                    p.unbindAll()
                    p.bindToLifecycle(owner, selector, preview)
                    useCase = preview
                    error = null
                } catch (t: Throwable) {
                    error = t.message ?: t.javaClass.simpleName
                }
            }, ContextCompat.getMainExecutor(ctx))
            onDispose {
                runCatching { useCase?.let { provider?.unbind(it) } }
            }
        }
    }

    Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        if (granted && !reversing) AndroidView({ previewView }, Modifier.fillMaxSize())
        val msg = if (!granted) "ยังไม่ได้อนุญาตสิทธิ์กล้อง"
        else if (reversing) "เกียร์ R — ปล่อยกล้องให้ระบบกล้องถอยของรถ"
        else error?.let { "เปิดกล้องไม่ได้: $it\n(กล้องรอบคันบางรุ่นถูกล็อกให้ใช้ได้เฉพาะแอปของรถ ลองใช้โหมด ‘เปิดแอปกล้องของรถ’)" }
        if (msg != null) {
            Text(msg, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
        }
    }
}

@Composable
fun CameraScreen(cfg: AppSettings, onBack: (() -> Unit)?) {
    val ctx = LocalContext.current
    val app = ctx.app
    val cams = remember { listCameras(ctx) }
    val names = remember(cfg.cameraNames) { parseCameraNames(cfg.cameraNames) }
    var selected by rememberSaveable { mutableStateOf(cams.firstOrNull()?.id ?: "") }
    var granted by remember { mutableStateOf(hasCameraPermission(ctx)) }
    val req = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }

    Column(Modifier.fillMaxSize().systemBarsPadding().padding(22.dp)) {
        ScreenHeader("กล้องรอบคัน", onBack) {
            Button(onClick = {
                if (!app.apps.launchTarget(cfg.panoramaPkg)) ctx.toast("ตั้งค่าแอป 360° ได้ที่ ตั้งค่า > กล้อง & ไฟเลี้ยว")
            }) {
                Icon(Icons.Rounded.ThreeSixty, null)
                Spacer(Modifier.width(8.dp))
                Text("เปิด 360° ของรถ")
            }
        }
        if (cams.isEmpty()) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("ไม่พบกล้องที่แอปทั่วไปเข้าถึงได้", style = MaterialTheme.typography.titleMedium)
                Text(
                    "หัวเครื่อง BYD บางรุ่นเปิดกล้องรอบคันให้เฉพาะแอปของระบบ ใช้ปุ่ม ‘เปิด 360° ของรถ’ แทน " +
                        "แล้วตั้งค่าแพ็กเกจแอปกล้องในเมนูตั้งค่า (หาได้จากเมนู สแกน BYD API)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                cams.forEach { c ->
                    FilterChip(
                        selected = selected == c.id,
                        onClick = { selected = c.id },
                        label = { Text(names[c.id] ?: "กล้อง ${c.id} (${c.facing})") },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(LocalUi.current.corner))) {
                if (granted) {
                    CameraPreview(selected, Modifier.fillMaxSize())
                    if (cfg.camGuides) GuideLines(Modifier.fillMaxSize())
                } else {
                    Column(
                        Modifier.fillMaxSize().background(Color.Black),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("ต้องการสิทธิ์ใช้กล้อง", color = Color.White)
                        Spacer(Modifier.height(12.dp))
                        Button({ req.launch(Manifest.permission.CAMERA) }) { Text("อนุญาต") }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton({ app.settings.update { it.copy(leftCamId = selected) } }) {
                    Text(if (cfg.leftCamId == selected) "✓ กล้องไฟเลี้ยวซ้าย" else "ใช้เป็นกล้องไฟเลี้ยวซ้าย")
                }
                OutlinedButton({ app.settings.update { it.copy(rightCamId = selected) } }) {
                    Text(if (cfg.rightCamId == selected) "✓ กล้องไฟเลี้ยวขวา" else "ใช้เป็นกล้องไฟเลี้ยวขวา")
                }
            }
        }
    }
}

/** แผงกล้องที่เด้งขึ้นเมื่อเปิดไฟเลี้ยว */
@Composable
fun TurnCameraPanel(turn: TurnSignal, camId: String, onClose: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Box(Modifier.fillMaxSize().clip(shape).background(Color.Black).border(3.dp, StatusColors.warn, shape)) {
        CameraPreview(camId, Modifier.fillMaxSize())
        Row(
            Modifier.align(Alignment.TopStart).padding(12.dp).clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TurnArrows(turn, 20.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                when (turn) {
                    TurnSignal.RIGHT -> "เลี้ยวขวา"
                    TurnSignal.HAZARD -> "ไฟฉุกเฉิน"
                    else -> "เลี้ยวซ้าย"
                },
                color = Color.White, fontWeight = FontWeight.SemiBold,
            )
        }
        IconButton(onClose, Modifier.align(Alignment.TopEnd).padding(6.dp)) {
            Icon(Icons.Rounded.Close, "ปิด", tint = Color.White)
        }
    }
}

/** เส้นช่วยกะระยะ (เหลือง = แนวรถ, เขียว = ระยะปลอดภัย, แดง = ใกล้มาก) — เป็นเส้นอ้างอิงคร่าวๆ ไม่ใช่ระยะจริง */
@Composable
fun GuideLines(modifier: Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val w = size.width
        val h = size.height
        val sw = 3.dp.toPx()
        val amber = Color(0xFFFBBF24)
        drawLine(amber, androidx.compose.ui.geometry.Offset(w * 0.22f, h), androidx.compose.ui.geometry.Offset(w * 0.39f, h * 0.55f), sw)
        drawLine(amber, androidx.compose.ui.geometry.Offset(w * 0.78f, h), androidx.compose.ui.geometry.Offset(w * 0.61f, h * 0.55f), sw)
        drawLine(Color(0xFF34D399), androidx.compose.ui.geometry.Offset(w * 0.33f, h * 0.72f), androidx.compose.ui.geometry.Offset(w * 0.67f, h * 0.72f), sw)
        drawLine(Color(0xFFF87171), androidx.compose.ui.geometry.Offset(w * 0.28f, h * 0.88f), androidx.compose.ui.geometry.Offset(w * 0.72f, h * 0.88f), sw)
    }
}
