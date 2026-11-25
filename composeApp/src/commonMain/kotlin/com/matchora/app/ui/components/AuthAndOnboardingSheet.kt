package com.matchora.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.data.*
import kotlinx.serialization.json.*
import kotlinx.coroutines.launch
import com.matchora.app.domain.Sport
import com.matchora.app.state.*
import com.matchora.app.ui.theme.MColors
import com.matchora.app.ui.theme.MGradients
import kotlinx.coroutines.delay

enum class AuthStage { LOGIN_OPTIONS, PHONE_INPUT, PHONE_OTP, PASSPORT_SETUP, LEGAL_REVIEW }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthAndOnboardingSheet(
    onDismiss: () -> Unit,
    initialStage: AuthStage = AuthStage.LOGIN_OPTIONS,
    currentCity: String = "İstanbul",
    currentDistrict: String = "Kadıköy",
    existingProfile: UserProfile? = null,
    backend: BackendRepository? = null,
    externallyBusy: Boolean = false,
    providerSignIn: (suspend (String) -> String)? = null,
    onCompletePassport: (UserProfile, String) -> Unit,
    onContinueAsGuest: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val backendConfig = if (backend != null) backend.configurations.collectAsState().value else null
    val legalRequired = backendConfig?.b("requiresAcceptance") == true
    var legalAccepted by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var stage by remember { mutableStateOf(initialStage) }
    var provider by remember { mutableStateOf(if (storePlatform == "ios") "Apple" else "Google") }
    var selectedCountry by remember { mutableStateOf(defaultPhoneCountry) }
    var phone by remember { mutableStateOf("") }
    var sentPhone by remember { mutableStateOf("") }
    var challengeId by remember { mutableStateOf("") }
    var resendSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(resendSeconds) { if (resendSeconds > 0) { delay(1000); resendSeconds-- } }
    var code by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf(PassportDraft(city = currentCity, district = currentDistrict)) }
    var photo by remember { mutableStateOf<ImageBitmap?>(null) }
    var restoredProfile by remember { mutableStateOf(existingProfile) }
    var section by remember { mutableIntStateOf(0) }
    fun continueToPassport() {
        val snap = backend?.snapshots?.value
        val isAdminRole = snap?.s("role") == "admin"
        val restored = snap?.let(::BackendSnapshot)?.profile ?: existingProfile
        if (restored == null && !isAdminRole) {
            backend?.profileHints?.let { hints ->
                draft = draft.copy(
                    name = draft.name.ifBlank { hints.s("name") },
                    email = draft.email.ifBlank { hints.s("email") },
                    phone = draft.phone.ifBlank { hints.s("phone") }
                )
            }
        }
        if (snap?.b("legalAcceptanceRequired") == true) {
            restoredProfile = restored
            stage = AuthStage.LEGAL_REVIEW
        } else if (restored != null) {
            restoredProfile = restored
            onCompletePassport(restored, provider)
        } else if (isAdminRole) {
            onCompletePassport(
                UserProfile(
                    name = "Yönetici",
                    city = currentCity,
                    area = currentDistrict,
                    bio = "",
                    birthYear = 1990,
                    phone = "",
                    email = "",
                    skills = emptyList(),
                    verifications = setOf(VerificationLevel.BASIC),
                    reliability = 100,
                    fairPlay = 5f,
                    verifiedMatches = 0,
                    streakWeeks = 0,
                    campus = null,
                    company = null,
                    dominantSide = "",
                    availability = emptyList()
                ),
                provider
            )
        } else {
            stage = AuthStage.PASSPORT_SETUP
        }
    }
    LaunchedEffect(initialStage) {
        if (!busy) {
            if (initialStage == AuthStage.PASSPORT_SETUP && backend?.authenticated == true) continueToPassport()
            else if (initialStage == AuthStage.LEGAL_REVIEW) stage = initialStage
        }
    }
    fun enter(selected: String) {
        if (busy || externallyBusy) return
        provider = selected
        if (backend == null) { authError = "Giriş servisi henüz bağlanmadı."; return }
        scope.launch {
            busy = true; authError = null
            try {
                if (backend.configuration == null) backend.refreshConfiguration()
                val providerLower = selected.lowercase()
                if (providerLower == "apple" && isNativeAppleSignInSupported) {
                    val nativeResult = performNativeAppleSignIn()
                    if (nativeResult != null) {
                        backend.exchangeIdToken("apple", nativeResult.idToken, nativeResult.fullName, nativeResult.nonce)
                    } else {
                        backend.socialLogin("apple")
                    }
                } else if (providerLower == "google" && isNativeGoogleSignInSupported) {
                    val nativeResult = performNativeGoogleSignIn()
                    if (nativeResult != null) {
                        backend.exchangeIdToken("google", nativeResult.idToken, nativeResult.fullName)
                    } else {
                        backend.socialLogin("google")
                    }
                } else if (providerSignIn != null) backend.exchangeProviderToken(providerSignIn(selected))
                else backend.socialLogin(selected)
                continueToPassport()
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) {
                authError = when {
                    error is BackendFailure && error.code == "AUTH_CANCELLED" -> null
                    error is BackendFailure && error.code == "CONFIGURATION_REQUIRED" -> "Giriş servisi henüz bağlanmadı. Lütfen daha sonra tekrar dene."
                    else -> error.message
                }
            }
            finally { busy = false }
        }
    }
    fun sendPhoneCode() {
        if (busy || externallyBusy) return
        val normalized = normalizePhoneWithCountry(selectedCountry, phone) ?: return
        if (sentPhone == normalized && resendSeconds > 0) return
        if (backend == null) { authError = "Giriş servisi henüz bağlanmadı."; return }
        scope.launch {
            busy = true; authError = null; otpError = null
            try {
                if (backend.configuration == null) backend.refreshConfiguration()
                if (isNativePhoneAuthSupported) {
                    val verification = startNativePhoneVerification(normalized)
                    if (verification.verificationId.startsWith("instant:")) {
                        val nativeAuth = verifyNativePhoneCode(verification.verificationId, "")
                        backend.exchangeIdToken("firebase", nativeAuth.idToken, null)
                        draft = draft.copy(phone = nativeAuth.phoneNumber.ifBlank { normalized })
                        provider = "Telefon"
                        continueToPassport()
                    } else {
                        sentPhone = normalized
                        challengeId = verification.verificationId
                        resendSeconds = 60
                        code = ""
                        provider = "Telefon"
                        stage = AuthStage.PHONE_OTP
                    }
                } else {
                    val response = backend.startPhoneOtp(normalized)
                    sentPhone = normalized
                    challengeId = response.s("challengeId")
                    resendSeconds = response.i("resendAfterSeconds", 60)
                    code = response.s("developmentCode")
                    provider = "Telefon"
                    stage = AuthStage.PHONE_OTP
                }
            } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (error: Exception) {
                authError = when {
                    error is BackendFailure && error.code == "AUTH_CANCELLED" -> null
                    error is BackendFailure && error.code == "CONFIGURATION_REQUIRED" -> "Giriş servisi henüz bağlanmadı. Lütfen daha sonra tekrar dene."
                    error is BackendFailure && error.code == "FIREBASE_PHONE_ERROR" -> formatFirebasePhoneAuthError(error.message)
                    else -> error.message
                }
            }
            finally { busy = false }
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MColors.Canvas, tonalElevation = 0.dp,
        scrimColor = MColors.Ink900.copy(alpha = .48f),
        dragHandle = { MPremiumSheetHandle() },
        modifier = Modifier.statusBarsPadding().padding(top = 12.dp)
    ) {
        Column {
        if (busy || externallyBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MColors.Brand)
        authError?.let { Text(it, Modifier.padding(horizontal = 22.dp, vertical = 10.dp), color = MColors.Rose, style = MaterialTheme.typography.bodySmall) }
        AnimatedContent(targetState = stage,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) }, label = "auth_stage") { current ->
            when (current) {
                AuthStage.LEGAL_REVIEW -> Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AuthHeader("Belgeleri gözden geçir", "Hesabınla devam etmeden önce güncel belgeleri incele.", onDismiss)
                    AppLegalLinks()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(legalAccepted, { legalAccepted = it })
                        Text("Güncel kullanım koşullarını ve gizlilik politikasını okudum.", style = MaterialTheme.typography.bodySmall)
                    }
                    MButton("Devam et", { if (!busy && !externallyBusy && backend != null) scope.launch {
                        busy = true; authError = null
                        try {
                            backend.acceptCurrentLegal()
                            val snap = backend.snapshots.value
                            val finalProfile = restoredProfile
                                ?: snap?.let(::BackendSnapshot)?.profile
                                ?: if (snap?.s("role") == "admin") UserProfile(
                                    name = "Yönetici", city = currentCity, area = currentDistrict, bio = "", birthYear = 1990,
                                    phone = "", email = "", skills = emptyList(), verifications = setOf(VerificationLevel.BASIC),
                                    reliability = 100, fairPlay = 5f, verifiedMatches = 0, streakWeeks = 0,
                                    campus = null, company = null, dominantSide = "", availability = emptyList()
                                ) else null
                            finalProfile?.let { onCompletePassport(it, provider) } ?: run { stage = AuthStage.PASSPORT_SETUP }
                        }
                        catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                        catch (e: Exception) { authError = e.message }
                        finally { busy = false }
                    } }, enabled = legalAccepted && !busy && !externallyBusy)
                }
                AuthStage.LOGIN_OPTIONS -> LoginOptionsView(
                    { enter("Apple") }, { enter("Google") }, { authError = null; stage = AuthStage.PHONE_INPUT },
                    { onContinueAsGuest() }, onDismiss, busy = busy || externallyBusy)
                AuthStage.PHONE_INPUT -> PhoneInputView(
                    selectedCountry = selectedCountry,
                    onSelectCountry = { selectedCountry = it; authError = null },
                    phone = phone,
                    onChange = { phone = it; authError = null },
                    onBack = { stage = AuthStage.LOGIN_OPTIONS; authError = null },
                    onSend = ::sendPhoneCode,
                    busy = busy || externallyBusy,
                    cooldown = if (normalizePhoneWithCountry(selectedCountry, phone) == sentPhone) resendSeconds else 0
                )
                AuthStage.PHONE_OTP -> PhoneOtpView(sentPhone, code, otpError,
                    { code = it; otpError = null }, { stage = AuthStage.PHONE_INPUT; authError = null }, {
                        if (!busy && !externallyBusy && backend != null) scope.launch {
                            busy = true; authError = null
                            try {
                                if (isNativePhoneAuthSupported) {
                                    val nativeAuth = verifyNativePhoneCode(challengeId, code)
                                    backend.exchangeIdToken("firebase", nativeAuth.idToken, null)
                                    draft = draft.copy(phone = nativeAuth.phoneNumber.ifBlank { sentPhone })
                                    continueToPassport()
                                } else {
                                    backend.verifyPhoneOtp(challengeId, code)
                                    draft = draft.copy(phone = sentPhone)
                                    continueToPassport()
                                }
                            }
                            catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                            catch (error: Exception) {
                                otpError = if (error is BackendFailure && error.code in setOf("INVALID_CODE", "FIREBASE_PHONE_ERROR")) {
                                    formatFirebasePhoneAuthError(error.message)
                                } else error.message
                            }
                            finally { busy = false }
                        }
                    }, busy = busy || externallyBusy, resendSeconds = resendSeconds, onResend = ::sendPhoneCode)
                AuthStage.PASSPORT_SETUP -> PassportSetupView(draft, { draft = it }, photo, { photo = it },
                    section, { section = it }, provider, { stage = AuthStage.LOGIN_OPTIONS },
                    legalRequired = legalRequired, legalAccepted = legalAccepted, onLegalAccepted = { legalAccepted = it }, phoneLocked = backend != null, busy = busy || externallyBusy, onSave = {
                        if (!busy && !externallyBusy && legalRequired && backend != null) scope.launch {
                            busy = true; authError = null
                            try { backend.acceptCurrentLegal(); onCompletePassport(draft.toProfile().copy(avatar = photo), provider) }
                            catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                            catch (e: Exception) { authError = e.message }
                            finally { busy = false }
                        } else if (!busy && !externallyBusy) onCompletePassport(draft.toProfile().copy(avatar = photo), provider)
                    })
            }
        }
        }
    }
}

