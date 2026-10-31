package com.matchora.app.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.domain.Sport
import com.matchora.app.state.TeamProfile
import com.matchora.app.state.emoji
import com.matchora.app.state.label
import com.matchora.app.ui.theme.MColors

// ═══════════════════════════════════════════════════════════════════════════════
// CREATE TEAM SHEET — RESMİ SPOR KULÜBÜ / TAKIM OLUŞTURMA
// Kaptan takım kurarken branşı, bölgeyi, armada rengini ve oyuncu alım kuralını belirler.
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTeamSheet(
    onDismiss: () -> Unit,
    onCreateTeam: (TeamProfile) -> Unit
) {
    var teamName by remember { mutableStateOf("") }
    var selectedSport by remember { mutableStateOf(Sport.FOOTBALL) }
    var selectedArea by remember { mutableStateOf("Kadıköy") }
    var badgeText by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    var minMmr by remember { mutableIntStateOf(1200) }
    var recruitingOpen by remember { mutableStateOf(true) }
    var bio by remember { mutableStateOf("") }

    val teamColors = listOf(
        MColors.Brand,
        Color(0xFF2563EB),
        Color(0xFF0FA968),
        Color(0xFF7457F5),
        Color(0xFFDC2626),
        Color(0xFFD97706),
        Color(0xFF0284C7)
    )

    val areas = listOf("Kadıköy", "Beşiktaş", "Üsküdar", "Ataşehir", "Moda", "Bostancı", "Online / Crossplay")
    val mmrOptions = listOf(1000, 1200, 1400, 1600)

    val effectiveBadge = remember(badgeText, teamName) {
        if (badgeText.isNotBlank()) badgeText.take(3).uppercase()
        else teamName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("").ifEmpty { "TK" }
    }

    ModalBottomSheet(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        scrimColor = androidx.compose.ui.graphics.Color(0xFF0B1220).copy(alpha = .48f),

        onDismissRequest = onDismiss,
        modifier = Modifier.statusBarsPadding().padding(top = 12.dp),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFFF5F7F4),
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
                MSheetHeading("Kulüp kur", "Renklerini seç, kadronu bir araya getir.", Icons.Rounded.Groups, onDismiss)
            }

            // Canlı Takım Arması & Önizleme
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF20362D),
                    border = BorderStroke(1.dp, Color(0x334F775E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(teamColors[selectedColorIndex]),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = effectiveBadge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                text = teamName.ifBlank { "Takımının adı" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                color = if (teamName.isNotBlank()) Color.White else Color(0xFFB8C8BD),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${selectedSport.emoji} ${selectedSport.label} · $selectedArea · Min $minMmr MMR",
                                fontSize = 12.sp,
                                color = Color(0xFFD1DDD5),
                                fontWeight = FontWeight.Medium,
                                maxLines = 2
                            )
                        }
                    }
                }
            }

            // Takım Adı & Arma Kısaltması (Yüzen Beyaz Ada)
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 5.dp,
                    border = BorderStroke(1.dp, Color(0xFFE4E9F2)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(7.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x280B1220), spotColor = Color(0x380B1220))
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Kulüp Kimliği", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MColors.Ink900)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                value = teamName,
                                onValueChange = { teamName = it },
                                label = { Text("Takım Adı") },
                                placeholder = { Text("Örn: Kadıköy Wolves") },
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true,
                                modifier = Modifier.weight(2f),
                                colors = premiumTextFieldColors()
                            )

                            OutlinedTextField(
                                textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                value = badgeText,
                                onValueChange = { if (it.length <= 3) badgeText = it },
                                label = { Text("Arma") },
                                placeholder = { Text("KW") },
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = premiumTextFieldColors()
                            )
                        }
                    }
                }
            }

            // Renk Seçici
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White,
                    shadowElevation = 5.dp,
                    border = BorderStroke(1.dp, Color(0xFFE4E9F2)),
                    modifier = Modifier.fillMaxWidth().shadow(7.dp, RoundedCornerShape(22.dp), ambientColor = Color(0x260B1220), spotColor = Color(0x360B1220))
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Kulüp renkleri", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MColors.Ink900)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            teamColors.forEachIndexed { index, color ->
                                val isSel = selectedColorIndex == index
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .shadow(if (isSel) 6.dp else 3.dp, CircleShape)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSel) 3.dp else 1.dp,
                                            color = if (isSel) Color.White else Color(0x20000000),
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColorIndex = index },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSel) {
                                        Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Branş, Ana Saha, Başvurular ve Minimum MMR (Yüzen Gölgeli Açılır Menüler)
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White,
                    shadowElevation = 5.dp,
                    border = BorderStroke(1.dp, Color(0xFFE4E9F2)),
                    modifier = Modifier.fillMaxWidth().shadow(7.dp, RoundedCornerShape(22.dp), ambientColor = Color(0x260B1220), spotColor = Color(0x360B1220))
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SportPickerField(selectedSport, { if (it != null) selectedSport = it }, allowAll = false)

                        MFloatingDropdownBox(
                            label = "Kulübün Ana Sahası",
                            selectedValue = selectedArea,
                            options = areas,
                            optionLabel = { it },
                            onSelect = { selectedArea = it },
                            icon = Icons.Rounded.LocationOn
                        )

                        MFloatingDropdownBox(
                            label = "Oyuncu Başvuruları",
                            selectedValue = recruitingOpen,
                            options = listOf(true, false),
                            optionLabel = { if (it) "Açık · Katılıma Uygun" else "Kapalı · Sadece Davetle" },
                            onSelect = { recruitingOpen = it },
                            icon = Icons.Rounded.PersonAddAlt1
                        )

                        MFloatingDropdownBox(
                            label = "Minimum MMR",
                            selectedValue = minMmr,
                            options = mmrOptions,
                            optionLabel = { "$it+ MMR" },
                            onSelect = { minMmr = it },
                            icon = Icons.Rounded.Leaderboard
                        )
                    }
                }
            }

            // Takım Açıklaması (Premium Yüzen Açıklama Kutusu)
            item {
                MFloatingInputBox(
                    label = "Takım Hedefi & Kısa Açıklama",
                    value = bio,
                    onValueChange = { bio = it },
                    placeholder = "Örn: Hafta içi akşamları düzenli rekabetçi maçlar arıyoruz.",
                    subtitle = "Kulübünün oyun kültürünü ve aradığı sporcu profilini özetle.",
                    icon = Icons.Rounded.EditNote,
                    badgeText = "Kulüp Vizyonu",
                    singleLine = false,
                    minLines = 2,
                    maxLines = 4,
                    suggestions = listOf(
                        "Hafta içi akşam düzenli rekabetçi maçlar",
                        "Centilmence oyun ve yüksek kondisyon",
                        "Turnuva & Şehir Ligi şampiyonluk hedefi"
                    )
                )
            }

            // Takımı Kur Butonu
            item {
                MButton(
                    text = "Kulübü kur ve devam et",
                    onClick = {
if (teamName.isNotBlank()) {
                            val newTeam = TeamProfile(
                                id = "team_${teamName.hashCode()}",
                                name = teamName.trim(),
                                sport = selectedSport,
                                area = selectedArea,
                                badge = effectiveBadge,
                                color = teamColors[selectedColorIndex],
                                captainId = "me",
                                coCaptainId = null,
                                memberIds = listOf("me"),
                                substituteIds = emptyList(),
                                rating = 1400,
                                wins = 0,
                                losses = 0,
                                draws = 0,
                                verifiedMatches = 0,
                                fairPlay = 5.0f,
                                reliability = 100,
                                lookingForOpponent = true,
                                availability = "Hafta içi & Hafta sonu",
                                isMine = true,
                                description = bio.ifBlank { "Yeni kurulan aktif spor takımı." },
                                recruitingOpen = recruitingOpen,
                                requiredMmr = minMmr
                            )
                            onCreateTeam(newTeam)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.Shield,
                    enabled = teamName.isNotBlank()
                )
            }
        }
    }
}
