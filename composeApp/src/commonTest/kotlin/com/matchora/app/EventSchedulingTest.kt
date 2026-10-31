package com.matchora.app

import com.matchora.app.state.*
import kotlinx.datetime.*
import kotlin.test.*

class EventSchedulingTest {
    private val zone = TimeZone.of("Europe/Istanbul")
    @Test fun calendarRoundTripKeepsLeapDayAndYearBoundary() {
        listOf(LocalDate(2028, 2, 29), LocalDate(2026, 12, 31), LocalDate(2027, 1, 1)).forEach {
            assertEquals(it, pickerDate(pickerMillis(it)))
        }
    }
    @Test fun exactMinutesAndLocalTimezoneArePreserved() {
        val date = LocalDate(2026, 12, 31)
        val instant = Instant.fromEpochMilliseconds(eventStartMillis(date, 23, 59, zone))
        assertEquals(LocalDateTime(2026, 12, 31, 23, 59), instant.toLocalDateTime(zone))
        assertEquals("31.12.2026", eventDateLabel(date))
    }
    @Test fun pastAndCurrentStartCannotBePublished() {
        val date = LocalDate(2026, 10, 24)
        val now = eventStartMillis(date, 20, 30, zone)
        assertFalse(isFutureEvent(date, 20, 29, now, zone))
        assertFalse(isFutureEvent(date, 20, 30, now, zone))
        assertTrue(isFutureEvent(date, 20, 31, now, zone))
    }
    @Test fun newDatedEventsStillMatchRelativeFilters() {
        val event = DemoData.events.first().copy(startsAtEpochMillis = eventStartMillis(LocalDate(2027, 1, 1), 0, 7, zone), day = "01.01.2027")
        assertTrue(event.matchesDayFilter("Yarın", LocalDate(2026, 12, 31), zone))
        assertFalse(event.matchesDayFilter("Bugün", LocalDate(2026, 12, 31), zone))
    }
}
