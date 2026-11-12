package com.matchora.app.data

expect suspend fun authenticateInBrowser(url: String): String

/** Only auth callbacks reach the browser flow. Tokens are never accepted from a deep link. */
object BrowserAuthCallback {
    var handler: ((String) -> Unit)? = null
    fun deliver(url: String): Boolean {
        if (!url.startsWith("matchora://auth/callback?")) return false
        handler?.invoke(url)
        return true
    }
}

fun callbackParameters(url: String): Map<String, String> = url.substringAfter('?', "").substringBefore('#')
    .split('&').filter { '=' in it }.associate { it.substringBefore('=') to decodeQueryValue(it.substringAfter('=')) }

private fun decodeQueryValue(value: String): String {
    val bytes = mutableListOf<Byte>(); var index = 0
    while (index < value.length) {
        if (value[index] == '%' && index + 2 < value.length) {
            val n = value.substring(index + 1, index + 3).toIntOrNull(16)
            if (n != null) { bytes += n.toByte(); index += 3; continue }
        }
        bytes.addAll((if (value[index] == '+') " " else value[index].toString()).encodeToByteArray().toList()); index++
    }
    return bytes.toByteArray().decodeToString()
}
