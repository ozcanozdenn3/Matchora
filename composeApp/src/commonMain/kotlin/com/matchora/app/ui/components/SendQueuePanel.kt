package com.matchora.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.matchora.app.data.*
import com.matchora.app.ui.theme.MColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun PendingSendsPanel(backend: BackendRepository, threadId: String? = null, allDestinations: Boolean = false) {
    val items by backend.pendingSends.collectAsState()
    val scope = rememberCoroutineScope()
    var error by remember(backend.userId) { mutableStateOf<String?>(null) }
    var removalId by remember(backend.userId) { mutableStateOf<String?>(null) }
    var replacingId by remember(backend.userId) { mutableStateOf<String?>(null) }
    val openPicker = rememberGalleryPhotoPicker { photo ->
        replacingId?.let { id ->
            scope.launch {
                try {
                    backend.replacePendingPhoto(id, photo)
                    backend.processPendingSends()
                } catch (cancel: CancellationException) { throw cancel }
                catch (failure: Exception) { error = failure.message }
                finally { replacingId = null }
            }
        }
    }
    val visible = pendingSendsForDisplay(items, threadId, allDestinations)
    val sendCard: @Composable (PendingSend) -> Unit = { item ->
            MCard(Modifier.fillMaxWidth(), backgroundColor = MColors.Surface) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (allDestinations) Text(item.destination, style = MaterialTheme.typography.labelMedium, color = MColors.BrandDeep)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (item.hasPhoto) Icons.Rounded.Photo else Icons.Rounded.ChatBubbleOutline,
                            contentDescription = null,
                            tint = MColors.BrandDeep
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            item.text.ifBlank { if (item.hasPhoto) "Fotoğraf" else "Mesaj" },
                            Modifier.weight(1f), maxLines = 2
                        )
                        if (item.state in listOf("failed", "queued")) {
                            IconButton(onClick = { removalId = item.id }) {
                                Icon(Icons.Rounded.Close, "Bekleyen gönderimi kaldır")
                            }
                        }
                    }
                    Text(
                        item.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.state == "failed") MColors.Rose else MColors.Ink600
                    )
                    if (item.state == "uploading") {
                        LinearProgressIndicator(
                            progress = { item.percent / 100f },
                            modifier = Modifier.fillMaxWidth(), color = MColors.Brand
                        )
                    }
                    if (item.state in listOf("failed", "queued")) {
                        TextButton(onClick = {
                            scope.launch {
                                try {
                                    if (item.needsPhoto) {
                                        replacingId = item.id
                                        openPicker()
                                    } else {
                                        backend.retrySend(item.id)
                                        backend.processPendingSends()
                                    }
                                } catch (cancel: CancellationException) { throw cancel }
                                catch (failure: Exception) { error = failure.message }
                            }
                        }) { Text(if (item.needsPhoto) "Fotoğrafı yeniden seç" else "Tekrar dene") }
                    }
                }
            }
    }
    if (allDestinations) {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 540.dp), contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(visible, key = { it.id }) { sendCard(it) }
            if (visible.isEmpty()) item { Text("Bekleyen gönderimin yok.", Modifier.padding(vertical = 20.dp), color = MColors.Ink600) }
        }
    } else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { visible.forEach { sendCard(it) } }
    removalId?.let { id ->
        AlertDialog(
            onDismissRequest = { removalId = null },
            title = { Text("Gönderim kaldırılsın mı?") },
            text = { Text("Bekleyen mesaj veya fotoğraf cihazdaki gönderim listesinden kaldırılacak.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            backend.removePendingSend(id)
                            removalId = null
                        } catch (cancel: CancellationException) { throw cancel }
                        catch (failure: Exception) { error = failure.message }
                    }
                }) { Text("Kaldır", color = MColors.Rose) }
            },
            dismissButton = { TextButton(onClick = { removalId = null }) { Text("Vazgeç") } }
        )
    }
    error?.let { message ->
        AlertDialog(
            onDismissRequest = { error = null }, title = { Text("Gönderim") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { error = null }) { Text("Tamam") } }
        )
    }
}

/** This entry stays available even if the conversation is no longer in the inbox. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingOutboxButton(backend: BackendRepository) {
    val items by backend.pendingSends.collectAsState()
    val requireAccount = LocalRequireAccount.current
    var open by remember(backend.userId) { mutableStateOf(false) }
    if (items.isNotEmpty()) MCard(Modifier.fillMaxWidth()) {
        MPremiumMenuRow(
            text = "Bekleyen gönderimler · ${items.size}",
            subtitle = "Mesajlarını ve fotoğraflarını yönet",
            icon = Icons.Rounded.CloudUpload,
            onClick = { requireAccount { open = true } }
        )
    }
    if (open) ModalBottomSheet(
        onDismissRequest = { open = false }, containerColor = MColors.Canvas,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.statusBarsPadding().padding(top = 12.dp),
        dragHandle = { MPremiumSheetHandle() }
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MSheetHeading("Bekleyen gönderimler", "Tüm sohbetlerin ve Anı Vitrini", Icons.Rounded.CloudUpload, { open = false })
            PendingSendsPanel(backend, allDestinations = true)
        }
    }
}
