package com.matchora.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.matchora.app.data.*
import com.matchora.app.domain.Sport
import com.matchora.app.state.*
import com.matchora.app.ui.theme.MColors
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

private fun Throwable.safeOperatorMessage(fallback: String): String = (this as? BackendFailure)?.message ?: fallback

private data class OperatorAction(val title: String, val operation: String, val payload: JsonObject,
    val field: String? = "reason", val label: String = "Gerekçe", val minimum: Int = 10,
    val message: String = "İşlem yetkin ve kaydın güncel durumu sunucuda denetlenerek kaydedilir.",
    val destructive: Boolean = false)

@Composable
private fun OperatorActionDialog(action: OperatorAction, onDismiss: () -> Unit) {
    var value by remember(action) { mutableStateOf("") }
    val command = rememberBackendAction()
    AlertDialog(onDismissRequest = onDismiss, title = { Text(action.title) }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(action.message, style = MaterialTheme.typography.bodySmall)
        if (action.field != null) OutlinedTextField(value, { value = it.take(if (action.field == "reference") 120 else 1000) }, label = { Text(action.label) }, colors = premiumTextFieldColors())
    } }, confirmButton = {
        val enabled = action.field == null || value.trim().length >= action.minimum
        if (action.destructive) {
            Button(
                onClick = {
                    command(action.operation, if (action.field == null) action.payload else JsonObject(action.payload + (action.field to JsonPrimitive(value.trim())))); onDismiss()
                },
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(containerColor = MColors.Rose)
            ) { Text("Onayla ve kaydet", color = Color.White) }
        } else {
            TextButton(onClick = {
                command(action.operation, if (action.field == null) action.payload else JsonObject(action.payload + (action.field to JsonPrimitive(value.trim())))); onDismiss()
            }, enabled = enabled) { Text("Onayla ve kaydet") }
        }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } })
}

