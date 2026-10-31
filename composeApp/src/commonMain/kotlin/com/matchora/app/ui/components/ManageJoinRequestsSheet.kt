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
    onDirectMessage: (Player) -> Unit
) {
    val pendingRequests = remember(requests) {
        requests.filter { it.status == JoinRequestStatus.PENDING }
    }
    val processedRequests = remember(requests) {
        requests.filter { it.status != JoinRequestStatus.PENDING }
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
            // Başlık
            item {
                MSheetHeading(
                    title = "Kadro başvuruları",
                    subtitle = "${event.title} · ${pendingRequests.size} bekleyen (${event.freeSpots} boş yer)",
                    icon = Icons.Rounded.FactCheck,
                    onDismiss = onDismiss
                )
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
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(player.color),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
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
                                            text = "%${player.reliability} Güven",
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
                                            text = "${player.rating} MMR",
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
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0x10000000)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
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
                                    Text(req.player.initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(req.player.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MColors.Ink900)
                                    Text(req.requestedPosition ?: "Oyuncu", fontSize = 10.sp, color = MColors.Ink500)
                                }
                            }

                            MBadge(
                                text = if (isApproved) "✓ Onaylandı" else "✕ Reddedildi",
                                textColor = if (isApproved) MColors.Mint else MColors.Rose,
                                backgroundColor = if (isApproved) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
                            )
                        }
                    }
                }
            }
        }
    }
}
