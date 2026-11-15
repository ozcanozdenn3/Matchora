package com.matchora.app

import com.matchora.app.data.*
import com.matchora.app.state.*
import com.matchora.app.ui.components.*
import com.matchora.app.ui.screens.resolveChatParticipantPlayer
import com.matchora.app.ui.screens.resolvePassportDisplayState
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import kotlin.test.*

class SocialClientTest {
    private class MemorySession : SessionPersistence {
        var value: String? = null
        override fun read() = value
        override fun write(value: String?) { this.value = value }
    }
    private fun response(value: JsonObject) = ApiHttpResponse(200, value.toString().encodeToByteArray())
    private val threadId = "12345678-1234-1234-1234-123456789012"
    private fun message(id: String, sequence: Int) = mapOf("id" to id, "sequence" to sequence, "createdAt" to 1, "text" to id)
    private fun snapshot(revision: String) = apiPayload("threads" to listOf(mapOf("id" to threadId, "historyRevision" to revision, "messages" to listOf(message("latest", 3)), "nextMessageCursor" to "older")))

    @Test fun changedHistoryRevisionDropsPreviouslyLoadedDeletedContent() = runBlocking {
        var revision = "0:1"
        val repository = BackendRepository("https://api.example.invalid", "local", transport = { request ->
            response(when {
                request.url.endsWith("/config") -> apiPayload()
                request.url.contains("/messages?") -> apiPayload("items" to listOf(message("deleted-private", 1), message("older", 2)))
                else -> snapshot(revision)
            })
        }, persistence = MemorySession())
        repository.start(); repository.loadOlderMessages(threadId)
        assertEquals(listOf("deleted-private", "older", "latest"), repository.snapshots.value!!.a("threads").single().a("messages").map { it.s("id") })
        revision = "1:1"; repository.reload()
        assertEquals(listOf("latest"), repository.snapshots.value!!.a("threads").single().a("messages").map { it.s("id") })
    }

    @Test fun switchingAccountsDoesNotInheritAnotherMembersCachedHistoryOrFeed() = runBlocking {
        var account = "first"
        val repository = BackendRepository("https://api.example.invalid", "local", transport = { request ->
            response(when {
                request.url.endsWith("/auth/dev") -> apiPayload("userId" to account, "accessToken" to account)
                request.url.contains("/messages?") -> apiPayload("items" to listOf(message("first-only", 1)))
                request.url.contains("/memories?") -> apiPayload("items" to listOf(mapOf("id" to "first-pending")))
                else -> snapshot("0:1")
            })
        }, persistence = MemorySession())
        repository.developmentLogin(); repository.loadOlderMessages(threadId)
        repository.selectMemoryFeed("mine", "İstanbul", "Kadıköy", null, null, "seed")
        account = "second"; repository.developmentLogin()
        assertNull(repository.memoryFeed.value)
        assertEquals(listOf("latest"), repository.snapshots.value!!.a("threads").single().a("messages").map { it.s("id") })
    }

    @Test fun feedPagesAreDeduplicatedRevalidatedAfterBlockingAndFiltersAreEncoded() = runBlocking {
        var blocked = false
        val urls = mutableListOf<String>()
        val repository = BackendRepository("https://api.example.invalid", "local", transport = { request ->
            urls += request.url
            response(when {
                request.url.contains("/commands/") -> { blocked = true; apiPayload("result" to mapOf("blocked" to true), "snapshot" to emptyMap<String, String>()) }
                request.url.contains("after=") -> apiPayload("items" to if (blocked) emptyList() else listOf(mapOf("id" to "first"), mapOf("id" to "second")))
                else -> apiPayload("items" to listOf(mapOf("id" to "first")), "nextCursor" to "private+cursor")
            })
        }, persistence = MemorySession())
        repository.selectMemoryFeed("nearby", "İstanbul", "Kadıköy & Moda", 41.0, 29.0, "seed")
        repository.loadMoreMemories()
        assertEquals(listOf("first", "second"), repository.memoryFeed.value!!.a("items").map { it.s("id") })
        assertTrue(urls.first().contains("district=Kad%C4%B1k%C3%B6y%20%26%20Moda"))
        assertTrue(urls.last().contains("after=private%2Bcursor"))
        repository.command("user.block", apiPayload("userId" to "peer", "blocked" to true), "block-once")
        assertEquals(listOf("first"), repository.memoryFeed.value!!.a("items").map { it.s("id") })
    }

