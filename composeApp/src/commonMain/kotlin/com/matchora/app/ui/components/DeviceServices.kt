package com.matchora.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*

data class DeviceLocation(val latitude: Double, val longitude: Double, val city: String?, val district: String?, val capturedAt: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds())
object DeviceLocationState {
    val current = kotlinx.coroutines.flow.MutableStateFlow<DeviceLocation?>(null)
}

internal fun freshLocationFix(latitude: Double, longitude: Double, ageMillis: Double, accuracyMeters: Double): Boolean =
    validMapCoordinate(latitude, longitude) && ageMillis.isFinite() && ageMillis in -5000.0..120000.0 &&
        accuracyMeters.isFinite() && accuracyMeters >= 0

/** Native geocoders can return an English spelling even when the app uses Turkish city names. */
internal fun canonicalRegionCity(value: String?): String? {
    val city = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
    fun key(text: String) = text.replace('İ', 'i').replace('ı', 'i').replace('Ş', 's').replace('ş', 's')
        .replace('Ğ', 'g').replace('ğ', 'g').replace('Ü', 'u').replace('ü', 'u')
        .replace('Ö', 'o').replace('ö', 'o').replace('Ç', 'c').replace('ç', 'c').lowercase()
    return com.matchora.app.state.turkeyCities.firstOrNull { key(it) == key(city) } ?: city
}

fun approximateDistanceKm(from: DeviceLocation, latitude: Double, longitude: Double): Float {
    val radians = kotlin.math.PI / 180.0
    val dLat = (latitude - from.latitude) * radians
    val dLon = (longitude - from.longitude) * radians
    val a = kotlin.math.sin(dLat / 2).let { it * it } + kotlin.math.cos(from.latitude * radians) * kotlin.math.cos(latitude * radians) * kotlin.math.sin(dLon / 2).let { it * it }
    return (6371.0 * 2 * kotlin.math.asin(kotlin.math.sqrt(a.coerceIn(0.0, 1.0)))).toFloat()
}
sealed class LocationOutcome {
    data class Located(val value: DeviceLocation) : LocationOutcome()
    data class Failed(val message: String, val settingsAvailable: Boolean = false) : LocationOutcome()
}

@Composable
expect fun rememberLocationRequester(requestPermission: Boolean = true, onResult: (LocationOutcome) -> Unit): () -> Unit

/** Resolves a manually chosen region without requesting device location permission. */
@Composable
expect fun rememberRegionCenter(city: String, district: String): DeviceLocation?

@Composable
expect fun rememberNotificationPermissionRequester(onResult: (Boolean) -> Unit): () -> Unit

expect fun openApplicationSettings()

expect fun shareAccountExport(content: String)

@Composable
expect fun rememberAppActive(): Boolean

@Composable
fun NotificationPermissionButton() {
    var message by remember { mutableStateOf<String?>(null) }
    val request = rememberNotificationPermissionRequester { granted ->
        message = if (granted) "Bildirim izni açık. Bildirim tercihlerini bu ekrandan yönetebilirsin."
        else "Bildirim izni kapalı. İstersen cihaz ayarlarından Matchora bildirimlerini açabilirsin."
    }
    MPremiumMenuRow(
        text = "Cihaz bildirim iznini kontrol et",
        subtitle = "Maç, kadro ve sohbet bildirimleri için sistem izni",
        icon = Icons.Rounded.NotificationsActive,
        onClick = request
    )
    message?.let { text -> AlertDialog(onDismissRequest = { message = null },
        title = { Text("Bildirim izni") }, text = { Text(text) },
        confirmButton = { TextButton(onClick = { message = null }) { Text("Tamam") } },
        dismissButton = { TextButton(onClick = { message = null; openApplicationSettings() }) { Text("Cihaz ayarları") } }) }
}

expect fun hasForegroundLocationPermission(): Boolean

/** Refresh only an already granted foreground location; never opens a permission dialog. */
@Composable
fun rememberForegroundLocationRefresh(active: Boolean) {
    if (!active) return
    val location by DeviceLocationState.current.collectAsState()
    if (location == null) return
    val request = rememberLocationRequester(requestPermission = false) { result ->
        DeviceLocationState.current.value = when (result) {
            is LocationOutcome.Located -> result.value
            is LocationOutcome.Failed -> null
        }
    }
    LaunchedEffect(active, location != null) {
        if (active && location != null) {
            while (true) {
                if (!hasForegroundLocationPermission()) { DeviceLocationState.current.value = null; break }
                val point = DeviceLocationState.current.value ?: break
                if (kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - point.capturedAt >= 60000) request()
                kotlinx.coroutines.delay(60000)
            }
        }
    }
}
