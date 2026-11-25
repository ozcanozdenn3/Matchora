package com.matchora.app.data

data class PhoneCountryCode(
    val iso: String,
    val name: String,
    val flag: String,
    val dialCode: String,
    val sampleLocal: String,
    val stripsLeadingZero: Boolean = true
)

val phoneCountryCodes: List<PhoneCountryCode> = listOf(
    PhoneCountryCode("TR", "Türkiye", "🇹🇷", "+90", "5XX XXX XX XX"),
    PhoneCountryCode("DE", "Almanya", "🇩🇪", "+49", "15X XXX XXXX"),
    PhoneCountryCode("GB", "Birleşik Krallık", "🇬🇧", "+44", "7XXX XXXXXX"),
    PhoneCountryCode("US", "ABD", "🇺🇸", "+1", "XXX XXX XXXX"),
    PhoneCountryCode("FR", "Fransa", "🇫🇷", "+33", "6 XX XX XX XX"),
    PhoneCountryCode("NL", "Hollanda", "🇳🇱", "+31", "6 XXXXXXXX"),
    PhoneCountryCode("AZ", "Azerbaycan", "🇦🇿", "+994", "5X XXX XX XX"),
    PhoneCountryCode("BE", "Belçika", "🇧🇪", "+32", "4XX XX XX XX"),
    PhoneCountryCode("CH", "İsviçre", "🇨🇭", "+41", "7X XXX XX XX"),
    PhoneCountryCode("AT", "Avusturya", "🇦🇹", "+43", "6XX XXXXXXX"),
    PhoneCountryCode("IT", "İtalya", "🇮🇹", "+39", "3XX XXX XXXX", stripsLeadingZero = false),
    PhoneCountryCode("ES", "İspanya", "🇪🇸", "+34", "6XX XXX XXX"),
    PhoneCountryCode("GR", "Yunanistan", "🇬🇷", "+30", "69X XXX XXXX"),
    PhoneCountryCode("BG", "Bulgaristan", "🇧🇬", "+359", "8X XXX XXXX"),
    PhoneCountryCode("GE", "Gürcistan", "🇬🇪", "+995", "5XX XX XX XX"),
    PhoneCountryCode("CY", "Kıbrıs", "🇨🇾", "+357", "9X XXXXXX"),
    PhoneCountryCode("AE", "Birleşik Arap Emirlikleri", "🇦🇪", "+971", "5X XXX XXXX"),
    PhoneCountryCode("SA", "Suudi Arabistan", "🇸🇦", "+966", "5X XXX XXXX"),
    PhoneCountryCode("QA", "Katar", "🇶🇦", "+974", "3XXX XXXX"),
    PhoneCountryCode("KW", "Kuveyt", "🇰🇼", "+965", "5XXX XXXX"),
    PhoneCountryCode("RU", "Rusya", "🇷🇺", "+7", "9XX XXX XX XX"),
    PhoneCountryCode("UA", "Ukrayna", "🇺🇦", "+380", "50 XXX XX XX"),
    PhoneCountryCode("PL", "Polonya", "🇵🇱", "+48", "5XX XXX XXX"),
    PhoneCountryCode("RO", "Romanya", "🇷🇴", "+40", "7XX XXX XXX"),
    PhoneCountryCode("SE", "İsveç", "🇸🇪", "+46", "7X XXX XX XX"),
    PhoneCountryCode("NO", "Norveç", "🇳🇴", "+47", "4XX XX XXX"),
    PhoneCountryCode("DK", "Danimarka", "🇩🇰", "+45", "2X XX XX XX"),
    PhoneCountryCode("FI", "Finlandiya", "🇫🇮", "+358", "4X XXX XXXX"),
    PhoneCountryCode("PT", "Portekiz", "🇵🇹", "+351", "9X XXX XXXX"),
    PhoneCountryCode("IE", "İrlanda", "🇮🇪", "+353", "8X XXX XXXX"),
    PhoneCountryCode("CZ", "Çekya", "🇨🇿", "+420", "60X XXX XXX"),
    PhoneCountryCode("HU", "Macaristan", "🇭🇺", "+36", "20 XXX XXXX"),
    PhoneCountryCode("HR", "Hırvatistan", "🇭🇷", "+385", "9X XXX XXXX"),
    PhoneCountryCode("RS", "Sırbistan", "🇷🇸", "+381", "6X XXX XXXX"),
    PhoneCountryCode("BA", "Bosna Hersek", "🇧🇦", "+387", "6X XXX XXX"),
    PhoneCountryCode("AL", "Arnavutluk", "🇦🇱", "+355", "6X XXX XXXX"),
    PhoneCountryCode("MK", "Kuzey Makedonya", "🇲🇰", "+389", "7X XXX XXX"),
    PhoneCountryCode("CA", "Kanada", "🇨🇦", "+1", "XXX XXX XXXX"),
    PhoneCountryCode("EG", "Mısır", "🇪🇬", "+20", "10X XXX XXXX"),
    PhoneCountryCode("MA", "Fas", "🇲🇦", "+212", "6XX XXXXXX"),
    PhoneCountryCode("DZ", "Cezayir", "🇩🇿", "+213", "5XX XX XX XX"),
    PhoneCountryCode("TN", "Tunus", "🇹🇳", "+216", "2X XXX XXX"),
    PhoneCountryCode("JO", "Ürdün", "🇯🇴", "+962", "7X XXX XXXX"),
    PhoneCountryCode("LB", "Lübnan", "🇱🇧", "+961", "3 XXX XXX"),
    PhoneCountryCode("IQ", "Irak", "🇮🇶", "+964", "7XX XXX XXXX"),
    PhoneCountryCode("IR", "İran", "🇮🇷", "+98", "9XX XXX XXXX"),
    PhoneCountryCode("PK", "Pakistan", "🇵🇰", "+92", "3XX XXXXXXX"),
    PhoneCountryCode("IN", "Hindistan", "🇮🇳", "+91", "9XXXX XXXXX"),
    PhoneCountryCode("CN", "Çin", "🇨🇳", "+86", "13X XXXX XXXX"),
    PhoneCountryCode("JP", "Japonya", "🇯🇵", "+81", "90 XXXX XXXX"),
    PhoneCountryCode("KR", "Güney Kore", "🇰🇷", "+82", "10 XXXX XXXX"),
    PhoneCountryCode("SG", "Singapur", "🇸🇬", "+65", "8XXX XXXX"),
    PhoneCountryCode("MY", "Malezya", "🇲🇾", "+60", "1X XXX XXXX"),
    PhoneCountryCode("ID", "Endonezya", "🇮🇩", "+62", "81X XXXX XXXX"),
    PhoneCountryCode("TH", "Tayland", "🇹🇭", "+66", "8X XXX XXXX"),
    PhoneCountryCode("AU", "Avustralya", "🇦🇺", "+61", "4XX XXX XXX"),
    PhoneCountryCode("NZ", "Yeni Zelanda", "🇳🇿", "+64", "21 XXX XXXX"),
    PhoneCountryCode("BR", "Brezilya", "🇧🇷", "+55", "11 9XXXX XXXX"),
    PhoneCountryCode("MX", "Meksika", "🇲🇽", "+52", "55 XXXX XXXX"),
    PhoneCountryCode("AR", "Arjantin", "🇦🇷", "+54", "9 11 XXXX XXXX"),
    PhoneCountryCode("ZA", "Güney Afrika", "🇿🇦", "+27", "82 XXX XXXX"),
    PhoneCountryCode("NG", "Nijerya", "🇳🇬", "+234", "80X XXX XXXX"),
    PhoneCountryCode("UZ", "Özbekistan", "🇺🇿", "+998", "90 XXX XX XX"),
    PhoneCountryCode("KZ", "Kazakistan", "🇰🇿", "+7", "70X XXX XX XX"),
    PhoneCountryCode("KG", "Kırgızistan", "🇰🇬", "+996", "55X XXX XXX"),
    PhoneCountryCode("TM", "Türkmenistan", "🇹🇲", "+993", "6X XXXXXX"),
    PhoneCountryCode("TJ", "Tacikistan", "🇹🇯", "+992", "9X XXX XXXX")
)

