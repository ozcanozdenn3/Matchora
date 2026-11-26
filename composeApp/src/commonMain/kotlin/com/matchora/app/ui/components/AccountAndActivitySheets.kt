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
import com.matchora.app.data.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import com.matchora.app.domain.EventStatus
import com.matchora.app.state.*
import com.matchora.app.ui.theme.MColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ActivitySheet(title: String, subtitle: String, onDismiss: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Rounded.ManageAccounts, confirmDismiss: () -> Boolean = { true }, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { it != SheetValue.Hidden || confirmDismiss() }),
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
    var showModeration by remember { mutableStateOf(false) }
    var showVenueOperator by remember { mutableStateOf(false) }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Teknik sorun") }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    var deletionRequested by remember { mutableStateOf(false) }
    val repository = LocalBackendRepository.current
    val toastNotifier = LocalToastNotifier.current
    val snapshot by (repository?.snapshots ?: kotlinx.coroutines.flow.MutableStateFlow<kotlinx.serialization.json.JsonObject?>(null)).collectAsState()
    val blockedPeople = remember(snapshot) { snapshot?.let(::blockedPlayerEntries).orEmpty() }
    val command = rememberBackendAction()
    val pendingDeletion = (snapshot?.get("deletionRequest") as? kotlinx.serialization.json.JsonObject)?.get("status")?.toString() == "\"pending\""
    var deletionError by remember { mutableStateOf<String?>(null) }
    val deletionScope = rememberCoroutineScope()
    var exporting by remember { mutableStateOf(false) }
    var sessions by remember { mutableStateOf<List<JsonObject>?>(null) }
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
                SettingsGroup(if (repository?.enabled == true) "Engellenen sporcular" else "Engellenen sohbetler") {
                    if (repository?.enabled == true) {
                        if (blockedPeople.isEmpty()) Text("Engellediğin sporcu yok.", color = MColors.Ink500, style = MaterialTheme.typography.bodySmall)
                        blockedPeople.forEach { player -> Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(player.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = { command("user.block", apiPayload("userId" to player.userId, "blocked" to false)) }) { Text("Engeli kaldır") }
                        } }
                    } else {
                        if (blockedChats.isEmpty()) Text("Engellediğin sohbet yok.", color = MColors.Ink500, style = MaterialTheme.typography.bodySmall)
                        blockedChats.forEach { thread -> Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(thread.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = { onUnblock(thread.id) }) { Text("Engeli kaldır") }
                        } }
                    }
                }
            }
            1 -> SettingsGroup("Bildirim tercihlerin") {
                NotificationPermissionButton()
                PreferenceRow("Maç hatırlatmaları", "Yaklaşan maçlar ve kadro güncellemeleri", notifications.upcomingMatch) { onNotifications(notifications.copy(upcomingMatch = it)) }
                PreferenceRow("Acil oyuncu çağrıları", "Yakınındaki eksik kadrolar", notifications.emergencyFill) { onNotifications(notifications.copy(emergencyFill = it)) }
                PreferenceRow("Meydan okumalar", "Takımın için gelen teklifler", notifications.challenges) { onNotifications(notifications.copy(challenges = it)) }
                PreferenceRow("Bekleme listesi", "Açılan kontenjanlar", notifications.waitlist) { onNotifications(notifications.copy(waitlist = it)) }
                PreferenceRow("Sosyal bildirimler", "Mesajlar ve saha anıları", notifications.social) { onNotifications(notifications.copy(social = it)) }
                PreferenceRow("Sessiz saatler", "${notifications.quietFrom} – ${notifications.quietTo}", notifications.quietHours) { onNotifications(notifications.copy(quietHours = it)) }
                PreferenceRow("Haftalık spor özeti", "Doğrulanmış maçların ve MMR değişimin", notifications.weeklyDigest) { onNotifications(notifications.copy(weeklyDigest = it)) }
                PreferenceRow("Yeniden maç önerileri", "İkinizin de tekrar oynamak istediği sporcular", notifications.rematch) { onNotifications(notifications.copy(rematch = it)) }
                NotificationDeliveryControls(notifications, onNotifications)
                PreferenceRow("Kampanyalar", "İsteğe bağlı duyuru ve teklifler", notifications.marketing) { onNotifications(notifications.copy(marketing = it)) }
            }
            2 -> {
                MFloatingDropdownBox("Konu", category, listOf("Teknik sorun", "Hesap", "Ödeme", "Maç ve güvenlik"), { it }, { category = it }, icon = Icons.Rounded.SupportAgent)
                MFloatingInputBox("Başlık", subject, { subject = it.take(100) }, placeholder = "Kısaca konuyu belirt", icon = Icons.Rounded.Title)
                MFloatingInputBox("Nasıl yardımcı olalım?", message, { message = it.take(1000) }, placeholder = "Yaşadığın durumu veya talebini detaylıca yaz...", icon = Icons.Rounded.EditNote, singleLine = false, minLines = 3, maxLines = 5)
                MButton("Destek talebini kaydet", { onSupport(subject.trim(), message.trim(), category); subject = ""; message = "" }, enabled = subject.trim().length >= 3 && message.trim().length >= 10)
                AccountHistoryButton("support", "Tüm destek geçmişi")
                AccountHistoryButton("reports", "Tüm şikâyet geçmişi")
                SettingsGroup("Taleplerin") {
                    if (supportRequests.isEmpty() && reports.isEmpty()) Text("Henüz bir talep yok.", color = MColors.Ink500)
                    supportRequests.forEach { request ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            val state = when (request.status) { "resolved" -> "Yanıtlandı"; "rejected" -> "Kapandı"; else -> "İnceleniyor" }
                            Text("${request.subject}\n${request.category} · $state", style = MaterialTheme.typography.bodySmall)
                            request.response?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MColors.Ink700) }
                        }
                    }
                    reports.forEach {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("${it.target}\n${it.reason} · ${when (it.status) { "resolved" -> "İşlendi"; "dismissed" -> "Kapandı"; else -> "İnceleniyor" }}", style = MaterialTheme.typography.bodySmall)
                            it.resolutionReason?.let { res -> Text(res, style = MaterialTheme.typography.bodySmall, color = MColors.Ink700) }
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = MColors.Line)
        SettingsGroup("Bilgiler ve bağlantılar") {
            AppLegalLinks()
            LegalReviewButton()
            AppLinkButton(AppLink.GUIDELINES)
            AppLinkButton(AppLink.DELETION)
            AppLinkButton(AppLink.SUPPORT)
        }
        if (snapshot?.s("role") in listOf("admin", "venue_owner")) {
            MPremiumMenuRow(
                text = "Tesis yönetimi",
                subtitle = "Saha saatleri, rezervasyonlar ve tesis bilgileri",
                icon = Icons.Rounded.Stadium,
                onClick = { showVenueOperator = true }
            )
        }
        OutlinedButton(onClick = { confirmLogout = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("Oturumu kapat", color = MColors.Rose, fontWeight = FontWeight.Bold) }
        if (repository?.enabled == true) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MPillActionButton(
                    text = if (exporting) "Hazırlanıyor…" else "Verilerimi dışa aktar",
                    icon = Icons.Rounded.Download,
                    tone = MPillTone.NEUTRAL,
                    compact = true,
                    enabled = !exporting,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        deletionScope.launch {
                            exporting = true
                            try {
                                shareAccountExport(repository.exportAccount().toString())
                                toastNotifier?.invoke("Veri dışa aktarıldı", "✓")
                            }
                            catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                            catch (error: Exception) {
                                val msg = (error as? BackendFailure)?.message ?: "Verilerin dışa aktarılamadı."
                                deletionError = msg
                                toastNotifier?.invoke(msg, "⚠️")
                            }
                            finally { exporting = false }
                        }
                    }
                )
                MPillActionButton(
                    text = "Açık oturumlarım",
                    icon = Icons.Rounded.Devices,
                    tone = MPillTone.NEUTRAL,
                    compact = true,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        deletionScope.launch {
                            runCatching { repository.activeSessions().a("items") }
                                .onSuccess { sessions = it }
                                .onFailure {
                                    val msg = (it as? BackendFailure)?.message ?: "Oturumlar yüklenemedi."
                                    deletionError = msg
                                    toastNotifier?.invoke(msg, "⚠️")
                                }
                        }
                    }
                )
            }
        }
        TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth(), enabled = if (repository?.enabled == true) !pendingDeletion else !deletionRequested) { Text(if (pendingDeletion || deletionRequested) "Hesap silme talebi kaydedildi" else "Hesabımı silmek istiyorum", color = MColors.Rose) }
        if (pendingDeletion) TextButton(onClick = { deletionScope.launch {
            runCatching { repository?.command("account.delete.cancel", com.matchora.app.data.apiPayload(), repository.requestKey("cancel-deletion")) }
                .onSuccess { toastNotifier?.invoke("Hesap silme talebi geri çekildi", "↩️") }
                .onFailure {
                    val msg = (it as? BackendFailure)?.message ?: "Silme talebi geri çekilemedi."
                    deletionError = msg
                    toastNotifier?.invoke(msg, "⚠️")
                }
        } }) { Text("Silme talebini geri çek") }
    }
    if (showModeration) ModerationSheet { showModeration = false }
    if (showVenueOperator) VenueOperatorSheet { showVenueOperator = false }
    if (confirmLogout) AlertDialog(
        onDismissRequest = { confirmLogout = false },
        title = { Text("Oturum kapatılsın mı?") },
        text = { Text("Tekrar giriş yaparak hesabına ve verilerine erişebilirsin.") },
        confirmButton = { TextButton(onClick = { confirmLogout = false; onLogout() }) { Text("Çıkış Yap", color = MColors.Rose) } },
        dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Vazgeç") } }
    )
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Hesap silme talebi") },
        text = { Text(if (repository?.enabled == true) "Hesabın ve kişisel verilerin sunucuda silinmek üzere sıraya alınır. İşlem tamamlanmadan bu ekrandan talebini geri çekebilirsin. Saklanması gereken işlem kayıtları anonim hesap kimliğiyle tutulur." else "Önizlemede yalnızca talep kaydı oluşturulur.") },
        confirmButton = { TextButton(onClick = { onRequestDeletion(); deletionRequested = true; confirmDelete = false }) { Text("Talep oluştur", color = MColors.Rose) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Vazgeç") } })
    deletionError?.let { error -> AlertDialog(onDismissRequest = { deletionError = null }, title = { Text("İşlem tamamlanamadı") }, text = { Text(error) }, confirmButton = { TextButton(onClick = { deletionError = null }) { Text("Tamam") } }) }
    sessions?.let { items -> AlertDialog(onDismissRequest = { sessions = null }, title = { Text("Açık oturumlar") },
        text = { Column { items.forEach { session -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (session.b("current")) "Bu cihaz" else "Diğer oturum", Modifier.weight(1f))
            if (!session.b("current")) TextButton(onClick = { deletionScope.launch {
                runCatching { repository?.command("account.session.revoke", apiPayload("sessionId" to session.s("id")), repository.requestKey("revoke")) }
                    .onSuccess {
                        sessions = items.filter { it.s("id") != session.s("id") }
                        toastNotifier?.invoke("Oturum kapatıldı", "✓")
                    }.onFailure {
                        val msg = (it as? BackendFailure)?.message ?: "Oturum kapatılamadı."
                        deletionError = msg
                        toastNotifier?.invoke(msg, "⚠️")
                    }
            } }) { Text("Kapat", color = MColors.Rose) }
        } } } }, confirmButton = { TextButton(onClick = { sessions = null }) { Text("Tamam") } }) }
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MyActivitySheet(events: List<SportEvent>, joinedIds: Set<String>, waitlistedIds: Set<String>,
    pendingRequests: List<JoinRequest>, reservations: List<VenuePayment>, onOpenEvent: (SportEvent) -> Unit,
    onCancelReservation: (VenuePayment) -> Unit, onDismiss: () -> Unit,
    onCreateListingForReservation: ((VenuePayment) -> Unit)? = null) {
    val hasServerHistory = LocalBackendRepository.current != null
    var review by remember { mutableStateOf<VenuePayment?>(null) }
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
                if (hasServerHistory) item { AccountHistoryButton("reservations", "Tüm rezervasyon geçmişi") }
                if (reservations.isEmpty()) item { Text("Saha rezervasyonların burada görünecek.", color = MColors.Ink500) }
                items(reservations, key = { it.id }) { payment -> MCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(payment.venue, style = MaterialTheme.typography.titleMedium)
                        Text(payment.description, style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
                        Text("₺${payment.amountLabel} · ${payment.method} · ${payment.status.label}", color = MColors.BrandDeep, style = MaterialTheme.typography.labelMedium)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (payment.bookingStatus == "completed" && payment.status == PaymentStatus.PAID) {
                                MPillActionButton("Tesisi değerlendir", icon = Icons.Rounded.StarOutline, tone = MPillTone.NEUTRAL, compact = true, onClick = { review = payment })
                            }
                            if (payment.status != PaymentStatus.CANCELLED && payment.status != PaymentStatus.REFUNDED && payment.bookingStatus != "completed" && onCreateListingForReservation != null) {
                                MPillActionButton("Bu saat için maç ilanı aç", icon = Icons.Rounded.Add, tone = MPillTone.BRAND, compact = true, onClick = { onCreateListingForReservation(payment) })
                            }
                            if (payment.status == PaymentStatus.PENDING) {
                                MPillActionButton("Rezervasyonu iptal et", icon = Icons.Rounded.Close, tone = MPillTone.DANGER, compact = true, onClick = { cancel = payment })
                            }
                        }
                    }
                } }
            }
        }
    }
    review?.let { VenueReviewEditorSheet(it) { review = null } }
    cancel?.let { payment -> AlertDialog(onDismissRequest = { cancel = null }, title = { Text("Rezervasyon iptal edilsin mi?") }, text = { Text("${payment.venue} rezervasyonunu iptal edeceksin.") }, confirmButton = { TextButton(onClick = { onCancelReservation(payment); cancel = null }) { Text("İptal et", color = MColors.Rose) } }, dismissButton = { TextButton(onClick = { cancel = null }) { Text("Vazgeç") } }) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MatchResultSheet(event: SportEvent, result: MatchResultDraft?, canSubmit: Boolean,
    onSubmit: (MatchResultDraft) -> Unit, onDispute: (String) -> Unit, onDismiss: () -> Unit,
    onConfirm: (() -> Unit)? = null, onOpenFeedback: (() -> Unit)? = null, players: List<Player> = emptyList(), isCheckedIn: Boolean = true) {
    var home by remember(result) { mutableStateOf(result?.homeScore?.toString().orEmpty()) }
    var away by remember(result) { mutableStateOf(result?.awayScore?.toString().orEmpty()) }
    var note by remember(result) { mutableStateOf(result?.note.orEmpty()) }
    var fairPlay by remember(result) { mutableIntStateOf(result?.fairPlay ?: 5) }
    var reason by remember { mutableStateOf("") }
    var editingPending by remember(result) { mutableStateOf(false) }
    val backendMode = LocalBackendRepository.current?.enabled == true
    var homeIds by remember(event.id, result) { mutableStateOf((result?.homeParticipantIds?.takeIf { it.isNotEmpty() } ?: event.homeParticipantIds).toSet()) }
    var awayIds by remember(event.id, result) { mutableStateOf((result?.awayParticipantIds?.takeIf { it.isNotEmpty() } ?: event.awayParticipantIds).toSet()) }
    val sidesReady = homeIds.isNotEmpty() && awayIds.isNotEmpty() && homeIds.size + awayIds.size == event.participantIds.distinct().size
    val submitterOnHome = result?.submittedBy in result?.homeParticipantIds.orEmpty()
    val submitterOnAway = result?.submittedBy in result?.awayParticipantIds.orEmpty()
    val meOnOpposingSide = when {
        submitterOnHome -> DemoData.ME in result?.awayParticipantIds.orEmpty()
        submitterOnAway -> DemoData.ME in result?.homeParticipantIds.orEmpty()
        else -> result?.submittedBy != DemoData.ME
    }
    ActivitySheet("Maç sonucu", event.title, onDismiss, Icons.Rounded.Scoreboard) {
        RatingStatusCard(event.id)
        if (result != null && !editingPending) {
            MFeatureHeader("${result.homeScore} – ${result.awayScore}", when { result.status == "verified" -> "Sonuç doğrulandı"; result.status == "rejected" -> "Sonuç reddedildi"; result.disputed -> "İtiraz kaydı var · inceleme bekleniyor"; else -> "Sonuç bildirildi · doğrulama bekleniyor" }, Icons.Rounded.Scoreboard)
            if (onConfirm != null && result.status == "pending" && result.submittedBy != DemoData.ME && meOnOpposingSide && isCheckedIn) MButton("Skoru onaylıyorum", onConfirm, icon = Icons.Rounded.Verified)
            if (result.status == "pending" && result.submittedBy == DemoData.ME && canSubmit) {
                MPillActionButton("Skoru düzenle", { editingPending = true }, icon = Icons.Rounded.Edit, tone = MPillTone.NEUTRAL, compact = true)
            }
            if (isCheckedIn && event.status in setOf(EventStatus.COMPLETED, EventStatus.VERIFIED)) onOpenFeedback?.let { action -> MButton("Saha deneyimini değerlendir", action, isPrimary = false, icon = Icons.Rounded.StarOutline) }
            Text(result.note.ifBlank { "Sonuç notu eklenmedi." }, color = MColors.Ink500)
            if (!backendMode) Text("Fair play değerlendirmen: ${result.fairPlay}/5", style = MaterialTheme.typography.bodySmall)
            if (!result.disputed && result.status == "pending" && (result.disputeUntil == null || kotlinx.datetime.Clock.System.now().toEpochMilliseconds() < result.disputeUntil)) {
                MPremiumTextField(reason, { reason = it.take(500) }, label = "İtiraz gerekçen", singleLine = false, minLines = 2, maxLines = 4)
                MButton("Sonuca itiraz et", { onDispute(reason.trim()) }, enabled = reason.trim().length >= 10, isPrimary = false)
            }
        } else if (canSubmit) {
            Text("Skoru ve iki tarafın kadrosunu kaydet. Maç sayısı, seri ve görevler katılımı doğrulanmış sonuçla güncellenir. Rekabetçi maçta MMR da hesaplanır.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MPremiumTextField(home, { home = it.filter(Char::isDigit).take(3) }, label = "Ev sahibi", modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                MPremiumTextField(away, { away = it.filter(Char::isDigit).take(3) }, label = "Rakip", modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            if (backendMode) {
                Text("Maç tarafları", style = MaterialTheme.typography.titleSmall)
                event.participantIds.distinct().forEach { id -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(players.firstOrNull { it.id == id }?.name ?: if (id == DemoData.ME) "Sen" else "Sporcu", Modifier.weight(1f))
                    FilterChip(id in homeIds, { homeIds = homeIds + id; awayIds = awayIds - id }, enabled = !event.isTeamEvent, label = { Text("Ev") })
                    Spacer(Modifier.width(6.dp))
                    FilterChip(id in awayIds, { awayIds = awayIds + id; homeIds = homeIds - id }, enabled = !event.isTeamEvent, label = { Text("Rakip") })
                } }
            } else {
                Text("Fair play", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { (1..5).forEach { score -> FilterChip(fairPlay == score, { fairPlay = score }, label = { Text("$score ★") }, colors = premiumChoiceChipColors()) } }
            }
            MPremiumTextField(note, { note = it.take(500) }, label = "Maç notu · isteğe bağlı", singleLine = false, minLines = 2, maxLines = 4)
            MButton(if (editingPending) "Skoru güncelle" else "Sonucu bildir", { editingPending = false; onSubmit(MatchResultDraft(event.id, home.toInt(), away.toInt(), note.trim(), fairPlay, homeParticipantIds = homeIds.toList(), awayParticipantIds = awayIds.toList())) }, enabled = home.toIntOrNull() != null && away.toIntOrNull() != null && (!backendMode || sidesReady))
            if (editingPending) MPillActionButton("Vazgeç", { editingPending = false }, icon = Icons.Rounded.Close, tone = MPillTone.NEUTRAL, compact = true)
        } else Text(if (backendMode && !isCheckedIn) "Bu maçta saha check-in kaydın bulunmuyor. Sonuç bildirmek ve pasaportuna maç eklenmesi için maç oynanırken katılımını doğrulamalısın." else "Sonuç kaydı tamamlanan veya devam eden maçlarda açılır.", color = MColors.Ink500)
    }
}

@Composable
fun ContactVerificationSheet(initialPhone: String, onRecordRequest: (String) -> Unit, onDismiss: () -> Unit, backend: BackendRepository? = null) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var challengeId by remember { mutableStateOf("") }
    var previewCode by remember { mutableStateOf<String?>(if (backend == null) "2026" else null) }
    val initialSplit = remember(initialPhone) { splitE164Phone(initialPhone) }
    var selectedCountry by remember(initialPhone) { mutableStateOf(initialSplit.first) }
    var phone by remember(initialPhone) { mutableStateOf(initialSplit.second) }
    var step by remember { mutableIntStateOf(0) }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val normalizedPhone = normalizePhoneWithCountry(selectedCountry, phone)
    ActivitySheet("İletişim doğrulaması", "Sahada güvenle buluş", onDismiss, Icons.Rounded.PhoneIphone) {
        when (step) {
            0 -> {
                Text("Cep telefonu numaranı doğrulama akışına ekle. Numaran sporcu kartında herkese açık gösterilmez.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
                PhoneNumberFieldWithCountryPicker(
                    selectedCountry = selectedCountry,
                    onSelectCountry = { selectedCountry = it; error = null },
                    localPhone = phone,
                    onLocalPhoneChange = { phone = it; error = null },
                    label = "Cep telefonu",
                    enabled = !busy
                )
                MButton("Doğrulama koduna geç", {
                    val targetPhone = normalizedPhone ?: return@MButton
                    if (backend == null) step = 1 else scope.launch {
                        busy = true; error = null
                        try {
                            if (com.matchora.app.data.isNativePhoneAuthSupported) {
                                val verification = com.matchora.app.data.startNativePhoneVerification(targetPhone)
                                if (verification.verificationId.startsWith("instant:")) {
                                    val nativeAuth = com.matchora.app.data.verifyNativePhoneCode(verification.verificationId, "")
                                    backend.verifyContactWithIdToken(nativeAuth.idToken)
                                    step = 2
                                } else {
                                    challengeId = verification.verificationId
                                    previewCode = null
                                    step = 1
                                }
                            } else {
                                val response = backend.startContactOtp(targetPhone)
                                challengeId = response.getValue("challengeId").jsonPrimitive.content
                                previewCode = response["developmentCode"]?.jsonPrimitive?.contentOrNull
                                step = 1
                            }
                        } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                        catch (e: Exception) {
                            error = if (e is BackendFailure && e.code == "FIREBASE_PHONE_ERROR") com.matchora.app.data.formatFirebasePhoneAuthError(e.message)
                            else (e as? BackendFailure)?.message ?: "Doğrulama kodu gönderilemedi."
                        }
                        finally { busy = false }
                    }
                }, enabled = normalizedPhone != null && !busy)
                error?.let { Text(it, color = MColors.Rose) }
            }
            1 -> {
                Text(if (previewCode != null) "Geliştirme kodu: $previewCode" else "Telefonuna gönderilen 6 haneli kodu gir.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
                MPremiumTextField(code, { code = it.filter(Char::isDigit).take(if (backend == null) 4 else 6); error = null }, label = "Doğrulama kodu", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), isError = error != null)
                error?.let { Text(it, color = MColors.Rose) }
                MButton("Doğrulama talebini kaydet", {
                    if (backend == null) {
                        if (code != "2026") error = "Kod eşleşmedi. Tekrar dene."
                        else { onRecordRequest(normalizedPhone.orEmpty()); step = 2 }
                    } else scope.launch {
                        busy = true; error = null
                        try {
                            if (com.matchora.app.data.isNativePhoneAuthSupported) {
                                val nativeAuth = com.matchora.app.data.verifyNativePhoneCode(challengeId, code)
                                backend.verifyContactWithIdToken(nativeAuth.idToken)
                            } else {
                                backend.verifyContactOtp(challengeId, code)
                            }
                            step = 2
                        } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                        catch (e: Exception) {
                            error = if (e is BackendFailure && e.code in setOf("INVALID_CODE", "FIREBASE_PHONE_ERROR")) com.matchora.app.data.formatFirebasePhoneAuthError(e.message)
                            else (e as? BackendFailure)?.message ?: "Kod doğrulanamadı."
                        }
                        finally { busy = false }
                    }
                }, enabled = code.length == (if (backend == null) 4 else 6) && !busy)
                MPillActionButton("Numarayı değiştir", { step = 0 }, icon = Icons.Rounded.Edit, tone = MPillTone.NEUTRAL, compact = true)
            }
            else -> {
                MFeatureHeader(if (backend == null) "Talebin kaydedildi" else "Telefonun doğrulandı", if (backend == null) "Hesap → Destek bölümünden ulaşabilirsin" else "İletişim bilgin hesabına bağlandı", Icons.Rounded.VerifiedUser)
                MButton("Tamam", onDismiss)
            }
        }
    }
}

@Composable
fun PhotoVerificationSheet(profile: UserProfile, onPhotoRequest: (androidx.compose.ui.graphics.ImageBitmap?, (Boolean) -> Unit) -> Unit, onDismiss: () -> Unit) {
    val backend = LocalBackendRepository.current
    val wire = if (backend != null) backend.snapshots.collectAsState().value else null
    val existingAvatarUploadId = wire?.o("profile")?.s("avatarUploadId")?.ifBlank { null }
    val photoRequests = wire?.a("supportRequests").orEmpty().filter { it.s("category") == "Fotoğraf doğrulaması" }
    val openRequest = photoRequests.firstOrNull { it.s("status") == "open" }
    val rejectedRequest = photoRequests.firstOrNull { it.s("status") == "rejected" }
    var photo by remember { mutableStateOf(profile.avatar) }
    var pickedNewPhoto by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val pick = rememberGalleryPhotoPicker { photo = it; pickedNewPhoto = true; submitted = false }
    val hasPhotoSource = photo != null || existingAvatarUploadId != null
    val blockedByPendingSamePhoto = openRequest != null && !pickedNewPhoto
    ActivitySheet("Fotoğraf doğrulaması", "Sporcu kartını güvenle tamamla", onDismiss, Icons.Rounded.VerifiedUser) {
        Text("Yüzünün net göründüğü, tek başına olduğun bir fotoğraf seç. Seçimin profil fotoğrafın olarak kaydedilir. İnceleme tamamlanmadan doğrulama rozeti verilmez.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
        if (openRequest != null && !pickedNewPhoto) {
            MSubCard {
                Text("Mevcut profil fotoğrafın için doğrulama talebi inceleme bekliyor. Farklı bir fotoğraf yüklemek istersen aşağıdan seçebilirsin.", style = MaterialTheme.typography.bodySmall, color = MColors.BrandDeep, modifier = Modifier.padding(12.dp))
            }
        } else if (rejectedRequest != null && !pickedNewPhoto) {
            MSubCard {
                val reason = rejectedRequest.s("response").ifBlank { null }
                Text("Son doğrulama talebi reddedildi${reason?.let { ": $it" }.orEmpty()}. Yeni bir fotoğraf seçip gönderebilirsin.", style = MaterialTheme.typography.bodySmall, color = MColors.Rose, modifier = Modifier.padding(12.dp))
            }
        }
        val localPhoto = photo
        if (localPhoto != null) {
            androidx.compose.foundation.Image(localPhoto, "İncelemeye eklenecek fotoğraf", Modifier.fillMaxWidth().height(200.dp), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
        } else if (existingAvatarUploadId != null) {
            BackendPhoto(existingAvatarUploadId, "Mevcut profil fotoğrafı", Modifier.fillMaxWidth().height(200.dp))
        }
        MPremiumMenuRow(
            text = if (!hasPhotoSource) "Fotoğraf seç" else "Fotoğrafı değiştir",
            subtitle = "Kamerayla çek veya galeriden net bir portre seç",
            icon = Icons.Rounded.AddAPhoto,
            onClick = { if (!busy) pick() }
        )
        MButton(
            when {
                busy -> "Gönderiliyor…"
                submitted -> "İnceleme talebi kaydedildi"
                blockedByPendingSamePhoto -> "İnceleme bekleniyor"
                else -> "İnceleme talebi oluştur"
            },
            {
                if (hasPhotoSource) {
                    busy = true
                    onPhotoRequest(if (pickedNewPhoto) photo else profile.avatar ?: null) { success ->
                        submitted = success
                        busy = false
                    }
                }
            },
            enabled = hasPhotoSource && !blockedByPendingSamePhoto && !submitted && !busy
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerFeedbackSheet(players: List<Player>, onSubmit: (String, Int, Boolean) -> Unit, onDismiss: () -> Unit) {
    var targetId by remember(players) { mutableStateOf(players.firstOrNull()?.id) }
    var rating by remember(targetId) { mutableIntStateOf(5) }
    var again by remember(targetId) { mutableStateOf(true) }
    ActivitySheet("Saha deneyimin", "Ortak maçtaki sporcuları değerlendir", onDismiss, Icons.Rounded.StarOutline) {
        if (players.isEmpty()) Text("Değerlendirilebilecek başka katılımcı yok.", color = MColors.Ink500)
        else {
            MFloatingDropdownBox("Sporcu", targetId, players.map { it.id }, { userId -> players.firstOrNull { it.id == userId }?.name.orEmpty() }, { targetId = it })
            Text("Fair play", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { (1..5).forEach { score -> FilterChip(rating == score, { rating = score }, label = { Text("$score ★") }, colors = premiumChoiceChipColors()) } }
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(again, { again = it }); Text("Tekrar birlikte oynamak isterim") }
            MButton("Değerlendirmeyi kaydet", { targetId?.let { onSubmit(it, rating, again) } }, enabled = targetId != null)
        }
    }
}
