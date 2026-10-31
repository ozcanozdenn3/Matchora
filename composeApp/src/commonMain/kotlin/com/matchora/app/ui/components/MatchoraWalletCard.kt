package com.matchora.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.state.PaymentMethod
import com.matchora.app.ui.theme.MColors

@Composable
fun MatchoraWalletCard(holder: String, balance: Int = 0, locked: Int = 0, card: PaymentMethod? = null) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(25.dp),
                ambientColor = Color(0x280B1220),
                spotColor = Color(0x380B1220)
            ),
        shape = RoundedCornerShape(25.dp),
        color = Color.White,
        border = BorderStroke(1.2.dp, Color(0xFFFFB37B).copy(alpha = 0.55f))
    ) {
        SportsFlowBackground(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(25.dp)),
            glyphCount = 20,
            protectCenter = false,
            animate = true
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "MATCHORA",
                            color = MColors.Ink900,
                            fontSize = 15.sp,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (card == null) "SPORCU CÜZDANI" else "ÖDEME KARTI",
                            color = MColors.BrandDeep,
                            fontSize = 9.5.sp,
                            letterSpacing = 1.4.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.Contactless,
                        contentDescription = "Temassız",
                        tint = MColors.BrandDeep,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = Color(0xFFE8BC67),
                        border = BorderStroke(1.dp, Color(0xFFC79842)),
                        shadowElevation = 3.dp
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Memory,
                            contentDescription = "Çip",
                            modifier = Modifier.padding(7.dp).size(24.dp),
                            tint = Color(0xFF5E3F18)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        if (card == null) {
                            Text(
                                text = "Kullanılabilir bakiye",
                                color = MColors.Ink600,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$balance",
                                    color = MColors.Ink900,
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = " TKN",
                                    color = MColors.BrandDeep,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "••••  ••••  ${card.last4}",
                                color = MColors.Ink900,
                                fontSize = 20.sp,
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "KART SAHİBİ",
                            color = MColors.Ink500,
                            fontSize = 8.5.sp,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = holder.uppercase(),
                            color = MColors.Ink900,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (card == null) "$locked TKN kilitli" else card.expiry,
                            color = MColors.BrandDeep,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = card?.brand ?: "M / CLUB",
                            color = MColors.Ink900,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}
