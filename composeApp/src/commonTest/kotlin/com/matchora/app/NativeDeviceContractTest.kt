package com.matchora.app

import com.matchora.app.state.DemoData
import com.matchora.app.state.EventGeoPoint
import com.matchora.app.ui.components.*
import kotlin.test.*

class NativeDeviceContractTest {
    @Test fun invalidCoordinatesNeverReachNativeMapOrDirections() {
        assertTrue(validMapCoordinate(40.98, 29.04))
        assertFalse(validMapCoordinate(Double.NaN, 29.04))
        assertFalse(validMapCoordinate(40.98, Double.POSITIVE_INFINITY))
        assertFalse(validMapCoordinate(90.01, 0.0))
        assertFalse(validMapCoordinate(0.0, -180.01))
    }

    @Test fun staleFutureOrInvalidAccuracyFixIsNotCurrentGps() {
        assertTrue(freshLocationFix(40.98, 29.04, 0.0, 80.0))
        assertTrue(freshLocationFix(40.98, 29.04, 119999.0, 2000.0))
        assertFalse(freshLocationFix(40.98, 29.04, 120001.0, 1.0))
        assertFalse(freshLocationFix(40.98, 29.04, -5001.0, 1.0))
        assertFalse(freshLocationFix(40.98, 29.04, 0.0, -1.0))
        assertFalse(freshLocationFix(40.98, 29.04, 0.0, Double.NaN))
        assertFalse(freshLocationFix(Double.NaN, 29.04, 0.0, 1.0))
    }

    @Test fun nativeZoomUsesGroundSpanAndLatitude() {
        val base = mapZoomForMeters(0.0, 4200.0, 256.0)
        assertEquals(base - 1.0, mapZoomForMeters(0.0, 8400.0, 256.0), 0.000001)
        assertEquals(base - 1.0, mapZoomForMeters(60.0, 4200.0, 256.0), 0.000001)
        assertTrue(mapZoomForMeters(Double.NaN, Double.NaN, 0.0).isFinite())
    }

    @Test fun geocoderCitySpellingMatchesTheTurkishCatalogue() {
        assertEquals("İstanbul", canonicalRegionCity(" Istanbul "))
        assertEquals("Şanlıurfa", canonicalRegionCity("Sanliurfa"))
        assertEquals("İzmir", canonicalRegionCity("IZMIR"))
        assertNull(canonicalRegionCity(" "))
    }

    @Test fun onlineMeetingsHaveNoPhysicalDirectionsWhileLanAndPhysicalMeetingsDo() {
        val physical = DemoData.events.first().copy(area = "Kadıköy", venue = "Arena", gamePlatform = null,
            approximateLocation = null, meetingLocation = null)
        assertFalse(isOnlineMeeting(physical))
        assertTrue(isOnlineMeeting(physical.copy(area = "Online")))
        assertTrue(isOnlineMeeting(physical.copy(gamePlatform = "PC", gameServer = "TR")))
        assertFalse(isOnlineMeeting(physical.copy(gamePlatform = "PC", meetingLocation = EventGeoPoint(40.98, 29.04))))
        val exact = EventGeoPoint(40.981234, 29.041234)
        val mapped = physical.copy(approximateLocation = EventGeoPoint(40.98, 29.04), meetingLocation = exact)
        assertEquals(exact, eventDirectionsPoint(mapped))
        assertNull(eventDirectionsPoint(mapped.copy(meetingLocation = null)))
        assertNull(eventDirectionsPoint(mapped.copy(meetingLocation = EventGeoPoint(Double.NaN, 29.04))))
        assertNull(eventDirectionsPoint(mapped.copy(area = "Online")))
    }

    @Test fun onlineCalendarKeepsLobbyAndVoiceInformation() {
        val online = DemoData.events.first().copy(area = "Online", gamePlatform = "PC", gameServer = "TR-1", voiceChannel = "Takım odası")
        val notes = eventCalendarNotes(online)
        assertTrue(notes.contains("matchora://event/${online.id}"))
        assertTrue(notes.contains("Platform: PC"))
        assertTrue(notes.contains("Sunucu / lobi: TR-1"))
        assertTrue(notes.contains("Ses kanalı: Takım odası"))

        val strippedNotes = calendarNotesForViewer(online, includeInviteLink = false)
        assertFalse(strippedNotes.contains("matchora://"))
        assertTrue(strippedNotes.contains("Platform: PC"))
        assertTrue(strippedNotes.contains("Ses kanalı: Takım odası"))
    }

    @Test fun approximateDistanceStaysFiniteAtTheSamePointAndAntipode() {
        val point = DeviceLocation(0.0, 0.0, null, null)
        assertEquals(0f, approximateDistanceKm(point, 0.0, 0.0))
        assertEquals(20015.086f, approximateDistanceKm(point, 0.0, 180.0), 1f)
    }
}
