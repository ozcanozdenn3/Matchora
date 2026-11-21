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
import com.matchora.app.ui.components.LocalRequireAccount
import com.matchora.app.ui.components.LocalBackendRepository
import com.matchora.app.ui.components.DeviceLocationState
import com.matchora.app.ui.components.BackendMoreEventsButton
import com.matchora.app.ui.components.rememberRegionCenter
import com.matchora.app.ui.components.MBadge
import com.matchora.app.ui.components.MCard
import com.matchora.app.ui.components.MChemistryBadge
import com.matchora.app.ui.components.MChip
import com.matchora.app.ui.components.MPillActionButton
import com.matchora.app.ui.components.MPillTone
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
    pendingRequestEventIds: Set<String> = emptySet(),
    onLocationClick: () -> Unit = {},
    onSelectEvent: (SportEvent) -> Unit,
    onQuickJoin: (SportEvent) -> Unit,
    onToggleWaitlist: (SportEvent) -> Unit = {}
) {
    var selectedSport by remember { mutableStateOf<Sport?>(null) }
    var isMapView by remember { mutableStateOf(true) }
    var selectedEventId by remember(currentCity, currentDistrict) { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var activeAiFilterLabel by remember { mutableStateOf<String?>(null) }
    var showSmartMatchSheet by remember { mutableStateOf(false) }
    val backendMode = LocalBackendRepository.current?.enabled == true
    val requireAccount = LocalRequireAccount.current

    val networkEvents = remember(events, user.campus, user.company, backendMode) {
        events.filter { event ->
            when (event.networkScope) {
                EventNetworkScope.CITY, EventNetworkScope.TRAVEL -> true
                EventNetworkScope.CAMPUS, EventNetworkScope.COMPANY -> true
            }
        }
    }

    val filteredEvents = remember(networkEvents, selectedSport, searchQuery, activeAiFilterLabel, backendMode, currentCity) {
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
            val matchCity = !backendMode || com.matchora.app.state.matchesTurkishPlace(ev.city, currentCity)
            isDiscoverable && matchSport && matchAi && matchQuery && matchCity
        }
    }

    val deviceLocation by DeviceLocationState.current.collectAsState()
    val regionCenter = rememberRegionCenter(currentCity, currentDistrict)
    val mapPinDataList = remember(filteredEvents, backendMode) {
        filteredEvents.filterNot { it.area.contains("online", ignoreCase = true) }
            .mapNotNull { event ->
                val point = event.approximateLocation?.let { it.latitude to it.longitude } ?: if (backendMode) return@mapNotNull null else demoCoordinatesForArea(event.area)
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
    val selectedPoint = selectedEvent?.takeUnless { it.area.contains("online", ignoreCase = true) }?.approximateLocation
    val selectedRegionLocation = deviceLocation?.takeIf {
        isGpsActive || it.city.isNullOrBlank() || com.matchora.app.state.matchesTurkishPlace(it.city, currentCity)
    }
    val liveRegionCenter = selectedRegionLocation ?: regionCenter
    val mapCenter = run {
        selectedPoint?.let { it.latitude to it.longitude }
            ?: if (!backendMode && selectedEvent != null) demoCoordinatesForArea(selectedEvent.area)
            else liveRegionCenter?.let { it.latitude to it.longitude }
                ?: if (!backendMode && com.matchora.app.state.matchesTurkishPlace(currentCity, "İstanbul")) demoCoordinatesForArea(currentDistrict)
                else com.matchora.app.state.cityMapCenter(currentCity).let { it.latitude to it.longitude }
    }

    val carouselListState = rememberLazyListState()

    // Filtre değişince seçili pin filtre dışına düşerse ilk sonuca dön.
    LaunchedEffect(filteredEvents) {
        if (filteredEvents.none { it.id == selectedEventId }) {
            selectedEventId = null
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
                zoomMeters = if (selectedEvent == null && selectedRegionLocation != null) 3800.0
                    else if (selectedEvent == null && liveRegionCenter == null && currentCity != "İstanbul") 1600000.0
                    else if (selectedEvent == null && (currentDistrict.isBlank() || currentDistrict == "Merkez")) 60000.0 else 3800.0,
                pins = mapPinDataList,
                selectedPinId = selectedEventId,
                onPinSelected = { pinId ->
                    selectedEventId = pinId
                }
            )

            // ═══════════════════════════════════════════════════════════════
            // YÜZEN KONTROLLER: SOL ALT KÖŞEDE ALT ALTA SPOR RADARI & CANLI KONUM
            // ═══════════════════════════════════════════════════════════════
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(
                        bottom = paddingValues.calculateBottomPadding() + 188.dp,
                        start = 16.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box {
                    Surface(
                        onClick = { requireAccount { showSmartMatchSheet = true } },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFDC2626),
                        shadowElevation = 6.dp,
                        border = BorderStroke(
                            width = 1.2.dp,
                            color = if (activeAiFilterLabel != null) Color.White else Color.White.copy(alpha = 0.42f)
                        ),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFFF453A), Color(0xFFD32F2F))
                                    )
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Radar,
                                contentDescription = "Spor Radarı",
                                tint = Color.White,
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
                    onClick = { onLocationClick() },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF203B35),
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.2.dp, if (isGpsActive) MColors.Mint else Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = if (isGpsActive) Icons.Rounded.MyLocation else Icons.Rounded.LocationSearching,
                            contentDescription = "Konumumu Bul",
                            tint = if (isGpsActive) Color(0xFF9EDDC8) else Color(0xFFE1CAA0),
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════════
            // ALTTA SÜZÜLEN ETKİNLİK KARTLARI CAROUSEL (Airbnb & Apple Maps Tarzı)
            // ═══════════════════════════════════════════════════════════════
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = paddingValues.calculateBottomPadding() + 18.dp)
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
                    val hasActiveMapFilter = selectedSport != null || searchQuery.isNotBlank() || activeAiFilterLabel != null
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
                            if (hasActiveMapFilter) {
                                MPillActionButton(
                                    text = "Filtreleri temizle",
                                    icon = Icons.Rounded.FilterAltOff,
                                    tone = MPillTone.BRAND,
                                    compact = true,
                                    onClick = {
                                        selectedSport = null
                                        searchQuery = ""
                                        activeAiFilterLabel = null
                                    }
                                )
                            }
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

                                    val isHost = ev.hostId == DemoData.ME
                                    val alreadyJoined = joinedEventIds.contains(ev.id)
                                    val alreadyWaiting = waitlistedEventIds.contains(ev.id)
                                    val alreadyPending = pendingRequestEventIds.contains(ev.id)
                                    val canJoin = ev.freeSpots > 0 && ev.status in setOf(
                                        com.matchora.app.domain.EventStatus.PUBLISHED,
                                        com.matchora.app.domain.EventStatus.FILLING
                                    )
                                    val canWaitlist = ev.status == com.matchora.app.domain.EventStatus.FULL
                                    val canAct = !isHost && !alreadyJoined && !alreadyPending && (alreadyWaiting || canJoin || canWaitlist)
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
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                isHost -> "İlanın"
                                                alreadyJoined -> "Katıldın"
                                                alreadyPending -> "Bekliyor"
                                                alreadyWaiting -> "Listeden çık"
                                                canWaitlist -> "Bekleme"
                                                canJoin -> if (ev.approvalRequired) "Başvur" else "Katıl"
                                                else -> "Kapalı"
                                            },
                                            color = if (canAct) Color.White else MColors.Ink500,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            softWrap = false
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
                    if (backendMode) {
                        item {
                            Box(modifier = Modifier.height(124.dp), contentAlignment = Alignment.Center) {
                                BackendMoreEventsButton()
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
                        val hasActiveListFilter = selectedSport != null || searchQuery.isNotBlank() || activeAiFilterLabel != null
                        MCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Bu filtrelerde karşılaşma yok", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MColors.Ink900)
                                Text("Filtreleri genişletip tekrar dene.", fontSize = 12.sp, color = MColors.Ink500)
                                if (hasActiveListFilter) {
                                    MPillActionButton(
                                        text = "Filtreleri temizle",
                                        icon = Icons.Rounded.FilterAltOff,
                                        tone = MPillTone.BRAND,
                                        compact = true,
                                        onClick = {
                                            selectedSport = null
                                            searchQuery = ""
                                            activeAiFilterLabel = null
                                        }
                                    )
                                }
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
                                val listBadge = when {
                                    ev.hostId == DemoData.ME -> "İlanın"
                                    ev.id in joinedEventIds -> "Katıldın"
                                    ev.id in pendingRequestEventIds -> "Bekliyor"
                                    ev.id in waitlistedEventIds -> "Yedek listede"
                                    else -> "İncele >"
                                }
                                Text(
                                    text = listBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MColors.Brand
                                )
                            }
                        }
                    }
                }
                if (backendMode) item { BackendMoreEventsButton() }
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
                        tint = Color(0xFF203B35),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onLocationClick() }
                        ) {
                            Icon(
                                imageVector = if (isGpsActive) Icons.Rounded.MyLocation else Icons.Rounded.LocationOn,
                                contentDescription = null,
                                tint = if (isGpsActive) MColors.Mint else Color(0xFF203B35),
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
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it.take(80) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MColors.Ink800),
                            decorationBox = { innerTextField ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "Saha, maç ara veya bölge değiştir",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 11.sp,
                                                color = MColors.Ink500
                                            )
                                        }
                                        innerTextField()
                                    }
                                    if (searchQuery.isNotEmpty()) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Temizle",
                                            tint = MColors.Ink400,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { searchQuery = "" }
                                        )
                                    }
                                }
                            }
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
