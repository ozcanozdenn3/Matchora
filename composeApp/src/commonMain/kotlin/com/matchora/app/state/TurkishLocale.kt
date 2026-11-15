package com.matchora.app.state

private val turkishFoldMap = mapOf(
    'İ' to 'i', 'I' to 'i', 'ı' to 'i',
    'Ş' to 's', 'ş' to 's',
    'Ğ' to 'g', 'ğ' to 'g',
    'Ü' to 'u', 'ü' to 'u',
    'Ö' to 'o', 'ö' to 'o',
    'Ç' to 'c', 'ç' to 'c',
    'Â' to 'a', 'â' to 'a',
    'Î' to 'i', 'î' to 'i',
    'Û' to 'u', 'û' to 'u'
)

/**
 * Normalizes Turkish diacritics and casing cleanly on every device locale (including en_US),
 * preventing "İstanbul" / "Istanbul" / "Kadıköy" / "Kadikoy" mismatches.
 */
fun canonicalTurkishKey(value: String?): String {
    if (value.isNullOrBlank()) return ""
    return buildString(value.length) {
        for (ch in value.trim()) {
            if (ch == '\u0307') continue
            append((turkishFoldMap[ch] ?: ch).lowercaseChar())
        }
    }
}

private val canonicalCityLookup: Map<String, String> by lazy {
    turkeyCities.associateBy { canonicalTurkishKey(it) }
}

fun canonicalTurkishCityName(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val trimmed = raw.trim()
    return canonicalCityLookup[canonicalTurkishKey(trimmed)] ?: trimmed
}

fun matchesTurkishPlace(left: String?, right: String?): Boolean {
    val a = canonicalTurkishKey(left)
    val b = canonicalTurkishKey(right)
    return a.isNotEmpty() && b.isNotEmpty() && a == b
}

