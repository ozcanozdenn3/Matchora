package com.matchora.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.data.*
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
    onCreateTeam: (TeamProfile) -> Unit,
    currentCity: String = "İstanbul",
    currentArea: String = "Kadıköy",
    onPublished: (String) -> Unit = {}
) {
    val form = rememberCreationForm("team")
    val draft = form.restored
    var teamName by remember(form) { mutableStateOf(draft.s("name")) }
    var selectedSport by remember(form) { mutableStateOf(draft.formEnum("sport", Sport.FOOTBALL)) }
    var selectedArea by remember(form) { mutableStateOf(draft.s("area", currentArea)) }
    var selectedCity by remember(form) { mutableStateOf(draft.s("city", currentCity)) }
    var homeVenue by remember(form) { mutableStateOf(draft.s("venue")) }
    var availability by remember(form) { mutableStateOf(draft.s("availability", "Hafta içi & Hafta sonu")) }
    var showRegion by remember { mutableStateOf(false) }
    var badgeText by remember(form) { mutableStateOf(draft.s("badge")) }
    var selectedColorIndex by remember(form) { mutableIntStateOf(draft.i("color").coerceIn(0, 7)) }
    var minMmr by remember(form) { mutableIntStateOf(draft.i("mmr", 1000)) }
    var recruitingOpen by remember(form) { mutableStateOf(draft.b("recruiting", true)) }
    var bio by remember(form) { mutableStateOf(draft.s("bio")) }

    val teamColors = listOf(
        MColors.Brand,
        Color(0xFF203B35),
        Color(0xFF2563EB),
        Color(0xFF0FA968),
        Color(0xFF7C3AED),
        Color(0xFFDC2626),
        Color(0xFFD97706),
        Color(0xFF111827)
    )

    val mmrOptions = listOf(1000, 1200, 1400, 1600)

    val effectiveBadge = remember(badgeText, teamName) {
        if (badgeText.isNotBlank()) badgeText.take(3).uppercase()
        else teamName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("").ifEmpty { "TK" }
    }

    CreationDraftEffect(form) { apiPayload(
        "name" to teamName, "sport" to selectedSport, "area" to selectedArea, "city" to selectedCity,
        "venue" to homeVenue, "availability" to availability, "badge" to badgeText,
        "color" to selectedColorIndex, "mmr" to minMmr, "recruiting" to recruitingOpen, "bio" to bio
    ) }

    ModalBottomSheet(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        scrimColor = Color(0xFF0B1220).copy(alpha = .48f),
        onDismissRequest = { form.requestDismiss(onDismiss) },
        modifier = Modifier.statusBarsPadding().padding(top = 12.dp),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { it != SheetValue.Hidden || form.confirmDismiss(onDismiss) }),
        containerColor = Color(0xFFF8FAFC),
        dragHandle = { MPremiumSheetHandle() }
    ) {
        CreationFormBody(form) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                MSheetHeading("Kulüp kur", "Renklerini seç, kadronu bir araya getir.", Icons.Rounded.Groups, { form.requestDismiss(onDismiss) })
            }

            // Kart 1: Kulüp Kimliği, Canlı Arma & Renk Stüdyosu
            item {
                ClubSectionCard {
                    ClubIdentityPreviewBanner(
                        badgeText = effectiveBadge,
                        crestColor = teamColors[selectedColorIndex],
                        teamName = teamName,
                        sportEmoji = selectedSport.emoji,
                        sportLabel = selectedSport.label,
                        city = selectedCity,
                        area = selectedArea,
                        recruitingOpen = recruitingOpen,
                        minMmr = minMmr
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MPremiumTextField(
                            value = teamName,
                            onValueChange = { teamName = it },
                            label = "Takım / Kulüp Adı",
                            placeholder = "Örn: Kadıköy Wolves",
                            modifier = Modifier.weight(2.1f)
                        )
                        MPremiumTextField(
                            value = badgeText,
                            onValueChange = { if (it.length <= 3) badgeText = it },
                            label = "Arma (Maks 3)",
                            placeholder = "KW",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    ClubFieldLabel("ARMA RENGİ")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        teamColors.forEachIndexed { index, color ->
                            val isSel = selectedColorIndex == index
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSel) 2.5.dp else 1.dp,
                                        color = if (isSel) Color.White else Color(0x26000000),
                                        shape = CircleShape
                                    )
                                    .then(
                                        if (isSel) Modifier.border(1.5.dp, MColors.Ink900, CircleShape)
                                        else Modifier
                                    )
                                    .clickable { selectedColorIndex = index },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSel) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Kart 2: Branş, Bölge & Ev Sahibi Saha
            item {
                ClubSectionCard {
                    ClubSectionHeader(Icons.Rounded.Place, "Branş ve Merkez Saha")

                    SportPickerField(
                        selectedSport = selectedSport,
                        onSelect = { if (it != null) selectedSport = it },
                        allowAll = false
                    )

                    MFloatingActionRow(
                        label = "Kulübün bölgesi",
                        valueText = "$selectedCity · $selectedArea",
                        icon = Icons.Rounded.LocationOn,
                        onClick = { showRegion = true }
                    )

                    VenueChoiceField(
                        label = "Ev sahibi tesis / saha",
                        value = homeVenue,
                        onChange = { homeVenue = it.take(160) },
                        previewOptions = emptyList(),
                        city = selectedCity
                    )
                }
            }

            // Kart 3: Kadro Kriterleri & Kulüp Vizyonu
            item {
                ClubSectionCard {
                    ClubSectionHeader(Icons.Rounded.Groups, "Kadro Kriterleri ve Düzen")

                    ClubFieldLabel("OYUNCU ALIMI")
                    MSegmentPillBar(
                        options = listOf(
                            MSegmentPillOption(true, "Açık Başvuru", emoji = "🟢"),
                            MSegmentPillOption(false, "Sadece Davetle", emoji = "🔒")
                        ),
                        selectedValue = recruitingOpen,
                        onSelect = { recruitingOpen = it }
                    )

                    ClubFieldLabel("MİNİMUM SEVİYE (MMR)")
                    MSegmentPillBar(
                        options = mmrOptions.map { MSegmentPillOption(it, "$it+ MMR") },
                        selectedValue = minMmr,
                        onSelect = { minMmr = it },
                        fontSizeSp = 11f
                    )

                    MFloatingInputBox(
                        label = "Oynama Zamanları",
                        value = availability,
                        onValueChange = { availability = it.take(200) },
                        placeholder = "Örn: Hafta içi akşam & Hafta sonu",
                        icon = Icons.Rounded.Schedule,
                        suggestions = listOf("Hafta içi & Hafta sonu", "Hafta içi 20:00 - 23:00", "Hafta sonu tüm gün")
                    )

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
            }

            // Takımı Kur Butonu
            item {
                CreationFormNotice(form)
                MButton(
                    text = if (form.busy) "Kulübün kaydediliyor…" else "Kulübü kur ve devam et",
                    onClick = {
                        if (teamName.isNotBlank()) {
                            val newTeam = TeamProfile(
                                id = "team_${teamName.hashCode().toUInt().toString(16)}",
                                name = teamName.trim(),
                                sport = selectedSport,
                                area = selectedArea,
                                badge = effectiveBadge,
                                color = teamColors[selectedColorIndex],
                                captainId = "me",
                                coCaptainId = null,
                                memberIds = listOf("me"),
                                substituteIds = emptyList(),
                                rating = 1000,
                                wins = 0,
                                losses = 0,
                                draws = 0,
                                verifiedMatches = 0,
                                fairPlay = 5.0f,
                                reliability = 100,
                                lookingForOpponent = true,
                                availability = availability.ifBlank { "Hafta içi & Hafta sonu" },
                                isMine = true,
                                description = bio.ifBlank { "Yeni kurulan aktif spor takımı." },
                                recruitingOpen = recruitingOpen,
                                requiredMmr = minMmr,
                                city = selectedCity,
                                homeVenue = homeVenue.trim()
                            )
                            form.submit("team.create", newTeam.toApiPayload(), { onCreateTeam(newTeam) }, onPublished, onDismiss)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.Shield,
                    enabled = !form.busy && teamName.trim().length >= 3
                )
            }
        }
    }
    }
    CreationFormDialogs(form, onDismiss)
    if (showRegion) CityAndLocationSelectorSheet(selectedCity, selectedArea, false, { showRegion = false }, { city, area, _ -> selectedCity = city; selectedArea = area })
}

@Composable
private fun ClubSectionCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
            content = content
        )
    }
}

