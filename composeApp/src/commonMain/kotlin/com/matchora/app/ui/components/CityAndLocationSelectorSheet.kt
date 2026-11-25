package com.matchora.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.ui.theme.MColors

data class DistrictItem(
    val city: String,
    val district: String,
    val venuesCount: Int,
    val matchesCount: Int,
    val tag: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityAndLocationSelectorSheet(
    currentCity: String,
    currentDistrict: String,
    isGpsActive: Boolean,
    onDismiss: () -> Unit,
    onSelectLocation: (city: String, district: String, isGps: Boolean) -> Unit
) {
    var city by remember { mutableStateOf(currentCity) }
    var district by remember { mutableStateOf(currentDistrict) }
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var locating by remember { mutableStateOf(false) }
    var gpsSelected by remember { mutableStateOf(isGpsActive) }
    var locationError by remember { mutableStateOf<LocationOutcome.Failed?>(null) }
    val requestLocation = rememberLocationRequester { result ->
        locating = false
        when (result) {
            is LocationOutcome.Located -> {
                DeviceLocationState.current.value = result.value
                val foundCity = result.value.city
                if (foundCity.isNullOrBlank()) locationError = LocationOutcome.Failed("Konum alındı ancak il ve ilçe çözümlenemedi. Bölgeni elle seçebilirsin.")
                else {
                    val foundDistrict = result.value.district?.trim()?.takeIf { it.isNotBlank() } ?: "Merkez"
                    city = foundCity
                    district = foundDistrict
                    gpsSelected = true
                    onSelectLocation(foundCity, foundDistrict, true)
                    onDismiss()
                }
            }
            is LocationOutcome.Failed -> {
                if (!hasForegroundLocationPermission()) DeviceLocationState.current.value = null
                locationError = result
            }
        }
    }
    ModalBottomSheet(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        scrimColor = androidx.compose.ui.graphics.Color(0xFF0B1220).copy(alpha = .48f),
onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MColors.Canvas, dragHandle = { MPremiumSheetHandle() }) {
        Column(Modifier.fillMaxWidth().imePadding().padding(20.dp).padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            MSheetHeading("Şehir ve bölge", "Türkiye’nin 81 ilinde spor", Icons.Rounded.LocationCity, onDismiss)
            Surface(
                onClick = { if (!locating) { locating = true; requestLocation() } },
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF203B35),
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, MColors.Mint.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(Color(0xFF22423B), Color(0xFF162B26))))
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(11.dp),
                        color = MColors.Mint.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, MColors.Mint.copy(alpha = 0.4f)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.MyLocation, null, tint = Color(0xFF9EDDC8), modifier = Modifier.size(19.dp))
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (locating) "Konumun bulunuyor…" else "Mevcut Konumu Kullan",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isGpsActive) "Canlı GPS aktif · yenilemek için dokun" else "Bulunduğun ile ve ilçeye tek tıkla geç",
                            fontSize = 11.sp,
                            color = Color(0xFF9EDDC8)
                        )
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = Color(0xFF9EDDC8), modifier = Modifier.size(20.dp))
                }
            }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    colors = com.matchora.app.ui.components.premiumTextFieldColors(),
                    textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
value = if (expanded) query else city, onValueChange = { query = it },
                    readOnly = !expanded, label = { Text("İl seç") }, singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false; query = "" }, modifier = Modifier.heightIn(max = 320.dp)) {
                    com.matchora.app.state.turkeyCities.filter { com.matchora.app.state.canonicalTurkishKey(it).contains(com.matchora.app.state.canonicalTurkishKey(query)) }.forEach { name ->
                        DropdownMenuItem(text = { Text(name) }, onClick = { city = name; district = "Merkez"; gpsSelected = false; expanded = false; query = "" },
                            trailingIcon = { if (city == name) Icon(Icons.Rounded.Check, null, tint = MColors.BrandDeep) })
                    }
                }
            }
            OutlinedTextField(
                    colors = com.matchora.app.ui.components.premiumTextFieldColors(),
                    textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    value = district, onValueChange = { district = it; gpsSelected = false }, label = { Text("İlçe / bölge") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
            MButton("$city için uygula", { onSelectLocation(city, district.trim().ifBlank { "Merkez" }, gpsSelected); onDismiss() }, icon = Icons.Rounded.Check)
        }
    }
    locationError?.let { failure -> AlertDialog(onDismissRequest = { locationError = null },
        title = { Text("Konum erişimi") }, text = { Text(failure.message) },
        confirmButton = { TextButton(onClick = { locationError = null }) { Text("Tamam") } },
        dismissButton = { if (failure.settingsAvailable) TextButton(onClick = { locationError = null; openApplicationSettings() }) { Text("Ayarları aç") } }) }
}
