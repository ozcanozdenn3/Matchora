package com.matchora.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.matchora.app.ui.components.LocalBackendRepository
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.domain.Sport
import com.matchora.app.state.*
import com.matchora.app.ui.components.*
import com.matchora.app.ui.theme.MColors
import com.matchora.app.ui.theme.MGradients

// ═══════════════════════════════════════════════════════════════════════════════
// EXECUTIVE DIGITAL ATHLETE PASSPORT (Minimalist, Net ve Derli Toplu)
// Aşırı metin kalabalığı temizlendi, net ve prestijli dijital sporcu kimliği.
// ═══════════════════════════════════════════════════════════════════════════════

internal data class PassportDisplayState(
    val effectiveUser: UserProfile,
    val history: List<MatchHistoryItem>,
    val badges: List<BadgeInfo>,
    val allMissions: List<Mission>,
    val reliabilityRecords: List<ReliabilityRecord>
)

private val passportDayOrder = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
private val passportWeekdayOrder = listOf("Pzt", "Sal", "Çar", "Per", "Cum")
private val passportDayFullNames = mapOf(
    "Pzt" to "Pazartesi",
    "Sal" to "Salı",
    "Çar" to "Çarşamba",
    "Per" to "Perşembe",
    "Cum" to "Cuma",
    "Cmt" to "Cumartesi",
    "Paz" to "Pazar"
)
private val dayPrefixedAvailabilityRegex = Regex("""^(Pzt|Sal|Çar|Per|Cum|Cmt|Paz)\s+(.+)$""")

private val PassportFootIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "PassportFoot",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        fun addToe(cx: Float, cy: Float, r: Float) {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(cx - r, cy)
                arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, cx + r, cy)
                arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, cx - r, cy)
                close()
            }
        }
        addToe(7.8f, 5.2f, 1.55f)
        addToe(11.0f, 4.4f, 1.25f)
        addToe(13.7f, 5.4f, 1.1f)
        addToe(15.8f, 7.1f, 0.95f)
        addToe(17.0f, 9.2f, 0.85f)
        path(
            fill = SolidColor(Color.Black),
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(10.4f, 8.2f)
            curveTo(13.4f, 8.2f, 15.6f, 10.2f, 15.2f, 12.8f)
            curveTo(14.9f, 14.5f, 13.8f, 15.6f, 13.5f, 17.2f)
            curveTo(13.2f, 19.6f, 11.9f, 21.5f, 10.0f, 21.5f)
            curveTo(8.0f, 21.5f, 7.0f, 19.8f, 7.4f, 17.6f)
            curveTo(7.7f, 15.8f, 8.7f, 15.0f, 8.0f, 13.4f)
            curveTo(7.2f, 11.6f, 7.6f, 8.2f, 10.4f, 8.2f)
            close()
        }
    }.build()
}

private val PassportHandIcon: ImageVector get() = Icons.Rounded.BackHand

internal fun passportTraitItems(user: UserProfile): List<Pair<ImageVector, String>> = buildList {
    user.dominantSide.split('·', '/', ',', ';').map { it.trim() }.filter { it.isNotBlank() }.forEach { part ->
        val icon = when {
            part.contains("ayak", ignoreCase = true) -> PassportFootIcon
            part.contains("el", ignoreCase = true) -> PassportHandIcon
            else -> Icons.Rounded.Sports
        }
        add(icon to part)
    }
    user.campus?.trim()?.takeIf { it.isNotBlank() }?.let { add(Icons.Rounded.School to it) }
    user.company?.trim()?.takeIf { it.isNotBlank() }?.let { add(Icons.Rounded.Business to it) }
    user.jerseyNumber?.let { add(Icons.Rounded.Tag to "#$it") }
}

