package com.matchora.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.ui.theme.MColors

@Composable
actual fun NativeMapView(
    modifier: Modifier,
    latitude: Double,
    longitude: Double,
    zoomMeters: Double,
    pins: List<MapPinData>,
    selectedPinId: String?,
    onPinSelected: (String) -> Unit,
    interactive: Boolean
) {
    Box(
        modifier = modifier
            .background(Color(0xFFF8FAFC))
            .drawBehind {
                val w = size.width
                val h = size.height

                // 1. Marmara Denizi & Sahil
                val seaBrush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFFD6EAF8), Color(0xFFEBF5FB)),
                    startX = 0f,
                    endX = w * 0.35f
                )
                val coastPath = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(w * 0.28f, 0f)
                    cubicTo(w * 0.32f, h * 0.25f, w * 0.36f, h * 0.45f, w * 0.30f, h * 0.65f)
                    cubicTo(w * 0.26f, h * 0.80f, w * 0.34f, h * 0.90f, w * 0.28f, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(coastPath, brush = seaBrush)
                drawPath(coastPath, color = Color(0xFFAED6F1), style = Stroke(width = 2.5f))

                // 2. Parklar
                val parkColor = Color(0xFFE8F8F5)
                val parkBorder = Color(0xFFA3E4D7)

                drawRoundRect(parkColor, Offset(w * 0.32f, h * 0.12f), Size(w * 0.30f, h * 0.18f), CornerRadius(18f, 18f))
                drawRoundRect(parkBorder, Offset(w * 0.32f, h * 0.12f), Size(w * 0.30f, h * 0.18f), CornerRadius(18f, 18f), style = Stroke(1.5f))

                drawRoundRect(parkColor, Offset(w * 0.34f, h * 0.45f), Size(w * 0.38f, h * 0.20f), CornerRadius(18f, 18f))
                drawRoundRect(parkBorder, Offset(w * 0.34f, h * 0.45f), Size(w * 0.38f, h * 0.20f), CornerRadius(18f, 18f), style = Stroke(1.5f))

                // 3. Caddeler
                fun drawCleanRoad(start: Offset, end: Offset, width: Float) {
                    drawLine(Color(0xFFCBD5E1), start, end, strokeWidth = width + 2f)
                    drawLine(Color.White, start, end, strokeWidth = width)
                }

                drawCleanRoad(Offset(w * 0.30f, 0f), Offset(w * 0.30f, h), 12f)
                drawCleanRoad(Offset(w * 0.30f, h * 0.35f), Offset(w, h * 0.35f), 10f)
                drawCleanRoad(Offset(w * 0.30f, h * 0.70f), Offset(w, h * 0.70f), 10f)
                drawCleanRoad(Offset(w * 0.65f, 0f), Offset(w * 0.65f, h), 8f)
            }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val totalWidth = maxWidth
            val totalHeight = maxHeight

            // Koordinat oranlarına göre pin yerleşimi
            pins.forEachIndexed { index, pin ->
                val isSelected = pin.id == selectedPinId
                // Dağıtılmış koordinatlar
                val xRatio = when (index) {
                    0 -> 0.38f
                    1 -> 0.58f
                    2 -> 0.76f
                    3 -> 0.46f
                    4 -> 0.72f
                    5 -> 0.35f
                    else -> 0.64f
                }
                val yRatio = when (index) {
                    0 -> 0.22f
                    1 -> 0.36f
                    2 -> 0.24f
                    3 -> 0.48f
                    4 -> 0.42f
                    5 -> 0.32f
                    else -> 0.52f
                }

                val xPos = totalWidth * xRatio
                val yPos = totalHeight * yRatio

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .offset(x = (xPos.value - 30).dp, y = (yPos.value - 20).dp)
                        .clickable { onPinSelected(pin.id) }
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MColors.Brand else Color.White,
                        shadowElevation = if (isSelected) 8.dp else 3.dp,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) MColors.BrandLight else Color(0x20000000)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(pin.sportEmoji, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = pin.title.take(12),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (isSelected) Color.White else MColors.Ink900
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) MColors.Brand else Color(0xFF64748B))
                    )
                }
            }
        }
    }
}
