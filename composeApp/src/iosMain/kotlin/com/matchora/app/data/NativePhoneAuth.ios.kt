package com.matchora.app.data

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual val isNativePhoneAuthSupported: Boolean = true

actual suspend fun startNativePhoneVerification(phoneNumber: String): NativePhoneVerification {
    val delegate = DeviceEventBridge.phoneAuthDelegate
        ?: throw BackendFailure("PHONE_AUTH_UNAVAILABLE", "iOS telefon doğrulama servisi hazır değil.")

    return suspendCancellableCoroutine { continuation ->
        delegate.startVerification(
            phoneNumber = phoneNumber,
            callback = object : PhoneVerificationCallback {
                override fun onCodeSent(verificationId: String) {
                    if (continuation.isActive) {
                        continuation.resume(NativePhoneVerification(verificationId))
                    }
                }

                override fun onError(error: String) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            BackendFailure("FIREBASE_PHONE_ERROR", error)
                        )
                    }
                }
            }
        )
    }
}

actual suspend fun verifyNativePhoneCode(verificationId: String, code: String): NativePhoneAuthResult {
    val delegate = DeviceEventBridge.phoneAuthDelegate
        ?: throw BackendFailure("PHONE_AUTH_UNAVAILABLE", "iOS telefon doğrulama servisi hazır değil.")

    return suspendCancellableCoroutine { continuation ->
        delegate.verifyCode(
            verificationId = verificationId,
            code = code,
            callback = object : PhoneAuthCallback {
                override fun onSuccess(idToken: String, phoneNumber: String) {
                    if (continuation.isActive) {
                        continuation.resume(NativePhoneAuthResult(idToken = idToken, phoneNumber = phoneNumber))
                    }
                }

                override fun onError(error: String) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            BackendFailure("INVALID_CODE", error)
                        )
                    }
                }
            }
        )
    }
}
