package com.matchora.app.data

import androidx.compose.ui.graphics.ImageBitmap

data class ApiHttpRequest(val url: String, val method: String = "GET", val headers: Map<String, String> = emptyMap(), val body: ByteArray? = null)
data class ApiHttpResponse(val status: Int, val bytes: ByteArray) { val text: String get() = bytes.decodeToString() }
expect suspend fun executeApiRequest(request: ApiHttpRequest): ApiHttpResponse
expect object SessionVault {
    fun read(): String?
    fun write(value: String?)
}
expect fun encodeUploadPhoto(bitmap: ImageBitmap): ByteArray
expect fun decodeApiPhoto(bytes: ByteArray): ImageBitmap
