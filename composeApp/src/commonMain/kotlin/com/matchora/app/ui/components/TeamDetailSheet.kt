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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.state.DemoData
import com.matchora.app.state.Player
import com.matchora.app.state.TeamProfile
import com.matchora.app.state.emoji
import com.matchora.app.state.label
import com.matchora.app.ui.theme.MColors

// ═══════════════════════════════════════════════════════════════════════════════
// TEAM DETAIL & ROSTER SHEET — KULÜP DETAYI, KUPA DOLABI & OYUNCU KADROSU
// Takım istatistikleri, kupa vitrini, AI taktik kimyası, oyuncu listesi, meydan okuma
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDetailSheet(
    team: TeamProfile,
    currentPlayer: Player = DemoData.mePlayer,
    myTeam: TeamProfile?,
    pendingRequestsCount: Int = 0,
    onDismiss: () -> Unit,
    onOpenChallenge: () -> Unit,
    onApplyToTeam: () -> Unit,
    onManageRequests: () -> Unit,
    onOpenCaptainChat: (Player) -> Unit
) {
    val captainPlayer = remember(team.captainId, currentPlayer) {
        if (team.captainId == DemoData.ME) currentPlayer else DemoData.player(team.captainId) ?: currentPlayer
    }

    val rosterPlayers = remember(team.memberIds, currentPlayer) {
        team.memberIds.mapNotNull { id -> if (id == DemoData.ME) currentPlayer else DemoData.player(id) }
    }
    val matchCount = team.wins + team.draws + team.losses
    val winRate = if (matchCount > 0) team.wins * 100 / matchCount else 0
    val fairPlayScore = (team.fairPlay.coerceIn(0f, 5f) * 20f).toInt()
    val teamFit = if (matchCount > 0) {
        ((winRate + team.reliability + fairPlayScore) / 3).coerceIn(0, 100)
    } else {
        ((team.reliability + fairPlayScore) / 2).coerceIn(0, 100)
    }
    val teamInsight = if (matchCount > 0) {
        "${team.record} · $matchCount maç. Galibiyet oranı %$winRate, güvenilirlik %${team.reliability}."
    } else {
        "Maç kaydı geldikçe takım özeti güncellenecek. Güvenilirlik %${team.reliability}; kadroda ${rosterPlayers.size} oyuncu var."
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
            // 1. Kulüp Hero Kartı & Başlık
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MColors.Ink900,
                    border = BorderStroke(1.dp, team.color.copy(alpha = .3f)),
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.background(Brush.linearGradient(listOf(Color(0xFF293D46), MColors.Ink900))).padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.weight(1f).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(58.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Brush.linearGradient(listOf(team.color, team.color.copy(alpha = .76f)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = team.badge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = team.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (team.isMine) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MColors.Brand)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Kaptansın",
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${team.sport.emoji} ${team.sport.label} · ${team.area} · ${team.tier.label}",
                                        fontSize = 11.sp,
                                        color = Color(0xFFCCD8DB),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "KULÜP ID: TR-FC-${team.id.takeLast(3).uppercase()} · ${team.memberIds.size} Lisanslı Sporcu",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                        color = Color(0xFFFFD28C)
                                    )
                                }
                            }

                            Surface(
                                onClick = onDismiss,
                                shape = CircleShape,
                                color = Color(0xFF283748),
                                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.16f)),
                                shadowElevation = 4.dp,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Close, "Kapat", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // 2. Metrik Matrisi (4'lü Temiz Kart)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tile 1: MMR
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
                            Text("${team.rating}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MColors.Brand)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("MMR PUANI", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MColors.Ink500)
                        }
                    }
                    // Tile 2: G / B / M
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
                            Text(team.record, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MColors.Ink900)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("G / B / M", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MColors.Ink500)
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
                                Text("${team.fairPlay}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(Icons.Rounded.Star, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("CENTİLMENLİK", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MColors.Ink500)
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
                            Text("%${team.reliability}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MColors.Mint)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("GÜVENİLİRLİK", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MColors.Ink500)
                        }
                    }
                }
            }

            // 3. Kulüp Kupa Vitrini (Club Trophy Cabinet)
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, MColors.Gold.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🏆", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "KUPA DOLABI · DEMO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = Color(0xFF92400E)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (team.achievements.isNotEmpty()) {
                            val achievementColors = listOf(Color(0xFFD97706), Color(0xFFB45309), MColors.Brand, MColors.Mint)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(team.achievements.take(8).mapIndexed { index, title ->
                                    title to achievementColors[index % achievementColors.size]
                                }) { (achievement, color) ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White)
                                            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(achievement, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        } else {
                            Text("Kazanılan başarılar burada görünecek.", fontSize = 11.sp, color = MColors.Ink500)
                        }
                    }
                }
            }

            // 4. AI Kadro Kimyası & Taktik Raporu
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🤖", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "DEMO · TAKIM ÖZETİ",
                                    fontSize = 11.sp,
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
                                Text("Uyum %$teamFit", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = teamInsight,
                            fontSize = 11.sp,
                            color = Color(0xFF14532D),
                            lineHeight = 15.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 5. Kulüp Hakkında & Hedef
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0x10000000)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.premiumSurface().padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "KULÜP HAKKINDA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MColors.Ink400
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (team.recruitingOpen) MColors.MintTint else Color(0xFFF1F5F9))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (team.recruitingOpen) "Kadro Alımı Açık" else "Kadro Kapalı",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (team.recruitingOpen) MColors.Mint else MColors.Ink500
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = team.description,
                            fontSize = 12.sp,
                            color = MColors.Ink700,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "🗓️ Maç Saatleri: ${team.availability} · Min Aranan: ${team.requiredMmr} MMR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MColors.Ink500
                        )
                    }
                }
            }

            // 6. Oyuncu Kadrosu (Roster)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TAKIM KADROSU (${rosterPlayers.size} Sporcu)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MColors.Ink400
                    )
                    Text(
                        text = "Kaptan: ${captainPlayer.name}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MColors.Brand
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rosterPlayers.forEach { player ->
                        val isCaptain = player.id == team.captainId
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, if (isCaptain) MColors.Brand.copy(alpha = .35f) else MColors.Line),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 11.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(modifier = Modifier.weight(1f).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(player.color.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = player.name.take(1),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = player.color
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f, fill = false)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = player.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MColors.Ink900,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (isCaptain) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("👑 Kaptan", fontSize = 10.sp, color = MColors.Brand, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Text(
                                            text = "${player.position} · ${player.rating} MMR",
                                            fontSize = 11.sp,
                                            color = MColors.Ink500,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MColors.MintTint)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "%${player.reliability} Güvenilirlik",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MColors.Mint
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 7. Aksiyon Butonları
            item {
                if (team.isMine) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MButton(
                            text = if (pendingRequestsCount > 0) "Başvuruları İncele ($pendingRequestsCount) 👥" else "Başvuruları Yönet 👥",
                            onClick = onManageRequests,
                            icon = Icons.Rounded.Groups
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MButton("Meydan Oku", onOpenChallenge, icon = Icons.Rounded.SportsKabaddi)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (team.recruitingOpen) {
                                MButton("Kadroya Katıl", onApplyToTeam, Modifier.weight(1f), icon = Icons.Rounded.PersonAddAlt1, isPrimary = false, height = 44.dp)
                            }

                            MButton("Kaptana Mesaj", { onOpenCaptainChat(captainPlayer) }, Modifier.weight(1f), icon = Icons.Rounded.ChatBubbleOutline, isPrimary = false, height = 44.dp)
                        }
                    }
                }
            }
        }
    }
}
