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
fun VenueSpotlightCard(venue: VenueInfo, bookedSlotIds: Set<String>, onSelect: (VenueSlotInfo) -> Unit) {
    val accent = if (venue.name.contains("Kort", true) || venue.name.contains("Tenis", true)) Color(0xFF9EDDC8) else Color(0xFFFFC08E)
    val availableCount = venue.slots.count { it.available && "${venue.id}:${it.id}" !in bookedSlotIds }

    MCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        elevation = 2.dp,
        borderColor = Color(0xFFE4E9F1),
        borderWidth = 1.dp
    ) {
        // 1. Üst Başlık: Tesis adı, konum, saha sayısı ve başlangıç fiyatı tek bakışta okunaklı
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
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(11.dp),
                color = accent.copy(alpha = .14f),
                border = BorderStroke(0.8.dp, accent.copy(alpha = .28f)),
                shadowElevation = 4.dp,
                modifier = Modifier.shadow(6.dp, RoundedCornerShape(11.dp), ambientColor = Color(0x55000000), spotColor = Color(0x77000000))
            ) {
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Stadium, null, Modifier.size(17.dp), tint = accent)
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = venue.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Rounded.LocationOn, null, Modifier.size(12.dp), tint = accent)
                    Text(
                        text = "${venue.area} · ${venue.distanceKm} km · ${venue.courts} saha",
                        color = Color(0xFFC8D5DC),
                        fontSize = 11.5.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(9.dp),
                color = Color.White.copy(alpha = .08f),
                border = BorderStroke(0.8.dp, accent.copy(alpha = .30f)),
                shadowElevation = 4.dp,
                modifier = Modifier.shadow(6.dp, RoundedCornerShape(9.dp), ambientColor = Color(0x55000000), spotColor = Color(0x77000000))
            ) {
                Text(
                    text = "${venue.fromPriceTry} TKN",
                    color = accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                )
            }
        }

        // 2. Gövde: Ferah olanaklar ve kart üzerinde yüzen gölgeli saat kartları
        Column(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(venue.amenities) { amenity ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            shadowElevation = 4.dp,
                            border = BorderStroke(1.dp, Color(0xFFE4E9F2)),
                            modifier = Modifier
                                .padding(vertical = 1.dp)
                                .shadow(
                                    elevation = 5.dp,
                                    shape = RoundedCornerShape(8.dp),
                                    ambientColor = Color(0x280B1220),
                                    spotColor = Color(0x380B1220)
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.5.dp)
                            ) {
                                Icon(Icons.Rounded.Check, null, Modifier.size(11.dp), tint = MColors.Mint)
                                Text(amenity, fontSize = 10.5.sp, color = MColors.Ink700, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (availableCount > 0) Color(0xFFF0FDF4) else Color.White,
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.dp, if (availableCount > 0) Color(0xFFBBF7D0) else Color(0xFFE4E9F2)),
                    modifier = Modifier
                        .padding(vertical = 1.dp)
                        .shadow(
                            elevation = 5.dp,
                            shape = RoundedCornerShape(8.dp),
                            ambientColor = Color(0x260B1220),
                            spotColor = Color(0x360B1220)
                        )
                ) {
                    Text(
                        text = if (availableCount > 0) "$availableCount boş saat" else "Dolu",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (availableCount > 0) Color(0xFF15803D) else MColors.Ink500,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 3.dp, horizontal = 2.dp)
            ) {
                items(venue.slots, key = { it.id }) { slot ->
                    val booked = "${venue.id}:${slot.id}" in bookedSlotIds
                    val available = slot.available && !booked
                    Surface(
                        onClick = { onSelect(slot) },
                        enabled = available,
                        shape = RoundedCornerShape(13.dp),
                        color = if (available) Color.White else Color(0xFFF7F9FC),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (available) Color(0xFFDCE4F0) else Color(0xFFE8ECF2)
                        ),
                        shadowElevation = if (available) 6.dp else 2.dp,
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .shadow(
                                elevation = if (available) 8.dp else 3.dp,
                                shape = RoundedCornerShape(13.dp),
                                ambientColor = Color(0x320B1220),
                                spotColor = Color(0x440B1220)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .widthIn(min = 84.dp)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = slot.time,
                                fontSize = 13.5.sp,
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
                                text = if (booked) "Rezerve" else if (!available) "Dolu" else "${slot.priceTry} TKN",
                                fontSize = 10.5.sp,
                                lineHeight = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (available) MColors.BrandDeep else MColors.Ink400
                            )
                        }
                    }
                }
            }
        }
    }
}

