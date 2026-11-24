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
import com.matchora.app.data.*
import com.matchora.app.domain.Sport
import com.matchora.app.state.*
import com.matchora.app.ui.theme.MColors
import kotlinx.datetime.Clock

@Composable
fun CreateCommunitySheet(city: String, district: String, onDismiss: () -> Unit, onCreate: (CommunityInfo) -> Unit, onPublished: (String) -> Unit = {}) {
    val form = rememberCreationForm("community")
    val draft = form.restored
    var name by remember(form) { mutableStateOf(draft.s("name", "")) }
    var description by remember(form) { mutableStateOf(draft.s("description", "")) }
    var type by remember(form) { mutableStateOf(draft.formEnum("type", CommunityType.CLUB)) }
    var access by remember(form) { mutableStateOf(draft.formEnum("access", CommunityAccess.OPEN)) }
    var sport by remember(form) { mutableStateOf(draft.formEnum("sport", Sport.FOOTBALL)) }
    var sports by remember(form) { mutableStateOf(draft.ss("sports").mapNotNull { runCatching { Sport.valueOf(it) }.getOrNull() }.ifEmpty { listOf(sport) }) }
    var selectedCity by remember(form) { mutableStateOf(draft.s("city", city)) }
    var selectedDistrict by remember(form) { mutableStateOf(draft.s("district", district)) }
    var location by remember { mutableStateOf(false) }
    CreationDraftEffect(form) { apiPayload("name" to name, "description" to description, "type" to type,
        "access" to access, "sport" to sport, "sports" to sports, "city" to selectedCity, "district" to selectedDistrict) }
    ActivitySheet("Topluluğunu oluştur", "Spor çevreni bir araya getir", { form.requestDismiss(onDismiss) }, Icons.Rounded.Hub,
        confirmDismiss = { form.confirmDismiss(onDismiss) }) {
        CreationFormBody(form) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                        onClick = {
                            if (sports.size > 1) {
                                val nextSports = sports - sp
                                sports = nextSports
                                if (sport == sp) sport = nextSports.first()
                            }
                        }
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
        CreationFormNotice(form)
        MButton(if (form.busy) "Topluluğun kaydediliyor…" else "Topluluğu oluştur", {
            val district = selectedDistrict.trim().ifBlank { "Merkez" }
            val community = CommunityInfo("community_${Clock.System.now().toEpochMilliseconds()}", name.trim(), type, district, 1,
                access, type.emoji, 0, description.trim(), sports, selectedCity)
            form.submit("community.create", community.toApiPayload(), { onCreate(community) }, onPublished, onDismiss)
        }, enabled = !form.busy && name.trim().length >= 3 && selectedCity.isNotBlank() && selectedDistrict.trim().length >= 2)
        }
        }
    }
    CreationFormDialogs(form, onDismiss)
    if (location) CityAndLocationSelectorSheet(selectedCity, selectedDistrict, false, { location = false }, { c, d, _ -> selectedCity = c; selectedDistrict = d })
}
