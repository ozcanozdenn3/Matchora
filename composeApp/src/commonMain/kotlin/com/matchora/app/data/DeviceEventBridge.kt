package com.matchora.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PushDevice(val platform: String, val token: String)
data class PendingNavigation(val destination: String, val targetId: String)
object DeviceEventBridge {
    val nativeServicesEnabled: Boolean get() = BackendBuildConfig.MODE != "preview"
    private val deviceState = MutableStateFlow<PushDevice?>(null)
    val devices = deviceState.asStateFlow()
    private val navigationState = MutableStateFlow<PendingNavigation?>(null)
    val navigation = navigationState.asStateFlow()
    fun updatePushToken(platform: String, token: String) {
        if (platform in listOf("ios", "android") && token.length in 16..4096) deviceState.value = PushDevice(platform, token)
    }
    fun openNotification(destination: String, targetId: String) {
        if (destination in listOf("EVENT", "CHAT", "CHALLENGES", "TEAM", "TOURNAMENT", "WALLET", "PLUS", "ACCOUNT", "RANKING", "PASSPORT", "PLAY", "NETWORK", "VENUES", "MODERATION", "RESERVATIONS", "VENUE_MANAGEMENT") && targetId.length <= 200)
            navigationState.value = PendingNavigation(destination, targetId)
    }
    fun openUrl(url: String) {
        if (BrowserAuthCallback.deliver(url)) return
        val id = url.removePrefix("matchora://event/").substringBefore('?').substringBefore('#')
        if (url.startsWith("matchora://event/") && (Regex("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}").matches(id) ||
            BackendBuildConfig.MODE == "preview" && Regex("[A-Za-z0-9_-]{1,100}").matches(id))) openNotification("EVENT", id.lowercase())
    }
    fun consumeNavigation() { navigationState.value = null }
}
