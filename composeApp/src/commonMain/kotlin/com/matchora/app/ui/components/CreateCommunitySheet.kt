package com.matchora.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.matchora.app.domain.Sport
import com.matchora.app.state.*
import com.matchora.app.ui.theme.MColors
import kotlinx.datetime.Clock

@Composable
fun CreateCommunitySheet(city: String, district: String, onDismiss: () -> Unit, onCreate: (CommunityInfo) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(CommunityType.CLUB) }
    var access by remember { mutableStateOf(CommunityAccess.OPEN) }
    var sport by remember { mutableStateOf(Sport.FOOTBALL) }
    var sports by remember { mutableStateOf(listOf(Sport.FOOTBALL)) }
    var selectedCity by remember { mutableStateOf(city) }
    var selectedDistrict by remember { mutableStateOf(district) }
    var location by remember { mutableStateOf(false) }
    ActivitySheet("Topluluğunu oluştur", "Spor çevreni bir araya getir", onDismiss, Icons.Rounded.Hub) {
        MFloatingInputBox(
            label = "Topluluk Adı",
            value = name,
            onValueChange = { name = it.take(80) },
            placeholder = "Örn: Kadıköy Sabah Koşu & Tenis Kulübü",
            icon = Icons.Rounded.Hub,
            badgeText = "Zorunlu"
        )
        MFloatingDropdownBox("Topluluğun yapısı", type, listOf(CommunityType.CLUB, CommunityType.CAMPUS, CommunityType.COMPANY), {
            when (it) { CommunityType.CAMPUS -> "Kampüs topluluğu"; CommunityType.COMPANY -> "Şirket spor topluluğu"; else -> "Spor grubu / kulüp çevresi" }
        }, { type = it }, icon = Icons.Rounded.Category)
        Text(
            "Topluluk kalıcı bir spor çevresidir. Tek maçlık Saha veya Seyahat buluşması için İlan ver’i kullan; turnuva kadrosu için Yarış’tan takım kur.",
            style = MaterialTheme.typography.bodySmall,
            color = MColors.Ink600
        )
        MFloatingDropdownBox("Katılım", access, CommunityAccess.entries, { it.label }, { access = it }, icon = Icons.Rounded.LockOpen)
        SportPickerField(sport, { selected ->
            if (selected != null) {
                sport = selected
                if (selected !in sports && sports.size < 12) sports = sports + selected
            }
        }, allowAll = false)
        if (sports.size > 1) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sports.forEach { sp ->
                    MPillActionButton(
                        text = "${sp.emoji} ${sp.label} ✕",
                        tone = MPillTone.NEUTRAL,
                        compact = true,
                        onClick = { if (sports.size > 1) sports = sports - sp }
                    )
                }
            }
        }
        MFloatingActionRow("Bölge", "$selectedCity · $selectedDistrict", Icons.Rounded.LocationOn, onClick = { location = true })
        MFloatingInputBox(
            label = "Topluluğunu Anlat",
            value = description,
            onValueChange = { description = it.take(1000) },
            placeholder = "Etkinlik düzeniniz, buluşma günleriniz ve katılım kurallarınız...",
            icon = Icons.Rounded.EditNote,
            singleLine = false,
            minLines = 3,
            maxLines = 4
        )
        Text(
            "Açık topluluklara doğrudan katılınır. Onaylı topluluklarda başvuruları, davetli topluluklarda davetleri sen yönetirsin.",
            style = MaterialTheme.typography.bodySmall,
            color = MColors.Ink600
        )
        MButton("Topluluğu oluştur", {
            onCreate(CommunityInfo("community_${Clock.System.now().toEpochMilliseconds()}", name.trim(), type, selectedDistrict, 1,
                access, type.emoji, 0, description.trim(), sports, selectedCity))
            onDismiss()
        }, enabled = name.trim().length >= 3)
    }
    if (location) CityAndLocationSelectorSheet(selectedCity, selectedDistrict, false, { location = false }, { c, d, _ -> selectedCity = c; selectedDistrict = d })
}
