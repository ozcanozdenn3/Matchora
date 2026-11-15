package com.matchora.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    if (cursor.isNotBlank()) Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MPillActionButton(
            text = if (busy) "Yükleniyor…" else when (kind) {
                "teams" -> "Diğer takımları yükle"
                "tournaments" -> "Diğer turnuvaları yükle"
                "communities" -> "Diğer toplulukları yükle"
                else -> "Diğer tesisleri yükle"
            },
            icon = Icons.Rounded.ExpandMore,
            tone = MPillTone.NEUTRAL,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            onClick = { scope.launch {
                busy = true; error = null
                try { backend.loadMoreCatalog(kind) }
                catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                catch (failure: Exception) { error = failure.message }
                finally { busy = false }
            } }
        )
        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MColors.Rose) }
    }
}
