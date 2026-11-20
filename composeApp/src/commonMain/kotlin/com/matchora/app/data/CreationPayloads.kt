package com.matchora.app.data

import androidx.compose.ui.graphics.toArgb
import com.matchora.app.state.CommunityInfo
import com.matchora.app.state.TeamProfile
import com.matchora.app.state.TournamentInfo

fun TeamProfile.toApiPayload() = apiPayload(
    "name" to name, "sport" to sport, "city" to city, "area" to area,
    "homeVenue" to homeVenue, "networkScope" to networkScope.name,
    "description" to description, "availability" to availability,
    "recruitingOpen" to recruitingOpen, "requiredMmr" to requiredMmr, "badge" to badge,
    "colorHex" to ("#" + color.toArgb().toUInt().toString(16).padStart(8, '0'))
)

fun TournamentInfo.toApiPayload() = apiPayload(
    "name" to name, "sport" to sport, "format" to format, "capacity" to capacity,
    "dates" to dates, "requirement" to requirement, "feeNote" to feeNote, "isLeague" to isLeague,
    "city" to city, "area" to area, "networkScope" to networkScope.name, "venue" to venue,
    "prizeToken" to prizeToken, "prizeCash" to prizeCash, "customSportName" to customSportName,
    "organizerName" to organizer, "gamePlatform" to gamePlatform, "serverRegion" to serverRegion,
    "formatCode" to formatCode, "minRating" to minRating, "verifiedOnly" to verifiedOnly,
    "rosterSize" to rosterSize
)

fun CommunityInfo.toApiPayload() = apiPayload(
    "name" to name, "type" to type, "access" to access, "city" to city, "area" to area,
    "description" to description, "sport" to sports.first(), "sports" to sports.map { it.name }
)
