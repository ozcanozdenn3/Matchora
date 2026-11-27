package com.matchora.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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
    cornerRadius: Dp = 18.dp,
    elevation: Dp = 2.dp,
    borderColor: Color = Color(0xFFE2E8F0),
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
            ambientColor = Color(0x140B1220),
            spotColor = Color(0x1E0B1220)
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
    borderColor: Color = Color(0xFFE2E8F0),
    cornerRadius: Dp = 14.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    var mod = modifier
        .fillMaxWidth()
        .padding(vertical = 2.dp)
        .shadow(
            elevation = 1.dp,
            shape = shape,
            ambientColor = Color(0x100B1220),
            spotColor = Color(0x140B1220)
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
    val chipColor by animateColorAsState(
        targetValue = if (selected) MColors.Ink900 else Color(0xFFF8FAFC),
        label = "filter_color"
    )
    Surface(
        shape = RoundedCornerShape(11.dp),
        color = chipColor,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MColors.Ink900 else Color(0xFFE2E8F0)
        ),
        shadowElevation = if (selected) 1.dp else 0.dp,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.heightIn(min = 36.dp).padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (emoji != null) {
                Text(text = emoji, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) Color.White else MColors.Ink700,
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
    height: Dp = 46.dp
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
    val backgroundBrush = if (isPrimary && enabled && !isDestructive) {
        MGradients.Brand
    } else {
        Brush.linearGradient(listOf(bg, bg))
    }

    Box(
        modifier = modifier.pressMotion(interaction)
            .fillMaxWidth()
            .heightIn(min = height)
            .shadow(
                elevation = if (isPrimary && enabled) 2.dp else 0.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = if (isPrimary) MColors.Brand.copy(alpha = 0.25f) else Color.Transparent,
                spotColor = if (isPrimary) MColors.Brand.copy(alpha = 0.35f) else Color.Transparent
            )
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundBrush)
            .border(1.dp, borderCol, RoundedCornerShape(14.dp))
            .clickable(interactionSource = interaction, indication = ripple(), enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
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
    isLoggedIn: Boolean = true,
    onOpenAuth: () -> Unit = {},
    onOpenWallet: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenMenu: () -> Unit,
    onOpenProfile: () -> Unit,
    onCityClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MColors.Surface,
            shadowElevation = 7.dp,
            border = BorderStroke(1.dp, Brush.linearGradient(listOf(Color.White, Color(0xFFF1DCD0), Color.White)))
        ) {
            Row(
                Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color.White, Color(0xFFFFF7F1), Color.White)))
                    .padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sol Logo / Profil Avatar
                Box(
                    Modifier.size(42.dp)
                        .shadow(3.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(MGradients.Brand)
                        .clickable(onClick = if (isLoggedIn) onOpenProfile else onOpenAuth),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Explore, "Matchora pusulası", tint = Color.White, modifier = Modifier.size(25.dp))
                }
                Spacer(Modifier.width(10.dp))

                // Marka & İlçe
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "MATCHORA",
                        modifier = Modifier.clickable(onClick = if (isLoggedIn) onOpenProfile else onOpenAuth),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                        color = MColors.Ink900
                    )
                    Row(
                        Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onCityClick).padding(top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(currentDistrict, style = MaterialTheme.typography.bodySmall, color = MColors.Ink500, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Rounded.KeyboardArrowDown, null, tint = MColors.Ink500, modifier = Modifier.size(16.dp))
                    }
                }

                // Sağ Taraf: Giriş Yapıldıysa Premium Token Kapsülü; Misafir ise Eşleşen Giriş Kapsülü
                if (isLoggedIn) {
                    Surface(
                        onClick = onOpenWallet,
                        shape = RoundedCornerShape(14.dp),
                        color = MColors.Ink900,
                        shadowElevation = 2.dp,
                        border = BorderStroke(1.dp, Color(0xFFFFB68F).copy(alpha = 0.42f))
                    ) {
                        Row(
                            modifier = Modifier
                                .background(Brush.linearGradient(listOf(Color(0xFF1F2D42), MColors.Ink900)))
                                .height(38.dp)
                                .padding(start = 7.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(0xFFFFB68F).copy(alpha = 0.16f), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFFFFB68F).copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Toll,
                                    contentDescription = null,
                                    tint = Color(0xFFFFC599),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "$tokenBalance",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "TKN",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.4.sp,
                                    color = Color(0xFFFFB68F),
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(bottom = 1.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(Color(0xFF283A54), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = Color(0xFFFFC599),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        onClick = onOpenAuth,
                        shape = RoundedCornerShape(14.dp),
                        color = MColors.Ink900,
                        shadowElevation = 2.dp,
                        border = BorderStroke(1.dp, Color(0xFFFFB68F).copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .background(Brush.linearGradient(listOf(Color(0xFF1F2D42), MColors.Ink900)))
                                .height(38.dp)
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.PersonOutline, contentDescription = null, tint = Color(0xFFFFC599), modifier = Modifier.size(16.dp))
                            Text("Giriş", style = MaterialTheme.typography.titleSmall, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                val menuInteraction = remember { MutableInteractionSource() }
                val isMenuPressed by menuInteraction.collectIsPressedAsState()
                val menuHighlighted = isMenuActive || isMenuPressed
                val menuTint by animateColorAsState(
                    targetValue = if (menuHighlighted) MColors.Brand else MColors.Ink700,
                    animationSpec = tween(180),
                    label = "menuIconTint"
                )
                val menuScale by animateFloatAsState(
                    targetValue = if (menuHighlighted) 1.12f else 1f,
                    animationSpec = spring(dampingRatio = 0.65f, stiffness = 520f),
                    label = "menuIconScale"
                )
                val menuIndicatorWidth by animateDpAsState(
                    targetValue = if (menuHighlighted) 12.dp else 0.dp,
                    animationSpec = tween(180),
                    label = "menuIndicatorWidth"
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .pressMotion(menuInteraction)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = menuInteraction,
                            indication = null,
                            onClick = onOpenMenu
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GridView,
                            contentDescription = "Menü",
                            tint = menuTint,
                            modifier = Modifier
                                .size(22.dp)
                                .graphicsLayer {
                                    scaleX = menuScale
                                    scaleY = menuScale
                                    alpha = 0.99f
                                }
                                .drawWithCache {
                                    onDrawWithContent {
                                        drawContent()
                                        if (menuHighlighted) {
                                            drawRect(MGradients.Brand, blendMode = BlendMode.SrcAtop)
                                        }
                                    }
                                }
                        )
                        Spacer(Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .width(menuIndicatorWidth)
                                .height(2.5.dp)
                                .clip(CircleShape)
                                .background(MGradients.Brand)
                        )
                    }
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
        containerColor = Color(0xFFF6F7FA),
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

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExploreMenuCard(
                    tab = 2,
                    selected = selectedTab == 2,
                    badge = "REKABET & LİG",
                    title = "Yarış",
                    description = "Sıralamalar, turnuvalar ve takımlar",
                    icon = Icons.Rounded.EmojiEvents,
                    tags = listOf("MMR Sıralaması", "Turnuvalar", "Kulüp Ligi"),
                    onClick = {
                        onDismiss()
                        onSelectTab(2)
                    }
                )
                ExploreMenuCard(
                    tab = 3,
                    selected = selectedTab == 3,
                    badge = "KULÜP & TESİS",
                    title = "Ağ",
                    description = "Topluluklar, kulüpler ve tesisler",
                    icon = Icons.Rounded.Groups,
                    tags = listOf("Spor Tesisleri", "Topluluklar", "Kort & Saha"),
                    onClick = {
                        onDismiss()
                        onSelectTab(3)
                    }
                )
            }
        }
    }
}

@Composable
private fun ExploreMenuCard(
    tab: Int,
    selected: Boolean,
    badge: String,
    title: String,
    description: String,
    icon: ImageVector,
    tags: List<String>,
    onClick: () -> Unit
) {
    val goldWarm = Color(0xFFD5B488)
    val goldSoft = Color(0xFFE5DDD0)
    val goldRim = Color(0xFFC8A273)
    val bronze = Color(0xFF96704D)
    val bronzeDark = Color(0xFF7A5838)

    val borderBrush = Brush.linearGradient(
        if (selected) {
            listOf(goldWarm, bronze.copy(alpha = 0.72f), Color(0xFFF0D7B0), goldRim)
        } else {
            listOf(goldWarm.copy(alpha = 0.85f), bronze.copy(alpha = 0.42f), goldSoft, goldRim.copy(alpha = 0.75f))
        }
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        shadowElevation = if (selected) 9.dp else 7.dp,
        border = BorderStroke(if (selected) 1.5.dp else 1.1.dp, borderBrush),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color.White, Color(0xFFFDFAF5))))
                .drawWithCache {
                    val center = Offset(size.width * 0.96f, 18.dp.toPx())
                    onDrawBehind {
                        listOf(42.dp, 64.dp, 86.dp).forEach { radius ->
                            drawCircle(
                                color = goldRim.copy(alpha = if (tab == 2) 0.09f else 0.075f),
                                radius = radius.toPx(),
                                center = center,
                                style = Stroke(1.dp.toPx())
                            )
                        }
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .shadow(3.dp, RoundedCornerShape(15.dp))
                            .clip(RoundedCornerShape(15.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFC89D66), bronze, Color(0xFF6A4B2F))
                                )
                            )
                            .border(1.1.dp, Color(0xFFF3DEC0), RoundedCornerShape(15.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(7.dp),
                                color = Color(0xFFF7F0E4),
                                border = BorderStroke(0.9.dp, Color(0xFFDDC5A2))
                            ) {
                                Text(
                                    text = badge,
                                    fontSize = 9.5.sp,
                                    letterSpacing = 0.6.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = bronzeDark,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                                )
                            }
                            if (selected) {
                                Surface(
                                    shape = RoundedCornerShape(7.dp),
                                    color = bronzeDark
                                ) {
                                    Text(
                                        text = "AÇIK",
                                        fontSize = 9.sp,
                                        letterSpacing = 0.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = title,
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MColors.Ink900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = description,
                            fontSize = 12.5.sp,
                            lineHeight = 17.sp,
                            color = Color(0xFF475569),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(11.dp),
                        color = Color(0xFFF8F2E8),
                        border = BorderStroke(1.dp, Color(0xFFD8C09A)),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = bronzeDark,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.8.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(goldWarm.copy(alpha = 0.48f), Color(0xFFEAE2D6), Color.Transparent)
                            )
                        )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFAF6F0),
                            border = BorderStroke(0.9.dp, Color(0xFFE4D5BF))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(4.5.dp)
                                        .clip(CircleShape)
                                        .background(bronze)
                                )
                                Text(
                                    text = tag,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF334155),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

