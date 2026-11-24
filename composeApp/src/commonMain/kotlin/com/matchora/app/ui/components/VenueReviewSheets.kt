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
        reviewable.forEach { payment ->
            MPremiumMenuRow(
                text = "Rezervasyonunu değerlendir",
                subtitle = payment.description,
                icon = Icons.Rounded.RateReview,
                onClick = { editing = payment }
            )
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let {
            Text(it, color = MColors.Rose)
            MPillActionButton("Tekrar dene", { refresh++ }, icon = Icons.Rounded.Refresh, tone = MPillTone.NEUTRAL, compact = true)
        }
        if (rows.isEmpty() && !busy && error == null) Text("Henüz yayınlanmış değerlendirme yok.")
        rows.forEach { review -> MSubCard {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${review.s("authorName")} · ${review.i("rating")}/5", style = MaterialTheme.typography.titleSmall)
                Text(review.s("comment"), style = MaterialTheme.typography.bodySmall)
                if (review.s("status") == "pending") Text("İnceleme bekliyor", color = MColors.Ink500)
                if (backend != null && !review.b("own")) {
                    MPillActionButton("Yorumu bildir", { report = review }, icon = Icons.Rounded.Flag, tone = MPillTone.DANGER, compact = true)
                }
            }
        } }
        if (backend != null && nextCursor != null) MPillActionButton(
            text = "Önceki değerlendirmeleri yükle",
            onClick = { actions.launch {
                val cursor = nextCursor ?: return@launch
                busy = true; error = null
                try { val page = backend.venueReviews(venue.id, cursor); rows = (rows + page.a("items")).distinctBy { it.s("id") }; nextCursor = page.s("nextCursor").ifBlank { null } }
                catch (cancel: CancellationException) { throw cancel }
                catch (failure: Exception) { error = (failure as? BackendFailure)?.message ?: "Değerlendirmeler yüklenemedi." }
                finally { busy = false }
            } },
            icon = Icons.Rounded.ExpandMore,
            tone = MPillTone.NEUTRAL,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        )
    }
    report?.let { review -> ReportAndDisputeDialog("${venue.name} değerlendirmesi", { report = null }, { reason, details -> onReport(review.s("id"), reason, details); report = null }) }
    editing?.let { payment -> VenueReviewEditorSheet(payment, onSaved = { editing = null; refresh++ }) { editing = null } }
}

@Composable
fun VenueReviewEditorSheet(payment: VenuePayment, onSaved: (() -> Unit)? = null, onDismiss: () -> Unit) {
    val backend = LocalBackendRepository.current
    val wire = if (backend != null) backend.snapshots.collectAsState().value else null
    val original = remember(payment.id) { wire?.a("venueReviews")?.firstOrNull { it.s("booking_id") == payment.id } }
    var stars by remember { mutableIntStateOf(original?.i("rating", 5) ?: 5) }
    var comment by remember { mutableStateOf(original?.s("comment").orEmpty()) }
    var remove by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val actions = rememberCoroutineScope()
    val feedback = LocalToastNotifier.current
    fun submitReview(action: String, payload: JsonObject, successMessage: String) {
        val repo = backend ?: return
        actions.launch {
            busy = true; error = null
            try {
                repo.command(action, payload, repo.requestKey("review"))
                feedback?.invoke(successMessage, "✓")
                (onSaved ?: onDismiss).invoke()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (failure: Exception) {
                error = (failure as? BackendFailure)?.message ?: "İşlem kaydedilemedi."
            } finally {
                busy = false
            }
        }
    }
    ActivitySheet("Tesisi değerlendir", payment.venue, onDismiss, Icons.Rounded.StarOutline) {
        Text("Değerlendirmen kontrol edildikten sonra yayınlanır. Aynı rezervasyon için tek yorum kaydı tutulur.", style = MaterialTheme.typography.bodySmall)
        MFloatingDropdownBox("Puanın", stars, (1..5).toList(), { "$it / 5" }, { stars = it })
        MPremiumTextField(comment, { comment = it.take(1000) }, label = "Deneyimini anlat", singleLine = false, minLines = 3, maxLines = 5)
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MColors.Rose, style = MaterialTheme.typography.bodySmall) }
        MButton("İncelemeye gönder", {
            submitReview(
                "venue.review.save",
                apiPayload("bookingId" to payment.id, "rating" to stars, "comment" to comment, "expectedVersion" to original?.i("version")),
                "Değerlendirmen gönderildi"
            )
        }, enabled = !busy && backend?.authenticated == true)
        if (original != null && original.s("status") != "deleted") {
            MPillActionButton("Değerlendirmemi kaldır", { remove = true }, icon = Icons.Rounded.DeleteOutline, tone = MPillTone.DANGER, compact = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
        }
    }
    if (remove) AlertDialog(onDismissRequest = { remove = false }, title = { Text("Değerlendirme kaldırılsın mı?") }, text = { Text("Yorumun ve puanın tesisin görünümünden kaldırılacak.") },
        confirmButton = { TextButton(enabled = !busy, onClick = { remove = false; submitReview("venue.review.delete", apiPayload("reviewId" to original?.s("id")), "Değerlendirmen kaldırıldı") }) { Text("Kaldır", color = MColors.Rose) } }, dismissButton = { TextButton(onClick = { remove = false }) { Text("Vazgeç") } })
}