@Composable
fun ModerationSheet(onDismiss: () -> Unit) {
    val backend = LocalBackendRepository.current ?: return
    val wire by backend.snapshots.collectAsState()
    var data by remember { mutableStateOf<JsonObject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var section by remember { mutableStateOf("support") }
    var action by remember { mutableStateOf<OperatorAction?>(null) }
    var search by remember { mutableStateOf("") }
    var announcementTitle by remember { mutableStateOf("") }
    var announcementBody by remember { mutableStateOf("") }
    var announcementCity by remember { mutableStateOf("") }
    var legalDocKey by remember { mutableStateOf("terms") }
    var legalVersion by remember { mutableStateOf("2026.2") }
    var legalUrl by remember { mutableStateOf("") }
    val sections = listOf("venueApplications" to "Tesis başvuruları", "support" to "Destek", "reports" to "Şikayetler", "memories" to "Anılar", "photos" to "Fotoğraf doğrulama",
        "results" to "Maç sonuçları", "reviews" to "Tesis yorumları", "outboxFailures" to "Bildirim hataları", "users" to "Kullanıcı yönetimi", "announcements" to "Duyurular", "flags" to "Özellikler", "legal" to "Sözleşmeler")
    LaunchedEffect(wire, refresh) {
        loading = true; error = null
        try { data = backend.adminInbox() }
        catch (cancel: CancellationException) { throw cancel }
        catch (failure: Exception) { error = failure.safeOperatorMessage("Operasyon kayıtları yüklenemedi.") }
        finally { loading = false }
    }
    ActivitySheet("Operasyon merkezi", "Yalnız yetkili yönetici hesabı", onDismiss, Icons.Rounded.AdminPanelSettings) {
        MFloatingDropdownBox("İşlem alanı", section, sections.map { it.first }, { id -> sections.first { it.first == id }.second }, { section = it })
        TextButton(onClick = { refresh++ }, enabled = !loading) { Text(if (loading) "Yükleniyor…" else "Yenile") }
        error?.let { Text(it, color = MColors.Rose) }
        if (section == "announcements") {
            MPremiumTextField(announcementTitle, { announcementTitle = it.take(120) }, label = "Başlık")
            MPremiumTextField(announcementBody, { announcementBody = it.take(1000) }, label = "Duyuru metni", singleLine = false, minLines = 3, maxLines = 5)
            MPremiumTextField(announcementCity, { announcementCity = it.take(60) }, label = "İl · boşsa tüm iller")
            Text("Yalnız kampanya bildirimine izin veren sporculara gönderilir. Yönetici başına günde bir duyuru yapılabilir.", style = MaterialTheme.typography.bodySmall)
            MButton("Duyuruyu gözden geçir", { action = OperatorAction("Duyuruyu gönder", "admin.announcement.send", apiPayload("title" to announcementTitle, "body" to announcementBody, "city" to announcementCity), field = null, message = "$announcementTitle\n$announcementBody\nBölge: ${announcementCity.ifBlank { "Tüm iller" }} · İzin veren kullanıcılar") }, enabled = announcementTitle.trim().length >= 3 && announcementBody.trim().length >= 10)
            data?.a("announcements").orEmpty().forEach { Text("Duyuru · ${it.i("recipients")} kullanıcı", style = MaterialTheme.typography.bodySmall) }
        } else if (section == "legal") {
            MFloatingDropdownBox("Sözleşme türü", legalDocKey, listOf("terms", "privacy", "guidelines", "deletion"), {
                when (it) {
                    "privacy" -> "Gizlilik ve KVKK"
                    "guidelines" -> "Topluluk ve Fair Play Kuralları"
                    "deletion" -> "Hesap ve Veri Silme Politikası"
                    else -> "Kullanım Koşulları"
                }
            }, {
                legalDocKey = it
            })
            MPremiumTextField(legalVersion, { legalVersion = it.take(80) }, label = "Sürüm (örn. 2026.2)")
            MPremiumTextField(legalUrl, { legalUrl = it.take(2000) }, label = "HTTPS belge bağlantısı (https://...)", placeholder = "https://matchora.app/legal/terms-v2")
            Text("Yayınlanan sürüm bağlantısı değiştirilemez. Kullanım koşulları ve gizlilik güncellemeleri yeni sürüm onayı gerektirir.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
            val publishPayload = buildLegalPublishPayload(legalDocKey, legalVersion, legalUrl)
            MButton("Sözleşme sürümünü yayınla", {
                if (publishPayload != null) {
                    val cleanVersion = legalVersion.trim()
                    val cleanUrl = legalUrl.trim()
                    action = OperatorAction(
                        title = "Sözleşmeyi yayınla",
                        operation = "admin.legal.publish",
                        payload = publishPayload,
                        field = null,
                        message = "Tür: $legalDocKey · Sürüm: $cleanVersion\nBağlantı: $cleanUrl"
                    )
                }
            }, enabled = publishPayload != null)
        } else if (section == "flags") {
            listOf("billing", "memories", "tournaments", "venues", "chat").forEach { flag ->
                val enabled = wire?.a("featureFlags")?.firstOrNull { it.s("name") == flag }?.i("enabled", 1) != 0
                Row(verticalAlignment = Alignment.CenterVertically) { Text(mapOf("billing" to "Mağaza", "memories" to "Anılar", "tournaments" to "Turnuvalar", "venues" to "Tesisler", "chat" to "Sohbet")[flag] ?: flag, Modifier.weight(1f)); Switch(enabled, { next -> action = OperatorAction("Özelliği ${if (next) "aç" else "kapat"}", "admin.feature.set", apiPayload("name" to flag, "enabled" to next), field = null) }) }
            }
        } else {
            if (section == "users") MPremiumTextField(search, { search = it }, label = "Sporcu ara", leadingIcon = Icons.Rounded.Search)
            val rows = data?.a(section).orEmpty().filter { section != "users" || it.s("name").contains(search, true) }
            if (rows.isEmpty() && !loading && error == null) Text("Bu alanda bekleyen kayıt yok.", color = MColors.Ink500)
            rows.forEach { item -> MSubCard {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    when (section) {
                        "venueApplications" -> {
                            Text(item.s("name"), style = MaterialTheme.typography.titleSmall)
                            Text("${item.s("applicantName")} · ${item.s("city")} · ${item.s("area")}\n${item.s("address")}")
                            if (item.s("phone").isNotBlank()) Text("Telefon: ${item.s("phone")}", style = MaterialTheme.typography.bodySmall)
                            if (item.s("description").isNotBlank()) Text(item.s("description"), style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
                            Text("${item.i("courts")} saha · ${item.ss("sports").joinToString()}\n${item.ss("amenities").joinToString()}", style = MaterialTheme.typography.bodySmall)
                            ModerationVenueApplicationPhotos(item.ss("photoUploadIds"))
                            Text("Onay, başvurana işletmeci yetkisi verir ve tesisi yayınlar. İşletme yetkisini doğruladıktan sonra onayla.", style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(true to "Onayla ve yayınla", false to "Reddet").forEach { (approved, title) -> TextButton(onClick = {
                                action = OperatorAction(title, "admin.venue.application.review", apiPayload("applicationId" to item.s("id"), "expectedVersion" to item.i("version", 1), "approve" to approved), field = "response", label = "Başvurana yanıt", minimum = 3, destructive = !approved)
                            }) { Text(title, color = if (!approved) MColors.Rose else MColors.BrandDeep) } } }
                        }
                        "support" -> {
                            Text(item.s("subject"), style = MaterialTheme.typography.titleSmall); Text(item.s("message"))
                            TextButton(onClick = { action = OperatorAction("Destek talebini yanıtla", "admin.support.resolve", apiPayload("requestId" to item.s("id"), "status" to "resolved"), field = "response", label = "Kullanıcıya yanıt", minimum = 3) }) { Text("Yanıtla") }
                        }
                        "reports" -> {
                            Text(item.s("reason"), style = MaterialTheme.typography.titleSmall); Text(item.s("details")); item.o("context").takeIf { it.isNotEmpty() }?.let { Text("${it.s("title")}\n${it.s("description")}", style = MaterialTheme.typography.bodySmall) }; Text("${item.s("target_type")} · ${item.s("target_id")}", style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("resolved" to "İşlendi", "dismissed" to "Kapat").forEach { (status, title) -> TextButton(onClick = { action = OperatorAction(title, "admin.report.resolve", apiPayload("reportId" to item.s("id"), "status" to status), destructive = status == "dismissed") }) { Text(title, color = if (status == "dismissed") MColors.Rose else MColors.BrandDeep) } } }
                            if (item.s("target_type") == "memory" || item.s("target_type") == "venue_review") TextButton(onClick = {
                                val memory = item.s("target_type") == "memory"
                                action = OperatorAction("Bildirilen içeriği kaldır", if (memory) "memory.delete" else "venue.review.delete", apiPayload((if (memory) "memoryId" else "reviewId") to item.s("target_id")), field = null, destructive = true)
                            }) { Text("İçeriği kaldır", color = MColors.Rose) }
                        }
                        "memories" -> {
                            BackendPhoto(item.s("upload_id"), "İnceleme bekleyen anı", Modifier.fillMaxWidth().height(180.dp))
                            val memory = runCatching { Json.parseToJsonElement(item.s("payload")).jsonObject }.getOrNull()
                            Text(memory?.s("caption").orEmpty(), style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(true to "Onayla", false to "Reddet").forEach { (approved, title) -> TextButton(onClick = { action = OperatorAction("Anıyı $title", "admin.memory.review", apiPayload("memoryId" to item.s("id"), "approved" to approved), minimum = 3, destructive = !approved) }) { Text(title, color = if (!approved) MColors.Rose else MColors.BrandDeep) } } }
                        }
                        "photos" -> {
                            if (item.b("stale")) {
                                Text("Bu talebin fotoğrafı güncel değil.")
                                TextButton(onClick = { action = OperatorAction("Eski talebi kapat", "admin.support.resolve", apiPayload("requestId" to item.s("id"), "status" to "rejected"), field = "response", label = "Açıklama", minimum = 3, destructive = true) }) { Text("Talebi kapat", color = MColors.Rose) }
                            } else {
                                BackendPhoto(item.s("uploadId"), "İncelenen profil fotoğrafı", Modifier.fillMaxWidth().height(180.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(true to "Onayla", false to "Reddet").forEach { (approved, title) -> TextButton(onClick = { action = OperatorAction("Fotoğrafı $title", "admin.verification.photo", apiPayload("userId" to item.s("user_id"), "uploadId" to item.s("uploadId"), "requestId" to item.s("id"), "approved" to approved), minimum = 3, destructive = !approved) }) { Text(title, color = if (!approved) MColors.Rose else MColors.BrandDeep) } } }
                            }
                        }
                        "results" -> {
                            val result = runCatching { Json.parseToJsonElement(item.s("payload")).jsonObject }.getOrNull()
                            val event = runCatching { Json.parseToJsonElement(item.s("event_payload")).jsonObject }.getOrNull()
                            Text(event?.s("title") ?: "Maç sonucu", style = MaterialTheme.typography.titleSmall)
                            Text("${result?.i("homeScore")} – ${result?.i("awayScore")} · ${item.s("status")}")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(true to "Doğrula", false to "Reddet").forEach { (approved, title) -> TextButton(onClick = { action = OperatorAction("Sonucu $title", "admin.result.resolve", apiPayload("eventId" to item.s("event_id"), "approve" to approved), destructive = !approved) }) { Text(title, color = if (!approved) MColors.Rose else MColors.BrandDeep) } } }
                        }
                        "reviews" -> {
                            Text("${item.i("rating")}/5 · ${item.s("comment")}")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(true to "Yayınla", false to "Reddet").forEach { (approved, title) -> TextButton(onClick = { action = OperatorAction(title, "admin.venue.review", apiPayload("reviewId" to item.s("id"), "approved" to approved), destructive = !approved) }) { Text(title, color = if (!approved) MColors.Rose else MColors.BrandDeep) } } }
                        }
                        "outboxFailures" -> {
                            Text("Bildirim · ${item.i("attempts")} deneme")
                            TextButton(onClick = { action = OperatorAction("Bildirimi tekrar sıraya al", "admin.outbox.retry", apiPayload("outboxId" to item.s("id")), field = null) }) { Text("Tekrar dene") }
                        }
                        "users" -> {
                            Text("${item.s("name")} · ${item.s("role")}")
                            if (item.s("role") != "admin" && item.s("status") == "active") TextButton(onClick = { action = OperatorAction("İşletmeci yetkisini değiştir", "admin.operator.assign", apiPayload("userId" to item.s("id"), "role" to if (item.s("role") == "venue_owner") "user" else "venue_owner"), destructive = item.s("role") == "venue_owner") }) { Text(if (item.s("role") == "venue_owner") "Yetkiyi kaldır" else "İşletmeci yap", color = if (item.s("role") == "venue_owner") MColors.Rose else MColors.BrandDeep) }
                            if (item.s("role") != "admin") TextButton(onClick = { action = OperatorAction("Hesap erişimini değiştir", "admin.user.status", apiPayload("userId" to item.s("id"), "status" to if (item.s("status") == "suspended") "active" else "suspended"), destructive = item.s("status") != "suspended") }) { Text(if (item.s("status") == "suspended") "Hesabı yeniden aç" else "Hesabı askıya al", color = if (item.s("status") != "suspended") MColors.Rose else MColors.BrandDeep) }
                        }
                    }
                }
            } }
        }
    }
    action?.let { OperatorActionDialog(it) { action = null } }
}

@Composable
fun VenueOperatorSheet(onDismiss: () -> Unit) {
    val backend = LocalBackendRepository.current ?: return
    val wire by backend.snapshots.collectAsState()
    var data by remember { mutableStateOf<JsonObject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var create by remember { mutableStateOf(false) }
    var editVenue by remember { mutableStateOf<JsonObject?>(null) }
    var createSlot by remember { mutableStateOf<JsonObject?>(null) }
    var planSlots by remember { mutableStateOf<JsonObject?>(null) }
    var editSlot by remember { mutableStateOf<Pair<JsonObject, JsonObject>?>(null) }
    var action by remember { mutableStateOf<OperatorAction?>(null) }
    LaunchedEffect(wire, refresh) {
        error = null
        try { data = backend.operatorVenues() }
        catch (cancel: CancellationException) { throw cancel }
        catch (failure: Exception) { error = failure.safeOperatorMessage("Tesis bilgileri yüklenemedi.") }
    }
    ActivitySheet("Tesis yönetimi", "Saat, rezervasyon ve tesiste ödeme kayıtları", onDismiss, Icons.Rounded.Stadium) {
        MButton("Tesis ekle", { create = true }, icon = Icons.Rounded.Add)
        TextButton(onClick = { refresh++ }) { Text("Yenile") }
        error?.let { Text(it, color = MColors.Rose) }
        data?.a("items").orEmpty().forEach { venue -> MCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(venue.s("name"), style = MaterialTheme.typography.titleMedium)
                val photoCount = venue.ss("photoUploadIds").size
                Text("${venue.s("city")} · ${venue.s("area")}${if (photoCount > 0) " · $photoCount fotoğraf" else ""}", style = MaterialTheme.typography.bodySmall)
                if (venue.b("archived")) Text("Yeni rezervasyonlara kapalı", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
                else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MButton("Tesisi düzenle", { editVenue = venue }, Modifier.weight(1f), isPrimary = false, height = 38.dp)
                            MButton("Saat ekle", { createSlot = venue }, Modifier.weight(1f), isPrimary = false, height = 38.dp)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MButton("Haftalık plan", { planSlots = venue }, Modifier.weight(1f), isPrimary = false, height = 38.dp)
                            MButton(
                                "Tesisi kapat",
                                {
                                    action = OperatorAction(
                                        "Tesisi yeni rezervasyonlara kapat",
                                        "venue.archive",
                                        apiPayload("venueId" to venue.s("id"), "expectedVersion" to venue.i("version", 1)),
                                        field = null,
                                        message = "Gelecekteki tahsil edilmemiş rezervasyonlar iptal edilip müşterilere bildirilir. Ödenmiş veya devam eden rezervasyon varsa önce sonuçlandırılmalıdır. Geçmiş kayıtlar korunur.",
                                        destructive = true
                                    )
                                },
                                Modifier.weight(1f),
                                isPrimary = false,
                                isDestructive = true,
                                height = 38.dp
                            )
                        }
                    }
                }
                venue.a("slots").forEach { slot ->
                    val start = kotlinx.datetime.Instant.fromEpochMilliseconds(slot.l("starts_at")).toLocalDateTime(TimeZone.of("Europe/Istanbul")).toString().replace('T', ' ')
                    Text("${slot.s("court")} · $start · ₺${formatTryMinor(slot.i("priceMinor"))} · ${slot.s("status")}", style = MaterialTheme.typography.bodySmall)
                    val futureSlot = slot.l("starts_at") > kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                    val activeBooking = venue.a("bookings").any { it.s("slot_id") == slot.s("id") && it.s("status") in listOf("pending", "confirmed") }
                    if (!venue.b("archived") && slot.s("status") == "open" && futureSlot) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!activeBooking) TextButton(onClick = { editSlot = venue to slot }) { Text("Saati / ücreti düzenle") }
                        TextButton(onClick = { action = OperatorAction("Saati kapat", "venue.slot.cancel", apiPayload("slotId" to slot.s("id"), "expectedVersion" to slot.i("version", 1)), field = null, destructive = true) }) { Text("Saati kapat", color = MColors.Rose) }
                    }
                    if (!venue.b("archived") && slot.s("status") == "cancelled" && futureSlot && !activeBooking) TextButton(onClick = {
                        action = OperatorAction("Saati yeniden aç", "venue.slot.reopen", apiPayload("slotId" to slot.s("id"), "expectedVersion" to slot.i("version", 1)), field = null,
                            message = "Saha çakışması ve güncel rezervasyon durumu kontrol edilerek bu saat yeniden rezervasyona açılır.")
                    }) { Text("Saati yeniden aç") }
                }
                venue.a("bookings").forEach { booking -> MSubCard {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${booking.s("customerName")} · ₺${formatTryMinor(booking.i("amount_minor"))}")
                        Text("${booking.s("status")} · ${booking.s("payment_status")}", style = MaterialTheme.typography.bodySmall)
                        val status = booking.s("status")
                        val paid = booking.s("payment_status") == "paid"
                        if (status == "pending") TextButton(onClick = { action = OperatorAction("Rezervasyonu onayla", "booking.confirm", apiPayload("bookingId" to booking.s("id")), field = null) }) { Text("Onayla") }
                        if (status == "confirmed" && !paid) TextButton(onClick = { action = OperatorAction("Tesiste ödemeyi kaydet", "booking.payment.record", apiPayload("bookingId" to booking.s("id")), field = "reference", label = "Makbuz / ödeme referansı", minimum = 3, message = "Bu işlem karttan para çekmez. Tesiste alınmış ödemenin kaydını oluşturur.") }) { Text("Ödeme kaydı") }
                        if (status == "confirmed" && paid && booking.l("ends_at") <= kotlinx.datetime.Clock.System.now().toEpochMilliseconds()) TextButton(onClick = { action = OperatorAction("Rezervasyonu tamamla", "booking.complete", apiPayload("bookingId" to booking.s("id")), field = null) }) { Text("Tamamla") }
                        if (status in listOf("pending", "confirmed") && !paid) TextButton(onClick = { action = OperatorAction("Rezervasyonu iptal et", "booking.cancel", apiPayload("bookingId" to booking.s("id")), field = null, destructive = true) }) { Text("İptal et", color = MColors.Rose) }
                        if (status in listOf("confirmed", "completed") && paid) TextButton(onClick = { action = OperatorAction("Tesiste yapılan iadeyi kaydet", "booking.refund.record", apiPayload("bookingId" to booking.s("id")), field = "reference", label = "İade referansı", minimum = 3, message = "Bu işlem para göndermez. Kullanıcıya yapılmış tam iadenin kaydını oluşturur ve rezervasyonu kapatır.", destructive = true) }) { Text("İadeyi kaydet", color = MColors.Rose) }
                    }
                } }
            }
        } }
    }
    if (create) CreateVenueSheet(onDismiss = { create = false })
    planSlots?.let { VenueScheduleSheet(it) { planSlots = null } }
    editVenue?.let { venue -> CreateVenueSheet(venue, onDismiss = { editVenue = null }) }
    createSlot?.let { venue -> CreateVenueSlotSheet(venue) { createSlot = null } }
    editSlot?.let { (venue, slot) -> CreateVenueSlotSheet(venue, slot) { editSlot = null } }
    action?.let { OperatorActionDialog(it) { action = null } }
}

