package com.matchora.app.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual val isNativePhoneAuthSupported: Boolean = true

private fun getFirebaseAuth(context: Context): FirebaseAuth {
    if (FirebaseApp.getApps(context).isEmpty()) {
        if (BackendBuildConfig.FIREBASE_APP_ID.isNotBlank()) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId(BackendBuildConfig.FIREBASE_APP_ID)
                    .setApiKey(BackendBuildConfig.FIREBASE_API_KEY)
                    .setProjectId(BackendBuildConfig.FIREBASE_PROJECT_ID)
                    .setGcmSenderId(BackendBuildConfig.FIREBASE_SENDER_ID)
                    .build()
            )
        } else {
            FirebaseApp.initializeApp(context)
        }
    }
    return FirebaseAuth.getInstance()
}

actual suspend fun startNativePhoneVerification(phoneNumber: String): NativePhoneVerification {
    val activity = AndroidBackendRuntime.currentActivity
        ?: throw BackendFailure("ACTIVITY_UNAVAILABLE", "Doğrulama ekranı açılamadı.")

    val auth = getFirebaseAuth(activity)

    return suspendCancellableCoroutine { continuation ->
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    val code = credential.smsCode
                    if (code != null && continuation.isActive) {
                        continuation.resume(NativePhoneVerification(verificationId = "instant:$code"))
                    }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            BackendFailure("FIREBASE_PHONE_ERROR", e.localizedMessage ?: "SMS kodu gönderilemedi.")
                        )
                    }
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    if (continuation.isActive) {
                        continuation.resume(NativePhoneVerification(verificationId))
                    }
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }
}

actual suspend fun verifyNativePhoneCode(verificationId: String, code: String): NativePhoneAuthResult {
    return try {
        val context = AndroidBackendRuntime.context
        val auth = getFirebaseAuth(context)
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        val authResult = auth.signInWithCredential(credential).await()
        val user = authResult.user
            ?: throw BackendFailure("AUTH_FAILED", "Kullanıcı oturumu açılamadı.")
        val tokenResult = user.getIdToken(false).await()
        val idToken = tokenResult.token
            ?: throw BackendFailure("TOKEN_FAILED", "Kimlik belirteci alınamadı.")
        val phone = user.phoneNumber.orEmpty()
        NativePhoneAuthResult(idToken = idToken, phoneNumber = phone)
    } catch (e: Exception) {
        throw BackendFailure("INVALID_CODE", e.localizedMessage ?: "Geçersiz veya süresi dolmuş kod.")
    }
}
