package com.matchora.app

import com.matchora.app.data.*
import com.matchora.app.state.*
import kotlin.test.*
import kotlinx.coroutines.runBlocking

class CompetitionWorkflowClientTest {
    @Test fun fixtureAndRosterTargetsSurviveWithoutTheirTeamsBeingInTheFirstCatalogPage() {
        val data = BackendSnapshot(apiPayload("userId" to "own", "tournaments" to listOf(mapOf(
            "id" to "cup", "sport" to "FOOTBALL", "status" to "completed", "rosterSize" to 2, "championTeamId" to "away",
            "fixtures" to listOf(mapOf("round" to 1, "event_id" to "match-id", "home_team" to "home", "away_team" to "away", "homeName" to "Başkent", "awayName" to "Konuk", "startsAtEpochMillis" to 1000000, "venue" to "Spor salonu", "event_status" to "VERIFIED", "homeScore" to 1, "awayScore" to 2)),
            "registrations" to listOf(mapOf("teamId" to "away", "name" to "Konuk", "captainId" to "own", "rosterIds" to listOf("own", "other"), "status" to "joined"))
        ))))
        val cup = data.tournaments.single(); val fixture = cup.bracket.single()
        assertEquals("Başkent", fixture.teamA); assertEquals("Konuk", fixture.teamB)
        assertEquals("match-id", fixture.eventId); assertEquals(1000000L, fixture.startsAtEpochMillis)
        assertEquals("Spor salonu", fixture.venue); assertEquals("away", cup.championTeamId)
        assertEquals(2, cup.rosterSize); assertEquals(DemoData.ME, cup.registrations.single().captainId)
        assertEquals(listOf(DemoData.ME, "other"), cup.registrations.single().rosterIds)
    }
    @Test fun teamCityAndRemovedRegistrationStateAreDecodedForTheCorrectControls() {
        val data = BackendSnapshot(apiPayload("teams" to listOf(mapOf("id" to "club", "name" to "Kulüp", "city" to "Ankara", "area" to "Çankaya")),
            "tournaments" to listOf(mapOf("id" to "cup", "registrations" to listOf(mapOf("teamId" to "club", "status" to "removed"))))))
        assertEquals("Ankara", data.teams.single().city)
        assertEquals("removed", data.tournaments.single().registrations.single().status)
    }
    @Test fun aNotificationTargetRevalidatesOutsideTheCatalogPageAndDropsWhenArchived() = runBlocking {
        val id = "467a6cdf-3c04-4f5d-9af0-7ad95e33591a"
        var archived = false
        val storage = object : SessionPersistence { override fun read(): String? = null; override fun write(value: String?) {} }
        val repository = BackendRepository("https://api.example.invalid", "local", transport = { request ->
            if (request.url.endsWith("/catalog/teams/$id")) {
                if (archived) ApiHttpResponse(404, apiPayload("error" to mapOf("code" to "NOT_FOUND", "message" to "Kapalı")).toString().encodeToByteArray())
                else ApiHttpResponse(200, apiPayload("item" to mapOf("id" to id)).toString().encodeToByteArray())
            } else ApiHttpResponse(200, apiPayload("teams" to emptyList<String>()).toString().encodeToByteArray())
        }, persistence = storage)
        repository.reload(); repository.setActiveCatalogs(setOf("teams")); repository.includeCatalogItem("teams", id)
        assertEquals(id, repository.snapshots.value!!.a("teams").single().s("id"))
        repository.reload(); assertEquals(id, repository.snapshots.value!!.a("teams").single().s("id"))
        archived = true; repository.reload(); assertTrue(repository.snapshots.value!!.a("teams").isEmpty())
    }
}
