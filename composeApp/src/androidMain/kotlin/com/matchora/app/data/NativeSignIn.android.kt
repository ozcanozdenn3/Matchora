package com.matchora.app.data

import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

actual val isNativeAppleSignInSupported: Boolean = false
actual suspend fun performNativeAppleSignIn(): NativeAuthResult? = null

actual val isNativeGoogleSignInSupported: Boolean = true

private const val GOOGLE_SERVER_CLIENT_ID = "715540839618-b9in3qocsbktaor26rvog84786lbnq8d.apps.googleusercontent.com"

actual suspend fun performNativeGoogleSignIn(): NativeAuthResult? {
    val activity = AndroidBackendRuntime.currentActivity ?: return null
    val credentialManager = CredentialManager.create(activity)

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(GOOGLE_SERVER_CLIENT_ID)
        .setAutoSelectEnabled(false)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    return try {
        val result = credentialManager.getCredential(activity, request)
        val credential = result.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            NativeAuthResult(
                idToken = googleIdTokenCredential.idToken,
                fullName = googleIdTokenCredential.displayName,
                email = googleIdTokenCredential.id
            )
        } else {
            null
        }
    } catch (cancel: GetCredentialCancellationException) {
        throw BackendFailure("AUTH_CANCELLED", "Google ile giriş iptal edildi.")
    } catch (e: GetCredentialException) {
        throw BackendFailure("GOOGLE_AUTH_FAILED", e.message ?: "Google ile giriş tamamlanamadı.")
    } catch (e: Exception) {
        throw BackendFailure("GOOGLE_AUTH_FAILED", e.message ?: "Google ile giriş tamamlanamadı.")
    }
}
