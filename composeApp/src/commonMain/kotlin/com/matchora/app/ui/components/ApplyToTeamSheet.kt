package com.matchora.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.domain.Sport
import com.matchora.app.state.TeamProfile
import com.matchora.app.state.emoji
import com.matchora.app.state.label
import com.matchora.app.ui.theme.MColors

// ═══════════════════════════════════════════════════════════════════════════════
// APPLY TO TEAM SHEET — TAKIM KADROSUNA BAŞVURU FORMU
// Sporcu mevkisini ve kaptana ileteceği notu seçerek kulüp kadrosuna başvurur.
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyToTeamSheet(
    team: TeamProfile,
    onDismiss: () -> Unit,
    onSubmitApplication: (position: String, note: String) -> Unit
) {
    var selectedPosition by remember { mutableStateOf("Orta Saha") }
    var applicationNote by remember { mutableStateOf("") }

    val positions = remember(team.sport) {
        when (team.sport) {
            Sport.FOOTBALL -> listOf("Kaleci", "Defans", "Orta Saha", "Kanat", "Forvet")
            Sport.BASKETBALL -> listOf("Oyun Kurucu (PG)", "Şutör (SG)", "Kısa Forvet (SF)", "Uzun Forvet (PF)", "Pivot (C)")
            Sport.VOLLEYBALL -> listOf("Pasör", "Smaçör", "Orta Oyuncu", "Libero")
            Sport.TENNIS, Sport.PADEL -> listOf("Tekler", "Çiftler (Sağ)", "Çiftler (Sol)")
            Sport.ESPORTS_FC -> listOf("1v1 Ana Oyuncu", "Yedek Oyuncu")
            Sport.ESPORTS_VALORANT -> listOf("Duelist", "Initiator", "Controller", "Sentinel", "IGL (Kaptan)")
            else -> listOf("Ana Kadro", "Yedek", "Rotasyon")
        }
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
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Başlık & Kapat
            item {
                MSheetHeading(
                    title = "Kadroya başvur",
                    subtitle = "${team.name} · ${team.sport.label} (${team.area})",
                    icon = Icons.Rounded.PersonAddAlt1,
                    onDismiss = onDismiss
                )
            }

            // Mevki Seçimi (Yüzen Gölgeli Açılır Menü)
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 5.dp,
                    border = BorderStroke(1.dp, Color(0xFFE4E9F2)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 7.dp,
                            shape = RoundedCornerShape(20.dp),
                            ambientColor = Color(0x280B1220),
                            spotColor = Color(0x380B1220)
                        )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        MFloatingDropdownBox(
                            label = "Tercih Edilen Mevki / Rol",
                            selectedValue = selectedPosition,
                            options = positions,
                            optionLabel = { it },
                            onSelect = { selectedPosition = it },
                            icon = Icons.Rounded.Sports
                        )
                    }
                }
            }

            // Kaptana Not (Premium Yüzen Açıklama Kutusu)
            item {
                MFloatingInputBox(
                    label = "Kaptana Başvuru Notu",
                    value = applicationNote,
                    onValueChange = { applicationNote = it },
                    placeholder = "Örn: Kadıköy'de oturuyorum, haftalık maçlarınızda düzenli oynayabilirim.",
                    subtitle = "Oyun stilini ve müsaitlik durumunu kısaca paylaş.",
                    icon = Icons.Rounded.EditNote,
                    badgeText = "İsteğe bağlı",
                    singleLine = false,
                    minLines = 2,
                    maxLines = 4,
                    suggestions = listOf(
                        "Haftalık maçlara düzenli katılırım",
                        "Mevkimde tecrübeliyim, kondisyonum hazır",
                        "Bölgeye yakınım, son dakika da gelebilirim"
                    )
                )
            }

            // Güvence Kapsülü (Yüzen Beyaz Ada)
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.dp, Color(0xFFE4E9F2)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = Color(0x240B1220),
                            spotColor = Color(0x340B1220)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MColors.MintTint,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.VerifiedUser, null, tint = MColors.Mint, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Başvurun kulüp kaptanına iletilir. Onaylandığında bildirim ve takım sohbet erişimi açılır.",
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                            color = MColors.Ink600
                        )
                    }
                }
            }

            // Gönder Butonu
            item {
                Button(
                    onClick = {
                        onSubmitApplication(selectedPosition, applicationNote)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MColors.Brand),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "Başvuruyu İlet 👥",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
