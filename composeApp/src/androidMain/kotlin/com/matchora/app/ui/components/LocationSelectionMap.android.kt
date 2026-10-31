package com.matchora.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.matchora.app.state.EventGeoPoint
import kotlin.math.roundToInt

@Composable
actual fun LocationSelectionMap(modifier: Modifier, initialPoint: EventGeoPoint, onCenterChanged: (EventGeoPoint) -> Unit) {
    var point by remember { mutableStateOf(initialPoint) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val callback by rememberUpdatedState(onCenterChanged)
    Box(modifier.background(Color(0xFFEAF0EB)).drawBehind {
        val step = 70.dp.toPx()
        for (i in -1..(size.width / step).toInt() + 1) {
            val x = i * step + pan.x % step
            drawLine(Color.White, Offset(x, 0f), Offset(x, size.height), 10.dp.toPx())
        }
        for (i in -1..(size.height / step).toInt() + 1) {
            val y = i * step + pan.y % step
            drawLine(Color.White, Offset(0f, y), Offset(size.width, y), 10.dp.toPx())
        }
    }.pointerInput(Unit) {
        detectDragGestures { change, drag ->
            change.consume(); pan += drag
            point = EventGeoPoint((point.latitude + drag.y * .000025).coerceIn(-85.0, 85.0), (point.longitude - drag.x * .000025).coerceIn(-180.0, 180.0))
            callback(point)
        }
    }) {
        Text("Şematik harita · kaydırarak seç\n${(point.latitude * 10000).roundToInt() / 10000.0}, ${(point.longitude * 10000).roundToInt() / 10000.0}", Modifier.background(Color.White).padding(10.dp))
    }
}
