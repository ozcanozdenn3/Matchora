package com.matchora.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.state.UserProfile
import com.matchora.app.state.VerificationLevel
import com.matchora.app.ui.theme.MColors

// ═══════════════════════════════════════════════════════════════════════════════
// DİJİTAL SPORCU KARTI ÖNİZLEMESİ
// QR ve barkod öğeleri demo görünümüdür; doğrulanmış bilet veya kimlik yerine geçmez.
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AthleteQrPassSheet(
    user: UserProfile,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        scrimColor = androidx.compose.ui.graphics.Color(0xFF0B1220).copy(alpha = .48f),

        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFFF5F6F8),
        dragHandle = { MPremiumSheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Üst Başlık & Kapat Butonu
            MSheetHeading(
                title = "Dijital sporcu kartı",
                subtitle = "${user.name} · QR geçiş önizlemesi",
                icon = Icons.Rounded.QrCode2,
                onDismiss = onDismiss
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Dijital profil kartı önizlemesi
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                shadowElevation = 8.dp,
                border = BorderStroke(1.5.dp, MColors.Brand.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Profil Başlığı
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(MColors.Brand, Color(0xFFF97316))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(user.initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = user.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MColors.Ink900
                                )
                                Text(
                                    text = "${user.city} · ${user.area}",
                                    fontSize = 11.sp,
                                    color = MColors.Ink500
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (VerificationLevel.PHOTO in user.verifications) Color(0xFFF0FDF4) else MColors.SurfaceMuted,
                            border = BorderStroke(1.dp, if (VerificationLevel.PHOTO in user.verifications) MColors.Mint.copy(alpha = 0.5f) else MColors.Line)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (VerificationLevel.PHOTO in user.verifications) Icons.Rounded.Verified else Icons.Rounded.Info,
                                    null,
                                    tint = if (VerificationLevel.PHOTO in user.verifications) MColors.Mint else MColors.Ink500,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (VerificationLevel.PHOTO in user.verifications) "FOTOĞRAF ONAYLI" else "DOĞRULAMA BEKLİYOR",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (VerificationLevel.PHOTO in user.verifications) MColors.Mint else MColors.Ink600
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 2. BÜYÜK QR KOD ALANI (Hiçbir Yazı İle Çakışmayan Temiz Beyaz Alan)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(2.dp, Color(0x15000000)),
                        modifier = Modifier.size(190.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.QrCode2,
                                contentDescription = "Örnek QR görünümü",
                                tint = MColors.Ink900,
                                modifier = Modifier.size(165.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("QR önizlemesi · demo", fontSize = 10.sp, color = MColors.Ink500)

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. ÇİZGİSEL BARKOD ALANI
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(30.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF8FAFC))
                                .padding(horizontal = 16.dp)
                        ) {
                            val bars = listOf(3, 1, 2, 4, 1, 3, 2, 1, 4, 2, 1, 3, 1, 2, 4, 1, 2, 3, 1, 4, 2, 1, 3, 2)
                            bars.forEach { w ->
                                Box(
                                    modifier = Modifier
                                        .width(w.dp)
                                        .fillMaxHeight(0.85f)
                                        .background(MColors.Ink900)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "DEMO · ${user.area.uppercase()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp,
                            color = MColors.Ink700
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. METRİKLER (3 Temiz Sütun)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Güvenilirlik", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MColors.Ink500)
                                Text("%${user.reliability}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MColors.Mint)
                            }
                            Box(modifier = Modifier.height(18.dp).width(1.dp).background(Color(0x15000000)))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Maç", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MColors.Ink500)
                                Text("${user.verifiedMatches}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MColors.Ink900)
                            }
                            Box(modifier = Modifier.height(18.dp).width(1.dp).background(Color(0x15000000)))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Spor Ahlakı", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MColors.Ink500)
                                Text("${user.fairPlay}★", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MColors.Gold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tamam / Kapat Butonu
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MColors.Brand,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Önizlemeyi kapat",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
