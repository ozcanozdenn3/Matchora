package com.matchora.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.ui.components.SportPickerSheet
import com.matchora.app.domain.Sport
import com.matchora.app.state.*
import com.matchora.app.ui.components.*
import com.matchora.app.ui.theme.MColors

// ═══════════════════════════════════════════════════════════════════════════════
// EXECUTIVE DIGITAL ATHLETE PASSPORT (Minimalist, Net ve Derli Toplu)
// Aşırı metin kalabalığı temizlendi, net ve prestijli dijital sporcu kimliği.
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun SportsPassportScreen(
    paddingValues: PaddingValues,
    user: UserProfile,
    activeProPlan: ProPlan? = null,
    onOpenWallet: () -> Unit,
    onVerifyContact: () -> Unit,
    onVerifyPhoto: () -> Unit,
    onUpdateProfile: (UserProfile) -> Unit
) {
    var showReliabilityDetail by remember { mutableStateOf(false) }
    var selectedSkillForDetail by remember { mutableStateOf<SportSkill?>(null) }
    var selectedSkillForEdit by remember { mutableStateOf<SportSkill?>(null) }
    var selectedBadgeForDetail by remember { mutableStateOf<BadgeInfo?>(null) }
    var showQrPassSheet by remember { mutableStateOf(false) }
    var showAddSport by remember { mutableStateOf(false) }
    var showProfileEditor by remember { mutableStateOf(false) }
    var showAllSkills by remember { mutableStateOf(false) }
    var showAllMatches by remember { mutableStateOf(false) }

    val weeklyMission = DemoData.missions.firstOrNull()
    val earnedBadges = DemoData.badges.count { it.earned }
    val listState = rememberLazyListState()
    val scrollToTopTrigger = LocalScrollToTopTrigger.current
    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            listState.animateScrollToItem(0)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(
                top = 10.dp,
                bottom = paddingValues.calculateBottomPadding() + 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(21.dp), color = MColors.Ink900,
                    border = BorderStroke(1.dp, Color(0xFF40505B)), shadowElevation = 3.dp) {
                    Column(Modifier.background(Brush.linearGradient(listOf(Color(0xFF293D46), MColors.Ink900))).padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("SPORCU PASAPORTU", Modifier.weight(1f), fontSize = 10.sp, letterSpacing = 1.3.sp,
                                fontWeight = FontWeight.Bold, color = Color(0xFFE1C49D))
                            if (activeProPlan != null) MBadge("PRO", Color(0xFFE1C49D), Color(0xFF3E423E))
                            TextButton(
                                onClick = onOpenWallet,
                                modifier = Modifier.height(26.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Rounded.AccountBalanceWallet, null, Modifier.size(14.dp), tint = Color(0xFFE1C49D))
                                Spacer(Modifier.width(4.dp))
                                Text("Cüzdan", fontSize = 11.sp, color = Color(0xFFE1C49D))
                            }
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(40.dp).background(Brush.linearGradient(listOf(Color(0xFFE1BE87), Color(0xFF96704D))), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                                Text(user.initials, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MColors.Ink900)
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(user.name, fontSize = 16.5.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold, color = Color.White,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${user.city} · ${user.area}", fontSize = 11.sp, color = Color(0xFFB8C7CE), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Surface(
                                onClick = { showQrPassSheet = true },
                                shape = RoundedCornerShape(11.dp),
                                color = Color(0xFF2B3B45),
                                shadowElevation = 4.dp,
                                border = BorderStroke(0.8.dp, Color.White.copy(alpha = .16f)),
                                modifier = Modifier
                                    .size(34.dp)
                                    .shadow(6.dp, RoundedCornerShape(11.dp), ambientColor = Color(0x55000000), spotColor = Color(0x70000000))
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.QrCode2, "Dijital kart", Modifier.size(17.dp), tint = Color(0xFFE1C49D))
                                }
                            }
                            Surface(
                                onClick = { showProfileEditor = true },
                                shape = RoundedCornerShape(11.dp),
                                color = Color(0xFF2B3B45),
                                shadowElevation = 4.dp,
                                border = BorderStroke(0.8.dp, Color.White.copy(alpha = .16f)),
                                modifier = Modifier
                                    .size(34.dp)
                                    .shadow(6.dp, RoundedCornerShape(11.dp), ambientColor = Color(0x55000000), spotColor = Color(0x70000000))
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Edit, "Profili düzenle", Modifier.size(16.dp), tint = Color(0xFFE1C49D))
                                }
                            }
                        }
                        if (user.bio.isNotBlank()) Text(user.bio, fontSize = 11.5.sp, lineHeight = 15.sp, color = Color(0xFFD0DADF), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            VerificationChip(
                                label = if (VerificationLevel.BASIC in user.verifications) "İletişim onaylı" else "İletişimi doğrula",
                                verified = VerificationLevel.BASIC in user.verifications,
                                onClick = if (VerificationLevel.BASIC in user.verifications) null else onVerifyContact)
                            VerificationChip(
                                label = if (VerificationLevel.PHOTO in user.verifications) "Fotoğraf onaylı" else "Fotoğrafı doğrula",
                                verified = VerificationLevel.PHOTO in user.verifications,
                                onClick = if (VerificationLevel.PHOTO in user.verifications) null else onVerifyPhoto)
                        }
                        val profileContext = listOfNotNull(user.dominantSide.takeIf { it.isNotBlank() }, user.campus, user.company).joinToString(" · ")
                        if (profileContext.isNotBlank()) Text(profileContext, fontSize = 11.sp, color = Color(0xFF9EAFBB), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (user.availability.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Icon(Icons.Rounded.Schedule, null, Modifier.size(12.dp), tint = Color(0xFF9EAFBB))
                                Text(user.availability.joinToString(" · "), fontSize = 11.sp, lineHeight = 14.sp, color = Color(0xFFB8C7CE), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PassportMetric("GÜVEN", "%${user.reliability}", Modifier.weight(1f).fillMaxHeight())
                            PassportMetric("MAÇ", "${user.verifiedMatches}", Modifier.weight(1f).fillMaxHeight())
                            PassportMetric("FAIR PLAY", "${user.fairPlay}/5", Modifier.weight(1f).fillMaxHeight())
                            PassportMetric("SERİ", "${user.streakWeeks} hf", Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                }
            }

            item {
                PassportSectionCard(
                    title = "Branş dereceleri",
                    detail = "${user.skills.size} branş · MMR"
                ) {
                    MButton("Branş ekle", { showAddSport = true }, icon = Icons.Rounded.Add, isPrimary = false, height = 40.dp)
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        user.skills.take(if (showAllSkills) Int.MAX_VALUE else 3).forEach { skill ->
                            Surface(
                                onClick = { selectedSkillForDetail = skill },
                                shape = RoundedCornerShape(15.dp),
                                color = Color.White,
                                shadowElevation = 6.dp,
                                border = BorderStroke(1.dp, Color(0xFFE4E9F2)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 3.dp, vertical = 2.dp)
                                    .shadow(
                                        elevation = 9.dp,
                                        shape = RoundedCornerShape(15.dp),
                                        ambientColor = Color(0x320B1220),
                                        spotColor = Color(0x460B1220)
                                    )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFFAFBFD))))
                                        .padding(vertical = 9.dp, horizontal = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .shadow(3.dp, RoundedCornerShape(11.dp), ambientColor = Color(0x220B1220), spotColor = Color(0x300B1220))
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(Color(0xFFFFF7ED))
                                            .border(0.8.dp, Color(0xFFFDE6C8), RoundedCornerShape(11.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(skill.sport.emoji, fontSize = 18.sp)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            skill.sport.label,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MColors.Ink900,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "${skill.tier.label} · ${skill.verifiedMatches} maç",
                                            fontSize = 11.5.sp,
                                            color = MColors.Ink500,
                                            maxLines = 1
                                        )
                                    }
                                    Column(
                                        modifier = Modifier.padding(start = 6.dp),
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text("${skill.rating} MMR", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = MColors.Brand, maxLines = 1)
                                        Text(
                                            skill.localRank?.let { "#$it · ${user.area}" } ?: "Yerel sıra —",
                                            fontSize = 11.sp,
                                            color = MColors.Ink500,
                                            maxLines = 1
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Surface(
                                        onClick = { selectedSkillForEdit = skill },
                                        shape = RoundedCornerShape(9.dp),
                                        color = Color.White,
                                        shadowElevation = 3.dp,
                                        border = BorderStroke(0.8.dp, Color(0xFFE4E9F2)),
                                        modifier = Modifier
                                            .size(32.dp)
                                            .shadow(4.dp, RoundedCornerShape(9.dp), ambientColor = Color(0x240B1220), spotColor = Color(0x340B1220))
                                    ) {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Rounded.Edit,
                                                contentDescription = "${skill.sport.label} profilini düzenle",
                                                tint = MColors.Brand,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (user.skills.size > 3) {
                        Spacer(Modifier.height(10.dp))
                        ExpandPassportRows(
                            expanded = showAllSkills,
                            label = if (showAllSkills) "Daha az göster" else "Diğer ${user.skills.size - 3} branşı göster",
                            onClick = { showAllSkills = !showAllSkills }
                        )
                    }
                }
            }

            item {
                PassportSectionCard(
                    title = "Rozetler",
                    detail = "$earnedBadges / ${DemoData.badges.size} kazanıldı"
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 3.dp, vertical = 4.dp)
                    ) {
                        items(DemoData.badges.size) { index ->
                            val badge = DemoData.badges[index]
                            Surface(
                                onClick = { selectedBadgeForDetail = badge },
                                shape = RoundedCornerShape(15.dp),
                                color = Color.White,
                                shadowElevation = 6.dp,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (badge.earned) MColors.Brand.copy(alpha = 0.28f) else Color(0xFFE4E9F2)
                                ),
                                modifier = Modifier
                                    .width(86.dp)
                                    .shadow(
                                        elevation = 8.dp,
                                        shape = RoundedCornerShape(15.dp),
                                        ambientColor = Color(0x300B1220),
                                        spotColor = Color(0x420B1220)
                                    )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFFAFBFD))))
                                        .padding(vertical = 10.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .shadow(3.dp, CircleShape, ambientColor = Color(0x220B1220), spotColor = Color(0x300B1220))
                                            .clip(CircleShape)
                                            .background(if (badge.earned) Color(0xFFFFF7ED) else Color(0xFFF8FAFC))
                                            .border(
                                                1.dp,
                                                if (badge.earned) MColors.Brand.copy(alpha = 0.45f) else MColors.Line,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(badge.emoji, fontSize = 18.sp, color = if (badge.earned) Color.Unspecified else MColors.Ink400)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        badge.title,
                                        fontSize = 11.5.sp,
                                        lineHeight = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MColors.Ink800,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        minLines = 2
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = if (badge.earned) "Kazanıldı" else "%${(badge.progress.coerceIn(0f, 1f) * 100).toInt()}",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (badge.earned) MColors.Mint else MColors.Ink500
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (weeklyMission != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFFF6F0E5),
                        border = BorderStroke(1.dp, MColors.Brand.copy(alpha = 0.18f)),
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Surface(
                                        shape = RoundedCornerShape(11.dp),
                                        color = Color.White,
                                        shadowElevation = 4.dp,
                                        border = BorderStroke(0.8.dp, Color(0xFFEDE2D3)),
                                        modifier = Modifier
                                            .size(38.dp)
                                            .shadow(5.dp, RoundedCornerShape(11.dp), ambientColor = Color(0x260B1220), spotColor = Color(0x360B1220))
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(weeklyMission.emoji, fontSize = 18.sp)
                                        }
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("Haftalık görev", fontSize = 11.5.sp, color = MColors.Ink500, fontWeight = FontWeight.SemiBold)
                                        Text(weeklyMission.title, fontSize = 14.sp, color = MColors.Ink900, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Surface(
                                    color = Color.White,
                                    shape = RoundedCornerShape(10.dp),
                                    shadowElevation = 4.dp,
                                    border = BorderStroke(0.8.dp, Color(0xFFEDE2D3)),
                                    modifier = Modifier.shadow(6.dp, RoundedCornerShape(10.dp), ambientColor = Color(0x280B1220), spotColor = Color(0x380B1220))
                                ) {
                                    Text(
                                        "+${weeklyMission.rewardPoints} puan",
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                        fontSize = 12.sp,
                                        color = MColors.Brand,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(13.dp),
                                color = Color.White,
                                shadowElevation = 4.dp,
                                border = BorderStroke(0.8.dp, Color(0xFFEDE2D3)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(6.dp, RoundedCornerShape(13.dp), ambientColor = Color(0x240B1220), spotColor = Color(0x340B1220))
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    LinearProgressIndicator(
                                        progress = {
                                            if (weeklyMission.target <= 0) 0f
                                            else (weeklyMission.progress.toFloat() / weeklyMission.target).coerceIn(0f, 1f)
                                        },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                        color = MColors.Brand,
                                        trackColor = Color(0xFFEDE7E2)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(weeklyMission.description, modifier = Modifier.weight(1f).padding(end = 10.dp), fontSize = 11.5.sp, color = MColors.Ink600)
                                        Text(
                                            "${weeklyMission.progress.coerceAtMost(weeklyMission.target)}/${weeklyMission.target}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MColors.Ink800
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                PassportSectionCard(title = "Güvenilirlik geçmişi", detail = "%${user.reliability}") {
                    Surface(
                        onClick = { showReliabilityDetail = !showReliabilityDetail },
                        shape = RoundedCornerShape(13.dp),
                        color = Color.White,
                        shadowElevation = 5.dp,
                        border = BorderStroke(1.dp, Color(0xFFE4E9F2)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 3.dp, vertical = 2.dp)
                            .shadow(
                                elevation = 7.dp,
                                shape = RoundedCornerShape(13.dp),
                                ambientColor = Color(0x2E0B1220),
                                spotColor = Color(0x3E0B1220)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Son hareketler", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = MColors.Ink800)
                            Icon(
                                if (showReliabilityDetail) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                null,
                                tint = MColors.Ink500,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    AnimatedVisibility(visible = showReliabilityDetail) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 9.dp)) {
                            DemoData.reliabilityRecords.forEach { record ->
                                Surface(
                                    shape = RoundedCornerShape(13.dp),
                                    color = Color.White,
                                    shadowElevation = 5.dp,
                                    border = BorderStroke(1.dp, Color(0xFFE4E9F2)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 3.dp, vertical = 1.dp)
                                        .shadow(
                                            elevation = 7.dp,
                                            shape = RoundedCornerShape(13.dp),
                                            ambientColor = Color(0x2C0B1220),
                                            spotColor = Color(0x3C0B1220)
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 9.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(record.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MColors.Ink900)
                                            Text("${record.time} · ${record.note}", fontSize = 11.5.sp, color = MColors.Ink500)
                                        }
                                        val deltaLabel = when {
                                            record.delta > 0 -> "+${record.delta}"
                                            else -> record.delta.toString()
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = when {
                                                record.delta > 0 -> Color(0xFFEAF8F1)
                                                record.delta < 0 -> Color(0xFFFFF0F0)
                                                else -> MColors.SurfaceMuted
                                            }
                                        ) {
                                            Text(
                                                "$deltaLabel puan",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when {
                                                    record.delta > 0 -> MColors.Mint
                                                    record.delta < 0 -> MColors.Rose
                                                    else -> MColors.Ink500
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                PassportSectionCard(title = "Son maçlar", detail = "${DemoData.history.size} kayıt") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DemoData.history.take(if (showAllMatches) Int.MAX_VALUE else 3).forEach { match ->
                            Surface(
                                shape = RoundedCornerShape(15.dp),
                                color = Color.White,
                                shadowElevation = 6.dp,
                                border = BorderStroke(1.dp, Color(0xFFE4E9F2)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 3.dp, vertical = 2.dp)
                                    .shadow(
                                        elevation = 9.dp,
                                        shape = RoundedCornerShape(15.dp),
                                        ambientColor = Color(0x320B1220),
                                        spotColor = Color(0x460B1220)
                                    )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFFAFBFD))))
                                        .padding(horizontal = 11.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .shadow(3.dp, RoundedCornerShape(10.dp), ambientColor = Color(0x200B1220), spotColor = Color(0x2E0B1220))
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFF8FAFC))
                                            .border(0.8.dp, Color(0xFFE4E9F2), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(match.sport.emoji, fontSize = 17.sp)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            match.title,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MColors.Ink900,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text("${match.date} · ${match.result}", fontSize = 11.5.sp, color = MColors.Ink500, maxLines = 1)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (match.ratingDelta > 0) Color(0xFFEAF8F1) else if (match.ratingDelta < 0) Color(0xFFFFF0F0) else MColors.SurfaceMuted,
                                        border = BorderStroke(
                                            0.7.dp,
                                            if (match.ratingDelta > 0) MColors.Mint.copy(alpha = 0.25f) else if (match.ratingDelta < 0) MColors.Rose.copy(alpha = 0.25f) else MColors.Line
                                        )
                                    ) {
                                        Text(
                                            if (match.ratingDelta > 0) "+${match.ratingDelta}" else match.ratingDelta.toString(),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (match.ratingDelta > 0) MColors.Mint else if (match.ratingDelta < 0) MColors.Rose else MColors.Ink500
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (DemoData.history.size > 3) {
                        Spacer(Modifier.height(10.dp))
                        ExpandPassportRows(
                            expanded = showAllMatches,
                            label = if (showAllMatches) "Daha az göster" else "Diğer ${DemoData.history.size - 3} maçı göster",
                            onClick = { showAllMatches = !showAllMatches }
                        )
                    }
                }
            }
        }

        if (showAddSport) {
            SportPickerSheet(
                selectedSport = null,
                sports = Sport.entries.filter { sport -> user.skills.none { it.sport == sport } },
                allowAll = false,
                onDismiss = { showAddSport = false },
                onSelect = { sport ->
                    if (sport != null) {
                        selectedSkillForEdit = SportSkill(sport, SkillLevel.BEGINNER, 1000, 0, 0, null, null)
                        showAddSport = false
                    }
                }
            )
        }
        selectedSkillForDetail?.let { skill ->
            SkillBreakdownDialog(skill = skill, area = user.area, onDismiss = { selectedSkillForDetail = null })
        }
        selectedSkillForEdit?.let { skill ->
            SkillProfileEditorSheet(
                skill = skill,
                onDismiss = { selectedSkillForEdit = null },
                onSave = { updatedSkill ->
                    val updated = if (user.skills.any { it.sport == updatedSkill.sport }) {
                        user.skills.map { current -> if (current.sport == updatedSkill.sport) updatedSkill else current }
                    } else user.skills + updatedSkill
                    onUpdateProfile(user.copy(skills = updated))
                    selectedSkillForEdit = null
                }
            )
        }
        selectedBadgeForDetail?.let { badge ->
            BadgeDetailDialog(
                emoji = badge.emoji,
                title = badge.title,
                desc = badge.description,
                earned = badge.earned,
                progress = badge.progress,
                onDismiss = { selectedBadgeForDetail = null }
            )
        }
        if (showQrPassSheet) {
            AthleteQrPassSheet(user = user, onDismiss = { showQrPassSheet = false })
        }
        if (showProfileEditor) {
            ProfileEditorSheet(
                user = user,
                onDismiss = { showProfileEditor = false },
                onSave = { updated ->
                    onUpdateProfile(updated)
                    showProfileEditor = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkillProfileEditorSheet(
    skill: SportSkill,
    onDismiss: () -> Unit,
    onSave: (SportSkill) -> Unit
) {
    var level by remember(skill) { mutableStateOf(skill.level) }
    var position by remember(skill) { mutableStateOf(skill.position.orEmpty()) }

    ModalBottomSheet(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        scrimColor = androidx.compose.ui.graphics.Color(0xFF0B1220).copy(alpha = .48f),

        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF5F6F8),
        dragHandle = { MPremiumSheetHandle() }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MSheetHeading(
                title = "${skill.sport.emoji} ${skill.sport.label}",
                subtitle = "Oyun tarzını ve seviyeni güncelle.",
                icon = Icons.Rounded.Sports,
                onDismiss = onDismiss
            )

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                shadowElevation = 5.dp,
                border = BorderStroke(1.dp, Color(0xFFE4E9F2)),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(7.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x280B1220), spotColor = Color(0x380B1220))
            ) {
                Column(Modifier.padding(14.dp)) {
                    MFloatingDropdownBox(
                        label = "Seviye Seçimi",
                        selectedValue = level,
                        options = SkillLevel.entries,
                        optionLabel = { it.label },
                        onSelect = { level = it },
                        icon = Icons.Rounded.Leaderboard
                    )
                }
            }

            MFloatingInputBox(
                label = "Pozisyon & Oyun Tarzı",
                value = position,
                onValueChange = { position = it },
                placeholder = "Örn: Oyun Kurucu, Sağ Kanat, Çiftler Sol...",
                subtitle = "Bu branşta sahadaki rolünü ve güçlü yönünü belirt.",
                icon = Icons.Rounded.EditNote,
                badgeText = "Branş Rolü",
                suggestions = listOf("Hücum Odaklı", "Oyun Kurucu", "Savunma Lideri", "Çok Yönlü")
            )
            Surface(
                shape = RoundedCornerShape(11.dp),
                color = MColors.SurfaceMuted,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mevcut MMR", modifier = Modifier.weight(1f), fontSize = 11.sp, color = MColors.Ink600)
                    Text(
                        "${skill.rating} · ${skill.verifiedMatches} doğrulanmış maç",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MColors.Ink800,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text("Derecen, doğrulanmış maç performansına göre güncellenir.", fontSize = 11.sp, color = MColors.Ink500)
            Button(
                onClick = { onSave(skill.copy(level = level, position = position.trim().ifBlank { null })) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MColors.Brand)
            ) {
                Icon(Icons.Rounded.Check, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text("Branş profilini kaydet", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileEditorSheet(
    user: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit
) {
    var name by remember(user) { mutableStateOf(user.name) }
    var bio by remember(user) { mutableStateOf(user.bio) }
    var city by remember(user) { mutableStateOf(user.city) }
    var area by remember(user) { mutableStateOf(user.area) }
    var birthYear by remember(user) { mutableStateOf(user.birthYear.toString()) }
    var phone by remember(user) { mutableStateOf(user.phone) }
    var email by remember(user) { mutableStateOf(user.email) }
    var campus by remember(user) { mutableStateOf(user.campus.orEmpty()) }
    var company by remember(user) { mutableStateOf(user.company.orEmpty()) }
    var dominantSide by remember(user) { mutableStateOf(user.dominantSide) }
    var availability by remember(user) { mutableStateOf(user.availability.joinToString(", ")) }

    val parsedBirthYear = birthYear.toIntOrNull()
    val emailIsValid = email.contains('@') && email.substringAfter('@').contains('.')
    val phoneIsValid = phone.count { it.isDigit() } >= 7
    val isValid = name.isNotBlank() && city.isNotBlank() && area.isNotBlank() && parsedBirthYear?.let { it in 1900..2026 } == true && emailIsValid && phoneIsValid

    var section by remember { mutableIntStateOf(0) }
    var showLocationPicker by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    LaunchedEffect(section) { scrollState.scrollTo(0) }

    ModalBottomSheet(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        scrimColor = androidx.compose.ui.graphics.Color(0xFF0B1220).copy(alpha = .48f),

        onDismissRequest = onDismiss,
        modifier = Modifier.statusBarsPadding().padding(top = 12.dp),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFFFAF9F6),
        dragHandle = { MPremiumSheetHandle() }
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.92f).imePadding()) {
            Box(Modifier.padding(horizontal = 20.dp)) {
                MSheetHeading(
                    title = "Profili düzenle",
                    subtitle = "Sporcu pasaportu kimlik ve oyun tercihlerin",
                    icon = Icons.Rounded.Badge,
                    onDismiss = onDismiss
                )
            }
            Column(Modifier.weight(1f).verticalScroll(scrollState).padding(horizontal = 22.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Spacer(Modifier.height(2.dp))
                Row(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFFFFEBD9), Color(0xFFF5EFE6))), RoundedCornerShape(24.dp)).padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.size(52.dp).background(Color.White.copy(alpha = .8f), CircleShape).border(1.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
                        Text(name.trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase().ifBlank { "M" }, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = MColors.BrandDeep)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(name.ifBlank { "Sporcu profilin" }, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = MColors.Ink900, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("$city · $area", style = MaterialTheme.typography.bodySmall, color = MColors.Ink600)
                        Text("Profil önizlemesi", style = MaterialTheme.typography.labelSmall, color = MColors.BrandDeep)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    listOf("Kimlik", "Spor", "İletişim").forEachIndexed { index, title ->
                        Column(Modifier.weight(1f).clickable { section = index }.padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(title, fontWeight = if (section == index) FontWeight.Bold else FontWeight.Medium, color = if (section == index) MColors.Ink900 else MColors.Ink400)
                            Box(Modifier.fillMaxWidth().height(3.dp).background(if (section == index) MColors.Brand else MColors.Line, CircleShape))
                        }
                    }
                }
                when (section) {
                    0 -> {
                        ProfileEditorSectionTitle("Sahada seni tanısınlar")
                        ProfileEditField("Ad soyad", name, { name = it }, isError = name.isBlank())
                        Surface(onClick = { showLocationPicker = true }, shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, MColors.Line)) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Rounded.LocationOn, null, tint = MColors.BrandDeep)
                                Column(Modifier.weight(1f)) {
                                    Text("Şehir ve bölge", style = MaterialTheme.typography.labelSmall, color = MColors.Ink500)
                                    Text("$city · $area", color = MColors.Ink900, fontWeight = FontWeight.SemiBold)
                                }
                                Icon(Icons.Rounded.ExpandMore, "Konumu değiştir", tint = MColors.Ink500)
                            }
                        }
                        ProfileEditField("Doğum yılı", birthYear, { value -> birthYear = value.filter { it.isDigit() }.take(4) }, isError = parsedBirthYear?.let { it !in 1900..2026 } ?: true)
                        MFloatingInputBox(
                            label = "Kendinden Bahset & Kısa Açıklama",
                            value = bio,
                            onValueChange = { bio = it },
                            placeholder = "Örn: Hafta içi akşamları düzenli futbol ve tenis oynuyorum...",
                            subtitle = "Sporcu pasaportunda görünecek kısa biyografin.",
                            icon = Icons.Rounded.EditNote,
                            badgeText = "Pasaport Notu",
                            singleLine = false,
                            minLines = 3,
                            maxLines = 5,
                            suggestions = listOf(
                                "Tempolu ve centilmence maç arıyorum",
                                "Hafta içi akşamları düzenli oynarım",
                                "Turnuva ve lig maçlarına açığım"
                            )
                        )
                    }
                    1 -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Rounded.Sports, null, tint = MColors.BrandDeep, modifier = Modifier.size(30.dp))
                            Column {
                                ProfileEditorSectionTitle("Oyun tarzın")
                                Text("Doğru ekiple aynı sahada buluş.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
                            }
                        }
                        ProfileEditField("Baskın taraf / pozisyon", dominantSide, { dominantSide = it })
                        Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFEDF4F0)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Rounded.Schedule, null, tint = MColors.Ink600, modifier = Modifier.size(19.dp))
                                    Text("Oyun takvimin", fontWeight = FontWeight.Bold, color = MColors.Ink900)
                                }
                                ProfileEditField("Müsaitlik saatleri", availability, { availability = it }, singleLine = false, minLines = 3, maxLines = 5)
                                Text("Örnek: Hafta içi 19.00 sonrası, Pazar sabahı", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
                            }
                        }
                    }
                    else -> {
                        ProfileEditorSectionTitle("İletişim bilgilerin")
                        ProfileEditField("Telefon", phone, { phone = it }, isError = !phoneIsValid)
                        ProfileEditField("E-posta", email, { email = it }, isError = !emailIsValid)
                        HorizontalDivider(color = MColors.Line)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Bağlantıların", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = MColors.Ink900)
                            Text("İsteğe bağlı", style = MaterialTheme.typography.labelSmall, color = MColors.Ink400)
                        }
                        ProfileEditField("Kampüs", campus, { campus = it })
                        ProfileEditField("Şirket", company, { company = it })
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Surface(color = Color.White, shadowElevation = 8.dp) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isValid) Text("Kimlik ve iletişim alanlarındaki bilgileri kontrol et.", style = MaterialTheme.typography.bodySmall, color = MColors.Rose)
                    MButton("Değişiklikleri kaydet", onClick = {
                    onSave(
                        user.copy(
                            name = name.trim(),
                            city = city.trim(),
                            area = area.trim(),
                            bio = bio.trim(),
                            birthYear = parsedBirthYear ?: user.birthYear,
                            phone = phone.trim(),
                            email = email.trim(),
                            verifications = if (phone.trim() != user.phone || email.trim() != user.email) {
                                user.verifications - VerificationLevel.BASIC
                            } else user.verifications,
                            campus = campus.trim().ifBlank { null },
                            company = company.trim().ifBlank { null },
                            dominantSide = dominantSide.trim(),
                            availability = availability.split(',').map { it.trim() }.filter { it.isNotBlank() }
                        )
                    )
                    }, modifier = Modifier.fillMaxWidth(), icon = Icons.Rounded.Check, enabled = isValid)
                }
            }
        }
    }
    if (showLocationPicker) {
        CityAndLocationSelectorSheet(city, area, false, onDismiss = { showLocationPicker = false }, onSelectLocation = { selectedCity, district, _ ->
            city = selectedCity
            area = district
            showLocationPicker = false
        })
    }
}

@Composable
private fun ProfileEditorSectionTitle(text: String) {
    Text(
        text,
        modifier = Modifier.padding(top = 7.dp),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.9.sp,
        color = MColors.Ink500
    )
}

@Composable
private fun ProfileEditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
    isError: Boolean = false
) {
    OutlinedTextField(
                    textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium,

        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        isError = isError,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White,
            unfocusedBorderColor = MColors.Line,
            focusedBorderColor = MColors.Brand,
            focusedLabelColor = MColors.Brand,
            cursorColor = MColors.Brand
        )
    )
}

@Composable
private fun PassportMetric(label: String, value: String, modifier: Modifier = Modifier) {
    val accent = when (label) { "GÜVEN" -> Color(0xFF8FE3CC); "FAIR PLAY" -> Color(0xFFFFD28C); "SERİ" -> Color(0xFFFFAC88); else -> Color(0xFFB8ACFF) }
    Column(
        modifier
            .padding(vertical = 1.dp)
            .heightIn(min = 58.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = Color(0x66000000),
                spotColor = Color(0x88000000)
            )
            .background(Brush.linearGradient(listOf(Color(0xFF394854), Color(0xFF23303C))), RoundedCornerShape(12.dp))
            .border(0.8.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(when (label) { "GÜVEN" -> Icons.Rounded.VerifiedUser; "FAIR PLAY" -> Icons.Rounded.EmojiEvents; "SERİ" -> Icons.Rounded.LocalFireDepartment; else -> Icons.Rounded.Sports }, null, tint = accent, modifier = Modifier.size(15.dp))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1)
        Text(label, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = accent, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun VerificationChip(label: String, verified: Boolean, onClick: (() -> Unit)?) {
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = if (verified) Color(0xFF29433B) else Color(0xFF364350),
        border = BorderStroke(1.dp, if (verified) Color(0xFF4B7366) else Color(0xFF5A6873)),
        shadowElevation = 5.dp,
        modifier = Modifier
            .padding(vertical = 1.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(9.dp),
                ambientColor = Color(0x55000000),
                spotColor = Color(0x77000000)
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (verified) Icons.Rounded.Verified else Icons.Rounded.AddCircleOutline,
                null,
                tint = if (verified) Color(0xFF9EDDC8) else Color(0xFFD1D9DF),
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (verified) Color(0xFF9EDDC8) else Color(0xFFD1D9DF))
        }
    }
}

@Composable
private fun ExpandPassportRows(expanded: Boolean, label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp)
            .shadow(
                elevation = 7.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = Color(0x2E0B1220),
                spotColor = Color(0x3E0B1220)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 5.dp,
        border = BorderStroke(1.dp, Color(0xFFE4E9F2))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MColors.Ink800)
            Spacer(Modifier.width(6.dp))
            Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = MColors.Brand, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun PassportSectionCard(
    title: String,
    detail: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MColors.Line),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, modifier = Modifier.weight(1f).padding(end = 8.dp), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MColors.Ink900)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    shadowElevation = 3.dp,
                    border = BorderStroke(0.8.dp, Color(0xFFE4E9F2)),
                    modifier = Modifier.shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(8.dp),
                        ambientColor = Color(0x240B1220),
                        spotColor = Color(0x340B1220)
                    )
                ) {
                    Text(detail, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MColors.Ink600)
                }
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}
