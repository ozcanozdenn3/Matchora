package com.matchora.app.data

import com.matchora.app.domain.EventStatus
import com.matchora.app.domain.MatchEvent
import com.matchora.app.domain.Community
import com.matchora.app.domain.RankingEntry
import com.matchora.app.domain.Sport
import com.matchora.app.domain.Tournament
import com.matchora.app.domain.Venue

interface MatchoraRepository {
    fun nearbyEvents(): List<MatchEvent>

    fun rankings(): List<RankingEntry> = emptyList()

    fun communities(): List<Community> = emptyList()

    fun tournaments(): List<Tournament> = emptyList()

    fun venues(): List<Venue> = emptyList()
}

object DemoMatchoraRepository : MatchoraRepository {
    override fun nearbyEvents(): List<MatchEvent> = listOf(
        MatchEvent(
            id = "event-kadikoy-football",
            title = "Akşam halı saha",
            sport = Sport.FOOTBALL,
            locationArea = "Kadıköy",
            startsAt = "2026-10-17T20:30:00+03:00",
            participantCount = 7,
            capacity = 10,
            status = EventStatus.OPEN,
            verifiedOnly = true
        ),
        MatchEvent(
            id = "event-atasehir-tennis",
            title = "Yeni başlayanlar için tenis",
            sport = Sport.TENNIS,
            locationArea = "Ataşehir",
            startsAt = "2026-10-18T18:00:00+03:00",
            participantCount = 3,
            capacity = 4,
            status = EventStatus.OPEN,
            verifiedOnly = false
        )
    )

    override fun rankings(): List<RankingEntry> = listOf(
        RankingEntry("Kadıköy", Sport.FOOTBALL, "demo-ozcan", 12, 1480, 12),
        RankingEntry("Kadıköy", Sport.TENNIS, "demo-ozcan", 8, 1210, 7)
    )

    override fun communities(): List<Community> = listOf(
        Community("community-kadikoy", "Kadıköy Spor Çevresi", "Kadıköy", 842, "OPEN")
    )

    override fun tournaments(): List<Tournament> = listOf(
        Tournament("tournament-autumn", "Sonbahar Kupası", Sport.FOOTBALL, "SINGLE_ELIMINATION", 12, 16, "OPEN")
    )

    override fun venues(): List<Venue> = listOf(
        Venue("venue-kadikoy", "Moda Sports Hall", "Kadıköy", listOf(Sport.FOOTBALL, Sport.BASKETBALL), true, 4.7f)
    )
}