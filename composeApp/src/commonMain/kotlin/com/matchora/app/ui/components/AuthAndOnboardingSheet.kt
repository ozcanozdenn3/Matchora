package com.matchora.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
    PASSPORT_SETUP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthAndOnboardingSheet(
    onDismiss: () -> Unit,
    initialStage: AuthStage = AuthStage.LOGIN_OPTIONS,
    currentCity: String = "İstanbul",
    currentDistrict: String = "Kadıköy",
    onCompletePassport: (UserProfile, String) -> Unit,
    onContinueAsGuest: () -> Unit
) {
    var stage by remember { mutableStateOf(initialStage) }
    var loginProvider by remember { mutableStateOf("Apple ID") }
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var athleteName by remember { mutableStateOf("Özcan Özden") }

    ModalBottomSheet(
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        tonalElevation = 0.dp,
        scrimColor = Color(0xFF0B1220).copy(alpha = .56f),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF8FAFC),
        dragHandle = {
            MPremiumSheetHandle()
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
                            loginProvider = "Apple ID"
                            athleteName = "Özcan Özden"
                            stage = AuthStage.PASSPORT_SETUP
                        },
                        onSelectGoogle = {
                            loginProvider = "Google"
                            athleteName = "Özcan Özden"
                            stage = AuthStage.PASSPORT_SETUP
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
                            loginProvider = "Telefon"
                            athleteName = "Özcan Özden"
                            stage = AuthStage.PASSPORT_SETUP
                        }
                    )
                }

                AuthStage.PASSPORT_SETUP -> {
                    PassportSetupView(
                        loginProvider = loginProvider,
                        prefilledName = athleteName,
                        prefilledPhone = phoneNumber,
                        prefilledEmail = if (loginProvider == "Apple ID") "ozcan@icloud.com" else if (loginProvider == "Google") "ozcan@gmail.com" else "",
                        initialCity = currentCity,
                        initialDistrict = currentDistrict,
                        onBack = { stage = AuthStage.LOGIN_OPTIONS },
                        onSaveAndContinue = { completedProfile ->
                            onCompletePassport(completedProfile, loginProvider)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 1. GİRİŞ SEÇENEKLERİ EKRANI
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
                text = "Giriş yap ve tek ekranda sporcu pasaportunu tamamla.",
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
                        text = "Girişte pasaportunla birlikte cüzdanına aktarılır.",
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
// 2. SPORCU PASAPORTU BİLGİLERİNİ DOLDURMA PENCERESİ (GİRİŞ SONRASI ADIM)
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun PassportFormCard(
    sectionNumber: String,
    title: String,
    subtitle: String? = null,
    badgeText: String? = null,
    badgeColor: Color = Color(0xFFFFF7ED),
    badgeTextColor: Color = Color(0xFF9A3412),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 5.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color(0x160B1220),
                spotColor = Color(0x220B1220)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MColors.Ink900,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = sectionNumber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFFB68F)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = title,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MColors.Ink900
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                fontSize = 11.sp,
                                color = MColors.Ink500
                            )
                        }
                    }
                }
                if (badgeText != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = badgeColor,
                        border = BorderStroke(0.8.dp, badgeTextColor.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            content()
        }
    }
}

