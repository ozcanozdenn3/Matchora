package com.matchora.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.matchora.app.data.*
import com.matchora.app.state.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun BackendEventEditing(event: SportEvent) {
    val backend = LocalBackendRepository.current ?: return
    if (event.hostId != DemoData.ME || event.isTeamEvent || (event.startsInMin ?: 1) <= 0) return
    val snapshot by backend.snapshots.collectAsState()
    val command = rememberBackendAction()
    var open by remember { mutableStateOf(false) }
    var editingVersion by remember(event.id) { mutableStateOf<Int?>(null) }
    var title by remember(event.id) { mutableStateOf(event.title) }
    var venue by remember(event.id) { mutableStateOf(event.venue) }
    var capacity by remember(event.id) { mutableStateOf(event.capacity.toString()) }
    var durationMin by remember(event.id) { mutableIntStateOf(event.durationMin) }
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
    TextButton(onClick = {
        editingVersion = snapshot?.a("events")?.find { it.s("id") == event.id }?.i("version")
        title = event.title; venue = event.venue; description = event.description; capacity = event.capacity.toString()
        durationMin = event.durationMin; pricePerPersonTry = event.pricePerPersonTry.toString()
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
    }) { Text("Maç planını düzenle") }
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text("Maç planı") },
        text = { Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(title, { title = it.take(100) }, label = { Text("Başlık") })
            OutlinedTextField(venue, { next ->
                val trimmed = next.take(160)
                if (trimmed != venue) point = null
                venue = trimmed
            }, label = { Text("Saha / buluşma yeri") })
            if (!event.sport.isESport) {
                TextButton(onClick = { pickingPoint = true }) { Text(if (point == null) "Haritadan buluşma noktası seç" else "Buluşma noktasını değiştir") }
                Text("Kesin nokta yalnız organizatöre ve kadrodaki sporculara görünür.", style = MaterialTheme.typography.bodySmall)
            }
            OutlinedTextField(description, { description = it.take(2000) }, label = { Text("Açıklama") })
            OutlinedTextField(capacity, { capacity = it.filter(Char::isDigit).take(3) }, label = { Text("Toplam kontenjan") })
            MFloatingDropdownBox("Maç Süresi", durationMin, listOf(60, 90, 120), { "$it Dakika" }, { durationMin = it })
            OutlinedTextField(pricePerPersonTry, { pricePerPersonTry = it.filter(Char::isDigit).take(6) }, label = { Text("Kişi başı ücret (₺)") })
            MFloatingDropdownBox("Seviye Aralığı", levelRange, listOf("Tüm seviyeler", "Başlangıç – Orta", "Orta – İleri", "İleri"), { it }, { levelRange = it })
            MFloatingDropdownBox("Oyun Modu", mode, PlayMode.entries, { it.label }, { mode = it })
            MFloatingDropdownBox("Katılım Şekli", approvalRequired, listOf(true, false), { if (it) "Organizatör onayı ile" else "Doğrudan katılım" }, { approvalRequired = it })
            OutlinedTextField(neededRolesText, { neededRolesText = it.take(160) }, label = { Text("Aranan roller (virgülle ayır)") })
            if (event.sport.isESport || event.gamePlatform != null) {
                OutlinedTextField(gamePlatform, { gamePlatform = it.take(60) }, label = { Text("Oyun platformu") })
                OutlinedTextField(gameServer, { gameServer = it.take(80) }, label = { Text("Sunucu / bölge") })
                OutlinedTextField(voiceChannel, { voiceChannel = it.take(120) }, label = { Text("Sesli iletişim") })
                OutlinedTextField(hostGameTag, { hostGameTag = it.take(80) }, label = { Text("Organizatör Oyun ID / GamerTag") })
            }
            EventDateTimeField(date, hour, minute) { d, h, m -> date = d; hour = h; minute = m }
            Text("Değişiklikler katılımcılara bildirilir.", style = MaterialTheme.typography.bodySmall)
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
                "durationMin" to durationMin,
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
        }, enabled = title.trim().length >= 3 && venue.trim().length >= 2 && (capacity.toIntOrNull() ?: 0) in 2..100) { Text("Kaydet") } }, dismissButton = { TextButton(onClick = { open = false }) { Text("Vazgeç") } })
    if (pickingPoint) EventLocationPicker(initialPoint = point, initialAddress = venue, city = event.city, onDismiss = { pickingPoint = false }, onConfirm = { selected, address -> point = selected; if (venue.isBlank()) venue = address; pickingPoint = false }, district = event.area)
}

