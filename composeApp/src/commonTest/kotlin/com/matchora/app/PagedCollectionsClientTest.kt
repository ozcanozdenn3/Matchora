package com.matchora.app

import com.matchora.app.data.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import kotlin.test.*

class PagedCollectionsClientTest {
    private class MemorySession : SessionPersistence { var value: String? = null; override fun read() = value; override fun write(value: String?) { this.value = value } }
    private fun response(value: JsonObject) = ApiHttpResponse(200, value.toString().encodeToByteArray())
    @Test fun catalogPagesRevalidateAccessAndKeepFreshWalletAndPlayerValues() = runBlocking {
        var archived = false
        var balance = 20
        val repository = BackendRepository("https://api.example.invalid", "local", transport = { request ->
            response(when {
                request.url.endsWith("/config") -> apiPayload()
                request.url.contains("/catalog/teams?") -> apiPayload("items" to if (archived) emptyList() else listOf(mapOf("id" to "older-team")), "players" to listOf(mapOf("id" to "older-player", "name" to "Güncel sporcu")))
                request.url.contains("/commands/") -> { balance = 10; apiPayload("result" to emptyMap<String, String>(), "snapshot" to mapOf("teams" to listOf(mapOf("id" to "first")), "nextCatalogCursors" to mapOf("teams" to "next"), "wallet" to mapOf("balance" to balance))) }
                else -> apiPayload("teams" to listOf(mapOf("id" to "first")), "nextCatalogCursors" to mapOf("teams" to "next"), "wallet" to mapOf("balance" to balance))
            })
        }, persistence = MemorySession())
        repository.start(); repository.setActiveCatalogs(setOf("teams")); repository.loadMoreCatalog("teams")
        assertEquals(listOf("first", "older-team"), repository.snapshots.value!!.a("teams").map { it.s("id") })
        assertEquals("Güncel sporcu", repository.snapshots.value!!.a("players").single().s("name"))
        archived = true; repository.command("team.update", apiPayload(), "catalog-command")
        assertEquals(listOf("first"), repository.snapshots.value!!.a("teams").map { it.s("id") })
        assertEquals(10, BackendSnapshot(repository.snapshots.value!!).balance)
        repository.setActiveCatalogs(emptySet()); assertEquals(10, BackendSnapshot(repository.snapshots.value!!).balance)
    }
    @Test fun leavingCatalogDropsExtraPagesAndRestoresItsBaseCursor() = runBlocking {
        val repository = BackendRepository("https://api.example.invalid", "local", transport = { request -> response(if (request.url.contains("/catalog/venues?")) apiPayload("items" to listOf(mapOf("id" to "second"))) else apiPayload("venues" to listOf(mapOf("id" to "first")), "nextCatalogCursors" to mapOf("venues" to "base-cursor"))) }, persistence = MemorySession())
        repository.start(); repository.setActiveCatalogs(setOf("venues")); repository.loadMoreCatalog("venues"); repository.setActiveCatalogs(emptySet())
        assertEquals(listOf("first"), repository.snapshots.value!!.a("venues").map { it.s("id") })
        assertEquals("base-cursor", repository.snapshots.value!!.o("nextCatalogCursors").s("venues"))
    }
    @Test fun notificationPagingUsesServerFiltersAndRevalidatesAfterPersonalClear() = runBlocking {
        var cleared = false
        val urls = mutableListOf<String>()
        val repository = BackendRepository("https://api.example.invalid", "local", transport = { request ->
            urls += request.url
            response(when {
                request.url.contains("/commands/") -> { cleared = true; apiPayload("result" to emptyMap<String, String>(), "snapshot" to emptyMap<String, String>()) }
                request.url.contains("after=") -> apiPayload("items" to listOf(mapOf("id" to "older")), "unreadCount" to 130)
                else -> apiPayload("items" to if (cleared) emptyList() else listOf(mapOf("id" to "new")), "nextCursor" to if (cleared) null else "cursor+private", "unreadCount" to if (cleared) 0 else 130)
            })
        }, persistence = MemorySession())
        repository.selectNotifications("wallet"); repository.loadMoreNotifications()
        assertTrue(urls.all { it.contains("filter=wallet") }); assertTrue(urls.last().contains("after=cursor%2Bprivate"))
        assertEquals(listOf("new", "older"), repository.notificationFeed.value!!.a("items").map { it.s("id") })
        repository.command("notification.clear", apiPayload(), "clear-personal")
        assertTrue(repository.notificationFeed.value!!.a("items").isEmpty()); assertEquals(0, repository.notificationFeed.value!!.i("unreadCount"))
    }
    @Test fun closingTheInboxDiscardsAnInFlightPrivatePage() = runBlocking {
        val started = CompletableDeferred<Unit>(); val finish = CompletableDeferred<Unit>()
        val repository = BackendRepository("https://api.example.invalid", "local", transport = { started.complete(Unit); finish.await(); response(apiPayload("items" to listOf(mapOf("id" to "private")))) }, persistence = MemorySession())
        val request = launch { repository.selectNotifications("all") }
        started.await(); repository.closeNotifications(); finish.complete(Unit); request.join()
        assertNull(repository.notificationFeed.value)
    }
}
