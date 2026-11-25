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
    currentCity: String = "İstanbul",
    currentDistrict: String = "Kadıköy",
    venues: List<VenueInfo> = emptyList(),
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
    onCancelTournament: (TournamentInfo) -> Unit = {},
    onCreatedTeam: (String) -> Unit = {},
    onCreatedTournament: (String) -> Unit = {}
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) } // 0: Sıralama, 1: Turnuva & Lig, 2: Takım & Meydan Oku
    var selectedSport by remember { mutableStateOf(Sport.FOOTBALL) }
    var selectedScope by remember { mutableStateOf("İlçe") }
    var rankingCity by remember(currentCity) { mutableStateOf(currentCity.ifBlank { user.city }) }
    var rankingDistrict by remember(currentDistrict) { mutableStateOf(currentDistrict.ifBlank { user.area }) }
    var tournamentAllCities by remember { mutableStateOf(false) }
    var tournamentPickedCity by remember { mutableStateOf<String?>(null) }
    var showTournamentCityPicker by remember { mutableStateOf(false) }
    var teamAllCities by remember { mutableStateOf(false) }
    var teamPickedCity by remember { mutableStateOf<String?>(null) }
    var showTeamCityPicker by remember { mutableStateOf(false) }
    var showRankingLocation by remember { mutableStateOf(false) }
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
            catch (error: Exception) { rankingError = (error as? BackendFailure)?.message ?: "Sıralama yüklenemedi." }
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
    var inviteTarget by remember { mutableStateOf<Pair<Player, Sport>?>(null) }
    var challengeNotice by remember { mutableStateOf<String?>(null) }
    var playerChallengeNotice by remember { mutableStateOf<String?>(null) }
    var teamToApply by remember { mutableStateOf<TeamProfile?>(null) }
    var selectedChallengeForDetail by remember { mutableStateOf<ChallengeOffer?>(null) }
    var showAiMatchScouterSheet by remember { mutableStateOf(false) }
    var selectedTournamentSport by remember { mutableStateOf<Sport?>(null) }
    var selectedTeamSport by remember { mutableStateOf<Sport?>(null) }

    LaunchedEffect(tournaments, teams) {
        selectedTournamentForDetail?.let { selected -> selectedTournamentForDetail = tournaments.firstOrNull { it.id == selected.id } }
        selectedTeamForDetail?.let { selected -> selectedTeamForDetail = teams.firstOrNull { it.id == selected.id } }
    }
    LaunchedEffect(focusedId, tournaments, teams, challenges) {
        if (focusedId != null) {
            val matchedTournament = tournaments.firstOrNull { it.id == focusedId }
            val matchedTeam = teams.firstOrNull { it.id == focusedId }
            val matchedChallenge = challenges.firstOrNull { it.id == focusedId }
            when {
                matchedTournament != null -> {
                    selectedTab = 1
                    selectedTournamentForDetail = matchedTournament
                    onFocusConsumed()
                }
                matchedTeam != null -> {
                    selectedTab = 2
                    selectedTeamForDetail = matchedTeam
                    onFocusConsumed()
                }
                matchedChallenge != null -> {
                    selectedTab = 2
                    selectedChallengeForDetail = matchedChallenge
                    onFocusConsumed()
                }
            }
        }
    }

    val leaderboard = remember(selectedSport, selectedScope, rankingCity, rankingDistrict, user, remotePlayers, rankingValues) {
        (rankingValues?.filter { it.sport == selectedSport && (selectedScope == "Ulusal" || com.matchora.app.state.matchesTurkishPlace(it.city, rankingCity)) && (selectedScope != "İlçe" || com.matchora.app.state.matchesTurkishPlace(it.area, rankingDistrict)) }?.map { it.row } ?: remotePlayers?.filter { selectedSport in it.sports }?.filter { (selectedScope == "Ulusal" || com.matchora.app.state.matchesTurkishPlace(it.city, rankingCity)) && (selectedScope != "İlçe" || com.matchora.app.state.matchesTurkishPlace(it.area, rankingDistrict)) }?.map { p -> LeaderboardRow(0, p.id, p.name, p.rating, 0, p.verifiedMatches, p.id == DemoData.ME) }
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
    val activeTournamentCity = when {
        tournamentAllCities -> null
        tournamentPickedCity != null -> tournamentPickedCity
        else -> currentCity
    }
    val displayTournamentList = remember(tournaments, user.name, registeredTournamentIds, selectedTournamentSport, tournamentAllCities, tournamentPickedCity, currentCity) {
        tournaments.filter {
            (selectedTournamentSport == null || it.sport == selectedTournamentSport) &&
                (activeTournamentCity == null || matchesTurkishPlace(it.city, activeTournamentCity) || (tournamentPickedCity == null && (it.sport.isESport || it.id in registeredTournamentIds || it.creatorId == DemoData.ME)))
        }.map { tournament ->
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
    val activeTeamCity = when {
        teamAllCities -> null
        teamPickedCity != null -> teamPickedCity
        else -> currentCity
    }
    val opponents = remember(teams, selectedTeamSport, teamAllCities, teamPickedCity, currentCity) {
        teams.filter {
            !it.isMine &&
                (selectedTeamSport == null || it.sport == selectedTeamSport) &&
                (activeTeamCity == null || matchesTurkishPlace(it.city, activeTeamCity) || (teamPickedCity == null && it.sport.isESport))
        }
    }
    val managedTeam = myTeams.firstOrNull { it.id == managedTeamId && it.captainId == DemoData.ME }
    val managedRequests = teamJoinRequests.filter { it.teamId == managedTeam?.id && it.status == JoinRequestStatus.PENDING }
    fun findPlayerTargetTeam(playerId: String, preferredSport: Sport): TeamProfile? {
        return teams.firstOrNull { t ->
            !t.isMine && t.sport == preferredSport && (t.captainId == playerId || t.coCaptainId == playerId || playerId in t.memberIds || playerId in t.substituteIds)
        } ?: teams.firstOrNull { t ->
            !t.isMine && (t.captainId == playerId || t.coCaptainId == playerId || playerId in t.memberIds || playerId in t.substituteIds)
        }
    }
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
            // 0. KOMPAKT BAŞLIK + AKSİYON VE PRO SLIDING TAB MENÜSÜ
            // ═══════════════════════════════════════════════════════════════════
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MCompactHeroStrip(
                    title = "Yarış",
                    subtitle = when (selectedTab) {
                        0 -> "Yerel ve ulusal sıralamalar"
                        1 -> "Kupalar ve fikstür takibi"
                        else -> "Kulüpler ve meydan okumalar"
                    },
                    icon = Icons.Rounded.EmojiEvents,
                    accent = Color(0xFFFFD28C),
                    trailingAction = {
                        when (selectedTab) {
                            0 -> if (rankingBackend != null) {
                                MHeroActionButton(
                                    text = if (rankingBusy) "Yenileniyor" else "Yenile",
                                    icon = Icons.Rounded.Refresh,
                                    enabled = !rankingBusy,
                                    isUtility = true,
                                    onClick = { rankingRetry++ }
                                )
                            }
                            1 -> MHeroActionButton(
                                text = "Kupa aç",
                                icon = Icons.Rounded.Add,
                                onClick = { requireAccount { showCreateTournamentSheet = true } }
                            )
                            else -> MHeroActionButton(
                                text = "Kulüp kur",
                                icon = Icons.Rounded.Add,
                                onClick = { requireAccount { showCreateTeamSheet = true } }
                            )
                        }
                    }
                )
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
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = paddingValues.calculateBottomPadding() + 28.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            MCompactSportPill(
                                                selectedSport = selectedSport,
                                                onSelect = { if (it != null) selectedSport = it },
                                                allowAll = false,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (selectedScope != "Ulusal") {
                                                Surface(
                                                    onClick = { showRankingLocation = true },
                                                    shape = RoundedCornerShape(11.dp),
                                                    color = Color(0xFFF8FAFC),
                                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                                    modifier = Modifier.weight(1f).height(38.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Surface(
                                                            shape = RoundedCornerShape(7.dp),
                                                            color = MColors.BrandTint
                                                        ) {
                                                            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                                                                Icon(
                                                                    imageVector = Icons.Rounded.LocationOn,
                                                                    contentDescription = null,
                                                                    tint = MColors.BrandDeep,
                                                                    modifier = Modifier.size(14.dp)
                                                                )
                                                            }
                                                        }
                                                        Text(
                                                            text = if (selectedScope == "Şehir") rankingCity else "$rankingCity · $rankingDistrict",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MColors.Ink900,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Icon(
                                                            imageVector = Icons.Rounded.KeyboardArrowDown,
                                                            contentDescription = null,
                                                            tint = MColors.Ink400,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        MSegmentPillBar(
                                            options = listOf(
                                                MSegmentPillOption("İlçe", "İlçe", Icons.Rounded.NearMe),
                                                MSegmentPillOption("Şehir", "İl geneli", Icons.Rounded.LocationCity),
                                                MSegmentPillOption("Ulusal", "Türkiye", Icons.Rounded.Public)
                                            ),
                                            selectedValue = selectedScope,
                                            onSelect = { selectedScope = it }
                                        )
                                    }
                                }
                            }

                            if (rankingBackend != null) {
                                rankingError?.let { err ->
                                    item { Text(err, color = MColors.Rose, style = MaterialTheme.typography.bodySmall) }
                                }
                            }

                            item {
                                when {
                                    rankingBackend != null && rankingBusy && rankingWire == null -> MSubCard {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MColors.Brand)
                                            Text("Sıralama yükleniyor…", style = MaterialTheme.typography.bodyMedium, color = MColors.Ink600)
                                        }
                                    }
                                    rankingBackend != null && rankingError != null && rankingWire == null -> MSubCard {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = rankingError ?: "Sıralama yüklenemedi.",
                                                modifier = Modifier.weight(1f),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MColors.Rose
                                            )
                                            MPillActionButton(
                                                text = "Yenile",
                                                tone = MPillTone.BRAND,
                                                compact = true,
                                                onClick = { rankingRetry += 1 }
                                            )
                                        }
                                    }
                                    else -> leaderboard.firstOrNull()?.let { athlete -> AthleteAwardCard(athlete) { selectedPlayerForProfile = athlete } }
                                        ?: MSubCard {
                                            Text(
                                                text = "Bu filtrede henüz sıralamaya giren sporcu yok.",
                                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MColors.Ink600
                                            )
                                        }
                                }
                            }
                            items(leaderboard, key = { it.playerId }) { row ->
                                RankingSpotlightCard(row) { selectedPlayerForProfile = row }
                            }
                            if (rankingBackend != null && rankingWire?.s("nextCursor")?.isNotBlank() == true) item {
                                MPillActionButton(
                                    text = if (rankingBusy) "Yükleniyor…" else "Daha fazla sporcu göster",
                                    icon = Icons.Rounded.ExpandMore,
                                    tone = MPillTone.NEUTRAL,
                                    enabled = !rankingBusy,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        rankingActions.launch {
                                            val cursor = rankingWire?.s("nextCursor") ?: return@launch
                                            val requestFilters = listOf(selectedSport.name, rankingScopeCode, rankingCity, rankingDistrict)
                                            rankingBusy = true; rankingError = null
                                            try {
                                                val next = rankingBackend.rankings(requestFilters[0], requestFilters[1], requestFilters[2], requestFilters[3], cursor)
                                                if (requestFilters == listOf(selectedSport.name, rankingScopeCode, rankingCity, rankingDistrict)) rankingWire = JsonObject(next + ("items" to JsonArray((rankingWire?.a("items").orEmpty() + next.a("items")).distinctBy { it.s("userId") })))
                                            } catch (cancel: CancellationException) { throw cancel }
                                            catch (error: Exception) { rankingError = error.message }
                                            finally { rankingBusy = false }
                                        }
                                    }
                                )
                            }
                            val ownRanking = rankingWire?.o("own")
                            if (rankingBackend != null && ownRanking?.isNotEmpty() == true && leaderboard.none { it.isMe }) item {
                                val own = com.matchora.app.data.BackendSnapshot(com.matchora.app.data.apiPayload("userId" to rankingBackend.userId, "rankings" to listOf(ownRanking))).rankings.single().row
                                RankingSpotlightCard(own) { selectedPlayerForProfile = own }
                            }
                        }
                    }

                    1 -> {
                        // ═══════════════ TAB 1: TURNUVALAR & LİGLER ═══════════════
                        LazyColumn(
                            state = tabListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = paddingValues.calculateBottomPadding() + 28.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        MCompactSportPill(
                                            selectedSport = selectedTournamentSport,
                                            onSelect = { selectedTournamentSport = it },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        MCityFilterBar(
                                            currentCity = currentCity,
                                            selectedCity = tournamentPickedCity,
                                            useAllCities = tournamentAllCities,
                                            onSelectCurrentCity = { tournamentAllCities = false; tournamentPickedCity = null },
                                            onSelectAllCities = { tournamentAllCities = true; tournamentPickedCity = null },
                                            onOpenCityPicker = { showTournamentCityPicker = true }
                                        )
                                    }
                                }
                            }

                            if (displayTournamentList.isEmpty()) {
                                item {
                                    MSubCard {
                                        Text(
                                            text = if (activeTournamentCity != null) "$activeTournamentCity ilinde açık turnuva bulunamadı." else "Henüz açık turnuva bulunamadı.",
                                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MColors.Ink600
                                        )
                                    }
                                }
                            }

                            // Turnuva Listesi
                            items(displayTournamentList, key = { it.id }) { tournament ->
                                TournamentSpotlightCard(
                                    tournament = tournament,
                                    onShare = onShareTournament?.let { share -> { share(tournament) } }
                                ) { selectedTournamentForDetail = tournament }
                            }
                            item { BackendMoreCatalogButton("tournaments") }
                        }
                    }

                    2 -> {
                        // ═══════════════ TAB 2: TAKIM AĞI & MEYDAN OKU ═══════════════
                        LazyColumn(
                            state = tabListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = paddingValues.calculateBottomPadding() + 28.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item { BackendIncomingTeamInvites(teams) { requireAccount { selectedTeamForDetail = it } } }

                            // Kendi Takımım Kartları
                            items(myTeams, key = { "mine-${it.id}" }) { ownTeam ->
                                val canManage = ownTeam.captainId == DemoData.ME
                                val requestCount = teamJoinRequests.count { it.teamId == ownTeam.id && it.status == JoinRequestStatus.PENDING }
                                TeamSpotlightCard(ownTeam,
                                    if (!canManage) "Takımı incele" else if (requestCount > 0) "Başvurular ($requestCount)" else "Başvurular",
                                    onPrimary = { requireAccount { if (canManage) { managedTeamId = ownTeam.id; showManageTeamRequestsSheet = true } else selectedTeamForDetail = ownTeam } },
                                    onDetails = { requireAccount { selectedTeamForDetail = ownTeam } })
                            }

                            if (challenges.isNotEmpty()) {
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
                                        onChat = {
                                            requireAccount {
                                                val captain = (remotePlayers ?: DemoData.players).firstOrNull { it.id == opponentTeam.captainId }
                                                    ?: currentPlayer.copy(
                                                        id = opponentTeam.captainId,
                                                        name = "${opponentTeam.name} Kaptanı",
                                                        verified = false,
                                                        rating = opponentTeam.rating,
                                                        verifiedMatches = 0,
                                                        reliability = 0,
                                                        position = null,
                                                        reliabilityVisible = false,
                                                        profileAvailable = false
                                                    )
                                                onOpenDirectChat(captain)
                                            }
                                        },
                                        canRespond = !backendMode || teams.firstOrNull { it.id == challenge.toTeamId }?.captainId == DemoData.ME)
                                }
                            }

                            // Rakip Kulüpler Filtre Kartı
                            item {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            MCompactSportPill(
                                                selectedSport = selectedTeamSport,
                                                onSelect = { selectedTeamSport = it },
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (myTeam != null) {
                                                MPillActionButton(
                                                    text = "Scouter",
                                                    icon = Icons.Rounded.AutoAwesome,
                                                    tone = MPillTone.BRAND,
                                                    compact = true,
                                                    onClick = { requireAccount { showAiMatchScouterSheet = true } }
                                                )
                                            }
                                        }
                                        MCityFilterBar(
                                            currentCity = currentCity,
                                            selectedCity = teamPickedCity,
                                            useAllCities = teamAllCities,
                                            onSelectCurrentCity = {
                                                teamAllCities = false
                                                teamPickedCity = null
                                            },
                                            onSelectAllCities = {
                                                teamAllCities = true
                                                teamPickedCity = null
                                            },
                                            onOpenCityPicker = { showTeamCityPicker = true }
                                        )
                                    }
                                }
                            }

                            if (opponents.isEmpty()) {
                                item {
                                    val emptyCityLabel = activeTeamCity ?: "Seçili kapsam"
                                    MSubCard {
                                        Text(
                                            text = if (teamAllCities) "Eşleşen rakip takım yok." else "$emptyCityLabel için eşleşen rakip takım yok.",
                                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MColors.Ink600
                                        )
                                    }
                                }
                            }

                            items(opponents, key = { it.id }) { opponent ->
                                val canChallenge = captainedTeams.any { it.sport == opponent.sport }
                                TeamSpotlightCard(opponent, if (!canChallenge) "Takımı incele" else "Meydan oku",
                                    onPrimary = { if (canChallenge) requireAccount { requestChallenge(opponent) } else selectedTeamForDetail = opponent },
                                    onDetails = { selectedTeamForDetail = opponent }, compact = true)
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

    // Oyuncu Profil Modalı
    val playerProf = selectedPlayerForProfile
    if (playerProf != null) {
        val targetPlayer = if (playerProf.playerId == DemoData.ME) currentPlayer else (remotePlayers ?: DemoData.players).firstOrNull { it.id == playerProf.playerId } ?: Player(
            id = playerProf.playerId,
            name = playerProf.name,
            area = if (remotePlayers == null) currentDistrict else "",
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
            isInvitedToTeam = targetPlayer.id in invitedPlayerIds,
            sport = selectedSport,
            onDismiss = { selectedPlayerForProfile = null },
            onChallenge = {
                requireAccount {
                    val targetTeam = findPlayerTargetTeam(targetPlayer.id, selectedSport)
                    selectedPlayerForProfile = null
                    if (targetTeam != null) {
                        requestChallenge(targetTeam)
                    } else {
                        selectedTab = 2
                        playerChallengeNotice = "${targetPlayer.name} henüz bir kulüpte değil. Takımlar sekmesinden diğer rakiplere meydan okuyabilirsin."
                    }
                }
            },
            onInviteTeam = {
                requireAccount { inviteTarget = targetPlayer to selectedSport }
            },
            onDirectMessage = {
                requireAccount {
                    selectedPlayerForProfile = null
                    onOpenDirectChat(targetPlayer)
                }
            }
        )
    }
    inviteTarget?.let { (player, inviteSport) ->
        val options = captainedTeams.filter { it.sport == inviteSport && player.id !in it.memberIds }
        AlertDialog(
            onDismissRequest = { inviteTarget = null },
            title = { Text("Davet göndereceğin takımı seç") },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (options.isEmpty()) {
                        Text("Bu branşta kaptanı olduğun ve sporcunun henüz katılmadığı bir takım gerekiyor.")
                    }
                    options.forEach { team ->
                        MPremiumMenuRow(
                            text = "${team.badge} ${team.name}",
                            subtitle = "${team.city} · ${team.memberIds.size} üye",
                            icon = Icons.Rounded.Shield,
                            onClick = { onInvitePlayerToTeam(player, team); inviteTarget = null }
                        )
                    }
                }
            },
            confirmButton = {
                MPillActionButton(text = "Kapat", tone = MPillTone.NEUTRAL, compact = true, onClick = { inviteTarget = null })
            }
        )
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
                requireAccount {
                    selectedTournamentForDetail?.let { tournament ->
                        onTournamentRegistered(tournament)
                    }
                }
            },
            onWithdrawRegistration = {
                requireAccount {
                    selectedTournamentForDetail?.let(onWithdrawTournamentRegistration)
                }
            },
            onCancelTournament = {
                requireAccount {
                    selectedTournamentForDetail?.let(onCancelTournament)
                }
            },
            onShareTournament = {
                selectedTournamentForDetail?.let { tourney -> onShareTournament?.invoke(tourney) }
            }
        )
    }

    // Yeni Turnuva / E-Spor Kupası Oluşturma Modalı
    if (showCreateTournamentSheet) {
        CreateTournamentSheet(
            currentCity = currentCity.ifBlank { user.city }, currentArea = currentDistrict.ifBlank { user.area }, venues = venues, primarySport = user.skills.firstOrNull()?.sport ?: Sport.FOOTBALL,
            onDismiss = { showCreateTournamentSheet = false },
            tokenBalance = tokenBalance,
            organizerName = user.name,
            newTournamentId = newTournamentId,
            onRequestTokens = onRequestTokens,
            onPublished = onCreatedTournament,
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
            onPublished = onCreatedTeam,
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
            players = remotePlayers ?: DemoData.players,
            team = detailTeam,
            currentPlayer = currentPlayer,
            myTeam = captainedTeams.firstOrNull { it.sport == detailTeam.sport && it.id != detailTeam.id },
            pendingRequestsCount = teamJoinRequests.count { it.teamId == detailTeam.id && it.status == JoinRequestStatus.PENDING },
            hasPendingJoinRequest = teamJoinRequests.any { it.teamId == detailTeam.id && it.player.id == DemoData.ME && it.status == JoinRequestStatus.PENDING },
            onDismiss = { selectedTeamForDetail = null },
            onOpenChallenge = {
                requireAccount {
                    selectedTeamForDetail = null
                    requestChallenge(detailTeam)
                }
            },
            onApplyToTeam = {
                requireAccount { teamToApply = detailTeam }
            },
            onManageRequests = {
                requireAccount {
                    selectedTeamForDetail = null
                    managedTeamId = detailTeam.id
                    showManageTeamRequestsSheet = true
                }
            },
            onOpenCaptainChat = { captain ->
                requireAccount {
                    selectedTeamForDetail = null
                    onOpenDirectChat(captain)
                }
            },
            onShareTeam = { onShareTeam?.invoke(detailTeam) },
            onChallengePlayer = { player ->
                requireAccount {
                    val targetTeam = if (!detailTeam.isMine) detailTeam else findPlayerTargetTeam(player.id, detailTeam.sport)
                    selectedTeamForDetail = null
                    if (targetTeam != null) {
                        requestChallenge(targetTeam)
                    } else {
                        selectedTab = 2
                        playerChallengeNotice = "${player.name} için meydan okunabilecek uygun bir rakip kulüp bulunamadı."
                    }
                }
            },
            onInvitePlayerToTeam = { player ->
                requireAccount { inviteTarget = player to detailTeam.sport }
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
            opponents = opponents.filter { it.sport == myTeam.sport },
            onDismiss = { showAiMatchScouterSheet = false },
            onIssueBeltChallenge = { targetTeam ->
                showAiMatchScouterSheet = false
                requestChallenge(targetTeam)
            }
        )
    }
    challengeTarget?.let { target ->
        val currentTarget = teams.firstOrNull { it.id == target.id }
        AlertDialog(
            shape = RoundedCornerShape(22.dp),
            containerColor = Color.White,
            onDismissRequest = { challengeTarget = null },
            title = { Text("Kaynak takımını seç", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    captainedTeams.filter { it.sport == target.sport && it.id != target.id }.forEach { source ->
                        MPremiumMenuRow(
                            text = "${source.badge} ${source.name}",
                            subtitle = source.sport.label,
                            icon = Icons.Rounded.Shield,
                            onClick = { challengeTarget = null; currentTarget?.let { onOpenChallengeSheet(source, it) } }
                        )
                    }
                }
            },
            confirmButton = {
                MPillActionButton("Vazgeç", tone = MPillTone.NEUTRAL, compact = true, onClick = { challengeTarget = null })
            }
        )
    }
    if (showTournamentCityPicker) {
        CityFilterPickerSheet(
            selectedCity = tournamentPickedCity,
            currentCity = currentCity,
            title = "Turnuva ili seç",
            onSelectCity = { city -> tournamentAllCities = false; tournamentPickedCity = city },
            onDismiss = { showTournamentCityPicker = false }
        )
    }
    if (showTeamCityPicker) {
        CityFilterPickerSheet(
            selectedCity = teamPickedCity,
            currentCity = currentCity,
            title = "Takım ili seç",
            onSelectCity = { city -> teamAllCities = false; teamPickedCity = city },
            onDismiss = { showTeamCityPicker = false }
        )
    }
    challengeNotice?.let { message -> AlertDialog(onDismissRequest = { challengeNotice = null }, title = { Text("Takım kaptanlığı gerekiyor") }, text = { Text(message) },
        confirmButton = { MPillActionButton(text = "Takım oluştur", icon = Icons.Rounded.Add, tone = MPillTone.BRAND, compact = true, onClick = { challengeNotice = null; showCreateTeamSheet = true }) },
        dismissButton = { MPillActionButton(text = "Kapat", tone = MPillTone.NEUTRAL, compact = true, onClick = { challengeNotice = null }) }) }
    playerChallengeNotice?.let { message -> AlertDialog(onDismissRequest = { playerChallengeNotice = null }, title = { Text("Rakip kulüp bulunamadı") }, text = { Text(message) },
        confirmButton = { MPillActionButton(text = "Tamam", tone = MPillTone.NEUTRAL, compact = true, onClick = { playerChallengeNotice = null }) }) }
}
