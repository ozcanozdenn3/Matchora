package com.matchora.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.state.*
import com.matchora.app.ui.theme.MColors

private val MintAccent = Color(0xFF9EDDC8)
private val GoldAccent = Color(0xFFFFD28C)

@Composable
private fun SpotlightTitle(
    title: String,
    subtitle: String,
    emblem: String,
    category: String,
    accent: Color,
    compact: Boolean = true,
    codeBadge: String? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(Color(0xFF293D46), MColors.Ink900)))
            .drawBehind {
                val center = Offset(size.width * .94f, size.height * .25f)
                listOf(40.dp, 68.dp, 96.dp).forEach { radius ->
                    drawCircle(accent.copy(alpha = .10f), radius.toPx(), center, style = Stroke(1.dp.toPx()))
                }
            }
            .padding(horizontal = 12.dp, vertical = if (compact) 10.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = accent.copy(alpha = .16f),
            border = BorderStroke(0.8.dp, accent.copy(alpha = .32f)),
            shadowElevation = 0.dp
        ) {
            Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                Text(emblem, color = accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category,
                    color = accent,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = .4.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (!codeBadge.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = Color.White.copy(alpha = 0.10f),
                        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.18f))
                    ) {
                        Text(
                            text = codeBadge,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
            Text(
                text = title,
                fontSize = 14.5.sp,
                lineHeight = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = Color(0xFFCCD8DB),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SpotlightStats(
    values: List<Pair<String, String>>,
    compact: Boolean = true,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Row(modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        values.forEach { (value, label) ->
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF8FAFC),
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    Modifier.padding(horizontal = 9.dp, vertical = if (compact) 7.dp else 8.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(value, color = MColors.Ink900, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(label, color = MColors.Ink500, fontSize = 9.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun CommunitySpotlightCard(
    community: CommunityInfo,
    joined: Boolean,
    requested: Boolean,
    onShare: (() -> Unit)? = null,
    onCardClick: (() -> Unit)? = null,
    onJoin: () -> Unit
) {
    val backendEnabled = LocalBackendRepository.current?.enabled == true
    val placeLabel = listOf(community.area, community.city).filter { it.isNotBlank() }.distinct().joinToString(", ")
    MCard(Modifier.fillMaxWidth(), cornerRadius = 16.dp, elevation = 2.dp, onClick = onCardClick ?: onJoin) {
        SpotlightTitle(
            title = community.name,
            subtitle = placeLabel,
            emblem = community.emoji,
            category = "${community.type.label} · ${community.access.label}",
            accent = MintAccent,
            codeBadge = communityDisplayId(community.id)
        )
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SpotlightStats(
                listOf(
                    "${community.members}" to "Üye",
                    "${community.upcomingEvents}" to "Yaklaşan",
                    community.access.label to "Katılım"
                )
            )
            if (community.description.isNotBlank()) {
                Text(
                    text = community.description,
                    fontSize = 11.5.sp,
                    lineHeight = 15.5.sp,
                    color = MColors.Ink600,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (community.sports.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(community.sports) { sport ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            shadowElevation = 0.dp,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Text(
                                text = "${sport.emoji} ${sport.label}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MColors.Ink700,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                val isOwner = community.ownerId == DemoData.ME
                MButton(when {
                    isOwner -> "Topluluğu yönet"
                    joined -> "Katıldın · Ayrıl"
                    requested -> "Başvuru bekliyor · İptal"
                    community.access == CommunityAccess.OPEN -> "Topluluğa katıl"
                    community.access == CommunityAccess.APPROVAL -> "Katılım başvurusu"
                    else -> "Davet gerekli"
                }, if (isOwner) ({ onCardClick?.invoke(); Unit }) else onJoin, Modifier.weight(1f), enabled = isOwner || joined || requested || community.access != CommunityAccess.INVITE, isPrimary = !joined && !requested && !isOwner, isDestructive = !isOwner && (joined || requested), height = 38.dp)
                if (onShare != null) {
                    FilledTonalButton(
                        onClick = onShare,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Share,
                            if (backendEnabled) "Paylaş" else "Paylaş (+2 TKN)",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RankingSpotlightCard(row: LeaderboardRow, onClick: () -> Unit) {
    val podium = row.rank <= 3
    MCard(Modifier.fillMaxWidth().clickable(onClick = onClick), cornerRadius = 14.dp, elevation = if (podium) 2.dp else 1.dp) {
        Row(Modifier.fillMaxWidth().background(if (row.isMe) MColors.BrandTint else Color.Transparent).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (podium) MColors.Ink900 else Color(0xFFF8FAFC),
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, if (podium) GoldAccent.copy(alpha = 0.35f) else Color(0xFFE2E8F0))
            ) {
                Column(Modifier.width(34.dp).padding(vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${row.rank}", color = if (podium) GoldAccent else MColors.Ink700, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    if (podium) Text("★", color = GoldAccent, fontSize = 8.sp)
                }
            }
            UserAvatar(
                userId = row.playerId,
                name = row.name,
                modifier = Modifier.size(34.dp),
                fallbackColor = if (podium) Color(0xFFE1C49D) else MColors.BrandTint,
                fallbackTextColor = if (podium) MColors.Ink900 else MColors.Brand
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(if (row.isMe) "${row.name} · Sen" else row.name, color = MColors.Ink900, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(row.tier.label, color = MColors.BrandDeep, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    Text("·", color = MColors.Ink400, fontSize = 10.sp)
                    Text("${row.verifiedMatches} maç", color = MColors.Ink500, fontSize = 10.sp)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${row.rating}", color = MColors.Ink900, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    Text("MMR", fontSize = 8.5.sp, color = MColors.Ink400, fontWeight = FontWeight.Bold)
                }
                Text(if (row.delta > 0) "↑ ${row.delta}" else if (row.delta < 0) "↓ ${-row.delta}" else "—", color = if (row.delta < 0) MColors.Rose else MColors.BrandDeep, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TournamentSpotlightCard(
    tournament: TournamentInfo,
    onShare: (() -> Unit)? = null,
    onOpen: () -> Unit
) {
    val backendEnabled = LocalBackendRepository.current?.enabled == true
    MCard(Modifier.fillMaxWidth(), cornerRadius = 16.dp, elevation = 2.dp, onClick = onOpen) {
        SpotlightTitle(
            title = tournament.name,
            subtitle = "${tournament.format} · ${tournament.dates}",
            emblem = tournament.displaySportEmoji,
            category = tournament.status,
            accent = GoldAccent,
            codeBadge = tournamentDisplayId(tournament.id)
        )
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SpotlightStats(
                listOf(
                    "${tournament.participants}/${tournament.capacity}" to "Katılım",
                    if (tournament.isLeague) "Lig" to "Tür" else "Kupa" to "Tür"
                )
            )
            LinearProgressIndicator(
                progress = { (tournament.participants.toFloat() / tournament.capacity.coerceAtLeast(1)).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = MColors.Brand,
                trackColor = MColors.BrandTint
            )
            Text(
                text = listOf(tournament.organizer, tournament.requirement).filter { it.isNotBlank() }.joinToString(" · "),
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = MColors.Ink600,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                MButton("Fikstür ve kayıt", onOpen, Modifier.weight(1f), height = 38.dp)
                if (onShare != null) {
                    FilledTonalButton(
                        onClick = onShare,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Share,
                            if (backendEnabled) "Paylaş" else "Paylaş (+2 TKN)",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TeamSpotlightCard(team: TeamProfile, primaryLabel: String, onPrimary: () -> Unit, onDetails: () -> Unit, compact: Boolean = true) {
    MCard(Modifier.fillMaxWidth(), cornerRadius = 16.dp, elevation = 2.dp, onClick = onDetails) {
        SpotlightTitle(
            title = team.name,
            subtitle = "${team.sport.emoji} ${team.sport.label} · ${team.area}",
            emblem = team.badge,
            category = if (team.isMine) "TAKIMIN · KAPTANSIN" else if (team.lookingForOpponent) "RAKİP ARIYOR" else "KULÜP PROFİLİ",
            accent = if (team.isMine) GoldAccent else MintAccent,
            compact = compact,
            codeBadge = teamDisplayId(team.id)
        )
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SpotlightStats(listOf("${team.rating}" to "MMR", "${team.wins}" to "Galibiyet", "${team.memberIds.size}" to "Kadro"), compact)
            Text(team.record, fontSize = 11.sp, color = MColors.Ink600, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MButton("Kadro", onDetails, Modifier.weight(1f), isPrimary = false, height = 38.dp)
                MButton(primaryLabel, onPrimary, Modifier.weight(1.4f), height = 38.dp)
            }
        }
    }
}

@Composable
fun ChallengeSpotlightCard(challenge: ChallengeOffer, opponent: TeamProfile, onDetails: () -> Unit, onAccept: () -> Unit, onDecline: () -> Unit, onChat: () -> Unit, canRespond: Boolean = true) {
    var confirmDecline by remember(challenge.id) { mutableStateOf(false) }
    MCard(Modifier.fillMaxWidth(), cornerRadius = 16.dp, elevation = 2.dp, onClick = onDetails) {
        Column(Modifier.clickable(onClick = onDetails)) {
            SpotlightTitle(opponent.name, if (challenge.incoming) "Gelen meydan okuma · Detayları aç →" else "Gönderilen teklif · Detayları aç →", opponent.badge, challenge.status.label, GoldAccent)
        }
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(challenge.proposedTime, fontSize = 13.sp, color = MColors.Ink900, fontWeight = FontWeight.Bold)
            Text(challenge.venue, fontSize = 11.sp, color = MColors.Ink500)
            if (!challenge.note.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    shadowElevation = 0.dp,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(challenge.note, Modifier.fillMaxWidth().padding(9.dp), fontSize = 11.sp, color = MColors.Ink700)
                }
            }
            if (canRespond && challenge.incoming && challenge.status == ChallengeStatus.PENDING) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MButton("Kabul et", onAccept, Modifier.weight(1f), height = 38.dp)
                    MButton("Reddet", { confirmDecline = true }, Modifier.weight(1f), isPrimary = false, isDestructive = true, height = 38.dp)
                }
                TextButton(onClick = onChat, modifier = Modifier.fillMaxWidth().height(36.dp), contentPadding = PaddingValues(0.dp)) { Text("Kaptana mesaj gönder", fontSize = 12.sp, color = MColors.Ink700) }
            }
        }
    }
    if (confirmDecline) {
        AlertDialog(
            onDismissRequest = { confirmDecline = false },
            title = { Text("Meydan okumayı reddet?", fontWeight = FontWeight.Bold) },
            text = { Text("${opponent.name} takımının ${challenge.proposedTime} teklifi reddedilecek.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDecline = false
                        onDecline()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MColors.Rose)
                ) {
                    Text("Reddet", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDecline = false }) { Text("Vazgeç") }
            }
        )
    }
}
