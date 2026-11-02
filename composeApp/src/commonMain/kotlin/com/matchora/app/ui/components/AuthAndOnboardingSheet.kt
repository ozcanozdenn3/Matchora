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
import androidx.compose.ui.graphics.vector.ImageVector
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
// 2. SPORCU PASAPORTU BİLGİLERİNİ DOLDURMA PENCERESİ (DERLİ TOPLU & EKSİKSİZ)
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun TidySectionCard(
    number: String,
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MColors.Ink900,
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = number,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB68F)
                        )
                    }
                }
                Column {
                    Text(
                        text = title,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MColors.Ink900
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            fontSize = 10.5.sp,
                            color = MColors.Ink500
                        )
                    }
                }
            }
            content()
        }
    }
}

@Composable
private fun CleanInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    icon: ImageVector? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MColors.Ink700
        )
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 11.dp, vertical = if (singleLine) 9.dp else 8.dp),
                verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (icon != null) {
                    Icon(icon, null, tint = MColors.Ink500, modifier = Modifier.size(15.dp))
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = singleLine,
                    minLines = minLines,
                    maxLines = maxLines,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 13.sp,
                        color = MColors.Ink900,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(placeholder, fontSize = 13.sp, color = MColors.Ink400)
                        }
                        innerTextField()
                    }
                )
            }
        }
    }
}

