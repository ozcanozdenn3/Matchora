package com.matchora.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
private fun SpotlightTitle(title: String, subtitle: String, emblem: String, category: String, accent: Color, compact: Boolean = true) {
    Row(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF293D46), MColors.Ink900)))
        .drawBehind {
            val center = Offset(size.width * .94f, size.height * .25f)
            listOf(40.dp, 68.dp, 96.dp).forEach { radius ->
                drawCircle(accent.copy(alpha = .10f), radius.toPx(), center, style = Stroke(1.dp.toPx()))
            }
        }.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(11.dp),
            color = accent.copy(alpha = .16f),
            border = BorderStroke(0.8.dp, accent.copy(alpha = .32f)),
            shadowElevation = 4.dp,
            modifier = Modifier.shadow(6.dp, RoundedCornerShape(11.dp), ambientColor = Color(0x55000000), spotColor = Color(0x77000000))
        ) {
            Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Text(emblem, color = accent, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(category, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .5.sp)
            Text(title, fontSize = 14.5.sp, lineHeight = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Text(subtitle, fontSize = 10.5.sp, lineHeight = 14.sp, color = Color(0xFFCCD8DB))
        }
    }
}

@Composable
private fun SpotlightStats(values: List<Pair<String, String>>, compact: Boolean = true) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        values.forEach { (value, label) ->
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 2.dp)
                    .shadow(
                        elevation = 7.dp,
                        shape = RoundedCornerShape(11.dp),
                        ambientColor = Color(0x2D0B1220),
                        spotColor = Color(0x3D0B1220)
                    ),
                shape = RoundedCornerShape(11.dp),
                color = Color.White,
                shadowElevation = 5.dp,
                border = BorderStroke(1.dp, Color(0xFFE4E9F2))
            ) {
                Column(Modifier.padding(horizontal = 9.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(value, color = MColors.Ink900, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(label, color = MColors.Ink500, fontSize = 9.5.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun CommunitySpotlightCard(community: CommunityInfo, joined: Boolean, requested: Boolean, onJoin: () -> Unit) {
    MCard(Modifier.fillMaxWidth(), cornerRadius = 16.dp, elevation = 2.dp) {
        SpotlightTitle(community.name, community.area, community.emoji, "${community.type.label} · ${community.access.label}", MintAccent)
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            SpotlightStats(listOf("${community.members + if (joined) 1 else 0}" to "Üye", "${community.upcomingEvents}" to "Yaklaşan oyun"))
            Text(community.description, fontSize = 11.5.sp, lineHeight = 15.sp, color = MColors.Ink600)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
                items(community.sports) { sport ->
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = Color.White,
                        shadowElevation = 4.dp,
                        border = BorderStroke(1.dp, Color(0xFFE4E9F2)),
                        modifier = Modifier
                            .padding(vertical = 1.dp)
                            .shadow(
                                elevation = 5.dp,
                                shape = RoundedCornerShape(9.dp),
                                ambientColor = Color(0x260B1220),
                                spotColor = Color(0x360B1220)
                            )
                    ) {
                        Text("${sport.emoji} ${sport.label}", Modifier.padding(horizontal = 8.dp, vertical = 4.5.dp), fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = MColors.Ink700)
                    }
                }
            }
            MButton(when {
                joined -> "Katıldın · Ayrıl"
                requested -> "Başvuru bekliyor · İptal"
                community.access == CommunityAccess.OPEN -> "Topluluğa katıl"
                community.access == CommunityAccess.APPROVAL -> "Katılım başvurusu"
                else -> "Davet gerekli"
            }, onJoin, Modifier.fillMaxWidth(), enabled = community.access != CommunityAccess.INVITE, isPrimary = !joined && !requested, height = 38.dp)
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
                color = if (podium) MColors.Ink900 else Color.White,
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, if (podium) GoldAccent.copy(alpha = 0.35f) else Color(0xFFE4E9F2)),
                modifier = Modifier
                    .padding(vertical = 1.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(10.dp),
                        ambientColor = Color(0x2D0B1220),
                        spotColor = Color(0x3D0B1220)
                    )
            ) {
                Column(Modifier.width(34.dp).padding(vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${row.rank}", color = if (podium) GoldAccent else MColors.Ink700, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    if (podium) Text("★", color = GoldAccent, fontSize = 8.sp)
                }
            }
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
fun TournamentSpotlightCard(tournament: TournamentInfo, onOpen: () -> Unit) {
    MCard(Modifier.fillMaxWidth(), cornerRadius = 16.dp, elevation = 2.dp) {
        SpotlightTitle(tournament.name, "${tournament.format} · ${tournament.dates}", tournament.displaySportEmoji, tournament.status, GoldAccent)
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SpotlightStats(listOf("${tournament.participants}/${tournament.capacity}" to "Katılım", if (tournament.isLeague) "Lig" to "Organizasyon" else "Kupa" to "Organizasyon"))
            LinearProgressIndicator(progress = { (tournament.participants.toFloat() / tournament.capacity.coerceAtLeast(1)).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(4.dp), color = MColors.Brand, trackColor = MColors.BrandTint)
            Text(tournament.organizer, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MColors.Ink900)
            Text(tournament.requirement, fontSize = 10.5.sp, color = MColors.Ink500)
            MButton("Fikstür ve kayıt", onOpen, Modifier.fillMaxWidth(), height = 38.dp)
        }
    }
}

@Composable
fun TeamSpotlightCard(team: TeamProfile, primaryLabel: String, onPrimary: () -> Unit, onDetails: () -> Unit, compact: Boolean = true) {
    MCard(Modifier.fillMaxWidth(), cornerRadius = 16.dp, elevation = 2.dp) {
        SpotlightTitle(team.name, "${team.sport.emoji} ${team.sport.label} · ${team.area}", team.badge, if (team.isMine) "TAKIMIN · KAPTANSIN" else if (team.lookingForOpponent) "RAKİP ARIYOR" else "KULÜP PROFİLİ", if (team.isMine) GoldAccent else MintAccent, compact)
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SpotlightStats(listOf("${team.rating}" to "MMR", "${team.wins}" to "Galibiyet", "${team.memberIds.size}" to "Kadro"), compact)
            Text(team.record, fontSize = 10.5.sp, color = MColors.Ink600)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MButton("Kadro", onDetails, Modifier.weight(1f), isPrimary = false, height = 38.dp)
                MButton(primaryLabel, onPrimary, Modifier.weight(1.4f), height = 38.dp)
            }
        }
    }
}

@Composable
fun ChallengeSpotlightCard(challenge: ChallengeOffer, opponent: TeamProfile, onDetails: () -> Unit, onAccept: () -> Unit, onDecline: () -> Unit, onChat: () -> Unit) {
    MCard(Modifier.fillMaxWidth(), cornerRadius = 16.dp, elevation = 2.dp) {
        Column(Modifier.clickable(onClick = onDetails)) {
            SpotlightTitle(opponent.name, if (challenge.incoming) "Gelen meydan okuma · Detayları aç →" else "Gönderilen teklif · Detayları aç →", opponent.badge, challenge.status.label, GoldAccent)
        }
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(challenge.proposedTime, fontSize = 13.sp, color = MColors.Ink900, fontWeight = FontWeight.Bold)
            Text(challenge.venue, fontSize = 11.sp, color = MColors.Ink500)
            if (!challenge.note.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.dp, Color(0xFFE4E9F2)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(10.dp),
                            ambientColor = Color(0x2A0B1220),
                            spotColor = Color(0x3A0B1220)
                        )
                ) {
                    Text(challenge.note, Modifier.fillMaxWidth().padding(9.dp), fontSize = 11.sp, color = MColors.Ink700)
                }
            }
            if (challenge.incoming && challenge.status == ChallengeStatus.PENDING) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MButton("Kabul et", onAccept, Modifier.weight(1f), height = 38.dp)
                    MButton("Reddet", onDecline, Modifier.weight(1f), isPrimary = false, height = 38.dp)
                }
                TextButton(onClick = onChat, modifier = Modifier.fillMaxWidth().height(34.dp), contentPadding = PaddingValues(0.dp)) { Text("Kaptana mesaj gönder", fontSize = 12.sp, color = MColors.Ink700) }
            }
        }
    }
}
