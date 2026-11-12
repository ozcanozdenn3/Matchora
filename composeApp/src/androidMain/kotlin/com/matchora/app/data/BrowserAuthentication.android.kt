package com.matchora.app.data

import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual suspend fun authenticateInBrowser(url: String): String = withTimeout(5 * 60 * 1000L) {
    suspendCancellableCoroutine { continuation ->
        val callback: (String) -> Unit = { result -> if (continuation.isActive) continuation.resume(result) }
        BrowserAuthCallback.handler = callback
        continuation.invokeOnCancellation { if (BrowserAuthCallback.handler === callback) BrowserAuthCallback.handler = null }
        try {
            require(Uri.parse(url).scheme == "https")
            AndroidBackendRuntime.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            if (BrowserAuthCallback.handler === callback) BrowserAuthCallback.handler = null
            continuation.resumeWithException(BackendFailure("BROWSER_UNAVAILABLE", "Giriş penceresi açılamadı. Cihazında bir tarayıcı bulunduğunu kontrol et."))
        }
    }.also { BrowserAuthCallback.handler = null }
}
