package com.matchora.app.data

fun supportedSignInProviders(platform: String): List<String> =
    if (platform == "ios") listOf("apple", "google", "email") else listOf("google", "email")

fun normalizedAuthEmail(value: String): String? = value.trim().lowercase().takeIf { email ->
    email.length in 3..254 && email.none { it.code < 32 || it.code == 127 } && Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email)
}
