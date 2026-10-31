package com.matchora.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

@Composable
expect fun rememberGalleryPhotoPicker(onPhotoPicked: (ImageBitmap) -> Unit): () -> Unit

