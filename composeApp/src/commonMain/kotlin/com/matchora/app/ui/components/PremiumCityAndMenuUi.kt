package com.matchora.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.state.canonicalTurkishKey
import com.matchora.app.state.matchesTurkishPlace
import com.matchora.app.state.turkeyCities
import com.matchora.app.ui.theme.MColors

/**
 * Kompakt ekran başlığı ve sağ kenar birincil aksiyon alanı; dikey kalabalığı azaltır.
 */
@Composable
fun MCompactHeroStrip(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accent: Color = Color(0xFFFFB68F),
    trailingAction: (@Composable () -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MColors.Ink900,
        border = BorderStroke(1.dp, Color(0xFF283A52)),
        shadowElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF18263A), Color(0xFF0F1826))
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.18f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFD0DCEF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            trailingAction?.invoke()
        }
    }
}

/**
 * Koyu başlık şeridi içinde yüksek kontrastlı, okunaklı ve dikkat çekici premium aksiyon butonu.
 */
@Composable
fun MHeroActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isUtility: Boolean = false
) {
    val gradient = when {
        !enabled -> Brush.horizontalGradient(listOf(Color(0xFF253549), Color(0xFF1E2C3E)))
        isUtility -> Brush.horizontalGradient(listOf(Color(0xFF243750), Color(0xFF1B2B40)))
        else -> Brush.horizontalGradient(listOf(Color(0xFFFF6B2B), Color(0xFFE04812)))
    }
    val borderColor = when {
        !enabled -> Color(0xFF3A4E68)
        isUtility -> Color(0xFF4B6587)
        else -> Color(0xFFFFA076)
    }
    val badgeBg = when {
        isUtility -> Color(0xFFFFB68F).copy(alpha = 0.20f)
        else -> Color.White.copy(alpha = 0.22f)
    }
    val iconTint = when {
        !enabled -> Color(0xFFCBD5E1)
        isUtility -> Color(0xFFFFC29E)
        else -> Color.White
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(13.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = if (enabled && !isUtility) 5.dp else 2.dp,
        modifier = modifier.height(40.dp)
    ) {
        Row(
            modifier = Modifier
                .background(gradient)
                .padding(start = 9.dp, end = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.1.sp,
                color = Color.White,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Derli toplu 3 seçenekli il filtresi:
 * - Konuma göre (currentCity)
 * - İl seç (listeden seçilen özel il)
 * - Tüm iller
 */
@Composable
fun MCityFilterBar(
    currentCity: String,
    selectedCity: String?,
    useAllCities: Boolean,
    onSelectCurrentCity: () -> Unit,
    onSelectAllCities: () -> Unit,
    onOpenCityPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLocationSelected = !useAllCities && selectedCity == null
    val isPickedSelected = !useAllCities && selectedCity != null
    val isAllSelected = useAllCities

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CityFilterSegmentPill(
                label = currentCity.ifBlank { "Konum" },
                icon = Icons.Rounded.NearMe,
                selected = isLocationSelected,
                onClick = onSelectCurrentCity,
                modifier = Modifier.weight(1.05f)
            )
            CityFilterSegmentPill(
                label = selectedCity ?: "İl seç",
                icon = Icons.Rounded.LocationCity,
                trailingIcon = Icons.Rounded.KeyboardArrowDown,
                selected = isPickedSelected,
                onClick = onOpenCityPicker,
                modifier = Modifier.weight(1.1f)
            )
            CityFilterSegmentPill(
                label = "Tüm iller",
                icon = Icons.Rounded.Public,
                selected = isAllSelected,
                onClick = onSelectAllCities,
                modifier = Modifier.weight(0.95f)
            )
        }
    }
}

@Composable
private fun CityFilterSegmentPill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = null
) {
    UnifiedSegmentPill(
        label = label,
        icon = icon,
        emoji = null,
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        trailingIcon = trailingIcon,
        horizontalPadding = 8.dp,
        iconSize = 14.dp,
        fontSizeSp = 11.5f
    )
}

/**
 * Tekil bir sekment veya filtre hapı seçeneği; `MSegmentPillBar` içinde tek tip görünüm sağlar.
 */
data class MSegmentPillOption<T>(
    val value: T,
    val label: String,
    val icon: ImageVector? = null,
    val emoji: String? = null,
    val weight: Float = 1f
)

/**
 * Ekranlar ve alt sayfalarda dağınık duran sekmentli seçicileri `MCityFilterBar` ile aynı çizgide birleştirir.
 */
@Composable
fun <T> MSegmentPillBar(
    options: List<MSegmentPillOption<T>>,
    selectedValue: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    pillHeightDp: Int = 35,
    fontSizeSp: Float = 11.5f,
    containerColor: Color = Color(0xFFF1F5F9),
    borderColor: Color = Color(0xFFE2E8F0)
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { option ->
                val isSelected = option.value == selectedValue
                UnifiedSegmentPill(
                    label = option.label,
                    icon = option.icon,
                    emoji = option.emoji,
                    selected = isSelected,
                    onClick = { onSelect(option.value) },
                    modifier = Modifier.weight(option.weight),
                    pillHeightDp = pillHeightDp,
                    horizontalPadding = 7.dp,
                    iconSize = 13.dp,
                    fontSizeSp = fontSizeSp
                )
            }
        }
    }
}

