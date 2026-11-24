package com.matchora.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.ui.theme.MColors
import com.matchora.app.data.BackendBuildConfig
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.IconFactory

@Composable
actual fun NativeMapView(
    modifier: Modifier, latitude: Double, longitude: Double, zoomMeters: Double,
    pins: List<MapPinData>, selectedPinId: String?, onPinSelected: (String) -> Unit, interactive: Boolean
) {
    if (BackendBuildConfig.MODE == "preview") {
        PreviewNativeMapView(modifier, latitude, longitude, zoomMeters, pins, selectedPinId, onPinSelected, interactive)
    } else AndroidLiveMap(modifier, latitude, longitude, zoomMeters, pins, selectedPinId, onPinSelected, interactive)
}

private class AndroidMapLifetime { var destroyed = false; var viewportApplied = false }

/** Real native map shared by discovery, the mini radar and the meeting-point picker. */
@Suppress("DEPRECATION")
@Composable
internal fun AndroidLiveMap(
    modifier: Modifier, latitude: Double, longitude: Double, zoomMeters: Double,
    pins: List<MapPinData> = emptyList(), selectedPinId: String? = null,
    onPinSelected: (String) -> Unit = {}, interactive: Boolean = true,
    onCenterChanged: ((Double, Double) -> Unit)? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    var viewportDp by remember { mutableDoubleStateOf(256.0) }
    val pinCallback by rememberUpdatedState(onPinSelected)
    val centerCallback by rememberUpdatedState(onCenterChanged)
    val lifetime = remember { AndroidMapLifetime() }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    val markers = remember { mutableMapOf<Long, String>() }
    val styleUrl = BackendBuildConfig.MAP_STYLE_URL
    if (styleUrl.isBlank()) {
        Box(modifier.background(Color(0xFFEAF0EB)), contentAlignment = Alignment.Center) {
            Text("Harita şu anda kullanılamıyor.", Modifier.padding(16.dp))
        }
        return
    }
    val mapView = remember(context) {
        MapLibre.getInstance(context)
        MapView(context, MapLibreMapOptions().textureMode(true)).apply { onCreate(null) }
    }
    val activity = remember(context) {
        generateSequence(context) { (it as? android.content.ContextWrapper)?.baseContext }
            .filterIsInstance<androidx.activity.ComponentActivity>().firstOrNull()
    }
    DisposableEffect(mapView, activity) {
        var started = false
        var resumed = false
        fun start() { if (!started) { mapView.onStart(); started = true } }
        fun resume() { start(); if (!resumed) { mapView.onResume(); resumed = true } }
        fun pause() { if (resumed) { mapView.onPause(); resumed = false } }
        fun stop() { pause(); if (started) { mapView.onStop(); started = false } }
        val observer = LifecycleEventObserver { _, event -> when (event) {
            Lifecycle.Event.ON_START -> start()
            Lifecycle.Event.ON_RESUME -> resume()
            Lifecycle.Event.ON_PAUSE -> pause()
            Lifecycle.Event.ON_STOP -> stop()
            else -> Unit
        } }
        activity?.lifecycle?.addObserver(observer)
        if (activity == null || activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) start()
        if (activity == null || activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) resume()
        val memory = object : ComponentCallbacks2 {
            override fun onConfigurationChanged(configuration: Configuration) {}
            override fun onLowMemory() { mapView.onLowMemory() }
            override fun onTrimMemory(level: Int) { if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) mapView.onLowMemory() }
        }
        context.registerComponentCallbacks(memory)
        onDispose {
            lifetime.destroyed = true
            activity?.lifecycle?.removeObserver(observer)
            context.unregisterComponentCallbacks(memory)
            stop(); mapView.onDestroy()
        }
    }
    LaunchedEffect(mapView) {
        mapView.addOnDidFailLoadingMapListener {
            if (!lifetime.destroyed) { styleReady = false; error = "Harita yüklenemedi. İnternet bağlantını kontrol edip tekrar dene." }
        }
        mapView.getMapAsync { native ->
            if (!lifetime.destroyed) {
                map = native
                native.setOnMarkerClickListener { marker ->
                    markers[marker.id]?.let { pinCallback(it) }; false
                }
                native.addOnCameraIdleListener {
                    if (styleReady && lifetime.viewportApplied && !lifetime.destroyed)
                        native.cameraPosition.target?.let { centerCallback?.invoke(it.latitude, it.longitude) }
                }
            }
        }
    }
    LaunchedEffect(map, retry) {
        val native = map ?: return@LaunchedEffect
        styleReady = false; error = null
        native.setStyle(styleUrl) { if (!lifetime.destroyed) { styleReady = true; error = null } }
    }
    LaunchedEffect(map, interactive) {
        map?.uiSettings?.apply {
            isScrollGesturesEnabled = interactive; isZoomGesturesEnabled = interactive
            isRotateGesturesEnabled = interactive; isTiltGesturesEnabled = interactive
            isDoubleTapGesturesEnabled = interactive
            isQuickZoomGesturesEnabled = interactive
        }
    }
    LaunchedEffect(map, latitude, longitude, zoomMeters, viewportDp) {
        val native = map ?: return@LaunchedEffect
        if (validMapCoordinate(latitude, longitude)) {
            native.cameraPosition = CameraPosition.Builder().target(LatLng(latitude.coerceIn(-85.0, 85.0), longitude))
                .zoom(mapZoomForMeters(latitude, zoomMeters, viewportDp)).build()
            lifetime.viewportApplied = true
        }
    }
    LaunchedEffect(map, styleReady, pins, selectedPinId) {
        val native = map ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect
        native.removeAnnotations(); markers.clear()
        val icons = mutableMapOf<Pair<String, Boolean>, org.maplibre.android.annotations.Icon>()
        pins.filter { validMapCoordinate(it.latitude, it.longitude) }.distinctBy { it.id }.forEach { pin ->
            val icon = icons.getOrPut(pin.sportEmoji to (pin.id == selectedPinId)) {
                val bitmap = android.graphics.Bitmap.createBitmap(72, 80, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                paint.color = if (pin.id == selectedPinId) android.graphics.Color.rgb(224, 90, 18) else android.graphics.Color.WHITE
                canvas.drawCircle(36f, 36f, 32f, paint)
                paint.color = android.graphics.Color.rgb(224, 90, 18); canvas.drawCircle(36f, 74f, 5f, paint)
                paint.textSize = 32f; paint.textAlign = android.graphics.Paint.Align.CENTER
                canvas.drawText(pin.sportEmoji, 36f, 47f, paint)
                IconFactory.getInstance(context).fromBitmap(bitmap)
            }
            val marker = native.addMarker(MarkerOptions().position(LatLng(pin.latitude, pin.longitude))
                .title(pin.title).snippet(pin.subtitle).icon(icon))
            markers[marker.id] = pin.id
            if (pin.id == selectedPinId) native.selectMarker(marker)
        }
    }
    Box(modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize().onSizeChanged {
            if (it.width > 0 && it.height > 0) viewportDp = minOf(it.width, it.height) / density.toDouble()
        })
        error?.let { message -> Surface(Modifier.align(Alignment.Center).padding(12.dp), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text(message, style = MaterialTheme.typography.bodySmall)
                androidx.compose.material3.TextButton(onClick = { retry++ }) { Text("Tekrar dene") }
            }
        } }
    }
}

