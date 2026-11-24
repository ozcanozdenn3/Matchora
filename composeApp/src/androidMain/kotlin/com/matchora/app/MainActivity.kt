package com.matchora.app

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.matchora.app.data.AndroidBackendRuntime.context = applicationContext
        com.matchora.app.data.AndroidBackendRuntime.currentActivity = this
        com.matchora.app.data.initializePush()
        handleNotification(intent)
        intent?.dataString?.let(com.matchora.app.data.DeviceEventBridge::openUrl)
        setContent { App() }
    }
    override fun onDestroy() {
        if (com.matchora.app.data.AndroidBackendRuntime.currentActivity === this) {
            com.matchora.app.data.AndroidBackendRuntime.currentActivity = null
        }
        super.onDestroy()
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotification(intent)
        intent.dataString?.let(com.matchora.app.data.DeviceEventBridge::openUrl)
    }
    private fun handleNotification(intent: Intent?) {
        intent?.getStringExtra("destination")?.let { destination ->
            com.matchora.app.data.DeviceEventBridge.openPushNotification(
                destination,
                intent.getStringExtra("targetId").orEmpty(),
                intent.getStringExtra("notificationId").orEmpty()
            )
        }
    }
}