val defaultPhoneCountry: PhoneCountryCode = phoneCountryCodes.first()

fun supportedSignInProviders(platform: String): List<String> =
    if (platform == "ios") listOf("apple", "google", "phone") else listOf("google", "phone")

fun normalizePhoneWithCountry(country: PhoneCountryCode, localInput: String): String? {
    val raw = localInput.trim()
    if (raw.isBlank()) return null
    if (raw.startsWith("+")) {
        val digits = raw.substring(1).filter(Char::isDigit)
        return if (digits.length in 7..15) "+$digits" else null
    }
    val dialDigits = country.dialCode.removePrefix("+")
    var digits = raw.filter(Char::isDigit)
    if (digits.startsWith("00$dialDigits") && digits.length > dialDigits.length + 5) {
        digits = digits.drop(2 + dialDigits.length)
    }
    if (country.dialCode == "+90") {
        if (digits.startsWith("90") && digits.length == 12) digits = digits.drop(2)
        if (digits.startsWith("0")) digits = digits.drop(1)
        return if (Regex("^5\\d{9}$").matches(digits)) "+90$digits" else null
    }
    if (country.stripsLeadingZero) {
        digits = digits.trimStart('0')
    }
    val e164Digits = dialDigits + digits
    return if (digits.length in 5..14 && e164Digits.length in 7..15) "+$e164Digits" else null
}

