package com.matchora.app

import com.matchora.app.data.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.json.*
import kotlin.test.*

class PendingSendTest {
    private class Disk : PendingSendStorage {
        val files = mutableMapOf<String, String>()
        var writable = true
        override fun read(owner: String) = files[owner]
        override fun write(owner: String, value: String?) {
            check(writable) { "Disk full" }
            if (value == null) files.remove(owner) else files[owner] = value
        }
    }
    private class Session(private var value: String?) : SessionPersistence {
        override fun read() = value
        override fun write(value: String?) { this.value = value }
    }
    private fun response(data: JsonObject) = ApiHttpResponse(200, data.toString().encodeToByteArray())
    private fun session(owner: String) = apiPayload("userId" to owner, "accessToken" to owner, "refreshToken" to "refresh")

    @Test fun inFlightPhotoSurvivesRestartWithItsOriginalKeyAndUploadedReceipt() = runBlocking<Unit> {
        val disk = Disk()
        val first = PendingSendQueue(disk)
        val id = first.add("owner", "chat.send", apiPayload("threadId" to "thread", "text" to "Selam"), "base64-photo")
        first.change("owner", id, "state" to "uploading", "attempts" to 1)
        val restored = PendingSendQueue(disk)
        restored.load("owner")
        assertEquals(id, restored.items.value.single().id)
        assertEquals("queued", restored.items.value.single().state)
        assertTrue(restored.items.value.single().hasPhoto)
        restored.change("owner", id, "payload" to apiPayload("threadId" to "thread", "uploadId" to "server-upload"), "photo" to null, "state" to "sending")
        val afterUpload = PendingSendQueue(disk)
        afterUpload.load("owner")
        assertEquals(id, afterUpload.items.value.single().id)
        assertEquals("server-upload", afterUpload.items.value.single().payload.s("uploadId"))
        assertEquals("", afterUpload.items.value.single().record.s("photo"))
    }

    @Test fun accountSwitchCannotDisplayOrEraseThePreviousAccountsOutbox() = runBlocking<Unit> {
        val disk = Disk()
        val queue = PendingSendQueue(disk)
        val id = queue.add("alice", "chat.send", apiPayload("threadId" to "private", "text" to "Özel"))
        queue.hide()
        queue.load("bob")
        assertTrue(queue.items.value.isEmpty())
        queue.change("alice", id, "state" to "failed")
        assertTrue(queue.items.value.isEmpty())
        queue.add("bob", "chat.send", apiPayload("threadId" to "bob-thread"))
        queue.load("alice")
        assertEquals(id, queue.items.value.single().id)
        assertEquals("failed", queue.items.value.single().state)
        assertNotNull(disk.files["bob"])
    }

    @Test fun failedPersistencePreservesExistingItemsAndDoesNotInventAnEnqueue() = runBlocking<Unit> {
        val disk = Disk()
        val queue = PendingSendQueue(disk)
        val id = queue.add("owner", "chat.send", apiPayload("text" to "İlk"))
        disk.writable = false
        assertFailsWith<IllegalStateException> { queue.add("owner", "chat.send", apiPayload("text" to "İkinci")) }
        assertEquals(listOf(id), queue.items.value.map { it.id })
        disk.writable = true
        val restored = PendingSendQueue(disk)
        restored.load("owner")
        assertEquals(listOf(id), restored.items.value.map { it.id })
    }

    @Test fun lostServerReplyRetriesTheSameCommandAfterAppRestartAndRemovesOnlyAfterReceipt() = runBlocking<Unit> {
        val disk = Disk()
        val stored = Session(session("owner").toString())
        val requests = mutableListOf<ApiHttpRequest>()
        var offline = true
        val transport: suspend (ApiHttpRequest) -> ApiHttpResponse = { request ->
            requests += request
            if (offline) throw IllegalStateException("Lost acknowledgement")
            if (request.url.endsWith("/command-receipts/resolve")) response(apiPayload("found" to true, "result" to mapOf("id" to "message"))) else response(apiPayload("result" to mapOf("id" to "message"), "snapshot" to mapOf("userId" to "owner")))
        }
        val first = BackendRepository("https://api.invalid", "local", transport, stored, disk)
        val id = first.enqueueSend("chat.send", apiPayload("threadId" to "thread", "text" to "Mesaj"))
        first.processPendingSends()
        assertEquals("queued", first.pendingSends.value.single().state)
        assertNotNull(disk.files["owner"])
        offline = false
        val restarted = BackendRepository("https://api.invalid", "local", transport, stored, disk)
        restarted.loadPendingSends()
        restarted.retrySend(id)
        restarted.processPendingSends()
        assertTrue(restarted.pendingSends.value.isEmpty())
        assertNull(disk.files["owner"])
        val commands = requests.filter { "/commands/" in it.url }
        assertEquals(listOf(id, id), commands.map { it.headers["Idempotency-Key"] })
        assertContentEquals(commands[0].body, commands[1].body)
    }

