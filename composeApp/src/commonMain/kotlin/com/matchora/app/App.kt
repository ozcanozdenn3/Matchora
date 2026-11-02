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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

// ═══════════════════════════════════════════════════════════════════════════════
// MATCHORA SPORTS OPERATING SYSTEM (SPORTS NETWORK OS)
// Pure Light Mode · Sports Flow Background (Gri Tenis Kortu, Kale, Pota, Emojiler)
// Uçtan uca mimari: Şimdi Oyna + Eksik Oyuncu + Sports Passport + Reliability +
// Team Challenge + Yerel Ranking + Commitment Token + Sports Chemistry + Venues
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun App() {
    var showSplash by remember { mutableStateOf(true) }

    MatchoraTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            SportsFlowBackground(animate = !showSplash) {
                MainAppShell()
            }

            if (showSplash) {
                MatchoraSplashScreen(
                    onSplashFinished = { showSplash = false }
                )
            }
        }
    }
}

@Composable
private fun MainAppShell() {
    val clipboardManager = LocalClipboardManager.current
    val demoScope = rememberCoroutineScope()
    // ═══════════════ STATE MANAGEMENT ═══════════════
    var currentTab by remember { mutableIntStateOf(0) }
    var scrollToTopTrigger by remember { mutableIntStateOf(0) }
    var competitionEntryTab by remember { mutableIntStateOf(0) }
    var currentCity by remember { mutableStateOf("İstanbul") }
    var currentDistrict by remember { mutableStateOf("Kadıköy") }
    var isGpsLocationActive by remember { mutableStateOf(false) }
    var tokenBalance by remember { mutableIntStateOf(120) }
    var lockedTokens by remember { mutableIntStateOf(20) }
    var selectedTopUpPack by remember { mutableStateOf(DemoData.tokenPacks.getOrNull(1) ?: DemoData.tokenPacks.first()) }
    var activeProPlan by remember { mutableStateOf<ProPlan?>(null) }
    var isLoggedIn by remember { mutableStateOf(true) }

    var profile by remember { mutableStateOf(DemoData.me) }
    var events by remember { mutableStateOf(DemoData.events) }
    var joinedEventIds by remember { mutableStateOf(setOf("e5", "e7")) }
    var waitlistedEventIds by remember { mutableStateOf(emptySet<String>()) }
    var checkedInEventIds by remember { mutableStateOf(setOf<String>()) }
    var joinRequests by remember { mutableStateOf(DemoData.initialJoinRequests) }

    var threads by remember { mutableStateOf(DemoData.chats) }
    var memoryPosts by remember { mutableStateOf(DemoData.initialMemoryPosts) }
    var activeChatThreadId by remember { mutableStateOf<String?>(null) }
    var notifications by remember { mutableStateOf(DemoData.notifications) }
    var ledger by remember { mutableStateOf(DemoData.initialLedger) }
    var venuePayments by remember { mutableStateOf(DemoData.initialVenuePayments) }
    var bookedSlotIds by remember { mutableStateOf(emptySet<String>()) }
    var paymentMethods by remember { mutableStateOf(DemoData.initialPaymentMethods) }

    var teams by remember { mutableStateOf(DemoData.teams) }
    var tournaments by remember { mutableStateOf(DemoData.tournaments) }
    var registeredTournamentIds by remember { mutableStateOf(emptySet<String>()) }
    var invitedPlayerIds by remember { mutableStateOf(emptySet<String>()) }
    var challenges by remember { mutableStateOf(DemoData.challenges) }
    var teamJoinRequests by remember { mutableStateOf(DemoData.teamJoinRequests) }
    var joinedCommunityIds by remember { mutableStateOf(setOf<String>()) }
    var requestedCommunityIds by remember { mutableStateOf(setOf<String>()) }

    val currentPlayer = remember(profile) {
        val primarySkill = profile.skills.firstOrNull()
        DemoData.mePlayer.copy(
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
    var showPlayNowSheet by remember { mutableStateOf(false) }
    var showWalletSheet by remember { mutableStateOf(false) }
    var showCreateEventSheet by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var challengeTargetTeam by remember { mutableStateOf<TeamProfile?>(null) }
    var reportTargetName by remember { mutableStateOf<String?>(null) }
    var showTokenTopUpSheet by remember { mutableStateOf(false) }
    var showCityLocationSheet by remember { mutableStateOf(false) }
    var showSubmitJoinSheetForEvent by remember { mutableStateOf<SportEvent?>(null) }
    var showManageJoinRequestsForEvent by remember { mutableStateOf<SportEvent?>(null) }
    var showMenuSheet by remember { mutableStateOf(false) }
    var showAuthSheet by remember { mutableStateOf(false) }
    var currentToast by remember { mutableStateOf<Toast?>(null) }

    val unreadNotificationsCount = remember(notifications) {
        notifications.count { !it.read }
    }
    val unreadChatThreadsCount = remember(threads) {
        threads.count { it.unread > 0 }
    }

    // Toast Gösterici Fonksiyon
    fun showToast(message: String, emoji: String = "✓") {
        currentToast = Toast(message, emoji, ++nextIdCounter)
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

    fun joinEventImmediately(event: SportEvent, reason: String) {
        if (joinedEventIds.contains(event.id)) {
            showToast("Zaten bu karşılaşmanın kadrosundasın", "ℹ️")
            return
        }
        if (event.status !in setOf(EventStatus.PUBLISHED, EventStatus.FILLING) || event.freeSpots <= 0) {
            showToast("Bu karşılaşmada boş yer kalmadı", "ℹ️")
            return
        }
        if (!lockTokens(10, "Katılım")) return

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
            LedgerEntry(nextUniqueId("l_hold"), LedgerType.HOLD, -10, "$reason · ${event.title}", "Şimdi", idempotencyKey = operationId)
        ) + ledger
        showToast("10 TKN kilitlendi · ${event.title} kadrosuna katıldın", "✓")
    }

    fun openDirectChatWithPlayer(targetPlayer: Player) {
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
        selectedDetailEvent = null
        showSubmitJoinSheetForEvent = null
        showManageJoinRequestsForEvent = null
        currentTab = 4 // Sohbetler sekmesi
        showToast("${targetPlayer.name} ile sohbet açıldı", "💬")
    }

    fun approveJoinRequest(req: JoinRequest) {
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
        if (req.status != JoinRequestStatus.PENDING) return
        joinRequests = joinRequests.map {
            if (it.id == req.id) it.copy(status = JoinRequestStatus.REJECTED) else it
        }
        showToast("${req.player.name} başvurusu reddedildi", "✕")
    }

    fun submitJoinRequest(ev: SportEvent, position: String, note: String) {
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
        if (joinRequests.none { it.id == req.id && it.status == JoinRequestStatus.PENDING }) return
        joinRequests = joinRequests.filter { it.id != req.id }
        val released = releaseTokens(10)
        ledger = listOf(
            LedgerEntry(nextUniqueId("l_rel"), LedgerType.RELEASE, released, "Kadro başvuru iadesi", "Şimdi", idempotencyKey = "rel-${req.id}")
        ) + ledger
        showToast("Başvurun geri çekildi · 10 TKN iade edildi", "↩️")
    }

    fun createTeam(newTeam: TeamProfile) {
        teams = listOf(newTeam) + teams
        showToast("${newTeam.name} kulübü kuruldu!", "🛡️")
    }

    fun sendTeamChallenge(challenge: ChallengeOffer) {
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
        challenges = challenges.map {
            if (it.id == challenge.id) it.copy(status = ChallengeStatus.DECLINED) else it
        }
        showToast("Meydan okuma reddedildi", "✕")
    }

    fun applyToTeam(team: TeamProfile, position: String, note: String) {
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
        teamJoinRequests = teamJoinRequests.map {
            if (it.id == req.id) it.copy(status = JoinRequestStatus.REJECTED) else it
        }
        showToast("${req.player.name} başvurusu reddedildi", "✕")
    }

    // Otomatik Toast Kapanışı
    LaunchedEffect(currentToast) {
        if (currentToast != null) {
            delay(2800)
            currentToast = null
        }
    }

    Scaffold(
        containerColor = Color.Transparent, // Şeffaf zemin: Arkadaki akıcı spor desenleri görünür!
        topBar = {
            if (currentTab != 1 && !(currentTab == 4 && activeChatThreadId != null)) {
                MTopBar(
                    currentCity = currentCity,
                    currentDistrict = currentDistrict,
                    isGpsActive = isGpsLocationActive,
                    tokenBalance = tokenBalance,
                    unreadNotifications = unreadNotificationsCount,
                    isMenuActive = currentTab in setOf(2, 3),
                    isLoggedIn = isLoggedIn,
                    onOpenAuth = { showAuthSheet = true },
                    onOpenWallet = { showWalletSheet = true },
                    onOpenNotifications = { showNotificationsSheet = true },
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
                    unreadChats = unreadChatThreadsCount,
                    onSelectTab = { clickedTab ->
                        if (currentTab == clickedTab) {
                            scrollToTopTrigger++
                        } else {
                            currentTab = clickedTab
                        }
                    },
                    onPlayNowClick = { showPlayNowSheet = true }
                )
            }
        }
    ) { paddingValues ->
        CompositionLocalProvider(LocalScrollToTopTrigger provides scrollToTopTrigger) {
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
                    currentPlayer = currentPlayer,
                    onSelectEvent = { selectedDetailEvent = it },
                    onOpenMap = { currentTab = 1 },
                    onOpenPlayNow = { showPlayNowSheet = true },
                    onOpenEmergencyFill = {
                        val em = events.firstOrNull { it.emergency }
                        if (em != null) selectedDetailEvent = em
                    },
                    onOpenAutoSquad = {
                        val squad = threads.firstOrNull { it.type == ThreadType.SQUAD }
                        if (squad != null) {
                            activeChatThreadId = squad.id
                            currentTab = 4
                            showToast("Perşembe Auto-Squad sohbeti açıldı", "✨")
                        } else {
                            currentTab = 4
                            showToast("Sana uygun bir Auto-Squad sohbeti bulunamadı", "ℹ️")
                        }
                    },
                    onOpenTeamChallenge = { competitionEntryTab = 2; currentTab = 2 },
                    onOpenLeaderboard = { competitionEntryTab = 0; currentTab = 2 },
                    onOpenWallet = { showWalletSheet = true },
                    onOpenProfile = { currentTab = 5 }
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
                    onLocationClick = { showCityLocationSheet = true },
                    onSelectEvent = { selectedDetailEvent = it },
                    onQuickJoin = { ev ->
                        joinEventImmediately(ev, "Taahhüt")
                    },
                    onToggleWaitlist = { ev ->
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
                )
                2 -> CompetitionScreen(
                    paddingValues = paddingValues,
                    initialTab = competitionEntryTab,
                    teams = teams,
                    currentPlayer = currentPlayer,
                    user = profile,
                    tournaments = tournaments,
                    registeredTournamentIds = registeredTournamentIds,
                    invitedPlayerIds = invitedPlayerIds,
                    tokenBalance = tokenBalance,
                    newTournamentId = { nextUniqueId("tr") },
                    onRequestTokens = {
                        showTokenTopUpSheet = true
                    },
                    challenges = challenges,
                    teamJoinRequests = teamJoinRequests,
                    onCreateTeam = { createTeam(it) },
                    onOpenChallengeSheet = { _, target -> challengeTargetTeam = target },
                    onAcceptChallenge = { acceptTeamChallenge(it) },
                    onDeclineChallenge = { declineTeamChallenge(it) },
                    onApplyToTeam = { team, pos, note -> applyToTeam(team, pos, note) },
                    onApproveTeamJoinRequest = { approveTeamJoinRequest(it) },
                    onRejectTeamJoinRequest = { rejectTeamJoinRequest(it) },
                    onOpenDirectChat = { openDirectChatWithPlayer(it) },
                    onInvitePlayerToTeam = { player ->
                        invitedPlayerIds = invitedPlayerIds + player.id
                        val teamName = teams.firstOrNull { it.isMine }?.name ?: "takımın"
                        showToast("${player.name}, $teamName kadrosuna davet edildi", "🛡️")
                    },
                    onCreateTournament = { tournament ->
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
                    },
                    onTournamentRegistered = { tournament ->
                        registeredTournamentIds = registeredTournamentIds + tournament.id
                        showToast("${tournament.name} için demo kaydın alındı", "🏆")
                    },
                    onWithdrawTournamentRegistration = { tournament ->
                        registeredTournamentIds = registeredTournamentIds - tournament.id
                        showToast("${tournament.name} başvurun geri çekildi", "↩️")
                    },
                    onCancelTournament = { tournament ->
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
                    }
                )
                3 -> CommunityAndVenueScreen(
                    paddingValues = paddingValues,
                    currentDistrict = currentDistrict,
                    joinedCommunityIds = joinedCommunityIds,
                    requestedCommunityIds = requestedCommunityIds,
                    bookedSlotIds = bookedSlotIds,
                    onBookSlot = { venue, slot, splitCount, amount, method ->
                        val bookingKey = "${venue.id}:${slot.id}"
                        if (bookingKey !in bookedSlotIds && slot.available) {
                        if (amount > tokenBalance) {
                            showToast("Yetersiz token · bakiyeni yükle", "ℹ️")
                            showTokenTopUpSheet = true
                        } else {
                        tokenBalance -= amount
                        ledger = listOf(LedgerEntry(nextUniqueId("l_venue"), LedgerType.SPEND, -amount, "Saha rezervasyonu · ${venue.name}", "Şimdi", idempotencyKey = nextUniqueId("venue"))) + ledger
                        bookedSlotIds = bookedSlotIds + bookingKey
                        val newPayment = VenuePayment(
                            id = nextUniqueId("vp"),
                            venue = venue.name,
                            description = "${slot.court} · ${slot.day} ${slot.time} · $splitCount kişilik paylaşım",
                            amountTry = amount,
                            method = method,
                            time = "Şimdi",
                            status = PaymentStatus.PAID
                        )
                        venuePayments = listOf(newPayment) + venuePayments
                        showToast("Rezervasyon tamamlandı · $amount TKN", "🏟️")
                        }
                        } else showToast("Bu saat zaten rezerve edildi", "ℹ️")
                    },
                    onJoinCommunity = { com ->
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
                    }
                )
                4 -> ChatHubScreen(
                    paddingValues = paddingValues,
                    threads = threads,
                    memories = memoryPosts,
                    currentCity = currentCity,
                    currentDistrict = currentDistrict,
                    currentUser = profile,
                    initialActiveThreadId = activeChatThreadId,
                    onActiveThreadChange = { threadId ->
                        activeChatThreadId = threadId
                        if (threadId != null) {
                            threads = threads.map { thread ->
                                if (thread.id == threadId) thread.copy(unread = 0) else thread
                            }
                        }
                    },
                    onSendMessage = { threadId, text ->
                        val message = text.trim()
                        val target = threads.firstOrNull { it.id == threadId }
                        if (message.isNotEmpty() && target != null && !target.locked) {
                            threads = threads.map { th ->
                                if (th.id == threadId) th.copy(messages = th.messages + ChatMsg(
                                    id = nextUniqueId("m"), senderId = DemoData.ME,
                                    senderName = profile.name, text = message, time = "Şimdi", isMine = true
                                )) else th
                            }
                            demoScope.launch {
                                delay(1200)
                                threads = threads.map { th ->
                                    if (th.id == threadId) th.copy(
                                        messages = th.messages + ChatMsg(
                                            id = nextUniqueId("m_demo"), senderId = "demo_partner", senderName = "Demo sporcu",
                                            text = "Mesajını aldım! Buluşma detaylarını buradan konuşabiliriz. Görüşürüz 👋", time = "Şimdi"
                                        ),
                                        unread = if (currentTab == 4 && activeChatThreadId == threadId) 0 else th.unread + 1
                                    ) else th
                                }
                            }
                        }
                    },
                    onSendMemoryToChat = { threadId, caption, memory ->
                        threads = threads.map { th ->
                            if (th.id == threadId) th.copy(
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
                    },
                    onCreateMemoryPost = { newPost, targetThreadId ->
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
                    },
                    onToggleLikeMemory = { memoryId ->
                        memoryPosts = memoryPosts.map { post ->
                            if (post.id == memoryId) {
                                val nowLiked = !post.likedByMe
                                post.copy(
                                    likedByMe = nowLiked,
                                    likes = (post.likes + if (nowLiked) 1 else -1).coerceAtLeast(0)
                                )
                            } else post
                        }
                    }
                )
                5 -> SportsPassportScreen(
                    paddingValues = paddingValues,
                    user = profile,
                    activeProPlan = activeProPlan,
                    isLoggedIn = isLoggedIn,
                    onOpenAuth = { showAuthSheet = true },
                    onLogout = {
                        isLoggedIn = false
                        showToast("Çıkış yapıldı · Misafir modundasın", "ℹ️")
                    },
                    onOpenWallet = { showWalletSheet = true },
                    onVerifyContact = {
                        if (profile.phone.count { it.isDigit() } < 7 || !profile.email.contains('@')) {
                            showToast("Önce geçerli telefon ve e-posta bilgisi ekle", "ℹ️")
                        } else {
                            profile = profile.copy(verifications = profile.verifications + VerificationLevel.BASIC)
                            showToast("İletişim bilgilerin demo olarak doğrulandı", "✓")
                        }
                    },
                    onVerifyPhoto = {
                        if (VerificationLevel.PHOTO in profile.verifications) {
                            showToast("Fotoğrafın zaten doğrulanmış", "✓")
                        } else {
                            profile = profile.copy(verifications = profile.verifications + VerificationLevel.PHOTO)
                            showToast("Demo fotoğraf doğrulaması tamamlandı", "🤳")
                        }
                    },
                    onUpdateProfile = { updatedProfile ->
                        profile = updatedProfile
                        currentCity = updatedProfile.city
                        currentDistrict = updatedProfile.area
                        showToast("Sporcu profilin güncellendi", "✓")
                    }
                )
                else -> Unit
                }
            }


        }
        }
    }

    if (currentToast != null) androidx.compose.ui.window.Popup(alignment = Alignment.TopCenter) {
        MToastBanner(toast = currentToast)
    }

    // ═══════════════ MODAL BOTTOM SHEETS ═══════════════

    // 1. Etkinlik Detay Sayfası
    if (selectedDetailEvent != null) {
        val ev = events.firstOrNull { it.id == selectedDetailEvent!!.id } ?: selectedDetailEvent!!
        val isJoined = joinedEventIds.contains(ev.id)
        val isCheckedIn = checkedInEventIds.contains(ev.id)
        val isHost = ev.hostId == DemoData.ME || ev.hostId == "me"
        val pendingRequest = joinRequests.firstOrNull { it.eventId == ev.id && it.player.id == DemoData.ME && it.status == JoinRequestStatus.PENDING }
        val pendingRequestsCount = joinRequests.count { it.eventId == ev.id && it.status == JoinRequestStatus.PENDING }
        val isOnWaitlist = waitlistedEventIds.contains(ev.id)

        EventDetailSheet(
            event = ev,
            currentPlayer = currentPlayer,
            isJoined = isJoined,
            isCheckedIn = isCheckedIn,
            isOnWaitlist = isOnWaitlist,
            isHost = isHost,
            pendingRequest = pendingRequest,
            pendingRequestsCount = pendingRequestsCount,
            onDismiss = { selectedDetailEvent = null },
            onToggleJoin = {
                if (isJoined) {
                    if (isCheckedIn) {
                        showToast("Check-in tamamlanan karşılaşmadan ayrılamazsın", "ℹ️")
                    } else {
                        joinedEventIds = joinedEventIds - ev.id
                        val released = releaseTokens(10)
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
            },
            onToggleWaitlist = {
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
            },
            onRequestJoin = {
                showSubmitJoinSheetForEvent = ev
            },
            onCancelRequest = {
                val r = pendingRequest
                if (r != null) cancelJoinRequest(r)
            },
            onOpenManageRequests = {
                showManageJoinRequestsForEvent = ev
            },
            onDirectMessageHost = {
                val hostPlayer = DemoData.players.firstOrNull { it.id == ev.hostId } ?: DemoData.player("p1")!!
                openDirectChatWithPlayer(hostPlayer)
            },
            onCheckIn = {
                if (!isJoined) {
                    showToast("Check-in için önce kadroya katıl", "ℹ️")
                } else if (checkedInEventIds.contains(ev.id)) {
                    showToast("Check-in zaten tamamlandı", "ℹ️")
                } else {
                    checkedInEventIds = checkedInEventIds + ev.id
                    profile = profile.copy(reliability = (profile.reliability + 2).coerceAtMost(100))
                    val released = releaseTokens(10)
                    ledger = listOf(
                        LedgerEntry(nextUniqueId("l_rel"), LedgerType.RELEASE, released, "Check-in katılım iadesi · ${ev.title}", "Şimdi", idempotencyKey = nextUniqueId("op_checkin"))
                    ) + ledger
                    showToast("Check-in tamamlandı · $released TKN iade edildi", "📍")
                }
            },
            onOpenChat = {
                if (isJoined || isHost) {
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
            },
            onReportEvent = {
                reportTargetName = ev.title
            },
            onShare = {
                clipboardManager.setText(AnnotatedString("https://matchora.app/m/${ev.id}"))
                showToast("Demo davet bağlantısı kopyalandı", "🔗")
            }
        )
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
        PlayNowModalSheet(
            allEvents = events,
            tokenBalance = tokenBalance,
            joinedEventIds = joinedEventIds,
            currentArea = currentDistrict,
            currentCity = currentCity,
            currentCampus = profile.campus,
            currentCompany = profile.company,
            newEventId = { nextUniqueId("ev_ad") },
            onDismiss = { showPlayNowSheet = false },
            onSelectEvent = { ev ->
                showPlayNowSheet = false
                selectedDetailEvent = ev
            },
            onQuickJoin = { ev ->
                showPlayNowSheet = false
                joinEventImmediately(ev, "Hızlı katılım")
            },
            onCreateEvent = { newEvent ->
                if (events.any { it.id == newEvent.id }) {
                    showToast("Bu ilan kimliği zaten kullanılmış; yeniden dene", "⚠️")
                } else if (lockTokens(10, "İlan oluşturma")) {
                    events = listOf(newEvent) + events
                    joinedEventIds = joinedEventIds + newEvent.id
                    ledger = listOf(
                        LedgerEntry(nextUniqueId("l_ad"), LedgerType.HOLD, -10, "İlan Kilidi · ${newEvent.title}", "Şimdi", idempotencyKey = nextUniqueId("op_ad"))
                    ) + ledger
                    showToast("İlan yayınlandı · ${newEvent.title}", "📢")
                }
            },
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
            onDismiss = { showWalletSheet = false },
            onSelectTokenPack = { pack ->
                val added = pack.tokens + pack.bonus
                tokenBalance += added
                ledger = listOf(
                    LedgerEntry(
                        id = nextUniqueId("l_topup"),
                        type = LedgerType.PURCHASE,
                        amount = added,
                        description = "Apple / Google Pay · ${pack.tokens} TKN",
                        time = "Şimdi",
                        idempotencyKey = nextUniqueId("topup")
                    )
                ) + ledger
                showToast("$added TKN Apple Pay ile hesabına yüklendi!", "💎")
            },
            onSubscribePlan = { plan ->
                activeProPlan = plan
                tokenBalance += 100
                ledger = listOf(
                    LedgerEntry(
                        id = nextUniqueId("l_sub"),
                        type = LedgerType.PURCHASE,
                        amount = 100,
                        description = "Matchora Plus (${plan.title}) Hoş Geldin Bonusu",
                        time = "Şimdi",
                        idempotencyKey = nextUniqueId("sub")
                    )
                ) + ledger
                showToast("Matchora Plus üyeliğin aktif! +100 TKN eklendi 👑", "⭐")
            }
        )
    }

    // 4. Etkinlik Oluşturma
    if (showCreateEventSheet) {
        CreateEventSheet(
            onDismiss = { showCreateEventSheet = false },
            newEventId = { nextUniqueId("ev_legacy") },
            onCreateEvent = { newEvent ->
                if (events.any { it.id == newEvent.id }) {
                    showToast("Bu ilan kimliği zaten kullanılmış; yeniden dene", "⚠️")
                } else if (lockTokens(10, "İlan oluşturma")) {
                    events = listOf(newEvent) + events
                    joinedEventIds = joinedEventIds + newEvent.id
                    ledger = listOf(
                        LedgerEntry(nextUniqueId("l_ad"), LedgerType.HOLD, -10, "İlan Kilidi · ${newEvent.title}", "Şimdi", idempotencyKey = nextUniqueId("op_ad"))
                    ) + ledger
                    showToast("${newEvent.title} oluşturuldu ve yayına alındı", "🎉")
                }
            }
        )
    }

    // 5. Bildirimler Merkezi
    if (showNotificationsSheet) {
        NotificationsSheet(
            notifications = notifications,
            onDismiss = { showNotificationsSheet = false },
            onNotificationClick = { notif ->
                notifications = notifications.map { if (it.id == notif.id) it.copy(read = true) else it }
                if (notif.eventId != null) {
                    val ev = events.firstOrNull { it.id == notif.eventId }
                    if (ev != null) {
                        showNotificationsSheet = false
                        selectedDetailEvent = ev
                    }
                }
            },
            onMarkAllRead = {
                notifications = notifications.map { it.copy(read = true) }
                showToast("Tüm bildirimler okundu olarak işaretlendi", "✓")
            }
        )
    }

    // 6. Takım Challenge Teklifi
    if (challengeTargetTeam != null) {
        val myTeam = teams.firstOrNull { it.isMine } ?: teams.first()
        SendTeamChallengeSheet(
            myTeam = myTeam,
            targetTeam = challengeTargetTeam!!,
            tokenBalance = tokenBalance,
            onDismiss = { challengeTargetTeam = null },
            onSendChallenge = { challenge ->
                sendTeamChallenge(challenge)
            }
        )
    }

    // 7. Şikayet ve İtiraz / Dispute
    if (reportTargetName != null) {
        ReportAndDisputeDialog(
            targetName = reportTargetName!!,
            onDismiss = { reportTargetName = null },
            onSubmitReport = { reason, _ ->
                showToast("Bildirimin alındı ($reason). Moderasyon kuyruğuna iletildi.", "🛡️")
            }
        )
    }

    // 8. Apple Pay & Google Play In-App Purchase Token Yükleme
    if (showTokenTopUpSheet) {
        TokenTopUpCardSheet(
            onDismiss = { showTokenTopUpSheet = false },
            initialPack = selectedTopUpPack,
            currentBalance = tokenBalance,
            userName = profile.name,
            ledger = ledger,
            onConfirmTopUp = { pack, method ->
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
            }
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
                currentCity = newCity
                currentDistrict = newDistrict
                isGpsLocationActive = isGps
                val msg = if (isGps) {
                    "Demo konumu seçildi: $newDistrict, $newCity"
                } else {
                    "Bölge seçildi: $newCity · $newDistrict (Maç ağı güncellendi)"
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
                currentTab = tab
            }
        )
    }

    // 11. Giriş, Kayıt ve Sporcu Pasaportu Onboarding Çekmecesi
    if (showAuthSheet) {
        AuthAndOnboardingSheet(
            onDismiss = { showAuthSheet = false },
            onLoginSuccess = { method ->
                isLoggedIn = true
                showAuthSheet = false
                showToast("$method ile giriş yapıldı!", "✅")
            },
            onCompleteOnboarding = { newProfile ->
                profile = newProfile
                isLoggedIn = true
                showAuthSheet = false
                tokenBalance += 20
                ledger = listOf(
                    LedgerEntry(
                        id = nextUniqueId("l_onboard"),
                        type = LedgerType.PURCHASE,
                        amount = 20,
                        description = "Sporcu Pasaportu Hoş Geldin Bonusu",
                        time = "Şimdi",
                        idempotencyKey = nextUniqueId("onboard")
                    )
                ) + ledger
                showToast("Sporcu Pasaportun oluşturuldu! +20 TKN hediye 🎁", "🏆")
            },
            onContinueAsGuest = {
                isLoggedIn = false
                showAuthSheet = false
                showToast("Misafir modunda keşfediyorsun 👁️", "ℹ️")
            }
        )
    }
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
    Box(
        modifier = modifier
            .size(72.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(MGradients.Brand)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
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
    val activeColor = MColors.BrandDeep
    val inactiveColor = MColors.Ink400

    val animatedTint by animateColorAsState(
        targetValue = if (isSelected) activeColor else inactiveColor,
        animationSpec = tween(160),
        label = "navTint"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) MColors.BrandTint else Color.Transparent, RoundedCornerShape(12.dp))
            .semantics { selected = isSelected }
            .clickable(
                role = Role.Tab,
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
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
                modifier = Modifier.size(22.dp)
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
    }
}
