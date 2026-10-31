package com.matchora.app.state

import com.matchora.app.domain.Sport

/** Formats are shared by the demo creation UI; group activities do not imply opposing teams. */
fun formatsForSport(sport: Sport): List<String> = when (sport) {
    Sport.FOOTBALL -> listOf("5v5", "6v6", "7v7", "11v11")
    Sport.FUTSAL -> listOf("5v5")
    Sport.BASKETBALL -> listOf("1v1", "2v2", "3v3", "5v5")
    Sport.VOLLEYBALL -> listOf("2v2", "4v4", "6v6")
    Sport.BEACH_VOLLEYBALL -> listOf("2v2", "4v4")
    Sport.HANDBALL, Sport.WATER_POLO -> listOf("7v7")
    Sport.RUGBY -> listOf("7v7", "15v15")
    Sport.AMERICAN_FOOTBALL, Sport.HOCKEY, Sport.CRICKET -> listOf("11v11")
    Sport.BASEBALL, Sport.SOFTBALL -> listOf("9v9")
    Sport.ICE_HOCKEY -> listOf("6v6")
    Sport.TENNIS, Sport.PADEL, Sport.TABLE_TENNIS, Sport.BADMINTON, Sport.SQUASH, Sport.PICKLEBALL,
    Sport.ESPORTS_FC, Sport.CHESS -> listOf("1v1", "2v2")
    Sport.ESPORTS_VALORANT, Sport.ESPORTS_LOL, Sport.ESPORTS_CS2 -> listOf("5v5")
    Sport.BOXING, Sport.KICKBOXING, Sport.WRESTLING, Sport.JUDO, Sport.KARATE, Sport.TAEKWONDO, Sport.FENCING -> listOf("1v1", "Grup · 6 kişi")
    else -> listOf("Grup · 6 kişi", "Grup · 12 kişi", "Grup · 20 kişi")
}

fun defaultFormatForSport(sport: Sport): String = when (sport) {
    Sport.FOOTBALL -> "7v7"
    Sport.BASKETBALL -> "3v3"
    Sport.VOLLEYBALL -> "6v6"
    Sport.PADEL -> "2v2"
    else -> formatsForSport(sport).first()
}

fun capacityForFormat(format: String): Int {
    val group = Regex("Grup · (\\d+) kişi").matchEntire(format)?.groupValues?.get(1)?.toIntOrNull()
    if (group != null) return group.coerceIn(2, 40)
    val sides = format.split("v").mapNotNull { it.toIntOrNull() }
    return if (sides.size == 2 && sides.all { it in 1..20 }) sides.sum() else 6
}

fun bookingShare(total: Int, people: Int): Int =
    (total.coerceAtLeast(0) + people.coerceAtLeast(1) - 1) / people.coerceAtLeast(1)
