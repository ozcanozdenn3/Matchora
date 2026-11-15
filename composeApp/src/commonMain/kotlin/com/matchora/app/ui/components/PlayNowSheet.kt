package com.matchora.app.ui.components

import com.matchora.app.ui.components.premiumSurface
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.domain.EventStatus
import com.matchora.app.domain.Sport
import com.matchora.app.state.*
import com.matchora.app.ui.theme.MColors
import kotlin.random.Random

// ═══════════════════════════════════════════════════════════════════════════════
// PLAY NOW WIZARD & İLAN VERME ALANI (Strateji Dokümanı §04, §07, §28)
// 1) Hızlı Eşleş: Özel Tarih/Saat stepper ve filtreleri ile anında maç bul
// 2) İlan oluştur: Eksik oyuncu veya yeni karşılaşma ilanı oluştur
// ═══════════════════════════════════════════════════════════════════════════════

private fun formatTime(hour: Int, minute: Int): String {
    val hStr = if (hour < 10) "0$hour" else "$hour"
    val mStr = if (minute < 10) "0$minute" else "$minute"
    return "$hStr:$mStr"
}

private fun parseClockMinutes(value: String): Int? {
    val parts = value.split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

private enum class ListingIntent(val title: String) {
    NEW_MATCH("Yeni maç"),
    FIND_PLAYERS("Eksik oyuncu ara")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayNowModalSheet(
    onDismiss: () -> Unit,
    onSelectEvent: (SportEvent) -> Unit,
    onQuickJoin: (SportEvent) -> Unit,
    onToggleWaitlist: (SportEvent) -> Unit = {},
    onCreateEvent: (SportEvent) -> Unit = {},
    allEvents: List<SportEvent>,
    tokenBalance: Int = 0,
    joinedEventIds: Set<String> = emptySet(),
    waitlistedEventIds: Set<String> = emptySet(),
    pendingRequestEventIds: Set<String> = emptySet(),
    currentArea: String = "",
    currentCity: String = "İstanbul",
    currentCampus: String? = null,
    currentCompany: String? = null,
    eligibleCommunities: List<CommunityInfo>? = null,
    initialNetworkScope: EventNetworkScope = EventNetworkScope.CITY,
    initialVenue: String = "",
    initialTime: String? = null,
    newEventId: () -> String = { "draft_${Random.nextLong().toString(16)}" },
    onRequestTokens: () -> Unit = {}
) {
    var adStep by remember { mutableIntStateOf(if (initialVenue.isNotBlank()) 1 else 0) }
    var showSearchOptions by remember { mutableStateOf(false) }
    var activeTab by remember { mutableIntStateOf(if (initialVenue.isNotBlank()) 1 else 0) } // 0: Hızlı Eşleş, 1: İlan oluştur

    // ── Hızlı Eşleş Durumları ──
    var selectedSport by remember { mutableStateOf<Sport?>(Sport.FOOTBALL) }
    var searchCategory by remember { mutableStateOf<SportCategory?>(null) }
    var customDate by remember { mutableStateOf("Tümü") }
    var customHour by remember { mutableIntStateOf(19) }
    var customMinute by remember { mutableIntStateOf(0) }
    var selectedMode by remember { mutableStateOf<PlayMode?>(null) }
    var verifiedOnly by remember { mutableStateOf(false) }
    var maxDistanceKm by remember { mutableStateOf<Int?>(null) }

    // ── İlan Verme Durumları ──
    var adTitle by remember(initialVenue) { mutableStateOf(if (initialVenue.isNotBlank()) "$initialVenue Maçı" else "") }
    var adCategory by remember { mutableStateOf(SportCategory.PHYSICAL) }
    var adSport by remember { mutableStateOf(Sport.FOOTBALL) }
    var customSportName by remember { mutableStateOf("") }
    var customIsGaming by remember { mutableStateOf(false) }
    var adLocation by remember(currentArea, currentCity) { mutableStateOf(currentArea.ifBlank { currentCity }) }
    var adVenue by remember(initialVenue) { mutableStateOf(initialVenue) }
    var adPosition by remember { mutableStateOf("") }
    var adCostPerPlayer by remember { mutableStateOf("") }
    var adDescription by remember { mutableStateOf("") }
    var adFormat by remember { mutableStateOf("7v7") }
    var adLevelRange by remember { mutableStateOf("Orta – İleri") }
    var adDurationText by remember { mutableStateOf("60") }
    var adNetworkScope by remember { mutableStateOf(initialNetworkScope) }
    var adCommunityId by remember { mutableStateOf<String?>(null) }
    var adNetworkName by remember(currentCampus, currentCompany) {
        mutableStateOf("")
    }
    var adCalendarDate by remember { mutableStateOf(LocalDate.fromEpochDays(todayDate().toEpochDays() + 1)) }
    var adCity by remember(currentCity) { mutableStateOf(currentCity) }
    var adPoint by remember { mutableStateOf<EventGeoPoint?>(null) }
    var showAdMap by remember { mutableStateOf(false) }
    var showAdRegion by remember { mutableStateOf(false) }
    val parsedInitialHour = remember(initialTime) { initialTime?.substringBefore(':')?.trim()?.toIntOrNull() }
    val parsedInitialMinute = remember(initialTime) { initialTime?.substringAfter(':')?.substringBefore(' ')?.trim()?.toIntOrNull() }
    var adHour by remember(initialTime) { mutableIntStateOf(parsedInitialHour ?: 20) }
    var adMinute by remember(initialTime) { mutableIntStateOf(parsedInitialMinute ?: 30) }
    var adMode by remember { mutableStateOf(PlayMode.SOCIAL) }
    var listingIntent by remember { mutableStateOf(ListingIntent.FIND_PLAYERS) }
    var adVerifiedOnly by remember { mutableStateOf(false) }
    var adMinReliability by remember { mutableIntStateOf(0) }
    var adApprovalRequired by remember { mutableStateOf(true) }
    var adFormError by remember { mutableStateOf<String?>(null) }

    // E-Spor & Akıl Oyunları Özelleştirmeleri
    var esPlatform by remember { mutableStateOf("🖥️ PC") }
    var esServer by remember { mutableStateOf("") }
    var esVoiceComm by remember { mutableStateOf("") }
    var esGamerTag by remember { mutableStateOf("") }
    var esAdType by remember { mutableStateOf("5v5") }
    var esNeededRole by remember { mutableStateOf("") }
    var urgentPlayerFill by remember { mutableStateOf(false) }

    val backendMode = LocalBackendRepository.current?.enabled == true
    val deviceLocation by DeviceLocationState.current.collectAsState()
    var locationError by remember { mutableStateOf<String?>(null) }
    var locationSettings by remember { mutableStateOf(false) }
    val requestSearchLocation = rememberLocationRequester { result ->
        when (result) {
            is LocationOutcome.Located -> { DeviceLocationState.current.value = result.value; locationError = null; locationSettings = false }
            is LocationOutcome.Failed -> { DeviceLocationState.current.value = null; locationError = result.message; locationSettings = result.settingsAvailable }
        }
    }
    val regionCenter = rememberRegionCenter(currentCity, currentArea)
    val origin = deviceLocation ?: regionCenter
    fun effectiveDistanceKm(event: SportEvent): Float? =
        origin?.let { ref -> event.approximateLocation?.let { pt -> approximateDistanceKm(ref, pt.latitude, pt.longitude) } }
            ?: event.distanceKm
    val filteredEvents = remember(backendMode, currentCity, currentArea, origin, allEvents, searchCategory, selectedSport, selectedMode, verifiedOnly, maxDistanceKm, customDate, customHour, customMinute) {
        val selectedClock = customHour * 60 + customMinute
        allEvents.filter { event ->
            (!backendMode || matchesTurkishPlace(event.city, currentCity)) &&
            event.status in setOf(EventStatus.PUBLISHED, EventStatus.FILLING, EventStatus.FULL) &&
            (searchCategory == null || event.sport.category == searchCategory) &&
            (selectedSport == null || event.sport == selectedSport) &&
            (selectedMode == null || event.mode == selectedMode) &&
            (!verifiedOnly || event.verifiedOnly) &&
            (maxDistanceKm?.toFloat()?.let { limit -> effectiveDistanceKm(event)?.let { distance -> distance <= limit } == true } ?: true) &&
            event.matchesDayFilter(customDate) &&
            (customDate == "Tümü" || (parseClockMinutes(event.time)?.let { it >= selectedClock } ?: false))
        }.sortedWith(compareByDescending<SportEvent> { it.freeSpots > 0 }.thenByDescending { it.score.total })
    }

    ModalBottomSheet(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        scrimColor = androidx.compose.ui.graphics.Color(0xFF0B1220).copy(alpha = .48f),

        onDismissRequest = onDismiss,
        containerColor = MColors.Canvas,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { MPremiumSheetHandle() }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Başlık & İkili Sekme Seçici
            item {
                MSheetHeading(if (activeTab == 0) "Şimdi oyna" else "İlan oluştur",
                    if (activeTab == 0) "Sana uygun buluşmayı keşfet." else "Oyunu başlat. Ekibini tamamla.",
                    Icons.Rounded.Sports, onDismiss)
                Spacer(Modifier.height(12.dp))
                MSegmentedControl(listOf("Maç bul", "İlan ver"), activeTab, { activeTab = it })
            }

            if (activeTab == 0) {
                // ═══════════════════════════════════════════════════════════════
                // SEKME 0: HIZLI EŞLEŞ (ÖZEL TARİH & SAAT SEÇİCİ İLE)
                // ═══════════════════════════════════════════════════════════════
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color.White,
                        shadowElevation = 4.dp,
                        border = BorderStroke(1.dp, Color(0xFFE6EBF2)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SportPickerField(selectedSport, { selectedSport = it; searchCategory = null })

                            Surface(
                                onClick = { showSearchOptions = !showSearchOptions },
                                shape = RoundedCornerShape(13.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Rounded.Tune, null, tint = MColors.BrandDeep, modifier = Modifier.size(17.dp))
                                        Text(
                                            text = if (showSearchOptions) "Filtreleri gizle" else "Zaman ve oyun tercihleri",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MColors.Ink900
                                        )
                                    }
                                    Icon(
                                        imageVector = if (showSearchOptions) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = MColors.Ink500,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            if (showSearchOptions) {
                                MFloatingDropdownBox(
                                    label = "En Fazla Uzaklık",
                                    selectedValue = maxDistanceKm,
                                    options = listOf(null, 5, 10, 25),
                                    optionLabel = { km -> km?.let { "≤ $it km" } ?: "Farketmez" },
                                    onSelect = { maxDistanceKm = it },
                                    icon = Icons.Rounded.NearMe
                                )
                                if (backendMode && maxDistanceKm != null && deviceLocation == null) {
                                    Text("Uzaklığa göre eşleşmek için cihaz konumunu paylaş. Konumu olmayan ilanlar bu filtrede gösterilmez.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
                                    TextButton(onClick = requestSearchLocation) { Text("Konumumu kullan") }
                                    locationError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MColors.Rose) }
                                    if (locationSettings) TextButton(onClick = ::openApplicationSettings) { Text("Cihaz ayarlarını aç") }
                                }

                                MFloatingDropdownBox(
                                    label = "Oyun Modu",
                                    selectedValue = selectedMode,
                                    options = listOf(null, PlayMode.SOCIAL, PlayMode.COMPETITIVE),
                                    optionLabel = { mode ->
                                        when (mode) {
                                            null -> "Tümü"
                                            PlayMode.SOCIAL -> "Sosyal"
                                            PlayMode.COMPETITIVE -> "Rekabetçi"
                                        }
                                    },
                                    onSelect = { selectedMode = it },
                                    icon = Icons.Rounded.EmojiEvents
                                )

                                MFloatingDropdownBox(
                                    label = "Katılımcı Filtresi",
                                    selectedValue = verifiedOnly,
                                    options = listOf(false, true),
                                    optionLabel = { if (it) "Yalnızca Doğrulanmış" else "Tüm Sporcular" },
                                    onSelect = { verifiedOnly = it },
                                    icon = Icons.Rounded.VerifiedUser
                                )

                                MFloatingDropdownBox(
                                    label = "Maç Günü",
                                    selectedValue = customDate,
                                    options = listOf("Tümü", "Bugün", "Yarın", "Cumartesi", "Pazar"),
                                    optionLabel = { it },
                                    onSelect = { customDate = it },
                                    icon = Icons.Rounded.CalendarMonth
                                )

                                val timeSlots = remember {
                                    (9..23).flatMap { h -> listOf(0, 15, 30, 45).map { m -> h to m } }
                                }
                                MFloatingDropdownBox(
                                    label = "Başlangıç Saati",
                                    selectedValue = customHour to customMinute,
                                    options = timeSlots,
                                    optionLabel = { (h, m) -> "${formatTime(h, m)} sonrası" },
                                    onSelect = { (h, m) ->
                                        customHour = h
                                        customMinute = m
                                    },
                                    icon = Icons.Rounded.Schedule
                                )
                            }
                        }
                    }
                }

                // Bulunan Maçlar
                item {
                    Text(
                        text = "UYGUN MAÇLAR (${filteredEvents.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MColors.Ink400,
                        letterSpacing = 1.sp
                    )
                }

                items(filteredEvents) { event ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 1.dp,
                        border = BorderStroke(1.dp, if (event.emergency) MColors.Brand else Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clickable { onSelectEvent(event) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(13.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(11.dp),
                                    color = MColors.BrandTint,
                                    shadowElevation = 2.dp,
                                    border = BorderStroke(1.dp, MColors.Brand.copy(alpha = 0.2f)),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(event.sport.emoji, fontSize = 20.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = event.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MColors.Ink900,
                                        maxLines = 1
                                    )
                                    val dist = effectiveDistanceKm(event)
                                    Text(
                                        text = "${event.venue}${dist?.let { " · $it km" }.orEmpty()} · ${event.time}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = MColors.Ink500
                                    )
                                }
                            }

                            val alreadyJoined = joinedEventIds.contains(event.id)
                            val alreadyPending = pendingRequestEventIds.contains(event.id)
                            val alreadyWaiting = waitlistedEventIds.contains(event.id)
                            val isHost = event.hostId == DemoData.ME
                            val canJoin = event.freeSpots > 0 && event.status in setOf(EventStatus.PUBLISHED, EventStatus.FILLING)
                            val canWaitlist = event.status == EventStatus.FULL
                            val canAct = !isHost && !alreadyJoined && !alreadyPending && (alreadyWaiting || canJoin || canWaitlist)
                            Button(
                                onClick = {
                                    when {
                                        alreadyWaiting || (!canJoin && canWaitlist) -> onToggleWaitlist(event)
                                        canJoin -> onQuickJoin(event)
                                        else -> onSelectEvent(event)
                                    }
                                },
                                enabled = canAct,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (canAct) MColors.Brand else MColors.SurfaceSunken,
                                    contentColor = if (canAct) Color.White else MColors.Ink500
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(
                                    when {
                                        isHost -> "İlanın"
                                        alreadyJoined -> "Katıldın"
                                        alreadyPending -> "Bekliyor"
                                        alreadyWaiting -> "Listeden çık"
                                        canWaitlist -> "Bekleme"
                                        event.approvalRequired -> "Başvur"
                                        canJoin -> "Katıl"
                                        else -> "Kapalı"
                                    },
                                    color = if (canAct) Color.White else MColors.Ink500,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (filteredEvents.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            shadowElevation = 1.dp,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Rounded.SearchOff, null, tint = MColors.Ink400, modifier = Modifier.size(28.dp))
                                Text("Uygun karşılaşma bulunamadı", fontWeight = FontWeight.Bold, color = MColors.Ink900)
                                Text("Filtreleri değiştirip yeniden deneyebilirsin.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
                                TextButton(onClick = {
                                    searchCategory = null
                                    selectedSport = null
                                    selectedMode = null
                                    verifiedOnly = false
                                    maxDistanceKm = null
                                    customDate = "Tümü"
                                }) { Text("Filtreleri temizle") }
                            }
                        }
                    }
                }
            } else {
                // ═══════════════════════════════════════════════════════════════
                // SEKME 1: 📢 İLAN VER / KADRO AÇ (KULLANICI İLANI)
                // ═══════════════════════════════════════════════════════════════
                item {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        shadowElevation = 4.dp,
                        border = BorderStroke(1.dp, Color(0xFFE6EBF2)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // İlan Tipi & Güvence Kartı
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                shadowElevation = 1.dp,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(11.dp),
                                            color = MColors.BrandTint,
                                            border = BorderStroke(1.dp, MColors.Brand.copy(alpha = 0.22f)),
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Rounded.Campaign, null, tint = MColors.BrandDeep, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Yeni karşılaşma", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = MColors.Ink900)
                                            Text("Katılım güvencesi · 10 TKN", style = MaterialTheme.typography.labelSmall, color = MColors.Ink500)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFECFDF5),
                                            border = BorderStroke(1.dp, MColors.Mint.copy(alpha = 0.35f))
                                        ) {
                                            Text(
                                                text = "Güvenceli",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MColors.Mint,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    MSegmentPillBar(
                                        options = listOf(
                                            MSegmentPillOption(ListingIntent.NEW_MATCH, ListingIntent.NEW_MATCH.title, Icons.Rounded.Sports),
                                            MSegmentPillOption(ListingIntent.FIND_PLAYERS, ListingIntent.FIND_PLAYERS.title, Icons.Rounded.PersonAdd)
                                        ),
                                        selectedValue = listingIntent,
                                        onSelect = { listingIntent = it }
                                    )
                                }
                            }

                            MSegmentedControl(listOf("1 · Branş", "2 · Buluşma", "3 · Tercihler"), adStep, { adStep = it })
                            if (adStep == 0) {
                            // İlan Başlığı (Yüzen Premium Kutu)
                            MFloatingInputBox(
                                label = "İlan Başlığı",
                                value = adTitle,
                                onValueChange = { adTitle = it; adFormError = null },
                                placeholder = "Örn. Akşam maçı · orta saha aranıyor",
                                icon = Icons.Rounded.Campaign,
                                badgeText = "Zorunlu",
                                singleLine = true,
                                isError = adFormError != null && adTitle.isBlank(),
                                suggestions = listOf(
                                    "$adLocation akşam maçı · Orta saha aranıyor",
                                    "$adLocation dostluk maçı · 2 eksik",
                                    "Hafta sonu kondisyon maçı"
                                )
                            )

                            if (adFormError != null) {
                                Text(adFormError!!, color = MColors.Rose, style = MaterialTheme.typography.bodySmall)
                            }

                            Text("İlanın yayınlanacağı ağ", style = MaterialTheme.typography.labelLarge, color = MColors.Ink900)
                            MSegmentPillBar(
                                options = listOf(
                                    MSegmentPillOption(EventNetworkScope.CITY, "Saha", Icons.Rounded.Stadium, weight = 0.9f),
                                    MSegmentPillOption(EventNetworkScope.CAMPUS, "Kampüs", Icons.Rounded.School, weight = 1.05f),
                                    MSegmentPillOption(EventNetworkScope.COMPANY, "Şirket", Icons.Rounded.Business, weight = 1f),
                                    MSegmentPillOption(EventNetworkScope.TRAVEL, "Seyahat", Icons.Rounded.FlightTakeoff, weight = 1.05f)
                                ),
                                selectedValue = adNetworkScope,
                                onSelect = { scope ->
                                    adNetworkScope = scope
                                    adCommunityId = null
                                    adNetworkName = ""
                                    adFormError = null
                                },
                                fontSizeSp = 11f
                            )
                            Text(when (adNetworkScope) {
                                EventNetworkScope.CITY -> "Seçtiğin şehirdeki sporculara açık tek buluşma ilanı."
                                EventNetworkScope.CAMPUS, EventNetworkScope.COMPANY -> "Yeni topluluk oluşturmaz; üye olduğun topluluğa tek buluşma ilanı yayınlar."
                                EventNetworkScope.TRAVEL -> "Gideceğin şehirde sporculara açık buluşma ilanı."
                            }, style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)

                            if (adNetworkScope != EventNetworkScope.CITY) {
                                val restrictedNetwork = eligibleCommunities != null && adNetworkScope in listOf(EventNetworkScope.CAMPUS, EventNetworkScope.COMPANY)
                                val approvedNetworks = eligibleCommunities.orEmpty().filter { if (adNetworkScope == EventNetworkScope.CAMPUS) it.type == CommunityType.CAMPUS else it.type == CommunityType.COMPANY }
                                val networkFieldLabel = when (adNetworkScope) {
                                    EventNetworkScope.CAMPUS -> "Kampüs / Okul Adı"
                                    EventNetworkScope.COMPANY -> "Şirket / Kurum Adı"
                                    EventNetworkScope.TRAVEL -> "Gidilecek Şehir"
                                    EventNetworkScope.CITY -> "Ağ"
                                }
                                if (restrictedNetwork) {
                                    if (approvedNetworks.isEmpty()) Text("Bu ağda ilan açmak için önce bir topluluğa katılıp onay almalısın.", color = MColors.Ink500, style = MaterialTheme.typography.bodySmall)
                                    else MFloatingDropdownBox(label = networkFieldLabel, selectedValue = adCommunityId.orEmpty(),
                                        options = approvedNetworks.map { it.id }, optionLabel = { id -> approvedNetworks.firstOrNull { it.id == id }?.name ?: "Ağ seç" },
                                        onSelect = { id -> adCommunityId = id; adNetworkName = approvedNetworks.first { it.id == id }.name }, icon = Icons.Rounded.Hub)
                                } else if (adNetworkScope == EventNetworkScope.TRAVEL) {
                                    MFloatingActionRow(label = "Gidilecek şehir ve bölge", valueText = "$adCity · $adLocation", icon = Icons.Rounded.Flight, onClick = { showAdRegion = true })
                                } else MFloatingInputBox(
                                    label = networkFieldLabel,
                                    value = adNetworkName,
                                    onValueChange = { adNetworkName = it; adFormError = null },
                                    placeholder = "Ağ adını gir",
                                    icon = Icons.Rounded.Hub
                                )
                            }


                            // Kısa Açıklama Kutusu (Yüzen Premium Açıklama Kartı)
                            MFloatingInputBox(
                                label = "Kısa Açıklama",
                                value = adDescription,
                                onValueChange = { adDescription = it },
                                icon = Icons.Rounded.EditNote,
                                singleLine = false,
                                minLines = 2,
                                maxLines = 3
                            )

                            // Kategori Seçici: [🏃‍♂️ Fiziksel] [🎮 E-Spor] [✨ Özel Branş]
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                shadowElevation = 1.dp,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("BRANŞ KATEGORİSİ", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MColors.Ink500)
                                Spacer(modifier = Modifier.height(6.dp))
                                MSegmentPillBar(
                                    options = listOf(
                                        MSegmentPillOption(SportCategory.PHYSICAL, "Fiziksel", emoji = "🏃‍♂️"),
                                        MSegmentPillOption(SportCategory.ESPORTS, "E-Spor", emoji = "🎮"),
                                        MSegmentPillOption(SportCategory.CUSTOM, "Özel Branş", emoji = "✨", weight = 1.15f)
                                    ),
                                    selectedValue = adCategory,
                                    onSelect = { cat ->
                                        adCategory = cat
                                        if (cat == SportCategory.PHYSICAL && adSport.category != SportCategory.PHYSICAL) {
                                            adSport = Sport.FOOTBALL
                                            adFormat = defaultFormatForSport(Sport.FOOTBALL)
                                        } else if (cat == SportCategory.ESPORTS && adSport.category != SportCategory.ESPORTS) {
                                            adSport = Sport.ESPORTS_FC
                                            esAdType = defaultFormatForSport(Sport.ESPORTS_FC)
                                        } else if (cat == SportCategory.CUSTOM) {
                                            adSport = Sport.CUSTOM
                                            adFormat = defaultFormatForSport(Sport.CUSTOM)
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                if (adCategory == SportCategory.CUSTOM || adSport == Sport.CUSTOM) {
                                    MFloatingInputBox(
                                        label = "Özel Branş / Oyun Adı",
                                        value = customSportName,
                                        onValueChange = {
                                            customSportName = it
                                            adFormError = null
                                        },
                                        placeholder = "Örn: Boks, Squash, Tırmanış, Dota 2, Karting...",
                                        icon = Icons.Rounded.AutoAwesome,
                                        badgeText = "Özel",
                                        singleLine = true
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    MFloatingDropdownBox(
                                        label = "Ortam Türü",
                                        selectedValue = customIsGaming,
                                        options = listOf(false, true),
                                        optionLabel = { if (it) "🎮 Online / Gaming" else "🏟️ Saha / Salon" },
                                        onSelect = {
                                            customIsGaming = it
                                            if (it) esAdType = defaultFormatForSport(Sport.ESPORTS_FC) else adFormat = defaultFormatForSport(Sport.CUSTOM)
                                        },
                                        icon = Icons.Rounded.Category
                                    )
                                } else {
                                    val currentList = if (adCategory == SportCategory.PHYSICAL) physicalSports else esportSports
                                    SportPickerField(adSport, { selected ->
                                        if (selected != null) {
                                            adSport = selected
                                            adFormat = defaultFormatForSport(selected)
                                            esAdType = defaultFormatForSport(selected)
                                            esNeededRole = if (selected.isTeamSport) "Oyuncu" else "Rakip"
                                        }
                                    }, sports = currentList, allowAll = false)
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                            }

                            }
                            val isGaming = (adSport.isESport || adSport == Sport.CHESS) || (adSport == Sport.CUSTOM && customIsGaming)

                            if (adStep == 1) {
                                EventDateTimeField(adCalendarDate, adHour, adMinute) { date, hour, minute ->
                                    adCalendarDate = date; adHour = hour; adMinute = minute; adFormError = null
                                }
                            if (isGaming) {
                                // ── E-SPOR & GAMING AYARLARI ──
                                MFloatingDropdownBox(
                                    label = "Oyun Platformu",
                                    selectedValue = esPlatform,
                                    options = listOf("🖥️ PC", "🎮 PS5", "🟩 Xbox", "📱 Mobil", "🌐 Crossplay"),
                                    optionLabel = { it },
                                    onSelect = { esPlatform = it },
                                    icon = Icons.Rounded.SportsEsports
                                )

                                MFloatingDropdownBox(
                                    label = "Maç Formatı",
                                    selectedValue = esAdType,
                                    options = formatsForSport(adSport),
                                    optionLabel = { it },
                                    onSelect = { esAdType = it },
                                    icon = Icons.Rounded.Groups
                                )

                                MFloatingInputBox(
                                    label = "Sunucu & Bölge",
                                    value = esServer,
                                    onValueChange = { esServer = it; adFormError = null },
                                    placeholder = "TR / EU West",
                                    icon = Icons.Rounded.Dns,
                                    suggestions = listOf("TR Sunucusu", "EU West", "Frankfurt")
                                )

                                MFloatingInputBox(
                                    label = "Sesli İletişim",
                                    value = esVoiceComm,
                                    onValueChange = { esVoiceComm = it },
                                    placeholder = "Discord / oyun içi ses",
                                    icon = Icons.Rounded.HeadsetMic,
                                    badgeText = "Lobi"
                                )

                                MFloatingInputBox(
                                    label = "Oyun İçi ID / GamerTag",
                                    value = esGamerTag,
                                    onValueChange = { esGamerTag = it },
                                    placeholder = "Örn: Oyuncu#TR1",
                                    icon = Icons.Rounded.Badge,
                                    badgeText = "ID"
                                )

                                if (listingIntent == ListingIntent.FIND_PLAYERS) {
                                    MFloatingInputBox(
                                        label = "Aranan Rol / Oyuncu",
                                        value = esNeededRole,
                                        onValueChange = { esNeededRole = it; adFormError = null },
                                        placeholder = "Örn. Düellocu, destek",
                                        icon = Icons.Rounded.PersonSearch,
                                        badgeText = "Zorunlu"
                                    )
                                }
                            } else {
                                // ── KLASİK SPOR ALANLARI ──
                                MFloatingDropdownBox(
                                    label = "Maç Formatı",
                                    selectedValue = adFormat,
                                    options = formatsForSport(adSport),
                                    optionLabel = { it },
                                    onSelect = { adFormat = it },
                                    icon = Icons.Rounded.Groups
                                )

                                MFloatingActionRow(
                                    label = "Şehir ve Bölge",
                                    valueText = "$adCity · $adLocation",
                                    icon = Icons.Rounded.LocationCity,
                                    onClick = { showAdRegion = true }
                                )

                                MFloatingActionRow(
                                    label = "Harita Konumu",
                                    valueText = if (adPoint == null) "Haritadan Seç" else "Konum Seçildi",
                                    icon = if (adPoint == null) Icons.Rounded.AddLocationAlt else Icons.Rounded.WhereToVote,
                                    onClick = { showAdMap = true }
                                )

                                // Tesis / Lokasyon (Yüzen Premium Kutu)
                                MFloatingInputBox(
                                    label = "Tesis / Saha Adı",
                                    value = adVenue,
                                    onValueChange = { adVenue = it; adFormError = null },
                                    placeholder = "Saha adı veya açık adres",
                                    icon = Icons.Rounded.Stadium,
                                    badgeText = "Konum",
                                    suggestions = listOf(
                                        "$adLocation Merkez Saha",
                                        "$adCity Spor Kompleksi",
                                        "$adLocation Açık Kort"
                                    )
                                )

                                // Aranan Pozisyon / Rol (Yüzen Premium Kutu)
                                MFloatingInputBox(
                                    label = "Aranan Pozisyon / Rol",
                                    value = adPosition,
                                    onValueChange = { adPosition = it; adFormError = null },
                                    placeholder = "İstediğin pozisyonu veya rolü yaz",
                                    icon = Icons.Rounded.PersonSearch,
                                    badgeText = "Kadro"
                                )
                            }

                            }
                            if (adStep == 2) {
                            if (listingIntent == ListingIntent.FIND_PLAYERS) {
                                MFloatingDropdownBox(
                                    label = "Çağrı Önceliği",
                                    selectedValue = urgentPlayerFill,
                                    options = listOf(false, true),
                                    optionLabel = { if (it) "Acil Oyuncu Çağrısı" else "Standart Çağrı" },
                                    onSelect = { urgentPlayerFill = it },
                                    icon = Icons.Rounded.NotificationsActive
                                )
                            }

                            MFloatingDropdownBox(
                                label = "Seviye Aralığı",
                                selectedValue = adLevelRange,
                                options = listOf("Tüm seviyeler", "Başlangıç – Orta", "Orta – İleri", "İleri"),
                                optionLabel = { it },
                                onSelect = { adLevelRange = it },
                                icon = Icons.Rounded.Leaderboard
                            )

                            val parsedDurationMin = adDurationText.toIntOrNull()
                            MFloatingInputBox(
                                label = "Maç Süresi (Dakika)",
                                value = adDurationText,
                                onValueChange = { raw ->
                                    adDurationText = raw.filter(Char::isDigit).take(3)
                                    adFormError = null
                                },
                                placeholder = "İstediğin süreyi dakika olarak gir (15–720)",
                                icon = Icons.Rounded.Timer,
                                badgeText = parsedDurationMin?.let { "$it dk" } ?: "Dakika",
                                isError = adFormError != null && (parsedDurationMin == null || parsedDurationMin !in 15..720)
                            )

                            MFloatingDropdownBox(
                                label = "Oyun Modu",
                                selectedValue = adMode,
                                options = PlayMode.entries,
                                optionLabel = { mode ->
                                    when (mode) {
                                        PlayMode.SOCIAL -> "Sosyal"
                                        PlayMode.COMPETITIVE -> "Rekabetçi"
                                    }
                                },
                                onSelect = { adMode = it },
                                icon = Icons.Rounded.EmojiEvents
                            )

                            val trustOptions = remember {
                                listOf(
                                    false to 0,
                                    false to 50,
                                    false to 60,
                                    false to 70,
                                    false to 80,
                                    false to 90,
                                    true to 0,
                                    true to 50,
                                    true to 60,
                                    true to 70,
                                    true to 75,
                                    true to 80,
                                    true to 85,
                                    true to 90,
                                    true to 95,
                                    true to 100
                                )
                            }
                            val currentTrustSelection = (adVerifiedOnly to adMinReliability).let { current ->
                                if (current in trustOptions) current else (false to 0)
                            }
                            MFloatingDropdownBox(
                                label = "Katılımcı Güveni",
                                selectedValue = currentTrustSelection,
                                options = trustOptions,
                                optionLabel = { (verified, minRel) ->
                                    when {
                                        !verified && minRel == 0 -> "Tüm Sporcular"
                                        !verified -> "Min %$minRel Güven"
                                        minRel == 0 -> "Doğrulanmış"
                                        minRel == 100 -> "Doğrulanmış · %100"
                                        else -> "Doğrulanmış · %$minRel+"
                                    }
                                },
                                onSelect = { (verified, minRel) ->
                                    adVerifiedOnly = verified
                                    adMinReliability = minRel
                                },
                                icon = Icons.Rounded.VerifiedUser
                            )

                            MFloatingDropdownBox("Katılım şekli", adApprovalRequired, listOf(true, false), { if (it) "Organizatör onayı ile" else "Doğrudan katılım" }, { adApprovalRequired = it }, icon = Icons.Rounded.HowToReg)

                            MFloatingInputBox(
                                label = "Kişi Başı Ücret (₺)",
                                value = adCostPerPlayer,
                                onValueChange = { raw ->
                                    adCostPerPlayer = raw.filter(Char::isDigit).take(6)
                                    adFormError = null
                                },
                                placeholder = "0 ücretsiz",
                                icon = Icons.Rounded.Toll,
                                badgeText = if (adCostPerPlayer == "0" || adCostPerPlayer.isBlank()) "Ücretsiz" else "₺$adCostPerPlayer"
                            )

                            MSubCard(onClick = { adStep = 1 }) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text("Buluşma Özeti", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = MColors.Ink900)
                                        Text("${eventDateLabel(adCalendarDate)} · ${formatTime(adHour, adMinute)} · ${adDurationText.ifBlank { "--" }} dk", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MColors.Ink700)
                                        Text(
                                            text = if (isGaming) "$esPlatform · ${esServer.ifBlank { "Sunucu seçilmedi" }}" else "$adCity · $adLocation${if (adVenue.isNotBlank()) " · $adVenue" else ""}",
                                            fontSize = 11.5.sp,
                                            color = MColors.Ink500,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MColors.BrandTint,
                                        border = BorderStroke(1.dp, MColors.Brand.copy(alpha = 0.25f)),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Rounded.Edit, null, tint = MColors.BrandDeep, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                            }
                        }
                    }
                }

                item {
                    if (adFormError != null) Text(adFormError!!, style = MaterialTheme.typography.bodyMedium, color = MColors.Rose)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (adStep > 0) MButton("Geri", { adStep-- }, isPrimary = false, modifier = Modifier.weight(1f))
                        if (adStep < 2) MButton("Devam et", { adStep++ }, modifier = Modifier.weight(1f))
                    }
                }
                if (adStep == 2) {
                // İlanı Yayınla Butonu
                item {
                    val isGaming = (adSport.isESport || adSport == Sport.CHESS) || (adSport == Sport.CUSTOM && customIsGaming)
                    val cleanTitle = adTitle.trim()
                    val cleanLocation = adLocation.trim()
                    val cleanVenue = adVenue.trim()
                    val cleanRole = if (isGaming) esNeededRole.trim() else adPosition.trim()
                    val cleanFormat = if (isGaming) esAdType.trim() else adFormat.trim()
                    val fee = adCostPerPlayer.ifBlank { "0" }.toIntOrNull()
                    val parsedDuration = adDurationText.toIntOrNull()
                    val eventCapacity = capacityForFormat(cleanFormat)
                    MButton(
                        text = if (tokenBalance < 10) "Token yükle · 10 TKN gerekli" else "İlanı yayınla · 10 TKN kilitle",
                        onClick = {
                            val error = when {
                                cleanTitle.isBlank() -> "İlan başlığını gir."
                                adSport == Sport.CUSTOM && customSportName.isBlank() -> "Özel branş adını gir."
                                isGaming && esServer.isBlank() -> "Oyun sunucusu veya bölgesini gir."
                                !isGaming && cleanLocation.isBlank() -> "Maç konumunu gir."
                                !isGaming && cleanVenue.isBlank() -> "Tesis veya saha bilgisini gir."
                                !isGaming && adPoint == null -> "Buluşma noktasını haritadan seç."
                                !isFutureEvent(adCalendarDate, adHour, adMinute) -> "Gelecekte bir tarih ve saat seç."
                                eligibleCommunities != null && adNetworkScope in listOf(EventNetworkScope.CAMPUS, EventNetworkScope.COMPANY) && adCommunityId == null -> "İlanın yayınlanacağı onaylı topluluğunu seç. Üyeliğin yoksa Ağ bölümünden katılım isteği gönder."
                                adNetworkScope in listOf(EventNetworkScope.CAMPUS, EventNetworkScope.COMPANY) && adNetworkName.isBlank() -> "Seçtiğin ağın adını gir."
                                listingIntent == ListingIntent.FIND_PLAYERS && cleanRole.isBlank() -> "Aranan rolü veya formatı gir."
                                parsedDuration == null || parsedDuration !in 15..720 -> "Maç süresi için 15 ile 720 dakika arasında bir değer gir."
                                fee == null -> "Kişi başı ücret için sayı gir; ücretsizse 0 yaz."
                                else -> null
                            }
                            if (error != null) {
                                adFormError = error
                            } else if (tokenBalance < 10) {
                                onRequestTokens()
                            } else {
                                val customName = if (adSport == Sport.CUSTOM) customSportName.trim() else null
                                val cleanNetworkName = if (adNetworkScope == EventNetworkScope.TRAVEL) adCity.trim() else adNetworkName.trim()
                                val newEvent = SportEvent(
                                    id = newEventId(),
                                    title = cleanTitle,
                                    sport = adSport,
                                    venue = if (isGaming) "$esPlatform · ${esServer.trim()}".trim() else cleanVenue,
                                    area = if (isGaming) "Online" else cleanLocation,
                                    day = eventDateLabel(adCalendarDate),
                                    startsAtEpochMillis = eventStartMillis(adCalendarDate, adHour, adMinute),
                                    city = adCity,
                                    approximateLocation = adPoint.takeUnless { isGaming },
                                    time = formatTime(adHour, adMinute),
                                    startsInMin = null,
                                    durationMin = parsedDuration!!,
                                    distanceKm = null,
                                    capacity = eventCapacity,
                                    participantIds = listOf(DemoData.ME),
                                    waitlistCount = 0,
                                    mode = adMode,
                                    levelRange = adLevelRange,
                                    minReliability = adMinReliability,
                                    verifiedOnly = adVerifiedOnly,
                                    pricePerPersonTry = fee!!,
                                    commitmentTokens = 10,
                                    hostId = DemoData.ME,
                                    status = if (listingIntent == ListingIntent.FIND_PLAYERS) {
                                        com.matchora.app.domain.EventStatus.FILLING
                                    } else {
                                        com.matchora.app.domain.EventStatus.PUBLISHED
                                    },
                                    score = ScoreBreakdown(0, 0, 0, 0, 0, emptyList()),
                                    neededRoles = cleanRole.split(',').map { it.trim() }.filter { it.isNotBlank() },
                                    emergency = listingIntent == ListingIntent.FIND_PLAYERS && urgentPlayerFill,
                                    description = adDescription.trim(),
                                    customSportName = customName,
                                    networkScope = adNetworkScope,
                                    networkName = cleanNetworkName.takeIf { adNetworkScope != EventNetworkScope.CITY },
                                    communityId = adCommunityId,
                                    approvalRequired = adApprovalRequired,
                                    listingType = if (listingIntent == ListingIntent.FIND_PLAYERS) EventListingType.SEEK_PLAYERS else EventListingType.OPEN_MATCH,
                                    format = cleanFormat,
                                    gamePlatform = esPlatform.takeIf { isGaming },
                                    gameServer = esServer.trim().takeIf { isGaming && it.isNotBlank() },
                                    voiceChannel = esVoiceComm.trim().takeIf { isGaming && it.isNotBlank() },
                                    hostGameTag = esGamerTag.trim().takeIf { isGaming && it.isNotBlank() }
                                )
                                onCreateEvent(newEvent)
                                onDismiss()
                            }
                        },
                        icon = Icons.Rounded.Campaign
                    )
                }
                }
            }
        }
    }
    if (showAdMap) EventLocationPicker(
        initialPoint = adPoint ?: if (backendMode) null else cityMapCenter(adCity), initialAddress = adVenue, city = adCity, district = adLocation,
        onDismiss = { showAdMap = false }, onConfirm = { point, address -> adPoint = point; if (adVenue.isBlank()) adVenue = address; adFormError = null })
    if (showAdRegion) CityAndLocationSelectorSheet(adCity, adLocation, false,
        onDismiss = { showAdRegion = false }, onSelectLocation = { city, district, _ ->
            adCity = city; adLocation = district; adPoint = null; adVenue = ""; showAdRegion = false; adFormError = null
        })

}
