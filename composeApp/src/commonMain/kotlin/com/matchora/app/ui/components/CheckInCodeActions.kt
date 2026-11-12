package com.matchora.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

expect fun makeQrCode(value: String): ImageBitmap?
expect fun readQrCode(image: ImageBitmap): String?

@Composable
fun CheckInCodeActions(hostCode: String, onCode: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val picker = rememberGalleryPhotoPicker { image ->
        if (image != null) scope.launch {
            busy = true
            try {
                val code = withContext(Dispatchers.Default) { readQrCode(image) }
                if (code.isNullOrBlank() || code.length > 2048) message = "Geçerli bir check-in QR kodu bulunamadı. Kodu yakından, düz açıyla çekebilir veya kodu elle girebilirsin."
                else onCode(code)
            } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (_: Exception) { message = "QR kodu okunamadı. Tekrar dene veya kodu elle gir." }
            finally { busy = false }
        }
    }
    TextButton(onClick = picker, enabled = !busy) { Text(if (busy) "QR okunuyor…" else "QR fotoğrafını çek / seç") }
    val qr = remember(hostCode) { if (hostCode.isBlank()) null else makeQrCode(hostCode) }
    qr?.let { Image(it, "Organizatör check-in QR kodu", Modifier.fillMaxWidth().height(200.dp)) }
    message?.let { text -> AlertDialog(onDismissRequest = { message = null }, title = { Text("Check-in QR") }, text = { Text(text) }, confirmButton = { TextButton(onClick = { message = null }) { Text("Tamam") } }) }
}