private data class VenuePhotoDraft(val uploadId: String? = null, val bitmap: ImageBitmap? = null)

@Composable
private fun ModerationVenueApplicationPhotos(uploadIds: List<String>) {
    val clean = remember(uploadIds) { uploadIds.distinct().take(4) }
    if (clean.isEmpty()) return
    var previewId by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Tesis fotoğrafları (${clean.size})", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(clean) { index, uploadId ->
                Box(
                    modifier = Modifier
                        .size(width = 104.dp, height = 78.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { previewId = uploadId }
                ) {
                    BackendPhoto(uploadId, "Başvuru tesis fotoğrafı ${index + 1}", Modifier.fillMaxSize())
                }
            }
        }
    }
    previewId?.let { id ->
        Dialog(onDismissRequest = { previewId = null }) {
            Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFF0B1220)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = { previewId = null }) { Icon(Icons.Rounded.Close, "Kapat", tint = Color.White) }
                    }
                    Box(Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(12.dp))) {
                        BackendPhoto(id, "Tesis fotoğrafı önizleme", Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

@Composable
fun CreateVenueSheet(venue: JsonObject? = null, application: Boolean = false, onDismiss: () -> Unit) {
    val backend = LocalBackendRepository.current
    val wire = backend?.snapshots?.collectAsState()?.value
    val scope = rememberCoroutineScope()
    var name by remember(venue?.s("id"), application) { mutableStateOf(venue?.s("name").orEmpty()) }
    var city by remember(venue?.s("id"), application) { mutableStateOf(venue?.s("city") ?: wire?.o("profile")?.s("city", "İstanbul") ?: "İstanbul") }
    var area by remember(venue?.s("id"), application) { mutableStateOf(venue?.s("area") ?: wire?.o("profile")?.s("area", "Kadıköy") ?: "Kadıköy") }
    var address by remember(venue?.s("id"), application) { mutableStateOf(venue?.s("address").orEmpty()) }
    var phone by remember(venue?.s("id"), application) { mutableStateOf(venue?.s("phone").orEmpty()) }
    var description by remember(venue?.s("id"), application) { mutableStateOf(venue?.s("description").orEmpty()) }
    var point by remember(venue?.s("id"), application) { mutableStateOf(venue?.o("approximateLocation")?.takeIf { it.isNotEmpty() }?.let { loc -> loc.s("latitude").toDoubleOrNull()?.let { lat -> loc.s("longitude").toDoubleOrNull()?.let { lon -> EventGeoPoint(lat, lon) } } }) }
    var pickingPoint by remember { mutableStateOf(false) }
    var pickingRegion by remember { mutableStateOf(false) }
    var sport by remember { mutableStateOf(Sport.FOOTBALL) }
    var sports by remember(venue?.s("id"), application) { mutableStateOf(venue?.ss("sports")?.mapNotNull { name -> Sport.entries.firstOrNull { it.name == name } }?.takeIf { it.isNotEmpty() } ?: listOf(Sport.FOOTBALL)) }
    var amenities by remember(venue?.s("id"), application) { mutableStateOf(venue?.ss("amenities")?.joinToString(", ").orEmpty()) }
    var courts by remember(venue?.s("id"), application) { mutableIntStateOf(venue?.i("courts", 1) ?: 1) }
    var cancellation by remember(venue?.s("id"), application) { mutableIntStateOf(venue?.i("cancellationHours", 24) ?: 24) }
    var photos by remember(venue?.s("id"), application) { mutableStateOf<List<VenuePhotoDraft>>(venue?.ss("photoUploadIds")?.take(4)?.map { VenuePhotoDraft(uploadId = it) }.orEmpty()) }
    var saving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pendingKey by remember { mutableStateOf<String?>(null) }
    val pickVenuePhoto = rememberGalleryPhotoPicker { bitmap ->
        if (photos.size < 4) {
            photos = photos + VenuePhotoDraft(bitmap = bitmap)
            pendingKey = null
            errorMessage = null
        }
    }
    ActivitySheet(if (application) "Tesisini ekle" else if (venue == null) "Tesis ekle" else "Tesisi düzenle", if (application) "İşletmeci başvurusu · yönetici onayı gerekir" else "Tesisinin bilgilerini kaydet", onDismiss, Icons.Rounded.Stadium) {
        MFloatingInputBox(
            label = "Tesis Adı",
            value = name,
            onValueChange = { name = it.take(100); pendingKey = null },
            placeholder = "Örn: Moda Sahil Arena & Tenis Kulübü",
            icon = Icons.Rounded.Stadium,
            badgeText = "Zorunlu"
        )
        MFloatingActionRow("İl ve ilçe", "$city · $area", Icons.Rounded.LocationCity, onClick = { pickingRegion = true })
        MFloatingInputBox(
            label = "Açık Tesis Adresi",
            value = address,
            onValueChange = { address = it.take(300); pendingKey = null },
            placeholder = "Mahalle, cadde, sokak ve kapı numarası...",
            icon = Icons.Rounded.Place,
            badgeText = if (application) "Zorunlu" else "İsteğe bağlı"
        )
        MFloatingActionRow(
            "Harita konumu",
            if (point == null) "Haritada işaretle" else "İşaretlendi · Değiştir",
            Icons.Rounded.PinDrop,
            onClick = { pickingPoint = true }
        )
        MFloatingInputBox(
            label = "İletişim Telefonu",
            value = phone,
            onValueChange = { phone = it.take(30); pendingKey = null },
            placeholder = "05XX XXX XX XX veya sabit hat",
            icon = Icons.Rounded.Phone,
            badgeText = "İsteğe bağlı"
        )
        MFloatingInputBox(
            label = "Tesis Açıklaması",
            value = description,
            onValueChange = { description = it.take(600); pendingKey = null },
            placeholder = "Zemin yapısı, aydınlatma, ulaşım ve saha özellikleri...",
            icon = Icons.Rounded.EditNote,
            badgeText = "İsteğe bağlı",
            singleLine = false,
            minLines = 2,
            maxLines = 4
        )
        SportPickerField(sport, { selected -> if (selected != null) { sport = selected; if (selected !in sports && sports.size < 20) { sports = sports + selected; pendingKey = null } } }, allowAll = false)
        Text("Branş seçerek ekle; çıkarmak için adına dokun. En az bir branş gerekir.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            sports.forEach { selected ->
                MPillActionButton(
                    text = "${selected.emoji} ${selected.label}${if (sports.size > 1) " ✕" else ""}",
                    tone = MPillTone.NEUTRAL,
                    compact = true,
                    enabled = sports.size > 1 && !saving,
                    onClick = { sports = sports - selected; pendingKey = null }
                )
            }
        }
        MFloatingInputBox(
            label = "Olanaklar (Virgülle Ayır)",
            value = amenities,
            onValueChange = { amenities = it.take(1800); pendingKey = null },
            placeholder = "Örn: Duş, Otopark, Kafe, Ekipman Kiralama",
            icon = Icons.Rounded.Verified
        )
        MFloatingDropdownBox("Saha sayısı", courts, (1..50).toList(), { "$it" }, { courts = it; pendingKey = null }, icon = Icons.Rounded.GridView)
        MFloatingDropdownBox("Ücretsiz iptal sınırı", cancellation, (listOf(0, 6, 12, 24, 48, 72, 168) + cancellation).distinct().sorted(), { "$it saat önce" }, { cancellation = it; pendingKey = null }, icon = Icons.Rounded.EventBusy)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Tesis fotoğrafları (${photos.size}/4)", style = MaterialTheme.typography.titleSmall)
                Text("Kartta değil detayda görünür", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(photos) { index, item ->
                    Box(modifier = Modifier.size(92.dp).clip(RoundedCornerShape(14.dp))) {
                        when {
                            item.bitmap != null -> Image(item.bitmap, "Seçilen tesis fotoğrafı ${index + 1}", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            item.uploadId != null -> BackendPhoto(item.uploadId, "Tesis fotoğrafı ${index + 1}", Modifier.fillMaxSize())
                        }
                        if (!saving) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xCC0B1220),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(5.dp)
                                    .size(22.dp)
                                    .clickable {
                                        photos = photos.filterIndexed { i, _ -> i != index }
                                        pendingKey = null
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Close, "Fotoğrafı kaldır", tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
                if (photos.size < 4) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MColors.SurfaceMuted,
                            border = BorderStroke(1.dp, MColors.LineSoft),
                            modifier = Modifier
                                .size(92.dp)
                                .clickable(enabled = !saving && backend?.authenticated == true) { pickVenuePhoto() }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Rounded.AddPhotoAlternate, "Tesis fotoğrafı ekle", tint = MColors.Brand, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.height(4.dp))
                                Text("Resim ekle", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MColors.Ink800)
                            }
                        }
                    }
                }
            }
        }
        Text("İptal politikası mevcut rezervasyonların son tarihini değiştirmez. Aktif rezervasyon varken bölge, adres, branş ve saha sayısı değiştirilemez.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
        val cleanAmenities = amenities.split(',').map(String::trim).filter(String::isNotBlank).distinct()
        if (application) Text("Başvurun yöneticilere gider. Onaylandığında tesisin yayınlanır; tesis yönetiminden gün, saat, süre ve ücret belirleyerek rezervasyona açabilirsin.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
        if (backend == null) Text("Önizleme formu · başvuru göndermek için canlı hesabın gerekir.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
        errorMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MColors.Rose) }
        MButton(
            text = when {
                saving -> "Kaydediliyor…"
                application -> "Başvuruyu gönder"
                else -> "Tesisi kaydet"
            },
            onClick = {
                val repo = backend ?: return@MButton
                if (saving) return@MButton
                scope.launch {
                    saving = true
                    errorMessage = null
                    try {
                        val updatedPhotos = photos.toMutableList()
                        for (index in updatedPhotos.indices) {
                            val current = updatedPhotos[index]
                            if (current.uploadId == null && current.bitmap != null) {
                                val newId = repo.uploadPhoto(current.bitmap)
                                updatedPhotos[index] = current.copy(uploadId = newId)
                                photos = updatedPhotos.toList()
                            }
                        }
                        val uploadIds = updatedPhotos.mapNotNull { it.uploadId }.distinct().take(4)
                        val requestKey = pendingKey ?: repo.requestKey("venue").also { pendingKey = it }
                        val fields = apiPayload(
                            "name" to name,
                            "city" to city,
                            "area" to area,
                            "address" to address.trim(),
                            "phone" to phone.trim().ifBlank { null },
                            "description" to description.trim().ifBlank { null },
                            "approximateLocation" to point?.let { mapOf("latitude" to it.latitude, "longitude" to it.longitude) },
                            "sports" to sports,
                            "amenities" to cleanAmenities,
                            "courts" to courts,
                            "cancellationHours" to cancellation,
                            "photoUploadIds" to uploadIds
                        )
                        val op = if (application) "venue.application.create" else if (venue == null) "venue.create" else "venue.update"
                        val payload = if (application || venue == null) fields else JsonObject(fields + apiPayload("venueId" to venue.s("id"), "expectedVersion" to venue.i("version", 1)))
                        repo.command(op, payload, requestKey)
                        pendingKey = null
                        onDismiss()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        errorMessage = failure.safeOperatorMessage("Tesis kaydedilemedi.")
                    } finally {
                        saving = false
                    }
                }
            },
            enabled = !saving && backend?.authenticated == true && name.trim().length >= 3 && city.trim().length >= 2 && area.trim().length >= 2 &&
                (address.trim().length >= 10 || !application && address.isBlank()) && sports.isNotEmpty() && cleanAmenities.size <= 30 && cleanAmenities.all { it.length <= 60 }
        )
    }
    if (pickingRegion) {
        CityAndLocationSelectorSheet(city, area, false, onDismiss = { pickingRegion = false }) { pickedCity, pickedArea, _ ->
            city = canonicalTurkishCityName(pickedCity)
            area = pickedArea
            pendingKey = null
            pickingRegion = false
        }
    }
    if (pickingPoint) EventLocationPicker(initialPoint = point, initialAddress = address, city = city, district = area, onDismiss = { pickingPoint = false }, onConfirm = { selected, pickedAddress -> point = selected; if (address.isBlank()) address = pickedAddress; pendingKey = null; pickingPoint = false })
}

