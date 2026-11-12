package com.matchora.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.matchora.app.data.*
import kotlinx.coroutines.launch

@Composable
fun BackendMoreEventsButton() {
    val backend = LocalBackendRepository.current ?: return
    if (!backend.enabled) return
    val snapshot by backend.snapshots.collectAsState()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    if (snapshot?.s("nextEventCursor").isNullOrBlank()) return
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        OutlinedButton(enabled = !busy, onClick = {
            scope.launch {
                busy = true; error = null
                try { backend.loadMoreEvents() }
                catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                catch (failure: Exception) { error = failure.message ?: "Maçlar yüklenemedi." }
                finally { busy = false }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Maçlar yükleniyor…" else "Daha fazla maç göster") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}