/**
 * Tesisler sekmesi için km yarıçapı ve il kapsamını `MCityFilterBar` çizgisinde sunan şık filtre çubuğu.
 */
@Composable
fun MRadiusFilterBar(
    currentCity: String,
    selectedRadiusKm: Int?,
    onSelectRadiusKm: (Int?) -> Unit,
    radiiKm: List<Int?> = listOf(5, 10, 25, 50, null),
    modifier: Modifier = Modifier
) {
    val safeCity = currentCity.ifBlank { "Şehir" }
    val allCityLabel = if (safeCity.length <= 8) "Tüm $safeCity" else "Tüm il"
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            radiiKm.forEach { km ->
                val selected = selectedRadiusKm == km
                val isAllCity = km == null
                val pillWeight = when {
                    isAllCity -> 1.68f
                    selected -> 1.08f
                    else -> 0.86f
                }
                UnifiedSegmentPill(
                    label = if (isAllCity) allCityLabel else "$km km",
                    icon = when {
                        isAllCity -> Icons.Rounded.Public
                        selected -> Icons.Rounded.NearMe
                        else -> null
                    },
                    emoji = null,
                    selected = selected,
                    onClick = { onSelectRadiusKm(km) },
                    modifier = Modifier.weight(pillWeight),
                    horizontalPadding = 4.dp,
                    iconSize = 12.dp,
                    fontSizeSp = 11f
                )
            }
        }
    }
}