private fun dayOrder(day: String): Int = when (day) {
    "Pzt" -> 1
    "Sal" -> 2
    "Çar" -> 3
    "Per" -> 4
    "Cum" -> 5
    "Cmt" -> 6
    "Paz" -> 7
    else -> 8
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

    // Profil & Kimlik Değerleri
    var athleteName by remember { mutableStateOf(prefilledName.ifBlank { "Özcan Özden" }) }
    var birthYear by remember { mutableStateOf("1996") }
    var jerseyNumber by remember { mutableStateOf("10") }
    var phone by remember { mutableStateOf(prefilledPhone.ifBlank { "+90 532 555 42 18" }) }
    var email by remember { mutableStateOf(prefilledEmail.ifBlank { "ozcan@matchora.app" }) }
    var bio by remember {
        mutableStateOf("Centilmence ve tempolu oyun. Fair play önce gelir.")
    }

    // Konum & Topluluk
    var city by remember { mutableStateOf(initialCity) }
    var district by remember { mutableStateOf(initialDistrict) }
    var showLocationPicker by remember { mutableStateOf(false) }
    var campus by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }

    // Branş & E-Spor Kategorisi
    var sportTab by remember { mutableStateOf("Tümü") }
    val sportTabs = listOf("Tümü", "Saha & Takım ⚽", "Raket 🎾", "E-Spor & Zeka 🎮", "Bireysel 🏃")

    var selectedSport by remember { mutableStateOf(Sport.FOOTBALL) }
    var selectedPosition by remember { mutableStateOf("Orta Saha") }

    // Fiziksel Tercihler (Sol El, Sağ El, Her İkisi / Ayak)
    var dominantHand by remember { mutableStateOf("Sağ El") }
    var dominantFoot by remember { mutableStateOf("Sağ Ayak") }

    // Seviye & MMR
    var selectedLevel by remember { mutableStateOf(SkillLevel.INTERMEDIATE) }

    // Manuel Müsaitlik (Gün & Saat Serbest Seçimi)
    var selectedDays by remember { mutableStateOf(setOf("Pzt", "Çar", "Cum")) }
    var startHour by remember { mutableStateOf("19:00") }
    var endHour by remember { mutableStateOf("23:00") }

    // Spor Filtresi
    val filteredSports = remember(sportTab) {
        when (sportTab) {
            "Saha & Takım ⚽" -> listOf(Sport.FOOTBALL, Sport.BASKETBALL, Sport.VOLLEYBALL, Sport.FUTSAL, Sport.HANDBALL, Sport.BEACH_VOLLEYBALL)
            "Raket 🎾" -> listOf(Sport.TENNIS, Sport.PADEL, Sport.TABLE_TENNIS, Sport.BADMINTON, Sport.SQUASH, Sport.PICKLEBALL)
            "E-Spor & Zeka 🎮" -> listOf(Sport.ESPORTS_FC, Sport.ESPORTS_VALORANT, Sport.ESPORTS_CS2, Sport.ESPORTS_LOL, Sport.CHESS)
            "Bireysel 🏃" -> listOf(Sport.FITNESS, Sport.RUNNING, Sport.SWIMMING, Sport.CYCLING, Sport.BOXING, Sport.PILATES_YOGA)
            else -> listOf(
                Sport.FOOTBALL, Sport.BASKETBALL, Sport.TENNIS, Sport.PADEL, Sport.VOLLEYBALL,
                Sport.ESPORTS_FC, Sport.ESPORTS_VALORANT, Sport.ESPORTS_CS2, Sport.ESPORTS_LOL, Sport.CHESS,
                Sport.TABLE_TENNIS, Sport.SWIMMING, Sport.FITNESS, Sport.RUNNING
            )
        }
    }

    // Dinamik roller
    val sportPositions = remember(selectedSport) {
        when (selectedSport) {
            Sport.ESPORTS_FC -> listOf("Forvet / Bitirici 🎯", "Orta Saha / Pasör 🧠", "Defans / Bek 🛡️", "Kaleci 🧤", "1v1 Tekler ⚡")
            Sport.ESPORTS_VALORANT -> listOf("Duelist (Giriş) 💥", "Initiator (Öncü) 🎯", "Controller (Duman) 🌫️", "Sentinel (Gözcü) 🛡️", "IGL / Kaptan 🧠")
            Sport.ESPORTS_CS2 -> listOf("Entry Fragger 💥", "AWPer (Sniper) 🎯", "Rifler 🔫", "Support 🛡️", "IGL / Kaptan 🧠")
            Sport.ESPORTS_LOL -> listOf("Top (Üst) 🛡️", "Jungle (Orman) 🌲", "Mid (Orta) ⚡", "ADC (Alt) 🎯", "Support (Destek) 🤝")
            Sport.CHESS -> listOf("Beyazlar (Hücum)", "Siyahlar (Savunma)", "Blitz (Yıldırım) ⚡", "Rapid (Hızlı) ⏱️", "Klasik")
            Sport.FOOTBALL -> listOf("Kaleci 🧤", "Defans 🛡️", "Orta Saha ⚡", "Forvet / Kanat 🎯")
            Sport.BASKETBALL -> listOf("Oyun Kurucu (PG) 🧠", "Şutör Gard (SG) 🎯", "Kısa Forvet (SF) ⚡", "Uzun Forvet (PF) 💪", "Pivot (C) 🛡️")
            Sport.TENNIS -> listOf("Tekler (Singles) 🎾", "Çiftler (Doubles) 👥", "Baseline ⚡", "File Önü 🎯")
            Sport.PADEL -> listOf("Sol Taraf 🎾", "Sağ Taraf 🎾", "Çiftler 👥", "Ofansif 🎯")
            Sport.VOLLEYBALL -> listOf("Pasör 🧠", "Smaçör ⚡", "Orta Oyuncu 🛡️", "Libero 🧤", "Çapraz 🎯")
            Sport.TABLE_TENNIS -> listOf("Ofansif Forehand 🏓", "Defansif Blok 🛡️", "Spin / Çok Yönlü ⚡")
            Sport.SWIMMING -> listOf("Serbest & Kelebek 🏊", "Kurbağalama 🌊", "Sırtüstü ⚡")
            Sport.FITNESS -> listOf("Fonksiyonel & HIIT 🏋️", "Güç & Kondisyon 💪", "Kardiyo 🏃")
            else -> listOf("Genel / Çok Yönlü ⚡", "Hücum / Ofansif 🎯", "Savunma / Defansif 🛡️", "Kaptan / Lider 🧠")
        }
    }

    LaunchedEffect(selectedSport) {
        selectedPosition = sportPositions.first().substringBefore(" ")
    }

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
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
            .padding(bottom = 14.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Üst Bar: Geri ve Başlık
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onBack, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Geri", tint = MColors.Ink900)
                }
                Text(
                    text = "Sporcu Pasaportu Kaydı",
                    fontSize = 16.sp,
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
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Rounded.Check, null, tint = Color(0xFF059669), modifier = Modifier.size(12.dp))
                    Text(
                        text = loginProvider,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF047857)
                    )
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // DERLİ TOPLU SPORCU KİMLİK ROZETİ (TEMİZ & ŞIK)
        // ══════════════════════════════════════════════════════════════════════
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MColors.Ink900,
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFE1BE87), Color(0xFF96704D))),
                            RoundedCornerShape(11.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MColors.Ink900
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = athleteName.ifBlank { "Yeni Sporcu" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$city · $district · $dominantFoot · $dominantHand",
                        fontSize = 10.5.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = Color(0xFFE1C49D).copy(alpha = 0.16f),
                    border = BorderStroke(0.8.dp, Color(0xFFE1C49D).copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "🎁 +20 TKN",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE1C49D),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // 01 · KİMLİK & İLETİŞİM
        // ══════════════════════════════════════════════════════════════════════
        TidySectionCard(
            number = "1",
            title = "Kimlik & İletişim",
            subtitle = "Saha eşleşmelerinde ve QR kartında görünür"
        ) {
            CleanInputField(
                label = "Sporcu Adı Soyadı",
                value = athleteName,
                onValueChange = { athleteName = it },
                placeholder = "Örn: Özcan Özden",
                icon = Icons.Rounded.Person
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CleanInputField(
                    label = "Doğum Yılı",
                    value = birthYear,
                    onValueChange = { if (it.length <= 4) birthYear = it.filter { c -> c.isDigit() } },
                    placeholder = "1996",
                    icon = Icons.Rounded.Cake,
                    modifier = Modifier.weight(1f)
                )
                CleanInputField(
                    label = "Forma Numarası",
                    value = jerseyNumber,
                    onValueChange = { if (it.length <= 2) jerseyNumber = it.filter { c -> c.isDigit() } },
                    placeholder = "10",
                    icon = Icons.Rounded.Pin,
                    modifier = Modifier.weight(1f)
                )
            }

            // Yaş rozeti
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF1F5F9),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text("🎂", fontSize = 12.sp)
                    Text(
                        text = "$athleteAge Yaşında · $ageCategory",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MColors.Ink700
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CleanInputField(
                    label = "Telefon (Acil Maç)",
                    value = phone,
                    onValueChange = { phone = it },
                    placeholder = "+90 5•• ••• •• ••",
                    icon = Icons.Rounded.Phone,
                    modifier = Modifier.weight(1f)
                )
                CleanInputField(
                    label = "E-posta",
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "sporcu@matchora.app",
                    icon = Icons.Rounded.Email,
                    modifier = Modifier.weight(1f)
                )
            }

            CleanInputField(
                label = "Motto / Biyografi (İsteğe Bağlı)",
                value = bio,
                onValueChange = { bio = it },
                placeholder = "Sahadaki tarzın, fair play yaklaşımın...",
                icon = Icons.Rounded.EditNote,
                singleLine = false,
                minLines = 2,
                maxLines = 3
            )
        }

        // ══════════════════════════════════════════════════════════════════════
        // 02 · BRANŞ, E-SPOR & OYUN ROLÜ
        // ══════════════════════════════════════════════════════════════════════
        TidySectionCard(
            number = "2",
            title = "Branş & Oyun Rolün",
            subtitle = "Fiziksel sporlar, kort branşları veya e-spor / online oyunlar"
        ) {
            // Kategori Sekmeleri
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(sportTabs) { tab ->
                    val isTabSelected = sportTab == tab
                    Surface(
                        onClick = { sportTab = tab },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isTabSelected) MColors.Ink900 else Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = tab,
                            fontSize = 11.sp,
                            fontWeight = if (isTabSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isTabSelected) Color.White else MColors.Ink700,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Spor Listesi
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(filteredSports) { sport ->
                    val isSelected = selectedSport == sport
                    Surface(
                        onClick = { selectedSport = sport },
                        shape = RoundedCornerShape(9.dp),
                        color = if (isSelected) MColors.Ink900 else Color.White,
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFFFFB37B) else Color(0xFFE2E8F0))
                    ) {
                        Text(
                            text = "${sport.emoji} ${sport.label}",
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MColors.Ink800,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            // Sahadaki Rol / Mevki
            Text("Sahadaki Rol / Mevkin:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MColors.Ink700)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(sportPositions) { pos ->
                    val isSelected = selectedPosition.equals(pos.substringBefore(" "), ignoreCase = true)
                    Surface(
                        onClick = { selectedPosition = pos.substringBefore(" ") },
                        shape = RoundedCornerShape(9.dp),
                        color = if (isSelected) MColors.BrandDeep else Color.White,
                        border = BorderStroke(1.dp, if (isSelected) MColors.BrandDeep else Color(0xFFE2E8F0))
                    ) {
                        Text(
                            text = pos,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MColors.Ink800,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Fiziksel Tercihler: Baskın El & Baskın Ayak (Sol El, Sağ El, Her İkisi)
            HorizontalDivider(color = Color(0xFFE2E8F0))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // El
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("✋ Baskın El:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MColors.Ink700)
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        listOf("Sağ El", "Sol El", "Her İkisi").forEach { opt ->
                            val isSel = dominantHand == opt
                            Surface(
                                onClick = { dominantHand = opt },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(7.dp),
                                color = if (isSel) MColors.Ink900 else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isSel) Color(0xFFFFB37B) else Color(0xFFE2E8F0))
                            ) {
                                Box(Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = opt.replace(" El", ""),
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) Color.White else MColors.Ink800
                                    )
                                }
                            }
                        }
                    }
                }

                // Ayak
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("⚽ Baskın Ayak:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MColors.Ink700)
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        listOf("Sağ Ayak", "Sol Ayak", "Her İki Ayak").forEach { opt ->
                            val isSel = dominantFoot == opt
                            Surface(
                                onClick = { dominantFoot = opt },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(7.dp),
                                color = if (isSel) MColors.Ink900 else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isSel) Color(0xFFFFB37B) else Color(0xFFE2E8F0))
                            ) {
                                Box(Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = opt.replace(" Ayak", "").replace("Her İki", "Çift"),
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) Color.White else MColors.Ink800
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Yetenek / MMR Seviyesi
            Text("Yetenek / MMR Derecesi:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MColors.Ink700)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                val levels = listOf(
                    SkillLevel.BEGINNER to "Başlangıç\n1.100 MMR",
                    SkillLevel.INTERMEDIATE to "Orta\n1.350 MMR",
                    SkillLevel.ADVANCED to "İleri\n1.550 MMR",
                    SkillLevel.PRO to "Pro\n1.800 MMR"
                )
                levels.forEach { (lvl, label) ->
                    val isSel = selectedLevel == lvl
                    Surface(
                        onClick = { selectedLevel = lvl },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSel) MColors.Ink900 else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isSel) Color(0xFFFFB37B) else Color(0xFFE2E8F0))
                    ) {
                        Box(Modifier.padding(vertical = 6.dp, horizontal = 2.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color.White else MColors.Ink800
                            )
                        }
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // 03 · MÜSAİTLİK GÜN VE SAATLERİ (MANUEL SERBEST SEÇİM)
        // ══════════════════════════════════════════════════════════════════════
        TidySectionCard(
            number = "3",
            title = "Müsaitlik Gün ve Saatleri",
            subtitle = "İstediğin günleri dokunarak aç/kapat, serbest saat belirle"
        ) {
            // Hızlı Gün Şablonları & Gün Butonları
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Oynayabileceğin Günler:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MColors.Ink700)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Hafta İçi",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MColors.BrandDeep,
                        modifier = Modifier
                            .clickable { selectedDays = setOf("Pzt", "Sal", "Çar", "Per", "Cum") }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    Text("·", fontSize = 10.5.sp, color = MColors.Ink400)
                    Text(
                        text = "Hafta Sonu",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MColors.BrandDeep,
                        modifier = Modifier
                            .clickable { selectedDays = setOf("Cmt", "Paz") }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    Text("·", fontSize = 10.5.sp, color = MColors.Ink400)
                    Text(
                        text = "Tümü",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MColors.BrandDeep,
                        modifier = Modifier
                            .clickable { selectedDays = setOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz") }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            // 7 Gün Butonu
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz").forEach { day ->
                    val isChecked = selectedDays.contains(day)
                    Surface(
                        onClick = {
                            selectedDays = if (isChecked) selectedDays - day else selectedDays + day
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isChecked) MColors.Ink900 else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (isChecked) Color(0xFFFFB37B) else Color(0xFFE2E8F0))
                    ) {
                        Box(Modifier.padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = day,
                                fontSize = 11.sp,
                                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                                color = if (isChecked) Color.White else MColors.Ink800
                            )
                        }
                    }
                }
            }

            // Saat Aralığı Seçimi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Başlangıç Saati
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Başlangıç Saati:", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = MColors.Ink600)
                    val startHours = listOf("09:00", "12:00", "18:00", "19:00", "20:00", "21:00")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(startHours) { h ->
                            val isSel = startHour == h
                            Surface(
                                onClick = { startHour = h },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSel) MColors.BrandDeep else Color(0xFFF8FAFC),
                                border = BorderStroke(0.8.dp, if (isSel) MColors.BrandDeep else Color(0xFFE2E8F0))
                            ) {
                                Text(
                                    text = h,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) Color.White else MColors.Ink800,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Bitiş Saati
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Bitiş Saati:", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = MColors.Ink600)
                    val endHours = listOf("14:00", "21:00", "22:00", "23:00", "00:00", "01:00")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(endHours) { h ->
                            val isSel = endHour == h
                            Surface(
                                onClick = { endHour = h },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSel) MColors.BrandDeep else Color(0xFFF8FAFC),
                                border = BorderStroke(0.8.dp, if (isSel) MColors.BrandDeep else Color(0xFFE2E8F0))
                            ) {
                                Text(
                                    text = h,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) Color.White else MColors.Ink800,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Canlı Özet Satırı
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFFF7ED),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text("📅", fontSize = 11.5.sp)
                    Text(
                        text = if (selectedDays.isEmpty()) "Lütfen en az bir gün seç"
                        else "${selectedDays.sortedBy { dayOrder(it) }.joinToString(", ")} · $startHour - $endHour",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9A3412)
                    )
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // 04 · KONUM & TOPLULUK (İSTEĞE BAĞLI)
        // ══════════════════════════════════════════════════════════════════════
        TidySectionCard(
            number = "4",
            title = "Konum & Topluluk",
            subtitle = "Sana yakın sahaları, üniversite veya şirket maçlarını bağlar"
        ) {
            Surface(
                onClick = { showLocationPicker = true },
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.LocationOn, null, tint = MColors.BrandDeep, modifier = Modifier.size(16.dp))
                        Text(
                            text = "$city · $district (GPS Aktif 📍)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MColors.Ink900
                        )
                    }
                    Text("Değiştir ➔", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MColors.BrandDeep)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CleanInputField(
                    label = "Üniversite (Kampüs Ligi)",
                    value = campus,
                    onValueChange = { campus = it },
                    placeholder = "Örn: İTÜ, Boğaziçi",
                    icon = Icons.Rounded.School,
                    modifier = Modifier.weight(1f)
                )
                CleanInputField(
                    label = "Şirket / Kurum (Şirket Ligi)",
                    value = company,
                    onValueChange = { company = it },
                    placeholder = "Örn: Acıbadem, Trendyol",
                    icon = Icons.Rounded.BusinessCenter,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // HOŞ GELDİN AVANTAJLARI
        // ══════════════════════════════════════════════════════════════════════
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFFFF7ED),
            border = BorderStroke(1.dp, Color(0xFFFFB37B).copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("🎁", fontSize = 14.sp)
                    Text("+20 TKN Hediye", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9A3412))
                }
                Text("🛡️ %95 Fair Play", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF9A3412))
                Text("📱 QR Pasaport", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF9A3412))
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // KAYDET VE SAHAYA ÇIK
        // ══════════════════════════════════════════════════════════════════════
        MButton(
            text = "Pasaportumu Kaydet & Sahaya Çık 🚀",
            onClick = {
                val availabilityText = if (selectedDays.isEmpty()) {
                    "Hafta içi $startHour–$endHour"
                } else {
                    "${selectedDays.sortedBy { dayOrder(it) }.joinToString(", ")} $startHour–$endHour"
                }

                val newProfile = UserProfile(
                    name = athleteName.trim().ifBlank { "Yeni Sporcu" },
                    city = city.trim().ifBlank { "İstanbul" },
                    area = district.trim().ifBlank { "Kadıköy" },
                    bio = bio.trim().ifBlank { "Sporcu pasaportu aktif. Fair play ve tempolu oyun." },
                    birthYear = birthYear.toIntOrNull() ?: 1996,
                    phone = phone.trim().ifBlank { "+90 532 555 42 18" },
                    email = email.trim().ifBlank { "sporcu@matchora.app" },
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
                            localRank = 14,
                            position = if (jerseyNumber.isNotBlank()) "$selectedPosition (#$jerseyNumber)" else selectedPosition
                        )
                    ),
                    verifications = setOf(VerificationLevel.BASIC, VerificationLevel.PHOTO),
                    reliability = 95,
                    fairPlay = 5.0f,
                    verifiedMatches = 1,
                    streakWeeks = 1,
                    campus = campus.trim().ifBlank { null },
                    company = company.trim().ifBlank { null },
                    dominantSide = "$dominantFoot · $dominantHand",
                    availability = listOf(availabilityText)
                )
                onSaveAndContinue(newProfile)
            },
            enabled = athleteName.trim().isNotBlank(),
            icon = Icons.Rounded.Check
        )

        Spacer(Modifier.height(8.dp))
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
