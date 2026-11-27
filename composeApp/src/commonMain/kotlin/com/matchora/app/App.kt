package com.matchora.app

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.data.*
import kotlinx.serialization.json.*
import com.matchora.app.domain.*
import com.matchora.app.state.*
import com.matchora.app.ui.components.*
import com.matchora.app.ui.screens.*
import com.matchora.app.ui.theme.MColors
import com.matchora.app.ui.theme.MGradients
import com.matchora.app.ui.theme.MatchoraTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private var nextIdCounter = 1000L
private fun nextUniqueId(prefix: String = "id"): String = "${prefix}_${++nextIdCounter}"

private fun adminTabForAlert(title: String, body: String): Int {
    val text = "$title $body".lowercase()
    return when {
        "fotoğraf" in text || "destek" in text -> 0
        "şikâyet" in text || "anı" in text || "yayından" in text -> 1
        "tesis" in text || "skor" in text || "itiraz" in text || "değerlendirme" in text -> 2
        "kimlik" in text || "silme" in text || "hata" in text -> 3
        else -> 0
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MATCHORA SPORTS OPERATING SYSTEM (SPORTS NETWORK OS)
// Pure Light Mode · Sports Flow Background (Gri Tenis Kortu, Kale, Pota, Emojiler)
// Uçtan uca mimari: Şimdi Oyna + Eksik Oyuncu + Sports Passport + Reliability +
// Team Challenge + Yerel Ranking + Commitment Token + Sports Chemistry + Venues
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun App() {
    var showSplash by remember { mutableStateOf(true) }
    val backend = remember { BackendRepository() }
    val authBackend = remember(backend) { if (backend.enabled) backend else BackendRepository(mode = "production") }

    CompositionLocalProvider(LocalBackendRepository provides backend.takeIf { it.enabled }) {
    MatchoraTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            SportsFlowBackground(animate = !showSplash) {
                MainAppShell(backend, authBackend, splashFinished = !showSplash)
            }

            if (showSplash) {
                MatchoraSplashScreen(
                    onSplashFinished = { showSplash = false }
                )
            }
        }
    }
    }
}

