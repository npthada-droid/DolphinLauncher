package th.dolphin.launcher.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AirlineSeatReclineNormal
import androidx.compose.material.icons.rounded.DoorFront
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TireRepair
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import th.dolphin.launcher.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import th.dolphin.launcher.alerts.AlertLevel
import th.dolphin.launcher.alerts.VehicleAlert
import th.dolphin.launcher.ui.theme.StatusColors

@Composable
fun AlertBannerStack(alerts: List<VehicleAlert>, modifier: Modifier = Modifier, onTap: (String) -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        alerts.forEach { a -> key(a.id) { AlertBanner(a) { onTap(a.id) } } }
    }
}

@Composable
fun AlertBanner(a: VehicleAlert, onClick: () -> Unit) {
    val color = when (a.level) {
        AlertLevel.CRITICAL -> StatusColors.bad
        AlertLevel.WARN -> StatusColors.warn
        AlertLevel.INFO -> StatusColors.info
    }
    val pulse = rememberInfiniteTransition(label = "pulse")
    val alpha by pulse.animateFloat(1f, if (a.level == AlertLevel.CRITICAL) 0.25f else 0.8f,
        infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "p")
    val shape = RoundedCornerShape(22.dp)
    val icon = when {
        a.id.startsWith("door") -> Icons.Rounded.DoorFront
        a.id.startsWith("belt") -> Icons.Rounded.AirlineSeatReclineNormal
        a.id.startsWith("tire") -> Icons.Rounded.TireRepair
        a.id == "speed" -> Icons.Rounded.Speed
        a.id == "service" -> Icons.Rounded.Build
        else -> Icons.Rounded.Warning
    }
    Row(
        Modifier
            .widthIn(max = 760.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xF2141922))
            .border(2.dp, color.copy(alpha = alpha), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(color.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(a.title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (a.detail.isNotBlank()) Text(a.detail, color = Color(0xFFB8C2D0), style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            if (a.level == AlertLevel.CRITICAL) "วิกฤต" else "เตือน",
            color = color, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold
        )
    }
}