@Composable
private fun VenueScheduleSheet(venue: JsonObject, onDismiss: () -> Unit) {
    var date by remember { mutableStateOf(kotlinx.datetime.LocalDate.fromEpochDays(todayDate().toEpochDays()+1)) }
    var hour by remember { mutableIntStateOf(18) }; var minute by remember { mutableIntStateOf(0) }
    var days by remember { mutableIntStateOf(14) }; var duration by remember { mutableIntStateOf(60) }
    var perDay by remember { mutableIntStateOf(3) }; var court by remember { mutableStateOf("Saha 1") }
    var price by remember { mutableStateOf("") }; var weekdays by remember { mutableStateOf(setOf(1,2,3,4,5,6,7)) }
    val command = rememberBackendAction()
    ActivitySheet("Haftalık müsaitlik planı", venue.s("name"), onDismiss, Icons.Rounded.CalendarMonth) {
        EventDateTimeField(date,hour,minute) { d,h,m -> date=d;hour=h;minute=m }
        MFloatingInputBox("Saha / Kort Adı", court, { court = it.take(60) }, placeholder = "Örn: Saha 1, Merkez Kort", icon = Icons.Rounded.Stadium)
        MFloatingDropdownBox("Planın uzunluğu",days,listOf(7,14,28,56),{"$it gün"},{days=it}, icon = Icons.Rounded.DateRange)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Açık günler", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MColors.Ink900)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz").forEachIndexed { index, label ->
                    val dayNum = index + 1
                    val selected = dayNum in weekdays
                    FilterChip(
                        selected = selected,
                        onClick = { weekdays = if (selected) weekdays - dayNum else weekdays + dayNum },
                        label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = premiumChoiceChipColors()
                    )
                }
            }
        }
        MFloatingDropdownBox("Günlük saat sayısı",perDay,(1..8).toList(),{"$it rezervasyon aralığı"},{perDay=it}, icon = Icons.Rounded.ViewList)
        MFloatingDropdownBox("Her aralığın süresi",duration,listOf(30,60,90,120),{"$it dakika"},{duration=it}, icon = Icons.Rounded.Schedule)
        MFloatingInputBox("Her Aralığın Bedeli (₺)", price, { price = it.take(10) }, placeholder = "Örn: 1200", icon = Icons.Rounded.Payments)
        Text("Seçtiğin günlerde başlangıç saatinden itibaren ardışık saatler açılır. Plan en fazla 120 saat içerebilir. Tek çakışma varsa hiçbir saat eklenmez; mevcut rezervasyonlar korunur.",style=MaterialTheme.typography.bodySmall, color = MColors.Ink600)
        val amount=venuePriceMinor(price)
        val count=(0 until days).count { offset -> kotlinx.datetime.LocalDate.fromEpochDays(date.toEpochDays()+offset).dayOfWeek.ordinal+1 in weekdays }*perDay
        MButton("$count müsait saat oluştur",{command("venue.slot.bulk.create",apiPayload("venueId" to venue.s("id"),"court" to court.trim(),"startsAtEpochMillis" to eventStartMillis(date,hour,minute),"days" to days,"weekdays" to weekdays.sorted(),"slotsPerDay" to perDay,"durationMin" to duration,"priceMinor" to amount));onDismiss()},
            enabled=amount!=null&&court.trim().isNotEmpty()&&count in 1..120&&hour*60+minute+perDay*duration<=1440&&eventStartMillis(date,hour,minute)>kotlinx.datetime.Clock.System.now().toEpochMilliseconds()+60000)
    }
}

