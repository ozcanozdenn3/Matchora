package com.matchora.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.matchora.app.domain.EventStatus
import com.matchora.app.domain.Sport
import com.matchora.app.state.*
import com.matchora.app.ui.theme.MColors

// ═══════════════════════════════════════════════════════════════════════════════
// ETKİNLİK OLUŞTURMA & TEAM CHALLENGE SHEET (Strateji Dokümanı §08, §09)
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventSheet(
    onDismiss: () -> Unit,
    newEventId: () -> String = { "legacy_${kotlin.random.Random.nextLong().toString(16)}" },
    onCreateEvent: (SportEvent) -> Unit
) {
    var selectedSport by remember { mutableStateOf(Sport.FOOTBALL) }
    var sportCategory by remember { mutableStateOf(SportCategory.PHYSICAL) }
    var customSportName by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var venue by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("Kadıköy") }
    var time by remember { mutableStateOf("21:00") }
    var day by remember { mutableStateOf("Bugün") }
    var mode by remember { mutableStateOf(PlayMode.COMPETITIVE) }
    var capacity by remember { mutableIntStateOf(10) }
    var priceTry by remember { mutableIntStateOf(180) }
    var isEmergency by remember { mutableStateOf(false) }

    ModalBottomSheet(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        scrimColor = androidx.compose.ui.graphics.Color(0xFF0B1220).copy(alpha = .48f),

        onDismissRequest = onDismiss,
        containerColor = MColors.Surface,
        dragHandle = { MPremiumSheetHandle() }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                MSheetHeading(
                    title = "Yeni etkinlik oluştur",
                    subtitle = "Maç, açık oyun veya acil eksik oyuncu ilanı yayınla",
                    icon = Icons.Rounded.Campaign,
                    onDismiss = onDismiss
                )
            }

            // Kategori & Branş Seçimi
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    shadowElevation = 5.dp,
                    border = BorderStroke(1.dp, Color(0xFFE4E9F2)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 7.dp,
                            shape = RoundedCornerShape(18.dp),
                            ambientColor = Color(0x280B1220),
                            spotColor = Color(0x380B1220)
                        )
                ) {
                Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "BRANŞ KATEGORİSİ",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MColors.Ink400
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Kategori Butonları: [🏃‍♂️ Fiziksel Sporlar] [🎮 E-Spor & Zeka] [✨ Özel / Diğer]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SportCategory.entries.forEach { cat ->
                        val isSel = sportCategory == cat
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) Color.White else Color.Transparent)
                                .clickable {
                                    sportCategory = cat
                                    if (cat == SportCategory.PHYSICAL && selectedSport.category != SportCategory.PHYSICAL) {
                                        selectedSport = Sport.FOOTBALL
                                    } else if (cat == SportCategory.ESPORTS && selectedSport.category != SportCategory.ESPORTS) {
                                        selectedSport = Sport.ESPORTS_FC
                                    } else if (cat == SportCategory.CUSTOM) {
                                        selectedSport = Sport.CUSTOM
                                    }
                                }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${cat.emoji} ${cat.label}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) MColors.Brand else MColors.Ink600,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (sportCategory == SportCategory.CUSTOM || selectedSport == Sport.CUSTOM) {
                    MFloatingInputBox(
                        label = "Özel Branş / Oyun Adı",
                        value = customSportName,
                        onValueChange = { customSportName = it },
                        placeholder = "Örn: Boks, Squash, Tırmanış, Dota 2...",
                        icon = Icons.Rounded.AutoAwesome,
                        badgeText = "Özel"
                    )
                } else {
                    val currentList = if (sportCategory == SportCategory.PHYSICAL) physicalSports else esportSports
                    SportPickerField(selectedSport, { if (it != null) { selectedSport = it; capacity = capacityForFormat(defaultFormatForSport(it)) } }, sports = currentList, allowAll = false)
                }
                }
                }
            }

            // Başlık & Tesis (Yüzen Premium Kutular)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MFloatingInputBox(
                        label = "Etkinlik Başlığı",
                        value = title,
                        onValueChange = { title = it },
                        placeholder = "Örn: Akşam Halı Saha · Orta Saha Eksik",
                        icon = Icons.Rounded.Campaign,
                        badgeText = "Başlık",
                        suggestions = listOf("Akşam Halı Saha", "Dostluk Maçı", "Hafta Sonu Maçı")
                    )
                    MFloatingInputBox(
                        label = "Tesis / Saha Adı",
                        value = venue,
                        onValueChange = { venue = it },
                        placeholder = "Örn: Moda Spor Parkı",
                        icon = Icons.Rounded.Stadium,
                        badgeText = "Konum",
                        suggestions = listOf("Moda Spor Parkı", "Kalamış Sahil Kortları", "Caddebostan Saha")
                    )
                }
            }

            // Gün, Saat, Kapasite, Mod ve Acil Çağrı (Yüzen Gölgeli Açılır Menüler)
            item {
                MCard(Modifier.fillMaxWidth(), cornerRadius = 22.dp, elevation = 6.dp) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MFloatingDropdownBox(
                            label = "Etkinlik Günü",
                            selectedValue = day,
                            options = listOf("Bugün", "Yarın", "Cumartesi", "Pazar", "Hafta İçi"),
                            optionLabel = { it },
                            onSelect = { day = it },
                            icon = Icons.Rounded.CalendarMonth
                        )

                        MFloatingDropdownBox(
                            label = "Başlangıç Saati",
                            selectedValue = time,
                            options = listOf("18:00", "19:00", "20:00", "20:30", "21:00", "21:30", "22:00"),
                            optionLabel = { it },
                            onSelect = { time = it },
                            icon = Icons.Rounded.Schedule
                        )

                        MFloatingDropdownBox(
                            label = "Kontenjan / Kapasite",
                            selectedValue = capacity,
                            options = listOf(2, 4, 6, 8, 10, 12, 14, 16, 20),
                            optionLabel = { "$it Sporcu" },
                            onSelect = { capacity = it },
                            icon = Icons.Rounded.Groups
                        )

                        MFloatingDropdownBox(
                            label = "Oyun Modu",
                            selectedValue = mode,
                            options = PlayMode.entries,
                            optionLabel = { if (it == PlayMode.SOCIAL) "Sosyal Maç" else "Rekabetçi (Rating)" },
                            onSelect = { mode = it },
                            icon = Icons.Rounded.EmojiEvents
                        )

                        MFloatingDropdownBox(
                            label = "Çağrı Önceliği",
                            selectedValue = isEmergency,
                            options = listOf(false, true),
                            optionLabel = { if (it) "Acil Oyuncu Çağrısı" else "Standart Yayın" },
                            onSelect = { isEmergency = it },
                            icon = Icons.Rounded.NotificationsActive
                        )
                    }
                }
            }

            // Gönder Butonu
            item {
                MButton(
                    text = "Etkinliği Yayınla (10 TKN Taahhüt İle)",
                    onClick = {
                        val newEv = SportEvent(
                            id = newEventId(),
                            title = if (title.isNotBlank()) title else "${selectedSport.label} Karşılaşması",
                            sport = selectedSport,
                            venue = venue,
                            area = area,
                            day = day,
                            time = time,
                            startsInMin = 90,
                            durationMin = 60,
                            distanceKm = 1.0f,
                            capacity = capacity,
                            participantIds = listOf(DemoData.ME),
                            waitlistCount = 0,
                            mode = mode,
                            levelRange = "Her seviye",
                            minReliability = 75,
                            verifiedOnly = mode == PlayMode.COMPETITIVE,
                            pricePerPersonTry = priceTry,
                            hostId = DemoData.ME,
                            status = EventStatus.FILLING,
                            score = ScoreBreakdown(95, 95, 90, 95, 90, listOf("Kendi oluşturduğun maç")),
                            emergency = isEmergency,
                            customSportName = if (selectedSport == Sport.CUSTOM) customSportName else null
                        )
                        onCreateEvent(newEv)
                        onDismiss()
                    }
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TEAM CHALLENGE GÖNDERME DİYALOĞU
// ═══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamChallengeSheet(
    myTeam: TeamProfile,
    targetTeam: TeamProfile,
    onDismiss: () -> Unit,
    onSendChallenge: (proposedTime: String, venue: String) -> Unit
) {
    // ═══════════════ İNTERAKTİF SEÇİM DURUMLARI ═══════════════
    var selectedDay by remember { mutableStateOf("Cumartesi (19 Eki)") }
    var selectedTimeSlot by remember { mutableStateOf("20:30") }
    var hour by remember { mutableIntStateOf(20) }
    var minute by remember { mutableIntStateOf(30) }
    var isCustomTime by remember { mutableStateOf(false) }

    var selectedVenue by remember { mutableStateOf("Moda Spor Parkı · Saha 1 (Suni Çim)") }
    var selectedFormat by remember { mutableStateOf("7v7 Halı Saha") }
    var tokenStake by remember { mutableIntStateOf(25) }
    var needReferee by remember { mutableStateOf(false) }

    val formattedTime = remember(selectedDay, isCustomTime, selectedTimeSlot, hour, minute) {
        val timeStr = if (isCustomTime) "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}" else selectedTimeSlot
        "$selectedDay $timeStr"
    }

    ModalBottomSheet(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        scrimColor = androidx.compose.ui.graphics.Color(0xFF0B1220).copy(alpha = .48f),

        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF5F6F8),
        dragHandle = { MPremiumSheetHandle() }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Başlık
            item {
                MSheetHeading(
                    title = "Meydan Oku · Sezon 4",
                    subtitle = "Tarih, saat ve saha belirleyerek resmi maç teklifi ilet",
                    icon = Icons.Rounded.SportsKabaddi,
                    onDismiss = onDismiss
                )
            }

            // Karşılaşma Kartı: Biz vs Rakip
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    shadowElevation = 5.dp,
                    border = BorderStroke(1.dp, Color(0xFFE4E9F2)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 7.dp,
                            shape = RoundedCornerShape(18.dp),
                            ambientColor = Color(0x280B1220),
                            spotColor = Color(0x380B1220)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Bizim Takım
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(myTeam.color),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(myTeam.badge, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(myTeam.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MColors.Ink900, maxLines = 1)
                                Text("${myTeam.rating} MMR", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MColors.Brand)
                            }
                        }

                        // VS Rozeti
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF1F5F9))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("VS", color = MColors.Brand, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        // Rakip Takım
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End, modifier = Modifier.weight(1f)) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(targetTeam.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MColors.Ink900, maxLines = 1)
                                Text("${targetTeam.rating} MMR", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MColors.Mint)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(targetTeam.color),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(targetTeam.badge, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Tarih, Saat, Saha, Format, Ödül Havuzu ve Hakem (Yüzen Gölgeli Açılır Menüler)
            item {
                MCard(Modifier.fillMaxWidth(), cornerRadius = 22.dp, elevation = 6.dp) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val days = listOf("Bugün", "Yarın", "Cumartesi (19 Eki)", "Pazar (20 Eki)", "Pazartesi (21 Eki)")
                        MFloatingDropdownBox(
                            label = "Maç Tarihi",
                            selectedValue = selectedDay,
                            options = days,
                            optionLabel = { it },
                            onSelect = { selectedDay = it },
                            icon = Icons.Rounded.CalendarMonth
                        )

                        val slots = listOf("18:30", "19:30", "20:00", "20:30", "21:00", "21:30", "22:00")
                        MFloatingDropdownBox(
                            label = "Maç Saati",
                            selectedValue = selectedTimeSlot,
                            options = slots,
                            optionLabel = { it },
                            onSelect = {
                                selectedTimeSlot = it
                                isCustomTime = false
                            },
                            icon = Icons.Rounded.Schedule
                        )

                        val venues = listOf(
                            "Moda Spor Parkı · Saha 1 (Suni Çim)",
                            "Kalamış Spor Merkezi (Açık Halı Saha)",
                            "Caddebostan Halı Saha (Kapalı)",
                            "Fenerbahçe Dereağzı Tesisleri"
                        )
                        MFloatingDropdownBox(
                            label = "Saha ve Tesis",
                            selectedValue = selectedVenue,
                            options = venues,
                            optionLabel = { it },
                            onSelect = { selectedVenue = it },
                            icon = Icons.Rounded.Stadium
                        )

                        MFloatingDropdownBox(
                            label = "Maç Formatı",
                            selectedValue = selectedFormat,
                            options = listOf("5v5 Halı Saha", "7v7 Halı Saha", "11v11 Tam"),
                            optionLabel = { it },
                            onSelect = { selectedFormat = it },
                            icon = Icons.Rounded.Groups
                        )

                        MFloatingDropdownBox(
                            label = "Bahis / Ödül Havuzu",
                            selectedValue = tokenStake,
                            options = listOf(10, 25, 50),
                            optionLabel = { "$it TKN · Havuz ${it * 2} TKN" },
                            onSelect = { tokenStake = it },
                            icon = Icons.Rounded.Toll
                        )

                        MFloatingDropdownBox(
                            label = "Gözlemci Hakem",
                            selectedValue = needReferee,
                            options = listOf(false, true),
                            optionLabel = { if (it) "Resmî Hakem Talebi (+15 TKN)" else "Hakemsiz · Kaptan Onaylı" },
                            onSelect = { needReferee = it },
                            icon = Icons.Rounded.Gavel
                        )
                    }
                }
            }

            // GÖNDER BUTONU
            item {
                val summaryText = "$formattedTime · $selectedVenue · $selectedFormat · $tokenStake TKN Bahis"
                Button(
                    onClick = {
                        onSendChallenge(formattedTime, selectedVenue)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MColors.Brand),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Send, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Meydan Okumayı Gönder ($tokenStake TKN Kilitlenecek)",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// ŞİKAYET & İTİRAZ / DISPUTE DİYALOĞU (Strateji Dokümanı §12, §23)
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun ReportAndDisputeDialog(
    targetName: String,
    onDismiss: () -> Unit,
    onSubmitReport: (reason: String, note: String) -> Unit
) {
    var selectedReason by remember { mutableStateOf("No-show (Maça gelmedi)") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        tonalElevation = 0.dp,

        onDismissRequest = onDismiss,
        title = {
            Text("İtiraz / Bildirim: $targetName", fontWeight = FontWeight.Bold, color = MColors.Ink900)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Bu işlem moderasyon ve güven ekibi tarafından incelenir. Sahte bildirimler bildiren kullanıcının güven skorunu düşürür.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MColors.Ink600
                )

                val reasons = listOf(
                    "No-show (Haber vermeden maça gelmedi)",
                    "Son dakika iptali (Maçı riske attı)",
                    "Fair play / Sportmenlik dışı davranış",
                    "Yanlış skor beyanı (Rating itirazı)",
                    "Uygunsuz içerik / İletişim"
                )
                MFloatingDropdownBox(
                    label = "Bildirim Nedeni",
                    selectedValue = selectedReason,
                    options = reasons,
                    optionLabel = { it },
                    onSelect = { selectedReason = it },
                    icon = Icons.Rounded.ReportProblem
                )

                MFloatingInputBox(
                    label = "Ek Açıklama",
                    value = note,
                    onValueChange = { note = it },
                    placeholder = "Olayla ilgili kısa not...",
                    icon = Icons.Rounded.EditNote,
                    badgeText = "Opsiyonel",
                    singleLine = false,
                    minLines = 2,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmitReport(selectedReason, note)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MColors.Rose)
            ) {
                Text("Bildirimi Gönder", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç", color = MColors.Ink500)
            }
        },
        containerColor = MColors.Surface
    )
}
