package com.matchora.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.matchora.app.data.*
import com.matchora.app.domain.Sport
import com.matchora.app.state.label
import com.matchora.app.ui.theme.MColors
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonObject

private val personalHistoryKinds = listOf("matches", "ratings", "reliability", "wallet")

/** A paginated view independent of bootstrap limits, with a lazy list for long histories. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalHistorySheet(initialKind: String = "matches", onDismiss: () -> Unit) {
    val backend = LocalBackendRepository.current
    val owner = backend?.userId
    var kind by remember(owner) { mutableStateOf(initialKind.takeIf { it in personalHistoryKinds } ?: "matches") }
    var rows by remember(owner, kind) { mutableStateOf(emptyList<JsonObject>()) }
    var cursor by remember(owner, kind) { mutableStateOf<String?>(null) }
    var requestedPage by remember(owner, kind) { mutableIntStateOf(0) }
    var loading by remember(owner, kind) { mutableStateOf(true) }
    var error by remember(owner, kind) { mutableStateOf<String?>(null) }
    LaunchedEffect(backend, owner, kind, requestedPage) {
        loading = true
        error = null
        try {
            if (backend == null || !backend.authenticated) {
                error = "Geçmişini görmek için giriş yap."
            } else {
                val page = backend.personalHistory(kind, if (requestedPage == 0) null else cursor)
                val items = if (kind == "wallet") {
                    page.a("items").filterNot { it.s("ref") in setOf("welcome", "passport_complete") }
                } else page.a("items")
                rows = (rows + items).distinctBy { it.s("id") }
                cursor = page.s("nextCursor").ifBlank { null }
            }
        } catch (cancel: kotlinx.coroutines.CancellationException) {
            throw cancel
        } catch (failure: Exception) {
            if (failure is BackendFailure && failure.code == "INVALID_CURSOR") {
                rows = emptyList()
                cursor = null
            }
            error = failure.message ?: "Geçmiş yüklenemedi. Tekrar dene."
        } finally {
            loading = false
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.statusBarsPadding().padding(top = 12.dp),
        containerColor = MColors.Canvas,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { MPremiumSheetHandle() }
    ) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(.9f).navigationBarsPadding().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MSheetHeading("Geçmişin", "Maçların, gelişimin ve Token hareketlerin", Icons.Rounded.History, onDismiss)
            MSegmentedControl(
                labels = listOf("Maçlar", "MMR", "Güven", "Cüzdan"),
                selectedIndex = personalHistoryKinds.indexOf(kind),
                onSelect = { kind = personalHistoryKinds[it] }
            )
            LazyColumn(
                Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rows, key = { it.s("id") }) { record -> PersonalHistoryRow(kind, record) }
                if (error != null) item {
                    MSubCard {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(error.orEmpty(), color = MColors.Rose, style = MaterialTheme.typography.bodySmall)
                            MButton("Tekrar dene", { requestedPage++ }, isPrimary = false, enabled = !loading)
                        }
                    }
                }
                if (loading) item {
                    Box(Modifier.fillMaxWidth().padding(18.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(24.dp), color = MColors.BrandDeep, strokeWidth = 2.dp)
                    }
                }
                if (!loading && rows.isEmpty() && error == null) item {
                    Text("Bu bölümde henüz kayıt yok.", Modifier.padding(vertical = 20.dp), color = MColors.Ink500)
                }
                if (!loading && error == null && cursor != null) item {
                    MButton("Daha eski kayıtları yükle", { requestedPage++ }, isPrimary = false)
                }
            }
        }
    }
}

@Composable
private fun PersonalHistoryRow(kind: String, record: JsonObject) {
    val sport = runCatching { Sport.valueOf(record.s("sport")).label }.getOrDefault("")
    val title = when (kind) {
        "matches" -> record.s("title", "Karşılaşma")
        "ratings" -> "$sport · MMR"
        "reliability" -> record.s("reason", "Güvenilirlik")
        else -> record.s("description", "Token hareketi")
    }
    val timestamp = record.l("createdAt").takeIf { it > 0 } ?: record.l("created_at").takeIf { it > 0 }
    val date = timestamp?.let {
        Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.of("Europe/Istanbul")).date.toString()
    } ?: record.s("date").ifBlank { record.s("time") }
    val detail = when (kind) {
        "matches" -> listOf(sport, date).filter { it.isNotBlank() }.joinToString(" · ")
        "reliability" -> listOf(record.s("detail"), date).filter { it.isNotBlank() }.joinToString(" · ")
        else -> date
    }
    val delta = when (kind) {
        "matches" -> record.i("mmrDelta")
        "wallet" -> if (record.s("type") == "SPEND" && record.i("amount") == 0) record.i("lockedDelta") else record.i("amount")
        else -> record.i("delta")
    }
    val unit = when (kind) { "wallet" -> "TKN"; "reliability" -> "puan"; else -> "MMR" }
    val deltaColor = when {
        kind == "wallet" && record.s("type") == "HOLD" -> MColors.Amber
        delta > 0 -> MColors.Mint
        delta < 0 -> MColors.Rose
        else -> MColors.Ink500
    }
    MSubCard {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    color = MColors.Ink900, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall,
                    color = MColors.Ink500, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (kind == "matches") Text(record.s("score"), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = MColors.Ink900)
                if (kind == "ratings") Text("${record.i("rating")} MMR", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, color = MColors.Ink900)
                Text("${if (delta > 0) "+" else ""}$delta $unit", color = deltaColor,
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PersonalHistoryButton(kind: String, label: String) {
    val backend = LocalBackendRepository.current
    if (backend == null) return
    var open by remember(backend.userId) { mutableStateOf(false) }
    val requireAccount = LocalRequireAccount.current
    TextButton(onClick = { requireAccount { open = true } }) {
        Icon(Icons.Rounded.History, null, Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label)
    }
    if (open) PersonalHistorySheet(kind) { open = false }
}
