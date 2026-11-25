package com.matchora.app

import com.matchora.app.data.*
import com.matchora.app.state.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import kotlin.test.*

class BackendClientTest {
    @Test fun resultSidesKeepIdentityForOpponentConfirmationAndLegacyEvents() {
        val event = mapOf("id" to "event", "homeParticipantIds" to listOf("user-test"), "awayParticipantIds" to listOf("opponent"))
        val result = mapOf("event_id" to "event", "submitted_by" to "user-test", "status" to "pending", "dispute_until" to 1000,
            "payload" to apiPayload("homeScore" to 2, "awayScore" to 1, "sides" to mapOf("home" to listOf("user-test"), "away" to listOf("opponent"))).toString())
        val mapped = BackendSnapshot(apiPayload("userId" to "user-test", "events" to listOf(event), "results" to listOf(result))).results.getValue("event")
        assertEquals(listOf(DemoData.ME), mapped.homeParticipantIds)
        assertEquals(listOf("opponent"), mapped.awayParticipantIds)
        assertEquals(DemoData.ME, mapped.submittedBy)
        val legacy = result + ("payload" to apiPayload("homeScore" to 2, "awayScore" to 1).toString())
        val restored = BackendSnapshot(apiPayload("userId" to "user-test", "events" to listOf(event), "results" to listOf(legacy))).results.getValue("event")
        assertEquals(mapped.homeParticipantIds, restored.homeParticipantIds)
        assertEquals(mapped.awayParticipantIds, restored.awayParticipantIds)
    }

    private class MemorySession : SessionPersistence { var value: String? = null; override fun read() = value; override fun write(value: String?) { this.value = value } }
    private fun response(status: Int, data: JsonObject) = ApiHttpResponse(status, data.toString().encodeToByteArray())
    private val session = apiPayload("accessToken" to "access-test", "refreshToken" to "refresh-test", "userId" to "user-test", "expiresAt" to 9999999999)