@Composable
private fun CreateVenueSlotSheet(venue: JsonObject, slot: JsonObject? = null, onDismiss: () -> Unit) {
    val originalStart = remember { slot?.l("starts_at")?.let { kotlinx.datetime.Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.of("Europe/Istanbul")) } }
    var court by remember { mutableStateOf(slot?.s("court") ?: "Saha 1") }
    var date by remember { mutableStateOf(originalStart?.date ?: todayDate()) }
    var hour by remember { mutableIntStateOf(originalStart?.hour ?: 20) }
    var minute by remember { mutableIntStateOf(originalStart?.minute ?: 0) }
    var duration by remember { mutableIntStateOf(slot?.i("durationMin", 60) ?: 60) }
    var price by remember { mutableStateOf(slot?.i("priceMinor")?.let(::formatTryMinor).orEmpty()) }
    val command = rememberBackendAction()
    ActivitySheet(if (slot == null) "Müsait saat ekle" else "Saati / ücreti düzenle", venue.s("name"), onDismiss, Icons.Rounded.Schedule) {
        MFloatingInputBox("Saha / Kort Adı", court, { court = it.take(60) }, placeholder = "Örn: Saha 1, Merkez Kort", icon = Icons.Rounded.Stadium)
        EventDateTimeField(date, hour, minute) { d, h, m -> date = d; hour = h; minute = m }
        MFloatingDropdownBox("Süre", duration, (listOf(15, 30, 60, 90, 120, 180, 240) + duration).distinct().sorted(), { "$it dakika" }, { duration = it }, icon = Icons.Rounded.Schedule)
        MFloatingInputBox("Toplam Saha Bedeli (₺)", price, { price = it.filter { char -> char.isDigit() || char == ',' || char == '.' }.take(10) }, placeholder = "Örn: 1200 veya 1200,50", icon = Icons.Rounded.Payments)
        val priceMinor = venuePriceMinor(price)
        MButton(if (slot == null) "Saati aç" else "Değişiklikleri kaydet", {
            val fields = apiPayload("court" to court, "startsAtEpochMillis" to eventStartMillis(date, hour, minute), "durationMin" to duration, "priceMinor" to priceMinor)
            command(if (slot == null) "venue.slot.create" else "venue.slot.update", JsonObject(fields + if (slot == null) apiPayload("venueId" to venue.s("id")) else apiPayload("slotId" to slot.s("id"), "expectedVersion" to slot.i("version", 1))))
            onDismiss()
        }, enabled = court.trim().isNotEmpty() && priceMinor != null && eventStartMillis(date, hour, minute) > kotlinx.datetime.Clock.System.now().toEpochMilliseconds() + 60000)
    }
}