@Composable
fun BackendTeamEditing(team: TeamProfile) {
    val backend = LocalBackendRepository.current ?: return
    val snapshot by backend.snapshots.collectAsState()
    val command = rememberBackendAction()
    if (team.captainId == DemoData.ME) {
        var open by remember { mutableStateOf(false) }
        var description by remember(team.id) { mutableStateOf(team.description) }
        var recruiting by remember(team.id) { mutableStateOf(team.recruitingOpen) }
        var name by remember(team.id) { mutableStateOf(team.name) }
        var badge by remember(team.id) { mutableStateOf(team.badge) }
        var area by remember(team.id) { mutableStateOf(team.area) }
        var city by remember(team.id) { mutableStateOf("") }
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
        TextButton(onClick = {
            val original = snapshot?.a("teams")?.firstOrNull { it.s("id") == team.id }
            version = original?.i("version", 1); city = original?.s("city")?.ifBlank { null } ?: snapshot?.o("profile")?.s("city").orEmpty()
            name = team.name; badge = team.badge; area = team.area; homeVenue = team.homeVenue.orEmpty(); description = team.description; recruiting = team.recruitingOpen; mmr = team.requiredMmr.toString(); availability = team.availability
            color = "#" + team.color.toArgb().toUInt().toString(16).padStart(8, '0').uppercase(); open = true
        }) { Text("Takım ilanını düzenle") }
        if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text("Takım ilanı") },
            text = { Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it.take(60) }, label = { Text("Takım adı") })
                OutlinedTextField(badge, { badge = it.take(12) }, label = { Text("Amblem") })
                MFloatingDropdownBox("Takım rengi", color, (colorNames.keys + color).distinct(), { colorNames[it] ?: "Mevcut renk" }, { color = it })
                OutlinedTextField(city, { city = it.take(60) }, label = { Text("İl") })
                OutlinedTextField(area, { area = it.take(60) }, label = { Text("İlçe / bölge") })
                VenueChoiceField("Ev sahibi tesis / saha", homeVenue, { homeVenue = it.take(160) }, emptyList(), city)
                OutlinedTextField(mmr, { mmr = it.filter(Char::isDigit).take(4) }, label = { Text("Yeni başvurular için en az MMR") })
                OutlinedTextField(availability, { availability = it.take(200) }, label = { Text("Oynama zamanları") })
                OutlinedTextField(description, { description = it.take(1000) }, label = { Text("Açıklama") })
                Row { Text("Oyuncu başvurularına açık", Modifier.weight(1f)); Switch(recruiting, { recruiting = it }) }
            } }, confirmButton = { TextButton(enabled = name.trim().length >= 3 && badge.isNotBlank() && city.trim().length >= 2 && area.trim().length >= 2 && (mmr.toIntOrNull() ?: -1) in 0..3000, onClick = { open = false; command("team.update", apiPayload("teamId" to team.id, "expectedVersion" to version, "name" to name, "badge" to badge, "colorHex" to color, "city" to city, "area" to area, "homeVenue" to homeVenue.ifBlank { null }, "requiredMmr" to mmr.toIntOrNull(), "availability" to availability, "description" to description, "recruitingOpen" to recruiting)) }) { Text("Kaydet") } }, dismissButton = { TextButton(onClick = { open = false }) { Text("Vazgeç") } })
    } else snapshot?.a("teamJoinRequests")?.firstOrNull { it.s("scope_id") == team.id && it.s("user_id") == backend.userId && it.s("status") == "pending" }?.let { request ->
        TextButton(onClick = { command("team.request.cancel", apiPayload("requestId" to request.s("id"))) }) { Text("Takım başvurumu geri çek") }
    }
}

@Composable
fun BackendChallengeActions(challenge: ChallengeOffer) {
    if (LocalBackendRepository.current == null || challenge.incoming || challenge.status != ChallengeStatus.PENDING) return
    val command = rememberBackendAction()
    var confirm by remember { mutableStateOf(false) }
    TextButton(onClick = { confirm = true }) { Text("Bekleyen teklifi geri çek") }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Meydan okumayı geri çek") }, text = { Text("Teklif kapatılacak ve kilitli taahhüt iade edilecek.") }, confirmButton = { TextButton(onClick = { confirm = false; command("challenge.cancel", apiPayload("challengeId" to challenge.id)) }) { Text("Geri çek") } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("Vazgeç") } })
}
