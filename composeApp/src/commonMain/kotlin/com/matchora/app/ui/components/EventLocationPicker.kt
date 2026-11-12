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
fun EventLocationPicker(initialPoint: EventGeoPoint?, initialAddress: String, city: String,
    onDismiss: () -> Unit, onConfirm: (EventGeoPoint, String) -> Unit, district: String = "") {
    val backendMode = LocalBackendRepository.current?.enabled == true
    val suppliedPoint = initialPoint?.takeIf { validMapCoordinate(it.latitude, it.longitude) }
    val startingPoint = suppliedPoint ?: EventGeoPoint(39.0, 35.0)
    var point by remember(city, district, suppliedPoint) { mutableStateOf(startingPoint) }
    var pointChosen by remember(city, district, suppliedPoint) { mutableStateOf(suppliedPoint != null) }
    var resolving by remember(city, district, suppliedPoint) { mutableStateOf(backendMode && suppliedPoint == null) }
    var address by remember { mutableStateOf(initialAddress) }
    var mapCenter by remember(city, district, suppliedPoint) { mutableStateOf(startingPoint) }
    val region = rememberRegionCenter(if (backendMode && suppliedPoint == null) city else "", district)
    LaunchedEffect(region) {
        if (!pointChosen) region?.let {
            mapCenter = EventGeoPoint(it.latitude, it.longitude)
            point = mapCenter; pointChosen = true; resolving = false
        }
    }
    LaunchedEffect(city, district, suppliedPoint) {
        if (backendMode && suppliedPoint == null) { kotlinx.coroutines.delay(12500); resolving = false }
    }
    var locating by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    val locate = rememberLocationRequester { result ->
        locating = false
        when (result) {
            is LocationOutcome.Located -> {
                DeviceLocationState.current.value = result.value
                mapCenter = EventGeoPoint(result.value.latitude, result.value.longitude); point = mapCenter; pointChosen = true; resolving = false
            }
            is LocationOutcome.Failed -> locationError = result.message
        }
    }
    ModalBottomSheet(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        scrimColor = androidx.compose.ui.graphics.Color(0xFF0B1220).copy(alpha = .48f),
onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { MPremiumSheetHandle() }, containerColor = MColors.Canvas) {
        Column(Modifier.fillMaxWidth().imePadding().padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MSheetHeading("Buluşma konumu", "$city · haritayı işaretin altına kaydır", Icons.Rounded.LocationOn, onDismiss)
            TextButton(onClick = { locating = true; locationError = null; locate() }, enabled = !locating) { Text(if (locating) "Konum bulunuyor…" else "Güncel konumuma git") }
            locationError?.let { Text(it, color = MColors.Rose, style = MaterialTheme.typography.bodySmall); TextButton(onClick = ::openApplicationSettings) { Text("Konum ayarlarını aç") } }
            Box(Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(min = 200.dp, max = 360.dp).height(320.dp).clip(RoundedCornerShape(22.dp))) {
                key(mapCenter) { LocationSelectionMap(Modifier.fillMaxSize(), mapCenter) {
                    if (validMapCoordinate(it.latitude, it.longitude)) {
                        point = it
                        if (kotlin.math.abs(it.latitude - mapCenter.latitude) > 0.000001 || kotlin.math.abs(it.longitude - mapCenter.longitude) > 0.000001) pointChosen = true
                    }
                } }
                Icon(Icons.Rounded.LocationOn, "Seçilen buluşma noktası", tint = MColors.BrandDeep,
                    modifier = Modifier.align(Alignment.Center).offset(y = (-18).dp).size(38.dp))
            }
            OutlinedTextField(
                    colors = com.matchora.app.ui.components.premiumTextFieldColors(),
                    textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    value = address, onValueChange = { address = it }, label = { Text("Buluşma noktası / açık adres") },
                placeholder = { Text("Örn. parkın ana girişi") }, modifier = Modifier.fillMaxWidth(), maxLines = 2, shape = RoundedCornerShape(14.dp))
            Text(when {
                pointChosen -> "İşaretli nokta ilanın haritasında görünecek."
                resolving -> "Bölgen çözümleniyor. Haritayı kaydırarak veya güncel konumunu kullanarak nokta seçebilirsin."
                else -> "Bölgen çözümlenemedi. Haritayı kaydırarak veya güncel konumunu kullanarak nokta seç."
            }, style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
            MButton("Bu konumu kullan", { onConfirm(point, address.trim()); onDismiss() }, icon = Icons.Rounded.Check,
                enabled = pointChosen && address.isNotBlank() && validMapCoordinate(point.latitude, point.longitude))
        }
    }
}
