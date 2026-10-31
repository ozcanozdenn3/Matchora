package com.matchora.app.ui.components

import com.matchora.app.ui.components.premiumSurface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.state.Player
import com.matchora.app.state.TeamJoinRequest
import com.matchora.app.state.TeamProfile
import com.matchora.app.ui.theme.MColors

// ═══════════════════════════════════════════════════════════════════════════════
// MANAGE TEAM REQUESTS SHEET — TAKIM KADRO BAŞVURULARI YÖNETİMİ
// Kaptan takıma girmek isteyen sporcuların başvurularını onaylar, reddeder veya DM atar.
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageTeamRequestsSheet(
    team: TeamProfile,
    requests: List<TeamJoinRequest>,
    onDismiss: () -> Unit,
    onApprove: (TeamJoinRequest) -> Unit,
    onReject: (TeamJoinRequest) -> Unit,
    onDirectMessage: (Player) -> Unit
) {
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
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Başlık & Kapat
            item {
                MSheetHeading(
                    title = "Kadro başvuruları",
                    subtitle = "${team.name} · ${requests.size} bekleyen sporcu",
                    icon = Icons.Rounded.Groups,
                    onDismiss = onDismiss
                )
            }

            if (requests.isEmpty()) {
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
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("👥", fontSize = 34.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Bekleyen kadro başvurusu bulunmuyor",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MColors.Ink800
                            )
                        }
                    }
                }
            } else {
                items(requests) { req ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE4E9F2)),
                        shadowElevation = 5.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(7.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x280B1220), spotColor = Color(0x380B1220))
                    ) {
                        Column(modifier = Modifier.premiumSurface().padding(14.dp)) {
                            // Sporcu Bilgisi
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(req.player.color.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = req.player.name.take(1),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = req.player.color
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = req.player.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MColors.Ink900
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "• ${req.player.rating} MMR",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MColors.Brand
                                            )
                                        }
                                        Text(
                                            text = "Tercih: ${req.preferredPosition} · ${req.timestamp}",
                                            fontSize = 11.sp,
                                            color = MColors.Ink500
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
                                        text = "%${req.player.reliability} Güvenilirlik",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MColors.Mint
                                    )
                                }
                            }

                            // Sporcu Mesajı
                            if (!req.message.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF8FAFC),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "💬 \"${req.message}\"",
                                        fontSize = 12.sp,
                                        color = MColors.Ink700,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = Color(0x0C000000))
                            Spacer(modifier = Modifier.height(10.dp))

                            // Aksiyon Butonları
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onApprove(req) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MColors.Mint),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                ) {
                                    Text("✓ Kadroya Al", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { onReject(req) },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0x20000000)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                ) {
                                    Text("✕ Reddet", color = MColors.Ink700, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { onDirectMessage(req.player) },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, MColors.Brand),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Icon(Icons.Rounded.Chat, contentDescription = "Mesaj", tint = MColors.Brand, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
