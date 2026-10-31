package com.matchora.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.domain.*
import com.matchora.app.state.*
import com.matchora.app.ui.components.*
import com.matchora.app.ui.theme.MColors

// ═══════════════════════════════════════════════════════════════════════════════
// REDESIGNED EXECUTIVE HOME SCREEN (Farklı Kart Tipleri, Sıfır Metin Kalabalığı)
// 1) Canlı Radar & Branş Hub'ı (Keşif Kapsülü)
// 2) Hero Spotlight Match Card (Acil / Öne Çıkan Maç — Farklı Görsel Kimlik)
// 3) Sports Chemistry™ Taktiksel Kadro Kartı (Yapay Zeka Vitrini)
// 4) Bento Grid: Takım Challenge & Lig Sıralaması
// ═══════════════════════════════════════════════════════════════════════════════
// AĞ MODLARI (Strateji Belgesi Bölüm 14: Topluluk, Kampüs, Şirket ve Seyahat Modları)
// ═══════════════════════════════════════════════════════════════════════════════
enum class NetworkMode(val label: String, val icon: String, val badge: String) {
    CITY("Saha", "🏟️", "Kadıköy & Sahalar"),
    CAMPUS("Kampüs", "🎓", "İTÜ & Boğaziçi Ligleri"),
    COMPANY("Şirket", "🏢", "Kurumsal Wellness Ligi"),
    TRAVEL("Seyahat", "✈️", "Misafir Sporcu Pasaportu")
}