    @Test fun compactEventResponseUpdatesMembershipAndPreservesUnrelatedLists() = runBlocking {
        val initial = apiPayload("userId" to "user-test", "teams" to listOf(mapOf("id" to "team")),
            "events" to listOf(mapOf("id" to "older")), "joinedEventIds" to listOf("older"))
        val repository = BackendRepository("https://api.example.invalid", "local", transport = { request ->
            when {
                request.url.endsWith("/auth/dev") -> response(200, session)
                request.url.endsWith("/bootstrap") -> response(200, initial)
                else -> {
                    assertEquals("return=changes", request.headers["Prefer"])
                    response(200, apiPayload("result" to mapOf("id" to "new"),
                        "changes" to mapOf("userId" to "user-test", "event" to mapOf("id" to "new"),
                            "fields" to mapOf("wallet" to mapOf("balance" to 10)))))
                }
            }
        }, persistence = MemorySession())
        repository.developmentLogin()
        repository.command("event.create",apiPayload(),"create-key")
        val updated = requireNotNull(repository.snapshots.value)
        assertEquals(setOf("older","new"), updated.ss("joinedEventIds").toSet())
        assertEquals(setOf("older","new"), updated.a("events").map { it.s("id") }.toSet())
        assertEquals(initial["teams"], updated["teams"])
        assertEquals(10, updated.o("wallet").i("balance"))
    }
    @Test fun jsonPayloadEscapesUserTextWithoutChangingItsMeaning() {
        val input = "Satır\n\"alıntı\" \\ Türkçe: ıüğ"
        assertEquals(input, Json.parseToJsonElement(apiPayload("text" to input).toString()).jsonObject.getValue("text").jsonPrimitive.content)
    }
    @Test fun retryKeepsTheCommandKeyAndDoesNotInventSuccessfulState() = runBlocking {
        val requests = mutableListOf<ApiHttpRequest>()
        var fail = true
        val repository = BackendRepository("https://api.example.invalid", "local", transport = { request ->
            requests += request
            when {
                request.url.endsWith("/auth/dev") -> response(200, session)
                request.url.endsWith("/bootstrap") -> response(200, apiPayload("userId" to "user-test", "wallet" to mapOf("balance" to 20)))
                fail -> throw IllegalStateException("offline")
                else -> response(200, apiPayload("result" to mapOf("joined" to true), "snapshot" to mapOf("userId" to "user-test", "wallet" to mapOf("balance" to 10))))
            }
        }, persistence = MemorySession())
        repository.developmentLogin()
        val payload = apiPayload("eventId" to "event-1")
        assertFailsWith<BackendFailure> { repository.command("event.join", payload, "stable-request-1") }
        assertEquals(20, BackendSnapshot(requireNotNull(repository.snapshots.value)).balance)
        fail = false
        repository.command("event.join", payload, "stable-request-1")
        val commands = requests.filter { it.url.contains("/commands/") }
        assertEquals(commands[0].headers["Idempotency-Key"], commands[1].headers["Idempotency-Key"])
        assertContentEquals(commands[0].body, commands[1].body)
        assertEquals(10, BackendSnapshot(requireNotNull(repository.snapshots.value)).balance)
    }
    @Test fun serverValidationErrorsArePreservedAndDoNotFallBackToDemoData() = runBlocking {
        val repository = BackendRepository("https://api.example.invalid", "local", transport = { response(409, apiPayload("error" to mapOf("code" to "EVENT_FULL", "message" to "Kontenjan doldu."))) }, persistence = MemorySession())
        val failure = assertFailsWith<BackendFailure> { repository.command("event.join", apiPayload(), "stable-request-2") }
        assertTrue(failure.code == "EVENT_FULL")
        assertEquals("Kontenjan doldu.", failure.message)
        assertNull(repository.snapshots.value)
    }
    @Test fun expiredSessionRefreshesAndPersistsTheRotatedTokenBeforeRetry() = runBlocking {
        val storage = MemorySession().apply { value = session.toString() }
        val seen = mutableListOf<String?>()
        val repository = BackendRepository("https://api.example.invalid", "production", transport = { request ->
            if (request.url.endsWith("/config")) { assertNull(request.headers["Authorization"]); response(200, apiPayload()) }
            else if (request.url.endsWith("/refresh")) response(200, apiPayload("accessToken" to "rotated-access", "refreshToken" to "rotated-refresh", "userId" to "user-test"))
            else { seen += request.headers["Authorization"]; if (seen.size == 1) response(401, apiPayload("error" to mapOf("code" to "SESSION_EXPIRED", "message" to "Süre doldu"))) else response(200, apiPayload("userId" to "user-test")) }
        }, persistence = storage)
        repository.reload()
        assertEquals(listOf<String?>("Bearer access-test", "Bearer rotated-access"), seen.toList())
        assertTrue(requireNotNull(storage.value).contains("rotated-refresh"))
    }
    @Test fun currentUserAliasIsMappedOnlyToItsOwnIdentity() {
        val data = BackendSnapshot(apiPayload("userId" to "real-user", "joinedEventIds" to listOf("event-1"), "wallet" to mapOf("balance" to 17, "locked" to 3), "players" to listOf(mapOf("id" to "real-user", "name" to "Ben", "skills" to emptyList<String>()), mapOf("id" to "other-user", "name" to "Diğer", "skills" to emptyList<String>()))))
        assertEquals(DemoData.ME, data.players[0].id)
        assertEquals("other-user", data.players[1].id)
        assertEquals(17, data.balance)
        assertEquals(3, data.locked)
        assertTrue(data.events.isEmpty())
    }
    @Test fun revokedRefreshClearsPrivateSnapshotAndPersistentSession() = runBlocking {
        val storage = MemorySession().apply { value = session.toString() }
        var revoked = false
        val repository = BackendRepository("https://api.example.invalid", "production", transport = { request ->
            when {
                request.url.endsWith("/config") -> response(200, apiPayload())
                revoked -> response(401, apiPayload("error" to mapOf("code" to "UNAUTHENTICATED", "message" to "Tekrar giriş gerekiyor.")))
                else -> response(200, apiPayload("userId" to "user-test", "ledger" to listOf(mapOf("id" to "private-record"))))
            }
        }, persistence = storage)
        repository.start()
        assertNotNull(repository.snapshots.value)
        revoked = true
        assertFailsWith<BackendFailure> { repository.reload() }
        assertNull(storage.value)
        assertNull(repository.snapshots.value)
        assertFalse(repository.authenticated)
    }
    @Test fun logoutClearsLocalSessionEvenWhenTheServerIsOffline() = runBlocking {
        val storage = MemorySession().apply { value = session.toString() }
        val repository = BackendRepository("https://api.example.invalid", "production", transport = { throw IllegalStateException("offline") }, persistence = storage)
        assertFailsWith<BackendFailure> { repository.logout() }
        assertFalse(repository.authenticated)
        assertNull(storage.value)
    }
    @Test fun olderMessagesSurvivePollingAndNewerServerValuesWin() = runBlocking {
        val storage = MemorySession().apply { value = session.toString() }
        val threadId = "12345678-1234-1234-1234-123456789012"
        var changed = false
        var blocked = false
        fun message(id: String, order: Int, text: String) = mapOf("id" to id, "createdAt" to order, "text" to text)
        val repository = BackendRepository("https://api.example.invalid", "production", transport = { request ->
            when {
                request.url.endsWith("/config") -> response(200, apiPayload())
                request.url.contains("/messages?before=") -> response(200, apiPayload("items" to listOf(message("m1", 1, "İlk"), message("m2", 2, "İkinci")), "nextCursor" to null))
                else -> response(200, apiPayload("userId" to "user-test", "threads" to listOf(mapOf("id" to threadId, "locked" to blocked, "messages" to if (blocked) emptyList() else listOf(message("m3", 3, if (changed) "Güncel" else "Eski"), message("m4", 4, "Son")), "nextMessageCursor" to "cursor-test"))))
            }
        }, persistence = storage)
        repository.start(); assertTrue(repository.canLoadOlderMessages(threadId))
        repository.loadOlderMessages(threadId); assertFalse(repository.canLoadOlderMessages(threadId))
        changed = true; repository.reload()
        val messages = repository.snapshots.value!!.a("threads").single().a("messages")
        assertEquals(listOf("m1", "m2", "m3", "m4"), messages.map { it.s("id") })
        assertEquals("Güncel", messages[2].s("text"))
        blocked = true; repository.reload()
        assertTrue(repository.snapshots.value!!.a("threads").single().a("messages").isEmpty())
    }
    @Test fun livePlayerWithoutChemistryEvidenceHasNoInventedScore() {
        val snapshot = BackendSnapshot(apiPayload("players" to listOf(mapOf("id" to "athlete", "name" to "Sporcu", "skills" to emptyList<String>()))))
        val player = snapshot.players.single()
        assertNull(player.chemistry)
        assertFalse(player.verified)
        assertEquals(0, player.playedTogether)
        assertEquals(0, player.verifiedMatches)
    }
    @Test fun discoveryPagesAreRevalidatedOnRefreshAndRemovedWhenMembershipIsRevoked() = runBlocking {
        var revoked = false
        var pageReads = 0
        val repository = BackendRepository("https://api.example.invalid", "production", transport = { request ->
            when {
                request.url.endsWith("/config") -> response(200, apiPayload())
                request.url.contains("/events?after=") -> { pageReads++; response(200, apiPayload("items" to if (revoked) emptyList() else listOf(mapOf("id" to "private", "startsAtEpochMillis" to 2)), "nextCursor" to null)) }
                else -> response(200, apiPayload("events" to listOf(mapOf("id" to "public", "startsAtEpochMillis" to 1)), "nextEventCursor" to "cursor-safe", "wallet" to mapOf("balance" to 21)))
            }
        }, persistence = MemorySession())
        repository.start(); repository.loadMoreEvents()
        assertEquals(listOf("public", "private"), repository.snapshots.value!!.a("events").map { it.s("id") })
        repository.reload()
        assertEquals(2, pageReads)
        assertEquals(2, repository.snapshots.value!!.a("events").size)
        revoked = true; repository.reload()
        assertEquals(listOf("public"), repository.snapshots.value!!.a("events").map { it.s("id") })
        assertEquals(21, repository.snapshots.value!!.o("wallet").i("balance"))
    }
    @Test fun discoveryCityIsEncodedScopedToListsAndRetryableAfterAnOfflineChange() = runBlocking {
        val seen = mutableListOf<String>()
        var fail = true
        val repository = BackendRepository("https://api.example.invalid", "production", transport = { request ->
            seen += request.url
            if (request.url.contains("bootstrap?") && fail) throw IllegalStateException("offline")
            if (request.url.contains("/commands/")) response(200, apiPayload("result" to emptyMap<String, String>(), "snapshot" to mapOf("events" to emptyList<String>())))
            else response(200, apiPayload("events" to emptyList<String>()))
        }, persistence = MemorySession())
        assertFailsWith<BackendFailure> { repository.setBrowseCity("İstanbul") }
        fail = false; repository.setBrowseCity("İstanbul")
        assertEquals(2, seen.count { it.endsWith("/bootstrap?city=%C4%B0stanbul") })
        repository.command("event.join", apiPayload(), "city-command-1")
        assertTrue(seen.last().endsWith("/commands/event.join?city=%C4%B0stanbul"))
        repository.playerDetails("12345678-1234-1234-1234-123456789012", "FOOTBALL")
        assertFalse(seen.last().contains("city="))
    }
    @Test fun leavingDiscoveryReleasesExtraPagesWithoutRestoringOldWalletState() = runBlocking {
        var balance = 21
        val repository = BackendRepository("https://api.example.invalid", "production", transport = { request ->
            when {
                request.url.endsWith("/config") -> response(200, apiPayload())
                request.url.contains("/events?after=") -> response(200, apiPayload("items" to listOf(mapOf("id" to "next"))))
                request.url.contains("/commands/") -> { balance = 10; response(200, apiPayload("result" to emptyMap<String, String>(), "snapshot" to mapOf("events" to listOf(mapOf("id" to "first")), "nextEventCursor" to "cursor", "wallet" to mapOf("balance" to balance)))) }
                else -> response(200, apiPayload("events" to listOf(mapOf("id" to "first")), "nextEventCursor" to "cursor", "wallet" to mapOf("balance" to balance)))
            }
        }, persistence = MemorySession())
        repository.start(); repository.loadMoreEvents(); repository.command("event.join", apiPayload(), "fresh-wallet")
        repository.setDiscoveryActive(false)
        assertEquals(listOf("first"), repository.snapshots.value!!.a("events").map { it.s("id") })
        assertEquals(10, repository.snapshots.value!!.o("wallet").i("balance"))
    }
    @Test fun screenMetadataSurvivesTheServerToUiBoundary() {
        val snapshot = BackendSnapshot(apiPayload(
            "memories" to listOf(mapOf("id" to "memory", "authorName" to "Sporcu", "sceneTheme" to "SUNSET_TENNIS", "matchScoreTag" to "6-3 / 6-2")),
            "teams" to listOf(mapOf("id" to "team", "colorHex" to "#FF123456")),
            "tournaments" to listOf(mapOf("id" to "cup", "organizerName" to "Kulüp", "customSportName" to "Özel spor", "gamePlatform" to "PC", "serverRegion" to "TR lobi")),
            "communities" to listOf(mapOf("id" to "network", "type" to "TRAVEL", "upcomingEvents" to 3)),
            "supportRequests" to listOf(mapOf("id" to "request", "status" to "resolved", "response" to "Destek yanıtı")),
            "reports" to listOf(mapOf("id" to "report", "status" to "dismissed"))
        ))
        assertEquals(com.matchora.app.state.MemorySceneTheme.SUNSET_TENNIS, snapshot.memories.single().sceneTheme)
        assertEquals("6-3 / 6-2", snapshot.memories.single().matchScoreTag)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF123456), snapshot.teams.single().color)
        assertEquals("Kulüp", snapshot.tournaments.single().organizer)
        assertEquals("Özel spor", snapshot.tournaments.single().customSportName)
        assertEquals("PC", snapshot.tournaments.single().gamePlatform)
        assertEquals("TR lobi", snapshot.tournaments.single().serverRegion)
        assertEquals(3, snapshot.communities.single().upcomingEvents)
        assertEquals(com.matchora.app.state.CommunityType.TRAVEL, snapshot.communities.single().type)
        assertEquals("resolved", snapshot.support.single().status)
        assertEquals("Destek yanıtı", snapshot.support.single().response)
        assertEquals("dismissed", snapshot.reports.single().status)
    }
    @Test fun playerDetailAcceptsEverySportCodeAndRejectsQueryInjection() = runBlocking {
        val seen = mutableListOf<String>()
        val repository = BackendRepository("https://api.example.invalid", "production", transport = { request ->
            seen += request.url
            response(200, apiPayload("recentForm" to emptyList<String>()))
        }, persistence = MemorySession())
        val player = "12345678-1234-1234-1234-123456789012"
        repository.playerDetails(player, "ESPORTS_CS2")
        assertEquals("https://api.example.invalid/v1/players/$player?sport=ESPORTS_CS2", seen.single())
        assertFailsWith<IllegalArgumentException> { repository.playerDetails(player, "FOOTBALL&other=true") }
        assertEquals(1, seen.size)
    }
    @Test fun physicalVenueCurrencyPreservesKurusAndRefundedSlotsAreReleased() {
        val snapshot = BackendSnapshot(apiPayload(
            "venues" to listOf(mapOf("id" to "venue", "slots" to listOf(mapOf("id" to "slot", "priceTry" to 1001, "priceMinor" to 100001, "available" to true)))),
            "venuePayments" to listOf(mapOf("id" to "booking", "amountTry" to 1001, "amountMinor" to 100001, "bookingKey" to "venue:slot", "bookingStatus" to "cancelled", "status" to "REFUNDED")),
            "progression" to mapOf("missions" to listOf(mapOf("id" to "goal", "claimed" to true, "rewardTokens" to 15)))
        ))
        assertEquals("1000,01", snapshot.venues.single().slots.single().priceLabel)
        assertEquals("1000,01", snapshot.venues.single().fromPriceLabel)
        assertEquals("1000,01", snapshot.venuePayments.single().amountLabel)
        assertTrue(snapshot.bookedSlotIds.isEmpty())
        assertTrue(snapshot.missions.single().claimed)
        assertEquals("250,01", formatTryMinor(splitTryMinor(snapshot.venuePayments.single().amountMinor, 4)))
    }
    @Test fun authCallbackDecodingPreservesCodeAndPrivateStateWithoutAcceptingTokens() {
        val callback = callbackParameters("matchora://auth/callback?flow=flow-123&code=a%2Bb%2Fc%3D&error_description=%C4%B0ptal+edildi")
        assertEquals("flow-123", callback["flow"])
        assertEquals("a+b/c=", callback["code"])
        assertEquals("İptal edildi", callback["error_description"])
    }
    @Test fun adminSearchAndModerationReasonsSurviveClientBoundary() = runBlocking {
        val snapshot = BackendSnapshot(apiPayload(
            "memories" to listOf(mapOf("id" to "m1", "authorName" to "Ali", "status" to "rejected", "moderationReason" to "Yüz bulanık görünüyor")),
            "reports" to listOf(mapOf("id" to "r1", "target" to "t1", "reason" to "Uygunsuz", "details" to "", "status" to "resolved", "resolutionReason" to "İçerik yayından kaldırıldı")),
            "results" to listOf(mapOf("event_id" to "e1", "status" to "rejected", "payload" to mapOf("homeScore" to 1, "awayScore" to 2)))
        ))
        assertEquals("Yüz bulanık görünüyor", snapshot.memories.single().moderationReason)
        assertEquals("İçerik yayından kaldırıldı", snapshot.reports.single().resolutionReason)
        assertEquals("rejected", snapshot.results.getValue("e1").status)

        val urls = mutableListOf<String>()
        val repo = BackendRepository("https://api.example.invalid", "production", transport = { req ->
            urls += req.url
            response(200, apiPayload("counts" to mapOf("photos" to 1)))
        }, persistence = MemorySession())
        repo.adminInbox("mehmet toker")
        assertEquals("https://api.example.invalid/v1/admin/inbox?q=mehmet%20toker", urls.single())
    }
}
