package th.dolphin.launcher.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import th.dolphin.launcher.R
import th.dolphin.launcher.location.GeoFix
import java.io.File

/**
 * แผนที่ OpenStreetMap (ไม่ต้องใช้ API key) — แสดงตำแหน่งและทิศทางรถจาก GPS
 * แคชแผนที่ไว้ในเครื่อง ใช้งานต่อได้ในพื้นที่ที่เคยโหลดแล้วแม้สัญญาณเน็ตหาย
 */
@Composable
fun OsmMap(fix: GeoFix?, zoom: Double, darkTiles: Boolean, modifier: Modifier) {
    val ctx = LocalContext.current
    val map = remember {
        Configuration.getInstance().apply {
            userAgentValue = ctx.packageName
            osmdroidBasePath = File(ctx.cacheDir, "osm")
            osmdroidTileCache = File(ctx.cacheDir, "osm/tiles")
        }
        MapView(ctx).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(zoom)
            minZoomLevel = 4.0
            maxZoomLevel = 19.0
        }
    }
    val marker = remember {
        Marker(map).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = ContextCompat.getDrawable(ctx, R.drawable.ic_nav_arrow)
            setInfoWindow(null)
            map.overlays.add(this)
        }
    }
    LaunchedEffect(darkTiles) {
        map.overlayManager.tilesOverlay.setColorFilter(if (darkTiles) TilesOverlay.INVERT_COLORS else null)
        map.invalidate()
    }
    LaunchedEffect(zoom) { map.controller.setZoom(zoom) }
    LaunchedEffect(fix) {
        val f = fix ?: return@LaunchedEffect
        val p = GeoPoint(f.lat, f.lon)
        marker.position = p
        f.bearing?.let { marker.rotation = -it }
        map.controller.animateTo(p)
        map.invalidate()
    }
    DisposableEffect(Unit) {
        map.onResume()
        onDispose {
            map.onPause()
            map.onDetach()
        }
    }
    AndroidView({ map }, modifier)
}
