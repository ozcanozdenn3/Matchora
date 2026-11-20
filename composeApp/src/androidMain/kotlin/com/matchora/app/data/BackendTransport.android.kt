package com.matchora.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object AndroidBackendRuntime {
    lateinit var context: Context
    var currentActivity: androidx.activity.ComponentActivity? = null
}
actual suspend fun executeApiRequest(request: ApiHttpRequest): ApiHttpResponse = withContext(Dispatchers.IO) {
    val connection = URL(request.url).openConnection() as HttpURLConnection
    try {
        connection.requestMethod = request.method
        connection.connectTimeout = 15000; connection.readTimeout = 35000; connection.instanceFollowRedirects = false
        request.headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
        request.body?.let { bytes -> connection.doOutput = true; connection.setFixedLengthStreamingMode(bytes.size); connection.outputStream.use { output ->
            var offset = 0
            while (offset < bytes.size) {
                val length = minOf(65536,bytes.size-offset)
                output.write(bytes,offset,length)
                offset += length
                request.onUploadProgress?.invoke(offset.toLong(),bytes.size.toLong())
            }
        } }
        val status = connection.responseCode
        val bytes = (if (status in 200..299) connection.inputStream else connection.errorStream)?.use { stream ->
            val output = ByteArrayOutputStream(); val buffer = ByteArray(8192); var total = 0
            while (true) { val read = stream.read(buffer); if (read < 0) break; total += read; check(total <= 8 * 1024 * 1024); output.write(buffer, 0, read) }; output.toByteArray()
        } ?: ByteArray(0)
        ApiHttpResponse(status, bytes)
    } finally { connection.disconnect() }
}
actual object SessionVault {
    private const val ALIAS = "matchora_session_key"
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    private fun preferences() = AndroidBackendRuntime.context.getSharedPreferences("matchora_secure_session", Context.MODE_PRIVATE)
    actual fun read(): String? = runCatching {
        val stored = preferences().getString("session", null) ?: return null
        val bytes = Base64.decode(stored, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12))) }
        cipher.doFinal(bytes.copyOfRange(12, bytes.size)).decodeToString()
    }.getOrNull()
    actual fun write(value: String?) {
        if (value == null) { check(preferences().edit().remove("session").commit()); return }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encrypted = cipher.iv + cipher.doFinal(value.encodeToByteArray())
        check(preferences().edit().putString("session", Base64.encodeToString(encrypted, Base64.NO_WRAP)).commit())
    }
}
actual fun encodeUploadPhoto(bitmap: ImageBitmap): ByteArray {
    val source = bitmap.asAndroidBitmap(); val scale = minOf(1f, 2048f / maxOf(source.width, source.height))
    val resized = if (scale < 1f) Bitmap.createScaledBitmap(source, (source.width * scale).toInt(), (source.height * scale).toInt(), true) else source
    return ByteArrayOutputStream().use { output -> resized.compress(Bitmap.CompressFormat.JPEG, 85, output); output.toByteArray() }
}
actual fun decodeApiPhoto(bytes: ByteArray): ImageBitmap = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size)).asImageBitmap()
