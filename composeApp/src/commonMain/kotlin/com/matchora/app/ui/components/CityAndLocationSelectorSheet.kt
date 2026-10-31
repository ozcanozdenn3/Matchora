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
    ModalBottomSheet(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        scrimColor = androidx.compose.ui.graphics.Color(0xFF0B1220).copy(alpha = .48f),
onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MColors.Canvas, dragHandle = { MPremiumSheetHandle() }) {
        Column(Modifier.fillMaxWidth().imePadding().padding(20.dp).padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            MSheetHeading("Şehir ve bölge", "Türkiye’nin 81 ilinde spor", Icons.Rounded.LocationCity, onDismiss)
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    colors = com.matchora.app.ui.components.premiumTextFieldColors(),
                    textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
value = if (expanded) query else city, onValueChange = { query = it },
                    readOnly = !expanded, label = { Text("İl seç") }, singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false; query = "" }, modifier = Modifier.heightIn(max = 320.dp)) {
                    com.matchora.app.state.turkeyCities.filter { it.lowercase().replace('ı','i').contains(query.lowercase().replace('ı','i')) }.forEach { name ->
                        DropdownMenuItem(text = { Text(name) }, onClick = { city = name; district = "Merkez"; expanded = false; query = "" },
                            trailingIcon = { if (city == name) Icon(Icons.Rounded.Check, null, tint = MColors.BrandDeep) })
                    }
                }
            }
            OutlinedTextField(
                    colors = com.matchora.app.ui.components.premiumTextFieldColors(),
                    textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    value = district, onValueChange = { district = it }, label = { Text("İlçe / bölge") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
            MButton("$city için uygula", { onSelectLocation(city, district.trim().ifBlank { "Merkez" }, false); onDismiss() }, icon = Icons.Rounded.Check)
        }
    }
}
