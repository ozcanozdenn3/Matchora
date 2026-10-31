package com.matchora.app.ui.components

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.*
import androidx.compose.foundation.shape.RoundedCornerShape

object DockCutoutShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val base = RoundedCornerShape(26.dp).createOutline(size, layoutDirection, density)
        val path = Path().apply { addRoundRect((base as Outline.Rounded).roundRect) }
        val radius = with(density) { 39.dp.toPx() }
        val centerY = with(density) { 16.dp.toPx() }
        val hole = Path().apply { addOval(Rect(size.width / 2 - radius, centerY - radius, size.width / 2 + radius, centerY + radius)) }
        return Outline.Generic(Path.combine(PathOperation.Difference, path, hole))
    }
}
