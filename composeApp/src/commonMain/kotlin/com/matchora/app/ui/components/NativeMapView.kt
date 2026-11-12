package com.matchora.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlin.math.cos
import kotlin.math.ln

internal fun validMapCoordinate(latitude: Double, longitude: Double): Boolean =
    latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0

/** Converts a ground span to MapLibre's 512-pixel zoom scale. */
internal fun mapZoomForMeters(latitude: Double, spanMeters: Double, viewportDp: Double): Double {
    val safeLatitude = latitude.takeIf { it.isFinite() }?.coerceIn(-85.0, 85.0) ?: 39.0
    val span = spanMeters.takeIf { it.isFinite() && it > 0 } ?: 4200.0
    val size = viewportDp.takeIf { it.isFinite() && it > 0 } ?: 256.0
    return (ln(40075016.68557849 * cos(safeLatitude * kotlin.math.PI / 180.0) * size / (512.0 * span)) / ln(2.0)).coerceIn(0.0, 20.0)
}

data class MapPinData(
    val id: String,
    val title: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
    val sportEmoji: String
)

@Composable
expect fun NativeMapView(
    modifier: Modifier = Modifier,
    latitude: Double,
    longitude: Double,
    zoomMeters: Double,
    pins: List<MapPinData>,
    selectedPinId: String?,
    onPinSelected: (String) -> Unit,
    interactive: Boolean = true
)
