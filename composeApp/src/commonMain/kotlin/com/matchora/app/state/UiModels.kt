package com.matchora.app.state

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.matchora.app.domain.EventStatus
import com.matchora.app.domain.Sport
import com.matchora.app.ui.theme.MColors

// ═══════════════════════════════════════════════════════════════════════════════
// UI MODELLERİ — Strateji belgesindeki ekran envanterini besleyen görünüm modelleri.
// Backend sözleşmeleri domain/Models.kt içinde kalır.
// ═══════════════════════════════════════════════════════════════════════════════

val Sport.label: String
    get() = when (this) {
        Sport.FOOTBALL -> "Futbol"
        Sport.TENNIS -> "Tenis"
        Sport.PADEL -> "Padel"
        Sport.BASKETBALL -> "Basketbol"
        Sport.VOLLEYBALL -> "Voleybol"
        Sport.RUNNING -> "Koşu"
        Sport.SWIMMING -> "Yüzme"
        Sport.TABLE_TENNIS -> "Masa Tenisi"
        Sport.BADMINTON -> "Badminton"
        Sport.FITNESS -> "Fitness / GYM"
        Sport.PILATES_YOGA -> "Pilates & Yoga"
        Sport.CYCLING -> "Bisiklet"
        Sport.ESPORTS_FC -> "FC 25 (FIFA)"
        Sport.ESPORTS_VALORANT -> "Valorant"
        Sport.ESPORTS_LOL -> "League of Legends"
        Sport.ESPORTS_CS2 -> "CS2"
        Sport.CHESS -> "Satranç"
        Sport.HANDBALL -> "Hentbol"
        Sport.FUTSAL -> "Futsal"
        Sport.RUGBY -> "Ragbi"
        Sport.AMERICAN_FOOTBALL -> "Amerikan Futbolu"
        Sport.BASEBALL -> "Beyzbol"
        Sport.SOFTBALL -> "Softbol"
        Sport.CRICKET -> "Kriket"
        Sport.HOCKEY -> "Çim Hokeyi"
        Sport.ICE_HOCKEY -> "Buz Hokeyi"
        Sport.WATER_POLO -> "Su Topu"
        Sport.BEACH_VOLLEYBALL -> "Plaj Voleybolu"
        Sport.SQUASH -> "Squash"
        Sport.PICKLEBALL -> "Pickleball"
        Sport.BOXING -> "Boks"
        Sport.KICKBOXING -> "Kick Boks"
        Sport.WRESTLING -> "Güreş"
        Sport.JUDO -> "Judo"
        Sport.KARATE -> "Karate"
        Sport.TAEKWONDO -> "Tekvando"
        Sport.FENCING -> "Eskrim"
        Sport.ATHLETICS -> "Atletizm"
        Sport.GYMNASTICS -> "Jimnastik"
        Sport.WEIGHTLIFTING -> "Halter"
        Sport.CROSSFIT -> "CrossFit"
        Sport.DANCE -> "Dans"
        Sport.HIKING -> "Doğa Yürüyüşü"
        Sport.CLIMBING -> "Tırmanış"
        Sport.ORIENTEERING -> "Oryantiring"
        Sport.TRIATHLON -> "Triatlon"
        Sport.ROWING -> "Kürek"
        Sport.CANOEING -> "Kano"
        Sport.SAILING -> "Yelken"
        Sport.SURFING -> "Sörf"
        Sport.DIVING -> "Dalış"
        Sport.SKIING -> "Kayak"
        Sport.SNOWBOARDING -> "Snowboard"
        Sport.ICE_SKATING -> "Buz Pateni"
        Sport.SKATEBOARDING -> "Kaykay"
        Sport.ROLLER_SKATING -> "Paten"
        Sport.GOLF -> "Golf"
        Sport.ARCHERY -> "Okçuluk"
        Sport.EQUESTRIAN -> "Binicilik"
        Sport.BOWLING -> "Bowling"
        Sport.BILLIARDS -> "Bilardo"
        Sport.DARTS -> "Dart"
        Sport.CUSTOM -> "Özel / Diğer"
    }

