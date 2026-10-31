package com.matchora.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

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
