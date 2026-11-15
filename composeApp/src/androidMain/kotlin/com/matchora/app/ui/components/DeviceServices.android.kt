package com.matchora.app.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.matchora.app.data.AndroidBackendRuntime
import kotlinx.coroutines.*
import java.util.Locale
import kotlin.coroutines.resume

actual fun shareAccountExport(content: String) {
    val context = AndroidBackendRuntime.context
    val folder = java.io.File(context.cacheDir, "exports").apply { mkdirs() }
    folder.listFiles()?.forEach { it.delete() }
    val file = java.io.File(folder, "matchora-account.json").apply { writeText(content) }
    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.photos", file)
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "application/json"; putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }, "Verilerini kaydet").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

@Composable
actual fun rememberAppActive(): Boolean {
    val context = LocalContext.current
    val activity = remember(context) { generateSequence(context) { (it as? android.content.ContextWrapper)?.baseContext }
        .filterIsInstance<androidx.activity.ComponentActivity>().firstOrNull() }
    var active by remember { mutableStateOf(activity?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) == true) }
    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            active = activity?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) == true
            if (event == Lifecycle.Event.ON_RESUME &&
                context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                DeviceLocationState.current.value = null
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }
    return active
}

actual fun openApplicationSettings() {
    val context = runCatching { AndroidBackendRuntime.context }.getOrNull() ?: return
    runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** A provider may ignore interruption; its late reply must never keep the UI suspended. */
private suspend fun boundedGeocode(scope: CoroutineScope, timeoutMillis: Long = 6000, request: () -> android.location.Address?): android.location.Address? =
    withTimeoutOrNull(timeoutMillis) {
        suspendCancellableCoroutine { continuation ->
            val worker = scope.launch(Dispatchers.IO) {
                val result = runCatching { runInterruptible { request() } }.getOrNull()
                if (continuation.isActive) continuation.resume(result)
            }
            continuation.invokeOnCancellation { worker.cancel() }
        }
    }

@Composable
actual fun rememberRegionCenter(city: String, district: String): DeviceLocation? {
    val context = LocalContext.current.applicationContext
    val geocodingScope = rememberCoroutineScope()
    val regionCity = city.trim()
    val regionDistrict = district.trim()
    var center by remember(regionCity, regionDistrict) { mutableStateOf<DeviceLocation?>(null) }
    LaunchedEffect(regionCity, regionDistrict) {
        if (regionCity.isBlank() || !Geocoder.isPresent()) return@LaunchedEffect
        val query = listOf(regionDistrict, regionCity, "Türkiye").filter { it.isNotBlank() }.joinToString(", ")
        center = try {
            val address = boundedGeocode(geocodingScope, 12000) {
                @Suppress("DEPRECATION")
                Geocoder(context, Locale("tr", "TR")).getFromLocationName(query, 1)?.firstOrNull()
            }
            address?.takeIf { it.hasLatitude() && it.hasLongitude() && validMapCoordinate(it.latitude, it.longitude) }?.let {
                DeviceLocation(it.latitude, it.longitude, regionCity, regionDistrict.ifBlank { null })
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }
    return center
}

@SuppressLint("MissingPermission")
@Composable
actual fun rememberLocationRequester(requestPermission: Boolean, onResult: (LocationOutcome) -> Unit): () -> Unit {
    val context = LocalContext.current
    val callback by rememberUpdatedState(onResult)
    val scope = rememberCoroutineScope()
    val manager = remember { context.getSystemService(LocationManager::class.java) }
    var listener by remember { mutableStateOf<LocationListener?>(null) }
    var timeout by remember { mutableStateOf<Job?>(null) }
    var reverseLookup by remember { mutableStateOf<Job?>(null) }
    var generation by remember { mutableIntStateOf(0) }
    val stop = {
        listener?.let { runCatching { manager.removeUpdates(it) } }
        listener = null; timeout?.cancel(); timeout = null
    }
    DisposableEffect(manager) {
        onDispose { generation++; stop(); reverseLookup?.cancel() }
    }
    val locate = {
        stop(); reverseLookup?.cancel(); generation++
        val requestGeneration = generation
        val fine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val providers = listOfNotNull(
            if (Build.VERSION.SDK_INT >= 31) LocationManager.FUSED_PROVIDER else null,
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        ).distinct().filter {
            (it != LocationManager.GPS_PROVIDER || fine) && runCatching { manager.isProviderEnabled(it) }.getOrDefault(false)
        }
        if (providers.isEmpty()) callback(LocationOutcome.Failed("Konum alınamadı. Cihazın konum hizmetlerini kontrol et veya bölgeni elle seç.", true))
        else {
            var delivered = false
            fun valid(location: Location): Boolean = freshLocationFix(location.latitude, location.longitude,
                (android.os.SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1000000.0,
                if (location.hasAccuracy()) location.accuracy.toDouble() else Double.NaN)
            val deliver: (Location) -> Unit = { location ->
                if (!delivered && generation == requestGeneration && valid(location)) {
                    delivered = true; stop()
                    reverseLookup = scope.launch {
                        val address = if (Geocoder.isPresent()) boundedGeocode(scope) {
                            @Suppress("DEPRECATION")
                            Geocoder(context, Locale("tr", "TR")).getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
                        } else null
                        if (generation == requestGeneration) callback(LocationOutcome.Located(DeviceLocation(location.latitude, location.longitude,
                            canonicalRegionCity(address?.adminArea), address?.subAdminArea ?: address?.locality,
                            capturedAt = System.currentTimeMillis() - ((android.os.SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1000000L).coerceAtLeast(0))))
                    }
                }
            }
            val cached = providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
                .filter { valid(it) }.minByOrNull { if (it.hasAccuracy()) it.accuracy else Float.MAX_VALUE }
            if (cached != null) deliver(cached)
            else {
                val next = object : LocationListener {
                    override fun onLocationChanged(location: Location) = deliver(location)
                    override fun onProviderDisabled(provider: String) {
                        if (!delivered && generation == requestGeneration && providers.none { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }) {
                            delivered = true; stop(); callback(LocationOutcome.Failed("Konum hizmeti kapatıldı. Tekrar deneyebilirsin.", true))
                        }
                    }
                    override fun onProviderEnabled(provider: String) {}
                    @Deprecated("Legacy location callback")
                    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                }
                listener = next
                val requested = providers.count { provider -> runCatching {
                    @Suppress("DEPRECATION")
                    manager.requestSingleUpdate(provider, next, Looper.getMainLooper())
                }.isSuccess }
                if (requested == 0) { stop(); callback(LocationOutcome.Failed("Konum alınamadı. Konum iznini ve cihaz ayarlarını kontrol et.", true)) }
                else timeout = scope.launch {
                    delay(25000)
                    if (!delivered && generation == requestGeneration) {
                        delivered = true; stop(); callback(LocationOutcome.Failed("Konum zamanında alınamadı. Tekrar dene veya bölgeni elle seç."))
                    }
                }
            }
        }
        Unit
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true || permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) locate()
        else { DeviceLocationState.current.value = null; callback(LocationOutcome.Failed("Yakındaki maçları bulmak için konum izni gerekiyor. Bölgeni elle de seçebilirsin.", true)) }
    }
    return {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) locate()
        else if (requestPermission) permission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
        else { DeviceLocationState.current.value = null; callback(LocationOutcome.Failed("Konum izni kapalı. Bölgeni elle seçebilir veya cihaz ayarlarından izin verebilirsin.", true)) }
    }
}

@Composable
actual fun rememberNotificationPermissionRequester(onResult: (Boolean) -> Unit): () -> Unit {
    val context = LocalContext.current
    val callback by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { callback(it) }
    return {
        if (Build.VERSION.SDK_INT >= 26) context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel("matchora_activity", "Maçlar ve mesajlar", NotificationManager.IMPORTANCE_DEFAULT))
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        else callback(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
}

actual fun hasForegroundLocationPermission(): Boolean {
    val context = runCatching { AndroidBackendRuntime.context }.getOrNull() ?: return false
    val manager = context.getSystemService(LocationManager::class.java)
    val fine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    return ((fine || coarse) && runCatching { manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)) ||
        (fine && runCatching { manager.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false))
}
