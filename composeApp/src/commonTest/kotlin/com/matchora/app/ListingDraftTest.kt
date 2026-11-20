package com.matchora.app

import com.matchora.app.data.*
import kotlin.test.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class ListingDraftTest {
    private val now = 1_000_000L
    private fun fields() = apiPayload("title" to "Akşam maçı", "sport" to "FOOTBALL", "networkScope" to "CITY",
        "startsAt" to now + 600000, "city" to "İstanbul", "area" to "Kadıköy", "venue" to "Merkez saha",
        "point" to mapOf("latitude" to 40.9, "longitude" to 29.0), "intent" to "FIND_PLAYERS", "role" to "Kaleci",
        "format" to "7v7", "duration" to "60", "fee" to "0", "levelRange" to "Orta")
    @Test fun storedDraftAndPublicationKeySurviveReopeningAndAreScopedToTheAccount() {
        val values = mutableMapOf<String,String>()
        var next = 0
        fun store(owner: String) = ListingDraftStore(owner, { values[it] }, { owner,value -> if (value == null) values.remove(owner) else values[owner] = value }, { "key-${++next}" })
        val first = store("first")
        first.save(fields())
        val key = first.publicationKey(apiPayload("title" to "Akşam maçı"))
        val restored = store("first")
        assertEquals(fields(),restored.restore())
        assertEquals(key,restored.publicationKey(apiPayload("title" to "Akşam maçı")))
        assertNotEquals(key,restored.publicationKey(apiPayload("title" to "Yeni maç")))
        assertNull(store("second").restore())
        restored.clear()
        assertNull(store("first").restore())
    }
    @Test fun stepChecksStopAtTheOwningStepAndMirrorServerBounds() {
        assertNull(listingDraftError(fields(),2,now=now))
        val shortTitle = JsonObject(fields() + ("title" to JsonPrimitive("ab")))
        assertEquals(0,listingDraftError(shortTitle,0,now=now)?.first)
        val missingPoint = JsonObject(fields() - "point")
        assertNull(listingDraftError(missingPoint,0,now=now))
        assertEquals(1,listingDraftError(missingPoint,1,now=now)?.first)
        assertEquals(2,listingDraftError(JsonObject(fields() + ("fee" to JsonPrimitive("100001"))),2,now=now)?.first)
        val restricted = JsonObject(fields() + apiPayload("networkScope" to "CAMPUS", "communityId" to "unapproved", "networkName" to "Okul"))
        assertEquals(0,listingDraftError(restricted,2,setOf("approved"),now)?.first)
    }
    @Test fun publicationReceiptRetainsOriginalPayloadAfterBalanceAndDateOrDraftChange() {
        var disk: String? = null
        fun store() = ListingDraftStore("owner", { disk }, { _, value -> disk = value }, { "stable-publication-key" })
        val original = fields()
        val first = store()
        first.save(original)
        val payload = apiPayload("title" to original.s("title"), "startsAtEpochMillis" to original.l("startsAt"))
        val key = first.publicationKey(payload)
        val edits = JsonObject(original + apiPayload("title" to "Sonraki ilan"))
        first.save(edits)
        val restored = store()
        assertEquals(PendingPublication(key, payload, original), restored.pendingPublication)
        assertNotNull(listingDraftError(original, 2, now = now + 86400000))
        assertTrue(restored.finishRecoveredPublication())
        assertEquals(edits, store().restore())
        assertFalse(store().hasPublicationAttempt)
    }

    @Test fun recoveredUneditedPublicationClearsDraftButStepNavigationDoesNotCountAsAnEdit() {
        var disk: String? = null
        val store = ListingDraftStore("owner", { disk }, { _, value -> disk = value })
        store.save(fields())
        store.publicationKey(apiPayload("title" to "Akşam maçı"))
        store.save(JsonObject(fields() + apiPayload("step" to 2)))
        assertFalse(store.finishRecoveredPublication())
        assertNull(store.restore())
        assertNull(disk)
    }

    @Test fun legacyPublicationWithoutFieldSnapshotConservativelyKeepsItsDraft() {
        var disk: String? = apiPayload("fields" to fields(), "fingerprint" to apiPayload("title" to "Akşam maçı").toString(), "requestKey" to "legacy-key").toString()
        val store = ListingDraftStore("owner", { disk }, { _, value -> disk = value })
        assertNotNull(store.pendingPublication)
        assertTrue(store.finishRecoveredPublication())
        assertEquals(fields(), store.restore())
        assertFalse(store.hasPublicationAttempt)
    }

    private class Session : SessionPersistence {
        override fun read() = apiPayload("userId" to "owner", "accessToken" to "token", "refreshToken" to "refresh").toString()
        override fun write(value: String?) {}
    }

    @Test fun confirmedPublicationBypassesNewListingValidationAndRefreshesWalletWithoutNewKey() = runBlocking<Unit> {
        var disk: String? = null
        val store = ListingDraftStore("owner", { disk }, { _, value -> disk = value }, { "listing-original-key" })
        val original = fields()
        store.save(original)
        val payload = apiPayload("title" to original.s("title"), "startsAtEpochMillis" to original.l("startsAt"))
        val key = store.publicationKey(payload)
        val requests = mutableListOf<ApiHttpRequest>()
        val backend = BackendRepository("https://api.invalid", "local", transport = { request ->
            requests += request
            val body = kotlinx.serialization.json.Json.parseToJsonElement(requireNotNull(request.body).decodeToString()) as JsonObject
            val data = if (request.url.endsWith("/command-receipts/resolve")) {
                assertEquals(payload, body.o("payload"))
                assertEquals(key, body.s("requestKey"))
                apiPayload("found" to true, "result" to mapOf("id" to "existing-event"))
            } else {
                assertEquals(payload, body)
                assertEquals(key, request.headers["Idempotency-Key"])
                apiPayload("result" to mapOf("id" to "existing-event"), "snapshot" to mapOf("userId" to "owner", "wallet" to mapOf("balance" to 0, "locked" to 10)))
            }
            ApiHttpResponse(200, data.toString().encodeToByteArray())
        }, persistence = Session())
        assertNotNull(listingDraftError(original, 2, now = now + 86400000))
        assertEquals(RecoveredPublication("existing-event", false), recoverListingPublication(store, backend, "owner"))
        assertEquals(2, requests.size)
        assertEquals(0, backend.snapshots.value?.o("wallet")?.i("balance"))
        assertNull(disk)
    }

    @Test fun missingOrUnreachableReceiptKeepsTheDraftAndCannotPublishAnUncertainSecondListing() = runBlocking<Unit> {
        var disk: String? = null
        val store = ListingDraftStore("owner", { disk }, { _, value -> disk = value })
        store.save(fields())
        val key = store.publicationKey(apiPayload("title" to "Akşam maçı"))
        var offline = false
        val backend = BackendRepository("https://api.invalid", "local", transport = { request ->
            assertTrue(request.url.endsWith("/command-receipts/resolve"))
            if (offline) throw IllegalStateException("Disconnected")
            ApiHttpResponse(200, apiPayload("found" to false).toString().encodeToByteArray())
        }, persistence = Session())
        assertNull(recoverListingPublication(store, backend, "owner"))
        assertEquals(key, store.pendingPublication?.key)
        offline = true
        assertFailsWith<BackendFailure> { recoverListingPublication(store, backend, "owner") }
        assertEquals(key, store.pendingPublication?.key)
        assertEquals(fields(), store.restore())
    }

}
