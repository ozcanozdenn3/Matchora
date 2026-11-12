@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.matchora.app.data

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AuthenticationServices.*
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual val isNativeAppleSignInSupported: Boolean = true

actual val isNativeGoogleSignInSupported: Boolean = false
actual suspend fun performNativeGoogleSignIn(): NativeAuthResult? = null

private var activePresentationAnchor: ASPresentationAnchor? = null
private var activeDelegate: NSObject? = null
private var activePresentationContext: NSObject? = null

actual suspend fun performNativeAppleSignIn(): NativeAuthResult? = suspendCancellableCoroutine { continuation ->
    val anchor = UIApplication.sharedApplication.keyWindow
    if (anchor == null) {
        continuation.resume(null)
        return@suspendCancellableCoroutine
    }
    activePresentationAnchor = anchor

    val request = ASAuthorizationAppleIDProvider().createRequest()
    request.requestedScopes = listOf(ASAuthorizationScopeFullName, ASAuthorizationScopeEmail)

    val controller = ASAuthorizationController(listOf(request))

    val presentationContext = object : NSObject(), ASAuthorizationControllerPresentationContextProvidingProtocol {
        override fun presentationAnchorForAuthorizationController(controller: ASAuthorizationController): ASPresentationAnchor {
            return activePresentationAnchor ?: UIApplication.sharedApplication.keyWindow!!
        }
    }

    val delegate = object : NSObject(), ASAuthorizationControllerDelegateProtocol {
        override fun authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization: ASAuthorization) {
            val credential = didCompleteWithAuthorization.credential as? ASAuthorizationAppleIDCredential
            val tokenData = credential?.identityToken
            val tokenString = tokenData?.let { data ->
                NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString()
            }
            if (tokenString.isNullOrBlank()) {
                if (continuation.isActive) continuation.resumeWithException(BackendFailure("AUTH_FAILED", "Apple kimlik belirteci alınamadı."))
                cleanup()
                return
            }
            val nameComponents = credential.fullName
            val given = nameComponents?.givenName.orEmpty()
            val family = nameComponents?.familyName.orEmpty()
            val fullName = listOf(given, family).filter { it.isNotBlank() }.joinToString(" ").takeIf { it.isNotBlank() }
            val email = credential.email

            if (continuation.isActive) {
                continuation.resume(NativeAuthResult(idToken = tokenString, fullName = fullName, email = email))
            }
            cleanup()
        }

        override fun authorizationController(controller: ASAuthorizationController, didCompleteWithError: NSError) {
            if (continuation.isActive) {
                val message = when (didCompleteWithError.code) {
                    1000L -> "Cihazında aktif bir Apple hesabı bulunamadı. Lütfen cihaz Ayarları'ndan Apple ID ile oturum açıp tekrar dene."
                    1001L -> "Apple ile giriş iptal edildi."
                    1002L -> "Apple giriş yanıtı doğrulanamadı. Lütfen tekrar dene."
                    1003L -> "Apple ile giriş isteği işlenemedi. Lütfen tekrar dene."
                    1004L -> "Apple ile giriş başarısız oldu. Lütfen tekrar dene."
                    1005L -> "Apple ile giriş ekranı açılamadı. Cihazını kontrol edip tekrar dene."
                    else -> "Apple ile giriş yapılamadı. Lütfen tekrar dene."
                }
                val code = if (didCompleteWithError.code == 1001L) "AUTH_CANCELLED" else "APPLE_AUTH_ERROR"
                continuation.resumeWithException(BackendFailure(code, message))
            }
            cleanup()
        }

        private fun cleanup() {
            activeDelegate = null
            activePresentationContext = null
            activePresentationAnchor = null
        }
    }

    activeDelegate = delegate
    activePresentationContext = presentationContext

    controller.presentationContextProvider = presentationContext
    controller.delegate = delegate

    continuation.invokeOnCancellation {
        activeDelegate = null
        activePresentationContext = null
        activePresentationAnchor = null
    }

    controller.performRequests()
}
