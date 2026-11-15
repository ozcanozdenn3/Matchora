package com.matchora.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.matchora.app.data.*
import com.matchora.app.state.*
import com.matchora.app.ui.theme.MColors
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun BackendEventEditing(event: SportEvent) {
    val backend = LocalBackendRepository.current ?: return
    if (!backend.enabled) return
    if (event.hostId != DemoData.ME || event.isTeamEvent || (event.startsInMin ?: 1) <= 0) return
    val snapshot by backend.snapshots.collectAsState()
    val command = rememberBackendAction()
    var open by remember { mutableStateOf(false) }
    var editingVersion by remember(event.id) { mutableStateOf<Int?>(null) }
    var title by remember(event.id) { mutableStateOf(event.title) }
    var venue by remember(event.id) { mutableStateOf(event.venue) }
    var capacity by remember(event.id) { mutableStateOf(event.capacity.toString()) }
    var durationText by remember(event.id) { mutableStateOf(event.durationMin.toString()) }
    var pricePerPersonTry by remember(event.id) { mutableStateOf(event.pricePerPersonTry.toString()) }
    var levelRange by remember(event.id) { mutableStateOf(event.levelRange) }
    var mode by remember(event.id) { mutableStateOf(event.mode) }
    var approvalRequired by remember(event.id) { mutableStateOf(event.approvalRequired) }
    var neededRolesText by remember(event.id) { mutableStateOf(event.neededRoles.joinToString(", ")) }
    var gamePlatform by remember(event.id) { mutableStateOf(event.gamePlatform.orEmpty()) }
    var gameServer by remember(event.id) { mutableStateOf(event.gameServer.orEmpty()) }
    var voiceChannel by remember(event.id) { mutableStateOf(event.voiceChannel.orEmpty()) }
    var hostGameTag by remember(event.id) { mutableStateOf(event.hostGameTag.orEmpty()) }
    var description by remember(event.id) { mutableStateOf(event.description) }
    var point by remember(event.id) { mutableStateOf(event.meetingLocation ?: event.approximateLocation) }
    var pickingPoint by remember { mutableStateOf(false) }
    val start = remember(event.id) { event.startsAtEpochMillis?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.of("Europe/Istanbul")) } }
    var date by remember(event.id) { mutableStateOf(start?.date ?: todayDate()) }
    var hour by remember(event.id) { mutableIntStateOf(start?.hour ?: 20) }
    var minute by remember(event.id) { mutableIntStateOf(start?.minute ?: 0) }
    MPillActionButton(
        text = "Maç planını düzenle",
        icon = Icons.Rounded.Edit,
        tone = MPillTone.BRAND,
        compact = true,
        onClick = {
            editingVersion = snapshot?.a("events")?.find { it.s("id") == event.id }?.i("version")
            title = event.title; venue = event.venue; description = event.description; capacity = event.capacity.toString()
            durationText = event.durationMin.toString(); pricePerPersonTry = event.pricePerPersonTry.toString()
            levelRange = event.levelRange; mode = event.mode; approvalRequired = event.approvalRequired
            neededRolesText = event.neededRoles.joinToString(", ")
            gamePlatform = event.gamePlatform.orEmpty(); gameServer = event.gameServer.orEmpty()
            voiceChannel = event.voiceChannel.orEmpty(); hostGameTag = event.hostGameTag.orEmpty()
            point = event.meetingLocation ?: event.approximateLocation
            event.startsAtEpochMillis?.let { stamp ->
                val latest = Instant.fromEpochMilliseconds(stamp).toLocalDateTime(TimeZone.of("Europe/Istanbul"))
                date = latest.date; hour = latest.hour; minute = latest.minute
            }
            open = true
        }
    )
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text("Maç planı") },
        text = { Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MPremiumTextField(title, { title = it.take(100) }, label = "Başlık")
            MPremiumTextField(venue, { next ->
                venue = next.take(160)
            }, label = "Saha / buluşma yeri")
            if (!event.sport.isESport) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MPillActionButton(
                        text = if (point == null) "Haritadan buluşma noktası seç" else "Buluşma noktasını değiştir",
                        icon = Icons.Rounded.AddLocationAlt,
                        tone = MPillTone.NEUTRAL,
                        compact = true,
                        onClick = { pickingPoint = true }
                    )
                    if (point != null) {
                        MPillActionButton(
                            text = "Pini temizle",
                            icon = Icons.Rounded.Close,
                            tone = MPillTone.DANGER,
                            compact = true,
                            onClick = { point = null }
                        )
                    }
                }
                Text("Kesin nokta yalnız organizatöre ve kadrodaki sporculara görünür.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink600)
            }
            MPremiumTextField(description, { description = it.take(2000) }, label = "Açıklama", singleLine = false, minLines = 2, maxLines = 4)
            MPremiumTextField(capacity, { capacity = it.filter(Char::isDigit).take(3) }, label = "Toplam kontenjan")
            MPremiumTextField(durationText, { durationText = it.filter(Char::isDigit).take(3) }, label = "Maç süresi (dakika, 15–720)")
            MPremiumTextField(pricePerPersonTry, { pricePerPersonTry = it.filter(Char::isDigit).take(6) }, label = "Kişi başı ücret (₺)")
            MFloatingDropdownBox("Seviye Aralığı", levelRange, listOf("Tüm seviyeler", "Başlangıç – Orta", "Orta – İleri", "İleri"), { it }, { levelRange = it })
            MFloatingDropdownBox("Oyun Modu", mode, PlayMode.entries, { it.label }, { mode = it })
            MFloatingDropdownBox("Katılım Şekli", approvalRequired, listOf(true, false), { if (it) "Organizatör onayı ile" else "Doğrudan katılım" }, { approvalRequired = it })
            MPremiumTextField(neededRolesText, { neededRolesText = it.take(160) }, label = "Aranan roller (virgülle ayır)")
            if (event.sport.isESport || event.gamePlatform != null) {
                MPremiumTextField(gamePlatform, { gamePlatform = it.take(60) }, label = "Oyun platformu")
                MPremiumTextField(gameServer, { gameServer = it.take(80) }, label = "Sunucu / bölge")
                MPremiumTextField(voiceChannel, { voiceChannel = it.take(120) }, label = "Sesli iletişim")
                MPremiumTextField(hostGameTag, { hostGameTag = it.take(80) }, label = "Organizatör Oyun ID / GamerTag")
            }
            EventDateTimeField(date, hour, minute) { d, h, m -> date = d; hour = h; minute = m }
            Text("Değişiklikler katılımcılara bildirilir.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink600)
        } }, confirmButton = { TextButton(onClick = {
            open = false
            val roles = neededRolesText.split(',').map { it.trim() }.filter { it.isNotBlank() }
            command("event.update", apiPayload(
                "eventId" to event.id,
                "expectedVersion" to editingVersion,
                "title" to title,
                "venue" to venue,
                "description" to description,
                "capacity" to capacity.toIntOrNull(),
                "durationMin" to (durationText.toIntOrNull() ?: event.durationMin).coerceIn(15, 720),
                "pricePerPersonTry" to (pricePerPersonTry.toIntOrNull() ?: 0),
                "levelRange" to levelRange,
                "mode" to mode.name,
                "approvalRequired" to approvalRequired,
                "neededRoles" to roles,
                "gamePlatform" to gamePlatform.takeIf { it.isNotBlank() },
                "gameServer" to gameServer.takeIf { it.isNotBlank() },
                "voiceChannel" to voiceChannel.takeIf { it.isNotBlank() },
                "hostGameTag" to hostGameTag.takeIf { it.isNotBlank() },
                "startsAtEpochMillis" to eventStartMillis(date, hour, minute),
                "approximateLocation" to point?.let { mapOf("latitude" to it.latitude, "longitude" to it.longitude) }
            ))
        }, enabled = title.trim().length >= 3 && venue.trim().length >= 2 && (capacity.toIntOrNull() ?: 0) in 2..100 && (durationText.toIntOrNull() ?: 0) in 15..720) { Text("Kaydet") } }, dismissButton = { TextButton(onClick = { open = false }) { Text("Vazgeç") } })
    if (pickingPoint) EventLocationPicker(initialPoint = point, initialAddress = venue, city = event.city, onDismiss = { pickingPoint = false }, onConfirm = { selected, address -> point = selected; if (venue.isBlank()) venue = address; pickingPoint = false }, district = event.area)
}

