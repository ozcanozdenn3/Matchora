package com.matchora.app.ui.components

import com.matchora.app.ui.components.premiumSurface
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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.matchora.app.state.ChallengeOffer
import com.matchora.app.state.ChallengeStatus
import com.matchora.app.state.Player
import com.matchora.app.state.TeamProfile
import com.matchora.app.ui.theme.MColors
import kotlin.math.abs

// ═══════════════════════════════════════════════════════════════════════════════
// SEND TEAM CHALLENGE SHEET — RESMİ MEYDAN OKUMA VE MAÇ SÖZLEŞMESİ
// Kaptan rakip takıma maç günü, saha, taahhüt tokenı, hakem ve AI oranlarıyla meydan okur.
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendTeamChallengeSheet(
    myTeam: TeamProfile,
    targetTeam: TeamProfile,
    tokenBalance: Int = 120,
    onDismiss: () -> Unit,
    onSendChallenge: (ChallengeOffer) -> Unit
) {
    var proposedTime by remember { mutableStateOf("Çarşamba 21:00") }
    var selectedVenue by remember { mutableStateOf("Moda Spor Parkı · Saha 2") }
    var stakeTokens by remember { mutableIntStateOf(50) }
    var challengeNote by remember { mutableStateOf("") }
    var officialReferee by remember { mutableStateOf(true) }

    val timeOptions = listOf("Bu Akşam 21:00", "Çarşamba 20:30", "Perşembe 21:00", "Cumartesi 18:00", "Pazar 19:30")
    val venueOptions = listOf(
        "Moda Spor Parkı · Saha 2",
        "Kalamış Sahil Kortları",
        "Bostancı Halı Saha",
        "Fikirtepe Arena",
        "Online Discord Sunucusu"
    )

    val myProb = remember(myTeam.rating, targetTeam.rating) {
        val diff = myTeam.rating - targetTeam.rating
        val p = 50 + (diff / 25)
        p.coerceIn(25, 75)
    }
    val oppProb = 100 - myProb
    val ratingGap = myTeam.rating - targetTeam.rating
    val teamFit = (96 - abs(ratingGap) / 8).coerceIn(60, 96)
    val matchupInsight = when {
        ratingGap > 50 -> "MMR üstünlüğünü koru; savunma dengesini bozma."
        ratingGap < -50 -> "Rakibin MMR üstünlüğüne karşı kompakt oyun ve hızlı geçişleri dene."
        else -> "Takımlar yakın güçte; oyun disiplini ve ilk skor belirleyici olabilir."
    }

    ModalBottomSheet(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        scrimColor = androidx.compose.ui.graphics.Color(0xFF0B1220).copy(alpha = .48f),

        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF5F6F7),
        dragHandle = { MPremiumSheetHandle() }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                MSheetHeading("Meydan oku", "Maç teklifini hazırla · %$teamFit takım uyumu", Icons.Rounded.SportsSoccer, onDismiss)
            }

            item { PremiumMatchupPanel(myTeam, targetTeam) }

            // AI Kazanma İhtimali & Taktik Önizleme
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, MColors.Mint.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("DEMO · AI TAHMİNİ", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.35.sp, color = Color(0xFF166534), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text("%$myProb – %$oppProb", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFFE2E8F0))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(myProb / 100f)
                                    .fillMaxHeight()
                                    .background(MColors.Mint)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = matchupInsight,
                            fontSize = 11.sp,
                            color = Color(0xFF14532D),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Maç Zamanı, Saha ve Kasa Kilidi (Yüzen Gölgeli Açılır Menüler)
            item {
                MCard(Modifier.fillMaxWidth(), cornerRadius = 22.dp, elevation = 6.dp) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MFloatingDropdownBox(
                            label = "Önerilen Maç Zamanı",
                            selectedValue = proposedTime,
                            options = timeOptions,
                            optionLabel = { it },
                            onSelect = { proposedTime = it },
                            icon = Icons.Rounded.Schedule
                        )

                        MFloatingDropdownBox(
                            label = "Önerilen Saha / Tesis",
                            selectedValue = selectedVenue,
                            options = venueOptions,
                            optionLabel = { it },
                            onSelect = { selectedVenue = it },
                            icon = Icons.Rounded.LocationOn
                        )

                        MFloatingDropdownBox(
                            label = "Maç Türü ve Kasa Kilidi",
                            selectedValue = stakeTokens,
                            options = listOf(0, 50),
                            optionLabel = { stake ->
                                if (stake == 0) "Dostluk Maçı · 0 TKN" else "Derbi Ligi · 50 TKN"
                            },
                            onSelect = { stakeTokens = it },
                            icon = Icons.Rounded.Toll
                        )
                    }
                }
            }

            // Kaptana Not (Premium Yüzen Açıklama Kutusu)
            item {
                MFloatingInputBox(
                    label = "Rakip Kaptana Kısa Not",
                    value = challengeNote,
                    onValueChange = { challengeNote = it },
                    placeholder = "Örn: Kadromuz hazır, saha bize uygundur.",
                    subtitle = "Maç teklifinle birlikte rakip takım kaptanına iletilecek mesaj.",
                    icon = Icons.Rounded.Forum,
                    badgeText = "İsteğe bağlı",
                    singleLine = false,
                    minLines = 2,
                    maxLines = 3,
                    suggestions = listOf(
                        "Kadromuz tam, sahaya hazırız",
                        "Saat ve saha bize uygundur",
                        "Centilmence güzel bir maç olsun"
                    )
                )
            }

            // Meydan Okuma Gönder Butonu
            item {
                MButton(
                    text = "Meydan okumayı gönder",
                    icon = Icons.Rounded.Send,
                    onClick = {
                        val newChallenge = ChallengeOffer(
                            id = "c_${myTeam.id}_${targetTeam.id}_${proposedTime.hashCode()}",
                            fromTeamId = myTeam.id,
                            toTeamId = targetTeam.id,
                            proposedTime = proposedTime,
                            venue = selectedVenue,
                            status = ChallengeStatus.PENDING,
                            incoming = false,
                            fitScore = 94,
                            stakeTokens = stakeTokens,
                            note = challengeNote.ifBlank { "Resmi maç teklifi iletildi." }
                        )
                        onSendChallenge(newChallenge)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MATCH CHALLENGE DETAIL SHEET — MEVCUT MEYDAN OKUMA VE RESMİ SÖZLEŞME MODALI
// Bir meydan okumaya tıklandığında açılan kapsamlı, ikna edici sözleşme & maç detayı
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchChallengeDetailSheet(
    challenge: ChallengeOffer,
    fromTeam: TeamProfile,
    toTeam: TeamProfile,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onOpenCaptainChat: (Player) -> Unit,
    onDismiss: () -> Unit
) {
    val oppCaptain = remember(fromTeam, toTeam, challenge.incoming) {
        val target = if (challenge.incoming) fromTeam else toTeam
        com.matchora.app.state.DemoData.player(target.captainId) ?: com.matchora.app.state.DemoData.players.first()
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
            contentPadding = PaddingValues(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Başlık & Kapat
            item {
                MSheetHeading(
                    title = "Maç sözleşmesi",
                    subtitle = "${challenge.status.label} · PROTOKOL-${challenge.id.takeLast(4).uppercase()}",
                    icon = Icons.Rounded.Gavel,
                    onDismiss = onDismiss
                )
            }

            item { PremiumMatchupPanel(fromTeam, toTeam) }

            // Maç Detayları (Zaman, Saha, Hakem) — Yüzen Beyaz Ada
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    shadowElevation = 5.dp,
                    border = BorderStroke(1.dp, Color(0xFFE4E9F2)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(7.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x280B1220), spotColor = Color(0x380B1220))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CalendarToday, null, tint = MColors.Brand, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Maç Zamanı: ${challenge.proposedTime}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MColors.Ink900)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.LocationOn, null, tint = MColors.Brand, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tesis / Saha: ${challenge.venue}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MColors.Ink800)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Shield, null, tint = MColors.Mint, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Kasa Güvencesi: ${challenge.stakeTokens * 2} TKN Toplam Havuz (Kazanan +25 MMR)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MColors.Mint)
                        }
                    }
                }
            }

            // Kaptan Notu (Varsa) — Yüzen Beyaz Ada
            if (!challenge.note.isNullOrBlank()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 4.dp,
                        border = BorderStroke(1.dp, MColors.Brand.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(6.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x240B1220), spotColor = Color(0x340B1220))
                    ) {
                        Row(modifier = Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(10.dp), color = MColors.BrandTint, modifier = Modifier.size(32.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.FormatQuote, null, tint = MColors.BrandDeep, modifier = Modifier.size(17.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "“${challenge.note}”",
                                fontSize = 12.sp,
                                color = MColors.Ink800,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Aksiyon Butonları
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (challenge.incoming && challenge.status == ChallengeStatus.PENDING) {
                        MButton(
                            text = "Meydan Okumayı Kabul Et ✓",
                            onClick = {
                                onAccept()
                                onDismiss()
                            },
                            icon = Icons.Rounded.CheckCircle
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MButton(
                                text = "Reddet",
                                onClick = {
                                    onDecline()
                                    onDismiss()
                                },
                                icon = Icons.Rounded.Close,
                                modifier = Modifier.weight(1f),
                                isPrimary = false,
                                height = 44.dp
                            )

                            MButton(
                                text = "Kaptanla Görüş",
                                onClick = {
                                    onDismiss()
                                    onOpenCaptainChat(oppCaptain)
                                },
                                icon = Icons.Rounded.ChatBubbleOutline,
                                modifier = Modifier.weight(1f),
                                isPrimary = false,
                                height = 44.dp
                            )
                        }
                    } else {
                        MButton(
                            text = "Kaptan ile Canlı Sohbet Odası",
                            onClick = {
                                onDismiss()
                                onOpenCaptainChat(oppCaptain)
                            },
                            icon = Icons.Rounded.Chat
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// AI MATCH SCOUTER & ŞEHİR ŞAMPİYONLUĞU KEMERİ MODALI (World-First Feature)
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiMatchScouterSheet(
    myTeam: TeamProfile,
    opponents: List<TeamProfile>,
    onDismiss: () -> Unit,
    onIssueBeltChallenge: (targetTeam: TeamProfile) -> Unit
) {
    var selectedOpponent by remember { mutableStateOf(opponents.firstOrNull() ?: myTeam) }
    val champion = (listOf(myTeam) + opponents).maxByOrNull { it.rating } ?: myTeam
    val ratingGap = myTeam.rating - selectedOpponent.rating
    val teamFit = (96 - abs(ratingGap) / 8).coerceIn(60, 96)
    val matchupInsight = when {
        ratingGap > 50 -> "MMR üstünlüğünü koru; savunma dengesini bozma."
        ratingGap < -50 -> "Rakibin MMR üstünlüğüne karşı kompakt oyun ve hızlı geçişleri dene."
        else -> "Takımlar yakın güçte; oyun disiplini ve ilk skor belirleyici olabilir."
    }

    val myProb = remember(myTeam.rating, selectedOpponent.rating) {
        val diff = myTeam.rating - selectedOpponent.rating
        (52 + (diff / 20)).coerceIn(28, 76)
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
            contentPadding = PaddingValues(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                MSheetHeading("Match Scouter", "Takım karşılaştırması · taktik simülasyon", Icons.Rounded.AutoAwesome, onDismiss)
                Spacer(Modifier.height(12.dp))
                MFeatureHeader("%$teamFit eşleşme", "${myTeam.name} × ${selectedOpponent.name}", Icons.Rounded.Insights, accent = Color(0xFFB9AEFF))
            }

            // Şehir Şampiyonluk Kemeri Hero Kartı (City Championship Belt)
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, MColors.Gold.copy(alpha = 0.5f)),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.premiumSurface().padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(Modifier.weight(1f).padding(end = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("🥋🏆", fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("ŞEHİR ŞAMPİYONLUĞU KEMERİ", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color(0xFFD97706))
                                    Text("Sahip: ${champion.name} · ${champion.record}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MColors.Ink900, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFEF3C7))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(if (champion.id == selectedOpponent.id) "Kemer" else "Hedefle", maxLines = 1, softWrap = false, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                            }
                        }
                    }
                }
            }

            // Rakip Seçici (Yüzen Gölgeli Açılır Menü)
            if (opponents.isNotEmpty()) {
                item {
                    MCard(Modifier.fillMaxWidth(), cornerRadius = 20.dp, elevation = 6.dp) {
                        Column(Modifier.padding(14.dp)) {
                            MFloatingDropdownBox(
                                label = "Rakip Takım",
                                selectedValue = selectedOpponent,
                                options = opponents,
                                optionLabel = { "${it.badge} ${it.name} · ${it.rating} MMR" },
                                onSelect = { selectedOpponent = it },
                                icon = Icons.Rounded.Shield
                            )
                        }
                    }
                }
            }

            // Simülasyon Sonuç Kartı
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, MColors.Mint.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        if (opponents.isEmpty()) {
                            Text("Analiz için eşleşebileceğin bir rakip takım bulunmuyor.", fontSize = 11.sp, color = Color(0xFF14532D))
                        } else {
                            Text("Tahmini güç dengesi · MMR tabanlı demo", fontSize = 10.sp, color = Color(0xFF15803D))
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${myTeam.name} · %$myProb", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${selectedOpponent.name} · %${100 - myProb}", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFDCFCE7))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(myProb / 100f)
                                        .fillMaxHeight()
                                        .background(MColors.Mint)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Demo öneri · $matchupInsight", fontSize = 11.sp, color = Color(0xFF14532D), lineHeight = 15.sp)
                        }
                    }
                }
            }

            // Kemer Maçı İçin Meydan Oku
            item {
                Button(
                    onClick = {
                        onDismiss()
                        onIssueBeltChallenge(selectedOpponent)
                    },
                    enabled = opponents.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = MColors.Brand),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(
                        text = when {
                            opponents.isEmpty() -> "Rakip takım bulunamadı"
                            champion.id == selectedOpponent.id -> "Kemer maçı için meydan oku ⚔️"
                            else -> "Takıma meydan oku ⚔️"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumMatchupPanel(home: TeamProfile, away: TeamProfile) {
    Surface(shape = RoundedCornerShape(20.dp), color = MColors.Ink900,
        border = BorderStroke(1.dp, Color(0xFF384454))) {
        Row(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF283D46), MColors.Ink900))).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(home, away).forEachIndexed { index, team ->
                if (index == 1) {
                    Surface(shape = CircleShape, color = Color(0xFFFFD28C)) {
                        Text("VS", Modifier.padding(9.dp), color = MColors.Ink900, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(shape = RoundedCornerShape(14.dp), color = team.color, border = BorderStroke(1.dp, Color.White.copy(alpha = .25f))) {
                        Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                            Text(team.badge, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(team.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text("${team.rating} MMR", color = Color(0xFFFFD28C), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
