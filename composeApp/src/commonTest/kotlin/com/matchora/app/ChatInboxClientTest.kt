package com.matchora.app

import com.matchora.app.data.*
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class ChatInboxClientTest {
    private val id = "c49c8ddd-4a65-41d8-92cc-555336cb84dc"
    private class Storage : SessionPersistence {
        override fun read() = apiPayload("accessToken" to "test", "userId" to "own").toString()
        override fun write(value: String?) {}
    }
    private fun response(value: kotlinx.serialization.json.JsonObject) = ApiHttpResponse(200, value.toString().encodeToByteArray())
    private fun thread(id: String, locked: Boolean = false) = apiPayload("id" to id, "type" to "DIRECT", "activityAt" to 1, "locked" to locked, "messages" to emptyList<String>())
    @Test fun olderInboxPagesRevalidateAndRemovedConversationsDoNotReturnFromCache() = runBlocking {
        var removed = false
        val repository = BackendRepository("https://api.example.invalid", "local", transport = { request ->
            when {
                request.url.contains("/v1/threads?") -> response(apiPayload("items" to if (removed) emptyList() else listOf(thread(id)), "nextCursor" to null))
                else -> response(apiPayload("userId" to "own", "threads" to listOf(thread("first")), "nextThreadCursor" to "next"))
            }
        }, persistence = Storage())
        repository.reload(); repository.setInboxActive(true); repository.loadMoreThreads()
        assertTrue(repository.snapshots.value!!.a("threads").any { it.s("id") == id })
        repository.reload(); assertTrue(repository.snapshots.value!!.a("threads").any { it.s("id") == id })
        removed = true; repository.reload()
        assertFalse(repository.snapshots.value!!.a("threads").any { it.s("id") == id })
    }
    @Test fun notificationCanOpenAConversationOutsideTheFirstPageAndRefreshItsAccess() = runBlocking {
        var locked = false
        val repository = BackendRepository("https://api.example.invalid", "local", transport = { request ->
            if (request.url.endsWith("/v1/threads/$id")) response(apiPayload("item" to thread(id, locked)))
            else response(apiPayload("userId" to "own", "threads" to emptyList<String>()))
        }, persistence = Storage())
        repository.reload(); repository.setActiveThread(id)
        assertEquals(id, repository.snapshots.value!!.a("threads").single().s("id"))
        locked = true; repository.reload()
        assertTrue(repository.snapshots.value!!.a("threads").single().b("locked"))
        repository.setActiveThread(null); repository.reload()
        assertTrue(repository.snapshots.value!!.a("threads").isEmpty())
    }
}
