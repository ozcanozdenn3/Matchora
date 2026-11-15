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

fun cityMapCenter(city: String): EventGeoPoint = when (canonicalTurkishKey(city)) {
    "istanbul" -> EventGeoPoint(40.9909, 29.0303)
    "ankara" -> EventGeoPoint(39.9208, 32.8541)
    "izmir" -> EventGeoPoint(38.4237, 27.1428)
    "antalya" -> EventGeoPoint(36.8969, 30.7133)
    "bursa" -> EventGeoPoint(40.1950, 29.0600)
    "eskisehir" -> EventGeoPoint(39.7667, 30.5256)
    "adana" -> EventGeoPoint(37.0000, 35.3213)
    "konya" -> EventGeoPoint(37.8746, 32.4932)
    "gaziantep" -> EventGeoPoint(37.0662, 37.3833)
    "mersin" -> EventGeoPoint(36.8121, 34.6415)
    "kocaeli" -> EventGeoPoint(40.8533, 29.8815)
    "kayseri" -> EventGeoPoint(38.7312, 35.4787)
    "samsun" -> EventGeoPoint(41.2867, 36.3300)
    "trabzon" -> EventGeoPoint(41.0027, 39.7168)
    "diyarbakir" -> EventGeoPoint(37.9144, 40.2306)
    else -> EventGeoPoint(39.9208, 32.8541)
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