@Composable
private fun LoginOptionsView(onSelectApple: () -> Unit, onSelectGoogle: () -> Unit,
    onSelectPhone: () -> Unit, onContinueAsGuest: () -> Unit, onDismiss: () -> Unit, busy: Boolean) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding()
        .padding(horizontal = 22.dp).padding(bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AuthHeader("Matchora’ya Giriş", "Sporcu pasaportunla sahadaki yerini al.", onDismiss)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
            if ("apple" in supportedSignInProviders(storePlatform)) ProviderButton("Apple ile devam et", onSelectApple, Color.Black, Color.White, enabled = !busy) { AppleLogoIcon(Modifier.size(20.dp)) }
            ProviderButton("Google ile devam et", onSelectGoogle, Color.White, MColors.Ink900, enabled = !busy) { GoogleLogoIcon(Modifier.size(19.dp)) }
            ProviderButton("Telefon ile devam et", onSelectPhone, Color.White, MColors.Ink900, enabled = !busy) {
                Icon(Icons.Rounded.PhoneAndroid, null, Modifier.size(20.dp), tint = MColors.BrandDeep)
            }
        }
        OutlinedButton(
            onClick = onContinueAsGuest,
            enabled = !busy,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MColors.Line),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = MColors.SurfaceMuted, contentColor = MColors.Ink700),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Misafir olarak göz at", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
        HorizontalDivider(color = MColors.Line.copy(alpha = 0.6f))
        AppLegalLinks(prominent = true)
    }
}

