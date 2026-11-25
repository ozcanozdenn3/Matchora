@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.matchora.app.data

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AuthenticationServices.*
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Foundation.*
import platform.Security.SecRandomCopyBytes
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

private fun resolveAppleAnchor(): UIWindow? {
    val activeScene = UIApplication.sharedApplication.connectedScenes
        .filterIsInstance<UIWindowScene>()
        .firstOrNull { it.activationState == UISceneActivationStateForegroundActive }
        ?: UIApplication.sharedApplication.connectedScenes.filterIsInstance<UIWindowScene>().firstOrNull()
    val sceneWindows = activeScene?.windows?.filterIsInstance<UIWindow>().orEmpty()
    return sceneWindows.firstOrNull { it.isKeyWindow() }
        ?: sceneWindows.firstOrNull()
        ?: UIApplication.sharedApplication.keyWindow
}

private fun randomNonce(length: Int = 32): String {
    val charset = "0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._"
    val bytes = UByteArray(length)
    bytes.usePinned { pinned ->
        SecRandomCopyBytes(null, length.toULong(), pinned.addressOf(0))
    }
    return buildString(length) {
        for (b in bytes) append(charset[(b.toInt() and 0xFF) % charset.length])
    }
}

private fun sha256Hex(input: String): String {
    val data = input.encodeToByteArray()
    val digest = UByteArray(CC_SHA256_DIGEST_LENGTH)
    data.usePinned { dataPinned ->
        digest.usePinned { digestPinned ->
            CC_SHA256(dataPinned.addressOf(0), data.size.toUInt(), digestPinned.addressOf(0))
        }
    }
    return digest.joinToString("") { it.toString(16).padStart(2, '0') }
}

actual suspend fun performNativeAppleSignIn(): NativeAuthResult? = suspendCancellableCoroutine { continuation ->
    val anchor = resolveAppleAnchor()
    if (anchor == null) {
        continuation.resume(null)
        return@suspendCancellableCoroutine
    }
    activePresentationAnchor = anchor

    val rawNonce = randomNonce()
    val request = ASAuthorizationAppleIDProvider().createRequest()
    request.requestedScopes = listOf(ASAuthorizationScopeFullName, ASAuthorizationScopeEmail)
    request.nonce = sha256Hex(rawNonce)

    val controller = ASAuthorizationController(listOf(request))

    val presentationContext = object : NSObject(), ASAuthorizationControllerPresentationContextProvidingProtocol {
        override fun presentationAnchorForAuthorizationController(controller: ASAuthorizationController): ASPresentationAnchor {
            return activePresentationAnchor ?: resolveAppleAnchor()!!
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
                continuation.resume(NativeAuthResult(idToken = tokenString, fullName = fullName, email = email, nonce = rawNonce))
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