@Composable
private fun PreviewNativeMapView(
    modifier: Modifier,
    latitude: Double,
    longitude: Double,
    zoomMeters: Double,
    pins: List<MapPinData>,
    selectedPinId: String?,
    onPinSelected: (String) -> Unit,
    interactive: Boolean
) {
    Box(
        modifier = modifier
            .background(Color(0xFFF8FAFC))
            .drawBehind {
                val w = size.width
                val h = size.height

                // 1. Marmara Denizi & Sahil
                val seaBrush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFFD6EAF8), Color(0xFFEBF5FB)),
                    startX = 0f,
                    endX = w * 0.35f
                )
                val coastPath = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(w * 0.28f, 0f)
                    cubicTo(w * 0.32f, h * 0.25f, w * 0.36f, h * 0.45f, w * 0.30f, h * 0.65f)
                    cubicTo(w * 0.26f, h * 0.80f, w * 0.34f, h * 0.90f, w * 0.28f, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(coastPath, brush = seaBrush)
                drawPath(coastPath, color = Color(0xFFAED6F1), style = Stroke(width = 2.5f))

                // 2. Parklar
                val parkColor = Color(0xFFE8F8F5)
                val parkBorder = Color(0xFFA3E4D7)

                drawRoundRect(parkColor, Offset(w * 0.32f, h * 0.12f), Size(w * 0.30f, h * 0.18f), CornerRadius(18f, 18f))
                drawRoundRect(parkBorder, Offset(w * 0.32f, h * 0.12f), Size(w * 0.30f, h * 0.18f), CornerRadius(18f, 18f), style = Stroke(1.5f))

                drawRoundRect(parkColor, Offset(w * 0.34f, h * 0.45f), Size(w * 0.38f, h * 0.20f), CornerRadius(18f, 18f))
                drawRoundRect(parkBorder, Offset(w * 0.34f, h * 0.45f), Size(w * 0.38f, h * 0.20f), CornerRadius(18f, 18f), style = Stroke(1.5f))

                // 3. Caddeler
                fun drawCleanRoad(start: Offset, end: Offset, width: Float) {
                    drawLine(Color(0xFFCBD5E1), start, end, strokeWidth = width + 2f)
                    drawLine(Color.White, start, end, strokeWidth = width)
                }

                drawCleanRoad(Offset(w * 0.30f, 0f), Offset(w * 0.30f, h), 12f)
                drawCleanRoad(Offset(w * 0.30f, h * 0.35f), Offset(w, h * 0.35f), 10f)
                drawCleanRoad(Offset(w * 0.30f, h * 0.70f), Offset(w, h * 0.70f), 10f)
                drawCleanRoad(Offset(w * 0.65f, 0f), Offset(w * 0.65f, h), 8f)
            }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val totalWidth = maxWidth
            val totalHeight = maxHeight

            // Koordinat oranlarına göre pin yerleşimi
            pins.forEachIndexed { index, pin ->
                val isSelected = pin.id == selectedPinId
                // Dağıtılmış koordinatlar
                val xRatio = when (index) {
                    0 -> 0.38f
                    1 -> 0.58f
                    2 -> 0.76f
                    3 -> 0.46f
                    4 -> 0.72f
                    5 -> 0.35f
                    else -> 0.64f
                }
                val yRatio = when (index) {
                    0 -> 0.22f
                    1 -> 0.36f
                    2 -> 0.24f
                    3 -> 0.48f
                    4 -> 0.42f
                    5 -> 0.32f
                    else -> 0.52f
                }

                val xPos = totalWidth * xRatio
                val yPos = totalHeight * yRatio

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .offset(x = (xPos.value - 30).dp, y = (yPos.value - 20).dp)
                        .clickable(enabled = interactive) { onPinSelected(pin.id) }
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MColors.Brand else Color.White,
                        shadowElevation = if (isSelected) 8.dp else 3.dp,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) MColors.BrandLight else Color(0x20000000)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(pin.sportEmoji, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = pin.title.take(12),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (isSelected) Color.White else MColors.Ink900
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) MColors.Brand else Color(0xFF64748B))
                    )
                }
            }
        }
    }
}
