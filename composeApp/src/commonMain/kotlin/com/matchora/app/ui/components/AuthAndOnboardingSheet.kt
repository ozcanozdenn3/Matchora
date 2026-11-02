package com.matchora.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.domain.Sport
import com.matchora.app.state.*
import com.matchora.app.ui.theme.MColors

enum class AuthStage {
    LOGIN_OPTIONS,
    PHONE_INPUT,
    PHONE_OTP,
    ONBOARDING_STEP1_SPORT_LOCATION,
    ONBOARDING_STEP2_POSITION_LEVEL,
    ONBOARDING_STEP3_IDENTITY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthAndOnboardingSheet(
    onDismiss: () -> Unit,
    initialStage: AuthStage = AuthStage.LOGIN_OPTIONS,
    currentCity: String = "İstanbul",
    currentDistrict: String = "Kadıköy",
    onLoginSuccess: (provider: String) -> Unit,
    onCompleteOnboarding: (UserProfile) -> Unit,
    onContinueAsGuest: () -> Unit
) {
    var stage by remember { mutableStateOf(initialStage) }
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }

    // Onboarding Form State
    var selectedSport by remember { mutableStateOf(Sport.FOOTBALL) }
    var selectedCity by remember { mutableStateOf(currentCity) }
    var selectedDistrict by remember { mutableStateOf(currentDistrict) }
    var selectedPosition by remember { mutableStateOf("Orta Saha") }
    var selectedLevel by remember { mutableStateOf(SkillLevel.INTERMEDIATE) }
    var athleteName by remember { mutableStateOf("") }
    var dominantFoot by remember { mutableStateOf("Sağ") }
    var jerseyNumber by remember { mutableStateOf("10") }

    val sportPositions: List<String> = remember(selectedSport) {
        when (selectedSport) {
            Sport.FOOTBALL -> listOf("Kaleci 🧤", "Defans 🛡️", "Orta Saha ⚡", "Forvet / Kanat 🎯")
            Sport.TENNIS, Sport.PADEL -> listOf("Tekler 🎾", "Çiftler 👥", "Sağ Kanat", "Sol Kanat")
            Sport.BASKETBALL -> listOf("Oyun Kurucu (PG)", "Şutör Gard (SG)", "Forvet (SF/PF)", "Pivot (C)")
            Sport.VOLLEYBALL -> listOf("Pasör", "Smaçör", "Orta Oyuncu", "Libero")
            else -> listOf("Genel / Çok Yönlü ⚡", "Hücum / Forvet 🎯", "Savunma / Defans 🛡️", "Oyun Kurucu 🧠")
        }
    }

    LaunchedEffect(selectedSport) {
        selectedPosition = sportPositions.first().substringBefore(" ")
    }

