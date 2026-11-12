@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.matchora.app.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import com.matchora.app.data.encodeUploadPhoto
import com.matchora.app.data.decodeApiPhoto
import kotlinx.cinterop.*
import platform.CoreImage.*
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*
import platform.posix.memcpy

private fun ByteArray.qrData(): NSData = usePinned { NSData.create(bytes = it.addressOf(0), length = size.toULong()) }
actual fun makeQrCode(value: String): ImageBitmap? = runCatching {
    val filter = CIFilter.filterWithName("CIQRCodeGenerator") ?: return null
    filter.setValue(value.encodeToByteArray().qrData(), forKey = "inputMessage")
    filter.setValue("M", forKey = "inputCorrectionLevel")
    val output = filter.outputImage?.imageByApplyingTransform(CGAffineTransformMakeScale(8.0, 8.0)) ?: return null
    val cg = CIContext.contextWithOptions(null).createCGImage(output, fromRect = output.extent) ?: return null
    try {
        val image = UIImage.imageWithCGImage(cg)
        val data = UIImagePNGRepresentation(image) ?: return null
        val bytes = ByteArray(data.length.toInt()); bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }; decodeApiPhoto(bytes)
    } finally { CGImageRelease(cg) }
}.getOrNull()
actual fun readQrCode(image: ImageBitmap): String? = runCatching {
    val ci = CIImage.imageWithData(encodeUploadPhoto(image).qrData()) ?: return null
    val detector = CIDetector.detectorOfType(CIDetectorTypeQRCode, context = null, options = mapOf(CIDetectorAccuracy to CIDetectorAccuracyHigh)) ?: return null
    (detector.featuresInImage(ci).firstOrNull() as? CIQRCodeFeature)?.messageString
}.getOrNull()
