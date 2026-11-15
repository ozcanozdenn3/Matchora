package com.matchora.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.matchora.app.data.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

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
    val backend = LocalBackendRepository.current
    val backendWire = if (backend != null) backend.snapshots.collectAsState().value else null
    val avatarUploadId = backendWire?.o("profile")?.s("avatarUploadId")?.takeIf { it.isNotBlank() && it != "null" }
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showVerifyInput by remember { mutableStateOf(false) }
    var verifyCodeInput by remember { mutableStateOf("") }
    var verifyResult by remember { mutableStateOf<String?>(null) }
    fun refresh() { if (backend != null) scope.launch {
        try { code = backend.command("passport.code", apiPayload(), backend.requestKey("passport-code")).s("code"); error = null }
        catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
        catch (e: Exception) { error = e.message }
    } }
    LaunchedEffect(backend) { if (backend != null) { refresh(); while (true) { delay(10 * 60_000L); refresh() } } }
    val qr = remember(code) { if (code.isBlank() || backend == null) null else makeQrCode("${backend.baseUrl}/v1/passports/verify?code=$code") }
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
                subtitle = "${user.name} · ${if (backend == null) "QR geçiş önizlemesi" else "Sporcu kartı"}",
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
                                if (user.avatar != null) {
                                    Image(user.avatar, user.name, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                                } else if (avatarUploadId != null) {
                                    BackendPhoto(avatarUploadId, user.name, Modifier.fillMaxSize())
                                } else {
                                    Text(user.initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                }
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
                            if (backend != null) {
                                if (qr != null) Image(qr, "Süreli sporcu kartı QR kodu", Modifier.size(165.dp))
                                else CircularProgressIndicator(color = MColors.Brand)
                            } else Icon(
                                imageVector = Icons.Rounded.QrCode2,
                                contentDescription = "Örnek QR görünümü",
                                tint = MColors.Ink900,
                                modifier = Modifier.size(165.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(if (backend == null) "QR önizlemesi · demo" else "15 dakika geçerli · kimlik belgesi yerine geçmez", fontSize = 10.sp, color = MColors.Ink500)

                    Spacer(modifier = Modifier.height(14.dp))

                    if (error != null) TextButton(onClick = ::refresh) { Text("${error} · Tekrar dene", color = MColors.Rose) }
                    // 3. ÇİZGİSEL BARKOD ALANI
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (backend == null) Row(
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
                            text = if (backend == null) "DEMO · ${user.area.uppercase()}" else "ID · ${backend.userId.orEmpty().takeLast(8).uppercase()}",
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
                                Text(if (user.fairPlay > 0) "${user.fairPlay}★" else "—", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MColors.Gold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (backend != null) {
                MPillActionButton(
                    text = if (showVerifyInput) "Kod doğrulamayı gizle" else "Başka sporcu kartını doğrula",
                    icon = Icons.Rounded.QrCodeScanner,
                    tone = MPillTone.NEUTRAL,
                    compact = true,
                    onClick = { showVerifyInput = !showVerifyInput }
                )
                if (showVerifyInput) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MPremiumTextField(
                            value = verifyCodeInput,
                            onValueChange = { verifyCodeInput = it.trim() },
                            label = "Pasaport kodu",
                            modifier = Modifier.weight(1f)
                        )
                        MPillActionButton(
                            text = "Doğrula",
                            icon = Icons.Rounded.VerifiedUser,
                            tone = MPillTone.BRAND,
                            enabled = verifyCodeInput.isNotBlank(),
                            onClick = {
                                scope.launch {
                                    try {
                                        val res = backend.verifyPassport(verifyCodeInput)
                                        val p = res.o("passport")
                                        verifyResult = "✓ Doğrulandı: ${p.s("name")} (${p.s("city")} · Güvenilirlik %${p.i("reliability")})"
                                    } catch (e: Exception) {
                                        verifyResult = (e as? com.matchora.app.data.BackendFailure)?.message ?: "Kod doğrulanamadı."
                                    }
                                }
                            }
                        )
                    }
                    verifyResult?.let {
                        Text(it, fontSize = 11.5.sp, color = if (it.startsWith("✓")) MColors.Mint else MColors.Rose, modifier = Modifier.padding(bottom = 8.dp))
                    }
                }
            }

            // Tamam / Kapat Butonu
            MButton(
                text = if (backend == null) "Önizlemeyi kapat" else "Kapat",
                onClick = onDismiss,
                icon = Icons.Rounded.Check
            )
        }
    }
}