private fun venuePriceMinor(value: String): Int? {
    if (!Regex("^\\d{1,7}([.,]\\d{1,2})?$").matches(value.trim())) return null
    val parts = value.trim().replace(',', '.').split('.')
    val amount = (parts[0].toLongOrNull() ?: return null) * 100 + (parts.getOrNull(1)?.padEnd(2, '0')?.toLongOrNull() ?: 0)
    return amount.takeIf { it in 100L..100000000L }?.toInt()
}

internal val LEGAL_DOCUMENT_KINDS = setOf("terms", "privacy", "guidelines", "deletion")

internal fun buildLegalPublishPayload(kind: String, version: String, url: String): JsonObject? {
    val cleanKind = kind.trim()
    val cleanVersion = version.trim()
    val cleanUrl = url.trim()
    val host = cleanUrl.removePrefix("https://").substringBefore('/').substringBefore('?').substringBefore('#')
    val validHost = host.isNotBlank() &&
        '@' !in host &&
        !host.startsWith("localhost", ignoreCase = true) &&
        !host.startsWith("127.0.0.1") &&
        !host.startsWith("[::1]") &&
        !host.startsWith("::1")
    if (cleanKind !in LEGAL_DOCUMENT_KINDS || cleanVersion.isEmpty() || !cleanUrl.startsWith("https://") || cleanUrl.length < 12 || !validHost) {
        return null
    }
    return apiPayload("kind" to cleanKind, "version" to cleanVersion, "url" to cleanUrl)
}
