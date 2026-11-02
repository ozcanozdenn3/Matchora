package com.matchora.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.ui.theme.MColors

// Static light and court-inspired contours: no continuous repainting of list cards.
fun Modifier.premiumSurface(accent: Color = MColors.Brand): Modifier = drawBehind {
    drawRect(Brush.linearGradient(listOf(Color.White.copy(alpha = .42f), accent.copy(alpha = .035f), Color.Transparent)))
    val center = Offset(size.width + 18.dp.toPx(), -22.dp.toPx())
    for (radius in listOf(70f, 94f, 118f)) {
        drawCircle(accent.copy(alpha = .065f), radius.dp.toPx(), center, style = Stroke(1.dp.toPx()))
    }
}

@Composable
fun Modifier.pressMotion(source: MutableInteractionSource): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .975f else 1f,
        spring(dampingRatio = .7f, stiffness = 650f), label = "touch_response")
    return graphicsLayer { scaleX = scale; scaleY = scale }
}

@Composable
fun MFeatureHeader(title: String, subtitle: String, icon: ImageVector,
                   accent: Color = Color(0xFFFFB68F), modifier: Modifier = Modifier) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val reveal by animateFloatAsState(if (appeared) 1f else 0f, tween(420), label = "header_reveal")
    Surface(modifier.fillMaxWidth().graphicsLayer { alpha = reveal; translationY = (1f - reveal) * 14.dp.toPx() },
        shape = RoundedCornerShape(24.dp), color = MColors.Ink900, shadowElevation = 5.dp) {
        Row(Modifier.background(Brush.linearGradient(listOf(Color(0xFF24344C), MColors.Ink900)))
            .drawBehind {
                val center = Offset(size.width * .93f, size.height * .25f)
                drawCircle(accent.copy(alpha = .09f), 72.dp.toPx(), center)
                drawCircle(accent.copy(alpha = .13f), 92.dp.toPx(), center, style = Stroke(1.dp.toPx()))
                drawCircle(accent.copy(alpha = .08f), 108.dp.toPx(), center, style = Stroke(1.dp.toPx()))
            }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(24.dp, 3.dp).background(accent, RoundedCornerShape(2.dp)))
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFFCCD6E5))
            }
            Spacer(Modifier.width(12.dp))
            Surface(shape = RoundedCornerShape(18.dp), color = accent.copy(alpha = .13f)) {
                Icon(icon, null, Modifier.padding(10.dp).size(23.dp), tint = accent)
            }
        }
    }
}

@Composable
fun MPremiumSheetHandle() {
    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(38.dp, 4.dp).background(
            Brush.horizontalGradient(listOf(MColors.Ink200, MColors.BrandLight, MColors.Ink200)), RoundedCornerShape(4.dp)))
    }
}

@Composable
fun MSheetHeading(title: String, subtitle: String, icon: ImageVector, onDismiss: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MColors.Ink900,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, Color(0xFF28374E)),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Color(0x320B1220),
                spotColor = Color(0x420B1220)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF1F2D42), MColors.Ink900)))
                .drawBehind {
                    val center = Offset(size.width * .92f, size.height * .22f)
                    drawCircle(Color(0xFFFFB68F).copy(alpha = .08f), 68.dp.toPx(), center)
                    drawCircle(Color(0xFFFFB68F).copy(alpha = .12f), 88.dp.toPx(), center, style = Stroke(1.dp.toPx()))
                }
                .padding(horizontal = 15.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF24344D),
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, Color(0x38FFB68F)),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(22.dp), tint = Color(0xFFFFB68F))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF24344D),
                shadowElevation = 3.dp,
                border = BorderStroke(1.dp, Color(0xFF364966)),
                modifier = Modifier.size(35.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Close, "Kapat", tint = Color.White, modifier = Modifier.size(17.dp))
                }
            }
        }
    }
}

/**
 * Premium floating white card with a header row ([icon] + [label], e.g. "İlan Başlığı", "Kısa Açıklama")
 * and an empty floating shadowed white input box inside that focuses immediately on tap.
 */
