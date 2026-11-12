@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.matchora.app.data

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import platform.AuthenticationServices.*
import platform.Foundation.NSURL
import platform.UIKit.*
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private var currentAuthSession: ASWebAuthenticationSession? = null
private var currentAuthPresentation: ASWebAuthenticationPresentationContextProvidingProtocol? = null

actual suspend fun authenticateInBrowser(url: String): String = withTimeout(5 * 60 * 1000L) {
    suspendCancellableCoroutine { continuation ->
        val anchor = UIApplication.sharedApplication.keyWindow
        val endpoint = NSURL.URLWithString(url)
        if (anchor == null || endpoint?.scheme != "https") {
            continuation.resumeWithException(BackendFailure("BROWSER_UNAVAILABLE", "Giriş penceresi açılamadı."))
            return@suspendCancellableCoroutine
        }
        val presentation = object : NSObject(), ASWebAuthenticationPresentationContextProvidingProtocol {
            override fun presentationAnchorForWebAuthenticationSession(session: ASWebAuthenticationSession): ASPresentationAnchor = anchor
        }
        val session = ASWebAuthenticationSession(endpoint, "matchora") { callback, error ->
            if (continuation.isActive) {
                if (callback != null) continuation.resume(callback.absoluteString.orEmpty())
                else continuation.resumeWithException(BackendFailure("AUTH_CANCELLED", if (error?.code == 1L) "Giriş iptal edildi." else "Giriş tamamlanamadı. Tekrar dene."))
            }
            currentAuthSession = null; currentAuthPresentation = null
        }
        session.presentationContextProvider = presentation
        currentAuthSession = session; currentAuthPresentation = presentation
        continuation.invokeOnCancellation { session.cancel(); currentAuthSession = null; currentAuthPresentation = null }
        if (!session.start()) {
            currentAuthSession = null; currentAuthPresentation = null
            continuation.resumeWithException(BackendFailure("BROWSER_UNAVAILABLE", "Giriş penceresi başlatılamadı."))
        }
    }
}
