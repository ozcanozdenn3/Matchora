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
    isJoined: Boolean,
    isCheckedIn: Boolean,
    isOnWaitlist: Boolean = false,
    isHost: Boolean = false,
    pendingRequest: JoinRequest? = null,
    pendingRequestsCount: Int = 0,
    onDismiss: () -> Unit,
    onToggleJoin: () -> Unit,
    onToggleWaitlist: () -> Unit = {},
    onRequestJoin: () -> Unit = {},
    onCancelRequest: () -> Unit = {},
    onOpenManageRequests: () -> Unit = {},
    onDirectMessageHost: () -> Unit = {},
    onCheckIn: () -> Unit,
    onOpenChat: () -> Unit,
    onReportEvent: () -> Unit,
    onStartEvent: () -> Unit = {},
    onCompleteEvent: () -> Unit = {},
    onCancelEvent: () -> Unit = {},
    hasResult: Boolean = false,
    onOpenResult: () -> Unit = {},
    onShare: () -> Unit
) {
    var showMatchScoreDetail by remember { mutableStateOf(false) }
    var hostAction by remember { mutableStateOf<String?>(null) }
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
            confirmButton = { TextButton(onClick = { hostAction = null; when (action) { "start" -> onStartEvent(); "complete" -> onCompleteEvent(); else -> onCancelEvent() } }) { Text("Onayla") } },
            dismissButton = { TextButton(onClick = { hostAction = null }) { Text("Vazgeç") } })
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
                            MButton(if (event.status == EventStatus.IN_PROGRESS) "Maçı tamamla" else "Maçı başlat", {
                                hostAction = if (event.status == EventStatus.IN_PROGRESS) "complete" else "start"
                            }, isPrimary = false)
                            TextButton(onClick = { hostAction = "cancel" }) { Text("Maçı iptal et", color = MColors.Rose) }
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

            event.approximateLocation?.let { point ->
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
                                Text(if (event.pricePerPersonTry > 0) "TKN ${event.pricePerPersonTry}" else "Ücretsiz", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MColors.Brand, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            items(displayedParticipantIds) { pid ->
                val p = if (pid == DemoData.ME) currentPlayer else DemoData.player(pid)
                if (p != null) {
                    MCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(p.color),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(p.initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
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
                                MBadge(text = "R:${p.reliability}", textColor = MColors.Mint, backgroundColor = MColors.MintTint)
                                if (p.chemistry != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    MBadge(text = "%${p.chemistry}", textColor = MColors.Lilac, backgroundColor = MColors.LilacTint)
                                }
                            }
                        }
                    }
                }
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
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MColors.Brand),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Ö", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Sen · ${currentPlayer.name}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MColors.Ink900, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("10 TKN taahhüt kilitlendi", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = MColors.Ink600)
                                }
                            }
                            MBadge(
                                text = if (isCheckedIn) "✓ Check-in" else "Kadroda",
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
                                    Text("Rol: ${pendingRequest.requestedPosition ?: "Oyuncu"} · 10 TKN kilitte", fontSize = 10.sp, color = MColors.Ink600)
                                }
                            }
                            TextButton(onClick = onCancelRequest) {
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
                        // KAPTAN GÖRÜNÜMÜ: Başvuruları Yönet
                        MButton(
                            text = "📋 Başvuruları yönet ($pendingRequestsCount Bekleyen)",
                            onClick = onOpenManageRequests,
                            isPrimary = true
                        )

                        MButton(
                            text = "💬 Etkinlik Sohbetine Git",
                            onClick = onOpenChat,
                            isPrimary = false
                        )
                    } else if (isJoined) {
                        // KATILMIŞ KULLANICI GÖRÜNÜMÜ
                        if (!isCheckedIn) {
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

                        OutlinedButton(
                            onClick = onToggleJoin,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MColors.Rose)
                        ) {
                            Text("Maç Kadrosundan Çık (Taahhüt İade Edilir)")
                        }
                    } else if (pendingRequest != null) {
                        // BAŞVURUSU BEKLEYEN KULLANICI
                        MButton(
                            text = "💬 Kaptana / Organizatöre Mesaj Gönder",
                            onClick = onDirectMessageHost,
                            isPrimary = true
                        )
                    } else if (isOnWaitlist) {
                        MButton(text = "Bekleme listesinden ayrıl", onClick = onToggleWaitlist, isPrimary = false)
                    } else if (event.status == EventStatus.FULL || displayedFreeSpots == 0) {
                        MButton(text = "Bekleme listesine katıl", onClick = onToggleWaitlist, isPrimary = true)
                    } else if (event.status in setOf(EventStatus.PUBLISHED, EventStatus.FILLING)) {
                        // YENİ KATILACAK KULLANICI: Kadroya Başvur + Organizatöre Mesaj
                        MButton(
                            text = "✓ Kadroya Katıl / Başvur (${event.commitmentTokens} TKN kilit)",
                            onClick = onRequestJoin,
                            isPrimary = true
                        )

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

                    // Paylaş & Şikayet Et
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = onShare) {
                            Icon(Icons.Rounded.Share, null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Davet Linkini Paylaş", style = MaterialTheme.typography.labelSmall)
                        }
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
}
