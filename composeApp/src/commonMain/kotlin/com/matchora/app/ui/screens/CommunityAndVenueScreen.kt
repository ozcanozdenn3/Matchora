package com.matchora.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
    onDirections: ((VenueInfo) -> Unit)? = null
) {
    var reviewVenue by remember { mutableStateOf<VenueInfo?>(null) }
    var manageVenue by remember { mutableStateOf(false) }
    var createCommunity by remember { mutableStateOf(false) }
    var venueApplication by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Topluluk & Kampüs, 1: Tesisler & Saha Bul
    var selectedSportFilter by remember { mutableStateOf<com.matchora.app.domain.Sport?>(null) }
    LaunchedEffect(initialTab, focusedId) { selectedTab = initialTab.coerceIn(0, 1) }
    var travelModeActive by remember { mutableStateOf(false) }
    var travelCity by remember { mutableStateOf("") }
    var showTravelLocation by remember { mutableStateOf(false) }
    val backend = LocalBackendRepository.current
    val requireAccount = LocalRequireAccount.current
    val wire = if (backend != null) backend.snapshots.collectAsState().value else null
    val command = rememberBackendAction()
    LaunchedEffect(wire) { wire?.o("discovery")?.let { travelModeActive = it.b("travelMode"); travelCity = it.s("city") } }
    if (showTravelLocation) CityAndLocationSelectorSheet(travelCity.ifBlank { "İstanbul" }, currentDistrict, false,
        onDismiss = { showTravelLocation = false }, onSelectLocation = { city, district, _ ->
            if (backend?.enabled == true) command("discovery.save", apiPayload("city" to city, "district" to district, "travelMode" to true))
            else { travelModeActive = true; travelCity = city }
        })
    if (createCommunity) CreateCommunitySheet(currentCity, currentDistrict, { createCommunity = false }, onCreateCommunity)
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ═══════════════════════════════════════════════════════════════════
            // 0. ÜST BAŞLIK VE PRO OBSIDIAN SEKME KONTROLÜ
            // ═══════════════════════════════════════════════════════════════════
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    MFeatureHeader("Spor ağı", "Yeni insanlar. Yeni sahalar. Aynı tutku.", Icons.Rounded.Hub, accent = Color(0xFF85DFCC))
                    MSegmentedControl(listOf("Topluluklar", "Tesisler"), selectedTab, { selectedTab = it })
                }
            }

        when (selectedTab) {
            0 -> {
                // ═══════════════ TAB 0: TOPLULUK & KAMPÜS MODÜLLERİ ═══════════════
                // Modül A: Travel Mode
                item {
                    MCard(Modifier.fillMaxWidth(), cornerRadius = 16.dp, elevation = 2.dp, onClick = { requireAccount { showTravelLocation = true } }) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                shape = RoundedCornerShape(11.dp),
                                color = MColors.Ink900,
                                shadowElevation = 4.dp,
                                modifier = Modifier.shadow(6.dp, RoundedCornerShape(11.dp), ambientColor = Color(0x360B1220), spotColor = Color(0x480B1220))
                            ) {
                                Icon(Icons.Rounded.FlightTakeoff, null, Modifier.padding(8.dp).size(18.dp), tint = Color(0xFF9EDDC8))
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Seyahat modu", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = MColors.Ink900)
                                Text(if (travelModeActive) "$travelCity · Misafir sporcu" else "Spor pasaportun seninle", fontSize = 11.sp, color = MColors.Ink500)
                            }
                            Switch(checked = travelModeActive, onCheckedChange = { enabled ->
                                requireAccount {
                                    if (enabled) showTravelLocation = true
                                    else if (backend?.enabled == true) command("discovery.save", apiPayload("city" to wire?.o("profile")?.s("city", "İstanbul"), "district" to wire?.o("profile")?.s("area", currentDistrict), "travelMode" to false))
                                    else travelModeActive = false
                                }
                            }, modifier = Modifier.height(28.dp))
                        }
                    }
                }

                item { MButton("Topluluğunu oluştur", { requireAccount { createCommunity = true } }, icon = Icons.Rounded.Add) }
                item { SportPickerField(selectedSportFilter, { selectedSportFilter = it }) }

                // Modül B: Topluluklar Listesi
                items(communities.filter { selectedSportFilter == null || selectedSportFilter in it.sports }.sortedBy { it.id != focusedId }, key = { it.id }) { community ->
                    val isJoined = community.id in joinedCommunityIds
                    CommunitySpotlightCard(
                        community = community,
                        joined = isJoined,
                        requested = community.id in requestedCommunityIds,
                        onShare = { onShareCommunity?.invoke(community) },
                        onJoin = { onJoinCommunity(community) }
                    )
                    if (isJoined) {
                        TextButton(onClick = { requireAccount { onOpenCommunityChat(community) } }) {
                            Icon(Icons.Rounded.Forum, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Topluluk sohbeti")
                        }
                    }
                    BackendCommunityActions(community)
                }
                item { BackendMoreCatalogButton("communities") }
            }

            1 -> {
                // ═══════════════ TAB 1: SAHA VE KORT REZERVASYONU MODÜLLERİ ═══════════════
                if (wire?.s("role") in listOf("admin", "venue_owner")) item { TextButton(onClick = { requireAccount { manageVenue = true } }) { Text("Tesis yönetimi") } }
                else item { MButton("Tesisini ekle", { requireAccount { venueApplication = true } }, icon = Icons.Rounded.AddBusiness) }
                item { SportPickerField(selectedSportFilter, { selectedSportFilter = it }) }
                wire?.a("venueApplications").orEmpty().forEach { application -> item(key = "application-${application.s("id")}") {
                    MCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(application.s("name"), style = MaterialTheme.typography.titleSmall)
                        Text(when (application.s("status")) { "approved" -> "Onaylandı · tesisin yayında"; "rejected" -> "Başvuru kabul edilmedi"; "withdrawn" -> "Başvurun geri çekildi"; else -> "İşletmeci başvurun inceleniyor" }, style = MaterialTheme.typography.bodySmall)
                        if (application.s("response").isNotBlank()) Text(application.s("response"), style = MaterialTheme.typography.bodySmall)
                        if (application.s("status") == "pending") TextButton(onClick = { command("venue.application.withdraw", apiPayload("applicationId" to application.s("id"), "expectedVersion" to application.i("version", 1))) }) { Text("Başvuruyu geri çek") }
                    } }
                } }
                items(venues.filter { selectedSportFilter == null || selectedSportFilter in it.sports }.sortedBy { it.id != focusedId }, key = { it.id }) { venue ->
                    VenueSpotlightCard(
                        venue = venue,
                        bookedSlotIds = bookedSlotIds,
                        onDirections = { onDirections?.invoke(venue) },
                        onShareVenue = { onShareVenue?.invoke(venue) },
                        onCardClick = {
                            requireAccount {
                                val firstAvailable = venue.slots.firstOrNull { it.available && "${venue.id}:${it.id}" !in bookedSlotIds }
                                if (firstAvailable != null) selectedBookingSlot = venue to firstAvailable else reviewVenue = venue
                            }
                        }
                    ) { slot -> requireAccount { selectedBookingSlot = venue to slot } }
                    TextButton(onClick = { requireAccount { reviewVenue = venue } }) { Text("Tesis değerlendirmeleri") }
                }
                item { BackendMoreCatalogButton("venues") }

            }
        }

        item {
            Spacer(modifier = Modifier.height(70.dp))
        }
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
}
}
