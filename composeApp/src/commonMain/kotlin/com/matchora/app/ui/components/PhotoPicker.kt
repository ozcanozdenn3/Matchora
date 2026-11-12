package com.matchora.app.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap

@Composable
internal expect fun rememberNativeGalleryPicker(onPhoto: (ImageBitmap) -> Unit, onError: (String) -> Unit): () -> Unit

@Composable
internal expect fun rememberNativeCameraPicker(onPhoto: (ImageBitmap) -> Unit, onError: (String) -> Unit): () -> Unit

/** Shared entry point for passport, chat, verification and memories. */
@Composable
fun rememberGalleryPhotoPicker(onPhotoPicked: (ImageBitmap) -> Unit): () -> Unit {
    var choose by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val gallery = rememberNativeGalleryPicker(onPhotoPicked) { error = it }
    val camera = rememberNativeCameraPicker(onPhotoPicked) { error = it }
    if (choose) AlertDialog(
        onDismissRequest = { choose = false }, title = { Text("Fotoğraf ekle") },
        text = { Text("Kamera ile çek veya galeriden paylaşmak istediğin fotoğrafı seç.") },
        confirmButton = { TextButton(onClick = { choose = false; camera() }) { Text("Kamera") } },
        dismissButton = { TextButton(onClick = { choose = false; gallery() }) { Text("Galeri") } }
    )
    error?.let { message -> AlertDialog(
        onDismissRequest = { error = null }, title = { Text("Fotoğraf erişimi") }, text = { Text(message) },
        confirmButton = { TextButton(onClick = { error = null; openApplicationSettings() }) { Text("Ayarları aç") } },
        dismissButton = { TextButton(onClick = { error = null }) { Text("Kapat") } }
    ) }
    return { choose = true }
}