val LocalToastNotifier = staticCompositionLocalOf<((String, String) -> Unit)?> { null }

// ═══════════════════════════════════════════════════════════════════════════════
// TOAST BANNER (Dynamic Island / Solid High-Contrast Floating Capsule)
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun MToastBanner(
    toast: Toast?,
    busyMessage: String? = null,
    modifier: Modifier = Modifier
) {
    val activeToast = toast
    val activeBusy = if (activeToast == null) busyMessage else null
    AnimatedVisibility(
        visible = activeToast != null || activeBusy != null,
        enter = slideInVertically(
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = 0.75f,
                stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
            )
        ) { -it * 2 } + fadeIn(),
        exit = slideOutVertically { -it * 2 } + fadeOut(),
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        if (activeToast != null || activeBusy != null) {
            val isAlert = activeToast?.emoji in setOf("⚠️", "✕")
            val messageText = activeToast?.message ?: activeBusy.orEmpty()
            val accentColor = when {
                activeBusy != null -> MColors.BrandLight
                isAlert -> MColors.Amber
                else -> MColors.Mint
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(
                        1.dp,
                        if (isAlert) MColors.Amber.copy(alpha = 0.55f) else Color(0xFF1E3A5F)
                    ),
                    shadowElevation = 16.dp,
                    tonalElevation = 0.dp,
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
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (activeBusy != null) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MColors.BrandLight
                                )
                            } else {
                                Text(text = activeToast?.emoji ?: "✓", fontSize = 17.sp)
                            }
                        }

                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (activeBusy != null) "İŞLEM SÜRÜYOR" else "MATCHORA",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = accentColor
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(accentColor)
                                )
                            }
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = messageText,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (activeToast != null) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x20FFFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isAlert) Icons.Rounded.PriorityHigh else Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
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