@Composable
private fun MainAppShell(backend: BackendRepository, authBackend: BackendRepository, splashFinished: Boolean) {
    val authWire by authBackend.snapshots.collectAsState()
    val backendWire by backend.snapshots.collectAsState()
    val live = remember(backendWire) { backendWire?.let(::BackendSnapshot) }
    val currentDeviceLocation by DeviceLocationState.current.collectAsState()
    var backendBusy by remember { mutableIntStateOf(0) }
    var completingAuthentication by remember { mutableStateOf(false) }
    var sessionLeaving by remember { mutableStateOf(false) }
    var backendError by remember { mutableStateOf<String?>(null) }
    var backendRetry by remember { mutableStateOf<(() -> Unit)?>(null) }
    var remotePlayers by remember { mutableStateOf(emptyList<Player>()) }
    var remoteCommunities by remember { mutableStateOf(emptyList<CommunityInfo>()) }
    var previewCommunities by remember { mutableStateOf(DemoData.communities) }
    var remoteVenues by remember { mutableStateOf(emptyList<VenueInfo>()) }
    val clipboardManager = LocalClipboardManager.current
    val demoScope = rememberCoroutineScope()
    var currentTab by remember { mutableIntStateOf(0) }
    var scrollToTopTrigger by remember { mutableIntStateOf(0) }
    var competitionEntryTab by remember { mutableIntStateOf(0) }
    var competitionFocusId by remember { mutableStateOf<String?>(null) }
    var networkEntryTab by remember { mutableIntStateOf(0) }
    var networkTargetId by remember { mutableStateOf<String?>(null) }
    val persistedRegion = remember {
        loadPersistedRegionSelection()?.also { saved ->
            if (saved.isGps && saved.latitude != null && saved.longitude != null && DeviceLocationState.current.value == null) {
                DeviceLocationState.current.value = DeviceLocation(
                    saved.latitude,
                    saved.longitude,
                    canonicalTurkishCityName(saved.city),
                    canonicalTurkishDistrictName(saved.district)
                )
            }
        }
    }
    var currentCity by remember { mutableStateOf(canonicalTurkishCityName(persistedRegion?.city).ifBlank { "İstanbul" }) }
    var currentDistrict by remember { mutableStateOf(canonicalTurkishDistrictName(persistedRegion?.district).ifBlank { "Kadıköy" }) }
    var browseRegionSelected by remember { mutableStateOf(persistedRegion != null) }
    var manualNonGpsRegion by remember { mutableStateOf(persistedRegion?.manualNonGps ?: false) }
    var initialListingScope by remember { mutableStateOf(EventNetworkScope.CITY) }
    var isGpsLocationActive by remember { mutableStateOf(persistedRegion?.isGps ?: false) }
    var notificationPromptGate by remember { mutableStateOf<kotlinx.coroutines.CompletableDeferred<Boolean>?>(null) }
    val requestNotifications = rememberNotificationPermissionRequester { granted ->
        notificationPromptGate?.complete(granted)
    }
    val handleAutoLocationResult: (LocationOutcome) -> Unit = { result ->
        if (result is LocationOutcome.Located) {
            DeviceLocationState.current.value = result.value
            if (!manualNonGpsRegion) {
                isGpsLocationActive = true
                browseRegionSelected = true
                val foundCity = result.value.city?.trim()?.takeIf { it.isNotBlank() }
                if (foundCity != null) {
                    currentCity = canonicalTurkishCityName(foundCity)
                    val foundDistrict = canonicalTurkishDistrictName(result.value.district)
                    if (foundDistrict.isNotBlank()) {
                        currentDistrict = foundDistrict
                    } else if (!matchesTurkishPlace(currentCity, "İstanbul")) {
                        currentDistrict = "Merkez"
                    }
                }
                savePersistedRegionSelection(
                    PersistedRegionSelection(
                        city = currentCity,
                        district = currentDistrict,
                        isGps = true,
                        manualNonGps = false,
                        latitude = result.value.latitude,
                        longitude = result.value.longitude
                    )
                )
            }
        }
    }
    val requestAutoLocationWithPrompt = rememberLocationRequester(requestPermission = true, onResult = handleAutoLocationResult)
    val requestAutoLocationSilent = rememberLocationRequester(requestPermission = false, onResult = handleAutoLocationResult)
    LaunchedEffect(splashFinished) {
        if (!splashFinished) return@LaunchedEffect
        if (!hasPromptedInitialNotificationPermission()) {
            markInitialNotificationPermissionPrompted()
            val gate = kotlinx.coroutines.CompletableDeferred<Boolean>()
            notificationPromptGate = gate
            try {
                requestNotifications()
                kotlinx.coroutines.withTimeoutOrNull(60000L) { gate.await() }
            } finally {
                if (notificationPromptGate === gate) notificationPromptGate = null
            }
            delay(300)
        }
        if (!hasPromptedInitialLocationPermission()) {
            markInitialLocationPermissionPrompted()
            requestAutoLocationWithPrompt()
        } else if (hasForegroundLocationPermission()) {
            requestAutoLocationSilent()
        }
    }
    LaunchedEffect(currentDeviceLocation) {
        val loc = currentDeviceLocation
        if (loc != null) {
            if (!manualNonGpsRegion) {
                isGpsLocationActive = true
                browseRegionSelected = true
                loc.city?.trim()?.takeIf { it.isNotBlank() }?.let { foundCity ->
                    currentCity = canonicalTurkishCityName(foundCity)
                    val foundDistrict = canonicalTurkishDistrictName(loc.district)
                    if (foundDistrict.isNotBlank()) {
                        currentDistrict = foundDistrict
                    } else if (!matchesTurkishPlace(currentCity, "İstanbul")) {
                        currentDistrict = "Merkez"
                    }
                }
                savePersistedRegionSelection(
                    PersistedRegionSelection(
                        city = currentCity,
                        district = currentDistrict,
                        isGps = true,
                        manualNonGps = false,
                        latitude = loc.latitude,
                        longitude = loc.longitude
                    )
                )
            }
        } else if (backend.enabled) {
            isGpsLocationActive = false
        }
    }
    // ═══════════════ STATE MANAGEMENT ═══════════════
    var tokenBalance by remember { mutableIntStateOf(if (backend.enabled) 0 else 120) }
    var lockedTokens by remember { mutableIntStateOf(0) }
    var eventTokenHolds by remember { mutableStateOf(emptyMap<String, Int>()) }
    var selectedTopUpPack by remember { mutableStateOf(DemoData.tokenPacks.getOrNull(1) ?: DemoData.tokenPacks.first()) }
    var activeProPlan by remember { mutableStateOf<ProPlan?>(null) }
    var isLoggedIn by remember { mutableStateOf(authBackend.authenticated) }
    var hasCompletedPassport by remember { mutableStateOf(false) }
    var pendingAccountAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var checkInEvent by remember { mutableStateOf<SportEvent?>(null) }
    var checkInCode by remember { mutableStateOf("") }
    var hostCheckInCode by remember { mutableStateOf("") }
    var showContactVerification by remember { mutableStateOf(false) }
    var showPhotoVerification by remember { mutableStateOf(false) }
    var showAccountSettings by remember { mutableStateOf(false) }
    var showVenueOperator by remember { mutableStateOf(false) }
    var showModeration by remember { mutableStateOf(false) }
    var adminTab by remember { mutableIntStateOf(0) }
    var adminRefreshSignal by remember { mutableIntStateOf(0) }
    val isAdminAccount = backend.enabled && backend.authenticated && !sessionLeaving && (live?.wire?.s("role") == "admin" || authWire?.s("role") == "admin")
    val isAdminSession = isAdminAccount && authWire?.b("legalAcceptanceRequired") != true
    var showMyActivity by remember { mutableStateOf(false) }
    var feedbackEvent by remember { mutableStateOf<SportEvent?>(null) }
    var resultEvent by remember { mutableStateOf<SportEvent?>(null) }
    var matchResults by remember { mutableStateOf(emptyMap<String, MatchResultDraft>()) }
    var privacyPrefs by remember { mutableStateOf(PrivacyPrefs()) }
    var notificationPrefs by remember { mutableStateOf(NotificationPrefs()) }
    var blockedChatIds by remember { mutableStateOf(emptySet<String>()) }
    var supportRequests by remember { mutableStateOf(emptyList<SupportRequest>()) }
    var moderationReports by remember { mutableStateOf(emptyList<ModerationReport>()) }

    val guestProfile = remember(currentCity, currentDistrict) { UserProfile("Misafir Sporcu", currentCity, currentDistrict, "", 2000, "", "", emptyList(), emptySet(), 100, 0f, 0, 0, null, null, "", emptyList()) }
    var profile by remember { mutableStateOf(if (backend.enabled) guestProfile else DemoData.me) }
    LaunchedEffect(backend.enabled, isLoggedIn, guestProfile) {
        if (backend.enabled && !isLoggedIn) profile = guestProfile
    }
    var events by remember { mutableStateOf(if (backend.enabled) emptyList() else DemoData.events) }
    var joinedEventIds by remember { mutableStateOf(if (backend.enabled) emptySet() else setOf("e5", "e7")) }
    var waitlistedEventIds by remember { mutableStateOf(emptySet<String>()) }
    var checkedInEventIds by remember { mutableStateOf(setOf<String>()) }
    var joinRequests by remember { mutableStateOf(if (backend.enabled) emptyList() else DemoData.initialJoinRequests) }

    var threads by remember { mutableStateOf(if (backend.enabled) emptyList() else DemoData.chats) }
    var memoryPosts by remember { mutableStateOf(if (backend.enabled) emptyList() else DemoData.initialMemoryPosts) }
    var activeChatThreadId by remember { mutableStateOf<String?>(null) }
    var chatEntrySection by remember { mutableStateOf(ChatHubSection.MESSAGES) }
    var focusedMemoryId by remember { mutableStateOf<String?>(null) }
    var notifications by remember { mutableStateOf(if (backend.enabled) emptyList() else DemoData.notifications) }
    var ledger by remember { mutableStateOf(if (backend.enabled) emptyList() else DemoData.initialLedger) }
    var venuePayments by remember { mutableStateOf(if (backend.enabled) emptyList() else DemoData.initialVenuePayments) }
    var bookedSlotIds by remember { mutableStateOf(emptySet<String>()) }

    var teams by remember { mutableStateOf(if (backend.enabled) emptyList() else DemoData.teams) }
    var tournaments by remember { mutableStateOf(if (backend.enabled) emptyList() else DemoData.tournaments) }
    var registeredTournamentIds by remember { mutableStateOf(emptySet<String>()) }
    var invitedPlayerIds by remember { mutableStateOf(emptySet<String>()) }
    var challenges by remember { mutableStateOf(if (backend.enabled) emptyList() else DemoData.challenges) }
    var teamJoinRequests by remember { mutableStateOf(if (backend.enabled) emptyList() else DemoData.teamJoinRequests) }
    var joinedCommunityIds by remember { mutableStateOf(setOf<String>()) }
    var requestedCommunityIds by remember { mutableStateOf(setOf<String>()) }

    val currentPlayer = remember(profile, backend.enabled) {
        val primarySkill = profile.skills.firstOrNull()
        if (backend.enabled) Player(
            id = DemoData.ME,
            name = profile.name,
            area = profile.area,
            sports = profile.skills.map { it.sport },
            level = primarySkill?.level ?: SkillLevel.INTERMEDIATE,
            reliability = profile.reliability,
            verifiedMatches = profile.verifiedMatches,
            rating = primarySkill?.rating ?: 1000,
            chemistry = null,
            verified = VerificationLevel.PHOTO in profile.verifications,
            position = primarySkill?.position ?: profile.dominantSide,
            distanceKm = null,
            playedTogether = 0,
            availableNow = false,
            color = MColors.Brand,
            bio = profile.bio,
            reliabilityVisible = true,
            profileAvailable = profile.skills.isNotEmpty(),
            dominantSide = profile.dominantSide,
            availability = profile.availability,
            campus = profile.campus,
            company = profile.company,
            jerseyNumber = profile.jerseyNumber,
            fairPlay = profile.fairPlay
        ) else DemoData.mePlayer.copy(
            name = profile.name,
            area = profile.area,
            sports = profile.skills.map { it.sport },
            level = primarySkill?.level ?: DemoData.mePlayer.level,
            reliability = profile.reliability,
            verifiedMatches = profile.verifiedMatches,
            rating = primarySkill?.rating ?: DemoData.mePlayer.rating,
            verified = VerificationLevel.PHOTO in profile.verifications,
            position = primarySkill?.position ?: profile.dominantSide
        )
    }

    // Dialog & Modal State
    var selectedDetailEvent by remember { mutableStateOf<SportEvent?>(null) }
    var directInvitedEventId by remember { mutableStateOf<String?>(null) }
    var tournamentTeamChoice by remember { mutableStateOf<Pair<TournamentInfo, Boolean>?>(null) }
    var selectedDetailPlayers by remember { mutableStateOf<List<Player>>(emptyList()) }
    var previewRemovedEventMembers by remember { mutableStateOf<Map<String, Set<String>>>(emptyMap()) }
    var showPlayNowSheet by remember { mutableStateOf(false) }
    var playNowInitialVenue by remember { mutableStateOf<String?>(null) }
    var playNowInitialTime by remember { mutableStateOf<String?>(null) }
    var walletStartsOnPlus by remember { mutableStateOf(false) }
    var showWalletSheet by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var challengeTargetTeam by remember { mutableStateOf<TeamProfile?>(null) }
    var challengeSourceTeam by remember { mutableStateOf<TeamProfile?>(null) }
    var reportTargetId by remember { mutableStateOf("") }
    var reportTargetType by remember { mutableStateOf("event") }
    var reportTargetName by remember { mutableStateOf<String?>(null) }
    var showTokenTopUpSheet by remember { mutableStateOf(false) }
    var showCityLocationSheet by remember { mutableStateOf(false) }
    var showSubmitJoinSheetForEvent by remember { mutableStateOf<SportEvent?>(null) }
    var showManageJoinRequestsForEvent by remember { mutableStateOf<SportEvent?>(null) }
    var showMenuSheet by remember { mutableStateOf(false) }
    var showAuthSheet by remember { mutableStateOf(false) }
    var currentToast by remember { mutableStateOf<Toast?>(null) }

    fun requireAccount(action: () -> Unit) {
        if (!sessionLeaving && isLoggedIn && hasCompletedPassport && authWire?.b("legalAcceptanceRequired") != true) action() else {
            pendingAccountAction = action
            showAuthSheet = true
        }
    }

    val unreadNotificationsCount = if (backend.enabled) live?.wire?.i("unreadNotificationCount") ?: 0 else notifications.count { !it.read }
    val unreadChatThreadsCount = if (backend.enabled) live?.wire?.i("unreadThreadCount") ?: 0 else threads.count { it.unread > 0 }

    // Toast Gösterici Fonksiyon
    fun showToast(message: String, emoji: String = "✓") {
        currentToast = Toast(message, emoji, ++nextIdCounter)
    }

    fun defaultOperationToast(operation: String, payload: JsonObject): Pair<String, String>? = when (operation) {
        "discovery.save", "discovery.location", "chat.read", "device.register", "device.remove" -> null
        "notification.read" -> if (payload.s("notificationId").isBlank()) "Bildirimler okundu işaretlendi" to "✓" else null
        "notification.clear" -> if (payload.s("notificationId").isBlank()) "Bildirimler temizlendi" to "✓" else "Bildirim silindi" to "✓"
        "event.join" -> "Kadroya katıldın" to "⚽"
        "event.leave" -> "Kadrodan ayrıldın" to "↩️"
        "event.waitlist" -> when (payload.s("action")) {
            "leave" -> "Bekleme listesinden ayrıldın" to "↩️"
            "accept" -> "Kadroya katıldın" to "⚽"
            else -> "Bekleme listesine eklendin" to "⏳"
        }
        "event.request" -> "Katılım başvurun iletildi" to "⏳"
        "event.request.review" -> if (payload.b("approve")) "Başvuru onaylandı" to "✓" else "Başvuru reddedildi" to "✕"
        "event.request.cancel" -> "Başvurun geri çekildi" to "↩️"
        "event.transition" -> "Maç durumu güncellendi" to "⚡"
        "event.member.remove" -> "Oyuncu kadrodan çıkarıldı" to "↩️"
        "event.member.restore" -> "Oyuncu yeniden kadroya alındı" to "✓"
        "event.checkin" -> "Saha check-in tamamlandı" to "📍"
        "event.checkin.code" -> "Check-in kodu oluşturuldu" to "📍"
        "event.create" -> "İlan yayınlandı" to "🎉"
        "team.create" -> "Takım oluşturuldu" to "🛡️"
        "team.invite" -> "Takım daveti gönderildi" to "🛡️"
        "team.request" -> "Takım başvurun iletildi" to "⏳"
        "team.request.review" -> if (payload.b("approve")) "Takım başvurusu onaylandı" to "✓" else "Takım başvurusu reddedildi" to "✕"
        "challenge.create" -> "Meydan okuma gönderildi" to "⚔️"
        "challenge.respond" -> if (payload.b("accept")) "Meydan okuma kabul edildi" to "✓" else "Meydan okuma reddedildi" to "✕"
        "tournament.create" -> "Turnuva oluşturuldu" to "🏆"
        "tournament.register" -> "Turnuva kaydı tamamlandı" to "🏆"
        "tournament.withdraw" -> "Turnuva kaydı geri çekildi" to "↩️"
        "tournament.cancel" -> "Turnuva iptal edildi" to "↩️"
        "booking.create" -> "Saha rezervasyonu kaydedildi" to "🏟️"
        "booking.cancel" -> "Rezervasyon iptal edildi" to "↩️"
        "community.join" -> "Topluluk işlemi gönderildi" to "🤝"
        "community.leave" -> "Topluluktan ayrıldın" to "↩️"
        "chat.send", "message.edit", "message.delete", "message.hide", "chat.clear", "chat.delete", "chat.mute" -> "Sohbet güncellendi" to "💬"
        "memory.like", "memory.create", "memory.delete", "memory.update", "memory.comment" -> "Anı vitrini güncellendi" to "📸"
        "user.block" -> if (payload.b("blocked")) "Kullanıcı engellendi" to "🛡️" else "Engel kaldırıldı" to "🛡️"
        "report.create" -> "Bildirimin iletildi" to "🛡️"
        "support.create" -> "Destek talebin alındı" to "💬"
        "preferences.save" -> "Tercihlerin kaydedildi" to "✓"
        "result.submit" -> "Maç sonucu kaydedildi" to "🏆"
        "result.confirm" -> "Maç sonucu onaylandı" to "🏆"
        "result.dispute" -> "Skor itirazı kaydedildi" to "⚖️"
        "feedback.submit" -> "Değerlendirmen kaydedildi" to "⭐"
        "account.delete" -> "Hesap silme talebi kaydedildi" to "ℹ️"
        else -> "İşlem tamamlandı" to "✓"
    }

    fun remote(operation: String, payload: JsonObject, silent: Boolean = false, after: (JsonObject) -> Unit = {}): Boolean {
        if (sessionLeaving || !isLoggedIn || !hasCompletedPassport || authWire?.b("legalAcceptanceRequired") == true) { pendingAccountAction = { remote(operation, payload, silent, after) }; showAuthSheet = true; return true }
        if (!backend.enabled) return false
        val key = backend.requestKey(operation)
        lateinit var execute: () -> Unit
        execute = {
            demoScope.launch {
                if (!silent) backendBusy++
                try {
                    val toastBefore = currentToast?.id
                    val result = backend.command(operation, payload, key)
                    after(result)
                    backendError = null; backendRetry = null
                    if (!silent && currentToast?.id == toastBefore) {
                        defaultOperationToast(operation, payload)?.let { (msg, emoji) -> showToast(msg, emoji) }
                    }
                    if (operation in listOf("event.join", "event.create", "booking.create")) requestNotifications()
                }
                catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (error: Exception) {
                    val msg = error.message ?: "İşlem tamamlanamadı."
                    showToast(msg, "⚠️")
                    if (!silent) { backendError = msg; backendRetry = execute }
                }
                finally { if (!silent) backendBusy-- }
            }
        }
        execute()
        return true
    }
    fun remoteProfile(updated: UserProfile, after: () -> Unit = {}): Boolean {
        if (!backend.enabled) return false
        val key = backend.requestKey("profile")
        var uploadId = if (updated.avatarRemovalRequested) null else live?.avatarUploadId
        var preparedPayload: JsonObject? = null
        lateinit var execute: () -> Unit
        execute = {
            demoScope.launch {
                backendBusy++
                try {
                    if (preparedPayload == null) {
                        if (!updated.avatarRemovalRequested && updated.avatar != null && (uploadId == null || updated.avatar !== profile.avatar)) uploadId = backend.uploadPhoto(updated.avatar)
                        preparedPayload = updated.toApiPayload(updated.version.takeIf { it > 0 }, uploadId)
                    }
                    val toastBefore = currentToast?.id
                    backend.command("profile.save", requireNotNull(preparedPayload), key)
                    after(); backendError = null; backendRetry = null
                    if (currentToast?.id == toastBefore) showToast("Sporcu profilin güncellendi", "✓")
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (error: Exception) {
                    val msg = error.message ?: "Profil kaydedilemedi."
                    showToast(msg, "⚠️")
                    backendError = msg; backendRetry = execute
                }
                finally { backendBusy-- }
            }
        }
        execute(); return true
    }
    fun remotePhoto(memory: MatchMemoryPost, threadId: String? = null, publishMemory: Boolean = false): Boolean {
        if (!backend.enabled) return false
        lateinit var execute: () -> Unit
        execute = { demoScope.launch {
            backendBusy++
            try {
                if (memory.remoteUploadId == null && memory.customBitmap == null) {
                    throw BackendFailure("PHOTO_REQUIRED", "Bir fotoğraf seç.")
                }
                val operation = if (publishMemory || threadId == null) "memory.create" else "chat.send"
                val payload = if (operation == "memory.create") apiPayload(
                    "caption" to memory.caption, "sport" to memory.sport, "venue" to memory.venue,
                    "area" to memory.area, "city" to memory.city, "uploadId" to memory.remoteUploadId,
                    "shareThreadId" to threadId, "matchScoreTag" to memory.matchScoreTag,
                    "sceneTheme" to memory.sceneTheme, "approximateLocation" to memory.approximateLocation?.let {
                        mapOf("latitude" to it.latitude, "longitude" to it.longitude)
                    }
                ) else apiPayload(
                    "threadId" to threadId, "text" to memory.caption, "uploadId" to memory.remoteUploadId,
                    "forwardId" to memory.id.takeIf { memory.remoteUploadId != null }
                )
                backend.enqueueSend(operation, payload, memory.customBitmap.takeIf { memory.remoteUploadId == null })
                backendError = null; backendRetry = null
                showToast(if (operation == "memory.create") "Anı paylaşımı gönderiliyor" else "Fotoğraf sohbete gönderiliyor", "📸")
                demoScope.launch { backend.processPendingSends() }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) {
                val msg = error.message ?: "Fotoğraf kaydedilemedi."
                showToast(msg, "⚠️")
                backendError = msg; backendRetry = execute
            }
            finally { backendBusy-- }
        } }
        execute(); return true
    }
    fun remoteMemoryEdit(memory: MatchMemoryPost): Boolean {
        if (!backend.enabled) return false
        var upload = memory.remoteUploadId.takeIf { memory.customBitmap == null }
        val key = backend.requestKey("memory-edit")
        lateinit var execute: () -> Unit
        execute = { demoScope.launch {
            backendBusy++
            try {
                if (upload == null) upload = memory.customBitmap?.let { backend.uploadPhoto(it) }
                backend.command("memory.update", apiPayload("memoryId" to memory.id, "expectedVersion" to memory.version,
                    "caption" to memory.caption, "venue" to memory.venue, "area" to memory.area, "city" to memory.city, "sport" to memory.sport,
                    "matchScoreTag" to memory.matchScoreTag, "sceneTheme" to memory.sceneTheme, "uploadId" to upload,
                    "approximateLocation" to memory.approximateLocation?.let { mapOf("latitude" to it.latitude, "longitude" to it.longitude) }), key)
                backendError = null; backendRetry = null
                showToast("Anı güncellendi", "✓")
            } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (error: Exception) {
                val msg = error.message ?: "Anı güncellenemedi."
                showToast(msg, "⚠️")
                backendError = msg; backendRetry = execute
            }
            finally { backendBusy-- }
        } }
        execute(); return true
    }
    fun remotePhotoVerification(image: androidx.compose.ui.graphics.ImageBitmap?, after: (Boolean) -> Unit): Boolean {
        if (!backend.enabled) return false
        val key = backend.requestKey("verification")
        val existingUploadId = live?.avatarUploadId
        var uploadId: String? = if (image == null || image === profile.avatar) existingUploadId else null
        var savedPayload: JsonObject? = null
        lateinit var execute: () -> Unit
        execute = { demoScope.launch {
            backendBusy++
            try {
                if (uploadId == null) {
                    val bmp = image ?: throw BackendFailure("PHOTO_REQUIRED", "Doğrulama için bir profil fotoğrafı seç.")
                    uploadId = backend.uploadPhoto(bmp)
                }
                if (uploadId != existingUploadId) {
                    if (savedPayload == null) savedPayload = profile.toApiPayload(backend.profileVersion, uploadId)
                    backend.command("profile.save", requireNotNull(savedPayload), "$key-profile")
                }
                backend.command("verification.photo", apiPayload("uploadId" to requireNotNull(uploadId)), "$key-review")
                showPhotoVerification = false; backendError = null; after(true)
                showToast("Fotoğraf doğrulama talebi iletildi", "✓")
            } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (e: Exception) {
                val msg = e.message ?: "Fotoğraf doğrulaması gönderilemedi."
                showToast(msg, "⚠️")
                after(false); backendError = msg; backendRetry = execute
            }
            finally { backendBusy-- }
        } }
        execute(); return true
    }
    fun remoteLogout(): Boolean {
        if (!authBackend.authenticated && !backend.enabled) return false
        sessionLeaving = true; isLoggedIn = false; hasCompletedPassport = false; showAuthSheet = false
        demoScope.launch {
            try { authBackend.logout() } catch (_: Exception) { }
            finally { sessionLeaving = false }
            isLoggedIn = false; hasCompletedPassport = false; activeChatThreadId = null; profile = guestProfile
            adminTab = 0; showAccountSettings = false; currentTab = 0; showAuthSheet = false
            showToast("Çıkış yapıldı · Misafir modundasın", "ℹ️")
            if (backend.enabled) runCatching { backend.reload() }
        }
        return true
    }
    fun finishAuthentication(signedProfile: UserProfile) {
        profile = if (backend.enabled) signedProfile else signedProfile.copy(
            reliability = DemoData.me.reliability, fairPlay = DemoData.me.fairPlay,
            verifiedMatches = DemoData.me.verifiedMatches, streakWeeks = DemoData.me.streakWeeks,
            skills = signedProfile.skills.map { selected -> DemoData.me.skills.firstOrNull { it.sport == selected.sport }?.copy(level = selected.level, position = selected.position) ?: selected } +
                DemoData.me.skills.filter { demo -> signedProfile.skills.none { it.sport == demo.sport } }
        )
        isLoggedIn = true; hasCompletedPassport = true; showAuthSheet = false
        showToast("Pasaportun hazır · Hoş geldin!", "🪪")
        val pending = pendingAccountAction; pendingAccountAction = null; pending?.invoke()
    }
    fun completeAuthentication(newProfile: UserProfile) {
        if (completingAuthentication || sessionLeaving) return
        if (!authBackend.authenticated) { backendError = "Oturum doğrulanmadı. Giriş yaparak tekrar dene."; return }
        val key = authBackend.requestKey("first-passport")
        var uploaded: String? = null
        lateinit var execute: () -> Unit
        execute = { completingAuthentication = true; demoScope.launch {
            backendBusy++
            try {
                val isAdminSnap = authBackend.snapshots.value?.s("role") == "admin"
                if (authBackend.profileVersion == 0 && !isAdminSnap) {
                    if (uploaded == null && newProfile.avatar != null) uploaded = authBackend.uploadPhoto(newProfile.avatar)
                    authBackend.command("profile.save", newProfile.toApiPayload(0, uploaded), key)
                }
                val saved = authBackend.snapshots.value?.let(::BackendSnapshot)?.profile
                    ?: if (isAdminSnap) newProfile else throw BackendFailure("PASSPORT_REQUIRED", "Sporcu pasaportun kaydedilemedi.")
                finishAuthentication(saved); backendError = null; backendRetry = null
            } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (error: Exception) {
                val msg = error.message ?: "Sporcu pasaportun kaydedilemedi."
                showToast(msg, "⚠️")
                backendError = msg; backendRetry = execute
            }
            finally { backendBusy--; completingAuthentication = false }
        } }
        execute()
    }
    LaunchedEffect(authBackend) {
        if (authBackend !== backend && authBackend.baseUrl.isNotBlank()) {
            try {
                authBackend.start()
                val saved = authBackend.snapshots.value?.let(::BackendSnapshot)?.profile
                if (authBackend.authenticated && saved != null && authBackend.snapshots.value?.b("legalAcceptanceRequired") != true) finishAuthentication(saved)
            } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (_: Exception) { /* Login stays locked; explicit sign-in displays retryable errors. */ }
        }
    }
    fun openEventById(eventId: String, grantDirectInvite: Boolean = false) {
        if (sessionLeaving || !isLoggedIn || !hasCompletedPassport || authWire?.b("legalAcceptanceRequired") == true) { requireAccount { openEventById(eventId, grantDirectInvite) }; return }
        val clean = eventId.trim().removePrefix("#")

        // 1. Önce Turnuva eşleşmesini kontrol et (#TRN-... veya turnuva ID)
        val matchedTournament = tournaments.firstOrNull {
            it.id.equals(clean, ignoreCase = true) ||
            tournamentDisplayId(it.id).equals("#$clean", ignoreCase = true) ||
            tournamentDisplayId(it.id).removePrefix("#").equals(clean, ignoreCase = true) ||
            it.id.startsWith(clean, ignoreCase = true)
        }
        if (matchedTournament != null) {
            competitionEntryTab = 1
            competitionFocusId = matchedTournament.id
            currentTab = 2
            showToast("🏆 ${matchedTournament.name} bulundu · Turnuva açıldı", "🎟️")
            return
        }

        // 2. Kulüp / Takım eşleşmesini kontrol et (#KLB-... veya takım ID)
        val matchedTeam = teams.firstOrNull {
            it.id.equals(clean, ignoreCase = true) ||
            teamDisplayId(it.id).equals("#$clean", ignoreCase = true) ||
            teamDisplayId(it.id).removePrefix("#").equals(clean, ignoreCase = true) ||
            it.id.startsWith(clean, ignoreCase = true)
        }
        if (matchedTeam != null) {
            competitionEntryTab = 2
            competitionFocusId = matchedTeam.id
            currentTab = 2
            showToast("🛡️ ${matchedTeam.name} bulundu · Kulüp sayfası açıldı", "⚽")
            return
        }

        // 3. Tesis eşleşmesini kontrol et (#TSS-... veya tesis ID)
        val matchedVenue = (if (backend.enabled) remoteVenues else DemoData.venues).firstOrNull {
            it.id.equals(clean, ignoreCase = true) ||
            venueDisplayId(it.id).equals("#$clean", ignoreCase = true) ||
            venueDisplayId(it.id).removePrefix("#").equals(clean, ignoreCase = true) ||
            it.id.startsWith(clean, ignoreCase = true)
        }
        if (matchedVenue != null) {
            networkEntryTab = 1
            networkTargetId = matchedVenue.id
            currentTab = 3
            showToast("📍 ${matchedVenue.name} bulundu · Tesis açıldı", "🏟️")
            return
        }

        // 4. Topluluk eşleşmesini kontrol et (#TPL-... veya topluluk ID)
        val matchedCommunity = (if (backend.enabled) remoteCommunities else previewCommunities).firstOrNull {
            it.id.equals(clean, ignoreCase = true) ||
            communityDisplayId(it.id).equals("#$clean", ignoreCase = true) ||
            communityDisplayId(it.id).removePrefix("#").equals(clean, ignoreCase = true) ||
            it.id.startsWith(clean, ignoreCase = true)
        }
        if (matchedCommunity != null) {
            networkEntryTab = 0
            networkTargetId = matchedCommunity.id
            currentTab = 3
            showToast("👥 ${matchedCommunity.name} bulundu · Topluluk açıldı", "🌟")
            return
        }

        // 5. İlan eşleşmesini kontrol et (#ILN-... veya tam maç ID)
        val matchedEvent = events.firstOrNull { matchesEventInviteCode(it.id, clean) }
        val targetEventId = matchedEvent?.id ?: clean
        if (matchedEvent != null) {
            if (grantDirectInvite) directInvitedEventId = matchedEvent.id
            selectedDetailEvent = matchedEvent
            showToast(if (grantDirectInvite) "🎟️ ${matchedEvent.title} bulundu · Özel davet onaylandı!" else "⚽ ${matchedEvent.title} açıldı", if (grantDirectInvite) "✅" else "🎟️")
            return
        }

        if (!backend.enabled) { showToast("Bu ID ile eşleşen bir maç, turnuva, kulüp veya tesis bulunamadı", "ℹ️"); return }
        demoScope.launch {
            backendBusy++
            try {
                val detail = backend.eventDetails(targetEventId)
                val parsed = BackendSnapshot(apiPayload("userId" to backend.userId, "events" to listOf(detail), "players" to detail.a("players")))
                selectedDetailPlayers = parsed.players
                val loadedEvent = parsed.events.firstOrNull()
                if (loadedEvent != null) {
                    if (grantDirectInvite) directInvitedEventId = loadedEvent.id
                    selectedDetailEvent = loadedEvent
                    showToast(if (grantDirectInvite) "🎟️ ${loadedEvent.title} bulundu · Özel davet onaylandı!" else "⚽ ${loadedEvent.title} açıldı", if (grantDirectInvite) "✅" else "🎟️")
                } else {
                    showToast("Bu maç artık listede bulunmuyor", "ℹ️")
                }
            } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (error: Exception) {
                val msg = error.message ?: "İlan yüklenemedi."
                showToast(msg, "⚠️")
                backendError = msg; backendRetry = { openEventById(eventId, grantDirectInvite) }
            }
            finally { backendBusy-- }
        }
    }
    fun rewardShareBonus(description: String) {
        if (backend.enabled) {
            showToast("Paylaşım hazırlandı", "📤")
            return
        }
        tokenBalance += 2
        ledger = listOf(
            LedgerEntry(nextUniqueId("l_share"), LedgerType.BONUS, 2, description, "Şimdi", idempotencyKey = nextUniqueId("op_share"))
        ) + ledger
        showToast("Paylaşıldı · +2 TKN cüzdanına eklendi 💎", "📤")
    }
    fun openCatalogTarget(kind: String, id: String) {
        if (sessionLeaving || !isLoggedIn || !hasCompletedPassport || authWire?.b("legalAcceptanceRequired") == true) { requireAccount { openCatalogTarget(kind,id) }; return }
        fun showTarget() {
            if (kind in listOf("teams", "tournaments")) { competitionEntryTab = if (kind == "tournaments") 1 else 2; competitionFocusId = id; currentTab = 2 }
            else { networkEntryTab = if (kind == "venues") 1 else 0; networkTargetId = id; currentTab = 3 }
        }
        if (!backend.enabled) { showTarget(); return }
        demoScope.launch {
            backendBusy++
            try { backend.includeCatalogItem(kind, id); showTarget() }
            catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (error: Exception) { backendError = error.message; backendRetry = { openCatalogTarget(kind,id) } }
            finally { backendBusy-- }
        }
    }
    LaunchedEffect(backend) {
        if (backend.enabled) {
            backendBusy++
            try {
                backend.start()
                val saved = backend.snapshots.value?.let(::BackendSnapshot)?.profile
                if (backend.authenticated && saved != null && backend.snapshots.value?.b("legalAcceptanceRequired") != true) {
                    finishAuthentication(saved)
                }
            }
            catch (error: Exception) {
                val msg = error.message ?: "Sunucuya bağlanılamadı."
                showToast(msg, "⚠️")
                backendError = msg; backendRetry = { demoScope.launch { runCatching { backend.start() }.onFailure { val m = it.message ?: "Sunucuya bağlanılamadı."; showToast(m, "⚠️"); backendError = m } } }
            }
            finally { backendBusy-- }
        }
    }
    LaunchedEffect(backend, currentCity) {
        if (backend.enabled) {
            try { backend.setBrowseCity(currentCity) }
            catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (error: Exception) {
                val msg = error.message ?: "Şehir verileri yüklenemedi."
                showToast(msg, "⚠️")
                backendError = msg; backendRetry = { demoScope.launch { runCatching { backend.setBrowseCity(currentCity) }.onFailure { val m = it.message ?: "Şehir verileri yüklenemedi."; showToast(m, "⚠️"); backendError = m } } }
            }
        }
    }
    LaunchedEffect(backend, currentTab) {
        if (backend.enabled) {
            try {
                backend.setDiscoveryActive(currentTab == 0 || currentTab == 1)
                backend.setInboxActive(currentTab == 4)
                backend.setActiveCatalogs(when (currentTab) { 2 -> setOf("teams", "tournaments"); 3 -> setOf("communities", "venues"); else -> emptySet() })
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                throw cancel
            } catch (_: Exception) {
                /* Existing snapshot remains available; explicit user actions surface errors. */
            }
        }
    }
    val appActive = rememberAppActive()
    LaunchedEffect(authBackend.authenticated, authWire) {
        val serverProfile = authWire?.let(::BackendSnapshot)?.profile
        if (authBackend.authenticated && serverProfile != null && authWire?.b("legalAcceptanceRequired") != true) {
            if (!isLoggedIn || !hasCompletedPassport || profile !== serverProfile) {
                finishAuthentication(serverProfile)
            }
        } else if (authBackend.authenticated && serverProfile == null && authWire != null) {
            if (!isLoggedIn) isLoggedIn = true
            hasCompletedPassport = false
        } else if (!authBackend.authenticated && isLoggedIn && authBackend !== backend) {
            isLoggedIn = false; hasCompletedPassport = false; profile = guestProfile
        }
    }
    LaunchedEffect(authBackend, appActive, isLoggedIn) {
        if (authBackend !== backend && appActive && isLoggedIn) while (true) {
            delay(20000)
            try { authBackend.poll() }
            catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (_: Exception) { /* Session invalidation clears auth state; temporary offline preserves a verified session. */ }
        }
    }
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn && hasForegroundLocationPermission()) requestAutoLocationSilent()
    }
    rememberForegroundLocationRefresh(appActive && backend.enabled && isLoggedIn && hasCompletedPassport)
    LaunchedEffect(backend, live?.userId, isLoggedIn, currentDeviceLocation, appActive) {
        if (backend.enabled && isLoggedIn && appActive) {
            val point = currentDeviceLocation?.takeIf { kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - it.capturedAt < 120000 }
            try { backend.command("discovery.location", apiPayload("latitude" to point?.latitude, "longitude" to point?.longitude), backend.requestKey("location")) }
            catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (_: Exception) { /* A location refresh never blocks navigation; the previous server fix expires. */ }
        }
    }
    LaunchedEffect(backend, isLoggedIn, hasCompletedPassport, isGpsLocationActive, currentCity, currentDistrict) {
        if (backend.enabled && isLoggedIn && hasCompletedPassport && isGpsLocationActive && currentDeviceLocation != null) {
            try {
                backend.command("discovery.save", apiPayload("city" to currentCity, "district" to currentDistrict, "travelMode" to false), backend.requestKey("gps-discovery"))
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                throw cancel
            } catch (_: Exception) {
                /* Silent sync: manual location selector or next discovery save will retry if needed. */
            }
        }
    }
    val pushDevice by DeviceEventBridge.devices.collectAsState()
    val incomingNavigation by DeviceEventBridge.navigation.collectAsState()
    LaunchedEffect(pushDevice, live?.userId) {
        val device = pushDevice
        if (backend.enabled && backend.authenticated && device != null) {
            runCatching { backend.command("device.register", apiPayload("platform" to device.platform, "token" to device.token), backend.requestKey("device")) }
        }
    }
    LaunchedEffect(incomingNavigation, isLoggedIn, hasCompletedPassport, authWire, appActive) {
        val navigation = incomingNavigation
        if (navigation != null && appActive) {
            if (backend.enabled && backend.authenticated && authWire == null) return@LaunchedEffect
            if (!isLoggedIn || !hasCompletedPassport || authWire?.b("legalAcceptanceRequired") == true) showAuthSheet = true
            else {
                try {
                    if (navigation.notificationId.isNotBlank()) {
                        notifications = notifications.map { if (it.id == navigation.notificationId) it.copy(read = true) else it }
                        if (backend.enabled) remote("notification.read", apiPayload("notificationId" to navigation.notificationId), silent = true)
                    }
                    if (isAdminSession && navigation.destination in setOf(
                            "CHALLENGES", "TEAM", "TOURNAMENT", "RANKING", "PASSPORT", "CHAT", "NETWORK", "VENUES", "MEMORIES"
                        )
                    ) {
                        val matchedNotif = notifications.firstOrNull { it.id == navigation.notificationId || it.targetId == navigation.targetId }
                        adminTab = adminTabForAlert(matchedNotif?.title.orEmpty(), matchedNotif?.body.orEmpty())
                        adminRefreshSignal++
                    } else when (navigation.destination) {
                        "EVENT" -> openEventById(navigation.targetId)
                        "CHAT" -> {
                            activeChatThreadId = navigation.targetId
                            chatEntrySection = ChatHubSection.MESSAGES
                            focusedMemoryId = null
                            currentTab = 4
                        }
                        "CHALLENGES" -> {
                            competitionEntryTab = 2
                            competitionFocusId = navigation.targetId.takeIf { it.isNotBlank() }
                            currentTab = 2
                        }
                        "TEAM", "TOURNAMENT" -> openCatalogTarget(if (navigation.destination == "TOURNAMENT") "tournaments" else "teams", navigation.targetId)
                        "WALLET", "PLUS" -> { walletStartsOnPlus = navigation.destination == "PLUS"; showWalletSheet = true }
                        "ACCOUNT" -> showAccountSettings = true
                        "RANKING" -> { competitionEntryTab = 0; currentTab = 2 }
                        "PASSPORT" -> currentTab = 5
                        "PLAY" -> showPlayNowSheet = true
                        "NETWORK", "VENUES" -> {
                            val target = navigation.targetId.orEmpty()
                            if (target.isBlank()) {
                                currentTab = 3
                            } else {
                                openCatalogTarget(if (navigation.destination == "VENUES") "venues" else "communities", target)
                            }
                        }
                        "MEMORIES" -> {
                            activeChatThreadId = null
                            chatEntrySection = ChatHubSection.SHOWCASE
                            focusedMemoryId = navigation.targetId.takeIf { it.isNotBlank() }
                            currentTab = 4
                        }
                        "MODERATION" -> {
                            if (isAdminAccount) {
                                val matchedNotif = notifications.firstOrNull { it.id == navigation.notificationId || it.targetId == navigation.targetId }
                                adminTab = adminTabForAlert(matchedNotif?.title.orEmpty(), matchedNotif?.body.orEmpty())
                                adminRefreshSignal++
                            } else {
                                showToast("Bu bildirim yönetici paneline özeldir.", "ℹ️")
                            }
                        }
                        "RESERVATIONS" -> showMyActivity = true
                        "VENUE_MANAGEMENT" -> showVenueOperator = true
                    }
                } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                catch (error: Exception) { backendError = (error as? BackendFailure)?.message ?: "Yönlendirme açılamadı." }
                DeviceEventBridge.consumeNavigation()
            }
        }
    }
    LaunchedEffect(currentTab) {
        if (currentTab != 4) {
            focusedMemoryId = null
            chatEntrySection = ChatHubSection.MESSAGES
        }
    }
    LaunchedEffect(selectedDetailEvent?.id, backend.enabled) {
        val targetId = selectedDetailEvent?.id
        if (targetId == null || !backend.enabled) {
            if (targetId == null) selectedDetailPlayers = emptyList()
            return@LaunchedEffect
        }
        runCatching {
            val detail = backend.eventDetails(targetId)
            val parsed = BackendSnapshot(apiPayload("userId" to backend.userId, "events" to listOf(detail), "players" to detail.a("players")))
            if (selectedDetailEvent?.id == targetId) {
                selectedDetailPlayers = parsed.players
                parsed.events.firstOrNull()?.let { selectedDetailEvent = it }
            }
        }
    }
    LaunchedEffect(backend, appActive, isLoggedIn, live?.userId) {
        if (backend.enabled && appActive && isLoggedIn) backend.runRealtime()
    }
    LaunchedEffect(backend, appActive, isLoggedIn, live?.userId) {
        if (backend.enabled && appActive && isLoggedIn) {
            while (true) {
                try { backend.processPendingSends() }
                catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                catch (error: Exception) { backendError = error.message ?: "Bekleyen gönderimler okunamadı." }
                kotlinx.coroutines.delay(5000L)
            }
        }
    }
    LaunchedEffect(backend, appActive, activeChatThreadId) {
        if (backend.enabled && appActive) {
            try { backend.setActiveThread(activeChatThreadId) }
            catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (error: Exception) {
                if (activeChatThreadId != null) {
                    showToast((error as? BackendFailure)?.message ?: "Sohbet akışı şu an yenilenemedi.", "⚠️")
                }
            }
            var offlineNotified = false
            while (true) {
                kotlinx.coroutines.delay(if (activeChatThreadId != null && !backend.realtimeConnected.value) 3000L else 20000L)
                try {
                    backend.poll()
                    offlineNotified = false
                }
                catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                catch (error: Exception) {
                    if (error is BackendFailure && error.code == "OFFLINE" && !offlineNotified) {
                        offlineNotified = true
                        showToast(error.message ?: "İnternet bağlantısı yok · Çevrimdışı", "⚠️")
                    }
                }
            }
        }
    }
    LaunchedEffect(backendWire, currentDeviceLocation) {
        val data = live
        if (data == null) {
            if (backend.enabled && !backend.authenticated) {
                isLoggedIn = false; hasCompletedPassport = false; profile = guestProfile
                threads = emptyList(); notifications = emptyList(); ledger = emptyList(); venuePayments = emptyList()
                tokenBalance = 0; lockedTokens = 0; activeProPlan = null; supportRequests = emptyList(); moderationReports = emptyList()
                joinRequests = emptyList(); teamJoinRequests = emptyList(); joinedEventIds = emptySet(); checkedInEventIds = emptySet()
                registeredTournamentIds = emptySet(); challenges = emptyList(); matchResults = emptyMap(); activeChatThreadId = null
                events = emptyList(); teams = emptyList(); tournaments = emptyList(); remotePlayers = emptyList(); memoryPosts = emptyList()
                remoteCommunities = emptyList(); remoteVenues = emptyList(); joinedCommunityIds = emptySet(); requestedCommunityIds = emptySet()
            }
            return@LaunchedEffect
        }
        val serverIsAdmin = data.wire.s("role") == "admin"
        isLoggedIn = !sessionLeaving && backend.authenticated && (data.profile != null || serverIsAdmin)
        hasCompletedPassport = data.profile != null || serverIsAdmin
        data.profile?.let {
            if (!browseRegionSelected && (profile.city != it.city || profile.area != it.area)) { currentCity = it.city; currentDistrict = it.area }
            profile = it
        }
        if (!browseRegionSelected) data.wire.o("discovery").takeIf { it.isNotEmpty() }?.let { currentCity = it.s("city"); currentDistrict = it.s("district") }
        tokenBalance = data.balance; lockedTokens = data.locked
        val activeDistanceLocation = currentDeviceLocation?.takeIf { it.matchesSelectedRegion(currentCity, currentDistrict, isGpsLocationActive) }
        events = data.events.map { event -> event.approximateLocation?.let { point -> activeDistanceLocation?.let { location -> event.copy(distanceKm = approximateDistanceKm(location, point.latitude, point.longitude)) } } ?: event }; joinedEventIds = data.joinedIds; checkedInEventIds = data.checkedInIds; waitlistedEventIds = data.waitlistedIds
        joinRequests = data.joinRequests; teamJoinRequests = data.teamRequests; teams = data.teams; tournaments = data.tournaments; challenges = data.challenges
        threads = data.threads; memoryPosts = data.memories; notifications = data.notifications; ledger = data.ledger
        venuePayments = data.venuePayments; bookedSlotIds = data.bookedSlotIds; activeProPlan = data.activePlan
        privacyPrefs = data.privacy; notificationPrefs = data.notificationPrefs; blockedChatIds = data.blockedThreadIds
        supportRequests = data.support; moderationReports = data.reports; matchResults = data.results
        registeredTournamentIds = data.registeredTournamentIds; invitedPlayerIds = data.invitedIds
        joinedCommunityIds = data.joinedCommunityIds; requestedCommunityIds = data.requestedCommunityIds
        remotePlayers = data.players; remoteCommunities = data.communities; remoteVenues = data.venues.map { venue ->
            venue.approximateLocation?.let { point -> activeDistanceLocation?.let { location -> venue.copy(distanceKm = approximateDistanceKm(location, point.latitude, point.longitude)) } } ?: venue
        }
        if (selectedDetailEvent != null) selectedDetailEvent = data.events.firstOrNull { it.id == selectedDetailEvent?.id } ?: selectedDetailEvent
        if (showManageJoinRequestsForEvent != null) showManageJoinRequestsForEvent = data.events.firstOrNull { it.id == showManageJoinRequestsForEvent?.id }
        if (data.avatarUploadId != null) runCatching { backend.photo(data.avatarUploadId) }.getOrNull()?.let { image -> if (backend.userId == data.userId) profile = profile.copy(avatar = image) }
    }

    fun activateDemoPlan(plan: ProPlan) {
        if (backend.enabled) { showToast("Abonelik satın alma şu anda kullanılamıyor.", "ℹ️"); return }
        if (activeProPlan != null) return
        activeProPlan = plan
        tokenBalance += 100
        ledger = listOf(LedgerEntry(nextUniqueId("l_sub"), LedgerType.BONUS, 100,
            "Matchora Plus (${plan.title}) · Aylık token", "Şimdi", idempotencyKey = "subscription-${plan.sku}")) + ledger
        showToast("Matchora Plus demo üyeliğin aktif · 100 TKN eklendi", "⭐")
    }

    fun lockTokens(amount: Int, reason: String): Boolean {
        if (amount <= 0) return true
        if (tokenBalance < amount) {
            showToast("$reason için $amount TKN gerekli", "💎")
            showTokenTopUpSheet = true
            return false
        }
        tokenBalance -= amount
        lockedTokens += amount
        return true
    }

    fun releaseTokens(amount: Int): Int {
        val released = amount.coerceAtLeast(0).coerceAtMost(lockedTokens)
        tokenBalance += released
        lockedTokens -= released
        return released
    }

    fun releaseEventTokens(eventId: String): Int {
        val amount = eventTokenHolds[eventId] ?: return 0
        eventTokenHolds = eventTokenHolds - eventId
        return releaseTokens(amount)
    }

    fun joinEventImmediately(event: SportEvent, reason: String, bypassApproval: Boolean = false) {
        val isInvited = bypassApproval || (directInvitedEventId == event.id)
        if (event.approvalRequired && !isInvited) { showSubmitJoinSheetForEvent = event; return }
        val joinPayload = if (isInvited) {
            apiPayload("eventId" to event.id, "bypassApproval" to true, "invitationId" to event.id)
        } else {
            apiPayload("eventId" to event.id)
        }
        if (remote("event.join", joinPayload)) return
        if (joinedEventIds.contains(event.id)) {
            showToast("Zaten bu karşılaşmanın kadrosundasın", "ℹ️")
            return
        }
        if (event.status !in setOf(EventStatus.PUBLISHED, EventStatus.FILLING) || event.freeSpots <= 0) {
            showToast("Bu karşılaşmada boş yer kalmadı", "ℹ️")
            return
        }
        if (!lockTokens(10, if (isInvited) "Davetli Katılım" else "Katılım")) return
        eventTokenHolds = eventTokenHolds + (event.id to 10)

        joinedEventIds = joinedEventIds + event.id
        events = events.map { current ->
            if (current.id != event.id) current
            else {
                val participantIds = (current.participantIds + DemoData.ME).distinct()
                current.copy(
                    participantIds = participantIds,
                    status = if (participantIds.size >= current.capacity) EventStatus.FULL else current.status
                )
            }
        }
        selectedDetailEvent = events.firstOrNull { it.id == event.id } ?: event
        val operationId = nextUniqueId("op_join")
        ledger = listOf(
            LedgerEntry(nextUniqueId("l_hold"), LedgerType.HOLD, -10, "${if (isInvited) "Davetli Katılım" else reason} · ${event.title}", "Şimdi", idempotencyKey = operationId)
        ) + ledger
        showToast(if (isInvited) "Özel davet koduyla doğrudan kadroya katıldın 🎉 (10 TKN kilitlendi)" else "10 TKN kilitlendi · ${event.title} kadrosuna katıldın", "✓")
    }

    fun openDirectChatWithPlayer(targetPlayer: Player) {
        if (remote("chat.direct", apiPayload("userId" to backend.serverUserId(targetPlayer.id))) { result -> activeChatThreadId = result["id"]?.jsonPrimitive?.content; chatEntrySection = ChatHubSection.MESSAGES; focusedMemoryId = null; selectedDetailEvent = null; currentTab = 4 }) return
        val existingThread = threads.firstOrNull { it.type == ThreadType.DIRECT && it.title == targetPlayer.name }
        val threadId = if (existingThread != null) {
            existingThread.id
        } else {
            val newId = nextUniqueId("ch_dm")
            val newThread = ChatThread(
                id = newId,
                type = ThreadType.DIRECT,
                title = targetPlayer.name,
                emoji = "💬",
                memberCount = 2,
                messages = listOf(
                    ChatMsg(
                        id = nextUniqueId("m_dm_sys"),
                        senderId = "sys",
                        senderName = "Matchora",
                        text = "${targetPlayer.name} ile bireysel sohbet başlatıldı.",
                        time = "Şimdi",
                        isSystem = true
                    )
                ),
                unread = 0,
                locked = false
            )
            threads = listOf(newThread) + threads
            newId
        }
        activeChatThreadId = threadId
        chatEntrySection = ChatHubSection.MESSAGES
        focusedMemoryId = null
        selectedDetailEvent = null
        showSubmitJoinSheetForEvent = null
        showManageJoinRequestsForEvent = null
        currentTab = 4 // Sohbetler sekmesi
        showToast("${targetPlayer.name} ile sohbet açıldı", "💬")
    }

    fun approveJoinRequest(req: JoinRequest) {
        if (remote("event.request.review", apiPayload("requestId" to req.id, "approve" to true))) return
        if (req.status != JoinRequestStatus.PENDING) return
        val targetEvent = events.firstOrNull { it.id == req.eventId }
        if (targetEvent == null || (!targetEvent.participantIds.contains(req.player.id) && targetEvent.freeSpots <= 0)) {
            showToast("Kadro dolu; başvuru onaylanamadı", "ℹ️")
            return
        }
        joinRequests = joinRequests.map {
            if (it.id == req.id) it.copy(status = JoinRequestStatus.APPROVED) else it
        }
        events = events.map { ev ->
            if (ev.id == req.eventId && !ev.participantIds.contains(req.player.id)) {
                val participantIds = ev.participantIds + req.player.id
                ev.copy(
                    participantIds = participantIds,
                    status = if (participantIds.size >= ev.capacity) EventStatus.FULL else ev.status
                )
            } else ev
        }
        if (selectedDetailEvent?.id == req.eventId) {
            selectedDetailEvent = events.firstOrNull { it.id == req.eventId }
        }
        if (req.player.id == DemoData.ME) joinedEventIds = joinedEventIds + req.eventId
        showToast("${req.player.name} kadroya dahil edildi!", "✓")
    }

    fun rejectJoinRequest(req: JoinRequest) {
        if (remote("event.request.review", apiPayload("requestId" to req.id, "approve" to false))) return
        if (req.status != JoinRequestStatus.PENDING) return
        joinRequests = joinRequests.map {
            if (it.id == req.id) it.copy(status = JoinRequestStatus.REJECTED) else it
        }
        showToast("${req.player.name} başvurusu reddedildi", "✕")
    }

    fun submitJoinRequest(ev: SportEvent, position: String, note: String) {
        if (remote("event.request", apiPayload("eventId" to ev.id, "position" to position, "note" to note))) return
        if (joinedEventIds.contains(ev.id)) {
            showToast("Zaten bu karşılaşmanın kadrosundasın", "ℹ️")
            return
        }
        if (ev.status !in setOf(EventStatus.PUBLISHED, EventStatus.FILLING) || ev.freeSpots <= 0) {
            showToast("Bu karşılaşma artık başvuru almıyor", "ℹ️")
            return
        }
        if (joinRequests.any { it.eventId == ev.id && it.player.id == DemoData.ME && it.status == JoinRequestStatus.PENDING }) {
            showToast("Bu karşılaşma için bekleyen bir başvurun var", "ℹ️")
            return
        }
        if (!lockTokens(10, "Başvuru")) return
        eventTokenHolds = eventTokenHolds + (ev.id to 10)
        val requestId = nextUniqueId("jr")
        val newReq = JoinRequest(
            id = requestId,
            eventId = ev.id,
            player = currentPlayer,
            requestedPosition = position,
            message = if (note.isBlank()) null else note,
            timestamp = "Şimdi",
            status = JoinRequestStatus.PENDING
        )
        joinRequests = joinRequests + newReq
        ledger = listOf(
            LedgerEntry(nextUniqueId("l_hold"), LedgerType.HOLD, -10, "Kadro Başvurusu · ${ev.title}", "Şimdi", idempotencyKey = "hold-$requestId")
        ) + ledger
        showToast("Demo başvurun iletildi · kaptanın yanıtı bekleniyor", "⏳")
        demoScope.launch {
            delay(4000)
            val pending = joinRequests.firstOrNull { it.id == requestId && it.status == JoinRequestStatus.PENDING }
            if (pending != null && events.any { it.id == ev.id && it.freeSpots > 0 }) {
                approveJoinRequest(pending)
                notifications = listOf(AppNotification(nextUniqueId("n_demo"), NotifPriority.P1, "✓",
                    "Demo başvurun onaylandı", "${ev.title} kadrosundasın. Sohbet ve check-in hazır.", "Şimdi", eventId = ev.id)) + notifications
            }
        }
    }

    fun cancelJoinRequest(req: JoinRequest) {
        if (remote("event.request.cancel", apiPayload("requestId" to req.id))) return
        if (joinRequests.none { it.id == req.id && it.status == JoinRequestStatus.PENDING }) return
        joinRequests = joinRequests.filter { it.id != req.id }
        val released = releaseEventTokens(req.eventId)
        ledger = listOf(
            LedgerEntry(nextUniqueId("l_rel"), LedgerType.RELEASE, released, "Kadro başvuru iadesi", "Şimdi", idempotencyKey = "rel-${req.id}")
        ) + ledger
        showToast("Başvurun geri çekildi · 10 TKN iade edildi", "↩️")
    }

    fun createTeam(newTeam: TeamProfile) {
        if (remote("team.create", apiPayload("name" to newTeam.name, "sport" to newTeam.sport, "city" to newTeam.city.ifBlank { currentCity }, "area" to newTeam.area, "homeVenue" to newTeam.homeVenue, "networkScope" to newTeam.networkScope.name, "description" to newTeam.description, "availability" to newTeam.availability, "recruitingOpen" to newTeam.recruitingOpen, "requiredMmr" to newTeam.requiredMmr, "badge" to newTeam.badge, "colorHex" to ("#" + newTeam.color.toArgb().toUInt().toString(16).padStart(8, '0')))) { result -> competitionEntryTab = 2; competitionFocusId = result.s("id") }) return
        teams = listOf(newTeam) + teams
        showToast("${newTeam.name} kulübü kuruldu!", "🛡️")
    }

    fun sendTeamChallenge(challenge: ChallengeOffer) {
        if (remote("challenge.create", apiPayload("fromTeamId" to challenge.fromTeamId, "toTeamId" to challenge.toTeamId, "proposedTime" to challenge.proposedTime, "venue" to challenge.venue, "stakeTokens" to challenge.stakeTokens, "note" to challenge.note.orEmpty(), "startsAtEpochMillis" to challenge.startsAtEpochMillis))) return
        if (challenges.any { it.id == challenge.id }) return
        if (challenge.stakeTokens > 0 && !lockTokens(challenge.stakeTokens, "Meydan okuma")) return
        challenges = listOf(challenge) + challenges
        if (challenge.stakeTokens > 0) {
            ledger = listOf(
                LedgerEntry(nextUniqueId("l_stake"), LedgerType.HOLD, -challenge.stakeTokens, "Meydan Okuma Kilidi", "Şimdi", idempotencyKey = "stake-${challenge.id}")
            ) + ledger
        }
        val target = teams.firstOrNull { it.id == challenge.toTeamId }
        showToast("${target?.name ?: "Rakip takıma"} meydan okuma iletildi!", "⚔️")
    }

    fun acceptTeamChallenge(challenge: ChallengeOffer) {
        if (remote("challenge.respond", apiPayload("challengeId" to challenge.id, "accept" to true))) return
        challenges = challenges.map {
            if (it.id == challenge.id) it.copy(status = ChallengeStatus.SCHEDULED) else it
        }
        val fromTeam = teams.firstOrNull { it.id == challenge.fromTeamId }
        val toTeam = teams.firstOrNull { it.id == challenge.toTeamId }
        val newEvent = SportEvent(
            id = nextUniqueId("ev_derby"),
            title = "${fromTeam?.name ?: "Takım"} vs ${toTeam?.name ?: "Takım"} (Resmi Derbi)",
            sport = fromTeam?.sport ?: com.matchora.app.domain.Sport.FOOTBALL,
            venue = challenge.venue,
            area = "Kadıköy",
            day = challenge.proposedTime,
            time = "21:00",
            startsInMin = 360,
            durationMin = 60,
            distanceKm = 1.0f,
            capacity = 14,
            participantIds = listOf("me", "p1", "p2", "p3", "p4", "p5"),
            waitlistCount = 0,
            mode = PlayMode.COMPETITIVE,
            levelRange = "İleri",
            minReliability = 85,
            verifiedOnly = true,
            pricePerPersonTry = 150,
            hostId = "me",
            status = EventStatus.FILLING,
            score = ScoreBreakdown(95, 90, 92, 95, 96, listOf("Resmi Takım Meydan Okuması", "MMR Puanına Etki Eder")),
            description = "Resmi kulüp meydan okuması. Sonuçlar takım MMR ve lig tablosuna doğrudan işlenir."
        )
        events = listOf(newEvent) + events
        showToast("Meydan okuma kabul edildi! Maç takvime eklendi ⚔️", "✓")
    }

    fun declineTeamChallenge(challenge: ChallengeOffer) {
        if (remote("challenge.respond", apiPayload("challengeId" to challenge.id, "accept" to false))) return
        challenges = challenges.map {
            if (it.id == challenge.id) it.copy(status = ChallengeStatus.DECLINED) else it
        }
        showToast("Meydan okuma reddedildi", "✕")
    }

    fun applyToTeam(team: TeamProfile, position: String, note: String) {
        if (remote("team.request", apiPayload("teamId" to team.id, "position" to position, "note" to note))) return
        val newReq = TeamJoinRequest(
            id = nextUniqueId("tjr"),
            teamId = team.id,
            player = currentPlayer,
            preferredPosition = position,
            message = if (note.isBlank()) null else note,
            timestamp = "Şimdi",
            status = JoinRequestStatus.PENDING
        )
        teamJoinRequests = listOf(newReq) + teamJoinRequests
        showToast("${team.name} kadrosuna başvurun iletildi! 👥", "⏳")
    }

    fun approveTeamJoinRequest(req: TeamJoinRequest) {
        if (remote("team.request.review", apiPayload("requestId" to req.id, "approve" to true))) return
        teamJoinRequests = teamJoinRequests.map {
            if (it.id == req.id) it.copy(status = JoinRequestStatus.APPROVED) else it
        }
        teams = teams.map { team ->
            if (team.id == req.teamId && !team.memberIds.contains(req.player.id)) {
                team.copy(memberIds = team.memberIds + req.player.id)
            } else team
        }
        showToast("${req.player.name} takım kadrosuna dahil edildi!", "✓")
    }

    fun rejectTeamJoinRequest(req: TeamJoinRequest) {
        if (remote("team.request.review", apiPayload("requestId" to req.id, "approve" to false))) return
        teamJoinRequests = teamJoinRequests.map {
            if (it.id == req.id) it.copy(status = JoinRequestStatus.REJECTED) else it
        }
        showToast("${req.player.name} başvurusu reddedildi", "✕")
    }

    val unreadInActiveThread = backendWire?.a("threads")?.firstOrNull { it.s("id") == activeChatThreadId && !it.b("locked") }?.i("unread") ?: 0
    LaunchedEffect(activeChatThreadId, unreadInActiveThread, appActive, currentTab) {
        if (backend.enabled && appActive && currentTab == 4 && activeChatThreadId != null && unreadInActiveThread > 0) {
            try { backend.command("chat.read", apiPayload("threadId" to activeChatThreadId), backend.requestKey("visible-read")) }
            catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (_: Exception) { /* Polling retries after the next server snapshot. */ }
        }
    }
    CompositionLocalProvider(
        LocalBackendAction provides { operation, payload -> remote(operation, payload); Unit },
        LocalToastNotifier provides ::showToast
    ) {
    if (showVenueOperator) VenueOperatorSheet { showVenueOperator = false }
    if (showModeration) ModerationSheet { showModeration = false }
    // Otomatik Toast Kapanışı
    LaunchedEffect(currentToast) {
        if (currentToast != null) {
            delay(2800)
            currentToast = null
        }
    }

    val featuresLocked = sessionLeaving || !isLoggedIn || !hasCompletedPassport || authWire?.b("legalAcceptanceRequired") == true
    val pendingRequestEventIds = if (!isLoggedIn) emptySet() else joinRequests.filter { it.player.id == DemoData.ME && it.status == JoinRequestStatus.PENDING }.map { it.eventId }.toSet()
    LaunchedEffect(isAdminSession, notifications.firstOrNull()?.id, showVenueOperator) {
        if (isAdminSession && !showVenueOperator) adminRefreshSignal++
    }
    Box(Modifier.fillMaxSize()) {
    Box(Modifier.fillMaxSize()) {
    if (isAdminSession) {
        AdminConsoleScreen(
            selectedTab = adminTab,
            onSelectTab = { adminTab = it },
            unreadNotificationsCount = unreadNotificationsCount,
            refreshSignal = adminRefreshSignal,
            onOpenNotifications = { showNotificationsSheet = true },
            onOpenVenueOperator = { showVenueOperator = true },
            onLogout = { remoteLogout() }
        )
    } else {
    Scaffold(
        containerColor = Color.Transparent, // Şeffaf zemin: Arkadaki akıcı spor desenleri görünür!
        topBar = {
            if (currentTab != 1 && !(currentTab == 4 && activeChatThreadId != null)) {
                MTopBar(
                    currentCity = currentCity,
                    currentDistrict = currentDistrict,
                    isGpsActive = isGpsLocationActive,
                    tokenBalance = tokenBalance,
                    unreadNotifications = if (isLoggedIn) unreadNotificationsCount else 0,
                    isMenuActive = currentTab in setOf(2, 3) || showMenuSheet,
                    isLoggedIn = isLoggedIn,
                    onOpenAuth = { showAuthSheet = true },
                    onOpenWallet = { requireAccount { showWalletSheet = true  }},
                    onOpenNotifications = { requireAccount { showNotificationsSheet = true } },
                    onOpenMenu = { showMenuSheet = true },
                    onOpenProfile = {
                        if (currentTab == 5) scrollToTopTrigger++ else currentTab = 5
                    },
                    onCityClick = { showCityLocationSheet = true }
                )
            }
        },
        bottomBar = {
            if (!(currentTab == 4 && activeChatThreadId != null)) {
                FloatingAppDock(
                    selectedTab = currentTab,
                    unreadChats = if (isLoggedIn) unreadChatThreadsCount else 0,
                    onSelectTab = { clickedTab ->
                        if (clickedTab in setOf(2, 3)) {
                            requireAccount {
                                if (currentTab == clickedTab) scrollToTopTrigger++ else currentTab = clickedTab
                            }
                        } else if (currentTab == clickedTab) {
                            scrollToTopTrigger++
                        } else {
                            currentTab = clickedTab
                        }
                    },
                    onPlayNowClick = { requireAccount { showPlayNowSheet = true  }}
                )
            }
        }
    ) { paddingValues ->
        CompositionLocalProvider(
            LocalScrollToTopTrigger provides scrollToTopTrigger,
            LocalFeaturesLocked provides featuresLocked,
            LocalRequireAccount provides ::requireAccount
        ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Ana Sekmeler
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    (fadeIn(tween(170)) + slideInHorizontally(tween(220)) { it / 28 }) togetherWith
                        (fadeOut(tween(110)) + slideOutHorizontally(tween(170)) { -it / 40 })
                },
                label = "main_navigation"
            ) { tab ->
                when (tab) {
                0 -> HomeScreen(
                    paddingValues = paddingValues,
                    events = events,
                    currentCity = currentCity,
                    onOpenLocation = { showCityLocationSheet = true },
                    user = profile,
                    currentDistrict = currentDistrict,
                    currentPlayer = currentPlayer,
                    players = if (backend.enabled) remotePlayers else DemoData.players,
                    joinedEventIds = joinedEventIds,
                    waitlistedEventIds = waitlistedEventIds,
                    pendingRequestEventIds = pendingRequestEventIds,
                    onSelectEvent = { selectedDetailEvent = it },
                    onFindEventById = { requireAccount { openEventById(it, grantDirectInvite = true) } },
                    onNetworkScopeChanged = { scope ->
                        initialListingScope = scope
                        if (backend.enabled && isLoggedIn) remote("discovery.save", apiPayload("city" to currentCity, "district" to currentDistrict, "travelMode" to (scope == EventNetworkScope.TRAVEL)), silent = true)
                    },
                    onOpenMap = { currentTab = 1 },
                    onOpenPlayNow = { requireAccount { showPlayNowSheet = true  }},
                    onOpenEmergencyFill = {
                        requireAccount {
                            val em = events.firstOrNull { it.emergency }
                            if (em != null) selectedDetailEvent = em
                        }
                    },
                    onOpenAutoSquad = { selectedMatch -> requireAccount {
                        val squad = threads.firstOrNull { if (backend.enabled) it.relatedEventId == selectedMatch?.id && selectedMatch?.id in joinedEventIds else it.type == ThreadType.SQUAD }
                        if (squad != null) {
                            activeChatThreadId = squad.id
                            chatEntrySection = ChatHubSection.MESSAGES
                            focusedMemoryId = null
                            currentTab = 4
                            showToast("Maç sohbeti açıldı", "✨")
                        } else {
                            chatEntrySection = ChatHubSection.MESSAGES
                            focusedMemoryId = null
                            currentTab = 4
                            selectedMatch?.let { selectedDetailEvent = it }; showToast("Sohbete katılmak için maç kadrosuna katılmalısın", "ℹ️")
                        }
                     }},
                    onOpenTeamChallenge = { requireAccount { competitionEntryTab = 2; currentTab = 2 } },
                    onOpenLeaderboard = { requireAccount { competitionEntryTab = 0; currentTab = 2 } },
                    onOpenWallet = { requireAccount { showWalletSheet = true  }},
                    onOpenProfile = { currentTab = 5 },
                    onOpenCommunities = { requireAccount { currentTab = 3 } }
                )
                1 -> DiscoverMapScreen(
                    paddingValues = paddingValues,
                    events = events,
                    user = profile,
                    currentCity = currentCity,
                    currentDistrict = currentDistrict,
                    isGpsActive = isGpsLocationActive,
                    joinedEventIds = joinedEventIds,
                    waitlistedEventIds = waitlistedEventIds,
                    pendingRequestEventIds = pendingRequestEventIds,
                    onLocationClick = { showCityLocationSheet = true },
                    onSelectEvent = { selectedDetailEvent = it },
                    onQuickJoin = { ev -> requireAccount {
                        if (ev.approvalRequired) {
                            showSubmitJoinSheetForEvent = ev
                        } else joinEventImmediately(ev, "Taahhüt")
                     }},
                    onToggleWaitlist = { ev -> if (!remote("event.waitlist", apiPayload("eventId" to ev.id, "action" to live?.waitlistAction(ev.id)))) { requireAccount {
                        if (waitlistedEventIds.contains(ev.id)) {
                            waitlistedEventIds = waitlistedEventIds - ev.id
                            events = events.map { current ->
                                if (current.id == ev.id) current.copy(waitlistCount = (current.waitlistCount - 1).coerceAtLeast(0)) else current
                            }
                            showToast("Bekleme listesinden ayrıldın", "↩️")
                        } else if (ev.status == EventStatus.FULL) {
                            waitlistedEventIds = waitlistedEventIds + ev.id
                            events = events.map { current -> if (current.id == ev.id) current.copy(waitlistCount = current.waitlistCount + 1) else current }
                            showToast("Bekleme listesine eklendin · yer açılınca haber vereceğiz", "⏳")
                        }
                     } } }
                )
                2 -> CompetitionScreen(
                    paddingValues = paddingValues,
                    initialTab = competitionEntryTab,
                    focusedId = competitionFocusId,
                    onFocusConsumed = { competitionFocusId = null },
                    onOpenFixture = { requireAccount { openEventById(it) } },
                    remotePlayers = if (backend.enabled) remotePlayers else null,
                    remoteRankings = if (backend.enabled) live?.rankings.orEmpty() else null,
                    backendMode = backend.enabled,
                    currentCity = currentCity,
                    currentDistrict = currentDistrict,
                    venues = if (backend.enabled) remoteVenues else DemoData.venues,
                    teams = teams,
                    currentPlayer = currentPlayer,
                    user = profile,
                    tournaments = tournaments,
                    registeredTournamentIds = registeredTournamentIds,
                    invitedPlayerIds = invitedPlayerIds,
                    tokenBalance = tokenBalance,
                    newTournamentId = { nextUniqueId("tr") },
                    onRequestTokens = {
                        requireAccount { showTokenTopUpSheet = true }
                    },
                    challenges = challenges,
                    teamJoinRequests = teamJoinRequests,
                    onCreateTeam = { requireAccount { createTeam(it)  }},
                    onCreatedTeam = { id -> competitionEntryTab = 2; competitionFocusId = id; showToast("Kulübün oluşturuldu.", "🛡️") },
                    onCreatedTournament = { id -> competitionEntryTab = 1; competitionFocusId = id; showToast("Turnuvan yayınlandı.", "🏆") },
                    onOpenChallengeSheet = { source, target -> requireAccount { challengeSourceTeam = source; challengeTargetTeam = target }},
                    onAcceptChallenge = { requireAccount { acceptTeamChallenge(it)  }},
                    onDeclineChallenge = { requireAccount { declineTeamChallenge(it)  }},
                    onApplyToTeam = { team, pos, note -> requireAccount { applyToTeam(team, pos, note)  }},
                    onApproveTeamJoinRequest = { requireAccount { approveTeamJoinRequest(it)  }},
                    onRejectTeamJoinRequest = { requireAccount { rejectTeamJoinRequest(it)  }},
                    onOpenDirectChat = { requireAccount { openDirectChatWithPlayer(it)  }},
                    onInvitePlayerToTeam = { player, team -> if (!(remote("team.invite", apiPayload("teamId" to team.id, "userId" to backend.serverUserId(player.id))) {
                        invitedPlayerIds = invitedPlayerIds + player.id
                    })) { requireAccount {
                        invitedPlayerIds = invitedPlayerIds + player.id
                        val teamName = team.name
                        showToast("${player.name}, $teamName kadrosuna davet edildi", "🛡️")
                     } } },
                    onShareTeam = { team ->
                        shareText("Matchora Kulüp Daveti", teamInvitationText(team.name, team.id, team.sport.label, team.area, team.badge))
                        rewardShareBonus("Kulüp Paylaşım Bonusu · ${team.name}")
                    },
                    onShareTournament = { tourney ->
                        shareText("Matchora Turnuva Daveti", tournamentInvitationText(tourney.name, tourney.id, tourney.dates, tourney.format, tourney.organizer))
                        rewardShareBonus("Turnuva Paylaşım Bonusu · ${tourney.name}")
                    },
                    onCreateTournament = { tournament -> if (!(remote("tournament.create", apiPayload("name" to tournament.name, "sport" to tournament.sport, "format" to tournament.format, "capacity" to tournament.capacity, "dates" to tournament.dates, "requirement" to tournament.requirement, "feeNote" to tournament.feeNote, "isLeague" to tournament.isLeague, "city" to tournament.city.ifBlank { currentCity }, "area" to tournament.area.ifBlank { currentDistrict }, "networkScope" to tournament.networkScope.name, "venue" to tournament.venue, "prizeToken" to tournament.prizeToken, "prizeCash" to tournament.prizeCash, "customSportName" to tournament.customSportName, "organizerName" to tournament.organizer, "gamePlatform" to tournament.gamePlatform, "serverRegion" to tournament.serverRegion, "formatCode" to tournament.formatCode, "minRating" to tournament.minRating, "verifiedOnly" to tournament.verifiedOnly, "rosterSize" to tournament.rosterSize)) { result -> competitionEntryTab = 1; competitionFocusId = result.s("id") })) { requireAccount {
                        tournaments = listOf(tournament) + tournaments
                        val lockAmount = tournament.organizerLockTokens
                        tokenBalance -= lockAmount
                        lockedTokens += lockAmount
                        ledger = listOf(
                            LedgerEntry(
                                id = nextUniqueId("l_tournament_hold"),
                                type = LedgerType.HOLD,
                                amount = -lockAmount,
                                description = "Turnuva kilidi · ${tournament.name}",
                                time = "Şimdi",
                                ref = tournament.id,
                                idempotencyKey = "hold-${tournament.id}"
                            )
                        ) + ledger
                        showToast("${tournament.name} oluşturuldu · $lockAmount TKN kilitlendi", "🏆")
                     } } },
                    onTournamentRegistered = { tournament -> requireAccount { tournamentTeamChoice = tournament to false } },
                    onWithdrawTournamentRegistration = { tournament -> requireAccount { tournamentTeamChoice = tournament to true } },
                    onCancelTournament = { tournament -> if (!(remote("tournament.cancel", apiPayload("tournamentId" to tournament.id)))) { requireAccount {
                        val lockAmount = tournament.organizerLockTokens
                        tournaments = tournaments.map { current ->
                            if (current.id == tournament.id) current.copy(status = "İptal edildi", organizerLockTokens = 0) else current
                        }
                        registeredTournamentIds = registeredTournamentIds - tournament.id
                        if (lockAmount > 0) {
                            lockedTokens = (lockedTokens - lockAmount).coerceAtLeast(0)
                            tokenBalance += lockAmount
                            ledger = listOf(
                                LedgerEntry(
                                    id = nextUniqueId("l_tournament_release"),
                                    type = LedgerType.RELEASE,
                                    amount = lockAmount,
                                    description = "Turnuva kilidi iade · ${tournament.name}",
                                    time = "Şimdi",
                                    ref = tournament.id,
                                    idempotencyKey = "release-${tournament.id}"
                                )
                            ) + ledger
                        }
                        showToast("Turnuva iptal edildi · $lockAmount TKN iade edildi", "↩️")
                     } } }
                )
                3 -> CommunityAndVenueScreen(
                    paddingValues = paddingValues,
                    initialTab = networkEntryTab,
                    focusedId = networkTargetId,
                    currentDistrict = currentDistrict,
                    communities = if (backend.enabled) remoteCommunities else previewCommunities,
                    currentCity = currentCity,
                    deviceLocation = currentDeviceLocation,
                    onShareVenue = { venue ->
                        shareText("Matchora Tesis Paylaşımı", venueInvitationText(venue.name, venue.id, venue.area, currentCity))
                        rewardShareBonus("Tesis Paylaşım Bonusu · ${venue.name}")
                    },
                    onShareCommunity = { community ->
                        shareText("Matchora Topluluk Daveti", communityInvitationText(community.name, community.id, community.area, community.type.label))
                        rewardShareBonus("Topluluk Paylaşım Bonusu · ${community.name}")
                    },
                    onDirections = { venue ->
                        val point = venue.approximateLocation?.takeIf { validMapCoordinate(it.latitude, it.longitude) }
                        val query = buildString {
                            append(venue.name)
                            if (venue.address.isNotBlank()) append(", ${venue.address}")
                            else append(", ${venue.area}, ${venue.city.ifBlank { currentCity }}")
                        }
                        if (!openLocationDirections(point?.latitude, point?.longitude, query)) {
                            showToast("Harita uygulaması açılamadı", "🗺️")
                        }
                    },
                    onReportReview = { id, reason, details ->
                        requireAccount {
                            if (!remote("report.create", apiPayload("targetType" to "venue_review", "targetId" to id, "reason" to reason, "details" to details)))
                                moderationReports = listOf(ModerationReport(nextUniqueId("report"), "Tesis değerlendirmesi · $id", reason, details)) + moderationReports
                            showToast("Bildirim kaydın Hesap → Destek bölümünde", "🛡️")
                        }
                    },
                    onPublishedCommunity = { id -> networkEntryTab = 0; networkTargetId = id; showToast("Topluluğun oluşturuldu.", "🤝") },
                    onCreateCommunity = { community -> if (!remote("community.create", apiPayload("name" to community.name, "type" to community.type, "access" to community.access, "city" to community.city, "area" to community.area, "description" to community.description, "sport" to community.sports.first(), "sports" to community.sports.map { it.name }))) requireAccount { previewCommunities = listOf(community) + previewCommunities; joinedCommunityIds = joinedCommunityIds + community.id; showToast("${community.name} topluluğu oluşturuldu", "🤝") } },
                    onOpenCommunityChat = { com ->
                        if (!remote("community.chat", apiPayload("communityId" to com.id)) { res ->
                            activeChatThreadId = res.s("id")
                            chatEntrySection = ChatHubSection.MESSAGES
                            focusedMemoryId = null
                            currentTab = 4
                        }) {
                            requireAccount {
                                val threadId = "th_com_${com.id}"
                                if (threads.none { it.id == threadId }) {
                                    threads = listOf(
                                        ChatThread(
                                            id = threadId,
                                            type = ThreadType.SQUAD,
                                            title = "${com.name} Sohbeti",
                                            emoji = com.emoji,
                                            memberCount = com.members,
                                            messages = listOf(
                                                ChatMsg(
                                                    id = nextUniqueId("m_com_sys"),
                                                    senderId = "sys",
                                                    senderName = "Matchora",
                                                    text = "${com.name} topluluk sohbet odası açıldı.",
                                                    time = "Şimdi",
                                                    isSystem = true
                                                )
                                            ),
                                            unread = 0,
                                            locked = false
                                        )
                                    ) + threads
                                }
                                activeChatThreadId = threadId
                                chatEntrySection = ChatHubSection.MESSAGES
                                focusedMemoryId = null
                                currentTab = 4
                                showToast("${com.name} sohbet odası açıldı", "💬")
                            }
                        }
                    },
                    venues = if (backend.enabled) remoteVenues else DemoData.venues,
                    joinedCommunityIds = joinedCommunityIds,
                    requestedCommunityIds = requestedCommunityIds,
                    bookedSlotIds = bookedSlotIds,
                    onBookSlot = { venue, slot, splitCount, amount, method -> if (!(remote("booking.create", apiPayload("slotId" to slot.id, "splitCount" to splitCount)))) { requireAccount {
                        val bookingKey = "${venue.id}:${slot.id}"
                        if (bookingKey !in bookedSlotIds && slot.available) {
                            bookedSlotIds = bookedSlotIds + bookingKey
                            venuePayments = listOf(VenuePayment(nextUniqueId("vp"), venue.name,
                                "${slot.court} · ${slot.day} ${slot.time} · $splitCount kişilik paylaşım",
                                amount, method, "Şimdi", PaymentStatus.PENDING, bookingKey)) + venuePayments
                            showToast("Rezervasyon kaydedildi · ₺$amount tesiste ödenecek", "🏟️")
                        } else showToast("Bu saat zaten rezerve edildi", "ℹ️")
                    }  } },
                    onJoinCommunity = { com -> if (!(remote(if (com.id in joinedCommunityIds || com.id in requestedCommunityIds) "community.leave" else "community.join", apiPayload("communityId" to com.id)))) { requireAccount {
                        when (com.access) {
                            CommunityAccess.OPEN -> {
                                if (com.id in joinedCommunityIds) {
                                    joinedCommunityIds = joinedCommunityIds - com.id
                                    showToast("${com.name} topluluğundan ayrıldın", "↩️")
                                } else {
                                    joinedCommunityIds = joinedCommunityIds + com.id
                                    showToast("${com.name} topluluğuna katıldın", "🤝")
                                }
                            }
                            CommunityAccess.APPROVAL -> {
                                if (com.id in requestedCommunityIds) {
                                    requestedCommunityIds = requestedCommunityIds - com.id
                                    showToast("${com.name} başvurun geri çekildi", "↩️")
                                } else {
                                    requestedCommunityIds = requestedCommunityIds + com.id
                                    showToast("${com.name} için katılım başvurun iletildi", "🤝")
                                }
                            }
                            CommunityAccess.INVITE -> showToast("Bu topluluk davetle katılıma açık", "🔒")
                        }
                     } } }
                )
                4 -> ChatHubScreen(
                    paddingValues = paddingValues,
                    threads = threads,
                    onOpenDirectChat = { requireAccount { openDirectChatWithPlayer(it) } },
                    blockedThreadIds = blockedChatIds,
                    ownBlockedThreadIds = if (!backend.enabled) blockedChatIds else threads.filter { thread -> live?.otherUserForThread(thread.id) in live?.wire?.ss("blockedUserIds").orEmpty() }.map { it.id }.toSet(),
                    onToggleBlock = { threadId ->
                        val other = live?.otherUserForThread(threadId)
                        if (!backend.enabled) blockedChatIds = if (threadId in blockedChatIds) blockedChatIds - threadId else blockedChatIds + threadId
                        else if (other != null) remote("user.block", apiPayload("userId" to other, "blocked" to (other !in live?.wire?.ss("blockedUserIds").orEmpty())))
                    },
                    onReportThread = { thread -> requireAccount { reportTargetName = thread.title; reportTargetId = thread.id; reportTargetType = "thread" } },
                    onReportMessage = { msg -> requireAccount { reportTargetName = "${msg.senderName} · Mesaj"; reportTargetId = msg.id; reportTargetType = "message" } },
                    onDeleteMemory = { post -> if (!remote("memory.delete", apiPayload("memoryId" to post.id))) requireAccount { memoryPosts = memoryPosts.filterNot { it.id == post.id } } },
                    onReportMemory = { post -> requireAccount { reportTargetName = "${post.authorName} · Fotoğraf"; reportTargetId = if (post.isShowcase) post.id else activeChatThreadId.orEmpty(); reportTargetType = if (post.isShowcase) "memory" else "thread" } },
                    onAddMemoryComment = { memoryId, text -> if (!remote("memory.comment", apiPayload("memoryId" to memoryId, "text" to text))) { requireAccount { memoryPosts = memoryPosts.map { if (it.id == memoryId) it.copy(comments = it.comments + MemoryComment(nextUniqueId("mc"), DemoData.ME, profile.name, text, "Şimdi")) else it } } } },
                    onUpdateMemory = { post -> if (!remoteMemoryEdit(post)) memoryPosts = memoryPosts.map { if (it.id == post.id) post.copy(status = "approved") else it } },
                    onSendReply = { threadId, text, replyTo ->
                        if (!remote("chat.send", apiPayload("threadId" to threadId, "text" to text, "replyTo" to replyTo))) {
                            val original = threads.firstOrNull { it.id == threadId }?.messages?.firstOrNull { it.id == replyTo }
                            threads = threads.map { if (it.id == threadId) it.copy(messages = it.messages + ChatMsg(nextUniqueId("reply"), DemoData.ME, profile.name, text, "Şimdi", isMine = true, replyToId = replyTo, replyText = original?.text)) else it }
                        }
                    },
                    onEditMessage = { message, text ->
                        if (!remote("message.edit", apiPayload("messageId" to message.id, "expectedVersion" to message.version, "text" to text))) threads = threads.map { it.copy(messages = it.messages.map { m -> if (m.id == message.id) m.copy(text = text, edited = true, version = m.version + 1) else m }) }
                    },
                    onDeleteMessage = { message, everyone ->
                        if (!remote(if (everyone) "message.delete" else "message.hide", if (everyone) apiPayload("messageId" to message.id, "expectedVersion" to message.version) else apiPayload("messageId" to message.id))) threads = threads.map { t -> t.copy(messages = if (everyone) t.messages.map { if (it.id == message.id) it.copy(text = "Bu mesaj silindi.", attachedMemory = null, deleted = true) else it } else t.messages.filterNot { it.id == message.id }) }
                    },
                    onClearChat = { thread -> if (!remote("chat.clear", apiPayload("threadId" to thread.id))) threads = threads.map { if (it.id == thread.id) it.copy(messages = emptyList(), unread = 0) else it } },
                    onDeleteChat = { thread, block ->
                        if (!remote("chat.delete", apiPayload("threadId" to thread.id, "blockUser" to block))) { threads = threads.filterNot { it.id == thread.id }; if (block) blockedChatIds = blockedChatIds + thread.id }
                        activeChatThreadId = null
                    },
                    memories = memoryPosts,
                    players = if (backend.enabled) remotePlayers else DemoData.players,
                    currentCity = currentCity,
                    currentDistrict = currentDistrict,
                    currentUser = profile,
                    initialActiveThreadId = activeChatThreadId,
                    initialSection = chatEntrySection,
                    focusedMemoryId = focusedMemoryId,
                    onOpenRelatedEvent = { eventId -> openEventById(eventId) },
                    onOpenRelatedTeam = { teamKey ->
                        val targetTeam = teams.firstOrNull { it.id == teamKey || it.name.equals(teamKey, ignoreCase = true) }
                        if (targetTeam != null) {
                            competitionEntryTab = 2
                            competitionFocusId = targetTeam.id
                            currentTab = 2
                        } else if (backend.enabled) {
                            openCatalogTarget("teams", teamKey)
                        } else {
                            showToast("Takım bulunamadı", "ℹ️")
                        }
                    },
                    onActiveThreadChange = { threadId ->
                        activeChatThreadId = threadId
                        if (threadId != null) {
                            threads = threads.map { thread ->
                                if (thread.id == threadId) thread.copy(unread = 0) else thread
                            }
                        }
                    },
                    onSendMessage = { threadId, text -> if (!(remote("chat.send", apiPayload("threadId" to threadId, "text" to text)))) { requireAccount {
                        val message = text.trim()
                        val target = threads.firstOrNull { it.id == threadId }
                        if (message.isNotEmpty() && target != null && !target.locked && threadId !in blockedChatIds) {
                            threads = threads.map { th ->
                                if (th.id == threadId) th.copy(messages = th.messages + ChatMsg(
                                    id = nextUniqueId("m"), senderId = DemoData.ME,
                                    senderName = profile.name, text = message, time = "Şimdi", isMine = true
                                )) else th
                            }
                            demoScope.launch {
                                delay(1200)
                                threads = threads.map { th ->
                                    if (th.id == threadId && threadId !in blockedChatIds) th.copy(
                                        messages = th.messages + ChatMsg(
                                            id = nextUniqueId("m_demo"), senderId = "demo_partner", senderName = "Demo sporcu",
                                            text = "Mesajını aldım! Buluşma detaylarını buradan konuşabiliriz. Görüşürüz 👋", time = "Şimdi"
                                        ),
                                        unread = if (currentTab == 4 && activeChatThreadId == threadId) 0 else th.unread + 1
                                    ) else th
                                }
                            }
                        }
                     } } },
                    onSendMemoryToChat = { threadId, caption, memory -> if (!(remotePhoto(memory.copy(caption = caption), threadId))) { requireAccount {
                        threads = threads.map { th ->
                            if (th.id == threadId && !th.locked && threadId !in blockedChatIds) th.copy(
                                messages = th.messages + ChatMsg(
                                    id = nextUniqueId("m_mem"),
                                    senderId = DemoData.ME,
                                    senderName = profile.name,
                                    text = caption,
                                    time = "Şimdi",
                                    isMine = true,
                                    attachedMemory = memory
                                )
                            ) else th
                        }
                        showToast(
                            if (memory.venue.isBlank()) "Fotoğraf sohbete gönderildi" else "Saha anısı sohbete gönderildi",
                            "📸"
                        )
                     } } },
                    onCreateMemoryPost = { newPost, targetThreadId -> if (!(remotePhoto(newPost, targetThreadId, publishMemory = true))) { requireAccount {
                        memoryPosts = listOf(newPost) + memoryPosts
                        if (targetThreadId != null) {
                            threads = threads.map { th ->
                                if (th.id == targetThreadId) th.copy(
                                    messages = th.messages + ChatMsg(
                                        id = nextUniqueId("m_mem"),
                                        senderId = DemoData.ME,
                                        senderName = profile.name,
                                        text = newPost.caption,
                                        time = "Şimdi",
                                        isMine = true,
                                        attachedMemory = newPost
                                    )
                                ) else th
                            }
                            showToast("Anı vitrine ve sohbete eklendi!", "📸")
                        } else {
                            showToast("Saha anısı vitrine eklendi!", "📸")
                        }
                     } } },
                    onToggleLikeMemory = { memoryId, liked -> if (!(remote("memory.like", apiPayload("memoryId" to memoryId, "liked" to liked)))) { requireAccount {
                        memoryPosts = memoryPosts.map { post ->
                            if (post.id == memoryId) {
                                val nowLiked = !post.likedByMe
                                post.copy(
                                    likedByMe = nowLiked,
                                    likes = (post.likes + if (nowLiked) 1 else -1).coerceAtLeast(0)
                                )
                            } else post
                        }
                     } } }
                )
                5 -> SportsPassportScreen(
                    paddingValues = paddingValues,
                    user = profile,
                    activeProPlan = activeProPlan,
                    isLoggedIn = isLoggedIn,
                    historyOverride = if (backend.enabled) live?.history.orEmpty() else DemoData.history,
                    reliabilityOverride = if (backend.enabled) live?.reliabilityHistory.orEmpty() else DemoData.reliabilityRecords,
                    badgesOverride = if (backend.enabled) live?.badges.orEmpty() else DemoData.badges,
                    missionsOverride = if (backend.enabled) live?.missions.orEmpty() else DemoData.missions,
                    pendingMatchCount = if (backend.enabled) events.count { it.id in joinedEventIds && it.status in setOf(EventStatus.COMPLETED, EventStatus.DISPUTED) } else 0,
                    onOpenAuth = { showAuthSheet = true },
                    onOpenSettings = { showAccountSettings = true },
                    onOpenActivity = { showMyActivity = true },
                    onLogout = { if (!(remoteLogout())) {
                        isLoggedIn = false
                        showToast("Çıkış yapıldı · Misafir modundasın", "ℹ️")
                     } },
                    onOpenWallet = { requireAccount { showWalletSheet = true  }},
                    onVerifyContact = { showContactVerification = true },
                    onVerifyPhoto = { showPhotoVerification = true },
                    onUpdateProfile = { updatedProfile -> if (!(remoteProfile(updatedProfile))) {
                        profile = updatedProfile.copy(avatarRemovalRequested = false)
                        currentCity = updatedProfile.city
                        currentDistrict = updatedProfile.area
                        showToast("Sporcu profilin güncellendi", "✓")
                     } }
                )
                else -> Unit
                }
            }


        }
        }
    }
    } // end if (!isAdminSession)

    }
    }

    backendError?.let { message -> AlertDialog(onDismissRequest = { backendError = null }, title = { Text("İşlem tamamlanamadı") }, text = { Text(message) },
        confirmButton = { TextButton(onClick = { backendError = null; backendRetry?.invoke() }) { Text("Tekrar dene") } },
        dismissButton = { TextButton(onClick = { backendError = null }) { Text("Kapat") } }) }
    checkInEvent?.let { event -> AlertDialog(onDismissRequest = { checkInEvent = null }, title = { Text("Saha check-in") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Organizatörün sahada paylaştığı güncel kodu gir.")
            OutlinedTextField(checkInCode, { checkInCode = it }, label = { Text("Check-in kodu") })
            CheckInCodeActions(hostCheckInCode) { checkInCode = it }
            if (event.hostId == DemoData.ME) TextButton(onClick = { remote("event.checkin.code", apiPayload("eventId" to event.id)) { hostCheckInCode = it["code"]?.jsonPrimitive?.content.orEmpty(); checkInCode = hostCheckInCode } }) { Text("Organizatör kodu oluştur") }
            if (hostCheckInCode.isNotBlank()) { Text(hostCheckInCode, style = MaterialTheme.typography.bodySmall); TextButton(onClick = { clipboardManager.setText(AnnotatedString(hostCheckInCode)); showToast("Check-in kodu kopyalandı", "✓") }) { Text("Kodu kopyala") } }
        } }, confirmButton = { TextButton(onClick = { remote("event.checkin", apiPayload("eventId" to event.id, "code" to checkInCode)) { checkInEvent = null } }, enabled = checkInCode.isNotBlank()) { Text("Check-in yap") } },
        dismissButton = { TextButton(onClick = { checkInEvent = null }) { Text("Kapat") } }) }

    LaunchedEffect(featuresLocked) {
        if (featuresLocked) {
            if (authBackend.authenticated && (authWire?.b("legalAcceptanceRequired") == true || (authWire != null && authWire?.s("role") != "admin" && authWire?.let(::BackendSnapshot)?.profile == null))) {
                showAuthSheet = true
            }
            if (currentTab in setOf(2, 3)) currentTab = 0
            selectedDetailEvent = null; selectedDetailPlayers = emptyList(); activeChatThreadId = null
            tournamentTeamChoice = null; showPlayNowSheet = false; showWalletSheet = false
            showNotificationsSheet = false; showCityLocationSheet = false
            showSubmitJoinSheetForEvent = null; showManageJoinRequestsForEvent = null
            challengeTargetTeam = null; challengeSourceTeam = null; reportTargetName = null
            showMyActivity = false; showAccountSettings = false; showModeration = false; showVenueOperator = false
            showContactVerification = false; showPhotoVerification = false; checkInEvent = null
            resultEvent = null; feedbackEvent = null
        }
    }

    // ═══════════════ MODAL BOTTOM SHEETS ═══════════════

    // 1. Etkinlik Detay Sayfası
    if (selectedDetailEvent != null) {
        val ev = events.firstOrNull { it.id == selectedDetailEvent!!.id } ?: selectedDetailEvent!!
        val isJoined = isLoggedIn && joinedEventIds.contains(ev.id)
        val isCheckedIn = checkedInEventIds.contains(ev.id)
        val isHost = isLoggedIn && (ev.hostId == DemoData.ME || ev.hostId == "me")
        val pendingRequest = if (!isLoggedIn) null else joinRequests.firstOrNull { it.eventId == ev.id && it.player.id == DemoData.ME && it.status == JoinRequestStatus.PENDING }
        val pendingRequestsCount = joinRequests.count { it.eventId == ev.id && it.status == JoinRequestStatus.PENDING }
        val isOnWaitlist = waitlistedEventIds.contains(ev.id)

        EventDetailSheet(
            event = ev,
            players = if (backend.enabled) (remotePlayers + selectedDetailPlayers).distinctBy { it.id } else DemoData.players,
            currentPlayer = currentPlayer,
            isJoined = isJoined,
            isCheckedIn = isCheckedIn,
            isOnWaitlist = isOnWaitlist,
            isHost = isHost,
            pendingRequest = pendingRequest,
            pendingRequestsCount = pendingRequestsCount,
            invitedPlayerIds = invitedPlayerIds,
            isRemovedByHost = if (backend.enabled) ev.id in live?.wire?.ss("removedEventIds").orEmpty() else DemoData.ME in previewRemovedEventMembers[ev.id].orEmpty(),
            removedPlayers = if (backend.enabled) remotePlayers.filter { player -> live?.wire?.a("removedEventMembers")?.any { it.s("event_id") == ev.id && it.s("user_id") == backend.serverUserId(player.id) } == true } else DemoData.players.filter { it.id in previewRemovedEventMembers[ev.id].orEmpty() },
            onRemoveParticipant = { player -> requireAccount {
                val expectedVer = live?.eventVersion(ev.id)?.takeIf { it > 0 } ?: ev.version
                val handled = remote("event.member.remove", apiPayload("eventId" to ev.id, "userId" to backend.serverUserId(player.id), "expectedVersion" to expectedVer)) {
                    previewRemovedEventMembers = previewRemovedEventMembers + (ev.id to (previewRemovedEventMembers[ev.id].orEmpty() + player.id))
                    events = events.map { if (it.id == ev.id) it.copy(participantIds = it.participantIds - player.id, status = EventStatus.FILLING) else it }
                    showToast("${player.name} kadrodan çıkarıldı", "↩️")
                }
                if (!handled) {
                    previewRemovedEventMembers = previewRemovedEventMembers + (ev.id to (previewRemovedEventMembers[ev.id].orEmpty() + player.id))
                    events = events.map { if (it.id == ev.id) it.copy(participantIds = it.participantIds - player.id, status = EventStatus.FILLING) else it }
                    showToast("${player.name} kadrodan çıkarıldı", "↩️")
                }
            } },
            onRestoreParticipant = { player -> requireAccount {
                val expectedVer = live?.eventVersion(ev.id)?.takeIf { it > 0 } ?: ev.version
                val handled = remote("event.member.restore", apiPayload("eventId" to ev.id, "userId" to backend.serverUserId(player.id), "expectedVersion" to expectedVer)) {
                    previewRemovedEventMembers = previewRemovedEventMembers + (ev.id to (previewRemovedEventMembers[ev.id].orEmpty() - player.id))
                    showToast("${player.name} yeniden katılabilir", "✓")
                }
                if (!handled) {
                    previewRemovedEventMembers = previewRemovedEventMembers + (ev.id to (previewRemovedEventMembers[ev.id].orEmpty() - player.id))
                    showToast("${player.name} yeniden katılabilir", "✓")
                }
            } },
            onDismiss = {
                if (directInvitedEventId == ev.id) directInvitedEventId = null
                selectedDetailEvent = null
                selectedDetailPlayers = emptyList()
            },
            onToggleJoin = {
                val operation = if (isJoined) "event.leave" else "event.join"
                val handled = remote(operation, apiPayload("eventId" to ev.id)) {
                    showToast(if (isJoined) "Kadrodan ayrıldın" else "Kadroya katıldın! İyi oyunlar.", if (isJoined) "↩️" else "⚽")
                }
                if (!handled) { requireAccount {
                if (isJoined) {
                    if (isCheckedIn) {
                        showToast("Check-in tamamlanan karşılaşmadan ayrılamazsın", "ℹ️")
                    } else {
                        joinedEventIds = joinedEventIds - ev.id
                        val released = releaseEventTokens(ev.id)
                        events = events.map { current ->
                            if (current.id != ev.id) current
                            else current.copy(
                                participantIds = current.participantIds - DemoData.ME,
                                status = if (current.status == EventStatus.FULL) EventStatus.FILLING else current.status
                            )
                        }
                        selectedDetailEvent = events.firstOrNull { it.id == ev.id } ?: ev
                        ledger = listOf(
                            LedgerEntry(nextUniqueId("l_rel"), LedgerType.RELEASE, released, "Kadro iadesi · ${ev.title}", "Şimdi", idempotencyKey = nextUniqueId("op_leave"))
                        ) + ledger
                        showToast("Kadrodan ayrıldın · $released TKN iade edildi", "↩️")
                    }
                } else {
                    joinEventImmediately(ev, "Taahhüt")
                }
             } } },
            waitlistOfferUntil = live?.wire?.a("waitlist")?.firstOrNull { it.s("event_id") == ev.id && it.s("status") == "offered" }?.l("offered_until"),
            onAcceptWaitlistOffer = { remote("event.waitlist", apiPayload("eventId" to ev.id, "action" to "accept")) },
            onToggleWaitlist = { if (!remote("event.waitlist", apiPayload("eventId" to ev.id, "action" to live?.waitlistAction(ev.id)))) { requireAccount {
                if (isOnWaitlist) {
                    waitlistedEventIds = waitlistedEventIds - ev.id
                    events = events.map { current ->
                        if (current.id == ev.id) current.copy(waitlistCount = (current.waitlistCount - 1).coerceAtLeast(0)) else current
                    }
                    selectedDetailEvent = events.firstOrNull { it.id == ev.id } ?: ev
                    showToast("Bekleme listesinden ayrıldın", "↩️")
                } else if (ev.status == EventStatus.FULL && !joinedEventIds.contains(ev.id)) {
                    waitlistedEventIds = waitlistedEventIds + ev.id
                    events = events.map { current -> if (current.id == ev.id) current.copy(waitlistCount = current.waitlistCount + 1) else current }
                    selectedDetailEvent = events.firstOrNull { it.id == ev.id } ?: ev
                    showToast("Bekleme listesine eklendin · yer açılınca haber vereceğiz", "⏳")
                }
             } } },
            onRequestJoin = { requireAccount {
                val isDirect = directInvitedEventId == ev.id
                if (isDirect) joinEventImmediately(ev, "Davetli Katılım", bypassApproval = true)
                else if (ev.approvalRequired || !backend.enabled) showSubmitJoinSheetForEvent = ev
                else joinEventImmediately(ev, "Katılım")
             }},
            onCancelRequest = { requireAccount {
                val r = pendingRequest
                if (r != null) cancelJoinRequest(r)
             }},
            onOpenManageRequests = { requireAccount {
                showManageJoinRequestsForEvent = ev
             }},
            onDirectMessageHost = { requireAccount {
                val pool = if (backend.enabled) (remotePlayers + selectedDetailPlayers).distinctBy { it.id } else DemoData.players
                val hostPlayer = pool.firstOrNull { it.id == ev.hostId } ?: if (backend.enabled) {
                    currentPlayer.copy(
                        id = ev.hostId,
                        name = "Organizatör",
                        verified = false,
                        rating = 0,
                        verifiedMatches = 0,
                        reliability = 0,
                        position = null,
                        reliabilityVisible = false,
                        profileAvailable = false,
                        city = ev.city
                    )
                } else null
                if (hostPlayer != null) openDirectChatWithPlayer(hostPlayer) else showToast("Organizatör profili şu anda yüklenemiyor.", "ℹ️")
             }},
            onDirectMessagePlayer = { player -> requireAccount { openDirectChatWithPlayer(player) } },
            onCheckIn = { requireAccount {
                if (backend.enabled) {
                    checkInEvent = ev; checkInCode = ""; hostCheckInCode = ""
                } else if (!isJoined) {
                    showToast("Check-in için önce kadroya katıl", "ℹ️")
                } else if (checkedInEventIds.contains(ev.id)) {
                    showToast("Check-in zaten tamamlandı", "ℹ️")
                } else {
                    checkedInEventIds = checkedInEventIds + ev.id
                    profile = profile.copy(reliability = (profile.reliability + 2).coerceAtMost(100))
                    val released = releaseEventTokens(ev.id)
                    ledger = listOf(
                        LedgerEntry(nextUniqueId("l_rel"), LedgerType.RELEASE, released, "Check-in katılım iadesi · ${ev.title}", "Şimdi", idempotencyKey = nextUniqueId("op_checkin"))
                    ) + ledger
                    showToast("Check-in tamamlandı · $released TKN iade edildi", "📍")
                }
            } },
            onOpenChat = { requireAccount {
                if (backend.enabled) {
                    val targetThread = threads.firstOrNull { it.relatedEventId == ev.id }
                    if (targetThread != null) {
                        activeChatThreadId = targetThread.id; chatEntrySection = ChatHubSection.MESSAGES; focusedMemoryId = null; selectedDetailEvent = null; currentTab = 4
                    } else {
                        showToast("Etkinlik sohbetini açmak için önce kadroya katılmalısın", "💬")
                    }
                } else if (isJoined || isHost) {
                    val existing = threads.firstOrNull { it.relatedEventId == ev.id }
                    val threadId = existing?.id ?: "chat_${ev.id}"
                    if (existing == null) {
                        threads = listOf(ChatThread(
                            id = threadId, type = ThreadType.EVENT, title = ev.title,
                            emoji = ev.displaySportEmoji, memberCount = ev.participantIds.size,
                            messages = listOf(ChatMsg(nextUniqueId("m_welcome"), "system", "Matchora",
                                "${ev.title} sohbetine hoş geldin. Buradaki mesajlar demo oturumunda saklanır.", "Şimdi", isSystem = true)),
                            unread = 0, relatedEventId = ev.id
                        )) + threads
                    }
                    activeChatThreadId = threadId
                    selectedDetailEvent = null
                    currentTab = 4
                } else showToast("Sohbet için önce etkinliğe katıl", "ℹ️")
             }},
            onReportEvent = { requireAccount {
                reportTargetName = ev.title; reportTargetId = ev.id; reportTargetType = "event"
             }},
            onStartEvent = {
                val expectedVer = live?.eventVersion(ev.id)?.takeIf { it > 0 } ?: ev.version
                val handled = remote("event.transition", apiPayload("eventId" to ev.id, "status" to "IN_PROGRESS", "expectedVersion" to expectedVer)) {
                    events = events.map { if (it.id == ev.id) it.copy(status = EventStatus.IN_PROGRESS, version = expectedVer + 1) else it }
                    selectedDetailEvent = events.firstOrNull { it.id == ev.id } ?: ev.copy(status = EventStatus.IN_PROGRESS, version = expectedVer + 1)
                    showToast("Maç başladı! İyi oyunlar.", "⚡")
                }
                if (!handled) {
                    events = events.map { if (it.id == ev.id) it.copy(status = EventStatus.IN_PROGRESS) else it }
                    selectedDetailEvent = events.firstOrNull { it.id == ev.id }
                    showToast("Maç başladı! İyi oyunlar.", "⚡")
                }
            },
            onCompleteEvent = {
                val expectedVer = live?.eventVersion(ev.id)?.takeIf { it > 0 } ?: ev.version
                val handled = remote("event.transition", apiPayload("eventId" to ev.id, "status" to "COMPLETED", "expectedVersion" to expectedVer)) {
                    events = events.map { if (it.id == ev.id) it.copy(status = EventStatus.COMPLETED, version = expectedVer + 1) else it }
                    val completed = events.firstOrNull { it.id == ev.id } ?: ev.copy(status = EventStatus.COMPLETED, version = expectedVer + 1)
                    selectedDetailEvent = null
                    resultEvent = completed
                    showToast("Maç tamamlandı! Sonucu bildirebilirsin.", "🏆")
                }
                if (!handled) {
                    events = events.map { if (it.id == ev.id) it.copy(status = EventStatus.COMPLETED) else it }
                    selectedDetailEvent = null
                    resultEvent = events.firstOrNull { it.id == ev.id }
                    showToast("Maç tamamlandı! Sonucu bildirebilirsin.", "🏆")
                }
            },
            onCancelEvent = {
                val expectedVer = live?.eventVersion(ev.id)?.takeIf { it > 0 } ?: ev.version
                val handled = remote("event.transition", apiPayload("eventId" to ev.id, "status" to "CANCELLED", "expectedVersion" to expectedVer)) {
                    events = events.map { if (it.id == ev.id) it.copy(status = EventStatus.CANCELLED, version = expectedVer + 1) else it }
                    val released = releaseEventTokens(ev.id)
                    if (released > 0) ledger = listOf(LedgerEntry(nextUniqueId("cancel_release"), LedgerType.RELEASE, released, "İptal iadesi · ${ev.title}", "Şimdi", idempotencyKey = "cancel-${ev.id}")) + ledger
                    joinedEventIds = joinedEventIds - ev.id
                    waitlistedEventIds = waitlistedEventIds - ev.id
                    joinRequests = joinRequests.map { if (it.eventId == ev.id && it.status == JoinRequestStatus.PENDING) it.copy(status = JoinRequestStatus.REJECTED) else it }
                    selectedDetailEvent = null
                    showToast("Maç iptal edildi", "↩️")
                }
                if (!handled) {
                    events = events.map { if (it.id == ev.id) it.copy(status = EventStatus.CANCELLED) else it }
                    val released = releaseEventTokens(ev.id)
                    if (released > 0) ledger = listOf(LedgerEntry(nextUniqueId("cancel_release"), LedgerType.RELEASE, released, "İptal iadesi · ${ev.title}", "Şimdi", idempotencyKey = "cancel-${ev.id}")) + ledger
                    joinedEventIds = joinedEventIds - ev.id
                    waitlistedEventIds = waitlistedEventIds - ev.id
                    joinRequests = joinRequests.map { if (it.eventId == ev.id && it.status == JoinRequestStatus.PENDING) it.copy(status = JoinRequestStatus.REJECTED) else it }
                    selectedDetailEvent = null
                    showToast("Maç iptal edildi", "↩️")
                }
            },
            hasResult = ev.id in matchResults,
            onOpenResult = { requireAccount { resultEvent = ev } },
            isDirectInvited = (directInvitedEventId == ev.id),
            onChallengePlayer = { player ->
                requireAccount {
                    val targetTeam = teams.firstOrNull { t ->
                        !t.isMine && t.sport == ev.sport && (t.captainId == player.id || t.coCaptainId == player.id || player.id in t.memberIds || player.id in t.substituteIds)
                    } ?: teams.firstOrNull { t ->
                        !t.isMine && (t.captainId == player.id || t.coCaptainId == player.id || player.id in t.memberIds || player.id in t.substituteIds)
                    }
                    selectedDetailEvent = null
                    if (targetTeam != null) {
                        challengeSourceTeam = teams.firstOrNull { it.captainId == DemoData.ME && it.sport == targetTeam.sport }
                        challengeTargetTeam = targetTeam
                    } else {
                        competitionEntryTab = 2
                        currentTab = 2
                        showToast("${player.name} henüz bir kulüpte değil — takımlara göz atabilirsin ⚔️", "ℹ️")
                    }
                }
            },
            onInvitePlayerToTeam = { player ->
                requireAccount {
                    val myTeam = teams.firstOrNull { it.captainId == DemoData.ME && it.sport == ev.sport }
                    if (myTeam != null) {
                        if (!remote("team.invite", apiPayload("teamId" to myTeam.id, "userId" to backend.serverUserId(player.id))) {
                                invitedPlayerIds = invitedPlayerIds + player.id
                            }) {
                            invitedPlayerIds = invitedPlayerIds + player.id
                            showToast("${player.name}, ${myTeam.name} kadrosuna davet edildi", "🛡️")
                        }
                    } else {
                        selectedDetailEvent = null
                        competitionEntryTab = 2
                        currentTab = 2
                        showToast("Önce bu branşta bir takım kurmalısın 🛡️", "ℹ️")
                    }
                }
            },
            onShare = {
                shareEventInvitation(ev)
                rewardShareBonus("İlan Paylaşım Bonusu · ${ev.title}")
            }
        )
    }

    tournamentTeamChoice?.let { (tournament, withdraw) ->
        TournamentRegistrationSheet(
            tournament = tournament,
            teams = teams,
            players = if (backend.enabled) remotePlayers else DemoData.players,
            withdraw = withdraw,
            onDismiss = { tournamentTeamChoice = null },
            onCreateNewTeam = {
                tournamentTeamChoice = null
                competitionEntryTab = 2
                currentTab = 2
                showToast("Turnuva için önce takımını kur 🛡️", "ℹ️")
            },
            onSubmit = { team, roster ->
                val payload = if (withdraw) apiPayload("tournamentId" to tournament.id, "teamId" to team.id)
                    else apiPayload("tournamentId" to tournament.id, "teamId" to team.id, "roster" to roster.map(backend::serverUserId))
                if (!remote(if (withdraw) "tournament.withdraw" else "tournament.register", payload) { tournamentTeamChoice = null }) {
                    val registrations = tournament.registrations.filterNot { it.teamId == team.id } + if (withdraw) emptyList() else listOf(TournamentRegistration(team.id, team.name, team.captainId, roster))
                    tournaments = tournaments.map { if (it.id == tournament.id) it.copy(registrations = registrations, participants = (it.participants + if (withdraw) -1 else 1).coerceIn(0, it.capacity)) else it }
                    registeredTournamentIds = if (registrations.any { DemoData.ME in it.rosterIds }) registeredTournamentIds + tournament.id else registeredTournamentIds - tournament.id
                    tournamentTeamChoice = null
                    showToast(if (withdraw) "Takımın turnuva kaydı çekildi" else "Takım ve maç kadrosu kaydedildi", "🏆")
                }
            })
    }

    // 1.1 Katılım İstekleri Yönetimi Modalı (Kaptan & Organizatör)
    if (showManageJoinRequestsForEvent != null) {
        val manageEv = showManageJoinRequestsForEvent!!
        ManageJoinRequestsSheet(
            event = manageEv,
            requests = joinRequests.filter { it.eventId == manageEv.id },
            onDismiss = { showManageJoinRequestsForEvent = null },
            onApprove = { req ->
                approveJoinRequest(req)
            },
            onReject = { req ->
                rejectJoinRequest(req)
            },
            onDirectMessage = { player ->
                openDirectChatWithPlayer(player)
            },
            onShare = {
                shareEventInvitation(manageEv)
                rewardShareBonus("İlan Paylaşım Bonusu · ${manageEv.title}")
            }
        )
    }

    // 1.2 Kadroya Katılma / Başvuru Yapma Modalı (Sporcu Pozisyon Seçimi)
    if (showSubmitJoinSheetForEvent != null) {
        val submitEv = showSubmitJoinSheetForEvent!!
        SubmitJoinRequestSheet(
            event = submitEv,
            tokenBalance = tokenBalance,
            onDismiss = { showSubmitJoinSheetForEvent = null },
            onSubmitRequest = { pos, note ->
                submitJoinRequest(submitEv, pos, note)
                showSubmitJoinSheetForEvent = null
            },
            onOpenTopUp = {
                showSubmitJoinSheetForEvent = null
                showTokenTopUpSheet = true
            }
        )
    }

    // 2. Şimdi Oyna & İlan Verme (Play Now Wizard & Ad Post)
    if (showPlayNowSheet) {
        val listingPublicationOwner = backend.userId
        PlayNowModalSheet(
            allEvents = events,
            tokenBalance = tokenBalance,
            joinedEventIds = joinedEventIds,
            waitlistedEventIds = waitlistedEventIds,
            pendingRequestEventIds = pendingRequestEventIds,
            currentArea = currentDistrict,
            currentCity = currentCity,
            currentCampus = profile.campus,
            currentCompany = profile.company,
            initialNetworkScope = initialListingScope,
            initialVenue = playNowInitialVenue.orEmpty(),
            initialTime = playNowInitialTime,
            eligibleCommunities = if (backend.enabled) remoteCommunities.filter { it.id in joinedCommunityIds } else null,
            newEventId = { nextUniqueId("ev_ad") },
            onDismiss = { showPlayNowSheet = false; playNowInitialVenue = null; playNowInitialTime = null },
            onSelectEvent = { ev ->
                showPlayNowSheet = false
                playNowInitialVenue = null
                playNowInitialTime = null
                selectedDetailEvent = ev
            },
            onQuickJoin = { ev -> requireAccount {
                showPlayNowSheet = false
                playNowInitialVenue = null
                playNowInitialTime = null
                if (ev.approvalRequired) {
                    showSubmitJoinSheetForEvent = ev
                } else {
                    joinEventImmediately(ev, "Hızlı katılım")
                }
             }},
            onToggleWaitlist = { ev ->
                if (!remote("event.waitlist", apiPayload("eventId" to ev.id, "action" to live?.waitlistAction(ev.id)))) {
                    requireAccount {
                        if (waitlistedEventIds.contains(ev.id)) {
                            waitlistedEventIds = waitlistedEventIds - ev.id
                            events = events.map { current ->
                                if (current.id == ev.id) current.copy(waitlistCount = (current.waitlistCount - 1).coerceAtLeast(0)) else current
                            }
                            showToast("Bekleme listesinden ayrıldın", "↩️")
                        } else if (ev.status == EventStatus.FULL) {
                            waitlistedEventIds = waitlistedEventIds + ev.id
                            events = events.map { current -> if (current.id == ev.id) current.copy(waitlistCount = current.waitlistCount + 1) else current }
                            showToast("Bekleme listesine eklendin · yer açılınca haber vereceğiz", "⏳")
                        }
                    }
                }
            },
            onCreateEvent = { newEvent -> playNowInitialVenue = null; playNowInitialTime = null; if (!(remote("event.create", newEvent.toApiPayload()) { result ->
                openEventById(result.s("id"))
                showToast("İlan yayınlandı. ID’sini detaydan paylaşabilirsin.", "📢")
            })) {
                if (events.any { it.id == newEvent.id }) {
                    showToast("Bu ilan kimliği zaten kullanılmış; yeniden dene", "⚠️")
                } else if (lockTokens(10, "İlan oluşturma")) {
                    events = listOf(newEvent) + events
                    joinedEventIds = joinedEventIds + newEvent.id
                    eventTokenHolds = eventTokenHolds + (newEvent.id to 10)
                    ledger = listOf(
                        LedgerEntry(nextUniqueId("l_ad"), LedgerType.HOLD, -10, "İlan Kilidi · ${newEvent.title}", "Şimdi", idempotencyKey = nextUniqueId("op_ad"))
                    ) + ledger
                    showToast("İlan yayınlandı · ID: ${eventDisplayId(newEvent.id)}", "🎉")
                    selectedDetailEvent = newEvent
                    showManageJoinRequestsForEvent = newEvent
                }
             } },
            draftOwnerKey = backend.userId ?: "preview",
            onRecoveredPublication = { id, keptDraft ->
                openEventById(id)
                showToast(if (keptDraft) "İlanın yayınlandığı doğrulandı. Sonraki değişikliklerin taslakta saklandı." else "İlanının yayınlandığı doğrulandı.", "📢")
            },
            onPublishEvent = if (backend.enabled) { event, key ->
                val result = backend.command("event.create", event.toApiPayload(), key, expectedOwner = listingPublicationOwner)
                openEventById(result.s("id"))
                showToast("İlan yayınlandı. ID’sini detaydan paylaşabilirsin.", "📢")
            } else null,
            onRequestTokens = {
                showTokenTopUpSheet = true
            }
        )
    }

    // 3. Cüzdan ve Ödeme Sayfası (Ayrık Ledger & In-App Purchase)
    if (showWalletSheet) {
        WalletAndPaymentSheet(
            tokenBalance = tokenBalance,
            lockedTokens = lockedTokens,
            ledger = ledger,
            paymentHolderName = profile.name,
            activeProPlan = activeProPlan,
            initialPlusTab = walletStartsOnPlus,
            onDismiss = { showWalletSheet = false; walletStartsOnPlus = false },
            onSelectTokenPack = { pack -> if (!(backend.enabled.also { if (it) showToast("Satın alma şu anda kullanılamıyor.", "ℹ️") })) {
                val added = pack.tokens + pack.bonus
                tokenBalance += added
                ledger = listOf(
                    LedgerEntry(
                        id = nextUniqueId("l_topup"),
                        type = LedgerType.PURCHASE,
                        amount = added,
                        description = "Token paketi · ${pack.tokens} TKN",
                        time = "Şimdi",
                        idempotencyKey = nextUniqueId("topup")
                    )
                ) + ledger
                showToast("$added TKN demo cüzdanına yüklendi!", "💎")
             } },
            onSubscribePlan = { activateDemoPlan(it) }
        )
    }

    // 5. Bildirimler Merkezi
    if (showNotificationsSheet) {
        NotificationsSheet(
            notifications = notifications,
            preferences = notificationPrefs,
            onPreferencesChange = { prefs -> if (!remote("preferences.save", apiPayload("notifications" to JsonObject(prefs.toApiPayload().filter { (key, value) -> notificationPrefs.toApiPayload()[key] != value })))) notificationPrefs = prefs },
            onDismiss = { showNotificationsSheet = false },
            onNotificationClick = { notif ->
                if (backend.enabled) remote("notification.read", apiPayload("notificationId" to notif.id))
                notifications = notifications.map { if (it.id == notif.id) it.copy(read = true) else it }
                showNotificationsSheet = false
                if (isAdminSession && notif.destination in setOf(
                        NotificationDestination.CHALLENGES,
                        NotificationDestination.TEAM,
                        NotificationDestination.TOURNAMENT,
                        NotificationDestination.RANKING,
                        NotificationDestination.PASSPORT,
                        NotificationDestination.CHAT,
                        NotificationDestination.NETWORK,
                        NotificationDestination.VENUES,
                        NotificationDestination.MEMORIES
                    )
                ) {
                    adminTab = adminTabForAlert(notif.title, notif.body)
                    adminRefreshSignal++
                } else when (notif.destination) {
                    NotificationDestination.EVENT -> {
                        (notif.eventId ?: notif.targetId)?.let { openEventById(it) } ?: showToast("Bu bildirimin maç bağlantısı bulunamadı", "ℹ️")
                    }
                    NotificationDestination.CHALLENGES -> {
                        competitionEntryTab = 2
                        competitionFocusId = notif.targetId?.takeIf { it.isNotBlank() }
                        currentTab = 2
                    }
                    NotificationDestination.TEAM, NotificationDestination.TOURNAMENT -> notif.targetId?.let { openCatalogTarget(if (notif.destination == NotificationDestination.TOURNAMENT) "tournaments" else "teams", it) }
                    NotificationDestination.RANKING -> { competitionEntryTab = 0; currentTab = 2 }
                    NotificationDestination.PASSPORT -> { currentTab = 5 }
                    NotificationDestination.ACCOUNT -> requireAccount { showAccountSettings = true }
                    NotificationDestination.PLAY -> requireAccount { showPlayNowSheet = true }
                    NotificationDestination.PLUS -> requireAccount { walletStartsOnPlus = true; showWalletSheet = true }
                    NotificationDestination.WALLET -> requireAccount { walletStartsOnPlus = false; showWalletSheet = true }
                    NotificationDestination.CHAT -> requireAccount {
                        activeChatThreadId = notif.targetId
                        chatEntrySection = ChatHubSection.MESSAGES
                        focusedMemoryId = null
                        currentTab = 4
                    }
                    NotificationDestination.NETWORK, NotificationDestination.VENUES -> {
                        val target = notif.targetId.orEmpty()
                        if (target.isBlank()) currentTab = 3
                        else openCatalogTarget(if (notif.destination == NotificationDestination.VENUES) "venues" else "communities", target)
                    }
                    NotificationDestination.MEMORIES -> {
                        activeChatThreadId = null
                        chatEntrySection = ChatHubSection.SHOWCASE
                        focusedMemoryId = notif.targetId?.takeIf { it.isNotBlank() }
                        currentTab = 4
                    }
                    NotificationDestination.MODERATION -> requireAccount {
                        if (isAdminAccount) {
                            adminTab = adminTabForAlert(notif.title, notif.body)
                            adminRefreshSignal++
                        } else {
                            showToast("Bu bildirim yönetici paneline özeldir.", "ℹ️")
                        }
                    }
                    NotificationDestination.RESERVATIONS -> requireAccount { showMyActivity = true }
                    NotificationDestination.VENUE_MANAGEMENT -> requireAccount { showVenueOperator = true }
                }
            },
            onClearNotifications = { id -> if (!remote("notification.clear", apiPayload("notificationId" to id))) {
                notifications = if (id == null) emptyList() else notifications.filterNot { it.id == id }
                showToast(if (id == null) "Bildirimler temizlendi" else "Bildirim silindi", "✓")
            } },
            onMarkAllRead = { if (!(remote("notification.read", apiPayload()))) {
                notifications = notifications.map { it.copy(read = true) }
                showToast("Tüm bildirimler okundu olarak işaretlendi", "✓")
             } }
        )
    }

    // 6. Takım Challenge Teklifi
    if (challengeTargetTeam != null) {
        val target = challengeTargetTeam!!
        val myTeam = teams.firstOrNull { it.id == challengeSourceTeam?.id && it.captainId == DemoData.ME && it.sport == target.sport }
            ?: teams.firstOrNull { it.captainId == DemoData.ME && it.sport == target.sport }
        if (myTeam != null) {
            SendTeamChallengeSheet(
                myTeam = myTeam,
                targetTeam = target,
                tokenBalance = tokenBalance,
                onDismiss = { challengeTargetTeam = null; challengeSourceTeam = null },
                onSendChallenge = { challenge ->
                    sendTeamChallenge(challenge)
                },
                onRequestTokens = {
                    challengeTargetTeam = null
                    challengeSourceTeam = null
                    showTokenTopUpSheet = true
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { challengeTargetTeam = null; challengeSourceTeam = null },
                title = { Text("Kaptan Takımı Gerekli") },
                text = { Text("${target.name} takımına meydan okumak için bu branşta (${target.sport.label}) kaptanı olduğun bir takımın olmalı.") },
                confirmButton = {
                    TextButton(onClick = {
                        challengeTargetTeam = null
                        challengeSourceTeam = null
                        competitionEntryTab = 2
                        currentTab = 2
                    }) { Text("Takım Kur") }
                },
                dismissButton = {
                    TextButton(onClick = { challengeTargetTeam = null; challengeSourceTeam = null }) { Text("Vazgeç") }
                }
            )
        }
    }

    // 7. Şikayet ve İtiraz / Dispute
    if (reportTargetName != null) {
        ReportAndDisputeDialog(
            targetName = reportTargetName!!,
            onDismiss = { reportTargetName = null },
            onSubmitReport = { reason, details ->
                if (!remote("report.create", apiPayload("targetType" to reportTargetType, "targetId" to reportTargetId, "reason" to reason, "details" to details), after = {
                    showToast("Bildirim kaydın Hesap → Destek bölümünde", "🛡️")
                })) {
                    moderationReports = listOf(ModerationReport(nextUniqueId("report"), reportTargetName.orEmpty(), reason, details)) + moderationReports
                    showToast("Bildirim kaydın Hesap → Destek bölümünde", "🛡️")
                }
            }
        )
    }

    // 8. Apple Pay & Google Play In-App Purchase Token Yükleme
    if (showTokenTopUpSheet) {
        TokenTopUpCardSheet(
            onDismiss = { showTokenTopUpSheet = false },
            initialPack = selectedTopUpPack,
            currentBalance = tokenBalance,
            lockedTokens = lockedTokens,
            activeProPlan = activeProPlan,
            onSubscribePlan = { activateDemoPlan(it) },
            userName = profile.name,
            ledger = ledger,
            onConfirmTopUp = { pack, method -> if (!(backend.enabled.also { if (it) showToast("Satın alma şu anda kullanılamıyor.", "ℹ️") })) {
                val added = pack.tokens + pack.bonus
                tokenBalance += added
                ledger = listOf(
                    LedgerEntry(
                        id = nextUniqueId("l_topup"),
                        type = LedgerType.PURCHASE,
                        amount = added,
                        description = "${pack.tokens} TKN Yüklendi ($method)",
                        time = "Şimdi",
                        idempotencyKey = nextUniqueId("topup")
                    )
                ) + ledger
                showToast("$added TKN hesabına yüklendi! ($method)", "💎")
             } }
        )
    }

    // 9. Konum ve Şehir Seçim Çekmecesi (Canlı GPS & İlçe Ağı)
    if (showCityLocationSheet) {
        CityAndLocationSelectorSheet(
            currentCity = currentCity,
            currentDistrict = currentDistrict,
            isGpsActive = isGpsLocationActive,
            onDismiss = { showCityLocationSheet = false },
            onSelectLocation = { newCity, newDistrict, isGps ->
                val cleanCity = canonicalTurkishCityName(newCity).ifBlank { currentCity }
                val cleanDistrict = canonicalTurkishDistrictName(newDistrict).ifBlank { "Merkez" }
                browseRegionSelected = true
                manualNonGpsRegion = !isGps
                currentCity = cleanCity
                currentDistrict = cleanDistrict
                isGpsLocationActive = isGps
                savePersistedRegionSelection(
                    PersistedRegionSelection(
                        city = cleanCity,
                        district = cleanDistrict,
                        isGps = isGps,
                        manualNonGps = !isGps,
                        latitude = if (isGps) DeviceLocationState.current.value?.latitude else null,
                        longitude = if (isGps) DeviceLocationState.current.value?.longitude else null
                    )
                )
                if (backend.enabled && isLoggedIn) remote("discovery.save", apiPayload("city" to cleanCity, "district" to cleanDistrict, "travelMode" to false), silent = true)
                val msg = if (isGps) {
                    "Güncel konum seçildi: $cleanDistrict, $cleanCity"
                } else {
                    "Bölge seçildi: $cleanCity · $cleanDistrict (Maç ağı güncellendi)"
                }
                showToast(msg, "📍")
            }
        )
    }

    // 10. Keşfet Menüsü (Yarış, Ağ)
    if (showMenuSheet) {
        ExploreMenuSheet(
            selectedTab = currentTab,
            onDismiss = { showMenuSheet = false },
            onSelectTab = { tab ->
                if (tab in setOf(2, 3)) {
                    requireAccount { currentTab = tab }
                } else {
                    currentTab = tab
                }
            }
        )
    }

    // 11. Giriş ve Pasaport Doldurma Çekmecesi
    if (showAuthSheet) CompositionLocalProvider(LocalBackendRepository provides authBackend) {
        val authProfile = authWire?.let(::BackendSnapshot)?.profile
        AuthAndOnboardingSheet(
            onDismiss = { showAuthSheet = false; pendingAccountAction = null },
            initialStage = when {
                authBackend.authenticated && authProfile != null && authWire?.b("legalAcceptanceRequired") == true -> AuthStage.LEGAL_REVIEW
                authBackend.authenticated && authWire != null && authProfile == null -> AuthStage.PASSPORT_SETUP
                else -> AuthStage.LOGIN_OPTIONS
            },
            backend = authBackend,
            externallyBusy = sessionLeaving || completingAuthentication,
            existingProfile = authProfile,
            currentCity = currentCity,
            currentDistrict = currentDistrict,
            onCompletePassport = { newProfile, _ -> completeAuthentication(newProfile) },
            onContinueAsGuest = {
                pendingAccountAction = null; showAuthSheet = false
                isLoggedIn = false; hasCompletedPassport = false
                if (authBackend.authenticated) demoScope.launch {
                    sessionLeaving = true
                    runCatching { authBackend.logout() }
                    sessionLeaving = false
                    isLoggedIn = false; hasCompletedPassport = false; profile = guestProfile
                    if (backend.enabled) runCatching { backend.reload() }
                }
                else { isLoggedIn = false; hasCompletedPassport = false }
            }
        )
    }
    if (showContactVerification) ContactVerificationSheet(profile.phone,
        backend = backend.takeIf { it.enabled },
        onRecordRequest = { phone ->
            profile = profile.copy(phone = phone, verifications = profile.verifications + VerificationLevel.BASIC)
            supportRequests = listOf(SupportRequest(nextUniqueId("contact_verify"), "İletişim doğrulaması", "Telefon doğrulama talebi", "Hesap")) + supportRequests
            showToast("Telefon doğrulama talebin kaydedildi", "✓")
        }, onDismiss = { showContactVerification = false })
    if (showPhotoVerification) PhotoVerificationSheet(profile,
        onPhotoRequest = { photo, completed -> if (!remotePhotoVerification(photo, completed)) {
            profile = profile.copy(avatar = photo ?: profile.avatar)
            supportRequests = listOf(SupportRequest(nextUniqueId("photo_verify"), "Fotoğraf doğrulaması", "Fotoğraf inceleme talebi", "Hesap")) + supportRequests
            showToast("Fotoğraf doğrulama talebin kaydedildi", "✓")
            completed(true)
        } }, onDismiss = { showPhotoVerification = false })
    if (showAccountSettings) AccountSettingsSheet(
        privacyPrefs, { prefs -> if (!remote("preferences.save", apiPayload("privacy" to JsonObject(prefs.toApiPayload().filter { (key, value) -> privacyPrefs.toApiPayload()[key] != value })))) privacyPrefs = prefs },
        notificationPrefs, { prefs -> if (!remote("preferences.save", apiPayload("notifications" to JsonObject(prefs.toApiPayload().filter { (key, value) -> notificationPrefs.toApiPayload()[key] != value })))) notificationPrefs = prefs },
        threads.filter { if (backend.enabled) live?.otherUserForThread(it.id) in live?.wire?.ss("blockedUserIds").orEmpty() else it.id in blockedChatIds }, { threadId ->
            if (!backend.enabled) {
                blockedChatIds = blockedChatIds - threadId
                showToast("Engel kaldırıldı", "🛡️")
            } else live?.otherUserForThread(threadId)?.let { remote("user.block", apiPayload("userId" to it, "blocked" to false)) }
        },
        supportRequests, moderationReports,
        onSupport = { subject, message, category -> if (!remote("support.create", apiPayload("subject" to subject, "message" to message, "category" to category))) { supportRequests = listOf(SupportRequest(nextUniqueId("support"), subject, message, category)) + supportRequests; showToast("Destek talebin alındı", "💬") } },
        onLogout = { if (!(remoteLogout())) { isLoggedIn = false; showAccountSettings = false; currentTab = 5; activeChatThreadId = null; showToast("Çıkış yapıldı · Misafir modundasın", "ℹ️") } },
        onRequestDeletion = { if (!remote("account.delete", apiPayload())) { if (supportRequests.none { it.subject == "Hesap silme talebi" }) supportRequests = listOf(SupportRequest(nextUniqueId("deletion"), "Hesap silme talebi", "Hesap ve kişisel verilerin silinmesi talebi", "Hesap")) + supportRequests; showToast("Hesap silme talebi kaydedildi", "ℹ️") } },
        onDismiss = { showAccountSettings = false }
    )
    if (showMyActivity) MyActivitySheet(events, joinedEventIds, waitlistedEventIds,
        joinRequests.filter { it.player.id == DemoData.ME && it.status == JoinRequestStatus.PENDING },
        venuePayments, { showMyActivity = false; selectedDetailEvent = it },
        onCancelReservation = { payment -> if (!(remote("booking.cancel", apiPayload("bookingId" to payment.id)))) {
            venuePayments = venuePayments.map { if (it.id == payment.id) it.copy(status = PaymentStatus.CANCELLED) else it }
            payment.bookingKey?.let { bookedSlotIds = bookedSlotIds - it }
            showToast("Rezervasyon iptal edildi", "↩️")
         } },
        onCreateListingForReservation = { payment ->
            showMyActivity = false
            playNowInitialVenue = payment.venue
            val extractedTime = Regex("""\b(\d{1,2}:\d{2})\b""").find(payment.description)?.value
            playNowInitialTime = extractedTime
            showPlayNowSheet = true
        },
        onDismiss = { showMyActivity = false })
    resultEvent?.let { selectedEvent ->
        val event = events.firstOrNull { it.id == selectedEvent.id } ?: selectedEvent
        MatchResultSheet(event, matchResults[event.id], players = (remotePlayers + selectedDetailPlayers).distinctBy { it.id },
        isCheckedIn = !backend.enabled || event.id in checkedInEventIds,
        canSubmit = (event.id in joinedEventIds || event.hostId == DemoData.ME) && (!backend.enabled || event.id in checkedInEventIds) && event.status in setOf(EventStatus.IN_PROGRESS, EventStatus.COMPLETED, EventStatus.VERIFIED, EventStatus.DISPUTED),
        onSubmit = { result -> if (!remote("result.submit", apiPayload("eventId" to event.id, "homeScore" to result.homeScore, "awayScore" to result.awayScore, "note" to result.note, "sides" to mapOf("home" to result.homeParticipantIds.map(backend::serverUserId), "away" to result.awayParticipantIds.map(backend::serverUserId))))) { matchResults = matchResults + (event.id to result); showToast("Sonuç kaydedildi · doğrulama bekleniyor", "✓") } },
        onDispute = { reason ->
            if (!remote("result.dispute", apiPayload("eventId" to event.id, "reason" to reason))) {
                matchResults[event.id]?.let { matchResults = matchResults + (event.id to it.copy(disputed = true, disputeReason = reason)) }
                moderationReports = listOf(ModerationReport(nextUniqueId("result_dispute"), event.title, "Skor itirazı", reason)) + moderationReports
            }
        }, onDismiss = { resultEvent = null },
        onConfirm = {
            if (!remote("result.confirm", apiPayload("eventId" to event.id))) {
                matchResults[event.id]?.let { matchResults = matchResults + (event.id to it.copy(status = "verified")) }
                showToast("Maç sonucu onaylandı", "🏆")
            }
        },
        onOpenFeedback = { feedbackEvent = event }) }
    feedbackEvent?.let { event ->
        val candidatePool = if (backend.enabled) (remotePlayers + selectedDetailPlayers).distinctBy { it.id } else DemoData.players
        PlayerFeedbackSheet(candidatePool.filter { it.id != DemoData.ME && it.id in event.participantIds && (!backend.enabled || event.checkedInParticipantIds.isEmpty() || it.id in event.checkedInParticipantIds) && (event.id to backend.serverUserId(it.id)) !in live?.submittedFeedbackTargets.orEmpty() },
        onSubmit = { target, fairPlay, again ->
            if (!remote("feedback.submit", apiPayload("eventId" to event.id, "targetId" to backend.serverUserId(target), "fairPlay" to fairPlay, "playAgain" to again)) { feedbackEvent = null }) {
                feedbackEvent = null
                showToast("Değerlendirmen kaydedildi", "⭐")
            }
        },
        onDismiss = { feedbackEvent = null }) }

    val uploadProgress by backend.uploadProgress.collectAsState()
    var showBusyPopup by remember { mutableStateOf(false) }
    LaunchedEffect(backendBusy) {
        if (backendBusy > 0) {
            kotlinx.coroutines.delay(450L)
            showBusyPopup = true
        } else {
            showBusyPopup = false
        }
    }
    val busyPopupText = if (showBusyPopup) when (val progress = uploadProgress) {
        null -> "Güncelleniyor…"
        -1 -> "Fotoğraf hazırlanıyor…"
        100 -> "Fotoğraf işleniyor…"
        else -> "Fotoğraf yükleniyor · %$progress"
    } else null

    if (currentToast != null || busyPopupText != null) androidx.compose.ui.window.Popup(alignment = Alignment.TopCenter) {
        MToastBanner(toast = currentToast, busyMessage = busyPopupText)
    }

    } // Persistent command host survives child sheet dismissal.

}

