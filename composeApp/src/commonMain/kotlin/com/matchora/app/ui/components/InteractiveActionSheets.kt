package com.matchora.app.ui.components

import com.matchora.app.ui.components.premiumSurface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.domain.Sport
import com.matchora.app.state.*
import com.matchora.app.ui.theme.MColors

// ═══════════════════════════════════════════════════════════════════════════════
// 1. SPORTS CHEMISTRY™ OTOMATİK KADRO YÖNETİM MODALI
// ═══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoSquadSheet(
    event: SportEvent?,
    currentPlayer: Player = DemoData.mePlayer,
    onDismiss: () -> Unit,
    onConfirmSquad: () -> Unit,
    onOpenChat: () -> Unit
) {
    val participants = remember(event, currentPlayer) {
        event?.participantIds?.distinct()?.mapNotNull { id -> if (id == DemoData.ME) currentPlayer else DemoData.player(id) }.orEmpty()
    }

    ModalBottomSheet(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        scrimColor = androidx.compose.ui.graphics.Color(0xFF0B1220).copy(alpha = .48f),

        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF8FAFC),
        dragHandle = { MPremiumSheetHandle() }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                MSheetHeading(
                    title = "Maç kadrosu",
                    subtitle = "Katılımcılar ve açık kontenjan",
                    icon = Icons.Rounded.Groups,
                    onDismiss = onDismiss
                )
            }

            if (event == null) {
                item {
                    MSubCard(backgroundColor = Color.White) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.Groups, null, tint = MColors.Ink400, modifier = Modifier.size(30.dp))
                            Text("Önce bir karşılaşma seç", fontWeight = FontWeight.Bold, color = MColors.Ink900)
                            Text("Maç seçildiğinde katılımcı ve kontenjan bilgileri burada görünür.", color = MColors.Ink500, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, MColors.Line)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(MColors.BrandTint),
                                contentAlignment = Alignment.Center
                            ) { Text(event.displaySportEmoji, fontSize = 22.sp) }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(event.title, fontWeight = FontWeight.Bold, color = MColors.Ink900, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("${event.day} · ${event.time} · ${event.area}", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SquadSummaryTile("${participants.size}", "Katılımcı", Modifier.weight(1f))
                        SquadSummaryTile("${event.freeSpots}", "Boş yer", Modifier.weight(1f))
                        SquadSummaryTile("TKN ${event.pricePerPersonTry}", "Kişi başı", Modifier.weight(1f))
                    }
                }

                item {
                    Text("KATILIMCILAR", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = MColors.Ink500)
                }

                if (participants.isEmpty()) {
                    item {
                        MSubCard(backgroundColor = Color.White) {
                            Text("Henüz katılımcı yok.", modifier = Modifier.padding(4.dp), color = MColors.Ink500, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else {
                    items(participants, key = { it.id }) { player ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, MColors.Line)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(38.dp).clip(CircleShape).background(player.color),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(player.initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(player.name, fontWeight = FontWeight.Bold, color = MColors.Ink900, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(listOfNotNull(player.position, "${player.rating} MMR").joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MColors.Ink500, maxLines = 1)
                                }
                                if (player.verified) {
                                    Icon(Icons.Rounded.Verified, "Doğrulanmış profil", tint = MColors.Mint, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onConfirmSquad,
                        enabled = event != null,
                        colors = ButtonDefaults.buttonColors(containerColor = MColors.Brand),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Maç ayrıntılarını aç", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onOpenChat,
                        enabled = event != null,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("Maç sohbetine git", color = MColors.Ink900, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SquadSummaryTile(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MColors.Line)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
            Text(value, fontWeight = FontWeight.Bold, color = MColors.Ink900)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MColors.Ink500, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 2. YEREL SIRALAMA OYUNCU PROFİLİ & ATLETİK PASAPORT MODALI
// ═══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerProfileSheet(
    name: String,
    initials: String,
    rating: Int,
    rank: Int,
    player: Player,
    isInvitedToTeam: Boolean = false,
    sport: Sport = Sport.FOOTBALL,
    onDismiss: () -> Unit,
    onChallenge: () -> Unit,
    onInviteTeam: () -> Unit,
    onDirectMessage: () -> Unit = {}
) {
    val isCurrentUser = player.id == DemoData.ME

    ModalBottomSheet(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        scrimColor = androidx.compose.ui.graphics.Color(0xFF0B1220).copy(alpha = .48f),

        onDismissRequest = onDismiss,
        modifier = Modifier.statusBarsPadding().padding(top = 12.dp),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFFF7F7F4),
        dragHandle = { MPremiumSheetHandle() }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Atletik Başlık Kartı & Pasaport ID
            item {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = MColors.Ink900,
                    border = BorderStroke(1.dp, Color(0xFF34445A)),
                    shadowElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.weight(1f).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(Color(0xFFFFC078), MColors.Brand))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(name, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (player.verified) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(Icons.Rounded.Verified, null, tint = MColors.Mint, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${sport.emoji} ${sport.label} · ${player.area} · $rank. sırada",
                                        fontSize = 12.sp,
                                        color = Color(0xFFD0D9E4),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "ATLET PASAPORTU: ${player.id.uppercase()} · ${player.position ?: player.level.label}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                        color = Color(0xFFFFC078)
                                    )
                                }
                            }

                            // MMR Rozeti
                            Column(horizontalAlignment = Alignment.End) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MColors.Brand)
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "$rating MMR",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Tier: ${player.tier.label}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFCFD6E0)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Performans İstatistik Matrisi
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tile 1: Resmi Maç
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                        ) {
                            Text("${player.verifiedMatches}", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = MColors.Brand)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("DOĞR. MAÇ", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MColors.Ink600, maxLines = 1)
                        }
                    }
                    // Tile 2: Galibiyet
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                        ) {
                            Text("%${player.reliability}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MColors.Mint)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("GÜVENİLİRLİK", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MColors.Ink600, maxLines = 1)
                        }
                    }
                    // Tile 3: Fair play with vector star
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(player.chemistry?.let { "%$it" } ?: "—", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                                Spacer(modifier = Modifier.width(2.dp))
                                if (player.chemistry != null) Icon(Icons.Rounded.AutoAwesome, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("UYUM", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MColors.Ink600, maxLines = 1)
                        }
                    }
                    // Tile 4: Güvenilirlik
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                        ) {
                            Text("${player.playedTogether}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MColors.Mint)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("BİRLİKTE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MColors.Ink600, maxLines = 1)
                        }
                    }
                }
            }

            // 3. AI Maç & Taktik Keşif Raporu (Matchora AI Scouting)
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFFEAF3ED),
                    border = BorderStroke(1.dp, Color(0xFFC9DECF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("✦", fontSize = 19.sp, color = Color(0xFF315B40))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "OYUN PROFİLİ",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = Color(0xFF166534)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFDCFCE7))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("MATCHORA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${player.sports.firstOrNull()?.label ?: sport.label} · ${player.position ?: player.level.label} · Reliability %${player.reliability}. ${if (player.availableNow) "Şu an müsait." else "Planlı katılıma açık."}",
                            fontSize = 14.sp,
                            color = Color(0xFF244C33),
                            lineHeight = 20.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 4. Son 5 Maç Formu & Puanlar
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0x10000000)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Son maç formu", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MColors.Ink800)
                            Text("Son karşılaşmalardaki puanların", fontSize = 11.sp, color = MColors.Ink500)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                Pair("W", "8.7"),
                                Pair("W", "8.2"),
                                Pair("W", "9.1"),
                                Pair("L", "7.3"),
                                Pair("W", "8.9")
                            ).forEach { (res, ratingVal) ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(if (res == "W") MColors.Mint else MColors.Rose),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(res, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(ratingVal, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MColors.Ink600)
                                }
                            }
                        }
                    }
                }
            }

            // 5. Rozetler & Atlet Başarıları
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "🏅 Sezon 3 MVP" to MColors.Gold,
                        "🔥 12 Maç Namağlup" to MColors.Brand,
                        "🤝 FairPlay Lideri" to MColors.Mint
                    ).forEach { (badgeTitle, color) ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = color.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = badgeTitle,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = color,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 6. Aksiyon Butonları
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onDirectMessage()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MColors.Brand),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Icon(Icons.Rounded.ChatBubbleOutline, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Direkt Mesaj Gönder 💬", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onChallenge()
                                onDismiss()
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0x25000000)),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text("Meydan Oku ⚔️", fontWeight = FontWeight.Bold, color = MColors.Ink900, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                onInviteTeam()
                                onDismiss()
                            },
                            enabled = !isInvitedToTeam && !isCurrentUser,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MColors.Brand),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text(
                                when {
                                    isCurrentUser -> "Kendi profilin"
                                    isInvitedToTeam -> "Davet gönderildi ✓"
                                    else -> "Takıma Davet Et 🛡️"
                                },
                                fontWeight = FontWeight.Bold,
                                color = if (isInvitedToTeam) MColors.Mint else MColors.Brand,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 3. SAHA VE KORT REZERVASYON MODALI (Takım İle Hesap Bölüşme Seçenekli)
// ═══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VenueBookingSheet(
    venue: VenueInfo,
    slot: VenueSlotInfo,
    onDismiss: () -> Unit,
    onConfirmBooking: (splitCount: Int, finalAmount: Int, method: String) -> Unit
) {
    var splitPlayers by remember { mutableIntStateOf(1) } // 1, 2, 4, 10 kişi
    var selectedMethod by remember { mutableStateOf("Tesiste ödeme") }
    val pricePerPlayer = bookingShare(slot.priceTry, splitPlayers)

    ModalBottomSheet(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        scrimColor = androidx.compose.ui.graphics.Color(0xFF0B1220).copy(alpha = .48f),

        onDismissRequest = onDismiss,
        containerColor = MColors.Canvas,
        modifier = Modifier.statusBarsPadding().padding(top = 12.dp),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { MPremiumSheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MSheetHeading("Rezervasyon", "Saatini ayır, ekibini hazırla", Icons.Rounded.EventAvailable, onDismiss)
            MFeatureHeader(venue.name, "${venue.area} · ${slot.court}", Icons.Rounded.Stadium, accent = Color(0xFF9EDDC8))
            MCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(slot.day, style = MaterialTheme.typography.labelMedium, color = MColors.Ink500)
                        Text(slot.time, style = MaterialTheme.typography.headlineMedium, color = MColors.Ink900)
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Toplam", style = MaterialTheme.typography.labelMedium, color = MColors.Ink500)
                        Text("₺${slot.priceTry}", style = MaterialTheme.typography.headlineSmall, color = MColors.BrandDeep)
                    }
                }
            }

            // Takımla Bölüş Seçeneği (Yüzen Gölgeli Açılır Menü)
            MCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MFloatingDropdownBox(
                        label = "Ücreti Takımla Bölüş",
                        selectedValue = splitPlayers,
                        options = listOf(1, 2, 4, 10),
                        optionLabel = { count ->
                            val perPlayerPrice = bookingShare(slot.priceTry, count)
                            if (count == 1) "Tek Kişi · ₺$perPlayerPrice" else "$count Kişi · Kişi Başı ₺$perPlayerPrice"
                        },
                        onSelect = { splitPlayers = it },
                        icon = Icons.Rounded.Groups
                    )
                }
            }

            Text("Tesiste ödeme · dijital token bakiyen kullanılmaz", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)

            // Onay Butonu
            Button(
                onClick = {
                    onConfirmBooking(splitPlayers, pricePerPlayer, selectedMethod)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MColors.Brand),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("₺$pricePerPlayer · Yerimi ayır", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 4. RESMİ TURNUVA & LİG KUPASI MERKEZİ (Fikstür, Puan Durumu, SafePlay & AI Tahmin)
// ═══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentDetailSheet(
    tournament: TournamentInfo,
    isRegistered: Boolean = false,
    isOrganizer: Boolean = false,
    onDismiss: () -> Unit,
    onRegisterTeam: () -> Unit,
    onWithdrawRegistration: () -> Unit = {},
    onCancelTournament: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Fikstür & Eşleşme, 1: Puan Durumu, 2: Kurallar, 3: AI Şampiyonluk Radarı
    var showCancelConfirmation by remember { mutableStateOf(false) }
    var showWithdrawConfirmation by remember { mutableStateOf(false) }
    val statusAllowsRegistration = tournament.status.contains("açık", ignoreCase = true) ||
        tournament.status.contains("son ", ignoreCase = true)
    val canRegister = !isOrganizer && !isRegistered && statusAllowsRegistration && tournament.participants < tournament.capacity
    val canCancelTournament = isOrganizer && tournament.organizerLockTokens > 0 &&
        !tournament.status.contains("iptal", ignoreCase = true)
    val statusColor = when {
        tournament.status.contains("iptal", ignoreCase = true) -> MColors.Rose
        statusAllowsRegistration -> MColors.Mint
        else -> MColors.Ink600
    }
    val statusTint = when {
        tournament.status.contains("iptal", ignoreCase = true) -> MColors.RoseTint
        statusAllowsRegistration -> MColors.MintTint
        else -> MColors.SurfaceMuted
    }
    val aiEntries = remember(tournament) {
        val standings = tournament.standings
            .sortedByDescending { it.points }
            .take(4)
            .map { it.team to it.points.coerceAtLeast(1).toFloat() }
        if (standings.isNotEmpty()) standings else {
            tournament.bracket
                .flatMap { listOf(it.teamA, it.teamB) }
                .filter { it.isNotBlank() && it != "TBD" }
                .distinct()
                .take(4)
                .mapIndexed { index, name -> name to (4 - index).coerceAtLeast(1).toFloat() }
        }
    }
    val aiWeightTotal = aiEntries.sumOf { it.second.toDouble() }.toFloat().coerceAtLeast(1f)

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
            // 0. Başlık
            item {
                MSheetHeading(
                    title = tournament.name,
                    subtitle = "${tournament.displaySportLabel} · ${tournament.format} · ${tournament.dates}",
                    icon = Icons.Rounded.EmojiEvents,
                    onDismiss = onDismiss
                )
            }

            // 1. Üst Turnuva / Lig Hero Kartı
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.premiumSurface().padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.weight(1f).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MColors.Brand),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🏆", fontSize = 24.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = tournament.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = MColors.Ink900,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${tournament.displaySportEmoji} ${tournament.displaySportLabel} · ${tournament.format} · ${tournament.dates}",
                                        fontSize = 11.sp,
                                        color = MColors.Ink500,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "📍 ${tournament.organizer}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MColors.Brand,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(statusTint)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(tournament.status, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            // 2. Katılım ve kontenjan bilgisi
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFFF7ED),
                    border = BorderStroke(1.dp, MColors.Brand.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ÜCRET / ÖDÜL BİLGİSİ", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = MColors.Brand)
                            MBadge(
                                text = "${tournament.participants}/${tournament.capacity} kişi",
                                textColor = MColors.Ink700,
                                backgroundColor = Color.White
                            )
                        }
                        Text(tournament.feeNote, fontSize = 12.sp, lineHeight = 17.sp, color = MColors.Ink700)
                    }
                }
            }

            // 3. İç Sekme Seçici (Fikstür | Puan Tablosu | Kurallar | AI Radar)
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF1F5F9),
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
                        val tabs = listOf(
                            "Fikstür" to "⚔️",
                            "Puanlar" to "📊",
                            "Kurallar" to "📜",
                            "AI Radar" to "🤖"
                        )
                        tabs.forEachIndexed { idx, (label, icon) ->
                            val isSel = selectedTab == idx
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .then(
                                        if (isSel) {
                                            Modifier
                                                .background(Color.White)
                                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                        } else {
                                            Modifier.background(Color.Transparent)
                                        }
                                    )
                                    .clickable { selectedTab = idx },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(icon, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSel) MColors.Brand else Color(0xFF64748B),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Sekme İçerikleri
            when (selectedTab) {
                0 -> {
                    // Fikstür & Eşleşmeler
                    if (tournament.bracket.isNotEmpty()) {
                        items(tournament.bracket) { match ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0x12000000)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${match.teamA}  vs  ${match.teamB}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MColors.Ink900
                                        )
                                        Text(
                                            text = "${tournament.organizer} · ${tournament.dates}",
                                            fontSize = 10.sp,
                                            color = MColors.Ink500,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (match.scoreA != null) MColors.BrandTint else Color(0xFFE2E8F0))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (match.scoreA != null) "${match.scoreA} - ${match.scoreB}" else "Bekliyor ⏳",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (match.scoreA != null) MColors.Brand else MColors.Ink600
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Text(
                                if (tournament.status.contains("açık", ignoreCase = true)) "Kayıtlar tamamlandığında fikstür burada görünecek."
                                else "Bu etkinlik için fikstür bilgisi paylaşılmadı.",
                                fontSize = 12.sp,
                                color = MColors.Ink600
                            )
                        }
                    }
                }
                1 -> {
                    // Puan Durumu
                    if (tournament.standings.isNotEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0x10000000)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("KULÜP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MColors.Ink400, modifier = Modifier.weight(1f))
                                        Text("O  G  B  M  AV  P", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MColors.Ink400)
                                    }
                                    HorizontalDivider(color = Color(0x10000000))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    tournament.standings.forEachIndexed { i, s ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${i + 1}.",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (i < 2) MColors.Mint else MColors.Ink600,
                                                    modifier = Modifier.width(18.dp)
                                                )
                                                Text(s.team, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MColors.Ink900)
                                            }
                                            Text(
                                                text = "${s.played}   ${s.won}   ${s.drawn}   ${s.lost}   ${s.goalDiff}   ${s.points}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (i < 2) MColors.Mint else MColors.Ink800
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Text("Bu turnuvada henüz puan tablosu oluşmadı.", fontSize = 12.sp, color = MColors.Ink600)
                        }
                    }
                }
                2 -> {
                    // Turnuva Kuralları & Tescil
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0x10000000)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Gavel, null, tint = MColors.Brand, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("TURNUVA BİLGİLERİ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MColors.Ink900)
                                }
                                Text("Format · ${tournament.format}", fontSize = 11.sp, color = MColors.Ink700, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("Tarih · ${tournament.dates}", fontSize = 11.sp, color = MColors.Ink700)
                                Text("Katılım · ${tournament.requirement}", fontSize = 11.sp, color = MColors.Ink700, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                Text("Bilgi · ${tournament.feeNote}", fontSize = 11.sp, color = MColors.Ink700, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                3 -> {
                    // Mock AI tournament analysis
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, MColors.Mint.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🤖 TURNUVA ANALİZİ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                                    Text("DEMO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Örnek dağılım", fontSize = 10.sp, color = Color(0xFF15803D))
                                if (aiEntries.isEmpty()) {
                                    Text("Analiz için fikstür veya puan verisi bekleniyor.", fontSize = 11.sp, color = Color(0xFF166534))
                                }
                                aiEntries.forEach { (team, weight) ->
                                    val fraction = (weight / aiWeightTotal).coerceIn(0f, 1f)
                                    val percent = (fraction * 100).toInt()
                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(team, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF14532D), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("%$percent", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(Color(0xFFDCFCE7))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(fraction)
                                                    .fillMaxHeight()
                                                    .background(MColors.Mint)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Kayıt durumu
            item {
                Button(
                    onClick = {
                        onRegisterTeam()
                        onDismiss()
                    },
                    enabled = canRegister,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MColors.Brand,
                        disabledContainerColor = MColors.SurfaceMuted
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            when {
                                tournament.status.contains("iptal", ignoreCase = true) -> "Turnuva iptal edildi"
                                isOrganizer -> "Organizatör görünümü"
                                isRegistered -> "Kaydın alındı ✓"
                                tournament.participants >= tournament.capacity -> "Kontenjan doldu"
                                !statusAllowsRegistration -> "Kayıt süreci kapalı"
                                else -> "Turnuvaya kaydol 🏆"
                            },
                            color = if (canRegister) Color.White else MColors.Ink600,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            if (canCancelTournament) {
                item {
                    OutlinedButton(
                        onClick = { showCancelConfirmation = true },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MColors.Rose.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("Turnuvayı iptal et · ${tournament.organizerLockTokens} TKN iade", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MColors.Rose)
                    }
                }
            }

            if (isRegistered && statusAllowsRegistration) {
                item {
                    OutlinedButton(
                        onClick = { showWithdrawConfirmation = true },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MColors.Line),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("Kaydı geri çek", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MColors.Ink600)
                    }
                }
            }
        }

        if (showCancelConfirmation) {
            AlertDialog(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        containerColor = androidx.compose.ui.graphics.Color(0xFFFAFBFC),
        tonalElevation = 0.dp,

                onDismissRequest = { showCancelConfirmation = false },
                title = { Text("Turnuva iptal edilsin mi?") },
                text = { Text("Demo turnuva yayından kaldırılacak ve ${tournament.organizerLockTokens} TKN kilidi bakiyene dönecek.") },
                confirmButton = {
                    TextButton(onClick = {
                        showCancelConfirmation = false
                        onCancelTournament()
                        onDismiss()
                    }) { Text("İptal et", color = MColors.Rose) }
                },
                dismissButton = {
                    TextButton(onClick = { showCancelConfirmation = false }) { Text("Vazgeç") }
                }
            )
        }

        if (showWithdrawConfirmation) {
            AlertDialog(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        containerColor = androidx.compose.ui.graphics.Color(0xFFFAFBFC),
        tonalElevation = 0.dp,

                onDismissRequest = { showWithdrawConfirmation = false },
                title = { Text("Kaydını geri çek?") },
                text = { Text("Turnuva başvurun iptal edilecek. Kayıtlar açık olduğu sürece yeniden katılabilirsin.") },
                confirmButton = {
                    TextButton(onClick = {
                        showWithdrawConfirmation = false
                        onWithdrawRegistration()
                        onDismiss()
                    }) { Text("Kaydı geri çek", color = MColors.Rose) }
                },
                dismissButton = {
                    TextButton(onClick = { showWithdrawConfirmation = false }) { Text("Vazgeç") }
                }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 5. YETENEK & MMR ANALİZ DİYALOĞU
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun SkillBreakdownDialog(
    skill: SportSkill,
    area: String = DemoData.me.area,
    onDismiss: () -> Unit
) {
    val nextTier = Tier.entries.firstOrNull { it.minRating > skill.rating }
    val tierFloor = skill.tier.minRating
    val progress = nextTier?.let { next ->
        ((skill.rating - tierFloor).toFloat() / (next.minRating - tierFloor).coerceAtLeast(1)).coerceIn(0f, 1f)
    }

    AlertDialog(
        containerColor = androidx.compose.ui.graphics.Color(0xFFFAFBFC),
        tonalElevation = 0.dp,

        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(skill.sport.emoji, fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("${skill.sport.label} derecesi", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("${skill.rating} MMR · ${skill.tier.label}", fontWeight = FontWeight.Bold, color = MColors.Brand)
                        Text("Kazanma oranı %${skill.winRate}", fontSize = 12.sp, color = MColors.Ink700)
                        Text("${skill.verifiedMatches} doğrulanmış maç", fontSize = 12.sp, color = MColors.Ink700)
                        skill.position?.takeIf { it.isNotBlank() }?.let {
                            Text("Pozisyon / oyun stili: $it", fontSize = 12.sp, color = MColors.Ink700)
                        }
                        Text(
                            skill.localRank?.let { "${skill.localRank}. sıra · $area" } ?: "Yerel sıralama henüz oluşmadı",
                            fontSize = 12.sp,
                            color = MColors.Ink600
                        )
                    }
                }
                if (nextTier != null && progress != null) {
                    Text("${nextTier.minRating - skill.rating} MMR sonra ${nextTier.label}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MColors.Ink700)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = MColors.Brand,
                        trackColor = MColors.SurfaceMuted
                    )
                } else {
                    Text("En üst kademe", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MColors.Gold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat", fontWeight = FontWeight.Bold, color = MColors.Brand)
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// 6. ROZET DETAY DİYALOĞU
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun BadgeDetailDialog(
    emoji: String,
    title: String,
    desc: String,
    earned: Boolean,
    progress: Float,
    onDismiss: () -> Unit
) {
    AlertDialog(
        containerColor = androidx.compose.ui.graphics.Color(0xFFFAFBFC),
        tonalElevation = 0.dp,

        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 26.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(desc, style = MaterialTheme.typography.bodyMedium, color = MColors.Ink800)
                if (earned) {
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = Color(0xFFECFDF5),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Rozet kazanıldı ve pasaportunda görünüyor.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MColors.Mint,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                } else {
                    val percent = (progress.coerceIn(0f, 1f) * 100).toInt()
                    Text("İlerleme · %$percent", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MColors.Ink700)
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = MColors.Brand,
                        trackColor = MColors.SurfaceMuted
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat", fontWeight = FontWeight.Bold, color = MColors.Brand)
            }
        }
    )
}