    @Test fun notificationCategoryWinsOverPriorityAndTextHeuristics() {
        val notification = AppNotification(id = "n", priority = NotifPriority.P0, emoji = "", title = "Token başvurusu", body = "", timeAgo = "Şimdi", category = "teams")
        assertTrue(notification.matchesFilter(NotifFilterCategory.SQUADS))
        assertFalse(notification.matchesFilter(NotifFilterCategory.WALLET))
    }
    @Test fun leavingWaitlistNeverAcceptsAnOfferAndVerifiedHistoryPreservesTheActualOutcome() {
        val snapshot = BackendSnapshot(apiPayload("userId" to "me", "waitlist" to listOf(mapOf("event_id" to "event", "status" to "offered")),
            "matchHistory" to listOf(mapOf("id" to "played", "sport" to "TENNIS", "title" to "Kort maçı", "date" to "8 Kas", "score" to "2 – 1", "won" to true, "verified" to true, "mode" to "COMPETITIVE", "mmrDelta" to 12)),
            "reliabilityHistory" to listOf(mapOf("reason" to "Doğrulanmış katılım", "delta" to 2, "date" to "8 Kas", "detail" to "Kort maçı"))))
        assertEquals("leave", snapshot.waitlistAction("event"))
        assertEquals("join", snapshot.waitlistAction("other-event"))
        assertEquals(true, snapshot.history.single().won)
        assertEquals("8 Kas", snapshot.reliabilityHistory.single().time)
    }

    @Test fun liveGuestPassportNeverLeaksDemoIdentityOrRecords() {
        val guest = UserProfile("Misafir Sporcu", "Ankara", "Çankaya", "", 2000, "", "", emptyList(), emptySet(), 100, 0f, 0, 0, null, null, "", emptyList())
        val state = resolvePassportDisplayState(
            user = guest,
            isLoggedIn = false,
            liveMode = true,
            historyOverride = emptyList(),
            reliabilityOverride = emptyList(),
            badgesOverride = emptyList(),
            missionsOverride = emptyList()
        )
        assertEquals("Misafir Sporcu", state.effectiveUser.name)
        assertEquals("Ankara", state.effectiveUser.city)
        assertEquals("Çankaya", state.effectiveUser.area)
        assertTrue(state.history.isEmpty())
        assertTrue(state.reliabilityRecords.isEmpty())
        assertTrue(state.badges.none { it.earned })
        assertTrue(state.allMissions.all { it.progress == 0 })
    }

    @Test fun liveChatParticipantResolutionAvoidsStaticDemoStatsAndUsesRealIds() {
        val demoPeerName = DemoData.players.first().name
        val uncached = resolveChatParticipantPlayer(
            name = demoPeerName,
            threadId = "thread-1",
            backendMode = true,
            livePlayers = emptyList(),
            directOtherUserId = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
            fallbackArea = "Çankaya"
        )
        assertEquals("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa", uncached.id)
        assertEquals(0, uncached.verifiedMatches)
        assertEquals(false, uncached.reliabilityVisible)
        assertEquals(false, uncached.verified)
        assertEquals("", uncached.bio)

        val livePlayer = uncached.copy(verifiedMatches = 19, reliability = 97, rating = 1520, verified = true, bio = "Canlı profil")
        val resolved = resolveChatParticipantPlayer(
            name = demoPeerName,
            threadId = "thread-1",
            backendMode = true,
            livePlayers = listOf(livePlayer),
            directOtherUserId = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
            fallbackArea = "Çankaya"
        )
        assertEquals(1520, resolved.rating)
        assertEquals("Canlı profil", resolved.bio)
    }

    @Test fun liveMemoryFeedPaginationMergesAdditionalPagesCleanly() = runBlocking {
        val repository = BackendRepository("https://api.example.invalid", "local", transport = { request ->
            response(when {
                request.url.contains("after=page-2") -> apiPayload(
                    "items" to (25..30).map { idx -> mapOf("id" to "mem-$idx", "authorId" to "u1", "authorName" to "Sporcu", "sport" to "FOOTBALL", "venue" to "Saha", "area" to "Kadıköy", "city" to "İstanbul", "caption" to "Kare $idx") },
                    "nextCursor" to null
                )
                else -> apiPayload(
                    "items" to (1..24).map { idx -> mapOf("id" to "mem-$idx", "authorId" to "u1", "authorName" to "Sporcu", "sport" to "FOOTBALL", "venue" to "Saha", "area" to "Kadıköy", "city" to "İstanbul", "caption" to "Kare $idx") },
                    "nextCursor" to "page-2"
                )
            })
        }, persistence = MemorySession())
        repository.selectMemoryFeed("nearby", "İstanbul", "Kadıköy", null, null, "seed")
        assertEquals(24, repository.memoryFeed.value!!.a("items").size)
        assertEquals("page-2", repository.memoryFeed.value!!.s("nextCursor"))
        repository.loadMoreMemories()
        assertEquals(30, repository.memoryFeed.value!!.a("items").size)
        assertEquals("", repository.memoryFeed.value!!.s("nextCursor"))
    }
}
