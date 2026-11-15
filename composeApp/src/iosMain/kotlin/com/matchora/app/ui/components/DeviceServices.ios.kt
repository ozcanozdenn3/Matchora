@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.matchora.app.ui.components

import androidx.compose.runtime.*
import kotlinx.cinterop.useContents
import kotlinx.coroutines.*
import platform.CoreLocation.*
import platform.Foundation.*
import platform.UIKit.*
import platform.UserNotifications.*
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume

actual fun shareAccountExport(content: String) {
    val folder = NSTemporaryDirectory() + "matchora-exports/"
    val files = NSFileManager.defaultManager
    files.createDirectoryAtPath(folder, withIntermediateDirectories = true, attributes = null, error = null)
    (files.contentsOfDirectoryAtPath(folder, error = null) as? List<*>)?.forEach { files.removeItemAtPath(folder + it.toString(), error = null) }
    val path = folder + "matchora-account.json"
    check(NSString.create(string = content).writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)) { "Dışa aktarma dosyası oluşturulamadı." }
    val root = (UIApplication.sharedApplication.keyWindow
        ?: UIApplication.sharedApplication.windows.filterIsInstance<UIWindow>().firstOrNull { it.isKeyWindow() })?.rootViewController
        ?: error("Paylaşım penceresi açılamadı. Tekrar deneyebilirsin.")
    val top = generateSequence(root) { it.presentedViewController }.last()
    val sheet = UIActivityViewController(listOf(NSURL.fileURLWithPath(path)), null)
    sheet.popoverPresentationController?.sourceView = top.view
    top.presentViewController(sheet, animated = true, completion = null)
}

@Composable
actual fun rememberAppActive(): Boolean {
    var active by remember { mutableStateOf(UIApplication.sharedApplication.applicationState == UIApplicationState.UIApplicationStateActive) }
    DisposableEffect(Unit) {
        val center = NSNotificationCenter.defaultCenter
        val resumed = center.addObserverForName(UIApplicationDidBecomeActiveNotification, null, NSOperationQueue.mainQueue) {
            active = true
            if (CLLocationManager.authorizationStatus() !in listOf(kCLAuthorizationStatusAuthorizedAlways, kCLAuthorizationStatusAuthorizedWhenInUse))
                DeviceLocationState.current.value = null
        }
        val paused = center.addObserverForName(UIApplicationWillResignActiveNotification, null, NSOperationQueue.mainQueue) { active = false }
        onDispose { center.removeObserver(resumed); center.removeObserver(paused) }
    }
    return active
}

actual fun openApplicationSettings() {
    NSURL.URLWithString(UIApplicationOpenSettingsURLString)?.let {
        UIApplication.sharedApplication.openURL(it, options = emptyMap<Any?, Any>(), completionHandler = null)
    }
}

@Composable
actual fun rememberRegionCenter(city: String, district: String): DeviceLocation? {
    val regionCity = city.trim()
    val regionDistrict = district.trim()
    var center by remember(regionCity, regionDistrict) { mutableStateOf<DeviceLocation?>(null) }
    LaunchedEffect(regionCity, regionDistrict) {
        if (regionCity.isBlank()) return@LaunchedEffect
        val geocoder = CLGeocoder()
        val query = listOf(regionDistrict, regionCity, "Türkiye").filter { it.isNotBlank() }.joinToString(", ")
        try {
            center = withTimeoutOrNull(12000) {
                suspendCancellableCoroutine<DeviceLocation?> { continuation ->
                    continuation.invokeOnCancellation {
                        dispatch_async(dispatch_get_main_queue()) { geocoder.cancelGeocode() }
                    }
                    geocoder.geocodeAddressString(query) { placemarks, _ ->
                        dispatch_async(dispatch_get_main_queue()) {
                            if (continuation.isActive) {
                                val location = (placemarks?.firstOrNull() as? CLPlacemark)?.location
                                val result = location?.coordinate?.useContents {
                                    DeviceLocation(latitude, longitude, regionCity, regionDistrict.ifBlank { null })
                                }
                                continuation.resume(result)
                            }
                        }
                    }
                }
            }
        } finally {
            geocoder.cancelGeocode()
        }
    }
    return center
}