// ═══════════════════════════════════════════════════════════════════════════════
// ═══════════════════════════════════════════════════════════════════════════════
// MATCHORA APP DOCK (All main surfaces remain directly reachable)
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun FloatingAppDock(
    selectedTab: Int,
    unreadChats: Int = 0,
    onSelectTab: (Int) -> Unit,
    onPlayNowClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = DockCutoutShape, color = Color.White,
            shadowElevation = 6.dp, border = BorderStroke(1.dp, MColors.Line),
            modifier = Modifier.fillMaxWidth().height(72.dp)
        ) {
            Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { DockNavItem(Icons.Rounded.Home, "Akış", selectedTab == 0, onClick = { onSelectTab(0) }) }
                Box(Modifier.weight(1f)) { DockNavItem(Icons.Rounded.Map, "Harita", selectedTab == 1, onClick = { onSelectTab(1) }) }
                Spacer(Modifier.width(84.dp))
                Box(Modifier.weight(1f)) { DockNavItem(Icons.Rounded.Forum, "Sohbet", selectedTab == 4, badgeCount = unreadChats, onClick = { onSelectTab(4) }) }
                Box(Modifier.weight(1f)) { DockNavItem(Icons.Rounded.Badge, "Pasaport", selectedTab == 5, onClick = { onSelectTab(5) }) }
            }
        }
        FloatingPlayButton(onClick = onPlayNowClick, modifier = Modifier.align(Alignment.TopCenter).offset(y = (-20).dp))
    }
}