val Sport.emoji: String
    get() = when (this) {
        Sport.FOOTBALL -> "⚽"
        Sport.TENNIS -> "🎾"
        Sport.PADEL -> "🏓"
        Sport.BASKETBALL -> "🏀"
        Sport.VOLLEYBALL -> "🏐"
        Sport.RUNNING -> "🏃"
        Sport.SWIMMING -> "🏊"
        Sport.TABLE_TENNIS -> "🏓"
        Sport.BADMINTON -> "🏸"
        Sport.FITNESS -> "🏋️"
        Sport.PILATES_YOGA -> "🧘"
        Sport.CYCLING -> "🚴"
        Sport.ESPORTS_FC -> "🎮"
        Sport.ESPORTS_VALORANT -> "🎯"
        Sport.ESPORTS_LOL -> "⚔️"
        Sport.ESPORTS_CS2 -> "💣"
        Sport.CHESS -> "♟️"
        Sport.HANDBALL -> "🤾"
        Sport.FUTSAL -> "⚽"
        Sport.RUGBY -> "🏉"
        Sport.AMERICAN_FOOTBALL -> "🏈"
        Sport.BASEBALL -> "⚾"
        Sport.SOFTBALL -> "🥎"
        Sport.CRICKET -> "🏏"
        Sport.HOCKEY -> "🏑"
        Sport.ICE_HOCKEY -> "🏒"
        Sport.WATER_POLO -> "🤽"
        Sport.BEACH_VOLLEYBALL -> "🏐"
        Sport.SQUASH -> "🎾"
        Sport.PICKLEBALL -> "🏓"
        Sport.BOXING -> "🥊"
        Sport.KICKBOXING -> "🥊"
        Sport.WRESTLING -> "🤼"
        Sport.JUDO -> "🥋"
        Sport.KARATE -> "🥋"
        Sport.TAEKWONDO -> "🥋"
        Sport.FENCING -> "🤺"
        Sport.ATHLETICS -> "🏃"
        Sport.GYMNASTICS -> "🤸"
        Sport.WEIGHTLIFTING -> "🏋️"
        Sport.CROSSFIT -> "🏋️"
        Sport.DANCE -> "💃"
        Sport.HIKING -> "🥾"
        Sport.CLIMBING -> "🧗"
        Sport.ORIENTEERING -> "🧭"
        Sport.TRIATHLON -> "🏊"
        Sport.ROWING -> "🚣"
        Sport.CANOEING -> "🛶"
        Sport.SAILING -> "⛵"
        Sport.SURFING -> "🏄"
        Sport.DIVING -> "🤿"
        Sport.SKIING -> "🎿"
        Sport.SNOWBOARDING -> "🏂"
        Sport.ICE_SKATING -> "⛸️"
        Sport.SKATEBOARDING -> "🛹"
        Sport.ROLLER_SKATING -> "🛼"
        Sport.GOLF -> "⛳"
        Sport.ARCHERY -> "🏹"
        Sport.EQUESTRIAN -> "🏇"
        Sport.BOWLING -> "🎳"
        Sport.BILLIARDS -> "🎱"
        Sport.DARTS -> "🎯"
        Sport.CUSTOM -> "✨"
    }

val Sport.isTeamSport: Boolean
    get() = when (this) {
        Sport.FOOTBALL, Sport.BASKETBALL, Sport.VOLLEYBALL, Sport.HANDBALL, Sport.FUTSAL,
        Sport.RUGBY, Sport.AMERICAN_FOOTBALL, Sport.BASEBALL, Sport.SOFTBALL, Sport.CRICKET,
        Sport.HOCKEY, Sport.ICE_HOCKEY, Sport.WATER_POLO, Sport.BEACH_VOLLEYBALL,
        Sport.ESPORTS_VALORANT, Sport.ESPORTS_LOL, Sport.ESPORTS_CS2 -> true
        else -> false
    }

