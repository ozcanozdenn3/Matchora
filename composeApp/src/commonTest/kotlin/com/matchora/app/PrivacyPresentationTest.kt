package com.matchora.app

import com.matchora.app.data.BackendSnapshot
import com.matchora.app.data.apiPayload
import com.matchora.app.data.blockedPlayerEntries
import com.matchora.app.data.toApiPayload
import com.matchora.app.state.DemoData
import com.matchora.app.state.EventGeoPoint
import com.matchora.app.ui.components.eventDisplayId
import com.matchora.app.ui.components.matchesEventInviteCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonPrimitive

class PrivacyPresentationTest {
    private val skill = mapOf("sport" to "FOOTBALL", "level" to "INTERMEDIATE", "rating" to 1000)

    @Test fun avatarRemovalCannotAccidentallyReuseThePersistedUpload() {
        val draft = DemoData.me.copy(avatar = null, avatarRemovalRequested = true)
        val payload = draft.copy(name = "Değişen isim").toApiPayload(7, "old-upload")
        assertEquals(JsonNull, payload["avatarUploadId"])
        assertFalse("avatarRemovalRequested" in payload)
        assertEquals("Değişen isim", payload.getValue("name").jsonPrimitive.content)
    }

    @Test fun aNewPhotoSelectionCancelsRemovalAndRetainsTheNewUpload() {
        val draft = DemoData.me.copy(avatarRemovalRequested = true).copy(avatarRemovalRequested = false)
        val payload = draft.toApiPayload(7, "new-upload")
        assertEquals("new-upload", payload.getValue("avatarUploadId").jsonPrimitive.content)
        assertFalse("avatarRemovalRequested" in payload)
        assertFalse(BackendSnapshot(apiPayload("profile" to mapOf("name" to "Sporcu", "skills" to listOf(skill)))).profile!!.avatarRemovalRequested)
    }

    @Test fun hiddenReliabilityIsDistinctFromAnExplicitVisibleZero() {
        val snapshot = BackendSnapshot(apiPayload("players" to listOf(
            mapOf("id" to "hidden", "name" to "Gizli sporcu", "skills" to listOf(skill), "reliability" to 0, "reliabilityVisible" to false),
            mapOf("id" to "visible-zero", "name" to "Açık sporcu", "skills" to listOf(skill), "reliability" to 0, "reliabilityVisible" to true)
        )))
        assertEquals("—", snapshot.players[0].reliabilityLabel)
        assertEquals("%0", snapshot.players[1].reliabilityLabel)
        assertEquals("0 MMR", snapshot.players[1].copy(rating = 0).ratingLabel)
        assertTrue(snapshot.players.all { it.profileAvailable })
    }

    @Test fun missingPublicProfileAndDistancesDoNotTurnIntoRealZeroValues() {
        val snapshot = BackendSnapshot(apiPayload(
            "players" to listOf(mapOf("id" to "missing-profile", "name" to "Yeni sporcu")),
            "venues" to listOf(mapOf("id" to "venue", "name" to "Saha"))
        ))
        assertFalse(snapshot.players.single().profileAvailable)
        assertFalse(snapshot.players.single().reliabilityVisible)
        assertEquals("—", snapshot.players.single().reliabilityLabel)
        assertEquals("—", snapshot.players.single().ratingLabel)
        assertNull(snapshot.players.single().distanceKm)
        assertNull(snapshot.venues.single().distanceKm)
        assertEquals("%100", DemoData.mePlayer.copy(reliability = 100).reliabilityLabel)
    }

    @Test fun aBlockedAthleteCanBeListedWithoutAnyConversationOrCachedProfile() {
        val entries = blockedPlayerEntries(apiPayload(
            "blockedUserIds" to listOf("athlete-one", "missing-athlete", "athlete-one"),
            "players" to listOf(mapOf("id" to "athlete-one", "name" to "Engellenen sporcu")),
            "threads" to emptyList<String>()
        ))
        assertEquals(listOf("athlete-one", "missing-athlete"), entries.map { it.userId })
        assertEquals("Engellenen sporcu", entries[0].name)
        assertEquals("Sporcu · missing-", entries[1].name)
        assertTrue(blockedPlayerEntries(apiPayload()).isEmpty())
    }

    @Test fun meetingLocationOnlyExistsWhenTheAuthorizedResponseSuppliesIt() {
        val approximate = mapOf("latitude" to 40.99, "longitude" to 29.03)
        val precise = mapOf("latitude" to 40.991234, "longitude" to 29.034567)
        val snapshot = BackendSnapshot(apiPayload("events" to listOf(
            mapOf("id" to "public-view", "approximateLocation" to approximate),
            mapOf("id" to "member-view", "approximateLocation" to approximate, "meetingLocation" to precise)
        )))
        assertEquals(EventGeoPoint(40.99, 29.03), snapshot.events[0].approximateLocation)
        assertNull(snapshot.events[0].meetingLocation)
        assertEquals(EventGeoPoint(40.991234, 29.034567), snapshot.events[1].meetingLocation)
        assertEquals(snapshot.events[0].approximateLocation, snapshot.events[1].approximateLocation)
    }

    @Test fun malformedCoordinatesStayUnknownInsteadOfBecomingAZeroPin() {
        val snapshot = BackendSnapshot(apiPayload("events" to listOf(
            mapOf("id" to "incomplete", "meetingLocation" to mapOf("latitude" to 41.0)),
            mapOf("id" to "out-of-range", "approximateLocation" to mapOf("latitude" to 91.0, "longitude" to 29.0)),
            mapOf("id" to "malformed", "meetingLocation" to mapOf("latitude" to "unknown", "longitude" to 29.0))
        )))
        assertTrue(snapshot.events.all { it.meetingLocation == null && it.approximateLocation == null })
    }

    @Test fun eventInviteCodeMatchingRequiresExactIdOrExplicitDisplayCodeNeverPartialPrefixes() {
        val id = "df52a1b2-3c4d-4e5f-8a9b-0c1d2e842104"
        val displayCode = eventDisplayId(id)
        assertTrue(matchesEventInviteCode(id, id))
        assertTrue(matchesEventInviteCode(id, id.uppercase()))
        assertTrue(matchesEventInviteCode(id, displayCode))
        assertTrue(matchesEventInviteCode(id, displayCode.removePrefix("#").lowercase()))
        assertFalse(matchesEventInviteCode(id, ""))
        assertFalse(matchesEventInviteCode(id, "#"))
        assertFalse(matchesEventInviteCode(id, "d"))
        assertFalse(matchesEventInviteCode(id, "df52a1b2"))
        assertFalse(matchesEventInviteCode(id, displayCode.removePrefix("#ILN-")))
    }
}
