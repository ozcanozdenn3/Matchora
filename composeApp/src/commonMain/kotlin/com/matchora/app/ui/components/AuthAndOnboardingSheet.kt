package com.matchora.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.domain.Sport
import com.matchora.app.state.*
import com.matchora.app.ui.theme.MColors
import com.matchora.app.ui.theme.MGradients
import kotlinx.coroutines.delay

enum class AuthStage {
    LOGIN_OPTIONS,
    PHONE_INPUT,
    PHONE_OTP,
    ONBOARDING_STEP1_SPORT_LOCATION,
    ONBOARDING_STEP2_POSITION_LEVEL,
    ONBOARDING_STEP3_IDENTITY
}

private enum class AuthTab(val label: String) {
    LOGIN("Giriş Yap"),
    REGISTER("Pasaport Oluştur")
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
    var activeTab by remember { mutableStateOf(AuthTab.LOGIN) }
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }

    // Onboarding Form State
    var selectedSport by remember { mutableStateOf(Sport.FOOTBALL) }
    var selectedCity by remember { mutableStateOf(currentCity) }
    var selectedDistrict by remember { mutableStateOf(currentDistrict) }
    var selectedPosition by remember { mutableStateOf("Orta Saha") }
    var selectedLevel by remember { mutableStateOf(SkillLevel.INTERMEDIATE) }
    var athleteName by remember { mutableStateOf("Özcan Özden") }
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
                        .width(42.dp)
                        .height(4.5.dp)
                        .background(Color(0xFFCBD5E1), RoundedCornerShape(3.dp))
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
                        activeTab = activeTab,
                        onTabChange = { activeTab = it },
                        onQuickLogin = { provider ->
                            onLoginSuccess(provider)
                            onDismiss()
                        },
                        onStartRegisterWithApple = {
                            athleteName = "Özcan Özden"
                            stage = AuthStage.ONBOARDING_STEP1_SPORT_LOCATION
                        },
                        onStartRegisterWithGoogle = {
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
                        isRegister = (activeTab == AuthTab.REGISTER),
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
                        isRegister = (activeTab == AuthTab.REGISTER),
                        phoneNumber = phoneNumber,
                        otpCode = otpCode,
                        onOtpChange = { otpCode = it },
                        onBack = { stage = AuthStage.PHONE_INPUT },
                        onVerify = {
                            if (activeTab == AuthTab.LOGIN) {
                                onLoginSuccess("Telefon Numarası (SMS)")
                                onDismiss()
                            } else {
                                athleteName = "Sporcu"
                                stage = AuthStage.ONBOARDING_STEP1_SPORT_LOCATION
                            }
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
                        selectedSport = selectedSport,
                        selectedPosition = selectedPosition,
                        selectedLevel = selectedLevel,
                        dominantFoot = dominantFoot,
                        onFootSelect = { dominantFoot = it },
                        jerseyNumber = jerseyNumber,
                        onJerseyNumberChange = { jerseyNumber = it },
                        onFinish = {
                            val newProfile = DemoData.me.copy(
                                name = athleteName.ifBlank { "Yeni Sporcu" },
                                city = selectedCity,
                                area = selectedDistrict,
                                dominantSide = "$dominantFoot Ayak",
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
// 1. GİRİŞ VE KAYIT SEÇENEKLERİ (SEKME VE ÇİFT MODLU TASARIM)
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun LoginOptionsView(
    activeTab: AuthTab,
    onTabChange: (AuthTab) -> Unit,
    onQuickLogin: (provider: String) -> Unit,
    onStartRegisterWithApple: () -> Unit,
    onStartRegisterWithGoogle: () -> Unit,
    onSelectPhone: () -> Unit,
    onContinueAsGuest: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Üst Kapatma ve Başlık Satırı
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MGradients.Brand, RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SportsSoccer,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "MATCHORA",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                        color = MColors.Ink900
                    )
                    Text(
                        text = "Sporcu İşletim Sistemi",
                        fontSize = 10.5.sp,
                        color = MColors.Ink500
                    )
                }
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

        // Segmented Switcher: [ 🔑 Giriş Yap ] | [ 🪪 Pasaport Oluştur (+20 TKN) ]
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFE2E8F0),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(3.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Giriş Yap Sekmesi
                val isLogin = activeTab == AuthTab.LOGIN
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (isLogin) Color.White else Color.Transparent)
                        .clickable { onTabChange(AuthTab.LOGIN) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(
                            Icons.Rounded.Login,
                            contentDescription = null,
                            tint = if (isLogin) MColors.BrandDeep else MColors.Ink500,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Giriş Yap",
                            fontSize = 13.sp,
                            fontWeight = if (isLogin) FontWeight.Bold else FontWeight.Medium,
                            color = if (isLogin) MColors.Ink900 else MColors.Ink600
                        )
                    }
                }

                // Pasaport Oluştur Sekmesi
                val isRegister = activeTab == AuthTab.REGISTER
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (isRegister) Color.White else Color.Transparent)
                        .clickable { onTabChange(AuthTab.REGISTER) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(
                            Icons.Rounded.Badge,
                            contentDescription = null,
                            tint = if (isRegister) MColors.BrandDeep else MColors.Ink500,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Pasaport Oluştur",
                            fontSize = 13.sp,
                            fontWeight = if (isRegister) FontWeight.Bold else FontWeight.Medium,
                            color = if (isRegister) MColors.Ink900 else MColors.Ink600
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFF944D)
                        ) {
                            Text(
                                text = "+20",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }

        if (activeTab == AuthTab.LOGIN) {
            // ──────────────── GİRİŞ YAP MODU ────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Tekrar Hoş Geldin!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MColors.Ink900
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Sporcu pasaportuna, maçlarına ve cüzdanına bağlan",
                        fontSize = 12.5.sp,
                        color = MColors.Ink600,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(2.dp))

                // 1. Apple ile Hızlı Giriş (App Store Standartlarında Siyah Buton)
                Surface(
                    onClick = { onQuickLogin("Apple ID") },
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .shadow(6.dp, RoundedCornerShape(14.dp), ambientColor = Color(0x30000000), spotColor = Color(0x50000000))
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
                    onClick = { onQuickLogin("Google") },
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.2.dp, Color(0xFFDCE3ED)),
                    shadowElevation = 2.dp,
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
                    shadowElevation = 3.dp,
                    border = BorderStroke(1.dp, Color(0xFFFFB37B).copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
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
            }
        } else {
            // ──────────────── PASAPORT OLUŞTUR (KAYIT) MODU ────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Hediye Token Çağrısı
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFFF7ED),
                    border = BorderStroke(1.dp, Color(0xFFFFB37B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(Brush.linearGradient(listOf(Color(0xFFFF944D), Color(0xFFEA580C))), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎁", fontSize = 16.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Kayıt Bonusu: +20 TKN Hediye!",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF9A3412)
                            )
                            Text(
                                text = "Sporcu pasaportunu tamamladığın an cüzdanına tanımlanır.",
                                fontSize = 10.5.sp,
                                color = Color(0xFFC2410C)
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "3 Adımda Pasaportunu Başlat",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MColors.Ink900
                    )
                    Text(
                        text = "Kadıköy maçlarına katıl, yetenek (MMR) puanını kazan",
                        fontSize = 12.sp,
                        color = MColors.Ink600
                    )
                }

                // Apple ile Pasaportu Başlat
                Surface(
                    onClick = onStartRegisterWithApple,
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .shadow(6.dp, RoundedCornerShape(14.dp), ambientColor = Color(0x30000000), spotColor = Color(0x50000000))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        AppleLogoIcon(modifier = Modifier.size(19.dp), tint = Color.White)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Apple ile Hızlı Pasaport Oluştur",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Google ile Pasaportu Başlat
                Surface(
                    onClick = onStartRegisterWithGoogle,
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.2.dp, Color(0xFFDCE3ED)),
                    shadowElevation = 2.dp,
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
                            text = "Google ile Pasaport Oluştur",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MColors.Ink900
                        )
                    }
                }

                // Telefon ile Pasaport Başlat
                Surface(
                    onClick = onSelectPhone,
                    shape = RoundedCornerShape(14.dp),
                    color = MColors.Ink900,
                    shadowElevation = 3.dp,
                    border = BorderStroke(1.dp, Color(0xFFFFB37B).copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
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
                            text = "Telefon Numarası ile Kayıt Ol",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Ayırıcı ve Misafir Modu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp),
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

        // Misafir Modu Butonu
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
    isRegister: Boolean,
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
                text = if (isRegister) "Telefon ile Pasaport Kaydı" else "Telefon Numarası ile Giriş",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MColors.Ink900
            )
        }

        Text(
            text = "Doğrulama kodu gönderebilmemiz için cep telefonu numaranı gir. Sahada güvenilir eşleşme için numara onayı zorunludur.",
            fontSize = 12.5.sp,
            color = MColors.Ink600,
            lineHeight = 17.sp
        )

        // Telefon Input Kartı
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFDCE3ED)),
            shadowElevation = 2.dp,
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
// 3. SMS OTP ONAY EKRANI (6 HANELİ GÖRSEL PIN VE GERİ SAYIM)
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun PhoneOtpView(
    isRegister: Boolean,
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
                text = "SMS Kodunu Doğrula",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MColors.Ink900
            )
        }

        Text(
            text = "+90 $phoneNumber numarana 4 haneli SMS onay kodu gönderildi. (Demo Kodu: 2026)",
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
                        shadowElevation = 2.dp,
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
            text = if (isRegister) "Kodu Doğrula & Pasaporta Geç ➔" else "Giriş Yap ve Sahaya Bağlan ⚡",
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
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OnboardingProgressBar(step = 1, totalSteps = 3, title = "Ana Branşın & Konumun")

        Text(
            text = "Hangi sporu aktif olarak oynuyorsun? Sana özel maç akışı ve semt radarı buna göre oluşturulur.",
            fontSize = 12.5.sp,
            color = MColors.Ink600,
            lineHeight = 17.sp
        )

        // Spor Seçim Izgarası
        val sports = listOf(
            Sport.FOOTBALL to "Futbol ⚽",
            Sport.BASKETBALL to "Basketbol 🏀",
            Sport.TENNIS to "Tenis 🎾",
            Sport.PADEL to "Padel 🎾",
            Sport.VOLLEYBALL to "Voleybol 🏐",
            Sport.RUNNING to "Koşu 🏃",
            Sport.TABLE_TENNIS to "Masa Tenisi 🏓"
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            sports.chunked(2).forEach { pair ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { (sport, label) ->
                        val isSelected = selectedSport == sport
                        Surface(
                            onClick = { onSportSelect(sport) },
                            shape = RoundedCornerShape(13.dp),
                            color = if (isSelected) MColors.Ink900 else Color.White,
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) Color(0xFFFFB37B) else Color(0xFFE2E8F0)
                            ),
                            shadowElevation = if (isSelected) 4.dp else 1.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) Color.White else MColors.Ink900
                                )
                                if (isSelected) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB37B),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // Konum Seçimi
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.LocationOn, null, tint = MColors.BrandDeep, modifier = Modifier.size(20.dp))
                    Column {
                        Text("Oynayacağın Bölge", fontSize = 11.sp, color = MColors.Ink500)
                        Text("$selectedCity · $selectedDistrict", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = MColors.Ink900)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFF7ED),
                    border = BorderStroke(0.8.dp, Color(0xFFFFB37B))
                ) {
                    Text(
                        "GPS Aktif 📍",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC2410C)
                    )
                }
            }
        }

        MButton(
            text = "Sonraki Adım: Mevki & Seviye ➔",
            onClick = onNext,
            icon = Icons.AutoMirrored.Rounded.ArrowForward
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 5. ONBOARDING ADIM 2: MEVKİ VE SEVİYE SEÇİMİ
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
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OnboardingProgressBar(step = 2, totalSteps = 3, title = "Mevki ve Yetenek Seviyesi")

        Text(
            text = "Sahadaki mevkini ve tecrübeni seç. Denk maçlara katılabilmen için MMR puanın buna göre belirlenir.",
            fontSize = 12.5.sp,
            color = MColors.Ink600,
            lineHeight = 17.sp
        )

        // Mevki Seçimi
        Text(
            text = "Sahadaki Rolün / Mevkin",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MColors.Ink900
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(positions) { pos ->
                val isSelected = selectedPosition.equals(pos.substringBefore(" "), ignoreCase = true)
                Surface(
                    onClick = { onPositionSelect(pos) },
                    shape = RoundedCornerShape(11.dp),
                    color = if (isSelected) MColors.BrandDeep else Color.White,
                    border = BorderStroke(1.dp, if (isSelected) MColors.BrandDeep else Color(0xFFCBD5E1)),
                    shadowElevation = if (isSelected) 3.dp else 1.dp
                ) {
                    Text(
                        text = pos,
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else MColors.Ink800,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Seviye Seçimi
        Text(
            text = "Yetenek / Seviye Durumu",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MColors.Ink900
        )

        val levels = listOf(
            SkillLevel.BEGINNER to ("Başlangıç (1.100 MMR)" to "Hobi amaçlı, keyifli oyun"),
            SkillLevel.INTERMEDIATE to ("Orta Seviye (1.350 MMR)" to "Düzenli oynayan, tempolu"),
            SkillLevel.ADVANCED to ("İleri Seviye (1.550 MMR)" to "Taktik bilen, yüksek tempo"),
            SkillLevel.PRO to ("Pro / Lisanslı (1.800 MMR)" to "Turnuva & lig tecrübesi")
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            levels.forEach { (level, texts) ->
                val isSelected = selectedLevel == level
                Surface(
                    onClick = { onLevelSelect(level) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MColors.Ink900 else Color.White,
                    border = BorderStroke(
                        if (isSelected) 1.5.dp else 1.dp,
                        if (isSelected) Color(0xFFFFB37B) else Color(0xFFE2E8F0)
                    ),
                    shadowElevation = if (isSelected) 4.dp else 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = texts.first,
                                fontSize = 13.sp,
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
                            Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFFFFB37B), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        MButton(
            text = "Sonraki Adım: Pasaportunu Tamamla ➔",
            onClick = onNext,
            icon = Icons.AutoMirrored.Rounded.ArrowForward
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 6. ONBOARDING ADIM 3: SPORCU PASAPORTU ÖNİZLEMESİ VE AKTİVASYON
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun OnboardingStep3View(
    name: String,
    onNameChange: (String) -> Unit,
    selectedSport: Sport,
    selectedPosition: String,
    selectedLevel: SkillLevel,
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
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OnboardingProgressBar(step = 3, totalSteps = 3, title = "Sporcu Pasaportu Kimliği")

        // CANLI PASAPORT ÖNİZLEME KARTI
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MColors.Ink900,
            border = BorderStroke(1.2.dp, Brush.linearGradient(listOf(Color(0xFFE1C49D), Color(0xFF40505B)))),
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .background(Brush.linearGradient(listOf(Color(0xFF1E293B), MColors.Ink900)))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "SPORCU PASAPORTU · DİJİTAL KİMLİK",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = Color(0xFFE1C49D)
                    )
                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFFFA500).copy(alpha = 0.2f)) {
                        Text(
                            "#$jerseyNumber",
                            Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFC078)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Brush.linearGradient(listOf(Color(0xFFE1BE87), Color(0xFF88633F))), RoundedCornerShape(13.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name.trim().split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifBlank { "SP" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MColors.Ink900
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name.ifBlank { "Sporcu Adı" },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${selectedSport.name.lowercase().replaceFirstChar { it.uppercase() }} · $selectedPosition · $dominantFoot Ayak",
                            fontSize = 11.5.sp,
                            color = Color(0xFFB8C7CE)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E3A8A)
                    ) {
                        Text(
                            text = when (selectedLevel) {
                                SkillLevel.BEGINNER -> "1.100 MMR"
                                SkillLevel.INTERMEDIATE -> "1.350 MMR"
                                SkillLevel.ADVANCED -> "1.550 MMR"
                                SkillLevel.PRO -> "1.800 MMR"
                            },
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF93C5FD)
                        )
                    }
                }
            }
        }

        MFloatingInputBox(
            value = name,
            onValueChange = onNameChange,
            label = "Sporcu Adı Soyadı",
            placeholder = "Özcan Özden",
            icon = Icons.Rounded.Person,
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    label = "Forma Numarası",
                    placeholder = "10",
                    icon = Icons.Rounded.Pin,
                    singleLine = true
                )
            }
        }

        // Hoş Geldin Token Hediyesi Kutusu
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFFFF7ED),
            border = BorderStroke(1.2.dp, Color(0xFFFFB37B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Brush.linearGradient(listOf(Color(0xFFFF944D), Color(0xFFEA580C))), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎁", fontSize = 18.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "+20 TKN Hoş Geldin Bonusu Tanımlandı!",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF9A3412)
                    )
                    Text(
                        text = "Sporcu pasaportun hazırlandığı an cüzdanına ücretsiz aktarılır.",
                        fontSize = 10.5.sp,
                        color = Color(0xFFC2410C)
                    )
                }
            }
        }

        MButton(
            text = "Pasaportu Aktifleştir & Başla 🚀",
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
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 16.5.sp,
                fontWeight = FontWeight.Bold,
                color = MColors.Ink900
            )
            Text(
                text = "$step / $totalSteps",
                fontSize = 11.5.sp,
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
                        .height(4.5.dp)
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

        // 4 Renkli Google 'G' çizimi
        drawArc(Color(0xFFEA4335), 210f, 100f, false, style = Stroke(stroke, cap = StrokeCap.Round))
        drawArc(Color(0xFF4285F4), 310f, 100f, false, style = Stroke(stroke, cap = StrokeCap.Round))
        drawArc(Color(0xFF34A853), 50f, 100f, false, style = Stroke(stroke, cap = StrokeCap.Round))
        drawArc(Color(0xFFFBBC05), 150f, 60f, false, style = Stroke(stroke, cap = StrokeCap.Round))
        drawLine(Color(0xFF4285F4), center, Offset(size.width * 0.94f, center.y), stroke, StrokeCap.Round)
    }
}
