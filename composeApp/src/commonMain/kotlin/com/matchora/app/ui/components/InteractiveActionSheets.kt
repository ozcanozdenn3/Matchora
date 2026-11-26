package com.matchora.app.ui.components

import com.matchora.app.ui.components.premiumSurface
import kotlinx.datetime.toLocalDateTime
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.data.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.coroutines.CancellationException
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
    players: List<Player> = DemoData.players,
    onDismiss: () -> Unit,
    onConfirmSquad: () -> Unit,
    onOpenChat: () -> Unit
) {
    val participants = remember(event, currentPlayer, players) {
        event?.participantIds?.distinct()?.mapNotNull { id -> if (id == DemoData.ME) currentPlayer else players.firstOrNull { it.id == id } }.orEmpty()
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
                        SquadSummaryTile("₺${event.pricePerPersonTry}", "Kişi başı", Modifier.weight(1f))
                    }
                }

                if (participants.size >= 2) {
                    val sorted = participants.sortedByDescending { it.rating }
                    val teamA = mutableListOf<Player>()
                    val teamB = mutableListOf<Player>()
                    sorted.forEachIndexed { idx, p ->
                        if (idx % 4 in listOf(0, 3)) teamA.add(p) else teamB.add(p)
                    }
                    val avgA = if (teamA.isEmpty()) 0 else teamA.sumOf { it.rating } / teamA.size
                    val avgB = if (teamB.isEmpty()) 0 else teamB.sumOf { it.rating } / teamB.size
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, MColors.Line)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("OTOMATİK DENGE ÖNERİSİ", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MColors.BrandDeep)
                                    Text("Fark: ${kotlin.math.abs(avgA - avgB)} MMR", style = MaterialTheme.typography.labelSmall, color = MColors.Mint, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Takım A · Ort. $avgA MMR", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MColors.Ink900)
                                        Text(teamA.joinToString(", ") { it.name }, style = MaterialTheme.typography.bodySmall, color = MColors.Ink600, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Takım B · Ort. $avgB MMR", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MColors.Ink900)
                                        Text(teamB.joinToString(", ") { it.name }, style = MaterialTheme.typography.bodySmall, color = MColors.Ink600, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
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
                                    val avatar = playerAvatarUploadId(player)
                                    if (avatar != null) BackendPhoto(avatar, player.name, Modifier.fillMaxSize())
                                    else Text(player.initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
    onChallenge: (() -> Unit)? = null,
    onInviteTeam: (() -> Unit)? = null,
    onDirectMessage: () -> Unit = {}
) {
    val isCurrentUser = player.id == DemoData.ME
    val backend = LocalBackendRepository.current
    val isPreview = backend == null
    var details by remember(player.id, sport, backend?.userId) { mutableStateOf<JsonObject?>(null) }
    var detailsError by remember(player.id, sport, backend?.userId) { mutableStateOf<String?>(null) }
    var detailsBusy by remember { mutableStateOf(false) }
    var retryDetails by remember { mutableIntStateOf(0) }
    LaunchedEffect(player.id, sport, backend?.userId, retryDetails) {
        if (backend != null) {
            detailsBusy = true; detailsError = null; details = null
            try { details = backend.playerDetails(backend.serverUserId(player.id), sport.name) }
            catch (cancel: CancellationException) { throw cancel }
            catch (error: Exception) { detailsError = (error as? BackendFailure)?.message ?: "Sporcu ayrıntıları alınamadı." }
            finally { detailsBusy = false }
        }
    }
    val serverPlayer = details?.o("player")
    val selectedSkill = serverPlayer?.a("skills")?.firstOrNull { it.s("sport") == sport.name }
    val isVerifiedAthlete = if (isPreview) player.verified else ("PHOTO" in serverPlayer?.ss("verifications").orEmpty()) || player.verified
    val displayArea = if (isPreview) player.area else serverPlayer?.s("area")?.ifBlank { null } ?: player.area
    val displayMatches = if (isPreview) player.verifiedMatches else serverPlayer?.i("verifiedMatches", player.verifiedMatches) ?: player.verifiedMatches
    val displayRating = if (isPreview) rating else selectedSkill?.i("rating", rating) ?: rating
    val chemistryScore = if (isPreview) player.chemistry else (details?.get("chemistry") as? JsonPrimitive)?.intOrNull
    val sharedMatches = if (isPreview) player.playedTogether else (details?.get("playedTogether") as? JsonPrimitive)?.intOrNull
    val activeRole = if (isPreview) player.position ?: player.level.label else selectedSkill?.s("position")?.ifBlank { null }
        ?: SkillLevel.entries.firstOrNull { it.name == selectedSkill?.s("level") }?.label ?: "Sporcu"
    val reliabilityLabel = if (isPreview) player.reliabilityLabel else details?.o("player")?.takeIf { it.b("reliabilityVisible", false) }?.let { value -> (value["reliability"] as? JsonPrimitive)?.intOrNull?.let { "%$it" } } ?: "—"
    val recentForm = if (isPreview) listOf("W" to "8.7", "W" to "8.2", "W" to "9.1", "L" to "7.3", "W" to "8.9")
        else details?.a("recentForm").orEmpty().map { form ->
            val delta = (form["ratingDelta"] as? JsonPrimitive)?.intOrNull
            form.s("outcome").ifBlank { "•" } to (delta?.let { if (it > 0) "+$it" else "$it" } ?: "—")
        }
    val earnedBadges = if (isPreview) listOf("🏅 Sezon 3 MVP" to MColors.Gold, "🔥 12 Maç Namağlup" to MColors.Brand, "🤝 FairPlay Lideri" to MColors.Mint)
        else details?.a("badges").orEmpty().take(3).map { "${it.s("emoji")} ${it.s("title")}" to MColors.Mint }

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
                                    val avatar = details?.o("player")?.s("avatarUploadId")?.ifBlank { null } ?: playerAvatarUploadId(player)
                                    if (avatar != null) BackendPhoto(avatar, name, Modifier.fillMaxSize())
                                    else Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(name, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (isVerifiedAthlete) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(Icons.Rounded.Verified, null, tint = MColors.Mint, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val locationSegment = displayArea.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
                                    val rankSegment = if (rank > 0) " · $rank. sırada" else ""
                                    Text(
                                        text = "${sport.emoji} ${sport.label}$locationSegment$rankSegment",
                                        fontSize = 12.sp,
                                        color = Color(0xFFD0D9E4),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "ATLET PASAPORTU: ${player.id.uppercase()} · ${activeRole}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                        color = Color(0xFFFFC078)
                                    )
                                }
                            }

                            // MMR Rozeti & Kapat
                            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MColors.Brand)
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = "$displayRating MMR",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Seviye: ${Tier.of(displayRating).label}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFCFD6E0)
                                    )
                                }
                                IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Kapat", tint = Color(0xFFD0D9E4), modifier = Modifier.size(18.dp))
                                }
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
                            Text("$displayMatches", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = MColors.Brand)
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
                            Text(reliabilityLabel, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MColors.Mint)
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
                                Text(chemistryScore?.let { "%$it" } ?: "—", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                                Spacer(modifier = Modifier.width(2.dp))
                                if (chemistryScore != null) Icon(Icons.Rounded.AutoAwesome, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
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
                            Text(sharedMatches?.toString() ?: "—", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MColors.Mint)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("BİRLİKTE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MColors.Ink600, maxLines = 1)
                        }
                    }
                }
            }

            item { RatingPolicyButton() }

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
                        val serverPlayer = details?.o("player")
                        val bioText = serverPlayer?.s("bio")?.ifBlank { null } ?: player.bio.ifBlank { null }
                        val sideText = serverPlayer?.s("dominantSide")?.ifBlank { null } ?: player.dominantSide.ifBlank { null }
                        val availText = serverPlayer?.ss("availability")?.joinToString(", ")?.ifBlank { null } ?: serverPlayer?.s("availability")?.ifBlank { null } ?: player.availability.joinToString(", ").ifBlank { null }
                        val campusText = serverPlayer?.s("campus")?.ifBlank { null } ?: player.campus?.ifBlank { null }
                        val companyText = serverPlayer?.s("company")?.ifBlank { null } ?: player.company?.ifBlank { null }
                        val jerseyNum = serverPlayer?.i("jerseyNumber", -1)?.takeIf { it >= 0 } ?: player.jerseyNumber
                        val profileMeta = listOfNotNull(
                            if (isPreview) player.sports.firstOrNull()?.label ?: sport.label else sport.label,
                            activeRole,
                            sideText?.let { "Baskın yön: $it" },
                            jerseyNum?.let { "#$it" },
                            "Reliability $reliabilityLabel",
                            availText?.let { "Müsaitlik: $it" } ?: if (isPreview) (if (player.availableNow) "Şu an müsait" else "Planlı katılıma açık") else null,
                            campusText?.let { "🎓 $it" },
                            companyText?.let { "🏢 $it" }
                        ).joinToString(" · ")
                        Text(
                            text = if (bioText != null) "$profileMeta\n$bioText" else profileMeta,
                            fontSize = 14.sp,
                            color = Color(0xFF244C33),
                            lineHeight = 20.sp,
                            maxLines = 4,
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
                            Text(if (isPreview) "Son karşılaşmalardaki puanların" else when {
                                detailsBusy -> "Yükleniyor…"
                                detailsError != null -> "Ayrıntılar alınamadı"
                                details?.b("historyVisible") == false -> "Geçmiş paylaşımı kapalı"
                                recentForm.isEmpty() -> "Paylaşılan doğrulanmış maç yok"
                                else -> "Doğrulanmış sonuç · MMR değişimi"
                            }, fontSize = 11.sp, color = MColors.Ink500)
                            if (detailsError != null) TextButton(onClick = { retryDetails++ }) { Text("Yeniden dene") }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            recentForm.forEach { (res, ratingVal) ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(if (res == "W") MColors.Mint else if (res == "L") MColors.Rose else MColors.Ink500),
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
                    if (!isPreview && earnedBadges.isEmpty()) Text(if (details?.b("historyVisible") == false) "Başarı paylaşımı kapalı." else "Paylaşılan kazanılmış rozet yok.", fontSize = 11.sp, color = MColors.Ink500)
                    earnedBadges.forEach { (badgeTitle, color) ->
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

            item { BackendPlayerSafetyActions(player) }

            // 6. Aksiyon Butonları
            item {
                if (isCurrentUser) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MColors.SurfaceMuted,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Kendi sporcu profilin",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MColors.Ink600,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MButton(
                            text = "Direkt Mesaj Gönder 💬",
                            onClick = {
                                onDirectMessage()
                                onDismiss()
                            },
                            icon = Icons.Rounded.ChatBubbleOutline
                        )

                        if (onChallenge != null || onInviteTeam != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (onChallenge != null) {
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
                                }

                                if (onInviteTeam != null) {
                                    OutlinedButton(
                                        onClick = {
                                            onInviteTeam()
                                            onDismiss()
                                        },
                                        enabled = !isInvitedToTeam,
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, MColors.Brand),
                                        modifier = Modifier.weight(1f).height(44.dp)
                                    ) {
                                        Text(
                                            if (isInvitedToTeam) "Davet gönderildi ✓" else "Takıma Davet Et 🛡️",
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
    val pricePerPlayer = formatTryMinor(splitTryMinor(slot.priceMinor, splitPlayers))

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
            if (venue.address.isNotBlank()) Text(venue.address, style = MaterialTheme.typography.bodySmall, color = MColors.Ink600)
            MCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(slot.day, style = MaterialTheme.typography.labelMedium, color = MColors.Ink500)
                        Text(slot.time, style = MaterialTheme.typography.headlineMedium, color = MColors.Ink900)
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Toplam", style = MaterialTheme.typography.labelMedium, color = MColors.Ink500)
                        Text("₺${slot.priceLabel}", style = MaterialTheme.typography.headlineSmall, color = MColors.BrandDeep)
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
                            val perPlayerPrice = formatTryMinor(splitTryMinor(slot.priceMinor, count))
                            if (count == 1) "Tek Kişi · ₺$perPlayerPrice" else "$count Kişi · Kişi Başı ₺$perPlayerPrice"
                        },
                        onSelect = { splitPlayers = it },
                        icon = Icons.Rounded.Groups
                    )
                }
            }

            Text("Tesiste ödeme · toplam ₺${slot.priceLabel}. Kişi başı hesap, paylaşım önerisidir; rezervasyon toplam ücret üzerinden yapılır.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)

            // Onay Butonu
            MButton(
                text = "Toplam ₺${slot.priceLabel} · Yerimi ayır",
                onClick = {
                    onConfirmBooking(splitPlayers, slot.priceTry, selectedMethod)
                    onDismiss()
                },
                icon = Icons.Rounded.EventAvailable
            )
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
    onCancelTournament: () -> Unit = {},
    onOpenFixture: (String) -> Unit = {},
    onShareTournament: (() -> Unit)? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) } // Fikstür, puanlar, kurallar ve kayıtlar
    var showCancelConfirmation by remember { mutableStateOf(false) }
    var showWithdrawConfirmation by remember { mutableStateOf(false) }
    val statusAllowsRegistration = tournament.status.contains("açık", ignoreCase = true) ||
        tournament.status.contains("son ", ignoreCase = true)
    val canRegister = statusAllowsRegistration && tournament.participants < tournament.capacity
    val canCancelTournament = isOrganizer && tournament.organizerLockTokens > 0 &&
        !tournament.status.contains("iptal", ignoreCase = true) &&
        !tournament.status.contains("tamamland", ignoreCase = true)
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
    val previewMode = LocalBackendRepository.current?.enabled != true
    val previewParticipantNames = remember(tournament, previewMode) {
        if (previewMode && tournament.registrations.isEmpty() && tournament.participants > 0)
            (tournament.bracket.flatMap { listOf(it.teamA, it.teamB) } + tournament.standings.map { it.team })
                .filter { it.isNotBlank() && it != "TBD" }.distinct().take(tournament.participants)
        else emptyList()
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
            // 0. Başlık
            item {
                MSheetHeading(
                    title = tournament.name,
                    subtitle = "${tournament.displaySportEmoji} ${tournament.displaySportLabel} · ${tournament.format} · ${tournament.dates}",
                    icon = Icons.Rounded.EmojiEvents,
                    onDismiss = onDismiss
                )
            }

            // 1. Birleşik Turnuva Özeti, ID, Paylaşım ve Ücret Bilgisi
            item {
                var copiedTourneyId by remember { mutableStateOf(false) }
                MCard(Modifier.fillMaxWidth(), cornerRadius = 16.dp, elevation = 2.dp) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📍 ${tournament.organizer}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MColors.Ink700,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                MBadge("${tournament.participants}/${tournament.capacity} takım", MColors.Ink700, Color(0xFFF1F5F9))
                                MBadge(tournament.status, statusColor, statusTint)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            modifier = Modifier.size(32.dp).background(MColors.BrandTint, RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Rounded.Tag, null, tint = MColors.Brand, modifier = Modifier.size(16.dp))
                                        }
                                        Column {
                                            Text("TURNUVA ID", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MColors.Ink400, letterSpacing = 0.5.sp)
                                            Text(tournamentDisplayId(tournament.id), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = MColors.BrandDeep)
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
                                        val toast = LocalToastNotifier.current
                                        val backendEnabled = LocalBackendRepository.current?.enabled == true
                                        FilledTonalButton(
                                            onClick = {
                                                clipboard.setText(androidx.compose.ui.text.AnnotatedString(tournamentDisplayId(tournament.id)))
                                                copiedTourneyId = true
                                                toast?.invoke("Turnuva kodu kopyalandı", "✓")
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 5.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(13.dp))
                                            Spacer(Modifier.width(3.dp))
                                            Text(if (copiedTourneyId) "Kopyalandı" else "Kopyala", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = {
                                                if (onShareTournament != null) {
                                                    onShareTournament.invoke()
                                                } else {
                                                    shareText("Turnuva Daveti", tournamentInvitationText(tournament.name, tournament.id, tournament.dates, tournament.format, tournament.organizer))
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MColors.Brand),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 5.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(Icons.Rounded.Share, null, modifier = Modifier.size(13.dp))
                                            Spacer(Modifier.width(3.dp))
                                            Text(if (backendEnabled) "Paylaş" else "Paylaş (+2 TKN)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Text(
                                    text = "Kaptanlar bu ID ile akıştan turnuvayı bulup takımlarını kaydettirebilir.",
                                    fontSize = 10.5.sp,
                                    color = MColors.Ink500
                                )
                            }
                        }

                        if (isOrganizer) {
                            Surface(
                                shape = RoundedCornerShape(11.dp),
                                color = Color(0xFFFFFBEB),
                                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("👑", fontSize = 18.sp)
                                    Column {
                                        Text("Turnuva Yönetim Paneli", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF92400E))
                                        Text("Organizatörsün · ${tournament.registrations.size} kayıtlı takım · ${tournament.organizerLockTokens} TKN güvence kilitli.", fontSize = 11.sp, color = Color(0xFFB45309))
                                    }
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(11.dp),
                            color = Color(0xFFFFF7ED),
                            border = BorderStroke(1.dp, MColors.Brand.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("ÜCRET & ÖDÜL", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp, color = MColors.Brand)
                                Text(tournament.feeNote, fontSize = 12.sp, lineHeight = 16.sp, color = MColors.Ink700)
                            }
                        }
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
                            "Kayıtlar" to "🛡️"
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
                            val kickoff = match.startsAtEpochMillis?.let { millis ->
                                val time = kotlinx.datetime.Instant.fromEpochMilliseconds(millis).toLocalDateTime(kotlinx.datetime.TimeZone.of("Europe/Istanbul"))
                                "${eventDateLabel(time.date)} · ${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0x12000000)),
                                modifier = Modifier.fillMaxWidth().clickable(enabled = match.eventId != null) { match.eventId?.let(onOpenFixture) }
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
                                            text = if (match.eventId != null) "$kickoff · ${match.venue} · Maçı aç →" else "${tournament.organizer} · ${tournament.dates}",
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
                    if (previewParticipantNames.isNotEmpty()) {
                        item { Text("Örnek katılımcılar", style = MaterialTheme.typography.labelLarge, color = MColors.Ink600) }
                        items(previewParticipantNames, key = { "preview-$it" }) { name ->
                            MCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(name, fontWeight = FontWeight.Bold)
                                Text("Önizleme kaydı", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
                            } }
                        }
                    } else if (tournament.registrations.isEmpty()) item { Text("Henüz takım kaydı yok. Kaptanlar takımını ve maç kadrosunu seçerek kayıt olur.", fontSize = 12.sp, color = MColors.Ink600) }
                    items(tournament.registrations, key = { it.teamId }) { registration ->
                        MCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(registration.name, fontWeight = FontWeight.Bold)
                            Text("${registration.rosterIds.size} sporcu · ${when (registration.status) { "removed" -> "Kaydı kaldırıldı"; "left" -> "Kaydı geri çekildi"; else -> if (registration.captainId == DemoData.ME) "Kaptanı sensin" else "Kayıtlı takım" }}", style = MaterialTheme.typography.bodySmall)
                            BackendTournamentRegistrationActions(tournament, registration)
                        } }
                    }
                }
            }

            // 5. Kayıt durumu & organizatör kontrolü
            if (tournament.championTeamId != null) item {
                Text("🏆 Şampiyon: ${tournament.registrations.firstOrNull { it.teamId == tournament.championTeamId }?.name ?: "Turnuva tamamlandı"}", color = MColors.Brand, fontWeight = FontWeight.Bold)
            }
            if (canCancelTournament || (tournament.creatorId == DemoData.ME && tournament.status !in listOf("Tamamlandı", "İptal edildi"))) {
                item {
                    MCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Organizatör yönetimi", style = MaterialTheme.typography.titleSmall, color = MColors.Ink900)
                            BackendTournamentActions(tournament)
                            if (canCancelTournament) {
                                MButton(
                                    text = "Turnuvayı iptal et · ${tournament.organizerLockTokens} TKN iade",
                                    onClick = { showCancelConfirmation = true },
                                    isPrimary = false,
                                    isDestructive = true,
                                    icon = Icons.Rounded.Cancel,
                                    height = 44.dp
                                )
                            }
                        }
                    }
                }
            }
            item {
                MButton(
                    text = when {
                        tournament.status.contains("iptal", ignoreCase = true) -> "Turnuva iptal edildi"
                        isRegistered && canRegister -> "Başka takım / kadro kaydet"
                        isRegistered -> "Kaydın alındı ✓"
                        tournament.participants >= tournament.capacity -> "Kontenjan doldu"
                        !statusAllowsRegistration -> "Kayıt süreci kapalı"
                        else -> "Turnuvaya kaydol 🏆"
                    },
                    onClick = {
                        onRegisterTeam()
                        onDismiss()
                    },
                    enabled = canRegister
                )
            }

            if (isRegistered && statusAllowsRegistration) {
                item {
                    MButton(
                        text = "Kaydı geri çek",
                        onClick = {
                            onWithdrawRegistration()
                            onDismiss()
                        },
                        isPrimary = false,
                        isDestructive = true,
                        height = 44.dp
                    )
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
                text = { Text("Turnuva yayından kaldırılacak ve ${tournament.organizerLockTokens} TKN kilidi bakiyene dönecek.") },
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
                RatingPolicyButton()
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