val Sport.isESport: Boolean
    get() = when (this) {
        Sport.ESPORTS_FC, Sport.ESPORTS_VALORANT, Sport.ESPORTS_LOL, Sport.ESPORTS_CS2, Sport.CHESS -> true
        else -> false
    }

enum class SportCategory(val label: String, val emoji: String) {
    PHYSICAL("Fiziksel Sporlar", "🏃‍♂️"),
    ESPORTS("E-Spor & Zeka", "🎮"),
    CUSTOM("Özel / Diğer", "✨")
}

/** Discovery audience for event feeds. Keep this explicit so network filters map to backend data. */
enum class EventNetworkScope {
    CITY,
    CAMPUS,
    COMPANY,
    TRAVEL
}

enum class EventListingType {
    OPEN_MATCH,
    SEEK_PLAYERS
}

val EventNetworkScope.label: String
    get() = when (this) {
        EventNetworkScope.CITY -> "Şehir"
        EventNetworkScope.CAMPUS -> "Kampüs"
        EventNetworkScope.COMPANY -> "Şirket"
        EventNetworkScope.TRAVEL -> "Seyahat"
    }

data class EventGeoPoint(
    val latitude: Double,
    val longitude: Double
)

val Sport.category: SportCategory
    get() = when (this) {
        Sport.ESPORTS_FC, Sport.ESPORTS_VALORANT, Sport.ESPORTS_LOL,
        Sport.ESPORTS_CS2, Sport.CHESS -> SportCategory.ESPORTS
        Sport.CUSTOM -> SportCategory.CUSTOM
        else -> SportCategory.PHYSICAL
    }

val physicalSports: List<Sport> = Sport.entries.filter { it.category == SportCategory.PHYSICAL }
val esportSports: List<Sport> = Sport.entries.filter { it.category == SportCategory.ESPORTS }

val EventStatus.label: String
    get() = when (this) {
        EventStatus.DRAFT -> "Taslak"
        EventStatus.PUBLISHED -> "Yayında"
        EventStatus.FILLING -> "Doluyor"
        EventStatus.FULL -> "Dolu"
        EventStatus.IN_PROGRESS -> "Oynanıyor"
        EventStatus.COMPLETED -> "Tamamlandı"
        EventStatus.VERIFIED -> "Doğrulandı"
        EventStatus.DISPUTED -> "İtirazda"
        EventStatus.ARCHIVED -> "Arşiv"
        EventStatus.CANCELLED -> "İptal"
    }

enum class PlayMode(val label: String, val emoji: String) {
    SOCIAL("Sosyal", "🤝"),
    COMPETITIVE("Rekabetçi", "🔥")
}

enum class SkillLevel(val label: String, val short: String) {
    BEGINNER("Başlangıç", "B"),
    INTERMEDIATE("Orta", "O"),
    ADVANCED("İleri", "İ"),
    PRO("Pro", "P")
}

enum class Tier(val label: String, val color: Color, val tint: Color, val minRating: Int) {
    BRONZE("Bronze", Color(0xFFB07040), Color(0xFFF8EDE4), 0),
    SILVER("Silver", Color(0xFF7D8799), Color(0xFFEFF1F5), 1100),
    GOLD("Gold", MColors.Gold, MColors.GoldTint, 1300),
    PLATINUM("Platinum", Color(0xFF3A9FA8), Color(0xFFE3F5F6), 1500),
    DIAMOND("Diamond", MColors.Sky, MColors.SkyTint, 1700),
    ELITE("Elite", MColors.Lilac, MColors.LilacTint, 1900);

    companion object {
        fun of(rating: Int): Tier = entries.last { rating >= it.minRating }
    }
}

