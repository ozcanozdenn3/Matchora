package com.matchora.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.state.*
import com.matchora.app.ui.theme.MColors

@Composable
fun VenueSpotlightCard(
    venue: VenueInfo,
    bookedSlotIds: Set<String>,
    onCardClick: (() -> Unit)? = null,
    onDirections: (() -> Unit)? = null,
    onShareVenue: (() -> Unit)? = null,
    onSelect: (VenueSlotInfo) -> Unit
) {
    val accent = if (venue.name.contains("Kort", true) || venue.name.contains("Tenis", true)) Color(0xFF9EDDC8) else Color(0xFFFFC08E)
    val availableCount = venue.slots.count { it.available && "${venue.id}:${it.id}" !in bookedSlotIds }

    MCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        elevation = 2.dp,
        borderColor = Color(0xFFE2E8F0),
        borderWidth = 1.dp,
        onClick = onCardClick
    ) {
        // 1. Üst Başlık: Tesis adı, konum, saha sayısı, fiyat ve Tesis ID
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF1F323A), Color(0xFF111B24))))
                .drawBehind {
                    val x = size.width * .64f
                    val y = -10.dp.toPx()
                    val w = size.width * .40f
                    val h = size.height + 20.dp.toPx()
                    val line = accent.copy(alpha = .07f)
                    val stroke = Stroke(0.8.dp.toPx())
                    drawCircle(accent.copy(alpha = .06f), radius = 54.dp.toPx(), center = Offset(size.width * .88f, size.height * .3f))
                    drawRoundRect(line, Offset(x, y), Size(w, h), CornerRadius(6.dp.toPx()), style = stroke)
                    drawLine(line, Offset(x, y + h / 2), Offset(x + w, y + h / 2), 0.8.dp.toPx())
                    drawCircle(line, w * .22f, Offset(x + w / 2, y + h / 2), style = stroke)
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accent.copy(alpha = .16f),
                border = BorderStroke(0.8.dp, accent.copy(alpha = .32f)),
                shadowElevation = 0.dp
            ) {
                Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Stadium, null, Modifier.size(18.dp), tint = accent)
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = venue.name,
                    color = Color.White,
                    fontSize = 14.5.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Rounded.LocationOn, null, Modifier.size(12.dp), tint = accent)
                    val placeText = listOf(venue.area, venue.city).filter { it.isNotBlank() }.distinct().joinToString(", ")
                    Text(
                        text = "$placeText${venue.distanceKm?.let { " · $it km" }.orEmpty()} · ${venue.courts} saha",
                        color = Color(0xFFC8D5DC),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = Color.White.copy(alpha = 0.10f),
                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.18f))
                ) {
                    Text(
                        text = venueDisplayId(venue.id),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accent.copy(alpha = .14f),
                    border = BorderStroke(0.8.dp, accent.copy(alpha = .32f)),
                    shadowElevation = 0.dp
                ) {
                    Text(
                        text = "₺${venue.fromPriceLabel}",
                        color = accent,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // 2. Gövde: Sade olanaklar, temiz saat kartları ve birleşik aksiyon satırı
        Column(
            modifier = Modifier.padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(venue.amenities) { amenity ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            shadowElevation = 0.dp,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.5.dp)
                            ) {
                                Icon(Icons.Rounded.Check, null, Modifier.size(11.dp), tint = MColors.Mint)
                                Text(amenity, fontSize = 10.5.sp, color = MColors.Ink700, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (availableCount > 0) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                    shadowElevation = 0.dp,
                    border = BorderStroke(1.dp, if (availableCount > 0) Color(0xFFBBF7D0) else Color(0xFFE2E8F0))
                ) {
                    Text(
                        text = if (availableCount > 0) "$availableCount boş saat" else "Dolu",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (availableCount > 0) Color(0xFF15803D) else MColors.Ink500,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                contentPadding = PaddingValues(vertical = 1.dp)
            ) {
                items(venue.slots, key = { it.id }) { slot ->
                    val booked = "${venue.id}:${slot.id}" in bookedSlotIds
                    val available = slot.available && !booked
                    Surface(
                        onClick = { onSelect(slot) },
                        enabled = available,
                        shape = RoundedCornerShape(11.dp),
                        color = if (available) Color(0xFFF8FAFC) else Color(0xFFF1F5F9),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (available) Color(0xFFDCE4F0) else Color(0xFFE2E8F0)
                        ),
                        shadowElevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .widthIn(min = 82.dp)
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = slot.time,
                                fontSize = 13.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (available) MColors.Ink900 else MColors.Ink400
                            )
                            Text(
                                text = "${slot.day} · ${slot.court}",
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (available) MColors.Ink600 else MColors.Ink400
                            )
                            Text(
                                text = if (booked) "Rezerve" else if (!available) "Dolu" else "₺${slot.priceLabel}",
                                fontSize = 10.5.sp,
                                lineHeight = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (available) MColors.BrandDeep else MColors.Ink400
                            )
                        }
                    }
                }
            }

            // Aksiyon Satırı: Detay & Saatler, Yol Tarifi ve Paylaşım
            if (onCardClick != null || onDirections != null || onShareVenue != null) {
                val backendEnabled = LocalBackendRepository.current?.enabled == true
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onCardClick != null) {
                        MButton(
                            text = "Detay & Saatler",
                            onClick = onCardClick,
                            modifier = Modifier.weight(1f),
                            isPrimary = false,
                            height = 38.dp
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    if (onDirections != null) {
                        MPillActionButton(
                            text = "Yol Tarifi",
                            icon = Icons.Rounded.Directions,
                            tone = MPillTone.BRAND,
                            compact = true,
                            onClick = onDirections
                        )
                    }
                    if (onShareVenue != null) {
                        FilledTonalButton(
                            onClick = onShareVenue,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Share,
                                if (backendEnabled) "Paylaş" else "Paylaş (+2 TKN)",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

