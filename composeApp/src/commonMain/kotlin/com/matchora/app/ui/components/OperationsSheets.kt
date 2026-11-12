package com.matchora.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.matchora.app.data.*
import com.matchora.app.domain.Sport
import com.matchora.app.state.*
import com.matchora.app.ui.theme.MColors
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*

private data class OperatorAction(val title: String, val operation: String, val payload: JsonObject,
    val field: String? = "reason", val label: String = "Gerekçe", val minimum: Int = 10,
    val message: String = "İşlem yetkin ve kaydın güncel durumu sunucuda denetlenerek kaydedilir.")

@Composable
private fun OperatorActionDialog(action: OperatorAction, onDismiss: () -> Unit) {
    var value by remember(action) { mutableStateOf("") }
    val command = rememberBackendAction()
    AlertDialog(onDismissRequest = onDismiss, title = { Text(action.title) }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(action.message, style = MaterialTheme.typography.bodySmall)
        if (action.field != null) OutlinedTextField(value, { value = it.take(if (action.field == "reference") 120 else 1000) }, label = { Text(action.label) }, colors = premiumTextFieldColors())
    } }, confirmButton = { TextButton(onClick = {
        command(action.operation, if (action.field == null) action.payload else JsonObject(action.payload + (action.field to JsonPrimitive(value.trim())))); onDismiss()
    }, enabled = action.field == null || value.trim().length >= action.minimum) { Text("Onayla ve kaydet") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } })
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
    var legalTitle by remember { mutableStateOf("Kullanım Koşulları") }
    var legalContent by remember { mutableStateOf("") }
    val sections = listOf("venueApplications" to "Tesis başvuruları", "support" to "Destek", "reports" to "Şikayetler", "memories" to "Anılar", "photos" to "Fotoğraf doğrulama",
        "results" to "Maç sonuçları", "reviews" to "Tesis yorumları", "outboxFailures" to "Bildirim hataları", "users" to "Kullanıcı yönetimi", "announcements" to "Duyurular", "flags" to "Özellikler", "legal" to "Sözleşmeler")
    LaunchedEffect(wire, refresh) {
        loading = true; error = null
        try { data = backend.adminInbox() }
        catch (cancel: CancellationException) { throw cancel }
        catch (failure: Exception) { error = failure.message }
        finally { loading = false }
    }
    ActivitySheet("Operasyon merkezi", "Yalnız yetkili yönetici hesabı", onDismiss, Icons.Rounded.AdminPanelSettings) {
        MFloatingDropdownBox("İşlem alanı", section, sections.map { it.first }, { id -> sections.first { it.first == id }.second }, { section = it })
        TextButton(onClick = { refresh++ }, enabled = !loading) { Text(if (loading) "Yükleniyor…" else "Yenile") }
        error?.let { Text(it, color = MColors.Rose) }
        if (section == "announcements") {
            OutlinedTextField(announcementTitle, { announcementTitle = it.take(120) }, label = { Text("Başlık") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(announcementBody, { announcementBody = it.take(1000) }, label = { Text("Duyuru metni") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(announcementCity, { announcementCity = it.take(60) }, label = { Text("İl · boşsa tüm iller") }, modifier = Modifier.fillMaxWidth())
            Text("Yalnız kampanya bildirimine izin veren sporculara gönderilir. Yönetici başına günde bir duyuru yapılabilir.", style = MaterialTheme.typography.bodySmall)
            MButton("Duyuruyu gözden geçir", { action = OperatorAction("Duyuruyu gönder", "admin.announcement.send", apiPayload("title" to announcementTitle, "body" to announcementBody, "city" to announcementCity), field = null, message = "$announcementTitle\n$announcementBody\nBölge: ${announcementCity.ifBlank { "Tüm iller" }} · İzin veren kullanıcılar") }, enabled = announcementTitle.trim().length >= 3 && announcementBody.trim().length >= 10)
            data?.a("announcements").orEmpty().forEach { Text("Duyuru · ${it.i("recipients")} kullanıcı", style = MaterialTheme.typography.bodySmall) }
        } else if (section == "legal") {
            MFloatingDropdownBox("Sözleşme türü", legalDocKey, listOf("terms", "privacy", "community_rules"), {
                when (it) { "privacy" -> "Gizlilik ve KVKK"; "community_rules" -> "Topluluk ve Fair Play Kuralları"; else -> "Kullanım Koşulları" }
            }, {
                legalDocKey = it
                legalTitle = when (it) { "privacy" -> "Gizlilik ve KVKK Aydınlatma Metni"; "community_rules" -> "Topluluk ve Fair Play Kuralları"; else -> "Kullanım Koşulları" }
            })
            OutlinedTextField(legalVersion, { legalVersion = it.take(40) }, label = { Text("Sürüm (örn. 2026.2)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(legalTitle, { legalTitle = it.take(120) }, label = { Text("Başlık") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(legalContent, { legalContent = it.take(8000) }, label = { Text("Sözleşme metni (en az 40 karakter)") }, minLines = 4, modifier = Modifier.fillMaxWidth())
            MButton("Sözleşme sürümünü yayınla", {
                action = OperatorAction("Sözleşmeyi yayınla", "admin.legal.publish", apiPayload("documentKey" to legalDocKey, "version" to legalVersion.trim(), "title" to legalTitle.trim(), "content" to legalContent.trim()), minimum = 3)
            }, enabled = legalVersion.trim().length >= 3 && legalTitle.trim().length >= 3 && legalContent.trim().length >= 40)
        } else if (section == "flags") {
            listOf("billing", "memories", "tournaments", "venues", "chat").forEach { flag ->
                val enabled = wire?.a("featureFlags")?.firstOrNull { it.s("name") == flag }?.i("enabled", 1) != 0
                Row { Text(mapOf("billing" to "Mağaza", "memories" to "Anılar", "tournaments" to "Turnuvalar", "venues" to "Tesisler", "chat" to "Sohbet")[flag] ?: flag, Modifier.weight(1f)); Switch(enabled, { next -> action = OperatorAction("Özelliği ${if (next) "aç" else "kapat"}", "admin.feature.set", apiPayload("name" to flag, "enabled" to next), field = null) }) }
            }
        } else {
            if (section == "users") OutlinedTextField(search, { search = it }, label = { Text("Sporcu ara") }, modifier = Modifier.fillMaxWidth())
            val rows = data?.a(section).orEmpty().filter { section != "users" || it.s("name").contains(search, true) }
            if (rows.isEmpty() && !loading && error == null) Text("Bu alanda bekleyen kayıt yok.", color = MColors.Ink500)
            rows.forEach { item -> MSubCard {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    when (section) {
                        "venueApplications" -> {
                            Text(item.s("name"), style = MaterialTheme.typography.titleSmall)
                            Text("${item.s("applicantName")} · ${item.s("city")} · ${item.s("area")}\n${item.s("address")}")
                            Text("${item.i("courts")} saha · ${item.ss("sports").joinToString()}\n${item.ss("amenities").joinToString()}", style = MaterialTheme.typography.bodySmall)
                            Text("Onay, başvurana işletmeci yetkisi verir ve tesisi yayınlar. İşletme yetkisini doğruladıktan sonra onayla.", style = MaterialTheme.typography.bodySmall)
                            Row { listOf(true to "Onayla ve yayınla", false to "Reddet").forEach { (approved, title) -> TextButton(onClick = {
                                action = OperatorAction(title, "admin.venue.application.review", apiPayload("applicationId" to item.s("id"), "expectedVersion" to item.i("version", 1), "approve" to approved), field = "response", label = "Başvurana yanıt", minimum = 3)
                            }) { Text(title) } } }
                        }
                        "support" -> {
                            Text(item.s("subject"), style = MaterialTheme.typography.titleSmall); Text(item.s("message"))
                            TextButton(onClick = { action = OperatorAction("Destek talebini yanıtla", "admin.support.resolve", apiPayload("requestId" to item.s("id"), "status" to "resolved"), field = "response", label = "Kullanıcıya yanıt", minimum = 3) }) { Text("Yanıtla") }
                        }
                        "reports" -> {
                            Text(item.s("reason"), style = MaterialTheme.typography.titleSmall); Text(item.s("details")); item.o("context").takeIf { it.isNotEmpty() }?.let { Text("${it.s("title")}\n${it.s("description")}", style = MaterialTheme.typography.bodySmall) }; Text("${item.s("target_type")} · ${item.s("target_id")}", style = MaterialTheme.typography.bodySmall)
                            Row { listOf("resolved" to "İşlendi", "dismissed" to "Kapat").forEach { (status, title) -> TextButton(onClick = { action = OperatorAction(title, "admin.report.resolve", apiPayload("reportId" to item.s("id"), "status" to status)) }) { Text(title) } } }
                            if (item.s("target_type") == "memory" || item.s("target_type") == "venue_review") TextButton(onClick = {
                                val memory = item.s("target_type") == "memory"
                                action = OperatorAction("Bildirilen içeriği kaldır", if (memory) "memory.delete" else "venue.review.delete", apiPayload((if (memory) "memoryId" else "reviewId") to item.s("target_id")), field = null)
                            }) { Text("İçeriği kaldır") }
                        }
                        "memories" -> {
                            BackendPhoto(item.s("upload_id"), "İnceleme bekleyen anı", Modifier.fillMaxWidth().height(180.dp))
                            val memory = runCatching { Json.parseToJsonElement(item.s("payload")).jsonObject }.getOrNull()
                            Text(memory?.s("caption").orEmpty(), style = MaterialTheme.typography.bodySmall)
                            Row { listOf(true to "Onayla", false to "Reddet").forEach { (approved, title) -> TextButton(onClick = { action = OperatorAction("Anıyı $title", "admin.memory.review", apiPayload("memoryId" to item.s("id"), "approved" to approved), minimum = 3) }) { Text(title) } } }
                        }
                        "photos" -> {
                            if (item.b("stale")) {
                                Text("Bu talebin fotoğrafı güncel değil.")
                                TextButton(onClick = { action = OperatorAction("Eski talebi kapat", "admin.support.resolve", apiPayload("requestId" to item.s("id"), "status" to "rejected"), field = "response", label = "Açıklama", minimum = 3) }) { Text("Talebi kapat") }
                            } else {
                                BackendPhoto(item.s("uploadId"), "İncelenen profil fotoğrafı", Modifier.fillMaxWidth().height(180.dp))
                                Row { listOf(true to "Onayla", false to "Reddet").forEach { (approved, title) -> TextButton(onClick = { action = OperatorAction("Fotoğrafı $title", "admin.verification.photo", apiPayload("userId" to item.s("user_id"), "uploadId" to item.s("uploadId"), "requestId" to item.s("id"), "approved" to approved), minimum = 3) }) { Text(title) } } }
                            }
                        }
                        "results" -> {
                            val result = runCatching { Json.parseToJsonElement(item.s("payload")).jsonObject }.getOrNull()
                            val event = runCatching { Json.parseToJsonElement(item.s("event_payload")).jsonObject }.getOrNull()
                            Text(event?.s("title") ?: "Maç sonucu", style = MaterialTheme.typography.titleSmall)
                            Text("${result?.i("homeScore")} – ${result?.i("awayScore")} · ${item.s("status")}")
                            Row { listOf(true to "Doğrula", false to "Reddet").forEach { (approved, title) -> TextButton(onClick = { action = OperatorAction("Sonucu $title", "admin.result.resolve", apiPayload("eventId" to item.s("event_id"), "approve" to approved)) }) { Text(title) } } }
                        }
                        "reviews" -> {
                            Text("${item.i("rating")}/5 · ${item.s("comment")}")
                            Row { listOf(true to "Yayınla", false to "Reddet").forEach { (approved, title) -> TextButton(onClick = { action = OperatorAction(title, "admin.venue.review", apiPayload("reviewId" to item.s("id"), "approved" to approved)) }) { Text(title) } } }
                        }
                        "outboxFailures" -> {
                            Text("Bildirim · ${item.i("attempts")} deneme")
                            TextButton(onClick = { action = OperatorAction("Bildirimi tekrar sıraya al", "admin.outbox.retry", apiPayload("outboxId" to item.s("id")), field = null) }) { Text("Tekrar dene") }
                        }
                        "users" -> {
                            Text("${item.s("name")} · ${item.s("role")}")
                            if (item.s("role") != "admin" && item.s("status") == "active") TextButton(onClick = { action = OperatorAction("İşletmeci yetkisini değiştir", "admin.operator.assign", apiPayload("userId" to item.s("id"), "role" to if (item.s("role") == "venue_owner") "user" else "venue_owner")) }) { Text(if (item.s("role") == "venue_owner") "Yetkiyi kaldır" else "İşletmeci yap") }
                            if (item.s("role") != "admin") TextButton(onClick = { action = OperatorAction("Hesap erişimini değiştir", "admin.user.status", apiPayload("userId" to item.s("id"), "status" to if (item.s("status") == "suspended") "active" else "suspended")) }) { Text(if (item.s("status") == "suspended") "Hesabı yeniden aç" else "Hesabı askıya al") }
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
        catch (failure: Exception) { error = failure.message }
    }
    ActivitySheet("Tesis yönetimi", "Saat, rezervasyon ve tesiste ödeme kayıtları", onDismiss, Icons.Rounded.Stadium) {
        MButton("Tesis ekle", { create = true }, icon = Icons.Rounded.Add)
        TextButton(onClick = { refresh++ }) { Text("Yenile") }
        error?.let { Text(it, color = MColors.Rose) }
        data?.a("items").orEmpty().forEach { venue -> MCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(venue.s("name"), style = MaterialTheme.typography.titleMedium)
                Text("${venue.s("city")} · ${venue.s("area")}", style = MaterialTheme.typography.bodySmall)
                if (venue.b("archived")) Text("Yeni rezervasyonlara kapalı", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
                else {
                    Row {
                        TextButton(onClick = { editVenue = venue }) { Text("Tesisi düzenle") }
                        TextButton(onClick = { createSlot = venue }) { Text("Müsait saat ekle") }
                    }
                    TextButton(onClick = { planSlots = venue }) { Text("Haftalık müsaitlik planı") }
                    TextButton(onClick = { action = OperatorAction("Tesisi yeni rezervasyonlara kapat", "venue.archive", apiPayload("venueId" to venue.s("id"), "expectedVersion" to venue.i("version", 1)), field = null,
                        message = "Gelecekteki tahsil edilmemiş rezervasyonlar iptal edilip müşterilere bildirilir. Ödenmiş veya devam eden rezervasyon varsa önce sonuçlandırılmalıdır. Geçmiş kayıtlar korunur.") }) { Text("Tesisi kapat", color = MColors.Rose) }
                }
                venue.a("slots").forEach { slot ->
                    val start = kotlinx.datetime.Instant.fromEpochMilliseconds(slot.l("starts_at")).toLocalDateTime(TimeZone.of("Europe/Istanbul")).toString().replace('T', ' ')
                    Text("${slot.s("court")} · $start · ₺${formatTryMinor(slot.i("priceMinor"))} · ${slot.s("status")}", style = MaterialTheme.typography.bodySmall)
                    val futureSlot = slot.l("starts_at") > kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                    val activeBooking = venue.a("bookings").any { it.s("slot_id") == slot.s("id") && it.s("status") in listOf("pending", "confirmed") }
                    if (!venue.b("archived") && slot.s("status") == "open" && futureSlot) Row {
                        if (!activeBooking) TextButton(onClick = { editSlot = venue to slot }) { Text("Saati / ücreti düzenle") }
                        TextButton(onClick = { action = OperatorAction("Saati kapat", "venue.slot.cancel", apiPayload("slotId" to slot.s("id"), "expectedVersion" to slot.i("version", 1)), field = null) }) { Text("Saati kapat") }
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
                        if (status in listOf("pending", "confirmed") && !paid) TextButton(onClick = { action = OperatorAction("Rezervasyonu iptal et", "booking.cancel", apiPayload("bookingId" to booking.s("id")), field = null) }) { Text("İptal et") }
                        if (status in listOf("confirmed", "completed") && paid) TextButton(onClick = { action = OperatorAction("Tesiste yapılan iadeyi kaydet", "booking.refund.record", apiPayload("bookingId" to booking.s("id")), field = "reference", label = "İade referansı", minimum = 3, message = "Bu işlem para göndermez. Kullanıcıya yapılmış tam iadenin kaydını oluşturur ve rezervasyonu kapatır.") }) { Text("İadeyi kaydet") }
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

@Composable
fun CreateVenueSheet(venue: JsonObject? = null, application: Boolean = false, onDismiss: () -> Unit) {
    val backend = LocalBackendRepository.current
    val wire = backend?.snapshots?.collectAsState()?.value
    var name by remember { mutableStateOf(venue?.s("name").orEmpty()) }
    var city by remember { mutableStateOf(venue?.s("city") ?: wire?.o("profile")?.s("city", "İstanbul") ?: "İstanbul") }
    var area by remember { mutableStateOf(venue?.s("area") ?: wire?.o("profile")?.s("area", "Kadıköy") ?: "Kadıköy") }
    var address by remember { mutableStateOf(venue?.s("address").orEmpty()) }
    var phone by remember { mutableStateOf(venue?.s("phone").orEmpty()) }
    var description by remember { mutableStateOf(venue?.s("description").orEmpty()) }
    var point by remember { mutableStateOf(venue?.o("approximateLocation")?.takeIf { it.isNotEmpty() }?.let { loc -> loc.s("latitude").toDoubleOrNull()?.let { lat -> loc.s("longitude").toDoubleOrNull()?.let { lon -> EventGeoPoint(lat, lon) } } }) }
    var pickingPoint by remember { mutableStateOf(false) }
    var sport by remember { mutableStateOf(Sport.FOOTBALL) }
    var sports by remember { mutableStateOf(venue?.ss("sports")?.mapNotNull { name -> Sport.entries.firstOrNull { it.name == name } }?.takeIf { it.isNotEmpty() } ?: listOf(Sport.FOOTBALL)) }
    var amenities by remember { mutableStateOf(venue?.ss("amenities")?.joinToString(", ").orEmpty()) }
    var courts by remember { mutableIntStateOf(venue?.i("courts", 1) ?: 1) }
    var cancellation by remember { mutableIntStateOf(venue?.i("cancellationHours", 24) ?: 24) }
    val command = rememberBackendAction()
    ActivitySheet(if (application) "Tesisini ekle" else if (venue == null) "Tesis ekle" else "Tesisi düzenle", if (application) "İşletmeci başvurusu · yönetici onayı gerekir" else "Tesisinin bilgilerini kaydet", onDismiss, Icons.Rounded.Stadium) {
        OutlinedTextField(name, { name = it.take(100) }, label = { Text("Tesis adı") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(city, { city = it.take(60) }, label = { Text("İl") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(area, { area = it.take(60) }, label = { Text("İlçe") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(address, { address = it.take(300) }, label = { Text("Açık tesis adresi${if (application) "" else " · isteğe bağlı"}") }, modifier = Modifier.fillMaxWidth())
        TextButton(onClick = { pickingPoint = true }) { Text(if (point == null) "Haritadan tesis konumu seç (isteğe bağlı)" else "Harita konumunu değiştir") }
        OutlinedTextField(phone, { phone = it.take(30) }, label = { Text("İletişim telefonu · isteğe bağlı") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(description, { description = it.take(600) }, label = { Text("Tesis açıklaması · isteğe bağlı") }, modifier = Modifier.fillMaxWidth())
        SportPickerField(sport, { selected -> if (selected != null) { sport = selected; if (selected !in sports && sports.size < 20) sports = sports + selected } }, allowAll = false)
        Text("Branş seçerek ekle; çıkarmak için adına dokun. En az bir branş gerekir.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
        sports.forEach { selected -> TextButton(onClick = { sports = sports - selected }, enabled = sports.size > 1) { Text("${selected.emoji} ${selected.label} · kaldır") } }
        OutlinedTextField(amenities, { amenities = it.take(1800) }, label = { Text("Olanaklar · virgülle ayır") }, modifier = Modifier.fillMaxWidth())
        MFloatingDropdownBox("Saha sayısı", courts, (1..50).toList(), { "$it" }, { courts = it })
        MFloatingDropdownBox("Ücretsiz iptal sınırı", cancellation, (listOf(0, 6, 12, 24, 48, 72, 168) + cancellation).distinct().sorted(), { "$it saat önce" }, { cancellation = it })
        Text("İptal politikası mevcut rezervasyonların son tarihini değiştirmez. Aktif rezervasyon varken bölge, adres, branş ve saha sayısı değiştirilemez.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
        val cleanAmenities = amenities.split(',').map(String::trim).filter(String::isNotBlank).distinct()
        if (application) Text("Başvurun yöneticilere gider. Onaylandığında tesisin yayınlanır; tesis yönetiminden gün, saat, süre ve ücret belirleyerek rezervasyona açabilirsin.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
        if (backend == null) Text("Önizleme formu · başvuru göndermek için canlı hesabın gerekir.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
        MButton(if (application) "Başvuruyu gönder" else "Tesisi kaydet", {
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
                "cancellationHours" to cancellation
            )
            command(if (application) "venue.application.create" else if (venue == null) "venue.create" else "venue.update", if (application || venue == null) fields else JsonObject(fields + apiPayload("venueId" to venue.s("id"), "expectedVersion" to venue.i("version", 1))))
            onDismiss()
        }, enabled = backend?.authenticated == true && name.trim().length >= 3 && city.trim().length >= 2 && area.trim().length >= 2 &&
            (address.trim().length >= 10 || !application && address.isBlank()) && sports.isNotEmpty() && cleanAmenities.size <= 30 && cleanAmenities.all { it.length <= 60 })
    }
    if (pickingPoint) EventLocationPicker(initialPoint = point, initialAddress = address, city = city, district = area, onDismiss = { pickingPoint = false }, onConfirm = { selected, pickedAddress -> point = selected; if (address.isBlank()) address = pickedAddress; pickingPoint = false })
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
        OutlinedTextField(court,{court=it.take(60)},label={Text("Saha / kort adı")},modifier=Modifier.fillMaxWidth())
        MFloatingDropdownBox("Planın uzunluğu",days,listOf(7,14,28,56),{"$it gün"},{days=it})
        listOf("Pzt","Sal","Çar","Per","Cum","Cmt","Paz").forEachIndexed { index,label -> Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(index+1 in weekdays,{checked->weekdays=if(checked)weekdays+(index+1) else weekdays-(index+1)});Text(label)
        } }
        MFloatingDropdownBox("Günlük saat sayısı",perDay,(1..8).toList(),{"$it rezervasyon aralığı"},{perDay=it})
        MFloatingDropdownBox("Her aralığın süresi",duration,listOf(30,60,90,120),{"$it dakika"},{duration=it})
        OutlinedTextField(price,{price=it.take(10)},label={Text("Her aralığın bedeli · TL")},modifier=Modifier.fillMaxWidth())
        Text("Seçtiğin günlerde başlangıç saatinden itibaren ardışık saatler açılır. Plan en fazla 120 saat içerebilir. Tek çakışma varsa hiçbir saat eklenmez; mevcut rezervasyonlar korunur.",style=MaterialTheme.typography.bodySmall)
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
        OutlinedTextField(court, { court = it.take(60) }, label = { Text("Saha / kort") }, modifier = Modifier.fillMaxWidth())
        EventDateTimeField(date, hour, minute) { d, h, m -> date = d; hour = h; minute = m }
        MFloatingDropdownBox("Süre", duration, (listOf(15, 30, 60, 90, 120, 180, 240) + duration).distinct().sorted(), { "$it dakika" }, { duration = it })
        OutlinedTextField(price, { price = it.filter { char -> char.isDigit() || char == ',' || char == '.' }.take(10) }, label = { Text("Toplam saha bedeli · TL, kuruş yazılabilir") }, modifier = Modifier.fillMaxWidth())
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
