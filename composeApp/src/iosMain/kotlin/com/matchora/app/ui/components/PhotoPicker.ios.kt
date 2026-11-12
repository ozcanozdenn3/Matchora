@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.matchora.app.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.*
import org.jetbrains.skia.Image
import platform.AVFoundation.*
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.Photos.*
import platform.PhotosUI.*
import platform.UIKit.*
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

private fun NSData.photoBytes(): ByteArray {
    require(length in 1uL..(20uL * 1024uL * 1024uL))
    val result = ByteArray(length.toInt())
    result.usePinned { memcpy(it.addressOf(0), bytes, length) }
    return result
}

private fun preparePhoto(image: UIImage): ImageBitmap {
    val width = image.size.useContents { width }
    val height = image.size.useContents { height }
    require(width > 0 && height > 0)
    val scale = minOf(1.0, 2048.0 / maxOf(width, height))
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(width * scale, height * scale), false, 1.0)
    val resized = try {
        image.drawInRect(CGRectMake(0.0, 0.0, width * scale, height * scale))
        UIGraphicsGetImageFromCurrentImageContext()
    } finally { UIGraphicsEndImageContext() }
    val bytes = requireNotNull(UIImageJPEGRepresentation(resized ?: image, 0.85)).photoBytes()
    return Image.makeFromEncoded(bytes).toComposeImageBitmap()
}

private fun topController(): UIViewController? {
    val root = UIApplication.sharedApplication.keyWindow?.rootViewController
        ?: UIApplication.sharedApplication.windows.firstOrNull { (it as? UIWindow)?.isKeyWindow() == true }
            ?.let { (it as UIWindow).rootViewController }
    return generateSequence(root) { it.presentedViewController }.lastOrNull()
}

private class PhotoPickerLifetime {
    var active = true
    var controller: UIViewController? = null
    var loading: NSProgress? = null
    fun close() {
        active = false; loading?.cancel(); loading = null
        controller?.dismissViewControllerAnimated(false, completion = null); controller = null
    }
}

@Composable
internal actual fun rememberNativeGalleryPicker(onPhoto: (ImageBitmap) -> Unit, onError: (String) -> Unit): () -> Unit {
    val callback by rememberUpdatedState(onPhoto)
    val error by rememberUpdatedState(onError)
    val lifetime = remember { PhotoPickerLifetime() }
    DisposableEffect(lifetime) { onDispose { lifetime.close() } }
    val delegate = remember {
        object : NSObject(), PHPickerViewControllerDelegateProtocol {
            override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
                val result = didFinishPicking.firstOrNull() as? PHPickerResult
                picker.dismissViewControllerAnimated(true, completion = null)
                lifetime.controller = null
                if (result == null || !lifetime.active) return
                lifetime.loading = result.itemProvider.loadDataRepresentationForTypeIdentifier("public.image") { data, _ ->
                    dispatch_async(dispatch_get_main_queue()) {
                        if (lifetime.active) try {
                            val source = requireNotNull(data)
                            require(source.length <= 20uL * 1024uL * 1024uL)
                            callback(preparePhoto(requireNotNull(UIImage.imageWithData(source))))
                        } catch (_: Exception) { error("Seçilen fotoğraf okunamadı. Başka bir fotoğraf deneyebilirsin.") }
                    }
                }
            }
        }
    }
    val presentPicker = {
        val controller = topController()
        if (controller == null) error("Fotoğraf seçici açılamadı. Tekrar deneyebilirsin.")
        else if (lifetime.active && lifetime.controller == null) {
            val configuration = PHPickerConfiguration().apply { selectionLimit = 1; filter = PHPickerFilter.imagesFilter }
            val picker = PHPickerViewController(configuration).apply { this.delegate = delegate }
            lifetime.controller = picker
            controller.presentViewController(picker, animated = true, completion = null)
        }
    }
    return {
        when (PHPhotoLibrary.authorizationStatus()) {
            PHAuthorizationStatusAuthorized, PHAuthorizationStatusLimited -> presentPicker()
            PHAuthorizationStatusNotDetermined -> PHPhotoLibrary.requestAuthorization { status ->
                dispatch_async(dispatch_get_main_queue()) {
                    if (lifetime.active) {
                        if (status == PHAuthorizationStatusAuthorized || status == PHAuthorizationStatusLimited) presentPicker()
                        else error("Fotoğraf arşivine erişim izni verilmedi. İzni cihaz ayarlarından açabilirsin.")
                    }
                }
            }
            else -> error("Fotoğraf arşivine erişim kapalı. Fotoğraf seçmek için cihaz ayarlarından izin verebilirsin.")
        }
    }
}

@Composable
internal actual fun rememberNativeCameraPicker(onPhoto: (ImageBitmap) -> Unit, onError: (String) -> Unit): () -> Unit {
    val callback by rememberUpdatedState(onPhoto)
    val error by rememberUpdatedState(onError)
    val lifetime = remember { PhotoPickerLifetime() }
    DisposableEffect(lifetime) { onDispose { lifetime.close() } }
    val delegate = remember {
        object : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
            override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
                val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
                picker.dismissViewControllerAnimated(true) {
                    lifetime.controller = null
                    if (lifetime.active) try { callback(preparePhoto(requireNotNull(image))) }
                    catch (_: Exception) { error("Çekilen fotoğraf okunamadı.") }
                }
            }
            override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
                lifetime.controller = null
                picker.dismissViewControllerAnimated(true, completion = null)
            }
        }
    }
    val present = {
        val controller = topController()
        if (controller == null) error("Kamera açılamadı.")
        else if (lifetime.active && lifetime.controller == null) {
            val picker = UIImagePickerController().apply {
                sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
                allowsEditing = false
                this.delegate = delegate
            }
            lifetime.controller = picker
            controller.presentViewController(picker, animated = true, completion = null)
        }
    }
    return {
        if (!UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera))
            error("Bu cihazda kamera kullanılamıyor. Galeriden fotoğraf seçebilirsin.")
        else when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> present()
            AVAuthorizationStatusNotDetermined -> AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                dispatch_async(dispatch_get_main_queue()) {
                    if (lifetime.active) { if (granted) present() else error("Kamera izni verilmedi. İzni cihaz ayarlarından açabilirsin.") }
                }
            }
            else -> error("Kamera izni kapalı. Fotoğraf çekmek için cihaz ayarlarından izin verebilirsin.")
        }
    }
}