@Composable
private fun PassportSetupView(
    loginProvider: String,
    prefilledName: String,
    prefilledPhone: String,
    prefilledEmail: String,
    initialCity: String,
    initialDistrict: String,
    onBack: () -> Unit,
    onSaveAndContinue: (UserProfile) -> Unit
) {
    val scrollState = rememberScrollState()

    // Form State
    var athleteName by remember { mutableStateOf(prefilledName.ifBlank { "Özcan Özden" }) }
    var birthYear by remember { mutableStateOf("1996") }
    var bio by remember {
        mutableStateOf("Hafta içi akşam halı saha, hafta sonu tenis. Fair play ve tempolu oyun.")
    }
    var phone by remember {
        mutableStateOf(prefilledPhone.ifBlank { "+90 532 555 42 18" })
    }
    var email by remember {
        mutableStateOf(prefilledEmail.ifBlank { "ozcan@matchora.app" })
    }
    var city by remember { mutableStateOf(initialCity) }
    var district by remember { mutableStateOf(initialDistrict) }
    var showLocationPicker by remember { mutableStateOf(false) }

    var campus by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }

    // Hand & Foot Preferences (Requested explicitly: "sol el sağ el ikside seçenekleride olmalı")
    var dominantHand by remember { mutableStateOf("Sağ El") }
    var dominantFoot by remember { mutableStateOf("Sağ Ayak") }

    // Sports & Role
    var selectedSport by remember { mutableStateOf(Sport.FOOTBALL) }
    var selectedPosition by remember { mutableStateOf("Orta Saha") }
    var jerseyNumber by remember { mutableStateOf("10") }
    var secondarySports by remember { mutableStateOf(setOf(Sport.TENNIS, Sport.PADEL)) }

    // Level
    var selectedLevel by remember { mutableStateOf(SkillLevel.INTERMEDIATE) }

    // Availability
    var selectedAvailability by remember {
        mutableStateOf(setOf("Hafta içi akşam (19:00 - 23:00) 🌙", "Hafta sonu sabah (09:00 - 13:00) ☀️"))
    }

    // Dynamic Positions
    val sportPositions = remember(selectedSport) {
        when (selectedSport) {
            Sport.FOOTBALL -> listOf("Kaleci 🧤", "Defans 🛡️", "Orta Saha ⚡", "Forvet / Kanat 🎯")
            Sport.BASKETBALL -> listOf("Oyun Kurucu (PG) 🧠", "Şutör Gard (SG) 🎯", "Kısa Forvet (SF) ⚡", "Uzun Forvet (PF) 💪", "Pivot (C) 🛡️")
            Sport.TENNIS -> listOf("Tekler (Singles) 🎾", "Çiftler (Doubles) 👥", "Baseline ⚡", "File Önü 🎯")
            Sport.PADEL -> listOf("Sol Taraf 🎾", "Sağ Taraf 🎾", "Çiftler 👥", "Ofansif 🎯")
            Sport.VOLLEYBALL -> listOf("Pasör 🧠", "Smaçör ⚡", "Orta Oyuncu 🛡️", "Libero 🧤", "Pasör Çaprazı 🎯")
            Sport.TABLE_TENNIS -> listOf("Ofansif Forehand 🏓", "Defansif Blok 🛡️", "Çok Yönlü Spin ⚡")
            Sport.SWIMMING -> listOf("Serbest & Kelebek 🏊", "Kurbağalama 🌊", "Sırtüstü ⚡")
            Sport.FITNESS -> listOf("Fonksiyonel & HIIT 🏋️", "Güç & Kondisyon 💪", "Kardiyo 🏃")
            else -> listOf("Genel / Çok Yönlü ⚡", "Hücum / Forvet 🎯", "Savunma / Defans 🛡️", "Oyun Kurucu 🧠")
        }
    }

    LaunchedEffect(selectedSport) {
        selectedPosition = sportPositions.first().substringBefore(" ")
    }

    // Derived values
    val initials = remember(athleteName) {
        athleteName.trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase().ifBlank { "M" }
    }
    val parsedYear = birthYear.toIntOrNull() ?: 1996
    val athleteAge = (2026 - parsedYear).coerceIn(10, 85)
    val ageCategory = when {
        athleteAge < 18 -> "Genç Gelişim (U-18)"
        athleteAge < 35 -> "Büyükler / Amatör Açık Lig"
        else -> "Veteran / Master Ligi (+35)"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .navigationBarsPadding()
            .padding(bottom = 16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        // Üst Bar: Geri ve Başlık
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Geri", tint = MColors.Ink900)
                }
                Text(
                    text = "Sporcu Pasaportunu Oluştur",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MColors.Ink900
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFECFDF5),
                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Rounded.Check, null, tint = Color(0xFF059669), modifier = Modifier.size(13.dp))
                    Text(
                        text = loginProvider,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF047857)
                    )
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // HERO: DİJİTAL SPORCU PASAPORTU ÖNİZLEME KARTI
        // ══════════════════════════════════════════════════════════════════════
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MColors.Ink900,
            shadowElevation = 5.dp,
            border = BorderStroke(1.dp, Color(0xFF40505B)),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(7.dp, RoundedCornerShape(22.dp), ambientColor = Color(0x35000000), spotColor = Color(0x50000000))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF293D46), MColors.Ink900)))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "T.C. SPORCU PASAPORTU · RESMİ AKTİVASYON",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.1.sp,
                        color = Color(0xFFE1C49D)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE1C49D).copy(alpha = 0.18f),
                        border = BorderStroke(0.8.dp, Color(0xFFE1C49D).copy(alpha = 0.55f))
                    ) {
                        Text(
                            text = "🎁 +20 TKN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE1C49D),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            maxLines = 1
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFFE1BE87), Color(0xFF96704D))),
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MColors.Ink900
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = athleteName.ifBlank { "Yeni Sporcu" },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                shape = RoundedCornerShape(5.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.2f),
                                border = BorderStroke(0.6.dp, Color(0xFF10B981).copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "✓ $loginProvider",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6EE7B7),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "$city · $district · $dominantFoot · $dominantHand",
                            fontSize = 11.sp,
                            color = Color(0xFFB8C7CE),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // BÖLÜM 1: KİMLİK & SAHADAKİ DURUŞ
        // ══════════════════════════════════════════════════════════════════════
        PassportFormCard(
            sectionNumber = "1",
            title = "Sporcu Kimliği & Duruş",
            subtitle = "Saha eşleşmelerinde ve dijital QR kartında görünür",
            badgeText = "Temel Bilgiler"
        ) {
            MFloatingInputBox(
                label = "Sporcu Adı Soyadı",
                value = athleteName,
                onValueChange = { athleteName = it },
                placeholder = "Örn: Özcan Özden",
                icon = Icons.Rounded.Person,
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(Modifier.weight(1f)) {
                    MFloatingInputBox(
                        label = "Doğum Yılı",
                        value = birthYear,
                        onValueChange = { if (it.length <= 4) birthYear = it.filter { c -> c.isDigit() } },
                        placeholder = "1996",
                        icon = Icons.Rounded.Cake,
                        singleLine = true
                    )
                }
                Box(Modifier.weight(1f)) {
                    MFloatingInputBox(
                        label = "Forma Numarası",
                        value = jerseyNumber,
                        onValueChange = { if (it.length <= 2) jerseyNumber = it.filter { c -> c.isDigit() } },
                        placeholder = "10",
                        icon = Icons.Rounded.Pin,
                        singleLine = true
                    )
                }
            }

            // Yaş ve Lig Kategorisi Bilgisi
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF1F5F9),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("🎂", fontSize = 13.sp)
                    Text(
                        text = "$athleteAge Yaşında · $ageCategory",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MColors.Ink700
                    )
                }
            }

            // İletişim Bilgileri (Acil Maç ve Bildirimler için)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(Modifier.weight(1f)) {
                    MFloatingInputBox(
                        label = "Telefon",
                        value = phone,
                        onValueChange = { phone = it },
                        placeholder = "+90 5•• ••• •• ••",
                        icon = Icons.Rounded.Phone,
                        singleLine = true
                    )
                }
                Box(Modifier.weight(1f)) {
                    MFloatingInputBox(
                        label = "E-posta",
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "sporcu@matchora.app",
                        icon = Icons.Rounded.Email,
                        singleLine = true
                    )
                }
            }

            // Motto & Biyografi
            MFloatingInputBox(
                label = "Sahadaki Biyografin & Motto",
                value = bio,
                onValueChange = { bio = it },
                placeholder = "Örn: Hafta içi akşam halı saha, hafta sonu tenis. Fair play ve tempolu oyun...",
                icon = Icons.Rounded.EditNote,
                singleLine = false,
                minLines = 2,
                maxLines = 4,
                suggestions = listOf(
                    "Fair play & tempolu oyun ⚡",
                    "Hafta içi akşam halı saha ⚽",
                    "Kortta centilmenlik ve rekabet 🎾",
                    "Takım oyununa ve paslaşmaya açığım 🤝"
                ),
                onSuggestionClick = { bio = it }
            )
        }

        // ══════════════════════════════════════════════════════════════════════
        // BÖLÜM 2: ANA SPOR BRANŞI & SAHADAKİ ROLÜN
        // ══════════════════════════════════════════════════════════════════════
        PassportFormCard(
            sectionNumber = "2",
            title = "Ana Spor Branşı & Mevki",
            subtitle = "Kadro ve maç aramalarında eşleşeceğin pozisyon",
            badgeText = "${selectedSport.label} · $selectedPosition"
        ) {
            Text("Ana Spor Branşın", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MColors.Ink700)
            val allSports = listOf(
                Sport.FOOTBALL to "Futbol ⚽",
                Sport.BASKETBALL to "Basketbol 🏀",
                Sport.TENNIS to "Tenis 🎾",
                Sport.PADEL to "Padel 🎾",
                Sport.VOLLEYBALL to "Voleybol 🏐",
                Sport.TABLE_TENNIS to "Masa Tenisi 🏓",
                Sport.SWIMMING to "Yüzme 🏊",
                Sport.FITNESS to "Fitness 🏃"
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(allSports) { (sport, label) ->
                    val isSelected = selectedSport == sport
                    Surface(
                        onClick = { selectedSport = sport },
                        shape = RoundedCornerShape(11.dp),
                        color = if (isSelected) MColors.Ink900 else Color.White,
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFFFFB37B) else Color(0xFFCBD5E1))
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MColors.Ink800,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Text("Sahadaki Mevkin / Rolün", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MColors.Ink700)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sportPositions) { pos ->
                    val isSelected = selectedPosition.equals(pos.substringBefore(" "), ignoreCase = true)
                    Surface(
                        onClick = { selectedPosition = pos.substringBefore(" ") },
                        shape = RoundedCornerShape(11.dp),
                        color = if (isSelected) MColors.BrandDeep else Color.White,
                        border = BorderStroke(1.dp, if (isSelected) MColors.BrandDeep else Color(0xFFCBD5E1))
                    ) {
                        Text(
                            text = pos,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MColors.Ink800,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            // İkincil İlgilendiğin Diğer Branşlar
            Text("İlgilendiğin Diğer Branşlar (Çoklu Seçim)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MColors.Ink700)
            val otherSports = listOf(
                Sport.BASKETBALL to "Basketbol 🏀",
                Sport.TENNIS to "Tenis 🎾",
                Sport.PADEL to "Padel 🎾",
                Sport.VOLLEYBALL to "Voleybol 🏐",
                Sport.TABLE_TENNIS to "Masa Tenisi 🏓",
                Sport.SWIMMING to "Yüzme 🏊",
                Sport.CHESS to "Satranç ♟️"
            ).filter { it.first != selectedSport }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(otherSports) { (s, label) ->
                    val isPicked = secondarySports.contains(s)
                    Surface(
                        onClick = {
                            secondarySports = if (isPicked) secondarySports - s else secondarySports + s
                        },
                        shape = RoundedCornerShape(9.dp),
                        color = if (isPicked) Color(0xFFFFF7ED) else Color.White,
                        border = BorderStroke(1.dp, if (isPicked) Color(0xFFFFB37B) else Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isPicked) {
                                Icon(Icons.Rounded.Check, null, tint = Color(0xFFC2410C), modifier = Modifier.size(13.dp))
                            }
                            Text(
                                text = label,
                                fontSize = 11.5.sp,
                                fontWeight = if (isPicked) FontWeight.Bold else FontWeight.Normal,
                                color = if (isPicked) Color(0xFF9A3412) else MColors.Ink700
                            )
                        }
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // BÖLÜM 3: BASKIN TARAF (SOL EL, SAĞ EL, HER İKİSİ - EL & AYAK SEÇENEKLERİ)
        // ══════════════════════════════════════════════════════════════════════
        PassportFormCard(
            sectionNumber = "3",
            title = "Baskın Taraf (El & Ayak)",
            subtitle = "Kramponlu saha sporları ve raket/salon sporları için tam uyum",
            badgeText = "$dominantFoot · $dominantHand"
        ) {
            // 1. Baskın El Tercihi
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("✋ Baskın El Tercihin:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MColors.Ink800)
                    Text("(Tenis, Padel, Basketbol, Voleybol)", fontSize = 10.5.sp, color = MColors.Ink500)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val handOptions = listOf(
                        "Sağ El" to "Sağ El ✋",
                        "Sol El" to "Sol El 🤚",
                        "Her İkisi" to "Her İkisi 👐"
                    )
                    handOptions.forEach { (key, label) ->
                        val isSelected = dominantHand == key
                        Surface(
                            onClick = { dominantHand = key },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MColors.Ink900 else Color.White,
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFFFFB37B) else Color(0xFFE2E8F0)),
                            shadowElevation = if (isSelected) 3.dp else 1.dp
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 9.dp, horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MColors.Ink800,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // 2. Baskın Ayak Tercihi
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("⚽ Baskın Ayak Tercihin:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MColors.Ink800)
                    Text("(Futbol, Futsal, Ayak Tenisi)", fontSize = 10.5.sp, color = MColors.Ink500)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val footOptions = listOf(
                        "Sağ Ayak" to "Sağ Ayak ⚽",
                        "Sol Ayak" to "Sol Ayak ⚽",
                        "Her İki Ayak" to "Her İki Ayak ⚡"
                    )
                    footOptions.forEach { (key, label) ->
                        val isSelected = dominantFoot == key
                        Surface(
                            onClick = { dominantFoot = key },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MColors.Ink900 else Color.White,
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFFFFB37B) else Color(0xFFE2E8F0)),
                            shadowElevation = if (isSelected) 3.dp else 1.dp
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 9.dp, horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MColors.Ink800,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Canlı Özet Kutusu
            Surface(
                shape = RoundedCornerShape(11.dp),
                color = Color(0xFFFFF7ED),
                border = BorderStroke(0.8.dp, Color(0xFFFFB37B).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text("🎯", fontSize = 14.sp)
                    Text(
                        text = "Fiziksel Profil: $dominantFoot · $dominantHand Tercihi",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9A3412)
                    )
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // BÖLÜM 4: YETENEK SEVİYESİ & MMR DERECE
        // ══════════════════════════════════════════════════════════════════════
        PassportFormCard(
            sectionNumber = "4",
            title = "Yetenek Seviyesi & MMR",
            subtitle = "Maçora adil eşleştirme algoritması maç dengesini bu puanla kurar",
            badgeText = "${selectedLevel.label} (${when (selectedLevel) {
                SkillLevel.BEGINNER -> 1100
                SkillLevel.INTERMEDIATE -> 1350
                SkillLevel.ADVANCED -> 1550
                SkillLevel.PRO -> 1800
            }} MMR)"
        ) {
            val levels = listOf(
                SkillLevel.BEGINNER to Pair("Başlangıç (1.100 MMR)", "Temel kurallar, hobi & dostluk maçları"),
                SkillLevel.INTERMEDIATE to Pair("Orta Seviye (1.350 MMR)", "Düzenli oynayan, tempolu rekabetçi ligler (Önerilen)"),
                SkillLevel.ADVANCED to Pair("İleri Seviye (1.550 MMR)", "Taktiksel oyun, yüksek tempo ve turnuva seviyesi"),
                SkillLevel.PRO to Pair("Pro / Lisanslı (1.800 MMR)", "Kulüp, akademi veya lisanslı sporcu geçmişi")
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                levels.forEach { (level, details) ->
                    val (title, desc) = details
                    val isSelected = selectedLevel == level
                    Surface(
                        onClick = { selectedLevel = level },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MColors.Ink900 else Color.White,
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFFFFB37B) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = title,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) Color.White else MColors.Ink900
                                )
                                Text(
                                    text = desc,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color(0xFFCBD5E1) else MColors.Ink500
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFFFFB37B), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // BÖLÜM 5: KONUM, TOPLULUK & MÜSAİTLİK
        // ══════════════════════════════════════════════════════════════════════
        PassportFormCard(
            sectionNumber = "5",
            title = "Topluluk, Konum & Müsaitlik",
            subtitle = "Sana en yakın maçları ve uygun takımları bulur",
            badgeText = "$city · $district"
        ) {
            // Şehir & Bölge Seçici
            Surface(
                onClick = { showLocationPicker = true },
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.LocationOn, null, tint = MColors.BrandDeep, modifier = Modifier.size(18.dp))
                        Column {
                            Text("Şehir ve İlçe (Radar Konumu)", fontSize = 11.sp, color = MColors.Ink500)
                            Text("$city · $district", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MColors.Ink900)
                        }
                    }
                    Text("Değiştir ➔", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MColors.BrandDeep)
                }
            }

            // Topluluk Entegrasyonu: Kampüs & Şirket (App'in üst barındaki modları bağlar)
            MFloatingInputBox(
                label = "Üniversite / Kampüs (İsteğe Bağlı)",
                value = campus,
                onValueChange = { campus = it },
                placeholder = "Örn: İTÜ, Boğaziçi...",
                icon = Icons.Rounded.School,
                subtitle = "Uygulamanın Kampüs modunda üniversite liglerine katılmanı sağlar.",
                suggestions = listOf("İTÜ", "Boğaziçi", "ODTÜ", "Yıldız Teknik", "Koç"),
                onSuggestionClick = { campus = it }
            )

            MFloatingInputBox(
                label = "Şirket / Kurum (İsteğe Bağlı)",
                value = company,
                onValueChange = { company = it },
                placeholder = "Örn: Acıbadem, Trendyol...",
                icon = Icons.Rounded.BusinessCenter,
                subtitle = "Şirket modunda kurumsal turnuvalara ve şirket maçlarına bağlar.",
                suggestions = listOf("Acıbadem", "Trendyol", "Garanti BBVA", "Koç Holding"),
                onSuggestionClick = { company = it }
            )

            // Müsaitlik Saatleri
            Text("Oyun Zamanların & Müsaitlik (Çoklu Seçim)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MColors.Ink700)
            val availabilityOptions = listOf(
                "Hafta içi akşam (19:00 - 23:00) 🌙",
                "Hafta sonu sabah (09:00 - 13:00) ☀️",
                "Hafta sonu akşam (17:00 - 23:00) 🌆",
                "Hafta içi öğle (12:00 - 14:00) ⚡"
            )
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                availabilityOptions.forEach { option ->
                    val isChecked = selectedAvailability.contains(option)
                    Surface(
                        onClick = {
                            selectedAvailability = if (isChecked) selectedAvailability - option else selectedAvailability + option
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isChecked) Color(0xFFFFF7ED) else Color.White,
                        border = BorderStroke(1.dp, if (isChecked) Color(0xFFFFB37B) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = option,
                                fontSize = 12.sp,
                                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                                color = if (isChecked) Color(0xFF9A3412) else MColors.Ink800
                            )
                            if (isChecked) {
                                Icon(Icons.Rounded.Check, null, tint = Color(0xFFC2410C), modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // BÖLÜM 6: AKTİVASYON & HOŞ GELDİN PAKETİ
        // ══════════════════════════════════════════════════════════════════════
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFFF7ED),
            border = BorderStroke(1.dp, Color(0xFFFFB37B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🎁", fontSize = 20.sp)
                    Column {
                        Text(
                            text = "+20 TKN Hoş Geldin Jetonu Hazır!",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9A3412)
                        )
                        Text(
                            text = "Pasaportunu kaydettiğin an cüzdanına tanımlanır.",
                            fontSize = 11.sp,
                            color = Color(0xFFC2410C)
                        )
                    }
                }
                HorizontalDivider(color = Color(0xFFFFB37B).copy(alpha = 0.4f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("🛡️ %95 Fair Play Skoru", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C2D12))
                    Text("📱 QR Geçiş Kartı Aktif", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C2D12))
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // EYLEM BUTONU
        // ══════════════════════════════════════════════════════════════════════
        MButton(
            text = "Pasaportumu Onayla & Sahaya Çık 🚀",
            onClick = {
                val newProfile = UserProfile(
                    name = athleteName.trim().ifBlank { "Yeni Sporcu" },
                    city = city.trim().ifBlank { "İstanbul" },
                    area = district.trim().ifBlank { "Kadıköy" },
                    bio = bio.trim().ifBlank { "Sporcu pasaportu aktif. Fair play ve tempolu oyun." },
                    birthYear = birthYear.toIntOrNull() ?: 1996,
                    phone = phone.trim().ifBlank { "+90 532 555 42 18" },
                    email = email.trim().ifBlank { "sporcu@matchora.app" },
                    skills = buildList {
                        // Ana Branş
                        add(
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
                                localRank = 14,
                                position = if (jerseyNumber.isNotBlank()) "$selectedPosition (#$jerseyNumber)" else selectedPosition
                            )
                        )
                        // İkincil Branşlar
                        secondarySports.filter { it != selectedSport }.forEach { secSport ->
                            add(
                                SportSkill(
                                    sport = secSport,
                                    level = SkillLevel.INTERMEDIATE,
                                    rating = 1250,
                                    verifiedMatches = 0,
                                    winRate = 50,
                                    localRank = null,
                                    position = null
                                )
                            )
                        }
                    },
                    verifications = setOf(VerificationLevel.BASIC, VerificationLevel.PHOTO),
                    reliability = 95,
                    fairPlay = 5.0f,
                    verifiedMatches = 1,
                    streakWeeks = 1,
                    campus = campus.trim().ifBlank { null },
                    company = company.trim().ifBlank { null },
                    dominantSide = "$dominantFoot · $dominantHand",
                    availability = if (selectedAvailability.isNotEmpty()) selectedAvailability.toList() else listOf("Hafta içi akşam (19:00–23:00)", "Hafta sonu")
                )
                onSaveAndContinue(newProfile)
            },
            enabled = athleteName.trim().isNotBlank(),
            icon = Icons.Rounded.Check
        )

        Spacer(Modifier.height(10.dp))
    }

    if (showLocationPicker) {
        CityAndLocationSelectorSheet(
            currentCity = city,
            currentDistrict = district,
            isGpsActive = true,
            onDismiss = { showLocationPicker = false },
            onSelectLocation = { selectedCity, selectedDistrict, _ ->
                city = selectedCity
                district = selectedDistrict
                showLocationPicker = false
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 3. TELEFON NUMARASI GİRİŞ EKRANI
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
            text = "Doğrulama kodu gönderebilmemiz için telefon numaranı gir. Giriş yaptığında sporcu pasaportun hazırlanacaktır.",
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
// 4. SMS OTP ONAY EKRANI
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
            text = "Kodu Onayla & Pasaport Bilgilerine Geç ➔",
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
