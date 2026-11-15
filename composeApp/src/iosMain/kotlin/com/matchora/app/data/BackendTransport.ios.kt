@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.matchora.app.data

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.*
import kotlinx.coroutines.suspendCancellableCoroutine
import org.jetbrains.skia.Image
import org.jetbrains.skia.EncodedImageFormat
import platform.CoreFoundation.*
import platform.Foundation.*
import platform.Security.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import platform.posix.memcpy

actual suspend fun executeApiRequest(request: ApiHttpRequest): ApiHttpResponse = suspendCancellableCoroutine { continuation ->
    val native = NSMutableURLRequest.requestWithURL(requireNotNull(NSURL.URLWithString(request.url))).apply {
        setHTTPMethod(request.method); setTimeoutInterval(35.0)
        request.headers.forEach { (key, value) -> setValue(value, forHTTPHeaderField = key) }
        request.body?.let { bytes -> HTTPBody = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) } }
    }
    val task = NSURLSession.sharedSession.dataTaskWithRequest(native) { data, response, error ->
        if (continuation.isActive) {
            if (error != null) continuation.resumeWithException(IllegalStateException("Ağa bağlanılamadı."))
            else if (data != null && data.length <= 8u * 1024u * 1024u) {
                val bytes = ByteArray(data.length.toInt()); if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
                continuation.resume(ApiHttpResponse((response as? NSHTTPURLResponse)?.statusCode?.toInt() ?: 0, bytes))
            } else continuation.resumeWithException(IllegalStateException("Sunucu yanıtı okunamadı."))
        }
    }
    continuation.invokeOnCancellation { task.cancel() }; task.resume()
}
actual object SessionVault {
    private fun <T> query(block: (CFMutableDictionaryRef?) -> T): T {
        val dictionary = CFDictionaryCreateMutable(null, 0, null, null)
        val service = CFStringCreateWithCString(null, "com.matchora.api.session", kCFStringEncodingUTF8)
        try { CFDictionaryAddValue(dictionary, kSecClass, kSecClassGenericPassword); CFDictionaryAddValue(dictionary, kSecAttrService, service); return block(dictionary) }
        finally { CFRelease(service); CFRelease(dictionary) }
    }
    actual fun read(): String? = query { dictionary -> memScoped {
        CFDictionaryAddValue(dictionary, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(dictionary, kSecMatchLimit, kSecMatchLimitOne)
        val result = alloc<CFTypeRefVar>()
        if (SecItemCopyMatching(dictionary, result.ptr) != errSecSuccess) null else {
            val data = result.value
            try { val length = CFDataGetLength(data?.reinterpret()).toInt(); CFDataGetBytePtr(data?.reinterpret())?.readBytes(length)?.decodeToString() }
            finally { if (data != null) CFRelease(data) }
        }
    } }
    actual fun write(value: String?) = query { dictionary ->
        SecItemDelete(dictionary)
        if (value != null) {
            val bytes = value.encodeToByteArray()
            val data = bytes.usePinned { CFDataCreate(null, it.addressOf(0).reinterpret(), bytes.size.toLong()) }
            try {
                CFDictionaryAddValue(dictionary, kSecValueData, data)
                CFDictionaryAddValue(dictionary, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
                check(SecItemAdd(dictionary, null) == errSecSuccess) { "Oturum güvenli alana kaydedilemedi." }
            } finally { CFRelease(data) }
        }
    }
}
actual fun encodeUploadPhoto(bitmap: ImageBitmap): ByteArray = requireNotNull(Image.makeFromBitmap(bitmap.asSkiaBitmap()).encodeToData(EncodedImageFormat.JPEG, 85)).bytes
actual fun decodeApiPhoto(bytes: ByteArray): ImageBitmap = Image.makeFromEncoded(bytes).toComposeImageBitmap()
