package com.matchora.app.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.matchora.app.state.SportEvent
import com.matchora.app.state.EventGeoPoint

/** Clean unique display code for share sheets and UI cards (e.g. #ILN-842104). */
fun eventDisplayId(eventId: String): String {
    val clean = eventId.replace("-", "").replace("_", "")
    val code = if (clean.length > 6) clean.takeLast(6).uppercase() else clean.uppercase()
    return "#ILN-$code"
}

/** Clean unique display code for tournaments (e.g. #TRN-391024). */
fun tournamentDisplayId(tournamentId: String): String {
    val clean = tournamentId.replace("-", "").replace("_", "")
    val code = if (clean.length > 6) clean.takeLast(6).uppercase() else clean.uppercase()
    return "#TRN-$code"
}

/** Clean unique display code for venues (e.g. #TSS-201948). */
fun venueDisplayId(venueId: String): String {
    val clean = venueId.replace("-", "").replace("_", "")
    val code = if (clean.length > 6) clean.takeLast(6).uppercase() else clean.uppercase()
    return "#TSS-$code"
}

/** Clean unique display code for clubs/teams (e.g. #KLB-510924). */
fun teamDisplayId(teamId: String): String {
    val clean = teamId.replace("-", "").replace("_", "")
    val code = if (clean.length > 6) clean.takeLast(6).uppercase() else clean.uppercase()
    return "#KLB-$code"
}

/** Clean unique display code for communities (e.g. #TPL-912384). */
fun communityDisplayId(communityId: String): String {
    val clean = communityId.replace("-", "").replace("_", "")
    val code = if (clean.length > 6) clean.takeLast(6).uppercase() else clean.uppercase()
    return "#TPL-$code"
}

/** The server UUID is the permanent invitation ID; preview fixtures keep their own IDs. */
internal fun eventInvitationId(value: String, preview: Boolean = false): String? {
    val input = value.trim()
    val candidate = if (input.startsWith("matchora://event/")) input.removePrefix("matchora://event/").substringBefore('?').substringBefore('#')
        else if (input.startsWith("matchora://tournament/")) input.removePrefix("matchora://tournament/").substringBefore('?').substringBefore('#')
        else if (input.startsWith("matchora://venue/")) input.removePrefix("matchora://venue/").substringBefore('?').substringBefore('#')
        else if (input.startsWith("matchora://team/")) input.removePrefix("matchora://team/").substringBefore('?').substringBefore('#')
        else if (input.startsWith("matchora://community/")) input.removePrefix("matchora://community/").substringBefore('?').substringBefore('#')
        else input.removePrefix("#").trim()
    if (candidate.startsWith("https://") || candidate.startsWith("http://") || candidate.contains("/") || candidate.contains("..")) return null
    return when {
        Regex("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}").matches(candidate) -> candidate.lowercase()
        Regex("(?i)^(ILN|TRN|TSS|KLB|TPL|TKM|EV|LGE)-[A-Za-z0-9_-]{1,50}$").matches(candidate) -> candidate
        preview && Regex("[A-Za-z0-9_-]{1,100}").matches(candidate) -> candidate
        else -> null
    }
}

internal fun eventInvitationText(event: SportEvent): String = buildString {
    append("${event.title}\n${event.day} · ${event.time}\n${event.venue} · ${event.area}, ${event.city}")
    append("\n\nİlan ID: ${event.id}\nDavet Kodu: ${eventDisplayId(event.id)}\nMatchora → Spor radarı → ID ile bul")
    append("\nmatchora://event/${event.id}")
}

internal fun tournamentInvitationText(name: String, id: String, dates: String, format: String, organizer: String): String = buildString {
    append("🏆 Matchora Turnuva / Lig Daveti: $name\n")
    append("Format: $format · Tarih: $dates\nOrganizatör: $organizer\n")
    append("Turnuva Kodu: ${tournamentDisplayId(id)}\n")
    append("Turnuva ID: $id\n\n")
    append("Matchora uygulamasını aç → 'ID ile bul' alanına kodu yapıştırarak turnuvaya doğrudan katılabilirsin:\n")
    append("matchora://tournament/$id")
}

internal fun teamInvitationText(name: String, id: String, sport: String, area: String, badge: String): String = buildString {
    append("🛡️ $badge $name Kulüp Daveti\n")
    append("Branş: $sport · Bölge: $area\n")
    append("Kulüp Kodu: ${teamDisplayId(id)}\nKulüp ID: $id\n\n")
    append("Matchora uygulamasını aç → 'ID ile bul' alanına bu kodu yapıştırarak kulübü görüntüleyebilir ve kadroya katılabilirsin:\n")
    append("matchora://team/$id")
}

