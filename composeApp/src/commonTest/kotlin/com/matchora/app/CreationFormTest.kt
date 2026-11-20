package com.matchora.app

import com.matchora.app.data.*
import com.matchora.app.ui.components.CreationFormState
import kotlinx.coroutines.*
import kotlinx.serialization.json.JsonObject
import kotlin.test.*

class CreationFormTest {
    private class Session : SessionPersistence {
        override fun read() = apiPayload("userId" to "owner", "accessToken" to "token").toString()
        override fun write(value: String?) {}
    }
    private fun response(value: JsonObject) = ApiHttpResponse(200, value.toString().encodeToByteArray())

    @Test fun formStaysOpenUntilAcknowledgementAndRestoresItsAttemptAfterFailure() = runBlocking<Unit> {
        val files = mutableMapOf<String,String>()
        fun draft() = ListingDraftStore("team:owner", { files[it] }, { key,value -> if (value == null) files.remove(key) else files[key] = value })
        val started = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        val keys = mutableListOf<String?>()
        var networkFails = true
        val repository = BackendRepository("https://api.invalid", "local", transport = { request ->
            keys += request.headers["Idempotency-Key"]
            started.complete(Unit)
            finish.await()
            if (networkFails) throw IllegalStateException("Lost reply")
            response(apiPayload("result" to mapOf("id" to "created-team"), "snapshot" to mapOf("userId" to "owner")))
        }, persistence = Session())
        var dismissed = 0
        var published = ""
        val first = CreationFormState(repository, "owner", draft(), this)
        val payload = apiPayload("name" to "Takım", "sport" to "TENNIS")
        first.fields = payload
        first.initialFields = apiPayload()
        first.submit("team.create", payload, { fail("Preview must not run") }, { published = it }, { dismissed++ })
        started.await()
        assertTrue(first.busy)
        assertEquals(0, dismissed)
        assertFalse(first.confirmDismiss { dismissed++ })
        finish.complete(Unit)
        yield()
        assertFalse(first.busy)
        assertNotNull(first.error)
        assertEquals(0, dismissed)
        assertEquals("", published)
        val restored = CreationFormState(repository, "owner", draft(), this)
        assertEquals(payload, restored.restored)
        assertTrue(restored.hasAttempt)
        networkFails = false
        restored.submit("team.create", payload, { fail("Preview must not run") }, { published = it }, { dismissed++ })
        yield()
        assertEquals("created-team", published)
        assertEquals(1, dismissed)
        assertTrue(files.isEmpty())
        assertEquals(2, keys.size)
        assertEquals(keys[0], keys[1])
    }

    @Test fun lastInputIsSavedEvenBeforeTheNextUiFrame() = runBlocking<Unit> {
        var saved: String? = null
        var current = apiPayload("name" to "Önceki")
        val draft = ListingDraftStore("owner", { saved }, { _,value -> saved = value })
        val form = CreationFormState(null, null, draft, this)
        form.fields = current
        form.initialFields = current
        form.fieldProvider = { current }
        current = apiPayload("name" to "Son yazılan")
        assertTrue(form.dirty)
        assertTrue(form.save())
        val restored = ListingDraftStore("owner", { saved }, { _,_ -> })
        assertEquals("Son yazılan", requireNotNull(restored.restore()).s("name"))
    }

    @Test fun diskFailureDoesNotSendOrDismissAndAccountChangesCannotPublishTheOldDraft() = runBlocking<Unit> {
        var calls = 0
        val repository = BackendRepository("https://api.invalid", "local", transport = { calls++; response(apiPayload()) }, persistence = Session())
        val broken = ListingDraftStore("owner", { null }, { _,_ -> error("Disk full") })
        val form = CreationFormState(repository, "owner", broken, this)
        form.fields = apiPayload("name" to "Kulüp")
        var closed = false
        form.submit("team.create", form.fields, {}, {}, { closed = true })
        assertEquals(0, calls)
        assertFalse(closed)
        assertFalse(form.busy)
        assertEquals("Disk full", form.error)
        val otherDraft = ListingDraftStore("other", { null }, { _,_ -> })
        val wrongOwner = CreationFormState(repository, "other", otherDraft, this)
        wrongOwner.fields = apiPayload("name" to "Başka hesap")
        wrongOwner.submit("team.create", wrongOwner.fields, {}, {}, { closed = true })
        yield()
        assertEquals(0, calls)
        assertFalse(closed)
        assertNotNull(wrongOwner.error)
    }
}