enum class VerificationLevel(val label: String, val description: String, val emoji: String) {
    BASIC("Temel Doğrulama", "Telefon ve e-posta doğrulandı", "📱"),
    PHOTO("Fotoğraf Doğrulama", "Selfie + canlılık kontrolü ile profil fotoğrafı eşleşti", "🤳"),
    ID("Kimlik Doğrulama", "Opsiyonel yüksek güven: resmi kimlik kontrolü", "🪪"),
    ORGANIZER("Organizatör / Tesis", "İşletme veya organizasyon belgeleri onaylandı", "🏟️")
}

data class Player(
    val id: String,
    val name: String,
    val area: String,
    val sports: List<Sport>,
    val level: SkillLevel,
    val reliability: Int,
    val verifiedMatches: Int,
    val rating: Int,
    val chemistry: Int?,
    val verified: Boolean,
    val position: String?,
    val distanceKm: Float,
    val playedTogether: Int,
    val availableNow: Boolean,
    val color: Color
) {
    val initials: String get() = name.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
    val firstName: String get() = name.substringBefore(" ")
    val tier: Tier get() = Tier.of(rating)
}

data class ScoreBreakdown(
    val time: Int,
    val distance: Int,
    val level: Int,
    val reliability: Int,
    val preference: Int,
    val reasons: List<String>
) {
    // Belge §07 başlangıç ağırlıkları: zaman %30, mesafe %25, seviye %20, Reliability %15, tercih %10
    val total: Int get() = ((time * 30 + distance * 25 + level * 20 + reliability * 15 + preference * 10) / 100f).toInt()
}

data class SportEvent(
    val id: String,
    val title: String,
    val sport: Sport,
    val venue: String,
    val area: String,
    val day: String,
    val time: String,
    val startsInMin: Int?,
    val durationMin: Int,
    val distanceKm: Float?,
    val capacity: Int,
    val participantIds: List<String>,
    val waitlistCount: Int,
    val mode: PlayMode,
    val levelRange: String,
    val minReliability: Int,
    val verifiedOnly: Boolean,
    val pricePerPersonTry: Int,
    val commitmentTokens: Int = 10,
    val hostId: String,
    val status: EventStatus,
    val score: ScoreBreakdown,
    val neededRoles: List<String> = emptyList(),
    val emergency: Boolean = false,
    val cancellationDeadline: String = "Başlangıçtan 3 saat önce",
    val checkInWindow: String = "Başlangıçtan 30 dk önce açılır",
    val description: String = "",
    val boosted: Boolean = false,
    val circleOnly: Boolean = false,
    val isTeamEvent: Boolean = false,
    val customSportName: String? = null,
    val networkScope: EventNetworkScope = EventNetworkScope.CITY,
    val networkName: String? = null,
    val listingType: EventListingType = EventListingType.OPEN_MATCH,
    val format: String? = null,
    val gamePlatform: String? = null,
    val gameServer: String? = null,
    val voiceChannel: String? = null,
    val hostGameTag: String? = null,
    val approximateLocation: EventGeoPoint? = null,
    val city: String = "İstanbul",
    val startsAtEpochMillis: Long? = null
) {
    val freeSpots: Int get() = (capacity - participantIds.size).coerceAtLeast(0)
    val fillRatio: Float get() = if (capacity <= 0) 0f else (participantIds.size.toFloat() / capacity.toFloat()).coerceIn(0f, 1f)
    val isFull: Boolean get() = capacity <= 0 || participantIds.size >= capacity
    val displaySportLabel: String get() = if (sport == Sport.CUSTOM && !customSportName.isNullOrBlank()) customSportName else sport.label
    val displaySportEmoji: String get() = if (sport == Sport.CUSTOM) "✨" else sport.emoji
}

