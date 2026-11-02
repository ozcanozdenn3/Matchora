package com.matchora.app

import com.matchora.app.domain.Sport
import com.matchora.app.state.*
import kotlin.test.*

class PassportDraftTest {
    private fun draft() = PassportDraft(name = "Deniz", birthYear = (currentYear() - 24).toString())

    @Test fun identityRequiresRealInputAndAValidAge() {
        assertNotNull(PassportDraft().identityError())
        assertNotNull(draft().copy(birthYear = "abcd").identityError())
        assertNotNull(draft().copy(birthYear = currentYear().toString()).identityError())
        assertNull(draft().identityError())
    }

    @Test fun optionalContactAndAvailabilityDoNotBlockTheFirstPassport() {
        assertNull(draft().preferencesError())
        val profile = draft().toProfile()
        assertEquals("", profile.phone)
        assertEquals("", profile.email)
        assertTrue(profile.availability.isEmpty())
    }

    @Test fun contactFormatsAreValidatedAndNormalized() {
        assertEquals("+905321234567", normalizeTurkishPhone("0532 123 45 67"))
        assertEquals("+905321234567", normalizeTurkishPhone("+90 532 123 45 67"))
        assertNull(normalizeTurkishPhone("1234567890"))
        assertNotNull(draft().copy(email = "a@b").preferencesError())
        assertNotNull(draft().copy(phone = "123").preferencesError())
    }

    @Test fun selectedDaysRequireAnOrderedTimeWindow() {
        assertNotNull(draft().copy(days = setOf("Pzt"), startTime = "21:00", endTime = "19:00").preferencesError())
        assertNull(draft().copy(days = setOf("Pzt"), startTime = "19:00", endTime = "21:00").preferencesError())
        assertEquals(listOf("Pzt 19:00–23:00", "Paz 19:00–23:00"), draft().copy(days = setOf("Paz", "Pzt")).toProfile().availability)
    }

    @Test fun firstPassportDoesNotFabricateVerifiedHistoryOrBadges() {
        val profile = draft().toProfile()
        assertTrue(profile.verifications.isEmpty())
        assertEquals(0, profile.verifiedMatches)
        assertEquals(0, profile.skills.single().verifiedMatches)
        assertNull(profile.skills.single().localRank)
    }

    @Test fun allSportsHaveCompleteRolesAndTheSelectedRoleIsPreserved() {
        Sport.entries.forEach { sport ->
            val role = positionsForSport(sport).last()
            assertTrue(role.isNotBlank())
            assertEquals(role, draft().copy(sport = sport, position = role).toProfile().skills.single().position)
        }
        assertFailsWith<IllegalArgumentException> { draft().copy(sport = Sport.TENNIS, position = "Kaleci").toProfile() }
    }

    @Test fun jerseyNumberIsStoredAsItsOwnField() {
        val profile = draft().copy(jerseyNumber = "10").toProfile()
        assertEquals(10, profile.jerseyNumber)
        assertEquals("Orta saha", profile.skills.single().position)
    }
}
