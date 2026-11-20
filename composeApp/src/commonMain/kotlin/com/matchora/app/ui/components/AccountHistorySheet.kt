package com.matchora.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.matchora.app.data.*
import com.matchora.app.state.*
import com.matchora.app.ui.theme.MColors
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonObject

private val accountHistoryKinds = listOf("reservations", "support", "reports")

@Composable
fun AccountHistoryButton(kind: String, label: String) {
    val backend = LocalBackendRepository.current ?: return
    val requireAccount = LocalRequireAccount.current
    var open by remember(backend.userId) { mutableStateOf(false) }
    TextButton(onClick = { requireAccount { open = true } }) {
        Icon(Icons.Rounded.History, null, Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label)
    }
    if (open) AccountHistorySheet(kind) { open = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountHistorySheet(initialKind: String, onDismiss: () -> Unit) {
    val backend = LocalBackendRepository.current
    val owner = backend?.userId
    var kind by remember(owner) { mutableStateOf(initialKind.takeIf { it in accountHistoryKinds } ?: "reservations") }
    var rows by remember(owner, kind) { mutableStateOf(emptyList<JsonObject>()) }
    var cursor by remember(owner, kind) { mutableStateOf<String?>(null) }
    var page by remember(owner, kind) { mutableIntStateOf(0) }
    var busy by remember(owner, kind) { mutableStateOf(true) }
    var error by remember(owner, kind) { mutableStateOf<String?>(null) }
    var review by remember(owner) { mutableStateOf<VenuePayment?>(null) }
    LaunchedEffect(backend, owner, kind, page) {
        busy = true
        error = null
        try {
            if (backend == null || !backend.authenticated) throw BackendFailure("UNAUTHENTICATED", "Geçmişini görmek için giriş yap.")
            val result = backend.personalHistory(kind, if (page == 0) null else cursor)
            rows = (rows + result.a("items")).distinctBy { it.s("id") }
            cursor = result.s("nextCursor").ifBlank { null }
        } catch (cancel: CancellationException) { throw cancel }
        catch (failure: Exception) {
            if (failure is BackendFailure && failure.code == "INVALID_CURSOR") { rows = emptyList(); cursor = null }
            error = failure.message ?: "Geçmiş yüklenemedi."
        } finally { busy = false }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss, containerColor = MColors.Canvas,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.statusBarsPadding().padding(top = 12.dp),
        dragHandle = { MPremiumSheetHandle() }
    ) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(.9f).navigationBarsPadding().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MSheetHeading("Hesap geçmişin", "Rezervasyonların ve iletişim kayıtların", Icons.Rounded.History, onDismiss)
            MSegmentedControl(listOf("Sahalar", "Destek", "Şikâyetler"), accountHistoryKinds.indexOf(kind), { kind = accountHistoryKinds[it] })
            LazyColumn(
                Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(rows, key = { it.s("id") }) { record ->
                    AccountHistoryRow(kind, record) { payment -> review = payment }
                }
                if (busy) item {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(24.dp), color = MColors.BrandDeep, strokeWidth = 2.dp)
                    }
                }
                error?.let { message -> item {
                    Text(message, color = MColors.Rose, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { page++ }, enabled = !busy) { Text("Tekrar dene") }
                } }
                if (!busy && error == null && rows.isEmpty()) item {
                    Text("Bu bölümde henüz kayıt yok.", color = MColors.Ink500)
                }
                if (!busy && error == null && cursor != null) item {
                    MButton("Daha eski kayıtları yükle", { busy = true; page++ }, isPrimary = false)
                }
            }
        }
    }
    review?.let { payment -> VenueReviewEditorSheet(payment) { review = null } }
}

@Composable
private fun AccountHistoryRow(kind: String, record: JsonObject, onReview: (VenuePayment) -> Unit) {
    val stamp = record.l("createdAt")
    val date = if (stamp > 0) Instant.fromEpochMilliseconds(stamp).toLocalDateTime(TimeZone.of("Europe/Istanbul")).date.toString() else ""
    val status = when (record.s("status")) {
        "resolved" -> "Yanıtlandı"; "rejected", "dismissed" -> "Kapandı"
        "CANCELLED" -> "İptal edildi"; "REFUNDED" -> "İade edildi"; "PAID" -> "Ödendi"
        "PENDING" -> "Ödeme bekleniyor"; else -> "İnceleniyor"
    }
    MCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(when (kind) {
                "reservations" -> record.s("venue")
                "support" -> record.s("subject")
                else -> record.s("reason")
            }, style = MaterialTheme.typography.titleSmall)
            Text("$date · $status", style = MaterialTheme.typography.labelMedium, color = MColors.BrandDeep)
            if (kind == "reservations") {
                val payment = remember(record) { BackendSnapshot(apiPayload("venuePayments" to listOf(record))).venuePayments.single() }
                Text("${payment.description} · ₺${payment.amountLabel}", style = MaterialTheme.typography.bodySmall, color = MColors.Ink600)
                if (payment.bookingStatus == "completed" && payment.status == PaymentStatus.PAID) {
                    TextButton(onClick = { onReview(payment) }) { Text("Tesisi değerlendir") }
                }
            } else {
                val text = if (kind == "support") record.s("message") else record.s("details")
                if (text.isNotBlank()) Text(text, style = MaterialTheme.typography.bodySmall, color = MColors.Ink600)
                record.s("response").takeIf { it.isNotBlank() }?.let {
                    Text("Yanıt: $it", style = MaterialTheme.typography.bodySmall, color = MColors.Ink800)
                }
            }
        }
    }
}
