package com.matchora.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PushDevice(val platform: String, val token: String)
data class PendingNavigation(val destination: String, val targetId: String, val notificationId: String = "")

interface PhoneVerificationCallback {
    fun onCodeSent(verificationId: String)
    fun onError(error: String)
}

interface PhoneAuthCallback {
    fun onSuccess(idToken: String, phoneNumber: String)
    fun onError(error: String)
}

interface NativePhoneAuthDelegate {
    fun startVerification(phoneNumber: String, callback: PhoneVerificationCallback)
    fun verifyCode(verificationId: String, code: String, callback: PhoneAuthCallback)
}

object DeviceEventBridge {
    val nativeServicesEnabled: Boolean get() = BackendBuildConfig.MODE != "preview"
    private val deviceState = MutableStateFlow<PushDevice?>(null)
    val devices = deviceState.asStateFlow()
    private val navigationState = MutableStateFlow<PendingNavigation?>(null)
    val navigation = navigationState.asStateFlow()
    var phoneAuthDelegate: NativePhoneAuthDelegate? = null
    fun updatePushToken(platform: String, token: String) {
        if (platform in listOf("ios", "android") && token.length in 16..4096) deviceState.value = PushDevice(platform, token)
    }
    private val allowedNotificationDestinations = listOf(
        "EVENT", "CHAT", "CHALLENGES", "TEAM", "TOURNAMENT", "WALLET", "PLUS", "ACCOUNT",
        "RANKING", "PASSPORT", "PLAY", "NETWORK", "VENUES", "MEMORIES", "MODERATION",
        "RESERVATIONS", "VENUE_MANAGEMENT"
    )
    fun openNotification(destination: String, targetId: String) {
        openPushNotification(destination, targetId, "")
    }
    fun openPushNotification(destination: String, targetId: String, notificationId: String) {
        if (destination in allowedNotificationDestinations && targetId.length <= 200 && notificationId.length <= 200)
            navigationState.value = PendingNavigation(destination, targetId, notificationId)
    }
    fun openUrl(url: String) {
        if (BrowserAuthCallback.deliver(url)) return
        val routes = listOf(
            "matchora://event/" to "EVENT",
            "matchora://tournament/" to "TOURNAMENT",
            "matchora://team/" to "TEAM",
            "matchora://venue/" to "VENUES",
            "matchora://community/" to "NETWORK"
        )
        val (prefix, destination) = routes.firstOrNull { (p, _) -> url.startsWith(p) } ?: return
        val id = url.removePrefix(prefix).substringBefore('?').substringBefore('#')
        val validUuid = Regex("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}").matches(id)
        val validPreviewId = BackendBuildConfig.MODE == "preview" && Regex("[A-Za-z0-9_-]{1,100}").matches(id)
        if (validUuid || validPreviewId) openNotification(destination, id.lowercase())
    }
    fun consumeNavigation() { navigationState.value = null }
}
