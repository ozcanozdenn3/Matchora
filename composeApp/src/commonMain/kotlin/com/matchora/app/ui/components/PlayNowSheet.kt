package com.matchora.app.ui.components

import com.matchora.app.ui.components.premiumSurface
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
    onCreateEvent: (SportEvent) -> Unit = {},
    allEvents: List<SportEvent>,
    tokenBalance: Int = 0,
    joinedEventIds: Set<String> = emptySet(),
    currentArea: String = "",
    currentCity: String = "İstanbul",
    currentCampus: String? = null,
    currentCompany: String? = null,
    newEventId: () -> String = { "draft_${Random.nextLong().toString(16)}" },
    onRequestTokens: () -> Unit = {}
) {
    var adStep by remember { mutableIntStateOf(0) }
    var showSearchOptions by remember { mutableStateOf(false) }
    var activeTab by remember { mutableIntStateOf(0) } // 0: Hızlı Eşleş, 1: İlan oluştur

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
    var adTitle by remember { mutableStateOf("") }
    var adCategory by remember { mutableStateOf(SportCategory.PHYSICAL) }
    var adSport by remember { mutableStateOf(Sport.FOOTBALL) }
    var customSportName by remember { mutableStateOf("") }
    var customIsGaming by remember { mutableStateOf(false) }
    var adLocation by remember(currentArea) { mutableStateOf(currentArea.ifBlank { "Kadıköy" }) }
    var adVenue by remember { mutableStateOf("") }
    var adPosition by remember { mutableStateOf("") }
    var adCostPerPlayer by remember { mutableStateOf("") }
    var adDescription by remember { mutableStateOf("") }
    var adFormat by remember { mutableStateOf("7v7") }
    var adLevelRange by remember { mutableStateOf("Orta – İleri") }
    var adDurationMin by remember { mutableIntStateOf(60) }
    var adNetworkScope by remember { mutableStateOf(EventNetworkScope.CITY) }
    var adNetworkName by remember(currentCampus, currentCompany) {
        mutableStateOf("")
    }
    var adCalendarDate by remember { mutableStateOf(LocalDate.fromEpochDays(todayDate().toEpochDays() + 1)) }
    var adCity by remember(currentCity) { mutableStateOf(currentCity) }
    var adPoint by remember { mutableStateOf<EventGeoPoint?>(null) }
    var showAdMap by remember { mutableStateOf(false) }
    var showAdRegion by remember { mutableStateOf(false) }
    var adHour by remember { mutableIntStateOf(20) }
    var adMinute by remember { mutableIntStateOf(30) }
    var adMode by remember { mutableStateOf(PlayMode.SOCIAL) }
    var listingIntent by remember { mutableStateOf(ListingIntent.FIND_PLAYERS) }
    var adVerifiedOnly by remember { mutableStateOf(false) }
    var adMinReliability by remember { mutableIntStateOf(0) }
    var adFormError by remember { mutableStateOf<String?>(null) }

    // E-Spor & Akıl Oyunları Özelleştirmeleri
    var esPlatform by remember { mutableStateOf("🖥️ PC") }
    var esServer by remember { mutableStateOf("") }
    var esVoiceComm by remember { mutableStateOf("") }
    var esGamerTag by remember { mutableStateOf("") }
    var esAdType by remember { mutableStateOf("5v5") }
    var esNeededRole by remember { mutableStateOf("") }
    var urgentPlayerFill by remember { mutableStateOf(false) }

    val filteredEvents = remember(allEvents, searchCategory, selectedSport, selectedMode, verifiedOnly, maxDistanceKm, customDate, customHour, customMinute) {
        val selectedClock = customHour * 60 + customMinute
        allEvents.filter { event ->
            event.status in setOf(EventStatus.PUBLISHED, EventStatus.FILLING) && event.freeSpots > 0 &&
            (searchCategory == null || event.sport.category == searchCategory) &&
            (selectedSport == null || event.sport == selectedSport) &&
            (selectedMode == null || event.mode == selectedMode) &&
            (!verifiedOnly || event.verifiedOnly) &&
            (maxDistanceKm?.toFloat()?.let { limit -> event.distanceKm?.let { distance -> distance <= limit } == true } ?: true) &&
            event.matchesDayFilter(customDate) &&
            (customDate == "Tümü" || (parseClockMinutes(event.time)?.let { it >= selectedClock } ?: false))
        }.sortedByDescending { it.score.total }
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
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                shadowElevation = 3.dp,
                                border = BorderStroke(1.dp, Color(0xFFE4E9F2)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(
                                        elevation = 5.dp,
                                        shape = RoundedCornerShape(12.dp),
                                        ambientColor = Color(0x220B1220),
                                        spotColor = Color(0x320B1220)
                                    )
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
                        shadowElevation = 5.dp,
                        border = BorderStroke(1.dp, if (event.emergency) MColors.Brand else Color(0xFFE4E9F2)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .shadow(
                                elevation = 7.dp,
                                shape = RoundedCornerShape(16.dp),
                                ambientColor = Color(0x280B1220),
                                spotColor = Color(0x380B1220)
                            )
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
                                    Text(
                                        text = "${event.venue} · ${event.time}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = MColors.Ink500
                                    )
                                }
                            }

                            Button(
                                onClick = { onQuickJoin(event) },
                                enabled = !joinedEventIds.contains(event.id),
                                colors = ButtonDefaults.buttonColors(containerColor = MColors.Brand),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(
                                    if (joinedEventIds.contains(event.id)) "Katıldın" else "Katıl",
                                    color = Color.White,
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
                            shadowElevation = 5.dp,
                            border = BorderStroke(1.dp, Color(0xFFE4E9F2)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = 7.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    ambientColor = Color(0x280B1220),
                                    spotColor = Color(0x380B1220)
                                )
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
                            // Yüzen İlan Tipi & Güvence Adası
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                shadowElevation = 5.dp,
                                border = BorderStroke(1.dp, Color(0xFFE4E9F2)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(
                                        elevation = 7.dp,
                                        shape = RoundedCornerShape(16.dp),
                                        ambientColor = Color(0x260B1220),
                                        spotColor = Color(0x360B1220)
                                    )
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
                                            shadowElevation = 2.dp,
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

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        ListingIntent.entries.forEach { intent ->
                                            val isSel = listingIntent == intent
                                            Surface(
                                                onClick = { listingIntent = intent },
                                                shape = RoundedCornerShape(11.dp),
                                                color = if (isSel) MColors.BrandTint else Color(0xFFF8FAFC),
                                                shadowElevation = if (isSel) 3.dp else 1.dp,
                                                border = BorderStroke(
                                                    width = if (isSel) 1.5.dp else 1.dp,
                                                    color = if (isSel) MColors.Brand.copy(alpha = 0.45f) else Color(0xFFE2E8F0)
                                                ),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                                                ) {
                                                    Text(
                                                        text = intent.title,
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                        color = if (isSel) MColors.BrandDeep else MColors.Ink700
                                                    )
                                                }
                                            }
                                        }
                                    }
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
                                    "Moda akşam maçı · Orta saha aranıyor",
                                    "Kadıköy dostluk maçı · 2 eksik",
                                    "Hafta sonu kondisyon maçı"
                                )
                            )

                            if (adFormError != null) {
                                Text(adFormError!!, color = MColors.Rose, style = MaterialTheme.typography.bodySmall)
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

                            // Kategori Seçici: [🏃‍♂️ Fiziksel Sporlar] [🎮 E-Spor & Gaming] [✨ Özel / Diğer Branş]
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                shadowElevation = 5.dp,
                                border = BorderStroke(1.dp, Color(0xFFE4E9F2)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .shadow(
                                        elevation = 7.dp,
                                        shape = RoundedCornerShape(16.dp),
                                        ambientColor = Color(0x280B1220),
                                        spotColor = Color(0x380B1220)
                                    )
                            ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("BRANŞ KATEGORİSİ", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MColors.Ink500)
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(13.dp),
                                    color = Color(0xFFF1F5F9), // Light Luxury Segmented Track
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(3.dp),
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        listOf(
                                            Triple(SportCategory.PHYSICAL, "🏃‍♂️", "Fiziksel"),
                                            Triple(SportCategory.ESPORTS, "🎮", "E-Spor"),
                                            Triple(SportCategory.CUSTOM, "✨", "Özel Branş")
                                        ).forEach { (cat, icon, label) ->
                                            val isSel = adCategory == cat
                                            val animatedBg by animateColorAsState(
                                                targetValue = if (isSel) Color.White else Color.Transparent,
                                                animationSpec = tween(180)
                                            )
                                            val animatedTextColor by animateColorAsState(
                                                targetValue = if (isSel) MColors.Brand else Color(0xFF64748B),
                                                animationSpec = tween(180)
                                            )

                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = animatedBg,
                                                border = if (isSel) BorderStroke(1.dp, Color(0x18000000)) else null,
                                                shadowElevation = if (isSel) 2.dp else 0.dp,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(36.dp)
                                                    .clickable {
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
                                            ) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.padding(horizontal = 4.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.Center
                                                    ) {
                                                        Text(icon, fontSize = 12.sp)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = label,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold,
                                                            color = animatedTextColor,
                                                            fontSize = 11.5.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

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
                                        onSelect = { customIsGaming = it },
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
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                shadowElevation = 5.dp,
                                border = BorderStroke(1.dp, Color(0xFFE4E9F2)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .shadow(
                                        elevation = 7.dp,
                                        shape = RoundedCornerShape(16.dp),
                                        ambientColor = Color(0x280B1220),
                                        spotColor = Color(0x380B1220)
                                    )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Tarih ve saat", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = MColors.Ink900)
                                    EventDateTimeField(adCalendarDate, adHour, adMinute) { date, hour, minute ->
                                        adCalendarDate = date; adHour = hour; adMinute = minute; adFormError = null
                                    }
                                }
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
                                    label = "Sesli İletişim & GamerTag",
                                    value = esVoiceComm,
                                    onValueChange = { esVoiceComm = it },
                                    placeholder = "Discord / oyun içi ses",
                                    icon = Icons.Rounded.HeadsetMic,
                                    badgeText = esGamerTag.ifBlank { "Lobi" }
                                )

                                if (listingIntent == ListingIntent.FIND_PLAYERS) {
                                    MFloatingInputBox(
                                        label = "Aranan Rol / Oyuncu",
                                        value = esNeededRole,
                                        onValueChange = { esNeededRole = it; adFormError = null },
                                        placeholder = "Örn. Düellocu, destek",
                                        icon = Icons.Rounded.PersonSearch,
                                        badgeText = "Zorunlu",
                                        suggestions = listOf("Düellocu", "Destek / Support", "Oyun Kurucu / IGL")
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
                                        "Moda Spor Parkı · Saha 1",
                                        "Kalamış Sahil Kortları",
                                        "Caddebostan Halı Saha"
                                    )
                                )

                                // Aranan Pozisyon / Rol (Yüzen Premium Kutu)
                                MFloatingInputBox(
                                    label = "Aranan Pozisyon / Rol",
                                    value = adPosition,
                                    onValueChange = { adPosition = it; adFormError = null },
                                    placeholder = "Örn. Kaleci, orta saha",
                                    icon = Icons.Rounded.PersonSearch,
                                    badgeText = "Kadro",
                                    suggestions = listOf("Orta saha", "Kaleci", "Defans", "Forvet", "Farketmez")
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

                            MFloatingDropdownBox(
                                label = "Maç Süresi",
                                selectedValue = adDurationMin,
                                options = listOf(60, 90, 120),
                                optionLabel = { "$it Dakika" },
                                onSelect = { adDurationMin = it },
                                icon = Icons.Rounded.Timer
                            )

                            MFloatingDropdownBox(
                                label = "Paylaşım Ağı",
                                selectedValue = adNetworkScope,
                                options = EventNetworkScope.entries,
                                optionLabel = { scope ->
                                    when (scope) {
                                        EventNetworkScope.CITY -> "Şehir"
                                        EventNetworkScope.CAMPUS -> "Kampüs"
                                        EventNetworkScope.COMPANY -> "Şirket"
                                        EventNetworkScope.TRAVEL -> "Seyahat"
                                    }
                                },
                                onSelect = { adNetworkScope = it },
                                icon = Icons.Rounded.Public
                            )

                            if (adNetworkScope != EventNetworkScope.CITY) {
                                val networkFieldLabel = when (adNetworkScope) {
                                    EventNetworkScope.CAMPUS -> "Kampüs / Okul Adı"
                                    EventNetworkScope.COMPANY -> "Şirket / Kurum Adı"
                                    EventNetworkScope.TRAVEL -> "Gidilecek Şehir"
                                    EventNetworkScope.CITY -> "Ağ"
                                }
                                MFloatingInputBox(
                                    label = networkFieldLabel,
                                    value = adNetworkName,
                                    onValueChange = { adNetworkName = it; adFormError = null },
                                    placeholder = "Ağ adını gir",
                                    icon = Icons.Rounded.Hub
                                )
                            }

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

                            val currentTrustSelection = if (!adVerifiedOnly) 0 else (if (adMinReliability in listOf(70, 80, 90)) adMinReliability else 80)
                            MFloatingDropdownBox(
                                label = "Katılımcı Güveni",
                                selectedValue = currentTrustSelection,
                                options = listOf(0, 70, 80, 90),
                                optionLabel = { level ->
                                    when (level) {
                                        0 -> "Tüm Sporcular"
                                        70 -> "Doğrulanmış · Min %70"
                                        80 -> "Doğrulanmış · Min %80"
                                        90 -> "Doğrulanmış · Min %90"
                                        else -> "Tüm Sporcular"
                                    }
                                },
                                onSelect = { level ->
                                    if (level == 0) {
                                        adVerifiedOnly = false
                                        adMinReliability = 0
                                    } else {
                                        adVerifiedOnly = true
                                        adMinReliability = level
                                    }
                                },
                                icon = Icons.Rounded.VerifiedUser
                            )

                            MFloatingInputBox(
                                label = "Kişi Başı Ücret (TKN)",
                                value = adCostPerPlayer,
                                onValueChange = { raw ->
                                    adCostPerPlayer = raw.filter(Char::isDigit).take(6)
                                    adFormError = null
                                },
                                placeholder = "0 ücretsiz",
                                icon = Icons.Rounded.Toll,
                                badgeText = if (adCostPerPlayer == "0" || adCostPerPlayer.isBlank()) "Ücretsiz" else "$adCostPerPlayer TKN",
                                suggestions = listOf("0", "120", "160", "200")
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
                                        Text("${eventDateLabel(adCalendarDate)} · ${formatTime(adHour, adMinute)} · $adDurationMin dk", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MColors.Ink700)
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
                        if (adStep > 0) OutlinedButton(onClick = { adStep-- }, modifier = Modifier.weight(1f)) { Text("Geri") }
                        if (adStep < 2) Button(onClick = { adStep++ }, modifier = Modifier.weight(1f)) { Text("Devam et") }
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
                    val eventCapacity = capacityForFormat(cleanFormat)
                    Button(
                        onClick = {
                            val error = when {
                                cleanTitle.isBlank() -> "İlan başlığını gir."
                                adSport == Sport.CUSTOM && customSportName.isBlank() -> "Özel branş adını gir."
                                isGaming && esServer.isBlank() -> "Oyun sunucusu veya bölgesini gir."
                                !isGaming && cleanLocation.isBlank() -> "Maç konumunu gir."
                                !isGaming && cleanVenue.isBlank() -> "Tesis veya saha bilgisini gir."
                                !isGaming && adPoint == null -> "Buluşma noktasını haritadan seç."
                                !isFutureEvent(adCalendarDate, adHour, adMinute) -> "Gelecekte bir tarih ve saat seç."
                                    adNetworkScope != EventNetworkScope.CITY && adNetworkName.isBlank() -> "Seçtiğin ağın adını gir."
                                listingIntent == ListingIntent.FIND_PLAYERS && cleanRole.isBlank() -> "Aranan rolü veya formatı gir."
                                fee == null -> "Kişi başı ücret için sayı gir; ücretsizse 0 yaz."
                                else -> null
                            }
                            if (error != null) {
                                adFormError = error
                            } else if (tokenBalance < 10) {
                                onRequestTokens()
                            } else {
                                val customName = if (adSport == Sport.CUSTOM) customSportName.trim() else null
                                val cleanNetworkName = adNetworkName.trim()
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
                                    durationMin = adDurationMin,
                                    distanceKm = null,
                                    capacity = eventCapacity,
                                    participantIds = listOf(DemoData.ME),
                                    waitlistCount = 0,
                                    mode = adMode,
                                    levelRange = adLevelRange,
                                    minReliability = if (adVerifiedOnly) adMinReliability else 0,
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
                                    neededRoles = if (listingIntent == ListingIntent.FIND_PLAYERS) listOf(cleanRole) else emptyList(),
                                    emergency = listingIntent == ListingIntent.FIND_PLAYERS && urgentPlayerFill,
                                    description = adDescription.trim(),
                                    customSportName = customName,
                                    networkScope = adNetworkScope,
                                    networkName = cleanNetworkName.takeIf { adNetworkScope != EventNetworkScope.CITY },
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
                        colors = ButtonDefaults.buttonColors(containerColor = MColors.Brand),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(Icons.Rounded.Campaign, null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (tokenBalance < 10) "Token yükle · 10 TKN gerekli" else "İlanı yayınla · 10 TKN kilitle",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                }
            }
        }
    }
    if (showAdMap) EventLocationPicker(
        initialPoint = adPoint ?: cityMapCenter(adCity), initialAddress = adVenue, city = adCity,
        onDismiss = { showAdMap = false }, onConfirm = { point, address -> adPoint = point; adVenue = address; adFormError = null })
    if (showAdRegion) CityAndLocationSelectorSheet(adCity, adLocation, false,
        onDismiss = { showAdRegion = false }, onSelectLocation = { city, district, _ ->
            adCity = city; adLocation = district; adPoint = null; adVenue = ""; showAdRegion = false; adFormError = null
        })

}