internal fun formatPassportAvailability(availability: List<String>): String {
    val buckets = linkedMapOf<String, LinkedHashSet<String>>()
    val tokens = mutableListOf<Pair<Boolean, String>>()
    availability.map { it.trim() }.filter { it.isNotBlank() }.forEach { item ->
        val match = dayPrefixedAvailabilityRegex.matchEntire(item)
        if (match == null) {
            tokens += false to item
        } else {
            val day = match.groupValues[1]
            val window = match.groupValues[2].trim()
            val existingBucket = buckets[window]
            if (existingBucket == null) {
                buckets[window] = linkedSetOf(day)
                tokens += true to window
            } else {
                existingBucket.add(day)
            }
        }
    }
    val formatted = mutableListOf<String>()
    tokens.forEach { (isBucket, value) ->
        if (!isBucket) {
            formatted += value
        } else {
            val days = buckets.getValue(value).sortedBy { passportDayOrder.indexOf(it) }
            when {
                days.size == 7 -> formatted += "Her gün $value"
                passportWeekdayOrder.all { it in days } -> {
                    formatted += "Hafta içi $value"
                    val hasSat = "Cmt" in days
                    val hasSun = "Paz" in days
                    when {
                        hasSat && hasSun -> formatted += "Hafta sonu $value"
                        hasSat -> formatted += "Cumartesi $value"
                        hasSun -> formatted += "Pazar $value"
                    }
                }
                days.size == 2 && "Cmt" in days && "Paz" in days -> formatted += "Hafta sonu $value"
                days.size == 1 -> formatted += "${passportDayFullNames.getValue(days.single())} $value"
                else -> formatted += "${days.joinToString(", ")} $value"
            }
        }
    }
    return formatted.distinct().joinToString(" · ")
}

