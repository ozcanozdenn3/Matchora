package com.matchora.app.state

import com.matchora.app.domain.Sport
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** The same validated draft can be sent to a profile repository after authentication. */
data class PassportDraft(
    val name: String = "",
    val birthYear: String = "",
    val city: String = "İstanbul",
    val district: String = "Kadıköy",
    val sport: Sport = Sport.FOOTBALL,
    val position: String = "Orta saha",
    val level: SkillLevel = SkillLevel.INTERMEDIATE,
    val jerseyNumber: String = "",
    val bio: String = "",
    val phone: String = "",
    val email: String = "",
    val dominantHand: String = "Sağ el",
    val dominantFoot: String = "Sağ ayak",
    val days: Set<String> = emptySet(),
    val startTime: String = "19:00",
    val endTime: String = "23:00",
    val campus: String = "",
    val company: String = ""
) {
    fun identityError(year: Int = currentYear()): String? = when {
        name.trim().length < 2 -> "Sahada kullanacağın adı en az 2 karakterle yaz."
        birthYear.toIntOrNull()?.let { it in (year - 100)..(year - 13) } != true -> "Geçerli bir doğum yılı gir (en az 13 yaş)."
        city.isBlank() || district.isBlank() -> "İl ve ilçeni seç."
        else -> null
    }

    fun preferencesError(): String? = when {
        phone.isNotBlank() && normalizeTurkishPhone(phone) == null -> "Telefon numaranı 5 ile başlayan 10 hane olarak gir."
        email.isNotBlank() && !Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+").matches(email.trim()) -> "E-posta adresini kontrol et."
        days.isNotEmpty() && clockMinutes(startTime) >= clockMinutes(endTime) -> "Bitiş saati başlangıçtan sonra olmalı."
        else -> null
    }

    fun toProfile(): UserProfile {
        require(identityError() == null && preferencesError() == null)
        require(position in positionsForSport(sport))
        return UserProfile(name.trim(), city.trim(), district.trim(), bio.trim(), birthYear.toInt(),
            normalizeTurkishPhone(phone).orEmpty(), email.trim(),
            listOf(SportSkill(sport, level, 1000, 0, 0, null, position)),
            emptySet(), 0, 0f, 0, 0, campus.trim().ifBlank { null }, company.trim().ifBlank { null },
            "$dominantFoot · $dominantHand", days.sortedBy { passportDays.indexOf(it) }.map { "$it $startTime–$endTime" },
            jerseyNumber = jerseyNumber.toIntOrNull())
    }
}

val passportDays = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
fun currentYear(): Int = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
private fun clockMinutes(value: String): Int = value.split(':').let { (h, m) -> (h.toIntOrNull() ?: 0) * 60 + (m.toIntOrNull() ?: 0) }
fun normalizeTurkishPhone(value: String): String? {
    val digits = value.filter(Char::isDigit)
    val local = when { digits.length == 12 && digits.startsWith("90") -> digits.drop(2); digits.length == 11 && digits.startsWith("0") -> digits.drop(1); else -> digits }
    return if (local.length == 10 && local.startsWith('5')) "+90$local" else null
}
fun positionsForSport(sport: Sport): List<String> = when (sport) {
    Sport.FOOTBALL, Sport.FUTSAL, Sport.ESPORTS_FC -> listOf("Kaleci", "Defans", "Orta saha", "Forvet / kanat")
    Sport.BASKETBALL -> listOf("Oyun kurucu", "Şutör gard", "Kısa forvet", "Uzun forvet", "Pivot")
    Sport.VOLLEYBALL, Sport.BEACH_VOLLEYBALL -> listOf("Pasör", "Smaçör", "Orta oyuncu", "Libero", "Çapraz")
    Sport.ESPORTS_VALORANT -> listOf("Duelist", "Initiator", "Controller", "Sentinel", "IGL")
    Sport.ESPORTS_CS2 -> listOf("Entry fragger", "AWPer", "Rifler", "Support", "IGL")
    Sport.ESPORTS_LOL -> listOf("Top", "Jungle", "Mid", "ADC", "Support")
    Sport.TENNIS, Sport.TABLE_TENNIS, Sport.BADMINTON, Sport.SQUASH, Sport.PICKLEBALL -> listOf("Tekler", "Çiftler", "Her ikisi")
    Sport.PADEL -> listOf("Sol taraf", "Sağ taraf", "Her iki taraf")
    Sport.CHESS -> listOf("Blitz", "Rapid", "Klasik")
    Sport.SWIMMING -> listOf("Serbest", "Sırtüstü", "Kurbağalama", "Kelebek")
    else -> listOf("Katılımcı", "Antrenman arkadaşı", "Grup lideri")
}
