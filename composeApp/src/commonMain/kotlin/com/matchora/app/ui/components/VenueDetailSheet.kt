package com.matchora.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.matchora.app.state.*
import com.matchora.app.ui.theme.MColors
import kotlin.math.roundToInt

// ═══════════════════════════════════════════════════════════════════════════════
// VENUE DETAIL SHEET — TESİS DETAYLARI VE FOTOĞRAF GALERİSİ
// Tesis kartına tıklandığında tesisin bilgilerini, en fazla 4 fotoğrafını ve müsait saatlerini gösterir.
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VenueDetailSheet(
    venue: VenueInfo,
    bookedSlotIds: Set<String>,
    onDismiss: () -> Unit,
    onDirections: () -> Unit,
    onShare: () -> Unit,
    onOpenReviews: () -> Unit,
    onSelectSlot: (VenueSlotInfo) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var fullscreenPhotoIndex by remember(venue.id) { mutableStateOf<Int?>(null) }
    val photos = remember(venue.id, venue.photoUploadIds) { venue.photoUploadIds.distinct().take(4) }
    val availableCount = remember(venue.slots, bookedSlotIds) {
        venue.slots.count { it.available && "${venue.id}:${it.id}" !in bookedSlotIds }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        scrimColor = Color(0xFF0B1220).copy(alpha = 0.48f),
        containerColor = Color(0xFFF5F6F8),
        dragHandle = { MPremiumSheetHandle() }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                val cityArea = listOf(venue.area, venue.city).filter { it.isNotBlank() }.distinct().joinToString(" · ")
                val distLabel = venue.distanceKm?.let { "$it km" }
                val ratingText = if (venue.reviews > 0) "★ ${(venue.rating * 10).roundToInt() / 10.0} (${venue.reviews})" else "Yeni tesis"
                val subtitle = listOfNotNull(cityArea.takeIf { it.isNotBlank() }, distLabel, ratingText, venueDisplayId(venue.id)).joinToString(" · ")
                MSheetHeading(
                    title = venue.name,
                    subtitle = subtitle,
                    icon = Icons.Rounded.Stadium,
                    onDismiss = onDismiss
                )
            }

            if (photos.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE4E9F2)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(6.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x200B1220), spotColor = Color(0x2D0B1220))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tesis fotoğrafları (${photos.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MColors.Ink900
                                )
                                Text(
                                    text = "Büyütmek için dokun",
                                    fontSize = 11.sp,
                                    color = MColors.Ink500
                                )
                            }
                            if (photos.size == 1) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(190.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { fullscreenPhotoIndex = 0 }
                                ) {
                                    BackendPhoto(
                                        uploadId = photos[0],
                                        description = "${venue.name} fotoğrafı",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            } else {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    itemsIndexed(photos) { index, uploadId ->
                                        Box(
                                            modifier = Modifier
                                                .width(230.dp)
                                                .height(156.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable { fullscreenPhotoIndex = index }
                                        ) {
                                            BackendPhoto(
                                                uploadId = uploadId,
                                                description = "${venue.name} fotoğrafı ${index + 1}",
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(999.dp),
                                                color = Color(0xAA0B1220),
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(8.dp)
                                            ) {
                                                Text(
                                                    text = "${index + 1}/${photos.size}",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
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

            item {
                MSubCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tesis bilgileri",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MColors.Ink900
                            )
                            if (venue.verified) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = MColors.Brand.copy(alpha = 0.12f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Rounded.Verified, null, tint = MColors.Brand, modifier = Modifier.size(14.dp))
                                        Text("Onaylı tesis", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MColors.Brand)
                                    }
                                }
                            }
                        }

                        if (venue.description.isNotBlank()) {
                            Text(
                                text = venue.description,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = MColors.Ink700
                            )
                        }

                        VenueInfoRow(
                            icon = Icons.Rounded.LocationOn,
                            label = "Adres",
                            value = venue.address.ifBlank { listOf(venue.area, venue.city).filter { it.isNotBlank() }.joinToString(", ") }
                        )

                        if (venue.phone.isNotBlank()) {
                            VenueInfoRow(
                                icon = Icons.Rounded.Phone,
                                label = "Telefon",
                                value = venue.phone,
                                onClick = {
                                    val digits = venue.phone.filter { it.isDigit() || it == '+' }
                                    if (digits.isNotEmpty()) runCatching { uriHandler.openUri("tel:$digits") }
                                }
                            )
                        }

                        VenueInfoRow(
                            icon = Icons.Rounded.SportsSoccer,
                            label = "Branşlar ve sahalar",
                            value = "${venue.sports.joinToString(", ") { it.label }} · ${venue.courts} saha"
                        )

                        VenueInfoRow(
                            icon = Icons.Rounded.Schedule,
                            label = "İptal politikası",
                            value = "Ücretsiz iptal: ${venue.cancellationPolicy}"
                        )

                        if (venue.amenities.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                venue.amenities.forEach { amenity ->
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = MColors.SurfaceMuted,
                                        border = BorderStroke(1.dp, MColors.LineSoft)
                                    ) {
                                        Text(
                                            text = amenity,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MColors.Ink800,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MButton(
                        text = "Yol Tarifi",
                        onClick = onDirections,
                        modifier = Modifier.weight(1f),
                        isPrimary = false,
                        height = 42.dp
                    )
                    MButton(
                        text = "Yorumlar (${venue.reviews})",
                        onClick = onOpenReviews,
                        modifier = Modifier.weight(1f),
                        isPrimary = false,
                        height = 42.dp
                    )
                    FilledTonalButton(
                        onClick = onShare,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(42.dp)
                    ) {
                        Icon(Icons.Rounded.Share, "Tesisi paylaş", modifier = Modifier.size(18.dp), tint = MColors.BrandDeep)
                    }
                }
            }

            item {
                Text(
                    text = if (availableCount > 0) "Müsait saatler ($availableCount)" else "Müsait saatler",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MColors.Ink900
                )
            }

            if (venue.slots.isEmpty()) {
                item {
                    MSubCard {
                        Text(
                            text = "Şu anda listelenen saat yok. İşletmeci yeni saat eklediğinde buradan rezervasyon yapabilirsin.",
                            fontSize = 13.sp,
                            color = MColors.Ink500
                        )
                    }
                }
            } else {
                items(venue.slots, key = { it.id }) { slot ->
                    val booked = "${venue.id}:${slot.id}" in bookedSlotIds
                    val isOpen = slot.available && !booked
                    MSubCard(
                        borderColor = if (isOpen) MColors.Brand.copy(alpha = 0.35f) else Color(0xFFE4E9F2),
                        backgroundColor = if (isOpen) Color.White else Color(0xFFF3F4F6),
                        onClick = if (isOpen) ({ onSelectSlot(slot) }) else null
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${slot.day} · ${slot.time}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isOpen) MColors.Ink900 else MColors.Ink500
                                )
                                Text(
                                    text = "${slot.court} · ${slot.durationMin} dk",
                                    fontSize = 12.sp,
                                    color = MColors.Ink500
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isOpen) MColors.Brand else Color(0xFFE5E7EB)
                            ) {
                                Text(
                                    text = if (booked) "Rezerve" else if (isOpen) "₺${formatTryMinor(slot.priceMinor)} · Ayır" else "Dolu",
                                    color = if (isOpen) Color.White else MColors.Ink500,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fullscreenPhotoIndex?.let { initial ->
        var current by remember(initial) { mutableIntStateOf(initial.coerceIn(0, (photos.size - 1).coerceAtLeast(0))) }
        Dialog(onDismissRequest = { fullscreenPhotoIndex = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0B1220),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${venue.name} · ${current + 1}/${photos.size}",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { fullscreenPhotoIndex = null }) {
                            Icon(Icons.Rounded.Close, "Kapat", tint = Color.White)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        BackendPhoto(
                            uploadId = photos[current],
                            description = "${venue.name} fotoğraf ${current + 1}",
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x88000000))))
                                .padding(8.dp)
                        )
                    }
                    if (photos.size > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { if (current > 0) current-- },
                                enabled = current > 0
                            ) {
                                Text("Önceki", color = if (current > 0) Color.White else Color.Gray)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                photos.indices.forEach { i ->
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (i == current) MColors.Brand else Color.White.copy(alpha = 0.35f))
                                    )
                                }
                            }
                            TextButton(
                                onClick = { if (current < photos.lastIndex) current++ },
                                enabled = current < photos.lastIndex
                            ) {
                                Text("Sonraki", color = if (current < photos.lastIndex) Color.White else Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VenueInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, tint = MColors.Brand, modifier = Modifier.size(18.dp).padding(top = 2.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, color = MColors.Ink500, fontWeight = FontWeight.Medium)
            Text(
                text = value,
                fontSize = 13.sp,
                color = if (onClick != null) MColors.Brand else MColors.Ink900,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