internal fun resolvePassportDisplayState(
    user: UserProfile,
    isLoggedIn: Boolean,
    liveMode: Boolean,
    historyOverride: List<MatchHistoryItem>?,
    reliabilityOverride: List<ReliabilityRecord>?,
    badgesOverride: List<BadgeInfo>?,
    missionsOverride: List<Mission>?
): PassportDisplayState {
    if (!isLoggedIn) {
        // Display-only sample: never reuse the identity, contact details or photo of a previous session.
        val sample = DemoData.me.copy(
            name = "Deniz Yılmaz",
            city = user.city.ifBlank { "İstanbul" },
            area = user.area.ifBlank { "Kadıköy" },
            bio = "Sol ayağım oyunda, aklım takımda. Akşamları saha, hafta sonu tenis.",
            dominantSide = "Sol ayak · Sağ el",
            phone = "", email = "", avatar = null
        )
        return PassportDisplayState(sample, DemoData.history, DemoData.badges, DemoData.missions, DemoData.reliabilityRecords)
    }
    val history = historyOverride ?: if (!liveMode && user.verifiedMatches > 0) DemoData.history else emptyList()
    val badges = badgesOverride ?: if (liveMode) emptyList() else DemoData.badges.map {
        if (user.verifiedMatches > 0) it else it.copy(earned = false, progress = 0f)
    }
    val allMissions = missionsOverride ?: if (liveMode) emptyList() else DemoData.missions.map {
        if (user.verifiedMatches > 0) it else it.copy(progress = 0)
    }
    val reliabilityRecords = reliabilityOverride ?: if (!liveMode && user.verifiedMatches > 0) DemoData.reliabilityRecords else emptyList()
    return PassportDisplayState(user, history, badges, allMissions, reliabilityRecords)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SportsPassportScreen(
    paddingValues: PaddingValues,
    user: UserProfile,
    activeProPlan: ProPlan? = null,
    historyOverride: List<MatchHistoryItem>? = null,
    reliabilityOverride: List<ReliabilityRecord>? = null,
    badgesOverride: List<BadgeInfo>? = null,
    missionsOverride: List<Mission>? = null,
    pendingMatchCount: Int = 0,
    isLoggedIn: Boolean = true,
    onOpenAuth: () -> Unit = {},
    onLogout: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenActivity: () -> Unit = {},
    onOpenWallet: () -> Unit,
    onVerifyContact: () -> Unit,
    onVerifyPhoto: () -> Unit,
    onUpdateProfile: (UserProfile) -> Unit
) {
    var showReliabilityDetail by remember { mutableStateOf(false) }
    var selectedBadgeForDetail by remember { mutableStateOf<BadgeInfo?>(null) }
    var showQrPassSheet by remember { mutableStateOf(false) }
    var showProfileEditor by remember { mutableStateOf(false) }
    var showAllMatches by remember { mutableStateOf(false) }

    val liveMode = LocalBackendRepository.current != null
    val passportDisplay = resolvePassportDisplayState(
        user = user,
        isLoggedIn = isLoggedIn,
        liveMode = liveMode,
        historyOverride = historyOverride,
        reliabilityOverride = reliabilityOverride,
        badgesOverride = badgesOverride,
        missionsOverride = missionsOverride
    )
    val effectiveUser = passportDisplay.effectiveUser
    val history = passportDisplay.history
    val badges = passportDisplay.badges
    val earnedBadges = badges.count { it.earned }
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
                        border = BorderStroke(if (activeProPlan != null) 1.5.dp else 1.dp, if (activeProPlan != null) MColors.Gold else Color(0xFF40505B)), shadowElevation = 3.dp) {
                    Column(Modifier.background(Brush.linearGradient(listOf(Color(0xFF293D46), MColors.Ink900))).padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("SPORCU PASAPORTU", Modifier.weight(1f), fontSize = 10.sp, letterSpacing = 1.3.sp,
                                fontWeight = FontWeight.Bold, color = Color(0xFFE1C49D))
                            if (!isLoggedIn) MBadge("ÖRNEK", Color(0xFFE1C49D), Color(0xFF3E423E))
                            else if (activeProPlan != null) MBadge("PRO", Color(0xFFE1C49D), Color(0xFF3E423E))
                            TextButton(
                                onClick = { if (!isLoggedIn) onOpenAuth() else onOpenWallet() },
                                modifier = Modifier.height(26.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Rounded.AccountBalanceWallet, null, Modifier.size(14.dp), tint = Color(0xFFE1C49D))
                                Spacer(Modifier.width(4.dp))
                                Text("Cüzdan", fontSize = 11.sp, color = Color(0xFFE1C49D))
                            }
                            if (isLoggedIn) {
                                TextButton(
                                    onClick = onLogout,
                                    modifier = Modifier.height(26.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.Logout, null, Modifier.size(13.dp), tint = Color(0xFFB8C7CE))
                                    Spacer(Modifier.width(3.dp))
                                    Text("Çıkış", fontSize = 10.sp, color = Color(0xFFB8C7CE))
                                }
                            } else {
                                TextButton(
                                    onClick = onOpenAuth,
                                    modifier = Modifier.height(26.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.Login, null, Modifier.size(13.dp), tint = Color(0xFFE1C49D))
                                    Spacer(Modifier.width(3.dp))
                                    Text("Giriş Yap", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE1C49D))
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(40.dp).background(Brush.linearGradient(listOf(Color(0xFFE1BE87), Color(0xFF96704D))), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                                if (effectiveUser.avatar != null) androidx.compose.foundation.Image(effectiveUser.avatar, "Profil fotoğrafı", Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                                else {
                                    val avatarUploadId = if (isLoggedIn) userAvatarUploadId(DemoData.ME) else null
                                    if (avatarUploadId != null) BackendPhoto(avatarUploadId, "Profil fotoğrafı", Modifier.fillMaxSize())
                                    else Text(effectiveUser.initials, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MColors.Ink900)
                                }
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(effectiveUser.name, fontSize = 16.5.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold, color = Color.White,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${effectiveUser.city} · ${effectiveUser.area}", fontSize = 11.sp, color = Color(0xFFB8C7CE), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Surface(
                                onClick = { if (!isLoggedIn) onOpenAuth() else showQrPassSheet = true },
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
                                onClick = { if (!isLoggedIn) onOpenAuth() else showProfileEditor = true },
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
                        if (effectiveUser.bio.isNotBlank()) Text(effectiveUser.bio, fontSize = 11.5.sp, lineHeight = 16.sp, color = Color(0xFFD0DADF), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            VerificationChip(
                                label = if (VerificationLevel.BASIC in effectiveUser.verifications) "İletişim onaylı" else "İletişimi doğrula",
                                verified = VerificationLevel.BASIC in effectiveUser.verifications,
                                onClick = if (!isLoggedIn) onOpenAuth else (if (VerificationLevel.BASIC in effectiveUser.verifications) null else onVerifyContact))
                            VerificationChip(
                                label = if (VerificationLevel.PHOTO in effectiveUser.verifications) "Fotoğraf onaylı" else "Fotoğrafı doğrula",
                                verified = VerificationLevel.PHOTO in effectiveUser.verifications,
                                onClick = if (!isLoggedIn) onOpenAuth else (if (VerificationLevel.PHOTO in effectiveUser.verifications) null else onVerifyPhoto))
                        }
                        val traits = passportTraitItems(effectiveUser)
                        if (traits.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                traits.forEach { (icon, label) -> GuestPassportTrait(icon, label) }
                            }
                        }
                        val availabilityLabel = formatPassportAvailability(effectiveUser.availability)
                        if (availabilityLabel.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Icon(Icons.Rounded.Schedule, null, Modifier.size(12.dp), tint = Color(0xFF9EAFBB))
                                Text(availabilityLabel, fontSize = 11.sp, lineHeight = 15.sp, color = Color(0xFFB8C7CE), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PassportMetric("GÜVEN", "%${effectiveUser.reliability}", Modifier.weight(1f).fillMaxHeight())
                            PassportMetric("ONAYLI MAÇ", "${effectiveUser.verifiedMatches}", Modifier.weight(1f).fillMaxHeight())
                            PassportMetric("FAIR PLAY", if (effectiveUser.fairPlay > 0) "${effectiveUser.fairPlay}/5" else "—", Modifier.weight(1f).fillMaxHeight())
                            PassportMetric("SERİ", "${effectiveUser.streakWeeks} hf", Modifier.weight(1f).fillMaxHeight())
                        }
                        if (liveMode && isLoggedIn) {
                            Text("Maç ve haftalık seri, katılımı doğrulanmış sonuçlarla güncellenir.", fontSize = 10.sp, lineHeight = 14.sp, color = Color(0xFFB8C7CE))
                            if (pendingMatchCount > 0) TextButton(onClick = onOpenActivity, contentPadding = PaddingValues(0.dp)) {
                                Text("$pendingMatchCount maçın sonuç kaydı veya onayı eksik", fontSize = 11.sp, color = Color(0xFFFFD28C))
                            }
                        }
                    }
                }
            }

            if (!isLoggedIn) {
                item {
                    PassportHighlightsCard(onOpenAuth = onOpenAuth)
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { if (!isLoggedIn) onOpenAuth() else onOpenActivity() }, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Rounded.CalendarMonth, null, Modifier.size(17.dp)); Spacer(Modifier.width(6.dp)); Text("Oyun takvimim", fontSize = 12.sp) }
                    OutlinedButton(onClick = { if (!isLoggedIn) onOpenAuth() else onOpenSettings() }, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Rounded.Tune, null, Modifier.size(17.dp)); Spacer(Modifier.width(6.dp)); Text("Hesap ayarları", fontSize = 12.sp, maxLines = 1) }
                }
            }

            item {
                PassportSectionCard(
                    title = "Rozetler",
                    detail = "$earnedBadges / ${badges.size} kazanıldı"
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 3.dp, vertical = 4.dp)
                    ) {
                        items(badges.size) { index ->
                            val badge = badges[index]
                            Surface(
                                onClick = { if (!isLoggedIn) onOpenAuth() else selectedBadgeForDetail = badge },
                                shape = RoundedCornerShape(15.dp),
                                color = Color.White,
                                shadowElevation = 1.dp,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (badge.earned) MColors.Brand.copy(alpha = 0.28f) else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.width(86.dp)
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

            item {
                PassportSectionCard(title = "Güvenilirlik geçmişi", detail = "%${effectiveUser.reliability}") {
                    PersonalHistoryButton("reliability", "Tüm güvenilirlik geçmişi")
                    Surface(
                        onClick = { if (!isLoggedIn) onOpenAuth() else showReliabilityDetail = !showReliabilityDetail },
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
                            passportDisplay.reliabilityRecords.forEach { record ->
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
                PassportSectionCard(title = "Son maçlar", detail = "${history.size} kayıt") {
                    PersonalHistoryButton("matches", "Tüm maç geçmişi")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (history.isEmpty()) Text("İlk doğrulanmış maçından sonra sportif geçmişin burada oluşur.", color = MColors.Ink500, style = MaterialTheme.typography.bodySmall)
                        history.take(if (showAllMatches) Int.MAX_VALUE else 3).forEach { match ->
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
                    if (history.size > 3) {
                        Spacer(Modifier.height(10.dp))
                        ExpandPassportRows(
                            expanded = showAllMatches,
                            label = if (showAllMatches) "Daha az göster" else "Diğer ${history.size - 3} maçı göster",
                            onClick = { showAllMatches = !showAllMatches }
                        )
                    }
                }
            }
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
                },
                onVerifyContact = { showProfileEditor = false; onVerifyContact() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkillProfileEditorSheet(
    skill: SportSkill,
    onDismiss: () -> Unit,
    onSave: (SportSkill) -> Unit,
    onRemove: (() -> Unit)? = null
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

            MFloatingDropdownBox(
                label = "Seviye Seçimi",
                selectedValue = level,
                options = SkillLevel.entries,
                optionLabel = { it.label },
                onSelect = { level = it },
                icon = Icons.Rounded.Leaderboard
            )

            MFloatingInputBox(
                label = "Pozisyon & Oyun Tarzı",
                value = position,
                onValueChange = { position = it },
                placeholder = "Örn: Oyun Kurucu, Sağ Kanat, Çiftler Sol...",
                subtitle = "Bu branşta sahadaki rolünü ve güçlü yönünü belirt.",
                icon = Icons.Rounded.EditNote,
                badgeText = "Branş Rolü"
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
            if (onRemove != null) {
                TextButton(
                    onClick = onRemove,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Branşı kaldır", color = MColors.Rose, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ProfileEditorSheet(
    user: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit,
    onVerifyContact: () -> Unit
) {
    val backend = com.matchora.app.ui.components.LocalBackendRepository.current
    val phoneLocked = backend?.enabled == true
    val backendWire = if (backend != null) backend.snapshots.collectAsState().value else null
    val avatarUploadId = backendWire?.get("profile")?.let { it as? kotlinx.serialization.json.JsonObject }?.get("avatarUploadId")?.let { it as? kotlinx.serialization.json.JsonPrimitive }?.content?.takeIf { it.isNotBlank() && it != "null" }
    val original = remember { user }
    var avatar by remember(original) { mutableStateOf(original.avatar) }
    var avatarRemovalRequested by remember(original) { mutableStateOf(original.avatarRemovalRequested) }
    var confirmAvatarRemoval by remember { mutableStateOf(false) }
    val pickAvatar = rememberGalleryPhotoPicker { avatar = it; avatarRemovalRequested = false }
    var jerseyNumber by remember(original) { mutableStateOf(original.jerseyNumber?.toString().orEmpty()) }
    var name by remember(original) { mutableStateOf(original.name) }
    var bio by remember(original) { mutableStateOf(original.bio) }
    var city by remember(original) { mutableStateOf(original.city) }
    var area by remember(original) { mutableStateOf(original.area) }
    var birthYear by remember(original) { mutableStateOf(original.birthYear.toString()) }
    var phone by remember(original) { mutableStateOf(original.phone) }
    var email by remember(original) { mutableStateOf(original.email) }
    var campus by remember(original) { mutableStateOf(original.campus.orEmpty()) }
    var company by remember(original) { mutableStateOf(original.company.orEmpty()) }
    var dominantSide by remember(original) { mutableStateOf(original.dominantSide) }
    var availability by remember(original) { mutableStateOf(original.availability.joinToString(", ")) }
    var sportsSkills by remember(original) {
        mutableStateOf(
            original.skills.ifEmpty {
                listOf(SportSkill(Sport.FOOTBALL, SkillLevel.INTERMEDIATE, 1200, 0, 0, null, null))
            }
        )
    }

    val parsedBirthYear = birthYear.toIntOrNull()
    val emailIsValid = email.contains('@') && email.substringAfter('@').contains('.')
    val phoneIsValid = phone.count { it.isDigit() } >= 7
    val isValid = name.trim().length in 2..40 &&
        city.trim().length in 2..60 &&
        area.trim().length in 2..60 &&
        bio.trim().length <= 280 &&
        (campus.isBlank() || campus.trim().length in 2..80) &&
        (company.isBlank() || company.trim().length in 2..80) &&
        sportsSkills.isNotEmpty() &&
        parsedBirthYear?.let { it in (currentYear() - 100)..(currentYear() - 13) } == true &&
        (email.isBlank() || emailIsValid) &&
        (phone.isBlank() || phoneIsValid)

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
                    Box(Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = .8f), CircleShape).border(1.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
                        if (avatar != null) androidx.compose.foundation.Image(requireNotNull(avatar), "Profil fotoğrafı", Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                        else if (!avatarRemovalRequested && avatarUploadId != null) BackendPhoto(avatarUploadId, "Profil fotoğrafı", Modifier.fillMaxSize())
                        else Text(name.trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase().ifBlank { "M" }, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = MColors.BrandDeep)
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
                        OutlinedButton(onClick = pickAvatar, modifier = Modifier.fillMaxWidth()) { Text(if (avatar == null && (avatarUploadId == null || avatarRemovalRequested)) "Profil fotoğrafı ekle" else "Profil fotoğrafını değiştir") }
                        if (!avatarRemovalRequested && (avatar != null || avatarUploadId != null)) TextButton(onClick = { confirmAvatarRemoval = true }) { Text("Fotoğrafı kaldır", color = MColors.Rose) }
                        if (avatarRemovalRequested) Text("Fotoğrafın değişiklikleri kaydettiğinde kaldırılacak.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
                        ProfileEditField("Ad soyad", name, { name = it }, isError = name.isBlank())
                        ProfileEditField("Forma numarası · isteğe bağlı", jerseyNumber, { jerseyNumber = it.filter(Char::isDigit).take(2) })
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
                        ProfileEditField("Doğum yılı", birthYear, { value -> birthYear = value.filter { it.isDigit() }.take(4) }, isError = parsedBirthYear?.let { it !in (currentYear() - 100)..(currentYear() - 13) } ?: true)
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
                        ProfileEditorSectionTitle("Branşların")
                        SportPickerField(
                            selectedSport = sportsSkills.firstOrNull()?.sport,
                            onSelect = { picked ->
                                if (picked != null && sportsSkills.none { it.sport == picked }) {
                                    sportsSkills = sportsSkills + SportSkill(picked, SkillLevel.INTERMEDIATE, 1200, 0, 0, null, null)
                                }
                            },
                            allowAll = false
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            sportsSkills.forEach { skill ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text("${skill.sport.emoji} ${skill.sport.label}", fontWeight = FontWeight.Bold, color = MColors.Ink900)
                                            if (sportsSkills.size > 1) {
                                                MPillActionButton(
                                                    text = "Kaldır",
                                                    tone = MPillTone.DANGER,
                                                    compact = true,
                                                    onClick = { sportsSkills = sportsSkills.filterNot { it.sport == skill.sport } }
                                                )
                                            }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                            SkillLevel.entries.forEach { lvl ->
                                                val sel = skill.level == lvl
                                                FilterChip(
                                                    selected = sel,
                                                    onClick = {
                                                        sportsSkills = sportsSkills.map { if (it.sport == skill.sport) it.copy(level = lvl) else it }
                                                    },
                                                    label = { Text(lvl.label, fontSize = 10.sp, maxLines = 1) },
                                                    colors = premiumChoiceChipColors()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Rounded.Sports, null, tint = MColors.BrandDeep, modifier = Modifier.size(30.dp))
                            Column {
                                ProfileEditorSectionTitle("Oyun tarzın")
                                Text("Doğru ekiple aynı sahada buluş.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
                            }
                        }
                        val sideParts = dominantSide.split('·', '/', ',', ';').map { it.trim() }.filter { it.isNotBlank() }
                        val selectedFoot = sideParts.firstOrNull { it.contains("ayak", ignoreCase = true) }.orEmpty()
                        val selectedHand = sideParts.firstOrNull { it.contains("el", ignoreCase = true) }.orEmpty()
                        fun updateSide(foot: String, hand: String) {
                            val extras = sideParts.filterNot { it.contains("ayak", ignoreCase = true) || it.contains("el", ignoreCase = true) }
                            dominantSide = listOfNotNull(foot.takeIf { it.isNotBlank() }, hand.takeIf { it.isNotBlank() }, *extras.toTypedArray()).joinToString(" · ")
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Sol ayak", "Sağ ayak").forEach { foot ->
                                FilterChip(
                                    selected = selectedFoot.equals(foot, ignoreCase = true),
                                    onClick = { updateSide(if (selectedFoot.equals(foot, ignoreCase = true)) "" else foot, selectedHand) },
                                    leadingIcon = { Icon(PassportFootIcon, null, Modifier.size(14.dp)) },
                                    label = { Text(foot) },
                                    colors = premiumChoiceChipColors()
                                )
                            }
                            listOf("Sağ el", "Sol el").forEach { hand ->
                                FilterChip(
                                    selected = selectedHand.equals(hand, ignoreCase = true),
                                    onClick = { updateSide(selectedFoot, if (selectedHand.equals(hand, ignoreCase = true)) "" else hand) },
                                    leadingIcon = { Icon(PassportHandIcon, null, Modifier.size(14.dp)) },
                                    label = { Text(hand) },
                                    colors = premiumChoiceChipColors()
                                )
                            }
                        }
                        ProfileEditField("Baskın taraf / pozisyon", dominantSide, { dominantSide = it })
                        Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFEDF4F0)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Rounded.Schedule, null, tint = MColors.Ink600, modifier = Modifier.size(19.dp))
                                    Text("Oyun takvimin", fontWeight = FontWeight.Bold, color = MColors.Ink900)
                                }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val existing = availability.split(',').map { it.trim() }.filter { it.isNotBlank() }
                                    listOf("Hafta içi 19:00–23:00", "Cumartesi 09:00–13:00", "Hafta sonu 10:00–14:00").forEach { preset ->
                                        val active = existing.any { it.equals(preset, ignoreCase = true) }
                                        FilterChip(
                                            selected = active,
                                            onClick = {
                                                val next = if (active) existing.filterNot { it.equals(preset, ignoreCase = true) } else existing + preset
                                                availability = next.joinToString(", ")
                                            },
                                            label = { Text(preset) },
                                            colors = premiumChoiceChipColors()
                                        )
                                    }
                                }
                                ProfileEditField("Müsaitlik saatleri", availability, { availability = it }, singleLine = false, minLines = 3, maxLines = 5)
                                Text("Örnek: Hafta içi 19:00–23:00, Cumartesi 09:00–13:00", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
                            }
                        }
                    }
                    else -> {
                        ProfileEditorSectionTitle("İletişim bilgilerin")
                        ProfileEditField("Telefon", phone, { phone = it }, isError = phone.isNotBlank() && !phoneIsValid, readOnly = phoneLocked)
                        if (phoneLocked) {
                            Text("Telefon numaran doğrulama akışıyla hesabına bağlanır.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
                            TextButton(onClick = onVerifyContact) { Text(if (phone.isBlank()) "Telefonumu doğrula" else "Telefon numaramı değiştir") }
                        }
                        ProfileEditField("E-posta", email, { email = it }, isError = email.isNotBlank() && !emailIsValid)
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
                        original.copy(
                            avatar = avatar,
                            avatarRemovalRequested = avatarRemovalRequested,
                            jerseyNumber = jerseyNumber.toIntOrNull(),
                            name = name.trim(),
                            city = city.trim(),
                            area = area.trim(),
                            bio = bio.trim(),
                            birthYear = parsedBirthYear ?: original.birthYear,
                            phone = phone.trim(),
                            email = email.trim(),
                            verifications = (if (phone.trim() != original.phone || email.trim() != original.email) {
                                original.verifications - VerificationLevel.BASIC
                            } else original.verifications).let { if (avatarRemovalRequested) it - VerificationLevel.PHOTO else it },
                            campus = campus.trim().ifBlank { null },
                            company = company.trim().ifBlank { null },
                            skills = sportsSkills,
                            dominantSide = dominantSide.trim(),
                            availability = availability.split(',').map { it.trim() }.filter { it.isNotBlank() }
                        )
                    )
                    }, modifier = Modifier.fillMaxWidth(), icon = Icons.Rounded.Check, enabled = isValid)
                }
            }
        }
    }
    if (confirmAvatarRemoval) AlertDialog(onDismissRequest = { confirmAvatarRemoval = false }, title = { Text("Profil fotoğrafı kaldırılsın mı?") },
        text = { Text("Fotoğrafın ve varsa fotoğraf doğrulama rozetin değişiklikleri kaydettiğinde kaldırılır.") },
        confirmButton = { TextButton(onClick = { avatar = null; avatarRemovalRequested = true; confirmAvatarRemoval = false }) { Text("Fotoğrafı kaldır") } },
        dismissButton = { TextButton(onClick = { confirmAvatarRemoval = false }) { Text("Vazgeç") } })
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
    isError: Boolean = false,
    readOnly: Boolean = false
) {
    OutlinedTextField(
                    textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium,

        value = value,
        onValueChange = onValueChange,
        readOnly = readOnly,
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

// ═══════════════════════════════════════════════════════════════════════════════
// MİSAFİR MODU CAN ALICI AYRICALIKLAR BÖLÜMÜ
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun GuestPassportTrait(icon: ImageVector, label: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = .06f),
        border = BorderStroke(.6.dp, Color.White.copy(alpha = .09f))) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(icon, null, Modifier.size(12.dp), tint = Color(0xFFE1C49D))
            Text(label, fontSize = 10.5.sp, color = Color(0xFFD0DADF))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PassportHighlightsCard(onOpenAuth: () -> Unit) {
    val gold = Color(0xFFE5C89E)
    Surface(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        color = Color(0xFF101C29), shadowElevation = 5.dp,
        border = BorderStroke(1.dp, Brush.linearGradient(listOf(gold.copy(alpha = .6f), Color(0xFF344858), gold.copy(alpha = .18f))))
    ) {
        Box(Modifier.background(Brush.linearGradient(listOf(Color(0xFF233746), Color(0xFF101C29), Color(0xFF14212C))))) {
            Canvas(Modifier.matchParentSize()) {
                val center = Offset(size.width * 1.02f, 18.dp.toPx())
                listOf(62.dp, 94.dp, 126.dp).forEach { radius ->
                    drawCircle(gold.copy(alpha = .055f), radius.toPx(), center, style = Stroke(1.dp.toPx()))
                }
            }
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(28.dp).background(gold.copy(alpha = .12f), RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(15.dp), tint = gold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("MATCHORA AYRICALIKLARI", Modifier.weight(1f), fontSize = 10.sp, letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold, color = gold)
                }
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Oyununu bul.\nİzini bırak.", fontSize = 29.sp, lineHeight = 32.sp, letterSpacing = (-.7).sp,
                        fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Maçtan takıma, gelişiminden rekabete.\nSpor hayatın tek bir yerde.", fontSize = 12.sp,
                        lineHeight = 18.sp, color = Color(0xFFB5C5CF))
                }
                Surface(shape = RoundedCornerShape(17.dp), color = Color.White.copy(alpha = .055f),
                    border = BorderStroke(.8.dp, gold.copy(alpha = .19f))) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                            Box(Modifier.size(40.dp).background(Brush.linearGradient(listOf(Color(0xFFE9D0AB), Color(0xFFB38D61))), RoundedCornerShape(13.dp)),
                                contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Radar, null, Modifier.size(22.dp), tint = Color(0xFF172531))
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Doğru oyuncu. Doğru maç.", fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Sana uyan oyunu keşfet, eksik kadronu tamamla.", fontSize = 11.5.sp, lineHeight = 16.sp, color = Color(0xFFC3CFD7))
                            }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Seviyene göre", "Mevkine göre", "Yakınında").forEach { label ->
                                Surface(shape = RoundedCornerShape(7.dp), color = Color(0xFF152532), border = BorderStroke(.6.dp, Color.White.copy(alpha = .08f))) {
                                    Text(label, Modifier.padding(horizontal = 8.dp, vertical = 5.dp), fontSize = 10.sp, color = gold, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    PassportPrivilegeRow(Icons.Rounded.Insights, "Performansın pasaportunda.",
                        "Branş MMR’ı, sıralamalar, doğrulanmış maçlar ve fair play geçmişin.")
                    HorizontalDivider(color = Color.White.copy(alpha = .08f))
                    PassportPrivilegeRow(Icons.Rounded.EmojiEvents, "Takımını kur. Rekabeti büyüt.",
                        "Meydan okumalar, turnuvalar ve fikstürlerle oyunu birlikte sürdür.")
                }
                Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.Explore, null, Modifier.size(13.dp), tint = gold.copy(alpha = .8f))
                        Text("Sahada · kampüste · şirkette · seyahatte", fontSize = 10.5.sp, lineHeight = 15.sp, color = Color(0xFF9EB2C0))
                    }
                    MButton("Sporcu pasaportunu oluştur", onOpenAuth, Modifier.fillMaxWidth(), height = 46.dp,
                        icon = Icons.AutoMirrored.Rounded.ArrowForward)
                }
            }
        }
    }
}

@Composable
private fun PassportPrivilegeRow(icon: ImageVector, title: String, description: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(11.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(31.dp).background(Color(0xFFE5C89E).copy(alpha = .09f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(17.dp), tint = Color(0xFFE5C89E))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFF4F6F8))
            Text(description, fontSize = 11.5.sp, lineHeight = 17.sp, color = Color(0xFFACBDC9))
        }
    }
}

