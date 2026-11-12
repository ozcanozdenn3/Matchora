package com.matchora.app.data

data class NativeAuthResult(
    val idToken: String,
    val fullName: String? = null,
    val email: String? = null
)

expect val isNativeAppleSignInSupported: Boolean
expect suspend fun performNativeAppleSignIn(): NativeAuthResult?

expect val isNativeGoogleSignInSupported: Boolean
expect suspend fun performNativeGoogleSignIn(): NativeAuthResult?