    @Test fun uploadedPhotoIsNotUploadedAgainWhenMessageAcknowledgementIsLost() = runBlocking<Unit> {
        val disk = Disk()
        val queue = PendingSendQueue(disk)
        val id = queue.add("owner", "chat.send", apiPayload("threadId" to "thread", "text" to "Fotoğraf"), "aW1hZ2U=")
        val stored = Session(session("owner").toString())
        var uploadCount = 0
        var offline = true
        val commandKeys = mutableListOf<String?>()
        val transport: suspend (ApiHttpRequest) -> ApiHttpResponse = { request ->
            if (request.url.endsWith("/media")) {
                uploadCount++
                assertEquals(id, request.headers["Idempotency-Key"])
                request.onUploadProgress?.invoke(5, 5)
                response(apiPayload("id" to "saved-upload"))
            } else if (request.url.endsWith("/command-receipts/resolve")) {
                response(apiPayload("found" to true, "result" to mapOf("id" to "message")))
            } else {
                commandKeys += request.headers["Idempotency-Key"]
                val payload = Json.parseToJsonElement(requireNotNull(request.body).decodeToString()).jsonObject
                assertEquals("saved-upload", payload.s("uploadId"))
                if (offline) throw IllegalStateException("Lost message receipt")
                response(apiPayload("result" to mapOf("id" to "message"), "snapshot" to mapOf("userId" to "owner")))
            }
        }
        val first = BackendRepository("https://api.invalid", "local", transport, stored, disk)
        first.processPendingSends()
        assertEquals("saved-upload", first.pendingSends.value.single().payload.s("uploadId"))
        assertEquals("aW1hZ2U=", first.pendingSends.value.single().record.s("photo"))
        assertNull(first.uploadProgress.value)
        offline = false
        val restarted = BackendRepository("https://api.invalid", "local", transport, stored, disk)
        restarted.loadPendingSends()
        restarted.retrySend(id)
        restarted.processPendingSends()
        assertEquals(1, uploadCount)
        assertEquals(2, commandKeys.size)
        assertTrue(commandKeys.all { it == id })
        assertTrue(restarted.pendingSends.value.isEmpty())
    }

    @Test fun deletingConversationDuringPhotoUploadCannotResurrectItWithAPendingMessage() = runBlocking<Unit> {
        val disk = Disk()
        PendingSendQueue(disk).add("owner", "chat.send", apiPayload("threadId" to "thread", "text" to "Fotoğraf"), "aW1hZ2U=")
        val uploading = CompletableDeferred<Unit>()
        val finishUpload = CompletableDeferred<Unit>()
        val commands = mutableListOf<String>()
        val repository = BackendRepository("https://api.invalid", "local", transport = { request ->
            if (request.url.endsWith("/media")) {
                uploading.complete(Unit)
                finishUpload.await()
                response(apiPayload("id" to "uploaded-photo"))
            } else {
                commands += request.url.substringAfterLast('/')
                response(apiPayload("result" to mapOf("hidden" to true), "snapshot" to mapOf("userId" to "owner", "threads" to emptyList<String>())))
            }
        }, persistence = Session(session("owner").toString()), pendingSendStorage = disk)
        val worker = async { repository.processPendingSends() }
        uploading.await()
        repository.command("chat.delete", apiPayload("threadId" to "thread"), "delete-thread-key")
        finishUpload.complete(Unit)
        worker.await()
        assertEquals(listOf("chat.delete"), commands)
        assertTrue(repository.pendingSends.value.isEmpty())
        assertNull(disk.files["owner"])
    }

    @Test fun deletingAccountClearsItsDurableQueueWhileOtherAccountsFilesRemain() = runBlocking<Unit> {
        val disk = Disk()
        val queue = PendingSendQueue(disk)
        queue.add("other-owner", "chat.send", apiPayload("text" to "Diğer hesap"))
        queue.add("owner", "chat.send", apiPayload("threadId" to "thread", "text" to "Silinecek"))
        val repository = BackendRepository("https://api.invalid", "local", transport = {
            response(apiPayload("result" to mapOf("requested" to true), "snapshot" to mapOf("userId" to "owner")))
        }, persistence = Session(session("owner").toString()), pendingSendStorage = disk)
        repository.loadPendingSends()
        repository.command("account.delete", apiPayload(), "delete-account-key")
        assertTrue(repository.pendingSends.value.isEmpty())
        assertNull(disk.files["owner"])
        assertNotNull(disk.files["other-owner"])
    }

