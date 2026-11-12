package com.matchora.app

import com.matchora.app.ui.components.*
import com.matchora.app.state.DemoData
import com.matchora.app.data.*
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class EventInvitationTest {
    private val id = "df52123a-9e7b-4aa0-8143-5c819cbbbd36"
    @Test fun sharedIdAndDeepLinkResolveToTheSamePermanentId() {
        assertEquals(id, eventInvitationId("  ${id.uppercase()}  "))
        assertEquals(id, eventInvitationId("matchora://event/$id?source=share#invite"))
        assertNull(eventInvitationId("../../account"))
        assertNull(eventInvitationId("https://attacker.invalid/$id"))
        assertNull(eventInvitationId("-".repeat(36)))
        assertNull(eventInvitationId("ev_ad_1"))
        assertEquals("ev_ad_1", eventInvitationId("ev_ad_1", preview = true))
    }
    @Test fun bothNativeShareSheetsUseAnExplicitIdAndRadarInstructions() {
        val event = DemoData.events.first().copy(id = id)
        val text = eventInvitationText(event)
        assertTrue(text.contains("İlan ID: $id"))
        assertTrue(text.contains("Spor radarı → ID ile bul"))
        assertTrue(text.contains("matchora://event/$id"))
    }
    @Test fun invitationLookupIsIndependentOfTheSelectedBrowseCity() = runBlocking {
        val requests = mutableListOf<String>()
        val storage = object : SessionPersistence { override fun read(): String? = null; override fun write(value: String?) {} }
        val repository = BackendRepository("https://api.example.invalid", "local", transport = { request ->
            requests += request.url
            ApiHttpResponse(200, apiPayload("id" to id, "city" to "Ankara").toString().encodeToByteArray())
        }, persistence = storage)
        repository.setBrowseCity("İstanbul")
        assertEquals("Ankara", repository.eventDetails(id).s("city"))
        assertTrue(requests.last().contains("/v1/events/$id"))
    }
}
