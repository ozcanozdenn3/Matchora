package com.matchora.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.data.PhoneCountryCode
import com.matchora.app.data.extractCountryAndLocalInput
import com.matchora.app.data.normalizePhoneWithCountry
import com.matchora.app.data.phoneCountryCodes
import com.matchora.app.state.canonicalTurkishKey
import com.matchora.app.ui.theme.MColors

/** Shared phone number field with selectable country code and automatic trunk-zero normalization. */
@Composable
fun PhoneNumberFieldWithCountryPicker(
    selectedCountry: PhoneCountryCode,
    onSelectCountry: (PhoneCountryCode) -> Unit,
    localPhone: String,
    onLocalPhoneChange: (String) -> Unit,
    label: String = "Telefon numaran",
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var showCountryPicker by remember { mutableStateOf(false) }
    val normalized = normalizePhoneWithCountry(selectedCountry, localPhone)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MColors.Ink600)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = { if (enabled) showCountryPicker = true },
                enabled = enabled,
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFDCE4F0)),
                modifier = Modifier.height(56.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(selectedCountry.flag, fontSize = 18.sp)
                    Text(
                        selectedCountry.dialCode,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MColors.Ink900
                    )
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Ülke kodu seç",
                        tint = MColors.Ink500,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            OutlinedTextField(
                value = localPhone,
                onValueChange = { raw ->
                    val (nextCountry, nextLocal) = extractCountryAndLocalInput(selectedCountry, raw)
                    if (nextCountry != selectedCountry) onSelectCountry(nextCountry)
                    onLocalPhoneChange(nextLocal)
                },
                placeholder = { Text(selectedCountry.sampleLocal, color = MColors.Ink400) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                enabled = enabled,
                colors = premiumTextFieldColors(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            )
        }

        val helperText = when {
            normalized != null -> "Kod gönderilecek numara: $normalized"
            localPhone.isNotBlank() && selectedCountry.dialCode == "+90" ->
                "Ülke kodu olmadan ${selectedCountry.sampleLocal} formatında gir (başta 0 olabilir)."
            localPhone.isNotBlank() ->
                "Ülke kodu olmadan geçerli yerel telefon numaranı gir."
            else ->
                "Ülke kodu yazmadan numaranı gir (${selectedCountry.flag} ${selectedCountry.dialCode} otomatik eklenir)."
        }
        Text(
            helperText,
            style = MaterialTheme.typography.labelSmall,
            color = if (normalized != null) MColors.BrandDeep else MColors.Ink500
        )
    }

    if (showCountryPicker) {
        CountryCodePickerSheet(
            selectedCountry = selectedCountry,
            onSelect = { country ->
                onSelectCountry(country)
                showCountryPicker = false
            },
            onDismiss = { showCountryPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountryCodePickerSheet(
    selectedCountry: PhoneCountryCode,
    onSelect: (PhoneCountryCode) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val normalizedQuery = canonicalTurkishKey(query.trim())
    val filteredCountries = remember(query) {
        if (normalizedQuery.isBlank() && !query.trim().startsWith("+")) phoneCountryCodes
        else {
            val rawLower = query.trim().lowercase()
            phoneCountryCodes.filter { country ->
                canonicalTurkishKey(country.name).contains(normalizedQuery) ||
                    country.iso.lowercase().contains(rawLower) ||
                    country.dialCode.contains(rawLower.removePrefix("00"))
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        scrimColor = Color(0xFF0B1220).copy(alpha = .48f),
        containerColor = MColors.Canvas,
        dragHandle = { MPremiumSheetHandle() },
        modifier = Modifier.statusBarsPadding().padding(top = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MSheetHeading("Ülke kodu seç", "Ülke kodu eklemeden numaranı yazabilirsin", Icons.Rounded.Public, onDismiss)

            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(40) },
                placeholder = { Text("Ülke adı veya kod ara (örn. Türkiye, +49)", color = MColors.Ink400) },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = MColors.Ink500) },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Close, "Temizle", tint = MColors.Ink500)
                        }
                    }
                } else null,
                singleLine = true,
                colors = premiumTextFieldColors(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (filteredCountries.isEmpty()) {
                Text(
                    "Aramayla eşleşen ülke bulunamadı.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MColors.Ink500,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredCountries, key = { "${it.iso}_${it.dialCode}" }) { country ->
                        val isSelected = country.iso == selectedCountry.iso
                        Surface(
                            onClick = { onSelect(country) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MColors.BrandTint else Color.White,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MColors.BrandDeep else Color(0xFFDCE4F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(country.flag, fontSize = 20.sp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        country.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = MColors.Ink900,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        country.iso,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MColors.Ink500
                                    )
                                }
                                Text(
                                    country.dialCode,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) MColors.BrandDeep else MColors.Ink700
                                )
                                if (isSelected) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = "Seçili",
                                        tint = MColors.BrandDeep,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

