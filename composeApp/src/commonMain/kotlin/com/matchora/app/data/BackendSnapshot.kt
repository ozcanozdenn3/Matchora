package com.matchora.app.data

import androidx.compose.ui.graphics.Color
import com.matchora.app.domain.EventStatus
import com.matchora.app.domain.Sport
import com.matchora.app.state.*
import kotlinx.serialization.json.*

internal fun JsonObject.s(key: String, default: String = ""): String = (get(key) as? JsonPrimitive)?.contentOrNull ?: default
internal fun JsonObject.i(key: String, default: Int = 0): Int = (get(key) as? JsonPrimitive)?.intOrNull ?: default
internal fun JsonObject.l(key: String, default: Long = 0): Long = (get(key) as? JsonPrimitive)?.longOrNull ?: default
internal fun JsonObject.f(key: String, default: Float = 0f): Float = (get(key) as? JsonPrimitive)?.floatOrNull ?: default
internal fun JsonObject.b(key: String, default: Boolean = false): Boolean = (get(key) as? JsonPrimitive)?.booleanOrNull ?: default
internal fun JsonObject.o(key: String): JsonObject = (get(key) as? JsonObject) ?: JsonObject(emptyMap())
internal fun JsonObject.a(key: String): List<JsonObject> = (get(key) as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()
internal fun JsonObject.ss(key: String): List<String> = (get(key) as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.orEmpty()
private inline fun <reified T : Enum<T>> enumValue(value: String, fallback: T): T = enumValues<T>().firstOrNull { it.name == value } ?: fallback
private fun JsonObject.payload(): JsonObject = runCatching { Json.parseToJsonElement(s("payload")).jsonObject }.getOrElse { JsonObject(emptyMap()) }

internal fun relativeTimeLabel(stamp: Long, now: Long): String {
    val minutes = ((now - stamp).coerceAtLeast(0) / 60000)
    return when { stamp <= 0 -> ""; minutes < 1 -> "Az önce"; minutes < 60 -> "$minutes dk önce"; minutes < 1440 -> "${minutes / 60} sa önce"; else -> "${minutes / 1440} gün önce" }
}

data class BlockedPlayerEntry(val userId: String, val name: String)

fun blockedPlayerEntries(wire: JsonObject): List<BlockedPlayerEntry> {
    val names = wire.a("players").associate { it.s("id") to it.s("name") }
    return wire.ss("blockedUserIds").distinct().map { id ->
        BlockedPlayerEntry(id, names[id]?.takeIf { it.isNotBlank() } ?: "Sporcu · ${id.take(8)}")
    }
}

private fun JsonObject.geoPoint(key: String): EventGeoPoint? {
    val point = o(key)
    val latitude = (point["latitude"] as? JsonPrimitive)?.doubleOrNull ?: return null
    val longitude = (point["longitude"] as? JsonPrimitive)?.doubleOrNull ?: return null
    return if (latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0) EventGeoPoint(latitude, longitude) else null
}

/** UI models remain independent of database tables and transport field naming. */
class BackendSnapshot(val wire: JsonObject) {
    val userId = wire.s("userId")
    private fun alias(value: String) = if (value == userId && userId.isNotBlank()) DemoData.ME else value
    val profile: UserProfile? = wire["profile"]?.takeIf { it is JsonObject }?.jsonObject?.let { p ->
        UserProfile(p.s("name"), p.s("city"), p.s("area"), p.s("bio"), p.i("birthYear"), p.s("phone"), p.s("email"),
            p.a("skills").map { skill(it) }, p.ss("verifications").mapNotNull { v -> VerificationLevel.entries.firstOrNull { it.name == v } }.toSet(),
            p.i("reliability"), p.f("fairPlay"), p.i("verifiedMatches"), p.i("streakWeeks"), p.s("campus").ifBlank { null }, p.s("company").ifBlank { null }, p.s("dominantSide"), p.ss("availability"), jerseyNumber = (p["jerseyNumber"] as? JsonPrimitive)?.intOrNull, version = p.i("version"))
    }
    val avatarUploadId = wire.o("profile").s("avatarUploadId").ifBlank { null }
    private fun skill(p: JsonObject) = SportSkill(enumValue(p.s("sport"), Sport.FOOTBALL), enumValue(p.s("level"), SkillLevel.INTERMEDIATE), p.i("rating", 1000), p.i("verifiedMatches"), p.i("winRate"), (p["localRank"] as? JsonPrimitive)?.intOrNull, p.s("position").ifBlank { null })
    val players = wire.a("players").map { p ->
        val skills = p.a("skills"); val first = skills.firstOrNull() ?: JsonObject(emptyMap())
        Player(alias(p.s("id")), p.s("name"), p.s("area"), skills.map { enumValue(it.s("sport"), Sport.FOOTBALL) }, enumValue(first.s("level"), SkillLevel.INTERMEDIATE), p.i("reliability"), p.i("verifiedMatches"), first.i("rating", 1000), null, "PHOTO" in p.ss("verifications"), first.s("position"), null, 0, false, Color(0xFFFF4D00), reliabilityVisible = p.b("reliabilityVisible", false), profileAvailable = skills.isNotEmpty(), bio = p.s("bio"), dominantSide = p.s("dominantSide"), availability = p.ss("availability"), campus = p.s("campus").ifBlank { null }, company = p.s("company").ifBlank { null }, jerseyNumber = (p["jerseyNumber"] as? JsonPrimitive)?.intOrNull, fairPlay = p.f("fairPlay"), city = canonicalTurkishCityName(p.s("city", "İstanbul").ifBlank { "İstanbul" }))
    }
    private fun player(id: String) = players.firstOrNull { it.id == alias(id) } ?: Player(alias(id), "Sporcu", "", emptyList(), SkillLevel.INTERMEDIATE, 0, 0, 1000, null, false, "", null, 0, false, Color(0xFFFF4D00), reliabilityVisible = false, profileAvailable = false)
    val events = wire.a("events").map { e ->
        SportEvent(id = e.s("id"), title = e.s("title"), sport = enumValue(e.s("sport"), Sport.FOOTBALL), venue = e.s("venue"), area = e.s("area"), day = e.s("day"), time = e.s("time"), startsInMin = e.i("startsInMin"), durationMin = e.i("durationMin", 60), distanceKm = null, capacity = e.i("capacity", 2), participantIds = e.ss("participantIds").map(::alias), waitlistCount = e.i("waitlistCount"), mode = enumValue(e.s("mode"), PlayMode.SOCIAL), levelRange = e.s("levelRange"), minReliability = e.i("minReliability"), verifiedOnly = e.b("verifiedOnly"), pricePerPersonTry = e.i("pricePerPersonTry"), commitmentTokens = e.i("commitmentTokens", 10), hostId = alias(e.s("hostId")), status = enumValue(e.s("status"), EventStatus.PUBLISHED), score = e.o("score").let { score -> ScoreBreakdown(score.i("time"), score.i("distance"), score.i("level"), score.i("reliability"), score.i("preference"), score.ss("reasons")) }, description = e.s("description"), emergency = e.b("emergency"), isTeamEvent = e.b("isTeamEvent"), homeParticipantIds = e.ss("homeParticipantIds").map(::alias), awayParticipantIds = e.ss("awayParticipantIds").map(::alias), neededRoles = e.ss("neededRoles"), customSportName = e.s("customSportName").ifBlank { null }, networkScope = enumValue(e.s("networkScope"), EventNetworkScope.CITY), networkName = e.s("networkName").ifBlank { null }, communityId = e.s("communityId").ifBlank { null }, approvalRequired = e.b("approvalRequired", e.s("listingType") == "SEEK_PLAYERS"), listingType = enumValue(e.s("listingType"), EventListingType.OPEN_MATCH), format = e.s("format").ifBlank { null }, gamePlatform = e.s("gamePlatform").ifBlank { null }, gameServer = e.s("gameServer").ifBlank { null }, voiceChannel = e.s("voiceChannel").ifBlank { null }, hostGameTag = e.s("hostGameTag").ifBlank { null }, city = e.s("city", "İstanbul"), startsAtEpochMillis = e.l("startsAtEpochMillis"), approximateLocation = e.geoPoint("approximateLocation"), meetingLocation = e.geoPoint("meetingLocation"), checkedInParticipantIds = e.ss("checkedInParticipantIds").map(::alias), version = e.i("version", 1))
    }
    fun eventVersion(id: String) = wire.a("events").firstOrNull { it.s("id") == id }?.i("version", 1) ?: 1
    val joinedIds = wire.ss("joinedEventIds").toSet()
    val checkedInIds = wire.ss("checkedInEventIds").toSet()
    val waitlistedIds = wire.a("waitlist").map { it.s("event_id") }.toSet()
    fun waitlistAction(eventId: String) = if (eventId in waitlistedIds) "leave" else "join"
    val balance = wire.o("wallet").i("balance")
    val locked = wire.o("wallet").i("locked")
    val ledger = wire.a("ledger")
        .filterNot { it.s("ref") in setOf("welcome", "passport_complete") }
        .map { LedgerEntry(it.s("id"), enumValue(it.s("type"), LedgerType.ADJUSTMENT), it.i("amount"), it.s("description"), it.s("time"), it.s("ref"), it.s("idempotencyKey")) }
    val teams = wire.a("teams").map { t -> TeamProfile(t.s("id"), t.s("name"), enumValue(t.s("sport"), Sport.FOOTBALL), t.s("area"), t.s("badge", "🛡️"), t.s("colorHex").removePrefix("#").toLongOrNull(16)?.let { Color(it) } ?: Color(0xFFFF4D00), alias(t.s("captainId")), t.s("coCaptainId").ifBlank { null }?.let(::alias), t.ss("memberIds").map(::alias), t.ss("substituteIds").map(::alias), t.i("rating", 1000), t.i("won"), t.i("lost"), t.i("drawn"), t.i("played"), t.f("fairPlay", 4.8f), t.i("reliability"), true, t.s("availability"), t.b("isMine"), t.s("description"), t.b("recruitingOpen", true), t.i("requiredMmr"), t.ss("achievements"), city = t.s("city"), homeVenue = t.s("homeVenue"), networkScope = enumValue(t.s("networkScope"), EventNetworkScope.CITY), version = t.i("version", 1)) }
    val tournaments = wire.a("tournaments").map { t -> TournamentInfo(t.s("id"), t.s("name"), enumValue(t.s("sport"), Sport.FOOTBALL), t.s("format"), t.i("participants"), t.i("capacity"), t.s("dates"), t.s("organizerName").ifBlank { players.firstOrNull { it.id == alias(t.s("creatorId")) }?.name ?: "Organizatör" }, t.s("requirement"), when (t.s("status")) { "open" -> "Kayıt açık"; "cancelled" -> "İptal edildi"; "completed" -> "Tamamlandı"; else -> "Devam ediyor" }, t.s("feeNote"), t.a("fixtures").map { f -> BracketMatch(f.i("round"), f.s("homeName").ifBlank { teams.firstOrNull { it.id == f.s("home_team") }?.name ?: "Takım" }, f.s("awayName").ifBlank { teams.firstOrNull { it.id == f.s("away_team") }?.name ?: "Takım" }, (f["homeScore"] as? JsonPrimitive)?.intOrNull, (f["awayScore"] as? JsonPrimitive)?.intOrNull, eventId = f.s("event_id").ifBlank { null }, startsAtEpochMillis = f.l("startsAtEpochMillis").takeIf { it > 0 }, venue = f.s("venue"), status = f.s("event_status")) }, t.a("standings").map { StandingRow(it.s("team"), it.i("played"), it.i("won"), it.i("drawn"), it.i("lost"), it.i("goalDiff"), it.i("points")) }, t.b("isLeague"), customSportName = t.s("customSportName").ifBlank { null }, gamePlatform = t.s("gamePlatform"), serverRegion = t.s("serverRegion"), formatCode = t.s("formatCode", if (t.b("isLeague")) "ROUND_ROBIN" else "KNOCKOUT"), minRating = t.i("minRating"), verifiedOnly = t.b("verifiedOnly"), creatorId = alias(t.s("creatorId")), organizerLockTokens = if (t.s("status") in listOf("open", "running")) 20 else 0, rosterSize = t.i("rosterSize").takeIf { it > 0 }, registrations = t.a("registrations").map { r -> TournamentRegistration(r.s("teamId"), r.s("name"), alias(r.s("captainId")), r.ss("rosterIds").map(::alias), r.s("status", "joined")) }, championTeamId = t.s("championTeamId").ifBlank { null }, city = t.s("city"), area = t.s("area"), networkScope = enumValue(t.s("networkScope"), EventNetworkScope.CITY), networkName = t.s("networkName").ifBlank { null }, communityId = t.s("communityId").ifBlank { null }, venue = t.s("venue"), prizeToken = t.s("prizeToken"), prizeCash = t.s("prizeCash"), version = t.i("version", 1)) }
    private val serverNow = wire.l("serverTime", kotlinx.datetime.Clock.System.now().toEpochMilliseconds())
    val joinRequests = wire.a("joinRequests").map { r -> JoinRequest(r.s("id"), r.s("scope_id"), player(r.s("user_id")), r.payload().s("position"), r.payload().s("note"), timestamp = relativeTimeLabel(r.l("created_at"), serverNow).ifBlank { "Şimdi" }, status = when (r.s("status")) { "pending" -> JoinRequestStatus.PENDING; "approved" -> JoinRequestStatus.APPROVED; "cancelled" -> JoinRequestStatus.CANCELLED; else -> JoinRequestStatus.REJECTED }) }
    val teamRequests = wire.a("teamJoinRequests").map { r -> TeamJoinRequest(r.s("id"), r.s("scope_id"), player(r.s("user_id")), r.payload().s("position"), r.payload().s("note"), timestamp = relativeTimeLabel(r.l("created_at"), serverNow).ifBlank { "Şimdi" }, status = when (r.s("status")) { "pending" -> JoinRequestStatus.PENDING; "approved" -> JoinRequestStatus.APPROVED; "cancelled" -> JoinRequestStatus.CANCELLED; else -> JoinRequestStatus.REJECTED }) }
    val challenges = wire.a("challenges").map { c -> val p = c.payload(); ChallengeOffer(c.s("id"), c.s("from_team"), c.s("to_team"), p.s("proposedTime"), p.s("venue"), enumValue(c.s("status"), ChallengeStatus.PENDING), teams.any { it.id == c.s("to_team") && it.isMine }, 0, p.i("stakeTokens"), p.s("note"), p.l("startsAtEpochMillis")) }
    val communities = wire.a("communities").map { c -> val primary = enumValue(c.s("sport"), Sport.FOOTBALL); val sports = c.ss("sports").map { enumValue(it, Sport.FOOTBALL) }.ifEmpty { listOf(primary) }; CommunityInfo(c.s("id"), c.s("name"), enumValue(c.s("type"), CommunityType.CITY), c.s("area"), c.i("memberCount"), enumValue(c.s("access"), CommunityAccess.OPEN), "🤝", c.i("upcomingEvents"), c.s("description"), sports, city = c.s("city"), ownerId = alias(c.s("ownerId")).ifBlank { null }, version = c.i("version", 1)) }
    val venues = wire.a("venues").map { v -> val slots = v.a("slots").map { s -> VenueSlotInfo(s.s("id"), s.s("court"), s.s("day"), s.s("time"), s.i("durationMin", 60), s.i("priceTry"), s.b("available"), s.i("priceMinor", s.i("priceTry") * 100)) }; VenueInfo(v.s("id"), v.s("name"), v.s("area"), v.ss("sports").map { enumValue(it, Sport.FOOTBALL) }, v.f("rating"), v.i("reviews"), v.b("verified"), (v["distanceKm"] as? JsonPrimitive)?.floatOrNull, slots.minOfOrNull { it.priceTry } ?: 0, v.ss("amenities"), v.i("courts", 1), "Başlangıçtan ${v.i("cancellationHours", 24)} saat önce", slots, Color(0xFF0FA968), address = v.s("address"), approximateLocation = v.geoPoint("approximateLocation"), city = v.s("city", "İstanbul"), phone = v.s("phone"), description = v.s("description"), photoUploadIds = v.ss("photoUploadIds"), ownerId = v.s("ownerId")) }
    val venuePayments = wire.a("venuePayments").map { VenuePayment(it.s("id"), it.s("venue"), it.s("description"), it.i("amountTry"), it.s("method"), it.s("time"), enumValue(it.s("status"), PaymentStatus.PENDING), it.s("bookingKey"), bookingStatus = it.s("bookingStatus", "pending"), amountMinor = it.i("amountMinor", it.i("amountTry") * 100)) }
    val bookedSlotIds = venuePayments.filter { it.status !in setOf(PaymentStatus.CANCELLED, PaymentStatus.REFUNDED) }.mapNotNull { it.bookingKey }.toSet()
    val memories = wire.a("memories").map { m -> MatchMemoryPost(m.s("id"), alias(m.s("authorId")), m.s("authorName"), m.s("authorName").split(' ').mapNotNull { it.firstOrNull() }.take(2).joinToString(""), enumValue(m.s("sport"), Sport.FOOTBALL), m.s("venue"), m.s("area"), m.s("city"), (m["distanceKm"] as? JsonPrimitive)?.floatOrNull, m.s("caption"), matchScoreTag = m.s("matchScoreTag").ifBlank { null }, timeAgo = relativeTimeLabel(m.l("createdAt"), serverNow), likes = m.i("likes"), likedByMe = m.b("likedByMe"), sceneTheme = enumValue(m.s("sceneTheme"), MemorySceneTheme.NIGHT_TURF), remoteUploadId = m.s("uploadId").ifBlank { null }, status = m.s("status", "approved"), version = m.i("version", 1), approximateLocation = m.o("approximateLocation").takeIf { it.isNotEmpty() }?.let { EventGeoPoint(it["latitude"]!!.jsonPrimitive.double, it["longitude"]!!.jsonPrimitive.double) }, authorAvatarUploadId = m.s("authorAvatarUploadId").ifBlank { null }, comments = m.a("comments").map { c -> MemoryComment(c.s("id"), alias(c.s("authorId")), c.s("authorName"), c.s("text"), relativeTimeLabel(c.l("createdAt"), serverNow).ifBlank { "Az önce" }) }) }
    val threads = wire.a("threads").map { t -> ChatThread(t.s("id"), enumValue(t.s("type"), ThreadType.DIRECT), t.s("title"), "💬", t.i("memberCount"), t.a("messages").map { m -> val meta = m.o("memoryMeta"); val reply = m.o("reply"); ChatMsg(m.s("id"), alias(m.s("senderId")), m.s("senderName"), m.s("text"), m.s("time"), m.b("isMine"), attachedMemory = m.s("uploadId").takeIf { it.isNotBlank() }?.let { upload -> MatchMemoryPost(meta.s("id").ifBlank { m.s("id") }, alias(m.s("senderId")), m.s("senderName"), m.s("senderName").split(' ').mapNotNull { it.firstOrNull() }.take(2).joinToString(""), enumValue(meta.s("sport"), Sport.FOOTBALL), meta.s("venue"), meta.s("area"), meta.s("city"), (meta["distanceKm"] as? JsonPrimitive)?.floatOrNull, m.s("text"), matchScoreTag = meta.s("matchScoreTag").ifBlank { null }, timeAgo = m.s("time"), likes = 0, sceneTheme = enumValue(meta.s("sceneTheme"), MemorySceneTheme.NIGHT_TURF), remoteUploadId = upload, isShowcase = meta.b("isShowcase", false)) }, version = m.i("version", 1), createdAt = m.l("createdAt"), edited = m.b("edited"), deleted = m.b("deleted"), readBy = m.i("readBy"), day = m.s("day"), replyToId = reply.s("id").ifBlank { null }, replyText = reply.s("text").ifBlank { null }, replySenderId = reply.s("senderId").ifBlank { null }?.let(::alias), replyDeleted = reply.b("deleted"), replyUnavailable = reply.b("unavailable")) }, t.i("unread"), t.b("locked"), t.s("relatedEventId").ifBlank { null }, t.b("muted")) }
    val notifications = wire.a("notifications").map { n -> AppNotification(n.s("id"), enumValue(n.s("priority"), NotifPriority.P2), n.s("emoji", "🔔"), n.s("title"), n.s("body"), n.s("timeAgo"), n.b("read"), if (n.s("destination") == "EVENT") n.s("targetId") else null, destination = enumValue(n.s("destination"), NotificationDestination.PASSPORT), targetId = n.s("targetId"), category = n.s("category").ifBlank { null }) }
    val support = wire.a("supportRequests").map { SupportRequest(it.s("id"), it.s("subject"), it.s("message"), it.s("category"), it.s("status", "open"), it.s("response").ifBlank { null }) }
    val reports = wire.a("reports").map { ModerationReport(it.s("id"), it.s("target"), it.s("reason"), it.s("details"), it.s("status", "open")) }
    val results = wire.a("results").associate { r ->
        val payload = r.payload()
        val event = events.firstOrNull { it.id == r.s("event_id") }
        val sides = payload.o("sides")
        r.s("event_id") to MatchResultDraft(
            r.s("event_id"), payload.i("homeScore"), payload.i("awayScore"), payload.s("note"), 5,
            r.s("status") == "disputed", status = r.s("status"), submittedBy = alias(r.s("submitted_by")),
            disputeUntil = r.l("dispute_until").takeIf { it > 0 },
            homeParticipantIds = if ("home" in sides) sides.ss("home").map(::alias) else event?.homeParticipantIds.orEmpty(),
            awayParticipantIds = if ("away" in sides) sides.ss("away").map(::alias) else event?.awayParticipantIds.orEmpty()
        )
    }
    val badges = wire.o("progression").a("badges").map { BadgeInfo(it.s("id"), it.s("title"), it.s("emoji"), it.s("description"), it.b("earned"), it.f("progress")) }
    val missions = wire.o("progression").a("missions").map { Mission(it.s("id"), it.s("title"), it.s("description"), it.i("progress"), it.i("goal"), it.i("rewardTokens"), it.s("emoji"), it.b("claimed")) }
    val history = if (wire.containsKey("matchHistory")) wire.a("matchHistory").map { m -> MatchHistoryItem(m.s("id"), enumValue(m.s("sport"), Sport.FOOTBALL), m.s("title"), m.s("date"), m.s("score"), (m["won"] as? JsonPrimitive)?.booleanOrNull, m.i("mmrDelta"), m.b("verified"), enumValue(m.s("mode"), PlayMode.SOCIAL)) } else wire.a("results").filter { it.s("status") == "verified" }.mapNotNull { r ->
        val event = events.firstOrNull { it.id == r.s("event_id") } ?: return@mapNotNull null
        val rating = wire.a("ratingHistory").firstOrNull { it.s("event_id") == event.id }
        val score = r.payload()
        MatchHistoryItem(event.id, event.sport, event.title, event.day, "${score.i("homeScore")} – ${score.i("awayScore")}", null, rating?.i("delta") ?: 0, true, event.mode)
    }
    val reliabilityHistory = wire.a("reliabilityHistory").map { ReliabilityRecord(it.s("reason"), it.i("delta"), it.s("date"), it.s("detail", "Doğrulanmış maç")) }
    val rankings = wire.a("rankings").map { r -> ApiRanking(enumValue(r.s("sport"), Sport.FOOTBALL), r.s("city"), r.s("area"), LeaderboardRow(r.i("rank"), alias(r.s("userId")), r.s("name"), r.i("rating"), r.i("delta"), r.i("verifiedMatches"), r.s("userId") == userId)) }
    val privacy: PrivacyPrefs = wire.o("privacy").let { p -> PrivacyPrefs(p.b("showReliability", true), p.b("showRatingHistory", true), p.b("showInRankings", true), p.s("dmPolicy", "Ortak etkinlik sonrası"), p.b("showActivityHeatmap", true), true, p.b("chemistrySignals", true)) }
    val notificationPrefs: NotificationPrefs = wire.o("notificationPrefs").let { p -> NotificationPrefs(p.b("emergencyFill", true), p.b("upcomingMatch", true), p.b("challenges", true), p.b("waitlist", true), p.b("rematch", true), p.b("social", true), p.b("weeklyDigest", true), p.b("marketing"), p.b("quietHours", true), p.s("quietFrom", "23:00"), p.s("quietTo", "08:00"), p.i("dailyCap", 6), p.i("emergencyRadiusKm", 3)) }
    val registeredTournamentIds = wire.ss("registeredTournamentIds").toSet()
    val invitedIds = wire.ss("invitedPlayerIds").map(::alias).toSet()
    val joinedCommunityIds = wire.ss("joinedCommunityIds").toSet()
    val requestedCommunityIds = wire.ss("requestedCommunityIds").toSet()
    val blockedThreadIds = wire.ss("blockedThreadIds").toSet()
    val nextThreadCursor: String? = wire.s("nextThreadCursor").ifBlank { null }
    val submittedFeedbackTargets: Set<Pair<String, String>> = wire.a("submittedFeedbackTargets").map { it.s("event_id") to alias(it.s("to_id")) }.toSet()
    val featureFlags: Map<String, Boolean> = wire.a("featureFlags").associate { it.s("name") to (it.i("enabled", 1) != 0 && it.b("enabled", true)) }
    val activePlan = wire.o("subscription").takeIf { it.s("status") == "active" }?.let { sub -> DemoData.proPlans.firstOrNull { it.sku == sub.s("product_id") } }
    fun otherUserForThread(threadId: String) = wire.a("threads").firstOrNull { it.s("id") == threadId }?.ss("memberIds")?.firstOrNull { it != userId }
}

fun UserProfile.toApiPayload(expectedVersion: Int?, avatarUploadId: String? = null) = apiPayload(
    "name" to name, "city" to city, "area" to area, "bio" to bio, "birthYear" to birthYear, "phone" to phone, "email" to email,
    "skills" to skills.map { mapOf("sport" to it.sport.name, "level" to it.level.name, "position" to it.position.orEmpty()) },
    "campus" to campus, "company" to company, "dominantSide" to dominantSide, "availability" to availability, "jerseyNumber" to jerseyNumber,
    "avatarUploadId" to if (avatarRemovalRequested) null else avatarUploadId, "expectedVersion" to expectedVersion)
fun SportEvent.toApiPayload() = apiPayload("title" to title, "sport" to sport, "venue" to venue, "city" to city, "area" to area,
    "capacity" to capacity, "startsAtEpochMillis" to startsAtEpochMillis, "durationMin" to durationMin, "mode" to mode, "levelRange" to levelRange,
    "minReliability" to minReliability, "verifiedOnly" to verifiedOnly, "description" to description, "neededRoles" to neededRoles, "customSportName" to customSportName,
    "approvalRequired" to approvalRequired, "networkScope" to networkScope, "networkName" to networkName, "communityId" to communityId, "listingType" to listingType, "format" to (format ?: "Grup"),
    "gamePlatform" to gamePlatform.orEmpty(), "gameServer" to gameServer.orEmpty(), "voiceChannel" to voiceChannel.orEmpty(), "hostGameTag" to hostGameTag.orEmpty(),
    "approximateLocation" to approximateLocation?.let { mapOf("latitude" to it.latitude, "longitude" to it.longitude) }, "pricePerPersonTry" to pricePerPersonTry, "emergency" to emergency)
fun PrivacyPrefs.toApiPayload() = apiPayload("showReliability" to showReliability, "showRatingHistory" to showRatingHistory, "showInRankings" to showInRankings, "dmPolicy" to dmPolicy, "showActivityHeatmap" to showActivityHeatmap, "approximateAreaOnly" to true, "chemistrySignals" to chemistrySignals)
fun NotificationPrefs.toApiPayload() = apiPayload("emergencyFill" to emergencyFill, "upcomingMatch" to upcomingMatch, "challenges" to challenges, "waitlist" to waitlist, "rematch" to rematch, "social" to social, "weeklyDigest" to weeklyDigest, "marketing" to marketing, "quietHours" to quietHours, "quietFrom" to quietFrom, "quietTo" to quietTo, "dailyCap" to dailyCap, "emergencyRadiusKm" to emergencyRadiusKm)

data class ApiRanking(val sport: Sport, val city: String, val area: String, val row: LeaderboardRow)
