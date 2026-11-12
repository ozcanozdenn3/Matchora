package com.matchora.app.ui.components

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer

actual fun makeQrCode(value: String): ImageBitmap? = runCatching {
    val matrix = MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, 640, 640, mapOf(EncodeHintType.MARGIN to 4))
    val pixels = IntArray(matrix.width * matrix.height) { i -> if (matrix[i % matrix.width, i / matrix.width]) android.graphics.Color.BLACK else android.graphics.Color.WHITE }
    Bitmap.createBitmap(pixels, matrix.width, matrix.height, Bitmap.Config.ARGB_8888).asImageBitmap()
}.getOrNull()
actual fun readQrCode(image: ImageBitmap): String? = runCatching {
    val bitmap = image.asAndroidBitmap(); val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(bitmap.width, bitmap.height, pixels))),
        mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE), DecodeHintType.TRY_HARDER to true)).text
}.getOrNull()
