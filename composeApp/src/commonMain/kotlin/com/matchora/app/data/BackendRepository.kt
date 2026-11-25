package com.matchora.app.data

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import kotlinx.datetime.Clock

class BackendFailure(val code: String, message: String, val status: Int = 0) : Exception(message)
fun apiPayload(vararg fields: Pair<String, Any?>): JsonObject = JsonObject(fields.associate { it.first to jsonValue(it.second) })
fun jsonValue(value: Any?): JsonElement = when (value) {
    null -> JsonNull; is JsonElement -> value; is String -> JsonPrimitive(value); is Boolean -> JsonPrimitive(value)
    is Number -> JsonPrimitive(value); is Enum<*> -> JsonPrimitive(value.name)
    is Map<*, *> -> JsonObject(value.entries.associate { it.key.toString() to jsonValue(it.value) })
    is Iterable<*> -> JsonArray(value.map(::jsonValue)); else -> error("Desteklenmeyen API alanı")
}

/** All remote changes are serialized and snapshots are applied only after the server commits. */
interface SessionPersistence { fun read(): String?; fun write(value: String?) }
object NativeSessionPersistence : SessionPersistence {
    override fun read() = SessionVault.read()
    override fun write(value: String?) = SessionVault.write(value)
}
class BackendRepository(val baseUrl: String = BackendBuildConfig.BASE_URL, val mode: String = BackendBuildConfig.MODE,
    private val transport: suspend (ApiHttpRequest) -> ApiHttpResponse = ::executeApiRequest,
    private val persistence: SessionPersistence = NativeSessionPersistence,
    pendingSendStorage: PendingSendStorage = DevicePendingSendStorage) {
    val enabled: Boolean get() = mode != "preview"
    private val parser = Json { ignoreUnknownKeys = true }
    private val requestMutex = Mutex()
    private val refreshMutex = Mutex()
    private var pendingCommands = 0
    private var session: JsonObject? = runCatching { persistence.read()?.let { parser.parseToJsonElement(it).jsonObject } }.getOrNull()
    private val mutableSnapshot = MutableStateFlow<JsonObject?>(null)
    val snapshots = mutableSnapshot.asStateFlow()
    private val mutableConfiguration = MutableStateFlow<JsonObject?>(null)
    val configurations = mutableConfiguration.asStateFlow()
    val configuration: JsonObject? get() = mutableConfiguration.value
    private val messageHistory = mutableMapOf<String, JsonObject>()
    private val mutableMemoryFeed = MutableStateFlow<JsonObject?>(null)
    val memoryFeed = mutableMemoryFeed.asStateFlow()
    private var memoryFeedQuery: String? = null
    private var memoryFeedPages = 0
    private val mutableNotificationFeed = MutableStateFlow<JsonObject?>(null)
    val notificationFeed = mutableNotificationFeed.asStateFlow()
    private var notificationsActive = false
    private var notificationFilter = "all"
    private var notificationPages = 0
    private val sendQueue = PendingSendQueue(pendingSendStorage)
    val pendingSends = sendQueue.items
    private val sendWorker = Mutex()
    private val mutableUploadProgress = MutableStateFlow<Int?>(null)
    val uploadProgress = mutableUploadProgress.asStateFlow()
    private val mutableTyping = MutableStateFlow<List<JsonObject>>(emptyList())
    val typing = mutableTyping.asStateFlow()
    private val mutableRealtimeConnected = MutableStateFlow(false)
    val realtimeConnected = mutableRealtimeConnected.asStateFlow()

    suspend fun personalHistory(kind: String, after: String? = null): JsonObject {
        require(kind in setOf("matches", "ratings", "reliability", "wallet", "reservations", "support", "reports"))
        return request("/v1/history/$kind?limit=50" + (after?.let { "&after=${queryValue(it)}" } ?: ""))
    }
    suspend fun searchPlayers(query: String, sport: String? = null, city: String? = null, area: String? = null, after: String? = null): JsonObject {
        val fields = listOfNotNull("q" to query.trim(), sport?.takeIf(String::isNotBlank)?.let { "sport" to it },
            city?.takeIf(String::isNotBlank)?.let { "city" to it }, area?.takeIf(String::isNotBlank)?.let { "area" to it }, after?.let { "after" to it })
        return request("/v1/players/search?" + fields.joinToString("&") { "${it.first}=${queryValue(it.second)}" })
    }
    suspend fun sendTyping(threadId: String, value: Boolean) {
        if (authenticated) request("/v1/threads/$threadId/typing", "POST", apiPayload("typing" to value))
    }
    internal suspend fun refreshRealtime(forceBootstrap: Boolean = false, changedThreadIds: Set<String> = emptySet()) = requestMutex.withLock {
        if (!authenticated) return@withLock
        val current = mutableSnapshot.value
        if (!forceBootstrap && changedThreadIds.isNotEmpty() && changedThreadIds.size <= 8 && current != null) {
            var updated: JsonObject = current
            try {
                for (id in changedThreadIds) {
                    require(Regex("[a-fA-F0-9-]{36}").matches(id))
                    updated = mergeThreadDetail(updated, request("/v1/threads/$id"))
                }
                applySnapshot(updated)
                if (notificationsActive) refreshNotificationsLocked()
                return@withLock
            } catch (error: BackendFailure) {
                if (error.status !in listOf(403, 404)) throw error
            }
        }
        applyServerSnapshot(request("/v1/bootstrap"))
    }
    suspend fun runRealtime() = kotlinx.coroutines.coroutineScope {
        val realtimeOwner = userId
        val events = kotlinx.coroutines.channels.Channel<String>(64, kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST)
        val invalidations = kotlinx.coroutines.channels.Channel<Unit>(kotlinx.coroutines.channels.Channel.CONFLATED)
        val invalidationMutex = Mutex()
        var reconcileNeeded = true
        val changedThreads = mutableSetOf<String>()
        val refresh = launch {
            for (ignored in invalidations) {
                kotlinx.coroutines.delay(100)
                val (full, changed) = invalidationMutex.withLock {
                    val pending = reconcileNeeded to changedThreads.toSet()
                    reconcileNeeded = false
                    changedThreads.clear()
                    pending
                }
                try { refreshRealtime(full, changed) }
                catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                catch (_: Exception) {
                    invalidationMutex.withLock {
                        reconcileNeeded = reconcileNeeded || full
                        changedThreads.addAll(changed)
                    }
                }
            }
        }
        val collector = launch {
            for (frame in events) {
                if (userId != realtimeOwner) continue
                val event = runCatching { parser.parseToJsonElement(frame).jsonObject }.getOrNull() ?: continue
                when (event.s("type")) {
                    "typing" -> mutableTyping.value = mutableTyping.value.filterNot { it.s("userId") == event.s("userId") && it.s("threadId") == event.s("threadId") } + event
                    "invalidate" -> {
                        mutableRealtimeConnected.value = true
                        invalidationMutex.withLock {
                            val threadId = event.s("threadId")
                            if (Regex("[a-fA-F0-9-]{36}").matches(threadId)) changedThreads.add(threadId)
                            else reconcileNeeded = true
                        }
                        invalidations.trySend(Unit)
                    }
                    "sessionExpired" -> mutableRealtimeConnected.value = false
                }
            }
        }
        val expireTyping = launch {
            while (kotlinx.coroutines.currentCoroutineContext().isActive) {
                kotlinx.coroutines.delay(1000)
                val now = Clock.System.now().toEpochMilliseconds()
                mutableTyping.value = mutableTyping.value.filter { it.l("expiresAt") > now }
            }
        }
        var backoff = 1000L
        try {
            while (kotlinx.coroutines.currentCoroutineContext().isActive && authenticated && userId == realtimeOwner) {
                try {
                    // Refresh expired access credentials before opening a long-lived request.
                    request("/v1/account/sessions")
                    val owner = userId
                    val token = session?.s("accessToken") ?: break
                    invalidationMutex.withLock { reconcileNeeded = true }
                    observeRealtime(baseUrl.trimEnd('/') + "/v1/realtime", mapOf("Authorization" to "Bearer $token")) { frame ->
                        if (userId == owner) events.trySend(frame)
                    }
                    backoff = 1000
                } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                catch (_: Exception) { backoff = (backoff * 2).coerceAtMost(20000) }
                finally { mutableRealtimeConnected.value = false; mutableTyping.value = emptyList() }
                kotlinx.coroutines.delay(backoff)
            }
        } finally {
            events.close(); invalidations.close(); collector.cancel(); refresh.cancel(); expireTyping.cancel()
            mutableRealtimeConnected.value = false; mutableTyping.value = emptyList()
        }
    }

    suspend fun loadPendingSends() { userId?.let { sendQueue.load(it) } }
    @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
    suspend fun enqueueSend(operation: String, payload: JsonObject, bitmap: ImageBitmap? = null): String {
        require(operation in setOf("chat.send", "memory.create"))
        val owner = userId ?: throw BackendFailure("UNAUTHENTICATED", "Giriş yapmalısın.")
        val photo = if (bitmap != null) {
            mutableUploadProgress.value = -1
            try { withContext(Dispatchers.Default) {
                val bytes = encodeUploadPhoto(bitmap)
                if (bytes.size > 5 * 1024 * 1024) throw BackendFailure("IMAGE_SIZE", "Fotoğraf en fazla 5 MB olabilir.")
                kotlin.io.encoding.Base64.encode(bytes)
            } } finally { mutableUploadProgress.value = null }
        } else null
        check(userId == owner) { "Hesap değişti. Tekrar dene." }
        val threadId = payload.s("threadId").ifBlank { payload.s("shareThreadId") }
        val destination = if (operation == "memory.create") "Anı Vitrini" else mutableSnapshot.value?.a("threads")?.firstOrNull { it.s("id") == threadId }?.s("title")
        val id = sendQueue.add(owner, operation, payload, photo, destination)
        if (userId == owner) bitmap?.let { cachePhoto("pending:$id", it) }
        return id
    }
    suspend fun retrySend(id: String) { userId?.let { sendQueue.change(it, id, "state" to "queued", "attempts" to 0, "retryAt" to 0, "error" to "", "errorCode" to "") } }
    suspend fun removePendingSend(id: String) {
        if (!sendWorker.tryLock()) throw BackendFailure("SEND_IN_PROGRESS", "Gönderim sürüyor. Tamamlandıktan sonra tekrar dene.")
        try { userId?.let { sendQueue.remove(it, id) } }
        finally { sendWorker.unlock() }
    }
    suspend fun resolveCommand(operation: String, payload: JsonObject, key: String, expectedOwner: String? = userId): JsonObject = requestMutex.withLock {
        if (expectedOwner == null || userId != expectedOwner) throw BackendFailure("SESSION_CHANGED", "Hesap değişti. Tekrar giriş yap.")
        request("/v1/command-receipts/resolve", "POST", apiPayload("operation" to operation, "payload" to payload, "requestKey" to key))
    }

    @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
    suspend fun replacePendingPhoto(id: String, bitmap: ImageBitmap) {
        if (replacePendingPhotoSource(id) { encodeUploadPhoto(bitmap) } && pendingSends.value.any { it.id == id }) cachePhoto("pending:$id", bitmap)
    }

    @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
    internal suspend fun replacePendingPhotoSource(id: String, source: suspend () -> ByteArray): Boolean = sendWorker.withLock {
        val owner = userId ?: throw BackendFailure("UNAUTHENTICATED", "Giriş yapmalısın.")
        sendQueue.load(owner)
        val item = pendingSends.value.firstOrNull { it.id == id }
            ?: throw BackendFailure("SEND_NOT_PENDING", "Bu gönderim tamamlanmış veya kaldırılmış.")
        if (!item.needsPhoto) throw BackendFailure("SEND_NOT_REPLACEABLE", "Bu gönderimin fotoğrafı şu anda değiştirilemiyor.")
        if (resolveCommand(item.operation, item.payload, item.id, owner).b("found")) {
            command(item.operation, item.payload, item.id, expectedOwner = owner, expectedSendId = item.id)
            sendQueue.remove(owner, item.id)
            return@withLock false
        }
        val bytes = withContext(Dispatchers.Default) { source() }
        if (bytes.size > 5 * 1024 * 1024) throw BackendFailure("IMAGE_SIZE", "Fotoğraf en fazla 5 MB olabilir.")
        val photo = withContext(Dispatchers.Default) { kotlin.io.encoding.Base64.encode(bytes) }
        check(userId == owner) { "Hesap değişti. Tekrar dene." }
        val generation = item.record.i("uploadGeneration") + 1
        sendQueue.change(owner, id, "photo" to photo, "payload" to JsonObject(item.payload + ("uploadId" to JsonNull)),
            "uploadGeneration" to generation, "state" to "queued", "commandAttempted" to false,
            "error" to "", "errorCode" to "", "attempts" to 0, "retryAt" to 0)
        true
    }

    suspend fun processPendingSends() {
        if (sendWorker.isLocked || !authenticated) return
        sendWorker.withLock {
            val owner = userId ?: return@withLock
            sendQueue.load(owner)
            for (item in pendingSends.value) {
                if (userId != owner) return@withLock
                if (item.state == "failed" || item.record.l("retryAt") > Clock.System.now().toEpochMilliseconds()) continue
                val attempts = item.record.i("attempts") + 1
                try {
                    sendPending(owner, item, attempts)
                } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                catch (error: Exception) {
                    val retryable = error is BackendFailure && error.code !in setOf("FEATURE_UNAVAILABLE", "LEGAL_DOCUMENTS_PENDING") && (error.code == "OFFLINE" || error.status in listOf(408,429,500,502,503,504))
                    sendQueue.change(owner, item.id, "state" to if (retryable && attempts < 8) "queued" else "failed",
                        "attempts" to attempts, "error" to (error.message ?: "Gönderilemedi."),
                        "errorCode" to (error as? BackendFailure)?.code,
                        "retryAt" to (Clock.System.now().toEpochMilliseconds() + 15000))
                    if (retryable) break
                }
            }
        }
    }

    @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
    private suspend fun sendPending(owner: String, item: PendingSend, attempts: Int) {
        var payload = item.payload
        val photo = item.record.s("photo")
        var generation = item.record.i("uploadGeneration")
        var recovered = false
        // Reconcile an uncertain request before changing any previously submitted payload.
        if (item.record.b("commandAttempted") || item.record.i("attempts") > 0 && payload.s("uploadId").isNotBlank()) {
            sendQueue.change(owner, item.id, "state" to "checking", "attempts" to attempts)
            if (resolveCommand(item.operation, payload, item.id, owner).b("found")) {
                command(item.operation, payload, item.id, expectedOwner = owner, expectedSendId = item.id)
                sendQueue.remove(owner, item.id)
                return
            }
        }
        while (true) {
            check(userId == owner) { "Hesap değişti." }
            if (photo.isNotEmpty() && payload.s("uploadId").isBlank()) {
                sendQueue.change(owner, item.id, "state" to "uploading", "percent" to 0, "attempts" to attempts)
                val bytes = withContext(Dispatchers.Default) { kotlin.io.encoding.Base64.decode(photo) }
                suspend fun upload(): String {
                    val key = if (generation == 0) item.id else "${item.id}.photo.$generation"
                    return uploadBytes(bytes, key) { sent, total ->
                        if (total > 0) sendQueue.progress(owner, item.id, ((sent * 100) / total).toInt())
                    }
                }
                val uploaded = try { upload() }
                catch (failure: BackendFailure) {
                    if (failure.code != "UPLOAD_EXPIRED") throw failure
                    generation++
                    sendQueue.change(owner, item.id, "uploadGeneration" to generation)
                    upload()
                }
                check(userId == owner) { "Hesap değişti." }
                imageCache.remove("pending:${item.id}")?.let { cachePhoto(uploaded, it) }
                payload = JsonObject(payload + ("uploadId" to JsonPrimitive(uploaded)))
                // Keep the encoded source until the message/post itself is acknowledged.
                sendQueue.change(owner, item.id, "payload" to payload)
            }
            sendQueue.change(owner, item.id, "state" to "sending", "attempts" to attempts, "commandAttempted" to true)
            try {
                command(item.operation, payload, item.id, expectedOwner = owner, expectedSendId = item.id)
                sendQueue.remove(owner, item.id)
                return
            } catch (failure: BackendFailure) {
                if (failure.code != "UPLOAD_EXPIRED" || recovered) throw failure
                if (resolveCommand(item.operation, payload, item.id, owner).b("found")) {
                    command(item.operation, payload, item.id, expectedOwner = owner, expectedSendId = item.id)
                    sendQueue.remove(owner, item.id)
                    return
                }
                if (photo.isEmpty()) throw BackendFailure("UPLOAD_RECOVERY_REQUIRED", "Eski fotoğrafın kopyası cihazda yok. Fotoğrafı yeniden seçebilirsin.")
                recovered = true
                generation++
                payload = JsonObject(payload + ("uploadId" to JsonNull))
                sendQueue.change(owner, item.id, "payload" to payload, "uploadGeneration" to generation, "commandAttempted" to false)
            }
        }
    }
    private suspend fun uploadBytes(bytes: ByteArray, key: String? = null, progress: ((Long,Long)->Unit)? = null): String {
        val headers = mutableMapOf("Content-Type" to "image/jpeg")
        key?.let { headers["Idempotency-Key"] = it }
        return try {
            val data = parse(authorizedRaw("/v1/media", "POST", bytes, headers, onUploadProgress = { sent,total ->
                if (total > 0) mutableUploadProgress.value = ((sent*100)/total).toInt()
                progress?.invoke(sent,total)
            }))
            data.getValue("id").jsonPrimitive.content
        } finally { mutableUploadProgress.value = null }
    }

    private val imageCache = LinkedHashMap<String, ImageBitmap>()
    private var browseCity: String? = null
    private var discoveryPages = 0
    private var discoveryActive = true
    private var inboxActive = false
    private var inboxPages = 0
    private var activeThreadId: String? = null
    private val catalogKinds = setOf("teams", "tournaments", "communities", "venues")
    private var activeCatalogs = emptySet<String>()
    private val pinnedCatalogRecords = mutableMapOf<String, String>()
    private val catalogPages = mutableMapOf<String, Int>()
    private val baseCatalogIds = mutableMapOf<String, Set<String>>()
    private var baseCatalogCursors = JsonObject(emptyMap())
    private var baseEventIds = emptySet<String>()
    private var baseEventCursor: JsonElement = JsonNull
    val userId: String? get() = session?.get("userId")?.jsonPrimitive?.contentOrNull
    val profileHints: JsonObject get() = session?.o("profileHints") ?: JsonObject(emptyMap())
    val authenticated: Boolean get() = userId != null
    val profileVersion: Int get() = mutableSnapshot.value?.get("profile")?.takeUnless { it is JsonNull }?.jsonObject?.get("version")?.jsonPrimitive?.intOrNull ?: 0
    private fun saveSession(value: JsonObject?) {
        val changedUser = session?.s("userId") != value?.s("userId")
        persistence.write(value?.toString()); session = value
        if (value == null || changedUser) {
            sendQueue.hide(); mutableTyping.value = emptyList()
            inboxPages = 0; activeThreadId = null
            imageCache.clear(); messageHistory.clear(); discoveryPages = 0; baseEventIds = emptySet(); baseEventCursor = JsonNull; mutableSnapshot.value = null
            mutableMemoryFeed.value = null; memoryFeedQuery = null; memoryFeedPages = 0
            mutableNotificationFeed.value = null; notificationsActive = false; notificationPages = 0; notificationFilter = "all"
            catalogPages.clear(); baseCatalogIds.clear(); baseCatalogCursors = JsonObject(emptyMap())
            pinnedCatalogRecords.clear()
        }
    }

    private suspend fun raw(path: String, method: String, body: ByteArray?, headers: Map<String, String>, onUploadProgress: ((Long,Long)->Unit)? = null): ApiHttpResponse {
        if (baseUrl.isBlank()) throw BackendFailure("CONFIGURATION_REQUIRED", "API adresi yapılandırılmadı.")
        return try { transport(ApiHttpRequest(baseUrl.trimEnd('/') + path, method, headers, body, onUploadProgress)) }
        catch (error: kotlinx.coroutines.CancellationException) { throw error }
        catch (error: Exception) { throw BackendFailure("OFFLINE", "Sunucuya ulaşılamadı. Bağlantını kontrol edip tekrar dene.") }
    }
    private suspend fun parse(response: ApiHttpResponse): JsonObject {
        val data = if (response.bytes.size >= 8192) {
            withContext(Dispatchers.Default) { runCatching { parser.parseToJsonElement(response.text).jsonObject }.getOrNull() }
        } else runCatching { parser.parseToJsonElement(response.text).jsonObject }.getOrNull()
        if (response.status !in 200..299) {
            val error = data?.get("error")?.jsonObject
            throw BackendFailure(error?.get("code")?.jsonPrimitive?.content ?: "SERVER_ERROR", error?.get("message")?.jsonPrimitive?.content ?: "İşlem tamamlanamadı.", response.status)
        }
        return data ?: throw BackendFailure("INVALID_RESPONSE", "Sunucu yanıtı okunamadı.")
    }
    private suspend fun authorizedRaw(path: String, method: String, body: ByteArray?, extraHeaders: Map<String, String>, authenticatedRequest: Boolean = true, onUploadProgress: ((Long,Long)->Unit)? = null): ApiHttpResponse {
        val headers = extraHeaders.toMutableMap()
        val requestOwner = userId
        val usedAccess = if (authenticatedRequest) session?.get("accessToken")?.jsonPrimitive?.content else null
        usedAccess?.let { headers["Authorization"] = "Bearer $it" }
        var response = raw(path, method, body, headers, onUploadProgress)
        if (response.status == 401 && usedAccess != null) {
            refreshMutex.withLock {
                if (userId != requestOwner) throw BackendFailure("SESSION_CHANGED", "Hesap değişti. Tekrar dene.")
                if (session?.get("accessToken")?.jsonPrimitive?.content == usedAccess) {
                    val refresh = session?.get("refreshToken")?.jsonPrimitive?.content ?: throw BackendFailure("UNAUTHENTICATED", "Tekrar giriş yapmalısın.")
                    try { saveSession(parse(raw("/v1/auth/refresh", "POST", apiPayload("refreshToken" to refresh).toString().encodeToByteArray(), mapOf("Content-Type" to "application/json")))) }
                    catch (error: BackendFailure) { if (error.status == 401) saveSession(null); throw error }
                }
            }
            if (userId != requestOwner) throw BackendFailure("SESSION_CHANGED", "Hesap değişti. Tekrar dene.")
            session?.get("accessToken")?.jsonPrimitive?.content?.let { headers["Authorization"] = "Bearer $it" }
            response = raw(path, method, body, headers, onUploadProgress)
        }
        return response
    }
    private suspend fun request(path: String, method: String = "GET", payload: JsonObject? = null, requestKey: String? = null, authenticatedRequest: Boolean = true): JsonObject {
        val headers = mutableMapOf("Content-Type" to "application/json")
        requestKey?.let { headers["Idempotency-Key"] = it }
        if (path.startsWith("/v1/commands/") && userId != null && mutableSnapshot.value?.s("userId") == userId) {
            headers["Prefer"] = "return=changes"
        }
        val scoped = if (path == "/v1/bootstrap" || path.startsWith("/v1/commands/") || path.startsWith("/v1/events?") || path == "/v1/auth/contact/verify") {
            browseCity?.let { path + (if ('?' in path) "&" else "?") + "city=" + queryValue(it) } ?: path
        } else path
        return parse(authorizedRaw(scoped, method, payload?.toString()?.encodeToByteArray(), headers, authenticatedRequest))
    }
    private fun queryValue(value: String): String = buildString {
        value.encodeToByteArray().forEach { byte ->
            val n = byte.toInt() and 255
            if (n in 65..90 || n in 97..122 || n in 48..57 || n in listOf(45, 46, 95, 126)) append(n.toChar())
            else { append('%'); append("0123456789ABCDEF"[n / 16]); append("0123456789ABCDEF"[n % 16]) }
        }
    }
    private suspend fun applyServerSnapshot(base: JsonObject) {
        for (kind in catalogKinds) baseCatalogIds[kind] = base.a(kind).map { it.s("id") }.toSet()
        baseCatalogCursors = base.o("nextCatalogCursors")
        baseEventIds = base.a("events").map { it.s("id") }.toSet()
        baseEventCursor = base["nextEventCursor"] ?: JsonNull
        // Always remove old pages first: access to a private community may have changed.
        applySnapshot(base)
        var updated = base
        var loaded = 0
        if (discoveryActive) {
            try {
                repeat(discoveryPages) {
                    val cursor = updated.s("nextEventCursor")
                    if (cursor.isBlank()) return@repeat
                    val page = request("/v1/events?after=${queryValue(cursor)}&limit=100")
                    updated = mergeEventPage(updated, page); loaded++
                }
            } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (_: BackendFailure) { /* The base and committed command remain valid; pages can be retried. */ }
        }
        discoveryPages = loaded
        for (kind in activeCatalogs) {
            var pages = 0
            try {
                repeat(catalogPages[kind] ?: 0) {
                    val cursor = updated.o("nextCatalogCursors").s(kind)
                    if (cursor.isNotBlank()) {
                        val cityQuery = if (kind == "venues") browseCity?.let { "&city=${queryValue(it)}" }.orEmpty() else ""
                        updated = mergeCatalogPage(updated, kind, request("/v1/catalog/$kind?after=${queryValue(cursor)}&limit=100$cityQuery")); pages++
                    }
                }
            } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (_: BackendFailure) { /* Keep fresh authorized pages; the load-more button retries the remaining cursor. */ }
            catalogPages[kind] = pages
            pinnedCatalogRecords[kind]?.takeIf { id -> updated.a(kind).none { it.s("id") == id } }?.let { id ->
                try { updated = mergeCatalogDetail(updated, kind, request("/v1/catalog/$kind/$id")) }
                catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                catch (_: BackendFailure) { pinnedCatalogRecords.remove(kind) }
            }
        }
        var loadedInboxPages = 0
        if (inboxActive && authenticated) {
            try {
                repeat(inboxPages) {
                    val cursor = updated.s("nextThreadCursor")
                    if (cursor.isNotBlank()) {
                        updated = mergeThreadPage(updated, request("/v1/threads?after=${queryValue(cursor)}&limit=100")); loadedInboxPages++
                    }
                }
            } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (_: BackendFailure) { /* Fresh pages remain usable; never restore stale message access. */ }
        }
        inboxPages = loadedInboxPages
        activeThreadId?.takeIf { authenticated && updated.a("threads").none { thread -> thread.s("id") == it } }?.let { id ->
            try { updated = mergeThreadDetail(updated, request("/v1/threads/$id")) }
            catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (_: BackendFailure) { messageHistory.remove(id) }
        }
        if (updated !== base) applySnapshot(updated)
        if (notificationsActive) {
            try { refreshNotificationsLocked() }
            catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (_: BackendFailure) { mutableNotificationFeed.value = null }
        }
    }
    private fun mergeEventPage(snapshot: JsonObject, page: JsonObject): JsonObject {
        val events = (snapshot.a("events") + page.a("items")).associateBy { it.s("id") }.values.toList()
            .sortedWith(compareBy<JsonObject> { it.l("startsAtEpochMillis") }.thenBy { it.s("id") })
        return JsonObject(snapshot + ("events" to JsonArray(events)) + ("nextEventCursor" to (page["nextCursor"] ?: JsonNull)))
    }
    suspend fun setBrowseCity(city: String) = requestMutex.withLock {
        val normalized = city.trim().ifBlank { null }
        if (browseCity == normalized) return@withLock
        val previous = browseCity
        browseCity = normalized; discoveryPages = 0
        try { applyServerSnapshot(request("/v1/bootstrap")) }
        catch (failure: Exception) { browseCity = previous; throw failure }
    }
    suspend fun setDiscoveryActive(active: Boolean) = requestMutex.withLock {
        discoveryActive = active
        if (!active && discoveryPages > 0) {
            discoveryPages = 0
            mutableSnapshot.value?.let { snapshot ->
                applySnapshot(JsonObject(snapshot + ("events" to JsonArray(snapshot.a("events").filter { it.s("id") in baseEventIds })) + ("nextEventCursor" to baseEventCursor)))
            }
        }
    }
    suspend fun loadMoreEvents() = requestMutex.withLock {
        val snapshot = mutableSnapshot.value ?: return@withLock
        val cursor = snapshot.s("nextEventCursor").ifBlank { return@withLock }
        val page = request("/v1/events?after=${queryValue(cursor)}&limit=100")
        applySnapshot(mergeEventPage(snapshot, page)); discoveryPages++
    }
    private fun mergeCatalogPage(snapshot: JsonObject, kind: String, page: JsonObject): JsonObject {
        val items = (snapshot.a(kind) + page.a("items")).associateBy { it.s("id") }.values.toList()
        val players = (snapshot.a("players") + page.a("players")).associateBy { it.s("id") }.values.toList()
        return JsonObject(snapshot + (kind to JsonArray(items)) + ("players" to JsonArray(players)) +
            ("nextCatalogCursors" to JsonObject(snapshot.o("nextCatalogCursors") + (kind to (page["nextCursor"] ?: JsonNull)))))
    }
    suspend fun setActiveCatalogs(kinds: Set<String>) = requestMutex.withLock {
        require(catalogKinds.containsAll(kinds))
        val dropped = activeCatalogs - kinds
        activeCatalogs = kinds
        var snapshot = mutableSnapshot.value
        for (kind in dropped) {
            val hadPinned = pinnedCatalogRecords.remove(kind) != null
            if (((catalogPages.remove(kind) ?: 0) > 0 || hadPinned) && snapshot != null) snapshot = JsonObject(snapshot +
                (kind to JsonArray(snapshot.a(kind).filter { it.s("id") in baseCatalogIds[kind].orEmpty() })) +
                ("nextCatalogCursors" to JsonObject(snapshot.o("nextCatalogCursors") + (kind to (baseCatalogCursors[kind] ?: JsonNull)))))
        }
        snapshot?.let(::applySnapshot)
    }
    suspend fun loadMoreCatalog(kind: String) = requestMutex.withLock {
        require(kind in catalogKinds)
        val snapshot = mutableSnapshot.value ?: return@withLock
        val cursor = snapshot.o("nextCatalogCursors").s(kind).ifBlank { return@withLock }
        val cityQuery = if (kind == "venues") browseCity?.let { "&city=${queryValue(it)}" }.orEmpty() else ""
        val page = request("/v1/catalog/$kind?after=${queryValue(cursor)}&limit=100$cityQuery")
        activeCatalogs = activeCatalogs + kind
        applySnapshot(mergeCatalogPage(snapshot, kind, page)); catalogPages[kind] = (catalogPages[kind] ?: 0) + 1
    }
    private fun mergeCatalogDetail(snapshot: JsonObject, kind: String, detail: JsonObject): JsonObject {
        val merged = mergeCatalogPage(snapshot, kind, apiPayload("items" to listOf(detail.o("item")), "players" to detail.a("players")))
        return JsonObject(merged + ("nextCatalogCursors" to snapshot.o("nextCatalogCursors")))
    }
    suspend fun includeCatalogItem(kind: String, id: String) = requestMutex.withLock {
        require(kind in catalogKinds && Regex("[a-fA-F0-9-]{36}").matches(id))
        val detail = request("/v1/catalog/$kind/$id")
        pinnedCatalogRecords[kind] = id
        applySnapshot(mergeCatalogDetail(mutableSnapshot.value ?: apiPayload(), kind, detail))
    }
    private fun mergeThreadPage(snapshot: JsonObject, page: JsonObject): JsonObject {
        val threads = (snapshot.a("threads") + page.a("items")).associateBy { it.s("id") }.values.toList()
            .sortedWith(compareByDescending<JsonObject> { it.l("activityAt") }.thenByDescending { it.s("id") })
        val players = (snapshot.a("players") + page.a("players")).associateBy { it.s("id") }.values.toList()
        return JsonObject(snapshot + ("threads" to JsonArray(threads)) + ("players" to JsonArray(players)) +
            ("blockedThreadIds" to jsonValue(threads.filter { it.b("locked") && it.s("type") == "DIRECT" }.map { it.s("id") })) +
            ("unreadThreadCount" to (page["unreadThreadCount"] ?: snapshot["unreadThreadCount"] ?: JsonPrimitive(0))) +
            ("nextThreadCursor" to (page["nextCursor"] ?: JsonNull)))
    }
    private fun mergeThreadDetail(snapshot: JsonObject, detail: JsonObject): JsonObject {
        val counts = detail.filterKeys { it in setOf("unreadThreadCount", "unreadNotificationCount") }
        val page = JsonObject(apiPayload("items" to listOf(detail.o("item")), "players" to detail.a("players")) + counts)
        val merged = mergeThreadPage(snapshot, page)
        return JsonObject(merged + counts + ("nextThreadCursor" to (snapshot["nextThreadCursor"] ?: JsonNull)))
    }
    suspend fun setInboxActive(active: Boolean) = requestMutex.withLock { inboxActive = active; if (!active) inboxPages = 0 }
    suspend fun loadMoreThreads() = requestMutex.withLock {
        val snapshot = mutableSnapshot.value ?: return@withLock
        val cursor = snapshot.s("nextThreadCursor").ifBlank { return@withLock }
        val page = request("/v1/threads?after=${queryValue(cursor)}&limit=100")
        inboxActive = true; inboxPages++; applySnapshot(mergeThreadPage(snapshot, page))
    }
    suspend fun setActiveThread(id: String?) = requestMutex.withLock {
        require(id == null || Regex("[a-fA-F0-9-]{36}").matches(id))
        activeThreadId = id
        if (id != null && mutableSnapshot.value?.a("threads")?.none { it.s("id") == id } != false) {
            val detail = request("/v1/threads/$id")
            applySnapshot(mergeThreadDetail(mutableSnapshot.value ?: apiPayload(), detail))
        }
    }
    private fun applySnapshot(value: JsonObject) {
        val present = value.a("threads").map { it.s("id") }.toSet()
        messageHistory.keys.retainAll(present)
        val threads = value.a("threads").map { thread ->
            val id = thread.s("id")
            if (messageHistory[id]?.s("historyRevision") != thread.s("historyRevision")) messageHistory.remove(id)
            if (thread.b("locked")) { messageHistory.remove(id); thread }
            else messageHistory[id]?.let { history ->
                val messages = (history.a("messages") + thread.a("messages")).associateBy { it.s("id") }.values.toList()
                    .sortedWith(compareBy<JsonObject> { it.l("sequence").takeIf { n -> n > 0 } ?: it.l("createdAt") }.thenBy { it.s("id") })
                val merged = JsonObject(thread + ("messages" to JsonArray(messages)) + ("nextMessageCursor" to (history["nextMessageCursor"] ?: JsonNull)))
                messageHistory[id] = merged; merged
            } ?: thread
        }
        mutableSnapshot.value = JsonObject(value + ("threads" to JsonArray(threads)))
    }
    fun canLoadOlderMessages(threadId: String): Boolean = mutableSnapshot.value?.a("threads")?.find { it.s("id") == threadId }?.s("nextMessageCursor")?.isNotBlank() == true
    suspend fun loadOlderMessages(threadId: String) = requestMutex.withLock {
        require(Regex("[a-fA-F0-9-]{36}").matches(threadId))
        val snapshot = mutableSnapshot.value ?: return@withLock
        val thread = snapshot.a("threads").find { it.s("id") == threadId } ?: return@withLock
        val cursor = thread.s("nextMessageCursor").ifBlank { return@withLock }
        val page = request("/v1/threads/$threadId/messages?before=$cursor&limit=50")
        val messages = (page.a("items") + thread.a("messages")).distinctBy { it.s("id") }
        messageHistory[threadId] = JsonObject(thread + ("messages" to JsonArray(messages)) + ("nextMessageCursor" to (page["nextCursor"] ?: JsonNull)))
        applySnapshot(snapshot)
    }
    suspend fun start() = requestMutex.withLock {
        mutableConfiguration.value = request("/v1/config", authenticatedRequest = false)
        applyServerSnapshot(request("/v1/bootstrap"))
    }
    suspend fun reload() = requestMutex.withLock { mutableConfiguration.value = request("/v1/config", authenticatedRequest = false); applyServerSnapshot(request("/v1/bootstrap")) }
    private var lastFullPoll = kotlin.time.TimeSource.Monotonic.markNow()

    suspend fun poll() {
        if (pendingCommands > 0 || requestMutex.isLocked) return
        requestMutex.withLock {
            if (pendingCommands > 0) return@withLock
            val threadId = activeThreadId
            val current = mutableSnapshot.value
            if (threadId != null && current != null && authenticated && lastFullPoll.elapsedNow().inWholeSeconds < 20) {
                try {
                    val detail = request("/v1/threads/$threadId")
                    applySnapshot(mergeThreadDetail(current, detail))
                    return@withLock
                } catch (error: BackendFailure) {
                    if (error.status !in listOf(403, 404)) throw error
                    // Membership or visibility changed. Reconcile against the authorized inbox.
                }
            }
            if (mutableConfiguration.value == null) {
                mutableConfiguration.value = request("/v1/config", authenticatedRequest = false)
            }
            applyServerSnapshot(request("/v1/bootstrap"))
            lastFullPoll = kotlin.time.TimeSource.Monotonic.markNow()
        }
    }
    suspend fun refreshConfiguration() = requestMutex.withLock {
        mutableConfiguration.value = request("/v1/config", authenticatedRequest = false)
    }
    suspend fun selectMemoryFeed(mode: String, city: String, district: String, latitude: Double?, longitude: Double?, seed: String) = requestMutex.withLock {
        val fields = listOf("mode" to mode, "city" to city, "district" to district, "seed" to seed) +
            if (latitude != null && longitude != null) listOf("latitude" to latitude.toString(), "longitude" to longitude.toString()) else emptyList()
        val query = "/v1/memories?" + fields.joinToString("&") { (key, value) -> "$key=${queryValue(value)}" }
        if (query != memoryFeedQuery) { memoryFeedPages = 0; mutableMemoryFeed.value = null }
        memoryFeedQuery = query
        refreshMemoryFeedLocked()
    }
    private suspend fun refreshMemoryFeedLocked() {
        val query = memoryFeedQuery ?: return
        var feed = request(query)
        val pages = memoryFeedPages
        memoryFeedPages = 0
        repeat(pages) {
            val cursor = feed.s("nextCursor")
            if (cursor.isNotBlank()) {
                val page = request(query + "&after=" + queryValue(cursor))
                feed = JsonObject(page + ("items" to JsonArray((feed.a("items") + page.a("items")).associateBy { it.s("id") }.values.toList())))
                memoryFeedPages++
            }
        }
        mutableMemoryFeed.value = feed
    }
    suspend fun refreshMemoryFeed() = requestMutex.withLock { refreshMemoryFeedLocked() }
    suspend fun loadMoreMemories() = requestMutex.withLock {
        val feed = mutableMemoryFeed.value ?: return@withLock
        val cursor = feed.s("nextCursor").ifBlank { return@withLock }
        val query = memoryFeedQuery ?: return@withLock
        val page = request(query + "&after=" + queryValue(cursor))
        mutableMemoryFeed.value = JsonObject(page + ("items" to JsonArray((feed.a("items") + page.a("items")).associateBy { it.s("id") }.values.toList())))
        memoryFeedPages++
    }
    suspend fun rankings(sport: String, scope: String, city: String, district: String, after: String? = null): JsonObject =
        request("/v1/rankings?" + listOf("sport" to sport, "scope" to scope, "city" to city, "district" to district).joinToString("&") { (key, value) -> "$key=${queryValue(value)}" } + (after?.let { "&after=${queryValue(it)}" } ?: ""))
    suspend fun selectNotifications(filter: String) = requestMutex.withLock {
        require(filter in setOf("all", "matches", "squads", "wallet"))
        if (notificationFilter != filter || !notificationsActive) { notificationPages = 0; mutableNotificationFeed.value = null }
        notificationFilter = filter; notificationsActive = true
        refreshNotificationsLocked()
    }
    fun closeNotifications() { notificationsActive = false; notificationPages = 0; mutableNotificationFeed.value = null }
    private suspend fun refreshNotificationsLocked() {
        if (!notificationsActive) return
        val selected = notificationFilter
        var feed = request("/v1/notifications?filter=$selected&limit=50")
        val pages = notificationPages
        var loaded = 0
        repeat(pages) {
            val cursor = feed.s("nextCursor")
            if (cursor.isNotBlank()) {
                val page = request("/v1/notifications?filter=$selected&limit=50&after=${queryValue(cursor)}")
                feed = JsonObject(page + ("items" to JsonArray((feed.a("items") + page.a("items")).distinctBy { it.s("id") })))
                loaded++
            }
        }
        if (notificationsActive && notificationFilter == selected) { notificationPages = loaded; mutableNotificationFeed.value = feed }
    }
    suspend fun loadMoreNotifications() = requestMutex.withLock {
        if (!notificationsActive) return@withLock
        val selected = notificationFilter
        val feed = mutableNotificationFeed.value ?: return@withLock
        val cursor = feed.s("nextCursor").ifBlank { return@withLock }
        val page = request("/v1/notifications?filter=$selected&limit=50&after=${queryValue(cursor)}")
        if (notificationsActive && notificationFilter == selected) {
            mutableNotificationFeed.value = JsonObject(page + ("items" to JsonArray((feed.a("items") + page.a("items")).distinctBy { it.s("id") })))
            notificationPages++
        }
    }
    suspend fun playerDetails(playerId: String, sport: String): JsonObject {
        require(Regex("[a-fA-F0-9-]{36}").matches(playerId))
        require(Regex("[A-Z_0-9]+").matches(sport))
        return request("/v1/players/$playerId?sport=$sport")
    }
    suspend fun acceptCurrentLegal() {
        val documents = configuration?.o("documents") ?: throw BackendFailure("LEGAL_DOCUMENTS_PENDING", "Belgeler henüz paylaşılmadı.")
        command("legal.accept", apiPayload("termsVersion" to documents.o("terms").s("version"), "privacyVersion" to documents.o("privacy").s("version")), requestKey("legal"))
    }
    suspend fun developmentLogin(identity: String = "mobile-local") = requestMutex.withLock {
        check(mode == "local")
        saveSession(request("/v1/auth/dev", "POST", apiPayload("identity" to identity), authenticatedRequest = false))
        applyServerSnapshot(request("/v1/bootstrap"))
    }
    /** Native provider SDK returns a provider access token; never accepts a user ID from the UI. */
    suspend fun exchangeProviderToken(providerToken: String) = requestMutex.withLock {
        saveSession(request("/v1/auth/exchange", "POST", apiPayload("providerToken" to providerToken), authenticatedRequest = false))
        applyServerSnapshot(request("/v1/bootstrap"))
    }
    suspend fun exchangeIdToken(provider: String, idToken: String, fullName: String? = null, nonce: String? = null) = requestMutex.withLock {
        saveSession(request("/v1/auth/id-token", "POST", apiPayload("provider" to provider.lowercase(), "idToken" to idToken, "fullName" to fullName, "nonce" to nonce), authenticatedRequest = false))
        applyServerSnapshot(request("/v1/bootstrap"))
    }
    suspend fun socialLogin(provider: String, browserAuth: suspend (String) -> String = ::authenticateInBrowser) {
        val flow = request("/v1/auth/oauth/start", "POST", apiPayload("provider" to provider.lowercase()), authenticatedRequest = false)
        val callback = browserAuth(flow.s("url"))
        val values = callbackParameters(callback)
        if (!callback.startsWith("matchora://auth/callback?") || values["flow"] != flow.s("flowId"))
            throw BackendFailure("OAUTH_STATE", "Giriş yanıtı bu isteğe ait değil. Tekrar dene.")
        if (values["error"] != null || values["code"].isNullOrBlank())
            throw BackendFailure("AUTH_CANCELLED", "Giriş onaylanmadı. Tekrar deneyebilirsin.")
        requestMutex.withLock {
            saveSession(request("/v1/auth/oauth/finish", "POST", apiPayload("flowId" to flow.s("flowId"), "clientSecret" to flow.s("clientSecret"), "code" to values.getValue("code")), authenticatedRequest = false))
            applyServerSnapshot(request("/v1/bootstrap"))
        }
    }
    suspend fun startPhoneOtp(phone: String): JsonObject = request("/v1/auth/otp/start", "POST", apiPayload("phone" to phone), authenticatedRequest = false)
    suspend fun verifyPassport(code: String): JsonObject =
        request("/v1/passports/verify?code=${queryValue(code.trim())}", authenticatedRequest = false)
    suspend fun adminInbox(query: String = ""): JsonObject = request("/v1/admin/inbox" + (query.trim().takeIf { it.isNotEmpty() }?.let { "?q=${queryValue(it)}" } ?: ""))
    suspend fun operatorVenues(): JsonObject = request("/v1/operator/venues")
    suspend fun venueReviews(venueId: String, after: String? = null): JsonObject {
        require(Regex("[a-fA-F0-9-]{36}").matches(venueId))
        return request("/v1/venues/$venueId/reviews" + (after?.let { "?after=${queryValue(it)}" } ?: ""))
    }
    suspend fun exportAccount(): JsonObject = request("/v1/account/export")
    suspend fun eventDetails(eventId: String): JsonObject {
        require(Regex("[a-fA-F0-9-]{36}").matches(eventId))
        return request("/v1/events/$eventId")
    }
    suspend fun activeSessions(): JsonObject = request("/v1/account/sessions")
    suspend fun verifyPhoneOtp(challengeId: String, code: String) = requestMutex.withLock {
        saveSession(request("/v1/auth/otp/verify", "POST", apiPayload("challengeId" to challengeId, "code" to code), authenticatedRequest = false))
        applyServerSnapshot(request("/v1/bootstrap"))
    }
    suspend fun startContactOtp(phone: String): JsonObject = request("/v1/auth/contact/start", "POST", apiPayload("phone" to phone))
    suspend fun verifyContactOtp(challengeId: String, code: String) = requestMutex.withLock {
        val result = request("/v1/auth/contact/verify", "POST", apiPayload("challengeId" to challengeId, "code" to code))
        applyServerSnapshot(result.getValue("snapshot").jsonObject)
    }
    suspend fun verifyContactWithIdToken(idToken: String) = requestMutex.withLock {
        val result = request("/v1/auth/contact/verify-id-token", "POST", apiPayload("idToken" to idToken))
        applyServerSnapshot(result.getValue("snapshot").jsonObject)
    }
    suspend fun command(operation: String, payload: JsonObject, requestKey: String, expectedOwner: String? = null, expectedSendId: String? = null): JsonObject {
        pendingCommands++
        return try {
            requestMutex.withLock {
                if (expectedOwner != null && userId != expectedOwner) throw BackendFailure("SESSION_CHANGED", "Hesap değişti. Tekrar dene.")
                if (expectedSendId != null && pendingSends.value.none { it.id == expectedSendId }) {
                    throw BackendFailure("SEND_CANCELLED", "Bekleyen gönderim kaldırıldı.")
                }
                val commandOwner = userId
                val response = request("/v1/commands/$operation", "POST", payload, requestKey)
                if (commandOwner != null) {
                    when (operation) {
                        "chat.delete" -> sendQueue.discard(commandOwner, payload.s("threadId"))
                        "account.delete" -> sendQueue.discard(commandOwner)
                    }
                }
                val changes = response["changes"] as? JsonObject
                if (changes == null) {
                    applyServerSnapshot(response.getValue("snapshot").jsonObject)
                } else {
                    val current = mutableSnapshot.value
                    check(current != null && current.s("userId") == changes.s("userId") && userId == changes.s("userId"))
                    var updated = JsonObject(current + changes.o("fields"))
                    (changes["event"] as? JsonObject)?.let { event ->
                        val id = event.s("id")
                        val events = (updated.a("events").filterNot { it.s("id") == id } + event)
                            .sortedWith(compareBy<JsonObject> { it.l("startsAtEpochMillis") }.thenBy { it.s("id") })
                        baseEventIds = baseEventIds + id
                        val joined = (updated["joinedEventIds"] as? JsonArray ?: JsonArray(emptyList())) + JsonPrimitive(id)
                        updated = JsonObject(updated + ("events" to JsonArray(events)) + ("joinedEventIds" to JsonArray(joined.distinct())))
                    }
                    (changes["thread"] as? JsonObject)?.let { updated = mergeThreadDetail(updated, it) }
                    applySnapshot(updated)
                    if (notificationsActive && "notifications" in changes.o("fields")) {
                        try { refreshNotificationsLocked() } catch (_: BackendFailure) { mutableNotificationFeed.value = null }
                    }
                }
                if (operation.startsWith("memory.") || operation == "user.block") {
                    try { refreshMemoryFeedLocked() }
                    catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                    catch (_: BackendFailure) { mutableMemoryFeed.value = null }
                }
                response["result"]?.jsonObject ?: JsonObject(emptyMap())
            }
        } finally {
            pendingCommands--
        }
    }
    suspend fun logout() = requestMutex.withLock {
        try {
            DeviceEventBridge.devices.value?.let { runCatching { request("/v1/commands/device.remove", "POST", apiPayload("token" to it.token), requestKey("remove-device")) } }
            request("/v1/auth/logout", "POST", apiPayload())
        }
        finally { StoreBilling.clearProducts(); saveSession(null); imageCache.clear(); mutableSnapshot.value = null }
    }
    private fun cachePhoto(uploadId: String, bitmap: ImageBitmap) {
        imageCache.remove(uploadId)
        val pixelBudget = 8L * 1024 * 1024
        val pixels = bitmap.width.toLong() * bitmap.height
        if (pixels > pixelBudget) return
        var occupied = imageCache.values.sumOf { it.width.toLong() * it.height }
        while (imageCache.isNotEmpty() && (occupied + pixels > pixelBudget || imageCache.size >= 72)) {
            val oldest = imageCache.remove(imageCache.keys.first()) ?: break
            occupied -= oldest.width.toLong() * oldest.height
        }
        imageCache[uploadId] = bitmap
    }
    suspend fun uploadPhoto(bitmap: ImageBitmap): String {
        mutableUploadProgress.value = -1
        try {
            val bytes = withContext(Dispatchers.Default) { encodeUploadPhoto(bitmap) }
            if (bytes.size > 5 * 1024 * 1024) throw BackendFailure("IMAGE_SIZE", "Fotoğraf en fazla 5 MB olabilir.")
            if (!authenticated) throw BackendFailure("UNAUTHENTICATED", "Giriş yapmalısın.")
            val uploadId = uploadBytes(bytes)
            cachePhoto(uploadId, bitmap)
            return uploadId
        } finally { mutableUploadProgress.value = null }
    }
    suspend fun photo(uploadId: String): ImageBitmap {
        imageCache.remove(uploadId)?.let { cached ->
            imageCache[uploadId] = cached
            return cached
        }
        require(Regex("[a-f0-9-]{36}").matches(uploadId))
        val headers = session?.get("accessToken")?.jsonPrimitive?.content?.let { mapOf("Authorization" to "Bearer $it") } ?: emptyMap()
        val response = authorizedRaw("/v1/media/$uploadId", "GET", null, headers)
        if (response.status !in 200..299) throw BackendFailure("PHOTO_UNAVAILABLE", "Fotoğraf yüklenemedi.")
        val bitmap = withContext(Dispatchers.Default) { decodeApiPhoto(response.bytes) }
        cachePhoto(uploadId, bitmap); return bitmap
    }
    fun serverUserId(uiId: String): String = if (uiId == com.matchora.app.state.DemoData.ME) userId ?: uiId else uiId
    fun requestKey(prefix: String = "mobile"): String = "$prefix-${Clock.System.now().toEpochMilliseconds()}-${kotlin.random.Random.nextLong().toString().replace('-', 'n')}"
}
