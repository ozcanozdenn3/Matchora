package com.matchora.app.state

import kotlinx.datetime.*

const val MILLIS_PER_DAY = 86_400_000L
fun todayDate(): LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
fun pickerMillis(date: LocalDate): Long = date.toEpochDays().toLong() * MILLIS_PER_DAY
fun pickerDate(millis: Long): LocalDate = LocalDate.fromEpochDays((millis / MILLIS_PER_DAY).toInt())
fun eventStartMillis(date: LocalDate, hour: Int, minute: Int, zone: TimeZone = TimeZone.currentSystemDefault()): Long =
    LocalDateTime(date, LocalTime(hour, minute)).toInstant(zone).toEpochMilliseconds()
fun eventDateLabel(date: LocalDate): String = "${date.dayOfMonth.toString().padStart(2, '0')}.${date.monthNumber.toString().padStart(2, '0')}.${date.year}"
fun isFutureEvent(date: LocalDate, hour: Int, minute: Int, now: Long = Clock.System.now().toEpochMilliseconds(), zone: TimeZone = TimeZone.currentSystemDefault()): Boolean =
    eventStartMillis(date, hour, minute, zone) > now

fun cityMapCenter(city: String): EventGeoPoint = when (city) {
    "Ankara" -> EventGeoPoint(39.9208, 32.8541)
    "İzmir" -> EventGeoPoint(38.4237, 27.1428)
    "Antalya" -> EventGeoPoint(36.8969, 30.7133)
    "Bursa" -> EventGeoPoint(40.1950, 29.0600)
    "Eskişehir" -> EventGeoPoint(39.7667, 30.5256)
    else -> EventGeoPoint(40.9909, 29.0303)
}

fun SportEvent.matchesDayFilter(filter: String, today: LocalDate = todayDate(), zone: TimeZone = TimeZone.currentSystemDefault()): Boolean {
    if (filter == "Tümü") return true
    val date = startsAtEpochMillis?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(zone).date }
        ?: return day.startsWith(filter, ignoreCase = true)
    return when (filter) {
        "Bugün" -> date == today
        "Yarın" -> date.toEpochDays() == today.toEpochDays() + 1
        "Cumartesi", "Cmt" -> date.dayOfWeek == DayOfWeek.SATURDAY
        "Pazar", "Paz" -> date.dayOfWeek == DayOfWeek.SUNDAY
        else -> eventDateLabel(date) == filter
    }
}
