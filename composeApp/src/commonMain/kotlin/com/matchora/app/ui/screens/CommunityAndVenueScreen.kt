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
    bookedSlotIds: Set<String> = emptySet()
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Topluluk & Kampüs, 1: Tesisler & Saha Bul
    var travelModeActive by remember { mutableStateOf(false) }
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
                    MCard(Modifier.fillMaxWidth(), cornerRadius = 16.dp, elevation = 2.dp) {
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
                                Text(if (travelModeActive) "İzmir · Misafir sporcu" else "Spor pasaportun seninle", fontSize = 11.sp, color = MColors.Ink500)
                            }
                            Switch(checked = travelModeActive, onCheckedChange = { travelModeActive = it }, modifier = Modifier.height(28.dp))
                        }
                    }
                }

                // Modül B: Topluluklar Listesi
                items(DemoData.communities, key = { it.id }) { community ->
                    CommunitySpotlightCard(community, community.id in joinedCommunityIds,
                        community.id in requestedCommunityIds) { onJoinCommunity(community) }
                }
            }

            1 -> {
                // ═══════════════ TAB 1: SAHA VE KORT REZERVASYONU MODÜLLERİ ═══════════════
                items(DemoData.venues, key = { it.id }) { venue ->
                    VenueSpotlightCard(venue, bookedSlotIds) { selectedBookingSlot = venue to it }
                }

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
