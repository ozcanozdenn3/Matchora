package com.matchora.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.matchora.app.data.*
import com.matchora.app.state.*
import com.matchora.app.ui.theme.MColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

@Composable
fun VenueReviewsSheet(venue: VenueInfo, onDismiss: () -> Unit, onReport: (String, String, String) -> Unit) {
    val backend = LocalBackendRepository.current
    val backendWire = backend?.snapshots?.collectAsState()?.value
    var reviewable by remember(venue.id) { mutableStateOf<List<VenuePayment>>(emptyList()) }
    var editing by remember(venue.id) { mutableStateOf<VenuePayment?>(null) }
    var rows by remember(venue.id) { mutableStateOf<List<JsonObject>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var report by remember { mutableStateOf<JsonObject?>(null) }
    var busy by remember { mutableStateOf(false) }
    var nextCursor by remember(venue.id) { mutableStateOf<String?>(null) }
    val actions = rememberCoroutineScope()
    LaunchedEffect(venue.id, refresh, backend?.userId) {
        if (backend == null) {
            rows = listOf(apiPayload("id" to "demo-review", "authorName" to "Demo sporcu", "rating" to 5, "comment" to "Saha ve organizasyon çok iyiydi.", "status" to "published"))
        } else {
            busy = true; error = null
            try { val page = backend.venueReviews(venue.id); rows = page.a("items"); nextCursor = page.s("nextCursor").ifBlank { null }; reviewable = BackendSnapshot(apiPayload("venuePayments" to page.a("reviewableBookings"))).venuePayments }
            catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) { error = (failure as? BackendFailure)?.message ?: "Değerlendirmeler yüklenemedi." }
            finally { busy = false }
        }
    }
    ActivitySheet("Tesis değerlendirmeleri", venue.name, onDismiss, Icons.Rounded.RateReview) {
        Text("Tamamlanan rezervasyonlardan, inceleme sonrası yayınlanan yorumlar.", style = MaterialTheme.typography.bodySmall)
        if (backend == null) Text("Önizleme değerlendirmesi", color = MColors.Ink500, style = MaterialTheme.typography.bodySmall)
        reviewable.forEach { payment -> TextButton(onClick = { editing = payment }) { Text("Rezervasyonunu değerlendir · ${payment.description}") } }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MColors.Rose); TextButton(onClick = { refresh++ }) { Text("Tekrar dene") } }
        if (rows.isEmpty() && !busy && error == null) Text("Henüz yayınlanmış değerlendirme yok.")
        rows.forEach { review -> MSubCard {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${review.s("authorName")} · ${review.i("rating")}/5", style = MaterialTheme.typography.titleSmall)
                Text(review.s("comment"), style = MaterialTheme.typography.bodySmall)
                if (review.s("status") == "pending") Text("İnceleme bekliyor", color = MColors.Ink500)
                if (backend != null && !review.b("own")) TextButton(onClick = { report = review }) { Text("Yorumu bildir", color = MColors.Rose) }
            }
        } }
        if (backend != null && nextCursor != null) TextButton(enabled = !busy, onClick = { actions.launch {
            val cursor = nextCursor ?: return@launch
            busy = true; error = null
            try { val page = backend.venueReviews(venue.id, cursor); rows = (rows + page.a("items")).distinctBy { it.s("id") }; nextCursor = page.s("nextCursor").ifBlank { null } }
            catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) { error = (failure as? BackendFailure)?.message ?: "Değerlendirmeler yüklenemedi." }
            finally { busy = false }
        } }) { Text("Önceki değerlendirmeleri yükle") }
    }
    report?.let { review -> ReportAndDisputeDialog("${venue.name} değerlendirmesi", { report = null }, { reason, details -> onReport(review.s("id"), reason, details); report = null }) }
    editing?.let { payment -> VenueReviewEditorSheet(payment) { editing = null; refresh++ } }
}

@Composable
fun VenueReviewEditorSheet(payment: VenuePayment, onDismiss: () -> Unit) {
    val backend = LocalBackendRepository.current
    val wire = if (backend != null) backend.snapshots.collectAsState().value else null
    val original = remember(payment.id) { wire?.a("venueReviews")?.firstOrNull { it.s("booking_id") == payment.id } }
    var stars by remember { mutableIntStateOf(original?.i("rating", 5) ?: 5) }
    var comment by remember { mutableStateOf(original?.s("comment").orEmpty()) }
    var remove by remember { mutableStateOf(false) }
    val command = rememberBackendAction()
    ActivitySheet("Tesisi değerlendir", payment.venue, onDismiss, Icons.Rounded.StarOutline) {
        Text("Değerlendirmen kontrol edildikten sonra yayınlanır. Aynı rezervasyon için tek yorum kaydı tutulur.", style = MaterialTheme.typography.bodySmall)
        MFloatingDropdownBox("Puanın", stars, (1..5).toList(), { "$it / 5" }, { stars = it })
        OutlinedTextField(comment, { comment = it.take(1000) }, label = { Text("Deneyimini anlat") }, minLines = 3, modifier = Modifier.fillMaxWidth(), colors = premiumTextFieldColors())
        MButton("İncelemeye gönder", { command("venue.review.save", apiPayload("bookingId" to payment.id, "rating" to stars, "comment" to comment, "expectedVersion" to original?.i("version"))); onDismiss() }, enabled = backend?.authenticated == true)
        if (original != null && original.s("status") != "deleted") TextButton(onClick = { remove = true }) { Text("Değerlendirmemi kaldır", color = MColors.Rose) }
    }
    if (remove) AlertDialog(onDismissRequest = { remove = false }, title = { Text("Değerlendirme kaldırılsın mı?") }, text = { Text("Yorumun ve puanın tesisin görünümünden kaldırılacak.") },
        confirmButton = { TextButton(onClick = { command("venue.review.delete", apiPayload("reviewId" to original?.s("id"))); remove = false; onDismiss() }) { Text("Kaldır", color = MColors.Rose) } }, dismissButton = { TextButton(onClick = { remove = false }) { Text("Vazgeç") } })
}
