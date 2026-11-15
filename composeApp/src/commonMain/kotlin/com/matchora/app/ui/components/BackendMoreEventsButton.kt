package com.matchora.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
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
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MPillActionButton(
            text = if (busy) "Maçlar yükleniyor…" else "Daha fazla maç göster",
            icon = Icons.Rounded.ExpandMore,
            tone = MPillTone.NEUTRAL,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                scope.launch {
                    busy = true; error = null
                    try { backend.loadMoreEvents() }
                    catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                    catch (failure: Exception) { error = failure.message ?: "Maçlar yüklenemedi." }
                    finally { busy = false }
                }
            }
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}