    @Test fun permanentRejectionRemainsVisibleAndNeverFallsBackToMockSuccess() = runBlocking<Unit> {
        val disk = Disk()
        val repository = BackendRepository("https://api.invalid", "local", transport = {
            ApiHttpResponse(403, apiPayload("error" to mapOf("code" to "BLOCKED", "message" to "Sohbet engelli.")).toString().encodeToByteArray())
        }, persistence = Session(session("owner").toString()), pendingSendStorage = disk)
        repository.enqueueSend("chat.send", apiPayload("threadId" to "thread", "text" to "Mesaj"))
        repository.processPendingSends()
        assertEquals("failed", repository.pendingSends.value.single().state)
        assertEquals("Sohbet engelli.", repository.pendingSends.value.single().error)
        assertNotNull(disk.files["owner"])
        assertNull(repository.snapshots.value)
    }
    @Test fun expiredPhotoUsesItsRetainedBytesAndANewUploadKeyWithoutChangingTheSendKey() = runBlocking<Unit> {
        val disk = Disk()
        val queue = PendingSendQueue(disk)
        val id = queue.add("owner", "memory.create", apiPayload("caption" to "Anı", "uploadId" to "expired-upload"), "aW1hZ2U=")
        queue.change("owner", id, "commandAttempted" to true, "attempts" to 1)
        val commandPayloads = mutableListOf<JsonObject>()
        val uploadKeys = mutableListOf<String?>()
        val repository = BackendRepository("https://api.invalid", "local", transport = { request ->
            when {
                request.url.endsWith("/command-receipts/resolve") -> response(apiPayload("found" to false))
                request.url.endsWith("/media") -> {
                    uploadKeys += request.headers["Idempotency-Key"]
                    assertContentEquals("image".encodeToByteArray(), request.body)
                    response(apiPayload("id" to "renewed-upload"))
                }
                else -> {
                    assertEquals(id, request.headers["Idempotency-Key"])
                    val payload = Json.parseToJsonElement(requireNotNull(request.body).decodeToString()).jsonObject
                    commandPayloads += payload
                    if (payload.s("uploadId") == "expired-upload") ApiHttpResponse(409, apiPayload("error" to mapOf("code" to "UPLOAD_EXPIRED", "message" to "Süresi doldu.")).toString().encodeToByteArray())
                    else response(apiPayload("result" to mapOf("id" to "one-memory"), "snapshot" to mapOf("userId" to "owner")))
                }
            }
        }, persistence = Session(session("owner").toString()), pendingSendStorage = disk)
        repository.processPendingSends()
        assertEquals(listOf("expired-upload", "renewed-upload"), commandPayloads.map { it.s("uploadId") })
        assertEquals<List<String?>>(listOf("$id.photo.1"), uploadKeys)
        assertTrue(repository.pendingSends.value.isEmpty())
        assertNull(disk.files["owner"])
    }

    @Test fun expiredUploadReceiptAfterLostUploadReplyRenewsTheUploadBeforeSending() = runBlocking<Unit> {
        val disk = Disk()
        val id = PendingSendQueue(disk).add("owner", "chat.send", apiPayload("threadId" to "thread"), "aW1hZ2U=")
        val uploadKeys = mutableListOf<String?>()
        var sends = 0
        val repository = BackendRepository("https://api.invalid", "local", transport = { request ->
            if (request.url.endsWith("/media")) {
                uploadKeys += request.headers["Idempotency-Key"]
                if (uploadKeys.size == 1) ApiHttpResponse(409, apiPayload("error" to mapOf("code" to "UPLOAD_EXPIRED", "message" to "Süresi doldu.")).toString().encodeToByteArray())
                else response(apiPayload("id" to "renewed"))
            } else {
                sends++
                assertEquals(id, request.headers["Idempotency-Key"])
                response(apiPayload("result" to mapOf("id" to "one-message"), "snapshot" to mapOf("userId" to "owner")))
            }
        }, persistence = Session(session("owner").toString()), pendingSendStorage = disk)
        repository.processPendingSends()
        assertEquals<List<String?>>(listOf(id, "$id.photo.1"), uploadKeys)
        assertEquals(1, sends)
        assertTrue(repository.pendingSends.value.isEmpty())
    }

