package com.matchora.app.data

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val persistence: SessionPersistence = NativeSessionPersistence) {
    val enabled: Boolean get() = mode != "preview"
    private val parser = Json { ignoreUnknownKeys = true }
    private val requestMutex = Mutex()
    private val refreshMutex = Mutex()
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
            inboxPages = 0; activeThreadId = null
            imageCache.clear(); messageHistory.clear(); discoveryPages = 0; baseEventIds = emptySet(); baseEventCursor = JsonNull; mutableSnapshot.value = null
            mutableMemoryFeed.value = null; memoryFeedQuery = null; memoryFeedPages = 0
            mutableNotificationFeed.value = null; notificationsActive = false; notificationPages = 0; notificationFilter = "all"
            catalogPages.clear(); baseCatalogIds.clear(); baseCatalogCursors = JsonObject(emptyMap())
            pinnedCatalogRecords.clear()
        }
    }

    private suspend fun raw(path: String, method: String, body: ByteArray?, headers: Map<String, String>): ApiHttpResponse {
        if (baseUrl.isBlank()) throw BackendFailure("CONFIGURATION_REQUIRED", "API adresi yapılandırılmadı.")
        return try { transport(ApiHttpRequest(baseUrl.trimEnd('/') + path, method, headers, body)) }
        catch (error: kotlinx.coroutines.CancellationException) { throw error }
        catch (error: Exception) { throw BackendFailure("OFFLINE", "Sunucuya ulaşılamadı. Bağlantını kontrol edip tekrar dene.") }
    }
    private fun parse(response: ApiHttpResponse): JsonObject {
        val data = runCatching { parser.parseToJsonElement(response.text).jsonObject }.getOrNull()
        if (response.status !in 200..299) {
            val error = data?.get("error")?.jsonObject
            throw BackendFailure(error?.get("code")?.jsonPrimitive?.content ?: "SERVER_ERROR", error?.get("message")?.jsonPrimitive?.content ?: "İşlem tamamlanamadı.", response.status)
        }
        return data ?: throw BackendFailure("INVALID_RESPONSE", "Sunucu yanıtı okunamadı.")
    }
    private suspend fun authorizedRaw(path: String, method: String, body: ByteArray?, extraHeaders: Map<String, String>, authenticatedRequest: Boolean = true): ApiHttpResponse {
        val headers = extraHeaders.toMutableMap()
        val usedAccess = if (authenticatedRequest) session?.get("accessToken")?.jsonPrimitive?.content else null
        usedAccess?.let { headers["Authorization"] = "Bearer $it" }
        var response = raw(path, method, body, headers)
        if (response.status == 401 && usedAccess != null) {
            refreshMutex.withLock {
                if (session?.get("accessToken")?.jsonPrimitive?.content == usedAccess) {
                    val refresh = session?.get("refreshToken")?.jsonPrimitive?.content ?: throw BackendFailure("UNAUTHENTICATED", "Tekrar giriş yapmalısın.")
                    try { saveSession(parse(raw("/v1/auth/refresh", "POST", apiPayload("refreshToken" to refresh).toString().encodeToByteArray(), mapOf("Content-Type" to "application/json")))) }
                    catch (error: BackendFailure) { if (error.status == 401) saveSession(null); throw error }
                }
            }
            session?.get("accessToken")?.jsonPrimitive?.content?.let { headers["Authorization"] = "Bearer $it" }
            response = raw(path, method, body, headers)
        }
        return response
    }
    private suspend fun request(path: String, method: String = "GET", payload: JsonObject? = null, requestKey: String? = null, authenticatedRequest: Boolean = true): JsonObject {
        val headers = mutableMapOf("Content-Type" to "application/json")
        requestKey?.let { headers["Idempotency-Key"] = it }
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
                applySnapshot(updated)
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
                        updated = mergeCatalogPage(updated, kind, request("/v1/catalog/$kind?after=${queryValue(cursor)}&limit=100")); pages++
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
        applySnapshot(updated)
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
        val cityQuery = if (kind in listOf("venues", "communities")) browseCity?.let { "&city=${queryValue(it)}" }.orEmpty() else ""
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
        val merged = mergeThreadPage(snapshot, apiPayload("items" to listOf(detail.o("item")), "players" to detail.a("players")))
        return JsonObject(merged + ("nextThreadCursor" to (snapshot["nextThreadCursor"] ?: JsonNull)))
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
    suspend fun exchangeIdToken(provider: String, idToken: String, fullName: String? = null) = requestMutex.withLock {
        saveSession(request("/v1/auth/id-token", "POST", apiPayload("provider" to provider.lowercase(), "idToken" to idToken, "fullName" to fullName), authenticatedRequest = false))
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
    suspend fun startEmailOtp(email: String): JsonObject = request("/v1/auth/email/start", "POST", apiPayload("email" to email.trim()), authenticatedRequest = false)
    suspend fun verifyEmailOtp(email: String, code: String) = requestMutex.withLock {
        saveSession(request("/v1/auth/email/verify", "POST", apiPayload("email" to email.trim(), "code" to code), authenticatedRequest = false))
        applyServerSnapshot(request("/v1/bootstrap"))
    }
    suspend fun verifyPassport(code: String): JsonObject =
        request("/v1/passports/verify?code=${queryValue(code.trim())}", authenticatedRequest = false)
    suspend fun adminInbox(): JsonObject = request("/v1/admin/inbox")
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
    suspend fun command(operation: String, payload: JsonObject, requestKey: String): JsonObject = requestMutex.withLock {
        val response = request("/v1/commands/$operation", "POST", payload, requestKey)
        applyServerSnapshot(response["snapshot"]?.jsonObject ?: throw BackendFailure("INVALID_RESPONSE", "Güncel durum alınamadı."))
        if (operation.startsWith("memory.") || operation == "user.block") {
            try { refreshMemoryFeedLocked() }
            catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (_: BackendFailure) { mutableMemoryFeed.value = null }
        }
        response["result"]?.jsonObject ?: JsonObject(emptyMap())
    }
    suspend fun logout() = requestMutex.withLock {
        try {
            DeviceEventBridge.devices.value?.let { runCatching { request("/v1/commands/device.remove", "POST", apiPayload("token" to it.token), requestKey("remove-device")) } }
            request("/v1/auth/logout", "POST", apiPayload())
        }
        finally { StoreBilling.clearProducts(); saveSession(null); imageCache.clear(); mutableSnapshot.value = null }
    }
    suspend fun uploadPhoto(bitmap: ImageBitmap): String {
        val bytes = encodeUploadPhoto(bitmap)
        if (bytes.size > 5 * 1024 * 1024) throw BackendFailure("IMAGE_SIZE", "Fotoğraf en fazla 5 MB olabilir.")
        if (!authenticated) throw BackendFailure("UNAUTHENTICATED", "Giriş yapmalısın.")
        val data = parse(authorizedRaw("/v1/media", "POST", bytes, mapOf("Content-Type" to "image/jpeg")))
        return data.getValue("id").jsonPrimitive.content
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
        val bitmap = decodeApiPhoto(response.bytes)
        if (imageCache.size >= 72) imageCache.remove(imageCache.keys.first())
        imageCache[uploadId] = bitmap; return bitmap
    }
    fun serverUserId(uiId: String): String = if (uiId == com.matchora.app.state.DemoData.ME) userId ?: uiId else uiId
    fun requestKey(prefix: String = "mobile"): String = "$prefix-${Clock.System.now().toEpochMilliseconds()}-${kotlin.random.Random.nextLong().toString().replace('-', 'n')}"
}
