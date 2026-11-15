package com.matchora.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.matchora.app.data.*

internal fun venueChoiceValue(name: String, area: String, address: String): String {
    val suffix = " · $area"
    val room = (160 - suffix.length).coerceAtLeast(0)
    val cleanAddress = address.trim()
    val location = if (cleanAddress.isBlank()) name.take(room) else {
        val nameLimit = (room - minOf(cleanAddress.length, 50) - 2).coerceAtLeast(1)
        "${name.take(nameLimit)}, $cleanAddress".take(room)
    }
    return location + suffix
}

@Composable
fun VenueChoiceField(label: String, value: String, onChange: (String) -> Unit, previewOptions: List<String>, city: String = "") {
    val backend = LocalBackendRepository.current
    val wire = if (backend != null) backend.snapshots.collectAsState().value else null
    val manual = "Başka saha / yer gir" to ""
    val places = if (backend == null) previewOptions.map { it to it } else wire?.a("venues").orEmpty()
        .filter { city.isBlank() || it.s("city").isBlank() || com.matchora.app.state.matchesTurkishPlace(it.s("city"), city) }
        .map { "${it.s("name")} · ${it.s("area")}" to venueChoiceValue(it.s("name"), it.s("area"), it.s("address")) }.distinct()
    val selected = places.firstOrNull { value == it.second || value == it.first }
        ?: if (backend == null) value to value else manual
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MFloatingDropdownBox(label, selected, if (backend == null) places else places + manual,
            { it.first }, { onChange(it.second) })
        if (backend != null && selected == manual) {
            MPremiumTextField(
                value = value,
                onValueChange = { onChange(it.take(160)) },
                label = "Saha / buluşma yeri",
                placeholder = "Saha adı, tesis veya açık konum"
            )
        }
    }
}
