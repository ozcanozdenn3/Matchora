package com.matchora.app

import com.matchora.app.ui.components.venueChoiceValue
import kotlin.test.*

class VenueSelectionContractTest {
    @Test fun selectedAddressStaysInVenueTextAndDoesNotCorruptTheDistrict() {
        val value = venueChoiceValue("Moda Arena", "Kadıköy", "Moda Caddesi 30")
        assertEquals("Moda Arena, Moda Caddesi 30", value.substringBefore(" · "))
        assertEquals("Kadıköy", value.substringAfter(" · "))
        assertEquals("Moda Arena · Kadıköy", venueChoiceValue("Moda Arena", "Kadıköy", ""))
    }

    @Test fun selectionRespectsTheExisting160CharacterVenueLimit() {
        val address = "Moda Caddesi ".repeat(20)
        val district = "B".repeat(80)
        val value = venueChoiceValue("A".repeat(100), district, address)
        assertTrue(value.length <= 160)
        assertTrue(value.substringBefore(" · ").contains("Moda Caddesi"))
        assertEquals(district, value.substringAfter(" · "))
    }
}