@Composable
fun BackendTeamEditing(team: TeamProfile) {
    val backend = LocalBackendRepository.current ?: return
    if (!backend.enabled) return
    val snapshot by backend.snapshots.collectAsState()
    val command = rememberBackendAction()
    if (team.captainId == DemoData.ME) {
        var open by remember { mutableStateOf(false) }
        var showLocationSheet by remember { mutableStateOf(false) }
        var description by remember(team.id) { mutableStateOf(team.description) }
        var recruiting by remember(team.id) { mutableStateOf(team.recruitingOpen) }
        var name by remember(team.id) { mutableStateOf(team.name) }
        var badge by remember(team.id) { mutableStateOf(team.badge) }
        var area by remember(team.id) { mutableStateOf(team.area) }
        var city by remember(team.id) { mutableStateOf(team.city) }
        var homeVenue by remember(team.id) { mutableStateOf(team.homeVenue.orEmpty()) }
        var mmr by remember(team.id) { mutableStateOf(team.requiredMmr.toString()) }
        var availability by remember(team.id) { mutableStateOf(team.availability) }
        var color by remember(team.id) { mutableStateOf("#" + team.color.toArgb().toUInt().toString(16).padStart(8, '0').uppercase()) }
        var version by remember(team.id) { mutableStateOf<Int?>(null) }
        val colorNames = mapOf(
            "#FFFF4D00" to "Turuncu",
            "#FF203B35" to "Yeşil",
            "#FF2563EB" to "Mavi",
            "#FF7C3AED" to "Mor",
            "#FF111827" to "Gece",
            "#FFDC2626" to "Kırmızı",
            "#FF0D9488" to "Turkuaz",
            "#FFD97706" to "Kehribar"
        )
        MPillActionButton(
            text = "Takım ilanını düzenle",
            icon = Icons.Rounded.Edit,
            tone = MPillTone.BRAND,
            compact = true,
            onClick = {
                val original = snapshot?.a("teams")?.firstOrNull { it.s("id") == team.id }
                version = original?.i("version", 1); city = original?.s("city")?.ifBlank { null } ?: snapshot?.o("profile")?.s("city").orEmpty()
                name = team.name; badge = team.badge; area = team.area; homeVenue = team.homeVenue.orEmpty(); description = team.description; recruiting = team.recruitingOpen; mmr = team.requiredMmr.toString(); availability = team.availability
                color = "#" + team.color.toArgb().toUInt().toString(16).padStart(8, '0').uppercase(); open = true
            }
        )
        if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text("Takım ilanı") },
            text = { Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MPremiumTextField(name, { name = it.take(60) }, label = "Takım adı")
                MPremiumTextField(badge, { badge = it.take(12) }, label = "Amblem")
                MFloatingDropdownBox("Takım rengi", color, (colorNames.keys + color).distinct(), { colorNames[it] ?: "Mevcut renk" }, { color = it })
                MFloatingActionRow(
                    label = "İl ve İlçe / Bölge",
                    valueText = listOf(area, city).filter { it.isNotBlank() }.joinToString(", ").ifBlank { "Şehir ve ilçe seç" },
                    icon = Icons.Rounded.Place,
                    onClick = { showLocationSheet = true }
                )
                val liveVenueOptions = remember(snapshot, city) {
                    snapshot?.let(::BackendSnapshot)?.venues.orEmpty()
                        .filter { city.isBlank() || matchesTurkishPlace(it.city, city) }
                        .map { "${it.name} · ${it.area}" }
                        .distinct()
                }
                VenueChoiceField("Ev sahibi tesis / saha", homeVenue, { homeVenue = it.take(160) }, liveVenueOptions, city)
                MPremiumTextField(mmr, { mmr = it.filter(Char::isDigit).take(4) }, label = "Yeni başvurular için en az MMR")
                MPremiumTextField(availability, { availability = it.take(200) }, label = "Oynama zamanları")
                MPremiumTextField(description, { description = it.take(1000) }, label = "Açıklama", singleLine = false, minLines = 2, maxLines = 4)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { Text("Oyuncu başvurularına açık", Modifier.weight(1f)); Switch(recruiting, { recruiting = it }) }
            } }, confirmButton = { TextButton(enabled = name.trim().length >= 3 && badge.isNotBlank() && city.trim().length >= 2 && area.trim().length >= 2 && (mmr.toIntOrNull() ?: -1) in 0..3000, onClick = { open = false; command("team.update", apiPayload("teamId" to team.id, "expectedVersion" to version, "name" to name, "badge" to badge, "colorHex" to color, "city" to city, "area" to area, "homeVenue" to homeVenue.ifBlank { null }, "requiredMmr" to mmr.toIntOrNull(), "availability" to availability, "description" to description, "recruitingOpen" to recruiting)) }) { Text("Kaydet") } }, dismissButton = { TextButton(onClick = { open = false }) { Text("Vazgeç") } })
        if (showLocationSheet) {
            CityAndLocationSelectorSheet(
                currentCity = city.ifBlank { "İstanbul" },
                currentDistrict = area,
                isGpsActive = false,
                onDismiss = { showLocationSheet = false },
                onSelectLocation = { newCity, newDistrict, _ ->
                    city = newCity
                    area = newDistrict.ifBlank { "Merkez" }
                    showLocationSheet = false
                }
            )
        }
    } else snapshot?.a("teamJoinRequests")?.firstOrNull { it.s("scope_id") == team.id && it.s("user_id") == backend.userId && it.s("status") == "pending" }?.let { request ->
        MPillActionButton(
            text = "Takım başvurumu geri çek",
            icon = Icons.Rounded.Close,
            tone = MPillTone.DANGER,
            compact = true,
            onClick = { command("team.request.cancel", apiPayload("requestId" to request.s("id"))) }
        )
    }
}

@Composable
fun BackendChallengeActions(challenge: ChallengeOffer) {
    if (LocalBackendRepository.current?.enabled != true || challenge.incoming || challenge.status != ChallengeStatus.PENDING) return
    val command = rememberBackendAction()
    var confirm by remember { mutableStateOf(false) }
    MPillActionButton(
        text = "Bekleyen teklifi geri çek",
        icon = Icons.Rounded.Close,
        tone = MPillTone.DANGER,
        compact = true,
        onClick = { confirm = true }
    )
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Meydan okumayı geri çek") }, text = { Text("Teklif kapatılacak ve kilitli taahhüt iade edilecek.") }, confirmButton = { TextButton(onClick = { confirm = false; command("challenge.cancel", apiPayload("challengeId" to challenge.id)) }) { Text("Geri çek", color = MColors.Rose) } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("Vazgeç") } })
}
