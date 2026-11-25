package com.matchora.app

import com.matchora.app.data.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import kotlin.test.*

class AuthenticationFlowClientTest {
    private class Storage : SessionPersistence { var value: String? = null; override fun read() = value; override fun write(value: String?) { this.value = value } }
    private fun response(status: Int = 200, value: JsonObject) = ApiHttpResponse(status, value.toString().encodeToByteArray())
    private val session = apiPayload("userId" to "user", "accessToken" to "matchora-session", "refreshToken" to "refresh")
    @Test fun androidNeverOffersAppleAndBothPlatformsOfferPhone() {
        assertEquals(listOf("google", "phone"), supportedSignInProviders("android"))
        assertEquals(listOf("apple", "google", "phone"), supportedSignInProviders("ios"))
    }
    @Test fun countryPhonePickerNormalizesLocalAndInternationalNumbersWithoutManualCountryCode() {
        val turkey = defaultPhoneCountry
        assertEquals("+905350705672", normalizePhoneWithCountry(turkey, "5350705672"))
        assertEquals("+905350705672", normalizePhoneWithCountry(turkey, "0535 070 56 72"))
        assertNull(normalizePhoneWithCountry(turkey, "535070567"))

        val germany = phoneCountryCodes.first { it.iso == "DE" }
        assertEquals("+491512345678", normalizePhoneWithCountry(germany, "1512345678"))
        assertEquals("+491512345678", normalizePhoneWithCountry(germany, "0151 234 5678"))

        val uk = phoneCountryCodes.first { it.iso == "GB" }
        assertEquals("+447911123456", normalizePhoneWithCountry(uk, "07911123456"))

        val (extractedCountry, extractedLocal) = extractCountryAndLocalInput(turkey, "+49 1512345678")
        assertEquals("DE", extractedCountry.iso)
        assertEquals("1512345678", extractedLocal)

        val localized = formatFirebasePhoneAuthError("SMS unable to be sent until this region enabled by the app developer.")
        assertTrue(localized.contains("SMS region policy"))
    }
    @Test fun phoneOtpIsVerifiedBeforeAnySessionOrPassportIsAccepted() = runBlocking {
        val storage = Storage(); val requests = mutableListOf<ApiHttpRequest>()
        val repository = BackendRepository("https://api.example.invalid", "production", transport = { request ->
            requests += request
            when {
                request.url.endsWith("/otp/start") -> response(value = apiPayload("challengeId" to "chal-1", "expiresAt" to 9999999L))
                request.url.endsWith("/otp/verify") -> response(value = session)
                else -> response(value = apiPayload("userId" to "user", "role" to "user", "profile" to null))
            }
        }, persistence = storage)
        repository.startPhoneOtp("+905551112233"); assertFalse(repository.authenticated); assertNull(storage.value)
        repository.verifyPhoneOtp("chal-1", "123456")
        assertTrue(repository.authenticated)
        assertEquals("user", repository.snapshots.value?.s("role"))
        assertNull(BackendSnapshot(repository.snapshots.value!!).profile)
        assertFalse(requests.any { it.url.endsWith("/dev") })
        val verify = requests.first { it.url.endsWith("/otp/verify") }
        assertNull(verify.headers["Authorization"]); assertFalse(storage.value!!.contains("123456"))
    }
    @Test fun rejectedPhoneCodeCannotUnlockAnAccount() = runBlocking {
        val storage = Storage()
        val repository = BackendRepository("https://api.example.invalid", "production", transport = {
            response(401, apiPayload("error" to mapOf("code" to "OTP_INVALID", "message" to "Kod geçersiz")))
        }, persistence = storage)
        assertEquals("OTP_INVALID", assertFailsWith<BackendFailure> { repository.verifyPhoneOtp("chal-1", "000000") }.code)
        assertFalse(repository.authenticated); assertNull(storage.value); assertNull(repository.snapshots.value)
    }
    @Test fun socialButtonsUseRealPkceEvenInLocalModeAndNeverDevelopmentLogin() = runBlocking {
        val requests = mutableListOf<ApiHttpRequest>(); val storage = Storage()
        val repository = BackendRepository("https://api.example.invalid", "local", transport = { request ->
            requests += request
            when {
                request.url.endsWith("/oauth/start") -> response(value = apiPayload("flowId" to "flow", "clientSecret" to "secret", "url" to "https://auth.example.invalid/authorize"))
                request.url.endsWith("/oauth/finish") -> response(value = session)
                else -> response(value = apiPayload("userId" to "user"))
            }
        }, persistence = storage)
        repository.socialLogin("Google") { url -> assertEquals("https://auth.example.invalid/authorize", url); "matchora://auth/callback?flow=flow&code=provider-code" }
        assertTrue(repository.authenticated); assertFalse(requests.any { it.url.endsWith("/dev") })
        assertEquals("google", Json.parseToJsonElement(requests.first().body!!.decodeToString()).jsonObject.s("provider"))
    }
    @Test fun anUnrelatedBrowserCallbackCannotCompleteSignIn() = runBlocking {
        var finished = false
        val repository = BackendRepository("https://api.example.invalid", "production", transport = { request ->
            if (request.url.endsWith("/oauth/finish")) finished = true
            response(value = apiPayload("flowId" to "expected", "clientSecret" to "private", "url" to "https://auth.example.invalid/authorize"))
        }, persistence = Storage())
        assertEquals("OAUTH_STATE", assertFailsWith<BackendFailure> { repository.socialLogin("Apple") { "matchora://auth/callback?flow=wrong&code=provider-code" } }.code)
        assertFalse(finished); assertFalse(repository.authenticated)
    }
}