@Composable
fun HomeScreen(
    paddingValues: PaddingValues,
    events: List<SportEvent>,
    currentCity: String,
    onOpenLocation: () -> Unit,
    user: UserProfile,
    currentPlayer: Player,
    onSelectEvent: (SportEvent) -> Unit,
    onOpenMap: () -> Unit,
    onOpenPlayNow: () -> Unit,
    onOpenEmergencyFill: () -> Unit,
    onOpenAutoSquad: () -> Unit,
    onOpenTeamChallenge: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenProfile: () -> Unit
) {
    var selectedSportFilter by remember { mutableStateOf<Sport?>(null) }
    var activeNetworkMode by remember { mutableStateOf(NetworkMode.CITY) }
    var activeAiFilterLabel by remember { mutableStateOf<String?>(null) }
    var expandedEventIds by remember { mutableStateOf(setOf<String>()) }
    var showAutoSquadSheet by remember { mutableStateOf(false) }
    var showSmartMatchSheet by remember { mutableStateOf(false) }

    val scopedEvents = remember(events, activeNetworkMode, user.campus, user.company, currentCity) {
        events.filter { event ->
            event.status in setOf(EventStatus.PUBLISHED, EventStatus.FILLING, EventStatus.FULL) &&
            when (activeNetworkMode) {
                NetworkMode.CITY -> event.networkScope == EventNetworkScope.CITY && event.city.equals(currentCity, ignoreCase = true)
                NetworkMode.CAMPUS -> event.networkScope == EventNetworkScope.CAMPUS &&
                    !user.campus.isNullOrBlank() && event.networkName.equals(user.campus, ignoreCase = true)
                NetworkMode.COMPANY -> event.networkScope == EventNetworkScope.COMPANY &&
                    !user.company.isNullOrBlank() && event.networkName.equals(user.company, ignoreCase = true)
                NetworkMode.TRAVEL -> event.networkScope == EventNetworkScope.TRAVEL
            }
        }
    }
    val networkBadge = when (activeNetworkMode) {
        NetworkMode.CITY -> "$currentCity · spor buluşmaları"
        NetworkMode.CAMPUS -> user.campus?.let { "$it Kampüs Ligi" } ?: "Kampüs profili ekle"
        NetworkMode.COMPANY -> user.company?.let { "$it Ağı" } ?: "Şirket profili ekle"
        NetworkMode.TRAVEL -> "Seyahat etkinlikleri"
    }
    val missingNetworkProfile = when (activeNetworkMode) {
        NetworkMode.CAMPUS -> user.campus.isNullOrBlank()
        NetworkMode.COMPANY -> user.company.isNullOrBlank()
        else -> false
    }
    val filteredEvents = remember(scopedEvents, selectedSportFilter, activeAiFilterLabel) {
        scopedEvents.filter { event ->
            (selectedSportFilter == null || event.sport == selectedSportFilter) &&
            (activeAiFilterLabel == null || when (activeAiFilterLabel) {
                "FOOTBALL_TONIGHT" -> event.sport == Sport.FOOTBALL && event.matchesDayFilter("Bugün")
                "WEEKEND_RACKET" -> (event.sport == Sport.TENNIS || event.sport == Sport.PADEL) &&
                    (event.matchesDayFilter("Cumartesi") || event.matchesDayFilter("Pazar"))
                "URGENT_FILL" -> event.emergency && event.freeSpots > 0
                "TEAM_MATCH" -> event.isTeamEvent
                else -> true
            })
        }
    }
    val noMatchAfterFiltering = scopedEvents.isNotEmpty() && filteredEvents.isEmpty()
    val emergencyEvent = filteredEvents.firstOrNull { it.emergency }
    val featuredEvent = filteredEvents.firstOrNull { !it.emergency } ?: filteredEvents.firstOrNull()
    val activeMatch = emergencyEvent ?: featuredEvent
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
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 10.dp,
                bottom = paddingValues.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ═══════════════════════════════════════════════════════════════════
            // 0. AĞ KAPSAMI SEÇİCİ (Saha · Kampüs · Şirket · Seyahat)
            // ═══════════════════════════════════════════════════════════════════
            item {
                NetworkScopeIsland(activeNetworkMode, { activeNetworkMode = it })
            }

            // ═══════════════════════════════════════════════════════════════════
            // KART TİPİ 1: 📍 CANLI SPOR RADARI & BRANŞ KAPSÜLÜ
            // ═══════════════════════════════════════════════════════════════════
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFC4D8D2)),
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.background(Brush.linearGradient(listOf(Color(0xFFFEFFFC), Color(0xFFE3EFEA))))
                        .drawBehind {
                            val origin = Offset(size.width - 18.dp.toPx(), 30.dp.toPx())
                            listOf(24.dp, 42.dp, 60.dp).forEach { radius ->
                                drawCircle(Color(0xFF376B60).copy(alpha = .08f), radius.toPx(), origin, style = Stroke(.8.dp.toPx()))
                            }
                            drawLine(Color(0xFFB79154), Offset(14.dp.toPx(), 1.dp.toPx()), Offset(size.width - 14.dp.toPx(), 1.dp.toPx()), 2.dp.toPx())
                        }.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .shadow(6.dp, RoundedCornerShape(13.dp), ambientColor = Color(0x360B1220), spotColor = Color(0x480B1220))
                                    .background(Brush.linearGradient(listOf(Color(0xFF285B58), MColors.Ink900)), RoundedCornerShape(13.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Radar, null, tint = Color(0xFFADE6D3), modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(androidx.compose.ui.text.buildAnnotatedString {
                                    pushStyle(androidx.compose.ui.text.SpanStyle(color = Color(0xFF163D35), fontWeight = FontWeight.ExtraBold))
                                    append("Spor ")
                                    pop()
                                    pushStyle(androidx.compose.ui.text.SpanStyle(color = Color(0xFF8A6534), fontWeight = FontWeight.ExtraBold))
                                    append("radarı")
                                    pop()
                                }, style = MaterialTheme.typography.titleLarge)
                                Text(networkBadge, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = Color(0xFF597168), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Surface(
                                onClick = onOpenMap,
                                shape = RoundedCornerShape(11.dp),
                                color = Color.White,
                                shadowElevation = 4.dp,
                                border = BorderStroke(1.dp, Color(0xFFD2E2DB)),
                                modifier = Modifier.shadow(
                                    elevation = 6.dp,
                                    shape = RoundedCornerShape(11.dp),
                                    ambientColor = Color(0x2A0B1220),
                                    spotColor = Color(0x3A0B1220)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Haritayı aç", color = Color(0xFF23665D), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Rounded.ArrowOutward, null, tint = Color(0xFF23665D), modifier = Modifier.padding(start = 4.dp).size(15.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Mini Radar Harita Vitrini
                        LiveMiniRadarView(
                            events = filteredEvents,
                            scopeLabel = activeNetworkMode.label,
                            hasActiveSmartMatch = activeAiFilterLabel != null,
                            onOpenSmartMatch = { showSmartMatchSheet = true },
                            onOpenMap = onOpenMap
                        )

                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = onOpenLocation,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFD8E2EE)),
                                modifier = Modifier
                                    .heightIn(min = 58.dp)
                                    .shadow(
                                        elevation = 7.dp,
                                        shape = RoundedCornerShape(14.dp),
                                        ambientColor = Color(0x2C0B1220),
                                        spotColor = Color(0x3C0B1220)
                                    )
                            ) {
                                Icon(Icons.Rounded.LocationOn, null, tint = MColors.BrandDeep, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(currentCity, color = MColors.Ink800, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                            SportPickerField(selectedSportFilter, { selectedSportFilter = it }, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            if (filteredEvents.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, MColors.Line),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                when {
                                    missingNetworkProfile -> "Ağ profilini tamamla"
                                    noMatchAfterFiltering -> "Filtreyle eşleşen yok"
                                    else -> "Bu ağda henüz karşılaşma yok"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MColors.Ink900
                            )
                            Text(
                                when {
                                    missingNetworkProfile -> "Kampüs veya şirket bilgini pasaportuna ekleyince sana uygun ilanları gösteririz."
                                    noMatchAfterFiltering -> "Filtreleri temizle veya başka bir branş seç."
                                    else -> "Başka bir kapsam seçebilir veya yeni ilan açabilirsin."
                                },
                                fontSize = 11.sp,
                                color = MColors.Ink500
                            )
                            TextButton(
                                onClick = when {
                                    missingNetworkProfile -> onOpenProfile
                                    noMatchAfterFiltering -> {
                                        {
                                            selectedSportFilter = null
                                            activeAiFilterLabel = null
                                        }
                                    }
                                    else -> onOpenPlayNow
                                }
                            ) {
                                Text(
                                    when {
                                        missingNetworkProfile -> "Pasaportu tamamla"
                                        noMatchAfterFiltering -> "Filtreleri temizle"
                                        else -> "İlan oluştur"
                                    },
                                    color = MColors.Brand
                                )
                            }
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════════════
            // KART TİPİ 2: ⚡ SPOTLIGHT HERO MAÇ KARTI (TAMAMEN FARKLI TASARIM)
            // ═══════════════════════════════════════════════════════════════════
            if (activeMatch != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, if (activeMatch.emergency) Color(0xFFFF4D00).copy(alpha = 0.35f) else Color(0x18000000)),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectEvent(activeMatch) }
                    ) {
                        Column(
                            modifier = Modifier.feedCardFinish(Color(0xFFFFF5EA), if (activeMatch.emergency) MColors.BrandDeep else MColors.Sky, 2)
                        ) {
                            // Üst Vurgu Çubuğu
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.5.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(MColors.Brand, Color(0xFFF97316), Color(0xFFFBBF24))
                                        )
                                    )
                            )

                            Column(modifier = Modifier.padding(14.dp)) {
                                // Rozetler ve Zaman
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (activeMatch.emergency) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFFFFF1EA),
                                                shadowElevation = 3.dp,
                                                border = BorderStroke(0.8.dp, Color(0xFFFF4D00).copy(alpha = 0.4f)),
                                                modifier = Modifier.shadow(4.dp, RoundedCornerShape(8.dp), ambientColor = Color(0x280B1220), spotColor = Color(0x380B1220))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Box(Modifier.size(5.dp).background(Color(0xFFEF4444), CircleShape))
                                                    Text(
                                                        text = "ACİL · OYUNCU ARANIYOR",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = MColors.BrandDeep,
                                                        letterSpacing = 0.4.sp
                                                    )
                                                }
                                            }
                                        } else if (activeMatch.score.reasons.isNotEmpty()) {
                                            MChemistryBadge(percent = activeMatch.score.total, label = "Uyum")
                                        } else {
                                            Text("Yeni ilan", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MColors.Ink500)
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White,
                                        shadowElevation = 3.dp,
                                        border = BorderStroke(0.8.dp, Color(0xFFE2E8F0)),
                                        modifier = Modifier.shadow(4.dp, RoundedCornerShape(8.dp), ambientColor = Color(0x260B1220), spotColor = Color(0x360B1220))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Rounded.Schedule, null, tint = MColors.Ink600, modifier = Modifier.size(11.dp))
                                            Text(
                                                text = "${activeMatch.day} · ${activeMatch.time}",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MColors.Ink800,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Maç Başlığı & Tesis
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .shadow(6.dp, RoundedCornerShape(13.dp), ambientColor = Color(0x300B1220), spotColor = Color(0x420B1220))
                                            .background(
                                                Brush.linearGradient(listOf(Color(0xFFFFE8CC), Color(0xFFF5CFAD))),
                                                RoundedCornerShape(13.dp)
                                            )
                                            .border(1.dp, Color(0xFFFF4D00).copy(alpha = 0.18f), RoundedCornerShape(13.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(activeMatch.sport.emoji, fontSize = 22.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = activeMatch.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MColors.Ink900,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f, fill = false)
                                            ) {
                                                Icon(Icons.Rounded.LocationOn, null, tint = MColors.BrandDeep, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "${activeMatch.venue}${activeMatch.distanceKm?.let { " · $it km" }.orEmpty()}",
                                                    fontSize = 11.sp,
                                                    color = MColors.Ink600,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color.White,
                                                shadowElevation = 3.dp,
                                                border = BorderStroke(0.8.dp, Color(0xFFE2E8F0)),
                                                modifier = Modifier.shadow(4.dp, RoundedCornerShape(6.dp), ambientColor = Color(0x240B1220), spotColor = Color(0x340B1220))
                                            ) {
                                                Text(
                                                    text = "TKN ${activeMatch.pricePerPersonTry}",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MColors.Ink800,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = Color(0xFFEEDAC9), thickness = 1.dp)
                                Spacer(modifier = Modifier.height(9.dp))

                                // Alt Aksiyon: Boş Yerler & Katıl Butonu
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MColors.MintTint,
                                        shadowElevation = 3.dp,
                                        border = BorderStroke(0.8.dp, MColors.Mint.copy(alpha = 0.35f)),
                                        modifier = Modifier.shadow(4.dp, RoundedCornerShape(8.dp), ambientColor = Color(0x260B1220), spotColor = Color(0x360B1220))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(MColors.Mint)
                                            )
                                            Text(
                                                text = "Son ${activeMatch.freeSpots} Boş Kontenjan",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF047857)
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MColors.Brand,
                                        shadowElevation = 6.dp,
                                        modifier = Modifier
                                            .shadow(7.dp, RoundedCornerShape(10.dp), ambientColor = Color(0x360B1220), spotColor = Color(0x480B1220))
                                            .clickable { onSelectEvent(activeMatch) }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .background(Brush.horizontalGradient(listOf(Color(0xFFB33D15), Color(0xFFE76C32))))
                                                .padding(horizontal = 14.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text("Kadroya Katıl", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════════════
            // ═══════════════════════════════════════════════════════════════════
            // KART TİPİ 4: 🏆 BENTO GRID (TAKIM CHALLENGE & LİDERLİK)
            // ═══════════════════════════════════════════════════════════════════
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Sol Bento: Takım Challenge
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF192B35),
                        border = BorderStroke(1.dp, Brush.linearGradient(listOf(Color(0xFF4C5960), MColors.Brand.copy(alpha = 0.35f), Color(0xFF4C5960)))),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onOpenTeamChallenge)
                    ) {
                        Column(
                            modifier = Modifier
                                .feedCardFinish(Color(0xFF192B35), Color(0xFFFFAF77), 1, dark = true)
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A))), RoundedCornerShape(7.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Shield,
                                            contentDescription = null,
                                            tint = Color(0xFFFFB68F),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = "CHALLENGE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.4.sp,
                                        color = Color(0xFFF4F7F8)
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier
                                        .background(Color(0xFF253F3B), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Box(Modifier.size(5.dp).background(MColors.Mint, CircleShape))
                                    Text("AKTİF", fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF9EDDC8))
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Kadıköy Utd",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF4F7F8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "1.640 MMR",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFBE91)
                                )
                                Text("•", fontSize = 10.sp, color = MColors.Ink300)
                                Text(
                                    text = "1 Açık Teklif",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFFBCCBD2)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = Color(0xFF34424B),
                                shadowElevation = 5.dp,
                                border = BorderStroke(0.8.dp, MColors.Brand.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(
                                        elevation = 6.dp,
                                        shape = RoundedCornerShape(9.dp),
                                        ambientColor = Color(0x55000000),
                                        spotColor = Color(0x77000000)
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Rakip Bul",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFBE91)
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = null,
                                        tint = Color(0xFFFFBE91),
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Sağ Bento: Lig Sıralaması
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Brush.linearGradient(listOf(Color(0xFFE4D4B6), Color(0xFFD97706).copy(alpha = 0.35f), Color(0xFFE4D4B6)))),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onOpenLeaderboard)
                    ) {
                        Column(
                            modifier = Modifier
                                .feedCardFinish(Color(0xFFF8EDDA), Color(0xFFAC7E3D), 0)
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .shadow(4.dp, RoundedCornerShape(7.dp), ambientColor = Color(0x280B1220), spotColor = Color(0x380B1220))
                                            .background(Brush.linearGradient(listOf(Color(0xFFD97706), Color(0xFFB45309))), RoundedCornerShape(7.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.EmojiEvents,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = "SIRALAMA",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.4.sp,
                                        color = Color(0xFF493A27)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFFF7ED),
                                    shadowElevation = 3.dp,
                                    border = BorderStroke(0.5.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                                    modifier = Modifier.shadow(4.dp, RoundedCornerShape(6.dp), ambientColor = Color(0x240B1220), spotColor = Color(0x340B1220))
                                ) {
                                    Text(
                                        text = "3. Sıra",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFB45309),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Kadıköy Ligi",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF493A27),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "1.820 MMR",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF795B30)
                                )
                                Text("•", fontSize = 10.sp, color = MColors.Ink300)
                                Text(
                                    text = "+18 Puan",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MColors.Mint
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = Color(0xFF2D302F),
                                shadowElevation = 5.dp,
                                border = BorderStroke(0.8.dp, Color(0xFF41443C)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(
                                        elevation = 6.dp,
                                        shape = RoundedCornerShape(9.dp),
                                        ambientColor = Color(0x360B1220),
                                        spotColor = Color(0x480B1220)
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Tabloyu Gör",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFD99B)
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD99B),
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════════════
            // KART TİPİ 5: 🛡️ NO-SHOW & GÜVENCE ŞERİDİ
            // ═══════════════════════════════════════════════════════════════════
            // ═══════════════════════════════════════════════════════════════════
            // BÖLGENDEKİ TÜM KARŞILAŞMALAR LİSTESİ (Akışta atlanan maç kalmaz!)
            // ═══════════════════════════════════════════════════════════════════
            val remainingEvents = filteredEvents.filter { it.id != activeMatch?.id }
            if (remainingEvents.isNotEmpty()) {
                item {
                    CompactEventDirectory(
                        events = remainingEvents,
                        city = currentCity,
                        selectedSport = selectedSportFilter,
                        onSelectEvent = onSelectEvent
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showSmartMatchSheet) {
        SmartMatchSheet(
            activeFilterKey = activeAiFilterLabel,
            onSelectFilter = { activeAiFilterLabel = it },
            matchingCount = filteredEvents.size,
            bestMatch = activeMatch,
            onDismiss = { showSmartMatchSheet = false },
            onJoinBestMatch = {
                showSmartMatchSheet = false
                showAutoSquadSheet = true
            }
        )
    }

    if (showAutoSquadSheet) {
        AutoSquadSheet(
            event = activeMatch,
            currentPlayer = currentPlayer,
            onDismiss = { showAutoSquadSheet = false },
            onConfirmSquad = {
                showAutoSquadSheet = false
                activeMatch?.let(onSelectEvent)
            },
            onOpenChat = {
                showAutoSquadSheet = false
                onOpenAutoSquad()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartMatchSheet(
    activeFilterKey: String?,
    onSelectFilter: (String?) -> Unit,
    matchingCount: Int,
    bestMatch: SportEvent?,
    onDismiss: () -> Unit,
    onJoinBestMatch: () -> Unit
) {
    val filterOptions = listOf(
        null to "Tüm Uygun Oyunlar",
        "FOOTBALL_TONIGHT" to "Bu akşam",
        "WEEKEND_RACKET" to "Hafta sonu",
        "URGENT_FILL" to "Acil oyuncu",
        "TEAM_MATCH" to "Takım maçı"
    )
    val selectedOption = filterOptions.firstOrNull { it.first == activeFilterKey } ?: filterOptions.first()

    ModalBottomSheet(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        scrimColor = Color(0xFF0B1220).copy(alpha = .48f),
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF5F6F8),
        dragHandle = { MPremiumSheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MSheetHeading(
                title = "Akıllı Eşleşme",
                subtitle = "$matchingCount uygun oyun bulundu",
                icon = Icons.Rounded.AutoAwesome,
                onDismiss = onDismiss
            )

            MCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MFloatingDropdownBox(
                        label = "Eşleşme Tercihi",
                        selectedValue = selectedOption,
                        options = filterOptions,
                        optionLabel = { it.second },
                        onSelect = { onSelectFilter(it.first) },
                        icon = Icons.Rounded.AutoAwesome
                    )

                // En Uygun Eşleşme Yüzen Kartı
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 2.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = Color(0x2E0B1220),
                            spotColor = Color(0x400B1220)
                        )
                        .clickable(enabled = bestMatch != null) { onJoinBestMatch() }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(13.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f).padding(end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .shadow(5.dp, RoundedCornerShape(11.dp), ambientColor = Color(0x2C0B1220), spotColor = Color(0x3E0B1220))
                                        .background(
                                            Brush.linearGradient(listOf(Color(0xFF283548), MColors.Ink900)),
                                            RoundedCornerShape(11.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = bestMatch?.sport?.emoji ?: "✨",
                                        fontSize = 18.sp
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = bestMatch?.title ?: "Uygun maç bulunamadı",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MColors.Ink900,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (bestMatch != null) {
                                            "${bestMatch.day} · ${bestMatch.time} · ${bestMatch.venue}"
                                        } else {
                                            "Eşleşme tercihini değiştirerek yeni oyunları görebilirsin"
                                        },
                                        fontSize = 11.sp,
                                        color = MColors.Ink500,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (bestMatch != null) {
                                MChemistryBadge(percent = bestMatch.score.total, label = "Uyum")
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (activeFilterKey != null) {
                                Surface(
                                    onClick = { onSelectFilter(null) },
                                    shape = RoundedCornerShape(9.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    shadowElevation = 2.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Rounded.Close, null, tint = MColors.Ink600, modifier = Modifier.size(13.dp))
                                        Text("Sıfırla", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MColors.Ink700)
                                    }
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (matchingCount > 0) MColors.Mint else MColors.Amber)
                                    )
                                    Text(
                                        text = "$matchingCount uygun oyun",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MColors.Ink600
                                    )
                                }
                            }

                            Surface(
                                onClick = {
                                    if (bestMatch != null) onJoinBestMatch() else onDismiss()
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (bestMatch != null) MColors.BrandDeep else MColors.Ink900,
                                shadowElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .background(
                                            if (bestMatch != null) {
                                                Brush.linearGradient(listOf(Color(0xFFDF5E28), Color(0xFFB13D15)))
                                            } else {
                                                Brush.linearGradient(listOf(Color(0xFF283548), MColors.Ink900))
                                            }
                                        )
                                        .padding(horizontal = 13.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        text = if (bestMatch != null) "Katıl" else "Tamam",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// RADAR MİNİ HARİTA VİTRİNİ (Gerçek Apple Maps Görünümü)
// Kullanıcı dokunduğunda doğrudan interaktif geniş Harita sekmesine yönlendirir
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun LiveMiniRadarView(
    events: List<SportEvent>,
    scopeLabel: String,
    hasActiveSmartMatch: Boolean,
    onOpenSmartMatch: () -> Unit,
    onOpenMap: () -> Unit
) {
    val previewPins = remember(events) {
        events.filterNot { it.area.contains("online", ignoreCase = true) }
            .take(6)
            .mapIndexed { index, event ->
                val location = event.approximateLocation?.let { it.latitude to it.longitude } ?: demoCoordinatesForArea(event.area)
                MapPinData(
                    id = event.id,
                    title = event.title,
                    subtitle = if (event.freeSpots > 0) "${event.freeSpots} boş · ${event.time}" else "Dolu · ${event.time}",
                    latitude = location.first,
                    longitude = location.second,
                    sportEmoji = event.sport.emoji
                )
            }
    }
    val mapCenter = previewPins.firstOrNull()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(154.dp)
            .padding(vertical = 2.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0x320B1220),
                spotColor = Color(0x440B1220)
            )
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0x22000000), RoundedCornerShape(16.dp))
    ) {
        // Gerçek Harita Katmanı (Harita sekmesindeki gerçek Apple Maps görünümü)
        NativeMapView(
            modifier = Modifier.fillMaxSize(),
            latitude = mapCenter?.latitude ?: 40.9760,
            longitude = mapCenter?.longitude ?: 29.0400,
            zoomMeters = 4200.0,
            pins = previewPins,
            selectedPinId = null,
            onPinSelected = { onOpenMap() },
            interactive = false
        )

        // Şeffaf dokunma katmanı — tıklandığı an geniş harita bölümüne yönlendirir
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .clickable(onClick = onOpenMap)
        )

        // Sol üst: Akıllı Eşleşme ufak sembol butonu
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(9.dp)
        ) {
            Surface(
                onClick = onOpenSmartMatch,
                shape = RoundedCornerShape(11.dp),
                color = MColors.Ink900,
                shadowElevation = 6.dp,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (hasActiveSmartMatch) Color(0xFFFFB37B) else Color.White.copy(alpha = 0.25f)
                ),
                modifier = Modifier
                    .size(36.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(11.dp),
                        ambientColor = Color(0x3A0B1220),
                        spotColor = Color(0x520B1220)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF283548), MColors.Ink900)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "Akıllı Eşleşme",
                        tint = Color(0xFFFFB37B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (hasActiveSmartMatch) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(MColors.Mint)
                        .border(1.5.dp, Color.White, CircleShape)
                )
            }
        }

        // Harita üstü durum rozeti (RADAR ibaresi kaldırıldı)
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(9.dp)
                .widthIn(max = 190.dp),
            shape = RoundedCornerShape(9.dp),
            color = Color.White.copy(alpha = .96f),
            border = BorderStroke(.7.dp, Color(0xFFD1DED8)),
            shadowElevation = 2.dp
        ) {
            Row(
                Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(Modifier.size(5.dp).background(MColors.Mint, CircleShape))
                Text(
                    "$scopeLabel · ${events.size} oyun",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF213E38),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// AÇILIR KART: BÖLGESEL KARŞILAŞMA AKORDEONU (Expandable Match Card)
// Sayfayı doldurup taşırmayan, kompakt başlayıp tıklandıkça açılan lüks kart
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun ExpandableMatchCard(
    event: SportEvent,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSelectEvent: () -> Unit
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "ChevronRotation"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(
            width = if (isExpanded) 1.5.dp else 1.dp,
            color = if (isExpanded) MColors.Brand.copy(alpha = 0.5f) else Color(0xFFE2E8F0)
        ),
        shadowElevation = if (isExpanded) 3.dp else 1.5.dp,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable(onClick = onToggleExpand)
    ) {
        Column(modifier = Modifier.premiumSurface().padding(13.dp)) {
            // ═══════════════════════════════════════════════════════════════
            // 1. KOMPAKT ÜST SATIR (Her zaman derli toplu ve tek bakışta anlaşılır)
            // ═══════════════════════════════════════════════════════════════
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sol Bölüm: Branş Emojisi & Başlık & Saat
                Row(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (event.emergency) Color(0xFFFFF1EB) else MColors.BrandTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(event.sport.emoji, fontSize = 20.sp)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = event.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MColors.Ink900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (event.emergency) {
                                Spacer(modifier = Modifier.width(5.dp))
                                Surface(
                                    shape = RoundedCornerShape(5.dp),
                                    color = Color(0xFFFFF1EB)
                                ) {
                                    Text(
                                        text = "🚨 ACİL",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MColors.Brand,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${event.day} · ${event.time} · 📍 ${event.venue}${event.distanceKm?.let { " · $it km" }.orEmpty()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MColors.Ink500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Sağ Bölüm: Boş Kontenjan + Uyum Rozeti + Dönen Açılır Ok
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (event.freeSpots <= 2) Color(0xFFFFF7ED) else Color(0xFFF0FDF4),
                        border = BorderStroke(
                            1.dp,
                            if (event.freeSpots <= 2) Color(0xFFFFEDD5) else Color(0xFFDCFCE7)
                        )
                    ) {
                        Text(
                            text = "${event.freeSpots} Boş",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (event.freeSpots <= 2) MColors.Brand else Color(0xFF15803D),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    if (event.score.reasons.isNotEmpty()) {
                        MChemistryBadge(percent = event.score.total, label = "%")
                    } else {
                        Text("Yeni ilan", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MColors.Ink500)
                    }

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Daralt" else "Aç",
                            tint = MColors.Ink700,
                            modifier = Modifier
                                .size(17.dp)
                                .rotate(chevronRotation)
                        )
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════════
            // 2. AÇILIR DETAY ALANI (Sadece Kart Genişletildiğinde Akıcı Açılır)
            // ═══════════════════════════════════════════════════════════════
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // Fiyat, Katılım ve Güvence Şeridi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Kişi Başı Ücret",
                            fontSize = 11.sp,
                            color = MColors.Ink500,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "TKN ${event.pricePerPersonTry}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MColors.Brand
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Shield, null, tint = Color(0xFF15803D), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "10 TKN Güvenli Havuz",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Kadro İlerleme ve Güvenilirlik Çubuğu
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Kadro Doluluğu: ${event.participantIds.size} / ${event.capacity} Oyuncu",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MColors.Ink800
                            )
                            Text(
                                text = "Min. ${event.minReliability} Güvenilirlik",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MColors.Brand
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { (event.participantIds.size.toFloat() / event.capacity.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (event.freeSpots <= 2) MColors.Brand else MColors.Mint,
                            trackColor = Color(0xFFE2E8F0)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Mini Roster Avatarları
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            event.participantIds.take(5).forEach { pid ->
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(MColors.BrandTint),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = pid.take(1).uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MColors.Brand
                                    )
                                }
                            }
                            repeat(minOf(event.freeSpots, 3)) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, Color(0xFFCBD5E1), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PersonAdd,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            if (event.capacity > 8) {
                                Text(
                                    text = "+${event.capacity - 8}",
                                    fontSize = 11.sp,
                                    color = MColors.Ink500,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Aksiyon Butonları
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSelectEvent,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Text(
                            text = "Detay & Konum",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MColors.Ink800
                        )
                    }

                    Button(
                        onClick = onSelectEvent,
                        colors = ButtonDefaults.buttonColors(containerColor = MColors.Brand),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.3f).height(40.dp)
                    ) {
                        Text(
                            text = "Kadroya Katıl →",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}


private fun demoCoordinatesForArea(area: String): Pair<Double, Double> = when {
    area.contains("Maslak", ignoreCase = true) -> 41.1118 to 29.0225
    area.contains("Çankaya", ignoreCase = true) -> 39.9120 to 32.8597
    area.contains("Kalamış", ignoreCase = true) || area.contains("Fenerbahçe", ignoreCase = true) -> 40.9748 to 29.0430
    area.contains("Caddebostan", ignoreCase = true) -> 40.9628 to 29.0640
    area.contains("Suadiye", ignoreCase = true) -> 40.9584 to 29.0831
    area.contains("Bostancı", ignoreCase = true) -> 40.9514 to 29.0962
    area.contains("Acıbadem", ignoreCase = true) -> 40.9944 to 29.0480
    area.contains("Fikirtepe", ignoreCase = true) -> 40.9897 to 29.0471
    else -> 40.9840 to 29.0260
}

@Composable
private fun CompactEventDirectory(
    events: List<SportEvent>, city: String, selectedSport: Sport?, onSelectEvent: (SportEvent) -> Unit
) {
    var page by remember(city, selectedSport, events.map { it.id }) { mutableIntStateOf(0) }
    val groups = events.groupBy { it.sport }.entries.toList()
    val pageCount = ((groups.size + 2) / 3).coerceAtLeast(1)
    val currentPage = page.coerceIn(0, pageCount - 1)
    MCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Spor buluşmaları", style = MaterialTheme.typography.titleLarge, color = MColors.Ink900)
                    Text("$city · ${events.size} etkinlik · ${groups.size} branş", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
                }
                MBadge("${currentPage + 1} / $pageCount", MColors.BrandDeep, MColors.BrandTint)
            }
            groups.drop(currentPage * 3).take(3).forEach { (sport, matches) ->
                var matchPage by remember(city, sport, matches.map { it.id }) { mutableIntStateOf(0) }
                val match = matches[matchPage.coerceIn(0, matches.lastIndex)]
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${sport.emoji} ${sport.label}", style = MaterialTheme.typography.titleSmall, color = MColors.Ink800, modifier = Modifier.weight(1f))
                        if (matches.size > 1) {
                            IconButton(onClick = { matchPage = (matchPage - 1 + matches.size) % matches.size }, modifier = Modifier.size(32.dp)) { Icon(Icons.Rounded.ChevronLeft, "Önceki etkinlik") }
                            Text("${matchPage + 1}/${matches.size}", style = MaterialTheme.typography.labelSmall, color = MColors.Ink500)
                            IconButton(onClick = { matchPage = (matchPage + 1) % matches.size }, modifier = Modifier.size(32.dp)) { Icon(Icons.Rounded.ChevronRight, "Sonraki etkinlik") }
                        }
                    }
                    Surface(
                        onClick = { onSelectEvent(match) },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 7.dp,
                        border = BorderStroke(1.dp, Color(0xFFE4E9F2)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 3.dp, vertical = 3.dp)
                            .shadow(
                                elevation = 10.dp,
                                shape = RoundedCornerShape(16.dp),
                                ambientColor = Color(0x360B1220),
                                spotColor = Color(0x4A0B1220)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFFAFBFD))))
                                .premiumSurface()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .width(52.dp)
                                    .shadow(4.dp, RoundedCornerShape(12.dp), ambientColor = Color(0x280B1220), spotColor = Color(0x360B1220))
                                    .background(MColors.Ink900, RoundedCornerShape(12.dp))
                                    .padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(match.day, color = Color(0xFFFFC09C), fontSize = 10.sp, maxLines = 1)
                                Text(match.time, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(match.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MColors.Ink900, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(match.area, style = MaterialTheme.typography.bodySmall, color = MColors.Ink500, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(Modifier.width(8.dp))
                            MBadge(if (match.isFull) "Dolu" else "${match.freeSpots} yer", if (match.isFull) MColors.Ink500 else MColors.BrandDeep, MColors.BrandTint)
                            Icon(Icons.Rounded.ChevronRight, null, tint = MColors.Ink400, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            if (pageCount > 1) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { page = currentPage - 1 }, enabled = currentPage > 0) { Icon(Icons.Rounded.ChevronLeft, null); Text("Önceki") }
                Text("Branşlar", style = MaterialTheme.typography.labelSmall, color = MColors.Ink500)
                TextButton(onClick = { page = currentPage + 1 }, enabled = currentPage < pageCount - 1) { Text("Sonraki"); Icon(Icons.Rounded.ChevronRight, null) }
            }
        }
    }
}

@Composable
private fun NetworkScopeIsland(selected: NetworkMode, onSelect: (NetworkMode) -> Unit) {
    Surface(shape = RoundedCornerShape(22.dp), color = Color.White.copy(alpha = 0.97f),
        shadowElevation = 3.dp, border = BorderStroke(1.dp, Color.White), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(5.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            NetworkMode.entries.forEach { mode ->
                val active = mode == selected
                val capsuleColor by animateColorAsState(if (active) MColors.Ink900 else Color.Transparent, tween(260), label = "network_selection")
                Surface(onClick = { onSelect(mode) }, shape = RoundedCornerShape(17.dp),
                    color = capsuleColor,
                    shadowElevation = if (active) 3.dp else 0.dp, modifier = Modifier.weight(1f)) {
                    Column(Modifier.background(Brush.linearGradient(if (active) listOf(Color(0xFF293951), MColors.Ink900) else listOf(Color.Transparent, Color.Transparent))).padding(vertical = 10.dp, horizontal = 2.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(when (mode) {
                            NetworkMode.CITY -> Icons.Rounded.SportsSoccer
                            NetworkMode.CAMPUS -> Icons.Rounded.School
                            NetworkMode.COMPANY -> Icons.Rounded.BusinessCenter
                            NetworkMode.TRAVEL -> Icons.Rounded.FlightTakeoff
                        }, null, tint = if (active) Color(0xFFFFB68F) else MColors.Ink400, modifier = Modifier.size(19.dp))
                        Text(mode.label, style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            color = if (active) Color.White else MColors.Ink500, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

// Draw-only finish: preserves the cards' exact layout, hit targets and content size.
private fun Modifier.feedCardFinish(base: Color, accent: Color, pattern: Int, dark: Boolean = false): Modifier = drawBehind {
    drawRect(Brush.linearGradient(listOf(if (dark) Color(0xFF304550) else Color.White, base)))
    val center = Offset(size.width * 1.05f, size.height * .15f)
    when (pattern) {
        0 -> listOf(52.dp, 76.dp, 100.dp).forEach {
            drawCircle(accent.copy(alpha = .12f), it.toPx(), center, style = Stroke(.8.dp.toPx()))
        }
        1 -> repeat(4) { index ->
            val x = size.width - (index * 18 + 12).dp.toPx()
            drawLine(accent.copy(alpha = if (dark) .10f else .07f), Offset(x, 0f), Offset(x + 70.dp.toPx(), size.height), 1.dp.toPx())
        }
        else -> {
            drawCircle(accent.copy(alpha = .035f), 100.dp.toPx(), center)
            drawCircle(accent.copy(alpha = .10f), 76.dp.toPx(), center, style = Stroke(.8.dp.toPx()))
        }
    }
    drawLine(if (dark) Color.White.copy(alpha = .18f) else Color.White, Offset(20.dp.toPx(), 1.dp.toPx()), Offset(size.width - 20.dp.toPx(), 1.dp.toPx()), 1.dp.toPx())
}
