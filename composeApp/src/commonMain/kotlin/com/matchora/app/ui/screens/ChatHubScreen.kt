@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.matchora.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchora.app.domain.Sport
import com.matchora.app.state.*
import com.matchora.app.ui.components.*
import com.matchora.app.ui.theme.MColors
import com.matchora.app.data.*
import kotlinx.coroutines.launch

private enum class ChatHubSection(val label: String) {
    MESSAGES("Mesajlar"),
    SHOWCASE("Anı Vitrini")
}

private enum class ShowcaseFeedMode(val label: String) {
    RANDOM_EXPLORE("Keşfet · Karışık Akış"),
    NEARBY_LOCATION("Konuma Göre Vitrin"),
    TOP_LIKED("En Çok Beğenilenler"),
    NEWEST("En Yeniler"),
    MINE("Paylaşımlarım")
}

internal fun resolveChatParticipantPlayer(
    name: String,
    threadId: String?,
    backendMode: Boolean,
    livePlayers: List<Player>,
    directOtherUserId: String?,
    fallbackArea: String
): Player {
    if (!backendMode) {
        val found = DemoData.players.firstOrNull { it.name.equals(name, ignoreCase = true) }
        if (found != null) return found
    }
    val livePool = if (backendMode) livePlayers else emptyList()
    val matched = (directOtherUserId?.let { id -> livePool.firstOrNull { it.id == id } }
        ?: livePool.firstOrNull { it.name.equals(name, ignoreCase = true) })
    if (matched != null) return matched
    val fallbackId = directOtherUserId ?: "u_${name.lowercase().replace(" ", "_")}"
    return Player(
        id = fallbackId,
        name = name,
        area = if (!backendMode) fallbackArea else "",
        sports = listOf(Sport.FOOTBALL),
        level = SkillLevel.INTERMEDIATE,
        reliability = 0,
        verifiedMatches = 0,
        rating = 1000,
        chemistry = null,
        verified = false,
        position = "Sporcu",
        distanceKm = null,
        playedTogether = 0,
        availableNow = false,
        color = MColors.Brand,
        bio = "",
        reliabilityVisible = false,
        profileAvailable = false
    )
}

