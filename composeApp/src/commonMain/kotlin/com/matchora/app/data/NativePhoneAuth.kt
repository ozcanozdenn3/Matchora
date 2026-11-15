package com.matchora.app.data

data class NativePhoneVerification(
    val verificationId: String
)

data class NativePhoneAuthResult(
    val idToken: String,
    val phoneNumber: String
)

expect val isNativePhoneAuthSupported: Boolean

expect suspend fun startNativePhoneVerification(phoneNumber: String): NativePhoneVerification

expect suspend fun verifyNativePhoneCode(verificationId: String, code: String): NativePhoneAuthResult

