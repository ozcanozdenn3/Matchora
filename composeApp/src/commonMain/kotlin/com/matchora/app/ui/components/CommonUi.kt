package com.matchora.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateContentSize
import androidx.compose.runtime.getValue
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.state.Toast
import com.matchora.app.ui.theme.MColors
import com.matchora.app.ui.theme.MGradients

// ═══════════════════════════════════════════════════════════════════════════════
// PREMIUM LIGHT ELEVATED CARD
// Beyaz arka plan üzerinde kristal netliğinde, Apple/Nike tarzı minimal derinlik
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun MCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    elevation: Dp = 6.dp,
    borderColor: Color = Color(0xFFE4E9F2),
    backgroundColor: Color = Color.White,
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val interaction = remember { MutableInteractionSource() }
    var cardMod = modifier.pressMotion(interaction)
        .padding(vertical = 2.dp)
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = Color(0x280B1220),
            spotColor = Color(0x380B1220)
        )
        .clip(shape)
        .background(backgroundColor)
        .premiumSurface()
        .border(borderWidth, borderColor, shape)

    if (onClick != null) {
        cardMod = cardMod.clickable(
            interactionSource = interaction,
            indication = ripple(),
            onClick = onClick
        )
    }

    Column(
        modifier = cardMod,
        content = content
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// UNIFIED SCREEN HEADER CARD (Hiçbir Başlık Boşta Kalmasın)
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun MScreenHeaderCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 5.dp,
        border = BorderStroke(1.dp, Color(0xFFE4E9F2)),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .shadow(
                elevation = 7.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color(0x260B1220),
                spotColor = Color(0x360B1220)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MColors.Ink900,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    color = MColors.Ink500,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(10.dp))
                trailingContent()
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// UNIFIED MODULE CONTAINER CARD (Birleşik Modüler Kart)
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun MModuleCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    iconEmoji: String? = null,
    badgeText: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    elevation: Dp = 3.dp,
    borderColor: Color = MColors.Line,
    backgroundColor: Color = Color.White,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    MCard(modifier = modifier.fillMaxWidth(), cornerRadius = 24.dp,
        backgroundColor = backgroundColor, elevation = elevation, borderColor = borderColor) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (title != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (iconEmoji != null) {
                            Box(Modifier.size(38.dp).background(MColors.BrandTint, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text(text = iconEmoji, fontSize = 20.sp) }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = MColors.Ink900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (subtitle != null) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 12.sp,
                                    color = MColors.Ink500,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (badgeText != null) {
                        MBadge(
                            text = badgeText,
                            textColor = MColors.Brand,
                            backgroundColor = MColors.BrandTint
                        )
                    } else if (actionText != null && onActionClick != null) {
                        Text(
                            text = actionText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MColors.Brand,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable(onClick = onActionClick)
                        )
                    }
                }
                Box(Modifier.padding(horizontal = 18.dp).fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(MColors.Brand.copy(alpha = .25f), MColors.LineSoft, Color.Transparent))))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
                content = content
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// UNIFIED SUB-ITEM TILE (Kart İçi Düzenli Alt Zemin)
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun MSubCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    borderColor: Color = Color(0xFFE4E9F2),
    cornerRadius: Dp = 15.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    var mod = modifier
        .fillMaxWidth()
        .padding(vertical = 2.dp)
        .shadow(
            elevation = 6.dp,
            shape = shape,
            ambientColor = Color(0x280B1220),
            spotColor = Color(0x380B1220)
        )
        .clip(shape)
        .background(backgroundColor)
        .border(1.dp, borderColor, shape)

    if (onClick != null) {
        mod = mod.clickable(onClick = onClick)
    }

    Column(
        modifier = mod.padding(13.dp),
        content = content
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION HEADER
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun MSectionHeader(
    title: String,
    subtitle: String? = null,
    actionText: String? = null,
    actionIcon: ImageVector? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MColors.Ink900,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MColors.Ink500,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (actionText != null && onActionClick != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MColors.Brand,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                if (actionIcon != null) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = null,
                        tint = MColors.Brand,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// BADGE & TAGS
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun MBadge(
    text: String,
    textColor: Color,
    backgroundColor: Color,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// CHEMISTRY & COMPATIBILITY PILL (Guaranteed single line, never drops label below number)
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun MChemistryBadge(
    percent: Int,
    label: String = "Uyum",
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF0FDF4),
        border = BorderStroke(1.dp, Color(0x3515803D)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "%$percent",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF15803D),
                softWrap = false,
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF15803D),
                softWrap = false,
                maxLines = 1
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// FILTER CHIP (Executive Light Luxury Pill)
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun MChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emoji: String? = null
) {
    val chipColor by animateColorAsState(if (selected) MColors.BrandTint else Color(0xFFF8FAFC), label = "filter_color")
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = chipColor,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MColors.Brand else Color(0xFFE2E8F0)
        ),
        shadowElevation = if (selected) 2.dp else 0.dp,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.heightIn(min = 44.dp).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (emoji != null) {
                Text(text = emoji, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) MColors.BrandDeep else MColors.Ink700,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// BUTTONS
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun MButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = true,
    isDestructive: Boolean = false,
    enabled: Boolean = true,
    height: Dp = 50.dp
) {
    val interaction = remember { MutableInteractionSource() }
    val bg = when {
        !enabled -> MColors.Ink200
        isDestructive -> MColors.Rose
        isPrimary -> MColors.BrandDeep
        else -> MColors.Surface
    }
    val contentColor = when {
        !enabled -> MColors.Ink400
        isPrimary || isDestructive -> Color.White
        else -> MColors.Ink900
    }
    val borderCol = if (!isPrimary && !isDestructive && enabled) MColors.Line else Color.Transparent

    Box(
        modifier = modifier.pressMotion(interaction)
            .fillMaxWidth()
            .heightIn(min = height)
            .shadow(
                elevation = if (isPrimary && enabled) 2.dp else 0.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = if (isPrimary) MColors.Brand.copy(alpha = 0.25f) else Color.Transparent,
                spotColor = if (isPrimary) MColors.Brand.copy(alpha = 0.35f) else Color.Transparent
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(if (isPrimary && enabled && !isDestructive) Color(0xFFD84D19) else bg, bg)))
            .border(1.dp, borderCol, RoundedCornerShape(16.dp))
            .clickable(interactionSource = interaction, indication = ripple(), enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = contentColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = true,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// GLOBAL TOP APP BAR (Action Center, Token Pill, Menu, Notifications, Avatar)
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun MTopBar(
    currentCity: String,
    currentDistrict: String = "Kadıköy",
    isGpsActive: Boolean = false,
    tokenBalance: Int,
    unreadNotifications: Int,
    isMenuActive: Boolean = false,
    onOpenWallet: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenMenu: () -> Unit,
    onOpenProfile: () -> Unit,
    onCityClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Surface(shape = RoundedCornerShape(24.dp), color = MColors.Surface, shadowElevation = 7.dp,
            border = BorderStroke(1.dp, Brush.linearGradient(listOf(Color.White, Color(0xFFF1DCD0), Color.White)))) {
            Row(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color.White, Color(0xFFFFF7F1), Color.White)))
                .padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).shadow(3.dp, RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp))
                    .background(MGradients.Brand).clickable(onClick = onOpenProfile), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Explore, "Spor pasaportun", tint = Color.White, modifier = Modifier.size(25.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("MATCHORA", Modifier.clickable(onClick = onOpenProfile), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.3.sp, color = MColors.Ink900)
                    Row(Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onCityClick).padding(top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(currentDistrict, style = MaterialTheme.typography.bodySmall, color = MColors.Ink500, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Rounded.KeyboardArrowDown, null, tint = MColors.Ink500, modifier = Modifier.size(16.dp))
                    }
                }
                Surface(onClick = onOpenWallet, shape = RoundedCornerShape(14.dp), color = MColors.Ink900) {
                    Row(Modifier.heightIn(min = 40.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Toll, null, tint = Color(0xFFFFB68F), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("$tokenBalance", style = MaterialTheme.typography.titleSmall, color = Color.White)
                    }
                }
                IconButton(onClick = onOpenMenu, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.GridView,
                        contentDescription = "Menü",
                        tint = if (isMenuActive) MColors.BrandDeep else MColors.Ink700,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = onOpenNotifications, modifier = Modifier.size(40.dp)) {
                    BadgedBox(badge = { if (unreadNotifications > 0) Badge(containerColor = MColors.BrandDeep) }) {
                        Icon(Icons.Rounded.NotificationsNone, "Bildirimler", tint = MColors.Ink700, modifier = Modifier.size(23.dp))
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// KEŞFET MENÜSÜ BOTTOM SHEET (Yarış & Ağ)
// ═══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreMenuSheet(
    selectedTab: Int,
    onDismiss: () -> Unit,
    onSelectTab: (Int) -> Unit
) {
    ModalBottomSheet(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        scrimColor = Color(0xFF0B1220).copy(alpha = .48f),
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF6F8FB),
        dragHandle = { MPremiumSheetHandle() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MSheetHeading(
                title = "Matchora'da keşfet",
                subtitle = "Spor hayatının diğer alanları",
                icon = Icons.Rounded.GridView,
                onDismiss = onDismiss
            )

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, Color(0xFFE6EBF2)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        Triple(2, "Yarış", "Sıralamalar, turnuvalar ve takımlar"),
                        Triple(3, "Ağ", "Topluluklar, kulüpler ve tesisler")
                    ).forEach { (tab, title, description) ->
                        val isSel = selectedTab == tab
                        val icon = when (tab) {
                            2 -> Icons.Rounded.EmojiEvents
                            3 -> Icons.Rounded.Groups
                            else -> Icons.Rounded.Badge
                        }
                        val tags = when (tab) {
                            2 -> listOf("MMR Sıralaması", "Turnuvalar", "Kulüp Ligi")
                            else -> listOf("Spor Tesisleri", "Topluluklar", "Kort & Saha")
                        }
                        Surface(
                            onClick = {
                                onDismiss()
                                onSelectTab(tab)
                            },
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White,
                            shadowElevation = 6.dp,
                            border = BorderStroke(
                                width = if (isSel) 1.5.dp else 1.dp,
                                color = if (isSel) MColors.Brand.copy(alpha = 0.45f) else Color(0xFFE4E9F2)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(18.dp),
                                    ambientColor = Color(0x280B1220),
                                    spotColor = Color(0x380B1220)
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSel) MColors.BrandTint else Color(0xFFF8FAFC),
                                        shadowElevation = 3.dp,
                                        border = BorderStroke(1.dp, if (isSel) MColors.Brand.copy(alpha = 0.25f) else Color(0xFFE2E8F0)),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = MColors.BrandDeep,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = title,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MColors.Ink900
                                            )
                                            if (isSel) {
                                                Surface(
                                                    shape = RoundedCornerShape(7.dp),
                                                    color = MColors.BrandTint,
                                                    border = BorderStroke(1.dp, MColors.Brand.copy(alpha = 0.25f))
                                                ) {
                                                    Text(
                                                        text = "Açık",
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MColors.BrandDeep,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = description,
                                            fontSize = 12.sp,
                                            color = MColors.Ink500
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSel) MColors.BrandTint else Color(0xFFF8FAFC),
                                        shadowElevation = 2.dp,
                                        border = BorderStroke(1.dp, if (isSel) MColors.Brand.copy(alpha = 0.2f) else Color(0xFFE2E8F0)),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.ChevronRight,
                                                contentDescription = null,
                                                tint = if (isSel) MColors.BrandDeep else MColors.Ink500,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    tags.forEach { tag ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.White,
                                            shadowElevation = 2.dp,
                                            border = BorderStroke(1.dp, Color(0xFFE4E9F2))
                                        ) {
                                            Text(
                                                text = tag,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MColors.Ink700,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TOAST BANNER (Dynamic Island / Frosted Glass Floating Capsule)
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun MToastBanner(
    toast: Toast?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = toast != null,
        enter = slideInVertically(
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = 0.75f,
                stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
            )
        ) { -it * 2 } + fadeIn(),
        exit = slideOutVertically { -it * 2 } + fadeOut(),
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        if (toast != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xF20F172A), // Deep obsidian glass
                    border = BorderStroke(1.dp, Color(0x3538BDF8)),
                    shadowElevation = 14.dp,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .wrapContentWidth()
                        .widthIn(max = 380.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Sol İkon / Emoji Rozeti
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(Color(0x3338BDF8), Color(0x15FFFFFF)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = toast.emoji, fontSize = 17.sp)
                        }

                        // Metin Alanı
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "MATCHORA",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MColors.Mint
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(MColors.Mint)
                                )
                            }
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = toast.message,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Sağ Onay İndikatörü
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0x20FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MColors.Mint,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}


/** A single, consistent tab treatment without nested badges or competing accent colors. */
@Composable
fun MSegmentedControl(labels: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    if (labels.isEmpty()) return
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(17.dp), color = Color.White,
        border = BorderStroke(1.dp, MColors.Line), shadowElevation = 3.dp) {
        BoxWithConstraints(Modifier.padding(4.dp)) {
            val segmentWidth = maxWidth / labels.size
            val offset by animateDpAsState(segmentWidth * selectedIndex.coerceIn(0, labels.lastIndex),
                spring(dampingRatio = .85f, stiffness = 400f), label = "menu_capsule")
            Box(Modifier.matchParentSize()) {
                Box(Modifier.offset(x = offset).width(segmentWidth).fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp)).background(MGradients.Ink)
                    .border(1.dp, Color(0xFF40516A), RoundedCornerShape(12.dp)))
            }
            Row(Modifier.fillMaxWidth()) {
                labels.forEachIndexed { index, label ->
                    val color by animateColorAsState(if (selectedIndex == index) Color.White else MColors.Ink500, label = "menu_label")
                    Box(Modifier.weight(1f).heightIn(min = 40.dp).clip(RoundedCornerShape(12.dp))
                        .selectable(selected = selectedIndex == index, role = Role.Tab, onClick = { onSelect(index) }).padding(horizontal = 4.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                            color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