private class IosLocationRequest(
    private val scope: CoroutineScope,
    private val requestPermission: Boolean,
    private val callback: (LocationOutcome) -> Unit
) : NSObject(), CLLocationManagerDelegateProtocol {
    private val manager = CLLocationManager().apply { desiredAccuracy = kCLLocationAccuracyHundredMeters }
    private val geocoder = CLGeocoder()
    private var active = false
    private var requestedLocation = false
    private var resolving = false
    private var timeout: Job? = null

    fun start() {
        active = true; manager.delegate = this
        if (!CLLocationManager.locationServicesEnabled()) {
            finish(LocationOutcome.Failed("Cihazın konum hizmetleri kapalı.", true)); return
        }
        timeout = scope.launch { delay(25000); finish(LocationOutcome.Failed("Konum zamanında alınamadı. Tekrar dene veya bölgeni elle seç.")) }
        authorize()
    }
    fun cancel() {
        active = false; timeout?.cancel(); timeout = null
        geocoder.cancelGeocode(); manager.stopUpdatingLocation(); manager.delegate = null
    }
    private fun finish(result: LocationOutcome) {
        if (!active) return
        cancel(); callback(result)
    }
    private fun authorize() {
        if (!active) return
        when (CLLocationManager.authorizationStatus()) {
            kCLAuthorizationStatusAuthorizedAlways, kCLAuthorizationStatusAuthorizedWhenInUse -> if (!requestedLocation) {
                requestedLocation = true
                val cached = manager.location?.takeIf {
                    it.coordinate.useContents { freshLocationFix(latitude, longitude, -it.timestamp.timeIntervalSinceNow * 1000.0, it.horizontalAccuracy) }
                }
                if (cached != null) {
                    locationManager(manager, listOf(cached))
                } else {
                    manager.startUpdatingLocation()
                    manager.requestLocation()
                }
            }
            kCLAuthorizationStatusDenied, kCLAuthorizationStatusRestricted -> {
                DeviceLocationState.current.value = null
                finish(LocationOutcome.Failed("Konum izni kapalı. Ayarlardan izin verebilir veya bölgeni elle seçebilirsin.", true))
            }
            else -> if (requestPermission) manager.requestWhenInUseAuthorization()
                else finish(LocationOutcome.Failed("Konum izni kapalı. Bölgeni elle seçebilir veya cihaz ayarlarından izin verebilirsin.", true))
        }
    }
    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) { authorize() }
    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        if (!active || resolving) return
        val location = didUpdateLocations.filterIsInstance<CLLocation>().lastOrNull {
            it.coordinate.useContents { freshLocationFix(latitude, longitude, -it.timestamp.timeIntervalSinceNow * 1000.0, it.horizontalAccuracy) }
        } ?: return
        val latitude = location.coordinate.useContents { latitude }
        val longitude = location.coordinate.useContents { longitude }
        val capturedAt = (location.timestamp.timeIntervalSince1970 * 1000.0).toLong()
        resolving = true; manager.stopUpdatingLocation(); timeout?.cancel()
        // A reverse-geocoder outage must not discard a valid, fresh GPS fix.
        timeout = scope.launch { delay(6000); finish(LocationOutcome.Located(DeviceLocation(latitude, longitude, null, null, capturedAt))) }
        geocoder.reverseGeocodeLocation(location) { placemarks, _ ->
            dispatch_async(dispatch_get_main_queue()) {
                if (active) {
                    val place = placemarks?.firstOrNull() as? CLPlacemark
                    finish(LocationOutcome.Located(DeviceLocation(latitude, longitude, canonicalRegionCity(place?.administrativeArea),
                        place?.subAdministrativeArea ?: place?.locality, capturedAt)))
                }
            }
        }
    }
    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        if (!active || resolving) return
        // Core Location's transient locationUnknown error has code 0; retain the bounded retry window.
        if (didFailWithError.code == 0L) scope.launch { delay(800); if (active && !resolving) manager.requestLocation() }
        else finish(LocationOutcome.Failed("Konum alınamadı. Konum hizmetlerini kontrol et veya bölgeni elle seç.", true))
    }
}

@Composable
actual fun rememberLocationRequester(requestPermission: Boolean, onResult: (LocationOutcome) -> Unit): () -> Unit {
    val callback by rememberUpdatedState(onResult)
    val scope = rememberCoroutineScope()
    var request by remember { mutableStateOf<IosLocationRequest?>(null) }
    DisposableEffect(Unit) { onDispose { request?.cancel() } }
    return {
        request?.cancel()
        request = IosLocationRequest(scope, requestPermission) { callback(it) }.also { it.start() }
    }
}

@Composable
actual fun rememberNotificationPermissionRequester(onResult: (Boolean) -> Unit): () -> Unit {
    val callback by rememberUpdatedState(onResult)
    return {
        UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound
        ) { granted, _ ->
            dispatch_async(dispatch_get_main_queue()) {
                if (granted) UIApplication.sharedApplication.registerForRemoteNotifications()
                callback(granted)
            }
        }
    }
}

actual fun hasForegroundLocationPermission(): Boolean {
    val status = CLLocationManager.authorizationStatus()
    return CLLocationManager.locationServicesEnabled() && (status == kCLAuthorizationStatusAuthorizedWhenInUse || status == kCLAuthorizationStatusAuthorizedAlways)
}
