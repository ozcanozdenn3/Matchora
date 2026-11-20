@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.matchora.app.data

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.*
import platform.darwin.NSObject
import platform.posix.memcpy
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual suspend fun observeRealtime(url: String, headers: Map<String, String>, onEvent: (String) -> Unit) {
    suspendCancellableCoroutine<Unit> { continuation ->
        val endpoint = NSURL.URLWithString(url)
        if (endpoint == null) {
            continuation.resumeWithException(BackendFailure("REALTIME_URL", "Anlık bağlantı adresi geçersiz."))
            return@suspendCancellableCoroutine
        }
        val decoder = RealtimeEventDecoder(onEvent)
        val delegate = object : NSObject(), NSURLSessionDataDelegateProtocol {
            override fun URLSession(session: NSURLSession, dataTask: NSURLSessionDataTask, didReceiveData: NSData) {
                if (!continuation.isActive) return
                val status = (dataTask.response as? NSHTTPURLResponse)?.statusCode?.toInt() ?: 0
                if (status != 200) return
                try {
                    require(didReceiveData.length <= 1024uL * 1024uL)
                    val bytes = ByteArray(didReceiveData.length.toInt())
                    if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), didReceiveData.bytes, didReceiveData.length) }
                    decoder.append(bytes)
                } catch (error: Exception) {
                    if (continuation.isActive) continuation.resumeWithException(error)
                    session.invalidateAndCancel()
                }
            }

            override fun URLSession(session: NSURLSession, task: NSURLSessionTask, didCompleteWithError: NSError?) {
                if (continuation.isActive) {
                    val status = (task.response as? NSHTTPURLResponse)?.statusCode?.toInt() ?: 0
                    when {
                        status != 0 && status != 200 -> continuation.resumeWithException(BackendFailure("REALTIME_REJECTED", "Anlık bağlantı açılamadı.", status))
                        didCompleteWithError != null -> continuation.resumeWithException(BackendFailure("REALTIME_OFFLINE", "Anlık bağlantı kesildi."))
                        else -> continuation.resume(Unit)
                    }
                }
                session.finishTasksAndInvalidate()
            }
        }
        val configuration = NSURLSessionConfiguration.ephemeralSessionConfiguration.apply {
            timeoutIntervalForRequest = 40.0
            timeoutIntervalForResource = 3600.0
        }
        val session = NSURLSession.sessionWithConfiguration(configuration, delegate, NSOperationQueue.mainQueue)
        val request = NSMutableURLRequest.requestWithURL(endpoint).apply {
            setHTTPMethod("GET")
            setValue("text/event-stream", forHTTPHeaderField = "Accept")
            headers.forEach { (key, value) -> setValue(value, forHTTPHeaderField = key) }
        }
        val task = session.dataTaskWithRequest(request)
        continuation.invokeOnCancellation { task.cancel(); session.invalidateAndCancel() }
        task.resume()
    }
}
