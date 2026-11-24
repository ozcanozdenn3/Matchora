package com.matchora.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.state.*
import com.matchora.app.data.*
import com.matchora.app.ui.components.*
import com.matchora.app.ui.theme.MColors

// ═══════════════════════════════════════════════════════════════════════════════
// UNIFIED COMMUNITY & VENUE SCREEN (Strateji Dokümanı §14, §15)
// Birleşik modüler kart yapısı, her öğenin arkasında şık bir zemin var!
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityAndVenueScreen(
    paddingValues: PaddingValues,
    onBookSlot: (venue: VenueInfo, slot: VenueSlotInfo, splitCount: Int, amount: Int, method: String) -> Unit,
    onJoinCommunity: (CommunityInfo) -> Unit,
    currentDistrict: String = "Kadıköy",
    joinedCommunityIds: Set<String> = emptySet(),
    requestedCommunityIds: Set<String> = emptySet(),
    bookedSlotIds: Set<String> = emptySet(),
    communities: List<CommunityInfo> = DemoData.communities,
    venues: List<VenueInfo> = DemoData.venues,
    currentCity: String = "İstanbul",
    initialTab: Int = 0,
    focusedId: String? = null,
    onCreateCommunity: (CommunityInfo) -> Unit = {},
    onOpenCommunityChat: (CommunityInfo) -> Unit = {},
    onReportReview: (String, String, String) -> Unit = { _, _, _ -> },
    onShareVenue: ((VenueInfo) -> Unit)? = null,
    onShareCommunity: ((CommunityInfo) -> Unit)? = null,
    onDirections: ((VenueInfo) -> Unit)? = null,
    deviceLocation: DeviceLocation? = null,
    onPublishedCommunity: (String) -> Unit = {}
) {
    var detailVenue by remember { mutableStateOf<VenueInfo?>(null) }
    var reviewVenue by remember { mutableStateOf<VenueInfo?>(null) }
    var manageVenue by remember { mutableStateOf(false) }
    var createCommunity by remember { mutableStateOf(false) }
    var venueApplication by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Topluluk & Kampüs, 1: Tesisler & Saha Bul
    var selectedSportFilter by remember { mutableStateOf<com.matchora.app.domain.Sport?>(null) }
    var maxVenueRadiusKm by remember { mutableStateOf<Int?>(25) }
    var allCommunityCities by remember(currentCity) { mutableStateOf(false) }
    var pickedCommunityCity by remember(currentCity) { mutableStateOf<String?>(null) }
    var showCommunityCityPicker by remember { mutableStateOf(false) }
    var pendingWithdrawApp by remember { mutableStateOf<kotlinx.serialization.json.JsonObject?>(null) }
    val withdrawCommand = rememberBackendAction()
    val venueOrigin = deviceLocation ?: rememberRegionCenter(currentCity, currentDistrict)
    var detailCommunity by remember { mutableStateOf<CommunityInfo?>(null) }
    var leaveCommunityConfirm by remember { mutableStateOf<CommunityInfo?>(null) }
    var consumedFocusId by remember { mutableStateOf<String?>(null) }
    val feedback = LocalToastNotifier.current
    pendingWithdrawApp?.let { targetApp ->
        AlertDialog(
            onDismissRequest = { pendingWithdrawApp = null },
            title = { Text("Tesis başvurusu geri çekilsin mi?") },
            text = { Text("Bekleyen tesis başvurunu iptal etmek istediğine emin misin?") },
            confirmButton = {
                TextButton(onClick = {
                    withdrawCommand("venue.application.withdraw", apiPayload("applicationId" to targetApp.s("id"), "expectedVersion" to targetApp.i("version", 1)))
                    pendingWithdrawApp = null
                }) { Text("Geri çek", color = MColors.Rose) }
            },
            dismissButton = { TextButton(onClick = { pendingWithdrawApp = null }) { Text("Vazgeç") } }
        )
    }
    LaunchedEffect(initialTab) { selectedTab = initialTab.coerceIn(0, 1) }
    LaunchedEffect(focusedId, communities, venues) {
        if (focusedId == null) {
            consumedFocusId = null
        } else if (focusedId != consumedFocusId) {
            val matchedCommunity = communities.firstOrNull { it.id == focusedId }
            val matchedVenue = venues.firstOrNull { it.id == focusedId }
            when {
                matchedCommunity != null -> {
                    selectedTab = 0
                    detailCommunity = matchedCommunity
                    consumedFocusId = focusedId
                }
                matchedVenue != null -> {
                    selectedTab = 1
                    detailVenue = matchedVenue
                    consumedFocusId = focusedId
                }
            }
        }
    }
    var travelModeActive by remember { mutableStateOf(false) }
    var travelCity by remember { mutableStateOf("") }
    var showTravelLocation by remember { mutableStateOf(false) }
    val backend = LocalBackendRepository.current
    val requireAccount = LocalRequireAccount.current
    val wire = if (backend != null) backend.snapshots.collectAsState().value else null
    val command = rememberBackendAction()
    LaunchedEffect(wire) { wire?.o("discovery")?.let { travelModeActive = it.b("travelMode"); travelCity = it.s("city") } }
    if (showTravelLocation) CityAndLocationSelectorSheet(travelCity.ifBlank { currentCity }, currentDistrict, false,
        onDismiss = { showTravelLocation = false }, onSelectLocation = { city, district, _ ->
            if (backend?.enabled == true) command("discovery.save", apiPayload("city" to city, "district" to district, "travelMode" to true))
            else { travelModeActive = true; travelCity = city; feedback?.invoke("$city keşfi açıldı", "✈️") }
        })
    if (createCommunity) CreateCommunitySheet(currentCity, currentDistrict, { createCommunity = false }, onCreateCommunity, onPublishedCommunity)
    reviewVenue?.let { VenueReviewsSheet(it, { reviewVenue = null }, onReportReview) }
    if (manageVenue) VenueOperatorSheet { manageVenue = false }
    if (venueApplication) CreateVenueSheet(application = true, onDismiss = { venueApplication = false })
    var selectedBookingSlot by remember { mutableStateOf<Pair<VenueInfo, VenueSlotInfo>?>(null) }
    val listState = rememberLazyListState()
    val scrollToTopTrigger = LocalScrollToTopTrigger.current
    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            listState.animateScrollToItem(0)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = paddingValues.calculateBottomPadding() + 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ═══════════════════════════════════════════════════════════════════
            // 0. KOMPAKT BAŞLIK + AKSİYON & SEKME KONTROLÜ
            // ═══════════════════════════════════════════════════════════════════
            item {
                val isOperator = wire?.s("role") in listOf("admin", "venue_owner")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MCompactHeroStrip(
                        title = "Spor ağı",
                        subtitle = if (selectedTab == 0) "Topluluklar ve misafir spor" else "Kort ve saha rezervasyonu",
                        icon = Icons.Rounded.Hub,
                        accent = Color(0xFF85DFCC),
                        trailingAction = {
                            if (selectedTab == 0) {
                                MHeroActionButton(
                                    text = "Topluluk aç",
                                    icon = Icons.Rounded.Add,
                                    onClick = { requireAccount { createCommunity = true } }
                                )
                            } else {
                                MHeroActionButton(
                                    text = if (isOperator) "Tesis yönet" else "Tesis ekle",
                                    icon = if (isOperator) Icons.Rounded.Storefront else Icons.Rounded.AddBusiness,
                                    onClick = { requireAccount { if (isOperator) manageVenue = true else venueApplication = true } }
                                )
                            }
                        }
                    )
                    MSegmentedControl(listOf("Topluluklar", "Tesisler"), selectedTab, { selectedTab = it })
                }
            }

        when (selectedTab) {
            0 -> {
                // ═══════════════ TAB 0: TEK DERLİ TOPLU FİLTRE KARTI ═══════════════
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MCompactSportPill(
                                    selectedSport = selectedSportFilter,
                                    onSelect = { selectedSportFilter = it },
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    onClick = { requireAccount { showTravelLocation = true } },
                                    shape = RoundedCornerShape(11.dp),
                                    color = if (travelModeActive) MColors.Ink900 else Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, if (travelModeActive) MColors.Ink900 else Color(0xFFE2E8F0)),
                                    shadowElevation = if (travelModeActive) 1.dp else 0.dp,
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.FlightTakeoff,
                                            contentDescription = "Seyahat modu",
                                            tint = if (travelModeActive) Color(0xFFFFB37B) else MColors.BrandDeep,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = if (travelModeActive) travelCity.ifBlank { "Seyahat açık" } else "Seyahat modu",
                                            fontSize = 11.5.sp,
                                            fontWeight = if (travelModeActive) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (travelModeActive) Color.White else MColors.Ink700,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (travelModeActive) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "Seyahat modunu kapat",
                                                tint = Color.White.copy(alpha = 0.85f),
                                                modifier = Modifier
                                                    .size(15.dp)
                                                    .clickable {
                                                        requireAccount {
                                                            if (backend?.enabled == true) {
                                                                command(
                                                                    "discovery.save",
                                                                    apiPayload(
                                                                        "city" to wire?.o("profile")?.s("city", "İstanbul"),
                                                                        "district" to wire?.o("profile")?.s("area", currentDistrict),
                                                                        "travelMode" to false
                                                                    )
                                                                )
                                                            } else {
                                                                travelModeActive = false
                                                                feedback?.invoke("Seyahat modu kapatıldı", "📍")
                                                            }
                                                        }
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                            MCityFilterBar(
                                currentCity = currentCity,
                                selectedCity = pickedCommunityCity,
                                useAllCities = allCommunityCities,
                                onSelectCurrentCity = { allCommunityCities = false; pickedCommunityCity = null },
                                onSelectAllCities = { allCommunityCities = true; pickedCommunityCity = null },
                                onOpenCityPicker = { showCommunityCityPicker = true }
                            )
                        }
                    }
                }
                val activeCommunityCity = when {
                    allCommunityCities -> null
                    pickedCommunityCity != null -> pickedCommunityCity
                    travelModeActive && travelCity.isNotBlank() -> travelCity
                    else -> currentCity
                }
                val filteredCommunities = communities.filter { community ->
                    val isFocused = focusedId != null && community.id == focusedId
                    val isJoined = community.id in joinedCommunityIds
                    val matchesSport = selectedSportFilter == null || selectedSportFilter in community.sports
                    val matchesRegion = activeCommunityCity == null || matchesTurkishPlace(community.city, activeCommunityCity) || (pickedCommunityCity == null && isJoined)
                    isFocused || (matchesSport && matchesRegion)
                }.sortedBy { it.id != focusedId }
                if (filteredCommunities.isEmpty()) {
                    item {
                        MSubCard {
                            Text(
                                text = if (activeCommunityCity != null) "$activeCommunityCity içinde uygun topluluk bulunamadı." else "Bu filtrede uygun topluluk bulunamadı.",
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MColors.Ink600
                            )
                        }
                    }
                }

                // Modül B: Topluluklar Listesi
                items(filteredCommunities, key = { it.id }) { community ->
                    val isJoined = community.id in joinedCommunityIds
                    val isOwner = community.ownerId == DemoData.ME
                    CommunitySpotlightCard(
                        community = community,
                        joined = isJoined,
                        requested = community.id in requestedCommunityIds,
                        onShare = { onShareCommunity?.invoke(community) },
                        onJoin = {
                            if (isOwner) detailCommunity = community
                            else if (isJoined) leaveCommunityConfirm = community
                            else onJoinCommunity(community)
                        },
                        onCardClick = { detailCommunity = community }
                    )
                }
                item { BackendMoreCatalogButton("communities") }
            }

            1 -> {
                // ═══════════════ TAB 1: TEK DERLİ TOPLU TESİS FİLTRE KARTI ═══════════════
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MCompactSportPill(
                                selectedSport = selectedSportFilter,
                                onSelect = { selectedSportFilter = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                            MRadiusFilterBar(
                                currentCity = currentCity,
                                selectedRadiusKm = maxVenueRadiusKm,
                                onSelectRadiusKm = { maxVenueRadiusKm = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                wire?.a("venueApplications").orEmpty().forEach { application -> item(key = "application-${application.s("id")}") {
                    val appStatus = application.s("status")
                    val (statusLabel, statusTint, statusColor) = when (appStatus) {
                        "approved" -> Triple("Onaylandı", Color(0xFFDCFCE7), Color(0xFF15803D))
                        "rejected" -> Triple("Reddedildi", Color(0xFFFEE2E2), MColors.Rose)
                        "withdrawn" -> Triple("Geri çekildi", Color(0xFFF1F5F9), MColors.Ink600)
                        else -> Triple("İnceleniyor", Color(0xFFFEF3C7), Color(0xFFB45309))
                    }
                    MCard(Modifier.fillMaxWidth(), cornerRadius = 16.dp, elevation = 2.dp) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f).padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(application.s("name"), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MColors.Ink900, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        when (appStatus) {
                                            "approved" -> "Başvurun onaylandı · tesisin yayında"
                                            "rejected" -> "Başvurun kabul edilmedi"
                                            "withdrawn" -> "Başvurun geri çekildi"
                                            else -> "İşletmeci başvurun inceleme aşamasında"
                                        },
                                        fontSize = 11.5.sp,
                                        color = MColors.Ink600
                                    )
                                }
                                MBadge(text = statusLabel, textColor = statusColor, backgroundColor = statusTint)
                            }
                            if (application.s("response").isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(application.s("response"), modifier = Modifier.padding(9.dp), fontSize = 11.5.sp, color = MColors.Ink700)
                                }
                            }
                            if (appStatus == "pending") {
                                MPillActionButton(
                                    text = "Başvuruyu geri çek",
                                    icon = Icons.Rounded.Close,
                                    tone = MPillTone.DANGER,
                                    compact = true,
                                    onClick = { pendingWithdrawApp = application }
                                )
                            }
                        }
                    }
                } }
                val scopedVenues = venues.map { venue ->
                    val effectiveKm = venue.distanceKm ?: venue.approximateLocation?.let { pt ->
                        venueOrigin?.let { approximateDistanceKm(it, pt.latitude, pt.longitude) }
                    }
                    venue.copy(distanceKm = effectiveKm)
                }.filter { venue ->
                    val isFocused = focusedId != null && venue.id == focusedId
                    val matchesSport = selectedSportFilter == null || selectedSportFilter in venue.sports
                    val matchesCity = matchesTurkishPlace(venue.city, currentCity)
                    val matchesRadius = when (val limit = maxVenueRadiusKm) {
                        null -> true
                        else -> {
                            val d = venue.distanceKm
                            if (d != null) d <= limit.toFloat()
                            else currentDistrict.isBlank() || matchesTurkishPlace(venue.area, currentDistrict)
                        }
                    }
                    isFocused || (matchesSport && matchesCity && matchesRadius)
                }.sortedWith(compareBy<VenueInfo> { it.id != focusedId }.thenBy { it.distanceKm ?: Float.MAX_VALUE })
                if (scopedVenues.isEmpty()) {
                    item {
                        MSubCard {
                            Text(
                                text = "$currentCity için ${maxVenueRadiusKm?.let { "$it km içinde " }.orEmpty()}uygun tesis bulunamadı.",
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MColors.Ink600
                            )
                        }
                    }
                }
                items(scopedVenues, key = { it.id }) { venue ->
                    VenueSpotlightCard(
                        venue = venue,
                        bookedSlotIds = bookedSlotIds,
                        onDirections = { onDirections?.invoke(venue) },
                        onShareVenue = { onShareVenue?.invoke(venue) },
                        onCardClick = {
                            detailVenue = venue
                        }
                    ) { slot -> requireAccount { selectedBookingSlot = venue to slot } }
                }
                item { BackendMoreCatalogButton("venues") }

            }
        }

        item {
            Spacer(modifier = Modifier.height(70.dp))
        }
    }

    leaveCommunityConfirm?.let { community ->
        AlertDialog(
            onDismissRequest = { leaveCommunityConfirm = null },
            title = { Text("Topluluktan ayrıl?") },
            text = { Text("${community.name} üyeliğinden ayrılmak istediğine emin misin?") },
            confirmButton = {
                TextButton(onClick = { onJoinCommunity(community); leaveCommunityConfirm = null }) {
                    Text("Ayrıl", color = MColors.Rose)
                }
            },
            dismissButton = { TextButton(onClick = { leaveCommunityConfirm = null }) { Text("Vazgeç") } }
        )
    }

    detailCommunity?.let { selected ->
        val fresh = communities.firstOrNull { it.id == selected.id } ?: selected
        val isJoined = fresh.id in joinedCommunityIds
        val isRequested = fresh.id in requestedCommunityIds
        val backendEnabled = LocalBackendRepository.current?.enabled == true
        ModalBottomSheet(
            onDismissRequest = { detailCommunity = null },
            containerColor = Color(0xFFF5F6F8),
            dragHandle = { MPremiumSheetHandle() },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    MSheetHeading(
                        title = fresh.name,
                        subtitle = "${fresh.city} · ${fresh.area} · ${fresh.type.label}",
                        icon = Icons.Rounded.Hub,
                        onDismiss = { detailCommunity = null }
                    )
                }
                item {
                    MCard(Modifier.fillMaxWidth(), cornerRadius = 16.dp, elevation = 2.dp) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Text(
                                        text = communityDisplayId(fresh.id),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MColors.BrandDeep
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    MBadge("${fresh.members} üye", MColors.Ink700, Color(0xFFF1F5F9))
                                    MBadge(fresh.access.label, MColors.BrandDeep, MColors.BrandTint)
                                }
                            }
                            Text(
                                text = fresh.description.ifBlank { "Yerel sporcuları aynı çatı altında buluşturan topluluk ağı." },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MColors.Ink700
                            )
                            if (fresh.sports.isNotEmpty()) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(fresh.sports) { sport ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFF1F5F9),
                                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                        ) {
                                            Text(
                                                text = "${sport.emoji} ${sport.label}",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MColors.Ink700
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    val isOwner = fresh.ownerId == DemoData.ME
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isJoined) {
                                MButton(
                                    text = "Topluluk sohbeti",
                                    onClick = { detailCommunity = null; requireAccount { onOpenCommunityChat(fresh) } },
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Rounded.Forum,
                                    height = 44.dp
                                )
                            } else {
                                val inviteOnlyBlocked = fresh.access == CommunityAccess.INVITE && !isRequested
                                MButton(
                                    text = when {
                                        isRequested -> "Başvuru bekliyor · İptal"
                                        inviteOnlyBlocked -> "Yalnızca Davetle"
                                        else -> "Topluluğa katıl"
                                    },
                                    onClick = { detailCommunity = null; onJoinCommunity(fresh) },
                                    modifier = Modifier.weight(1f),
                                    enabled = !inviteOnlyBlocked,
                                    isPrimary = !isRequested && !inviteOnlyBlocked,
                                    isDestructive = isRequested,
                                    icon = Icons.Rounded.GroupAdd,
                                    height = 44.dp
                                )
                            }
                            onShareCommunity?.let { share ->
                                FilledTonalButton(
                                    onClick = { share(fresh) },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Icon(Icons.Rounded.Share, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text(if (backendEnabled) "Paylaş" else "+2 TKN", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        if (isJoined && !isOwner) {
                            MPillActionButton(
                                text = "Topluluktan ayrıl",
                                icon = Icons.Rounded.ExitToApp,
                                tone = MPillTone.DANGER,
                                compact = true,
                                onClick = { detailCommunity = null; leaveCommunityConfirm = fresh }
                            )
                        }
                    }
                }
                item { BackendCommunityActions(fresh, onArchived = { detailCommunity = null }) }
            }
        }
    }

    detailVenue?.let { selected ->
        val rawVenue = venues.firstOrNull { it.id == selected.id } ?: selected
        val freshVenue = rawVenue.copy(distanceKm = rawVenue.distanceKm ?: rawVenue.approximateLocation?.let { pt -> venueOrigin?.let { approximateDistanceKm(it, pt.latitude, pt.longitude) } })
        VenueDetailSheet(
            venue = freshVenue,
            bookedSlotIds = bookedSlotIds,
            onDismiss = { detailVenue = null },
            onDirections = { onDirections?.invoke(freshVenue) },
            onShare = { onShareVenue?.invoke(freshVenue) },
            onOpenReviews = {
                detailVenue = null
                reviewVenue = freshVenue
            },
            onSelectSlot = { slot ->
                requireAccount {
                    detailVenue = null
                    selectedBookingSlot = freshVenue to slot
                }
            }
        )
    }

    if (selectedBookingSlot != null) {
        val (v, s) = selectedBookingSlot!!
        VenueBookingSheet(
            venue = v,
            slot = s,
            onDismiss = { selectedBookingSlot = null },
            onConfirmBooking = { splitCount, finalAmount, method ->
                onBookSlot(v, s, splitCount, finalAmount, method)
                selectedBookingSlot = null
            }
        )
    }

    if (showCommunityCityPicker) {
        CityFilterPickerSheet(
            selectedCity = pickedCommunityCity,
            currentCity = currentCity,
            title = "Topluluk ili seç",
            onSelectCity = { chosen ->
                allCommunityCities = false
                pickedCommunityCity = chosen
            },
            onDismiss = { showCommunityCityPicker = false }
        )
    }
}
}
