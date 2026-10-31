package com.matchora.app.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.UIKit.*
import platform.darwin.NSObject
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size <= 0) return ByteArray(0)
    val result = ByteArray(size)
    result.usePinned { pinned ->
        memcpy(pinned.addressOf(0), bytes, length)
    }
    return result
}

@Composable
actual fun rememberGalleryPhotoPicker(onPhotoPicked: (ImageBitmap) -> Unit): () -> Unit {
    val currentCallback by rememberUpdatedState(onPhotoPicked)
    val delegate = remember {
        object : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
            override fun imagePickerController(
                picker: UIImagePickerController,
                didFinishPickingMediaWithInfo: Map<Any?, *>
            ) {
                val image = (didFinishPickingMediaWithInfo[UIImagePickerControllerEditedImage]
                    ?: didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage]) as? UIImage
                val data = image?.let { UIImageJPEGRepresentation(it, 0.85) ?: UIImagePNGRepresentation(it) }
                val bytes = data?.toByteArray()
                picker.dismissViewControllerAnimated(true) {
                    if (bytes != null && bytes.isNotEmpty()) {
                        runCatching {
                            Image.makeFromEncoded(bytes).toComposeImageBitmap()
                        }.getOrNull()?.let { bitmap ->
                            currentCallback(bitmap)
                        }
                    }
                }
            }

            override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
                picker.dismissViewControllerAnimated(true, completion = null)
            }
        }
    }

    return remember(delegate) {
        {
            val rootController = UIApplication.sharedApplication.keyWindow?.rootViewController
                ?: UIApplication.sharedApplication.windows.firstOrNull { (it as? UIWindow)?.isKeyWindow() == true }
                    ?.let { (it as UIWindow).rootViewController }
            val topController = generateSequence(rootController) { it.presentedViewController }.lastOrNull()
            if (topController != null && UIImagePickerController.isSourceTypeAvailable(
                    UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
                )
            ) {
                val picker = UIImagePickerController().apply {
                    sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
                    allowsEditing = true
                    this.delegate = delegate
                }
                topController.presentViewController(picker, animated = true, completion = null)
            }
        }
    }
}

