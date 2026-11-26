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

private val iosRegionCenterCache = mutableMapOf<String, DeviceLocation>()

@Composable
actual fun rememberRegionCenter(city: String, district: String): DeviceLocation? {
    val regionCity = city.trim()
    val regionDistrict = district.trim()
    val cacheKey = "${com.matchora.app.state.canonicalTurkishKey(regionCity)}|${com.matchora.app.state.canonicalTurkishKey(regionDistrict)}"
    var center by remember(regionCity, regionDistrict) { mutableStateOf(iosRegionCenterCache[cacheKey]) }
    LaunchedEffect(regionCity, regionDistrict) {
        if (regionCity.isBlank() || center != null) return@LaunchedEffect
        val geocoder = CLGeocoder()
        val query = listOf(regionDistrict, regionCity, "Türkiye").filter { it.isNotBlank() }.joinToString(", ")
        try {
            val resolved = withTimeoutOrNull(12000) {
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
            if (resolved != null) {
                iosRegionCenterCache[cacheKey] = resolved
                center = resolved
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
                timeout?.cancel()
                timeout = scope.launch { delay(25000); finish(LocationOutcome.Failed("Konum zamanında alınamadı. Tekrar dene veya bölgeni elle seç.")) }
                val cached = manager.location?.takeIf {
                    it.coordinate.useContents { validMapCoordinate(latitude, longitude) } &&
                        it.horizontalAccuracy.isFinite() && it.horizontalAccuracy >= 0.0
                }
                if (cached != null) {
                    locationManager(manager, listOf(cached))
                } else {
                    manager.startUpdatingLocation()
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
            it.coordinate.useContents { validMapCoordinate(latitude, longitude) } &&
                it.horizontalAccuracy.isFinite() && it.horizontalAccuracy >= 0.0
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
                    val rawCity = place?.administrativeArea ?: place?.locality ?: place?.subAdministrativeArea ?: place?.name
                    val rawDistrict = place?.subAdministrativeArea ?: place?.locality ?: place?.subLocality ?: "Merkez"
                    finish(LocationOutcome.Located(DeviceLocation(latitude, longitude, canonicalRegionCity(rawCity), canonicalRegionDistrict(rawDistrict) ?: "Merkez", capturedAt)))
                }
            }
        }
    }
    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        if (!active || resolving) return
        // Core Location's transient locationUnknown error has code 0; retain the bounded retry window.
        if (didFailWithError.code == 0L) scope.launch { delay(800); if (active && !resolving) manager.startUpdatingLocation() }
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
                UIApplication.sharedApplication.registerForRemoteNotifications()
                callback(granted)
            }
        }
    }
}

actual fun hasForegroundLocationPermission(): Boolean {
    val status = CLLocationManager.authorizationStatus()
    return CLLocationManager.locationServicesEnabled() && (status == kCLAuthorizationStatusAuthorizedWhenInUse || status == kCLAuthorizationStatusAuthorizedAlways)
}

private const val INITIAL_NOTIFICATION_PROMPT_KEY = "matchora.initial_notification_prompted"
private const val INITIAL_LOCATION_PROMPT_KEY = "matchora.initial_location_prompted"

actual fun hasPromptedInitialNotificationPermission(): Boolean =
    NSUserDefaults.standardUserDefaults.boolForKey(INITIAL_NOTIFICATION_PROMPT_KEY)

actual fun markInitialNotificationPermissionPrompted() {
    NSUserDefaults.standardUserDefaults.setBool(true, forKey = INITIAL_NOTIFICATION_PROMPT_KEY)
}

actual fun hasPromptedInitialLocationPermission(): Boolean =
    NSUserDefaults.standardUserDefaults.boolForKey(INITIAL_LOCATION_PROMPT_KEY)

actual fun markInitialLocationPermissionPrompted() {
    NSUserDefaults.standardUserDefaults.setBool(true, forKey = INITIAL_LOCATION_PROMPT_KEY)
}

private const val REGION_CITY_KEY = "matchora.region.city"
private const val REGION_DISTRICT_KEY = "matchora.region.district"
private const val REGION_IS_GPS_KEY = "matchora.region.is_gps"
private const val REGION_MANUAL_NON_GPS_KEY = "matchora.region.manual_non_gps"
private const val REGION_HAS_COORDS_KEY = "matchora.region.has_coords"
private const val REGION_LAT_KEY = "matchora.region.lat"
private const val REGION_LON_KEY = "matchora.region.lon"

actual fun loadPersistedRegionSelection(): PersistedRegionSelection? {
    val defaults = NSUserDefaults.standardUserDefaults
    val rawCity = defaults.stringForKey(REGION_CITY_KEY)?.trim()?.takeIf { it.isNotBlank() } ?: return null
    val city = canonicalRegionCity(rawCity) ?: rawCity
    val rawDistrict = defaults.stringForKey(REGION_DISTRICT_KEY)?.trim()?.takeIf { it.isNotBlank() } ?: "Merkez"
    val district = canonicalRegionDistrict(rawDistrict) ?: rawDistrict
    val isGps = defaults.boolForKey(REGION_IS_GPS_KEY)
    val manualNonGps = defaults.boolForKey(REGION_MANUAL_NON_GPS_KEY)
    val hasCoords = defaults.boolForKey(REGION_HAS_COORDS_KEY)
    val lat = if (hasCoords) defaults.doubleForKey(REGION_LAT_KEY) else null
    val lon = if (hasCoords) defaults.doubleForKey(REGION_LON_KEY) else null
    return PersistedRegionSelection(
        city = city,
        district = district,
        isGps = isGps,
        manualNonGps = manualNonGps,
        latitude = lat?.takeIf { validMapCoordinate(it, lon ?: 0.0) },
        longitude = lon?.takeIf { validMapCoordinate(lat ?: 0.0, it) }
    )
}

actual fun savePersistedRegionSelection(selection: PersistedRegionSelection) {
    val defaults = NSUserDefaults.standardUserDefaults
    val cleanCity = canonicalRegionCity(selection.city) ?: selection.city.trim()
    val cleanDistrict = canonicalRegionDistrict(selection.district) ?: selection.district.trim().ifBlank { "Merkez" }
    defaults.setObject(cleanCity, forKey = REGION_CITY_KEY)
    defaults.setObject(cleanDistrict, forKey = REGION_DISTRICT_KEY)
    defaults.setBool(selection.isGps, forKey = REGION_IS_GPS_KEY)
    defaults.setBool(selection.manualNonGps, forKey = REGION_MANUAL_NON_GPS_KEY)
    if (selection.latitude != null && selection.longitude != null && validMapCoordinate(selection.latitude, selection.longitude)) {
        defaults.setBool(true, forKey = REGION_HAS_COORDS_KEY)
        defaults.setDouble(selection.latitude, forKey = REGION_LAT_KEY)
        defaults.setDouble(selection.longitude, forKey = REGION_LON_KEY)
    } else {
        defaults.setBool(false, forKey = REGION_HAS_COORDS_KEY)
    }
}
