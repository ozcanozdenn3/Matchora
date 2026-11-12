package com.matchora.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import com.matchora.app.data.*
import com.matchora.app.ui.theme.MColors
import kotlinx.coroutines.launch

@Composable
fun BackendMoreCatalogButton(kind: String) {
    val backend = LocalBackendRepository.current ?: return
    val snapshot by backend.snapshots.collectAsState()
    val cursor = snapshot?.o("nextCatalogCursors")?.s(kind).orEmpty()
    var busy by remember(kind) { mutableStateOf(false) }
    var error by remember(kind) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    if (cursor.isNotBlank()) Column {
        TextButton(enabled = !busy, onClick = { scope.launch {
            busy = true; error = null
            try { backend.loadMoreCatalog(kind) }
            catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (failure: Exception) { error = failure.message }
            finally { busy = false }
        } }) { Text(if (busy) "Yükleniyor…" else when (kind) { "teams" -> "Diğer takımları yükle"; "tournaments" -> "Diğer turnuvaları yükle"; "communities" -> "Diğer toplulukları yükle"; else -> "Diğer tesisleri yükle" }) }
        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MColors.Rose) }
    }
}
