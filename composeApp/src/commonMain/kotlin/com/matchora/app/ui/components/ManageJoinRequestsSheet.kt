package com.matchora.app.ui.components

import com.matchora.app.ui.components.premiumSurface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.state.*
import com.matchora.app.ui.theme.MColors

// ═══════════════════════════════════════════════════════════════════════════════
// KATILIM İSTEKLERİ & KADRO ONAY/RET YÖNETİMİ (ORGANİZATÖR & KAPTAN ÇEKMECESİ)
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageJoinRequestsSheet(
    event: SportEvent,
    requests: List<JoinRequest>,
    onDismiss: () -> Unit,
    onApprove: (JoinRequest) -> Unit,
    onReject: (JoinRequest) -> Unit,
    onDirectMessage: (Player) -> Unit,
    onShare: () -> Unit = {}
) {
    val pendingRequests = remember(requests) {
        requests.filter { it.status == JoinRequestStatus.PENDING }
    }
    val processedRequests = remember(requests) {
        requests.filter { it.status != JoinRequestStatus.PENDING }
    }
    var copyFeedback by remember { mutableStateOf(false) }
    var selectedPlayerProfile by remember { mutableStateOf<Player?>(null) }

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
            // Başlık
            item {
                MSheetHeading(
                    title = "İlan Yönetim Paneli",
                    subtitle = "${event.title} · ${pendingRequests.size} bekleyen istek (${event.freeSpots} boş yer)",
                    icon = Icons.Rounded.DashboardCustomize,
                    onDismiss = onDismiss
                )
            }

            // Benzersiz Paylaşılabilir İlan ID Kartı
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x200B1220), spotColor = Color(0x300B1220))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(MColors.BrandTint, RoundedCornerShape(11.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Tag, null, tint = MColors.Brand, modifier = Modifier.size(19.dp))
                                }
                                Column {
                                    Text("BENZERSİZ İLAN ID", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MColors.Ink400, letterSpacing = 0.5.sp)
                                    Text(eventDisplayId(event.id), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = MColors.BrandDeep)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
                                FilledTonalButton(
                                    onClick = {
                                        clipboard.setText(androidx.compose.ui.text.AnnotatedString(event.id))
                                        copyFeedback = true
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text(if (copyFeedback) "Kopyalandı" else "Kopyala", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = onShare,
                                    colors = ButtonDefaults.buttonColors(containerColor = MColors.Brand),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Rounded.Share, null, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text("Paylaş (+2 TKN)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Rounded.Info, null, tint = MColors.Ink500, modifier = Modifier.size(14.dp))
                                Text("Bu ID'yi alan sporcular, ana sayfadaki 'ID ile bul' alanından ilana doğrudan onaysız katılabilir.", fontSize = 10.sp, color = MColors.Ink600)
                            }
                        }
                    }
                }
            }

            // Token Güvence Kartı
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("💎", fontSize = 20.sp)
                        Column {
                            Text("10 TKN İlan Teminatı Kilitli", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF92400E))
                            Text("Maç tamamlandığında veya check-in yapıldığında kilitli token cüzdanına iade edilir.", fontSize = 11.sp, color = Color(0xFFB45309))
                        }
                    }
                }
            }

            // Bekleyen Başvurular Başlığı
            item {
                Text(
                    text = "Onay Bekleyen Sporcular",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MColors.Ink800
                )
            }

            if (pendingRequests.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        shadowElevation = 5.dp,
                        border = BorderStroke(1.dp, Color(0xFFE4E9F2)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(7.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x260B1220), spotColor = Color(0x360B1220))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("✨", fontSize = 28.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Bekleyen Başvuru Yok",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MColors.Ink900
                            )
                            Text(
                                text = "İlanınız yayında. Yeni bir sporcu katılmak istediğinde bildirim alacaksınız.",
                                fontSize = 11.sp,
                                color = MColors.Ink500,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(pendingRequests) { req ->
                    val player = req.player
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        border = BorderStroke(1.5.dp, MColors.Brand.copy(alpha = 0.35f)),
                        shadowElevation = 5.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(7.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x280B1220), spotColor = Color(0x380B1220))
                    ) {
                        Column(modifier = Modifier.premiumSurface().padding(14.dp)) {
                            // Oyuncu Üst Satırı
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedPlayerProfile = player }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(player.color),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val avatar = playerAvatarUploadId(player)
                                        if (avatar != null) BackendPhoto(avatar, player.name, Modifier.fillMaxSize())
                                        else Text(
                                            text = player.initials,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = player.name,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                color = MColors.Ink900
                                            )
                                            if (player.verified) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(Icons.Rounded.Verified, null, tint = MColors.Mint, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                        Text(
                                            text = "${player.area} · ${req.requestedPosition ?: player.position ?: "Oyuncu"}",
                                            fontSize = 11.sp,
                                            color = MColors.Ink500
                                        )
                                    }
                                }

                                Text(
                                    text = req.timestamp,
                                    fontSize = 10.sp,
                                    color = MColors.Ink400,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Metrikler
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF0FDF4),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🛡️", fontSize = 11.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${player.reliabilityLabel} Güven",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MColors.Mint
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFFF7ED),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("⚡", fontSize = 11.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = player.ratingLabel,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MColors.Brand
                                        )
                                    }
                                }

                                if (player.chemistry != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF5F3FF),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("🧬", fontSize = 11.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "%${player.chemistry} Uyum",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MColors.Lilac,
                                                softWrap = false,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            // Başvuru Notu (Varsa)
                            if (!req.message.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF8FAFC),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text("💬", fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = req.message,
                                            fontSize = 11.sp,
                                            color = MColors.Ink800,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Aksiyon Butonları: [✓ Onayla] [✕ Reddet] [💬 Mesaj At]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. ONAYLA (Primary Mint)
                                Button(
                                    onClick = { onApprove(req) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MColors.Mint),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f).height(38.dp)
                                ) {
                                    Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Onayla", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                }

                                // 2. REDDET (Soft Red Outlined)
                                OutlinedButton(
                                    onClick = { onReject(req) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MColors.Rose),
                                    border = BorderStroke(1.dp, MColors.Rose.copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(0.9f).height(38.dp)
                                ) {
                                    Icon(Icons.Rounded.Close, null, tint = MColors.Rose, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Reddet", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MColors.Rose)
                                }

                                // 3. MESAJ AT (1-on-1 DM Açma)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, Color(0x18000000)),
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clickable { onDirectMessage(player) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.ChatBubbleOutline,
                                            contentDescription = "Mesaj Gönder",
                                            tint = MColors.Brand,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                // 4. SPORCU PASAPORTU
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, Color(0x18000000)),
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clickable { selectedPlayerProfile = player }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("🪪", fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // İşlenmiş / Geçmiş Başvurular (Varsa)
            if (processedRequests.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "İşlem Gören Başvurular",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MColors.Ink800
                    )
                }

                items(processedRequests) { req ->
                    val isApproved = req.status == JoinRequestStatus.APPROVED
                    val isCancelled = req.status == JoinRequestStatus.CANCELLED
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0x10000000)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .clickable { selectedPlayerProfile = req.player },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(req.player.color),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val avatar = playerAvatarUploadId(req.player)
                                    if (avatar != null) BackendPhoto(avatar, req.player.name, Modifier.fillMaxSize())
                                    else Text(req.player.initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(req.player.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MColors.Ink900)
                                    Text(req.requestedPosition ?: "Oyuncu", fontSize = 10.sp, color = MColors.Ink500)
                                }
                            }

                            MBadge(
                                text = when {
                                    isApproved -> "✓ Onaylandı"
                                    isCancelled -> "↩ İptal edildi"
                                    else -> "✕ Reddedildi"
                                },
                                textColor = when {
                                    isApproved -> MColors.Mint
                                    isCancelled -> MColors.Ink600
                                    else -> MColors.Rose
                                },
                                backgroundColor = when {
                                    isApproved -> Color(0xFFF0FDF4)
                                    isCancelled -> Color(0xFFF1F5F9)
                                    else -> Color(0xFFFEF2F2)
                                }
                            )
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
            sport = event.sport,
            onDismiss = { selectedPlayerProfile = null },
            onChallenge = {
                selectedPlayerProfile = null
            },
            onInviteTeam = {
                selectedPlayerProfile = null
            },
            onDirectMessage = {
                selectedPlayerProfile = null
                onDirectMessage(player)
            }
        )
    }
}
