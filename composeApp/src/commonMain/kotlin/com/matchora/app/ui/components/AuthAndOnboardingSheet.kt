package com.matchora.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        scrimColor = Color(0xFF0B1220).copy(alpha = .48f),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFFAF9F6),
        dragHandle = {
            MPremiumSheetHandle()
        },
        modifier = Modifier.statusBarsPadding().padding(top = 12.dp)
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
// 2. SPORCU PASAPORTU BİLGİLERİNİ DOLDURMA PENCERESİ (PREMIUM & MENÜLÜ SEÇİM)
// ═══════════════════════════════════════════════════════════════════════════════
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
private fun ProfileEditorSectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 4.dp),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.9.sp,
        color = MColors.Ink500
    )
}

@Composable
private fun ProfileEditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
    isError: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        textStyle = MaterialTheme.typography.bodyMedium,
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White,
            unfocusedBorderColor = MColors.Line,
            focusedBorderColor = MColors.Brand,
            focusedLabelColor = MColors.Brand,
            cursorColor = MColors.Brand
        )
    )
}

@Composable
private fun TimePickerMenu(
    label: String,
    selectedTime: String,
    onTimeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val timeOptions = remember {
        listOf(
            "06:00", "06:30", "07:00", "07:30", "08:00", "08:30",
            "09:00", "09:30", "10:00", "10:30", "11:00", "11:30",
            "12:00", "12:30", "13:00", "13:30", "14:00", "14:30",
            "15:00", "15:30", "16:00", "16:30", "17:00", "17:30",
            "18:00", "18:30", "19:00", "19:30", "20:00", "20:30",
            "21:00", "21:30", "22:00", "22:30", "23:00", "23:30",
            "00:00", "00:30", "01:00", "01:30", "02:00"
        )
    }

    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, if (expanded) MColors.Brand else MColors.Line),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = MColors.BrandDeep,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MColors.Ink500
                        )
                        Text(
                            text = selectedTime,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MColors.Ink900
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Saat Menüsü",
                    tint = MColors.Ink500,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 10.dp,
            border = BorderStroke(1.dp, Color(0xFFE4E9F2)),
            modifier = Modifier
                .widthIn(min = 160.dp, max = 220.dp)
                .heightIn(max = 280.dp)
        ) {
            timeOptions.forEach { time ->
                val isSelected = time == selectedTime
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = time,
                                fontSize = 13.5.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) MColors.BrandDeep else MColors.Ink900
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    null,
                                    tint = MColors.BrandDeep,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    onClick = {
                        onTimeSelected(time)
                        expanded = false
                    }
                )
            }
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
    var section by remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()
    LaunchedEffect(section) { scrollState.scrollTo(0) }

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

    Column(Modifier.fillMaxWidth().fillMaxHeight(.92f).imePadding()) {
        // 1. MSheetHeading Header
        Box(Modifier.padding(horizontal = 20.dp)) {
            MSheetHeading(
                title = "Sporcu Pasaportu",
                subtitle = "Kimlik, branş ve oyun tercihlerini belirle",
                icon = Icons.Rounded.Badge,
                onDismiss = onBack
            )
        }

        // 2. Scrollable Content
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(Modifier.height(2.dp))

            // Lüks Profil Önizleme Kartı (ProfileEditorSheet ile birebir aynı gradient ve tasarım)
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(listOf(Color(0xFFFFEBD9), Color(0xFFF5EFE6))),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    Modifier
                        .size(52.dp)
                        .background(Color.White.copy(alpha = .8f), CircleShape)
                        .border(1.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        initials,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        color = MColors.BrandDeep
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        athleteName.ifBlank { "Sporcu profilin" },
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MColors.Ink900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "$city · $district · $dominantFoot · $dominantHand",
                        style = MaterialTheme.typography.bodySmall,
                        color = MColors.Ink600,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White.copy(alpha = 0.85f),
                            border = BorderStroke(0.5.dp, MColors.Line)
                        ) {
                            Text(
                                "✓ $loginProvider",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MColors.Ink700,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFE1C49D).copy(alpha = 0.35f)
                        ) {
                            Text(
                                "🎁 +20 TKN Hediye",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF9A3412),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Segmentli Sekmeler: Kimlik · Spor & Rol · Müsaitlik & Saat
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                listOf("Kimlik", "Spor & Rol", "Müsaitlik & Saat").forEachIndexed { index, title ->
                    Column(
                        Modifier
                            .weight(1f)
                            .clickable { section = index }
                            .padding(top = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            title,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = if (section == index) FontWeight.Bold else FontWeight.Medium,
                            color = if (section == index) MColors.Ink900 else MColors.Ink400
                        )
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(if (section == index) MColors.Brand else MColors.Line, CircleShape)
                        )
                    }
                }
            }

            // Sekme İçerikleri
            when (section) {
                0 -> {
                    // ══════════════════════════════════════════════════════════════
                    // SEKME 0: KİMLİK
                    // ══════════════════════════════════════════════════════════════
                    ProfileEditorSectionTitle("SAHADA SENİ TANISINLAR")
                    ProfileEditField(
                        label = "Ad soyad",
                        value = athleteName,
                        onValueChange = { athleteName = it },
                        isError = athleteName.isBlank()
                    )

                    Surface(
                        onClick = { showLocationPicker = true },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, MColors.Line)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Rounded.LocationOn, null, tint = MColors.BrandDeep)
                            Column(Modifier.weight(1f)) {
                                Text("Şehir ve bölge", style = MaterialTheme.typography.labelSmall, color = MColors.Ink500)
                                Text("$city · $district", color = MColors.Ink900, fontWeight = FontWeight.SemiBold)
                            }
                            Icon(Icons.Rounded.ExpandMore, "Konumu değiştir", tint = MColors.Ink500)
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileEditField(
                            label = "Doğum yılı",
                            value = birthYear,
                            onValueChange = { value -> birthYear = value.filter { it.isDigit() }.take(4) },
                            isError = parsedYear !in 1900..2026,
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                        ProfileEditField(
                            label = "Forma no",
                            value = jerseyNumber,
                            onValueChange = { value -> jerseyNumber = value.filter { it.isDigit() }.take(2) },
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🎂", fontSize = 14.sp)
                            Text(
                                text = "$athleteAge Yaşında · $ageCategory",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MColors.Ink700
                            )
                        }
                    }

                    MFloatingInputBox(
                        label = "Kendinden Bahset & Kısa Biyografi",
                        value = bio,
                        onValueChange = { bio = it },
                        placeholder = "Örn: Tempolu maçları severim, fair play her zaman önceliğimdir...",
                        subtitle = "Sporcu pasaportunda ve maç eşleşmelerinde görünür.",
                        icon = Icons.Rounded.EditNote,
                        badgeText = "Pasaport Notu",
                        singleLine = false,
                        minLines = 3,
                        maxLines = 5,
                        suggestions = listOf(
                            "Tempolu ve centilmence maç arıyorum",
                            "Hafta içi akşamları düzenli oynarım",
                            "Turnuva ve lig maçlarına açığım",
                            "E-Spor rekabetçi kadro arıyorum"
                        )
                    )
                }

                1 -> {
                    // ══════════════════════════════════════════════════════════════
                    // SEKME 1: SPOR & BRANŞ
                    // ══════════════════════════════════════════════════════════════
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.Sports, null, tint = MColors.BrandDeep, modifier = Modifier.size(28.dp))
                        Column {
                            ProfileEditorSectionTitle("BRANŞ VE OYUN TARZIN")
                            Text("Doğru ekiple aynı sahada veya oyunda buluş.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
                        }
                    }

                    // Branş Kategorisi
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(sportTabs) { tab ->
                            val isTabSelected = sportTab == tab
                            Surface(
                                onClick = { sportTab = tab },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isTabSelected) MColors.Ink900 else Color.White,
                                border = BorderStroke(1.dp, if (isTabSelected) MColors.Ink900 else MColors.Line)
                            ) {
                                Text(
                                    text = tab,
                                    fontSize = 12.sp,
                                    fontWeight = if (isTabSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isTabSelected) Color.White else MColors.Ink700,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    // Branş Seçici
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredSports) { sport ->
                            val isSelected = selectedSport == sport
                            Surface(
                                onClick = { selectedSport = sport },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MColors.Ink900 else Color.White,
                                border = BorderStroke(1.2.dp, if (isSelected) Color(0xFFFFB37B) else MColors.Line)
                            ) {
                                Text(
                                    text = "${sport.emoji} ${sport.label}",
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MColors.Ink900,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    // Sahadaki Rol / Mevki
                    Text("Sahadaki Rol / Mevkin:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MColors.Ink700)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(sportPositions) { pos ->
                            val isSelected = selectedPosition.equals(pos.substringBefore(" "), ignoreCase = true)
                            Surface(
                                onClick = { selectedPosition = pos.substringBefore(" ") },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MColors.BrandDeep else Color.White,
                                border = BorderStroke(1.dp, if (isSelected) MColors.BrandDeep else MColors.Line)
                            ) {
                                Text(
                                    text = pos,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MColors.Ink800,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    // Fiziksel Tercihler: Baskın El ve Baskın Ayak
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, MColors.Line),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("✋ Baskın El:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MColors.Ink600)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Sağ El", "Sol El", "Her İkisi").forEach { opt ->
                                    val isSel = dominantHand == opt
                                    Surface(
                                        onClick = { dominantHand = opt },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSel) MColors.Ink900 else Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, if (isSel) Color(0xFFFFB37B) else MColors.Line)
                                    ) {
                                        Box(Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = opt,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSel) Color.White else MColors.Ink800
                                            )
                                        }
                                    }
                                }
                            }

                            Text("⚽ Baskın Ayak:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MColors.Ink600)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Sağ Ayak", "Sol Ayak", "Her İki Ayak").forEach { opt ->
                                    val isSel = dominantFoot == opt
                                    Surface(
                                        onClick = { dominantFoot = opt },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSel) MColors.Ink900 else Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, if (isSel) Color(0xFFFFB37B) else MColors.Line)
                                    ) {
                                        Box(Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = opt,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSel) Color.White else MColors.Ink800
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Seviye / MMR Derecesi
                    Text("Yetenek / MMR Derecesi:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MColors.Ink700)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSel) MColors.Ink900 else Color.White,
                                border = BorderStroke(1.2.dp, if (isSel) Color(0xFFFFB37B) else MColors.Line)
                            ) {
                                Box(Modifier.padding(vertical = 8.dp, horizontal = 2.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp,
                                        textAlign = TextAlign.Center,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) Color.White else MColors.Ink800
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // ══════════════════════════════════════════════════════════════
                    // SEKME 2: MÜSAİTLİK & İLETİŞİM
                    // ══════════════════════════════════════════════════════════════
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.Schedule, null, tint = MColors.BrandDeep, modifier = Modifier.size(28.dp))
                        Column {
                            ProfileEditorSectionTitle("OYUN TAKVİMİN VE İLETİŞİM")
                            Text("Maç davetleri ve organizasyon takvimin.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
                        }
                    }

                    // Gün Seçici Kartı
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, MColors.Line),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Oynayabileceğin Günler:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MColors.Ink600)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Hafta İçi",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MColors.BrandDeep,
                                        modifier = Modifier
                                            .clickable { selectedDays = setOf("Pzt", "Sal", "Çar", "Per", "Cum") }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                    Text("·", fontSize = 11.sp, color = MColors.Ink400)
                                    Text(
                                        text = "Hafta Sonu",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MColors.BrandDeep,
                                        modifier = Modifier
                                            .clickable { selectedDays = setOf("Cmt", "Paz") }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                    Text("·", fontSize = 11.sp, color = MColors.Ink400)
                                    Text(
                                        text = "Tümü",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MColors.BrandDeep,
                                        modifier = Modifier
                                            .clickable { selectedDays = setOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz") }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz").forEach { day ->
                                    val isChecked = selectedDays.contains(day)
                                    Surface(
                                        onClick = {
                                            selectedDays = if (isChecked) selectedDays - day else selectedDays + day
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isChecked) MColors.Ink900 else Color(0xFFF1F5F9),
                                        border = BorderStroke(1.dp, if (isChecked) Color(0xFFFFB37B) else MColors.Line)
                                    ) {
                                        Box(Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = day,
                                                fontSize = 11.5.sp,
                                                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isChecked) Color.White else MColors.Ink800
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Saat Seçimi (AÇILIR MENÜ ŞEKLİNDE - DROP-DOWN PICKER)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimePickerMenu(
                            label = "Başlangıç Saati",
                            selectedTime = startHour,
                            onTimeSelected = { startHour = it },
                            modifier = Modifier.weight(1f)
                        )
                        TimePickerMenu(
                            label = "Bitiş Saati",
                            selectedTime = endHour,
                            onTimeSelected = { endHour = it },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Canlı Özet Satırı
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFF7ED),
                        border = BorderStroke(1.dp, Color(0xFFFFD8BF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("📅", fontSize = 15.sp)
                            Text(
                                text = if (selectedDays.isEmpty()) "Lütfen en az bir gün seç"
                                else "${selectedDays.sortedBy { dayOrder(it) }.joinToString(", ")} · $startHour - $endHour",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF9A3412)
                            )
                        }
                    }

                    ProfileEditorSectionTitle("İLETİŞİM BİLGİLERİN")
                    ProfileEditField(
                        label = "Telefon (Acil maç bildirimleri)",
                        value = phone,
                        onValueChange = { phone = it },
                        isError = phone.count { it.isDigit() } < 7,
                        keyboardType = KeyboardType.Phone
                    )
                    ProfileEditField(
                        label = "E-posta",
                        value = email,
                        onValueChange = { email = it },
                        isError = !email.contains('@'),
                        keyboardType = KeyboardType.Email
                    )

                    HorizontalDivider(color = MColors.Line)
                    ProfileEditorSectionTitle("BAĞLANTILARIN (İSTEĞE BAĞLI)")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProfileEditField(
                            label = "Kampüs / Üniversite",
                            value = campus,
                            onValueChange = { campus = it },
                            modifier = Modifier.weight(1f)
                        )
                        ProfileEditField(
                            label = "Şirket / Kurum",
                            value = company,
                            onValueChange = { company = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }

        // 3. Sabit Yapışkan Alt Eylem Barı
        Surface(color = Color.White, shadowElevation = 8.dp) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (section > 0) {
                        Surface(
                            onClick = { section-- },
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, MColors.Line),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Önceki Adım",
                                    tint = MColors.Ink800,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    MButton(
                        text = when (section) {
                            0 -> "Sonraki: Spor & Branş →"
                            1 -> "Sonraki: Müsaitlik & İletişim →"
                            else -> "Pasaportumu Kaydet & Sahaya Çık 🚀"
                        },
                        onClick = {
                            if (section < 2) {
                                section++
                            } else {
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
                            }
                        },
                        enabled = athleteName.trim().isNotBlank(),
                        modifier = Modifier.weight(1f),
                        icon = if (section == 2) Icons.Rounded.Check else null
                    )
                }
            }
        }
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