@Composable
private fun ProviderButton(title: String, action: () -> Unit, color: Color, ink: Color, enabled: Boolean = true, logo: @Composable () -> Unit) {
    Surface(onClick = action, enabled = enabled, shape = RoundedCornerShape(14.dp), color = color,
        border = BorderStroke(1.dp, if (color == Color.Black) Color.Black else MColors.Line), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.width(24.dp), contentAlignment = Alignment.Center) { logo() }
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = ink)
            Icon(Icons.Rounded.ChevronRight, null, Modifier.size(18.dp), tint = ink.copy(alpha = .6f))
        }
    }
}

@Composable
private fun AuthHeader(title: String, subtitle: String, onDismiss: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = RoundedCornerShape(12.dp), color = MColors.BrandTint) {
            Icon(Icons.Rounded.SportsSoccer, null, Modifier.padding(10.dp).size(23.dp), tint = MColors.BrandDeep)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = MColors.Ink900)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(44.dp)) { Icon(Icons.Rounded.Close, "Kapat", tint = MColors.Ink500) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PassportSetupView(draft: PassportDraft, onDraft: (PassportDraft) -> Unit,
    photo: ImageBitmap?, onPhoto: (ImageBitmap) -> Unit, section: Int, onSection: (Int) -> Unit,
    provider: String, onBack: () -> Unit, legalRequired: Boolean = false, legalAccepted: Boolean = false, onLegalAccepted: (Boolean) -> Unit = {}, phoneLocked: Boolean = false, busy: Boolean = false, onSave: () -> Unit) {
    val scroll = rememberScrollState()
    var showLocation by remember { mutableStateOf(false) }
    var showExtras by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val pickPhoto = rememberGalleryPhotoPicker(onPhoto)
    LaunchedEffect(section) { scroll.scrollTo(0); error = null }
    val titles = listOf("Kimlik", "Spor & rol", "Tercihler")
    Column(Modifier.fillMaxWidth().fillMaxHeight(.92f).imePadding()) {
        Column(Modifier.padding(horizontal = 22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AuthHeader("Sporcu pasaportun", "${section + 1} / 3 · ${titles[section]}", onBack)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                titles.forEachIndexed { index, _ -> Box(Modifier.weight(1f).height(3.dp).background(if (index <= section) MColors.Brand else MColors.Line, CircleShape)) }
            }
        }
        Column(Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = 22.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(shape = RoundedCornerShape(20.dp), color = MColors.Ink900, border = BorderStroke(1.dp, Color(0xFF40505B))) {
                Row(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF293D46), MColors.Ink900)))
                    .padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(50.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF46504F)), contentAlignment = Alignment.Center) {
                        if (photo != null) Image(photo, "Profil fotoğrafı", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else Text(draft.name.split(' ').mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("").ifBlank { "M" }, color = Color(0xFFE1C49D), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(draft.name.ifBlank { "Sahadaki yerin hazır" }, color = Color.White, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${draft.sport.emoji} ${draft.sport.label} · ${draft.district}", color = Color(0xFFB8C7CE), style = MaterialTheme.typography.bodySmall)
                        Text("SPORCU PASAPORTU · $provider", color = Color(0xFFE1C49D), fontSize = 9.sp, letterSpacing = 1.sp)
                    }
                }
            }
            when (section) {
                0 -> {
                    Text("Sahada seni tanısınlar", style = MaterialTheme.typography.titleLarge)
                    ProfileEditField("Adın / takma adın", draft.name, { onDraft(draft.copy(name = it.take(40))) })
                    ProfileEditField("Doğum yılı", draft.birthYear, { onDraft(draft.copy(birthYear = it.filter(Char::isDigit).take(4))) }, keyboardType = KeyboardType.Number)
                    Surface(onClick = { showLocation = true }, shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, MColors.Line)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Rounded.LocationOn, null, tint = MColors.BrandDeep)
                            Column(Modifier.weight(1f)) { Text("İl / ilçe", style = MaterialTheme.typography.labelSmall, color = MColors.Ink500); Text("${draft.city} · ${draft.district}", style = MaterialTheme.typography.bodyMedium) }
                            Icon(Icons.Rounded.ChevronRight, "Konumu seç", tint = MColors.Ink500)
                        }
                    }
                    OutlinedButton(onClick = pickPhoto, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Rounded.AddAPhoto, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(if (photo == null) "Profil fotoğrafı ekle · isteğe bağlı" else "Fotoğrafı değiştir")
                    }
                    Text("Konumun ilçe düzeyinde görünür. Fotoğrafını daha sonra da ekleyebilirsin.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
                }
                1 -> {
                    Text("Oyunun ve rolün", style = MaterialTheme.typography.titleLarge)
                    SportPickerField(draft.sport, { sport -> if (sport != null) onDraft(draft.copy(sport = sport, position = positionsForSport(sport).first())) }, allowAll = false)
                    Text("Tercih ettiğin rol", style = MaterialTheme.typography.labelMedium, color = MColors.Ink500)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        positionsForSport(draft.sport).forEach { value -> FilterChip(draft.position == value, { onDraft(draft.copy(position = value)) }, label = { Text(value) }, colors = premiumChoiceChipColors()) }
                    }
                    Text("Kendini nasıl değerlendirirsin?", style = MaterialTheme.typography.labelMedium, color = MColors.Ink500)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SkillLevel.entries.forEach { level -> FilterChip(draft.level == level, { onDraft(draft.copy(level = level)) }, label = { Text(level.label) }, colors = premiumChoiceChipColors()) }
                    }
                    Text("Seviyen başlangıç tercihini belirler. Doğrulanmış maçlarınla sportif geçmişin oluşur.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
                }
                2 -> {
                    Text("Ne zaman oynarsın?", style = MaterialTheme.typography.titleLarge)
                    Text("İsteğe bağlı · uygun davetler için günlerini seç.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        passportDays.forEach { day -> FilterChip(day in draft.days, { onDraft(draft.copy(days = if (day in draft.days) draft.days - day else draft.days + day)) }, label = { Text(day) }, colors = premiumChoiceChipColors()) }
                    }
                    if (draft.days.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TimePickerMenu("Başlangıç", draft.startTime, { onDraft(draft.copy(startTime = it)) }, Modifier.weight(1f))
                        TimePickerMenu("Bitiş", draft.endTime, { onDraft(draft.copy(endTime = it)) }, Modifier.weight(1f))
                    }
                    MCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${draft.sport.emoji} ${draft.sport.label} · ${draft.position}", style = MaterialTheme.typography.titleMedium)
                            Text("${draft.level.label} · ${draft.city} / ${draft.district}", color = MColors.Ink500, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    AppLegalLinks()
                    if (legalRequired) Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = legalAccepted, onCheckedChange = onLegalAccepted)
                        Text("Kullanım koşullarını ve gizlilik politikasını inceledim, kabul ediyorum.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink600)
                    }
                    TextButton(onClick = { showExtras = !showExtras }) { Text(if (showExtras) "Ek bilgileri kapat" else "Ek bilgiler · isteğe bağlı", color = MColors.Ink600); Icon(if (showExtras) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null) }
                    AnimatedVisibility(showExtras) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            ProfileEditField("Kısa biyografi", draft.bio, { onDraft(draft.copy(bio = it.take(280))) }, singleLine = false, minLines = 2, maxLines = 4)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                MFloatingDropdownBox("Baskın el", draft.dominantHand, listOf("Sağ el", "Sol el", "Her ikisi"), { it }, { onDraft(draft.copy(dominantHand = it)) }, modifier = Modifier.weight(1f))
                                MFloatingDropdownBox("Baskın ayak", draft.dominantFoot, listOf("Sağ ayak", "Sol ayak", "Her ikisi"), { it }, { onDraft(draft.copy(dominantFoot = it)) }, modifier = Modifier.weight(1f))
                            }
                            ProfileEditField("Forma numarası", draft.jerseyNumber, { onDraft(draft.copy(jerseyNumber = it.filter(Char::isDigit).take(2))) }, keyboardType = KeyboardType.Number)
                            ProfileEditField("Telefon", draft.phone, { onDraft(draft.copy(phone = it.take(20))) }, keyboardType = KeyboardType.Phone, readOnly = phoneLocked)
                            if (phoneLocked) Text("Telefonunu pasaportunu oluşturduktan sonra İletişimi doğrula bölümünden ekleyebilir veya değiştirebilirsin.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
                            ProfileEditField("E-posta", draft.email, { onDraft(draft.copy(email = it.take(100))) }, keyboardType = KeyboardType.Email)
                            ProfileEditField("Kampüs / üniversite", draft.campus, { onDraft(draft.copy(campus = it.take(80))) })
                            ProfileEditField("Şirket / kurum", draft.company, { onDraft(draft.copy(company = it.take(80))) })
                        }
                    }
                }
            }
        }
        Surface(color = Color.White, shadowElevation = 6.dp) {
            Column(Modifier.navigationBarsPadding().padding(horizontal = 22.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MColors.Rose) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedIconButton(onClick = { if (section == 0) onBack() else onSection(section - 1) }, modifier = Modifier.size(50.dp), shape = RoundedCornerShape(14.dp)) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Geri") }
                    MButton(if (section < 2) "Devam et · ${titles[section + 1]}" else "Pasaportumu oluştur", {
                        val validation = if (section == 0) draft.identityError() else if (section == 2) draft.identityError() ?: draft.preferencesError() else null
                        if (validation != null) error = validation else if (section < 2) onSection(section + 1) else onSave()
                    }, Modifier.weight(1f), icon = if (section == 2) Icons.Rounded.Check else null, enabled = !busy && (section < 2 || !legalRequired || legalAccepted))
                }
            }
        }
    }
    if (showLocation) CityAndLocationSelectorSheet(draft.city, draft.district, false, { showLocation = false }, { city, district, _ -> onDraft(draft.copy(city = city, district = district)); showLocation = false })
}