data class TeamProfile(
    val id: String,
    val name: String,
    val sport: Sport,
    val area: String,
    val badge: String,
    val color: Color,
    val captainId: String,
    val coCaptainId: String?,
    val memberIds: List<String>,
    val substituteIds: List<String>,
    val rating: Int,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val verifiedMatches: Int,
    val fairPlay: Float,
    val reliability: Int,
    val lookingForOpponent: Boolean,
    val availability: String,
    val isMine: Boolean = false,
    val description: String = "Kadıköy ve çevresinde haftalık düzenli maçlar yapan aktif takım.",
    val recruitingOpen: Boolean = true,
    val requiredMmr: Int = 1200,
    val achievements: List<String> = emptyList()
) {
    val tier: Tier get() = Tier.of(rating)
    val record: String get() = "$wins G · $draws B · $losses M"
}

enum class ChallengeStatus(val label: String) { PENDING("Bekliyor"), ACCEPTED("Kabul edildi"), DECLINED("Reddedildi"), SCHEDULED("Planlandı") }

enum class JoinRequestStatus(val label: String) {
    PENDING("Bekliyor"),
    APPROVED("Onaylandı"),
    REJECTED("Reddedildi")
}

data class JoinRequest(
    val id: String,
    val eventId: String,
    val player: Player,
    val requestedPosition: String? = null,
    val message: String? = null,
    val timestamp: String = "Şimdi",
    val status: JoinRequestStatus = JoinRequestStatus.PENDING
)

data class TeamJoinRequest(
    val id: String,
    val teamId: String,
    val player: Player,
    val preferredPosition: String,
    val message: String? = null,
    val timestamp: String = "Şimdi",
    val status: JoinRequestStatus = JoinRequestStatus.PENDING
)

data class ChallengeOffer(
    val id: String,
    val fromTeamId: String,
    val toTeamId: String,
    val proposedTime: String,
    val venue: String,
    val status: ChallengeStatus,
    val incoming: Boolean,
    val fitScore: Int,
    val stakeTokens: Int = 50,
    val note: String? = null
)

data class LeaderboardRow(
    val rank: Int,
    val playerId: String,
    val name: String,
    val rating: Int,
    val delta: Int,
    val verifiedMatches: Int,
    val isMe: Boolean = false
) {
    val tier: Tier get() = Tier.of(rating)
    val initials: String get() = name.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
}

data class BracketMatch(
    val round: Int,
    val teamA: String,
    val teamB: String,
    val scoreA: Int?,
    val scoreB: Int?
) {
    val winnerA: Boolean? get() = if (scoreA == null || scoreB == null) null else scoreA > scoreB
}

data class StandingRow(val team: String, val played: Int, val won: Int, val drawn: Int, val lost: Int, val goalDiff: Int, val points: Int)

data class TournamentInfo(
    val id: String,
    val name: String,
    val sport: Sport,
    val format: String,
    val participants: Int,
    val capacity: Int,
    val dates: String,
    val organizer: String,
    val requirement: String,
    val status: String,
    val feeNote: String,
    val bracket: List<BracketMatch>,
    val standings: List<StandingRow>,
    val isLeague: Boolean,
    val customSportName: String? = null,
    val creatorId: String? = null,
    val organizerLockTokens: Int = 0
) {
    val displaySportLabel: String get() = if (sport == Sport.CUSTOM && !customSportName.isNullOrBlank()) customSportName else sport.label
    val displaySportEmoji: String get() = if (sport == Sport.CUSTOM) "✨" else sport.emoji
}

enum class CommunityType(val label: String, val emoji: String) {
    CITY("Şehir", "🏙️"), CAMPUS("Kampüs", "🎓"), COMPANY("Şirket", "🏢"), CLUB("Kulüp", "🛡️")
}

enum class CommunityAccess(val label: String) { OPEN("Açık"), APPROVAL("Onaylı"), INVITE("Davetli") }

data class CommunityInfo(
    val id: String,
    val name: String,
    val type: CommunityType,
    val area: String,
    val members: Int,
    val access: CommunityAccess,
    val emoji: String,
    val upcomingEvents: Int,
    val description: String,
    val sports: List<Sport>
)

data class VenueSlotInfo(
    val id: String,
    val court: String,
    val day: String,
    val time: String,
    val durationMin: Int,
    val priceTry: Int,
    val available: Boolean
)