fun extractCountryAndLocalInput(currentCountry: PhoneCountryCode, rawInput: String): Pair<PhoneCountryCode, String> {
    val trimmed = rawInput.trim()
    if (trimmed.startsWith("+")) {
        val afterPlus = trimmed.substring(1).filter(Char::isDigit)
        val matched = phoneCountryCodes
            .sortedByDescending { it.dialCode.length }
            .firstOrNull { afterPlus.startsWith(it.dialCode.removePrefix("+")) }
        if (matched != null) {
            val localDigits = afterPlus.removePrefix(matched.dialCode.removePrefix("+")).take(15)
            return matched to localDigits
        }
    }
    val cleaned = buildString {
        var digitCount = 0
        for (ch in rawInput) {
            if (ch.isDigit()) {
                if (digitCount < 15) {
                    append(ch)
                    digitCount++
                }
            } else if (ch == ' ' && isNotEmpty() && last() != ' ') {
                append(ch)
            }
        }
    }
    return currentCountry to cleaned
}

fun splitE164Phone(e164: String): Pair<PhoneCountryCode, String> {
    val trimmed = e164.trim()
    if (trimmed.isBlank()) return defaultPhoneCountry to ""
    val digits = trimmed.removePrefix("+").filter(Char::isDigit)
    val matched = phoneCountryCodes
        .sortedByDescending { it.dialCode.length }
        .firstOrNull { digits.startsWith(it.dialCode.removePrefix("+")) }
        ?: defaultPhoneCountry
    val localDigits = if (trimmed.startsWith("+")) digits.removePrefix(matched.dialCode.removePrefix("+")) else digits
    return matched to localDigits.take(15)
}

fun normalizeAuthPhone(value: String): String? {
    val raw = value.trim()
    if (raw.startsWith("+")) {
        val digits = raw.substring(1).filter(Char::isDigit)
        return if (digits.length in 7..15) "+$digits" else null
    }
    var digits = raw.filter(Char::isDigit)
    if (digits.startsWith("90") && digits.length == 12) digits = digits.substring(2)
    if (digits.startsWith("0") && digits.length == 11) digits = digits.substring(1)
    if (Regex("^5\\d{9}$").matches(digits)) return "+90$digits"
    return if (digits.length in 7..15) "+$digits" else null
}

fun formatFirebasePhoneAuthError(rawMessage: String?): String {
    val msg = rawMessage?.trim().orEmpty()
    if (msg.isBlank()) return "SMS doğrulaması tamamlanamadı. Tekrar dene."
    val lower = msg.lowercase()
    return when {
        "region enabled" in lower || "sms_not_allowed_for_region" in lower || "operation-not-allowed" in lower || "operation_not_allowed" in lower ->
            "Bu ülke/bölge için Firebase SMS gönderimi kapalı. Firebase Console → Authentication → Settings → SMS region policy üzerinden Türkiye (+90) veya ilgili ülkeyi açmalısın."
        "billing" in lower || "quota_exceeded" in lower || "sms quota" in lower ->
            "Firebase SMS kotası doldu veya Blaze faturalandırması gerekli. Test için Firebase Console → Authentication → Phone → Phone numbers for testing kısmını kullanabilirsin."
        "too_many_requests" in lower || "too many" in lower || "blocked all requests" in lower || "unusual activity" in lower ->
            "Çok fazla SMS denemesi yapıldı. Lütfen biraz bekleyip tekrar dene."
        "invalid_phone_number" in lower || "too_short" in lower || "too_long" in lower || ("invalid" in lower && "phone" in lower) ->
            "Telefon numarası geçersiz görünüyor. Ülke kodunu ve numarayı kontrol et."
        "invalid_verification_code" in lower || "invalid verification code" in lower ->
            "Girdiğin doğrulama kodu hatalı. Kontrol edip tekrar dene."
        "session_expired" in lower || "expired" in lower ->
            "Doğrulama kodunun süresi doldu. Yeni kod iste."
        "web_context_cancelled" in lower || "web context was cancelled" in lower || "cancelled" in lower || "canceled" in lower ->
            "Doğrulama penceresi kapatıldı. Devam etmek için tekrar dene."
        "network" in lower ->
            "İnternet bağlantısı nedeniyle doğrulama yapılamadı. Bağlantını kontrol et."
        else -> msg
    }
}
