package com.matchora.app.data

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual suspend fun observeRealtime(url: String, headers: Map<String, String>, onEvent: (String) -> Unit) {
    suspendCancellableCoroutine<Unit> { continuation ->
        val connection = URL(url).openConnection() as HttpURLConnection
        val worker = CoroutineScope(Dispatchers.IO).launch {
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 40000
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("Accept", "text/event-stream")
                headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
                val status = connection.responseCode
                if (status != 200) throw BackendFailure("REALTIME_REJECTED", "Anlık bağlantı açılamadı.", status)
                val decoder = RealtimeEventDecoder(onEvent)
                connection.inputStream.use { input ->
                    val buffer = ByteArray(4096)
                    while (continuation.isActive) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (count > 0) decoder.append(buffer.copyOf(count))
                    }
                }
                if (continuation.isActive) continuation.resume(Unit)
            } catch (error: Exception) {
                if (continuation.isActive) continuation.resumeWithException(error)
            } finally {
                connection.disconnect()
            }
        }
        continuation.invokeOnCancellation {
            connection.disconnect()
            worker.cancel()
        }
    }
}
