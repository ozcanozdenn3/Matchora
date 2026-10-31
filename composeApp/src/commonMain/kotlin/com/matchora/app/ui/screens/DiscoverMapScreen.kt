package com.matchora.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.domain.*
import com.matchora.app.state.*
import com.matchora.app.ui.components.LocalScrollToTopTrigger
import com.matchora.app.ui.components.MBadge
import com.matchora.app.ui.components.MCard
import com.matchora.app.ui.components.MChemistryBadge
import com.matchora.app.ui.components.MChip
import com.matchora.app.ui.components.MapPinData
import com.matchora.app.ui.components.NativeMapView
import com.matchora.app.ui.components.SportPickerField
import com.matchora.app.ui.theme.MColors

// ═══════════════════════════════════════════════════════════════════════════════
// APPLE MAPS / AIRBNB LUXURY SPORTS RADAR SCREEN
// Gerçek Apple Maps (MKMapView) entegrasyonu, yüzen zarif arama kapsülü,
// etkileşimli yatay etkinlik kartları ve akıllı filtreleme.
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun DiscoverMapScreen(
    paddingValues: PaddingValues,
    events: List<SportEvent>,
    user: UserProfile = DemoData.me,
    currentCity: String = "İstanbul",
    currentDistrict: String = "Kadıköy",
    isGpsActive: Boolean = false,
    joinedEventIds: Set<String> = emptySet(),
    waitlistedEventIds: Set<String> = emptySet(),
    onLocationClick: () -> Unit = {},
    onSelectEvent: (SportEvent) -> Unit,
    onQuickJoin: (SportEvent) -> Unit,
    onToggleWaitlist: (SportEvent) -> Unit = {}
) {
    var selectedSport by remember { mutableStateOf<Sport?>(null) }
    var isMapView by remember { mutableStateOf(true) }
    var selectedEventId by remember { mutableStateOf<String?>(events.firstOrNull()?.id) }
    var searchQuery by remember { mutableStateOf("") }
    var activeAiFilterLabel by remember { mutableStateOf<String?>(null) }
    var showSmartMatchSheet by remember { mutableStateOf(false) }

    val networkEvents = remember(events, user.campus, user.company) {
        events.filter { event ->
            when (event.networkScope) {
                EventNetworkScope.CITY, EventNetworkScope.TRAVEL -> true
                EventNetworkScope.CAMPUS -> !user.campus.isNullOrBlank() && event.networkName.equals(user.campus, ignoreCase = true)
                EventNetworkScope.COMPANY -> !user.company.isNullOrBlank() && event.networkName.equals(user.company, ignoreCase = true)
            }
        }
    }

    val filteredEvents = remember(networkEvents, selectedSport, searchQuery, activeAiFilterLabel) {
        networkEvents.filter { ev ->
            val isDiscoverable = ev.status in setOf(
                com.matchora.app.domain.EventStatus.PUBLISHED,
                com.matchora.app.domain.EventStatus.FILLING,
                com.matchora.app.domain.EventStatus.FULL
            )
            val matchSport = selectedSport == null || ev.sport == selectedSport
            val matchAi = activeAiFilterLabel == null || when (activeAiFilterLabel) {
                "FOOTBALL_TONIGHT" -> ev.sport == Sport.FOOTBALL && ev.matchesDayFilter("Bugün")
                "WEEKEND_RACKET" -> (ev.sport == Sport.TENNIS || ev.sport == Sport.PADEL) &&
                    (ev.matchesDayFilter("Cumartesi") || ev.matchesDayFilter("Pazar"))
                "URGENT_FILL" -> ev.emergency && ev.freeSpots > 0
                "TEAM_MATCH" -> ev.isTeamEvent
                else -> true
            }
            val matchQuery = searchQuery.isBlank() ||
                ev.title.contains(searchQuery, ignoreCase = true) ||
                ev.venue.contains(searchQuery, ignoreCase = true) ||
                ev.area.contains(searchQuery, ignoreCase = true) ||
                ev.sport.label.contains(searchQuery, ignoreCase = true)
            isDiscoverable && matchSport && matchAi && matchQuery
        }
    }

    val mapPinDataList = remember(filteredEvents) {
        filteredEvents.filterNot { it.area.contains("online", ignoreCase = true) }
            .map { event ->
                val point = event.approximateLocation?.let { it.latitude to it.longitude } ?: demoCoordinatesForArea(event.area)
                MapPinData(
                    id = event.id,
                    title = event.title,
                    subtitle = when {
                        event.freeSpots > 0 -> "${event.freeSpots} boş · ${event.time}"
                        event.waitlistCount > 0 -> "Dolu · ${event.waitlistCount} bekleme"
                        else -> "Dolu · ${event.time}"
                    },
                    latitude = point.first,
                    longitude = point.second,
                    sportEmoji = event.sport.emoji
                )
            }
    }
    val selectedEvent = filteredEvents.firstOrNull { it.id == selectedEventId }
    val mapCenter = selectedEvent?.takeUnless { it.area.contains("online", ignoreCase = true) }?.let { event -> event.approximateLocation?.let { it.latitude to it.longitude } ?: demoCoordinatesForArea(event.area) }
        ?: demoCoordinatesForArea(currentDistrict)

    val carouselListState = rememberLazyListState()

    // Filtre değişince seçili pin filtre dışına düşerse ilk sonuca dön.
    LaunchedEffect(filteredEvents) {
        if (filteredEvents.none { it.id == selectedEventId }) {
            selectedEventId = filteredEvents.firstOrNull()?.id
        }
    }

    // Seçili etkinlik değiştiğinde alttaki kart carousel'ını kaydır
    LaunchedEffect(selectedEventId) {
        val index = filteredEvents.indexOfFirst { it.id == selectedEventId }
        if (index >= 0) {
            carouselListState.animateScrollToItem(index)
        }
    }

    val safeTopPadding = maxOf(paddingValues.calculateTopPadding(), 54.dp)

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (isMapView) {
            // ═══════════════════════════════════════════════════════════════
            // GERÇEK TAM EKRAN APPLE MAPS (MKMapView)
            // ═══════════════════════════════════════════════════════════════
            NativeMapView(
                modifier = Modifier.fillMaxSize(),
                latitude = mapCenter.first,
                longitude = mapCenter.second,
                zoomMeters = 3800.0,
                pins = mapPinDataList,
                selectedPinId = selectedEventId,
                onPinSelected = { pinId ->
                    selectedEventId = pinId
                }
            )

            // ═══════════════════════════════════════════════════════════════
            // SAĞ YÜZEN AKILLI EŞLEŞME SEMBOLÜ & CANLI KONUM FAB
            // ═══════════════════════════════════════════════════════════════
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        bottom = paddingValues.calculateBottomPadding() + 242.dp,
                        end = 16.dp
                    )
            ) {
                Surface(
                    onClick = { showSmartMatchSheet = true },
                    shape = RoundedCornerShape(15.dp),
                    color = MColors.Ink900,
                    shadowElevation = 8.dp,
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = if (activeAiFilterLabel != null) Color(0xFFFFB37B) else Color.White.copy(alpha = 0.22f)
                    ),
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(15.dp),
                            ambientColor = Color(0x3A0B1220),
                            spotColor = Color(0x520B1220)
                        )
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF283548), MColors.Ink900)
                                )
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "Akıllı Eşleşme",
                            tint = Color(0xFFFFB37B),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                if (activeAiFilterLabel != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MColors.Mint)
                            .border(1.5.dp, Color.White, CircleShape)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(15.dp),
                color = Color(0xFF203B35),
                shadowElevation = 8.dp,
                border = BorderStroke(1.5.dp, if (isGpsActive) MColors.Mint else Color(0x18000000)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        bottom = paddingValues.calculateBottomPadding() + 188.dp,
                        end = 16.dp
                    )
                    .size(46.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onLocationClick() }
                ) {
                    Icon(
                        imageVector = if (isGpsActive) Icons.Rounded.MyLocation else Icons.Rounded.LocationSearching,
                        contentDescription = "Konumumu Bul",
                        tint = if (isGpsActive) Color(0xFF9EDDC8) else Color(0xFFE1CAA0),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // ═══════════════════════════════════════════════════════════════
            // ALTTA SÜZÜLEN ETKİNLİK KARTLARI CAROUSEL (Airbnb & Apple Maps Tarzı)
            // ═══════════════════════════════════════════════════════════════
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = paddingValues.calculateBottomPadding() + 8.dp)
            ) {
                // Canlı Gösterge Hapı
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF203B35).copy(alpha = .97f))
                        .border(1.dp, Color(0x18000000), RoundedCornerShape(20.dp))
                        .clickable { onLocationClick() }
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isGpsActive) MColors.Mint else MColors.Brand)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isGpsActive) "Canlı GPS · $currentDistrict (${filteredEvents.size} Aktif Maç)" else "$currentDistrict · ${filteredEvents.size} Aktif Maç & Kort",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (isGpsActive) Color(0xFF9EDDC8) else Color(0xFFF2F5F2)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Yatay Kaydırılabilir Kartlar
                if (filteredEvents.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, MColors.Line),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Bu filtrelerde karşılaşma yok", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MColors.Ink900)
                                Text("Bölge veya spor seçimini genişlet.", fontSize = 11.sp, color = MColors.Ink500)
                            }
                            TextButton(onClick = {
                                selectedSport = null
                                searchQuery = ""
                            }) { Text("Filtreleri temizle") }
                        }
                    }
                } else LazyRow(
                    state = carouselListState,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filteredEvents, key = { it.id }) { ev ->
                        val isSelected = ev.id == selectedEventId

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            shadowElevation = if (isSelected) 10.dp else 4.dp,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(0xFF4E7566) else Color(0xFFD0DDD6)
                            ),
                            modifier = Modifier
                                .width(308.dp)
                                .height(124.dp)
                                .clickable {
                                    selectedEventId = ev.id
                                    onSelectEvent(ev)
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.linearGradient(listOf(Color.White, Color(0xFFF2F6F3))))
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFFE4EDE6)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(ev.sport.emoji, fontSize = 20.sp)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                                            Text(
                                                text = ev.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = MColors.Ink900,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${ev.venue} · ${ev.time}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 11.sp,
                                                color = MColors.Ink500,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    if (ev.score.reasons.isNotEmpty()) {
                                        MChemistryBadge(percent = ev.score.total, label = "Uyum")
                                    } else {
                                        Text("Yeni ilan", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MColors.Ink500)
                                    }
                                }

                                HorizontalDivider(color = MColors.LineSoft)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(MColors.Mint)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "Son ${ev.freeSpots} Boş",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MColors.Mint
                                        )
                                        Text(
                                            text = ev.distanceKm?.let { " · $it km" }.orEmpty(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MColors.Ink500
                                        )
                                    }

                                    val alreadyJoined = joinedEventIds.contains(ev.id)
                                    val alreadyWaiting = waitlistedEventIds.contains(ev.id)
                                    val canJoin = ev.freeSpots > 0 && ev.status in setOf(
                                        com.matchora.app.domain.EventStatus.PUBLISHED,
                                        com.matchora.app.domain.EventStatus.FILLING
                                    )
                                    val canWaitlist = ev.status == com.matchora.app.domain.EventStatus.FULL
                                    val canAct = !alreadyJoined && (canJoin || canWaitlist)
                                    Button(
                                        onClick = {
                                            when {
                                                alreadyWaiting -> onToggleWaitlist(ev)
                                                canJoin -> onQuickJoin(ev)
                                                canWaitlist -> onToggleWaitlist(ev)
                                            }
                                        },
                                        enabled = canAct,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (canAct) Color(0xFF203B35) else MColors.SurfaceSunken,
                                            contentColor = if (canAct) Color.White else MColors.Ink500
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                alreadyJoined -> "Katıldın"
                                                alreadyWaiting -> "Listeden çık"
                                                canWaitlist -> "Bekleme"
                                                canJoin -> "Katıl"
                                                else -> "Kapalı"
                                            },
                                            color = if (canAct) Color.White else MColors.Ink500,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                            contentDescription = null,
                                            tint = if (canAct) Color.White else MColors.Ink500,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ═══════════════════════════════════════════════════════════════
            // LİSTE GÖRÜNÜMÜ: TERTEMİZ, TAŞMASIZ KARTLAR
            // ═══════════════════════════════════════════════════════════════
            val listViewState = rememberLazyListState()
            val scrollToTopTrigger = LocalScrollToTopTrigger.current
            LaunchedEffect(scrollToTopTrigger) {
                if (scrollToTopTrigger > 0) {
                    listViewState.animateScrollToItem(0)
                }
            }
            LazyColumn(
                state = listViewState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(
                    top = safeTopPadding + 115.dp,
                    bottom = 90.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredEvents.isEmpty()) {
                    item {
                        MCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Bu filtrelerde karşılaşma yok", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MColors.Ink900)
                                Text("Filtreleri genişletip tekrar dene.", fontSize = 12.sp, color = MColors.Ink500)
                                TextButton(onClick = {
                                    selectedSport = null
                                            searchQuery = ""
                                }) { Text("Filtreleri temizle") }
                            }
                        }
                    }
                }
                items(filteredEvents, key = { it.id }) { ev ->
                    MCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onSelectEvent(ev) }
                    ) {
                        Column(modifier = Modifier.background(Brush.linearGradient(listOf(Color.White, Color(0xFFF2F6F3)))).padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(ev.sport.emoji, fontSize = 22.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                                        Text(
                                            text = ev.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MColors.Ink900,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${ev.venue}${ev.distanceKm?.let { " · $it km" }.orEmpty()} · ${ev.day} ${ev.time}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MColors.Ink500,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (ev.score.reasons.isNotEmpty()) {
                                    MChemistryBadge(percent = ev.score.total, label = "Uyum")
                                } else {
                                    Text("Yeni ilan", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MColors.Ink500)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${ev.freeSpots} Boş Koltuk / ${ev.capacity} Kişi",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MColors.Mint
                                )
                                Text(
                                    text = "İncele >",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MColors.Brand
                                )
                            }
                        }
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // ÜST BÖLÜM: YÜZEN ZARİF ARAMA KAPSÜLÜ & SPOR FİLTRELERİ (Floating Glass)
        // ═══════════════════════════════════════════════════════════════════
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = safeTopPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Yüzen Arama & Toggle Barı
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFFFFEFA),
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Color(0xFFD0DDD6)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Ara",
                        tint = MColors.Brand,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onLocationClick() }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isGpsActive) Icons.Rounded.MyLocation else Icons.Rounded.LocationOn,
                                contentDescription = null,
                                tint = if (isGpsActive) MColors.Mint else MColors.Brand,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isGpsActive) "Canlı GPS · $currentDistrict" else "$currentCity · $currentDistrict",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isGpsActive) MColors.Mint else MColors.Ink900
                            )
                        }
                        Text(
                            text = "Bölgeyi değiştir",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MColors.Ink500
                        )
                    }

                    // Harita / Liste Geçiş Butonları
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isMapView) Color(0xFF203B35) else Color.Transparent)
                                .clickable { isMapView = true }
                                .padding(horizontal = 9.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Map,
                                contentDescription = "Harita",
                                tint = if (isMapView) Color.White else MColors.Ink500,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (!isMapView) Color(0xFF203B35) else Color.Transparent)
                                .clickable { isMapView = false }
                                .padding(horizontal = 9.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Menu,
                                contentDescription = "Liste",
                                tint = if (!isMapView) Color.White else MColors.Ink500,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            SportPickerField(selectedSport, { selectedSport = it })
        }
    }

    if (showSmartMatchSheet) {
        val bestMatch = filteredEvents.firstOrNull { it.emergency } ?: filteredEvents.firstOrNull()
        SmartMatchSheet(
            activeFilterKey = activeAiFilterLabel,
            onSelectFilter = { activeAiFilterLabel = it },
            matchingCount = filteredEvents.size,
            bestMatch = bestMatch,
            onDismiss = { showSmartMatchSheet = false },
            onJoinBestMatch = {
                showSmartMatchSheet = false
                bestMatch?.let(onSelectEvent)
            }
        )
    }
}

private fun demoCoordinatesForArea(area: String): Pair<Double, Double> = when {
    area.contains("Ankara", ignoreCase = true) -> 39.9208 to 32.8541
    area.contains("Maslak", ignoreCase = true) -> 41.1118 to 29.0225
    area.contains("Çankaya", ignoreCase = true) -> 39.9120 to 32.8597
    area.contains("Kalamış", ignoreCase = true) || area.contains("Fenerbahçe", ignoreCase = true) -> 40.9748 to 29.0430
    area.contains("Caddebostan", ignoreCase = true) -> 40.9628 to 29.0640
    area.contains("Suadiye", ignoreCase = true) -> 40.9584 to 29.0831
    area.contains("Bostancı", ignoreCase = true) -> 40.9514 to 29.0962
    area.contains("Acıbadem", ignoreCase = true) -> 40.9944 to 29.0480
    area.contains("Fikirtepe", ignoreCase = true) -> 40.9897 to 29.0471
    area.contains("Online", ignoreCase = true) -> 40.9760 to 29.0400
    area.contains("Moda", ignoreCase = true) -> 40.9840 to 29.0260
    else -> 40.9760 to 29.0400
}
