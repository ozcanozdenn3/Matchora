package com.matchora.app.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import com.matchora.app.data.AndroidBackendRuntime
import com.matchora.app.state.SportEvent

actual fun shareEventInvitation(event: SportEvent) {
    shareText("Maç davetini paylaş", eventInvitationText(event))
}

actual fun shareText(title: String, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
    runCatching { AndroidBackendRuntime.context.startActivity(Intent.createChooser(intent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
actual fun openEventDirections(event: SportEvent): Boolean = runCatching {
    check(!isOnlineMeeting(event))
    val point = eventDirectionsPoint(event)
    openLocationDirections(point?.latitude, point?.longitude, "${event.venue}, ${event.area}, ${event.city}")
}.getOrDefault(false)
actual fun openLocationDirections(latitude: Double?, longitude: Double?, query: String): Boolean = runCatching {
    val address = if (latitude != null && longitude != null) {
        Uri.encode("$latitude,$longitude($query)")
    } else {
        Uri.encode(query)
    }
    AndroidBackendRuntime.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$address")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
}.getOrDefault(false)
actual fun openEventCalendar(event: SportEvent): Boolean = runCatching {
    val start = requireNotNull(event.startsAtEpochMillis)
    AndroidBackendRuntime.context.startActivity(Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).apply {
        putExtra(CalendarContract.Events.TITLE, event.title)
        putExtra(CalendarContract.Events.EVENT_LOCATION, if (isOnlineMeeting(event)) event.venue else "${event.venue}, ${event.area}, ${event.city}")
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, start + event.durationMin * 60_000L)
        putExtra(CalendarContract.Events.DESCRIPTION, eventCalendarNotes(event))
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}.isSuccess
