package com.matchora.app.state

/** Local UI records. A server adapter owns acceptance, verification and final settlement. */
data class MatchResultDraft(
    val eventId: String,
    val homeScore: Int,
    val awayScore: Int,
    val note: String,
    val fairPlay: Int,
    val disputed: Boolean = false,
    val disputeReason: String = "",
    val status: String = "pending",
    val submittedBy: String? = null,
    val disputeUntil: Long? = null,
    val homeParticipantIds: List<String> = emptyList(),
    val awayParticipantIds: List<String> = emptyList()
)

data class SupportRequest(val id: String, val subject: String, val message: String, val category: String, val status: String = "open", val response: String? = null)
data class ModerationReport(val id: String, val target: String, val reason: String, val details: String, val status: String = "open")


fun formatTryMinor(amount: Int): String {
    require(amount >= 0)
    val cents = amount % 100
    return if (cents == 0) "${amount / 100}" else "${amount / 100},${cents.toString().padStart(2, '0')}"
}
fun splitTryMinor(amount: Int, people: Int): Int {
    require(amount >= 0 && people > 0)
    return ((amount.toLong() + people - 1) / people).toInt()
}
val VenueSlotInfo.priceLabel: String get() = formatTryMinor(priceMinor)
val VenuePayment.amountLabel: String get() = formatTryMinor(amountMinor)
val VenueInfo.fromPriceLabel: String get() = formatTryMinor(slots.minOfOrNull { it.priceMinor } ?: fromPriceTry * 100)
