package com.matchora.app.domain

enum class Sport {
    FOOTBALL,
    TENNIS,
    PADEL,
    BASKETBALL,
    VOLLEYBALL,
    RUNNING,
    SWIMMING,
    TABLE_TENNIS,
    BADMINTON,
    FITNESS,
    PILATES_YOGA,
    CYCLING,
    HANDBALL, FUTSAL, RUGBY, AMERICAN_FOOTBALL, BASEBALL, SOFTBALL, CRICKET,
    HOCKEY, ICE_HOCKEY, WATER_POLO, BEACH_VOLLEYBALL,
    SQUASH, PICKLEBALL,
    BOXING, KICKBOXING, WRESTLING, JUDO, KARATE, TAEKWONDO, FENCING,
    ATHLETICS, GYMNASTICS, WEIGHTLIFTING, CROSSFIT, DANCE,
    HIKING, CLIMBING, ORIENTEERING, TRIATHLON,
    ROWING, CANOEING, SAILING, SURFING, DIVING,
    SKIING, SNOWBOARDING, ICE_SKATING, SKATEBOARDING, ROLLER_SKATING,
    GOLF, ARCHERY, EQUESTRIAN, BOWLING, BILLIARDS, DARTS,
    // E-Spor & Zeka Oyunları
    ESPORTS_FC,
    ESPORTS_VALORANT,
    ESPORTS_LOL,
    ESPORTS_CS2,
    CHESS,
    // Özel / Kullanıcı Tanımlı Branş
    CUSTOM
}

/**
 * Strateji belgesi §08 — etkinlik durum makinesi:
 * Draft → Published → Filling → Full → In Progress → Completed → Verified/Disputed → Archived
 */
enum class EventStatus {
    DRAFT,
    PUBLISHED,
    FILLING,
    FULL,
    IN_PROGRESS,
    COMPLETED,
    VERIFIED,
    DISPUTED,
    ARCHIVED,
    CANCELLED;

    companion object {
        val OPEN = FILLING
    }
}

data class SportsPassport(
    val sports: List<Sport>,
    val verifiedMatches: Int,
    val reliabilityScore: Int,
    val fairPlayRating: Float
)

data class MatchEvent(
    val id: String,
    val title: String,
    val sport: Sport,
    val locationArea: String,
    val startsAt: String,
    val participantCount: Int,
    val capacity: Int,
    val status: EventStatus,
    val verifiedOnly: Boolean,
    val customSportName: String? = null
)

data class ReliabilityEvent(
    val type: String,
    val severity: Int,
    val occurredAt: String,
    val source: String
)

data class MatchoraUser(
    val id: String,
    val displayName: String,
    val ageVerified: Boolean,
    val passport: SportsPassport,
    val reliabilityHistory: List<ReliabilityEvent>
)

data class EventChat(
    val eventId: String,
    val participantIds: List<String>,
    val unreadCount: Int
)

data class VerificationCheck(
    val userId: String,
    val type: String,
    val isVerified: Boolean,
    val checkedAt: String?
)

data class EventParticipant(
    val eventId: String,
    val userId: String,
    val role: String,
    val status: String,
    val joinedAt: String
)

data class Team(
    val id: String,
    val name: String,
    val captainId: String,
    val memberIds: List<String>
)

data class Challenge(
    val id: String,
    val challengerTeamId: String,
    val challengedTeamId: String,
    val sport: Sport,
    val status: String
)

data class CheckIn(
    val eventId: String,
    val userId: String,
    val checkedInAt: String,
    val approximateArea: String
)

data class MatchResult(
    val eventId: String,
    val winningTeamId: String?,
    val score: String,
    val submittedBy: String,
    val disputeUntil: String?
)

data class RatingEvent(
    val matchId: String,
    val authorId: String,
    val targetId: String,
    val value: Int,
    val createdAt: String
)

data class Report(
    val reporterId: String,
    val targetId: String,
    val reason: String,
    val severity: Int,
    val status: String
)

data class WalletTransaction(
    val walletId: String,
    val type: String,
    val amount: Long,
    val source: String,
    val idempotencyKey: String
)

data class ChatMessage(
    val chatId: String,
    val senderId: String,
    val body: String,
    val sentAt: String
)

data class NotificationItem(
    val id: String,
    val category: String,
    val title: String,
    val isRead: Boolean
)

data class AuditLog(
    val actorId: String,
    val action: String,
    val reason: String,
    val occurredAt: String
)

data class AvailabilityWindow(
    val userId: String,
    val dayOfWeek: String,
    val startTime: String,
    val endTime: String
)

data class WaitlistEntry(
    val eventId: String,
    val userId: String,
    val position: Int,
    val offerExpiresAt: String?,
    val status: String
)

data class RankingEntry(
    val scope: String,
    val sport: Sport,
    val userId: String,
    val rank: Int,
    val rating: Int,
    val verifiedMatchCount: Int
)

data class Community(
    val id: String,
    val name: String,
    val area: String,
    val memberCount: Int,
    val access: String
)

data class Tournament(
    val id: String,
    val name: String,
    val sport: Sport,
    val format: String,
    val participantCount: Int,
    val capacity: Int,
    val status: String
)

data class Venue(
    val id: String,
    val name: String,
    val area: String,
    val sports: List<Sport>,
    val verifiedBusiness: Boolean,
    val rating: Float
)

data class VenueSlot(
    val venueId: String,
    val startsAt: String,
    val endsAt: String,
    val price: Long,
    val status: String
)

data class Booking(
    val id: String,
    val venueId: String,
    val eventId: String,
    val slot: VenueSlot,
    val status: String,
    val cancellationDeadline: String
)

data class Purchase(
    val id: String,
    val sku: String,
    val platform: String,
    val amount: Long,
    val status: String,
    val idempotencyKey: String
)

data class PairFeedbackEvent(
    val matchId: String,
    val fromUserId: String,
    val toUserId: String,
    val playAgain: Boolean,
    val createdAt: String
)

data class CompatibilityEdge(
    val userA: String,
    val userB: String,
    val score: Int,
    val confidence: Int,
    val sampleCount: Int,
    val lastUpdatedAt: String
)

data class GroupFitSnapshot(
    val groupId: String,
    val score: Int,
    val confidence: Int,
    val memberCount: Int,
    val calculatedAt: String
)

data class AutoSquad(
    val id: String,
    val eventId: String,
    val memberIds: List<String>,
    val fitScore: Int,
    val acceptanceStatus: String
)

data class AnalyticsEvent(
    val name: String,
    val userId: String?,
    val properties: Map<String, String>,
    val occurredAt: String
)
