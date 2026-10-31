package com.matchora.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.matchora.app.state.*
import com.matchora.app.ui.theme.MColors

// ═══════════════════════════════════════════════════════════════════════════════
// KADROYA KATILMA / BAŞVURU ÇEKMECESİ (POZİSYON SEÇİMİ & TAAHHÜT KİLİDİ)
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitJoinRequestSheet(
    event: SportEvent,
    tokenBalance: Int,
    onDismiss: () -> Unit,
    onSubmitRequest: (position: String, note: String) -> Unit,
    onOpenTopUp: () -> Unit
) {
    val suggestedPositions = remember(event.sport) {
        when (event.sport) {
            Sport.FOOTBALL -> listOf("Orta Saha (MID)", "Forvet (FWD)", "Defans (DEF)", "Kaleci (GK)", "Kanat")
            Sport.BASKETBALL -> listOf("Point Guard", "Shooting Guard", "Small Forward", "Power Forward", "Center")
            Sport.VOLLEYBALL -> listOf("Smaçör", "Pasör", "Orta Oyuncu", "Libero", "Pasör Çaprazı")
            Sport.TENNIS, Sport.PADEL -> listOf("Tekler Oyuncusu", "Çiftler (Sol)", "Çiftler (Sağ)")
            Sport.ESPORTS_VALORANT, Sport.ESPORTS_CS2 -> listOf("Duelist / Entry", "Initiator / Öncü", "Controller / Smoke", "Sentinel / Anchor", "IGL (Kaptan)")
            Sport.ESPORTS_LOL -> listOf("Mid Laner", "ADC / Carry", "Support / Destek", "Top Laner", "Jungler")
            Sport.ESPORTS_FC -> listOf("1v1 Standart", "2v2 Co-Op")
            else -> listOf("Oyuncu", "Yedek", "Kaptan")
        }
    }

    var selectedPosition by remember { mutableStateOf(suggestedPositions.first()) }
    var applicantNote by remember { mutableStateOf("") }
    val hasEnoughTokens = tokenBalance >= event.commitmentTokens

    ModalBottomSheet(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        scrimColor = androidx.compose.ui.graphics.Color(0xFF0B1220).copy(alpha = .48f),

        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF5F6F8),
        dragHandle = { MPremiumSheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Başlık
            MSheetHeading(
                title = "Kadroya katıl",
                subtitle = "${event.title} · Son ${event.freeSpots} boş yer",
                icon = Icons.Rounded.Sports,
                onDismiss = onDismiss
            )

            // Pozisyon / Rol Seçimi (Yüzen Gölgeli Açılır Menü)
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
                        label = "Oynamak İstediğin Rol",
                        selectedValue = selectedPosition,
                        options = suggestedPositions,
                        optionLabel = { it },
                        onSelect = { selectedPosition = it },
                        icon = Icons.Rounded.Sports
                    )
                }
            }

            // Kaptana / Organizatöre Not (Premium Yüzen Açıklama Kutusu)
            MFloatingInputBox(
                label = "Organizatöre Kısa Not",
                value = applicantNote,
                onValueChange = { applicantNote = it },
                placeholder = "Örn: Moda'dayım, forma hazır, zamanında gelirim.",
                subtitle = "Kaptana katılım durumunla ilgili kısa bir bilgi bırak.",
                icon = Icons.Rounded.EditNote,
                badgeText = "Opsiyonel",
                singleLine = false,
                minLines = 2,
                maxLines = 3,
                suggestions = listOf(
                    "Forma hazır, vaktinde sahadayım",
                    "Bölgedeyim, hemen katılabilirim",
                    "Ekipmanım tam, her mevkide oynarım"
                )
            )

            // Taahhüt Kilidi Bilgilendirme Kapsülü (Yüzen Beyaz Ada)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 5.dp,
                border = BorderStroke(1.dp, Color(0xFFE4E9F2)),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 7.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = Color(0x260B1220),
                        spotColor = Color(0x360B1220)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MColors.BrandTint,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Lock, null, tint = MColors.BrandDeep, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${event.commitmentTokens} TKN Taahhüt Kilidi",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MColors.Ink900
                        )
                        Text(
                            text = "Maça gidip check-in yaptığında token hesabına iade edilir ve +2 Güven puanı kazanırsın.",
                            fontSize = 10.5.sp,
                            color = MColors.Ink600,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            // Bakiye ve Aksiyon Butonu
            if (!hasEnoughTokens) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF2F2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Yetersiz Bakiye: Mevcut $tokenBalance TKN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MColors.Rose
                        )
                        TextButton(onClick = onOpenTopUp) {
                            Text("Token Yükle →", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MColors.Rose)
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (hasEnoughTokens) {
                        onSubmitRequest(selectedPosition, applicantNote)
                        onDismiss()
                    } else {
                        onOpenTopUp()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (hasEnoughTokens) MColors.Brand else Color(0xFF94A3B8)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Rounded.Send, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (hasEnoughTokens) "Katılım İsteğini Gönder (10 TKN Kilitlensin)" else "Önce Token Yükle",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