@Composable
private fun ClubIdentityPreviewBanner(
    badgeText: String,
    crestColor: Color,
    teamName: String,
    sportEmoji: String,
    sportLabel: String,
    city: String,
    area: String,
    recruitingOpen: Boolean,
    minMmr: Int
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MColors.Ink900,
        border = BorderStroke(1.dp, Color(0xFF28374E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF1F2D42), MColors.Ink900)))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(crestColor)
                    .border(1.5.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeText,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    maxLines = 1
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = teamName.ifBlank { "Kulübünün adı" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (teamName.isNotBlank()) Color.White else Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$sportEmoji $sportLabel · $city / $area",
                    fontSize = 11.5.sp,
                    color = Color(0xFFCBD5E1),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFD48A).copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, Color(0xFFFFD48A).copy(alpha = 0.30f))
                ) {
                    Text(
                        text = if (recruitingOpen) "Katılıma Açık · $minMmr+ MMR" else "Davetli · $minMmr+ MMR",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD48A),
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ClubSectionHeader(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(9.dp),
            color = Color(0xFFFFF3EC),
            border = BorderStroke(1.dp, MColors.Brand.copy(alpha = 0.2f)),
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MColors.BrandDeep, modifier = Modifier.size(15.dp))
            }
        }
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MColors.Ink900
        )
    }
}

@Composable
private fun ClubFieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        color = MColors.Ink500
    )
}
