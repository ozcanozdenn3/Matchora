package com.matchora.app

import com.matchora.app.domain.EventStatus
import com.matchora.app.domain.Sport
import com.matchora.app.state.*
import kotlin.test.*

class DemoCatalogTest {
    @Test fun everySportHasDiscoverableDemoContent() {
        Sport.entries.forEach { sport ->
            assertTrue(sport.label.isNotBlank())
            assertTrue(sport.emoji.isNotBlank())
            assertTrue(DemoData.events.any { it.sport == sport && it.status in setOf(EventStatus.PUBLISHED, EventStatus.FILLING, EventStatus.FULL) }, sport.name)
        }
    }

    @Test fun fixturesHaveUniqueIdsAndConsistentRosters() {
        val events = DemoData.events
        assertEquals(events.size, events.map { it.id }.toSet().size)
        events.forEach { event ->
            assertTrue(event.capacity > 0, event.id)
            assertTrue(event.participantIds.size <= event.capacity, event.id)
            assertEquals(event.participantIds.distinct(), event.participantIds, event.id)
            assertTrue(event.fillRatio in 0f..1f, event.id)
            if (event.status == EventStatus.FULL) assertEquals(0, event.freeSpots, event.id)
        }
    }

    @Test fun groupAndTeamFormatsProduceCorrectCapacities() {
        assertEquals(6, capacityForFormat(defaultFormatForSport(Sport.HIKING)))
        assertEquals(4, capacityForFormat(defaultFormatForSport(Sport.PADEL)))
        assertEquals(14, capacityForFormat(defaultFormatForSport(Sport.HANDBALL)))
        assertEquals(30, capacityForFormat("15v15"))
        assertEquals(20, capacityForFormat("Grup · 20 kişi"))
        assertEquals(6, capacityForFormat("invalid"))
        Sport.entries.forEach { sport ->
            assertTrue(defaultFormatForSport(sport) in formatsForSport(sport))
            formatsForSport(sport).forEach { assertTrue(capacityForFormat(it) in 2..40) }
        }
    }

    @Test fun splitBookingDoesNotDropRemainderOrDivideByZero() {
        assertEquals(250, bookingShare(1000, 4))
        assertEquals(251, bookingShare(1001, 4))
        assertEquals(1000, bookingShare(1000, 0))
        assertEquals(0, bookingShare(0, 4))
    }
    @Test fun regionalRankingsAreStableAndSupportEveryCity() {
        assertEquals(81, turkeyCities.distinct().size)
        turkeyCities.forEach { city ->
            val rows = DemoData.leaderboard(Sport.FOOTBALL, "Şehir", city)
            assertEquals(rows, DemoData.leaderboard(Sport.FOOTBALL, "Şehir", city))
            assertEquals(rows.size, rows.map { it.playerId }.distinct().size)
            assertEquals(1, rows.count { it.isMe })
        }
        assertNotEquals(DemoData.leaderboard(Sport.FOOTBALL, "Şehir", "Ankara"), DemoData.leaderboard(Sport.FOOTBALL, "Şehir", "İzmir"))
    }
}