internal fun venueInvitationText(name: String, id: String, area: String, city: String): String = buildString {
    append("🏟️ $name · Spor Tesisi\n")
    append("Konum: $area, $city\n")
    append("Tesis Kodu: ${venueDisplayId(id)}\nTesis ID: $id\n\n")
    append("Matchora uygulamasını aç → 'ID ile bul' alanına bu kodu yapıştırarak tesisi inceleyebilir ve saha rezervasyonu yapabilirsin:\n")
    append("matchora://venue/$id")
}

internal fun communityInvitationText(name: String, id: String, area: String, type: String): String = buildString {
    append("👥 $name · Spor Topluluğu Daveti\n")
    append("Tür: $type · Bölge: $area\n")
    append("Topluluk Kodu: ${communityDisplayId(id)}\nTopluluk ID: $id\n\n")
    append("Matchora uygulamasını aç → 'ID ile bul' alanına bu kodu yapıştırarak topluluğu görüntüleyebilir ve katılabilirsin:\n")
    append("matchora://community/$id")
}

internal fun isOnlineMeeting(event: SportEvent): Boolean =
    event.area.contains("online", ignoreCase = true) || event.venue.contains("online", ignoreCase = true) ||
        (event.gamePlatform?.isNotBlank() == true && event.approximateLocation == null && event.meetingLocation == null)

/** Public map pins are deliberately rounded and must not become a precise navigation target. */
internal fun eventDirectionsPoint(event: SportEvent): EventGeoPoint? = if (isOnlineMeeting(event)) null
    else event.meetingLocation?.takeIf { validMapCoordinate(it.latitude, it.longitude) }

internal fun eventCalendarNotes(event: SportEvent): String = buildString {
    append("Matchora\nmatchora://event/${event.id}")
    if (isOnlineMeeting(event)) {
        event.gamePlatform?.takeIf { it.isNotBlank() }?.let { append("\nPlatform: $it") }
        event.gameServer?.takeIf { it.isNotBlank() }?.let { append("\nSunucu / lobi: $it") }
        event.voiceChannel?.takeIf { it.isNotBlank() }?.let { append("\nSes kanalı: $it") }
    }
}

expect fun shareEventInvitation(event: SportEvent)
expect fun shareText(title: String, text: String)
expect fun openEventDirections(event: SportEvent): Boolean
expect fun openLocationDirections(latitude: Double?, longitude: Double?, query: String): Boolean
internal fun calendarNotesForViewer(event: SportEvent, includeInviteLink: Boolean): String =
    eventCalendarNotes(event).let { notes ->
        if (includeInviteLink) notes
        else notes.lines().filterNot { it.startsWith("matchora://") }.joinToString("\n")
    }

/** Matches only exact event IDs or explicit #ILN-XXXXXX display invite codes (never partial prefixes). */
internal fun matchesEventInviteCode(eventId: String, query: String): Boolean {
    val trimmed = query.trim()
    val normalized = trimmed.removePrefix("#")
    if (normalized.isBlank()) return false
    val displayCode = eventDisplayId(eventId).removePrefix("#")
    return eventId.equals(trimmed, ignoreCase = true) ||
        eventId.equals(normalized, ignoreCase = true) ||
        displayCode.equals(normalized, ignoreCase = true)
}

expect fun openEventCalendar(event: SportEvent, includeInviteLink: Boolean = true): Boolean

@Composable
fun EventDeviceActions(event: SportEvent, showDirections: Boolean = true, includeInviteLink: Boolean = false) {
    var error by remember { mutableStateOf<String?>(null) }
    if (showDirections && !isOnlineMeeting(event)) TextButton(onClick = { if (!openEventDirections(event)) error = "Harita uygulaması açılamadı. Tesis adresini elle arayabilirsin." }) { Text("Yol tarifi al") }
    if (event.startsAtEpochMillis != null) TextButton(onClick = { if (!openEventCalendar(event, includeInviteLink)) error = "Takvim açılamadı. Cihazında bir takvim uygulaması bulunduğunu kontrol et." }) { Text("Cihaz takvimine ekle") }
    error?.let { message -> AlertDialog(onDismissRequest = { error = null }, title = { Text("Cihaz bağlantısı") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { error = null }) { Text("Tamam") } }) }
}