@Composable
fun ChatHubScreen(
    paddingValues: PaddingValues,
    threads: List<ChatThread>,
    memories: List<MatchMemoryPost> = DemoData.initialMemoryPosts,
    currentCity: String = "İstanbul",
    currentDistrict: String = "Kadıköy",
    currentUser: UserProfile = DemoData.me,
    initialActiveThreadId: String? = null,
    onActiveThreadChange: (String?) -> Unit = {},
    onSendMessage: (String, String) -> Unit,
    onSendMemoryToChat: (threadId: String, caption: String, memory: MatchMemoryPost) -> Unit = { _, _, _ -> },
    onCreateMemoryPost: (MatchMemoryPost, String?) -> Unit = { _, _ -> },
    onToggleLikeMemory: (String, Boolean) -> Unit = { _, _ -> },
    onUpdateMemory: (MatchMemoryPost) -> Unit = {},
    onRequireLogin: () -> Unit = {},
    onSendReply: (String, String, String?) -> Unit = { thread, text, _ -> onSendMessage(thread, text) },
    onEditMessage: (ChatMsg, String) -> Unit = { _, _ -> },
    onDeleteMessage: (ChatMsg, Boolean) -> Unit = { _, _ -> },
    onClearChat: (ChatThread) -> Unit = {},
    onDeleteChat: (ChatThread, Boolean) -> Unit = { _, _ -> },
    blockedThreadIds: Set<String> = emptySet(),
    ownBlockedThreadIds: Set<String> = blockedThreadIds,
    onToggleBlock: (String) -> Unit = {},
    onReportThread: (ChatThread) -> Unit = {},
    onReportMessage: (ChatMsg) -> Unit = {},
    onDeleteMemory: (MatchMemoryPost) -> Unit = {},
    onReportMemory: (MatchMemoryPost) -> Unit = {},
    onAddMemoryComment: (String, String) -> Unit = { _, _ -> },
    players: List<Player> = DemoData.players,
    onOpenRelatedEvent: ((String) -> Unit)? = null,
    onOpenRelatedTeam: ((String) -> Unit)? = null,
    onOpenDirectChat: (Player) -> Unit = {}
) {
    val backend = LocalBackendRepository.current
    val requireAccount = LocalRequireAccount.current
    val featuresLocked = LocalFeaturesLocked.current
    val inboxSnapshot = backend?.snapshots?.collectAsState()?.value
    var inboxBusy by remember { mutableStateOf(false) }
    var inboxError by remember { mutableStateOf<String?>(null) }
    val appForeground = rememberAppActive()
    val feed = if (backend != null) backend.memoryFeed.collectAsState().value else null
    val visibleMemories = if (backend != null) BackendSnapshot(apiPayload("userId" to backend.userId, "memories" to feed?.a("items").orEmpty(), "serverTime" to feed?.l("serverTime"))).memories else memories
    var feedBusy by remember { mutableStateOf(false) }
    var feedError by remember { mutableStateOf<String?>(null) }
    val feedScope = rememberCoroutineScope()
    val deviceLocation by DeviceLocationState.current.collectAsState()
    var locationError by remember { mutableStateOf<String?>(null) }
    var locationSettings by remember { mutableStateOf(false) }
    val requestFeedLocation = rememberLocationRequester { result ->
        when (result) {
            is LocationOutcome.Located -> { DeviceLocationState.current.value = result.value; locationError = null; locationSettings = false }
            is LocationOutcome.Failed -> { locationError = result.message; locationSettings = result.settingsAvailable }
        }
    }
    var editingMemory by remember { mutableStateOf<MatchMemoryPost?>(null) }
    var messageMenu by remember { mutableStateOf<ChatMsg?>(null) }
    var confirmBlockThreadId by remember { mutableStateOf<String?>(null) }
    var deletingMessage by remember { mutableStateOf<Pair<ChatMsg, Boolean>?>(null) }
    var editingMessage by remember { mutableStateOf<ChatMsg?>(null) }
    var editedText by remember { mutableStateOf("") }
    var replyTarget by remember { mutableStateOf<ChatMsg?>(null) }
    var clearChatConfirmation by remember { mutableStateOf<Boolean?>(null) }
    var deleteAndBlock by remember { mutableStateOf(false) }
    var activeId by remember(initialActiveThreadId) { mutableStateOf(initialActiveThreadId) }
    var activeSection by remember { mutableStateOf(ChatHubSection.MESSAGES) }
    var input by remember(activeId) { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableIntStateOf(0) }
    var showInfo by remember { mutableStateOf(false) }

    // Anı Vitrini & Fotoğraf Paylaşım State
    var showcaseMode by remember { mutableStateOf(ShowcaseFeedMode.RANDOM_EXPLORE) }
    var shuffleSeed by remember { mutableIntStateOf(if (backend != null) kotlin.random.Random.nextInt(1, Int.MAX_VALUE) else 1) }
    var selectedMemoryForDetail by remember { mutableStateOf<MatchMemoryPost?>(null) }
    var showCreateMemorySheet by remember { mutableStateOf(false) }
    var preselectedChatIdForMemory by remember { mutableStateOf<String?>(null) }
    var inspectedPlayer by remember { mutableStateOf<Player?>(null) }

    fun resolvePlayerForName(name: String, threadId: String? = null): Player {
        val otherId = if (threadId != null && backend != null) {
            inboxSnapshot?.let { BackendSnapshot(it).otherUserForThread(threadId) }
        } else null
        val livePool = if (backend != null) {
            inboxSnapshot?.let { BackendSnapshot(it).players }.orEmpty() + players
        } else emptyList()
        return resolveChatParticipantPlayer(
            name = name,
            threadId = threadId,
            backendMode = backend != null,
            livePlayers = livePool,
            directOtherUserId = otherId,
            fallbackArea = currentDistrict
        )
    }

    val active = threads.firstOrNull { it.id == activeId }
    LaunchedEffect(activeId) { replyTarget = null; editingMessage = null; messageMenu = null; clearChatConfirmation = null }
    fun open(id: String?) {
        activeId = id
        onActiveThreadChange(id)
    }

    val feedLatitude = if (showcaseMode == ShowcaseFeedMode.NEARBY_LOCATION) deviceLocation?.latitude?.let { kotlin.math.round(it * 1000) / 1000 } else null
    val feedLongitude = if (showcaseMode == ShowcaseFeedMode.NEARBY_LOCATION) deviceLocation?.longitude?.let { kotlin.math.round(it * 1000) / 1000 } else null
    LaunchedEffect(backend, showcaseMode, shuffleSeed, currentCity, currentDistrict, feedLatitude, feedLongitude, activeSection, appForeground) {
        if (backend != null && activeSection == ChatHubSection.SHOWCASE && appForeground) {
            val mode = when (showcaseMode) { ShowcaseFeedMode.RANDOM_EXPLORE -> "random"; ShowcaseFeedMode.NEARBY_LOCATION -> "nearby"; ShowcaseFeedMode.TOP_LIKED -> "top"; ShowcaseFeedMode.NEWEST -> "newest"; ShowcaseFeedMode.MINE -> "mine" }
            feedBusy = true; feedError = null
            try { backend.selectMemoryFeed(mode, currentCity, currentDistrict, feedLatitude, feedLongitude, shuffleSeed.toString()) }
            catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (error: Exception) { feedError = error.message }
            finally { feedBusy = false }
            while (true) {
                kotlinx.coroutines.delay(20000)
                try { backend.refreshMemoryFeed(); feedError = null }
                catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                catch (error: Exception) { feedError = error.message }
            }
        }
    }
    val displayedMemories = remember(visibleMemories, showcaseMode, shuffleSeed, currentCity, currentDistrict, backend) {
        if (backend != null) visibleMemories else when (showcaseMode) {
            ShowcaseFeedMode.RANDOM_EXPLORE -> {
                if (shuffleSeed <= 1) visibleMemories
                else visibleMemories.shuffled(kotlin.random.Random(shuffleSeed))
            }
            ShowcaseFeedMode.NEARBY_LOCATION -> {
                visibleMemories.sortedWith(
                    compareBy<MatchMemoryPost> {
                        when {
                            com.matchora.app.state.matchesTurkishPlace(it.area, currentDistrict) -> 0
                            com.matchora.app.state.matchesTurkishPlace(it.city, currentCity) -> 1
                            else -> 2
                        }
                    }.thenBy { it.distanceKm ?: Float.MAX_VALUE }
                )
            }
            ShowcaseFeedMode.TOP_LIKED -> visibleMemories.sortedByDescending { it.likes }
            ShowcaseFeedMode.NEWEST -> visibleMemories
            ShowcaseFeedMode.MINE -> visibleMemories.filter { it.authorId == DemoData.ME }
        }
    }

    if (active == null) {
        val hubListState = rememberLazyListState()
        var localScrollToTop by remember { mutableIntStateOf(0) }
        val scrollToTopTrigger = LocalScrollToTopTrigger.current
        LaunchedEffect(scrollToTopTrigger, localScrollToTop) {
            if (scrollToTopTrigger > 0 || localScrollToTop > 0) {
                hubListState.animateScrollToItem(0)
            }
        }

        val filteredThreads = threads.filter {
            (filter == 0 || if (filter == 1) it.type == ThreadType.DIRECT else it.type != ThreadType.DIRECT) &&
                (query.isBlank() || it.title.contains(query, true) || it.lastMessage?.text?.contains(query, true) == true)
        }

        val nextShowcaseCursor = feed?.s("nextCursor").orEmpty()
        LaunchedEffect(hubListState, activeSection, nextShowcaseCursor, feedBusy, feedError, backend) {
            if (backend != null && activeSection == ChatHubSection.SHOWCASE && nextShowcaseCursor.isNotBlank() && !feedBusy && feedError == null) {
                snapshotFlow {
                    val info = hubListState.layoutInfo
                    val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
                    info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - 3
                }.collect { nearBottom ->
                    if (nearBottom && !feedBusy && feed?.s("nextCursor")?.isNotBlank() == true) {
                        feedBusy = true
                        feedError = null
                        try {
                            backend.loadMoreMemories()
                        } catch (cancel: kotlinx.coroutines.CancellationException) {
                            throw cancel
                        } catch (error: Exception) {
                            feedError = error.message
                        } finally {
                            feedBusy = false
                        }
                    }
                }
            }
        }
        LazyColumn(
            state = hubListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 10.dp,
                bottom = paddingValues.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. MESAJLAR / ANI VİTRİNİ GEÇİŞ ADASI
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChatHubSection.entries.forEach { section ->
                            val selected = activeSection == section
                            val bg by animateColorAsState(
                                targetValue = if (selected) MColors.Ink900 else Color.White.copy(alpha = 0.7f),
                                animationSpec = tween(200),
                                label = "chat_hub_section"
                            )
                            Surface(
                                onClick = {
                                    if (activeSection == section) localScrollToTop++ else activeSection = section
                                },
                                shape = RoundedCornerShape(11.dp),
                                color = bg,
                                shadowElevation = if (selected) 2.dp else 0.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (section == ChatHubSection.MESSAGES) Icons.Rounded.Forum else Icons.Rounded.PhotoLibrary,
                                        contentDescription = null,
                                        tint = if (selected) Color(0xFFFFB37B) else MColors.BrandDeep,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = section.label,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (selected) Color.White else MColors.Ink800
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (activeSection == ChatHubSection.MESSAGES) {
                // 3B. SOHBET FİLTRESİ & ARAMA (Derli Toplu Yüzen Kutu)
                item {
                    val filterOptions = listOf(
                        0 to "Tüm Sohbetler",
                        1 to "Kişisel Mesajlar",
                        2 to "Takım & Etkinlik Grupları"
                    )
                    val selectedFilterPair = filterOptions.firstOrNull { it.first == filter } ?: filterOptions.first()

                    MCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(13.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MFloatingDropdownBox(
                                label = "Sohbet Kategorisi",
                                selectedValue = selectedFilterPair,
                                options = filterOptions,
                                optionLabel = { it.second },
                                onSelect = { filter = it.first },
                                icon = Icons.Rounded.FilterList
                            )

                            Box(Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    colors = premiumTextFieldColors(),
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    value = query,
                                    onValueChange = { requireAccount { query = it } },
                                    readOnly = featuresLocked,
                                    placeholder = { Text("Sohbet veya oyuncu ara…") },
                                    leadingIcon = { Icon(Icons.Rounded.Search, null, tint = MColors.Ink500) },
                                    trailingIcon = if (query.isNotEmpty()) {
                                        {
                                            IconButton(onClick = { requireAccount { query = "" } }) {
                                                Icon(Icons.Rounded.Close, null, tint = MColors.Ink500, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    } else null,
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (featuresLocked) {
                                    Box(
                                        Modifier
                                            .matchParentSize()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { requireAccount {} }
                                    )
                                }
                            }
                        }
                    }
                }

                if (filteredThreads.isEmpty()) item {
                    MCard(Modifier.fillMaxWidth(), onClick = { requireAccount {} }) { Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (query.isBlank()) "Henüz sohbet yok" else "Sohbet bulunamadı", style = MaterialTheme.typography.titleMedium)
                        Text(if (query.isBlank()) "Bir maça katıldığında takım sohbetin burada açılır." else "Başka bir isim veya mesajla tekrar ara.", style = MaterialTheme.typography.bodySmall, color = MColors.Ink500)
                    } }
                }
                // 3C. SOHBET LİSTESİ
                items(filteredThreads, key = { it.id }) { thread ->
            val directOtherId = inboxSnapshot?.let { BackendSnapshot(it).otherUserForThread(thread.id) }
            val hasPhotoInLast = thread.lastMessage?.attachedMemory != null
                    MCard(
                        modifier = Modifier.fillMaxWidth().animateItem(),
                        onClick = { requireAccount { open(thread.id) } }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ChatAvatar(thread, directUserId = directOtherId)
                            Spacer(Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = thread.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MColors.Ink900,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = thread.lastMessage?.time.orEmpty(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MColors.Ink500
                                    )
                                }
                                Text(
                                    text = when {
                                        hasPhotoInLast -> {
                                            val lastTxt = thread.lastMessage?.text.orEmpty()
                                            if (lastTxt.isNotBlank()) "📸 Fotoğraf · $lastTxt" else "📸 Fotoğraf gönderildi"
                                        }
                                        else -> thread.lastMessage?.text ?: "Konuşmayı başlat"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (hasPhotoInLast) MColors.BrandDeep else MColors.Ink500,
                                    fontWeight = if (hasPhotoInLast) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    MBadge(thread.type.label, MColors.Ink600, MColors.SurfaceSunken)
                                    if (thread.muted) {
                                        Icon(Icons.Rounded.NotificationsOff, "Sessiz sohbet", modifier = Modifier.size(14.dp), tint = MColors.Ink400)
                                    }
                                    Spacer(Modifier.weight(1f))
                                    if (thread.unread > 0) {
                                        Badge(containerColor = MColors.BrandDeep) { Text("${thread.unread}") }
                                    } else if (thread.locked) {
                                        Icon(Icons.Rounded.Lock, "Kilitli sohbet", modifier = Modifier.size(14.dp), tint = MColors.Ink400)
                                    } else {
                                        Icon(Icons.Rounded.ChevronRight, null, tint = MColors.Ink400, modifier = Modifier.size(17.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                if (backend != null && inboxSnapshot?.s("nextThreadCursor")?.isNotBlank() == true) item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        inboxError?.let { Text(it, color = MColors.Rose, style = MaterialTheme.typography.bodySmall) }
                        MPillActionButton(
                            text = if (inboxBusy) "Sohbetler yükleniyor…" else "Daha eski sohbetleri göster",
                            icon = Icons.Rounded.ExpandMore,
                            tone = MPillTone.NEUTRAL,
                            enabled = !inboxBusy,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                feedScope.launch {
                                    inboxBusy = true; inboxError = null
                                    try { backend.loadMoreThreads() }
                                    catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                                    catch (error: Exception) { inboxError = error.message }
                                    finally { inboxBusy = false }
                                }
                            }
                        )
                    }
                }
            } else {
                // ═══════════════════════════════════════════════════════════════
                // 4. INSTAGRAM KEŞFET TARZI SAHA ANILARI VİTRİNİ (KONUM & RANDOM AKIŞ)
                // ═══════════════════════════════════════════════════════════════
                if (backend != null && showcaseMode == ShowcaseFeedMode.NEARBY_LOCATION) item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NearMe,
                                    contentDescription = null,
                                    tint = MColors.BrandDeep,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (feed?.s("locationBasis") == "gps") "GPS konumuna göre · fotoğraf noktaları yaklaşık" else "$currentCity · $currentDistrict bölgesine göre",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MColors.Ink700,
                                    modifier = Modifier.weight(1f)
                                )
                                MPillActionButton(
                                    text = "Konumum",
                                    icon = Icons.Rounded.MyLocation,
                                    tone = MPillTone.BRAND,
                                    compact = true,
                                    onClick = requestFeedLocation
                                )
                            }
                            locationError?.let { Text(it, color = MColors.Rose, style = MaterialTheme.typography.bodySmall) }
                            if (locationSettings) {
                                MPillActionButton(
                                    text = "Konum ayarlarını aç",
                                    icon = Icons.Rounded.Settings,
                                    tone = MPillTone.NEUTRAL,
                                    compact = true,
                                    onClick = ::openApplicationSettings
                                )
                            }
                        }
                    }
                }
                if (feedBusy && displayedMemories.isEmpty()) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                feedError?.let { error ->
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFFF1F2),
                            border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(error, color = MColors.Rose, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                MPillActionButton(
                                    text = "Tekrar dene",
                                    icon = Icons.Rounded.Refresh,
                                    tone = MPillTone.DANGER,
                                    compact = true,
                                    onClick = { shuffleSeed++ }
                                )
                            }
                        }
                    }
                }
                item {
                    var expandedFilter by remember { mutableStateOf(false) }
                    val filterOptions = listOf(
                        ShowcaseFeedMode.NEARBY_LOCATION to "Konuma Göre",
                        ShowcaseFeedMode.RANDOM_EXPLORE to "Karışık",
                        ShowcaseFeedMode.TOP_LIKED to "En Çok Beğenilen",
                        ShowcaseFeedMode.NEWEST to "En Yeniler",
                        ShowcaseFeedMode.MINE to "Paylaşımlarım"
                    )
                    val currentLabel = filterOptions.firstOrNull { it.first == showcaseMode }?.second ?: "Konuma Göre"

                    MCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Tek Açılır Menü (Konuma Göre / Karışık / En Çok Beğenilen)
                            Box(modifier = Modifier.weight(1f)) {
                                Surface(
                                    onClick = { expandedFilter = true },
                                    shape = RoundedCornerShape(11.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (expandedFilter) MColors.Brand.copy(alpha = 0.6f) else Color(0xFFE2E8F0)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = currentLabel,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MColors.Ink900,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Icon(
                                            imageVector = if (expandedFilter) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = MColors.Ink700,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = expandedFilter,
                                    onDismissRequest = { expandedFilter = false },
                                    containerColor = Color.White,
                                    shape = RoundedCornerShape(16.dp),
                                    shadowElevation = 10.dp,
                                    border = BorderStroke(1.dp, Color(0xFFE4E9F2)),
                                    modifier = Modifier.widthIn(min = 190.dp)
                                ) {
                                    filterOptions.forEach { (mode, label) ->
                                        val isSelected = mode == showcaseMode
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = label,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                        color = if (isSelected) MColors.BrandDeep else MColors.Ink900
                                                    )
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Check,
                                                            contentDescription = null,
                                                            tint = MColors.BrandDeep,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                if (mode == ShowcaseFeedMode.MINE) requireAccount { showcaseMode = mode } else showcaseMode = mode
                                                expandedFilter = false
                                            }
                                        )
                                    }
                                }
                            }

                            Surface(
                                onClick = { shuffleSeed++ },
                                shape = RoundedCornerShape(11.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Refresh, "Akışı yenile", tint = MColors.Ink700, modifier = Modifier.size(19.dp))
                                }
                            }
                            // Fotoğraf Ekle Butonu
                            Surface(
                                onClick = {
                                    requireAccount {
                                        preselectedChatIdForMemory = null
                                        showCreateMemorySheet = true
                                    }
                                },
                                shape = RoundedCornerShape(11.dp),
                                color = MColors.Ink900,
                                shadowElevation = 1.dp,
                                border = BorderStroke(1.dp, Color(0xFFFFB37B).copy(alpha = 0.45f)),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .background(Brush.linearGradient(listOf(Color(0xFF283548), MColors.Ink900)))
                                        .padding(horizontal = 13.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AddAPhoto,
                                        contentDescription = "Fotoğraf Ekle",
                                        tint = Color(0xFFFFB37B),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Fotoğraf Ekle",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        val hasMoreMemories = backend != null && nextShowcaseCursor.isNotBlank()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${displayedMemories.size}${if (hasMoreMemories) "+" else ""} saha karesi",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MColors.Ink700
                            )
                            Text(
                                text = if (hasMoreMemories) "Aşağı kaydırdıkça yeni kareler yüklenir" else "Düzenli 2'li kare akışı",
                                fontSize = 10.5.sp,
                                color = MColors.Ink500
                            )
                        }
                        }
                    }
                }

                if (displayedMemories.isEmpty()) {
                    item {
                        MCard(modifier = Modifier.fillMaxWidth(), onClick = {
                            requireAccount {
                                preselectedChatIdForMemory = null
                                showCreateMemorySheet = true
                            }
                        }) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Henüz saha anısı yok", fontWeight = FontWeight.Bold, color = MColors.Ink900)
                                MPillActionButton(
                                    text = "İlk anıyı sen ekle",
                                    icon = Icons.Rounded.AddAPhoto,
                                    tone = MPillTone.BRAND,
                                    onClick = {
                                        requireAccount {
                                            preselectedChatIdForMemory = null
                                            showCreateMemorySheet = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // 2 Sütunlu Düzenli Instagram Explore Kare Izgarası (Her resim eşit 1:1 kare, sonsuz akış)
                    items(
                        items = displayedMemories.chunked(2),
                        key = { row -> "mem_row_" + row.joinToString("_") { it.id } }
                    ) { rowPosts ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowPosts.forEach { post ->
                                Box(modifier = Modifier.weight(1f)) {
                                    GridMemoryExploreTile(
                                        post = post,
                                        onClick = { selectedMemoryForDetail = post },
                                        onLike = { requireAccount { onToggleLikeMemory(post.id, !post.likedByMe) } }
                                    )
                                }
                            }
                            if (rowPosts.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    if (backend != null && (feedBusy || nextShowcaseCursor.isNotBlank())) {
                        item(key = "showcase_load_more") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (feedBusy) {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                }
                                if (nextShowcaseCursor.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            feedScope.launch {
                                                feedBusy = true
                                                feedError = null
                                                try {
                                                    backend.loadMoreMemories()
                                                } catch (cancel: kotlinx.coroutines.CancellationException) {
                                                    throw cancel
                                                } catch (error: Exception) {
                                                    feedError = error.message
                                                } finally {
                                                    feedBusy = false
                                                }
                                            }
                                        },
                                        enabled = !feedBusy,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text(if (feedBusy) "Daha fazla kare yükleniyor…" else "Daha fazla fotoğraf göster")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // ═══════════════════════════════════════════════════════════════════════
        // AKTİF SOHBET ODASI (MESAJLAŞMA + RESİM / SAHA ANISI PAYLAŞMA)
        // ═══════════════════════════════════════════════════════════════════════
        val listState = rememberLazyListState()
        val openChatGalleryPicker = rememberGalleryPhotoPicker { bitmap ->
            if (bitmap != null) {
                val captionText = input.trim()
                val chatPhoto = MatchMemoryPost(
                    id = "chat_img_${kotlin.random.Random.nextInt(10000, 99999)}", isShowcase = false,
                    authorId = DemoData.ME,
                    authorName = currentUser.name,
                    authorInitials = currentUser.initials,
                    sport = Sport.FOOTBALL,
                    venue = "",
                    area = currentDistrict,
                    city = currentCity,
                    distanceKm = null,
                    caption = captionText,
                    matchScoreTag = null,
                    timeAgo = "Şimdi",
                    likes = 0,
                    customBitmap = bitmap
                )
                onSendMemoryToChat(active.id, captionText, chatPhoto)
                input = ""
            }
        }
        LaunchedEffect(active.id, active.messages.lastOrNull()?.id) {
            if (active.messages.isNotEmpty()) listState.animateScrollToItem(active.messages.size)
        }
        Column(Modifier.fillMaxSize().background(MColors.Canvas).imePadding()) {
            Surface(color = Color.White, shadowElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { open(null) }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Sohbetlere dön", tint = MColors.Ink800)
                    }
                    val activeDirectOtherId = inboxSnapshot?.let { BackendSnapshot(it).otherUserForThread(active.id) }
                    Box(
                        modifier = Modifier.clickable(enabled = active.type == ThreadType.DIRECT) {
                            inspectedPlayer = resolvePlayerForName(active.title, active.id)
                        }
                    ) {
                        ChatAvatar(active, compact = true, directUserId = activeDirectOtherId)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(
                        modifier = Modifier.weight(1f).clickable {
                            if (active.type == ThreadType.DIRECT) {
                                inspectedPlayer = resolvePlayerForName(active.title, active.id)
                            } else {
                                showInfo = true
                            }
                        },
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = active.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MColors.Ink900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (active.type == ThreadType.DIRECT) "Sporcu Pasaportu için tıkla 🪪" else "${active.memberCount} kişi · ${active.type.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (active.type == ThreadType.DIRECT) MColors.BrandDeep else MColors.Ink500
                        )
                    }
                    IconButton(onClick = { showInfo = true }) {
                        Icon(Icons.Rounded.MoreHoriz, "Sohbet bilgisi", tint = MColors.Ink600)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "older_messages") { OlderMessagesAction(active.id) }
                itemsIndexed(active.messages, key = { _, message -> message.id }) { index, message ->
                    if (index == 0 || message.day != active.messages[index - 1].day) Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                        Text(message.day.ifBlank { if (backend == null) "Bugün" else "Sohbet geçmişi" }, style = MaterialTheme.typography.labelMedium, color = MColors.Ink400)
                    }
                    if (message.isSystem) {
                        Text(
                            text = message.text,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MColors.Ink500,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 20.dp,
                                    topEnd = 20.dp,
                                    bottomStart = if (message.isMine) 20.dp else 6.dp,
                                    bottomEnd = if (message.isMine) 6.dp else 20.dp
                                ),
                                shadowElevation = 4.dp,
                                color = if (message.isMine) MColors.Ink800 else Color.White,
                                modifier = Modifier.widthIn(max = 296.dp).combinedClickable(onClick = {}, onLongClick = { messageMenu = message })
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (!message.isMine && active.type != ThreadType.DIRECT) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable {
                                                inspectedPlayer = resolvePlayerForName(message.senderName, message.senderId)
                                            }
                                        ) {
                                            UserAvatar(
                                                userId = message.senderId,
                                                name = message.senderName,
                                                modifier = Modifier.size(20.dp),
                                                fallbackColor = MColors.BrandTint,
                                                fallbackTextColor = MColors.BrandDeep
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = message.senderName,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MColors.BrandDeep
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = "🪪",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }

                                    message.replyText?.let { quote ->
                                        Text(
                                            text = "↪ $quote",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (message.isMine) Color.White.copy(alpha = .75f) else MColors.Ink500,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.clickable(enabled = message.replyToId != null) {
                                                val targetIdx = active.messages.indexOfFirst { it.id == message.replyToId }
                                                if (targetIdx >= 0) feedScope.launch { listState.animateScrollToItem(targetIdx + 1) }
                                            }
                                        )
                                    }
                                    // Eğer mesajda paylaşılan bir fotoğraf / saha anısı varsa:
                                    message.attachedMemory?.let { memory ->
                                        ChatAttachedPhotoBubble(
                                            memory = memory,
                                            onClick = { selectedMemoryForDetail = memory }
                                        )
                                    }

                                    if (message.text.isNotBlank()) {
                                        Text(
                                            text = message.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (message.isMine) Color.White else MColors.Ink900
                                        )
                                    }

                                    Text(
                                        text = "${message.time}${if (message.edited) " · düzenlendi" else ""}${if (message.isMine && message.readBy > 0) " · görüldü" else ""}",
                                        modifier = Modifier.align(Alignment.End),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (message.isMine) Color.White.copy(alpha = 0.65f) else MColors.Ink400
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Surface(color = Color.White, shadowElevation = 8.dp) {
                Column(Modifier.navigationBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp)) {
                    replyTarget?.let { reply -> Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) { Text("↪ ${reply.senderName}: ${reply.text}", Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis); IconButton(onClick = { replyTarget = null }) { Icon(Icons.Rounded.Close, "Yanıtı iptal et") } } }
                    if (active.locked || active.id in blockedThreadIds) {
                        Text(
                            text = if (active.id in ownBlockedThreadIds) "Bu sohbet engellendi. Bilgi menüsünden engeli kaldırabilirsin." else "Bu sohbete erişimin kapalı.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MColors.Ink500
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Galeriden Doğrudan Sohbete Fotoğraf Gönderme Butonu
                            Surface(
                                onClick = { openChatGalleryPicker() },
                                shape = RoundedCornerShape(12.dp),
                                color = MColors.Ink900,
                                shadowElevation = 4.dp,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        imageVector = Icons.Rounded.AddPhotoAlternate,
                                        contentDescription = "Galeriden Fotoğraf Gönder",
                                        tint = Color(0xFFFFB37B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            OutlinedTextField(
                                textStyle = MaterialTheme.typography.bodyMedium,
                                value = input,
                                onValueChange = { input = it },
                                placeholder = { Text("Mesaj yaz…") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(22.dp),
                                maxLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MColors.Line,
                                    focusedBorderColor = MColors.Ink500,
                                    unfocusedContainerColor = MColors.SurfaceMuted
                                )
                            )

                            FilledIconButton(
                                onClick = {
                                    val text = input.trim()
                                    if (text.isNotEmpty()) {
                                        if (replyTarget != null) onSendReply(active.id, text, replyTarget?.id) else onSendMessage(active.id, text)
                                        input = ""; replyTarget = null
                                    }
                                },
                                enabled = input.isNotBlank(),
                                modifier = Modifier.size(44.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MColors.BrandDeep)
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Send, "Gönder", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        if (showInfo) {
            val infoDirectOtherId = inboxSnapshot?.let { BackendSnapshot(it).otherUserForThread(active.id) }
            AlertDialog(
                shape = RoundedCornerShape(24.dp),
                containerColor = Color(0xFFFAFBFC),
                tonalElevation = 0.dp,
                onDismissRequest = { showInfo = false },
                icon = { ChatAvatar(active, directUserId = infoDirectOtherId) },
                title = { Text(active.title) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (active.locked) "Bu sohbet yalnızca katılımcılara açık." else "${active.type.label} · ${active.memberCount} kişi",
                            style = MaterialTheme.typography.bodySmall,
                            color = MColors.Ink500
                        )
                        if (active.relatedEventId != null && onOpenRelatedEvent != null) {
                            MPremiumMenuRow("Maç / Etkinlik Detayı", Icons.Rounded.Sports, {
                                showInfo = false
                                onOpenRelatedEvent(active.relatedEventId)
                            })
                        }
                        if (active.type == ThreadType.TEAM && onOpenRelatedTeam != null) {
                            MPremiumMenuRow("Takım Sayfasına Git", Icons.Rounded.Shield, {
                                showInfo = false
                                onOpenRelatedTeam(active.title)
                            })
                        }
                        if (active.type == ThreadType.DIRECT) {
                            MPremiumMenuRow("Sporcu Pasaportunu Gör", Icons.Rounded.Badge, {
                                showInfo = false
                                inspectedPlayer = resolvePlayerForName(active.title, active.id)
                            })
                        }
                        BackendChatMuteAction(active)
                        MPremiumMenuRow("Geçmişi temizle", Icons.Rounded.CleaningServices, { showInfo = false; clearChatConfirmation = false })
                        MPremiumMenuRow("Sohbeti sil", Icons.Rounded.DeleteOutline, { showInfo = false; deleteAndBlock = false; clearChatConfirmation = true }, destructive = true)
                        MPremiumMenuRow("Sohbeti bildir", Icons.Rounded.Flag, { showInfo = false; onReportThread(active) }, destructive = true)
                        if (active.type == ThreadType.DIRECT) {
                            MPremiumMenuRow(
                                text = if (active.id in ownBlockedThreadIds) "Engeli kaldır" else "Sohbeti engelle",
                                icon = Icons.Rounded.Block,
                                onClick = { showInfo = false; confirmBlockThreadId = active.id },
                                destructive = true
                            )
                        }
                    }
                },
                confirmButton = { MPillActionButton("Kapat", tone = MPillTone.NEUTRAL, compact = true, onClick = { showInfo = false }) }
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MAÇ SONRASI ANI BIRAK / SOHBETTE FOTOĞRAF PAYLAŞ SHEET
    // ═══════════════════════════════════════════════════════════════════════════
    confirmBlockThreadId?.let { targetThreadId ->
        val isBlocked = targetThreadId in ownBlockedThreadIds
        AlertDialog(
            onDismissRequest = { confirmBlockThreadId = null },
            title = { Text(if (isBlocked) "Engel kaldırılsın mı?" else "Sohbet engellensin mi?") },
            text = { Text(if (isBlocked) "Bu kişiyle tekrar direkt mesajlaşabilirsin." else "Bu kişiyle yeni direkt mesajlaşma engellenir.") },
            confirmButton = {
                TextButton(onClick = { onToggleBlock(targetThreadId); confirmBlockThreadId = null }) {
                    Text(if (isBlocked) "Engeli kaldır" else "Engelle", color = if (isBlocked) MColors.BrandDeep else MColors.Rose)
                }
            },
            dismissButton = { TextButton(onClick = { confirmBlockThreadId = null }) { Text("Vazgeç") } }
        )
    }
    if (showCreateMemorySheet) {
        CreateMemoryPostSheet(
            currentUser = currentUser,
            currentCity = currentCity,
            currentDistrict = currentDistrict,
            threads = threads.filter { !it.locked },
            preselectedThreadId = preselectedChatIdForMemory,
            onDismiss = { showCreateMemorySheet = false },
            onPublish = { newPost, targetThreadId ->
                onCreateMemoryPost(newPost, targetThreadId)
                showCreateMemorySheet = false
            }
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ANI DETAY & SOHBETE GÖNDERME PENCERESİ
    // ═══════════════════════════════════════════════════════════════════════════
    selectedMemoryForDetail?.let { detailPost ->
        val livePost = visibleMemories.firstOrNull { it.id == detailPost.id } ?: detailPost
        MemoryDetailSheet(
            post = livePost,
            threads = threads.filter { !it.locked },
            onDismiss = { selectedMemoryForDetail = null },
            onLike = if (livePost.isShowcase && livePost.status == "approved") ({ requireAccount { onToggleLikeMemory(livePost.id, !livePost.likedByMe) } }) else null,
            onAddComment = if (livePost.isShowcase && livePost.status == "approved") ({ text -> requireAccount { onAddMemoryComment(livePost.id, text) } }) else null,
            onDelete = if (livePost.authorId == DemoData.ME && livePost.isShowcase) ({ requireAccount { onDeleteMemory(livePost); selectedMemoryForDetail = null } }) else null,
            onEdit = if (livePost.authorId == DemoData.ME && livePost.isShowcase) ({ requireAccount { editingMemory = livePost; selectedMemoryForDetail = null } }) else null,
            onReport = { requireAccount { onReportMemory(livePost); selectedMemoryForDetail = null } },
            onShareToThread = { threadId ->
                requireAccount {
                    onSendMemoryToChat(threadId, livePost.caption, livePost)
                    selectedMemoryForDetail = null
                    open(threadId)
                }
            }
        )
    }
    messageMenu?.let { message -> AlertDialog(onDismissRequest = { messageMenu = null }, title = { Text("Mesaj işlemleri") }, text = {
        Column {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!message.deleted && active?.locked != true) MPremiumMenuRow("Yanıtla", Icons.Rounded.Reply, { replyTarget = message; messageMenu = null })
                if (message.isMine && !message.deleted && (backend == null || kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - message.createdAt <= 15 * 60000)) MPremiumMenuRow("Düzenle", Icons.Rounded.Edit, { editingMessage = message; editedText = message.text; messageMenu = null })
                MPremiumMenuRow("Benden sil", Icons.Rounded.VisibilityOff, { deletingMessage = message to false; messageMenu = null })
                if (message.isMine && !message.deleted) MPremiumMenuRow("Herkesten sil", Icons.Rounded.DeleteOutline, { deletingMessage = message to true; messageMenu = null }, destructive = true)
                if (!message.isMine && !message.deleted) MPremiumMenuRow("Mesajı bildir", Icons.Rounded.Flag, { onReportMessage(message); messageMenu = null }, destructive = true)
            }
        }
    }, confirmButton = { TextButton(onClick = { messageMenu = null }) { Text("Kapat") } }) }
    deletingMessage?.let { (message, everyone) -> AlertDialog(onDismissRequest = { deletingMessage = null }, title = { Text(if (everyone) "Mesaj herkesten silinsin mi?" else "Mesaj senden silinsin mi?") }, text = { Text(if (everyone) "İçerik kaldırılır; diğer katılımcılar silinen mesaj işaretini görür." else "Mesaj yalnız senin geçmişinden kaldırılır.") },
        confirmButton = { TextButton(onClick = { onDeleteMessage(message, everyone); deletingMessage = null }) { Text("Sil", color = MColors.Rose) } }, dismissButton = { TextButton(onClick = { deletingMessage = null }) { Text("Vazgeç") } }) }
    editingMessage?.let { message -> AlertDialog(onDismissRequest = { editingMessage = null }, title = { Text("Mesajı düzenle") }, text = { OutlinedTextField(editedText, { editedText = it.take(4000) }, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(enabled = editedText.isNotBlank(), onClick = { onEditMessage(message, editedText.trim()); editingMessage = null }) { Text("Kaydet") } }, dismissButton = { TextButton(onClick = { editingMessage = null }) { Text("Vazgeç") } }) }
    clearChatConfirmation?.let { remove -> active?.let { thread -> AlertDialog(onDismissRequest = { clearChatConfirmation = null }, title = { Text(if (remove) "Sohbet silinsin mi?" else "Geçmiş temizlensin mi?") }, text = {
        Column { Text("Bu işlem yalnız senin geçmişini temizler. Karşı tarafın mesajları değişmez.${if (remove) " Yeni mesaj gelirse sohbet tekrar görünür; grup üyeliğin devam eder." else ""}")
            if (remove && thread.type == ThreadType.DIRECT) Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(deleteAndBlock, { deleteAndBlock = it }); Text("Kişiyi de engelle") }
        }
    }, confirmButton = { TextButton(onClick = { if (remove) { onDeleteChat(thread, deleteAndBlock); open(null) } else onClearChat(thread); clearChatConfirmation = null }) { Text(if (remove) "Sohbeti sil" else "Geçmişi temizle", color = MColors.Rose) } }, dismissButton = { TextButton(onClick = { clearChatConfirmation = null }) { Text("Vazgeç") } }) } }
    editingMemory?.let { post -> CreateMemoryPostSheet(currentUser, post.city, post.area, threads.filter { !it.locked }, null, { editingMemory = null }, { updated, _ -> onUpdateMemory(updated); editingMemory = null }, initialPost = post) }

    inspectedPlayer?.let { player ->
        PlayerProfileSheet(
            name = player.name,
            initials = player.initials,
            rating = player.rating,
            rank = 1,
            player = player,
            sport = Sport.FOOTBALL,
            onDismiss = { inspectedPlayer = null },
            onChallenge = null,
            onInviteTeam = null,
            onDirectMessage = {
                val target = player
                inspectedPlayer = null
                val existingThread = threads.firstOrNull { it.type == ThreadType.DIRECT && it.title.equals(target.name, ignoreCase = true) }
                if (existingThread != null) {
                    open(existingThread.id)
                } else {
                    onOpenDirectChat(target)
                }
            }
        )
    }
}

@Composable
private fun OlderMessagesAction(threadId: String) {
    val backend = LocalBackendRepository.current ?: return
    val snapshot by backend.snapshots.collectAsState()
    val scope = rememberCoroutineScope()
    var loading by remember(threadId) { mutableStateOf(false) }
    var error by remember(threadId) { mutableStateOf<String?>(null) }
    if (snapshot != null && backend.canLoadOlderMessages(threadId)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            TextButton(onClick = { scope.launch {
                loading = true; error = null
                try { backend.loadOlderMessages(threadId) }
                catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                catch (e: Exception) { error = e.message }
                finally { loading = false }
            } }, enabled = !loading) { Text(if (loading) "Mesajlar yükleniyor…" else "Önceki mesajları yükle") }
            error?.let { Text(it, color = MColors.Rose, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// GÖRSEL BİLEŞENLER: SAHA ANILARI VİTRİNİ (INSTAGRAM KEŞFET STİLİ)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun MiniMemoryStoryTile(
    post: MatchMemoryPost,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MColors.Ink900,
        shadowElevation = 5.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .width(84.dp)
            .height(104.dp)
            .shadow(6.dp, RoundedCornerShape(14.dp), ambientColor = Color(0x2A0B1220), spotColor = Color(0x3C0B1220))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            MatchMemoryVisualCanvas(
                post = post,
                modifier = Modifier.fillMaxSize()
            )
            // Üst konum rozeti
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color.Black.copy(alpha = 0.58f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
            ) {
                Text(
                    text = "📍 ${post.area}${post.distanceKm?.let { " · ≈$it km" }.orEmpty()}",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
            // Alt bilgi
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                        )
                    )
                    .padding(horizontal = 6.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "${post.sport.emoji} ${post.authorName.substringBefore(" ")}",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HeroMemoryExploreCard(
    post: MatchMemoryPost,
    onClick: () -> Unit,
    onLike: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MColors.Ink900,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, Color(0xFFD8E2EE)),
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x340B1220), spotColor = Color(0x480B1220))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            MatchMemoryVisualCanvas(
                post = post,
                modifier = Modifier.fillMaxSize()
            )

            // Üst Satır: Konum & Maç Skoru Etiketi
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.62f),
                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.22f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.LocationOn, null, tint = Color(0xFFFFB37B), modifier = Modifier.size(13.dp))
                        Text(
                            text = "${post.venue} · ${post.area}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                post.matchScoreTag?.let { score ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MColors.Ink900.copy(alpha = 0.85f),
                        border = BorderStroke(0.8.dp, post.sceneTheme.accentColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "${post.sport.emoji} $score",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Alt Satır: Oyuncu, Anı Notu ve Beğeni
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = Modifier.weight(1f).padding(end = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(post.sceneTheme.accentColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (post.authorAvatarUploadId != null) {
                                BackendPhoto(post.authorAvatarUploadId, post.authorName, Modifier.fillMaxSize())
                            } else {
                                Text(
                                    text = post.authorInitials,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MColors.Ink900
                                )
                            }
                        }
                        Text(
                            text = "${post.authorName} · ${post.timeAgo}${if (post.status != "approved") " · İncelemede" else ""}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.92f)
                        )
                    }
                    Text(
                        text = post.caption,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    onClick = onLike,
                    shape = RoundedCornerShape(11.dp),
                    color = if (post.likedByMe) Color(0xFFE11D48) else Color.White.copy(alpha = 0.18f),
                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.28f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (post.likedByMe) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Beğen",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${post.likes}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GridMemoryExploreTile(
    post: MatchMemoryPost,
    onClick: () -> Unit,
    onLike: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MColors.Ink900,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, Color(0xFFD8E2EE)),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x2E0B1220), spotColor = Color(0x420B1220))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            MatchMemoryVisualCanvas(
                post = post,
                modifier = Modifier.fillMaxSize()
            )

            // Üst Konum & Branş Hapı
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.58f)
                ) {
                    Text(
                        text = "📍 ${post.area}${post.distanceKm?.let { " · ≈$it km" }.orEmpty()}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(post.sport.emoji, fontSize = 13.sp)
                }
            }

            // Alt Bilgi Şeridi
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                        )
                    )
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = post.caption,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = post.authorName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        modifier = Modifier.clickable { onLike() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = if (post.likedByMe) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = null,
                            tint = if (post.likedByMe) Color(0xFFFB7185) else Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${post.likes}",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatAttachedPhotoBubble(
    memory: MatchMemoryPost,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MColors.Ink900,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            MatchMemoryVisualCanvas(
                post = memory,
                modifier = Modifier.fillMaxSize()
            )
            if (memory.venue.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.62f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "📍 ${memory.venue} · ${memory.area}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
            memory.matchScoreTag?.let { score ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MColors.Ink900.copy(alpha = 0.82f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "${memory.sport.emoji} $score",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchMemoryVisualCanvas(
    post: MatchMemoryPost,
    modifier: Modifier = Modifier
) {
    if (post.customBitmap != null) {
        Image(
            bitmap = post.customBitmap,
            contentDescription = post.caption,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else if (post.remoteUploadId != null) {
        BackendPhoto(post.remoteUploadId, post.caption, modifier)
    } else {
        val theme = post.sceneTheme
        Box(
            modifier = modifier.background(
                Brush.linearGradient(
                    listOf(theme.topColor, theme.bottomColor)
                )
            )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Saha / Kort Çizgileri ve Işık Efektleri
                drawCircle(
                    color = theme.accentColor.copy(alpha = 0.16f),
                    radius = size.minDimension * 0.55f,
                    center = Offset(size.width * 0.82f, size.height * 0.22f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.10f),
                    radius = size.minDimension * 0.28f,
                    center = Offset(size.width * 0.5f, size.height * 0.52f),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.11f),
                    topLeft = Offset(size.width * 0.08f, size.height * 0.14f),
                    size = Size(size.width * 0.84f, size.height * 0.72f),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                    style = Stroke(width = 1.2.dp.toPx())
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.10f),
                    start = Offset(size.width * 0.5f, size.height * 0.14f),
                    end = Offset(size.width * 0.5f, size.height * 0.86f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            // Merkezde Spor Atmosfer İkonu
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(54.dp)
                    .background(Color.White.copy(alpha = 0.10f), CircleShape)
                    .border(1.dp, theme.accentColor.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(post.sport.emoji, fontSize = 26.sp)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SAHA ANISI BIRAK / FOTOĞRAF EKLEME PENCERESİ
// ═══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateMemoryPostSheet(
    currentUser: UserProfile,
    currentCity: String,
    currentDistrict: String,
    threads: List<ChatThread>,
    preselectedThreadId: String?,
    onDismiss: () -> Unit,
    onPublish: (MatchMemoryPost, String?) -> Unit,
    initialPost: MatchMemoryPost? = null
) {
    val liveMode = LocalBackendRepository.current != null
    var selectedSport by remember { mutableStateOf(initialPost?.sport ?: Sport.FOOTBALL) }
    val venueOptions = if (liveMode) emptyList() else listOf(
        "Moda Sahil Arena · Moda",
        "Kalamış Tenis Kulübü · Kalamış",
        "Caddebostan Sahil Kortu · Caddebostan",
        "Suadiye Padel Loft · Suadiye",
        "Fenerbahçe Kum Kortu · Fenerbahçe",
        "Acıbadem Kampüs Sahası · Acıbadem"
    )
    var selectedVenue by remember { mutableStateOf(initialPost?.let { "${it.venue} · ${it.area}" } ?: if (liveMode) "" else venueOptions.first()) }
    var selectedTheme by remember { mutableStateOf(initialPost?.sceneTheme ?: MemorySceneTheme.NIGHT_TURF) }
    var pickedBitmap by remember { mutableStateOf<ImageBitmap?>(initialPost?.customBitmap) }
    var caption by remember { mutableStateOf(initialPost?.caption.orEmpty()) }
    var scoreTag by remember { mutableStateOf(initialPost?.matchScoreTag.orEmpty()) }
    var mapPoint by remember { mutableStateOf(initialPost?.approximateLocation) }
    var showMap by remember { mutableStateOf(false) }
    var postCity by remember { mutableStateOf(initialPost?.city ?: currentCity) }
    var postDistrict by remember { mutableStateOf(initialPost?.area ?: currentDistrict) }
    var showPostRegion by remember { mutableStateOf(false) }

    val shareTargetOptions: List<Pair<String?, String>> = remember(threads) {
        listOf(null to "Yalnızca Anı Vitrininde Paylaş") +
            threads.map { it.id to "Vitrin + ${it.title} Sohbeti" }
    }
    var selectedShareTarget by remember(preselectedThreadId, shareTargetOptions) {
        mutableStateOf(
            shareTargetOptions.firstOrNull { it.first == preselectedThreadId } ?: shareTargetOptions.first()
        )
    }

    val openGalleryPicker = rememberGalleryPhotoPicker { bitmap ->
        pickedBitmap = bitmap
    }

    ModalBottomSheet(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        scrimColor = Color(0xFF0B1220).copy(alpha = .48f),
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF5F6F8),
        dragHandle = { MPremiumSheetHandle() }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                MSheetHeading(
                    title = if (initialPost == null) "Saha Anısı Bırak" else "Paylaşımını düzenle",
                    subtitle = if (initialPost == null) "Saha karesini anında vitrine ve konuma göre akışa ekle" else "Güncellediğin bilgiler anında vitrinde görünür",
                    icon = Icons.Rounded.AddAPhoto,
                    onDismiss = onDismiss
                )
            }

            // Canlı Önizleme & Galeriden Fotoğraf Seçimi
            item {
                val previewPost = MatchMemoryPost(
                    id = "preview",
                    authorId = DemoData.ME,
                    authorName = currentUser.name,
                    authorInitials = currentUser.initials,
                    sport = selectedSport,
                    venue = selectedVenue.substringBefore(" · "),
                    area = selectedVenue.substringAfter(" · ", postDistrict),
                    city = postCity,
                    distanceKm = null,
                    caption = caption.ifBlank { "Saha karesi 🔥" },
                    matchScoreTag = scoreTag.takeIf { it.isNotBlank() },
                    timeAgo = "Şimdi",
                    likes = 1,
                    sceneTheme = selectedTheme,
                    customBitmap = pickedBitmap, remoteUploadId = initialPost?.remoteUploadId
                )

                MCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(168.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { openGalleryPicker() }
                        ) {
                            MatchMemoryVisualCanvas(
                                post = previewPost,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                onClick = { openGalleryPicker() },
                                shape = RoundedCornerShape(11.dp),
                                color = MColors.Ink900.copy(alpha = 0.88f),
                                border = BorderStroke(1.dp, Color(0xFFFFB37B).copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PhotoLibrary,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB37B),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = if (pickedBitmap != null) "Fotoğrafı Değiştir" else "Galeriden Fotoğraf Seç",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        MFloatingDropdownBox(
                            label = "Saha Atmosferi / Tema",
                            selectedValue = selectedTheme,
                            options = MemorySceneTheme.entries,
                            optionLabel = { it.label },
                            onSelect = { selectedTheme = it },
                            icon = Icons.Rounded.Palette
                        )

                        SportPickerField(selectedSport, { if (it != null) selectedSport = it }, allowAll = false)

                        MFloatingActionRow("İl ve ilçe", "$postCity · $postDistrict", Icons.Rounded.LocationCity, onClick = { showPostRegion = true })
                        VenueChoiceField("Konum & Saha", selectedVenue, { selectedVenue = it; mapPoint = null }, venueOptions, postCity)
                        MFloatingActionRow("Yaklaşık bölge", if (mapPoint == null) "Haritada işaretle · isteğe bağlı" else "Haritada işaretlendi", Icons.Rounded.LocationOn, onClick = { showMap = true })

                        if (initialPost == null) MFloatingDropdownBox(
                            label = "Paylaşım Yeri",
                            selectedValue = selectedShareTarget,
                            options = shareTargetOptions,
                            optionLabel = { it.second },
                            onSelect = { selectedShareTarget = it },
                            icon = Icons.Rounded.Share
                        )

                        MFloatingInputBox(
                            value = caption,
                            onValueChange = { caption = it.take(500) },
                            label = "Anı Notu",
                            placeholder = "Bu kareden bir not bırak…",
                            icon = Icons.Rounded.EditNote,
                            singleLine = false,
                            minLines = 2,
                            maxLines = 3
                        )

                        MFloatingInputBox(
                            value = scoreTag,
                            onValueChange = { scoreTag = it.take(80) },
                            label = "Etiket / Skor (İsteğe Bağlı)",
                            placeholder = "Maç Öncesi · Isınma · 7 - 5",
                            icon = Icons.Rounded.EmojiEvents,
                            singleLine = true
                        )

                        MButton(
                            text = if (initialPost == null) "Anıyı Paylaş" else "Değişiklikleri kaydet",
                            onClick = {
                                val post = MatchMemoryPost(
                                    id = initialPost?.id ?: "mem_${kotlin.random.Random.nextInt(10000, 99999)}",
                                    authorId = DemoData.ME,
                                    authorName = currentUser.name,
                                    authorInitials = currentUser.initials,
                                    sport = selectedSport,
                                    venue = selectedVenue.substringBefore(" · "),
                                    area = selectedVenue.substringAfter(" · ", postDistrict),
                                    city = postCity,
                                    distanceKm = null,
                                    caption = caption.trim().ifBlank { "Sahadan güzel bir anı! 🔥" },
                                    matchScoreTag = scoreTag.trim().takeIf { it.isNotBlank() },
                                    timeAgo = "Şimdi",
                                    likes = 1,
                                    likedByMe = true,
                                    sceneTheme = selectedTheme,
                                    customBitmap = pickedBitmap, remoteUploadId = initialPost?.remoteUploadId,
                                    version = initialPost?.version ?: 1, status = if (initialPost?.status == "rejected") "pending" else "approved", approximateLocation = mapPoint
                                )
                                onPublish(post, selectedShareTarget.first)
                            },
                            icon = Icons.Rounded.Check,
                            enabled = !liveMode || ((pickedBitmap != null || initialPost?.remoteUploadId != null) && selectedVenue.trim().length >= 2)
                        )
                    }
                }
            }
        }
    }
    if (showPostRegion) CityAndLocationSelectorSheet(postCity, postDistrict, false, { showPostRegion = false }, { city, district, _ ->
        if (city != postCity || district != postDistrict) { selectedVenue = ""; mapPoint = null }
        postCity = city; postDistrict = district
    })
    if (showMap) EventLocationPicker(mapPoint, selectedVenue.substringBefore(" · "), postCity, { showMap = false }, { point, address -> mapPoint = point; if (address.isNotBlank()) selectedVenue = "$address · $postDistrict"; showMap = false }, district = postDistrict)

}

// ═══════════════════════════════════════════════════════════════════════════════
// ANI DETAY & SOHBETE İLETME PENCERESİ
// ═══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemoryDetailSheet(
    post: MatchMemoryPost,
    threads: List<ChatThread>,
    onDismiss: () -> Unit,
    onLike: (() -> Unit)?,
    onShareToThread: (String) -> Unit,
    onAddComment: ((String) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onReport: () -> Unit = {}
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var commentInput by remember { mutableStateOf("") }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Anı silinsin mi?") }, text = { Text("Bu paylaşım vitrinden kaldırılacak.") }, confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete?.invoke() }) { Text("Sil", color = MColors.Rose) } }, dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Vazgeç") } })
    var selectedThread by remember(threads) { mutableStateOf(threads.firstOrNull()) }

    val scrollState = rememberScrollState()

    ModalBottomSheet(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        scrimColor = Color(0xFF0B1220).copy(alpha = .48f),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF5F6F8),
        dragHandle = { MPremiumSheetHandle() },
        modifier = Modifier.navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MSheetHeading(
                title = if (post.venue.isNotBlank()) "${post.venue} · ${post.area}" else "Paylaşılan Fotoğraf",
                subtitle = "${post.authorName} · ${post.timeAgo}",
                icon = Icons.Rounded.PhotoCamera,
                onDismiss = onDismiss
            )

            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (post.status != "approved") {
                    Surface(shape = RoundedCornerShape(10.dp), color = MColors.SurfaceSunken, border = BorderStroke(1.dp, MColors.Line), modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (post.status == "rejected") "Bu paylaşım yayınlanmadı. Düzenleyip yeniden incelemeye gönderebilirsin." else "Paylaşımın içerik incelemesi bekliyor.",
                            style = MaterialTheme.typography.bodySmall, color = MColors.Ink600, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (onEdit != null) TextButton(onClick = onEdit) { Text("Paylaşımı düzenle") }
                    if (onDelete != null) TextButton(onClick = { confirmDelete = true }) { Text("Paylaşımı sil", color = MColors.Rose) }
                    if (onEdit == null && onDelete == null) TextButton(onClick = onReport) { Text("İçeriği bildir", color = MColors.Ink500) }
                }
            }

            MCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        MatchMemoryVisualCanvas(
                            post = post,
                            modifier = Modifier.fillMaxSize()
                        )
                        post.matchScoreTag?.let { score ->
                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = MColors.Ink900.copy(alpha = 0.86f),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "${post.sport.emoji} $score",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    if (post.caption.isNotBlank()) {
                        Text(
                            text = post.caption,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MColors.Ink900
                        )
                    }

                    if (threads.isNotEmpty() && selectedThread != null) {
                        MFloatingDropdownBox(
                            label = "Sohbet Odasına Gönder",
                            selectedValue = selectedThread!!,
                            options = threads,
                            optionLabel = { "${it.emoji} ${it.title}" },
                            onSelect = { selectedThread = it },
                            icon = Icons.Rounded.Forum
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Beğen Butonu (Genişlik ve yazısı tam sığacak şekilde optimize edildi)
                        if (onLike != null) Surface(
                            onClick = onLike,
                            shape = RoundedCornerShape(13.dp),
                            color = if (post.likedByMe) Color(0xFFFFF1F2) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (post.likedByMe) Color(0xFFFDA4AF) else Color(0xFFDCE4F0)),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (post.likedByMe) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (post.likedByMe) Color(0xFFE11D48) else MColors.Ink700,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Beğen (${post.likes})",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (post.likedByMe) Color(0xFFBE123C) else MColors.Ink900,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Sohbette Paylaş Butonu (Genişlik ve yazısı tam sığacak şekilde optimize edildi)
                        if (selectedThread != null) {
                            Surface(
                                onClick = { onShareToThread(selectedThread!!.id) },
                                shape = RoundedCornerShape(13.dp),
                                color = MColors.BrandDeep,
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .weight(1.35f)
                                    .height(46.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Send,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Sohbette Paylaş",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    if (post.comments.isNotEmpty() || onAddComment != null) {
                        HorizontalDivider(color = MColors.Line)
                        Text(
                            text = "Yorumlar (${post.comments.size})",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MColors.Ink800
                        )
                        post.comments.forEach { c ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(c.authorName, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MColors.Ink900)
                                    Text(c.text, fontSize = 12.5.sp, color = MColors.Ink700)
                                }
                                Text(c.timeAgo, fontSize = 10.sp, color = MColors.Ink400)
                            }
                        }
                        if (onAddComment != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = commentInput,
                                    onValueChange = { commentInput = it.take(280) },
                                    placeholder = { Text("Yorum yaz…") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                FilledIconButton(
                                    onClick = {
                                        val trimmed = commentInput.trim()
                                        if (trimmed.isNotEmpty()) {
                                            onAddComment(trimmed)
                                            commentInput = ""
                                        }
                                    },
                                    enabled = commentInput.isNotBlank(),
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MColors.BrandDeep)
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.Send, "Gönder", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatAvatar(thread: ChatThread, compact: Boolean = false, directUserId: String? = null) {
    if (thread.type == ThreadType.DIRECT) {
        val otherUserId = directUserId?.takeIf { it.isNotBlank() } ?: thread.messages.firstOrNull { !it.isMine }?.senderId ?: thread.id
        UserAvatar(
            userId = otherUserId,
            name = thread.title,
            modifier = Modifier.size(if (compact) 40.dp else 52.dp),
            fallbackColor = Color(0xFFFFE0C7),
            fallbackTextColor = MColors.BrandDeep
        )
    } else {
        Box(
            modifier = Modifier
                .size(if (compact) 40.dp else 52.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFDCD6FF), Color(0xFFEDF0FF))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (thread.type) {
                    ThreadType.TEAM, ThreadType.SQUAD -> Icons.Rounded.Groups
                    ThreadType.EVENT -> Icons.Rounded.Sports
                    else -> Icons.Rounded.Forum
                },
                contentDescription = null,
                tint = MColors.Ink600,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
