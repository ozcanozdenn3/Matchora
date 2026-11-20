package com.matchora.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.*

expect object PendingSendPersistence {
    fun read(owner: String): String?
    fun write(owner: String, value: String?)
}

interface PendingSendStorage {
    fun read(owner: String): String?
    fun write(owner: String, value: String?)
}
object DevicePendingSendStorage : PendingSendStorage {
    override fun read(owner: String) = PendingSendPersistence.read(owner)
    override fun write(owner: String, value: String?) = PendingSendPersistence.write(owner, value)
}

data class PendingSend(val record: JsonObject) {
    val id get() = record.s("id")
    val operation get() = record.s("operation")
    val payload get() = record.o("payload")
    val state get() = record.s("state", "queued")
    val error get() = record.s("error")
    val percent get() = record.i("percent")
    val threadId get() = payload.s("threadId").ifBlank { payload.s("shareThreadId") }
    val text get() = payload.s("text").ifBlank { payload.s("caption") }
    val hasPhoto get() = record.s("photo").isNotEmpty() || payload.s("uploadId").isNotEmpty()
    val destination get() = record.s("destination").ifBlank { if (operation == "memory.create") "Anı Vitrini" else "Sohbet" }
    val needsPhoto get() = state == "failed" && record.s("errorCode") == "UPLOAD_RECOVERY_REQUIRED"
    val label get() = when (state) {
        "checking" -> "Önceki gönderim kontrol ediliyor…"
        "uploading" -> if (percent >= 100) "Fotoğraf işleniyor…" else "Fotoğraf yükleniyor · %$percent"
        "sending" -> if (operation == "memory.create") "Yayınlanıyor…" else "Gönderiliyor…"
        "failed" -> error.ifBlank { "Gönderilemedi. Tekrar deneyebilirsin." }
        else -> "Bağlantı bekleniyor · gönderim kaydedildi"
    }
}

/** Owner-scoped disk queue. A successful server receipt is the only removal path. */
class PendingSendQueue(private val storage: PendingSendStorage = DevicePendingSendStorage) {
    private val mutex = Mutex()
    private var owner: String? = null
    private var generation = 0
    private val mutableItems = MutableStateFlow<List<PendingSend>>(emptyList())
    val items = mutableItems.asStateFlow()
    suspend fun load(user: String) = mutex.withLock {
        if (owner == user) return@withLock
        val revision = generation
        val saved = withContext(Dispatchers.Default) {
            storage.read(user)?.let { Json.parseToJsonElement(it).jsonArray.map { value -> PendingSend(value.jsonObject) } }.orEmpty()
        }
        if (generation != revision) return@withLock
        owner = user
        mutableItems.value = saved.map { item ->
            if (item.state in listOf("uploading", "sending", "checking")) PendingSend(JsonObject(item.record + ("state" to JsonPrimitive("queued")))) else item
        }
    }
    fun hide() { generation++; owner = null; mutableItems.value = emptyList() }
    private fun save(user: String, records: List<PendingSend>) {
        storage.write(user, records.takeIf { it.isNotEmpty() }?.let { JsonArray(it.map(PendingSend::record)).toString() })
        if (owner == user) mutableItems.value = records
    }
    suspend fun add(user: String, operation: String, payload: JsonObject, photo: String? = null, destination: String? = null): String {
        load(user)
        return mutex.withLock { withContext(Dispatchers.Default) {
            check(owner == user) { "Hesap değişti. Yeniden dene." }
            val records = mutableItems.value
            if (records.size >= 50 || records.sumOf { it.record.s("photo").length } + (photo?.length ?: 0) > 28 * 1024 * 1024)
                throw BackendFailure("QUEUE_FULL", "Bekleyen gönderimler doldu. Önce bekleyenleri gönder veya kaldır.")
            val id = "send-${Clock.System.now().toEpochMilliseconds()}-${kotlin.random.Random.nextLong().toULong()}"
            val next = PendingSend(apiPayload("id" to id, "operation" to operation, "payload" to payload,
                "photo" to photo, "destination" to destination, "state" to "queued", "attempts" to 0, "createdAt" to Clock.System.now().toEpochMilliseconds()))
            save(user, records + next)
            id
        } }
    }
    suspend fun change(user: String, id: String, vararg fields: Pair<String, Any?>) = mutex.withLock { withContext(Dispatchers.Default) {
        val records = if (owner == user) mutableItems.value else storage.read(user)?.let { Json.parseToJsonElement(it).jsonArray.map { v -> PendingSend(v.jsonObject) } }.orEmpty()
        val next = records.map { if (it.id == id) PendingSend(JsonObject(it.record + apiPayload(*fields))) else it }
        if (fields.any { it.first == "photo" } && next.sumOf { it.record.s("photo").length } > 28 * 1024 * 1024) {
            throw BackendFailure("QUEUE_FULL", "Bekleyen fotoğraflar doldu. Önce diğer gönderimleri tamamla veya kaldır.")
        }
        save(user, next)
    } }
    suspend fun remove(user: String, id: String) = mutex.withLock { withContext(Dispatchers.Default) {
        val records = if (owner == user) mutableItems.value else storage.read(user)?.let { Json.parseToJsonElement(it).jsonArray.map { v -> PendingSend(v.jsonObject) } }.orEmpty()
        save(user, records.filterNot { it.id == id })
    } }
    suspend fun discard(user: String, threadId: String? = null) = mutex.withLock { withContext(Dispatchers.Default) {
        val records = if (owner == user) mutableItems.value else storage.read(user)?.let {
            Json.parseToJsonElement(it).jsonArray.map { value -> PendingSend(value.jsonObject) }
        }.orEmpty()
        val kept = if (threadId == null) emptyList() else records.filterNot {
            it.operation == "chat.send" && it.threadId == threadId
        }
        save(user, kept)
    } }
    fun progress(user: String, id: String, percent: Int) {
        if (owner == user) mutableItems.update { records -> records.map { item ->
            if (item.id == id) PendingSend(JsonObject(item.record + apiPayload("percent" to percent.coerceIn(0,100)))) else item
        } }
    }
}

/** Global outbox deliberately does not depend on the set of currently accessible conversations. */
fun pendingSendsForDisplay(items: List<PendingSend>, threadId: String? = null, allDestinations: Boolean = false): List<PendingSend> =
    if (allDestinations) items else items.filter { if (threadId == null) it.operation == "memory.create" else it.threadId == threadId }
