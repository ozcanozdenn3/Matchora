package com.matchora.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.ui.theme.MColors
import com.matchora.app.ui.theme.MGradients
import kotlinx.coroutines.delay

enum class AuthStage {
    LOGIN_OPTIONS,
    PHONE_INPUT,
    PHONE_OTP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthAndOnboardingSheet(
    onDismiss: () -> Unit,
    initialStage: AuthStage = AuthStage.LOGIN_OPTIONS,
    onLoginSuccess: (provider: String, name: String) -> Unit,
    onContinueAsGuest: () -> Unit
) {
    var stage by remember { mutableStateOf(initialStage) }
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }

    ModalBottomSheet(
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        tonalElevation = 0.dp,
        scrimColor = Color(0xFF0B1220).copy(alpha = .56f),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF8FAFC),
        dragHandle = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color(0xFFCBD5E1), RoundedCornerShape(2.dp))
                )
            }
        },
        modifier = Modifier.statusBarsPadding().padding(top = 8.dp)
    ) {
        AnimatedContent(
            targetState = stage,
            transitionSpec = {
                fadeIn(tween(180)) togetherWith fadeOut(tween(140))
            },
            label = "auth_stage_transition"
        ) { currentStage ->
            when (currentStage) {
                AuthStage.LOGIN_OPTIONS -> {
                    LoginOptionsView(
                        onSelectApple = {
                            onLoginSuccess("Apple ID", "Özcan Özden")
                            onDismiss()
                        },
                        onSelectGoogle = {
                            onLoginSuccess("Google", "Özcan Özden")
                            onDismiss()
                        },
                        onSelectPhone = { stage = AuthStage.PHONE_INPUT },
                        onContinueAsGuest = {
                            onContinueAsGuest()
                            onDismiss()
                        },
                        onDismiss = onDismiss
                    )
                }

                AuthStage.PHONE_INPUT -> {
                    PhoneInputView(
                        phoneNumber = phoneNumber,
                        onPhoneChange = { phoneNumber = it },
                        onBack = { stage = AuthStage.LOGIN_OPTIONS },
                        onSendCode = {
                            if (phoneNumber.length >= 10) {
                                stage = AuthStage.PHONE_OTP
                            }
                        }
                    )
                }

                AuthStage.PHONE_OTP -> {
                    PhoneOtpView(
                        phoneNumber = phoneNumber,
                        otpCode = otpCode,
                        onOtpChange = { otpCode = it },
                        onBack = { stage = AuthStage.PHONE_INPUT },
                        onVerify = {
                            onLoginSuccess("Telefon Numarası", "Özcan Özden")
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 1. TEK VE NET GİRİŞ EKRANI (OTOMATİK PASAPORT OLUŞTURMA İLE)
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun LoginOptionsView(
    onSelectApple: () -> Unit,
    onSelectGoogle: () -> Unit,
    onSelectPhone: () -> Unit,
    onContinueAsGuest: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Üst Başlık ve Kapatma İkonu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(MGradients.Brand, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SportsSoccer,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Text(
                    text = "MATCHORA",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                    color = MColors.Ink900
                )
            }

            Surface(
                onClick = onDismiss,
                shape = CircleShape,
                color = Color(0xFFE2E8F0),
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Close, "Kapat", tint = MColors.Ink700, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Başlık ve Açıklama
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "Matchora'ya Giriş Yap",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MColors.Ink900
            )
            Text(
                text = "Giriş yaptığında sporcu pasaportun ve başlangıç bakiyen otomatik hazırlanır.",
                fontSize = 12.5.sp,
                color = MColors.Ink600,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp
            )
        }

        // Hediye Token Çağrısı
        Surface(
            shape = RoundedCornerShape(13.dp),
            color = Color(0xFFFFF7ED),
            border = BorderStroke(1.dp, Color(0xFFFFB37B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text("🎁", fontSize = 18.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "+20 TKN Hoş Geldin Bonusu Tanımlanır",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9A3412)
                    )
                    Text(
                        text = "İlk girişte pasaportunla birlikte cüzdanına aktarılır.",
                        fontSize = 11.sp,
                        color = Color(0xFFC2410C)
                    )
                }
            }
        }

        Spacer(Modifier.height(2.dp))

        // 1. Apple ile Giriş Yap (App Store standart siyah buton)
        Surface(
            onClick = onSelectApple,
            shape = RoundedCornerShape(14.dp),
            color = Color.Black,
            shadowElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                AppleLogoIcon(modifier = Modifier.size(19.dp), tint = Color.White)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Apple ile Giriş Yap",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // 2. Google ile Giriş Yap
        Surface(
            onClick = onSelectGoogle,
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            border = BorderStroke(1.2.dp, Color(0xFFDCE3ED)),
            shadowElevation = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                GoogleLogoIcon(modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Google ile Devam Et",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MColors.Ink900
                )
            }
        }

        // 3. Telefon Numarası ile Giriş
        Surface(
            onClick = onSelectPhone,
            shape = RoundedCornerShape(14.dp),
            color = MColors.Ink900,
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhoneIphone,
                    contentDescription = null,
                    tint = Color(0xFFFFB37B),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Telefon Numarası ile Giriş",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Ayırıcı
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
            Text(
                text = "veya",
                fontSize = 11.sp,
                color = MColors.Ink400,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
        }

        // Misafir Olarak Devam Et
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TextButton(
                onClick = onContinueAsGuest,
                modifier = Modifier.height(34.dp),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
            ) {
                Text(
                    text = "Şimdilik giriş yapmadan keşfet →",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MColors.BrandDeep
                )
            }
            Text(
                text = "Tüm maçları ve sahaları serbestçe inceleyebilirsin",
                fontSize = 10.5.sp,
                color = MColors.Ink500
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 2. TELEFON NUMARASI GİRİŞ EKRANI
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun PhoneInputView(
    phoneNumber: String,
    onPhoneChange: (String) -> Unit,
    onBack: () -> Unit,
    onSendCode: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Geri", tint = MColors.Ink900)
            }
            Text(
                text = "Telefon Numarası ile Giriş",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MColors.Ink900
            )
        }

        Text(
            text = "Doğrulama kodu gönderebilmemiz için telefon numaranı gir. Giriş yaptığında sporcu pasaportun hazır olacaktır.",
            fontSize = 12.5.sp,
            color = MColors.Ink600,
            lineHeight = 17.sp
        )

        // Telefon Input Kartı
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFDCE3ED)),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = "🇹🇷 +90",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MColors.Ink800,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }

                TextField(
                    value = phoneNumber,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }.take(10)
                        onPhoneChange(digits)
                    },
                    placeholder = { Text("5XX XXX XX XX", fontSize = 14.sp, color = MColors.Ink400) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.weight(1f)
                )

                if (phoneNumber.isNotEmpty()) {
                    IconButton(onClick = { onPhoneChange("") }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Rounded.Cancel, null, tint = MColors.Ink400, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        MButton(
            text = "Doğrulama Kodu Gönder 💬",
            onClick = onSendCode,
            enabled = phoneNumber.length >= 10,
            icon = Icons.AutoMirrored.Rounded.Send
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 3. SMS OTP ONAY EKRANI
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun PhoneOtpView(
    phoneNumber: String,
    otpCode: String,
    onOtpChange: (String) -> Unit,
    onBack: () -> Unit,
    onVerify: () -> Unit
) {
    var timeLeft by remember { mutableIntStateOf(50) }
    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Geri", tint = MColors.Ink900)
            }
            Text(
                text = "SMS Kodunu Onayla",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MColors.Ink900
            )
        }

        Text(
            text = "+90 $phoneNumber numarana gelen 4 haneli SMS onay kodunu gir. (Demo Kodu: 2026)",
            fontSize = 12.5.sp,
            color = MColors.Ink600,
            lineHeight = 17.sp
        )

        // 4 Haneli Görsel OTP Kutuları
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (i in 0..3) {
                    val char = otpCode.getOrNull(i)?.toString() ?: ""
                    val isCurrent = (otpCode.length == i)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(
                            if (isCurrent) 1.8.dp else 1.dp,
                            if (isCurrent) MColors.BrandDeep else Color(0xFFCBD5E1)
                        ),
                        shadowElevation = 1.dp,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = char,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MColors.Ink900
                            )
                        }
                    }
                }
            }
        }

        // Gizli Klavye Girişi
        OutlinedTextField(
            value = otpCode,
            onValueChange = { input ->
                val digits = input.filter { it.isDigit() }.take(4)
                onOtpChange(digits)
            },
            placeholder = { Text("Kodu buraya yaz (2026)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (timeLeft > 0) "Tekrar kod gönder ($timeLeft sn)" else "Tekrar kod gönder",
                fontSize = 12.sp,
                color = if (timeLeft > 0) MColors.Ink500 else MColors.BrandDeep,
                fontWeight = if (timeLeft == 0) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.clickable(enabled = timeLeft == 0) { timeLeft = 50 }
            )
            Text(
                text = "Hızlı Doldur (2026)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MColors.BrandDeep,
                modifier = Modifier.clickable { onOtpChange("2026") }
            )
        }

        MButton(
            text = "Kodu Onayla & Sahaya Bağlan ⚡",
            onClick = onVerify,
            enabled = otpCode.length >= 4,
            icon = Icons.Rounded.Check
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// APPLE & GOOGLE LOGO İKONLARI
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun AppleLogoIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text("", fontSize = 21.sp, color = tint, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GoogleLogoIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.22f
        val center = Offset(size.width / 2f, size.height / 2f)

        // 4 Renkli Google 'G' çizimi
        drawArc(Color(0xFFEA4335), 210f, 100f, false, style = Stroke(stroke, cap = StrokeCap.Round))
        drawArc(Color(0xFF4285F4), 310f, 100f, false, style = Stroke(stroke, cap = StrokeCap.Round))
        drawArc(Color(0xFF34A853), 50f, 100f, false, style = Stroke(stroke, cap = StrokeCap.Round))
        drawArc(Color(0xFFFBBC05), 150f, 60f, false, style = Stroke(stroke, cap = StrokeCap.Round))
        drawLine(Color(0xFF4285F4), center, Offset(size.width * 0.94f, center.y), stroke, StrokeCap.Round)
    }
}
