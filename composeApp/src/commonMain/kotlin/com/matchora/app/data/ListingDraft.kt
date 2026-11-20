package com.matchora.app.data

import kotlinx.serialization.json.*
import kotlin.random.Random

expect object ListingDraftStorage {
    fun read(owner: String): String?
    fun write(owner: String, value: String?)
}

data class PendingPublication(val key: String, val payload: JsonObject, val fields: JsonObject?)

/** A form draft and its publication attempt are stored together, scoped to the signed-in account. */
class ListingDraftStore(
    private val owner: String,
    private val read: (String) -> String? = ListingDraftStorage::read,
    private val write: (String, String?) -> Unit = ListingDraftStorage::write,
    private val newKey: () -> String = { "listing-${Random.nextLong().toString(16).replace('-', 'n')}-${Random.nextLong().toString(16).replace('-', 'n')}" }
) {
    private var record = runCatching { read(owner)?.let { Json.parseToJsonElement(it).jsonObject } }.getOrNull() ?: apiPayload()
    fun restore(): JsonObject? = record["fields"] as? JsonObject
    val hasPublicationAttempt: Boolean get() = record.s("requestKey").isNotBlank()
    val pendingPublication: PendingPublication? get() = if (!hasPublicationAttempt) null else {
        val payload = runCatching { Json.parseToJsonElement(record.s("fingerprint")).jsonObject }.getOrNull()
        payload?.let { PendingPublication(record.s("requestKey"), it, record["attemptFields"] as? JsonObject) }
    }
    fun save(fields: JsonObject) {
        persist(JsonObject(record + ("fields" to fields)))
    }
    fun publicationKey(payload: JsonObject): String {
        val fingerprint = payload.toString()
        if (record.s("fingerprint") == fingerprint && record.s("requestKey").isNotBlank()) return record.s("requestKey")
        val key = newKey()
        persist(JsonObject(record + apiPayload("fingerprint" to fingerprint, "requestKey" to key, "attemptFields" to restore())))
        return key
    }
    /** Keep edits made after the original request; older drafts lack a comparable field snapshot. */
    fun finishRecoveredPublication(): Boolean {
        val attempted = pendingPublication?.fields
        val current = restore()
        val keepDraft = current != null && (attempted == null || JsonObject(current - "step") != JsonObject(attempted - "step"))
        if (keepDraft) {
            persist(JsonObject(record - setOf("fingerprint", "requestKey", "attemptFields")))
        } else clear()
        return keepDraft
    }
    private fun persist(next: JsonObject) { write(owner, next.toString()); record = next }
    fun clear() { write(owner, null); record = apiPayload() }
}

data class RecoveredPublication(val id: String, val keptDraft: Boolean)

/** Run before new-publication validation: a committed event can have a past date and no free tokens. */
suspend fun recoverListingPublication(store: ListingDraftStore, backend: BackendRepository, owner: String): RecoveredPublication? {
    if (!store.hasPublicationAttempt) return null
    val attempt = store.pendingPublication
        ?: throw BackendFailure("DRAFT_RECEIPT", "Önceki yayın kaydı okunamadı. Taslağın korunuyor.")
    if (!backend.resolveCommand("event.create", attempt.payload, attempt.key, owner).b("found")) return null
    // Replay uses the same immutable payload/key and refreshes the wallet and event snapshot.
    val result = backend.command("event.create", attempt.payload, attempt.key, expectedOwner = owner)
    return RecoveredPublication(result.s("id"), store.finishRecoveredPublication())
}

/** Returns the step that owns the first invalid field, including when jumping ahead using the tabs. */
fun listingDraftError(fields: JsonObject, throughStep: Int, approvedCommunityIds: Set<String>? = null, now: Long): Pair<Int, String>? {
    val gaming = fields.b("gaming")
    val title = fields.s("title").trim()
    val network = fields.s("networkScope", "CITY")
    val restricted = network == "CAMPUS" || network == "COMPANY"
    fun invalid(step: Int, text: String) = step to text
    if (title.length !in 3..120) return invalid(0, "İlan başlığı 3–120 karakter olmalı.")
    if (fields.s("description").length > 2000) return invalid(0, "Açıklama en fazla 2000 karakter olabilir.")
    if (fields.s("sport") == "CUSTOM" && fields.s("customSportName").trim().length !in 2..60) return invalid(0, "Özel branş adı 2–60 karakter olmalı.")
    if (restricted && approvedCommunityIds != null && fields.s("communityId") !in approvedCommunityIds) return invalid(0, "İlanı yayınlayacağın onaylı topluluğu seç.")
    if (restricted && fields.s("networkName").trim().length !in 2..80) return invalid(0, "Seçtiğin ağın adını gir.")
    if (throughStep < 1) return null
    if (fields.l("startsAt") < now + 60000) return invalid(1, "En az bir dakika ileride bir tarih ve saat seç.")
    if (fields.l("startsAt") > now + 365L * 86400000) return invalid(1, "Bir yıl içindeki bir tarih seç.")
    if (fields.s("city").trim().length !in 2..60) return invalid(1, "Şehir bilgisini kontrol et.")
    if (gaming) {
        if (fields.s("server").trim().length !in 2..160) return invalid(1, "Oyun sunucusu veya bölgesi 2–160 karakter olmalı.")
        if ((fields.s("platform") + " · " + fields.s("server").trim()).length > 160) return invalid(1, "Platform ve sunucu bilgilerini kısalt.")
    } else {
        if (fields.s("area").trim().length !in 2..60) return invalid(1, "İlçe bilgisini kontrol et.")
        if (fields.s("venue").trim().length !in 2..160) return invalid(1, "Tesis veya saha adı 2–160 karakter olmalı.")
        val point = fields["point"] as? JsonObject
        val latitude = point?.get("latitude")?.jsonPrimitive?.doubleOrNull
        val longitude = point?.get("longitude")?.jsonPrimitive?.doubleOrNull
        if (latitude == null || longitude == null || !latitude.isFinite() || !longitude.isFinite() || latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return invalid(1, "Buluşma noktasını haritadan seç.")
    }
    val role = fields.s(if (gaming) "gamingRole" else "role").trim()
    if (fields.s("intent") == "FIND_PLAYERS" && role.isBlank()) return invalid(1, "Aranan rolü veya oyuncuyu gir.")
    if (role.split(',').filter { it.isNotBlank() }.let { it.size > 10 || it.any { role -> role.trim().length > 80 } }) return invalid(1, "En fazla 10 rol gir; her rol en fazla 80 karakter olabilir.")
    val format = fields.s(if (gaming) "gamingFormat" else "format").trim()
    if (format.length !in 1..80) return invalid(1, "Maç formatını kontrol et.")
    if (throughStep < 2) return null
    val duration = fields.s("duration").toIntOrNull()
    if (duration == null || duration !in 15..720) return invalid(2, "Maç süresi 15–720 dakika olmalı.")
    val fee = fields.s("fee").ifBlank { "0" }.toIntOrNull()
    if (fee == null || fee !in 0..100000) return invalid(2, "Kişi başı ücret 0–100.000 TL arasında olmalı.")
    if (fields.s("levelRange").trim().length !in 1..80) return invalid(2, "Seviye bilgisini kontrol et.")
    if (fields.s("voice").length > 200 || fields.s("gamerTag").length > 80) return invalid(1, "Sesli iletişim veya oyuncu ID’sini kısalt.")
    return null
}