@Composable
private fun UnifiedSegmentPill(
    label: String,
    icon: ImageVector?,
    emoji: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = null,
    pillHeightDp: Int = 35,
    horizontalPadding: androidx.compose.ui.unit.Dp = 8.dp,
    iconSize: androidx.compose.ui.unit.Dp = 13.dp,
    fontSizeSp: Float = 11.5f
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) MColors.Ink900 else Color.White.copy(alpha = 0.68f),
        animationSpec = tween(180),
        label = "unified_segment_pill_bg"
    )
    val contentColor = if (selected) Color.White else MColors.Ink800
    val iconColor = if (selected) Color(0xFFFFB37B) else MColors.BrandDeep
    val iconTextSpacing = if (horizontalPadding <= 5.dp) 3.dp else 4.dp

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(9.dp),
        color = bgColor,
        shadowElevation = if (selected) 2.dp else 0.dp,
        modifier = modifier.height(pillHeightDp.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (emoji != null) {
                Text(text = emoji, fontSize = fontSizeSp.sp)
                Spacer(Modifier.width(iconTextSpacing))
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(iconSize)
                )
                Spacer(Modifier.width(iconTextSpacing))
            }
            Text(
                text = label,
                fontSize = fontSizeSp.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (trailingIcon != null) {
                Spacer(Modifier.width(2.dp))
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.85f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityFilterPickerSheet(
    selectedCity: String?,
    currentCity: String,
    title: String = "İl seç",
    subtitle: String = "81 ilden ara ve seç",
    onSelectCity: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filteredCities = remember(query) {
        val needle = canonicalTurkishKey(query)
        val sorted = turkeyCities.sortedBy { canonicalTurkishKey(it) }
        if (needle.isBlank()) sorted else sorted.filter { canonicalTurkishKey(it).contains(needle) }
    }

    ModalBottomSheet(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        scrimColor = Color(0xFF0B1220).copy(alpha = 0.48f),
        dragHandle = { MPremiumSheetHandle() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MColors.Surface,
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
                .imePadding()
                .padding(horizontal = 20.dp)
        ) {
            MSheetHeading(
                title = title,
                subtitle = subtitle,
                icon = Icons.Rounded.LocationCity,
                onDismiss = onDismiss
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                colors = premiumTextFieldColors(),
                textStyle = MaterialTheme.typography.bodyMedium,
                label = { Text("İl ara") },
                placeholder = { Text("Örn. İstanbul, Ankara, İzmir…") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = if (query.isNotEmpty()) {
                    { IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "Temizle") } }
                } else null,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            Text(
                text = "${filteredCities.size} il",
                style = MaterialTheme.typography.labelMedium,
                color = MColors.Ink500
            )
            Spacer(Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredCities, key = { it }) { city ->
                    val isSelected = selectedCity != null && matchesTurkishPlace(selectedCity, city)
                    val isCurrent = matchesTurkishPlace(currentCity, city)
                    Surface(
                        onClick = {
                            onSelectCity(city)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(13.dp),
                        color = if (isSelected) MColors.BrandTint else Color.White,
                        border = BorderStroke(1.dp, if (isSelected) MColors.BrandDeep else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (isSelected) Color.White else Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isCurrent) Icons.Rounded.NearMe else Icons.Rounded.LocationOn,
                                    contentDescription = null,
                                    tint = if (isSelected || isCurrent) MColors.BrandDeep else MColors.Ink500,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = city,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = MColors.Ink900
                                )
                                if (isCurrent) {
                                    Text(
                                        text = "Mevcut konumun",
                                        fontSize = 10.5.sp,
                                        color = MColors.BrandDeep,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Seçili",
                                    tint = MColors.BrandDeep,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class MPillTone { NEUTRAL, BRAND, DARK, DANGER }

@Composable
fun MPillActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tone: MPillTone = MPillTone.BRAND,
    enabled: Boolean = true,
    compact: Boolean = false
) {
    val container = when {
        !enabled -> Color(0xFFE2E8F0)
        tone == MPillTone.BRAND -> MColors.Brand
        tone == MPillTone.DARK -> MColors.Ink900
        tone == MPillTone.DANGER -> Color(0xFFFFF1F2)
        else -> Color(0xFFF8FAFC)
    }
    val border = when {
        !enabled -> Color(0xFFCBD5E1)
        tone == MPillTone.BRAND -> Color(0xFFFF8552)
        tone == MPillTone.DARK -> MColors.Ink900
        tone == MPillTone.DANGER -> Color(0xFFFECDD3)
        else -> Color(0xFFCBD5E1)
    }
    val fg = when {
        !enabled -> MColors.Ink600
        tone == MPillTone.BRAND -> Color.White
        tone == MPillTone.DARK -> Color.White
        tone == MPillTone.DANGER -> MColors.Rose
        else -> MColors.Ink900
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(if (compact) 11.dp else 13.dp),
        color = container,
        border = BorderStroke(1.dp, border),
        shadowElevation = if (enabled && (tone == MPillTone.BRAND || tone == MPillTone.DARK)) 2.dp else 0.dp,
        modifier = modifier.height(if (compact) 36.dp else 42.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (compact) 12.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(if (compact) 15.dp else 17.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = text,
                fontSize = if (compact) 12.sp else 13.sp,
                fontWeight = FontWeight.Bold,
                color = fg,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MPremiumMenuRow(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    destructive: Boolean = false
) {
    val accent = if (destructive) MColors.Rose else MColors.Ink800
    val iconBg = if (destructive) Color(0xFFFFF1F2) else Color(0xFFF1F5F9)
    val iconTint = if (destructive) MColors.Rose else MColors.BrandDeep
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(13.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, if (destructive) Color(0xFFFECDD3) else Color(0xFFE2E8F0)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(17.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = MColors.Ink500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MColors.Ink400,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

