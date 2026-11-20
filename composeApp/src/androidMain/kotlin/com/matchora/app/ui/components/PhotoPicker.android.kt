package com.matchora.app.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private suspend fun readPhoto(context: android.content.Context, uri: Uri): ImageBitmap = withContext(Dispatchers.IO) {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    require(bounds.outWidth > 0 && bounds.outHeight > 0)
    var sample = 1
    while (bounds.outWidth / sample > 2048 || bounds.outHeight / sample > 2048) sample *= 2
    val bitmap = context.contentResolver.openInputStream(uri)?.use {
        requireNotNull(BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }))
    } ?: error("Fotoğraf okunamadı")
    val orientation = runCatching { context.contentResolver.openInputStream(uri)?.use { android.media.ExifInterface(it).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1) } }.getOrNull() ?: 1
    val matrix = android.graphics.Matrix().apply { when (orientation) {
        2 -> setScale(-1f, 1f); 3 -> setRotate(180f); 4 -> setScale(1f, -1f)
        5 -> { setRotate(90f); postScale(-1f, 1f) }; 6 -> setRotate(90f)
        7 -> { setRotate(-90f); postScale(-1f, 1f) }; 8 -> setRotate(-90f)
    } }
    if (orientation in 2..8) android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).asImageBitmap() else bitmap.asImageBitmap()
}

@Composable
internal actual fun rememberNativeGalleryPicker(onPhoto: (ImageBitmap) -> Unit, onError: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val callback by rememberUpdatedState(onPhoto)
    val error by rememberUpdatedState(onError)
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            try { callback(readPhoto(context, uri)) }
            catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (_: Exception) { error("Seçilen fotoğraf okunamadı. Başka bir fotoğraf deneyebilirsin.") }
        }
    }
    val fallbackPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            try { callback(readPhoto(context, uri)) }
            catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (_: Exception) { error("Seçilen fotoğraf okunamadı. Başka bir fotoğraf deneyebilirsin.") }
        }
    }
    val launchPicker = {
        runCatching { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            .onFailure {
                runCatching { fallbackPicker.launch("image/*") }
                    .onFailure { error("Bu cihazda fotoğraf seçici açılamadı.") }
            }
        Unit
    }
    // PickVisualMedia falls back to the system document picker on older devices.
    // Both grant access to the selected URI without broad library permissions.
    return launchPicker
}

@Composable
internal actual fun rememberNativeCameraPicker(onPhoto: (ImageBitmap) -> Unit, onError: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val callback by rememberUpdatedState(onPhoto)
    val error by rememberUpdatedState(onError)
    val scope = rememberCoroutineScope()
    // Activity Result can be delivered after Activity recreation (for example, the camera app).
    var pendingPath by rememberSaveable { mutableStateOf<String?>(null) }
    val capture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val file = pendingPath?.let(::File)
        pendingPath = null
        if (file != null) scope.launch {
            try { if (saved) callback(readPhoto(context, Uri.fromFile(file))) }
            catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (_: Exception) { error("Çekilen fotoğraf okunamadı.") }
            finally { file.delete() }
        }
    }
    val take = {
        runCatching {
            val directory = File(context.cacheDir, "camera").apply { mkdirs() }
            directory.listFiles()?.filter { it.lastModified() < System.currentTimeMillis() - 86400000 }?.forEach { it.delete() }
            val file = File.createTempFile("capture-", ".jpg", directory)
            pendingPath = file.absolutePath
            capture.launch(FileProvider.getUriForFile(context, "${context.packageName}.photos", file))
        }.onFailure { pendingPath?.let { File(it).delete() }; pendingPath = null; error("Kamera açılamadı. Bu cihazda kullanılabilir bir kamera uygulaması olmayabilir.") }
        Unit
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) take() else error("Fotoğraf çekmek için kamera izni gerekiyor. İzni cihaz ayarlarından açabilirsin.")
    }
    return { if (context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) take()
        else permission.launch(Manifest.permission.CAMERA) }
}
