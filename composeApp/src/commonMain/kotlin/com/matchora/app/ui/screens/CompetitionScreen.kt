package com.matchora.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
    onInvitePlayerToTeam: (Player) -> Unit = {},
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
    var selectedPlayerForProfile by remember { mutableStateOf<LeaderboardRow?>(null) }
    var selectedTournamentForDetail by remember { mutableStateOf<TournamentInfo?>(null) }
    var showCreateTournamentSheet by remember { mutableStateOf(false) }

    // Takım ve Meydan Okuma Modalleri
    var showCreateTeamSheet by remember { mutableStateOf(false) }
    var selectedTeamForDetail by remember { mutableStateOf<TeamProfile?>(null) }
    var showManageTeamRequestsSheet by remember { mutableStateOf(false) }
    var teamToApply by remember { mutableStateOf<TeamProfile?>(null) }
    var selectedChallengeForDetail by remember { mutableStateOf<ChallengeOffer?>(null) }
    var showAiMatchScouterSheet by remember { mutableStateOf(false) }

    val leaderboard = remember(selectedSport, selectedScope, rankingCity, rankingDistrict, user) {
        DemoData.leaderboard(selectedSport, selectedScope, if (selectedScope == "Ulusal") "" else if (selectedScope == "Şehir") rankingCity else "$rankingCity/$rankingDistrict").map { row ->
            if (row.isMe) {
                val skill = user.skills.firstOrNull { it.sport == selectedSport }
                row.copy(
                    name = user.name,
                    rating = skill?.rating ?: row.rating,
                    verifiedMatches = skill?.verifiedMatches ?: row.verifiedMatches
                )
            } else row
        }.sortedByDescending { it.rating }.mapIndexed { index, row -> row.copy(rank = index + 1) }
    }
    val displayTournamentList = remember(tournaments, user.name, registeredTournamentIds) {
        tournaments.map { tournament ->
            tournament.copy(
                participants = (tournament.participants + if (tournament.id in registeredTournamentIds) 1 else 0)
                    .coerceAtMost(tournament.capacity),
                standings = tournament.standings.map { row ->
                    if (row.team == DemoData.me.name) row.copy(team = user.name) else row
                }
            )
        }
    }

    val myTeam = remember(teams) { teams.firstOrNull { it.isMine } ?: teams.first() }
    val opponents = remember(teams) { teams.filter { !it.isMine } }
    val myTeamRequests = remember(teamJoinRequests, myTeam) {
        teamJoinRequests.filter { it.teamId == myTeam.id && it.status == JoinRequestStatus.PENDING }
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
                                val athlete = leaderboard.first()
                                AthleteAwardCard(athlete) { selectedPlayerForProfile = athlete }
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
                            items(leaderboard, key = { it.playerId }) { row ->
                                RankingSpotlightCard(row) { selectedPlayerForProfile = row }
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
                                CreateCupCard { showCreateTournamentSheet = true }
                            }

                            // Turnuva Listesi
                            items(displayTournamentList, key = { it.id }) { tournament ->
                                TournamentSpotlightCard(tournament) { selectedTournamentForDetail = tournament }
                            }
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
                            // Yeni Takım Kur Kapsülü
                            item {
                                CreateClubCard { showCreateTeamSheet = true }
                            }

                            // Kendi Takımım Kartı
                            item {
                                TeamSpotlightCard(myTeam,
                                    if (myTeamRequests.isNotEmpty()) "Başvurular (${myTeamRequests.size})" else "Başvurular",
                                    onPrimary = { showManageTeamRequestsSheet = true },
                                    onDetails = { selectedTeamForDetail = myTeam })
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
                                ChallengeSpotlightCard(challenge, opponentTeam,
                                    onDetails = { selectedChallengeForDetail = challenge },
                                    onAccept = { onAcceptChallenge(challenge) },
                                    onDecline = { onDeclineChallenge(challenge) },
                                    onChat = { onOpenDirectChat(DemoData.player(opponentTeam.captainId) ?: DemoData.players.first()) })
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
                                TeamSpotlightCard(opponent, "Meydan oku",
                                    onPrimary = { onOpenChallengeSheet(myTeam, opponent) },
                                    onDetails = { selectedTeamForDetail = opponent }, compact = true)
                            }
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
        val targetPlayer = if (playerProf.playerId == DemoData.ME) currentPlayer else DemoData.players.firstOrNull { it.id == playerProf.playerId } ?: Player(
            id = playerProf.playerId,
            name = playerProf.name,
            area = "Kadıköy",
            sports = listOf(Sport.FOOTBALL),
            level = SkillLevel.ADVANCED,
            reliability = 95,
            verifiedMatches = playerProf.verifiedMatches,
            rating = playerProf.rating,
            chemistry = 92,
            verified = true,
            position = "Oyuncu",
            distanceKm = 1.2f,
            playedTogether = 3,
            availableNow = true,
            color = MColors.Brand
        )

        PlayerProfileSheet(
            name = playerProf.name,
            initials = playerProf.initials,
            rating = playerProf.rating,
            rank = playerProf.rank,
            player = targetPlayer,
            isInvitedToTeam = targetPlayer.id in invitedPlayerIds,
            sport = selectedSport,
            onDismiss = { selectedPlayerForProfile = null },
            onChallenge = {
                selectedPlayerForProfile = null
                selectedTab = 2
            },
            onInviteTeam = {
                onInvitePlayerToTeam(targetPlayer)
            },
            onDirectMessage = {
                selectedPlayerForProfile = null
                onOpenDirectChat(targetPlayer)
            }
        )
    }

    // Turnuva Detay & Fikstür Modalı
    if (selectedTournamentForDetail != null) {
        TournamentDetailSheet(
            tournament = selectedTournamentForDetail!!,
            isRegistered = selectedTournamentForDetail!!.id in registeredTournamentIds,
            isOrganizer = selectedTournamentForDetail!!.creatorId == DemoData.ME,
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
            }
        )
    }

    // Yeni Turnuva / E-Spor Kupası Oluşturma Modalı
    if (showCreateTournamentSheet) {
        CreateTournamentSheet(
            onDismiss = { showCreateTournamentSheet = false },
            tokenBalance = tokenBalance,
            organizerName = user.name,
            newTournamentId = newTournamentId,
            onRequestTokens = onRequestTokens,
            onCreateTournament = { newTourney ->
                onCreateTournament(newTourney)
                selectedTournamentForDetail = newTourney
            }
        )
    }

    // Yeni Takım Kur Modalı
    if (showCreateTeamSheet) {
        CreateTeamSheet(
            onDismiss = { showCreateTeamSheet = false },
            onCreateTeam = { newTeam ->
                onCreateTeam(newTeam)
                selectedTeamForDetail = newTeam
            }
        )
    }

    // Takım Detay Modalı
    if (selectedTeamForDetail != null) {
        val detailTeam = selectedTeamForDetail!!
        TeamDetailSheet(
            team = detailTeam,
            currentPlayer = currentPlayer,
            myTeam = myTeam,
            pendingRequestsCount = myTeamRequests.size,
            onDismiss = { selectedTeamForDetail = null },
            onOpenChallenge = {
                selectedTeamForDetail = null
                onOpenChallengeSheet(myTeam, detailTeam)
            },
            onApplyToTeam = {
                teamToApply = detailTeam
            },
            onManageRequests = {
                selectedTeamForDetail = null
                showManageTeamRequestsSheet = true
            },
            onOpenCaptainChat = { captain ->
                selectedTeamForDetail = null
                onOpenDirectChat(captain)
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
    if (showManageTeamRequestsSheet) {
        ManageTeamRequestsSheet(
            team = myTeam,
            requests = myTeamRequests,
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
        val toT = teams.firstOrNull { it.id == ch.toTeamId } ?: opponents.first()
        MatchChallengeDetailSheet(
            challenge = ch,
            fromTeam = fromT,
            toTeam = toT,
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
    if (showAiMatchScouterSheet) {
        AiMatchScouterSheet(
            myTeam = myTeam,
            opponents = opponents,
            onDismiss = { showAiMatchScouterSheet = false },
            onIssueBeltChallenge = { targetTeam ->
                showAiMatchScouterSheet = false
                onOpenChallengeSheet(myTeam, targetTeam)
            }
        )
    }
}