@Composable
private fun FloatingPlayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .pressMotion(interaction)
            .size(72.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(MGradients.Brand)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MGradients.Brand),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.FlashOn,
                    contentDescription = "Şimdi Oyna",
                    tint = Color.White,
                    modifier = Modifier.size(27.dp)
                )
            }
        }
    }
}

@Composable
private fun DockNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    val activeColor = MColors.Brand
    val inactiveColor = MColors.Ink400
    val interaction = remember { MutableInteractionSource() }

    val animatedTint by animateColorAsState(
        targetValue = if (isSelected) activeColor else inactiveColor,
        animationSpec = tween(180),
        label = "navTint"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 520f),
        label = "navIconScale"
    )
    val indicatorWidth by animateDpAsState(
        targetValue = if (isSelected) 14.dp else 0.dp,
        animationSpec = tween(180),
        label = "navIndicatorWidth"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pressMotion(interaction)
            .semantics { selected = isSelected }
            .clickable(
                role = Role.Tab,
                onClick = onClick,
                interactionSource = interaction,
                indication = null
            )
            .heightIn(min = 54.dp)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BadgedBox(
            badge = {
                if (badgeCount > 0) {
                    Badge(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White,
                        modifier = Modifier.offset(x = 5.dp, y = (-8).dp)
                    ) {
                        Text(
                            text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = animatedTint,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                        alpha = 0.99f
                    }
                    .drawWithCache {
                        onDrawWithContent {
                            drawContent()
                            if (isSelected) {
                                drawRect(MGradients.Brand, blendMode = BlendMode.SrcAtop)
                            }
                        }
                    }
            )
        }
        Text(
            text = label,
            color = animatedTint,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .width(indicatorWidth)
                .height(2.5.dp)
                .clip(CircleShape)
                .background(MGradients.Brand)
        )
    }
}