@Composable
private fun PhoneInputView(
    selectedCountry: PhoneCountryCode,
    onSelectCountry: (PhoneCountryCode) -> Unit,
    phone: String,
    onChange: (String) -> Unit,
    onBack: () -> Unit,
    onSend: () -> Unit,
    busy: Boolean,
    cooldown: Int
) {
    val isPhoneValid = normalizePhoneWithCountry(selectedCountry, phone) != null
    Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(22.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AuthHeader("Telefon ile devam et", "Şifresiz SMS doğrulamasıyla giriş veya kayıt.", onBack)
        PhoneNumberFieldWithCountryPicker(
            selectedCountry = selectedCountry,
            onSelectCountry = onSelectCountry,
            localPhone = phone,
            onLocalPhoneChange = onChange,
            label = "Telefon numaran",
            enabled = !busy
        )
        Text("Numarana 6 haneli tek kullanımlık bir SMS kodu göndereceğiz. Hesabın varsa doğrudan giriş yaparsın; yoksa sporcu pasaportunu oluşturursun.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink600)
        MButton(if (cooldown > 0) "Yeni kod için $cooldown sn bekle" else "Doğrulama kodu gönder", onSend, enabled = !busy && cooldown == 0 && isPhoneValid, icon = Icons.Rounded.Sms)
    }
}

@Composable
private fun PhoneOtpView(phone: String, code: String, error: String?, onCode: (String) -> Unit, onBack: () -> Unit,
    onVerify: () -> Unit, busy: Boolean, resendSeconds: Int, onResend: () -> Unit) {
    Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(22.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AuthHeader("Numaranı doğrula", phone, onBack)
        Text("SMS ile gelen altı haneli doğrulama kodunu gir.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink600)
        OutlinedTextField(code, { onCode(it.filter(Char::isDigit).take(6)) }, label = { Text("6 haneli SMS kodu") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), isError = error != null,
            enabled = !busy, singleLine = true, colors = premiumTextFieldColors(), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
        error?.let { Text(it, color = MColors.Rose, style = MaterialTheme.typography.bodySmall) }
        TextButton(onClick = onResend, enabled = !busy && resendSeconds == 0) { Text(if (resendSeconds > 0) "Tekrar kod iste · $resendSeconds sn" else "Tekrar kod iste") }
        MButton("Doğrula ve devam et", onVerify, enabled = !busy && code.length == 6)
    }
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
    keyboardType: KeyboardType = KeyboardType.Text,
    readOnly: Boolean = false
) {
    OutlinedTextField(
        textStyle = MaterialTheme.typography.bodyMedium,
        value = value,
        onValueChange = onValueChange,
        readOnly = readOnly,
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