data class VenueInfo(
    val id: String,
    val name: String,
    val area: String,
    val sports: List<Sport>,
    val rating: Float,
    val reviews: Int,
    val verified: Boolean,
    val distanceKm: Float,
    val fromPriceTry: Int,
    val amenities: List<String>,
    val courts: Int,
    val cancellationPolicy: String,
    val slots: List<VenueSlotInfo>,
    val accent: Color
)

enum class ThreadType(val label: String) {
    EVENT("Etkinlik"), TEAM("Takım"), SQUAD("Squad"), BROADCAST("Duyuru"), DIRECT("Direkt")
}

enum class MemorySceneTheme(val label: String, val topColor: Color, val bottomColor: Color, val accentColor: Color) {
    NIGHT_TURF("Gece Sahası", Color(0xFF0D332B), Color(0xFF081B18), Color(0xFF4ADE80)),
    SUNSET_TENNIS("Kort Gün Batımı", Color(0xFF6B2D1E), Color(0xFF231524), Color(0xFFFFB37B)),
    ARENA_HOOPS("Salon Parkesi", Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF38BDF8)),
    PADEL_GLASS("Cam Kort", Color(0xFF1A365D), Color(0xFF0B192C), Color(0xFF60A5FA)),
    BEACH_VOLLEY("Sahil Voleybolu", Color(0xFF4C3319), Color(0xFF1F1610), Color(0xFFFBBF24)),
    TROPHY_MOMENT("Zafer Karesi", Color(0xFF31204A), Color(0xFF141122), Color(0xFFC084FC))
}

data class MatchMemoryPost(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorInitials: String,
    val sport: Sport,
    val venue: String,
    val area: String,
    val city: String,
    val distanceKm: Float,
    val caption: String,
    val matchScoreTag: String? = null,
    val timeAgo: String,
    val likes: Int,
    val likedByMe: Boolean = false,
    val sceneTheme: MemorySceneTheme = MemorySceneTheme.NIGHT_TURF,
    val customBitmap: ImageBitmap? = null
)

data class ChatMsg(
    val id: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val time: String,
    val isMine: Boolean = false,
    val isSystem: Boolean = false,
    val attachedMemory: MatchMemoryPost? = null
)

data class ChatThread(
    val id: String,
    val type: ThreadType,
    val title: String,
    val emoji: String,
    val memberCount: Int,
    val messages: List<ChatMsg>,
    val unread: Int,
    val locked: Boolean = false,
    val relatedEventId: String? = null,
    val muted: Boolean = false
) {
    val lastMessage: ChatMsg? get() = messages.lastOrNull()
}

enum class NotifPriority(val label: String, val color: Color, val tint: Color) {
    P0("Güvenlik & Ödeme", MColors.Rose, MColors.RoseTint),
    P1("Maç & Acil", MColors.Brand, MColors.BrandTint),
    P2("Challenge & Waitlist", MColors.Lilac, MColors.LilacTint),
    P3("Sosyal & Rozet", MColors.Sky, MColors.SkyTint),
    P4("Kampanya", MColors.Ink500, MColors.SurfaceMuted)
}

data class AppNotification(
    val id: String,
    val priority: NotifPriority,
    val emoji: String,
    val title: String,
    val body: String,
    val timeAgo: String,
    val read: Boolean = false,
    val eventId: String? = null,
    val expiresIn: String? = null
)

enum class LedgerType(val label: String, val color: Color, val tint: Color) {
    PURCHASE("Satın Alma", MColors.Mint, MColors.MintTint),
    BONUS("Bonus", MColors.Lilac, MColors.LilacTint),
    HOLD("Taahhüt Kilidi", MColors.Amber, MColors.AmberTint),
    RELEASE("Kilit İadesi", MColors.Sky, MColors.SkyTint),
    SPEND("Harcama", MColors.Brand, MColors.BrandTint),
    REFUND("İade", MColors.Mint, MColors.MintTint),
    ADJUSTMENT("Destek Düzeltmesi", MColors.Ink500, MColors.SurfaceMuted)
}

