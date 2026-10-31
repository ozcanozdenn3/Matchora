package com.matchora.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.matchora.app.state.EventGeoPoint
import com.matchora.app.ui.theme.MColors

@Composable
expect fun LocationSelectionMap(modifier: Modifier, initialPoint: EventGeoPoint, onCenterChanged: (EventGeoPoint) -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventLocationPicker(initialPoint: EventGeoPoint, initialAddress: String, city: String,
    onDismiss: () -> Unit, onConfirm: (EventGeoPoint, String) -> Unit) {
    var point by remember { mutableStateOf(initialPoint) }
    var address by remember { mutableStateOf(initialAddress) }
    ModalBottomSheet(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        scrimColor = androidx.compose.ui.graphics.Color(0xFF0B1220).copy(alpha = .48f),
onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { MPremiumSheetHandle() }, containerColor = MColors.Canvas) {
        Column(Modifier.fillMaxWidth().imePadding().padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MSheetHeading("Buluşma konumu", "$city · haritayı işaretin altına kaydır", Icons.Rounded.LocationOn, onDismiss)
            Box(Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(min = 200.dp, max = 360.dp).height(320.dp).clip(RoundedCornerShape(22.dp))) {
                LocationSelectionMap(Modifier.fillMaxSize(), initialPoint, { point = it })
                Icon(Icons.Rounded.LocationOn, "Seçilen buluşma noktası", tint = MColors.BrandDeep,
                    modifier = Modifier.align(Alignment.Center).offset(y = (-18).dp).size(38.dp))
            }
            OutlinedTextField(
                    colors = com.matchora.app.ui.components.premiumTextFieldColors(),
                    textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    value = address, onValueChange = { address = it }, label = { Text("Buluşma noktası / açık adres") },
                placeholder = { Text("Örn. parkın ana girişi") }, modifier = Modifier.fillMaxWidth(), maxLines = 2, shape = RoundedCornerShape(14.dp))
            Text("İşaretli nokta ilanın haritasında görünecek.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
            MButton("Bu konumu kullan", { onConfirm(point, address.trim()); onDismiss() }, icon = Icons.Rounded.Check, enabled = address.isNotBlank())
        }
    }
}
