package com.matchora.app.data

/** Suspends for the lifetime of one authenticated SSE connection. Cancellation closes it. */
expect suspend fun observeRealtime(url: String, headers: Map<String, String>, onEvent: (String) -> Unit)

/** Streaming UTF-8 decoder keeps partial lines across arbitrary network chunks. */
internal class RealtimeEventDecoder(private val onEvent: (String) -> Unit) {
    private var pending = ByteArray(0)
    private val data = mutableListOf<String>()
    private var eventSize = 0

    fun append(bytes: ByteArray) {
        val combined = pending + bytes
        var start = 0
        for (index in combined.indices) {
            if (combined[index] != 10.toByte()) continue
            require(index - start <= 65536) { "Anlık veri satırı çok büyük." }
            line(combined.copyOfRange(start, index).decodeToString().removeSuffix("\r"))
            start = index + 1
        }
        pending = combined.copyOfRange(start, combined.size)
        require(pending.size <= 65536) { "Anlık veri satırı çok büyük." }
    }

    private fun line(value: String) {
        if (value.isEmpty()) {
            if (data.isNotEmpty()) onEvent(data.joinToString("\n"))
            data.clear()
            eventSize = 0
        } else if (value.startsWith("data:")) {
            val content = value.substring(5).removePrefix(" ")
            eventSize += content.length
            require(eventSize <= 65536) { "Anlık veri paketi çok büyük." }
            data += content
        }
    }
}
