package com.matchora.app.ui.components

import com.matchora.app.ui.components.premiumSurface
import com.matchora.app.data.*
import kotlinx.serialization.json.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.domain.EventStatus
import com.matchora.app.domain.Sport
import com.matchora.app.state.*
import com.matchora.app.ui.theme.MColors
import com.matchora.app.ui.theme.MGradients
import kotlin.random.Random

// ═══════════════════════════════════════════════════════════════════════════════
// PLAY NOW WIZARD & İLAN VERME ALANI (Strateji Dokümanı §04, §07, §28)
// 1) Hızlı Eşleş: Özel Tarih/Saat stepper ve filtreleri ile anında maç bul
// 2) İlan oluştur: Eksik oyuncu veya yeni karşılaşma ilanı oluştur
// ═══════════════════════════════════════════════════════════════════════════════

private fun formatTime(hour: Int, minute: Int): String {
    val hStr = if (hour < 10) "0$hour" else "$hour"
    val mStr = if (minute < 10) "0$minute" else "$minute"
    return "$hStr:$mStr"
}

private fun parseClockMinutes(value: String): Int? {
    val parts = value.split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

private enum class ListingIntent(val title: String) {
    NEW_MATCH("Yeni maç"),
    FIND_PLAYERS("Eksik oyuncu ara")
}

private inline fun <reified T : Enum<T>> draftEnum(value: String, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: fallback

private fun networkScopeLabel(scope: EventNetworkScope): String = when (scope) {
    EventNetworkScope.CITY -> "Saha"
    EventNetworkScope.CAMPUS -> "Kampüs"
    EventNetworkScope.COMPANY -> "Şirket"
    EventNetworkScope.TRAVEL -> "Seyahat"
}

private fun defaultListingTitle(
    area: String,
    city: String,
    sport: Sport,
    customSportName: String,
    initialVenue: String
): String {
    val place = area.trim().ifBlank { city.trim().ifBlank { "Merkez" } }
    return when {
        initialVenue.isNotBlank() -> "${initialVenue.trim()} · ${sport.label} Maçı"
        sport == Sport.CUSTOM && customSportName.isNotBlank() -> "$place ${customSportName.trim()} Maçı"
        else -> "$place ${sport.label} Maçı"
    }
}

private fun defaultVenueName(area: String, city: String): String =
    "${area.trim().ifBlank { city.trim().ifBlank { "Merkez" } }} Merkez Saha"

private fun defaultPositionForSport(sport: Sport): String =
    positionsForSport(sport).firstOrNull() ?: "Oyuncu"

@Composable
private fun ExpandableListingSectionToggle(
    title: String,
    summary: String,
    expanded: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, null, tint = MColors.BrandDeep, modifier = Modifier.size(17.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = title,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MColors.Ink900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (summary.isNotBlank()) {
                        Text(
                            text = summary,
                            fontSize = 11.sp,
                            color = MColors.Ink500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = MColors.Ink500,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayNowModalSheet(
    onDismiss: () -> Unit,
    onSelectEvent: (SportEvent) -> Unit,
    onQuickJoin: (SportEvent) -> Unit,
    onToggleWaitlist: (SportEvent) -> Unit = {},
    onCreateEvent: (SportEvent) -> Unit = {},
    allEvents: List<SportEvent>,
    tokenBalance: Int = 0,
    joinedEventIds: Set<String> = emptySet(),
    waitlistedEventIds: Set<String> = emptySet(),
    pendingRequestEventIds: Set<String> = emptySet(),
    currentArea: String = "",
    currentCity: String = "İstanbul",
    currentCampus: String? = null,
    currentCompany: String? = null,
    eligibleCommunities: List<CommunityInfo>? = null,
    initialNetworkScope: EventNetworkScope = EventNetworkScope.CITY,
    initialVenue: String = "",
    initialTime: String? = null,
    newEventId: () -> String = { "draft_${Random.nextLong().toString(16)}" },
    onRequestTokens: () -> Unit = {},
    draftOwnerKey: String = "preview",
    onPublishEvent: (suspend (SportEvent, String) -> Unit)? = null,
    onRecoveredPublication: (String, Boolean) -> Unit = { _, _ -> }
) {
    val backend = LocalBackendRepository.current
    val draftStore = remember(draftOwnerKey) { ListingDraftStore(draftOwnerKey) }
    val restored = remember(draftOwnerKey) { draftStore.restore() ?: apiPayload() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var publishing by remember(draftOwnerKey) { mutableStateOf(false) }
    var draftFinished by remember(draftOwnerKey) { mutableStateOf(false) }
    var showDraftClose by remember(draftOwnerKey) { mutableStateOf(false) }

    var adStep by remember(draftOwnerKey) { mutableIntStateOf(restored.i("step", if (initialVenue.isNotBlank()) 1 else 0).coerceIn(0, 2)) }
    var showSearchOptions by remember { mutableStateOf(false) }
    var activeTab by remember(draftOwnerKey) { mutableIntStateOf(if (restored.isNotEmpty()) 1 else if (initialVenue.isNotBlank()) 1 else 0) } // 0: Hızlı Eşleş, 1: İlan oluştur

    // ── Hızlı Eşleş Durumları ──
    var selectedSport by remember { mutableStateOf<Sport?>(Sport.FOOTBALL) }
    var searchCategory by remember { mutableStateOf<SportCategory?>(null) }
    var customDate by remember { mutableStateOf("Tümü") }
    var customHour by remember { mutableIntStateOf(19) }
    var customMinute by remember { mutableIntStateOf(0) }
    var selectedMode by remember { mutableStateOf<PlayMode?>(null) }
    var verifiedOnly by remember { mutableStateOf(false) }
    var maxDistanceKm by remember { mutableStateOf<Int?>(null) }

    val backendMode = LocalBackendRepository.current?.enabled == true
    val deviceLocation by DeviceLocationState.current.collectAsState()
    val regionCenter = rememberRegionCenter(currentCity, currentArea)

    // ── İlan Verme Durumları (Akıllı varsayılanlar ile dolu başlar) ──
    var adCategory by remember(draftOwnerKey) { mutableStateOf(draftEnum(restored.s("category"), SportCategory.PHYSICAL)) }
    var adSport by remember(draftOwnerKey) { mutableStateOf(draftEnum(restored.s("sport"), Sport.FOOTBALL)) }
    var customSportName by remember(draftOwnerKey) { mutableStateOf(restored.s("customSportName", "")) }
    var customIsGaming by remember(draftOwnerKey) { mutableStateOf(restored.b("customGaming", false)) }
    var adLocation by remember(draftOwnerKey) { mutableStateOf(restored.s("area", currentArea.ifBlank { currentCity })) }
    var adCity by remember(draftOwnerKey) { mutableStateOf(restored.s("city", currentCity)) }
    val initialDefaultTitle = remember(draftOwnerKey) {
        defaultListingTitle(currentArea, currentCity, Sport.FOOTBALL, "", initialVenue)
    }
    val initialDefaultVenue = remember(draftOwnerKey) {
        initialVenue.ifBlank { defaultVenueName(currentArea, currentCity) }
    }
    var adTitle by remember(draftOwnerKey) {
        mutableStateOf(restored.s("title").ifBlank { initialDefaultTitle })
    }
    var adTitleCustomized by remember(draftOwnerKey) {
        mutableStateOf(restored.s("title").isNotBlank() && restored.s("title") != initialDefaultTitle)
    }
    var adVenue by remember(draftOwnerKey) {
        mutableStateOf(restored.s("venue").ifBlank { initialDefaultVenue })
    }
    var adVenueCustomized by remember(draftOwnerKey) {
        mutableStateOf(restored.s("venue").isNotBlank() || initialVenue.isNotBlank())
    }
    var adPosition by remember(draftOwnerKey) {
        mutableStateOf(restored.s("role").ifBlank { defaultPositionForSport(draftEnum(restored.s("sport"), Sport.FOOTBALL)) })
    }
    var adPositionCustomized by remember(draftOwnerKey) {
        mutableStateOf(restored.s("role").isNotBlank())
    }
    var adCostPerPlayer by remember(draftOwnerKey) { mutableStateOf(restored.s("fee", "0").ifBlank { "0" }) }
    var adDescription by remember(draftOwnerKey) { mutableStateOf(restored.s("description", "")) }
    var adFormat by remember(draftOwnerKey) { mutableStateOf(restored.s("format", "7v7")) }
    var adLevelRange by remember(draftOwnerKey) { mutableStateOf(restored.s("levelRange", "Orta – İleri")) }
    var adDurationText by remember(draftOwnerKey) { mutableStateOf(restored.s("duration", "60")) }
    var adNetworkScope by remember(draftOwnerKey) { mutableStateOf(draftEnum(restored.s("networkScope"), initialNetworkScope)) }
    var adCommunityId by remember(draftOwnerKey) { mutableStateOf(restored.s("communityId").ifBlank { null }) }
    var adNetworkName by remember(draftOwnerKey) { mutableStateOf(restored.s("networkName")) }
    var adCalendarDate by remember(draftOwnerKey) { mutableStateOf(runCatching { LocalDate.parse(restored.s("date")) }.getOrElse { LocalDate.fromEpochDays(todayDate().toEpochDays() + 1) }) }
    val initialAutoPoint = remember(draftOwnerKey) {
        val gpsMatch = deviceLocation?.takeIf { it.city.isNullOrBlank() || matchesTurkishPlace(it.city, currentCity) }
        gpsMatch?.let { EventGeoPoint(it.latitude, it.longitude) }
            ?: regionCenter?.let { EventGeoPoint(it.latitude, it.longitude) }
            ?: cityMapCenter(currentCity)
    }
    var adPoint by remember(draftOwnerKey) {
        mutableStateOf(
            (restored["point"] as? JsonObject)?.let { point ->
                val latitude = point["latitude"]?.jsonPrimitive?.doubleOrNull
                val longitude = point["longitude"]?.jsonPrimitive?.doubleOrNull
                if (latitude != null && longitude != null) EventGeoPoint(latitude, longitude) else null
            } ?: initialAutoPoint
        )
    }
    var showAdMap by remember { mutableStateOf(false) }
    var showAdRegion by remember { mutableStateOf(false) }
    var showStep1Advanced by remember(draftOwnerKey) {
        mutableStateOf(
            restored.s("networkScope").let { it.isNotBlank() && it != EventNetworkScope.CITY.name } ||
                initialNetworkScope != EventNetworkScope.CITY ||
                restored.s("category").let { it.isNotBlank() && it != SportCategory.PHYSICAL.name } ||
                restored.s("description").isNotBlank()
        )
    }
    var showStep2Advanced by remember(draftOwnerKey) { mutableStateOf(false) }
    var showStep3Advanced by remember(draftOwnerKey) { mutableStateOf(restored.b("urgent", false)) }
    val parsedInitialHour = remember(initialTime) { initialTime?.substringBefore(':')?.trim()?.toIntOrNull() }
    val parsedInitialMinute = remember(initialTime) { initialTime?.substringAfter(':')?.substringBefore(' ')?.trim()?.toIntOrNull() }
    var adHour by remember(draftOwnerKey) { mutableIntStateOf(restored.i("hour", parsedInitialHour ?: 20).coerceIn(0, 23)) }
    var adMinute by remember(draftOwnerKey) { mutableIntStateOf(restored.i("minute", parsedInitialMinute ?: 30).coerceIn(0, 59)) }
    var adMode by remember(draftOwnerKey) { mutableStateOf(draftEnum(restored.s("mode"), PlayMode.SOCIAL)) }
    var listingIntent by remember(draftOwnerKey) { mutableStateOf(draftEnum(restored.s("intent"), ListingIntent.FIND_PLAYERS)) }
    var adVerifiedOnly by remember(draftOwnerKey) { mutableStateOf(restored.b("verifiedOnly", false)) }
    var adMinReliability by remember(draftOwnerKey) { mutableIntStateOf(restored.i("minReliability").coerceIn(0, 100)) }
    var adApprovalRequired by remember(draftOwnerKey) { mutableStateOf(restored.b("approvalRequired", true)) }
    var adFormError by remember { mutableStateOf<String?>(null) }

    // E-Spor & Akıl Oyunları Özelleştirmeleri
    var esPlatform by remember(draftOwnerKey) { mutableStateOf(restored.s("platform", "🖥️ PC")) }
    var esServer by remember(draftOwnerKey) { mutableStateOf(restored.s("server", "TR Sunucusu").ifBlank { "TR Sunucusu" }) }
    var esVoiceComm by remember(draftOwnerKey) { mutableStateOf(restored.s("voice", "")) }
    var esGamerTag by remember(draftOwnerKey) { mutableStateOf(restored.s("gamerTag", "")) }
    var esAdType by remember(draftOwnerKey) { mutableStateOf(restored.s("gamingFormat", "5v5")) }
    var esNeededRole by remember(draftOwnerKey) { mutableStateOf(restored.s("gamingRole", "Esnek Rol").ifBlank { "Esnek Rol" }) }
    var urgentPlayerFill by remember(draftOwnerKey) { mutableStateOf(restored.b("urgent", false)) }

    fun draftFields() = apiPayload(
        "step" to adStep, "title" to adTitle, "category" to adCategory, "sport" to adSport,
        "customSportName" to customSportName, "customGaming" to customIsGaming,
        "area" to adLocation, "venue" to adVenue, "role" to adPosition, "fee" to adCostPerPlayer,
        "description" to adDescription, "format" to adFormat, "levelRange" to adLevelRange,
        "duration" to adDurationText, "networkScope" to adNetworkScope, "communityId" to adCommunityId,
        "networkName" to adNetworkName, "date" to adCalendarDate.toString(), "city" to adCity,
        "point" to adPoint?.let { mapOf("latitude" to it.latitude, "longitude" to it.longitude) },
        "hour" to adHour, "minute" to adMinute, "mode" to adMode, "intent" to listingIntent,
        "verifiedOnly" to adVerifiedOnly, "minReliability" to adMinReliability,
        "approvalRequired" to adApprovalRequired, "platform" to esPlatform, "server" to esServer,
        "voice" to esVoiceComm, "gamerTag" to esGamerTag, "gamingFormat" to esAdType,
        "gamingRole" to esNeededRole, "urgent" to urgentPlayerFill,
        "gaming" to (adSport.isESport || adSport == Sport.CHESS || adSport == Sport.CUSTOM && customIsGaming),
        "startsAt" to eventStartMillis(adCalendarDate, adHour, adMinute)
    )
    val currentDraftFields = draftFields()
    val initialFields = remember(draftOwnerKey) { JsonObject(currentDraftFields - "step") }
    val hasDraft = restored.isNotEmpty() || JsonObject(currentDraftFields - "step") != initialFields
    fun persistDraft(): Boolean = try {
        draftStore.save(draftFields())
        true
    } catch (error: Exception) {
        adFormError = error.message ?: "İlan taslağı kaydedilemedi."
        false
    }
    LaunchedEffect(draftStore, currentDraftFields, hasDraft, draftFinished, publishing) {
        if (hasDraft && !draftFinished && !publishing) {
            delay(250)
            try { draftStore.save(currentDraftFields) }
            catch (error: Exception) { adFormError = error.message ?: "İlan taslağı kaydedilemedi." }
        }
    }
    key(draftStore) {
        val latestFields by rememberUpdatedState(currentDraftFields)
        val latestHasDraft by rememberUpdatedState(hasDraft)
        DisposableEffect(Unit) {
            onDispose { if (latestHasDraft && !draftFinished) runCatching { draftStore.save(latestFields) } }
        }
    }
    fun requestDismiss() {
        if (publishing) return
        if (hasDraft && !draftFinished) showDraftClose = true else onDismiss()
    }
    fun stepError(through: Int) = listingDraftError(draftFields(), through,
        eligibleCommunities?.filter { if (adNetworkScope == EventNetworkScope.CAMPUS) it.type == CommunityType.CAMPUS else it.type == CommunityType.COMPANY }?.map { it.id }?.toSet(), Clock.System.now().toEpochMilliseconds())
    fun selectStep(target: Int) {
        if (publishing) return
        if (target <= adStep) { adStep = target; adFormError = null; return }
        val error = stepError(target - 1)
        if (error == null) { adStep = target; adFormError = null }
        else { adStep = error.first; adFormError = error.second }
    }

    var locationError by remember { mutableStateOf<String?>(null) }
    var locationSettings by remember { mutableStateOf(false) }
    val requestSearchLocation = rememberLocationRequester { result ->
        when (result) {
            is LocationOutcome.Located -> { DeviceLocationState.current.value = result.value; locationError = null; locationSettings = false }
            is LocationOutcome.Failed -> { DeviceLocationState.current.value = null; locationError = result.message; locationSettings = result.settingsAvailable }
        }
    }
    val origin = deviceLocation ?: regionCenter
    fun effectiveDistanceKm(event: SportEvent): Float? =
        origin?.let { ref -> event.approximateLocation?.let { pt -> approximateDistanceKm(ref, pt.latitude, pt.longitude) } }
            ?: event.distanceKm
    val filteredEvents = remember(backendMode, currentCity, currentArea, origin, allEvents, searchCategory, selectedSport, selectedMode, verifiedOnly, maxDistanceKm, customDate, customHour, customMinute) {
        val selectedClock = customHour * 60 + customMinute
        allEvents.filter { event ->
            (!backendMode || matchesTurkishPlace(event.city, currentCity)) &&
            event.status in setOf(EventStatus.PUBLISHED, EventStatus.FILLING, EventStatus.FULL) &&
            (searchCategory == null || event.sport.category == searchCategory) &&
            (selectedSport == null || event.sport == selectedSport) &&
            (selectedMode == null || event.mode == selectedMode) &&
            (!verifiedOnly || event.verifiedOnly) &&
            (maxDistanceKm?.toFloat()?.let { limit -> effectiveDistanceKm(event)?.let { distance -> distance <= limit } == true } ?: true) &&
            event.matchesDayFilter(customDate) &&
            (customDate == "Tümü" || (parseClockMinutes(event.time)?.let { it >= selectedClock } ?: false))
        }.sortedWith(compareByDescending<SportEvent> { it.freeSpots > 0 }.thenByDescending { it.score.total })
    }

    ModalBottomSheet(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        scrimColor = androidx.compose.ui.graphics.Color(0xFF0B1220).copy(alpha = .48f),

        onDismissRequest = ::requestDismiss,
        containerColor = MColors.Canvas,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { state ->
            if (state == SheetValue.Hidden && publishing) false
            else if (state == SheetValue.Hidden && hasDraft && !draftFinished) { showDraftClose = true; false }
            else true
        }),
        dragHandle = { MPremiumSheetHandle() }
    ) {
        Box {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp)
                .then(if (publishing) Modifier.clearAndSetSemantics { } else Modifier),
            contentPadding = PaddingValues(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Başlık & İkili Sekme Seçici
            item {
                MSheetHeading(if (activeTab == 0) "Şimdi oyna" else "İlan oluştur",
                    if (activeTab == 0) "Sana uygun buluşmayı keşfet." else "Oyunu başlat. Ekibini tamamla.",
                    Icons.Rounded.Sports, ::requestDismiss)
                Spacer(Modifier.height(12.dp))
                MSegmentedControl(listOf("Maç bul", "İlan ver"), activeTab, { activeTab = it })
            }

            if (activeTab == 0) {
                // ═══════════════════════════════════════════════════════════════
                // SEKME 0: HIZLI EŞLEŞ (ÖZEL TARİH & SAAT SEÇİCİ İLE)
                // ═══════════════════════════════════════════════════════════════
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color.White,
                        shadowElevation = 4.dp,
                        border = BorderStroke(1.dp, Color(0xFFE6EBF2)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SportPickerField(selectedSport, { selectedSport = it; searchCategory = null })

                            Surface(
                                onClick = { showSearchOptions = !showSearchOptions },
                                shape = RoundedCornerShape(13.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Rounded.Tune, null, tint = MColors.BrandDeep, modifier = Modifier.size(17.dp))
                                        Text(
                                            text = if (showSearchOptions) "Filtreleri gizle" else "Zaman ve oyun tercihleri",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MColors.Ink900
                                        )
                                    }
                                    Icon(
                                        imageVector = if (showSearchOptions) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = MColors.Ink500,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            if (showSearchOptions) {
                                MFloatingDropdownBox(
                                    label = "En Fazla Uzaklık",
                                    selectedValue = maxDistanceKm,
                                    options = listOf(null, 5, 10, 25),
                                    optionLabel = { km -> km?.let { "≤ $it km" } ?: "Farketmez" },
                                    onSelect = { maxDistanceKm = it },
                                    icon = Icons.Rounded.NearMe
                                )
                                if (backendMode && maxDistanceKm != null && deviceLocation == null) {
                                    Text("Uzaklığa göre eşleşmek için cihaz konumunu paylaş. Konumu olmayan ilanlar bu filtrede gösterilmez.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
                                    TextButton(onClick = requestSearchLocation) { Text("Konumumu kullan") }
                                    locationError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MColors.Rose) }
                                    if (locationSettings) TextButton(onClick = ::openApplicationSettings) { Text("Cihaz ayarlarını aç") }
                                }

                                MFloatingDropdownBox(
                                    label = "Oyun Modu",
                                    selectedValue = selectedMode,
                                    options = listOf(null, PlayMode.SOCIAL, PlayMode.COMPETITIVE),
                                    optionLabel = { mode ->
                                        when (mode) {
                                            null -> "Tümü"
                                            PlayMode.SOCIAL -> "Sosyal"
                                            PlayMode.COMPETITIVE -> "Rekabetçi"
                                        }
                                    },
                                    onSelect = { selectedMode = it },
                                    icon = Icons.Rounded.EmojiEvents
                                )

                                MFloatingDropdownBox(
                                    label = "Katılımcı Filtresi",
                                    selectedValue = verifiedOnly,
                                    options = listOf(false, true),
                                    optionLabel = { if (it) "Yalnızca Doğrulanmış" else "Tüm Sporcular" },
                                    onSelect = { verifiedOnly = it },
                                    icon = Icons.Rounded.VerifiedUser
                                )

                                MFloatingDropdownBox(
                                    label = "Maç Günü",
                                    selectedValue = customDate,
                                    options = listOf("Tümü", "Bugün", "Yarın", "Cumartesi", "Pazar"),
                                    optionLabel = { it },
                                    onSelect = { customDate = it },
                                    icon = Icons.Rounded.CalendarMonth
                                )

                                val timeSlots = remember {
                                    (9..23).flatMap { h -> listOf(0, 15, 30, 45).map { m -> h to m } }
                                }
                                MFloatingDropdownBox(
                                    label = "Başlangıç Saati",
                                    selectedValue = customHour to customMinute,
                                    options = timeSlots,
                                    optionLabel = { (h, m) -> "${formatTime(h, m)} sonrası" },
                                    onSelect = { (h, m) ->
                                        customHour = h
                                        customMinute = m
                                    },
                                    icon = Icons.Rounded.Schedule
                                )
                            }
                        }
                    }
                }

                // Bulunan Maçlar
                item {
                    Text(
                        text = "UYGUN MAÇLAR (${filteredEvents.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MColors.Ink400,
                        letterSpacing = 1.sp
                    )
                }

                items(filteredEvents) { event ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 1.dp,
                        border = BorderStroke(1.dp, if (event.emergency) MColors.Brand else Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clickable { onSelectEvent(event) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(13.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(11.dp),
                                    color = MColors.BrandTint,
                                    shadowElevation = 2.dp,
                                    border = BorderStroke(1.dp, MColors.Brand.copy(alpha = 0.2f)),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(event.sport.emoji, fontSize = 20.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = event.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MColors.Ink900,
                                        maxLines = 1
                                    )
                                    val dist = effectiveDistanceKm(event)
                                    Text(
                                        text = "${event.venue}${dist?.let { " · $it km" }.orEmpty()} · ${event.time}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = MColors.Ink500
                                    )
                                }
                            }

                            val alreadyJoined = joinedEventIds.contains(event.id)
                            val alreadyPending = pendingRequestEventIds.contains(event.id)
                            val alreadyWaiting = waitlistedEventIds.contains(event.id)
                            val isHost = event.hostId == DemoData.ME
                            val canJoin = event.freeSpots > 0 && event.status in setOf(EventStatus.PUBLISHED, EventStatus.FILLING)
                            val canWaitlist = event.status == EventStatus.FULL
                            val canAct = !isHost && !alreadyJoined && !alreadyPending && (alreadyWaiting || canJoin || canWaitlist)
                            Button(
                                onClick = {
                                    when {
                                        alreadyWaiting || (!canJoin && canWaitlist) -> onToggleWaitlist(event)
                                        canJoin -> onQuickJoin(event)
                                        else -> onSelectEvent(event)
                                    }
                                },
                                enabled = canAct,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (canAct) MColors.Brand else MColors.SurfaceSunken,
                                    contentColor = if (canAct) Color.White else MColors.Ink500
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(
                                    when {
                                        isHost -> "İlanın"
                                        alreadyJoined -> "Katıldın"
                                        alreadyPending -> "Bekliyor"
                                        alreadyWaiting -> "Listeden çık"
                                        canWaitlist -> "Bekleme"
                                        event.approvalRequired -> "Başvur"
                                        canJoin -> "Katıl"
                                        else -> "Kapalı"
                                    },
                                    color = if (canAct) Color.White else MColors.Ink500,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (filteredEvents.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            shadowElevation = 1.dp,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Rounded.SearchOff, null, tint = MColors.Ink400, modifier = Modifier.size(28.dp))
                                Text("Uygun karşılaşma bulunamadı", fontWeight = FontWeight.Bold, color = MColors.Ink900)
                                Text("Filtreleri değiştirip yeniden deneyebilirsin.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
                                TextButton(onClick = {
                                    searchCategory = null
                                    selectedSport = null
                                    selectedMode = null
                                    verifiedOnly = false
                                    maxDistanceKm = null
                                    customDate = "Tümü"
                                }) { Text("Filtreleri temizle") }
                            }
                        }
                    }
                }
            } else {
                // ═══════════════════════════════════════════════════════════════
                // SEKME 1: 📢 İLAN VER / KADRO AÇ (KULLANICI İLANI)
                // ═══════════════════════════════════════════════════════════════
                item {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        shadowElevation = 4.dp,
                        border = BorderStroke(1.dp, Color(0xFFE6EBF2)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // İlan Tipi & Güvence Kartı
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                shadowElevation = 1.dp,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(11.dp),
                                            color = MColors.BrandTint,
                                            border = BorderStroke(1.dp, MColors.Brand.copy(alpha = 0.22f)),
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Rounded.Campaign, null, tint = MColors.BrandDeep, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Yeni karşılaşma", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = MColors.Ink900)
                                            Text("Katılım güvencesi · 10 TKN", style = MaterialTheme.typography.labelSmall, color = MColors.Ink500)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFECFDF5),
                                            border = BorderStroke(1.dp, MColors.Mint.copy(alpha = 0.35f))
                                        ) {
                                            Text(
                                                text = "Güvenceli",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MColors.Mint,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    MSegmentPillBar(
                                        options = listOf(
                                            MSegmentPillOption(ListingIntent.NEW_MATCH, ListingIntent.NEW_MATCH.title, Icons.Rounded.Sports),
                                            MSegmentPillOption(ListingIntent.FIND_PLAYERS, ListingIntent.FIND_PLAYERS.title, Icons.Rounded.PersonAdd)
                                        ),
                                        selectedValue = listingIntent,
                                        onSelect = { listingIntent = it }
                                    )
                                }
                            }

                            MSegmentedControl(listOf("1 · Branş", "2 · Buluşma", "3 · Tercihler"), adStep, ::selectStep)
                            if (adStep == 0) {
                                if (adCategory == SportCategory.CUSTOM || adSport == Sport.CUSTOM) {
                                    MFloatingInputBox(
                                        label = "Özel Branş / Oyun Adı",
                                        value = customSportName,
                                        onValueChange = {
                                            customSportName = it
                                            if (!adTitleCustomized) {
                                                adTitle = defaultListingTitle(adLocation, adCity, adSport, it, initialVenue)
                                            }
                                            adFormError = null
                                        },
                                        placeholder = "Örn: Boks, Squash, Tırmanış, Dota 2...",
                                        icon = Icons.Rounded.AutoAwesome,
                                        badgeText = "Özel",
                                        singleLine = true
                                    )
                                    MFloatingDropdownBox(
                                        label = "Ortam Türü",
                                        selectedValue = customIsGaming,
                                        options = listOf(false, true),
                                        optionLabel = { if (it) "🎮 Online / Gaming" else "🏟️ Saha / Salon" },
                                        onSelect = {
                                            customIsGaming = it
                                            if (it) esAdType = defaultFormatForSport(Sport.ESPORTS_FC) else adFormat = defaultFormatForSport(Sport.CUSTOM)
                                        },
                                        icon = Icons.Rounded.Category
                                    )
                                } else {
                                    val currentList = if (adCategory == SportCategory.PHYSICAL) physicalSports else esportSports
                                    SportPickerField(adSport, { selected ->
                                        if (selected != null) {
                                            adSport = selected
                                            adFormat = defaultFormatForSport(selected)
                                            esAdType = defaultFormatForSport(selected)
                                            if (!adPositionCustomized) adPosition = defaultPositionForSport(selected)
                                            esNeededRole = if (selected.isTeamSport) "Oyuncu" else "Rakip"
                                            if (!adTitleCustomized) {
                                                adTitle = defaultListingTitle(adLocation, adCity, selected, customSportName, initialVenue)
                                            }
                                        }
                                    }, sports = currentList, allowAll = false)
                                }

                                MFloatingInputBox(
                                    label = "İlan Başlığı",
                                    value = adTitle,
                                    onValueChange = { adTitle = it; adTitleCustomized = true; adFormError = null },
                                    placeholder = "Örn. Kadıköy Halı Saha · Orta saha aranıyor",
                                    icon = Icons.Rounded.Campaign,
                                    badgeText = if (adTitle.trim().length >= 3) "Hazır" else "Zorunlu",
                                    singleLine = true,
                                    isError = adFormError != null && adTitle.isBlank(),
                                    suggestions = listOf(
                                        "$adLocation ${adSport.label} · Eksik oyuncu",
                                        "$adLocation akşam maçı",
                                        "Hafta sonu keyif maçı"
                                    )
                                )

                                if (adFormError != null) {
                                    Text(adFormError!!, color = MColors.Rose, style = MaterialTheme.typography.bodySmall)
                                }

                                ExpandableListingSectionToggle(
                                    title = "Ağ, kategori ve açıklama",
                                    summary = "${networkScopeLabel(adNetworkScope)} · ${adCategory.label}${if (adDescription.isNotBlank()) " · Açıklama eklendi" else ""}",
                                    expanded = showStep1Advanced,
                                    icon = Icons.Rounded.Tune,
                                    onClick = { showStep1Advanced = !showStep1Advanced }
                                )

                                if (showStep1Advanced) {
                                    Text("İlanın yayınlanacağı ağ", style = MaterialTheme.typography.labelLarge, color = MColors.Ink900)
                                    MSegmentPillBar(
                                        options = listOf(
                                            MSegmentPillOption(EventNetworkScope.CITY, "Saha", Icons.Rounded.Stadium, weight = 0.9f),
                                            MSegmentPillOption(EventNetworkScope.CAMPUS, "Kampüs", Icons.Rounded.School, weight = 1.05f),
                                            MSegmentPillOption(EventNetworkScope.COMPANY, "Şirket", Icons.Rounded.Business, weight = 1f),
                                            MSegmentPillOption(EventNetworkScope.TRAVEL, "Seyahat", Icons.Rounded.FlightTakeoff, weight = 1.05f)
                                        ),
                                        selectedValue = adNetworkScope,
                                        onSelect = { scope ->
                                            adNetworkScope = scope
                                            adCommunityId = null
                                            adNetworkName = ""
                                            adFormError = null
                                        },
                                        fontSizeSp = 11f,
                                        modifier = Modifier.background(MGradients.BrandSoft, RoundedCornerShape(12.dp)),
                                        containerColor = Color.Transparent,
                                        borderColor = MColors.Brand.copy(alpha = 0.16f)
                                    )
                                    Text(
                                        when (adNetworkScope) {
                                            EventNetworkScope.CITY -> "Seçtiğin şehirdeki sporculara açık tek buluşma ilanı."
                                            EventNetworkScope.CAMPUS, EventNetworkScope.COMPANY -> "Yeni topluluk oluşturmaz; üye olduğun topluluğa tek buluşma ilanı yayınlar."
                                            EventNetworkScope.TRAVEL -> "Gideceğin şehirde sporculara açık buluşma ilanı."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MColors.Ink500
                                    )

                                    if (adNetworkScope != EventNetworkScope.CITY) {
                                        val restrictedNetwork = eligibleCommunities != null && adNetworkScope in listOf(EventNetworkScope.CAMPUS, EventNetworkScope.COMPANY)
                                        val approvedNetworks = eligibleCommunities.orEmpty().filter {
                                            if (adNetworkScope == EventNetworkScope.CAMPUS) it.type == CommunityType.CAMPUS else it.type == CommunityType.COMPANY
                                        }
                                        val networkFieldLabel = when (adNetworkScope) {
                                            EventNetworkScope.CAMPUS -> "Kampüs / Okul Adı"
                                            EventNetworkScope.COMPANY -> "Şirket / Kurum Adı"
                                            EventNetworkScope.TRAVEL -> "Gidilecek Şehir"
                                            EventNetworkScope.CITY -> "Ağ"
                                        }
                                        if (restrictedNetwork) {
                                            if (approvedNetworks.isEmpty()) Text("Bu ağda ilan açmak için önce bir topluluğa katılıp onay almalısın.", color = MColors.Ink500, style = MaterialTheme.typography.bodySmall)
                                            else MFloatingDropdownBox(
                                                label = networkFieldLabel,
                                                selectedValue = adCommunityId.orEmpty(),
                                                options = approvedNetworks.map { it.id },
                                                optionLabel = { id -> approvedNetworks.firstOrNull { it.id == id }?.name ?: "Ağ seç" },
                                                onSelect = { id -> adCommunityId = id; adNetworkName = approvedNetworks.first { it.id == id }.name },
                                                icon = Icons.Rounded.Hub
                                            )
                                        } else if (adNetworkScope == EventNetworkScope.TRAVEL) {
                                            MFloatingActionRow(label = "Gidilecek şehir ve bölge", valueText = "$adCity · $adLocation", icon = Icons.Rounded.Flight, onClick = { showAdRegion = true })
                                        } else MFloatingInputBox(
                                            label = networkFieldLabel,
                                            value = adNetworkName,
                                            onValueChange = { adNetworkName = it; adFormError = null },
                                            placeholder = "Ağ adını gir",
                                            icon = Icons.Rounded.Hub
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color.White,
                                        shadowElevation = 1.dp,
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("BRANŞ KATEGORİSİ", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MColors.Ink500)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            MSegmentPillBar(
                                                options = listOf(
                                                    MSegmentPillOption(SportCategory.PHYSICAL, "Fiziksel", emoji = "🏃‍♂️"),
                                                    MSegmentPillOption(SportCategory.ESPORTS, "E-Spor", emoji = "🎮"),
                                                    MSegmentPillOption(SportCategory.CUSTOM, "Özel Branş", emoji = "✨", weight = 1.15f)
                                                ),
                                                selectedValue = adCategory,
                                                onSelect = { cat ->
                                                    adCategory = cat
                                                    if (cat == SportCategory.PHYSICAL && adSport.category != SportCategory.PHYSICAL) {
                                                        adSport = Sport.FOOTBALL
                                                        adFormat = defaultFormatForSport(Sport.FOOTBALL)
                                                    } else if (cat == SportCategory.ESPORTS && adSport.category != SportCategory.ESPORTS) {
                                                        adSport = Sport.ESPORTS_FC
                                                        esAdType = defaultFormatForSport(Sport.ESPORTS_FC)
                                                    } else if (cat == SportCategory.CUSTOM) {
                                                        adSport = Sport.CUSTOM
                                                        adFormat = defaultFormatForSport(Sport.CUSTOM)
                                                    }
                                                    if (!adPositionCustomized) adPosition = defaultPositionForSport(adSport)
                                                    if (!adTitleCustomized) {
                                                        adTitle = defaultListingTitle(adLocation, adCity, adSport, customSportName, initialVenue)
                                                    }
                                                }
                                            )
                                        }
                                    }

                                    MFloatingInputBox(
                                        label = "Kısa Açıklama",
                                        value = adDescription,
                                        onValueChange = { adDescription = it },
                                        placeholder = "Varsa ek notlar (krampon, renkli forma vb.)",
                                        icon = Icons.Rounded.EditNote,
                                        singleLine = false,
                                        minLines = 2,
                                        maxLines = 3
                                    )
                                }
                            }
                            val isGaming = (adSport.isESport || adSport == Sport.CHESS) || (adSport == Sport.CUSTOM && customIsGaming)

                            if (adStep == 1) {
                                EventDateTimeField(adCalendarDate, adHour, adMinute) { date, hour, minute ->
                                    adCalendarDate = date; adHour = hour; adMinute = minute; adFormError = null
                                }
                            if (isGaming) {
                                MFloatingInputBox(
                                    label = "Sunucu & Bölge",
                                    value = esServer,
                                    onValueChange = { esServer = it; adFormError = null },
                                    placeholder = "TR / EU West",
                                    icon = Icons.Rounded.Dns,
                                    badgeText = if (esServer.isNotBlank()) "Hazır" else null,
                                    suggestions = listOf("TR Sunucusu", "EU West", "Frankfurt")
                                )

                                if (listingIntent == ListingIntent.FIND_PLAYERS) {
                                    MFloatingInputBox(
                                        label = "Aranan Rol / Oyuncu",
                                        value = esNeededRole,
                                        onValueChange = { esNeededRole = it; adFormError = null },
                                        placeholder = "Örn. Düellocu, destek",
                                        icon = Icons.Rounded.PersonSearch,
                                        badgeText = if (esNeededRole.isNotBlank()) "Hazır" else "Zorunlu",
                                        suggestions = positionsForSport(adSport).ifEmpty { listOf("Esnek Rol", "Oyuncu", "Destek") }
                                    )
                                }

                                ExpandableListingSectionToggle(
                                    title = "Platform, format ve sesli iletişim",
                                    summary = "$esPlatform · $esAdType${if (esVoiceComm.isNotBlank()) " · $esVoiceComm" else ""}",
                                    expanded = showStep2Advanced,
                                    icon = Icons.Rounded.Tune,
                                    onClick = { showStep2Advanced = !showStep2Advanced }
                                )

                                if (showStep2Advanced) {
                                    MFloatingDropdownBox(
                                        label = "Oyun Platformu",
                                        selectedValue = esPlatform,
                                        options = listOf("🖥️ PC", "🎮 PS5", "🟩 Xbox", "📱 Mobil", "🌐 Crossplay"),
                                        optionLabel = { it },
                                        onSelect = { esPlatform = it },
                                        icon = Icons.Rounded.SportsEsports
                                    )

                                    MFloatingDropdownBox(
                                        label = "Maç Formatı",
                                        selectedValue = esAdType,
                                        options = formatsForSport(adSport),
                                        optionLabel = { it },
                                        onSelect = { esAdType = it },
                                        icon = Icons.Rounded.Groups
                                    )

                                    MFloatingInputBox(
                                        label = "Sesli İletişim",
                                        value = esVoiceComm,
                                        onValueChange = { esVoiceComm = it },
                                        placeholder = "Discord / oyun içi ses",
                                        icon = Icons.Rounded.HeadsetMic,
                                        badgeText = "Lobi"
                                    )

                                    MFloatingInputBox(
                                        label = "Oyun İçi ID / GamerTag",
                                        value = esGamerTag,
                                        onValueChange = { esGamerTag = it },
                                        placeholder = "Örn: Oyuncu#TR1",
                                        icon = Icons.Rounded.Badge,
                                        badgeText = "ID"
                                    )
                                }
                            } else {
                                MFloatingInputBox(
                                    label = "Tesis / Saha Adı",
                                    value = adVenue,
                                    onValueChange = { adVenue = it; adVenueCustomized = true; adFormError = null },
                                    placeholder = "Saha adı veya açık adres",
                                    icon = Icons.Rounded.Stadium,
                                    badgeText = if (adVenue.isNotBlank()) "Hazır" else "Konum",
                                    suggestions = listOf(
                                        "$adLocation Merkez Saha",
                                        "$adCity Spor Kompleksi",
                                        "$adLocation Açık Kort"
                                    )
                                )

                                if (listingIntent == ListingIntent.FIND_PLAYERS) {
                                    MFloatingInputBox(
                                        label = "Aranan Pozisyon / Rol",
                                        value = adPosition,
                                        onValueChange = { adPosition = it; adPositionCustomized = true; adFormError = null },
                                        placeholder = "İstediğin pozisyonu veya rolü yaz",
                                        icon = Icons.Rounded.PersonSearch,
                                        badgeText = if (adPosition.isNotBlank()) "Hazır" else "Kadro",
                                        suggestions = positionsForSport(adSport)
                                    )
                                }

                                ExpandableListingSectionToggle(
                                    title = "Bölge, harita konumu ve format",
                                    summary = "$adCity · $adLocation · $adFormat · ${if (adPoint != null) "Harita hazır" else "Konum seç"}",
                                    expanded = showStep2Advanced,
                                    icon = Icons.Rounded.Tune,
                                    onClick = { showStep2Advanced = !showStep2Advanced }
                                )

                                if (showStep2Advanced) {
                                    MFloatingActionRow(
                                        label = "Şehir ve Bölge",
                                        valueText = "$adCity · $adLocation",
                                        icon = Icons.Rounded.LocationCity,
                                        onClick = { showAdRegion = true }
                                    )

                                    MFloatingActionRow(
                                        label = "Harita Konumu",
                                        valueText = if (adPoint == null) "Haritadan Seç" else "Konum Hazır · Değiştir",
                                        icon = if (adPoint == null) Icons.Rounded.AddLocationAlt else Icons.Rounded.WhereToVote,
                                        onClick = { showAdMap = true }
                                    )

                                    MFloatingDropdownBox(
                                        label = "Maç Formatı",
                                        selectedValue = adFormat,
                                        options = formatsForSport(adSport),
                                        optionLabel = { it },
                                        onSelect = { adFormat = it },
                                        icon = Icons.Rounded.Groups
                                    )

                                    if (listingIntent != ListingIntent.FIND_PLAYERS) {
                                        MFloatingInputBox(
                                            label = "Aranan Pozisyon / Rol (İsteğe Bağlı)",
                                            value = adPosition,
                                            onValueChange = { adPosition = it; adPositionCustomized = true; adFormError = null },
                                            placeholder = "İstediğin pozisyonu veya rolü yaz",
                                            icon = Icons.Rounded.PersonSearch,
                                            badgeText = "Opsiyonel",
                                            suggestions = positionsForSport(adSport)
                                        )
                                    }
                                }
                            }

                            }
                            if (adStep == 2) {
                            MSubCard(onClick = { adStep = 1 }) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text("Buluşma Özeti", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = MColors.Ink900)
                                        Text("${eventDateLabel(adCalendarDate)} · ${formatTime(adHour, adMinute)} · ${adDurationText.ifBlank { "--" }} dk", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MColors.Ink700)
                                        Text(
                                            text = if (isGaming) "$esPlatform · ${esServer.ifBlank { "Sunucu seçilmedi" }}" else "$adCity · $adLocation${if (adVenue.isNotBlank()) " · $adVenue" else ""}",
                                            fontSize = 11.5.sp,
                                            color = MColors.Ink500,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MColors.BrandTint,
                                        border = BorderStroke(1.dp, MColors.Brand.copy(alpha = 0.25f)),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Rounded.Edit, null, tint = MColors.BrandDeep, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }

                            MFloatingInputBox(
                                label = "Kişi Başı Ücret (₺)",
                                value = adCostPerPlayer,
                                onValueChange = { raw ->
                                    adCostPerPlayer = raw.filter(Char::isDigit).take(6)
                                    adFormError = null
                                },
                                placeholder = "0 ücretsiz",
                                icon = Icons.Rounded.Toll,
                                badgeText = if (adCostPerPlayer == "0" || adCostPerPlayer.isBlank()) "Ücretsiz" else "₺$adCostPerPlayer",
                                suggestions = listOf("0", "150", "250", "350")
                            )

                            MFloatingDropdownBox("Katılım şekli", adApprovalRequired, listOf(true, false), { if (it) "Organizatör onayı ile" else "Doğrudan katılım" }, { adApprovalRequired = it }, icon = Icons.Rounded.HowToReg)

                            ExpandableListingSectionToggle(
                                title = "Detaylı maç kuralları",
                                summary = "$adLevelRange · ${adDurationText.ifBlank { "60" }} dk · ${if (adMode == PlayMode.SOCIAL) "Sosyal" else "Rekabetçi"}",
                                expanded = showStep3Advanced,
                                icon = Icons.Rounded.Tune,
                                onClick = { showStep3Advanced = !showStep3Advanced }
                            )

                            if (showStep3Advanced) {
                                if (listingIntent == ListingIntent.FIND_PLAYERS) {
                                    MFloatingDropdownBox(
                                        label = "Çağrı Önceliği",
                                        selectedValue = urgentPlayerFill,
                                        options = listOf(false, true),
                                        optionLabel = { if (it) "Acil Oyuncu Çağrısı" else "Standart Çağrı" },
                                        onSelect = { urgentPlayerFill = it },
                                        icon = Icons.Rounded.NotificationsActive
                                    )
                                }

                                MFloatingDropdownBox(
                                    label = "Seviye Aralığı",
                                    selectedValue = adLevelRange,
                                    options = listOf("Tüm seviyeler", "Başlangıç – Orta", "Orta – İleri", "İleri"),
                                    optionLabel = { it },
                                    onSelect = { adLevelRange = it },
                                    icon = Icons.Rounded.Leaderboard
                                )

                                val parsedDurationMin = adDurationText.toIntOrNull()
                                MFloatingInputBox(
                                    label = "Maç Süresi (Dakika)",
                                    value = adDurationText,
                                    onValueChange = { raw ->
                                        adDurationText = raw.filter(Char::isDigit).take(3)
                                        adFormError = null
                                    },
                                    placeholder = "İstediğin süreyi dakika olarak gir (15–720)",
                                    icon = Icons.Rounded.Timer,
                                    badgeText = parsedDurationMin?.let { "$it dk" } ?: "Dakika",
                                    isError = adFormError != null && (parsedDurationMin == null || parsedDurationMin !in 15..720),
                                    suggestions = listOf("45", "60", "90")
                                )

                                MFloatingDropdownBox(
                                    label = "Oyun Modu",
                                    selectedValue = adMode,
                                    options = PlayMode.entries,
                                    optionLabel = { mode ->
                                        when (mode) {
                                            PlayMode.SOCIAL -> "Sosyal"
                                            PlayMode.COMPETITIVE -> "Rekabetçi"
                                        }
                                    },
                                    onSelect = { adMode = it },
                                    icon = Icons.Rounded.EmojiEvents
                                )

                                val trustOptions = remember {
                                    listOf(
                                        false to 0,
                                        false to 50,
                                        false to 60,
                                        false to 70,
                                        false to 80,
                                        false to 90,
                                        true to 0,
                                        true to 50,
                                        true to 60,
                                        true to 70,
                                        true to 75,
                                        true to 80,
                                        true to 85,
                                        true to 90,
                                        true to 95,
                                        true to 100
                                    )
                                }
                                val currentTrustSelection = (adVerifiedOnly to adMinReliability).let { current ->
                                    if (current in trustOptions) current else (false to 0)
                                }
                                MFloatingDropdownBox(
                                    label = "Katılımcı Güveni",
                                    selectedValue = currentTrustSelection,
                                    options = trustOptions,
                                    optionLabel = { (verified, minRel) ->
                                        when {
                                            !verified && minRel == 0 -> "Tüm Sporcular"
                                            !verified -> "Min %$minRel Güven"
                                            minRel == 0 -> "Doğrulanmış"
                                            minRel == 100 -> "Doğrulanmış · %100"
                                            else -> "Doğrulanmış · %$minRel+"
                                        }
                                    },
                                    onSelect = { (verified, minRel) ->
                                        adVerifiedOnly = verified
                                        adMinReliability = minRel
                                    },
                                    icon = Icons.Rounded.VerifiedUser
                                )
                            }
                            }
                        }
                    }
                }

                item {
                    if (adFormError != null) Text(adFormError!!, style = MaterialTheme.typography.bodyMedium, color = MColors.Rose)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (adStep > 0) MButton("Geri", { selectStep(adStep - 1) }, isPrimary = false, enabled = !publishing, modifier = Modifier.weight(1f))
                        if (adStep < 2) MButton("Devam et", { selectStep(adStep + 1) }, enabled = !publishing, modifier = Modifier.weight(1f))
                    }
                }
                if (adStep == 2 || (draftStore.hasPublicationAttempt && backend != null)) {
                // A previous publication can be checked from every draft step, even with an expired date.
                item {
                    MButton(
                        text = if (publishing) "Yayın durumu kontrol ediliyor…" else if (draftStore.hasPublicationAttempt && backend != null) "Yayın durumunu kontrol et" else if (tokenBalance < 10) "Token yükle · 10 TKN gerekli" else "İlanı yayınla · 10 TKN kilitle",
                        enabled = !publishing,
                        onClick = {
                            if (publishing) return@MButton
                            focusManager.clearFocus()
                            if (!persistDraft()) return@MButton
                            publishing = true
                            adFormError = null
                            scope.launch {
                                try {
                                    if (backend != null) {
                                        val recovered = recoverListingPublication(draftStore, backend, draftOwnerKey)
                                        if (recovered != null) {
                                            draftFinished = true
                                            onRecoveredPublication(recovered.id, recovered.keptDraft)
                                            onDismiss()
                                            return@launch
                                        }
                                        if (backend.userId != draftOwnerKey) throw BackendFailure("SESSION_CHANGED", "Hesap değişti. Tekrar giriş yap.")
                                    }
                                    val error = stepError(2)
                                    if (error != null) {
                                        adStep = error.first
                                        adFormError = error.second
                                        return@launch
                                    }
                                    if (tokenBalance < 10) { onRequestTokens(); return@launch }
                                    val isGaming = (adSport.isESport || adSport == Sport.CHESS) || (adSport == Sport.CUSTOM && customIsGaming)
                                    val cleanTitle = adTitle.trim()
                                    val cleanLocation = adLocation.trim()
                                    val cleanVenue = adVenue.trim()
                                    val cleanRole = if (isGaming) esNeededRole.trim() else adPosition.trim()
                                    val cleanFormat = if (isGaming) esAdType.trim() else adFormat.trim()
                                    val fee = adCostPerPlayer.ifBlank { "0" }.toIntOrNull()
                                    val parsedDuration = adDurationText.toIntOrNull()
                                    val eventCapacity = capacityForFormat(cleanFormat)
                                    val customName = if (adSport == Sport.CUSTOM) customSportName.trim() else null
                                    val cleanNetworkName = if (adNetworkScope == EventNetworkScope.TRAVEL) adCity.trim() else adNetworkName.trim()
                                    val newEvent = SportEvent(
                                        id = newEventId(),
                                        title = cleanTitle,
                                        sport = adSport,
                                        venue = if (isGaming) "$esPlatform · ${esServer.trim()}".trim() else cleanVenue,
                                        area = if (isGaming) "Online" else cleanLocation,
                                        day = eventDateLabel(adCalendarDate),
                                        startsAtEpochMillis = eventStartMillis(adCalendarDate, adHour, adMinute),
                                        city = adCity,
                                        approximateLocation = adPoint.takeUnless { isGaming },
                                        time = formatTime(adHour, adMinute),
                                        startsInMin = null,
                                        durationMin = parsedDuration!!,
                                        distanceKm = null,
                                        capacity = eventCapacity,
                                        participantIds = listOf(DemoData.ME),
                                        waitlistCount = 0,
                                        mode = adMode,
                                        levelRange = adLevelRange,
                                        minReliability = adMinReliability,
                                        verifiedOnly = adVerifiedOnly,
                                        pricePerPersonTry = fee!!,
                                        commitmentTokens = 10,
                                        hostId = DemoData.ME,
                                        status = if (listingIntent == ListingIntent.FIND_PLAYERS) {
                                            com.matchora.app.domain.EventStatus.FILLING
                                        } else {
                                            com.matchora.app.domain.EventStatus.PUBLISHED
                                        },
                                        score = ScoreBreakdown(0, 0, 0, 0, 0, emptyList()),
                                        neededRoles = cleanRole.split(',').map { it.trim() }.filter { it.isNotBlank() },
                                        emergency = listingIntent == ListingIntent.FIND_PLAYERS && urgentPlayerFill,
                                        description = adDescription.trim(),
                                        customSportName = customName,
                                        networkScope = adNetworkScope,
                                        networkName = cleanNetworkName.takeIf { adNetworkScope != EventNetworkScope.CITY },
                                        communityId = adCommunityId,
                                        approvalRequired = adApprovalRequired,
                                        listingType = if (listingIntent == ListingIntent.FIND_PLAYERS) EventListingType.SEEK_PLAYERS else EventListingType.OPEN_MATCH,
                                        format = cleanFormat,
                                        gamePlatform = esPlatform.takeIf { isGaming },
                                        gameServer = esServer.trim().takeIf { isGaming && it.isNotBlank() },
                                        voiceChannel = esVoiceComm.trim().takeIf { isGaming && it.isNotBlank() },
                                        hostGameTag = esGamerTag.trim().takeIf { isGaming && it.isNotBlank() }
                                    )
                                    val requestKey = draftStore.publicationKey(newEvent.toApiPayload())
                                    if (onPublishEvent != null) onPublishEvent(newEvent, requestKey) else onCreateEvent(newEvent)
                                    draftStore.clear()
                                    draftFinished = true
                                    onDismiss()
                                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                                catch (error: Exception) { adFormError = error.message ?: "İlan yayınlanamadı. Bilgilerin taslakta duruyor; tekrar deneyebilirsin." }
                                finally { publishing = false }
                            }
                        },
                        icon = Icons.Rounded.Campaign
                    )
                }
                }
            }
        }
        if (publishing) {
            Box(Modifier.matchParentSize().clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {})
            LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter), color = MColors.Brand)
        }
        }
    }
    if (showDraftClose) AlertDialog(
        onDismissRequest = { showDraftClose = false },
        title = { Text("İlan taslağı saklansın mı?") },
        text = { Text("Girdiğin bilgilere daha sonra kaldığın yerden devam edebilirsin.") },
        confirmButton = { TextButton(onClick = { if (persistDraft()) { showDraftClose = false; onDismiss() } }) { Text("Taslağı sakla") } },
        dismissButton = { Row {
            TextButton(onClick = { showDraftClose = false }) { Text("Düzenlemeye devam et") }
            TextButton(onClick = {
                try { draftStore.clear(); draftFinished = true; showDraftClose = false; onDismiss() }
                catch (error: Exception) { showDraftClose = false; adFormError = error.message ?: "Taslak silinemedi." }
            }) { Text("Vazgeç ve sil", color = MColors.Rose) }
        } }
    )
    if (showAdMap) EventLocationPicker(
        initialPoint = adPoint ?: cityMapCenter(adCity), initialAddress = adVenue, city = adCity, district = adLocation,
        onDismiss = { showAdMap = false }, onConfirm = { point, address ->
            adPoint = point
            if (adVenue.isBlank() || !adVenueCustomized) {
                adVenue = address
                adVenueCustomized = true
            }
            adFormError = null
        })
    if (showAdRegion) CityAndLocationSelectorSheet(adCity, adLocation, false,
        onDismiss = { showAdRegion = false }, onSelectLocation = { city, district, isGps ->
            adCity = city
            adLocation = district
            adPoint = if (isGps) DeviceLocationState.current.value?.let { EventGeoPoint(it.latitude, it.longitude) } ?: cityMapCenter(city)
            else cityMapCenter(city)
            if (!adVenueCustomized || adVenue.isBlank()) adVenue = defaultVenueName(district, city)
            if (!adTitleCustomized || adTitle.isBlank()) adTitle = defaultListingTitle(district, city, adSport, customSportName, initialVenue)
            showAdRegion = false
            adFormError = null
        })

}
