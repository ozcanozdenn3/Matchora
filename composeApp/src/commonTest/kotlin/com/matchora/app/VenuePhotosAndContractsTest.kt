package com.matchora.app

import com.matchora.app.data.BackendSnapshot
import com.matchora.app.data.DeviceEventBridge
import com.matchora.app.data.apiPayload
import com.matchora.app.ui.components.buildLegalPublishPayload
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.*

class VenuePhotosAndContractsTest {
    @Test fun backendSnapshotParsesUpToFourVenuePhotoUploadIdsAndDefaultsEmpty() {
        val photos = listOf(
            "11111111-1111-4111-8111-111111111111",
            "22222222-2222-4222-8222-222222222222",
            "33333333-3333-4333-8333-333333333333",
            "44444444-4444-4444-8444-444444444444"
        )
        val snapshot = BackendSnapshot(
            apiPayload(
                "userId" to "user-1",
                "venues" to listOf(
                    mapOf(
                        "id" to "venue-with-photos",
                        "name" to "Moda Spor Parkı",
                        "area" to "Kadıköy",
                        "city" to "İstanbul",
                        "sports" to listOf("FOOTBALL"),
                        "rating" to 4.8f,
                        "reviews" to 12,
                        "verified" to true,
                        "amenities" to listOf("Duş"),
                        "courts" to 2,
                        "cancellationHours" to 24,
                        "address" to "Moda Caddesi No 30",
                        "slots" to emptyList<Map<String, Any>>(),
                        "photoUploadIds" to photos
                    ),
                    mapOf(
                        "id" to "venue-no-photos",
                        "name" to "Acıbadem Kort",
                        "area" to "Üsküdar",
                        "city" to "İstanbul",
                        "sports" to listOf("TENNIS"),
                        "rating" to 4.5f,
                        "reviews" to 3,
                        "verified" to false,
                        "amenities" to emptyList<String>(),
                        "courts" to 1,
                        "cancellationHours" to 12,
                        "slots" to emptyList<Map<String, Any>>()
                    )
                )
            )
        )

        val withPhotos = snapshot.venues.first { it.id == "venue-with-photos" }
        val withoutPhotos = snapshot.venues.first { it.id == "venue-no-photos" }
        assertEquals(photos, withPhotos.photoUploadIds)
        assertTrue(withoutPhotos.photoUploadIds.isEmpty())
    }

    @Test fun deviceEventBridgeRoutesAllFiveMatchoraDeepLinksAndRejectsMalformedUrls() {
        DeviceEventBridge.consumeNavigation()
        val cases = listOf(
            "matchora://event/AAAAAAAA-BBBB-4CCC-8DDD-EEEEEEEEEEEE?ref=share#top" to ("EVENT" to "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee"),
            "matchora://tournament/11111111-1111-4111-8111-111111111111" to ("TOURNAMENT" to "11111111-1111-4111-8111-111111111111"),
            "matchora://team/22222222-2222-4222-8222-222222222222" to ("TEAM" to "22222222-2222-4222-8222-222222222222"),
            "matchora://venue/33333333-3333-4333-8333-333333333333" to ("VENUES" to "33333333-3333-4333-8333-333333333333"),
            "matchora://community/44444444-4444-4444-8444-444444444444" to ("NETWORK" to "44444444-4444-4444-8444-444444444444")
        )
        for ((url, expected) in cases) {
            DeviceEventBridge.openUrl(url)
            val nav = DeviceEventBridge.navigation.value
            assertNotNull(nav, "Expected navigation for $url")
            assertEquals(expected.first, nav.destination)
            assertEquals(expected.second, nav.targetId)
            DeviceEventBridge.consumeNavigation()
        }

        DeviceEventBridge.openUrl("https://matchora.app/event/11111111-1111-4111-8111-111111111111")
        assertNull(DeviceEventBridge.navigation.value)
        DeviceEventBridge.openUrl("matchora://unknown/11111111-1111-4111-8111-111111111111")
        assertNull(DeviceEventBridge.navigation.value)
        DeviceEventBridge.openUrl("matchora://event/not valid!!")
        assertNull(DeviceEventBridge.navigation.value)
    }

    @Test fun legalPublishPayloadMatchesBackendContractAndRejectsUnsafeUrls() {
        val valid = buildLegalPublishPayload("privacy", " 2026.2 ", " https://matchora.app/legal/privacy-v2 ")
        assertNotNull(valid)
        assertEquals(setOf("kind", "version", "url"), valid.keys)
        assertEquals("privacy", valid.getValue("kind").jsonPrimitive.content)
        assertEquals("2026.2", valid.getValue("version").jsonPrimitive.content)
        assertEquals("https://matchora.app/legal/privacy-v2", valid.getValue("url").jsonPrimitive.content)
        assertFalse("reason" in valid.keys)

        assertNull(buildLegalPublishPayload("unknown", "2026.2", "https://matchora.app/legal"))
        assertNull(buildLegalPublishPayload("terms", "   ", "https://matchora.app/terms"))
        assertNull(buildLegalPublishPayload("terms", "2026.2", "http://matchora.app/terms"))
        assertNull(buildLegalPublishPayload("guidelines", "2026.2", "https://localhost:8787/guidelines"))
        assertNull(buildLegalPublishPayload("deletion", "2026.2", "https://127.0.0.1/deletion"))
        assertNull(buildLegalPublishPayload("terms", "2026.2", "https://user:pass@matchora.app/terms"))
    }
}

