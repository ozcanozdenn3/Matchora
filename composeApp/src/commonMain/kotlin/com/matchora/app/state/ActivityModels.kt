package com.matchora.app.state

/** Local UI records. A server adapter owns acceptance, verification and final settlement. */
data class MatchResultDraft(
    val eventId: String,
    val homeScore: Int,
    val awayScore: Int,
    val note: String,
    val fairPlay: Int,
    val disputed: Boolean = false,
    val disputeReason: String = ""
)

data class SupportRequest(val id: String, val subject: String, val message: String, val category: String)
data class ModerationReport(val id: String, val target: String, val reason: String, val details: String)