    ModalBottomSheet(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        scrimColor = Color(0xFF0B1220).copy(alpha = .52f),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF7F8FA),
        dragHandle = { MPremiumSheetHandle() },
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
                            athleteName = "Özcan Özden"
                            stage = AuthStage.ONBOARDING_STEP1_SPORT_LOCATION
                        },
                        onSelectGoogle = {
                            athleteName = "Özcan Özden"
                            stage = AuthStage.ONBOARDING_STEP1_SPORT_LOCATION
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
                            athleteName = "Sporcu"
                            stage = AuthStage.ONBOARDING_STEP1_SPORT_LOCATION
                        }
                    )
                }

                AuthStage.ONBOARDING_STEP1_SPORT_LOCATION -> {
                    OnboardingStep1View(
                        selectedSport = selectedSport,
                        onSportSelect = { selectedSport = it },
                        selectedCity = selectedCity,
                        selectedDistrict = selectedDistrict,
                        onCityChange = { c, d ->
                            selectedCity = c
                            selectedDistrict = d
                        },
                        onNext = { stage = AuthStage.ONBOARDING_STEP2_POSITION_LEVEL },
                        onBack = { stage = AuthStage.LOGIN_OPTIONS }
                    )
                }

                AuthStage.ONBOARDING_STEP2_POSITION_LEVEL -> {
                    OnboardingStep2View(
                        sport = selectedSport,
                        positions = sportPositions,
                        selectedPosition = selectedPosition,
                        onPositionSelect = { selectedPosition = it.substringBefore(" ") },
                        selectedLevel = selectedLevel,
                        onLevelSelect = { selectedLevel = it },
                        onNext = { stage = AuthStage.ONBOARDING_STEP3_IDENTITY },
                        onBack = { stage = AuthStage.ONBOARDING_STEP1_SPORT_LOCATION }
                    )
                }

                AuthStage.ONBOARDING_STEP3_IDENTITY -> {
                    OnboardingStep3View(
                        name = athleteName,
                        onNameChange = { athleteName = it },
                        dominantFoot = dominantFoot,
                        onFootSelect = { dominantFoot = it },
                        jerseyNumber = jerseyNumber,
                        onJerseyNumberChange = { jerseyNumber = it },
                        onFinish = {
                            val newProfile = DemoData.me.copy(
                                name = athleteName.trim().ifBlank { "Yeni Sporcu" },
                                city = selectedCity,
                                area = selectedDistrict,
                                dominantSide = "$dominantFoot Ayak · No:$jerseyNumber",
                                skills = listOf(
                                    SportSkill(
                                        sport = selectedSport,
                                        level = selectedLevel,
                                        rating = when (selectedLevel) {
                                            SkillLevel.BEGINNER -> 1100
                                            SkillLevel.INTERMEDIATE -> 1350
                                            SkillLevel.ADVANCED -> 1550
                                            SkillLevel.PRO -> 1800
                                        },
                                        verifiedMatches = 1,
                                        winRate = 60,
                                        localRank = 28,
                                        position = selectedPosition
                                    )
                                )
                            )
                            onCompleteOnboarding(newProfile)
                            onDismiss()
                        },
                        onBack = { stage = AuthStage.ONBOARDING_STEP2_POSITION_LEVEL }
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 1. GİRİŞ VE KAYIT SEÇENEKLERİ (APPLE, GOOGLE, TELEFON, MİSAFİR)
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
            .padding(bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Logo & Üst Başlık
        Box(
            modifier = Modifier
                .size(54.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x350B1220), spotColor = Color(0x500B1220))
                .background(
                    Brush.linearGradient(listOf(Color(0xFF283548), MColors.Ink900)),
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.SportsSoccer,
                contentDescription = null,
                tint = Color(0xFFFFB37B),
                modifier = Modifier.size(30.dp)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Matchora'ya Katıl",
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MColors.Ink900
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Akıllı maç bul, kadrolara katıl ve sporcu pasaportunu oluştur",
                fontSize = 12.5.sp,
                color = MColors.Ink600,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp
            )
        }

        Spacer(Modifier.height(2.dp))

        // 1. Apple ile Giriş Yap (App Store standartlarında siyah buton)
        Surface(
            onClick = onSelectApple,
            shape = RoundedCornerShape(15.dp),
            color = Color.Black,
            shadowElevation = 5.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(49.dp)
                .shadow(6.dp, RoundedCornerShape(15.dp), ambientColor = Color(0x30000000), spotColor = Color(0x50000000))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
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

        // 2. Google ile Giriş Yap (Temiz beyaz buton)
        Surface(
            onClick = onSelectGoogle,
            shape = RoundedCornerShape(15.dp),
            color = Color.White,
            border = BorderStroke(1.2.dp, Color(0xFFDCE3ED)),
            shadowElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(49.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
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
            shape = RoundedCornerShape(15.dp),
            color = MColors.Ink900,
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, Color(0xFFFFB37B).copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(49.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(Color(0xFF263346), MColors.Ink900)))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhoneIphone,
                    contentDescription = null,
                    tint = Color(0xFFFFB37B),
                    modifier = Modifier.size(19.dp)
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

        // Ayırıcı ve Misafir Modu
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

        TextButton(
            onClick = onContinueAsGuest,
            modifier = Modifier.height(36.dp),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
        ) {
            Text(
                text = "Şimdilik giriş yapmadan keşfet →",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MColors.BrandDeep
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
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Geri", tint = MColors.Ink900)
            }
            Text("Telefon ile Giriş", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MColors.Ink900)
        }

        Text(
            text = "Doğrulama kodu gönderebilmemiz için telefon numaranı gir. Sahada güvenilir eşleşme için numara onayı gereklidir.",
            fontSize = 13.sp,
            color = MColors.Ink600,
            lineHeight = 18.sp
        )

        MFloatingInputBox(
            value = phoneNumber,
            onValueChange = onPhoneChange,
            label = "Telefon Numarası",
            placeholder = "5XX XXX XX XX",
            icon = Icons.Rounded.Phone,
            singleLine = true
        )

        MButton(
            text = "Doğrulama Kodu Gönder",
            onClick = onSendCode,
            enabled = phoneNumber.trim().length >= 10,
            icon = Icons.Rounded.Send
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Geri", tint = MColors.Ink900)
            }
            Text("Kodu Onayla", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MColors.Ink900)
        }

        Text(
            text = "+90 $phoneNumber numarasına 4 haneli SMS doğrulama kodu gönderildi.",
            fontSize = 13.sp,
            color = MColors.Ink600
        )

        MFloatingInputBox(
            value = otpCode,
            onValueChange = { if (it.length <= 6) onOtpChange(it) },
            label = "SMS Kodu (Demo: 2026)",
            placeholder = "2026",
            icon = Icons.Rounded.Key,
            singleLine = true
        )

        MButton(
            text = "Giriş Yap ve Pasaportunu Kur",
            onClick = onVerify,
            enabled = otpCode.length >= 4,
            icon = Icons.Rounded.Check
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 4. ONBOARDING ADIM 1: SPOR VE KONUM SEÇİMİ
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun OnboardingStep1View(
    selectedSport: Sport,
    onSportSelect: (Sport) -> Unit,
    selectedCity: String,
    selectedDistrict: String,
    onCityChange: (String, String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OnboardingProgressBar(step = 1, totalSteps = 3, title = "Ana Branşın & Konumun")

        Text(
            text = "En çok hangi sporu oynuyorsun? Sana özel radar ve maç akışı buna göre ayarlanacak.",
            fontSize = 13.sp,
            color = MColors.Ink600
        )

        // Spor Seçim Kartları
        val sports = listOf(
            Sport.FOOTBALL to "Futbol",
            Sport.TENNIS to "Tenis",
            Sport.PADEL to "Padel",
            Sport.BASKETBALL to "Basketbol",
            Sport.VOLLEYBALL to "Voleybol"
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            sports.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { (sp, label) ->
                        val isSelected = sp == selectedSport
                        Surface(
                            onClick = { onSportSelect(sp) },
                            shape = RoundedCornerShape(15.dp),
                            color = if (isSelected) MColors.Ink900 else Color.White,
                            border = BorderStroke(1.2.dp, if (isSelected) Color(0xFFFFB37B) else Color(0xFFE2E8F0)),
                            shadowElevation = if (isSelected) 5.dp else 2.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(sp.emoji, fontSize = 22.sp)
                                Text(
                                    text = label,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MColors.Ink900
                                )
                            }
                        }
                    }
                    if (row.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        MFloatingDropdownBox(
            label = "Oynadığın Bölge",
            selectedValue = "$selectedCity · $selectedDistrict",
            options = listOf("İstanbul · Kadıköy", "İstanbul · Beşiktaş", "İstanbul · Ataşehir", "Ankara · Çankaya", "İzmir · Karşıyaka"),
            optionLabel = { it },
            onSelect = {
                val city = it.substringBefore(" · ")
                val dist = it.substringAfter(" · ")
                onCityChange(city, dist)
            },
            icon = Icons.Rounded.LocationOn
        )

        MButton(
            text = "Sonraki Adım: Rol & Seviye →",
            onClick = onNext,
            icon = Icons.Rounded.ArrowForward
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 5. ONBOARDING ADIM 2: MEVKİ & OYUN SEVİYESİ
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun OnboardingStep2View(
    sport: Sport,
    positions: List<String>,
    selectedPosition: String,
    onPositionSelect: (String) -> Unit,
    selectedLevel: SkillLevel,
    onLevelSelect: (SkillLevel) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OnboardingProgressBar(step = 2, totalSteps = 3, title = "Mevki & Oyun Seviyen")

        Text(
            text = "Sahadaki yerin ve seviyen doğru takımlarla eşleşmeni sağlar.",
            fontSize = 13.sp,
            color = MColors.Ink600
        )

        Text("Sahadaki Mevkin", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MColors.Ink900)

        // Mevki Seçenekleri
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            positions.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { pos ->
                        val posKey = pos.substringBefore(" ")
                        val isSelected = posKey == selectedPosition
                        Surface(
                            onClick = { onPositionSelect(pos) },
                            shape = RoundedCornerShape(13.dp),
                            color = if (isSelected) MColors.BrandDeep else Color.White,
                            border = BorderStroke(1.dp, if (isSelected) MColors.BrandDeep else Color(0xFFDCE3ED)),
                            shadowElevation = if (isSelected) 4.dp else 1.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = pos,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MColors.Ink800
                                )
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text("Oyun Seviyen", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MColors.Ink900)

        val levels = listOf(
            SkillLevel.BEGINNER to ("Başlangıç" to "Keyif amaçlı oynuyorum"),
            SkillLevel.INTERMEDIATE to ("Orta Seviye" to "Ritim ve kondisyonum iyi"),
            SkillLevel.ADVANCED to ("İleri Seviye" to "Rekabetçi ve taktiksel oynarım"),
            SkillLevel.PRO to ("Yarı-Pro / Lisanslı" to "Turnuva ve lig tecrübem var")
        )

        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            levels.forEach { (lvl, texts) ->
                val isSelected = lvl == selectedLevel
                Surface(
                    onClick = { onLevelSelect(lvl) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) MColors.Ink900 else Color.White,
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFFFFB37B) else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = texts.first,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MColors.Ink900
                            )
                            Text(
                                text = texts.second,
                                fontSize = 11.sp,
                                color = if (isSelected) Color(0xFFFFB37B) else MColors.Ink500
                            )
                        }
                        if (isSelected) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFFFFB37B), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        MButton(
            text = "Sonraki Adım: Kimlik Bilgileri →",
            onClick = onNext,
            icon = Icons.Rounded.ArrowForward
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 6. ONBOARDING ADIM 3: SPORCU KİMLİĞİ VE HOŞ GELDİN ÖDÜLÜ
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun OnboardingStep3View(
    name: String,
    onNameChange: (String) -> Unit,
    dominantFoot: String,
    onFootSelect: (String) -> Unit,
    jerseyNumber: String,
    onJerseyNumberChange: (String) -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OnboardingProgressBar(step = 3, totalSteps = 3, title = "Sporcu Pasaportu Kimliği")

        MFloatingInputBox(
            value = name,
            onValueChange = onNameChange,
            label = "Ad Soyad",
            placeholder = "Özcan Özden",
            icon = Icons.Rounded.Person,
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.weight(1f)) {
                MFloatingDropdownBox(
                    label = "Tercih Ayak/El",
                    selectedValue = dominantFoot,
                    options = listOf("Sağ", "Sol", "Her İkisi"),
                    optionLabel = { "$it Ayak" },
                    onSelect = onFootSelect,
                    icon = Icons.Rounded.Sports
                )
            }
            Box(Modifier.weight(1f)) {
                MFloatingInputBox(
                    value = jerseyNumber,
                    onValueChange = { if (it.length <= 2) onJerseyNumberChange(it) },
                    label = "Forma No",
                    placeholder = "10",
                    icon = Icons.Rounded.Pin,
                    singleLine = true
                )
            }
        }

        // Hoş Geldin Token Hediyesi Kutusu
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFFFF7ED),
            border = BorderStroke(1.2.dp, Color(0xFFFFB37B)),
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x20FF8800), spotColor = Color(0x30FF8800))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(Brush.linearGradient(listOf(Color(0xFFFF944D), Color(0xFFEA580C))), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎁", fontSize = 22.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "+20 TKN Hoş Geldin Bonusu!",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF9A3412)
                    )
                    Text(
                        text = "Sporcu pasaportunu tamamladığın için token bakiyene anında eklendi.",
                        fontSize = 11.5.sp,
                        color = Color(0xFFC2410C),
                        lineHeight = 15.sp
                    )
                }
            }
        }

        MButton(
            text = "Pasaportu Oluştur ve Sahaya Çık 🚀",
            onClick = onFinish,
            enabled = name.trim().isNotBlank(),
            icon = Icons.Rounded.Check
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// ONBOARDING PROGRESS BAR
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun OnboardingProgressBar(step: Int, totalSteps: Int, title: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MColors.Ink900
            )
            Text(
                text = "$step / $totalSteps",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MColors.BrandDeep
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (i in 1..totalSteps) {
                val isFilled = i <= step
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isFilled) MColors.BrandDeep else Color(0xFFE2E8F0))
                )
            }
        }
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
        val radius = (size.minDimension - stroke) / 2f

        // 4 Renkli Google 'G' çizimi
        drawArc(Color(0xFFEA4335), 210f, 100f, false, style = Stroke(stroke, cap = StrokeCap.Round))
        drawArc(Color(0xFF4285F4), 310f, 100f, false, style = Stroke(stroke, cap = StrokeCap.Round))
        drawArc(Color(0xFF34A853), 50f, 100f, false, style = Stroke(stroke, cap = StrokeCap.Round))
        drawArc(Color(0xFFFBBC05), 150f, 60f, false, style = Stroke(stroke, cap = StrokeCap.Round))
        drawLine(Color(0xFF4285F4), center, Offset(size.width * 0.94f, center.y), stroke, StrokeCap.Round)
    }
}
