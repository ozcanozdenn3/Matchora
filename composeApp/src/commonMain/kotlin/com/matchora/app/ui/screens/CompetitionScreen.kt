package com.matchora.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.domain.Sport
import com.matchora.app.data.*
import com.matchora.app.state.*
import com.matchora.app.ui.components.*
import com.matchora.app.ui.components.MSegmentedControl
import com.matchora.app.ui.theme.MColors

// ═══════════════════════════════════════════════════════════════════════════════
// EXECUTIVE COMPETITION, TOURNAMENT & TEAM CHALLENGE HUB
// • 1. Yerel Sıralama (Leaderboard & King of City)
// • 2. Turnuvalar & Ligler (Fiziksel Spor & E-Spor Kupaları, Fikstür & Kayıt)
// • 3. Takım Ağı & Meydan Oku (Takım Kurma, Meydan Okuma Kabul/Ret, Kadro Başvuruları)
// • Ultra-Premium Sliding Tab Bar & Akıcı Yatay Geçişler
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun CompetitionScreen(
    paddingValues: PaddingValues,
    initialTab: Int = 0,
    focusedId: String? = null,
    onFocusConsumed: () -> Unit = {},
    onOpenFixture: (String) -> Unit = {},
    remotePlayers: List<Player>? = null,
    remoteRankings: List<com.matchora.app.data.ApiRanking>? = null,
    backendMode: Boolean = false,
    currentPlayer: Player = DemoData.mePlayer,
    user: UserProfile = DemoData.me,
    tournaments: List<TournamentInfo> = DemoData.tournaments,
    registeredTournamentIds: Set<String> = emptySet(),
    invitedPlayerIds: Set<String> = emptySet(),
    tokenBalance: Int = Int.MAX_VALUE,
    newTournamentId: () -> String = { "tr_${(1000..9999).random()}" },
    onRequestTokens: () -> Unit = {},
    teams: List<TeamProfile> = DemoData.teams,
    challenges: List<ChallengeOffer> = DemoData.challenges,
    teamJoinRequests: List<TeamJoinRequest> = DemoData.teamJoinRequests,
    onCreateTeam: (TeamProfile) -> Unit = {},
    onOpenChallengeSheet: (myTeam: TeamProfile, targetTeam: TeamProfile) -> Unit,
    onAcceptChallenge: (ChallengeOffer) -> Unit = {},
    onDeclineChallenge: (ChallengeOffer) -> Unit = {},
    onApplyToTeam: (team: TeamProfile, position: String, note: String) -> Unit = { _, _, _ -> },
    onApproveTeamJoinRequest: (TeamJoinRequest) -> Unit = {},
    onRejectTeamJoinRequest: (TeamJoinRequest) -> Unit = {},
    onOpenDirectChat: (Player) -> Unit = {},
    onInvitePlayerToTeam: (Player, TeamProfile) -> Unit = { _, _ -> },
    onShareTeam: ((TeamProfile) -> Unit)? = null,
    onShareTournament: ((TournamentInfo) -> Unit)? = null,
    onCreateTournament: (TournamentInfo) -> Unit = {},
    onTournamentRegistered: (TournamentInfo) -> Unit = {},
    onWithdrawTournamentRegistration: (TournamentInfo) -> Unit = {},
    onCancelTournament: (TournamentInfo) -> Unit = {}
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) } // 0: Sıralama, 1: Turnuva & Lig, 2: Takım & Meydan Oku
    var selectedSport by remember { mutableStateOf(Sport.FOOTBALL) }
    var selectedScope by remember { mutableStateOf("İlçe") }
    var rankingCity by remember { mutableStateOf(user.city) }
    var rankingDistrict by remember { mutableStateOf(user.area) }
    var showRankingLocation by remember { mutableStateOf(false) }
    var showRankingSports by remember { mutableStateOf(false) }
    val requireAccount = LocalRequireAccount.current
    val rankingBackend = LocalBackendRepository.current
    val rankingActions = rememberCoroutineScope()
    var rankingWire by remember(selectedSport, selectedScope, rankingCity, rankingDistrict) { mutableStateOf<JsonObject?>(null) }
    var rankingBusy by remember { mutableStateOf(false) }
    var rankingError by remember { mutableStateOf<String?>(null) }
    var rankingRetry by remember { mutableIntStateOf(0) }
    val rankingScopeCode = when (selectedScope) { "Ulusal" -> "national"; "Şehir" -> "city"; else -> "district" }
    LaunchedEffect(rankingBackend, selectedSport, selectedScope, rankingCity, rankingDistrict, selectedTab, rankingRetry) {
        if (rankingBackend != null && selectedTab == 0) {
            rankingBusy = true; rankingError = null
            try { rankingWire = rankingBackend.rankings(selectedSport.name, rankingScopeCode, rankingCity, rankingDistrict) }
            catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) { rankingError = error.message }
            finally { rankingBusy = false }
        }
    }
    val rankingValues = if (rankingBackend != null) com.matchora.app.data.BackendSnapshot(com.matchora.app.data.apiPayload("userId" to rankingBackend.userId, "rankings" to rankingWire?.a("items").orEmpty())).rankings else remoteRankings
    var selectedPlayerForProfile by remember { mutableStateOf<LeaderboardRow?>(null) }
    var selectedTournamentForDetail by remember { mutableStateOf<TournamentInfo?>(null) }
    var showCreateTournamentSheet by remember { mutableStateOf(false) }

    // Takım ve Meydan Okuma Modalleri
    var showCreateTeamSheet by remember { mutableStateOf(false) }
    var selectedTeamForDetail by remember { mutableStateOf<TeamProfile?>(null) }
    var showManageTeamRequestsSheet by remember { mutableStateOf(false) }
    var managedTeamId by remember { mutableStateOf<String?>(null) }
    var challengeTarget by remember { mutableStateOf<TeamProfile?>(null) }
    var inviteTarget by remember { mutableStateOf<Player?>(null) }
    var challengeNotice by remember { mutableStateOf<String?>(null) }
    var teamToApply by remember { mutableStateOf<TeamProfile?>(null) }
    var selectedChallengeForDetail by remember { mutableStateOf<ChallengeOffer?>(null) }
    var showAiMatchScouterSheet by remember { mutableStateOf(false) }
    var selectedTournamentSport by remember { mutableStateOf<Sport?>(null) }
    var selectedTeamSport by remember { mutableStateOf<Sport?>(null) }

    LaunchedEffect(tournaments, teams) {
        selectedTournamentForDetail?.let { selected -> selectedTournamentForDetail = tournaments.firstOrNull { it.id == selected.id } }
        selectedTeamForDetail?.let { selected -> selectedTeamForDetail = teams.firstOrNull { it.id == selected.id } }
    }
    LaunchedEffect(focusedId, tournaments, teams) {
        if (focusedId != null) {
            tournaments.firstOrNull { it.id == focusedId }?.let { selectedTournamentForDetail = it }
            teams.firstOrNull { it.id == focusedId }?.let { selectedTeamForDetail = it }
            if (tournaments.any { it.id == focusedId } || teams.any { it.id == focusedId }) onFocusConsumed()
        }
    }

    val leaderboard = remember(selectedSport, selectedScope, rankingCity, rankingDistrict, user, remotePlayers, rankingValues) {
        (rankingValues?.filter { it.sport == selectedSport && (selectedScope == "Ulusal" || it.city == rankingCity) && (selectedScope != "İlçe" || it.area == rankingDistrict) }?.map { it.row } ?: remotePlayers?.filter { selectedSport in it.sports }?.filter { selectedScope != "İlçe" || it.area == rankingDistrict }?.map { p -> LeaderboardRow(0, p.id, p.name, p.rating, 0, p.verifiedMatches, p.id == DemoData.ME) }
            ?: DemoData.leaderboard(selectedSport, selectedScope, if (selectedScope == "Ulusal") "" else if (selectedScope == "Şehir") rankingCity else "$rankingCity/$rankingDistrict")).map { row ->
            if (row.isMe) {
                val skill = user.skills.firstOrNull { it.sport == selectedSport }
                row.copy(
                    name = user.name,
                    rating = skill?.rating ?: row.rating,
                    verifiedMatches = skill?.verifiedMatches ?: row.verifiedMatches
                )
            } else row
        }.sortedByDescending { it.rating }.mapIndexed { index, row -> row.copy(rank = if (rankingBackend != null) row.rank else index + 1) }
    }
    val displayTournamentList = remember(tournaments, user.name, registeredTournamentIds, selectedTournamentSport) {
        tournaments.filter { selectedTournamentSport == null || it.sport == selectedTournamentSport }.map { tournament ->
            tournament.copy(
                participants = tournament.participants.coerceAtMost(tournament.capacity),
                standings = tournament.standings.map { row ->
                    if (row.team == DemoData.me.name) row.copy(team = user.name) else row
                }
            )
        }
    }

    val myTeams = remember(teams, backendMode) { teams.filter { it.isMine }.ifEmpty { if (backendMode) emptyList() else teams.take(1) } }
    val captainedTeams = myTeams.filter { it.captainId == DemoData.ME }
    val myTeam = captainedTeams.firstOrNull() ?: myTeams.firstOrNull()
    val opponents = remember(teams, selectedTeamSport) { teams.filter { !it.isMine && (selectedTeamSport == null || it.sport == selectedTeamSport) } }
    val managedTeam = myTeams.firstOrNull { it.id == managedTeamId && it.captainId == DemoData.ME }
    val managedRequests = teamJoinRequests.filter { it.teamId == managedTeam?.id && it.status == JoinRequestStatus.PENDING }
    fun requestChallenge(target: TeamProfile) {
        val candidates = captainedTeams.filter { it.sport == target.sport && it.id != target.id }
        when (candidates.size) {
            0 -> challengeNotice = "${target.sport.label} karşılaşması için kaptanı olduğun bir takım gerekiyor."
            1 -> onOpenChallengeSheet(candidates.first(), target)
            else -> challengeTarget = target
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            // ═══════════════════════════════════════════════════════════════════
            // 0. SABİT ÜST BAŞLIK VE PRO SLIDING TAB MENÜSÜ (ASLA KAYBOLMAZ)
            // ═══════════════════════════════════════════════════════════════════
            Column(Modifier.padding(horizontal = 20.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MFeatureHeader("Yarış", "Sıralamanı yükselt. Takımını sahaya taşı.", Icons.Rounded.EmojiEvents, accent = Color(0xFFFFD28C))
                MSegmentedControl(listOf("Sıralama", "Turnuvalar", "Takımlar"), selectedTab, { selectedTab = it })
            }

            // ═══════════════════════════════════════════════════════════════════
            // ANİMASYONLU SEKME İÇERİĞİ (AKICI YATAY GEÇİŞ / SLIDE & FADE MOTION)
            // ═══════════════════════════════════════════════════════════════════
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width / 4 } + fadeIn(animationSpec = tween(200)))
                            .togetherWith(slideOutHorizontally { width -> -width / 4 } + fadeOut(animationSpec = tween(160)))
                    } else {
                        (slideInHorizontally { width -> -width / 4 } + fadeIn(animationSpec = tween(200)))
                            .togetherWith(slideOutHorizontally { width -> width / 4 } + fadeOut(animationSpec = tween(160)))
                    }
                },
                modifier = Modifier.weight(1f),
                label = "CompetitionTabTransition"
            ) { currentTab ->
                val tabListState = rememberLazyListState()
                val scrollToTopTrigger = LocalScrollToTopTrigger.current
                LaunchedEffect(scrollToTopTrigger) {
                    if (scrollToTopTrigger > 0) {
                        tabListState.animateScrollToItem(0)
                    }
                }
                when (currentTab) {
                    0 -> {
                        // ═══════════════ TAB 0: HİPER-LOKAL RANKING ═══════════════
                        LazyColumn(
                            state = tabListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = paddingValues.calculateBottomPadding() + 28.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // King of Kadıköy Kartı
                            item {
                                leaderboard.firstOrNull()?.let { athlete -> AthleteAwardCard(athlete) { requireAccount { selectedPlayerForProfile = athlete } } }
                                    ?: MSubCard { Text("Bu filtrede henüz sıralamaya giren sporcu yok.", Modifier.padding(12.dp), color = MColors.Ink500) }
                            }

                            item {
                                MCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    cornerRadius = 20.dp,
                                    elevation = 6.dp
                                ) {
                                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        MFloatingActionRow(
                                            label = "Branş",
                                            valueText = "${selectedSport.emoji} ${selectedSport.label}",
                                            icon = Icons.Rounded.Sports,
                                            onClick = { showRankingSports = true }
                                        )

                                        MFloatingDropdownBox(
                                            label = "Sıralama Kapsamı",
                                            selectedValue = selectedScope,
                                            options = listOf("İlçe", "Şehir", "Ulusal"),
                                            optionLabel = { scope ->
                                                when (scope) {
                                                    "İlçe" -> "İlçe Sıralaması"
                                                    "Şehir" -> "İl Sıralaması"
                                                    else -> "Türkiye Geneli"
                                                }
                                            },
                                            onSelect = { selectedScope = it },
                                            icon = Icons.Rounded.Public
                                        )

                                        if (selectedScope != "Ulusal") {
                                            MFloatingActionRow(
                                                label = "Konum",
                                                valueText = if (selectedScope == "Şehir") rankingCity else "$rankingCity · $rankingDistrict",
                                                icon = Icons.Rounded.LocationOn,
                                                onClick = { showRankingLocation = true }
                                            )
                                        }
                                    }
                                }
                            }

                            // Sıralama Listesi
                            if (rankingBackend != null) item {
                                Column {
                                    Text("${rankingWire?.i("total") ?: 0} sporcu", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
                                    TextButton(onClick = { rankingRetry++ }, enabled = !rankingBusy) { Text(if (rankingBusy) "Sıralama yükleniyor…" else "Sıralamayı yenile") }
                                    rankingError?.let { Text(it, color = MColors.Rose, style = MaterialTheme.typography.bodySmall) }
                                }
                            }
                            items(leaderboard, key = { it.playerId }) { row ->
                                RankingSpotlightCard(row) { requireAccount { selectedPlayerForProfile = row } }
                            }
                            if (rankingBackend != null && rankingWire?.s("nextCursor")?.isNotBlank() == true) item {
                                TextButton(enabled = !rankingBusy, onClick = { rankingActions.launch {
                                    val cursor = rankingWire?.s("nextCursor") ?: return@launch
                                    val requestFilters = listOf(selectedSport.name, rankingScopeCode, rankingCity, rankingDistrict)
                                    rankingBusy = true; rankingError = null
                                    try {
                                        val next = rankingBackend.rankings(requestFilters[0], requestFilters[1], requestFilters[2], requestFilters[3], cursor)
                                        if (requestFilters == listOf(selectedSport.name, rankingScopeCode, rankingCity, rankingDistrict)) rankingWire = JsonObject(next + ("items" to JsonArray((rankingWire?.a("items").orEmpty() + next.a("items")).distinctBy { it.s("userId") })))
                                    } catch (cancel: CancellationException) { throw cancel }
                                    catch (error: Exception) { rankingError = error.message }
                                    finally { rankingBusy = false }
                                } }) { Text("Sıradaki sporcuları yükle") }
                            }
                            val ownRanking = rankingWire?.o("own")
                            if (rankingBackend != null && ownRanking?.isNotEmpty() == true && leaderboard.none { it.isMe }) item {
                                val own = com.matchora.app.data.BackendSnapshot(com.matchora.app.data.apiPayload("userId" to rankingBackend.userId, "rankings" to listOf(ownRanking))).rankings.single().row
                                RankingSpotlightCard(own) { requireAccount { selectedPlayerForProfile = own } }
                            }
                        }
                    }

                    1 -> {
                        // ═══════════════ TAB 1: TURNUVALAR & LİGLER ═══════════════
                        LazyColumn(
                            state = tabListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = paddingValues.calculateBottomPadding() + 28.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Turnuva / E-Spor Kupası Aç Kapsülü
                            item {
                                CreateCupCard { requireAccount { showCreateTournamentSheet = true } }
                            }
                            item {
                                SportPickerField(selectedTournamentSport, { selectedTournamentSport = it })
                            }

                            // Turnuva Listesi
                            items(displayTournamentList, key = { it.id }) { tournament ->
                                TournamentSpotlightCard(tournament) { requireAccount { selectedTournamentForDetail = tournament } }
                            }
                            item { BackendMoreCatalogButton("tournaments") }
                        }
                    }

                    2 -> {
                        // ═══════════════ TAB 2: TAKIM AĞI & MEYDAN OKU ═══════════════
                        LazyColumn(
                            state = tabListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = paddingValues.calculateBottomPadding() + 28.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item { BackendIncomingTeamInvites(teams) { requireAccount { selectedTeamForDetail = it } } }
                            // Yeni Takım Kur Kapsülü
                            item {
                                CreateClubCard { requireAccount { showCreateTeamSheet = true } }
                            }
                            if (myTeam != null) {
                                item {
                                    MButton(
                                        text = "Match Scouter & Kemer Analizi",
                                        onClick = { requireAccount { showAiMatchScouterSheet = true } },
                                        icon = Icons.Rounded.AutoAwesome,
                                        isPrimary = false
                                    )
                                }
                            }
                            item {
                                SportPickerField(selectedTeamSport, { selectedTeamSport = it })
                            }

                            // Kendi Takımım Kartı
                            items(myTeams, key = { "mine-${it.id}" }) { ownTeam ->
                                val canManage = ownTeam.captainId == DemoData.ME
                                val requestCount = teamJoinRequests.count { it.teamId == ownTeam.id && it.status == JoinRequestStatus.PENDING }
                                TeamSpotlightCard(ownTeam,
                                    if (!canManage) "Takımı incele" else if (requestCount > 0) "Başvurular ($requestCount)" else "Başvurular",
                                    onPrimary = { requireAccount { if (canManage) { managedTeamId = ownTeam.id; showManageTeamRequestsSheet = true } else selectedTeamForDetail = ownTeam } },
                                    onDetails = { requireAccount { selectedTeamForDetail = ownTeam } })
                            }

                            // ═══════════════════════════════════════════════════════════
                            // AKTİF MEYDAN OKUMALAR HUB
                            // ═══════════════════════════════════════════════════════════
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "AKTİF MEYDAN OKUMALAR",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = MColors.Ink400
                                    )
                                    Text(
                                        text = "${challenges.size} Teklif",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MColors.Brand
                                    )
                                }
                            }

                            items(challenges, key = { it.id }) { challenge ->
                                val opponentTeam = teams.firstOrNull { it.id == if (challenge.incoming) challenge.fromTeamId else challenge.toTeamId } ?: myTeam
                                if (opponentTeam != null) ChallengeSpotlightCard(challenge, opponentTeam,
                                    onDetails = { requireAccount { selectedChallengeForDetail = challenge } },
                                    onAccept = { requireAccount { onAcceptChallenge(challenge) } },
                                    onDecline = { requireAccount { onDeclineChallenge(challenge) } },
                                    onChat = { requireAccount { (remotePlayers ?: DemoData.players).firstOrNull { it.id == opponentTeam.captainId }?.let(onOpenDirectChat) } },
                                    canRespond = !backendMode || teams.firstOrNull { it.id == challenge.toTeamId }?.captainId == DemoData.ME)
                            }

                            // ═══════════════════════════════════════════════════════════
                            // RAKİP ARAYAN TAKIMLAR KATALOĞU
                            // ═══════════════════════════════════════════════════════════
                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "RAKİP VE OYUNCU ARAYAN KULÜPLER",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MColors.Ink400
                                )
                            }

                            items(opponents, key = { it.id }) { opponent ->
                                val canChallenge = captainedTeams.any { it.sport == opponent.sport }
                                TeamSpotlightCard(opponent, if (!canChallenge) "Takımı incele" else "Meydan oku",
                                    onPrimary = { requireAccount { if (canChallenge) requestChallenge(opponent) else selectedTeamForDetail = opponent } },
                                    onDetails = { requireAccount { selectedTeamForDetail = opponent } }, compact = true)
                            }
                            item { BackendMoreCatalogButton("teams") }
                        }
                    }
                }
            }
        }
    }

    if (showRankingLocation) {
        CityAndLocationSelectorSheet(rankingCity, rankingDistrict, false,
            onDismiss = { showRankingLocation = false },
            onSelectLocation = { city, district, _ -> rankingCity = city; rankingDistrict = district; showRankingLocation = false })
    }
    if (showRankingSports) {
        SportPickerSheet(selectedSport = selectedSport, allowAll = false,
            onDismiss = { showRankingSports = false },
            onSelect = { sport -> if (sport != null) selectedSport = sport; showRankingSports = false })
    }

    // Oyuncu Profil Modalı
    val playerProf = selectedPlayerForProfile
    if (playerProf != null) {
        val targetPlayer = if (playerProf.playerId == DemoData.ME) currentPlayer else (remotePlayers ?: DemoData.players).firstOrNull { it.id == playerProf.playerId } ?: Player(
            id = playerProf.playerId,
            name = playerProf.name,
            area = if (remotePlayers == null) "Kadıköy" else "",
            sports = listOf(selectedSport),
            level = if (remotePlayers == null) SkillLevel.ADVANCED else SkillLevel.INTERMEDIATE,
            reliability = if (remotePlayers == null) 95 else 0,
            verifiedMatches = playerProf.verifiedMatches,
            rating = playerProf.rating,
            chemistry = if (remotePlayers == null) 92 else null,
            verified = remotePlayers == null,
            position = "Oyuncu",
            distanceKm = if (remotePlayers == null) 1.2f else null,
            playedTogether = if (remotePlayers == null) 3 else 0,
            availableNow = remotePlayers == null,
            color = MColors.Brand,
            reliabilityVisible = remotePlayers == null
        )

        PlayerProfileSheet(
            name = playerProf.name,
            initials = playerProf.initials,
            rating = playerProf.rating,
            rank = playerProf.rank,
            player = targetPlayer,
            isInvitedToTeam = !backendMode && targetPlayer.id in invitedPlayerIds,
            sport = selectedSport,
            onDismiss = { selectedPlayerForProfile = null },
            onChallenge = {
                selectedPlayerForProfile = null
                selectedTab = 2
            },
            onInviteTeam = {
                inviteTarget = targetPlayer
            },
            onDirectMessage = {
                selectedPlayerForProfile = null
                onOpenDirectChat(targetPlayer)
            }
        )
    }
    inviteTarget?.let { player ->
        val options = captainedTeams.filter { it.sport == selectedSport && player.id !in it.memberIds }
        AlertDialog(onDismissRequest = { inviteTarget = null }, title = { Text("Davet göndereceğin takımı seç") }, text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                if (options.isEmpty()) Text("Bu branşta kaptanı olduğun ve sporcunun henüz katılmadığı bir takım gerekiyor.")
                options.forEach { team -> TextButton(onClick = { onInvitePlayerToTeam(player, team); inviteTarget = null }) { Text("${team.badge} ${team.name}") } }
            }
        }, confirmButton = { TextButton(onClick = { inviteTarget = null }) { Text("Kapat") } })
    }

    // Turnuva Detay & Fikstür Modalı
    if (selectedTournamentForDetail != null) {
        TournamentDetailSheet(
            tournament = selectedTournamentForDetail!!,
            isRegistered = selectedTournamentForDetail!!.id in registeredTournamentIds,
            isOrganizer = selectedTournamentForDetail!!.creatorId == DemoData.ME,
            onOpenFixture = { id -> selectedTournamentForDetail = null; onOpenFixture(id) },
            onDismiss = { selectedTournamentForDetail = null },
            onRegisterTeam = {
                selectedTournamentForDetail?.let { tournament ->
                    onTournamentRegistered(tournament)
                }
            },
            onWithdrawRegistration = {
                selectedTournamentForDetail?.let(onWithdrawTournamentRegistration)
            },
            onCancelTournament = {
                selectedTournamentForDetail?.let(onCancelTournament)
            },
            onShareTournament = {
                selectedTournamentForDetail?.let { tourney -> onShareTournament?.invoke(tourney) }
            }
        )
    }

    // Yeni Turnuva / E-Spor Kupası Oluşturma Modalı
    if (showCreateTournamentSheet) {
        CreateTournamentSheet(
            currentCity = user.city, currentArea = user.area, primarySport = user.skills.firstOrNull()?.sport ?: Sport.FOOTBALL,
            onDismiss = { showCreateTournamentSheet = false },
            tokenBalance = tokenBalance,
            organizerName = user.name,
            newTournamentId = newTournamentId,
            onRequestTokens = onRequestTokens,
            onCreateTournament = { newTourney ->
                onCreateTournament(newTourney)
                if (!backendMode) selectedTournamentForDetail = newTourney
            }
        )
    }

    // Yeni Takım Kur Modalı
    if (showCreateTeamSheet) {
        CreateTeamSheet(
            currentCity = user.city,
            currentArea = user.area,
            onDismiss = { showCreateTeamSheet = false },
            onCreateTeam = { newTeam ->
                onCreateTeam(newTeam)
                if (!backendMode) selectedTeamForDetail = newTeam
            }
        )
    }

    // Takım Detay Modalı
    if (selectedTeamForDetail != null) {
        val detailTeam = selectedTeamForDetail!!
        TeamDetailSheet(
            players = remotePlayers ?: DemoData.players,            team = detailTeam,
            currentPlayer = currentPlayer,
            myTeam = captainedTeams.firstOrNull { it.sport == detailTeam.sport && it.id != detailTeam.id },
            pendingRequestsCount = teamJoinRequests.count { it.teamId == detailTeam.id && it.status == JoinRequestStatus.PENDING },
            onDismiss = { selectedTeamForDetail = null },
            onOpenChallenge = {
                selectedTeamForDetail = null
                requestChallenge(detailTeam)
            },
            onApplyToTeam = {
                teamToApply = detailTeam
            },
            onManageRequests = {
                selectedTeamForDetail = null
                managedTeamId = detailTeam.id
                showManageTeamRequestsSheet = true
            },
            onOpenCaptainChat = { captain ->
                selectedTeamForDetail = null
                onOpenDirectChat(captain)
            },
            onShareTeam = { onShareTeam?.invoke(detailTeam) },
            onChallengePlayer = { player ->
                selectedTeamForDetail = null
                selectedTab = 2
            },
            onInvitePlayerToTeam = { player ->
                inviteTarget = player
            }
        )
    }

    // Kadroya Katılma Başvurusu Modalı
    if (teamToApply != null) {
        ApplyToTeamSheet(
            team = teamToApply!!,
            onDismiss = { teamToApply = null },
            onSubmitApplication = { pos, note ->
                onApplyToTeam(teamToApply!!, pos, note)
                teamToApply = null
            }
        )
    }

    // Takım Kadro Başvuruları Yönetim Modalı (Kaptan)
    if (showManageTeamRequestsSheet && managedTeam != null) {
        ManageTeamRequestsSheet(
            team = managedTeam,
            requests = managedRequests,
            onDismiss = { showManageTeamRequestsSheet = false },
            onApprove = { req ->
                onApproveTeamJoinRequest(req)
            },
            onReject = { req ->
                onRejectTeamJoinRequest(req)
            },
            onDirectMessage = { player ->
                showManageTeamRequestsSheet = false
                onOpenDirectChat(player)
            }
        )
    }

    // Mevcut Meydan Okuma Detayı / Maç Sözleşmesi Modalı
    if (selectedChallengeForDetail != null) {
        val ch = selectedChallengeForDetail!!
        val fromT = teams.firstOrNull { it.id == ch.fromTeamId } ?: myTeam
        val toT = teams.firstOrNull { it.id == ch.toTeamId } ?: teams.firstOrNull()
        if (fromT != null && toT != null) MatchChallengeDetailSheet(
            players = remotePlayers ?: DemoData.players,            challenge = ch,
            fromTeam = fromT,
            toTeam = toT,
            canRespond = !backendMode || toT.captainId == DemoData.ME,
            onAccept = {
                onAcceptChallenge(ch)
                selectedChallengeForDetail = null
            },
            onDecline = {
                onDeclineChallenge(ch)
                selectedChallengeForDetail = null
            },
            onOpenCaptainChat = { captain ->
                selectedChallengeForDetail = null
                onOpenDirectChat(captain)
            },
            onDismiss = { selectedChallengeForDetail = null }
        )
    }

    // AI Match Scouter & Şehir Şampiyonluğu Kemeri Modalı
    if (showAiMatchScouterSheet && myTeam != null) {
        AiMatchScouterSheet(
            myTeam = myTeam,
            opponents = opponents,
            onDismiss = { showAiMatchScouterSheet = false },
            onIssueBeltChallenge = { targetTeam ->
                showAiMatchScouterSheet = false
                requestChallenge(targetTeam)
            }
        )
    }
    challengeTarget?.let { target ->
        val currentTarget = teams.firstOrNull { it.id == target.id }
        AlertDialog(onDismissRequest = { challengeTarget = null }, title = { Text("Kaynak takımını seç") },
            text = { Column { captainedTeams.filter { it.sport == target.sport && it.id != target.id }.forEach { source ->
                TextButton(onClick = { challengeTarget = null; currentTarget?.let { onOpenChallengeSheet(source, it) } }) { Text("${source.badge} ${source.name} · ${source.sport.label}") }
            } } }, confirmButton = { TextButton(onClick = { challengeTarget = null }) { Text("Vazgeç") } })
    }
    challengeNotice?.let { message -> AlertDialog(onDismissRequest = { challengeNotice = null }, title = { Text("Takım kaptanlığı gerekiyor") }, text = { Text(message) },
        confirmButton = { TextButton(onClick = { challengeNotice = null; showCreateTeamSheet = true }) { Text("Takım oluştur") } },
        dismissButton = { TextButton(onClick = { challengeNotice = null }) { Text("Kapat") } }) }
}
