package com.matchora.app

import com.matchora.app.data.RealtimeEventDecoder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RealtimeDecoderTest {
    @Test fun chunksCanSplitUtf8AndCrlfWithoutLosingOrDuplicatingEvents() {
        val events = mutableListOf<String>()
        val decoder = RealtimeEventDecoder(events::add)
        val bytes = ": heartbeat\r\n\r\ndata: {\"type\":\"invalidate\",\"label\":\"Şükrü\"}\r\n\r\ndata: first\ndata: second\n\n".encodeToByteArray()
        for (byte in bytes) decoder.append(byteArrayOf(byte))
        assertEquals(listOf("{\"type\":\"invalidate\",\"label\":\"Şükrü\"}", "first\nsecond"), events)
    }

    @Test fun oversizedUndelimitedFramesAreRejected() {
        val decoder = RealtimeEventDecoder { }
        assertFailsWith<IllegalArgumentException> { decoder.append(ByteArray(65537) { 65 }) }
    }
}