data class LedgerEntry(
    val id: String,
    val type: LedgerType,
    val amount: Int,
    val description: String,
    val time: String,
    val ref: String? = null,
    val idempotencyKey: String,
    val channel: String = "Matchora"
)

enum class PaymentStatus(val label: String) { PAID("Ödendi"), REFUNDED("İade edildi"), PENDING("Bekliyor") }

data class VenuePayment(
    val id: String,
    val venue: String,
    val description: String,
    val amountTry: Int,
    val method: String,
    val time: String,
    val status: PaymentStatus
)

data class PaymentMethod(
    val id: String,
    val brand: String,
    val last4: String,
    val expiry: String,
    val holder: String,
    val isDefault: Boolean
)

data class TokenPack(
    val sku: String,
    val tokens: Int,
    val bonus: Int,
    val priceLabel: String,
    val tag: String? = null
)

data class ProPlan(
    val sku: String,
    val title: String,
    val priceLabel: String,
    val period: String,
    val note: String?
)

data class BadgeInfo(
    val id: String,
    val title: String,
    val emoji: String,
    val description: String,
    val earned: Boolean,
    val progress: Float
)

data class Mission(
    val id: String,
    val title: String,
    val description: String,
    val progress: Int,
    val target: Int,
    val rewardPoints: Int,
    val emoji: String
)

data class ReliabilityRecord(
    val title: String,
    val delta: Int,
    val time: String,
    val note: String
)

data class SportSkill(
    val sport: Sport,
    val level: SkillLevel,
    val rating: Int,
    val verifiedMatches: Int,
    val winRate: Int,
    val localRank: Int?,
    val position: String?
) {
    val tier: Tier get() = Tier.of(rating)
}

data class MatchHistoryItem(
    val id: String,
    val sport: Sport,
    val title: String,
    val date: String,
    val result: String,
    val won: Boolean?,
    val ratingDelta: Int,
    val verified: Boolean,
    val mode: PlayMode
)

data class AutoSquadInfo(
    val id: String,
    val name: String,
    val sport: Sport,
    val memberIds: List<String>,
    val fitScore: Int,
    val slot: String,
    val reasons: List<String>,
    val accepted: Boolean = false
)

data class UserProfile(
    val name: String,
    val city: String,
    val area: String,
    val bio: String,
    val birthYear: Int,
    val phone: String,
    val email: String,
    val skills: List<SportSkill>,
    val verifications: Set<VerificationLevel>,
    val reliability: Int,
    val fairPlay: Float,
    val verifiedMatches: Int,
    val streakWeeks: Int,
    val campus: String?,
    val company: String?,
    val dominantSide: String,
    val availability: List<String>
) {
    val initials: String get() = name.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
    val firstName: String get() = name.substringBefore(" ")
}

data class NotificationPrefs(
    val emergencyFill: Boolean = true,
    val upcomingMatch: Boolean = true,
    val challenges: Boolean = true,
    val waitlist: Boolean = true,
    val rematch: Boolean = true,
    val social: Boolean = true,
    val weeklyDigest: Boolean = true,
    val marketing: Boolean = false,
    val quietHours: Boolean = true,
    val quietFrom: String = "23:00",
    val quietTo: String = "08:00",
    val dailyCap: Int = 6,
    val emergencyRadiusKm: Int = 3
)

data class PrivacyPrefs(
    val showReliability: Boolean = true,
    val showRatingHistory: Boolean = true,
    val showInRankings: Boolean = true,
    val dmPolicy: String = "Ortak etkinlik sonrası",
    val showActivityHeatmap: Boolean = true,
    val approximateAreaOnly: Boolean = true,
    val chemistrySignals: Boolean = true
)

data class Toast(val message: String, val emoji: String = "✓", val id: Long)