@Composable
fun MFloatingInputBox(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    subtitle: String? = null,
    icon: ImageVector = Icons.Rounded.EditNote,
    badgeText: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 3,
    isError: Boolean = false,
    suggestions: List<String> = emptyList(),
    onSuggestionClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 6.dp,
        border = BorderStroke(
            width = 1.dp,
            color = when {
                isError -> MColors.Rose.copy(alpha = 0.65f)
                isFocused -> MColors.Brand.copy(alpha = 0.55f)
                else -> Color(0xFFE4E9F2)
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color(0x2E0B1220),
                spotColor = Color(0x400B1220)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                focusRequester.requestFocus()
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White, Color(0xFFFCFDFE))
                    )
                )
                .padding(horizontal = 13.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Kutu Başlığı (örn. İlan Başlığı, Kısa Açıklama)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = MColors.Ink900,
                    shadowElevation = 3.dp,
                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.14f)),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color(0xFFFFB37B),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MColors.Ink900
                )
            }

            // İçeriği Boş Yüzen Beyaz Yazı Kutusu
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                shadowElevation = if (isFocused) 6.dp else 4.dp,
                border = BorderStroke(
                    width = if (isFocused) 1.5.dp else 1.dp,
                    color = when {
                        isError -> MColors.Rose.copy(alpha = 0.75f)
                        isFocused -> MColors.Brand
                        else -> Color(0xFFDCE4F0)
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = if (isFocused) 7.dp else 5.dp,
                        shape = RoundedCornerShape(14.dp),
                        ambientColor = Color(0x280B1220),
                        spotColor = Color(0x380B1220)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 12.dp,
                            vertical = if (singleLine) 11.dp else 12.dp
                        ),
                    verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = if (singleLine) 0.dp else 2.dp)
                            .width(3.dp)
                            .height(if (singleLine) 18.dp else 36.dp)
                            .background(
                                color = when {
                                    isError -> MColors.Rose
                                    isFocused || value.isNotEmpty() -> MColors.Brand
                                    else -> Color(0xFFDCE4F0)
                                },
                                shape = RoundedCornerShape(999.dp)
                            )
                    )

                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = singleLine,
                        minLines = minLines,
                        maxLines = maxLines,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.5.sp,
                            fontWeight = if (singleLine) FontWeight.SemiBold else FontWeight.Medium,
                            lineHeight = 19.sp,
                            color = MColors.Ink900
                        ),
                        cursorBrush = SolidColor(MColors.BrandDeep),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = if (singleLine) 22.dp else (minLines * 22).coerceAtLeast(48).dp)
                            .focusRequester(focusRequester)
                            .onFocusChanged { isFocused = it.isFocused },
                        decorationBox = { innerTextField ->
                            Box(
                                contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                innerTextField()
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Kart üzerinde yüzen gölgeli açılır seçim menüsü (Seviye Aralığı, Süre, Paylaşım Ağı, Maç Formatı vb.).
 * Çoklu buton ve metin kalabalığını azaltırken tüm seçenekleri eksiksiz korur.
 */
@Composable
fun <T> MFloatingDropdownBox(
    label: String,
    selectedValue: T,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    icon: ImageVector? = Icons.Rounded.Tune,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = if (expanded) 8.dp else 5.dp,
            border = BorderStroke(
                width = if (expanded) 1.5.dp else 1.dp,
                color = if (expanded) MColors.Brand.copy(alpha = 0.65f) else Color(0xFFE4E9F2)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .shadow(
                    elevation = if (expanded) 9.dp else 7.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color(0x2A0B1220),
                    spotColor = Color(0x3C0B1220)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFFCFDFE))))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    if (icon != null) {
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = MColors.Ink900,
                            shadowElevation = 3.dp,
                            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.14f)),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB37B),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MColors.Ink900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(11.dp),
                    color = Color.White,
                    shadowElevation = 3.dp,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (expanded) MColors.Brand.copy(alpha = 0.55f) else Color(0xFFDCE4F0)
                    ),
                    modifier = Modifier.shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(11.dp),
                        ambientColor = Color(0x200B1220),
                        spotColor = Color(0x300B1220)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = optionLabel(selectedValue),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MColors.BrandDeep,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 175.dp)
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MColors.BrandDeep,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 10.dp,
            border = BorderStroke(1.dp, Color(0xFFE4E9F2)),
            modifier = Modifier.widthIn(min = 210.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selectedValue
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = optionLabel(option),
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (isSelected) MColors.BrandDeep else MColors.Ink900,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = MColors.BrandTint,
                                    border = BorderStroke(1.dp, MColors.Brand.copy(alpha = 0.35f)),
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = MColors.BrandDeep,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Kart üzerinde yüzen gölgeli tek satırlık seçici/tetikleyici (örn. Şehir · İlçe seçimi).
 */
@Composable
fun MFloatingActionRow(
    label: String,
    valueText: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 5.dp,
        border = BorderStroke(1.dp, Color(0xFFE4E9F2)),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .shadow(
                elevation = 7.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0x2A0B1220),
                spotColor = Color(0x3C0B1220)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFFCFDFE))))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = MColors.Ink900,
                    shadowElevation = 3.dp,
                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.14f)),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color(0xFFFFB37B),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MColors.Ink900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(11.dp),
                color = Color.White,
                shadowElevation = 3.dp,
                border = BorderStroke(1.dp, Color(0xFFDCE4F0)),
                modifier = Modifier.shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(11.dp),
                    ambientColor = Color(0x200B1220),
                    spotColor = Color(0x300B1220)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = valueText,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MColors.BrandDeep,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 175.dp)
                    )
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MColors.BrandDeep,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

/** Shared input treatment for sheets, dialogs, search, and creation forms. */
@Composable
fun premiumTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    focusedBorderColor = MColors.BrandDeep,
    unfocusedBorderColor = Color(0xFFDCE4F0),
    focusedLabelColor = MColors.BrandDeep,
    unfocusedLabelColor = MColors.Ink500,
    cursorColor = MColors.BrandDeep
)

/** Triggered when the user taps the already-selected bottom navigation tab to scroll to top. */
val LocalScrollToTopTrigger = compositionLocalOf { 0 }

/** Selection colors for the account flows, matching the existing ink and gold cards. */
@Composable
fun premiumChoiceChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = Color.White,
    labelColor = MColors.Ink600,
    selectedContainerColor = MColors.Ink900,
    selectedLabelColor = Color(0xFFE1C49D)
)
