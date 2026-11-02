package com.matchora.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.matchora.app.domain.EventStatus
import com.matchora.app.state.*
import com.matchora.app.ui.theme.MColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivitySheet(title: String, subtitle: String, onDismiss: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Rounded.ManageAccounts, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.statusBarsPadding().padding(top = 12.dp), containerColor = MColors.Canvas,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), dragHandle = { MPremiumSheetHandle() }) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            MSheetHeading(title, subtitle, icon, onDismiss)
            content()
        }
    }
}

@Composable
fun AccountSettingsSheet(privacy: PrivacyPrefs, onPrivacy: (PrivacyPrefs) -> Unit,
    notifications: NotificationPrefs, onNotifications: (NotificationPrefs) -> Unit,
    blockedChats: List<ChatThread>, onUnblock: (String) -> Unit,
    supportRequests: List<SupportRequest>, reports: List<ModerationReport>,
    onSupport: (String, String, String) -> Unit, onLogout: () -> Unit,
    onRequestDeletion: () -> Unit, onDismiss: () -> Unit) {
    var section by remember { mutableIntStateOf(0) }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Teknik sorun") }
    var confirmDelete by remember { mutableStateOf(false) }
    var deletionRequested by remember { mutableStateOf(false) }
    ActivitySheet("Hesap ve tercihler", "Spor hayatının kontrolü sende", onDismiss) {
        MSegmentedControl(listOf("Gizlilik", "Bildirim", "Destek"), section, { section = it })
        when (section) {
            0 -> {
                SettingsGroup("Görünürlük") {
                    PreferenceRow("Sıralamada görün", "Yerel lig tablolarındaki sporcu kartın", privacy.showInRankings) { onPrivacy(privacy.copy(showInRankings = it)) }
                    PreferenceRow("Güvenilirlik puanı", "Sporcu kartında göster", privacy.showReliability) { onPrivacy(privacy.copy(showReliability = it)) }
                    PreferenceRow("Puan geçmişi", "Maçlarla oluşan gelişimini paylaş", privacy.showRatingHistory) { onPrivacy(privacy.copy(showRatingHistory = it)) }
                    PreferenceRow("Akıllı eşleşme sinyalleri", "Oyun tercihlerini eşleşmede kullan", privacy.chemistrySignals) { onPrivacy(privacy.copy(chemistrySignals = it)) }
                }
                MFloatingDropdownBox("Mesaj istekleri", privacy.dmPolicy, listOf("Ortak etkinlik sonrası", "Herkes", "Kapalı"), { it }, { onPrivacy(privacy.copy(dmPolicy = it)) }, icon = Icons.Rounded.ChatBubbleOutline)
                Text("Konumun ilçe düzeyinde paylaşılır; canlı kesin konumun profilinde gösterilmez.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
                SettingsGroup("Engellenen sohbetler") {
                    if (blockedChats.isEmpty()) Text("Engellediğin sohbet yok.", color = MColors.Ink500, style = MaterialTheme.typography.bodySmall)
                    blockedChats.forEach { thread -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(thread.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { onUnblock(thread.id) }) { Text("Engeli kaldır") }
                    } }
                }
            }
            1 -> SettingsGroup("Bildirim tercihlerin") {
                PreferenceRow("Maç hatırlatmaları", "Yaklaşan maçlar ve kadro güncellemeleri", notifications.upcomingMatch) { onNotifications(notifications.copy(upcomingMatch = it)) }
                PreferenceRow("Acil oyuncu çağrıları", "Yakınındaki eksik kadrolar", notifications.emergencyFill) { onNotifications(notifications.copy(emergencyFill = it)) }
                PreferenceRow("Meydan okumalar", "Takımın için gelen teklifler", notifications.challenges) { onNotifications(notifications.copy(challenges = it)) }
                PreferenceRow("Bekleme listesi", "Açılan kontenjanlar", notifications.waitlist) { onNotifications(notifications.copy(waitlist = it)) }
                PreferenceRow("Sosyal bildirimler", "Mesajlar ve saha anıları", notifications.social) { onNotifications(notifications.copy(social = it)) }
                PreferenceRow("Sessiz saatler", "${notifications.quietFrom} – ${notifications.quietTo}", notifications.quietHours) { onNotifications(notifications.copy(quietHours = it)) }
                PreferenceRow("Kampanyalar", "İsteğe bağlı duyuru ve teklifler", notifications.marketing) { onNotifications(notifications.copy(marketing = it)) }
            }
            2 -> {
                MFloatingDropdownBox("Konu", category, listOf("Teknik sorun", "Hesap", "Ödeme", "Maç ve güvenlik"), { it }, { category = it })
                OutlinedTextField(subject, { subject = it.take(100) }, label = { Text("Başlık") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(14.dp))
                OutlinedTextField(message, { message = it.take(1000) }, label = { Text("Nasıl yardımcı olalım?") }, modifier = Modifier.fillMaxWidth(), minLines = 3, shape = RoundedCornerShape(14.dp))
                MButton("Destek talebini kaydet", { onSupport(subject.trim(), message.trim(), category); subject = ""; message = "" }, enabled = subject.trim().length >= 3 && message.trim().length >= 10)
                SettingsGroup("Taleplerin") {
                    if (supportRequests.isEmpty() && reports.isEmpty()) Text("Henüz bir talep yok.", color = MColors.Ink500)
                    supportRequests.forEach { Text("${it.subject}\n${it.category} · Bu oturumda kaydedildi", style = MaterialTheme.typography.bodySmall) }
                    reports.forEach { Text("${it.target}\n${it.reason} · Bildirim kaydı", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        HorizontalDivider(color = MColors.Line)
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Oturumu kapat") }
        TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth(), enabled = !deletionRequested) { Text(if (deletionRequested) "Hesap silme talebi kaydedildi" else "Hesabımı silmek istiyorum", color = MColors.Rose) }
    }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Hesap silme talebi") },
        text = { Text("Hesap ve kişisel verilerinin silinmesi için talep oluşturulur. Mevcut önizlemede yalnızca talep kaydı tutulur.") },
        confirmButton = { TextButton(onClick = { onRequestDeletion(); deletionRequested = true; confirmDelete = false }) { Text("Talep oluştur", color = MColors.Rose) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Vazgeç") } })
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    MCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); content() } }
}

@Composable
private fun PreferenceRow(title: String, detail: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) { Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold); Text(detail, style = MaterialTheme.typography.bodySmall, color = MColors.Ink500) }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyActivitySheet(events: List<SportEvent>, joinedIds: Set<String>, waitlistedIds: Set<String>,
    pendingRequests: List<JoinRequest>, reservations: List<VenuePayment>, onOpenEvent: (SportEvent) -> Unit,
    onCancelReservation: (VenuePayment) -> Unit, onDismiss: () -> Unit) {
    var section by remember { mutableIntStateOf(0) }
    var cancel by remember { mutableStateOf<VenuePayment?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = MColors.Canvas, dragHandle = { MPremiumSheetHandle() }) {
        LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { MSheetHeading("Oyun takvimim", "Maçlar, başvurular ve rezervasyonlar", Icons.Rounded.CalendarMonth, onDismiss) }
            item { MSegmentedControl(listOf("Maçlarım", "Başvurular", "Sahalar"), section, { section = it }) }
            val myEvents = events.filter { if (section == 0) it.id in joinedIds || it.hostId == DemoData.ME else it.id in waitlistedIds || pendingRequests.any { request -> request.eventId == it.id } }
            if (section < 2) {
                if (myEvents.isEmpty()) item { Text(if (section == 0) "Katıldığın maçlar burada görünecek." else "Bekleyen başvurun veya sıra kaydın yok.", color = MColors.Ink500) }
                items(myEvents, key = { it.id }) { event -> MCard(Modifier.fillMaxWidth(), onClick = { onOpenEvent(event) }) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) { Text("${event.displaySportEmoji} ${event.title}", style = MaterialTheme.typography.titleMedium); Text("${event.day} · ${event.time} · ${event.venue}", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500); Text(if (event.id in waitlistedIds) "Bekleme listesi" else if (section == 1) "Kadro yanıtı bekleniyor" else event.status.label, style = MaterialTheme.typography.labelSmall, color = MColors.BrandDeep) }
                        Icon(Icons.Rounded.ChevronRight, "Maçı aç", tint = MColors.Ink500)
                    }
                } }
            } else {
                if (reservations.isEmpty()) item { Text("Saha rezervasyonların burada görünecek.", color = MColors.Ink500) }
                items(reservations, key = { it.id }) { payment -> MCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(payment.venue, style = MaterialTheme.typography.titleMedium)
                        Text(payment.description, style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
                        Text("₺${payment.amountTry} · ${payment.method} · ${payment.status.label}", color = MColors.BrandDeep, style = MaterialTheme.typography.labelMedium)
                        if (payment.status == PaymentStatus.PENDING) TextButton(onClick = { cancel = payment }) { Text("Rezervasyonu iptal et", color = MColors.Rose) }
                    }
                } }
            }
        }
    }
    cancel?.let { payment -> AlertDialog(onDismissRequest = { cancel = null }, title = { Text("Rezervasyon iptal edilsin mi?") }, text = { Text("${payment.venue} rezervasyonunu iptal edeceksin.") }, confirmButton = { TextButton(onClick = { onCancelReservation(payment); cancel = null }) { Text("İptal et") } }, dismissButton = { TextButton(onClick = { cancel = null }) { Text("Vazgeç") } }) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MatchResultSheet(event: SportEvent, result: MatchResultDraft?, canSubmit: Boolean,
    onSubmit: (MatchResultDraft) -> Unit, onDispute: (String) -> Unit, onDismiss: () -> Unit) {
    var home by remember { mutableStateOf(result?.homeScore?.toString().orEmpty()) }
    var away by remember { mutableStateOf(result?.awayScore?.toString().orEmpty()) }
    var note by remember { mutableStateOf(result?.note.orEmpty()) }
    var fairPlay by remember { mutableIntStateOf(result?.fairPlay ?: 5) }
    var reason by remember { mutableStateOf("") }
    ActivitySheet("Maç sonucu", event.title, onDismiss, Icons.Rounded.Scoreboard) {
        if (result != null) {
            MFeatureHeader("${result.homeScore} – ${result.awayScore}", if (result.disputed) "İtiraz kaydı var · inceleme bekleniyor" else "Sonuç bildirildi · doğrulama bekleniyor", Icons.Rounded.Scoreboard)
            Text(result.note.ifBlank { "Sonuç notu eklenmedi." }, color = MColors.Ink500)
            Text("Fair play değerlendirmen: ${result.fairPlay}/5", style = MaterialTheme.typography.bodySmall)
            if (!result.disputed) {
                OutlinedTextField(reason, { reason = it.take(500) }, label = { Text("İtiraz gerekçen") }, modifier = Modifier.fillMaxWidth(), minLines = 2, shape = RoundedCornerShape(16.dp), colors = premiumTextFieldColors())
                MButton("Sonuca itiraz et", { onDispute(reason.trim()) }, enabled = reason.trim().length >= 10, isPrimary = false)
            }
        } else if (canSubmit) {
            Text("Skoru ve saha deneyimini kaydet. Sonuç doğrulanmadan MMR veya güvenilirlik puanı değişmez.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(home, { home = it.filter(Char::isDigit).take(3) }, label = { Text("Ev sahibi") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(16.dp), colors = premiumTextFieldColors())
                OutlinedTextField(away, { away = it.filter(Char::isDigit).take(3) }, label = { Text("Rakip") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(16.dp), colors = premiumTextFieldColors())
            }
            Text("Fair play", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { (1..5).forEach { score -> FilterChip(fairPlay == score, { fairPlay = score }, label = { Text("$score ★") }, colors = premiumChoiceChipColors()) } }
            OutlinedTextField(note, { note = it.take(500) }, label = { Text("Maç notu · isteğe bağlı") }, minLines = 2, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = premiumTextFieldColors())
            MButton("Sonucu bildir", { onSubmit(MatchResultDraft(event.id, home.toInt(), away.toInt(), note.trim(), fairPlay)) }, enabled = home.toIntOrNull() != null && away.toIntOrNull() != null)
        } else Text("Sonuç kaydı tamamlanan veya devam eden maçlarda açılır.", color = MColors.Ink500)
    }
}

@Composable
fun ContactVerificationSheet(initialPhone: String, onRecordRequest: (String) -> Unit, onDismiss: () -> Unit) {
    var phone by remember { mutableStateOf(initialPhone.removePrefix("+90")) }
    var step by remember { mutableIntStateOf(0) }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    ActivitySheet("İletişim doğrulaması", "Sahada güvenle buluş", onDismiss, Icons.Rounded.PhoneIphone) {
        when (step) {
            0 -> {
                Text("Cep telefonu numaranı doğrulama akışına ekle. Numaran sporcu kartında herkese açık gösterilmez.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
                OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(10) }, prefix = { Text("+90 ") }, label = { Text("Cep telefonu") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp), colors = premiumTextFieldColors())
                MButton("Doğrulama koduna geç", { step = 1 }, enabled = normalizeTurkishPhone(phone) != null)
            }
            1 -> {
                Text("Önizleme kodu: 2026. Bu akış SMS göndermez; hesabına doğrulama rozeti eklemez.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
                OutlinedTextField(code, { code = it.filter(Char::isDigit).take(4); error = null }, label = { Text("Doğrulama kodu") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth(), singleLine = true, isError = error != null, shape = RoundedCornerShape(16.dp), colors = premiumTextFieldColors())
                error?.let { Text(it, color = MColors.Rose) }
                MButton("Doğrulama talebini kaydet", {
                    if (code != "2026") error = "Kod eşleşmedi. Tekrar dene."
                    else { onRecordRequest(normalizeTurkishPhone(phone).orEmpty()); step = 2 }
                }, enabled = code.length == 4)
                TextButton(onClick = { step = 0 }) { Text("Numarayı değiştir") }
            }
            else -> {
                MFeatureHeader("Talebin kaydedildi", "Hesap → Destek bölümünden ulaşabilirsin", Icons.Rounded.VerifiedUser)
                MButton("Tamam", onDismiss)
            }
        }
    }
}

@Composable
fun PhotoVerificationSheet(profile: UserProfile, onPhotoRequest: (androidx.compose.ui.graphics.ImageBitmap) -> Unit, onDismiss: () -> Unit) {
    var photo by remember { mutableStateOf(profile.avatar) }
    var submitted by remember { mutableStateOf(false) }
    val pick = rememberGalleryPhotoPicker { photo = it; submitted = false }
    ActivitySheet("Fotoğraf doğrulaması", "Sporcu kartını güvenle tamamla", onDismiss, Icons.Rounded.VerifiedUser) {
        Text("Yüzünün net göründüğü, tek başına olduğun bir fotoğraf seç. İnceleme tamamlanmadan doğrulama rozeti verilmez.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
        photo?.let { androidx.compose.foundation.Image(it, "İncelemeye eklenecek fotoğraf", Modifier.fillMaxWidth().height(200.dp), contentScale = androidx.compose.ui.layout.ContentScale.Fit) }
        OutlinedButton(onClick = pick, modifier = Modifier.fillMaxWidth()) { Text(if (photo == null) "Fotoğraf seç" else "Fotoğrafı değiştir") }
        MButton(if (submitted) "İnceleme talebi kaydedildi" else "İnceleme talebi oluştur", { photo?.let { onPhotoRequest(it); submitted = true } }, enabled = photo != null && !submitted)
    }
}
