package com.matchora.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp

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
        onDismissRequest = { choose = false },
        title = { Text("Fotoğraf ekle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Paylaşmak veya doğrulamak istediğin görselin kaynağını seç.", style = MaterialTheme.typography.bodySmall)
                MPremiumMenuRow(
                    text = "Galeriden fotoğraf seç",
                    subtitle = "Albümünden mevcut bir görsel yükle",
                    icon = Icons.Rounded.PhotoLibrary,
                    onClick = { choose = false; gallery() }
                )
                MPremiumMenuRow(
                    text = "Kamerayla fotoğraf çek",
                    subtitle = "Anlık fotoğraf çek ve kullan",
                    icon = Icons.Rounded.PhotoCamera,
                    onClick = { choose = false; camera() }
                )
            }
        },
        confirmButton = { TextButton(onClick = { choose = false }) { Text("Vazgeç") } }
    )
    error?.let { message -> AlertDialog(
        onDismissRequest = { error = null }, title = { Text("Fotoğraf erişimi") }, text = { Text(message) },
        confirmButton = { TextButton(onClick = { error = null; openApplicationSettings() }) { Text("Ayarları aç") } },
        dismissButton = { TextButton(onClick = { error = null }) { Text("Kapat") } }
    ) }
    return { choose = true }
}
