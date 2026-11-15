package com.matchora.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.domain.EventStatus
import com.matchora.app.state.*
import com.matchora.app.ui.theme.MColors

// ═══════════════════════════════════════════════════════════════════════════════
// EVENT DETAIL SHEET (Strateji Dokümanı §08, §12)
// Durum makinesi, katılımcı kadrosu, Match Score açıklaması, QR/Geofence check-in,
// taahhüt kilidi, katılma başvurusu ve organizatör onay akışı.
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailSheet(
    event: SportEvent,
    currentPlayer: Player = DemoData.mePlayer,
    players: List<Player> = DemoData.players,
    isJoined: Boolean,
    isCheckedIn: Boolean,
    isOnWaitlist: Boolean = false,
    waitlistOfferUntil: Long? = null,
    isHost: Boolean = false,
    pendingRequest: JoinRequest? = null,
    pendingRequestsCount: Int = 0,
    onDismiss: () -> Unit,
    onToggleJoin: () -> Unit,
    onToggleWaitlist: () -> Unit = {},
    onAcceptWaitlistOffer: () -> Unit = {},
    onRequestJoin: () -> Unit = {},
    onCancelRequest: () -> Unit = {},
    onOpenManageRequests: () -> Unit = {},
    onDirectMessageHost: () -> Unit = {},
    onDirectMessagePlayer: (Player) -> Unit = {},
    onCheckIn: () -> Unit,
    onOpenChat: () -> Unit,
    onReportEvent: () -> Unit,
    onStartEvent: () -> Unit = {},
    onCompleteEvent: () -> Unit = {},
    onCancelEvent: () -> Unit = {},
    hasResult: Boolean = false,
    onOpenResult: () -> Unit = {},
    isRemovedByHost: Boolean = false,
    removedPlayers: List<Player> = emptyList(),
    onRemoveParticipant: (Player) -> Unit = {},
    onRestoreParticipant: (Player) -> Unit = {},
    isDirectInvited: Boolean = false,
    onChallengePlayer: ((Player) -> Unit)? = null,
    onInvitePlayerToTeam: ((Player) -> Unit)? = null,
    onShare: () -> Unit
) {
    var showMatchScoreDetail by remember { mutableStateOf(false) }
    var hostAction by remember { mutableStateOf<String?>(null) }
    var confirmMemberAction by remember { mutableStateOf<String?>(null) }
    var directionsError by remember { mutableStateOf(false) }
    var removalTarget by remember { mutableStateOf<Player?>(null) }
    var showRemoved by remember { mutableStateOf(false) }
    var selectedPlayerProfile by remember { mutableStateOf<Player?>(null) }
    val backendMode = LocalBackendRepository.current?.enabled == true
    val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    val preStart = if (backendMode) event.startsAtEpochMillis?.let { it > now } ?: false else (event.startsInMin ?: 1) > 0
    val active = event.status in setOf(EventStatus.PUBLISHED, EventStatus.FILLING, EventStatus.FULL)
    val canManageRoster = active && preStart && !event.isTeamEvent
    val checkInOpen = !backendMode || event.startsAtEpochMillis?.let { now >= it - 30 * 60000 && now <= it + event.durationMin * 60000 } == true
    val mayCheckIn = checkInOpen && (active || event.status == EventStatus.IN_PROGRESS)
    removalTarget?.let { player -> AlertDialog(onDismissRequest = { removalTarget = null }, title = { Text("Kadrodan çıkarılsın mı?") },
        text = { Text("${player.name} kadrodan çıkarılacak. Varsa taahhüdü iade edilir; bu işlem güvenilirlik cezası vermez. Yeniden katılabilmesi için izin vermen gerekir.") },
        confirmButton = { TextButton(onClick = { removalTarget = null; onRemoveParticipant(player) }) { Text("Kadrodan çıkar", color = MColors.Rose) } },
        dismissButton = { TextButton(onClick = { removalTarget = null }) { Text("Vazgeç") } }) }
    if (showRemoved) AlertDialog(onDismissRequest = { showRemoved = false }, title = { Text("Çıkarılan katılımcılar") }, text = {
        Column { if (removedPlayers.isEmpty()) Text("Çıkarılmış katılımcı yok.")
            removedPlayers.forEach { player -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(player.name, Modifier.weight(1f))
                TextButton(onClick = { onRestoreParticipant(player) }) { Text("Yeniden izin ver") }
            } }
            Text("İzin vermek sporcuyu otomatik katmaz ve Token harcatmaz.", style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = { TextButton(onClick = { showRemoved = false }) { Text("Kapat") } })
    val displayedParticipantIds = if (isJoined) {
        (event.participantIds + DemoData.ME).distinct()
    } else {
        event.participantIds.distinct()
    }
    val displayedFreeSpots = (event.capacity - displayedParticipantIds.size).coerceAtLeast(0)

    hostAction?.let { action ->
        AlertDialog(onDismissRequest = { hostAction = null },
            title = { Text(when (action) { "start" -> "Maç başlatılsın mı?"; "complete" -> "Maç tamamlansın mı?"; else -> "Maç iptal edilsin mi?" }) },
            text = { Text(when (action) { "start" -> "Katılımcı kadrosunu kontrol ederek maçı başlat."; "complete" -> "Tamamladıktan sonra sonucu ve fair play değerlendirmesini bildirebilirsin."; else -> "Karşılaşma iptal edilir ve kendi taahhüt kilidin varsa iade edilir." }) },
            confirmButton = { TextButton(onClick = { hostAction = null; when (action) { "start" -> onStartEvent(); "complete" -> onCompleteEvent(); else -> onCancelEvent() } }) { Text(if (action == "cancel") "Maçı İptal Et" else "Onayla", color = if (action == "cancel") MColors.Rose else MColors.BrandDeep) } },
            dismissButton = { TextButton(onClick = { hostAction = null }) { Text("Vazgeç") } })
    }
    confirmMemberAction?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmMemberAction = null },
            title = {
                Text(
                    when (action) {
                        "cancel_request" -> "Başvuru iptal edilsin mi?"
                        "leave_event" -> "Maç kadrosundan ayrıl?"
                        else -> "Bekleme listesinden ayrıl?"
                    }
                )
            },
            text = {
                Text(
                    when (action) {
                        "cancel_request" -> "Katılım başvurun iptal edilir ve kilitli taahhüdün varsa iade edilir."
                        "leave_event" -> if (!backendMode || (event.startsInMin ?: 0) >= 180) {
                            "Kadrodan ayrılacaksın ve taahhüdün iade edilecek."
                        } else {
                            "Başlangıca 3 saatten az kaldığı için geç iptal sayılır ve taahhüt iade edilmez."
                        }
                        else -> "Bekleme sıranı bırakacaksın; yer açılırsa tekrar sıraya girmen gerekir."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmMemberAction = null
                        when (action) {
                            "cancel_request" -> onCancelRequest()
                            "leave_event" -> onToggleJoin()
                            else -> onToggleWaitlist()
                        }
                    }
                ) {
                    Text(
                        when (action) {
                            "cancel_request" -> "Başvuruyu İptal Et"
                            "leave_event" -> "Kadrodan Ayrıl"
                            else -> "Listeden Ayrıl"
                        },
                        color = MColors.Rose
                    )
                }
            },
            dismissButton = { TextButton(onClick = { confirmMemberAction = null }) { Text("Vazgeç") } }
        )
    }
    if (directionsError) {
        AlertDialog(
            onDismissRequest = { directionsError = false },
            title = { Text("Yol tarifi açılamadı") },
            text = { Text("Harita uygulaması açılamadı. Tesis adresini elle arayabilirsin.") },
            confirmButton = { TextButton(onClick = { directionsError = false }) { Text("Tamam") } }
        )
    }

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Başlık & Spor
            item {
                MSheetHeading(
                    title = event.title,
                    subtitle = "${event.displaySportLabel} · ${event.mode.label} · ${event.status.label}",
                    icon = Icons.Rounded.Sports,
                    onDismiss = onDismiss
                )
            }

            if ((isJoined || isHost) && (hasResult || event.status in setOf(EventStatus.IN_PROGRESS, EventStatus.COMPLETED, EventStatus.VERIFIED, EventStatus.DISPUTED))) {
                item { MButton(if (hasResult) "Maç sonucu ve değerlendirme" else "Sonuç bildir ve değerlendir", onOpenResult, icon = Icons.Rounded.Scoreboard, isPrimary = false) }
            }

            if (isHost && event.status in setOf(EventStatus.PUBLISHED, EventStatus.FILLING, EventStatus.FULL, EventStatus.IN_PROGRESS)) {
                item {
                    MCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Organizatör işlemleri", style = MaterialTheme.typography.titleSmall)
                            if (canManageRoster) {
                                MButton(
                                    text = "İlan Yönetim Paneli ($pendingRequestsCount Başvuru)",
                                    onClick = onOpenManageRequests,
                                    icon = Icons.Rounded.ManageAccounts,
                                    isPrimary = true
                                )
                            }
                            if (event.status == EventStatus.IN_PROGRESS || !backendMode || event.startsAtEpochMillis?.let { now < it + event.durationMin * 60000 } == true) {
                                MButton(
                                    text = if (event.status == EventStatus.IN_PROGRESS) "Maçı tamamla" else "Maçı başlat",
                                    onClick = { hostAction = if (event.status == EventStatus.IN_PROGRESS) "complete" else "start" },
                                    isPrimary = false
                                )
                            }
                            if (mayCheckIn) MButton("Saha check-in kodu", onCheckIn, isPrimary = false, icon = Icons.Rounded.QrCode)
                            BackendEventEditing(event)
                            MButton(
                                text = "Maçı iptal et",
                                onClick = { hostAction = "cancel" },
                                isPrimary = false,
                                isDestructive = true,
                                icon = Icons.Rounded.Cancel
                            )
                        }
                    }
                }
            }

            // Kaptan / Organizatör Bilgilendirme Rozeti
            if (isHost) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFF7ED),
                        border = BorderStroke(1.dp, MColors.Brand.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("👑", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Bu İlanın Kaptanısın",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MColors.Brand
                                )
                                Text(
                                    text = "Başvuruları inceleyebilir, onaylayıp reddedebilir veya mesaj atabilirsin.",
                                    fontSize = 11.sp,
                                    color = MColors.Ink600
                                )
                            }
                        }
                    }
                }
            }

            (event.meetingLocation ?: event.approximateLocation)?.let { point ->
                item {
                    NativeMapView(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(20.dp)),
                        point.latitude, point.longitude, 1000.0,
                        listOf(MapPinData(event.id, event.venue, event.area, point.latitude, point.longitude, event.displaySportEmoji)),
                        null, {}, interactive = true)
                }
            }

            // Özet Bilgi Kartı (Saha, Saat, Mesafe, Ücret)
            item {
                MCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.LocationOn, null, tint = MColors.Brand, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    event.venue,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MColors.Ink900,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${event.area}${event.distanceKm?.let { " · $it km" }.orEmpty()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MColors.Ink500,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (!isOnlineMeeting(event)) {
                                OutlinedButton(
                                    onClick = { if (!openEventDirections(event)) directionsError = true },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Rounded.Directions, null, Modifier.size(14.dp), tint = MColors.BrandDeep)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Yol Tarifi", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MColors.BrandDeep)
                                }
                            }
                        }
                        val sessionDetails = listOfNotNull(
                            event.format?.takeIf { it.isNotBlank() },
                            if (event.listingType == EventListingType.SEEK_PLAYERS) "Oyuncu aranıyor" else null,
                            event.neededRoles.takeIf { it.isNotEmpty() }?.joinToString(" / ")?.let { "Aranan: $it" },
                            if (event.emergency) "Acil çağrı" else null,
                            event.networkName?.takeIf { it.isNotBlank() }?.let { "Ağ: $it" },
                            event.gamePlatform?.takeIf { it.isNotBlank() },
                            event.gameServer?.takeIf { it.isNotBlank() },
                            event.voiceChannel?.takeIf { it.isNotBlank() }?.let { "Ses: $it" },
                            event.hostGameTag?.takeIf { it.isNotBlank() }?.let { "Oyuncu ID: $it" },
                            event.networkScope.label
                        ).joinToString(" · ")
                        if (sessionDetails.isNotBlank()) {
                            Text(sessionDetails, fontSize = 10.sp, color = MColors.Ink600, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        if (event.description.isNotBlank()) {
                            Text(event.description, fontSize = 11.sp, lineHeight = 16.sp, color = MColors.Ink600, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }

                        HorizontalDivider(color = MColors.LineSoft)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("BAŞLANGIÇ", style = MaterialTheme.typography.labelSmall, color = MColors.Ink400)
                                Text("${event.day} ${event.time}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MColors.Ink900, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Column {
                                Text("SÜRE", style = MaterialTheme.typography.labelSmall, color = MColors.Ink400)
                                Text("${event.durationMin} dk", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MColors.Ink900, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Column {
                                Text("KİŞİ BAŞI", style = MaterialTheme.typography.labelSmall, color = MColors.Ink400)
                                Text(if (event.pricePerPersonTry > 0) "₺${event.pricePerPersonTry}" else "Ücretsiz", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MColors.Brand, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            // Match Score açıklaması yalnızca öneri motoru girdisi mevcutsa gösterilir.
            item {
                if (event.score.reasons.isEmpty()) {
                    MCard(modifier = Modifier.fillMaxWidth(), backgroundColor = MColors.SurfaceMuted) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, null, tint = MColors.Ink500, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Yeni ilan", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MColors.Ink800)
                                Text("Eşleşme analizi öneriler oluştuğunda görünür.", fontSize = 10.sp, color = MColors.Ink500)
                            }
                        }
                    }
                } else MCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MColors.BrandTint,
                    borderColor = MColors.Brand.copy(alpha = 0.3f),
                    onClick = { showMatchScoreDetail = !showMatchScoreDetail }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.AutoAwesome, null, tint = MColors.Brand, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Match Score™: %${event.score.total} Uyum",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MColors.Brand,
                                    softWrap = false,
                                    maxLines = 1
                                )
                            }
                            Icon(
                                imageVector = if (showMatchScoreDetail) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MColors.Brand
                            )
                        }

                        if (showMatchScoreDetail) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    Pair("Zaman", "%${event.score.time}"),
                                    Pair("Mesafe", "%${event.score.distance}"),
                                    Pair("Seviye", "%${event.score.level}"),
                                    Pair("Güven", "%${event.score.reliability}")
                                ).forEach { (k, v) ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            Text(v, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MColors.Brand)
                                            Text(k, fontSize = 8.sp, color = MColors.Ink500)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Emergency Fill Uyarısı (Varsa)
            if (event.emergency) {
                item {
                    MCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0xFFFFF4EE),
                        borderColor = MColors.Brand,
                        borderWidth = 1.5.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.WarningAmber, null, tint = MColors.Brand, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Son dakika oyuncu çağrısı",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MColors.Brand
                                )
                                Text(
                                    text = "${event.day} · ${event.time} · Aranan: ${event.neededRoles.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MColors.Ink700
                                )
                            }
                        }
                    }
                }
            }

            // Katılımcı Kadrosu Başlığı
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Oyuncu Kadrosu (${displayedParticipantIds.size} / ${event.capacity})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MColors.Ink900
                    )
                    Text(
                        text = "$displayedFreeSpots Boş Yer",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MColors.Mint
                    )
                }
            }

            // Oyuncu Listesi
            items(displayedParticipantIds.filter { !isJoined || it != DemoData.ME }) { pid ->
                val p = if (pid == DemoData.ME) currentPlayer else players.firstOrNull { it.id == pid }
                if (p != null) {
                    MCard(modifier = Modifier.fillMaxWidth(), onClick = { if (p.id != DemoData.ME) selectedPlayerProfile = p }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                UserAvatar(
                                    userId = p.id,
                                    name = p.name,
                                    modifier = Modifier.size(36.dp),
                                    fallbackColor = p.color
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(p.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MColors.Ink900)
                                        if (p.verified) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(Icons.Rounded.Verified, null, tint = MColors.Mint, modifier = Modifier.size(13.dp))
                                        }
                                    }
                                    Text("${p.level.label} · ${p.position ?: "Oyuncu"}", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = MColors.Ink500)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (pid in event.checkedInParticipantIds) {
                                    MBadge(text = "✓ Check-in", textColor = MColors.Mint, backgroundColor = MColors.MintTint)
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                MBadge(text = "R:${p.reliabilityLabel}", textColor = MColors.Mint, backgroundColor = MColors.MintTint)
                                if (isHost && canManageRoster && pid != event.hostId) IconButton(onClick = { removalTarget = p }) {
                                    Icon(Icons.Rounded.PersonRemove, contentDescription = "${p.name} kadrodan çıkar", tint = MColors.Rose, modifier = Modifier.size(20.dp))
                                }
                                if (p.chemistry != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    MBadge(text = "%${p.chemistry}", textColor = MColors.Lilac, backgroundColor = MColors.LilacTint)
                                }
                            }
                        }
                    }
                }
            }

            if (isHost && canManageRoster && removedPlayers.isNotEmpty()) item {
                TextButton(onClick = { showRemoved = true }) { Text("Çıkarılan katılımcılar (${removedPlayers.size})") }
            }

            // Sen katıldıysan senin kartın
            if (isJoined) {
                item {
                    MCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = MColors.MintTint,
                        borderColor = MColors.Mint
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                UserAvatar(
                                    userId = currentPlayer.id,
                                    name = currentPlayer.name,
                                    modifier = Modifier.size(36.dp),
                                    fallbackColor = MColors.Brand
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Sen · ${currentPlayer.name}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MColors.Ink900, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${event.commitmentTokens} TKN taahhüt kilitlendi", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = MColors.Ink600)
                                }
                            }
                            MBadge(
                                text = if (isCheckedIn || DemoData.ME in event.checkedInParticipantIds) "✓ Check-in" else "Kadroda",
                                textColor = MColors.Mint,
                                backgroundColor = Color.White
                            )
                        }
                    }
                }
            }

            // Bekleyen Başvuru Varsa
            if (pendingRequest != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFFF7ED),
                        border = BorderStroke(1.dp, MColors.Brand.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⏳", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Başvurun bekliyor", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MColors.Brand)
                                    Text("Rol: ${pendingRequest.requestedPosition ?: "Oyuncu"} · ${event.commitmentTokens} TKN kilitte", fontSize = 10.sp, color = MColors.Ink600)
                                }
                            }
                            TextButton(onClick = { confirmMemberAction = "cancel_request" }) {
                                Text("İptal Et", fontSize = 11.sp, color = MColors.Rose, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Aksiyon Butonları
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isHost) {
                        // KAPTAN GÖRÜNÜMÜ: Sohbet erişimi (diğer kontroller üst panelde)
                        MButton(
                            text = "💬 Etkinlik Sohbetine Git",
                            onClick = onOpenChat,
                            isPrimary = false
                        )
                    } else if (isJoined) {
                        // KATILMIŞ KULLANICI GÖRÜNÜMÜ
                        if (!isCheckedIn && mayCheckIn) {
                            MButton(
                                text = "📍 Check-in yap",
                                onClick = onCheckIn,
                                isPrimary = true
                            )
                        }

                        MButton(
                            text = "💬 Etkinlik Sohbetine Git",
                            onClick = onOpenChat,
                            isPrimary = false
                        )

                        if (canManageRoster && !isCheckedIn) MButton(
                            text = if (!backendMode || (event.startsInMin ?: 0) >= 180) {
                                "Maç kadrosundan ayrıl · taahhüt iade"
                            } else {
                                "Maç kadrosundan ayrıl · geç iptal"
                            },
                            onClick = { confirmMemberAction = "leave_event" },
                            isPrimary = false,
                            isDestructive = true
                        )
                    } else if (isRemovedByHost) {
                        Text("Organizatör katılımını sonlandırdı. Yeniden katılım için organizatörün izin vermesi gerekiyor.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink600)
                    } else if (!active || !preStart || event.isTeamEvent) {
                        Text(if (event.isTeamEvent) "Bu resmi maçın kadrosu takım kaydından belirlenir." else "Bu karşılaşma artık katılım almıyor.", style = MaterialTheme.typography.bodySmall)
                    } else if (pendingRequest != null) {
                        // BAŞVURUSU BEKLEYEN KULLANICI
                        MButton(
                            text = "💬 Kaptana / Organizatöre Mesaj Gönder",
                            onClick = onDirectMessageHost,
                            isPrimary = true
                        )
                    } else if (isOnWaitlist) {
                        if (waitlistOfferUntil != null && waitlistOfferUntil > now) {
                            Text("Bekleme sırasında yer açıldı. Teklif süresi dolmadan yanıtla.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink600)
                            MButton(text = if (event.approvalRequired) "Kadro başvurusu gönder · organizatör onayı gerekir" else "Açılan yeri kabul et (${event.commitmentTokens} TKN kilit)", onClick = onAcceptWaitlistOffer)
                        }
                        MButton(text = "Bekleme listesinden ayrıl", onClick = { confirmMemberAction = "leave_waitlist" }, isPrimary = false)
                    } else if (event.status == EventStatus.FULL || displayedFreeSpots == 0) {
                        MButton(text = "Bekleme listesine katıl", onClick = onToggleWaitlist, isPrimary = true)
                    } else if (event.status in setOf(EventStatus.PUBLISHED, EventStatus.FILLING)) {
                        // YENİ KATILACAK KULLANICI: Doğrudan Davetli veya Standart
                        if (isDirectInvited) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFF0FDF4),
                                border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.size(34.dp).background(Color(0xFFDCFCE7), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.ConfirmationNumber, null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Özel Davet Kodu Doğrulandı", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF14532D))
                                        Text("İlan sahibinin doğrudan davetlisisin · Ev sahibi onayı gerekmeden anında katılabilirsin.", fontSize = 11.sp, color = Color(0xFF166534), lineHeight = 15.sp)
                                    }
                                }
                            }
                            MButton(
                                text = "⚡ Doğrudan Kadroya Katıl (${event.commitmentTokens} TKN kilit)",
                                onClick = onRequestJoin,
                                isPrimary = true
                            )
                        } else {
                            MButton(
                                text = if (event.approvalRequired) "Kadroya başvur (${event.commitmentTokens} TKN kilit)" else "Kadroya katıl (${event.commitmentTokens} TKN kilit)",
                                onClick = onRequestJoin,
                                isPrimary = true
                            )
                        }

                        OutlinedButton(
                            onClick = onDirectMessageHost,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MColors.Brand.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MColors.Brand)
                        ) {
                            Icon(Icons.Rounded.ChatBubbleOutline, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Organizatöre Mesaj Gönder", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text("Bu karşılaşma artık katılım almıyor.", fontSize = 12.sp, color = MColors.Ink500)
                    }

                    // Benzersiz İlan ID Kartı ve Paylaşım (yalnızca ilanı açan kişi görebilir)
                    if (isHost) {
                        var copiedToast by remember { mutableStateOf(false) }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("BENZERSİZ İLAN ID", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MColors.Ink400, letterSpacing = 0.5.sp)
                                    Text(eventDisplayId(event.id), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = MColors.BrandDeep)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
                                    FilledTonalButton(
                                        onClick = {
                                            clipboard.setText(androidx.compose.ui.text.AnnotatedString(event.id))
                                            copiedToast = true
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(13.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (copiedToast) "Kopyalandı" else "Kopyala", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = onShare,
                                        colors = ButtonDefaults.buttonColors(containerColor = MColors.Brand),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Rounded.Share, null, modifier = Modifier.size(13.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (backendMode) "Paylaş" else "Paylaş (+2 TKN)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    EventDeviceActions(event, showDirections = false)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onReportEvent) {
                            Icon(Icons.Rounded.Flag, null, tint = MColors.Ink400, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("İtiraz / Bildir", style = MaterialTheme.typography.labelSmall, color = MColors.Ink400)
                        }
                    }
                }
            }
        }
    }
    selectedPlayerProfile?.let { player ->
        PlayerProfileSheet(
            name = player.name,
            initials = player.initials,
            rating = player.rating,
            rank = 1,
            player = player,
            isInvitedToTeam = false,
            sport = event.sport,
            onDismiss = { selectedPlayerProfile = null },
            onChallenge = onChallengePlayer?.let { cb ->
                {
                    val target = player
                    selectedPlayerProfile = null
                    cb(target)
                }
            },
            onInviteTeam = onInvitePlayerToTeam?.let { cb ->
                {
                    val target = player
                    selectedPlayerProfile = null
                    cb(target)
                }
            },
            onDirectMessage = { selectedPlayerProfile = null; onDirectMessagePlayer(player) }
        )
    }
}