    @Test fun legacyExpiredPhotoCanBeFoundGloballyAfterConversationAccessIsLost() = runBlocking<Unit> {
        val disk = Disk()
        val queue = PendingSendQueue(disk)
        val id = queue.add("owner", "chat.send", apiPayload("threadId" to "unavailable-thread", "uploadId" to "expired"), destination = "Eski takım")
        val repository = BackendRepository("https://api.invalid", "local", transport = { request ->
            if (request.url.endsWith("/command-receipts/resolve")) response(apiPayload("found" to false))
            else ApiHttpResponse(409, apiPayload("error" to mapOf("code" to "UPLOAD_EXPIRED", "message" to "Süresi doldu.")).toString().encodeToByteArray())
        }, persistence = Session(session("owner").toString()), pendingSendStorage = disk)
        repository.processPendingSends()
        val item = repository.pendingSends.value.single()
        assertTrue(item.needsPhoto)
        assertEquals("Eski takım", item.destination)
        assertTrue(pendingSendsForDisplay(repository.pendingSends.value).isEmpty())
        assertEquals(listOf(id), pendingSendsForDisplay(repository.pendingSends.value, allDestinations = true).map { it.id })
        repository.removePendingSend(id)
        assertTrue(repository.pendingSends.value.isEmpty())
        assertNull(disk.files["owner"])
    }

    @Test fun replacingPhotoCannotBypassQueueStorageLimitOrDamageExistingOutbox() = runBlocking<Unit> {
        val disk = Disk()
        val queue = PendingSendQueue(disk)
        val id = queue.add("owner", "chat.send", apiPayload("threadId" to "thread"), "source")
        assertFailsWith<BackendFailure> { queue.change("owner", id, "photo" to "a".repeat(28 * 1024 * 1024 + 1)) }
        assertEquals("source", queue.items.value.single().record.s("photo"))
        val restored = PendingSendQueue(disk)
        restored.load("owner")
        assertEquals("source", restored.items.value.single().record.s("photo"))
    }

    @Test fun legacyPhotoReselectionReusesTheSendAndChecksForAnAlreadyCommittedMessage() = runBlocking<Unit> {
        val disk = Disk()
        val queue = PendingSendQueue(disk)
        val id = queue.add("owner", "chat.send", apiPayload("threadId" to "thread", "uploadId" to "expired"))
        queue.change("owner", id, "state" to "failed", "errorCode" to "UPLOAD_RECOVERY_REQUIRED")
        val committed = false
        var uploadCount = 0
        val commandKeys = mutableListOf<String?>()
        val backend = BackendRepository("https://api.invalid", "local", transport = { request ->
            when {
                request.url.endsWith("/command-receipts/resolve") -> response(apiPayload("found" to committed))
                request.url.endsWith("/media") -> {
                    uploadCount++
                    assertEquals("$id.photo.1", request.headers["Idempotency-Key"])
                    response(apiPayload("id" to "new-photo"))
                }
                else -> {
                    commandKeys += request.headers["Idempotency-Key"]
                    response(apiPayload("result" to mapOf("id" to "message"), "snapshot" to mapOf("userId" to "owner")))
                }
            }
        }, persistence = Session(session("owner").toString()), pendingSendStorage = disk)
        assertTrue(backend.replacePendingPhotoSource(id) { "replacement".encodeToByteArray() })
        assertEquals(id, backend.pendingSends.value.single().id)
        assertFalse(backend.pendingSends.value.single().needsPhoto)
        backend.processPendingSends()
        assertEquals(1, uploadCount)
        assertEquals<List<String?>>(listOf(id), commandKeys)
        // A late original receipt must win before the replacement source is read.
        val secondQueue = PendingSendQueue(disk)
        val second = secondQueue.add("owner", "chat.send", apiPayload("threadId" to "thread", "uploadId" to "old-photo"))
        secondQueue.change("owner", second, "state" to "failed", "errorCode" to "UPLOAD_RECOVERY_REQUIRED")
        // A separate repository simulates reopening the persisted queue.
        val reopened = BackendRepository("https://api.invalid", "local", transport = { request ->
            if (request.url.endsWith("/command-receipts/resolve")) response(apiPayload("found" to true))
            else {
                assertEquals(second, request.headers["Idempotency-Key"])
                response(apiPayload("result" to mapOf("id" to "original-message"), "snapshot" to mapOf("userId" to "owner")))
            }
        }, persistence = Session(session("owner").toString()), pendingSendStorage = disk)
        assertFalse(reopened.replacePendingPhotoSource(second) { fail("Committed photos must not be replaced") })
        assertFalse(reopened.pendingSends.value.any { it.id == second })
    }

}
